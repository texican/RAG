package com.byo.rag.core.service;

import com.byo.rag.core.client.EmbeddingServiceClient;
import com.byo.rag.core.dto.RagQueryRequest;
import com.byo.rag.core.dto.RagQueryResponse;
import com.byo.rag.core.dto.RagResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for RagService - Core RAG pipeline functionality.
 * 
 * Tests the complete RAG workflow including query optimization, document retrieval,
 * context assembly, LLM integration, caching, async processing, and statistics.
 * 
 * Follows enterprise testing standards from TESTING_BEST_PRACTICES.md:
 * - Uses public API exclusively (no reflection)
 * - Clear test intent with @DisplayName annotations
 * - Realistic test data mimicking production usage
 * - Descriptive assertions with business context
 * - Comprehensive edge case and error handling validation
 * 
 * @see com.byo.rag.core.service.RagService
 * @author BYO RAG Development Team
 * @version 1.0
 * @since 2025-09-09
 */
@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private EmbeddingServiceClient embeddingServiceClient;
    
    @Mock
    private LLMIntegrationService llmIntegrationService;
    
    @Mock
    private ContextAssemblyService contextAssemblyService;
    
    @Mock
    private ConversationService conversationService;
    
    @Mock
    private CacheService cacheService;
    
    @Mock
    private QueryOptimizationService queryOptimizationService;

    @InjectMocks
    private RagService ragService;

    private RagQueryRequest testRequest;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        testRequest = RagQueryRequest.simple(tenantId, "What is Spring AI?");
    }

    /**
     * Validates the complete RAG processing pipeline with successful flow.
     * 
     * This test ensures that:
     * 1. Query optimization service enhances the user query
     * 2. Cache check is performed and returns null (cache miss)
     * 3. Embedding service successfully retrieves similar documents
     * 4. Context assembly service creates coherent context from documents
     * 5. LLM integration service generates appropriate response
     * 6. Response is cached for future requests
     * 7. Final RagQueryResponse contains all expected components
     * 
     * Tests the primary happy path through the entire RAG pipeline.
     */
    /**
     * Validates the complete RAG processing pipeline with successful flow.
     * 
     * This test ensures that:
     * 1. Query optimization service enhances the user query
     * 2. Cache check is performed and returns null (cache miss)
     * 3. Embedding service successfully retrieves similar documents
     * 4. Context assembly service creates coherent context from documents
     * 5. LLM integration service generates appropriate response
     * 6. Response is cached for future requests
     * 7. Final RagQueryResponse contains all expected components
     * 
     * Tests the primary happy path through the entire RAG pipeline following
     * TESTING_BEST_PRACTICES.md guidelines for business logic validation.
     */
    @Test
    @DisplayName("Should complete full RAG pipeline and return valid response")
    void processQuery_SuccessfulFlow_ReturnsValidResponse() {
        // Arrange
        String optimizedQuery = "enhanced: What is Spring AI?";
        String assembledContext = "Context: Spring AI framework information...";
        String llmResponse = "Spring AI is a comprehensive framework for building AI-powered applications in Java.";
        
        RagQueryRequest optimizedRequest = RagQueryRequest.simple(testRequest.tenantId(), optimizedQuery);
        when(queryOptimizationService.optimizeQuery(any(RagQueryRequest.class))).thenReturn(optimizedRequest);
        when(cacheService.getResponse(anyString())).thenReturn(null);
        
        // Mock embedding service response with correct constructor signature
        EmbeddingServiceClient.SearchResponse searchResponse = new EmbeddingServiceClient.SearchResponse(
            testRequest.tenantId(),
            "test query", 
            "test-model",
            List.of(new EmbeddingServiceClient.SearchResult(
                UUID.randomUUID(), // chunkId
                UUID.randomUUID(), // documentId  
                "Test content about Spring AI", // content
                0.95, // score
                Map.of("category", "framework"), // metadata
                "spring-ai-doc.pdf", // documentTitle
                "pdf" // documentType
            )),
            1, // totalResults
            0.95, // maxScore
            100L, // searchTimeMs
            Instant.now().toString() // searchedAt
        );
        when(embeddingServiceClient.search(any(EmbeddingServiceClient.SearchRequest.class), any(UUID.class)))
            .thenReturn(searchResponse);
            
        when(contextAssemblyService.assembleContext(anyList(), any(RagQueryRequest.class)))
            .thenReturn(assembledContext);
        when(llmIntegrationService.generateResponse(anyString(), anyString(), any(RagQueryRequest.class)))
            .thenReturn(llmResponse);
        when(llmIntegrationService.getProviderUsed()).thenReturn("openai");
        
        // Act
        RagQueryResponse response = ragService.processQuery(testRequest);
        
        // Assert
        assertNotNull(response);
        assertEquals(llmResponse, response.response());
        assertEquals(testRequest.query(), response.query());
        assertEquals(testRequest.tenantId(), response.tenantId());
        assertEquals("SUCCESS", response.status());
        assertFalse(response.sources().isEmpty());
        
        // Verify service interactions
        verify(queryOptimizationService).optimizeQuery(any(RagQueryRequest.class));
        verify(embeddingServiceClient).search(any(EmbeddingServiceClient.SearchRequest.class), eq(testRequest.tenantId()));
        verify(contextAssemblyService).assembleContext(anyList(), any(RagQueryRequest.class));
        verify(llmIntegrationService).generateResponse(eq(optimizedQuery), eq(assembledContext), any(RagQueryRequest.class));
    }

    /**
     * Validates cache hit behavior in RAG pipeline.
     * 
     * Ensures that when a cached response exists:
     * 1. No downstream services are called (efficiency)
     * 2. Cached response is returned immediately
     * 3. Response maintains proper format and status
     * 
     * This test validates caching optimization for performance.
     */
    @Test
    @DisplayName("Should return cached response when cache hit occurs")
    void processQuery_CacheHit_ReturnsCachedResponse() {
        // Arrange
        RagResponse cachedRagResponse = new RagResponse(
            "Cached: Spring AI is a framework...",
            0.95,
            List.of(),
            100L,
            java.time.LocalDateTime.now(),
            "conv-123",
            new RagResponse.QueryMetadata(5, "openai", 50, true, "cached")
        );
        
        when(cacheService.getResponse(anyString())).thenReturn(cachedRagResponse);
        
        // Act
        RagQueryResponse response = ragService.processQuery(testRequest);
        
        // Assert
        assertNotNull(response);
        assertNotNull(response.response());
        assertEquals("SUCCESS", response.status());
        
        // Verify no downstream processing occurred
        verify(embeddingServiceClient, never()).search(any(EmbeddingServiceClient.SearchRequest.class), any(UUID.class));
        verify(llmIntegrationService, never()).generateResponse(anyString(), anyString(), any(RagQueryRequest.class));
    }

    /**
     * Validates handling when no relevant documents are found.
     * 
     * Tests that when embedding service returns empty results:
     * 1. Status is set to "EMPTY" appropriately
     * 2. Sources list is empty as expected
     * 3. Context assembly is never called (no documents to assemble)
     * 
     * This validates graceful handling of queries with no relevant content.
     */
    @Test
    @DisplayName("Should handle empty search results gracefully")
    void processQuery_NoDocumentsFound_ReturnsAppropriateResponse() {
        // Arrange
        RagQueryRequest optimizedRequest = RagQueryRequest.simple(testRequest.tenantId(), "optimized query");
        when(queryOptimizationService.optimizeQuery(any(RagQueryRequest.class))).thenReturn(optimizedRequest);
        when(cacheService.getResponse(anyString())).thenReturn(null);
        
        // Mock empty search response
        EmbeddingServiceClient.SearchResponse emptyResponse = new EmbeddingServiceClient.SearchResponse(
            testRequest.tenantId(),
            "test query",
            "test-model", 
            Collections.emptyList(),
            0,
            0.0,
            100L,
            Instant.now().toString()
        );
        when(embeddingServiceClient.search(any(EmbeddingServiceClient.SearchRequest.class), any(UUID.class)))
            .thenReturn(emptyResponse);
        
        // Act
        RagQueryResponse response = ragService.processQuery(testRequest);
        
        // Assert
        assertNotNull(response);
        assertEquals("EMPTY", response.status());
        assertTrue(response.sources().isEmpty());
        
        // Verify context assembly was never called (no documents to assemble)
        verify(contextAssemblyService, never()).assembleContext(anyList(), any(RagQueryRequest.class));
    }

    /**
     * Validates error handling when LLM service fails.
     * 
     * Tests that when LLM integration throws exceptions:
     * 1. Error is caught and handled gracefully
     * 2. Response status is set to "FAILED"
     * 3. Error message is included in response
     * 4. Partial processing results are maintained
     * 
     * This ensures system resilience when external services fail.
     */
    @Test
    @DisplayName("Should handle LLM service failures gracefully")
    void processQuery_LLMFailure_HandlesGracefully() {
        // Arrange
        RagQueryRequest optimizedRequest = RagQueryRequest.simple(testRequest.tenantId(), "optimized query");
        when(queryOptimizationService.optimizeQuery(any(RagQueryRequest.class))).thenReturn(optimizedRequest);
        when(cacheService.getResponse(anyString())).thenReturn(null);
        
        // Mock successful document retrieval
        EmbeddingServiceClient.SearchResponse searchResponse = new EmbeddingServiceClient.SearchResponse(
            testRequest.tenantId(),
            "test query",
            "test-model",
            List.of(new EmbeddingServiceClient.SearchResult(
                UUID.randomUUID(), UUID.randomUUID(), "content", 0.9, 
                Map.of(), "doc.pdf", "pdf"
            )),
            1, 0.9, 100L, Instant.now().toString()
        );
        when(embeddingServiceClient.search(any(EmbeddingServiceClient.SearchRequest.class), any(UUID.class)))
            .thenReturn(searchResponse);
        when(contextAssemblyService.assembleContext(anyList(), any(RagQueryRequest.class)))
            .thenReturn("assembled context");
        when(llmIntegrationService.generateResponse(anyString(), anyString(), any(RagQueryRequest.class)))
            .thenThrow(new RuntimeException("LLM service unavailable"));
        
        // Act
        RagQueryResponse response = ragService.processQuery(testRequest);
        
        // Assert
        assertNotNull(response);
        assertEquals("FAILED", response.status());
        assertNotNull(response.error());
        assertTrue(response.error().contains("LLM service unavailable"));
        
        // Verify partial processing occurred
        verify(embeddingServiceClient).search(any(EmbeddingServiceClient.SearchRequest.class), any(UUID.class));
        verify(contextAssemblyService).assembleContext(anyList(), any(RagQueryRequest.class));
    }

    /**
     * Validates input validation for null requests.
     * 
     * Ensures that:
     * 1. Null request throws appropriate exception
     * 2. No processing occurs with invalid input
     * 3. System maintains defensive programming standards
     * 
     * This validates proper input validation and defensive coding.
     */
    @Test
    @DisplayName("Should throw exception for null request")
    void processQuery_NullRequest_ThrowsException() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> ragService.processQuery(null));
        
        // Verify no processing occurred
        verifyNoInteractions(queryOptimizationService, embeddingServiceClient, llmIntegrationService);
    }

    /**
     * Validates handling of empty query strings.
     * 
     * Tests that empty queries are processed appropriately:
     * 1. Query optimization still occurs
     * 2. Empty search results are handled
     * 3. Status reflects no content found
     * 
     * This ensures robustness with edge case inputs.
     */
    @Test
    @DisplayName("Should handle empty query strings gracefully")
    void processQuery_EmptyQuery_HandlesGracefully() {
        // Arrange
        RagQueryRequest emptyQueryRequest = new RagQueryRequest(
            testRequest.tenantId(),
            "",
            testRequest.conversationId(),
            testRequest.userId(),
            testRequest.sessionId(),
            testRequest.documentIds(),
            testRequest.filters(),
            testRequest.options()
        );
        
        // Mock optimization service to return the empty request
        when(queryOptimizationService.optimizeQuery(any(RagQueryRequest.class))).thenReturn(emptyQueryRequest);
        when(cacheService.getResponse(anyString())).thenReturn(null);
        
        // Mock empty search response for empty query
        EmbeddingServiceClient.SearchResponse emptyResponse = new EmbeddingServiceClient.SearchResponse(
            emptyQueryRequest.tenantId(),
            "",
            "test-model",
            Collections.emptyList(),
            0, 0.0, 100L, Instant.now().toString()
        );
        when(embeddingServiceClient.search(any(EmbeddingServiceClient.SearchRequest.class), any(UUID.class)))
            .thenReturn(emptyResponse);
        
        // Act
        RagQueryResponse response = ragService.processQuery(emptyQueryRequest);
        
        // Assert
        assertNotNull(response);
        assertEquals("EMPTY", response.status());
        assertTrue(response.sources().isEmpty());
    }

    /**
     * Validates tenant isolation requirements.
     * 
     * Ensures that:
     * 1. Null tenant ID is rejected immediately
     * 2. Multi-tenant security is maintained
     * 3. Proper validation occurs before processing
     * 
     * This validates critical multi-tenant security requirements.
     */
    @Test
    @DisplayName("Should reject null tenant ID for security")
    void processQuery_NullTenantId_ThrowsException() {
        // Arrange
        RagQueryRequest nullTenantRequest = new RagQueryRequest(
            null,
            testRequest.query(),
            testRequest.conversationId(),
            testRequest.userId(),
            testRequest.sessionId(),
            testRequest.documentIds(),
            testRequest.filters(),
            testRequest.options()
        );
        
        // Act & Assert
        assertThrows(NullPointerException.class, () -> ragService.processQuery(nullTenantRequest));
    }

    /**
     * Validates asynchronous query processing capability.
     * 
     * Tests that:
     * 1. Async method returns non-null CompletableFuture
     * 2. Future can be completed successfully
     * 3. Result contains expected response format
     * 
     * This validates async processing capabilities for high-throughput scenarios.
     */
    @Test
    @DisplayName("Should process queries asynchronously via CompletableFuture")
    void processQueryAsync_ReturnsCompletableFuture() {
        // Arrange
        when(queryOptimizationService.optimizeQuery(any())).thenReturn(testRequest);
        when(cacheService.getResponse(anyString())).thenReturn(null);
        
        EmbeddingServiceClient.SearchResponse emptyResponse = new EmbeddingServiceClient.SearchResponse(
            testRequest.tenantId(), "query", "model", Collections.emptyList(), 
            0, 0.0, 100L, Instant.now().toString()
        );
        when(embeddingServiceClient.search(any(EmbeddingServiceClient.SearchRequest.class), any(UUID.class))).thenReturn(emptyResponse);
        
        // Act & Assert
        assertDoesNotThrow(() -> {
            var future = ragService.processQueryAsync(testRequest);
            assertNotNull(future);
            var result = future.join(); // This will complete synchronously in test
            assertNotNull(result);
        });
    }

    /**
     * Validates statistics retrieval functionality.
     * 
     * Tests that:
     * 1. Statistics are returned for valid tenant
     * 2. Default values are appropriate (zeros)
     * 3. All metrics are properly initialized
     * 
     * This validates monitoring and analytics capabilities.
     */
    @Test
    @DisplayName("Should return initialized statistics for tenant")
    void getStats_ReturnsDefaultStats() {
        // Act
        RagService.RagStats stats = ragService.getStats(tenantId.toString());
        
        // Assert
        assertNotNull(stats);
        assertEquals(0L, stats.totalQueries());
        assertEquals(0L, stats.successfulQueries());
        assertEquals(0L, stats.cachedQueries());
        assertEquals(0.0, stats.averageResponseTimeMs());
        assertEquals(0.0, stats.averageRelevanceScore());
    }

    /**
     * Validates conversation context integration.
     * 
     * Tests that when conversation ID is provided:
     * 1. Conversation context is retrieved and used
     * 2. Query optimization includes conversation history
     * 3. Response is added to conversation history
     * 4. Context assembly includes conversational context
     * 
     * This validates conversational AI capabilities and context continuity.
     */
    @Test
    @DisplayName("Should integrate conversation history for contextual responses")
    void processQuery_WithConversationHistory_IncludesContext() {
        // Arrange
        String conversationContext = "Previous conversation context...";
        String optimizedQuery = "optimized with context";
        
        // Create request with conversation options
        RagQueryRequest requestWithHistory = new RagQueryRequest(
            testRequest.tenantId(),
            testRequest.query(),
            "conv-123",
            UUID.randomUUID().toString(), // Provide a valid userId
            "session-123",
            null,
            null,
            RagQueryRequest.RagOptions.defaultOptions()
        );
        
        when(conversationService.contextualizeQuery(anyString(), anyString()))
            .thenReturn(conversationContext);
        RagQueryRequest optimizedRequest = RagQueryRequest.simple(requestWithHistory.tenantId(), optimizedQuery);
        when(queryOptimizationService.optimizeQuery(any(RagQueryRequest.class)))
            .thenReturn(optimizedRequest);
        when(cacheService.getResponse(anyString())).thenReturn(null);
        
        // Mock successful document retrieval
        EmbeddingServiceClient.SearchResponse searchResponse = new EmbeddingServiceClient.SearchResponse(
            requestWithHistory.tenantId(),
            "test query",
            "test-model",
            List.of(new EmbeddingServiceClient.SearchResult(
                UUID.randomUUID(), UUID.randomUUID(), "content", 0.9,
                Map.of(), "doc.pdf", "pdf"
            )),
            1, 0.9, 100L, Instant.now().toString()
        );
        when(embeddingServiceClient.search(any(EmbeddingServiceClient.SearchRequest.class), any(UUID.class)))
            .thenReturn(searchResponse);
        when(contextAssemblyService.assembleContext(anyList(), any(RagQueryRequest.class)))
            .thenReturn("assembled context with history");
        when(llmIntegrationService.generateResponse(anyString(), anyString(), any(RagQueryRequest.class)))
            .thenReturn("Response with conversation context");
        when(llmIntegrationService.getProviderUsed()).thenReturn("openai");
        
        // Act
        RagQueryResponse response = ragService.processQuery(requestWithHistory);
        
        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.status());
        verify(conversationService).contextualizeQuery(eq(requestWithHistory.conversationId()), anyString());
        verify(conversationService).addExchange(
            eq(requestWithHistory.conversationId()), 
            any(UUID.class),
            eq(requestWithHistory.query()), 
            eq(response.response()),
            anyList()
        );
    }
}
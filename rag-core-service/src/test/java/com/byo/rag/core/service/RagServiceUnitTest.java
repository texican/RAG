package com.byo.rag.core.service;

import com.byo.rag.core.client.EmbeddingServiceClient;
import com.byo.rag.core.dto.RagQueryRequest;
import com.byo.rag.core.dto.RagQueryResponse;
import com.byo.rag.core.dto.RagResponse;
import org.junit.jupiter.api.BeforeEach;
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
 * Unit tests for RagService - Core RAG pipeline functionality.
 * Tests the complete RAG workflow including query optimization, document retrieval,
 * context assembly, LLM integration, and caching.
 */
@ExtendWith(MockitoExtension.class)
class RagServiceUnitTest {

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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
    void processQuery_NullRequest_ThrowsException() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> ragService.processQuery(null));
        
        // Verify no processing occurred
        verifyNoInteractions(queryOptimizationService, embeddingServiceClient, llmIntegrationService);
    }

    @Test
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

    @Test
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

    @Test
    void processQuery_WithConversationHistory_IncludesContext() {
        // Arrange
        String conversationContext = "Previous conversation context...";
        String optimizedQuery = "optimized with context";
        
        // Create request with conversation options
        RagQueryRequest requestWithHistory = new RagQueryRequest(
            testRequest.tenantId(),
            testRequest.query(),
            "conv-123",
            testRequest.userId(),
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
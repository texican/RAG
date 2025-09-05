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
 * Comprehensive unit tests for RAG Core Service - demonstrates systematic testing approach.
 * This test class validates the core RAG pipeline functionality including:
 * - Query processing and optimization
 * - Document retrieval and context assembly  
 * - LLM integration and response generation
 * - Caching and conversation management
 * - Error handling and validation
 */
@ExtendWith(MockitoExtension.class)
class RagCoreServiceTest {

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
        
        // Mock the complete successful flow
        when(queryOptimizationService.optimizeQuery(any(RagQueryRequest.class))).thenReturn(optimizedRequest);
        when(cacheService.getResponse(anyString())).thenReturn(null);
        
        // Mock embedding service response
        EmbeddingServiceClient.SearchResponse mockSearchResponse = new EmbeddingServiceClient.SearchResponse(
            testRequest.tenantId(),
            "test query",
            "test-model",
            List.of(
                new EmbeddingServiceClient.SearchResult(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "Test document content about Spring AI framework",
                    0.95,
                    Map.of("category", "framework"),
                    "Test Document",
                    "pdf"
                )
            ),
            1,
            0.95,
            100L,
            Instant.now().toString()
        );
        when(embeddingServiceClient.search(any(), any())).thenReturn(mockSearchResponse);
        
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
        assertNull(response.error());
        assertNotNull(response.sources());
        assertFalse(response.sources().isEmpty());
        
        // Verify service interactions
        verify(queryOptimizationService).optimizeQuery(any(RagQueryRequest.class));
        verify(embeddingServiceClient).search(any(), eq(testRequest.tenantId()));
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
        verify(embeddingServiceClient, never()).search(any(), any());
        verify(llmIntegrationService, never()).generateResponse(anyString(), anyString(), any(RagQueryRequest.class));
    }

    @Test
    void processQuery_NoDocumentsFound_ReturnsEmptyResponse() {
        // Arrange
        RagQueryRequest optimizedRequest = RagQueryRequest.simple(testRequest.tenantId(), "optimized query");
        when(queryOptimizationService.optimizeQuery(any(RagQueryRequest.class))).thenReturn(optimizedRequest);
        when(cacheService.getResponse(anyString())).thenReturn(null);
        
        // Mock empty search response
        EmbeddingServiceClient.SearchResponse emptySearchResponse = new EmbeddingServiceClient.SearchResponse(
            testRequest.tenantId(),
            "test query",
            "test-model",
            Collections.emptyList(),
            0,
            0.0,
            100L,
            Instant.now().toString()
        );
        when(embeddingServiceClient.search(any(), any())).thenReturn(emptySearchResponse);
        
        // Act
        RagQueryResponse response = ragService.processQuery(testRequest);
        
        // Assert
        assertNotNull(response);
        assertEquals("EMPTY", response.status());
        assertTrue(response.sources().isEmpty());
        assertNotNull(response.metrics());
        
        // Verify context assembly was never called (no documents to assemble)
        verify(contextAssemblyService, never()).assembleContext(anyList(), any(RagQueryRequest.class));
    }

    @Test
    void processQuery_EmbeddingServiceFailure_HandlesGracefully() {
        // Arrange
        RagQueryRequest optimizedRequest = RagQueryRequest.simple(testRequest.tenantId(), "optimized query");
        when(queryOptimizationService.optimizeQuery(any(RagQueryRequest.class))).thenReturn(optimizedRequest);
        when(cacheService.getResponse(anyString())).thenReturn(null);
        when(embeddingServiceClient.search(any(), any()))
            .thenThrow(new RuntimeException("Embedding service unavailable"));
        
        // Act
        RagQueryResponse response = ragService.processQuery(testRequest);
        
        // Assert
        assertNotNull(response);
        assertEquals("FAILED", response.status());
        assertNotNull(response.error());
        assertTrue(response.error().contains("Embedding service unavailable") || 
                  response.error().contains("Document retrieval failed"));
        
        // Verify downstream services were not called
        verify(contextAssemblyService, never()).assembleContext(anyList(), any(RagQueryRequest.class));
        verify(llmIntegrationService, never()).generateResponse(anyString(), anyString(), any(RagQueryRequest.class));
    }

    @Test
    void processQuery_NullRequest_ThrowsException() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> ragService.processQuery(null));
        
        // Verify no processing occurred
        verifyNoInteractions(queryOptimizationService, embeddingServiceClient, llmIntegrationService);
    }

    @Test
    void processQueryAsync_ReturnsCompletableFuture() {
        // Arrange
        when(queryOptimizationService.optimizeQuery(any())).thenReturn(testRequest);
        when(cacheService.getResponse(anyString())).thenReturn(null);
        
        EmbeddingServiceClient.SearchResponse emptyResponse = new EmbeddingServiceClient.SearchResponse(
            testRequest.tenantId(), "query", "model", Collections.emptyList(), 
            0, 0.0, 100L, Instant.now().toString()
        );
        when(embeddingServiceClient.search(any(), any())).thenReturn(emptyResponse);
        
        // Act & Assert
        assertDoesNotThrow(() -> {
            var future = ragService.processQueryAsync(testRequest);
            assertNotNull(future);
            var result = future.join(); // This will complete synchronously in test
            assertNotNull(result);
        });
    }

    @Test
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
}
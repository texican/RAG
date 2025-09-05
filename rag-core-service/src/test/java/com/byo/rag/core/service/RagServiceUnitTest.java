package com.byo.rag.core.service;

import com.byo.rag.core.client.EmbeddingServiceClient;
import com.byo.rag.core.dto.RagQueryRequest;
import com.byo.rag.core.dto.RagQueryResponse;
import com.byo.rag.shared.dto.DocumentChunkDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RagService - Core RAG pipeline functionality.
 * 
 * Tests cover the complete RAG workflow:
 * - Query processing and validation
 * - Vector search and document retrieval  
 * - Context assembly and LLM integration
 * - Response generation and error handling
 * - Caching behavior and performance optimization
 * 
 * @author BYO RAG System
 * @version 1.0
 * @since 2025-09-05
 */
@ExtendWith(MockitoExtension.class)
class RagServiceUnitTest {

    @Mock
    private EmbeddingServiceClient embeddingServiceClient;
    
    @Mock
    private LLMIntegrationService llmIntegrationService;
    
    @Mock
    private VectorSearchService vectorSearchService;
    
    @Mock
    private ContextAssemblyService contextAssemblyService;
    
    @Mock
    private CacheService cacheService;
    
    @Mock
    private ConversationService conversationService;
    
    @Mock
    private QueryOptimizationService queryOptimizationService;

    @InjectMocks
    private RagService ragService;

    private RagQueryRequest testRequest;
    private List<DocumentChunkDto> mockChunks;

    @BeforeEach
    void setUp() {
        testRequest = new RagQueryRequest();
        testRequest.setQuery("What is Spring AI?");
        testRequest.setTenantId("test-tenant");
        testRequest.setUserId("test-user");
        
        // Create mock document chunks
        DocumentChunkDto chunk1 = new DocumentChunkDto();
        chunk1.setId(1L);
        chunk1.setContent("Spring AI is a framework for building AI applications with Java.");
        chunk1.setSimilarityScore(0.95);
        
        DocumentChunkDto chunk2 = new DocumentChunkDto();
        chunk2.setId(2L);
        chunk2.setContent("It provides integration with various LLM providers like OpenAI and Ollama.");
        chunk2.setSimilarityScore(0.87);
        
        mockChunks = Arrays.asList(chunk1, chunk2);
    }

    /**
     * Test successful RAG query processing with all components working.
     */
    @Test
    void processRagQuery_SuccessfulFlow_ReturnsValidResponse() {
        // Arrange
        String optimizedQuery = "enhanced: What is Spring AI?";
        String assembledContext = "Context: Spring AI framework information...";
        String llmResponse = "Spring AI is a comprehensive framework for building AI-powered applications in Java.";
        
        when(queryOptimizationService.optimizeQuery(anyString(), anyString())).thenReturn(optimizedQuery);
        when(cacheService.getCachedResponse(anyString())).thenReturn(null);
        when(vectorSearchService.searchSimilarDocuments(anyString(), anyString(), anyInt()))
            .thenReturn(mockChunks);
        when(contextAssemblyService.assembleContext(anyList(), anyString()))
            .thenReturn(assembledContext);
        when(llmIntegrationService.generateResponse(anyString(), anyString()))
            .thenReturn(llmResponse);
        
        // Act
        RagQueryResponse response = ragService.processRagQuery(testRequest);
        
        // Assert
        assertNotNull(response);
        assertEquals(llmResponse, response.getResponse());
        assertEquals(testRequest.getQuery(), response.getOriginalQuery());
        assertEquals(2, response.getSourceDocuments().size());
        assertTrue(response.getResponseTimeMs() > 0);
        
        // Verify service interactions
        verify(queryOptimizationService).optimizeQuery(testRequest.getQuery(), testRequest.getTenantId());
        verify(vectorSearchService).searchSimilarDocuments(eq(optimizedQuery), eq(testRequest.getTenantId()), anyInt());
        verify(contextAssemblyService).assembleContext(eq(mockChunks), eq(optimizedQuery));
        verify(llmIntegrationService).generateResponse(eq(assembledContext), eq(optimizedQuery));
        verify(cacheService).cacheResponse(anyString(), any(RagQueryResponse.class));
    }

    /**
     * Test cache hit scenario - should return cached response without processing.
     */
    @Test
    void processRagQuery_CacheHit_ReturnsCachedResponse() {
        // Arrange
        RagQueryResponse cachedResponse = new RagQueryResponse();
        cachedResponse.setResponse("Cached: Spring AI is a framework...");
        cachedResponse.setOriginalQuery(testRequest.getQuery());
        
        when(queryOptimizationService.optimizeQuery(anyString(), anyString())).thenReturn("optimized query");
        when(cacheService.getCachedResponse(anyString())).thenReturn(cachedResponse);
        
        // Act
        RagQueryResponse response = ragService.processRagQuery(testRequest);
        
        // Assert
        assertNotNull(response);
        assertEquals(cachedResponse.getResponse(), response.getResponse());
        
        // Verify no downstream processing occurred
        verify(vectorSearchService, never()).searchSimilarDocuments(anyString(), anyString(), anyInt());
        verify(llmIntegrationService, never()).generateResponse(anyString(), anyString());
    }

    /**
     * Test behavior when no relevant documents are found.
     */
    @Test
    void processRagQuery_NoDocumentsFound_ReturnsAppropriateResponse() {
        // Arrange
        when(queryOptimizationService.optimizeQuery(anyString(), anyString())).thenReturn("optimized query");
        when(cacheService.getCachedResponse(anyString())).thenReturn(null);
        when(vectorSearchService.searchSimilarDocuments(anyString(), anyString(), anyInt()))
            .thenReturn(Collections.emptyList());
        when(llmIntegrationService.generateResponse(anyString(), anyString()))
            .thenReturn("I don't have enough information to answer your question.");
        
        // Act
        RagQueryResponse response = ragService.processRagQuery(testRequest);
        
        // Assert
        assertNotNull(response);
        assertTrue(response.getResponse().contains("don't have enough information"));
        assertTrue(response.getSourceDocuments().isEmpty());
        
        verify(contextAssemblyService, never()).assembleContext(anyList(), anyString());
    }

    /**
     * Test error handling when LLM integration fails.
     */
    @Test
    void processRagQuery_LLMFailure_HandlesGracefully() {
        // Arrange
        when(queryOptimizationService.optimizeQuery(anyString(), anyString())).thenReturn("optimized query");
        when(cacheService.getCachedResponse(anyString())).thenReturn(null);
        when(vectorSearchService.searchSimilarDocuments(anyString(), anyString(), anyInt()))
            .thenReturn(mockChunks);
        when(contextAssemblyService.assembleContext(anyList(), anyString()))
            .thenReturn("assembled context");
        when(llmIntegrationService.generateResponse(anyString(), anyString()))
            .thenThrow(new RuntimeException("LLM service unavailable"));
        
        // Act & Assert
        assertThrows(RuntimeException.class, () -> ragService.processRagQuery(testRequest));
        
        // Verify partial processing occurred
        verify(vectorSearchService).searchSimilarDocuments(anyString(), anyString(), anyInt());
        verify(contextAssemblyService).assembleContext(eq(mockChunks), anyString());
    }

    /**
     * Test null request validation.
     */
    @Test
    void processRagQuery_NullRequest_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> ragService.processRagQuery(null));
        
        // Verify no processing occurred
        verifyNoInteractions(queryOptimizationService, vectorSearchService, llmIntegrationService);
    }

    /**
     * Test empty query validation.
     */
    @Test
    void processRagQuery_EmptyQuery_ThrowsException() {
        // Arrange
        testRequest.setQuery("");
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> ragService.processRagQuery(testRequest));
        
        // Verify no processing occurred
        verifyNoInteractions(queryOptimizationService, vectorSearchService, llmIntegrationService);
    }

    /**
     * Test missing tenant ID validation.
     */
    @Test
    void processRagQuery_MissingTenantId_ThrowsException() {
        // Arrange
        testRequest.setTenantId(null);
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> ragService.processRagQuery(testRequest));
        
        // Verify no processing occurred
        verifyNoInteractions(queryOptimizationService, vectorSearchService, llmIntegrationService);
    }

    /**
     * Test conversation context integration.
     */
    @Test
    void processRagQuery_WithConversationHistory_IncludesContext() {
        // Arrange
        testRequest.setIncludeConversationHistory(true);
        String conversationContext = "Previous conversation context...";
        String optimizedQuery = "optimized with context";
        
        when(conversationService.getConversationContext(anyString(), anyString()))
            .thenReturn(conversationContext);
        when(queryOptimizationService.optimizeQuery(anyString(), anyString()))
            .thenReturn(optimizedQuery);
        when(cacheService.getCachedResponse(anyString())).thenReturn(null);
        when(vectorSearchService.searchSimilarDocuments(anyString(), anyString(), anyInt()))
            .thenReturn(mockChunks);
        when(contextAssemblyService.assembleContext(anyList(), anyString()))
            .thenReturn("assembled context with history");
        when(llmIntegrationService.generateResponse(anyString(), anyString()))
            .thenReturn("Response with conversation context");
        
        // Act
        RagQueryResponse response = ragService.processRagQuery(testRequest);
        
        // Assert
        assertNotNull(response);
        verify(conversationService).getConversationContext(testRequest.getTenantId(), testRequest.getUserId());
        verify(conversationService).addToConversationHistory(
            eq(testRequest.getTenantId()), 
            eq(testRequest.getUserId()), 
            eq(testRequest.getQuery()), 
            eq(response.getResponse())
        );
    }
}
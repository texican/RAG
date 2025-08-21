package com.enterprise.rag.embedding;

import com.enterprise.rag.embedding.config.EmbeddingConfig.EmbeddingClientRegistry;
import com.enterprise.rag.embedding.dto.EmbeddingRequest;
import com.enterprise.rag.embedding.dto.EmbeddingResponse;
import com.enterprise.rag.embedding.service.EmbeddingCacheService;
import com.enterprise.rag.embedding.service.EmbeddingService;
import com.enterprise.rag.embedding.service.VectorStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.embedding.EmbeddingRequest as SpringEmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse as SpringEmbeddingResponse;
import org.springframework.ai.embedding.Embedding;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmbeddingService.
 */
@ExtendWith(MockitoExtension.class)
public class EmbeddingServiceTest {

    @Mock
    private EmbeddingClientRegistry clientRegistry;
    
    @Mock
    private EmbeddingCacheService cacheService;
    
    @Mock
    private VectorStorageService vectorStorageService;
    
    @Mock
    private EmbeddingClient embeddingClient;
    
    private EmbeddingService embeddingService;
    
    @BeforeEach
    void setUp() {
        embeddingService = new EmbeddingService(clientRegistry, cacheService, vectorStorageService);
    }
    
    @Test
    @DisplayName("Should generate embeddings successfully")
    void shouldGenerateEmbeddingsSuccessfully() {
        // Arrange
        UUID tenantId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        String text = "This is a test document";
        String modelName = "test-model";
        
        List<Float> mockEmbedding = List.of(0.1f, 0.2f, 0.3f, 0.4f);
        
        EmbeddingRequest request = EmbeddingRequest.singleText(
            tenantId, text, modelName, documentId, chunkId);
        
        // Mock dependencies
        when(clientRegistry.hasModel(modelName)).thenReturn(true);
        when(clientRegistry.getClient(modelName)).thenReturn(embeddingClient);
        when(clientRegistry.defaultModelName()).thenReturn("default-model");
        when(cacheService.getCachedEmbedding(any(), anyString(), anyString())).thenReturn(null);
        
        // Mock Spring AI response
        Embedding mockSpringEmbedding = new Embedding(mockEmbedding, 0);
        SpringEmbeddingResponse mockSpringResponse = new SpringEmbeddingResponse(
            List.of(mockSpringEmbedding));
        
        when(embeddingClient.call(any(SpringEmbeddingRequest.class)))
            .thenReturn(mockSpringResponse);
        
        // Act
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        
        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.status());
        assertEquals(tenantId, response.tenantId());
        assertEquals(documentId, response.documentId());
        assertEquals(1, response.embeddings().size());
        
        EmbeddingResponse.EmbeddingResult result = response.embeddings().get(0);
        assertEquals(chunkId, result.chunkId());
        assertEquals(text, result.text());
        assertEquals(mockEmbedding, result.embedding());
        assertEquals("SUCCESS", result.status());
        assertNull(result.error());
        
        // Verify interactions
        verify(cacheService).cacheEmbedding(tenantId, text, modelName, mockEmbedding);
        verify(vectorStorageService).storeVectors(eq(tenantId), eq(documentId), any(), eq(modelName));
    }
    
    @Test
    @DisplayName("Should use cached embeddings when available")
    void shouldUseCachedEmbeddingsWhenAvailable() {
        // Arrange
        UUID tenantId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        String text = "Cached test document";
        String modelName = "test-model";
        
        List<Float> cachedEmbedding = List.of(0.5f, 0.6f, 0.7f, 0.8f);
        
        EmbeddingRequest request = EmbeddingRequest.singleText(
            tenantId, text, modelName, documentId, chunkId);
        
        // Mock cached embedding
        when(clientRegistry.hasModel(modelName)).thenReturn(true);
        when(cacheService.getCachedEmbedding(tenantId, text, modelName))
            .thenReturn(cachedEmbedding);
        
        // Act
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        
        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.status());
        assertEquals(1, response.embeddings().size());
        
        EmbeddingResponse.EmbeddingResult result = response.embeddings().get(0);
        assertEquals(cachedEmbedding, result.embedding());
        
        // Verify that embedding client was not called
        verify(embeddingClient, never()).call(any(SpringEmbeddingRequest.class));
        verify(cacheService, never()).cacheEmbedding(any(), anyString(), anyString(), any());
    }
    
    @Test
    @DisplayName("Should handle batch embedding requests")
    void shouldHandleBatchEmbeddingRequests() {
        // Arrange
        UUID tenantId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        List<UUID> chunkIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        List<String> texts = List.of("First text", "Second text");
        String modelName = "test-model";
        
        List<Float> embedding1 = List.of(0.1f, 0.2f, 0.3f);
        List<Float> embedding2 = List.of(0.4f, 0.5f, 0.6f);
        
        EmbeddingRequest request = EmbeddingRequest.batchTexts(
            tenantId, texts, modelName, documentId, chunkIds);
        
        // Mock dependencies
        when(clientRegistry.hasModel(modelName)).thenReturn(true);
        when(clientRegistry.getClient(modelName)).thenReturn(embeddingClient);
        when(cacheService.getCachedEmbedding(any(), anyString(), anyString())).thenReturn(null);
        
        // Mock Spring AI response
        List<Embedding> mockEmbeddings = List.of(
            new Embedding(embedding1, 0),
            new Embedding(embedding2, 1)
        );
        SpringEmbeddingResponse mockSpringResponse = new SpringEmbeddingResponse(mockEmbeddings);
        
        when(embeddingClient.call(any(SpringEmbeddingRequest.class)))
            .thenReturn(mockSpringResponse);
        
        // Act
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        
        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.status());
        assertEquals(2, response.embeddings().size());
        
        // Verify first result
        EmbeddingResponse.EmbeddingResult result1 = response.embeddings().get(0);
        assertEquals(chunkIds.get(0), result1.chunkId());
        assertEquals(texts.get(0), result1.text());
        assertEquals(embedding1, result1.embedding());
        
        // Verify second result
        EmbeddingResponse.EmbeddingResult result2 = response.embeddings().get(1);
        assertEquals(chunkIds.get(1), result2.chunkId());
        assertEquals(texts.get(1), result2.text());
        assertEquals(embedding2, result2.embedding());
    }
    
    @Test
    @DisplayName("Should handle embedding generation failure")
    void shouldHandleEmbeddingGenerationFailure() {
        // Arrange
        UUID tenantId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        String text = "Test text";
        String modelName = "test-model";
        
        EmbeddingRequest request = EmbeddingRequest.singleText(
            tenantId, text, modelName, documentId, chunkId);
        
        // Mock dependencies
        when(clientRegistry.hasModel(modelName)).thenReturn(true);
        when(clientRegistry.getClient(modelName)).thenReturn(embeddingClient);
        when(cacheService.getCachedEmbedding(any(), anyString(), anyString())).thenReturn(null);
        
        // Mock failure
        when(embeddingClient.call(any(SpringEmbeddingRequest.class)))
            .thenThrow(new RuntimeException("API error"));
        
        // Act
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        
        // Assert
        assertNotNull(response);
        assertEquals("FAILED", response.status());
        assertEquals(1, response.embeddings().size());
        
        EmbeddingResponse.EmbeddingResult result = response.embeddings().get(0);
        assertEquals("FAILED", result.status());
        assertNotNull(result.error());
        assertNull(result.embedding());
    }
    
    @Test
    @DisplayName("Should use fallback model when requested model unavailable")
    void shouldUseFallbackModelWhenRequestedModelUnavailable() {
        // Arrange
        UUID tenantId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        String text = "Test text";
        String unavailableModel = "unavailable-model";
        String defaultModel = "default-model";
        
        List<Float> mockEmbedding = List.of(0.1f, 0.2f, 0.3f);
        
        EmbeddingRequest request = EmbeddingRequest.singleText(
            tenantId, text, unavailableModel, documentId, chunkId);
        
        // Mock dependencies
        when(clientRegistry.hasModel(unavailableModel)).thenReturn(false);
        when(clientRegistry.defaultModelName()).thenReturn(defaultModel);
        when(clientRegistry.getClient(defaultModel)).thenReturn(embeddingClient);
        when(cacheService.getCachedEmbedding(any(), anyString(), anyString())).thenReturn(null);
        
        // Mock Spring AI response
        Embedding mockSpringEmbedding = new Embedding(mockEmbedding, 0);
        SpringEmbeddingResponse mockSpringResponse = new SpringEmbeddingResponse(
            List.of(mockSpringEmbedding));
        
        when(embeddingClient.call(any(SpringEmbeddingRequest.class)))
            .thenReturn(mockSpringResponse);
        
        // Act
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        
        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.status());
        assertEquals(defaultModel, response.modelName());
        
        // Verify fallback model was used
        verify(clientRegistry).getClient(defaultModel);
        verify(cacheService).cacheEmbedding(tenantId, text, defaultModel, mockEmbedding);
    }
}
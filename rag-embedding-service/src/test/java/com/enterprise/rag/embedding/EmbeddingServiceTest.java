package com.enterprise.rag.embedding;

import com.enterprise.rag.embedding.config.EmbeddingConfig.EmbeddingModelRegistry;
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
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.Embedding;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmbeddingService.
 */
@ExtendWith(MockitoExtension.class)
public class EmbeddingServiceTest {

    @Mock
    private EmbeddingModelRegistry modelRegistry;
    
    @Mock
    private EmbeddingCacheService cacheService;
    
    @Mock
    private VectorStorageService vectorStorageService;
    
    @Mock
    private EmbeddingModel embeddingModel;
    
    private EmbeddingService embeddingService;
    
    @BeforeEach
    void setUp() {
        embeddingService = new EmbeddingService(modelRegistry, cacheService, vectorStorageService);
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
        
        EmbeddingRequest request = new EmbeddingRequest(
            tenantId, List.of(text), modelName, documentId, List.of(chunkId));
        
        // Mock dependencies
        when(modelRegistry.hasModel(modelName)).thenReturn(true);
        when(modelRegistry.getClient(modelName)).thenReturn(embeddingModel);
        when(cacheService.getCachedEmbedding(any(), anyString(), anyString())).thenReturn(null);
        
        // Mock Spring AI response - using mocks to avoid constructor issues
        Embedding mockSpringEmbedding = mock(Embedding.class);
        when(mockSpringEmbedding.getOutput()).thenReturn(mockEmbedding.stream().map(Float::doubleValue).toList());
        
        org.springframework.ai.embedding.EmbeddingResponse mockSpringResponse = 
            mock(org.springframework.ai.embedding.EmbeddingResponse.class);
        when(mockSpringResponse.getResults()).thenReturn(List.of(mockSpringEmbedding));
        
        when(embeddingModel.embedForResponse(any(List.class)))
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
        verify(vectorStorageService).storeEmbeddings(eq(tenantId), eq(modelName), any());
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
        
        EmbeddingRequest request = new EmbeddingRequest(
            tenantId, List.of(text), modelName, documentId, List.of(chunkId));
        
        // Mock cached embedding and model registry
        when(modelRegistry.hasModel(modelName)).thenReturn(true);
        when(modelRegistry.getClient(modelName)).thenReturn(embeddingModel);
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
        
        // Verify that embedding model was not called
        verify(embeddingModel, never()).embedForResponse(any(List.class));
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
        
        EmbeddingRequest request = new EmbeddingRequest(
            tenantId, texts, modelName, documentId, chunkIds);
        
        // Mock dependencies
        when(modelRegistry.hasModel(modelName)).thenReturn(true);
        when(modelRegistry.getClient(modelName)).thenReturn(embeddingModel);
        when(cacheService.getCachedEmbedding(any(), anyString(), anyString())).thenReturn(null);
        
        // Mock Spring AI response - using mocks
        Embedding mockSpringEmbedding1 = mock(Embedding.class);
        when(mockSpringEmbedding1.getOutput()).thenReturn(embedding1.stream().map(Float::doubleValue).toList());
        
        Embedding mockSpringEmbedding2 = mock(Embedding.class);
        when(mockSpringEmbedding2.getOutput()).thenReturn(embedding2.stream().map(Float::doubleValue).toList());
        
        org.springframework.ai.embedding.EmbeddingResponse mockSpringResponse = 
            mock(org.springframework.ai.embedding.EmbeddingResponse.class);
        when(mockSpringResponse.getResults()).thenReturn(List.of(mockSpringEmbedding1, mockSpringEmbedding2));
        
        when(embeddingModel.embedForResponse(any(List.class)))
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
        
        EmbeddingRequest request = new EmbeddingRequest(
            tenantId, List.of(text), modelName, documentId, List.of(chunkId));
        
        // Mock dependencies - ensure the model is not cached and available
        when(modelRegistry.hasModel(modelName)).thenReturn(true);
        when(modelRegistry.getClient(modelName)).thenReturn(embeddingModel);
        // Ensure no cached embedding so the service calls the embedding model
        when(cacheService.getCachedEmbedding(tenantId, text, modelName)).thenReturn(null);
        
        // Mock failure
        when(embeddingModel.embedForResponse(any(List.class)))
            .thenThrow(new RuntimeException("API error"));
        
        // Act
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        
        // Assert
        assertNotNull(response);
        assertEquals("FAILED", response.status());
        // When embedding generation fails at the service level, 
        // it returns a failure response with empty embeddings list
        assertEquals(0, response.embeddings().size());
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
        
        EmbeddingRequest request = new EmbeddingRequest(
            tenantId, List.of(text), unavailableModel, documentId, List.of(chunkId));
        
        // Mock dependencies
        when(modelRegistry.hasModel(unavailableModel)).thenReturn(false);
        when(modelRegistry.defaultModelName()).thenReturn(defaultModel);
        when(modelRegistry.getClient(defaultModel)).thenReturn(embeddingModel);
        when(cacheService.getCachedEmbedding(any(), anyString(), anyString())).thenReturn(null);
        
        // Mock Spring AI response - using mocks
        Embedding mockSpringEmbedding = mock(Embedding.class);
        when(mockSpringEmbedding.getOutput()).thenReturn(mockEmbedding.stream().map(Float::doubleValue).toList());
        
        org.springframework.ai.embedding.EmbeddingResponse mockSpringResponse = 
            mock(org.springframework.ai.embedding.EmbeddingResponse.class);
        when(mockSpringResponse.getResults()).thenReturn(List.of(mockSpringEmbedding));
        
        when(embeddingModel.embedForResponse(any(List.class)))
            .thenReturn(mockSpringResponse);
        
        // Act
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        
        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.status());
        assertEquals(defaultModel, response.modelName());
        
        // Verify fallback model was used
        verify(modelRegistry).getClient(defaultModel);
        verify(cacheService).cacheEmbedding(tenantId, text, defaultModel, mockEmbedding);
    }
}
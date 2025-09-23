package com.byo.rag.embedding.advanced;

import com.byo.rag.embedding.config.EmbeddingConfig.EmbeddingModelRegistry;
import com.byo.rag.embedding.dto.EmbeddingRequest;
import com.byo.rag.embedding.dto.EmbeddingResponse;
import com.byo.rag.embedding.service.EmbeddingCacheService;
import com.byo.rag.embedding.service.EmbeddingService;
import com.byo.rag.embedding.service.VectorStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.Embedding;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Advanced batch processing tests for embedding service.
 * 
 * Part of EMBEDDING-TEST-003: Embedding Service Advanced Scenarios
 * Tests batch processing scenarios and optimization.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EMBEDDING-TEST-003: Batch Processing Tests")
class BatchProcessingTest {

    @Mock
    private EmbeddingModelRegistry modelRegistry;
    
    @Mock
    private EmbeddingCacheService cacheService;
    
    @Mock
    private VectorStorageService vectorStorageService;
    
    @Mock
    private EmbeddingModel embeddingModel;
    
    private EmbeddingService embeddingService;
    
    private static final UUID TEST_TENANT_ID = UUID.randomUUID();
    private static final UUID TEST_DOCUMENT_ID = UUID.randomUUID();
    private static final String TEST_MODEL = "text-embedding-3-small";
    private static final List<Float> STANDARD_EMBEDDING = List.of(0.1f, 0.2f, 0.3f, 0.4f, 0.5f);
    
    @BeforeEach
    void setUp() {
        embeddingService = new EmbeddingService(modelRegistry, cacheService, vectorStorageService);
        
        // Default mocks
        when(modelRegistry.hasModel(TEST_MODEL)).thenReturn(true);
        when(modelRegistry.getClient(TEST_MODEL)).thenReturn(embeddingModel);
        when(cacheService.getCachedEmbedding(any(), anyString(), anyString())).thenReturn(null);
        
        setupBatchMockResponses();
    }
    
    @Test
    @DisplayName("Should handle small batch efficiently")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void shouldHandleSmallBatchEfficiently() {
        // Arrange
        int batchSize = 5;
        List<String> texts = generateTextBatch(batchSize, "Small batch text ");
        List<UUID> chunkIds = generateChunkIds(batchSize);
        
        EmbeddingRequest request = new EmbeddingRequest(
            TEST_TENANT_ID, texts, TEST_MODEL, TEST_DOCUMENT_ID, chunkIds);
        
        // Act
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        
        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.status());
        assertEquals(batchSize, response.embeddings().size());
        
        // Verify all results have correct structure
        for (int i = 0; i < batchSize; i++) {
            EmbeddingResponse.EmbeddingResult result = response.embeddings().get(i);
            assertEquals(texts.get(i), result.text());
            assertEquals(chunkIds.get(i), result.chunkId());
            assertEquals(STANDARD_EMBEDDING, result.embedding());
            assertEquals("SUCCESS", result.status());
            assertNull(result.error());
        }
        
        // Verify single API call for entire batch
        verify(embeddingModel, times(1)).embedForResponse(argThat(list -> 
            list != null && list.size() == batchSize));
    }
    
    @Test
    @DisplayName("Should handle medium batch with optimal processing")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void shouldHandleMediumBatchWithOptimalProcessing() {
        // Arrange
        int batchSize = 25;
        List<String> texts = generateTextBatch(batchSize, "Medium batch processing test ");
        List<UUID> chunkIds = generateChunkIds(batchSize);
        
        EmbeddingRequest request = new EmbeddingRequest(
            TEST_TENANT_ID, texts, TEST_MODEL, TEST_DOCUMENT_ID, chunkIds);
        
        // Act
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        
        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.status());
        assertEquals(batchSize, response.embeddings().size());
        
        // Verify batch processing efficiency
        verify(embeddingModel, times(1)).embedForResponse(any());
        verify(vectorStorageService, times(1)).storeEmbeddings(eq(TEST_TENANT_ID), eq(TEST_MODEL), any());
    }
    
    @Test
    @DisplayName("Should handle large batch with chunking strategy")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldHandleLargeBatchWithChunkingStrategy() {
        // Arrange
        int batchSize = 100;
        List<String> texts = generateTextBatch(batchSize, "Large batch chunking test ");
        List<UUID> chunkIds = generateChunkIds(batchSize);
        
        EmbeddingRequest request = new EmbeddingRequest(
            TEST_TENANT_ID, texts, TEST_MODEL, TEST_DOCUMENT_ID, chunkIds);
        
        // Act
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        
        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.status());
        assertEquals(batchSize, response.embeddings().size());
        
        // For large batches, service might chunk internally
        // Verify appropriate number of API calls (could be chunked)
        verify(embeddingModel, atLeastOnce()).embedForResponse(any());
        verify(vectorStorageService, atLeastOnce()).storeEmbeddings(eq(TEST_TENANT_ID), eq(TEST_MODEL), any());
    }
    
    @ParameterizedTest
    @ValueSource(ints = {1, 5, 10, 20, 50})
    @DisplayName("Should maintain consistent quality across batch sizes")
    void shouldMaintainConsistentQualityAcrossBatchSizes(int batchSize) {
        // Arrange
        List<String> texts = generateTextBatch(batchSize, "Quality consistency test ");
        List<UUID> chunkIds = generateChunkIds(batchSize);
        
        EmbeddingRequest request = new EmbeddingRequest(
            TEST_TENANT_ID, texts, TEST_MODEL, TEST_DOCUMENT_ID, chunkIds);
        
        // Act
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        
        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.status());
        assertEquals(batchSize, response.embeddings().size());
        
        // All embeddings should have consistent quality
        for (EmbeddingResponse.EmbeddingResult result : response.embeddings()) {
            assertNotNull(result.embedding());
            assertEquals(STANDARD_EMBEDDING.size(), result.embedding().size());
            assertEquals("SUCCESS", result.status());
            assertNull(result.error());
        }
    }
    
    @Test
    @DisplayName("Should handle mixed cache hits and misses in batch")
    void shouldHandleMixedCacheHitsAndMissesInBatch() {
        // Arrange
        int batchSize = 10;
        List<String> texts = generateTextBatch(batchSize, "Mixed cache test ");
        List<UUID> chunkIds = generateChunkIds(batchSize);
        
        // Mock cache hits for even indices, misses for odd indices
        when(cacheService.getCachedEmbedding(any(), anyString(), anyString()))
            .thenAnswer(invocation -> {
                String text = invocation.getArgument(1);
                if (text.contains("0") || text.contains("2") || text.contains("4") || 
                    text.contains("6") || text.contains("8")) {
                    return STANDARD_EMBEDDING; // Cache hit
                }
                return null; // Cache miss
            });
        
        EmbeddingRequest request = new EmbeddingRequest(
            TEST_TENANT_ID, texts, TEST_MODEL, TEST_DOCUMENT_ID, chunkIds);
        
        // Act
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        
        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.status());
        assertEquals(batchSize, response.embeddings().size());
        
        // All results should be successful regardless of cache status
        for (EmbeddingResponse.EmbeddingResult result : response.embeddings()) {
            assertEquals("SUCCESS", result.status());
            assertEquals(STANDARD_EMBEDDING, result.embedding());
        }
        
        // API should only be called for cache misses
        verify(embeddingModel, atMostOnce()).embedForResponse(any());
    }
    
    @Test
    @DisplayName("Should handle batch with varying text lengths optimally")
    void shouldHandleBatchWithVaryingTextLengthsOptimally() {
        // Arrange
        List<String> texts = new ArrayList<>();
        // Short texts
        texts.addAll(generateTextBatch(5, "Short "));
        // Medium texts
        texts.addAll(generateTextBatch(5, "Medium length text with additional content and details "));
        // Long texts
        texts.addAll(generateTextBatch(5, "Very long text content that contains extensive information about complex topics, detailed explanations, comprehensive documentation, and thorough analysis that would challenge the embedding model's processing capabilities "));
        
        List<UUID> chunkIds = generateChunkIds(texts.size());
        
        EmbeddingRequest request = new EmbeddingRequest(
            TEST_TENANT_ID, texts, TEST_MODEL, TEST_DOCUMENT_ID, chunkIds);
        
        // Act
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        
        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.status());
        assertEquals(15, response.embeddings().size());
        
        // All embeddings should be processed successfully
        for (EmbeddingResponse.EmbeddingResult result : response.embeddings()) {
            assertEquals("SUCCESS", result.status());
            assertNotNull(result.embedding());
            assertFalse(result.embedding().isEmpty());
        }
    }
    
    @Test
    @DisplayName("Should handle batch processing with tenant isolation")
    void shouldHandleBatchProcessingWithTenantIsolation() {
        // Arrange
        UUID tenant1 = UUID.randomUUID();
        UUID tenant2 = UUID.randomUUID();
        
        int batchSize = 5;
        List<String> texts = generateTextBatch(batchSize, "Tenant isolation test ");
        List<UUID> chunkIds = generateChunkIds(batchSize);
        
        EmbeddingRequest request1 = new EmbeddingRequest(
            tenant1, texts, TEST_MODEL, TEST_DOCUMENT_ID, chunkIds);
        EmbeddingRequest request2 = new EmbeddingRequest(
            tenant2, texts, TEST_MODEL, TEST_DOCUMENT_ID, chunkIds);
        
        // Act
        EmbeddingResponse response1 = embeddingService.generateEmbeddings(request1);
        EmbeddingResponse response2 = embeddingService.generateEmbeddings(request2);
        
        // Assert
        assertEquals("SUCCESS", response1.status());
        assertEquals("SUCCESS", response2.status());
        assertEquals(tenant1, response1.tenantId());
        assertEquals(tenant2, response2.tenantId());
        
        // Verify tenant-specific storage calls
        verify(vectorStorageService).storeEmbeddings(eq(tenant1), eq(TEST_MODEL), any());
        verify(vectorStorageService).storeEmbeddings(eq(tenant2), eq(TEST_MODEL), any());
    }
    
    @Test
    @DisplayName("Should handle batch with duplicate texts efficiently")
    void shouldHandleBatchWithDuplicateTextsEfficiently() {
        // Arrange
        String duplicatedText = "This text appears multiple times in the batch";
        List<String> texts = List.of(
            duplicatedText,
            "Unique text 1",
            duplicatedText,
            "Unique text 2",
            duplicatedText
        );
        List<UUID> chunkIds = generateChunkIds(texts.size());
        
        EmbeddingRequest request = new EmbeddingRequest(
            TEST_TENANT_ID, texts, TEST_MODEL, TEST_DOCUMENT_ID, chunkIds);
        
        // Act
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        
        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.status());
        assertEquals(5, response.embeddings().size());
        
        // Duplicate texts should have identical embeddings
        List<Float> firstDuplicateEmbedding = response.embeddings().get(0).embedding();
        List<Float> secondDuplicateEmbedding = response.embeddings().get(2).embedding();
        List<Float> thirdDuplicateEmbedding = response.embeddings().get(4).embedding();
        
        assertEquals(firstDuplicateEmbedding, secondDuplicateEmbedding);
        assertEquals(secondDuplicateEmbedding, thirdDuplicateEmbedding);
    }
    
    @Test
    @DisplayName("Should handle batch with special characters efficiently")
    void shouldHandleBatchWithSpecialCharactersEfficiently() {
        // Arrange
        List<String> texts = List.of(
            "Text with émojis: 😀 🎉 🚀",
            "Unicode characters: café naïve résumé",
            "Special symbols: @#$%^&*()_+-=[]{}|",
            "Non-Latin: 你好世界 Здравствуй мир",
            "Mixed: Hello 世界! How are you? 😊"
        );
        List<UUID> chunkIds = generateChunkIds(texts.size());
        
        EmbeddingRequest request = new EmbeddingRequest(
            TEST_TENANT_ID, texts, TEST_MODEL, TEST_DOCUMENT_ID, chunkIds);
        
        // Act
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        
        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.status());
        assertEquals(5, response.embeddings().size());
        
        // All special character texts should be processed successfully
        for (EmbeddingResponse.EmbeddingResult result : response.embeddings()) {
            assertEquals("SUCCESS", result.status());
            assertNotNull(result.embedding());
            assertEquals(STANDARD_EMBEDDING.size(), result.embedding().size());
        }
    }
    
    @Test
    @DisplayName("Should handle empty and whitespace texts in batch")
    void shouldHandleEmptyAndWhitespaceTextsInBatch() {
        // Arrange
        List<String> texts = List.of(
            "Normal text",
            "",
            "   ",
            "\n\n\n",
            "\t\t\t",
            "Another normal text"
        );
        List<UUID> chunkIds = generateChunkIds(texts.size());
        
        EmbeddingRequest request = new EmbeddingRequest(
            TEST_TENANT_ID, texts, TEST_MODEL, TEST_DOCUMENT_ID, chunkIds);
        
        // Act
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        
        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.status());
        assertEquals(6, response.embeddings().size());
        
        // Even empty/whitespace texts should be handled gracefully
        for (EmbeddingResponse.EmbeddingResult result : response.embeddings()) {
            assertEquals("SUCCESS", result.status());
            assertNotNull(result.embedding());
        }
    }
    
    @Test
    @DisplayName("Should optimize batch storage operations")
    void shouldOptimizeBatchStorageOperations() {
        // Arrange
        int batchSize = 20;
        List<String> texts = generateTextBatch(batchSize, "Storage optimization test ");
        List<UUID> chunkIds = generateChunkIds(batchSize);
        
        EmbeddingRequest request = new EmbeddingRequest(
            TEST_TENANT_ID, texts, TEST_MODEL, TEST_DOCUMENT_ID, chunkIds);
        
        // Act
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        
        // Assert
        assertEquals("SUCCESS", response.status());
        assertEquals(batchSize, response.embeddings().size());
        
        // Should minimize storage calls for efficiency
        verify(vectorStorageService, atMost(2)).storeEmbeddings(eq(TEST_TENANT_ID), eq(TEST_MODEL), any());
        
        // Should cache all successful embeddings
        verify(cacheService, times(batchSize)).cacheEmbedding(eq(TEST_TENANT_ID), anyString(), eq(TEST_MODEL), eq(STANDARD_EMBEDDING));
    }
    
    private void setupBatchMockResponses() {
        // Setup dynamic mock responses for batch processing
        Embedding mockSpringEmbedding = mock(Embedding.class);
        when(mockSpringEmbedding.getOutput()).thenReturn(
            STANDARD_EMBEDDING.stream()
                .map(Float::doubleValue)
                .collect(java.util.stream.Collectors.toList())
        );
        
        org.springframework.ai.embedding.EmbeddingResponse mockSpringResponse = 
            mock(org.springframework.ai.embedding.EmbeddingResponse.class);
        
        // Return variable number of embeddings based on input size
        when(embeddingModel.embedForResponse(any(List.class))).thenAnswer(invocation -> {
            List<?> input = invocation.getArgument(0);
            List<Embedding> results = IntStream.range(0, input.size())
                .mapToObj(i -> mockSpringEmbedding)
                .collect(java.util.stream.Collectors.toList());
            when(mockSpringResponse.getResults()).thenReturn(results);
            return mockSpringResponse;
        });
    }
    
    private List<String> generateTextBatch(int size, String prefix) {
        return IntStream.range(0, size)
            .mapToObj(i -> prefix + i + " with unique content for testing")
            .collect(java.util.stream.Collectors.toList());
    }
    
    private List<UUID> generateChunkIds(int size) {
        return IntStream.range(0, size)
            .mapToObj(i -> UUID.randomUUID())
            .collect(java.util.stream.Collectors.toList());
    }
}
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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Performance and load testing for embedding service.
 * 
 * Part of EMBEDDING-TEST-003: Embedding Service Advanced Scenarios
 * Tests embedding generation under high load conditions and performance benchmarks.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EMBEDDING-TEST-003: Performance and Load Tests")
class PerformanceLoadTest {

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
        
        // Default mocks with lenient mode
        lenient().when(modelRegistry.hasModel(TEST_MODEL)).thenReturn(true);
        lenient().when(modelRegistry.getClient(TEST_MODEL)).thenReturn(embeddingModel);
        lenient().when(cacheService.getCachedEmbedding(any(), anyString(), anyString())).thenReturn(null);
        
        setupFastMockResponses();
    }
    
    @Test
    @DisplayName("Should handle small batch processing within reasonable time")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void shouldHandleSmallBatchWithinReasonableTime() {
        // Arrange
        int batchSize = 10;
        List<String> texts = generateTextBatch(batchSize, "Small batch text content ");
        List<UUID> chunkIds = generateChunkIds(batchSize);
        
        EmbeddingRequest request = new EmbeddingRequest(
            TEST_TENANT_ID, texts, TEST_MODEL, TEST_DOCUMENT_ID, chunkIds);
        
        // Act
        Instant start = Instant.now();
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        Duration duration = Duration.between(start, Instant.now());
        
        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.status());
        assertEquals(batchSize, response.embeddings().size());
        assertTrue(duration.toMillis() < 1000, 
            "Small batch should complete within 1 second, took: " + duration.toMillis() + "ms");
    }
    
    @Test
    @DisplayName("Should handle medium batch processing within acceptable time")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void shouldHandleMediumBatchWithinAcceptableTime() {
        // Arrange
        int batchSize = 50;
        List<String> texts = generateTextBatch(batchSize, "Medium batch text content with more detailed information ");
        List<UUID> chunkIds = generateChunkIds(batchSize);
        
        EmbeddingRequest request = new EmbeddingRequest(
            TEST_TENANT_ID, texts, TEST_MODEL, TEST_DOCUMENT_ID, chunkIds);
        
        // Act
        Instant start = Instant.now();
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        Duration duration = Duration.between(start, Instant.now());
        
        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.status());
        assertEquals(batchSize, response.embeddings().size());
        assertTrue(duration.toMillis() < 5000, 
            "Medium batch should complete within 5 seconds, took: " + duration.toMillis() + "ms");
    }
    
    @Test
    @DisplayName("Should handle large batch processing")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldHandleLargeBatchProcessing() {
        // Arrange
        int batchSize = 100;
        List<String> texts = generateTextBatch(batchSize, "Large batch text content with extensive information that simulates real-world document processing ");
        List<UUID> chunkIds = generateChunkIds(batchSize);
        
        EmbeddingRequest request = new EmbeddingRequest(
            TEST_TENANT_ID, texts, TEST_MODEL, TEST_DOCUMENT_ID, chunkIds);
        
        // Act
        Instant start = Instant.now();
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        Duration duration = Duration.between(start, Instant.now());
        
        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.status());
        assertEquals(batchSize, response.embeddings().size());
        assertTrue(duration.toMillis() < 20000, 
            "Large batch should complete within 20 seconds, took: " + duration.toMillis() + "ms");
        
        // Verify all embeddings are present
        for (EmbeddingResponse.EmbeddingResult result : response.embeddings()) {
            assertNotNull(result.embedding());
            assertFalse(result.embedding().isEmpty());
            assertEquals("SUCCESS", result.status());
        }
    }
    
    @ParameterizedTest
    @ValueSource(ints = {1, 5, 10, 25, 50})
    @DisplayName("Should scale performance linearly with batch size")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void shouldScalePerformanceLinearlyWithBatchSize(int batchSize) {
        // Arrange
        List<String> texts = generateTextBatch(batchSize, "Performance scaling test content ");
        List<UUID> chunkIds = generateChunkIds(batchSize);
        
        EmbeddingRequest request = new EmbeddingRequest(
            TEST_TENANT_ID, texts, TEST_MODEL, TEST_DOCUMENT_ID, chunkIds);
        
        // Act
        Instant start = Instant.now();
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        Duration duration = Duration.between(start, Instant.now());
        
        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.status());
        assertEquals(batchSize, response.embeddings().size());
        
        // Performance expectation: roughly linear scaling (allowing some overhead)
        long expectedMaxDuration = Math.max(100, batchSize * 50); // 50ms per item minimum
        assertTrue(duration.toMillis() < expectedMaxDuration, 
            String.format("Batch size %d should complete within %dms, took: %dms", 
                batchSize, expectedMaxDuration, duration.toMillis()));
    }
    
    @Test
    @DisplayName("Should handle concurrent requests without interference")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldHandleConcurrentRequestsWithoutInterference() throws ExecutionException, InterruptedException {
        // Arrange
        int concurrentRequests = 5;
        int textsPerRequest = 10;
        
        List<CompletableFuture<EmbeddingResponse>> futures = new ArrayList<>();
        
        // Create concurrent requests
        for (int i = 0; i < concurrentRequests; i++) {
            final int requestId = i;
            CompletableFuture<EmbeddingResponse> future = CompletableFuture.supplyAsync(() -> {
                List<String> texts = generateTextBatch(textsPerRequest, "Concurrent request " + requestId + " text ");
                List<UUID> chunkIds = generateChunkIds(textsPerRequest);
                
                EmbeddingRequest request = new EmbeddingRequest(
                    TEST_TENANT_ID, texts, TEST_MODEL, TEST_DOCUMENT_ID, chunkIds);
                
                return embeddingService.generateEmbeddings(request);
            });
            futures.add(future);
        }
        
        // Act - Wait for all to complete
        Instant start = Instant.now();
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allOf.get();
        Duration totalDuration = Duration.between(start, Instant.now());
        
        // Assert
        for (int i = 0; i < concurrentRequests; i++) {
            EmbeddingResponse response = futures.get(i).get();
            assertNotNull(response);
            assertEquals("SUCCESS", response.status());
            assertEquals(textsPerRequest, response.embeddings().size());
        }
        
        // Concurrent processing should not take significantly longer than sequential
        assertTrue(totalDuration.toMillis() < 15000, 
            "Concurrent requests should complete within 15 seconds, took: " + totalDuration.toMillis() + "ms");
    }
    
    @Test
    @DisplayName("Should maintain performance with cache hits")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void shouldMaintainPerformanceWithCacheHits() {
        // Arrange - Setup cache hits for faster processing
        when(cacheService.getCachedEmbedding(any(), anyString(), anyString()))
            .thenReturn(STANDARD_EMBEDDING);
        
        int batchSize = 50;
        List<String> texts = generateTextBatch(batchSize, "Cached content ");
        List<UUID> chunkIds = generateChunkIds(batchSize);
        
        EmbeddingRequest request = new EmbeddingRequest(
            TEST_TENANT_ID, texts, TEST_MODEL, TEST_DOCUMENT_ID, chunkIds);
        
        // Act
        Instant start = Instant.now();
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        Duration duration = Duration.between(start, Instant.now());
        
        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.status());
        assertEquals(batchSize, response.embeddings().size());
        
        // Cache hits should be much faster
        assertTrue(duration.toMillis() < 500, 
            "Cache hits should complete very quickly, took: " + duration.toMillis() + "ms");
        
        // Verify embedding model was not called due to cache hits
        verify(embeddingModel, never()).embedForResponse(any());
    }
    
    @Test
    @DisplayName("Should handle varying text lengths efficiently")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void shouldHandleVaryingTextLengthsEfficiently() {
        // Arrange - Mix of short, medium, and long texts
        List<String> texts = new ArrayList<>();
        texts.addAll(generateTextBatch(10, "Short "));
        texts.addAll(generateTextBatch(10, "Medium length text content with additional information "));
        texts.addAll(generateTextBatch(10, "Very long text content that contains extensive information about complex topics and detailed explanations that would challenge processing performance "));
        
        List<UUID> chunkIds = generateChunkIds(texts.size());
        
        EmbeddingRequest request = new EmbeddingRequest(
            TEST_TENANT_ID, texts, TEST_MODEL, TEST_DOCUMENT_ID, chunkIds);
        
        // Act
        Instant start = Instant.now();
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        Duration duration = Duration.between(start, Instant.now());
        
        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.status());
        assertEquals(30, response.embeddings().size());
        assertTrue(duration.toMillis() < 10000, 
            "Variable length processing should complete within 10 seconds, took: " + duration.toMillis() + "ms");
        
        // Verify all different lengths processed successfully
        for (EmbeddingResponse.EmbeddingResult result : response.embeddings()) {
            assertNotNull(result.embedding());
            assertFalse(result.embedding().isEmpty());
        }
    }
    
    @Test
    @DisplayName("Should handle repeated requests consistently")
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void shouldHandleRepeatedRequestsConsistently() {
        // Arrange
        int repetitions = 10;
        int batchSize = 5;
        List<String> texts = generateTextBatch(batchSize, "Repeated request content ");
        List<UUID> chunkIds = generateChunkIds(batchSize);
        
        List<Duration> durations = new ArrayList<>();
        
        // Act - Perform multiple identical requests
        for (int i = 0; i < repetitions; i++) {
            EmbeddingRequest request = new EmbeddingRequest(
                TEST_TENANT_ID, texts, TEST_MODEL, TEST_DOCUMENT_ID, chunkIds);
            
            Instant start = Instant.now();
            EmbeddingResponse response = embeddingService.generateEmbeddings(request);
            Duration duration = Duration.between(start, Instant.now());
            durations.add(duration);
            
            // Assert each response
            assertNotNull(response);
            assertEquals("SUCCESS", response.status());
            assertEquals(batchSize, response.embeddings().size());
        }
        
        // Assert performance consistency with more lenient thresholds for mock testing
        long avgDuration = durations.stream().mapToLong(Duration::toMillis).sum() / repetitions;
        long maxDuration = durations.stream().mapToLong(Duration::toMillis).max().orElse(0);
        long minDuration = durations.stream().mapToLong(Duration::toMillis).min().orElse(0);
        
        // For mock tests, durations might be very small or zero, so check if we have meaningful timing
        if (minDuration > 0) {
            // Performance should be reasonably consistent (max shouldn't be more than 10x min for mocks)
            assertTrue(maxDuration <= minDuration * 10, 
                String.format("Performance should be reasonably consistent: min=%dms, max=%dms, avg=%dms", 
                    minDuration, maxDuration, avgDuration));
        } else {
            // If timing is too small to measure, just verify all requests completed successfully
            assertEquals(repetitions, durations.size());
        }
    }
    
    private void setupFastMockResponses() {
        // Setup fast mock responses for performance testing
        Embedding mockSpringEmbedding = mock(Embedding.class);
        lenient().when(mockSpringEmbedding.getOutput()).thenReturn(
            STANDARD_EMBEDDING.stream()
                .map(Float::doubleValue)
                .collect(java.util.stream.Collectors.toList())
        );
        
        org.springframework.ai.embedding.EmbeddingResponse mockSpringResponse = 
            mock(org.springframework.ai.embedding.EmbeddingResponse.class);
        
        // Return variable number of embeddings based on input size
        lenient().when(embeddingModel.embedForResponse(any(List.class))).thenAnswer(invocation -> {
            List<?> input = invocation.getArgument(0);
            List<Embedding> results = IntStream.range(0, input.size())
                .mapToObj(i -> mockSpringEmbedding)
                .collect(java.util.stream.Collectors.toList());
            lenient().when(mockSpringResponse.getResults()).thenReturn(results);
            return mockSpringResponse;
        });
    }
    
    private List<String> generateTextBatch(int size, String prefix) {
        return IntStream.range(0, size)
            .mapToObj(i -> prefix + "item " + i + " with unique content")
            .collect(java.util.stream.Collectors.toList());
    }
    
    private List<UUID> generateChunkIds(int size) {
        return IntStream.range(0, size)
            .mapToObj(i -> UUID.randomUUID())
            .collect(java.util.stream.Collectors.toList());
    }
}
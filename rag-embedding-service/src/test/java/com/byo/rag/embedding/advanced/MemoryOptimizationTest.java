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

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Memory usage and optimization tests for embedding service.
 * 
 * Part of EMBEDDING-TEST-003: Embedding Service Advanced Scenarios
 * Tests memory usage patterns and optimization strategies.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EMBEDDING-TEST-003: Memory Usage and Optimization Tests")
class MemoryOptimizationTest {

    @Mock
    private EmbeddingModelRegistry modelRegistry;
    
    @Mock
    private EmbeddingCacheService cacheService;
    
    @Mock
    private VectorStorageService vectorStorageService;
    
    @Mock
    private EmbeddingModel embeddingModel;
    
    private EmbeddingService embeddingService;
    private MemoryMXBean memoryBean;
    
    private static final UUID TEST_TENANT_ID = UUID.randomUUID();
    private static final UUID TEST_DOCUMENT_ID = UUID.randomUUID();
    private static final String TEST_MODEL = "text-embedding-3-small";
    private static final List<Float> STANDARD_EMBEDDING = List.of(0.1f, 0.2f, 0.3f, 0.4f, 0.5f);
    
    @BeforeEach
    void setUp() {
        embeddingService = new EmbeddingService(modelRegistry, cacheService, vectorStorageService);
        memoryBean = ManagementFactory.getMemoryMXBean();
        
        // Default mocks with lenient mode
        lenient().when(modelRegistry.hasModel(TEST_MODEL)).thenReturn(true);
        lenient().when(modelRegistry.getClient(TEST_MODEL)).thenReturn(embeddingModel);
        lenient().when(cacheService.getCachedEmbedding(any(), anyString(), anyString())).thenReturn(null);
        
        setupMemoryEfficientMockResponses();
    }
    
    @Test
    @DisplayName("Should maintain stable memory usage for small batches")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void shouldMaintainStableMemoryUsageForSmallBatches() {
        // Arrange
        int batchSize = 10;
        List<String> texts = generateTextBatch(batchSize, "Memory test small batch ");
        List<UUID> chunkIds = generateChunkIds(batchSize);
        
        long initialMemory = getCurrentMemoryUsage();
        
        // Act - Process multiple small batches
        for (int i = 0; i < 10; i++) {
            EmbeddingRequest request = new EmbeddingRequest(
                TEST_TENANT_ID, texts, TEST_MODEL, TEST_DOCUMENT_ID, chunkIds);
            
            EmbeddingResponse response = embeddingService.generateEmbeddings(request);
            
            // Assert each response
            assertEquals("SUCCESS", response.status());
            assertEquals(batchSize, response.embeddings().size());
        }
        
        // Force garbage collection and measure memory
        System.gc();
        Thread.yield();
        long finalMemory = getCurrentMemoryUsage();
        
        // Memory usage should not grow significantly (allowing for some variance)
        long memoryIncrease = finalMemory - initialMemory;
        assertTrue(memoryIncrease < 50_000_000, // 50MB threshold
            String.format("Memory usage increased too much: %d bytes", memoryIncrease));
    }
    
    @Test
    @DisplayName("Should handle large embedding dimensions efficiently")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void shouldHandleLargeEmbeddingDimensionsEfficiently() {
        // Arrange - Simulate large embedding dimensions (1536D like OpenAI ada-002)
        int dimension = 1536;
        List<Float> largeEmbedding = generateLargeEmbedding(dimension);
        
        String text = "Large dimension embedding test";
        UUID chunkId = UUID.randomUUID();
        
        EmbeddingRequest request = new EmbeddingRequest(
            TEST_TENANT_ID, List.of(text), TEST_MODEL, TEST_DOCUMENT_ID, List.of(chunkId));
        
        mockLargeDimensionEmbedding(largeEmbedding);
        
        long initialMemory = getCurrentMemoryUsage();
        
        // Act - Process multiple large embeddings
        for (int i = 0; i < 20; i++) {
            EmbeddingResponse response = embeddingService.generateEmbeddings(request);
            
            assertEquals("SUCCESS", response.status());
            assertEquals(1, response.embeddings().size());
            assertEquals(dimension, response.embeddings().get(0).embedding().size());
        }
        
        System.gc();
        long finalMemory = getCurrentMemoryUsage();
        
        // Assert reasonable memory usage for large embeddings
        long memoryIncrease = finalMemory - initialMemory;
        assertTrue(memoryIncrease < 100_000_000, // 100MB threshold for large embeddings
            String.format("Large embedding memory usage too high: %d bytes", memoryIncrease));
    }
    
    @ParameterizedTest
    @ValueSource(ints = {10, 25, 50, 100})
    @DisplayName("Should scale memory linearly with batch size")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldScaleMemoryLinearlyWithBatchSize(int batchSize) {
        // Arrange
        List<String> texts = generateTextBatch(batchSize, "Memory scaling test ");
        List<UUID> chunkIds = generateChunkIds(batchSize);
        
        EmbeddingRequest request = new EmbeddingRequest(
            TEST_TENANT_ID, texts, TEST_MODEL, TEST_DOCUMENT_ID, chunkIds);
        
        long initialMemory = getCurrentMemoryUsage();
        
        // Act
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        
        System.gc();
        long finalMemory = getCurrentMemoryUsage();
        
        // Assert
        assertEquals("SUCCESS", response.status());
        assertEquals(batchSize, response.embeddings().size());
        
        // Memory usage should scale reasonably with batch size
        long memoryIncrease = finalMemory - initialMemory;
        long memoryPerItem = memoryIncrease / batchSize;
        
        // Each item should not use more than 1MB of memory (very generous threshold)
        assertTrue(memoryPerItem < 1_000_000, 
            String.format("Memory per item too high for batch size %d: %d bytes per item", 
                batchSize, memoryPerItem));
    }
    
    @Test
    @DisplayName("Should optimize memory with cache usage")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void shouldOptimizeMemoryWithCacheUsage() {
        // Arrange
        int batchSize = 20;
        List<String> texts = generateTextBatch(batchSize, "Cache optimization test ");
        List<UUID> chunkIds = generateChunkIds(batchSize);
        
        // First request - no cache hits
        when(cacheService.getCachedEmbedding(any(), anyString(), anyString())).thenReturn(null);
        
        EmbeddingRequest request = new EmbeddingRequest(
            TEST_TENANT_ID, texts, TEST_MODEL, TEST_DOCUMENT_ID, chunkIds);
        
        long initialMemory = getCurrentMemoryUsage();
        
        // Act - First request (cache misses)
        EmbeddingResponse response1 = embeddingService.generateEmbeddings(request);
        
        long afterFirstRequest = getCurrentMemoryUsage();
        
        // Setup cache hits for second request
        when(cacheService.getCachedEmbedding(any(), anyString(), anyString()))
            .thenReturn(STANDARD_EMBEDDING);
        
        // Act - Second request (cache hits)
        EmbeddingResponse response2 = embeddingService.generateEmbeddings(request);
        
        System.gc();
        long finalMemory = getCurrentMemoryUsage();
        
        // Assert
        assertEquals("SUCCESS", response1.status());
        assertEquals("SUCCESS", response2.status());
        
        // Cache hits should use less memory than cache misses
        long firstRequestMemory = afterFirstRequest - initialMemory;
        long secondRequestMemory = finalMemory - afterFirstRequest;
        
        assertTrue(secondRequestMemory <= firstRequestMemory, 
            String.format("Cache hits should use less memory: first=%d, second=%d", 
                firstRequestMemory, secondRequestMemory));
        
        // Verify no API calls for cached responses
        verify(embeddingModel, times(1)).embedForResponse(any());
    }
    
    @Test
    @DisplayName("Should handle memory pressure gracefully")
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void shouldHandleMemoryPressureGracefully() {
        // Arrange - Simulate memory pressure with large batches
        int largeBatchSize = 200;
        List<String> largeTexts = generateLargeTextBatch(largeBatchSize, 1000); // 1KB per text
        List<UUID> chunkIds = generateChunkIds(largeBatchSize);
        
        EmbeddingRequest request = new EmbeddingRequest(
            TEST_TENANT_ID, largeTexts, TEST_MODEL, TEST_DOCUMENT_ID, chunkIds);
        
        long initialMemory = getCurrentMemoryUsage();
        
        // Act
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        
        System.gc();
        long finalMemory = getCurrentMemoryUsage();
        MemoryUsage heapAfter = memoryBean.getHeapMemoryUsage();
        
        // Assert
        assertEquals("SUCCESS", response.status());
        assertEquals(largeBatchSize, response.embeddings().size());
        
        // Should not exceed available heap space
        assertTrue(heapAfter.getUsed() < heapAfter.getMax() * 0.8, 
            "Memory usage should stay below 80% of max heap");
        
        // Memory increase should be reasonable
        long memoryIncrease = finalMemory - initialMemory;
        assertTrue(memoryIncrease < 500_000_000, // 500MB threshold
            String.format("Memory increase too high under pressure: %d bytes", memoryIncrease));
    }
    
    @Test
    @DisplayName("Should clean up resources after processing")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void shouldCleanUpResourcesAfterProcessing() {
        // Arrange
        int iterations = 50;
        int batchSize = 10;
        
        long initialMemory = getCurrentMemoryUsage();
        
        // Act - Process many small batches
        for (int i = 0; i < iterations; i++) {
            List<String> texts = generateTextBatch(batchSize, "Cleanup test " + i + " ");
            List<UUID> chunkIds = generateChunkIds(batchSize);
            
            EmbeddingRequest request = new EmbeddingRequest(
                TEST_TENANT_ID, texts, TEST_MODEL, TEST_DOCUMENT_ID, chunkIds);
            
            EmbeddingResponse response = embeddingService.generateEmbeddings(request);
            assertEquals("SUCCESS", response.status());
            
            // Periodic garbage collection
            if (i % 10 == 0) {
                System.gc();
                Thread.yield();
            }
        }
        
        // Force final cleanup
        System.gc();
        Thread.yield();
        System.gc();
        
        long finalMemory = getCurrentMemoryUsage();
        
        // Assert memory is cleaned up properly
        long memoryIncrease = finalMemory - initialMemory;
        assertTrue(memoryIncrease < 30_000_000, // 30MB threshold for cleanup
            String.format("Memory not cleaned up properly: %d bytes remain", memoryIncrease));
    }
    
    @Test
    @DisplayName("Should optimize memory for repeated identical requests")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void shouldOptimizeMemoryForRepeatedIdenticalRequests() {
        // Arrange
        String repeatedText = "This text is used repeatedly for memory optimization testing";
        UUID chunkId = UUID.randomUUID();
        
        EmbeddingRequest request = new EmbeddingRequest(
            TEST_TENANT_ID, List.of(repeatedText), TEST_MODEL, TEST_DOCUMENT_ID, List.of(chunkId));
        
        long initialMemory = getCurrentMemoryUsage();
        
        // Act - Repeat same request many times
        for (int i = 0; i < 100; i++) {
            EmbeddingResponse response = embeddingService.generateEmbeddings(request);
            assertEquals("SUCCESS", response.status());
        }
        
        System.gc();
        long finalMemory = getCurrentMemoryUsage();
        
        // Assert minimal memory increase for repeated requests
        long memoryIncrease = finalMemory - initialMemory;
        assertTrue(memoryIncrease < 20_000_000, // 20MB threshold for repeated requests
            String.format("Memory not optimized for repeated requests: %d bytes", memoryIncrease));
    }
    
    @Test
    @DisplayName("Should handle concurrent memory usage efficiently")
    @Timeout(value = 25, unit = TimeUnit.SECONDS)
    void shouldHandleConcurrentMemoryUsageEfficiently() {
        // Arrange
        int threadCount = 5;
        int requestsPerThread = 10;
        int batchSize = 5;
        
        long initialMemory = getCurrentMemoryUsage();
        
        // Act - Simulate concurrent usage
        List<Thread> threads = new ArrayList<>();
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            Thread thread = new Thread(() -> {
                for (int i = 0; i < requestsPerThread; i++) {
                    List<String> texts = generateTextBatch(batchSize, "Concurrent test T" + threadId + "R" + i + " ");
                    List<UUID> chunkIds = generateChunkIds(batchSize);
                    
                    EmbeddingRequest request = new EmbeddingRequest(
                        TEST_TENANT_ID, texts, TEST_MODEL, TEST_DOCUMENT_ID, chunkIds);
                    
                    EmbeddingResponse response = embeddingService.generateEmbeddings(request);
                    assertEquals("SUCCESS", response.status());
                }
            });
            threads.add(thread);
            thread.start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("Thread interrupted");
            }
        }
        
        System.gc();
        long finalMemory = getCurrentMemoryUsage();
        
        // Assert reasonable memory usage for concurrent processing
        long memoryIncrease = finalMemory - initialMemory;
        assertTrue(memoryIncrease < 100_000_000, // 100MB threshold for concurrent usage
            String.format("Concurrent memory usage too high: %d bytes", memoryIncrease));
    }
    
    private void setupMemoryEfficientMockResponses() {
        // Setup lightweight mock responses for memory testing
        Embedding mockSpringEmbedding = mock(Embedding.class);
        lenient().when(mockSpringEmbedding.getOutput()).thenReturn(
            STANDARD_EMBEDDING.stream()
                .map(Float::doubleValue)
                .collect(java.util.stream.Collectors.toList())
        );
        
        org.springframework.ai.embedding.EmbeddingResponse mockSpringResponse = 
            mock(org.springframework.ai.embedding.EmbeddingResponse.class);
        
        lenient().when(embeddingModel.embedForResponse(any(List.class))).thenAnswer(invocation -> {
            List<?> input = invocation.getArgument(0);
            List<Embedding> results = IntStream.range(0, input.size())
                .mapToObj(i -> mockSpringEmbedding)
                .collect(java.util.stream.Collectors.toList());
            lenient().when(mockSpringResponse.getResults()).thenReturn(results);
            return mockSpringResponse;
        });
    }
    
    private void mockLargeDimensionEmbedding(List<Float> largeEmbedding) {
        Embedding mockSpringEmbedding = mock(Embedding.class);
        lenient().when(mockSpringEmbedding.getOutput()).thenReturn(
            largeEmbedding.stream()
                .map(Float::doubleValue)
                .collect(java.util.stream.Collectors.toList())
        );
        
        org.springframework.ai.embedding.EmbeddingResponse mockSpringResponse = 
            mock(org.springframework.ai.embedding.EmbeddingResponse.class);
        lenient().when(mockSpringResponse.getResults()).thenReturn(List.of(mockSpringEmbedding));
        
        lenient().when(embeddingModel.embedForResponse(any(List.class)))
            .thenReturn(mockSpringResponse);
    }
    
    private List<Float> generateLargeEmbedding(int dimension) {
        return IntStream.range(0, dimension)
            .mapToObj(i -> (float) (Math.sin(i * 0.01) * 0.5))
            .collect(java.util.stream.Collectors.toList());
    }
    
    private List<String> generateTextBatch(int size, String prefix) {
        return IntStream.range(0, size)
            .mapToObj(i -> prefix + i + " content")
            .collect(java.util.stream.Collectors.toList());
    }
    
    private List<String> generateLargeTextBatch(int size, int textSizeBytes) {
        String baseText = "a".repeat(textSizeBytes);
        return IntStream.range(0, size)
            .mapToObj(i -> "Large text " + i + " " + baseText)
            .collect(java.util.stream.Collectors.toList());
    }
    
    private List<UUID> generateChunkIds(int size) {
        return IntStream.range(0, size)
            .mapToObj(i -> UUID.randomUUID())
            .collect(java.util.stream.Collectors.toList());
    }
    
    private long getCurrentMemoryUsage() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        return heapUsage.getUsed();
    }
}
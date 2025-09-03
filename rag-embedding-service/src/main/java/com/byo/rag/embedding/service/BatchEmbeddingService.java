package com.byo.rag.embedding.service;

import com.byo.rag.embedding.dto.EmbeddingRequest;
import com.byo.rag.embedding.dto.EmbeddingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for efficient batch processing of embedding requests.
 */
@Service
public class BatchEmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(BatchEmbeddingService.class);
    
    private final EmbeddingService embeddingService;
    private final ExecutorService batchExecutor;
    private final Queue<BatchItem> pendingRequests;
    private final AtomicBoolean processingBatch;
    
    private final int batchSize;
    private final Duration batchTimeout;
    private final int maxRetries;
    private final boolean parallelProcessing;
    
    public BatchEmbeddingService(
            EmbeddingService embeddingService,
            @Value("${embedding.batch.size:50}") int batchSize,
            @Value("${embedding.batch.timeout:30s}") Duration batchTimeout,
            @Value("${embedding.batch.max-retries:3}") int maxRetries,
            @Value("${embedding.batch.parallel-processing:true}") boolean parallelProcessing,
            @Value("${embedding.batch.max-threads:4}") int maxThreads) {
        
        this.embeddingService = embeddingService;
        this.batchSize = batchSize;
        this.batchTimeout = batchTimeout;
        this.maxRetries = maxRetries;
        this.parallelProcessing = parallelProcessing;
        
        this.pendingRequests = new ConcurrentLinkedQueue<>();
        this.processingBatch = new AtomicBoolean(false);
        this.batchExecutor = Executors.newFixedThreadPool(maxThreads);
        
        logger.info("Initialized batch embedding service with batch size: {}, timeout: {}", 
                   batchSize, batchTimeout);
    }
    
    /**
     * Submit embedding request for batch processing.
     */
    @Async
    public CompletableFuture<EmbeddingResponse> submitForBatching(EmbeddingRequest request) {
        CompletableFuture<EmbeddingResponse> future = new CompletableFuture<>();
        
        BatchItem item = new BatchItem(
            request,
            future,
            System.currentTimeMillis(),
            0
        );
        
        pendingRequests.offer(item);
        
        logger.debug("Submitted request for batching: {} texts for tenant: {}", 
                    request.texts().size(), request.tenantId());
        
        // Trigger immediate processing if batch is full
        if (pendingRequests.size() >= batchSize) {
            processBatch();
        }
        
        return future;
    }
    
    /**
     * Process pending requests in batches - scheduled every 5 seconds.
     */
    @Scheduled(fixedDelay = 5000)
    public void processScheduledBatch() {
        if (!pendingRequests.isEmpty() && !processingBatch.get()) {
            processBatch();
        }
    }
    
    /**
     * Process timeout requests - scheduled every 30 seconds.
     */
    @Scheduled(fixedDelay = 30000)
    public void processTimeoutBatch() {
        long now = System.currentTimeMillis();
        long timeoutMs = batchTimeout.toMillis();
        
        List<BatchItem> timedOutItems = new ArrayList<>();
        
        // Find timed out items
        Iterator<BatchItem> iterator = pendingRequests.iterator();
        while (iterator.hasNext()) {
            BatchItem item = iterator.next();
            if (now - item.submittedAt() > timeoutMs) {
                timedOutItems.add(item);
                iterator.remove();
            }
        }
        
        // Process timed out items immediately
        if (!timedOutItems.isEmpty()) {
            logger.info("Processing {} timed out requests", timedOutItems.size());
            processBatchItems(timedOutItems);
        }
    }
    
    private void processBatch() {
        if (!processingBatch.compareAndSet(false, true)) {
            return; // Another thread is already processing
        }
        
        try {
            List<BatchItem> currentBatch = new ArrayList<>();
            
            // Collect items for current batch
            while (!pendingRequests.isEmpty() && currentBatch.size() < batchSize) {
                BatchItem item = pendingRequests.poll();
                if (item != null) {
                    currentBatch.add(item);
                }
            }
            
            if (!currentBatch.isEmpty()) {
                logger.info("Processing batch of {} requests", currentBatch.size());
                processBatchItems(currentBatch);
            }
            
        } finally {
            processingBatch.set(false);
        }
    }
    
    private void processBatchItems(List<BatchItem> items) {
        if (parallelProcessing) {
            // Process items in parallel
            List<CompletableFuture<Void>> futures = items.stream()
                .map(item -> CompletableFuture.runAsync(() -> processItem(item), batchExecutor))
                .toList();
            
            // Wait for all to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .join();
                
        } else {
            // Process items sequentially
            items.forEach(this::processItem);
        }
    }
    
    private void processItem(BatchItem item) {
        try {
            EmbeddingResponse response = embeddingService.generateEmbeddings(item.request());
            item.future().complete(response);
            
            logger.debug("Completed batch item for tenant: {}", item.request().tenantId());
            
        } catch (Exception e) {
            logger.error("Failed to process batch item for tenant: {}", 
                        item.request().tenantId(), e);
            
            // Retry logic
            if (item.retryCount() < maxRetries) {
                BatchItem retryItem = new BatchItem(
                    item.request(),
                    item.future(),
                    item.submittedAt(),
                    item.retryCount() + 1
                );
                
                pendingRequests.offer(retryItem);
                logger.info("Retrying batch item (attempt {}/{})", 
                           retryItem.retryCount(), maxRetries);
                           
            } else {
                // Max retries exceeded, complete with error
                EmbeddingResponse errorResponse = EmbeddingResponse.failure(
                    item.request().tenantId(),
                    item.request().documentId(),
                    item.request().modelName(),
                    "Max retries exceeded: " + e.getMessage(),
                    0
                );
                
                item.future().complete(errorResponse);
            }
        }
    }
    
    /**
     * Get batch processing statistics.
     */
    public BatchStats getStats() {
        return new BatchStats(
            pendingRequests.size(),
            batchSize,
            batchTimeout.toMillis(),
            maxRetries,
            parallelProcessing
        );
    }
    
    /**
     * Clear all pending requests (for testing or emergency scenarios).
     */
    public int clearPendingRequests() {
        int cleared = 0;
        while (!pendingRequests.isEmpty()) {
            BatchItem item = pendingRequests.poll();
            if (item != null) {
                item.future().cancel(false);
                cleared++;
            }
        }
        
        logger.warn("Cleared {} pending batch requests", cleared);
        return cleared;
    }
    
    /**
     * Individual batch item.
     */
    private record BatchItem(
        EmbeddingRequest request,
        CompletableFuture<EmbeddingResponse> future,
        long submittedAt,
        int retryCount
    ) {}
    
    /**
     * Batch processing statistics.
     */
    public record BatchStats(
        int pendingRequests,
        int batchSize,
        long batchTimeoutMs,
        int maxRetries,
        boolean parallelProcessing
    ) {}
}
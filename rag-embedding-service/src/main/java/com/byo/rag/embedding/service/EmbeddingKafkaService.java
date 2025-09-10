package com.byo.rag.embedding.service;

import com.byo.rag.embedding.dto.EmbeddingRequest;
import com.byo.rag.embedding.dto.EmbeddingResponse;
import com.byo.rag.shared.util.JsonUtils;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for handling embedding operations via Kafka messaging with comprehensive error handling.
 */
@Service
@ConditionalOnBean(KafkaTemplate.class)
public class EmbeddingKafkaService {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingKafkaService.class);
    private static final String TRACE_ID_KEY = "traceId";
    private static final String TENANT_ID_KEY = "tenantId";
    private static final String CHUNK_ID_KEY = "chunkId";
    
    private final EmbeddingService embeddingService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final NotificationService notificationService;
    private final DeadLetterQueueService deadLetterQueueService;
    private final String embeddingCompleteTopic;
    
    // Thread-safe counters for tracking attempts
    private final AtomicInteger totalProcessedMessages = new AtomicInteger(0);
    private final AtomicInteger totalFailedMessages = new AtomicInteger(0);
    
    // Metrics counters
    private final Counter successCounter;
    private final Counter errorCounter;
    private final MeterRegistry meterRegistry;
    
    public EmbeddingKafkaService(
            EmbeddingService embeddingService,
            KafkaTemplate<String, String> kafkaTemplate,
            NotificationService notificationService,
            DeadLetterQueueService deadLetterQueueService,
            MeterRegistry meterRegistry,
            @Value("${kafka.topics.embedding-complete:embedding-complete}") String embeddingCompleteTopic) {
        this.embeddingService = embeddingService;
        this.kafkaTemplate = kafkaTemplate;
        this.notificationService = notificationService;
        this.deadLetterQueueService = deadLetterQueueService;
        this.embeddingCompleteTopic = embeddingCompleteTopic;
        this.meterRegistry = meterRegistry;
        
        // Initialize metrics counters
        this.successCounter = meterRegistry.counter("embedding.kafka.processing.success", 
            "description", "Number of successfully processed embedding messages");
        this.errorCounter = meterRegistry.counter("embedding.kafka.processing.error", 
            "description", "Number of failed embedding message processing attempts");
    }
    
    /**
     * Process embedding generation requests from Kafka with comprehensive error handling.
     */
    @KafkaListener(topics = "${kafka.topics.embedding-generation:embedding-generation}")
    public void processEmbeddingRequest(String message) {
        final int messageNumber = totalProcessedMessages.incrementAndGet();
        EmbeddingGenerationMessage request = null;
        
        try {
            // Parse the message first
            request = JsonUtils.fromJson(message, EmbeddingGenerationMessage.class);
            
            // Set up MDC for tracing
            setupMDC(request, messageNumber);
            
            logger.info("Starting processing embedding request #{} for chunk: {} (tenant: {}, document: {})",
                messageNumber, request.chunkId(), request.tenantId(), request.documentId());
            
            // Process the embedding with retry and circuit breaker
            processEmbeddingWithErrorHandling(request, messageNumber);
            
        } catch (Exception e) {
            handleFinalFailure(request, e, messageNumber);
        } finally {
            // Clear MDC to avoid memory leaks
            MDC.clear();
        }
    }
    
    /**
     * Setup MDC for request tracing.
     */
    private void setupMDC(EmbeddingGenerationMessage request, int messageNumber) {
        MDC.put(TRACE_ID_KEY, UUID.randomUUID().toString());
        MDC.put(TENANT_ID_KEY, request.tenantId().toString());
        MDC.put(CHUNK_ID_KEY, request.chunkId().toString());
        MDC.put("messageNumber", String.valueOf(messageNumber));
    }
    
    /**
     * Process embedding with retry logic and circuit breaker protection.
     */
    @Retry(name = "embeddingGeneration", fallbackMethod = "fallbackEmbeddingGeneration")
    @CircuitBreaker(name = "embeddingService", fallbackMethod = "circuitBreakerFallback")
    private void processEmbeddingWithErrorHandling(EmbeddingGenerationMessage request, int messageNumber) {
        try {
            long startTime = System.currentTimeMillis();
            
            // Create embedding request
            EmbeddingRequest embeddingRequest = EmbeddingRequest.singleText(
                request.tenantId(),
                request.content(),
                null, // Use default model
                request.documentId(),
                request.chunkId()
            );
            
            logger.debug("Calling embedding service for chunk: {}", request.chunkId());
            
            // Generate embedding
            EmbeddingResponse response = embeddingService.generateEmbeddings(embeddingRequest);
            
            long processingTime = System.currentTimeMillis() - startTime;
            logger.debug("Embedding generation completed in {}ms for chunk: {}", 
                processingTime, request.chunkId());
            
            // Send completion notification
            sendEmbeddingCompleteWithRetry(request, response);
            
            logger.info("Successfully processed embedding request #{} for chunk: {} in {}ms", 
                messageNumber, request.chunkId(), processingTime);
            
            // Increment success metrics
            successCounter.increment();
            
        } catch (Exception e) {
            logger.error("Error in embedding processing for chunk: {} (attempt will be retried)", 
                request.chunkId(), e);
            throw e; // Re-throw to trigger retry mechanism
        }
    }
    
    /**
     * Fallback method for retry failures - final attempt after all retries exhausted.
     */
    public void fallbackEmbeddingGeneration(EmbeddingGenerationMessage request, int messageNumber, Exception ex) {
        logger.error("All retry attempts exhausted for chunk: {} after processing message #{}", 
            request.chunkId(), messageNumber, ex);
        handleFinalFailure(request, ex, messageNumber);
    }
    
    /**
     * Fallback method for circuit breaker failures.
     */
    public void circuitBreakerFallback(EmbeddingGenerationMessage request, int messageNumber, Exception ex) {
        logger.error("Circuit breaker is open for embedding service - chunk: {} (message #{})", 
            request.chunkId(), messageNumber, ex);
        handleFinalFailure(request, ex, messageNumber);
    }
    
    /**
     * Handle final failure after all error handling mechanisms have been exhausted.
     */
    private void handleFinalFailure(EmbeddingGenerationMessage request, Exception error, int messageNumber) {
        if (request == null) {
            logger.error("Critical error: Unable to parse message #{} - sending to DLQ", messageNumber, error);
            return;
        }
        
        final int failedCount = totalFailedMessages.incrementAndGet();
        final int maxRetries = 3; // This should match the retry configuration
        
        logger.error("Final failure for chunk: {} (message #{}, failed message #{}/{}): {}", 
            request.chunkId(), messageNumber, failedCount, totalProcessedMessages.get(), 
            error.getMessage(), error);
        
        // Increment error metrics with error type tag
        meterRegistry.counter("embedding.kafka.processing.error", 
            "error_type", error.getClass().getSimpleName()).increment();
        
        try {
            // Send to dead letter queue
            deadLetterQueueService.sendToDeadLetterQueue(request, error, maxRetries);
            
            // Send failure notification to administrators
            notificationService.sendFailureAlert(request, error, maxRetries);
            
            logger.warn("Sent failure notifications for chunk: {} after {} total failures", 
                request.chunkId(), failedCount);
                
        } catch (Exception notificationError) {
            logger.error("Critical error: Failed to send failure notifications for chunk: {}", 
                request.chunkId(), notificationError);
        }
    }
    
    /**
     * Send embedding completion notification with retry protection.
     */
    @CircuitBreaker(name = "kafka", fallbackMethod = "kafkaPublishFallback")
    private void sendEmbeddingCompleteWithRetry(EmbeddingGenerationMessage originalRequest, 
                                               EmbeddingResponse response) {
        try {
            EmbeddingCompleteMessage message = new EmbeddingCompleteMessage(
                originalRequest.chunkId(),
                originalRequest.tenantId(),
                originalRequest.documentId(),
                response.status(),
                response.embeddings().isEmpty() ? null : response.embeddings().get(0).embedding(),
                response.modelName(),
                System.currentTimeMillis()
            );
            
            String messageJson = JsonUtils.toJson(message);
            
            logger.debug("Sending completion notification for chunk: {}", originalRequest.chunkId());
            
            CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(embeddingCompleteTopic, originalRequest.chunkId().toString(), messageJson);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.debug("Successfully sent embedding completion for chunk: {}", 
                        originalRequest.chunkId());
                } else {
                    logger.error("Failed to send embedding completion for chunk: {} - will retry", 
                               originalRequest.chunkId(), ex);
                    throw new RuntimeException("Kafka publish failed", ex);
                }
            });
            
        } catch (Exception e) {
            logger.error("Error sending embedding completion notification for chunk: {}", 
                originalRequest.chunkId(), e);
            throw e; // Re-throw to trigger circuit breaker
        }
    }
    
    /**
     * Fallback method for Kafka publishing failures.
     */
    public void kafkaPublishFallback(EmbeddingGenerationMessage originalRequest, EmbeddingResponse response, Exception ex) {
        logger.error("Kafka circuit breaker activated - failed to send completion for chunk: {}", 
            originalRequest.chunkId(), ex);
        // In production, you might want to store this in a local queue or database for later retry
    }
    
    /**
     * Message format for embedding generation requests.
     */
    public record EmbeddingGenerationMessage(
        UUID chunkId,
        String content,
        UUID tenantId,
        UUID documentId
    ) {}
    
    /**
     * Message format for embedding completion notifications.
     */
    public record EmbeddingCompleteMessage(
        UUID chunkId,
        UUID tenantId,
        UUID documentId,
        String status,
        List<Float> embedding,
        String modelName,
        long completedAt
    ) {}
}
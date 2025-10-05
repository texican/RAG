package com.byo.rag.embedding.service;

import com.byo.rag.embedding.service.EmbeddingKafkaService.EmbeddingGenerationMessage;
import com.byo.rag.shared.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for handling messages that consistently fail processing.
 */
@Service
@ConditionalOnBean(KafkaTemplate.class)
public class DeadLetterQueueService {

    private static final Logger logger = LoggerFactory.getLogger(DeadLetterQueueService.class);
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String deadLetterTopic;

    public DeadLetterQueueService(
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${kafka.topics.dead-letter-queue:embedding-dlq}") String deadLetterTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.deadLetterTopic = deadLetterTopic;
    }

    /**
     * Send a message to the dead letter queue after all retry attempts have failed.
     */
    public void sendToDeadLetterQueue(EmbeddingGenerationMessage originalMessage, 
                                    Exception lastError, 
                                    int totalAttempts) {
        try {
            DeadLetterMessage dlqMessage = new DeadLetterMessage(
                UUID.randomUUID(),
                originalMessage,
                lastError.getClass().getSimpleName(),
                lastError.getMessage(),
                totalAttempts,
                Instant.now(),
                "EMBEDDING_PROCESSING_FAILURE"
            );

            String messageJson = JsonUtils.toJson(dlqMessage);
            
            kafkaTemplate.send(deadLetterTopic, originalMessage.chunkId().toString(), messageJson)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        logger.warn("Sent message to dead letter queue for chunk: {} after {} attempts. Reason: {}", 
                            originalMessage.chunkId(), totalAttempts, lastError.getMessage());
                    } else {
                        logger.error("Failed to send message to dead letter queue for chunk: {}", 
                            originalMessage.chunkId(), ex);
                    }
                });
                
        } catch (Exception e) {
            logger.error("Critical error: Failed to send message to dead letter queue", e);
        }
    }

    /**
     * Record for dead letter queue messages.
     */
    public record DeadLetterMessage(
        UUID dlqId,
        EmbeddingGenerationMessage originalMessage,
        String errorType,
        String errorMessage,
        int attemptCount,
        Instant failedAt,
        String failureReason
    ) {}
}
package com.byo.rag.embedding.service;

import com.byo.rag.embedding.service.EmbeddingKafkaService.EmbeddingGenerationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for sending failure notifications to administrators.
 */
@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    private final KafkaTemplate<String, String> kafkaTemplate;

    public NotificationService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Send an alert notification for persistent failures.
     */
    public void sendFailureAlert(EmbeddingGenerationMessage originalMessage, Exception error, int attemptCount) {
        try {
            FailureAlert alert = new FailureAlert(
                UUID.randomUUID(),
                "EMBEDDING_PROCESSING_FAILURE",
                String.format("Failed to process embedding for chunk %s after %d attempts: %s", 
                    originalMessage.chunkId(), attemptCount, error.getMessage()),
                "HIGH",
                originalMessage.tenantId(),
                originalMessage.documentId(),
                originalMessage.chunkId(),
                error.getClass().getSimpleName(),
                Instant.now()
            );

            String alertJson = com.byo.rag.shared.util.JsonUtils.toJson(alert);
            
            kafkaTemplate.send("failure-alerts", alert.alertId().toString(), alertJson)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        logger.info("Sent failure alert for chunk: {}", originalMessage.chunkId());
                    } else {
                        logger.error("Failed to send failure alert for chunk: {}", 
                            originalMessage.chunkId(), ex);
                    }
                });
                
        } catch (Exception e) {
            logger.error("Error creating failure alert", e);
        }
    }

    /**
     * Record for failure alert messages.
     */
    public record FailureAlert(
        UUID alertId,
        String alertType,
        String message,
        String severity,
        UUID tenantId,
        UUID documentId,
        UUID chunkId,
        String errorType,
        Instant timestamp
    ) {}
}
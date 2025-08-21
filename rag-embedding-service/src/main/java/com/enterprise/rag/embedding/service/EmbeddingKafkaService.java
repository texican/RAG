package com.enterprise.rag.embedding.service;

import com.enterprise.rag.embedding.dto.EmbeddingRequest;
import com.enterprise.rag.embedding.dto.EmbeddingResponse;
import com.enterprise.rag.shared.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for handling embedding operations via Kafka messaging.
 */
@Service
public class EmbeddingKafkaService {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingKafkaService.class);
    
    private final EmbeddingService embeddingService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String embeddingCompleteTopic;
    
    public EmbeddingKafkaService(
            EmbeddingService embeddingService,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${kafka.topics.embedding-complete:embedding-complete}") String embeddingCompleteTopic) {
        this.embeddingService = embeddingService;
        this.kafkaTemplate = kafkaTemplate;
        this.embeddingCompleteTopic = embeddingCompleteTopic;
    }
    
    /**
     * Process embedding generation requests from Kafka.
     */
    @KafkaListener(topics = "${kafka.topics.embedding-generation:embedding-generation}")
    public void processEmbeddingRequest(String message) {
        try {
            logger.debug("Received embedding generation request");
            
            EmbeddingGenerationMessage request = JsonUtils.fromJson(message, EmbeddingGenerationMessage.class);
            
            // Create embedding request
            EmbeddingRequest embeddingRequest = EmbeddingRequest.singleText(
                request.tenantId(),
                request.content(),
                null, // Use default model
                request.documentId(),
                request.chunkId()
            );
            
            // Generate embedding
            EmbeddingResponse response = embeddingService.generateEmbeddings(embeddingRequest);
            
            // Send completion notification
            sendEmbeddingComplete(request, response);
            
            logger.info("Processed embedding request for chunk: {} in tenant: {}", 
                       request.chunkId(), request.tenantId());
            
        } catch (Exception e) {
            logger.error("Failed to process embedding generation request", e);
            // TODO: Send failure notification or retry
        }
    }
    
    /**
     * Send embedding completion notification.
     */
    private void sendEmbeddingComplete(EmbeddingGenerationMessage originalRequest, 
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
            
            CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(embeddingCompleteTopic, originalRequest.chunkId().toString(), messageJson);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.debug("Sent embedding completion for chunk: {}", originalRequest.chunkId());
                } else {
                    logger.error("Failed to send embedding completion for chunk: {}", 
                               originalRequest.chunkId(), ex);
                }
            });
            
        } catch (Exception e) {
            logger.error("Error sending embedding completion notification", e);
        }
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
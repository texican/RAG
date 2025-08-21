package com.enterprise.rag.document.service;

import com.enterprise.rag.shared.entity.DocumentChunk;
import com.enterprise.rag.shared.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Profile("!test")
public class DocumentProcessingKafkaService implements DocumentProcessingKafkaServiceInterface {

    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingKafkaService.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String documentProcessingTopic;
    private final String embeddingGenerationTopic;

    public DocumentProcessingKafkaService(
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${kafka.topics.document-processing:document-processing}") String documentProcessingTopic,
            @Value("${kafka.topics.embedding-generation:embedding-generation}") String embeddingGenerationTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.documentProcessingTopic = documentProcessingTopic;
        this.embeddingGenerationTopic = embeddingGenerationTopic;
    }

    @Override
    public void sendDocumentForProcessing(UUID documentId) {
        try {
            DocumentProcessingMessage message = new DocumentProcessingMessage(documentId);
            String messageJson = JsonUtils.toJson(message);

            CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(documentProcessingTopic, documentId.toString(), messageJson);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("Sent document for processing: {} with offset: {}", 
                               documentId, result.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to send document for processing: {}", documentId, ex);
                }
            });

        } catch (Exception e) {
            logger.error("Error sending document processing message for document: {}", documentId, e);
        }
    }

    @Override
    public void sendChunksForEmbedding(List<DocumentChunk> chunks) {
        try {
            for (DocumentChunk chunk : chunks) {
                EmbeddingGenerationMessage message = new EmbeddingGenerationMessage(
                    chunk.getId(),
                    chunk.getContent(),
                    chunk.getTenant().getId(),
                    chunk.getDocument().getId()
                );

                String messageJson = JsonUtils.toJson(message);

                CompletableFuture<SendResult<String, String>> future = 
                    kafkaTemplate.send(embeddingGenerationTopic, chunk.getId().toString(), messageJson);

                future.whenComplete((result, ex) -> {
                    if (ex == null) {
                        logger.debug("Sent chunk for embedding: {} with offset: {}", 
                                   chunk.getId(), result.getRecordMetadata().offset());
                    } else {
                        logger.error("Failed to send chunk for embedding: {}", chunk.getId(), ex);
                    }
                });
            }

            logger.info("Sent {} chunks for embedding generation", chunks.size());

        } catch (Exception e) {
            logger.error("Error sending chunks for embedding generation", e);
        }
    }

    public record DocumentProcessingMessage(UUID documentId) {}

    public record EmbeddingGenerationMessage(
        UUID chunkId,
        String content,
        UUID tenantId,
        UUID documentId
    ) {}
}
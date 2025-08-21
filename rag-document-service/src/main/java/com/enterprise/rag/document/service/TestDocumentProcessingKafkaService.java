package com.enterprise.rag.document.service;

import com.enterprise.rag.shared.entity.DocumentChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Test implementation of DocumentProcessingKafkaService that doesn't require Kafka.
 * This allows tests to run without a Kafka instance.
 */
@Service
@Primary
@Profile("test")
public class TestDocumentProcessingKafkaService implements DocumentProcessingKafkaServiceInterface {

    private static final Logger logger = LoggerFactory.getLogger(TestDocumentProcessingKafkaService.class);

    @Override
    public void sendDocumentForProcessing(UUID documentId) {
        logger.debug("Test: Would send document for processing: {}", documentId);
        // No-op for testing
    }

    @Override
    public void sendChunksForEmbedding(List<DocumentChunk> chunks) {
        logger.debug("Test: Would send {} chunks for embedding generation", chunks.size());
        // No-op for testing
    }
}
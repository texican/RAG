package com.enterprise.rag.document.service;

import com.enterprise.rag.shared.entity.DocumentChunk;

import java.util.List;
import java.util.UUID;

/**
 * Interface for document processing Kafka operations.
 * Allows for easy testing with mock implementations.
 */
public interface DocumentProcessingKafkaServiceInterface {
    
    /**
     * Send a document for asynchronous processing.
     * 
     * @param documentId the ID of the document to process
     */
    void sendDocumentForProcessing(UUID documentId);
    
    /**
     * Send document chunks for embedding generation.
     * 
     * @param chunks the list of document chunks to process
     */
    void sendChunksForEmbedding(List<DocumentChunk> chunks);
}
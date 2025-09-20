package com.byo.rag.document.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Event DTO for document processing pipeline initiation.
 * 
 * <p>This event is published when a document is uploaded and ready for processing.
 * It triggers the asynchronous document processing pipeline including text extraction,
 * chunking, and preparation for embedding generation.</p>
 * 
 * <p><strong>Event Flow:</strong></p>
 * <ol>
 *   <li>Document uploaded and stored</li>
 *   <li>DocumentProcessingEvent published</li>
 *   <li>Document processor consumes event</li>
 *   <li>Text extraction and chunking performed</li>
 *   <li>ChunkEmbeddingEvent(s) published</li>
 * </ol>
 * 
 * @param documentId the unique identifier of the document to process
 * @param timestamp the time when the event was created
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 */
public record DocumentProcessingEvent(
    UUID documentId,
    Instant timestamp
) {
    /**
     * Creates a new document processing event with the current timestamp.
     * 
     * @param documentId the document ID to process
     * @return new event with current timestamp
     */
    public static DocumentProcessingEvent now(UUID documentId) {
        return new DocumentProcessingEvent(documentId, Instant.now());
    }
}
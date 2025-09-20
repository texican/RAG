package com.byo.rag.document.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Event DTO for chunk embedding generation requests.
 * 
 * <p>This event is published when document chunks are created and ready for vector embedding
 * generation. It contains all necessary information for the embedding service to process
 * the chunk and generate appropriate vector representations.</p>
 * 
 * <p><strong>Embedding Pipeline:</strong></p>
 * <ol>
 *   <li>Document processed and chunks created</li>
 *   <li>ChunkEmbeddingEvent published for each chunk</li>
 *   <li>Embedding service consumes events</li>
 *   <li>Vector embeddings generated</li>
 *   <li>Embeddings stored and linked to chunks</li>
 * </ol>
 * 
 * <p><strong>Event Content:</strong></p>
 * <ul>
 *   <li><strong>Chunk Information:</strong> ID, sequence, content, token count</li>
 *   <li><strong>Context Information:</strong> Document and tenant IDs for isolation</li>
 *   <li><strong>Processing Metadata:</strong> Timestamp for tracking and monitoring</li>
 * </ul>
 * 
 * @param chunkId the unique identifier of the document chunk
 * @param documentId the ID of the parent document
 * @param tenantId the ID of the tenant for multi-tenant isolation
 * @param content the text content of the chunk to embed
 * @param sequenceNumber the order of this chunk within the document
 * @param tokenCount the estimated token count for the chunk content
 * @param timestamp the time when the event was created
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 */
public record ChunkEmbeddingEvent(
    UUID chunkId,
    UUID documentId,
    UUID tenantId,
    String content,
    Integer sequenceNumber,
    Integer tokenCount,
    Instant timestamp
) {
    /**
     * Creates a new chunk embedding event with the current timestamp.
     * 
     * @param chunkId the chunk ID
     * @param documentId the document ID
     * @param tenantId the tenant ID
     * @param content the chunk content
     * @param sequenceNumber the chunk sequence number
     * @param tokenCount the estimated token count
     * @return new event with current timestamp
     */
    public static ChunkEmbeddingEvent now(UUID chunkId, UUID documentId, UUID tenantId, 
                                        String content, Integer sequenceNumber, Integer tokenCount) {
        return new ChunkEmbeddingEvent(chunkId, documentId, tenantId, content, 
                                     sequenceNumber, tokenCount, Instant.now());
    }
    
    /**
     * Validates that the event contains all required information.
     * 
     * @throws IllegalArgumentException if any required field is null or invalid
     */
    public void validate() {
        if (chunkId == null) {
            throw new IllegalArgumentException("Chunk ID cannot be null");
        }
        if (documentId == null) {
            throw new IllegalArgumentException("Document ID cannot be null");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content cannot be null or empty");
        }
        if (sequenceNumber == null || sequenceNumber < 0) {
            throw new IllegalArgumentException("Sequence number must be non-negative");
        }
        if (tokenCount == null || tokenCount < 0) {
            throw new IllegalArgumentException("Token count must be non-negative");
        }
    }
}
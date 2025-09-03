package com.byo.rag.embedding.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for embedding generation operations.
 */
public record EmbeddingResponse(
    UUID tenantId,
    UUID documentId,
    String modelName,
    List<EmbeddingResult> embeddings,
    int dimension,
    String status,
    Instant createdAt,
    long processingTimeMs
) {
    
    /**
     * Individual embedding result.
     */
    public record EmbeddingResult(
        UUID chunkId,
        String text,
        List<Float> embedding,
        String status,
        String error
    ) {
        
        /**
         * Create successful embedding result.
         */
        public static EmbeddingResult success(UUID chunkId, String text, List<Float> embedding) {
            return new EmbeddingResult(chunkId, text, embedding, "SUCCESS", null);
        }
        
        /**
         * Create failed embedding result.
         */
        public static EmbeddingResult failure(UUID chunkId, String text, String error) {
            return new EmbeddingResult(chunkId, text, null, "FAILED", error);
        }
    }
    
    /**
     * Create successful response with embeddings.
     */
    public static EmbeddingResponse success(UUID tenantId, UUID documentId, String modelName,
                                          List<EmbeddingResult> embeddings, int dimension,
                                          long processingTimeMs) {
        return new EmbeddingResponse(
            tenantId,
            documentId,
            modelName,
            embeddings,
            dimension,
            "SUCCESS",
            Instant.now(),
            processingTimeMs
        );
    }
    
    /**
     * Create partial success response (some embeddings failed).
     */
    public static EmbeddingResponse partial(UUID tenantId, UUID documentId, String modelName,
                                          List<EmbeddingResult> embeddings, int dimension,
                                          long processingTimeMs) {
        return new EmbeddingResponse(
            tenantId,
            documentId,
            modelName,
            embeddings,
            dimension,
            "PARTIAL",
            Instant.now(),
            processingTimeMs
        );
    }
    
    /**
     * Create failed response.
     */
    public static EmbeddingResponse failure(UUID tenantId, UUID documentId, String modelName,
                                          String error, long processingTimeMs) {
        return new EmbeddingResponse(
            tenantId,
            documentId,
            modelName,
            List.of(),
            0,
            "FAILED",
            Instant.now(),
            processingTimeMs
        );
    }
}
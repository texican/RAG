package com.enterprise.rag.embedding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for embedding generation operations.
 */
public record EmbeddingRequest(
    @NotNull
    UUID tenantId,
    
    @NotEmpty
    @Size(max = 100, message = "Maximum 100 texts can be processed in a single request")
    List<@NotBlank @Size(max = 8000, message = "Text must not exceed 8000 characters") String> texts,
    
    @Size(max = 50)
    String modelName,
    
    @NotNull
    UUID documentId,
    
    List<UUID> chunkIds
) {
    
    public EmbeddingRequest {
        // Validation: ensure chunkIds matches texts size if provided
        if (chunkIds != null && !chunkIds.isEmpty() && chunkIds.size() != texts.size()) {
            throw new IllegalArgumentException("Number of chunk IDs must match number of texts");
        }
    }
    
    /**
     * Create request for single text embedding.
     */
    public static EmbeddingRequest singleText(UUID tenantId, String text, String modelName, 
                                            UUID documentId, UUID chunkId) {
        return new EmbeddingRequest(
            tenantId, 
            List.of(text), 
            modelName, 
            documentId, 
            chunkId != null ? List.of(chunkId) : null
        );
    }
    
    /**
     * Create request for batch text embedding.
     */
    public static EmbeddingRequest batchTexts(UUID tenantId, List<String> texts, String modelName,
                                            UUID documentId, List<UUID> chunkIds) {
        return new EmbeddingRequest(tenantId, texts, modelName, documentId, chunkIds);
    }
}
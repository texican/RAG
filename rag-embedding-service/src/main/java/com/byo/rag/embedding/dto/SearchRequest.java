package com.byo.rag.embedding.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for vector similarity search operations.
 */
public record SearchRequest(
    @NotNull
    UUID tenantId,
    
    @NotBlank
    @Size(max = 2000, message = "Query text must not exceed 2000 characters")
    String query,
    
    @Min(1)
    @Max(100)
    int topK,
    
    @Min(0)
    @Max(1)
    double threshold,
    
    @Size(max = 50)
    String modelName,
    
    List<UUID> documentIds,
    
    Map<String, Object> filters,
    
    boolean includeContent,
    
    boolean includeMetadata
) {
    
    /**
     * Default constructor with common defaults.
     */
    public SearchRequest {
        if (topK <= 0) topK = 10;
        if (threshold < 0) threshold = 0.0;
    }
    
    /**
     * Create simple search request with defaults.
     */
    public static SearchRequest simple(UUID tenantId, String query) {
        return new SearchRequest(
            tenantId,
            query,
            10,
            0.0,
            null,
            null,
            null,
            true,
            true
        );
    }
    
    /**
     * Create advanced search request with all options.
     */
    public static SearchRequest advanced(UUID tenantId, String query, int topK, double threshold,
                                       String modelName, List<UUID> documentIds,
                                       Map<String, Object> filters) {
        return new SearchRequest(
            tenantId,
            query,
            topK,
            threshold,
            modelName,
            documentIds,
            filters,
            true,
            true
        );
    }
    
    /**
     * Create content-only search (no metadata).
     */
    public static SearchRequest contentOnly(UUID tenantId, String query, int topK) {
        return new SearchRequest(
            tenantId,
            query,
            topK,
            0.0,
            null,
            null,
            null,
            true,
            false
        );
    }
}
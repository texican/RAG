package com.enterprise.rag.embedding.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for vector similarity search operations.
 */
public record SearchResponse(
    UUID tenantId,
    String query,
    String modelName,
    List<SearchResult> results,
    int totalResults,
    double maxScore,
    long searchTimeMs,
    Instant searchedAt
) {
    
    /**
     * Individual search result.
     */
    public record SearchResult(
        UUID chunkId,
        UUID documentId,
        String content,
        double score,
        Map<String, Object> metadata,
        String documentTitle,
        String documentType
    ) {
        
        /**
         * Create search result with full data.
         */
        public static SearchResult of(UUID chunkId, UUID documentId, String content,
                                    double score, Map<String, Object> metadata,
                                    String documentTitle, String documentType) {
            return new SearchResult(chunkId, documentId, content, score, metadata,
                                  documentTitle, documentType);
        }
        
        /**
         * Create minimal search result (ID and score only).
         */
        public static SearchResult minimal(UUID chunkId, UUID documentId, double score) {
            return new SearchResult(chunkId, documentId, null, score, null, null, null);
        }
    }
    
    /**
     * Create successful search response.
     */
    public static SearchResponse success(UUID tenantId, String query, String modelName,
                                       List<SearchResult> results, long searchTimeMs) {
        double maxScore = results.isEmpty() ? 0.0 : 
            results.stream().mapToDouble(SearchResult::score).max().orElse(0.0);
        
        return new SearchResponse(
            tenantId,
            query,
            modelName,
            results,
            results.size(),
            maxScore,
            searchTimeMs,
            Instant.now()
        );
    }
    
    /**
     * Create empty search response (no results found).
     */
    public static SearchResponse empty(UUID tenantId, String query, String modelName,
                                     long searchTimeMs) {
        return new SearchResponse(
            tenantId,
            query,
            modelName,
            List.of(),
            0,
            0.0,
            searchTimeMs,
            Instant.now()
        );
    }
}
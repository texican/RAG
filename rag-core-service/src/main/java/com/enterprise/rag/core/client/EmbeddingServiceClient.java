package com.enterprise.rag.core.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Feign client for communicating with the embedding service.
 */
@FeignClient(
    name = "embedding-service",
    url = "${services.embedding.url}",
    configuration = EmbeddingServiceClientConfig.class
)
public interface EmbeddingServiceClient {
    
    /**
     * Search request DTO for embedding service.
     */
    record SearchRequest(
        UUID tenantId,
        String query,
        int topK,
        double threshold,
        String modelName,
        List<UUID> documentIds,
        Map<String, Object> filters,
        boolean includeContent,
        boolean includeMetadata
    ) {}
    
    /**
     * Search response DTO from embedding service.
     */
    record SearchResponse(
        UUID tenantId,
        String query,
        String modelName,
        List<SearchResult> results,
        int totalResults,
        double maxScore,
        long searchTimeMs,
        String searchedAt
    ) {}
    
    /**
     * Individual search result.
     */
    record SearchResult(
        UUID chunkId,
        UUID documentId,
        String content,
        double score,
        Map<String, Object> metadata,
        String documentTitle,
        String documentType
    ) {}
    
    /**
     * Perform semantic similarity search.
     */
    @PostMapping("/api/v1/embeddings/search")
    SearchResponse search(
        @RequestBody SearchRequest request,
        @RequestHeader("X-Tenant-ID") UUID tenantId
    );
    
    /**
     * Perform batch similarity search.
     */
    @PostMapping("/api/v1/embeddings/search/batch")
    List<SearchResponse> searchBatch(
        @RequestBody List<SearchRequest> requests,
        @RequestHeader("X-Tenant-ID") UUID tenantId
    );
    
    /**
     * Get available embedding models.
     */
    @GetMapping("/api/v1/embeddings/models")
    Map<String, Object> getAvailableModels();
    
    /**
     * Get embedding service statistics.
     */
    @GetMapping("/api/v1/embeddings/stats")
    Map<String, Object> getStats(@RequestHeader("X-Tenant-ID") UUID tenantId);
    
    /**
     * Health check for embedding service.
     */
    @GetMapping("/api/v1/embeddings/health")
    Map<String, Object> healthCheck();
}
package com.enterprise.rag.core.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for RAG query operations.
 */
public record RagQueryResponse(
    UUID tenantId,
    String query,
    String conversationId,
    String response,
    List<SourceDocument> sources,
    RagMetrics metrics,
    String status,
    String error,
    Instant createdAt
) {
    
    /**
     * Source document information.
     */
    public record SourceDocument(
        UUID documentId,
        UUID chunkId,
        String title,
        String content,
        double relevanceScore,
        Map<String, Object> metadata,
        String documentType,
        Instant createdAt
    ) {
        
        /**
         * Create source document with essential information.
         */
        public static SourceDocument of(UUID documentId, UUID chunkId, String title, 
                                      String content, double relevanceScore) {
            return new SourceDocument(
                documentId,
                chunkId,
                title,
                content,
                relevanceScore,
                null,
                null,
                null
            );
        }
        
        /**
         * Create source document with full metadata.
         */
        public static SourceDocument withMetadata(UUID documentId, UUID chunkId, String title,
                                                String content, double relevanceScore,
                                                Map<String, Object> metadata, String documentType,
                                                Instant createdAt) {
            return new SourceDocument(
                documentId,
                chunkId,
                title,
                content,
                relevanceScore,
                metadata,
                documentType,
                createdAt
            );
        }
    }
    
    /**
     * RAG processing metrics.
     */
    public record RagMetrics(
        long totalProcessingTimeMs,
        long retrievalTimeMs,
        long contextAssemblyTimeMs,
        long generationTimeMs,
        int chunksRetrieved,
        int chunksUsed,
        int tokensGenerated,
        double averageRelevanceScore,
        String llmProvider,
        String embeddingModel
    ) {
        
        /**
         * Create metrics with basic timing information.
         */
        public static RagMetrics withTiming(long retrievalTime, long contextTime, 
                                          long generationTime, int chunksRetrieved,
                                          int chunksUsed) {
            return new RagMetrics(
                retrievalTime + contextTime + generationTime,
                retrievalTime,
                contextTime,
                generationTime,
                chunksRetrieved,
                chunksUsed,
                0,
                0.0,
                null,
                null
            );
        }
        
        /**
         * Create comprehensive metrics.
         */
        public static RagMetrics comprehensive(long retrievalTime, long contextTime,
                                             long generationTime, int chunksRetrieved,
                                             int chunksUsed, int tokensGenerated,
                                             double avgRelevance, String llmProvider,
                                             String embeddingModel) {
            return new RagMetrics(
                retrievalTime + contextTime + generationTime,
                retrievalTime,
                contextTime,
                generationTime,
                chunksRetrieved,
                chunksUsed,
                tokensGenerated,
                avgRelevance,
                llmProvider,
                embeddingModel
            );
        }
    }
    
    /**
     * Create successful RAG response.
     */
    public static RagQueryResponse success(UUID tenantId, String query, String conversationId,
                                         String response, List<SourceDocument> sources,
                                         RagMetrics metrics) {
        return new RagQueryResponse(
            tenantId,
            query,
            conversationId,
            response,
            sources,
            metrics,
            "SUCCESS",
            null,
            Instant.now()
        );
    }
    
    /**
     * Create partial RAG response (some processing succeeded).
     */
    public static RagQueryResponse partial(UUID tenantId, String query, String conversationId,
                                         String response, List<SourceDocument> sources,
                                         RagMetrics metrics, String warning) {
        return new RagQueryResponse(
            tenantId,
            query,
            conversationId,
            response,
            sources,
            metrics,
            "PARTIAL",
            warning,
            Instant.now()
        );
    }
    
    /**
     * Create failed RAG response.
     */
    public static RagQueryResponse failure(UUID tenantId, String query, String conversationId,
                                         String error, RagMetrics metrics) {
        return new RagQueryResponse(
            tenantId,
            query,
            conversationId,
            null,
            List.of(),
            metrics,
            "FAILED",
            error,
            Instant.now()
        );
    }
    
    /**
     * Create empty RAG response (no relevant documents found).
     */
    public static RagQueryResponse empty(UUID tenantId, String query, String conversationId,
                                       RagMetrics metrics) {
        return new RagQueryResponse(
            tenantId,
            query,
            conversationId,
            "I couldn't find any relevant information to answer your question. Please try rephrasing your query or check if the relevant documents have been uploaded.",
            List.of(),
            metrics,
            "EMPTY",
            null,
            Instant.now()
        );
    }
}
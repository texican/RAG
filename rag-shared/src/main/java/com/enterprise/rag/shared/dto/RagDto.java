package com.enterprise.rag.shared.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public sealed interface RagDto permits 
    RagDto.QueryRequest,
    RagDto.QueryResponse,
    RagDto.SearchResult,
    RagDto.ChunkMatch {

    record QueryRequest(
        @NotBlank @Size(max = 1000) String query,
        Integer maxResults,
        Float similarityThreshold,
        List<UUID> documentIds,
        Map<String, Object> filters,
        QueryOptions options
    ) implements RagDto {
        public QueryRequest {
            if (maxResults == null) maxResults = 5;
            if (similarityThreshold == null) similarityThreshold = 0.7f;
            if (options == null) options = new QueryOptions(true, true, false);
        }
    }

    record QueryResponse(
        String answer,
        List<ChunkMatch> sourceChunks,
        String conversationId,
        QueryMetadata metadata,
        LocalDateTime timestamp
    ) implements RagDto {}

    record SearchResult(
        List<ChunkMatch> chunks,
        QueryMetadata metadata,
        LocalDateTime timestamp
    ) implements RagDto {}

    record ChunkMatch(
        UUID chunkId,
        String content,
        Float similarity,
        DocumentDto.DocumentSummary document,
        Integer sequenceNumber,
        Map<String, Object> metadata
    ) implements RagDto {}

    record QueryOptions(
        Boolean includeMetadata,
        Boolean enableCaching,
        Boolean streamResponse
    ) {
        public QueryOptions {
            if (includeMetadata == null) includeMetadata = true;
            if (enableCaching == null) enableCaching = true;
            if (streamResponse == null) streamResponse = false;
        }
    }

    record QueryMetadata(
        Integer totalChunks,
        String embeddingModel,
        String llmModel,
        Long processingTimeMs,
        Boolean cacheHit
    ) {}
}
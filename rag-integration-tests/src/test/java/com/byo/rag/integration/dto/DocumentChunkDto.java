package com.byo.rag.integration.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Objects for document chunk operations in integration tests.
 * 
 * This sealed interface provides DTOs for handling document chunks in
 * integration testing scenarios where we need to validate chunking results.
 */
public sealed interface DocumentChunkDto permits 
    DocumentChunkDto.ChunkResponse {

    /**
     * Response DTO for document chunk data retrieved from the database.
     * 
     * @param id unique chunk identifier
     * @param documentId the document this chunk belongs to
     * @param chunkIndex the order of this chunk within the document (0-based)
     * @param content the actual text content of the chunk
     * @param characterCount number of characters in the chunk content
     * @param tokenCount estimated number of tokens in the chunk
     * @param createdAt when the chunk was created
     * @param updatedAt when the chunk was last updated
     */
    record ChunkResponse(
        UUID id,
        UUID documentId,
        Integer chunkIndex,
        String content,
        Integer characterCount,
        Integer tokenCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) implements DocumentChunkDto {}
}
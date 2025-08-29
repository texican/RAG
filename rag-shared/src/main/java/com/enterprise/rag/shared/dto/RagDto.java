package com.enterprise.rag.shared.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Data Transfer Objects for RAG (Retrieval Augmented Generation) operations in the Enterprise RAG System.
 * <p>
 * This sealed interface defines all RAG-related DTOs used for query processing, semantic search,
 * and LLM-powered response generation. It supports advanced features like conversation context,
 * document filtering, similarity thresholds, and streaming responses.
 * </p>
 * 
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Semantic Search</strong> - Vector-based similarity matching with configurable thresholds</li>
 *   <li><strong>Context Assembly</strong> - Intelligent selection and ranking of relevant document chunks</li>
 *   <li><strong>LLM Integration</strong> - Prompt construction and response generation with multiple models</li>
 *   <li><strong>Conversation Support</strong> - Multi-turn conversations with context preservation</li>
 *   <li><strong>Performance Optimization</strong> - Caching, streaming, and processing time tracking</li>
 * </ul>
 * 
 * <h3>RAG Pipeline Flow:</h3>
 * <ol>
 *   <li><strong>Query Processing</strong> - Parse and validate user query with filters and options</li>
 *   <li><strong>Embedding Generation</strong> - Convert query to vector embedding for similarity search</li>
 *   <li><strong>Semantic Search</strong> - Find most relevant document chunks using vector similarity</li>
 *   <li><strong>Context Assembly</strong> - Rank and select chunks for LLM context window</li>
 *   <li><strong>Response Generation</strong> - Generate comprehensive answer using LLM with retrieved context</li>
 *   <li><strong>Response Delivery</strong> - Return answer with source citations and metadata</li>
 * </ol>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Create RAG query request
 * var queryRequest = new RagDto.QueryRequest(
 *     "What are the security requirements for authentication?",
 *     10,                                    // Return top 10 results
 *     0.8f,                                 // High similarity threshold
 *     List.of(securityDocId, complianceDocId), // Filter to specific documents
 *     Map.of("category", "security"),       // Additional metadata filters
 *     new RagDto.QueryOptions(true, true, false) // Include metadata, enable caching
 * );
 * 
 * // Process response
 * if (response instanceof RagDto.QueryResponse(var answer, var chunks, var convId, var meta, var time)) {
 *     // Display answer with source citations
 *     chunks.forEach(chunk -> log.info("Source: {} (similarity: {})", 
 *                                       chunk.document().filename(), 
 *                                       chunk.similarity()));
 * }
 * }</pre>
 * 
 * @author Enterprise RAG Development Team
 * @since 1.0.0
 * @see com.enterprise.rag.shared.dto.DocumentChunkDto
 */
public sealed interface RagDto permits 
    RagDto.QueryRequest,
    RagDto.QueryResponse,
    RagDto.SearchResult,
    RagDto.ChunkMatch {

    /**
     * Request DTO for RAG query operations.
     * <p>
     * Contains the user query and configuration parameters for semantic search and response generation.
     * All optional parameters have sensible defaults for immediate usability.
     * </p>
     * 
     * @param query user's natural language question or query (max 1000 characters)
     * @param maxResults maximum number of relevant chunks to retrieve (default: 5)
     * @param similarityThreshold minimum similarity score for chunk inclusion (default: 0.7)
     * @param documentIds optional list of document IDs to limit search scope
     * @param filters additional metadata filters for chunk selection
     * @param options query processing options (caching, metadata, streaming)
     */
    record QueryRequest(
        @NotBlank @Size(max = 1000) String query,
        Integer maxResults,
        Float similarityThreshold,
        List<UUID> documentIds,
        Map<String, Object> filters,
        QueryOptions options
    ) implements RagDto {
        /**
         * Canonical constructor with default value initialization.
         * Ensures sensible defaults for optimal RAG performance.
         */
        public QueryRequest {
            if (maxResults == null) maxResults = 5;
            if (similarityThreshold == null) similarityThreshold = 0.7f;
            if (options == null) options = new QueryOptions(true, true, false);
        }
    }

    /**
     * Complete RAG query response with generated answer and source information.
     * <p>
     * Contains the LLM-generated answer along with the source chunks used for context,
     * conversation tracking, and processing metadata for transparency and debugging.
     * </p>
     * 
     * @param answer LLM-generated response based on retrieved context
     * @param sourceChunks ranked list of document chunks used as context
     * @param conversationId identifier for conversation continuity (if applicable)
     * @param metadata processing information including models used and timing
     * @param timestamp when this response was generated
     */
    record QueryResponse(
        String answer,
        List<ChunkMatch> sourceChunks,
        String conversationId,
        QueryMetadata metadata,
        LocalDateTime timestamp
    ) implements RagDto {}

    /**
     * Semantic search results without LLM response generation.
     * <p>
     * Contains only the ranked document chunks from semantic search,
     * used for search-only operations or when raw retrieval results are needed.
     * </p>
     * 
     * @param chunks ranked list of matching document chunks
     * @param metadata search processing information
     * @param timestamp when this search was performed
     */
    record SearchResult(
        List<ChunkMatch> chunks,
        QueryMetadata metadata,
        LocalDateTime timestamp
    ) implements RagDto {}

    /**
     * Individual document chunk match with similarity scoring.
     * <p>
     * Represents a single relevant document chunk found during semantic search,
     * including similarity score, source document information, and chunk metadata.
     * </p>
     * 
     * @param chunkId unique identifier of the matched chunk
     * @param content text content of the chunk
     * @param similarity cosine similarity score with the query (0.0 to 1.0)
     * @param document summary of the source document
     * @param sequenceNumber position of this chunk within the source document
     * @param metadata additional chunk metadata for context
     */
    record ChunkMatch(
        UUID chunkId,
        String content,
        Float similarity,
        DocumentDto.DocumentSummary document,
        Integer sequenceNumber,
        Map<String, Object> metadata
    ) implements RagDto {}

    /**
     * Configuration options for RAG query processing.
     * <p>
     * Controls various aspects of query processing including metadata inclusion,
     * caching behavior, and response streaming for optimized user experience.
     * </p>
     * 
     * @param includeMetadata whether to include processing metadata in response (default: true)
     * @param enableCaching whether to use caching for performance optimization (default: true)
     * @param streamResponse whether to stream the response for real-time delivery (default: false)
     */
    record QueryOptions(
        Boolean includeMetadata,
        Boolean enableCaching,
        Boolean streamResponse
    ) {
        /**
         * Canonical constructor with default value initialization.
         * Ensures optimal configuration for most use cases.
         */
        public QueryOptions {
            if (includeMetadata == null) includeMetadata = true;
            if (enableCaching == null) enableCaching = true;
            if (streamResponse == null) streamResponse = false;
        }
    }

    /**
     * Processing metadata for RAG operations.
     * <p>
     * Provides transparency into the RAG processing pipeline including
     * performance metrics, model information, and caching effectiveness.
     * </p>
     * 
     * @param totalChunks total number of chunks searched
     * @param embeddingModel AI model used for embedding generation
     * @param llmModel AI model used for response generation
     * @param processingTimeMs total processing time in milliseconds
     * @param cacheHit whether this query was served from cache
     */
    record QueryMetadata(
        Integer totalChunks,
        String embeddingModel,
        String llmModel,
        Long processingTimeMs,
        Boolean cacheHit
    ) {}
}
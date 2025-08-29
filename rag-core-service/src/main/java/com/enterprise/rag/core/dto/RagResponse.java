package com.enterprise.rag.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.enterprise.rag.shared.dto.DocumentChunkDto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for RAG (Retrieval Augmented Generation) query processing operations.
 * <p>
 * This record encapsulates the complete response from a RAG query, including the
 * generated answer, confidence metrics, source documents, and comprehensive
 * metadata about the processing pipeline. It provides transparency into the
 * RAG system's decision-making process and enables response quality assessment.
 * 
 * <h2>Response Components</h2>
 * <ul>
 *   <li><strong>Generated Answer</strong> - AI-generated response based on retrieved context</li>
 *   <li><strong>Confidence Score</strong> - System confidence in the response accuracy (0.0-1.0)</li>
 *   <li><strong>Source Documents</strong> - Document chunks used to generate the answer</li>
 *   <li><strong>Processing Metrics</strong> - Performance and operational metadata</li>
 *   <li><strong>Conversation Context</strong> - Multi-turn conversation tracking</li>
 * </ul>
 * 
 * <h2>Confidence Scoring</h2>
 * The confidence score reflects:
 * <ul>
 *   <li><strong>Source Relevance</strong> - How well retrieved documents match the query</li>
 *   <li><strong>Answer Quality</strong> - LLM's assessment of response completeness</li>
 *   <li><strong>Context Sufficiency</strong> - Adequacy of available context for answering</li>
 *   <li><strong>Semantic Coherence</strong> - Consistency between query and response</li>
 * </ul>
 * 
 * <h2>Source Attribution</h2>
 * <ul>
 *   <li><strong>Document References</strong> - Links to original source documents</li>
 *   <li><strong>Chunk Information</strong> - Specific text segments used in generation</li>
 *   <li><strong>Relevance Scores</strong> - Per-chunk similarity scores</li>
 *   <li><strong>Metadata Preservation</strong> - Original document metadata</li>
 * </ul>
 * 
 * <h2>Query Metadata</h2>
 * The embedded QueryMetadata provides insights into:
 * <ul>
 *   <li><strong>Retrieval Performance</strong> - Number of chunks retrieved and processing time</li>
 *   <li><strong>Model Information</strong> - LLM model used for response generation</li>
 *   <li><strong>Token Usage</strong> - Cost tracking and usage monitoring</li>
 *   <li><strong>Caching Information</strong> - Whether response came from cache</li>
 *   <li><strong>Strategy Details</strong> - Retrieval strategy and optimization applied</li>
 * </ul>
 * 
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Simple response creation
 * var response = new RagResponse(
 *     "The company provides 15 days of vacation annually.",
 *     0.89,
 *     sourceChunks,
 *     1250L,
 *     LocalDateTime.now(),
 *     "conv-123",
 *     metadata
 * );
 * 
 * // Accessing metadata
 * if (response.metadata().fromCache()) {
 *     logger.info("Response served from cache in {}ms", response.processingTimeMs());
 * }
 * }</pre>
 * 
 * @author Enterprise RAG Development Team
 * @version 0.8.0
 * @since 0.1.0
 * @see com.enterprise.rag.shared.dto.DocumentChunkDto
 * @see QueryMetadata
 */
@Schema(description = "Response from RAG query processing")
public record RagResponse(
    
    @Schema(description = "The generated answer", example = "The company provides 15 days of vacation...")
    String answer,
    
    @Schema(description = "Confidence score of the answer", example = "0.85")
    Double confidence,
    
    @Schema(description = "Document chunks used to generate the answer")
    List<DocumentChunkDto> sources,
    
    @Schema(description = "Processing time in milliseconds", example = "1250")
    Long processingTimeMs,
    
    @Schema(description = "When the response was generated")
    LocalDateTime timestamp,
    
    @Schema(description = "Conversation ID if applicable", example = "conv-123")
    String conversationId,
    
    @Schema(description = "Metadata about the query processing")
    QueryMetadata metadata
) {
    
    @Schema(description = "Metadata about query processing")
    public record QueryMetadata(
        
        @Schema(description = "Number of chunks retrieved", example = "5")
        int chunksRetrieved,
        
        @Schema(description = "Model used for generation", example = "gpt-4o-mini")
        String modelUsed,
        
        @Schema(description = "Tokens used in the request", example = "150")
        int tokensUsed,
        
        @Schema(description = "Whether the response was cached", example = "false")
        boolean fromCache,
        
        @Schema(description = "Retrieval strategy used", example = "semantic_similarity")
        String retrievalStrategy
    ) {}
}
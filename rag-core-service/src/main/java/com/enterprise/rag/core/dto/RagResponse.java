package com.enterprise.rag.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.enterprise.rag.shared.dto.DocumentChunkDto;

import java.time.LocalDateTime;
import java.util.List;

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
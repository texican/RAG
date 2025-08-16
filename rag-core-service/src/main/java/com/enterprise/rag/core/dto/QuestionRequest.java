package com.enterprise.rag.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to ask a question using RAG")
public record QuestionRequest(
    
    @Schema(description = "The question to ask", example = "What is the company's vacation policy?")
    @NotBlank(message = "Question cannot be blank")
    @Size(max = 1000, message = "Question cannot exceed 1000 characters")
    String question,
    
    @Schema(description = "Optional conversation ID for context", example = "conv-123")
    String conversationId,
    
    @Schema(description = "Maximum number of document chunks to retrieve", example = "5")
    Integer maxChunks,
    
    @Schema(description = "Minimum similarity threshold for chunks", example = "0.7")
    Double similarityThreshold,
    
    @Schema(description = "Whether to include source citations", example = "true")
    Boolean includeSources
) {
    
    public QuestionRequest {
        // Provide defaults
        if (maxChunks == null) maxChunks = 5;
        if (similarityThreshold == null) similarityThreshold = 0.7;
        if (includeSources == null) includeSources = true;
    }
}
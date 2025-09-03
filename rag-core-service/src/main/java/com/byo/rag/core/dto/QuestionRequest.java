package com.byo.rag.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for asking questions using the RAG (Retrieval Augmented Generation) system.
 * <p>
 * This record represents a simplified question-asking interface for the RAG system,
 * providing an easy-to-use API for basic question answering scenarios. It abstracts
 * the complexity of the full RagQueryRequest for common use cases where users
 * simply want to ask questions about their documents.
 * 
 * <h2>Request Components</h2>
 * <ul>
 *   <li><strong>Question Text</strong> - The user's question in natural language</li>
 *   <li><strong>Conversation Context</strong> - Optional conversation ID for multi-turn dialogue</li>
 *   <li><strong>Retrieval Parameters</strong> - Configuration for document retrieval</li>
 *   <li><strong>Response Options</strong> - Control over response format and content</li>
 * </ul>
 * 
 * <h2>Input Validation</h2>
 * <ul>
 *   <li><strong>Question Validation</strong> - Required, non-blank, max 1000 characters</li>
 *   <li><strong>Conversation ID</strong> - Optional UUID for conversation tracking</li>
 *   <li><strong>Chunk Limits</strong> - Configurable document chunk retrieval limits</li>
 *   <li><strong>Similarity Threshold</strong> - Minimum relevance score for document inclusion</li>
 * </ul>
 * 
 * <h2>Default Behavior</h2>
 * <ul>
 *   <li><strong>Max Chunks</strong> - Defaults to 5 most relevant document chunks</li>
 *   <li><strong>Similarity Threshold</strong> - Defaults to 0.7 for high-quality matches</li>
 *   <li><strong>Include Sources</strong> - Defaults to true for transparency</li>
 *   <li><strong>Response Format</strong> - Standard RAG response with citations</li>
 * </ul>
 * 
 * <h2>Use Cases</h2>
 * <ul>
 *   <li><strong>Simple Q&A</strong> - Direct question answering from documents</li>
 *   <li><strong>Interactive Chat</strong> - Multi-turn conversations with context</li>
 *   <li><strong>Knowledge Discovery</strong> - Exploratory questions about document content</li>
 *   <li><strong>Content Summarization</strong> - Questions that require synthesis</li>
 * </ul>
 * 
 * <h2>API Integration</h2>
 * <ul>
 *   <li><strong>REST Endpoint</strong> - Primary interface for /rag/question endpoint</li>
 *   <li><strong>Frontend Integration</strong> - Simplified API for UI components</li>
 *   <li><strong>Mobile Support</strong> - Optimized for mobile and web applications</li>
 *   <li><strong>Third-Party Integration</strong> - Easy integration for external systems</li>
 * </ul>
 * 
 * <h2>Conversion to RagQueryRequest</h2>
 * This simplified request is internally converted to a full RagQueryRequest with:
 * <ul>
 *   <li>Tenant ID extracted from authentication context</li>
 *   <li>User ID from JWT token claims</li>
 *   <li>Session ID generated automatically</li>
 *   <li>Default retrieval options applied</li>
 * </ul>
 * 
 * @author Enterprise RAG Development Team
 * @version 0.8.0
 * @since 0.1.0
 * @see com.byo.rag.core.dto.RagQueryRequest
 * @see com.byo.rag.core.dto.RagResponse
 */
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
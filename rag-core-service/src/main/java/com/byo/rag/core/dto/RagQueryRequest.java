package com.byo.rag.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for RAG query operations.
 */
public record RagQueryRequest(
    @NotNull
    UUID tenantId,
    
    @NotBlank
    @Size(max = 2000, message = "Query must not exceed 2000 characters")
    String query,
    
    @Size(max = 50)
    String conversationId,
    
    @Size(max = 50) 
    String userId,
    
    @Size(max = 100)
    String sessionId,
    
    List<UUID> documentIds,
    
    Map<String, Object> filters,
    
    RagOptions options
) {
    
    /**
     * RAG processing options.
     */
    public record RagOptions(
        Integer maxChunks,
        Double relevanceThreshold,
        Integer maxTokens,
        Double temperature,
        String llmProvider,
        Boolean streaming,
        Boolean includeMetadata,
        Boolean includeSources,
        String language,
        String intent,
        String systemPrompt
    ) {
        
        /**
         * Default options.
         */
        public static RagOptions defaultOptions() {
            return new RagOptions(
                10,      // maxChunks
                0.7,     // relevanceThreshold
                2000,    // maxTokens
                0.1,     // temperature
                null,    // llmProvider (use default)
                true,    // streaming
                true,    // includeMetadata
                true,    // includeSources
                "en",    // language
                null,    // intent
                null     // systemPrompt
            );
        }
        
        /**
         * Fast response options (fewer chunks, less processing).
         */
        public static RagOptions fastOptions() {
            return new RagOptions(
                5,       // maxChunks
                0.8,     // relevanceThreshold
                1000,    // maxTokens
                0.0,     // temperature
                null,    // llmProvider
                true,    // streaming
                false,   // includeMetadata
                true,    // includeSources
                "en",    // language
                null,    // intent
                null     // systemPrompt
            );
        }
        
        /**
         * Comprehensive options (more chunks, detailed processing).
         */
        public static RagOptions comprehensiveOptions() {
            return new RagOptions(
                15,      // maxChunks
                0.6,     // relevanceThreshold
                3000,    // maxTokens
                0.2,     // temperature
                null,    // llmProvider
                true,    // streaming
                true,    // includeMetadata
                true,    // includeSources
                "en",    // language
                null,    // intent
                null     // systemPrompt
            );
        }
    }
    
    /**
     * Create simple RAG query request.
     */
    public static RagQueryRequest simple(UUID tenantId, String query) {
        return new RagQueryRequest(
            tenantId,
            query,
            null,
            null,
            null,
            null,
            null,
            RagOptions.defaultOptions()
        );
    }
    
    /**
     * Create RAG query request with conversation context.
     */
    public static RagQueryRequest withConversation(UUID tenantId, String query, 
                                                 String conversationId, String userId) {
        return new RagQueryRequest(
            tenantId,
            query,
            conversationId,
            userId,
            null,
            null,
            null,
            RagOptions.defaultOptions()
        );
    }
    
    /**
     * Create RAG query request with document filtering.
     */
    public static RagQueryRequest withDocuments(UUID tenantId, String query, 
                                              List<UUID> documentIds) {
        return new RagQueryRequest(
            tenantId,
            query,
            null,
            null,
            null,
            documentIds,
            null,
            RagOptions.defaultOptions()
        );
    }
    
    /**
     * Create RAG query request with custom options.
     */
    public static RagQueryRequest withOptions(UUID tenantId, String query, 
                                            RagOptions options) {
        return new RagQueryRequest(
            tenantId,
            query,
            null,
            null,
            null,
            null,
            null,
            options
        );
    }
}
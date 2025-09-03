/**
 * Data Transfer Objects for RAG processing operations.
 * 
 * <p>This package contains specialized DTOs for RAG (Retrieval Augmented Generation)
 * operations in the Enterprise RAG System. These DTOs handle complex RAG workflows,
 * query processing, response formatting, conversation management, and streaming
 * operations with comprehensive validation and optimization features.</p>
 * 
 * <h2>DTO Categories</h2>
 * <p>RAG DTOs are organized by functional area:</p>
 * <ul>
 *   <li><strong>Query DTOs</strong> - RAG query requests and processing parameters</li>
 *   <li><strong>Response DTOs</strong> - RAG responses with context and source attribution</li>
 *   <li><strong>Conversation DTOs</strong> - Multi-turn conversation management</li>
 *   <li><strong>Streaming DTOs</strong> - Real-time streaming response components</li>
 *   <li><strong>Context DTOs</strong> - Context assembly and ranking information</li>
 *   <li><strong>Analytics DTOs</strong> - RAG processing metrics and analytics</li>
 * </ul>
 * 
 * <h2>RAG Query Processing DTOs</h2>
 * <p>Comprehensive query processing data structures:</p>
 * <ul>
 *   <li><strong>RAG Query Request</strong> - Complete query processing requests</li>
 *   <li><strong>Query Parameters</strong> - Advanced query configuration and parameters</li>
 *   <li><strong>Search Criteria</strong> - Vector search configuration and filters</li>
 *   <li><strong>Context Requirements</strong> - Context assembly and ranking preferences</li>
 *   <li><strong>LLM Configuration</strong> - Language model selection and parameters</li>
 * </ul>
 * 
 * <h2>RAG Response DTOs</h2>
 * <p>Rich response structures with comprehensive metadata:</p>
 * <ul>
 *   <li><strong>RAG Response</strong> - Complete RAG processing results</li>
 *   <li><strong>Source Attribution</strong> - Detailed source document references</li>
 *   <li><strong>Confidence Metrics</strong> - Response quality and confidence indicators</li>
 *   <li><strong>Processing Metadata</strong> - Pipeline processing information</li>
 *   <li><strong>Quality Indicators</strong> - Response validation and quality metrics</li>
 * </ul>
 * 
 * <h2>Conversation Management DTOs</h2>
 * <p>Advanced conversation handling data structures:</p>
 * <ul>
 *   <li><strong>Conversation Request</strong> - Multi-turn conversation requests</li>
 *   <li><strong>Conversation History</strong> - Historical conversation context</li>
 *   <li><strong>Turn Management</strong> - Individual conversation turn handling</li>
 *   <li><strong>Context Memory</strong> - Conversation context and memory management</li>
 *   <li><strong>Topic Tracking</strong> - Conversation topic and intent tracking</li>
 * </ul>
 * 
 * <h2>Streaming Response DTOs</h2>
 * <p>Real-time streaming response components:</p>
 * <ul>
 *   <li><strong>Stream Chunks</strong> - Individual streaming response chunks</li>
 *   <li><strong>Stream Metadata</strong> - Streaming session information</li>
 *   <li><strong>Progress Indicators</strong> - Processing progress and status</li>
 *   <li><strong>Stream Events</strong> - Streaming event types and payloads</li>
 *   <li><strong>Completion Signals</strong> - Stream completion and finalization</li>
 * </ul>
 * 
 * <h2>Context and Source DTOs</h2>
 * <p>Context assembly and source attribution structures:</p>
 * <ul>
 *   <li><strong>Context Fragments</strong> - Individual context pieces</li>
 *   <li><strong>Source Documents</strong> - Source document metadata and references</li>
 *   <li><strong>Relevance Scores</strong> - Context relevance and ranking information</li>
 *   <li><strong>Citation Information</strong> - Detailed citation and reference data</li>
 *   <li><strong>Context Assembly</strong> - Assembled context with optimization metadata</li>
 * </ul>
 * 
 * <h2>Advanced Validation</h2>
 * <p>Comprehensive validation for RAG operations:</p>
 * <ul>
 *   <li><strong>Query Validation</strong> - Query format and content validation</li>
 *   <li><strong>Parameter Validation</strong> - RAG parameter constraints and limits</li>
 *   <li><strong>Context Validation</strong> - Context size and quality validation</li>
 *   <li><strong>Security Validation</strong> - Input sanitization and security checks</li>
 *   <li><strong>Business Rule Validation</strong> - RAG-specific business rules</li>
 * </ul>
 * 
 * <h2>Performance Optimization</h2>
 * <p>DTOs optimized for RAG processing performance:</p>
 * <ul>
 *   <li><strong>Efficient Serialization</strong> - Optimized JSON serialization for large responses</li>
 *   <li><strong>Streaming Support</strong> - DTOs designed for streaming serialization</li>
 *   <li><strong>Memory Efficiency</strong> - Memory-optimized data structures</li>
 *   <li><strong>Lazy Loading</strong> - Strategic lazy loading for large context assemblies</li>
 * </ul>
 * 
 * <h2>Multi-Tenant Support</h2>
 * <p>DTOs support complete multi-tenant isolation:</p>
 * <ul>
 *   <li><strong>Tenant Context</strong> - Automatic tenant context in all DTOs</li>
 *   <li><strong>Tenant Isolation</strong> - No cross-tenant data exposure</li>
 *   <li><strong>Tenant-Specific Configuration</strong> - Per-tenant RAG configuration</li>
 *   <li><strong>Tenant Analytics</strong> - Tenant-scoped analytics and metrics</li>
 * </ul>
 * 
 * <h2>Analytics and Metrics DTOs</h2>
 * <p>Comprehensive analytics and monitoring data structures:</p>
 * <ul>
 *   <li><strong>Processing Metrics</strong> - RAG pipeline performance metrics</li>
 *   <li><strong>Quality Metrics</strong> - Response quality and accuracy metrics</li>
 *   <li><strong>Usage Analytics</strong> - Query pattern and usage analytics</li>
 *   <li><strong>Cost Tracking</strong> - LLM token usage and cost information</li>
 *   <li><strong>Performance Reports</strong> - Detailed performance analysis data</li>
 * </ul>
 * 
 * <h2>Integration Support</h2>
 * <p>DTOs designed for seamless system integration:</p>
 * <ul>
 *   <li><strong>Service Communication</strong> - DTOs for inter-service communication</li>
 *   <li><strong>Event Publishing</strong> - Event payload DTOs for analytics</li>
 *   <li><strong>External API Integration</strong> - DTOs for external LLM provider APIs</li>
 *   <li><strong>Webhook Payloads</strong> - DTOs for webhook notifications</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * /**
 *  * Comprehensive RAG query request with advanced configuration.
 *  *\/
 * @JsonInclude(JsonInclude.Include.NON_NULL)
 * public record RagQueryRequest(
 *     
 *     @NotBlank(message = "Query text is required")
 *     @Size(max = 10000, message = "Query must not exceed 10,000 characters")
 *     String query,
 *     
 *     @Valid
 *     SearchParameters searchParameters,
 *     
 *     @Valid
 *     ContextParameters contextParameters,
 *     
 *     @Valid
 *     LLMParameters llmParameters,
 *     
 *     String conversationId,
 *     
 *     @Valid
 *     Map<String, Object> metadata
 * ) {
 *     
 *     /**
 *      * Search configuration parameters.
 *      *\/
 *     public record SearchParameters(
 *         @Min(value = 1, message = "Max results must be at least 1")
 *         @Max(value = 100, message = "Max results must not exceed 100")
 *         Integer maxResults,
 *         
 *         @DecimalMin(value = "0.0", message = "Similarity threshold must be non-negative")
 *         @DecimalMax(value = "1.0", message = "Similarity threshold must not exceed 1.0")
 *         Double similarityThreshold,
 *         
 *         Boolean hybridSearchEnabled,
 *         
 *         List<String> documentFilters,
 *         
 *         Map<String, Object> searchMetadata
 *     ) {}
 *     
 *     /**
 *      * Context assembly parameters.
 *      *\/
 *     public record ContextParameters(
 *         @Min(value = 100, message = "Max context length must be at least 100")
 *         @Max(value = 32000, message = "Max context length must not exceed 32,000")
 *         Integer maxContextLength,
 *         
 *         Boolean deduplicationEnabled,
 *         Boolean summarizationEnabled,
 *         
 *         ContextRankingStrategy rankingStrategy,
 *         
 *         Map<String, Object> assemblyOptions
 *     ) {}
 * }
 * 
 * /**
 *  * Comprehensive RAG response with rich metadata and source attribution.
 *  *\/
 * @JsonInclude(JsonInclude.Include.NON_NULL)
 * public record RagResponse(
 *     String queryId,
 *     String query,
 *     String response,
 *     
 *     @Valid
 *     List<SourceAttribution> sources,
 *     
 *     @Valid
 *     QualityMetrics quality,
 *     
 *     @Valid
 *     ProcessingMetrics processing,
 *     
 *     Instant timestamp,
 *     String conversationId
 * ) {
 *     
 *     /**
 *      * Source document attribution with detailed metadata.
 *      *\/
 *     public record SourceAttribution(
 *         String documentId,
 *         String title,
 *         String excerpt,
 *         String url,
 *         Double relevanceScore,
 *         Integer position,
 *         Map<String, Object> metadata
 *     ) {}
 *     
 *     /**
 *      * Response quality and confidence metrics.
 *      *\/
 *     public record QualityMetrics(
 *         Double confidenceScore,
 *         Double relevanceScore,
 *         Boolean hallucinationDetected,
 *         Boolean factsVerified,
 *         Map<String, Double> qualityScores
 *     ) {}
 *     
 *     /**
 *      * RAG processing pipeline metrics.
 *      *\/
 *     public record ProcessingMetrics(
 *         Duration totalProcessingTime,
 *         Duration searchTime,
 *         Duration contextAssemblyTime,
 *         Duration llmProcessingTime,
 *         Integer tokensUsed,
 *         Integer contextLength,
 *         String llmModel
 *     ) {}
 * }
 * }</pre>
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 * @see javax.validation.constraints Validation annotations
 * @see com.fasterxml.jackson.annotation Jackson JSON annotations
 * @see com.byo.rag.shared.dto Shared DTO components
 * @see com.byo.rag.core.controller RAG controllers
 */
package com.byo.rag.core.dto;
package com.byo.rag.core.service;

import com.byo.rag.core.client.EmbeddingServiceClient;
import com.byo.rag.core.dto.RagQueryRequest;
import com.byo.rag.core.dto.RagQueryResponse;
import com.byo.rag.core.dto.RagResponse;
import com.byo.rag.core.dto.RagQueryResponse.RagMetrics;
import com.byo.rag.core.dto.RagQueryResponse.SourceDocument;
import com.byo.rag.shared.exception.RagException;
import org.slf4j.Logger;
import java.util.UUID;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Main RAG service orchestrating the complete Retrieval Augmented Generation pipeline.
 * 
 * <p>This service is the core component of the Enterprise RAG system, orchestrating
 * the complete flow from query processing to response generation. It implements
 * a multi-stage pipeline that includes query optimization, document retrieval,
 * context assembly, and LLM-based response generation.</p>
 * 
 * <p>Pipeline stages:</p>
 * <ol>
 *   <li><strong>Cache Check:</strong> Attempts to return cached responses for performance</li>
 *   <li><strong>Query Optimization:</strong> Enhances queries for better retrieval results</li>
 *   <li><strong>Context Retrieval:</strong> Gets conversation history if applicable</li>
 *   <li><strong>Semantic Search:</strong> Retrieves relevant document chunks using embeddings</li>
 *   <li><strong>Context Assembly:</strong> Combines retrieved chunks into coherent context</li>
 *   <li><strong>Response Generation:</strong> Uses LLM to generate final response</li>
 *   <li><strong>Conversation Update:</strong> Stores exchange for future context</li>
 *   <li><strong>Response Caching:</strong> Caches response for performance optimization</li>
 * </ol>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Multi-tenant isolation and security</li>
 *   <li>Comprehensive caching strategy for performance</li>
 *   <li>Streaming responses for real-time user experience</li>
 *   <li>Detailed metrics and monitoring</li>
 *   <li>Conversation context management</li>
 *   <li>Configurable retrieval and generation parameters</li>
 * </ul>
 * 
 * <p>Thread Safety: This service is thread-safe and can handle concurrent requests
 * from multiple tenants simultaneously.</p>
 * 
 * @author Enterprise RAG Development Team
 * @since 1.0.0
 * @version 1.0
 * @see RagQueryRequest
 * @see RagQueryResponse
 * @see EmbeddingServiceClient
 * @see LLMIntegrationService
 */
@Service
public class RagService {

    private static final Logger logger = LoggerFactory.getLogger(RagService.class);
    
    private final EmbeddingServiceClient embeddingClient;
    private final ContextAssemblyService contextService;
    private final LLMIntegrationService llmService;
    private final ConversationService conversationService;
    private final CacheService cacheService;
    private final QueryOptimizationService queryOptimizationService;
    
    public RagService(
            EmbeddingServiceClient embeddingClient,
            ContextAssemblyService contextService,
            LLMIntegrationService llmService,
            ConversationService conversationService,
            CacheService cacheService,
            QueryOptimizationService queryOptimizationService) {
        this.embeddingClient = embeddingClient;
        this.contextService = contextService;
        this.llmService = llmService;
        this.conversationService = conversationService;
        this.cacheService = cacheService;
        this.queryOptimizationService = queryOptimizationService;
    }
    
    /**
     * Process RAG query and return complete response.
     */
    public RagQueryResponse processQuery(RagQueryRequest request) {
        // Validate required parameters
        if (request == null) {
            throw new NullPointerException("RagQueryRequest cannot be null");
        }
        if (request.tenantId() == null) {
            throw new NullPointerException("Tenant ID cannot be null");
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("Processing RAG query for tenant: {}, conversation: {}", 
                       request.tenantId(), request.conversationId());
            
            // Step 1: Check cache for similar queries
            String cacheKey = generateCacheKey(request);
            RagResponse cachedResponse = cacheService.getResponse(cacheKey);
            if (cachedResponse != null) {
                logger.info("Returning cached response for tenant: {}", request.tenantId());
                return convertToQueryResponse(cachedResponse, request);
            }
            
            // Step 2: Query preprocessing and optimization
            RagQueryRequest optimizedRequest = queryOptimizationService.optimizeQuery(request);
            
            // Step 3: Retrieve conversation context if needed
            String contextualizedQuery = optimizedRequest.query();
            if (request.conversationId() != null) {
                contextualizedQuery = conversationService.contextualizeQuery(
                    request.conversationId(), optimizedRequest.query());
            }
            
            // Step 4: Retrieve relevant documents using semantic search
            long retrievalStartTime = System.currentTimeMillis();
            List<SourceDocument> retrievedDocuments = retrieveRelevantDocuments(
                optimizedRequest, contextualizedQuery);
            long retrievalTime = System.currentTimeMillis() - retrievalStartTime;
            
            if (retrievedDocuments.isEmpty()) {
                logger.info("No relevant documents found for query in tenant: {}", 
                           request.tenantId());
                           
                RagMetrics metrics = RagMetrics.withTiming(
                    retrievalTime, 0, 0, 0, 0);
                
                return RagQueryResponse.empty(
                    request.tenantId(), request.query(), 
                    request.conversationId(), metrics);
            }
            
            // Step 5: Assemble context from retrieved documents
            long contextStartTime = System.currentTimeMillis();
            String assembledContext = contextService.assembleContext(
                retrievedDocuments, optimizedRequest);
            long contextTime = System.currentTimeMillis() - contextStartTime;
            
            // Step 6: Generate response using LLM
            long generationStartTime = System.currentTimeMillis();
            String generatedResponse = llmService.generateResponse(
                contextualizedQuery, assembledContext, optimizedRequest);
            long generationTime = System.currentTimeMillis() - generationStartTime;
            
            // Step 7: Update conversation history
            if (request.conversationId() != null && request.userId() != null) {
                conversationService.addExchange(
                    request.conversationId(),
                    UUID.fromString(request.userId()),
                    request.query(),
                    generatedResponse,
                    retrievedDocuments
                );
            }
            
            // Step 8: Create response with metrics
            RagMetrics metrics = RagMetrics.comprehensive(
                retrievalTime,
                contextTime,
                generationTime,
                retrievedDocuments.size(),
                retrievedDocuments.size(),
                estimateTokenCount(generatedResponse),
                calculateAverageRelevance(retrievedDocuments),
                llmService.getProviderUsed(),
                "default" // TODO: Get actual embedding model used
            );
            
            RagQueryResponse response = RagQueryResponse.success(
                request.tenantId(),
                request.query(),
                request.conversationId(),
                generatedResponse,
                retrievedDocuments,
                metrics
            );
            
            // Step 9: Cache the response
            RagResponse cacheResponse = convertToRagResponse(response);
            cacheService.cacheResponse(cacheKey, cacheResponse);
            
            long totalTime = System.currentTimeMillis() - startTime;
            logger.info("RAG query processed successfully for tenant: {} in {}ms", 
                       request.tenantId(), totalTime);
            
            return response;
            
        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            logger.error("Failed to process RAG query for tenant: {}", 
                        request.tenantId(), e);
            
            RagMetrics errorMetrics = RagMetrics.withTiming(0, 0, 0, 0, 0);
            
            return RagQueryResponse.failure(
                request.tenantId(),
                request.query(),
                request.conversationId(),
                e.getMessage(),
                errorMetrics
            );
        }
    }
    
    /**
     * Process RAG query asynchronously.
     */
    public CompletableFuture<RagQueryResponse> processQueryAsync(RagQueryRequest request) {
        return CompletableFuture.supplyAsync(() -> processQuery(request));
    }
    
    /**
     * Process RAG query with streaming response.
     */
    public Flux<String> processQueryStreaming(RagQueryRequest request) {
        return Mono.fromCallable(() -> processQuery(request))
            .flatMapMany(response -> {
                if ("SUCCESS".equals(response.status())) {
                    return llmService.generateResponseStreaming(
                        request.query(),
                        contextService.assembleContext(response.sources(), request),
                        request
                    );
                } else {
                    return Flux.just("Error: " + response.error());
                }
            })
            .onErrorResume(error -> {
                logger.error("Streaming RAG query failed for tenant: {}", 
                            request.tenantId(), error);
                return Flux.just("Error: " + error.getMessage());
            });
    }
    
    /**
     * Get RAG processing statistics.
     */
    public RagStats getStats(String tenantId) {
        // TODO: Implement comprehensive statistics gathering
        return new RagStats(
            0L,    // totalQueries
            0L,    // successfulQueries
            0L,    // cachedQueries
            0.0,   // averageResponseTime
            0.0,   // averageRelevanceScore
            null   // providerStats
        );
    }
    
    private List<SourceDocument> retrieveRelevantDocuments(RagQueryRequest request, 
                                                          String contextualizedQuery) {
        try {
            EmbeddingServiceClient.SearchRequest searchRequest = 
                new EmbeddingServiceClient.SearchRequest(
                    request.tenantId(),
                    contextualizedQuery,
                    request.options() != null ? request.options().maxChunks() : 10,
                    request.options() != null ? request.options().relevanceThreshold() : 0.7,
                    null, // Use default embedding model
                    request.documentIds(),
                    request.filters(),
                    true,  // includeContent
                    true   // includeMetadata
                );
            
            EmbeddingServiceClient.SearchResponse searchResponse = 
                embeddingClient.search(searchRequest, request.tenantId());
            
            return searchResponse.results().stream()
                .map(result -> SourceDocument.withMetadata(
                    result.documentId(),
                    result.chunkId(),
                    result.documentTitle(),
                    result.content(),
                    result.score(),
                    result.metadata(),
                    result.documentType(),
                    null // createdAt would need to be mapped from metadata
                ))
                .toList();
                
        } catch (Exception e) {
            logger.error("Failed to retrieve relevant documents for tenant: {}", 
                        request.tenantId(), e);
            throw new RagException("Document retrieval failed", e);
        }
    }
    
    private int estimateTokenCount(String text) {
        // Simple estimation: ~4 characters per token
        return text.length() / 4;
    }
    
    private double calculateAverageRelevance(List<SourceDocument> documents) {
        return documents.stream()
            .mapToDouble(SourceDocument::relevanceScore)
            .average()
            .orElse(0.0);
    }
    
    /**
     * Generate cache key for a RAG query request.
     */
    private String generateCacheKey(RagQueryRequest request) {
        return "rag:" + request.tenantId() + ":" + 
               Integer.toHexString(request.query().hashCode());
    }
    
    /**
     * Convert RagResponse to RagQueryResponse.
     */
    private RagQueryResponse convertToQueryResponse(RagResponse ragResponse, RagQueryRequest request) {
        // Convert sources from DocumentChunkDto to SourceDocument
        List<SourceDocument> sources = ragResponse.sources().stream()
            .map(chunk -> new SourceDocument(
                UUID.randomUUID(), // documentId (not available in chunk)
                chunk.id(),
                chunk.documentFilename() != null ? chunk.documentFilename() : "Unknown",
                chunk.content(),
                1.0, // Default relevance score
                chunk.metadata(),
                null, // documentType
                java.time.Instant.now() // createdAt (not available in chunk)
            ))
            .toList();
            
        // Convert metadata
        RagMetrics metrics = new RagMetrics(
            ragResponse.processingTimeMs(),
            0L, // retrievalTimeMs
            0L, // contextAssemblyTimeMs
            0L, // generationTimeMs
            ragResponse.metadata().chunksRetrieved(),
            ragResponse.metadata().chunksRetrieved(), // chunksUsed = chunksRetrieved
            ragResponse.metadata().tokensUsed(),
            0.0, // averageRelevanceScore
            ragResponse.metadata().modelUsed(),
            null // embeddingModel
        );
        
        return new RagQueryResponse(
            request.tenantId(),
            request.query(),
            ragResponse.conversationId(),
            ragResponse.answer(),
            sources,
            metrics,
            "SUCCESS",
            null,
            ragResponse.timestamp().atZone(java.time.ZoneOffset.UTC).toInstant()
        );
    }
    
    /**
     * Convert RagQueryResponse to RagResponse for caching.
     */
    private RagResponse convertToRagResponse(RagQueryResponse queryResponse) {
        // Convert sources from SourceDocument to DocumentChunkDto
        List<com.byo.rag.shared.dto.DocumentChunkDto> sources = queryResponse.sources().stream()
            .map(source -> new com.byo.rag.shared.dto.DocumentChunkDto(
                source.chunkId(),
                source.content(),
                1, // sequenceNumber (default)
                null, // startIndex
                null, // endIndex
                null, // tokenCount
                source.title(),
                source.metadata()
            ))
            .toList();
        
        // Convert metadata
        RagResponse.QueryMetadata metadata = new RagResponse.QueryMetadata(
            queryResponse.metrics().chunksRetrieved(),
            queryResponse.metrics().llmProvider(),
            queryResponse.metrics().tokensGenerated(),
            false, // fromCache
            "semantic_similarity" // retrievalStrategy
        );
        
        return new RagResponse(
            queryResponse.response(),
            1.0, // confidence
            sources,
            queryResponse.metrics().totalProcessingTimeMs(),
            queryResponse.createdAt().atZone(java.time.ZoneOffset.UTC).toLocalDateTime(),
            queryResponse.conversationId(),
            metadata
        );
    }
    
    /**
     * RAG processing statistics.
     */
    public record RagStats(
        long totalQueries,
        long successfulQueries,
        long cachedQueries,
        double averageResponseTimeMs,
        double averageRelevanceScore,
        Object providerStats
    ) {}
}
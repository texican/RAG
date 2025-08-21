package com.enterprise.rag.core.service;

import com.enterprise.rag.core.client.EmbeddingServiceClient;
import com.enterprise.rag.core.dto.RagQueryRequest;
import com.enterprise.rag.core.dto.RagQueryResponse;
import com.enterprise.rag.core.dto.RagQueryResponse.RagMetrics;
import com.enterprise.rag.core.dto.RagQueryResponse.SourceDocument;
import com.enterprise.rag.shared.exception.RagException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Main RAG service orchestrating the complete RAG pipeline.
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
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("Processing RAG query for tenant: {}, conversation: {}", 
                       request.tenantId(), request.conversationId());
            
            // Step 1: Check cache for similar queries
            RagQueryResponse cachedResponse = cacheService.getCachedResponse(request);
            if (cachedResponse != null) {
                logger.info("Returning cached response for tenant: {}", request.tenantId());
                return cachedResponse;
            }
            
            // Step 2: Query preprocessing and optimization
            long queryOptStartTime = System.currentTimeMillis();
            RagQueryRequest optimizedRequest = queryOptimizationService.optimizeQuery(request);
            long queryOptTime = System.currentTimeMillis() - queryOptStartTime;
            
            // Step 3: Retrieve conversation context if needed
            String contextualizedQuery = optimizedRequest.query();
            if (optimizedRequest.conversationId() != null) {
                contextualizedQuery = conversationService.contextualizeQuery(
                    optimizedRequest.conversationId(), optimizedRequest.query());
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
            if (optimizedRequest.conversationId() != null) {
                conversationService.addExchange(
                    optimizedRequest.conversationId(),
                    optimizedRequest.userId(),
                    optimizedRequest.query(),
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
            cacheService.cacheResponse(request, response);
            
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
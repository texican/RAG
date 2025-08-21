package com.enterprise.rag.embedding.service;

import com.enterprise.rag.embedding.dto.SearchRequest;
import com.enterprise.rag.embedding.dto.SearchResponse;
import com.enterprise.rag.shared.exception.EmbeddingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for performing vector similarity searches.
 */
@Service
public class SimilaritySearchService {

    private static final Logger logger = LoggerFactory.getLogger(SimilaritySearchService.class);
    
    private final EmbeddingService embeddingService;
    private final VectorStorageService vectorStorageService;
    private final EmbeddingCacheService cacheService;
    private final ExecutorService executorService;
    
    public SimilaritySearchService(
            EmbeddingService embeddingService,
            VectorStorageService vectorStorageService,
            EmbeddingCacheService cacheService) {
        this.embeddingService = embeddingService;
        this.vectorStorageService = vectorStorageService;
        this.cacheService = cacheService;
        this.executorService = Executors.newFixedThreadPool(4);
    }
    
    /**
     * Perform semantic search using vector similarity.
     */
    public SearchResponse search(SearchRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            logger.debug("Performing similarity search for tenant: {}, query: '{}'", 
                        request.tenantId(), truncateQuery(request.query()));
            
            // Generate query embedding
            String modelName = getEffectiveModelName(request.modelName());
            List<Float> queryEmbedding = generateQueryEmbedding(
                request.tenantId(), request.query(), modelName);
            
            // Search for similar vectors
            SearchResponse response = vectorStorageService.searchSimilar(request, queryEmbedding);
            
            long totalTime = System.currentTimeMillis() - startTime;
            
            logger.info("Completed similarity search for tenant: {} in {}ms, found {} results", 
                       request.tenantId(), totalTime, response.totalResults());
            
            return response;
            
        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            logger.error("Failed similarity search for tenant: {}, query: '{}'", 
                        request.tenantId(), truncateQuery(request.query()), e);
            
            return SearchResponse.empty(
                request.tenantId(), 
                request.query(), 
                getEffectiveModelName(request.modelName()),
                totalTime
            );
        }
    }
    
    /**
     * Perform asynchronous similarity search.
     */
    public CompletableFuture<SearchResponse> searchAsync(SearchRequest request) {
        return CompletableFuture.supplyAsync(() -> search(request), executorService);
    }
    
    /**
     * Perform batch similarity search for multiple queries.
     */
    public List<SearchResponse> searchBatch(List<SearchRequest> requests) {
        logger.info("Performing batch similarity search for {} queries", requests.size());
        
        return requests.parallelStream()
            .map(this::search)
            .toList();
    }
    
    /**
     * Perform hybrid search combining keyword and vector similarity.
     */
    public SearchResponse hybridSearch(SearchRequest request, List<String> keywords) {
        logger.debug("Performing hybrid search for tenant: {}, query: '{}', keywords: {}", 
                    request.tenantId(), truncateQuery(request.query()), keywords);
        
        try {
            // First perform vector similarity search
            SearchResponse vectorResults = search(request);
            
            // TODO: Implement keyword search and result fusion
            // This would involve:
            // 1. Performing keyword search on text content
            // 2. Fusing results using reciprocal rank fusion or similar
            // 3. Re-ranking based on combined scores
            
            // For now, return vector results with hybrid flag in metadata
            logger.info("Hybrid search completed for tenant: {} with {} vector results", 
                       request.tenantId(), vectorResults.totalResults());
            
            return vectorResults;
            
        } catch (Exception e) {
            logger.error("Failed hybrid search for tenant: {}", request.tenantId(), e);
            return SearchResponse.empty(
                request.tenantId(), 
                request.query(), 
                getEffectiveModelName(request.modelName()),
                System.currentTimeMillis()
            );
        }
    }
    
    /**
     * Find similar documents to a given document.
     */
    public SearchResponse findSimilarDocuments(UUID tenantId, UUID documentId, 
                                             int topK, String modelName) {
        try {
            // TODO: Implement document-to-document similarity
            // This would involve:
            // 1. Retrieving document vectors
            // 2. Computing average or representative vector
            // 3. Searching for similar vectors
            
            logger.info("Finding similar documents to {} for tenant: {}", documentId, tenantId);
            
            // Placeholder implementation
            SearchRequest request = SearchRequest.simple(tenantId, "");
            return SearchResponse.empty(tenantId, "", getEffectiveModelName(modelName), 0);
            
        } catch (Exception e) {
            logger.error("Failed to find similar documents for: {} in tenant: {}", 
                        documentId, tenantId, e);
            return SearchResponse.empty(tenantId, "", getEffectiveModelName(modelName), 0);
        }
    }
    
    /**
     * Get search recommendations based on user history.
     */
    public List<String> getSearchRecommendations(UUID tenantId, UUID userId, int limit) {
        try {
            // TODO: Implement search recommendations based on:
            // 1. User search history
            // 2. Popular searches in tenant
            // 3. Related queries from similar users
            
            logger.debug("Getting search recommendations for user: {} in tenant: {}", 
                        userId, tenantId);
            
            // Placeholder implementation
            return List.of();
            
        } catch (Exception e) {
            logger.error("Failed to get search recommendations for user: {} in tenant: {}", 
                        userId, tenantId, e);
            return List.of();
        }
    }
    
    /**
     * Analyze search query and suggest improvements.
     */
    public SearchAnalysis analyzeQuery(String query) {
        try {
            // Basic query analysis
            int wordCount = query.trim().split("\\s+").length;
            boolean hasQuestionWords = containsQuestionWords(query);
            boolean tooShort = query.length() < 10;
            boolean tooLong = query.length() > 500;
            
            List<String> suggestions = List.of();
            if (tooShort) {
                suggestions = List.of("Consider adding more specific details to your query");
            } else if (tooLong) {
                suggestions = List.of("Try to make your query more concise");
            }
            
            return new SearchAnalysis(
                wordCount,
                hasQuestionWords,
                tooShort,
                tooLong,
                suggestions
            );
            
        } catch (Exception e) {
            logger.error("Failed to analyze query: '{}'", truncateQuery(query), e);
            return new SearchAnalysis(0, false, false, false, List.of());
        }
    }
    
    private List<Float> generateQueryEmbedding(UUID tenantId, String query, String modelName) {
        try {
            // Check cache first
            List<Float> cachedEmbedding = cacheService.getCachedEmbedding(tenantId, query, modelName);
            if (cachedEmbedding != null) {
                logger.debug("Using cached embedding for query");
                return cachedEmbedding;
            }
            
            // Generate new embedding
            List<Float> embedding = embeddingService.generateEmbedding(tenantId, query, modelName);
            
            // Cache the result
            cacheService.cacheEmbedding(tenantId, query, modelName, embedding);
            
            return embedding;
            
        } catch (Exception e) {
            logger.error("Failed to generate query embedding for tenant: {}", tenantId, e);
            throw new EmbeddingException("Failed to generate query embedding", e);
        }
    }
    
    private String getEffectiveModelName(String requestedModel) {
        // Default model selection logic
        return requestedModel != null && !requestedModel.isEmpty() ? 
            requestedModel : "openai-text-embedding-3-small";
    }
    
    private String truncateQuery(String query) {
        return query.length() > 100 ? query.substring(0, 100) + "..." : query;
    }
    
    private boolean containsQuestionWords(String query) {
        String lowerQuery = query.toLowerCase();
        return lowerQuery.contains("what") || lowerQuery.contains("how") || 
               lowerQuery.contains("why") || lowerQuery.contains("when") ||
               lowerQuery.contains("where") || lowerQuery.contains("who") ||
               lowerQuery.contains("which") || lowerQuery.endsWith("?");
    }
    
    /**
     * Query analysis result.
     */
    public record SearchAnalysis(
        int wordCount,
        boolean hasQuestionWords,
        boolean tooShort,
        boolean tooLong,
        List<String> suggestions
    ) {}
}
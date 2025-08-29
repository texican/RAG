package com.enterprise.rag.embedding.controller;

import com.enterprise.rag.embedding.dto.EmbeddingRequest;
import com.enterprise.rag.embedding.dto.EmbeddingResponse;
import com.enterprise.rag.embedding.dto.SearchRequest;
import com.enterprise.rag.embedding.dto.SearchResponse;
import com.enterprise.rag.embedding.service.EmbeddingCacheService;
import com.enterprise.rag.embedding.service.EmbeddingService;
import com.enterprise.rag.embedding.service.SimilaritySearchService;
import com.enterprise.rag.embedding.service.VectorStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Enterprise-grade REST controller for comprehensive vector embedding and semantic search operations.
 * 
 * <p>This controller provides the complete vector operations interface for the Enterprise RAG system,
 * including embedding generation, semantic similarity search, hybrid search capabilities, and vector
 * storage management. All operations are multi-tenant aware with comprehensive security and validation.</p>
 * 
 * <p><strong>Core Capabilities:</strong></p>
 * <ul>
 *   <li><strong>Embedding Generation:</strong> Text-to-vector conversion using multiple AI models</li>
 *   <li><strong>Semantic Search:</strong> High-performance similarity search with configurable parameters</li>
 *   <li><strong>Hybrid Search:</strong> Combined semantic and keyword search for enhanced relevance</li>
 *   <li><strong>Batch Operations:</strong> Efficient bulk processing for enterprise-scale workloads</li>
 *   <li><strong>Async Processing:</strong> Non-blocking operations with CompletableFuture support</li>
 *   <li><strong>Vector Management:</strong> Complete vector lifecycle and storage operations</li>
 * </ul>
 * 
 * <p><strong>Multi-Tenant Architecture:</strong></p>
 * <ul>
 *   <li>Complete vector isolation between tenant organizations</li>
 *   <li>Tenant-specific embedding models and configurations</li>
 *   <li>Per-tenant caching and performance optimization</li>
 *   <li>Secure access control through X-Tenant-ID header validation</li>
 * </ul>
 * 
 * <p><strong>Supported AI Models:</strong></p>
 * <ul>
 *   <li><strong>OpenAI Models:</strong> text-embedding-3-small, text-embedding-3-large</li>
 *   <li><strong>Sentence Transformers:</strong> all-MiniLM-L6-v2 for local processing</li>
 *   <li><strong>Custom Models:</strong> Extensible architecture for additional embedding models</li>
 * </ul>
 * 
 * <p><strong>Search Features:</strong></p>
 * <ul>
 *   <li><strong>Semantic Search:</strong> Vector similarity with cosine distance</li>
 *   <li><strong>Keyword Search:</strong> Traditional text matching capabilities</li>
 *   <li><strong>Hybrid Search:</strong> Weighted combination of semantic and keyword results</li>
 *   <li><strong>Filtered Search:</strong> Document-specific and metadata-based filtering</li>
 *   <li><strong>Batch Search:</strong> Multiple queries processed efficiently</li>
 * </ul>
 * 
 * <p><strong>Performance Features:</strong></p>
 * <ul>
 *   <li>Redis-based vector caching for enhanced performance</li>
 *   <li>Asynchronous processing with CompletableFuture</li>
 *   <li>Batch operations for reduced API overhead</li>
 *   <li>Configurable search thresholds and result limits</li>
 *   <li>Real-time statistics and performance monitoring</li>
 * </ul>
 * 
 * <p><strong>Integration Points:</strong></p>
 * <ul>
 *   <li>{@link EmbeddingService} - Core embedding generation and AI model integration</li>
 *   <li>{@link SimilaritySearchService} - Advanced search algorithms and result ranking</li>
 *   <li>{@link VectorStorageService} - Redis-based vector storage and retrieval</li>
 *   <li>{@link EmbeddingCacheService} - Performance optimization through intelligent caching</li>
 * </ul>
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 * @see EmbeddingService
 * @see SimilaritySearchService
 * @see VectorStorageService
 */
@RestController
@RequestMapping("/api/v1/embeddings")
@Tag(name = "Embedding Operations", description = "Vector operations and similarity search")
@Validated
public class EmbeddingController {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingController.class);
    
    private final EmbeddingService embeddingService;
    private final SimilaritySearchService searchService;
    private final VectorStorageService vectorStorageService;
    private final EmbeddingCacheService cacheService;
    
    public EmbeddingController(
            EmbeddingService embeddingService,
            SimilaritySearchService searchService,
            VectorStorageService vectorStorageService,
            EmbeddingCacheService cacheService) {
        this.embeddingService = embeddingService;
        this.searchService = searchService;
        this.vectorStorageService = vectorStorageService;
        this.cacheService = cacheService;
    }
    
    @PostMapping("/generate")
    @Operation(summary = "Generate embeddings for text chunks")
    public ResponseEntity<EmbeddingResponse> generateEmbeddings(
            @Valid @RequestBody EmbeddingRequest request,
            @RequestHeader("X-Tenant-ID") 
            @Parameter(description = "Tenant identifier") UUID tenantId) {
        
        logger.info("Generating embeddings for {} texts in tenant: {}", 
                   request.texts().size(), tenantId);
        
        // Ensure tenant ID matches request
        EmbeddingRequest validatedRequest = new EmbeddingRequest(
            tenantId,
            request.texts(),
            request.modelName(),
            request.documentId(),
            request.chunkIds()
        );
        
        EmbeddingResponse response = embeddingService.generateEmbeddings(validatedRequest);
        
        logger.info("Generated embeddings with status: {} for tenant: {}", 
                   response.status(), tenantId);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/generate/async")
    @Operation(summary = "Generate embeddings asynchronously")
    public ResponseEntity<CompletableFuture<EmbeddingResponse>> generateEmbeddingsAsync(
            @Valid @RequestBody EmbeddingRequest request,
            @RequestHeader("X-Tenant-ID") 
            @Parameter(description = "Tenant identifier") UUID tenantId) {
        
        logger.info("Starting async embedding generation for {} texts in tenant: {}", 
                   request.texts().size(), tenantId);
        
        // Ensure tenant ID matches request
        EmbeddingRequest validatedRequest = new EmbeddingRequest(
            tenantId,
            request.texts(),
            request.modelName(),
            request.documentId(),
            request.chunkIds()
        );
        
        CompletableFuture<EmbeddingResponse> future = 
            embeddingService.generateEmbeddingsAsync(validatedRequest);
        
        return ResponseEntity.accepted().body(future);
    }
    
    @PostMapping("/search")
    @Operation(summary = "Perform semantic similarity search")
    public ResponseEntity<SearchResponse> search(
            @Valid @RequestBody SearchRequest request,
            @RequestHeader("X-Tenant-ID") 
            @Parameter(description = "Tenant identifier") UUID tenantId) {
        
        logger.info("Performing similarity search for tenant: {}, query: '{}'", 
                   tenantId, truncateQuery(request.query()));
        
        // Ensure tenant ID matches request
        SearchRequest validatedRequest = new SearchRequest(
            tenantId,
            request.query(),
            request.topK(),
            request.threshold(),
            request.modelName(),
            request.documentIds(),
            request.filters(),
            request.includeContent(),
            request.includeMetadata()
        );
        
        SearchResponse response = searchService.search(validatedRequest);
        
        logger.info("Similarity search completed for tenant: {}, found {} results", 
                   tenantId, response.totalResults());
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/search/async")
    @Operation(summary = "Perform asynchronous similarity search")
    public ResponseEntity<CompletableFuture<SearchResponse>> searchAsync(
            @Valid @RequestBody SearchRequest request,
            @RequestHeader("X-Tenant-ID") 
            @Parameter(description = "Tenant identifier") UUID tenantId) {
        
        logger.info("Starting async similarity search for tenant: {}", tenantId);
        
        // Ensure tenant ID matches request
        SearchRequest validatedRequest = new SearchRequest(
            tenantId,
            request.query(),
            request.topK(),
            request.threshold(),
            request.modelName(),
            request.documentIds(),
            request.filters(),
            request.includeContent(),
            request.includeMetadata()
        );
        
        CompletableFuture<SearchResponse> future = searchService.searchAsync(validatedRequest);
        
        return ResponseEntity.accepted().body(future);
    }
    
    @PostMapping("/search/batch")
    @Operation(summary = "Perform batch similarity search")
    public ResponseEntity<List<SearchResponse>> searchBatch(
            @Valid @RequestBody List<SearchRequest> requests,
            @RequestHeader("X-Tenant-ID") 
            @Parameter(description = "Tenant identifier") UUID tenantId) {
        
        logger.info("Performing batch similarity search for {} queries in tenant: {}", 
                   requests.size(), tenantId);
        
        // Validate and update tenant IDs
        List<SearchRequest> validatedRequests = requests.stream()
            .map(req -> new SearchRequest(
                tenantId,
                req.query(),
                req.topK(),
                req.threshold(),
                req.modelName(),
                req.documentIds(),
                req.filters(),
                req.includeContent(),
                req.includeMetadata()
            ))
            .toList();
        
        List<SearchResponse> responses = searchService.searchBatch(validatedRequests);
        
        logger.info("Batch similarity search completed for tenant: {}, {} results", 
                   tenantId, responses.size());
        
        return ResponseEntity.ok(responses);
    }
    
    @PostMapping("/search/hybrid")
    @Operation(summary = "Perform hybrid search combining semantic and keyword search")
    public ResponseEntity<SearchResponse> hybridSearch(
            @Valid @RequestBody SearchRequest request,
            @RequestParam(required = false) List<String> keywords,
            @RequestHeader("X-Tenant-ID") 
            @Parameter(description = "Tenant identifier") UUID tenantId) {
        
        logger.info("Performing hybrid search for tenant: {}", tenantId);
        
        // Ensure tenant ID matches request
        SearchRequest validatedRequest = new SearchRequest(
            tenantId,
            request.query(),
            request.topK(),
            request.threshold(),
            request.modelName(),
            request.documentIds(),
            request.filters(),
            request.includeContent(),
            request.includeMetadata()
        );
        
        SearchResponse response = searchService.hybridSearch(validatedRequest, keywords);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/similar/{documentId}")
    @Operation(summary = "Find documents similar to a given document")
    public ResponseEntity<SearchResponse> findSimilarDocuments(
            @PathVariable UUID documentId,
            @RequestParam(defaultValue = "10") int topK,
            @RequestParam(required = false) String modelName,
            @RequestHeader("X-Tenant-ID") 
            @Parameter(description = "Tenant identifier") UUID tenantId) {
        
        logger.info("Finding similar documents to {} for tenant: {}", documentId, tenantId);
        
        SearchResponse response = searchService.findSimilarDocuments(
            tenantId, documentId, topK, modelName);
        
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/documents/{documentId}")
    @Operation(summary = "Delete all vectors for a document")
    public ResponseEntity<Void> deleteDocumentVectors(
            @PathVariable UUID documentId,
            @RequestParam(required = false) String modelName,
            @RequestHeader("X-Tenant-ID") 
            @Parameter(description = "Tenant identifier") UUID tenantId) {
        
        logger.info("Deleting vectors for document: {} in tenant: {}", documentId, tenantId);
        
        vectorStorageService.deleteVectors(tenantId, modelName);
        
        logger.info("Successfully deleted vectors for document: {}", documentId);
        
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/stats")
    @Operation(summary = "Get vector storage statistics")
    public ResponseEntity<Map<String, Object>> getStats(
            @RequestParam(required = false) String modelName,
            @RequestHeader("X-Tenant-ID") 
            @Parameter(description = "Tenant identifier") UUID tenantId) {
        
        VectorStorageService.VectorStats vectorStats = 
            vectorStorageService.getStats();
        
        EmbeddingCacheService.CacheStats cacheStats = cacheService.getCacheStats();
        
        Map<String, Object> stats = Map.of(
            "tenant_id", tenantId,
            "model_name", modelName != null ? modelName : "default",
            "vector_storage", Map.of(
                "total_vectors", vectorStats.totalVectors(),
                "memory_usage_mb", vectorStats.memoryUsageMB(),
                "average_vector_size", vectorStats.averageVectorSize(),
                "last_updated", vectorStats.lastUpdated()
            ),
            "cache", Map.of(
                "total_cached_items", cacheStats.totalCachedItems(),
                "ttl_seconds", cacheStats.ttlSeconds()
            )
        );
        
        return ResponseEntity.ok(stats);
    }
    
    @DeleteMapping("/cache")
    @Operation(summary = "Invalidate embedding cache for tenant")
    public ResponseEntity<Void> invalidateCache(
            @RequestParam(required = false) String modelName,
            @RequestHeader("X-Tenant-ID") 
            @Parameter(description = "Tenant identifier") UUID tenantId) {
        
        logger.info("Invalidating cache for tenant: {}, model: {}", tenantId, modelName);
        
        if (modelName != null) {
            cacheService.invalidateModelCache(modelName);
        } else {
            cacheService.invalidateTenantCache(tenantId);
        }
        
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/models")
    @Operation(summary = "Get available embedding models")
    public ResponseEntity<Map<String, Object>> getAvailableModels() {
        // TODO: Get actual available models from registry
        Map<String, Object> models = Map.of(
            "default", "openai-text-embedding-3-small",
            "available_models", List.of(
                "openai-text-embedding-3-small",
                "openai-text-embedding-3-large", 
                "sentence-transformers-all-minilm-l6-v2"
            ),
            "model_dimensions", Map.of(
                "openai-text-embedding-3-small", 1536,
                "openai-text-embedding-3-large", 3072,
                "sentence-transformers-all-minilm-l6-v2", 384
            )
        );
        
        return ResponseEntity.ok(models);
    }
    
    @GetMapping("/health")
    @Operation(summary = "Health check for embedding service")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = Map.of(
            "status", "UP",
            "service", "rag-embedding-service",
            "timestamp", System.currentTimeMillis()
        );
        
        return ResponseEntity.ok(health);
    }
    
    private String truncateQuery(String query) {
        return query.length() > 100 ? query.substring(0, 100) + "..." : query;
    }
}
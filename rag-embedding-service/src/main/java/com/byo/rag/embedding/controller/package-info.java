/**
 * REST API controllers for vector embedding and similarity search operations.
 * 
 * <p>This package contains REST controllers that provide the public API
 * for vector embedding and similarity search operations in the Enterprise RAG
 * System. Controllers handle embedding generation, vector storage, semantic search,
 * and index management with comprehensive security, validation, and multi-tenant
 * isolation.</p>
 * 
 * <h2>Controller Architecture</h2>
 * <p>Embedding controllers implement comprehensive vector operation endpoints:</p>
 * <ul>
 *   <li><strong>Embedding Controller</strong> - Embedding generation and management</li>
 *   <li><strong>Search Controller</strong> - Similarity search and retrieval operations</li>
 *   <li><strong>Index Controller</strong> - Vector index management and optimization</li>
 *   <li><strong>Batch Controller</strong> - Bulk vector processing operations</li>
 *   <li><strong>Health Controller</strong> - Service health and status monitoring</li>
 * </ul>
 * 
 * <h2>Embedding Generation API</h2>
 * <p>Comprehensive embedding generation and management:</p>
 * <ul>
 *   <li><strong>Single Embedding</strong> - Generate embeddings for individual texts</li>
 *   <li><strong>Batch Embedding</strong> - Bulk embedding generation for multiple texts</li>
 *   <li><strong>Model Selection</strong> - Dynamic embedding model selection</li>
 *   <li><strong>Quality Validation</strong> - Embedding quality assessment and validation</li>
 *   <li><strong>Caching Support</strong> - Intelligent embedding caching for performance</li>
 * </ul>
 * 
 * <h2>Similarity Search API</h2>
 * <p>Advanced semantic search and retrieval capabilities:</p>
 * <ul>
 *   <li><strong>Vector Search</strong> - Pure vector similarity search</li>
 *   <li><strong>Hybrid Search</strong> - Combined vector and text search</li>
 *   <li><strong>Filtered Search</strong> - Metadata-based search filtering</li>
 *   <li><strong>Multi-Modal Search</strong> - Search across different content types</li>
 *   <li><strong>Ranked Results</strong> - Advanced ranking and scoring algorithms</li>
 * </ul>
 * 
 * <h2>Multi-Tenant Security</h2>
 * <p>Controllers implement comprehensive multi-tenant security:</p>
 * <ul>
 *   <li><strong>Tenant Isolation</strong> - Complete vector isolation between tenants</li>
 *   <li><strong>Access Control</strong> - Fine-grained vector access permissions</li>
 *   <li><strong>Authentication</strong> - JWT-based user authentication</li>
 *   <li><strong>Authorization</strong> - Role-based access control for vector operations</li>
 *   <li><strong>Audit Logging</strong> - Comprehensive audit trails for all operations</li>
 * </ul>
 * 
 * <h2>Request/Response Processing</h2>
 * <p>Advanced request and response handling:</p>
 * <ul>
 *   <li><strong>DTO Validation</strong> - Comprehensive validation of embedding requests</li>
 *   <li><strong>Response Formatting</strong> - Structured vector response formatting</li>
 *   <li><strong>Error Handling</strong> - Standardized error responses with details</li>
 *   <li><strong>Content Negotiation</strong> - Multiple response format support</li>
 *   <li><strong>Compression</strong> - Response compression for large vector data</li>
 * </ul>
 * 
 * <h2>Performance Optimization</h2>
 * <p>Controllers optimized for high-performance vector operations:</p>
 * <ul>
 *   <li><strong>Async Processing</strong> - Non-blocking vector processing</li>
 *   <li><strong>Streaming Support</strong> - Streaming for large vector datasets</li>
 *   <li><strong>Caching Headers</strong> - Appropriate cache control for vector data</li>
 *   <li><strong>Compression</strong> - Response compression for bandwidth optimization</li>
 *   <li><strong>Batch Operations</strong> - Support for bulk vector operations</li>
 * </ul>
 * 
 * <h2>API Documentation</h2>
 * <p>Comprehensive API documentation and examples:</p>
 * <ul>
 *   <li><strong>OpenAPI Specification</strong> - Complete API documentation</li>
 *   <li><strong>Request Examples</strong> - Sample requests for all endpoints</li>
 *   <li><strong>Response Examples</strong> - Sample responses with explanations</li>
 *   <li><strong>Error Documentation</strong> - Complete error scenario documentation</li>
 *   <li><strong>Integration Guide</strong> - Client integration guidelines</li>
 * </ul>
 * 
 * <h2>Monitoring and Observability</h2>
 * <p>Controllers include comprehensive monitoring:</p>
 * <ul>
 *   <li><strong>Request Metrics</strong> - HTTP request metrics and timing</li>
 *   <li><strong>Vector Metrics</strong> - Vector operation performance metrics</li>
 *   <li><strong>Search Metrics</strong> - Similarity search performance</li>
 *   <li><strong>Error Tracking</strong> - Comprehensive error tracking and alerting</li>
 *   <li><strong>Usage Analytics</strong> - Vector usage patterns and insights</li>
 * </ul>
 * 
 * <h2>Integration Patterns</h2>
 * <p>Controllers integrate with the complete vector processing ecosystem:</p>
 * <ul>
 *   <li><strong>Service Layer Integration</strong> - Clean separation with service layer</li>
 *   <li><strong>Event Publishing</strong> - Vector event publishing for analytics</li>
 *   <li><strong>Circuit Breaker Integration</strong> - Resilience patterns for AI models</li>
 *   <li><strong>Distributed Tracing</strong> - End-to-end request tracing</li>
 * </ul>
 * 
 * <h2>API Endpoints</h2>
 * <p>Complete embedding service API specification:</p>
 * <ul>
 *   <li><strong>POST /api/v1/embeddings/generate</strong> - Generate single embedding</li>
 *   <li><strong>POST /api/v1/embeddings/batch</strong> - Batch embedding generation</li>
 *   <li><strong>POST /api/v1/search/vector</strong> - Vector similarity search</li>
 *   <li><strong>POST /api/v1/search/hybrid</strong> - Hybrid semantic search</li>
 *   <li><strong>GET /api/v1/embeddings/{id}</strong> - Retrieve specific embedding</li>
 *   <li><strong>DELETE /api/v1/embeddings/{id}</strong> - Delete embedding</li>
 *   <li><strong>GET /api/v1/indices</strong> - List vector indices</li>
 *   <li><strong>POST /api/v1/indices</strong> - Create vector index</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @RestController
 * @RequestMapping("/api/v1/embeddings")
 * @Validated
 * @Slf4j
 * public class EmbeddingController {
 *     
 *     private final EmbeddingService embeddingService;
 *     private final VectorStorageService vectorStorageService;
 *     private final SecurityService securityService;
 *     
 *     @PostMapping("/generate")
 *     @PreAuthorize("hasRole('USER')")
 *     public CompletableFuture<ResponseEntity<EmbeddingResponse>> generateEmbedding(
 *             @Valid @RequestBody EmbeddingRequest request,
 *             @RequestHeader("X-Tenant-ID") String tenantId,
 *             Authentication authentication) {
 *         
 *         return securityService.validateTenantAccess(tenantId, authentication)
 *             .thenCompose(ignored -> embeddingService.generateEmbedding(tenantId, request))
 *             .thenApply(response -> {
 *                 // Add response headers
 *                 HttpHeaders headers = new HttpHeaders();
 *                 headers.add("X-Embedding-ID", response.getId());
 *                 headers.add("X-Model-Used", response.getModel());
 *                 headers.add("X-Dimensions", String.valueOf(response.getDimensions()));
 *                 
 *                 // Log embedding generation
 *                 auditService.logEmbeddingGeneration(
 *                     tenantId,
 *                     authentication.getName(),
 *                     response.getId(),
 *                     request.getModel()
 *                 );
 *                 
 *                 return ResponseEntity.ok()
 *                     .headers(headers)
 *                     .body(response);
 *             })
 *             .exceptionally(this::handleEmbeddingError);
 *     }
 *     
 *     @PostMapping("/batch")
 *     @PreAuthorize("hasRole('USER')")
 *     public CompletableFuture<ResponseEntity<BatchEmbeddingResponse>> generateBatchEmbeddings(
 *             @Valid @RequestBody BatchEmbeddingRequest request,
 *             @RequestHeader("X-Tenant-ID") String tenantId,
 *             Authentication authentication) {
 *         
 *         // Validate batch size limits
 *         if (request.getTexts().size() > MAX_BATCH_SIZE) {
 *             return CompletableFuture.completedFuture(
 *                 ResponseEntity.badRequest()
 *                     .body(BatchEmbeddingResponse.error("Batch size exceeds maximum limit"))
 *             );
 *         }
 *         
 *         return securityService.validateTenantAccess(tenantId, authentication)
 *             .thenCompose(ignored -> embeddingService.generateBatchEmbeddings(tenantId, request))
 *             .thenApply(embeddings -> {
 *                 BatchEmbeddingResponse response = BatchEmbeddingResponse.builder()
 *                     .batchId(request.getBatchId())
 *                     .embeddings(embeddings)
 *                     .totalCount(embeddings.size())
 *                     .model(request.getModel())
 *                     .processingTime(calculateProcessingTime())
 *                     .build();
 *                 
 *                 // Add response headers
 *                 HttpHeaders headers = new HttpHeaders();
 *                 headers.add("X-Batch-ID", request.getBatchId());
 *                 headers.add("X-Total-Count", String.valueOf(embeddings.size()));
 *                 
 *                 // Log batch processing
 *                 auditService.logBatchEmbeddingGeneration(
 *                     tenantId,
 *                     authentication.getName(),
 *                     request.getBatchId(),
 *                     embeddings.size()
 *                 );
 *                 
 *                 return ResponseEntity.ok()
 *                     .headers(headers)
 *                     .body(response);
 *             })
 *             .exceptionally(this::handleBatchEmbeddingError);
 *     }
 *     
 *     @PostMapping("/search/vector")
 *     @PreAuthorize("hasRole('USER')")
 *     public CompletableFuture<ResponseEntity<VectorSearchResponse>> searchVectors(
 *             @Valid @RequestBody VectorSearchRequest request,
 *             @RequestHeader("X-Tenant-ID") String tenantId,
 *             Authentication authentication) {
 *         
 *         return securityService.validateTenantAccess(tenantId, authentication)
 *             .thenCompose(ignored -> searchService.searchSimilarVectors(tenantId, request))
 *             .thenApply(searchResults -> {
 *                 VectorSearchResponse response = VectorSearchResponse.builder()
 *                     .queryId(UUID.randomUUID().toString())
 *                     .results(searchResults)
 *                     .totalResults(searchResults.size())
 *                     .searchTime(calculateSearchTime())
 *                     .similarityThreshold(request.getSimilarityThreshold())
 *                     .build();
 *                 
 *                 // Add response headers
 *                 HttpHeaders headers = new HttpHeaders();
 *                 headers.add("X-Query-ID", response.getQueryId());
 *                 headers.add("X-Results-Count", String.valueOf(searchResults.size()));
 *                 
 *                 return ResponseEntity.ok()
 *                     .headers(headers)
 *                     .body(response);
 *             })
 *             .exceptionally(this::handleSearchError);
 *     }
 *     
 *     @GetMapping("/{id}")
 *     @PreAuthorize("hasRole('USER')")
 *     public ResponseEntity<EmbeddingResponse> getEmbedding(
 *             @PathVariable String id,
 *             @RequestHeader("X-Tenant-ID") String tenantId,
 *             Authentication authentication) {
 *         
 *         try {
 *             // Validate tenant access
 *             securityService.validateTenantAccess(tenantId, authentication);
 *             
 *             // Retrieve embedding
 *             Optional<StoredVector> vector = vectorStorageService.getVector(tenantId, id);
 *             
 *             if (vector.isEmpty()) {
 *                 return ResponseEntity.notFound().build();
 *             }
 *             
 *             EmbeddingResponse response = EmbeddingResponse.builder()
 *                 .id(id)
 *                 .embedding(vector.get().getVector())
 *                 .model(vector.get().getMetadata().getModel())
 *                 .dimensions(vector.get().getVector().size())
 *                 .createdAt(vector.get().getMetadata().getTimestamp())
 *                 .build();
 *             
 *             return ResponseEntity.ok(response);
 *             
 *         } catch (TenantAccessException e) {
 *             return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
 *         } catch (Exception e) {
 *             log.error("Failed to retrieve embedding: {}", id, e);
 *             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
 *         }
 *     }
 *     
 *     private ResponseEntity<EmbeddingResponse> handleEmbeddingError(Throwable error) {
 *         log.error("Embedding generation error: {}", error.getMessage(), error);
 *         
 *         if (error instanceof EmbeddingValidationException) {
 *             return ResponseEntity.badRequest()
 *                 .body(EmbeddingResponse.error(error.getMessage()));
 *         } else if (error instanceof ModelUnavailableException) {
 *             return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
 *                 .body(EmbeddingResponse.error("Embedding model temporarily unavailable"));
 *         } else {
 *             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
 *                 .body(EmbeddingResponse.error("Internal server error"));
 *         }
 *     }
 * }
 * }</pre>
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 * @see org.springframework.web.bind.annotation Spring MVC annotations
 * @see java.util.concurrent.CompletableFuture Async processing
 * @see org.springframework.security.access.prepost Method security
 * @see com.byo.rag.embedding.service Embedding service layer
 */
package com.byo.rag.embedding.controller;
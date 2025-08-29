/**
 * REST API controllers for RAG processing operations.
 * 
 * <p>This package contains REST controllers that provide the public API
 * for Retrieval Augmented Generation operations in the Enterprise RAG System.
 * Controllers handle HTTP requests for RAG queries, conversation management,
 * and streaming responses while ensuring proper security and validation.</p>
 * 
 * <h2>Controller Architecture</h2>
 * <p>RAG controllers follow reactive Spring WebFlux patterns:</p>
 * <ul>
 *   <li><strong>RAG Controller</strong> - Main RAG query processing endpoints</li>
 *   <li><strong>Conversation Controller</strong> - Multi-turn conversation management</li>
 *   <li><strong>Streaming Controller</strong> - Real-time streaming response endpoints</li>
 *   <li><strong>Health Controller</strong> - RAG pipeline health and status monitoring</li>
 *   <li><strong>Analytics Controller</strong> - RAG usage analytics and metrics</li>
 * </ul>
 * 
 * <h2>Reactive Processing</h2>
 * <p>Controllers implement reactive patterns for optimal performance:</p>
 * <ul>
 *   <li><strong>Non-Blocking I/O</strong> - Reactive processing with WebFlux</li>
 *   <li><strong>Backpressure Handling</strong> - Automatic backpressure management</li>
 *   <li><strong>Stream Processing</strong> - Real-time response streaming</li>
 *   <li><strong>Async Validation</strong> - Non-blocking request validation</li>
 *   <li><strong>Error Handling</strong> - Reactive error handling and recovery</li>
 * </ul>
 * 
 * <h2>RAG Query Processing API</h2>
 * <p>Core RAG processing endpoints:</p>
 * <ul>
 *   <li><strong>Single-Turn Queries</strong> - One-time question answering</li>
 *   <li><strong>Multi-Turn Conversations</strong> - Context-aware conversation processing</li>
 *   <li><strong>Streaming Responses</strong> - Real-time response streaming</li>
 *   <li><strong>Batch Processing</strong> - Multiple query processing</li>
 *   <li><strong>Query Optimization</strong> - Query enhancement and preprocessing</li>
 * </ul>
 * 
 * <h2>Security and Multi-Tenancy</h2>
 * <p>Controllers implement comprehensive security:</p>
 * <ul>
 *   <li><strong>JWT Authentication</strong> - Token-based authentication</li>
 *   <li><strong>Tenant Isolation</strong> - Complete tenant data isolation</li>
 *   <li><strong>Rate Limiting</strong> - Query rate limiting per tenant and user</li>
 *   <li><strong>Input Validation</strong> - Comprehensive input sanitization</li>
 *   <li><strong>Authorization</strong> - Role-based access control</li>
 * </ul>
 * 
 * <h2>Request/Response Processing</h2>
 * <p>Advanced request and response handling:</p>
 * <ul>
 *   <li><strong>DTO Validation</strong> - Comprehensive validation of RAG request DTOs</li>
 *   <li><strong>Response Formatting</strong> - Structured RAG response formatting</li>
 *   <li><strong>Error Handling</strong> - Standardized error responses</li>
 *   <li><strong>Content Negotiation</strong> - Multiple response format support</li>
 *   <li><strong>Compression</strong> - Response compression for large responses</li>
 * </ul>
 * 
 * <h2>Streaming Response Support</h2>
 * <p>Real-time streaming capabilities:</p>
 * <ul>
 *   <li><strong>Server-Sent Events</strong> - SSE-based response streaming</li>
 *   <li><strong>WebSocket Support</strong> - Real-time bidirectional communication</li>
 *   <li><strong>Chunk Processing</strong> - Incremental response chunk delivery</li>
 *   <li><strong>Stream Lifecycle</strong> - Stream connection management</li>
 *   <li><strong>Error Recovery</strong> - Stream error handling and recovery</li>
 * </ul>
 * 
 * <h2>Performance Optimization</h2>
 * <p>Controllers optimized for high-performance RAG processing:</p>
 * <ul>
 *   <li><strong>Connection Keep-Alive</strong> - Efficient HTTP connection management</li>
 *   <li><strong>Request Caching</strong> - Intelligent request/response caching</li>
 *   <li><strong>Compression Headers</strong> - Response compression for bandwidth optimization</li>
 *   <li><strong>Async Processing</strong> - Non-blocking request processing</li>
 *   <li><strong>Connection Pooling</strong> - Optimized downstream service connections</li>
 * </ul>
 * 
 * <h2>API Documentation</h2>
 * <p>Comprehensive API documentation and examples:</p>
 * <ul>
 *   <li><strong>OpenAPI Specification</strong> - Complete API documentation</li>
 *   <li><strong>Request Examples</strong> - Sample requests for all endpoints</li>
 *   <li><strong>Response Examples</strong> - Sample responses with explanations</li>
 *   <li><strong>Error Documentation</strong> - Complete error scenario documentation</li>
 *   <li><strong>Usage Guidelines</strong> - Best practices and usage patterns</li>
 * </ul>
 * 
 * <h2>Monitoring and Observability</h2>
 * <p>Controllers include comprehensive monitoring:</p>
 * <ul>
 *   <li><strong>Request Metrics</strong> - HTTP request metrics and timing</li>
 *   <li><strong>RAG Metrics</strong> - RAG processing performance metrics</li>
 *   <li><strong>Error Tracking</strong> - Comprehensive error tracking and alerting</li>
 *   <li><strong>Usage Analytics</strong> - Query pattern analysis and insights</li>
 *   <li><strong>Health Monitoring</strong> - Endpoint health and availability monitoring</li>
 * </ul>
 * 
 * <h2>Integration Patterns</h2>
 * <p>Controllers integrate with the complete RAG ecosystem:</p>
 * <ul>
 *   <li><strong>Service Layer Integration</strong> - Clean separation with service layer</li>
 *   <li><strong>Event Publishing</strong> - RAG event publishing for analytics</li>
 *   <li><strong>Circuit Breaker Integration</strong> - Resilience patterns for external calls</li>
 *   <li><strong>Distributed Tracing</strong> - End-to-end request tracing</li>
 * </ul>
 * 
 * <h2>API Endpoints</h2>
 * <p>Complete RAG API endpoint specification:</p>
 * <ul>
 *   <li><strong>POST /api/v1/rag/query</strong> - Single-turn RAG query processing</li>
 *   <li><strong>POST /api/v1/rag/conversation</strong> - Multi-turn conversation processing</li>
 *   <li><strong>GET /api/v1/rag/conversation/{id}</strong> - Conversation history retrieval</li>
 *   <li><strong>POST /api/v1/rag/stream</strong> - Streaming RAG response processing</li>
 *   <li><strong>POST /api/v1/rag/batch</strong> - Batch query processing</li>
 *   <li><strong>GET /api/v1/rag/health</strong> - RAG pipeline health status</li>
 *   <li><strong>GET /api/v1/rag/metrics</strong> - RAG processing metrics</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @RestController
 * @RequestMapping("/api/v1/rag")
 * @Validated
 * @Slf4j
 * public class RagController {
 *     
 *     private final RagService ragService;
 *     private final ConversationService conversationService;
 *     private final SecurityContextService securityService;
 *     
 *     @PostMapping("/query")
 *     @PreAuthorize("hasRole('USER')")
 *     public Mono<ResponseEntity<RagResponse>> processQuery(
 *             @Valid @RequestBody RagQueryRequest request,
 *             @RequestHeader("X-Tenant-ID") String tenantId,
 *             Authentication authentication) {
 *         
 *         return securityService.validateTenantAccess(tenantId, authentication)
 *             .then(ragService.processQuery(tenantId, request))
 *             .map(response -> {
 *                 // Add response headers
 *                 HttpHeaders headers = new HttpHeaders();
 *                 headers.add("X-Query-ID", response.getQueryId());
 *                 headers.add("X-Processing-Time", response.getProcessingTime().toString());
 *                 
 *                 return ResponseEntity.ok()
 *                     .headers(headers)
 *                     .body(response);
 *             })
 *             .doOnNext(response -> logRagQuery(tenantId, request, response.getBody()))
 *             .onErrorResume(this::handleRagError);
 *     }
 *     
 *     @PostMapping("/stream")
 *     @PreAuthorize("hasRole('USER')")
 *     public Flux<ServerSentEvent<RagStreamChunk>> streamQuery(
 *             @Valid @RequestBody RagQueryRequest request,
 *             @RequestHeader("X-Tenant-ID") String tenantId,
 *             Authentication authentication) {
 *         
 *         return securityService.validateTenantAccess(tenantId, authentication)
 *             .thenMany(ragService.processStreamingQuery(tenantId, request))
 *             .map(chunk -> ServerSentEvent.<RagStreamChunk>builder()
 *                 .id(chunk.getChunkId())
 *                 .event("rag-chunk")
 *                 .data(chunk)
 *                 .build())
 *             .doOnComplete(() -> log.info("Completed streaming query for tenant: {}", tenantId))
 *             .onErrorResume(error -> handleStreamingError(error, tenantId));
 *     }
 *     
 *     private Mono<ResponseEntity<RagResponse>> handleRagError(Throwable error) {
 *         log.error("RAG processing error: {}", error.getMessage(), error);
 *         
 *         if (error instanceof ValidationException) {
 *             return Mono.just(ResponseEntity.badRequest().build());
 *         } else if (error instanceof TenantAccessException) {
 *             return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
 *         } else {
 *             return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
 *         }
 *     }
 * }
 * }</pre>
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 * @see org.springframework.web.bind.annotation Spring MVC annotations
 * @see org.springframework.web.reactive.function.server Reactive routing
 * @see reactor.core.publisher Reactive streams
 * @see com.enterprise.rag.core.service RAG service layer
 * @see com.enterprise.rag.core.dto RAG DTOs
 */
package com.enterprise.rag.core.controller;
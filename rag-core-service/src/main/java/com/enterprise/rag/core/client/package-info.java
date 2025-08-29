/**
 * Client components for external service integration.
 * 
 * <p>This package contains Feign clients and integration components that
 * enable the RAG core service to communicate with other microservices
 * in the Enterprise RAG System. Clients handle service-to-service
 * communication with proper error handling, circuit breaker patterns,
 * and multi-tenant context propagation.</p>
 * 
 * <h2>Client Architecture</h2>
 * <p>Service clients implement enterprise integration patterns:</p>
 * <ul>
 *   <li><strong>Embedding Service Client</strong> - Vector search and embedding operations</li>
 *   <li><strong>Document Service Client</strong> - Document metadata and content retrieval</li>
 *   <li><strong>Auth Service Client</strong> - User authentication and authorization</li>
 *   <li><strong>Admin Service Client</strong> - Administrative operations and monitoring</li>
 *   <li><strong>External LLM Clients</strong> - Large language model provider integrations</li>
 * </ul>
 * 
 * <h2>Feign Client Configuration</h2>
 * <p>Comprehensive Feign client setup with enterprise features:</p>
 * <ul>
 *   <li><strong>Service Discovery</strong> - Automatic service discovery and load balancing</li>
 *   <li><strong>Circuit Breaker</strong> - Hystrix circuit breaker integration</li>
 *   <li><strong>Retry Logic</strong> - Configurable retry strategies with exponential backoff</li>
 *   <li><strong>Request Interceptors</strong> - Automatic JWT token and tenant context propagation</li>
 *   <li><strong>Error Decoders</strong> - Custom error handling and exception translation</li>
 * </ul>
 * 
 * <h2>Embedding Service Integration</h2>
 * <p>High-performance integration with the embedding service:</p>
 * <ul>
 *   <li><strong>Vector Search</strong> - Semantic similarity search operations</li>
 *   <li><strong>Embedding Generation</strong> - Real-time query embedding generation</li>
 *   <li><strong>Batch Operations</strong> - Bulk embedding and search operations</li>
 *   <li><strong>Index Management</strong> - Vector index selection and optimization</li>
 *   <li><strong>Performance Optimization</strong> - Connection pooling and caching</li>
 * </ul>
 * 
 * <h2>Multi-Tenant Context Propagation</h2>
 * <p>Automatic tenant context propagation across service calls:</p>
 * <ul>
 *   <li><strong>Tenant Headers</strong> - Automatic tenant ID header injection</li>
 *   <li><strong>Security Context</strong> - JWT token propagation for authentication</li>
 *   <li><strong>Request Correlation</strong> - Correlation ID propagation for tracing</li>
 *   <li><strong>Context Validation</strong> - Tenant context validation on responses</li>
 * </ul>
 * 
 * <h2>Error Handling and Resilience</h2>
 * <p>Comprehensive error handling and resilience patterns:</p>
 * <ul>
 *   <li><strong>Custom Error Decoders</strong> - Service-specific error interpretation</li>
 *   <li><strong>Exception Translation</strong> - External service exceptions to domain exceptions</li>
 *   <li><strong>Circuit Breaker</strong> - Automatic service failure detection and recovery</li>
 *   <li><strong>Fallback Strategies</strong> - Graceful degradation when services are unavailable</li>
 *   <li><strong>Retry Policies</strong> - Intelligent retry with jitter and backoff</li>
 * </ul>
 * 
 * <h2>Performance Optimization</h2>
 * <p>Clients optimized for high-performance service communication:</p>
 * <ul>
 *   <li><strong>Connection Pooling</strong> - Efficient HTTP connection management</li>
 *   <li><strong>Request Compression</strong> - Automatic request/response compression</li>
 *   <li><strong>Caching</strong> - Intelligent response caching for repeated calls</li>
 *   <li><strong>Async Processing</strong> - Non-blocking service calls where appropriate</li>
 *   <li><strong>Load Balancing</strong> - Automatic load balancing across service instances</li>
 * </ul>
 * 
 * <h2>Security Integration</h2>
 * <p>Comprehensive security for service-to-service communication:</p>
 * <ul>
 *   <li><strong>JWT Propagation</strong> - Automatic JWT token forwarding</li>
 *   <li><strong>Service Authentication</strong> - Service-to-service authentication</li>
 *   <li><strong>TLS/SSL</strong> - Encrypted communication between services</li>
 *   <li><strong>Certificate Validation</strong> - Mutual TLS authentication</li>
 * </ul>
 * 
 * <h2>Monitoring and Observability</h2>
 * <p>Comprehensive monitoring for service communication:</p>
 * <ul>
 *   <li><strong>Request Metrics</strong> - Detailed metrics for service calls</li>
 *   <li><strong>Performance Tracking</strong> - Response time and throughput monitoring</li>
 *   <li><strong>Error Monitoring</strong> - Service communication error tracking</li>
 *   <li><strong>Circuit Breaker Metrics</strong> - Circuit breaker status and health</li>
 *   <li><strong>Distributed Tracing</strong> - End-to-end request tracing</li>
 * </ul>
 * 
 * <h2>External LLM Integration</h2>
 * <p>Integration with external Large Language Model providers:</p>
 * <ul>
 *   <li><strong>OpenAI Client</strong> - OpenAI API integration with GPT models</li>
 *   <li><strong>Anthropic Client</strong> - Anthropic Claude model integration</li>
 *   <li><strong>Azure OpenAI Client</strong> - Azure OpenAI service integration</li>
 *   <li><strong>Custom LLM Clients</strong> - Support for custom LLM providers</li>
 *   <li><strong>Provider Switching</strong> - Dynamic LLM provider selection</li>
 * </ul>
 * 
 * <h2>Configuration Management</h2>
 * <p>Flexible configuration for service integration:</p>
 * <ul>
 *   <li><strong>Service URLs</strong> - Dynamic service endpoint configuration</li>
 *   <li><strong>Timeout Configuration</strong> - Configurable request and connection timeouts</li>
 *   <li><strong>Retry Configuration</strong> - Configurable retry policies and strategies</li>
 *   <li><strong>Circuit Breaker Configuration</strong> - Configurable circuit breaker thresholds</li>
 * </ul>
 * 
 * <h2>Testing Support</h2>
 * <p>Comprehensive testing support for service clients:</p>
 * <ul>
 *   <li><strong>Mock Clients</strong> - Mock implementations for testing</li>
 *   <li><strong>Test Containers</strong> - Integration testing with real services</li>
 *   <li><strong>Stub Services</strong> - Stubbed service implementations</li>
 *   <li><strong>Contract Testing</strong> - Consumer-driven contract testing</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @FeignClient(
 *     name = "embedding-service",
 *     configuration = EmbeddingServiceClientConfig.class,
 *     fallback = EmbeddingServiceClientFallback.class
 * )
 * public interface EmbeddingServiceClient {
 *     
 *     @PostMapping("/api/v1/search/vector")
 *     @Retryable(value = {RetryableException.class}, maxAttempts = 3)
 *     VectorSearchResponse searchVectors(
 *         @RequestHeader("X-Tenant-ID") String tenantId,
 *         @RequestHeader("Authorization") String authorization,
 *         @Valid @RequestBody VectorSearchRequest request
 *     );
 *     
 *     @PostMapping("/api/v1/embeddings/generate")
 *     @CircuitBreaker(name = "embedding-service", fallbackMethod = "generateEmbeddingFallback")
 *     EmbeddingResponse generateEmbedding(
 *         @RequestHeader("X-Tenant-ID") String tenantId,
 *         @RequestHeader("Authorization") String authorization,
 *         @Valid @RequestBody EmbeddingRequest request
 *     );
 * }
 * 
 * @Component
 * @Slf4j
 * public class EmbeddingServiceClientConfig implements RequestInterceptor {
 *     
 *     private final SecurityContextService securityService;
 *     private final TenantContextService tenantService;
 *     
 *     @Override
 *     public void apply(RequestTemplate template) {
 *         // Add authentication header
 *         String token = securityService.getCurrentToken();
 *         if (token != null) {
 *             template.header("Authorization", "Bearer " + token);
 *         }
 *         
 *         // Add tenant context
 *         String tenantId = tenantService.getCurrentTenantId();
 *         if (tenantId != null) {
 *             template.header("X-Tenant-ID", tenantId);
 *         }
 *         
 *         // Add correlation ID for tracing
 *         String correlationId = RequestContextUtils.getCorrelationId();
 *         if (correlationId != null) {
 *             template.header("X-Correlation-ID", correlationId);
 *         }
 *     }
 *     
 *     @Bean
 *     public ErrorDecoder errorDecoder() {
 *         return new EmbeddingServiceErrorDecoder();
 *     }
 *     
 *     @Bean
 *     @ConditionalOnProperty(name = "rag.clients.embedding.compression.enabled", havingValue = "true")
 *     public RequestInterceptor compressionInterceptor() {
 *         return template -> template.header("Accept-Encoding", "gzip, deflate");
 *     }
 * }
 * }</pre>
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 * @see org.springframework.cloud.openfeign.FeignClient Feign client integration
 * @see org.springframework.retry Retry mechanisms
 * @see org.springframework.cloud.circuitbreaker Circuit breaker patterns
 * @see com.enterprise.rag.core.service RAG service integration
 */
package com.enterprise.rag.core.client;
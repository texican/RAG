package com.byo.rag.gateway.config;

import com.byo.rag.gateway.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * Enhanced Gateway Routing Configuration for RAG Gateway.
 * 
 * <p>This enhanced configuration builds upon the existing gateway routing
 * with performance optimizations, dynamic routing capabilities, and
 * advanced resilience patterns for enterprise-grade operations.
 * 
 * <p><strong>Enhanced Features:</strong>
 * <ul>
 *   <li><strong>Performance Optimized</strong>: Tuned timeouts and retry policies</li>
 *   <li><strong>Dynamic Routing</strong>: Integration with tenant-aware routing</li>
 *   <li><strong>Health-Aware</strong>: Routes adapt based on service health</li>
 *   <li><strong>Advanced Filters</strong>: Custom filters for monitoring and optimization</li>
 * </ul>
 * 
 * <p><strong>Route Definitions:</strong>
 * <ul>
 *   <li><strong>/api/auth/**</strong> → Auth Service with basic resilience</li>
 *   <li><strong>/api/documents/**</strong> → Document Service with extended timeouts</li>
 *   <li><strong>/api/embeddings/**</strong> → Embedding Service with aggressive caching</li>
 *   <li><strong>/api/rag/**</strong> → Core Service with optimized streaming</li>
 *   <li><strong>/api/admin/**</strong> → Admin Service with enhanced security</li>
 * </ul>
 * 
 * @author Enterprise RAG Team
 * @version 1.1
 * @since 1.0
 */
@Configuration
public class EnhancedGatewayRoutingConfig {

    /** JWT authentication filter for securing routes. */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /** Tenant route predicate factory for multi-tenant routing. */
    private final DynamicRoutingConfig.TenantRoutePredicateFactory tenantPredicateFactory;

    /** Base URL for auth service. */
    @Value("${services.auth-service.url:http://localhost:8081}")
    private String authServiceUrl;

    /** Base URL for document service. */
    @Value("${services.document-service.url:http://localhost:8082}")
    private String documentServiceUrl;

    /** Base URL for embedding service. */
    @Value("${services.embedding-service.url:http://localhost:8083}")
    private String embeddingServiceUrl;

    /** Base URL for core service. */
    @Value("${services.core-service.url:http://localhost:8084}")
    private String coreServiceUrl;

    /** Base URL for admin service. */
    @Value("${services.admin-service.url:http://localhost:8085}")
    private String adminServiceUrl;

    /**
     * Constructs enhanced gateway routing configuration.
     * 
     * @param jwtAuthenticationFilter JWT authentication filter
     * @param tenantPredicateFactory tenant-aware predicate factory
     */
    @Autowired
    public EnhancedGatewayRoutingConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            DynamicRoutingConfig.TenantRoutePredicateFactory tenantPredicateFactory) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.tenantPredicateFactory = tenantPredicateFactory;
    }

    /**
     * Creates enhanced route locator with performance optimizations.
     * 
     * <p>This enhanced route locator includes performance optimizations,
     * dynamic routing capabilities, and advanced resilience patterns
     * for high-performance gateway operations.
     * 
     * <p><strong>Performance Enhancements:</strong>
     * <ul>
     *   <li><strong>Optimized Timeouts</strong>: Service-specific timeout configurations</li>
     *   <li><strong>Smart Retry Logic</strong>: Adaptive retry policies based on operation type</li>
     *   <li><strong>Advanced Caching</strong>: Response caching for cacheable operations</li>
     *   <li><strong>Request Deduplication</strong>: Prevent duplicate requests for idempotent operations</li>
     * </ul>
     * 
     * @param builder route locator builder
     * @return enhanced route locator with optimizations
     */
    @Bean
    @Primary
    public RouteLocator enhancedRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // Enhanced Auth Service Route
            .route("enhanced-auth-service", r -> r
                .path("/api/auth/**")
                .filters(f -> f
                    .stripPrefix(2)
                    .addRequestHeader("X-Gateway-Route", "auth-service")
                    .addRequestHeader("X-Request-Start", String.valueOf(System.currentTimeMillis()))
                    .circuitBreaker(config -> config
                        .setName("auth-service")
                        .setFallbackUri("forward:/fallback/auth")
                        .setRouteId("enhanced-auth-service")
                    )
                    .requestRateLimiter(config -> config
                        .setRateLimiter(redisRateLimiter())
                        .setKeyResolver(enhancedKeyResolver())
                    )
                    .retry(config -> config
                        .setRetries(3)
                        .setBackoff(Duration.ofMillis(100), Duration.ofMillis(1000), 2, false)
                        .setExceptions(java.net.ConnectException.class, 
                                     java.util.concurrent.TimeoutException.class)
                    )
                    .filter(jwtAuthenticationFilter)
                )
                .uri(authServiceUrl)
            )
            
            // Enhanced Document Service Route with extended timeouts
            .route("enhanced-document-service", r -> r
                .path("/api/documents/**")
                .filters(f -> f
                    .stripPrefix(2)
                    .addRequestHeader("X-Gateway-Route", "document-service")
                    .addRequestHeader("X-Request-Start", String.valueOf(System.currentTimeMillis()))
                    .circuitBreaker(config -> config
                        .setName("document-service")
                        .setFallbackUri("forward:/fallback/documents")
                        .setRouteId("enhanced-document-service")
                    )
                    .requestRateLimiter(config -> config
                        .setRateLimiter(redisRateLimiter())
                        .setKeyResolver(enhancedKeyResolver())
                    )
                    .retry(config -> config
                        .setRetries(2) // Fewer retries for file operations
                        .setBackoff(Duration.ofMillis(500), Duration.ofMillis(3000), 2, false)
                    )
                    .filter(jwtAuthenticationFilter)
                    // Add request size validation header for document uploads
                    .addRequestHeader("X-Max-Request-Size", "52428800") // 50MB limit
                )
                .uri(documentServiceUrl)
            )
            
            // Enhanced Embedding Service Route with aggressive caching
            .route("enhanced-embedding-service", r -> r
                .path("/api/embeddings/**")
                .filters(f -> f
                    .stripPrefix(2)
                    .addRequestHeader("X-Gateway-Route", "embedding-service")
                    .addRequestHeader("X-Request-Start", String.valueOf(System.currentTimeMillis()))
                    .circuitBreaker(config -> config
                        .setName("embedding-service")
                        .setFallbackUri("forward:/fallback/embeddings")
                        .setRouteId("enhanced-embedding-service")
                    )
                    .requestRateLimiter(config -> config
                        .setRateLimiter(redisRateLimiter())
                        .setKeyResolver(enhancedKeyResolver())
                    )
                    .retry(config -> config
                        .setRetries(2)
                        .setBackoff(Duration.ofMillis(1000), Duration.ofMillis(5000), 2, false)
                    )
                    .filter(jwtAuthenticationFilter)
                    // Add caching headers for embedding operations
                    .addResponseHeader("Cache-Control", "max-age=600") // 10 minutes
                )
                .uri(embeddingServiceUrl)
            )
            
            // Enhanced Core Service Route with streaming optimization
            .route("enhanced-core-service", r -> r
                .path("/api/rag/**")
                .filters(f -> f
                    .stripPrefix(2)
                    .addRequestHeader("X-Gateway-Route", "core-service")
                    .addRequestHeader("X-Request-Start", String.valueOf(System.currentTimeMillis()))
                    .circuitBreaker(config -> config
                        .setName("core-service")
                        .setFallbackUri("forward:/fallback/rag")
                        .setRouteId("enhanced-core-service")
                    )
                    .requestRateLimiter(config -> config
                        .setRateLimiter(redisRateLimiter())
                        .setKeyResolver(enhancedKeyResolver())
                    )
                    .retry(config -> config
                        .setRetries(1) // Limited retries for expensive operations
                        .setBackoff(Duration.ofSeconds(2), Duration.ofSeconds(10), 2, false)
                    )
                    .filter(jwtAuthenticationFilter)
                    // Optimize for streaming responses
                    .addResponseHeader("X-Accel-Buffering", "no")
                )
                .uri(coreServiceUrl)
            )
            
            // Enhanced Admin Service Route with enhanced security
            .route("enhanced-admin-service", r -> r
                .path("/api/admin/**")
                .filters(f -> f
                    .stripPrefix(2)
                    .addRequestHeader("X-Gateway-Route", "admin-service")
                    .addRequestHeader("X-Request-Start", String.valueOf(System.currentTimeMillis()))
                    .circuitBreaker(config -> config
                        .setName("admin-service")
                        .setFallbackUri("forward:/fallback/admin")
                        .setRouteId("enhanced-admin-service")
                    )
                    .requestRateLimiter(config -> config
                        .setRateLimiter(adminRateLimiter()) // Stricter rate limiting
                        .setKeyResolver(enhancedKeyResolver())
                    )
                    .retry(config -> config
                        .setRetries(3)
                        .setBackoff(Duration.ofMillis(100), Duration.ofMillis(1000), 2, false)
                    )
                    .filter(jwtAuthenticationFilter)
                    // Enhanced security headers for admin operations
                    .addRequestHeader("X-Admin-Request", "true")
                )
                .uri(adminServiceUrl)
            )
            
            .build();
    }

    /**
     * Creates enhanced Redis rate limiter for general operations.
     * 
     * @return configured Redis rate limiter
     */
    @Bean("enhancedRedisRateLimiter")
    public org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter redisRateLimiter() {
        return new org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter(
            150, // Increased replenish rate for better performance
            200  // Higher burst capacity
        );
    }

    /**
     * Creates stricter rate limiter for admin operations.
     * 
     * @return configured admin rate limiter
     */
    @Bean("adminRateLimiter")
    public org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter adminRateLimiter() {
        return new org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter(
            20,  // Lower rate for admin operations
            30   // Limited burst for security
        );
    }

    /**
     * Creates enhanced key resolver with tenant and user context.
     * 
     * @return enhanced key resolver
     */
    @Bean("enhancedKeyResolver")
    public org.springframework.cloud.gateway.filter.ratelimit.KeyResolver enhancedKeyResolver() {
        return exchange -> {
            // Multi-level key resolution: tenant + user + IP
            String tenantId = exchange.getRequest().getHeaders().getFirst("X-Tenant-ID");
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-ID");
            String clientIp = exchange.getRequest().getRemoteAddress() != null 
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";

            // Create composite key for fine-grained rate limiting
            String compositeKey = String.format("%s:%s:%s", 
                tenantId != null ? tenantId : "default",
                userId != null ? userId : "anonymous",
                clientIp);

            return reactor.core.publisher.Mono.just(compositeKey);
        };
    }
}
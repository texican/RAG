package com.byo.rag.gateway.config;

import com.byo.rag.gateway.security.HierarchicalRateLimitingService;
import com.byo.rag.gateway.service.AdvancedRateLimitingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Rate Limiting Configuration for RAG Gateway.
 * 
 * <p>This configuration integrates hierarchical rate limiting into the
 * gateway filter chain, providing comprehensive rate limiting across
 * multiple levels with sophisticated controls and monitoring.
 * 
 * <p><strong>Integration Features:</strong>
 * <ul>
 *   <li><strong>Filter Integration</strong>: Seamless integration with gateway filters</li>
 *   <li><strong>Error Handling</strong>: Proper error responses for rate limit violations</li>
 *   <li><strong>Metrics Integration</strong>: Rate limiting metrics for monitoring</li>
 *   <li><strong>Performance Optimized</strong>: Minimal latency impact on requests</li>
 * </ul>
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.1
 */
@Configuration
public class RateLimitingConfig {

    /** Hierarchical rate limiting service. */
    private final HierarchicalRateLimitingService hierarchicalRateLimitingService;

    /**
     * Constructs rate limiting configuration.
     * 
     * @param hierarchicalRateLimitingService hierarchical rate limiting service
     */
    @Autowired
    public RateLimitingConfig(HierarchicalRateLimitingService hierarchicalRateLimitingService) {
        this.hierarchicalRateLimitingService = hierarchicalRateLimitingService;
    }

    /**
     * Creates hierarchical rate limiting filter.
     * 
     * <p>This filter applies hierarchical rate limiting to all requests,
     * checking limits at global, tenant, user, endpoint, and IP levels
     * with adaptive controls based on system load.
     * 
     * @return hierarchical rate limiting filter
     */
    @Bean
    public GatewayFilter hierarchicalRateLimitingFilter() {
        return new HierarchicalRateLimitingFilter(hierarchicalRateLimitingService);
    }

    /**
     * Hierarchical Rate Limiting Filter implementation.
     */
    public static class HierarchicalRateLimitingFilter implements GatewayFilter, Ordered {

        private final HierarchicalRateLimitingService rateLimitingService;

        public HierarchicalRateLimitingFilter(HierarchicalRateLimitingService rateLimitingService) {
            this.rateLimitingService = rateLimitingService;
        }

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
            // Extract request context
            String clientIp = getClientIpAddress(exchange);
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-ID");
            String tenantId = exchange.getRequest().getHeaders().getFirst("X-Tenant-ID");
            String path = exchange.getRequest().getURI().getPath();
            String endpoint = extractEndpoint(path);

            // Skip rate limiting for health checks and actuator endpoints
            if (isExemptEndpoint(path)) {
                return chain.filter(exchange);
            }

            // Determine rate limit type based on endpoint
            AdvancedRateLimitingService.RateLimitType rateLimitType = determineRateLimitType(path);

            // Create rate limiting context
            HierarchicalRateLimitingService.RateLimitContext context = 
                new HierarchicalRateLimitingService.RateLimitContext(
                    clientIp, userId, tenantId, endpoint, path, rateLimitType);

            // Perform hierarchical rate limit check
            return rateLimitingService.checkHierarchicalRateLimit(context)
                .flatMap(result -> {
                    if (result.isAllowed()) {
                        // Add rate limiting headers to response
                        addRateLimitHeaders(exchange, result);
                        return chain.filter(exchange);
                    } else {
                        // Create rate limit exceeded response
                        return createRateLimitExceededResponse(exchange, result);
                    }
                });
        }

        /**
         * Extracts client IP address from request.
         */
        private String getClientIpAddress(ServerWebExchange exchange) {
            String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            return exchange.getRequest().getRemoteAddress() != null 
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() 
                : "unknown";
        }

        /**
         * Extracts endpoint from request path.
         */
        private String extractEndpoint(String path) {
            if (path.startsWith("/api/")) {
                String[] pathParts = path.split("/");
                if (pathParts.length >= 3) {
                    return "/api/" + pathParts[2]; // e.g., "/api/auth", "/api/documents"
                }
            }
            return path;
        }

        /**
         * Determines rate limit type based on request path.
         */
        private AdvancedRateLimitingService.RateLimitType determineRateLimitType(String path) {
            if (path.startsWith("/api/auth/")) {
                return AdvancedRateLimitingService.RateLimitType.AUTHENTICATION;
            } else if (path.startsWith("/api/documents/upload")) {
                return AdvancedRateLimitingService.RateLimitType.API_UPLOAD;
            } else if (path.startsWith("/api/embeddings/search") || path.startsWith("/api/rag/query")) {
                return AdvancedRateLimitingService.RateLimitType.API_SEARCH;
            } else if (path.startsWith("/api/admin/")) {
                return AdvancedRateLimitingService.RateLimitType.ADMIN_OPERATIONS;
            } else if (path.contains("refresh")) {
                return AdvancedRateLimitingService.RateLimitType.TOKEN_REFRESH;
            } else {
                return AdvancedRateLimitingService.RateLimitType.API_GENERAL;
            }
        }

        /**
         * Checks if endpoint is exempt from rate limiting.
         */
        private boolean isExemptEndpoint(String path) {
            return path.startsWith("/actuator/") || 
                   path.startsWith("/health") ||
                   path.equals("/favicon.ico");
        }

        /**
         * Adds rate limiting headers to response.
         */
        private void addRateLimitHeaders(ServerWebExchange exchange, 
                                       HierarchicalRateLimitingService.HierarchicalRateLimitResult result) {
            exchange.getResponse().getHeaders().add("X-RateLimit-Global", 
                String.valueOf(result.getGlobalRequests()));
            exchange.getResponse().getHeaders().add("X-RateLimit-Tenant", 
                String.valueOf(result.getTenantRequests()));
            exchange.getResponse().getHeaders().add("X-RateLimit-User", 
                String.valueOf(result.getUserRequests()));
            exchange.getResponse().getHeaders().add("X-RateLimit-Endpoint", 
                String.valueOf(result.getEndpointRequests()));
            exchange.getResponse().getHeaders().add("X-RateLimit-IP", 
                String.valueOf(result.getIpRequests()));
            
            if (result.isAdaptivelyLimited()) {
                exchange.getResponse().getHeaders().add("X-RateLimit-Adaptive", "true");
            }
        }

        /**
         * Creates rate limit exceeded response.
         */
        private Mono<Void> createRateLimitExceededResponse(
                ServerWebExchange exchange, 
                HierarchicalRateLimitingService.HierarchicalRateLimitResult result) {

            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
            exchange.getResponse().getHeaders().add("Retry-After", 
                String.valueOf(result.getRetryAfter().getSeconds()));
            exchange.getResponse().getHeaders().add("X-RateLimit-Level", result.getBlockedLevel());

            String requestId = exchange.getRequest().getHeaders().getFirst("X-Request-ID");
            if (requestId == null) {
                requestId = java.util.UUID.randomUUID().toString().substring(0, 8);
            }

            String errorResponse = String.format(
                "{" +
                "\"error\":\"RATE_LIMIT_EXCEEDED\"," +
                "\"message\":\"%s\"," +
                "\"level\":\"%s\"," +
                "\"retryAfter\":%d," +
                "\"timestamp\":\"%s\"," +
                "\"requestId\":\"%s\"," +
                "\"limits\":{" +
                    "\"global\":%d," +
                    "\"tenant\":%d," +
                    "\"user\":%d," +
                    "\"endpoint\":%d," +
                    "\"ip\":%d" +
                "}" +
                "}", 
                result.getReason(),
                result.getBlockedLevel(),
                result.getRetryAfter().getSeconds(),
                LocalDateTime.now(),
                requestId,
                result.getGlobalRequests(),
                result.getTenantRequests(),
                result.getUserRequests(),
                result.getEndpointRequests(),
                result.getIpRequests()
            );

            DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(errorResponse.getBytes(StandardCharsets.UTF_8));
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }

        @Override
        public int getOrder() {
            return -70; // Execute after security headers but before authentication
        }
    }
}
package com.byo.rag.gateway.filter;

import com.byo.rag.gateway.service.*;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Enhanced JWT Authentication Filter with comprehensive security features.
 * 
 * <p>This filter extends the basic JWT authentication with advanced security
 * capabilities including rate limiting, request validation, audit logging,
 * and session management. It provides enterprise-grade protection against
 * various attack vectors and security threats.
 * 
 * <p><strong>Security Features:</strong>
 * <ul>
 *   <li>Multi-layer rate limiting (IP, user, endpoint)</li>
 *   <li>Comprehensive request validation and sanitization</li>
 *   <li>Advanced audit logging for security events</li>
 *   <li>Session management with token refresh capabilities</li>
 *   <li>Real-time threat detection and response</li>
 *   <li>Progressive security penalties for violations</li>
 * </ul>
 * 
 * <p><strong>Performance Optimizations:</strong>
 * <ul>
 *   <li>Asynchronous processing with reactive streams</li>
 *   <li>Efficient caching of security decisions</li>
 *   <li>Parallel validation of security constraints</li>
 *   <li>Minimal latency impact on legitimate requests</li>
 * </ul>
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.0
 */
@Component
public class EnhancedJwtAuthenticationFilter implements GatewayFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedJwtAuthenticationFilter.class);

    /** JWT validation service for token processing. */
    private final JwtValidationService jwtValidationService;
    
    /** Advanced rate limiting service. */
    private final AdvancedRateLimitingService rateLimitingService;
    
    /** Request validation and sanitization service. */
    private final RequestValidationService validationService;
    
    /** Security audit logging service. */
    private final SecurityAuditService auditService;
    
    /** Session management service. */
    private final SessionManagementService sessionService;

    /** List of public endpoints that bypass authentication. */
    private static final List<String> PUBLIC_ENDPOINTS = Arrays.asList(
        "/api/auth/",
        "/actuator/health",
        "/actuator/info",
        "/actuator/metrics",
        "/swagger-ui/",
        "/v3/api-docs/",
        "/favicon.ico"
    );

    /** List of admin-only endpoints requiring elevated privileges. */
    private static final List<String> ADMIN_ENDPOINTS = Arrays.asList(
        "/api/admin/",
        "/actuator/",
        "/api/management/"
    );

    /**
     * Constructs enhanced JWT authentication filter with all security services.
     */
    public EnhancedJwtAuthenticationFilter(
            @Autowired JwtValidationService jwtValidationService,
            @Autowired AdvancedRateLimitingService rateLimitingService,
            @Autowired RequestValidationService validationService,
            @Autowired SecurityAuditService auditService,
            @Autowired SessionManagementService sessionService) {
        this.jwtValidationService = jwtValidationService;
        this.rateLimitingService = rateLimitingService;
        this.validationService = validationService;
        this.auditService = auditService;
        this.sessionService = sessionService;
    }

    /**
     * Main filter method implementing comprehensive security validation.
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String clientIP = getClientIP(request);
        String userAgent = request.getHeaders().getFirst("User-Agent");

        // Allow public endpoints without authentication
        if (isPublicEndpoint(path)) {
            return applyPublicEndpointSecurity(exchange, chain, clientIP, path);
        }

        // Extract and validate JWT token
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            auditService.logAuthenticationFailure("missing-auth-header", clientIP, userAgent, 
                "Missing or invalid Authorization header", path);
            return unauthorizedResponse(exchange, "Missing authorization header");
        }

        String token = authHeader.substring(7); // Remove "Bearer " prefix

        // Comprehensive security validation pipeline
        return validateToken(token, clientIP, userAgent, path)
            .flatMap(tokenValid -> {
                if (!tokenValid) {
                    return unauthorizedResponse(exchange, "Invalid or expired token");
                }
                
                return extractUserContext(token)
                    .flatMap(userContext -> 
                        validateRequestSecurity(exchange, userContext, clientIP, path)
                            .flatMap(securityValid -> {
                                if (!securityValid) {
                                    return forbiddenResponse(exchange, "Security validation failed");
                                }
                                
                                return applyAuthenticatedRequestProcessing(exchange, chain, userContext, clientIP);
                            })
                    );
            })
            .onErrorResume(error -> {
                logger.error("Security validation error for request from {}: {}", clientIP, error.getMessage());
                auditService.logValidationFailure("FILTER_ERROR", error.getMessage(), path, clientIP, null);
                return unauthorizedResponse(exchange, "Authentication failed");
            });
    }

    /**
     * Applies security measures for public endpoints.
     */
    private Mono<Void> applyPublicEndpointSecurity(ServerWebExchange exchange, GatewayFilterChain chain, 
                                                  String clientIP, String path) {
        // Apply rate limiting for public endpoints
        return rateLimitingService.checkIPRateLimit(clientIP, 
                AdvancedRateLimitingService.RateLimitType.API_GENERAL, path)
            .flatMap(rateLimit -> {
                if (!rateLimit.isAllowed()) {
                    return rateLimitExceededResponse(exchange, rateLimit);
                }
                
                // Apply request validation even for public endpoints
                return validationService.validateRequest(exchange, null)
                    .flatMap(validation -> {
                        if (!validation.isValid()) {
                            return badRequestResponse(exchange, "Request validation failed", 
                                validation.getViolations());
                        }
                        
                        return chain.filter(addSecurityHeaders(exchange));
                    });
            });
    }

    /**
     * Validates JWT token with comprehensive security checks.
     */
    private Mono<Boolean> validateToken(String token, String clientIP, String userAgent, String path) {
        return Mono.fromCallable(() -> {
            // Check if token is blacklisted
            if (sessionService.isTokenBlacklisted(token)) {
                auditService.logAuthenticationFailure("blacklisted-token", clientIP, userAgent, 
                    "Blacklisted token used", path);
                return false;
            }

            // Validate token structure and signature
            if (!jwtValidationService.isTokenValid(token)) {
                auditService.logAuthenticationFailure("invalid-token", clientIP, userAgent, 
                    "Invalid token signature or expired", path);
                return false;
            }

            // Verify this is an access token (not refresh token)
            if (jwtValidationService.isRefreshToken(token)) {
                auditService.logAuthenticationFailure("refresh-token-misuse", clientIP, userAgent, 
                    "Refresh token used for authentication", path);
                return false;
            }

            return true;
        });
    }

    /**
     * Extracts user context from validated JWT token.
     */
    private Mono<UserContext> extractUserContext(String token) {
        return Mono.fromCallable(() -> {
            try {
                Claims claims = jwtValidationService.extractClaims(token);
                String userId = jwtValidationService.extractUserId(token).toString();
                String tenantId = jwtValidationService.extractTenantId(token) != null ? 
                    jwtValidationService.extractTenantId(token).toString() : null;
                String role = jwtValidationService.extractRole(token);
                String email = jwtValidationService.extractEmail(token);
                String sessionId = claims.get("sessionId", String.class);

                return new UserContext(userId, tenantId, role, email, sessionId);
            } catch (Exception e) {
                logger.error("Error extracting user context from token", e);
                throw new RuntimeException("Invalid token claims");
            }
        });
    }

    /**
     * Validates request security including rate limiting and request validation.
     */
    private Mono<Boolean> validateRequestSecurity(ServerWebExchange exchange, UserContext userContext, 
                                                 String clientIP, String path) {
        // Determine appropriate rate limit based on endpoint
        AdvancedRateLimitingService.RateLimitType rateLimitType = determineRateLimitType(path);
        
        return rateLimitingService.checkUserRateLimit(userContext.getUserId(), userContext.getTenantId(), 
                clientIP, rateLimitType, path)
            .flatMap(rateLimit -> {
                if (!rateLimit.isAllowed()) {
                    return Mono.just(false);
                }
                
                // Validate request structure and content
                return validationService.validateRequest(exchange, userContext.getUserId())
                    .map(validation -> validation.isValid());
            });
    }

    /**
     * Processes authenticated requests with user context.
     */
    private Mono<Void> applyAuthenticatedRequestProcessing(ServerWebExchange exchange, GatewayFilterChain chain,
                                                          UserContext userContext, String clientIP) {
        String path = exchange.getRequest().getURI().getPath();
        
        // Check admin endpoint access
        if (isAdminEndpoint(path) && !"ADMIN".equals(userContext.getRole())) {
            auditService.logAuthorizationFailure(userContext.getUserId(), userContext.getTenantId(),
                "ADMIN", userContext.getRole(), path, clientIP);
            return forbiddenResponse(exchange, "Insufficient privileges");
        }

        // Add user context headers and security headers
        ServerWebExchange enhancedExchange = addUserContextHeaders(exchange, userContext);
        enhancedExchange = addSecurityHeaders(enhancedExchange);
        
        // Log successful authentication
        auditService.logAuthenticationSuccess(userContext.getUserId(), userContext.getTenantId(),
            clientIP, exchange.getRequest().getHeaders().getFirst("User-Agent"), "JWT_FILTER");

        return chain.filter(enhancedExchange);
    }

    /**
     * User context holder for authentication information.
     */
    private static class UserContext {
        private final String userId;
        private final String tenantId;
        private final String role;
        private final String email;
        private final String sessionId;

        public UserContext(String userId, String tenantId, String role, String email, String sessionId) {
            this.userId = userId;
            this.tenantId = tenantId;
            this.role = role;
            this.email = email;
            this.sessionId = sessionId;
        }

        public String getUserId() { return userId; }
        public String getTenantId() { return tenantId; }
        public String getRole() { return role; }
        public String getEmail() { return email; }
        public String getSessionId() { return sessionId; }
    }

    // Helper methods

    private boolean isPublicEndpoint(String path) {
        return PUBLIC_ENDPOINTS.stream().anyMatch(path::startsWith);
    }

    private boolean isAdminEndpoint(String path) {
        return ADMIN_ENDPOINTS.stream().anyMatch(path::startsWith);
    }

    private AdvancedRateLimitingService.RateLimitType determineRateLimitType(String path) {
        if (path.contains("/auth")) return AdvancedRateLimitingService.RateLimitType.AUTHENTICATION;
        if (path.contains("/search")) return AdvancedRateLimitingService.RateLimitType.API_SEARCH;
        if (path.contains("/upload") || path.contains("/documents")) return AdvancedRateLimitingService.RateLimitType.API_UPLOAD;
        if (path.contains("/admin")) return AdvancedRateLimitingService.RateLimitType.ADMIN_OPERATIONS;
        return AdvancedRateLimitingService.RateLimitType.API_GENERAL;
    }

    private String getClientIP(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }

        return request.getRemoteAddress() != null ? 
            request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }

    private ServerWebExchange addUserContextHeaders(ServerWebExchange exchange, UserContext userContext) {
        ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate();
        
        requestBuilder.headers(headers -> {
            headers.add("X-User-Id", userContext.getUserId());
            if (userContext.getTenantId() != null) {
                headers.add("X-Tenant-Id", userContext.getTenantId());
            }
            if (userContext.getRole() != null) {
                headers.add("X-User-Role", userContext.getRole());
            }
            if (userContext.getEmail() != null) {
                headers.add("X-User-Email", userContext.getEmail());
            }
            if (userContext.getSessionId() != null) {
                headers.add("X-Session-Id", userContext.getSessionId());
            }
        });

        return exchange.mutate().request(requestBuilder.build()).build();
    }

    private ServerWebExchange addSecurityHeaders(ServerWebExchange exchange) {
        // Add essential security headers directly
        Map<String, String> securityHeaders = new java.util.HashMap<>();
        securityHeaders.put("X-Content-Type-Options", "nosniff");
        securityHeaders.put("X-Frame-Options", "DENY");
        securityHeaders.put("X-XSS-Protection", "1; mode=block");
        securityHeaders.put("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // Add security headers to response
        securityHeaders.forEach((name, value) -> 
            exchange.getResponse().getHeaders().add(name, value)
        );
        
        return exchange;
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String reason) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("WWW-Authenticate", "Bearer");
        exchange.getResponse().getHeaders().add("X-Error-Reason", reason);
        return exchange.getResponse().setComplete();
    }

    private Mono<Void> forbiddenResponse(ServerWebExchange exchange, String reason) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().add("X-Error-Reason", reason);
        return exchange.getResponse().setComplete();
    }

    private Mono<Void> rateLimitExceededResponse(ServerWebExchange exchange, 
                                                AdvancedRateLimitingService.RateLimitResult rateLimit) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", "0");
        exchange.getResponse().getHeaders().add("X-RateLimit-Reset", 
            String.valueOf(System.currentTimeMillis() + rateLimit.getRetryAfter().toMillis()));
        exchange.getResponse().getHeaders().add("Retry-After", 
            String.valueOf(rateLimit.getRetryAfter().toSeconds()));
        return exchange.getResponse().setComplete();
    }

    private Mono<Void> badRequestResponse(ServerWebExchange exchange, String message, List<String> violations) {
        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
        exchange.getResponse().getHeaders().add("X-Error-Message", message);
        exchange.getResponse().getHeaders().add("X-Validation-Errors", String.join(", ", violations));
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -100; // Execute early in the filter chain
    }
}
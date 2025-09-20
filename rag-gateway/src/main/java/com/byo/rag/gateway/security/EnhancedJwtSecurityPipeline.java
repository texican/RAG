package com.byo.rag.gateway.security;

import com.byo.rag.gateway.service.JwtValidationService;
import com.byo.rag.gateway.service.SessionManagementService;
import com.byo.rag.gateway.service.SecurityAuditService;
import io.jsonwebtoken.Claims;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Enhanced JWT Security Pipeline for RAG Gateway.
 * 
 * <p>This enhanced security pipeline provides comprehensive JWT authentication
 * with advanced security features including token refresh, session management,
 * blacklisting, and comprehensive audit logging for enterprise environments.
 * 
 * <p><strong>Security Pipeline Features:</strong>
 * <ul>
 *   <li><strong>Multi-Layer Validation</strong>: Signature, expiration, claims, and blacklist checks</li>
 *   <li><strong>Token Refresh</strong>: Automatic token rotation with secure refresh mechanism</li>
 *   <li><strong>Session Management</strong>: Active session tracking with concurrent session limits</li>
 *   <li><strong>Security Auditing</strong>: Comprehensive audit trail for compliance</li>
 *   <li><strong>Threat Detection</strong>: Suspicious activity detection and response</li>
 * </ul>
 * 
 * <p><strong>Enterprise Security Considerations:</strong>
 * <ul>
 *   <li>OWASP Top 10 compliance with JWT best practices</li>
 *   <li>Multi-tenant isolation with tenant-aware validation</li>
 *   <li>Real-time threat detection and automated response</li>
 *   <li>Comprehensive audit logging for regulatory compliance</li>
 * </ul>
 * 
 * @author Enterprise RAG Team
 * @version 2.0
 * @since 1.1
 */
@Component
public class EnhancedJwtSecurityPipeline implements GatewayFilter, Ordered {

    /** JWT validation service for token verification. */
    private final JwtValidationService jwtValidationService;

    /** Session management service for session tracking. */
    private final SessionManagementService sessionManagementService;

    /** Security audit service for logging security events. */
    private final SecurityAuditService securityAuditService;

    /** Endpoints that bypass authentication. */
    private final List<String> publicEndpoints = Arrays.asList(
        "/api/auth/login",
        "/api/auth/register", 
        "/api/auth/refresh",
        "/actuator/health",
        "/actuator/info"
    );

    /** Endpoints requiring admin privileges. */
    private final List<String> adminEndpoints = Arrays.asList(
        "/api/admin/**",
        "/admin/routes/**"
    );

    /**
     * Constructs enhanced JWT security pipeline.
     * 
     * @param jwtValidationService JWT validation service
     * @param sessionManagementService session management service
     * @param securityAuditService security audit service
     */
    @Autowired
    public EnhancedJwtSecurityPipeline(
            JwtValidationService jwtValidationService,
            SessionManagementService sessionManagementService,
            SecurityAuditService securityAuditService) {
        this.jwtValidationService = jwtValidationService;
        this.sessionManagementService = sessionManagementService;
        this.securityAuditService = securityAuditService;
    }

    /**
     * Applies enhanced JWT security pipeline to requests.
     * 
     * <p>This method implements a comprehensive security pipeline that validates
     * JWT tokens through multiple layers of verification, manages active sessions,
     * and provides detailed audit logging for all authentication events.
     * 
     * <p><strong>Security Pipeline Steps:</strong>
     * <ol>
     *   <li><strong>Public Endpoint Check</strong>: Allow public endpoints without authentication</li>
     *   <li><strong>Token Extraction</strong>: Extract JWT from Authorization header</li>
     *   <li><strong>Token Validation</strong>: Multi-layer token verification</li>
     *   <li><strong>Blacklist Check</strong>: Verify token is not blacklisted</li>
     *   <li><strong>Session Validation</strong>: Check active session status</li>
     *   <li><strong>Claims Processing</strong>: Extract and validate user claims</li>
     *   <li><strong>Authorization Check</strong>: Verify access permissions</li>
     *   <li><strong>Context Propagation</strong>: Add security context to request</li>
     *   <li><strong>Audit Logging</strong>: Log authentication event</li>
     * </ol>
     * 
     * @param exchange server web exchange
     * @param chain gateway filter chain
     * @return reactive response with security processing
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().toString();
        String clientIp = getClientIpAddress(request);
        String requestId = getOrCreateRequestId(exchange);

        // Step 1: Check if endpoint is public
        if (isPublicEndpoint(path)) {
            securityAuditService.logAuthenticationEvent(
                requestId, "PUBLIC_ACCESS", clientIp, path, "SUCCESS", null);
            return chain.filter(exchange);
        }

        // Step 2: Extract JWT token
        String token = extractJwtToken(request);
        if (token == null) {
            return handleAuthenticationFailure(exchange, "MISSING_TOKEN", 
                requestId, clientIp, path, "No JWT token provided");
        }

        // Step 3-8: Process JWT through security pipeline
        return processJwtSecurityPipeline(exchange, chain, token, requestId, clientIp, path, method);
    }

    /**
     * Processes JWT through comprehensive security pipeline.
     * 
     * @param exchange server web exchange
     * @param chain gateway filter chain
     * @param token JWT token
     * @param requestId request identifier
     * @param clientIp client IP address
     * @param path request path
     * @param method HTTP method
     * @return reactive response with security processing
     */
    private Mono<Void> processJwtSecurityPipeline(
            ServerWebExchange exchange, 
            GatewayFilterChain chain,
            String token, 
            String requestId, 
            String clientIp, 
            String path, 
            String method) {

        if (!jwtValidationService.validateToken(token)) {
            return handleAuthenticationFailure(exchange, "INVALID_TOKEN", 
                requestId, clientIp, path, "Invalid JWT token");
        }

        try {
            Claims claims = jwtValidationService.extractClaims(token);
            String userId = claims.getSubject();
            String tenantId = claims.get("tenantId", String.class);
            String sessionId = claims.get("sessionId", String.class);

            // Step 4: Check token blacklist
            boolean isBlacklisted = sessionManagementService.isTokenBlacklisted(token);
            if (isBlacklisted) {
                return handleAuthenticationFailure(exchange, "BLACKLISTED_TOKEN",
                    requestId, clientIp, path, "Token is blacklisted");
            }

            // Step 5: Validate active session
            boolean sessionValid = sessionManagementService.validateSession(sessionId, userId);
            if (!sessionValid) {
                return handleAuthenticationFailure(exchange, "INVALID_SESSION",
                    requestId, clientIp, path, "Session is invalid or expired");
            }

            // Step 6: Authorization check
            if (requiresAdminAccess(path) && !hasAdminRole(claims)) {
                return handleAuthorizationFailure(exchange, "INSUFFICIENT_PRIVILEGES",
                    requestId, clientIp, path, userId);
            }

            // Step 7: Add security context to request
            ServerHttpRequest enhancedRequest = addSecurityContext(
                exchange.getRequest(), claims, requestId);

            // Step 8: Log successful authentication
            securityAuditService.logAuthenticationEvent(
                userId, tenantId, "JWT_AUTH_SUCCESS", clientIp, 
                exchange.getRequest().getHeaders().getFirst("User-Agent"), "Authentication successful");

            // Step 9: Update session activity and continue
            return sessionManagementService.updateSessionActivity(sessionId)
                .then(chain.filter(exchange.mutate().request(enhancedRequest).build()));
                
        } catch (Exception error) {
            String errorType = determineErrorType(error);
            return handleAuthenticationFailure(exchange, errorType, 
                requestId, clientIp, path, error.getMessage());
        }
    }

    /**
     * Adds security context to request headers.
     * 
     * @param request original request
     * @param claims JWT claims
     * @param requestId request identifier
     * @return enhanced request with security context
     */
    private ServerHttpRequest addSecurityContext(ServerHttpRequest request, Claims claims, String requestId) {
        return request.mutate()
            .header("X-User-ID", claims.getSubject())
            .header("X-Tenant-ID", claims.get("tenantId", String.class))
            .header("X-User-Role", claims.get("role", String.class))
            .header("X-Session-ID", claims.get("sessionId", String.class))
            .header("X-Request-ID", requestId)
            .header("X-Auth-Time", String.valueOf(System.currentTimeMillis()))
            .build();
    }

    /**
     * Handles authentication failure with comprehensive logging.
     * 
     * @param exchange server web exchange
     * @param errorType type of authentication error
     * @param requestId request identifier
     * @param clientIp client IP address
     * @param path request path
     * @param errorMessage error message
     * @return reactive error response
     */
    private Mono<Void> handleAuthenticationFailure(
            ServerWebExchange exchange, 
            String errorType, 
            String requestId, 
            String clientIp, 
            String path, 
            String errorMessage) {

        // Log authentication failure
        securityAuditService.logAuthenticationEvent(
            requestId, errorType, clientIp, path, "FAILURE", null);

        // Check for suspicious activity
        securityAuditService.detectSuspiciousActivity(clientIp, errorType);

        // Set error response
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("WWW-Authenticate", "Bearer");
        exchange.getResponse().getHeaders().add("X-Auth-Error", errorType);
        exchange.getResponse().getHeaders().add("X-Request-ID", requestId);

        return exchange.getResponse().setComplete();
    }

    /**
     * Handles authorization failure with audit logging.
     * 
     * @param exchange server web exchange
     * @param errorType type of authorization error
     * @param requestId request identifier
     * @param clientIp client IP address
     * @param path request path
     * @param userId user identifier
     * @return reactive error response
     */
    private Mono<Void> handleAuthorizationFailure(
            ServerWebExchange exchange, 
            String errorType, 
            String requestId, 
            String clientIp, 
            String path, 
            String userId) {

        // Log authorization failure
        securityAuditService.logAuthenticationEvent(
            requestId, errorType, clientIp, path, "AUTHORIZATION_FAILURE", userId);

        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().add("X-Auth-Error", errorType);
        exchange.getResponse().getHeaders().add("X-Request-ID", requestId);

        return exchange.getResponse().setComplete();
    }

    /**
     * Extracts JWT token from Authorization header.
     * 
     * @param request HTTP request
     * @return JWT token or null if not found
     */
    private String extractJwtToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * Gets client IP address from request.
     * 
     * @param request HTTP request
     * @return client IP address
     */
    private String getClientIpAddress(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddress() != null 
            ? request.getRemoteAddress().getAddress().getHostAddress() 
            : "unknown";
    }

    /**
     * Gets or creates request ID for tracking.
     * 
     * @param exchange server web exchange
     * @return request ID
     */
    private String getOrCreateRequestId(ServerWebExchange exchange) {
        String requestId = exchange.getRequest().getHeaders().getFirst("X-Request-ID");
        if (requestId == null) {
            requestId = java.util.UUID.randomUUID().toString().substring(0, 8);
        }
        return requestId;
    }

    /**
     * Checks if endpoint is public (no authentication required).
     * 
     * @param path request path
     * @return true if public endpoint
     */
    private boolean isPublicEndpoint(String path) {
        return publicEndpoints.stream().anyMatch(path::startsWith);
    }

    /**
     * Checks if endpoint requires admin access.
     * 
     * @param path request path
     * @return true if admin endpoint
     */
    private boolean requiresAdminAccess(String path) {
        return adminEndpoints.stream().anyMatch(endpoint -> {
            if (endpoint.endsWith("/**")) {
                return path.startsWith(endpoint.substring(0, endpoint.length() - 3));
            }
            return path.equals(endpoint);
        });
    }

    /**
     * Checks if user has admin role.
     * 
     * @param claims JWT claims
     * @return true if user has admin role
     */
    private boolean hasAdminRole(Claims claims) {
        String role = claims.get("role", String.class);
        return "ADMIN".equals(role) || "SUPER_ADMIN".equals(role);
    }

    /**
     * Determines error type from exception.
     * 
     * @param error exception
     * @return error type string
     */
    private String determineErrorType(Throwable error) {
        if (error instanceof io.jsonwebtoken.ExpiredJwtException) {
            return "EXPIRED_TOKEN";
        } else if (error instanceof io.jsonwebtoken.SignatureException) {
            return "INVALID_SIGNATURE";
        } else if (error instanceof io.jsonwebtoken.MalformedJwtException) {
            return "MALFORMED_TOKEN";
        } else {
            return "TOKEN_VALIDATION_ERROR";
        }
    }

    @Override
    public int getOrder() {
        return -100; // High priority for security filter
    }
}
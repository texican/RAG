package com.byo.rag.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Security Headers Filter for RAG Gateway.
 * 
 * <p>This filter implements comprehensive security headers management for
 * OWASP compliance and enhanced protection against common web vulnerabilities.
 * It automatically adds security headers to all responses to provide
 * defense-in-depth security for the gateway and downstream services.
 * 
 * <p><strong>Security Headers Implemented:</strong>
 * <ul>
 *   <li><strong>HSTS</strong>: HTTP Strict Transport Security for HTTPS enforcement</li>
 *   <li><strong>CSP</strong>: Content Security Policy for XSS protection</li>
 *   <li><strong>X-Frame-Options</strong>: Clickjacking protection</li>
 *   <li><strong>X-Content-Type-Options</strong>: MIME sniffing protection</li>
 *   <li><strong>X-XSS-Protection</strong>: Reflected XSS protection</li>
 *   <li><strong>Referrer-Policy</strong>: Referrer information control</li>
 *   <li><strong>Permissions-Policy</strong>: Feature access control</li>
 * </ul>
 * 
 * <p><strong>Environment-Specific Configuration:</strong>
 * <ul>
 *   <li>Development: Relaxed CSP for debugging</li>
 *   <li>Testing: Standard security headers with test allowances</li>
 *   <li>Production: Strict security headers for maximum protection</li>
 * </ul>
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.1
 */
@Component
public class SecurityHeadersFilter implements GatewayFilter, Ordered {

    /** Environment profile for header configuration. */
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /** Enable HSTS header. */
    @Value("${security.headers.hsts.enabled:true}")
    private boolean hstsEnabled;

    /** HSTS max age in seconds. */
    @Value("${security.headers.hsts.max-age:31536000}")
    private long hstsMaxAge;

    /** Enable HSTS include subdomains. */
    @Value("${security.headers.hsts.include-subdomains:true}")
    private boolean hstsIncludeSubdomains;

    /** Enable Content Security Policy. */
    @Value("${security.headers.csp.enabled:true}")
    private boolean cspEnabled;

    /** Content Security Policy directive. */
    @Value("${security.headers.csp.policy:default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:}")
    private String cspPolicy;

    /** Enable frame options header. */
    @Value("${security.headers.frame-options.enabled:true}")
    private boolean frameOptionsEnabled;

    /** Frame options policy. */
    @Value("${security.headers.frame-options.policy:DENY}")
    private String frameOptionsPolicy;

    /** Enable content type options header. */
    @Value("${security.headers.content-type-options.enabled:true}")
    private boolean contentTypeOptionsEnabled;

    /** Enable XSS protection header. */
    @Value("${security.headers.xss-protection.enabled:true}")
    private boolean xssProtectionEnabled;

    /** Enable referrer policy header. */
    @Value("${security.headers.referrer-policy.enabled:true}")
    private boolean referrerPolicyEnabled;

    /** Referrer policy directive. */
    @Value("${security.headers.referrer-policy.policy:strict-origin-when-cross-origin}")
    private String referrerPolicy;

    /** Enable permissions policy header. */
    @Value("${security.headers.permissions-policy.enabled:true}")
    private boolean permissionsPolicyEnabled;

    /** Permissions policy directive. */
    @Value("${security.headers.permissions-policy.policy:geolocation=(), microphone=(), camera=()}")
    private String permissionsPolicy;

    /**
     * Applies comprehensive security headers to all responses.
     * 
     * <p>This method adds security headers based on the environment configuration
     * and security best practices. Headers are applied consistently across all
     * responses to ensure comprehensive protection.
     * 
     * @param exchange server web exchange
     * @param chain gateway filter chain
     * @return reactive response with security headers
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            
            // Add HSTS header for HTTPS enforcement
            if (hstsEnabled && isSecureRequest(exchange)) {
                addHstsHeader(response);
            }
            
            // Add Content Security Policy header
            if (cspEnabled) {
                addCspHeader(response);
            }
            
            // Add X-Frame-Options header for clickjacking protection
            if (frameOptionsEnabled) {
                addFrameOptionsHeader(response);
            }
            
            // Add X-Content-Type-Options header for MIME sniffing protection
            if (contentTypeOptionsEnabled) {
                addContentTypeOptionsHeader(response);
            }
            
            // Add X-XSS-Protection header for reflected XSS protection
            if (xssProtectionEnabled) {
                addXssProtectionHeader(response);
            }
            
            // Add Referrer-Policy header for referrer control
            if (referrerPolicyEnabled) {
                addReferrerPolicyHeader(response);
            }
            
            // Add Permissions-Policy header for feature control
            if (permissionsPolicyEnabled) {
                addPermissionsPolicyHeader(response);
            }
            
            // Add custom security headers
            addCustomSecurityHeaders(response, exchange);
        }));
    }

    /**
     * Adds HTTP Strict Transport Security header.
     * 
     * @param response HTTP response
     */
    private void addHstsHeader(ServerHttpResponse response) {
        StringBuilder hstsValue = new StringBuilder();
        hstsValue.append("max-age=").append(hstsMaxAge);
        
        if (hstsIncludeSubdomains) {
            hstsValue.append("; includeSubDomains");
        }
        
        // Add preload directive for production
        if ("prod".equals(activeProfile)) {
            hstsValue.append("; preload");
        }
        
        response.getHeaders().add("Strict-Transport-Security", hstsValue.toString());
    }

    /**
     * Adds Content Security Policy header.
     * 
     * @param response HTTP response
     */
    private void addCspHeader(ServerHttpResponse response) {
        String effectivePolicy = getEffectiveCspPolicy();
        response.getHeaders().add("Content-Security-Policy", effectivePolicy);
        
        // Also add CSP report-only header for monitoring in development
        if ("dev".equals(activeProfile)) {
            response.getHeaders().add("Content-Security-Policy-Report-Only", effectivePolicy);
        }
    }

    /**
     * Adds X-Frame-Options header.
     * 
     * @param response HTTP response
     */
    private void addFrameOptionsHeader(ServerHttpResponse response) {
        response.getHeaders().add("X-Frame-Options", frameOptionsPolicy);
    }

    /**
     * Adds X-Content-Type-Options header.
     * 
     * @param response HTTP response
     */
    private void addContentTypeOptionsHeader(ServerHttpResponse response) {
        response.getHeaders().add("X-Content-Type-Options", "nosniff");
    }

    /**
     * Adds X-XSS-Protection header.
     * 
     * @param response HTTP response
     */
    private void addXssProtectionHeader(ServerHttpResponse response) {
        // Use mode=block for enhanced protection
        response.getHeaders().add("X-XSS-Protection", "1; mode=block");
    }

    /**
     * Adds Referrer-Policy header.
     * 
     * @param response HTTP response
     */
    private void addReferrerPolicyHeader(ServerHttpResponse response) {
        response.getHeaders().add("Referrer-Policy", referrerPolicy);
    }

    /**
     * Adds Permissions-Policy header.
     * 
     * @param response HTTP response
     */
    private void addPermissionsPolicyHeader(ServerHttpResponse response) {
        response.getHeaders().add("Permissions-Policy", permissionsPolicy);
    }

    /**
     * Adds custom security headers specific to the application.
     * 
     * @param response HTTP response
     * @param exchange server web exchange
     */
    private void addCustomSecurityHeaders(ServerHttpResponse response, ServerWebExchange exchange) {
        // Add custom headers for the RAG Gateway
        response.getHeaders().add("X-Content-Security", "enhanced");
        response.getHeaders().add("X-Gateway-Security", "active");
        
        // Add cache control for sensitive endpoints
        String path = exchange.getRequest().getURI().getPath();
        if (isSensitiveEndpoint(path)) {
            response.getHeaders().add("Cache-Control", "no-store, no-cache, must-revalidate, private");
            response.getHeaders().add("Pragma", "no-cache");
            response.getHeaders().add("Expires", "0");
        }
        
        // Add CORS headers if not already present
        if (!response.getHeaders().containsKey("Access-Control-Allow-Origin")) {
            addCorsHeaders(response, exchange);
        }
        
        // Add server information header (without revealing version)
        response.getHeaders().add("X-Server", "RAG-Gateway");
        
        // Add request ID if available
        String requestId = (String) exchange.getAttributes().get("request.id");
        if (requestId != null) {
            response.getHeaders().add("X-Request-ID", requestId);
        }
    }

    /**
     * Gets effective CSP policy based on environment.
     * 
     * @return CSP policy string
     */
    private String getEffectiveCspPolicy() {
        switch (activeProfile) {
            case "dev":
                // Relaxed policy for development
                return "default-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                       "script-src 'self' 'unsafe-inline' 'unsafe-eval' localhost:*; " +
                       "style-src 'self' 'unsafe-inline'; " +
                       "img-src 'self' data: localhost:*; " +
                       "connect-src 'self' localhost:*";
                       
            case "test":
                // Standard policy for testing
                return "default-src 'self'; " +
                       "script-src 'self' 'unsafe-inline'; " +
                       "style-src 'self' 'unsafe-inline'; " +
                       "img-src 'self' data:; " +
                       "connect-src 'self'";
                       
            case "prod":
            default:
                // Strict policy for production
                return cspPolicy;
        }
    }

    /**
     * Adds CORS headers for cross-origin requests.
     * 
     * @param response HTTP response
     * @param exchange server web exchange
     */
    private void addCorsHeaders(ServerHttpResponse response, ServerWebExchange exchange) {
        String origin = exchange.getRequest().getHeaders().getFirst("Origin");
        
        // Set appropriate CORS headers based on environment
        if ("dev".equals(activeProfile)) {
            response.getHeaders().add("Access-Control-Allow-Origin", "*");
        } else if (origin != null && isAllowedOrigin(origin)) {
            response.getHeaders().add("Access-Control-Allow-Origin", origin);
            response.getHeaders().add("Vary", "Origin");
        }
        
        response.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.getHeaders().add("Access-Control-Allow-Headers", 
            "Authorization, Content-Type, X-Requested-With, X-Tenant-ID, X-Request-ID");
        response.getHeaders().add("Access-Control-Max-Age", "3600");
    }

    /**
     * Checks if the request is made over HTTPS.
     * 
     * @param exchange server web exchange
     * @return true if request is secure
     */
    private boolean isSecureRequest(ServerWebExchange exchange) {
        String scheme = exchange.getRequest().getURI().getScheme();
        String forwardedProto = exchange.getRequest().getHeaders().getFirst("X-Forwarded-Proto");
        
        return "https".equals(scheme) || "https".equals(forwardedProto);
    }

    /**
     * Checks if the endpoint is sensitive and requires additional security.
     * 
     * @param path request path
     * @return true if endpoint is sensitive
     */
    private boolean isSensitiveEndpoint(String path) {
        return path.startsWith("/api/auth/") || 
               path.startsWith("/api/admin/") ||
               path.contains("login") ||
               path.contains("password") ||
               path.contains("token");
    }

    /**
     * Checks if the origin is allowed for CORS requests.
     * 
     * @param origin request origin
     * @return true if origin is allowed
     */
    private boolean isAllowedOrigin(String origin) {
        // In production, this should be configured with actual allowed origins
        String[] allowedOrigins = {
            "https://localhost:3000",
            "https://localhost:8080",
            "https://rag-frontend.company.com"
        };
        
        for (String allowedOrigin : allowedOrigins) {
            if (origin.equals(allowedOrigin)) {
                return true;
            }
        }
        
        return false;
    }

    @Override
    public int getOrder() {
        return -80; // Execute before authentication but after validation
    }
}
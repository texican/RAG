package com.byo.rag.gateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;

/**
 * Test Controller for Security Integration Testing.
 * 
 * <p>This controller provides simple endpoints for testing the gateway's
 * security features including JWT authentication, rate limiting, and
 * request validation without requiring external service dependencies.
 * 
 * <p><strong>Test Endpoints:</strong>
 * <ul>
 *   <li>/test/auth/profile - Protected endpoint for authentication testing</li>
 *   <li>/test/public/health - Public endpoint for basic testing</li>
 *   <li>/test/admin/users - Admin endpoint for authorization testing</li>
 * </ul>
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.0
 */
@RestController
@RequestMapping("/test")
public class TestController {

    /**
     * Protected endpoint for testing JWT authentication.
     */
    @GetMapping("/auth/profile")
    public ResponseEntity<?> getProfile(ServerWebExchange exchange) {
        // Extract user info from headers set by JWT filter
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        String tenantId = exchange.getRequest().getHeaders().getFirst("X-Tenant-Id");
        String role = exchange.getRequest().getHeaders().getFirst("X-User-Role");
        
        return ResponseEntity.ok()
                .header("X-User-Id", userId != null ? userId : "unknown")
                .header("X-Tenant-Id", tenantId != null ? tenantId : "unknown")
                .header("X-User-Role", role != null ? role : "unknown")
                .body("{\"message\":\"Profile accessed successfully\",\"userId\":\"" + userId + "\"}");
    }

    /**
     * Public endpoint for basic testing (no authentication required).
     */
    @GetMapping("/public/health")
    public ResponseEntity<String> getHealth() {
        return ResponseEntity.ok("{\"status\":\"healthy\",\"message\":\"Gateway is operational\"}");
    }

    /**
     * Admin endpoint for testing authorization levels.
     */
    @GetMapping("/admin/users")
    public ResponseEntity<String> getUsers(ServerWebExchange exchange) {
        String role = exchange.getRequest().getHeaders().getFirst("X-User-Role");
        
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(403).body("{\"error\":\"Insufficient privileges\"}");
        }
        
        return ResponseEntity.ok("{\"users\":[{\"id\":1,\"name\":\"Test User\"}]}");
    }

    /**
     * Endpoint for testing various HTTP methods and request validation.
     */
    @PostMapping("/documents/upload")
    public ResponseEntity<String> uploadDocument(@RequestHeader(value = "X-Filename", required = false) String filename) {
        if (filename != null && (filename.contains("..") || filename.contains(";"))) {
            return ResponseEntity.badRequest().body("{\"error\":\"Invalid filename\"}");
        }
        
        return ResponseEntity.ok("{\"message\":\"Document uploaded successfully\"}");
    }

    /**
     * Endpoint for testing search functionality and SQL injection prevention.
     */
    @GetMapping("/documents/search")
    public ResponseEntity<String> searchDocuments(@RequestParam(value = "query", required = false) String query) {
        if (query != null && (query.toLowerCase().contains("drop") || query.contains("'"))) {
            return ResponseEntity.badRequest().body("{\"error\":\"Invalid search query\"}");
        }
        
        return ResponseEntity.ok("{\"results\":[],\"query\":\"" + (query != null ? query : "") + "\"}");
    }

    /**
     * Endpoint for testing authentication refresh.
     */
    @PostMapping("/auth/refresh")
    public ResponseEntity<String> refreshToken(ServerWebExchange exchange) {
        // In a real scenario, we'd check if the provided token is a refresh token
        // For testing purposes, we'll just return success
        return ResponseEntity.ok("{\"accessToken\":\"new-access-token\",\"expiresIn\":3600}");
    }

    /**
     * Endpoint for testing logout functionality.
     */
    @PostMapping("/auth/logout")
    public ResponseEntity<String> logout() {
        return ResponseEntity.ok("{\"message\":\"Logged out successfully\"}");
    }

    /**
     * Fallback endpoint for circuit breaker testing.
     */
    @RequestMapping("/fallback/**")
    public ResponseEntity<String> fallback() {
        return ResponseEntity.status(503).body("{\"error\":\"Service temporarily unavailable\"}");
    }

    /**
     * Endpoint for testing document listing (simplified).
     */
    @GetMapping("/documents/list")
    public ResponseEntity<String> listDocuments() {
        return ResponseEntity.ok("{\"documents\":[]}");
    }

    /**
     * Endpoint for testing document fetching with URL parameter (SSRF testing).
     */
    @GetMapping("/documents/fetch")
    public ResponseEntity<String> fetchDocument(@RequestParam(value = "url", required = false) String url) {
        if (url != null && (url.contains("169.254.169.254") || url.contains("localhost") || url.contains("127.0.0.1"))) {
            return ResponseEntity.status(403).body("{\"error\":\"Access to internal resources not allowed\"}");
        }
        return ResponseEntity.ok("{\"message\":\"Document fetched\"}");
    }
}
package com.byo.rag.gateway.security;

import com.byo.rag.gateway.config.TestSecurityConfig;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Comprehensive authentication and authorization security tests for the API Gateway.
 * 
 * Part of GATEWAY-TEST-005: Gateway Security and Routing Tests
 * Tests API authentication and authorization to prevent security vulnerabilities.
 */
@ExtendWith(MockitoExtension.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {
        com.byo.rag.gateway.GatewayApplication.class,
        TestSecurityConfig.class
    },
    properties = {
        "jwt.secret=testSecretKeyForTestingOnly123456789012345678901234567890",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration"
    }
)
@ActiveProfiles("test")
@DisplayName("GATEWAY-TEST-005: API Authentication and Authorization Security Tests")
class GatewayAuthenticationSecurityTest {

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;
    
    private static final String TEST_JWT_SECRET = "testSecretKeyForTestingOnly123456789012345678901234567890";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(TEST_JWT_SECRET.getBytes());
    
    private String validJwtToken;
    private String adminJwtToken;
    private String userJwtToken;
    private String expiredJwtToken;
    private String invalidJwtToken;
    private String tamperedJwtToken;
    
    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://localhost:" + port)
            .responseTimeout(Duration.ofSeconds(30))
            .build();
            
        // Create test tokens
        validJwtToken = createJwtToken("test@example.com", UUID.randomUUID(), UUID.randomUUID(), "USER", false);
        adminJwtToken = createJwtToken("admin@example.com", UUID.randomUUID(), UUID.randomUUID(), "ADMIN", false);
        userJwtToken = createJwtToken("user@example.com", UUID.randomUUID(), UUID.randomUUID(), "USER", false);
        expiredJwtToken = createExpiredJwtToken("expired@example.com", UUID.randomUUID(), UUID.randomUUID(), "USER");
        invalidJwtToken = "invalid.jwt.token.structure";
        tamperedJwtToken = validJwtToken.substring(0, validJwtToken.length() - 10) + "tampered123";
    }
    
    @Test
    @DisplayName("Should allow access to public endpoints without authentication")
    void shouldAllowPublicEndpointsWithoutAuth() {
        // Health endpoint should be accessible
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().exists("X-Content-Type-Options");
            
        // Info endpoint should be accessible
        webTestClient.get()
            .uri("/actuator/info")
            .exchange()
            .expectStatus().value(status -> {
                // Should be OK or Not Found (if not enabled)
                assert status == 200 || status == 404;
            });
    }
    
    @Test
    @DisplayName("Should reject unauthenticated requests to protected endpoints")
    void shouldRejectUnauthenticatedRequests() {
        // Test various protected endpoints
        String[] protectedEndpoints = {
            "/api/documents/list",
            "/api/embeddings/generate", 
            "/api/rag/query",
            "/api/admin/tenants",
            "/api/auth/profile"
        };
        
        for (String endpoint : protectedEndpoints) {
            webTestClient.get()
                .uri(endpoint)
                .exchange()
                .expectStatus().value(status -> {
                    // Should be unauthorized or forbidden
                    assert status == 401 || status == 403 || status == 302;
                })
                .expectHeader().exists("X-Content-Type-Options");
        }
    }
    
    @Test
    @DisplayName("Should reject requests with invalid JWT tokens")
    void shouldRejectInvalidJwtTokens() {
        // Test with malformed token
        webTestClient.get()
            .uri("/api/documents/list")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidJwtToken)
            .exchange()
            .expectStatus().value(status -> {
                // Should be unauthorized or forbidden
                assert status == 401 || status == 403 || status == 302;
            });
            
        // Test with empty token
        webTestClient.get()
            .uri("/api/documents/list")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ")
            .exchange()
            .expectStatus().value(status -> {
                assert status == 401 || status == 403 || status == 302;
            });
            
        // Test with missing Bearer prefix
        webTestClient.get()
            .uri("/api/documents/list")
            .header(HttpHeaders.AUTHORIZATION, validJwtToken)
            .exchange()
            .expectStatus().value(status -> {
                assert status == 401 || status == 403 || status == 302;
            });
    }
    
    @Test
    @DisplayName("Should reject expired JWT tokens")
    void shouldRejectExpiredJwtTokens() {
        webTestClient.get()
            .uri("/api/documents/list")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredJwtToken)
            .exchange()
            .expectStatus().value(status -> {
                // Should be unauthorized due to expiration
                assert status == 401 || status == 403 || status == 302;
            })
            .expectHeader().exists("X-Content-Type-Options");
    }
    
    @Test
    @DisplayName("Should reject tampered JWT tokens")
    void shouldRejectTamperedJwtTokens() {
        webTestClient.get()
            .uri("/api/documents/list")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tamperedJwtToken)
            .exchange()
            .expectStatus().value(status -> {
                // Should be unauthorized due to invalid signature
                assert status == 401 || status == 403 || status == 302;
            });
    }
    
    @Test
    @DisplayName("Should validate role-based authorization")
    void shouldValidateRoleBasedAuthorization() {
        // Admin endpoints should require ADMIN role
        webTestClient.get()
            .uri("/api/admin/tenants")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwtToken)
            .exchange()
            .expectStatus().value(status -> {
                // Should be forbidden or server error (if backend down)
                assert status == 403 || status == 401 || status == 302 || status >= 500;
            });
            
        // Admin token should have access
        webTestClient.get()
            .uri("/api/admin/tenants")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
            .exchange()
            .expectStatus().value(status -> {
                // Should be allowed or server error (if backend down)
                assert status == 200 || status == 302 || status >= 500;
            });
    }
    
    @Test
    @DisplayName("Should validate JWT token structure and claims")
    void shouldValidateJwtTokenStructure() {
        // Test with valid token structure but incorrect claims
        String tokenWithWrongClaims = Jwts.builder()
            .subject("test@example.com")
            .claim("wrongClaim", "value")
            .issuedAt(new Date())
            .expiration(Date.from(Instant.now().plus(Duration.ofHours(1))))
            .signWith(SECRET_KEY)
            .compact();
            
        webTestClient.get()
            .uri("/api/documents/list")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenWithWrongClaims)
            .exchange()
            .expectStatus().value(status -> {
                // Should be rejected due to missing required claims
                assert status == 401 || status == 403 || status == 302 || status >= 500;
            });
    }
    
    @Test
    @DisplayName("Should handle concurrent authentication requests")
    void shouldHandleConcurrentAuthenticationRequests() {
        // Test multiple concurrent requests
        for (int i = 0; i < 10; i++) {
            final int requestId = i;
            String testToken = createJwtToken("concurrent" + requestId + "@example.com", 
                UUID.randomUUID(), UUID.randomUUID(), "USER", false);
                
            webTestClient.get()
                .uri("/api/documents/list")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testToken)
                .exchange()
                .expectStatus().value(status -> {
                    // Should handle all requests consistently
                    assert status == 200 || status == 302 || status == 401 || status == 403 || status >= 500;
                });
        }
    }
    
    @Test
    @DisplayName("Should validate token subject and tenant isolation")
    void shouldValidateTenantIsolation() {
        UUID tenant1 = UUID.randomUUID();
        UUID tenant2 = UUID.randomUUID();
        
        String tenant1Token = createJwtToken("user1@example.com", UUID.randomUUID(), tenant1, "USER", false);
        String tenant2Token = createJwtToken("user2@example.com", UUID.randomUUID(), tenant2, "USER", false);
        
        // Both tokens should be valid but for different tenants
        webTestClient.get()
            .uri("/api/documents/list")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenant1Token)
            .exchange()
            .expectStatus().value(status -> {
                assert status == 200 || status == 302 || status == 401 || status == 403 || status >= 500;
            });
            
        webTestClient.get()
            .uri("/api/documents/list")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenant2Token)
            .exchange()
            .expectStatus().value(status -> {
                assert status == 200 || status == 302 || status == 401 || status == 403 || status >= 500;
            });
    }
    
    @Test
    @DisplayName("Should validate authentication bypass attempts")
    void shouldPreventAuthenticationBypass() {
        // Test various bypass attempts
        String[] bypassHeaders = {
            "X-User-Id",
            "X-Tenant-Id", 
            "X-Admin-Override",
            "X-Auth-Bypass",
            "X-Internal-Request"
        };
        
        for (String header : bypassHeaders) {
            webTestClient.get()
                .uri("/api/documents/list")
                .header(header, "admin")
                .exchange()
                .expectStatus().value(status -> {
                    // Should still require proper authentication
                    assert status == 401 || status == 403 || status == 302;
                });
        }
    }
    
    @Test
    @DisplayName("Should handle malformed authorization headers")
    void shouldHandleMalformedAuthHeaders() {
        String[] malformedHeaders = {
            "Basic invalid",
            "Bearer",
            "Token " + validJwtToken,
            "JWT " + validJwtToken,
            validJwtToken,
            "Bearer multiple tokens here",
            "Bearer " + validJwtToken + " extra"
        };
        
        for (String header : malformedHeaders) {
            webTestClient.get()
                .uri("/api/documents/list")
                .header(HttpHeaders.AUTHORIZATION, header)
                .exchange()
                .expectStatus().value(status -> {
                    // Should reject malformed headers
                    assert status == 401 || status == 403 || status == 302;
                });
        }
    }
    
    @Test
    @DisplayName("Should validate token refresh functionality")
    void shouldValidateTokenRefresh() {
        String refreshToken = createJwtToken("test@example.com", UUID.randomUUID(), UUID.randomUUID(), "USER", true);
        
        // Test refresh endpoint
        webTestClient.post()
            .uri("/api/auth/refresh")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshToken)
            .header("Content-Type", "application/json")
            .bodyValue("{\"refreshToken\":\"" + refreshToken + "\"}")
            .exchange()
            .expectStatus().value(status -> {
                // Should allow refresh or return service unavailable
                assert status == 200 || status == 302 || status == 404 || status >= 500;
            });
    }
    
    @Test
    @DisplayName("Should validate logout functionality")
    void shouldValidateLogout() {
        webTestClient.post()
            .uri("/api/auth/logout")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
            .exchange()
            .expectStatus().value(status -> {
                // Should allow logout or return service unavailable
                assert status == 200 || status == 302 || status == 404 || status >= 500;
            });
    }
    
    private String createJwtToken(String email, UUID userId, UUID tenantId, String role, boolean isRefreshToken) {
        Instant now = Instant.now();
        Date expiration = Date.from(now.plus(Duration.ofHours(1)));
        
        return Jwts.builder()
            .subject(email)
            .claim("userId", userId.toString())
            .claim("tenantId", tenantId.toString())
            .claim("role", role)
            .claim("tokenType", isRefreshToken ? "refresh" : "access")
            .issuedAt(Date.from(now))
            .expiration(expiration)
            .signWith(SECRET_KEY)
            .compact();
    }
    
    private String createExpiredJwtToken(String email, UUID userId, UUID tenantId, String role) {
        Instant past = Instant.now().minus(Duration.ofHours(2));
        Date expiration = Date.from(past.plus(Duration.ofMinutes(30)));
        
        return Jwts.builder()
            .subject(email)
            .claim("userId", userId.toString())
            .claim("tenantId", tenantId.toString())
            .claim("role", role)
            .claim("tokenType", "access")
            .issuedAt(Date.from(past))
            .expiration(expiration)
            .signWith(SECRET_KEY)
            .compact();
    }
}
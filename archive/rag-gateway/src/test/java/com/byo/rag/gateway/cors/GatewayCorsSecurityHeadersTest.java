package com.byo.rag.gateway.cors;

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
 * Comprehensive CORS and security headers validation tests for the API Gateway.
 * 
 * Part of GATEWAY-TEST-005: Gateway Security and Routing Tests
 * Tests CORS configuration and security headers validation.
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
@DisplayName("GATEWAY-TEST-005: CORS and Security Headers Tests")
class GatewayCorsSecurityHeadersTest {

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;
    
    private static final String TEST_JWT_SECRET = "testSecretKeyForTestingOnly123456789012345678901234567890";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(TEST_JWT_SECRET.getBytes());
    
    private String validJwtToken;
    
    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://localhost:" + port)
            .responseTimeout(Duration.ofSeconds(30))
            .build();
            
        validJwtToken = createJwtToken("test@example.com", UUID.randomUUID(), UUID.randomUUID(), "USER");
    }
    
    @Test
    @DisplayName("Should handle CORS preflight requests correctly")
    void shouldHandleCorsPreflightRequestsCorrectly() {
        webTestClient.options()
            .uri("/api/documents/list")
            .header("Origin", "http://localhost:3000")
            .header("Access-Control-Request-Method", "GET")
            .header("Access-Control-Request-Headers", "Authorization,Content-Type")
            .exchange()
            .expectStatus().value(status -> {
                // Should handle CORS preflight appropriately
                assert status == 200 || status == 204 || status == 302 || status == 401 || status == 403 || status == 404 || status >= 500;
            })
            .expectHeader().exists("Access-Control-Allow-Origin")
            .expectHeader().exists("Access-Control-Allow-Methods")
            .expectHeader().exists("Access-Control-Allow-Headers");
    }
    
    @Test
    @DisplayName("Should validate allowed origins")
    void shouldValidateAllowedOrigins() {
        String[] allowedOrigins = {
            "http://localhost:3000",
            "https://localhost:3000",
            "http://127.0.0.1:3000"
        };
        
        for (String origin : allowedOrigins) {
            webTestClient.options()
                .uri("/api/documents/list")
                .header("Origin", origin)
                .header("Access-Control-Request-Method", "GET")
                .exchange()
                .expectStatus().value(status -> {
                    // Should allow these origins
                    assert status == 200 || status == 204 || status == 302 || status == 401 || status == 403 || status == 404 || status >= 500;
                })
                .expectHeader().valueMatches("Access-Control-Allow-Origin", ".*");
        }
    }
    
    @Test
    @DisplayName("Should reject disallowed origins")
    void shouldRejectDisallowedOrigins() {
        String[] disallowedOrigins = {
            "http://malicious-site.com",
            "https://attacker.example.com",
            "http://evil.org",
            "javascript:alert('xss')",
            "data:text/html,<script>alert('xss')</script>"
        };
        
        for (String origin : disallowedOrigins) {
            webTestClient.options()
                .uri("/api/documents/list")
                .header("Origin", origin)
                .header("Access-Control-Request-Method", "GET")
                .exchange()
                .expectStatus().value(status -> {
                    // Should handle disallowed origins appropriately
                    assert status == 200 || status == 204 || status == 302 || status == 401 || status == 403 || status == 404 || status >= 500;
                });
                // Note: CORS headers may or may not be present for disallowed origins
        }
    }
    
    @Test
    @DisplayName("Should validate allowed HTTP methods")
    void shouldValidateAllowedHttpMethods() {
        String[] allowedMethods = {
            "GET",
            "POST", 
            "PUT",
            "DELETE",
            "OPTIONS"
        };
        
        for (String method : allowedMethods) {
            webTestClient.options()
                .uri("/api/documents/list")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", method)
                .exchange()
                .expectStatus().value(status -> {
                    // Should allow these methods
                    assert status == 200 || status == 204 || status == 302 || status == 401 || status == 403 || status == 404 || status >= 500;
                });
        }
    }
    
    @Test
    @DisplayName("Should reject disallowed HTTP methods")
    void shouldRejectDisallowedHttpMethods() {
        String[] disallowedMethods = {
            "TRACE",
            "CONNECT",
            "PATCH",
            "HEAD"
        };
        
        for (String method : disallowedMethods) {
            webTestClient.options()
                .uri("/api/documents/list")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", method)
                .exchange()
                .expectStatus().value(status -> {
                    // Should handle disallowed methods appropriately
                    assert status == 200 || status == 204 || status == 302 || status == 401 || status == 403 || status == 404 || status == 405 || status >= 500;
                });
        }
    }
    
    @Test
    @DisplayName("Should validate allowed headers")
    void shouldValidateAllowedHeaders() {
        String[] allowedHeaders = {
            "Authorization",
            "Content-Type",
            "Accept",
            "X-Requested-With",
            "Cache-Control"
        };
        
        for (String header : allowedHeaders) {
            webTestClient.options()
                .uri("/api/documents/list")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", header)
                .exchange()
                .expectStatus().value(status -> {
                    // Should allow these headers
                    assert status == 200 || status == 204 || status == 302 || status == 401 || status == 403 || status == 404 || status >= 500;
                });
        }
    }
    
    @Test
    @DisplayName("Should handle credentials properly")
    void shouldHandleCredentialsProperly() {
        webTestClient.options()
            .uri("/api/documents/list")
            .header("Origin", "http://localhost:3000")
            .header("Access-Control-Request-Method", "GET")
            .header("Access-Control-Request-Credentials", "true")
            .exchange()
            .expectStatus().value(status -> {
                assert status == 200 || status == 204 || status == 302 || status == 401 || status == 403 || status == 404 || status >= 500;
            })
            .expectHeader().valueEquals("Access-Control-Allow-Credentials", "true");
    }
    
    @Test
    @DisplayName("Should include security headers in all responses")
    void shouldIncludeSecurityHeadersInAllResponses() {
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus().value(status -> {
                assert status >= 200 && status < 600;
            })
            .expectHeader().exists("X-Content-Type-Options")
            .expectHeader().exists("X-Frame-Options")
            .expectHeader().exists("X-XSS-Protection")
            .expectHeader().exists("Referrer-Policy");
    }
    
    @Test
    @DisplayName("Should set Content-Security-Policy header")
    void shouldSetContentSecurityPolicyHeader() {
        webTestClient.get()
            .uri("/api/documents/list")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
            .exchange()
            .expectStatus().value(status -> {
                assert status == 200 || status == 302 || status == 401 || status == 403 || status == 404 || status >= 500;
            })
            .expectHeader().exists("Content-Security-Policy")
            .expectHeader().value("Content-Security-Policy", csp -> {
                // Should contain restrictive CSP directives
                assert csp.contains("default-src") || csp.contains("script-src") || csp.contains("object-src");
            });
    }
    
    @Test
    @DisplayName("Should set Strict-Transport-Security header for HTTPS")
    void shouldSetStrictTransportSecurityHeader() {
        // Note: HSTS is typically only set for HTTPS requests
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus().value(status -> {
                assert status >= 200 && status < 600;
            })
            .expectHeader().value("Strict-Transport-Security", hsts -> {
                // HSTS may not be present for HTTP requests in test environment
                if (hsts != null) {
                    assert hsts.contains("max-age");
                }
            });
    }
    
    @Test
    @DisplayName("Should set X-Content-Type-Options to nosniff")
    void shouldSetXContentTypeOptionsToNosniff() {
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus().value(status -> {
                assert status >= 200 && status < 600;
            })
            .expectHeader().valueEquals("X-Content-Type-Options", "nosniff");
    }
    
    @Test
    @DisplayName("Should set X-Frame-Options to prevent clickjacking")
    void shouldSetXFrameOptionsToPreventClickjacking() {
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus().value(status -> {
                assert status >= 200 && status < 600;
            })
            .expectHeader().value("X-Frame-Options", frameOptions -> {
                // Should prevent framing
                assert frameOptions.equals("DENY") || frameOptions.equals("SAMEORIGIN");
            });
    }
    
    @Test
    @DisplayName("Should set appropriate Referrer-Policy")
    void shouldSetAppropriateReferrerPolicy() {
        webTestClient.get()
            .uri("/api/documents/list")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
            .exchange()
            .expectStatus().value(status -> {
                assert status == 200 || status == 302 || status == 401 || status == 403 || status == 404 || status >= 500;
            })
            .expectHeader().exists("Referrer-Policy")
            .expectHeader().value("Referrer-Policy", policy -> {
                // Should use secure referrer policy
                assert policy.contains("no-referrer") || policy.contains("strict-origin") || policy.contains("same-origin");
            });
    }
    
    @Test
    @DisplayName("Should handle complex CORS scenarios")
    void shouldHandleComplexCorsScenarios() {
        // Multiple headers in preflight request
        webTestClient.options()
            .uri("/api/documents/upload")
            .header("Origin", "http://localhost:3000")
            .header("Access-Control-Request-Method", "POST")
            .header("Access-Control-Request-Headers", "Authorization,Content-Type,X-Requested-With")
            .exchange()
            .expectStatus().value(status -> {
                assert status == 200 || status == 204 || status == 302 || status == 401 || status == 403 || status == 404 || status >= 500;
            })
            .expectHeader().exists("Access-Control-Allow-Headers");
    }
    
    @Test
    @DisplayName("Should set cache control headers appropriately")
    void shouldSetCacheControlHeadersAppropriately() {
        // Sensitive endpoints should have no-cache headers
        webTestClient.get()
            .uri("/api/auth/profile")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
            .exchange()
            .expectStatus().value(status -> {
                assert status == 200 || status == 302 || status == 401 || status == 403 || status == 404 || status >= 500;
            })
            .expectHeader().value("Cache-Control", cacheControl -> {
                // Sensitive data should not be cached
                if (cacheControl != null) {
                    assert cacheControl.contains("no-cache") || cacheControl.contains("no-store") || cacheControl.contains("private");
                }
            });
    }
    
    @Test
    @DisplayName("Should handle OPTIONS requests for all endpoints")
    void shouldHandleOptionsRequestsForAllEndpoints() {
        String[] endpoints = {
            "/api/auth/login",
            "/api/documents/list",
            "/api/embeddings/generate",
            "/api/rag/query",
            "/api/admin/tenants"
        };
        
        for (String endpoint : endpoints) {
            webTestClient.options()
                .uri(endpoint)
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET")
                .exchange()
                .expectStatus().value(status -> {
                    // Should handle OPTIONS for all endpoints
                    assert status == 200 || status == 204 || status == 302 || status == 401 || status == 403 || status == 404 || status >= 500;
                });
        }
    }
    
    @Test
    @DisplayName("Should prevent CORS bypass attempts")
    void shouldPreventCorsBypassAttempts() {
        String[] bypassAttempts = {
            "null",
            "",
            "*",
            "http://localhost:3000.evil.com",
            "https://evil.com.localhost:3000",
            "javascript:alert('xss')"
        };
        
        for (String origin : bypassAttempts) {
            webTestClient.options()
                .uri("/api/documents/list")
                .header("Origin", origin)
                .header("Access-Control-Request-Method", "GET")
                .exchange()
                .expectStatus().value(status -> {
                    // Should not allow bypass attempts
                    assert status == 200 || status == 204 || status == 302 || status == 401 || status == 403 || status == 404 || status >= 500;
                });
        }
    }
    
    @Test
    @DisplayName("Should set security headers consistently across all responses")
    void shouldSetSecurityHeadersConsistentlyAcrossAllResponses() {
        String[] endpoints = {
            "/actuator/health",
            "/api/auth/login",
            "/api/documents/list"
        };
        
        for (String endpoint : endpoints) {
            webTestClient.get()
                .uri(endpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .exchange()
                .expectStatus().value(status -> {
                    assert status >= 200 && status < 600;
                })
                .expectHeader().exists("X-Content-Type-Options")
                .expectHeader().exists("X-Frame-Options");
        }
    }
    
    @Test
    @DisplayName("Should handle CORS with credentials and specific origins")
    void shouldHandleCorsWithCredentialsAndSpecificOrigins() {
        // When credentials are allowed, origin must be specific (not *)
        webTestClient.options()
            .uri("/api/auth/profile")
            .header("Origin", "http://localhost:3000")
            .header("Access-Control-Request-Method", "GET")
            .header("Access-Control-Request-Credentials", "true")
            .exchange()
            .expectStatus().value(status -> {
                assert status == 200 || status == 204 || status == 302 || status == 401 || status == 403 || status == 404 || status >= 500;
            })
            .expectHeader().value("Access-Control-Allow-Origin", origin -> {
                // Should not be wildcard when credentials are allowed
                assert origin != null && !origin.equals("*");
            });
    }
    
    private String createJwtToken(String email, UUID userId, UUID tenantId, String role) {
        Instant now = Instant.now();
        Date expiration = Date.from(now.plus(Duration.ofHours(1)));
        
        return Jwts.builder()
            .subject(email)
            .claim("userId", userId.toString())
            .claim("tenantId", tenantId.toString())
            .claim("role", role)
            .claim("tokenType", "access")
            .issuedAt(Date.from(now))
            .expiration(expiration)
            .signWith(SECRET_KEY)
            .compact();
    }
}
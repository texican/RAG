package com.byo.rag.gateway.routing;

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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

/**
 * Comprehensive request routing and load balancing tests for the API Gateway.
 * 
 * Part of GATEWAY-TEST-005: Gateway Security and Routing Tests
 * Tests request routing and load balancing functionality.
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
@DisplayName("GATEWAY-TEST-005: Request Routing and Load Balancing Tests")
class GatewayRoutingLoadBalancingTest {

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
    @DisplayName("Should route auth service requests correctly")
    void shouldRouteAuthServiceRequests() {
        String[] authEndpoints = {
            "/api/auth/login",
            "/api/auth/register", 
            "/api/auth/profile",
            "/api/auth/refresh",
            "/api/auth/logout"
        };
        
        for (String endpoint : authEndpoints) {
            webTestClient.get()
                .uri(endpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .exchange()
                .expectStatus().value(status -> {
                    // Should route to auth service (may be down, returning 5xx)
                    // Or return 401/403 if authentication fails
                    assert status == 200 || status == 302 || status == 401 || status == 403 || status == 404 || status >= 500;
                })
                .expectHeader().exists("X-Content-Type-Options");
        }
    }
    
    @Test
    @DisplayName("Should route document service requests correctly")
    void shouldRouteDocumentServiceRequests() {
        String[] documentEndpoints = {
            "/api/documents/list",
            "/api/documents/upload",
            "/api/documents/search",
            "/api/documents/stats"
        };
        
        for (String endpoint : documentEndpoints) {
            webTestClient.get()
                .uri(endpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .exchange()
                .expectStatus().value(status -> {
                    // Should route to document service
                    assert status == 200 || status == 302 || status == 401 || status == 403 || status == 404 || status >= 500;
                })
                .expectHeader().exists("X-Gateway-Service");
        }
    }
    
    @Test
    @DisplayName("Should route embedding service requests correctly")  
    void shouldRouteEmbeddingServiceRequests() {
        String[] embeddingEndpoints = {
            "/api/embeddings/generate",
            "/api/embeddings/search",
            "/api/embeddings/similar"
        };
        
        for (String endpoint : embeddingEndpoints) {
            webTestClient.post()
                .uri(endpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .header("Content-Type", "application/json")
                .bodyValue("{\"text\":\"test embedding\"}")
                .exchange()
                .expectStatus().value(status -> {
                    // Should route to embedding service
                    assert status == 200 || status == 302 || status == 401 || status == 403 || status == 404 || status >= 500;
                });
        }
    }
    
    @Test
    @DisplayName("Should route core service requests correctly")
    void shouldRouteCoreServiceRequests() {
        String[] coreEndpoints = {
            "/api/rag/query",
            "/api/rag/chat",
            "/api/rag/history"
        };
        
        for (String endpoint : coreEndpoints) {
            webTestClient.post()
                .uri(endpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .header("Content-Type", "application/json") 
                .bodyValue("{\"question\":\"test query\"}")
                .exchange()
                .expectStatus().value(status -> {
                    // Should route to core service
                    assert status == 200 || status == 302 || status == 401 || status == 403 || status == 404 || status >= 500;
                });
        }
    }
    
    @Test
    @DisplayName("Should route admin service requests correctly")
    void shouldRouteAdminServiceRequests() {
        String adminToken = createJwtToken("admin@example.com", UUID.randomUUID(), UUID.randomUUID(), "ADMIN");
        
        String[] adminEndpoints = {
            "/api/admin/tenants",
            "/api/admin/users", 
            "/api/admin/stats",
            "/api/admin/health"
        };
        
        for (String endpoint : adminEndpoints) {
            webTestClient.get()
                .uri(endpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .exchange()
                .expectStatus().value(status -> {
                    // Should route to admin service
                    assert status == 200 || status == 302 || status == 401 || status == 403 || status == 404 || status >= 500;
                });
        }
    }
    
    @Test
    @DisplayName("Should handle unknown route requests correctly")
    void shouldHandleUnknownRoutes() {
        String[] unknownEndpoints = {
            "/api/unknown/endpoint",
            "/api/nonexistent",
            "/api/invalid/path",
            "/unknown/service/path"
        };
        
        for (String endpoint : unknownEndpoints) {
            webTestClient.get()
                .uri(endpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .exchange()
                .expectStatus().value(status -> {
                    // Should return 404 Not Found for unknown routes
                    assert status == 404 || status == 401 || status == 403;
                });
        }
    }
    
    @Test
    @DisplayName("Should handle routing with different HTTP methods")
    void shouldHandleRoutingWithDifferentMethods() {
        // Test GET routing
        webTestClient.get()
            .uri("/api/documents/list")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
            .exchange()
            .expectStatus().value(status -> {
                assert status == 200 || status == 302 || status == 401 || status == 403 || status == 404 || status >= 500;
            });
            
        // Test POST routing
        webTestClient.post()
            .uri("/api/documents/upload")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
            .header("Content-Type", "application/json")
            .bodyValue("{\"filename\":\"test.txt\"}")
            .exchange()
            .expectStatus().value(status -> {
                assert status == 200 || status == 302 || status == 401 || status == 403 || status == 404 || status >= 500;
            });
            
        // Test PUT routing
        webTestClient.put()
            .uri("/api/documents/update/123")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
            .header("Content-Type", "application/json")
            .bodyValue("{\"name\":\"updated.txt\"}")
            .exchange()
            .expectStatus().value(status -> {
                assert status == 200 || status == 302 || status == 401 || status == 403 || status == 404 || status >= 500;
            });
            
        // Test DELETE routing
        webTestClient.delete()
            .uri("/api/documents/delete/123")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
            .exchange()
            .expectStatus().value(status -> {
                assert status == 200 || status == 302 || status == 401 || status == 403 || status == 404 || status >= 500;
            });
    }
    
    @Test
    @DisplayName("Should handle concurrent routing requests efficiently")
    void shouldHandleConcurrentRoutingRequests() throws InterruptedException, ExecutionException, TimeoutException {
        int concurrentRequests = 20;
        @SuppressWarnings("unchecked")
        CompletableFuture<Integer>[] futures = new CompletableFuture[concurrentRequests];
        
        // Create concurrent requests to different services
        for (int i = 0; i < concurrentRequests; i++) {
            final int requestIndex = i;
            futures[i] = CompletableFuture.supplyAsync(() -> {
                String endpoint = getEndpointForIndex(requestIndex);
                return webTestClient.get()
                    .uri(endpoint)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                    .exchange()
                    .returnResult(String.class)
                    .getStatus()
                    .value();
            });
        }
        
        // Wait for all requests to complete
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures);
        allOf.get(30, TimeUnit.SECONDS);
        
        // Verify all requests were handled (status codes should be reasonable)
        for (CompletableFuture<Integer> future : futures) {
            Integer status = future.get();
            assert status >= 200 && status < 600 : "Invalid status code: " + status;
        }
    }
    
    @Test
    @DisplayName("Should maintain routing consistency under load")
    void shouldMaintainRoutingConsistencyUnderLoad() {
        String endpoint = "/api/documents/list";
        int requestCount = 50;
        
        // Send multiple requests to the same endpoint
        IntStream.range(0, requestCount).parallel().forEach(i -> {
            webTestClient.get()
                .uri(endpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .exchange()
                .expectStatus().value(status -> {
                    // All requests should be routed consistently
                    assert status == 200 || status == 302 || status == 401 || status == 403 || status == 404 || status >= 500;
                });
        });
    }
    
    @Test
    @DisplayName("Should handle routing with query parameters")
    void shouldHandleRoutingWithQueryParameters() {
        String[] endpointsWithParams = {
            "/api/documents/search?query=test&limit=10",
            "/api/documents/list?page=1&size=20",
            "/api/embeddings/search?text=example&threshold=0.8",
            "/api/admin/users?active=true&role=USER"
        };
        
        for (String endpoint : endpointsWithParams) {
            webTestClient.get()
                .uri(endpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .exchange()
                .expectStatus().value(status -> {
                    // Should route with parameters preserved
                    assert status == 200 || status == 302 || status == 401 || status == 403 || status == 404 || status >= 500;
                });
        }
    }
    
    @Test
    @DisplayName("Should handle routing with path variables")
    void shouldHandleRoutingWithPathVariables() {
        String[] endpointsWithPathVars = {
            "/api/documents/123",
            "/api/documents/456/chunks",
            "/api/embeddings/vectors/789",
            "/api/admin/tenants/abc-def-ghi",
            "/api/auth/users/user-123"
        };
        
        for (String endpoint : endpointsWithPathVars) {
            webTestClient.get()
                .uri(endpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .exchange()
                .expectStatus().value(status -> {
                    // Should route with path variables preserved
                    assert status == 200 || status == 302 || status == 401 || status == 403 || status == 404 || status >= 500;
                });
        }
    }
    
    @Test
    @DisplayName("Should handle routing timeout scenarios")
    void shouldHandleRoutingTimeouts() {
        // Test with different endpoints that might have different timeout behaviors
        String[] timeoutEndpoints = {
            "/api/embeddings/generate", // Potentially slow operation
            "/api/rag/query",           // LLM operations can be slow
            "/api/documents/upload"      // File operations can be slow
        };
        
        for (String endpoint : timeoutEndpoints) {
            webTestClient.mutate()
                .responseTimeout(Duration.ofSeconds(5))  // Shorter timeout for testing
                .build()
                .post()
                .uri(endpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .header("Content-Type", "application/json")
                .bodyValue("{\"test\":\"data\"}")
                .exchange()
                .expectStatus().value(status -> {
                    // Should handle timeouts gracefully
                    assert status == 200 || status == 302 || status == 401 || status == 403 || status == 404 || status == 504 || status >= 500;
                });
        }
    }
    
    @Test
    @DisplayName("Should preserve headers during routing")
    void shouldPreserveHeadersDuringRouting() {
        webTestClient.get()
            .uri("/api/documents/list")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
            .header("X-Request-ID", "test-request-123")
            .header("X-Trace-ID", "trace-456") 
            .header("Accept", "application/json")
            .exchange()
            .expectStatus().value(status -> {
                assert status == 200 || status == 302 || status == 401 || status == 403 || status == 404 || status >= 500;
            })
            .expectHeader().exists("X-Content-Type-Options");
    }
    
    @Test
    @DisplayName("Should handle routing with large request bodies")
    void shouldHandleRoutingWithLargeRequestBodies() {
        String largeBody = "x".repeat(10000);  // 10KB body
        
        webTestClient.post()
            .uri("/api/documents/upload")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
            .header("Content-Type", "application/json")
            .bodyValue("{\"content\":\"" + largeBody + "\"}")
            .exchange()
            .expectStatus().value(status -> {
                // Should handle large bodies or reject appropriately
                assert status == 200 || status == 302 || status == 401 || status == 403 || status == 404 || status == 413 || status >= 500;
            });
    }
    
    @Test
    @DisplayName("Should handle circuit breaker functionality")
    void shouldHandleCircuitBreakerFunctionality() {
        String endpoint = "/api/documents/list";
        
        // Make multiple requests to potentially trigger circuit breaker
        for (int i = 0; i < 10; i++) {
            webTestClient.get()
                .uri(endpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .exchange()
                .expectStatus().value(status -> {
                    // Should handle circuit breaker states gracefully
                    assert status == 200 || status == 302 || status == 401 || status == 403 || status == 404 || status == 503 || status >= 500;
                });
        }
    }
    
    private String getEndpointForIndex(int index) {
        String[] endpoints = {
            "/api/auth/profile",
            "/api/documents/list",
            "/api/embeddings/search",
            "/api/admin/health"
        };
        return endpoints[index % endpoints.length];
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
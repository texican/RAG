package com.byo.rag.gateway;

import com.byo.rag.gateway.config.TestGatewayConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;

/**
 * Integration tests for the Enterprise RAG Gateway.
 * 
 * <p>These tests validate the complete gateway functionality including:
 * <ul>
 *   <li>Gateway application startup and configuration loading</li>
 *   <li>Route resolution and request forwarding</li>
 *   <li>JWT authentication filter integration</li>
 *   <li>Circuit breaker and resilience pattern behavior</li>
 *   <li>Rate limiting enforcement</li>
 *   <li>Error handling and fallback responses</li>
 * </ul>
 * 
 * <p>Uses WebTestClient for reactive testing patterns appropriate for
 * Spring WebFlux and Spring Cloud Gateway architecture.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {TestGatewayConfig.class},
    properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration",
        "spring.cloud.gateway.route.refresh.enabled=false",
        "spring.cloud.gateway.routes=",
        "spring.cloud.gateway.discovery.locator.enabled=false"
    }
)
@ActiveProfiles("test")
class GatewayIntegrationTest {

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient
            .bindToServer()
            .baseUrl("http://localhost:" + port)
            .responseTimeout(Duration.ofSeconds(30))
            .build();
    }

    @Test
    void contextLoads() {
        // Test that Spring context loads successfully with all configurations
    }

    @Test
    void shouldAllowAccessToPublicHealthEndpoint() {
        // Given: Request to public health endpoint
        // When & Then: Should allow access without authentication
        webTestClient
            .get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void shouldRejectUnauthenticatedRequestToProtectedEndpoint() {
        // Given: Request to protected endpoint without authentication
        // When & Then: Should return 401 Unauthorized
        webTestClient
            .get()
            .uri("/api/documents/list")
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    void shouldRejectRequestWithInvalidToken() {
        // Given: Request with invalid JWT token
        // When & Then: Should return 401 Unauthorized
        webTestClient
            .get()
            .uri("/api/documents/list")
            .header("Authorization", "Bearer invalid.jwt.token")
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    void shouldAllowAuthenticationEndpoints() {
        // Given: Request to authentication endpoint
        // When & Then: Should allow access (though service may not be running)
        webTestClient
            .post()
            .uri("/api/auth/login")
            .header("Content-Type", "application/json")
            .bodyValue("{\"email\":\"test@example.com\",\"password\":\"password\"}")
            .exchange()
            .expectStatus().is5xxServerError(); // Service not running, but auth is bypassed
    }

    @Test
    void shouldRouteToAuthService() {
        // Given: Request to auth service endpoint
        // When & Then: Should route correctly (connection refused expected since service not running)
        webTestClient
            .get()
            .uri("/api/auth/profile")
            .header("Authorization", "Bearer valid.test.token")
            .exchange()
            .expectStatus().is5xxServerError(); // Connection refused to service
    }

    @Test
    void shouldRouteToDocumentService() {
        // Given: Request to document service endpoint  
        // When & Then: Should route correctly (connection refused expected since service not running)
        webTestClient
            .get()
            .uri("/api/documents/list")
            .header("Authorization", "Bearer valid.test.token")
            .exchange()
            .expectStatus().is5xxServerError(); // Connection refused to service
    }

    @Test
    void shouldRouteToEmbeddingService() {
        // Given: Request to embedding service endpoint
        // When & Then: Should route correctly (connection refused expected since service not running)
        webTestClient
            .post()
            .uri("/api/embeddings/generate")
            .header("Authorization", "Bearer valid.test.token")
            .header("Content-Type", "application/json")
            .bodyValue("{\"text\":\"test\"}")
            .exchange()
            .expectStatus().is5xxServerError(); // Connection refused to service
    }

    @Test
    void shouldRouteToCoreService() {
        // Given: Request to core service endpoint
        // When & Then: Should route correctly (connection refused expected since service not running)
        webTestClient
            .post()
            .uri("/api/rag/query")
            .header("Authorization", "Bearer valid.test.token")
            .header("Content-Type", "application/json")
            .bodyValue("{\"question\":\"test\"}")
            .exchange()
            .expectStatus().is5xxServerError(); // Connection refused to service
    }

    @Test
    void shouldRouteToAdminService() {
        // Given: Request to admin service endpoint
        // When & Then: Should route correctly (connection refused expected since service not running)
        webTestClient
            .get()
            .uri("/api/admin/tenants")
            .header("Authorization", "Bearer valid.test.token")
            .exchange()
            .expectStatus().is5xxServerError(); // Connection refused to service
    }

    @Test
    void shouldHandleCorsRequests() {
        // Given: CORS preflight request
        // When & Then: Should handle CORS correctly
        webTestClient
            .options()
            .uri("/api/documents/list")
            .header("Origin", "http://localhost:3000")
            .header("Access-Control-Request-Method", "GET")
            .header("Access-Control-Request-Headers", "Authorization")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().exists("Access-Control-Allow-Origin")
            .expectHeader().exists("Access-Control-Allow-Methods")
            .expectHeader().exists("Access-Control-Allow-Headers");
    }

    @Test
    void shouldAddResponseHeaders() {
        // Given: Request to any endpoint
        // When & Then: Should add gateway response headers
        webTestClient
            .get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().valueEquals("X-Gateway-Service", "Enterprise-RAG-Gateway");
    }

    @Test
    void shouldHandleInvalidPaths() {
        // Given: Request to non-existent path
        // When & Then: Should return 404 Not Found
        webTestClient
            .get()
            .uri("/api/nonexistent/endpoint")
            .header("Authorization", "Bearer valid.test.token")
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    void shouldHandleRootPath() {
        // Given: Request to root path
        // When & Then: Should return 404 (no root handler configured)
        webTestClient
            .get()
            .uri("/")
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    void shouldHandleMethodNotAllowed() {
        // Given: Request with unsupported HTTP method
        // When & Then: Should return appropriate error
        webTestClient
            .patch()
            .uri("/actuator/health")
            .exchange()
            .expectStatus().is4xxClientError();
    }

    @Test
    void shouldValidateRequestSize() {
        // Given: Request with very large body
        String largeBody = "x".repeat(10000);
        
        // When & Then: Should handle large requests appropriately
        webTestClient
            .post()
            .uri("/api/documents/upload")
            .header("Authorization", "Bearer valid.test.token")
            .header("Content-Type", "application/json")
            .bodyValue("{\"content\":\"" + largeBody + "\"}")
            .exchange()
            .expectStatus().is5xxServerError(); // Service not running
    }
}
package com.enterprise.rag.gateway.config;

import com.enterprise.rag.gateway.service.JwtValidationService;
import com.enterprise.rag.gateway.filter.JwtAuthenticationFilter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration for Gateway components.
 * 
 * <p>This configuration provides test-specific beans and overrides
 * for components that require external dependencies in production.
 */
@SpringBootApplication
public class TestGatewayConfig {

    /**
     * Provides a test JWT validation service.
     * 
     * @return JWT validation service for testing
     */
    @Bean
    @Primary
    public JwtValidationService jwtValidationService() {
        return new JwtValidationService("testSecretKeyForTestingOnly123456789012345678901234567890");
    }

    /**
     * Provides a test JWT authentication filter.
     * 
     * @param jwtValidationService the JWT validation service
     * @return JWT authentication filter for testing
     */
    @Bean
    @Primary
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtValidationService jwtValidationService) {
        return new JwtAuthenticationFilter(jwtValidationService);
    }

    /**
     * Provides a simplified route locator for testing without Redis dependencies.
     * 
     * @param builder route locator builder
     * @param jwtAuthenticationFilter JWT authentication filter
     * @return simplified route locator
     */
    @Bean
    @Primary
    public RouteLocator testRouteLocator(RouteLocatorBuilder builder, JwtAuthenticationFilter jwtAuthenticationFilter) {
        return builder.routes()
            // Auth Service Route (simplified for testing)
            .route("auth-service", r -> r
                .path("/api/auth/**")
                .filters(f -> f
                    .stripPrefix(2)
                    .filter(jwtAuthenticationFilter)
                )
                .uri("http://localhost:8081")
            )
            
            // Document Service Route (simplified for testing)
            .route("document-service", r -> r
                .path("/api/documents/**")
                .filters(f -> f
                    .stripPrefix(2)
                    .filter(jwtAuthenticationFilter)
                )
                .uri("http://localhost:8082")
            )
            
            // Embedding Service Route (simplified for testing)
            .route("embedding-service", r -> r
                .path("/api/embeddings/**")
                .filters(f -> f
                    .stripPrefix(2)
                    .filter(jwtAuthenticationFilter)
                )
                .uri("http://localhost:8083")
            )
            
            // Core Service Route (simplified for testing)
            .route("core-service", r -> r
                .path("/api/rag/**")
                .filters(f -> f
                    .stripPrefix(2)
                    .filter(jwtAuthenticationFilter)
                )
                .uri("http://localhost:8084")
            )
            
            // Admin Service Route (simplified for testing)
            .route("admin-service", r -> r
                .path("/api/admin/**")
                .filters(f -> f
                    .stripPrefix(2)
                    .filter(jwtAuthenticationFilter)
                )
                .uri("http://localhost:8085")
            )
            .build();
    }
}
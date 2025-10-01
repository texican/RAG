package com.byo.rag.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 configuration for the RAG API Gateway.
 * 
 * <p>This configuration provides comprehensive API documentation for the
 * API Gateway, which serves as the primary entry point for all external
 * requests to the RAG system.
 * 
 * <p><strong>Features:</strong>
 * <ul>
 *   <li>Unified API access point for all services</li>
 *   <li>JWT authentication and validation</li>
 *   <li>Request routing and load balancing</li>
 *   <li>Rate limiting and security enforcement</li>
 * </ul>
 * 
 * @author RAG Development Team
 * @version 1.0.0
 * @since 2025-09-24
 */
@Configuration
public class OpenApiConfig {

    /**
     * Main OpenAPI configuration for the API Gateway.
     * 
     * @return configured OpenAPI instance with complete metadata
     */
    @Bean
    public OpenAPI gatewayOpenAPI() {
        return new OpenAPI()
                .info(createApiInfo())
                .servers(createServers())
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("Bearer Authentication", createBearerSecurityScheme()));
    }

    /**
     * Grouped API configuration for authentication routes.
     * 
     * @return GroupedOpenApi for auth routes
     */
    @Bean
    public GroupedOpenApi authRoutesApi() {
        return GroupedOpenApi.builder()
                .group("auth-routes")
                .displayName("Authentication Routes")
                .pathsToMatch("/api/auth/**")
                .build();
    }

    /**
     * Grouped API configuration for document routes.
     * 
     * @return GroupedOpenApi for document routes
     */
    @Bean
    public GroupedOpenApi documentRoutesApi() {
        return GroupedOpenApi.builder()
                .group("document-routes")
                .displayName("Document Service Routes")
                .pathsToMatch("/api/documents/**")
                .build();
    }

    /**
     * Grouped API configuration for embedding routes.
     * 
     * @return GroupedOpenApi for embedding routes
     */
    @Bean
    public GroupedOpenApi embeddingRoutesApi() {
        return GroupedOpenApi.builder()
                .group("embedding-routes")
                .displayName("Embedding Service Routes")
                .pathsToMatch("/api/embeddings/**")
                .build();
    }

    /**
     * Grouped API configuration for RAG routes.
     * 
     * @return GroupedOpenApi for RAG routes
     */
    @Bean
    public GroupedOpenApi ragRoutesApi() {
        return GroupedOpenApi.builder()
                .group("rag-routes")
                .displayName("RAG Core Service Routes")
                .pathsToMatch("/api/rag/**")
                .build();
    }

    /**
     * Grouped API configuration for admin routes.
     * 
     * @return GroupedOpenApi for admin routes
     */
    @Bean
    public GroupedOpenApi adminRoutesApi() {
        return GroupedOpenApi.builder()
                .group("admin-routes")
                .displayName("Admin Service Routes")
                .pathsToMatch("/api/admin/**")
                .build();
    }

    /**
     * Creates comprehensive API information metadata.
     * 
     * @return Info object with complete API details
     */
    private Info createApiInfo() {
        return new Info()
                .title("RAG System API Gateway")
                .description("""
                        **Enterprise RAG System API Gateway**
                        
                        The API Gateway serves as the unified entry point for all client applications
                        accessing the RAG (Retrieval Augmented Generation) system services.
                        
                        ## Core Features
                        - **Unified API Access**: Single endpoint for all RAG system services
                        - **Authentication & Authorization**: JWT validation and security enforcement
                        - **Request Routing**: Intelligent routing to appropriate microservices
                        - **Load Balancing**: Distribute load across service instances
                        
                        ## Security Features
                        - **JWT Validation**: Comprehensive token validation and verification
                        - **Rate Limiting**: Configurable rate limits per endpoint and user
                        - **CORS Support**: Cross-origin resource sharing configuration
                        - **Security Headers**: Automatic security header injection
                        
                        ## Routing Configuration
                        - **Auth Routes**: `/api/auth/*` → Auth Service (port 8081)
                        - **Document Routes**: `/api/documents/*` → Document Service (port 8082)
                        - **Embedding Routes**: `/api/embeddings/*` → Embedding Service (port 8083)
                        - **RAG Routes**: `/api/rag/*` → Core Service (port 8084)
                        - **Admin Routes**: `/api/admin/*` → Admin Service (port 8085)
                        
                        ## Performance Features
                        - **Circuit Breakers**: Automatic failure detection and recovery
                        - **Timeout Management**: Configurable request timeouts
                        - **Retry Logic**: Intelligent retry mechanisms for failed requests
                        - **Connection Pooling**: Optimized backend connections
                        
                        ## Architecture
                        - **Spring Cloud Gateway**: Reactive gateway framework
                        - **WebFlux**: Non-blocking reactive web stack
                        - **Netty**: High-performance network layer
                        - **Redis**: Session and cache management
                        
                        ## Monitoring & Observability
                        - **Health Checks**: Gateway and downstream service health
                        - **Metrics Collection**: Request metrics and performance data
                        - **Distributed Tracing**: Request tracing across services
                        - **Logging**: Comprehensive request and error logging
                        
                        ## Service Discovery
                        - **Dynamic Routing**: Service discovery and routing updates
                        - **Health-Based Routing**: Route only to healthy instances
                        - **Graceful Degradation**: Handle service unavailability
                        - **A/B Testing**: Support for gradual feature rollouts
                        
                        ## Rate Limiting
                        - **Per-User Limits**: Individual user rate limiting
                        - **Per-Endpoint Limits**: Endpoint-specific rate controls
                        - **Burst Handling**: Configurable burst capacity
                        - **Hierarchical Limits**: Tenant and system-level limits
                        
                        ## Getting Started
                        1. All requests should be sent to the Gateway at port 8080
                        2. Authenticate via `/api/auth/login` to receive JWT tokens
                        3. Include JWT tokens in Authorization headers for protected endpoints
                        4. Use appropriate route prefixes for different services
                        
                        ## Available Services
                        - **Authentication Service**: User and tenant authentication
                        - **Document Service**: Document upload and processing
                        - **Embedding Service**: Vector embedding generation and search
                        - **RAG Core Service**: Intelligent question answering
                        - **Admin Service**: System administration and analytics
                        
                        ## Support
                        - **Gateway Health**: http://localhost:8080/actuator/health
                        - **Service Status**: Individual service health via gateway routing
                        - **Environment**: Primary access point at port 8080
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("RAG Development Team")
                        .email("dev-team@enterprise-rag.com")
                        .url("https://github.com/your-org/enterprise-rag"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }

    /**
     * Creates server configuration for different environments.
     * 
     * @return List of configured servers
     */
    private List<Server> createServers() {
        return List.of(
                new Server()
                        .url("http://localhost:8080")
                        .description("Local Development Gateway"),
                new Server()
                        .url("https://api.yourcompany.com")
                        .description("Production Gateway")
        );
    }

    /**
     * Creates JWT Bearer token security scheme configuration.
     * 
     * @return SecurityScheme for JWT authentication
     */
    private SecurityScheme createBearerSecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("""
                        **JWT Bearer Token Authentication (via Gateway)**
                        
                        The API Gateway validates all JWT tokens before routing requests
                        to downstream services.
                        
                        **Authentication Flow:**
                        1. Obtain JWT token via `/api/auth/login`
                        2. Include token in Authorization header for all requests
                        3. Gateway validates token and routes to appropriate service
                        
                        **Gateway Security Features:**
                        - **Token Validation**: Comprehensive JWT verification
                        - **Expiration Checking**: Automatic token expiration handling
                        - **Rate Limiting**: Per-user and per-endpoint limits
                        - **CORS Protection**: Cross-origin request security
                        
                        **Routing Benefits:**
                        - **Single Endpoint**: All services accessible via gateway
                        - **Consistent Security**: Uniform security enforcement
                        - **Load Balancing**: Automatic load distribution
                        - **Service Discovery**: Dynamic service routing
                        
                        **Error Handling:**
                        - **401 Unauthorized**: Invalid or expired tokens
                        - **403 Forbidden**: Insufficient permissions
                        - **429 Too Many Requests**: Rate limit exceeded
                        - **503 Service Unavailable**: Downstream service issues
                        
                        **Example:**
                        ```
                        Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
                        ```
                        """);
    }
}
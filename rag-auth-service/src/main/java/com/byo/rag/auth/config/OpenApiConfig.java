package com.byo.rag.auth.config;

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
 * OpenAPI 3.0 configuration for the RAG Authentication Service.
 * 
 * <p>This configuration provides comprehensive API documentation for all authentication
 * endpoints including user management, JWT operations, and tenant management.
 * 
 * <p><strong>Features:</strong>
 * <ul>
 *   <li>Complete endpoint documentation with examples</li>
 *   <li>JWT Bearer token authentication configuration</li>
 *   <li>Request/response schema definitions</li>
 *   <li>Interactive Swagger UI at /swagger-ui.html</li>
 * </ul>
 * 
 * @author RAG Development Team
 * @version 1.0.0
 * @since 2025-09-24
 */
@Configuration
public class OpenApiConfig {

    /**
     * Main OpenAPI configuration for the Authentication Service.
     * 
     * @return configured OpenAPI instance with complete metadata
     */
    @Bean
    public OpenAPI authServiceOpenAPI() {
        return new OpenAPI()
                .info(createApiInfo())
                .servers(createServers())
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("Bearer Authentication", createBearerSecurityScheme()));
    }

    /**
     * Grouped API configuration for public endpoints.
     * 
     * @return GroupedOpenApi for authentication endpoints
     */
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("auth-public")
                .displayName("Public Authentication API")
                .pathsToMatch("/auth/**")
                .build();
    }

    /**
     * Grouped API configuration for user management endpoints.
     * 
     * @return GroupedOpenApi for user management endpoints
     */
    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("user-management")
                .displayName("User Management API")
                .pathsToMatch("/users/**")
                .build();
    }

    /**
     * Grouped API configuration for tenant management endpoints.
     * 
     * @return GroupedOpenApi for tenant management endpoints
     */
    @Bean
    public GroupedOpenApi tenantApi() {
        return GroupedOpenApi.builder()
                .group("tenant-management")
                .displayName("Tenant Management API")
                .pathsToMatch("/tenants/**")
                .build();
    }

    /**
     * Creates comprehensive API information metadata.
     * 
     * @return Info object with complete API details
     */
    private Info createApiInfo() {
        return new Info()
                .title("RAG Authentication Service API")
                .description("""
                        **Enterprise RAG System Authentication Service**
                        
                        This service provides comprehensive authentication and authorization capabilities for the 
                        RAG (Retrieval Augmented Generation) system including:
                        
                        ## Core Features
                        - **JWT Authentication**: Secure token-based authentication
                        - **Multi-Tenant Support**: Complete tenant isolation and management
                        - **User Management**: Registration, verification, and profile management
                        - **Role-Based Access Control**: Granular permission management
                        
                        ## Security Features
                        - **BCrypt Password Hashing**: Industry-standard password security
                        - **JWT Token Refresh**: Seamless session management
                        - **Email Verification**: Secure account activation
                        - **Rate Limiting**: Protection against brute force attacks
                        
                        ## Architecture
                        - **Spring Boot 3.2.8**: Modern framework with reactive capabilities
                        - **PostgreSQL**: Robust multi-tenant data storage
                        - **Spring Security**: Comprehensive security framework
                        - **Database Migrations**: Flyway-managed schema evolution
                        
                        ## Getting Started
                        1. Register a new tenant organization
                        2. Create user accounts within your tenant
                        3. Authenticate to receive JWT tokens
                        4. Use tokens for accessing protected resources
                        
                        ## Support
                        - **Service Status**: http://localhost:8081/actuator/health
                        - **Documentation**: Complete endpoint documentation below
                        - **Environment**: Docker-based deployment at port 8081
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
                        .url("http://localhost:8081")
                        .description("Local Development Server"),
                new Server()
                        .url("http://localhost:8080/api/auth")
                        .description("API Gateway (Recommended)"),
                new Server()
                        .url("https://api.yourcompany.com/auth")
                        .description("Production Server")
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
                        **JWT Bearer Token Authentication**
                        
                        To authenticate with the API:
                        1. Login via `/auth/login` to receive a JWT token
                        2. Include the token in the Authorization header
                        3. Format: `Authorization: Bearer <your-jwt-token>`
                        
                        **Token Properties:**
                        - **Expiration**: 1 hour (configurable)
                        - **Refresh**: Use `/auth/refresh` with refresh token
                        - **Scope**: Tenant-specific access control
                        
                        **Example:**
                        ```
                        Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
                        ```
                        """);
    }
}
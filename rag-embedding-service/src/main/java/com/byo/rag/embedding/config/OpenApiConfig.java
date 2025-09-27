package com.byo.rag.embedding.config;

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
 * OpenAPI 3.0 configuration for the RAG Embedding Service.
 * 
 * <p>This configuration provides comprehensive API documentation for vector
 * embedding generation, similarity search, and vector storage operations.
 * 
 * <p><strong>Features:</strong>
 * <ul>
 *   <li>Vector embedding generation with multiple models</li>
 *   <li>High-performance similarity search</li>
 *   <li>Redis-based vector storage with TTL caching</li>
 *   <li>Multi-tenant vector isolation</li>
 * </ul>
 * 
 * @author RAG Development Team
 * @version 1.0.0
 * @since 2025-09-24
 */
@Configuration
public class OpenApiConfig {

    /**
     * Main OpenAPI configuration for the Embedding Service.
     * 
     * @return configured OpenAPI instance with complete metadata
     */
    @Bean
    public OpenAPI embeddingServiceOpenAPI() {
        return new OpenAPI()
                .info(createApiInfo())
                .servers(createServers())
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("Bearer Authentication", createBearerSecurityScheme()));
    }

    /**
     * Grouped API configuration for embedding generation endpoints.
     * 
     * @return GroupedOpenApi for embedding operations
     */
    @Bean
    public GroupedOpenApi embeddingApi() {
        return GroupedOpenApi.builder()
                .group("embedding-generation")
                .displayName("Embedding Generation API")
                .pathsToMatch("/embeddings/**")
                .build();
    }

    /**
     * Grouped API configuration for search endpoints.
     * 
     * @return GroupedOpenApi for search operations
     */
    @Bean
    public GroupedOpenApi searchApi() {
        return GroupedOpenApi.builder()
                .group("vector-search")
                .displayName("Vector Search API")
                .pathsToMatch("/search/**")
                .build();
    }

    /**
     * Creates comprehensive API information metadata.
     * 
     * @return Info object with complete API details
     */
    private Info createApiInfo() {
        return new Info()
                .title("RAG Embedding Service API")
                .description("""
                        **Enterprise RAG System Vector Embedding Service**
                        
                        This service provides high-performance vector embedding generation and similarity 
                        search capabilities for the RAG system.
                        
                        ## Core Features
                        - **Multi-Model Support**: OpenAI, Ollama, and custom embedding models
                        - **Vector Storage**: Redis-based high-performance vector database
                        - **Similarity Search**: Cosine similarity with configurable thresholds
                        - **Batch Processing**: Efficient bulk embedding generation
                        
                        ## Vector Operations
                        - **Embedding Generation**: Convert text to high-dimensional vectors
                        - **Similarity Search**: Find semantically similar documents
                        - **Vector Caching**: TTL-based caching for performance optimization
                        - **Model Versioning**: Support for multiple embedding models
                        
                        ## Performance Features
                        - **Redis Integration**: Sub-millisecond vector operations
                        - **Content Hashing**: SHA-256 deduplication for efficiency
                        - **TTL Management**: Automatic cache expiration
                        - **Tenant Isolation**: Complete multi-tenant vector separation
                        
                        ## Architecture
                        - **Spring Boot 3.2.8**: Modern framework with async processing
                        - **Redis Stack**: Vector database with HNSW indexing
                        - **Spring AI**: Integration with multiple AI providers
                        - **Apache Kafka**: Event-driven embedding processing
                        
                        ## Supported Models
                        - **OpenAI**: text-embedding-ada-002, text-embedding-3-small/large
                        - **Ollama**: Local embedding models (nomic-embed-text)
                        - **Custom Models**: Extensible model integration
                        
                        ## Search Capabilities
                        - **Semantic Search**: Find conceptually similar content
                        - **Hybrid Search**: Combine vector and keyword search
                        - **Filtered Search**: Apply metadata filters to results
                        - **Ranked Results**: Relevance scoring and result ranking
                        
                        ## Getting Started
                        1. Authenticate with JWT token from Auth Service
                        2. Generate embeddings for your text content
                        3. Perform similarity searches to find relevant documents
                        4. Use batch operations for high-volume processing
                        
                        ## Support
                        - **Service Status**: http://localhost:8083/actuator/health
                        - **Vector Storage**: Redis Stack with RediSearch
                        - **Environment**: Docker deployment at port 8083
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
                        .url("http://localhost:8083")
                        .description("Local Development Server"),
                new Server()
                        .url("http://localhost:8080/api/embeddings")
                        .description("API Gateway (Recommended)"),
                new Server()
                        .url("https://api.yourcompany.com/embeddings")
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
                        
                        Vector operations require authentication through JWT tokens
                        obtained from the Auth Service.
                        
                        **Required Permissions:**
                        - **EMBEDDING_READ**: View and search embeddings
                        - **EMBEDDING_WRITE**: Generate new embeddings
                        - **VECTOR_SEARCH**: Perform similarity searches
                        
                        **Multi-Tenant Access:**
                        - Vector data isolated by tenant
                        - Search results scoped to user's tenant
                        - Cache isolation maintained per tenant
                        
                        **Performance Considerations:**
                        - Embedding generation is compute-intensive
                        - Use batch operations for multiple texts
                        - Cache results are shared within tenant
                        
                        **Example:**
                        ```
                        Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
                        ```
                        """);
    }
}
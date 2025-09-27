package com.byo.rag.core.config;

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
 * OpenAPI 3.0 configuration for the RAG Core Service.
 * 
 * <p>This configuration provides comprehensive API documentation for the core
 * RAG (Retrieval Augmented Generation) operations including query processing,
 * context assembly, and LLM integration.
 * 
 * <p><strong>Features:</strong>
 * <ul>
 *   <li>Intelligent question answering with RAG pipeline</li>
 *   <li>Context assembly from multiple sources</li>
 *   <li>LLM integration with streaming responses</li>
 *   <li>Conversation management and history</li>
 * </ul>
 * 
 * @author RAG Development Team
 * @version 1.0.0
 * @since 2025-09-24
 */
@Configuration
public class OpenApiConfig {

    /**
     * Main OpenAPI configuration for the Core Service.
     * 
     * @return configured OpenAPI instance with complete metadata
     */
    @Bean
    public OpenAPI coreServiceOpenAPI() {
        return new OpenAPI()
                .info(createApiInfo())
                .servers(createServers())
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("Bearer Authentication", createBearerSecurityScheme()));
    }

    /**
     * Grouped API configuration for RAG query endpoints.
     * 
     * @return GroupedOpenApi for RAG operations
     */
    @Bean
    public GroupedOpenApi ragApi() {
        return GroupedOpenApi.builder()
                .group("rag-queries")
                .displayName("RAG Query API")
                .pathsToMatch("/rag/**")
                .build();
    }

    /**
     * Grouped API configuration for conversation endpoints.
     * 
     * @return GroupedOpenApi for conversation management
     */
    @Bean
    public GroupedOpenApi conversationApi() {
        return GroupedOpenApi.builder()
                .group("conversations")
                .displayName("Conversation Management API")
                .pathsToMatch("/conversations/**")
                .build();
    }

    /**
     * Creates comprehensive API information metadata.
     * 
     * @return Info object with complete API details
     */
    private Info createApiInfo() {
        return new Info()
                .title("RAG Core Service API")
                .description("""
                        **Enterprise RAG System Core Processing Service**
                        
                        This service provides the core RAG (Retrieval Augmented Generation) capabilities,
                        combining document retrieval with large language model generation for intelligent
                        question answering.
                        
                        ## Core Features
                        - **RAG Pipeline**: Complete retrieval-augmented generation workflow
                        - **Context Assembly**: Intelligent context construction from multiple sources
                        - **LLM Integration**: Support for OpenAI GPT and local Ollama models
                        - **Streaming Responses**: Real-time answer generation with progressive display
                        
                        ## RAG Process Flow
                        1. **Query Processing**: Parse and optimize user questions
                        2. **Document Retrieval**: Find relevant documents using vector search
                        3. **Context Assembly**: Combine retrieved content into coherent context
                        4. **LLM Generation**: Generate answers using language models
                        5. **Response Streaming**: Deliver answers in real-time chunks
                        
                        ## Conversation Management
                        - **Session Tracking**: Maintain conversation context across queries
                        - **History Management**: Store and retrieve conversation history
                        - **Context Continuity**: Preserve context across multiple questions
                        - **Multi-turn Conversations**: Support for follow-up questions
                        
                        ## Advanced Features
                        - **Query Optimization**: Enhance queries for better retrieval
                        - **Result Ranking**: Score and rank generated responses
                        - **Source Attribution**: Track sources used in answers
                        - **Confidence Scoring**: Assess answer reliability
                        
                        ## Architecture
                        - **Spring Boot 3.2.8**: Modern framework with reactive capabilities
                        - **Spring AI**: Integration with multiple AI providers
                        - **Vector Search Integration**: Connects to Embedding Service
                        - **Caching Layer**: Redis-based performance optimization
                        
                        ## Supported Models
                        - **OpenAI**: GPT-4, GPT-3.5-turbo with streaming
                        - **Ollama**: Local LLM models (Llama2, CodeLlama, etc.)
                        - **Custom Models**: Extensible model integration framework
                        
                        ## Performance Features
                        - **Streaming**: Real-time response generation
                        - **Caching**: Intelligent response caching
                        - **Load Balancing**: Multiple model endpoint support
                        - **Timeout Management**: Configurable response timeouts
                        
                        ## Getting Started
                        1. Authenticate with JWT token from Auth Service
                        2. Submit questions via the RAG query endpoints
                        3. Receive streaming responses with source attribution
                        4. Manage conversations for multi-turn interactions
                        
                        ## Support
                        - **Service Status**: http://localhost:8084/actuator/health
                        - **Model Health**: Integrated health checks for AI models
                        - **Environment**: Docker deployment at port 8084
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
                        .url("http://localhost:8084")
                        .description("Local Development Server"),
                new Server()
                        .url("http://localhost:8080/api/rag")
                        .description("API Gateway (Recommended)"),
                new Server()
                        .url("https://api.yourcompany.com/rag")
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
                        
                        RAG operations require authentication through JWT tokens
                        obtained from the Auth Service.
                        
                        **Required Permissions:**
                        - **RAG_QUERY**: Submit questions and receive answers
                        - **CONVERSATION_READ**: Access conversation history
                        - **CONVERSATION_WRITE**: Create and manage conversations
                        
                        **Multi-Tenant Access:**
                        - Queries scoped to user's tenant documents
                        - Conversation history isolated by tenant
                        - Response caching separated by tenant
                        
                        **Usage Considerations:**
                        - RAG queries consume AI model tokens
                        - Streaming responses require persistent connections
                        - Context size limited by model capabilities
                        
                        **Example:**
                        ```
                        Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
                        ```
                        """);
    }
}
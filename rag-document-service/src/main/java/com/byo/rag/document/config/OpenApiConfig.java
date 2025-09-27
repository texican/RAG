package com.byo.rag.document.config;

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
 * OpenAPI 3.0 configuration for the RAG Document Service.
 * 
 * <p>This configuration provides comprehensive API documentation for document
 * processing, file management, and text extraction operations.
 * 
 * <p><strong>Features:</strong>
 * <ul>
 *   <li>Multi-format document upload and processing</li>
 *   <li>Intelligent text chunking strategies</li>
 *   <li>Asynchronous processing with Kafka integration</li>
 *   <li>Complete multi-tenant file management</li>
 * </ul>
 * 
 * @author RAG Development Team
 * @version 1.0.0
 * @since 2025-09-24
 */
@Configuration
public class OpenApiConfig {

    /**
     * Main OpenAPI configuration for the Document Service.
     * 
     * @return configured OpenAPI instance with complete metadata
     */
    @Bean
    public OpenAPI documentServiceOpenAPI() {
        return new OpenAPI()
                .info(createApiInfo())
                .servers(createServers())
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("Bearer Authentication", createBearerSecurityScheme()));
    }

    /**
     * Grouped API configuration for document management endpoints.
     * 
     * @return GroupedOpenApi for document operations
     */
    @Bean
    public GroupedOpenApi documentApi() {
        return GroupedOpenApi.builder()
                .group("document-management")
                .displayName("Document Management API")
                .pathsToMatch("/documents/**")
                .build();
    }

    /**
     * Grouped API configuration for file operations endpoints.
     * 
     * @return GroupedOpenApi for file operations
     */
    @Bean
    public GroupedOpenApi fileApi() {
        return GroupedOpenApi.builder()
                .group("file-operations")
                .displayName("File Operations API")
                .pathsToMatch("/files/**")
                .build();
    }

    /**
     * Creates comprehensive API information metadata.
     * 
     * @return Info object with complete API details
     */
    private Info createApiInfo() {
        return new Info()
                .title("RAG Document Service API")
                .description("""
                        **Enterprise RAG System Document Processing Service**
                        
                        This service provides comprehensive document processing capabilities for the 
                        RAG system including upload, text extraction, chunking, and management.
                        
                        ## Core Features
                        - **Multi-Format Support**: PDF, DOCX, TXT, Markdown, HTML processing
                        - **Intelligent Text Extraction**: Apache Tika-powered content analysis
                        - **Smart Chunking**: Multiple strategies (fixed-size, semantic, sliding window)
                        - **Asynchronous Processing**: Kafka-based event-driven architecture
                        
                        ## Processing Pipeline
                        1. **Document Upload**: Secure multi-tenant file storage
                        2. **Text Extraction**: Content and metadata extraction
                        3. **Chunking**: Optimized text segmentation
                        4. **Event Publishing**: Kafka events for embedding generation
                        
                        ## File Management
                        - **Secure Storage**: Tenant-isolated file system
                        - **Metadata Tracking**: Complete document lifecycle
                        - **Version Control**: Document revision management
                        - **Batch Operations**: Bulk upload and processing
                        
                        ## Architecture
                        - **Spring Boot 3.2.8**: Modern framework with async processing
                        - **Apache Tika**: Multi-format text extraction
                        - **PostgreSQL**: Document metadata storage
                        - **Apache Kafka**: Event-driven processing
                        
                        ## Supported Formats
                        - **PDF**: Portable Document Format with OCR support
                        - **DOCX**: Microsoft Word documents with rich formatting
                        - **TXT**: Plain text files with encoding detection
                        - **Markdown**: GitHub-flavored markdown processing
                        - **HTML**: Web content with link extraction
                        
                        ## Getting Started
                        1. Authenticate with the Auth Service to get JWT token
                        2. Upload documents using the upload endpoints
                        3. Monitor processing status via status endpoints
                        4. Retrieve processed chunks and metadata
                        
                        ## Support
                        - **Service Status**: http://localhost:8082/actuator/health
                        - **File Storage**: Docker volume-mounted storage
                        - **Environment**: Docker deployment at port 8082
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
                        .url("http://localhost:8082")
                        .description("Local Development Server"),
                new Server()
                        .url("http://localhost:8080/api/documents")
                        .description("API Gateway (Recommended)"),
                new Server()
                        .url("https://api.yourcompany.com/documents")
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
                        
                        Document operations require authentication through JWT tokens
                        obtained from the Auth Service.
                        
                        **Required Permissions:**
                        - **DOCUMENT_READ**: View and download documents
                        - **DOCUMENT_WRITE**: Upload and modify documents
                        - **DOCUMENT_DELETE**: Remove documents (admin only)
                        
                        **Multi-Tenant Access:**
                        - Users can only access documents within their tenant
                        - Tenant isolation enforced at database and file system levels
                        
                        **Example:**
                        ```
                        Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
                        ```
                        """);
    }
}
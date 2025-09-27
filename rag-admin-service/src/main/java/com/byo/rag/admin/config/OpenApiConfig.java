package com.byo.rag.admin.config;

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
 * OpenAPI 3.0 configuration for the RAG Admin Service.
 * 
 * <p>This configuration provides comprehensive API documentation for system
 * administration, tenant management, and analytics operations.
 * 
 * <p><strong>Features:</strong>
 * <ul>
 *   <li>Complete system administration capabilities</li>
 *   <li>Multi-tenant organization management</li>
 *   <li>User administration across all tenants</li>
 *   <li>System analytics and monitoring</li>
 * </ul>
 * 
 * @author RAG Development Team
 * @version 1.0.0
 * @since 2025-09-24
 */
@Configuration
public class OpenApiConfig {

    /**
     * Main OpenAPI configuration for the Admin Service.
     * 
     * @return configured OpenAPI instance with complete metadata
     */
    @Bean
    public OpenAPI adminServiceOpenAPI() {
        return new OpenAPI()
                .info(createApiInfo())
                .servers(createServers())
                .addSecurityItem(new SecurityRequirement().addList("Admin Authentication"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("Admin Authentication", createAdminSecurityScheme()));
    }

    /**
     * Grouped API configuration for admin authentication endpoints.
     * 
     * @return GroupedOpenApi for admin auth operations
     */
    @Bean
    public GroupedOpenApi adminAuthApi() {
        return GroupedOpenApi.builder()
                .group("admin-auth")
                .displayName("Admin Authentication API")
                .pathsToMatch("/admin/api/auth/**")
                .build();
    }

    /**
     * Grouped API configuration for tenant management endpoints.
     * 
     * @return GroupedOpenApi for tenant management
     */
    @Bean
    public GroupedOpenApi tenantManagementApi() {
        return GroupedOpenApi.builder()
                .group("tenant-management")
                .displayName("Tenant Management API")
                .pathsToMatch("/admin/api/tenants/**")
                .build();
    }

    /**
     * Grouped API configuration for user administration endpoints.
     * 
     * @return GroupedOpenApi for user administration
     */
    @Bean
    public GroupedOpenApi userAdminApi() {
        return GroupedOpenApi.builder()
                .group("user-administration")
                .displayName("User Administration API")
                .pathsToMatch("/admin/api/users/**")
                .build();
    }

    /**
     * Grouped API configuration for system analytics endpoints.
     * 
     * @return GroupedOpenApi for system analytics
     */
    @Bean
    public GroupedOpenApi analyticsApi() {
        return GroupedOpenApi.builder()
                .group("system-analytics")
                .displayName("System Analytics API")
                .pathsToMatch("/admin/api/analytics/**", "/admin/api/system/**")
                .build();
    }

    /**
     * Creates comprehensive API information metadata.
     * 
     * @return Info object with complete API details
     */
    private Info createApiInfo() {
        return new Info()
                .title("RAG Admin Service API")
                .description("""
                        **Enterprise RAG System Administration Service**
                        
                        This service provides comprehensive administrative capabilities for the RAG system,
                        including tenant management, user administration, and system analytics.
                        
                        ## Core Features
                        - **Tenant Management**: Complete tenant lifecycle management
                        - **User Administration**: Cross-tenant user management
                        - **System Analytics**: Performance metrics and usage analytics
                        - **Health Monitoring**: Deep system health and diagnostics
                        
                        ## Administrative Capabilities
                        - **Tenant Operations**: Create, update, suspend, and delete tenants
                        - **User Management**: Manage users across all tenant organizations
                        - **Resource Management**: Monitor and control system resources
                        - **Audit Logging**: Complete audit trail of administrative actions
                        
                        ## Analytics & Monitoring
                        - **Usage Metrics**: Document processing, query statistics
                        - **Performance Monitoring**: Response times, throughput metrics
                        - **Resource Utilization**: Database, storage, and compute usage
                        - **Health Dashboards**: Real-time system health indicators
                        
                        ## Security Features
                        - **Admin Authentication**: Separate admin authentication system
                        - **Role-Based Access**: Granular administrative permissions
                        - **Audit Trails**: Complete logging of administrative actions
                        - **Session Management**: Secure admin session handling
                        
                        ## Architecture
                        - **Spring Boot 3.2.8**: Modern administrative framework
                        - **PostgreSQL**: Administrative data storage
                        - **Redis**: Performance metrics caching
                        - **Comprehensive Logging**: Detailed operational logging
                        
                        ## Administrative Roles
                        - **SYSTEM_ADMIN**: Full system administration capabilities
                        - **TENANT_ADMIN**: Tenant-specific administrative access
                        - **SUPPORT_ADMIN**: Read-only access for support operations
                        - **AUDIT_ADMIN**: Audit log access and compliance reporting
                        
                        ## Tenant Management
                        - **Creation**: Set up new tenant organizations
                        - **Configuration**: Resource limits and feature enablement
                        - **Monitoring**: Usage tracking and performance metrics
                        - **Lifecycle**: Suspension, reactivation, and deletion
                        
                        ## User Administration
                        - **Cross-Tenant Access**: Manage users across all tenants
                        - **Role Assignment**: Grant and revoke user permissions
                        - **Password Management**: Admin password reset capabilities
                        - **Account Status**: Activate, deactivate, and suspend accounts
                        
                        ## Getting Started
                        1. Authenticate with admin credentials via `/admin/api/auth/login`
                        2. Use admin JWT tokens for subsequent operations
                        3. Access tenant and user management endpoints
                        4. Monitor system health and analytics
                        
                        ## Support
                        - **Service Status**: http://localhost:8085/admin/api/actuator/health
                        - **Admin Dashboard**: Comprehensive administrative interface
                        - **Environment**: Docker deployment at port 8085
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
                        .url("http://localhost:8085/admin/api")
                        .description("Local Development Server"),
                new Server()
                        .url("http://localhost:8080/api/admin")
                        .description("API Gateway (Recommended)"),
                new Server()
                        .url("https://api.yourcompany.com/admin")
                        .description("Production Server")
        );
    }

    /**
     * Creates admin JWT authentication security scheme configuration.
     * 
     * @return SecurityScheme for admin authentication
     */
    private SecurityScheme createAdminSecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("""
                        **Admin JWT Authentication**
                        
                        Administrative operations require special admin authentication
                        separate from regular user authentication.
                        
                        **Authentication Process:**
                        1. Login via `/admin/api/auth/login` with admin credentials
                        2. Receive admin-specific JWT token with elevated privileges
                        3. Use token for administrative operations
                        
                        **Admin Token Properties:**
                        - **Enhanced Security**: Shorter expiration times
                        - **Elevated Privileges**: Cross-tenant access capabilities
                        - **Audit Logging**: All operations logged for compliance
                        - **Session Tracking**: Admin session monitoring
                        
                        **Required Credentials:**
                        - **Username**: admin@enterprise-rag.com (default)
                        - **Password**: admin123 (change after first login)
                        
                        **Security Considerations:**
                        - Admin tokens have system-wide access
                        - All administrative actions are audited
                        - Sessions timeout faster than user sessions
                        - IP-based access restrictions may apply
                        
                        **Example:**
                        ```
                        Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
                        ```
                        """);
    }
}
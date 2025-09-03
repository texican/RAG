package com.enterprise.rag.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

/**
 * Spring Boot application class for the Enterprise RAG Authentication Service.
 * <p>
 * <strong>✅ Production Ready & Fully Operational (2025-09-03):</strong> This microservice provides 
 * comprehensive authentication and authorization capabilities for the Enterprise RAG system. 
 * Successfully deployed in Docker at port 8081 with complete database integration and 
 * health monitoring.
 * <p>
 * <strong>🐳 Docker Status:</strong> Healthy and operational with PostgreSQL integration,
 * JWT token operations, tenant-based access control, and security policies
 * across the entire multi-tenant platform.
 * 
 * <h2>Authentication Capabilities</h2>
 * <ul>
 *   <li><strong>User Authentication</strong> - Email/password and token-based authentication</li>
 *   <li><strong>JWT Token Management</strong> - Secure token generation, validation, and refresh</li>
 *   <li><strong>Multi-Tenant Security</strong> - Tenant-aware access control and isolation</li>
 *   <li><strong>Role-Based Authorization</strong> - Fine-grained permission management</li>
 *   <li><strong>Session Management</strong> - Secure session handling and lifecycle</li>
 * </ul>
 * 
 * <h2>Security Architecture</h2>
 * <ul>
 *   <li><strong>Stateless Design</strong> - JWT-based stateless authentication</li>
 *   <li><strong>Tenant Isolation</strong> - Complete separation of tenant user data</li>
 *   <li><strong>Password Security</strong> - BCrypt hashing with configurable strength</li>
 *   <li><strong>Token Security</strong> - Configurable token expiration and refresh policies</li>
 * </ul>
 * 
 * <h2>User Management</h2>
 * <ul>
 *   <li><strong>User Registration</strong> - New user account creation with validation</li>
 *   <li><strong>Profile Management</strong> - User profile updates and preferences</li>
 *   <li><strong>Account Lifecycle</strong> - Account activation, deactivation, and deletion</li>
 *   <li><strong>Tenant Association</strong> - User-tenant relationship management</li>
 * </ul>
 * 
 * <h2>API Endpoints</h2>
 * <ul>
 *   <li><strong>/auth/login</strong> - User authentication and token generation</li>
 *   <li><strong>/auth/refresh</strong> - JWT token refresh operations</li>
 *   <li><strong>/auth/logout</strong> - Secure session termination</li>
 *   <li><strong>/auth/users/**</strong> - User management operations</li>
 *   <li><strong>/auth/tenants/**</strong> - Tenant configuration endpoints</li>
 * </ul>
 * 
 * <h2>Integration Points</h2>
 * <ul>
 *   <li><strong>All Services</strong> - Provides authentication for all microservices</li>
 *   <li><strong>API Gateway</strong> - Central authentication validation</li>
 *   <li><strong>Admin Service</strong> - Administrative user management</li>
 *   <li><strong>Document Service</strong> - Document access authorization</li>
 * </ul>
 * 
 * <h2>Configuration Features</h2>
 * <ul>
 *   <li><strong>Wide Component Scanning</strong> - Scans entire RAG package hierarchy</li>
 *   <li><strong>Shared Entity Access</strong> - Uses common entity model for consistency</li>
 *   <li><strong>Database Integration</strong> - Automatic repository discovery</li>
 *   <li><strong>Security Configuration</strong> - Comprehensive Spring Security setup</li>
 * </ul>
 * 
 * @author Enterprise RAG Development Team
 * @version 0.8.0
 * @since 0.1.0
 * @see org.springframework.boot.autoconfigure.SpringBootApplication
 * @see org.springframework.boot.autoconfigure.domain.EntityScan
 */
@SpringBootApplication(scanBasePackages = "com.enterprise.rag")
@EntityScan("com.enterprise.rag.shared.entity")
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
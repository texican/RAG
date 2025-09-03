package com.byo.rag.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot application class for the Enterprise RAG Administrative Service.
 * <p>
 * <strong>✅ Production Ready & Database-Integrated (2025-09-03):</strong> This microservice provides 
 * comprehensive administrative capabilities for the Enterprise RAG system, including tenant management, 
 * user administration, system monitoring, and operational controls. Successfully deployed in Docker 
 * at port 8085 with full PostgreSQL integration and health monitoring.
 * <p>
 * <strong>🐳 Docker Status:</strong> Healthy and operational with complete database backend,
 * serving as the central management hub for multi-tenant RAG deployments.
 * 
 * <h2>Administrative Capabilities</h2>
 * <ul>
 *   <li><strong>Tenant Management</strong> - Complete tenant lifecycle operations (CRUD)</li>
 *   <li><strong>User Administration</strong> - User account management and access control</li>
 *   <li><strong>System Monitoring</strong> - Health checks, metrics, and operational dashboards</li>
 *   <li><strong>Security Administration</strong> - JWT token management and authentication</li>
 *   <li><strong>Configuration Management</strong> - System-wide configuration and feature flags</li>
 * </ul>
 * 
 * <h2>Database Integration</h2>
 * <ul>
 *   <li><strong>Full JPA Support</strong> - Complete database persistence with PostgreSQL</li>
 *   <li><strong>Repository Pattern</strong> - Custom repositories for complex queries</li>
 *   <li><strong>Entity Management</strong> - Shared entity model from rag-shared module</li>
 *   <li><strong>Transaction Support</strong> - ACID compliance for administrative operations</li>
 * </ul>
 * 
 * <h2>Enterprise Features</h2>
 * <ul>
 *   <li><strong>Scheduled Tasks</strong> - Automated maintenance and cleanup operations</li>
 *   <li><strong>Audit Logging</strong> - Comprehensive tracking of administrative actions</li>
 *   <li><strong>Multi-Tenant Architecture</strong> - Tenant isolation and resource management</li>
 *   <li><strong>Role-Based Access</strong> - Administrative permission controls</li>
 * </ul>
 * 
 * <h2>API Endpoints</h2>
 * <ul>
 *   <li><strong>/admin/auth/**</strong> - Authentication and JWT token management</li>
 *   <li><strong>/admin/tenants/**</strong> - Tenant management operations</li>
 *   <li><strong>/admin/users/**</strong> - User administration endpoints</li>
 *   <li><strong>/admin/system/**</strong> - System health and monitoring</li>
 * </ul>
 * 
 * <h2>Configuration Details</h2>
 * <ul>
 *   <li><strong>Component Scanning</strong> - Automatic discovery of admin components</li>
 *   <li><strong>Entity Scanning</strong> - Uses shared entities for data consistency</li>
 *   <li><strong>Repository Config</strong> - Admin-specific repository implementations</li>
 *   <li><strong>Scheduling</strong> - Background task execution for maintenance</li>
 * </ul>
 * 
 * <h2>Security Configuration</h2>
 * <ul>
 *   <li><strong>JWT Authentication</strong> - Token-based admin authentication</li>
 *   <li><strong>Role Validation</strong> - ADMIN role requirement for all endpoints</li>
 *   <li><strong>Request Filtering</strong> - Security filters for all admin operations</li>
 *   <li><strong>Error Handling</strong> - Comprehensive exception management</li>
 * </ul>
 * 
 * @author Enterprise RAG Development Team
 * @version 0.8.0
 * @since 0.1.0
 * @see org.springframework.boot.autoconfigure.SpringBootApplication
 * @see org.springframework.scheduling.annotation.EnableScheduling
 * @see org.springframework.data.jpa.repository.config.EnableJpaRepositories
 */
@SpringBootApplication
@EntityScan(basePackages = {"com.byo.rag.shared.entity"})
@EnableJpaRepositories(basePackages = {"com.byo.rag.admin.repository"})
@EnableScheduling
public class AdminServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(AdminServiceApplication.class, args);
    }
}
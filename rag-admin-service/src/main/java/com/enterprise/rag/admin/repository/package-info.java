/**
 * Data access repositories for administrative operations.
 * 
 * <p>This package contains Spring Data JPA repositories that provide
 * comprehensive data access operations for administrative functions.
 * All repositories are optimized for administrative workloads and include
 * advanced querying capabilities for tenant management, user administration,
 * and system analytics.</p>
 * 
 * <h2>Repository Architecture</h2>
 * <p>Administrative repositories extend shared base repositories with specialized functionality:</p>
 * <ul>
 *   <li><strong>Tenant Repository</strong> - Complete tenant lifecycle and analytics queries</li>
 *   <li><strong>User Repository</strong> - Cross-tenant user management and reporting</li>
 *   <li><strong>Admin Audit Repository</strong> - Administrative action logging and compliance</li>
 *   <li><strong>System Metrics Repository</strong> - System monitoring and performance data</li>
 *   <li><strong>Configuration Repository</strong> - System and tenant configuration management</li>
 * </ul>
 * 
 * <h2>Advanced Query Capabilities</h2>
 * <p>Repositories include sophisticated querying for administrative needs:</p>
 * <ul>
 *   <li><strong>Dynamic Search</strong> - JPA Criteria API for flexible search functionality</li>
 *   <li><strong>Aggregation Queries</strong> - Statistical aggregations for analytics</li>
 *   <li><strong>Time-Series Queries</strong> - Time-based data analysis and reporting</li>
 *   <li><strong>Cross-Entity Joins</strong> - Complex joins for comprehensive reporting</li>
 *   <li><strong>Performance-Optimized Queries</strong> - Optimized queries for large datasets</li>
 * </ul>
 * 
 * <h2>Tenant Repository Features</h2>
 * <p>Comprehensive tenant data access operations:</p>
 * <ul>
 *   <li><strong>Tenant CRUD Operations</strong> - Complete tenant lifecycle management</li>
 *   <li><strong>Tenant Search and Filtering</strong> - Advanced search with multiple criteria</li>
 *   <li><strong>Tenant Analytics</strong> - Usage statistics and billing information</li>
 *   <li><strong>Tenant Status Queries</strong> - Status-based filtering and reporting</li>
 *   <li><strong>Tenant Resource Queries</strong> - Resource utilization and capacity queries</li>
 * </ul>
 * 
 * <h2>User Repository Features</h2>
 * <p>Cross-tenant user management capabilities:</p>
 * <ul>
 *   <li><strong>User Search</strong> - Advanced user search across all tenants</li>
 *   <li><strong>User Analytics</strong> - User activity and engagement metrics</li>
 *   <li><strong>Role Management Queries</strong> - User role and permission queries</li>
 *   <li><strong>User Status Tracking</strong> - Account status and lifecycle queries</li>
 *   <li><strong>Compliance Queries</strong> - GDPR and privacy regulation compliance</li>
 * </ul>
 * 
 * <h2>Analytics and Reporting Queries</h2>
 * <p>Specialized queries for administrative analytics:</p>
 * <ul>
 *   <li><strong>Usage Statistics</strong> - System and tenant usage analytics</li>
 *   <li><strong>Performance Metrics</strong> - System performance and optimization data</li>
 *   <li><strong>Financial Reporting</strong> - Billing and cost allocation queries</li>
 *   <li><strong>Compliance Reporting</strong> - Audit trails and regulatory compliance</li>
 *   <li><strong>Trend Analysis</strong> - Historical data analysis and forecasting</li>
 * </ul>
 * 
 * <h2>Performance Optimizations</h2>
 * <p>Repositories are optimized for administrative workloads:</p>
 * <ul>
 *   <li><strong>Strategic Indexing</strong> - Database indexes optimized for admin queries</li>
 *   <li><strong>Query Optimization</strong> - JPQL and native SQL optimization</li>
 *   <li><strong>Entity Graphs</strong> - Efficient relationship loading strategies</li>
 *   <li><strong>Read Replicas</strong> - Support for read replica database routing</li>
 *   <li><strong>Batch Processing</strong> - Efficient bulk operations for administrative tasks</li>
 * </ul>
 * 
 * <h2>Transaction Management</h2>
 * <p>Comprehensive transaction handling for administrative operations:</p>
 * <ul>
 *   <li><strong>ACID Compliance</strong> - Full ACID compliance for critical operations</li>
 *   <li><strong>Isolation Levels</strong> - Appropriate isolation for concurrent access</li>
 *   <li><strong>Rollback Strategies</strong> - Proper rollback handling for complex operations</li>
 *   <li><strong>Distributed Transactions</strong> - Support for distributed transaction coordination</li>
 * </ul>
 * 
 * <h2>Audit and Compliance</h2>
 * <p>Comprehensive audit and compliance support:</p>
 * <ul>
 *   <li><strong>Change Tracking</strong> - Automatic tracking of all administrative changes</li>
 *   <li><strong>Audit Trails</strong> - Complete audit trails for regulatory compliance</li>
 *   <li><strong>Data Retention</strong> - Automated data retention and purging policies</li>
 *   <li><strong>Compliance Queries</strong> - Specialized queries for compliance reporting</li>
 * </ul>
 * 
 * <h2>Custom Query Methods</h2>
 * <p>Specialized administrative query methods:</p>
 * <ul>
 *   <li><strong>Dashboard Queries</strong> - Optimized queries for administrative dashboards</li>
 *   <li><strong>Report Generation</strong> - Complex queries for report generation</li>
 *   <li><strong>Health Check Queries</strong> - System health and monitoring queries</li>
 *   <li><strong>Capacity Planning</strong> - Resource utilization and planning queries</li>
 * </ul>
 * 
 * <h2>Integration Features</h2>
 * <p>Support for integration with external systems:</p>
 * <ul>
 *   <li><strong>Data Export</strong> - Efficient data export for external systems</li>
 *   <li><strong>Bulk Import</strong> - High-performance bulk data import</li>
 *   <li><strong>Synchronization</strong> - Data synchronization with external systems</li>
 *   <li><strong>Migration Support</strong> - Database migration and upgrade support</li>
 * </ul>
 * 
 * <h2>Monitoring and Metrics</h2>
 * <p>Built-in monitoring for repository operations:</p>
 * <ul>
 *   <li><strong>Query Performance</strong> - Automatic query performance monitoring</li>
 *   <li><strong>Slow Query Detection</strong> - Detection and alerting for slow queries</li>
 *   <li><strong>Connection Pool Monitoring</strong> - Database connection usage tracking</li>
 *   <li><strong>Cache Hit Rates</strong> - Repository-level caching effectiveness</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Repository
 * @Transactional(readOnly = true)
 * public interface TenantRepository extends JpaRepository<Tenant, String>, JpaSpecificationExecutor<Tenant> {
 *     
 *     // Basic tenant queries
 *     @Query("SELECT t FROM Tenant t WHERE t.status = :status ORDER BY t.createdAt DESC")
 *     List<Tenant> findByStatus(@Param("status") TenantStatus status);
 *     
 *     // Analytics queries
 *     @Query("SELECT t.plan, COUNT(t) FROM Tenant t GROUP BY t.plan")
 *     List<Object[]> getTenantCountByPlan();
 *     
 *     // Usage statistics
 *     @Query("""
 *         SELECT t.id, t.name, COUNT(u.id) as userCount, 
 *                COUNT(d.id) as documentCount, t.createdAt
 *         FROM Tenant t
 *         LEFT JOIN User u ON u.tenantId = t.id AND u.status = 'ACTIVE'
 *         LEFT JOIN Document d ON d.tenantId = t.id
 *         WHERE t.status = 'ACTIVE'
 *         GROUP BY t.id, t.name, t.createdAt
 *         ORDER BY userCount DESC
 *     """)
 *     List<TenantUsageStats> getTenantUsageStatistics();
 *     
 *     // Time-based queries for analytics
 *     @Query("""
 *         SELECT DATE(t.createdAt) as date, COUNT(t.id) as count
 *         FROM Tenant t
 *         WHERE t.createdAt >= :startDate AND t.createdAt <= :endDate
 *         GROUP BY DATE(t.createdAt)
 *         ORDER BY date
 *     """)
 *     List<Object[]> getTenantCreationTrend(
 *         @Param("startDate") LocalDateTime startDate,
 *         @Param("endDate") LocalDateTime endDate
 *     );
 *     
 *     // Complex search with custom specification
 *     default Page<Tenant> findTenants(TenantSearchCriteria criteria, Pageable pageable) {
 *         return findAll(TenantSpecifications.withCriteria(criteria), pageable);
 *     }
 * }
 * }</pre>
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 * @see org.springframework.data.jpa.repository Spring Data JPA
 * @see org.springframework.data.jpa.repository.JpaSpecificationExecutor Dynamic queries
 * @see com.enterprise.rag.shared.entity Shared entities
 * @see javax.persistence.criteria JPA Criteria API
 */
package com.enterprise.rag.admin.repository;
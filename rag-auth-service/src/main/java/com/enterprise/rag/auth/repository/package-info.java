/**
 * Data access repositories for authentication and user management.
 * 
 * <p>This package contains Spring Data JPA repositories that provide
 * data access operations for authentication-related entities. All
 * repositories implement multi-tenant data isolation and include
 * optimized queries for high-performance authentication operations.</p>
 * 
 * <h2>Repository Architecture</h2>
 * <p>Repositories extend the shared base repository with authentication-specific functionality:</p>
 * <ul>
 *   <li><strong>User Repository</strong> - User account management with tenant isolation</li>
 *   <li><strong>Tenant Repository</strong> - Multi-tenant organization management</li>
 *   <li><strong>Role Repository</strong> - Role and permission management</li>
 *   <li><strong>Session Repository</strong> - User session tracking and management</li>
 *   <li><strong>Audit Repository</strong> - Security event logging and audit trails</li>
 * </ul>
 * 
 * <h2>Multi-Tenant Data Access</h2>
 * <p>All repositories enforce strict tenant isolation:</p>
 * <ul>
 *   <li><strong>Automatic Tenant Filtering</strong> - All queries automatically include tenant ID filtering</li>
 *   <li><strong>Cross-Tenant Prevention</strong> - Impossible to access data across tenant boundaries</li>
 *   <li><strong>Tenant Validation</strong> - Automatic tenant existence and status validation</li>
 *   <li><strong>Tenant Analytics</strong> - Built-in tenant usage metrics and analytics queries</li>
 * </ul>
 * 
 * <h2>User Repository Features</h2>
 * <p>Comprehensive user data access operations:</p>
 * <ul>
 *   <li><strong>Authentication Queries</strong> - Optimized queries for login operations</li>
 *   <li><strong>User Lookup</strong> - Find users by email, username, or external ID</li>
 *   <li><strong>Status Management</strong> - Queries for user activation, suspension, and deletion</li>
 *   <li><strong>Role Queries</strong> - User role and permission lookup operations</li>
 *   <li><strong>Profile Management</strong> - User profile update and retrieval operations</li>
 * </ul>
 * 
 * <h2>Security-Optimized Queries</h2>
 * <p>Repositories include specialized queries for security operations:</p>
 * <ul>
 *   <li><strong>Login Attempt Tracking</strong> - Failed login attempt counting and lockout queries</li>
 *   <li><strong>Session Queries</strong> - Active session lookup and concurrent session management</li>
 *   <li><strong>Token Blacklisting</strong> - JWT token revocation and blacklist management</li>
 *   <li><strong>Audit Queries</strong> - Security event retrieval and compliance reporting</li>
 *   <li><strong>Password History</strong> - Password reuse prevention queries</li>
 * </ul>
 * 
 * <h2>Performance Optimizations</h2>
 * <p>Repositories are optimized for authentication performance:</p>
 * <ul>
 *   <li><strong>Index Strategy</strong> - Strategic database indexing for authentication queries</li>
 *   <li><strong>Query Optimization</strong> - JPQL and native SQL optimization for common operations</li>
 *   <li><strong>Entity Graphs</strong> - Efficient relationship loading for complex queries</li>
 *   <li><strong>Caching Integration</strong> - Redis caching for frequently accessed user data</li>
 *   <li><strong>Read Replicas</strong> - Support for read replica database routing</li>
 * </ul>
 * 
 * <h2>Transaction Management</h2>
 * <p>Proper transaction handling for authentication operations:</p>
 * <ul>
 *   <li><strong>Read-Only Queries</strong> - Optimization for read-only authentication queries</li>
 *   <li><strong>Transaction Isolation</strong> - Appropriate isolation levels for concurrent access</li>
 *   <li><strong>Rollback Handling</strong> - Proper rollback for authentication failures</li>
 *   <li><strong>Batch Operations</strong> - Efficient bulk operations for user management</li>
 * </ul>
 * 
 * <h2>Audit and Compliance</h2>
 * <p>Comprehensive audit trail and compliance support:</p>
 * <ul>
 *   <li><strong>Change Tracking</strong> - Automatic tracking of all user data changes</li>
 *   <li><strong>Login Auditing</strong> - Complete audit trail of authentication events</li>
 *   <li><strong>Compliance Queries</strong> - Specialized queries for regulatory compliance</li>
 *   <li><strong>Data Retention</strong> - Automatic data purging based on retention policies</li>
 * </ul>
 * 
 * <h2>Custom Query Methods</h2>
 * <p>Specialized finder methods for authentication use cases:</p>
 * <ul>
 *   <li><strong>Active User Queries</strong> - Find active users with various criteria</li>
 *   <li><strong>Role-Based Queries</strong> - Find users by roles and permissions</li>
 *   <li><strong>Date Range Queries</strong> - User activity and login date range queries</li>
 *   <li><strong>Status Queries</strong> - Users by account status and lifecycle stage</li>
 * </ul>
 * 
 * <h2>Integration Features</h2>
 * <p>Support for external system integration:</p>
 * <ul>
 *   <li><strong>External ID Mapping</strong> - Queries for SSO and external system integration</li>
 *   <li><strong>Bulk Import</strong> - Efficient bulk user import operations</li>
 *   <li><strong>Data Export</strong> - User data export for migration and backup</li>
 *   <li><strong>Synchronization</strong> - Support for external directory synchronization</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Repository
 * @Transactional(readOnly = true)
 * public interface UserRepository extends BaseRepository<User, String> {
 *     
 *     // Authentication queries
 *     @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.email = :email AND u.status = 'ACTIVE'")
 *     Optional<User> findActiveUserByTenantAndEmail(
 *         @Param("tenantId") String tenantId, 
 *         @Param("email") String email
 *     );
 *     
 *     // Failed login tracking
 *     @Modifying
 *     @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1, u.lastFailedLoginAt = :timestamp WHERE u.id = :userId")
 *     void incrementFailedLoginAttempts(
 *         @Param("userId") String userId, 
 *         @Param("timestamp") LocalDateTime timestamp
 *     );
 *     
 *     // Session management
 *     @Query("SELECT COUNT(s) FROM UserSession s WHERE s.userId = :userId AND s.status = 'ACTIVE' AND s.expiresAt > :currentTime")
 *     long countActiveSessions(
 *         @Param("userId") String userId, 
 *         @Param("currentTime") LocalDateTime currentTime
 *     );
 *     
 *     // Role-based queries
 *     @Query("SELECT u FROM User u JOIN u.roles r WHERE u.tenantId = :tenantId AND r.name IN :roleNames")
 *     @EntityGraph(attributePaths = {"roles", "tenant"})
 *     List<User> findUsersByTenantAndRoles(
 *         @Param("tenantId") String tenantId, 
 *         @Param("roleNames") Set<String> roleNames
 *     );
 *     
 *     // Compliance queries
 *     @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.lastLoginAt < :cutoffDate")
 *     List<User> findInactiveUsers(
 *         @Param("tenantId") String tenantId, 
 *         @Param("cutoffDate") LocalDateTime cutoffDate
 *     );
 * }
 * }</pre>
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 * @see org.springframework.data.jpa.repository Spring Data JPA
 * @see com.enterprise.rag.shared.repository Base repository interfaces
 * @see com.enterprise.rag.shared.entity User and tenant entities
 * @see org.springframework.transaction.annotation.Transactional Transaction management
 */
package com.enterprise.rag.auth.repository;
package com.byo.rag.admin.repository;

import com.byo.rag.shared.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Enterprise-grade repository interface for comprehensive user data access and multi-tenant user management.
 * 
 * <p>This repository provides extensive user management capabilities across the multi-tenant Enterprise RAG
 * system, including CRUD operations, advanced search functionality, tenant-scoped operations, analytics queries,
 * and administrative reporting features. All operations maintain strict tenant isolation and data integrity.</p>
 * 
 * <p><strong>Core Capabilities:</strong></p>
 * <ul>
 *   <li><strong>Multi-Tenant Operations:</strong> Tenant-scoped user queries with strict isolation</li>
 *   <li><strong>Authentication Support:</strong> Email-based user lookup and credential validation</li>
 *   <li><strong>Advanced Search:</strong> Full-text search across names and email addresses</li>
 *   <li><strong>Role Management:</strong> Role-based filtering and administrative privilege queries</li>
 *   <li><strong>Status Tracking:</strong> User lifecycle and verification status management</li>
 *   <li><strong>Analytics Queries:</strong> Comprehensive user statistics and reporting</li>
 * </ul>
 * 
 * <p><strong>Multi-Tenant Architecture:</strong></p>
 * <ul>
 *   <li>Complete user isolation between tenant organizations</li>
 *   <li>Tenant administrator identification and protection</li>
 *   <li>Cross-tenant administrative operations with proper authorization</li>
 *   <li>Tenant-specific user analytics and capacity monitoring</li>
 * </ul>
 * 
 * <p><strong>Security Features:</strong></p>
 * <ul>
 *   <li>Email uniqueness validation across the entire system</li>
 *   <li>Role-based access control query support</li>
 *   <li>Email verification token management</li>
 *   <li>Inactive user identification for security audits</li>
 *   <li>Administrative user protection mechanisms</li>
 * </ul>
 * 
 * <p><strong>Administrative Analytics:</strong></p>
 * <ul>
 *   <li>System-wide user statistics and role distribution</li>
 *   <li>Tenant-specific user activity and status summaries</li>
 *   <li>User onboarding and verification tracking</li>
 *   <li>Activity-based user engagement analysis</li>
 * </ul>
 * 
 * <p><strong>Performance Optimization:</strong></p>
 * <p>All queries are optimized for enterprise-scale operations with proper indexing considerations.
 * Custom JPQL queries include parameterization to prevent SQL injection and maintain optimal
 * performance under high concurrent load across multiple tenant boundaries.</p>
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 * @see com.byo.rag.shared.entity.User
 * @see com.byo.rag.admin.service.UserService
 * @see com.byo.rag.shared.entity.Tenant
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by email (unique across system)
     */
    Optional<User> findByEmail(String email);

    /**
     * Find users by tenant ID
     */
    List<User> findByTenantId(UUID tenantId);

    /**
     * Find users by tenant ID with pagination
     */
    Page<User> findByTenantId(UUID tenantId, Pageable pageable);

    /**
     * Find users by tenant ID and status
     */
    List<User> findByTenantIdAndStatus(UUID tenantId, User.UserStatus status);

    /**
     * Find users by tenant ID and role
     */
    List<User> findByTenantIdAndRole(UUID tenantId, User.UserRole role);

    /**
     * Find users by status across all tenants
     */
    List<User> findByStatus(User.UserStatus status);

    /**
     * Find users by role across all tenants
     */
    List<User> findByRole(User.UserRole role);

    /**
     * Search users by name or email (case-insensitive)
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<User> findByNameOrEmailContainingIgnoreCase(@Param("searchTerm") String searchTerm);

    /**
     * Search users by name or email with pagination
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<User> findByNameOrEmailContainingIgnoreCase(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Find users by tenant and search term
     */
    @Query("SELECT u FROM User u WHERE u.tenant.id = :tenantId AND (" +
           "LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<User> findByTenantIdAndNameOrEmailContainingIgnoreCase(@Param("tenantId") UUID tenantId, @Param("searchTerm") String searchTerm);

    /**
     * Check if email exists (for validation)
     */
    boolean existsByEmail(String email);

    /**
     * Count users by tenant
     */
    long countByTenantId(UUID tenantId);

    /**
     * Count users by tenant and status
     */
    long countByTenantIdAndStatus(UUID tenantId, User.UserStatus status);

    /**
     * Count users by status across all tenants
     */
    long countByStatus(User.UserStatus status);

    /**
     * Count users by role across all tenants
     */
    long countByRole(User.UserRole role);

    /**
     * Count users by tenant and role
     */
    long countByTenantIdAndRole(UUID tenantId, User.UserRole role);

    /**
     * Find users who haven't logged in recently
     */
    @Query("SELECT u FROM User u WHERE u.lastLoginAt < :cutoffDate OR u.lastLoginAt IS NULL")
    List<User> findInactiveUsers(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find users pending email verification
     */
    List<User> findByEmailVerifiedFalse();

    /**
     * Find users by email verification token
     */
    Optional<User> findByEmailVerificationToken(String token);

    /**
     * Find tenant administrators
     */
    @Query("SELECT u FROM User u WHERE u.tenant.id = :tenantId AND u.role = 'ADMIN'")
    List<User> findTenantAdministrators(@Param("tenantId") UUID tenantId);

    /**
     * Find recently created users
     */
    @Query("SELECT u FROM User u WHERE u.createdAt >= :cutoffDate ORDER BY u.createdAt DESC")
    List<User> findRecentlyCreatedUsers(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Get user activity summary for tenant
     */
    @Query("SELECT u.status, COUNT(u) FROM User u WHERE u.tenant.id = :tenantId GROUP BY u.status")
    List<Object[]> getUserActivitySummaryByTenant(@Param("tenantId") UUID tenantId);

    /**
     * Get overall user statistics
     */
    @Query("SELECT u.status, u.role, COUNT(u) FROM User u GROUP BY u.status, u.role")
    List<Object[]> getUserStatistics();
}
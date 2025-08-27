package com.enterprise.rag.admin.repository;

import com.enterprise.rag.shared.entity.User;
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
 * Repository interface for User entity operations in the admin service.
 * Provides CRUD operations and custom queries for user management across tenants.
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
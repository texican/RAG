package com.enterprise.rag.admin.service;

import com.enterprise.rag.shared.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for user management operations in the admin service.
 * Provides comprehensive user administration across all tenants.
 */
public interface UserService {

    /**
     * Get user by ID
     */
    User getUserById(UUID userId);

    /**
     * Get user by email
     */
    User getUserByEmail(String email);

    /**
     * Get all users with pagination
     */
    Page<User> getAllUsers(Pageable pageable);

    /**
     * Get users by tenant ID
     */
    List<User> getUsersByTenantId(UUID tenantId);

    /**
     * Get users by tenant ID with pagination
     */
    Page<User> getUsersByTenantId(UUID tenantId, Pageable pageable);

    /**
     * Search users by name or email
     */
    Page<User> searchUsers(String searchTerm, Pageable pageable);

    /**
     * Get users by status
     */
    List<User> getUsersByStatus(User.UserStatus status);

    /**
     * Get users by role
     */
    List<User> getUsersByRole(User.UserRole role);

    /**
     * Update user status
     */
    User updateUserStatus(UUID userId, User.UserStatus status);

    /**
     * Update user role
     */
    User updateUserRole(UUID userId, User.UserRole role);

    /**
     * Reset user password
     */
    void resetUserPassword(UUID userId, String newPassword);

    /**
     * Delete user
     */
    void deleteUser(UUID userId);

    /**
     * Get user statistics
     */
    UserStatistics getUserStatistics();

    /**
     * Get tenant user statistics
     */
    TenantUserStatistics getTenantUserStatistics(UUID tenantId);

    /**
     * Find inactive users (haven't logged in for specified days)
     */
    List<User> findInactiveUsers(int days);

    /**
     * Find users pending email verification
     */
    List<User> findUsersAwaitingVerification();

    /**
     * Statistics classes
     */
    record UserStatistics(
            long totalUsers,
            long activeUsers,
            long suspendedUsers,
            long pendingVerificationUsers,
            long adminUsers,
            long regularUsers,
            long readerUsers
    ) {}

    record TenantUserStatistics(
            UUID tenantId,
            String tenantName,
            long totalUsers,
            long activeUsers,
            long adminUsers
    ) {}
}
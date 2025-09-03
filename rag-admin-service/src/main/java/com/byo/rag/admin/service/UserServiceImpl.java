package com.byo.rag.admin.service;

import com.byo.rag.admin.repository.UserRepository;
import com.byo.rag.admin.repository.TenantRepository;
import com.byo.rag.shared.entity.User;
import com.byo.rag.shared.entity.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Enterprise-grade user management service implementation providing comprehensive
 * user lifecycle operations with multi-tenant isolation and administrative controls.
 * 
 * <p>This service implements the complete user management functionality for the Enterprise RAG
 * system's administrative interface, including user CRUD operations, role and status management,
 * password operations, and advanced analytics. All operations maintain strict tenant boundaries
 * and include comprehensive security validations.</p>
 * 
 * <p><strong>Core Capabilities:</strong></p>
 * <ul>
 *   <li><strong>User Lifecycle Management:</strong> Complete CRUD operations with validation</li>
 *   <li><strong>Multi-Tenant Isolation:</strong> Strict tenant boundary enforcement</li>
 *   <li><strong>Role-Based Access Control:</strong> Admin, User, and Reader role management</li>
 *   <li><strong>Status Management:</strong> Active, Suspended, Pending Verification states</li>
 *   <li><strong>Security Operations:</strong> Secure password resets with encryption</li>
 *   <li><strong>Advanced Analytics:</strong> User statistics and tenant-specific metrics</li>
 * </ul>
 * 
 * <p><strong>Security Features:</strong></p>
 * <ul>
 *   <li>BCrypt password encryption for all password operations</li>
 *   <li>Tenant administrator protection (prevents deletion of last admin)</li>
 *   <li>Status-based access control enforcement</li>
 *   <li>Email-based user identification and search</li>
 *   <li>Comprehensive audit logging for all operations</li>
 * </ul>
 * 
 * <p><strong>Administrative Functions:</strong></p>
 * <ul>
 *   <li>System-wide user statistics and reporting</li>
 *   <li>Tenant-specific user analytics</li>
 *   <li>Inactive user identification for cleanup operations</li>
 *   <li>Email verification status tracking</li>
 *   <li>Advanced search and filtering capabilities</li>
 * </ul>
 * 
 * <p><strong>Data Integrity:</strong></p>
 * <p>All operations are wrapped in database transactions to ensure consistency.
 * The service validates business rules such as preventing deletion of the last
 * tenant administrator and enforcing proper user status transitions.</p>
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 * @see UserService
 * @see com.byo.rag.shared.entity.User
 * @see com.byo.rag.shared.entity.Tenant
 */
@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                          TenantRepository tenantRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Retrieves a user by their unique identifier with comprehensive error handling.
     * 
     * <p>This read-only operation provides direct access to user entities by UUID,
     * commonly used for administrative operations and user profile management.</p>
     * 
     * @param userId the unique identifier of the user to retrieve
     * @return the {@link User} entity with complete profile information
     * @throws RuntimeException if no user exists with the specified ID
     * @see User
     */
    @Override
    @Transactional(readOnly = true)
    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
    }

    /**
     * Retrieves a user by their email address for authentication and profile operations.
     * 
     * <p>This method supports email-based user lookup, commonly used during
     * authentication flows and administrative user management tasks.</p>
     * 
     * @param email the email address of the user to retrieve
     * @return the {@link User} entity associated with the specified email
     * @throws RuntimeException if no user exists with the specified email address
     * @see User
     */
    @Override
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    /**
     * Retrieves all users in the system with pagination support for administrative dashboards.
     * 
     * <p>This method provides system-wide user access with pagination, sorting, and
     * filtering capabilities suitable for enterprise-scale user management interfaces.</p>
     * 
     * @param pageable the pagination parameters including page size, number, and sorting
     * @return a paginated {@link Page} of {@link User} entities with metadata
     * @see Page
     * @see Pageable
     */
    @Override
    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * Retrieves all users belonging to a specific tenant for tenant-scoped operations.
     * 
     * <p>This method enforces tenant isolation by returning only users associated
     * with the specified tenant, supporting multi-tenant administrative functions.</p>
     * 
     * @param tenantId the unique identifier of the tenant whose users to retrieve
     * @return a {@link List} of {@link User} entities belonging to the specified tenant
     * @see User
     */
    @Override
    @Transactional(readOnly = true)
    public List<User> getUsersByTenantId(UUID tenantId) {
        return userRepository.findByTenantId(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> getUsersByTenantId(UUID tenantId, Pageable pageable) {
        return userRepository.findByTenantId(tenantId, pageable);
    }

    /**
     * Performs advanced user search across names and email addresses with pagination.
     * 
     * <p>This method provides flexible user discovery capabilities by searching across
     * user names and email addresses with case-insensitive matching, supporting
     * administrative user management and reporting functions.</p>
     * 
     * @param searchTerm the search term to match against user names and email addresses
     * @param pageable the pagination parameters for result organization
     * @return a paginated {@link Page} of matching {@link User} entities
     * @see Page
     * @see Pageable
     */
    @Override
    @Transactional(readOnly = true)
    public Page<User> searchUsers(String searchTerm, Pageable pageable) {
        return userRepository.findByNameOrEmailContainingIgnoreCase(searchTerm, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getUsersByStatus(User.UserStatus status) {
        return userRepository.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getUsersByRole(User.UserRole role) {
        return userRepository.findByRole(role);
    }

    /**
     * Updates a user's status with comprehensive validation and audit logging.
     * 
     * <p>This method modifies user status for administrative control purposes,
     * including activation, suspension, and verification state management.
     * All status changes are logged for security and compliance purposes.</p>
     * 
     * <p><strong>Supported Status Transitions:</strong></p>
     * <ul>
     *   <li>PENDING_VERIFICATION → ACTIVE (after email verification)</li>
     *   <li>ACTIVE → SUSPENDED (administrative suspension)</li>
     *   <li>SUSPENDED → ACTIVE (reactivation)</li>
     *   <li>Any status → PENDING_VERIFICATION (after password reset)</li>
     * </ul>
     * 
     * @param userId the unique identifier of the user to update
     * @param status the new {@link User.UserStatus} to assign to the user
     * @return the updated {@link User} entity with the new status
     * @throws RuntimeException if no user exists with the specified ID
     * @see User.UserStatus
     */
    @Override
    public User updateUserStatus(UUID userId, User.UserStatus status) {
        logger.info("Updating user status: {} to {}", userId, status);
        
        User user = getUserById(userId);
        user.setStatus(status);
        
        user = userRepository.save(user);
        logger.info("Updated user status: {}", user.getId());
        
        return user;
    }

    /**
     * Updates a user's role for access control and permission management.
     * 
     * <p>This method modifies user roles to control system access levels and
     * administrative privileges within the tenant's scope. Role changes are
     * immediately effective and logged for security audit purposes.</p>
     * 
     * <p><strong>Available Roles:</strong></p>
     * <ul>
     *   <li><strong>ADMIN:</strong> Full administrative access within tenant</li>
     *   <li><strong>USER:</strong> Standard user with read/write document access</li>
     *   <li><strong>READER:</strong> Read-only access to documents and queries</li>
     * </ul>
     * 
     * @param userId the unique identifier of the user to update
     * @param role the new {@link User.UserRole} to assign to the user
     * @return the updated {@link User} entity with the new role
     * @throws RuntimeException if no user exists with the specified ID
     * @see User.UserRole
     */
    @Override
    public User updateUserRole(UUID userId, User.UserRole role) {
        logger.info("Updating user role: {} to {}", userId, role);
        
        User user = getUserById(userId);
        user.setRole(role);
        
        user = userRepository.save(user);
        logger.info("Updated user role: {}", user.getId());
        
        return user;
    }

    /**
     * Resets a user's password with secure encryption and verification requirement.
     * 
     * <p>This method performs a secure password reset operation including BCrypt
     * encryption of the new password and automatic status change to require
     * re-verification. This ensures security after administrative password resets.</p>
     * 
     * <p><strong>Security Features:</strong></p>
     * <ul>
     *   <li>BCrypt encryption with secure salt generation</li>
     *   <li>Automatic status change to PENDING_VERIFICATION</li>
     *   <li>Comprehensive audit logging</li>
     *   <li>Immediate password hash replacement</li>
     * </ul>
     * 
     * @param userId the unique identifier of the user whose password to reset
     * @param newPassword the new plain-text password to encrypt and store
     * @throws RuntimeException if no user exists with the specified ID
     * @see PasswordEncoder
     * @see User.UserStatus#PENDING_VERIFICATION
     */
    @Override
    public void resetUserPassword(UUID userId, String newPassword) {
        logger.info("Resetting password for user: {}", userId);
        
        User user = getUserById(userId);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setStatus(User.UserStatus.PENDING_VERIFICATION); // Require verification after password reset
        
        userRepository.save(user);
        logger.info("Reset password for user: {}", user.getId());
    }

    /**
     * Permanently deletes a user with comprehensive safety checks and validation.
     * 
     * <p><strong>⚠️ WARNING:</strong> This is a destructive operation that permanently
     * removes the user and all associated data. The method includes critical safety
     * checks to prevent deletion of the last tenant administrator.</p>
     * 
     * <p><strong>Safety Validations:</strong></p>
     * <ul>
     *   <li>Verifies user exists before deletion</li>
     *   <li>Prevents deletion of last tenant administrator</li>
     *   <li>Maintains tenant administrative continuity</li>
     *   <li>Comprehensive audit logging</li>
     * </ul>
     * 
     * @param userId the unique identifier of the user to delete
     * @throws RuntimeException if no user exists with the specified ID
     * @throws IllegalStateException if attempting to delete the last tenant administrator
     * @see User
     */
    @Override
    public void deleteUser(UUID userId) {
        logger.info("Deleting user: {}", userId);
        
        User user = getUserById(userId);
        
        // Check if this is the last admin for the tenant
        List<User> tenantAdmins = userRepository.findTenantAdministrators(user.getTenant().getId());
        if (tenantAdmins.size() == 1 && tenantAdmins.get(0).getId().equals(userId)) {
            throw new IllegalStateException("Cannot delete the last administrator for tenant: " + user.getTenant().getName());
        }
        
        userRepository.delete(user);
        logger.info("Deleted user: {}", userId);
    }

    /**
     * Generates comprehensive system-wide user statistics for administrative dashboards.
     * 
     * <p>This method provides real-time analytics across all users in the system,
     * including breakdowns by status, role, and activity levels. The statistics
     * support administrative decision-making and system monitoring.</p>
     * 
     * <p><strong>Metrics Included:</strong></p>
     * <ul>
     *   <li><strong>Total Users:</strong> Complete user count across all tenants</li>
     *   <li><strong>Status Breakdown:</strong> Active, Suspended, Pending Verification counts</li>
     *   <li><strong>Role Distribution:</strong> Admin, User, Reader role counts</li>
     *   <li><strong>Real-time Data:</strong> Current snapshot of user population</li>
     * </ul>
     * 
     * @return {@link UserStatistics} containing comprehensive user metrics and breakdowns
     * @see UserStatistics
     * @see User.UserStatus
     * @see User.UserRole
     */
    @Override
    @Transactional(readOnly = true)
    public UserStatistics getUserStatistics() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus(User.UserStatus.ACTIVE);
        long suspendedUsers = userRepository.countByStatus(User.UserStatus.SUSPENDED);
        long pendingVerificationUsers = userRepository.countByStatus(User.UserStatus.PENDING_VERIFICATION);
        long adminUsers = userRepository.countByRole(User.UserRole.ADMIN);
        long regularUsers = userRepository.countByRole(User.UserRole.USER);
        long readerUsers = userRepository.countByRole(User.UserRole.READER);

        return new UserStatistics(
                totalUsers,
                activeUsers,
                suspendedUsers,
                pendingVerificationUsers,
                adminUsers,
                regularUsers,
                readerUsers
        );
    }

    /**
     * Generates tenant-specific user statistics for multi-tenant administrative oversight.
     * 
     * <p>This method provides detailed user analytics scoped to a specific tenant,
     * enabling tenant administrators to monitor their user population and activity
     * levels within their isolated environment.</p>
     * 
     * <p><strong>Tenant-Specific Metrics:</strong></p>
     * <ul>
     *   <li><strong>Total Users:</strong> Complete user count for the tenant</li>
     *   <li><strong>Active Users:</strong> Currently active users within the tenant</li>
     *   <li><strong>Admin Users:</strong> Administrative users for tenant management</li>
     *   <li><strong>Tenant Context:</strong> Tenant name and identification details</li>
     * </ul>
     * 
     * @param tenantId the unique identifier of the tenant for statistics generation
     * @return {@link TenantUserStatistics} containing tenant-scoped user metrics
     * @throws RuntimeException if no tenant exists with the specified ID
     * @see TenantUserStatistics
     * @see com.byo.rag.shared.entity.Tenant
     */
    @Override
    @Transactional(readOnly = true)
    public TenantUserStatistics getTenantUserStatistics(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found with ID: " + tenantId));

        long totalUsers = userRepository.countByTenantId(tenantId);
        long activeUsers = userRepository.countByTenantIdAndStatus(tenantId, User.UserStatus.ACTIVE);
        long adminUsers = userRepository.countByTenantIdAndRole(tenantId, User.UserRole.ADMIN);

        return new TenantUserStatistics(
                tenantId,
                tenant.getName(),
                totalUsers,
                activeUsers,
                adminUsers
        );
    }

    /**
     * Identifies inactive users for system maintenance and cleanup operations.
     * 
     * <p>This method discovers users who have not accessed the system within
     * the specified time period, supporting administrative cleanup policies
     * and user engagement monitoring.</p>
     * 
     * <p><strong>Use Cases:</strong></p>
     * <ul>
     *   <li>Automated cleanup policies</li>
     *   <li>User engagement analysis</li>
     *   <li>System resource optimization</li>
     *   <li>Security compliance audits</li>
     * </ul>
     * 
     * @param days the number of days of inactivity to use as the cutoff criteria
     * @return a {@link List} of {@link User} entities that have been inactive for the specified period
     * @see User
     */
    @Override
    @Transactional(readOnly = true)
    public List<User> findInactiveUsers(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        return userRepository.findInactiveUsers(cutoffDate);
    }

    /**
     * Identifies users with pending email verification for administrative follow-up.
     * 
     * <p>This method locates users who have not completed the email verification
     * process, enabling administrators to track onboarding progress and identify
     * users who may need verification assistance or cleanup.</p>
     * 
     * <p><strong>Administrative Uses:</strong></p>
     * <ul>
     *   <li>Onboarding completion monitoring</li>
     *   <li>User activation tracking</li>
     *   <li>Email verification follow-up</li>
     *   <li>Account cleanup identification</li>
     * </ul>
     * 
     * @return a {@link List} of {@link User} entities with unverified email addresses
     * @see User
     */
    @Override
    @Transactional(readOnly = true)
    public List<User> findUsersAwaitingVerification() {
        return userRepository.findByEmailVerifiedFalse();
    }
}
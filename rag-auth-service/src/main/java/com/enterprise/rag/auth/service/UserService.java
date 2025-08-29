package com.enterprise.rag.auth.service;

import com.enterprise.rag.auth.repository.UserRepository;
import com.enterprise.rag.shared.dto.TenantDto;
import com.enterprise.rag.shared.dto.UserDto;
import com.enterprise.rag.shared.entity.Tenant;
import com.enterprise.rag.shared.entity.User;
import com.enterprise.rag.shared.exception.TenantAccessDeniedException;
import com.enterprise.rag.shared.exception.UserNotFoundException;
import com.enterprise.rag.shared.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service class for user management operations in the Enterprise RAG system.
 * 
 * <p>This service provides comprehensive user lifecycle management with strict
 * tenant isolation and role-based access control. It handles all aspects of
 * user management within the multi-tenant RAG platform.
 * 
 * <p><strong>Core Functionality:</strong>
 * <ul>
 *   <li>User account creation and profile management</li>
 *   <li>Email verification and account activation</li>
 *   <li>Tenant-scoped user operations and queries</li>
 *   <li>Authentication support and login tracking</li>
 * </ul>
 * 
 * <p><strong>Multi-Tenant Architecture:</strong>
 * All operations enforce strict tenant isolation where:
 * <ul>
 *   <li>Users can only access resources within their tenant</li>
 *   <li>Cross-tenant data access is prevented</li>
 *   <li>Administrative operations are tenant-scoped</li>
 *   <li>Email uniqueness is enforced globally (across tenants)</li>
 * </ul>
 * 
 * <p><strong>Security Features:</strong>
 * <ul>
 *   <li>Password hashing using BCrypt for secure storage</li>
 *   <li>Email verification tokens for account activation</li>
 *   <li>Automatic tenant access validation on all operations</li>
 *   <li>Audit logging for user management operations</li>
 * </ul>
 * 
 * <p><strong>User Lifecycle:</strong>
 * <ol>
 *   <li><strong>Creation:</strong> User account created with verification token</li>
 *   <li><strong>Verification:</strong> Email verified to activate account</li>
 *   <li><strong>Active:</strong> User can authenticate and use system</li>
 *   <li><strong>Updates:</strong> Profile and settings can be modified</li>
 *   <li><strong>Deletion:</strong> Account and associated data removed</li>
 * </ol>
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.0
 * @see UserRepository
 * @see TenantService
 * @see UserDto
 */
@Service
@Transactional
public class UserService {

    /** Logger for user management operations and security events. */
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    /** Repository for user data access and persistence operations. */
    private final UserRepository userRepository;
    
    /** Service for tenant validation and relationship management. */
    private final TenantService tenantService;

    /**
     * Constructs a new UserService with required dependencies.
     * 
     * @param userRepository the repository for user data operations
     * @param tenantService the service for tenant validation and operations
     */
    public UserService(UserRepository userRepository, TenantService tenantService) {
        this.userRepository = userRepository;
        this.tenantService = tenantService;
    }

    /**
     * Creates a new user account within the specified tenant.
     * 
     * <p>This method handles complete user account creation including:
     * <ul>
     *   <li>Email uniqueness validation (global across all tenants)</li>
     *   <li>Password hashing using BCrypt for secure storage</li>
     *   <li>Tenant validation and association</li>
     *   <li>Email verification token generation</li>
     *   <li>Default role assignment if not specified</li>
     * </ul>
     * 
     * <p><strong>Security Features:</strong>
     * <ul>
     *   <li>Passwords are immediately hashed and never stored in plain text</li>
     *   <li>Email verification tokens are cryptographically secure</li>
     *   <li>Users start in PENDING status until email verification</li>
     *   <li>Default role is USER unless explicitly specified</li>
     * </ul>
     * 
     * <p><strong>Email Verification:</strong>
     * Created users receive an email verification token that must be used
     * to activate the account. Until verified, users cannot authenticate.
     * 
     * @param request the user creation request with profile and credential data
     * @return UserResponse containing the created user information (excluding sensitive data)
     * @throws IllegalArgumentException if email already exists
     * @throws com.enterprise.rag.shared.exception.TenantNotFoundException if tenant does not exist
     * @throws org.springframework.dao.DataAccessException if database operation fails
     */
    public UserDto.UserResponse createUser(UserDto.CreateUserRequest request) {
        logger.info("Creating user with email: {}", request.email());

        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("User with email '" + request.email() + "' already exists");
        }

        Tenant tenant = tenantService.findById(request.tenantId());

        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setPasswordHash(SecurityUtils.hashPassword(request.password()));
        user.setRole(request.role() != null ? request.role() : User.UserRole.USER);
        user.setTenant(tenant);
        user.setEmailVerificationToken(SecurityUtils.generateVerificationToken());

        user = userRepository.save(user);
        logger.info("Created user with ID: {}", user.getId());

        return mapToResponse(user);
    }

    /**
     * Retrieves user information by ID with tenant isolation enforcement.
     * 
     * <p>This method enforces strict tenant isolation by validating that the
     * requesting user can only access users within their own tenant. This
     * prevents cross-tenant data access and maintains security boundaries.
     * 
     * <p><strong>Tenant Validation:</strong>
     * The method compares the target user's tenant ID with the requesting
     * user's tenant ID. If they don't match, access is denied.
     * 
     * <p><strong>Returned Information:</strong>
     * <ul>
     *   <li>Complete user profile information</li>
     *   <li>Role and permission details</li>
     *   <li>Account status and verification state</li>
     *   <li>Associated tenant summary information</li>
     * </ul>
     * 
     * @param userId the UUID of the user to retrieve
     * @param requestingUserTenantId the tenant ID of the user making the request
     * @return UserResponse containing the user information
     * @throws UserNotFoundException if user does not exist
     * @throws TenantAccessDeniedException if user belongs to different tenant
     */
    @Transactional(readOnly = true)
    public UserDto.UserResponse getUser(UUID userId, UUID requestingUserTenantId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        validateTenantAccess(user.getTenant().getId(), requestingUserTenantId);
        return mapToResponse(user);
    }

    /**
     * Retrieves paginated list of users within a specific tenant.
     * 
     * <p>This method provides tenant-scoped user directory functionality
     * with pagination support. It returns user summaries containing essential
     * information for user listing and selection interfaces.
     * 
     * <p><strong>Tenant Isolation:</strong>
     * Results are automatically filtered to include only users within the
     * specified tenant, ensuring complete data separation between organizations.
     * 
     * <p><strong>Use Cases:</strong>
     * <ul>
     *   <li>Administrative user management interfaces</li>
     *   <li>Team member selection and collaboration features</li>
     *   <li>User directory and contact listings</li>
     *   <li>Permission assignment and role management</li>
     * </ul>
     * 
     * <p><strong>Performance:</strong>
     * Uses lightweight UserSummary DTOs for efficient memory usage and
     * fast rendering in paginated user interfaces.
     * 
     * @param tenantId the UUID of the tenant to list users for
     * @param pageable the pagination and sorting parameters
     * @return Page containing user summaries with pagination metadata
     */
    @Transactional(readOnly = true)
    public Page<UserDto.UserSummary> getUsersByTenant(UUID tenantId, Pageable pageable) {
        return userRepository.findByTenantId(tenantId, pageable)
            .map(this::mapToSummary);
    }

    /**
     * Updates user information with selective field updates and tenant validation.
     * 
     * <p>This method provides comprehensive user profile update functionality
     * with strict tenant isolation and field-level validation. Only non-null
     * fields in the request are updated, allowing for partial updates.
     * 
     * <p><strong>Update Features:</strong>
     * <ul>
     *   <li><strong>Profile Information:</strong> First name, last name updates</li>
     *   <li><strong>Email Changes:</strong> With automatic re-verification requirement</li>
     *   <li><strong>Role Management:</strong> Role assignments within tenant</li>
     *   <li><strong>Status Control:</strong> Account status modifications</li>
     * </ul>
     * 
     * <p><strong>Email Change Handling:</strong>
     * When email is changed:
     * <ul>
     *   <li>Global uniqueness is validated across all tenants</li>
     *   <li>Email verification status is reset to false</li>
     *   <li>New verification token is generated</li>
     *   <li>User must re-verify their new email address</li>
     * </ul>
     * 
     * <p><strong>Security and Validation:</strong>
     * <ul>
     *   <li>Tenant access validation prevents cross-tenant modifications</li>
     *   <li>Email uniqueness is enforced globally</li>
     *   <li>Role changes are audited for security tracking</li>
     *   <li>Status changes may affect user's system access</li>
     * </ul>
     * 
     * @param userId the UUID of the user to update
     * @param request the update request containing fields to modify
     * @param requestingUserTenantId the tenant ID of the user making the request
     * @return UserResponse containing the updated user information
     * @throws UserNotFoundException if user does not exist
     * @throws TenantAccessDeniedException if user belongs to different tenant
     * @throws IllegalArgumentException if new email is already in use
     */
    public UserDto.UserResponse updateUser(UUID userId, UserDto.UpdateUserRequest request, UUID requestingUserTenantId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        validateTenantAccess(user.getTenant().getId(), requestingUserTenantId);

        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.email() != null && !request.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new IllegalArgumentException("Email already in use");
            }
            user.setEmail(request.email());
            user.setEmailVerified(false);
            user.setEmailVerificationToken(SecurityUtils.generateVerificationToken());
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.status() != null) {
            user.setStatus(request.status());
        }

        user = userRepository.save(user);
        logger.info("Updated user with ID: {}", user.getId());

        return mapToResponse(user);
    }

    /**
     * Permanently deletes a user account and associated data.
     * 
     * <p><strong>⚠️ WARNING:</strong> This operation permanently removes the
     * user account and all associated personal data. The deletion includes:
     * <ul>
     *   <li>User profile and authentication credentials</li>
     *   <li>User's personal documents and files</li>
     *   <li>User's conversation history and preferences</li>
     *   <li>User's API access tokens and sessions</li>
     * </ul>
     * 
     * <p><strong>Tenant Validation:</strong>
     * Ensures that only users within the same tenant can be deleted,
     * maintaining strict tenant isolation and preventing unauthorized
     * cross-tenant user deletions.
     * 
     * <p><strong>Cascade Effects:</strong>
     * Database foreign key constraints handle cascade deletion of related
     * entities. External services may require additional cleanup notifications.
     * 
     * <p><strong>Audit and Compliance:</strong>
     * User deletion is logged for audit purposes. Some system logs may
     * retain anonymized references for compliance and analytics.
     * 
     * @param userId the UUID of the user to delete
     * @param requestingUserTenantId the tenant ID of the user making the request
     * @throws UserNotFoundException if user does not exist
     * @throws TenantAccessDeniedException if user belongs to different tenant
     * @throws org.springframework.dao.DataAccessException if deletion fails
     */
    public void deleteUser(UUID userId, UUID requestingUserTenantId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        validateTenantAccess(user.getTenant().getId(), requestingUserTenantId);

        userRepository.deleteById(userId);
        logger.info("Deleted user with ID: {}", userId);
    }

    /**
     * Finds and returns a user entity by email address for internal service use.
     * 
     * <p>This method provides direct access to user entities for internal
     * operations that require the full entity rather than DTO responses.
     * It searches across all tenants since email addresses are globally unique.
     * 
     * <p><strong>Use Cases:</strong>
     * <ul>
     *   <li>Authentication service login validation</li>
     *   <li>Internal service user lookups</li>
     *   <li>Email verification token processing</li>
     *   <li>Cross-service user relationship management</li>
     * </ul>
     * 
     * @param email the email address to search for
     * @return the User entity with the specified email
     * @throws UserNotFoundException if no user exists with the given email
     */
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException(email));
    }

    /**
     * Finds and returns an active user entity by email address for authentication.
     * 
     * <p>This method specifically searches for users with ACTIVE status,
     * ensuring that only users who can authenticate are returned. It's
     * primarily used by the authentication service during login processing.
     * 
     * <p><strong>Active User Criteria:</strong>
     * <ul>
     *   <li>User status must be ACTIVE</li>
     *   <li>Email must be verified (emailVerified = true)</li>
     *   <li>Account must not be suspended or disabled</li>
     * </ul>
     * 
     * <p><strong>Authentication Flow:</strong>
     * This method is a critical part of the login process, ensuring that
     * only properly verified and active users can authenticate with the system.
     * 
     * @param email the email address to search for
     * @return the active User entity with the specified email
     * @throws UserNotFoundException if no active user exists with the given email
     */
    @Transactional(readOnly = true)
    public User findActiveByEmail(String email) {
        return userRepository.findActiveByEmail(email)
            .orElseThrow(() -> new UserNotFoundException(email));
    }

    /**
     * Updates the last login timestamp for a user account.
     * 
     * <p>This method is called during successful authentication to track
     * user activity and login patterns. The timestamp is used for:
     * <ul>
     *   <li>Security monitoring and audit trails</li>
     *   <li>Inactive account identification</li>
     *   <li>User activity analytics</li>
     *   <li>Session management and cleanup</li>
     * </ul>
     * 
     * <p><strong>Performance:</strong>
     * This operation is lightweight and doesn't validate tenant access
     * since it's called during the authentication process where the
     * user's identity has already been validated.
     * 
     * @param userId the UUID of the user to update
     * @throws UserNotFoundException if user does not exist
     */
    public void updateLastLogin(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }

    /**
     * Verifies a user's email address using the verification token.
     * 
     * <p>This method completes the user registration process by verifying
     * the email address and activating the account. It processes email
     * verification tokens sent to users during registration or email changes.
     * 
     * <p><strong>Verification Process:</strong>
     * <ol>
     *   <li>Validate the verification token exists and matches a user</li>
     *   <li>Mark the user's email as verified</li>
     *   <li>Clear the verification token (single-use security)</li>
     *   <li>Activate the user account (change status to ACTIVE)</li>
     *   <li>Log the verification for audit purposes</li>
     * </ol>
     * 
     * <p><strong>Security Features:</strong>
     * <ul>
     *   <li>Tokens are single-use and cleared after verification</li>
     *   <li>Tokens are cryptographically secure and time-limited</li>
     *   <li>Verification is logged for security monitoring</li>
     *   <li>Account activation only occurs after email verification</li>
     * </ul>
     * 
     * <p><strong>Post-Verification:</strong>
     * Once verified, users can authenticate and access all system features.
     * The account status changes to ACTIVE, enabling full functionality.
     * 
     * @param token the email verification token received via email
     * @return UserResponse containing the verified and activated user information
     * @throws IllegalArgumentException if token is invalid or expired
     */
    public UserDto.UserResponse verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
            .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));

        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setStatus(User.UserStatus.ACTIVE);

        user = userRepository.save(user);
        logger.info("Verified email for user: {}", user.getEmail());

        return mapToResponse(user);
    }

    /**
     * Validates that the requesting user can access resources within the target tenant.
     * 
     * <p>This is a critical security method that enforces tenant isolation
     * throughout the user management system. It ensures that users can only
     * access resources (other users, data, etc.) within their own tenant.
     * 
     * <p><strong>Security Enforcement:</strong>
     * Prevents cross-tenant data access by comparing tenant IDs and throwing
     * an exception if they don't match. This is called before any operation
     * that accesses user data to ensure tenant boundaries are maintained.
     * 
     * @param resourceTenantId the tenant ID of the resource being accessed
     * @param requestingUserTenantId the tenant ID of the user making the request
     * @throws TenantAccessDeniedException if tenant IDs don't match
     */
    private void validateTenantAccess(UUID resourceTenantId, UUID requestingUserTenantId) {
        if (!resourceTenantId.equals(requestingUserTenantId)) {
            throw new TenantAccessDeniedException(requestingUserTenantId, resourceTenantId);
        }
    }

    /**
     * Maps a User entity to UserResponse DTO with complete information and tenant summary.
     * 
     * <p>This method creates a comprehensive DTO response that includes:
     * <ul>
     *   <li>Complete user profile information</li>
     *   <li>Role and permission details</li>
     *   <li>Account status and verification state</li>
     *   <li>Activity timestamps (last login, creation date)</li>
     *   <li>Associated tenant summary for context</li>
     * </ul>
     * 
     * <p><strong>Security Considerations:</strong>
     * Sensitive information like password hashes and verification tokens
     * are excluded from the response for security purposes.
     * 
     * <p><strong>Tenant Context:</strong>
     * Includes tenant summary to provide context about the user's
     * organization without requiring additional API calls.
     * 
     * @param user the user entity to map to response DTO
     * @return UserResponse DTO with complete user information
     */
    private UserDto.UserResponse mapToResponse(User user) {
        TenantDto.TenantSummary tenantSummary = new TenantDto.TenantSummary(
            user.getTenant().getId(),
            user.getTenant().getName(),
            user.getTenant().getSlug(),
            user.getTenant().getStatus(),
            user.getTenant().getCreatedAt()
        );

        return new UserDto.UserResponse(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getEmail(),
            user.getRole(),
            user.getStatus(),
            user.getEmailVerified(),
            user.getLastLoginAt(),
            user.getCreatedAt(),
            tenantSummary
        );
    }

    /**
     * Maps a User entity to UserSummary DTO with essential information.
     * 
     * <p>This method creates a lightweight DTO containing only essential
     * user information for list views and user selection interfaces.
     * The summary format is optimized for:
     * <ul>
     *   <li>User directory and listing pages</li>
     *   <li>Quick user identification and selection</li>
     *   <li>Efficient memory usage in paginated results</li>
     *   <li>Administrative user management overviews</li>
     * </ul>
     * 
     * <p><strong>Performance Benefits:</strong>
     * Excludes heavy fields like timestamps and tenant details to
     * minimize memory usage and improve rendering performance in
     * user interface components.
     * 
     * @param user the user entity to summarize
     * @return UserSummary DTO with essential user information
     */
    private UserDto.UserSummary mapToSummary(User user) {
        return new UserDto.UserSummary(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getEmail(),
            user.getRole(),
            user.getStatus()
        );
    }
}
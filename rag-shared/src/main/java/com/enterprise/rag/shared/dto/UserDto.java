package com.enterprise.rag.shared.dto;

import com.enterprise.rag.shared.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Objects for user management and authentication in the Enterprise RAG System.
 * <p>
 * This sealed interface defines all user-related DTOs used across the RAG microservices
 * for authentication, user management, and role-based access control within the multi-tenant architecture.
 * </p>
 * 
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Multi-tenant User Management</strong> - Users belong to specific tenants</li>
 *   <li><strong>Role-based Access Control</strong> - ADMIN, USER, VIEWER roles with different permissions</li>
 *   <li><strong>JWT Authentication</strong> - Token-based authentication for stateless operations</li>
 *   <li><strong>Email Verification</strong> - Account verification workflow support</li>
 * </ul>
 * 
 * <h3>User Lifecycle:</h3>
 * <ol>
 *   <li><strong>Registration</strong> - Create user account with tenant assignment</li>
 *   <li><strong>Email Verification</strong> - Verify email address before activation</li>
 *   <li><strong>Authentication</strong> - Login with email/password to receive JWT tokens</li>
 *   <li><strong>Active Usage</strong> - Access RAG features based on role permissions</li>
 *   <li><strong>Management</strong> - Update profile, change roles, deactivate accounts</li>
 * </ol>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Create new user
 * var createRequest = new UserDto.CreateUserRequest(
 *     "John",
 *     "Doe",
 *     "john.doe@company.com",
 *     "securePassword123",
 *     User.UserRole.USER,
 *     tenantId
 * );
 * 
 * // Login user
 * var loginRequest = new UserDto.LoginRequest("john.doe@company.com", "securePassword123");
 * 
 * // Handle authentication response
 * if (response instanceof UserDto.LoginResponse(var token, var refresh, var expires, var user)) {
 *     // Store tokens and redirect to dashboard
 * }
 * }</pre>
 * 
 * @author Enterprise RAG Development Team
 * @since 1.0.0
 * @see com.enterprise.rag.shared.entity.User
 */
public sealed interface UserDto permits 
    UserDto.CreateUserRequest,
    UserDto.UpdateUserRequest,
    UserDto.UserResponse,
    UserDto.UserSummary,
    UserDto.LoginRequest,
    UserDto.LoginResponse {

    /**
     * Request DTO for creating a new user account.
     * <p>
     * Contains all required information to create a user within a specific tenant.
     * Users are automatically associated with the specified tenant and assigned the given role.
     * Email addresses must be unique across the entire system.
     * </p>
     * 
     * @param firstName user's first name (2-100 characters)
     * @param lastName user's last name (2-100 characters)
     * @param email unique email address for authentication and communication
     * @param password secure password (minimum 8 characters)
     * @param role user role within the tenant (ADMIN, USER, VIEWER)
     * @param tenantId UUID of the tenant this user belongs to
     */
    record CreateUserRequest(
        @NotBlank @Size(min = 2, max = 100) String firstName,
        @NotBlank @Size(min = 2, max = 100) String lastName,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password,
        User.UserRole role,
        UUID tenantId
    ) implements UserDto {}

    /**
     * Request DTO for updating an existing user's profile and settings.
     * <p>
     * All fields are optional - only provided fields will be updated.
     * Email changes may require re-verification. Role and status changes
     * typically require admin privileges.
     * </p>
     * 
     * @param firstName updated first name (2-100 characters, null to keep current)
     * @param lastName updated last name (2-100 characters, null to keep current)
     * @param email updated email address (null to keep current)
     * @param role updated role (null to keep current)
     * @param status updated account status (null to keep current)
     */
    record UpdateUserRequest(
        @Size(min = 2, max = 100) String firstName,
        @Size(min = 2, max = 100) String lastName,
        @Email String email,
        User.UserRole role,
        User.UserStatus status
    ) implements UserDto {}

    /**
     * Complete user response with full profile information and tenant details.
     * <p>
     * This comprehensive response includes all user information, authentication status,
     * and associated tenant details. Used for user profile pages and administrative interfaces.
     * Sensitive information like passwords are never included.
     * </p>
     * 
     * @param id unique user identifier
     * @param firstName user's first name
     * @param lastName user's last name
     * @param email user's email address
     * @param role user's role within their tenant
     * @param status current account status (ACTIVE, SUSPENDED, PENDING_VERIFICATION)
     * @param emailVerified whether the user's email address has been verified
     * @param lastLoginAt timestamp of the user's last successful login
     * @param createdAt user account creation timestamp
     * @param tenant summary of the tenant this user belongs to
     */
    record UserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        User.UserRole role,
        User.UserStatus status,
        Boolean emailVerified,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        TenantDto.TenantSummary tenant
    ) implements UserDto {}

    /**
     * Lightweight user summary for efficient operations and references.
     * <p>
     * Contains essential user information without heavy tenant details or timestamps,
     * optimized for user lists, document ownership display, and quick references.
     * </p>
     * 
     * @param id unique user identifier
     * @param firstName user's first name
     * @param lastName user's last name
     * @param email user's email address
     * @param role user's role within their tenant
     * @param status current account status
     */
    record UserSummary(
        UUID id,
        String firstName,
        String lastName,
        String email,
        User.UserRole role,
        User.UserStatus status
    ) implements UserDto {}

    /**
     * Request DTO for user authentication (login).
     * <p>
     * Contains credentials required for JWT token-based authentication.
     * Email addresses are case-insensitive for login purposes.
     * </p>
     * 
     * @param email user's registered email address
     * @param password user's password
     */
    record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
    ) implements UserDto {}

    /**
     * Response DTO for successful authentication containing JWT tokens and user information.
     * <p>
     * Provides both access and refresh tokens for stateless authentication.
     * The access token should be used for API requests, while the refresh token
     * can be used to obtain new access tokens without re-authentication.
     * </p>
     * 
     * @param accessToken JWT access token for API authentication
     * @param refreshToken JWT refresh token for token renewal
     * @param expiresIn access token expiration time in seconds
     * @param user complete user information and tenant details
     */
    record LoginResponse(
        String accessToken,
        String refreshToken,
        Long expiresIn,
        UserResponse user
    ) implements UserDto {}
}
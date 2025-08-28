package com.enterprise.rag.auth.controller;

import com.enterprise.rag.auth.security.JwtAuthenticationFilter;
import com.enterprise.rag.auth.service.UserService;
import com.enterprise.rag.shared.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for user management operations in the Enterprise RAG system.
 * 
 * <p>This controller provides comprehensive user management functionality with
 * strict tenant isolation and role-based access control:
 * <ul>
 *   <li>Self-service user profile management</li>
 *   <li>Administrative user creation and management</li>
 *   <li>Tenant-scoped user listing and search</li>
 *   <li>User lifecycle operations (create, update, delete)</li>
 * </ul>
 * 
 * <p><strong>Tenant Isolation:</strong>
 * All operations are strictly scoped to the authenticated user's tenant.
 * Users can only interact with other users within their organization,
 * ensuring complete data separation between tenants.
 * 
 * <p><strong>Access Control Levels:</strong>
 * <ul>
 *   <li><strong>Self-Service:</strong> Users can view and update their own profiles</li>
 *   <li><strong>User Role:</strong> Can view other users in same tenant</li>
 *   <li><strong>Admin Role:</strong> Full CRUD operations within tenant</li>
 * </ul>
 * 
 * <p><strong>Security Features:</strong>
 * <ul>
 *   <li>JWT-based authentication required for all endpoints</li>
 *   <li>Method-level security with SpEL expressions</li>
 *   <li>Automatic tenant isolation based on JWT claims</li>
 *   <li>Password hashing and secure credential handling</li>
 * </ul>
 * 
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * // Get current user profile
 * GET /api/v1/users/me
 * Authorization: Bearer <jwt-token>
 * 
 * // Update current user profile
 * PUT /api/v1/users/me
 * {
 *   "firstName": "John",
 *   "lastName": "Doe",
 *   "email": "john.doe@example.com"
 * }
 * }</pre>
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.0
 * @see UserService
 * @see UserDto
 * @see JwtAuthenticationFilter
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "User CRUD operations")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    /** Service for user management operations and business logic. */
    private final UserService userService;

    /**
     * Constructs a new UserController with required user service.
     * 
     * @param userService the user service for user operations
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Creates a new user account within the tenant (admin-only).
     * 
     * <p>This endpoint allows tenant administrators to create new user accounts
     * with specified roles and permissions. The created user will receive an
     * email verification link to activate their account.
     * 
     * <p><strong>Admin Capabilities:</strong>
     * <ul>
     *   <li>Set user roles (USER, ADMIN) during creation</li>
     *   <li>Create users with pre-verified email status</li>
     *   <li>Assign specific tenant permissions</li>
     * </ul>
     * 
     * <p><strong>Validation:</strong>
     * <ul>
     *   <li>Email must be unique within the tenant</li>
     *   <li>Password must meet complexity requirements</li>
     *   <li>All required profile fields must be provided</li>
     * </ul>
     * 
     * @param request the user creation request with profile and credential data
     * @return ResponseEntity containing the created user information with CREATED status
     * @throws org.springframework.security.access.AccessDeniedException if user lacks ADMIN role
     * @throws com.enterprise.rag.shared.exception.RagException if email already exists
     * @throws jakarta.validation.ValidationException if request data is invalid
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create user", description = "Create a new user (admin only)")
    public ResponseEntity<UserDto.UserResponse> createUser(@Valid @RequestBody UserDto.CreateUserRequest request) {
        UserDto.UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves the current authenticated user's profile information.
     * 
     * <p>This endpoint provides self-service access to user profile data.
     * Users can view their own profile information including personal details,
     * role assignments, and account status.
     * 
     * <p><strong>Returned Information:</strong>
     * <ul>
     *   <li>Personal profile data (name, email, etc.)</li>
     *   <li>Account status and verification state</li>
     *   <li>Role and permission assignments</li>
     *   <li>Tenant association information</li>
     * </ul>
     * 
     * <p><strong>Security:</strong> User information is automatically filtered
     * based on the JWT token claims, ensuring users can only access their own data.
     * 
     * @param principal the authenticated user principal extracted from JWT token
     * @return ResponseEntity containing the current user's profile information
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Get current authenticated user information")
    public ResponseEntity<UserDto.UserResponse> getCurrentUser(
            @AuthenticationPrincipal JwtAuthenticationFilter.RagUserPrincipal principal) {
        UserDto.UserResponse response = userService.getUser(principal.userId(), principal.tenantId());
        return ResponseEntity.ok(response);
    }

    /**
     * Updates the current authenticated user's profile information.
     * 
     * <p>This endpoint provides self-service profile management capabilities.
     * Users can update their personal information, preferences, and non-security
     * related settings. Security-sensitive changes may require additional verification.
     * 
     * <p><strong>Updateable Fields:</strong>
     * <ul>
     *   <li>Personal information (first name, last name)</li>
     *   <li>Contact preferences and settings</li>
     *   <li>Profile customization options</li>
     * </ul>
     * 
     * <p><strong>Restrictions:</strong>
     * <ul>
     *   <li>Email changes require verification process</li>
     *   <li>Role and permission changes require admin approval</li>
     *   <li>Password changes use separate security endpoint</li>
     * </ul>
     * 
     * @param request the update request containing modified profile data
     * @param principal the authenticated user principal from JWT token
     * @return ResponseEntity containing the updated user profile information
     * @throws jakarta.validation.ValidationException if update data is invalid
     */
    @PutMapping("/me")
    @Operation(summary = "Update current user", description = "Update current authenticated user information")
    public ResponseEntity<UserDto.UserResponse> updateCurrentUser(
            @Valid @RequestBody UserDto.UpdateUserRequest request,
            @AuthenticationPrincipal JwtAuthenticationFilter.RagUserPrincipal principal) {
        UserDto.UserResponse response = userService.updateUser(principal.userId(), request, principal.tenantId());
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves user information by user ID with tenant isolation.
     * 
     * <p>This endpoint enforces strict access control and tenant isolation:
     * <ul>
     *   <li><strong>Admins:</strong> Can access any user within their tenant</li>
     *   <li><strong>Users:</strong> Can only access users within same tenant (via SpEL)</li>
     * </ul>
     * 
     * <p><strong>Access Control:</strong> The PreAuthorize annotation uses a Spring
     * Expression Language (SpEL) expression to verify that the requested user
     * belongs to the same tenant as the authenticated user.
     * 
     * <p><strong>Tenant Security:</strong> All user lookups are automatically
     * scoped to the requesting user's tenant, preventing cross-tenant data access.
     * 
     * @param userId the UUID of the user to retrieve
     * @param principal the authenticated user principal containing tenant context
     * @return ResponseEntity containing the requested user information
     * @throws org.springframework.security.access.AccessDeniedException if access denied
     * @throws com.enterprise.rag.shared.exception.UserNotFoundException if user not found in tenant
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @userService.getUser(#userId, authentication.principal.tenantId).tenant.id == authentication.principal.tenantId")
    @Operation(summary = "Get user by ID", description = "Retrieve user information")
    public ResponseEntity<UserDto.UserResponse> getUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal JwtAuthenticationFilter.RagUserPrincipal principal) {
        UserDto.UserResponse response = userService.getUser(userId, principal.tenantId());
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves paginated list of users within the current tenant.
     * 
     * <p>This endpoint provides tenant-scoped user directory functionality.
     * Results are automatically filtered to include only users within the
     * authenticated user's tenant, ensuring complete tenant isolation.
     * 
     * <p><strong>Access Permissions:</strong>
     * <ul>
     *   <li><strong>Admins:</strong> Can see full user details and management options</li>
     *   <li><strong>Users:</strong> Can see basic user directory for collaboration</li>
     * </ul>
     * 
     * <p><strong>Pagination Support:</strong>
     * <ul>
     *   <li><code>page</code>: Page number (0-based, default: 0)</li>
     *   <li><code>size</code>: Page size (default: 20, max: 100)</li>
     *   <li><code>sort</code>: Sort criteria (e.g., "lastName,asc")</li>
     * </ul>
     * 
     * <p><strong>Use Cases:</strong>
     * <ul>
     *   <li>User directory for team collaboration</li>
     *   <li>Administrative user management interface</li>
     *   <li>User search and selection for permissions</li>
     * </ul>
     * 
     * @param pageable the pagination and sorting parameters
     * @param principal the authenticated user principal containing tenant context
     * @return ResponseEntity containing paginated user summaries
     * @throws org.springframework.security.access.AccessDeniedException if user lacks required role
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    @Operation(summary = "List users", description = "List users in tenant")
    public ResponseEntity<Page<UserDto.UserSummary>> getUsers(
            Pageable pageable,
            @AuthenticationPrincipal JwtAuthenticationFilter.RagUserPrincipal principal) {
        Page<UserDto.UserSummary> response = userService.getUsersByTenant(principal.tenantId(), pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates user information and settings (admin-only).
     * 
     * <p>This endpoint allows tenant administrators to modify user accounts
     * including profile information, role assignments, and account status.
     * Updates are applied within the tenant scope to maintain isolation.
     * 
     * <p><strong>Admin Update Capabilities:</strong>
     * <ul>
     *   <li>Profile information (name, email, contact details)</li>
     *   <li>Role assignments (USER, ADMIN) within tenant</li>
     *   <li>Account status (active, suspended, pending verification)</li>
     *   <li>Permissions and feature access settings</li>
     * </ul>
     * 
     * <p><strong>Validation and Security:</strong>
     * <ul>
     *   <li>Updates are validated for data integrity</li>
     *   <li>Email changes trigger verification process</li>
     *   <li>Role changes are audited for security</li>
     *   <li>User is notified of significant account changes</li>
     * </ul>
     * 
     * @param userId the UUID of the user to update
     * @param request the update request containing modified user data
     * @param principal the authenticated admin principal
     * @return ResponseEntity containing the updated user information
     * @throws org.springframework.security.access.AccessDeniedException if user lacks ADMIN role
     * @throws com.enterprise.rag.shared.exception.UserNotFoundException if user not found in tenant
     * @throws jakarta.validation.ValidationException if update data is invalid
     */
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user", description = "Update user information (admin only)")
    public ResponseEntity<UserDto.UserResponse> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UserDto.UpdateUserRequest request,
            @AuthenticationPrincipal JwtAuthenticationFilter.RagUserPrincipal principal) {
        UserDto.UserResponse response = userService.updateUser(userId, request, principal.tenantId());
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a user account and associated data (admin-only).
     * 
     * <p><strong>⚠️ WARNING:</strong> This operation will permanently delete
     * the user account and associated personal data. The following data will be removed:
     * <ul>
     *   <li>User profile and authentication credentials</li>
     *   <li>User's document uploads and personal files</li>
     *   <li>User's conversation history and preferences</li>
     *   <li>User's API access tokens and sessions</li>
     * </ul>
     * 
     * <p><strong>Data Retention:</strong> Some audit and system logs may be
     * retained for compliance purposes, but personally identifiable information
     * will be anonymized or removed according to data protection policies.
     * 
     * <p><strong>Prerequisites:</strong>
     * <ul>
     *   <li>User account should be suspended before deletion</li>
     *   <li>Data export should be completed if required</li>
     *   <li>User should be notified of pending deletion</li>
     * </ul>
     * 
     * <p><strong>Tenant Scope:</strong> Deletion is scoped to the admin's tenant,
     * ensuring admins cannot delete users from other organizations.
     * 
     * @param userId the UUID of the user to delete
     * @param principal the authenticated admin principal
     * @return ResponseEntity with 204 No Content status on successful deletion
     * @throws org.springframework.security.access.AccessDeniedException if user lacks ADMIN role
     * @throws com.enterprise.rag.shared.exception.UserNotFoundException if user not found in tenant
     * @throws com.enterprise.rag.shared.exception.RagException if user has active dependencies
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user", description = "Delete user (admin only)")
    public ResponseEntity<Void> deleteUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal JwtAuthenticationFilter.RagUserPrincipal principal) {
        userService.deleteUser(userId, principal.tenantId());
        return ResponseEntity.noContent().build();
    }
}
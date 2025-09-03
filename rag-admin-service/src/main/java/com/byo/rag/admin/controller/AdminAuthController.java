package com.byo.rag.admin.controller;

import com.byo.rag.admin.dto.AdminLoginRequest;
import com.byo.rag.admin.dto.AdminLoginResponse;
import com.byo.rag.admin.dto.AdminRefreshRequest;
import com.byo.rag.admin.dto.AdminUserValidationResponse;
import com.byo.rag.admin.dto.LogoutResponse;
import com.byo.rag.admin.service.AdminJwtService;
import com.byo.rag.shared.entity.User;
import com.byo.rag.admin.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for administrative authentication and authorization operations.
 * 
 * <p>This controller provides comprehensive authentication services for administrative users
 * of the Enterprise RAG system, including secure login, token management, and user validation.
 * It implements role-based access control to ensure only authorized administrators can access
 * system management features.
 * 
 * <p><strong>Authentication Architecture:</strong>
 * <ul>
 *   <li><strong>Database Authentication:</strong> Credentials validated against user database</li>
 *   <li><strong>JWT Token Generation:</strong> Secure tokens for stateless authentication</li>
 *   <li><strong>Role-Based Access:</strong> ADMIN role requirement for all operations</li>
 *   <li><strong>Account Status Validation:</strong> Only active users can authenticate</li>
 * </ul>
 * 
 * <p><strong>Security Features:</strong>
 * <ul>
 *   <li><strong>BCrypt Password Verification:</strong> Secure password hashing validation</li>
 *   <li><strong>Token Refresh:</strong> Secure token renewal without re-authentication</li>
 *   <li><strong>Comprehensive Logging:</strong> Security events logged for monitoring</li>
 *   <li><strong>Error Handling:</strong> Secure error responses without information leakage</li>
 * </ul>
 * 
 * <p><strong>API Endpoints:</strong>
 * <ul>
 *   <li><strong>POST /auth/login:</strong> Administrative user authentication</li>
 *   <li><strong>POST /auth/refresh:</strong> JWT token refresh</li>
 *   <li><strong>POST /auth/logout:</strong> Administrative logout</li>
 *   <li><strong>GET /auth/validate:</strong> User existence and role validation</li>
 *   <li><strong>GET /auth/me:</strong> Current authenticated user information</li>
 * </ul>
 * 
 * <p><strong>Administrative Access Control:</strong>
 * <ul>
 *   <li>Only users with ADMIN role can authenticate through this controller</li>
 *   <li>Account must have ACTIVE status for successful authentication</li>
 *   <li>Failed authentication attempts are logged for security monitoring</li>
 *   <li>JWT tokens contain role information for downstream authorization</li>
 * </ul>
 * 
 * <p><strong>Integration Points:</strong>
 * <ul>
 *   <li><strong>AdminJwtService:</strong> Token generation and validation</li>
 *   <li><strong>UserRepository:</strong> Database user lookup and validation</li>
 *   <li><strong>PasswordEncoder:</strong> BCrypt password verification</li>
 *   <li><strong>Spring Security:</strong> Authentication context integration</li>
 * </ul>
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.0
 * @see AdminJwtService
 * @see UserRepository
 * @see AdminLoginRequest
 * @see AdminLoginResponse
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Admin Authentication", description = "Admin authentication and authorization endpoints")
@Validated
public class AdminAuthController {

    /** Logger for administrative authentication events and security monitoring. */
    private static final Logger logger = LoggerFactory.getLogger(AdminAuthController.class);

    /** Service for JWT token generation and validation operations. */
    private final AdminJwtService jwtService;
    
    /** Password encoder for BCrypt password verification. */
    private final PasswordEncoder passwordEncoder;
    
    /** Repository for user database access and validation. */
    private final UserRepository userRepository;

    /**
     * Constructs an administrative authentication controller.
     * 
     * @param jwtService service for JWT token operations
     * @param passwordEncoder encoder for password verification
     * @param userRepository repository for user data access
     */
    public AdminAuthController(AdminJwtService jwtService, PasswordEncoder passwordEncoder, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    /**
     * Authenticates administrative users and generates JWT tokens for secure access.
     * 
     * <p>This endpoint provides comprehensive authentication for administrative users,
     * validating credentials against the database and enforcing strict access controls.
     * Only users with ADMIN role and ACTIVE status can successfully authenticate.
     * 
     * <p><strong>Authentication Process:</strong>
     * <ol>
     *   <li><strong>User Lookup:</strong> Locate user by email in database</li>
     *   <li><strong>Password Verification:</strong> BCrypt password validation</li>
     *   <li><strong>Status Check:</strong> Verify account is ACTIVE</li>
     *   <li><strong>Role Validation:</strong> Confirm user has ADMIN role</li>
     *   <li><strong>Token Generation:</strong> Create JWT with admin privileges</li>
     * </ol>
     * 
     * <p><strong>Security Features:</strong>
     * <ul>
     *   <li><strong>Database Validation:</strong> Real-time credential verification</li>
     *   <li><strong>Role Enforcement:</strong> Strict ADMIN role requirement</li>
     *   <li><strong>Account Status:</strong> Active account status validation</li>
     *   <li><strong>Secure Logging:</strong> Authentication attempts logged for monitoring</li>
     * </ul>
     * 
     * <p><strong>Token Properties:</strong>
     * <ul>
     *   <li><strong>Validity:</strong> 24 hours (86400000 milliseconds)</li>
     *   <li><strong>Claims:</strong> Username and ADMIN role included</li>
     *   <li><strong>Format:</strong> Standard JWT with signature verification</li>
     * </ul>
     * 
     * <p><strong>Error Responses:</strong>
     * <ul>
     *   <li><strong>401 Unauthorized:</strong> Invalid credentials or inactive account</li>
     *   <li><strong>403 Forbidden:</strong> Valid user but insufficient privileges</li>
     *   <li><strong>500 Internal Server Error:</strong> System error during authentication</li>
     * </ul>
     * 
     * @param request the admin login request containing username and password
     * @return ResponseEntity containing JWT token and user info, or error details
     */
    @PostMapping("/login")
    @Operation(summary = "Admin login", description = "Authenticate admin user and return JWT token")
    public ResponseEntity<?> login(@Valid @RequestBody AdminLoginRequest request) {
        try {
            logger.debug("Admin login attempt for username: {}", request.username());
            
            // Find user in database
            Optional<User> userOpt = userRepository.findByEmail(request.username());
            if (userOpt.isEmpty()) {
                logger.warn("User not found for username: {}", request.username());
                return ResponseEntity.status(401)
                        .body(Map.of(
                                "error", "Invalid credentials",
                                "message", "Username or password is incorrect"
                        ));
            }
            
            User user = userOpt.get();
            
            // Validate password using BCrypt
            boolean passwordMatches = passwordEncoder.matches(request.password(), user.getPasswordHash());
            
            if (!passwordMatches) {
                logger.warn("Invalid password for username: {}", request.username());
                return ResponseEntity.status(401)
                        .body(Map.of(
                                "error", "Invalid credentials",
                                "message", "Username or password is incorrect"
                        ));
            }
            
            // Check if user account is active
            if (!user.getStatus().name().equals("ACTIVE")) {
                logger.warn("Inactive user login attempt: {}", request.username());
                return ResponseEntity.status(401)
                        .body(Map.of(
                                "error", "Account disabled",
                                "message", "Your account is not active"
                        ));
            }
            
            // Verify user has admin role
            if (!user.getRole().name().equals("ADMIN")) {
                logger.warn("Non-admin user login attempt: {}", request.username());
                return ResponseEntity.status(403)
                        .body(Map.of(
                                "error", "Access denied",
                                "message", "Admin access required"
                        ));
            }

            // Generate JWT token with admin role
            List<String> roles = List.of("ADMIN");
            String token = jwtService.generateToken(request.username(), roles);
            
            logger.info("Admin user successfully authenticated: {}", request.username());
            
            AdminLoginResponse response = new AdminLoginResponse(
                    token,
                    request.username(),
                    roles,
                    86400000L // 24 hours in milliseconds
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error during admin login", e);
            return ResponseEntity.status(500)
                    .body(Map.of(
                            "error", "Internal server error",
                            "message", "An error occurred during authentication"
                    ));
        }
    }

    /**
     * Refreshes expired or soon-to-expire JWT tokens for continued admin access.
     * 
     * <p>This endpoint provides secure token renewal for administrative users,
     * allowing continued access without requiring re-authentication. The refresh
     * process validates the existing token and generates a new one with updated
     * expiration time while preserving user identity and role information.
     * 
     * <p><strong>Token Refresh Process:</strong>
     * <ol>
     *   <li><strong>Token Validation:</strong> Verify current token signature and structure</li>
     *   <li><strong>Claims Extraction:</strong> Retrieve username and roles from existing token</li>
     *   <li><strong>New Token Generation:</strong> Create fresh token with same claims</li>
     *   <li><strong>Response Creation:</strong> Return new token with full admin context</li>
     * </ol>
     * 
     * <p><strong>Security Considerations:</strong>
     * <ul>
     *   <li><strong>Token Validation:</strong> Existing token must be structurally valid</li>
     *   <li><strong>Signature Verification:</strong> Cryptographic validation of token authenticity</li>
     *   <li><strong>Claim Preservation:</strong> User identity and roles maintained in new token</li>
     *   <li><strong>Secure Logging:</strong> Refresh operations logged for security monitoring</li>
     * </ul>
     * 
     * <p><strong>Use Cases:</strong>
     * <ul>
     *   <li>Automatic token renewal before expiration</li>
     *   <li>Extending admin sessions for active users</li>
     *   <li>Maintaining authentication in long-running admin operations</li>
     * </ul>
     * 
     * <p><strong>Error Handling:</strong>
     * <ul>
     *   <li><strong>401 Unauthorized:</strong> Invalid, expired, or malformed token</li>
     *   <li><strong>500 Internal Server Error:</strong> System error during token generation</li>
     * </ul>
     * 
     * @param request the refresh request containing the current JWT token
     * @return ResponseEntity containing new JWT token and user info, or error details
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh JWT token", description = "Refresh an existing JWT token")
    public ResponseEntity<?> refresh(@Valid @RequestBody AdminRefreshRequest request) {
        try {
            logger.debug("Token refresh attempt");
            
            // Validate existing token structure and signature
            if (!jwtService.isTokenValid(request.token())) {
                logger.warn("Invalid token provided for refresh");
                return ResponseEntity.status(401)
                        .body(Map.of(
                                "error", "Invalid token",
                                "message", "Token is invalid or expired"
                        ));
            }

            // Extract user identity and roles from current token
            String username = jwtService.extractUsername(request.token());
            List<String> roles = jwtService.extractRoles(request.token());
            
            // Generate new token with same claims but fresh expiration
            String newToken = jwtService.generateToken(username, roles);
            
            logger.info("Token refreshed successfully for user: {}", username);
            
            AdminLoginResponse response = new AdminLoginResponse(
                    newToken,
                    username,
                    roles,
                    86400000L // 24 hours validity
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error during token refresh", e);
            return ResponseEntity.status(500)
                    .body(Map.of(
                            "error", "Internal server error",
                            "message", "An error occurred during token refresh"
                    ));
        }
    }

    /**
     * Logs out administrative users and provides logout confirmation.
     * 
     * <p>This endpoint handles administrative logout operations, providing a clean
     * logout flow for admin users. In a stateless JWT architecture, logout is primarily
     * a client-side operation where tokens are discarded, but this endpoint provides
     * server-side logging and confirmation for audit purposes.
     * 
     * <p><strong>Logout Process:</strong>
     * <ul>
     *   <li><strong>Logout Logging:</strong> Server-side logging for security audit</li>
     *   <li><strong>Confirmation Response:</strong> Success confirmation to client</li>
     *   <li><strong>Client Responsibility:</strong> Token disposal handled client-side</li>
     * </ul>
     * 
     * <p><strong>Stateless Architecture:</strong>
     * In JWT-based systems, tokens are self-contained and validated without server state.
     * Actual logout is achieved by clients discarding tokens, but this endpoint:
     * <ul>
     *   <li>Provides audit trail for administrative logout events</li>
     *   <li>Offers confirmation to admin interfaces</li>
     *   <li>Supports future token blacklisting implementations</li>
     * </ul>
     * 
     * @return ResponseEntity containing logout confirmation message
     */
    @PostMapping("/logout")
    @Operation(summary = "Admin logout", description = "Logout admin user")
    public ResponseEntity<LogoutResponse> logout() {
        logger.info("Admin user logged out");
        return ResponseEntity.ok(new LogoutResponse("Logged out successfully"));
    }

    /**
     * Validates the existence and administrative privileges of a user account.
     * 
     * <p>This endpoint provides user validation services for administrative interfaces,
     * allowing systems to verify if a given username corresponds to an active
     * administrative user before presenting admin-specific features or workflows.
     * 
     * <p><strong>Validation Criteria:</strong>
     * A user is considered a valid admin if ALL conditions are met:
     * <ul>
     *   <li><strong>User Exists:</strong> Account found in database by email</li>
     *   <li><strong>Admin Role:</strong> User has ADMIN role assigned</li>
     *   <li><strong>Active Status:</strong> Account status is ACTIVE</li>
     * </ul>
     * 
     * <p><strong>Use Cases:</strong>
     * <ul>
     *   <li><strong>UI Enhancement:</strong> Show/hide admin features based on user status</li>
     *   <li><strong>Pre-Authentication:</strong> Validate admin accounts before login attempts</li>
     *   <li><strong>Access Control:</strong> Verify admin privileges for sensitive operations</li>
     *   <li><strong>Integration Support:</strong> External systems validating admin users</li>
     * </ul>
     * 
     * <p><strong>Security Considerations:</strong>
     * <ul>
     *   <li>This endpoint reveals user existence but not detailed account information</li>
     *   <li>Should be used in conjunction with proper authentication flows</li>
     *   <li>Validation results are logged for security monitoring</li>
     * </ul>
     * 
     * @param username the username (email) to validate for admin privileges
     * @return AdminUserValidationResponse indicating if user is a valid admin
     */
    @GetMapping("/validate")
    @Operation(summary = "Validate admin user", description = "Check if admin user exists")
    public ResponseEntity<AdminUserValidationResponse> validateUser(@RequestParam String username) {
        Optional<User> userOpt = userRepository.findByEmail(username);
        boolean exists = userOpt.isPresent() && 
                         userOpt.get().getRole().name().equals("ADMIN") &&
                         userOpt.get().getStatus().name().equals("ACTIVE");
        logger.debug("Admin user validation for {}: {}", username, exists);
        
        return ResponseEntity.ok(new AdminUserValidationResponse(exists, username));
    }

    /**
     * Retrieves information about the currently authenticated administrative user.
     * 
     * <p>This endpoint provides user context information for authenticated admin users,
     * extracting details from the Spring Security authentication context. It's commonly
     * used by admin interfaces to display current user information and role context.
     * 
     * <p><strong>Returned Information:</strong>
     * <ul>
     *   <li><strong>Username:</strong> The authenticated user's identifier (email)</li>
     *   <li><strong>Roles:</strong> List of assigned roles (typically ["ADMIN"])</li>
     * </ul>
     * 
     * <p><strong>Authentication Context:</strong>
     * This endpoint relies on Spring Security's authentication context, which is
     * populated by JWT authentication filters. The information is extracted from:
     * <ul>
     *   <li><strong>Principal:</strong> User identity from JWT token claims</li>
     *   <li><strong>Authorities:</strong> Granted authorities from JWT role claims</li>
     * </ul>
     * 
     * <p><strong>Security Features:</strong>
     * <ul>
     *   <li><strong>Authentication Required:</strong> Returns 401 if not authenticated</li>
     *   <li><strong>Real-time Context:</strong> Information reflects current JWT token</li>
     *   <li><strong>Role Processing:</strong> Strips Spring Security ROLE_ prefix</li>
     *   <li><strong>Secure Logging:</strong> User info requests logged for monitoring</li>
     * </ul>
     * 
     * <p><strong>Common Use Cases:</strong>
     * <ul>
     *   <li>Admin dashboard user display</li>
     *   <li>Role-based UI feature toggling</li>
     *   <li>User context for audit logging</li>
     *   <li>Profile information display</li>
     * </ul>
     * 
     * @param authentication the Spring Security authentication context
     * @return ResponseEntity containing user information or error details
     */
    @GetMapping("/me")
    @Operation(summary = "Get current admin user", description = "Get current authenticated admin user information")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        // Verify authentication context exists and is valid
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(Map.of(
                            "error", "Unauthorized",
                            "message", "Authentication required"
                    ));
        }

        // Extract user identity from authentication principal
        String username = authentication.getName();
        
        // Process granted authorities to extract clean role names
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role)
                .toList();

        // Build user information response
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("username", username);
        userInfo.put("roles", roles);
        
        logger.debug("Current admin user info requested: {}", username);
        
        return ResponseEntity.ok(userInfo);
    }
}
package com.enterprise.rag.auth.controller;

import com.enterprise.rag.auth.service.AuthService;
import com.enterprise.rag.auth.service.UserService;
import com.enterprise.rag.shared.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for authentication operations in the Enterprise RAG system.
 * 
 * <p>This controller provides comprehensive authentication endpoints including:
 * <ul>
 *   <li>User login with JWT token generation</li>
 *   <li>JWT token refresh and validation</li>
 *   <li>Public user registration</li>
 *   <li>Email verification for new users</li>
 * </ul>
 * 
 * <p>All endpoints follow REST conventions and return appropriate HTTP status codes.
 * Authentication is handled through JWT tokens with configurable expiration times.
 * 
 * <p><strong>Security Considerations:</strong>
 * <ul>
 *   <li>Registration endpoint is publicly accessible</li>
 *   <li>Login attempts are rate-limited per IP</li>
 *   <li>JWT tokens include tenant isolation claims</li>
 *   <li>Refresh tokens have longer expiration than access tokens</li>
 * </ul>
 * 
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * // Login request
 * POST /api/v1/auth/login
 * {
 *   "email": "user@example.com",
 *   "password": "securePassword123"
 * }
 * 
 * // Response includes access and refresh tokens
 * {
 *   "accessToken": "eyJ0eXAi...",
 *   "refreshToken": "eyJ0eXAi...",
 *   "user": { ... }
 * }
 * }</pre>
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.0
 * @see AuthService
 * @see UserService
 * @see UserDto
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication and token management")
public class AuthController {

    /** Service for authentication operations including login, token management, and validation. */
    private final AuthService authService;
    
    /** Service for user management operations including registration and email verification. */
    private final UserService userService;

    /**
     * Constructs a new AuthController with required services.
     * 
     * @param authService the authentication service for login and token operations
     * @param userService the user service for registration and user management
     */
    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    /**
     * Authenticates a user and returns JWT tokens for API access.
     * 
     * <p>This endpoint validates user credentials against the database and generates
     * both access and refresh JWT tokens. The access token is used for API calls,
     * while the refresh token can be used to obtain new access tokens.
     * 
     * <p><strong>Rate Limiting:</strong> This endpoint is rate-limited to prevent
     * brute force attacks. Excessive failed attempts may result in temporary lockout.
     * 
     * <p><strong>Security:</strong>
     * <ul>
     *   <li>Passwords are verified using BCrypt hashing</li>
     *   <li>Tokens include tenant isolation claims</li>
     *   <li>Failed attempts are logged for security monitoring</li>
     * </ul>
     * 
     * @param request the login request containing email and password
     * @return ResponseEntity containing login response with JWT tokens and user info
     * @throws org.springframework.security.authentication.BadCredentialsException if credentials are invalid
     * @throws com.enterprise.rag.shared.exception.UserNotFoundException if user does not exist
     */
    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT tokens")
    public ResponseEntity<UserDto.LoginResponse> login(@Valid @RequestBody UserDto.LoginRequest request) {
        UserDto.LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Generates a new access token using a valid refresh token.
     * 
     * <p>This endpoint allows clients to obtain new access tokens without
     * requiring the user to log in again. Refresh tokens have longer expiration
     * times than access tokens but are single-use.
     * 
     * <p><strong>Token Rotation:</strong> Each refresh operation invalidates the
     * used refresh token and returns a new one for enhanced security.
     * 
     * @param request the refresh token request containing the refresh token
     * @return ResponseEntity containing new JWT tokens
     * @throws com.enterprise.rag.shared.exception.RagException if refresh token is invalid or expired
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Generate new access token using refresh token")
    public ResponseEntity<UserDto.LoginResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        UserDto.LoginResponse response = authService.refreshToken(request.refreshToken());
        return ResponseEntity.ok(response);
    }

    /**
     * Validates a JWT token for authenticity and expiration.
     * 
     * <p>This endpoint is primarily used by other microservices in the RAG system
     * to verify the validity of JWT tokens before processing requests. It checks
     * both token structure and expiration without requiring database lookups.
     * 
     * <p><strong>Performance:</strong> Token validation is performed using cryptographic
     * signature verification, making it fast and suitable for high-frequency calls.
     * 
     * @param request the token validation request containing the JWT token to validate
     * @return ResponseEntity containing validation result (true if valid, false otherwise)
     */
    @PostMapping("/validate")
    @Operation(summary = "Validate token", description = "Validate JWT token")
    public ResponseEntity<TokenValidationResponse> validateToken(@RequestBody ValidateTokenRequest request) {
        boolean isValid = authService.validateToken(request.token());
        return ResponseEntity.ok(new TokenValidationResponse(isValid));
    }

    /**
     * Registers a new user in the system (public endpoint).
     * 
     * <p>This is a public endpoint that allows new users to create accounts.
     * Upon successful registration, an email verification token is sent to
     * the provided email address. The account remains inactive until verified.
     * 
     * <p><strong>Validation:</strong>
     * <ul>
     *   <li>Email address must be unique across all tenants</li>
     *   <li>Password must meet complexity requirements</li>
     *   <li>All required fields must be provided</li>
     * </ul>
     * 
     * <p><strong>Security:</strong>
     * <ul>
     *   <li>Passwords are hashed using BCrypt before storage</li>
     *   <li>Rate limiting prevents automated account creation</li>
     *   <li>Email verification is required for activation</li>
     * </ul>
     * 
     * @param request the user registration request with email, password, and profile info
     * @return ResponseEntity containing the created user information (password excluded)
     * @throws com.enterprise.rag.shared.exception.RagException if email already exists
     * @throws jakarta.validation.ValidationException if request data is invalid
     */
    @PostMapping("/register")
    @Operation(summary = "User registration", description = "Register a new user (public endpoint)")
    public ResponseEntity<UserDto.UserResponse> register(@Valid @RequestBody UserDto.CreateUserRequest request) {
        UserDto.UserResponse response = userService.createUser(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Verifies a user's email address using the verification token.
     * 
     * <p>This endpoint processes email verification tokens sent to users during
     * registration. Successful verification activates the user account and allows
     * login. Tokens are single-use and have limited validity periods.
     * 
     * <p><strong>Token Properties:</strong>
     * <ul>
     *   <li>Single-use: Tokens are invalidated after successful verification</li>
     *   <li>Time-limited: Tokens expire after 24 hours</li>
     *   <li>Secure: Tokens are cryptographically signed</li>
     * </ul>
     * 
     * @param request the email verification request containing the verification token
     * @return ResponseEntity containing the verified user information
     * @throws com.enterprise.rag.shared.exception.RagException if token is invalid or expired
     * @throws com.enterprise.rag.shared.exception.UserNotFoundException if associated user not found
     */
    @PostMapping("/verify-email")
    @Operation(summary = "Verify email", description = "Verify user email with token")
    public ResponseEntity<UserDto.UserResponse> verifyEmail(@RequestBody EmailVerificationRequest request) {
        UserDto.UserResponse response = userService.verifyEmail(request.token());
        return ResponseEntity.ok(response);
    }

    /**
     * Request record for token refresh operations.
     * 
     * @param refreshToken the JWT refresh token to use for generating new access tokens
     */
    public record RefreshTokenRequest(String refreshToken) {}
    
    /**
     * Request record for token validation operations.
     * 
     * @param token the JWT token to validate
     */
    public record ValidateTokenRequest(String token) {}
    
    /**
     * Response record for token validation results.
     * 
     * @param valid true if the token is valid and not expired, false otherwise
     */
    public record TokenValidationResponse(boolean valid) {}
    
    /**
     * Request record for email verification operations.
     * 
     * @param token the email verification token received via email
     */
    public record EmailVerificationRequest(String token) {}
}
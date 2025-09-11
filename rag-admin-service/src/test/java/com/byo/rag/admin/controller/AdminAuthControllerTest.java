package com.byo.rag.admin.controller;

import com.byo.rag.admin.dto.AdminLoginRequest;
import com.byo.rag.admin.dto.AdminLoginResponse;
import com.byo.rag.admin.dto.AdminRefreshRequest;
import com.byo.rag.admin.dto.AdminUserValidationResponse;
import com.byo.rag.admin.dto.LogoutResponse;
import com.byo.rag.admin.service.AdminJwtService;
import com.byo.rag.admin.repository.UserRepository;
import com.byo.rag.shared.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for AdminAuthController.
 * 
 * <p>Tests the authentication and authorization functionality for admin users,
 * including login, token refresh, logout, user validation, and current user information
 * retrieval. Uses comprehensive mocking to isolate controller logic from dependencies.</p>
 * 
 * <p>Key Test Coverage:</p>
 * <ul>
 *   <li>Authentication flows (login, logout, token refresh)</li>
 *   <li>Authorization validation and role-based access</li>
 *   <li>Error handling for invalid credentials and tokens</li>
 *   <li>User validation and information retrieval</li>
 *   <li>Edge cases with null/invalid inputs</li>
 * </ul>
 * 
 * <p><strong>Testing Best Practices Applied:</strong></p>
 * <ul>
 *   <li>✅ AssertJ descriptive assertions with detailed failure messages</li>
 *   <li>✅ @DisplayName annotations for clear test intent</li>
 *   <li>✅ Comprehensive Javadoc documentation</li>
 *   <li>✅ Public API testing without reflection usage</li>
 *   <li>✅ Realistic test data representative of production scenarios</li>
 * </ul>
 * 
 * @author RAG Admin Service Team
 * @since 1.0.0
 * @version 1.2.0 - Updated with enterprise testing best practices
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAuthController Unit Tests")
class AdminAuthControllerTest {

    @Mock
    private AdminJwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AdminAuthController controller;

    private final String validUsername = "admin@enterprise.com";
    private final String validPassword = "AdminPassword123!";
    private final String validToken = "valid.jwt.token";

    /**
     * Reset all mocks before each test to ensure test isolation.
     * This prevents test interdependencies and ensures each test starts with a clean state.
     */
    @BeforeEach
    void setUp() {
        reset(jwtService, passwordEncoder, userRepository, authentication);
    }

    /**
     * Tests successful admin login with valid credentials.
     * 
     * <p>Validates the complete login flow including:</p>
     * <ul>
     *   <li>User lookup by email</li>
     *   <li>Password verification using BCrypt encoder</li>
     *   <li>JWT token generation with proper roles</li>
     *   <li>Response structure with all required fields</li>
     * </ul>
     */
    @Test
    @DisplayName("Should login successfully with valid admin credentials and return JWT token")
    void shouldLoginSuccessfullyWithValidCredentials() {
        // Given
        AdminLoginRequest request = new AdminLoginRequest(validUsername, validPassword);
        User validUser = createValidAdminUser();
        when(userRepository.findByEmail(validUsername)).thenReturn(Optional.of(validUser));
        when(passwordEncoder.matches(validPassword, encodedPassword())).thenReturn(true);
        when(jwtService.generateToken(validUsername, List.of("ADMIN"))).thenReturn(validToken);

        // When
        ResponseEntity<?> response = controller.login(request);

        // Then
        assertThat(response.getStatusCode())
            .describedAs("Login should return HTTP 200 OK for valid admin credentials")
            .isEqualTo(HttpStatus.OK);
            
        AdminLoginResponse loginResponse = (AdminLoginResponse) response.getBody();
        assertThat(loginResponse)
            .describedAs("Login response body should not be null for successful authentication")
            .isNotNull();
        assertThat(loginResponse.token())
            .describedAs("Login response should contain the generated JWT token for session management")
            .isEqualTo(validToken);
        assertThat(loginResponse.username())
            .describedAs("Login response should return the authenticated admin username")
            .isEqualTo(validUsername);
        assertThat(loginResponse.roles())
            .describedAs("Login response should include admin roles for authorization")
            .containsExactly("ADMIN");
        assertThat(loginResponse.expiresIn())
            .describedAs("Login response should specify token expiration time for client-side management")
            .isEqualTo(86400000L);

        verify(userRepository).findByEmail(validUsername);
        verify(passwordEncoder).matches(validPassword, encodedPassword());
        verify(jwtService).generateToken(validUsername, List.of("ADMIN"));
    }

    /**
     * Tests authentication failure with incorrect password.
     * 
     * <p>Validates security behavior when:</p>
     * <ul>
     *   <li>User exists but password is incorrect</li>
     *   <li>Password encoder returns false for mismatch</li>
     *   <li>No JWT token is generated for failed authentication</li>
     *   <li>Generic error message prevents user enumeration</li>
     * </ul>
     */
    @Test
    @DisplayName("Should return HTTP 401 Unauthorized with descriptive error for invalid password")
    void shouldReturn401WithInvalidCredentials() {
        // Given
        AdminLoginRequest request = new AdminLoginRequest(validUsername, "WrongPassword");
        User validUser = createValidAdminUser();
        when(userRepository.findByEmail(validUsername)).thenReturn(Optional.of(validUser));
        when(passwordEncoder.matches("WrongPassword", encodedPassword())).thenReturn(false);

        // When
        ResponseEntity<?> response = controller.login(request);

        // Then
        assertThat(response.getStatusCode())
            .describedAs("Authentication should fail with HTTP 401 for incorrect password")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
        
        @SuppressWarnings("unchecked")
        Map<String, String> errorResponse = (Map<String, String>) response.getBody();
        assertThat(errorResponse)
            .describedAs("Error response should provide details for failed authentication")
            .isNotNull();
        assertThat(errorResponse.get("error"))
            .describedAs("Error response should contain 'Invalid credentials' to prevent user enumeration")
            .isEqualTo("Invalid credentials");
        assertThat(errorResponse.get("message"))
            .describedAs("Error response should include user-friendly message for debugging")
            .isNotNull();

        verify(userRepository).findByEmail(validUsername);
        verify(passwordEncoder).matches("WrongPassword", encodedPassword());
        verifyNoInteractions(jwtService);
    }

    /**
     * Tests authentication failure with non-existent user.
     * 
     * <p>Validates security behavior when:</p>
     * <ul>
     *   <li>Email address is not found in user repository</li>
     *   <li>Password encoding is skipped for non-existent users</li>
     *   <li>Generic error message prevents user enumeration attacks</li>
     * </ul>
     */
    @Test
    @DisplayName("Should return HTTP 401 Unauthorized for non-existent admin user email")
    void shouldReturn401WithNonExistentUsername() {
        // Given
        String nonExistentUser = "nonexistent@enterprise.com";
        AdminLoginRequest request = new AdminLoginRequest(nonExistentUser, validPassword);
        when(userRepository.findByEmail(nonExistentUser)).thenReturn(Optional.empty());

        // When
        ResponseEntity<?> response = controller.login(request);

        // Then
        assertThat(response.getStatusCode())
            .describedAs("Authentication should fail with HTTP 401 for non-existent user")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
        
        @SuppressWarnings("unchecked")
        Map<String, String> errorResponse = (Map<String, String>) response.getBody();
        assertThat(errorResponse)
            .describedAs("Error response should be provided for non-existent user")
            .isNotNull();
        assertThat(errorResponse.get("error"))
            .describedAs("Error message should be generic to prevent user enumeration attacks")
            .isEqualTo("Invalid credentials");

        verify(userRepository).findByEmail(nonExistentUser);
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    /**
     * Tests successful JWT token refresh with valid token.
     * 
     * <p>Validates the token refresh flow including:</p>
     * <ul>
     *   <li>Token validity verification</li>
     *   <li>Username and roles extraction from existing token</li>
     *   <li>New token generation with same credentials</li>
     *   <li>Response structure consistency with login response</li>
     * </ul>
     */
    @Test
    @DisplayName("Should refresh JWT token successfully with valid existing token")
    void shouldRefreshTokenSuccessfullyWithValidToken() {
        // Given
        String newToken = "new.jwt.token";
        List<String> roles = List.of("ADMIN");
        AdminRefreshRequest request = new AdminRefreshRequest(validToken);
        when(jwtService.isTokenValid(validToken)).thenReturn(true);
        when(jwtService.extractUsername(validToken)).thenReturn(validUsername);
        when(jwtService.extractRoles(validToken)).thenReturn(roles);
        when(jwtService.generateToken(validUsername, roles)).thenReturn(newToken);

        // When
        ResponseEntity<?> response = controller.refresh(request);

        // Then
        assertThat(response.getStatusCode())
            .describedAs("Token refresh should return HTTP 200 OK for valid token")
            .isEqualTo(HttpStatus.OK);
            
        AdminLoginResponse refreshResponse = (AdminLoginResponse) response.getBody();
        assertThat(refreshResponse)
            .describedAs("Refresh response should not be null for valid token")
            .isNotNull();
        assertThat(refreshResponse.token())
            .describedAs("Refresh response should contain newly generated JWT token")
            .isEqualTo(newToken);
        assertThat(refreshResponse.username())
            .describedAs("Refresh response should preserve original username")
            .isEqualTo(validUsername);
        assertThat(refreshResponse.roles())
            .describedAs("Refresh response should preserve original user roles")
            .isEqualTo(roles);

        verify(jwtService).isTokenValid(validToken);
        verify(jwtService).extractUsername(validToken);
        verify(jwtService).extractRoles(validToken);
        verify(jwtService).generateToken(validUsername, roles);
    }

    /**
     * Tests token refresh failure with invalid token.
     * 
     * <p>Validates security behavior when:</p>
     * <ul>
     *   <li>Token is expired, malformed, or otherwise invalid</li>
     *   <li>No token extraction or generation occurs for invalid tokens</li>
     *   <li>Clear error message indicates token invalidity</li>
     * </ul>
     */
    @Test
    @DisplayName("Should return HTTP 401 Unauthorized for invalid or expired refresh token")
    void shouldReturn401WithInvalidRefreshToken() {
        // Given
        String invalidToken = "invalid.jwt.token";
        AdminRefreshRequest request = new AdminRefreshRequest(invalidToken);
        when(jwtService.isTokenValid(invalidToken)).thenReturn(false);

        // When
        ResponseEntity<?> response = controller.refresh(request);

        // Then
        assertThat(response.getStatusCode())
            .describedAs("Token refresh should fail with HTTP 401 for invalid token")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
        
        @SuppressWarnings("unchecked")
        Map<String, String> errorResponse = (Map<String, String>) response.getBody();
        assertThat(errorResponse)
            .describedAs("Error response should indicate token invalidity")
            .isNotNull();
        assertThat(errorResponse.get("error"))
            .describedAs("Error message should clearly indicate token is invalid")
            .isEqualTo("Invalid token");

        verify(jwtService).isTokenValid(invalidToken);
        verify(jwtService, never()).extractUsername(anyString());
        verify(jwtService, never()).generateToken(anyString(), anyList());
    }

    /**
     * Tests successful logout operation.
     * 
     * <p>Validates logout behavior:</p>
     * <ul>
     *   <li>Logout always succeeds (stateless JWT tokens)</li>
     *   <li>Returns success message for client confirmation</li>
     *   <li>No server-side token invalidation needed for JWTs</li>
     * </ul>
     */
    @Test
    @DisplayName("Should logout successfully and return confirmation message")
    void shouldLogoutSuccessfully() {
        // When
        ResponseEntity<LogoutResponse> response = controller.logout();

        // Then
        assertThat(response.getStatusCode())
            .describedAs("Logout should always return HTTP 200 OK for admin users")
            .isEqualTo(HttpStatus.OK);
            
        LogoutResponse logoutResponse = response.getBody();
        assertThat(logoutResponse)
            .describedAs("Logout response should not be null")
            .isNotNull();
        assertThat(logoutResponse.message())
            .describedAs("Logout response should confirm successful logout to client")
            .isEqualTo("Logged out successfully");
    }

    /**
     * Tests admin user validation for existing user.
     * 
     * <p>Validates user existence checking:</p>
     * <ul>
     *   <li>Repository lookup by email address</li>
     *   <li>Positive validation response for existing users</li>
     *   <li>Username echo in response for confirmation</li>
     * </ul>
     */
    @Test
    @DisplayName("Should validate that admin user exists and return positive confirmation")
    void shouldValidateAdminUserExists() {
        // Given
        User validUser = createValidAdminUser();
        when(userRepository.findByEmail(validUsername)).thenReturn(Optional.of(validUser));
        
        // When
        ResponseEntity<AdminUserValidationResponse> response = controller.validateUser(validUsername);

        // Then
        assertThat(response.getStatusCode())
            .describedAs("User validation should return HTTP 200 OK")
            .isEqualTo(HttpStatus.OK);
            
        AdminUserValidationResponse validationResponse = response.getBody();
        assertThat(validationResponse)
            .describedAs("Validation response should not be null")
            .isNotNull();
        assertThat(validationResponse.exists())
            .describedAs("Validation should confirm user exists in database")
            .isTrue();
        assertThat(validationResponse.username())
            .describedAs("Validation response should echo the queried username")
            .isEqualTo(validUsername);
        
        verify(userRepository).findByEmail(validUsername);
    }

    /**
     * Tests admin user validation for non-existent user.
     * 
     * <p>Validates negative user existence checking:</p>
     * <ul>
     *   <li>Repository returns empty for non-existent users</li>
     *   <li>Negative validation response with clear indication</li>
     *   <li>Username echo for client confirmation</li>
     * </ul>
     */
    @Test
    @DisplayName("Should validate that non-existent admin user does not exist")
    void shouldReturnFalseForNonExistentAdminUser() {
        // Given
        String nonExistentUser = "nonexistent@enterprise.com";
        when(userRepository.findByEmail(nonExistentUser)).thenReturn(Optional.empty());

        // When
        ResponseEntity<AdminUserValidationResponse> response = controller.validateUser(nonExistentUser);

        // Then
        assertThat(response.getStatusCode())
            .describedAs("User validation should return HTTP 200 OK even for non-existent users")
            .isEqualTo(HttpStatus.OK);
            
        AdminUserValidationResponse validationResponse = response.getBody();
        assertThat(validationResponse)
            .describedAs("Validation response should not be null for non-existent user")
            .isNotNull();
        assertThat(validationResponse.exists())
            .describedAs("Validation should confirm user does not exist in database")
            .isFalse();
        assertThat(validationResponse.username())
            .describedAs("Validation response should echo the queried non-existent username")
            .isEqualTo(nonExistentUser);
        
        verify(userRepository).findByEmail(nonExistentUser);
    }

    /**
     * Tests current user information retrieval for authenticated admin.
     * 
     * <p>Validates authenticated user information:</p>
     * <ul>
     *   <li>Authentication status verification</li>
     *   <li>Username extraction from authentication context</li>
     *   <li>Role extraction and transformation</li>
     *   <li>Proper response structure for client use</li>
     * </ul>
     */
    @Test
    @DisplayName("Should return current admin user information for authenticated user")
    @SuppressWarnings("unchecked")
    void shouldReturnCurrentAdminUserInfo() {
        // Given
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(validUsername);
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        doReturn(authorities).when(authentication).getAuthorities();

        // When
        ResponseEntity<?> response = controller.getCurrentUser(authentication);

        // Then
        assertThat(response.getStatusCode())
            .describedAs("Current user info should return HTTP 200 OK for authenticated admin")
            .isEqualTo(HttpStatus.OK);
        
        Map<String, Object> userInfo = (Map<String, Object>) response.getBody();
        assertThat(userInfo)
            .describedAs("User info response should not be null for authenticated user")
            .isNotNull();
        assertThat(userInfo.get("username"))
            .describedAs("User info should include the authenticated username")
            .isEqualTo(validUsername);
        
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) userInfo.get("roles");
        assertThat(roles)
            .describedAs("User info should include user roles for authorization")
            .isNotNull()
            .hasSize(1);
        assertThat(roles.get(0))
            .describedAs("Role should be transformed from ROLE_SUPER_ADMIN to SUPER_ADMIN")
            .isEqualTo("SUPER_ADMIN");
    }

    /**
     * Tests current user information retrieval for unauthenticated user.
     * 
     * <p>Validates security behavior for unauthenticated requests:</p>
     * <ul>
     *   <li>Authentication status checking</li>
     *   <li>Rejection of unauthenticated requests</li>
     *   <li>Clear error message for unauthorized access</li>
     * </ul>
     */
    @Test
    @DisplayName("Should return HTTP 401 Unauthorized for unauthenticated user info request")
    void shouldReturn401ForUnauthenticatedUserInfoRequest() {
        // Given
        when(authentication.isAuthenticated()).thenReturn(false);

        // When
        ResponseEntity<?> response = controller.getCurrentUser(authentication);

        // Then
        assertThat(response.getStatusCode())
            .describedAs("User info should return HTTP 401 for unauthenticated user")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
        
        @SuppressWarnings("unchecked")
        Map<String, String> errorResponse = (Map<String, String>) response.getBody();
        assertThat(errorResponse)
            .describedAs("Error response should indicate unauthorized access")
            .isNotNull();
        assertThat(errorResponse.get("error"))
            .describedAs("Error message should clearly indicate unauthorized access")
            .isEqualTo("Unauthorized");
    }

    /**
     * Tests current user information retrieval with null authentication.
     * 
     * <p>Validates security behavior for null authentication:</p>
     * <ul>
     *   <li>Null safety in authentication handling</li>
     *   <li>Rejection of requests without authentication context</li>
     *   <li>Consistent error response for unauthorized access</li>
     * </ul>
     */
    @Test
    @DisplayName("Should return HTTP 401 Unauthorized for null authentication context")
    void shouldReturn401ForNullAuthentication() {
        // When
        ResponseEntity<?> response = controller.getCurrentUser(null);

        // Then
        assertThat(response.getStatusCode())
            .describedAs("User info should return HTTP 401 for null authentication")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
        
        @SuppressWarnings("unchecked")
        Map<String, String> errorResponse = (Map<String, String>) response.getBody();
        assertThat(errorResponse)
            .describedAs("Error response should indicate unauthorized access for null authentication")
            .isNotNull();
        assertThat(errorResponse.get("error"))
            .describedAs("Error message should clearly indicate unauthorized access")
            .isEqualTo("Unauthorized");
    }

    /**
     * Helper method to create a simulated encoded password.
     * 
     * @return BCrypt-encoded password hash for testing
     */
    private String encodedPassword() {
        return "$2a$10$KBdADFHGKGYwIjnfh56vq.i2AcnMUAdYgkEfnoqJxUr1vBD8AWODm";
    }
    
    /**
     * Helper method to create a valid admin user for testing.
     * 
     * <p>Creates a realistic User entity with:</p>
     * <ul>
     *   <li>Valid UUID identifier</li>
     *   <li>Admin email and encoded password</li>
     *   <li>ADMIN role and ACTIVE status</li>
     *   <li>Proper timestamp fields</li>
     * </ul>
     * 
     * @return User entity configured for admin authentication testing
     */
    private User createValidAdminUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(validUsername);
        user.setPasswordHash(encodedPassword());
        user.setRole(User.UserRole.ADMIN);
        user.setStatus(User.UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }
}
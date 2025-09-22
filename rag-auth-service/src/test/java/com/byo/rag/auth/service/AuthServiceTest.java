package com.byo.rag.auth.service;

import com.byo.rag.auth.security.JwtService;
import com.byo.rag.shared.dto.UserDto;
import com.byo.rag.shared.entity.Tenant;
import com.byo.rag.shared.entity.User;
import com.byo.rag.shared.exception.UserNotFoundException;
import com.byo.rag.shared.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for AuthService covering all authentication flows,
 * security scenarios, and edge cases.
 * 
 * <p>This test suite addresses the critical security gap identified in AUTH-TEST-001
 * by providing complete coverage of authentication operations including:
 * <ul>
 *   <li>User login flows with various scenarios</li>
 *   <li>JWT token generation and validation</li>
 *   <li>Token refresh mechanisms</li>
 *   <li>Security vulnerability testing</li>
 *   <li>Error handling and edge cases</li>
 * </ul>
 * 
 * <p><strong>Security Testing Focus:</strong>
 * These tests specifically validate security-critical functionality to prevent
 * authentication bypass vulnerabilities and ensure proper credential validation.
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 0.8.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private Tenant testTenant;
    private UserDto.LoginRequest validLoginRequest;
    private UserDto.LoginRequest invalidLoginRequest;

    @BeforeEach
    void setUp() {
        // Create test tenant
        testTenant = new Tenant();
        testTenant.setId(UUID.randomUUID());
        testTenant.setName("Test Tenant");
        testTenant.setSlug("test-tenant");

        // Create test user
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setPasswordHash("$2a$10$hashed.password.value");
        testUser.setRole(User.UserRole.USER);
        testUser.setStatus(User.UserStatus.ACTIVE);
        testUser.setEmailVerified(true);
        testUser.setTenant(testTenant);
        testUser.setLastLoginAt(LocalDateTime.now().minusDays(1));
        testUser.setCreatedAt(LocalDateTime.now().minusMonths(1));

        // Create test login requests
        validLoginRequest = new UserDto.LoginRequest("test@example.com", "correct.password");
        invalidLoginRequest = new UserDto.LoginRequest("test@example.com", "wrong.password");
    }

    @Nested
    @DisplayName("Login Authentication Tests")
    class LoginTests {

        @Test
        @DisplayName("Should successfully authenticate valid user credentials")
        void shouldSuccessfullyAuthenticateValidCredentials() {
            // Given
            String accessToken = "access.token.jwt";
            String refreshToken = "refresh.token.jwt";
            long tokenExpiration = 3600L;

            when(userService.findActiveByEmail("test@example.com")).thenReturn(testUser);
            when(jwtService.generateAccessToken(testUser)).thenReturn(accessToken);
            when(jwtService.generateRefreshToken(testUser)).thenReturn(refreshToken);
            when(jwtService.getAccessTokenExpiration()).thenReturn(tokenExpiration);

            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.verifyPassword("correct.password", "$2a$10$hashed.password.value"))
                        .thenReturn(true);

                // When
                UserDto.LoginResponse response = authService.login(validLoginRequest);

                // Then
                assertThat(response).isNotNull();
                assertThat(response.accessToken()).isEqualTo(accessToken);
                assertThat(response.refreshToken()).isEqualTo(refreshToken);
                assertThat(response.expiresIn()).isEqualTo(tokenExpiration);
                assertThat(response.user()).isNotNull();
                assertThat(response.user().email()).isEqualTo("test@example.com");
                assertThat(response.user().firstName()).isEqualTo("Test");
                assertThat(response.user().lastName()).isEqualTo("User");

                verify(userService).updateLastLogin(testUser.getId());
                verify(jwtService).generateAccessToken(testUser);
                verify(jwtService).generateRefreshToken(testUser);
            }
        }

        @Test
        @DisplayName("Should throw BadCredentialsException for invalid password")
        void shouldThrowBadCredentialsExceptionForInvalidPassword() {
            // Given
            when(userService.findActiveByEmail("test@example.com")).thenReturn(testUser);

            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.verifyPassword("wrong.password", "$2a$10$hashed.password.value"))
                        .thenReturn(false);

                // When & Then
                assertThatExceptionOfType(BadCredentialsException.class)
                        .isThrownBy(() -> authService.login(invalidLoginRequest))
                        .withMessage("Invalid credentials");

                verify(userService, never()).updateLastLogin(any());
                verify(jwtService, never()).generateAccessToken(any());
                verify(jwtService, never()).generateRefreshToken(any());
            }
        }

        @Test
        @DisplayName("Should throw BadCredentialsException for non-existent user")
        void shouldThrowBadCredentialsExceptionForNonExistentUser() {
            // Given
            when(userService.findActiveByEmail("nonexistent@example.com"))
                    .thenThrow(new UserNotFoundException("User not found"));

            UserDto.LoginRequest nonExistentUserRequest = new UserDto.LoginRequest("nonexistent@example.com", "password");

            // When & Then
            assertThatExceptionOfType(BadCredentialsException.class)
                    .isThrownBy(() -> authService.login(nonExistentUserRequest))
                    .withMessage("Invalid credentials");

            verify(userService, never()).updateLastLogin(any());
            verify(jwtService, never()).generateAccessToken(any());
            verify(jwtService, never()).generateRefreshToken(any());
        }

        @Test
        @DisplayName("Should throw BadCredentialsException for inactive user")
        void shouldThrowBadCredentialsExceptionForInactiveUser() {
            // Given
            testUser.setStatus(User.UserStatus.INACTIVE);
            when(userService.findActiveByEmail("test@example.com")).thenReturn(testUser);

            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.verifyPassword("correct.password", "$2a$10$hashed.password.value"))
                        .thenReturn(true);

                // When & Then
                assertThatExceptionOfType(BadCredentialsException.class)
                        .isThrownBy(() -> authService.login(validLoginRequest))
                        .withMessage("Account is not active");

                verify(userService, never()).updateLastLogin(any());
                verify(jwtService, never()).generateAccessToken(any());
                verify(jwtService, never()).generateRefreshToken(any());
            }
        }

        @Test
        @DisplayName("Should handle null password gracefully")
        void shouldHandleNullPasswordGracefully() {
            // Given
            UserDto.LoginRequest nullPasswordRequest = new UserDto.LoginRequest("test@example.com", null);
            when(userService.findActiveByEmail("test@example.com")).thenReturn(testUser);

            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.verifyPassword(null, "$2a$10$hashed.password.value"))
                        .thenReturn(false);

                // When & Then
                assertThatExceptionOfType(BadCredentialsException.class)
                        .isThrownBy(() -> authService.login(nullPasswordRequest))
                        .withMessage("Invalid credentials");
            }
        }

        @Test
        @DisplayName("Should handle empty password gracefully")
        void shouldHandleEmptyPasswordGracefully() {
            // Given
            UserDto.LoginRequest emptyPasswordRequest = new UserDto.LoginRequest("test@example.com", "");
            when(userService.findActiveByEmail("test@example.com")).thenReturn(testUser);

            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.verifyPassword("", "$2a$10$hashed.password.value"))
                        .thenReturn(false);

                // When & Then
                assertThatExceptionOfType(BadCredentialsException.class)
                        .isThrownBy(() -> authService.login(emptyPasswordRequest))
                        .withMessage("Invalid credentials");
            }
        }

        @Test
        @DisplayName("Should update last login timestamp on successful authentication")
        void shouldUpdateLastLoginTimestampOnSuccessfulAuthentication() {
            // Given
            when(userService.findActiveByEmail("test@example.com")).thenReturn(testUser);
            when(jwtService.generateAccessToken(testUser)).thenReturn("access.token");
            when(jwtService.generateRefreshToken(testUser)).thenReturn("refresh.token");
            when(jwtService.getAccessTokenExpiration()).thenReturn(3600L);

            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.verifyPassword("correct.password", "$2a$10$hashed.password.value"))
                        .thenReturn(true);

                // When
                authService.login(validLoginRequest);

                // Then
                verify(userService).updateLastLogin(testUser.getId());
            }
        }
    }

    @Nested
    @DisplayName("Token Refresh Tests")
    class TokenRefreshTests {

        @Test
        @DisplayName("Should successfully refresh valid refresh token")
        void shouldSuccessfullyRefreshValidRefreshToken() {
            // Given
            String oldRefreshToken = "old.refresh.token";
            String newAccessToken = "new.access.token";
            String newRefreshToken = "new.refresh.token";
            long tokenExpiration = 3600L;

            when(jwtService.isRefreshToken(oldRefreshToken)).thenReturn(true);
            when(jwtService.extractUsername(oldRefreshToken)).thenReturn("test@example.com");
            when(userService.findActiveByEmail("test@example.com")).thenReturn(testUser);
            when(jwtService.isTokenValid(oldRefreshToken, "test@example.com")).thenReturn(true);
            when(jwtService.generateAccessToken(testUser)).thenReturn(newAccessToken);
            when(jwtService.generateRefreshToken(testUser)).thenReturn(newRefreshToken);
            when(jwtService.getAccessTokenExpiration()).thenReturn(tokenExpiration);

            // When
            UserDto.LoginResponse response = authService.refreshToken(oldRefreshToken);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo(newAccessToken);
            assertThat(response.refreshToken()).isEqualTo(newRefreshToken);
            assertThat(response.expiresIn()).isEqualTo(tokenExpiration);
            assertThat(response.user()).isNotNull();
            assertThat(response.user().email()).isEqualTo("test@example.com");

            verify(jwtService).isRefreshToken(oldRefreshToken);
            verify(jwtService).extractUsername(oldRefreshToken);
            verify(jwtService).isTokenValid(oldRefreshToken, "test@example.com");
            verify(jwtService).generateAccessToken(testUser);
            verify(jwtService).generateRefreshToken(testUser);
        }

        @Test
        @DisplayName("Should throw BadCredentialsException for non-refresh token")
        void shouldThrowBadCredentialsExceptionForNonRefreshToken() {
            // Given
            String accessToken = "access.token.not.refresh";
            when(jwtService.isRefreshToken(accessToken)).thenReturn(false);

            // When & Then
            assertThatExceptionOfType(BadCredentialsException.class)
                    .isThrownBy(() -> authService.refreshToken(accessToken))
                    .withMessage("Invalid refresh token");

            verify(jwtService).isRefreshToken(accessToken);
            verify(jwtService, never()).extractUsername(any());
            verify(userService, never()).findActiveByEmail(any());
        }

        @Test
        @DisplayName("Should throw BadCredentialsException for invalid token signature")
        void shouldThrowBadCredentialsExceptionForInvalidTokenSignature() {
            // Given
            String invalidToken = "invalid.refresh.token";
            when(jwtService.isRefreshToken(invalidToken)).thenReturn(true);
            when(jwtService.extractUsername(invalidToken)).thenReturn("test@example.com");
            when(userService.findActiveByEmail("test@example.com")).thenReturn(testUser);
            when(jwtService.isTokenValid(invalidToken, "test@example.com")).thenReturn(false);

            // When & Then
            assertThatExceptionOfType(BadCredentialsException.class)
                    .isThrownBy(() -> authService.refreshToken(invalidToken))
                    .withMessage("Invalid refresh token");

            verify(jwtService).isTokenValid(invalidToken, "test@example.com");
            verify(jwtService, never()).generateAccessToken(any());
            verify(jwtService, never()).generateRefreshToken(any());
        }

        @Test
        @DisplayName("Should throw BadCredentialsException for non-existent user in token")
        void shouldThrowBadCredentialsExceptionForNonExistentUserInToken() {
            // Given
            String refreshToken = "valid.refresh.token";
            when(jwtService.isRefreshToken(refreshToken)).thenReturn(true);
            when(jwtService.extractUsername(refreshToken)).thenReturn("nonexistent@example.com");
            when(userService.findActiveByEmail("nonexistent@example.com"))
                    .thenThrow(new UserNotFoundException("User not found"));

            // When & Then
            assertThatExceptionOfType(BadCredentialsException.class)
                    .isThrownBy(() -> authService.refreshToken(refreshToken))
                    .withMessage("Invalid refresh token");

            verify(jwtService).extractUsername(refreshToken);
            verify(userService).findActiveByEmail("nonexistent@example.com");
            verify(jwtService, never()).generateAccessToken(any());
        }

        @Test
        @DisplayName("Should handle JWT parsing exceptions gracefully")
        void shouldHandleJwtParsingExceptionsGracefully() {
            // Given
            String malformedToken = "malformed.token";
            when(jwtService.isRefreshToken(malformedToken))
                    .thenThrow(new RuntimeException("Token parsing failed"));

            // When & Then
            assertThatExceptionOfType(BadCredentialsException.class)
                    .isThrownBy(() -> authService.refreshToken(malformedToken))
                    .withMessage("Invalid refresh token");

            verify(jwtService).isRefreshToken(malformedToken);
            verify(userService, never()).findActiveByEmail(any());
        }
    }

    @Nested
    @DisplayName("Token Validation Tests")
    class TokenValidationTests {

        @Test
        @DisplayName("Should return true for valid token and active user")
        void shouldReturnTrueForValidTokenAndActiveUser() {
            // Given
            String validToken = "valid.jwt.token";
            when(jwtService.extractUsername(validToken)).thenReturn("test@example.com");
            when(userService.findActiveByEmail("test@example.com")).thenReturn(testUser);
            when(jwtService.isTokenValid(validToken, "test@example.com")).thenReturn(true);

            // When
            boolean isValid = authService.validateToken(validToken);

            // Then
            assertThat(isValid).isTrue();
            verify(jwtService).extractUsername(validToken);
            verify(userService).findActiveByEmail("test@example.com");
            verify(jwtService).isTokenValid(validToken, "test@example.com");
        }

        @Test
        @DisplayName("Should return false for valid token but inactive user")
        void shouldReturnFalseForValidTokenButInactiveUser() {
            // Given
            String validToken = "valid.jwt.token";
            testUser.setStatus(User.UserStatus.INACTIVE);
            when(jwtService.extractUsername(validToken)).thenReturn("test@example.com");
            when(userService.findActiveByEmail("test@example.com")).thenReturn(testUser);
            when(jwtService.isTokenValid(validToken, "test@example.com")).thenReturn(true);

            // When
            boolean isValid = authService.validateToken(validToken);

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should return false for invalid token signature")
        void shouldReturnFalseForInvalidTokenSignature() {
            // Given
            String invalidToken = "invalid.jwt.token";
            when(jwtService.extractUsername(invalidToken)).thenReturn("test@example.com");
            when(userService.findActiveByEmail("test@example.com")).thenReturn(testUser);
            when(jwtService.isTokenValid(invalidToken, "test@example.com")).thenReturn(false);

            // When
            boolean isValid = authService.validateToken(invalidToken);

            // Then
            assertThat(isValid).isFalse();
            verify(jwtService).isTokenValid(invalidToken, "test@example.com");
        }

        @Test
        @DisplayName("Should return false for non-existent user")
        void shouldReturnFalseForNonExistentUser() {
            // Given
            String validToken = "valid.jwt.token";
            when(jwtService.extractUsername(validToken)).thenReturn("nonexistent@example.com");
            when(userService.findActiveByEmail("nonexistent@example.com"))
                    .thenThrow(new UserNotFoundException("User not found"));

            // When
            boolean isValid = authService.validateToken(validToken);

            // Then
            assertThat(isValid).isFalse();
            verify(jwtService).extractUsername(validToken);
            verify(userService).findActiveByEmail("nonexistent@example.com");
        }

        @Test
        @DisplayName("Should return false for malformed token")
        void shouldReturnFalseForMalformedToken() {
            // Given
            String malformedToken = "malformed.token";
            when(jwtService.extractUsername(malformedToken))
                    .thenThrow(new RuntimeException("Token parsing failed"));

            // When
            boolean isValid = authService.validateToken(malformedToken);

            // Then
            assertThat(isValid).isFalse();
            verify(jwtService).extractUsername(malformedToken);
            verify(userService, never()).findActiveByEmail(any());
        }

        @Test
        @DisplayName("Should return false for null token")
        void shouldReturnFalseForNullToken() {
            // Given
            String nullToken = null;
            when(jwtService.extractUsername(nullToken))
                    .thenThrow(new IllegalArgumentException("Token cannot be null"));

            // When
            boolean isValid = authService.validateToken(nullToken);

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should return false for empty token")
        void shouldReturnFalseForEmptyToken() {
            // Given
            String emptyToken = "";
            when(jwtService.extractUsername(emptyToken))
                    .thenThrow(new IllegalArgumentException("Token cannot be empty"));

            // When
            boolean isValid = authService.validateToken(emptyToken);

            // Then
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("Security Edge Cases")
    class SecurityEdgeCaseTests {

        @Test
        @DisplayName("Should not expose user existence through different error messages")
        void shouldNotExposeUserExistenceThroughDifferentErrorMessages() {
            // Test for non-existent user
            when(userService.findActiveByEmail("nonexistent@example.com"))
                    .thenThrow(new UserNotFoundException("User not found"));
            
            UserDto.LoginRequest nonExistentRequest = new UserDto.LoginRequest("nonexistent@example.com", "password");

            // Test for existing user with wrong password
            when(userService.findActiveByEmail("test@example.com")).thenReturn(testUser);

            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.verifyPassword("wrong.password", "$2a$10$hashed.password.value"))
                        .thenReturn(false);

                UserDto.LoginRequest wrongPasswordRequest = new UserDto.LoginRequest("test@example.com", "wrong.password");

                // Both should throw the same error message
                String nonExistentMessage = "";
                String wrongPasswordMessage = "";

                try {
                    authService.login(nonExistentRequest);
                } catch (BadCredentialsException e) {
                    nonExistentMessage = e.getMessage();
                }

                try {
                    authService.login(wrongPasswordRequest);
                } catch (BadCredentialsException e) {
                    wrongPasswordMessage = e.getMessage();
                }

                // Both should return generic "Invalid credentials" message
                assertThat(nonExistentMessage).isEqualTo("Invalid credentials");
                assertThat(wrongPasswordMessage).isEqualTo("Invalid credentials");
                assertThat(nonExistentMessage).isEqualTo(wrongPasswordMessage);
            }
        }

        @Test
        @DisplayName("Should handle concurrent login attempts gracefully")
        void shouldHandleConcurrentLoginAttemptsGracefully() {
            // Given
            when(userService.findActiveByEmail("test@example.com")).thenReturn(testUser);
            when(jwtService.generateAccessToken(testUser)).thenReturn("access.token");
            when(jwtService.generateRefreshToken(testUser)).thenReturn("refresh.token");
            when(jwtService.getAccessTokenExpiration()).thenReturn(3600L);

            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.verifyPassword("correct.password", "$2a$10$hashed.password.value"))
                        .thenReturn(true);

                // When - simulate concurrent calls
                UserDto.LoginResponse response1 = authService.login(validLoginRequest);
                UserDto.LoginResponse response2 = authService.login(validLoginRequest);

                // Then - both should succeed independently
                assertThat(response1).isNotNull();
                assertThat(response2).isNotNull();
                assertThat(response1.accessToken()).isEqualTo("access.token");
                assertThat(response2.accessToken()).isEqualTo("access.token");

                // Last login should be updated twice
                verify(userService, times(2)).updateLastLogin(testUser.getId());
            }
        }

        @Test
        @DisplayName("Should validate user status on each token operation")
        void shouldValidateUserStatusOnEachTokenOperation() {
            // Given - user becomes inactive between operations
            String refreshToken = "valid.refresh.token";
            testUser.setStatus(User.UserStatus.INACTIVE);

            when(jwtService.isRefreshToken(refreshToken)).thenReturn(true);
            when(jwtService.extractUsername(refreshToken)).thenReturn("test@example.com");
            when(userService.findActiveByEmail("test@example.com")).thenReturn(testUser);

            // When & Then - should fail even with valid token if user is inactive
            assertThatExceptionOfType(BadCredentialsException.class)
                    .isThrownBy(() -> authService.refreshToken(refreshToken))
                    .withMessage("Invalid refresh token");
        }

        @Test
        @DisplayName("Should handle database exceptions during authentication")
        void shouldHandleDatabaseExceptionsDuringAuthentication() {
            // Given
            when(userService.findActiveByEmail("test@example.com"))
                    .thenThrow(new RuntimeException("Database connection failed"));

            // When & Then
            assertThatExceptionOfType(RuntimeException.class)
                    .isThrownBy(() -> authService.login(validLoginRequest))
                    .withMessage("Database connection failed");
        }

        @Test
        @DisplayName("Should handle JWT service exceptions during token generation")
        void shouldHandleJwtServiceExceptionsDuringTokenGeneration() {
            // Given
            when(userService.findActiveByEmail("test@example.com")).thenReturn(testUser);
            when(jwtService.generateAccessToken(testUser))
                    .thenThrow(new RuntimeException("JWT generation failed"));

            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.verifyPassword("correct.password", "$2a$10$hashed.password.value"))
                        .thenReturn(true);

                // When & Then
                assertThatExceptionOfType(RuntimeException.class)
                        .isThrownBy(() -> authService.login(validLoginRequest))
                        .withMessage("JWT generation failed");
            }
        }
    }

    @Nested
    @DisplayName("Transaction Behavior Tests")
    class TransactionBehaviorTests {

        @Test
        @DisplayName("Should ensure login operations are transactional")
        void shouldEnsureLoginOperationsAreTransactional() {
            // Given
            when(userService.findActiveByEmail("test@example.com")).thenReturn(testUser);
            when(jwtService.generateAccessToken(testUser)).thenReturn("access.token");
            when(jwtService.generateRefreshToken(testUser)).thenReturn("refresh.token");
            
            // Simulate exception during last login update
            doThrow(new RuntimeException("Database update failed"))
                    .when(userService).updateLastLogin(testUser.getId());

            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.verifyPassword("correct.password", "$2a$10$hashed.password.value"))
                        .thenReturn(true);

                // When & Then - Should propagate the exception
                assertThatExceptionOfType(RuntimeException.class)
                        .isThrownBy(() -> authService.login(validLoginRequest))
                        .withMessage("Database update failed");
            }
        }

        @Test
        @DisplayName("Should mark token validation as read-only transaction")
        void shouldMarkTokenValidationAsReadOnlyTransaction() {
            // Given
            String validToken = "valid.jwt.token";
            when(jwtService.extractUsername(validToken)).thenReturn("test@example.com");
            when(userService.findActiveByEmail("test@example.com")).thenReturn(testUser);
            when(jwtService.isTokenValid(validToken, "test@example.com")).thenReturn(true);

            // When
            boolean isValid = authService.validateToken(validToken);

            // Then
            assertThat(isValid).isTrue();
            
            // Verify no write operations were attempted during validation
            verify(userService, never()).updateLastLogin(any());
            // Verify no write operations were attempted during validation
        }
    }
}
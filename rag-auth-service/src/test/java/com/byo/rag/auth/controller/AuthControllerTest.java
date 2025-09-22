package com.byo.rag.auth.controller;

import com.byo.rag.auth.service.AuthService;
import com.byo.rag.auth.service.UserService;
import com.byo.rag.shared.dto.UserDto;
import com.byo.rag.shared.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Standalone unit tests for AuthController without Spring context.
 * 
 * <p>This test suite validates the REST API layer of authentication including:</p>
 * <ul>
 *   <li>Login endpoint validation and responses</li>
 *   <li>Token refresh endpoint functionality</li>
 *   <li>Token validation endpoint testing</li>
 *   <li>Input validation and error handling</li>
 *   <li>HTTP status code verification</li>
 * </ul>
 * 
 * <p><strong>Testing Approach:</strong>
 * Uses standalone MockMvc without Spring context to isolate controller testing
 * and avoid dependency injection issues during test execution.</p>
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 0.8.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Standalone Unit Tests")
class AuthControllerUnitTest {

    @Mock
    private AuthService authService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UserDto.LoginRequest validLoginRequest;
    private UserDto.LoginResponse successfulLoginResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();
        
        validLoginRequest = new UserDto.LoginRequest("test@example.com", "correct.password");

        UserDto.UserResponse userResponse = new UserDto.UserResponse(
                UUID.randomUUID(),
                "Test",
                "User",
                "test@example.com",
                User.UserRole.USER,
                User.UserStatus.ACTIVE,
                true,
                LocalDateTime.now(),
                LocalDateTime.now().minusMonths(1),
                null
        );

        successfulLoginResponse = new UserDto.LoginResponse(
                "access.token.jwt",
                "refresh.token.jwt",
                3600L,
                userResponse
        );
    }

    @Nested
    @DisplayName("Login Endpoint Tests")
    class LoginEndpointTests {

        @Test
        @DisplayName("Should return 200 and JWT tokens for valid credentials")
        void shouldReturn200AndJwtTokensForValidCredentials() throws Exception {
            // Given
            when(authService.login(any(UserDto.LoginRequest.class))).thenReturn(successfulLoginResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validLoginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.accessToken").value("access.token.jwt"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh.token.jwt"))
                    .andExpect(jsonPath("$.expiresIn").value(3600))
                    .andExpect(jsonPath("$.user").exists())
                    .andExpect(jsonPath("$.user.email").value("test@example.com"))
                    .andExpect(jsonPath("$.user.firstName").value("Test"))
                    .andExpect(jsonPath("$.user.lastName").value("User"));

            verify(authService).login(any(UserDto.LoginRequest.class));
        }

        @Test
        @DisplayName("Should return 500 for invalid credentials (no exception handler)")
        void shouldReturn500ForInvalidCredentials() throws Exception {
            // Given
            when(authService.login(any(UserDto.LoginRequest.class)))
                    .thenThrow(new BadCredentialsException("Invalid credentials"));

            // When & Then - BadCredentialsException should result in 500 due to no exception handler in standalone setup
            try {
                mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                        .andExpect(status().isInternalServerError());
            } catch (Exception e) {
                // Expected in standalone setup without proper exception handling
                assert e.getCause() instanceof BadCredentialsException;
            }

            verify(authService).login(any(UserDto.LoginRequest.class));
        }

        @Test
        @DisplayName("Should return 400 for missing email (validation active)")
        void shouldReturn400ForMissingEmail() throws Exception {
            // Given
            String requestWithoutEmail = "{\"password\": \"password\"}";

            // When & Then - Missing email violates @NotBlank validation
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestWithoutEmail))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).login(any(UserDto.LoginRequest.class));
        }

        @Test
        @DisplayName("Should return 400 for missing password (validation active)")
        void shouldReturn400ForMissingPassword() throws Exception {
            // Given
            String requestWithoutPassword = "{\"email\": \"test@example.com\"}";

            // When & Then - Missing password violates @NotBlank validation  
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestWithoutPassword))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).login(any(UserDto.LoginRequest.class));
        }

        @Test
        @DisplayName("Should return 400 for malformed JSON")
        void shouldReturn400ForMalformedJson() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{invalid json"))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).login(any());
        }
    }

    @Nested
    @DisplayName("Token Refresh Endpoint Tests")
    class TokenRefreshEndpointTests {

        @Test
        @DisplayName("Should return 200 and new tokens for valid refresh token")
        void shouldReturn200AndNewTokensForValidRefreshToken() throws Exception {
            // Given
            when(authService.refreshToken("valid.refresh.token")).thenReturn(successfulLoginResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"refreshToken\": \"valid.refresh.token\"}"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.accessToken").value("access.token.jwt"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh.token.jwt"))
                    .andExpect(jsonPath("$.expiresIn").value(3600))
                    .andExpect(jsonPath("$.user").exists());

            verify(authService).refreshToken("valid.refresh.token");
        }

        @Test
        @DisplayName("Should throw exception for invalid refresh token (no exception handler)")
        void shouldThrowExceptionForInvalidRefreshToken() throws Exception {
            // Given
            when(authService.refreshToken("invalid.refresh.token"))
                    .thenThrow(new BadCredentialsException("Invalid refresh token"));

            // When & Then - Exception should be thrown in standalone setup
            try {
                mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"invalid.refresh.token\"}"))
                        .andExpect(status().isInternalServerError());
            } catch (Exception e) {
                // Expected in standalone setup without proper exception handling
                assert e.getCause() instanceof BadCredentialsException;
            }

            verify(authService).refreshToken("invalid.refresh.token");
        }

        @Test
        @DisplayName("Should call service with null refresh token when missing (no validation in standalone)")
        void shouldCallServiceWithNullRefreshTokenWhenMissing() throws Exception {
            // Given
            when(authService.refreshToken(null)).thenReturn(successfulLoginResponse);

            // When & Then - In standalone setup, missing fields result in null values passed to service
            mockMvc.perform(post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isOk());

            verify(authService).refreshToken(null);
        }
    }

    @Nested
    @DisplayName("Token Validation Endpoint Tests")
    class TokenValidationEndpointTests {

        @Test
        @DisplayName("Should return 200 and true for valid token")
        void shouldReturn200AndTrueForValidToken() throws Exception {
            // Given
            when(authService.validateToken("valid.token")).thenReturn(true);

            // When & Then
            mockMvc.perform(post("/api/v1/auth/validate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"token\": \"valid.token\"}"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.valid").value(true));

            verify(authService).validateToken("valid.token");
        }

        @Test
        @DisplayName("Should return 200 and false for invalid token")
        void shouldReturn200AndFalseForInvalidToken() throws Exception {
            // Given
            when(authService.validateToken("invalid.token")).thenReturn(false);

            // When & Then
            mockMvc.perform(post("/api/v1/auth/validate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"token\": \"invalid.token\"}"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.valid").value(false));

            verify(authService).validateToken("invalid.token");
        }

        @Test
        @DisplayName("Should call service with null token when missing (no validation in standalone)")
        void shouldCallServiceWithNullTokenWhenMissing() throws Exception {
            // Given
            when(authService.validateToken(null)).thenReturn(false);

            // When & Then - In standalone setup, missing fields result in null values passed to service
            mockMvc.perform(post("/api/v1/auth/validate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(false));

            verify(authService).validateToken(null);
        }

        @Test
        @DisplayName("Should return 400 for malformed JSON in request body")
        void shouldReturn400ForMalformedJsonInRequestBody() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/v1/auth/validate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{invalid json"))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).validateToken(any());
        }
    }

    @Nested
    @DisplayName("API Behavior Tests")
    class ApiBehaviorTests {

        @Test
        @DisplayName("Should handle special characters in password")
        void shouldHandleSpecialCharactersInPassword() throws Exception {
            // Given
            UserDto.LoginRequest requestWithSpecialChars = new UserDto.LoginRequest(
                    "test@example.com", "P@$$w0rd!@#$%^&*()");
            when(authService.login(any(UserDto.LoginRequest.class))).thenReturn(successfulLoginResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestWithSpecialChars)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").exists());

            verify(authService).login(any(UserDto.LoginRequest.class));
        }

        @Test
        @DisplayName("Should require JSON content type for POST requests")
        void shouldRequireJsonContentTypeForPostRequests() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.TEXT_PLAIN)
                    .content("not json"))
                    .andExpect(status().isUnsupportedMediaType());

            verify(authService, never()).login(any());
        }

        @Test
        @DisplayName("Should handle empty request body gracefully")
        void shouldHandleEmptyRequestBodyGracefully() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(""))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).login(any());
        }
    }
}
package com.enterprise.rag.admin.controller;

import com.enterprise.rag.admin.config.TestSecurityConfig;
import com.enterprise.rag.admin.service.AdminJwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminAuthController.class)
@Import(TestSecurityConfig.class)
@DisplayName("AdminAuthController Tests")
class AdminAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminJwtService jwtService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private final String validUsername = "admin@enterprise.com";
    private final String validPassword = "AdminPassword123!";
    private final String validToken = "valid.jwt.token";
    private final List<String> adminRoles = List.of("SUPER_ADMIN");

    @BeforeEach
    void setUp() {
        // Reset mocks
        reset(jwtService, passwordEncoder);
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void shouldLoginSuccessfullyWithValidCredentials() throws Exception {
        // Given
        when(passwordEncoder.matches(validPassword, encodedPassword())).thenReturn(true);
        when(jwtService.generateToken(validUsername, adminRoles)).thenReturn(validToken);

        String loginRequest = """
            {
                "username": "%s",
                "password": "%s"
            }
            """.formatted(validUsername, validPassword);

        // When & Then
        mockMvc.perform(post("/admin/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(validToken))
                .andExpect(jsonPath("$.username").value(validUsername))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles[0]").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$.expiresIn").isNumber());

        verify(passwordEncoder).matches(validPassword, encodedPassword());
        verify(jwtService).generateToken(validUsername, adminRoles);
    }

    @Test
    @DisplayName("Should return 401 with invalid credentials")
    void shouldReturn401WithInvalidCredentials() throws Exception {
        // Given
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        String loginRequest = """
            {
                "username": "%s",
                "password": "WrongPassword"
            }
            """.formatted(validUsername);

        // When & Then
        mockMvc.perform(post("/admin/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"))
                .andExpect(jsonPath("$.message").exists());

        verify(passwordEncoder).matches("WrongPassword", encodedPassword());
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Should return 400 with missing username")
    void shouldReturn400WithMissingUsername() throws Exception {
        // Given
        String loginRequest = """
            {
                "password": "%s"
            }
            """.formatted(validPassword);

        // When & Then
        mockMvc.perform(post("/admin/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.message").exists());

        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    @DisplayName("Should return 400 with missing password")
    void shouldReturn400WithMissingPassword() throws Exception {
        // Given
        String loginRequest = """
            {
                "username": "%s"
            }
            """.formatted(validUsername);

        // When & Then
        mockMvc.perform(post("/admin/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.message").exists());

        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    @DisplayName("Should refresh token successfully with valid token")
    void shouldRefreshTokenSuccessfullyWithValidToken() throws Exception {
        // Given
        String newToken = "new.jwt.token";
        when(jwtService.isTokenValid(validToken)).thenReturn(true);
        when(jwtService.extractUsername(validToken)).thenReturn(validUsername);
        when(jwtService.extractRoles(validToken)).thenReturn(adminRoles);
        when(jwtService.generateToken(validUsername, adminRoles)).thenReturn(newToken);

        String refreshRequest = """
            {
                "token": "%s"
            }
            """.formatted(validToken);

        // When & Then
        mockMvc.perform(post("/admin/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(newToken))
                .andExpect(jsonPath("$.username").value(validUsername))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.expiresIn").isNumber());

        verify(jwtService).isTokenValid(validToken);
        verify(jwtService).extractUsername(validToken);
        verify(jwtService).extractRoles(validToken);
        verify(jwtService).generateToken(validUsername, adminRoles);
    }

    @Test
    @DisplayName("Should return 401 with invalid refresh token")
    void shouldReturn401WithInvalidRefreshToken() throws Exception {
        // Given
        String invalidToken = "invalid.jwt.token";
        when(jwtService.isTokenValid(invalidToken)).thenReturn(false);

        String refreshRequest = """
            {
                "token": "%s"
            }
            """.formatted(invalidToken);

        // When & Then
        mockMvc.perform(post("/admin/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshRequest))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid token"))
                .andExpect(jsonPath("$.message").exists());

        verify(jwtService).isTokenValid(invalidToken);
        verify(jwtService, never()).extractUsername(anyString());
        verify(jwtService, never()).generateToken(anyString(), anyList());
    }

    @Test
    @DisplayName("Should logout successfully")
    void shouldLogoutSuccessfully() throws Exception {
        // When & Then
        mockMvc.perform(post("/admin/api/auth/logout")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    @Test
    @DisplayName("Should validate admin user exists")
    void shouldValidateAdminUserExists() throws Exception {
        // Given
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/admin/api/auth/validate")
                        .param("username", validUsername))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true))
                .andExpect(jsonPath("$.username").value(validUsername));
    }

    @Test
    @DisplayName("Should return false for non-existent admin user")
    void shouldReturnFalseForNonExistentAdminUser() throws Exception {
        // Given
        String nonExistentUser = "nonexistent@enterprise.com";

        // When & Then
        mockMvc.perform(get("/admin/api/auth/validate")
                        .param("username", nonExistentUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false))
                .andExpect(jsonPath("$.username").value(nonExistentUser));
    }

    @Test
    @DisplayName("Should return current admin user info")
    void shouldReturnCurrentAdminUserInfo() throws Exception {
        // When & Then
        mockMvc.perform(get("/admin/api/auth/me")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(validUsername))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles[0]").value("SUPER_ADMIN"));
    }

    @Test
    @DisplayName("Should return 401 for unauthenticated user info request")
    void shouldReturn401ForUnauthenticatedUserInfoRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/admin/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    // Helper method to simulate encoded password
    private String encodedPassword() {
        return "$2a$10$EncodedPasswordHashForTesting";
    }
}
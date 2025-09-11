package com.byo.rag.admin.controller;

import com.byo.rag.admin.AdminServiceApplication;
import com.byo.rag.admin.config.TestSecurityConfig;
import com.byo.rag.admin.config.TestDataConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestDataConfig.class})
@DisplayName("AdminAuthController Integration Tests")
class AdminAuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private final String validUsername = "admin@enterprise.com";
    private final String validPassword = "AdminPassword123!";

    @BeforeEach
    void setUp() {
        // No setup needed for integration tests
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void shouldLoginSuccessfullyWithValidCredentials() throws Exception {
        String loginRequest = """
            {
                "username": "%s",
                "password": "%s"
            }
            """.formatted(validUsername, validPassword);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value(validUsername))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.expiresIn").isNumber());
    }

    @Test
    @DisplayName("Should return 401 with invalid credentials")
    void shouldReturn401WithInvalidCredentials() throws Exception {
        String loginRequest = """
            {
                "username": "%s",
                "password": "WrongPassword"
            }
            """.formatted(validUsername);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Should return 400 with missing username")
    void shouldReturn400WithMissingUsername() throws Exception {
        String loginRequest = """
            {
                "password": "%s"
            }
            """.formatted(validPassword);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Should return 400 with missing password")
    void shouldReturn400WithMissingPassword() throws Exception {
        String loginRequest = """
            {
                "username": "%s"
            }
            """.formatted(validUsername);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Should refresh token successfully with valid token")
    void shouldRefreshTokenSuccessfullyWithValidToken() throws Exception {
        // First login to get a token
        String loginRequest = """
            {
                "username": "%s",
                "password": "%s"
            }
            """.formatted(validUsername, validPassword);

        String loginResponse = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(loginResponse).get("token").asText();

        String refreshRequest = """
            {
                "token": "%s"
            }
            """.formatted(token);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value(validUsername))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.expiresIn").isNumber());
    }

    @Test
    @DisplayName("Should return 401 with invalid refresh token")
    void shouldReturn401WithInvalidRefreshToken() throws Exception {
        String invalidToken = "invalid.jwt.token";
        String refreshRequest = """
            {
                "token": "%s"
            }
            """.formatted(invalidToken);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshRequest))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid token"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Should logout successfully")
    void shouldLogoutSuccessfully() throws Exception {
        // First login to get a token
        String loginRequest = """
            {
                "username": "%s",
                "password": "%s"
            }
            """.formatted(validUsername, validPassword);

        String loginResponse = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(loginResponse).get("token").asText();

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    @Test
    @DisplayName("Should validate admin user exists")
    void shouldValidateAdminUserExists() throws Exception {
        mockMvc.perform(get("/auth/validate")
                        .param("username", validUsername))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true))
                .andExpect(jsonPath("$.username").value(validUsername));
    }

    @Test
    @DisplayName("Should return false for non-existent admin user")
    void shouldReturnFalseForNonExistentAdminUser() throws Exception {
        String nonExistentUser = "nonexistent@enterprise.com";

        mockMvc.perform(get("/auth/validate")
                        .param("username", nonExistentUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false))
                .andExpect(jsonPath("$.username").value(nonExistentUser));
    }

    @Test
    @DisplayName("Should return current admin user info")
    void shouldReturnCurrentAdminUserInfo() throws Exception {
        // First login to get a token
        String loginRequest = """
            {
                "username": "%s",
                "password": "%s"
            }
            """.formatted(validUsername, validPassword);

        String loginResponse = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(loginResponse).get("token").asText();

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(validUsername))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"));
    }

    @Test
    @DisplayName("Should return 401 for unauthenticated user info request")
    void shouldReturn401ForUnauthenticatedUserInfoRequest() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
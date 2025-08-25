package com.enterprise.rag.admin.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminJwtService Tests")
class AdminJwtServiceTest {

    private AdminJwtService jwtService;
    private final String testSecret = "test-secret-key-that-is-at-least-256-bits-long-for-secure-jwt-token-generation";
    private final long testExpiration = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtService = new AdminJwtService(testSecret, testExpiration);
    }

    @Test
    @DisplayName("Should generate valid JWT token for admin user")
    void shouldGenerateValidJwtToken() {
        // Given
        String username = "admin@enterprise.com";
        List<String> roles = Arrays.asList("SUPER_ADMIN", "ADMIN");

        // When
        String token = jwtService.generateToken(username, roles);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
    }

    @Test
    @DisplayName("Should extract username from valid token")
    void shouldExtractUsernameFromToken() {
        // Given
        String username = "admin@enterprise.com";
        List<String> roles = List.of("SUPER_ADMIN");
        String token = jwtService.generateToken(username, roles);

        // When
        String extractedUsername = jwtService.extractUsername(token);

        // Then
        assertThat(extractedUsername).isEqualTo(username);
    }

    @Test
    @DisplayName("Should extract roles from valid token")
    void shouldExtractRolesFromToken() {
        // Given
        String username = "admin@enterprise.com";
        List<String> roles = Arrays.asList("SUPER_ADMIN", "ADMIN");
        String token = jwtService.generateToken(username, roles);

        // When
        List<String> extractedRoles = jwtService.extractRoles(token);

        // Then
        assertThat(extractedRoles).containsExactlyInAnyOrderElementsOf(roles);
    }

    @Test
    @DisplayName("Should validate valid token")
    void shouldValidateValidToken() {
        // Given
        String username = "admin@enterprise.com";
        List<String> roles = List.of("SUPER_ADMIN");
        String token = jwtService.generateToken(username, roles);

        // When
        boolean isValid = jwtService.isTokenValid(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject invalid token")
    void shouldRejectInvalidToken() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When
        boolean isValid = jwtService.isTokenValid(invalidToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject null token")
    void shouldRejectNullToken() {
        // When
        boolean isValid = jwtService.isTokenValid(null);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject empty token")
    void shouldRejectEmptyToken() {
        // When
        boolean isValid = jwtService.isTokenValid("");

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should handle token expiration")
    void shouldHandleTokenExpiration() {
        // Given - Create service with very short expiration
        AdminJwtService shortExpirationService = new AdminJwtService(testSecret, 1); // 1ms
        String token = shortExpirationService.generateToken("admin@enterprise.com", List.of("ADMIN"));

        // When - Wait for token to expire
        try {
            Thread.sleep(10); // Wait 10ms to ensure expiration
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Then
        assertThat(shortExpirationService.isTokenValid(token)).isFalse();
    }

    @Test
    @DisplayName("Should throw exception when extracting username from invalid token")
    void shouldThrowExceptionForInvalidTokenUsernameExtraction() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When & Then
        assertThatThrownBy(() -> jwtService.extractUsername(invalidToken))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should throw exception when extracting roles from invalid token")
    void shouldThrowExceptionForInvalidTokenRoleExtraction() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When & Then
        assertThatThrownBy(() -> jwtService.extractRoles(invalidToken))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should generate different tokens for different users")
    void shouldGenerateDifferentTokensForDifferentUsers() {
        // Given
        String user1 = "admin1@enterprise.com";
        String user2 = "admin2@enterprise.com";
        List<String> roles = List.of("ADMIN");

        // When
        String token1 = jwtService.generateToken(user1, roles);
        String token2 = jwtService.generateToken(user2, roles);

        // Then
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("Should generate different tokens for same user with different roles")
    void shouldGenerateDifferentTokensForDifferentRoles() {
        // Given
        String username = "admin@enterprise.com";
        List<String> roles1 = List.of("ADMIN");
        List<String> roles2 = List.of("SUPER_ADMIN");

        // When
        String token1 = jwtService.generateToken(username, roles1);
        String token2 = jwtService.generateToken(username, roles2);

        // Then
        assertThat(token1).isNotEqualTo(token2);
    }
}
package com.byo.rag.auth.security;

import com.byo.rag.shared.entity.Tenant;
import com.byo.rag.shared.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for JwtService covering all token operations,
 * security scenarios, and validation edge cases.
 * 
 * <p>This test suite addresses critical JWT security functionality including:
 * <ul>
 *   <li>Access and refresh token generation</li>
 *   <li>Token validation and claim extraction</li>
 *   <li>Security edge cases and attack vectors</li>
 *   <li>Token expiration and lifecycle management</li>
 *   <li>Cryptographic signature validation</li>
 * </ul>
 * 
 * <p><strong>Security Testing Focus:</strong>
 * These tests validate JWT security features to prevent token manipulation,
 * signature bypass attacks, and ensure proper claim validation.
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 0.8.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;
    private User testUser;
    private Tenant testTenant;
    
    private static final String TEST_SECRET = "TestSecretKeyForJWTThatIsAtLeast256BitsLongForHS256AlgorithmSecurity";
    private static final long ACCESS_TOKEN_EXPIRATION = 3600L; // 1 hour
    private static final long REFRESH_TOKEN_EXPIRATION = 604800L; // 7 days

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, ACCESS_TOKEN_EXPIRATION, REFRESH_TOKEN_EXPIRATION);
        
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
        testUser.setRole(User.UserRole.USER);
        testUser.setStatus(User.UserStatus.ACTIVE);
        testUser.setTenant(testTenant);
        testUser.setEmailVerified(true);
        testUser.setLastLoginAt(LocalDateTime.now());
        testUser.setCreatedAt(LocalDateTime.now().minusMonths(1));
    }

    @Nested
    @DisplayName("Access Token Generation Tests")
    class AccessTokenGenerationTests {

        @Test
        @DisplayName("Should generate valid access token with all required claims")
        void shouldGenerateValidAccessTokenWithAllRequiredClaims() {
            // When
            String accessToken = jwtService.generateAccessToken(testUser);

            // Then
            assertThat(accessToken).isNotNull();
            assertThat(accessToken).isNotEmpty();
            assertThat(accessToken.split("\\.")).hasSize(3); // JWT has 3 parts: header.payload.signature

            // Validate claims
            Claims claims = jwtService.extractClaims(accessToken);
            assertThat(claims.getSubject()).isEqualTo("test@example.com");
            assertThat(claims.get("userId", String.class)).isEqualTo(testUser.getId().toString());
            assertThat(claims.get("tenantId", String.class)).isEqualTo(testTenant.getId().toString());
            assertThat(claims.get("role", String.class)).isEqualTo("USER");
            assertThat(claims.get("email", String.class)).isEqualTo("test@example.com");
            assertThat(claims.get("firstName", String.class)).isEqualTo("Test");
            assertThat(claims.get("lastName", String.class)).isEqualTo("User");
            assertThat(claims.getIssuedAt()).isNotNull();
            assertThat(claims.getExpiration()).isNotNull();
            assertThat(claims.getExpiration()).isAfter(new Date());
        }

        @Test
        @DisplayName("Should generate tokens with correct expiration time")
        void shouldGenerateTokensWithCorrectExpirationTime() {
            // When
            String accessToken = jwtService.generateAccessToken(testUser);
            Date expirationDate = jwtService.getExpirationDate(accessToken);

            // Then
            long currentTime = System.currentTimeMillis();
            long expectedExpiration = currentTime + (ACCESS_TOKEN_EXPIRATION * 1000);
            long actualExpiration = expirationDate.getTime();

            // Allow 5 second tolerance for test execution time
            assertThat(actualExpiration).isBetween(expectedExpiration - 5000, expectedExpiration + 5000);
        }

        @Test
        @DisplayName("Should generate different tokens for same user on multiple calls")
        void shouldGenerateDifferentTokensForSameUserOnMultipleCalls() throws InterruptedException {
            // When
            String token1 = jwtService.generateAccessToken(testUser);
            Thread.sleep(1100); // Sleep to ensure different timestamps
            String token2 = jwtService.generateAccessToken(testUser);

            // Then
            assertThat(token1).isNotEqualTo(token2);
            
            // But should have same claims content (except timestamps)
            Claims claims1 = jwtService.extractClaims(token1);
            Claims claims2 = jwtService.extractClaims(token2);
            assertThat(claims1.getSubject()).isEqualTo(claims2.getSubject());
            assertThat(claims1.get("userId")).isEqualTo(claims2.get("userId"));
        }

        @Test
        @DisplayName("Should handle admin role correctly in token claims")
        void shouldHandleAdminRoleCorrectlyInTokenClaims() {
            // Given
            testUser.setRole(User.UserRole.ADMIN);

            // When
            String accessToken = jwtService.generateAccessToken(testUser);

            // Then
            Claims claims = jwtService.extractClaims(accessToken);
            assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        }
    }

    @Nested
    @DisplayName("Refresh Token Generation Tests")
    class RefreshTokenGenerationTests {

        @Test
        @DisplayName("Should generate valid refresh token with minimal claims")
        void shouldGenerateValidRefreshTokenWithMinimalClaims() {
            // When
            String refreshToken = jwtService.generateRefreshToken(testUser);

            // Then
            assertThat(refreshToken).isNotNull();
            assertThat(refreshToken).isNotEmpty();
            assertThat(refreshToken.split("\\.")).hasSize(3);

            // Validate claims - refresh tokens should have minimal claims
            Claims claims = jwtService.extractClaims(refreshToken);
            assertThat(claims.getSubject()).isEqualTo("test@example.com");
            assertThat(claims.get("userId", String.class)).isEqualTo(testUser.getId().toString());
            assertThat(claims.get("tokenType", String.class)).isEqualTo("refresh");
            assertThat(claims.getIssuedAt()).isNotNull();
            assertThat(claims.getExpiration()).isNotNull();

            // Should NOT contain full user details (security by minimality)
            assertThat(claims.get("tenantId")).isNull();
            assertThat(claims.get("role")).isNull();
            assertThat(claims.get("email")).isNull();
            assertThat(claims.get("firstName")).isNull();
        }

        @Test
        @DisplayName("Should generate refresh tokens with correct expiration time")
        void shouldGenerateRefreshTokensWithCorrectExpirationTime() {
            // When
            String refreshToken = jwtService.generateRefreshToken(testUser);
            Date expirationDate = jwtService.getExpirationDate(refreshToken);

            // Then
            long currentTime = System.currentTimeMillis();
            long expectedExpiration = currentTime + (REFRESH_TOKEN_EXPIRATION * 1000);
            long actualExpiration = expirationDate.getTime();

            // Allow 5 second tolerance
            assertThat(actualExpiration).isBetween(expectedExpiration - 5000, expectedExpiration + 5000);
        }

        @Test
        @DisplayName("Should identify refresh tokens correctly")
        void shouldIdentifyRefreshTokensCorrectly() {
            // When
            String refreshToken = jwtService.generateRefreshToken(testUser);
            String accessToken = jwtService.generateAccessToken(testUser);

            // Then
            assertThat(jwtService.isRefreshToken(refreshToken)).isTrue();
            assertThat(jwtService.isRefreshToken(accessToken)).isFalse();
        }
    }

    @Nested
    @DisplayName("Token Validation Tests")
    class TokenValidationTests {

        @Test
        @DisplayName("Should validate valid token with correct username")
        void shouldValidateValidTokenWithCorrectUsername() {
            // Given
            String token = jwtService.generateAccessToken(testUser);

            // When
            boolean isValid = jwtService.isTokenValid(token, "test@example.com");

            // Then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should reject token with incorrect username")
        void shouldRejectTokenWithIncorrectUsername() {
            // Given
            String token = jwtService.generateAccessToken(testUser);

            // When
            boolean isValid = jwtService.isTokenValid(token, "wrong@example.com");

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject malformed token")
        void shouldRejectMalformedToken() {
            // Given
            String malformedToken = "not.a.valid.jwt.token";

            // When
            boolean isValid = jwtService.isTokenValid(malformedToken, "test@example.com");

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject token with invalid signature")
        void shouldRejectTokenWithInvalidSignature() {
            // Given - generate token with different secret
            JwtService differentSecretService = new JwtService("DifferentSecretThatWillCauseSignatureFailure123456789", ACCESS_TOKEN_EXPIRATION, REFRESH_TOKEN_EXPIRATION);
            String tokenWithDifferentSignature = differentSecretService.generateAccessToken(testUser);

            // When
            boolean isValid = jwtService.isTokenValid(tokenWithDifferentSignature, "test@example.com");

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject expired token")
        void shouldRejectExpiredToken() {
            // Given - create service with very short expiration
            JwtService shortExpirationService = new JwtService(TEST_SECRET, 1L, REFRESH_TOKEN_EXPIRATION); // 1 second
            String shortLivedToken = shortExpirationService.generateAccessToken(testUser);

            // Wait for token to expire
            try {
                Thread.sleep(2000); // 2 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // When
            boolean isValid = shortExpirationService.isTokenValid(shortLivedToken, "test@example.com");

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should handle null token gracefully")
        void shouldHandleNullTokenGracefully() {
            // When
            boolean isValid = jwtService.isTokenValid(null, "test@example.com");

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should handle empty token gracefully")
        void shouldHandleEmptyTokenGracefully() {
            // When
            boolean isValid = jwtService.isTokenValid("", "test@example.com");

            // Then
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("Claim Extraction Tests")
    class ClaimExtractionTests {

        @Test
        @DisplayName("Should extract username correctly")
        void shouldExtractUsernameCorrectly() {
            // Given
            String token = jwtService.generateAccessToken(testUser);

            // When
            String extractedUsername = jwtService.extractUsername(token);

            // Then
            assertThat(extractedUsername).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Should extract user ID correctly")
        void shouldExtractUserIdCorrectly() {
            // Given
            String token = jwtService.generateAccessToken(testUser);

            // When
            UUID extractedUserId = jwtService.extractUserId(token);

            // Then
            assertThat(extractedUserId).isEqualTo(testUser.getId());
        }

        @Test
        @DisplayName("Should extract tenant ID correctly")
        void shouldExtractTenantIdCorrectly() {
            // Given
            String token = jwtService.generateAccessToken(testUser);

            // When
            UUID extractedTenantId = jwtService.extractTenantId(token);

            // Then
            assertThat(extractedTenantId).isEqualTo(testTenant.getId());
        }

        @Test
        @DisplayName("Should extract role correctly")
        void shouldExtractRoleCorrectly() {
            // Given
            String token = jwtService.generateAccessToken(testUser);

            // When
            String extractedRole = jwtService.extractRole(token);

            // Then
            assertThat(extractedRole).isEqualTo("USER");
        }

        @Test
        @DisplayName("Should return null tenant ID for refresh tokens")
        void shouldReturnNullTenantIdForRefreshTokens() {
            // Given
            String refreshToken = jwtService.generateRefreshToken(testUser);

            // When
            UUID extractedTenantId = jwtService.extractTenantId(refreshToken);

            // Then
            assertThat(extractedTenantId).isNull();
        }

        @Test
        @DisplayName("Should throw exception for malformed token claims")
        void shouldThrowExceptionForMalformedTokenClaims() {
            // Given
            String malformedToken = "malformed.jwt.token";

            // When & Then
            assertThatExceptionOfType(JwtException.class)
                    .isThrownBy(() -> jwtService.extractUsername(malformedToken));

            assertThatExceptionOfType(JwtException.class)
                    .isThrownBy(() -> jwtService.extractUserId(malformedToken));

            assertThatExceptionOfType(JwtException.class)
                    .isThrownBy(() -> jwtService.extractTenantId(malformedToken));
        }

        @Test
        @DisplayName("Should get expiration date correctly")
        void shouldGetExpirationDateCorrectly() {
            // Given
            String token = jwtService.generateAccessToken(testUser);

            // When
            Date expirationDate = jwtService.getExpirationDate(token);

            // Then
            assertThat(expirationDate).isNotNull();
            assertThat(expirationDate).isAfter(new Date());
            
            // Should be approximately 1 hour from now
            long currentTime = System.currentTimeMillis();
            long expectedExpiration = currentTime + (ACCESS_TOKEN_EXPIRATION * 1000);
            assertThat(expirationDate.getTime()).isBetween(expectedExpiration - 5000, expectedExpiration + 5000);
        }
    }

    @Nested
    @DisplayName("Security Edge Cases")
    class SecurityEdgeCaseTests {

        @Test
        @DisplayName("Should prevent token type confusion attacks")
        void shouldPreventTokenTypeConfusionAttacks() {
            // Given
            String accessToken = jwtService.generateAccessToken(testUser);
            String refreshToken = jwtService.generateRefreshToken(testUser);

            // When & Then
            assertThat(jwtService.isRefreshToken(accessToken)).isFalse();
            assertThat(jwtService.isRefreshToken(refreshToken)).isTrue();
        }

        @Test
        @DisplayName("Should handle JWT with none algorithm attack")
        void shouldHandleJwtWithNoneAlgorithmAttack() {
            // Given - manually crafted JWT with "none" algorithm (security attack)
            String noneAlgorithmJwt = "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwidXNlcklkIjoiMTIzNCIsInJvbGUiOiJBRE1JTiJ9.";

            // When & Then
            assertThat(jwtService.isTokenValid(noneAlgorithmJwt, "test@example.com")).isFalse();
            
            assertThatExceptionOfType(JwtException.class)
                    .isThrownBy(() -> jwtService.extractClaims(noneAlgorithmJwt));
        }

        @Test
        @DisplayName("Should handle algorithm confusion attack")
        void shouldHandleAlgorithmConfusionAttack() {
            // Given - Create a JWT that might be valid with different algorithm (RS256 vs HS256)
            String suspiciousToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIn0.invalid_signature";

            // When & Then - should reject tokens with different algorithms
            assertThat(jwtService.isTokenValid(suspiciousToken, "test@example.com")).isFalse();
        }

        @Test
        @DisplayName("Should validate token signature integrity")
        void shouldValidateTokenSignatureIntegrity() {
            // Given
            String validToken = jwtService.generateAccessToken(testUser);
            
            // Tamper with the token by changing multiple characters in the signature
            String[] parts = validToken.split("\\.");
            String tamperedSignature = "invalidSignatureValueThatWillFailValidation";
            String tamperedToken = parts[0] + "." + parts[1] + "." + tamperedSignature;

            // When & Then
            assertThat(jwtService.isTokenValid(tamperedToken, "test@example.com")).isFalse();
            
            assertThatExceptionOfType(JwtException.class)
                    .isThrownBy(() -> jwtService.extractClaims(tamperedToken));
        }

        @Test
        @DisplayName("Should validate token payload integrity")
        void shouldValidateTokenPayloadIntegrity() {
            // Given
            String validToken = jwtService.generateAccessToken(testUser);
            
            // Tamper with the payload by changing one character
            String[] parts = validToken.split("\\.");
            String tamperedPayload = parts[1].substring(0, parts[1].length() - 1) + "X";
            String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

            // When & Then
            assertThat(jwtService.isTokenValid(tamperedToken, "test@example.com")).isFalse();
            
            assertThatExceptionOfType(JwtException.class)
                    .isThrownBy(() -> jwtService.extractClaims(tamperedToken));
        }

        @Test
        @DisplayName("Should handle extremely large tokens")
        void shouldHandleExtremelyLargeTokens() {
            // Given - create user with very long name to test token size limits
            User userWithLongName = new User();
            userWithLongName.setId(UUID.randomUUID());
            userWithLongName.setEmail("test@example.com");
            userWithLongName.setFirstName("A".repeat(1000)); // Very long first name
            userWithLongName.setLastName("B".repeat(1000)); // Very long last name
            userWithLongName.setRole(User.UserRole.USER);
            userWithLongName.setTenant(testTenant);

            // When & Then - should handle large tokens gracefully
            assertThatNoException().isThrownBy(() -> {
                String largeToken = jwtService.generateAccessToken(userWithLongName);
                assertThat(largeToken).isNotNull();
                assertThat(jwtService.isTokenValid(largeToken, "test@example.com")).isTrue();
            });
        }
    }

    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {

        @Test
        @DisplayName("Should return correct access token expiration")
        void shouldReturnCorrectAccessTokenExpiration() {
            // When
            long expiration = jwtService.getAccessTokenExpiration();

            // Then
            assertThat(expiration).isEqualTo(ACCESS_TOKEN_EXPIRATION);
        }

        @Test
        @DisplayName("Should handle different secret key configurations")
        void shouldHandleDifferentSecretKeyConfigurations() {
            // Given
            String alternativeSecret = "AlternativeSecretKeyForJWTThatIsAtLeast256BitsLongForSecurityTesting";
            JwtService alternativeService = new JwtService(alternativeSecret, ACCESS_TOKEN_EXPIRATION, REFRESH_TOKEN_EXPIRATION);

            // When
            String token1 = jwtService.generateAccessToken(testUser);
            String token2 = alternativeService.generateAccessToken(testUser);

            // Then - tokens from different services should not validate each other
            assertThat(jwtService.isTokenValid(token2, "test@example.com")).isFalse();
            assertThat(alternativeService.isTokenValid(token1, "test@example.com")).isFalse();
        }

        @Test
        @DisplayName("Should handle custom expiration times")
        void shouldHandleCustomExpirationTimes() {
            // Given
            long customAccessExpiration = 7200L; // 2 hours
            long customRefreshExpiration = 1209600L; // 14 days
            JwtService customService = new JwtService(TEST_SECRET, customAccessExpiration, customRefreshExpiration);

            // When
            String accessToken = customService.generateAccessToken(testUser);
            Date expirationDate = customService.getExpirationDate(accessToken);

            // Then
            long currentTime = System.currentTimeMillis();
            long expectedExpiration = currentTime + (customAccessExpiration * 1000);
            assertThat(expirationDate.getTime()).isBetween(expectedExpiration - 5000, expectedExpiration + 5000);
            assertThat(customService.getAccessTokenExpiration()).isEqualTo(customAccessExpiration);
        }
    }
}
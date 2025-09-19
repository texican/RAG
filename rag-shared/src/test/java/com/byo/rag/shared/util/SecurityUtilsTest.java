package com.byo.rag.shared.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for SecurityUtils functionality including password hashing,
 * token generation, validation, and sanitization utilities.
 */
class SecurityUtilsTest {

    @Test
    @DisplayName("Should hash passwords securely using BCrypt")
    void shouldHashPasswordsSecurelyUsingBCrypt() {
        String plainPassword = "TestPassword123!";
        
        String hashedPassword = SecurityUtils.hashPassword(plainPassword);
        
        assertNotNull(hashedPassword, "Hashed password should not be null");
        assertNotEquals(plainPassword, hashedPassword, "Hashed password should differ from plain password");
        assertTrue(hashedPassword.startsWith("$2a$12$"), "Should use BCrypt with 12 rounds");
        assertTrue(hashedPassword.length() >= 60, "BCrypt hash should be at least 60 characters");
    }

    @Test
    @DisplayName("Should verify passwords correctly")
    void shouldVerifyPasswordsCorrectly() {
        String plainPassword = "MySecurePassword123!";
        String wrongPassword = "WrongPassword456!";
        
        // Hash the password
        String hashedPassword = SecurityUtils.hashPassword(plainPassword);
        
        // Verify correct password
        assertTrue(SecurityUtils.verifyPassword(plainPassword, hashedPassword), 
            "Should verify correct password");
        
        // Verify incorrect password
        assertFalse(SecurityUtils.verifyPassword(wrongPassword, hashedPassword), 
            "Should reject incorrect password");
    }

    @Test
    @DisplayName("Should generate unique API keys with correct format")
    void shouldGenerateUniqueApiKeysWithCorrectFormat() {
        String apiKey1 = SecurityUtils.generateApiKey();
        String apiKey2 = SecurityUtils.generateApiKey();
        
        // Check format
        assertNotNull(apiKey1, "API key should not be null");
        assertTrue(apiKey1.startsWith("rag_"), "API key should start with 'rag_' prefix");
        assertTrue(apiKey1.length() > 10, "API key should be sufficiently long");
        
        // Check uniqueness
        assertNotEquals(apiKey1, apiKey2, "API keys should be unique");
        
        // Check that both follow the same format
        assertTrue(apiKey2.startsWith("rag_"), "Second API key should also start with 'rag_' prefix");
    }

    @Test
    @DisplayName("Should generate verification tokens with correct format")
    void shouldGenerateVerificationTokensWithCorrectFormat() {
        String token1 = SecurityUtils.generateVerificationToken();
        String token2 = SecurityUtils.generateVerificationToken();
        
        // Check format
        assertNotNull(token1, "Verification token should not be null");
        assertEquals(32, token1.length(), "Verification token should be 32 characters");
        assertTrue(token1.matches("[a-f0-9]+"), "Verification token should be hexadecimal");
        assertFalse(token1.contains("-"), "Verification token should not contain hyphens");
        
        // Check uniqueness
        assertNotEquals(token1, token2, "Verification tokens should be unique");
    }

    @Test
    @DisplayName("Should generate secure tokens of specified length")
    void shouldGenerateSecureTokensOfSpecifiedLength() {
        int[] lengths = {16, 24, 32, 64};
        
        for (int length : lengths) {
            String token = SecurityUtils.generateSecureToken(length);
            
            assertNotNull(token, "Token should not be null for length " + length);
            assertFalse(token.isEmpty(), "Token should not be empty for length " + length);
            
            // Base64 URL-safe encoded length calculation
            // Each 3 bytes becomes 4 characters, but without padding, it's variable
            int expectedMinLength = (length * 4) / 3;
            assertTrue(token.length() >= expectedMinLength - 2, 
                "Token length should be appropriate for " + length + " bytes");
        }
    }

    @Test
    @DisplayName("Should validate password strength correctly")
    void shouldValidatePasswordStrengthCorrectly() {
        // Valid passwords
        assertTrue(SecurityUtils.isValidPassword("Password123!"), "Should accept password with all requirements");
        assertTrue(SecurityUtils.isValidPassword("MyStr0ng@Pass"), "Should accept another valid password");
        assertTrue(SecurityUtils.isValidPassword("C0mplex#P4ssw0rd"), "Should accept complex password");
        
        // Invalid passwords - too short
        assertFalse(SecurityUtils.isValidPassword("Pas1!"), "Should reject password too short");
        assertFalse(SecurityUtils.isValidPassword(""), "Should reject empty password");
        
        // Invalid passwords - missing requirements
        assertFalse(SecurityUtils.isValidPassword("password123!"), "Should reject password without uppercase");
        assertFalse(SecurityUtils.isValidPassword("PASSWORD123!"), "Should reject password without lowercase");
        assertFalse(SecurityUtils.isValidPassword("Password!"), "Should reject password without digit");
        assertFalse(SecurityUtils.isValidPassword("Password123"), "Should reject password without special character");
        
        // Null password
        assertFalse(SecurityUtils.isValidPassword(null), "Should reject null password");
    }

    @Test
    @DisplayName("Should sanitize slugs correctly")
    void shouldSanitizeSlugsCorrectly() {
        // Valid transformations
        assertEquals("my-company-llc", SecurityUtils.sanitizeSlug("My Company LLC!"), 
            "Should convert to lowercase and replace spaces/special chars");
        assertEquals("special-characters-123", SecurityUtils.sanitizeSlug("  Special@Characters#123  "), 
            "Should handle multiple special characters and trim");
        assertEquals("multiple-hyphens", SecurityUtils.sanitizeSlug("---multiple---hyphens---"), 
            "Should collapse multiple hyphens and trim");
        
        // Edge cases
        assertEquals("", SecurityUtils.sanitizeSlug(null), "Should handle null input");
        assertEquals("", SecurityUtils.sanitizeSlug(""), "Should handle empty input");
        assertEquals("", SecurityUtils.sanitizeSlug("!@#$%^&*()"), "Should handle only special characters");
        assertEquals("abc123", SecurityUtils.sanitizeSlug("abc123"), "Should leave valid slug unchanged");
        assertEquals("test-123", SecurityUtils.sanitizeSlug("test-123"), "Should preserve existing hyphens");
    }

    @Test
    @DisplayName("Should handle concurrent password operations safely")
    void shouldHandleConcurrentPasswordOperationsSafely() throws InterruptedException {
        String testPassword = "ConcurrentTest123!";
        String[] results = new String[5];
        Thread[] threads = new Thread[5];
        
        // Create multiple threads doing password operations
        for (int i = 0; i < 5; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                results[index] = SecurityUtils.hashPassword(testPassword);
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for completion
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Verify all operations completed successfully and results are unique
        for (int i = 0; i < 5; i++) {
            assertNotNull(results[i], "Thread " + i + " should have produced a result");
            assertTrue(SecurityUtils.verifyPassword(testPassword, results[i]), 
                "Result " + i + " should verify correctly");
            
            // Check uniqueness (BCrypt should produce different salts)
            for (int j = i + 1; j < 5; j++) {
                assertNotEquals(results[i], results[j], 
                    "Results should be unique due to different salts");
            }
        }
    }

    @Test
    @DisplayName("Should generate cryptographically secure random values")
    void shouldGenerateCryptographicallySecureRandomValues() {
        // Generate multiple API keys and verify they don't follow patterns
        String[] apiKeys = new String[10];
        for (int i = 0; i < 10; i++) {
            apiKeys[i] = SecurityUtils.generateApiKey();
        }
        
        // Check that all are unique
        for (int i = 0; i < 10; i++) {
            for (int j = i + 1; j < 10; j++) {
                assertNotEquals(apiKeys[i], apiKeys[j], "API keys should be unique");
            }
        }
        
        // Generate multiple tokens of the same length and verify uniqueness
        String[] tokens = new String[10];
        for (int i = 0; i < 10; i++) {
            tokens[i] = SecurityUtils.generateSecureToken(16);
        }
        
        for (int i = 0; i < 10; i++) {
            for (int j = i + 1; j < 10; j++) {
                assertNotEquals(tokens[i], tokens[j], "Secure tokens should be unique");
            }
        }
    }

    @Test
    @DisplayName("Should handle edge cases gracefully")
    void shouldHandleEdgeCasesGracefully() {
        // Password hashing with edge cases
        assertThrows(IllegalArgumentException.class, () -> {
            SecurityUtils.hashPassword(null);
        }, "Should throw exception for null password");
        
        // Test zero length token generation
        assertDoesNotThrow(() -> {
            String zeroToken = SecurityUtils.generateSecureToken(0);
            assertNotNull(zeroToken);
            assertEquals("", zeroToken, "Zero length token should be empty");
        }, "Should handle zero length token generation");
        
        // Test negative length - should throw
        assertThrows(Exception.class, () -> {
            SecurityUtils.generateSecureToken(-1);
        }, "Should handle negative length appropriately");
        
        // Valid edge cases
        boolean emptyVerification = SecurityUtils.verifyPassword("", SecurityUtils.hashPassword("test"));
        assertFalse(emptyVerification, "Should handle empty password verification");
        
        String singleByteToken = SecurityUtils.generateSecureToken(1);
        assertNotNull(singleByteToken, "Should generate token for single byte");
        assertTrue(singleByteToken.length() > 0, "Single byte token should have length > 0");
        
        // Test password verification with null password
        assertThrows(Exception.class, () -> {
            SecurityUtils.verifyPassword(null, "hash");
        }, "Should throw exception for null password in verification");
        
        // Test password verification with null hash - this returns false instead of throwing
        boolean nullHashResult = SecurityUtils.verifyPassword("pass", null);
        assertFalse(nullHashResult, "Should return false for null hash");
    }
}
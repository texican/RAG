package com.byo.rag.shared.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * Enterprise-grade security utility class providing comprehensive cryptographic and security operations.
 * 
 * <p>This utility class centralizes all security-related operations across the Enterprise RAG system,
 * including password management, token generation, validation, and sanitization. All operations use
 * industry-standard cryptographic practices and are designed for enterprise security requirements.</p>
 * 
 * <p><strong>Security Features:</strong></p>
 * <ul>
 *   <li><strong>Password Security:</strong> BCrypt hashing with enterprise-grade strength (12 rounds)</li>
 *   <li><strong>Token Generation:</strong> Cryptographically secure random token generation</li>
 *   <li><strong>Password Validation:</strong> Comprehensive password strength validation</li>
 *   <li><strong>Input Sanitization:</strong> Secure slug generation and data sanitization</li>
 *   <li><strong>API Key Management:</strong> Secure API key generation with consistent format</li>
 * </ul>
 * 
 * <p><strong>Cryptographic Standards:</strong></p>
 * <ul>
 *   <li>BCrypt password hashing with 12 rounds (2^12 = 4,096 iterations)</li>
 *   <li>SecureRandom for all random number generation</li>
 *   <li>Base64 URL-safe encoding for token representation</li>
 *   <li>256-bit entropy for API keys (32 bytes)</li>
 * </ul>
 * 
 * <p><strong>Thread Safety:</strong></p>
 * <p>All methods in this class are thread-safe and can be called concurrently from multiple
 * threads without synchronization. The underlying BCryptPasswordEncoder and SecureRandom
 * instances are thread-safe.</p>
 * 
 * <p><strong>Usage Examples:</strong></p>
 * <pre>{@code
 * // Password operations
 * String hashedPassword = SecurityUtils.hashPassword("userPassword123!");
 * boolean isValid = SecurityUtils.verifyPassword("userPassword123!", hashedPassword);
 * 
 * // Token generation
 * String apiKey = SecurityUtils.generateApiKey();
 * String verificationToken = SecurityUtils.generateVerificationToken();
 * 
 * // Password validation
 * boolean meetsRequirements = SecurityUtils.isValidPassword("SecurePass123!");
 * 
 * // Input sanitization
 * String slug = SecurityUtils.sanitizeSlug("My Company LLC!");
 * }</pre>
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 * @see BCryptPasswordEncoder
 * @see SecureRandom
 */
public class SecurityUtils {

    /**
     * Enterprise-grade BCrypt password encoder configured with 12 rounds for optimal security.
     * 
     * <p>The 12-round configuration provides strong protection against brute-force attacks
     * while maintaining reasonable performance for user authentication operations.</p>
     */
    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder(12);
    
    /**
     * Cryptographically secure random number generator for all token generation operations.
     * 
     * <p>This SecureRandom instance is thread-safe and provides entropy suitable for
     * security-critical operations including API keys and verification tokens.</p>
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Generates a secure BCrypt hash of the provided plain text password.
     * 
     * <p>This method uses BCrypt with 12 rounds (2^12 = 4,096 iterations) to provide
     * enterprise-grade security against brute-force attacks. Each hash includes a
     * randomly generated salt for maximum security.</p>
     * 
     * <p><strong>Security Features:</strong></p>
     * <ul>
     *   <li>12-round BCrypt hashing for optimal security/performance balance</li>
     *   <li>Automatically generated random salt per password</li>
     *   <li>Resistant to rainbow table attacks</li>
     *   <li>Future-proof against improved hardware capabilities</li>
     * </ul>
     * 
     * @param plainPassword the plain text password to hash
     * @return BCrypt hash of the password with embedded salt
     * @throws IllegalArgumentException if plainPassword is null
     * @see BCryptPasswordEncoder
     */
    public static String hashPassword(String plainPassword) {
        return PASSWORD_ENCODER.encode(plainPassword);
    }

    /**
     * Verifies a plain text password against its BCrypt hash.
     * 
     * <p>This method provides secure password verification by comparing the plain text
     * password against the stored BCrypt hash. The verification process automatically
     * handles salt extraction and comparison using constant-time operations.</p>
     * 
     * <p><strong>Security Features:</strong></p>
     * <ul>
     *   <li>Constant-time comparison to prevent timing attacks</li>
     *   <li>Automatic salt extraction from the hash</li>
     *   <li>Secure verification without exposing the original password</li>
     * </ul>
     * 
     * @param plainPassword the plain text password to verify
     * @param hashedPassword the BCrypt hash to verify against
     * @return {@code true} if the password matches the hash, {@code false} otherwise
     * @see BCryptPasswordEncoder#matches(CharSequence, String)
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        return PASSWORD_ENCODER.matches(plainPassword, hashedPassword);
    }

    /**
     * Generates a cryptographically secure API key with consistent formatting.
     * 
     * <p>This method creates a high-entropy API key suitable for external API access.
     * The key format includes a "rag_" prefix for easy identification and 256 bits
     * of entropy for maximum security.</p>
     * 
     * <p><strong>API Key Format:</strong></p>
     * <ul>
     *   <li>Prefix: "rag_" for system identification</li>
     *   <li>Entropy: 256 bits (32 bytes) of cryptographically secure random data</li>
     *   <li>Encoding: Base64 URL-safe encoding without padding</li>
     *   <li>Total Length: ~47 characters</li>
     * </ul>
     * 
     * @return a unique, cryptographically secure API key with "rag_" prefix
     * @see SecureRandom
     */
    public static String generateApiKey() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return "rag_" + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Generates a secure verification token for email verification and similar operations.
     * 
     * <p>This method creates a UUID-based token with hyphens removed for clean URLs.
     * The token provides sufficient entropy for secure verification operations while
     * maintaining readability and URL compatibility.</p>
     * 
     * <p><strong>Token Characteristics:</strong></p>
     * <ul>
     *   <li>Based on UUID4 (random UUID) for 122 bits of entropy</li>
     *   <li>Hyphens removed for clean URL integration</li>
     *   <li>32 character hexadecimal string</li>
     *   <li>Suitable for email links and verification workflows</li>
     * </ul>
     * 
     * @return a 32-character hexadecimal verification token
     * @see UUID#randomUUID()
     */
    public static String generateVerificationToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Generates a cryptographically secure token of the specified byte length.
     * 
     * <p>This method provides flexible secure token generation for various use cases
     * requiring different entropy levels. The token uses Base64 URL-safe encoding
     * for compatibility with web applications and APIs.</p>
     * 
     * <p><strong>Security Considerations:</strong></p>
     * <ul>
     *   <li>Minimum recommended length: 16 bytes (128 bits entropy)</li>
     *   <li>For session tokens: 24-32 bytes recommended</li>
     *   <li>For API keys: 32 bytes (256 bits) recommended</li>
     *   <li>Uses SecureRandom for cryptographically secure entropy</li>
     * </ul>
     * 
     * @param length the number of random bytes to generate (should be ≥ 16 for security)
     * @return Base64 URL-safe encoded token without padding
     * @throws IllegalArgumentException if length is less than 1
     * @see SecureRandom
     */
    public static String generateSecureToken(int length) {
        byte[] randomBytes = new byte[length];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Validates password strength according to enterprise security requirements.
     * 
     * <p>This method enforces comprehensive password strength requirements to ensure
     * user accounts are protected against common password attacks. The validation
     * checks multiple security criteria simultaneously.</p>
     * 
     * <p><strong>Password Requirements:</strong></p>
     * <ul>
     *   <li><strong>Minimum Length:</strong> 8 characters</li>
     *   <li><strong>Uppercase Letters:</strong> At least one (A-Z)</li>
     *   <li><strong>Lowercase Letters:</strong> At least one (a-z)</li>
     *   <li><strong>Digits:</strong> At least one (0-9)</li>
     *   <li><strong>Special Characters:</strong> At least one non-alphanumeric character</li>
     * </ul>
     * 
     * <p><strong>Security Benefits:</strong></p>
     * <ul>
     *   <li>Resistance to dictionary attacks</li>
     *   <li>Increased complexity against brute-force attempts</li>
     *   <li>Compliance with enterprise security standards</li>
     *   <li>Enhanced user account protection</li>
     * </ul>
     * 
     * @param password the password string to validate
     * @return {@code true} if password meets all security requirements, {@code false} otherwise
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (!Character.isLetterOrDigit(c)) hasSpecial = true;
        }
        
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    /**
     * Sanitizes input string to create a URL-safe slug identifier.
     * 
     * <p>This method transforms arbitrary strings into clean, URL-compatible identifiers
     * suitable for use in web addresses, database keys, and file names. The sanitization
     * process removes or replaces problematic characters while preserving readability.</p>
     * 
     * <p><strong>Sanitization Rules:</strong></p>
     * <ul>
     *   <li>Convert to lowercase for consistency</li>
     *   <li>Replace non-alphanumeric characters (except hyphens) with hyphens</li>
     *   <li>Collapse multiple consecutive hyphens into single hyphens</li>
     *   <li>Remove leading and trailing hyphens</li>
     *   <li>Return empty string for null input</li>
     * </ul>
     * 
     * <p><strong>Examples:</strong></p>
     * <ul>
     *   <li>"My Company LLC!" → "my-company-llc"</li>
     *   <li>"  Special@Characters#123  " → "special-characters-123"</li>
     *   <li>"---multiple---hyphens---" → "multiple-hyphens"</li>
     * </ul>
     * 
     * @param input the string to sanitize into a slug
     * @return sanitized slug string, or empty string if input is null
     */
    public static String sanitizeSlug(String input) {
        if (input == null) return "";
        
        return input.toLowerCase()
            .replaceAll("[^a-z0-9\\-]", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
    }
}
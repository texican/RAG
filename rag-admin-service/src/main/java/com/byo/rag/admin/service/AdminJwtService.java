package com.byo.rag.admin.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * Administrative JWT service for secure token generation and validation in the admin module.
 * 
 * <p>This service provides comprehensive JWT functionality specifically tailored for
 * administrative authentication in the Enterprise RAG system. It handles token lifecycle
 * management, cryptographic operations, and claims processing for admin users with
 * enhanced security features and validation.
 * 
 * <p><strong>Administrative Token Architecture:</strong>
 * <ul>
 *   <li><strong>Role-Based Claims:</strong> Tokens contain administrative role information</li>
 *   <li><strong>Secure Signing:</strong> HMAC-SHA256 cryptographic signature</li>
 *   <li><strong>Configurable Expiration:</strong> Flexible token lifetime management</li>
 *   <li><strong>Comprehensive Validation:</strong> Multi-layer token integrity checking</li>
 * </ul>
 * 
 * <p><strong>Security Features:</strong>
 * <ul>
 *   <li><strong>Cryptographic Security:</strong> Strong HMAC-SHA256 signing algorithm</li>
 *   <li><strong>Tamper Detection:</strong> Signature verification prevents token modification</li>
 *   <li><strong>Expiration Management:</strong> Automatic token lifetime enforcement</li>
 *   <li><strong>Error Handling:</strong> Secure failure modes without information leakage</li>
 * </ul>
 * 
 * <p><strong>Token Structure:</strong>
 * Administrative JWT tokens contain:
 * <ul>
 *   <li><strong>Subject:</strong> Administrator username/email identifier</li>
 *   <li><strong>Roles:</strong> Administrative role list for authorization</li>
 *   <li><strong>Issued At:</strong> Token creation timestamp</li>
 *   <li><strong>Expiration:</strong> Token validity end time</li>
 *   <li><strong>Signature:</strong> HMAC-SHA256 integrity protection</li>
 * </ul>
 * 
 * <p><strong>Administrative Integration:</strong>
 * <ul>
 *   <li><strong>Controller Integration:</strong> Used by AdminAuthController for login/refresh</li>
 *   <li><strong>Security Filter:</strong> Token validation in security filters</li>
 *   <li><strong>Role Authorization:</strong> Claims extraction for access control</li>
 *   <li><strong>Session Management:</strong> Stateless authentication support</li>
 * </ul>
 * 
 * <p><strong>Configuration:</strong>
 * <ul>
 *   <li><code>spring.security.jwt.secret</code>: Cryptographic signing key</li>
 *   <li><code>spring.security.jwt.expiration</code>: Token lifetime in milliseconds</li>
 * </ul>
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.0
 * @see Claims
 * @see SecretKey
 * @see AdminAuthController
 */
@Service
public class AdminJwtService {

    /** Cryptographic secret key for JWT token signing and verification. */
    private final SecretKey secretKey;
    
    /** Token expiration time in milliseconds. */
    private final long expirationTime;

    /**
     * Constructs an administrative JWT service with configurable security settings.
     * 
     * <p>The service initializes with a cryptographic secret key and expiration time,
     * providing secure token operations for administrative authentication workflows.
     * 
     * @param secret the secret key string for HMAC-SHA256 signing
     * @param expirationTime token validity period in milliseconds
     */
    public AdminJwtService(@Value("${spring.security.jwt.secret}") String secret,
                          @Value("${spring.security.jwt.expiration}") long expirationTime) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationTime = expirationTime;
    }

    /**
     * Generates a new administrative JWT token with user identity and role claims.
     * 
     * <p>This method creates a signed JWT token containing administrative user information
     * and role claims for secure authentication and authorization. The token is
     * cryptographically signed and includes expiration management.
     * 
     * <p><strong>Token Generation Process:</strong>
     * <ol>
     *   <li><strong>Timestamp Creation:</strong> Current time for issued-at claim</li>
     *   <li><strong>Expiration Calculation:</strong> Future timestamp based on configuration</li>
     *   <li><strong>Claims Assembly:</strong> Username, roles, and temporal claims</li>
     *   <li><strong>Cryptographic Signing:</strong> HMAC-SHA256 signature application</li>
     *   <li><strong>Token Compaction:</strong> Base64-encoded JWT string generation</li>
     * </ol>
     * 
     * <p><strong>Token Claims:</strong>
     * <ul>
     *   <li><strong>Subject (sub):</strong> Administrator username/email</li>
     *   <li><strong>Roles:</strong> List of administrative roles for authorization</li>
     *   <li><strong>Issued At (iat):</strong> Token creation timestamp</li>
     *   <li><strong>Expiration (exp):</strong> Token validity end time</li>
     * </ul>
     * 
     * @param username the administrator's username/email identifier
     * @param roles list of administrative roles for this user
     * @return compact JWT token string ready for HTTP transmission
     */
    public String generateToken(String username, List<String> roles) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Extracts the username (subject) from an administrative JWT token.
     * 
     * <p>This method retrieves the administrator's username from the token's subject claim,
     * providing user identification for authentication and authorization workflows.
     * The extraction process includes token validation and signature verification.
     * 
     * <p><strong>Security Validation:</strong>
     * <ul>
     *   <li>Token structure and format validation</li>
     *   <li>Cryptographic signature verification</li>
     *   <li>Claims parsing and extraction</li>
     * </ul>
     * 
     * @param token the JWT token containing username information
     * @return the administrator's username from the token subject
     * @throws RuntimeException if token is invalid or username cannot be extracted
     */
    public String extractUsername(String token) {
        try {
            return extractClaims(token).getSubject();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract username from token", e);
        }
    }

    /**
     * Extracts administrative roles from a JWT token for authorization decisions.
     * 
     * <p>This method retrieves the list of administrative roles from the token's custom
     * claims, enabling role-based access control and authorization logic throughout
     * the administrative interface.
     * 
     * <p><strong>Role-Based Authorization:</strong>
     * <ul>
     *   <li>Enables fine-grained access control</li>
     *   <li>Supports multiple administrative role levels</li>
     *   <li>Integrates with Spring Security authorization</li>
     * </ul>
     * 
     * @param token the JWT token containing role information
     * @return list of administrative roles for the user
     * @throws RuntimeException if token is invalid or roles cannot be extracted
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        try {
            return (List<String>) extractClaims(token).get("roles");
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract roles from token", e);
        }
    }

    /**
     * Validates a JWT token for authenticity, structure, and expiration.
     * 
     * <p>This method performs comprehensive token validation including null checking,
     * signature verification, structure validation, and expiration time verification.
     * It provides safe validation with exception handling for security filters.
     * 
     * <p><strong>Validation Checks:</strong>
     * <ul>
     *   <li><strong>Null/Empty Check:</strong> Validates token presence</li>
     *   <li><strong>Structure Validation:</strong> JWT format and claim structure</li>
     *   <li><strong>Signature Verification:</strong> Cryptographic authenticity</li>
     *   <li><strong>Expiration Check:</strong> Current time vs expiration time</li>
     * </ul>
     * 
     * <p><strong>Security Features:</strong>
     * <ul>
     *   <li>Safe exception handling prevents information disclosure</li>
     *   <li>Returns false for any validation failure</li>
     *   <li>Suitable for use in security filters and authentication logic</li>
     * </ul>
     * 
     * @param token the JWT token to validate
     * @return true if token is valid and not expired, false otherwise
     */
    public boolean isTokenValid(String token) {
        // Check for null or empty token
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        try {
            // Extract claims and verify signature/structure
            Claims claims = extractClaims(token);
            
            // Check if token has not expired
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            // Any exception during validation indicates invalid token
            return false;
        }
    }

    /**
     * Extracts and validates claims from a JWT token with signature verification.
     * 
     * <p>This private method handles the core JWT parsing and validation logic,
     * verifying the token signature and extracting the claims payload for use
     * by other methods in this service.
     * 
     * <p><strong>Processing Steps:</strong>
     * <ul>
     *   <li>JWT parser creation with signature verification</li>
     *   <li>Token parsing and structure validation</li>
     *   <li>Signature verification using stored secret key</li>
     *   <li>Claims payload extraction and return</li>
     * </ul>
     * 
     * @param token the JWT token to parse and validate
     * @return Claims object containing all token claims
     * @throws io.jsonwebtoken.JwtException if token is invalid, expired, or tampered
     */
    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
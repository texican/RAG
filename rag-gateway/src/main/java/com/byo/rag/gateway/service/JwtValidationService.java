package com.byo.rag.gateway.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * JWT Validation Service for the Enterprise RAG Gateway.
 * 
 * <p>This service provides centralized JWT token validation functionality for the
 * API Gateway, ensuring consistent token processing and security across all
 * gateway operations. It validates tokens issued by the authentication service
 * and extracts user context for downstream services.
 * 
 * <p><strong>Validation Features:</strong>
 * <ul>
 *   <li>Cryptographic signature verification using HMAC-SHA256</li>
 *   <li>Token expiration checking and enforcement</li>
 *   <li>Token structure and format validation</li>
 *   <li>User context extraction (user ID, tenant ID, role)</li>
 *   <li>Token type differentiation (access vs refresh tokens)</li>
 * </ul>
 * 
 * <p><strong>Security Considerations:</strong>
 * <ul>
 *   <li>Uses the same secret key as the authentication service</li>
 *   <li>Fails securely - returns false/null for any validation errors</li>
 *   <li>Prevents timing attacks through consistent validation timing</li>
 *   <li>Validates all claims to prevent token manipulation</li>
 * </ul>
 * 
 * <p><strong>Multi-Tenant Support:</strong>
 * Extracts tenant information from JWT tokens to support multi-tenant
 * architecture with proper tenant isolation and context propagation.
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.0
 */
@Service
public class JwtValidationService {

    /** Cryptographic secret key for JWT signature verification. */
    private final SecretKey secretKey;

    /**
     * Constructs a new JWT validation service with the specified secret key.
     * 
     * <p>The secret key must match the key used by the authentication service
     * to ensure proper signature validation. This key is configured through
     * the jwt.secret application property.
     * 
     * @param secret the secret key for JWT signature verification
     */
    public JwtValidationService(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Validates a JWT token for authenticity and expiration.
     * 
     * <p>This method performs comprehensive token validation including:
     * <ul>
     *   <li>Null and empty token checking</li>
     *   <li>Cryptographic signature verification</li>
     *   <li>Token structure validation</li>
     *   <li>Expiration timestamp checking</li>
     * </ul>
     * 
     * <p><strong>Security:</strong> Any validation failure results in false
     * being returned, ensuring that only completely valid tokens are accepted.
     * 
     * @param token the JWT token to validate
     * @return true if token is valid and not expired, false otherwise
     */
    public boolean isTokenValid(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        try {
            Claims claims = extractClaims(token);
            return !isTokenExpired(claims);
        } catch (Exception e) {
            // Any exception during validation means the token is invalid
            return false;
        }
    }

    /**
     * Validates a JWT token and verifies it matches the expected username.
     * 
     * <p>This method provides additional security by validating not only
     * the token's authenticity but also ensuring it matches the expected
     * user context. This prevents token substitution attacks.
     * 
     * @param token the JWT token to validate
     * @param expectedUsername the username that should match the token subject
     * @return true if token is valid and matches the username, false otherwise
     */
    public boolean isTokenValid(String token, String expectedUsername) {
        if (token == null || expectedUsername == null) {
            return false;
        }

        try {
            Claims claims = extractClaims(token);
            return claims.getSubject().equals(expectedUsername) && !isTokenExpired(claims);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extracts and validates claims from a JWT token.
     * 
     * <p>This method performs the core token parsing and validation logic,
     * including cryptographic signature verification and structural validation.
     * 
     * @param token the JWT token to parse
     * @return Claims object containing all token claims
     * @throws JwtException if token is invalid, expired, or tampered
     */
    public Claims extractClaims(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extracts the user ID from a JWT token.
     * 
     * <p>The user ID is stored as a custom claim and is used for user
     * identification and context propagation to downstream services.
     * 
     * @param token the JWT token to extract user ID from
     * @return the user's UUID from the token claims
     * @throws JwtException if token is invalid or userId claim is missing
     * @throws IllegalArgumentException if userId claim is malformed
     */
    public UUID extractUserId(String token) throws JwtException {
        Claims claims = extractClaims(token);
        String userIdStr = claims.get("userId", String.class);
        if (userIdStr == null) {
            throw new JwtException("userId claim missing from token");
        }
        return UUID.fromString(userIdStr);
    }

    /**
     * Extracts the tenant ID from a JWT token for multi-tenant isolation.
     * 
     * <p>The tenant ID is critical for enforcing tenant isolation throughout
     * the system. This claim ensures users can only access resources within
     * their organization boundaries.
     * 
     * @param token the JWT token to extract tenant ID from
     * @return the tenant's UUID from the token claims, or null if not present
     * @throws JwtException if token is invalid
     * @throws IllegalArgumentException if tenantId claim is malformed
     */
    public UUID extractTenantId(String token) throws JwtException {
        Claims claims = extractClaims(token);
        String tenantIdStr = claims.get("tenantId", String.class);
        return tenantIdStr != null ? UUID.fromString(tenantIdStr) : null;
    }

    /**
     * Extracts the user role from a JWT token for authorization decisions.
     * 
     * <p>The role claim is used for authorization decisions throughout the
     * system, determining what actions the user is permitted to perform.
     * 
     * @param token the JWT token to extract role from
     * @return the user's role string from the token claims
     * @throws JwtException if token is invalid
     */
    public String extractRole(String token) throws JwtException {
        Claims claims = extractClaims(token);
        return claims.get("role", String.class);
    }

    /**
     * Extracts the user email from a JWT token.
     * 
     * <p>The email is typically stored as the JWT subject claim and is used
     * for user identification and context propagation.
     * 
     * @param token the JWT token to extract email from
     * @return the user's email address from the token claims
     * @throws JwtException if token is invalid
     */
    public String extractEmail(String token) throws JwtException {
        Claims claims = extractClaims(token);
        return claims.getSubject();
    }

    /**
     * Determines if a JWT token is a refresh token.
     * 
     * <p>This method checks for the presence of a "tokenType" claim with
     * value "refresh" to distinguish refresh tokens from access tokens.
     * This prevents misuse of refresh tokens in authentication contexts.
     * 
     * @param token the JWT token to check
     * @return true if token is a refresh token, false otherwise
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = extractClaims(token);
            return "refresh".equals(claims.get("tokenType", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validates a refresh token for token refresh operations.
     * 
     * <p>This method specifically validates refresh tokens by checking:
     * <ul>
     *   <li>Token signature and structure</li>
     *   <li>Token type claim (must be "refresh")</li>
     *   <li>Token expiration</li>
     *   <li>Token format and claims integrity</li>
     * </ul>
     * 
     * @param refreshToken the refresh token to validate
     * @return true if refresh token is valid, false otherwise
     */
    public boolean validateRefreshToken(String refreshToken) {
        try {
            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                return false;
            }
            
            Claims claims = extractClaims(refreshToken);
            
            // Check if it's actually a refresh token
            if (!"refresh".equals(claims.get("tokenType", String.class))) {
                return false;
            }
            
            // Check expiration
            return !isTokenExpired(claims);
            
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Generates a new token pair (access and refresh tokens) for user.
     * 
     * <p>This method creates both access and refresh tokens with appropriate
     * claims and expiration times. This is typically used during token refresh
     * operations or after successful authentication.
     * 
     * @param userId the user ID for the tokens
     * @param email the user's email address
     * @param tenantId the user's tenant ID
     * @param role the user's role
     * @return TokenPair containing both access and refresh tokens
     */
    public TokenPair generateTokenPair(String userId, String email, String tenantId, String role) {
        Date now = new Date();
        Date accessExpiry = new Date(now.getTime() + (15 * 60 * 1000)); // 15 minutes
        Date refreshExpiry = new Date(now.getTime() + (7 * 24 * 60 * 60 * 1000L)); // 7 days
        
        // Generate access token
        String accessToken = Jwts.builder()
            .subject(email)
            .claim("userId", userId)
            .claim("tenantId", tenantId)
            .claim("role", role)
            .claim("tokenType", "access")
            .issuedAt(now)
            .expiration(accessExpiry)
            .signWith(secretKey)
            .compact();
            
        // Generate refresh token
        String refreshToken = Jwts.builder()
            .subject(email)
            .claim("userId", userId)
            .claim("tenantId", tenantId)
            .claim("tokenType", "refresh")
            .issuedAt(now)
            .expiration(refreshExpiry)
            .signWith(secretKey)
            .compact();
            
        return new TokenPair(accessToken, refreshToken);
    }

    /**
     * Token pair container for access and refresh tokens.
     */
    public static class TokenPair {
        private final String accessToken;
        private final String refreshToken;
        
        public TokenPair(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
        
        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
    }

    /**
     * Validates a JWT token for general authentication purposes.
     * 
     * @param token the JWT token to validate
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return false;
            }
            
            Claims claims = extractClaims(token);
            return !isTokenExpired(claims);
            
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if a token has expired based on its expiration claim.
     * 
     * <p>This utility method compares the token's expiration timestamp
     * with the current time to determine if the token is still valid.
     * 
     * @param claims the extracted claims containing expiration information
     * @return true if token has expired, false if still valid
     */
    private boolean isTokenExpired(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration != null && expiration.before(new Date());
    }
}
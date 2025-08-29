package com.enterprise.rag.auth.security;

import com.enterprise.rag.shared.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service class for JWT token generation, validation, and management in the Enterprise RAG system.
 * 
 * <p>This service provides comprehensive JWT functionality for secure authentication and authorization.
 * It handles both access tokens (short-lived for API calls) and refresh tokens (longer-lived for
 * token renewal) with cryptographic signing and validation.
 * 
 * <p><strong>Token Architecture:</strong>
 * <ul>
 *   <li><strong>Access Tokens:</strong> Short-lived (default 1 hour) tokens containing full user context</li>
 *   <li><strong>Refresh Tokens:</strong> Long-lived (default 7 days) tokens for generating new access tokens</li>
 *   <li><strong>Multi-Tenant Claims:</strong> Tokens include tenant isolation information</li>
 *   <li><strong>Role-Based Claims:</strong> User roles embedded for authorization decisions</li>
 * </ul>
 * 
 * <p><strong>Security Features:</strong>
 * <ul>
 *   <li>HMAC-SHA256 cryptographic signing with configurable secret key</li>
 *   <li>Automatic token expiration with configurable timeouts</li>
 *   <li>Token type differentiation (access vs refresh)</li>
 *   <li>Comprehensive claim validation and extraction</li>
 * </ul>
 * 
 * <p><strong>Token Claims:</strong>
 * Access tokens contain:
 * <ul>
 *   <li>User ID and tenant ID for multi-tenant isolation</li>
 *   <li>User role for authorization decisions</li>
 *   <li>User profile information (email, name) for convenience</li>
 *   <li>Standard JWT claims (subject, issued at, expiration)</li>
 * </ul>
 * 
 * <p><strong>Configuration:</strong>
 * <ul>
 *   <li><code>jwt.secret</code>: Secret key for token signing (required)</li>
 *   <li><code>jwt.access-token-expiration</code>: Access token lifetime in seconds (default: 3600)</li>
 *   <li><code>jwt.refresh-token-expiration</code>: Refresh token lifetime in seconds (default: 604800)</li>
 * </ul>
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.0
 * @see User
 * @see Claims
 */
@Service
public class JwtService {

    /** Cryptographic secret key for JWT token signing and verification. */
    private final SecretKey secretKey;
    
    /** Access token expiration time in seconds (default: 1 hour). */
    private final long accessTokenExpiration;
    
    /** Refresh token expiration time in seconds (default: 7 days). */
    private final long refreshTokenExpiration;

    /**
     * Constructs a new JwtService with configurable token expiration settings.
     * 
     * <p>The secret key is used for HMAC-SHA256 signing and must be kept secure.
     * Token expiration times can be configured via application properties.
     * 
     * @param secret the secret key for JWT signing (from jwt.secret property)
     * @param accessTokenExpiration access token lifetime in seconds (default: 3600)
     * @param refreshTokenExpiration refresh token lifetime in seconds (default: 604800)
     */
    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration:3600}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration:604800}") long refreshTokenExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    /**
     * Generates an access token with full user context and permissions.
     * 
     * <p>Access tokens are short-lived and contain comprehensive user information
     * for API authorization and user context. These tokens include:
     * <ul>
     *   <li>User and tenant IDs for multi-tenant isolation</li>
     *   <li>User role for permission checks</li>
     *   <li>Profile information for convenience</li>
     *   <li>Standard JWT claims (subject, expiration, etc.)</li>
     * </ul>
     * 
     * <p><strong>Security Considerations:</strong>
     * <ul>
     *   <li>Short expiration time reduces exposure window if compromised</li>
     *   <li>Contains sensitive user information - should be transmitted securely</li>
     *   <li>Used for all API calls requiring authentication</li>
     * </ul>
     * 
     * @param user the user entity to create the access token for
     * @return signed JWT access token containing user claims
     */
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("tenantId", user.getTenant().getId().toString());
        claims.put("role", user.getRole().toString());
        claims.put("email", user.getEmail());
        claims.put("firstName", user.getFirstName());
        claims.put("lastName", user.getLastName());
        
        return createToken(claims, user.getEmail(), accessTokenExpiration);
    }

    /**
     * Generates a refresh token for obtaining new access tokens.
     * 
     * <p>Refresh tokens are long-lived and contain minimal information,
     * primarily used to generate new access tokens without requiring
     * user re-authentication. They include:
     * <ul>
     *   <li>User ID for identification</li>
     *   <li>Token type marker for validation</li>
     *   <li>Standard JWT claims (subject, expiration)</li>
     * </ul>
     * 
     * <p><strong>Security Features:</strong>
     * <ul>
     *   <li>Longer expiration time for user convenience</li>
     *   <li>Minimal claims reduce information exposure</li>
     *   <li>Token type validation prevents misuse as access token</li>
     *   <li>Single-use pattern recommended (rotate on refresh)</li>
     * </ul>
     * 
     * @param user the user entity to create the refresh token for
     * @return signed JWT refresh token for token renewal
     */
    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("tokenType", "refresh");
        
        return createToken(claims, user.getEmail(), refreshTokenExpiration);
    }

    /**
     * Creates a JWT token with specified claims, subject, and expiration.
     * 
     * <p>This is the core token creation method that handles the JWT building
     * process with cryptographic signing. It creates tokens with:
     * <ul>
     *   <li>Custom claims provided by the caller</li>
     *   <li>Standard JWT subject claim</li>
     *   <li>Issued at timestamp for token age tracking</li>
     *   <li>Expiration timestamp based on provided lifetime</li>
     *   <li>HMAC-SHA256 signature for integrity verification</li>
     * </ul>
     * 
     * @param claims custom claims to include in the token
     * @param subject the subject claim (typically user email)
     * @param expiration token lifetime in seconds
     * @return compact JWT string ready for transmission
     */
    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        Instant now = Instant.now();
        Instant expiryDate = now.plus(expiration, ChronoUnit.SECONDS);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiryDate))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Extracts and validates claims from a JWT token.
     * 
     * <p>This method performs comprehensive token validation including:
     * <ul>
     *   <li>Cryptographic signature verification</li>
     *   <li>Token structure validation</li>
     *   <li>Expiration check (throws exception if expired)</li>
     *   <li>Claims extraction and parsing</li>
     * </ul>
     * 
     * <p><strong>Security:</strong> Invalid, tampered, or expired tokens
     * will cause this method to throw exceptions, ensuring only valid
     * tokens are processed by the application.
     * 
     * @param token the JWT token string to parse and validate
     * @return Claims object containing all token claims
     * @throws io.jsonwebtoken.JwtException if token is invalid, expired, or tampered
     */
    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extracts the username (subject) from a JWT token.
     * 
     * <p>The username is stored in the standard JWT subject claim and
     * is typically the user's email address in this system.
     * 
     * @param token the JWT token to extract username from
     * @return the username/email from the token's subject claim
     * @throws io.jsonwebtoken.JwtException if token is invalid
     */
    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    /**
     * Extracts the user ID from a JWT token.
     * 
     * <p>The user ID is stored as a custom claim and is used for
     * user identification and database lookups throughout the system.
     * 
     * @param token the JWT token to extract user ID from
     * @return the user's UUID from the token claims
     * @throws io.jsonwebtoken.JwtException if token is invalid
     * @throws IllegalArgumentException if userId claim is malformed
     */
    public UUID extractUserId(String token) {
        String userIdStr = extractClaims(token).get("userId", String.class);
        return UUID.fromString(userIdStr);
    }

    /**
     * Extracts the tenant ID from a JWT token for multi-tenant isolation.
     * 
     * <p>The tenant ID is a critical claim used throughout the system to
     * enforce tenant isolation and ensure users can only access resources
     * within their organization. This claim may be null for certain token types.
     * 
     * @param token the JWT token to extract tenant ID from
     * @return the tenant's UUID from the token claims, or null if not present
     * @throws io.jsonwebtoken.JwtException if token is invalid
     * @throws IllegalArgumentException if tenantId claim is malformed
     */
    public UUID extractTenantId(String token) {
        String tenantIdStr = extractClaims(token).get("tenantId", String.class);
        return tenantIdStr != null ? UUID.fromString(tenantIdStr) : null;
    }

    /**
     * Extracts the user role from a JWT token for authorization decisions.
     * 
     * <p>The role claim is used by Spring Security and custom authorization
     * logic to determine what actions the user is permitted to perform.
     * Common roles include USER and ADMIN.
     * 
     * @param token the JWT token to extract role from
     * @return the user's role string from the token claims
     * @throws io.jsonwebtoken.JwtException if token is invalid
     */
    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    /**
     * Validates a JWT token for authenticity, expiration, and subject match.
     * 
     * <p>This method performs comprehensive token validation including:
     * <ul>
     *   <li>Cryptographic signature verification</li>
     *   <li>Token structure and format validation</li>
     *   <li>Expiration timestamp checking</li>
     *   <li>Subject (username) matching for additional security</li>
     * </ul>
     * 
     * <p><strong>Validation Logic:</strong>
     * Returns true only if all conditions are met:
     * <ul>
     *   <li>Token is properly signed and not tampered with</li>
     *   <li>Token has not expired</li>
     *   <li>Token subject matches the provided username</li>
     * </ul>
     * 
     * <p><strong>Exception Handling:</strong>
     * Any exception during validation (malformed token, signature failure,
     * expiration) results in false being returned, making this method safe
     * for use in authentication filters.
     * 
     * @param token the JWT token to validate
     * @param username the expected username to match against token subject
     * @return true if token is valid and matches username, false otherwise
     */
    public boolean isTokenValid(String token, String username) {
        try {
            Claims claims = extractClaims(token);
            return claims.getSubject().equals(username) && !isTokenExpired(claims);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Determines if a JWT token is a refresh token based on token type claim.
     * 
     * <p>This method checks for the presence of a "tokenType" claim with
     * value "refresh" to distinguish refresh tokens from access tokens.
     * This prevents misuse of refresh tokens in places where access tokens
     * are expected.
     * 
     * <p><strong>Security Validation:</strong>
     * Refresh tokens should only be used for token renewal operations,
     * not for general API authentication. This method enables that validation.
     * 
     * @param token the JWT token to check
     * @return true if token has tokenType=refresh claim, false otherwise
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
     * Checks if a token has expired based on its expiration claim.
     * 
     * <p>This utility method compares the token's expiration timestamp
     * with the current time to determine if the token is still valid.
     * 
     * @param claims the extracted claims containing expiration information
     * @return true if token has expired, false if still valid
     */
    private boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    /**
     * Extracts the expiration date from a JWT token.
     * 
     * <p>This method is useful for client applications that need to
     * know when a token will expire to implement proactive token refresh.
     * 
     * @param token the JWT token to extract expiration from
     * @return the Date when the token expires
     * @throws io.jsonwebtoken.JwtException if token is invalid
     */
    public Date getExpirationDate(String token) {
        return extractClaims(token).getExpiration();
    }

    /**
     * Returns the configured access token expiration time in seconds.
     * 
     * <p>This value is used by client applications to understand how
     * long access tokens remain valid, enabling appropriate token
     * management strategies.
     * 
     * @return access token lifetime in seconds
     */
    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }
}
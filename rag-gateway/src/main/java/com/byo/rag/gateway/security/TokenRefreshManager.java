package com.byo.rag.gateway.security;

import com.byo.rag.gateway.service.JwtValidationService;
import com.byo.rag.gateway.service.SessionManagementService;
import com.byo.rag.gateway.service.SecurityAuditService;
import com.byo.rag.gateway.dto.TokenRefreshRequest;
import com.byo.rag.gateway.dto.TokenRefreshResponse;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Token Refresh Manager for RAG Gateway.
 * 
 * <p>This manager handles secure JWT token refresh operations with comprehensive
 * validation, rotation, and audit logging. It implements secure token rotation
 * patterns to prevent token replay attacks and maintain session security.
 * 
 * <p><strong>Token Refresh Features:</strong>
 * <ul>
 *   <li><strong>Secure Rotation</strong>: Automatic token rotation with refresh token validation</li>
 *   <li><strong>Session Continuity</strong>: Maintains active sessions during token refresh</li>
 *   <li><strong>Replay Protection</strong>: Prevents refresh token reuse attacks</li>
 *   <li><strong>Audit Logging</strong>: Comprehensive logging of all refresh operations</li>
 * </ul>
 * 
 * <p><strong>Security Considerations:</strong>
 * <ul>
 *   <li>Refresh tokens are single-use and immediately invalidated</li>
 *   <li>New access and refresh token pairs generated on each refresh</li>
 *   <li>Suspicious refresh patterns trigger security alerts</li>
 *   <li>All refresh operations are logged for audit compliance</li>
 * </ul>
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.1
 */
@Component
public class TokenRefreshManager {

    /** JWT validation service for token operations. */
    private final JwtValidationService jwtValidationService;

    /** Session management service for session tracking. */
    private final SessionManagementService sessionManagementService;

    /** Security audit service for logging. */
    private final SecurityAuditService securityAuditService;

    /** Refresh token validity duration in hours. */
    @Value("${jwt.refresh-token-expiration:168}") // 7 days default
    private int refreshTokenExpirationHours;

    /** Access token validity duration in hours. */
    @Value("${jwt.access-token-expiration:1}") // 1 hour default
    private int accessTokenExpirationHours;

    /** Maximum allowed refresh attempts per hour. */
    @Value("${security.max-refresh-attempts:10}")
    private int maxRefreshAttemptsPerHour;

    /**
     * Constructs token refresh manager.
     * 
     * @param jwtValidationService JWT validation service
     * @param sessionManagementService session management service
     * @param securityAuditService security audit service
     */
    @Autowired
    public TokenRefreshManager(
            JwtValidationService jwtValidationService,
            SessionManagementService sessionManagementService,
            SecurityAuditService securityAuditService) {
        this.jwtValidationService = jwtValidationService;
        this.sessionManagementService = sessionManagementService;
        this.securityAuditService = securityAuditService;
    }

    /**
     * Handles token refresh requests with comprehensive security validation.
     * 
     * <p>This method implements a secure token refresh flow that validates
     * refresh tokens, checks for suspicious activity, and generates new
     * token pairs while maintaining session continuity.
     * 
     * <p><strong>Refresh Flow:</strong>
     * <ol>
     *   <li><strong>Request Validation</strong>: Validate refresh token structure and signature</li>
     *   <li><strong>Replay Check</strong>: Ensure refresh token hasn't been used before</li>
     *   <li><strong>Session Validation</strong>: Verify associated session is active</li>
     *   <li><strong>Rate Limiting</strong>: Check refresh rate limits for user/IP</li>
     *   <li><strong>Token Generation</strong>: Generate new access and refresh token pair</li>
     *   <li><strong>Token Rotation</strong>: Invalidate old tokens and activate new ones</li>
     *   <li><strong>Audit Logging</strong>: Log refresh operation for compliance</li>
     * </ol>
     * 
     * @param request server request containing refresh token
     * @return reactive response with new token pair or error
     */
    public Mono<ServerResponse> handleTokenRefresh(ServerRequest request) {
        String clientIp = getClientIpAddress(request);
        String requestId = getRequestId(request);

        return request.bodyToMono(TokenRefreshRequest.class)
            .flatMap(refreshRequest -> {
                String refreshToken = refreshRequest.getRefreshToken();
                
                if (refreshToken == null || refreshToken.trim().isEmpty()) {
                    return createErrorResponse("MISSING_REFRESH_TOKEN", 
                        "Refresh token is required", requestId);
                }

                return processTokenRefresh(refreshToken, clientIp, requestId);
            })
            .onErrorResume(error -> {
                securityAuditService.logSecurityEvent(requestId, "TOKEN_REFRESH_ERROR", 
                    clientIp, "Refresh request processing failed: " + error.getMessage());
                return createErrorResponse("REFRESH_PROCESSING_ERROR", 
                    "Token refresh failed", requestId);
            });
    }

    /**
     * Processes token refresh with security validation.
     * 
     * @param refreshToken refresh token to validate
     * @param clientIp client IP address
     * @param requestId request identifier
     * @return reactive response with new tokens or error
     */
    private Mono<ServerResponse> processTokenRefresh(String refreshToken, String clientIp, String requestId) {
        if (!jwtValidationService.validateRefreshToken(refreshToken)) {
            return createErrorResponse("INVALID_REFRESH_TOKEN", "Refresh token is invalid", requestId);
        }

        try {
            Claims claims = jwtValidationService.extractClaims(refreshToken);
            String userId = claims.getSubject();
            String sessionId = claims.get("sessionId", String.class);
            String tokenId = claims.get("tokenId", String.class);

            // Check if refresh token has been used before
            boolean isUsed = sessionManagementService.isRefreshTokenUsed(tokenId);
            if (isUsed) {
                // Potential replay attack - invalidate all user sessions
                return handleSuspiciousRefreshActivity(userId, sessionId, clientIp, requestId);
            }

            // Validate session is active
            boolean sessionValid = sessionManagementService.validateSession(sessionId, userId);
            if (!sessionValid) {
                return createErrorResponse("INVALID_SESSION", 
                    "Session is invalid or expired", requestId);
            }

                                // Check refresh rate limits
            if (!checkRefreshRateLimit(userId, clientIp)) {
                return createErrorResponse("RATE_LIMIT_EXCEEDED", 
                    "Too many refresh attempts", requestId);
            }

            // Generate new token pair
            return generateNewTokenPair(claims, sessionId, clientIp, requestId);
            
        } catch (Exception error) {
            String errorType = determineRefreshErrorType(error);
            securityAuditService.logSecurityEvent("SYSTEM", errorType, clientIp, 
                "Refresh token validation failed: " + error.getMessage());
            return createErrorResponse(errorType, "Invalid refresh token", requestId);
        }
    }

    /**
     * Generates new access and refresh token pair.
     * 
     * @param originalClaims original token claims
     * @param sessionId session identifier
     * @param clientIp client IP address
     * @param requestId request identifier
     * @return reactive response with new tokens
     */
    private Mono<ServerResponse> generateNewTokenPair(
            Claims originalClaims, 
            String sessionId, 
            String clientIp, 
            String requestId) {

        String userId = originalClaims.getSubject();
        String tenantId = originalClaims.get("tenantId", String.class);
        String role = originalClaims.get("role", String.class);

        // Mark old refresh token as used
        String oldTokenId = originalClaims.get("tokenId", String.class);
        sessionManagementService.markRefreshTokenUsed(oldTokenId);
        
        // Generate new token pair
        JwtValidationService.TokenPair tokenPair = jwtValidationService.generateTokenPair(userId, 
            originalClaims.getSubject(), tenantId, role);
        
        // Log successful refresh
        securityAuditService.logAuthenticationEvent(userId, tenantId, "TOKEN_REFRESH",
            clientIp, "system", "Token successfully refreshed");
        
        // Create successful response with new tokens
        return createTokenResponse(tokenPair, requestId);
    }

    /**
     * Handles suspicious refresh activity (potential replay attack).
     * 
     * @param userId user identifier
     * @param sessionId session identifier
     * @param clientIp client IP address
     * @param requestId request identifier
     * @return reactive error response
     */
    private Mono<ServerResponse> handleSuspiciousRefreshActivity(
            String userId, 
            String sessionId, 
            String clientIp, 
            String requestId) {

        // Log security incident
        securityAuditService.logSecurityIncident(requestId, "REFRESH_TOKEN_REPLAY", 
            clientIp, userId, "Refresh token reuse detected - potential replay attack");

        // Invalidate all user sessions as security measure
        return sessionManagementService.invalidateAllUserSessions(userId, "Suspicious refresh token usage")
            .then(createErrorResponse("SECURITY_VIOLATION", 
                "Security violation detected. All sessions invalidated.", requestId));
    }

    /**
     * Checks refresh rate limits for user and IP.
     * 
     * @param userId user identifier
     * @param clientIp client IP address
     * @return true if rate limit is not exceeded
     */
    private boolean checkRefreshRateLimit(String userId, String clientIp) {
        Duration window = Duration.ofHours(1);
        
        int userRefreshCount = sessionManagementService.getRefreshCount(userId, window);
        if (userRefreshCount >= maxRefreshAttemptsPerHour) {
            return false;
        }
        
        int ipRefreshCount = sessionManagementService.getRefreshCount(clientIp, window);
        return ipRefreshCount < maxRefreshAttemptsPerHour * 2;
    }

    /**
     * Creates error response with security headers.
     * 
     * @param errorCode error code
     * @param errorMessage error message
     * @param requestId request identifier
     * @return reactive error response
     */
    /**
     * Creates a successful token response with new tokens.
     */
    private Mono<ServerResponse> createTokenResponse(JwtValidationService.TokenPair tokenPair, String requestId) {
        TokenRefreshResponse response = new TokenRefreshResponse(
            tokenPair.getAccessToken(),
            tokenPair.getRefreshToken(),
            900, // 15 minutes in seconds
            "Bearer"
        );

        return ServerResponse.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Request-ID", requestId)
            .header("Cache-Control", "no-store")
            .header("Pragma", "no-cache")
            .bodyValue(response);
    }

    private Mono<ServerResponse> createErrorResponse(String errorCode, String errorMessage, String requestId) {
        Map<String, Object> errorResponse = Map.of(
            "error", errorCode,
            "message", errorMessage,
            "timestamp", LocalDateTime.now().toString(),
            "requestId", requestId
        );

        return ServerResponse.status(HttpStatus.UNAUTHORIZED)
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Request-ID", requestId)
            .header("Cache-Control", "no-store")
            .header("Pragma", "no-cache")
            .bodyValue(errorResponse);
    }

    /**
     * Gets client IP address from request.
     * 
     * @param request server request
     * @return client IP address
     */
    private String getClientIpAddress(ServerRequest request) {
        String xForwardedFor = request.headers().firstHeader("X-Forwarded-For");
        if (xForwardedFor != null) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.remoteAddress()
            .map(address -> address.getAddress().getHostAddress())
            .orElse("unknown");
    }

    /**
     * Gets request ID from headers.
     * 
     * @param request server request
     * @return request ID
     */
    private String getRequestId(ServerRequest request) {
        String requestId = request.headers().firstHeader("X-Request-ID");
        return requestId != null ? requestId : java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Determines refresh error type from exception.
     * 
     * @param error exception
     * @return error type string
     */
    private String determineRefreshErrorType(Throwable error) {
        if (error instanceof io.jsonwebtoken.ExpiredJwtException) {
            return "REFRESH_TOKEN_EXPIRED";
        } else if (error instanceof io.jsonwebtoken.SignatureException) {
            return "INVALID_REFRESH_SIGNATURE";
        } else if (error instanceof io.jsonwebtoken.MalformedJwtException) {
            return "MALFORMED_REFRESH_TOKEN";
        } else {
            return "REFRESH_VALIDATION_ERROR";
        }
    }

    /**
     * Token refresh response model.
     */
    public static class TokenRefreshResponse {
        private final String accessToken;
        private final String refreshToken;
        private final int expiresIn;
        private final String tokenType;

        public TokenRefreshResponse(String accessToken, String refreshToken, int expiresIn, String tokenType) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresIn = expiresIn;
            this.tokenType = tokenType;
        }

        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public int getExpiresIn() { return expiresIn; }
        public String getTokenType() { return tokenType; }
    }
}
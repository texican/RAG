package com.byo.rag.gateway.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response DTO for token refresh operations.
 * 
 * <p>This class represents the response payload for JWT token refresh requests,
 * containing new access and refresh tokens along with their metadata.
 * 
 * <p><strong>Security Features:</strong>
 * <ul>
 *   <li>Token rotation - both access and refresh tokens are replaced</li>
 *   <li>Expiration information for proper client-side token management</li>
 *   <li>Session information for client state synchronization</li>
 *   <li>Error information for failed refresh attempts</li>
 * </ul>
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenRefreshResponse {

    /** The new access token (short-lived). */
    private String accessToken;

    /** The new refresh token (long-lived). */
    private String refreshToken;

    /** Token type (typically "Bearer"). */
    private String tokenType = "Bearer";

    /** Access token expiration time in seconds. */
    private Long expiresIn;

    /** Refresh token expiration time in seconds. */
    private Long refreshExpiresIn;

    /** Session identifier for tracking. */
    private String sessionId;

    /** Success indicator. */
    private boolean success;

    /** Error message if refresh failed. */
    private String error;

    /** Detailed error description. */
    private String errorDescription;

    /** Timestamp of token issuance. */
    private Long issuedAt;

    /**
     * Default constructor for JSON serialization.
     */
    public TokenRefreshResponse() {
    }

    /**
     * Constructor for successful token refresh.
     * 
     * @param accessToken the new access token
     * @param refreshToken the new refresh token
     * @param expiresIn access token expiration in seconds
     * @param refreshExpiresIn refresh token expiration in seconds
     * @param sessionId the session identifier
     */
    public TokenRefreshResponse(String accessToken, String refreshToken, Long expiresIn, 
                              Long refreshExpiresIn, String sessionId) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.refreshExpiresIn = refreshExpiresIn;
        this.sessionId = sessionId;
        this.success = true;
        this.issuedAt = System.currentTimeMillis() / 1000;
    }

    /**
     * Constructor for failed token refresh.
     * 
     * @param error the error code
     * @param errorDescription the error description
     */
    public TokenRefreshResponse(String error, String errorDescription) {
        this.error = error;
        this.errorDescription = errorDescription;
        this.success = false;
        this.issuedAt = System.currentTimeMillis() / 1000;
    }

    /**
     * Creates a successful response.
     * 
     * @param accessToken the new access token
     * @param refreshToken the new refresh token
     * @param expiresIn access token expiration in seconds
     * @param refreshExpiresIn refresh token expiration in seconds
     * @param sessionId the session identifier
     * @return successful token refresh response
     */
    public static TokenRefreshResponse success(String accessToken, String refreshToken, 
                                             Long expiresIn, Long refreshExpiresIn, String sessionId) {
        return new TokenRefreshResponse(accessToken, refreshToken, expiresIn, refreshExpiresIn, sessionId);
    }

    /**
     * Creates an error response.
     * 
     * @param error the error code
     * @param errorDescription the error description
     * @return error token refresh response
     */
    public static TokenRefreshResponse error(String error, String errorDescription) {
        return new TokenRefreshResponse(error, errorDescription);
    }

    // Getters and setters

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public Long getRefreshExpiresIn() {
        return refreshExpiresIn;
    }

    public void setRefreshExpiresIn(Long refreshExpiresIn) {
        this.refreshExpiresIn = refreshExpiresIn;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public Long getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Long issuedAt) {
        this.issuedAt = issuedAt;
    }

    @Override
    public String toString() {
        return "TokenRefreshResponse{" +
                "tokenType='" + tokenType + '\'' +
                ", expiresIn=" + expiresIn +
                ", refreshExpiresIn=" + refreshExpiresIn +
                ", sessionId='" + sessionId + '\'' +
                ", success=" + success +
                ", error='" + error + '\'' +
                ", errorDescription='" + errorDescription + '\'' +
                ", issuedAt=" + issuedAt +
                '}';
    }
}
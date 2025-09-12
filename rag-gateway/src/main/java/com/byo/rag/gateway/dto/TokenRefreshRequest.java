package com.byo.rag.gateway.dto;


/**
 * Request DTO for token refresh operations.
 * 
 * <p>This class represents the request payload for refreshing JWT tokens,
 * containing the refresh token and any additional security validation data.
 * 
 * <p><strong>Security Considerations:</strong>
 * <ul>
 *   <li>Refresh token should be validated for format and authenticity</li>
 *   <li>Client IP and User-Agent are used for session binding validation</li>
 *   <li>Request should be rate-limited to prevent abuse</li>
 * </ul>
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.0
 */
public class TokenRefreshRequest {

    /** The refresh token to be used for generating new tokens. */
    private String refreshToken;

    /** Optional client identifier for additional security validation. */
    private String clientId;

    /** Optional device fingerprint for enhanced security. */
    private String deviceFingerprint;

    /**
     * Default constructor for JSON deserialization.
     */
    public TokenRefreshRequest() {
    }

    /**
     * Constructor with refresh token.
     * 
     * @param refreshToken the refresh token
     */
    public TokenRefreshRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    /**
     * Constructor with all fields.
     * 
     * @param refreshToken the refresh token
     * @param clientId the client identifier
     * @param deviceFingerprint the device fingerprint
     */
    public TokenRefreshRequest(String refreshToken, String clientId, String deviceFingerprint) {
        this.refreshToken = refreshToken;
        this.clientId = clientId;
        this.deviceFingerprint = deviceFingerprint;
    }

    /**
     * Gets the refresh token.
     * 
     * @return the refresh token
     */
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * Sets the refresh token.
     * 
     * @param refreshToken the refresh token to set
     */
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    /**
     * Gets the client identifier.
     * 
     * @return the client identifier
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Sets the client identifier.
     * 
     * @param clientId the client identifier to set
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * Gets the device fingerprint.
     * 
     * @return the device fingerprint
     */
    public String getDeviceFingerprint() {
        return deviceFingerprint;
    }

    /**
     * Sets the device fingerprint.
     * 
     * @param deviceFingerprint the device fingerprint to set
     */
    public void setDeviceFingerprint(String deviceFingerprint) {
        this.deviceFingerprint = deviceFingerprint;
    }

    @Override
    public String toString() {
        return "TokenRefreshRequest{" +
                "refreshToken='[PROTECTED]'" +
                ", clientId='" + clientId + '\'' +
                ", deviceFingerprint='" + deviceFingerprint + '\'' +
                '}';
    }
}
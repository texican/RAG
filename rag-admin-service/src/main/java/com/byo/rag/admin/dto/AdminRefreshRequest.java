package com.byo.rag.admin.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Data transfer object for administrative token refresh requests.
 * 
 * <p>This record encapsulates the refresh token required to obtain a new
 * JWT access token for continued administrative access. It enables secure
 * token renewal without requiring credential re-authentication.</p>
 * 
 * <p>Usage pattern:</p>
 * <ul>
 *   <li>Client detects token approaching expiration</li>
 *   <li>Sends refresh token to renew access</li>
 *   <li>Receives new JWT token with extended validity</li>
 *   <li>Continues administrative operations seamlessly</li>
 * </ul>
 * 
 * <p>Security considerations: Refresh tokens should be stored securely
 * and transmitted only over HTTPS. Invalid or expired refresh tokens
 * will require full re-authentication through login.</p>
 * 
 * @param token the refresh token for generating new access tokens
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.0
 * @see AdminLoginResponse
 * @see com.byo.rag.admin.service.AdminJwtService
 */
public record AdminRefreshRequest(
        @NotBlank(message = "Token is required")
        String token
) {
}
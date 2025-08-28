package com.enterprise.rag.admin.dto;

import java.util.List;

/**
 * Response data transfer object for successful administrative authentication.
 * 
 * <p>This record contains all necessary information returned to administrators
 * after successful login, including JWT tokens and user context required
 * for subsequent authorized operations in the admin service.</p>
 * 
 * <p>Response structure includes:</p>
 * <ul>
 *   <li>JWT access token for API authentication</li>
 *   <li>Administrator identity information</li>
 *   <li>Role-based authorization data</li>
 *   <li>Token expiration metadata for client-side management</li>
 * </ul>
 * 
 * <p>Security note: The token should be stored securely on the client side
 * and transmitted only over HTTPS connections. Roles determine admin privileges
 * and should be validated on each protected request.</p>
 * 
 * @param token JWT access token for API authorization
 * @param username administrator's email/username identifier
 * @param roles list of administrative roles for authorization
 * @param expiresIn token validity duration in seconds
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.0
 * @see AdminLoginRequest
 * @see com.enterprise.rag.admin.service.AdminJwtService
 */
public record AdminLoginResponse(
        String token,
        String username,
        List<String> roles,
        Long expiresIn
) {
}
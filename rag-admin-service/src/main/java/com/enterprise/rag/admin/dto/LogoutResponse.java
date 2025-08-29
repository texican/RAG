package com.enterprise.rag.admin.dto;

/**
 * Response data transfer object for administrative logout operations.
 * 
 * <p>This simple record provides confirmation messaging for successful logout
 * operations, enabling client applications to display appropriate user feedback
 * and perform necessary cleanup operations.</p>
 * 
 * <p>Logout process includes:</p>
 * <ul>
 *   <li>JWT token invalidation on the server side</li>
 *   <li>Session termination and cleanup</li>
 *   <li>Security context clearing</li>
 *   <li>Audit logging of logout events</li>
 * </ul>
 * 
 * <p>Client responsibilities after logout:</p>
 * <ul>
 *   <li>Remove stored JWT tokens from secure storage</li>
 *   <li>Clear user session data and preferences</li>
 *   <li>Redirect to login page or public areas</li>
 *   <li>Display logout confirmation to user</li>
 * </ul>
 * 
 * @param message confirmation message for successful logout
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.0
 * @see AdminLoginResponse
 * @see com.enterprise.rag.admin.controller.AdminAuthController
 */
public record LogoutResponse(
        String message
) {
}
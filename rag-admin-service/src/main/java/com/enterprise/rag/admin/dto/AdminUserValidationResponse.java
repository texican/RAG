package com.enterprise.rag.admin.dto;

/**
 * Response data transfer object for administrative user validation operations.
 * 
 * <p>This record provides confirmation of user existence and identity validation
 * for administrative security operations. It supports user verification workflows
 * without exposing sensitive user details or system information.</p>
 * 
 * <p>Validation use cases:</p>
 * <ul>
 *   <li>Administrative user verification before sensitive operations</li>
 *   <li>Identity confirmation for security protocols</li>
 *   <li>User existence checks for audit and compliance</li>
 *   <li>Authorization prerequisite validation</li>
 * </ul>
 * 
 * <p>Security design principles:</p>
 * <ul>
 *   <li>Minimal information disclosure for privacy protection</li>
 *   <li>Boolean confirmation without detailed user data</li>
 *   <li>Username echo for operation confirmation</li>
 *   <li>No sensitive information in response payload</li>
 * </ul>
 * 
 * @param exists whether the specified administrative user exists in the system
 * @param username the username/email that was validated (for confirmation)
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.0
 * @see com.enterprise.rag.admin.service.UserService
 * @see com.enterprise.rag.admin.controller.AdminAuthController
 */
public record AdminUserValidationResponse(
        boolean exists,
        String username
) {
}
package com.enterprise.rag.shared.exception;

import java.util.UUID;

/**
 * Exception thrown when a requested user cannot be found in the system.
 * <p>
 * This exception is raised during user lookup operations when the specified
 * user ID or email address does not exist in the system. It supports both
 * UUID-based and email-based user identification for comprehensive user management.
 * 
 * <h2>User Lookup Operations</h2>
 * <ul>
 *   <li>User profile retrieval by ID or email</li>
 *   <li>Authentication credential validation</li>
 *   <li>User-tenant relationship verification</li>
 *   <li>Authorization checks for protected resources</li>
 * </ul>
 * 
 * <h2>Common Scenarios</h2>
 * <ul>
 *   <li>Invalid user UUID provided in API requests</li>
 *   <li>Email address not registered in the system</li>
 *   <li>User account deactivated or soft-deleted</li>
 *   <li>Cross-tenant user access attempts</li>
 *   <li>JWT token with non-existent user claims</li>
 * </ul>
 * 
 * <h2>Security Considerations</h2>
 * <ul>
 *   <li>Prevents information disclosure about user existence</li>
 *   <li>Maintains consistent error responses for security</li>
 *   <li>Supports audit logging for failed user lookups</li>
 *   <li>Tenant isolation maintained in error messages</li>
 * </ul>
 * 
 * @author Enterprise RAG Development Team
 * @version 0.8.0
 * @since 0.1.0
 * @see RagException
 */
public class UserNotFoundException extends RagException {
    
    public UserNotFoundException(UUID userId) {
        super("User not found with ID: " + userId, "USER_NOT_FOUND");
    }
    
    public UserNotFoundException(String email) {
        super("User not found with email: " + email, "USER_NOT_FOUND");
    }
}
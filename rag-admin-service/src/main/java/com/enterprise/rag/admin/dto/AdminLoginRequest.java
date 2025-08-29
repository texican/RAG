package com.enterprise.rag.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data transfer object for administrative login requests in the RAG system.
 * 
 * <p>This record encapsulates administrator credentials required for authentication
 * to the admin service. It enforces strong validation rules to ensure secure
 * login operations with proper email format and minimum security standards.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Email-based authentication for professional identity verification</li>
 *   <li>Strong password requirements with minimum length enforcement</li>
 *   <li>Built-in Jakarta validation for data integrity</li>
 *   <li>Immutable record structure for thread-safe operations</li>
 * </ul>
 * 
 * <p>Security considerations: This DTO should only be used over HTTPS connections
 * and passwords should be handled according to enterprise security policies.</p>
 * 
 * @param username administrator's email address (validated format)
 * @param password administrator's password (minimum 8 characters)
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.0
 * @see AdminLoginResponse
 * @see com.enterprise.rag.admin.controller.AdminAuthController
 */
public record AdminLoginRequest(
        @NotBlank(message = "Username is required")
        @Email(message = "Username must be a valid email address")
        String username,
        
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters long")
        String password
) {
}
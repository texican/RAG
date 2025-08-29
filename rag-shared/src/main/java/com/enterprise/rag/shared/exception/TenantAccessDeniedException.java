package com.enterprise.rag.shared.exception;

import java.util.UUID;

/**
 * Exception thrown when a user attempts to access a tenant they do not have permissions for.
 * <p>
 * This exception enforces multi-tenant security by preventing unauthorized access
 * to tenant-specific resources. It is thrown during authentication and authorization
 * checks when users attempt to access data outside their assigned tenant scope.
 * 
 * <h2>Security Context</h2>
 * <ul>
 *   <li>Enforces strict tenant isolation in multi-tenant architecture</li>
 *   <li>Prevents data leakage between different tenant organizations</li>
 *   <li>Validates user-tenant relationships during API requests</li>
 *   <li>Provides audit trail for unauthorized access attempts</li>
 * </ul>
 * 
 * <h2>Common Scenarios</h2>
 * <ul>
 *   <li>User JWT contains different tenant ID than requested resource</li>
 *   <li>User account has been removed from tenant but token still valid</li>
 *   <li>Cross-tenant resource access attempts</li>
 *   <li>Invalid or malformed tenant identifiers in requests</li>
 * </ul>
 * 
 * <h2>Error Response</h2>
 * Returns standardized error code "TENANT_ACCESS_DENIED" with:
 * <ul>
 *   <li>User ID and tenant ID for audit logging</li>
 *   <li>Timestamp for security monitoring</li>
 *   <li>Request context for investigation</li>
 * </ul>
 * 
 * @author Enterprise RAG Development Team
 * @version 0.8.0
 * @since 0.1.0
 * @see RagException
 */
public class TenantAccessDeniedException extends RagException {
    
    public TenantAccessDeniedException(UUID userId, UUID tenantId) {
        super("User " + userId + " does not have access to tenant " + tenantId, "TENANT_ACCESS_DENIED");
    }
    
    public TenantAccessDeniedException(String message) {
        super(message, "TENANT_ACCESS_DENIED");
    }
}
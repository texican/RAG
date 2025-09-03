package com.byo.rag.shared.exception;

import java.util.UUID;

/**
 * Exception thrown when a requested tenant cannot be found in the system.
 * <p>
 * This exception is raised during tenant lookup operations when the specified
 * tenant ID or slug does not exist in the system. It supports both UUID-based
 * and slug-based tenant identification for flexible tenant management.
 * 
 * <h2>Tenant Lookup Operations</h2>
 * <ul>
 *   <li>Tenant configuration retrieval by ID or slug</li>
 *   <li>Multi-tenant request routing and validation</li>
 *   <li>Tenant-scoped resource access verification</li>
 *   <li>Billing and subscription management operations</li>
 * </ul>
 * 
 * <h2>Common Scenarios</h2>
 * <ul>
 *   <li>Invalid tenant UUID provided in API requests</li>
 *   <li>Tenant slug not registered or deactivated</li>
 *   <li>Tenant account suspended or deleted</li>
 *   <li>JWT token with non-existent tenant claims</li>
 *   <li>Cross-service tenant reference validation failures</li>
 * </ul>
 * 
 * <h2>Multi-Tenant Architecture Impact</h2>
 * <ul>
 *   <li>Prevents access to non-existent tenant data</li>
 *   <li>Maintains data isolation integrity</li>
 *   <li>Supports tenant lifecycle management</li>
 *   <li>Enables proper error handling in multi-tenant contexts</li>
 * </ul>
 * 
 * @author Enterprise RAG Development Team
 * @version 0.8.0
 * @since 0.1.0
 * @see RagException
 */
public class TenantNotFoundException extends RagException {
    
    public TenantNotFoundException(UUID tenantId) {
        super("Tenant not found with ID: " + tenantId, "TENANT_NOT_FOUND");
    }
    
    public TenantNotFoundException(String slug) {
        super("Tenant not found with slug: " + slug, "TENANT_NOT_FOUND");
    }
}
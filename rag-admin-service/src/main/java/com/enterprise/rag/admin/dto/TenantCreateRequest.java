package com.enterprise.rag.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data transfer object for creating new tenants in the multi-tenant RAG system.
 * 
 * <p>This record encapsulates all required information for provisioning a new
 * organizational tenant, including administrative contact details and descriptive
 * metadata. Each tenant represents an isolated workspace with dedicated resources
 * and user management.</p>
 * 
 * <p>Tenant creation process:</p>
 * <ul>
 *   <li>Validates organizational information and admin contact</li>
 *   <li>Creates isolated tenant database schemas and configurations</li>
 *   <li>Provisions initial administrative user for the tenant</li>
 *   <li>Establishes security boundaries and resource quotas</li>
 * </ul>
 * 
 * <p>Multi-tenancy features enabled:</p>
 * <ul>
 *   <li>Complete data isolation between organizations</li>
 *   <li>Independent user management per tenant</li>
 *   <li>Dedicated RAG document processing pipelines</li>
 *   <li>Separate embedding storage and retrieval contexts</li>
 * </ul>
 * 
 * @param name organization name (2-100 characters, used for display)
 * @param adminEmail primary administrator email for initial tenant access
 * @param description optional organizational description (max 255 characters)
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.0
 * @see TenantResponse
 * @see com.enterprise.rag.admin.controller.TenantManagementController
 */
public record TenantCreateRequest(
        @NotBlank(message = "Tenant name is required")
        @Size(min = 2, max = 100, message = "Tenant name must be between 2 and 100 characters")
        String name,
        
        @NotBlank(message = "Admin email is required")
        @Email(message = "Admin email must be valid")
        String adminEmail,
        
        @Size(max = 255, message = "Description cannot exceed 255 characters")
        String description
) {
}
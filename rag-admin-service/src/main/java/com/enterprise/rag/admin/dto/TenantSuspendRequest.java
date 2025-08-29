package com.enterprise.rag.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data transfer object for tenant suspension requests in administrative operations.
 * 
 * <p>This record captures the required justification for suspending tenant access,
 * ensuring proper documentation of administrative actions. Tenant suspension temporarily
 * disables all services while preserving data for potential reactivation.</p>
 * 
 * <p>Suspension effects:</p>
 * <ul>
 *   <li>Immediate termination of active user sessions</li>
 *   <li>Blocking of new authentication attempts</li>
 *   <li>Suspension of document processing pipelines</li>
 *   <li>Halting of RAG query processing</li>
 *   <li>Preservation of all tenant data and configurations</li>
 * </ul>
 * 
 * <p>Administrative requirements:</p>
 * <ul>
 *   <li>Detailed reason documentation for audit trails</li>
 *   <li>Sufficient justification length for compliance</li>
 *   <li>Reversible action through separate reactivation process</li>
 * </ul>
 * 
 * @param reason detailed justification for tenant suspension (10-500 characters required)
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.0
 * @see com.enterprise.rag.admin.controller.TenantManagementController
 * @see com.enterprise.rag.admin.service.TenantService
 */
public record TenantSuspendRequest(
        @NotBlank(message = "Reason is required")
        @Size(min = 10, max = 500, message = "Reason must be between 10 and 500 characters")
        String reason
) {
}
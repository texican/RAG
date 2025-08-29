package com.enterprise.rag.admin.dto;

import java.time.LocalDateTime;

/**
 * Response data transfer object for tenant information in administrative operations.
 * 
 * <p>This record provides comprehensive tenant metadata and operational statistics
 * for administrative dashboards and management interfaces. It includes both
 * organizational details and real-time usage metrics for informed decision-making.</p>
 * 
 * <p>Information categories:</p>
 * <ul>
 *   <li><strong>Identity:</strong> Unique tenant ID and organizational name</li>
 *   <li><strong>Administrative:</strong> Primary contact and management details</li>
 *   <li><strong>Descriptive:</strong> Optional metadata and status information</li>
 *   <li><strong>Operational:</strong> Usage statistics and activity indicators</li>
 *   <li><strong>Temporal:</strong> Creation and modification timestamps</li>
 * </ul>
 * 
 * <p>Administrative use cases:</p>
 * <ul>
 *   <li>Tenant overview and management dashboards</li>
 *   <li>Usage monitoring and capacity planning</li>
 *   <li>Billing and subscription management integration</li>
 *   <li>Support and troubleshooting operations</li>
 * </ul>
 * 
 * @param tenantId unique identifier for the tenant organization
 * @param name display name of the organization
 * @param adminEmail primary administrative contact email
 * @param description optional organizational description or notes
 * @param status current tenant status (ACTIVE, SUSPENDED, etc.)
 * @param createdAt timestamp when the tenant was initially created
 * @param updatedAt timestamp of the most recent tenant modification
 * @param totalUsers count of users currently registered to this tenant
 * @param totalDocuments count of documents processed for this tenant
 * @param isActive boolean flag indicating if tenant is currently operational
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.0
 * @see TenantCreateRequest
 * @see TenantUpdateRequest
 * @see com.enterprise.rag.admin.service.TenantService
 */
public record TenantResponse(
        String tenantId,
        String name,
        String adminEmail,
        String description,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        int totalUsers,
        int totalDocuments,
        boolean isActive
) {
}
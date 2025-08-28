package com.enterprise.rag.admin.dto;

import java.util.List;

/**
 * Paginated response data transfer object for tenant listing operations.
 * 
 * <p>This record encapsulates tenant search results with pagination metadata,
 * enabling efficient browsing of large tenant datasets in administrative
 * interfaces. It supports standard pagination patterns for optimal performance
 * and user experience.</p>
 * 
 * <p>Pagination features:</p>
 * <ul>
 *   <li>Configurable page sizes for flexible data presentation</li>
 *   <li>Total count information for pagination controls</li>
 *   <li>Zero-based page indexing for API consistency</li>
 *   <li>Calculated total pages for navigation UI elements</li>
 * </ul>
 * 
 * <p>Common usage patterns:</p>
 * <ul>
 *   <li>Administrative tenant management dashboards</li>
 *   <li>Tenant search and filtering operations</li>
 *   <li>Bulk tenant operations and reporting</li>
 *   <li>System analytics and usage monitoring</li>
 * </ul>
 * 
 * @param tenants list of tenant details for the current page
 * @param totalCount total number of tenants matching the query
 * @param page current page number (zero-based indexing)
 * @param size number of tenants per page
 * @param totalPages calculated total number of pages available
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.0
 * @see TenantResponse
 * @see com.enterprise.rag.admin.controller.TenantManagementController
 */
public record TenantListResponse(
        List<TenantResponse> tenants,
        int totalCount,
        int page,
        int size,
        int totalPages
) {
}
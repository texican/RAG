package com.enterprise.rag.admin.service;

import com.enterprise.rag.admin.dto.*;
import org.springframework.data.domain.PageRequest;

/**
 * Service interface for comprehensive tenant management operations.
 * <p>
 * This service provides enterprise-grade tenant lifecycle management including
 * creation, configuration, monitoring, and administrative operations. It serves
 * as the primary interface for all tenant-related administrative functions
 * in the multi-tenant RAG system.
 * 
 * <h2>Core Tenant Operations</h2>
 * <ul>
 *   <li><strong>Tenant Lifecycle</strong> - Create, update, suspend, reactivate, delete</li>
 *   <li><strong>Configuration Management</strong> - Tenant-specific settings and preferences</li>
 *   <li><strong>Access Control</strong> - Tenant-based permissions and security policies</li>
 *   <li><strong>Resource Management</strong> - Storage quotas, rate limits, feature flags</li>
 * </ul>
 * 
 * <h2>Multi-Tenant Architecture</h2>
 * <ul>
 *   <li><strong>Data Isolation</strong> - Complete separation of tenant data</li>
 *   <li><strong>Resource Allocation</strong> - Per-tenant resource quotas and limits</li>
 *   <li><strong>Security Boundaries</strong> - Tenant-aware access control</li>
 *   <li><strong>Billing Integration</strong> - Usage tracking and billing management</li>
 * </ul>
 * 
 * <h2>Administrative Features</h2>
 * <ul>
 *   <li><strong>Bulk Operations</strong> - Paginated tenant listing and bulk updates</li>
 *   <li><strong>Monitoring</strong> - Tenant health, usage metrics, and analytics</li>
 *   <li><strong>Compliance</strong> - Audit logging and regulatory compliance support</li>
 *   <li><strong>Migration</strong> - Tenant data export, import, and migration tools</li>
 * </ul>
 * 
 * <h2>Implementation Requirements</h2>
 * All implementations must ensure:
 * <ul>
 *   <li>Transactional consistency for tenant operations</li>
 *   <li>Event publishing for tenant lifecycle changes</li>
 *   <li>Comprehensive error handling and validation</li>
 *   <li>Performance optimization for large tenant datasets</li>
 * </ul>
 * 
 * @author Enterprise RAG Development Team
 * @version 0.8.0
 * @since 0.1.0
 * @see TenantServiceImpl
 * @see TenantCreateRequest
 * @see TenantResponse
 */
public interface TenantService {
    TenantResponse createTenant(TenantCreateRequest request);
    TenantResponse getTenantById(String tenantId);
    TenantResponse updateTenant(String tenantId, TenantUpdateRequest request);
    TenantResponse suspendTenant(String tenantId, TenantSuspendRequest request);
    TenantResponse reactivateTenant(String tenantId);
    TenantListResponse getAllTenants(PageRequest pageRequest);
    void deleteTenant(String tenantId);
}
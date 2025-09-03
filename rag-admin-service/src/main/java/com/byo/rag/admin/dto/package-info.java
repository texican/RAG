/**
 * Data Transfer Objects for administrative operations.
 * 
 * <p>This package contains specialized DTOs for administrative operations
 * in the Enterprise RAG System. These DTOs handle complex administrative
 * workflows, tenant management, user administration, system monitoring,
 * and analytics reporting with comprehensive validation and security features.</p>
 * 
 * <h2>DTO Categories</h2>
 * <p>Administrative DTOs are organized by functional area:</p>
 * <ul>
 *   <li><strong>Authentication DTOs</strong> - Administrative login and token management</li>
 *   <li><strong>Tenant Management DTOs</strong> - Tenant creation, update, and status management</li>
 *   <li><strong>User Administration DTOs</strong> - Cross-tenant user management operations</li>
 *   <li><strong>System Monitoring DTOs</strong> - Health monitoring and performance metrics</li>
 *   <li><strong>Analytics DTOs</strong> - Usage analytics and reporting data structures</li>
 *   <li><strong>Configuration DTOs</strong> - System and tenant configuration management</li>
 * </ul>
 * 
 * <h2>Administrative Security</h2>
 * <p>All administrative DTOs implement enhanced security:</p>
 * <ul>
 *   <li><strong>Privilege Validation</strong> - Administrative privilege requirements</li>
 *   <li><strong>Data Sanitization</strong> - Automatic sanitization of administrative data</li>
 *   <li><strong>Audit Information</strong> - Administrative action context and metadata</li>
 *   <li><strong>Access Control</strong> - Role-based field access control</li>
 *   <li><strong>Security Headers</strong> - Security context propagation in DTOs</li>
 * </ul>
 * 
 * <h2>Tenant Management DTOs</h2>
 * <p>Comprehensive tenant management data structures:</p>
 * <ul>
 *   <li><strong>Tenant Creation</strong> - Complete tenant provisioning requests</li>
 *   <li><strong>Tenant Updates</strong> - Tenant configuration and status updates</li>
 *   <li><strong>Tenant Search</strong> - Advanced search criteria and filtering</li>
 *   <li><strong>Tenant Analytics</strong> - Usage metrics and billing information</li>
 *   <li><strong>Tenant Status</strong> - Status management and lifecycle operations</li>
 * </ul>
 * 
 * <h2>User Administration DTOs</h2>
 * <p>Cross-tenant user management data structures:</p>
 * <ul>
 *   <li><strong>User Search</strong> - Advanced user search across all tenants</li>
 *   <li><strong>User Updates</strong> - Administrative user profile updates</li>
 *   <li><strong>Role Management</strong> - User role assignment and permission management</li>
 *   <li><strong>Account Recovery</strong> - Administrative account recovery operations</li>
 *   <li><strong>User Analytics</strong> - User activity and engagement reporting</li>
 * </ul>
 * 
 * <h2>System Monitoring DTOs</h2>
 * <p>Real-time system monitoring data structures:</p>
 * <ul>
 *   <li><strong>Health Status</strong> - System and service health information</li>
 *   <li><strong>Performance Metrics</strong> - Real-time performance and resource data</li>
 *   <li><strong>Error Reports</strong> - System error tracking and analysis</li>
 *   <li><strong>Capacity Data</strong> - Resource utilization and capacity information</li>
 *   <li><strong>SLA Reports</strong> - Service level agreement compliance data</li>
 * </ul>
 * 
 * <h2>Analytics and Reporting DTOs</h2>
 * <p>Comprehensive analytics and reporting data structures:</p>
 * <ul>
 *   <li><strong>Usage Statistics</strong> - Detailed usage patterns and metrics</li>
 *   <li><strong>Performance Reports</strong> - System performance analysis data</li>
 *   <li><strong>Financial Reports</strong> - Billing and cost allocation data</li>
 *   <li><strong>Compliance Reports</strong> - Regulatory compliance and audit data</li>
 *   <li><strong>Custom Analytics</strong> - Configurable analytics and dashboard data</li>
 * </ul>
 * 
 * <h2>Validation and Business Rules</h2>
 * <p>Comprehensive validation for administrative operations:</p>
 * <ul>
 *   <li><strong>Administrative Privileges</strong> - Validation of administrative privileges</li>
 *   <li><strong>Business Rule Validation</strong> - Complex business logic validation</li>
 *   <li><strong>Cross-Field Validation</strong> - Multi-field consistency validation</li>
 *   <li><strong>Tenant Context Validation</strong> - Tenant-aware validation rules</li>
 *   <li><strong>Security Constraints</strong> - Security policy enforcement</li>
 * </ul>
 * 
 * <h2>Serialization and Performance</h2>
 * <p>Optimized for administrative workloads:</p>
 * <ul>
 *   <li><strong>Efficient Serialization</strong> - Optimized JSON serialization for large datasets</li>
 *   <li><strong>Pagination Support</strong> - Built-in pagination for large administrative queries</li>
 *   <li><strong>Lazy Loading</strong> - Strategic lazy loading for complex administrative data</li>
 *   <li><strong>Compression Support</strong> - Support for compressed responses</li>
 * </ul>
 * 
 * <h2>Audit and Compliance</h2>
 * <p>DTOs include comprehensive audit and compliance features:</p>
 * <ul>
 *   <li><strong>Change Tracking</strong> - Automatic change detection and logging</li>
 *   <li><strong>Audit Metadata</strong> - Rich audit information in all DTOs</li>
 *   <li><strong>Compliance Fields</strong> - GDPR and regulatory compliance fields</li>
 *   <li><strong>Data Classification</strong> - Automatic data classification and handling</li>
 * </ul>
 * 
 * <h2>Integration Support</h2>
 * <p>DTOs support integration with external systems:</p>
 * <ul>
 *   <li><strong>Export Formats</strong> - Multiple export formats for external systems</li>
 *   <li><strong>Import Validation</strong> - Comprehensive import validation</li>
 *   <li><strong>API Versioning</strong> - Support for API versioning and evolution</li>
 *   <li><strong>Webhook Payloads</strong> - DTOs optimized for webhook notifications</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * /**
 *  * Request DTO for creating a new tenant.
 *  * Includes comprehensive validation and administrative context.
 *  *\/
 * @JsonInclude(JsonInclude.Include.NON_NULL)
 * public record TenantCreateRequest(
 *     
 *     @NotBlank(message = "Tenant name is required")
 *     @Size(max = 100, message = "Tenant name must not exceed 100 characters")
 *     @Pattern(regexp = "^[a-zA-Z0-9\\s\\-_]+$", message = "Tenant name contains invalid characters")
 *     String name,
 *     
 *     @NotBlank(message = "Tenant domain is required")
 *     @Pattern(regexp = "^[a-z0-9\\-]+$", message = "Domain must contain only lowercase letters, numbers, and hyphens")
 *     @Size(max = 50, message = "Domain must not exceed 50 characters")
 *     String domain,
 *     
 *     @NotNull(message = "Tenant plan is required")
 *     @Valid
 *     TenantPlan plan,
 *     
 *     @NotNull(message = "Admin user information is required")
 *     @Valid
 *     AdminUserCreateRequest adminUser,
 *     
 *     @Size(max = 500, message = "Description must not exceed 500 characters")
 *     String description,
 *     
 *     Map<String, Object> configuration
 * ) {
 *     
 *     /**
 *      * Validates the tenant creation request.
 *      * Performs additional business rule validation beyond annotations.
 *      *\/
 *     @AssertTrue(message = "Domain must be unique")
 *     public boolean isDomainValid() {
 *         return domain != null && !domain.isEmpty() && 
 *                !RESERVED_DOMAINS.contains(domain.toLowerCase());
 *     }
 * }
 * 
 * /**
 *  * Response DTO for tenant information with administrative context.
 *  *\/
 * @JsonInclude(JsonInclude.Include.NON_NULL)
 * public record TenantResponse(
 *     String id,
 *     String name,
 *     String domain,
 *     TenantStatus status,
 *     TenantPlan plan,
 *     LocalDateTime createdAt,
 *     LocalDateTime updatedAt,
 *     
 *     // Administrative fields
 *     TenantUsageStats usage,
 *     TenantBillingInfo billing,
 *     Map<String, Object> configuration,
 *     
 *     // Audit fields
 *     String createdBy,
 *     String lastModifiedBy
 * ) {
 *     
 *     /**
 *      * Creates a response with sensitive information filtered based on user role.
 *      *\/
 *     public static TenantResponse fromEntity(Tenant tenant, boolean includeSensitive) {
 *         return new TenantResponse(
 *             tenant.getId(),
 *             tenant.getName(),
 *             tenant.getDomain(),
 *             tenant.getStatus(),
 *             tenant.getPlan(),
 *             tenant.getCreatedAt(),
 *             tenant.getUpdatedAt(),
 *             includeSensitive ? tenant.getUsageStats() : null,
 *             includeSensitive ? tenant.getBillingInfo() : null,
 *             includeSensitive ? tenant.getConfiguration() : Map.of(),
 *             tenant.getCreatedBy(),
 *             tenant.getLastModifiedBy()
 *         );
 *     }
 * }
 * }</pre>
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 * @see javax.validation.constraints Validation annotations
 * @see com.fasterxml.jackson.annotation Jackson JSON annotations
 * @see com.byo.rag.shared.dto Shared DTO components
 * @see com.byo.rag.admin.controller Administrative controllers
 */
package com.byo.rag.admin.dto;
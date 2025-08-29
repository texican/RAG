/**
 * REST API controllers for administrative operations.
 * 
 * <p>This package contains REST controllers that provide the administrative API
 * for the Enterprise RAG System. Controllers handle administrative HTTP requests,
 * implement comprehensive security, and coordinate with service layers to execute
 * administrative operations across the multi-tenant platform.</p>
 * 
 * <h2>Controller Architecture</h2>
 * <p>Administrative controllers follow enterprise patterns with enhanced security:</p>
 * <ul>
 *   <li><strong>Authentication Controllers</strong> - Administrative login and token management</li>
 *   <li><strong>Tenant Management Controllers</strong> - Complete tenant lifecycle management</li>
 *   <li><strong>User Management Controllers</strong> - Cross-tenant user administration</li>
 *   <li><strong>System Monitoring Controllers</strong> - Real-time system health and metrics</li>
 *   <li><strong>Analytics Controllers</strong> - Usage analytics and reporting endpoints</li>
 *   <li><strong>Configuration Controllers</strong> - System configuration management</li>
 * </ul>
 * 
 * <h2>Administrative Security</h2>
 * <p>Enhanced security for administrative operations:</p>
 * <ul>
 *   <li><strong>Admin Authentication</strong> - Dedicated administrative JWT authentication</li>
 *   <li><strong>Role-Based Authorization</strong> - Granular administrative role permissions</li>
 *   <li><strong>Multi-Factor Authentication</strong> - Enhanced MFA requirements for admin access</li>
 *   <li><strong>IP Restrictions</strong> - Administrative endpoint IP whitelisting</li>
 *   <li><strong>Rate Limiting</strong> - Aggressive rate limiting on administrative endpoints</li>
 *   <li><strong>Audit Logging</strong> - Comprehensive logging of all administrative actions</li>
 * </ul>
 * 
 * <h2>Tenant Management API</h2>
 * <p>Comprehensive tenant administration endpoints:</p>
 * <ul>
 *   <li><strong>Tenant CRUD Operations</strong> - Create, read, update, and delete tenants</li>
 *   <li><strong>Tenant Search and Filtering</strong> - Advanced search and filtering capabilities</li>
 *   <li><strong>Tenant Status Management</strong> - Activation, suspension, and deactivation</li>
 *   <li><strong>Tenant Configuration</strong> - Per-tenant configuration and policy management</li>
 *   <li><strong>Tenant Analytics</strong> - Usage metrics and billing information</li>
 * </ul>
 * 
 * <h2>User Administration API</h2>
 * <p>Cross-tenant user management capabilities:</p>
 * <ul>
 *   <li><strong>User Search</strong> - Search users across all tenants with filtering</li>
 *   <li><strong>User Status Management</strong> - Administrative user status changes</li>
 *   <li><strong>Role Management</strong> - Administrative role assignment and permissions</li>
 *   <li><strong>Account Recovery</strong> - Administrative account recovery operations</li>
 *   <li><strong>User Analytics</strong> - User activity and usage reporting</li>
 * </ul>
 * 
 * <h2>System Monitoring API</h2>
 * <p>Real-time system monitoring and health endpoints:</p>
 * <ul>
 *   <li><strong>Health Dashboard</strong> - Comprehensive system health overview</li>
 *   <li><strong>Performance Metrics</strong> - Real-time performance and resource metrics</li>
 *   <li><strong>Error Monitoring</strong> - System error tracking and analysis</li>
 *   <li><strong>Service Status</strong> - Individual microservice health and status</li>
 *   <li><strong>Infrastructure Monitoring</strong> - Database, cache, and messaging health</li>
 * </ul>
 * 
 * <h2>Request Validation and Error Handling</h2>
 * <p>Comprehensive request processing:</p>
 * <ul>
 *   <li><strong>Input Validation</strong> - JSR-303 validation with custom validators</li>
 *   <li><strong>Security Validation</strong> - Administrative privilege validation</li>
 *   <li><strong>Business Rule Validation</strong> - Complex business logic validation</li>
 *   <li><strong>Error Response Formatting</strong> - Standardized error responses</li>
 *   <li><strong>Exception Translation</strong> - Service exceptions to HTTP status codes</li>
 * </ul>
 * 
 * <h2>API Documentation and Testing</h2>
 * <p>Complete API documentation and testing support:</p>
 * <ul>
 *   <li><strong>OpenAPI Documentation</strong> - Comprehensive API documentation</li>
 *   <li><strong>Example Requests/Responses</strong> - Complete examples for all endpoints</li>
 *   <li><strong>Error Documentation</strong> - Documentation of all error scenarios</li>
 *   <li><strong>Integration Tests</strong> - Comprehensive endpoint testing</li>
 *   <li><strong>Security Testing</strong> - Administrative security validation tests</li>
 * </ul>
 * 
 * <h2>Performance Optimization</h2>
 * <p>Controllers optimized for administrative workloads:</p>
 * <ul>
 *   <li><strong>Async Processing</strong> - Non-blocking operations for long-running tasks</li>
 *   <li><strong>Pagination Support</strong> - Efficient pagination for large datasets</li>
 *   <li><strong>Caching Headers</strong> - Appropriate cache control for administrative data</li>
 *   <li><strong>Batch Operations</strong> - Support for bulk administrative operations</li>
 *   <li><strong>Streaming Responses</strong> - Streaming support for large reports</li>
 * </ul>
 * 
 * <h2>Integration Patterns</h2>
 * <p>Controllers integrate with all RAG system components:</p>
 * <ul>
 *   <li><strong>Service Layer Integration</strong> - Clean separation with service layer</li>
 *   <li><strong>Event Publishing</strong> - Administrative event publishing for audit trails</li>
 *   <li><strong>Circuit Breaker Integration</strong> - Resilience patterns for external calls</li>
 *   <li><strong>Metrics Collection</strong> - Automatic metrics collection for admin operations</li>
 * </ul>
 * 
 * <h2>Monitoring and Observability</h2>
 * <p>Comprehensive controller monitoring:</p>
 * <ul>
 *   <li><strong>Request Metrics</strong> - HTTP request metrics and timing</li>
 *   <li><strong>Error Tracking</strong> - Administrative error tracking and alerting</li>
 *   <li><strong>Security Events</strong> - Security-related event monitoring</li>
 *   <li><strong>Performance Monitoring</strong> - Response time and throughput monitoring</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @RestController
 * @RequestMapping("/api/v1/admin/tenants")
 * @Validated
 * @PreAuthorize("hasRole('ADMIN')")
 * @Slf4j
 * public class TenantManagementController {
 *     
 *     private final TenantService tenantService;
 *     private final AdminAuditService auditService;
 *     
 *     @GetMapping
 *     @PreAuthorize("hasAuthority('TENANT_READ')")
 *     public ResponseEntity<Page<TenantResponse>> getTenants(
 *             @RequestParam(defaultValue = "0") int page,
 *             @RequestParam(defaultValue = "20") int size,
 *             @RequestParam(required = false) String search,
 *             @RequestParam(required = false) TenantStatus status) {
 *         
 *         Pageable pageable = PageRequest.of(page, size);
 *         TenantSearchCriteria criteria = TenantSearchCriteria.builder()
 *             .search(search)
 *             .status(status)
 *             .build();
 *         
 *         Page<Tenant> tenants = tenantService.searchTenants(criteria, pageable);
 *         Page<TenantResponse> response = tenants.map(TenantMapper::toResponse);
 *         
 *         return ResponseEntity.ok(response);
 *     }
 *     
 *     @PostMapping
 *     @PreAuthorize("hasAuthority('TENANT_CREATE')")
 *     public ResponseEntity<TenantResponse> createTenant(
 *             @Valid @RequestBody TenantCreateRequest request,
 *             Authentication authentication) {
 *         
 *         // Create tenant
 *         Tenant tenant = tenantService.createTenant(request);
 *         TenantResponse response = TenantMapper.toResponse(tenant);
 *         
 *         // Log administrative action
 *         auditService.logTenantCreation(
 *             authentication.getName(), 
 *             tenant.getId(), 
 *             request
 *         );
 *         
 *         return ResponseEntity.status(HttpStatus.CREATED).body(response);
 *     }
 * }
 * }</pre>
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 * @see org.springframework.web.bind.annotation Spring MVC annotations
 * @see org.springframework.security.access.prepost Method security
 * @see com.enterprise.rag.admin.service Administrative service layer
 * @see com.enterprise.rag.admin.dto Administrative DTOs
 */
package com.enterprise.rag.admin.controller;
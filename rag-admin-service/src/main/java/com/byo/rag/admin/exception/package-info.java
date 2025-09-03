/**
 * Specialized exception classes for administrative operations.
 * 
 * <p>This package contains specialized exception classes that handle
 * administrative operation errors in the Enterprise RAG System. These
 * exceptions provide enhanced context, security considerations, and
 * comprehensive error handling specifically designed for administrative
 * workflows and operations.</p>
 * 
 * <h2>Administrative Exception Hierarchy</h2>
 * <p>Specialized exception hierarchy for administrative operations:</p>
 * <ul>
 *   <li><strong>Administrative Base Exceptions</strong> - Common administrative exception patterns</li>
 *   <li><strong>Tenant Management Exceptions</strong> - Tenant lifecycle and management errors</li>
 *   <li><strong>User Administration Exceptions</strong> - Cross-tenant user management errors</li>
 *   <li><strong>Security Exceptions</strong> - Administrative security and access control errors</li>
 *   <li><strong>System Monitoring Exceptions</strong> - Monitoring and health check errors</li>
 *   <li><strong>Analytics Exceptions</strong> - Reporting and analytics processing errors</li>
 * </ul>
 * 
 * <h2>Enhanced Error Context</h2>
 * <p>Administrative exceptions include comprehensive context information:</p>
 * <ul>
 *   <li><strong>Administrative Context</strong> - Admin user and operation context</li>
 *   <li><strong>Security Context</strong> - Security implications and risk assessment</li>
 *   <li><strong>Tenant Context</strong> - Multi-tenant context for administrative operations</li>
 *   <li><strong>Operation Context</strong> - Specific administrative operation details</li>
 *   <li><strong>System Context</strong> - System state and resource information</li>
 * </ul>
 * 
 * <h2>Security-Aware Exception Handling</h2>
 * <p>Administrative exceptions implement security-conscious error handling:</p>
 * <ul>
 *   <li><strong>Information Disclosure Prevention</strong> - Careful control of sensitive information exposure</li>
 *   <li><strong>Security Event Logging</strong> - Automatic security event logging for exceptions</li>
 *   <li><strong>Audit Trail Integration</strong> - Comprehensive audit trail for administrative errors</li>
 *   <li><strong>Privilege Context</strong> - Administrative privilege validation in exceptions</li>
 *   <li><strong>Rate Limiting Integration</strong> - Exception-based rate limiting for security</li>
 * </ul>
 * 
 * <h2>Tenant Management Exceptions</h2>
 * <p>Specialized exceptions for tenant management operations:</p>
 * <ul>
 *   <li><strong>Tenant Creation Exceptions</strong> - Errors during tenant provisioning</li>
 *   <li><strong>Tenant Configuration Exceptions</strong> - Configuration validation and update errors</li>
 *   <li><strong>Tenant Status Exceptions</strong> - Tenant lifecycle and status management errors</li>
 *   <li><strong>Tenant Resource Exceptions</strong> - Resource allocation and capacity errors</li>
 *   <li><strong>Tenant Migration Exceptions</strong> - Data migration and consolidation errors</li>
 * </ul>
 * 
 * <h2>User Administration Exceptions</h2>
 * <p>Cross-tenant user management exception handling:</p>
 * <ul>
 *   <li><strong>User Search Exceptions</strong> - Advanced search and filtering errors</li>
 *   <li><strong>User Status Exceptions</strong> - Administrative user status management errors</li>
 *   <li><strong>Role Management Exceptions</strong> - Role assignment and permission errors</li>
 *   <li><strong>Account Recovery Exceptions</strong> - Administrative account recovery errors</li>
 *   <li><strong>Compliance Exceptions</strong> - GDPR and privacy regulation compliance errors</li>
 * </ul>
 * 
 * <h2>System Monitoring Exceptions</h2>
 * <p>Specialized exceptions for system monitoring and health operations:</p>
 * <ul>
 *   <li><strong>Health Check Exceptions</strong> - System health monitoring errors</li>
 *   <li><strong>Performance Monitoring Exceptions</strong> - Performance metrics collection errors</li>
 *   <li><strong>Alert Configuration Exceptions</strong> - Alerting setup and configuration errors</li>
 *   <li><strong>Metrics Collection Exceptions</strong> - Metrics gathering and processing errors</li>
 *   <li><strong>System Resource Exceptions</strong> - Resource monitoring and capacity errors</li>
 * </ul>
 * 
 * <h2>Analytics and Reporting Exceptions</h2>
 * <p>Specialized exceptions for analytics and reporting operations:</p>
 * <ul>
 *   <li><strong>Report Generation Exceptions</strong> - Report creation and processing errors</li>
 *   <li><strong>Data Aggregation Exceptions</strong> - Data processing and aggregation errors</li>
 *   <li><strong>Export Exceptions</strong> - Data export and integration errors</li>
 *   <li><strong>Dashboard Exceptions</strong> - Dashboard configuration and rendering errors</li>
 *   <li><strong>Analytics Query Exceptions</strong> - Complex analytics query processing errors</li>
 * </ul>
 * 
 * <h2>Administrative HTTP Status Mapping</h2>
 * <p>Administrative exceptions map to appropriate HTTP status codes:</p>
 * <ul>
 *   <li><strong>400 Bad Request</strong> - Administrative input validation and parameter errors</li>
 *   <li><strong>401 Unauthorized</strong> - Administrative authentication failures</li>
 *   <li><strong>403 Forbidden</strong> - Administrative authorization and privilege errors</li>
 *   <li><strong>404 Not Found</strong> - Administrative resource not found errors</li>
 *   <li><strong>409 Conflict</strong> - Administrative business rule and constraint violations</li>
 *   <li><strong>422 Unprocessable Entity</strong> - Administrative business logic validation errors</li>
 *   <li><strong>429 Too Many Requests</strong> - Administrative rate limiting and throttling</li>
 *   <li><strong>500 Internal Server Error</strong> - Administrative system errors</li>
 *   <li><strong>503 Service Unavailable</strong> - Administrative service degradation</li>
 * </ul>
 * 
 * <h2>Error Recovery and Resilience</h2>
 * <p>Administrative exceptions support error recovery patterns:</p>
 * <ul>
 *   <li><strong>Retry Strategies</strong> - Intelligent retry logic for transient errors</li>
 *   <li><strong>Circuit Breaker Integration</strong> - Circuit breaker pattern for external dependencies</li>
 *   <li><strong>Graceful Degradation</strong> - Fallback strategies for non-critical operations</li>
 *   <li><strong>Compensation Actions</strong> - Automatic rollback and compensation for failed operations</li>
 * </ul>
 * 
 * <h2>Compliance and Audit Integration</h2>
 * <p>Administrative exceptions support compliance and audit requirements:</p>
 * <ul>
 *   <li><strong>Audit Trail Exceptions</strong> - Automatic audit logging for all administrative errors</li>
 *   <li><strong>Compliance Violations</strong> - Specialized handling for compliance-related errors</li>
 *   <li><strong>Data Privacy Errors</strong> - GDPR and privacy regulation error handling</li>
 *   <li><strong>Security Compliance</strong> - Security framework compliance error handling</li>
 * </ul>
 * 
 * <h2>Monitoring and Alerting Integration</h2>
 * <p>Administrative exceptions integrate with monitoring systems:</p>
 * <ul>
 *   <li><strong>Metrics Integration</strong> - Automatic metrics collection for administrative errors</li>
 *   <li><strong>Alert Triggers</strong> - Automatic alerting for critical administrative errors</li>
 *   <li><strong>Health Check Integration</strong> - Exception-based health status updates</li>
 *   <li><strong>Performance Impact</strong> - Performance impact assessment for administrative errors</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * /**
 *  * Specialized exception for tenant management operations.
 *  * Includes enhanced context and security considerations.
 *  *\/
 * public class TenantManagementException extends AdminServiceException {
 *     
 *     private final String tenantId;
 *     private final TenantOperation operation;
 *     private final Map<String, Object> operationContext;
 *     
 *     public TenantManagementException(
 *             String message, 
 *             String tenantId, 
 *             TenantOperation operation,
 *             Map<String, Object> context) {
 *         super(message, "TENANT_MANAGEMENT_ERROR");
 *         this.tenantId = tenantId;
 *         this.operation = operation;
 *         this.operationContext = context != null ? context : Map.of();
 *         
 *         // Add administrative context
 *         addContext("tenantId", tenantId);
 *         addContext("operation", operation.name());
 *         addContext("timestamp", Instant.now());
 *         addContext("operationContext", operationContext);
 *     }
 *     
 *     @Override
 *     public HttpStatus getHttpStatus() {
 *         return switch (operation) {
 *             case CREATE -> HttpStatus.CONFLICT;
 *             case UPDATE -> HttpStatus.UNPROCESSABLE_ENTITY;
 *             case DELETE -> HttpStatus.CONFLICT;
 *             case ACTIVATE, SUSPEND -> HttpStatus.UNPROCESSABLE_ENTITY;
 *             default -> HttpStatus.BAD_REQUEST;
 *         };
 *     }
 *     
 *     @Override
 *     public boolean isSecuritySensitive() {
 *         return operation == TenantOperation.CREATE || 
 *                operation == TenantOperation.DELETE ||
 *                operation == TenantOperation.SUSPEND;
 *     }
 *     
 *     @Override
 *     public Map<String, Object> getSanitizedContext() {
 *         Map<String, Object> sanitized = new HashMap<>(getErrorContext());
 *         
 *         // Remove sensitive information from context
 *         sanitized.remove("adminApiKey");
 *         sanitized.remove("sensitiveConfiguration");
 *         
 *         return sanitized;
 *     }
 * }
 * 
 * @RestControllerAdvice
 * public class AdminExceptionHandler extends GlobalExceptionHandler {
 *     
 *     private final AdminAuditService auditService;
 *     private final AdminSecurityService securityService;
 *     
 *     @ExceptionHandler(TenantManagementException.class)
 *     public ResponseEntity<AdminErrorResponse> handleTenantManagement(
 *             TenantManagementException ex, 
 *             HttpServletRequest request,
 *             Authentication authentication) {
 *         
 *         // Log administrative error with security context
 *         auditService.logAdministrativeError(
 *             authentication.getName(),
 *             ex.getOperation(),
 *             ex.getTenantId(),
 *             ex
 *         );
 *         
 *         // Check for security implications
 *         if (ex.isSecuritySensitive()) {
 *             securityService.handleSecuritySensitiveError(ex, authentication);
 *         }
 *         
 *         AdminErrorResponse response = AdminErrorResponse.builder()
 *             .code(ex.getErrorCode())
 *             .message(ex.getMessage())
 *             .operation(ex.getOperation().name())
 *             .timestamp(Instant.now())
 *             .path(request.getRequestURI())
 *             .correlationId(RequestContextUtils.getCorrelationId())
 *             .context(ex.getSanitizedContext())
 *             .build();
 *         
 *         return ResponseEntity
 *             .status(ex.getHttpStatus())
 *             .body(response);
 *     }
 * }
 * }</pre>
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 * @see org.springframework.web.bind.annotation.RestControllerAdvice Exception handling
 * @see org.springframework.http.ResponseEntity HTTP response handling
 * @see com.byo.rag.shared.exception Base exception classes
 * @see com.byo.rag.admin.dto Administrative error response DTOs
 */
package com.byo.rag.admin.exception;
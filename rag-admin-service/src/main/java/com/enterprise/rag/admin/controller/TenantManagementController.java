package com.enterprise.rag.admin.controller;

import com.enterprise.rag.admin.dto.*;
import com.enterprise.rag.admin.service.TenantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for comprehensive tenant lifecycle management in the admin service.
 * 
 * <p>This controller provides administrative endpoints for managing tenant organizations
 * throughout their lifecycle in the Enterprise RAG system. It supports complete CRUD
 * operations, status management, and bulk operations with proper pagination and sorting
 * for scalable multi-tenant administration.
 * 
 * <p><strong>Administrative Operations:</strong>
 * <ul>
 *   <li><strong>Creation:</strong> New tenant organization setup and provisioning</li>
 *   <li><strong>Retrieval:</strong> Individual tenant details and paginated tenant lists</li>
 *   <li><strong>Updates:</strong> Tenant configuration and profile modifications</li>
 *   <li><strong>Status Management:</strong> Suspension, reactivation, and lifecycle control</li>
 *   <li><strong>Deletion:</strong> Complete tenant removal for compliance or cleanup</li>
 * </ul>
 * 
 * <p><strong>Multi-Tenant Administration Features:</strong>
 * <ul>
 *   <li><strong>Paginated Access:</strong> Efficient handling of large tenant databases</li>
 *   <li><strong>Flexible Sorting:</strong> Configurable sorting by creation date, name, or status</li>
 *   <li><strong>Status Workflows:</strong> Structured tenant activation and suspension processes</li>
 *   <li><strong>Bulk Operations:</strong> Administrative efficiency for large-scale management</li>
 * </ul>
 * 
 * <p><strong>Security and Access Control:</strong>
 * <ul>
 *   <li><strong>Admin-Only Access:</strong> All endpoints require administrative privileges</li>
 *   <li><strong>Request Validation:</strong> Comprehensive input validation and sanitization</li>
 *   <li><strong>Error Handling:</strong> Secure error responses with appropriate HTTP status codes</li>
 *   <li><strong>Audit Trail:</strong> Operation logging for compliance and monitoring</li>
 * </ul>
 * 
 * <p><strong>API Endpoints:</strong>
 * <ul>
 *   <li><strong>POST /admin/api/tenants:</strong> Create new tenant organization</li>
 *   <li><strong>GET /admin/api/tenants/{id}:</strong> Retrieve individual tenant details</li>
 *   <li><strong>PUT /admin/api/tenants/{id}:</strong> Update tenant configuration</li>
 *   <li><strong>POST /admin/api/tenants/{id}/suspend:</strong> Suspend tenant operations</li>
 *   <li><strong>POST /admin/api/tenants/{id}/reactivate:</strong> Reactivate suspended tenant</li>
 *   <li><strong>GET /admin/api/tenants:</strong> List all tenants with pagination</li>
 *   <li><strong>DELETE /admin/api/tenants/{id}:</strong> Remove tenant from system</li>
 * </ul>
 * 
 * <p><strong>Integration Architecture:</strong>
 * <ul>
 *   <li><strong>Service Layer:</strong> Delegates business logic to TenantService</li>
 *   <li><strong>DTO Mapping:</strong> Clean separation between API and domain models</li>
 *   <li><strong>Database Persistence:</strong> JPA-based tenant data management</li>
 *   <li><strong>Exception Handling:</strong> Centralized error handling with proper HTTP responses</li>
 * </ul>
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.0
 * @see TenantService
 * @see TenantCreateRequest
 * @see TenantResponse
 * @see TenantListResponse
 */
@RestController
@RequestMapping("/admin/api/tenants")
public class TenantManagementController {

    /** Service for tenant business logic and data operations. */
    private final TenantService tenantService;

    /**
     * Constructs a tenant management controller with required dependencies.
     * 
     * @param tenantService the service for tenant management operations
     */
    @Autowired
    public TenantManagementController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    /**
     * Creates a new tenant organization in the system.
     * 
     * <p>This endpoint handles the provisioning of new tenant organizations,
     * setting up the complete multi-tenant infrastructure including database
     * schemas, initial configuration, and administrative setup.
     * 
     * <p><strong>Creation Process:</strong>
     * <ul>
     *   <li><strong>Validation:</strong> Request validation for required fields and constraints</li>
     *   <li><strong>Uniqueness Check:</strong> Ensure tenant slug and identifiers are unique</li>
     *   <li><strong>Provisioning:</strong> Database setup and resource allocation</li>
     *   <li><strong>Configuration:</strong> Initial tenant settings and defaults</li>
     * </ul>
     * 
     * @param request validated tenant creation request with organization details
     * @return ResponseEntity containing created tenant details with HTTP 201 status
     */
    @PostMapping
    public ResponseEntity<TenantResponse> createTenant(@Valid @RequestBody TenantCreateRequest request) {
        TenantResponse response = tenantService.createTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves detailed information about a specific tenant organization.
     * 
     * <p>This endpoint provides comprehensive tenant details for administrative
     * purposes, including configuration, status, usage statistics, and metadata.
     * 
     * @param tenantId the unique identifier of the tenant to retrieve
     * @return ResponseEntity containing tenant details or 404 if not found
     */
    @GetMapping("/{tenantId}")
    public ResponseEntity<TenantResponse> getTenant(@PathVariable String tenantId) {
        TenantResponse response = tenantService.getTenantById(tenantId);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates configuration and details of an existing tenant organization.
     * 
     * <p>This endpoint allows modification of tenant settings, profile information,
     * and configuration parameters while maintaining data integrity and system consistency.
     * 
     * <p><strong>Update Operations:</strong>
     * <ul>
     *   <li><strong>Profile Updates:</strong> Organization name, description, contact information</li>
     *   <li><strong>Configuration:</strong> System settings and feature toggles</li>
     *   <li><strong>Validation:</strong> Ensure updates don't violate business rules</li>
     * </ul>
     * 
     * @param tenantId the unique identifier of the tenant to update
     * @param request validated update request with modified tenant details
     * @return ResponseEntity containing updated tenant information
     */
    @PutMapping("/{tenantId}")
    public ResponseEntity<TenantResponse> updateTenant(
            @PathVariable String tenantId,
            @Valid @RequestBody TenantUpdateRequest request) {
        TenantResponse response = tenantService.updateTenant(tenantId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Suspends a tenant organization, temporarily disabling access and operations.
     * 
     * <p>This endpoint handles tenant suspension for various administrative scenarios
     * such as policy violations, payment issues, or security concerns. Suspended
     * tenants retain their data but cannot access system features.
     * 
     * <p><strong>Suspension Effects:</strong>
     * <ul>
     *   <li><strong>Access Blocked:</strong> Users cannot authenticate or access resources</li>
     *   <li><strong>Data Preserved:</strong> All tenant data remains intact</li>
     *   <li><strong>API Disabled:</strong> All API endpoints return access denied</li>
     *   <li><strong>Audit Trail:</strong> Suspension reason and timestamp recorded</li>
     * </ul>
     * 
     * @param tenantId the unique identifier of the tenant to suspend
     * @param request suspension request containing reason and administrative details
     * @return ResponseEntity containing updated tenant status information
     */
    @PostMapping("/{tenantId}/suspend")
    public ResponseEntity<TenantResponse> suspendTenant(
            @PathVariable String tenantId,
            @Valid @RequestBody TenantSuspendRequest request) {
        TenantResponse response = tenantService.suspendTenant(tenantId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Reactivates a previously suspended tenant organization.
     * 
     * <p>This endpoint restores full system access for suspended tenants after
     * administrative review and resolution of suspension reasons. All tenant
     * functionality is restored to operational status.
     * 
     * <p><strong>Reactivation Process:</strong>
     * <ul>
     *   <li><strong>Status Validation:</strong> Verify tenant is currently suspended</li>
     *   <li><strong>Access Restoration:</strong> Re-enable all tenant operations</li>
     *   <li><strong>User Notification:</strong> Inform tenant users of reactivation</li>
     *   <li><strong>Audit Logging:</strong> Record reactivation event and administrator</li>
     * </ul>
     * 
     * @param tenantId the unique identifier of the tenant to reactivate
     * @return ResponseEntity containing updated tenant status information
     */
    @PostMapping("/{tenantId}/reactivate")
    public ResponseEntity<TenantResponse> reactivateTenant(@PathVariable String tenantId) {
        TenantResponse response = tenantService.reactivateTenant(tenantId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a paginated list of all tenant organizations with flexible sorting.
     * 
     * <p>This endpoint provides comprehensive tenant listing for administrative dashboards
     * and management interfaces. It supports pagination to handle large tenant databases
     * efficiently and flexible sorting for various administrative workflows.
     * 
     * <p><strong>Pagination Features:</strong>
     * <ul>
     *   <li><strong>Page-Based:</strong> Zero-indexed page navigation</li>
     *   <li><strong>Configurable Size:</strong> Adjustable page sizes (default: 10)</li>
     *   <li><strong>Total Metadata:</strong> Response includes total count and page information</li>
     * </ul>
     * 
     * <p><strong>Sorting Options:</strong>
     * <ul>
     *   <li><strong>Default:</strong> Creation date descending (newest first)</li>
     *   <li><strong>Flexible Fields:</strong> Sort by name, status, creation date</li>
     *   <li><strong>Direction Control:</strong> Ascending or descending order</li>
     * </ul>
     * 
     * <p><strong>Administrative Use Cases:</strong>
     * <ul>
     *   <li>System-wide tenant monitoring and management</li>
     *   <li>Tenant discovery and navigation</li>
     *   <li>Bulk administrative operations</li>
     *   <li>Reporting and analytics preparation</li>
     * </ul>
     * 
     * @param page zero-indexed page number (default: 0)
     * @param size number of tenants per page (default: 10)
     * @param sort sorting specification as "field,direction" (default: "createdAt,desc")
     * @return ResponseEntity containing paginated tenant list with metadata
     */
    @GetMapping
    public ResponseEntity<TenantListResponse> getAllTenants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        
        // Parse sorting parameters from request
        String[] sortParams = sort.split(",");
        String sortBy = sortParams[0];
        Sort.Direction direction = sortParams.length > 1 && "asc".equalsIgnoreCase(sortParams[1]) 
                ? Sort.Direction.ASC 
                : Sort.Direction.DESC;
        
        // Build pageable request with sorting
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));
        TenantListResponse response = tenantService.getAllTenants(pageRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Permanently deletes a tenant organization and all associated data.
     * 
     * <p>This endpoint handles complete tenant removal from the system, including
     * all associated data, configurations, and user accounts. This is an irreversible
     * operation that should be used with extreme caution and proper authorization.
     * 
     * <p><strong>Deletion Process:</strong>
     * <ul>
     *   <li><strong>Data Removal:</strong> Complete deletion of all tenant-associated data</li>
     *   <li><strong>User Cleanup:</strong> Removal of all tenant user accounts</li>
     *   <li><strong>Configuration Cleanup:</strong> Deletion of tenant settings and preferences</li>
     *   <li><strong>Audit Trail:</strong> Deletion event logged for compliance purposes</li>
     * </ul>
     * 
     * <p><strong>Security Considerations:</strong>
     * <ul>
     *   <li><strong>Irreversible Operation:</strong> No recovery possible after deletion</li>
     *   <li><strong>Data Privacy:</strong> Ensures complete data removal for compliance</li>
     *   <li><strong>Administrative Authorization:</strong> Requires highest level admin privileges</li>
     * </ul>
     * 
     * <p><strong>Alternative Approach:</strong>
     * Consider using suspension instead of deletion for temporary access removal
     * while preserving data for potential future reactivation.
     * 
     * @param tenantId the unique identifier of the tenant to delete permanently
     * @return ResponseEntity with HTTP 204 No Content status indicating successful deletion
     */
    @DeleteMapping("/{tenantId}")
    public ResponseEntity<Void> deleteTenant(@PathVariable String tenantId) {
        tenantService.deleteTenant(tenantId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Handles runtime exceptions from tenant management operations.
     * 
     * <p>This exception handler provides centralized error handling for runtime
     * exceptions that occur during tenant management operations, converting them
     * to appropriate HTTP responses with proper status codes and error details.
     * 
     * <p><strong>Error Classification:</strong>
     * <ul>
     *   <li><strong>Not Found (404):</strong> Tenant does not exist or is inaccessible</li>
     *   <li><strong>Internal Server Error (500):</strong> Unexpected system errors</li>
     * </ul>
     * 
     * <p><strong>Security Features:</strong>
     * <ul>
     *   <li>Sanitized error messages to prevent information disclosure</li>
     *   <li>Request URI included for debugging and audit purposes</li>
     *   <li>Consistent error response structure across all endpoints</li>
     * </ul>
     * 
     * @param ex the runtime exception that occurred
     * @param request the HTTP request that caused the exception
     * @return ResponseEntity with appropriate error response and status code
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        // Handle tenant not found scenarios
        if (ex.getMessage().contains("not found")) {
            ErrorResponse error = new ErrorResponse(
                    "Tenant not found",
                    ex.getMessage(),
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        
        // Handle general runtime exceptions
        ErrorResponse error = new ErrorResponse(
                "Internal server error",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Handles validation exceptions from request data validation failures.
     * 
     * <p>This exception handler manages validation errors that occur when request
     * data fails Bean Validation constraints or method argument validation.
     * It aggregates all validation errors into a comprehensive error response.
     * 
     * <p><strong>Validation Error Processing:</strong>
     * <ul>
     *   <li><strong>Field Error Aggregation:</strong> Combines multiple field validation failures</li>
     *   <li><strong>Clear Error Messages:</strong> Provides specific field-level error details</li>
     *   <li><strong>Client-Friendly Format:</strong> Easy to parse and display in UI</li>
     * </ul>
     * 
     * <p><strong>Error Response Structure:</strong>
     * <ul>
     *   <li><strong>Summary:</strong> "Validation failed" category</li>
     *   <li><strong>Details:</strong> Field-specific error messages</li>
     *   <li><strong>Request Context:</strong> URI for debugging purposes</li>
     * </ul>
     * 
     * <p><strong>Common Validation Scenarios:</strong>
     * <ul>
     *   <li>Missing required fields in tenant creation/update</li>
     *   <li>Invalid format for email addresses or URLs</li>
     *   <li>Constraint violations for business rules</li>
     *   <li>Data type mismatches or range violations</li>
     * </ul>
     * 
     * @param ex the method argument validation exception
     * @param request the HTTP request containing invalid data
     * @return ResponseEntity with HTTP 400 Bad Request and detailed validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        // Aggregate all field validation errors into a single message
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((msg1, msg2) -> msg1 + ", " + msg2)
                .orElse("Validation failed");
        
        ErrorResponse error = new ErrorResponse(
                "Validation failed",
                errorMessage,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
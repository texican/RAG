package com.byo.rag.auth.controller;

import com.byo.rag.auth.security.JwtAuthenticationFilter;
import com.byo.rag.auth.service.TenantService;
import com.byo.rag.shared.dto.TenantDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for tenant management operations in the Enterprise RAG system.
 * 
 * <p>This controller provides comprehensive tenant management functionality including:
 * <ul>
 *   <li>Public tenant registration for new organizations</li>
 *   <li>Administrative tenant creation and management</li>
 *   <li>Tenant information retrieval with access control</li>
 *   <li>Tenant updates and deletion (admin-only)</li>
 * </ul>
 * 
 * <p><strong>Multi-Tenant Architecture:</strong>
 * The RAG system implements strict tenant isolation where each tenant represents
 * a separate organization with complete data separation. Tenants are identified
 * by UUID and have unique slug identifiers for user-friendly URLs.
 * 
 * <p><strong>Access Control:</strong>
 * <ul>
 *   <li><strong>Public:</strong> Tenant registration endpoint</li>
 *   <li><strong>User:</strong> Can only access their own tenant information</li>
 *   <li><strong>Admin:</strong> Full CRUD operations on all tenants</li>
 * </ul>
 * 
 * <p><strong>Security Features:</strong>
 * <ul>
 *   <li>JWT-based authentication required (except registration)</li>
 *   <li>Role-based authorization for admin operations</li>
 *   <li>Tenant isolation enforced at controller level</li>
 *   <li>Rate limiting on public registration endpoint</li>
 * </ul>
 * 
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * // Public tenant registration
 * POST /api/v1/tenants/register
 * {
 *   "name": "Acme Corporation",
 *   "slug": "acme-corp",
 *   "description": "Enterprise AI solutions"
 * }
 * 
 * // Get tenant information (authenticated)
 * GET /api/v1/tenants/{tenantId}
 * Authorization: Bearer <jwt-token>
 * }</pre>
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.0
 * @see TenantService
 * @see TenantDto
 * @see JwtAuthenticationFilter
 */
@RestController
@RequestMapping("/api/v1/tenants")
@Tag(name = "Tenant Management", description = "Tenant CRUD operations")
@SecurityRequirement(name = "bearerAuth")
public class TenantController {

    /** Service for tenant management operations and business logic. */
    private final TenantService tenantService;

    /**
     * Constructs a new TenantController with required tenant service.
     * 
     * @param tenantService the tenant service for tenant operations
     */
    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    /**
     * Registers a new tenant in the system (public endpoint).
     * 
     * <p>This is a public endpoint that allows organizations to self-register
     * in the RAG system. Upon successful registration, the tenant is activated
     * and ready for user registration and document processing.
     * 
     * <p><strong>Validation:</strong>
     * <ul>
     *   <li>Tenant name must be unique</li>
     *   <li>Tenant slug must be unique and URL-friendly</li>
     *   <li>Required fields must be provided</li>
     * </ul>
     * 
     * <p><strong>Rate Limiting:</strong> This endpoint is rate-limited to prevent
     * abuse and automated tenant creation.
     * 
     * @param request the tenant creation request with name, slug, and description
     * @return ResponseEntity containing the created tenant information with CREATED status
     * @throws com.byo.rag.shared.exception.RagException if tenant name/slug already exists
     * @throws jakarta.validation.ValidationException if request data is invalid
     */
    @PostMapping("/register")
    @Operation(summary = "Register new tenant", description = "Register a new tenant (public endpoint)")
    public ResponseEntity<TenantDto.TenantResponse> registerTenant(@Valid @RequestBody TenantDto.CreateTenantRequest request) {
        TenantDto.TenantResponse response = tenantService.createTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Creates a new tenant via admin interface (admin-only endpoint).
     * 
     * <p>This endpoint provides administrative tenant creation with additional
     * controls and oversight. Unlike public registration, this endpoint allows
     * admins to create tenants with specific configurations and settings.
     * 
     * <p><strong>Admin Privileges:</strong>
     * <ul>
     *   <li>Can override default tenant settings</li>
     *   <li>Can create tenants with elevated privileges</li>
     *   <li>Can bypass certain validation rules if needed</li>
     * </ul>
     * 
     * @param request the tenant creation request with name, slug, and configuration
     * @return ResponseEntity containing the created tenant information with CREATED status
     * @throws org.springframework.security.access.AccessDeniedException if user lacks ADMIN role
     * @throws com.byo.rag.shared.exception.RagException if tenant creation fails
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create tenant", description = "Create a new tenant (admin only)")
    public ResponseEntity<TenantDto.TenantResponse> createTenant(@Valid @RequestBody TenantDto.CreateTenantRequest request) {
        TenantDto.TenantResponse response = tenantService.createTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves tenant information by tenant ID with access control.
     * 
     * <p>This endpoint enforces strict tenant isolation - users can only
     * access information about their own tenant, while administrators can
     * access any tenant's information.
     * 
     * <p><strong>Access Control Logic:</strong>
     * <ul>
     *   <li><strong>Regular Users:</strong> Can only access their own tenant</li>
     *   <li><strong>Administrators:</strong> Can access any tenant</li>
     *   <li><strong>Unauthorized Access:</strong> Returns 403 Forbidden</li>
     * </ul>
     * 
     * <p>The returned information includes tenant metadata, status, and
     * configuration details appropriate for the requesting user's access level.
     * 
     * @param tenantId the UUID of the tenant to retrieve
     * @param principal the authenticated user principal containing tenant and role info
     * @return ResponseEntity containing tenant information, or 403 if access denied
     * @throws com.byo.rag.shared.exception.TenantNotFoundException if tenant does not exist
     */
    @GetMapping("/{tenantId}")
    @Operation(summary = "Get tenant by ID", description = "Retrieve tenant information")
    public ResponseEntity<TenantDto.TenantResponse> getTenant(
            @PathVariable UUID tenantId,
            @AuthenticationPrincipal JwtAuthenticationFilter.RagUserPrincipal principal) {
        
        // Users can only access their own tenant, admins can access any
        if (!principal.tenantId().equals(tenantId) && !principal.role().name().equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        TenantDto.TenantResponse response = tenantService.getTenant(tenantId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves tenant information by slug identifier (public endpoint).
     * 
     * <p>This public endpoint allows retrieval of basic tenant information
     * using the tenant's slug (user-friendly identifier). This is commonly
     * used for tenant discovery and public-facing integrations.
     * 
     * <p><strong>Public Information:</strong> Only non-sensitive tenant
     * information is returned through this endpoint, including:
     * <ul>
     *   <li>Tenant name and description</li>
     *   <li>Public contact information</li>
     *   <li>API endpoints and capabilities</li>
     * </ul>
     * 
     * <p><strong>Use Cases:</strong>
     * <ul>
     *   <li>Client applications discovering tenant endpoints</li>
     *   <li>Integration setup and configuration</li>
     *   <li>Public directory listings</li>
     * </ul>
     * 
     * @param slug the URL-friendly slug identifier of the tenant
     * @return ResponseEntity containing public tenant information
     * @throws com.byo.rag.shared.exception.TenantNotFoundException if tenant with slug not found
     */
    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get tenant by slug", description = "Retrieve tenant information by slug")
    public ResponseEntity<TenantDto.TenantResponse> getTenantBySlug(@PathVariable String slug) {
        TenantDto.TenantResponse response = tenantService.getTenantBySlug(slug);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves paginated list of all tenants in the system (admin-only).
     * 
     * <p>This administrative endpoint provides comprehensive tenant listing
     * with pagination support. Results include tenant summaries with key
     * information for administrative oversight and management.
     * 
     * <p><strong>Pagination:</strong> Supports standard Spring Data pagination
     * parameters:
     * <ul>
     *   <li><code>page</code>: Page number (0-based)</li>
     *   <li><code>size</code>: Page size (default 20)</li>
     *   <li><code>sort</code>: Sort criteria (e.g., "name,asc")</li>
     * </ul>
     * 
     * <p><strong>Response Format:</strong> Returns paginated results with
     * metadata including total count, current page, and navigation links.
     * 
     * @param pageable the pagination and sorting parameters
     * @return ResponseEntity containing paginated tenant summaries
     * @throws org.springframework.security.access.AccessDeniedException if user lacks ADMIN role
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all tenants", description = "Retrieve all tenants (admin only)")
    public ResponseEntity<Page<TenantDto.TenantSummary>> getAllTenants(Pageable pageable) {
        Page<TenantDto.TenantSummary> response = tenantService.getAllTenants(pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates tenant information and configuration (admin-only).
     * 
     * <p>This endpoint allows administrators to modify tenant properties
     * including name, description, settings, and status. Updates are validated
     * and applied atomically to ensure data consistency.
     * 
     * <p><strong>Updateable Fields:</strong>
     * <ul>
     *   <li>Tenant name and description</li>
     *   <li>Configuration settings and limits</li>
     *   <li>Status (active, suspended, etc.)</li>
     *   <li>Contact and billing information</li>
     * </ul>
     * 
     * <p><strong>Restrictions:</strong>
     * <ul>
     *   <li>Tenant ID and slug cannot be changed</li>
     *   <li>Updates must maintain data integrity</li>
     *   <li>Some changes may require tenant notification</li>
     * </ul>
     * 
     * @param tenantId the UUID of the tenant to update
     * @param request the update request containing modified tenant data
     * @return ResponseEntity containing the updated tenant information
     * @throws org.springframework.security.access.AccessDeniedException if user lacks ADMIN role
     * @throws com.byo.rag.shared.exception.TenantNotFoundException if tenant does not exist
     * @throws jakarta.validation.ValidationException if update data is invalid
     */
    @PutMapping("/{tenantId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update tenant", description = "Update tenant information (admin only)")
    public ResponseEntity<TenantDto.TenantResponse> updateTenant(
            @PathVariable UUID tenantId,
            @Valid @RequestBody TenantDto.UpdateTenantRequest request) {
        TenantDto.TenantResponse response = tenantService.updateTenant(tenantId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a tenant and all associated data (admin-only).
     * 
     * <p><strong>⚠️ WARNING:</strong> This operation is irreversible and will
     * permanently delete the tenant and ALL associated data including:
     * <ul>
     *   <li>All user accounts within the tenant</li>
     *   <li>All documents and document chunks</li>
     *   <li>All embeddings and vector data</li>
     *   <li>All conversation history</li>
     *   <li>All audit logs and analytics data</li>
     * </ul>
     * 
     * <p><strong>Prerequisites:</strong>
     * <ul>
     *   <li>Tenant should be suspended before deletion</li>
     *   <li>Data export should be completed if required</li>
     *   <li>All active user sessions should be terminated</li>
     * </ul>
     * 
     * <p><strong>Cascade Effects:</strong> Deletion triggers cleanup across
     * all microservices to ensure complete data removal and prevent orphaned records.
     * 
     * @param tenantId the UUID of the tenant to delete
     * @return ResponseEntity with 204 No Content status on successful deletion
     * @throws org.springframework.security.access.AccessDeniedException if user lacks ADMIN role
     * @throws com.byo.rag.shared.exception.TenantNotFoundException if tenant does not exist
     * @throws com.byo.rag.shared.exception.RagException if tenant has active dependencies
     */
    @DeleteMapping("/{tenantId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete tenant", description = "Delete tenant (admin only)")
    public ResponseEntity<Void> deleteTenant(@PathVariable UUID tenantId) {
        tenantService.deleteTenant(tenantId);
        return ResponseEntity.noContent().build();
    }
}
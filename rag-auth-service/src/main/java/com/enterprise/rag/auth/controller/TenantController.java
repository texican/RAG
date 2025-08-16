package com.enterprise.rag.auth.controller;

import com.enterprise.rag.auth.security.JwtAuthenticationFilter;
import com.enterprise.rag.auth.service.TenantService;
import com.enterprise.rag.shared.dto.TenantDto;
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

@RestController
@RequestMapping("/api/v1/tenants")
@Tag(name = "Tenant Management", description = "Tenant CRUD operations")
@SecurityRequirement(name = "bearerAuth")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register new tenant", description = "Register a new tenant (public endpoint)")
    public ResponseEntity<TenantDto.TenantResponse> registerTenant(@Valid @RequestBody TenantDto.CreateTenantRequest request) {
        TenantDto.TenantResponse response = tenantService.createTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create tenant", description = "Create a new tenant (admin only)")
    public ResponseEntity<TenantDto.TenantResponse> createTenant(@Valid @RequestBody TenantDto.CreateTenantRequest request) {
        TenantDto.TenantResponse response = tenantService.createTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

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

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get tenant by slug", description = "Retrieve tenant information by slug")
    public ResponseEntity<TenantDto.TenantResponse> getTenantBySlug(@PathVariable String slug) {
        TenantDto.TenantResponse response = tenantService.getTenantBySlug(slug);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all tenants", description = "Retrieve all tenants (admin only)")
    public ResponseEntity<Page<TenantDto.TenantSummary>> getAllTenants(Pageable pageable) {
        Page<TenantDto.TenantSummary> response = tenantService.getAllTenants(pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{tenantId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update tenant", description = "Update tenant information (admin only)")
    public ResponseEntity<TenantDto.TenantResponse> updateTenant(
            @PathVariable UUID tenantId,
            @Valid @RequestBody TenantDto.UpdateTenantRequest request) {
        TenantDto.TenantResponse response = tenantService.updateTenant(tenantId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{tenantId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete tenant", description = "Delete tenant (admin only)")
    public ResponseEntity<Void> deleteTenant(@PathVariable UUID tenantId) {
        tenantService.deleteTenant(tenantId);
        return ResponseEntity.noContent().build();
    }
}
package com.enterprise.rag.admin.service;

import com.enterprise.rag.admin.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TenantServiceImpl Tests")
class TenantServiceImplTest {

    private TenantServiceImpl tenantService;

    @BeforeEach
    void setUp() {
        tenantService = new TenantServiceImpl();
    }

    @Test
    @DisplayName("Should create tenant with valid request")
    void shouldCreateTenantWithValidRequest() {
        // Given
        TenantCreateRequest request = new TenantCreateRequest(
                "Test Tenant", 
                "admin@testtenant.com", 
                "A test tenant for enterprise RAG"
        );

        // When
        TenantResponse response = tenantService.createTenant(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.tenantId());
        assertEquals("Test Tenant", response.name());
        assertEquals("admin@testtenant.com", response.adminEmail());
        assertEquals("A test tenant for enterprise RAG", response.description());
        assertEquals("ACTIVE", response.status());
        assertTrue(response.isActive());
        assertEquals(0, response.totalUsers());
        assertEquals(0, response.totalDocuments());
        assertNotNull(response.createdAt());
        assertNotNull(response.updatedAt());
    }

    @Test
    @DisplayName("Should create tenant with generated ID")
    void shouldCreateTenantWithGeneratedId() {
        // Given
        TenantCreateRequest request1 = new TenantCreateRequest("Tenant 1", "admin1@test.com", "Description 1");
        TenantCreateRequest request2 = new TenantCreateRequest("Tenant 2", "admin2@test.com", "Description 2");

        // When
        TenantResponse response1 = tenantService.createTenant(request1);
        TenantResponse response2 = tenantService.createTenant(request2);

        // Then
        assertNotEquals(response1.tenantId(), response2.tenantId());
        assertTrue(response1.tenantId().startsWith("tenant-"));
        assertTrue(response2.tenantId().startsWith("tenant-"));
    }

    @Test
    @DisplayName("Should get tenant by ID successfully")
    void shouldGetTenantByIdSuccessfully() {
        // Given
        TenantCreateRequest request = new TenantCreateRequest("Test Tenant", "admin@test.com", "Description");
        TenantResponse created = tenantService.createTenant(request);

        // When
        TenantResponse retrieved = tenantService.getTenantById(created.tenantId());

        // Then
        assertEquals(created.tenantId(), retrieved.tenantId());
        assertEquals(created.name(), retrieved.name());
        assertEquals(created.adminEmail(), retrieved.adminEmail());
        assertEquals(created.description(), retrieved.description());
    }

    @Test
    @DisplayName("Should throw exception when tenant not found")
    void shouldThrowExceptionWhenTenantNotFound() {
        // Given
        String nonExistentTenantId = "non-existent-tenant";

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> tenantService.getTenantById(nonExistentTenantId));
        assertEquals("Tenant not found with ID: non-existent-tenant", exception.getMessage());
    }

    @Test
    @DisplayName("Should update tenant successfully")
    void shouldUpdateTenantSuccessfully() {
        // Given
        TenantCreateRequest createRequest = new TenantCreateRequest("Original Name", "original@test.com", "Original Description");
        TenantResponse created = tenantService.createTenant(createRequest);

        TenantUpdateRequest updateRequest = new TenantUpdateRequest(
                "Updated Name", 
                "updated@test.com", 
                "Updated Description"
        );

        // When
        TenantResponse updated = tenantService.updateTenant(created.tenantId(), updateRequest);

        // Then
        assertEquals(created.tenantId(), updated.tenantId());
        assertEquals("Updated Name", updated.name());
        assertEquals("updated@test.com", updated.adminEmail());
        assertEquals("Updated Description", updated.description());
        assertEquals(created.createdAt(), updated.createdAt()); // Created time shouldn't change
        assertTrue(updated.updatedAt().isAfter(created.updatedAt())); // Updated time should be newer
    }

    @Test
    @DisplayName("Should update tenant with partial data")
    void shouldUpdateTenantWithPartialData() {
        // Given
        TenantCreateRequest createRequest = new TenantCreateRequest("Original Name", "original@test.com", "Original Description");
        TenantResponse created = tenantService.createTenant(createRequest);

        TenantUpdateRequest updateRequest = new TenantUpdateRequest(
                "Updated Name", 
                null, // Don't update email
                null  // Don't update description
        );

        // When
        TenantResponse updated = tenantService.updateTenant(created.tenantId(), updateRequest);

        // Then
        assertEquals("Updated Name", updated.name());
        assertEquals("original@test.com", updated.adminEmail()); // Should remain unchanged
        assertEquals("Original Description", updated.description()); // Should remain unchanged
    }

    @Test
    @DisplayName("Should suspend tenant successfully")
    void shouldSuspendTenantSuccessfully() {
        // Given
        TenantCreateRequest createRequest = new TenantCreateRequest("Test Tenant", "admin@test.com", "Description");
        TenantResponse created = tenantService.createTenant(createRequest);

        TenantSuspendRequest suspendRequest = new TenantSuspendRequest(
                "Tenant violated terms of service by uploading inappropriate content"
        );

        // When
        TenantResponse suspended = tenantService.suspendTenant(created.tenantId(), suspendRequest);

        // Then
        assertEquals(created.tenantId(), suspended.tenantId());
        assertEquals("SUSPENDED", suspended.status());
        assertFalse(suspended.isActive());
        assertTrue(suspended.updatedAt().isAfter(created.updatedAt()));
    }

    @Test
    @DisplayName("Should reactivate tenant successfully")
    void shouldReactivateTenantSuccessfully() {
        // Given
        TenantCreateRequest createRequest = new TenantCreateRequest("Test Tenant", "admin@test.com", "Description");
        TenantResponse created = tenantService.createTenant(createRequest);

        TenantSuspendRequest suspendRequest = new TenantSuspendRequest("Suspension reason");
        TenantResponse suspended = tenantService.suspendTenant(created.tenantId(), suspendRequest);

        // When
        TenantResponse reactivated = tenantService.reactivateTenant(suspended.tenantId());

        // Then
        assertEquals(suspended.tenantId(), reactivated.tenantId());
        assertEquals("ACTIVE", reactivated.status());
        assertTrue(reactivated.isActive());
        assertTrue(reactivated.updatedAt().isAfter(suspended.updatedAt()));
    }

    @Test
    @DisplayName("Should get all tenants with pagination")
    void shouldGetAllTenantsWithPagination() {
        // Given
        // Create multiple tenants
        for (int i = 1; i <= 15; i++) {
            TenantCreateRequest request = new TenantCreateRequest(
                    "Tenant " + i, 
                    "admin" + i + "@test.com", 
                    "Description " + i
            );
            tenantService.createTenant(request);
        }

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "name"));

        // When
        TenantListResponse response = tenantService.getAllTenants(pageRequest);

        // Then
        assertEquals(10, response.tenants().size()); // First page should have 10 tenants
        assertEquals(15, response.totalCount()); // Total should be 15
        assertEquals(0, response.page()); // Current page
        assertEquals(10, response.size()); // Page size
        assertEquals(2, response.totalPages()); // Total pages (15/10 = 2)
        
        // Verify sorting
        assertEquals("Tenant 1", response.tenants().get(0).name());
        assertEquals("Tenant 10", response.tenants().get(1).name());
    }

    @Test
    @DisplayName("Should get second page of tenants")
    void shouldGetSecondPageOfTenants() {
        // Given
        // Create multiple tenants
        for (int i = 1; i <= 15; i++) {
            TenantCreateRequest request = new TenantCreateRequest(
                    "Tenant " + i, 
                    "admin" + i + "@test.com", 
                    "Description " + i
            );
            tenantService.createTenant(request);
        }

        PageRequest pageRequest = PageRequest.of(1, 10, Sort.by(Sort.Direction.ASC, "name"));

        // When
        TenantListResponse response = tenantService.getAllTenants(pageRequest);

        // Then
        assertEquals(5, response.tenants().size()); // Second page should have 5 remaining tenants
        assertEquals(15, response.totalCount());
        assertEquals(1, response.page());
        assertEquals(10, response.size());
        assertEquals(2, response.totalPages());
    }

    @Test
    @DisplayName("Should delete tenant successfully")
    void shouldDeleteTenantSuccessfully() {
        // Given
        TenantCreateRequest createRequest = new TenantCreateRequest("Test Tenant", "admin@test.com", "Description");
        TenantResponse created = tenantService.createTenant(createRequest);

        // When
        tenantService.deleteTenant(created.tenantId());

        // Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> tenantService.getTenantById(created.tenantId()));
        assertEquals("Tenant not found with ID: " + created.tenantId(), exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when trying to delete non-existent tenant")
    void shouldThrowExceptionWhenTryingToDeleteNonExistentTenant() {
        // Given
        String nonExistentTenantId = "non-existent-tenant";

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> tenantService.deleteTenant(nonExistentTenantId));
        assertEquals("Tenant not found with ID: non-existent-tenant", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when trying to update non-existent tenant")
    void shouldThrowExceptionWhenTryingToUpdateNonExistentTenant() {
        // Given
        String nonExistentTenantId = "non-existent-tenant";
        TenantUpdateRequest updateRequest = new TenantUpdateRequest("New Name", "new@test.com", "New Description");

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> tenantService.updateTenant(nonExistentTenantId, updateRequest));
        assertEquals("Tenant not found with ID: non-existent-tenant", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when trying to suspend non-existent tenant")
    void shouldThrowExceptionWhenTryingToSuspendNonExistentTenant() {
        // Given
        String nonExistentTenantId = "non-existent-tenant";
        TenantSuspendRequest suspendRequest = new TenantSuspendRequest("Some suspension reason");

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> tenantService.suspendTenant(nonExistentTenantId, suspendRequest));
        assertEquals("Tenant not found with ID: non-existent-tenant", exception.getMessage());
    }
}
package com.enterprise.rag.admin.controller;

import com.enterprise.rag.admin.dto.*;
import com.enterprise.rag.admin.service.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantManagementController Tests")
class TenantManagementControllerTest {

    @Mock
    private TenantService tenantService;

    @InjectMocks
    private TenantManagementController controller;

    private TenantCreateRequest validCreateRequest;
    private TenantUpdateRequest validUpdateRequest;
    private TenantSuspendRequest validSuspendRequest;
    private TenantResponse mockTenantResponse;
    private TenantListResponse mockListResponse;

    @BeforeEach
    void setUp() {
        reset(tenantService);

        validCreateRequest = new TenantCreateRequest(
                "Test Tenant",
                "test@enterprise.com",
                "Test description"
        );

        validUpdateRequest = new TenantUpdateRequest(
                "Updated Tenant",
                "updated@enterprise.com",
                "Updated description"
        );

        validSuspendRequest = new TenantSuspendRequest(
                "Policy violation detected in the system"
        );

        mockTenantResponse = new TenantResponse(
                "tenant-123",
                "Test Tenant",
                "test@enterprise.com",
                "Test description",
                "ACTIVE",
                LocalDateTime.now(),
                LocalDateTime.now(),
                0,
                0,
                true
        );

        mockListResponse = new TenantListResponse(
                List.of(mockTenantResponse),
                1,
                0,
                10,
                1
        );
    }

    @Test
    @DisplayName("Should create tenant successfully with valid data")
    void shouldCreateTenantSuccessfullyWithValidData() {
        // Given
        when(tenantService.createTenant(validCreateRequest)).thenReturn(mockTenantResponse);

        // When
        ResponseEntity<TenantResponse> response = controller.createTenant(validCreateRequest);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(mockTenantResponse, response.getBody());
        verify(tenantService).createTenant(validCreateRequest);
    }

    @Test
    @DisplayName("Should get tenant by ID successfully")
    void shouldGetTenantByIdSuccessfully() {
        // Given
        String tenantId = "tenant-123";
        when(tenantService.getTenantById(tenantId)).thenReturn(mockTenantResponse);

        // When
        ResponseEntity<TenantResponse> response = controller.getTenant(tenantId);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockTenantResponse, response.getBody());
        verify(tenantService).getTenantById(tenantId);
    }

    @Test
    @DisplayName("Should update tenant successfully")
    void shouldUpdateTenantSuccessfully() {
        // Given
        String tenantId = "tenant-123";
        TenantResponse updatedResponse = new TenantResponse(
                tenantId,
                "Updated Tenant",
                "updated@enterprise.com",
                "Updated description",
                "ACTIVE",
                LocalDateTime.now(),
                LocalDateTime.now(),
                0,
                0,
                true
        );
        when(tenantService.updateTenant(tenantId, validUpdateRequest)).thenReturn(updatedResponse);

        // When
        ResponseEntity<TenantResponse> response = controller.updateTenant(tenantId, validUpdateRequest);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(updatedResponse, response.getBody());
        verify(tenantService).updateTenant(tenantId, validUpdateRequest);
    }

    @Test
    @DisplayName("Should list all tenants with default pagination")
    void shouldListAllTenantsWithDefaultPagination() {
        // Given
        when(tenantService.getAllTenants(any(PageRequest.class))).thenReturn(mockListResponse);

        // When
        ResponseEntity<TenantListResponse> response = controller.getAllTenants(0, 10, "createdAt,desc");

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockListResponse, response.getBody());
        verify(tenantService).getAllTenants(any(PageRequest.class));
    }

    @Test
    @DisplayName("Should list tenants with custom pagination")
    void shouldListTenantsWithCustomPagination() {
        // Given
        int page = 1;
        int size = 5;
        String sort = "name,asc";
        when(tenantService.getAllTenants(any(PageRequest.class))).thenReturn(mockListResponse);

        // When
        ResponseEntity<TenantListResponse> response = controller.getAllTenants(page, size, sort);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockListResponse, response.getBody());
        verify(tenantService).getAllTenants(any(PageRequest.class));
    }

    @Test
    @DisplayName("Should suspend tenant successfully")
    void shouldSuspendTenantSuccessfully() {
        // Given
        String tenantId = "tenant-123";
        TenantResponse suspendedResponse = new TenantResponse(
                tenantId,
                "Test Tenant",
                "test@enterprise.com",
                "Test description",
                "SUSPENDED",
                LocalDateTime.now(),
                LocalDateTime.now(),
                0,
                0,
                false
        );
        when(tenantService.suspendTenant(tenantId, validSuspendRequest)).thenReturn(suspendedResponse);

        // When
        ResponseEntity<TenantResponse> response = controller.suspendTenant(tenantId, validSuspendRequest);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(suspendedResponse, response.getBody());
        verify(tenantService).suspendTenant(tenantId, validSuspendRequest);
    }

    @Test
    @DisplayName("Should reactivate tenant successfully")
    void shouldReactivateTenantSuccessfully() {
        // Given
        String tenantId = "tenant-123";
        TenantResponse reactivatedResponse = new TenantResponse(
                tenantId,
                "Test Tenant",
                "test@enterprise.com",
                "Test description",
                "ACTIVE",
                LocalDateTime.now(),
                LocalDateTime.now(),
                0,
                0,
                true
        );
        when(tenantService.reactivateTenant(tenantId)).thenReturn(reactivatedResponse);

        // When
        ResponseEntity<TenantResponse> response = controller.reactivateTenant(tenantId);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(reactivatedResponse, response.getBody());
        verify(tenantService).reactivateTenant(tenantId);
    }

    @Test
    @DisplayName("Should delete tenant successfully")
    void shouldDeleteTenantSuccessfully() {
        // Given
        String tenantId = "tenant-123";
        doNothing().when(tenantService).deleteTenant(tenantId);

        // When
        ResponseEntity<Void> response = controller.deleteTenant(tenantId);

        // Then
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(tenantService).deleteTenant(tenantId);
    }

    @Test
    @DisplayName("Should handle create request with null service response")
    void shouldHandleCreateRequestWithNullServiceResponse() {
        // Given
        when(tenantService.createTenant(any(TenantCreateRequest.class))).thenReturn(null);

        // When
        ResponseEntity<TenantResponse> response = controller.createTenant(validCreateRequest);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNull(response.getBody());
        verify(tenantService).createTenant(validCreateRequest);
    }

    @Test
    @DisplayName("Should handle get request with null service response")
    void shouldHandleGetRequestWithNullServiceResponse() {
        // Given
        String tenantId = "tenant-123";
        when(tenantService.getTenantById(tenantId)).thenReturn(null);

        // When
        ResponseEntity<TenantResponse> response = controller.getTenant(tenantId);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());
        verify(tenantService).getTenantById(tenantId);
    }

    @Test
    @DisplayName("Should handle update request with null service response")
    void shouldHandleUpdateRequestWithNullServiceResponse() {
        // Given
        String tenantId = "tenant-123";
        when(tenantService.updateTenant(tenantId, validUpdateRequest)).thenReturn(null);

        // When
        ResponseEntity<TenantResponse> response = controller.updateTenant(tenantId, validUpdateRequest);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());
        verify(tenantService).updateTenant(tenantId, validUpdateRequest);
    }

    @Test
    @DisplayName("Should handle empty list response")
    void shouldHandleEmptyListResponse() {
        // Given
        TenantListResponse emptyResponse = new TenantListResponse(
                List.of(),
                0,
                0,
                10,
                0
        );
        when(tenantService.getAllTenants(any(PageRequest.class))).thenReturn(emptyResponse);

        // When
        ResponseEntity<TenantListResponse> response = controller.getAllTenants(0, 10, "createdAt,desc");

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(emptyResponse, response.getBody());
        assertTrue(response.getBody().tenants().isEmpty());
    }
}
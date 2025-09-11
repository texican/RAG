package com.byo.rag.admin.controller;

import com.byo.rag.admin.dto.*;
import com.byo.rag.admin.service.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
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
        assertThat(response.getStatusCode())
            .describedAs("Tenant creation should return HTTP 201 Created for valid request data")
            .isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody())
            .describedAs("Response body should contain the created tenant data")
            .isEqualTo(mockTenantResponse);
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
        assertThat(response.getStatusCode())
            .describedAs("Tenant retrieval should return HTTP 200 OK for existing tenant")
            .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
            .describedAs("Response body should contain the retrieved tenant data")
            .isEqualTo(mockTenantResponse);
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
        assertThat(response.getStatusCode())
            .describedAs("Tenant update should return HTTP 200 OK for valid update data")
            .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
            .describedAs("Response body should contain the updated tenant data")
            .isEqualTo(updatedResponse);
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
        assertThat(response.getStatusCode())
            .describedAs("Tenant listing should return HTTP 200 OK with default pagination")
            .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
            .describedAs("Response body should contain paginated tenant list")
            .isEqualTo(mockListResponse);
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
        assertThat(response.getStatusCode())
            .describedAs("Tenant listing should return HTTP 200 OK with custom pagination")
            .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
            .describedAs("Response body should contain paginated tenant list with custom parameters")
            .isEqualTo(mockListResponse);
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
        assertThat(response.getStatusCode())
            .describedAs("Tenant suspension should return HTTP 200 OK for valid suspension request")
            .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
            .describedAs("Response body should contain suspended tenant data with SUSPENDED status")
            .isEqualTo(suspendedResponse);
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
        assertThat(response.getStatusCode())
            .describedAs("Tenant reactivation should return HTTP 200 OK for valid reactivation")
            .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
            .describedAs("Response body should contain reactivated tenant data with ACTIVE status")
            .isEqualTo(reactivatedResponse);
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
        assertThat(response.getStatusCode())
            .describedAs("Tenant deletion should return HTTP 204 No Content for successful deletion")
            .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody())
            .describedAs("Response body should be null for successful deletion")
            .isNull();
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
        assertThat(response.getStatusCode())
            .describedAs("Create should return HTTP 201 Created even with null service response")
            .isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody())
            .describedAs("Response body should be null when service returns null")
            .isNull();
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
        assertThat(response.getStatusCode())
            .describedAs("Get should return HTTP 200 OK even with null service response")
            .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
            .describedAs("Response body should be null when service returns null")
            .isNull();
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
        assertThat(response.getStatusCode())
            .describedAs("Update should return HTTP 200 OK even with null service response")
            .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
            .describedAs("Response body should be null when service returns null")
            .isNull();
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
        assertThat(response.getStatusCode())
            .describedAs("Empty list should return HTTP 200 OK with empty results")
            .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
            .describedAs("Response body should contain empty list response")
            .isEqualTo(emptyResponse);
        TenantListResponse responseBody = response.getBody();
        assertThat(responseBody)
            .describedAs("Response body should not be null even for empty list")
            .isNotNull();
        assertThat(responseBody.tenants())
            .describedAs("Tenant list should be empty when no tenants exist")
            .isEmpty();
    }
}
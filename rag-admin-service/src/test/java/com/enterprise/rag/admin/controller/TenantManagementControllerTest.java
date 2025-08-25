package com.enterprise.rag.admin.controller;

import com.enterprise.rag.admin.config.TestSecurityConfig;
import com.enterprise.rag.admin.dto.*;
import com.enterprise.rag.admin.service.AdminJwtService;
import com.enterprise.rag.admin.service.TenantService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TenantManagementController.class)
@Import(TestSecurityConfig.class)
@DisplayName("TenantManagementController Tests")
class TenantManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TenantService tenantService;

    @MockBean
    private AdminJwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private final String validToken = "valid.jwt.token";
    private final String tenantId = "tenant-123";

    @BeforeEach
    void setUp() {
        reset(tenantService, jwtService);
    }

    @Test
    @DisplayName("Should create tenant successfully with valid data")
    void shouldCreateTenantSuccessfullyWithValidData() throws Exception {
        // Given
        TenantCreateRequest request = new TenantCreateRequest(
                "Test Tenant", 
                "admin@testtenant.com", 
                "A test tenant for enterprise RAG"
        );

        TenantResponse response = new TenantResponse(
                "tenant-123",
                "Test Tenant",
                "admin@testtenant.com",
                "A test tenant for enterprise RAG",
                "ACTIVE",
                LocalDateTime.now(),
                LocalDateTime.now(),
                0,
                0,
                true
        );

        when(tenantService.createTenant(any(TenantCreateRequest.class))).thenReturn(response);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/admin/api/tenants")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value("tenant-123"))
                .andExpect(jsonPath("$.name").value("Test Tenant"))
                .andExpect(jsonPath("$.adminEmail").value("admin@testtenant.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.isActive").value(true));

        verify(tenantService).createTenant(any(TenantCreateRequest.class));
    }

    @Test
    @DisplayName("Should return 400 with invalid tenant name")
    void shouldReturn400WithInvalidTenantName() throws Exception {
        // Given
        TenantCreateRequest request = new TenantCreateRequest(
                "A", // Too short
                "admin@testtenant.com", 
                "A test tenant"
        );

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/admin/api/tenants")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.message").exists());

        verifyNoInteractions(tenantService);
    }

    @Test
    @DisplayName("Should return 400 with invalid email")
    void shouldReturn400WithInvalidEmail() throws Exception {
        // Given
        TenantCreateRequest request = new TenantCreateRequest(
                "Test Tenant", 
                "invalid-email", // Invalid email format
                "A test tenant"
        );

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/admin/api/tenants")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.message").exists());

        verifyNoInteractions(tenantService);
    }

    @Test
    @DisplayName("Should get tenant by ID successfully")
    void shouldGetTenantByIdSuccessfully() throws Exception {
        // Given
        TenantResponse response = new TenantResponse(
                tenantId,
                "Test Tenant",
                "admin@testtenant.com",
                "A test tenant",
                "ACTIVE",
                LocalDateTime.now(),
                LocalDateTime.now(),
                5,
                100,
                true
        );

        when(tenantService.getTenantById(tenantId)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/admin/api/tenants/{tenantId}", tenantId)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value(tenantId))
                .andExpect(jsonPath("$.name").value("Test Tenant"))
                .andExpect(jsonPath("$.totalUsers").value(5))
                .andExpect(jsonPath("$.totalDocuments").value(100));

        verify(tenantService).getTenantById(tenantId);
    }

    @Test
    @DisplayName("Should return 404 when tenant not found")
    void shouldReturn404WhenTenantNotFound() throws Exception {
        // Given
        String nonExistentTenantId = "non-existent-tenant";
        when(tenantService.getTenantById(nonExistentTenantId))
                .thenThrow(new RuntimeException("Tenant not found"));

        // When & Then
        mockMvc.perform(get("/admin/api/tenants/{tenantId}", nonExistentTenantId)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Tenant not found"))
                .andExpect(jsonPath("$.message").exists());

        verify(tenantService).getTenantById(nonExistentTenantId);
    }

    @Test
    @DisplayName("Should update tenant successfully")
    void shouldUpdateTenantSuccessfully() throws Exception {
        // Given
        TenantUpdateRequest request = new TenantUpdateRequest(
                "Updated Tenant Name",
                "newemail@testtenant.com",
                "Updated description"
        );

        TenantResponse response = new TenantResponse(
                tenantId,
                "Updated Tenant Name",
                "newemail@testtenant.com",
                "Updated description",
                "ACTIVE",
                LocalDateTime.now(),
                LocalDateTime.now(),
                5,
                100,
                true
        );

        when(tenantService.updateTenant(eq(tenantId), any(TenantUpdateRequest.class)))
                .thenReturn(response);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(put("/admin/api/tenants/{tenantId}", tenantId)
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value(tenantId))
                .andExpect(jsonPath("$.name").value("Updated Tenant Name"))
                .andExpect(jsonPath("$.adminEmail").value("newemail@testtenant.com"));

        verify(tenantService).updateTenant(eq(tenantId), any(TenantUpdateRequest.class));
    }

    @Test
    @DisplayName("Should suspend tenant successfully")
    void shouldSuspendTenantSuccessfully() throws Exception {
        // Given
        TenantSuspendRequest request = new TenantSuspendRequest(
                "Tenant violated terms of service by uploading inappropriate content"
        );

        TenantResponse response = new TenantResponse(
                tenantId,
                "Test Tenant",
                "admin@testtenant.com",
                "A test tenant",
                "SUSPENDED",
                LocalDateTime.now(),
                LocalDateTime.now(),
                5,
                100,
                false
        );

        when(tenantService.suspendTenant(eq(tenantId), any(TenantSuspendRequest.class)))
                .thenReturn(response);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/admin/api/tenants/{tenantId}/suspend", tenantId)
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value(tenantId))
                .andExpect(jsonPath("$.status").value("SUSPENDED"))
                .andExpect(jsonPath("$.isActive").value(false));

        verify(tenantService).suspendTenant(eq(tenantId), any(TenantSuspendRequest.class));
    }

    @Test
    @DisplayName("Should return 400 with invalid suspend reason")
    void shouldReturn400WithInvalidSuspendReason() throws Exception {
        // Given
        TenantSuspendRequest request = new TenantSuspendRequest("Too short"); // Too short

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/admin/api/tenants/{tenantId}/suspend", tenantId)
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.message").exists());

        verifyNoInteractions(tenantService);
    }

    @Test
    @DisplayName("Should reactivate tenant successfully")
    void shouldReactivateTenantSuccessfully() throws Exception {
        // Given
        TenantResponse response = new TenantResponse(
                tenantId,
                "Test Tenant",
                "admin@testtenant.com",
                "A test tenant",
                "ACTIVE",
                LocalDateTime.now(),
                LocalDateTime.now(),
                5,
                100,
                true
        );

        when(tenantService.reactivateTenant(tenantId)).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/admin/api/tenants/{tenantId}/reactivate", tenantId)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value(tenantId))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.isActive").value(true));

        verify(tenantService).reactivateTenant(tenantId);
    }

    @Test
    @DisplayName("Should list all tenants with default pagination")
    void shouldListAllTenantsWithDefaultPagination() throws Exception {
        // Given
        List<TenantResponse> tenants = List.of(
                new TenantResponse("tenant-1", "Tenant 1", "admin1@example.com", "Description 1", 
                        "ACTIVE", LocalDateTime.now(), LocalDateTime.now(), 10, 50, true),
                new TenantResponse("tenant-2", "Tenant 2", "admin2@example.com", "Description 2", 
                        "ACTIVE", LocalDateTime.now(), LocalDateTime.now(), 5, 25, true)
        );

        TenantListResponse listResponse = new TenantListResponse(tenants, 2, 0, 10, 1);

        when(tenantService.getAllTenants(any(PageRequest.class))).thenReturn(listResponse);

        // When & Then
        mockMvc.perform(get("/admin/api/tenants")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenants").isArray())
                .andExpect(jsonPath("$.tenants").value(hasSize(2)))
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));

        verify(tenantService).getAllTenants(any(PageRequest.class));
    }

    @Test
    @DisplayName("Should list tenants with custom pagination")
    void shouldListTenantsWithCustomPagination() throws Exception {
        // Given
        List<TenantResponse> tenants = List.of(
                new TenantResponse("tenant-3", "Tenant 3", "admin3@example.com", "Description 3", 
                        "ACTIVE", LocalDateTime.now(), LocalDateTime.now(), 8, 40, true)
        );

        TenantListResponse listResponse = new TenantListResponse(tenants, 11, 2, 5, 3);

        when(tenantService.getAllTenants(any(PageRequest.class))).thenReturn(listResponse);

        // When & Then
        mockMvc.perform(get("/admin/api/tenants")
                        .header("Authorization", "Bearer " + validToken)
                        .param("page", "2")
                        .param("size", "5")
                        .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenants").isArray())
                .andExpect(jsonPath("$.tenants").value(hasSize(1)))
                .andExpect(jsonPath("$.totalCount").value(11))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));

        verify(tenantService).getAllTenants(any(PageRequest.class));
    }

    @Test
    @DisplayName("Should delete tenant successfully")
    void shouldDeleteTenantSuccessfully() throws Exception {
        // Given
        doNothing().when(tenantService).deleteTenant(tenantId);

        // When & Then
        mockMvc.perform(delete("/admin/api/tenants/{tenantId}", tenantId)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNoContent());

        verify(tenantService).deleteTenant(tenantId);
    }

    @Test
    @DisplayName("Should return 401 for unauthenticated requests")
    void shouldReturn401ForUnauthenticatedRequests() throws Exception {
        // When & Then
        mockMvc.perform(get("/admin/api/tenants"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(tenantService);
    }

    // Helper method for hasSize matcher
    private org.hamcrest.Matcher<java.util.Collection<? extends Object>> hasSize(int size) {
        return Matchers.hasSize(size);
    }
}
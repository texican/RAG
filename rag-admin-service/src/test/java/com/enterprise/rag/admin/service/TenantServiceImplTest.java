package com.enterprise.rag.admin.service;

import com.enterprise.rag.admin.dto.*;
import com.enterprise.rag.admin.repository.TenantRepository;
import com.enterprise.rag.admin.repository.UserRepository;
import com.enterprise.rag.shared.entity.Tenant;
import com.enterprise.rag.shared.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantServiceImpl Tests")
class TenantServiceImplTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private TenantServiceImpl tenantService;

    private Tenant sampleTenant;
    private User sampleAdminUser;
    private UUID sampleTenantId;

    @BeforeEach
    void setUp() {
        sampleTenantId = UUID.randomUUID();
        sampleTenant = createSampleTenant();
        sampleAdminUser = createSampleAdminUser();
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

        when(tenantRepository.existsByName(anyString())).thenReturn(false);
        when(tenantRepository.existsBySlug(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> {
            Tenant tenant = invocation.getArgument(0);
            tenant.setId(sampleTenantId); // Set ID as if saved to database
            return tenant;
        });
        when(userRepository.save(any(User.class))).thenReturn(sampleAdminUser);
        when(userRepository.countByTenantId(any(UUID.class))).thenReturn(1L);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");

        // When
        TenantResponse response = tenantService.createTenant(request);

        // Then
        assertNotNull(response);
        assertEquals(sampleTenantId.toString(), response.tenantId());
        assertEquals(request.name(), response.name());
        assertEquals(request.adminEmail(), response.adminEmail());
        assertEquals(request.description(), response.description());
        assertEquals("ACTIVE", response.status());
        assertTrue(response.isActive());

        verify(tenantRepository).existsByName(request.name());
        verify(tenantRepository).existsBySlug(anyString());
        verify(userRepository).existsByEmail(request.adminEmail());
        verify(tenantRepository).save(any(Tenant.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when tenant name already exists")
    void shouldThrowExceptionWhenTenantNameAlreadyExists() {
        // Given
        TenantCreateRequest request = new TenantCreateRequest(
                "Existing Tenant",
                "admin@existing.com",
                "Description"
        );
        when(tenantRepository.existsByName(request.name())).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tenantService.createTenant(request)
        );
        assertEquals("Tenant with name 'Existing Tenant' already exists", exception.getMessage());

        verify(tenantRepository).existsByName(request.name());
        verify(tenantRepository, never()).save(any(Tenant.class));
    }

    @Test
    @DisplayName("Should throw exception when admin email already exists")
    void shouldThrowExceptionWhenAdminEmailAlreadyExists() {
        // Given
        TenantCreateRequest request = new TenantCreateRequest(
                "New Tenant",
                "existing@admin.com",
                "Description"
        );
        when(tenantRepository.existsByName(anyString())).thenReturn(false);
        when(tenantRepository.existsBySlug(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(request.adminEmail())).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tenantService.createTenant(request)
        );
        assertEquals("User with email 'existing@admin.com' already exists", exception.getMessage());

        verify(userRepository).existsByEmail(request.adminEmail());
        verify(tenantRepository, never()).save(any(Tenant.class));
    }

    @Test
    @DisplayName("Should get tenant by ID successfully")
    void shouldGetTenantByIdSuccessfully() {
        // Given
        String tenantId = sampleTenantId.toString();
        when(tenantRepository.findById(sampleTenantId)).thenReturn(Optional.of(sampleTenant));
        when(userRepository.findTenantAdministrators(sampleTenantId)).thenReturn(Arrays.asList(sampleAdminUser));
        when(userRepository.countByTenantId(sampleTenantId)).thenReturn(1L);

        // When
        TenantResponse response = tenantService.getTenantById(tenantId);

        // Then
        assertNotNull(response);
        assertEquals(tenantId, response.tenantId());
        assertEquals(sampleTenant.getName(), response.name());
        assertEquals(sampleAdminUser.getEmail(), response.adminEmail());

        verify(tenantRepository).findById(sampleTenantId);
        verify(userRepository).findTenantAdministrators(sampleTenantId);
    }

    @Test
    @DisplayName("Should throw exception when tenant not found")
    void shouldThrowExceptionWhenTenantNotFound() {
        // Given
        String tenantId = UUID.randomUUID().toString();
        when(tenantRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> tenantService.getTenantById(tenantId)
        );
        assertTrue(exception.getMessage().contains("Tenant not found"));

        verify(tenantRepository).findById(any(UUID.class));
    }

    @Test
    @DisplayName("Should update tenant successfully")
    void shouldUpdateTenantSuccessfully() {
        // Given
        String tenantId = sampleTenantId.toString();
        TenantUpdateRequest request = new TenantUpdateRequest(
                "Updated Tenant Name",
                null, // Keep existing admin email
                "Updated description"
        );

        when(tenantRepository.findById(sampleTenantId)).thenReturn(Optional.of(sampleTenant));
        when(tenantRepository.existsByName(request.name())).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(sampleTenant);
        when(userRepository.findTenantAdministrators(sampleTenantId)).thenReturn(Arrays.asList(sampleAdminUser));
        when(userRepository.countByTenantId(sampleTenantId)).thenReturn(1L);

        // When
        TenantResponse response = tenantService.updateTenant(tenantId, request);

        // Then
        assertNotNull(response);
        assertEquals(tenantId, response.tenantId());

        verify(tenantRepository).findById(sampleTenantId);
        verify(tenantRepository).existsByName(request.name());
        verify(tenantRepository).save(any(Tenant.class));
    }

    @Test
    @DisplayName("Should suspend tenant successfully")
    void shouldSuspendTenantSuccessfully() {
        // Given
        String tenantId = sampleTenantId.toString();
        TenantSuspendRequest request = new TenantSuspendRequest("Policy violation");

        when(tenantRepository.findById(sampleTenantId)).thenReturn(Optional.of(sampleTenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(sampleTenant);
        when(userRepository.findTenantAdministrators(sampleTenantId)).thenReturn(Arrays.asList(sampleAdminUser));
        when(userRepository.countByTenantId(sampleTenantId)).thenReturn(1L);

        // When
        TenantResponse response = tenantService.suspendTenant(tenantId, request);

        // Then
        assertNotNull(response);
        assertEquals(tenantId, response.tenantId());

        verify(tenantRepository).findById(sampleTenantId);
        verify(tenantRepository).save(argThat(tenant -> 
                tenant.getStatus() == Tenant.TenantStatus.SUSPENDED));
    }

    @Test
    @DisplayName("Should reactivate tenant successfully")
    void shouldReactivateTenantSuccessfully() {
        // Given
        String tenantId = sampleTenantId.toString();
        sampleTenant.setStatus(Tenant.TenantStatus.SUSPENDED);

        when(tenantRepository.findById(sampleTenantId)).thenReturn(Optional.of(sampleTenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(sampleTenant);
        when(userRepository.findTenantAdministrators(sampleTenantId)).thenReturn(Arrays.asList(sampleAdminUser));
        when(userRepository.countByTenantId(sampleTenantId)).thenReturn(1L);

        // When
        TenantResponse response = tenantService.reactivateTenant(tenantId);

        // Then
        assertNotNull(response);
        assertEquals(tenantId, response.tenantId());

        verify(tenantRepository).findById(sampleTenantId);
        verify(tenantRepository).save(argThat(tenant -> 
                tenant.getStatus() == Tenant.TenantStatus.ACTIVE));
    }

    @Test
    @DisplayName("Should get all tenants with pagination")
    void shouldGetAllTenantsWithPagination() {
        // Given
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("name"));
        List<Tenant> tenants = Arrays.asList(sampleTenant);
        Page<Tenant> tenantPage = new PageImpl<>(tenants, pageRequest, 1);

        when(tenantRepository.findAll(pageRequest)).thenReturn(tenantPage);
        when(userRepository.findTenantAdministrators(any(UUID.class))).thenReturn(Arrays.asList(sampleAdminUser));
        when(userRepository.countByTenantId(any(UUID.class))).thenReturn(1L);

        // When
        TenantListResponse response = tenantService.getAllTenants(pageRequest);

        // Then
        assertNotNull(response);
        assertEquals(1, response.totalCount());
        assertEquals(0, response.page());
        assertEquals(10, response.size());
        assertEquals(1, response.totalPages());
        assertEquals(1, response.tenants().size());

        verify(tenantRepository).findAll(pageRequest);
    }

    @Test
    @DisplayName("Should delete tenant successfully")
    void shouldDeleteTenantSuccessfully() {
        // Given
        String tenantId = sampleTenantId.toString();
        when(tenantRepository.findById(sampleTenantId)).thenReturn(Optional.of(sampleTenant));
        when(userRepository.countByTenantId(sampleTenantId)).thenReturn(0L); // No users

        // When
        assertDoesNotThrow(() -> tenantService.deleteTenant(tenantId));

        // Then
        verify(tenantRepository).findById(sampleTenantId);
        verify(userRepository).countByTenantId(sampleTenantId);
        verify(tenantRepository).delete(sampleTenant);
    }

    @Test
    @DisplayName("Should throw exception when trying to delete tenant with users")
    void shouldThrowExceptionWhenTryingToDeleteTenantWithUsers() {
        // Given
        String tenantId = sampleTenantId.toString();
        when(tenantRepository.findById(sampleTenantId)).thenReturn(Optional.of(sampleTenant));
        when(userRepository.countByTenantId(sampleTenantId)).thenReturn(1L); // Has users

        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> tenantService.deleteTenant(tenantId)
        );
        assertTrue(exception.getMessage().contains("Cannot delete tenant with existing users"));

        verify(tenantRepository).findById(sampleTenantId);
        verify(userRepository).countByTenantId(sampleTenantId);
        verify(tenantRepository, never()).delete(any(Tenant.class));
    }

    @Test
    @DisplayName("Should throw exception with invalid UUID format")
    void shouldThrowExceptionWithInvalidUUIDFormat() {
        // Given
        String invalidTenantId = "invalid-uuid";

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tenantService.getTenantById(invalidTenantId)
        );
        assertTrue(exception.getMessage().contains("Invalid tenant ID format"));
    }

    private Tenant createSampleTenant() {
        Tenant tenant = new Tenant();
        tenant.setId(sampleTenantId);
        tenant.setName("Sample Tenant");
        tenant.setSlug("sample-tenant");
        tenant.setDescription("A sample tenant");
        tenant.setStatus(Tenant.TenantStatus.ACTIVE);
        tenant.setMaxDocuments(1000);
        tenant.setMaxStorageMb(10240L);
        tenant.setCreatedAt(LocalDateTime.now().minusDays(1));
        tenant.setUpdatedAt(LocalDateTime.now());
        return tenant;
    }

    private User createSampleAdminUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFirstName("Admin");
        user.setLastName("User");
        user.setEmail("admin@sample.com");
        user.setRole(User.UserRole.ADMIN);
        user.setStatus(User.UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setTenant(sampleTenant);
        return user;
    }
}
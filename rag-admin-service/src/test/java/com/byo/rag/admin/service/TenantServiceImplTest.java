package com.byo.rag.admin.service;

import com.byo.rag.admin.dto.*;
import com.byo.rag.admin.repository.TenantRepository;
import com.byo.rag.admin.repository.UserRepository;
import com.byo.rag.shared.entity.Tenant;
import com.byo.rag.shared.entity.User;
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

import static org.assertj.core.api.Assertions.*;
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
        assertThat(response)
            .describedAs("Tenant creation response should not be null")
            .isNotNull();
        assertThat(response.tenantId())
            .describedAs("Response should contain the generated tenant ID")
            .isEqualTo(sampleTenantId.toString());
        assertThat(response.name())
            .describedAs("Response should contain the requested tenant name")
            .isEqualTo(request.name());
        assertThat(response.adminEmail())
            .describedAs("Response should contain the admin email address")
            .isEqualTo(request.adminEmail());
        assertThat(response.description())
            .describedAs("Response should contain the tenant description")
            .isEqualTo(request.description());
        assertThat(response.status())
            .describedAs("New tenant should have ACTIVE status")
            .isEqualTo("ACTIVE");
        assertThat(response.isActive())
            .describedAs("New tenant should be marked as active")
            .isTrue();

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
        assertThatThrownBy(() -> tenantService.createTenant(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Tenant with name 'Existing Tenant' already exists")
            .describedAs("Should throw IllegalArgumentException for duplicate tenant names");

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
        assertThatThrownBy(() -> tenantService.createTenant(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("User with email 'existing@admin.com' already exists")
            .describedAs("Should throw IllegalArgumentException for duplicate admin emails");

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
        assertThat(response)
            .describedAs("Tenant retrieval response should not be null")
            .isNotNull();
        assertThat(response.tenantId())
            .describedAs("Response should contain the requested tenant ID")
            .isEqualTo(tenantId);
        assertThat(response.name())
            .describedAs("Response should contain the tenant name")
            .isEqualTo(sampleTenant.getName());
        assertThat(response.adminEmail())
            .describedAs("Response should contain the admin user email")
            .isEqualTo(sampleAdminUser.getEmail());

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
        assertThatThrownBy(() -> tenantService.getTenantById(tenantId))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Tenant not found")
            .describedAs("Should throw RuntimeException for non-existent tenant IDs");

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
        assertThat(response)
            .describedAs("Tenant update response should not be null")
            .isNotNull();
        assertThat(response.tenantId())
            .describedAs("Response should contain the updated tenant ID")
            .isEqualTo(tenantId);

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
        assertThat(response)
            .describedAs("Tenant suspension response should not be null")
            .isNotNull();
        assertThat(response.tenantId())
            .describedAs("Response should contain the suspended tenant ID")
            .isEqualTo(tenantId);

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
        assertThat(response)
            .describedAs("Tenant reactivation response should not be null")
            .isNotNull();
        assertThat(response.tenantId())
            .describedAs("Response should contain the reactivated tenant ID")
            .isEqualTo(tenantId);

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
        assertThat(response)
            .describedAs("Tenant list response should not be null")
            .isNotNull();
        assertThat(response.totalCount())
            .describedAs("Response should contain correct total count")
            .isEqualTo(1);
        assertThat(response.page())
            .describedAs("Response should contain correct page number")
            .isEqualTo(0);
        assertThat(response.size())
            .describedAs("Response should contain correct page size")
            .isEqualTo(10);
        assertThat(response.totalPages())
            .describedAs("Response should contain correct total pages")
            .isEqualTo(1);
        assertThat(response.tenants())
            .describedAs("Response should contain tenant list with expected size")
            .hasSize(1);

        verify(tenantRepository).findAll(pageRequest);
    }

    @Test
    @DisplayName("Should delete tenant successfully")
    void shouldDeleteTenantSuccessfully() {
        // Given
        String tenantId = sampleTenantId.toString();
        when(tenantRepository.findById(sampleTenantId)).thenReturn(Optional.of(sampleTenant));
        when(userRepository.countByTenantId(sampleTenantId)).thenReturn(0L); // No users

        // When & Then
        assertThatCode(() -> tenantService.deleteTenant(tenantId))
            .describedAs("Tenant deletion should not throw exception when no users exist")
            .doesNotThrowAnyException();

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
        assertThatThrownBy(() -> tenantService.deleteTenant(tenantId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot delete tenant with existing users")
            .describedAs("Should throw IllegalStateException when tenant has existing users");

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
        assertThatThrownBy(() -> tenantService.getTenantById(invalidTenantId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid tenant ID format")
            .describedAs("Should throw IllegalArgumentException for malformed UUID strings");
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
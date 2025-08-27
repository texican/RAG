package com.enterprise.rag.admin.service;

import com.enterprise.rag.admin.dto.*;
import com.enterprise.rag.admin.repository.TenantRepository;
import com.enterprise.rag.admin.repository.UserRepository;
import com.enterprise.rag.shared.entity.Tenant;
import com.enterprise.rag.shared.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of TenantService using JPA repositories for database persistence.
 * Provides comprehensive tenant management with database backing.
 */
@Service
@Transactional
public class TenantServiceImpl implements TenantService {

    private static final Logger logger = LoggerFactory.getLogger(TenantServiceImpl.class);

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public TenantServiceImpl(TenantRepository tenantRepository, 
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public TenantResponse createTenant(TenantCreateRequest request) {
        logger.info("Creating new tenant: {}", request.name());

        // Validate unique constraints
        if (tenantRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Tenant with name '" + request.name() + "' already exists");
        }

        String slug = generateSlug(request.name());
        if (tenantRepository.existsBySlug(slug)) {
            throw new IllegalArgumentException("Tenant with slug '" + slug + "' already exists");
        }

        if (userRepository.existsByEmail(request.adminEmail())) {
            throw new IllegalArgumentException("User with email '" + request.adminEmail() + "' already exists");
        }

        try {
            // Create tenant
            Tenant tenant = new Tenant();
            tenant.setName(request.name());
            tenant.setSlug(slug);
            tenant.setDescription(request.description());
            tenant.setStatus(Tenant.TenantStatus.ACTIVE);
            tenant.setMaxDocuments(1000); // Default limit
            tenant.setMaxStorageMb(10240L); // 10GB default

            tenant = tenantRepository.save(tenant);
            logger.info("Created tenant with ID: {}", tenant.getId());

            // Create admin user for the tenant
            User adminUser = createTenantAdmin(tenant, request.adminEmail());
            logger.info("Created admin user for tenant: {}", adminUser.getId());

            return toTenantResponse(tenant, request.adminEmail());
        } catch (Exception e) {
            logger.error("Error creating tenant: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create tenant: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TenantResponse getTenantById(String tenantId) {
        UUID uuid = parseUUID(tenantId);
        Tenant tenant = tenantRepository.findById(uuid)
                .orElseThrow(() -> new RuntimeException("Tenant not found with ID: " + tenantId));

        String adminEmail = getAdminEmail(tenant);
        return toTenantResponse(tenant, adminEmail);
    }

    @Override
    public TenantResponse updateTenant(String tenantId, TenantUpdateRequest request) {
        logger.info("Updating tenant: {}", tenantId);
        UUID uuid = parseUUID(tenantId);
        
        Tenant tenant = tenantRepository.findById(uuid)
                .orElseThrow(() -> new RuntimeException("Tenant not found with ID: " + tenantId));

        // Validate unique constraints if name is being changed
        if (request.name() != null && !request.name().equals(tenant.getName())) {
            if (tenantRepository.existsByName(request.name())) {
                throw new IllegalArgumentException("Tenant with name '" + request.name() + "' already exists");
            }
            tenant.setName(request.name());
            tenant.setSlug(generateSlug(request.name()));
        }

        if (request.description() != null) {
            tenant.setDescription(request.description());
        }

        tenant = tenantRepository.save(tenant);
        logger.info("Updated tenant: {}", tenant.getId());

        String adminEmail = getAdminEmail(tenant);
        return toTenantResponse(tenant, adminEmail);
    }

    @Override
    public TenantResponse suspendTenant(String tenantId, TenantSuspendRequest request) {
        logger.info("Suspending tenant: {}", tenantId);
        UUID uuid = parseUUID(tenantId);
        
        Tenant tenant = tenantRepository.findById(uuid)
                .orElseThrow(() -> new RuntimeException("Tenant not found with ID: " + tenantId));

        tenant.setStatus(Tenant.TenantStatus.SUSPENDED);
        tenant = tenantRepository.save(tenant);

        logger.info("Suspended tenant: {}", tenant.getId());
        String adminEmail = getAdminEmail(tenant);
        return toTenantResponse(tenant, adminEmail);
    }

    @Override
    public TenantResponse reactivateTenant(String tenantId) {
        logger.info("Reactivating tenant: {}", tenantId);
        UUID uuid = parseUUID(tenantId);
        
        Tenant tenant = tenantRepository.findById(uuid)
                .orElseThrow(() -> new RuntimeException("Tenant not found with ID: " + tenantId));

        tenant.setStatus(Tenant.TenantStatus.ACTIVE);
        tenant = tenantRepository.save(tenant);

        logger.info("Reactivated tenant: {}", tenant.getId());
        String adminEmail = getAdminEmail(tenant);
        return toTenantResponse(tenant, adminEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public TenantListResponse getAllTenants(PageRequest pageRequest) {
        Page<Tenant> tenantPage = tenantRepository.findAll(pageRequest);
        
        List<TenantResponse> tenantResponses = tenantPage.getContent().stream()
                .map(tenant -> {
                    String adminEmail = getAdminEmail(tenant);
                    return toTenantResponse(tenant, adminEmail);
                })
                .collect(Collectors.toList());

        return new TenantListResponse(
                tenantResponses,
                (int) tenantPage.getTotalElements(),
                tenantPage.getNumber(),
                tenantPage.getSize(),
                tenantPage.getTotalPages()
        );
    }

    @Override
    public void deleteTenant(String tenantId) {
        logger.info("Deleting tenant: {}", tenantId);
        UUID uuid = parseUUID(tenantId);
        
        Tenant tenant = tenantRepository.findById(uuid)
                .orElseThrow(() -> new RuntimeException("Tenant not found with ID: " + tenantId));

        // Check if tenant has users or documents before deletion
        long userCount = userRepository.countByTenantId(uuid);
        if (userCount > 0) {
            throw new IllegalStateException("Cannot delete tenant with existing users. Please remove all users first.");
        }

        tenantRepository.delete(tenant);
        logger.info("Deleted tenant: {}", tenantId);
    }

    /**
     * Create an admin user for the tenant
     */
    private User createTenantAdmin(Tenant tenant, String adminEmail) {
        User adminUser = new User();
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setEmail(adminEmail);
        adminUser.setPasswordHash(passwordEncoder.encode("TempPassword123!")); // Temporary password
        adminUser.setRole(User.UserRole.ADMIN);
        adminUser.setStatus(User.UserStatus.PENDING_VERIFICATION);
        adminUser.setEmailVerified(false);
        adminUser.setTenant(tenant);

        return userRepository.save(adminUser);
    }

    /**
     * Get the admin email for a tenant
     */
    private String getAdminEmail(Tenant tenant) {
        List<User> admins = userRepository.findTenantAdministrators(tenant.getId());
        return admins.isEmpty() ? "no-admin@example.com" : admins.get(0).getEmail();
    }

    /**
     * Convert Tenant entity to TenantResponse DTO
     */
    private TenantResponse toTenantResponse(Tenant tenant, String adminEmail) {
        int userCount = (int) userRepository.countByTenantId(tenant.getId());
        // Note: Document count would need Document repository - setting to 0 for now
        int documentCount = 0; // TODO: Implement when document repository is available
        
        return new TenantResponse(
                tenant.getId().toString(),
                tenant.getName(),
                adminEmail,
                tenant.getDescription(),
                tenant.getStatus().toString(),
                tenant.getCreatedAt(),
                tenant.getUpdatedAt(),
                userCount,
                documentCount,
                tenant.isActive()
        );
    }

    /**
     * Generate a URL-friendly slug from the tenant name
     */
    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "") // Remove special characters
                .replaceAll("\\s+", "-") // Replace spaces with hyphens
                .replaceAll("-+", "-") // Remove duplicate hyphens
                .replaceAll("^-|-$", ""); // Remove leading/trailing hyphens
    }

    /**
     * Parse UUID from string with proper error handling
     */
    private UUID parseUUID(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid tenant ID format: " + id);
        }
    }
}
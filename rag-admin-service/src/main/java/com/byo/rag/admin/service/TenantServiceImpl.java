package com.byo.rag.admin.service;

import com.byo.rag.admin.dto.*;
import com.byo.rag.admin.repository.TenantRepository;
import com.byo.rag.admin.repository.UserRepository;
import com.byo.rag.shared.entity.Tenant;
import com.byo.rag.shared.entity.User;
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
 * Enterprise-grade tenant management service implementation providing comprehensive
 * multi-tenant operations with full database persistence and transactional integrity.
 * 
 * <p>This service implements the complete tenant lifecycle management for the Enterprise RAG
 * system, including tenant creation, updates, suspension/reactivation, and deletion operations.
 * All operations are performed with strict data validation, security checks, and proper
 * transactional boundaries to ensure data consistency across the multi-tenant architecture.</p>
 * 
 * <p><strong>Key Features:</strong></p>
 * <ul>
 *   <li><strong>Database-Backed Persistence:</strong> Full JPA repository integration with PostgreSQL</li>
 *   <li><strong>Transactional Safety:</strong> All operations wrapped in database transactions</li>
 *   <li><strong>Validation & Constraints:</strong> Unique constraint validation for names, slugs, and emails</li>
 *   <li><strong>Admin User Management:</strong> Automatic admin user creation during tenant provisioning</li>
 *   <li><strong>Status Management:</strong> Complete tenant lifecycle with suspension/reactivation</li>
 *   <li><strong>Audit Logging:</strong> Comprehensive operational logging for security and compliance</li>
 * </ul>
 * 
 * <p><strong>Multi-Tenant Architecture:</strong></p>
 * <p>Each tenant is completely isolated with its own administrative users, documents, and configurations.
 * The service enforces strict tenant boundaries and provides pagination support for enterprise-scale
 * tenant management operations.</p>
 * 
 * <p><strong>Security Considerations:</strong></p>
 * <ul>
 *   <li>Password encoding using BCrypt for admin users</li>
 *   <li>UUID-based tenant identification to prevent enumeration</li>
 *   <li>Status-based access control (ACTIVE/SUSPENDED)</li>
 *   <li>Validation of unique constraints across tenant boundaries</li>
 * </ul>
 * 
 * <p><strong>Integration Points:</strong></p>
 * <ul>
 *   <li>{@link TenantRepository} - Database operations for tenant entities</li>
 *   <li>{@link UserRepository} - User management within tenant boundaries</li>
 *   <li>{@link PasswordEncoder} - Secure password hashing for admin users</li>
 * </ul>
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 * @see TenantService
 * @see com.byo.rag.shared.entity.Tenant
 * @see com.byo.rag.shared.entity.User
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

    /**
     * Creates a new tenant with complete database persistence and automatic admin user provisioning.
     * 
     * <p>This method performs a comprehensive tenant creation process including:</p>
     * <ul>
     *   <li>Validation of unique constraints (name, slug, admin email)</li>
     *   <li>Tenant entity creation with default resource limits</li>
     *   <li>Automatic URL-friendly slug generation</li>
     *   <li>Admin user creation with secure password encoding</li>
     *   <li>Transactional integrity across all operations</li>
     * </ul>
     * 
     * <p><strong>Default Resource Limits:</strong></p>
     * <ul>
     *   <li>Max Documents: 1,000</li>
     *   <li>Max Storage: 10GB (10,240MB)</li>
     *   <li>Status: ACTIVE</li>
     * </ul>
     * 
     * @param request the tenant creation request containing name, description, and admin email
     * @return {@link TenantResponse} containing the created tenant details and admin information
     * @throws IllegalArgumentException if tenant name, slug, or admin email already exists
     * @throws RuntimeException if tenant creation fails due to database or system errors
     * @see TenantCreateRequest
     * @see TenantResponse
     */
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

    /**
     * Retrieves a tenant by its unique identifier with complete admin information.
     * 
     * <p>This read-only operation fetches the tenant details and automatically
     * resolves the associated admin user information for the response.</p>
     * 
     * @param tenantId the UUID string identifier of the tenant to retrieve
     * @return {@link TenantResponse} containing complete tenant and admin details
     * @throws IllegalArgumentException if the tenant ID format is invalid
     * @throws RuntimeException if no tenant exists with the specified ID
     * @see TenantResponse
     */
    @Override
    @Transactional(readOnly = true)
    public TenantResponse getTenantById(String tenantId) {
        UUID uuid = parseUUID(tenantId);
        Tenant tenant = tenantRepository.findById(uuid)
                .orElseThrow(() -> new RuntimeException("Tenant not found with ID: " + tenantId));

        String adminEmail = getAdminEmail(tenant);
        return toTenantResponse(tenant, adminEmail);
    }

    /**
     * Updates an existing tenant with comprehensive validation and constraint checking.
     * 
     * <p>This method supports partial updates with automatic slug regeneration when
     * the tenant name is modified. All unique constraints are validated before
     * applying changes to ensure data integrity.</p>
     * 
     * <p><strong>Supported Updates:</strong></p>
     * <ul>
     *   <li>Tenant name (with automatic slug regeneration)</li>
     *   <li>Tenant description</li>
     *   <li>Automatic timestamp updates</li>
     * </ul>
     * 
     * @param tenantId the UUID string identifier of the tenant to update
     * @param request the update request containing fields to modify
     * @return {@link TenantResponse} containing the updated tenant details
     * @throws IllegalArgumentException if tenant ID format is invalid or name conflicts exist
     * @throws RuntimeException if no tenant exists with the specified ID
     * @see TenantUpdateRequest
     * @see TenantResponse
     */
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

    /**
     * Suspends a tenant, effectively disabling all tenant operations while preserving data.
     * 
     * <p>When a tenant is suspended, all users within that tenant are prevented from
     * accessing the system, but all data remains intact for potential reactivation.</p>
     * 
     * <p><strong>Suspension Effects:</strong></p>
     * <ul>
     *   <li>Status changed to SUSPENDED</li>
     *   <li>All tenant users lose access</li>
     *   <li>Data preservation for potential reactivation</li>
     *   <li>Audit logging of suspension action</li>
     * </ul>
     * 
     * @param tenantId the UUID string identifier of the tenant to suspend
     * @param request the suspension request (may contain reason or additional metadata)
     * @return {@link TenantResponse} containing the updated tenant details with SUSPENDED status
     * @throws IllegalArgumentException if the tenant ID format is invalid
     * @throws RuntimeException if no tenant exists with the specified ID
     * @see TenantSuspendRequest
     * @see TenantResponse
     */
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

    /**
     * Reactivates a previously suspended tenant, restoring full system access.
     * 
     * <p>This operation changes the tenant status back to ACTIVE, allowing all
     * tenant users to regain access to the system with their existing data intact.</p>
     * 
     * <p><strong>Reactivation Effects:</strong></p>
     * <ul>
     *   <li>Status changed to ACTIVE</li>
     *   <li>All tenant users regain access</li>
     *   <li>All existing data remains accessible</li>
     *   <li>Audit logging of reactivation action</li>
     * </ul>
     * 
     * @param tenantId the UUID string identifier of the tenant to reactivate
     * @return {@link TenantResponse} containing the updated tenant details with ACTIVE status
     * @throws IllegalArgumentException if the tenant ID format is invalid
     * @throws RuntimeException if no tenant exists with the specified ID
     * @see TenantResponse
     */
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

    /**
     * Retrieves a paginated list of all tenants in the system with complete metadata.
     * 
     * <p>This read-only operation supports enterprise-scale tenant management by providing
     * paginated access to all tenants with their associated admin information and statistics.</p>
     * 
     * <p><strong>Response Includes:</strong></p>
     * <ul>
     *   <li>Paginated tenant list with admin details</li>
     *   <li>User count per tenant</li>
     *   <li>Document count per tenant (when available)</li>
     *   <li>Pagination metadata (total pages, current page, etc.)</li>
     * </ul>
     * 
     * @param pageRequest the pagination parameters (page number, size, sorting)
     * @return {@link TenantListResponse} containing paginated tenant data and metadata
     * @see TenantListResponse
     * @see TenantResponse
     */
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

    /**
     * Permanently deletes a tenant and all associated data with comprehensive safety checks.
     * 
     * <p><strong>⚠️ WARNING:</strong> This is a destructive operation that permanently removes
     * all tenant data. The operation includes safety checks to prevent accidental deletion
     * of tenants with active users or documents.</p>
     * 
     * <p><strong>Pre-Deletion Validation:</strong></p>
     * <ul>
     *   <li>Verifies tenant exists</li>
     *   <li>Ensures no active users are associated with the tenant</li>
     *   <li>Confirms no documents exist (when document service is integrated)</li>
     *   <li>Prevents deletion of tenants with data dependencies</li>
     * </ul>
     * 
     * @param tenantId the UUID string identifier of the tenant to delete
     * @throws IllegalArgumentException if the tenant ID format is invalid
     * @throws RuntimeException if no tenant exists with the specified ID
     * @throws IllegalStateException if tenant has existing users or data that prevents deletion
     */
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
     * Creates an administrative user for a newly created tenant with secure defaults.
     * 
     * <p>This method provisions an admin user with proper security settings including:
     * encrypted password, pending verification status, and administrative privileges
     * within the tenant's scope.</p>
     * 
     * <p><strong>Admin User Configuration:</strong></p>
     * <ul>
     *   <li>Role: ADMIN (tenant-level administrative privileges)</li>
     *   <li>Status: PENDING_VERIFICATION (requires email verification)</li>
     *   <li>Password: Securely encrypted temporary password</li>
     *   <li>Email Verification: Disabled pending verification process</li>
     * </ul>
     * 
     * @param tenant the tenant entity to associate the admin user with
     * @param adminEmail the email address for the admin user
     * @return the created and persisted {@link User} entity with admin privileges
     * @see User.UserRole#ADMIN
     * @see User.UserStatus#PENDING_VERIFICATION
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
     * Retrieves the primary administrative email address for a tenant.
     * 
     * <p>This method queries the user repository to find administrative users
     * associated with the tenant and returns the first admin's email address.
     * Returns a default placeholder if no admin users are found.</p>
     * 
     * @param tenant the tenant entity to find admin email for
     * @return the admin email address, or "no-admin@example.com" if no admin exists
     * @see UserRepository#findTenantAdministrators(UUID)
     */
    private String getAdminEmail(Tenant tenant) {
        List<User> admins = userRepository.findTenantAdministrators(tenant.getId());
        return admins.isEmpty() ? "no-admin@example.com" : admins.get(0).getEmail();
    }

    /**
     * Converts a Tenant entity to a complete TenantResponse DTO with statistics.
     * 
     * <p>This mapping method enriches the tenant data with computed statistics
     * including user counts and document counts (when available). The response
     * provides a comprehensive view suitable for API consumers.</p>
     * 
     * <p><strong>Computed Statistics:</strong></p>
     * <ul>
     *   <li>Active user count from database query</li>
     *   <li>Document count (placeholder - requires document service integration)</li>
     *   <li>Tenant status and activity indicators</li>
     * </ul>
     * 
     * @param tenant the tenant entity to convert
     * @param adminEmail the administrative email address for this tenant
     * @return a complete {@link TenantResponse} DTO with all tenant details and statistics
     * @see TenantResponse
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
     * Generates a URL-friendly slug from the tenant name following web standards.
     * 
     * <p>This method transforms tenant names into clean, URL-safe identifiers by:
     * removing special characters, converting to lowercase, replacing spaces with
     * hyphens, and eliminating duplicate or trailing separators.</p>
     * 
     * <p><strong>Transformation Rules:</strong></p>
     * <ul>
     *   <li>Convert to lowercase</li>
     *   <li>Remove special characters (keep alphanumeric, spaces, hyphens)</li>
     *   <li>Replace spaces with hyphens</li>
     *   <li>Collapse multiple hyphens to single hyphens</li>
     *   <li>Remove leading and trailing hyphens</li>
     * </ul>
     * 
     * @param name the tenant name to convert to a slug
     * @return a URL-friendly slug suitable for use in web addresses
     * @example "My Company LLC" becomes "my-company-llc"
     */
    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "") // Remove special characters
                .replaceAll("\\s+", "-") // Replace spaces with hyphens
                .replaceAll("-+", "-") // Remove duplicate hyphens
                .replaceAll("^-|-$", ""); // Remove leading/trailing hyphens
    }

    /**
     * Safely parses a UUID string with comprehensive error handling and validation.
     * 
     * <p>This utility method provides robust UUID parsing with descriptive error
     * messages for invalid formats, ensuring consistent error handling across
     * all tenant ID operations.</p>
     * 
     * @param id the string representation of the UUID to parse
     * @return a valid {@link UUID} object
     * @throws IllegalArgumentException if the string is not a valid UUID format
     * @see UUID#fromString(String)
     */
    private UUID parseUUID(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid tenant ID format: " + id);
        }
    }
}
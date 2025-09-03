package com.byo.rag.auth.repository;

import com.byo.rag.shared.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for user data access and multi-tenant user management.
 * 
 * <p>This repository provides comprehensive data access methods for user entities
 * in the Enterprise RAG system, with built-in support for multi-tenant isolation,
 * user lifecycle management, and authentication operations. All user operations
 * respect tenant boundaries and support paginated access for scalability.
 * 
 * <p><strong>Multi-Tenant User Management:</strong>
 * <ul>
 *   <li><strong>Tenant Isolation:</strong> All user queries support tenant-specific filtering</li>
 *   <li><strong>Cross-Tenant Prevention:</strong> Users can only access resources within their tenant</li>
 *   <li><strong>Scalable Access:</strong> Paginated queries for large tenant user bases</li>
 *   <li><strong>Status Management:</strong> Active/inactive user lifecycle support</li>
 * </ul>
 * 
 * <p><strong>Authentication Support:</strong>
 * <ul>
 *   <li><strong>Email-Based Login:</strong> Email addresses serve as primary user identifiers</li>
 *   <li><strong>Active User Validation:</strong> Authentication limited to active accounts</li>
 *   <li><strong>Email Verification:</strong> Token-based email verification workflow</li>
 *   <li><strong>Unique Constraints:</strong> Email uniqueness validation across the system</li>
 * </ul>
 * 
 * <p><strong>Query Performance:</strong>
 * <ul>
 *   <li><strong>Indexed Lookups:</strong> Email and tenant ID queries use database indexes</li>
 *   <li><strong>Status Filtering:</strong> Active user queries optimize for common access patterns</li>
 *   <li><strong>Pagination Support:</strong> Large tenant user lists handled efficiently</li>
 *   <li><strong>Count Queries:</strong> Statistical operations for administrative dashboards</li>
 * </ul>
 * 
 * <p><strong>Security Considerations:</strong>
 * <ul>
 *   <li>All user access must be validated against current tenant context</li>
 *   <li>Email addresses are sensitive PII and should be handled securely</li>
 *   <li>Status checks prevent access to disabled or suspended accounts</li>
 *   <li>Verification tokens provide secure email validation workflow</li>
 * </ul>
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.0
 * @see User
 * @see Tenant
 * @see JpaRepository
 * @see Page
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by their email address across all tenants.
     * 
     * <p>This method searches for users by email regardless of tenant or status,
     * primarily used for authentication and system administration. For tenant-specific
     * operations, use tenant-scoped query methods instead.
     * 
     * <p><strong>Authentication Usage:</strong>
     * Used during login to locate user accounts before tenant validation.
     * The authentication service then validates the user belongs to the
     * expected tenant context.
     * 
     * @param email the user's email address
     * @return Optional containing the user if found, empty otherwise
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks if a user exists with the specified email address.
     * 
     * <p>This method validates email uniqueness during user registration
     * and prevents duplicate accounts across all tenants in the system.
     * Email addresses must be globally unique.
     * 
     * @param email the email address to check
     * @return true if a user with this email exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Finds an active user by their email address.
     * 
     * <p>This method only returns users with ACTIVE status, ensuring that
     * suspended, disabled, or pending users cannot authenticate or access
     * the system. Used primarily by authentication filters and services.
     * 
     * <p><strong>Status Filtering:</strong>
     * Only returns users where status = 'ACTIVE', providing automatic
     * security filtering for user authentication operations.
     * 
     * @param email the user's email address
     * @return Optional containing the active user if found, empty otherwise
     */
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.status = 'ACTIVE'")
    Optional<User> findActiveByEmail(@Param("email") String email);

    /**
     * Finds all users belonging to a specific tenant with pagination support.
     * 
     * <p>This method provides tenant-scoped user access with pagination for
     * scalable user management operations. Returns users regardless of status
     * for administrative operations that need to see all users.
     * 
     * <p><strong>Multi-Tenant Isolation:</strong>
     * Automatically enforces tenant boundaries by filtering users based on
     * tenant ID, ensuring complete isolation between organizations.
     * 
     * <p><strong>Pagination:</strong>
     * Supports large tenant user bases by returning paginated results,
     * enabling efficient UI rendering and reduced memory usage.
     * 
     * @param tenantId the tenant's unique identifier
     * @param pageable pagination parameters (page number, size, sorting)
     * @return Page containing users for the specified tenant
     */
    @Query("SELECT u FROM User u WHERE u.tenant.id = :tenantId")
    Page<User> findByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    /**
     * Finds active users belonging to a specific tenant with pagination support.
     * 
     * <p>This method combines tenant isolation with status filtering to return
     * only operational user accounts within a tenant. Used for normal business
     * operations where suspended or disabled users should not appear.
     * 
     * <p><strong>Combined Filtering:</strong>
     * Applies both tenant isolation (tenant.id = :tenantId) and status
     * filtering (status = 'ACTIVE') for secure operational user access.
     * 
     * @param tenantId the tenant's unique identifier
     * @param pageable pagination parameters for result management
     * @return Page containing active users for the specified tenant
     */
    @Query("SELECT u FROM User u WHERE u.tenant.id = :tenantId AND u.status = 'ACTIVE'")
    Page<User> findActiveByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    /**
     * Counts the total number of users belonging to a specific tenant.
     * 
     * <p>This method provides tenant-specific user statistics for administrative
     * dashboards, capacity planning, and billing calculations. Counts all users
     * regardless of status for complete tenant metrics.
     * 
     * <p><strong>Administrative Uses:</strong>
     * <ul>
     *   <li>Tenant usage statistics and billing</li>
     *   <li>License compliance and seat management</li>
     *   <li>User interface pagination calculations</li>
     *   <li>System capacity planning per tenant</li>
     * </ul>
     * 
     * @param tenantId the tenant's unique identifier
     * @return the count of all users in the specified tenant
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.tenant.id = :tenantId")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Finds a user by their email verification token.
     * 
     * <p>This method supports the email verification workflow by locating users
     * based on verification tokens sent via email. Used during account activation
     * and email address verification processes.
     * 
     * <p><strong>Security Considerations:</strong>
     * <ul>
     *   <li>Verification tokens should be cryptographically secure</li>
     *   <li>Tokens should have expiration times for security</li>
     *   <li>Successful verification should clear the token</li>
     *   <li>Failed attempts should be logged for monitoring</li>
     * </ul>
     * 
     * @param token the email verification token to search for
     * @return Optional containing the user if token matches, empty otherwise
     */
    Optional<User> findByEmailVerificationToken(String token);
}
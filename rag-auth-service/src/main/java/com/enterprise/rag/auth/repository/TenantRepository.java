package com.enterprise.rag.auth.repository;

import com.enterprise.rag.shared.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for tenant data access and management operations.
 * 
 * <p>This repository provides comprehensive data access methods for tenant entities
 * in the Enterprise RAG system, supporting both basic CRUD operations and specialized
 * queries for tenant management, authentication, and system administration.
 * 
 * <p><strong>Multi-Tenant Architecture:</strong>
 * Tenants represent organizations or customers in the system, providing:
 * <ul>
 *   <li><strong>Isolation:</strong> Complete data separation between organizations</li>
 *   <li><strong>Identification:</strong> Unique slugs for tenant identification in URLs</li>
 *   <li><strong>Status Management:</strong> Active/inactive tenant lifecycle control</li>
 *   <li><strong>Scalability:</strong> Support for unlimited tenant organizations</li>
 * </ul>
 * 
 * <p><strong>Query Strategies:</strong>
 * <ul>
 *   <li><strong>Slug-Based Lookup:</strong> Fast tenant resolution from URLs and client requests</li>
 *   <li><strong>Status Filtering:</strong> Active tenant queries for operational features</li>
 *   <li><strong>Statistical Queries:</strong> System metrics and administrative reporting</li>
 *   <li><strong>Existence Checks:</strong> Duplicate prevention and validation support</li>
 * </ul>
 * 
 * <p><strong>Security Considerations:</strong>
 * <ul>
 *   <li>All tenant access should be validated through authentication service</li>
 *   <li>Tenant slugs are public identifiers and should not contain sensitive data</li>
 *   <li>Status checks prevent access to suspended or inactive tenants</li>
 * </ul>
 * 
 * <p><strong>Usage in Services:</strong>
 * <ul>
 *   <li><strong>Authentication:</strong> Tenant resolution during login and JWT validation</li>
 *   <li><strong>Registration:</strong> New tenant creation and slug validation</li>
 *   <li><strong>Administration:</strong> Tenant lifecycle management and system monitoring</li>
 *   <li><strong>Multi-Tenancy:</strong> Request context establishment and tenant isolation</li>
 * </ul>
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.0
 * @see Tenant
 * @see JpaRepository
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    /**
     * Finds a tenant by its unique slug identifier.
     * 
     * <p>Slugs are human-readable identifiers used in URLs and API requests
     * to identify tenant organizations. This method supports both active
     * and inactive tenants for administrative purposes.
     * 
     * @param slug the tenant's unique slug identifier
     * @return Optional containing the tenant if found, empty otherwise
     */
    Optional<Tenant> findBySlug(String slug);

    /**
     * Checks if a tenant exists with the specified slug.
     * 
     * <p>This method is useful for validation during tenant registration
     * to prevent duplicate slugs and ensure unique tenant identifiers
     * across the system.
     * 
     * @param slug the slug to check for existence
     * @return true if a tenant with this slug exists, false otherwise
     */
    boolean existsBySlug(String slug);

    /**
     * Finds an active tenant by its slug identifier.
     * 
     * <p>This method only returns tenants with ACTIVE status, ensuring
     * that suspended or disabled tenant organizations cannot be accessed
     * through normal application flows.
     * 
     * <p><strong>Status Filtering:</strong>
     * Only tenants with status = 'ACTIVE' are returned, providing automatic
     * filtering for operational tenant access patterns.
     * 
     * @param slug the tenant's slug identifier
     * @return Optional containing the active tenant if found, empty otherwise
     */
    @Query("SELECT t FROM Tenant t WHERE t.slug = :slug AND t.status = 'ACTIVE'")
    Optional<Tenant> findActiveBySlug(@Param("slug") String slug);

    /**
     * Counts the total number of active tenants in the system.
     * 
     * <p>This method provides system-wide statistics for administrative
     * dashboards and monitoring purposes, counting only tenants that are
     * currently active and operational.
     * 
     * <p><strong>Administrative Use:</strong>
     * Used for:
     * <ul>
     *   <li>System capacity planning and scaling decisions</li>
     *   <li>Business metrics and growth tracking</li>
     *   <li>License compliance and billing calculations</li>
     *   <li>Performance monitoring and resource allocation</li>
     * </ul>
     * 
     * @return the count of active tenants in the system
     */
    @Query("SELECT COUNT(t) FROM Tenant t WHERE t.status = 'ACTIVE'")
    long countActiveTenants();
}
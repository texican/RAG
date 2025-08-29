package com.enterprise.rag.admin.repository;

import com.enterprise.rag.shared.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Enterprise-grade repository interface for comprehensive tenant data access and management operations.
 * 
 * <p>This repository provides a complete suite of tenant management capabilities including CRUD operations,
 * advanced search functionality, analytics queries, and administrative reporting features. All operations
 * maintain data integrity and support the multi-tenant architecture with proper isolation boundaries.</p>
 * 
 * <p><strong>Core Capabilities:</strong></p>
 * <ul>
 *   <li><strong>Basic Operations:</strong> Standard CRUD with UUID-based primary keys</li>
 *   <li><strong>Advanced Search:</strong> Case-insensitive name search with pagination support</li>
 *   <li><strong>Status Management:</strong> Status-based filtering and tenant lifecycle operations</li>
 *   <li><strong>Validation Support:</strong> Unique constraint checking for names and slugs</li>
 *   <li><strong>Analytics Queries:</strong> Usage statistics, resource monitoring, and reporting</li>
 * </ul>
 * 
 * <p><strong>Multi-Tenant Features:</strong></p>
 * <ul>
 *   <li>Tenant isolation enforcement at the database level</li>
 *   <li>Resource usage tracking and quota management</li>
 *   <li>Document and storage usage analytics</li>
 *   <li>User association and relationship management</li>
 * </ul>
 * 
 * <p><strong>Administrative Reporting:</strong></p>
 * <ul>
 *   <li>Tenant lifecycle monitoring (creation dates, activity)</li>
 *   <li>Resource utilization reports (documents, storage, users)</li>
 *   <li>Capacity planning data (tenants approaching limits)</li>
 *   <li>System health indicators and compliance reporting</li>
 * </ul>
 * 
 * <p><strong>Data Integrity:</strong></p>
 * <p>All queries are designed with performance optimization and data consistency in mind.
 * Custom JPQL queries include proper parameterization to prevent SQL injection and
 * maintain optimal database performance under enterprise-scale load.</p>
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 * @see com.enterprise.rag.shared.entity.Tenant
 * @see com.enterprise.rag.admin.service.TenantService
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    /**
     * Retrieves a tenant by its unique URL-friendly slug identifier.
     * 
     * <p>Slugs are automatically generated from tenant names and provide a clean,
     * URL-safe way to identify tenants in web interfaces and API endpoints.</p>
     * 
     * @param slug the URL-friendly unique identifier for the tenant
     * @return an {@link Optional} containing the tenant if found, empty otherwise
     * @see com.enterprise.rag.shared.entity.Tenant
     */
    Optional<Tenant> findBySlug(String slug);

    /**
     * Retrieves all tenants with the specified operational status.
     * 
     * <p>This method supports administrative filtering and management operations
     * by allowing status-based tenant organization and bulk operations.</p>
     * 
     * @param status the {@link com.enterprise.rag.shared.entity.Tenant.TenantStatus} to filter by
     * @return a {@link List} of tenants matching the specified status
     * @see com.enterprise.rag.shared.entity.Tenant.TenantStatus
     */
    List<Tenant> findByStatus(Tenant.TenantStatus status);

    /**
     * Find tenants by status with pagination
     */
    Page<Tenant> findByStatus(Tenant.TenantStatus status, Pageable pageable);

    /**
     * Performs case-insensitive search for tenants containing the specified name fragment.
     * 
     * <p>This method enables flexible tenant discovery through partial name matching,
     * supporting administrative search interfaces and user-friendly tenant selection.</p>
     * 
     * @param name the name fragment to search for (case-insensitive)
     * @return a {@link List} of tenants whose names contain the specified fragment
     * @see com.enterprise.rag.shared.entity.Tenant
     */
    @Query("SELECT t FROM Tenant t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Tenant> findByNameContainingIgnoreCase(@Param("name") String name);

    /**
     * Find tenants by name containing with pagination
     */
    @Query("SELECT t FROM Tenant t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Tenant> findByNameContainingIgnoreCase(@Param("name") String name, Pageable pageable);

    /**
     * Validates whether a tenant slug already exists in the system.
     * 
     * <p>This method supports unique constraint validation during tenant creation
     * and update operations, ensuring slug uniqueness across all tenants.</p>
     * 
     * @param slug the slug to check for existence
     * @return {@code true} if a tenant with the specified slug exists, {@code false} otherwise
     */
    boolean existsBySlug(String slug);

    /**
     * Validates whether a tenant name already exists in the system.
     * 
     * <p>This method supports unique constraint validation during tenant creation
     * and update operations, ensuring name uniqueness across all organizations.</p>
     * 
     * @param name the tenant name to check for existence
     * @return {@code true} if a tenant with the specified name exists, {@code false} otherwise
     */
    boolean existsByName(String name);

    /**
     * Generates tenant count statistics grouped by operational status.
     * 
     * <p>This method provides essential metrics for administrative dashboards,
     * system monitoring, and capacity planning by counting tenants in each status category.</p>
     * 
     * @param status the {@link com.enterprise.rag.shared.entity.Tenant.TenantStatus} to count
     * @return the number of tenants with the specified status
     * @see com.enterprise.rag.shared.entity.Tenant.TenantStatus
     */
    @Query("SELECT COUNT(t) FROM Tenant t WHERE t.status = :status")
    long countByStatus(@Param("status") Tenant.TenantStatus status);

    /**
     * Retrieves active tenants enriched with user population statistics.
     * 
     * <p>This analytical query combines tenant data with user counts to provide
     * comprehensive tenant analytics for administrative reporting and monitoring.
     * Results are ordered by creation date with newest tenants first.</p>
     * 
     * @return a {@link List} of Object arrays containing [Tenant, userCount] pairs
     * @see com.enterprise.rag.shared.entity.Tenant
     */
    @Query("SELECT t, SIZE(t.users) as userCount FROM Tenant t WHERE t.status = 'ACTIVE' ORDER BY t.createdAt DESC")
    List<Object[]> findActivTenantsWithUserCount();

    /**
     * Generates comprehensive tenant resource utilization report with document and storage analytics.
     * 
     * <p>This advanced analytical query aggregates tenant data with document counts and
     * storage usage, providing essential metrics for capacity planning, billing, and
     * resource optimization across the multi-tenant architecture.</p>
     * 
     * @return a {@link List} of Object arrays containing [tenantId, name, slug, documentCount, totalStorage]
     * @see com.enterprise.rag.shared.entity.Tenant
     * @see com.enterprise.rag.shared.entity.Document
     */
    @Query("SELECT t.id, t.name, t.slug, COUNT(d.id) as documentCount, COALESCE(SUM(d.fileSize), 0) as totalStorage " +
           "FROM Tenant t LEFT JOIN t.documents d " +
           "GROUP BY t.id, t.name, t.slug " +
           "ORDER BY t.createdAt DESC")
    List<Object[]> findTenantsWithDocumentUsage();

    /**
     * Identifies tenants approaching their resource limits for proactive capacity management.
     * 
     * <p>This query detects tenants that have exceeded 80% of their document count or
     * storage quotas, enabling administrators to take proactive action before limits
     * are reached and service disruptions occur.</p>
     * 
     * <p><strong>Thresholds Monitored:</strong></p>
     * <ul>
     *   <li>Document count exceeding 80% of maxDocuments limit</li>
     *   <li>Storage usage exceeding 80% of maxStorageMb limit</li>
     * </ul>
     * 
     * @return a {@link List} of tenants approaching their resource limits
     * @see com.enterprise.rag.shared.entity.Tenant
     */
    @Query("SELECT t FROM Tenant t JOIN t.documents d " +
           "GROUP BY t " +
           "HAVING COUNT(d) > (t.maxDocuments * 0.8) OR COALESCE(SUM(d.fileSize), 0) > (t.maxStorageMb * 1024 * 1024 * 0.8)")
    List<Tenant> findTenantsApproachingLimits();

    /**
     * Retrieves tenants created after the specified cutoff date for onboarding and analytics.
     * 
     * <p>This method supports administrative monitoring of tenant growth patterns,
     * onboarding success tracking, and system adoption analytics by filtering
     * tenants based on creation timestamps.</p>
     * 
     * @param cutoffDate the minimum creation date for tenant inclusion
     * @return a {@link List} of tenants created on or after the cutoff date, ordered newest first
     * @see com.enterprise.rag.shared.entity.Tenant
     */
    @Query("SELECT t FROM Tenant t WHERE t.createdAt >= :cutoffDate ORDER BY t.createdAt DESC")
    List<Tenant> findRecentlyCreatedTenants(@Param("cutoffDate") LocalDateTime cutoffDate);
}
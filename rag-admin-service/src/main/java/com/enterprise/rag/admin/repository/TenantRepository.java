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
 * Repository interface for Tenant entity operations in the admin service.
 * Provides CRUD operations and custom queries for tenant management.
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    /**
     * Find tenant by slug (unique identifier)
     */
    Optional<Tenant> findBySlug(String slug);

    /**
     * Find tenants by status
     */
    List<Tenant> findByStatus(Tenant.TenantStatus status);

    /**
     * Find tenants by status with pagination
     */
    Page<Tenant> findByStatus(Tenant.TenantStatus status, Pageable pageable);

    /**
     * Find tenants by name containing (case-insensitive search)
     */
    @Query("SELECT t FROM Tenant t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Tenant> findByNameContainingIgnoreCase(@Param("name") String name);

    /**
     * Find tenants by name containing with pagination
     */
    @Query("SELECT t FROM Tenant t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Tenant> findByNameContainingIgnoreCase(@Param("name") String name, Pageable pageable);

    /**
     * Check if slug exists (used for validation)
     */
    boolean existsBySlug(String slug);

    /**
     * Check if name exists (used for validation)
     */
    boolean existsByName(String name);

    /**
     * Get tenant statistics
     */
    @Query("SELECT COUNT(t) FROM Tenant t WHERE t.status = :status")
    long countByStatus(@Param("status") Tenant.TenantStatus status);

    /**
     * Find active tenants with user count
     */
    @Query("SELECT t, SIZE(t.users) as userCount FROM Tenant t WHERE t.status = 'ACTIVE' ORDER BY t.createdAt DESC")
    List<Object[]> findActivTenantsWithUserCount();

    /**
     * Find tenants with document usage
     */
    @Query("SELECT t.id, t.name, t.slug, COUNT(d.id) as documentCount, COALESCE(SUM(d.fileSize), 0) as totalStorage " +
           "FROM Tenant t LEFT JOIN t.documents d " +
           "GROUP BY t.id, t.name, t.slug " +
           "ORDER BY t.createdAt DESC")
    List<Object[]> findTenantsWithDocumentUsage();

    /**
     * Find tenants approaching their limits
     */
    @Query("SELECT t FROM Tenant t JOIN t.documents d " +
           "GROUP BY t " +
           "HAVING COUNT(d) > (t.maxDocuments * 0.8) OR COALESCE(SUM(d.fileSize), 0) > (t.maxStorageMb * 1024 * 1024 * 0.8)")
    List<Tenant> findTenantsApproachingLimits();

    /**
     * Find recently created tenants
     */
    @Query("SELECT t FROM Tenant t WHERE t.createdAt >= :cutoffDate ORDER BY t.createdAt DESC")
    List<Tenant> findRecentlyCreatedTenants(@Param("cutoffDate") LocalDateTime cutoffDate);
}
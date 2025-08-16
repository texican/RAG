package com.enterprise.rag.auth.repository;

import com.enterprise.rag.shared.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("SELECT t FROM Tenant t WHERE t.slug = :slug AND t.status = 'ACTIVE'")
    Optional<Tenant> findActiveBySlug(@Param("slug") String slug);

    @Query("SELECT COUNT(t) FROM Tenant t WHERE t.status = 'ACTIVE'")
    long countActiveTenants();
}
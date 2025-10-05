package com.byo.rag.document.repository;

import com.byo.rag.shared.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for Tenant entity operations in the document service.
 * Provides database access for tenant lookups during document operations.
 *
 * @author Enterprise RAG Development Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {
}

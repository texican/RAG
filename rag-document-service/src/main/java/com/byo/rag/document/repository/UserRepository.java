package com.byo.rag.document.repository;

import com.byo.rag.shared.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity operations in the document service.
 * Provides database access for user lookups during document operations.
 *
 * @author Enterprise RAG Development Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by their email address and tenant ID.
     *
     * @param email the user's email address
     * @param tenantId the tenant ID
     * @return an Optional containing the user if found, empty otherwise
     */
    Optional<User> findByEmailAndTenantId(String email, UUID tenantId);
}

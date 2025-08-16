package com.enterprise.rag.auth.repository;

import com.enterprise.rag.shared.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.status = 'ACTIVE'")
    Optional<User> findActiveByEmail(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE u.tenant.id = :tenantId")
    Page<User> findByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.tenant.id = :tenantId AND u.status = 'ACTIVE'")
    Page<User> findActiveByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.tenant.id = :tenantId")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    Optional<User> findByEmailVerificationToken(String token);
}
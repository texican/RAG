package com.enterprise.rag.document.repository;

import com.enterprise.rag.shared.entity.DocumentChunk;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {

    @Query("SELECT dc FROM DocumentChunk dc WHERE dc.document.id = :documentId ORDER BY dc.sequenceNumber")
    List<DocumentChunk> findByDocumentIdOrderBySequenceNumber(@Param("documentId") UUID documentId);

    @Query("SELECT dc FROM DocumentChunk dc WHERE dc.tenant.id = :tenantId")
    Page<DocumentChunk> findByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT COUNT(dc) FROM DocumentChunk dc WHERE dc.document.id = :documentId")
    long countByDocumentId(@Param("documentId") UUID documentId);

    @Query("SELECT COUNT(dc) FROM DocumentChunk dc WHERE dc.tenant.id = :tenantId")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT dc FROM DocumentChunk dc WHERE dc.embeddingVectorId IS NULL AND dc.tenant.id = :tenantId")
    List<DocumentChunk> findChunksWithoutEmbeddings(@Param("tenantId") UUID tenantId, Pageable pageable);

    void deleteByDocumentId(UUID documentId);
}
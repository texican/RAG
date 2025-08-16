package com.enterprise.rag.shared.repository;

import com.enterprise.rag.shared.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {

    List<DocumentChunk> findByTenant_Id(UUID tenantId);
    
    List<DocumentChunk> findByDocument_Id(UUID documentId);
    
    List<DocumentChunk> findByTenant_IdAndContentContainingIgnoreCase(UUID tenantId, String content);
    
    @Query("SELECT dc FROM DocumentChunk dc WHERE dc.tenant.id = :tenantId ORDER BY dc.sequenceNumber")
    List<DocumentChunk> findByTenantIdOrderBySequenceNumber(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT dc FROM DocumentChunk dc WHERE dc.document.id = :documentId ORDER BY dc.sequenceNumber")
    List<DocumentChunk> findByDocumentIdOrderBySequenceNumber(@Param("documentId") UUID documentId);
    
    void deleteByTenant_Id(UUID tenantId);
    
    void deleteByDocument_Id(UUID documentId);
}
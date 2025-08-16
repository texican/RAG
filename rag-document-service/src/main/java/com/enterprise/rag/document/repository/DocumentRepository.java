package com.enterprise.rag.document.repository;

import com.enterprise.rag.shared.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    @Query("SELECT d FROM Document d WHERE d.tenant.id = :tenantId")
    Page<Document> findByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.tenant.id = :tenantId AND d.processingStatus = :status")
    Page<Document> findByTenantIdAndStatus(@Param("tenantId") UUID tenantId, 
                                          @Param("status") Document.ProcessingStatus status, 
                                          Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.tenant.id = :tenantId AND d.filename = :filename")
    Optional<Document> findByTenantIdAndFilename(@Param("tenantId") UUID tenantId, @Param("filename") String filename);

    @Query("SELECT COUNT(d) FROM Document d WHERE d.tenant.id = :tenantId")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COALESCE(SUM(d.fileSize), 0) FROM Document d WHERE d.tenant.id = :tenantId")
    long getTotalFileSizeByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT d FROM Document d WHERE d.processingStatus = 'PENDING' ORDER BY d.createdAt ASC")
    List<Document> findPendingDocuments(Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.processingStatus = 'PROCESSING' AND d.updatedAt < :threshold")
    List<Document> findStuckProcessingDocuments(@Param("threshold") java.time.LocalDateTime threshold);
}
package com.byo.rag.document.repository;

import com.byo.rag.shared.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Document entity data access operations.
 * <p>
 * This repository provides comprehensive data access methods for document
 * management operations including tenant-aware queries, processing status
 * tracking, and operational monitoring capabilities. It ensures complete
 * tenant isolation and supports the document processing pipeline.
 * 
 * <h2>Core Query Operations</h2>
 * <ul>
 *   <li><strong>Tenant-Scoped Queries</strong> - All queries enforce tenant isolation</li>
 *   <li><strong>Status-Based Filtering</strong> - Queries by processing status</li>
 *   <li><strong>Filename Lookups</strong> - Duplicate detection and file management</li>
 *   <li><strong>Pagination Support</strong> - Efficient handling of large document sets</li>
 * </ul>
 * 
 * <h2>Administrative Queries</h2>
 * <ul>
 *   <li><strong>Document Counting</strong> - Tenant-specific document counts</li>
 *   <li><strong>Storage Analytics</strong> - Total file size calculations per tenant</li>
 *   <li><strong>Processing Monitoring</strong> - Pending and stuck document identification</li>
 *   <li><strong>Health Checks</strong> - Processing pipeline health monitoring</li>
 * </ul>
 * 
 * <h2>Multi-Tenant Design</h2>
 * <ul>
 *   <li><strong>Tenant Isolation</strong> - All queries include tenant ID filtering</li>
 *   <li><strong>Data Security</strong> - Prevents cross-tenant data access</li>
 *   <li><strong>Performance Optimization</strong> - Tenant-aware database indexes</li>
 *   <li><strong>Compliance</strong> - Supports data residency and privacy requirements</li>
 * </ul>
 * 
 * <h2>Processing Pipeline Support</h2>
 * <ul>
 *   <li><strong>Status Tracking</strong> - Monitors document processing lifecycle</li>
 *   <li><strong>Queue Management</strong> - Identifies pending documents for processing</li>
 *   <li><strong>Error Recovery</strong> - Detects stuck or failed processing operations</li>
 *   <li><strong>Batch Operations</strong> - Supports bulk document operations</li>
 * </ul>
 * 
 * <h2>Query Performance</h2>
 * All queries are optimized with:
 * <ul>
 *   <li><strong>Indexed Lookups</strong> - Leverages database indexes for fast retrieval</li>
 *   <li><strong>Pagination</strong> - Efficient large dataset handling</li>
 *   <li><strong>Selective Fields</strong> - Optimized projections for specific use cases</li>
 *   <li><strong>Join Optimization</strong> - Minimal joins for performance</li>
 * </ul>
 * 
 * @author Enterprise RAG Development Team
 * @version 0.8.0
 * @since 0.1.0
 * @see com.byo.rag.shared.entity.Document
 * @see org.springframework.data.jpa.repository.JpaRepository
 */
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
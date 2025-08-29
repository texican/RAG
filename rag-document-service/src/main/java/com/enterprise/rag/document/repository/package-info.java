/**
 * Data access repositories for document management operations.
 * 
 * <p>This package contains Spring Data JPA repositories that provide
 * comprehensive data access operations for document management in the
 * Enterprise RAG System. Repositories handle document metadata, processing
 * status, chunk management, and provide optimized queries for document
 * search and retrieval operations with complete multi-tenant isolation.</p>
 * 
 * <h2>Repository Architecture</h2>
 * <p>Document repositories extend shared base repositories with specialized functionality:</p>
 * <ul>
 *   <li><strong>Document Repository</strong> - Core document metadata and lifecycle management</li>
 *   <li><strong>Document Chunk Repository</strong> - Text chunk storage and retrieval</li>
 *   <li><strong>Processing Status Repository</strong> - Document processing status tracking</li>
 *   <li><strong>Document Metadata Repository</strong> - Extended metadata and properties</li>
 *   <li><strong>Document Version Repository</strong> - Document version control and history</li>
 * </ul>
 * 
 * <h2>Multi-Tenant Document Access</h2>
 * <p>All repositories enforce strict tenant isolation:</p>
 * <ul>
 *   <li><strong>Automatic Tenant Filtering</strong> - All queries automatically include tenant ID filtering</li>
 *   <li><strong>Cross-Tenant Prevention</strong> - Impossible to access documents across tenant boundaries</li>
 *   <li><strong>Tenant Validation</strong> - Automatic tenant existence and access validation</li>
 *   <li><strong>Tenant Analytics</strong> - Built-in tenant document usage metrics</li>
 * </ul>
 * 
 * <h2>Document Repository Features</h2>
 * <p>Comprehensive document metadata and lifecycle operations:</p>
 * <ul>
 *   <li><strong>Document CRUD Operations</strong> - Complete document lifecycle management</li>
 *   <li><strong>Processing Status Queries</strong> - Document processing status tracking</li>
 *   <li><strong>Content Search</strong> - Full-text search within document content</li>
 *   <li><strong>Metadata Queries</strong> - Search by document properties and metadata</li>
 *   <li><strong>Batch Operations</strong> - Efficient bulk document operations</li>
 * </ul>
 * 
 * <h2>Document Chunk Repository</h2>
 * <p>Specialized operations for document text chunks:</p>
 * <ul>
 *   <li><strong>Chunk Storage</strong> - Efficient storage and retrieval of text chunks</li>
 *   <li><strong>Chunk Search</strong> - Search within document chunks</li>
 *   <li><strong>Chunk Analytics</strong> - Chunk statistics and quality metrics</li>
 *   <li><strong>Chunk Optimization</strong> - Queries optimized for embedding operations</li>
 *   <li><strong>Hierarchical Chunks</strong> - Support for hierarchical chunk structures</li>
 * </ul>
 * 
 * <h2>Advanced Query Capabilities</h2>
 * <p>Repositories include sophisticated querying for document operations:</p>
 * <ul>
 *   <li><strong>Dynamic Search</strong> - JPA Criteria API for flexible document search</li>
 *   <li><strong>Full-Text Search</strong> - Database full-text search capabilities</li>
 *   <li><strong>Metadata Filtering</strong> - Complex metadata-based filtering</li>
 *   <li><strong>Processing Analytics</strong> - Document processing statistics and metrics</li>
 *   <li><strong>Content Analytics</strong> - Document content analysis and insights</li>
 * </ul>
 * 
 * <h2>Performance Optimizations</h2>
 * <p>Repositories are optimized for document processing workloads:</p>
 * <ul>
 *   <li><strong>Index Strategy</strong> - Strategic database indexing for document queries</li>
 *   <li><strong>Query Optimization</strong> - JPQL and native SQL optimization for large documents</li>
 *   <li><strong>Entity Graphs</strong> - Efficient relationship loading for document hierarchies</li>
 *   <li><strong>Batch Processing</strong> - Optimized bulk operations for document processing</li>
 *   <li><strong>Caching Integration</strong> - Repository-level caching for frequently accessed data</li>
 * </ul>
 * 
 * <h2>Document Processing Queries</h2>
 * <p>Specialized queries for document processing operations:</p>
 * <ul>
 *   <li><strong>Processing Queue</strong> - Queries for document processing queue management</li>
 *   <li><strong>Status Tracking</strong> - Processing status monitoring and updates</li>
 *   <li><strong>Error Tracking</strong> - Processing error logging and retrieval</li>
 *   <li><strong>Retry Management</strong> - Failed processing retry queue management</li>
 *   <li><strong>Completion Tracking</strong> - Processing completion statistics</li>
 * </ul>
 * 
 * <h2>Content and Metadata Queries</h2>
 * <p>Advanced content and metadata access patterns:</p>
 * <ul>
 *   <li><strong>Content Retrieval</strong> - Efficient document content retrieval</li>
 *   <li><strong>Chunk Retrieval</strong> - Document chunk access with pagination</li>
 *   <li><strong>Metadata Search</strong> - Metadata-based document discovery</li>
 *   <li><strong>Version Management</strong> - Document version tracking and retrieval</li>
 * </ul>
 * 
 * <h2>Analytics and Reporting Queries</h2>
 * <p>Specialized queries for document analytics and reporting:</p>
 * <ul>
 *   <li><strong>Usage Statistics</strong> - Document usage and access patterns</li>
 *   <li><strong>Processing Metrics</strong> - Document processing performance metrics</li>
 *   <li><strong>Quality Metrics</strong> - Document and processing quality statistics</li>
 *   <li><strong>Tenant Analytics</strong> - Per-tenant document analytics</li>
 *   <li><strong>Trend Analysis</strong> - Document processing trends and patterns</li>
 * </ul>
 * 
 * <h2>Transaction Management</h2>
 * <p>Proper transaction handling for document operations:</p>
 * <ul>
 *   <li><strong>ACID Compliance</strong> - Full ACID compliance for document operations</li>
 *   <li><strong>Isolation Levels</strong> - Appropriate isolation for concurrent processing</li>
 *   <li><strong>Rollback Strategies</strong> - Proper rollback for processing failures</li>
 *   <li><strong>Distributed Transactions</strong> - Support for distributed transaction coordination</li>
 * </ul>
 * 
 * <h2>Audit and Compliance</h2>
 * <p>Comprehensive audit and compliance support:</p>
 * <ul>
 *   <li><strong>Change Tracking</strong> - Automatic tracking of all document changes</li>
 *   <li><strong>Access Logging</strong> - Complete audit trails for document access</li>
 *   <li><strong>Compliance Queries</strong> - Specialized queries for regulatory compliance</li>
 *   <li><strong>Data Retention</strong> - Automated data retention and purging</li>
 * </ul>
 * 
 * <h2>Integration Features</h2>
 * <p>Support for integration with document processing pipeline:</p>
 * <ul>
 *   <li><strong>Event Integration</strong> - Database triggers for event publishing</li>
 *   <li><strong>Bulk Operations</strong> - Efficient bulk document import and export</li>
 *   <li><strong>Migration Support</strong> - Database schema migration and upgrades</li>
 *   <li><strong>Backup Integration</strong> - Document metadata backup and recovery</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Repository
 * @Transactional(readOnly = true)
 * public interface DocumentRepository extends JpaRepository<Document, String>, 
 *                                            JpaSpecificationExecutor<Document> {
 *     
 *     // Basic document queries
 *     @Query("SELECT d FROM Document d WHERE d.tenantId = :tenantId AND d.status = :status")
 *     List<Document> findByTenantIdAndStatus(
 *         @Param("tenantId") String tenantId,
 *         @Param("status") ProcessingStatus status
 *     );
 *     
 *     // Document processing queue
 *     @Query("""
 *         SELECT d FROM Document d 
 *         WHERE d.tenantId = :tenantId 
 *         AND d.status = 'PENDING' 
 *         ORDER BY d.createdAt ASC
 *     """)
 *     List<Document> findPendingDocuments(
 *         @Param("tenantId") String tenantId,
 *         Pageable pageable
 *     );
 *     
 *     // Document analytics
 *     @Query("""
 *         SELECT d.contentType, COUNT(d), AVG(d.characterCount), SUM(d.fileSize)
 *         FROM Document d
 *         WHERE d.tenantId = :tenantId
 *         AND d.createdAt >= :startDate
 *         GROUP BY d.contentType
 *         ORDER BY COUNT(d) DESC
 *     """)
 *     List<Object[]> getDocumentStatistics(
 *         @Param("tenantId") String tenantId,
 *         @Param("startDate") LocalDateTime startDate
 *     );
 *     
 *     // Full-text search
 *     @Query(value = """
 *         SELECT d.* FROM documents d
 *         WHERE d.tenant_id = :tenantId
 *         AND (
 *             to_tsvector('english', d.extracted_text) @@ plainto_tsquery('english', :query)
 *             OR LOWER(d.file_name) LIKE LOWER(CONCAT('%', :query, '%'))
 *         )
 *         ORDER BY ts_rank(to_tsvector('english', d.extracted_text), plainto_tsquery('english', :query)) DESC
 *     """, nativeQuery = true)
 *     List<Document> searchDocuments(
 *         @Param("tenantId") String tenantId,
 *         @Param("query") String query,
 *         Pageable pageable
 *     );
 *     
 *     // Processing metrics
 *     @Query("""
 *         SELECT 
 *             COUNT(CASE WHEN d.status = 'COMPLETED' THEN 1 END) as completed,
 *             COUNT(CASE WHEN d.status = 'PROCESSING' THEN 1 END) as processing,
 *             COUNT(CASE WHEN d.status = 'FAILED' THEN 1 END) as failed,
 *             AVG(CASE WHEN d.status = 'COMPLETED' 
 *                 THEN EXTRACT(EPOCH FROM (d.processingCompletedAt - d.processingStartedAt)) 
 *                 END) as avgProcessingTime
 *         FROM Document d
 *         WHERE d.tenantId = :tenantId
 *         AND d.createdAt >= :startDate
 *     """)
 *     DocumentProcessingMetrics getProcessingMetrics(
 *         @Param("tenantId") String tenantId,
 *         @Param("startDate") LocalDateTime startDate
 *     );
 * }
 * 
 * @Repository
 * @Transactional(readOnly = true)
 * public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, String> {
 *     
 *     // Chunk retrieval by document
 *     @Query("SELECT c FROM DocumentChunk c WHERE c.document.tenantId = :tenantId AND c.document.id = :documentId ORDER BY c.chunkIndex")
 *     List<DocumentChunk> findByDocumentId(
 *         @Param("tenantId") String tenantId,
 *         @Param("documentId") String documentId
 *     );
 *     
 *     // Chunk search
 *     @Query(value = """
 *         SELECT c.* FROM document_chunks c
 *         JOIN documents d ON c.document_id = d.id
 *         WHERE d.tenant_id = :tenantId
 *         AND to_tsvector('english', c.content) @@ plainto_tsquery('english', :query)
 *         ORDER BY ts_rank(to_tsvector('english', c.content), plainto_tsquery('english', :query)) DESC
 *     """, nativeQuery = true)
 *     List<DocumentChunk> searchChunks(
 *         @Param("tenantId") String tenantId,
 *         @Param("query") String query,
 *         Pageable pageable
 *     );
 *     
 *     // Chunk statistics
 *     @Query("SELECT COUNT(c), AVG(LENGTH(c.content)), MIN(LENGTH(c.content)), MAX(LENGTH(c.content)) FROM DocumentChunk c JOIN c.document d WHERE d.tenantId = :tenantId")
 *     Object[] getChunkStatistics(@Param("tenantId") String tenantId);
 * }
 * }</pre>
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 * @see org.springframework.data.jpa.repository Spring Data JPA
 * @see org.springframework.data.jpa.repository.JpaSpecificationExecutor Dynamic queries
 * @see com.enterprise.rag.shared.entity Document entities
 * @see javax.persistence.criteria JPA Criteria API
 */
package com.enterprise.rag.document.repository;
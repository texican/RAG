/**
 * Base repository interfaces and common query patterns.
 * 
 * <p>This package contains base repository interfaces that provide common
 * data access patterns for all entities in the Enterprise RAG System.
 * All repositories implement multi-tenant data isolation and provide
 * optimized query methods for high-performance data access.</p>
 * 
 * <h2>Repository Architecture</h2>
 * <p>The repository layer follows Spring Data JPA patterns:</p>
 * <ul>
 *   <li><strong>Base Repository</strong> - Common CRUD operations with tenant isolation</li>
 *   <li><strong>Tenant-Aware Queries</strong> - All queries automatically filter by tenant ID</li>
 *   <li><strong>Custom Query Methods</strong> - Domain-specific finder methods</li>
 *   <li><strong>Pagination Support</strong> - Built-in pagination and sorting capabilities</li>
 *   <li><strong>Specification Support</strong> - Dynamic query building with JPA Criteria API</li>
 * </ul>
 * 
 * <h2>Multi-Tenant Data Access</h2>
 * <p>All repository operations enforce tenant isolation:</p>
 * <ul>
 *   <li><strong>Automatic Filtering</strong> - Base repository automatically adds tenant ID to queries</li>
 *   <li><strong>Security Enforcement</strong> - No cross-tenant data access possible</li>
 *   <li><strong>Index Optimization</strong> - Composite indexes on (tenant_id, business_key)</li>
 *   <li><strong>Performance Tuning</strong> - Query optimization for tenant-partitioned data</li>
 * </ul>
 * 
 * <h2>Query Optimization Features</h2>
 * <p>Repositories are optimized for high-performance operations:</p>
 * <ul>
 *   <li><strong>Custom Queries</strong> - @Query annotations with optimized JPQL/native SQL</li>
 *   <li><strong>Entity Graphs</strong> - @EntityGraph for efficient relationship loading</li>
 *   <li><strong>Projection Support</strong> - Interface-based projections for lightweight queries</li>
 *   <li><strong>Batch Operations</strong> - Support for bulk updates and deletes</li>
 *   <li><strong>Caching Integration</strong> - @Cacheable annotations for frequently accessed data</li>
 * </ul>
 * 
 * <h2>Common Query Patterns</h2>
 * <p>Base repositories provide standard query patterns:</p>
 * <ul>
 *   <li><strong>Find by Tenant</strong> - findAllByTenantId(String tenantId, Pageable pageable)</li>
 *   <li><strong>Active Records</strong> - findActiveByTenantId(String tenantId) (deletedAt IS NULL)</li>
 *   <li><strong>Search and Filter</strong> - findByTenantIdAndFieldContaining(String tenantId, String field)</li>
 *   <li><strong>Date Range Queries</strong> - findByTenantIdAndCreatedAtBetween(String tenantId, LocalDateTime start, LocalDateTime end)</li>
 *   <li><strong>Status Filtering</strong> - findByTenantIdAndStatus(String tenantId, StatusEnum status)</li>
 * </ul>
 * 
 * <h2>Transaction Management</h2>
 * <p>Repository operations support proper transaction handling:</p>
 * <ul>
 *   <li><strong>Read-Only Transactions</strong> - Query methods marked with @Transactional(readOnly = true)</li>
 *   <li><strong>Isolation Levels</strong> - Appropriate isolation levels for data consistency</li>
 *   <li><strong>Rollback Rules</strong> - Custom rollback rules for business exceptions</li>
 *   <li><strong>Propagation Settings</strong> - Proper transaction propagation for nested calls</li>
 * </ul>
 * 
 * <h2>Performance Monitoring</h2>
 * <p>Repositories include comprehensive monitoring:</p>
 * <ul>
 *   <li><strong>Query Metrics</strong> - Micrometer metrics for query execution times</li>
 *   <li><strong>Slow Query Detection</strong> - Automatic logging of slow database operations</li>
 *   <li><strong>Connection Pool Monitoring</strong> - Database connection usage tracking</li>
 *   <li><strong>Cache Hit Rates</strong> - Monitoring of repository-level caching effectiveness</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Repository
 * public interface DocumentRepository extends BaseRepository<Document, String> {
 *     
 *     @Query("SELECT d FROM Document d WHERE d.tenantId = :tenantId AND d.status = :status")
 *     @EntityGraph(attributePaths = {"chunks"})
 *     List<Document> findByTenantIdAndStatus(
 *         @Param("tenantId") String tenantId, 
 *         @Param("status") ProcessingStatus status
 *     );
 *     
 *     @Query("SELECT COUNT(d) FROM Document d WHERE d.tenantId = :tenantId AND d.createdAt >= :startDate")
 *     long countRecentDocuments(
 *         @Param("tenantId") String tenantId, 
 *         @Param("startDate") LocalDateTime startDate
 *     );
 * }
 * }</pre>
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 * @see org.springframework.data.jpa.repository.JpaRepository Spring Data JPA base interface
 * @see org.springframework.data.jpa.repository.Query Custom query annotation
 * @see com.byo.rag.shared.entity Entity classes for repository operations
 */
package com.byo.rag.shared.repository;
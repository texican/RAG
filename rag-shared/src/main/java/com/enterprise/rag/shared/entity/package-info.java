/**
 * Core JPA entity classes for the Enterprise RAG System.
 * 
 * <p>This package contains the foundational JPA entities that define the data model
 * for the Enterprise RAG System. All entities are designed with multi-tenant
 * architecture in mind, providing automatic tenant isolation and comprehensive
 * audit logging capabilities.</p>
 * 
 * <h2>Entity Architecture</h2>
 * <p>The entity layer follows enterprise JPA patterns:</p>
 * <ul>
 *   <li><strong>Base Entity</strong> - Common fields for ID, timestamps, and tenant isolation</li>
 *   <li><strong>Tenant Entity</strong> - Multi-tenant organization management</li>
 *   <li><strong>User Entity</strong> - User accounts with role-based access control</li>
 *   <li><strong>Document Entity</strong> - File metadata and processing status</li>
 *   <li><strong>Document Chunk</strong> - Text segments for vector processing</li>
 *   <li><strong>Conversation Entity</strong> - Chat session management</li>
 * </ul>
 * 
 * <h2>Multi-Tenant Design</h2>
 * <p>All entities implement tenant isolation through:</p>
 * <ul>
 *   <li><strong>Tenant ID Column</strong> - Every entity includes a tenantId field</li>
 *   <li><strong>Database Constraints</strong> - Foreign key relationships respect tenant boundaries</li>
 *   <li><strong>Index Strategy</strong> - Composite indexes on (tenantId, businessKey) for performance</li>
 *   <li><strong>Query Filtering</strong> - Automatic tenant filtering in repository queries</li>
 * </ul>
 * 
 * <h2>Audit Trail Features</h2>
 * <p>All entities inherit comprehensive audit capabilities:</p>
 * <ul>
 *   <li><strong>Creation Tracking</strong> - Automatic createdAt and createdBy fields</li>
 *   <li><strong>Modification Tracking</strong> - Automatic updatedAt and updatedBy fields</li>
 *   <li><strong>Version Control</strong> - Optimistic locking with @Version annotation</li>
 *   <li><strong>Soft Deletion</strong> - Logical deletion with deletedAt field</li>
 * </ul>
 * 
 * <h2>Database Mapping Strategy</h2>
 * <p>Entity mapping follows these conventions:</p>
 * <ul>
 *   <li><strong>Naming Convention</strong> - Snake_case table and column names</li>
 *   <li><strong>Primary Keys</strong> - UUID-based identifiers for distributed systems</li>
 *   <li><strong>Foreign Keys</strong> - Explicit foreign key constraints with proper cascading</li>
 *   <li><strong>Indexes</strong> - Strategic indexing for query performance</li>
 *   <li><strong>Data Types</strong> - PostgreSQL-optimized column types</li>
 * </ul>
 * 
 * <h2>Validation and Constraints</h2>
 * <p>All entities include comprehensive validation:</p>
 * <ul>
 *   <li><strong>Bean Validation</strong> - JSR-303 annotations for field validation</li>
 *   <li><strong>Custom Validators</strong> - Business logic validation for complex rules</li>
 *   <li><strong>Database Constraints</strong> - NOT NULL, UNIQUE, and CHECK constraints</li>
 *   <li><strong>Length Limits</strong> - Appropriate field sizes for storage optimization</li>
 * </ul>
 * 
 * <h2>Performance Optimizations</h2>
 * <p>Entities are optimized for high-performance operations:</p>
 * <ul>
 *   <li><strong>Lazy Loading</strong> - Strategic use of FetchType.LAZY for large collections</li>
 *   <li><strong>Query Optimization</strong> - Proper use of @Query and @EntityGraph annotations</li>
 *   <li><strong>Caching Strategy</strong> - @Cacheable annotations on frequently accessed entities</li>
 *   <li><strong>Batch Processing</strong> - Support for bulk operations and batch inserts</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Service
 * public class DocumentService {
 *     
 *     @Transactional
 *     public Document createDocument(String tenantId, DocumentCreateRequest request) {
 *         Document document = new Document();
 *         document.setTenantId(tenantId);
 *         document.setFileName(request.getFileName());
 *         document.setContentType(request.getContentType());
 *         document.setStatus(ProcessingStatus.PENDING);
 *         
 *         return documentRepository.save(document);
 *     }
 * }
 * }</pre>
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 * @see javax.persistence JPA annotations and interfaces
 * @see org.springframework.data.jpa Spring Data JPA integration
 * @see com.enterprise.rag.shared.repository Repository interfaces for these entities
 */
package com.enterprise.rag.shared.entity;
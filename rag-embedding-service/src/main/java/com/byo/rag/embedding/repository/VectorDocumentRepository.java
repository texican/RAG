package com.byo.rag.embedding.repository;

import com.byo.rag.embedding.entity.VectorDocument;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for VectorDocument entities.
 * 
 * <p>This repository provides data access operations for vector documents stored
 * in Redis. It supports standard CRUD operations as well as custom queries for
 * multi-tenant vector search and management.</p>
 * 
 * <p><strong>Query Capabilities:</strong></p>
 * <ul>
 *   <li><strong>Tenant Isolation:</strong> Find vectors by tenant for complete data separation</li>
 *   <li><strong>Model Filtering:</strong> Query vectors by embedding model for consistency</li>
 *   <li><strong>Document Scope:</strong> Retrieve vectors for specific documents</li>
 *   <li><strong>Batch Operations:</strong> Efficient multi-record operations</li>
 * </ul>
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 */
@Repository
public interface VectorDocumentRepository extends CrudRepository<VectorDocument, String> {
    
    /**
     * Find all vector documents for a specific tenant.
     *
     * @param tenantId The tenant identifier
     * @return List of vector documents for the tenant
     */
    List<VectorDocument> findByTenantId(UUID tenantId);
    
    /**
     * Find vector documents by tenant and model.
     *
     * @param tenantId The tenant identifier
     * @param modelName The embedding model name
     * @return List of vector documents matching the criteria
     */
    List<VectorDocument> findByTenantIdAndModelName(UUID tenantId, String modelName);
    
    /**
     * Find vector documents for a specific document.
     *
     * @param tenantId The tenant identifier
     * @param documentId The document identifier
     * @return List of vector documents for the document
     */
    List<VectorDocument> findByTenantIdAndDocumentId(UUID tenantId, UUID documentId);
    
    /**
     * Find a specific vector document by chunk.
     *
     * @param tenantId The tenant identifier
     * @param chunkId The chunk identifier
     * @param modelName The embedding model name
     * @return The vector document if found
     */
    VectorDocument findByTenantIdAndChunkIdAndModelName(UUID tenantId, UUID chunkId, String modelName);
    
    /**
     * Delete all vector documents for a tenant.
     *
     * @param tenantId The tenant identifier
     */
    void deleteByTenantId(UUID tenantId);
    
    /**
     * Delete vector documents by tenant and model.
     *
     * @param tenantId The tenant identifier
     * @param modelName The embedding model name
     */
    void deleteByTenantIdAndModelName(UUID tenantId, String modelName);
    
    /**
     * Delete vector documents for a specific document.
     *
     * @param tenantId The tenant identifier
     * @param documentId The document identifier
     */
    void deleteByTenantIdAndDocumentId(UUID tenantId, UUID documentId);
    
    /**
     * Count vector documents for a tenant.
     *
     * @param tenantId The tenant identifier
     * @return Number of vector documents for the tenant
     */
    long countByTenantId(UUID tenantId);
    
    /**
     * Count vector documents by tenant and model.
     *
     * @param tenantId The tenant identifier
     * @param modelName The embedding model name
     * @return Number of vector documents matching the criteria
     */
    long countByTenantIdAndModelName(UUID tenantId, String modelName);
}
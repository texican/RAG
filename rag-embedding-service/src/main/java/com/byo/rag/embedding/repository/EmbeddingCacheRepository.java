package com.byo.rag.embedding.repository;

import com.byo.rag.embedding.entity.EmbeddingCache;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for EmbeddingCache entities.
 * 
 * <p>This repository manages the Redis-based cache for embedding vectors,
 * providing efficient storage and retrieval operations for performance
 * optimization. It supports TTL-based expiration and tenant isolation.</p>
 * 
 * <p><strong>Cache Operations:</strong></p>
 * <ul>
 *   <li><strong>Cache Lookup:</strong> Find cached embeddings by text hash and model</li>
 *   <li><strong>Tenant Management:</strong> Cache operations scoped by tenant</li>
 *   <li><strong>Model Versioning:</strong> Model-specific cache entries</li>
 *   <li><strong>TTL Management:</strong> Automatic expiration handling</li>
 * </ul>
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 */
@Repository
public interface EmbeddingCacheRepository extends CrudRepository<EmbeddingCache, String> {
    
    /**
     * Find cached embedding by tenant, text hash, and model.
     *
     * @param tenantId The tenant identifier
     * @param textHash The hash of the text content
     * @param modelName The embedding model name
     * @return Optional cached embedding
     */
    Optional<EmbeddingCache> findByTenantIdAndTextHashAndModelName(
        UUID tenantId, String textHash, String modelName);
    
    /**
     * Find all cached embeddings for a tenant.
     *
     * @param tenantId The tenant identifier
     * @return List of cached embeddings for the tenant
     */
    List<EmbeddingCache> findByTenantId(UUID tenantId);
    
    /**
     * Find cached embeddings by model name.
     *
     * @param modelName The embedding model name
     * @return List of cached embeddings for the model
     */
    List<EmbeddingCache> findByModelName(String modelName);
    
    /**
     * Find cached embeddings by tenant and model.
     *
     * @param tenantId The tenant identifier
     * @param modelName The embedding model name
     * @return List of cached embeddings matching the criteria
     */
    List<EmbeddingCache> findByTenantIdAndModelName(UUID tenantId, String modelName);
    
    /**
     * Delete all cached embeddings for a tenant.
     *
     * @param tenantId The tenant identifier
     */
    void deleteByTenantId(UUID tenantId);
    
    /**
     * Delete cached embeddings by model name.
     *
     * @param modelName The embedding model name
     */
    void deleteByModelName(String modelName);
    
    /**
     * Delete cached embeddings by tenant and model.
     *
     * @param tenantId The tenant identifier
     * @param modelName The embedding model name
     */
    void deleteByTenantIdAndModelName(UUID tenantId, String modelName);
    
    /**
     * Count cached embeddings for a tenant.
     *
     * @param tenantId The tenant identifier
     * @return Number of cached embeddings for the tenant
     */
    long countByTenantId(UUID tenantId);
    
    /**
     * Count cached embeddings by model name.
     *
     * @param modelName The embedding model name
     * @return Number of cached embeddings for the model
     */
    long countByModelName(String modelName);
    
    /**
     * Count total cached embeddings.
     *
     * @return Total number of cached embeddings
     */
    long count();
}
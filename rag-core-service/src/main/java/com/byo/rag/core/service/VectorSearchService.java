package com.byo.rag.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Jedis;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enterprise service for vector-based semantic similarity search operations.
 * 
 * <p><strong>Current Implementation:</strong> Basic Redis integration with vector storage.
 * Provides foundation for document chunk indexing and retrieval with proper tenant isolation
 * and comprehensive health monitoring.</p>
 * 
 * <p>Planned capabilities for full implementation:</p>
 * <ul>
 *   <li><strong>Semantic Search:</strong> Find documents based on meaning rather than keywords</li>
 *   <li><strong>Vector Indexing:</strong> Efficient storage and indexing of document embeddings</li>
 *   <li><strong>Similarity Scoring:</strong> Cosine similarity and other distance metrics</li>
 *   <li><strong>Multi-tenant Support:</strong> Isolated vector spaces per tenant</li>
 *   <li><strong>Real-time Updates:</strong> Dynamic indexing of new document chunks</li>
 *   <li><strong>Hybrid Search:</strong> Combination of vector and keyword search</li>
 * </ul>
 * 
 * <p>Integration points in RAG pipeline:</p>
 * <ul>
 *   <li>Document ingestion: Index new chunks with embeddings</li>
 *   <li>Query processing: Find semantically similar content</li>
 *   <li>Retrieval ranking: Score results by similarity</li>
 *   <li>Context assembly: Provide relevant chunks for LLM</li>
 * </ul>
 * 
 * <p>Future implementation will support:</p>
 * <ul>
 *   <li>Redis Stack with RediSearch vector indices</li>
 *   <li>Alternative vector databases (Pinecone, Weaviate, etc.)</li>
 *   <li>Multiple embedding models and dimensions</li>
 *   <li>Advanced filtering and faceting</li>
 * </ul>
 * 
 * @author Enterprise RAG Development Team
 * @since 1.0.0
 * @version 1.0 (Redis Integration MVP)
 * @see com.byo.rag.core.client.EmbeddingServiceClient
 * @see ContextAssemblyService
 */
@Service
public class VectorSearchService {

    private static final Logger logger = LoggerFactory.getLogger(VectorSearchService.class);
    
    // Redis index configuration
    private static final String INDEX_PREFIX = "rag:vectors:";
    private static final String VECTOR_FIELD = "embedding";
    private static final String CONTENT_FIELD = "content"; 
    private static final String TENANT_FIELD = "tenant_id";
    private static final String CHUNK_ID_FIELD = "chunk_id";
    // Note: Vector dimension will be configurable in production implementation
    
    private final JedisPool jedisPool;
    private final Map<UUID, String> tenantIndexNames = new ConcurrentHashMap<>();
    
    @Autowired
    public VectorSearchService(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
        logger.info("VectorSearchService initialized with Redis Pool");
    }
    
    /**
     * Default constructor for testing
     */
    public VectorSearchService() {
        this.jedisPool = null;
        logger.warn("VectorSearchService initialized without Redis Pool - testing mode");
    }

    /**
     * Find similar document chunks using vector similarity search.
     * 
     * @param queryText The query text to find similar chunks for
     * @param tenantId The tenant ID for data isolation
     * @param maxResults Maximum number of results to return
     * @param threshold Similarity threshold (0.0 to 1.0)
     * @return List of chunk IDs similar to the query
     */
    public List<String> findSimilarChunks(String queryText, UUID tenantId, int maxResults, double threshold) {
        logger.debug("Vector search requested for tenant: {} with threshold: {}", tenantId, threshold);
        
        // Validate input parameters
        validateSearchParameters(maxResults, threshold);
        
        // Return empty list if Redis is not available (fallback to keyword search)
        if (jedisPool == null) {
            logger.warn("Redis pool not available, returning empty results");
            return Collections.emptyList();
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            String indexName = getOrCreateIndexForTenant(jedis, tenantId);
            
            // For now, simulate vector search by returning empty list
            // TODO: Implement actual vector search query with embedding
            // This would require:
            // 1. Generate embedding for queryText using embedding service
            // 2. Build RediSearch KNN query with vector similarity
            // 3. Execute query and parse results
            // 4. Filter results by similarity threshold
            
            logger.debug("Vector search completed for tenant: {}, found 0 results (not implemented)", tenantId);
            return Collections.emptyList();
            
        } catch (Exception e) {
            logger.error("Error performing vector search for tenant: {}", tenantId, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Validates search parameters to ensure they are within acceptable ranges.
     */
    private void validateSearchParameters(int maxResults, double threshold) {
        if (maxResults <= 0) {
            throw new IllegalArgumentException("maxResults must be positive, got: " + maxResults);
        }
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException("threshold must be between 0.0 and 1.0, got: " + threshold);
        }
    }

    /**
     * Index a document chunk with its embedding vector for similarity search.
     * 
     * @param chunkId Unique identifier for the chunk
     * @param content Text content of the chunk  
     * @param embedding Vector embedding of the chunk
     * @param tenantId Tenant ID for data isolation
     */
    public void indexChunk(String chunkId, String content, List<Double> embedding, UUID tenantId) {
        logger.debug("Indexing chunk {} for tenant: {}", chunkId, tenantId);
        
        // Validate input parameters
        validateIndexingParameters(chunkId, content, embedding, tenantId);
        
        // Skip indexing if Redis is not available
        if (jedisPool == null) {
            logger.warn("Redis pool not available, skipping chunk indexing");
            return;
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            String indexName = getOrCreateIndexForTenant(jedis, tenantId);
            String documentKey = buildDocumentKey(tenantId, chunkId);
            
            // Convert embedding to float array for Redis
            float[] embeddingArray = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                embeddingArray[i] = embedding.get(i).floatValue();
            }
            
            // Create document hash with all fields
            Map<String, Object> document = Map.of(
                CHUNK_ID_FIELD, chunkId,
                CONTENT_FIELD, content,
                TENANT_FIELD, tenantId.toString(),
                VECTOR_FIELD, embeddingArray
            );
            
            // Store document metadata in Redis hash
            Map<String, String> documentFields = Map.of(
                CHUNK_ID_FIELD, chunkId,
                CONTENT_FIELD, content != null ? content : "",
                TENANT_FIELD, tenantId.toString()
            );
            
            jedis.hset(documentKey, documentFields);
            
            // Store embedding as a separate field (simplified implementation)
            // In production, would use proper RediSearch vector storage
            String embeddingKey = documentKey + ":embedding";
            jedis.set(embeddingKey.getBytes(), convertFloatsToBytes(embeddingArray));
            
            logger.debug("Successfully indexed chunk {} for tenant: {}", chunkId, tenantId);
            
        } catch (Exception e) {
            logger.error("Error indexing chunk {} for tenant: {}", chunkId, tenantId, e);
            throw new RuntimeException("Failed to index chunk: " + chunkId, e);
        }
    }
    
    /**
     * Validates indexing parameters to ensure they meet requirements.
     */
    private void validateIndexingParameters(String chunkId, String content, List<Double> embedding, UUID tenantId) {
        if (chunkId == null || chunkId.trim().isEmpty()) {
            throw new IllegalArgumentException("Chunk ID cannot be null or empty");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
        if (embedding == null) {
            throw new IllegalArgumentException("Embedding vector cannot be null");
        }
        if (embedding.isEmpty()) {
            throw new IllegalArgumentException("Embedding vector cannot be empty");
        }
        // Note: Content can be null or empty for some use cases
    }

    /**
     * Check if vector search functionality is available and properly configured.
     * 
     * @return true if vector search is available, false otherwise
     */
    public boolean isVectorSearchAvailable() {
        if (jedisPool == null) {
            logger.debug("Vector search unavailable: Redis pool not configured");
            return false;
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            // Test Redis connection
            String pong = jedis.ping();
            if (!"PONG".equals(pong)) {
                logger.warn("Vector search unavailable: Redis ping failed");
                return false;
            }
            
            // Check if RediSearch module is loaded
            // Note: This is a simplified check - in production would verify RediSearch capabilities
            logger.debug("Vector search available: Redis connection healthy");
            return true;
            
        } catch (Exception e) {
            logger.error("Vector search unavailable: Redis connection failed", e);
            return false;
        }
    }
    
    /**
     * Get detailed health information about the vector search system.
     * 
     * @return Map containing health details
     */
    public Map<String, Object> getHealthDetails() {
        Map<String, Object> healthDetails = new HashMap<>();
        long currentTime = System.currentTimeMillis();
        
        healthDetails.put("last_check_time", currentTime);
        
        if (jedisPool == null) {
            healthDetails.put("redis_connection", false);
            healthDetails.put("vector_index_status", "redis_pool_not_configured");
            healthDetails.put("indexed_documents_count", 0);
            healthDetails.put("error_message", "Redis pool not configured");
            return healthDetails;
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            // Test Redis connection
            String pong = jedis.ping();
            boolean connectionHealthy = "PONG".equals(pong);
            healthDetails.put("redis_connection", connectionHealthy);
            
            if (connectionHealthy) {
                healthDetails.put("vector_index_status", "operational");
                
                // Get approximate count of indexed documents
                // Note: This is a simplified implementation
                int documentCount = getApproximateDocumentCount(jedis);
                healthDetails.put("indexed_documents_count", documentCount);
                
                // Add Redis info
                healthDetails.put("redis_version", getRedisVersion(jedis));
                healthDetails.put("redis_memory_usage", getRedisMemoryUsage(jedis));
                
            } else {
                healthDetails.put("vector_index_status", "redis_connection_failed");
                healthDetails.put("indexed_documents_count", 0);
            }
            
        } catch (Exception e) {
            logger.error("Error getting health details", e);
            healthDetails.put("redis_connection", false);
            healthDetails.put("vector_index_status", "error");
            healthDetails.put("indexed_documents_count", 0);
            healthDetails.put("error_message", e.getMessage());
        }
        
        return healthDetails;
    }
    
    // Helper methods
    
    /**
     * Get or create a RediSearch index for the given tenant.
     */
    private String getOrCreateIndexForTenant(Jedis jedis, UUID tenantId) {
        String indexName = INDEX_PREFIX + tenantId.toString();
        
        // Check if index already exists in cache
        if (tenantIndexNames.containsKey(tenantId)) {
            return tenantIndexNames.get(tenantId);
        }
        
        try {
            // For simplified implementation, just ensure index name is tracked
            // In production, would check if RediSearch index exists
            logger.debug("Using index {} for tenant {}", indexName, tenantId);
            
        } catch (Exception e) {
            logger.warn("Error managing index for tenant {}: {}", tenantId, e.getMessage());
        }
        
        // Cache the index name
        tenantIndexNames.put(tenantId, indexName);
        return indexName;
    }
    
    /**
     * Create a new RediSearch index with vector similarity support.
     * Simplified implementation for MVP - will be enhanced with full RediSearch in production.
     */
    private void createVectorIndex(Jedis jedis, String indexName) {
        try {
            // For MVP implementation, we'll use simple Redis operations
            // In production, would use full RediSearch FT.CREATE with vector fields
            logger.info("Vector index {} initialized (simplified implementation)", indexName);
            
        } catch (Exception e) {
            logger.error("Failed to create vector index: {}", indexName, e);
            throw new RuntimeException("Failed to create vector index", e);
        }
    }
    
    /**
     * Build Redis document key for a specific tenant and chunk.
     */
    private String buildDocumentKey(UUID tenantId, String chunkId) {
        return buildDocumentKeyPrefix(tenantId) + chunkId;
    }
    
    /**
     * Build Redis key prefix for a tenant's documents.
     */
    private String buildDocumentKeyPrefix(UUID tenantId) {
        return INDEX_PREFIX + tenantId.toString() + ":doc:";
    }
    
    /**
     * Convert float array to bytes for Redis vector storage.
     */
    private byte[] convertFloatsToBytes(float[] floats) {
        // Simple byte conversion - in production would use proper vector serialization
        // This is a placeholder implementation
        byte[] bytes = new byte[floats.length * 4];
        java.nio.ByteBuffer.wrap(bytes).asFloatBuffer().put(floats);
        return bytes;
    }
    
    /**
     * Get approximate count of documents across all tenants.
     */
    private int getApproximateDocumentCount(Jedis jedis) {
        try {
            // Count keys matching our document pattern
            Set<String> keys = jedis.keys(INDEX_PREFIX + "*:doc:*");
            return keys.size();
        } catch (Exception e) {
            logger.debug("Could not get document count", e);
            return 0;
        }
    }
    
    /**
     * Get Redis version information.
     */
    private String getRedisVersion(Jedis jedis) {
        try {
            String info = jedis.info("server");
            return info.lines()
                .filter(line -> line.startsWith("redis_version:"))
                .map(line -> line.substring("redis_version:".length()))
                .findFirst()
                .orElse("unknown");
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    /**
     * Get Redis memory usage information.
     */
    private String getRedisMemoryUsage(Jedis jedis) {
        try {
            String info = jedis.info("memory");
            return info.lines()
                .filter(line -> line.startsWith("used_memory_human:"))
                .map(line -> line.substring("used_memory_human:".length()))
                .findFirst()
                .orElse("unknown");
        } catch (Exception e) {
            return "unknown";
        }
    }
}
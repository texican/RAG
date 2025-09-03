package com.byo.rag.embedding.service;

import com.byo.rag.embedding.config.RedisConfig.VectorStorageProperties;
import com.byo.rag.embedding.dto.EmbeddingResponse.EmbeddingResult;
import com.byo.rag.embedding.dto.SearchRequest;
import com.byo.rag.embedding.dto.SearchResponse;
import com.byo.rag.embedding.dto.SearchResponse.SearchResult;
import com.byo.rag.shared.exception.EmbeddingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.*;

/**
 * Service for storing and retrieving vector embeddings in Redis.
 * Provides basic vector storage and similarity search functionality.
 */
@Service
public class VectorStorageService {

    private static final Logger logger = LoggerFactory.getLogger(VectorStorageService.class);
    
    private final JedisPool jedisPool;
    private final VectorStorageProperties properties;
    // Future: Track created indexes for cleanup
    
    public VectorStorageService(JedisPool jedisPool, VectorStorageProperties properties) {
        this.jedisPool = jedisPool;
        this.properties = properties;
    }
    
    /**
     * Store embedding vectors for a batch of chunks.
     */
    public void storeEmbeddings(UUID tenantId, String modelName, List<EmbeddingResult> results) {
        if (results.isEmpty()) {
            logger.warn("No embedding results to store for tenant: {}", tenantId);
            return;
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            for (EmbeddingResult result : results) {
                if (!"SUCCESS".equals(result.status())) {
                    continue; // Skip failed embeddings
                }
                
                String key = getVectorKey(tenantId, modelName, result.chunkId());
                
                // Prepare data for Redis hash
                Map<String, Object> fields = new HashMap<>();
                fields.put("tenant_id", tenantId.toString());
                fields.put("chunk_id", result.chunkId().toString());
                fields.put("model_name", modelName);
                fields.put("content", result.text());
                fields.put("created_at", System.currentTimeMillis());
                
                // Convert embedding to byte array for Redis
                byte[] vectorBytes = floatListToByteArray(result.embedding());
                fields.put("vector", vectorBytes);
                
                // Store in Redis - convert Object values to String for Jedis
                Map<String, String> stringFields = new HashMap<>();
                for (Map.Entry<String, Object> entry : fields.entrySet()) {
                    if (entry.getValue() instanceof byte[]) {
                        // Convert byte array to base64 string for storage
                        stringFields.put(entry.getKey(), 
                            java.util.Base64.getEncoder().encodeToString((byte[]) entry.getValue()));
                    } else {
                        stringFields.put(entry.getKey(), entry.getValue().toString());
                    }
                }
                jedis.hset(key, stringFields);
                
                // Add to tenant's vector index for search
                String indexKey = getTenantIndexKey(tenantId, modelName);
                jedis.sadd(indexKey, result.chunkId().toString());
                
                logger.debug("Stored vector for chunk: {} in tenant: {}", 
                           result.chunkId(), tenantId);
            }
            
        } catch (Exception e) {
            logger.error("Failed to store embeddings for tenant: {}", tenantId, e);
            throw new EmbeddingException("Failed to store embedding vectors", e);
        }
    }
    
    /**
     * Search for similar vectors using basic cosine similarity.
     * Note: This is a simplified implementation for demonstration.
     * In production, you would use a proper vector database like Redis Stack with RediSearch.
     */
    public SearchResponse searchSimilar(SearchRequest request, List<Float> queryEmbedding) {
        long startTime = System.currentTimeMillis();
        
        String modelName = getEffectiveModelName(request.modelName());
        
        try (Jedis jedis = jedisPool.getResource()) {
            String indexKey = getTenantIndexKey(request.tenantId(), modelName);
            Set<String> chunkIds = jedis.smembers(indexKey);
            
            if (chunkIds.isEmpty()) {
                logger.info("No vectors found for tenant: {} with model: {}", 
                           request.tenantId(), modelName);
                return SearchResponse.empty(request.tenantId(), request.query(), 
                                          modelName, System.currentTimeMillis() - startTime);
            }
            
            // Calculate similarities
            List<SearchResult> results = new ArrayList<>();
            
            for (String chunkIdStr : chunkIds) {
                UUID chunkId = UUID.fromString(chunkIdStr);
                String vectorKey = getVectorKey(request.tenantId(), modelName, chunkId);
                
                Map<String, String> vectorData = jedis.hgetAll(vectorKey);
                if (vectorData.isEmpty()) {
                    continue;
                }
                
                // Reconstruct vector from base64
                String vectorBase64 = vectorData.get("vector");
                if (vectorBase64 == null) {
                    continue;
                }
                
                byte[] vectorBytes = Base64.getDecoder().decode(vectorBase64);
                List<Float> storedVector = byteArrayToFloatList(vectorBytes);
                
                // Calculate cosine similarity
                double similarity = calculateCosineSimilarity(queryEmbedding, storedVector);
                
                if (similarity >= request.threshold()) {
                    SearchResult result = SearchResult.of(
                        chunkId,
                        UUID.randomUUID(), // TODO: Get actual document ID
                        request.includeContent() ? vectorData.get("content") : null,
                        similarity,
                        request.includeMetadata() ? Map.of("model", modelName) : null,
                        "Unknown Document",
                        "text"
                    );
                    results.add(result);
                }
            }
            
            // Sort by similarity score (descending) and limit results
            results.sort((a, b) -> Double.compare(b.score(), a.score()));
            if (results.size() > request.topK()) {
                results = results.subList(0, request.topK());
            }
            
            long searchTime = System.currentTimeMillis() - startTime;
            logger.info("Found {} similar vectors for tenant: {} in {}ms", 
                       results.size(), request.tenantId(), searchTime);
            
            return SearchResponse.success(request.tenantId(), request.query(), 
                                        modelName, results, searchTime);
            
        } catch (Exception e) {
            logger.error("Failed to search vectors for tenant: {}", request.tenantId(), e);
            return SearchResponse.empty(request.tenantId(), request.query(), 
                                      modelName, System.currentTimeMillis() - startTime);
        }
    }
    
    /**
     * Delete vectors for a tenant.
     */
    public void deleteVectors(UUID tenantId, String modelName) {
        try (Jedis jedis = jedisPool.getResource()) {
            String indexKey = getTenantIndexKey(tenantId, modelName);
            Set<String> chunkIds = jedis.smembers(indexKey);
            
            // Delete individual vector entries
            for (String chunkIdStr : chunkIds) {
                UUID chunkId = UUID.fromString(chunkIdStr);
                String vectorKey = getVectorKey(tenantId, modelName, chunkId);
                jedis.del(vectorKey);
            }
            
            // Delete the index
            jedis.del(indexKey);
            
            logger.info("Deleted {} vectors for tenant: {} with model: {}", 
                       chunkIds.size(), tenantId, modelName);
            
        } catch (Exception e) {
            logger.error("Failed to delete vectors for tenant: {}", tenantId, e);
            throw new EmbeddingException("Failed to delete vectors", e);
        }
    }
    
    /**
     * Get vector storage statistics.
     */
    public VectorStats getStats() {
        try (Jedis jedis = jedisPool.getResource()) {
            // Basic stats from Redis info
            String info = jedis.info("memory");
            long usedMemory = parseMemoryFromInfo(info);
            
            return new VectorStats(
                0L, // totalVectors - would need to count all keys
                usedMemory / (1024 * 1024), // memoryUsageMB
                0.0, // averageVectorSize - would need to calculate
                System.currentTimeMillis()
            );
            
        } catch (Exception e) {
            logger.error("Failed to get vector storage stats", e);
            return new VectorStats(0L, 0L, 0.0, System.currentTimeMillis());
        }
    }
    
    // Helper methods
    
    private String getVectorKey(UUID tenantId, String modelName, UUID chunkId) {
        return String.format("%s:vector:%s:%s:%s", 
                           properties.indexPrefix(), tenantId, modelName, chunkId);
    }
    
    private String getTenantIndexKey(UUID tenantId, String modelName) {
        return String.format("%s:index:%s:%s", 
                           properties.indexPrefix(), tenantId, modelName);
    }
    
    private String getEffectiveModelName(String requestedModel) {
        if (requestedModel == null || requestedModel.trim().isEmpty()) {
            return "default";
        }
        return requestedModel;
    }
    
    private byte[] floatListToByteArray(List<Float> floats) {
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(floats.size() * 4);
        for (Float f : floats) {
            buffer.putFloat(f);
        }
        return buffer.array();
    }
    
    private List<Float> byteArrayToFloatList(byte[] bytes) {
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(bytes);
        List<Float> floats = new ArrayList<>();
        while (buffer.hasRemaining()) {
            floats.add(buffer.getFloat());
        }
        return floats;
    }
    
    private double calculateCosineSimilarity(List<Float> vector1, List<Float> vector2) {
        if (vector1.size() != vector2.size()) {
            return 0.0;
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vector1.size(); i++) {
            dotProduct += vector1.get(i) * vector2.get(i);
            norm1 += vector1.get(i) * vector1.get(i);
            norm2 += vector2.get(i) * vector2.get(i);
        }
        
        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    private long parseMemoryFromInfo(String info) {
        // Simple parsing of used_memory from Redis INFO command
        String[] lines = info.split("\n");
        for (String line : lines) {
            if (line.startsWith("used_memory:")) {
                return Long.parseLong(line.split(":")[1].trim());
            }
        }
        return 0L;
    }
    
    /**
     * Vector storage statistics.
     */
    public record VectorStats(
        long totalVectors,
        long memoryUsageMB,
        double averageVectorSize,
        long lastUpdated
    ) {}
}
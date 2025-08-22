package com.enterprise.rag.embedding.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Service for caching embeddings to improve performance and reduce API calls.
 */
@Service
public class EmbeddingCacheService {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingCacheService.class);
    private static final String CACHE_PREFIX = "embedding:cache:";
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final Duration cacheTtl;
    
    public EmbeddingCacheService(
            RedisTemplate<String, Object> redisTemplate,
            @Value("${embedding.models.cache-ttl:3600}") int cacheTtlSeconds) {
        this.redisTemplate = redisTemplate;
        this.cacheTtl = Duration.ofSeconds(cacheTtlSeconds);
    }
    
    /**
     * Cache an embedding for a specific text and model.
     */
    public void cacheEmbedding(UUID tenantId, String text, String modelName, List<Float> embedding) {
        try {
            String cacheKey = generateCacheKey(tenantId, text, modelName);
            redisTemplate.opsForValue().set(cacheKey, embedding, cacheTtl);
            
            logger.debug("Cached embedding for tenant: {}, model: {}, text hash: {}", 
                        tenantId, modelName, hashText(text));
                        
        } catch (Exception e) {
            logger.warn("Failed to cache embedding for tenant: {}, model: {}", 
                       tenantId, modelName, e);
            // Don't fail the operation if caching fails
        }
    }
    
    /**
     * Retrieve cached embedding for a specific text and model.
     */
    public List<Float> getCachedEmbedding(UUID tenantId, String text, String modelName) {
        try {
            String cacheKey = generateCacheKey(tenantId, text, modelName);
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            
            if (cached instanceof List<?> list) {
                // Verify all elements are Float
                if (list.stream().allMatch(item -> item instanceof Number)) {
                    List<Float> embedding = list.stream()
                        .map(item -> ((Number) item).floatValue())
                        .toList();
                    
                    logger.debug("Retrieved cached embedding for tenant: {}, model: {}, text hash: {}", 
                                tenantId, modelName, hashText(text));
                    return embedding;
                }
            }
            
            return null;
            
        } catch (Exception e) {
            logger.warn("Failed to retrieve cached embedding for tenant: {}, model: {}", 
                       tenantId, modelName, e);
            return null;
        }
    }
    
    /**
     * Invalidate cached embeddings for a tenant.
     */
    public void invalidateTenantCache(UUID tenantId) {
        try {
            String pattern = CACHE_PREFIX + tenantId + ":*";
            var keys = redisTemplate.keys(pattern);
            
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                logger.info("Invalidated {} cached embeddings for tenant: {}", keys.size(), tenantId);
            }
            
        } catch (Exception e) {
            logger.error("Failed to invalidate cache for tenant: {}", tenantId, e);
        }
    }
    
    /**
     * Invalidate cached embeddings for a specific model.
     */
    public void invalidateModelCache(String modelName) {
        try {
            String pattern = CACHE_PREFIX + "*:" + modelName + ":*";
            var keys = redisTemplate.keys(pattern);
            
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                logger.info("Invalidated {} cached embeddings for model: {}", keys.size(), modelName);
            }
            
        } catch (Exception e) {
            logger.error("Failed to invalidate cache for model: {}", modelName, e);
        }
    }
    
    /**
     * Get cache statistics.
     */
    public CacheStats getCacheStats() {
        try {
            String pattern = CACHE_PREFIX + "*";
            var keys = redisTemplate.keys(pattern);
            long totalKeys = keys != null ? keys.size() : 0;
            
            return new CacheStats(totalKeys, cacheTtl.getSeconds());
            
        } catch (Exception e) {
            logger.error("Failed to get cache statistics", e);
            return new CacheStats(0, cacheTtl.getSeconds());
        }
    }
    
    private String generateCacheKey(UUID tenantId, String text, String modelName) {
        String textHash = hashText(text);
        return CACHE_PREFIX + tenantId + ":" + modelName + ":" + textHash;
    }
    
    private String hashText(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            // Use first 16 characters for reasonable key length
            return hexString.substring(0, 16);
            
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple hash if SHA-256 not available
            return String.valueOf(text.hashCode());
        }
    }
    
    /**
     * Cache statistics record.
     */
    public record CacheStats(
        long totalCachedItems,
        long ttlSeconds
    ) {}
}
package com.byo.rag.embedding.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Redis entity for caching embedding vectors to improve performance.
 * 
 * <p>This entity provides intelligent caching of embedding vectors to reduce
 * API calls to external embedding models and improve response times. The cache
 * uses content hashing to ensure cache validity and automatic TTL management.</p>
 * 
 * <p><strong>Caching Strategy:</strong></p>
 * <ul>
 *   <li><strong>Content Hashing:</strong> SHA-256 hash of text content for cache keys</li>
 *   <li><strong>Tenant Isolation:</strong> Complete cache separation between tenants</li>
 *   <li><strong>Model Versioning:</strong> Model-specific caching to prevent conflicts</li>
 *   <li><strong>Automatic Expiry:</strong> TTL-based expiration for cache freshness</li>
 * </ul>
 * 
 * <p><strong>Performance Benefits:</strong></p>
 * <ul>
 *   <li>Reduces API calls to external embedding services (OpenAI, etc.)</li>
 *   <li>Improves response times for repeated content</li>
 *   <li>Optimizes resource usage and reduces costs</li>
 *   <li>Enables offline processing for cached content</li>
 * </ul>
 * 
 * <p><strong>Cache Management:</strong></p>
 * <ul>
 *   <li>Configurable TTL per tenant or globally</li>
 *   <li>Manual cache invalidation capabilities</li>
 *   <li>Cache statistics and monitoring</li>
 *   <li>Memory usage optimization</li>
 * </ul>
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 */
@RedisHash(value = "embedding_cache", timeToLive = 3600) // Default 1 hour TTL
public class EmbeddingCache {
    
    @Id
    private String id; // Hash of tenant + text + model
    
    @Indexed
    private UUID tenantId;
    
    @Indexed
    private String textHash;
    
    @Indexed
    private String modelName;
    
    private List<Float> embedding;
    
    private LocalDateTime cachedAt;
    
    @TimeToLive
    private Long ttl; // Time to live in seconds
    
    /**
     * Default constructor for Redis serialization.
     */
    public EmbeddingCache() {
        this.cachedAt = LocalDateTime.now();
        this.ttl = 3600L; // Default 1 hour
    }
    
    /**
     * Constructor for creating new cache entries.
     *
     * @param tenantId The tenant identifier
     * @param textHash Hash of the original text
     * @param modelName The embedding model name
     * @param embedding The cached embedding vector
     * @param ttl Time to live in seconds
     */
    public EmbeddingCache(UUID tenantId, String textHash, String modelName, 
                         List<Float> embedding, Long ttl) {
        this();
        this.tenantId = tenantId;
        this.textHash = textHash;
        this.modelName = modelName;
        this.embedding = embedding;
        this.ttl = ttl;
        this.id = generateCacheKey(tenantId, textHash, modelName);
    }
    
    /**
     * Factory method for creating cache entries from text content.
     *
     * @param tenantId The tenant identifier
     * @param text The original text content
     * @param modelName The embedding model name
     * @param embedding The embedding vector to cache
     * @return A new EmbeddingCache instance
     */
    public static EmbeddingCache fromText(UUID tenantId, String text, String modelName, 
                                         List<Float> embedding) {
        String textHash = hashText(text);
        return new EmbeddingCache(tenantId, textHash, modelName, embedding, 3600L);
    }
    
    /**
     * Factory method with custom TTL.
     *
     * @param tenantId The tenant identifier
     * @param text The original text content
     * @param modelName The embedding model name
     * @param embedding The embedding vector to cache
     * @param ttlSeconds Custom TTL in seconds
     * @return A new EmbeddingCache instance
     */
    public static EmbeddingCache fromTextWithTtl(UUID tenantId, String text, String modelName, 
                                                List<Float> embedding, Long ttlSeconds) {
        String textHash = hashText(text);
        return new EmbeddingCache(tenantId, textHash, modelName, embedding, ttlSeconds);
    }
    
    /**
     * Generate cache key for lookup operations.
     *
     * @param tenantId The tenant identifier
     * @param text The text content
     * @param modelName The model name
     * @return The cache key for this combination
     */
    public static String generateCacheKeyForText(UUID tenantId, String text, String modelName) {
        String textHash = hashText(text);
        return generateCacheKey(tenantId, textHash, modelName);
    }
    
    /**
     * Check if the cache entry is still valid.
     *
     * @return true if the cache entry hasn't expired
     */
    public boolean isValid() {
        if (ttl == null) {
            return true; // No expiration
        }
        
        long ageSeconds = java.time.Duration.between(cachedAt, LocalDateTime.now()).getSeconds();
        return ageSeconds < ttl;
    }
    
    /**
     * Get the age of this cache entry in seconds.
     *
     * @return The age in seconds
     */
    public long getAgeSeconds() {
        return java.time.Duration.between(cachedAt, LocalDateTime.now()).getSeconds();
    }
    
    /**
     * Get the remaining TTL in seconds.
     *
     * @return Remaining TTL, or null if no expiration
     */
    public Long getRemainingTtl() {
        if (ttl == null) {
            return null;
        }
        
        long remaining = ttl - getAgeSeconds();
        return Math.max(0L, remaining);
    }
    
    /**
     * Check if this cache entry matches the given parameters.
     *
     * @param tenantId The tenant to check
     * @param text The text to check
     * @param modelName The model to check
     * @return true if matches, false otherwise
     */
    public boolean matches(UUID tenantId, String text, String modelName) {
        String expectedHash = hashText(text);
        return this.tenantId.equals(tenantId) && 
               this.textHash.equals(expectedHash) &&
               this.modelName.equals(modelName);
    }
    
    /**
     * Extend the TTL of this cache entry.
     *
     * @param additionalSeconds Additional seconds to add to TTL
     */
    public void extendTtl(Long additionalSeconds) {
        if (this.ttl != null && additionalSeconds != null) {
            this.ttl += additionalSeconds;
        }
    }
    
    // Helper methods
    
    private static String generateCacheKey(UUID tenantId, String textHash, String modelName) {
        return String.format("%s:%s:%s", tenantId.toString(), textHash, modelName);
    }
    
    private static String hashText(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(text.getBytes());
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            // Return first 16 characters for shorter keys
            return hexString.toString().substring(0, 16);
            
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple hash
            return String.valueOf(text.hashCode());
        }
    }
    
    // Getters and Setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public UUID getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }
    
    public String getTextHash() {
        return textHash;
    }
    
    public void setTextHash(String textHash) {
        this.textHash = textHash;
    }
    
    public String getModelName() {
        return modelName;
    }
    
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
    
    public List<Float> getEmbedding() {
        return embedding;
    }
    
    public void setEmbedding(List<Float> embedding) {
        this.embedding = embedding;
    }
    
    public LocalDateTime getCachedAt() {
        return cachedAt;
    }
    
    public void setCachedAt(LocalDateTime cachedAt) {
        this.cachedAt = cachedAt;
    }
    
    public Long getTtl() {
        return ttl;
    }
    
    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }
    
    @Override
    public String toString() {
        return "EmbeddingCache{" +
                "id='" + id + '\'' +
                ", tenantId=" + tenantId +
                ", textHash='" + textHash + '\'' +
                ", modelName='" + modelName + '\'' +
                ", dimension=" + (embedding != null ? embedding.size() : 0) +
                ", cachedAt=" + cachedAt +
                ", ttl=" + ttl +
                ", remainingTtl=" + getRemainingTtl() +
                '}';
    }
}
package com.byo.rag.embedding.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Redis entity for storing vector embeddings with metadata.
 * 
 * <p>This entity represents a vector document stored in Redis with all necessary
 * metadata for multi-tenant vector operations and similarity search. Each vector
 * is associated with a tenant, document, chunk, and AI model used for generation.</p>
 * 
 * <p><strong>Storage Structure:</strong></p>
 * <ul>
 *   <li><strong>Primary Key:</strong> Generated composite key for unique identification</li>
 *   <li><strong>Tenant Isolation:</strong> Complete separation via tenantId indexing</li>
 *   <li><strong>Model Versioning:</strong> Support for multiple embedding models</li>
 *   <li><strong>Vector Storage:</strong> High-dimensional embedding vectors</li>
 *   <li><strong>Metadata:</strong> Flexible key-value storage for additional properties</li>
 * </ul>
 * 
 * <p><strong>Redis Integration:</strong></p>
 * <ul>
 *   <li>Uses Redis Stack for vector similarity search capabilities</li>
 *   <li>Indexed fields enable efficient tenant and model-based queries</li>
 *   <li>Automatic TTL management for performance optimization</li>
 * </ul>
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 */
@RedisHash("vector")
public class VectorDocument {
    
    @Id
    private String id;
    
    @Indexed
    private UUID tenantId;
    
    @Indexed
    private UUID documentId;
    
    @Indexed
    private UUID chunkId;
    
    @Indexed
    private String modelName;
    
    private List<Float> embedding;
    
    private Map<String, Object> metadata;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    /**
     * Default constructor for Redis serialization.
     */
    public VectorDocument() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Constructor for creating new vector documents.
     *
     * @param tenantId The tenant identifier
     * @param documentId The document identifier
     * @param chunkId The chunk identifier
     * @param modelName The embedding model name
     * @param embedding The vector embedding
     * @param metadata Additional metadata
     */
    public VectorDocument(UUID tenantId, UUID documentId, UUID chunkId, 
                         String modelName, List<Float> embedding, 
                         Map<String, Object> metadata) {
        this();
        this.tenantId = tenantId;
        this.documentId = documentId;
        this.chunkId = chunkId;
        this.modelName = modelName;
        this.embedding = embedding;
        this.metadata = metadata;
    }
    
    /**
     * Factory method for creating vector documents from embedding results.
     *
     * @param tenantId The tenant identifier
     * @param documentId The document identifier
     * @param chunkId The chunk identifier
     * @param modelName The embedding model name
     * @param embedding The vector embedding
     * @return A new VectorDocument instance
     */
    public static VectorDocument of(UUID tenantId, UUID documentId, UUID chunkId,
                                   String modelName, List<Float> embedding) {
        return new VectorDocument(tenantId, documentId, chunkId, modelName, 
                                 embedding, Map.of());
    }
    
    /**
     * Factory method with metadata.
     *
     * @param tenantId The tenant identifier
     * @param documentId The document identifier
     * @param chunkId The chunk identifier
     * @param modelName The embedding model name
     * @param embedding The vector embedding
     * @param metadata Additional metadata
     * @return A new VectorDocument instance
     */
    public static VectorDocument withMetadata(UUID tenantId, UUID documentId, UUID chunkId,
                                             String modelName, List<Float> embedding,
                                             Map<String, Object> metadata) {
        return new VectorDocument(tenantId, documentId, chunkId, modelName, 
                                 embedding, metadata);
    }
    
    /**
     * Update the vector document with new embedding data.
     *
     * @param newEmbedding The new embedding vector
     * @param newMetadata Updated metadata
     */
    public void updateEmbedding(List<Float> newEmbedding, Map<String, Object> newMetadata) {
        this.embedding = newEmbedding;
        this.metadata = newMetadata != null ? newMetadata : this.metadata;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Get the vector dimension.
     *
     * @return The dimension of the embedding vector
     */
    public int getDimension() {
        return embedding != null ? embedding.size() : 0;
    }
    
    /**
     * Check if this vector document matches the given tenant and model.
     *
     * @param tenantId The tenant to check
     * @param modelName The model to check
     * @return true if matches, false otherwise
     */
    public boolean matches(UUID tenantId, String modelName) {
        return this.tenantId.equals(tenantId) && 
               this.modelName.equals(modelName);
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
    
    public UUID getDocumentId() {
        return documentId;
    }
    
    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }
    
    public UUID getChunkId() {
        return chunkId;
    }
    
    public void setChunkId(UUID chunkId) {
        this.chunkId = chunkId;
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
        this.updatedAt = LocalDateTime.now();
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @Override
    public String toString() {
        return "VectorDocument{" +
                "id='" + id + '\'' +
                ", tenantId=" + tenantId +
                ", documentId=" + documentId +
                ", chunkId=" + chunkId +
                ", modelName='" + modelName + '\'' +
                ", dimension=" + getDimension() +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
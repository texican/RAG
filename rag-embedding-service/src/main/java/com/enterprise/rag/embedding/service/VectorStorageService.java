package com.enterprise.rag.embedding.service;

import com.enterprise.rag.embedding.config.RedisConfig.VectorStorageProperties;
import com.enterprise.rag.embedding.dto.EmbeddingResponse.EmbeddingResult;
import com.enterprise.rag.embedding.dto.SearchRequest;
import com.enterprise.rag.embedding.dto.SearchResponse;
import com.enterprise.rag.embedding.dto.SearchResponse.SearchResult;
import com.enterprise.rag.shared.exception.EmbeddingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.search.*;
import redis.clients.jedis.search.schemafields.NumericField;
import redis.clients.jedis.search.schemafields.TextField;
import redis.clients.jedis.search.schemafields.VectorField;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for storing and retrieving vectors in Redis Stack.
 */
@Service
public class VectorStorageService {

    private static final Logger logger = LoggerFactory.getLogger(VectorStorageService.class);
    
    private final JedisPool jedisPool;
    private final VectorStorageProperties properties;
    private final Set<String> createdIndexes = ConcurrentHashMap.newKeySet();
    
    public VectorStorageService(JedisPool jedisPool, VectorStorageProperties properties) {
        this.jedisPool = jedisPool;
        this.properties = properties;
    }
    
    /**
     * Store vectors for document chunks.
     */
    public void storeVectors(UUID tenantId, UUID documentId, 
                           List<EmbeddingResult> results, String modelName) {
        
        String indexName = getIndexName(tenantId, modelName);
        
        try (Jedis jedis = jedisPool.getResource()) {
            // Ensure index exists
            ensureIndexExists(jedis, indexName);
            
            // Store vectors in batch
            for (EmbeddingResult result : results) {
                if (!"SUCCESS".equals(result.status()) || result.embedding() == null) {
                    continue;
                }
                
                String key = getVectorKey(tenantId, documentId, result.chunkId());
                Map<String, Object> fields = new HashMap<>();
                
                // Metadata fields
                fields.put("tenant_id", tenantId.toString());
                fields.put("document_id", documentId.toString());
                fields.put("chunk_id", result.chunkId().toString());
                fields.put("model_name", modelName);
                fields.put("content", result.text());
                fields.put("created_at", System.currentTimeMillis());
                
                // Convert embedding to byte array for Redis
                byte[] vectorBytes = floatListToByteArray(result.embedding());
                fields.put("vector", vectorBytes);
                
                // Store in Redis
                jedis.hset(key, fields);
                
                logger.debug("Stored vector for chunk: {} in tenant: {}", 
                           result.chunkId(), tenantId);
            }
            
            logger.info("Stored {} vectors for document: {} in tenant: {}", 
                       results.size(), documentId, tenantId);
                       
        } catch (Exception e) {
            logger.error("Failed to store vectors for tenant: {}, document: {}", 
                        tenantId, documentId, e);
            throw new EmbeddingException("Failed to store vectors in Redis", e);
        }
    }
    
    /**
     * Search for similar vectors.
     */
    public SearchResponse searchSimilar(SearchRequest request, List<Float> queryEmbedding) {
        long startTime = System.currentTimeMillis();
        
        String modelName = getEffectiveModelName(request.modelName());
        String indexName = getIndexName(request.tenantId(), modelName);
        
        try (Jedis jedis = jedisPool.getResource()) {
            // Ensure index exists
            if (!indexExists(jedis, indexName)) {
                logger.info("Index {} does not exist, returning empty results", indexName);
                return SearchResponse.empty(request.tenantId(), request.query(), 
                                          modelName, System.currentTimeMillis() - startTime);
            }
            
            // Build search query
            Query searchQuery = buildSearchQuery(request, queryEmbedding);
            
            // Execute search
            SearchResult searchResult = jedis.ftSearch(indexName, searchQuery);
            
            // Process results
            List<SearchResult> results = processSearchResults(searchResult.getDocuments(), request);
            
            long searchTime = System.currentTimeMillis() - startTime;
            
            logger.info("Found {} results for query in tenant: {} ({}ms)", 
                       results.size(), request.tenantId(), searchTime);
            
            return SearchResponse.success(request.tenantId(), request.query(), 
                                        modelName, results, searchTime);
                                        
        } catch (Exception e) {
            long searchTime = System.currentTimeMillis() - startTime;
            logger.error("Failed to search vectors for tenant: {}, query: {}", 
                        request.tenantId(), request.query(), e);
            
            return SearchResponse.empty(request.tenantId(), request.query(), 
                                      modelName, searchTime);
        }
    }
    
    /**
     * Delete vectors for a document.
     */
    public void deleteDocumentVectors(UUID tenantId, UUID documentId, String modelName) {
        String indexName = getIndexName(tenantId, modelName);
        
        try (Jedis jedis = jedisPool.getResource()) {
            if (!indexExists(jedis, indexName)) {
                return;
            }
            
            // Search for all vectors for this document
            Query query = new Query("@document_id:" + documentId.toString());
            SearchResult searchResult = jedis.ftSearch(indexName, query);
            
            // Delete all found documents
            for (Document doc : searchResult.getDocuments()) {
                jedis.del(doc.getId());
            }
            
            logger.info("Deleted vectors for document: {} in tenant: {}", documentId, tenantId);
            
        } catch (Exception e) {
            logger.error("Failed to delete vectors for document: {} in tenant: {}", 
                        documentId, tenantId, e);
        }
    }
    
    /**
     * Get statistics for tenant's vector storage.
     */
    public VectorStats getStats(UUID tenantId, String modelName) {
        String indexName = getIndexName(tenantId, modelName);
        
        try (Jedis jedis = jedisPool.getResource()) {
            if (!indexExists(jedis, indexName)) {
                return new VectorStats(0, 0, 0);
            }
            
            IndexInfo indexInfo = jedis.ftInfo(indexName);
            
            // Extract statistics from index info
            long totalVectors = indexInfo.getNumDocs();
            long indexSize = indexInfo.getInvertedSzMB();
            long vectorSize = indexInfo.getVectorIndexSzMB();
            
            return new VectorStats(totalVectors, indexSize, vectorSize);
            
        } catch (Exception e) {
            logger.error("Failed to get vector statistics for tenant: {}", tenantId, e);
            return new VectorStats(0, 0, 0);
        }
    }
    
    private void ensureIndexExists(Jedis jedis, String indexName) {
        if (createdIndexes.contains(indexName) || indexExists(jedis, indexName)) {
            return;
        }
        
        try {
            Schema schema = new Schema()
                .addField(new TextField("tenant_id", 1.0))
                .addField(new TextField("document_id", 1.0))
                .addField(new TextField("chunk_id", 1.0))
                .addField(new TextField("model_name", 1.0))
                .addField(new TextField("content", 1.0))
                .addField(new NumericField("created_at"))
                .addField(VectorField.builder()
                    .fieldName("vector")
                    .algorithm(VectorField.VectorAlgorithm.HNSW)
                    .attributes(Map.of(
                        "TYPE", "FLOAT32",
                        "DIM", properties.dimension(),
                        "DISTANCE_METRIC", properties.similarityAlgorithm(),
                        "INITIAL_CAP", "1000",
                        "M", properties.maxConnections(),
                        "EF_CONSTRUCTION", properties.efConstruction(),
                        "EF_RUNTIME", properties.efRuntime()
                    ))
                    .build());
            
            jedis.ftCreate(indexName, IndexOptions.defaultOptions(), schema);
            createdIndexes.add(indexName);
            
            logger.info("Created vector index: {}", indexName);
            
        } catch (Exception e) {
            if (e.getMessage().contains("Index already exists")) {
                createdIndexes.add(indexName);
                logger.debug("Index {} already exists", indexName);
            } else {
                logger.error("Failed to create index: {}", indexName, e);
                throw new EmbeddingException("Failed to create vector index", e);
            }
        }
    }
    
    private boolean indexExists(Jedis jedis, String indexName) {
        try {
            jedis.ftInfo(indexName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private Query buildSearchQuery(SearchRequest request, List<Float> queryEmbedding) {
        StringBuilder queryBuilder = new StringBuilder();
        
        // Tenant isolation
        queryBuilder.append("@tenant_id:").append(request.tenantId());
        
        // Document filtering
        if (request.documentIds() != null && !request.documentIds().isEmpty()) {
            queryBuilder.append(" @document_id:(");
            queryBuilder.append(request.documentIds().stream()
                .map(UUID::toString)
                .collect(Collectors.joining("|")));
            queryBuilder.append(")");
        }
        
        // Vector similarity search
        String vectorQuery = String.format("=>[KNN %d @vector $BLOB AS score]", request.topK());
        queryBuilder.append(" ").append(vectorQuery);
        
        Query query = new Query(queryBuilder.toString())
            .addParam("BLOB", floatListToByteArray(queryEmbedding))
            .returnFields("tenant_id", "document_id", "chunk_id", "content", "score")
            .setSortBy("score", false)  // Sort by similarity score descending
            .limit(0, request.topK());
            
        return query;
    }
    
    private List<SearchResult> processSearchResults(List<Document> documents, SearchRequest request) {
        return documents.stream()
            .map(doc -> {
                UUID chunkId = UUID.fromString(doc.getString("chunk_id"));
                UUID documentId = UUID.fromString(doc.getString("document_id"));
                String content = request.includeContent() ? doc.getString("content") : null;
                double score = Double.parseDouble(doc.getString("score"));
                
                // Apply threshold filtering
                if (score < request.threshold()) {
                    return null;
                }
                
                return SearchResult.of(
                    chunkId,
                    documentId, 
                    content,
                    score,
                    request.includeMetadata() ? getMetadata(doc) : null,
                    null,  // Document title would need separate lookup
                    null   // Document type would need separate lookup
                );
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    private Map<String, Object> getMetadata(Document doc) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("model_name", doc.getString("model_name"));
        metadata.put("created_at", doc.getString("created_at"));
        return metadata;
    }
    
    private byte[] floatListToByteArray(List<Float> floats) {
        byte[] bytes = new byte[floats.size() * 4];
        for (int i = 0; i < floats.size(); i++) {
            int bits = Float.floatToIntBits(floats.get(i));
            bytes[i * 4] = (byte) (bits >> 24);
            bytes[i * 4 + 1] = (byte) (bits >> 16);
            bytes[i * 4 + 2] = (byte) (bits >> 8);
            bytes[i * 4 + 3] = (byte) bits;
        }
        return bytes;
    }
    
    private String getIndexName(UUID tenantId, String modelName) {
        return properties.indexPrefix() + ":" + tenantId + ":" + modelName;
    }
    
    private String getVectorKey(UUID tenantId, UUID documentId, UUID chunkId) {
        return String.format("vector:%s:%s:%s", tenantId, documentId, chunkId);
    }
    
    private String getEffectiveModelName(String requestedModel) {
        return requestedModel != null ? requestedModel : "default";
    }
    
    /**
     * Vector storage statistics.
     */
    public record VectorStats(
        long totalVectors,
        long indexSizeMB,
        long vectorSizeMB
    ) {}
}
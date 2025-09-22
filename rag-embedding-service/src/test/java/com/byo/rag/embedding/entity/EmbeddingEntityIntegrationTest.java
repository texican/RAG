package com.byo.rag.embedding.entity;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Integration tests for embedding entities without Spring context.
 * 
 * <p>These tests validate entity functionality independently to ensure
 * the core business logic works correctly before full integration testing.</p>
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 */
@DisplayName("Embedding Entity Integration Tests")
class EmbeddingEntityIntegrationTest {
    
    private static final UUID TEST_TENANT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID TEST_DOCUMENT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID TEST_CHUNK_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
    private static final String TEST_MODEL_NAME = "text-embedding-3-small";
    private static final String TEST_TEXT = "This is a test text for embedding";
    private static final List<Float> TEST_EMBEDDING = List.of(0.1f, 0.2f, 0.3f, 0.4f, 0.5f);
    
    @Test
    @DisplayName("Should create and validate VectorDocument entity")
    void shouldCreateAndValidateVectorDocumentEntity() {
        // Given
        Map<String, Object> metadata = Map.of("source", "test", "confidence", 0.95);
        
        // When
        VectorDocument document = VectorDocument.withMetadata(
            TEST_TENANT_ID, TEST_DOCUMENT_ID, TEST_CHUNK_ID,
            TEST_MODEL_NAME, TEST_EMBEDDING, metadata);
        
        // Then
        Assertions.assertThat(document).isNotNull();
        Assertions.assertThat(document.getTenantId()).isEqualTo(TEST_TENANT_ID);
        Assertions.assertThat(document.getDocumentId()).isEqualTo(TEST_DOCUMENT_ID);
        Assertions.assertThat(document.getChunkId()).isEqualTo(TEST_CHUNK_ID);
        Assertions.assertThat(document.getModelName()).isEqualTo(TEST_MODEL_NAME);
        Assertions.assertThat(document.getEmbedding()).isEqualTo(TEST_EMBEDDING);
        Assertions.assertThat(document.getMetadata()).isEqualTo(metadata);
        Assertions.assertThat(document.getDimension()).isEqualTo(5);
        Assertions.assertThat(document.getCreatedAt()).isNotNull();
        Assertions.assertThat(document.getUpdatedAt()).isNotNull();
    }
    
    @Test
    @DisplayName("Should create and validate EmbeddingCache entity")
    void shouldCreateAndValidateEmbeddingCacheEntity() {
        // When
        EmbeddingCache cache = EmbeddingCache.fromText(
            TEST_TENANT_ID, TEST_TEXT, TEST_MODEL_NAME, TEST_EMBEDDING);
        
        // Then
        Assertions.assertThat(cache).isNotNull();
        Assertions.assertThat(cache.getTenantId()).isEqualTo(TEST_TENANT_ID);
        Assertions.assertThat(cache.getTextHash()).isNotNull();
        Assertions.assertThat(cache.getTextHash()).hasSize(16);
        Assertions.assertThat(cache.getModelName()).isEqualTo(TEST_MODEL_NAME);
        Assertions.assertThat(cache.getEmbedding()).isEqualTo(TEST_EMBEDDING);
        Assertions.assertThat(cache.getTtl()).isEqualTo(3600L);
        Assertions.assertThat(cache.getCachedAt()).isNotNull();
        Assertions.assertThat(cache.isValid()).isTrue();
    }
    
    @Test
    @DisplayName("Should demonstrate multi-tenant isolation")
    void shouldDemonstrateMultiTenantIsolation() {
        // Given
        UUID tenant1 = UUID.randomUUID();
        UUID tenant2 = UUID.randomUUID();
        
        // When
        VectorDocument doc1 = VectorDocument.of(tenant1, TEST_DOCUMENT_ID, TEST_CHUNK_ID, 
                                               TEST_MODEL_NAME, TEST_EMBEDDING);
        VectorDocument doc2 = VectorDocument.of(tenant2, TEST_DOCUMENT_ID, TEST_CHUNK_ID, 
                                               TEST_MODEL_NAME, TEST_EMBEDDING);
        
        EmbeddingCache cache1 = EmbeddingCache.fromText(tenant1, TEST_TEXT, TEST_MODEL_NAME, TEST_EMBEDDING);
        EmbeddingCache cache2 = EmbeddingCache.fromText(tenant2, TEST_TEXT, TEST_MODEL_NAME, TEST_EMBEDDING);
        
        // Then - Documents should be isolated by tenant
        Assertions.assertThat(doc1.matches(tenant1, TEST_MODEL_NAME)).isTrue();
        Assertions.assertThat(doc1.matches(tenant2, TEST_MODEL_NAME)).isFalse();
        Assertions.assertThat(doc2.matches(tenant2, TEST_MODEL_NAME)).isTrue();
        Assertions.assertThat(doc2.matches(tenant1, TEST_MODEL_NAME)).isFalse();
        
        // Then - Caches should be isolated by tenant
        Assertions.assertThat(cache1.matches(tenant1, TEST_TEXT, TEST_MODEL_NAME)).isTrue();
        Assertions.assertThat(cache1.matches(tenant2, TEST_TEXT, TEST_MODEL_NAME)).isFalse();
        Assertions.assertThat(cache2.matches(tenant2, TEST_TEXT, TEST_MODEL_NAME)).isTrue();
        Assertions.assertThat(cache2.matches(tenant1, TEST_TEXT, TEST_MODEL_NAME)).isFalse();
    }
    
    @Test
    @DisplayName("Should handle cache TTL scenarios")
    void shouldHandleCacheTtlScenarios() {
        // Given
        Long shortTtl = 1L; // 1 second
        Long longTtl = 3600L; // 1 hour
        
        // When
        EmbeddingCache shortCache = EmbeddingCache.fromTextWithTtl(
            TEST_TENANT_ID, TEST_TEXT, TEST_MODEL_NAME, TEST_EMBEDDING, shortTtl);
        EmbeddingCache longCache = EmbeddingCache.fromTextWithTtl(
            TEST_TENANT_ID, TEST_TEXT + "_different", TEST_MODEL_NAME, TEST_EMBEDDING, longTtl);
        EmbeddingCache noTtlCache = EmbeddingCache.fromText(
            TEST_TENANT_ID, TEST_TEXT + "_no_ttl", TEST_MODEL_NAME, TEST_EMBEDDING);
        noTtlCache.setTtl(null);
        
        // Then - TTL values should be set correctly
        Assertions.assertThat(shortCache.getTtl()).isEqualTo(shortTtl);
        Assertions.assertThat(longCache.getTtl()).isEqualTo(longTtl);
        Assertions.assertThat(noTtlCache.getTtl()).isNull();
        
        // Then - Validity checks should work
        Assertions.assertThat(longCache.isValid()).isTrue();
        Assertions.assertThat(noTtlCache.isValid()).isTrue(); // No TTL means always valid
        
        // Then - Remaining TTL should be calculated
        Assertions.assertThat(longCache.getRemainingTtl()).isLessThanOrEqualTo(longTtl);
        Assertions.assertThat(longCache.getRemainingTtl()).isGreaterThan(3500L);
        Assertions.assertThat(noTtlCache.getRemainingTtl()).isNull();
    }
    
    @Test
    @DisplayName("Should handle vector document updates")
    void shouldHandleVectorDocumentUpdates() {
        // Given
        VectorDocument document = VectorDocument.of(
            TEST_TENANT_ID, TEST_DOCUMENT_ID, TEST_CHUNK_ID,
            TEST_MODEL_NAME, TEST_EMBEDDING);
        
        List<Float> newEmbedding = List.of(0.6f, 0.7f, 0.8f, 0.9f);
        Map<String, Object> newMetadata = Map.of("updated", true, "version", 2);
        
        // When
        document.updateEmbedding(newEmbedding, newMetadata);
        
        // Then
        Assertions.assertThat(document.getEmbedding()).isEqualTo(newEmbedding);
        Assertions.assertThat(document.getMetadata()).isEqualTo(newMetadata);
        Assertions.assertThat(document.getDimension()).isEqualTo(4);
    }
    
    @Test
    @DisplayName("Should generate consistent cache keys")
    void shouldGenerateConsistentCacheKeys() {
        // Given
        String text1 = "Consistent text for caching";
        String text2 = "Different text for caching";
        
        // When
        String key1a = EmbeddingCache.generateCacheKeyForText(TEST_TENANT_ID, text1, TEST_MODEL_NAME);
        String key1b = EmbeddingCache.generateCacheKeyForText(TEST_TENANT_ID, text1, TEST_MODEL_NAME);
        String key2 = EmbeddingCache.generateCacheKeyForText(TEST_TENANT_ID, text2, TEST_MODEL_NAME);
        
        // Then
        Assertions.assertThat(key1a).isEqualTo(key1b); // Same text produces same key
        Assertions.assertThat(key1a).isNotEqualTo(key2); // Different text produces different key
        Assertions.assertThat(key1a).contains(TEST_TENANT_ID.toString());
        Assertions.assertThat(key1a).contains(TEST_MODEL_NAME);
    }
    
    @Test
    @DisplayName("Should handle large embeddings efficiently")
    void shouldHandleLargeEmbeddingsEfficiently() {
        // Given - Create realistic OpenAI embedding size
        List<Float> largeEmbedding = java.util.stream.IntStream.range(0, 1536)
            .mapToObj(i -> (float) Math.random())
            .toList();
        
        // When
        VectorDocument document = VectorDocument.of(
            TEST_TENANT_ID, TEST_DOCUMENT_ID, TEST_CHUNK_ID,
            TEST_MODEL_NAME, largeEmbedding);
        
        EmbeddingCache cache = EmbeddingCache.fromText(
            TEST_TENANT_ID, TEST_TEXT, TEST_MODEL_NAME, largeEmbedding);
        
        // Then
        Assertions.assertThat(document.getDimension()).isEqualTo(1536);
        Assertions.assertThat(document.getEmbedding()).hasSize(1536);
        Assertions.assertThat(cache.getEmbedding()).hasSize(1536);
        Assertions.assertThat(cache.getEmbedding()).isEqualTo(largeEmbedding);
    }
    
    @Test
    @DisplayName("Should validate toString implementations")
    void shouldValidateToStringImplementations() {
        // Given
        VectorDocument document = VectorDocument.of(
            TEST_TENANT_ID, TEST_DOCUMENT_ID, TEST_CHUNK_ID,
            TEST_MODEL_NAME, TEST_EMBEDDING);
        document.setId("test-vector-id");
        
        EmbeddingCache cache = EmbeddingCache.fromText(
            TEST_TENANT_ID, TEST_TEXT, TEST_MODEL_NAME, TEST_EMBEDDING);
        
        // When
        String documentString = document.toString();
        String cacheString = cache.toString();
        
        // Then
        Assertions.assertThat(documentString)
            .contains("VectorDocument{")
            .contains("id='test-vector-id'")
            .contains(TEST_TENANT_ID.toString())
            .contains(TEST_MODEL_NAME)
            .contains("dimension=5");
        
        Assertions.assertThat(cacheString)
            .contains("EmbeddingCache{")
            .contains(TEST_TENANT_ID.toString())
            .contains(TEST_MODEL_NAME)
            .contains("dimension=5")
            .contains("ttl=3600");
    }
}
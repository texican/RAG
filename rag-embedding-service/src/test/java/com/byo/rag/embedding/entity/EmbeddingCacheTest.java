package com.byo.rag.embedding.entity;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Comprehensive unit tests for EmbeddingCache entity.
 * 
 * <p>Tests cover cache creation, TTL management, content hashing,
 * cache validation, and performance optimization features.</p>
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 */
@DisplayName("EmbeddingCache Entity Tests")
class EmbeddingCacheTest {
    
    private static final UUID TEST_TENANT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final String TEST_TEXT = "This is a test text for embedding cache";
    private static final String TEST_MODEL_NAME = "text-embedding-3-small";
    private static final List<Float> TEST_EMBEDDING = List.of(0.1f, 0.2f, 0.3f, 0.4f, 0.5f);
    
    @Test
    @DisplayName("Should create EmbeddingCache with default constructor")
    void shouldCreateEmbeddingCacheWithDefaultConstructor() {
        // When
        EmbeddingCache cache = new EmbeddingCache();
        
        // Then
        Assertions.assertThat(cache.getId()).isNull();
        Assertions.assertThat(cache.getTenantId()).isNull();
        Assertions.assertThat(cache.getTextHash()).isNull();
        Assertions.assertThat(cache.getModelName()).isNull();
        Assertions.assertThat(cache.getEmbedding()).isNull();
        Assertions.assertThat(cache.getCachedAt()).isNotNull();
        Assertions.assertThat(cache.getTtl()).isEqualTo(3600L); // Default 1 hour
    }
    
    @Test
    @DisplayName("Should create EmbeddingCache with parameterized constructor")
    void shouldCreateEmbeddingCacheWithParameterizedConstructor() {
        // Given
        String textHash = "abc123def456";
        Long ttl = 7200L; // 2 hours
        
        // When
        EmbeddingCache cache = new EmbeddingCache(
            TEST_TENANT_ID, textHash, TEST_MODEL_NAME, TEST_EMBEDDING, ttl);
        
        // Then
        Assertions.assertThat(cache.getTenantId()).isEqualTo(TEST_TENANT_ID);
        Assertions.assertThat(cache.getTextHash()).isEqualTo(textHash);
        Assertions.assertThat(cache.getModelName()).isEqualTo(TEST_MODEL_NAME);
        Assertions.assertThat(cache.getEmbedding()).isEqualTo(TEST_EMBEDDING);
        Assertions.assertThat(cache.getTtl()).isEqualTo(ttl);
        Assertions.assertThat(cache.getCachedAt()).isNotNull();
        
        // ID should be generated from tenant + hash + model
        String expectedId = TEST_TENANT_ID + ":" + textHash + ":" + TEST_MODEL_NAME;
        Assertions.assertThat(cache.getId()).isEqualTo(expectedId);
    }
    
    @Test
    @DisplayName("Should create EmbeddingCache from text using factory method")
    void shouldCreateEmbeddingCacheFromTextUsingFactoryMethod() {
        // When
        EmbeddingCache cache = EmbeddingCache.fromText(
            TEST_TENANT_ID, TEST_TEXT, TEST_MODEL_NAME, TEST_EMBEDDING);
        
        // Then
        Assertions.assertThat(cache.getTenantId()).isEqualTo(TEST_TENANT_ID);
        Assertions.assertThat(cache.getTextHash()).isNotNull();
        Assertions.assertThat(cache.getTextHash()).hasSize(16); // First 16 chars of hash
        Assertions.assertThat(cache.getModelName()).isEqualTo(TEST_MODEL_NAME);
        Assertions.assertThat(cache.getEmbedding()).isEqualTo(TEST_EMBEDDING);
        Assertions.assertThat(cache.getTtl()).isEqualTo(3600L); // Default TTL
        Assertions.assertThat(cache.getCachedAt()).isNotNull();
    }
    
    @Test
    @DisplayName("Should create EmbeddingCache from text with custom TTL")
    void shouldCreateEmbeddingCacheFromTextWithCustomTtl() {
        // Given
        Long customTtl = 1800L; // 30 minutes
        
        // When
        EmbeddingCache cache = EmbeddingCache.fromTextWithTtl(
            TEST_TENANT_ID, TEST_TEXT, TEST_MODEL_NAME, TEST_EMBEDDING, customTtl);
        
        // Then
        Assertions.assertThat(cache.getTenantId()).isEqualTo(TEST_TENANT_ID);
        Assertions.assertThat(cache.getTextHash()).isNotNull();
        Assertions.assertThat(cache.getModelName()).isEqualTo(TEST_MODEL_NAME);
        Assertions.assertThat(cache.getEmbedding()).isEqualTo(TEST_EMBEDDING);
        Assertions.assertThat(cache.getTtl()).isEqualTo(customTtl);
    }
    
    @Test
    @DisplayName("Should generate consistent cache keys for same text")
    void shouldGenerateConsistentCacheKeysForSameText() {
        // When
        String key1 = EmbeddingCache.generateCacheKeyForText(TEST_TENANT_ID, TEST_TEXT, TEST_MODEL_NAME);
        String key2 = EmbeddingCache.generateCacheKeyForText(TEST_TENANT_ID, TEST_TEXT, TEST_MODEL_NAME);
        
        // Then
        Assertions.assertThat(key1).isEqualTo(key2);
        Assertions.assertThat(key1).contains(TEST_TENANT_ID.toString());
        Assertions.assertThat(key1).contains(TEST_MODEL_NAME);
    }
    
    @Test
    @DisplayName("Should generate different cache keys for different text")
    void shouldGenerateDifferentCacheKeysForDifferentText() {
        // Given
        String text1 = "First text content";
        String text2 = "Second text content";
        
        // When
        String key1 = EmbeddingCache.generateCacheKeyForText(TEST_TENANT_ID, text1, TEST_MODEL_NAME);
        String key2 = EmbeddingCache.generateCacheKeyForText(TEST_TENANT_ID, text2, TEST_MODEL_NAME);
        
        // Then
        Assertions.assertThat(key1).isNotEqualTo(key2);
    }
    
    @Test
    @DisplayName("Should validate cache entry is valid when within TTL")
    void shouldValidateCacheEntryIsValidWhenWithinTtl() {
        // Given
        EmbeddingCache cache = EmbeddingCache.fromTextWithTtl(
            TEST_TENANT_ID, TEST_TEXT, TEST_MODEL_NAME, TEST_EMBEDDING, 3600L);
        
        // When
        boolean isValid = cache.isValid();
        
        // Then
        Assertions.assertThat(isValid).isTrue();
    }
    
    @Test
    @DisplayName("Should return false for validity when TTL is zero")
    void shouldReturnFalseForValidityWhenTtlIsZero() {
        // Given
        EmbeddingCache cache = EmbeddingCache.fromTextWithTtl(
            TEST_TENANT_ID, TEST_TEXT, TEST_MODEL_NAME, TEST_EMBEDDING, 0L);
        
        // Wait a moment to ensure age > 0
        try { Thread.sleep(10); } catch (InterruptedException e) {}
        
        // When
        boolean isValid = cache.isValid();
        
        // Then
        Assertions.assertThat(isValid).isFalse();
    }
    
    @Test
    @DisplayName("Should return true for validity when TTL is null (no expiration)")
    void shouldReturnTrueForValidityWhenTtlIsNull() {
        // Given
        EmbeddingCache cache = EmbeddingCache.fromText(
            TEST_TENANT_ID, TEST_TEXT, TEST_MODEL_NAME, TEST_EMBEDDING);
        cache.setTtl(null); // No expiration
        
        // When
        boolean isValid = cache.isValid();
        
        // Then
        Assertions.assertThat(isValid).isTrue();
    }
    
    @Test
    @DisplayName("Should calculate age in seconds correctly")
    void shouldCalculateAgeInSecondsCorrectly() {
        // Given
        EmbeddingCache cache = EmbeddingCache.fromText(
            TEST_TENANT_ID, TEST_TEXT, TEST_MODEL_NAME, TEST_EMBEDDING);
        
        // Wait a moment
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        
        // When
        long age = cache.getAgeSeconds();
        
        // Then
        Assertions.assertThat(age).isGreaterThanOrEqualTo(0);
        Assertions.assertThat(age).isLessThan(10); // Should be very small
    }
    
    @Test
    @DisplayName("Should calculate remaining TTL correctly")
    void shouldCalculateRemainingTtlCorrectly() {
        // Given
        Long ttl = 3600L;
        EmbeddingCache cache = EmbeddingCache.fromTextWithTtl(
            TEST_TENANT_ID, TEST_TEXT, TEST_MODEL_NAME, TEST_EMBEDDING, ttl);
        
        // When
        Long remainingTtl = cache.getRemainingTtl();
        
        // Then
        Assertions.assertThat(remainingTtl).isNotNull();
        Assertions.assertThat(remainingTtl).isLessThanOrEqualTo(ttl);
        Assertions.assertThat(remainingTtl).isGreaterThan(3500L); // Should be close to original
    }
    
    @Test
    @DisplayName("Should return null remaining TTL when TTL is null")
    void shouldReturnNullRemainingTtlWhenTtlIsNull() {
        // Given
        EmbeddingCache cache = EmbeddingCache.fromText(
            TEST_TENANT_ID, TEST_TEXT, TEST_MODEL_NAME, TEST_EMBEDDING);
        cache.setTtl(null);
        
        // When
        Long remainingTtl = cache.getRemainingTtl();
        
        // Then
        Assertions.assertThat(remainingTtl).isNull();
    }
    
    @Test
    @DisplayName("Should return zero remaining TTL when expired")
    void shouldReturnZeroRemainingTtlWhenExpired() {
        // Given
        EmbeddingCache cache = EmbeddingCache.fromTextWithTtl(
            TEST_TENANT_ID, TEST_TEXT, TEST_MODEL_NAME, TEST_EMBEDDING, 0L);
        
        // Wait to ensure expiration
        try { Thread.sleep(10); } catch (InterruptedException e) {}
        
        // When
        Long remainingTtl = cache.getRemainingTtl();
        
        // Then
        Assertions.assertThat(remainingTtl).isEqualTo(0L);
    }
    
    @Test
    @DisplayName("Should match parameters correctly")
    void shouldMatchParametersCorrectly() {
        // Given
        EmbeddingCache cache = EmbeddingCache.fromText(
            TEST_TENANT_ID, TEST_TEXT, TEST_MODEL_NAME, TEST_EMBEDDING);
        
        // When & Then - Positive match
        Assertions.assertThat(cache.matches(TEST_TENANT_ID, TEST_TEXT, TEST_MODEL_NAME)).isTrue();
        
        // When & Then - Negative matches
        UUID differentTenant = UUID.randomUUID();
        String differentText = "Different text content";
        String differentModel = "different-model";
        
        Assertions.assertThat(cache.matches(differentTenant, TEST_TEXT, TEST_MODEL_NAME)).isFalse();
        Assertions.assertThat(cache.matches(TEST_TENANT_ID, differentText, TEST_MODEL_NAME)).isFalse();
        Assertions.assertThat(cache.matches(TEST_TENANT_ID, TEST_TEXT, differentModel)).isFalse();
    }
    
    @Test
    @DisplayName("Should extend TTL correctly")
    void shouldExtendTtlCorrectly() {
        // Given
        Long originalTtl = 3600L;
        Long additionalSeconds = 1800L;
        EmbeddingCache cache = EmbeddingCache.fromTextWithTtl(
            TEST_TENANT_ID, TEST_TEXT, TEST_MODEL_NAME, TEST_EMBEDDING, originalTtl);
        
        // When
        cache.extendTtl(additionalSeconds);
        
        // Then
        Assertions.assertThat(cache.getTtl()).isEqualTo(originalTtl + additionalSeconds);
    }
    
    @Test
    @DisplayName("Should not extend TTL when TTL is null")
    void shouldNotExtendTtlWhenTtlIsNull() {
        // Given
        EmbeddingCache cache = EmbeddingCache.fromText(
            TEST_TENANT_ID, TEST_TEXT, TEST_MODEL_NAME, TEST_EMBEDDING);
        cache.setTtl(null);
        
        // When
        cache.extendTtl(1800L);
        
        // Then
        Assertions.assertThat(cache.getTtl()).isNull();
    }
    
    @Test
    @DisplayName("Should not extend TTL when additional seconds is null")
    void shouldNotExtendTtlWhenAdditionalSecondsIsNull() {
        // Given
        Long originalTtl = 3600L;
        EmbeddingCache cache = EmbeddingCache.fromTextWithTtl(
            TEST_TENANT_ID, TEST_TEXT, TEST_MODEL_NAME, TEST_EMBEDDING, originalTtl);
        
        // When
        cache.extendTtl(null);
        
        // Then
        Assertions.assertThat(cache.getTtl()).isEqualTo(originalTtl);
    }
    
    @Test
    @DisplayName("Should generate consistent hash for same text content")
    void shouldGenerateConsistentHashForSameTextContent() {
        // Given
        String text = "Consistent text for hashing";
        
        // When
        EmbeddingCache cache1 = EmbeddingCache.fromText(TEST_TENANT_ID, text, TEST_MODEL_NAME, TEST_EMBEDDING);
        EmbeddingCache cache2 = EmbeddingCache.fromText(TEST_TENANT_ID, text, TEST_MODEL_NAME, TEST_EMBEDDING);
        
        // Then
        Assertions.assertThat(cache1.getTextHash()).isEqualTo(cache2.getTextHash());
    }
    
    @Test
    @DisplayName("Should generate different hash for different text content")
    void shouldGenerateDifferentHashForDifferentTextContent() {
        // Given
        String text1 = "First text content";
        String text2 = "Second text content";
        
        // When
        EmbeddingCache cache1 = EmbeddingCache.fromText(TEST_TENANT_ID, text1, TEST_MODEL_NAME, TEST_EMBEDDING);
        EmbeddingCache cache2 = EmbeddingCache.fromText(TEST_TENANT_ID, text2, TEST_MODEL_NAME, TEST_EMBEDDING);
        
        // Then
        Assertions.assertThat(cache1.getTextHash()).isNotEqualTo(cache2.getTextHash());
    }
    
    @Test
    @DisplayName("Should generate meaningful toString representation")
    void shouldGenerateMeaningfulToStringRepresentation() {
        // Given
        EmbeddingCache cache = EmbeddingCache.fromText(
            TEST_TENANT_ID, TEST_TEXT, TEST_MODEL_NAME, TEST_EMBEDDING);
        
        // When
        String toString = cache.toString();
        
        // Then
        Assertions.assertThat(toString)
            .contains("EmbeddingCache{")
            .contains("tenantId=" + TEST_TENANT_ID)
            .contains("textHash=")
            .contains("modelName='" + TEST_MODEL_NAME + "'")
            .contains("dimension=5")
            .contains("cachedAt=")
            .contains("ttl=3600")
            .contains("remainingTtl=");
    }
    
    @Test
    @DisplayName("Should handle large embedding vectors in cache")
    void shouldHandleLargeEmbeddingVectorsInCache() {
        // Given - Create a realistic embedding size (1536 dimensions for OpenAI)
        List<Float> largeEmbedding = java.util.stream.IntStream.range(0, 1536)
            .mapToObj(i -> (float) Math.random())
            .toList();
        
        // When
        EmbeddingCache cache = EmbeddingCache.fromText(
            TEST_TENANT_ID, TEST_TEXT, TEST_MODEL_NAME, largeEmbedding);
        
        // Then
        Assertions.assertThat(cache.getEmbedding()).hasSize(1536);
        Assertions.assertThat(cache.getEmbedding()).isEqualTo(largeEmbedding);
        Assertions.assertThat(cache.toString()).contains("dimension=1536");
    }
}
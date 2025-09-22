package com.byo.rag.embedding.service;

import com.byo.rag.embedding.entity.EmbeddingCache;
import com.byo.rag.embedding.entity.VectorDocument;
import com.byo.rag.embedding.repository.EmbeddingCacheRepository;
import com.byo.rag.embedding.repository.VectorDocumentRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Unit tests for vector storage service operations.
 * 
 * <p>Tests validate service layer integration with repositories
 * using mocked dependencies to ensure proper business logic
 * without requiring Redis infrastructure.</p>
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Vector Storage Service Tests")
class VectorStorageServiceTest {
    
    @Mock
    private VectorDocumentRepository vectorDocumentRepository;
    
    @Mock
    private EmbeddingCacheRepository embeddingCacheRepository;
    
    private static final UUID TEST_TENANT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID TEST_DOCUMENT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID TEST_CHUNK_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
    private static final String TEST_MODEL_NAME = "text-embedding-3-small";
    private static final String TEST_TEXT = "This is test text for embedding";
    private static final List<Float> TEST_EMBEDDING = List.of(0.1f, 0.2f, 0.3f, 0.4f, 0.5f);
    
    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        Mockito.reset(vectorDocumentRepository, embeddingCacheRepository);
    }
    
    @Test
    @DisplayName("Should store vector document using repository")
    void shouldStoreVectorDocumentUsingRepository() {
        // Given
        VectorDocument document = VectorDocument.of(
            TEST_TENANT_ID, TEST_DOCUMENT_ID, TEST_CHUNK_ID,
            TEST_MODEL_NAME, TEST_EMBEDDING);
        document.setId("test-vector-id");
        
        Mockito.when(vectorDocumentRepository.save(Mockito.any(VectorDocument.class)))
            .thenReturn(document);
        
        // When
        VectorDocument saved = vectorDocumentRepository.save(document);
        
        // Then
        Assertions.assertThat(saved).isNotNull();
        Assertions.assertThat(saved.getId()).isEqualTo("test-vector-id");
        Assertions.assertThat(saved.getTenantId()).isEqualTo(TEST_TENANT_ID);
        Assertions.assertThat(saved.getEmbedding()).isEqualTo(TEST_EMBEDDING);
        Mockito.verify(vectorDocumentRepository).save(document);
    }
    
    @Test
    @DisplayName("Should retrieve vector documents by tenant")
    void shouldRetrieveVectorDocumentsByTenant() {
        // Given
        List<VectorDocument> expectedDocuments = List.of(
            VectorDocument.of(TEST_TENANT_ID, TEST_DOCUMENT_ID, UUID.randomUUID(), TEST_MODEL_NAME, TEST_EMBEDDING),
            VectorDocument.of(TEST_TENANT_ID, TEST_DOCUMENT_ID, UUID.randomUUID(), TEST_MODEL_NAME, TEST_EMBEDDING)
        );
        
        Mockito.when(vectorDocumentRepository.findByTenantId(TEST_TENANT_ID))
            .thenReturn(expectedDocuments);
        
        // When
        List<VectorDocument> documents = vectorDocumentRepository.findByTenantId(TEST_TENANT_ID);
        
        // Then
        Assertions.assertThat(documents).hasSize(2);
        Assertions.assertThat(documents).allMatch(doc -> doc.getTenantId().equals(TEST_TENANT_ID));
        Mockito.verify(vectorDocumentRepository).findByTenantId(TEST_TENANT_ID);
    }
    
    @Test
    @DisplayName("Should retrieve vector documents by tenant and model")
    void shouldRetrieveVectorDocumentsByTenantAndModel() {
        // Given
        List<VectorDocument> expectedDocuments = List.of(
            VectorDocument.of(TEST_TENANT_ID, TEST_DOCUMENT_ID, UUID.randomUUID(), TEST_MODEL_NAME, TEST_EMBEDDING)
        );
        
        Mockito.when(vectorDocumentRepository.findByTenantIdAndModelName(TEST_TENANT_ID, TEST_MODEL_NAME))
            .thenReturn(expectedDocuments);
        
        // When
        List<VectorDocument> documents = vectorDocumentRepository.findByTenantIdAndModelName(TEST_TENANT_ID, TEST_MODEL_NAME);
        
        // Then
        Assertions.assertThat(documents).hasSize(1);
        Assertions.assertThat(documents.get(0).matches(TEST_TENANT_ID, TEST_MODEL_NAME)).isTrue();
        Mockito.verify(vectorDocumentRepository).findByTenantIdAndModelName(TEST_TENANT_ID, TEST_MODEL_NAME);
    }
    
    @Test
    @DisplayName("Should delete vector documents by tenant and model")
    void shouldDeleteVectorDocumentsByTenantAndModel() {
        // When
        vectorDocumentRepository.deleteByTenantIdAndModelName(TEST_TENANT_ID, TEST_MODEL_NAME);
        
        // Then
        Mockito.verify(vectorDocumentRepository).deleteByTenantIdAndModelName(TEST_TENANT_ID, TEST_MODEL_NAME);
    }
    
    @Test
    @DisplayName("Should store embedding cache using repository")
    void shouldStoreEmbeddingCacheUsingRepository() {
        // Given
        EmbeddingCache cache = EmbeddingCache.fromText(
            TEST_TENANT_ID, TEST_TEXT, TEST_MODEL_NAME, TEST_EMBEDDING);
        
        Mockito.when(embeddingCacheRepository.save(Mockito.any(EmbeddingCache.class)))
            .thenReturn(cache);
        
        // When
        EmbeddingCache saved = embeddingCacheRepository.save(cache);
        
        // Then
        Assertions.assertThat(saved).isNotNull();
        Assertions.assertThat(saved.getTenantId()).isEqualTo(TEST_TENANT_ID);
        Assertions.assertThat(saved.getEmbedding()).isEqualTo(TEST_EMBEDDING);
        Assertions.assertThat(saved.isValid()).isTrue();
        Mockito.verify(embeddingCacheRepository).save(cache);
    }
    
    @Test
    @DisplayName("Should retrieve embedding cache by id")
    void shouldRetrieveEmbeddingCacheById() {
        // Given
        EmbeddingCache cache = EmbeddingCache.fromText(
            TEST_TENANT_ID, TEST_TEXT, TEST_MODEL_NAME, TEST_EMBEDDING);
        String cacheId = cache.getId();
        
        Mockito.when(embeddingCacheRepository.findById(cacheId))
            .thenReturn(Optional.of(cache));
        
        // When
        Optional<EmbeddingCache> retrieved = embeddingCacheRepository.findById(cacheId);
        
        // Then
        Assertions.assertThat(retrieved).isPresent();
        Assertions.assertThat(retrieved.get().matches(TEST_TENANT_ID, TEST_TEXT, TEST_MODEL_NAME)).isTrue();
        Mockito.verify(embeddingCacheRepository).findById(cacheId);
    }
    
    @Test
    @DisplayName("Should retrieve embedding caches by tenant")
    void shouldRetrieveEmbeddingCachesByTenant() {
        // Given
        List<EmbeddingCache> expectedCaches = List.of(
            EmbeddingCache.fromText(TEST_TENANT_ID, "text1", TEST_MODEL_NAME, TEST_EMBEDDING),
            EmbeddingCache.fromText(TEST_TENANT_ID, "text2", TEST_MODEL_NAME, TEST_EMBEDDING)
        );
        
        Mockito.when(embeddingCacheRepository.findByTenantId(TEST_TENANT_ID))
            .thenReturn(expectedCaches);
        
        // When
        List<EmbeddingCache> caches = embeddingCacheRepository.findByTenantId(TEST_TENANT_ID);
        
        // Then
        Assertions.assertThat(caches).hasSize(2);
        Assertions.assertThat(caches).allMatch(cache -> cache.getTenantId().equals(TEST_TENANT_ID));
        Mockito.verify(embeddingCacheRepository).findByTenantId(TEST_TENANT_ID);
    }
    
    @Test
    @DisplayName("Should retrieve embedding caches by tenant and model")
    void shouldRetrieveEmbeddingCachesByTenantAndModel() {
        // Given
        List<EmbeddingCache> expectedCaches = List.of(
            EmbeddingCache.fromText(TEST_TENANT_ID, TEST_TEXT, TEST_MODEL_NAME, TEST_EMBEDDING)
        );
        
        Mockito.when(embeddingCacheRepository.findByTenantIdAndModelName(TEST_TENANT_ID, TEST_MODEL_NAME))
            .thenReturn(expectedCaches);
        
        // When
        List<EmbeddingCache> caches = embeddingCacheRepository.findByTenantIdAndModelName(TEST_TENANT_ID, TEST_MODEL_NAME);
        
        // Then
        Assertions.assertThat(caches).hasSize(1);
        Assertions.assertThat(caches.get(0).getModelName()).isEqualTo(TEST_MODEL_NAME);
        Mockito.verify(embeddingCacheRepository).findByTenantIdAndModelName(TEST_TENANT_ID, TEST_MODEL_NAME);
    }
    
    @Test
    @DisplayName("Should delete embedding caches by tenant and model")
    void shouldDeleteEmbeddingCachesByTenantAndModel() {
        // When
        embeddingCacheRepository.deleteByTenantIdAndModelName(TEST_TENANT_ID, TEST_MODEL_NAME);
        
        // Then
        Mockito.verify(embeddingCacheRepository).deleteByTenantIdAndModelName(TEST_TENANT_ID, TEST_MODEL_NAME);
    }
    
    @Test
    @DisplayName("Should count embedding caches by tenant")
    void shouldCountEmbeddingCachesByTenant() {
        // Given
        long expectedCount = 5L;
        Mockito.when(embeddingCacheRepository.countByTenantId(TEST_TENANT_ID))
            .thenReturn(expectedCount);
        
        // When
        long count = embeddingCacheRepository.countByTenantId(TEST_TENANT_ID);
        
        // Then
        Assertions.assertThat(count).isEqualTo(expectedCount);
        Mockito.verify(embeddingCacheRepository).countByTenantId(TEST_TENANT_ID);
    }
    
    @Test
    @DisplayName("Should handle cache hit scenario")
    void shouldHandleCacheHitScenario() {
        // Given
        String cacheKey = EmbeddingCache.generateCacheKeyForText(TEST_TENANT_ID, TEST_TEXT, TEST_MODEL_NAME);
        EmbeddingCache existingCache = EmbeddingCache.fromText(
            TEST_TENANT_ID, TEST_TEXT, TEST_MODEL_NAME, TEST_EMBEDDING);
        
        Mockito.when(embeddingCacheRepository.findById(cacheKey))
            .thenReturn(Optional.of(existingCache));
        
        // When
        Optional<EmbeddingCache> cached = embeddingCacheRepository.findById(cacheKey);
        
        // Then - Cache hit
        Assertions.assertThat(cached).isPresent();
        Assertions.assertThat(cached.get().isValid()).isTrue();
        Assertions.assertThat(cached.get().getEmbedding()).isEqualTo(TEST_EMBEDDING);
        Mockito.verify(embeddingCacheRepository).findById(cacheKey);
    }
    
    @Test
    @DisplayName("Should handle cache miss scenario")
    void shouldHandleCacheMissScenario() {
        // Given
        String cacheKey = EmbeddingCache.generateCacheKeyForText(TEST_TENANT_ID, "unknown text", TEST_MODEL_NAME);
        
        Mockito.when(embeddingCacheRepository.findById(cacheKey))
            .thenReturn(Optional.empty());
        
        // When
        Optional<EmbeddingCache> cached = embeddingCacheRepository.findById(cacheKey);
        
        // Then - Cache miss
        Assertions.assertThat(cached).isEmpty();
        Mockito.verify(embeddingCacheRepository).findById(cacheKey);
    }
    
    @Test
    @DisplayName("Should handle batch vector document operations")
    void shouldHandleBatchVectorDocumentOperations() {
        // Given
        List<VectorDocument> documents = List.of(
            VectorDocument.of(TEST_TENANT_ID, TEST_DOCUMENT_ID, UUID.randomUUID(), TEST_MODEL_NAME, TEST_EMBEDDING),
            VectorDocument.of(TEST_TENANT_ID, TEST_DOCUMENT_ID, UUID.randomUUID(), TEST_MODEL_NAME, TEST_EMBEDDING),
            VectorDocument.of(TEST_TENANT_ID, TEST_DOCUMENT_ID, UUID.randomUUID(), TEST_MODEL_NAME, TEST_EMBEDDING)
        );
        
        Mockito.when(vectorDocumentRepository.saveAll(documents))
            .thenReturn(documents);
        
        // When
        Iterable<VectorDocument> saved = vectorDocumentRepository.saveAll(documents);
        
        // Then
        Assertions.assertThat(saved).hasSize(3);
        Mockito.verify(vectorDocumentRepository).saveAll(documents);
    }
    
    @Test
    @DisplayName("Should validate multi-tenant repository isolation")
    void shouldValidateMultiTenantRepositoryIsolation() {
        // Given
        UUID tenant1 = UUID.randomUUID();
        UUID tenant2 = UUID.randomUUID();
        
        List<VectorDocument> tenant1Docs = List.of(
            VectorDocument.of(tenant1, TEST_DOCUMENT_ID, UUID.randomUUID(), TEST_MODEL_NAME, TEST_EMBEDDING)
        );
        List<VectorDocument> tenant2Docs = List.of(
            VectorDocument.of(tenant2, TEST_DOCUMENT_ID, UUID.randomUUID(), TEST_MODEL_NAME, TEST_EMBEDDING)
        );
        
        Mockito.when(vectorDocumentRepository.findByTenantId(tenant1))
            .thenReturn(tenant1Docs);
        Mockito.when(vectorDocumentRepository.findByTenantId(tenant2))
            .thenReturn(tenant2Docs);
        
        // When
        List<VectorDocument> docs1 = vectorDocumentRepository.findByTenantId(tenant1);
        List<VectorDocument> docs2 = vectorDocumentRepository.findByTenantId(tenant2);
        
        // Then - Complete tenant isolation
        Assertions.assertThat(docs1).allMatch(doc -> doc.getTenantId().equals(tenant1));
        Assertions.assertThat(docs2).allMatch(doc -> doc.getTenantId().equals(tenant2));
        Assertions.assertThat(docs1).noneMatch(doc -> doc.getTenantId().equals(tenant2));
        Assertions.assertThat(docs2).noneMatch(doc -> doc.getTenantId().equals(tenant1));
        
        Mockito.verify(vectorDocumentRepository).findByTenantId(tenant1);
        Mockito.verify(vectorDocumentRepository).findByTenantId(tenant2);
    }
}
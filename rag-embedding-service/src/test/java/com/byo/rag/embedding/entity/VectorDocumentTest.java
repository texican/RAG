package com.byo.rag.embedding.entity;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Comprehensive unit tests for VectorDocument entity.
 * 
 * <p>Tests cover entity creation, factory methods, business logic,
 * metadata management, and data integrity validation.</p>
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 */
@DisplayName("VectorDocument Entity Tests")
class VectorDocumentTest {
    
    private static final UUID TEST_TENANT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID TEST_DOCUMENT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID TEST_CHUNK_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
    private static final String TEST_MODEL_NAME = "text-embedding-3-small";
    private static final List<Float> TEST_EMBEDDING = List.of(0.1f, 0.2f, 0.3f, 0.4f, 0.5f);
    
    @Test
    @DisplayName("Should create VectorDocument with default constructor")
    void shouldCreateVectorDocumentWithDefaultConstructor() {
        // When
        VectorDocument document = new VectorDocument();
        
        // Then
        Assertions.assertThat(document.getId()).isNull();
        Assertions.assertThat(document.getTenantId()).isNull();
        Assertions.assertThat(document.getDocumentId()).isNull();
        Assertions.assertThat(document.getChunkId()).isNull();
        Assertions.assertThat(document.getModelName()).isNull();
        Assertions.assertThat(document.getEmbedding()).isNull();
        Assertions.assertThat(document.getMetadata()).isNull();
        Assertions.assertThat(document.getCreatedAt()).isNotNull();
        Assertions.assertThat(document.getUpdatedAt()).isNotNull();
    }
    
    @Test
    @DisplayName("Should create VectorDocument with parameterized constructor")
    void shouldCreateVectorDocumentWithParameterizedConstructor() {
        // Given
        Map<String, Object> metadata = Map.of("source", "test", "confidence", 0.95);
        
        // When
        VectorDocument document = new VectorDocument(
            TEST_TENANT_ID, TEST_DOCUMENT_ID, TEST_CHUNK_ID, 
            TEST_MODEL_NAME, TEST_EMBEDDING, metadata);
        
        // Then
        Assertions.assertThat(document.getTenantId()).isEqualTo(TEST_TENANT_ID);
        Assertions.assertThat(document.getDocumentId()).isEqualTo(TEST_DOCUMENT_ID);
        Assertions.assertThat(document.getChunkId()).isEqualTo(TEST_CHUNK_ID);
        Assertions.assertThat(document.getModelName()).isEqualTo(TEST_MODEL_NAME);
        Assertions.assertThat(document.getEmbedding()).isEqualTo(TEST_EMBEDDING);
        Assertions.assertThat(document.getMetadata()).isEqualTo(metadata);
        Assertions.assertThat(document.getCreatedAt()).isNotNull();
        Assertions.assertThat(document.getUpdatedAt()).isNotNull();
    }
    
    @Test
    @DisplayName("Should create VectorDocument using factory method 'of'")
    void shouldCreateVectorDocumentUsingFactoryMethodOf() {
        // When
        VectorDocument document = VectorDocument.of(
            TEST_TENANT_ID, TEST_DOCUMENT_ID, TEST_CHUNK_ID, 
            TEST_MODEL_NAME, TEST_EMBEDDING);
        
        // Then
        Assertions.assertThat(document.getTenantId()).isEqualTo(TEST_TENANT_ID);
        Assertions.assertThat(document.getDocumentId()).isEqualTo(TEST_DOCUMENT_ID);
        Assertions.assertThat(document.getChunkId()).isEqualTo(TEST_CHUNK_ID);
        Assertions.assertThat(document.getModelName()).isEqualTo(TEST_MODEL_NAME);
        Assertions.assertThat(document.getEmbedding()).isEqualTo(TEST_EMBEDDING);
        Assertions.assertThat(document.getMetadata()).isEqualTo(Map.of());
        Assertions.assertThat(document.getCreatedAt()).isNotNull();
        Assertions.assertThat(document.getUpdatedAt()).isNotNull();
    }
    
    @Test
    @DisplayName("Should create VectorDocument using factory method 'withMetadata'")
    void shouldCreateVectorDocumentUsingFactoryMethodWithMetadata() {
        // Given
        Map<String, Object> metadata = Map.of(
            "source", "test_document.pdf",
            "page", 1,
            "confidence", 0.98
        );
        
        // When
        VectorDocument document = VectorDocument.withMetadata(
            TEST_TENANT_ID, TEST_DOCUMENT_ID, TEST_CHUNK_ID, 
            TEST_MODEL_NAME, TEST_EMBEDDING, metadata);
        
        // Then
        Assertions.assertThat(document.getTenantId()).isEqualTo(TEST_TENANT_ID);
        Assertions.assertThat(document.getDocumentId()).isEqualTo(TEST_DOCUMENT_ID);
        Assertions.assertThat(document.getChunkId()).isEqualTo(TEST_CHUNK_ID);
        Assertions.assertThat(document.getModelName()).isEqualTo(TEST_MODEL_NAME);
        Assertions.assertThat(document.getEmbedding()).isEqualTo(TEST_EMBEDDING);
        Assertions.assertThat(document.getMetadata()).isEqualTo(metadata);
    }
    
    @Test
    @DisplayName("Should update embedding and metadata correctly")
    void shouldUpdateEmbeddingAndMetadataCorrectly() {
        // Given
        VectorDocument document = VectorDocument.of(
            TEST_TENANT_ID, TEST_DOCUMENT_ID, TEST_CHUNK_ID, 
            TEST_MODEL_NAME, TEST_EMBEDDING);
        
        LocalDateTime originalUpdatedAt = document.getUpdatedAt();
        
        List<Float> newEmbedding = List.of(0.6f, 0.7f, 0.8f);
        Map<String, Object> newMetadata = Map.of("updated", true);
        
        // Wait a bit to ensure timestamp difference
        try { Thread.sleep(10); } catch (InterruptedException e) {}
        
        // When
        document.updateEmbedding(newEmbedding, newMetadata);
        
        // Then
        Assertions.assertThat(document.getEmbedding()).isEqualTo(newEmbedding);
        Assertions.assertThat(document.getMetadata()).isEqualTo(newMetadata);
        Assertions.assertThat(document.getUpdatedAt()).isAfter(originalUpdatedAt);
    }
    
    @Test
    @DisplayName("Should preserve existing metadata when updating with null metadata")
    void shouldPreserveExistingMetadataWhenUpdatingWithNullMetadata() {
        // Given
        Map<String, Object> originalMetadata = Map.of("source", "test");
        VectorDocument document = VectorDocument.withMetadata(
            TEST_TENANT_ID, TEST_DOCUMENT_ID, TEST_CHUNK_ID, 
            TEST_MODEL_NAME, TEST_EMBEDDING, originalMetadata);
        
        List<Float> newEmbedding = List.of(0.6f, 0.7f, 0.8f);
        
        // When
        document.updateEmbedding(newEmbedding, null);
        
        // Then
        Assertions.assertThat(document.getEmbedding()).isEqualTo(newEmbedding);
        Assertions.assertThat(document.getMetadata()).isEqualTo(originalMetadata);
    }
    
    @Test
    @DisplayName("Should calculate correct dimension for embedding vector")
    void shouldCalculateCorrectDimensionForEmbeddingVector() {
        // Given
        List<Float> embedding = List.of(0.1f, 0.2f, 0.3f, 0.4f, 0.5f);
        VectorDocument document = VectorDocument.of(
            TEST_TENANT_ID, TEST_DOCUMENT_ID, TEST_CHUNK_ID, 
            TEST_MODEL_NAME, embedding);
        
        // When
        int dimension = document.getDimension();
        
        // Then
        Assertions.assertThat(dimension).isEqualTo(5);
    }
    
    @Test
    @DisplayName("Should return zero dimension for null embedding")
    void shouldReturnZeroDimensionForNullEmbedding() {
        // Given
        VectorDocument document = new VectorDocument();
        document.setEmbedding(null);
        
        // When
        int dimension = document.getDimension();
        
        // Then
        Assertions.assertThat(dimension).isEqualTo(0);
    }
    
    @Test
    @DisplayName("Should match tenant and model correctly")
    void shouldMatchTenantAndModelCorrectly() {
        // Given
        VectorDocument document = VectorDocument.of(
            TEST_TENANT_ID, TEST_DOCUMENT_ID, TEST_CHUNK_ID, 
            TEST_MODEL_NAME, TEST_EMBEDDING);
        
        // When & Then - Positive match
        Assertions.assertThat(document.matches(TEST_TENANT_ID, TEST_MODEL_NAME)).isTrue();
        
        // When & Then - Negative matches
        UUID differentTenant = UUID.randomUUID();
        String differentModel = "different-model";
        
        Assertions.assertThat(document.matches(differentTenant, TEST_MODEL_NAME)).isFalse();
        Assertions.assertThat(document.matches(TEST_TENANT_ID, differentModel)).isFalse();
        Assertions.assertThat(document.matches(differentTenant, differentModel)).isFalse();
    }
    
    @Test
    @DisplayName("Should update timestamp when setting new embedding")
    void shouldUpdateTimestampWhenSettingNewEmbedding() {
        // Given
        VectorDocument document = VectorDocument.of(
            TEST_TENANT_ID, TEST_DOCUMENT_ID, TEST_CHUNK_ID, 
            TEST_MODEL_NAME, TEST_EMBEDDING);
        
        LocalDateTime originalUpdatedAt = document.getUpdatedAt();
        
        // Wait a bit to ensure timestamp difference
        try { Thread.sleep(10); } catch (InterruptedException e) {}
        
        // When
        document.setEmbedding(List.of(0.9f, 0.8f, 0.7f));
        
        // Then
        Assertions.assertThat(document.getUpdatedAt()).isAfter(originalUpdatedAt);
    }
    
    @Test
    @DisplayName("Should update timestamp when setting new metadata")
    void shouldUpdateTimestampWhenSettingNewMetadata() {
        // Given
        VectorDocument document = VectorDocument.of(
            TEST_TENANT_ID, TEST_DOCUMENT_ID, TEST_CHUNK_ID, 
            TEST_MODEL_NAME, TEST_EMBEDDING);
        
        LocalDateTime originalUpdatedAt = document.getUpdatedAt();
        
        // Wait a bit to ensure timestamp difference
        try { Thread.sleep(10); } catch (InterruptedException e) {}
        
        // When
        document.setMetadata(Map.of("new", "metadata"));
        
        // Then
        Assertions.assertThat(document.getUpdatedAt()).isAfter(originalUpdatedAt);
    }
    
    @Test
    @DisplayName("Should generate meaningful toString representation")
    void shouldGenerateMeaningfulToStringRepresentation() {
        // Given
        VectorDocument document = VectorDocument.of(
            TEST_TENANT_ID, TEST_DOCUMENT_ID, TEST_CHUNK_ID, 
            TEST_MODEL_NAME, TEST_EMBEDDING);
        document.setId("test-id");
        
        // When
        String toString = document.toString();
        
        // Then
        Assertions.assertThat(toString)
            .contains("VectorDocument{")
            .contains("id='test-id'")
            .contains("tenantId=" + TEST_TENANT_ID)
            .contains("documentId=" + TEST_DOCUMENT_ID)
            .contains("chunkId=" + TEST_CHUNK_ID)
            .contains("modelName='" + TEST_MODEL_NAME + "'")
            .contains("dimension=5")
            .contains("createdAt=")
            .contains("updatedAt=");
    }
    
    @Test
    @DisplayName("Should handle large embedding vectors efficiently")
    void shouldHandleLargeEmbeddingVectorsEfficiently() {
        // Given - Create a realistic embedding size (1536 dimensions for OpenAI)
        List<Float> largeEmbedding = java.util.stream.IntStream.range(0, 1536)
            .mapToObj(i -> (float) Math.random())
            .toList();
        
        // When
        VectorDocument document = VectorDocument.of(
            TEST_TENANT_ID, TEST_DOCUMENT_ID, TEST_CHUNK_ID, 
            TEST_MODEL_NAME, largeEmbedding);
        
        // Then
        Assertions.assertThat(document.getDimension()).isEqualTo(1536);
        Assertions.assertThat(document.getEmbedding()).hasSize(1536);
        Assertions.assertThat(document.getEmbedding()).isEqualTo(largeEmbedding);
    }
}
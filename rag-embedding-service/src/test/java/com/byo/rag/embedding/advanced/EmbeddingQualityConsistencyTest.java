package com.byo.rag.embedding.advanced;

import com.byo.rag.embedding.config.EmbeddingConfig.EmbeddingModelRegistry;
import com.byo.rag.embedding.dto.EmbeddingRequest;
import com.byo.rag.embedding.dto.EmbeddingResponse;
import com.byo.rag.embedding.service.EmbeddingCacheService;
import com.byo.rag.embedding.service.EmbeddingService;
import com.byo.rag.embedding.service.VectorStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.Embedding;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for embedding quality and consistency validation.
 * 
 * Part of EMBEDDING-TEST-003: Embedding Service Advanced Scenarios
 * Tests embedding quality, consistency, and accuracy metrics.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EMBEDDING-TEST-003: Embedding Quality and Consistency Tests")
class EmbeddingQualityConsistencyTest {

    @Mock
    private EmbeddingModelRegistry modelRegistry;
    
    @Mock
    private EmbeddingCacheService cacheService;
    
    @Mock
    private VectorStorageService vectorStorageService;
    
    @Mock
    private EmbeddingModel embeddingModel;
    
    private EmbeddingService embeddingService;
    
    private static final UUID TEST_TENANT_ID = UUID.randomUUID();
    private static final UUID TEST_DOCUMENT_ID = UUID.randomUUID();
    private static final String TEST_MODEL = "text-embedding-3-small";
    
    @BeforeEach
    void setUp() {
        embeddingService = new EmbeddingService(modelRegistry, cacheService, vectorStorageService);
        
        // Default mocks
        when(modelRegistry.hasModel(TEST_MODEL)).thenReturn(true);
        when(modelRegistry.getClient(TEST_MODEL)).thenReturn(embeddingModel);
        when(cacheService.getCachedEmbedding(any(), anyString(), anyString())).thenReturn(null);
    }
    
    @Test
    @DisplayName("Should generate consistent embeddings for identical text")
    void shouldGenerateConsistentEmbeddingsForIdenticalText() {
        // Arrange
        String text = "This is consistent test content for embedding quality validation.";
        UUID chunkId = UUID.randomUUID();
        List<Float> expectedEmbedding = List.of(0.1f, 0.2f, 0.3f, 0.4f, 0.5f);
        
        EmbeddingRequest request = new EmbeddingRequest(
            TEST_TENANT_ID, List.of(text), TEST_MODEL, TEST_DOCUMENT_ID, List.of(chunkId));
        
        mockDeterministicEmbeddingResponse(text, expectedEmbedding);
        
        // Act - Generate embeddings multiple times for same text
        EmbeddingResponse response1 = embeddingService.generateEmbeddings(request);
        EmbeddingResponse response2 = embeddingService.generateEmbeddings(request);
        EmbeddingResponse response3 = embeddingService.generateEmbeddings(request);
        
        // Assert - All responses should be identical
        assertEquals("SUCCESS", response1.status());
        assertEquals("SUCCESS", response2.status());
        assertEquals("SUCCESS", response3.status());
        
        List<Float> embedding1 = response1.embeddings().get(0).embedding();
        List<Float> embedding2 = response2.embeddings().get(0).embedding();
        List<Float> embedding3 = response3.embeddings().get(0).embedding();
        
        assertEquals(embedding1, embedding2);
        assertEquals(embedding2, embedding3);
        assertEquals(embedding1, embedding3);
    }
    
    @Test
    @DisplayName("Should maintain embedding dimension consistency")
    void shouldMaintainEmbeddingDimensionConsistency() {
        // Arrange
        List<String> texts = List.of(
            "Short text",
            "Medium length text with more content",
            "Very long text content that contains extensive information and detailed explanations that would test dimension consistency"
        );
        List<UUID> chunkIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        
        EmbeddingRequest request = new EmbeddingRequest(
            TEST_TENANT_ID, texts, TEST_MODEL, TEST_DOCUMENT_ID, chunkIds);
        
        mockVariableLengthEmbeddings(texts);
        
        // Act
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        
        // Assert
        assertEquals("SUCCESS", response.status());
        assertEquals(3, response.embeddings().size());
        
        // All embeddings should have same dimension
        int expectedDimension = response.embeddings().get(0).embedding().size();
        for (EmbeddingResponse.EmbeddingResult result : response.embeddings()) {
            assertEquals(expectedDimension, result.embedding().size(), 
                "All embeddings should have consistent dimensions");
            assertFalse(result.embedding().isEmpty(), "Embeddings should not be empty");
        }
    }
    
    @Test
    @DisplayName("Should handle similar texts with similar embeddings")
    void shouldHandleSimilarTextsWithSimilarEmbeddings() {
        // Arrange
        List<String> similarTexts = List.of(
            "The cat sat on the mat",
            "A cat was sitting on the mat",
            "The feline sat upon the rug"
        );
        List<UUID> chunkIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        
        EmbeddingRequest request = new EmbeddingRequest(
            TEST_TENANT_ID, similarTexts, TEST_MODEL, TEST_DOCUMENT_ID, chunkIds);
        
        mockSimilarTextEmbeddings(similarTexts);
        
        // Act
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        
        // Assert
        assertEquals("SUCCESS", response.status());
        assertEquals(3, response.embeddings().size());
        
        List<List<Float>> embeddings = response.embeddings().stream()
            .map(EmbeddingResponse.EmbeddingResult::embedding)
            .collect(java.util.stream.Collectors.toList());
        
        // Similar texts should have similar embeddings (high cosine similarity)
        double similarity1_2 = calculateCosineSimilarity(embeddings.get(0), embeddings.get(1));
        double similarity1_3 = calculateCosineSimilarity(embeddings.get(0), embeddings.get(2));
        double similarity2_3 = calculateCosineSimilarity(embeddings.get(1), embeddings.get(2));
        
        assertTrue(similarity1_2 > 0.7, "Similar texts should have high cosine similarity: " + similarity1_2);
        assertTrue(similarity1_3 > 0.6, "Related texts should have reasonable similarity: " + similarity1_3);
        assertTrue(similarity2_3 > 0.6, "Related texts should have reasonable similarity: " + similarity2_3);
    }
    
    @Test
    @DisplayName("Should handle different texts with different embeddings")
    void shouldHandleDifferentTextsWithDifferentEmbeddings() {
        // Arrange
        List<String> differentTexts = List.of(
            "The weather is sunny today",
            "Database optimization techniques",
            "Quantum computing algorithms"
        );
        List<UUID> chunkIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        
        EmbeddingRequest request = new EmbeddingRequest(
            TEST_TENANT_ID, differentTexts, TEST_MODEL, TEST_DOCUMENT_ID, chunkIds);
        
        mockDifferentTextEmbeddings(differentTexts);
        
        // Act
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        
        // Assert
        assertEquals("SUCCESS", response.status());
        assertEquals(3, response.embeddings().size());
        
        List<List<Float>> embeddings = response.embeddings().stream()
            .map(EmbeddingResponse.EmbeddingResult::embedding)
            .collect(java.util.stream.Collectors.toList());
        
        // Different texts should have different embeddings (low cosine similarity)
        double similarity1_2 = calculateCosineSimilarity(embeddings.get(0), embeddings.get(1));
        double similarity1_3 = calculateCosineSimilarity(embeddings.get(0), embeddings.get(2));
        double similarity2_3 = calculateCosineSimilarity(embeddings.get(1), embeddings.get(2));
        
        assertTrue(similarity1_2 < 0.5, "Different texts should have low similarity: " + similarity1_2);
        assertTrue(similarity1_3 < 0.5, "Different texts should have low similarity: " + similarity1_3);
        assertTrue(similarity2_3 < 0.5, "Different texts should have low similarity: " + similarity2_3);
    }
    
    @ParameterizedTest
    @ValueSource(ints = {384, 512, 768, 1024, 1536})
    @DisplayName("Should handle different embedding dimensions correctly")
    void shouldHandleDifferentEmbeddingDimensions(int dimension) {
        // Arrange
        String text = "Testing embedding dimension consistency";
        UUID chunkId = UUID.randomUUID();
        List<Float> expectedEmbedding = generateNormalizedEmbedding(dimension);
        
        EmbeddingRequest request = new EmbeddingRequest(
            TEST_TENANT_ID, List.of(text), TEST_MODEL, TEST_DOCUMENT_ID, List.of(chunkId));
        
        mockSpecificDimensionEmbedding(expectedEmbedding);
        
        // Act
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        
        // Assert
        assertEquals("SUCCESS", response.status());
        assertEquals(1, response.embeddings().size());
        
        List<Float> actualEmbedding = response.embeddings().get(0).embedding();
        assertEquals(dimension, actualEmbedding.size(), "Embedding should have expected dimension");
        
        // Verify embedding is normalized (magnitude close to 1.0)
        double magnitude = calculateMagnitude(actualEmbedding);
        assertTrue(Math.abs(magnitude - 1.0) < 0.1, "Embedding should be approximately normalized");
    }
    
    @Test
    @DisplayName("Should validate embedding value ranges")
    void shouldValidateEmbeddingValueRanges() {
        // Arrange
        String text = "Embedding value range validation test";
        UUID chunkId = UUID.randomUUID();
        List<Float> validEmbedding = List.of(-0.5f, -0.1f, 0.0f, 0.1f, 0.5f);
        
        EmbeddingRequest request = new EmbeddingRequest(
            TEST_TENANT_ID, List.of(text), TEST_MODEL, TEST_DOCUMENT_ID, List.of(chunkId));
        
        mockDeterministicEmbeddingResponse(text, validEmbedding);
        
        // Act
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        
        // Assert
        assertEquals("SUCCESS", response.status());
        List<Float> embedding = response.embeddings().get(0).embedding();
        
        // Validate embedding values are in reasonable range
        for (Float value : embedding) {
            assertNotNull(value, "Embedding values should not be null");
            assertFalse(value.isNaN(), "Embedding values should not be NaN");
            assertFalse(value.isInfinite(), "Embedding values should not be infinite");
            assertTrue(value >= -1.0f && value <= 1.0f, 
                "Embedding values should be in range [-1.0, 1.0], found: " + value);
        }
    }
    
    @Test
    @DisplayName("Should maintain consistency across multiple model instances")
    void shouldMaintainConsistencyAcrossModelInstances() {
        // Arrange
        String text = "Cross-model consistency test";
        UUID chunkId = UUID.randomUUID();
        List<Float> expectedEmbedding = List.of(0.2f, 0.4f, 0.6f, 0.8f, 1.0f);
        
        // Create multiple embedding service instances
        EmbeddingService service1 = new EmbeddingService(modelRegistry, cacheService, vectorStorageService);
        EmbeddingService service2 = new EmbeddingService(modelRegistry, cacheService, vectorStorageService);
        
        EmbeddingRequest request = new EmbeddingRequest(
            TEST_TENANT_ID, List.of(text), TEST_MODEL, TEST_DOCUMENT_ID, List.of(chunkId));
        
        mockDeterministicEmbeddingResponse(text, expectedEmbedding);
        
        // Act
        EmbeddingResponse response1 = service1.generateEmbeddings(request);
        EmbeddingResponse response2 = service2.generateEmbeddings(request);
        
        // Assert
        assertEquals("SUCCESS", response1.status());
        assertEquals("SUCCESS", response2.status());
        
        List<Float> embedding1 = response1.embeddings().get(0).embedding();
        List<Float> embedding2 = response2.embeddings().get(0).embedding();
        
        assertEquals(embedding1, embedding2, "Different service instances should produce identical embeddings");
    }
    
    @Test
    @DisplayName("Should handle text preprocessing consistently")
    void shouldHandleTextPreprocessingConsistently() {
        // Arrange - Texts with different formatting but same semantic content
        List<String> equivalentTexts = List.of(
            "Hello World",
            "hello world",
            "HELLO WORLD",
            " Hello World ",
            "Hello\nWorld",
            "Hello\tWorld"
        );
        List<UUID> chunkIds = equivalentTexts.stream()
            .map(text -> UUID.randomUUID())
            .collect(java.util.stream.Collectors.toList());
        
        EmbeddingRequest request = new EmbeddingRequest(
            TEST_TENANT_ID, equivalentTexts, TEST_MODEL, TEST_DOCUMENT_ID, chunkIds);
        
        mockTextPreprocessingEmbeddings(equivalentTexts);
        
        // Act
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        
        // Assert
        assertEquals("SUCCESS", response.status());
        assertEquals(6, response.embeddings().size());
        
        List<List<Float>> embeddings = response.embeddings().stream()
            .map(EmbeddingResponse.EmbeddingResult::embedding)
            .collect(java.util.stream.Collectors.toList());
        
        // Semantically similar texts should have high similarity even with different formatting
        for (int i = 0; i < embeddings.size(); i++) {
            for (int j = i + 1; j < embeddings.size(); j++) {
                double similarity = calculateCosineSimilarity(embeddings.get(i), embeddings.get(j));
                assertTrue(similarity > 0.8, 
                    String.format("Semantically equivalent texts should have high similarity: %f between '%s' and '%s'", 
                        similarity, equivalentTexts.get(i), equivalentTexts.get(j)));
            }
        }
    }
    
    private void mockDeterministicEmbeddingResponse(String text, List<Float> embedding) {
        Embedding mockSpringEmbedding = mock(Embedding.class);
        when(mockSpringEmbedding.getOutput()).thenReturn(
            embedding.stream()
                .map(Float::doubleValue)
                .collect(java.util.stream.Collectors.toList())
        );
        
        org.springframework.ai.embedding.EmbeddingResponse mockSpringResponse = 
            mock(org.springframework.ai.embedding.EmbeddingResponse.class);
        when(mockSpringResponse.getResults()).thenReturn(List.of(mockSpringEmbedding));
        
        when(embeddingModel.embedForResponse(any(List.class)))
            .thenReturn(mockSpringResponse);
    }
    
    private void mockVariableLengthEmbeddings(List<String> texts) {
        List<Embedding> mockEmbeddings = texts.stream()
            .map(text -> {
                Embedding embedding = mock(Embedding.class);
                List<Float> embeddingValues = generateConsistentEmbedding(5); // Consistent dimension
                when(embedding.getOutput()).thenReturn(
                    embeddingValues.stream()
                        .map(Float::doubleValue)
                        .collect(java.util.stream.Collectors.toList())
                );
                return embedding;
            })
            .collect(java.util.stream.Collectors.toList());
        
        org.springframework.ai.embedding.EmbeddingResponse mockSpringResponse = 
            mock(org.springframework.ai.embedding.EmbeddingResponse.class);
        when(mockSpringResponse.getResults()).thenReturn(mockEmbeddings);
        
        when(embeddingModel.embedForResponse(any(List.class)))
            .thenReturn(mockSpringResponse);
    }
    
    private void mockSimilarTextEmbeddings(List<String> texts) {
        // Generate similar embeddings for similar texts
        List<Embedding> mockEmbeddings = IntStream.range(0, texts.size())
            .mapToObj(i -> {
                Embedding embedding = mock(Embedding.class);
                List<Float> baseEmbedding = List.of(0.8f, 0.7f, 0.6f, 0.5f, 0.4f);
                List<Float> similarEmbedding = baseEmbedding.stream()
                    .map(val -> val + (i * 0.05f)) // Small variations
                    .collect(java.util.stream.Collectors.toList());
                when(embedding.getOutput()).thenReturn(
                    similarEmbedding.stream()
                        .map(Float::doubleValue)
                        .collect(java.util.stream.Collectors.toList())
                );
                return embedding;
            })
            .collect(java.util.stream.Collectors.toList());
        
        org.springframework.ai.embedding.EmbeddingResponse mockSpringResponse = 
            mock(org.springframework.ai.embedding.EmbeddingResponse.class);
        when(mockSpringResponse.getResults()).thenReturn(mockEmbeddings);
        
        when(embeddingModel.embedForResponse(any(List.class)))
            .thenReturn(mockSpringResponse);
    }
    
    private void mockDifferentTextEmbeddings(List<String> texts) {
        // Generate truly different embeddings for different texts
        List<Embedding> mockEmbeddings = IntStream.range(0, texts.size())
            .mapToObj(i -> {
                Embedding embedding = mock(Embedding.class);
                List<Float> differentEmbedding = IntStream.range(0, 5)
                    .mapToObj(j -> (float) (Math.sin(i * Math.PI * 2 + j * Math.PI) * 0.8 + Math.cos(i * j)))
                    .collect(java.util.stream.Collectors.toList());
                when(embedding.getOutput()).thenReturn(
                    differentEmbedding.stream()
                        .map(Float::doubleValue)
                        .collect(java.util.stream.Collectors.toList())
                );
                return embedding;
            })
            .collect(java.util.stream.Collectors.toList());
        
        org.springframework.ai.embedding.EmbeddingResponse mockSpringResponse = 
            mock(org.springframework.ai.embedding.EmbeddingResponse.class);
        when(mockSpringResponse.getResults()).thenReturn(mockEmbeddings);
        
        when(embeddingModel.embedForResponse(any(List.class)))
            .thenReturn(mockSpringResponse);
    }
    
    private void mockSpecificDimensionEmbedding(List<Float> embedding) {
        Embedding mockSpringEmbedding = mock(Embedding.class);
        when(mockSpringEmbedding.getOutput()).thenReturn(
            embedding.stream()
                .map(Float::doubleValue)
                .collect(java.util.stream.Collectors.toList())
        );
        
        org.springframework.ai.embedding.EmbeddingResponse mockSpringResponse = 
            mock(org.springframework.ai.embedding.EmbeddingResponse.class);
        when(mockSpringResponse.getResults()).thenReturn(List.of(mockSpringEmbedding));
        
        when(embeddingModel.embedForResponse(any(List.class)))
            .thenReturn(mockSpringResponse);
    }
    
    private void mockTextPreprocessingEmbeddings(List<String> texts) {
        // All equivalent texts should produce very similar embeddings
        List<Float> baseEmbedding = List.of(0.9f, 0.8f, 0.7f, 0.6f, 0.5f);
        
        List<Embedding> mockEmbeddings = texts.stream()
            .map(text -> {
                Embedding embedding = mock(Embedding.class);
                when(embedding.getOutput()).thenReturn(
                    baseEmbedding.stream()
                        .map(Float::doubleValue)
                        .collect(java.util.stream.Collectors.toList())
                );
                return embedding;
            })
            .collect(java.util.stream.Collectors.toList());
        
        org.springframework.ai.embedding.EmbeddingResponse mockSpringResponse = 
            mock(org.springframework.ai.embedding.EmbeddingResponse.class);
        when(mockSpringResponse.getResults()).thenReturn(mockEmbeddings);
        
        when(embeddingModel.embedForResponse(any(List.class)))
            .thenReturn(mockSpringResponse);
    }
    
    private List<Float> generateConsistentEmbedding(int dimension) {
        return IntStream.range(0, dimension)
            .mapToObj(i -> (float) (Math.sin(i * 0.1) * 0.5))
            .collect(java.util.stream.Collectors.toList());
    }
    
    private List<Float> generateNormalizedEmbedding(int dimension) {
        List<Float> embedding = IntStream.range(0, dimension)
            .mapToObj(i -> (float) (Math.random() - 0.5))
            .collect(java.util.stream.Collectors.toList());
        
        // Normalize to unit length
        double magnitude = calculateMagnitude(embedding);
        return embedding.stream()
            .map(val -> (float) (val / magnitude))
            .collect(java.util.stream.Collectors.toList());
    }
    
    private double calculateCosineSimilarity(List<Float> vec1, List<Float> vec2) {
        if (vec1.size() != vec2.size()) {
            throw new IllegalArgumentException("Vectors must have same dimension");
        }
        
        double dotProduct = 0.0;
        double magnitude1 = 0.0;
        double magnitude2 = 0.0;
        
        for (int i = 0; i < vec1.size(); i++) {
            dotProduct += vec1.get(i) * vec2.get(i);
            magnitude1 += vec1.get(i) * vec1.get(i);
            magnitude2 += vec2.get(i) * vec2.get(i);
        }
        
        magnitude1 = Math.sqrt(magnitude1);
        magnitude2 = Math.sqrt(magnitude2);
        
        if (magnitude1 == 0.0 || magnitude2 == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (magnitude1 * magnitude2);
    }
    
    private double calculateMagnitude(List<Float> vector) {
        double sum = vector.stream()
            .mapToDouble(val -> val * val)
            .sum();
        return Math.sqrt(sum);
    }
}
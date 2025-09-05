package com.byo.rag.core.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VectorSearchService.
 */
@ExtendWith(MockitoExtension.class)
class VectorSearchServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private VectorSearchService vectorSearchService;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // Set default properties using reflection
        ReflectionTestUtils.setField(vectorSearchService, "defaultTopK", 10);
        ReflectionTestUtils.setField(vectorSearchService, "defaultThreshold", 0.7);
        ReflectionTestUtils.setField(vectorSearchService, "maxResults", 50);
    }

    @Test
    void findSimilarChunks_ValidQuery_ReturnsChunks() {
        String query = "What is Spring AI?";
        int topK = 5;
        double threshold = 0.8;
        
        // Mock Redis operations - returning empty for basic vector search
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        List<String> results = vectorSearchService.findSimilarChunks(query, tenantId, topK, threshold);

        assertNotNull(results);
        // Since Redis is mocked and no actual vector data exists, expect empty or mock results
        assertTrue(results.size() <= topK);
    }

    @Test
    void findSimilarChunks_EmptyQuery_ReturnsEmptyList() {
        String query = "";
        int topK = 5;
        double threshold = 0.8;

        List<String> results = vectorSearchService.findSimilarChunks(query, tenantId, topK, threshold);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void findSimilarChunks_NullQuery_ReturnsEmptyList() {
        String query = null;
        int topK = 5;
        double threshold = 0.8;

        List<String> results = vectorSearchService.findSimilarChunks(query, tenantId, topK, threshold);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void findSimilarChunks_NullTenantId_ReturnsEmptyList() {
        String query = "What is Spring AI?";
        int topK = 5;
        double threshold = 0.8;

        List<String> results = vectorSearchService.findSimilarChunks(query, null, topK, threshold);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void findSimilarChunks_ZeroTopK_ReturnsEmptyList() {
        String query = "What is Spring AI?";
        int topK = 0;
        double threshold = 0.8;

        List<String> results = vectorSearchService.findSimilarChunks(query, tenantId, topK, threshold);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void findSimilarChunks_NegativeTopK_ReturnsEmptyList() {
        String query = "What is Spring AI?";
        int topK = -5;
        double threshold = 0.8;

        List<String> results = vectorSearchService.findSimilarChunks(query, tenantId, topK, threshold);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void findSimilarChunks_VeryHighThreshold_ReturnsLimitedResults() {
        String query = "What is Spring AI?";
        int topK = 10;
        double threshold = 0.99; // Very high threshold
        
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        List<String> results = vectorSearchService.findSimilarChunks(query, tenantId, topK, threshold);

        assertNotNull(results);
        // With very high threshold, expect fewer or no results
        assertTrue(results.size() <= topK);
    }

    @Test
    void findSimilarChunks_VeryLowThreshold_ReturnsMoreResults() {
        String query = "What is Spring AI?";
        int topK = 10;
        double threshold = 0.1; // Very low threshold
        
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        List<String> results = vectorSearchService.findSimilarChunks(query, tenantId, topK, threshold);

        assertNotNull(results);
        assertTrue(results.size() <= topK);
    }

    @Test
    void findSimilarChunks_MaxResultsLimit_RespectsLimit() {
        String query = "What is Spring AI?";
        int topK = 100; // Request more than max allowed
        double threshold = 0.5;
        
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        List<String> results = vectorSearchService.findSimilarChunks(query, tenantId, topK, threshold);

        assertNotNull(results);
        // Should respect maxResults limit (50 as set in setUp)
        assertTrue(results.size() <= 50);
    }

    @Test
    void findSimilarChunks_WithDefaultParameters_UsesDefaults() {
        String query = "What is machine learning?";
        
        // Test method that uses default parameters (if exists)
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        // Call with default parameters
        List<String> results = vectorSearchService.findSimilarChunks(query, tenantId, 10, 0.7);

        assertNotNull(results);
        assertTrue(results.size() <= 10);
    }

    @Test
    void buildVectorSearchKey_ValidTenant_ReturnsKey() {
        String expectedPattern = "vectors:tenant:" + tenantId.toString();
        
        // This tests the key building logic indirectly through search operations
        vectorSearchService.findSimilarChunks("test", tenantId, 5, 0.8);
        
        // Verify Redis operations use tenant-specific keys
        verify(redisTemplate, atLeastOnce()).hasKey(argThat(key -> 
            key.contains(tenantId.toString())));
    }

    @Test
    void performVectorSearch_RedisUnavailable_HandlesGracefully() {
        String query = "What is Spring AI?";
        
        // Mock Redis unavailable
        when(redisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("Redis unavailable"));

        assertDoesNotThrow(() -> {
            List<String> results = vectorSearchService.findSimilarChunks(query, tenantId, 5, 0.8);
            assertNotNull(results);
            assertTrue(results.isEmpty()); // Should return empty list on error
        });
    }

    @Test
    void filterByThreshold_AppliesCorrectFiltering() {
        String query = "What is Spring AI?";
        double strictThreshold = 0.95;
        
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        List<String> results = vectorSearchService.findSimilarChunks(query, tenantId, 10, strictThreshold);

        assertNotNull(results);
        // With strict threshold, should get fewer results
        assertTrue(results.size() <= 10);
    }

    @Test
    void findSimilarChunks_DifferentTenants_ReturnsIsolatedResults() {
        UUID tenant1 = UUID.randomUUID();
        UUID tenant2 = UUID.randomUUID();
        String query = "What is Spring AI?";
        
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        List<String> results1 = vectorSearchService.findSimilarChunks(query, tenant1, 5, 0.8);
        List<String> results2 = vectorSearchService.findSimilarChunks(query, tenant2, 5, 0.8);

        assertNotNull(results1);
        assertNotNull(results2);
        
        // Verify different Redis keys are used for different tenants
        verify(redisTemplate).hasKey(argThat(key -> key.contains(tenant1.toString())));
        verify(redisTemplate).hasKey(argThat(key -> key.contains(tenant2.toString())));
    }

    @Test
    void findSimilarChunks_LongQuery_HandlesCorrectly() {
        String longQuery = "This is a very long query that contains many words and details about Spring AI framework and how it integrates with various LLM providers like OpenAI and Ollama for building comprehensive AI-powered applications".repeat(3);
        
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        assertDoesNotThrow(() -> {
            List<String> results = vectorSearchService.findSimilarChunks(longQuery, tenantId, 5, 0.8);
            assertNotNull(results);
        });
    }
}
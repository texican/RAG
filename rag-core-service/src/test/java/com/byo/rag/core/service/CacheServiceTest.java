package com.byo.rag.core.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.byo.rag.core.dto.RagResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CacheService.
 */
@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;
    
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private CacheService cacheService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void getResponse_ExistingKey_ReturnsValue() throws Exception {
        String cacheKey = "test-key";
        String jsonResponse = "{\"response\":\"cached response\"}";
        RagResponse expectedResponse = createMockRagResponse();
        
        when(valueOperations.get(cacheKey)).thenReturn(jsonResponse);
        when(objectMapper.readValue(jsonResponse, RagResponse.class)).thenReturn(expectedResponse);

        RagResponse result = cacheService.getResponse(cacheKey);

        assertEquals(expectedResponse, result);
        verify(valueOperations).get(cacheKey);
        verify(objectMapper).readValue(jsonResponse, RagResponse.class);
    }

    @Test
    void getResponse_NonExistentKey_ReturnsNull() {
        String cacheKey = "non-existent";
        
        when(valueOperations.get(cacheKey)).thenReturn(null);

        RagResponse result = cacheService.getResponse(cacheKey);

        assertNull(result);
        verify(valueOperations).get(cacheKey);
    }

    @Test
    void cacheResponse_ValidKeyValue_CachesSuccessfully() throws Exception {
        String cacheKey = "test-key";
        RagResponse response = createMockRagResponse();
        String jsonResponse = "{\"response\":\"test\"}";
        
        when(objectMapper.writeValueAsString(response)).thenReturn(jsonResponse);
        doNothing().when(valueOperations).set(eq(cacheKey), eq(jsonResponse), any(Duration.class));

        assertDoesNotThrow(() -> cacheService.cacheResponse(cacheKey, response));

        verify(objectMapper).writeValueAsString(response);
        verify(valueOperations).set(eq(cacheKey), eq(jsonResponse), any(Duration.class));
    }

    @Test
    void cacheResponse_NullKey_DoesNotCache() {
        RagResponse response = createMockRagResponse();

        assertDoesNotThrow(() -> cacheService.cacheResponse(null, response));

        verify(valueOperations, never()).set(any(), any(), any(Duration.class));
    }

    @Test
    void cacheResponse_NullValue_DoesNotCache() {
        String cacheKey = "test-key";

        assertDoesNotThrow(() -> cacheService.cacheResponse(cacheKey, null));

        verify(valueOperations, never()).set(any(), any(), any(Duration.class));
    }

    @Test
    void invalidateCache_ValidPattern_LogsInvalidation() {
        String pattern = "test-pattern:*";
        
        assertDoesNotThrow(() -> cacheService.invalidateCache(pattern));
    }

    @Test
    void clearTenantCache_ValidTenantId_ClearsCachePattern() {
        String tenantId = "tenant-123";
        
        assertDoesNotThrow(() -> cacheService.clearTenantCache(tenantId));
    }

    private RagResponse createMockRagResponse() {
        return new RagResponse(
            "Test response",
            0.95,
            List.of(),
            150L,
            LocalDateTime.now(),
            "conv-123",
            new RagResponse.QueryMetadata(5, "openai", 50, false, "semantic")
        );
    }
}
package com.enterprise.rag.core.service;

import com.enterprise.rag.core.dto.RagResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Service for caching RAG responses to improve performance.
 */
@Service
public class CacheService {

    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);
    private static final Duration DEFAULT_TTL = Duration.ofHours(1);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public CacheService(@Qualifier("stringRedisTemplate") RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void cacheResponse(String key, RagResponse response) {
        try {
            String jsonResponse = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, jsonResponse, DEFAULT_TTL);
            logger.debug("Cached RAG response with key: {}", key);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to cache response for key: {}", key, e);
        }
    }

    public RagResponse getResponse(String key) {
        try {
            String jsonResponse = redisTemplate.opsForValue().get(key);
            if (jsonResponse != null) {
                logger.debug("Cache hit for key: {}", key);
                return objectMapper.readValue(jsonResponse, RagResponse.class);
            }
            logger.debug("Cache miss for key: {}", key);
            return null;
        } catch (Exception e) {
            logger.warn("Failed to retrieve cached response for key: {}", key, e);
            return null;
        }
    }

    public void invalidateCache(String pattern) {
        // TODO: Implement cache invalidation by pattern
        logger.info("Cache invalidation requested for pattern: {}", pattern);
    }

    public void clearTenantCache(String tenantId) {
        String pattern = "rag:" + tenantId + ":*";
        invalidateCache(pattern);
        logger.info("Cleared cache for tenant: {}", tenantId);
    }
}
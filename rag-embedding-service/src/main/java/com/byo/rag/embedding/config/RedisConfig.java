package com.byo.rag.embedding.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Redis configuration for vector storage operations.
 */
@Configuration
@EnableConfigurationProperties(RedisConfig.VectorStorageProperties.class)
public class RedisConfig {
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Use JSON serializer for values
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }
    
    @Bean
    public JedisPool jedisPool(VectorStorageProperties properties) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(properties.maxConnections());
        poolConfig.setMaxIdle(properties.maxIdle());
        poolConfig.setMinIdle(properties.minIdle());
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setTimeBetweenEvictionRuns(java.time.Duration.ofSeconds(30));
        poolConfig.setMinEvictableIdleDuration(java.time.Duration.ofMinutes(1));
        
        return new JedisPool(
            poolConfig,
            properties.host(),
            properties.port(),
            properties.timeout(),
            null,
            properties.database()
        );
    }
    
    /**
     * Configuration properties for vector storage in Redis.
     */
    @ConfigurationProperties(prefix = "embedding.vector.redis")
    public record VectorStorageProperties(
        String host,
        int port,
        int database,
        int timeout,
        String indexPrefix,
        int batchSize,
        int dimension,
        String similarityAlgorithm,
        int efConstruction,
        int efRuntime,
        int maxConnections,
        int maxIdle,
        int minIdle
    ) {
        public VectorStorageProperties() {
            this(
                "localhost",
                6379,
                2,
                2000,
                "rag:vectors",
                100,
                1536,
                "COSINE",
                200,
                10,
                16,
                8,
                2
            );
        }
    }
}
package com.byo.rag.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;

/**
 * Redis configuration for the Core Service.
 *
 * <p>Provides JedisPool for direct Redis operations, particularly for vector search
 * operations that require Redis Stack features like RediSearch.</p>
 *
 * <p>This configuration coexists with Spring Data Redis (Lettuce) which is used
 * for standard caching operations. JedisPool is specifically used by VectorSearchService
 * for vector similarity operations.</p>
 *
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 */
@Configuration
public class RedisConfig {

    private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:#{null}}")
    private String redisPassword;

    @Value("${spring.data.redis.database:1}")
    private int redisDatabase;

    @Value("${spring.data.redis.lettuce.pool.max-active:15}")
    private int maxTotal;

    @Value("${spring.data.redis.lettuce.pool.max-idle:10}")
    private int maxIdle;

    @Value("${spring.data.redis.lettuce.pool.min-idle:3}")
    private int minIdle;

    @Value("${redis.jedis.timeout:2000}")
    private int timeout;

    /**
     * Creates and configures a JedisPool for direct Redis operations.
     *
     * <p>This pool is used by VectorSearchService for vector similarity operations
     * that leverage Redis Stack's RediSearch capabilities.</p>
     *
     * @return configured JedisPool instance
     */
    @Bean
    public JedisPool jedisPool() {
        logger.info("Initializing JedisPool with host: {}, port: {}, database: {}",
            redisHost, redisPort, redisDatabase);

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(maxTotal);
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMinIdle(minIdle);
        poolConfig.setMaxWait(Duration.ofMillis(-1)); // No wait limit
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setMinEvictableIdleTime(Duration.ofSeconds(60));
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);

        JedisPool pool;
        if (redisPassword != null && !redisPassword.isEmpty()) {
            pool = new JedisPool(poolConfig, redisHost, redisPort, timeout, redisPassword, redisDatabase);
            logger.info("JedisPool created with authentication");
        } else {
            pool = new JedisPool(poolConfig, redisHost, redisPort, timeout, null, redisDatabase);
            logger.info("JedisPool created without authentication");
        }

        logger.info("JedisPool successfully initialized for vector operations");
        return pool;
    }
}

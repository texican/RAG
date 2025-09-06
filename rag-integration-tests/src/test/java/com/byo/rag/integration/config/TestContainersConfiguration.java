package com.byo.rag.integration.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * TestContainers configuration for integration testing.
 * 
 * This configuration sets up PostgreSQL and Redis containers that are shared
 * across all integration tests to improve performance and reduce resource usage.
 */
@TestConfiguration
public class TestContainersConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(TestContainersConfiguration.class);
    
    // Database configuration
    private static final String POSTGRES_IMAGE = "postgres:15-alpine";
    private static final String POSTGRES_DB = "test_rag_db";
    private static final String POSTGRES_USER = "test_user";
    private static final String POSTGRES_PASSWORD = "test_password";
    
    // Redis configuration
    private static final String REDIS_IMAGE = "redis/redis-stack:7.2.0-v6";
    private static final int REDIS_PORT = 6379;
    
    /**
     * PostgreSQL container for integration testing.
     * Uses a shared container instance to improve test performance.
     */
    @Bean(destroyMethod = "stop")
    @Primary
    public PostgreSQLContainer<?> postgreSQLContainer() {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE))
                .withDatabaseName(POSTGRES_DB)
                .withUsername(POSTGRES_USER)
                .withPassword(POSTGRES_PASSWORD)
                .withInitScript("init-test-db.sql") // Will create this script
                .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 1));
        
        container.start();
        logger.info("Started PostgreSQL container at: {}:{}", container.getHost(), container.getFirstMappedPort());
        
        return container;
    }
    
    /**
     * Redis container for integration testing.
     * Uses Redis Stack image to support vector operations.
     */
    @Bean(destroyMethod = "stop")
    @Primary
    public GenericContainer<?> redisContainer() {
        GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse(REDIS_IMAGE))
                .withExposedPorts(REDIS_PORT)
                .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*", 1));
        
        container.start();
        logger.info("Started Redis container at: {}:{}", container.getHost(), container.getFirstMappedPort());
        
        return container;
    }
    
    /**
     * Dynamically configure Spring properties for TestContainers.
     * This method sets up the database and Redis connection properties
     * based on the dynamically allocated container ports.
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry, 
                                   PostgreSQLContainer<?> postgres,
                                   GenericContainer<?> redis) {
        
        // Database properties
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        
        // JPA properties for testing
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "true");
        registry.add("spring.jpa.properties.hibernate.format_sql", () -> "true");
        
        // Redis properties  
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(REDIS_PORT).toString());
        registry.add("spring.data.redis.password", () -> "");
        
        // Disable AI auto-configuration for testing
        registry.add("spring.ai.openai.api-key", () -> "test-key");
        registry.add("spring.ai.openai.base-url", () -> "http://localhost:11434");
        
        // Logging configuration
        registry.add("logging.level.com.byo.rag", () -> "DEBUG");
        registry.add("logging.level.org.springframework.test", () -> "INFO");
    }
}
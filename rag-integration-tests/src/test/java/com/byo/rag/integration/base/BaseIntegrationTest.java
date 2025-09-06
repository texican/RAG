package com.byo.rag.integration.base;

import com.byo.rag.integration.config.TestContainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * Base class for integration tests that provides TestContainers setup
 * and common testing utilities.
 * 
 * This class sets up shared PostgreSQL and Redis containers that are reused
 * across all integration tests to improve performance and reduce resource usage.
 */
@SpringBootTest(classes = com.byo.rag.integration.config.IntegrationTestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("integration-test")
@Import(TestContainersConfiguration.class)
public abstract class BaseIntegrationTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    /**
     * Shared PostgreSQL container across all tests.
     * Using static container for better performance.
     */
    @Container
    protected static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test_rag_db")
            .withUsername("test_user")
            .withPassword("test_password");

    /**
     * Shared Redis container across all tests.
     * Using Redis Stack for vector operation support.
     */
    @Container
    protected static GenericContainer<?> redisContainer = new GenericContainer<>("redis/redis-stack:7.2.0-v6")
            .withExposedPorts(6379);

    /**
     * Configure dynamic properties for TestContainers.
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Database configuration
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // JPA configuration for testing
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "false"); // Reduce log noise
        registry.add("spring.jpa.properties.hibernate.format_sql", () -> "false");

        // Redis configuration
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379).toString());
        registry.add("spring.data.redis.password", () -> "");

        // Test-specific configurations
        registry.add("spring.ai.openai.api-key", () -> "test-key");
        registry.add("spring.ai.openai.base-url", () -> "http://localhost:11434");
        
        // Logging configuration
        registry.add("logging.level.com.byo.rag", () -> "DEBUG");
        registry.add("logging.level.org.springframework.web", () -> "INFO");
    }

    @BeforeEach
    void setUp() {
        // Configure RestTemplate with base URL
        restTemplate = new TestRestTemplate(new RestTemplateBuilder()
                .rootUri("http://localhost:" + port));
    }

    /**
     * Helper method to get the base URL for the application.
     */
    protected String getBaseUrl() {
        return "http://localhost:" + port;
    }

    /**
     * Helper method to create HTTP headers with JSON content type.
     */
    protected HttpHeaders createJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * Helper method to create HTTP headers with authentication.
     */
    protected HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = createJsonHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    /**
     * Get PostgreSQL container JDBC URL for direct database access if needed.
     */
    protected String getDatabaseUrl() {
        return postgreSQLContainer.getJdbcUrl();
    }

    /**
     * Get Redis container connection details if needed.
     */
    protected String getRedisHost() {
        return redisContainer.getHost();
    }

    protected Integer getRedisPort() {
        return redisContainer.getMappedPort(6379);
    }
}
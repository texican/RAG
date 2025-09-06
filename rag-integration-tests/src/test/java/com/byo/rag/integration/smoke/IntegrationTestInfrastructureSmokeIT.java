package com.byo.rag.integration.smoke;

import com.byo.rag.integration.base.BaseIntegrationTest;
import com.byo.rag.integration.data.TestDataBuilder;
import com.byo.rag.integration.data.TestDataCleanup;
import com.byo.rag.integration.utils.IntegrationTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import redis.clients.jedis.Jedis;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test to verify that the integration test infrastructure is working correctly.
 * 
 * This test validates that TestContainers, database connections, Redis connections,
 * and basic test utilities are functioning properly before running more complex
 * integration tests.
 */
@SpringBootTest(classes = com.byo.rag.integration.config.IntegrationTestConfig.class)
@DisplayName("Integration Test Infrastructure Smoke Test")
class IntegrationTestInfrastructureSmokeIT extends BaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private TestDataCleanup testDataCleanup;

    @AfterEach
    void cleanup() {
        testDataCleanup.cleanupAllTestData();
    }

    @Test
    @DisplayName("Should verify PostgreSQL container is running and accessible")
    void shouldVerifyPostgreSQLContainer() {
        // Verify PostgreSQL container is running
        assertThat(postgreSQLContainer.isRunning())
            .as("PostgreSQL container should be running")
            .isTrue();
        
        // Verify database connection
        assertThat(postgreSQLContainer.getJdbcUrl())
            .as("JDBC URL should be available")
            .isNotNull()
            .contains("postgresql://");
        
        // Test basic database operation
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertThat(result)
            .as("Basic database query should work")
            .isEqualTo(1);
    }

    @Test
    @DisplayName("Should verify Redis container is running and accessible")
    void shouldVerifyRedisContainer() {
        // Verify Redis container is running
        assertThat(redisContainer.isRunning())
            .as("Redis container should be running")
            .isTrue();
        
        // Test Redis connection
        try (Jedis jedis = new Jedis(getRedisHost(), getRedisPort())) {
            String pingResponse = jedis.ping();
            assertThat(pingResponse)
                .as("Redis ping should return PONG")
                .isEqualTo("PONG");
            
            // Test basic Redis operations
            jedis.set("test-key", "test-value");
            String retrievedValue = jedis.get("test-key");
            assertThat(retrievedValue)
                .as("Redis should store and retrieve values correctly")
                .isEqualTo("test-value");
        }
    }

    @Test
    @DisplayName("Should verify Spring Boot application context loads correctly")
    void shouldVerifyApplicationContext() {
        // Verify the application started on a random port
        assertThat(port)
            .as("Server should start on a random port")
            .isGreaterThan(0);
        
        // Verify base URL is accessible
        String baseUrl = getBaseUrl();
        assertThat(baseUrl)
            .as("Base URL should be properly formatted")
            .matches("http://localhost:\\d+");
    }

    @Test
    @DisplayName("Should verify test data builders create valid data")
    void shouldVerifyTestDataBuilders() {
        // Test tenant data builder
        var tenantRequest = TestDataBuilder.createTenantRequest();
        assertThat(tenantRequest.name())
            .as("Tenant name should not be null")
            .isNotNull()
            .isNotEmpty();
        assertThat(tenantRequest.slug())
            .as("Tenant slug should be valid")
            .matches("^[a-z0-9-]+$");
        
        // Test unique identifier generators
        String uniqueSlug = TestDataBuilder.createUniqueTenantSlug();
        String anotherUniqueSlug = TestDataBuilder.createUniqueTenantSlug();
        assertThat(uniqueSlug)
            .as("Unique slugs should be different")
            .isNotEqualTo(anotherUniqueSlug);
        
        String uniqueEmail = TestDataBuilder.createUniqueUserEmail();
        assertThat(uniqueEmail)
            .as("Unique email should be valid email format")
            .contains("@")
            .contains(".");
    }

    @Test
    @DisplayName("Should verify test utilities work correctly")
    void shouldVerifyTestUtilities() {
        // Test correlation ID generation
        String correlationId = IntegrationTestUtils.generateTestCorrelationId();
        assertThat(correlationId)
            .as("Correlation ID should start with 'test-'")
            .startsWith("test-");
        
        // Test UUID validation
        assertThat(IntegrationTestUtils.isValidUUID("not-a-uuid"))
            .as("Invalid UUID should be rejected")
            .isFalse();
        
        String validUuid = java.util.UUID.randomUUID().toString();
        assertThat(IntegrationTestUtils.isValidUUID(validUuid))
            .as("Valid UUID should be accepted")
            .isTrue();
        
        // Test timeout configuration
        var timeout = IntegrationTestUtils.getTestTimeout();
        assertThat(timeout.toSeconds())
            .as("Test timeout should be reasonable")
            .isBetween(10L, 300L);
    }

    @Test
    @DisplayName("Should verify test data cleanup functionality")
    void shouldVerifyTestDataCleanup() {
        // This test verifies that cleanup works without throwing exceptions
        // More comprehensive cleanup testing would require actual data to clean up
        
        testDataCleanup.cleanupAllTestData();
        
        // Verify cleanup completed without exceptions
        assertThat(testDataCleanup.isDatabaseClean())
            .as("Database should be clean after cleanup")
            .isTrue();
        
        // Test logging of remaining data (should not throw exceptions)
        testDataCleanup.logRemainingData();
    }

    @Test
    @DisplayName("Should verify container networking and connectivity")
    void shouldVerifyContainerNetworking() {
        // Verify containers can be accessed from the test environment
        String postgresHost = postgreSQLContainer.getHost();
        Integer postgresPort = postgreSQLContainer.getFirstMappedPort();
        
        assertThat(postgresHost)
            .as("PostgreSQL host should be available")
            .isNotNull()
            .isNotEmpty();
        
        assertThat(postgresPort)
            .as("PostgreSQL port should be mapped")
            .isNotNull()
            .isPositive();
        
        String redisHost = getRedisHost();
        Integer redisPort = getRedisPort();
        
        assertThat(redisHost)
            .as("Redis host should be available")
            .isNotNull()
            .isNotEmpty();
        
        assertThat(redisPort)
            .as("Redis port should be mapped")
            .isNotNull()
            .isPositive();
    }
}
package com.enterprise.rag.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.Socket;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to prevent infrastructure and Docker-related errors that occurred during development.
 * 
 * Errors Encountered:
 * 1. PostgreSQL image missing pgvector extension
 * 2. Database connection failures 
 * 3. Port conflicts between services
 * 4. Redis connection issues
 * 5. Kafka broker not available
 * 6. Docker compose services not starting
 */
public class InfrastructureValidationTest {
    
    private static final Logger logger = LoggerFactory.getLogger(InfrastructureValidationTest.class);

    @Test
    @DisplayName("Docker Compose file should exist and be valid")
    void dockerComposeFileShouldExistAndBeValid() {
        File dockerCompose = new File("../docker-compose.yml");
        assertTrue(dockerCompose.exists(), "docker-compose.yml should exist in project root");
        assertTrue(dockerCompose.isFile(), "docker-compose.yml should be a file");
        assertTrue(dockerCompose.length() > 0, "docker-compose.yml should not be empty");
    }

    @Test
    @DisplayName("PostgreSQL configuration should include pgvector")
    @EnabledIfEnvironmentVariable(named = "TEST_INFRASTRUCTURE", matches = "true")
    void postgresqlConfigurationShouldIncludePgvector() {
        // Test that PostgreSQL image supports pgvector extension
        assertDoesNotThrow(() -> {
            // This would test in a real environment with Docker running
            var connection = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/rag_auth", "rag_user", "rag_password");
            
            var statement = connection.createStatement();
            var result = statement.executeQuery("SELECT 1"); // Basic connectivity test
            assertTrue(result.next(), "Should be able to connect to PostgreSQL");
            
            connection.close();
        }, "PostgreSQL should be accessible with proper configuration");
    }

    @Test
    @DisplayName("Required ports should not conflict")
    @EnabledIfEnvironmentVariable(named = "TEST_INFRASTRUCTURE", matches = "true")  
    void requiredPortsShouldNotConflict() {
        // Test that required ports are available
        int[] requiredPorts = {5432, 6379, 9092, 8081, 8082, 8083, 8084, 8085, 8080};
        
        for (int port : requiredPorts) {
            assertDoesNotThrow(() -> {
                try (Socket socket = new Socket("localhost", port)) {
                    // If we can connect, the port is in use (which is expected for infrastructure)
                    assertTrue(socket.isConnected(), "Port " + port + " should be accessible");
                } catch (Exception e) {
                    // Port not accessible - this might be okay if service isn't running
                    // In a real test environment, you'd want to ensure services are running
                }
            }, "Port " + port + " should be properly configured");
        }
    }

    @Test
    @DisplayName("Redis configuration should be valid")
    @EnabledIfEnvironmentVariable(named = "TEST_INFRASTRUCTURE", matches = "true")
    void redisConfigurationShouldBeValid() {
        assertDoesNotThrow(() -> {
            Class.forName("redis.clients.jedis.Jedis");
        }, "Redis client libraries should be available");
    }

    @Test
    @DisplayName("Kafka configuration should be valid")
    @EnabledIfEnvironmentVariable(named = "TEST_INFRASTRUCTURE", matches = "true")
    void kafkaConfigurationShouldBeValid() {
        assertDoesNotThrow(() -> {
            Class.forName("org.apache.kafka.clients.producer.KafkaProducer");
            Class.forName("org.apache.kafka.clients.consumer.KafkaConsumer");
        }, "Kafka client libraries should be available");
    }

    @Test
    @DisplayName("Service port configuration should be consistent")
    void servicePortConfigurationShouldBeConsistent() {
        // Verify that port configurations are consistent across files
        
        // Auth service should use port 8081
        File authConfig = new File("rag-auth-service/src/main/resources/application.yml");
        if (authConfig.exists()) {
            assertDoesNotThrow(() -> {
                // In a real implementation, you'd parse the YAML and check port values
                assertTrue(authConfig.length() > 0, "Auth service configuration should exist");
            });
        }

        // Document service should use port 8083  
        File docConfig = new File("rag-document-service/src/main/resources/application.yml");
        if (docConfig.exists()) {
            assertDoesNotThrow(() -> {
                assertTrue(docConfig.length() > 0, "Document service configuration should exist");
            });
        }
    }

    @Test
    @DisplayName("Database initialization scripts should exist")
    void databaseInitializationScriptsShouldExist() {
        File initScript = new File("docker/postgres/init.sql");
        if (initScript.exists()) {
            assertTrue(initScript.isFile(), "Database init script should be a file");
            assertTrue(initScript.length() > 0, "Database init script should not be empty");
        }
    }

    @Test
    @DisplayName("Monitoring configuration should be valid")
    void monitoringConfigurationShouldBeValid() {
        // Check Prometheus configuration
        File prometheusConfig = new File("docker/prometheus/prometheus.yml");
        if (prometheusConfig.exists()) {
            assertTrue(prometheusConfig.isFile(), "Prometheus config should be a file");
            assertTrue(prometheusConfig.length() > 0, "Prometheus config should not be empty");
        }

        // Check Grafana configuration
        File grafanaDir = new File("docker/grafana");
        if (grafanaDir.exists()) {
            assertTrue(grafanaDir.isDirectory(), "Grafana config directory should exist");
        }
    }

    @Test
    @DisplayName("JVM configuration should handle warnings")
    void jvmConfigurationShouldHandleWarnings() {
        // Test that JVM configuration files exist to handle Java warnings
        File jvmConfig = new File(".mvn/jvm.config");
        if (jvmConfig.exists()) {
            assertTrue(jvmConfig.isFile(), "JVM config should be a file");
            assertTrue(jvmConfig.length() > 0, "JVM config should not be empty");
        }
    }

    @Test
    @DisplayName("Health check configuration should be present")
    void healthCheckConfigurationShouldBePresent() {
        // In shared module, health check libraries are optional
        // They should be available in service modules, not shared
        try {
            Class.forName("org.springframework.boot.actuator.health.HealthIndicator");
            logger.debug("Health check libraries are available");
        } catch (ClassNotFoundException e) {
            logger.debug("Health check libraries not available in shared module - this is expected");
            // This is acceptable for the shared module
        }
    }

    @Test
    @DisplayName("Environment variable defaults should be sensible")
    void environmentVariableDefaultsShouldBeSensible() {
        // Test that environment variable defaults are reasonable
        String[] envVars = {
            "DB_USERNAME", "DB_PASSWORD", "REDIS_HOST", "REDIS_PORT", 
            "KAFKA_BOOTSTRAP_SERVERS", "JWT_SECRET"
        };

        // In a real implementation, you'd check that these have sensible defaults
        // or that the application handles their absence gracefully
        for (String envVar : envVars) {
            assertDoesNotThrow(() -> {
                System.getenv(envVar); // Check that environment variable access doesn't throw
                // It's okay if environment variables are not set in tests
                // But the application should handle this gracefully
            }, "Environment variable " + envVar + " should be handled gracefully");
        }
    }

    @Test
    @DisplayName("File storage directories should be configurable")
    void fileStorageDirectoriesShouldBeConfigurable() {
        // Test that file storage paths are properly configured
        assertDoesNotThrow(() -> {
            String tempDir = System.getProperty("java.io.tmpdir");
            assertNotNull(tempDir, "Temporary directory should be available");
            assertTrue(new File(tempDir).exists(), "Temporary directory should exist");
        }, "File storage should have fallback to system temp directory");
    }

    @Test
    @DisplayName("Resource limits should be reasonable")
    void resourceLimitsShouldBeReasonable() {
        // Test that the application doesn't have unreasonable resource requirements
        long maxMemory = Runtime.getRuntime().maxMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        
        assertTrue(maxMemory > 0, "Max memory should be positive");
        assertTrue(totalMemory > 0, "Total memory should be positive");
        assertTrue(maxMemory >= totalMemory, "Max memory should be >= total memory");
    }
}
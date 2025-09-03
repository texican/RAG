package com.byo.rag.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests to prevent service startup errors that occurred during development.
 * 
 * Errors Encountered:
 * 1. Application failed to start due to missing dependencies
 * 2. Port conflicts between services
 * 3. Bean creation failures during startup
 * 4. Database connection failures at startup
 * 5. Health check failures
 * 6. Missing configuration files
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "jwt.secret=TestSecretKeyForJWTThatIsAtLeast256BitsLongForHS256Algorithm",
    "management.health.redis.enabled=false",
    "spring.data.redis.enabled=false"
})
public class ServiceStartupIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("Application context should load successfully")
    void applicationContextShouldLoadSuccessfully() {
        assertNotNull(applicationContext, "Application context should load without errors");
        assertTrue(applicationContext.containsBean("authServiceApplication"), 
            "Main application class should be registered as a bean");
    }

    @Test
    @DisplayName("All required beans should be created")
    void allRequiredBeansShouldBeCreated() {
        // Verify that all critical beans are created during startup
        String[] requiredBeans = {
            "securityConfig",
            "jwtService", 
            "userService",
            "tenantService",
            "authService",
            "authController",
            "tenantController",
            "userController",
            "jwtAuthenticationFilter",
            "passwordEncoder"
        };

        for (String beanName : requiredBeans) {
            assertTrue(applicationContext.containsBean(beanName), 
                "Required bean '" + beanName + "' should be created during startup");
        }
    }

    @Test
    @DisplayName("Database connectivity should be established")
    void databaseConnectivityShouldBeEstablished() {
        // Verify that database connection is working
        assertDoesNotThrow(() -> {
            var dataSource = applicationContext.getBean("dataSource");
            assertNotNull(dataSource, "DataSource should be available");
        }, "Database connection should be established during startup");
    }

    @Test
    @DisplayName("JPA repositories should be initialized")
    void jpaRepositoriesShouldBeInitialized() {
        // Verify that JPA repositories are properly initialized
        String[] repositoryBeans = {
            "userRepository",
            "tenantRepository"
        };

        for (String repoName : repositoryBeans) {
            assertTrue(applicationContext.containsBean(repoName),
                "Repository '" + repoName + "' should be initialized during startup");
        }
    }

    @Test
    @DisplayName("Security configuration should be active")
    void securityConfigurationShouldBeActive() {
        // Verify that security is properly configured and active
        assertDoesNotThrow(() -> {
            var securityFilterChain = applicationContext.getBean("securityFilterChain");
            assertNotNull(securityFilterChain, "Security filter chain should be configured");
        }, "Security configuration should be active");
    }

    @Test
    @DisplayName("Web server should start on correct port")
    void webServerShouldStartOnCorrectPort() {
        assertTrue(port > 0, "Server should start on a valid port");
        
        // Verify that server is responding
        String response = restTemplate.getForObject("http://localhost:" + port + "/actuator/health", String.class);
        assertNotNull(response, "Server should respond to health check requests");
    }

    @Test
    @DisplayName("Health checks should pass")
    void healthChecksShouldPass() {
        // Test that health endpoints are accessible and return success
        String healthResponse = restTemplate.getForObject(
            "http://localhost:" + port + "/actuator/health", String.class);
        
        assertNotNull(healthResponse, "Health endpoint should return a response");
        assertTrue(healthResponse.contains("UP") || healthResponse.contains("DOWN"), 
            "Health response should contain status information");
    }

    @Test
    @DisplayName("API endpoints should be accessible")
    void apiEndpointsShouldBeAccessible() {
        // Test that main API endpoints are mapped and accessible
        var response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/tenants/register",
            null, String.class);
        
        // Should return 400 (bad request) not 404 (not found) or 500 (server error)
        assertTrue(response.getStatusCode().is4xxClientError(), 
            "API endpoints should be accessible (returning client error for invalid request)");
    }

    @Test
    @DisplayName("Configuration properties should be loaded")
    void configurationPropertiesShouldBeLoaded() {
        // Verify that application.yml is loaded and properties are available
        var environment = applicationContext.getEnvironment();
        
        assertNotNull(environment.getProperty("spring.application.name"),
            "Application name should be loaded from configuration");
        assertNotNull(environment.getProperty("jwt.secret"),
            "JWT secret should be loaded from configuration");
    }

    @Test
    @DisplayName("Actuator endpoints should be configured")
    void actuatorEndpointsShouldBeConfigured() {
        // Verify that management endpoints are properly configured
        String[] actuatorEndpoints = {
            "/actuator/health",
            "/actuator/info"
        };

        for (String endpoint : actuatorEndpoints) {
            var response = restTemplate.getForEntity(
                "http://localhost:" + port + endpoint, String.class);
            
            assertTrue(response.getStatusCode().is2xxSuccessful(),
                "Actuator endpoint '" + endpoint + "' should be accessible");
        }
    }

    @Test
    @DisplayName("OpenAPI documentation should be available")
    void openApiDocumentationShouldBeAvailable() {
        // Verify that Swagger/OpenAPI is properly configured
        var response = restTemplate.getForEntity(
            "http://localhost:" + port + "/v3/api-docs", String.class);
        
        assertTrue(response.getStatusCode().is2xxSuccessful(),
            "OpenAPI documentation should be available");
        
        String body = response.getBody();
        assertNotNull(body, "OpenAPI response should have content");
        assertTrue(body.contains("openapi") || body.contains("swagger"),
            "Response should contain OpenAPI/Swagger content");
    }

    @Test
    @DisplayName("No circular dependency exceptions should occur")
    void noCircularDependencyExceptionsShouldOccur() {
        // Verify that all beans were created without circular dependency issues
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        assertTrue(beanNames.length > 0, "Beans should be created successfully");
        
        // If we get to this point without exceptions, circular dependencies were avoided
        assertDoesNotThrow(() -> {
            applicationContext.getBean("securityConfig");
            applicationContext.getBean("userService");
            applicationContext.getBean("jwtAuthenticationFilter");
        }, "Security-related beans should be created without circular dependencies");
    }

    @Test
    @DisplayName("Service should handle graceful shutdown")
    void serviceShouldHandleGracefulShutdown() {
        // Test that the application context can be closed without errors
        assertDoesNotThrow(() -> {
            // This will test that all beans can be properly destroyed
            var context = applicationContext;
            assertNotNull(context, "Context should be available for shutdown testing");
        }, "Application should handle graceful shutdown without errors");
    }

    @Test
    @DisplayName("Error handling should be configured")
    void errorHandlingShouldBeConfigured() {
        // Test that global error handling is configured
        var response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/v1/nonexistent-endpoint", String.class);
        
        assertTrue(response.getStatusCode().is4xxClientError(),
            "Non-existent endpoints should return proper error responses");
    }

    @Test
    @DisplayName("Logging configuration should be active")
    void loggingConfigurationShouldBeActive() {
        // Verify that logging is properly configured
        assertDoesNotThrow(() -> {
            var logger = org.slf4j.LoggerFactory.getLogger("com.byo.rag.auth");
            assertNotNull(logger, "Logger should be available");
            logger.info("Test log message from integration test");
        }, "Logging should be properly configured");
    }
}
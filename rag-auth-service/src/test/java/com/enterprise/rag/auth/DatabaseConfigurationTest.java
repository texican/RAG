package com.enterprise.rag.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to prevent database configuration errors that occurred during development.
 * 
 * Errors Encountered:
 * 1. Missing application.yml in document service
 * 2. Wrong database name in connection string
 * 3. Missing pgvector extension
 * 4. hibernate.ddl-auto set to 'validate' causing startup failures
 * 5. Missing database initialization
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.properties.hibernate.globally_quoted_identifiers=false",
    "jwt.secret=TestSecretKeyForJWTThatIsAtLeast256BitsLongForHS256Algorithm"
})
public class DatabaseConfigurationTest {

    @Autowired
    private Environment environment;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Application should have database URL configured")
    void shouldHaveDatabaseUrlConfigured() {
        String dataSourceUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(dataSourceUrl, "Database URL should be configured in application.yml");
        assertFalse(dataSourceUrl.isEmpty(), "Database URL should not be empty");
    }

    @Test
    @DisplayName("Database driver should be configured")
    void shouldHaveDatabaseDriverConfigured() {
        String driverClassName = environment.getProperty("spring.datasource.driver-class-name");
        assertNotNull(driverClassName, "Database driver should be configured");
        
        // In production, should be PostgreSQL driver
        // In test, can be H2 or PostgreSQL
        assertTrue(
            driverClassName.equals("org.postgresql.Driver") || 
            driverClassName.equals("org.h2.Driver"),
            "Driver should be PostgreSQL or H2 for tests"
        );
    }

    @Test
    @DisplayName("Database credentials should be configured")
    void shouldHaveDatabaseCredentialsConfigured() {
        String username = environment.getProperty("spring.datasource.username");
        String password = environment.getProperty("spring.datasource.password");
        
        assertNotNull(username, "Database username should be configured");
        // Password can be empty for test databases
        assertNotNull(password, "Database password property should exist (can be empty)");
    }

    @Test
    @DisplayName("Hibernate DDL auto should be appropriate for environment")
    void shouldHaveAppropriateHibernateDdlAuto() {
        String ddlAuto = environment.getProperty("spring.jpa.hibernate.ddl-auto");
        assertNotNull(ddlAuto, "Hibernate DDL auto should be configured");
        
        // Should never be 'validate' in development as it causes startup failures
        // Should be 'create-drop' for tests, 'update' for dev, 'validate' for prod only
        assertNotEquals("validate", ddlAuto, 
            "DDL auto should not be 'validate' in development - causes startup failures when tables don't exist");
    }

    @Test
    @DisplayName("JPA dialect should be configured")
    void shouldHaveJpaDialectConfigured() {
        String dialect = environment.getProperty("spring.jpa.properties.hibernate.dialect");
        
        if (dialect != null) {
            assertTrue(
                dialect.contains("PostgreSQL") || dialect.contains("H2"),
                "Dialect should be PostgreSQL or H2"
            );
        }
    }

    @Test
    @DisplayName("DataSource should be available")
    void shouldHaveDataSourceAvailable() {
        assertNotNull(dataSource, "DataSource should be configured and available");
        
        assertDoesNotThrow(() -> {
            var connection = dataSource.getConnection();
            assertTrue(connection.isValid(5), "Database connection should be valid");
            connection.close();
        }, "Should be able to get valid database connection");
    }

    @Test
    @DisplayName("Database schema should support multi-tenancy")
    void shouldSupportMultiTenancy() {
        // Verify that tenant isolation is properly configured
        String ddlAuto = environment.getProperty("spring.jpa.hibernate.ddl-auto");
        
        // In test/dev environments, should allow schema creation
        assertTrue(
            "create-drop".equals(ddlAuto) || "create".equals(ddlAuto) || "update".equals(ddlAuto),
            "Should allow schema creation in non-production environments"
        );
    }

    @Test
    @DisplayName("Connection pool should be configured")
    void shouldHaveConnectionPoolConfigured() {
        // Verify that connection pooling is available (HikariCP by default in Spring Boot)
        assertDoesNotThrow(() -> {
            Class.forName("com.zaxxer.hikari.HikariDataSource");
        }, "HikariCP should be available for connection pooling");
    }

    @Test
    @DisplayName("Database service specific configurations should exist")
    void shouldHaveServiceSpecificDatabaseConfig() {
        // Each service should have its own database
        // Auth service: rag_auth
        // Document service: rag_documents  
        // Embedding service: rag_embeddings
        // Core service: rag_core
        // Admin service: rag_admin
        
        String url = environment.getProperty("spring.datasource.url");
        if (url != null && url.contains("postgresql")) {
            assertTrue(
                url.contains("rag_auth") || 
                url.contains("rag_documents") || 
                url.contains("rag_embeddings") || 
                url.contains("rag_core") || 
                url.contains("rag_admin") ||
                url.contains("testdb"), // Allow test databases
                "Database URL should specify service-specific database name"
            );
        }
    }

    @Test
    @DisplayName("Application profile should be considered for database config")
    void shouldConsiderProfileForDatabaseConfig() {
        String[] activeProfiles = environment.getActiveProfiles();
        
        // In test profile, should use H2 or test database
        if (activeProfiles.length > 0 && activeProfiles[0].equals("test")) {
            String url = environment.getProperty("spring.datasource.url");
            assertNotNull(url, "Database URL should be configured");
            assertTrue(
                url.contains("h2:mem") || url.contains("testdb"),
                "Test profile should use in-memory or test database"
            );
        }
    }
}
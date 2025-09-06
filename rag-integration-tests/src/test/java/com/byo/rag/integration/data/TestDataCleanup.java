package com.byo.rag.integration.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.testcontainers.containers.GenericContainer;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.UUID;

/**
 * Utility class for cleaning up test data after integration tests.
 * 
 * This component provides methods to clean up database and Redis data
 * created during integration tests, ensuring tests don't interfere
 * with each other and maintaining a clean test environment.
 */
@Component
public class TestDataCleanup {
    
    private static final Logger logger = LoggerFactory.getLogger(TestDataCleanup.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private GenericContainer<?> redisContainer;
    
    /**
     * Clean up all test data from the database.
     * This method should be called after each integration test to ensure isolation.
     */
    public void cleanupAllTestData() {
        try {
            logger.debug("Starting test data cleanup");
            
            // Clean up in reverse order of dependencies
            cleanupDocumentChunks();
            cleanupDocuments();
            cleanupUsers();
            cleanupTenants();
            cleanupRedisData();
            
            logger.debug("Test data cleanup completed successfully");
        } catch (Exception e) {
            logger.warn("Error during test data cleanup: {}", e.getMessage(), e);
            // Don't fail the test due to cleanup errors, but log them
        }
    }
    
    /**
     * Clean up data for a specific tenant.
     */
    public void cleanupTenantData(UUID tenantId) {
        if (tenantId == null) {
            return;
        }
        
        try {
            logger.debug("Cleaning up data for tenant: {}", tenantId);
            
            // Clean up tenant-specific data
            cleanupTenantDocumentChunks(tenantId);
            cleanupTenantDocuments(tenantId);
            cleanupTenantUsers(tenantId);
            cleanupTenantRedisData(tenantId);
            
            // Finally clean up the tenant itself
            jdbcTemplate.update("DELETE FROM tenants WHERE id = ?", tenantId);
            
            logger.debug("Tenant data cleanup completed for: {}", tenantId);
        } catch (Exception e) {
            logger.warn("Error cleaning up tenant data for {}: {}", tenantId, e.getMessage(), e);
        }
    }
    
    /**
     * Clean up document chunks from database.
     */
    private void cleanupDocumentChunks() {
        try {
            int deleted = jdbcTemplate.update("DELETE FROM document_chunks WHERE id IN (SELECT id FROM document_chunks)");
            logger.debug("Deleted {} document chunks", deleted);
        } catch (Exception e) {
            logger.debug("Error cleaning up document chunks (table may not exist): {}", e.getMessage());
        }
    }
    
    /**
     * Clean up document chunks for a specific tenant.
     */
    private void cleanupTenantDocumentChunks(UUID tenantId) {
        try {
            int deleted = jdbcTemplate.update(
                "DELETE FROM document_chunks WHERE document_id IN (SELECT id FROM documents WHERE tenant_id = ?)",
                tenantId
            );
            logger.debug("Deleted {} document chunks for tenant {}", deleted, tenantId);
        } catch (Exception e) {
            logger.debug("Error cleaning up tenant document chunks: {}", e.getMessage());
        }
    }
    
    /**
     * Clean up documents from database.
     */
    private void cleanupDocuments() {
        try {
            int deleted = jdbcTemplate.update("DELETE FROM documents WHERE id IN (SELECT id FROM documents)");
            logger.debug("Deleted {} documents", deleted);
        } catch (Exception e) {
            logger.debug("Error cleaning up documents (table may not exist): {}", e.getMessage());
        }
    }
    
    /**
     * Clean up documents for a specific tenant.
     */
    private void cleanupTenantDocuments(UUID tenantId) {
        try {
            int deleted = jdbcTemplate.update("DELETE FROM documents WHERE tenant_id = ?", tenantId);
            logger.debug("Deleted {} documents for tenant {}", deleted, tenantId);
        } catch (Exception e) {
            logger.debug("Error cleaning up tenant documents: {}", e.getMessage());
        }
    }
    
    /**
     * Clean up users from database.
     */
    private void cleanupUsers() {
        try {
            int deleted = jdbcTemplate.update("DELETE FROM users WHERE id IN (SELECT id FROM users)");
            logger.debug("Deleted {} users", deleted);
        } catch (Exception e) {
            logger.debug("Error cleaning up users (table may not exist): {}", e.getMessage());
        }
    }
    
    /**
     * Clean up users for a specific tenant.
     */
    private void cleanupTenantUsers(UUID tenantId) {
        try {
            int deleted = jdbcTemplate.update("DELETE FROM users WHERE tenant_id = ?", tenantId);
            logger.debug("Deleted {} users for tenant {}", deleted, tenantId);
        } catch (Exception e) {
            logger.debug("Error cleaning up tenant users: {}", e.getMessage());
        }
    }
    
    /**
     * Clean up tenants from database.
     */
    private void cleanupTenants() {
        try {
            int deleted = jdbcTemplate.update("DELETE FROM tenants WHERE id IN (SELECT id FROM tenants)");
            logger.debug("Deleted {} tenants", deleted);
        } catch (Exception e) {
            logger.debug("Error cleaning up tenants (table may not exist): {}", e.getMessage());
        }
    }
    
    /**
     * Clean up all Redis data.
     */
    private void cleanupRedisData() {
        try (Jedis jedis = new Jedis(redisContainer.getHost(), redisContainer.getMappedPort(6379))) {
            jedis.flushDB();
            logger.debug("Cleaned up Redis data");
        } catch (Exception e) {
            logger.debug("Error cleaning up Redis data: {}", e.getMessage());
        }
    }
    
    /**
     * Clean up Redis data for a specific tenant.
     */
    private void cleanupTenantRedisData(UUID tenantId) {
        try (Jedis jedis = new Jedis(redisContainer.getHost(), redisContainer.getMappedPort(6379))) {
            // Clean up tenant-specific keys
            String pattern = "tenant:" + tenantId + ":*";
            var keys = jedis.keys(pattern);
            if (!keys.isEmpty()) {
                jedis.del(keys.toArray(new String[0]));
                logger.debug("Deleted {} Redis keys for tenant {}", keys.size(), tenantId);
            }
        } catch (Exception e) {
            logger.debug("Error cleaning up tenant Redis data: {}", e.getMessage());
        }
    }
    
    /**
     * Get count of remaining records in database (for debugging).
     */
    public void logRemainingData() {
        try {
            List<String> tables = List.of("tenants", "users", "documents", "document_chunks");
            
            for (String table : tables) {
                try {
                    Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
                    if (count != null && count > 0) {
                        logger.debug("Remaining {} records in table: {}", count, table);
                    }
                } catch (Exception e) {
                    logger.debug("Table {} may not exist or be accessible", table);
                }
            }
        } catch (Exception e) {
            logger.debug("Error checking remaining data: {}", e.getMessage());
        }
    }
    
    /**
     * Verify database is clean (useful for test assertions).
     */
    public boolean isDatabaseClean() {
        try {
            List<String> tables = List.of("tenants", "users", "documents", "document_chunks");
            
            for (String table : tables) {
                try {
                    Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
                    if (count != null && count > 0) {
                        logger.debug("Found {} records in table {}", count, table);
                        return false;
                    }
                } catch (Exception e) {
                    // Table doesn't exist or is empty - that's fine
                    logger.debug("Table {} not accessible (likely doesn't exist yet)", table);
                }
            }
            return true;
        } catch (Exception e) {
            logger.debug("Error checking database cleanliness: {}", e.getMessage());
            return false; // Conservative approach
        }
    }
}
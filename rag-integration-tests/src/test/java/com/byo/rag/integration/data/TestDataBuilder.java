package com.byo.rag.integration.data;

import com.byo.rag.shared.dto.TenantDto;
import com.byo.rag.shared.dto.UserDto;
import com.byo.rag.shared.entity.User;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Test data builder for creating consistent test data across integration tests.
 * 
 * This class provides factory methods for creating DTOs and entities with
 * realistic test data, ensuring consistent test scenarios and reducing
 * boilerplate code in integration tests.
 */
public final class TestDataBuilder {
    
    private TestDataBuilder() {
        // Utility class - prevent instantiation
    }

    // Default test values
    private static final String DEFAULT_TENANT_NAME = "Test Company";
    private static final String DEFAULT_TENANT_SLUG = "test-company";
    private static final String DEFAULT_USER_FIRST_NAME = "John";
    private static final String DEFAULT_USER_LAST_NAME = "Doe";
    private static final String DEFAULT_USER_EMAIL = "john.doe@test-company.com";
    private static final String DEFAULT_PASSWORD = "TestPassword123!";

    /**
     * Create a basic tenant creation request with default values.
     */
    public static TenantDto.CreateTenantRequest createTenantRequest() {
        return createTenantRequest(DEFAULT_TENANT_NAME, DEFAULT_TENANT_SLUG);
    }

    /**
     * Create a tenant creation request with custom name and slug.
     */
    public static TenantDto.CreateTenantRequest createTenantRequest(String name, String slug) {
        return new TenantDto.CreateTenantRequest(
            name,
            slug,
            "Integration test tenant for " + name,
            createDefaultTenantConfig()
        );
    }

    /**
     * Create a tenant configuration with test-appropriate defaults.
     */
    public static TenantDto.TenantConfig createDefaultTenantConfig() {
        return new TenantDto.TenantConfig(
            100,  // maxDocuments - smaller for testing
            1024L,  // maxStorageMb - 1GB for testing
            "test-embedding-model",
            "test-llm-model",
            createDefaultChunkingConfig()
        );
    }

    /**
     * Create a chunking configuration optimized for testing.
     */
    public static TenantDto.ChunkingConfig createDefaultChunkingConfig() {
        return new TenantDto.ChunkingConfig(
            256,  // smaller chunk size for faster testing
            32,   // smaller overlap for faster testing
            TenantDto.ChunkingStrategy.SEMANTIC
        );
    }

    /**
     * Create a user creation request with default values.
     */
    public static UserDto.CreateUserRequest createUserRequest(UUID tenantId) {
        return createUserRequest(
            DEFAULT_USER_FIRST_NAME,
            DEFAULT_USER_LAST_NAME,
            DEFAULT_USER_EMAIL,
            tenantId,
            User.UserRole.USER
        );
    }

    /**
     * Create a user creation request with custom values.
     */
    public static UserDto.CreateUserRequest createUserRequest(
            String firstName, 
            String lastName, 
            String email, 
            UUID tenantId,
            User.UserRole role) {
        return new UserDto.CreateUserRequest(
            firstName,
            lastName,
            email,
            DEFAULT_PASSWORD,
            role,
            tenantId
        );
    }

    /**
     * Create an admin user creation request.
     */
    public static UserDto.CreateUserRequest createAdminUserRequest(UUID tenantId) {
        return createUserRequest(
            "Admin",
            "User",
            "admin@test-company.com",
            tenantId,
            User.UserRole.ADMIN
        );
    }

    /**
     * Create a login request with default credentials.
     */
    public static UserDto.LoginRequest createLoginRequest() {
        return createLoginRequest(DEFAULT_USER_EMAIL, DEFAULT_PASSWORD);
    }

    /**
     * Create a login request with custom credentials.
     */
    public static UserDto.LoginRequest createLoginRequest(String email, String password) {
        return new UserDto.LoginRequest(email, password);
    }

    /**
     * Create test document metadata.
     */
    public static Map<String, Object> createDocumentMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("category", "test");
        metadata.put("source", "integration-test");
        metadata.put("priority", "medium");
        metadata.put("created_by_test", true);
        return metadata;
    }

    /**
     * Create test document metadata with custom values.
     */
    public static Map<String, Object> createDocumentMetadata(String category, String source) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("category", category);
        metadata.put("source", source);
        metadata.put("priority", "high");
        metadata.put("created_by_test", true);
        metadata.put("test_timestamp", LocalDateTime.now().toString());
        return metadata;
    }

    /**
     * Create a unique tenant slug for testing (avoids conflicts).
     */
    public static String createUniqueTenantSlug() {
        return "test-tenant-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Create a unique user email for testing (avoids conflicts).
     */
    public static String createUniqueUserEmail() {
        return "test-user-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    /**
     * Create multiple tenant requests for bulk testing.
     */
    public static TenantDto.CreateTenantRequest[] createMultipleTenantRequests(int count) {
        TenantDto.CreateTenantRequest[] requests = new TenantDto.CreateTenantRequest[count];
        for (int i = 0; i < count; i++) {
            requests[i] = createTenantRequest(
                "Test Company " + (i + 1),
                "test-company-" + (i + 1)
            );
        }
        return requests;
    }

    /**
     * Create multiple user requests for bulk testing.
     */
    public static UserDto.CreateUserRequest[] createMultipleUserRequests(UUID tenantId, int count) {
        UserDto.CreateUserRequest[] requests = new UserDto.CreateUserRequest[count];
        for (int i = 0; i < count; i++) {
            requests[i] = createUserRequest(
                "User" + (i + 1),
                "Test",
                "user" + (i + 1) + "@test-company.com",
                tenantId,
                User.UserRole.USER
            );
        }
        return requests;
    }

    /**
     * Create test content for document upload testing.
     */
    public static String createTestDocumentContent() {
        return """
            # Test Document for Integration Testing
            
            This is a test document created for integration testing of the RAG system.
            It contains multiple paragraphs and sections to test chunking algorithms.
            
            ## Section 1: Introduction
            
            This section introduces the concept of integration testing in distributed systems.
            Integration testing validates that different components work together correctly.
            
            ## Section 2: RAG Systems
            
            Retrieval Augmented Generation (RAG) systems combine retrieval with generation.
            They first retrieve relevant documents and then generate responses based on them.
            
            ## Section 3: Testing Strategies
            
            Effective testing requires both unit tests and integration tests.
            Unit tests validate individual components in isolation.
            Integration tests validate the entire system workflow.
            
            This document should be processed into multiple chunks for embedding generation.
            Each chunk should maintain semantic coherence while staying within token limits.
            """;
    }

    /**
     * Create larger test document content for performance testing.
     */
    public static String createLargeTestDocumentContent() {
        StringBuilder content = new StringBuilder();
        content.append("# Large Test Document\n\n");
        
        for (int i = 1; i <= 10; i++) {
            content.append("## Chapter ").append(i).append(": Test Content\n\n");
            for (int j = 1; j <= 5; j++) {
                content.append("This is paragraph ").append(j)
                       .append(" of chapter ").append(i)
                       .append(". It contains sufficient content to test chunking algorithms ")
                       .append("and ensure that large documents are processed correctly. ")
                       .append("The content is designed to be semantically meaningful ")
                       .append("while providing enough text for comprehensive testing.\n\n");
            }
        }
        
        return content.toString();
    }
}
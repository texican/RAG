package com.enterprise.rag.document;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests to prevent API endpoint validation errors that occurred during development.
 * 
 * Errors Encountered:
 * 1. 404 Not Found for document upload endpoint (controller not created)
 * 2. Missing request mapping annotations
 * 3. Wrong HTTP method mappings
 * 4. Missing required headers validation
 * 5. Incorrect parameter binding
 * 6. Method signature mismatches with service layer
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
public class ApiEndpointValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Test
    @DisplayName("Document upload endpoint should exist")
    void documentUploadEndpointShouldExist() throws Exception {
        // This test prevents 404 errors when endpoints are not properly mapped
        // The endpoint should return 400 Bad Request when required file parameter is missing
        mockMvc.perform(post("/api/v1/documents/upload")
                .header("X-Tenant-ID", "550e8400-e29b-41d4-a716-446655440000")
                .contentType("multipart/form-data"))
                .andExpect(status().isBadRequest()); // Should not be 404 Not Found, expects file parameter
    }

    @Test
    @DisplayName("Document controller should be properly registered")
    void documentControllerShouldBeProperlyRegistered() {
        // Verify that DocumentController is registered as a Spring bean
        assertDoesNotThrow(() -> {
            webApplicationContext.getBean("documentController");
        }, "DocumentController should be registered as a Spring bean with @RestController");
    }

    @Test
    @DisplayName("All required endpoints should be mapped")
    void allRequiredEndpointsShouldBeMapped() throws Exception {
        String tenantId = "550e8400-e29b-41d4-a716-446655440000";

        // GET /api/v1/documents - list documents
        mockMvc.perform(get("/api/v1/documents")
                .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk());

        // GET /api/v1/documents/{id} - get specific document
        mockMvc.perform(get("/api/v1/documents/550e8400-e29b-41d4-a716-446655440000")
                .header("X-Tenant-ID", tenantId))
                .andExpect(status().isNotFound()); // Should be mapped, but return 404 for non-existent document

        // DELETE /api/v1/documents/{id} - delete document
        mockMvc.perform(delete("/api/v1/documents/550e8400-e29b-41d4-a716-446655440000")
                .header("X-Tenant-ID", tenantId))
                .andExpect(status().isNotFound()); // Should be mapped

        // PUT /api/v1/documents/{id} - update document
        mockMvc.perform(put("/api/v1/documents/550e8400-e29b-41d4-a716-446655440000")
                .header("X-Tenant-ID", tenantId)
                .contentType("application/json")
                .content("{}"))
                .andExpect(status().isBadRequest()); // Should be mapped, but return 400 for invalid request
    }

    @Test
    @DisplayName("Required headers should be validated")
    void requiredHeadersShouldBeValidated() throws Exception {
        // Test that X-Tenant-ID header is required
        mockMvc.perform(post("/api/v1/documents/upload")
                .contentType("multipart/form-data"))
                .andExpect(status().isBadRequest()); // Should fail without tenant header

        // Test with invalid tenant ID format
        mockMvc.perform(post("/api/v1/documents/upload")
                .header("X-Tenant-ID", "invalid-uuid")
                .contentType("multipart/form-data"))
                .andExpect(status().isBadRequest()); // Should validate UUID format
    }

    @Test
    @DisplayName("Content type validation should work")
    void contentTypeValidationShouldWork() throws Exception {
        // Test multipart form data for file upload
        mockMvc.perform(post("/api/v1/documents/upload")
                .header("X-Tenant-ID", "550e8400-e29b-41d4-a716-446655440000")
                .contentType("application/json"))
                .andExpect(status().isUnsupportedMediaType()); // Should expect multipart/form-data, not JSON

        // Test JSON content type for updates
        mockMvc.perform(put("/api/v1/documents/550e8400-e29b-41d4-a716-446655440000")
                .header("X-Tenant-ID", "550e8400-e29b-41d4-a716-446655440000")
                .contentType("text/plain"))
                .andExpect(status().isUnsupportedMediaType()); // Should expect application/json
    }

    @Test
    @DisplayName("HTTP method mapping should be correct")
    void httpMethodMappingShouldBeCorrect() throws Exception {
        String tenantId = "550e8400-e29b-41d4-a716-446655440000";
        String documentId = "550e8400-e29b-41d4-a716-446655440001";

        // POST for upload (creation)
        mockMvc.perform(post("/api/v1/documents/upload")
                .header("X-Tenant-ID", tenantId)
                .contentType("multipart/form-data"))
                .andExpect(status().isBadRequest()); // Should accept POST

        // GET for retrieval
        mockMvc.perform(get("/api/v1/documents")
                .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk()); // Should accept GET

        // PUT for updates
        mockMvc.perform(put("/api/v1/documents/" + documentId)
                .header("X-Tenant-ID", tenantId)
                .contentType("application/json")
                .content("{}"))
                .andExpect(status().isBadRequest()); // Should accept PUT

        // DELETE for removal
        mockMvc.perform(delete("/api/v1/documents/" + documentId)
                .header("X-Tenant-ID", tenantId))
                .andExpect(status().isNotFound()); // Should accept DELETE

        // Wrong methods should not be allowed
        mockMvc.perform(patch("/api/v1/documents/upload")
                .header("X-Tenant-ID", tenantId)
                .contentType("multipart/form-data"))
                .andExpect(status().isMethodNotAllowed()); // PATCH not supported for upload
    }

    @Test
    @DisplayName("Parameter binding should work correctly")
    void parameterBindingShouldWorkCorrectly() throws Exception {
        // Test path variable binding
        mockMvc.perform(get("/api/v1/documents/not-a-uuid")
                .header("X-Tenant-ID", "550e8400-e29b-41d4-a716-446655440000"))
                .andExpect(status().isBadRequest()); // Should validate UUID format in path

        // Test query parameter binding for pagination
        mockMvc.perform(get("/api/v1/documents")
                .header("X-Tenant-ID", "550e8400-e29b-41d4-a716-446655440000")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk()); // Should handle pagination parameters
    }

    @Test
    @DisplayName("Request validation should work")
    void requestValidationShouldWork() throws Exception {
        // Test that validation annotations work
        mockMvc.perform(put("/api/v1/documents/550e8400-e29b-41d4-a716-446655440000")
                .header("X-Tenant-ID", "550e8400-e29b-41d4-a716-446655440000")
                .contentType("application/json")
                .content("{}")) // Empty request body
                .andExpect(status().isBadRequest()); // Should validate request body
    }

    @Test
    @DisplayName("Service layer integration should work")
    void serviceLayerIntegrationShouldWork() {
        // Verify that controller can access service layer
        assertDoesNotThrow(() -> {
            var documentService = webApplicationContext.getBean("documentService");
            assertNotNull(documentService, "DocumentService should be available for injection");
        });
    }

    @Test
    @DisplayName("Exception handling should be configured")
    void exceptionHandlingShouldBeConfigured() throws Exception {
        // Test that exceptions are properly handled and don't cause 500 errors
        mockMvc.perform(get("/api/v1/documents/550e8400-e29b-41d4-a716-446655440000")
                .header("X-Tenant-ID", "550e8400-e29b-41d4-a716-446655440000"))
                .andExpect(status().isNotFound()) // Should return 404, not 500
                .andExpect(content().contentType("application/json")); // Should return JSON error response
    }

    @Test
    @DisplayName("OpenAPI documentation should be generated")
    void openApiDocumentationShouldBeGenerated() throws Exception {
        // Verify that Swagger/OpenAPI documentation is generated for endpoints
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    @DisplayName("Response format should be consistent")
    void responseFormatShouldBeConsistent() throws Exception {
        // Verify that successful responses return proper JSON structure
        mockMvc.perform(get("/api/v1/documents")
                .header("X-Tenant-ID", "550e8400-e29b-41d4-a716-446655440000"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.content").exists()) // Paginated response should have content
                .andExpect(jsonPath("$.totalElements").exists()); // Should have pagination info
    }
}
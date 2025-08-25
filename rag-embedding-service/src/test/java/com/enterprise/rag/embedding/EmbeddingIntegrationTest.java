package com.enterprise.rag.embedding;

import com.enterprise.rag.embedding.dto.EmbeddingRequest;
import com.enterprise.rag.embedding.dto.SearchRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for embedding service endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
public class EmbeddingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @DisplayName("Embedding service should start successfully")
    void embeddingServiceShouldStart() {
        // This test passes if the Spring context loads successfully
    }
    
    @Test
    @DisplayName("Health check endpoint should return UP status")
    void healthCheckShouldReturnUp() throws Exception {
        mockMvc.perform(get("/api/v1/embeddings/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.service").value("rag-embedding-service"));
    }
    
    @Test
    @DisplayName("Available models endpoint should return model information")
    void availableModelsShouldReturnModelInfo() throws Exception {
        mockMvc.perform(get("/api/v1/embeddings/models"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.default").exists())
            .andExpect(jsonPath("$.available_models").isArray())
            .andExpect(jsonPath("$.model_dimensions").exists());
    }
    
    @Test
    @DisplayName("Stats endpoint should return statistics")
    void statsShouldReturnStatistics() throws Exception {
        UUID tenantId = UUID.randomUUID();
        
        mockMvc.perform(get("/api/v1/embeddings/stats")
                .header("X-Tenant-ID", tenantId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tenant_id").value(tenantId.toString()))
            .andExpect(jsonPath("$.vector_storage").exists())
            .andExpect(jsonPath("$.cache").exists());
    }
    
    @Test
    @DisplayName("Generate embeddings should validate tenant header")
    void generateEmbeddingsShouldValidateTenantHeader() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        
        EmbeddingRequest request = EmbeddingRequest.singleText(
            tenantId, "Test text", null, documentId, null);
        
        // Without tenant header - should fail
        mockMvc.perform(post("/api/v1/embeddings/generate")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Search should validate tenant header")
    void searchShouldValidateTenantHeader() throws Exception {
        UUID tenantId = UUID.randomUUID();
        
        SearchRequest request = SearchRequest.simple(tenantId, "test query");
        
        // Without tenant header - should fail
        mockMvc.perform(post("/api/v1/embeddings/search")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Cache invalidation should work")
    void cacheInvalidationShouldWork() throws Exception {
        UUID tenantId = UUID.randomUUID();
        
        mockMvc.perform(delete("/api/v1/embeddings/cache")
                .header("X-Tenant-ID", tenantId))
            .andExpect(status().isNoContent());
    }
    
    @Test
    @DisplayName("Document vector deletion should work")
    void documentVectorDeletionShouldWork() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        
        mockMvc.perform(delete("/api/v1/embeddings/documents/" + documentId)
                .header("X-Tenant-ID", tenantId))
            .andExpect(status().isNoContent());
    }
}
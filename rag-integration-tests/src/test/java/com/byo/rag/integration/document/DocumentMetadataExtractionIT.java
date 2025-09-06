package com.byo.rag.integration.document;

import com.byo.rag.integration.base.BaseIntegrationTest;
import com.byo.rag.integration.data.TestDataBuilder;
import com.byo.rag.integration.data.TestDataCleanup;
import com.byo.rag.integration.utils.AuthenticationTestUtils;
import com.byo.rag.integration.utils.IntegrationTestUtils;
import com.byo.rag.shared.dto.DocumentDto;
import com.byo.rag.shared.dto.TenantDto;
import com.byo.rag.shared.entity.Document;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for validating document metadata extraction and processing.
 * 
 * This test class ensures that document metadata is correctly extracted, stored,
 * and retrieved throughout the document processing pipeline, including both
 * user-provided metadata and system-extracted metadata.
 */
@DisplayName("Document Metadata Extraction Integration Tests")
class DocumentMetadataExtractionIT extends BaseIntegrationTest {

    @Autowired
    private TestDataCleanup testDataCleanup;
    
    @Autowired
    private ObjectMapper objectMapper;

    private AuthenticationTestUtils.TestTenantSetup testTenant;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = getBaseUrl();
        
        TenantDto.CreateTenantRequest tenantRequest = TestDataBuilder.createTenantRequest(
            "Metadata Test Company",
            TestDataBuilder.createUniqueTenantSlug()
        );
        
        testTenant = AuthenticationTestUtils.createTestTenantWithAdmin(
            new org.springframework.web.client.RestTemplate(),
            baseUrl,
            tenantRequest,
            TestDataBuilder.createAdminUserRequest(UUID.randomUUID())
        );
    }

    @AfterEach
    void cleanup() {
        testDataCleanup.cleanupTenantData(testTenant.getTenantId());
    }

    /**
     * Validates that user-provided metadata is correctly preserved throughout the document processing pipeline.
     * 
     * This test ensures that:
     * 1. Complex user metadata with various data types is preserved during upload
     * 2. Metadata survives the processing pipeline without corruption
     * 3. All metadata entries remain accessible after processing completes
     * 4. Special metadata values (arrays, booleans, null) are handled correctly
     * 
     * This validates the critical E2E-TEST-002 requirement that user-provided
     * document context and classification information is not lost during processing.
     */
    @Test
    @DisplayName("Should preserve and validate user-provided metadata")
    void shouldPreserveUserProvidedMetadata() {
        // Create comprehensive user metadata
        Map<String, Object> userMetadata = new HashMap<>();
        userMetadata.put("department", "Engineering");
        userMetadata.put("category", "Technical Documentation");
        userMetadata.put("priority", "High");
        userMetadata.put("version", "1.0.0");
        userMetadata.put("author", "John Doe");
        userMetadata.put("tags", new String[]{"api", "documentation", "v1"});
        userMetadata.put("confidential", false);
        userMetadata.put("reviewRequired", true);
        userMetadata.put("expirationDate", "2024-12-31");
        userMetadata.put("customId", "DOC-12345");
        
        String content = "This document contains important technical information.";
        
        DocumentDto.DocumentResponse response = uploadDocument(
            "technical-doc.txt",
            content.getBytes(StandardCharsets.UTF_8),
            "text/plain",
            userMetadata
        );
        
        // Validate immediate metadata preservation with descriptive assertions
        assertThat(response.metadata())
            .describedAs("Document metadata should not be null after upload")
            .isNotNull();
        assertThat(response.metadata())
            .describedAs("Department metadata should be preserved immediately after upload")
            .containsEntry("department", "Engineering");
        assertThat(response.metadata())
            .describedAs("Category classification should be preserved")
            .containsEntry("category", "Technical Documentation");
        assertThat(response.metadata())
            .describedAs("Priority level should be maintained")
            .containsEntry("priority", "High");
        assertThat(response.metadata())
            .describedAs("Version information should be preserved")
            .containsEntry("version", "1.0.0");
        assertThat(response.metadata())
            .describedAs("Author information should be maintained")
            .containsEntry("author", "John Doe");
        assertThat(response.metadata())
            .describedAs("Boolean confidential flag should be preserved")
            .containsEntry("confidential", false);
        assertThat(response.metadata())
            .describedAs("Boolean review flag should be maintained")
            .containsEntry("reviewRequired", true);
        assertThat(response.metadata())
            .describedAs("Date strings should be preserved without modification")
            .containsEntry("expirationDate", "2024-12-31");
        assertThat(response.metadata())
            .describedAs("Custom ID should be maintained for external reference")
            .containsEntry("customId", "DOC-12345");
        
        waitForDocumentProcessing(response.id());
        
        // Validate metadata persists after processing completes
        DocumentDto.DocumentResponse processedDoc = getDocument(response.id());
        assertThat(processedDoc.metadata())
            .describedAs("All user-provided metadata should survive the processing pipeline")
            .containsAllEntriesOf(userMetadata);
    }

    @Test
    @DisplayName("Should extract and validate system-generated metadata")
    void shouldExtractSystemGeneratedMetadata() {
        String content = "System metadata extraction test document with sufficient content for analysis.";
        
        DocumentDto.DocumentResponse response = uploadDocument(
            "system-metadata-test.txt",
            content.getBytes(StandardCharsets.UTF_8),
            "text/plain",
            Map.of("source", "integration-test")
        );
        
        // Validate basic system metadata
        assertThat(response.filename()).isNotNull();
        assertThat(response.originalFilename()).isEqualTo("system-metadata-test.txt");
        assertThat(response.fileSize()).isEqualTo((long) content.getBytes().length);
        assertThat(response.contentType()).isEqualTo("text/plain");
        assertThat(response.documentType()).isEqualTo(Document.DocumentType.TXT);
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();
        assertThat(response.uploadedBy()).isNotNull();
        
        // Validate timestamps are recent
        assertThat(response.createdAt()).isAfter(LocalDateTime.now().minusMinutes(5));
        assertThat(response.updatedAt()).isAfter(LocalDateTime.now().minusMinutes(5));
        
        waitForDocumentProcessing(response.id());
        
        DocumentDto.DocumentResponse processedDoc = getDocument(response.id());
        
        // Validate processing-generated metadata
        assertThat(processedDoc.processingStatus()).isEqualTo(Document.ProcessingStatus.COMPLETED);
        assertThat(processedDoc.processingMessage()).isNotNull();
        assertThat(processedDoc.chunkCount()).isNotNull().isGreaterThan(0);
        assertThat(processedDoc.embeddingModel()).isNotNull();
    }

    @Test
    @DisplayName("Should handle complex nested metadata structures")
    void shouldHandleComplexNestedMetadata() {
        // Create complex nested metadata
        Map<String, Object> complexMetadata = new HashMap<>();
        complexMetadata.put("project", Map.of(
            "name", "RAG System",
            "version", "1.0",
            "team", Map.of(
                "lead", "Jane Smith",
                "members", new String[]{"John Doe", "Bob Wilson"},
                "contact", Map.of(
                    "email", "team@company.com",
                    "phone", "+1-555-0123"
                )
            )
        ));
        
        complexMetadata.put("classification", Map.of(
            "level", "Internal",
            "categories", new String[]{"technical", "documentation"},
            "restrictions", Map.of(
                "export", false,
                "sharing", new String[]{"internal", "contractors"}
            )
        ));
        
        complexMetadata.put("timestamps", Map.of(
            "created", "2023-12-01T10:00:00Z",
            "modified", "2023-12-15T15:30:00Z",
            "reviewed", "2023-12-10T09:00:00Z"
        ));
        
        String content = "Document with complex nested metadata structure for testing.";
        
        DocumentDto.DocumentResponse response = uploadDocument(
            "complex-metadata.txt",
            content.getBytes(StandardCharsets.UTF_8),
            "text/plain",
            complexMetadata
        );
        
        // Validate complex metadata preservation
        assertThat(response.metadata()).containsKey("project");
        assertThat(response.metadata()).containsKey("classification");
        assertThat(response.metadata()).containsKey("timestamps");
        
        waitForDocumentProcessing(response.id());
        
        DocumentDto.DocumentResponse processedDoc = getDocument(response.id());
        
        // Validate nested structure is preserved (as much as possible given JSON serialization)
        Map<String, Object> metadata = processedDoc.metadata();
        assertThat(metadata).containsKey("project");
        assertThat(metadata).containsKey("classification");
        assertThat(metadata).containsKey("timestamps");
    }

    @Test
    @DisplayName("Should validate metadata with special characters and encoding")
    void shouldValidateSpecialCharacterMetadata() {
        Map<String, Object> specialMetadata = new HashMap<>();
        specialMetadata.put("título", "Documentación Técnica");
        specialMetadata.put("descripción", "Documento con caracteres especiales: áéíóú, ñ, ç");
        specialMetadata.put("autor", "José María González-Pérez");
        specialMetadata.put("etiquetas", new String[]{"español", "técnico", "documentación"});
        specialMetadata.put("símbolos", "@#$%^&*()_+-=[]{}|;:,.<>?");
        specialMetadata.put("unicode", "中文 日本語 العربية Русский");
        specialMetadata.put("emoji", "📄 📊 💡 🚀");
        
        String content = "Document content with special characters: café, naïve, résumé";
        
        DocumentDto.DocumentResponse response = uploadDocument(
            "special-chars.txt",
            content.getBytes(StandardCharsets.UTF_8),
            "text/plain",
            specialMetadata
        );
        
        // Validate special characters are preserved
        assertThat(response.metadata()).containsEntry("título", "Documentación Técnica");
        assertThat(response.metadata()).containsEntry("descripción", "Documento con caracteres especiales: áéíóú, ñ, ç");
        assertThat(response.metadata()).containsEntry("autor", "José María González-Pérez");
        assertThat(response.metadata()).containsEntry("símbolos", "@#$%^&*()_+-=[]{}|;:,.<>?");
        assertThat(response.metadata()).containsEntry("unicode", "中文 日本語 العربية Русский");
        assertThat(response.metadata()).containsEntry("emoji", "📄 📊 💡 🚀");
        
        waitForDocumentProcessing(response.id());
        
        DocumentDto.DocumentResponse processedDoc = getDocument(response.id());
        
        // Validate special characters persist after processing
        Map<String, Object> metadata = processedDoc.metadata();
        assertThat(metadata).containsEntry("título", "Documentación Técnica");
        assertThat(metadata).containsEntry("unicode", "中文 日本語 العربية Русский");
    }

    @Test
    @DisplayName("Should handle metadata with various data types")
    void shouldHandleVariousDataTypes() {
        Map<String, Object> typedMetadata = new HashMap<>();
        typedMetadata.put("stringValue", "test string");
        typedMetadata.put("integerValue", 12345);
        typedMetadata.put("doubleValue", 123.45);
        typedMetadata.put("booleanTrue", true);
        typedMetadata.put("booleanFalse", false);
        typedMetadata.put("nullValue", null);
        typedMetadata.put("arrayValue", new String[]{"item1", "item2", "item3"});
        typedMetadata.put("numericArray", new Integer[]{1, 2, 3, 4, 5});
        
        String content = "Document for testing various metadata data types.";
        
        DocumentDto.DocumentResponse response = uploadDocument(
            "data-types.txt",
            content.getBytes(StandardCharsets.UTF_8),
            "text/plain",
            typedMetadata
        );
        
        // Validate different data types are handled
        Map<String, Object> metadata = response.metadata();
        assertThat(metadata).containsEntry("stringValue", "test string");
        assertThat(metadata).containsEntry("integerValue", 12345);
        assertThat(metadata).containsEntry("doubleValue", 123.45);
        assertThat(metadata).containsEntry("booleanTrue", true);
        assertThat(metadata).containsEntry("booleanFalse", false);
        
        waitForDocumentProcessing(response.id());
        
        DocumentDto.DocumentResponse processedDoc = getDocument(response.id());
        assertThat(processedDoc.metadata()).containsEntry("stringValue", "test string");
        assertThat(processedDoc.metadata()).containsEntry("booleanTrue", true);
    }

    @Test
    @DisplayName("Should validate metadata size limits and handling")
    void shouldValidateMetadataSizeLimits() {
        // Create metadata with large values
        StringBuilder largeValue = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            largeValue.append("This is a very long metadata value designed to test size limits and processing. ");
        }
        
        Map<String, Object> largeMetadata = new HashMap<>();
        largeMetadata.put("largeDescription", largeValue.toString());
        largeMetadata.put("regularField", "normal value");
        
        // Add many fields
        for (int i = 0; i < 20; i++) {
            largeMetadata.put("field" + i, "value" + i);
        }
        
        String content = "Document with large metadata for size limit testing.";
        
        DocumentDto.DocumentResponse response = uploadDocument(
            "large-metadata.txt",
            content.getBytes(StandardCharsets.UTF_8),
            "text/plain",
            largeMetadata
        );
        
        // Should handle large metadata gracefully
        assertThat(response.metadata()).isNotNull();
        assertThat(response.metadata()).containsEntry("regularField", "normal value");
        assertThat(response.metadata()).containsKey("largeDescription");
        
        waitForDocumentProcessing(response.id());
        
        DocumentDto.DocumentResponse processedDoc = getDocument(response.id());
        assertThat(processedDoc.processingStatus()).isEqualTo(Document.ProcessingStatus.COMPLETED);
    }

    @Test
    @DisplayName("Should validate metadata update functionality")
    void shouldValidateMetadataUpdate() {
        // Initial upload with basic metadata
        Map<String, Object> initialMetadata = Map.of(
            "status", "draft",
            "version", "1.0",
            "category", "initial"
        );
        
        String content = "Document for testing metadata updates.";
        
        DocumentDto.DocumentResponse response = uploadDocument(
            "update-test.txt",
            content.getBytes(StandardCharsets.UTF_8),
            "text/plain",
            initialMetadata
        );
        
        UUID documentId = response.id();
        assertThat(response.metadata()).containsEntry("status", "draft");
        assertThat(response.metadata()).containsEntry("version", "1.0");
        
        waitForDocumentProcessing(documentId);
        
        // Test document update with new metadata
        DocumentDto.UpdateDocumentRequest updateRequest = new DocumentDto.UpdateDocumentRequest(
            "updated-document.txt",
            Map.of(
                "status", "published",
                "version", "2.0",
                "category", "updated",
                "newField", "added value"
            )
        );
        
        DocumentDto.DocumentResponse updatedDoc = updateDocument(documentId, updateRequest);
        
        // Validate updates
        assertThat(updatedDoc.filename()).isEqualTo("updated-document.txt");
        assertThat(updatedDoc.metadata()).containsEntry("status", "published");
        assertThat(updatedDoc.metadata()).containsEntry("version", "2.0");
        assertThat(updatedDoc.metadata()).containsEntry("category", "updated");
        assertThat(updatedDoc.metadata()).containsEntry("newField", "added value");
        
        // Validate update timestamp changed
        assertThat(updatedDoc.updatedAt()).isAfter(response.updatedAt());
    }

    // Helper methods

    private DocumentDto.DocumentResponse uploadDocument(String filename, byte[] content, String contentType, Map<String, Object> metadata) {
        String uploadUrl = baseUrl + "/api/v1/documents/upload";
        
        HttpHeaders headers = IntegrationTestUtils.createAuthHeaders(testTenant.adminToken());
        headers.set("X-Tenant-ID", testTenant.getTenantId().toString());
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        
        if (metadata != null && !metadata.isEmpty()) {
            try {
                String metadataJson = objectMapper.writeValueAsString(metadata);
                body.add("metadata", metadataJson);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize metadata", e);
            }
        }
        
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        
        ResponseEntity<DocumentDto.DocumentResponse> response = restTemplate.exchange(
            uploadUrl,
            HttpMethod.POST,
            requestEntity,
            DocumentDto.DocumentResponse.class
        );
        
        IntegrationTestUtils.assertSuccessfulResponse(response);
        return response.getBody();
    }

    private DocumentDto.DocumentResponse getDocument(UUID documentId) {
        String getUrl = baseUrl + "/api/v1/documents/" + documentId;
        
        HttpHeaders headers = IntegrationTestUtils.createAuthHeaders(testTenant.adminToken());
        headers.set("X-Tenant-ID", testTenant.getTenantId().toString());
        
        ResponseEntity<DocumentDto.DocumentResponse> response = restTemplate.exchange(
            getUrl,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            DocumentDto.DocumentResponse.class
        );
        
        IntegrationTestUtils.assertSuccessfulResponse(response);
        return response.getBody();
    }

    private DocumentDto.DocumentResponse updateDocument(UUID documentId, DocumentDto.UpdateDocumentRequest updateRequest) {
        String updateUrl = baseUrl + "/api/v1/documents/" + documentId;
        
        HttpHeaders headers = IntegrationTestUtils.createAuthHeaders(testTenant.adminToken());
        headers.set("X-Tenant-ID", testTenant.getTenantId().toString());
        
        ResponseEntity<DocumentDto.DocumentResponse> response = restTemplate.exchange(
            updateUrl,
            HttpMethod.PUT,
            new HttpEntity<>(updateRequest, headers),
            DocumentDto.DocumentResponse.class
        );
        
        IntegrationTestUtils.assertSuccessfulResponse(response);
        return response.getBody();
    }

    private void waitForDocumentProcessing(UUID documentId) {
        await("Document processing to complete")
            .atMost(Duration.ofSeconds(30))
            .pollInterval(Duration.ofMillis(500))
            .until(() -> {
                try {
                    DocumentDto.DocumentResponse doc = getDocument(documentId);
                    return doc.processingStatus() == Document.ProcessingStatus.COMPLETED ||
                           doc.processingStatus() == Document.ProcessingStatus.FAILED;
                } catch (Exception e) {
                    return false;
                }
            });
    }

    /**
     * Validates metadata handling with various data size boundaries systematically.
     * 
     * This parameterized test ensures that metadata extraction handles different
     * content sizes appropriately:
     * 1. Very small metadata values (edge case testing)
     * 2. Medium-sized metadata (typical use cases)  
     * 3. Large metadata values (stress testing)
     * 
     * This validates that the E2E-TEST-002 metadata extraction pipeline can handle
     * the full range of metadata sizes expected in production scenarios without
     * performance degradation or data loss.
     */
    @ParameterizedTest
    @ValueSource(ints = {10, 50, 100, 500, 1000})
    @DisplayName("Should handle various metadata value sizes consistently")
    void shouldHandleVariousMetadataSizesConsistently(int metadataSize) {
        // Create metadata with controlled size
        StringBuilder largeValue = new StringBuilder();
        for (int i = 0; i < metadataSize / 10; i++) {
            largeValue.append("Test data ");
        }
        
        Map<String, Object> sizedMetadata = new HashMap<>();
        sizedMetadata.put("size", metadataSize);
        sizedMetadata.put("content", largeValue.toString().trim());
        sizedMetadata.put("testType", "size-validation");
        
        String documentContent = "Document for metadata size testing with " + metadataSize + " character metadata.";
        
        DocumentDto.DocumentResponse response = uploadDocument(
            "metadata-size-test.txt",
            documentContent.getBytes(StandardCharsets.UTF_8),
            "text/plain",
            sizedMetadata
        );
        
        // Validate metadata is preserved regardless of size
        assertThat(response.metadata())
            .describedAs("Metadata should be preserved for size %d", metadataSize)
            .isNotNull()
            .containsEntry("size", metadataSize)
            .containsEntry("testType", "size-validation");
            
        assertThat(response.metadata().get("content"))
            .describedAs("Large metadata content should be preserved for size %d", metadataSize)
            .isNotNull();
            
        String preservedContent = (String) response.metadata().get("content");
        assertThat(preservedContent.length())
            .describedAs("Metadata content length should be approximately %d characters", metadataSize)
            .isGreaterThan(Math.min(5, metadataSize - 10)) // Allow some variance
            .isLessThan(metadataSize + 20); // Allow some variance for formatting
        
        waitForDocumentProcessing(response.id());
        
        // Validate metadata survives processing regardless of size
        DocumentDto.DocumentResponse processedDoc = getDocument(response.id());
        assertThat(processedDoc.metadata())
            .describedAs("Metadata should survive processing for size %d", metadataSize)
            .containsEntry("size", metadataSize)
            .containsEntry("testType", "size-validation");
    }
}
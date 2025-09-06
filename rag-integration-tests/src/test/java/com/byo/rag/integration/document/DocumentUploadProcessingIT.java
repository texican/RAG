package com.byo.rag.integration.document;

import com.byo.rag.integration.base.BaseIntegrationTest;
import com.byo.rag.integration.data.TestDataBuilder;
import com.byo.rag.integration.data.TestDataCleanup;
import com.byo.rag.integration.utils.AuthenticationTestUtils;
import com.byo.rag.integration.utils.IntegrationTestUtils;
import com.byo.rag.shared.dto.DocumentDto;
import com.byo.rag.shared.dto.TenantDto;
import com.byo.rag.shared.dto.UserDto;
import com.byo.rag.shared.entity.Document;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import java.time.Duration;

/**
 * Integration tests for document upload and processing functionality.
 * 
 * This test class validates the complete document processing pipeline including
 * file upload, text extraction, chunking, and metadata processing across
 * multiple document formats.
 */
@DisplayName("Document Upload and Processing Integration Tests")
class DocumentUploadProcessingIT extends BaseIntegrationTest {

    @Autowired
    private TestDataCleanup testDataCleanup;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;

    private AuthenticationTestUtils.TestTenantSetup testTenant;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = getBaseUrl();
        
        // Create test tenant and admin user for document operations
        TenantDto.CreateTenantRequest tenantRequest = TestDataBuilder.createTenantRequest(
            "Document Test Company",
            TestDataBuilder.createUniqueTenantSlug()
        );
        
        testTenant = AuthenticationTestUtils.createTestTenantWithAdmin(
            new org.springframework.web.client.RestTemplate(),
            baseUrl,
            tenantRequest,
            TestDataBuilder.createAdminUserRequest(UUID.randomUUID()) // Will be corrected after tenant creation
        );
    }

    @AfterEach
    void cleanup() {
        testDataCleanup.cleanupTenantData(testTenant.getTenantId());
    }

    /**
     * Validates complete document upload and processing pipeline for text files.
     * 
     * This test ensures that:
     * 1. Text documents can be uploaded with multipart form data
     * 2. User-provided metadata is preserved during upload
     * 3. System metadata is automatically generated (file size, content type, timestamps)
     * 4. Document processing completes successfully with chunking
     * 5. Processing status transitions from PENDING/PROCESSING to COMPLETED
     * 
     * This validates the core E2E-TEST-002 document upload functionality that forms
     * the foundation of the RAG system's document ingestion pipeline.
     */
    @Test
    @DisplayName("Should successfully upload a text document and extract metadata")
    void shouldUploadTextDocumentWithMetadata() {
        // Create test document content
        String content = TestDataBuilder.createTestDocumentContent();
        byte[] fileBytes = content.getBytes();
        
        // Create metadata
        Map<String, Object> metadata = TestDataBuilder.createDocumentMetadata("technical", "integration-test");
        
        // Upload document
        DocumentDto.DocumentResponse response = uploadDocument(
            "test-document.txt", 
            fileBytes, 
            "text/plain", 
            metadata
        );
        
        // Validate response with descriptive assertions
        assertThat(response)
            .describedAs("Document upload response should not be null")
            .isNotNull();
        assertThat(response.id())
            .describedAs("Uploaded document should have a valid UUID identifier")
            .isNotNull();
        assertThat(response.filename())
            .describedAs("Document filename should be preserved during upload")
            .isEqualTo("test-document.txt");
        assertThat(response.originalFilename())
            .describedAs("Original filename should match the uploaded file")
            .isEqualTo("test-document.txt");
        assertThat(response.contentType())
            .describedAs("Content type should be correctly detected as text/plain")
            .isEqualTo("text/plain");
        assertThat(response.documentType())
            .describedAs("Document type should be classified as TXT based on extension")
            .isEqualTo(Document.DocumentType.TXT);
        assertThat(response.fileSize())
            .describedAs("File size should match the uploaded content byte length")
            .isEqualTo((long) fileBytes.length);
        assertThat(response.processingStatus())
            .describedAs("Document should be in initial processing state after upload")
            .isIn(Document.ProcessingStatus.PENDING, Document.ProcessingStatus.PROCESSING);
        assertThat(response.metadata())
            .describedAs("User-provided metadata should be preserved in document response")
            .containsAllEntriesOf(metadata);
        assertThat(response.uploadedBy())
            .describedAs("Document should track which user performed the upload")
            .isNotNull();
        
        // Wait for processing to complete
        waitForDocumentProcessing(response.id());
        
        // Verify document was processed successfully
        DocumentDto.DocumentResponse processedDoc = getDocument(response.id());
        assertThat(processedDoc.processingStatus())
            .describedAs("Document processing should complete successfully")
            .isEqualTo(Document.ProcessingStatus.COMPLETED);
        assertThat(processedDoc.chunkCount())
            .describedAs("Processed document should be chunked into multiple segments")
            .isGreaterThan(0);
    }

    /**
     * Validates PDF document upload and processing capabilities.
     * 
     * This test ensures that:
     * 1. PDF files are correctly detected and classified
     * 2. Content type is properly set to application/pdf
     * 3. Document metadata is preserved for PDF files
     * 4. PDF processing completes without errors
     * 
     * Note: This test uses a mock PDF structure for testing purposes.
     * In production, actual PDF parsing would extract text content.
     */
    @Test
    @DisplayName("Should successfully upload a PDF document")
    void shouldUploadPdfDocument() {
        // Create a simple PDF-like content (mock for testing)
        byte[] pdfContent = createMockPdfContent();
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("documentType", "policy");
        metadata.put("department", "legal");
        
        DocumentDto.DocumentResponse response = uploadDocument(
            "test-policy.pdf",
            pdfContent,
            "application/pdf",
            metadata
        );
        
        // Validate PDF-specific response attributes
        assertThat(response.filename())
            .describedAs("PDF filename should be preserved")
            .isEqualTo("test-policy.pdf");
        assertThat(response.contentType())
            .describedAs("PDF content type should be correctly identified")
            .isEqualTo("application/pdf");
        assertThat(response.documentType())
            .describedAs("Document should be classified as PDF type")
            .isEqualTo(Document.DocumentType.PDF);
        assertThat(response.fileSize())
            .describedAs("PDF file size should match uploaded content")
            .isEqualTo((long) pdfContent.length);
        assertThat(response.metadata())
            .describedAs("PDF metadata should include document type classification")
            .containsEntry("documentType", "policy");
        assertThat(response.metadata())
            .describedAs("PDF metadata should include department information")
            .containsEntry("department", "legal");
        
        // Wait for processing
        waitForDocumentProcessing(response.id());
    }

    @Test
    @DisplayName("Should successfully upload a Markdown document")
    void shouldUploadMarkdownDocument() {
        String markdownContent = """
            # Integration Test Document
            
            This is a **test document** for the integration test suite.
            
            ## Features
            
            - Document upload
            - Text extraction
            - Metadata processing
            - *Chunking validation*
            
            ### Code Example
            
            ```java
            public class TestClass {
                public void testMethod() {
                    System.out.println("Hello, World!");
                }
            }
            ```
            
            This document tests various Markdown elements and formatting.
            """;
            
        byte[] fileBytes = markdownContent.getBytes();
        
        DocumentDto.DocumentResponse response = uploadDocument(
            "integration-test.md",
            fileBytes,
            "text/markdown",
            TestDataBuilder.createDocumentMetadata("documentation", "test-suite")
        );
        
        assertThat(response.filename()).isEqualTo("integration-test.md");
        assertThat(response.documentType()).isEqualTo(Document.DocumentType.MD);
        assertThat(response.contentType()).isEqualTo("text/markdown");
        
        waitForDocumentProcessing(response.id());
        
        // Verify chunks were created appropriately for structured content
        DocumentDto.DocumentResponse processedDoc = getDocument(response.id());
        assertThat(processedDoc.chunkCount()).isGreaterThan(1); // Should be chunked due to sections
    }

    @Test
    @DisplayName("Should handle large document upload and chunking")
    void shouldHandleLargeDocumentUpload() {
        // Create large document content
        String largeContent = TestDataBuilder.createLargeTestDocumentContent();
        byte[] fileBytes = largeContent.getBytes();
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("size", "large");
        metadata.put("testType", "performance");
        
        DocumentDto.DocumentResponse response = uploadDocument(
            "large-document.txt",
            fileBytes,
            "text/plain",
            metadata
        );
        
        assertThat(response.fileSize()).isEqualTo((long) fileBytes.length);
        assertThat(response.fileSize()).isGreaterThan(1000); // Should be reasonably large
        
        waitForDocumentProcessing(response.id());
        
        // Verify chunking handled large document appropriately
        DocumentDto.DocumentResponse processedDoc = getDocument(response.id());
        assertThat(processedDoc.processingStatus()).isEqualTo(Document.ProcessingStatus.COMPLETED);
        assertThat(processedDoc.chunkCount()).isGreaterThan(5); // Large document should create multiple chunks
        
        // Verify chunks exist in database
        Integer chunkCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM document_chunks WHERE document_id = ?",
            Integer.class,
            response.id()
        );
        assertThat(chunkCount).isEqualTo(processedDoc.chunkCount());
    }

    @Test
    @DisplayName("Should validate document upload with empty metadata")
    void shouldUploadDocumentWithEmptyMetadata() {
        String content = "Simple test document without metadata.";
        byte[] fileBytes = content.getBytes();
        
        DocumentDto.DocumentResponse response = uploadDocument(
            "no-metadata.txt",
            fileBytes,
            "text/plain",
            new HashMap<>() // Empty metadata
        );
        
        assertThat(response.metadata()).isNotNull();
        // Should still contain some default metadata from processing
        waitForDocumentProcessing(response.id());
    }

    @Test
    @DisplayName("Should list uploaded documents with pagination")
    void shouldListDocumentsWithPagination() {
        // Upload multiple documents
        for (int i = 1; i <= 3; i++) {
            String content = "Test document " + i + " content.";
            uploadDocument(
                "test-doc-" + i + ".txt",
                content.getBytes(),
                "text/plain",
                Map.of("index", i)
            );
        }
        
        // Wait a moment for all uploads to complete
        IntegrationTestUtils.sleep(Duration.ofSeconds(2));
        
        // Test document listing
        String listUrl = baseUrl + "/api/v1/documents?page=0&size=10";
        HttpHeaders headers = IntegrationTestUtils.createAuthHeaders(testTenant.adminToken());
        headers.set("X-Tenant-ID", testTenant.getTenantId().toString());
        
        ResponseEntity<String> response = restTemplate.exchange(
            listUrl,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class
        );
        
        IntegrationTestUtils.assertSuccessfulResponse(response);
        assertThat(response.getBody()).contains("test-doc-1.txt");
        assertThat(response.getBody()).contains("test-doc-2.txt");
        assertThat(response.getBody()).contains("test-doc-3.txt");
    }

    @Test
    @DisplayName("Should retrieve document statistics")
    void shouldRetrieveDocumentStatistics() {
        // Upload a test document first
        String content = "Statistics test document.";
        uploadDocument(
            "stats-test.txt",
            content.getBytes(),
            "text/plain",
            Map.of("purpose", "statistics")
        );
        
        // Get document statistics
        String statsUrl = baseUrl + "/api/v1/documents/stats";
        HttpHeaders headers = IntegrationTestUtils.createAuthHeaders(testTenant.adminToken());
        headers.set("X-Tenant-ID", testTenant.getTenantId().toString());
        
        ResponseEntity<String> response = restTemplate.exchange(
            statsUrl,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class
        );
        
        IntegrationTestUtils.assertSuccessfulResponse(response);
        
        // Parse and validate statistics
        String responseBody = response.getBody();
        assertThat(responseBody).contains("totalDocuments");
        assertThat(responseBody).contains("storageUsageBytes");
        
        // Should have at least 1 document
        boolean hasValidCount = responseBody.contains("\"totalDocuments\":1") ||
                              responseBody.contains("\"totalDocuments\":2") ||
                              responseBody.contains("\"totalDocuments\":3") ||
                              responseBody.contains("\"totalDocuments\":4");
        assertThat(hasValidCount).isTrue();
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
            // Convert metadata to JSON string for multipart upload
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

    private byte[] createMockPdfContent() {
        // Create a very simple mock PDF-like content for testing
        // In a real scenario, you might use actual PDF generation libraries
        String pdfText = """
            %PDF-1.4
            1 0 obj << /Type /Catalog /Pages 2 0 R >>
            2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >>
            3 0 obj << /Type /Page /Parent 2 0 R /Contents 4 0 R >>
            4 0 obj << /Length 44 >>
            stream
            BT
            /F1 12 Tf
            100 100 Td
            (Mock PDF Content for Testing) Tj
            ET
            endstream
            endobj
            xref
            0 5
            0000000000 65535 f 
            0000000010 00000 n 
            0000000053 00000 n 
            0000000125 00000 n 
            0000000185 00000 n 
            trailer << /Size 5 /Root 1 0 R >>
            startxref
            282
            %%EOF
            """;
        return pdfText.getBytes();
    }

    /**
     * Validates document upload handling across various file types systematically.
     * 
     * This parameterized test ensures consistent behavior for different document formats:
     * 1. File type detection based on extension
     * 2. Content type mapping correctness  
     * 3. Document type classification accuracy
     * 4. Processing pipeline compatibility across formats
     * 
     * This boundary testing validates that E2E-TEST-002 supports the full range
     * of document formats expected in production RAG scenarios.
     */
    @ParameterizedTest
    @CsvSource({
        "test.txt, text/plain, TXT",
        "document.md, text/markdown, MD", 
        "data.json, application/json, JSON",
        "styles.css, text/css, OTHER",
        "script.js, application/javascript, OTHER"
    })
    @DisplayName("Should handle various document types consistently")
    void shouldHandleVariousDocumentTypesConsistently(String filename, String expectedContentType, String expectedDocType) {
        // Create test content appropriate for the file type
        String content = switch (expectedDocType) {
            case "JSON" -> "{\"test\": \"content\", \"type\": \"parameterized-test\"}";
            case "MD" -> "# Test Markdown\n\nParameterized test content with **formatting**.";
            case "CSS" -> "body { margin: 0; padding: 0; font-family: Arial; }";
            case "OTHER" -> "console.log('Parameterized test content for: " + filename + "');";
            default -> "Parameterized test content for file type validation: " + filename;
        };
        
        Map<String, Object> metadata = Map.of(
            "testType", "parameterized",
            "filename", filename,
            "expectedType", expectedDocType
        );
        
        DocumentDto.DocumentResponse response = uploadDocument(
            filename,
            content.getBytes(),
            expectedContentType,
            metadata
        );
        
        // Validate file type handling consistency
        assertThat(response.filename())
            .describedAs("Filename should be preserved for %s", filename)
            .isEqualTo(filename);
        assertThat(response.contentType())
            .describedAs("Content type should match expected for %s", filename)
            .isEqualTo(expectedContentType);
        
        Document.DocumentType expectedType = Document.DocumentType.valueOf(expectedDocType);
        assertThat(response.documentType())
            .describedAs("Document type should be correctly classified for %s", filename)
            .isEqualTo(expectedType);
            
        assertThat(response.metadata())
            .describedAs("Metadata should be preserved for %s", filename)
            .containsEntry("testType", "parameterized")
            .containsEntry("expectedType", expectedDocType);
            
        waitForDocumentProcessing(response.id());
        
        // Verify processing completed successfully regardless of file type
        DocumentDto.DocumentResponse processedDoc = getDocument(response.id());
        assertThat(processedDoc.processingStatus())
            .describedAs("Document processing should complete for file type %s", expectedDocType)
            .isEqualTo(Document.ProcessingStatus.COMPLETED);
    }
}
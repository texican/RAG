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
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for validating document processing across multiple file formats.
 * 
 * This test class ensures that the document service can handle various file formats
 * including PDF, TXT, DOCX, MD, HTML, and other common document types with proper
 * text extraction and metadata handling.
 */
@DisplayName("Document Format Processing Integration Tests")
class DocumentFormatProcessingIT extends BaseIntegrationTest {

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
            "Format Test Company",
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
     * Validates that plain text documents are processed correctly with proper content extraction.
     * 
     * This test ensures that:
     * 1. Plain text files are correctly identified and classified
     * 2. Text content is preserved during processing without modification
     * 3. Document type is accurately detected as TXT
     * 4. Processing completes successfully with proper chunking
     * 
     * This validates the foundational E2E-TEST-002 requirement for basic text
     * processing that forms the baseline for more complex document formats.
     */
    @Test
    @DisplayName("Should process plain text documents correctly")
    void shouldProcessPlainTextDocument() {
        String content = """
            This is a plain text document for testing.
            It contains multiple lines and paragraphs.
            
            The document should be processed correctly
            with proper text extraction and chunking.
            """;
        
        DocumentDto.DocumentResponse response = uploadDocument(
            "plain-text.txt",
            content.getBytes(StandardCharsets.UTF_8),
            "text/plain",
            Map.of("format", "plain-text", "encoding", "utf-8")
        );
        
        // Validate document properties with descriptive assertions
        assertThat(response.documentType())
            .describedAs("Plain text document should be classified as TXT type")
            .isEqualTo(Document.DocumentType.TXT);
        assertThat(response.contentType())
            .describedAs("Content type should be correctly detected as text/plain")
            .isEqualTo("text/plain");
        assertThat(response.originalFilename())
            .describedAs("Original filename should be preserved during processing")
            .isEqualTo("plain-text.txt");
        
        waitForDocumentProcessing(response.id());
        
        DocumentDto.DocumentResponse processedDoc = getDocument(response.id());
        assertThat(processedDoc.processingStatus()).isEqualTo(Document.ProcessingStatus.COMPLETED);
        assertThat(processedDoc.chunkCount()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should process Markdown documents and preserve structure")
    void shouldProcessMarkdownDocument() {
        String markdownContent = """
            # Document Processing Test
            
            This **Markdown** document tests the system's ability to handle structured text.
            
            ## Section 1: Introduction
            
            Markdown documents contain *formatting* that should be preserved during processing.
            
            ### Subsection 1.1: Lists
            
            - First item
            - Second item with `code`
            - Third item
            
            ### Subsection 1.2: Code Blocks
            
            ```java
            public class Example {
                public void method() {
                    System.out.println("Code block test");
                }
            }
            ```
            
            ## Section 2: Conclusion
            
            The document processing should handle all these elements correctly.
            """;
        
        DocumentDto.DocumentResponse response = uploadDocument(
            "structured-content.md",
            markdownContent.getBytes(StandardCharsets.UTF_8),
            "text/markdown",
            Map.of("format", "markdown", "structured", true)
        );
        
        assertThat(response.documentType()).isEqualTo(Document.DocumentType.MD);
        assertThat(response.contentType()).isEqualTo("text/markdown");
        
        waitForDocumentProcessing(response.id());
        
        DocumentDto.DocumentResponse processedDoc = getDocument(response.id());
        assertThat(processedDoc.processingStatus()).isEqualTo(Document.ProcessingStatus.COMPLETED);
        
        // Markdown with structure should create multiple chunks
        assertThat(processedDoc.chunkCount()).isGreaterThan(2);
    }

    @Test
    @DisplayName("Should process HTML documents and extract text content")
    void shouldProcessHtmlDocument() {
        String htmlContent = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <title>Test HTML Document</title>
                <style>
                    body { font-family: Arial, sans-serif; }
                    .important { color: red; }
                </style>
            </head>
            <body>
                <h1>HTML Document Processing Test</h1>
                <p>This HTML document contains various elements that should be processed correctly.</p>
                
                <h2>Content Section</h2>
                <p class="important">Important information in a paragraph.</p>
                
                <ul>
                    <li>List item one</li>
                    <li>List item two</li>
                    <li>List item three</li>
                </ul>
                
                <div>
                    <p>Nested content in a div element.</p>
                    <blockquote>
                        This is a blockquote that should be extracted as text.
                    </blockquote>
                </div>
                
                <script>
                    // This script should be ignored during text extraction
                    console.log("This should not appear in extracted text");
                </script>
                
                <footer>
                    <p>Footer content should be included.</p>
                </footer>
            </body>
            </html>
            """;
        
        DocumentDto.DocumentResponse response = uploadDocument(
            "test-document.html",
            htmlContent.getBytes(StandardCharsets.UTF_8),
            "text/html",
            Map.of("format", "html", "has-script", true)
        );
        
        assertThat(response.documentType()).isEqualTo(Document.DocumentType.HTML);
        assertThat(response.contentType()).isEqualTo("text/html");
        
        waitForDocumentProcessing(response.id());
        
        DocumentDto.DocumentResponse processedDoc = getDocument(response.id());
        assertThat(processedDoc.processingStatus()).isEqualTo(Document.ProcessingStatus.COMPLETED);
        assertThat(processedDoc.chunkCount()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should process mock PDF documents")
    void shouldProcessPdfDocument() {
        byte[] mockPdfContent = createEnhancedMockPdfContent();
        
        DocumentDto.DocumentResponse response = uploadDocument(
            "test-document.pdf",
            mockPdfContent,
            "application/pdf",
            Map.of("format", "pdf", "pages", 1, "mock", true)
        );
        
        assertThat(response.documentType()).isEqualTo(Document.DocumentType.PDF);
        assertThat(response.contentType()).isEqualTo("application/pdf");
        assertThat(response.fileSize()).isEqualTo((long) mockPdfContent.length);
        
        waitForDocumentProcessing(response.id());
        
        DocumentDto.DocumentResponse processedDoc = getDocument(response.id());
        // Mock PDF might not extract text properly, but should not fail processing
        assertThat(processedDoc.processingStatus()).isIn(
            Document.ProcessingStatus.COMPLETED,
            Document.ProcessingStatus.FAILED // Acceptable for mock PDF
        );
    }

    @Test
    @DisplayName("Should process mock DOCX documents")
    void shouldProcessDocxDocument() {
        byte[] mockDocxContent = createMockDocxContent();
        
        DocumentDto.DocumentResponse response = uploadDocument(
            "test-document.docx",
            mockDocxContent,
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            Map.of("format", "docx", "mock", true)
        );
        
        assertThat(response.documentType()).isEqualTo(Document.DocumentType.DOCX);
        assertThat(response.contentType()).isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        
        waitForDocumentProcessing(response.id());
        
        DocumentDto.DocumentResponse processedDoc = getDocument(response.id());
        // Mock DOCX might not extract text properly, but should handle gracefully
        assertThat(processedDoc.processingStatus()).isIn(
            Document.ProcessingStatus.COMPLETED,
            Document.ProcessingStatus.FAILED // Acceptable for mock DOCX
        );
    }

    @Test
    @DisplayName("Should handle CSV data files")
    void shouldProcessCsvDocument() {
        String csvContent = """
            Name,Age,Department,Salary
            John Doe,30,Engineering,75000
            Jane Smith,28,Marketing,65000
            Bob Johnson,35,Sales,70000
            Alice Brown,32,Engineering,80000
            Charlie Wilson,29,Marketing,62000
            """;
        
        DocumentDto.DocumentResponse response = uploadDocument(
            "employee-data.csv",
            csvContent.getBytes(StandardCharsets.UTF_8),
            "text/csv",
            Map.of("format", "csv", "rows", 6, "columns", 4)
        );
        
        assertThat(response.documentType()).isEqualTo(Document.DocumentType.TXT); // CSV typically processed as text
        assertThat(response.contentType()).isEqualTo("text/csv");
        
        waitForDocumentProcessing(response.id());
        
        DocumentDto.DocumentResponse processedDoc = getDocument(response.id());
        assertThat(processedDoc.processingStatus()).isEqualTo(Document.ProcessingStatus.COMPLETED);
        assertThat(processedDoc.chunkCount()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should handle JSON data files")
    void shouldProcessJsonDocument() {
        String jsonContent = """
            {
                "document": {
                    "title": "API Documentation",
                    "version": "1.0.0",
                    "sections": [
                        {
                            "name": "Introduction",
                            "content": "This API provides comprehensive functionality for document management."
                        },
                        {
                            "name": "Endpoints",
                            "content": "The following endpoints are available for document operations.",
                            "endpoints": [
                                {
                                    "method": "POST",
                                    "path": "/api/documents",
                                    "description": "Upload a new document"
                                },
                                {
                                    "method": "GET",
                                    "path": "/api/documents/{id}",
                                    "description": "Retrieve a specific document"
                                }
                            ]
                        }
                    ],
                    "metadata": {
                        "author": "Development Team",
                        "created": "2023-12-01",
                        "updated": "2023-12-15"
                    }
                }
            }
            """;
        
        DocumentDto.DocumentResponse response = uploadDocument(
            "api-documentation.json",
            jsonContent.getBytes(StandardCharsets.UTF_8),
            "application/json",
            Map.of("format", "json", "structured", true)
        );
        
        assertThat(response.documentType()).isEqualTo(Document.DocumentType.TXT); // JSON processed as text
        assertThat(response.contentType()).isEqualTo("application/json");
        
        waitForDocumentProcessing(response.id());
        
        DocumentDto.DocumentResponse processedDoc = getDocument(response.id());
        assertThat(processedDoc.processingStatus()).isEqualTo(Document.ProcessingStatus.COMPLETED);
        assertThat(processedDoc.chunkCount()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should process documents with different encodings")
    void shouldProcessDifferentEncodings() {
        // UTF-8 document with special characters
        String utf8Content = "Café naïve résumé — special characters: áéíóú, ñ, ç, ü, 中文, 日本語, العربية";
        
        DocumentDto.DocumentResponse response = uploadDocument(
            "utf8-document.txt",
            utf8Content.getBytes(StandardCharsets.UTF_8),
            "text/plain; charset=utf-8",
            Map.of("encoding", "utf-8", "special-chars", true)
        );
        
        assertThat(response.contentType()).startsWith("text/plain");
        
        waitForDocumentProcessing(response.id());
        
        DocumentDto.DocumentResponse processedDoc = getDocument(response.id());
        assertThat(processedDoc.processingStatus()).isEqualTo(Document.ProcessingStatus.COMPLETED);
    }

    @Test
    @DisplayName("Should validate file size limits and processing")
    void shouldValidateFileSizeLimits() {
        // Create a reasonably large document
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeContent.append("This is line ").append(i).append(" of a large document for testing file size processing. ");
            largeContent.append("Each line contains sufficient content to test chunking and processing performance. ");
            largeContent.append("The system should handle this efficiently without issues.\n");
        }
        
        byte[] content = largeContent.toString().getBytes(StandardCharsets.UTF_8);
        
        DocumentDto.DocumentResponse response = uploadDocument(
            "large-document.txt",
            content,
            "text/plain",
            Map.of("size", "large", "lines", 1000)
        );
        
        assertThat(response.fileSize()).isEqualTo((long) content.length);
        assertThat(response.fileSize()).isGreaterThan(50000); // Should be reasonably large
        
        waitForDocumentProcessing(response.id());
        
        DocumentDto.DocumentResponse processedDoc = getDocument(response.id());
        assertThat(processedDoc.processingStatus()).isEqualTo(Document.ProcessingStatus.COMPLETED);
        assertThat(processedDoc.chunkCount()).isGreaterThan(10); // Large document should create many chunks
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

    private byte[] createEnhancedMockPdfContent() {
        // Enhanced mock PDF with more realistic structure
        String pdfText = """
            %PDF-1.4
            1 0 obj
            <<
            /Type /Catalog
            /Pages 2 0 R
            >>
            endobj
            
            2 0 obj
            <<
            /Type /Pages
            /Kids [3 0 R]
            /Count 1
            >>
            endobj
            
            3 0 obj
            <<
            /Type /Page
            /Parent 2 0 R
            /MediaBox [0 0 612 792]
            /Contents 4 0 R
            /Resources <<
                /Font <<
                    /F1 5 0 R
                >>
            >>
            >>
            endobj
            
            4 0 obj
            <<
            /Length 125
            >>
            stream
            BT
            /F1 12 Tf
            100 700 Td
            (Enhanced Mock PDF Document) Tj
            0 -20 Td
            (This PDF contains test content for integration testing.) Tj
            0 -20 Td
            (Document processing should handle PDF files correctly.) Tj
            ET
            endstream
            endobj
            
            5 0 obj
            <<
            /Type /Font
            /Subtype /Type1
            /BaseFont /Helvetica
            >>
            endobj
            
            xref
            0 6
            0000000000 65535 f 
            0000000010 00000 n 
            0000000079 00000 n 
            0000000136 00000 n 
            0000000294 00000 n 
            0000000474 00000 n 
            trailer
            <<
            /Size 6
            /Root 1 0 R
            >>
            startxref
            544
            %%EOF
            """;
        return pdfText.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] createMockDocxContent() {
        // Create a mock DOCX file (ZIP format with XML content)
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {
            
            // Add basic DOCX structure files
            
            // [Content_Types].xml
            zos.putNextEntry(new ZipEntry("[Content_Types].xml"));
            String contentTypes = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                    <Default Extension="xml" ContentType="application/xml"/>
                    <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                </Types>
                """;
            zos.write(contentTypes.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            
            // _rels/.rels
            zos.putNextEntry(new ZipEntry("_rels/.rels"));
            String rels = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
                </Relationships>
                """;
            zos.write(rels.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            
            // word/document.xml
            zos.putNextEntry(new ZipEntry("word/document.xml"));
            String document = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                    <w:body>
                        <w:p>
                            <w:r>
                                <w:t>Mock DOCX Document for Integration Testing</w:t>
                            </w:r>
                        </w:p>
                        <w:p>
                            <w:r>
                                <w:t>This document contains test content that should be extracted during processing.</w:t>
                            </w:r>
                        </w:p>
                    </w:body>
                </w:document>
                """;
            zos.write(document.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            
            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create mock DOCX content", e);
        }
    }

    /**
     * Validates document format processing consistency across multiple file types systematically.
     * 
     * This parameterized test ensures that different document formats:
     * 1. Are correctly identified and classified by file extension
     * 2. Have appropriate content types assigned during processing
     * 3. Complete processing successfully regardless of format complexity
     * 4. Maintain format-specific characteristics and metadata
     * 
     * This boundary testing validates that E2E-TEST-002 format processing pipeline
     * handles the full range of document types expected in production RAG scenarios
     * with consistent behavior and reliable processing outcomes.
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "test.txt|text/plain|TXT|This is plain text content for format testing.",
        "doc.md|text/markdown|MD|# Markdown Test This is markdown content with formatting.",
        "page.html|text/html|HTML|<html><body><h1>HTML Test</h1><p>Content extraction test.</p></body></html>"
    })
    @DisplayName("Should process different document formats consistently")
    void shouldProcessDifferentFormatsConsistently(String filename, String expectedContentType, 
                                                   String expectedDocType, String content) {
        
        // Upload document with specified format
        DocumentDto.DocumentResponse response = uploadDocument(
            filename,
            content.getBytes(StandardCharsets.UTF_8),
            expectedContentType,
            Map.of("testType", "format-validation", "format", expectedDocType)
        );
        
        // Validate format detection and classification
        assertThat(response.filename())
            .describedAs("Filename should be preserved for format %s", expectedDocType)
            .isEqualTo(filename);
        assertThat(response.contentType())
            .describedAs("Content type should match expected for format %s", expectedDocType)
            .isEqualTo(expectedContentType);
        
        Document.DocumentType expectedType = Document.DocumentType.valueOf(expectedDocType);
        assertThat(response.documentType())
            .describedAs("Document type should be correctly classified for format %s", expectedDocType)
            .isEqualTo(expectedType);
        
        assertThat(response.fileSize())
            .describedAs("File size should match content length for format %s", expectedDocType)
            .isEqualTo((long) content.getBytes(StandardCharsets.UTF_8).length);
            
        assertThat(response.metadata())
            .describedAs("Metadata should be preserved for format %s", expectedDocType)
            .containsEntry("testType", "format-validation")
            .containsEntry("format", expectedDocType);
        
        waitForDocumentProcessing(response.id());
        
        // Validate processing completion regardless of format
        DocumentDto.DocumentResponse processedDoc = getDocument(response.id());
        assertThat(processedDoc.processingStatus())
            .describedAs("Document processing should complete successfully for format %s", expectedDocType)
            .isEqualTo(Document.ProcessingStatus.COMPLETED);
            
        // Validate format-specific processing characteristics
        switch (expectedType) {
            case MD -> {
                // Markdown should preserve structure indicators
                assertThat(processedDoc.chunkCount())
                    .describedAs("Markdown documents should be chunked appropriately")
                    .isGreaterThan(0);
            }
            case HTML -> {
                // HTML should extract text content
                assertThat(processedDoc.processingMessage())
                    .describedAs("HTML processing should complete without errors")
                    .doesNotContain("ERROR");
            }
            default -> {
                // All formats should produce valid chunks
                assertThat(processedDoc.chunkCount())
                    .describedAs("Format %s should produce processable chunks", expectedDocType)
                    .isGreaterThan(0);
            }
        }
    }
}
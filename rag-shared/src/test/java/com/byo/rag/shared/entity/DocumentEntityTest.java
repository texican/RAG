package com.byo.rag.shared.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for Document entity functionality including validation,
 * relationships, processing status, and business logic.
 */
class DocumentEntityTest {

    private Validator validator;
    private Tenant testTenant;
    private User testUser;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        // Create test tenant and user
        testTenant = new Tenant();
        testTenant.setId(UUID.randomUUID());
        testTenant.setName("Test Tenant");
        
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@example.com");
    }

    @Test
    @DisplayName("Should create valid document with required fields")
    void shouldCreateValidDocumentWithRequiredFields() {
        Document document = new Document(
            "test-file.pdf",
            "Original Test File.pdf",
            Document.DocumentType.PDF,
            testTenant,
            testUser
        );

        Set<ConstraintViolation<Document>> violations = validator.validate(document);
        assertTrue(violations.isEmpty(), "Valid document should have no violations");
        
        assertEquals("test-file.pdf", document.getFilename());
        assertEquals("Original Test File.pdf", document.getOriginalFilename());
        assertEquals(Document.DocumentType.PDF, document.getDocumentType());
        assertEquals(testTenant, document.getTenant());
        assertEquals(testUser, document.getUploadedBy());
        assertEquals(Document.ProcessingStatus.PENDING, document.getProcessingStatus());
        assertEquals(0, document.getChunkCount());
    }

    @Test
    @DisplayName("Should validate required fields")
    void shouldValidateRequiredFields() {
        Document document = new Document();
        
        Set<ConstraintViolation<Document>> violations = validator.validate(document);
        assertFalse(violations.isEmpty(), "Document without required fields should have violations");
        
        // Check for specific violations
        boolean hasFilenameViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("filename"));
        boolean hasOriginalFilenameViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("originalFilename"));
            
        assertTrue(hasFilenameViolation, "Should have filename violation");
        assertTrue(hasOriginalFilenameViolation, "Should have originalFilename violation");
    }

    @Test
    @DisplayName("Should handle document processing status correctly")
    void shouldHandleDocumentProcessingStatusCorrectly() {
        Document document = new Document(
            "test.pdf", "test.pdf", Document.DocumentType.PDF, testTenant, testUser
        );
        
        // Initial status should be PENDING
        assertEquals(Document.ProcessingStatus.PENDING, document.getProcessingStatus());
        assertFalse(document.isProcessed());
        assertFalse(document.isProcessing());
        assertFalse(document.hasFailed());
        
        // Set to PROCESSING
        document.setProcessingStatus(Document.ProcessingStatus.PROCESSING);
        assertFalse(document.isProcessed());
        assertTrue(document.isProcessing());
        assertFalse(document.hasFailed());
        
        // Set to COMPLETED
        document.setProcessingStatus(Document.ProcessingStatus.COMPLETED);
        assertTrue(document.isProcessed());
        assertFalse(document.isProcessing());
        assertFalse(document.hasFailed());
        
        // Set to FAILED
        document.setProcessingStatus(Document.ProcessingStatus.FAILED);
        assertFalse(document.isProcessed());
        assertFalse(document.isProcessing());
        assertTrue(document.hasFailed());
    }

    @Test
    @DisplayName("Should handle document chunks relationship")
    void shouldHandleDocumentChunksRelationship() {
        Document document = new Document(
            "test.pdf", "test.pdf", Document.DocumentType.PDF, testTenant, testUser
        );
        
        // Initially should have empty chunks list
        assertNotNull(document.getChunks());
        assertTrue(document.getChunks().isEmpty());
        assertEquals(0, document.getChunkCount());
        
        // Add chunks
        DocumentChunk chunk1 = new DocumentChunk("Content 1", 1, document, testTenant);
        DocumentChunk chunk2 = new DocumentChunk("Content 2", 2, document, testTenant);
        
        document.getChunks().add(chunk1);
        document.getChunks().add(chunk2);
        document.setChunkCount(2);
        
        assertEquals(2, document.getChunks().size());
        assertEquals(2, document.getChunkCount());
        assertEquals(document, chunk1.getDocument());
        assertEquals(document, chunk2.getDocument());
    }

    @Test
    @DisplayName("Should handle document metadata and file information")
    void shouldHandleDocumentMetadataAndFileInformation() {
        Document document = new Document(
            "test.pdf", "test.pdf", Document.DocumentType.PDF, testTenant, testUser
        );
        
        // Set file information
        document.setFilePath("/documents/test.pdf");
        document.setFileSize(1024L);
        document.setContentType("application/pdf");
        document.setExtractedText("This is extracted text content");
        document.setEmbeddingModel("sentence-transformers/all-MiniLM-L6-v2");
        document.setMetadata("{\"pages\": 5, \"author\": \"Test Author\"}");
        
        assertEquals("/documents/test.pdf", document.getFilePath());
        assertEquals(1024L, document.getFileSize());
        assertEquals("application/pdf", document.getContentType());
        assertEquals("This is extracted text content", document.getExtractedText());
        assertEquals("sentence-transformers/all-MiniLM-L6-v2", document.getEmbeddingModel());
        assertEquals("{\"pages\": 5, \"author\": \"Test Author\"}", document.getMetadata());
    }

    @Test
    @DisplayName("Should support all document types")
    void shouldSupportAllDocumentTypes() {
        Document.DocumentType[] types = Document.DocumentType.values();
        
        assertTrue(types.length > 0, "Should have document types defined");
        
        for (Document.DocumentType type : types) {
            Document document = new Document(
                "test." + type.name().toLowerCase(),
                "test." + type.name().toLowerCase(),
                type,
                testTenant,
                testUser
            );
            
            assertEquals(type, document.getDocumentType());
            assertDoesNotThrow(() -> validator.validate(document), 
                "Document type " + type + " should be valid");
        }
    }

    @Test
    @DisplayName("Should handle processing messages")
    void shouldHandleProcessingMessages() {
        Document document = new Document(
            "test.pdf", "test.pdf", Document.DocumentType.PDF, testTenant, testUser
        );
        
        // Set processing message for failure
        document.setProcessingStatus(Document.ProcessingStatus.FAILED);
        document.setProcessingMessage("Failed to extract text: corrupted PDF file");
        
        assertEquals("Failed to extract text: corrupted PDF file", document.getProcessingMessage());
        assertTrue(document.hasFailed());
    }

    @Test
    @DisplayName("Should handle filename size limits")
    void shouldHandleFilenameSizeLimits() {
        String longFilename = "a".repeat(256); // Exceeds 255 char limit
        
        Document document = new Document();
        document.setFilename(longFilename);
        document.setOriginalFilename("valid.pdf");
        document.setDocumentType(Document.DocumentType.PDF);
        document.setTenant(testTenant);
        document.setUploadedBy(testUser);
        
        Set<ConstraintViolation<Document>> violations = validator.validate(document);
        assertFalse(violations.isEmpty(), "Long filename should cause validation violation");
        
        boolean hasFilenameViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("filename"));
        assertTrue(hasFilenameViolation, "Should have filename size violation");
    }

    @Test
    @DisplayName("Should inherit BaseEntity functionality")
    void shouldInheritBaseEntityFunctionality() {
        Document document = new Document(
            "test.pdf", "test.pdf", Document.DocumentType.PDF, testTenant, testUser
        );
        
        // Should inherit ID generation
        UUID id = UUID.randomUUID();
        document.setId(id);
        assertEquals(id, document.getId());
        
        // Should inherit audit fields
        assertDoesNotThrow(() -> document.getCreatedAt());
        assertDoesNotThrow(() -> document.getUpdatedAt());
        assertDoesNotThrow(() -> document.getVersion());
        
        // Should inherit equals/hashCode behavior
        Document otherDocument = new Document(
            "other.pdf", "other.pdf", Document.DocumentType.PDF, testTenant, testUser
        );
        otherDocument.setId(id);
        assertEquals(document, otherDocument, "Documents with same ID should be equal");
    }

    @Test
    @DisplayName("Should handle tenant isolation")
    void shouldHandleTenantIsolation() {
        Tenant tenant1 = new Tenant();
        tenant1.setId(UUID.randomUUID());
        tenant1.setName("Tenant 1");
        
        Tenant tenant2 = new Tenant();
        tenant2.setId(UUID.randomUUID());
        tenant2.setName("Tenant 2");
        
        Document doc1 = new Document("test1.pdf", "test1.pdf", Document.DocumentType.PDF, tenant1, testUser);
        Document doc2 = new Document("test2.pdf", "test2.pdf", Document.DocumentType.PDF, tenant2, testUser);
        
        assertNotEquals(doc1.getTenant(), doc2.getTenant(), "Documents should have different tenants");
        assertNotEquals(doc1.getTenant().getId(), doc2.getTenant().getId(), "Tenant IDs should be different");
    }
}
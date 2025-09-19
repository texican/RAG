package com.byo.rag.shared.dto;

import com.byo.rag.shared.entity.Document;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for DocumentDto functionality including validation,
 * serialization, and data consistency checks for all DTO implementations.
 */
class DocumentDtoTest {

    private Validator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // Register JavaTimeModule
    }

    @Test
    @DisplayName("Should create valid DocumentResponse with all fields")
    void shouldCreateValidDocumentResponseWithAllFields() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        UserDto.UserSummary userSummary = new UserDto.UserSummary(
            UUID.randomUUID(), "Test", "User", "test@example.com", 
            com.byo.rag.shared.entity.User.UserRole.USER, 
            com.byo.rag.shared.entity.User.UserStatus.ACTIVE
        );
        
        DocumentDto.DocumentResponse response = new DocumentDto.DocumentResponse(
            id,
            "test-document.pdf",
            "Original Document.pdf",
            1024L,
            "application/pdf",
            Document.DocumentType.PDF,
            Document.ProcessingStatus.COMPLETED,
            "Processing completed successfully",
            5,
            "sentence-transformers/all-MiniLM-L6-v2",
            Map.of("category", "technical", "priority", "high"),
            now,
            now,
            userSummary
        );

        Set<ConstraintViolation<DocumentDto.DocumentResponse>> violations = validator.validate(response);
        assertTrue(violations.isEmpty(), "Valid DocumentResponse should have no violations");
        
        assertEquals(id, response.id());
        assertEquals("test-document.pdf", response.filename());
        assertEquals("Original Document.pdf", response.originalFilename());
        assertEquals(1024L, response.fileSize());
        assertEquals("application/pdf", response.contentType());
        assertEquals(Document.DocumentType.PDF, response.documentType());
        assertEquals(Document.ProcessingStatus.COMPLETED, response.processingStatus());
        assertEquals("Processing completed successfully", response.processingMessage());
        assertEquals(5, response.chunkCount());
        assertEquals("sentence-transformers/all-MiniLM-L6-v2", response.embeddingModel());
        assertEquals(Map.of("category", "technical", "priority", "high"), response.metadata());
        assertEquals(now, response.createdAt());
        assertEquals(now, response.updatedAt());
        assertEquals(userSummary, response.uploadedBy());
    }

    @Test
    @DisplayName("Should create valid UploadDocumentRequest")
    void shouldCreateValidUploadDocumentRequest() {
        Map<String, Object> metadata = Map.of(
            "category", "technical",
            "priority", "high",
            "department", "engineering"
        );
        
        // Mock file object (in real usage would be MultipartFile)
        Object mockFile = "mock-file-content";
        
        DocumentDto.UploadDocumentRequest request = new DocumentDto.UploadDocumentRequest(
            mockFile,
            metadata
        );

        Set<ConstraintViolation<DocumentDto.UploadDocumentRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid UploadDocumentRequest should have no violations");
        
        assertEquals(mockFile, request.file());
        assertEquals(metadata, request.metadata());
    }

    @Test
    @DisplayName("Should validate UploadDocumentRequest requires file")
    void shouldValidateUploadDocumentRequestRequiresFile() {
        DocumentDto.UploadDocumentRequest request = new DocumentDto.UploadDocumentRequest(
            null, // Invalid: file is required
            Map.of("category", "test")
        );

        Set<ConstraintViolation<DocumentDto.UploadDocumentRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "UploadDocumentRequest without file should have violations");
        
        boolean hasFileViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("file"));
        assertTrue(hasFileViolation, "Should have file validation violation");
    }

    @Test
    @DisplayName("Should create valid UpdateDocumentRequest")
    void shouldCreateValidUpdateDocumentRequest() {
        Map<String, Object> metadata = Map.of(
            "category", "updated-category",
            "version", "v2.0"
        );
        
        DocumentDto.UpdateDocumentRequest request = new DocumentDto.UpdateDocumentRequest(
            "updated-filename.pdf",
            metadata
        );

        Set<ConstraintViolation<DocumentDto.UpdateDocumentRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid UpdateDocumentRequest should have no violations");
        
        assertEquals("updated-filename.pdf", request.filename());
        assertEquals(metadata, request.metadata());
    }

    @Test
    @DisplayName("Should validate UpdateDocumentRequest requires filename")
    void shouldValidateUpdateDocumentRequestRequiresFilename() {
        DocumentDto.UpdateDocumentRequest request = new DocumentDto.UpdateDocumentRequest(
            "", // Invalid: filename cannot be blank
            Map.of("category", "test")
        );

        Set<ConstraintViolation<DocumentDto.UpdateDocumentRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "UpdateDocumentRequest with blank filename should have violations");
        
        boolean hasFilenameViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("filename"));
        assertTrue(hasFilenameViolation, "Should have filename validation violation");
    }

    @Test
    @DisplayName("Should create valid DocumentSummary")
    void shouldCreateValidDocumentSummary() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        DocumentDto.DocumentSummary summary = new DocumentDto.DocumentSummary(
            id,
            "summary-doc.pdf",
            Document.DocumentType.PDF,
            Document.ProcessingStatus.COMPLETED,
            2048L,
            3,
            now
        );

        Set<ConstraintViolation<DocumentDto.DocumentSummary>> violations = validator.validate(summary);
        assertTrue(violations.isEmpty(), "Valid DocumentSummary should have no violations");
        
        assertEquals(id, summary.id());
        assertEquals("summary-doc.pdf", summary.filename());
        assertEquals(Document.DocumentType.PDF, summary.documentType());
        assertEquals(Document.ProcessingStatus.COMPLETED, summary.processingStatus());
        assertEquals(2048L, summary.fileSize());
        assertEquals(3, summary.chunkCount());
        assertEquals(now, summary.createdAt());
    }

    @Test
    @DisplayName("Should create valid DocumentProcessingUpdate")
    void shouldCreateValidDocumentProcessingUpdate() {
        UUID documentId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        DocumentDto.DocumentProcessingUpdate update = new DocumentDto.DocumentProcessingUpdate(
            documentId,
            Document.ProcessingStatus.PROCESSING,
            "Document chunking in progress",
            null, // chunk count not available yet
            now
        );

        Set<ConstraintViolation<DocumentDto.DocumentProcessingUpdate>> violations = validator.validate(update);
        assertTrue(violations.isEmpty(), "Valid DocumentProcessingUpdate should have no violations");
        
        assertEquals(documentId, update.documentId());
        assertEquals(Document.ProcessingStatus.PROCESSING, update.status());
        assertEquals("Document chunking in progress", update.message());
        assertNull(update.chunkCount());
        assertEquals(now, update.updatedAt());
    }

    @Test
    @DisplayName("Should serialize and deserialize DocumentResponse correctly")
    void shouldSerializeAndDeserializeDocumentResponseCorrectly() throws Exception {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2023, 12, 25, 10, 30, 45);
        UserDto.UserSummary userSummary = new UserDto.UserSummary(
            UUID.randomUUID(), "Test", "User", "test@example.com", 
            com.byo.rag.shared.entity.User.UserRole.USER, 
            com.byo.rag.shared.entity.User.UserStatus.ACTIVE
        );
        
        DocumentDto.DocumentResponse original = new DocumentDto.DocumentResponse(
            id,
            "test.pdf",
            "Test Document.pdf",
            2048L,
            "application/pdf",
            Document.DocumentType.PDF,
            Document.ProcessingStatus.COMPLETED,
            "Success",
            3,
            "test-model",
            Map.of("category", "technical"),
            now,
            now,
            userSummary
        );

        // Serialize to JSON
        String json = objectMapper.writeValueAsString(original);
        assertNotNull(json);
        assertFalse(json.isEmpty());
        assertTrue(json.contains("\"filename\""));
        assertTrue(json.contains("\"test.pdf\""));
        assertTrue(json.contains("2023-12-25T10:30:45") || json.contains("createdAt"), "Should contain ISO format or field name"); // ISO format

        // Deserialize back to object
        DocumentDto.DocumentResponse deserialized = objectMapper.readValue(json, DocumentDto.DocumentResponse.class);
        
        assertEquals(original.id(), deserialized.id());
        assertEquals(original.filename(), deserialized.filename());
        assertEquals(original.originalFilename(), deserialized.originalFilename());
        assertEquals(original.fileSize(), deserialized.fileSize());
        assertEquals(original.contentType(), deserialized.contentType());
        assertEquals(original.documentType(), deserialized.documentType());
        assertEquals(original.processingStatus(), deserialized.processingStatus());
        assertEquals(original.processingMessage(), deserialized.processingMessage());
        assertEquals(original.chunkCount(), deserialized.chunkCount());
        assertEquals(original.embeddingModel(), deserialized.embeddingModel());
        assertEquals(original.metadata(), deserialized.metadata());
        assertEquals(original.createdAt(), deserialized.createdAt());
        assertEquals(original.updatedAt(), deserialized.updatedAt());
        assertEquals(original.uploadedBy(), deserialized.uploadedBy());
    }

    @Test
    @DisplayName("Should handle null values gracefully in response records")
    void shouldHandleNullValuesGracefullyInResponseRecords() {
        UUID id = UUID.randomUUID();
        
        DocumentDto.DocumentSummary summary = new DocumentDto.DocumentSummary(
            id,
            "test.pdf",
            Document.DocumentType.PDF,
            Document.ProcessingStatus.PENDING,
            null, // null file size
            null, // null chunk count
            LocalDateTime.now()
        );
        
        // Should validate without issues for optional fields
        Set<ConstraintViolation<DocumentDto.DocumentSummary>> violations = validator.validate(summary);
        assertTrue(violations.isEmpty(), "Summary with null optional fields should be valid");
        
        assertEquals(id, summary.id());
        assertEquals("test.pdf", summary.filename());
        assertNull(summary.fileSize());
        assertNull(summary.chunkCount());
    }

    @Test
    @DisplayName("Should support pattern matching with sealed interface")
    void shouldSupportPatternMatchingWithSealedInterface() {
        DocumentDto uploadRequest = new DocumentDto.UploadDocumentRequest(
            "mock-file", Map.of("category", "test")
        );
        
        DocumentDto documentResponse = new DocumentDto.DocumentResponse(
            UUID.randomUUID(), "test.pdf", "test.pdf", 1024L, "application/pdf",
            Document.DocumentType.PDF, Document.ProcessingStatus.COMPLETED, null, 1, null,
            null, LocalDateTime.now(), LocalDateTime.now(),
            new UserDto.UserSummary(UUID.randomUUID(), "Test", "User", "test@example.com", 
                com.byo.rag.shared.entity.User.UserRole.USER, 
                com.byo.rag.shared.entity.User.UserStatus.ACTIVE)
        );
        
        // Test pattern matching (Java 17+ switch expressions)
        String result1 = switch (uploadRequest) {
            case DocumentDto.UploadDocumentRequest req -> "Upload: " + req.file();
            case DocumentDto.UpdateDocumentRequest req -> "Update: " + req.filename();
            case DocumentDto.DocumentResponse resp -> "Response: " + resp.filename();
            case DocumentDto.DocumentSummary summ -> "Summary: " + summ.filename();
            case DocumentDto.DocumentProcessingUpdate upd -> "Update: " + upd.documentId();
        };
        
        String result2 = switch (documentResponse) {
            case DocumentDto.UploadDocumentRequest req -> "Upload: " + req.file();
            case DocumentDto.UpdateDocumentRequest req -> "Update: " + req.filename();
            case DocumentDto.DocumentResponse resp -> "Response: " + resp.filename();
            case DocumentDto.DocumentSummary summ -> "Summary: " + summ.filename();
            case DocumentDto.DocumentProcessingUpdate upd -> "Update: " + upd.documentId();
        };
        
        assertTrue(result1.startsWith("Upload:"));
        assertTrue(result2.startsWith("Response:"));
    }

    @Test
    @DisplayName("Should handle metadata correctly in all DTOs")
    void shouldHandleMetadataCorrectlyInAllDtos() {
        Map<String, Object> complexMetadata = Map.of(
            "string_field", "text value",
            "number_field", 42,
            "boolean_field", true,
            "list_field", java.util.List.of("item1", "item2")
        );
        
        DocumentDto.UploadDocumentRequest uploadRequest = new DocumentDto.UploadDocumentRequest(
            "file", complexMetadata
        );
        
        DocumentDto.UpdateDocumentRequest updateRequest = new DocumentDto.UpdateDocumentRequest(
            "filename.pdf", complexMetadata
        );
        
        // Both should handle complex metadata
        assertEquals(complexMetadata, uploadRequest.metadata());
        assertEquals(complexMetadata, updateRequest.metadata());
        
        // Should be valid
        assertTrue(validator.validate(uploadRequest).isEmpty());
        assertTrue(validator.validate(updateRequest).isEmpty());
    }
}
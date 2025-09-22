package com.byo.rag.document.service;

import com.byo.rag.document.repository.DocumentRepository;
import com.byo.rag.shared.dto.DocumentDto;
import com.byo.rag.shared.dto.TenantDto;
import com.byo.rag.shared.entity.Document;
import com.byo.rag.shared.entity.DocumentChunk;
import com.byo.rag.shared.entity.Tenant;
import com.byo.rag.shared.entity.User;
import com.byo.rag.shared.exception.DocumentNotFoundException;
import com.byo.rag.shared.exception.DocumentProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for DocumentService covering all core functionality.
 * 
 * Test Categories:
 * - Document Upload Processing and Validation
 * - Document Processing Pipeline (Text Extraction, Chunking, Kafka Events)
 * - Document CRUD Operations (Create, Read, Update, Delete)
 * - Multi-Tenant Access Control and Validation
 * - File Validation and Tenant Limits
 * - Error Handling and Exception Scenarios
 * - Async Processing and Status Management
 * - Document Statistics and Analytics
 */
@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;
    
    @Mock
    private DocumentChunkService chunkService;
    
    @Mock
    private FileStorageService fileStorageService;
    
    @Mock
    private TextExtractionService textExtractionService;
    
    @Mock
    private DocumentProcessingKafkaServiceInterface kafkaService;
    
    @InjectMocks
    private DocumentService documentService;

    private Tenant testTenant;
    private User testUser;
    private Document testDocument;
    private MultipartFile testFile;
    private UUID documentId;
    private UUID tenantId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        documentId = UUID.randomUUID();

        testTenant = createTestTenant(tenantId);
        testUser = createTestUser(userId, testTenant);
        testDocument = createTestDocument(documentId, testTenant, testUser);
        testFile = new MockMultipartFile("file", "test.pdf", "application/pdf", "test content".getBytes());
    }

    @Nested
    @DisplayName("Document Upload Tests")
    class DocumentUploadTests {

        @Test
        @DisplayName("Should successfully upload valid document")
        void shouldSuccessfullyUploadValidDocument() {
            // Given
            DocumentDto.UploadDocumentRequest request = new DocumentDto.UploadDocumentRequest(testFile, null);
            
            when(textExtractionService.detectDocumentType(testFile.getOriginalFilename(), testFile.getContentType()))
                .thenReturn(Document.DocumentType.PDF);
            when(textExtractionService.getMaxFileSize()).thenReturn(10485760L); // 10MB
            when(textExtractionService.isValidDocumentType(testFile.getOriginalFilename(), testFile.getContentType()))
                .thenReturn(true);
            when(documentRepository.countByTenantId(tenantId)).thenReturn(5L);
            when(fileStorageService.getTenantStorageUsage(tenantId)).thenReturn(1000000L); // 1MB
            when(documentRepository.save(any(Document.class))).thenReturn(testDocument);
            when(fileStorageService.storeFile(testFile, tenantId, testDocument.getId()))
                .thenReturn("/storage/tenant/document.pdf");

            // When
            DocumentDto.DocumentResponse response = documentService.uploadDocument(request, testTenant, testUser);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(testDocument.getId());
            assertThat(response.originalFilename()).isEqualTo("test.pdf");
            assertThat(response.documentType()).isEqualTo(Document.DocumentType.PDF);
            assertThat(response.processingStatus()).isEqualTo(Document.ProcessingStatus.PENDING);

            verify(documentRepository, times(2)).save(any(Document.class));
            verify(fileStorageService).storeFile(testFile, tenantId, testDocument.getId());
            verify(kafkaService).sendDocumentForProcessing(testDocument.getId());
        }

        @Test
        @DisplayName("Should handle file storage failure during upload")
        void shouldHandleFileStorageFailureDuringUpload() {
            // Given
            DocumentDto.UploadDocumentRequest request = new DocumentDto.UploadDocumentRequest(testFile, null);
            
            when(textExtractionService.detectDocumentType(testFile.getOriginalFilename(), testFile.getContentType()))
                .thenReturn(Document.DocumentType.PDF);
            when(textExtractionService.getMaxFileSize()).thenReturn(10485760L);
            when(textExtractionService.isValidDocumentType(testFile.getOriginalFilename(), testFile.getContentType()))
                .thenReturn(true);
            when(documentRepository.countByTenantId(tenantId)).thenReturn(5L);
            when(fileStorageService.getTenantStorageUsage(tenantId)).thenReturn(1000000L);
            when(documentRepository.save(any(Document.class))).thenReturn(testDocument);
            when(fileStorageService.storeFile(testFile, tenantId, testDocument.getId()))
                .thenThrow(new RuntimeException("Storage failure"));

            // When & Then
            assertThatThrownBy(() -> documentService.uploadDocument(request, testTenant, testUser))
                .isInstanceOf(DocumentProcessingException.class)
                .hasMessageContaining("Failed to upload document");

            ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
            verify(documentRepository, times(2)).save(documentCaptor.capture());
            
            Document failedDocument = documentCaptor.getAllValues().get(1);
            assertThat(failedDocument.getProcessingStatus()).isEqualTo(Document.ProcessingStatus.FAILED);
            assertThat(failedDocument.getProcessingMessage()).contains("Failed to store file");
        }

        @Test
        @DisplayName("Should upload document with null tenant and user (testing mode)")
        void shouldUploadDocumentWithNullTenantAndUser() {
            // Given
            DocumentDto.UploadDocumentRequest request = new DocumentDto.UploadDocumentRequest(testFile, null);
            
            when(textExtractionService.detectDocumentType(testFile.getOriginalFilename(), testFile.getContentType()))
                .thenReturn(Document.DocumentType.PDF);
            when(textExtractionService.getMaxFileSize()).thenReturn(10485760L);
            when(textExtractionService.isValidDocumentType(testFile.getOriginalFilename(), testFile.getContentType()))
                .thenReturn(true);
            when(documentRepository.countByTenantId(any(UUID.class))).thenReturn(5L);
            when(fileStorageService.getTenantStorageUsage(any(UUID.class))).thenReturn(1000000L);
            when(documentRepository.save(any(Document.class))).thenReturn(testDocument);
            when(fileStorageService.storeFile(eq(testFile), any(UUID.class), any(UUID.class)))
                .thenReturn("/storage/tenant/document.pdf");

            // When
            DocumentDto.DocumentResponse response = documentService.uploadDocument(request, null, null);

            // Then
            assertThat(response).isNotNull();
            verify(documentRepository, times(2)).save(any(Document.class));
        }
    }

    @Nested
    @DisplayName("File Validation Tests")
    class FileValidationTests {

        @Test
        @DisplayName("Should reject empty file")
        void shouldRejectEmptyFile() {
            // Given
            MultipartFile emptyFile = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[0]);
            DocumentDto.UploadDocumentRequest request = new DocumentDto.UploadDocumentRequest(emptyFile, null);

            // When & Then
            assertThatThrownBy(() -> documentService.uploadDocument(request, testTenant, testUser))
                .isInstanceOf(DocumentProcessingException.class)
                .hasMessageContaining("File is empty");
        }

        @Test
        @DisplayName("Should reject file exceeding size limit")
        void shouldRejectFileExceedingSizeLimit() {
            // Given
            MultipartFile largeFile = new MockMultipartFile("file", "large.pdf", "application/pdf", 
                new byte[20 * 1024 * 1024]); // 20MB
            DocumentDto.UploadDocumentRequest request = new DocumentDto.UploadDocumentRequest(largeFile, null);
            
            when(textExtractionService.getMaxFileSize()).thenReturn(10485760L); // 10MB

            // When & Then
            assertThatThrownBy(() -> documentService.uploadDocument(request, testTenant, testUser))
                .isInstanceOf(DocumentProcessingException.class)
                .hasMessageContaining("File size exceeds maximum allowed size");
        }

        @Test
        @DisplayName("Should reject unsupported file type")
        void shouldRejectUnsupportedFileType() {
            // Given
            MultipartFile unsupportedFile = new MockMultipartFile("file", "test.exe", "application/x-executable", 
                "executable content".getBytes());
            DocumentDto.UploadDocumentRequest request = new DocumentDto.UploadDocumentRequest(unsupportedFile, null);
            
            when(textExtractionService.getMaxFileSize()).thenReturn(10485760L);
            when(textExtractionService.isValidDocumentType(unsupportedFile.getOriginalFilename(), unsupportedFile.getContentType()))
                .thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> documentService.uploadDocument(request, testTenant, testUser))
                .isInstanceOf(DocumentProcessingException.class)
                .hasMessageContaining("Unsupported file type");
        }

        @Test
        @DisplayName("Should reject file when tenant document limit exceeded")
        void shouldRejectFileWhenTenantDocumentLimitExceeded() {
            // Given
            DocumentDto.UploadDocumentRequest request = new DocumentDto.UploadDocumentRequest(testFile, null);
            
            when(textExtractionService.getMaxFileSize()).thenReturn(10485760L);
            when(textExtractionService.isValidDocumentType(testFile.getOriginalFilename(), testFile.getContentType()))
                .thenReturn(true);
            when(documentRepository.countByTenantId(tenantId)).thenReturn(1000L); // Exceeds limit

            // When & Then
            assertThatThrownBy(() -> documentService.uploadDocument(request, testTenant, testUser))
                .isInstanceOf(DocumentProcessingException.class)
                .hasMessageContaining("Tenant document limit exceeded");
        }

        @Test
        @DisplayName("Should reject file when tenant storage limit exceeded")
        void shouldRejectFileWhenTenantStorageLimitExceeded() {
            // Given
            DocumentDto.UploadDocumentRequest request = new DocumentDto.UploadDocumentRequest(testFile, null);
            
            when(textExtractionService.getMaxFileSize()).thenReturn(10485760L);
            when(textExtractionService.isValidDocumentType(testFile.getOriginalFilename(), testFile.getContentType()))
                .thenReturn(true);
            when(documentRepository.countByTenantId(tenantId)).thenReturn(5L);
            when(fileStorageService.getTenantStorageUsage(tenantId)).thenReturn(1000L * 1024 * 1024); // 1GB, exceeds 500MB limit

            // When & Then
            assertThatThrownBy(() -> documentService.uploadDocument(request, testTenant, testUser))
                .isInstanceOf(DocumentProcessingException.class)
                .hasMessageContaining("Tenant storage limit exceeded");
        }
    }

    @Nested
    @DisplayName("Document Processing Tests")
    class DocumentProcessingTests {

        @Test
        @DisplayName("Should successfully process document")
        void shouldSuccessfullyProcessDocument() {
            // Given
            when(documentRepository.findById(documentId)).thenReturn(Optional.of(testDocument));
            when(fileStorageService.loadFile(testDocument.getFilePath())).thenReturn(Paths.get("/storage/test.pdf"));
            when(textExtractionService.extractText(any(Path.class), eq(Document.DocumentType.PDF)))
                .thenReturn("Extracted text content");
            when(textExtractionService.extractMetadata(any(Path.class)))
                .thenReturn(Map.of("author", "Test Author", "title", "Test Document"));
            
            List<DocumentChunk> chunks = List.of(createTestChunk(1), createTestChunk(2));
            when(chunkService.createChunks(eq(testDocument), eq("Extracted text content"), any(TenantDto.ChunkingConfig.class)))
                .thenReturn(chunks);
            when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

            // When
            documentService.processDocument(documentId);

            // Then
            ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
            verify(documentRepository, times(2)).save(documentCaptor.capture());
            
            Document processedDocument = documentCaptor.getAllValues().get(1);
            assertThat(processedDocument.getProcessingStatus()).isEqualTo(Document.ProcessingStatus.COMPLETED);
            assertThat(processedDocument.getExtractedText()).isEqualTo("Extracted text content");
            assertThat(processedDocument.getChunkCount()).isEqualTo(2);
            assertThat(processedDocument.getProcessingMessage()).isEqualTo("Successfully processed");

            verify(kafkaService).sendChunksForEmbedding(chunks);
        }

        @Test
        @DisplayName("Should handle document not found during processing")
        void shouldHandleDocumentNotFoundDuringProcessing() {
            // Given
            when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> documentService.processDocument(documentId))
                .isInstanceOf(DocumentNotFoundException.class);
        }

        @Test
        @DisplayName("Should handle text extraction failure during processing")
        void shouldHandleTextExtractionFailureDuringProcessing() {
            // Given
            when(documentRepository.findById(documentId)).thenReturn(Optional.of(testDocument));
            when(fileStorageService.loadFile(testDocument.getFilePath())).thenReturn(Paths.get("/storage/test.pdf"));
            when(textExtractionService.extractText(any(Path.class), eq(Document.DocumentType.PDF)))
                .thenThrow(new RuntimeException("Text extraction failed"));
            when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

            // When
            documentService.processDocument(documentId);

            // Then
            ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
            verify(documentRepository, times(2)).save(documentCaptor.capture());
            
            Document failedDocument = documentCaptor.getAllValues().get(1);
            assertThat(failedDocument.getProcessingStatus()).isEqualTo(Document.ProcessingStatus.FAILED);
            assertThat(failedDocument.getProcessingMessage()).contains("Processing failed");
        }
    }

    @Nested
    @DisplayName("Document CRUD Operations Tests")
    class DocumentCrudTests {

        @Test
        @DisplayName("Should successfully retrieve document by ID")
        void shouldSuccessfullyRetrieveDocumentById() {
            // Given
            when(documentRepository.findById(documentId)).thenReturn(Optional.of(testDocument));

            // When
            DocumentDto.DocumentResponse response = documentService.getDocument(documentId, tenantId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(documentId);
            assertThat(response.originalFilename()).isEqualTo(testDocument.getOriginalFilename());
        }

        @Test
        @DisplayName("Should throw exception when document not found")
        void shouldThrowExceptionWhenDocumentNotFound() {
            // Given
            when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> documentService.getDocument(documentId, tenantId))
                .isInstanceOf(DocumentNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when accessing document from different tenant")
        void shouldThrowExceptionWhenAccessingDocumentFromDifferentTenant() {
            // Given
            UUID differentTenantId = UUID.randomUUID();
            when(documentRepository.findById(documentId)).thenReturn(Optional.of(testDocument));

            // When & Then
            assertThatThrownBy(() -> documentService.getDocument(documentId, differentTenantId))
                .isInstanceOf(DocumentNotFoundException.class);
        }

        @Test
        @DisplayName("Should successfully retrieve documents by tenant")
        void shouldSuccessfullyRetrieveDocumentsByTenant() {
            // Given
            List<Document> documents = List.of(testDocument, createTestDocument(UUID.randomUUID(), testTenant, testUser));
            Page<Document> documentPage = new PageImpl<>(documents);
            when(documentRepository.findByTenantId(eq(tenantId), any(Pageable.class))).thenReturn(documentPage);

            // When
            Page<DocumentDto.DocumentSummary> result = documentService.getDocumentsByTenant(tenantId, Pageable.unpaged());

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.getContent().get(0).id()).isEqualTo(testDocument.getId());
        }

        @Test
        @DisplayName("Should successfully update document")
        void shouldSuccessfullyUpdateDocument() {
            // Given
            DocumentDto.UpdateDocumentRequest request = new DocumentDto.UpdateDocumentRequest(
                "updated-filename.pdf", Map.of("category", "updated"));
            when(documentRepository.findById(documentId)).thenReturn(Optional.of(testDocument));
            when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

            // When
            DocumentDto.DocumentResponse response = documentService.updateDocument(documentId, request, tenantId);

            // Then
            assertThat(response).isNotNull();
            verify(documentRepository).save(testDocument);
        }

        @Test
        @DisplayName("Should successfully delete document")
        void shouldSuccessfullyDeleteDocument() {
            // Given
            when(documentRepository.findById(documentId)).thenReturn(Optional.of(testDocument));

            // When
            documentService.deleteDocument(documentId, tenantId);

            // Then
            verify(chunkService).deleteChunksByDocument(documentId);
            verify(fileStorageService).deleteFile(testDocument.getFilePath());
            verify(documentRepository).delete(testDocument);
        }

        @Test
        @DisplayName("Should handle file deletion failure during document deletion")
        void shouldHandleFileDeletionFailureDuringDocumentDeletion() {
            // Given
            when(documentRepository.findById(documentId)).thenReturn(Optional.of(testDocument));
            doThrow(new RuntimeException("File deletion failed")).when(fileStorageService).deleteFile(testDocument.getFilePath());

            // When & Then
            assertThatThrownBy(() -> documentService.deleteDocument(documentId, tenantId))
                .isInstanceOf(DocumentProcessingException.class)
                .hasMessageContaining("Failed to delete document");
        }
    }

    @Nested
    @DisplayName("Document Statistics Tests")
    class DocumentStatisticsTests {

        @Test
        @DisplayName("Should return correct document count by tenant")
        void shouldReturnCorrectDocumentCountByTenant() {
            // Given
            when(documentRepository.countByTenantId(tenantId)).thenReturn(42L);

            // When
            long count = documentService.getDocumentCountByTenant(tenantId);

            // Then
            assertThat(count).isEqualTo(42L);
        }

        @Test
        @DisplayName("Should return correct storage usage by tenant")
        void shouldReturnCorrectStorageUsageByTenant() {
            // Given
            when(fileStorageService.getTenantStorageUsage(tenantId)).thenReturn(1048576L); // 1MB

            // When
            long usage = documentService.getStorageUsageByTenant(tenantId);

            // Then
            assertThat(usage).isEqualTo(1048576L);
        }

        @Test
        @DisplayName("Should return correct total document count")
        void shouldReturnCorrectTotalDocumentCount() {
            // Given
            when(documentRepository.count()).thenReturn(150L);

            // When
            long count = documentService.getTotalDocumentCount();

            // Then
            assertThat(count).isEqualTo(150L);
        }

        @Test
        @DisplayName("Should return correct pending document count")
        void shouldReturnCorrectPendingDocumentCount() {
            // Given
            when(documentRepository.countByProcessingStatus(Document.ProcessingStatus.PENDING)).thenReturn(12L);

            // When
            long count = documentService.getPendingDocumentCount();

            // Then
            assertThat(count).isEqualTo(12L);
        }

        @Test
        @DisplayName("Should return correct processing document count")
        void shouldReturnCorrectProcessingDocumentCount() {
            // Given
            when(documentRepository.countByProcessingStatus(Document.ProcessingStatus.PROCESSING)).thenReturn(8L);

            // When
            long count = documentService.getProcessingDocumentCount();

            // Then
            assertThat(count).isEqualTo(8L);
        }
    }

    // Helper methods for creating test data
    private Tenant createTestTenant(UUID tenantId) {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("Test Company");
        tenant.setSlug("test-company");
        tenant.setStatus(Tenant.TenantStatus.ACTIVE);
        tenant.setMaxDocuments(100);
        tenant.setMaxStorageMb(500L);
        return tenant;
    }

    private User createTestUser(UUID userId, Tenant tenant) {
        User user = new User();
        user.setId(userId);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test@test-company.com");
        user.setTenant(tenant);
        user.setRole(User.UserRole.USER);
        user.setStatus(User.UserStatus.ACTIVE);
        return user;
    }

    private Document createTestDocument(UUID documentId, Tenant tenant, User user) {
        Document document = new Document();
        document.setId(documentId);
        document.setOriginalFilename("test.pdf");
        document.setFilename("test-" + documentId + ".pdf");
        document.setFileSize(1024L);
        document.setContentType("application/pdf");
        document.setDocumentType(Document.DocumentType.PDF);
        document.setFilePath("/storage/tenant/test.pdf");
        document.setTenant(tenant);
        document.setUploadedBy(user);
        document.setProcessingStatus(Document.ProcessingStatus.PENDING);
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        return document;
    }

    private DocumentChunk createTestChunk(int sequenceNumber) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setId(UUID.randomUUID());
        chunk.setContent("Test chunk content " + sequenceNumber);
        chunk.setSequenceNumber(sequenceNumber);
        chunk.setTokenCount(10);
        chunk.setDocument(testDocument);
        chunk.setTenant(testTenant);
        return chunk;
    }
}
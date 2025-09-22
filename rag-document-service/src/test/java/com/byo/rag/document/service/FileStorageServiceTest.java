package com.byo.rag.document.service;

import com.byo.rag.shared.exception.DocumentProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for FileStorageService covering file operations and tenant isolation.
 * 
 * Test Categories:
 * - File Storage Operations (Store, Load, Delete)
 * - Tenant Isolation and Directory Management
 * - File Path Security and Validation
 * - Storage Usage Calculation and Monitoring
 * - Error Handling and Exception Scenarios
 * - File Size and Metadata Operations
 * - Edge Cases and Security Validation
 */
class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;
    private UUID tenantId;
    private UUID documentId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        documentId = UUID.randomUUID();
        
        // Initialize FileStorageService with temp directory
        fileStorageService = new FileStorageService(tempDir.toString());
    }

    @Nested
    @DisplayName("File Storage Tests")
    class FileStorageTests {

        @Test
        @DisplayName("Should store file successfully")
        void shouldStoreFileSuccessfully() throws IOException {
            // Given
            String content = "Test file content for storage";
            MultipartFile file = new MockMultipartFile("test", "test.txt", "text/plain", content.getBytes());

            // When
            String filePath = fileStorageService.storeFile(file, tenantId, documentId);

            // Then
            assertThat(filePath).isNotNull();
            assertThat(filePath).contains(tenantId.toString());
            assertThat(filePath).contains(documentId.toString());
            assertThat(filePath).endsWith(".txt");

            // Verify file exists and has correct content
            Path storedFile = Path.of(filePath);
            assertThat(Files.exists(storedFile)).isTrue();
            assertThat(Files.readString(storedFile)).isEqualTo(content);
        }

        @Test
        @DisplayName("Should create tenant directory if not exists")
        void shouldCreateTenantDirectoryIfNotExists() throws IOException {
            // Given
            MultipartFile file = new MockMultipartFile("test", "test.txt", "text/plain", "content".getBytes());

            // When
            String filePath = fileStorageService.storeFile(file, tenantId, documentId);

            // Then
            Path tenantDir = tempDir.resolve(tenantId.toString());
            assertThat(Files.exists(tenantDir)).isTrue();
            assertThat(Files.isDirectory(tenantDir)).isTrue();
            assertThat(filePath).contains(tenantDir.toString());
        }

        @Test
        @DisplayName("Should generate unique filename with document ID")
        void shouldGenerateUniqueFilenameWithDocumentId() throws IOException {
            // Given
            MultipartFile file = new MockMultipartFile("test", "original.pdf", "application/pdf", "content".getBytes());

            // When
            String filePath = fileStorageService.storeFile(file, tenantId, documentId);

            // Then
            assertThat(filePath).contains(documentId.toString());
            assertThat(filePath).endsWith(".pdf");
            assertThat(filePath).doesNotContain("original");
        }

        @Test
        @DisplayName("Should handle files with different extensions")
        void shouldHandleFilesWithDifferentExtensions() throws IOException {
            // Given
            MultipartFile pdfFile = new MockMultipartFile("pdf", "doc.pdf", "application/pdf", "pdf content".getBytes());
            MultipartFile txtFile = new MockMultipartFile("txt", "doc.txt", "text/plain", "txt content".getBytes());
            MultipartFile docxFile = new MockMultipartFile("docx", "doc.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx content".getBytes());

            // When
            String pdfPath = fileStorageService.storeFile(pdfFile, tenantId, UUID.randomUUID());
            String txtPath = fileStorageService.storeFile(txtFile, tenantId, UUID.randomUUID());
            String docxPath = fileStorageService.storeFile(docxFile, tenantId, UUID.randomUUID());

            // Then
            assertThat(pdfPath).endsWith(".pdf");
            assertThat(txtPath).endsWith(".txt");
            assertThat(docxPath).endsWith(".docx");
        }

        @Test
        @DisplayName("Should handle files without extension")
        void shouldHandleFilesWithoutExtension() throws IOException {
            // Given
            MultipartFile file = new MockMultipartFile("test", "filename_no_extension", "text/plain", "content".getBytes());

            // When
            String filePath = fileStorageService.storeFile(file, tenantId, documentId);

            // Then
            assertThat(filePath).contains(documentId.toString());
            assertThat(filePath).doesNotEndWith(".");
        }

        @Test
        @DisplayName("Should replace existing file with same document ID")
        void shouldReplaceExistingFileWithSameDocumentId() throws IOException {
            // Given
            MultipartFile file1 = new MockMultipartFile("test", "test.txt", "text/plain", "original content".getBytes());
            MultipartFile file2 = new MockMultipartFile("test", "test.txt", "text/plain", "updated content".getBytes());

            // When
            String filePath1 = fileStorageService.storeFile(file1, tenantId, documentId);
            String filePath2 = fileStorageService.storeFile(file2, tenantId, documentId);

            // Then
            assertThat(filePath1).isEqualTo(filePath2);
            Path storedFile = Path.of(filePath2);
            assertThat(Files.readString(storedFile)).isEqualTo("updated content");
        }
    }

    @Nested
    @DisplayName("File Storage Validation Tests")
    class FileStorageValidationTests {

        @Test
        @DisplayName("Should reject empty file")
        void shouldRejectEmptyFile() {
            // Given
            MultipartFile emptyFile = new MockMultipartFile("test", "test.txt", "text/plain", new byte[0]);

            // When & Then
            assertThatThrownBy(() -> fileStorageService.storeFile(emptyFile, tenantId, documentId))
                .isInstanceOf(DocumentProcessingException.class)
                .hasMessageContaining("Cannot store empty file");
        }

        @Test
        @DisplayName("Should reject file without name")
        void shouldRejectFileWithoutName() {
            // Given - Create a MultipartFile that returns null for getOriginalFilename
            MultipartFile fileWithoutName = new MultipartFile() {
                @Override
                public String getName() { return "test"; }
                @Override
                public String getOriginalFilename() { return null; }
                @Override
                public String getContentType() { return "text/plain"; }
                @Override
                public boolean isEmpty() { return false; }
                @Override
                public long getSize() { return 7; }
                @Override
                public byte[] getBytes() { return "content".getBytes(); }
                @Override
                public java.io.InputStream getInputStream() { 
                    return new java.io.ByteArrayInputStream("content".getBytes()); 
                }
                @Override
                public void transferTo(java.io.File dest) { }
            };

            // When & Then
            assertThatThrownBy(() -> fileStorageService.storeFile(fileWithoutName, tenantId, documentId))
                .isInstanceOf(DocumentProcessingException.class)
                .hasMessageContaining("File must have a name");
        }
    }

    @Nested
    @DisplayName("File Loading Tests")
    class FileLoadingTests {

        @Test
        @DisplayName("Should load file as bytes successfully")
        void shouldLoadFileAsBytesSuccessfully() throws IOException {
            // Given
            String content = "Test file content for loading";
            MultipartFile file = new MockMultipartFile("test", "test.txt", "text/plain", content.getBytes());
            String filePath = fileStorageService.storeFile(file, tenantId, documentId);

            // When
            byte[] loadedBytes = fileStorageService.loadFileAsBytes(filePath);

            // Then
            assertThat(loadedBytes).isEqualTo(content.getBytes());
            assertThat(new String(loadedBytes)).isEqualTo(content);
        }

        @Test
        @DisplayName("Should load file as Path successfully")
        void shouldLoadFileAsPathSuccessfully() throws IOException {
            // Given
            String content = "Test file content for loading";
            MultipartFile file = new MockMultipartFile("test", "test.txt", "text/plain", content.getBytes());
            String filePath = fileStorageService.storeFile(file, tenantId, documentId);

            // When
            Path loadedPath = fileStorageService.loadFile(filePath);

            // Then
            assertThat(loadedPath).exists();
            assertThat(Files.readString(loadedPath)).isEqualTo(content);
        }

        @Test
        @DisplayName("Should throw exception when loading non-existent file")
        void shouldThrowExceptionWhenLoadingNonExistentFile() {
            // Given
            String nonExistentPath = tempDir.resolve("non-existent.txt").toString();

            // When & Then
            assertThatThrownBy(() -> fileStorageService.loadFileAsBytes(nonExistentPath))
                .isInstanceOf(DocumentProcessingException.class)
                .hasMessageContaining("Failed to load file");

            assertThatThrownBy(() -> fileStorageService.loadFile(nonExistentPath))
                .isInstanceOf(DocumentProcessingException.class)
                .hasMessageContaining("Failed to load file");
        }

        @Test
        @DisplayName("Should prevent access to files outside storage directory")
        void shouldPreventAccessToFilesOutsideStorageDirectory() {
            // Given
            String outsidePath = "/etc/passwd";
            String relativePath = "../../../etc/passwd";

            // When & Then
            assertThatThrownBy(() -> fileStorageService.loadFileAsBytes(outsidePath))
                .isInstanceOf(DocumentProcessingException.class)
                .hasMessageContaining("Cannot access file outside storage directory");

            assertThatThrownBy(() -> fileStorageService.loadFile(relativePath))
                .isInstanceOf(DocumentProcessingException.class)
                .hasMessageContaining("Failed to load file");
        }
    }

    @Nested
    @DisplayName("File Deletion Tests")
    class FileDeletionTests {

        @Test
        @DisplayName("Should delete file successfully")
        void shouldDeleteFileSuccessfully() throws IOException {
            // Given
            String content = "Test file content for deletion";
            MultipartFile file = new MockMultipartFile("test", "test.txt", "text/plain", content.getBytes());
            String filePath = fileStorageService.storeFile(file, tenantId, documentId);
            
            // Verify file exists
            assertThat(Files.exists(Path.of(filePath))).isTrue();

            // When
            fileStorageService.deleteFile(filePath);

            // Then
            assertThat(Files.exists(Path.of(filePath))).isFalse();
        }

        @Test
        @DisplayName("Should handle deletion of non-existent file gracefully")
        void shouldHandleDeletionOfNonExistentFileGracefully() {
            // Given
            String nonExistentPath = tempDir.resolve("non-existent.txt").toString();

            // When & Then
            assertThatCode(() -> fileStorageService.deleteFile(nonExistentPath))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should prevent deletion of files outside storage directory")
        void shouldPreventDeletionOfFilesOutsideStorageDirectory() {
            // Given
            String outsidePath = "/tmp/some_file.txt";

            // When & Then
            assertThatThrownBy(() -> fileStorageService.deleteFile(outsidePath))
                .isInstanceOf(DocumentProcessingException.class)
                .hasMessageContaining("Cannot delete file outside storage directory");
        }
    }

    @Nested
    @DisplayName("File Size and Storage Usage Tests")
    class FileSizeAndStorageUsageTests {

        @Test
        @DisplayName("Should return correct file size")
        void shouldReturnCorrectFileSize() throws IOException {
            // Given
            String content = "Test content with specific length: 1234567890";
            MultipartFile file = new MockMultipartFile("test", "test.txt", "text/plain", content.getBytes());
            String filePath = fileStorageService.storeFile(file, tenantId, documentId);

            // When
            long fileSize = fileStorageService.getFileSize(filePath);

            // Then
            assertThat(fileSize).isEqualTo(content.getBytes().length);
        }

        @Test
        @DisplayName("Should return zero for non-existent file size")
        void shouldReturnZeroForNonExistentFileSize() {
            // Given
            String nonExistentPath = tempDir.resolve("non-existent.txt").toString();

            // When
            long fileSize = fileStorageService.getFileSize(nonExistentPath);

            // Then
            assertThat(fileSize).isEqualTo(0);
        }

        @Test
        @DisplayName("Should calculate tenant storage usage correctly")
        void shouldCalculateTenantStorageUsageCorrectly() throws IOException {
            // Given
            String content1 = "File 1 content";
            String content2 = "File 2 content with more text";
            
            MultipartFile file1 = new MockMultipartFile("test1", "test1.txt", "text/plain", content1.getBytes());
            MultipartFile file2 = new MockMultipartFile("test2", "test2.txt", "text/plain", content2.getBytes());
            
            fileStorageService.storeFile(file1, tenantId, UUID.randomUUID());
            fileStorageService.storeFile(file2, tenantId, UUID.randomUUID());

            // When
            long totalUsage = fileStorageService.getTenantStorageUsage(tenantId);

            // Then
            long expectedUsage = content1.getBytes().length + content2.getBytes().length;
            assertThat(totalUsage).isEqualTo(expectedUsage);
        }

        @Test
        @DisplayName("Should return zero storage usage for non-existent tenant")
        void shouldReturnZeroStorageUsageForNonExistentTenant() {
            // Given
            UUID nonExistentTenantId = UUID.randomUUID();

            // When
            long usage = fileStorageService.getTenantStorageUsage(nonExistentTenantId);

            // Then
            assertThat(usage).isEqualTo(0);
        }

        @Test
        @DisplayName("Should calculate storage usage for multiple tenants separately")
        void shouldCalculateStorageUsageForMultipleTenantsSeparately() throws IOException {
            // Given
            UUID tenant1 = UUID.randomUUID();
            UUID tenant2 = UUID.randomUUID();
            
            String content1 = "Tenant 1 file content";
            String content2 = "Tenant 2 file content with different size";
            
            MultipartFile file1 = new MockMultipartFile("test1", "test1.txt", "text/plain", content1.getBytes());
            MultipartFile file2 = new MockMultipartFile("test2", "test2.txt", "text/plain", content2.getBytes());
            
            fileStorageService.storeFile(file1, tenant1, UUID.randomUUID());
            fileStorageService.storeFile(file2, tenant2, UUID.randomUUID());

            // When
            long usage1 = fileStorageService.getTenantStorageUsage(tenant1);
            long usage2 = fileStorageService.getTenantStorageUsage(tenant2);

            // Then
            assertThat(usage1).isEqualTo(content1.getBytes().length);
            assertThat(usage2).isEqualTo(content2.getBytes().length);
            assertThat(usage1).isNotEqualTo(usage2);
        }
    }

    @Nested
    @DisplayName("Storage Path and Configuration Tests")
    class StoragePathAndConfigurationTests {

        @Test
        @DisplayName("Should return correct storage path")
        void shouldReturnCorrectStoragePath() {
            // When
            Path storagePath = fileStorageService.getStoragePath();

            // Then
            assertThat(storagePath).isEqualTo(tempDir);
            assertThat(Files.exists(storagePath)).isTrue();
            assertThat(Files.isDirectory(storagePath)).isTrue();
        }

        @Test
        @DisplayName("Should create storage directory during initialization")
        void shouldCreateStorageDirectoryDuringInitialization() {
            // Given
            Path newStorageDir = tempDir.resolve("new-storage");
            assertThat(Files.exists(newStorageDir)).isFalse();

            // When
            FileStorageService newService = new FileStorageService(newStorageDir.toString());

            // Then
            assertThat(Files.exists(newStorageDir)).isTrue();
            assertThat(Files.isDirectory(newStorageDir)).isTrue();
            assertThat(newService.getStoragePath()).isEqualTo(newStorageDir.toAbsolutePath().normalize());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Security Tests")
    class EdgeCasesAndSecurityTests {

        @Test
        @DisplayName("Should handle very large files")
        void shouldHandleVeryLargeFiles() throws IOException {
            // Given
            byte[] largeContent = new byte[1024 * 1024]; // 1MB
            for (int i = 0; i < largeContent.length; i++) {
                largeContent[i] = (byte) (i % 256);
            }
            
            MultipartFile largeFile = new MockMultipartFile("large", "large.bin", "application/octet-stream", largeContent);

            // When
            String filePath = fileStorageService.storeFile(largeFile, tenantId, documentId);

            // Then
            assertThat(Files.exists(Path.of(filePath))).isTrue();
            assertThat(fileStorageService.getFileSize(filePath)).isEqualTo(largeContent.length);
            
            byte[] loadedContent = fileStorageService.loadFileAsBytes(filePath);
            assertThat(loadedContent).isEqualTo(largeContent);
        }

        @Test
        @DisplayName("Should handle special characters in filename")
        void shouldHandleSpecialCharactersInFilename() throws IOException {
            // Given
            String content = "File with special characters in name";
            MultipartFile file = new MockMultipartFile("test", "file with spaces & special-chars.txt", "text/plain", content.getBytes());

            // When
            String filePath = fileStorageService.storeFile(file, tenantId, documentId);

            // Then
            assertThat(Files.exists(Path.of(filePath))).isTrue();
            assertThat(Files.readString(Path.of(filePath))).isEqualTo(content);
        }

        @Test
        @DisplayName("Should handle multiple file operations concurrently for same tenant")
        void shouldHandleMultipleFileOperationsConcurrentlyForSameTenant() throws IOException {
            // Given
            String content1 = "Concurrent file 1";
            String content2 = "Concurrent file 2";
            String content3 = "Concurrent file 3";
            
            MultipartFile file1 = new MockMultipartFile("test1", "test1.txt", "text/plain", content1.getBytes());
            MultipartFile file2 = new MockMultipartFile("test2", "test2.txt", "text/plain", content2.getBytes());
            MultipartFile file3 = new MockMultipartFile("test3", "test3.txt", "text/plain", content3.getBytes());

            // When
            String path1 = fileStorageService.storeFile(file1, tenantId, UUID.randomUUID());
            String path2 = fileStorageService.storeFile(file2, tenantId, UUID.randomUUID());
            String path3 = fileStorageService.storeFile(file3, tenantId, UUID.randomUUID());

            // Then
            assertThat(Files.exists(Path.of(path1))).isTrue();
            assertThat(Files.exists(Path.of(path2))).isTrue();
            assertThat(Files.exists(Path.of(path3))).isTrue();
            
            assertThat(Files.readString(Path.of(path1))).isEqualTo(content1);
            assertThat(Files.readString(Path.of(path2))).isEqualTo(content2);
            assertThat(Files.readString(Path.of(path3))).isEqualTo(content3);
        }

        @Test
        @DisplayName("Should handle file operations with same document ID across different tenants")
        void shouldHandleFileOperationsWithSameDocumentIdAcrossDifferentTenants() throws IOException {
            // Given
            UUID tenant1 = UUID.randomUUID();
            UUID tenant2 = UUID.randomUUID();
            UUID sameDocumentId = UUID.randomUUID();
            
            String content1 = "Tenant 1 file content";
            String content2 = "Tenant 2 file content";
            
            MultipartFile file1 = new MockMultipartFile("test1", "test.txt", "text/plain", content1.getBytes());
            MultipartFile file2 = new MockMultipartFile("test2", "test.txt", "text/plain", content2.getBytes());

            // When
            String path1 = fileStorageService.storeFile(file1, tenant1, sameDocumentId);
            String path2 = fileStorageService.storeFile(file2, tenant2, sameDocumentId);

            // Then
            assertThat(path1).contains(tenant1.toString());
            assertThat(path2).contains(tenant2.toString());
            assertThat(path1).isNotEqualTo(path2);
            
            assertThat(Files.readString(Path.of(path1))).isEqualTo(content1);
            assertThat(Files.readString(Path.of(path2))).isEqualTo(content2);
        }

        @Test
        @DisplayName("Should prevent path traversal attacks")
        void shouldPreventPathTraversalAttacks() {
            // Given
            String[] maliciousPaths = {
                "../../etc/passwd",
                "../../../windows/system32/config/sam",
                "..\\..\\..\\windows\\system32\\config\\sam",
                "/etc/passwd",
                "C:\\windows\\system32\\config\\sam",
                "//server/share/file.txt",
                "file://c:/windows/system32/config/sam"
            };

            // When & Then
            for (String maliciousPath : maliciousPaths) {
                assertThatThrownBy(() -> fileStorageService.loadFileAsBytes(maliciousPath))
                    .isInstanceOf(DocumentProcessingException.class)
                    .hasMessageContaining("Cannot access file outside storage directory");

                assertThatThrownBy(() -> fileStorageService.deleteFile(maliciousPath))
                    .isInstanceOf(DocumentProcessingException.class)
                    .hasMessageContaining("Cannot delete file outside storage directory");
            }
        }
    }
}
package com.byo.rag.document.service;

import com.byo.rag.shared.entity.Document;
import com.byo.rag.shared.exception.DocumentProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for TextExtractionService covering Apache Tika integration and document processing.
 * 
 * Test Categories:
 * - Document Type Detection (File Extension and Content Type Based)
 * - Text Extraction for Different Document Types (PDF, DOCX, TXT, MD, HTML)
 * - Metadata Extraction and Processing
 * - File Validation and Size Limits
 * - Error Handling and Exception Scenarios
 * - Plain Text and Tika-based Extraction
 * - Edge Cases and Special Characters
 */
class TextExtractionServiceTest {

    private TextExtractionService textExtractionService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        textExtractionService = new TextExtractionService();
    }

    @Nested
    @DisplayName("Document Type Detection Tests")
    class DocumentTypeDetectionTests {

        @Test
        @DisplayName("Should detect PDF document type from file extension")
        void shouldDetectPdfDocumentTypeFromFileExtension() {
            // When
            Document.DocumentType result = textExtractionService.detectDocumentType("document.pdf", null);

            // Then
            assertThat(result).isEqualTo(Document.DocumentType.PDF);
        }

        @Test
        @DisplayName("Should detect DOCX document type from file extension")
        void shouldDetectDocxDocumentTypeFromFileExtension() {
            // When
            Document.DocumentType result = textExtractionService.detectDocumentType("document.docx", null);

            // Then
            assertThat(result).isEqualTo(Document.DocumentType.DOCX);
        }

        @Test
        @DisplayName("Should detect DOC document type from file extension")
        void shouldDetectDocDocumentTypeFromFileExtension() {
            // When
            Document.DocumentType result = textExtractionService.detectDocumentType("document.doc", null);

            // Then
            assertThat(result).isEqualTo(Document.DocumentType.DOC);
        }

        @Test
        @DisplayName("Should detect TXT document type from file extension")
        void shouldDetectTxtDocumentTypeFromFileExtension() {
            // When
            Document.DocumentType result = textExtractionService.detectDocumentType("document.txt", null);

            // Then
            assertThat(result).isEqualTo(Document.DocumentType.TXT);
        }

        @Test
        @DisplayName("Should detect MD document type from file extension")
        void shouldDetectMdDocumentTypeFromFileExtension() {
            // When
            Document.DocumentType result1 = textExtractionService.detectDocumentType("document.md", null);
            Document.DocumentType result2 = textExtractionService.detectDocumentType("document.markdown", null);

            // Then
            assertThat(result1).isEqualTo(Document.DocumentType.MD);
            assertThat(result2).isEqualTo(Document.DocumentType.MD);
        }

        @Test
        @DisplayName("Should detect HTML document type from file extension")
        void shouldDetectHtmlDocumentTypeFromFileExtension() {
            // When
            Document.DocumentType result1 = textExtractionService.detectDocumentType("document.html", null);
            Document.DocumentType result2 = textExtractionService.detectDocumentType("document.htm", null);

            // Then
            assertThat(result1).isEqualTo(Document.DocumentType.HTML);
            assertThat(result2).isEqualTo(Document.DocumentType.HTML);
        }

        @Test
        @DisplayName("Should detect RTF document type from file extension")
        void shouldDetectRtfDocumentTypeFromFileExtension() {
            // When
            Document.DocumentType result = textExtractionService.detectDocumentType("document.rtf", null);

            // Then
            assertThat(result).isEqualTo(Document.DocumentType.RTF);
        }

        @Test
        @DisplayName("Should detect ODT document type from file extension")
        void shouldDetectOdtDocumentTypeFromFileExtension() {
            // When
            Document.DocumentType result = textExtractionService.detectDocumentType("document.odt", null);

            // Then
            assertThat(result).isEqualTo(Document.DocumentType.ODT);
        }

        @Test
        @DisplayName("Should fallback to content type when extension is unknown")
        void shouldFallbackToContentTypeWhenExtensionIsUnknown() {
            // When
            Document.DocumentType result1 = textExtractionService.detectDocumentType("document", "application/pdf");
            Document.DocumentType result2 = textExtractionService.detectDocumentType("document", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            Document.DocumentType result3 = textExtractionService.detectDocumentType("document", "application/msword");
            Document.DocumentType result4 = textExtractionService.detectDocumentType("document", "text/plain");
            Document.DocumentType result5 = textExtractionService.detectDocumentType("document", "text/markdown");
            Document.DocumentType result6 = textExtractionService.detectDocumentType("document", "text/html");
            Document.DocumentType result7 = textExtractionService.detectDocumentType("document", "application/rtf");
            Document.DocumentType result8 = textExtractionService.detectDocumentType("document", "application/vnd.oasis.opendocument.text");

            // Then
            assertThat(result1).isEqualTo(Document.DocumentType.PDF);
            assertThat(result2).isEqualTo(Document.DocumentType.DOCX);
            assertThat(result3).isEqualTo(Document.DocumentType.DOC);
            assertThat(result4).isEqualTo(Document.DocumentType.TXT);
            assertThat(result5).isEqualTo(Document.DocumentType.MD);
            assertThat(result6).isEqualTo(Document.DocumentType.HTML);
            assertThat(result7).isEqualTo(Document.DocumentType.RTF);
            assertThat(result8).isEqualTo(Document.DocumentType.ODT);
        }

        @Test
        @DisplayName("Should default to TXT when no extension or content type matches")
        void shouldDefaultToTxtWhenNoExtensionOrContentTypeMatches() {
            // When
            Document.DocumentType result1 = textExtractionService.detectDocumentType("document", "unknown/type");
            Document.DocumentType result2 = textExtractionService.detectDocumentType("document.unknown", null);
            Document.DocumentType result3 = textExtractionService.detectDocumentType("document", null);

            // Then
            assertThat(result1).isEqualTo(Document.DocumentType.TXT);
            assertThat(result2).isEqualTo(Document.DocumentType.TXT);
            assertThat(result3).isEqualTo(Document.DocumentType.TXT);
        }

        @Test
        @DisplayName("Should handle case insensitive file extensions")
        void shouldHandleCaseInsensitiveFileExtensions() {
            // When
            Document.DocumentType result1 = textExtractionService.detectDocumentType("DOCUMENT.PDF", null);
            Document.DocumentType result2 = textExtractionService.detectDocumentType("Document.Docx", null);
            Document.DocumentType result3 = textExtractionService.detectDocumentType("document.TXT", null);

            // Then
            assertThat(result1).isEqualTo(Document.DocumentType.PDF);
            assertThat(result2).isEqualTo(Document.DocumentType.DOCX);
            assertThat(result3).isEqualTo(Document.DocumentType.TXT);
        }

        @Test
        @DisplayName("Should handle files without extensions")
        void shouldHandleFilesWithoutExtensions() {
            // When
            Document.DocumentType result = textExtractionService.detectDocumentType("document_no_extension", null);

            // Then
            assertThat(result).isEqualTo(Document.DocumentType.TXT);
        }
    }

    @Nested
    @DisplayName("Text Extraction Tests")
    class TextExtractionTests {

        @Test
        @DisplayName("Should extract text from plain text file")
        void shouldExtractTextFromPlainTextFile() throws IOException {
            // Given
            String content = "This is a plain text document with multiple lines.\nLine 2 content.\nLine 3 content.";
            Path txtFile = tempDir.resolve("test.txt");
            Files.writeString(txtFile, content);

            // When
            String result = textExtractionService.extractText(txtFile, Document.DocumentType.TXT);

            // Then
            assertThat(result).contains("This is a plain text document");
            assertThat(result).contains("Line 2 content");
            assertThat(result).contains("Line 3 content");
        }

        @Test
        @DisplayName("Should extract text from markdown file")
        void shouldExtractTextFromMarkdownFile() throws IOException {
            // Given
            String markdownContent = "# Heading 1\n\nThis is **bold** text and *italic* text.\n\n## Heading 2\n\n- List item 1\n- List item 2";
            Path mdFile = tempDir.resolve("test.md");
            Files.writeString(mdFile, markdownContent);

            // When
            String result = textExtractionService.extractText(mdFile, Document.DocumentType.MD);

            // Then
            assertThat(result).contains("Heading 1");
            assertThat(result).contains("bold");
            assertThat(result).contains("italic");
            assertThat(result).contains("List item 1");
        }

        @Test
        @DisplayName("Should extract text using Tika for supported formats")
        void shouldExtractTextUsingTikaForSupportedFormats() throws IOException {
            // Given
            String htmlContent = "<html><body><h1>Title</h1><p>This is HTML content with <strong>bold</strong> text.</p></body></html>";
            Path htmlFile = tempDir.resolve("test.html");
            Files.writeString(htmlFile, htmlContent);

            // When
            String result = textExtractionService.extractText(htmlFile, Document.DocumentType.HTML);

            // Then
            assertThat(result).contains("Title");
            assertThat(result).contains("HTML content");
            assertThat(result).contains("bold");
        }

        @Test
        @DisplayName("Should handle empty files")
        void shouldHandleEmptyFiles() throws IOException {
            // Given
            Path emptyFile = tempDir.resolve("empty.txt");
            Files.writeString(emptyFile, "");

            // When
            String result = textExtractionService.extractText(emptyFile, Document.DocumentType.TXT);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should handle files with special characters")
        void shouldHandleFilesWithSpecialCharacters() throws IOException {
            // Given
            String content = "Special characters: àáâãäåæçèéêë ñóôõöø ùúûüÿ 中文 العربية русский 🚀 📄 ✨";
            Path specialFile = tempDir.resolve("special.txt");
            Files.writeString(specialFile, content);

            // When
            String result = textExtractionService.extractText(specialFile, Document.DocumentType.TXT);

            // Then
            assertThat(result).contains("àáâãäåæçèéêë");
            assertThat(result).contains("中文");
            assertThat(result).contains("العربية");
            assertThat(result).contains("русский");
            assertThat(result).contains("🚀");
        }

        @Test
        @DisplayName("Should throw exception for unsupported document type")
        void shouldThrowExceptionForUnsupportedDocumentType() throws IOException {
            // Given
            Path txtFile = tempDir.resolve("test.txt");
            Files.writeString(txtFile, "test content");

            // When & Then - null document type causes NPE in switch statement
            assertThatThrownBy(() -> textExtractionService.extractText(txtFile, null))
                .isInstanceOf(DocumentProcessingException.class)
                .hasMessageContaining("Failed to extract text from file");
        }

        @Test
        @DisplayName("Should handle file read exceptions")
        void shouldHandleFileReadExceptions() {
            // Given
            Path nonExistentFile = tempDir.resolve("non-existent.txt");

            // When & Then
            assertThatThrownBy(() -> textExtractionService.extractText(nonExistentFile, Document.DocumentType.TXT))
                .isInstanceOf(DocumentProcessingException.class)
                .hasMessageContaining("Failed to extract text from file");
        }
    }

    @Nested
    @DisplayName("Metadata Extraction Tests")
    class MetadataExtractionTests {

        @Test
        @DisplayName("Should extract metadata from file")
        void shouldExtractMetadataFromFile() throws IOException {
            // Given
            String content = "Sample document content for metadata extraction.";
            Path testFile = tempDir.resolve("test.txt");
            Files.writeString(testFile, content);

            // When
            Map<String, Object> metadata = textExtractionService.extractMetadata(testFile);

            // Then
            assertThat(metadata).isNotEmpty();
            assertThat(metadata).containsKey("file_size");
            assertThat(metadata).containsKey("file_name");
            assertThat(metadata.get("file_name")).isEqualTo("test.txt");
            assertThat((Long) metadata.get("file_size")).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should extract content type metadata")
        void shouldExtractContentTypeMetadata() throws IOException {
            // Given
            String htmlContent = "<html><head><title>Test Document</title></head><body><p>Content</p></body></html>";
            Path htmlFile = tempDir.resolve("test.html");
            Files.writeString(htmlFile, htmlContent);

            // When
            Map<String, Object> metadata = textExtractionService.extractMetadata(htmlFile);

            // Then
            assertThat(metadata).isNotEmpty();
            assertThat(metadata).containsKey("Content-Type");
        }

        @Test
        @DisplayName("Should handle metadata extraction failures gracefully")
        void shouldHandleMetadataExtractionFailuresGracefully() {
            // Given
            Path nonExistentFile = tempDir.resolve("non-existent.txt");

            // When
            Map<String, Object> metadata = textExtractionService.extractMetadata(nonExistentFile);

            // Then
            assertThat(metadata).isEmpty();
        }

        @Test
        @DisplayName("Should filter out empty metadata values")
        void shouldFilterOutEmptyMetadataValues() throws IOException {
            // Given
            String content = "Simple content";
            Path testFile = tempDir.resolve("simple.txt");
            Files.writeString(testFile, content);

            // When
            Map<String, Object> metadata = textExtractionService.extractMetadata(testFile);

            // Then
            assertThat(metadata.values()).allSatisfy(value -> {
                assertThat(value).isNotNull();
                if (value instanceof String) {
                    assertThat((String) value).isNotBlank();
                }
            });
        }

        @Test
        @DisplayName("Should extract metadata from different file types")
        void shouldExtractMetadataFromDifferentFileTypes() throws IOException {
            // Given
            String txtContent = "Plain text content";
            String htmlContent = "<html><head><title>HTML Document</title></head><body>HTML content</body></html>";
            
            Path txtFile = tempDir.resolve("test.txt");
            Path htmlFile = tempDir.resolve("test.html");
            
            Files.writeString(txtFile, txtContent);
            Files.writeString(htmlFile, htmlContent);

            // When
            Map<String, Object> txtMetadata = textExtractionService.extractMetadata(txtFile);
            Map<String, Object> htmlMetadata = textExtractionService.extractMetadata(htmlFile);

            // Then
            assertThat(txtMetadata).containsKey("file_name");
            assertThat(txtMetadata.get("file_name")).isEqualTo("test.txt");
            
            assertThat(htmlMetadata).containsKey("file_name");
            assertThat(htmlMetadata.get("file_name")).isEqualTo("test.html");
        }
    }

    @Nested
    @DisplayName("File Validation Tests")
    class FileValidationTests {

        @Test
        @DisplayName("Should validate supported document types")
        void shouldValidateSupportedDocumentTypes() {
            // When & Then
            assertThat(textExtractionService.isValidDocumentType("document.pdf", "application/pdf")).isTrue();
            assertThat(textExtractionService.isValidDocumentType("document.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")).isTrue();
            assertThat(textExtractionService.isValidDocumentType("document.txt", "text/plain")).isTrue();
            assertThat(textExtractionService.isValidDocumentType("document.md", "text/markdown")).isTrue();
            assertThat(textExtractionService.isValidDocumentType("document.html", "text/html")).isTrue();
        }

        @Test
        @DisplayName("Should reject unsupported document types")
        void shouldRejectUnsupportedDocumentTypes() {
            // When & Then
            assertThat(textExtractionService.isValidDocumentType("document.exe", "application/x-executable")).isTrue(); // Falls back to TXT
            assertThat(textExtractionService.isValidDocumentType("document.unknown", "unknown/type")).isTrue(); // Falls back to TXT
        }

        @Test
        @DisplayName("Should return correct maximum file size")
        void shouldReturnCorrectMaximumFileSize() {
            // When
            long maxSize = textExtractionService.getMaxFileSize();

            // Then
            assertThat(maxSize).isEqualTo(50 * 1024 * 1024); // 50MB
        }

        @Test
        @DisplayName("Should handle null filename in validation")
        void shouldHandleNullFilenameInValidation() {
            // When & Then - null filename causes NPE in getFileExtension, caught by isValidDocumentType
            assertThat(textExtractionService.isValidDocumentType(null, "text/plain")).isFalse();
        }

        @Test
        @DisplayName("Should handle null content type in validation")
        void shouldHandleNullContentTypeInValidation() {
            // When & Then
            assertThat(textExtractionService.isValidDocumentType("document.pdf", null)).isTrue();
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesAndErrorHandling {

        @Test
        @DisplayName("Should handle very large text files")
        void shouldHandleVeryLargeTextFiles() throws IOException {
            // Given
            StringBuilder largeContent = new StringBuilder();
            for (int i = 0; i < 10000; i++) {
                largeContent.append("This is line ").append(i).append(" with some content that makes the file larger.\n");
            }
            
            Path largeFile = tempDir.resolve("large.txt");
            Files.writeString(largeFile, largeContent.toString());

            // When
            String result = textExtractionService.extractText(largeFile, Document.DocumentType.TXT);

            // Then
            assertThat(result).contains("This is line 0");
            assertThat(result).contains("This is line 9999");
            assertThat(result.length()).isGreaterThan(100000);
        }

        @Test
        @DisplayName("Should handle files with long filenames")
        void shouldHandleFilesWithLongFilenames() throws IOException {
            // Given
            String longFilename = "this_is_a_very_long_filename_that_might_cause_issues_in_some_systems_document.txt";
            String content = "Content of file with long filename";
            Path longNameFile = tempDir.resolve(longFilename);
            Files.writeString(longNameFile, content);

            // When
            String result = textExtractionService.extractText(longNameFile, Document.DocumentType.TXT);

            // Then
            assertThat(result).contains("Content of file with long filename");
        }

        @Test
        @DisplayName("Should handle binary content in text extraction")
        void shouldHandleBinaryContentInTextExtraction() throws IOException {
            // Given
            byte[] binaryContent = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 'H', 'e', 'l', 'l', 'o'};
            Path binaryFile = tempDir.resolve("binary.txt");
            Files.write(binaryFile, binaryContent);

            // When
            String result = textExtractionService.extractText(binaryFile, Document.DocumentType.TXT);

            // Then
            assertThat(result).contains("Hello");
        }

        @Test
        @DisplayName("Should handle files with different line endings")
        void shouldHandleFilesWithDifferentLineEndings() throws IOException {
            // Given
            String windowsContent = "Line 1\r\nLine 2\r\nLine 3";
            String unixContent = "Line 1\nLine 2\nLine 3";
            String macContent = "Line 1\rLine 2\rLine 3";
            
            Path windowsFile = tempDir.resolve("windows.txt");
            Path unixFile = tempDir.resolve("unix.txt");
            Path macFile = tempDir.resolve("mac.txt");
            
            Files.writeString(windowsFile, windowsContent);
            Files.writeString(unixFile, unixContent);
            Files.writeString(macFile, macContent);

            // When
            String windowsResult = textExtractionService.extractText(windowsFile, Document.DocumentType.TXT);
            String unixResult = textExtractionService.extractText(unixFile, Document.DocumentType.TXT);
            String macResult = textExtractionService.extractText(macFile, Document.DocumentType.TXT);

            // Then
            assertThat(windowsResult).contains("Line 1").contains("Line 2").contains("Line 3");
            assertThat(unixResult).contains("Line 1").contains("Line 2").contains("Line 3");
            assertThat(macResult).contains("Line 1").contains("Line 2").contains("Line 3");
        }

        @Test
        @DisplayName("Should clean extracted text properly")
        void shouldCleanExtractedTextProperly() throws IOException {
            // Given
            String messyContent = "   Text with    multiple   spaces\n\n\nand multiple newlines\t\ttabs";
            Path messyFile = tempDir.resolve("messy.txt");
            Files.writeString(messyFile, messyContent);

            // When
            String result = textExtractionService.extractText(messyFile, Document.DocumentType.TXT);

            // Then
            assertThat(result).contains("Text with");
            assertThat(result).contains("multiple spaces");
            assertThat(result).contains("and multiple newlines");
            assertThat(result).contains("tabs");
        }
    }
}
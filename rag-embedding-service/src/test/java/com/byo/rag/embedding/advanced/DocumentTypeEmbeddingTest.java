package com.byo.rag.embedding.advanced;

import com.byo.rag.embedding.config.EmbeddingConfig.EmbeddingModelRegistry;
import com.byo.rag.embedding.dto.EmbeddingRequest;
import com.byo.rag.embedding.dto.EmbeddingResponse;
import com.byo.rag.embedding.service.EmbeddingCacheService;
import com.byo.rag.embedding.service.EmbeddingService;
import com.byo.rag.embedding.service.VectorStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.Embedding;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Advanced test suite for document type and format handling in embedding service.
 * 
 * Part of EMBEDDING-TEST-003: Embedding Service Advanced Scenarios
 * Tests embedding generation for different document types and formats.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EMBEDDING-TEST-003: Document Type and Format Tests")
class DocumentTypeEmbeddingTest {

    @Mock
    private EmbeddingModelRegistry modelRegistry;
    
    @Mock
    private EmbeddingCacheService cacheService;
    
    @Mock
    private VectorStorageService vectorStorageService;
    
    @Mock
    private EmbeddingModel embeddingModel;
    
    private EmbeddingService embeddingService;
    
    private static final UUID TEST_TENANT_ID = UUID.randomUUID();
    private static final UUID TEST_DOCUMENT_ID = UUID.randomUUID();
    private static final String TEST_MODEL = "text-embedding-3-small";
    
    @BeforeEach
    void setUp() {
        embeddingService = new EmbeddingService(modelRegistry, cacheService, vectorStorageService);
        
        // Default mocks
        when(modelRegistry.hasModel(TEST_MODEL)).thenReturn(true);
        when(modelRegistry.getClient(TEST_MODEL)).thenReturn(embeddingModel);
        when(cacheService.getCachedEmbedding(any(), anyString(), anyString())).thenReturn(null);
    }
    
    @Test
    @DisplayName("Should handle PDF text extraction content")
    void shouldHandlePdfTextContent() {
        // Arrange
        String pdfText = "This is extracted text from a PDF document with special formatting. " +
                        "It may contain page breaks, headers, footers, and other PDF-specific elements.";
        UUID chunkId = UUID.randomUUID();
        
        List<Float> mockEmbedding = List.of(0.1f, 0.2f, 0.3f, 0.4f, 0.5f);
        
        EmbeddingRequest request = new EmbeddingRequest(
            TEST_TENANT_ID, List.of(pdfText), TEST_MODEL, TEST_DOCUMENT_ID, List.of(chunkId));
        
        mockSuccessfulEmbeddingResponse(mockEmbedding);
        
        // Act
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        
        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.status());
        assertEquals(1, response.embeddings().size());
        
        EmbeddingResponse.EmbeddingResult result = response.embeddings().get(0);
        assertEquals(pdfText, result.text());
        assertEquals(mockEmbedding, result.embedding());
        
        verify(cacheService).cacheEmbedding(TEST_TENANT_ID, pdfText, TEST_MODEL, mockEmbedding);
    }
    
    @Test
    @DisplayName("Should handle Word document text content")
    void shouldHandleWordDocumentContent() {
        // Arrange
        String wordText = "This is text extracted from a Microsoft Word document.\n" +
                         "It contains multiple paragraphs, bullet points:\n" +
                         "• First point\n" +
                         "• Second point\n" +
                         "And various formatting elements that were preserved.";
        UUID chunkId = UUID.randomUUID();
        
        List<Float> mockEmbedding = List.of(0.2f, 0.3f, 0.4f, 0.5f, 0.6f);
        
        EmbeddingRequest request = new EmbeddingRequest(
            TEST_TENANT_ID, List.of(wordText), TEST_MODEL, TEST_DOCUMENT_ID, List.of(chunkId));
        
        mockSuccessfulEmbeddingResponse(mockEmbedding);
        
        // Act
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        
        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.status());
        assertEquals(1, response.embeddings().size());
        
        EmbeddingResponse.EmbeddingResult result = response.embeddings().get(0);
        assertEquals(wordText, result.text());
        assertEquals(mockEmbedding, result.embedding());
    }
    
    @Test
    @DisplayName("Should handle HTML content with tags stripped")
    void shouldHandleHtmlContent() {
        // Arrange - HTML content as it would appear after tag stripping
        String htmlText = "Web Page Title\n" +
                         "This is the main content of the web page.\n" +
                         "It includes navigation links, paragraphs, and other elements.\n" +
                         "Contact us at info@example.com for more information.";
        UUID chunkId = UUID.randomUUID();
        
        List<Float> mockEmbedding = List.of(0.3f, 0.4f, 0.5f, 0.6f, 0.7f);
        
        EmbeddingRequest request = new EmbeddingRequest(
            TEST_TENANT_ID, List.of(htmlText), TEST_MODEL, TEST_DOCUMENT_ID, List.of(chunkId));
        
        mockSuccessfulEmbeddingResponse(mockEmbedding);
        
        // Act
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        
        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.status());
        assertEquals(1, response.embeddings().size());
        
        EmbeddingResponse.EmbeddingResult result = response.embeddings().get(0);
        assertEquals(htmlText, result.text());
        assertEquals(mockEmbedding, result.embedding());
    }
    
    @Test
    @DisplayName("Should handle Markdown content")
    void shouldHandleMarkdownContent() {
        // Arrange
        String markdownText = "# Document Title\n\n" +
                             "This is a markdown document with **bold text** and *italic text*.\n\n" +
                             "## Section Header\n\n" +
                             "- List item 1\n" +
                             "- List item 2\n\n" +
                             "```java\n" +
                             "public class Example {\n" +
                             "    // Code block content\n" +
                             "}\n" +
                             "```";
        UUID chunkId = UUID.randomUUID();
        
        List<Float> mockEmbedding = List.of(0.4f, 0.5f, 0.6f, 0.7f, 0.8f);
        
        EmbeddingRequest request = new EmbeddingRequest(
            TEST_TENANT_ID, List.of(markdownText), TEST_MODEL, TEST_DOCUMENT_ID, List.of(chunkId));
        
        mockSuccessfulEmbeddingResponse(mockEmbedding);
        
        // Act
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        
        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.status());
        assertEquals(1, response.embeddings().size());
        
        EmbeddingResponse.EmbeddingResult result = response.embeddings().get(0);
        assertEquals(markdownText, result.text());
        assertEquals(mockEmbedding, result.embedding());
    }
    
    @ParameterizedTest
    @ValueSource(strings = {
        "Short text",
        "Medium length text content that spans multiple sentences and contains various types of information that would be typical in business documents.",
        "Very long text content that exceeds typical chunk sizes and contains extensive information about complex topics, detailed explanations, multiple paragraphs, technical specifications, business requirements, implementation details, and comprehensive documentation that would challenge the embedding model's ability to process and represent the semantic meaning effectively while maintaining consistency across similar content patterns and document structures."
    })
    @DisplayName("Should handle various text lengths consistently")
    void shouldHandleVariousTextLengths(String text) {
        // Arrange
        UUID chunkId = UUID.randomUUID();
        List<Float> mockEmbedding = generateMockEmbeddingBasedOnLength(text.length());
        
        EmbeddingRequest request = new EmbeddingRequest(
            TEST_TENANT_ID, List.of(text), TEST_MODEL, TEST_DOCUMENT_ID, List.of(chunkId));
        
        mockSuccessfulEmbeddingResponse(mockEmbedding);
        
        // Act
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        
        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.status());
        assertEquals(1, response.embeddings().size());
        
        EmbeddingResponse.EmbeddingResult result = response.embeddings().get(0);
        assertEquals(text, result.text());
        assertEquals(mockEmbedding, result.embedding());
        assertNotNull(result.embedding());
        assertFalse(result.embedding().isEmpty());
    }
    
    @Test
    @DisplayName("Should handle special characters and Unicode content")
    void shouldHandleSpecialCharactersAndUnicode() {
        // Arrange
        String specialText = "Special characters: @#$%^&*()_+-=[]{}|;':\",./<>?\n" +
                           "Unicode characters: café, naïve, résumé, coöperate\n" +
                           "Emojis: 😀 🎉 🚀 💡\n" +
                           "Non-Latin scripts: 你好世界 Здравствуй мир مرحبا بالعالم";
        UUID chunkId = UUID.randomUUID();
        
        List<Float> mockEmbedding = List.of(0.5f, 0.6f, 0.7f, 0.8f, 0.9f);
        
        EmbeddingRequest request = new EmbeddingRequest(
            TEST_TENANT_ID, List.of(specialText), TEST_MODEL, TEST_DOCUMENT_ID, List.of(chunkId));
        
        mockSuccessfulEmbeddingResponse(mockEmbedding);
        
        // Act
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        
        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.status());
        assertEquals(1, response.embeddings().size());
        
        EmbeddingResponse.EmbeddingResult result = response.embeddings().get(0);
        assertEquals(specialText, result.text());
        assertEquals(mockEmbedding, result.embedding());
    }
    
    @Test
    @DisplayName("Should handle mixed document format batch")
    void shouldHandleMixedDocumentFormatBatch() {
        // Arrange
        List<String> mixedTexts = List.of(
            "Plain text document content.",
            "# Markdown Header\nMarkdown content with **formatting**.",
            "HTML extracted text: Contact us at support@company.com",
            "PDF extracted content with page numbers and formatting artifacts."
        );
        List<UUID> chunkIds = List.of(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()
        );
        
        List<List<Float>> mockEmbeddings = List.of(
            List.of(0.1f, 0.2f, 0.3f),
            List.of(0.2f, 0.3f, 0.4f),
            List.of(0.3f, 0.4f, 0.5f),
            List.of(0.4f, 0.5f, 0.6f)
        );
        
        EmbeddingRequest request = new EmbeddingRequest(
            TEST_TENANT_ID, mixedTexts, TEST_MODEL, TEST_DOCUMENT_ID, chunkIds);
        
        mockBatchEmbeddingResponse(mockEmbeddings);
        
        // Act
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        
        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.status());
        assertEquals(4, response.embeddings().size());
        
        for (int i = 0; i < 4; i++) {
            EmbeddingResponse.EmbeddingResult result = response.embeddings().get(i);
            assertEquals(mixedTexts.get(i), result.text());
            assertEquals(mockEmbeddings.get(i), result.embedding());
            assertEquals(chunkIds.get(i), result.chunkId());
        }
    }
    
    @Test
    @DisplayName("Should handle empty and whitespace-only content")
    void shouldHandleEmptyAndWhitespaceContent() {
        // Arrange
        List<String> edgeCaseTexts = List.of(
            "",
            " ",
            "\n\n\n",
            "\t\t\t",
            "   \n  \t  \n   "
        );
        List<UUID> chunkIds = edgeCaseTexts.stream().map(text -> UUID.randomUUID()).collect(java.util.stream.Collectors.toList());
        
        // For empty/whitespace content, we expect the service to handle gracefully
        List<Float> emptyEmbedding = List.of(0.0f, 0.0f, 0.0f);
        
        EmbeddingRequest request = new EmbeddingRequest(
            TEST_TENANT_ID, edgeCaseTexts, TEST_MODEL, TEST_DOCUMENT_ID, chunkIds);
        
        mockBatchEmbeddingResponse(edgeCaseTexts.stream()
            .map(text -> emptyEmbedding)
            .collect(java.util.stream.Collectors.toList()));
        
        // Act
        EmbeddingResponse response = embeddingService.generateEmbeddings(request);
        
        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.status());
        assertEquals(5, response.embeddings().size());
        
        for (int i = 0; i < 5; i++) {
            EmbeddingResponse.EmbeddingResult result = response.embeddings().get(i);
            assertEquals(edgeCaseTexts.get(i), result.text());
            assertEquals(emptyEmbedding, result.embedding());
        }
    }
    
    private void mockSuccessfulEmbeddingResponse(List<Float> embedding) {
        Embedding mockSpringEmbedding = mock(Embedding.class);
        when(mockSpringEmbedding.getOutput()).thenReturn(embedding.stream().map(Float::doubleValue).toList());
        
        org.springframework.ai.embedding.EmbeddingResponse mockSpringResponse = 
            mock(org.springframework.ai.embedding.EmbeddingResponse.class);
        when(mockSpringResponse.getResults()).thenReturn(List.of(mockSpringEmbedding));
        
        when(embeddingModel.embedForResponse(any(List.class)))
            .thenReturn(mockSpringResponse);
    }
    
    private void mockBatchEmbeddingResponse(List<List<Float>> embeddings) {
        List<Embedding> mockSpringEmbeddings = embeddings.stream()
            .map(embedding -> {
                Embedding mockEmbedding = mock(Embedding.class);
                when(mockEmbedding.getOutput()).thenReturn(embedding.stream().map(Float::doubleValue).toList());
                return mockEmbedding;
            })
            .collect(java.util.stream.Collectors.toList());
        
        org.springframework.ai.embedding.EmbeddingResponse mockSpringResponse = 
            mock(org.springframework.ai.embedding.EmbeddingResponse.class);
        when(mockSpringResponse.getResults()).thenReturn(mockSpringEmbeddings);
        
        when(embeddingModel.embedForResponse(any(List.class)))
            .thenReturn(mockSpringResponse);
    }
    
    private List<Float> generateMockEmbeddingBasedOnLength(int textLength) {
        // Generate embedding values based on text length for realistic testing
        int embeddingSize = 5; // Standard test embedding size
        return java.util.stream.IntStream.range(0, embeddingSize)
            .mapToObj(i -> (float) ((textLength + i * 17) % 100) / 100.0f)
            .collect(java.util.stream.Collectors.toList());
    }
}
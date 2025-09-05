package com.byo.rag.core.service;

import com.byo.rag.shared.dto.DocumentChunkDto;
import com.byo.rag.core.dto.RagQueryResponse.SourceDocument;
import com.byo.rag.core.dto.RagQueryRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ContextAssemblyService.
 */
@ExtendWith(MockitoExtension.class)
class ContextAssemblyServiceTest {

    private ContextAssemblyService contextAssemblyService;

    @BeforeEach
    void setUp() {
        contextAssemblyService = new ContextAssemblyService();
        
        // Set properties using reflection
        ReflectionTestUtils.setField(contextAssemblyService, "maxContextLength", 4000);
        ReflectionTestUtils.setField(contextAssemblyService, "chunkSeparator", "\\n\\n---\\n\\n");
        ReflectionTestUtils.setField(contextAssemblyService, "includeMetadata", true);
    }

    @Test
    void assembleContext_ValidChunks_ReturnsContext() {
        List<SourceDocument> documents = createMockSourceDocuments();
        RagQueryRequest request = RagQueryRequest.simple(UUID.randomUUID(), "What is Spring AI?");

        String context = contextAssemblyService.assembleContext(documents, request);

        assertNotNull(context);
        assertFalse(context.isEmpty());
        assertTrue(context.contains("Spring AI"));
        assertTrue(context.contains("framework"));
    }

    @Test
    void assembleContext_EmptyChunks_ReturnsEmptyContext() {
        List<SourceDocument> documents = List.of();
        RagQueryRequest request = RagQueryRequest.simple(UUID.randomUUID(), "What is Spring AI?");

        String context = contextAssemblyService.assembleContext(documents, request);

        assertEquals("", context);
    }

    @Test
    void assembleContext_NullChunks_ReturnsEmptyContext() {
        RagQueryRequest request = RagQueryRequest.simple(UUID.randomUUID(), "What is Spring AI?");

        String context = contextAssemblyService.assembleContext(null, request);

        assertEquals("", context);
    }

    @Test
    void assembleContext_WithMetadata_IncludesMetadata() {
        List<SourceDocument> documents = createMockSourceDocuments();
        RagQueryRequest request = RagQueryRequest.simple(UUID.randomUUID(), "What is Spring AI?");
        ContextAssemblyService.ContextConfig config = new ContextAssemblyService.ContextConfig(true, true, 4000, "\n\n---\n\n");

        String context = contextAssemblyService.assembleContext(documents, request, config);

        assertNotNull(context);
        // Should include source information when metadata is enabled
        assertTrue(context.contains("Source:") || context.contains("spring-ai-guide.pdf"));
    }

    @Test
    void assembleContext_WithoutMetadata_OnlyContent() {
        ReflectionTestUtils.setField(contextAssemblyService, "includeMetadata", false);
        
        List<SourceDocument> documents = createMockSourceDocuments();
        RagQueryRequest request = RagQueryRequest.simple(UUID.randomUUID(), "What is Spring AI?");
        ContextAssemblyService.ContextConfig config = new ContextAssemblyService.ContextConfig(false, false, 4000, "\n\n---\n\n");

        String context = contextAssemblyService.assembleContext(documents, request, config);

        assertNotNull(context);
        assertFalse(context.isEmpty());
        // Should not include metadata when disabled
        assertFalse(context.contains("Source:"));
    }

    @Test
    void assembleContext_MaxLengthLimit_TruncatesContext() {
        // Set a very small max length
        ReflectionTestUtils.setField(contextAssemblyService, "maxContextLength", 100);
        
        List<SourceDocument> documents = createLongMockSourceDocuments();
        RagQueryRequest request = RagQueryRequest.simple(UUID.randomUUID(), "What is Spring AI?");
        ContextAssemblyService.ContextConfig config = new ContextAssemblyService.ContextConfig(true, true, 100, "\n\n---\n\n");

        String context = contextAssemblyService.assembleContext(documents, request, config);

        assertNotNull(context);
        assertTrue(context.length() <= 100);
    }

    @Test
    void assembleContext_MultipleChunks_MaintainsOrder() {
        List<SourceDocument> documents = createOrderedMockSourceDocuments();
        RagQueryRequest request = RagQueryRequest.simple(UUID.randomUUID(), "Explain the process");

        String context = contextAssemblyService.assembleContext(documents, request);

        assertNotNull(context);
        // Check that chunks appear in the expected order
        int firstPos = context.indexOf("First chunk");
        int secondPos = context.indexOf("Second chunk");
        int thirdPos = context.indexOf("Third chunk");
        
        assertTrue(firstPos < secondPos);
        assertTrue(secondPos < thirdPos);
    }

    @Test
    void assembleContext_ChunksWithScores_OrdersByRelevance() {
        List<SourceDocument> documents = createMockSourceDocumentsWithDifferentScores();
        RagQueryRequest request = RagQueryRequest.simple(UUID.randomUUID(), "What is important?");

        String context = contextAssemblyService.assembleContext(documents, request);

        assertNotNull(context);
        assertFalse(context.isEmpty());
        // Higher relevance chunks should appear first
        int highPos = context.indexOf("High relevance");
        int mediumPos = context.indexOf("Medium relevance");
        int lowPos = context.indexOf("Low relevance");
        
        assertTrue(highPos < mediumPos);
        assertTrue(mediumPos < lowPos);
    }

    @Test
    void assembleContext_DuplicateContent_RemovesDuplicates() {
        List<SourceDocument> documents = createMockSourceDocumentsWithDuplicates();
        RagQueryRequest request = RagQueryRequest.simple(UUID.randomUUID(), "Remove duplicates");

        String context = contextAssemblyService.assembleContext(documents, request);

        assertNotNull(context);
        // Should only contain unique content once
        int firstOccurrence = context.indexOf("Duplicate content");
        int secondOccurrence = context.indexOf("Duplicate content", firstOccurrence + 1);
        
        assertEquals(-1, secondOccurrence);
    }

    private List<SourceDocument> createMockSourceDocuments() {
        SourceDocument doc1 = SourceDocument.withMetadata(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "spring-ai-guide.pdf",
            "Spring AI is a comprehensive framework for building AI-powered applications with Java.",
            0.95,
            Map.of("category", "framework"),
            "pdf",
            java.time.Instant.now()
        );
        
        SourceDocument doc2 = SourceDocument.withMetadata(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "spring-ai-guide.pdf",
            "It provides integration with various LLM providers like OpenAI, Azure OpenAI, and Ollama.",
            0.87,
            Map.of("category", "integration"),
            "pdf",
            java.time.Instant.now()
        );
        
        return List.of(doc1, doc2);
    }

    private List<SourceDocument> createLongMockSourceDocuments() {
        String longContent = "This is a very long document chunk that contains a lot of information about Spring AI and its capabilities. ".repeat(10);
        
        SourceDocument doc = SourceDocument.withMetadata(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "long-document.pdf",
            longContent,
            0.90,
            Map.of(),
            "pdf",
            java.time.Instant.now()
        );
        
        return List.of(doc);
    }

    private List<SourceDocument> createOrderedMockSourceDocuments() {
        SourceDocument doc1 = SourceDocument.of(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "ordered-doc.pdf",
            "First chunk with initial information",
            0.90
        );
        
        SourceDocument doc2 = SourceDocument.of(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "ordered-doc.pdf",
            "Second chunk with more details",
            0.85
        );
        
        SourceDocument doc3 = SourceDocument.of(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "ordered-doc.pdf",
            "Third chunk with final information",
            0.80
        );
        
        return List.of(doc1, doc2, doc3);
    }

    private List<SourceDocument> createMockSourceDocumentsWithDifferentScores() {
        SourceDocument highScore = SourceDocument.of(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "relevance-doc.pdf",
            "High relevance content that matches perfectly",
            0.98
        );
        
        SourceDocument mediumScore = SourceDocument.of(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "relevance-doc.pdf",
            "Medium relevance content with some match",
            0.75
        );
        
        SourceDocument lowScore = SourceDocument.of(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "relevance-doc.pdf",
            "Low relevance content with minimal match",
            0.45
        );
        
        return List.of(lowScore, highScore, mediumScore); // Intentionally out of order
    }

    private List<SourceDocument> createMockSourceDocumentsWithDuplicates() {
        SourceDocument doc1 = SourceDocument.of(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "duplicate-doc.pdf",
            "Duplicate content that appears multiple times",
            0.90
        );
        
        SourceDocument doc2 = SourceDocument.of(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "duplicate-doc.pdf",
            "Duplicate content that appears multiple times",
            0.85
        );
        
        SourceDocument doc3 = SourceDocument.of(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "duplicate-doc.pdf",
            "Unique content that should be included",
            0.80
        );
        
        return List.of(doc1, doc2, doc3);
    }
}
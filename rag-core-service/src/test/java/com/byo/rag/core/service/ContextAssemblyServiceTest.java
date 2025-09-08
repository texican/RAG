package com.byo.rag.core.service;

import com.byo.rag.core.dto.RagQueryResponse.SourceDocument;
import com.byo.rag.core.dto.RagQueryRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
// Removed ReflectionTestUtils - violates testing best practices

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ContextAssemblyService following enterprise testing best practices.
 * 
 * <p>This test suite validates context assembly functionality including document formatting,
 * token limits, metadata inclusion, and relevance filtering. All tests follow the established
 * testing standards to prevent bugs and ensure reliable behavior.</p>
 * 
 * <p>Testing approach:</p>
 * <ul>
 *   <li><strong>Public API Testing:</strong> Tests use ContextConfig for configuration override</li>
 *   <li><strong>Descriptive Names:</strong> Each test clearly describes expected behavior</li>
 *   <li><strong>Realistic Data:</strong> Test data mirrors production usage patterns</li>
 *   <li><strong>Clear Assertions:</strong> All assertions include descriptive failure messages</li>
 * </ul>
 * 
 * @see ContextAssemblyService
 * @see com.byo.rag.shared.TESTING_BEST_PRACTICES
 */
@ExtendWith(MockitoExtension.class)
class ContextAssemblyServiceTest {

    private ContextAssemblyService contextAssemblyService;

    @BeforeEach
    void setUp() {
        contextAssemblyService = new ContextAssemblyService();
        // No longer using ReflectionTestUtils - all tests now use public API with ContextConfig
    }

    /**
     * Validates that the service can assemble context from valid documents.
     * 
     * Tests the basic happy path where:
     * 1. Valid documents with good relevance scores (>= 0.7) are provided
     * 2. Default configuration is used (4000 tokens, metadata enabled)
     * 3. Context is assembled containing expected content from source documents
     */
    @Test
    @DisplayName("Should assemble context from valid documents with expected content")
    void assembleContext_ValidChunks_ReturnsContext() {
        List<SourceDocument> documents = createMockSourceDocuments();
        RagQueryRequest request = RagQueryRequest.simple(UUID.randomUUID(), "What is Spring AI?");
        ContextAssemblyService.ContextConfig config = ContextAssemblyService.ContextConfig.defaultConfig();

        String context = contextAssemblyService.assembleContext(documents, request, config);

        assertThat(context)
            .as("Context should not be null or empty")
            .isNotNull()
            .isNotEmpty();
        assertThat(context)
            .as("Context should contain key content from source documents")
            .contains("Spring AI", "framework");
    }

    /**
     * Validates that the service handles empty document lists gracefully.
     * 
     * Edge case testing where:
     * 1. An empty list of documents is provided
     * 2. Service should return empty string rather than null or throwing exception
     * 3. This represents a valid scenario where no relevant documents were found
     */
    @Test
    @DisplayName("Should return empty context when no documents are provided")
    void assembleContext_EmptyChunks_ReturnsEmptyContext() {
        List<SourceDocument> documents = List.of();
        RagQueryRequest request = RagQueryRequest.simple(UUID.randomUUID(), "What is Spring AI?");
        ContextAssemblyService.ContextConfig config = ContextAssemblyService.ContextConfig.defaultConfig();

        String context = contextAssemblyService.assembleContext(documents, request, config);

        assertThat(context)
            .as("Empty document list should result in empty context")
            .isEmpty();
    }

    /**
     * Validates that the service handles null document input defensively.
     * 
     * Edge case testing where:
     * 1. Null document list is provided (defensive programming)
     * 2. Service should return empty string rather than throwing NPE
     * 3. This prevents service failures from upstream issues
     */
    @Test
    @DisplayName("Should return empty context when documents list is null")
    void assembleContext_NullChunks_ReturnsEmptyContext() {
        RagQueryRequest request = RagQueryRequest.simple(UUID.randomUUID(), "What is Spring AI?");
        ContextAssemblyService.ContextConfig config = ContextAssemblyService.ContextConfig.defaultConfig();

        String context = contextAssemblyService.assembleContext(null, request, config);

        assertThat(context)
            .as("Null document list should result in empty context")
            .isEmpty();
    }

    /**
     * Validates that the service includes document metadata when configured.
     * 
     * Configuration testing where:
     * 1. ContextConfig is used with includeMetadata=true
     * 2. Service should include document titles, types, and relevance scores
     * 3. Tests the public API configuration override behavior
     */
    @Test
    @DisplayName("Should include document metadata when metadata is enabled via config")
    void assembleContext_WithMetadata_IncludesMetadata() {
        List<SourceDocument> documents = createMockSourceDocuments();
        RagQueryRequest request = RagQueryRequest.simple(UUID.randomUUID(), "What is Spring AI?");
        ContextAssemblyService.ContextConfig config = new ContextAssemblyService.ContextConfig(4000, 0.7, true, "\\n\\n---\\n\\n");

        String context = contextAssemblyService.assembleContext(documents, request, config);

        assertThat(context)
            .as("Context should not be null when metadata is enabled")
            .isNotNull();
        assertThat(context)
            .as("Context should include metadata headers when metadata is enabled")
            .satisfiesAnyOf(
                ctx -> assertThat(ctx).contains("Document:"),
                ctx -> assertThat(ctx).contains("spring-ai-guide.pdf")
            );
    }

    /**
     * Validates that the service excludes metadata when configured to do so.
     * 
     * Configuration testing where:
     * 1. ContextConfig is used with includeMetadata=false
     * 2. Service should only include document content, no headers or metadata
     * 3. Tests API contract for metadata control (FIXED: removed reflection usage)
     */
    @Test
    @DisplayName("Should exclude document metadata when metadata is disabled via config")
    void assembleContext_WithoutMetadata_OnlyContent() {
        List<SourceDocument> documents = createMockSourceDocuments();
        RagQueryRequest request = RagQueryRequest.simple(UUID.randomUUID(), "What is Spring AI?");
        ContextAssemblyService.ContextConfig config = new ContextAssemblyService.ContextConfig(4000, 0.7, false, "\\n\\n---\\n\\n");

        String context = contextAssemblyService.assembleContext(documents, request, config);

        assertThat(context)
            .as("Context should not be null or empty even without metadata")
            .isNotNull()
            .isNotEmpty();
        assertThat(context)
            .as("Context should not include metadata headers when metadata is disabled")
            .doesNotContain("Document:");
    }

    @Test
    @DisplayName("Should truncate context when content exceeds token limit")
    void assembleContext_MaxLengthLimit_TruncatesContext() {
        // Set a very small max token limit
        int maxTokens = 100;
        
        List<SourceDocument> documents = createLongMockSourceDocuments();
        RagQueryRequest request = RagQueryRequest.simple(UUID.randomUUID(), "What is Spring AI?");
        ContextAssemblyService.ContextConfig config = new ContextAssemblyService.ContextConfig(maxTokens, 0.7, true, "\\n\\n---\\n\\n");

        String context = contextAssemblyService.assembleContext(documents, request, config);

        assertThat(context)
            .as("Context should not be null when processing documents")
            .isNotNull();
            
        // Should respect token limits - estimate tokens used
        int estimatedTokens = context.length() / 4; // Service uses 4 chars per token estimation
        assertThat(estimatedTokens)
            .as("Context should respect token limit of %d tokens, but got %d tokens", maxTokens, estimatedTokens)
            .isLessThanOrEqualTo(maxTokens);
    }

    /**
     * Validates that the service maintains document order in assembled context.
     * 
     * Business logic testing where:
     * 1. Multiple documents with different relevance scores are provided
     * 2. Service should maintain the original document order in the output
     * 3. This ensures predictable context structure for LLM processing
     */
    @Test
    @DisplayName("Should maintain document order when assembling multiple chunks")
    void assembleContext_MultipleChunks_MaintainsOrder() {
        List<SourceDocument> documents = createOrderedMockSourceDocuments();
        RagQueryRequest request = RagQueryRequest.simple(UUID.randomUUID(), "Explain the process");
        ContextAssemblyService.ContextConfig config = ContextAssemblyService.ContextConfig.defaultConfig();

        String context = contextAssemblyService.assembleContext(documents, request, config);

        assertThat(context)
            .as("Context should not be null for multiple documents")
            .isNotNull();
            
        // Check that chunks appear in the expected order
        int firstPos = context.indexOf("First chunk");
        int secondPos = context.indexOf("Second chunk");
        int thirdPos = context.indexOf("Third chunk");
        
        assertThat(firstPos)
            .as("First chunk should appear before second chunk")
            .isLessThan(secondPos);
        assertThat(secondPos)
            .as("Second chunk should appear before third chunk")
            .isLessThan(thirdPos);
    }

    /**
     * Validates that the service filters documents based on relevance threshold.
     * 
     * Business logic testing where:
     * 1. Documents with varying relevance scores (0.98, 0.75, 0.45) are provided
     * 2. Default relevance threshold is 0.7
     * 3. Service should include high (0.98) and medium (0.75) but exclude low (0.45)
     * 4. This ensures only relevant content reaches the LLM
     */
    @Test
    @DisplayName("Should filter out documents below relevance threshold")
    void assembleContext_LowRelevanceFiltered_ExcludesIrrelevantChunks() {
        List<SourceDocument> documents = createMockSourceDocumentsWithDifferentScores();
        RagQueryRequest request = RagQueryRequest.simple(UUID.randomUUID(), "What is important?");
        ContextAssemblyService.ContextConfig config = ContextAssemblyService.ContextConfig.defaultConfig();

        String context = contextAssemblyService.assembleContext(documents, request, config);

        assertThat(context)
            .as("Context should not be null when filtering by relevance")
            .isNotNull();
            
        // High and medium relevance should be included (>= 0.7)
        assertThat(context)
            .as("High relevance content (score 0.98) should be included")
            .contains("High relevance");
        assertThat(context)
            .as("Medium relevance content (score 0.75) should be included")
            .contains("Medium relevance");
            
        // Low relevance should be excluded (< 0.7)
        assertThat(context)
            .as("Low relevance content (score 0.45) should be excluded")
            .doesNotContain("Low relevance");
    }

    /**
     * Validates that the context optimization feature removes excessive whitespace.
     * 
     * Optimization testing where:
     * 1. Raw context with multiple spaces and excessive newlines is provided
     * 2. optimizeContext method should normalize whitespace
     * 3. Result should be shorter and cleaner while preserving content
     */
    @Test
    @DisplayName("Should remove excessive whitespace when optimizing context")
    void assembleContext_OptimizeContext_RemovesExcessiveWhitespace() {
        String rawContext = "This  has   excessive    whitespace\n\n\n\nand   multiple   spaces";
        RagQueryRequest request = RagQueryRequest.simple(UUID.randomUUID(), "test query");

        String optimizedContext = contextAssemblyService.optimizeContext(rawContext, request);

        assertThat(optimizedContext)
            .as("Optimized context should not be null")
            .isNotNull();
        assertThat(optimizedContext)
            .as("Optimized context should not contain excessive whitespace")
            .doesNotContain("   "); // No triple spaces
        assertThat(optimizedContext.length())
            .as("Optimized context should be shorter than original")
            .isLessThan(rawContext.length());
    }

    /**
     * Validates that context statistics are calculated correctly for valid documents.
     * 
     * Statistics testing where:
     * 1. Two valid documents with scores 0.95 and 0.87 are provided
     * 2. Both documents meet the default relevance threshold of 0.7
     * 3. Service should calculate accurate document counts, token estimates, and averages
     * 4. Uses explicit ContextConfig to ensure predictable behavior
     */
    @Test
    @DisplayName("Should calculate correct statistics for valid documents with configuration")
    void getContextStats_ValidDocuments_ReturnsCorrectStats() {
        List<SourceDocument> documents = createMockSourceDocuments();
        String assembledContext = "Test context for statistics";
        ContextAssemblyService.ContextConfig config = ContextAssemblyService.ContextConfig.defaultConfig();

        ContextAssemblyService.ContextStats stats = contextAssemblyService.getContextStats(documents, assembledContext, config);

        assertThat(stats)
            .as("Context stats should not be null")
            .isNotNull();
        assertThat(stats.totalDocuments())
            .as("Total documents should match input count")
            .isEqualTo(2);
        assertThat(stats.relevantDocuments())
            .as("Both documents should meet default threshold 0.7 (scores 0.95 and 0.87)")
            .isEqualTo(2);
        assertThat(stats.estimatedTokens())
            .as("Estimated tokens should be positive for non-empty context")
            .isPositive();
        assertThat(stats.averageRelevanceScore())
            .as("Average relevance should be > 0.8 for scores 0.95 and 0.87")
            .isGreaterThan(0.8);
        assertThat(stats.maxTokenLimit())
            .as("Max token limit should match config default (4000)")
            .isEqualTo(4000);
    }

    /**
     * Validates that context statistics handle empty document lists correctly.
     * 
     * Edge case testing where:
     * 1. Empty document list and empty context are provided
     * 2. Service should return zero values for all statistics
     * 3. This prevents division by zero and other edge case issues
     */
    @Test
    @DisplayName("Should return zero statistics for empty document list")
    void getContextStats_EmptyDocuments_ReturnsZeroStats() {
        List<SourceDocument> documents = List.of();
        String assembledContext = "";

        ContextAssemblyService.ContextStats stats = contextAssemblyService.getContextStats(documents, assembledContext);

        assertThat(stats)
            .as("Context stats should not be null even for empty input")
            .isNotNull();
        assertThat(stats.totalDocuments())
            .as("Total documents should be zero for empty list")
            .isZero();
        assertThat(stats.relevantDocuments())
            .as("Relevant documents should be zero for empty list")
            .isZero();
        assertThat(stats.estimatedTokens())
            .as("Estimated tokens should be zero for empty context")
            .isZero();
        assertThat(stats.averageRelevanceScore())
            .as("Average relevance should be zero for empty list")
            .isZero();
    }

    /**
     * Validates that the service respects various token limits consistently.
     * 
     * Boundary condition testing using parameterized approach:
     * 1. Tests multiple token limit values from very small (10) to large (5000)
     * 2. Each test uses the same long document content
     * 3. Service should consistently respect the token limit regardless of size
     * 4. Prevents regression of the token limiting bug that was fixed
     */
    @ParameterizedTest
    @ValueSource(ints = {10, 50, 100, 500, 1000, 4000, 5000})
    @DisplayName("Should respect various token limits consistently")
    void assembleContext_VariousTokenLimits_AlwaysRespectsLimit(int tokenLimit) {
        List<SourceDocument> documents = createLongMockSourceDocuments();
        RagQueryRequest request = RagQueryRequest.simple(UUID.randomUUID(), "What is Spring AI?");
        ContextAssemblyService.ContextConfig config = new ContextAssemblyService.ContextConfig(
            tokenLimit, 0.7, true, "\\n\\n---\\n\\n");

        String context = contextAssemblyService.assembleContext(documents, request, config);

        assertThat(context)
            .as("Context should not be null for token limit %d", tokenLimit)
            .isNotNull();
            
        int estimatedTokens = context.length() / 4; // Service uses 4 chars per token
        assertThat(estimatedTokens)
            .as("Context should respect token limit of %d tokens, but got %d tokens", tokenLimit, estimatedTokens)
            .isLessThanOrEqualTo(tokenLimit);
            
        // For reasonable token limits, context should not be empty
        if (tokenLimit >= 10) {
            assertThat(context)
                .as("Context should not be empty for reasonable token limit %d", tokenLimit)
                .isNotEmpty();
        }
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
        
        return List.of(highScore, mediumScore, lowScore);
    }
}
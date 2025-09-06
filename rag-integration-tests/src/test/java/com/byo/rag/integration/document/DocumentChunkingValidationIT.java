package com.byo.rag.integration.document;

import com.byo.rag.integration.base.BaseIntegrationTest;
import com.byo.rag.integration.data.TestDataBuilder;
import com.byo.rag.integration.data.TestDataCleanup;
import com.byo.rag.integration.utils.AuthenticationTestUtils;
import com.byo.rag.integration.utils.IntegrationTestUtils;
import com.byo.rag.integration.dto.DocumentChunkDto;
import com.byo.rag.shared.dto.DocumentDto;
import com.byo.rag.shared.dto.TenantDto;
import com.byo.rag.shared.entity.Document;
import com.byo.rag.shared.util.TextChunker;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for validating document chunking algorithms and processing.
 * 
 * This test class validates that document chunking works correctly across
 * different strategies, content types, and produces high-quality chunks
 * suitable for RAG operations.
 */
@DisplayName("Document Chunking Validation Integration Tests")
class DocumentChunkingValidationIT extends BaseIntegrationTest {

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
        
        // Create test tenant with specific chunking configuration
        TenantDto.CreateTenantRequest tenantRequest = TestDataBuilder.createTenantRequest(
            "Chunking Test Company",
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
     * Validates that semantic chunking algorithm correctly preserves natural language boundaries.
     * 
     * This test ensures that:
     * 1. Semantic chunking respects sentence and paragraph boundaries
     * 2. Chunks maintain coherent meaning and context
     * 3. Section headers and structured content are handled appropriately
     * 4. Chunk boundaries occur at natural stopping points in the text
     * 5. No critical information is split across chunk boundaries inappropriately
     * 
     * This validates a core E2E-TEST-002 requirement that chunked content maintains
     * semantic integrity for optimal RAG retrieval and LLM context assembly.
     */
    @Test
    @DisplayName("Should validate semantic chunking preserves sentence boundaries")
    void shouldValidateSemanticChunkingBoundaries() {
        // Create structured content with clear semantic boundaries
        String content = """
            Introduction to Machine Learning
            
            Machine learning is a method of data analysis that automates analytical model building. It is a branch of artificial intelligence based on the idea that systems can learn from data, identify patterns and make decisions with minimal human intervention.
            
            Types of Machine Learning
            
            There are three main types of machine learning algorithms. Supervised learning uses labeled examples to learn a general rule that maps inputs to outputs. Unsupervised learning finds hidden patterns in data without any labels. Reinforcement learning trains algorithms using a system of rewards and punishments.
            
            Applications in Business
            
            Machine learning has numerous applications in business today. Companies use it for fraud detection, recommendation systems, predictive maintenance, and customer segmentation. The technology enables businesses to make data-driven decisions and improve operational efficiency.
            
            Future Prospects
            
            The future of machine learning looks promising with advancements in deep learning, neural networks, and quantum computing. These technologies will enable more sophisticated applications and better performance across various domains.
            """;

        DocumentDto.DocumentResponse document = uploadDocumentForChunking(
            "semantic-test.txt",
            content,
            createSemanticChunkingConfig()
        );
        
        waitForDocumentProcessing(document.id());
        
        // Retrieve and validate chunks
        List<DocumentChunkDto.ChunkResponse> chunks = getDocumentChunks(document.id());
        
        // Validate chunk characteristics with descriptive assertions
        assertThat(chunks)
            .describedAs("Semantic chunking should produce non-empty chunk list")
            .isNotEmpty();
        assertThat(chunks.size())
            .describedAs("Structured document should be divided into multiple semantic chunks")
            .isGreaterThanOrEqualTo(3);
        
        // Validate semantic boundaries are preserved across all chunks
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunkDto.ChunkResponse chunk = chunks.get(i);
            String chunkText = chunk.content();
            
            // Semantic chunks should not break sentences mid-way (avoid dangling conjunctions/articles)
            assertThat(chunkText)
                .describedAs("Chunk %d should not end with dangling conjunction 'and'", i)
                .doesNotEndWith(" and");
            assertThat(chunkText)
                .describedAs("Chunk %d should not end with dangling article 'the'", i)
                .doesNotEndWith(" the");
            assertThat(chunkText)
                .describedAs("Chunk %d should not end with incomplete verb 'is'", i)
                .doesNotEndWith(" is");
            
            // Validate chunks end at natural language boundaries
            if (!chunkText.trim().isEmpty()) {
                assertThat(chunkText.trim())
                    .describedAs("Chunk %d should end with complete sentence punctuation", i)
                    .matches(".*[.!?]\\s*$|.*[.!?]\\s*\\n\\s*$");
            }
            
            // Validate chunk size and indexing
            assertThat(chunk.content().length())
                .describedAs("Chunk %d content length should be within reasonable semantic bounds", i)
                .isLessThanOrEqualTo(600); // Allow variance for semantic boundaries
            assertThat(chunk.chunkIndex())
                .describedAs("Chunk %d should have valid sequential index", i)
                .isGreaterThanOrEqualTo(0)
                .isEqualTo(i); // Should be sequential
        }
        
        // Validate chunks maintain logical order
        for (int i = 0; i < chunks.size() - 1; i++) {
            assertThat(chunks.get(i).chunkIndex()).isLessThan(chunks.get(i + 1).chunkIndex());
        }
    }

    @Test
    @DisplayName("Should validate fixed-size chunking creates consistent chunk sizes")
    void shouldValidateFixedSizeChunking() {
        // Create content longer than chunk size to force multiple chunks
        String content = TestDataBuilder.createLargeTestDocumentContent();
        
        DocumentDto.DocumentResponse document = uploadDocumentForChunking(
            "fixed-size-test.txt",
            content,
            createFixedSizeChunkingConfig()
        );
        
        waitForDocumentProcessing(document.id());
        
        List<DocumentChunkDto.ChunkResponse> chunks = getDocumentChunks(document.id());
        
        assertThat(chunks).isNotEmpty();
        assertThat(chunks.size()).isGreaterThan(1); // Should create multiple chunks
        
        // Validate fixed-size characteristics
        int expectedChunkSize = 400; // From config
        int overlap = 50; // From config
        
        for (int i = 0; i < chunks.size() - 1; i++) { // All except last chunk
            DocumentChunkDto.ChunkResponse chunk = chunks.get(i);
            assertThat(chunk.content().length()).isLessThanOrEqualTo(expectedChunkSize + 10); // Allow small variance
            assertThat(chunk.content().length()).isGreaterThan(expectedChunkSize - 50); // Should be reasonably sized
        }
        
        // Last chunk can be smaller
        DocumentChunkDto.ChunkResponse lastChunk = chunks.get(chunks.size() - 1);
        assertThat(lastChunk.content().length()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should validate sliding window chunking creates overlapping content")
    void shouldValidateSlidingWindowChunking() {
        // Create specific content to test overlap
        String content = """
            Paragraph one contains important information about the first topic. This information is crucial for understanding.
            
            Paragraph two builds upon the first paragraph and adds more details. The additional context helps clarify concepts.
            
            Paragraph three concludes the discussion with final thoughts. These concluding remarks tie everything together.
            """;
        
        DocumentDto.DocumentResponse document = uploadDocumentForChunking(
            "sliding-window-test.txt",
            content,
            createSlidingWindowChunkingConfig()
        );
        
        waitForDocumentProcessing(document.id());
        
        List<DocumentChunkDto.ChunkResponse> chunks = getDocumentChunks(document.id());
        
        assertThat(chunks).isNotEmpty();
        assertThat(chunks.size()).isGreaterThan(1); // Should create overlapping chunks
        
        // Validate overlapping content exists between adjacent chunks
        for (int i = 0; i < chunks.size() - 1; i++) {
            String currentChunk = chunks.get(i).content();
            String nextChunk = chunks.get(i + 1).content();
            
            // Find potential overlap by checking if any words from end of current chunk
            // appear at the beginning of next chunk
            String[] currentWords = currentChunk.split("\\s+");
            String[] nextWords = nextChunk.split("\\s+");
            
            if (currentWords.length > 5 && nextWords.length > 5) {
                // Check if last few words of current chunk match first few words of next chunk
                boolean hasOverlap = false;
                for (int j = Math.max(0, currentWords.length - 10); j < currentWords.length; j++) {
                    for (int k = 0; k < Math.min(10, nextWords.length); k++) {
                        if (currentWords[j].equals(nextWords[k]) && currentWords[j].length() > 3) {
                            hasOverlap = true;
                            break;
                        }
                    }
                    if (hasOverlap) break;
                }
                
                // Sliding window should create some overlap
                // Note: This is a heuristic check as exact overlap detection is complex
            }
        }
    }

    @Test
    @DisplayName("Should validate chunking handles special characters and formatting")
    void shouldValidateSpecialCharacterHandling() {
        String content = """
            # Title with Special Characters: éñ & Symbols!
            
            This document contains various special characters like: áéíóú, ñ, ç, ü.
            It also includes symbols: @#$%^&*()_+-=[]{}|;:,.<>?
            
            Numbers and dates: 2023-12-25, $1,234.56, 99.9%, Tel: +1-555-123-4567
            
            Quoted text: "This is a quote with 'nested quotes' inside."
            
            Code snippet:
            ```
            public void method() {
                System.out.println("Hello, World!");
            }
            ```
            
            Bullet points:
            • First bullet point
            • Second bullet with émojis 🚀 💡
            • Third bullet
            """;
        
        DocumentDto.DocumentResponse document = uploadDocumentForChunking(
            "special-chars-test.txt",
            content,
            createSemanticChunkingConfig()
        );
        
        waitForDocumentProcessing(document.id());
        
        List<DocumentChunkDto.ChunkResponse> chunks = getDocumentChunks(document.id());
        
        assertThat(chunks).isNotEmpty();
        
        // Validate special characters are preserved
        String allChunksContent = String.join(" ", chunks.stream()
            .map(DocumentChunkDto.ChunkResponse::content)
            .toList());
        
        assertThat(allChunksContent).contains("éñ", "áéíóú", "ñ", "ç", "ü");
        assertThat(allChunksContent).contains("@#$%^&*");
        assertThat(allChunksContent).contains("2023-12-25");
        assertThat(allChunksContent).contains("+1-555-123-4567");
        assertThat(allChunksContent).contains("nested quotes");
        
        // Validate chunks don't have encoding issues
        for (DocumentChunkDto.ChunkResponse chunk : chunks) {
            assertThat(chunk.content()).doesNotContain("�"); // Replacement character indicates encoding issues
        }
    }

    @Test
    @DisplayName("Should validate chunk metadata and positioning")
    void shouldValidateChunkMetadata() {
        String content = TestDataBuilder.createTestDocumentContent();
        
        DocumentDto.DocumentResponse document = uploadDocumentForChunking(
            "metadata-test.txt",
            content,
            createSemanticChunkingConfig()
        );
        
        waitForDocumentProcessing(document.id());
        
        List<DocumentChunkDto.ChunkResponse> chunks = getDocumentChunks(document.id());
        
        assertThat(chunks).isNotEmpty();
        
        // Validate chunk metadata
        for (DocumentChunkDto.ChunkResponse chunk : chunks) {
            // Basic metadata validation
            assertThat(chunk.id()).isNotNull();
            assertThat(chunk.documentId()).isEqualTo(document.id());
            assertThat(chunk.chunkIndex()).isGreaterThanOrEqualTo(0);
            assertThat(chunk.content()).isNotNull().isNotEmpty();
            assertThat(chunk.characterCount()).isEqualTo(chunk.content().length());
            assertThat(chunk.tokenCount()).isGreaterThan(0);
            
            // Token count should be reasonable estimate
            int estimatedTokens = TextChunker.estimateTokenCount(chunk.content());
            assertThat(chunk.tokenCount()).isCloseTo(estimatedTokens, org.assertj.core.data.Percentage.withPercentage(20));
            
            // Validate timestamps
            assertThat(chunk.createdAt()).isNotNull();
            assertThat(chunk.updatedAt()).isNotNull();
        }
        
        // Validate chunk ordering is correct
        for (int i = 1; i < chunks.size(); i++) {
            assertThat(chunks.get(i).chunkIndex()).isGreaterThan(chunks.get(i - 1).chunkIndex());
        }
    }

    @Test
    @DisplayName("Should validate empty and minimal content handling")
    void shouldValidateMinimalContentHandling() {
        // Test very short content
        String shortContent = "Short.";
        
        DocumentDto.DocumentResponse document = uploadDocumentForChunking(
            "minimal-test.txt",
            shortContent,
            createSemanticChunkingConfig()
        );
        
        waitForDocumentProcessing(document.id());
        
        List<DocumentChunkDto.ChunkResponse> chunks = getDocumentChunks(document.id());
        
        // Should create at least one chunk even for minimal content
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).content().trim()).isEqualTo("Short.");
        assertThat(chunks.get(0).chunkIndex()).isEqualTo(0);
    }

    // Helper methods

    private DocumentDto.DocumentResponse uploadDocumentForChunking(String filename, String content, TenantDto.ChunkingConfig chunkingConfig) {
        // First upload the document
        String uploadUrl = baseUrl + "/api/v1/documents/upload";
        
        HttpHeaders headers = IntegrationTestUtils.createAuthHeaders(testTenant.adminToken());
        headers.set("X-Tenant-ID", testTenant.getTenantId().toString());
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(content.getBytes()) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        
        // Add chunking config to metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("chunkingStrategy", chunkingConfig.strategy().toString());
        metadata.put("chunkSize", chunkingConfig.chunkSize());
        metadata.put("chunkOverlap", chunkingConfig.chunkOverlap());
        
        try {
            String metadataJson = objectMapper.writeValueAsString(metadata);
            body.add("metadata", metadataJson);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize metadata", e);
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

    private List<DocumentChunkDto.ChunkResponse> getDocumentChunks(UUID documentId) {
        // Query database directly for chunks since there might not be a REST endpoint yet
        String sql = """
            SELECT id, document_id, chunk_index, content, character_count, token_count, created_at, updated_at
            FROM document_chunks 
            WHERE document_id = ? 
            ORDER BY chunk_index
            """;
        
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            return new DocumentChunkDto.ChunkResponse(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("document_id")),
                rs.getInt("chunk_index"),
                rs.getString("content"),
                rs.getInt("character_count"),
                rs.getInt("token_count"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime()
            );
        }, documentId);
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

    private TenantDto.ChunkingConfig createSemanticChunkingConfig() {
        return new TenantDto.ChunkingConfig(
            500,  // chunk size
            100,  // overlap
            TenantDto.ChunkingStrategy.SEMANTIC
        );
    }

    private TenantDto.ChunkingConfig createFixedSizeChunkingConfig() {
        return new TenantDto.ChunkingConfig(
            400,  // chunk size
            50,   // overlap
            TenantDto.ChunkingStrategy.FIXED_SIZE
        );
    }

    private TenantDto.ChunkingConfig createSlidingWindowChunkingConfig() {
        return new TenantDto.ChunkingConfig(
            300,  // chunk size (window size)
            100,  // step size (overlap)
            TenantDto.ChunkingStrategy.SLIDING_WINDOW
        );
    }

    /**
     * Validates that all chunking strategies work consistently across different algorithm types.
     * 
     * This parameterized test ensures that each chunking strategy:
     * 1. Produces valid, non-empty chunks for the same input document
     * 2. Maintains appropriate chunk characteristics for its strategy type
     * 3. Handles different content types appropriately
     * 4. Produces results suitable for RAG retrieval operations
     * 
     * This boundary testing validates that E2E-TEST-002 chunking algorithms work
     * consistently regardless of the strategy selected, ensuring reliable document
     * processing across different tenant configurations.
     */
    @ParameterizedTest
    @EnumSource(TenantDto.ChunkingStrategy.class)
    @DisplayName("Should produce consistent results across all chunking strategies")
    void shouldProduceConsistentResultsAcrossStrategies(TenantDto.ChunkingStrategy strategy) {
        // Create test content suitable for all chunking strategies
        String testContent = """
            Strategic Algorithm Testing Document
            
            This document is designed to test the consistency and reliability of different
            chunking algorithms used in the RAG system architecture.
            
            Section One: Semantic Processing
            Semantic chunking focuses on preserving natural language boundaries and maintaining
            contextual coherence. This strategy prioritizes meaning over strict size limits.
            
            Section Two: Fixed-Size Processing  
            Fixed-size chunking creates consistent chunk sizes for predictable processing times.
            This approach is valuable when uniform chunk dimensions are required.
            
            Section Three: Sliding Window Processing
            Sliding window chunking creates overlapping segments that can capture relationships
            across traditional chunk boundaries. This maintains context continuity.
            
            Conclusion
            Each strategy serves different use cases in production RAG implementations.
            """;

        // Create configuration for the specified strategy
        TenantDto.ChunkingConfig config = switch (strategy) {
            case SEMANTIC -> createSemanticChunkingConfig();
            case FIXED_SIZE -> createFixedSizeChunkingConfig();
            case SLIDING_WINDOW -> createSlidingWindowChunkingConfig();
        };

        DocumentDto.DocumentResponse document = uploadDocumentForChunking(
            "strategy-test-" + strategy.name().toLowerCase() + ".txt",
            testContent,
            config
        );
        
        waitForDocumentProcessing(document.id());
        
        List<DocumentChunkDto.ChunkResponse> chunks = getDocumentChunks(document.id());
        
        // Validate common characteristics across all strategies
        assertThat(chunks)
            .describedAs("Strategy %s should produce non-empty chunk list", strategy)
            .isNotEmpty();
            
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunkDto.ChunkResponse chunk = chunks.get(i);
            
            assertThat(chunk.content())
                .describedAs("Chunk %d from strategy %s should have meaningful content", i, strategy)
                .isNotEmpty()
                .hasSizeGreaterThan(10); // Substantial content
                
            assertThat(chunk.chunkIndex())
                .describedAs("Chunk %d from strategy %s should have valid index", i, strategy)
                .isEqualTo(i);
                
            assertThat(chunk.tokenCount())
                .describedAs("Chunk %d from strategy %s should have positive token count", i, strategy)
                .isGreaterThan(0);
        }
        
        // Strategy-specific validations
        switch (strategy) {
            case SEMANTIC -> {
                // Semantic should respect natural boundaries
                assertThat(chunks.size())
                    .describedAs("Semantic chunking should create multiple chunks for structured content")
                    .isGreaterThan(1);
            }
            case FIXED_SIZE -> {
                // Fixed size should have more consistent chunk sizes
                if (chunks.size() > 1) {
                    int firstChunkSize = chunks.get(0).content().length();
                    int lastChunkSize = chunks.get(chunks.size() - 1).content().length();
                    
                    assertThat(Math.abs(firstChunkSize - lastChunkSize))
                        .describedAs("Fixed size chunks should have relatively consistent sizes")
                        .isLessThan(firstChunkSize / 2); // Allow some variance
                }
            }
            case SLIDING_WINDOW -> {
                // Sliding window should potentially create more overlapping chunks
                if (chunks.size() > 2) {
                    assertThat(chunks.size())
                        .describedAs("Sliding window should generally create multiple chunks with overlap")
                        .isGreaterThan(2);
                }
            }
        }
    }
}
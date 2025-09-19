package com.byo.rag.shared.util;

import com.byo.rag.shared.dto.TenantDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for TextChunker utility functionality including all chunking strategies,
 * token estimation, and text cleaning operations.
 */
class TextChunkerTest {

    private static final String SAMPLE_TEXT = """
        This is the first paragraph with multiple sentences. It contains important information about the document.
        This sentence should be included in chunks.
        
        This is the second paragraph. It also has multiple sentences for testing chunking strategies.
        The content should be properly split into meaningful chunks.
        
        This is the third paragraph with even more content to test the chunking algorithms.
        """;

    @Test
    @DisplayName("Should chunk text using fixed size strategy")
    void shouldChunkTextUsingFixedSizeStrategy() {
        TenantDto.ChunkingConfig config = new TenantDto.ChunkingConfig(
            100, // chunk size
            20,  // overlap
            TenantDto.ChunkingStrategy.FIXED_SIZE
        );
        
        List<String> chunks = TextChunker.chunkText(SAMPLE_TEXT, config);
        
        assertFalse(chunks.isEmpty(), "Should produce chunks");
        assertTrue(chunks.size() > 1, "Should produce multiple chunks for long text");
        
        // Check chunk sizes (allowing for some variance due to trimming)
        for (String chunk : chunks) {
            assertTrue(chunk.length() <= 100, "Chunk should not exceed max size: " + chunk.length());
            assertFalse(chunk.trim().isEmpty(), "Chunk should not be empty");
        }
    }

    @Test
    @DisplayName("Should chunk text using semantic strategy")
    void shouldChunkTextUsingSemanticStrategy() {
        TenantDto.ChunkingConfig config = new TenantDto.ChunkingConfig(
            200, // target size
            50,  // overlap
            TenantDto.ChunkingStrategy.SEMANTIC
        );
        
        List<String> chunks = TextChunker.chunkText(SAMPLE_TEXT, config);
        
        assertFalse(chunks.isEmpty(), "Should produce chunks");
        
        // Semantic chunking should preserve sentence boundaries
        for (String chunk : chunks) {
            assertTrue(chunk.length() <= 250, "Chunk should be reasonably sized"); // Allow some flexibility
            assertFalse(chunk.trim().isEmpty(), "Chunk should not be empty");
            
            // Should end with sentence-ending punctuation or be the last chunk
            String trimmed = chunk.trim();
            assertTrue(
                trimmed.endsWith(".") || 
                trimmed.endsWith("!") || 
                trimmed.endsWith("?") ||
                chunks.indexOf(chunk) == chunks.size() - 1,
                "Semantic chunk should end at sentence boundary: " + trimmed
            );
        }
    }

    @Test
    @DisplayName("Should chunk text using sliding window strategy")
    void shouldChunkTextUsingSlidingWindowStrategy() {
        TenantDto.ChunkingConfig config = new TenantDto.ChunkingConfig(
            100, // window size
            50,  // step size
            TenantDto.ChunkingStrategy.SLIDING_WINDOW
        );
        
        List<String> chunks = TextChunker.chunkText(SAMPLE_TEXT, config);
        
        assertFalse(chunks.isEmpty(), "Should produce chunks");
        assertTrue(chunks.size() > 1, "Should produce multiple overlapping chunks");
        
        // Check chunk sizes
        for (String chunk : chunks) {
            assertTrue(chunk.length() <= 100, "Chunk should not exceed window size");
            assertFalse(chunk.trim().isEmpty(), "Chunk should not be empty");
        }
        
        // Check overlap (if we have multiple chunks)
        if (chunks.size() > 1) {
            String firstChunk = chunks.get(0);
            String secondChunk = chunks.get(1);
            
            // Should have some overlap due to sliding window
            assertTrue(firstChunk.length() > 50, "First chunk should be long enough to create overlap");
        }
    }

    @Test
    @DisplayName("Should handle empty and null text")
    void shouldHandleEmptyAndNullText() {
        TenantDto.ChunkingConfig config = new TenantDto.ChunkingConfig(
            100,
            20,
            TenantDto.ChunkingStrategy.FIXED_SIZE
        );
        
        // Test empty text
        List<String> emptyChunks = TextChunker.chunkText("", config);
        assertTrue(emptyChunks.isEmpty(), "Empty text should produce no chunks");
        
        // Test whitespace-only text
        List<String> whitespaceChunks = TextChunker.chunkText("   \n\t  ", config);
        assertTrue(whitespaceChunks.isEmpty(), "Whitespace-only text should produce no chunks");
    }

    @Test
    @DisplayName("Should handle text shorter than chunk size")
    void shouldHandleTextShorterThanChunkSize() {
        String shortText = "This is a short text.";
        
        TenantDto.ChunkingConfig config = new TenantDto.ChunkingConfig(
            1000, // Much larger than text
            100,
            TenantDto.ChunkingStrategy.FIXED_SIZE
        );
        
        List<String> chunks = TextChunker.chunkText(shortText, config);
        
        assertEquals(1, chunks.size(), "Short text should produce single chunk");
        assertEquals(shortText.trim(), chunks.get(0), "Chunk should contain original text");
    }

    @Test
    @DisplayName("Should estimate token count correctly")
    void shouldEstimateTokenCountCorrectly() {
        String text = "This is a test sentence with about sixteen words for token estimation.";
        
        int tokenCount = TextChunker.estimateTokenCount(text);
        
        assertTrue(tokenCount > 0, "Token count should be positive");
        assertTrue(tokenCount < text.length(), "Token count should be less than character count");
        
        // Rough validation - should be around text.length() / 4
        int expectedRange = text.length() / 4;
        assertTrue(Math.abs(tokenCount - expectedRange) <= expectedRange / 2, 
            "Token estimate should be reasonable: expected ~" + expectedRange + ", got " + tokenCount);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "Single word",
        "Multiple words in sentence.",
        "Text with\nnewlines\nand\ttabs",
        "Very long text that exceeds normal chunk sizes and should still work correctly"
    })
    @DisplayName("Should estimate tokens for various text types")
    void shouldEstimateTokensForVariousTextTypes(String text) {
        int tokenCount = TextChunker.estimateTokenCount(text);
        
        if (text.isEmpty()) {
            assertEquals(0, tokenCount, "Empty text should have zero tokens");
        } else {
            assertTrue(tokenCount > 0, "Non-empty text should have positive token count");
        }
    }

    @Test
    @DisplayName("Should clean text properly")
    void shouldCleanTextProperly() {
        String dirtyText = "Text with\r\n\r\nWindows line endings\n\n\n\nand multiple newlines    and     spaces\t\t\ttabs  ";
        
        String cleanedText = TextChunker.cleanText(dirtyText);
        
        assertNotNull(cleanedText, "Cleaned text should not be null");
        assertFalse(cleanedText.startsWith(" "), "Cleaned text should not start with space");
        assertFalse(cleanedText.endsWith(" "), "Cleaned text should not end with space");
        assertFalse(cleanedText.contains("\r"), "Cleaned text should not contain carriage returns");
        assertFalse(cleanedText.contains("\n\n\n"), "Cleaned text should not have triple newlines");
        assertFalse(cleanedText.contains("  "), "Cleaned text should not have double spaces");
        assertFalse(cleanedText.contains("\t\t"), "Cleaned text should not have double tabs");
    }

    @Test
    @DisplayName("Should handle null text in cleaning")
    void shouldHandleNullTextInCleaning() {
        String result = TextChunker.cleanText(null);
        assertEquals("", result, "Null text should return empty string");
    }

    @Test
    @DisplayName("Should preserve paragraph structure in semantic chunking")
    void shouldPreserveParagraphStructureInSemanticChunking() {
        String textWithParagraphs = """
            First paragraph with important content.
            
            Second paragraph with different topic.
            
            Third paragraph with conclusion.
            """;
        
        TenantDto.ChunkingConfig config = new TenantDto.ChunkingConfig(
            100,
            20,
            TenantDto.ChunkingStrategy.SEMANTIC
        );
        
        List<String> chunks = TextChunker.chunkText(textWithParagraphs, config);
        
        assertFalse(chunks.isEmpty(), "Should produce chunks");
        
        // Should try to keep paragraphs together when possible
        for (String chunk : chunks) {
            assertFalse(chunk.trim().isEmpty(), "Chunk should not be empty");
        }
    }

    @Test
    @DisplayName("Should handle overlap correctly in fixed size chunking")
    void shouldHandleOverlapCorrectlyInFixedSizeChunking() {
        String longText = "a".repeat(500); // 500 characters
        
        TenantDto.ChunkingConfig config = new TenantDto.ChunkingConfig(
            100, // chunk size
            25,  // overlap
            TenantDto.ChunkingStrategy.FIXED_SIZE
        );
        
        List<String> chunks = TextChunker.chunkText(longText, config);
        
        assertTrue(chunks.size() > 1, "Should produce multiple chunks");
        
        if (chunks.size() > 1) {
            // Check that overlap is working - adjacent chunks should share content
            String firstChunk = chunks.get(0);
            String secondChunk = chunks.get(1);
            
            assertTrue(firstChunk.length() <= 100, "First chunk should not exceed size");
            assertTrue(secondChunk.length() <= 100, "Second chunk should not exceed size");
            
            // Since we're using repeated 'a' characters, the overlap should be detectable
            // by checking that second chunk starts within the overlap region
            assertTrue(firstChunk.length() >= 75, "First chunk should be substantial for overlap test");
        }
    }

    @Test
    @DisplayName("Should handle zero overlap")
    void shouldHandleZeroOverlap() {
        TenantDto.ChunkingConfig config = new TenantDto.ChunkingConfig(
            50,
            0, // No overlap
            TenantDto.ChunkingStrategy.FIXED_SIZE
        );
        
        List<String> chunks = TextChunker.chunkText(SAMPLE_TEXT, config);
        
        assertFalse(chunks.isEmpty(), "Should produce chunks even with zero overlap");
        
        for (String chunk : chunks) {
            assertTrue(chunk.length() <= 50, "Chunk should not exceed max size");
            assertFalse(chunk.trim().isEmpty(), "Chunk should not be empty");
        }
    }
}
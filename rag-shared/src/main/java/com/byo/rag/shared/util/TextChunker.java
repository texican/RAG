package com.byo.rag.shared.util;

import com.byo.rag.shared.dto.TenantDto;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Utility class for splitting text into optimally sized chunks for RAG processing.
 * 
 * <p>Text chunking is a critical preprocessing step in RAG systems that breaks large documents
 * into smaller, semantically meaningful pieces. This ensures that vector embeddings capture
 * focused semantic content and that retrieved context fits within LLM token limits.</p>
 * 
 * <p>Supported chunking strategies:</p>
 * <ul>
 *   <li><strong>Fixed Size:</strong> Simple character-based chunking with configurable overlap</li>
 *   <li><strong>Semantic:</strong> Intelligent chunking that preserves sentence and paragraph boundaries</li>
 *   <li><strong>Sliding Window:</strong> Overlapping chunks with configurable step size</li>
 * </ul>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * TenantDto.ChunkingConfig config = new TenantDto.ChunkingConfig(
 *     TenantDto.ChunkingStrategy.SEMANTIC, 
 *     1000, 
 *     200
 * );
 * List<String> chunks = TextChunker.chunkText(documentText, config);
 * }</pre>
 * 
 * @author Enterprise RAG Development Team
 * @since 1.0.0
 * @version 1.0
 */
public class TextChunker {

    /** Regular expression pattern for identifying sentence boundaries. */
    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("(?<=[.!?])\\s+");
    
    /** Regular expression pattern for identifying paragraph boundaries. */
    private static final Pattern PARAGRAPH_BOUNDARY = Pattern.compile("\\n\\s*\\n");

    /**
     * Splits text into chunks based on the specified chunking configuration.
     *
     * @param text the input text to be chunked
     * @param config the chunking configuration specifying strategy, size, and overlap
     * @return a list of text chunks optimized for RAG processing
     * @throws IllegalArgumentException if text is null or config is invalid
     */
    public static List<String> chunkText(String text, TenantDto.ChunkingConfig config) {
        return switch (config.strategy()) {
            case FIXED_SIZE -> chunkFixedSize(text, config.chunkSize(), config.chunkOverlap());
            case SEMANTIC -> chunkSemantic(text, config.chunkSize(), config.chunkOverlap());
            case SLIDING_WINDOW -> chunkSlidingWindow(text, config.chunkSize(), config.chunkOverlap());
        };
    }

    private static List<String> chunkFixedSize(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            String chunk = text.substring(start, end).trim();
            
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            
            start += chunkSize - overlap;
            if (start >= text.length()) break;
        }
        
        return chunks;
    }

    private static List<String> chunkSemantic(String text, int targetSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        String[] paragraphs = PARAGRAPH_BOUNDARY.split(text);
        
        StringBuilder currentChunk = new StringBuilder();
        
        for (String paragraph : paragraphs) {
            String[] sentences = SENTENCE_BOUNDARY.split(paragraph.trim());
            
            for (String sentence : sentences) {
                sentence = sentence.trim();
                if (sentence.isEmpty()) continue;
                
                if (currentChunk.length() + sentence.length() + 1 > targetSize && !currentChunk.isEmpty()) {
                    chunks.add(currentChunk.toString().trim());
                    
                    // Handle overlap by keeping the last few sentences
                    if (overlap > 0) {
                        String[] chunkSentences = SENTENCE_BOUNDARY.split(currentChunk.toString());
                        currentChunk = new StringBuilder();
                        
                        int overlapChars = 0;
                        for (int i = chunkSentences.length - 1; i >= 0 && overlapChars < overlap; i--) {
                            currentChunk.insert(0, chunkSentences[i] + " ");
                            overlapChars += chunkSentences[i].length();
                        }
                    } else {
                        currentChunk = new StringBuilder();
                    }
                }
                
                if (!currentChunk.isEmpty()) {
                    currentChunk.append(" ");
                }
                currentChunk.append(sentence);
            }
            
            if (!currentChunk.isEmpty()) {
                currentChunk.append("\n");
            }
        }
        
        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString().trim());
        }
        
        return chunks;
    }

    private static List<String> chunkSlidingWindow(String text, int windowSize, int stepSize) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        
        while (start < text.length()) {
            int end = Math.min(start + windowSize, text.length());
            String chunk = text.substring(start, end).trim();
            
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            
            start += stepSize;
            if (start >= text.length()) break;
        }
        
        return chunks;
    }

    /**
     * Estimates the token count for a given text using a simple heuristic.
     * <p>This uses an approximation of ~4 characters per token for English text,
     * which is suitable for rough capacity planning but not precise tokenization.</p>
     *
     * @param text the text to estimate tokens for
     * @return estimated number of tokens
     */
    public static int estimateTokenCount(String text) {
        // Rough estimation: ~4 characters per token for English text
        return (int) Math.ceil(text.length() / 4.0);
    }

    /**
     * Cleans and normalizes text for optimal chunking and processing.
     * <p>Performs the following normalization steps:</p>
     * <ul>
     *   <li>Normalizes line endings to Unix format (\n)</li>
     *   <li>Reduces multiple consecutive newlines to double newlines</li>
     *   <li>Collapses multiple spaces and tabs to single spaces</li>
     *   <li>Trims whitespace from beginning and end</li>
     * </ul>
     *
     * @param text the text to clean, may be null
     * @return cleaned text, or empty string if input was null
     */
    public static String cleanText(String text) {
        if (text == null) return "";
        
        return text
            .replaceAll("\\r\\n|\\r", "\n")  // Normalize line endings
            .replaceAll("\\n{3,}", "\n\n")   // Reduce multiple newlines
            .replaceAll("[ \\t]{2,}", " ")    // Reduce multiple spaces/tabs
            .trim();
    }
}
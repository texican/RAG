package com.enterprise.rag.shared.util;

import com.enterprise.rag.shared.dto.TenantDto;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class TextChunker {

    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("(?<=[.!?])\\s+");
    private static final Pattern PARAGRAPH_BOUNDARY = Pattern.compile("\\n\\s*\\n");

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

    public static int estimateTokenCount(String text) {
        // Rough estimation: ~4 characters per token for English text
        return (int) Math.ceil(text.length() / 4.0);
    }

    public static String cleanText(String text) {
        if (text == null) return "";
        
        return text
            .replaceAll("\\r\\n|\\r", "\n")  // Normalize line endings
            .replaceAll("\\n{3,}", "\n\n")   // Reduce multiple newlines
            .replaceAll("[ \\t]{2,}", " ")    // Reduce multiple spaces/tabs
            .trim();
    }
}
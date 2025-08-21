package com.enterprise.rag.core.service;

import com.enterprise.rag.core.dto.RagQueryRequest;
import com.enterprise.rag.core.dto.RagQueryResponse.SourceDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for assembling context from retrieved documents for LLM processing.
 */
@Service
public class ContextAssemblyService {

    private static final Logger logger = LoggerFactory.getLogger(ContextAssemblyService.class);

    @Value("${rag.context.max-tokens:4000}")
    private int maxContextTokens;

    @Value("${rag.context.chunk-separator:\n\n---\n\n}")
    private String chunkSeparator;

    @Value("${rag.context.include-metadata:true}")
    private boolean includeMetadata;

    @Value("${rag.context.relevance-threshold:0.7}")
    private double relevanceThreshold;

    /**
     * Assemble context from retrieved documents.
     */
    public String assembleContext(List<SourceDocument> documents, RagQueryRequest request) {
        if (documents == null || documents.isEmpty()) {
            logger.debug("No documents provided for context assembly");
            return "";
        }

        logger.debug("Assembling context from {} documents for tenant: {}", 
                    documents.size(), request.tenantId());

        // Filter documents by relevance threshold
        List<SourceDocument> relevantDocuments = documents.stream()
            .filter(doc -> doc.relevanceScore() >= relevanceThreshold)
            .collect(Collectors.toList());

        if (relevantDocuments.isEmpty()) {
            logger.warn("No documents meet relevance threshold {} for tenant: {}", 
                       relevanceThreshold, request.tenantId());
            return "";
        }

        // Build context while respecting token limits
        StringBuilder contextBuilder = new StringBuilder();
        int currentTokenCount = 0;
        int documentsUsed = 0;

        for (SourceDocument document : relevantDocuments) {
            String documentContext = formatDocumentForContext(document, request);
            int documentTokens = estimateTokenCount(documentContext);

            // Check if adding this document would exceed token limit
            if (currentTokenCount + documentTokens > maxContextTokens && documentsUsed > 0) {
                logger.debug("Token limit reached. Used {}/{} documents, {} tokens", 
                           documentsUsed, relevantDocuments.size(), currentTokenCount);
                break;
            }

            if (contextBuilder.length() > 0) {
                contextBuilder.append(chunkSeparator);
            }

            contextBuilder.append(documentContext);
            currentTokenCount += documentTokens;
            documentsUsed++;
        }

        String finalContext = contextBuilder.toString();
        logger.info("Context assembled: {} documents, {} estimated tokens, tenant: {}", 
                   documentsUsed, currentTokenCount, request.tenantId());

        return finalContext;
    }

    /**
     * Assemble context with custom configuration.
     */
    public String assembleContext(List<SourceDocument> documents, RagQueryRequest request,
                                ContextConfig config) {
        int originalMaxTokens = this.maxContextTokens;
        double originalThreshold = this.relevanceThreshold;
        boolean originalIncludeMetadata = this.includeMetadata;

        try {
            // Temporarily override configuration
            this.maxContextTokens = config.maxTokens();
            this.relevanceThreshold = config.relevanceThreshold();
            this.includeMetadata = config.includeMetadata();

            return assembleContext(documents, request);
        } finally {
            // Restore original configuration
            this.maxContextTokens = originalMaxTokens;
            this.relevanceThreshold = originalThreshold;
            this.includeMetadata = originalIncludeMetadata;
        }
    }

    /**
     * Optimize context for better LLM performance.
     */
    public String optimizeContext(String rawContext, RagQueryRequest request) {
        if (rawContext == null || rawContext.trim().isEmpty()) {
            return rawContext;
        }

        String optimizedContext = rawContext;

        // Remove excessive whitespace
        optimizedContext = optimizedContext.replaceAll("\\s+", " ");

        // Remove duplicate content (simple deduplication)
        optimizedContext = removeDuplicateContent(optimizedContext);

        // Trim to exact token limit if still too long
        if (estimateTokenCount(optimizedContext) > maxContextTokens) {
            optimizedContext = truncateToTokenLimit(optimizedContext, maxContextTokens);
        }

        logger.debug("Context optimized from {} to {} estimated tokens", 
                    estimateTokenCount(rawContext), estimateTokenCount(optimizedContext));

        return optimizedContext;
    }

    /**
     * Get context assembly statistics.
     */
    public ContextStats getContextStats(List<SourceDocument> documents, String assembledContext) {
        if (documents == null || documents.isEmpty()) {
            return new ContextStats(0, 0, 0, 0.0, 0);
        }

        int totalDocuments = documents.size();
        int relevantDocuments = (int) documents.stream()
            .filter(doc -> doc.relevanceScore() >= relevanceThreshold)
            .count();
        
        int estimatedTokens = estimateTokenCount(assembledContext);
        double averageRelevance = documents.stream()
            .mapToDouble(SourceDocument::relevanceScore)
            .average()
            .orElse(0.0);

        return new ContextStats(
            totalDocuments,
            relevantDocuments,
            estimatedTokens,
            averageRelevance,
            maxContextTokens
        );
    }

    private String formatDocumentForContext(SourceDocument document, RagQueryRequest request) {
        StringBuilder formatted = new StringBuilder();

        // Add document header with metadata if enabled
        if (includeMetadata && document.title() != null) {
            formatted.append("Document: ").append(document.title()).append("\n");
            
            if (document.documentType() != null) {
                formatted.append("Type: ").append(document.documentType()).append("\n");
            }
            
            formatted.append("Relevance: ").append(String.format("%.2f", document.relevanceScore())).append("\n");
            formatted.append("---\n");
        }

        // Add main content
        formatted.append(document.content());

        // Add chunk metadata if available
        if (includeMetadata && document.metadata() != null && !document.metadata().isEmpty()) {
            formatted.append("\n[Metadata: ");
            document.metadata().entrySet().stream()
                .filter(entry -> isRelevantMetadata(entry.getKey()))
                .forEach(entry -> formatted.append(entry.getKey())
                    .append("=").append(entry.getValue()).append(" "));
            formatted.append("]");
        }

        return formatted.toString();
    }

    private boolean isRelevantMetadata(String key) {
        // Only include metadata that might be useful for context
        return key.equals("section") || key.equals("page") || key.equals("chapter") ||
               key.equals("author") || key.equals("date") || key.equals("category");
    }

    private String removeDuplicateContent(String content) {
        // Simple deduplication - remove sentences that appear multiple times
        String[] sentences = content.split("\\. ");
        StringBuilder deduplicated = new StringBuilder();
        
        for (int i = 0; i < sentences.length; i++) {
            String sentence = sentences[i].trim();
            if (sentence.length() < 20) continue; // Skip very short sentences
            
            boolean isDuplicate = false;
            for (int j = i + 1; j < sentences.length; j++) {
                if (sentences[j].trim().equals(sentence)) {
                    isDuplicate = true;
                    break;
                }
            }
            
            if (!isDuplicate) {
                if (deduplicated.length() > 0) {
                    deduplicated.append(". ");
                }
                deduplicated.append(sentence);
            }
        }
        
        return deduplicated.toString();
    }

    private String truncateToTokenLimit(String content, int maxTokens) {
        int estimatedTokens = estimateTokenCount(content);
        if (estimatedTokens <= maxTokens) {
            return content;
        }

        // Estimate character count needed (rough approximation)
        double charactersPerToken = (double) content.length() / estimatedTokens;
        int targetCharacters = (int) (maxTokens * charactersPerToken * 0.9); // 10% buffer

        if (targetCharacters >= content.length()) {
            return content;
        }

        // Truncate at word boundary
        String truncated = content.substring(0, Math.min(targetCharacters, content.length()));
        int lastSpace = truncated.lastIndexOf(' ');
        if (lastSpace > targetCharacters * 0.8) { // Don't truncate too much
            truncated = truncated.substring(0, lastSpace);
        }

        return truncated + "...";
    }

    private int estimateTokenCount(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        // Rough estimation: ~4 characters per token for English text
        return text.length() / 4;
    }

    /**
     * Configuration for context assembly.
     */
    public record ContextConfig(
        int maxTokens,
        double relevanceThreshold,
        boolean includeMetadata,
        String chunkSeparator
    ) {
        public static ContextConfig defaultConfig() {
            return new ContextConfig(4000, 0.7, true, "\n\n---\n\n");
        }

        public static ContextConfig withMaxTokens(int maxTokens) {
            return new ContextConfig(maxTokens, 0.7, true, "\n\n---\n\n");
        }

        public static ContextConfig minimal() {
            return new ContextConfig(2000, 0.8, false, "\n\n");
        }
    }

    /**
     * Context assembly statistics.
     */
    public record ContextStats(
        int totalDocuments,
        int relevantDocuments,
        int estimatedTokens,
        double averageRelevanceScore,
        int maxTokenLimit
    ) {}
}
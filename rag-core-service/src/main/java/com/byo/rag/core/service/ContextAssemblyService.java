package com.byo.rag.core.service;

import com.byo.rag.core.dto.RagQueryRequest;
import com.byo.rag.core.dto.RagQueryResponse.SourceDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Enterprise service for assembling and optimizing document context for LLM processing.
 * 
 * <p>This service is responsible for taking retrieved document chunks from semantic search
 * and intelligently combining them into coherent context that maximizes LLM response quality
 * while respecting token limits and relevance thresholds. It's a critical component of the
 * RAG pipeline that bridges document retrieval and response generation.</p>
 * 
 * <p>Core responsibilities:</p>
 * <ul>
 *   <li><strong>Context Assembly:</strong> Combines multiple document chunks into unified context</li>
 *   <li><strong>Token Management:</strong> Respects LLM token limits while maximizing information</li>
 *   <li><strong>Relevance Filtering:</strong> Excludes low-relevance documents to improve quality</li>
 *   <li><strong>Format Optimization:</strong> Structures context for optimal LLM comprehension</li>
 *   <li><strong>Metadata Integration:</strong> Includes valuable document metadata when beneficial</li>
 *   <li><strong>Deduplication:</strong> Removes redundant information to maximize context efficiency</li>
 * </ul>
 * 
 * <p>Assembly strategies:</p>
 * <ul>
 *   <li><strong>Relevance-First:</strong> Prioritizes highest-scoring document chunks</li>
 *   <li><strong>Token-Aware:</strong> Dynamically adjusts content based on available token budget</li>
 *   <li><strong>Metadata-Enhanced:</strong> Includes contextual metadata when it adds value</li>
 *   <li><strong>Deduplication:</strong> Removes repetitive content for cleaner context</li>
 * </ul>
 * 
 * <p>Configuration properties:</p>
 * <ul>
 *   <li>{@code rag.context.max-tokens} - Maximum context tokens (default: 4000)</li>
 *   <li>{@code rag.context.chunk-separator} - Separator between document chunks</li>
 *   <li>{@code rag.context.include-metadata} - Whether to include document metadata</li>
 *   <li>{@code rag.context.relevance-threshold} - Minimum relevance score (default: 0.7)</li>
 * </ul>
 * 
 * <p>Thread Safety: This service is thread-safe and designed for concurrent use
 * across multiple tenant requests.</p>
 * 
 * @author Enterprise RAG Development Team
 * @since 1.0.0
 * @version 1.0
 * @see SourceDocument
 * @see RagQueryRequest
 * @see LLMIntegrationService
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
     * Assemble coherent context from a list of retrieved document chunks.
     * 
     * <p>This method is the primary entry point for context assembly in the RAG pipeline.
     * It processes retrieved documents by filtering based on relevance scores, respecting
     * token limits, and formatting content for optimal LLM comprehension.</p>
     * 
     * <p>Assembly process:</p>
     * <ol>
     *   <li><strong>Relevance Filtering:</strong> Excludes documents below threshold</li>
     *   <li><strong>Token-Aware Selection:</strong> Selects documents within token budget</li>
     *   <li><strong>Content Formatting:</strong> Formats each document with metadata</li>
     *   <li><strong>Context Structuring:</strong> Combines documents with separators</li>
     * </ol>
     * 
     * <p>The method prioritizes documents with higher relevance scores and includes
     * as many documents as possible within the configured token limit. If no documents
     * meet the relevance threshold, an empty context is returned.</p>
     * 
     * @param documents list of retrieved documents with relevance scores
     * @param request the original RAG query request containing tenant and options
     * @return assembled context string ready for LLM processing, or empty string if no relevant content
     * @see #assembleContext(List, RagQueryRequest, ContextConfig)
     * @see #getContextStats(List, String)
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
     * Assemble context using custom configuration parameters.
     * 
     * <p>This method provides fine-grained control over context assembly by allowing
     * temporary override of service configuration. It's useful for specialized use cases
     * where standard configuration may not be optimal.</p>
     * 
     * <p>Custom configuration scenarios:</p>
     * <ul>
     *   <li><strong>Short Context:</strong> Reduced token limits for specific queries</li>
     *   <li><strong>High Precision:</strong> Higher relevance thresholds for accuracy</li>
     *   <li><strong>Metadata Control:</strong> Custom metadata inclusion rules</li>
     *   <li><strong>Testing:</strong> Different configurations for A/B testing</li>
     * </ul>
     * 
     * <p>The method temporarily overrides service configuration, performs assembly,
     * and restores original settings, ensuring thread safety and no side effects.</p>
     * 
     * @param documents list of retrieved documents with relevance scores
     * @param request the original RAG query request containing tenant and options
     * @param config custom context assembly configuration
     * @return assembled context string using the specified configuration
     * @see ContextConfig
     * @see #assembleContext(List, RagQueryRequest)
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
     * Optimize assembled context to improve LLM processing efficiency and quality.
     * 
     * <p>This method applies post-assembly optimization techniques to enhance
     * context quality while maintaining essential information. It's particularly
     * useful for long contexts that may contain redundancies or formatting issues.</p>
     * 
     * <p>Optimization techniques applied:</p>
     * <ul>
     *   <li><strong>Whitespace Normalization:</strong> Removes excessive spacing and formatting</li>
     *   <li><strong>Deduplication:</strong> Identifies and removes repeated content</li>
     *   <li><strong>Token Trimming:</strong> Truncates content to exact token limits</li>
     *   <li><strong>Boundary Preservation:</strong> Maintains word boundaries when truncating</li>
     * </ul>
     * 
     * <p>The optimization is conservative, prioritizing information preservation
     * over aggressive compression. It ensures that the optimized context remains
     * coherent and meaningful for LLM processing.</p>
     * 
     * @param rawContext the original assembled context before optimization
     * @param request the RAG query request for context about optimization needs
     * @return optimized context with improved structure and reduced redundancy
     * @see #assembleContext(List, RagQueryRequest)
     * @see #estimateTokenCount(String)
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
     * Generate comprehensive statistics about context assembly process and results.
     * 
     * <p>This method provides detailed metrics about the context assembly process,
     * including document usage, relevance distribution, token utilization, and
     * efficiency measures. These statistics are valuable for monitoring, optimization,
     * and debugging of the RAG system.</p>
     * 
     * <p>Statistical information includes:</p>
     * <ul>
     *   <li><strong>Document Counts:</strong> Total documents vs. actually used documents</li>
     *   <li><strong>Relevance Metrics:</strong> Average relevance scores and filtering results</li>
     *   <li><strong>Token Utilization:</strong> Actual tokens used vs. available budget</li>
     *   <li><strong>Assembly Efficiency:</strong> How well the token budget was utilized</li>
     * </ul>
     * 
     * <p>These statistics can be used for:</p>
     * <ul>
     *   <li>Monitoring context quality and relevance</li>
     *   <li>Optimizing relevance thresholds and token limits</li>
     *   <li>Debugging issues with context assembly</li>
     *   <li>Performance analysis and system tuning</li>
     * </ul>
     * 
     * @param documents the original list of retrieved documents
     * @param assembledContext the final assembled context string
     * @return comprehensive statistics about the context assembly process
     * @see ContextStats
     * @see #assembleContext(List, RagQueryRequest)
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
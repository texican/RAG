package com.enterprise.rag.core.service;

import com.enterprise.rag.core.dto.RagQueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Enterprise service for analyzing, optimizing, and enhancing RAG queries for improved retrieval performance.
 * 
 * <p>This service implements sophisticated query preprocessing techniques to enhance the effectiveness
 * of semantic search and document retrieval. It analyzes incoming user queries for potential issues,
 * applies various optimization strategies, and provides suggestions for query improvement.</p>
 * 
 * <p>Core optimization capabilities:</p>
 * <ul>
 *   <li><strong>Query Analysis:</strong> Comprehensive analysis of query structure, complexity, and content</li>
 *   <li><strong>Automatic Enhancement:</strong> Acronym expansion, stopword handling, and text cleaning</li>
 *   <li><strong>Content Normalization:</strong> Standardization of query format and structure</li>
 *   <li><strong>Alternative Generation:</strong> Suggestion of alternative query phrasings</li>
 *   <li><strong>Term Extraction:</strong> Identification of key terms for highlighting and matching</li>
 * </ul>
 * 
 * <p>Optimization strategies implemented:</p>
 * <ul>
 *   <li><strong>Acronym Expansion:</strong> Replaces technical acronyms with full terms for better matching</li>
 *   <li><strong>Text Cleaning:</strong> Removes unnecessary punctuation and normalizes formatting</li>
 *   <li><strong>Length Validation:</strong> Ensures queries are within optimal length bounds</li>
 *   <li><strong>Stopword Management:</strong> Optional removal of common words (configurable)</li>
 *   <li><strong>Complexity Analysis:</strong> Assessment of query complexity for processing strategies</li>
 * </ul>
 * 
 * <p>Quality analysis features:</p>
 * <ul>
 *   <li>Detection of overly short or long queries</li>
 *   <li>Identification of queries with only common words</li>
 *   <li>Recognition of potential typos and formatting issues</li>
 *   <li>Complexity scoring for appropriate processing strategies</li>
 *   <li>Key term extraction for search enhancement</li>
 * </ul>
 * 
 * <p>Configuration properties:</p>
 * <ul>
 *   <li>{@code query.optimization.enabled} - Enable/disable optimization (default: true)</li>
 *   <li>{@code query.optimization.min-length} - Minimum query length (default: 3)</li>
 *   <li>{@code query.optimization.max-length} - Maximum query length (default: 500)</li>
 *   <li>{@code query.optimization.expand-acronyms} - Enable acronym expansion (default: true)</li>
 *   <li>{@code query.optimization.remove-stopwords} - Enable stopword removal (default: false)</li>
 * </ul>
 * 
 * @author Enterprise RAG Development Team
 * @since 1.0.0
 * @version 1.0
 * @see QueryAnalysis
 * @see QueryComplexity  
 * @see RagQueryRequest
 */
@Service
public class QueryOptimizationService {

    private static final Logger logger = LoggerFactory.getLogger(QueryOptimizationService.class);

    @Value("${query.optimization.enabled:true}")
    private boolean optimizationEnabled;

    @Value("${query.optimization.min-length:3}")
    private int minQueryLength;

    @Value("${query.optimization.max-length:500}")
    private int maxQueryLength;

    @Value("${query.optimization.expand-acronyms:true}")
    private boolean expandAcronyms;

    @Value("${query.optimization.remove-stopwords:false}")
    private boolean removeStopwords;

    // Common stop words (could be externalized)
    private static final Set<String> STOP_WORDS = Set.of(
        "a", "an", "and", "are", "as", "at", "be", "by", "for", "from", "in", "is", "it", 
        "of", "on", "that", "the", "to", "was", "will", "with", "the", "this", "these", "those"
    );

    // Common acronym expansions (could be loaded from configuration)
    private static final Map<String, String> ACRONYM_EXPANSIONS = Map.of(
        "AI", "artificial intelligence",
        "ML", "machine learning",
        "API", "application programming interface",
        "REST", "representational state transfer",
        "HTTP", "hypertext transfer protocol",
        "JSON", "javascript object notation",
        "SQL", "structured query language",
        "NoSQL", "not only structured query language"
    );

    // Patterns for query cleaning
    private static final Pattern EXTRA_WHITESPACE = Pattern.compile("\\s+");

    /**
     * Optimize an incoming RAG query request to improve retrieval and response quality.
     * 
     * <p>This method is the main entry point for query optimization in the RAG pipeline.
     * It applies various enhancement strategies to improve the effectiveness of semantic
     * search and document retrieval, while preserving the original intent of the user query.</p>
     * 
     * <p>Optimization process:</p>
     * <ol>
     *   <li><strong>Query Analysis:</strong> Evaluates query structure and content</li>
     *   <li><strong>Text Cleaning:</strong> Removes unnecessary punctuation and formatting</li>
     *   <li><strong>Acronym Expansion:</strong> Replaces acronyms with full terms</li>
     *   <li><strong>Stopword Processing:</strong> Optional removal of common words</li>
     *   <li><strong>Validation:</strong> Ensures optimized query meets quality standards</li>
     * </ol>
     * 
     * <p>The method is conservative in its approach - if optimization would make the query
     * worse (e.g., too short), it returns the original query unchanged. All optimizations
     * are logged for monitoring and debugging purposes.</p>
     * 
     * @param request the original RAG query request to optimize
     * @return optimized query request with enhanced query text, or original if optimization disabled/failed
     * @see #analyzeQuery(String)
     * @see #performQueryOptimization(String)
     */
    public RagQueryRequest optimizeQuery(RagQueryRequest request) {
        if (!optimizationEnabled || request.query() == null) {
            return request;
        }

        long startTime = System.currentTimeMillis();
        String originalQuery = request.query();

        try {
            String optimizedQuery = performQueryOptimization(originalQuery);
            
            // Create optimized request if query changed
            if (!originalQuery.equals(optimizedQuery)) {
                RagQueryRequest optimizedRequest = createOptimizedRequest(request, optimizedQuery);
                
                long optimizationTime = System.currentTimeMillis() - startTime;
                logger.debug("Query optimized for tenant: {} in {}ms - Original: '{}', Optimized: '{}'", 
                           request.tenantId(), optimizationTime, originalQuery, optimizedQuery);
                
                return optimizedRequest;
            }

            return request;

        } catch (Exception e) {
            logger.error("Query optimization failed for tenant: {}, using original query", 
                        request.tenantId(), e);
            return request;
        }
    }

    /**
     * Perform comprehensive analysis of a query to identify issues and suggest improvements.
     * 
     * <p>This method provides detailed analysis of query characteristics, potential problems,
     * and specific suggestions for improvement. It's used both for real-time query enhancement
     * and for providing feedback to users about query quality.</p>
     * 
     * <p>Analysis dimensions:</p>
     * <ul>
     *   <li><strong>Length Analysis:</strong> Evaluates if query is too short/long for effective retrieval</li>
     *   <li><strong>Content Analysis:</strong> Checks for meaningful content vs. common words</li>
     *   <li><strong>Technical Analysis:</strong> Identifies acronyms, typos, and formatting issues</li>
     *   <li><strong>Complexity Analysis:</strong> Assesses query complexity for processing strategy</li>
     *   <li><strong>Term Extraction:</strong> Identifies key terms for search optimization</li>
     * </ul>
     * 
     * <p>Analysis results include:</p>
     * <ul>
     *   <li>Specific issues identified in the query</li>
     *   <li>Actionable suggestions for improvement</li>
     *   <li>Key terms extracted for highlighting</li>
     *   <li>Complexity classification for processing</li>
     * </ul>
     * 
     * @param query the user query text to analyze
     * @return comprehensive analysis with issues, suggestions, and key terms
     * @see QueryAnalysis
     * @see #extractKeyTerms(String)
     * @see #analyzeComplexity(String)
     */
    public QueryAnalysis analyzeQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return QueryAnalysis.empty();
        }

        List<String> suggestions = new ArrayList<>();
        List<String> issues = new ArrayList<>();

        // Length analysis
        if (query.length() < minQueryLength) {
            issues.add("Query is very short and may not provide enough context");
            suggestions.add("Try adding more specific details or context to your question");
        } else if (query.length() > maxQueryLength) {
            issues.add("Query is very long and may be too complex");
            suggestions.add("Try breaking down your question into more specific parts");
        }

        // Content analysis
        if (containsOnlyStopWords(query)) {
            issues.add("Query contains mostly common words");
            suggestions.add("Add more specific terms related to your topic");
        }

        if (containsUnexpandedAcronyms(query)) {
            suggestions.add("Consider spelling out acronyms for better matching");
        }

        if (containsTypos(query)) {
            suggestions.add("Check for potential spelling errors");
        }

        // Complexity analysis
        QueryComplexity complexity = analyzeComplexity(query);
        
        return new QueryAnalysis(
            query,
            query.length(),
            countWords(query),
            complexity,
            issues,
            suggestions,
            extractKeyTerms(query)
        );
    }

    /**
     * Extract significant key terms from a query for search highlighting and enhancement.
     * 
     * <p>This method identifies the most important terms in a user query by filtering out
     * common words, short terms, and non-alphabetic content. The extracted terms are used
     * for search result highlighting, query expansion, and relevance scoring.</p>
     * 
     * <p>Extraction criteria:</p>
     * <ul>
     *   <li><strong>Minimum Length:</strong> Terms must be longer than 2 characters</li>
     *   <li><strong>Stopword Filtering:</strong> Removes common English words</li>
     *   <li><strong>Alphabetic Only:</strong> Excludes numbers and special characters</li>
     *   <li><strong>Case Normalization:</strong> Converts to lowercase for consistency</li>
     *   <li><strong>Deduplication:</strong> Removes duplicate terms</li>
     * </ul>
     * 
     * <p>Use cases:</p>
     * <ul>
     *   <li>Search result highlighting in user interfaces</li>
     *   <li>Query expansion for improved retrieval</li>
     *   <li>Relevance scoring and document ranking</li>
     *   <li>Analytics and query pattern analysis</li>
     * </ul>
     * 
     * @param query the user query to extract terms from
     * @return list of significant terms sorted alphabetically, or empty list if no meaningful terms
     * @see #analyzeQuery(String)
     * @see #suggestAlternatives(String)
     */
    public List<String> extractKeyTerms(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        return Arrays.stream(query.toLowerCase().split("\\s+"))
            .filter(word -> word.length() > 2)
            .filter(word -> !STOP_WORDS.contains(word))
            .filter(word -> word.matches("[a-zA-Z]+"))
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

    /**
     * Generate alternative phrasings of a user query to improve search results.
     * 
     * <p>This method creates variations of the original query that might yield better
     * search results or provide different perspectives on the same information need.
     * The alternatives are generated using common query patterns and transformations.</p>
     * 
     * <p>Alternative generation strategies:</p>
     * <ul>
     *   <li><strong>Question Forms:</strong> Converts statements to questions ("What is...?", "How does...?")</li>
     *   <li><strong>Context Extensions:</strong> Adds common suffixes ("examples", "definition")</li>
     *   <li><strong>Informational Variants:</strong> Creates "Tell me about..." variations</li>
     *   <li><strong>Practical Extensions:</strong> Adds "best practices" and similar practical terms</li>
     * </ul>
     * 
     * <p>Use cases:</p>
     * <ul>
     *   <li>Query suggestion dropdowns in user interfaces</li>
     *   <li>Automatic query expansion for broader search</li>
     *   <li>A/B testing different query formulations</li>
     *   <li>Helping users refine their search intent</li>
     * </ul>
     * 
     * <p>The method generates a maximum of 5 alternatives to avoid overwhelming users
     * while providing meaningful options for query refinement.</p>
     * 
     * @param query the original query to generate alternatives for
     * @return list of alternative query phrasings (max 5), or empty list if query is invalid
     * @see #analyzeQuery(String)
     * @see #extractKeyTerms(String)
     */
    public List<String> suggestAlternatives(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        List<String> alternatives = new ArrayList<>();

        // Add question form if not already a question
        if (!query.trim().endsWith("?")) {
            alternatives.add("What is " + query.toLowerCase() + "?");
            alternatives.add("How does " + query.toLowerCase() + " work?");
            alternatives.add("Tell me about " + query.toLowerCase());
        }

        // Add more specific versions
        alternatives.add(query + " examples");
        alternatives.add(query + " definition");
        alternatives.add(query + " best practices");

        return alternatives.stream()
            .limit(5)
            .collect(Collectors.toList());
    }

    private String performQueryOptimization(String query) {
        String optimized = query.trim();

        // Basic cleaning
        optimized = cleanQuery(optimized);

        // Expand acronyms if enabled
        if (expandAcronyms) {
            optimized = expandAcronyms(optimized);
        }

        // Remove stopwords if enabled (be careful with this)
        if (removeStopwords) {
            optimized = removeStopwords(optimized);
        }

        // Normalize whitespace
        optimized = EXTRA_WHITESPACE.matcher(optimized).replaceAll(" ").trim();

        // Validate result
        if (optimized.length() < minQueryLength) {
            return query; // Return original if optimization made it too short
        }

        return optimized;
    }

    private String cleanQuery(String query) {
        // Remove excessive punctuation but keep essential characters
        String cleaned = query.replaceAll("[!@#$%^&*()+=\\[\\]{}|;':\"<>?]", " ");
        
        // Normalize whitespace
        cleaned = EXTRA_WHITESPACE.matcher(cleaned).replaceAll(" ");
        
        return cleaned.trim();
    }

    private String expandAcronyms(String query) {
        String expanded = query;
        
        for (Map.Entry<String, String> entry : ACRONYM_EXPANSIONS.entrySet()) {
            String acronym = entry.getKey();
            String expansion = entry.getValue();
            
            // Replace whole word acronyms (case insensitive)
            expanded = expanded.replaceAll("\\b" + Pattern.quote(acronym) + "\\b", 
                                         expansion + " (" + acronym + ")");
        }
        
        return expanded;
    }

    private String removeStopwords(String query) {
        return Arrays.stream(query.split("\\s+"))
            .filter(word -> !STOP_WORDS.contains(word.toLowerCase()))
            .collect(Collectors.joining(" "));
    }

    private boolean containsOnlyStopWords(String query) {
        String[] words = query.toLowerCase().split("\\s+");
        return Arrays.stream(words).allMatch(STOP_WORDS::contains);
    }

    private boolean containsUnexpandedAcronyms(String query) {
        String upperQuery = query.toUpperCase();
        return ACRONYM_EXPANSIONS.keySet().stream()
            .anyMatch(acronym -> upperQuery.contains(acronym));
    }

    private boolean containsTypos(String query) {
        // Simple heuristic: look for unusual character patterns
        return query.matches(".*[a-z]{2}[A-Z].*") || // camelCase issues
               query.matches(".*\\d[a-zA-Z].*") ||    // numbers touching letters
               query.contains("..") ||                // double dots
               query.matches(".*[a-zA-Z]{15,}.*");    // very long words
    }

    private QueryComplexity analyzeComplexity(String query) {
        int words = countWords(query);
        int sentences = query.split("[.!?]+").length;
        boolean hasConjunctions = query.toLowerCase().matches(".*(and|or|but|however|therefore).*");

        if (words < 3) return QueryComplexity.SIMPLE;
        if (words < 8 && sentences == 1 && !hasConjunctions) return QueryComplexity.MODERATE;
        if (words < 15 && sentences <= 2) return QueryComplexity.COMPLEX;
        return QueryComplexity.VERY_COMPLEX;
    }

    private int countWords(String query) {
        if (query == null || query.trim().isEmpty()) {
            return 0;
        }
        return query.trim().split("\\s+").length;
    }

    private RagQueryRequest createOptimizedRequest(RagQueryRequest original, String optimizedQuery) {
        return new RagQueryRequest(
            original.tenantId(),
            optimizedQuery,
            original.conversationId(),
            original.userId(),
            original.sessionId(),
            original.documentIds(),
            original.filters(),
            original.options()
        );
    }

    /**
     * Query complexity levels.
     */
    public enum QueryComplexity {
        SIMPLE,
        MODERATE, 
        COMPLEX,
        VERY_COMPLEX
    }

    /**
     * Query analysis result.
     */
    public record QueryAnalysis(
        String originalQuery,
        int characterCount,
        int wordCount,
        QueryComplexity complexity,
        List<String> issues,
        List<String> suggestions,
        List<String> keyTerms
    ) {
        public static QueryAnalysis empty() {
            return new QueryAnalysis("", 0, 0, QueryComplexity.SIMPLE, List.of(), List.of(), List.of());
        }
    }

    /**
     * Query optimization statistics.
     */
    public record OptimizationStats(
        int totalQueries,
        int optimizedQueries,
        int acronymsExpanded,
        int stopwordsRemoved,
        double averageOptimizationTimeMs
    ) {}
}
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
 * Service for optimizing RAG queries for better retrieval and response quality.
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
     * Optimize the incoming RAG query request.
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
     * Analyze query to suggest improvements.
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
     * Extract key terms from query for highlighting.
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
     * Suggest alternative query phrasings.
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
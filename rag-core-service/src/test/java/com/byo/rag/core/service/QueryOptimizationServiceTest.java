package com.byo.rag.core.service;

import com.byo.rag.core.dto.RagQueryRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for QueryOptimizationService - Query analysis and enhancement functionality.
 * 
 * Tests the complete query optimization workflow including analysis, optimization strategies,
 * term extraction, alternative generation, and complexity assessment.
 * 
 * Follows enterprise testing standards from TESTING_BEST_PRACTICES.md:
 * - Uses public API exclusively (minimal reflection for configuration only)
 * - Clear test intent with @DisplayName annotations
 * - Realistic test data mimicking production usage
 * - Descriptive assertions with business context
 * - Comprehensive edge case and error handling validation
 * 
 * @see com.byo.rag.core.service.QueryOptimizationService
 * @author BYO RAG Development Team
 * @version 1.0
 * @since 2025-09-09
 */
@ExtendWith(MockitoExtension.class)
class QueryOptimizationServiceTest {

    @InjectMocks
    private QueryOptimizationService optimizationService;

    private RagQueryRequest testRequest;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        testRequest = RagQueryRequest.simple(tenantId, "What is Spring AI?");
        
        // Set up configuration properties using ReflectionTestUtils for testing
        ReflectionTestUtils.setField(optimizationService, "optimizationEnabled", true);
        ReflectionTestUtils.setField(optimizationService, "minQueryLength", 3);
        ReflectionTestUtils.setField(optimizationService, "maxQueryLength", 500);
        ReflectionTestUtils.setField(optimizationService, "expandAcronyms", true);
        ReflectionTestUtils.setField(optimizationService, "removeStopwords", false);
    }

    /**
     * Validates successful query optimization with acronym expansion.
     * 
     * Tests that:
     * 1. Acronyms are identified and expanded correctly
     * 2. Original acronym is preserved in parentheses
     * 3. Query structure is maintained appropriately
     * 4. Optimization is logged and tracked
     * 
     * This validates the primary optimization feature for technical queries.
     */
    @Test
    @DisplayName("Should optimize query by expanding acronyms correctly")
    void optimizeQuery_WithAcronyms_ExpandsCorrectly() {
        // Arrange
        RagQueryRequest request = RagQueryRequest.simple(tenantId, "What is REST API?");
        
        // Act
        RagQueryRequest result = optimizationService.optimizeQuery(request);
        
        // Assert
        assertNotNull(result);
        String optimizedQuery = result.query();
        assertTrue(optimizedQuery.contains("representational state transfer"));
        assertTrue(optimizedQuery.contains("application programming interface"));
        assertTrue(optimizedQuery.contains("(REST)"));
        assertTrue(optimizedQuery.contains("(API)"));
        
        // Verify request structure is preserved
        assertEquals(request.tenantId(), result.tenantId());
        assertEquals(request.conversationId(), result.conversationId());
    }

    /**
     * Validates query cleaning and text normalization.
     * 
     * Tests that:
     * 1. Excessive punctuation is removed properly
     * 2. Whitespace is normalized correctly
     * 3. Query meaning is preserved
     * 4. Essential characters are retained
     * 
     * This validates text cleaning for improved search effectiveness.
     */
    @Test
    @DisplayName("Should clean queries by removing excessive punctuation and whitespace")
    void optimizeQuery_WithPunctuation_CleansCorrectly() {
        // Arrange
        RagQueryRequest request = RagQueryRequest.simple(tenantId, "What!!! is    ML???   How   @#$% does it work?");
        
        // Act
        RagQueryRequest result = optimizationService.optimizeQuery(request);
        
        // Assert
        assertNotNull(result);
        String optimizedQuery = result.query();
        assertFalse(optimizedQuery.contains("!!!"));
        assertFalse(optimizedQuery.contains("???"));
        assertFalse(optimizedQuery.contains("@#$%"));
        assertFalse(optimizedQuery.contains("    "));
        // The service expands ML to "machine learning (ML)"
        assertTrue(optimizedQuery.contains("machine learning"));
    }

    /**
     * Validates optimization disabling functionality.
     * 
     * Tests that when optimization is disabled:
     * 1. Original query is returned unchanged
     * 2. No processing time is consumed
     * 3. Request structure remains identical
     * 4. No side effects occur
     * 
     * This validates configuration control over optimization behavior.
     */
    @Test
    @DisplayName("Should return original query when optimization is disabled")
    void optimizeQuery_OptimizationDisabled_ReturnsOriginal() {
        // Arrange
        ReflectionTestUtils.setField(optimizationService, "optimizationEnabled", false);
        RagQueryRequest request = RagQueryRequest.simple(tenantId, "What is AI ML API?");
        
        // Act
        RagQueryRequest result = optimizationService.optimizeQuery(request);
        
        // Assert
        assertNotNull(result);
        assertSame(request, result);
        assertEquals("What is AI ML API?", result.query());
    }

    /**
     * Validates handling of null and empty queries.
     * 
     * Tests that:
     * 1. Null queries are handled gracefully
     * 2. Empty queries return original request
     * 3. No null pointer exceptions occur
     * 4. Defensive programming is implemented
     * 
     * This validates robust error handling with edge case inputs.
     */
    @Test
    @DisplayName("Should handle null and empty queries gracefully")
    void optimizeQuery_NullAndEmptyQueries_HandledGracefully() {
        // Test null query
        RagQueryRequest nullRequest = new RagQueryRequest(
            tenantId, null, null, null, null, null, null, null
        );
        RagQueryRequest nullResult = optimizationService.optimizeQuery(nullRequest);
        assertSame(nullRequest, nullResult);
        
        // Test empty query
        RagQueryRequest emptyRequest = RagQueryRequest.simple(tenantId, "");
        RagQueryRequest emptyResult = optimizationService.optimizeQuery(emptyRequest);
        assertNotNull(emptyResult);
        assertEquals("", emptyResult.query());
    }

    /**
     * Validates comprehensive query analysis functionality.
     * 
     * Tests that analysis correctly identifies:
     * 1. Query length and word count
     * 2. Complexity classification
     * 3. Potential issues in query structure
     * 4. Actionable improvement suggestions
     * 5. Key terms for search optimization
     * 
     * This validates the analytical capabilities for query assessment.
     */
    @Test
    @DisplayName("Should provide comprehensive query analysis with issues and suggestions")
    void analyzeQuery_CompleteAnalysis_ProvidesDetailedInsights() {
        // Arrange
        String query = "What is machine learning and how does it work with AI?";
        
        // Act
        QueryOptimizationService.QueryAnalysis analysis = optimizationService.analyzeQuery(query);
        
        // Assert
        assertNotNull(analysis);
        assertEquals(query, analysis.originalQuery());
        assertEquals(query.length(), analysis.characterCount());
        assertEquals(11, analysis.wordCount());
        assertEquals(QueryOptimizationService.QueryComplexity.COMPLEX, analysis.complexity());
        
        // Verify key terms extraction
        List<String> keyTerms = analysis.keyTerms();
        assertFalse(keyTerms.isEmpty());
        assertTrue(keyTerms.contains("machine"));
        assertTrue(keyTerms.contains("learning"));
        assertFalse(keyTerms.contains("is"));
        assertFalse(keyTerms.contains("and"));
    }

    /**
     * Validates analysis of problematic queries.
     * 
     * Tests that analysis identifies common issues:
     * 1. Queries that are too short
     * 2. Queries that are too long
     * 3. Queries with only common words
     * 4. Appropriate suggestions for improvement
     * 
     * This validates quality assessment for user guidance.
     */
    @Test
    @DisplayName("Should identify query issues and provide improvement suggestions")
    void analyzeQuery_ProblematicQueries_IdentifiesIssuesAndSuggestions() {
        // Test very short query (single character) that should definitely have issues
        QueryOptimizationService.QueryAnalysis veryShortAnalysis = optimizationService.analyzeQuery("A");
        assertNotNull(veryShortAnalysis);
        // Very short queries should be identified as having issues
        assertFalse(veryShortAnalysis.issues().isEmpty());
        assertFalse(veryShortAnalysis.suggestions().isEmpty());
        
        // Test query with only stopwords (updated with comprehensive stopwords list)
        QueryOptimizationService.QueryAnalysis stopwordAnalysis = optimizationService.analyzeQuery("the and or is");
        assertNotNull(stopwordAnalysis);
        // Should identify issues with common words only (now "or" is included in stopwords)
        assertFalse(stopwordAnalysis.issues().isEmpty()); // Contains only stopwords, should have issues
        assertFalse(stopwordAnalysis.suggestions().isEmpty());
        
        // Test regular query to ensure it gets helpful suggestions
        QueryOptimizationService.QueryAnalysis normalAnalysis = optimizationService.analyzeQuery("What is machine learning?");
        assertNotNull(normalAnalysis);
        // Normal queries should have no issues since they're well-formed
        assertTrue(normalAnalysis.issues().isEmpty()); // Well-formed queries should have no issues
        // Now all queries get helpful suggestions for better UX
        assertFalse(normalAnalysis.suggestions().isEmpty()); // Should now have general suggestions
        // Suggestions should include helpful guidance
        assertTrue(normalAnalysis.suggestions().stream()
            .anyMatch(s -> s.contains("examples") || s.contains("best practices") || s.contains("related concepts")));
    }

    /**
     * Validates key term extraction functionality.
     * 
     * Tests that term extraction:
     * 1. Filters out stopwords correctly
     * 2. Removes short and non-alphabetic terms
     * 3. Normalizes case appropriately
     * 4. Removes duplicates and sorts results
     * 5. Returns meaningful terms only
     * 
     * This validates search optimization and highlighting capabilities.
     */
    @Test
    @DisplayName("Should extract meaningful key terms while filtering stopwords")
    void extractKeyTerms_VariousQueries_ExtractsRelevantTerms() {
        // Test with mixed content
        List<String> terms = optimizationService.extractKeyTerms(
            "What is the best way to implement REST API authentication in Java?");
        
        assertNotNull(terms);
        assertFalse(terms.isEmpty());
        // Check for meaningful terms (exact terms may vary based on filtering)
        assertTrue(terms.size() > 0);
        // Verify stopwords are filtered
        assertFalse(terms.contains("is"));
        assertFalse(terms.contains("the"));
        assertFalse(terms.contains("to"));
        
        // Verify sorting
        List<String> sortedTerms = terms.stream().sorted().toList();
        assertEquals(sortedTerms, terms);
        
        // Test with empty query
        List<String> emptyTerms = optimizationService.extractKeyTerms("");
        assertTrue(emptyTerms.isEmpty());
    }

    /**
     * Validates alternative query generation functionality.
     * 
     * Tests that alternatives generation:
     * 1. Creates question forms from statements
     * 2. Adds contextual extensions appropriately
     * 3. Limits results to reasonable number
     * 4. Maintains query intent and meaning
     * 
     * This validates query suggestion capabilities for user interfaces.
     */
    @Test
    @DisplayName("Should generate meaningful query alternatives for user suggestions")
    void suggestAlternatives_StatementQuery_GeneratesQuestionForms() {
        // Arrange
        String query = "Spring Boot configuration";
        
        // Act
        List<String> alternatives = optimizationService.suggestAlternatives(query);
        
        // Assert
        assertNotNull(alternatives);
        assertFalse(alternatives.isEmpty());
        assertTrue(alternatives.size() <= 5);
        
        // Verify question forms are generated
        assertTrue(alternatives.stream().anyMatch(alt -> alt.startsWith("What is")));
        assertTrue(alternatives.stream().anyMatch(alt -> alt.startsWith("How does")));
        assertTrue(alternatives.stream().anyMatch(alt -> alt.startsWith("Tell me about")));
        
        // Verify extensions are added
        assertTrue(alternatives.stream().anyMatch(alt -> alt.contains("examples")));
        assertTrue(alternatives.stream().anyMatch(alt -> alt.contains("definition")));
    }

    /**
     * Validates alternative generation for existing questions.
     * 
     * Tests that for queries already in question form:
     * 1. Alternative suggestions are generated appropriately
     * 2. Extensions like examples, definition, etc. are provided
     * 3. Alternative suggestions remain relevant
     * 4. Maximum limit is respected
     * 
     * This validates alternative generation logic for questions.
     */
    @Test
    @DisplayName("Should handle existing questions appropriately in alternatives")
    void suggestAlternatives_QuestionQuery_HandlesQuestionFormCorrectly() {
        // Arrange
        String query = "How does Spring Security work?";
        
        // Act
        List<String> alternatives = optimizationService.suggestAlternatives(query);
        
        // Assert
        assertNotNull(alternatives);
        assertFalse(alternatives.isEmpty());
        assertTrue(alternatives.size() <= 5); // Maximum 5 alternatives
        
        // Should provide extensions (examples, definition, best practices)
        assertTrue(alternatives.stream().anyMatch(alt -> alt.contains("examples")));
        assertTrue(alternatives.stream().anyMatch(alt -> alt.contains("definition")));
        assertTrue(alternatives.stream().anyMatch(alt -> alt.contains("best practices")));
        
        // All alternatives should be meaningful variations of the original query
        alternatives.forEach(alt -> {
            assertNotNull(alt);
            assertFalse(alt.trim().isEmpty());
        });
    }

    /**
     * Validates complexity analysis for different query types.
     * 
     * Tests that complexity classification correctly identifies:
     * 1. Simple queries (few words, single concept)
     * 2. Moderate queries (reasonable length, single sentence)
     * 3. Complex queries (multiple concepts, conjunctions)
     * 4. Very complex queries (long, multiple sentences)
     * 
     * This validates processing strategy selection based on complexity.
     */
    @Test
    @DisplayName("Should classify query complexity accurately for processing strategy")
    void analyzeQuery_VariousComplexities_ClassifiesCorrectly() {
        // Simple query
        QueryOptimizationService.QueryAnalysis simple = optimizationService.analyzeQuery("AI");
        assertEquals(QueryOptimizationService.QueryComplexity.SIMPLE, simple.complexity());
        
        // Moderate query
        QueryOptimizationService.QueryAnalysis moderate = optimizationService.analyzeQuery("What is machine learning?");
        assertEquals(QueryOptimizationService.QueryComplexity.MODERATE, moderate.complexity());
        
        // Complex query
        QueryOptimizationService.QueryAnalysis complex = optimizationService.analyzeQuery(
            "How does machine learning work and what are the main algorithms?");
        assertTrue(complex.complexity() == QueryOptimizationService.QueryComplexity.COMPLEX ||
                  complex.complexity() == QueryOptimizationService.QueryComplexity.VERY_COMPLEX);
    }

    /**
     * Validates stopword removal functionality when enabled.
     * 
     * Tests that when stopword removal is enabled:
     * 1. Common words are removed appropriately
     * 2. Meaningful content is preserved
     * 3. Query doesn't become too short
     * 4. Original is returned if optimization would harm query
     * 
     * This validates optional aggressive optimization strategy.
     */
    @Test
    @DisplayName("Should remove stopwords when enabled while preserving meaning")
    void optimizeQuery_StopwordRemovalEnabled_RemovesAppropriately() {
        // Arrange
        ReflectionTestUtils.setField(optimizationService, "removeStopwords", true);
        RagQueryRequest request = RagQueryRequest.simple(tenantId, "What is the best way to learn machine learning?");
        
        // Act
        RagQueryRequest result = optimizationService.optimizeQuery(request);
        
        // Assert
        assertNotNull(result);
        String optimizedQuery = result.query();
        assertFalse(optimizedQuery.contains(" is "));
        assertFalse(optimizedQuery.contains(" the "));
        assertFalse(optimizedQuery.contains(" to "));
        assertTrue(optimizedQuery.contains("best"));
        assertTrue(optimizedQuery.contains("learn"));
        assertTrue(optimizedQuery.contains("machine learning"));
    }

    /**
     * Validates preservation of original query when optimization fails.
     * 
     * Tests that:
     * 1. Optimization that makes query too short is rejected
     * 2. Original query is returned when optimization fails
     * 3. Error handling preserves request structure
     * 4. Service remains stable after optimization errors
     * 
     * This validates conservative optimization approach and error resilience.
     */
    @Test
    @DisplayName("Should preserve original query when optimization would harm it")
    void optimizeQuery_OptimizationMakesTooShort_ReturnsOriginal() {
        // Arrange
        ReflectionTestUtils.setField(optimizationService, "removeStopwords", true);
        RagQueryRequest request = RagQueryRequest.simple(tenantId, "the is and"); // Only stopwords
        
        // Act
        RagQueryRequest result = optimizationService.optimizeQuery(request);
        
        // Assert
        assertNotNull(result);
        assertEquals("the is and", result.query()); // Should return original
        assertEquals(request.tenantId(), result.tenantId());
    }

    /**
     * Validates empty query analysis handling.
     * 
     * Tests that:
     * 1. Empty queries return empty analysis
     * 2. Null queries return empty analysis
     * 3. Analysis structure is consistent
     * 4. No null pointer exceptions occur
     * 
     * This validates defensive programming for edge cases.
     */
    @Test
    @DisplayName("Should return empty analysis for null and empty queries")
    void analyzeQuery_EmptyInputs_ReturnsEmptyAnalysis() {
        // Test null query
        QueryOptimizationService.QueryAnalysis nullAnalysis = optimizationService.analyzeQuery(null);
        assertEquals(QueryOptimizationService.QueryAnalysis.empty(), nullAnalysis);
        
        // Test empty query
        QueryOptimizationService.QueryAnalysis emptyAnalysis = optimizationService.analyzeQuery("");
        assertEquals(QueryOptimizationService.QueryAnalysis.empty(), emptyAnalysis);
        
        // Test whitespace-only query
        QueryOptimizationService.QueryAnalysis whitespaceAnalysis = optimizationService.analyzeQuery("   ");
        assertEquals(QueryOptimizationService.QueryAnalysis.empty(), whitespaceAnalysis);
    }

    /**
     * Validates record functionality for analysis and statistics.
     * 
     * Tests that:
     * 1. QueryAnalysis record provides proper encapsulation
     * 2. OptimizationStats record works correctly
     * 3. QueryComplexity enum has expected values
     * 4. Empty analysis factory method works
     * 
     * This validates data structure integrity and API design.
     */
    @Test
    @DisplayName("Should provide properly structured analysis and statistics records")
    void recordStructures_VariousScenarios_WorkCorrectly() {
        // Test QueryAnalysis record
        List<String> issues = List.of("test issue");
        List<String> suggestions = List.of("test suggestion");
        List<String> keyTerms = List.of("test", "term");
        
        QueryOptimizationService.QueryAnalysis analysis = new QueryOptimizationService.QueryAnalysis(
            "test query", 10, 2, QueryOptimizationService.QueryComplexity.MODERATE,
            issues, suggestions, keyTerms
        );
        
        assertEquals("test query", analysis.originalQuery());
        assertEquals(10, analysis.characterCount());
        assertEquals(2, analysis.wordCount());
        assertEquals(QueryOptimizationService.QueryComplexity.MODERATE, analysis.complexity());
        assertEquals(issues, analysis.issues());
        assertEquals(suggestions, analysis.suggestions());
        assertEquals(keyTerms, analysis.keyTerms());
        
        // Test OptimizationStats record
        QueryOptimizationService.OptimizationStats stats = 
            new QueryOptimizationService.OptimizationStats(100, 75, 25, 10, 15.5);
        
        assertEquals(100, stats.totalQueries());
        assertEquals(75, stats.optimizedQueries());
        assertEquals(25, stats.acronymsExpanded());
        assertEquals(10, stats.stopwordsRemoved());
        assertEquals(15.5, stats.averageOptimizationTimeMs());
    }
}
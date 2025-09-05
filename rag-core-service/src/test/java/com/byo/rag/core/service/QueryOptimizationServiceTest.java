package com.byo.rag.core.service;

import com.byo.rag.core.dto.RagQueryRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for QueryOptimizationService.
 */
@ExtendWith(MockitoExtension.class)
class QueryOptimizationServiceTest {

    private QueryOptimizationService queryOptimizationService;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        queryOptimizationService = new QueryOptimizationService();
        tenantId = UUID.randomUUID();
        
        // Set properties using reflection to simulate @Value injection
        ReflectionTestUtils.setField(queryOptimizationService, "optimizationEnabled", true);
        ReflectionTestUtils.setField(queryOptimizationService, "minQueryLength", 3);
        ReflectionTestUtils.setField(queryOptimizationService, "maxQueryLength", 500);
        ReflectionTestUtils.setField(queryOptimizationService, "expandAcronyms", true);
        ReflectionTestUtils.setField(queryOptimizationService, "removeStopwords", false);
    }

    @Test
    void optimizeQuery_ValidQuery_ReturnsOptimizedQuery() {
        RagQueryRequest request = RagQueryRequest.simple(tenantId, "What is AI?");

        RagQueryRequest result = queryOptimizationService.optimizeQuery(request);

        assertNotNull(result);
        assertTrue(result.query().contains("artificial intelligence"));
        assertEquals(tenantId, result.tenantId());
    }

    @Test
    void optimizeQuery_NullQuery_ReturnsOriginalRequest() {
        RagQueryRequest request = new RagQueryRequest(
            tenantId,
            null, // null query
            "conv-123",
            UUID.randomUUID().toString(),
            "session-123", 
            null,
            null,
            null
        );

        RagQueryRequest result = queryOptimizationService.optimizeQuery(request);

        assertEquals(request, result);
    }

    @Test
    void optimizeQuery_OptimizationDisabled_ReturnsOriginalRequest() {
        ReflectionTestUtils.setField(queryOptimizationService, "optimizationEnabled", false);
        RagQueryRequest request = RagQueryRequest.simple(tenantId, "What is AI?");

        RagQueryRequest result = queryOptimizationService.optimizeQuery(request);

        assertEquals(request, result);
        assertFalse(result.query().contains("artificial intelligence"));
    }

    @Test
    void analyzeQuery_ValidQuery_ReturnsAnalysis() {
        String query = "What is machine learning?";

        QueryOptimizationService.QueryAnalysis analysis = queryOptimizationService.analyzeQuery(query);

        assertNotNull(analysis);
        assertEquals(query, analysis.originalQuery());
        assertEquals(query.length(), analysis.characterCount());
        assertEquals(4, analysis.wordCount());
        assertEquals(QueryOptimizationService.QueryComplexity.MODERATE, analysis.complexity());
        assertNotNull(analysis.keyTerms());
    }

    @Test
    void analyzeQuery_EmptyQuery_ReturnsEmptyAnalysis() {
        QueryOptimizationService.QueryAnalysis analysis = queryOptimizationService.analyzeQuery("");

        assertEquals(QueryOptimizationService.QueryAnalysis.empty(), analysis);
    }

    @Test
    void analyzeQuery_ShortQuery_IdentifiesIssue() {
        String query = "AI";

        QueryOptimizationService.QueryAnalysis analysis = queryOptimizationService.analyzeQuery(query);

        assertFalse(analysis.issues().isEmpty());
        assertTrue(analysis.issues().get(0).contains("very short"));
        assertFalse(analysis.suggestions().isEmpty());
    }

    @Test
    void analyzeQuery_LongQuery_IdentifiesIssue() {
        String longQuery = "a".repeat(600);

        QueryOptimizationService.QueryAnalysis analysis = queryOptimizationService.analyzeQuery(longQuery);

        assertFalse(analysis.issues().isEmpty());
        assertTrue(analysis.issues().get(0).contains("very long"));
    }

    @Test
    void analyzeQuery_OnlyStopWords_IdentifiesIssue() {
        String query = "the and of in";

        QueryOptimizationService.QueryAnalysis analysis = queryOptimizationService.analyzeQuery(query);

        assertFalse(analysis.issues().isEmpty());
        assertTrue(analysis.issues().get(0).contains("common words"));
    }

    @Test
    void extractKeyTerms_ValidQuery_ReturnsKeyTerms() {
        String query = "What is machine learning and artificial intelligence?";

        List<String> keyTerms = queryOptimizationService.extractKeyTerms(query);

        assertNotNull(keyTerms);
        assertFalse(keyTerms.isEmpty());
        assertTrue(keyTerms.contains("machine"));
        assertTrue(keyTerms.contains("learning"));
        assertTrue(keyTerms.contains("artificial"));
        assertTrue(keyTerms.contains("intelligence"));
        // Should not contain stop words
        assertFalse(keyTerms.contains("what"));
        assertFalse(keyTerms.contains("and"));
    }

    @Test
    void extractKeyTerms_EmptyQuery_ReturnsEmptyList() {
        List<String> keyTerms = queryOptimizationService.extractKeyTerms("");

        assertTrue(keyTerms.isEmpty());
    }

    @Test
    void extractKeyTerms_OnlyStopWords_ReturnsEmptyList() {
        String query = "the and of in";

        List<String> keyTerms = queryOptimizationService.extractKeyTerms(query);

        assertTrue(keyTerms.isEmpty());
    }

    @Test
    void suggestAlternatives_ValidQuery_ReturnsSuggestions() {
        String query = "Spring AI framework";

        List<String> suggestions = queryOptimizationService.suggestAlternatives(query);

        assertNotNull(suggestions);
        assertFalse(suggestions.isEmpty());
        assertTrue(suggestions.size() <= 5);
        assertTrue(suggestions.stream().anyMatch(s -> s.contains("What is")));
        assertTrue(suggestions.stream().anyMatch(s -> s.contains("examples")));
    }

    @Test
    void suggestAlternatives_QueryWithQuestionMark_ReturnsAppropriateAlternatives() {
        String query = "What is Spring AI?";

        List<String> suggestions = queryOptimizationService.suggestAlternatives(query);

        assertNotNull(suggestions);
        // Should not add "What is" prefix if already a question
        assertFalse(suggestions.stream().anyMatch(s -> s.contains("What is What is")));
    }

    @Test
    void suggestAlternatives_EmptyQuery_ReturnsEmptyList() {
        List<String> suggestions = queryOptimizationService.suggestAlternatives("");

        assertTrue(suggestions.isEmpty());
    }

    @Test
    void suggestAlternatives_NullQuery_ReturnsEmptyList() {
        List<String> suggestions = queryOptimizationService.suggestAlternatives(null);

        assertTrue(suggestions.isEmpty());
    }

    @Test
    void analyzeComplexity_SimpleQuery_ReturnsSimple() {
        String query = "AI";

        QueryOptimizationService.QueryAnalysis analysis = queryOptimizationService.analyzeQuery(query);

        assertEquals(QueryOptimizationService.QueryComplexity.SIMPLE, analysis.complexity());
    }

    @Test
    void analyzeComplexity_ModerateQuery_ReturnsModerate() {
        String query = "What is machine learning";

        QueryOptimizationService.QueryAnalysis analysis = queryOptimizationService.analyzeQuery(query);

        assertEquals(QueryOptimizationService.QueryComplexity.MODERATE, analysis.complexity());
    }

    @Test
    void analyzeComplexity_ComplexQuery_ReturnsComplex() {
        String query = "How does machine learning differ from traditional programming approaches";

        QueryOptimizationService.QueryAnalysis analysis = queryOptimizationService.analyzeQuery(query);

        assertEquals(QueryOptimizationService.QueryComplexity.COMPLEX, analysis.complexity());
    }

    @Test
    void analyzeComplexity_VeryComplexQuery_ReturnsVeryComplex() {
        String query = "Explain the differences between supervised and unsupervised machine learning algorithms. How do they work?";

        QueryOptimizationService.QueryAnalysis analysis = queryOptimizationService.analyzeQuery(query);

        assertEquals(QueryOptimizationService.QueryComplexity.VERY_COMPLEX, analysis.complexity());
    }
}
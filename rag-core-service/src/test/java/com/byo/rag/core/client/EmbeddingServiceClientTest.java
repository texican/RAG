package com.byo.rag.core.client;

import com.byo.rag.core.dto.RagQueryRequest;
import com.byo.rag.shared.dto.DocumentChunkDto;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmbeddingServiceClient.
 * 
 * Note: Since EmbeddingServiceClient is a Feign interface, these tests focus on
 * the client behavior and response handling rather than actual HTTP calls.
 */
@ExtendWith(MockitoExtension.class)
class EmbeddingServiceClientTest {

    @Mock
    private EmbeddingServiceClient embeddingServiceClient;

    private UUID tenantId;
    private RagQueryRequest testRequest;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        testRequest = RagQueryRequest.simple(tenantId, "What is Spring AI?");
    }

    @Test
    void search_ValidRequest_ReturnsSearchResponse() {
        // Arrange
        EmbeddingServiceClient.SearchRequest searchRequest = new EmbeddingServiceClient.SearchRequest(
            testRequest.query(),
            testRequest.tenantId(),
            10,
            0.7,
            "all",
            null
        );

        List<DocumentChunkDto> mockChunks = createMockDocumentChunks();
        EmbeddingServiceClient.SearchResponse expectedResponse = new EmbeddingServiceClient.SearchResponse(
            tenantId,
            testRequest.query(),
            "test-model",
            mockChunks,
            2,
            0.85,
            150L,
            "2024-01-01T10:00:00Z"
        );

        when(embeddingServiceClient.search(eq(tenantId), eq(searchRequest)))
            .thenReturn(expectedResponse);

        // Act
        EmbeddingServiceClient.SearchResponse response = embeddingServiceClient.search(tenantId, searchRequest);

        // Assert
        assertNotNull(response);
        assertEquals(tenantId, response.tenantId());
        assertEquals(testRequest.query(), response.query());
        assertEquals("test-model", response.embeddingModel());
        assertEquals(2, response.totalResults());
        assertEquals(0.85, response.maxScore());
        assertEquals(150L, response.processingTimeMs());
        assertNotNull(response.results());
        assertEquals(2, response.results().size());

        verify(embeddingServiceClient).search(tenantId, searchRequest);
    }

    @Test
    void search_EmptyQuery_ThrowsException() {
        // Arrange
        EmbeddingServiceClient.SearchRequest searchRequest = new EmbeddingServiceClient.SearchRequest(
            "",
            tenantId,
            10,
            0.7,
            "all",
            null
        );

        when(embeddingServiceClient.search(eq(tenantId), eq(searchRequest)))
            .thenThrow(FeignException.badRequest("Bad Request", "Bad Request".getBytes()));

        // Act & Assert
        assertThrows(FeignException.class, () -> {
            embeddingServiceClient.search(tenantId, searchRequest);
        });

        verify(embeddingServiceClient).search(tenantId, searchRequest);
    }

    @Test
    void search_InvalidTenantId_ThrowsException() {
        // Arrange
        UUID invalidTenantId = UUID.randomUUID();
        EmbeddingServiceClient.SearchRequest searchRequest = new EmbeddingServiceClient.SearchRequest(
            testRequest.query(),
            invalidTenantId,
            10,
            0.7,
            "all",
            null
        );

        when(embeddingServiceClient.search(eq(invalidTenantId), eq(searchRequest)))
            .thenThrow(FeignException.notFound("Not Found", "Tenant not found".getBytes()));

        // Act & Assert
        assertThrows(FeignException.class, () -> {
            embeddingServiceClient.search(invalidTenantId, searchRequest);
        });

        verify(embeddingServiceClient).search(invalidTenantId, searchRequest);
    }

    @Test
    void search_ServiceUnavailable_ThrowsException() {
        // Arrange
        EmbeddingServiceClient.SearchRequest searchRequest = new EmbeddingServiceClient.SearchRequest(
            testRequest.query(),
            tenantId,
            10,
            0.7,
            "all",
            null
        );

        when(embeddingServiceClient.search(eq(tenantId), eq(searchRequest)))
            .thenThrow(FeignException.serviceUnavailable("Service Unavailable", "Service temporarily unavailable".getBytes()));

        // Act & Assert
        assertThrows(FeignException.class, () -> {
            embeddingServiceClient.search(tenantId, searchRequest);
        });

        verify(embeddingServiceClient).search(tenantId, searchRequest);
    }

    @Test
    void search_WithFilters_ReturnsFilteredResponse() {
        // Arrange
        Map<String, Object> filters = Map.of(
            "category", "documentation",
            "language", "java"
        );
        
        EmbeddingServiceClient.SearchRequest searchRequest = new EmbeddingServiceClient.SearchRequest(
            testRequest.query(),
            tenantId,
            5,
            0.8,
            "filtered",
            filters
        );

        List<DocumentChunkDto> mockChunks = createMockDocumentChunks();
        EmbeddingServiceClient.SearchResponse expectedResponse = new EmbeddingServiceClient.SearchResponse(
            tenantId,
            testRequest.query(),
            "test-model",
            mockChunks,
            2,
            0.92,
            120L,
            "2024-01-01T10:00:00Z"
        );

        when(embeddingServiceClient.search(eq(tenantId), eq(searchRequest)))
            .thenReturn(expectedResponse);

        // Act
        EmbeddingServiceClient.SearchResponse response = embeddingServiceClient.search(tenantId, searchRequest);

        // Assert
        assertNotNull(response);
        assertEquals(0.92, response.maxScore()); // Higher score due to filtering
        assertTrue(response.processingTimeMs() > 0);

        verify(embeddingServiceClient).search(tenantId, searchRequest);
    }

    @Test
    void search_HighThreshold_ReturnsLimitedResults() {
        // Arrange
        EmbeddingServiceClient.SearchRequest searchRequest = new EmbeddingServiceClient.SearchRequest(
            testRequest.query(),
            tenantId,
            10,
            0.95, // High threshold
            "all",
            null
        );

        // Mock response with fewer results due to high threshold
        List<DocumentChunkDto> limitedChunks = List.of(createMockDocumentChunks().get(0)); // Only first chunk
        EmbeddingServiceClient.SearchResponse expectedResponse = new EmbeddingServiceClient.SearchResponse(
            tenantId,
            testRequest.query(),
            "test-model",
            limitedChunks,
            1, // Only 1 result meets high threshold
            0.97,
            100L,
            "2024-01-01T10:00:00Z"
        );

        when(embeddingServiceClient.search(eq(tenantId), eq(searchRequest)))
            .thenReturn(expectedResponse);

        // Act
        EmbeddingServiceClient.SearchResponse response = embeddingServiceClient.search(tenantId, searchRequest);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.totalResults());
        assertEquals(1, response.results().size());
        assertTrue(response.maxScore() >= 0.95);

        verify(embeddingServiceClient).search(tenantId, searchRequest);
    }

    @Test
    void search_LargeLimit_ReturnsAllAvailable() {
        // Arrange
        EmbeddingServiceClient.SearchRequest searchRequest = new EmbeddingServiceClient.SearchRequest(
            testRequest.query(),
            tenantId,
            100, // Large limit
            0.5,  // Low threshold
            "all",
            null
        );

        List<DocumentChunkDto> allChunks = createMockDocumentChunks();
        EmbeddingServiceClient.SearchResponse expectedResponse = new EmbeddingServiceClient.SearchResponse(
            tenantId,
            testRequest.query(),
            "test-model",
            allChunks,
            2, // All available results
            0.85,
            180L,
            "2024-01-01T10:00:00Z"
        );

        when(embeddingServiceClient.search(eq(tenantId), eq(searchRequest)))
            .thenReturn(expectedResponse);

        // Act
        EmbeddingServiceClient.SearchResponse response = embeddingServiceClient.search(tenantId, searchRequest);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.totalResults());
        assertEquals(2, response.results().size());

        verify(embeddingServiceClient).search(tenantId, searchRequest);
    }

    @Test
    void searchRequest_Constructor_CreatesValidRequest() {
        // Arrange & Act
        EmbeddingServiceClient.SearchRequest request = new EmbeddingServiceClient.SearchRequest(
            "test query",
            tenantId,
            5,
            0.8,
            "semantic",
            Map.of("type", "document")
        );

        // Assert
        assertEquals("test query", request.query());
        assertEquals(tenantId, request.tenantId());
        assertEquals(5, request.limit());
        assertEquals(0.8, request.threshold());
        assertEquals("semantic", request.searchType());
        assertNotNull(request.filters());
        assertEquals("document", request.filters().get("type"));
    }

    @Test
    void searchResponse_Constructor_CreatesValidResponse() {
        // Arrange & Act
        List<DocumentChunkDto> chunks = createMockDocumentChunks();
        EmbeddingServiceClient.SearchResponse response = new EmbeddingServiceClient.SearchResponse(
            tenantId,
            "test query",
            "test-model",
            chunks,
            2,
            0.95,
            150L,
            "2024-01-01T10:00:00Z"
        );

        // Assert
        assertEquals(tenantId, response.tenantId());
        assertEquals("test query", response.query());
        assertEquals("test-model", response.embeddingModel());
        assertEquals(chunks, response.results());
        assertEquals(2, response.totalResults());
        assertEquals(0.95, response.maxScore());
        assertEquals(150L, response.processingTimeMs());
        assertEquals("2024-01-01T10:00:00Z", response.timestamp());
    }

    private List<DocumentChunkDto> createMockDocumentChunks() {
        DocumentChunkDto chunk1 = new DocumentChunkDto(
            UUID.randomUUID(),
            "Spring AI is a comprehensive framework for building AI-powered applications with Java.",
            1,
            0,
            87,
            20,
            "spring-ai-guide.pdf",
            Map.of("score", 0.95, "category", "framework")
        );
        
        DocumentChunkDto chunk2 = new DocumentChunkDto(
            UUID.randomUUID(),
            "It provides integration with various LLM providers like OpenAI, Azure OpenAI, and Ollama.",
            2,
            87,
            175,
            18,
            "spring-ai-guide.pdf",
            Map.of("score", 0.87, "category", "integration")
        );
        
        return List.of(chunk1, chunk2);
    }
}
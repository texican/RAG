package com.byo.rag.core.controller;

import com.byo.rag.core.dto.RagQueryRequest;
import com.byo.rag.core.dto.RagQueryResponse;
import com.byo.rag.core.dto.RagQueryRequest.RagOptions;
import com.byo.rag.core.service.RagService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for RagController.
 * Tests REST API endpoints for RAG query processing including
 * synchronous, asynchronous, and streaming responses.
 */
@WebMvcTest(RagController.class)
@ActiveProfiles("test")
class RagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RagService ragService;

    private UUID tenantId;
    private RagQueryRequest testRequest;
    private RagQueryResponse testResponse;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        testRequest = RagQueryRequest.simple(tenantId, "What is Spring AI?");
        
        testResponse = RagQueryResponse.success(
            tenantId,
            "What is Spring AI?",
            null,
            "Spring AI is a comprehensive framework for building AI-powered applications.",
            List.of(),
            RagQueryResponse.RagMetrics.withTiming(100L, 50L, 200L, 5, 3)
        );
    }

    @Test
    void processQuery_ValidRequest_ReturnsSuccessResponse() throws Exception {
        // Arrange
        when(ragService.processQuery(any(RagQueryRequest.class))).thenReturn(testResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/rag/query")
                .header("X-Tenant-ID", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.tenantId").value(tenantId.toString()))
                .andExpect(jsonPath("$.query").value("What is Spring AI?"))
                .andExpect(jsonPath("$.response").value("Spring AI is a comprehensive framework for building AI-powered applications."))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(ragService).processQuery(any(RagQueryRequest.class));
    }

    @Test
    void processQuery_MissingTenantId_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/rag/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isBadRequest());

        verify(ragService, never()).processQuery(any(RagQueryRequest.class));
    }

    @Test
    void processQuery_InvalidTenantId_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/rag/query")
                .header("X-Tenant-ID", "invalid-uuid")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isBadRequest());

        verify(ragService, never()).processQuery(any(RagQueryRequest.class));
    }

    @Test
    void processQuery_EmptyRequestBody_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/rag/query")
                .header("X-Tenant-ID", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());

        verify(ragService, never()).processQuery(any(RagQueryRequest.class));
    }

    @Test
    void processQuery_ServiceFailure_ReturnsErrorResponse() throws Exception {
        // Arrange
        RagQueryResponse errorResponse = RagQueryResponse.failure(
            tenantId,
            "What is Spring AI?",
            null,
            "Service temporarily unavailable",
            RagQueryResponse.RagMetrics.withTiming(0L, 0L, 0L, 0, 0)
        );
        when(ragService.processQuery(any(RagQueryRequest.class))).thenReturn(errorResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/rag/query")
                .header("X-Tenant-ID", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isOk()) // Service errors still return 200 with error in body
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.error").value("Service temporarily unavailable"));

        verify(ragService).processQuery(any(RagQueryRequest.class));
    }

    @Test
    void processQueryAsync_ValidRequest_ReturnsAsyncResponse() throws Exception {
        // Arrange
        CompletableFuture<RagQueryResponse> futureResponse = CompletableFuture.completedFuture(testResponse);
        when(ragService.processQueryAsync(any(RagQueryRequest.class))).thenReturn(futureResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/rag/query/async")
                .header("X-Tenant-ID", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.tenantId").value(tenantId.toString()))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(ragService).processQueryAsync(any(RagQueryRequest.class));
    }

    @Test
    void processQueryStreaming_ValidRequest_ReturnsStreamingResponse() throws Exception {
        // Arrange
        Flux<String> streamingResponse = Flux.just("Spring", " AI", " is", " a", " framework");
        when(ragService.processQueryStreaming(any(RagQueryRequest.class))).thenReturn(streamingResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/rag/query/stream")
                .header("X-Tenant-ID", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/plain;charset=UTF-8"));

        verify(ragService).processQueryStreaming(any(RagQueryRequest.class));
    }

    @Test
    void getStats_ValidTenantId_ReturnsStats() throws Exception {
        // Arrange
        RagService.RagStats stats = new RagService.RagStats(
            100L, // totalQueries
            95L,  // successfulQueries
            20L,  // cachedQueries
            250.5, // averageResponseTimeMs
            0.85,  // averageRelevanceScore
            null   // providerStats
        );
        when(ragService.getStats(tenantId.toString())).thenReturn(stats);

        // Act & Assert
        mockMvc.perform(get("/api/v1/rag/stats")
                .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalQueries").value(100))
                .andExpect(jsonPath("$.successfulQueries").value(95))
                .andExpect(jsonPath("$.cachedQueries").value(20))
                .andExpect(jsonPath("$.averageResponseTimeMs").value(250.5))
                .andExpect(jsonPath("$.averageRelevanceScore").value(0.85));

        verify(ragService).getStats(tenantId.toString());
    }

    @Test
    void getStats_MissingTenantId_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/rag/stats"))
                .andExpect(status().isBadRequest());

        verify(ragService, never()).getStats(any());
    }

    @Test
    void healthCheck_ReturnsOk() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/rag/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));

        verifyNoInteractions(ragService);
    }

    @Test
    void processQuery_LargePayload_HandlesCorrectly() throws Exception {
        // Arrange
        String largeQuery = "What is Spring AI? ".repeat(1000); // Large query
        RagQueryRequest largeRequest = RagQueryRequest.simple(tenantId, largeQuery);
        
        when(ragService.processQuery(any(RagQueryRequest.class))).thenReturn(testResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/rag/query")
                .header("X-Tenant-ID", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(largeRequest)))
                .andExpect(status().isOk());

        verify(ragService).processQuery(any(RagQueryRequest.class));
    }

    @Test
    void processQuery_WithComplexOptions_HandlesCorrectly() throws Exception {
        // Arrange
        RagOptions complexOptions = new RagOptions(15, 0.8, 5000, null, 
                null, null, true, 
                null, null, null, null);

        RagQueryRequest complexRequest = new RagQueryRequest(
            tenantId,
            "Complex query with options",
            "conv-123",
            "user-456",
            "session-789",
            List.of(UUID.randomUUID(), UUID.randomUUID()),
            Collections.singletonMap("category", "technical"),
            complexOptions
        );

        when(ragService.processQuery(any(RagQueryRequest.class))).thenReturn(testResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/rag/query")
                .header("X-Tenant-ID", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(complexRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(ragService).processQuery(any(RagQueryRequest.class));
    }
}
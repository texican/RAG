package com.byo.rag.core.controller;

import com.byo.rag.core.config.TestCoreConfig;
import com.byo.rag.core.dto.RagQueryRequest;
import com.byo.rag.core.dto.RagQueryResponse;
import com.byo.rag.core.service.*;
import com.byo.rag.shared.exception.RagException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for RagController.
 * Tests all REST endpoints with proper Spring Boot context.
 */
@WebMvcTest(RagController.class)
@Import(TestCoreConfig.class)
class RagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RagService ragService;

    @MockBean
    private ConversationService conversationService;

    @MockBean
    private QueryOptimizationService queryOptimizationService;

    @MockBean
    private LLMIntegrationService llmService;

    private UUID tenantId;
    private RagQueryRequest validRequest;
    private RagQueryResponse mockResponse;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        
        validRequest = RagQueryRequest.simple(tenantId, "What is Spring AI?");
        
        mockResponse = RagQueryResponse.success(
            tenantId,
            "What is Spring AI?",
            "conversation-123",
            "Spring AI is a framework for building AI applications with Java.",
            List.of(),
            RagQueryResponse.RagMetrics.withTiming(50L, 30L, 120L, 3, 2)
        );
    }

    @Test
    void processQuery_ValidRequest_ReturnsSuccess() throws Exception {
        when(ragService.processQuery(any(RagQueryRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/rag/query")
                .header("X-Tenant-ID", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("Spring AI is a framework for building AI applications with Java."))
                .andExpect(jsonPath("$.originalQuery").value("What is Spring AI?"))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(ragService).processQuery(any(RagQueryRequest.class));
    }

    @Test
    void processQuery_TenantIdMismatch_ReturnsBadRequest() throws Exception {
        UUID differentTenantId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/rag/query")
                .header("X-Tenant-ID", differentTenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());

        verify(ragService, never()).processQuery(any(RagQueryRequest.class));
    }

    @Test
    void processQuery_MissingTenantHeader_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/rag/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void processQuery_InvalidRequestBody_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/rag/query")
                .header("X-Tenant-ID", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void processQuery_ServiceException_ReturnsInternalServerError() throws Exception {
        when(ragService.processQuery(any(RagQueryRequest.class)))
                .thenThrow(new RagException("Service unavailable"));

        mockMvc.perform(post("/api/v1/rag/query")
                .header("X-Tenant-ID", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void processQueryAsync_ValidRequest_ReturnsAccepted() throws Exception {
        CompletableFuture<RagQueryResponse> futureResponse = CompletableFuture.completedFuture(mockResponse);
        when(ragService.processQueryAsync(any(RagQueryRequest.class))).thenReturn(futureResponse);

        mockMvc.perform(post("/api/v1/rag/query/async")
                .header("X-Tenant-ID", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isAccepted());

        verify(ragService).processQueryAsync(any(RagQueryRequest.class));
    }

    @Test
    void processQueryStreaming_ValidRequest_ReturnsStreamingResponse() throws Exception {
        Flux<String> mockStream = Flux.just("Spring", " AI", " is", " a", " framework");
        when(ragService.processQueryStreaming(any(RagQueryRequest.class))).thenReturn(mockStream);

        mockMvc.perform(post("/api/v1/rag/query/stream")
                .header("X-Tenant-ID", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_PLAIN));

        verify(ragService).processQueryStreaming(any(RagQueryRequest.class));
    }

    @Test
    void analyzeQuery_ValidQuery_ReturnsAnalysis() throws Exception {
        QueryOptimizationService.QueryAnalysis mockAnalysis = new QueryOptimizationService.QueryAnalysis(
            "What is Spring AI?",
            15,
            3,
            QueryOptimizationService.QueryComplexity.MODERATE,
            List.of(),
            List.of("Consider adding more specific terms"),
            List.of("framework", "Spring", "AI")
        );

        when(queryOptimizationService.analyzeQuery(anyString())).thenReturn(mockAnalysis);

        Map<String, String> queryRequest = Map.of("query", "What is Spring AI?");

        mockMvc.perform(post("/api/v1/rag/query/analyze")
                .header("X-Tenant-ID", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(queryRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalQuery").value("What is Spring AI?"))
                .andExpect(jsonPath("$.characterCount").value(15))
                .andExpect(jsonPath("$.wordCount").value(3))
                .andExpect(jsonPath("$.complexity").value("MODERATE"));

        verify(queryOptimizationService).analyzeQuery("What is Spring AI?");
    }

    @Test
    void analyzeQuery_EmptyQuery_ReturnsBadRequest() throws Exception {
        Map<String, String> queryRequest = Map.of("query", "");

        mockMvc.perform(post("/api/v1/rag/query/analyze")
                .header("X-Tenant-ID", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(queryRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getQuerySuggestions_ValidQuery_ReturnsSuggestions() throws Exception {
        List<String> mockSuggestions = List.of(
            "What is the Spring AI framework?",
            "How does Spring AI work?",
            "Spring AI framework overview"
        );

        when(queryOptimizationService.suggestAlternatives(anyString())).thenReturn(mockSuggestions);

        Map<String, String> queryRequest = Map.of("query", "What is Spring AI?");

        mockMvc.perform(post("/api/v1/rag/query/suggestions")
                .header("X-Tenant-ID", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(queryRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3));

        verify(queryOptimizationService).suggestAlternatives("What is Spring AI?");
    }

    @Test
    void getConversation_ExistingConversation_ReturnsConversation() throws Exception {
        String conversationId = "conv-123";
        ConversationService.ConversationSummary mockSummary = new ConversationService.ConversationSummary(
            conversationId,
            UUID.randomUUID(),
            10,
            Instant.now(),
            Instant.now(),
            "What is Spring AI?",
            "How does AI work?"
        );

        when(conversationService.getConversationSummary(conversationId)).thenReturn(mockSummary);

        mockMvc.perform(get("/api/v1/rag/conversations/{conversationId}", conversationId)
                .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value(conversationId))
                .andExpect(jsonPath("$.totalExchanges").value(10));

        verify(conversationService).getConversationSummary(conversationId);
    }

    @Test
    void getConversation_NonExistentConversation_ReturnsNotFound() throws Exception {
        String conversationId = "non-existent";

        when(conversationService.getConversationSummary(conversationId)).thenReturn(null);

        mockMvc.perform(get("/api/v1/rag/conversations/{conversationId}", conversationId)
                .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteConversation_ExistingConversation_ReturnsNoContent() throws Exception {
        String conversationId = "conv-123";

        when(conversationService.deleteConversation(conversationId)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/rag/conversations/{conversationId}", conversationId)
                .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isNoContent());

        verify(conversationService).deleteConversation(conversationId);
    }

    @Test
    void deleteConversation_NonExistentConversation_ReturnsNotFound() throws Exception {
        String conversationId = "non-existent";

        when(conversationService.deleteConversation(conversationId)).thenReturn(false);

        mockMvc.perform(delete("/api/v1/rag/conversations/{conversationId}", conversationId)
                .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getConversationStats_ExistingConversation_ReturnsStats() throws Exception {
        String conversationId = "conv-123";
        ConversationService.ConversationStats mockStats = new ConversationService.ConversationStats(
            conversationId,
            10,
            5,
            3,
            Instant.now(),
            Instant.now(),
            120.5
        );

        when(conversationService.getStats(conversationId)).thenReturn(mockStats);

        mockMvc.perform(get("/api/v1/rag/conversations/{conversationId}/stats", conversationId)
                .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value(conversationId))
                .andExpect(jsonPath("$.totalExchanges").value(10))
                .andExpect(jsonPath("$.totalQueries").value(5))
                .andExpect(jsonPath("$.uniqueDocumentsReferenced").value(3));

        verify(conversationService).getStats(conversationId);
    }

    @Test
    void getConversationStats_NonExistentConversation_ReturnsNotFound() throws Exception {
        String conversationId = "non-existent";
        ConversationService.ConversationStats mockStats = new ConversationService.ConversationStats(
            null, 0, 0, 0, null, null, 0.0
        );

        when(conversationService.getStats(conversationId)).thenReturn(mockStats);

        mockMvc.perform(get("/api/v1/rag/conversations/{conversationId}/stats", conversationId)
                .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRagStats_ValidTenant_ReturnsStats() throws Exception {
        RagService.RagStats mockStats = new RagService.RagStats(
            100L,
            95L,
            5L,
            250.0,
            0.95,
            Map.of("openai", "healthy")
        );

        when(ragService.getStats(tenantId.toString())).thenReturn(mockStats);

        mockMvc.perform(get("/api/v1/rag/stats")
                .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQueries").value(100))
                .andExpect(jsonPath("$.successfulQueries").value(95))
                .andExpect(jsonPath("$.cachedQueries").value(5));

        verify(ragService).getStats(tenantId.toString());
    }

    @Test
    void getProviderStatus_ValidRequest_ReturnsStatus() throws Exception {
        Map<String, Object> mockStatus = Map.of(
            "openai", Map.of("status", "available", "responseTime", "120ms"),
            "ollama", Map.of("status", "unavailable", "error", "Connection timeout")
        );

        when(llmService.getProviderStatus()).thenReturn(mockStatus);

        mockMvc.perform(get("/api/v1/rag/providers/status")
                .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openai.status").value("available"))
                .andExpect(jsonPath("$.ollama.status").value("unavailable"));

        verify(llmService).getProviderStatus();
    }

    @Test
    void healthCheck_HealthySystem_ReturnsOk() throws Exception {
        when(llmService.isProviderAvailable("openai")).thenReturn(true);

        mockMvc.perform(get("/api/v1/rag/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.llmService").value("UP"));
    }

    @Test
    void healthCheck_UnhealthySystem_ReturnsServiceUnavailable() throws Exception {
        when(llmService.isProviderAvailable("openai")).thenReturn(false);

        mockMvc.perform(get("/api/v1/rag/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.components.llmService").value("DOWN"));
    }

    @Test
    void healthCheck_ExceptionDuringCheck_ReturnsServiceUnavailable() throws Exception {
        when(llmService.isProviderAvailable("openai")).thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(get("/api/v1/rag/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"));
    }
}
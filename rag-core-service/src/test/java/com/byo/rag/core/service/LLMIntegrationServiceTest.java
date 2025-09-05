package com.byo.rag.core.service;

import com.byo.rag.core.dto.RagQueryRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.StreamResponseSpec;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LLMIntegrationService.
 */
@ExtendWith(MockitoExtension.class)
class LLMIntegrationServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequest.CallResponseSpec callResponseSpec;

    @Mock
    private ChatClient.ChatClientRequest.StreamResponseSpec streamResponseSpec;

    @Mock
    private ChatClient.ChatClientRequest chatClientRequest;

    @InjectMocks
    private LLMIntegrationService llmIntegrationService;

    @BeforeEach
    void setUp() {
        when(chatClient.prompt()).thenReturn(chatClientRequest);
        when(chatClientRequest.user(anyString())).thenReturn(chatClientRequest);
        when(chatClientRequest.system(anyString())).thenReturn(chatClientRequest);
    }

    @Test
    void generateResponse_ValidInputs_ReturnsResponse() {
        String context = "Spring AI is a framework for building AI applications";
        String query = "What is Spring AI?";
        RagQueryRequest request = RagQueryRequest.simple(UUID.randomUUID(), query);
        
        when(chatClientRequest.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("Spring AI is a comprehensive framework for building AI-powered applications with Java.");

        String response = llmIntegrationService.generateResponse(context, query, request);

        assertNotNull(response);
        assertFalse(response.isEmpty());
        assertTrue(response.contains("Spring AI"));
        verify(chatClient).prompt();
        verify(chatClientRequest).user(anyString());
        verify(callResponseSpec).content();
    }

    @Test
    void generateResponse_EmptyContext_StillGeneratesResponse() {
        String context = "";
        String query = "What is machine learning?";
        RagQueryRequest request = RagQueryRequest.simple(UUID.randomUUID(), query);
        
        when(chatClientRequest.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("Machine learning is a subset of artificial intelligence.");

        String response = llmIntegrationService.generateResponse(context, query, request);

        assertNotNull(response);
        assertFalse(response.isEmpty());
        verify(chatClient).prompt();
    }

    @Test
    void generateResponse_NullInputs_HandlesGracefully() {
        when(chatClientRequest.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("I need more information to provide a helpful response.");

        assertDoesNotThrow(() -> {
            String response = llmIntegrationService.generateResponse(null, null, null);
            assertNotNull(response);
        });
    }

    @Test
    void generateResponseStreaming_ValidInputs_ReturnsFlux() {
        String context = "Spring AI provides AI integration";
        String query = "Tell me about Spring AI";
        RagQueryRequest request = RagQueryRequest.simple(UUID.randomUUID(), query);
        
        Flux<String> mockFlux = Flux.just("Spring", " AI", " is", " a", " framework");
        
        when(chatClientRequest.stream()).thenReturn(streamResponseSpec);
        when(streamResponseSpec.content()).thenReturn(mockFlux);

        Flux<String> responseFlux = llmIntegrationService.generateResponseStreaming(context, query, request);

        assertNotNull(responseFlux);
        
        // Verify the flux contains expected content
        String fullResponse = responseFlux.collectList().block().stream()
            .reduce("", (a, b) -> a + b);
        assertEquals("Spring AI is a framework", fullResponse);
        
        verify(chatClient).prompt();
        verify(chatClientRequest).stream();
        verify(streamResponseSpec).content();
    }

    @Test
    void generateResponseStreaming_EmptyContext_ReturnsFlux() {
        String context = "";
        String query = "What is AI?";
        RagQueryRequest request = RagQueryRequest.simple(UUID.randomUUID(), query);
        
        Flux<String> mockFlux = Flux.just("AI", " stands", " for", " Artificial", " Intelligence");
        
        when(chatClientRequest.stream()).thenReturn(streamResponseSpec);
        when(streamResponseSpec.content()).thenReturn(mockFlux);

        Flux<String> responseFlux = llmIntegrationService.generateResponseStreaming(context, query, request);

        assertNotNull(responseFlux);
        verify(chatClient).prompt();
    }

    @Test
    void isProviderAvailable_ValidProvider_ReturnsTrue() {
        // Mock provider availability check
        boolean available = llmIntegrationService.isProviderAvailable("openai");

        // Default implementation should return true for basic availability
        assertTrue(available);
    }

    @Test
    void isProviderAvailable_InvalidProvider_ReturnsFalse() {
        boolean available = llmIntegrationService.isProviderAvailable("invalid-provider");

        // Should return false for unknown providers
        assertFalse(available);
    }

    @Test
    void isProviderAvailable_NullProvider_ReturnsFalse() {
        boolean available = llmIntegrationService.isProviderAvailable(null);

        assertFalse(available);
    }

    @Test
    void getProviderStatus_ReturnsStatusMap() {
        Map<String, Object> status = llmIntegrationService.getProviderStatus();

        assertNotNull(status);
        assertFalse(status.isEmpty());
        assertTrue(status.containsKey("openai"));
    }

    @Test
    void buildPrompt_WithContext_IncludesContextInPrompt() {
        String context = "Spring AI is a framework for AI applications";
        String query = "What can you tell me about this?";
        
        // This is a protected method, so we test it indirectly through generateResponse
        when(chatClientRequest.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("Based on the context, Spring AI is a comprehensive framework...");
        
        RagQueryRequest request = RagQueryRequest.simple(UUID.randomUUID(), query);
        String response = llmIntegrationService.generateResponse(context, query, request);

        assertNotNull(response);
        verify(chatClientRequest).user(argThat(prompt -> 
            prompt.contains(context) && prompt.contains(query)));
    }

    @Test
    void buildPrompt_WithoutContext_OnlyIncludesQuery() {
        String query = "What is machine learning?";
        
        when(chatClientRequest.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("Machine learning is a method of data analysis...");
        
        RagQueryRequest request = RagQueryRequest.simple(UUID.randomUUID(), query);
        String response = llmIntegrationService.generateResponse("", query, request);

        assertNotNull(response);
        verify(chatClientRequest).user(argThat(prompt -> 
            prompt.contains(query)));
    }

    @Test
    void generateResponse_WithSystemPrompt_UsesSystemMessage() {
        String context = "Technical documentation about Spring AI";
        String query = "Explain Spring AI";
        RagQueryRequest request = RagQueryRequest.simple(UUID.randomUUID(), query);
        
        when(chatClientRequest.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("Spring AI provides comprehensive AI integration capabilities...");

        String response = llmIntegrationService.generateResponse(context, query, request);

        assertNotNull(response);
        verify(chatClientRequest).system(anyString());
        verify(chatClientRequest).user(anyString());
    }
}
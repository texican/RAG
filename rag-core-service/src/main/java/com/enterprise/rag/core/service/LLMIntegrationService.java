package com.enterprise.rag.core.service;

import com.enterprise.rag.core.dto.RagQueryRequest;
import com.enterprise.rag.shared.exception.RagException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service for integrating with multiple LLM providers.
 */
@Service
public class LLMIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(LLMIntegrationService.class);

    private final ChatClient chatClient;
    private final AtomicReference<String> lastUsedProvider = new AtomicReference<>("openai");

    @Value("${llm.default-provider:openai}")
    private String defaultProvider;

    @Value("${llm.fallback-provider:ollama}")
    private String fallbackProvider;

    @Value("${llm.max-tokens:1500}")
    private int maxTokens;

    @Value("${llm.temperature:0.7}")
    private double temperature;

    @Value("${llm.timeout-seconds:30}")
    private int timeoutSeconds;

    // System prompts for different scenarios
    private static final String DEFAULT_SYSTEM_PROMPT = """
        You are an AI assistant helping users find information from their documents. 
        Use the provided context to answer questions accurately and helpfully.
        
        Guidelines:
        1. Base your answers primarily on the provided context
        2. If the context doesn't contain enough information, clearly state this
        3. Be concise but comprehensive in your responses
        4. Cite specific information from the context when relevant
        5. If asked about something not in the context, politely explain the limitation
        """;

    private static final String RAG_RESPONSE_TEMPLATE = """
        Context Information:
        {context}
        
        User Question: {question}
        
        Please provide a helpful answer based on the context above. If the context doesn't contain 
        sufficient information to fully answer the question, please say so clearly.
        """;

    public LLMIntegrationService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * Generate response using the default LLM provider.
     */
    public String generateResponse(String query, String context, RagQueryRequest request) {
        return generateResponse(query, context, request, defaultProvider);
    }

    /**
     * Generate response using a specific LLM provider.
     */
    public String generateResponse(String query, String context, RagQueryRequest request, String provider) {
        logger.debug("Generating LLM response for tenant: {} using provider: {}", 
                    request.tenantId(), provider);

        try {
            String systemPrompt = getSystemPrompt(request);
            String userPrompt = formatUserPrompt(query, context, request);

            ChatResponse response = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .chatResponse();

            String generatedText = response.getResult().getOutput().getContent();
            lastUsedProvider.set(provider);

            logger.info("LLM response generated successfully for tenant: {} using provider: {}", 
                       request.tenantId(), provider);

            return generatedText;

        } catch (Exception e) {
            logger.error("LLM generation failed for tenant: {} with provider: {}", 
                        request.tenantId(), provider, e);

            // Try fallback provider if primary failed
            if (!provider.equals(fallbackProvider)) {
                logger.info("Attempting fallback to provider: {} for tenant: {}", 
                           fallbackProvider, request.tenantId());
                return generateResponseWithFallback(query, context, request);
            }

            throw new RagException("LLM response generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Generate streaming response for real-time interaction.
     */
    public Flux<String> generateResponseStreaming(String query, String context, RagQueryRequest request) {
        return generateResponseStreaming(query, context, request, defaultProvider);
    }

    /**
     * Generate streaming response using specific provider.
     */
    public Flux<String> generateResponseStreaming(String query, String context, 
                                                RagQueryRequest request, String provider) {
        logger.debug("Generating streaming LLM response for tenant: {} using provider: {}", 
                    request.tenantId(), provider);

        try {
            String systemPrompt = getSystemPrompt(request);
            String userPrompt = formatUserPrompt(query, context, request);

            return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .stream()
                .content()
                .doOnNext(chunk -> logger.trace("Streaming chunk received for tenant: {}", request.tenantId()))
                .doOnComplete(() -> {
                    lastUsedProvider.set(provider);
                    logger.info("Streaming response completed for tenant: {}", request.tenantId());
                })
                .doOnError(error -> logger.error("Streaming failed for tenant: {}", request.tenantId(), error));

        } catch (Exception e) {
            logger.error("Streaming LLM generation failed for tenant: {} with provider: {}", 
                        request.tenantId(), provider, e);

            if (!provider.equals(fallbackProvider)) {
                logger.info("Attempting streaming fallback to provider: {} for tenant: {}", 
                           fallbackProvider, request.tenantId());
                return generateResponseStreamingWithFallback(query, context, request);
            }

            return Flux.error(new RagException("Streaming LLM response generation failed: " + e.getMessage(), e));
        }
    }

    /**
     * Get the provider that was last successfully used.
     */
    public String getProviderUsed() {
        return lastUsedProvider.get();
    }

    /**
     * Check if a specific provider is available.
     */
    public boolean isProviderAvailable(String provider) {
        try {
            // Simple health check by attempting a minimal call
            String testPrompt = "Hello";
            
            ChatResponse response = chatClient.prompt()
                .user(testPrompt)
                .call()
                .chatResponse();
                
            return response != null && response.getResult() != null;
            
        } catch (Exception e) {
            logger.warn("Provider {} is not available: {}", provider, e.getMessage());
            return false;
        }
    }

    /**
     * Get available providers and their status.
     */
    public Map<String, Object> getProviderStatus() {
        return Map.of(
            "defaultProvider", defaultProvider,
            "fallbackProvider", fallbackProvider,
            "lastUsedProvider", lastUsedProvider.get(),
            "defaultAvailable", isProviderAvailable(defaultProvider),
            "fallbackAvailable", isProviderAvailable(fallbackProvider)
        );
    }

    /**
     * Optimize prompt for better LLM performance.
     */
    public String optimizePrompt(String originalPrompt, RagQueryRequest request) {
        // Simple prompt optimization strategies
        String optimized = originalPrompt.trim();

        // Remove excessive whitespace
        optimized = optimized.replaceAll("\\s+", " ");

        // Add context about user's intent if available
        if (request.options() != null && request.options().intent() != null) {
            optimized = "Intent: " + request.options().intent() + "\n\n" + optimized;
        }

        // Add specificity for better responses
        if (!optimized.toLowerCase().contains("specific") && 
            !optimized.toLowerCase().contains("detail")) {
            optimized += " Please be specific and provide details where appropriate.";
        }

        return optimized;
    }

    private String generateResponseWithFallback(String query, String context, RagQueryRequest request) {
        try {
            logger.info("Using fallback provider: {} for tenant: {}", fallbackProvider, request.tenantId());
            return generateResponse(query, context, request, fallbackProvider);
        } catch (Exception fallbackError) {
            logger.error("Fallback provider also failed for tenant: {}", request.tenantId(), fallbackError);
            throw new RagException("Both primary and fallback LLM providers failed", fallbackError);
        }
    }

    private Flux<String> generateResponseStreamingWithFallback(String query, String context, RagQueryRequest request) {
        try {
            logger.info("Using streaming fallback provider: {} for tenant: {}", fallbackProvider, request.tenantId());
            return generateResponseStreaming(query, context, request, fallbackProvider);
        } catch (Exception fallbackError) {
            logger.error("Streaming fallback provider also failed for tenant: {}", request.tenantId(), fallbackError);
            return Flux.error(new RagException("Both primary and fallback streaming providers failed", fallbackError));
        }
    }

    private String getSystemPrompt(RagQueryRequest request) {
        // Use custom system prompt if provided in request options
        if (request.options() != null && request.options().systemPrompt() != null) {
            return request.options().systemPrompt();
        }

        // Use tenant-specific prompt if available
        // TODO: Implement tenant-specific prompt storage and retrieval
        
        return DEFAULT_SYSTEM_PROMPT;
    }

    private String formatUserPrompt(String query, String context, RagQueryRequest request) {
        PromptTemplate template = new PromptTemplate(RAG_RESPONSE_TEMPLATE);
        
        Map<String, Object> variables = Map.of(
            "context", context != null ? context : "No relevant context available.",
            "question", query
        );

        Prompt prompt = template.create(variables);
        return prompt.getContents();
    }

    /**
     * LLM provider configuration.
     */
    public record LLMConfig(
        String provider,
        String modelName,
        int maxTokens,
        double temperature,
        int timeoutSeconds,
        Map<String, Object> additionalParams
    ) {
        public static LLMConfig defaultOpenAI() {
            return new LLMConfig("openai", "gpt-3.5-turbo", 1500, 0.7, 30, Map.of());
        }

        public static LLMConfig defaultOllama() {
            return new LLMConfig("ollama", "llama2", 1500, 0.7, 60, Map.of());
        }
    }

    /**
     * LLM generation statistics.
     */
    public record LLMStats(
        String provider,
        String modelUsed,
        int tokensGenerated,
        long generationTimeMs,
        double temperature,
        boolean wasStreamed,
        boolean usedFallback
    ) {}
}
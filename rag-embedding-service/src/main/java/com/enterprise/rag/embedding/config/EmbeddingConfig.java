package com.enterprise.rag.embedding.config;

import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.openai.OpenAiEmbeddingClient;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.transformers.TransformersEmbeddingClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration for embedding models and services.
 */
@Configuration
public class EmbeddingConfig {
    
    /**
     * Primary embedding client (OpenAI).
     */
    @Bean
    @Primary
    public EmbeddingClient primaryEmbeddingClient(EmbeddingModelProperties properties) {
        var openAiApi = new OpenAiApi(properties.openai().apiKey());
        var options = OpenAiEmbeddingOptions.builder()
            .withModel(properties.openai().model())
            .withDimensions(properties.openai().dimensions())
            .build();
        
        return new OpenAiEmbeddingClient(openAiApi, options);
    }
    
    /**
     * Fallback embedding client (Local Transformers).
     */
    @Bean("fallbackEmbeddingClient")
    public EmbeddingClient fallbackEmbeddingClient(EmbeddingModelProperties properties) {
        return new TransformersEmbeddingClient(properties.transformers().modelPath());
    }
    
    /**
     * Registry of all available embedding clients.
     */
    @Bean
    public EmbeddingClientRegistry embeddingClientRegistry(
            EmbeddingClient primaryEmbeddingClient,
            EmbeddingClient fallbackEmbeddingClient,
            EmbeddingModelProperties properties) {
        
        Map<String, EmbeddingClient> clients = new ConcurrentHashMap<>();
        clients.put(properties.defaultModel(), primaryEmbeddingClient);
        clients.put(properties.fallbackModel(), fallbackEmbeddingClient);
        clients.put("openai-text-embedding-3-small", primaryEmbeddingClient);
        clients.put("sentence-transformers-all-minilm-l6-v2", fallbackEmbeddingClient);
        
        return new EmbeddingClientRegistry(clients, properties.defaultModel());
    }
    
    /**
     * Registry for managing multiple embedding clients.
     */
    public record EmbeddingClientRegistry(
        Map<String, EmbeddingClient> clients,
        String defaultModelName
    ) {
        
        /**
         * Get embedding client by model name.
         */
        public EmbeddingClient getClient(String modelName) {
            if (modelName == null || modelName.isEmpty()) {
                return clients.get(defaultModelName);
            }
            
            return clients.getOrDefault(modelName, clients.get(defaultModelName));
        }
        
        /**
         * Check if model is available.
         */
        public boolean hasModel(String modelName) {
            return clients.containsKey(modelName);
        }
        
        /**
         * Get all available model names.
         */
        public java.util.Set<String> getAvailableModels() {
            return clients.keySet();
        }
    }
    
    /**
     * Configuration properties for embedding models.
     */
    @ConfigurationProperties(prefix = "embedding.models")
    public record EmbeddingModelProperties(
        String defaultModel,
        String fallbackModel,
        int cacheTtl,
        OpenAIConfig openai,
        TransformersConfig transformers
    ) {
        public EmbeddingModelProperties() {
            this(
                "openai-text-embedding-3-small",
                "sentence-transformers-all-minilm-l6-v2",
                3600,
                new OpenAIConfig(),
                new TransformersConfig()
            );
        }
        
        public record OpenAIConfig(
            String apiKey,
            String model,
            int dimensions
        ) {
            public OpenAIConfig() {
                this("your-openai-api-key", "text-embedding-3-small", 1536);
            }
        }
        
        public record TransformersConfig(
            String modelPath
        ) {
            public TransformersConfig() {
                this("models/sentence-transformers/all-MiniLM-L6-v2");
            }
        }
    }
}
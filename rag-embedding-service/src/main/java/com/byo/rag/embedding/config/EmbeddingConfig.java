package com.byo.rag.embedding.config;

import com.byo.rag.embedding.client.OllamaEmbeddingClient;
import com.byo.rag.embedding.model.OllamaEmbeddingModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.transformers.TransformersEmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration for embedding models and services.
 */
@Configuration
@EnableConfigurationProperties(EmbeddingConfig.EmbeddingModelProperties.class)
public class EmbeddingConfig {

    /**
     * RestTemplate for HTTP client operations (used by OllamaEmbeddingClient).
     */
    @Bean
    @ConditionalOnProperty(name = "spring.profiles.active", havingValue = "docker")
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Primary embedding client (Ollama) - Active when Docker profile is enabled.
     */
    @Bean("primaryEmbeddingModel")
    @Primary
    @ConditionalOnProperty(name = "spring.profiles.active", havingValue = "docker")
    public EmbeddingModel ollamaEmbeddingModel(OllamaEmbeddingClient ollamaClient) {
        return new OllamaEmbeddingModel(ollamaClient);
    }

    /**
     * Primary embedding client (OpenAI) - Active when Docker profile is NOT enabled.
     */
    @Bean("primaryEmbeddingModel")
    @Primary
    @ConditionalOnProperty(name = "spring.profiles.active", havingValue = "docker", matchIfMissing = false)
    public EmbeddingModel openAiEmbeddingModel(EmbeddingModelProperties properties) {
        // Note: For Spring AI 1.0.0-M1, the constructor signature has changed
        // Using a simplified initialization for now
        var openAiApi = new OpenAiApi(properties.openai().apiKey());
        return new OpenAiEmbeddingModel(openAiApi);
    }
    
    /**
     * Fallback embedding client (Local Transformers).
     */
    @Bean("fallbackEmbeddingModel")
    public EmbeddingModel fallbackEmbeddingModel(EmbeddingModelProperties properties) {
        return new TransformersEmbeddingModel();
    }
    
    /**
     * Registry of all available embedding clients.
     */
    @Bean
    public EmbeddingModelRegistry embeddingClientRegistry(
            EmbeddingModel primaryEmbeddingModel,
            EmbeddingModel fallbackEmbeddingModel,
            EmbeddingModelProperties properties) {
        
        Map<String, EmbeddingModel> clients = new ConcurrentHashMap<>();
        clients.put(properties.defaultModel(), primaryEmbeddingModel);
        clients.put(properties.fallbackModel(), fallbackEmbeddingModel);
        clients.put("openai-text-embedding-3-small", primaryEmbeddingModel);
        clients.put("sentence-transformers-all-minilm-l6-v2", fallbackEmbeddingModel);
        
        return new EmbeddingModelRegistry(clients, properties.defaultModel());
    }
    
    /**
     * Registry for managing multiple embedding clients.
     */
    public record EmbeddingModelRegistry(
        Map<String, EmbeddingModel> clients,
        String defaultModelName
    ) {
        
        /**
         * Get embedding client by model name.
         */
        public EmbeddingModel getClient(String modelName) {
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
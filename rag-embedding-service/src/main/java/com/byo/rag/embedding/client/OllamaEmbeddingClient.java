package com.byo.rag.embedding.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * REST client for Ollama embedding API.
 * Only active when running with docker profile.
 */
@Component
@ConditionalOnProperty(name = "spring.profiles.active", havingValue = "docker")
public class OllamaEmbeddingClient {

    private static final Logger logger = LoggerFactory.getLogger(OllamaEmbeddingClient.class);

    private final RestTemplate restTemplate;
    private final String ollamaBaseUrl;
    private final String embeddingModel;

    public OllamaEmbeddingClient(
            RestTemplate restTemplate,
            @Value("${spring.ai.ollama.base-url:http://localhost:11434}") String ollamaBaseUrl,
            @Value("${embedding.models.ollama:mxbai-embed-large}") String embeddingModel) {
        this.restTemplate = restTemplate;
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.embeddingModel = embeddingModel;
        logger.info("Initialized Ollama embedding client with URL: {} and model: {}",
                    ollamaBaseUrl, embeddingModel);
    }

    public List<Double> generateEmbedding(String text) {
        try {
            String url = ollamaBaseUrl + "/api/embeddings";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = Map.of(
                "model", embeddingModel,
                "prompt", text
            );

            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

            OllamaEmbeddingResponse response = restTemplate.postForObject(
                url, request, OllamaEmbeddingResponse.class
            );

            if (response == null || response.embedding == null) {
                throw new RuntimeException("Ollama embedding response is null");
            }

            return response.embedding;

        } catch (Exception e) {
            logger.error("Failed to generate embedding: {}", e.getMessage(), e);
            throw new RuntimeException("Ollama embedding generation failed", e);
        }
    }

    public static class OllamaEmbeddingResponse {
        @JsonProperty("embedding")
        public List<Double> embedding;
    }
}

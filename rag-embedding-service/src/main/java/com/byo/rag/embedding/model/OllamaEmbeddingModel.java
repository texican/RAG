package com.byo.rag.embedding.model;

import com.byo.rag.embedding.client.OllamaEmbeddingClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.Embedding;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring AI EmbeddingModel implementation using Ollama.
 * Provides local embedding generation compatible with Spring AI framework.
 *
 * Updated for Spring AI in Spring Boot 4.0 - uses float[] instead of List<Double>
 */
public class OllamaEmbeddingModel implements EmbeddingModel {

    private final OllamaEmbeddingClient client;

    public OllamaEmbeddingModel(OllamaEmbeddingClient client) {
        this.client = client;
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings = new ArrayList<>();

        for (String text : request.getInstructions()) {
            List<Double> vector = client.generateEmbedding(text);
            float[] floatArray = convertToFloatArray(vector);
            embeddings.add(new Embedding(floatArray, embeddings.size()));
        }

        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(Document document) {
        List<Double> vector = client.generateEmbedding(document.getText());
        return convertToFloatArray(vector);
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        return texts.stream()
                .map(text -> {
                    List<Double> vector = client.generateEmbedding(text);
                    return convertToFloatArray(vector);
                })
                .collect(Collectors.toList());
    }

    @Override
    public int dimensions() {
        return 1024; // mxbai-embed-large produces 1024-dimensional embeddings
    }

    /**
     * Convert List<Double> to float[] for Spring AI compatibility.
     */
    private float[] convertToFloatArray(List<Double> doubleList) {
        float[] result = new float[doubleList.size()];
        for (int i = 0; i < doubleList.size(); i++) {
            result[i] = doubleList.get(i).floatValue();
        }
        return result;
    }
}

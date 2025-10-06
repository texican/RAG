package com.byo.rag.embedding.model;

import com.byo.rag.embedding.client.OllamaEmbeddingClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring AI EmbeddingModel implementation using Ollama.
 * Provides local embedding generation compatible with Spring AI framework.
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
            embeddings.add(new Embedding(vector, embeddings.size()));
        }

        return new EmbeddingResponse(embeddings);
    }

    @Override
    public List<Double> embed(Document document) {
        return client.generateEmbedding(document.getContent());
    }

    @Override
    public List<List<Double>> embed(List<String> texts) {
        return texts.stream()
                .map(client::generateEmbedding)
                .collect(Collectors.toList());
    }

    @Override
    public int dimensions() {
        return 1024; // mxbai-embed-large produces 1024-dimensional embeddings
    }
}

package com.byo.rag.embedding.service;

import com.byo.rag.embedding.config.EmbeddingConfig.EmbeddingModelRegistry;
import com.byo.rag.embedding.dto.EmbeddingRequest;
import com.byo.rag.embedding.dto.EmbeddingResponse;
import com.byo.rag.embedding.dto.EmbeddingResponse.EmbeddingResult;
import com.byo.rag.shared.exception.EmbeddingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for generating embeddings using multiple AI models.
 */
@Service
public class EmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);
    
    private final EmbeddingModelRegistry clientRegistry;
    private final EmbeddingCacheService cacheService;
    private final VectorStorageService vectorStorageService;
    private final ExecutorService executorService;
    
    public EmbeddingService(
            EmbeddingModelRegistry clientRegistry,
            EmbeddingCacheService cacheService,
            VectorStorageService vectorStorageService) {
        this.clientRegistry = clientRegistry;
        this.cacheService = cacheService;
        this.vectorStorageService = vectorStorageService;
        this.executorService = Executors.newFixedThreadPool(4);
    }
    
    /**
     * Generate embeddings for texts with caching and storage.
     */
    public EmbeddingResponse generateEmbeddings(EmbeddingRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            String modelName = getEffectiveModelName(request.modelName());
            EmbeddingModel client = clientRegistry.getClient(modelName);
            
            if (client == null) {
                throw new EmbeddingException("No embedding client available for model: " + modelName);
            }
            
            List<EmbeddingResult> results = new ArrayList<>();
            List<String> textsToProcess = new ArrayList<>();
            List<Integer> indexesToProcess = new ArrayList<>();
            
            // Check cache first
            for (int i = 0; i < request.texts().size(); i++) {
                String text = request.texts().get(i);
                UUID chunkId = request.chunkIds() != null ? request.chunkIds().get(i) : UUID.randomUUID();
                
                List<Float> cachedEmbedding = cacheService.getCachedEmbedding(
                    request.tenantId(), text, modelName);
                
                if (cachedEmbedding != null) {
                    results.add(EmbeddingResult.success(chunkId, text, cachedEmbedding));
                    logger.debug("Using cached embedding for chunk: {}", chunkId);
                } else {
                    textsToProcess.add(text);
                    indexesToProcess.add(i);
                }
            }
            
            // Generate embeddings for non-cached texts
            if (!textsToProcess.isEmpty()) {
                List<EmbeddingResult> generatedResults = generateNewEmbeddings(
                    client, textsToProcess, indexesToProcess, request, modelName);
                results.addAll(generatedResults);
            }
            
            // Sort results by original order
            results.sort((a, b) -> {
                int indexA = request.chunkIds() != null ? 
                    request.chunkIds().indexOf(a.chunkId()) : 0;
                int indexB = request.chunkIds() != null ? 
                    request.chunkIds().indexOf(b.chunkId()) : 0;
                return Integer.compare(indexA, indexB);
            });
            
            // Store vectors in Redis
            storeVectors(request.tenantId(), request.documentId(), results, modelName);
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            // Determine response status
            boolean hasFailures = results.stream().anyMatch(r -> "FAILED".equals(r.status()));
            boolean hasSuccess = results.stream().anyMatch(r -> "SUCCESS".equals(r.status()));
            
            if (hasFailures && hasSuccess) {
                return EmbeddingResponse.partial(
                    request.tenantId(), request.documentId(), modelName,
                    results, getDimension(results), processingTime);
            } else if (hasSuccess) {
                return EmbeddingResponse.success(
                    request.tenantId(), request.documentId(), modelName,
                    results, getDimension(results), processingTime);
            } else {
                return EmbeddingResponse.failure(
                    request.tenantId(), request.documentId(), modelName,
                    "All embedding generations failed", processingTime);
            }
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            logger.error("Failed to generate embeddings for tenant: {}, document: {}", 
                        request.tenantId(), request.documentId(), e);
            
            return EmbeddingResponse.failure(
                request.tenantId(), request.documentId(), 
                getEffectiveModelName(request.modelName()),
                e.getMessage(), processingTime);
        }
    }
    
    /**
     * Generate embeddings asynchronously.
     */
    public CompletableFuture<EmbeddingResponse> generateEmbeddingsAsync(EmbeddingRequest request) {
        return CompletableFuture.supplyAsync(() -> generateEmbeddings(request), executorService);
    }
    
    /**
     * Generate embedding for a single text.
     */
    public List<Float> generateEmbedding(UUID tenantId, String text, String modelName) {
        EmbeddingRequest request = EmbeddingRequest.singleText(
            tenantId, text, modelName, UUID.randomUUID(), null);
        
        EmbeddingResponse response = generateEmbeddings(request);
        
        if (response.embeddings().isEmpty()) {
            throw new EmbeddingException("Failed to generate embedding for text");
        }
        
        EmbeddingResult result = response.embeddings().get(0);
        if (!"SUCCESS".equals(result.status())) {
            throw new EmbeddingException("Embedding generation failed: " + result.error());
        }
        
        return result.embedding();
    }
    
    private List<EmbeddingResult> generateNewEmbeddings(
            EmbeddingModel client, List<String> texts, List<Integer> indexes,
            EmbeddingRequest request, String modelName) {
        
        try {
            // Generate embeddings directly from text strings
            org.springframework.ai.embedding.EmbeddingResponse springResponse = client.embedForResponse(texts);
            
            List<EmbeddingResult> results = new ArrayList<>();
            
            for (int i = 0; i < springResponse.getResults().size(); i++) {
                try {
                    var embedding = springResponse.getResults().get(i);
                    int originalIndex = indexes.get(i);
                    String text = texts.get(i);
                    UUID chunkId = request.chunkIds() != null ? 
                        request.chunkIds().get(originalIndex) : UUID.randomUUID();
                    
                    List<Float> embeddingVector = embedding.getOutput().stream()
                        .map(Double::floatValue)
                        .toList();
                    
                    // Cache the embedding
                    cacheService.cacheEmbedding(request.tenantId(), text, modelName, embeddingVector);
                    
                    results.add(EmbeddingResult.success(chunkId, text, embeddingVector));
                    
                } catch (Exception e) {
                    logger.error("Failed to process embedding result at index: {}", i, e);
                    int originalIndex = indexes.get(i);
                    String text = texts.get(i);
                    UUID chunkId = request.chunkIds() != null ? 
                        request.chunkIds().get(originalIndex) : UUID.randomUUID();
                    
                    results.add(EmbeddingResult.failure(chunkId, text, e.getMessage()));
                }
            }
            
            return results;
            
        } catch (Exception e) {
            logger.error("Failed to generate embeddings", e);
            
            // Return failure results for all texts
            List<EmbeddingResult> results = new ArrayList<>();
            for (int i = 0; i < texts.size(); i++) {
                int originalIndex = indexes.get(i);
                String text = texts.get(i);
                UUID chunkId = request.chunkIds() != null ? 
                    request.chunkIds().get(originalIndex) : UUID.randomUUID();
                
                results.add(EmbeddingResult.failure(chunkId, text, e.getMessage()));
            }
            
            return results;
        }
    }
    
    private void storeVectors(UUID tenantId, UUID documentId, 
                            List<EmbeddingResult> results, String modelName) {
        try {
            List<EmbeddingResult> successfulResults = results.stream()
                .filter(r -> "SUCCESS".equals(r.status()))
                .toList();
                
            if (!successfulResults.isEmpty()) {
                vectorStorageService.storeEmbeddings(tenantId, modelName, successfulResults);
            }
        } catch (Exception e) {
            logger.error("Failed to store vectors for tenant: {}, document: {}", 
                        tenantId, documentId, e);
            // Don't fail the entire operation if vector storage fails
        }
    }
    
    private String getEffectiveModelName(String requestedModel) {
        if (requestedModel != null && clientRegistry.hasModel(requestedModel)) {
            return requestedModel;
        }
        return clientRegistry.defaultModelName();
    }
    
    private int getDimension(List<EmbeddingResult> results) {
        return results.stream()
            .filter(r -> "SUCCESS".equals(r.status()) && r.embedding() != null)
            .findFirst()
            .map(r -> r.embedding().size())
            .orElse(0);
    }
}
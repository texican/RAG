package com.byo.rag.core.client;

import com.byo.rag.shared.exception.EmbeddingException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom error decoder for embedding service client.
 */
public class EmbeddingServiceErrorDecoder implements ErrorDecoder {
    
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingServiceErrorDecoder.class);
    private final ErrorDecoder defaultErrorDecoder = new Default();
    
    @Override
    public Exception decode(String methodKey, Response response) {
        String message = String.format("Embedding service error: %s %d %s", 
                                     methodKey, response.status(), response.reason());
        
        logger.error("Embedding service call failed: {}", message);
        
        return switch (response.status()) {
            case 400 -> new EmbeddingException("Bad request to embedding service: " + message);
            case 401 -> new EmbeddingException("Unauthorized access to embedding service: " + message);
            case 403 -> new EmbeddingException("Forbidden access to embedding service: " + message);
            case 404 -> new EmbeddingException("Embedding service endpoint not found: " + message);
            case 429 -> new EmbeddingException("Embedding service rate limit exceeded: " + message);
            case 500 -> new EmbeddingException("Embedding service internal error: " + message);
            case 503 -> new EmbeddingException("Embedding service unavailable: " + message);
            default -> defaultErrorDecoder.decode(methodKey, response);
        };
    }
}
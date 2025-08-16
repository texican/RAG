package com.enterprise.rag.shared.exception;

public class EmbeddingException extends RagException {
    
    public EmbeddingException(String message) {
        super("Embedding operation failed: " + message, "EMBEDDING_FAILED");
    }
    
    public EmbeddingException(String message, Throwable cause) {
        super("Embedding operation failed: " + message, "EMBEDDING_FAILED", cause);
    }
}
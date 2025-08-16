package com.enterprise.rag.shared.exception;

import java.util.UUID;

public class DocumentProcessingException extends RagException {
    
    public DocumentProcessingException(String message) {
        super("Document processing failed: " + message, "DOCUMENT_PROCESSING_FAILED");
    }
    
    public DocumentProcessingException(UUID documentId, String message) {
        super("Document processing failed for document " + documentId + ": " + message, "DOCUMENT_PROCESSING_FAILED");
    }
    
    public DocumentProcessingException(UUID documentId, String message, Throwable cause) {
        super("Document processing failed for document " + documentId + ": " + message, "DOCUMENT_PROCESSING_FAILED", cause);
    }
    
    public DocumentProcessingException(String message, Throwable cause) {
        super("Document processing failed: " + message, "DOCUMENT_PROCESSING_FAILED", cause);
    }
}
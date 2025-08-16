package com.enterprise.rag.shared.exception;

import java.util.UUID;

public class DocumentNotFoundException extends RagException {
    
    public DocumentNotFoundException(UUID documentId) {
        super("Document not found with ID: " + documentId, "DOCUMENT_NOT_FOUND");
    }
    
    public DocumentNotFoundException(String filename, UUID tenantId) {
        super("Document not found with filename: " + filename + " for tenant: " + tenantId, "DOCUMENT_NOT_FOUND");
    }
}
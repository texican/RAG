package com.enterprise.rag.shared.exception;

import java.util.UUID;

/**
 * Exception thrown when a requested document cannot be found in the system.
 * <p>
 * This exception is raised when document retrieval operations fail due to
 * non-existent document IDs, invalid file references, or access control violations.
 * It provides specific error context for debugging and user feedback.
 * 
 * <h2>Document Lookup Scenarios</h2>
 * <ul>
 *   <li>Document ID lookup in database queries</li>
 *   <li>File system storage retrieval operations</li>
 *   <li>Document metadata validation checks</li>
 *   <li>Tenant-scoped document access validation</li>
 * </ul>
 * 
 * <h2>Common Causes</h2>
 * <ul>
 *   <li>Invalid or non-existent document UUID provided</li>
 *   <li>Document deleted but still referenced in queries</li>
 *   <li>Cross-tenant document access attempts</li>
 *   <li>File system storage corruption or migration issues</li>
 *   <li>Database synchronization problems</li>
 * </ul>
 * 
 * <h2>Error Information</h2>
 * Provides detailed context including:
 * <ul>
 *   <li>Document ID or filename that could not be found</li>
 *   <li>Tenant context for multi-tenant installations</li>
 *   <li>Operation type that triggered the lookup</li>
 *   <li>Standardized error code \"DOCUMENT_NOT_FOUND\"</li>
 * </ul>
 * 
 * @author Enterprise RAG Development Team
 * @version 0.8.0
 * @since 0.1.0
 * @see RagException
 */
public class DocumentNotFoundException extends RagException {
    
    public DocumentNotFoundException(UUID documentId) {
        super("Document not found with ID: " + documentId, "DOCUMENT_NOT_FOUND");
    }
    
    public DocumentNotFoundException(String filename, UUID tenantId) {
        super("Document not found with filename: " + filename + " for tenant: " + tenantId, "DOCUMENT_NOT_FOUND");
    }
}
package com.enterprise.rag.shared.exception;

import java.util.UUID;

/**
 * Exception thrown when document processing operations fail.
 * <p>
 * This exception is raised during document ingestion, parsing, chunking, or 
 * transformation operations. It provides detailed error information for
 * troubleshooting document processing pipeline failures.
 * 
 * <h2>Processing Pipeline Stages</h2>
 * <ul>
 *   <li>File upload and validation</li>
 *   <li>Text extraction from various formats (PDF, DOCX, TXT)</li>
 *   <li>Content chunking and segmentation</li>
 *   <li>Metadata extraction and enrichment</li>
 *   <li>Database persistence operations</li>
 * </ul>
 * 
 * <h2>Common Failure Scenarios</h2>
 * <ul>
 *   <li>Unsupported file formats or corrupted files</li>
 *   <li>Text extraction failures from complex documents</li>
 *   <li>Memory exhaustion during large file processing</li>
 *   <li>Database connection issues during persistence</li>
 *   <li>Kafka message publishing failures</li>
 * </ul>
 * 
 * <h2>Error Context</h2>
 * Provides comprehensive error information:
 * <ul>
 *   <li>Document ID for traceability across services</li>
 *   <li>Processing stage where failure occurred</li>
 *   <li>Root cause exception chain for debugging</li>
 *   <li>Standardized error code \"DOCUMENT_PROCESSING_FAILED\"</li>
 * </ul>
 * 
 * @author Enterprise RAG Development Team
 * @version 0.8.0
 * @since 0.1.0
 * @see RagException
 */
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
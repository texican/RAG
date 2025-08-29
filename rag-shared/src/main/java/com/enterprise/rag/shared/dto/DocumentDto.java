package com.enterprise.rag.shared.dto;

import com.enterprise.rag.shared.entity.Document;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Data Transfer Objects for document operations in the Enterprise RAG System.
 * <p>
 * This sealed interface defines all document-related DTOs used across the RAG microservices.
 * It provides type-safe representations for document upload, processing, and retrieval operations
 * with comprehensive validation and multi-tenant support.
 * </p>
 * 
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Sealed Interface</strong> - Compile-time exhaustive pattern matching</li>
 *   <li><strong>Validation Support</strong> - Bean validation annotations for request DTOs</li>
 *   <li><strong>Multi-tenant Aware</strong> - All operations respect tenant boundaries</li>
 *   <li><strong>Processing States</strong> - Track document processing lifecycle</li>
 * </ul>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Upload a document
 * var uploadRequest = new DocumentDto.UploadDocumentRequest(
 *     multipartFile,
 *     Map.of("category", "technical", "priority", "high")
 * );
 * 
 * // Process response
 * switch (documentDto) {
 *     case DocumentDto.DocumentResponse(var id, var filename, ...) -> 
 *         log.info("Document {} uploaded with ID {}", filename, id);
 *     case DocumentDto.DocumentProcessingUpdate(var id, var status, ...) ->
 *         log.info("Document {} processing status: {}", id, status);
 * }
 * }</pre>
 * 
 * @author Enterprise RAG Development Team
 * @since 1.0.0
 * @see com.enterprise.rag.shared.entity.Document
 */
public sealed interface DocumentDto permits 
    DocumentDto.UploadDocumentRequest,
    DocumentDto.UpdateDocumentRequest,
    DocumentDto.DocumentResponse,
    DocumentDto.DocumentSummary,
    DocumentDto.DocumentProcessingUpdate {

    /**
     * Request DTO for uploading a new document to the system.
     * <p>
     * This record encapsulates the file and associated metadata for document upload operations.
     * The file parameter is intentionally typed as Object to accommodate different implementations
     * (MultipartFile in web contexts, File in batch processing, etc.).
     * </p>
     * 
     * @param file the file to upload (typically MultipartFile in REST endpoints)
     * @param metadata optional key-value pairs for document categorization and metadata
     */
    record UploadDocumentRequest(
        @NotNull Object file, // Will be MultipartFile in document service
        Map<String, Object> metadata
    ) implements DocumentDto {}

    /**
     * Request DTO for updating an existing document's metadata.
     * <p>
     * Allows updating document filename and metadata without re-uploading the file content.
     * All updates respect multi-tenant boundaries and require appropriate permissions.
     * </p>
     * 
     * @param filename new filename for the document (must not be blank)
     * @param metadata updated metadata key-value pairs
     */
    record UpdateDocumentRequest(
        @NotBlank String filename,
        Map<String, Object> metadata
    ) implements DocumentDto {}

    /**
     * Complete document response with all details and processing information.
     * <p>
     * This comprehensive response includes file metadata, processing status,
     * chunking information, and audit fields. Used for detailed document retrieval
     * and status monitoring operations.
     * </p>
     * 
     * @param id unique document identifier
     * @param filename current filename in the system
     * @param originalFilename original filename when uploaded
     * @param fileSize file size in bytes
     * @param contentType MIME content type
     * @param documentType categorized document type (PDF, DOCX, etc.)
     * @param processingStatus current processing state (PENDING, PROCESSING, COMPLETED, FAILED)
     * @param processingMessage detailed processing status message
     * @param chunkCount number of text chunks created from this document
     * @param embeddingModel AI model used for embedding generation
     * @param metadata custom metadata key-value pairs
     * @param createdAt document creation timestamp
     * @param updatedAt last modification timestamp
     * @param uploadedBy summary of the user who uploaded the document
     */
    record DocumentResponse(
        UUID id,
        String filename,
        String originalFilename,
        Long fileSize,
        String contentType,
        Document.DocumentType documentType,
        Document.ProcessingStatus processingStatus,
        String processingMessage,
        Integer chunkCount,
        String embeddingModel,
        Map<String, Object> metadata,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        UserDto.UserSummary uploadedBy
    ) implements DocumentDto {}

    /**
     * Lightweight document summary for list operations and dashboards.
     * <p>
     * Contains essential document information without heavy metadata,
     * optimized for efficient listing and overview displays.
     * </p>
     * 
     * @param id unique document identifier
     * @param filename current filename
     * @param documentType categorized document type
     * @param processingStatus current processing state
     * @param fileSize file size in bytes
     * @param chunkCount number of text chunks
     * @param createdAt document creation timestamp
     */
    record DocumentSummary(
        UUID id,
        String filename,
        Document.DocumentType documentType,
        Document.ProcessingStatus processingStatus,
        Long fileSize,
        Integer chunkCount,
        LocalDateTime createdAt
    ) implements DocumentDto {}

    /**
     * Processing update notification for document pipeline status changes.
     * <p>
     * Used by Kafka events and WebSocket notifications to communicate
     * processing progress to clients and other microservices.
     * </p>
     * 
     * @param documentId the document being processed
     * @param status new processing status
     * @param message detailed status or error message
     * @param chunkCount current chunk count (if processing completed)
     * @param updatedAt timestamp of this update
     */
    record DocumentProcessingUpdate(
        UUID documentId,
        Document.ProcessingStatus status,
        String message,
        Integer chunkCount,
        LocalDateTime updatedAt
    ) implements DocumentDto {}
}
package com.enterprise.rag.shared.dto;

import com.enterprise.rag.shared.entity.Document;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public sealed interface DocumentDto permits 
    DocumentDto.UploadDocumentRequest,
    DocumentDto.UpdateDocumentRequest,
    DocumentDto.DocumentResponse,
    DocumentDto.DocumentSummary,
    DocumentDto.DocumentProcessingUpdate {

    record UploadDocumentRequest(
        @NotNull Object file, // Will be MultipartFile in document service
        Map<String, Object> metadata
    ) implements DocumentDto {}

    record UpdateDocumentRequest(
        @NotBlank String filename,
        Map<String, Object> metadata
    ) implements DocumentDto {}

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

    record DocumentSummary(
        UUID id,
        String filename,
        Document.DocumentType documentType,
        Document.ProcessingStatus processingStatus,
        Long fileSize,
        Integer chunkCount,
        LocalDateTime createdAt
    ) implements DocumentDto {}

    record DocumentProcessingUpdate(
        UUID documentId,
        Document.ProcessingStatus status,
        String message,
        Integer chunkCount,
        LocalDateTime updatedAt
    ) implements DocumentDto {}
}
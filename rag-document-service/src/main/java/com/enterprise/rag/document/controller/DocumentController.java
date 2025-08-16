package com.enterprise.rag.document.controller;

import com.enterprise.rag.document.service.DocumentService;
import com.enterprise.rag.shared.dto.DocumentDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Document Management", description = "Document upload and processing operations")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    @Operation(summary = "Upload document", description = "Upload a document for processing and indexing")
    public ResponseEntity<DocumentDto.DocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "metadata", required = false) String metadataJson,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        // Create a simple upload request - we'll create dummy tenant/user for now
        DocumentDto.UploadDocumentRequest request = new DocumentDto.UploadDocumentRequest(
            file, 
            null // metadata will be parsed from JSON if needed
        );
        
        // For now, pass null for tenant and user - we'll fix this properly later
        DocumentDto.DocumentResponse response = documentService.uploadDocument(request, null, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{documentId}")
    @Operation(summary = "Get document", description = "Get document information by ID")
    public ResponseEntity<DocumentDto.DocumentResponse> getDocument(
            @PathVariable UUID documentId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        DocumentDto.DocumentResponse response = documentService.getDocument(documentId, UUID.fromString(tenantId));
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List documents", description = "List documents for a tenant")
    public ResponseEntity<Page<DocumentDto.DocumentSummary>> getDocuments(
            @RequestHeader("X-Tenant-ID") String tenantId,
            Pageable pageable) {
        
        Page<DocumentDto.DocumentSummary> response = documentService.getDocumentsByTenant(UUID.fromString(tenantId), pageable);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{documentId}")
    @Operation(summary = "Delete document", description = "Delete a document and its chunks")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable UUID documentId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        documentService.deleteDocument(documentId, UUID.fromString(tenantId));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{documentId}")
    @Operation(summary = "Update document", description = "Update document metadata")
    public ResponseEntity<DocumentDto.DocumentResponse> updateDocument(
            @PathVariable UUID documentId,
            @RequestBody @Valid DocumentDto.UpdateDocumentRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        DocumentDto.DocumentResponse response = documentService.updateDocument(documentId, request, UUID.fromString(tenantId));
        return ResponseEntity.ok(response);
    }
}
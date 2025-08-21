package com.enterprise.rag.document.controller;

import com.enterprise.rag.document.service.DocumentService;
import com.enterprise.rag.shared.dto.DocumentDto;
import com.enterprise.rag.shared.entity.Tenant;
import com.enterprise.rag.shared.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Document Management", description = "Document upload, processing, and management operations")
@Validated
public class DocumentController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a document", description = "Upload a document file for processing and text extraction")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Document uploaded successfully", 
                    content = @Content(schema = @Schema(implementation = DocumentDto.DocumentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid file or request"),
        @ApiResponse(responseCode = "413", description = "File too large"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<DocumentDto.DocumentResponse> uploadDocument(
            @Parameter(description = "Document file to upload", required = true)
            @RequestParam("file") @NotNull MultipartFile file,
            
            @Parameter(description = "Additional metadata for the document")
            @RequestParam(value = "metadata", required = false) Map<String, Object> metadata,
            
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader("X-Tenant-ID") @NotNull UUID tenantId) {

        logger.info("Uploading document: {} for tenant: {}", file.getOriginalFilename(), tenantId);

        // Create upload request
        DocumentDto.UploadDocumentRequest request = new DocumentDto.UploadDocumentRequest(file, metadata);
        
        // For now, use dummy tenant and user - in production this would come from security context
        Tenant tenant = createDummyTenant(tenantId);
        User user = createDummyUser(tenant);

        DocumentDto.DocumentResponse response = documentService.uploadDocument(request, tenant, user);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List documents", description = "Get paginated list of documents for a tenant")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Documents retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    public ResponseEntity<Page<DocumentDto.DocumentSummary>> getDocuments(
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader("X-Tenant-ID") @NotNull UUID tenantId,
            
            @PageableDefault(size = 20) Pageable pageable) {

        logger.debug("Fetching documents for tenant: {} with pagination: {}", tenantId, pageable);

        Page<DocumentDto.DocumentSummary> documents = documentService.getDocumentsByTenant(tenantId, pageable);
        
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/{documentId}")
    @Operation(summary = "Get document", description = "Get detailed information about a specific document")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Document retrieved successfully",
                    content = @Content(schema = @Schema(implementation = DocumentDto.DocumentResponse.class))),
        @ApiResponse(responseCode = "404", description = "Document not found")
    })
    public ResponseEntity<DocumentDto.DocumentResponse> getDocument(
            @Parameter(description = "Document ID", required = true)
            @PathVariable @NotNull UUID documentId,
            
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader("X-Tenant-ID") @NotNull UUID tenantId) {

        logger.debug("Fetching document: {} for tenant: {}", documentId, tenantId);

        DocumentDto.DocumentResponse document = documentService.getDocument(documentId, tenantId);
        
        return ResponseEntity.ok(document);
    }

    @PutMapping("/{documentId}")
    @Operation(summary = "Update document", description = "Update document metadata and filename")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Document updated successfully",
                    content = @Content(schema = @Schema(implementation = DocumentDto.DocumentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "Document not found")
    })
    public ResponseEntity<DocumentDto.DocumentResponse> updateDocument(
            @Parameter(description = "Document ID", required = true)
            @PathVariable @NotNull UUID documentId,
            
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader("X-Tenant-ID") @NotNull UUID tenantId,
            
            @Parameter(description = "Document update request", required = true)
            @Valid @RequestBody DocumentDto.UpdateDocumentRequest request) {

        logger.info("Updating document: {} for tenant: {}", documentId, tenantId);

        DocumentDto.DocumentResponse response = documentService.updateDocument(documentId, request, tenantId);
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{documentId}")
    @Operation(summary = "Delete document", description = "Delete a document and all its associated data")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Document deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Document not found")
    })
    public ResponseEntity<Void> deleteDocument(
            @Parameter(description = "Document ID", required = true)
            @PathVariable @NotNull UUID documentId,
            
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader("X-Tenant-ID") @NotNull UUID tenantId) {

        logger.info("Deleting document: {} for tenant: {}", documentId, tenantId);

        documentService.deleteDocument(documentId, tenantId);
        
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    @Operation(summary = "Get document statistics", description = "Get document count and storage usage for a tenant")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    })
    public ResponseEntity<DocumentStatsResponse> getDocumentStats(
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader("X-Tenant-ID") @NotNull UUID tenantId) {

        logger.debug("Fetching document stats for tenant: {}", tenantId);

        long documentCount = documentService.getDocumentCountByTenant(tenantId);
        long storageUsage = documentService.getStorageUsageByTenant(tenantId);

        DocumentStatsResponse stats = new DocumentStatsResponse(documentCount, storageUsage);
        
        return ResponseEntity.ok(stats);
    }

    // Helper record for stats response
    public record DocumentStatsResponse(
        long totalDocuments,
        long storageUsageBytes
    ) {}

    // Temporary helper methods for testing - in production these would come from security context
    private Tenant createDummyTenant(UUID tenantId) {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("Test Company");
        tenant.setSlug("test-company");
        tenant.setStatus(Tenant.TenantStatus.ACTIVE);
        tenant.setMaxDocuments(1000);
        tenant.setMaxStorageMb(1024L); // 1GB
        return tenant;
    }
    
    private User createDummyUser(Tenant tenant) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test@test-company.com");
        user.setTenant(tenant);
        user.setRole(User.UserRole.USER);
        user.setStatus(User.UserStatus.ACTIVE);
        return user;
    }
}
/**
 * REST API controllers for document management operations.
 * 
 * <p>This package contains REST controllers that provide the public API
 * for document management operations in the Enterprise RAG System. Controllers
 * handle document upload, processing status monitoring, content retrieval,
 * and comprehensive document lifecycle management with proper security
 * and multi-tenant isolation.</p>
 * 
 * <h2>Controller Architecture</h2>
 * <p>Document controllers implement comprehensive document management:</p>
 * <ul>
 *   <li><strong>Document Controller</strong> - Main document management endpoints</li>
 *   <li><strong>Upload Controller</strong> - File upload and batch upload operations</li>
 *   <li><strong>Content Controller</strong> - Document content retrieval and streaming</li>
 *   <li><strong>Processing Controller</strong> - Processing status and monitoring</li>
 *   <li><strong>Exception Handler</strong> - Centralized error handling and responses</li>
 * </ul>
 * 
 * <h2>File Upload and Management</h2>
 * <p>Comprehensive file upload and document management:</p>
 * <ul>
 *   <li><strong>Multi-Format Upload</strong> - Support for various document formats</li>
 *   <li><strong>Batch Upload</strong> - Multiple file upload in single request</li>
 *   <li><strong>Upload Validation</strong> - File format, size, and content validation</li>
 *   <li><strong>Progress Tracking</strong> - Real-time upload progress monitoring</li>
 *   <li><strong>Metadata Extraction</strong> - Automatic metadata extraction during upload</li>
 *   <li><strong>Virus Scanning</strong> - Automatic malware detection and prevention</li>
 * </ul>
 * 
 * <h2>Multi-Tenant Security</h2>
 * <p>Controllers implement comprehensive multi-tenant security:</p>
 * <ul>
 *   <li><strong>Tenant Isolation</strong> - Complete document isolation between tenants</li>
 *   <li><strong>Access Control</strong> - Fine-grained document access permissions</li>
 *   <li><strong>Authentication</strong> - JWT-based user authentication</li>
 *   <li><strong>Authorization</strong> - Role-based access control for document operations</li>
 *   <li><strong>Audit Logging</strong> - Comprehensive audit trails for all operations</li>
 * </ul>
 * 
 * <h2>Document Processing API</h2>
 * <p>Complete document processing lifecycle management:</p>
 * <ul>
 *   <li><strong>Processing Status</strong> - Real-time processing status monitoring</li>
 *   <li><strong>Content Retrieval</strong> - Processed document content access</li>
 *   <li><strong>Chunk Management</strong> - Document chunk retrieval and management</li>
 *   <li><strong>Metadata Access</strong> - Document metadata and properties</li>
 *   <li><strong>Processing Control</strong> - Processing retry and cancellation</li>
 * </ul>
 * 
 * <h2>Request/Response Processing</h2>
 * <p>Advanced request and response handling:</p>
 * <ul>
 *   <li><strong>DTO Validation</strong> - Comprehensive validation of document requests</li>
 *   <li><strong>Response Formatting</strong> - Structured document response formatting</li>
 *   <li><strong>Error Handling</strong> - Standardized error responses with details</li>
 *   <li><strong>Content Negotiation</strong> - Multiple response format support</li>
 *   <li><strong>Compression</strong> - Response compression for large document data</li>
 * </ul>
 * 
 * <h2>Streaming and Large File Support</h2>
 * <p>Optimized handling of large documents and streaming:</p>
 * <ul>
 *   <li><strong>Streaming Upload</strong> - Chunked upload for large files</li>
 *   <li><strong>Streaming Download</strong> - Efficient large file download</li>
 *   <li><strong>Content Streaming</strong> - Streaming document content retrieval</li>
 *   <li><strong>Progress Monitoring</strong> - Real-time progress updates</li>
 *   <li><strong>Resume Support</strong> - Upload resume capabilities</li>
 * </ul>
 * 
 * <h2>Performance Optimization</h2>
 * <p>Controllers optimized for high-performance document operations:</p>
 * <ul>
 *   <li><strong>Async Processing</strong> - Non-blocking document processing initiation</li>
 *   <li><strong>Caching Headers</strong> - Appropriate cache control for document data</li>
 *   <li><strong>Compression</strong> - Response compression for bandwidth optimization</li>
 *   <li><strong>Connection Keep-Alive</strong> - Efficient HTTP connection management</li>
 *   <li><strong>Batch Operations</strong> - Support for bulk document operations</li>
 * </ul>
 * 
 * <h2>API Documentation</h2>
 * <p>Comprehensive API documentation and examples:</p>
 * <ul>
 *   <li><strong>OpenAPI Specification</strong> - Complete API documentation</li>
 *   <li><strong>Upload Examples</strong> - Sample upload requests with various formats</li>
 *   <li><strong>Response Examples</strong> - Sample responses with explanations</li>
 *   <li><strong>Error Documentation</strong> - Complete error scenario documentation</li>
 *   <li><strong>Integration Guide</strong> - Client integration guidelines</li>
 * </ul>
 * 
 * <h2>Monitoring and Observability</h2>
 * <p>Controllers include comprehensive monitoring:</p>
 * <ul>
 *   <li><strong>Request Metrics</strong> - HTTP request metrics and timing</li>
 *   <li><strong>Upload Metrics</strong> - File upload performance metrics</li>
 *   <li><strong>Processing Metrics</strong> - Document processing performance</li>
 *   <li><strong>Error Tracking</strong> - Comprehensive error tracking and alerting</li>
 *   <li><strong>Usage Analytics</strong> - Document usage patterns and insights</li>
 * </ul>
 * 
 * <h2>Integration Patterns</h2>
 * <p>Controllers integrate with the complete document processing ecosystem:</p>
 * <ul>
 *   <li><strong>Service Layer Integration</strong> - Clean separation with service layer</li>
 *   <li><strong>Event Publishing</strong> - Document event publishing for processing</li>
 *   <li><strong>Circuit Breaker Integration</strong> - Resilience patterns for external calls</li>
 *   <li><strong>Distributed Tracing</strong> - End-to-end request tracing</li>
 * </ul>
 * 
 * <h2>API Endpoints</h2>
 * <p>Complete document management API specification:</p>
 * <ul>
 *   <li><strong>POST /api/v1/documents/upload</strong> - Single file upload</li>
 *   <li><strong>POST /api/v1/documents/batch-upload</strong> - Multiple file upload</li>
 *   <li><strong>GET /api/v1/documents</strong> - List documents with filtering</li>
 *   <li><strong>GET /api/v1/documents/{id}</strong> - Get document details</li>
 *   <li><strong>GET /api/v1/documents/{id}/content</strong> - Retrieve document content</li>
 *   <li><strong>GET /api/v1/documents/{id}/chunks</strong> - Get document chunks</li>
 *   <li><strong>GET /api/v1/documents/{id}/status</strong> - Processing status</li>
 *   <li><strong>DELETE /api/v1/documents/{id}</strong> - Delete document</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @RestController
 * @RequestMapping("/api/v1/documents")
 * @Validated
 * @Slf4j
 * public class DocumentController {
 *     
 *     private final DocumentService documentService;
 *     private final FileStorageService fileStorageService;
 *     private final DocumentProcessingService processingService;
 *     
 *     @PostMapping("/upload")
 *     @PreAuthorize("hasRole('USER')")
 *     public ResponseEntity<DocumentUploadResponse> uploadDocument(
 *             @RequestParam("file") MultipartFile file,
 *             @RequestParam(value = "metadata", required = false) String metadataJson,
 *             @RequestHeader("X-Tenant-ID") String tenantId,
 *             Authentication authentication) {
 *         
 *         try {
 *             // Validate file
 *             validateUploadFile(file);
 *             
 *             // Parse metadata
 *             Map<String, Object> metadata = parseMetadata(metadataJson);
 *             
 *             // Create document upload request
 *             DocumentUploadRequest request = DocumentUploadRequest.builder()
 *                 .fileName(file.getOriginalFilename())
 *                 .contentType(file.getContentType())
 *                 .fileSize(file.getSize())
 *                 .metadata(metadata)
 *                 .build();
 *             
 *             // Process upload
 *             Document document = documentService.uploadDocument(
 *                 tenantId, 
 *                 request, 
 *                 file.getInputStream()
 *             );
 *             
 *             // Start async processing
 *             processingService.processDocumentAsync(tenantId, document.getId());
 *             
 *             // Build response
 *             DocumentUploadResponse response = DocumentUploadResponse.builder()
 *                 .documentId(document.getId())
 *                 .fileName(document.getFileName())
 *                 .status(document.getStatus())
 *                 .uploadedAt(document.getCreatedAt())
 *                 .processingUrl("/api/v1/documents/" + document.getId() + "/status")
 *                 .build();
 *             
 *             // Add response headers
 *             HttpHeaders headers = new HttpHeaders();
 *             headers.add("Location", "/api/v1/documents/" + document.getId());
 *             headers.add("X-Document-ID", document.getId());
 *             
 *             // Log upload
 *             auditService.logDocumentUpload(
 *                 tenantId, 
 *                 authentication.getName(), 
 *                 document.getId(),
 *                 file.getOriginalFilename()
 *             );
 *             
 *             return ResponseEntity.status(HttpStatus.CREATED)
 *                 .headers(headers)
 *                 .body(response);
 *                 
 *         } catch (InvalidFileException e) {
 *             return ResponseEntity.badRequest()
 *                 .body(DocumentUploadResponse.error(e.getMessage()));
 *         } catch (StorageException e) {
 *             log.error("Storage error during upload: {}", e.getMessage(), e);
 *             return ResponseEntity.status(HttpStatus.INSUFFICIENT_STORAGE)
 *                 .body(DocumentUploadResponse.error("Storage error"));
 *         }
 *     }
 *     
 *     @GetMapping("/{id}/status")
 *     @PreAuthorize("hasRole('USER')")
 *     public ResponseEntity<DocumentStatusResponse> getProcessingStatus(
 *             @PathVariable String id,
 *             @RequestHeader("X-Tenant-ID") String tenantId) {
 *         
 *         Optional<Document> document = documentService.findByTenantAndId(tenantId, id);
 *         
 *         if (document.isEmpty()) {
 *             return ResponseEntity.notFound().build();
 *         }
 *         
 *         DocumentStatusResponse response = DocumentStatusResponse.builder()
 *             .documentId(id)
 *             .status(document.get().getStatus())
 *             .processingStartedAt(document.get().getProcessingStartedAt())
 *             .processingCompletedAt(document.get().getProcessingCompletedAt())
 *             .chunkCount(document.get().getChunks().size())
 *             .errorMessage(document.get().getErrorMessage())
 *             .build();
 *         
 *         return ResponseEntity.ok(response);
 *     }
 *     
 *     private void validateUploadFile(MultipartFile file) {
 *         if (file.isEmpty()) {
 *             throw new InvalidFileException("File is empty");
 *         }
 *         
 *         if (file.getSize() > MAX_FILE_SIZE) {
 *             throw new InvalidFileException("File size exceeds maximum limit");
 *         }
 *         
 *         if (!isValidFileType(file.getContentType())) {
 *             throw new InvalidFileException("Unsupported file type");
 *         }
 *     }
 * }
 * }</pre>
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 * @see org.springframework.web.bind.annotation Spring MVC annotations
 * @see org.springframework.web.multipart Multipart file handling
 * @see org.springframework.security.access.prepost Method security
 * @see com.byo.rag.document.service Document service layer
 */
package com.byo.rag.document.controller;
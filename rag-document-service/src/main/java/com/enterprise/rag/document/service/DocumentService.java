package com.enterprise.rag.document.service;

import com.enterprise.rag.document.repository.DocumentRepository;
import com.enterprise.rag.shared.dto.DocumentDto;
import com.enterprise.rag.shared.dto.TenantDto;
import com.enterprise.rag.shared.dto.UserDto;
import com.enterprise.rag.shared.entity.Document;
import com.enterprise.rag.shared.entity.DocumentChunk;
import com.enterprise.rag.shared.entity.Tenant;
import com.enterprise.rag.shared.entity.User;
import com.enterprise.rag.shared.exception.DocumentNotFoundException;
import com.enterprise.rag.shared.exception.DocumentProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class DocumentService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final DocumentChunkService chunkService;
    private final FileStorageService fileStorageService;
    private final TextExtractionService textExtractionService;
    private final DocumentProcessingKafkaService kafkaService;

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentChunkService chunkService,
            FileStorageService fileStorageService,
            TextExtractionService textExtractionService,
            DocumentProcessingKafkaService kafkaService) {
        this.documentRepository = documentRepository;
        this.chunkService = chunkService;
        this.fileStorageService = fileStorageService;
        this.textExtractionService = textExtractionService;
        this.kafkaService = kafkaService;
    }

    public DocumentDto.DocumentResponse uploadDocument(DocumentDto.UploadDocumentRequest request, 
                                                       Tenant tenant, User user) {
        MultipartFile file = (MultipartFile) request.file();
        
        // For testing - create dummy tenant if null
        if (tenant == null) {
            tenant = createDummyTenant();
        }
        if (user == null) {
            user = createDummyUser(tenant);
        }
        
        logger.info("Uploading document: {} for tenant: {}", file.getOriginalFilename(), tenant.getSlug());

        validateFile(file, tenant);

        Document.DocumentType documentType = textExtractionService.detectDocumentType(
            file.getOriginalFilename(), file.getContentType());

        Document document = new Document();
        document.setOriginalFilename(file.getOriginalFilename());
        document.setFilename(generateUniqueFilename(file.getOriginalFilename()));
        document.setFileSize(file.getSize());
        document.setContentType(file.getContentType());
        document.setDocumentType(documentType);
        document.setTenant(tenant);
        document.setUploadedBy(user);
        document.setProcessingStatus(Document.ProcessingStatus.PENDING);

        document = documentRepository.save(document);

        try {
            // Store file
            String filePath = fileStorageService.storeFile(file, tenant.getId(), document.getId());
            document.setFilePath(filePath);
            document = documentRepository.save(document);

            // Send for async processing
            kafkaService.sendDocumentForProcessing(document.getId());

            logger.info("Successfully uploaded document with ID: {}", document.getId());

        } catch (Exception e) {
            document.setProcessingStatus(Document.ProcessingStatus.FAILED);
            document.setProcessingMessage("Failed to store file: " + e.getMessage());
            documentRepository.save(document);
            throw new DocumentProcessingException(document.getId(), "Failed to upload document", e);
        }

        return mapToResponse(document);
    }

    @Async
    public void processDocument(UUID documentId) {
        logger.info("Processing document: {}", documentId);

        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new DocumentNotFoundException(documentId));

        try {
            document.setProcessingStatus(Document.ProcessingStatus.PROCESSING);
            document = documentRepository.save(document);

            // Extract text
            Path filePath = fileStorageService.loadFile(document.getFilePath());
            String extractedText = textExtractionService.extractText(filePath, document.getDocumentType());
            
            // Extract metadata
            Map<String, Object> metadata = textExtractionService.extractMetadata(filePath);
            
            document.setExtractedText(extractedText);
            document.setMetadata(com.enterprise.rag.shared.util.JsonUtils.toJson(metadata));

            // Create chunks with tenant configuration
            TenantDto.ChunkingConfig chunkingConfig = getTenantChunkingConfig(document.getTenant());
            List<DocumentChunk> chunks = chunkService.createChunks(document, extractedText, chunkingConfig);
            
            document.setChunkCount(chunks.size());
            document.setProcessingStatus(Document.ProcessingStatus.COMPLETED);
            document.setProcessingMessage("Successfully processed");

            document = documentRepository.save(document);

            // Send chunks for embedding generation
            kafkaService.sendChunksForEmbedding(chunks);

            logger.info("Successfully processed document: {} with {} chunks", documentId, chunks.size());

        } catch (Exception e) {
            logger.error("Failed to process document: {}", documentId, e);
            
            document.setProcessingStatus(Document.ProcessingStatus.FAILED);
            document.setProcessingMessage("Processing failed: " + e.getMessage());
            documentRepository.save(document);
        }
    }

    @Transactional(readOnly = true)
    public DocumentDto.DocumentResponse getDocument(UUID documentId, UUID tenantId) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new DocumentNotFoundException(documentId));

        validateTenantAccess(document, tenantId);
        return mapToResponse(document);
    }

    @Transactional(readOnly = true)
    public Page<DocumentDto.DocumentSummary> getDocumentsByTenant(UUID tenantId, Pageable pageable) {
        return documentRepository.findByTenantId(tenantId, pageable)
            .map(this::mapToSummary);
    }

    public DocumentDto.DocumentResponse updateDocument(UUID documentId, DocumentDto.UpdateDocumentRequest request, UUID tenantId) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new DocumentNotFoundException(documentId));

        validateTenantAccess(document, tenantId);

        document.setFilename(request.filename());
        if (request.metadata() != null) {
            document.setMetadata(com.enterprise.rag.shared.util.JsonUtils.toJson(request.metadata()));
        }

        document = documentRepository.save(document);
        return mapToResponse(document);
    }

    public void deleteDocument(UUID documentId, UUID tenantId) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new DocumentNotFoundException(documentId));

        validateTenantAccess(document, tenantId);

        try {
            // Delete chunks
            chunkService.deleteChunksByDocument(documentId);
            
            // Delete file
            if (document.getFilePath() != null) {
                fileStorageService.deleteFile(document.getFilePath());
            }
            
            // Delete document record
            documentRepository.delete(document);
            
            logger.info("Successfully deleted document: {}", documentId);

        } catch (Exception e) {
            logger.error("Failed to delete document: {}", documentId, e);
            throw new DocumentProcessingException(documentId, "Failed to delete document", e);
        }
    }

    @Transactional(readOnly = true)
    public long getDocumentCountByTenant(UUID tenantId) {
        return documentRepository.countByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public long getStorageUsageByTenant(UUID tenantId) {
        return fileStorageService.getTenantStorageUsage(tenantId);
    }

    private void validateFile(MultipartFile file, Tenant tenant) {
        if (file.isEmpty()) {
            throw new DocumentProcessingException("File is empty");
        }

        if (file.getSize() > textExtractionService.getMaxFileSize()) {
            throw new DocumentProcessingException("File size exceeds maximum allowed size");
        }

        if (!textExtractionService.isValidDocumentType(file.getOriginalFilename(), file.getContentType())) {
            throw new DocumentProcessingException("Unsupported file type");
        }

        // Check tenant limits
        long currentDocCount = getDocumentCountByTenant(tenant.getId());
        if (currentDocCount >= tenant.getMaxDocuments()) {
            throw new DocumentProcessingException("Tenant document limit exceeded");
        }

        long currentStorage = getStorageUsageByTenant(tenant.getId());
        long maxStorageBytes = tenant.getMaxStorageMb() * 1024 * 1024;
        if (currentStorage + file.getSize() > maxStorageBytes) {
            throw new DocumentProcessingException("Tenant storage limit exceeded");
        }
    }

    private void validateTenantAccess(Document document, UUID tenantId) {
        if (!document.getTenant().getId().equals(tenantId)) {
            throw new DocumentNotFoundException(document.getId());
        }
    }

    private String generateUniqueFilename(String originalFilename) {
        String extension = "";
        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            extension = originalFilename.substring(lastDotIndex);
        }
        return UUID.randomUUID().toString() + extension;
    }

    private TenantDto.ChunkingConfig getTenantChunkingConfig(Tenant tenant) {
        // Default chunking configuration - in real implementation, this would be stored in tenant config
        return new TenantDto.ChunkingConfig(512, 64, TenantDto.ChunkingStrategy.SEMANTIC);
    }

    private DocumentDto.DocumentResponse mapToResponse(Document document) {
        UserDto.UserSummary uploadedBy = new UserDto.UserSummary(
            document.getUploadedBy().getId(),
            document.getUploadedBy().getFirstName(),
            document.getUploadedBy().getLastName(),
            document.getUploadedBy().getEmail(),
            document.getUploadedBy().getRole(),
            document.getUploadedBy().getStatus()
        );

        Map<String, Object> metadata = null;
        if (document.getMetadata() != null) {
            metadata = com.enterprise.rag.shared.util.JsonUtils.fromJson(
                document.getMetadata(), 
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
            );
        }

        return new DocumentDto.DocumentResponse(
            document.getId(),
            document.getFilename(),
            document.getOriginalFilename(),
            document.getFileSize(),
            document.getContentType(),
            document.getDocumentType(),
            document.getProcessingStatus(),
            document.getProcessingMessage(),
            document.getChunkCount(),
            document.getEmbeddingModel(),
            metadata,
            document.getCreatedAt(),
            document.getUpdatedAt(),
            uploadedBy
        );
    }

    private DocumentDto.DocumentSummary mapToSummary(Document document) {
        return new DocumentDto.DocumentSummary(
            document.getId(),
            document.getFilename(),
            document.getDocumentType(),
            document.getProcessingStatus(),
            document.getFileSize(),
            document.getChunkCount(),
            document.getCreatedAt()
        );
    }
    
    // Temporary helper methods for testing
    private Tenant createDummyTenant() {
        Tenant tenant = new Tenant();
        tenant.setId(UUID.fromString("4307c59c-f0a7-4252-8b0c-13021e81928e"));
        tenant.setName("Test Company");
        tenant.setSlug("test-company");
        tenant.setStatus(Tenant.TenantStatus.ACTIVE);
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
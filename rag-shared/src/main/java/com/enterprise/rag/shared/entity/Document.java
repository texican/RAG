package com.enterprise.rag.shared.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "documents", indexes = {
    @Index(name = "idx_document_tenant", columnList = "tenant_id"),
    @Index(name = "idx_document_status", columnList = "processing_status"),
    @Index(name = "idx_document_type", columnList = "document_type"),
    @Index(name = "idx_document_uploaded_by", columnList = "uploaded_by_id")
})
public class Document extends BaseEntity {

    @NotBlank
    @Size(max = 255)
    @Column(name = "filename", nullable = false)
    private String filename;

    @NotBlank
    @Size(max = 255)
    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_type")
    private String contentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false)
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;

    @Column(name = "processing_message", columnDefinition = "TEXT")
    private String processingMessage;

    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    @Column(name = "chunk_count")
    private Integer chunkCount = 0;

    @Column(name = "embedding_model")
    private String embeddingModel;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    private User uploadedBy;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DocumentChunk> chunks = new ArrayList<>();

    public Document() {}

    public Document(String filename, String originalFilename, DocumentType documentType, 
                   Tenant tenant, User uploadedBy) {
        this.filename = filename;
        this.originalFilename = originalFilename;
        this.documentType = documentType;
        this.tenant = tenant;
        this.uploadedBy = uploadedBy;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public ProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(ProcessingStatus processingStatus) {
        this.processingStatus = processingStatus;
    }

    public String getProcessingMessage() {
        return processingMessage;
    }

    public void setProcessingMessage(String processingMessage) {
        this.processingMessage = processingMessage;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }

    public Integer getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(Integer chunkCount) {
        this.chunkCount = chunkCount;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public User getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(User uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public List<DocumentChunk> getChunks() {
        return chunks;
    }

    public void setChunks(List<DocumentChunk> chunks) {
        this.chunks = chunks;
    }

    public boolean isProcessed() {
        return processingStatus == ProcessingStatus.COMPLETED;
    }

    public boolean isProcessing() {
        return processingStatus == ProcessingStatus.PROCESSING;
    }

    public boolean hasFailed() {
        return processingStatus == ProcessingStatus.FAILED;
    }

    public enum DocumentType {
        PDF,
        DOCX,
        DOC,
        TXT,
        MD,
        HTML,
        RTF,
        ODT
    }

    public enum ProcessingStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }
}
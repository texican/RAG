package com.enterprise.rag.shared.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "document_chunks", indexes = {
    @Index(name = "idx_chunk_document", columnList = "document_id"),
    @Index(name = "idx_chunk_tenant", columnList = "tenant_id"),
    @Index(name = "idx_chunk_sequence", columnList = "document_id, sequence_number")
})
public class DocumentChunk extends BaseEntity {

    @NotBlank
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @NotNull
    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Column(name = "start_index")
    private Integer startIndex;

    @Column(name = "end_index")
    private Integer endIndex;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "embedding_vector_id")
    private String embeddingVectorId;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    public DocumentChunk() {}

    public DocumentChunk(String content, Integer sequenceNumber, Document document, Tenant tenant) {
        this.content = content;
        this.sequenceNumber = sequenceNumber;
        this.document = document;
        this.tenant = tenant;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public Integer getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(Integer startIndex) {
        this.startIndex = startIndex;
    }

    public Integer getEndIndex() {
        return endIndex;
    }

    public void setEndIndex(Integer endIndex) {
        this.endIndex = endIndex;
    }

    public Integer getTokenCount() {
        return tokenCount;
    }

    public void setTokenCount(Integer tokenCount) {
        this.tokenCount = tokenCount;
    }

    public String getEmbeddingVectorId() {
        return embeddingVectorId;
    }

    public void setEmbeddingVectorId(String embeddingVectorId) {
        this.embeddingVectorId = embeddingVectorId;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public boolean hasEmbedding() {
        return embeddingVectorId != null && !embeddingVectorId.trim().isEmpty();
    }
}
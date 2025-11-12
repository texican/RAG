-- Flyway Baseline Migration V3: Create Documents Table
-- Description: Document metadata and processing status tracking
-- Author: Enterprise RAG Development Team
-- Date: 2025-11-12

CREATE TABLE IF NOT EXISTS documents (
    -- Primary key (UUID)
    id UUID PRIMARY KEY,
    
    -- Audit fields (from BaseEntity)
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT,
    
    -- File information
    filename VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(255),
    file_size BIGINT,
    content_type VARCHAR(100),
    
    -- Document classification
    document_type VARCHAR(20) NOT NULL,
    
    -- Processing status
    processing_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    processing_message TEXT,
    
    -- Content
    extracted_text TEXT,
    chunk_count INTEGER DEFAULT 0,
    embedding_model VARCHAR(100),
    metadata TEXT,
    
    -- Multi-tenant associations
    tenant_id UUID NOT NULL,
    uploaded_by_id UUID NOT NULL,
    
    -- Foreign key constraints
    CONSTRAINT fk_document_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_document_uploader FOREIGN KEY (uploaded_by_id) REFERENCES users(id) ON DELETE RESTRICT
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_document_tenant ON documents(tenant_id);
CREATE INDEX IF NOT EXISTS idx_document_status ON documents(processing_status);
CREATE INDEX IF NOT EXISTS idx_document_type ON documents(document_type);
CREATE INDEX IF NOT EXISTS idx_document_uploaded_by ON documents(uploaded_by_id);

-- Add comments for documentation
COMMENT ON TABLE documents IS 'Document metadata and processing status for RAG pipeline';
COMMENT ON COLUMN documents.id IS 'Unique identifier (UUID)';
COMMENT ON COLUMN documents.filename IS 'System-generated filename';
COMMENT ON COLUMN documents.original_filename IS 'User-provided original filename';
COMMENT ON COLUMN documents.document_type IS 'File type: PDF, DOCX, TXT, MD, HTML, etc.';
COMMENT ON COLUMN documents.processing_status IS 'Processing state: PENDING, PROCESSING, COMPLETED, FAILED';
COMMENT ON COLUMN documents.extracted_text IS 'Full text content extracted from document';
COMMENT ON COLUMN documents.chunk_count IS 'Number of chunks created from this document';
COMMENT ON COLUMN documents.tenant_id IS 'Tenant that owns this document';
COMMENT ON COLUMN documents.uploaded_by_id IS 'User who uploaded the document';

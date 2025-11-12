-- Flyway Baseline Migration V4: Create Document Chunks Table
-- Description: Text segments for vector embeddings and RAG retrieval
-- Author: Enterprise RAG Development Team
-- Date: 2025-11-12

CREATE TABLE IF NOT EXISTS document_chunks (
    -- Primary key (UUID)
    id UUID PRIMARY KEY,
    
    -- Audit fields (from BaseEntity)
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT,
    
    -- Chunk content
    content TEXT NOT NULL,
    sequence_number INTEGER NOT NULL,
    
    -- Position tracking
    start_index INTEGER,
    end_index INTEGER,
    token_count INTEGER,
    
    -- Embedding information
    embedding_vector_id VARCHAR(255),
    metadata TEXT,
    
    -- Multi-tenant associations
    document_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    
    -- Foreign key constraints
    CONSTRAINT fk_chunk_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    CONSTRAINT fk_chunk_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_chunk_document ON document_chunks(document_id);
CREATE INDEX IF NOT EXISTS idx_chunk_tenant ON document_chunks(tenant_id);
CREATE INDEX IF NOT EXISTS idx_chunk_sequence ON document_chunks(document_id, sequence_number);

-- Add comments for documentation
COMMENT ON TABLE document_chunks IS 'Text segments from documents for vector embeddings and RAG retrieval';
COMMENT ON COLUMN document_chunks.id IS 'Unique identifier (UUID)';
COMMENT ON COLUMN document_chunks.content IS 'Text content of this chunk';
COMMENT ON COLUMN document_chunks.sequence_number IS 'Order of chunk within parent document (0-indexed)';
COMMENT ON COLUMN document_chunks.start_index IS 'Character index where chunk starts in original text';
COMMENT ON COLUMN document_chunks.end_index IS 'Character index where chunk ends in original text';
COMMENT ON COLUMN document_chunks.token_count IS 'Number of tokens in this chunk';
COMMENT ON COLUMN document_chunks.embedding_vector_id IS 'Reference to vector embedding in Redis';
COMMENT ON COLUMN document_chunks.document_id IS 'Parent document that contains this chunk';
COMMENT ON COLUMN document_chunks.tenant_id IS 'Tenant that owns this chunk';

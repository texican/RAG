-- Flyway Baseline Migration V1: Create Tenants Table
-- Description: Multi-tenant organization units with resource limits and status management
-- Author: Enterprise RAG Development Team
-- Date: 2025-11-12

CREATE TABLE IF NOT EXISTS tenants (
    -- Primary key (UUID)
    id UUID PRIMARY KEY,
    
    -- Audit fields (from BaseEntity)
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT,
    
    -- Tenant identification
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(500),
    
    -- Tenant status and limits
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    max_documents INTEGER DEFAULT 1000,
    max_storage_mb BIGINT DEFAULT 10240
);

-- Create indexes for performance
CREATE UNIQUE INDEX IF NOT EXISTS idx_tenant_slug ON tenants(slug);
CREATE INDEX IF NOT EXISTS idx_tenant_status ON tenants(status);

-- Add comments for documentation
COMMENT ON TABLE tenants IS 'Multi-tenant organizations with resource limits and lifecycle management';
COMMENT ON COLUMN tenants.id IS 'Unique identifier (UUID)';
COMMENT ON COLUMN tenants.name IS 'Human-readable organization name';
COMMENT ON COLUMN tenants.slug IS 'URL-friendly unique identifier (lowercase, numbers, hyphens only)';
COMMENT ON COLUMN tenants.status IS 'Operational status: ACTIVE, SUSPENDED, INACTIVE';
COMMENT ON COLUMN tenants.max_documents IS 'Maximum number of documents allowed';
COMMENT ON COLUMN tenants.max_storage_mb IS 'Maximum storage capacity in megabytes';

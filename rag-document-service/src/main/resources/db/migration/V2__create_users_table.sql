-- Flyway Baseline Migration V2: Create Users Table
-- Description: User accounts with authentication, roles, and multi-tenant association
-- Author: Enterprise RAG Development Team
-- Date: 2025-11-12

CREATE TABLE IF NOT EXISTS users (
    -- Primary key (UUID)
    id UUID PRIMARY KEY,
    
    -- Audit fields (from BaseEntity)
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT,
    
    -- User identification
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    
    -- Authentication
    password_hash VARCHAR(255) NOT NULL,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    email_verification_token VARCHAR(255),
    last_login_at TIMESTAMP,
    
    -- Authorization
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    
    -- Multi-tenant association
    tenant_id UUID NOT NULL,
    
    -- Foreign key constraint
    CONSTRAINT fk_user_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);

-- Create indexes for performance
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_user_tenant ON users(tenant_id);
CREATE INDEX IF NOT EXISTS idx_user_status ON users(status);

-- Add comments for documentation
COMMENT ON TABLE users IS 'User accounts with role-based access control and multi-tenant isolation';
COMMENT ON COLUMN users.id IS 'Unique identifier (UUID)';
COMMENT ON COLUMN users.email IS 'Unique email address for authentication';
COMMENT ON COLUMN users.password_hash IS 'Bcrypt hashed password (never plain text)';
COMMENT ON COLUMN users.role IS 'User role: ADMIN, USER, READER';
COMMENT ON COLUMN users.status IS 'Account status: ACTIVE, SUSPENDED, INACTIVE, PENDING_VERIFICATION';
COMMENT ON COLUMN users.tenant_id IS 'Associated tenant organization';

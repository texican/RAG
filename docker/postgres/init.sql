-- Create single shared database for all services
CREATE DATABASE byo_rag_local;

-- Create pgvector extension for future vector operations
\c byo_rag_local;
CREATE EXTENSION IF NOT EXISTS vector;

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE byo_rag_local TO rag_user;
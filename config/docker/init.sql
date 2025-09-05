-- PostgreSQL initialization script for RAG Enterprise System
-- Creates the main database and enables pgvector extension

-- Enable pgvector extension for vector operations
CREATE EXTENSION IF NOT EXISTS vector;

-- Grant all privileges to the rag_user
GRANT ALL PRIVILEGES ON DATABASE rag_enterprise TO rag_user;
GRANT ALL ON SCHEMA public TO rag_user;
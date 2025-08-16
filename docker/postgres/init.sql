-- Create databases for each service
CREATE DATABASE rag_auth;
CREATE DATABASE rag_documents;
CREATE DATABASE rag_embeddings;
CREATE DATABASE rag_core;
CREATE DATABASE rag_admin;

-- Create pgvector extension for vector operations
\c rag_embeddings;
CREATE EXTENSION IF NOT EXISTS vector;

\c rag_core;
CREATE EXTENSION IF NOT EXISTS vector;

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE rag_auth TO rag_user;
GRANT ALL PRIVILEGES ON DATABASE rag_documents TO rag_user;
GRANT ALL PRIVILEGES ON DATABASE rag_embeddings TO rag_user;
GRANT ALL PRIVILEGES ON DATABASE rag_core TO rag_user;
GRANT ALL PRIVILEGES ON DATABASE rag_admin TO rag_user;
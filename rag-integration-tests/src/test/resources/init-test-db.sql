-- Integration Test Database Initialization Script
-- This script initializes the test database for integration testing

-- Enable the pgvector extension if available (for future vector support)
-- CREATE EXTENSION IF NOT EXISTS vector;

-- Create test schema if needed
-- CREATE SCHEMA IF NOT EXISTS test_schema;

-- Note: Actual table creation is handled by JPA with ddl-auto=create-drop
-- This script can be extended with test-specific database setup

-- Log successful initialization
SELECT 'Test database initialized successfully' as status;
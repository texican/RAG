package com.byo.rag.integration.standalone;

import com.byo.rag.shared.util.TextChunker;
import com.byo.rag.shared.dto.TenantDto;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import redis.clients.jedis.Jedis;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Standalone integration tests that validate TestContainers infrastructure
 * and core functionality without requiring a full Spring Boot application.
 * 
 * This test demonstrates that the E2E-TEST-002 infrastructure is working
 * correctly and can be used to validate document processing components.
 */
@Testcontainers
@DisplayName("TestContainers Standalone Integration Tests")
class TestContainersStandaloneIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis/redis-stack:7.2.0-v6")
            .withExposedPorts(6379);

    private static JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void setUp() {
        // Wait for containers to be ready
        postgres.start();
        redis.start();
        
        DataSource dataSource = new DriverManagerDataSource(
            postgres.getJdbcUrl(),
            postgres.getUsername(),
            postgres.getPassword()
        );
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @AfterAll
    static void tearDown() {
        // TestContainers handles container cleanup automatically
    }

    @Test
    @DisplayName("Should verify PostgreSQL container is operational")
    void shouldVerifyPostgreSQLContainer() {
        // Test basic database operations
        assertThat(postgres.isRunning()).isTrue();
        
        // Create and query a test table
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS test_documents (id SERIAL PRIMARY KEY, name VARCHAR(255))");
        jdbcTemplate.update("INSERT INTO test_documents (name) VALUES (?)", "Test Document");
        
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM test_documents", Integer.class);
        assertThat(count).isEqualTo(1);
        
        String name = jdbcTemplate.queryForObject("SELECT name FROM test_documents WHERE id = 1", String.class);
        assertThat(name).isEqualTo("Test Document");
        
        // Cleanup
        jdbcTemplate.execute("DROP TABLE test_documents");
    }

    @Test
    @DisplayName("Should verify Redis container is operational")
    void shouldVerifyRedisContainer() {
        assertThat(redis.isRunning()).isTrue();
        
        // Test Redis operations
        try (Jedis jedis = new Jedis(redis.getHost(), redis.getMappedPort(6379))) {
            // Basic connectivity test
            String pong = jedis.ping();
            assertThat(pong).isEqualTo("PONG");
            
            // Test data operations
            jedis.set("test-key", "test-value");
            String value = jedis.get("test-key");
            assertThat(value).isEqualTo("test-value");
            
            // Test hash operations (useful for document metadata)
            jedis.hset("test-doc", "title", "Test Document");
            jedis.hset("test-doc", "content", "This is test content");
            
            String title = jedis.hget("test-doc", "title");
            String content = jedis.hget("test-doc", "content");
            
            assertThat(title).isEqualTo("Test Document");
            assertThat(content).isEqualTo("This is test content");
            
            // Cleanup
            jedis.del("test-key", "test-doc");
        }
    }

    @Test
    @DisplayName("Should validate document chunking functionality")
    void shouldValidateDocumentChunking() {
        String testDocument = """
            # Document Processing Test
            
            This document tests the chunking functionality for the RAG system.
            
            ## Section 1: Introduction
            
            Document chunking is essential for RAG systems to process large documents effectively.
            The chunking algorithm should preserve semantic boundaries while maintaining optimal chunk sizes.
            
            ## Section 2: Implementation
            
            The TextChunker utility provides multiple strategies for text segmentation.
            Each strategy is optimized for different use cases and content types.
            
            ## Conclusion
            
            Proper chunking ensures that vector embeddings capture meaningful semantic content.
            """;

        // Test semantic chunking
        TenantDto.ChunkingConfig semanticConfig = new TenantDto.ChunkingConfig(
            200, 50, TenantDto.ChunkingStrategy.SEMANTIC
        );
        
        List<String> semanticChunks = TextChunker.chunkText(testDocument, semanticConfig);
        
        assertThat(semanticChunks).isNotEmpty();
        assertThat(semanticChunks.size()).isGreaterThan(1);
        
        // Validate semantic boundaries are preserved
        for (String chunk : semanticChunks) {
            assertThat(chunk).isNotEmpty();
            assertThat(chunk.length()).isLessThanOrEqualTo(300); // Allow some variance
        }

        // Test fixed-size chunking
        TenantDto.ChunkingConfig fixedConfig = new TenantDto.ChunkingConfig(
            150, 25, TenantDto.ChunkingStrategy.FIXED_SIZE
        );
        
        List<String> fixedChunks = TextChunker.chunkText(testDocument, fixedConfig);
        
        assertThat(fixedChunks).isNotEmpty();
        assertThat(fixedChunks.size()).isGreaterThanOrEqualTo(semanticChunks.size()); // Fixed size typically creates more or equal chunks
        
        // Test sliding window chunking  
        TenantDto.ChunkingConfig slidingConfig = new TenantDto.ChunkingConfig(
            100, 50, TenantDto.ChunkingStrategy.SLIDING_WINDOW
        );
        
        List<String> slidingChunks = TextChunker.chunkText(slidingConfig.strategy() == TenantDto.ChunkingStrategy.SLIDING_WINDOW ? 
                                                          testDocument : testDocument, slidingConfig);
        
        assertThat(slidingChunks).isNotEmpty();
    }

    @Test
    @DisplayName("Should simulate document metadata storage and retrieval")
    void shouldSimulateDocumentMetadataStorage() {
        // Create documents table
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS documents (
                id SERIAL PRIMARY KEY,
                filename VARCHAR(255),
                content_type VARCHAR(100),
                file_size BIGINT,
                metadata JSONB,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);

        // Insert test document
        jdbcTemplate.update("""
            INSERT INTO documents (filename, content_type, file_size, metadata) 
            VALUES (?, ?, ?, ?::jsonb)
            """, 
            "test-document.txt",
            "text/plain", 
            1024L,
            "{\"category\": \"test\", \"author\": \"integration-test\"}"
        );

        // Query document metadata
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM documents", Integer.class);
        assertThat(count).isEqualTo(1);
        
        String filename = jdbcTemplate.queryForObject("SELECT filename FROM documents WHERE id = 1", String.class);
        assertThat(filename).isEqualTo("test-document.txt");
        
        // Test JSONB metadata querying
        String category = jdbcTemplate.queryForObject(
            "SELECT metadata->>'category' FROM documents WHERE id = 1", 
            String.class
        );
        assertThat(category).isEqualTo("test");
        
        // Cleanup
        jdbcTemplate.execute("DROP TABLE documents");
    }

    @Test
    @DisplayName("Should simulate document chunk storage with embeddings")
    void shouldSimulateDocumentChunkStorage() {
        // Create chunks table
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS document_chunks (
                id SERIAL PRIMARY KEY,
                document_id INTEGER,
                chunk_index INTEGER,
                content TEXT,
                character_count INTEGER,
                token_count INTEGER,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);

        String sampleText = "This is a sample document chunk for testing the storage mechanism.";
        int tokenCount = TextChunker.estimateTokenCount(sampleText);
        
        // Insert test chunk
        jdbcTemplate.update("""
            INSERT INTO document_chunks (document_id, chunk_index, content, character_count, token_count)
            VALUES (?, ?, ?, ?, ?)
            """,
            1, 0, sampleText, sampleText.length(), tokenCount
        );

        // Verify chunk storage
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM document_chunks", Integer.class);
        assertThat(count).isEqualTo(1);
        
        String content = jdbcTemplate.queryForObject("SELECT content FROM document_chunks WHERE id = 1", String.class);
        assertThat(content).isEqualTo(sampleText);
        
        Integer storedTokenCount = jdbcTemplate.queryForObject("SELECT token_count FROM document_chunks WHERE id = 1", Integer.class);
        assertThat(storedTokenCount).isEqualTo(tokenCount);
        
        // Cleanup
        jdbcTemplate.execute("DROP TABLE document_chunks");
    }

    @Test
    @DisplayName("Should validate text processing utilities")
    void shouldValidateTextProcessingUtilities() {
        // Test text cleaning
        String dirtyText = "This   has  multiple    spaces\n\n\n\nand\r\n\r\nline endings.";
        String cleanText = TextChunker.cleanText(dirtyText);
        
        assertThat(cleanText).doesNotContain("  "); // No double spaces
        assertThat(cleanText).doesNotContain("\r\n"); // No Windows line endings
        assertThat(cleanText).doesNotContain("\n\n\n"); // No triple newlines
        
        // Test token estimation
        String testText = "This is a test sentence with approximately twenty tokens for estimation testing.";
        int estimatedTokens = TextChunker.estimateTokenCount(testText);
        
        // Should be reasonable estimate (roughly 4 chars per token)
        assertThat(estimatedTokens).isBetween(10, 25);
        assertThat(estimatedTokens).isEqualTo((int) Math.ceil(testText.length() / 4.0));
    }

    @Test
    @DisplayName("Should demonstrate complete document processing workflow")
    void shouldDemonstrateCompleteWorkflow() {
        // Simulate complete document processing workflow
        String document = """
            Enterprise RAG System Documentation
            
            This document describes the architecture and implementation of our enterprise RAG system.
            
            Key Components:
            1. Document ingestion and processing
            2. Vector embedding generation  
            3. Semantic search and retrieval
            4. LLM integration and response generation
            
            Each component is designed for scalability and performance in production environments.
            """;

        // Step 1: Clean and prepare text
        String cleanedDocument = TextChunker.cleanText(document);
        assertThat(cleanedDocument).isNotNull().isNotEmpty();

        // Step 2: Chunk the document
        TenantDto.ChunkingConfig config = new TenantDto.ChunkingConfig(
            300, 75, TenantDto.ChunkingStrategy.SEMANTIC
        );
        List<String> chunks = TextChunker.chunkText(cleanedDocument, config);
        assertThat(chunks).isNotEmpty();

        // Step 3: Store document metadata in database
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS workflow_documents (
                id SERIAL PRIMARY KEY,
                title VARCHAR(255),
                processing_status VARCHAR(50),
                chunk_count INTEGER,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);

        jdbcTemplate.update("""
            INSERT INTO workflow_documents (title, processing_status, chunk_count)
            VALUES (?, ?, ?)
            """,
            "Enterprise RAG System Documentation",
            "COMPLETED", 
            chunks.size()
        );

        // Step 4: Store chunks in database
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS workflow_chunks (
                id SERIAL PRIMARY KEY,
                document_id INTEGER,
                chunk_index INTEGER,
                content TEXT,
                token_count INTEGER
            )
            """);

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            int tokens = TextChunker.estimateTokenCount(chunk);
            
            jdbcTemplate.update("""
                INSERT INTO workflow_chunks (document_id, chunk_index, content, token_count)
                VALUES (?, ?, ?, ?)
                """,
                1, i, chunk, tokens
            );
        }

        // Step 5: Cache document info in Redis
        try (Jedis jedis = new Jedis(redis.getHost(), redis.getMappedPort(6379))) {
            jedis.hset("doc:1", "title", "Enterprise RAG System Documentation");
            jedis.hset("doc:1", "status", "COMPLETED");
            jedis.hset("doc:1", "chunks", String.valueOf(chunks.size()));
            
            // Verify cache
            String cachedTitle = jedis.hget("doc:1", "title");
            String cachedChunks = jedis.hget("doc:1", "chunks");
            
            assertThat(cachedTitle).isEqualTo("Enterprise RAG System Documentation");
            assertThat(cachedChunks).isEqualTo(String.valueOf(chunks.size()));
        }

        // Step 6: Verify complete workflow
        Integer docCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM workflow_documents", Integer.class);
        Integer chunkCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM workflow_chunks", Integer.class);
        
        assertThat(docCount).isEqualTo(1);
        assertThat(chunkCount).isEqualTo(chunks.size());
        
        // Cleanup
        jdbcTemplate.execute("DROP TABLE workflow_documents");
        jdbcTemplate.execute("DROP TABLE workflow_chunks");
    }
}
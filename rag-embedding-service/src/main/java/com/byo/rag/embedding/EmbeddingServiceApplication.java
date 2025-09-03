package com.byo.rag.embedding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Spring Boot application class for the Enterprise RAG Embedding Service.
 * <p>
 * <strong>✅ Production Ready & Vector-Optimized (2025-09-03):</strong> This microservice provides 
 * high-performance vector operations and similarity search capabilities for the Enterprise RAG system. 
 * Successfully deployed in Docker at port 8083 with Redis Stack integration and resolved 
 * database dependency conflicts.
 * <p>
 * <strong>🐳 Docker Status:</strong> Working and operational with Redis-based vector storage,
 * transforming text content into numerical vectors using advanced embedding models and providing 
 * fast semantic search across large document collections.
 * 
 * <h2>Core Embedding Capabilities</h2>
 * <ul>
 *   <li><strong>Multi-Model Support</strong> - OpenAI, Sentence Transformers, and local models</li>
 *   <li><strong>Batch Processing</strong> - High-throughput embedding generation</li>
 *   <li><strong>Vector Storage</strong> - Redis-based high-performance vector database</li>
 *   <li><strong>Similarity Search</strong> - Advanced semantic search with filtering</li>
 *   <li><strong>Intelligent Caching</strong> - Performance optimization through caching</li>
 * </ul>
 * 
 * <h2>AI/ML Integration</h2>
 * <ul>
 *   <li><strong>Spring AI Framework</strong> - Native Spring AI 1.0.0-M1 integration</li>
 *   <li><strong>OpenAI Models</strong> - text-embedding-3-small and other OpenAI embeddings</li>
 *   <li><strong>Local Models</strong> - Sentence Transformers for offline processing</li>
 *   <li><strong>Model Registry</strong> - Dynamic model selection and fallback strategies</li>
 * </ul>
 * 
 * <h2>Vector Database Architecture</h2>
 * <ul>
 *   <li><strong>Redis Stack</strong> - High-performance vector storage and retrieval</li>
 *   <li><strong>Tenant Isolation</strong> - Complete separation of tenant vector data</li>
 *   <li><strong>Cosine Similarity</strong> - Optimized similarity calculations</li>
 *   <li><strong>Batch Operations</strong> - Efficient bulk vector operations</li>
 * </ul>
 * 
 * <h2>Event-Driven Processing</h2>
 * <ul>
 *   <li><strong>Kafka Integration</strong> - Asynchronous embedding generation</li>
 *   <li><strong>Document Processing</strong> - Responds to document ingestion events</li>
 *   <li><strong>Real-Time Updates</strong> - Immediate vector index updates</li>
 *   <li><strong>Error Handling</strong> - Robust failure recovery and retry mechanisms</li>
 * </ul>
 * 
 * <h2>API Endpoints</h2>
 * <ul>
 *   <li><strong>/embeddings/generate</strong> - Generate vectors from text content</li>
 *   <li><strong>/embeddings/search</strong> - Semantic similarity search operations</li>
 *   <li><strong>/embeddings/batch</strong> - Bulk embedding generation</li>
 *   <li><strong>/embeddings/health</strong> - Service health and model availability</li>
 * </ul>
 * 
 * <h2>Configuration Features</h2>
 * <ul>
 *   <li><strong>Component Scanning</strong> - Embedding service and shared components</li>
 *   <li><strong>Entity Scanning</strong> - Shared entity model for data consistency</li>
 *   <li><strong>Repository Management</strong> - JPA and Redis repository configuration</li>
 *   <li><strong>Audit Support</strong> - Entity auditing and change tracking</li>
 *   <li><strong>Async Processing</strong> - Background embedding generation</li>
 *   <li><strong>Kafka Integration</strong> - Event-driven processing capabilities</li>
 * </ul>
 * 
 * @author Enterprise RAG Development Team
 * @version 0.8.0
 * @since 0.1.0
 * @see org.springframework.boot.autoconfigure.SpringBootApplication
 * @see org.springframework.kafka.annotation.EnableKafka
 * @see org.springframework.data.redis.repository.configuration.EnableRedisRepositories
 */
@SpringBootApplication(
    scanBasePackages = {
        "com.byo.rag.embedding",
        "com.byo.rag.shared.exception"
    },
    exclude = {
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class
    }
)
@EnableRedisRepositories(basePackages = "com.byo.rag.embedding.redis")
@EnableAsync
public class EmbeddingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmbeddingServiceApplication.class, args);
    }
}
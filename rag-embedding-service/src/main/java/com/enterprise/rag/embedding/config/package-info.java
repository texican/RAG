/**
 * Configuration classes for the Enterprise RAG Embedding Service.
 * <p>
 * This package contains Spring configuration classes that set up embedding models,
 * vector storage infrastructure, and service-specific configurations. The configuration
 * layer provides flexible, production-ready setup for both cloud-based and local
 * embedding operations.
 * 
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link com.enterprise.rag.embedding.config.EmbeddingConfig} - Embedding model configuration and registry</li>
 *   <li>{@link com.enterprise.rag.embedding.config.RedisConfig} - Redis vector storage configuration</li>
 * </ul>
 * 
 * <h2>Configuration Architecture</h2>
 * <ul>
 *   <li><strong>Multi-Model Support</strong> - Primary (OpenAI) and fallback (local Transformers) embedding models</li>
 *   <li><strong>Redis Vector Storage</strong> - Optimized Redis configuration for high-performance vector operations</li>
 *   <li><strong>Connection Pooling</strong> - Enterprise-grade connection pool management with health checks</li>
 *   <li><strong>Property-Based Configuration</strong> - Externalized configuration using Spring Boot properties</li>
 * </ul>
 * 
 * <h2>Embedding Model Registry</h2>
 * The configuration provides a flexible registry system that supports:
 * <ul>
 *   <li>Dynamic model selection based on requirements</li>
 *   <li>Automatic fallback to local models when cloud services are unavailable</li>
 *   <li>Model availability checking and validation</li>
 *   <li>Configuration-driven model parameters</li>
 * </ul>
 * 
 * <h2>Redis Vector Storage Configuration</h2>
 * <ul>
 *   <li><strong>Connection Pooling</strong> - Jedis pool with configurable min/max connections</li>
 *   <li><strong>Serialization</strong> - JSON serialization for complex vector data structures</li>
 *   <li><strong>Performance Tuning</strong> - Connection validation and eviction policies</li>
 *   <li><strong>Multi-Database Support</strong> - Separate Redis databases for different data types</li>
 * </ul>
 * 
 * <h2>Configuration Properties</h2>
 * <pre>{@code
 * # Embedding Model Configuration
 * embedding.models.default-model=openai-text-embedding-3-small
 * embedding.models.fallback-model=sentence-transformers-all-minilm-l6-v2
 * embedding.models.cache-ttl=3600
 * embedding.models.openai.api-key=${OPENAI_API_KEY}
 * embedding.models.openai.model=text-embedding-3-small
 * embedding.models.openai.dimensions=1536
 * 
 * # Redis Vector Storage Configuration
 * embedding.vector.redis.host=localhost
 * embedding.vector.redis.port=6379
 * embedding.vector.redis.database=2
 * embedding.vector.redis.timeout=2000
 * embedding.vector.redis.index-prefix=rag:vectors
 * embedding.vector.redis.batch-size=100
 * embedding.vector.redis.dimension=1536
 * embedding.vector.redis.similarity-algorithm=COSINE
 * embedding.vector.redis.max-connections=16
 * embedding.vector.redis.max-idle=8
 * embedding.vector.redis.min-idle=2
 * }</pre>
 * 
 * <h2>Production Considerations</h2>
 * <ul>
 *   <li><strong>High Availability</strong> - Multiple embedding model options for resilience</li>
 *   <li><strong>Performance Optimization</strong> - Connection pooling and caching strategies</li>
 *   <li><strong>Monitoring Integration</strong> - Metrics and health check endpoints</li>
 *   <li><strong>Security</strong> - Secure API key management and connection encryption</li>
 * </ul>
 * 
 * <h2>Integration with Spring AI</h2>
 * The configuration leverages Spring AI 1.0.0-M1 features:
 * <ul>
 *   <li>Native OpenAI API integration</li>
 *   <li>Local Transformers model support</li>
 *   <li>Embedding model abstraction layer</li>
 *   <li>Automatic configuration management</li>
 * </ul>
 * 
 * @author Enterprise RAG Development Team
 * @version 0.8.0
 * @since 0.1.0
 */
package com.enterprise.rag.embedding.config;
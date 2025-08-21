package com.enterprise.rag.embedding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * RAG Embedding Service Application
 * 
 * Provides vector operations and similarity search capabilities for the RAG system.
 * Features:
 * - Multiple embedding model support (OpenAI, Sentence-BERT, local models)
 * - Batch embedding generation for efficiency
 * - Redis vector storage with tenant isolation
 * - Advanced similarity search with filtering
 * - Embedding caching strategies
 * - Real-time processing via Kafka
 */
@SpringBootApplication(scanBasePackages = {
    "com.enterprise.rag.embedding",
    "com.enterprise.rag.shared.exception"
})
@EntityScan("com.enterprise.rag.shared.entity")
@EnableJpaRepositories(basePackages = "com.enterprise.rag.embedding.repository")
@EnableRedisRepositories(basePackages = "com.enterprise.rag.embedding.redis")
@EnableJpaAuditing
@EnableTransactionManagement
@EnableAsync
@EnableKafka
public class EmbeddingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmbeddingServiceApplication.class, args);
    }
}
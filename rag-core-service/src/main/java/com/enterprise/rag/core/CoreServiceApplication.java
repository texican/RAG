package com.enterprise.rag.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * RAG Core Service Application
 * 
 * The central orchestration service for the RAG system that handles:
 * - RAG query processing and orchestration
 * - Context assembly from retrieved documents
 * - LLM integration with multiple providers
 * - Response streaming and real-time delivery
 * - Conversation history and context management
 * - Intelligent caching and query optimization
 */
@SpringBootApplication(scanBasePackages = {
    "com.enterprise.rag.core",
    "com.enterprise.rag.shared.exception"
})
@EntityScan("com.enterprise.rag.shared.entity")
@EnableJpaRepositories(basePackages = "com.enterprise.rag.core.repository")
@EnableJpaAuditing
@EnableTransactionManagement
@EnableAsync
@EnableKafka
@EnableFeignClients
public class CoreServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoreServiceApplication.class, args);
    }
}
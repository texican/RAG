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
 * Spring Boot application class for the Enterprise RAG Core Service.
 * <p>
 * This microservice serves as the central orchestration hub for the Enterprise RAG
 * (Retrieval Augmented Generation) system. It coordinates the entire RAG pipeline
 * from query processing through document retrieval to AI-powered response generation,
 * providing the primary intelligence layer of the system.
 * 
 * <h2>RAG Pipeline Orchestration</h2>
 * <ul>
 *   <li><strong>Query Processing</strong> - Advanced query analysis and optimization</li>
 *   <li><strong>Vector Search</strong> - Semantic document retrieval via embedding service</li>
 *   <li><strong>Context Assembly</strong> - Intelligent context compilation from retrieved documents</li>
 *   <li><strong>LLM Integration</strong> - Multi-provider AI model integration and management</li>
 *   <li><strong>Response Generation</strong> - Streaming and real-time response delivery</li>
 * </ul>
 * 
 * <h2>AI/ML Capabilities</h2>
 * <ul>
 *   <li><strong>Spring AI Integration</strong> - Native ChatClient and embedding support</li>
 *   <li><strong>Multiple LLM Providers</strong> - OpenAI, Anthropic, local models</li>
 *   <li><strong>Conversation Management</strong> - Multi-turn dialogue and context tracking</li>
 *   <li><strong>Response Streaming</strong> - Real-time token streaming for better UX</li>
 *   <li><strong>Context Optimization</strong> - Intelligent context window management</li>
 * </ul>
 * 
 * <h2>Performance & Caching</h2>
 * <ul>
 *   <li><strong>Intelligent Caching</strong> - Redis-based query and response caching</li>
 *   <li><strong>Query Optimization</strong> - Advanced query preprocessing and enhancement</li>
 *   <li><strong>Context Caching</strong> - Cached document context for repeated queries</li>
 *   <li><strong>Session Management</strong> - Conversation state persistence</li>
 * </ul>
 * 
 * <h2>Service Integration</h2>
 * <ul>
 *   <li><strong>Embedding Service</strong> - Vector search and similarity operations</li>
 *   <li><strong>Document Service</strong> - Document retrieval and content access</li>
 *   <li><strong>Auth Service</strong> - User authentication and authorization</li>
 *   <li><strong>Admin Service</strong> - Administrative monitoring and controls</li>
 * </ul>
 * 
 * <h2>API Endpoints</h2>
 * <ul>
 *   <li><strong>/rag/query</strong> - Primary RAG query processing endpoint</li>
 *   <li><strong>/rag/stream</strong> - Streaming response endpoint for real-time UX</li>
 *   <li><strong>/rag/conversation</strong> - Multi-turn conversation management</li>
 *   <li><strong>/rag/health</strong> - Service health and dependency status</li>
 * </ul>
 * 
 * <h2>Configuration Features</h2>
 * <ul>
 *   <li><strong>Component Scanning</strong> - Core service and shared component discovery</li>
 *   <li><strong>Entity Management</strong> - Shared entity model for data consistency</li>
 *   <li><strong>Repository Config</strong> - Custom repository implementations</li>
 *   <li><strong>Audit Support</strong> - Entity auditing for compliance</li>
 *   <li><strong>Transaction Management</strong> - ACID compliance for operations</li>
 *   <li><strong>Async Processing</strong> - Background task execution</li>
 *   <li><strong>Kafka Integration</strong> - Event-driven architecture support</li>
 *   <li><strong>Feign Clients</strong> - Inter-service communication clients</li>
 * </ul>
 * 
 * @author Enterprise RAG Development Team
 * @version 0.8.0
 * @since 0.1.0
 * @see org.springframework.boot.autoconfigure.SpringBootApplication
 * @see org.springframework.cloud.openfeign.EnableFeignClients
 * @see org.springframework.kafka.annotation.EnableKafka
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
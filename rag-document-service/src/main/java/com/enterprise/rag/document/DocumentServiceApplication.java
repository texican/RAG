package com.enterprise.rag.document;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Spring Boot application class for the Enterprise RAG Document Processing Service.
 * <p>
 * This microservice is responsible for comprehensive document ingestion, processing,
 * and preparation for the RAG (Retrieval Augmented Generation) pipeline. It handles
 * multiple file formats, extracts text content, performs intelligent chunking,
 * and manages document lifecycle operations.
 * 
 * <h2>Core Responsibilities</h2>
 * <ul>
 *   <li><strong>Document Ingestion</strong> - Multi-format file upload and validation</li>
 *   <li><strong>Text Extraction</strong> - Content extraction from PDF, DOCX, TXT, and other formats</li>
 *   <li><strong>Intelligent Chunking</strong> - Context-aware text segmentation for optimal retrieval</li>
 *   <li><strong>Metadata Management</strong> - Document properties, tags, and classification</li>
 *   <li><strong>Storage Management</strong> - File system and database persistence coordination</li>
 * </ul>
 * 
 * <h2>Service Architecture</h2>
 * <ul>
 *   <li><strong>Asynchronous Processing</strong> - Background document processing pipelines</li>
 *   <li><strong>Event-Driven Design</strong> - Kafka integration for downstream notifications</li>
 *   <li><strong>Multi-Tenant Support</strong> - Complete tenant isolation and data security</li>
 *   <li><strong>Transactional Operations</strong> - ACID compliance for document operations</li>
 * </ul>
 * 
 * <h2>Processing Pipeline</h2>
 * <ol>
 *   <li><strong>Upload & Validation</strong> - File type validation and security scanning</li>
 *   <li><strong>Text Extraction</strong> - Apache Tika-based content extraction</li>
 *   <li><strong>Content Analysis</strong> - Language detection and content classification</li>
 *   <li><strong>Chunking Strategy</strong> - Intelligent text segmentation preserving context</li>
 *   <li><strong>Metadata Enrichment</strong> - Automatic tagging and categorization</li>
 *   <li><strong>Storage & Indexing</strong> - Database persistence and search index updates</li>
 * </ol>
 * 
 * <h2>Integration Points</h2>
 * <ul>
 *   <li><strong>Embedding Service</strong> - Sends processed chunks for vector generation</li>
 *   <li><strong>Auth Service</strong> - Validates user permissions and tenant access</li>
 *   <li><strong>Core Service</strong> - Provides document content for RAG queries</li>
 *   <li><strong>Admin Service</strong> - Supports administrative document operations</li>
 * </ul>
 * 
 * <h2>Configuration Features</h2>
 * <ul>
 *   <li><strong>Component Scanning</strong> - Automatic discovery of service components</li>
 *   <li><strong>Entity Management</strong> - JPA entity scanning from shared module</li>
 *   <li><strong>Repository Configuration</strong> - Custom repository implementations</li>
 *   <li><strong>Audit Support</strong> - Automatic entity auditing with timestamps</li>
 *   <li><strong>Transaction Management</strong> - Declarative transaction support</li>
 *   <li><strong>Async Processing</strong> - Background task execution capabilities</li>
 * </ul>
 * 
 * @author Enterprise RAG Development Team
 * @version 0.8.0
 * @since 0.1.0
 * @see org.springframework.boot.autoconfigure.SpringBootApplication
 * @see org.springframework.scheduling.annotation.EnableAsync
 */
@SpringBootApplication(scanBasePackages = {
    "com.enterprise.rag.document",
    "com.enterprise.rag.shared.exception"
})
@EntityScan("com.enterprise.rag.shared.entity")
@EnableJpaRepositories(basePackages = "com.enterprise.rag.document.repository")
@EnableJpaAuditing
@EnableTransactionManagement
@EnableAsync
public class DocumentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentServiceApplication.class, args);
    }
}
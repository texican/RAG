package com.enterprise.rag.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * JPA configuration for the Enterprise RAG System shared module.
 * <p>
 * This configuration class provides enterprise-grade JPA settings that are shared
 * across all microservices in the RAG system. It enables essential JPA features
 * including automatic auditing, repository scanning, and declarative transaction management.
 * </p>
 * 
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>JPA Auditing</strong> - Automatic population of audit fields (createdDate, lastModifiedDate)</li>
 *   <li><strong>Repository Scanning</strong> - Auto-discovery of JPA repositories across all enterprise.rag packages</li>
 *   <li><strong>Transaction Management</strong> - Declarative transaction support with @Transactional</li>
 * </ul>
 * 
 * <h3>Multi-tenant Considerations:</h3>
 * <p>
 * This configuration supports multi-tenant data isolation by enabling JPA auditing
 * and transaction management that work correctly with tenant-aware entities.
 * All repositories are automatically scanned from the enterprise.rag package hierarchy.
 * </p>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // This configuration is automatically applied when the shared module is included
 * @Entity
 * public class Document extends BaseEntity {
 *     // Audit fields (createdDate, lastModifiedDate) are automatically populated
 * }
 * }</pre>
 * 
 * @author Enterprise RAG Development Team
 * @since 1.0.0
 * @see org.springframework.data.jpa.repository.config.EnableJpaAuditing
 * @see org.springframework.data.jpa.repository.config.EnableJpaRepositories
 * @see org.springframework.transaction.annotation.EnableTransactionManagement
 */
@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.enterprise.rag")
@EnableTransactionManagement
public class JpaConfig {
}
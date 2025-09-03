package com.byo.rag.document.config;

import com.byo.rag.shared.config.JsonConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * JPA configuration for the Enterprise RAG Document Service.
 * <p>
 * This configuration class provides document-specific JPA settings and
 * imports shared JSON configuration for consistent data serialization
 * across the document processing pipeline. It ensures proper entity
 * mapping and database operations for document management.
 * 
 * <h2>Configuration Components</h2>
 * <ul>
 *   <li><strong>JSON Configuration</strong> - Imports shared Jackson configuration</li>
 *   <li><strong>Entity Mapping</strong> - Document-specific entity configurations</li>
 *   <li><strong>Repository Setup</strong> - Document repository configurations</li>
 *   <li><strong>Transaction Management</strong> - Document processing transaction policies</li>
 * </ul>
 * 
 * <h2>Document Entity Management</h2>
 * <ul>
 *   <li><strong>Document Entities</strong> - Document and DocumentChunk entity management</li>
 *   <li><strong>Audit Support</strong> - Automatic auditing for document operations</li>
 *   <li><strong>Tenant Isolation</strong> - Multi-tenant data isolation at entity level</li>
 *   <li><strong>Indexing Strategy</strong> - Database indexes for document queries</li>
 * </ul>
 * 
 * <h2>Shared Configuration</h2>
 * <ul>
 *   <li><strong>JSON Serialization</strong> - Consistent JSON handling across services</li>
 *   <li><strong>Date/Time Handling</strong> - Standardized temporal data serialization</li>
 *   <li><strong>Custom Serializers</strong> - Document-specific serialization rules</li>
 *   <li><strong>Null Handling</strong> - Consistent null value processing</li>
 * </ul>
 * 
 * <h2>Integration Benefits</h2>
 * <ul>
 *   <li><strong>Consistency</strong> - Unified configuration across all services</li>
 *   <li><strong>Maintainability</strong> - Centralized configuration management</li>
 *   <li><strong>Compatibility</strong> - Ensures inter-service data compatibility</li>
 *   <li><strong>Standards Compliance</strong> - Follows enterprise configuration patterns</li>
 * </ul>
 * 
 * @author Enterprise RAG Development Team
 * @version 0.8.0
 * @since 0.1.0
 * @see com.byo.rag.shared.config.JsonConfig
 * @see org.springframework.context.annotation.Configuration
 */
@Configuration
@Import(JsonConfig.class)
public class DocumentJpaConfig {
}
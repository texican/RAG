package com.byo.rag.shared.dto;

import com.byo.rag.shared.entity.Tenant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Objects for tenant management in the Enterprise RAG System.
 * <p>
 * This sealed interface defines all tenant-related DTOs used across the RAG microservices
 * for multi-tenant operations. It provides comprehensive tenant configuration management,
 * resource limits, and billing-related functionality.
 * </p>
 * 
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Multi-tenant Architecture</strong> - Complete tenant isolation and management</li>
 *   <li><strong>Resource Limits</strong> - Configurable limits for documents, storage, and API usage</li>
 *   <li><strong>AI Model Configuration</strong> - Per-tenant embedding and LLM model selection</li>
 *   <li><strong>Chunking Strategy</strong> - Customizable text processing configurations</li>
 * </ul>
 * 
 * <h3>Tenant Lifecycle:</h3>
 * <ol>
 *   <li><strong>Creation</strong> - New tenant with default configuration</li>
 *   <li><strong>Configuration</strong> - Customize AI models, limits, chunking</li>
 *   <li><strong>Active Usage</strong> - Document processing and RAG queries</li>
 *   <li><strong>Management</strong> - Updates, suspension, analytics</li>
 * </ol>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Create new tenant
 * var createRequest = new TenantDto.CreateTenantRequest(
 *     "Enterprise Corp",
 *     "enterprise-corp",
 *     "Large enterprise customer",
 *     new TenantDto.TenantConfig(5000, 50000L, "text-embedding-3-large", "gpt-4", null)
 * );
 * 
 * // Update tenant configuration
 * var updateRequest = new TenantDto.UpdateTenantRequest(
 *     "Enterprise Corporation", 
 *     "Updated description",
 *     Tenant.TenantStatus.ACTIVE,
 *     new TenantDto.TenantConfig(10000, 100000L, null, null, null)
 * );
 * }</pre>
 * 
 * @author Enterprise RAG Development Team
 * @since 1.0.0
 * @see com.byo.rag.shared.entity.Tenant
 */
public sealed interface TenantDto permits 
    TenantDto.CreateTenantRequest,
    TenantDto.UpdateTenantRequest,
    TenantDto.TenantResponse,
    TenantDto.TenantSummary {

    /**
     * Request DTO for creating a new tenant in the system.
     * <p>
     * Contains all required information to establish a new multi-tenant workspace
     * with default or custom configuration. The slug must be unique across the system
     * and will be used for tenant identification in URLs and routing.
     * </p>
     * 
     * @param name display name for the tenant (2-100 characters)
     * @param slug unique URL-safe identifier (lowercase, numbers, hyphens only, 2-50 characters)
     * @param description optional description of the tenant (max 500 characters)
     * @param config tenant configuration including resource limits and AI model preferences
     */
    record CreateTenantRequest(
        @NotBlank @Size(min = 2, max = 100) String name,
        @NotBlank @Pattern(regexp = "^[a-z0-9-]+$") @Size(min = 2, max = 50) String slug,
        @Size(max = 500) String description,
        @Valid TenantConfig config
    ) implements TenantDto {}

    /**
     * Request DTO for updating an existing tenant's information and configuration.
     * <p>
     * All fields are optional - only provided fields will be updated. This allows
     * for partial updates without affecting unchanged tenant settings.
     * </p>
     * 
     * @param name updated display name (2-100 characters, null to keep current)
     * @param description updated description (max 500 characters, null to keep current)
     * @param status new tenant status (ACTIVE, SUSPENDED, null to keep current)
     * @param config updated configuration (null to keep current configuration)
     */
    record UpdateTenantRequest(
        @Size(min = 2, max = 100) String name,
        @Size(max = 500) String description,
        Tenant.TenantStatus status,
        @Valid TenantConfig config
    ) implements TenantDto {}

    /**
     * Complete tenant response with full details, configuration, and statistics.
     * <p>
     * This comprehensive response includes all tenant information, current configuration,
     * and usage statistics. Used for detailed tenant management and administrative dashboards.
     * </p>
     * 
     * @param id unique tenant identifier
     * @param name tenant display name
     * @param slug unique URL-safe identifier
     * @param description tenant description
     * @param status current tenant status (ACTIVE, SUSPENDED, etc.)
     * @param config current tenant configuration including limits and AI models
     * @param createdAt tenant creation timestamp
     * @param updatedAt last modification timestamp
     * @param stats current usage statistics and metrics
     */
    record TenantResponse(
        UUID id,
        String name,
        String slug,
        String description,
        Tenant.TenantStatus status,
        TenantConfig config,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        TenantStats stats
    ) implements TenantDto {}

    /**
     * Lightweight tenant summary for list operations and quick reference.
     * <p>
     * Contains essential tenant information without heavy configuration details,
     * optimized for efficient tenant listing and selection interfaces.
     * </p>
     * 
     * @param id unique tenant identifier
     * @param name tenant display name
     * @param slug unique URL-safe identifier
     * @param status current tenant status
     * @param createdAt tenant creation timestamp
     */
    record TenantSummary(
        UUID id,
        String name,
        String slug,
        Tenant.TenantStatus status,
        LocalDateTime createdAt
    ) implements TenantDto {}

    /**
     * Tenant configuration record defining resource limits and AI model preferences.
     * <p>
     * This configuration controls tenant behavior including document limits, storage quotas,
     * AI model selection, and text processing strategies. All fields have sensible defaults
     * for immediate usability upon tenant creation.
     * </p>
     * 
     * <h3>Default Values:</h3>
     * <ul>
     *   <li>maxDocuments: 1000 documents</li>
     *   <li>maxStorageMb: 10GB storage limit</li>
     *   <li>embeddingModel: text-embedding-3-small (cost-effective)</li>
     *   <li>llmModel: gpt-4o-mini (balanced performance/cost)</li>
     *   <li>chunking: 512 token chunks with 64 token overlap, semantic strategy</li>
     * </ul>
     * 
     * @param maxDocuments maximum number of documents this tenant can store (default: 1000)
     * @param maxStorageMb maximum storage in megabytes (default: 10240 = 10GB)
     * @param embeddingModel AI model for embedding generation (default: text-embedding-3-small)
     * @param llmModel AI model for RAG responses (default: gpt-4o-mini)
     * @param chunking text chunking configuration (default: semantic chunking)
     */
    record TenantConfig(
        Integer maxDocuments,
        Long maxStorageMb,
        String embeddingModel,
        String llmModel,
        ChunkingConfig chunking
    ) {
        /**
         * Canonical constructor with default value initialization.
         * Ensures all configuration fields have sensible defaults for immediate tenant usability.
         */
        public TenantConfig {
            if (maxDocuments == null) maxDocuments = 1000;
            if (maxStorageMb == null) maxStorageMb = 10240L;
            if (embeddingModel == null) embeddingModel = "text-embedding-3-small";
            if (llmModel == null) llmModel = "gpt-4o-mini";
            if (chunking == null) chunking = new ChunkingConfig(512, 64, ChunkingStrategy.SEMANTIC);
        }
    }

    /**
     * Configuration for text chunking strategy and parameters.
     * <p>
     * Controls how documents are split into smaller chunks for embedding generation
     * and semantic search. Different strategies optimize for different use cases:
     * semantic chunking for natural boundaries, fixed-size for consistency,
     * sliding window for comprehensive coverage.
     * </p>
     * 
     * @param chunkSize target size for each text chunk in tokens (default: 512)
     * @param chunkOverlap number of overlapping tokens between chunks (default: 64)
     * @param strategy chunking strategy to use (default: SEMANTIC)
     */
    record ChunkingConfig(
        Integer chunkSize,
        Integer chunkOverlap,
        ChunkingStrategy strategy
    ) {
        /**
         * Canonical constructor with default value initialization.
         * Ensures optimal chunking configuration for most use cases.
         */
        public ChunkingConfig {
            if (chunkSize == null) chunkSize = 512;
            if (chunkOverlap == null) chunkOverlap = 64;
            if (strategy == null) strategy = ChunkingStrategy.SEMANTIC;
        }
    }

    /**
     * Current usage statistics and metrics for a tenant.
     * <p>
     * Provides real-time insights into tenant resource utilization,
     * used for billing, capacity planning, and usage monitoring.
     * </p>
     * 
     * @param documentCount total number of documents uploaded
     * @param chunkCount total number of text chunks created
     * @param storageMb current storage usage in megabytes
     * @param queryCount total number of RAG queries performed
     */
    record TenantStats(
        Long documentCount,
        Long chunkCount,
        Long storageMb,
        Long queryCount
    ) {}

    /**
     * Available text chunking strategies for document processing.
     * <p>
     * Different strategies optimize for different use cases and content types.
     * The choice affects both embedding quality and retrieval performance.
     * </p>
     * 
     * <h3>Strategy Descriptions:</h3>
     * <ul>
     *   <li><strong>FIXED_SIZE</strong> - Simple token-based chunking with fixed boundaries</li>
     *   <li><strong>SEMANTIC</strong> - Intelligent chunking respecting sentence and paragraph boundaries</li>
     *   <li><strong>SLIDING_WINDOW</strong> - Overlapping chunks with sliding window approach</li>
     * </ul>
     */
    enum ChunkingStrategy {
        /** Simple fixed-size chunking based on token count */
        FIXED_SIZE,
        /** Intelligent semantic chunking respecting natural text boundaries */
        SEMANTIC,
        /** Sliding window approach with configurable overlap */
        SLIDING_WINDOW
    }
}
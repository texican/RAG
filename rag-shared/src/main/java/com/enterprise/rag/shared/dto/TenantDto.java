package com.enterprise.rag.shared.dto;

import com.enterprise.rag.shared.entity.Tenant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public sealed interface TenantDto permits 
    TenantDto.CreateTenantRequest,
    TenantDto.UpdateTenantRequest,
    TenantDto.TenantResponse,
    TenantDto.TenantSummary {

    record CreateTenantRequest(
        @NotBlank @Size(min = 2, max = 100) String name,
        @NotBlank @Pattern(regexp = "^[a-z0-9-]+$") @Size(min = 2, max = 50) String slug,
        @Size(max = 500) String description,
        @Valid TenantConfig config
    ) implements TenantDto {}

    record UpdateTenantRequest(
        @Size(min = 2, max = 100) String name,
        @Size(max = 500) String description,
        Tenant.TenantStatus status,
        @Valid TenantConfig config
    ) implements TenantDto {}

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

    record TenantSummary(
        UUID id,
        String name,
        String slug,
        Tenant.TenantStatus status,
        LocalDateTime createdAt
    ) implements TenantDto {}

    record TenantConfig(
        Integer maxDocuments,
        Long maxStorageMb,
        String embeddingModel,
        String llmModel,
        ChunkingConfig chunking
    ) {
        public TenantConfig {
            if (maxDocuments == null) maxDocuments = 1000;
            if (maxStorageMb == null) maxStorageMb = 10240L;
            if (embeddingModel == null) embeddingModel = "text-embedding-3-small";
            if (llmModel == null) llmModel = "gpt-4o-mini";
            if (chunking == null) chunking = new ChunkingConfig(512, 64, ChunkingStrategy.SEMANTIC);
        }
    }

    record ChunkingConfig(
        Integer chunkSize,
        Integer chunkOverlap,
        ChunkingStrategy strategy
    ) {
        public ChunkingConfig {
            if (chunkSize == null) chunkSize = 512;
            if (chunkOverlap == null) chunkOverlap = 64;
            if (strategy == null) strategy = ChunkingStrategy.SEMANTIC;
        }
    }

    record TenantStats(
        Long documentCount,
        Long chunkCount,
        Long storageMb,
        Long queryCount
    ) {}

    enum ChunkingStrategy {
        FIXED_SIZE,
        SEMANTIC,
        SLIDING_WINDOW
    }
}
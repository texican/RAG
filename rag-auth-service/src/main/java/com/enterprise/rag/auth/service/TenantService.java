package com.enterprise.rag.auth.service;

import com.enterprise.rag.auth.repository.TenantRepository;
import com.enterprise.rag.shared.dto.TenantDto;
import com.enterprise.rag.shared.entity.Tenant;
import com.enterprise.rag.shared.exception.TenantNotFoundException;
import com.enterprise.rag.shared.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class TenantService {

    private static final Logger logger = LoggerFactory.getLogger(TenantService.class);

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public TenantDto.TenantResponse createTenant(TenantDto.CreateTenantRequest request) {
        logger.info("Creating tenant with slug: {}", request.slug());

        if (tenantRepository.existsBySlug(request.slug())) {
            throw new IllegalArgumentException("Tenant with slug '" + request.slug() + "' already exists");
        }

        Tenant tenant = new Tenant();
        tenant.setName(request.name());
        tenant.setSlug(SecurityUtils.sanitizeSlug(request.slug()));
        tenant.setDescription(request.description());
        
        if (request.config() != null) {
            tenant.setMaxDocuments(request.config().maxDocuments());
            tenant.setMaxStorageMb(request.config().maxStorageMb());
        }

        tenant = tenantRepository.save(tenant);
        logger.info("Created tenant with ID: {}", tenant.getId());

        return mapToResponse(tenant);
    }

    @Transactional(readOnly = true)
    public TenantDto.TenantResponse getTenant(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new TenantNotFoundException(tenantId));
        return mapToResponse(tenant);
    }

    @Transactional(readOnly = true)
    public TenantDto.TenantResponse getTenantBySlug(String slug) {
        Tenant tenant = tenantRepository.findBySlug(slug)
            .orElseThrow(() -> new TenantNotFoundException(slug));
        return mapToResponse(tenant);
    }

    @Transactional(readOnly = true)
    public Page<TenantDto.TenantSummary> getAllTenants(Pageable pageable) {
        return tenantRepository.findAll(pageable)
            .map(this::mapToSummary);
    }

    public TenantDto.TenantResponse updateTenant(UUID tenantId, TenantDto.UpdateTenantRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new TenantNotFoundException(tenantId));

        if (request.name() != null) {
            tenant.setName(request.name());
        }
        if (request.description() != null) {
            tenant.setDescription(request.description());
        }
        if (request.status() != null) {
            tenant.setStatus(request.status());
        }
        if (request.config() != null) {
            if (request.config().maxDocuments() != null) {
                tenant.setMaxDocuments(request.config().maxDocuments());
            }
            if (request.config().maxStorageMb() != null) {
                tenant.setMaxStorageMb(request.config().maxStorageMb());
            }
        }

        tenant = tenantRepository.save(tenant);
        logger.info("Updated tenant with ID: {}", tenant.getId());

        return mapToResponse(tenant);
    }

    public void deleteTenant(UUID tenantId) {
        if (!tenantRepository.existsById(tenantId)) {
            throw new TenantNotFoundException(tenantId);
        }
        
        tenantRepository.deleteById(tenantId);
        logger.info("Deleted tenant with ID: {}", tenantId);
    }

    @Transactional(readOnly = true)
    public Tenant findById(UUID tenantId) {
        return tenantRepository.findById(tenantId)
            .orElseThrow(() -> new TenantNotFoundException(tenantId));
    }

    @Transactional(readOnly = true)
    public Tenant findBySlug(String slug) {
        return tenantRepository.findBySlug(slug)
            .orElseThrow(() -> new TenantNotFoundException(slug));
    }

    @Transactional(readOnly = true)
    public boolean existsBySlug(String slug) {
        return tenantRepository.existsBySlug(slug);
    }

    private TenantDto.TenantResponse mapToResponse(Tenant tenant) {
        TenantDto.TenantConfig config = new TenantDto.TenantConfig(
            tenant.getMaxDocuments(),
            tenant.getMaxStorageMb(),
            "text-embedding-3-small", // Default values, could be stored in DB
            "gpt-4o-mini",
            new TenantDto.ChunkingConfig(512, 64, TenantDto.ChunkingStrategy.SEMANTIC)
        );

        TenantDto.TenantStats stats = new TenantDto.TenantStats(
            (long) tenant.getDocuments().size(),
            0L, // Would be calculated from document chunks
            0L, // Would be calculated from file sizes
            0L  // Would be tracked separately
        );

        return new TenantDto.TenantResponse(
            tenant.getId(),
            tenant.getName(),
            tenant.getSlug(),
            tenant.getDescription(),
            tenant.getStatus(),
            config,
            tenant.getCreatedAt(),
            tenant.getUpdatedAt(),
            stats
        );
    }

    private TenantDto.TenantSummary mapToSummary(Tenant tenant) {
        return new TenantDto.TenantSummary(
            tenant.getId(),
            tenant.getName(),
            tenant.getSlug(),
            tenant.getStatus(),
            tenant.getCreatedAt()
        );
    }
}
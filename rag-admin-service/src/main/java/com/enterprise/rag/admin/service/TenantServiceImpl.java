package com.enterprise.rag.admin.service;

import com.enterprise.rag.admin.dto.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class TenantServiceImpl implements TenantService {

    private final Map<String, TenantData> tenants = new ConcurrentHashMap<>();
    private final AtomicLong tenantCounter = new AtomicLong(1);

    @Override
    public TenantResponse createTenant(TenantCreateRequest request) {
        String tenantId = "tenant-" + tenantCounter.getAndIncrement();
        LocalDateTime now = LocalDateTime.now();
        
        TenantData tenantData = new TenantData(
                tenantId,
                request.name(),
                request.adminEmail(),
                request.description(),
                "ACTIVE",
                now,
                now,
                0, // totalUsers
                0, // totalDocuments
                true // isActive
        );
        
        tenants.put(tenantId, tenantData);
        
        return toTenantResponse(tenantData);
    }

    @Override
    public TenantResponse getTenantById(String tenantId) {
        TenantData tenantData = tenants.get(tenantId);
        if (tenantData == null) {
            throw new RuntimeException("Tenant not found with ID: " + tenantId);
        }
        return toTenantResponse(tenantData);
    }

    @Override
    public TenantResponse updateTenant(String tenantId, TenantUpdateRequest request) {
        TenantData existingTenant = tenants.get(tenantId);
        if (existingTenant == null) {
            throw new RuntimeException("Tenant not found with ID: " + tenantId);
        }
        
        TenantData updatedTenant = new TenantData(
                existingTenant.tenantId(),
                request.name() != null ? request.name() : existingTenant.name(),
                request.adminEmail() != null ? request.adminEmail() : existingTenant.adminEmail(),
                request.description() != null ? request.description() : existingTenant.description(),
                existingTenant.status(),
                existingTenant.createdAt(),
                LocalDateTime.now(), // Update the updatedAt timestamp
                existingTenant.totalUsers(),
                existingTenant.totalDocuments(),
                existingTenant.isActive()
        );
        
        tenants.put(tenantId, updatedTenant);
        return toTenantResponse(updatedTenant);
    }

    @Override
    public TenantResponse suspendTenant(String tenantId, TenantSuspendRequest request) {
        TenantData existingTenant = tenants.get(tenantId);
        if (existingTenant == null) {
            throw new RuntimeException("Tenant not found with ID: " + tenantId);
        }
        
        TenantData suspendedTenant = new TenantData(
                existingTenant.tenantId(),
                existingTenant.name(),
                existingTenant.adminEmail(),
                existingTenant.description(),
                "SUSPENDED",
                existingTenant.createdAt(),
                LocalDateTime.now(),
                existingTenant.totalUsers(),
                existingTenant.totalDocuments(),
                false // isActive = false when suspended
        );
        
        tenants.put(tenantId, suspendedTenant);
        return toTenantResponse(suspendedTenant);
    }

    @Override
    public TenantResponse reactivateTenant(String tenantId) {
        TenantData existingTenant = tenants.get(tenantId);
        if (existingTenant == null) {
            throw new RuntimeException("Tenant not found with ID: " + tenantId);
        }
        
        TenantData reactivatedTenant = new TenantData(
                existingTenant.tenantId(),
                existingTenant.name(),
                existingTenant.adminEmail(),
                existingTenant.description(),
                "ACTIVE",
                existingTenant.createdAt(),
                LocalDateTime.now(),
                existingTenant.totalUsers(),
                existingTenant.totalDocuments(),
                true // isActive = true when reactivated
        );
        
        tenants.put(tenantId, reactivatedTenant);
        return toTenantResponse(reactivatedTenant);
    }

    @Override
    public TenantListResponse getAllTenants(PageRequest pageRequest) {
        List<TenantData> allTenants = new ArrayList<>(tenants.values());
        
        // Apply sorting
        Comparator<TenantData> comparator = createComparator(pageRequest);
        allTenants.sort(comparator);
        
        // Apply pagination
        int totalCount = allTenants.size();
        int totalPages = (int) Math.ceil((double) totalCount / pageRequest.getPageSize());
        int startIndex = (int) pageRequest.getOffset();
        int endIndex = Math.min(startIndex + pageRequest.getPageSize(), totalCount);
        
        List<TenantData> paginatedTenants = startIndex < totalCount 
                ? allTenants.subList(startIndex, endIndex)
                : new ArrayList<>();
        
        List<TenantResponse> tenantResponses = paginatedTenants.stream()
                .map(this::toTenantResponse)
                .collect(Collectors.toList());
        
        return new TenantListResponse(
                tenantResponses,
                totalCount,
                pageRequest.getPageNumber(),
                pageRequest.getPageSize(),
                totalPages
        );
    }

    @Override
    public void deleteTenant(String tenantId) {
        TenantData existingTenant = tenants.get(tenantId);
        if (existingTenant == null) {
            throw new RuntimeException("Tenant not found with ID: " + tenantId);
        }
        tenants.remove(tenantId);
    }

    private TenantResponse toTenantResponse(TenantData tenantData) {
        return new TenantResponse(
                tenantData.tenantId(),
                tenantData.name(),
                tenantData.adminEmail(),
                tenantData.description(),
                tenantData.status(),
                tenantData.createdAt(),
                tenantData.updatedAt(),
                tenantData.totalUsers(),
                tenantData.totalDocuments(),
                tenantData.isActive()
        );
    }

    private Comparator<TenantData> createComparator(PageRequest pageRequest) {
        return pageRequest.getSort().stream()
                .map(order -> {
                    Comparator<TenantData> comp = switch (order.getProperty()) {
                        case "name" -> Comparator.comparing(TenantData::name);
                        case "adminEmail" -> Comparator.comparing(TenantData::adminEmail);
                        case "status" -> Comparator.comparing(TenantData::status);
                        case "createdAt" -> Comparator.comparing(TenantData::createdAt);
                        case "updatedAt" -> Comparator.comparing(TenantData::updatedAt);
                        default -> Comparator.comparing(TenantData::createdAt);
                    };
                    return order.isDescending() ? comp.reversed() : comp;
                })
                .reduce(Comparator::thenComparing)
                .orElse(Comparator.comparing(TenantData::createdAt));
    }

    // Internal data class for storing tenant information
    private record TenantData(
            String tenantId,
            String name,
            String adminEmail,
            String description,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            int totalUsers,
            int totalDocuments,
            boolean isActive
    ) {}
}
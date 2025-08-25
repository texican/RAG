package com.enterprise.rag.admin.service;

import com.enterprise.rag.admin.dto.*;
import org.springframework.data.domain.PageRequest;

public interface TenantService {
    TenantResponse createTenant(TenantCreateRequest request);
    TenantResponse getTenantById(String tenantId);
    TenantResponse updateTenant(String tenantId, TenantUpdateRequest request);
    TenantResponse suspendTenant(String tenantId, TenantSuspendRequest request);
    TenantResponse reactivateTenant(String tenantId);
    TenantListResponse getAllTenants(PageRequest pageRequest);
    void deleteTenant(String tenantId);
}
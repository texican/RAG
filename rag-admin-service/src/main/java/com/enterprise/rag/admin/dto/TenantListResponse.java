package com.enterprise.rag.admin.dto;

import java.util.List;

public record TenantListResponse(
        List<TenantResponse> tenants,
        int totalCount,
        int page,
        int size,
        int totalPages
) {
}
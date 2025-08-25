package com.enterprise.rag.admin.dto;

import java.time.LocalDateTime;

public record TenantResponse(
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
) {
}
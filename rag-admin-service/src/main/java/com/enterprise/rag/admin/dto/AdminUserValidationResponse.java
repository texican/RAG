package com.enterprise.rag.admin.dto;

public record AdminUserValidationResponse(
        boolean exists,
        String username
) {
}
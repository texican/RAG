package com.enterprise.rag.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminRefreshRequest(
        @NotBlank(message = "Token is required")
        String token
) {
}
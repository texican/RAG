package com.enterprise.rag.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record TenantUpdateRequest(
        @Size(min = 2, max = 100, message = "Tenant name must be between 2 and 100 characters")
        String name,
        
        @Email(message = "Admin email must be valid")
        String adminEmail,
        
        @Size(max = 255, message = "Description cannot exceed 255 characters")
        String description
) {
}
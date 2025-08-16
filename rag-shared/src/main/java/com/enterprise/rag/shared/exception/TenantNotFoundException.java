package com.enterprise.rag.shared.exception;

import java.util.UUID;

public class TenantNotFoundException extends RagException {
    
    public TenantNotFoundException(UUID tenantId) {
        super("Tenant not found with ID: " + tenantId, "TENANT_NOT_FOUND");
    }
    
    public TenantNotFoundException(String slug) {
        super("Tenant not found with slug: " + slug, "TENANT_NOT_FOUND");
    }
}
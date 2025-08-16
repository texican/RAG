package com.enterprise.rag.shared.exception;

import java.util.UUID;

public class TenantAccessDeniedException extends RagException {
    
    public TenantAccessDeniedException(UUID userId, UUID tenantId) {
        super("User " + userId + " does not have access to tenant " + tenantId, "TENANT_ACCESS_DENIED");
    }
    
    public TenantAccessDeniedException(String message) {
        super(message, "TENANT_ACCESS_DENIED");
    }
}
package com.enterprise.rag.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Data transfer object for updating existing tenant information in the RAG system.
 * 
 * <p>This record allows selective updates to tenant properties, supporting partial
 * modifications where only changed fields need to be specified. All fields are
 * optional, enabling flexible tenant management and configuration updates.</p>
 * 
 * <p>Update operations supported:</p>
 * <ul>
 *   <li>Organization name changes for rebranding or corrections</li>
 *   <li>Administrative contact email updates</li>
 *   <li>Descriptive metadata modifications</li>
 *   <li>Null values indicate fields should remain unchanged</li>
 * </ul>
 * 
 * <p>Business rules:</p>
 * <ul>
 *   <li>Tenant ID cannot be modified through updates</li>
 *   <li>Status changes require separate suspend/activate operations</li>
 *   <li>Updates preserve existing tenant data isolation</li>
 *   <li>Administrative email changes may require verification</li>
 * </ul>
 * 
 * @param name updated organization name (2-100 characters, null to keep current)
 * @param adminEmail updated primary administrator email (valid format, null to keep current)
 * @param description updated organizational description (max 255 characters, null to keep current)
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.0
 * @see TenantResponse
 * @see TenantCreateRequest
 * @see com.enterprise.rag.admin.controller.TenantManagementController
 */
public record TenantUpdateRequest(
        @Size(min = 2, max = 100, message = "Tenant name must be between 2 and 100 characters")
        String name,
        
        @Email(message = "Admin email must be valid")
        String adminEmail,
        
        @Size(max = 255, message = "Description cannot exceed 255 characters")
        String description
) {
}
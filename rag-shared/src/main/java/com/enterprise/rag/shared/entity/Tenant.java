package com.enterprise.rag.shared.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a tenant in the multi-tenant Enterprise RAG system.
 * 
 * <p>A tenant is an isolated unit of organization that contains users, documents, and configurations.
 * Each tenant has resource limits and operates independently from other tenants to ensure
 * data isolation and security in the multi-tenant architecture.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Unique slug-based identification for URL-friendly tenant references</li>
 *   <li>Resource limits for documents and storage consumption</li>
 *   <li>Status management for tenant lifecycle (active, suspended, inactive)</li>
 *   <li>One-to-many relationships with users and documents</li>
 *   <li>Database-level indexing for performance optimization</li>
 * </ul>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * Tenant tenant = new Tenant("Acme Corporation", "acme-corp");
 * tenant.setDescription("Enterprise customer with AI document processing needs");
 * tenant.setMaxDocuments(5000);
 * tenant.setMaxStorageMb(50000L); // 50GB limit
 * tenantRepository.save(tenant);
 * }</pre>
 * 
 * @author Enterprise RAG Development Team
 * @since 1.0.0
 * @version 1.0
 * @see User
 * @see Document
 * @see TenantStatus
 */
@Entity
@Table(name = "tenants", indexes = {
    @Index(name = "idx_tenant_slug", columnList = "slug", unique = true),
    @Index(name = "idx_tenant_status", columnList = "status")
})
public class Tenant extends BaseEntity {

    /**
     * The human-readable name of the tenant organization.
     * Used for display purposes in the UI.
     */
    @NotBlank
    @Size(min = 2, max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * URL-friendly unique identifier for the tenant.
     * Used in API endpoints and routing to identify tenant context.
     * Must contain only lowercase letters, numbers, and hyphens.
     */
    @NotBlank
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must contain only lowercase letters, numbers, and hyphens")
    @Size(min = 2, max = 50)
    @Column(name = "slug", nullable = false, unique = true, length = 50)
    private String slug;

    /**
     * Optional description of the tenant organization.
     * Can be used for administrative notes or customer information.
     */
    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Current operational status of the tenant.
     * Controls access to tenant resources and functionality.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TenantStatus status = TenantStatus.ACTIVE;

    /**
     * Maximum number of documents the tenant can store.
     * Used for resource limiting and billing purposes.
     */
    @Column(name = "max_documents")
    private Integer maxDocuments = 1000;

    /**
     * Maximum storage capacity in megabytes the tenant can use.
     * Default is 10GB (10240 MB).
     */
    @Column(name = "max_storage_mb")
    private Long maxStorageMb = 10240L; // 10GB default

    /**
     * List of users belonging to this tenant.
     * Managed with cascade operations for data consistency.
     */
    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<User> users = new ArrayList<>();

    /**
     * List of documents belonging to this tenant.
     * Managed with cascade operations for data consistency.
     */
    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Document> documents = new ArrayList<>();

    /**
     * Default constructor required by JPA.
     */
    public Tenant() {}

    /**
     * Convenience constructor for creating a new tenant with name and slug.
     *
     * @param name the human-readable name of the tenant
     * @param slug the URL-friendly unique identifier
     */
    public Tenant(String name, String slug) {
        this.name = name;
        this.slug = slug;
    }

    /**
     * Gets the human-readable name of the tenant.
     *
     * @return the tenant name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the human-readable name of the tenant.
     *
     * @param name the tenant name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the URL-friendly unique identifier of the tenant.
     *
     * @return the tenant slug
     */
    public String getSlug() {
        return slug;
    }

    /**
     * Sets the URL-friendly unique identifier of the tenant.
     *
     * @param slug the tenant slug to set (must match the pattern ^[a-z0-9-]+$)
     */
    public void setSlug(String slug) {
        this.slug = slug;
    }

    /**
     * Gets the optional description of the tenant.
     *
     * @return the tenant description, or null if not set
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the optional description of the tenant.
     *
     * @param description the tenant description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the current operational status of the tenant.
     *
     * @return the tenant status
     */
    public TenantStatus getStatus() {
        return status;
    }

    /**
     * Sets the operational status of the tenant.
     *
     * @param status the tenant status to set
     */
    public void setStatus(TenantStatus status) {
        this.status = status;
    }

    /**
     * Gets the maximum number of documents allowed for this tenant.
     *
     * @return the maximum document count
     */
    public Integer getMaxDocuments() {
        return maxDocuments;
    }

    /**
     * Sets the maximum number of documents allowed for this tenant.
     *
     * @param maxDocuments the maximum document count to set
     */
    public void setMaxDocuments(Integer maxDocuments) {
        this.maxDocuments = maxDocuments;
    }

    /**
     * Gets the maximum storage capacity in megabytes for this tenant.
     *
     * @return the maximum storage in MB
     */
    public Long getMaxStorageMb() {
        return maxStorageMb;
    }

    /**
     * Sets the maximum storage capacity in megabytes for this tenant.
     *
     * @param maxStorageMb the maximum storage in MB to set
     */
    public void setMaxStorageMb(Long maxStorageMb) {
        this.maxStorageMb = maxStorageMb;
    }

    /**
     * Gets the list of users belonging to this tenant.
     * <p>Note: This returns the actual list, not a copy. Modifications to the returned
     * list will affect the entity's state.</p>
     *
     * @return the list of users
     */
    public List<User> getUsers() {
        return users;
    }

    /**
     * Sets the list of users belonging to this tenant.
     *
     * @param users the list of users to set
     */
    public void setUsers(List<User> users) {
        this.users = users;
    }

    /**
     * Gets the list of documents belonging to this tenant.
     * <p>Note: This returns the actual list, not a copy. Modifications to the returned
     * list will affect the entity's state.</p>
     *
     * @return the list of documents
     */
    public List<Document> getDocuments() {
        return documents;
    }

    /**
     * Sets the list of documents belonging to this tenant.
     *
     * @param documents the list of documents to set
     */
    public void setDocuments(List<Document> documents) {
        this.documents = documents;
    }

    /**
     * Convenience method to check if the tenant is currently active.
     *
     * @return true if the tenant status is ACTIVE, false otherwise
     */
    public boolean isActive() {
        return status == TenantStatus.ACTIVE;
    }

    /**
     * Enumeration of possible tenant operational states.
     * Controls access to tenant resources and functionality.
     */
    public enum TenantStatus {
        /** Tenant is active and can access all functionality */
        ACTIVE,
        /** Tenant is temporarily suspended, access restricted */
        SUSPENDED,
        /** Tenant is inactive, no access to functionality */
        INACTIVE
    }
}
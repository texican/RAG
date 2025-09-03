package com.byo.rag.shared.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a user in the multi-tenant Enterprise RAG system.
 * 
 * <p><strong>✅ Production Ready & Database-Integrated (2025-09-03):</strong> Fully operational 
 * user entity with complete PostgreSQL integration and multi-tenant support deployed in Docker.</p>
 * 
 * <p>Users are scoped to specific tenants and have roles that determine their
 * access permissions within the system. Each user can upload documents, perform
 * RAG queries, and manage their profile within their tenant's boundaries.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Email-based unique identification across the entire system</li>
 *   <li>Role-based access control (ADMIN, USER, READER)</li>
 *   <li>Status management for user lifecycle and security</li>
 *   <li>Email verification workflow support</li>
 *   <li>Audit trail with last login tracking</li>
 *   <li>Tenant isolation for multi-tenancy</li>
 * </ul>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * User user = new User("John", "Doe", "john.doe@acme.com", tenant);
 * user.setRole(UserRole.ADMIN);
 * user.setPasswordHash(passwordEncoder.encode("secure-password"));
 * user.setEmailVerified(true);
 * userRepository.save(user);
 * }</pre>
 * 
 * @author Enterprise RAG Development Team
 * @since 1.0.0
 * @version 1.0
 * @see Tenant
 * @see Document
 * @see UserRole
 * @see UserStatus
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email", unique = true),
    @Index(name = "idx_user_tenant", columnList = "tenant_id"),
    @Index(name = "idx_user_status", columnList = "status")
})
public class User extends BaseEntity {

    /** User's first name for display and identification purposes. */
    @NotBlank
    @Size(min = 2, max = 100)
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    /** User's last name for display and identification purposes. */
    @NotBlank
    @Size(min = 2, max = 100)
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    /** 
     * User's email address, used as unique identifier for authentication.
     * Must be unique across the entire system.
     */
    @NotBlank
    @Email
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    /**
     * Hashed password for authentication.
     * Should never store plain text passwords.
     */
    @NotBlank
    @Size(min = 8)
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /** User's role determining their access permissions within the tenant. */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role = UserRole.USER;

    /** Current operational status of the user account. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    /** Timestamp of the user's most recent successful login. */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /** Flag indicating whether the user's email address has been verified. */
    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    /** Token used for email verification workflow. */
    @Column(name = "email_verification_token")
    private String emailVerificationToken;

    /** The tenant organization this user belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    /** List of documents uploaded by this user. */
    @OneToMany(mappedBy = "uploadedBy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Document> uploadedDocuments = new ArrayList<>();

    public User() {}

    public User(String firstName, String lastName, String email, Tenant tenant) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.tenant = tenant;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getEmailVerificationToken() {
        return emailVerificationToken;
    }

    public void setEmailVerificationToken(String emailVerificationToken) {
        this.emailVerificationToken = emailVerificationToken;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public List<Document> getUploadedDocuments() {
        return uploadedDocuments;
    }

    public void setUploadedDocuments(List<Document> uploadedDocuments) {
        this.uploadedDocuments = uploadedDocuments;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public boolean hasRole(UserRole requiredRole) {
        return role == requiredRole || (role == UserRole.ADMIN && requiredRole != UserRole.ADMIN);
    }

    public enum UserRole {
        ADMIN,
        USER,
        READER
    }

    public enum UserStatus {
        ACTIVE,
        SUSPENDED,
        INACTIVE,
        PENDING_VERIFICATION
    }
}
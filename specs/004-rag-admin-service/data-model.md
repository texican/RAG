# Data Model: RAG Admin Service

**Feature**: RAG Admin Service  
**Date**: 2025-09-20  
**Phase**: 1 - Data Model Documentation

## Overview

The RAG Admin Service utilizes a shared entity model from the `rag-shared` module to ensure data consistency across the entire Enterprise RAG system. This approach provides standardized data structures while allowing admin-specific repository implementations for specialized queries and operations.

## Entity Architecture

### Shared Entity Foundation

**Base Entity Pattern**:
- All entities extend `BaseEntity` class
- UUID primary keys for distributed system compatibility
- Automatic timestamp management (created/updated)
- Consistent audit trail across all entities

## Core Entity Models

### Tenant Entity Model

**Purpose**: Multi-tenant organization unit with configuration and operational management

**Database Mapping**:
```sql
Table: tenants
Indexes:
  - idx_tenant_slug (UNIQUE): slug
  - idx_tenant_status: status
```

**Entity Structure**:
```java
@Entity
@Table(name = "tenants")
public class Tenant extends BaseEntity {
    // Core identification
    @NotBlank @Size(min=2, max=100)
    private String name;              // Organization display name
    
    @NotBlank @Pattern(regexp="^[a-z0-9-]+$") @Size(min=2, max=50)
    private String slug;              // URL-friendly identifier (UNIQUE)
    
    @Size(max=500)
    private String description;       // Optional organization details
    
    // Operational status
    @Enumerated(EnumType.STRING)
    private TenantStatus status;      // ACTIVE, SUSPENDED, INACTIVE
    
    // Resource management
    private Integer maxDocuments;     // Document count limit (default: 1000)
    private Long maxStorageMb;        // Storage limit in MB (default: 10GB)
    
    // Relationships
    @OneToMany(mappedBy="tenant", cascade=CascadeType.ALL)
    private List<User> users;
    
    @OneToMany(mappedBy="tenant", cascade=CascadeType.ALL)
    private List<Document> documents;
}
```

**Validation Rules**:
- **Name**: 2-100 characters, required, human-readable
- **Slug**: 2-50 characters, lowercase+numbers+hyphens only, system-wide unique
- **Description**: Optional, max 500 characters
- **Status**: Must be valid TenantStatus enum value
- **Resource Limits**: Positive integers, enforced at application level

**Status State Machine**:
```
ACTIVE ←→ SUSPENDED
   ↓         ↓
 INACTIVE ←--+
```

**Business Rules**:
- Slug cannot be changed after creation (immutable)
- Resource limits cannot be reduced below current usage
- Status transitions must follow defined workflow
- Cascade deletion affects all related users and documents

### User Entity Model

**Purpose**: Individual user accounts with role-based permissions and tenant association

**Database Mapping**:
```sql
Table: users
Indexes:
  - idx_user_email (UNIQUE): email
  - idx_user_tenant: tenant_id
  - idx_user_status: status
```

**Entity Structure**:
```java
@Entity
@Table(name = "users")
public class User extends BaseEntity {
    // Identity information
    @NotBlank @Size(min=2, max=100)
    private String firstName;         // User first name
    
    @NotBlank @Size(min=2, max=100)
    private String lastName;          // User last name
    
    @NotBlank @Email
    private String email;             // Unique authentication identifier
    
    // Authentication
    @NotBlank @Size(min=8)
    private String passwordHash;      // BCrypt encrypted password
    
    // Authorization and status
    @Enumerated(EnumType.STRING)
    private UserRole role;            // ADMIN, USER, READER
    
    @Enumerated(EnumType.STRING)
    private UserStatus status;        // ACTIVE, SUSPENDED, INACTIVE, PENDING_VERIFICATION
    
    // Audit and verification
    private LocalDateTime lastLoginAt;      // Last successful login
    private Boolean emailVerified;          // Email verification status
    private String emailVerificationToken; // Email verification token
    
    // Multi-tenant relationship
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="tenant_id", nullable=false)
    private Tenant tenant;           // Required tenant association
    
    // Document relationships
    @OneToMany(mappedBy="uploadedBy", cascade=CascadeType.ALL)
    private List<Document> uploadedDocuments;
}
```

**Validation Rules**:
- **Email**: System-wide unique, valid email format, required
- **Names**: 2-100 characters each, required
- **Password**: Minimum 8 characters, BCrypt hashed storage
- **Role**: Must be valid UserRole enum (ADMIN, USER, READER)
- **Status**: Must be valid UserStatus enum
- **Tenant**: Required foreign key, cannot be null

**Role Hierarchy**:
```
ADMIN (full tenant access) > USER (standard operations) > READER (read-only)
```

**Status Lifecycle**:
```
PENDING_VERIFICATION → ACTIVE ←→ SUSPENDED
                          ↓
                      INACTIVE
```

**Security Features**:
- **Password Storage**: BCrypt hashing with salt
- **Email Verification**: Token-based verification workflow
- **Multi-Factor Support**: Framework for future MFA implementation
- **Audit Trail**: Last login tracking for security monitoring

### Admin Entity Considerations

**Admin User Pattern**:
The admin service uses the standard User entity with specific constraints:
- **Role Requirement**: Must have `UserRole.ADMIN`
- **Status Requirement**: Must have `UserStatus.ACTIVE`
- **Authentication**: Uses AdminJwtService for token management
- **Authorization**: Admin-specific business logic in service layer

**Admin-Specific Features**:
- Enhanced JWT tokens with admin role claims
- Access to admin-only repositories and services
- Elevated permissions for system-wide operations
- Specialized audit logging for administrative actions

### Audit Log Entity Model

**Purpose**: Comprehensive tracking of administrative actions for compliance and security

**Conceptual Structure** (Implementation in shared module):
```java
public class AuditLog extends BaseEntity {
    private String userId;           // Administrator who performed action
    private String tenantId;         // Target tenant (if applicable)
    private String action;           // Type of action performed
    private String resourceType;     // Type of resource affected
    private String resourceId;       // Specific resource identifier
    private String oldValues;        // JSON of previous values
    private String newValues;        // JSON of new values
    private LocalDateTime timestamp; // When action occurred
    private String ipAddress;        // Source IP address
    private String userAgent;        // Client user agent
    private Boolean success;         // Whether action succeeded
}
```

**Audit Requirements**:
- **Retention**: 7 years minimum for compliance
- **Immutability**: Audit logs cannot be modified after creation
- **Completeness**: All admin operations must be logged
- **Privacy**: Sensitive data anonymized in logs

### System Metrics Entity Model

**Purpose**: Performance, usage, and health data for monitoring and analytics

**Conceptual Structure**:
```java
public class SystemMetrics extends BaseEntity {
    private String metricType;       // PERFORMANCE, USAGE, HEALTH
    private String name;             // Metric name identifier
    private Double value;            // Numeric metric value
    private String unit;             // Unit of measurement
    private String tenantId;         // Associated tenant (if tenant-specific)
    private LocalDateTime timestamp; // Metric collection time
    private String tags;             // JSON object with metadata
}
```

**Metrics Aggregation**:
- **Real-time**: Kept for 24 hours
- **Hourly**: Aggregated for 30 days
- **Daily**: Aggregated for 1 year
- **Monthly**: Aggregated for 5 years

## Entity Relationships

### Multi-Tenant Data Architecture

**Tenant-Centric Design**:
```
                    Tenant (1)
                   /          \
              (Many)            (Many)
             User               Document
             /                      \
        (Many)                   (Many)
    UploadedDocument         DocumentChunk
```

**Relationship Details**:

1. **Tenant → User (1:M)**:
   - Foreign Key: `users.tenant_id → tenants.id`
   - Cascade: Tenant deletion transfers users to system tenant
   - Constraint: All users (except SUPER_ADMIN) must have tenant

2. **Tenant → Document (1:M)**:
   - Foreign Key: `documents.tenant_id → tenants.id`
   - Cascade: Tenant deletion archives documents
   - Isolation: Documents scoped to tenant

3. **User → Document (1:M)**:
   - Foreign Key: `documents.uploaded_by → users.id`
   - Relationship: Tracks document ownership
   - Audit: Maintains upload attribution

### Data Isolation Strategy

**Multi-Tenant Isolation**:
- **Row-Level Security**: Tenant ID in all tenant-scoped queries
- **Application-Level Filtering**: Service layer enforces tenant context
- **Admin Override**: SUPER_ADMIN can access cross-tenant data
- **Cascade Policies**: Defined behavior for tenant lifecycle events

**Admin Access Patterns**:
- **Tenant-Scoped Admins**: Access limited to their tenant
- **System Admins**: Cross-tenant access for system management
- **Audit Context**: All admin operations include tenant context

## Database Constraints and Indexing

### Performance Optimization

**Primary Indexes**:
- **Tenants**: `idx_tenant_slug` (unique), `idx_tenant_status`
- **Users**: `idx_user_email` (unique), `idx_user_tenant`, `idx_user_status`
- **Audit Logs**: `idx_audit_timestamp`, `idx_audit_user`, `idx_audit_tenant`

**Query Optimization**:
- **Tenant Lookups**: Slug-based routing for O(1) tenant resolution
- **User Authentication**: Email-based index for fast login
- **Cross-Tenant Queries**: Tenant ID indexes for admin operations
- **Temporal Queries**: Timestamp indexes for audit and metrics

### Data Integrity

**Foreign Key Constraints**:
- All entity relationships enforced at database level
- Cascade rules defined for data lifecycle management
- Referential integrity maintained across service boundaries

**Validation Layers**:
1. **Database**: Column constraints, foreign keys, check constraints
2. **JPA**: Bean validation annotations on entity fields
3. **Service**: Business rule validation in service layer
4. **API**: Request validation in controller layer

## Admin Service Data Operations

### Repository Pattern Implementation

**Admin-Specific Repositories**:
- **TenantRepository**: Extended queries for admin tenant management
- **UserRepository**: Admin-scoped user queries and operations
- **Custom Queries**: JPA custom queries for complex admin operations

**Data Access Patterns**:
- **Paginated Queries**: Large dataset handling for admin interfaces
- **Filtered Searches**: Status-based and role-based filtering
- **Aggregation Queries**: Usage statistics and analytics
- **Audit Queries**: Administrative action tracking

### Transaction Management

**Admin Transaction Scope**:
- **Single Entity**: Simple CRUD operations
- **Multi-Entity**: Tenant creation with user setup
- **Cross-Service**: Coordination with other RAG services
- **Audit Integration**: Automatic audit log creation

**Consistency Guarantees**:
- **ACID Compliance**: Database-level transaction guarantees
- **Business Rules**: Service-level consistency enforcement
- **Multi-Tenant Isolation**: Tenant-scoped transaction boundaries
- **Error Recovery**: Rollback and compensation strategies

## Status Summary

**Completed Tasks**:
- ✅ T011: Tenant entity model documented
- ✅ T012: User entity model documented  
- ✅ T013: Admin entity patterns documented
- ✅ T014: Audit log structure documented
- ✅ T015: Entity relationships mapped
- ✅ T016: Database constraints and validation documented

**Key Achievements**:
- **Comprehensive Entity Model**: All core entities documented with relationships
- **Multi-Tenant Architecture**: Isolation patterns and data access documented
- **Security Model**: Authentication, authorization, and audit patterns defined
- **Performance Considerations**: Indexing and query optimization strategies
- **Data Integrity**: Validation layers and constraint enforcement

**Next Phase**: Operational Documentation (T017-T021)
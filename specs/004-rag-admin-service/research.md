---
version: 1.0.0
last-updated: 2025-11-12
status: archived
applies-to: 0.8.0-SNAPSHOT
category: specifications
---

# Research: RAG Admin Service

**Feature**: RAG Admin Service  
**Date**: 2025-09-20  
**Phase**: 0 - Architecture Analysis

## T001: Project Structure Analysis

### Spring Boot Configuration Analysis

**Main Application Class**: `AdminServiceApplication.java`
- **Framework**: Spring Boot 3.x with auto-configuration
- **Entity Management**: Uses shared entities from `rag-shared` module via `@EntityScan`
- **Repository Pattern**: Admin-specific repositories in `com.byo.rag.admin.repository`
- **Scheduling**: Enabled for background maintenance tasks via `@EnableScheduling`

**Key Architectural Decisions**:

1. **Shared Entity Model**:
   - Decision: Use entities from `rag-shared` module
   - Rationale: Ensures data consistency across all RAG services
   - Pattern: `@EntityScan(basePackages = {"com.byo.rag.shared.entity"})`

2. **Repository Isolation**:
   - Decision: Admin-specific repository implementations
   - Rationale: Allows admin-specific queries while maintaining shared entities
   - Pattern: `@EnableJpaRepositories(basePackages = {"com.byo.rag.admin.repository"})`

3. **Background Processing**:
   - Decision: Scheduled task support for maintenance operations
   - Rationale: Automated cleanup, monitoring, and administrative tasks
   - Pattern: `@EnableScheduling` with task scheduling capabilities

### Project Structure

```
rag-admin-service/
├── src/main/java/com/byo/rag/admin/
│   ├── AdminServiceApplication.java    # Main application class
│   ├── config/                         # Configuration classes
│   │   ├── AdminSecurityConfig.java    # Security configuration
│   │   └── package-info.java
│   ├── controller/                     # REST API controllers
│   │   ├── AdminAuthController.java    # Authentication endpoints
│   │   ├── TenantManagementController.java # Tenant management
│   │   └── package-info.java
│   ├── dto/                           # Data Transfer Objects
│   │   ├── AdminLoginRequest.java      # Authentication DTOs
│   │   ├── TenantCreateRequest.java    # Tenant operation DTOs
│   │   └── package-info.java
│   ├── exception/                     # Exception handling
│   │   ├── GlobalExceptionHandler.java # Centralized error handling
│   │   └── package-info.java
│   ├── repository/                    # Data access layer
│   │   ├── TenantRepository.java       # Tenant data operations
│   │   ├── UserRepository.java         # User data operations
│   │   └── package-info.java
│   ├── security/                      # Security components
│   │   ├── AdminAuthenticationEntryPoint.java
│   │   ├── AdminJwtAuthenticationFilter.java
│   │   └── package-info.java
│   └── service/                       # Business logic layer
│       ├── AdminJwtService.java        # JWT token management
│       ├── TenantService.java          # Tenant business logic
│       ├── UserService.java            # User management logic
│       └── package-info.java
├── src/main/resources/
│   └── application.yml                # Configuration properties
├── src/test/                         # Test suites
├── Dockerfile                        # Container configuration
├── pom.xml                          # Maven dependencies
└── logs/                            # Application logs
```

### Configuration Analysis

**Port Configuration**: 8085 (dedicated admin service port)
**Context Path**: `/admin/api` (clear admin namespace)
**Database**: PostgreSQL with JPA/Hibernate
**Caching**: Redis for session management
**Security**: JWT-based authentication
**Monitoring**: Actuator endpoints with Prometheus metrics

### Enterprise Readiness Assessment

**Production Features**:
- ✅ Docker containerization
- ✅ Health checks and monitoring
- ✅ Structured logging
- ✅ Comprehensive security
- ✅ Database integration
- ✅ Test coverage

**Architectural Strengths**:
- Clean separation of concerns (controller/service/repository)
- Shared entity model for consistency
- Centralized exception handling
- Security-first design
- Comprehensive audit logging

**Status**: ✅ T001 Complete - Project structure analyzed and documented

## T002: JWT Authentication Implementation Analysis

### JWT Service Architecture

**Implementation Class**: `AdminJwtService.java`
- **Algorithm**: HMAC-SHA256 for cryptographic signing
- **Library**: jsonwebtoken (JJWT) for JWT operations
- **Security**: Configurable secret key and expiration

**Key Authentication Patterns**:

1. **Token Generation Pattern**:
   ```java
   generateToken(String username, List<String> roles)
   ```
   - **Subject Claim**: Administrator username/email
   - **Custom Claims**: Administrative roles for RBAC
   - **Temporal Claims**: Issued-at and expiration timestamps
   - **Signature**: HMAC-SHA256 with configurable secret

2. **Token Validation Pattern**:
   ```java
   isTokenValid(String token)
   ```
   - **Multi-layer Validation**: Null check → Structure → Signature → Expiration
   - **Exception Safety**: Returns false for any validation failure
   - **Security-First**: No information leakage on validation errors

3. **Claims Extraction Pattern**:
   ```java
   extractUsername(String token)
   extractRoles(String token)
   ```
   - **Identity Extraction**: Username from subject claim
   - **Authorization Data**: Roles for access control
   - **Error Handling**: Runtime exceptions for invalid tokens

### Security Implementation Analysis

**Cryptographic Security**:
- **Algorithm**: HMAC-SHA256 (industry standard)
- **Key Management**: Injectable secret key via configuration
- **Key Derivation**: UTF-8 byte conversion for key generation
- **Signature Verification**: Automatic verification during parsing

**Token Lifecycle Management**:
- **Configurable Expiration**: Injectable expiration time in milliseconds
- **Temporal Validation**: Automatic expiration checking
- **Issued-At Claims**: Creation timestamp tracking
- **Safe Expiration**: Grace period handling for clock skew

**Error Handling Security**:
- **Information Hiding**: No leakage of validation failure reasons
- **Exception Safety**: Graceful degradation for malformed tokens
- **Null Safety**: Comprehensive null and empty string checking
- **Side-Channel Protection**: Consistent timing behavior

### Integration Patterns

**Spring Boot Configuration**:
```yaml
spring.security.jwt.secret: ${JWT_SECRET}
spring.security.jwt.expiration: 86400000  # 24 hours
```

**Service Integration Points**:
- **AdminAuthController**: Token generation during login
- **Security Filters**: Token validation on protected endpoints
- **Authorization Logic**: Role extraction for access decisions
- **Session Management**: Stateless authentication support

**Dependency Injection**:
- **Constructor Injection**: Immutable configuration values
- **Value Annotation**: Spring configuration property binding
- **Service Annotation**: Spring component registration

### Security Assessment

**Strengths**:
- ✅ Industry-standard HMAC-SHA256 algorithm
- ✅ Comprehensive validation with multiple layers
- ✅ Secure error handling without information leakage
- ✅ Configurable expiration and secret management
- ✅ Role-based claims for fine-grained authorization

**Security Considerations**:
- 🔍 Secret key strength depends on configuration
- 🔍 Token lifetime management via expiration settings
- 🔍 No token revocation mechanism (stateless design)
- 🔍 Role claims trusted without re-validation

**Status**: ✅ T002 Complete - JWT authentication patterns documented

## T003: PostgreSQL Database Schema Analysis

### Database Configuration

**Database Setup**:
- **Database**: PostgreSQL (`jdbc:postgresql`)
- **Schema**: `rag_enterprise` database
- **Connection**: Configurable host/port with credential injection
- **Driver**: PostgreSQL JDBC driver

**JPA Configuration**:
```yaml
hibernate:
  ddl-auto: update         # Schema evolution management
  dialect: PostgreSQLDialect # PostgreSQL-specific optimizations
  format_sql: true         # Readable SQL in logs
  use_sql_comments: true   # Documentation in generated SQL
```

### Entity Relationship Analysis

**Core Entity Model** (from `rag-shared` module):

#### Tenant Entity
- **Table**: `tenants`
- **Primary Key**: UUID (inherited from BaseEntity)
- **Unique Constraints**: `slug` (URL-friendly identifier)
- **Key Fields**:
  - `name`: Organization name (2-100 chars, required)
  - `slug`: URL identifier (2-50 chars, lowercase+numbers+hyphens)
  - `description`: Optional details (max 500 chars)
  - `status`: Enum (ACTIVE, SUSPENDED, INACTIVE)
  - `max_documents`: Resource limit (default 1000)
  - `max_storage_mb`: Storage limit (default 10GB)

#### User Entity
- **Table**: `users`
- **Primary Key**: UUID (inherited from BaseEntity)
- **Unique Constraints**: `email` (system-wide uniqueness)
- **Key Fields**:
  - `first_name`, `last_name`: Identity (2-100 chars each)
  - `email`: Authentication identifier (unique, validated)
  - `password_hash`: Encrypted password (min 8 chars)
  - `role`: Enum (ADMIN, USER, READER)
  - `status`: Enum (ACTIVE, SUSPENDED, INACTIVE, PENDING_VERIFICATION)
  - `email_verified`: Boolean verification status
  - `last_login_at`: Audit timestamp

### Database Relationships

**Multi-Tenant Architecture**:
```
Tenant (1) ←→ (M) User
   ↓
Tenant (1) ←→ (M) Document
```

**Relationship Details**:
- **Tenant → Users**: One-to-Many with cascade operations
- **Tenant → Documents**: One-to-Many with cascade operations
- **User → Documents**: One-to-Many for uploaded documents
- **Foreign Keys**: All relationships use UUID foreign keys

### Indexing Strategy

**Performance Optimization**:
- **Tenant Indexes**:
  - `idx_tenant_slug`: Unique index for URL routing
  - `idx_tenant_status`: Query optimization for status filters
- **User Indexes**:
  - `idx_user_email`: Unique index for authentication
  - `idx_user_tenant`: Multi-tenant query optimization
  - `idx_user_status`: Status-based filtering

### Multi-Tenant Isolation

**Data Isolation Pattern**:
- **Row-Level Security**: Tenant ID in all tenant-scoped entities
- **Query Filtering**: Application-level tenant context
- **Cascade Operations**: Tenant deletion affects related entities
- **Foreign Key Constraints**: Referential integrity enforcement

**Status**: ✅ T003 Complete - Database schema and relationships documented

## T004: Redis Session Management Analysis

### Redis Configuration

**Connection Setup**:
```yaml
redis:
  host: ${REDIS_HOST:localhost}
  port: ${REDIS_PORT:6379}
  database: 3                    # Dedicated admin service database
  timeout: 2000ms               # Connection timeout
  lettuce:
    pool:
      max-active: 8             # Maximum connections
      max-idle: 8               # Idle connection pool
      min-idle: 0               # Minimum idle connections
```

### Caching Strategy

**Administrative Session Management**:
- **Database Isolation**: Database 3 for admin-specific data
- **Connection Pooling**: Lettuce connection pool for performance
- **Timeout Management**: 2-second connection timeout
- **Spring Integration**: Spring Data Redis auto-configuration

**Use Cases**:
- **JWT Token Management**: Session state for stateless auth
- **Admin Session Tracking**: Active administrator sessions
- **Cache Layer**: Frequently accessed admin data
- **Rate Limiting**: Admin operation throttling

**Session Architecture**:
- **Stateless Design**: JWT tokens with Redis for session enhancement
- **Admin Context**: Separate Redis database for admin operations
- **Performance**: Sub-millisecond cache operations
- **Scalability**: Distributed session management

**Status**: ✅ T004 Complete - Redis session management documented

## T005: Test Coverage Assessment

### Testing Framework

**Spring Boot Test Integration**:
- **Framework**: Spring Boot Test with JUnit 5
- **Database Testing**: TestContainers for PostgreSQL integration
- **Mocking**: WireMock for external service simulation
- **Test Database**: H2 for unit test isolation

### Test Coverage Analysis

**Controller Layer Tests**:
- `AdminAuthControllerTest`: Authentication endpoint testing
- `AdminAuthControllerIntegrationTest`: End-to-end auth flows
- `TenantManagementControllerTest`: Tenant CRUD operations

**Service Layer Tests**:
- `AdminJwtServiceTest`: Token generation and validation
- `TenantServiceImplTest`: Business logic validation
- `UserServiceImplTest`: User management operations

**Integration Tests**:
- **TestContainers**: Real PostgreSQL database testing
- **Security Tests**: Authentication and authorization flows
- **Repository Tests**: Database interaction validation

### Test Quality Assessment

**Strengths**:
- ✅ Multi-layer testing (unit, integration, end-to-end)
- ✅ Real database testing with TestContainers
- ✅ Security-focused test coverage
- ✅ Mock integration for external dependencies

**Coverage Areas**:
- **Authentication Flows**: Login, logout, token refresh
- **Admin Operations**: Tenant and user management
- **Security Scenarios**: Authorization and access control
- **Database Operations**: Entity persistence and queries

**Testing Patterns**:
- **Test Configuration**: Dedicated test configurations
- **Data Cleanup**: Proper test isolation
- **Mock Services**: External dependency simulation
- **Assertion Coverage**: Comprehensive validation

**Status**: ✅ T005 Complete - Test coverage assessment documented

---

## Phase 1 Analysis Summary

**Completed Tasks**:
- ✅ T001: Project structure and Spring Boot configuration analyzed
- ✅ T002: JWT authentication implementation documented  
- ✅ T003: PostgreSQL database schema and relationships mapped
- ✅ T004: Redis session management strategy documented
- ✅ T005: Test coverage and quality assessment completed

**Key Findings**:
- **Architecture**: Clean Spring Boot microservice with proper layering
- **Security**: Enterprise-grade JWT authentication with RBAC
- **Database**: Well-designed multi-tenant PostgreSQL schema
- **Performance**: Redis caching with connection pooling
- **Quality**: Comprehensive test coverage with TestContainers

**Next Phase**: API Documentation (T006-T010)
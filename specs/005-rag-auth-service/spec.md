---
version: 1.0.0
last-updated: 2025-11-12
status: archived
applies-to: 0.8.0-SNAPSHOT
category: specifications
---

# RAG Authentication Service Specification

## Overview

The RAG Authentication Service is a critical microservice providing centralized authentication and authorization for the Enterprise RAG system. It manages user credentials, JWT tokens, and tenant-based access control across all platform services.

## Current Status

**✅ Production Ready & Fully Operational (2025-09-20)**

- All tests passing (14/14)
- Docker deployment functional at port 8081
- PostgreSQL integration complete
- JWT token management operational
- Multi-tenant authentication working

## Service Architecture

### Core Responsibilities

1. **User Authentication**: Email/password validation and JWT token generation
2. **Token Management**: JWT access/refresh token lifecycle management
3. **User Registration**: New user account creation with email verification
4. **Multi-Tenant Security**: Tenant-aware access control and isolation
5. **Session Management**: Secure session handling and lifecycle

### Technical Stack

- **Framework**: Spring Boot 3.2.8 with Spring Security
- **Database**: PostgreSQL with JPA/Hibernate
- **Authentication**: JWT tokens with BCrypt password hashing
- **API Documentation**: OpenAPI 3 with Swagger
- **Testing**: JUnit 5 with Spring Boot Test

## API Endpoints

### Authentication Operations

#### POST /api/v1/auth/login
**Purpose**: Authenticate user and return JWT tokens

**Request**:
```json
{
  "email": "user@example.com",
  "password": "securePassword123"
}
```

**Response**:
```json
{
  "accessToken": "eyJ0eXAi...",
  "refreshToken": "eyJ0eXAi...",
  "expiresIn": 3600,
  "user": {
    "id": "uuid",
    "firstName": "John",
    "lastName": "Doe",
    "email": "user@example.com",
    "role": "USER",
    "status": "ACTIVE",
    "emailVerified": true,
    "lastLoginAt": "2025-09-20T10:30:00Z",
    "createdAt": "2025-09-15T08:00:00Z"
  }
}
```

#### POST /api/v1/auth/refresh
**Purpose**: Generate new access token using refresh token

**Request**:
```json
{
  "refreshToken": "eyJ0eXAi..."
}
```

**Response**: Same as login response with new tokens

#### POST /api/v1/auth/validate
**Purpose**: Validate JWT token (for other services)

**Request**:
```json
{
  "token": "eyJ0eXAi..."
}
```

**Response**:
```json
{
  "valid": true
}
```

### User Management Operations

#### POST /api/v1/auth/register
**Purpose**: Register new user (public endpoint)

**Request**:
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "user@example.com",
  "password": "securePassword123",
  "tenantId": "uuid"
}
```

**Response**:
```json
{
  "id": "uuid",
  "firstName": "John",
  "lastName": "Doe",
  "email": "user@example.com",
  "role": "USER",
  "status": "PENDING_VERIFICATION",
  "emailVerified": false,
  "createdAt": "2025-09-20T10:30:00Z"
}
```

#### POST /api/v1/auth/verify-email
**Purpose**: Verify user email with token

**Request**:
```json
{
  "token": "verification_token_here"
}
```

**Response**: User object with updated verification status

## Security Features

### Authentication Security

1. **Password Hashing**: BCrypt with configurable strength
2. **JWT Tokens**: Stateless authentication with tenant claims
3. **Token Rotation**: Automatic refresh token rotation
4. **Account Status Validation**: Active status verification
5. **Rate Limiting**: Brute force protection (configured at gateway level)

### Multi-Tenant Isolation

1. **Tenant-Aware Tokens**: JWT claims include tenant context
2. **Data Isolation**: Complete separation of tenant user data
3. **Access Control**: Tenant-based authorization rules
4. **Cross-Tenant Prevention**: No data leakage between tenants

### Defensive Security

1. **Input Validation**: Comprehensive request validation
2. **Error Handling**: Generic errors to prevent enumeration
3. **Audit Logging**: Security event logging
4. **Account Lockout**: Failed attempt tracking (future enhancement)

## Data Model

### User Entity

```java
@Entity
@Table(name = "users")
public class User extends BaseEntity {
    @Column(nullable = false, length = 100)
    private String firstName;
    
    @Column(nullable = false, length = 100)
    private String lastName;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String passwordHash;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role; // ADMIN, USER, READER
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status; // ACTIVE, SUSPENDED, INACTIVE, PENDING_VERIFICATION
    
    @Column(nullable = false)
    private Boolean emailVerified = false;
    
    @Column
    private String emailVerificationToken;
    
    @Column
    private LocalDateTime lastLoginAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;
}
```

### JWT Token Structure

```json
{
  "sub": "user@example.com",
  "tenant_id": "uuid",
  "user_id": "uuid",
  "role": "USER",
  "token_type": "access", // or "refresh"
  "iat": 1632150000,
  "exp": 1632153600
}
```

## Service Dependencies

### Internal Dependencies

1. **rag-shared**: Shared entities, DTOs, and utilities
2. **PostgreSQL**: Primary data storage
3. **Redis**: Token blacklist and caching (future enhancement)

### External Integration Points

1. **rag-gateway**: Token validation for all requests
2. **rag-admin-service**: Administrative user management
3. **rag-document-service**: User-based document access
4. **rag-core-service**: User context for RAG operations
5. **rag-embedding-service**: User-based embedding access

## Configuration

### Application Properties

```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/rag_auth
    username: ${DB_USERNAME:rag_user}
    password: ${DB_PASSWORD:rag_password}
    
  jpa:
    hibernate:
      ddl-auto: validate
    database-platform: org.hibernate.dialect.PostgreSQLDialect

jwt:
  secret: ${JWT_SECRET:default_secret_for_dev}
  access-token-expiration: 3600 # 1 hour
  refresh-token-expiration: 2592000 # 30 days

logging:
  level:
    com.byo.rag.auth: INFO
    org.springframework.security: DEBUG
```

### Docker Configuration

```dockerfile
FROM openjdk:21-jre-slim
COPY target/rag-auth-service-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## Testing Strategy

### Unit Tests

1. **Service Layer Tests**: Authentication logic and token management
2. **Controller Tests**: API endpoint validation
3. **Security Tests**: JWT token generation and validation
4. **Repository Tests**: Database operations

### Integration Tests

1. **Database Configuration**: JPA configuration and entity mapping
2. **Security Configuration**: Spring Security setup
3. **Service Startup**: Application context loading
4. **Circular Dependency Prevention**: Bean configuration validation

### Test Coverage

- **Current**: 14/14 tests passing
- **Coverage Areas**: Authentication, token management, user operations
- **Missing Areas**: Email service integration, rate limiting tests

## Performance Characteristics

### Throughput

- **Login Operations**: ~1000 req/sec (estimated)
- **Token Validation**: ~5000 req/sec (stateless validation)
- **Registration**: ~500 req/sec (with email verification)

### Latency

- **Authentication**: <100ms (with database lookup)
- **Token Validation**: <10ms (cryptographic validation)
- **Token Refresh**: <50ms (minimal database access)

### Scalability

- **Stateless Design**: Horizontal scaling supported
- **Database Connection Pooling**: HikariCP for efficient connections
- **JWT Tokens**: No server-side session storage required

## Monitoring and Observability

### Health Checks

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
```

### Metrics

1. **Authentication Success/Failure Rates**
2. **Token Generation/Validation Counts**
3. **Database Connection Pool Status**
4. **Response Time Distributions**

### Logging

1. **Security Events**: Login attempts, token operations
2. **Error Tracking**: Authentication failures, validation errors
3. **Audit Trail**: User registration, email verification
4. **Performance Metrics**: Response times, throughput

## Future Enhancements

### Security Improvements

1. **Account Lockout**: Failed attempt tracking and lockout
2. **Password Policies**: Complexity requirements and expiration
3. **Multi-Factor Authentication**: TOTP/SMS second factor
4. **OAuth2/OIDC**: External identity provider integration

### Performance Optimizations

1. **Redis Caching**: User session and token blacklist caching
2. **Token Blacklist**: Logout and compromised token management
3. **Database Optimization**: Query optimization and indexing
4. **Connection Pooling**: Tuned connection pool parameters

### Feature Extensions

1. **Password Reset**: Secure password reset workflow
2. **Social Login**: Google/Microsoft/GitHub integration
3. **API Keys**: Service-to-service authentication
4. **Audit Logs**: Comprehensive security audit logging

## Deployment Considerations

### Production Requirements

1. **Environment Variables**: Secure configuration management
2. **Database Migration**: Flyway/Liquibase for schema changes
3. **Load Balancing**: Multiple service instances
4. **SSL/TLS**: Secure communication channels

### Monitoring Setup

1. **Application Metrics**: Prometheus/Grafana dashboard
2. **Log Aggregation**: ELK stack or similar
3. **Alert Configuration**: Critical error and performance alerts
4. **Health Check**: Kubernetes readiness/liveness probes

### Security Hardening

1. **Network Security**: VPC/firewall configuration
2. **Secrets Management**: Kubernetes secrets or external vault
3. **Certificate Management**: Automated certificate rotation
4. **Security Scanning**: Container and dependency scanning
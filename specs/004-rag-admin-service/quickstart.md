---
version: 1.0.0
last-updated: 2025-11-12
status: archived
applies-to: 0.8.0-SNAPSHOT
category: specifications
---

# Quickstart Guide: RAG Admin Service

**Feature**: RAG Admin Service  
**Date**: 2025-09-20  
**Version**: 1.0.0

## Overview

This comprehensive guide walks through setting up, deploying, and using the RAG Admin Service for managing tenants, users, and system operations in the Enterprise RAG environment. The admin service provides centralized administrative capabilities through secure REST APIs with full audit logging.

## Prerequisites

### System Requirements
- **Java**: OpenJDK 17 or higher
- **Maven**: 3.8.0 or higher
- **Docker**: 20.10+ with Docker Compose
- **PostgreSQL**: 12+ (for production) or via Docker
- **Redis**: 6.0+ (for session management) or via Docker

### Development Environment
- **IDE**: IntelliJ IDEA, Eclipse, or VS Code with Java extensions
- **Git**: For source code management
- **Postman**: For API testing (optional)
- **cURL**: For command-line API testing

## Quick Start

### 1. Environment Setup

**Clone and Navigate**:
```bash
cd /path/to/rag-project/rag-admin-service
```

**Start Infrastructure Dependencies**:
```bash
# Start PostgreSQL and Redis using Docker Compose
cd ..
docker-compose up -d postgres redis

# Verify services are running
docker ps | grep -E "(postgres|redis)"
```

**Environment Variables**:
```bash
# Create .env file or export variables
export DB_HOST=localhost
export DB_NAME=rag_enterprise
export DB_USERNAME=rag_user
export DB_PASSWORD=rag_password
export REDIS_HOST=localhost
export REDIS_PORT=6379
export JWT_SECRET=your-256-bit-secret-key-here
export SERVER_PORT=8085
```

### 2. Service Deployment

**Option A: Docker Deployment (Recommended)**
```bash
# Build Docker image
docker build -t rag-admin-service .

# Run containerized service
docker run -d --name rag-admin-service \
  -p 8085:8085 \
  -e DB_HOST=host.docker.internal \
  -e DB_NAME=rag_enterprise \
  -e DB_USERNAME=rag_user \
  -e DB_PASSWORD=rag_password \
  -e REDIS_HOST=host.docker.internal \
  -e JWT_SECRET=your-256-bit-secret-key-here \
  --network rag-network \
  rag-admin-service:latest
```

**Option B: Local Development**
```bash
# Install dependencies and compile
mvn clean compile

# Run with development profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Or run compiled JAR
mvn clean package -DskipTests
java -jar target/rag-admin-service-*.jar
```

**Option C: Production Deployment**
```bash
# Production build
mvn clean package -Pprod

# Run with production configuration
java -jar target/rag-admin-service-*.jar \
  --spring.profiles.active=prod \
  --server.port=8085
```

### 3. Service Verification

**Health Check**:
```bash
# Basic health verification
curl http://localhost:8085/admin/api/actuator/health

# Expected Response:
# {"status":"UP","components":{"db":{"status":"UP"},"redis":{"status":"UP"}}}
```

**Service Information**:
```bash
# Get service info and metrics
curl http://localhost:8085/admin/api/actuator/info
curl http://localhost:8085/admin/api/actuator/metrics
```

**API Documentation**:
- **Swagger UI**: http://localhost:8085/admin/api/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8085/admin/api/v3/api-docs

## Administrative Operations

### Authentication Workflow

#### 1. Admin Login
```bash
# Authenticate as admin user
curl -X POST http://localhost:8085/admin/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin@company.com",
    "password": "secure-admin-password"
  }'
```

**Expected Response**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "admin@company.com",
  "roles": ["ADMIN"],
  "expiresIn": 86400000
}
```

#### 2. Save Authentication Token
```bash
# Export token for subsequent requests
export ADMIN_TOKEN="your-access-token-here"

# Or save to file for session persistence
echo "export ADMIN_TOKEN=\"your-access-token-here\"" > .admin-session
source .admin-session
```

#### 3. Validate Authentication
```bash
# Verify token is valid and get current user info
curl -X GET http://localhost:8085/admin/api/auth/me \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Expected Response:
# {"username": "admin@company.com", "roles": ["ADMIN"]}
```

### Tenant Management

#### 1. Create New Tenant
```bash
curl -X POST http://localhost:8085/admin/api/tenants \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Acme Corporation",
    "slug": "acme-corp",
    "description": "Enterprise customer with AI document processing needs",
    "maxDocuments": 5000,
    "maxStorageMb": 50000
  }'
```

**Expected Response**:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Acme Corporation",
  "slug": "acme-corp",
  "description": "Enterprise customer with AI document processing needs",
  "status": "ACTIVE",
  "maxDocuments": 5000,
  "maxStorageMb": 50000,
  "userCount": 0,
  "documentCount": 0,
  "createdAt": "2025-09-20T10:30:00Z",
  "updatedAt": "2025-09-20T10:30:00Z"
}
```

#### 2. List All Tenants
```bash
# Get paginated tenant list with default settings
curl -X GET http://localhost:8085/admin/api/tenants \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Get specific page with custom sorting
curl -X GET "http://localhost:8085/admin/api/tenants?page=0&size=20&sort=name,asc" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

#### 3. Get Tenant Details
```bash
# Retrieve specific tenant information
curl -X GET http://localhost:8085/admin/api/tenants/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

#### 4. Update Tenant Configuration
```bash
curl -X PUT http://localhost:8085/admin/api/tenants/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Acme Corporation Ltd",
    "description": "Updated enterprise customer description",
    "maxDocuments": 7500,
    "maxStorageMb": 75000
  }'
```

#### 5. Tenant Status Management

**Suspend Tenant**:
```bash
curl -X POST http://localhost:8085/admin/api/tenants/550e8400-e29b-41d4-a716-446655440000/suspend \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Policy violation - inappropriate content detected",
    "notifyUsers": true,
    "adminNotes": "Suspended pending investigation"
  }'
```

**Reactivate Tenant**:
```bash
curl -X POST http://localhost:8085/admin/api/tenants/550e8400-e29b-41d4-a716-446655440000/reactivate \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

#### 6. Delete Tenant (Irreversible)
```bash
# CAUTION: This permanently deletes all tenant data
curl -X DELETE http://localhost:8085/admin/api/tenants/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Expected Response: HTTP 204 No Content
```

### User Management

#### 1. List Users (All Tenants)
```bash
# Get all users across all tenants
curl -X GET http://localhost:8085/admin/api/users \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Filter users by specific tenant
curl -X GET "http://localhost:8085/admin/api/users?tenantId=550e8400-e29b-41d4-a716-446655440000" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

#### 2. Create User Account
```bash
curl -X POST http://localhost:8085/admin/api/users \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@acme.com",
    "password": "secure-user-password",
    "role": "USER",
    "tenantId": "550e8400-e29b-41d4-a716-446655440000"
  }'
```

#### 3. Update User Account
```bash
curl -X PUT http://localhost:8085/admin/api/users/user-uuid-here \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Smith",
    "email": "john.smith@acme.com",
    "role": "ADMIN",
    "status": "ACTIVE"
  }'
```

#### 4. Disable User Account
```bash
curl -X PUT http://localhost:8085/admin/api/users/user-uuid-here \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "SUSPENDED"
  }'
```

#### 5. Delete User Account
```bash
curl -X DELETE http://localhost:8085/admin/api/users/user-uuid-here \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### System Monitoring

#### 1. System Health Status
```bash
# Comprehensive health check
curl -X GET http://localhost:8085/admin/api/system/health \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Expected Response:
# {
#   "status": "UP",
#   "database": "CONNECTED",
#   "redis": "CONNECTED",
#   "services": ["auth", "tenant", "user"],
#   "timestamp": "2025-09-20T10:30:00Z"
# }
```

#### 2. System Metrics and Analytics
```bash
# Get operational metrics
curl -X GET http://localhost:8085/admin/api/system/metrics \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Get usage statistics
curl -X GET http://localhost:8085/admin/api/system/stats \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

#### 3. Audit Log Access
```bash
# View recent administrative actions
curl -X GET "http://localhost:8085/admin/api/system/audit?limit=50&offset=0" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Filter audit logs by date range
curl -X GET "http://localhost:8085/admin/api/system/audit?startDate=2025-09-19&endDate=2025-09-20" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

## Development Workflow

### 1. Local Development Setup

**IDE Configuration**:
```bash
# Import project into IDE
# - IntelliJ IDEA: Open pom.xml as project
# - Eclipse: Import as Maven project
# - VS Code: Open folder with Java Extension Pack

# Configure JDK 17
# Set JAVA_HOME to JDK 17 installation
```

**Database Development**:
```bash
# Use H2 for rapid development (in application-dev.yml)
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  h2:
    console:
      enabled: true  # Access at http://localhost:8085/admin/api/h2-console
```

**Hot Reload Development**:
```bash
# Enable Spring Boot DevTools for hot reload
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Or use IDE run configuration with:
# - Main class: com.byo.rag.admin.AdminServiceApplication
# - VM options: -Dspring.profiles.active=dev
# - Program arguments: --server.port=8085
```

### 2. Testing

**Unit Tests**:
```bash
# Run all unit tests
mvn test

# Run specific test class
mvn test -Dtest=AdminJwtServiceTest

# Run tests with coverage
mvn test jacoco:report
```

**Integration Tests**:
```bash
# Run integration tests (requires TestContainers)
mvn test -Dtest=*IntegrationTest

# Run specific integration test
mvn test -Dtest=AdminAuthControllerIntegrationTest
```

**API Testing with Postman**:
```bash
# Import Postman collection
# File: /postman/BYO_RAG_Admin_Service.postman_collection.json
# 
# Collection includes:
# - Authentication workflows
# - Tenant management operations
# - User administration tasks
# - System monitoring endpoints
```

### 3. Code Quality and Standards

**Code Formatting**:
```bash
# Format code using Maven plugin
mvn spotless:apply

# Check code style compliance
mvn spotless:check
```

**Static Analysis**:
```bash
# Run SpotBugs analysis
mvn spotbugs:check

# Run PMD analysis
mvn pmd:check

# Run Checkstyle validation
mvn checkstyle:check
```

**Test Coverage**:
```bash
# Generate test coverage report
mvn clean test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

## Configuration Management

### Environment-Specific Configuration

**Development (application-dev.yml)**:
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:devdb
  jpa:
    show-sql: true
  security:
    jwt:
      secret: dev-secret-key
      expiration: 3600000  # 1 hour for development

logging:
  level:
    com.byo.rag.admin: DEBUG
    org.springframework.security: DEBUG
```

**Production (application-prod.yml)**:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}
  jpa:
    show-sql: false
  security:
    jwt:
      secret: ${JWT_SECRET}  # Must be 256+ bits
      expiration: 86400000   # 24 hours

logging:
  level:
    com.byo.rag.admin: INFO
    org.springframework.security: WARN
  file:
    name: /var/log/rag-admin-service.log
```

### Security Configuration

**JWT Secret Management**:
```bash
# Generate secure 256-bit secret
openssl rand -base64 32

# Or use online generator
# Store in environment variable or secure vault
export JWT_SECRET="generated-256-bit-secret-key"
```

**Database Security**:
```bash
# Create dedicated database user
psql -U postgres -c "CREATE USER rag_admin WITH PASSWORD 'secure-password';"
psql -U postgres -c "GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO rag_admin;"
```

**CORS Configuration**:
```yaml
admin:
  security:
    cors:
      allowed-origins: 
        - https://admin.rag-enterprise.com
        - https://admin.staging.rag-enterprise.com
      allowed-methods: GET,POST,PUT,DELETE,OPTIONS
      allow-credentials: true
```

## Monitoring and Observability

### Application Metrics

**Prometheus Integration**:
```bash
# Access Prometheus metrics
curl http://localhost:8085/admin/api/actuator/prometheus

# Key metrics to monitor:
# - jvm_memory_used_bytes
# - http_server_requests_seconds
# - spring_data_repository_invocations_seconds
# - admin_operations_total
```

**Custom Metrics Configuration**:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: rag-admin-service
      environment: ${ENVIRONMENT:dev}
```

### Logging Configuration

**Structured Logging**:
```yaml
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [%X{traceId},%X{spanId}] %logger{36} - %msg%n"
  level:
    com.byo.rag.admin: DEBUG
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG
```

**Log Aggregation**:
```bash
# Use Logback configuration for ELK stack
# File: src/main/resources/logback-spring.xml
# 
# Configure log shipping to:
# - Elasticsearch for search and analysis
# - Kibana for visualization and dashboards
# - Logstash for log processing and transformation
```

### Health Checks

**Kubernetes Health Probes**:
```yaml
# Liveness probe
livenessProbe:
  httpGet:
    path: /admin/api/actuator/health/liveness
    port: 8085
  initialDelaySeconds: 30
  periodSeconds: 10

# Readiness probe
readinessProbe:
  httpGet:
    path: /admin/api/actuator/health/readiness
    port: 8085
  initialDelaySeconds: 10
  periodSeconds: 5
```

## Troubleshooting

### Common Issues

#### 1. Database Connection Failed
```bash
# Symptoms: Service fails to start with database errors
# Check database connectivity
pg_isready -h localhost -p 5432 -U rag_user

# Verify credentials
psql -h localhost -U rag_user -d rag_enterprise -c "SELECT 1;"

# Check network connectivity
telnet localhost 5432
```

**Solutions**:
- Verify database is running and accessible
- Check connection parameters in configuration
- Ensure database user has proper permissions
- Verify network connectivity and firewall rules

#### 2. Redis Connection Issues
```bash
# Check Redis connectivity
redis-cli -h localhost -p 6379 ping

# Test authentication (if configured)
redis-cli -h localhost -p 6379 -a password ping

# Check Redis logs
docker logs redis-container-name
```

**Solutions**:
- Ensure Redis is running and accessible
- Verify Redis configuration in application.yml
- Check Redis authentication and network settings
- Validate Redis database number configuration

#### 3. Authentication Failures
```bash
# Check JWT secret configuration
echo $JWT_SECRET | wc -c  # Should be 32+ characters for 256-bit key

# Verify admin user exists in database
psql -U rag_user -d rag_enterprise -c "SELECT email, role, status FROM users WHERE role = 'ADMIN';"

# Check token validity
curl -X GET http://localhost:8085/admin/api/auth/me \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

**Solutions**:
- Ensure JWT secret is properly configured (256+ bits)
- Verify admin user exists with ADMIN role and ACTIVE status
- Check password hash and authentication logic
- Validate token format and expiration

#### 4. Performance Issues
```bash
# Check application metrics
curl http://localhost:8085/admin/api/actuator/metrics/jvm.memory.used
curl http://localhost:8085/admin/api/actuator/metrics/http.server.requests

# Monitor database performance
psql -U rag_user -d rag_enterprise -c "SELECT * FROM pg_stat_activity;"

# Check Redis performance
redis-cli --latency -h localhost -p 6379
```

**Solutions**:
- Monitor JVM memory usage and garbage collection
- Analyze database query performance and indexing
- Review Redis cache hit rates and memory usage
- Check network latency and connection pooling

### Log Analysis

**Application Logs**:
```bash
# View real-time logs
tail -f logs/admin-service.log

# Search for errors
grep -i "error\|exception" logs/admin-service.log

# Analyze authentication patterns
grep "Admin login" logs/admin-service.log | head -20
```

**Security Monitoring**:
```bash
# Monitor failed login attempts
grep "Invalid credentials\|Authentication failed" logs/admin-service.log

# Track administrative actions
grep "successfully authenticated\|Tenant created\|User deleted" logs/admin-service.log

# Analyze access patterns
grep "Authorization: Bearer" logs/admin-service.log | wc -l
```

## Security Best Practices

### Production Security Checklist

- [ ] **JWT Secret**: Use 256+ bit randomly generated secret
- [ ] **Database Security**: Dedicated user with minimal privileges
- [ ] **HTTPS**: Enable TLS/SSL for all communications
- [ ] **CORS**: Configure allowed origins for production domains
- [ ] **Authentication**: Enforce strong password policies
- [ ] **Authorization**: Validate role-based access controls
- [ ] **Audit Logging**: Enable comprehensive audit trails
- [ ] **Network Security**: Use firewalls and VPN access
- [ ] **Secrets Management**: Use vault for sensitive configuration
- [ ] **Regular Updates**: Keep dependencies and base images updated

### Security Monitoring

**Alert Configuration**:
- Failed authentication attempts (threshold: 5 per minute)
- Unusual administrative activity patterns
- Database connection anomalies
- High error rates or performance degradation
- Unauthorized access attempts

**Compliance Requirements**:
- SOC 2 Type II audit logging
- GDPR data protection and right to deletion
- HIPAA compliance for healthcare customers
- PCI DSS for payment processing integration

## Next Steps

### Advanced Configuration
- Set up monitoring dashboards with Grafana
- Configure alerting with Prometheus AlertManager
- Implement log aggregation with ELK stack
- Set up automated backups and disaster recovery

### Integration Development
- Connect with external identity providers (SAML/OIDC)
- Implement webhook notifications for tenant events
- Add bulk import/export capabilities
- Develop admin dashboard frontend application

### Operational Procedures
- Create runbooks for common administrative tasks
- Establish backup and recovery procedures
- Document incident response protocols
- Plan capacity management and scaling strategies

---

For additional assistance, refer to the comprehensive API documentation at http://localhost:8085/admin/api/swagger-ui.html or contact the Enterprise RAG development team.
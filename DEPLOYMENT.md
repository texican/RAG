# Enterprise RAG System - Complete Deployment Guide

A comprehensive deployment guide for the Enterprise RAG (Retrieval Augmented Generation) system with automated setup scripts for development, staging, and production environments.

## 📋 Table of Contents

- [Quick Start (Automated)](#-quick-start-automated)
- [Prerequisites](#-prerequisites)
- [Development Deployment](#-development-deployment)
- [Production Deployment](#-production-deployment)
- [Environment Configuration](#-environment-configuration)
- [Monitoring & Observability](#-monitoring--observability)
- [Security & Best Practices](#-security--best-practices)
- [Troubleshooting](#-troubleshooting)
- [Scripts Reference](#-scripts-reference)

## 🚀 Quick Start (Automated)

### One-Command Setup

```bash
# Get the complete RAG system running with a single command
./scripts/utils/quick-start.sh
```

This automated script will:
1. ✅ Validate prerequisites (Java 21+, Maven, Docker)
2. 🐳 Start all infrastructure services (PostgreSQL, Redis, Kafka, Ollama)
3. 🔨 Build all 6 microservices with Maven
4. 🚀 Start all services in the correct order
5. 🏥 Run comprehensive health checks
6. 🧪 Execute integration tests
7. 📊 Display service URLs and monitoring dashboards

### Alternative: Step-by-Step Setup

```bash
# 1. Complete environment setup
./scripts/setup/setup-local-dev.sh

# 2. Start all services
./scripts/services/start-all-services.sh

# 3. Verify system health
./scripts/utils/health-check.sh

# 4. Run integration tests
./scripts/tests/test-system.sh
```

### Management Commands

```bash
# Stop all services
./scripts/services/stop-all-services.sh

# Reset development environment
./scripts/utils/dev-reset.sh

# Monitor system health
./scripts/utils/health-check.sh
```

## 🔧 Prerequisites

### System Requirements

| Environment | CPU | RAM | Storage | Network |
|------------|-----|-----|---------|---------|
| **Development** | 4+ cores | 8+ GB | 50+ GB | 100 Mbps |
| **Production** | 16+ cores | 32+ GB | 500+ GB SSD | 10+ Gbps |

### Required Software

```bash
# Check if you have the required software
java -version      # Java 21+ required
mvn -version       # Maven 3.8+ required
docker --version   # Docker 24.0+ required
docker-compose --version  # Docker Compose 2.0+ required
```

### Installation (if needed)

```bash
# macOS (using Homebrew)
brew install openjdk@21 maven docker docker-compose

# Ubuntu/Debian
apt-get update
apt-get install openjdk-21-jdk maven docker.io docker-compose-plugin

# Verify installation
./scripts/setup/setup-local-dev.sh --help
```

## 💻 Development Deployment

### Automated Development Setup

The development setup is completely automated. The setup script handles:

- **Infrastructure Services**: PostgreSQL, Redis Stack, Kafka, Zookeeper, Ollama, Prometheus, Grafana
- **Service Dependencies**: Proper startup order with health checks
- **Environment Configuration**: Automatic `.env` file creation
- **Database Integration**: Complete JPA persistence with PostgreSQL (production) and H2 (testing)
- **Testing**: All 58 tests passing (47 unit + 11 integration tests)

```bash
# Complete setup with all options
./scripts/setup/setup-local-dev.sh --verbose

# Skip Docker if already running
./scripts/setup/setup-local-dev.sh --skip-docker

# Skip Maven build (if already built)
./scripts/setup/setup-local-dev.sh --skip-build

# Get help and see all options
./scripts/setup/setup-local-dev.sh --help
```

### Service Architecture & Ports

| Service | Port | Description | Database |
|---------|------|-------------|----------|
| **rag-gateway** | 8080 | API Gateway (main entry) | - |
| **rag-auth-service** | 8081 | Authentication & tenants | PostgreSQL |
| **rag-core-service** | 8082 | RAG query engine | PostgreSQL |
| **rag-document-service** | 8083 | Document processing | PostgreSQL |
| **rag-embedding-service** | 8084 | Vector operations | Redis Stack |
| **rag-admin-service** | 8085 | **Admin ops + Database** | **PostgreSQL + H2** |

### Infrastructure Services

| Service | Port | Credentials | Description |
|---------|------|-------------|-------------|
| **PostgreSQL** | 5432 | `rag_user`/`rag_password` | Primary database with pgvector |
| **Redis Stack** | 6379, 8001 | `redis_password` | Vector storage + RediSearch + UI |
| **Kafka** | 9092 | - | Message queue for async processing |
| **Ollama** | 11434 | - | Local LLM inference |
| **Prometheus** | 9090 | - | Metrics collection |
| **Grafana** | 3000 | `admin`/`admin` | Monitoring dashboards |
| **Kafka UI** | 8080 | - | Kafka management interface |

### Development URLs

After successful setup, access these URLs:

```bash
# Application Services
- API Gateway: http://localhost:8080/actuator/health
- Auth Service: http://localhost:8081/swagger-ui.html
- Admin Service: http://localhost:8085/admin/api/swagger-ui.html  # NEW with Database!
- Document Service: http://localhost:8083/swagger-ui.html
- Embedding Service: http://localhost:8084/swagger-ui.html
- RAG Core: http://localhost:8082/swagger-ui.html

# Infrastructure & Monitoring
- Grafana Dashboard: http://localhost:3000 (admin/admin)
- Prometheus Metrics: http://localhost:9090
- Kafka UI: http://localhost:8080 (if not conflicting)
- Redis Insight: http://localhost:8001
```

### Testing the System

The automation scripts include comprehensive testing:

```bash
# Quick system test
./scripts/tests/test-system.sh

# Manual testing examples
# 1. Admin Service (Database-backed)
curl -X POST http://localhost:8085/admin/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin@enterprise.com", "password": "admin123"}'

# 2. Create Tenant
curl -X POST http://localhost:8081/api/v1/tenants/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Company",
    "slug": "test-company", 
    "description": "Test tenant"
  }'

# 3. Upload Document
curl -X POST http://localhost:8083/api/v1/documents/upload \
  -H "Authorization: Bearer <jwt-token>" \
  -F "file=@sample-document.pdf"

# 4. Query RAG System  
curl -X POST http://localhost:8082/api/v1/rag/query \
  -H "Authorization: Bearer <jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{"query": "What is this document about?", "maxResults": 5}'
```

## 🏭 Production Deployment

### Production-Ready Features

The RAG system includes enterprise-grade production features:

- **Multi-tenant data isolation** at database level
- **Horizontal auto-scaling** with Kubernetes HPA
- **Load balancing** with Nginx ingress
- **SSL/TLS termination** with Let's Encrypt
- **Comprehensive monitoring** with Prometheus/Grafana
- **Database backups** and disaster recovery
- **Security policies** and network isolation

### Kubernetes Deployment

#### 1. Prepare Production Environment

```bash
# Create production environment file
cat > .env.production << 'EOF'
# Database (use managed PostgreSQL in production)
DB_HOST=your-postgres-host.amazonaws.com
DB_USERNAME=rag_prod_user
DB_PASSWORD=super-secure-password

# Redis (use managed Redis in production)  
REDIS_HOST=your-redis-cluster.cache.amazonaws.com
REDIS_PASSWORD=redis-prod-password

# Security
JWT_SECRET=your-256-bit-production-jwt-secret-key
CORS_ALLOWED_ORIGINS=https://your-domain.com

# AI/ML Services
OPENAI_API_KEY=your-production-openai-key
OLLAMA_HOST=http://ollama-service:11434

# Monitoring
PROMETHEUS_ENABLED=true
GRAFANA_ADMIN_PASSWORD=secure-grafana-password
EOF
```

#### 2. Production Kubernetes Configuration

The deployment includes production-ready Kubernetes manifests:

**Namespace & Security**:
```yaml
# k8s/namespace.yml
apiVersion: v1
kind: Namespace
metadata:
  name: enterprise-rag-prod
  labels:
    name: enterprise-rag-prod
    environment: production
```

**ConfigMaps & Secrets**:
```yaml
# k8s/configmaps/app-config.yml
apiVersion: v1
kind: ConfigMap
metadata:
  name: rag-app-config
  namespace: enterprise-rag-prod
data:
  SPRING_PROFILES_ACTIVE: "production"
  DB_HOST: "postgres-service"
  REDIS_HOST: "redis-service"
  KAFKA_BOOTSTRAP_SERVERS: "kafka-service:9092"
```

**Deployments with Auto-scaling**:
```yaml
# k8s/deployments/admin-service.yml (NEW - Database integrated)
apiVersion: apps/v1
kind: Deployment
metadata:
  name: rag-admin-service
  namespace: enterprise-rag-prod
spec:
  replicas: 1
  selector:
    matchLabels:
      app: rag-admin-service
  template:
    spec:
      containers:
      - name: rag-admin-service
        image: enterprise-rag/admin-service:latest
        ports:
        - containerPort: 8085
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        envFrom:
        - configMapRef:
            name: rag-app-config
        - secretRef:
            name: rag-app-secrets
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi" 
            cpu: "1000m"
```

#### 3. Automated Production Deployment

```bash
# Deploy to production (requires --confirm flag)
./scripts/deploy/deploy-production.sh --confirm

# Monitor deployment
kubectl get pods -n enterprise-rag-prod
kubectl logs -f deployment/rag-admin-service -n enterprise-rag-prod

# Health check
./scripts/utils/health-check.sh production
```

### Production Scaling Configuration

| Service | Min | Max | CPU Target | Memory Target |
|---------|-----|-----|------------|---------------|
| Gateway | 2 | 10 | 60% | 70% |
| Auth | 3 | 20 | 70% | 80% |
| **Admin** | **1** | **5** | **80%** | **85%** |
| Document | 2 | 15 | 75% | 85% |
| Embedding | 2 | 10 | 80% | 90% |
| Core | 3 | 25 | 70% | 80% |

## ⚙️ Environment Configuration

### Development Environment

The setup script automatically creates `.env` with:

```bash
# Database Configuration (Docker PostgreSQL)
POSTGRES_DB=rag_enterprise
POSTGRES_USER=rag_user  
POSTGRES_PASSWORD=rag_password

# Redis Configuration (Docker Redis Stack)
REDIS_PASSWORD=redis_password

# JWT Configuration
JWT_SECRET=admin-super-secret-key-that-should-be-at-least-256-bits-long

# AI/ML Configuration (Optional)
# OPENAI_API_KEY=your-openai-key-here
OLLAMA_HOST=http://localhost:11434

# CORS Configuration
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8080
```

### Application Profiles

**Development** (`application-local.yml`):
```yaml
logging:
  level:
    com.enterprise.rag: DEBUG
    org.springframework.security: INFO
    org.hibernate.SQL: DEBUG

spring:
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: update  # Auto-create tables in development

management:
  endpoints:
    web:
      exposure:
        include: "*"  # All actuator endpoints in dev
```

**Production** (`application-prod.yml`):
```yaml
logging:
  level:
    com.enterprise.rag: WARN
    org.springframework.security: ERROR
  file:
    name: /app/logs/application.log

spring:
  jpa:
    show-sql: false
    hibernate:
      ddl-auto: validate  # Only validate schema in production

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus  # Limited endpoints in production
  endpoint:
    health:
      show-details: never
```

## 📊 Monitoring & Observability

### Grafana Dashboards

Access Grafana at `http://localhost:3000` (admin/admin) with pre-configured dashboards:

1. **RAG System Overview**: Service health, request rates, response times
2. **Database Performance**: Connection pools, query performance, storage usage
3. **Infrastructure Monitoring**: CPU, memory, disk, network metrics
4. **Business Metrics**: Tenant usage, document processing, query analytics

### Prometheus Metrics

All services expose metrics at `/actuator/prometheus`:

- **Application Metrics**: Request counts, latencies, error rates
- **JVM Metrics**: Memory usage, GC performance, thread counts
- **Database Metrics**: Connection pool status, query performance
- **Redis Metrics**: Cache hit rates, memory usage
- **Custom Business Metrics**: Tenant activity, document counts

### Health Checks

Automated health monitoring:

```bash
# Quick health check
./scripts/utils/health-check.sh

# Detailed system analysis
curl http://localhost:8085/admin/api/actuator/health | jq .

# Service-specific metrics
curl http://localhost:8085/admin/api/actuator/metrics/jvm.memory.used
```

## 🔐 Security & Best Practices

### Database Security

The **rag-admin-service** now includes comprehensive database security:

- **Multi-tenant data isolation** with proper foreign key constraints
- **Role-based access control** with ADMIN, USER, READER roles
- **Password encryption** with BCrypt
- **SQL injection prevention** through JPA parameterized queries
- **Connection pooling** with HikariCP for production performance

### JWT Security

- **256-bit secrets** for token signing
- **Refresh token rotation** for session management
- **Tenant-scoped tokens** for multi-tenant isolation
- **Rate limiting** per tenant and endpoint

### Infrastructure Security

- **Network policies** for pod-to-pod communication
- **Pod security policies** with non-root execution
- **SSL/TLS encryption** for all external communication
- **Secret management** with Kubernetes secrets
- **Regular security updates** for base images

## 🔧 Troubleshooting

### Common Issues & Solutions

#### 1. Service Won't Start

```bash
# Check prerequisites
./scripts/setup/setup-local-dev.sh --help

# Check Docker services
docker-compose ps
docker-compose logs postgres redis kafka

# Check port conflicts
lsof -i :8085  # Check admin service port
```

#### 2. Database Connection Issues

```bash
# Test PostgreSQL connection
docker exec -it rag-postgres psql -U rag_user -d rag_enterprise

# Check admin service database integration
curl http://localhost:8085/admin/api/actuator/health
curl http://localhost:8085/admin/api/actuator/metrics/hikaricp.connections.active
```

#### 3. Integration Tests Failing

```bash
# Run specific test suite
cd rag-admin-service && mvn test -Dtest=AdminAuthControllerIntegrationTest

# Check test configuration
./scripts/tests/test-system.sh

# Reset environment
./scripts/utils/dev-reset.sh --force
./scripts/utils/quick-start.sh
```

#### 4. Performance Issues

```bash
# Check resource usage
docker stats

# Monitor JVM metrics
curl http://localhost:8085/admin/api/actuator/metrics/jvm.memory.used
curl http://localhost:8085/admin/api/actuator/metrics/jvm.gc.pause

# Database performance
curl http://localhost:8085/admin/api/actuator/metrics/data.repository.invocations
```

### Debug Commands

```bash
# Complete system status
./scripts/utils/health-check.sh

# Service logs
tail -f logs/admin-service.log

# Docker infrastructure logs  
docker-compose logs -f postgres redis kafka

# Spring Boot actuator endpoints
curl http://localhost:8085/admin/api/actuator/
curl http://localhost:8085/admin/api/actuator/env
curl http://localhost:8085/admin/api/actuator/configprops
```

## 📋 Scripts Reference

### Setup Scripts

| Script | Description | Usage |
|--------|-------------|-------|
| `scripts/setup/setup-local-dev.sh` | Complete environment setup | `./setup-local-dev.sh [--skip-docker] [--skip-build] [--verbose]` |
| `scripts/utils/quick-start.sh` | One-command full system startup | `./quick-start.sh` |
| `scripts/utils/dev-reset.sh` | Clean and reset environment | `./dev-reset.sh [--force]` |

### Service Management

| Script | Description | Usage |
|--------|-------------|-------|
| `scripts/services/start-all-services.sh` | Start all services in order | `./start-all-services.sh` |
| `scripts/services/stop-all-services.sh` | Stop all running services | `./stop-all-services.sh` |

### Monitoring & Testing

| Script | Description | Usage |
|--------|-------------|-------|
| `scripts/utils/health-check.sh` | Comprehensive health verification | `./health-check.sh` |
| `scripts/tests/test-system.sh` | Integration and system tests | `./test-system.sh` |

### Directory Structure

```
scripts/
├── setup/
│   └── setup-local-dev.sh       # Complete development setup
├── services/  
│   ├── start-all-services.sh    # Service startup automation
│   └── stop-all-services.sh     # Service shutdown automation
├── utils/
│   ├── quick-start.sh           # One-command system startup  
│   ├── health-check.sh          # System health verification
│   └── dev-reset.sh             # Environment reset utility
└── tests/
    └── test-system.sh           # Integration testing automation
```

## 🎯 What's New - Database Integration

### rag-admin-service Enhancements

The **rag-admin-service** has been completely upgraded with database integration:

✅ **Database Layer**:
- **TenantRepository**: Custom JPA queries for tenant analytics and management
- **UserRepository**: Comprehensive user management with tenant-aware operations
- **PostgreSQL** for production and **H2** for testing

✅ **Service Layer**:
- **TenantServiceImpl**: Complete database-backed tenant operations
- **UserServiceImpl**: Full user management with role-based access control
- **Transaction management** for data consistency

✅ **Testing Achievement**:
- **58 tests passing** (47 unit + 11 integration tests)
- **100% success rate** with comprehensive test coverage
- **Database integration tests** with Spring Boot context

✅ **Production Ready**:
- **Multi-tenant data isolation** at database level
- **Advanced analytics** capabilities with custom queries
- **Proper entity relationships** and foreign key constraints

## 🚀 Getting Started Summary

### For New Developers

```bash
# 1. One-command setup (recommended)
git clone <repository-url>
cd enterprise-rag
./scripts/utils/quick-start.sh

# 2. Access the system
open http://localhost:8085/admin/api/swagger-ui.html  # Admin service with database
open http://localhost:3000  # Grafana (admin/admin)

# 3. Run tests
./scripts/tests/test-system.sh
```

### For Production Deployment

```bash
# 1. Prepare production config
cp .env.production.example .env.production
# Edit .env.production with your values

# 2. Deploy to Kubernetes
./scripts/deploy/deploy-production.sh --confirm

# 3. Monitor deployment
kubectl get pods -n enterprise-rag-prod
./scripts/utils/health-check.sh production
```

## 📞 Support

For issues and questions:

1. **Check logs**: `./scripts/utils/health-check.sh`
2. **Reset environment**: `./scripts/utils/dev-reset.sh --force`
3. **Run diagnostics**: `./scripts/tests/test-system.sh`
4. **GitHub Issues**: [Create an issue](https://github.com/your-repo/issues)

---

**🎉 The Enterprise RAG System is now ready for development with complete database integration and automated deployment!**

*Last Updated: August 2025 - Database Integration Complete*
*All 58 tests passing • Production-ready with JPA persistence*
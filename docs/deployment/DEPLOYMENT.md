---
version: 1.0.0
last-updated: 2025-11-12
status: active
applies-to: 0.8.0-SNAPSHOT
category: deployment
---

# BYO RAG System - Quick Deployment Guide

[![Version](https://img.shields.io/badge/Version-0.8.0--SNAPSHOT-blue.svg)](https://semver.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-brightgreen.svg)](https://www.docker.com/)
[![Status](https://img.shields.io/badge/Status-Complete-brightgreen.svg)](https://github.com/your-org/byo-rag)

> **✅ Development Status (2025-09-30)**: 5 active microservices (gateway archived per ADR-001) with 98% test success. Full Docker deployment ready.

Quick deployment guide for the BYO RAG (Build Your Own Retrieval Augmented Generation) system.

## 🔧 Prerequisites

### System Requirements
- **RAM**: 8 GB minimum, 16 GB recommended
- **Storage**: 50 GB minimum
- **Java**: 21+ (OpenJDK recommended)
- **Maven**: 3.8+
- **Docker**: 24.0+
- **Docker Compose**: 2.0+

## 🚀 Quick Start with Docker (Recommended)

> **🆕 NEW**: All services now include comprehensive interactive API documentation via Swagger UI!

### 1. Clone and Build
```bash
# Clone repository
git clone <repository-url>
cd byo-rag

# Build all services
mvn clean package -DskipTests
```

### 2. Start All Services
```bash
# Start with Docker Compose (or use: make start)
docker-compose up -d

# Check service status (or use: make status)
docker-compose ps

# View logs (or use: make logs)
docker-compose logs -f
```

### 3. Verify Services (Direct Access - ADR-001)
```bash
# Health checks (direct service access, no gateway)
curl http://localhost:8081/actuator/health  # Auth Service
curl http://localhost:8082/actuator/health  # Document Service
curl http://localhost:8083/actuator/health  # Embedding Service
curl http://localhost:8084/actuator/health  # Core Service
curl http://localhost:8085/admin/api/actuator/health  # Admin Service
```

> **Note**: Gateway bypassed per [ADR-001](../development/ADR-001-BYPASS-API-GATEWAY.md). Services accessed directly.

### 4. Access Points
- **Auth Service**: http://localhost:8081
- **Document Service**: http://localhost:8082
- **Embedding Service**: http://localhost:8083
- **Core Service**: http://localhost:8084
- **Admin Service**: http://localhost:8085
- **Redis Insight**: http://localhost:8001
- **Database**: localhost:5432 (user: `rag_user`, password: `rag_password`)

### 5. Explore APIs Interactively (Direct Service Access)
All services provide comprehensive interactive API documentation:

**📋 Quick Access:**
```bash
# Public access (no login required)
open http://localhost:8082/swagger-ui.html  # Document Service

# Authenticated access (see credentials guide)
open http://localhost:8081/swagger-ui.html  # Auth Service
open http://localhost:8084/swagger-ui.html  # Core Service
open http://localhost:8083/swagger-ui.html  # Embedding Service
open http://localhost:8085/admin/api/swagger-ui.html  # Admin Service
```

> **Note**: Gateway removed - access services directly per ADR-001

> **📋 Complete Credentials Guide**: See [docs/deployment/SWAGGER_UI_ACCESS_GUIDE.md](SWAGGER_UI_ACCESS_GUIDE.md) for detailed access information and troubleshooting

## 🛠️ Alternative: Manual Service Startup

If you prefer to run services individually:

### 1. Start Infrastructure
```bash
# Start only database and Redis
docker-compose up -d postgres redis-stack
```

### 2. Start Application Services
```bash
# In separate terminals, start each service:
cd rag-auth-service && mvn spring-boot:run        # Port 8081
cd rag-document-service && mvn spring-boot:run    # Port 8082
cd rag-embedding-service && mvn spring-boot:run   # Port 8083
cd rag-core-service && mvn spring-boot:run        # Port 8084
cd rag-admin-service && mvn spring-boot:run       # Port 8085
# Gateway archived per ADR-001 - use direct service access
```

## ⚙️ Configuration

### Environment Variables

Create `.env` file for customization:
```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_USERNAME=rag_user
DB_PASSWORD=rag_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT
JWT_SECRET=YourVerySecureJWTSecretKeyThatIsAtLeast256BitsLongForHS256Algorithm

# AI Configuration (optional)
OPENAI_API_KEY=your-openai-api-key
OLLAMA_HOST=http://localhost:11434
```

### Default Credentials
- **Admin User**: `admin@enterprise-rag.com`
- **Admin Password**: `admin123`

## 🧪 Testing the System (Direct Service Access)

> **💡 Tip**: Use interactive API documentation for each service - see service URLs above

### 1. Authenticate (Direct Auth Service)
```bash
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@enterprise-rag.com","password":"admin123"}'

# Save the JWT token from response
```

### 2. Test Document Upload (Direct Document Service)
```bash
curl -X POST http://localhost:8082/api/documents/upload \
  -H "Authorization: Bearer <your-jwt-token>" \
  -F "file=@sample.txt"
```

### 3. Test RAG Query (Direct Core Service)
```bash
curl -X POST http://localhost:8084/api/rag/query \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{"query":"What is the main topic of the document?"}'
```

> **Note**: All requests go directly to services (no gateway) per ADR-001

## 🔧 Stopping Services

```bash
# Stop all Docker services (or use: make stop)
docker-compose down

# Stop with volume cleanup (⚠️ deletes all data!)
docker-compose down -v
```

## 🔍 Troubleshooting

### Service Won't Start
```bash
# Check specific service logs (or use: make logs SERVICE=<name>)
docker-compose logs <service-name>

# Examples:
make logs SERVICE=rag-auth
make logs SERVICE=rag-core
```

### Database Issues
```bash
# Reset database (WARNING: loses all data)
docker-compose down -v
docker-compose up -d
```

### Port Conflicts
If ports are already in use, edit `docker-compose.yml` to change port mappings:
```yaml
ports:
  - "8081:8081"  # Change first number to available port
```

## 🎯 What's Running

After successful deployment, you'll have:
- **5 active microservices** handling authentication, document processing, embeddings, RAG queries, and admin functions
- **PostgreSQL database** for persistent data
- **Redis Stack** for vector storage and caching
- **Direct service access** (gateway archived per ADR-001)
- **Full RAG pipeline** from document upload to AI-powered queries

## 📚 Next Steps

- **🚀 Start Here**: Explore interactive API documentation for each service (see URLs above)
- **📖 Architecture**: Review [README.md](README.md) for detailed architecture overview
- **🧪 Testing**: Check [TESTING_BEST_PRACTICES.md](TESTING_BEST_PRACTICES.md) for testing guidelines
- **📋 Service Guide**: See [SERVICE_CONNECTION_GUIDE.md](SERVICE_CONNECTION_GUIDE.md) for comprehensive API usage
- **📚 API Portal**: Visit [API_DOCUMENTATION_PORTAL.md](../api/API_DOCUMENTATION_PORTAL.md) for complete API reference
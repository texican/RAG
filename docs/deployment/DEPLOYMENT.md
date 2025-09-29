# BYO RAG System - Quick Deployment Guide

[![Version](https://img.shields.io/badge/Version-0.8.0--SNAPSHOT-blue.svg)](https://semver.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-brightgreen.svg)](https://www.docker.com/)
[![Status](https://img.shields.io/badge/Status-Complete-brightgreen.svg)](https://github.com/your-org/byo-rag)

> **✅ Development Status (2025-09-05)**: All 6 microservices complete with 100% test success. Full Docker deployment ready.

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
# Start with Docker Compose
docker-compose -f config/docker/docker-compose.fixed.yml up -d

# Check service status
docker-compose -f config/docker/docker-compose.fixed.yml ps

# View logs
docker-compose -f config/docker/docker-compose.fixed.yml logs -f
```

### 3. Verify Services
```bash
# Health checks
curl http://localhost:8081/actuator/health  # Auth Service
curl http://localhost:8082/actuator/health  # Document Service  
curl http://localhost:8083/actuator/health  # Embedding Service
curl http://localhost:8084/actuator/health  # Core Service
curl http://localhost:8085/admin/api/actuator/health  # Admin Service
curl http://localhost:8080/actuator/health  # Gateway Service
```

### 4. Access Points
- **API Gateway**: http://localhost:8080
- **Interactive API Documentation**: http://localhost:8080/swagger-ui.html ✨ **NEW**
- **Redis Insight**: http://localhost:8001
- **Database**: localhost:5432 (user: `rag_user`, password: `rag_password`)

### 5. Explore APIs Interactively ✨ **NEW**
All services now provide comprehensive interactive API documentation:

**📋 Quick Access with Credentials:**
```bash
# Public access (no login required)
open http://localhost:8082/swagger-ui.html  # Document Service

# Authenticated access (username: user)
# API Gateway (password: 726bcacd-081f-4a08-96e1-9037edc2ac45)
open http://localhost:8080/swagger-ui.html

# Core Service (password: 77147b40-70e6-477d-8557-fcf417e9ca9f)  
open http://localhost:8084/swagger-ui.html

# Embedding Service (password: 681650f3-b562-4c16-828a-d8a996b01217)
open http://localhost:8083/swagger-ui.html

# Admin Service (password: 5080a46c-bfef-45fc-a403-2ea299ee531d)
open http://localhost:8085/admin/api/swagger-ui.html
```

> **📋 Complete Credentials Guide**: See [docs/deployment/SWAGGER_UI_ACCESS_GUIDE.md](SWAGGER_UI_ACCESS_GUIDE.md) for detailed access information and troubleshooting

## 🛠️ Alternative: Manual Service Startup

If you prefer to run services individually:

### 1. Start Infrastructure
```bash
# Start only database and Redis
docker-compose -f config/docker/docker-compose.fixed.yml up -d postgres redis-stack
```

### 2. Start Application Services
```bash
# In separate terminals, start each service:
cd rag-auth-service && mvn spring-boot:run        # Port 8081
cd rag-document-service && mvn spring-boot:run    # Port 8082
cd rag-embedding-service && mvn spring-boot:run   # Port 8083
cd rag-core-service && mvn spring-boot:run        # Port 8084
cd rag-admin-service && mvn spring-boot:run       # Port 8085
cd rag-gateway && mvn spring-boot:run             # Port 8080
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

## 🧪 Testing the System

> **💡 Tip**: Use the interactive API documentation at http://localhost:8080/swagger-ui.html for easy testing with built-in "Try It Out" functionality!

### 1. Create a Tenant
```bash
curl -X POST http://localhost:8080/api/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@enterprise-rag.com","password":"admin123"}'

# Save the JWT token from response
```

### 2. Test Document Upload
```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -H "Authorization: Bearer <your-jwt-token>" \
  -F "file=@sample.txt"
```

### 3. Test RAG Query
```bash
curl -X POST http://localhost:8080/api/rag/query \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{"query":"What is the main topic of the document?"}'
```

## 🔧 Stopping Services

```bash
# Stop all Docker services
docker-compose -f config/docker/docker-compose.fixed.yml down

# Stop with volume cleanup
docker-compose -f config/docker/docker-compose.fixed.yml down -v
```

## 🔍 Troubleshooting

### Service Won't Start
```bash
# Check specific service logs
docker-compose -f config/docker/docker-compose.fixed.yml logs <service-name>

# Examples:
docker-compose -f config/docker/docker-compose.fixed.yml logs rag-auth
docker-compose -f config/docker/docker-compose.fixed.yml logs rag-core
```

### Database Issues
```bash
# Reset database (WARNING: loses all data)
docker-compose -f config/docker/docker-compose.fixed.yml down -v
docker-compose -f config/docker/docker-compose.fixed.yml up -d
```

### Port Conflicts
If ports are already in use, edit `config/docker/docker-compose.fixed.yml` to change port mappings:
```yaml
ports:
  - "8081:8081"  # Change first number to available port
```

## 🎯 What's Running

After successful deployment, you'll have:
- **6 microservices** handling authentication, document processing, embeddings, RAG queries, and admin functions
- **PostgreSQL database** for persistent data
- **Redis Stack** for vector storage and caching
- **API Gateway** routing requests to appropriate services
- **Full RAG pipeline** from document upload to AI-powered queries

## 📚 Next Steps

- **🚀 Start Here**: Explore interactive API documentation at http://localhost:8080/swagger-ui.html
- **📖 Architecture**: Review [README.md](README.md) for detailed architecture overview
- **🧪 Testing**: Check [TESTING_BEST_PRACTICES.md](TESTING_BEST_PRACTICES.md) for testing guidelines
- **📋 Service Guide**: See [SERVICE_CONNECTION_GUIDE.md](SERVICE_CONNECTION_GUIDE.md) for comprehensive API usage
- **📚 API Portal**: Visit [API_DOCUMENTATION_PORTAL.md](../api/API_DOCUMENTATION_PORTAL.md) for complete API reference
# RAG System Service Connection and Usage Guide

**Last Updated**: 2025-09-24  
**System Version**: 0.8.0-SNAPSHOT

---

## 🎯 Overview

This guide provides comprehensive instructions for connecting to and using all services in the BYO RAG System. The system consists of 6 microservices plus infrastructure components, all accessible through well-defined REST APIs.

## 📋 Table of Contents

- [🎯 Overview](#-overview)
- [🚀 Quick Start](#-quick-start)
- [🔐 Authentication Flow](#-authentication-flow)
- [🌐 Service Directory](#-service-directory)
- [📡 API Gateway Usage](#-api-gateway-usage)
- [🔑 Auth Service](#-auth-service)
- [📄 Document Service](#-document-service)
- [🔍 Embedding Service](#-embedding-service)
- [🤖 RAG Core Service](#-rag-core-service)
- [⚙️ Admin Service](#️-admin-service)
- [🗄️ Infrastructure Services](#️-infrastructure-services)
- [🧪 Testing & Validation](#-testing--validation)
- [🔧 Troubleshooting](#-troubleshooting)

---

## 🚀 Quick Start

### Prerequisites
Ensure all services are running:
```bash
# Start all services
./scripts/utils/quick-start.sh

# Verify system health
./config/docker/docker-health.sh
```

### System Status Check
```bash
# Check all service health
curl http://localhost:8080/actuator/health  # API Gateway
curl http://localhost:8081/actuator/health  # Auth Service
curl http://localhost:8082/actuator/health  # Document Service
curl http://localhost:8083/actuator/health  # Embedding Service
curl http://localhost:8084/actuator/health  # Core Service
curl http://localhost:8085/admin/api/actuator/health  # Admin Service
```

---

## 🔐 Authentication Flow

### Step 1: Create a Tenant (Optional - for new organizations)
```bash
curl -X POST http://localhost:8080/api/auth/tenants/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Your Company",
    "slug": "your-company",
    "description": "Your company description"
  }'
```

### Step 2: Create Admin User (First Time Setup)
```bash
# Create the default admin user (only needed first time)
./scripts/db/create-admin-user.sh
```

### Step 3: Login and Get JWT Token
```bash
# Login with default admin credentials (Admin Service)
curl -X POST http://localhost:8085/admin/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin@enterprise-rag.com",
    "password": "admin123"
  }'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600,
  "tokenType": "Bearer"
}
```

### Step 4: Use Token for Authenticated Requests
```bash
# Store token for convenience
export JWT_TOKEN="your-jwt-token-here"

# Use token in subsequent requests
curl -X GET http://localhost:8080/api/admin/tenants \
  -H "Authorization: Bearer $JWT_TOKEN"
```

---

## 🌐 Service Directory

| Service | Port | Health Check | Interactive API Docs | Purpose |
|---------|------|--------------|---------------------|---------|
| **API Gateway** | 8080 | `/actuator/health` | `/swagger-ui.html` ✨ | Request routing & security |
| **Auth Service** | 8081 | `/actuator/health` | `/swagger-ui.html` ✨ | Authentication & authorization |
| **Document Service** | 8082 | `/actuator/health` | `/swagger-ui.html` ✨ | Document processing & storage |
| **Embedding Service** | 8083 | `/actuator/health` | `/swagger-ui.html` ✨ | Vector embeddings & search |
| **RAG Core Service** | 8084 | `/actuator/health` | `/swagger-ui.html` ✨ | AI/LLM integration |
| **Admin Service** | 8085 | `/admin/api/actuator/health` | `/admin/api/swagger-ui.html` ✨ | System administration |

### 📚 **NEW: Interactive API Documentation** ✨

**All services now provide comprehensive interactive API documentation powered by OpenAPI 3.0 and Swagger UI:**

#### **🎯 Swagger UI Access Points**

**✅ Publicly Accessible:**
- **Document Service**: http://localhost:8082/swagger-ui.html (No login required)

**🔑 Authentication Required (Username: `user`):**
- **API Gateway**: http://localhost:8080/swagger-ui.html (Password: `726bcacd-081f-4a08-96e1-9037edc2ac45`)
- **Embedding Service**: http://localhost:8083/swagger-ui.html (Password: `681650f3-b562-4c16-828a-d8a996b01217`)
- **Core Service**: http://localhost:8084/swagger-ui.html (Password: `77147b40-70e6-477d-8557-fcf417e9ca9f`)
- **Admin Service**: http://localhost:8085/admin/api/swagger-ui.html (Password: `5080a46c-bfef-45fc-a403-2ea299ee531d`)

> **📋 Complete Access Guide**: See [SWAGGER_UI_ACCESS_GUIDE.md](SWAGGER_UI_ACCESS_GUIDE.md) for detailed credentials and troubleshooting

#### **🔍 Service-Specific Documentation**
Each service provides detailed API documentation with:
- **✨ Interactive Testing**: "Try It Out" functionality for live API calls
- **🔐 JWT Authentication**: Built-in token management for authenticated endpoints
- **📊 Complete Schemas**: Request/response models with examples
- **📝 Detailed Descriptions**: Comprehensive endpoint documentation
- **🏷️ Organized Groups**: Logical API grouping for easy navigation

### Infrastructure Services
| Service | Port | UI/Access | Purpose |
|---------|------|-----------|---------|
| **PostgreSQL** | 5432 | `psql -h localhost -U rag_user -d rag_enterprise` | Primary database |
| **Redis Stack** | 6379 | http://localhost:8001 | Vector storage & caching |
| **Grafana** | 3000 | http://localhost:3000 (admin/admin) | Monitoring dashboards |
| **Ollama** | 11434 | http://localhost:11434 | Local LLM inference |

---

## 📡 API Gateway Usage

**Base URL**: `http://localhost:8080`

The API Gateway is the primary entry point for all external requests. It handles authentication, routing, and load balancing.

### Route Configuration
```
/api/auth/*      → Auth Service (8081)
/api/documents/* → Document Service (8082)  
/api/embeddings/* → Embedding Service (8083)
/api/rag/*       → RAG Core Service (8084)
/api/admin/*     → Admin Service (8085)
```

### Example Gateway Requests
```bash
# Health check through gateway
curl http://localhost:8080/actuator/health

# Login through gateway
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "password"}'

# Document upload through gateway
curl -X POST http://localhost:8080/api/documents/upload \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -F "file=@document.pdf" \
  -F "description=Sample document"
```

---

## 🔑 Auth Service

**Direct URL**: `http://localhost:8081`  
**Via Gateway**: `http://localhost:8080/api/auth`

### Key Endpoints

#### User Authentication
```bash
# Login
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password"
  }'

# Refresh token
curl -X POST http://localhost:8081/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "your-refresh-token"
  }'

# Logout
curl -X POST http://localhost:8081/auth/logout \
  -H "Authorization: Bearer $JWT_TOKEN"
```

#### User Management
```bash
# Register new user
curl -X POST http://localhost:8081/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newuser@example.com",
    "password": "securepassword",
    "firstName": "John",
    "lastName": "Doe",
    "role": "USER"
  }'

# Get user profile
curl -X GET http://localhost:8081/users/profile \
  -H "Authorization: Bearer $JWT_TOKEN"
```

#### Tenant Management
```bash
# Create tenant
curl -X POST http://localhost:8081/tenants \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Acme Corp",
    "slug": "acme-corp",
    "description": "Acme Corporation tenant"
  }'

# List tenants
curl -X GET http://localhost:8081/tenants \
  -H "Authorization: Bearer $JWT_TOKEN"
```

---

## 📄 Document Service

**Direct URL**: `http://localhost:8082`  
**Via Gateway**: `http://localhost:8080/api/documents`

### Key Endpoints

#### Document Upload & Processing
```bash
# Upload document
curl -X POST http://localhost:8082/documents/upload \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -F "file=@document.pdf" \
  -F "description=Important document" \
  -F "tags=research,analysis"

# Upload multiple documents
curl -X POST http://localhost:8082/documents/batch-upload \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -F "files=@doc1.pdf" \
  -F "files=@doc2.docx" \
  -F "description=Batch upload"
```

#### Document Management
```bash
# List documents
curl -X GET http://localhost:8082/documents \
  -H "Authorization: Bearer $JWT_TOKEN"

# Get document by ID
curl -X GET http://localhost:8082/documents/{documentId} \
  -H "Authorization: Bearer $JWT_TOKEN"

# Download document
curl -X GET http://localhost:8082/documents/{documentId}/download \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -o downloaded_document.pdf

# Delete document
curl -X DELETE http://localhost:8082/documents/{documentId} \
  -H "Authorization: Bearer $JWT_TOKEN"
```

#### Document Processing Status
```bash
# Get processing status
curl -X GET http://localhost:8082/documents/{documentId}/status \
  -H "Authorization: Bearer $JWT_TOKEN"

# Get document chunks
curl -X GET http://localhost:8082/documents/{documentId}/chunks \
  -H "Authorization: Bearer $JWT_TOKEN"
```

---

## 🔍 Embedding Service

**Direct URL**: `http://localhost:8083`  
**Via Gateway**: `http://localhost:8080/api/embeddings`

### Key Endpoints

#### Embedding Generation
```bash
# Generate single embedding
curl -X POST http://localhost:8083/embeddings/generate \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "text": "This is sample text to embed",
    "model": "text-embedding-ada-002"
  }'

# Generate batch embeddings
curl -X POST http://localhost:8083/embeddings/batch \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "texts": [
      "First text to embed",
      "Second text to embed",
      "Third text to embed"
    ],
    "model": "text-embedding-ada-002"
  }'
```

#### Vector Search
```bash
# Similarity search
curl -X POST http://localhost:8083/embeddings/search \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What is machine learning?",
    "topK": 5,
    "threshold": 0.7
  }'

# Advanced search with filters
curl -X POST http://localhost:8083/embeddings/search/advanced \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "artificial intelligence applications",
    "topK": 10,
    "filters": {
      "documentType": "research",
      "dateRange": {
        "start": "2024-01-01",
        "end": "2024-12-31"
      }
    }
  }'
```

---

## 🤖 RAG Core Service

**Direct URL**: `http://localhost:8084`  
**Via Gateway**: `http://localhost:8080/api/rag`

### Key Endpoints

#### RAG Query Processing
```bash
# Simple RAG query
curl -X POST http://localhost:8084/rag/query \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What are the benefits of machine learning?",
    "maxResults": 5,
    "includeMetadata": true
  }'

# Advanced RAG query with context
curl -X POST http://localhost:8084/rag/query/advanced \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Explain deep learning architectures",
    "context": {
      "conversationId": "conv-123",
      "previousQuestions": ["What is machine learning?"]
    },
    "parameters": {
      "temperature": 0.7,
      "maxTokens": 500,
      "model": "gpt-4"
    }
  }'
```

#### Conversation Management
```bash
# Start new conversation
curl -X POST http://localhost:8084/rag/conversations \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "ML Research Discussion",
    "context": "Research project on machine learning applications"
  }'

# Get conversation history
curl -X GET http://localhost:8084/rag/conversations/{conversationId} \
  -H "Authorization: Bearer $JWT_TOKEN"

# Continue conversation
curl -X POST http://localhost:8084/rag/conversations/{conversationId}/query \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Can you elaborate on that last point?"
  }'
```

---

## ⚙️ Admin Service

**Direct URL**: `http://localhost:8085/admin/api`  
**Via Gateway**: `http://localhost:8080/api/admin`

### Key Endpoints

#### System Administration
```bash
# Admin login (separate from user login)
curl -X POST http://localhost:8085/admin/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin@enterprise-rag.com",
    "password": "admin123"
  }'

# Get system statistics
curl -X GET http://localhost:8085/admin/api/system/stats \
  -H "Authorization: Bearer $ADMIN_JWT_TOKEN"

# Get all tenants
curl -X GET http://localhost:8085/admin/api/tenants \
  -H "Authorization: Bearer $ADMIN_JWT_TOKEN"
```

#### Tenant Management
```bash
# Create tenant (admin)
curl -X POST http://localhost:8085/admin/api/tenants \
  -H "Authorization: Bearer $ADMIN_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Enterprise Client",
    "slug": "enterprise-client",
    "description": "Large enterprise client",
    "settings": {
      "maxUsers": 1000,
      "maxDocuments": 10000
    }
  }'

# Suspend tenant
curl -X PUT http://localhost:8085/admin/api/tenants/{tenantId}/suspend \
  -H "Authorization: Bearer $ADMIN_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Payment overdue",
    "suspendUntil": "2024-02-01"
  }'
```

#### User Management
```bash
# List all users
curl -X GET http://localhost:8085/admin/api/users \
  -H "Authorization: Bearer $ADMIN_JWT_TOKEN"

# Get user details
curl -X GET http://localhost:8085/admin/api/users/{userId} \
  -H "Authorization: Bearer $ADMIN_JWT_TOKEN"

# Update user role
curl -X PUT http://localhost:8085/admin/api/users/{userId}/role \
  -H "Authorization: Bearer $ADMIN_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "role": "ADMIN"
  }'
```

---

## 🗄️ Infrastructure Services

### PostgreSQL Database
```bash
# Connect to database
docker exec -it rag-postgres psql -U rag_user -d rag_enterprise

# Common queries
# List all tables
\dt

# Check users table
SELECT id, email, role, tenant_id FROM users;

# Check documents table
SELECT id, filename, status, created_at FROM documents LIMIT 10;
```

### Redis Stack (Vector Database)
```bash
# Connect to Redis CLI
docker exec -it rag-redis redis-cli

# Common commands
# List all keys
KEYS *

# Check vector documents
FT._LIST

# Search vectors (example)
FT.SEARCH vector_idx "@text:machine learning" LIMIT 0 5
```

### Grafana Monitoring
- **URL**: http://localhost:3000
- **Login**: admin/admin
- **Purpose**: System monitoring and metrics visualization

**Available Dashboards:**
- System Health Overview
- Service Performance Metrics
- Database Connection Pools
- Redis Cache Statistics

---

## 🧪 Testing & Validation

### Complete System Test
```bash
# Run comprehensive system test
./scripts/tests/test-system.sh
```

### Admin Authentication Test
```bash
# Test admin authentication flow
curl -X POST http://localhost:8085/admin/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin@enterprise-rag.com", "password": "admin123"}' \
  | jq .

# Test document upload
JWT_TOKEN="your-token-here"
curl -X POST http://localhost:8080/api/documents/upload \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -F "file=@test-document.txt" \
  -F "description=Test upload"

# Test RAG query
curl -X POST http://localhost:8080/api/rag/query \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What is in the uploaded document?",
    "maxResults": 3
  }'
```

### Performance Testing
```bash
# Load test example (requires Apache Bench)
ab -n 100 -c 10 -H "Authorization: Bearer $JWT_TOKEN" \
  http://localhost:8080/api/rag/query

# Monitor response times
time curl -X GET http://localhost:8080/actuator/health
```

---

## 🔧 Troubleshooting

### Common Issues

#### 1. Service Not Responding
```bash
# Check if service is running
docker ps | grep rag-

# Check service logs
docker logs rag-gateway
docker logs rag-auth
docker logs rag-document

# Restart specific service
docker restart rag-gateway
```

#### 2. Authentication Failures
```bash
# Verify JWT token is valid
echo $JWT_TOKEN | cut -d'.' -f2 | base64 -d | jq .

# Check token expiration
curl -X GET http://localhost:8081/auth/validate \
  -H "Authorization: Bearer $JWT_TOKEN"

# Get new token
curl -X POST http://localhost:8081/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "your-refresh-token"}'
```

#### 3. Database Connection Issues
```bash
# Test PostgreSQL connection
docker exec -it rag-postgres pg_isready -U rag_user -d rag_enterprise

# Check Redis connection
docker exec -it rag-redis redis-cli ping

# View database logs
docker logs rag-postgres
docker logs rag-redis
```

#### 4. File Upload Problems
```bash
# Check disk space
df -h

# Verify file permissions
ls -la /path/to/upload/directory

# Check service logs for upload errors
docker logs rag-document | grep -i error
```

### Performance Optimization

#### 1. Database Performance
```sql
-- Check slow queries
SELECT query, mean_time, calls 
FROM pg_stat_statements 
ORDER BY mean_time DESC 
LIMIT 10;

-- Index usage
SELECT schemaname, tablename, attname, n_distinct, correlation
FROM pg_stats
WHERE schemaname = 'public';
```

#### 2. Redis Cache Optimization
```bash
# Check cache hit ratio
redis-cli info stats | grep cache

# Monitor memory usage
redis-cli info memory
```

### Service URLs Quick Reference
```bash
# Health checks
curl http://localhost:8080/actuator/health  # Gateway
curl http://localhost:8081/actuator/health  # Auth
curl http://localhost:8082/actuator/health  # Document
curl http://localhost:8083/actuator/health  # Embedding
curl http://localhost:8084/actuator/health  # Core
curl http://localhost:8085/admin/api/actuator/health  # Admin

# API Documentation
open http://localhost:8080/swagger-ui.html  # Gateway
open http://localhost:8081/swagger-ui.html  # Auth
open http://localhost:8082/swagger-ui.html  # Document
open http://localhost:8083/swagger-ui.html  # Embedding
open http://localhost:8084/swagger-ui.html  # Core
open http://localhost:8085/admin/api/swagger-ui.html  # Admin
```

---

## 📚 Additional Resources

- **System Architecture**: See `README.md` for detailed architecture overview
- **Deployment Guide**: See `docs/deployment/DEPLOYMENT.md`
- **Docker Setup**: See `docs/deployment/DOCKER.md`
- **API Documentation**: Available at each service's `/swagger-ui.html` endpoint
- **Monitoring**: Grafana dashboards at http://localhost:3000

---

**🎯 Need Help?**
- **📚 API Documentation**: Visit http://localhost:8080/swagger-ui.html for interactive API docs
- **🔍 Service Logs**: `docker logs [service-name]`
- **❤️ Health Checks**: `/actuator/health` endpoints
- **👤 Admin Setup**: `./scripts/db/create-admin-user.sh`
- **🧪 System Tests**: `./scripts/tests/test-system.sh`

**⚠️ Important Notes:**
- **API Documentation**: All services now have comprehensive Swagger UI documentation
- **Interactive Testing**: Use "Try It Out" in Swagger UI for live API testing
- **Authentication**: Admin service uses `username` field (not `email`) for login
- **Default Credentials**: Admin password is `admin123` (change after first login)
- **Separation**: User services are separate from admin authentication
- **HTTP Methods**: Health checks use GET requests, not POST

**🆕 What's New (DOC-002):**
- **100% API Coverage**: All endpoints now documented with OpenAPI 3.0
- **Interactive Docs**: Swagger UI available at all service endpoints
- **JWT Integration**: Built-in authentication testing in documentation
- **Developer Experience**: "Try It Out" functionality for all endpoints

This guide provides everything you need to connect to and use all services in the RAG system effectively!
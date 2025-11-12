---
version: 1.0.0
last-updated: 2025-11-12
status: active
applies-to: 0.8.0-SNAPSHOT
category: deployment
---

# Swagger UI Access Guide - RAG System

**Last Updated**: 2025-09-29  
**System Status**: All services operational with interactive API documentation

---

## 🎯 Overview

This guide provides the exact credentials and access information for all Swagger UI interfaces in the BYO RAG System. Each service provides comprehensive OpenAPI 3.0 specifications with interactive "Try It Out" functionality.

## 📋 Quick Access Summary

### ✅ **Publicly Accessible (No Authentication Required)**

| Service | URL | Status | Notes |
|---------|-----|--------|-------|
| **Document Service** | http://localhost:8082/swagger-ui.html | ✅ Public | Complete document processing API |

### 🔑 **Authentication Required Services**

| Service | URL | Username | Password | Status |
|---------|-----|----------|----------|--------|
| **API Gateway** | http://localhost:8080/swagger-ui.html | `user` | `726bcacd-081f-4a08-96e1-9037edc2ac45` | ✅ Working |
| **Embedding Service** | http://localhost:8083/swagger-ui.html | `user` | `681650f3-b562-4c16-828a-d8a996b01217` | ✅ Working |
| **Core Service** | http://localhost:8084/swagger-ui.html | `user` | `77147b40-70e6-477d-8557-fcf417e9ca9f` | ✅ Working |
| **Admin Service** | http://localhost:8085/admin/api/swagger-ui.html | `user` | `5080a46c-bfef-45fc-a403-2ea299ee531d` | ✅ Working |

### ⚠️ **Special Cases**

| Service | URL | Status | Issue |
|---------|-----|--------|-------|
| **Auth Service** | http://localhost:8081/swagger-ui.html | ❌ 403 Forbidden | No generated password, requires different auth |

---

## 🚀 Getting Started

### 1. **Start with Document Service (Recommended)**
- **URL**: http://localhost:8082/swagger-ui.html
- **Why**: No authentication required, full API available
- **Features**: Document upload, processing, chunking, metadata extraction

### 2. **Explore Core RAG Functionality**
- **URL**: http://localhost:8084/swagger-ui.html
- **Login**: user / `77147b40-70e6-477d-8557-fcf417e9ca9f`
- **Features**: RAG queries, conversation management, LLM integration

### 3. **Test Vector Operations**
- **URL**: http://localhost:8083/swagger-ui.html
- **Login**: user / `681650f3-b562-4c16-828a-d8a996b01217`
- **Features**: Embedding generation, vector search, similarity operations

### 4. **System Administration**
- **URL**: http://localhost:8085/admin/api/swagger-ui.html
- **Login**: user / `5080a46c-bfef-45fc-a403-2ea299ee531d`
- **Features**: Tenant management, user administration, system analytics

---

## 🔐 Authentication Details

### **Spring Security Generated Passwords**
Most services use Spring Security's default configuration with auto-generated passwords. These passwords are:
- **Generated at startup** and logged to service logs
- **Username is always `user`** for Spring Security default
- **Valid for the lifetime** of the service container
- **⚠️ IMPORTANT: Passwords change every time services restart**
- **Unique per service** and unpredictable

### **⚠️ Critical Note: Password Expiration**
**The passwords documented in this guide will become invalid when services restart!**

**When services restart, you must:**
1. Retrieve new passwords from service logs
2. Update your access credentials
3. Re-authenticate in Swagger UI interfaces

### **Database-Backed Authentication**
For API testing through services (not Swagger UI login), use:
- **Username**: `admin@enterprise-rag.com`
- **Password**: `admin123`
- **Use in**: JWT authentication, API testing, service-to-service calls

---

## 📚 API Documentation Features

Each Swagger UI provides:

### **🌟 Interactive Testing**
- **Try It Out**: Execute API calls directly from documentation
- **Request Builder**: Automatic request formatting and validation
- **Response Viewer**: Real-time response display with syntax highlighting

### **🔐 Authentication Integration**
- **Authorize Button**: Built-in JWT token management
- **Token Testing**: Test authentication flows interactively
- **Security Schemes**: Complete security documentation

### **📊 Complete API Specifications**
- **Request/Response Schemas**: Full data model documentation
- **Example Payloads**: Working examples for all endpoints
- **Error Responses**: Complete error handling documentation
- **Parameter Validation**: Input validation rules and constraints

---

## 🛠️ Troubleshooting

### **Swagger UI Won't Load**
```bash
# Check service health
curl http://localhost:8082/actuator/health

# Check if Swagger UI endpoint exists
curl -I http://localhost:8082/swagger-ui.html
```

### **Authentication Failures**
```bash
# Get current generated password from logs
docker logs rag-embedding 2>&1 | grep "Using generated security password"

# Verify service is responding
curl -u user:PASSWORD http://localhost:8083/swagger-ui.html
```

### **Service Unavailable**
```bash
# Check Docker container status
docker ps | grep rag-

# Restart specific service if needed
docker restart rag-embedding
```

---

## 🔄 Password Management

### **Getting Current Passwords**

**🚀 Quick Method (Recommended):**
```bash
# Get all current passwords at once
./scripts/utils/get-swagger-passwords.sh
```

**Manual Method (If needed):**
```bash
# API Gateway
docker logs rag-gateway 2>&1 | grep "Using generated security password"

# Embedding Service  
docker logs rag-embedding 2>&1 | grep "Using generated security password"

# Core Service
docker logs rag-core 2>&1 | grep "Using generated security password"

# Admin Service
docker logs rag-admin 2>&1 | grep "Using generated security password"
```

### **Password Rotation**
Passwords change when services restart:
```bash
# Restart service (generates new password)
docker restart rag-embedding

# Get ALL new passwords after restart
./scripts/utils/get-swagger-passwords.sh
```

---

## 🎯 Service-Specific Features

### **Document Service (Port 8082)**
- **File Upload Testing**: Upload documents directly through Swagger UI
- **Processing Pipeline**: Test chunking and text extraction
- **Format Support**: Test PDF, DOCX, TXT, MD, HTML processing

### **Embedding Service (Port 8083)**
- **Vector Generation**: Test embedding creation for text chunks
- **Similarity Search**: Test vector similarity operations
- **Batch Processing**: Test bulk embedding operations

### **Core Service (Port 8084)**
- **RAG Queries**: Test complete question-answering pipeline
- **Conversation Management**: Test chat conversation flows
- **LLM Integration**: Test integration with language models

### **Admin Service (Port 8085)**
- **Tenant Management**: Test multi-tenant operations
- **User Administration**: Test user lifecycle management
- **System Analytics**: Test reporting and monitoring endpoints

---

## 📋 API Gateway Note

**Important**: The API Gateway (port 8080) Swagger UI shows "no operations defined" because it's a **routing gateway**, not a service with REST endpoints. The gateway routes requests to backend services but doesn't define its own API operations.

**For API testing through the gateway**, use the route prefixes:
- `/api/auth/**` → Auth Service
- `/api/documents/**` → Document Service  
- `/api/embeddings/**` → Embedding Service
- `/api/rag/**` → Core Service
- `/api/admin/**` → Admin Service

---

## ✅ **Success Checklist**

- [ ] Document Service Swagger UI accessible without login
- [ ] Can login to Embedding Service with generated password
- [ ] Can login to Core Service with generated password  
- [ ] Can login to Admin Service with generated password
- [ ] "Try It Out" functionality works for API testing
- [ ] JWT authentication can be tested through Swagger UI
- [ ] All service APIs documented and interactive

---

**🎯 Ready to Explore**: All Swagger UI interfaces are now accessible with the provided credentials. Start with the Document Service for immediate testing, then explore the authenticated services for complete RAG system functionality!
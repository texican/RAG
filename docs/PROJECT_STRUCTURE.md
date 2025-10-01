# BYO RAG System - Project Structure

**Last Updated**: 2025-09-30
**Status**: Production-Ready Microservices Architecture

---

## 📁 **Root Directory Structure**

```
RAG/
├── docs/                           # Project documentation
│   ├── api/                        # API specifications & OpenAPI docs
│   ├── deployment/                 # Deployment guides & configurations
│   ├── development/                # Development guidelines & best practices
│   ├── project-management/         # Sprint plans, backlogs, completed work
│   ├── testing/                    # Test documentation & results
│   └── README.md                   # Documentation index
│
├── scripts/                        # Automation & utility scripts
│   ├── backlog/                    # Backlog management scripts
│   ├── db/                         # Database scripts
│   ├── deploy/                     # Deployment scripts
│   ├── dev/                        # Development helper scripts
│   ├── maintenance/                # Maintenance & cleanup scripts
│   ├── monitoring/                 # Monitoring & health check scripts
│   ├── quality/                    # Quality validation scripts
│   ├── setup/                      # Setup & initialization scripts
│   ├── tests/                      # Test execution scripts
│   └── utils/                      # Utility functions
│
├── docker/                         # Docker configurations
│   ├── grafana/                    # Grafana monitoring dashboards
│   ├── postgres/                   # PostgreSQL initialization scripts
│   └── prometheus/                 # Prometheus monitoring configuration
│
├── config/                         # Shared configuration files
│   ├── docker/                     # Docker-specific configurations
│   └── application-local.yml       # Local development configuration
│
├── postman/                        # Postman API collections
│   ├── BYO_RAG_Admin_Service.postman_collection.json
│   ├── BYO_RAG_Auth_Service.postman_collection.json
│   ├── BYO_RAG_Complete_Workflows.postman_collection.json
│   ├── BYO_RAG_Core_Service.postman_collection.json
│   ├── BYO_RAG_Document_Service.postman_collection.json
│   ├── BYO_RAG_Embedding_Service.postman_collection.json
│   └── ~~BYO_RAG_Gateway.postman_collection.json~~ (archived)
│
├── specs/                          # System specifications
│   ├── 001-rag-core-service/       # Core RAG service spec
│   ├── 002-rag-shared/             # Shared components spec
│   ├── ~~003-rag-gateway/~~        # ~~API Gateway spec~~ (archived)
│   ├── 004-rag-admin-service/      # Admin service spec
│   ├── 005-rag-auth-service/       # Auth service spec
│   ├── 006-rag-document-service/   # Document service spec
│   ├── 007-rag-embedding-service/  # Embedding service spec
│   ├── DOCUMENTATION_IMPROVEMENT_SPECIFICATION.md
│   └── RAG_SYSTEM_SPECIFICATION.md
│
├── logs/                           # Centralized application logs
│   └── .gitkeep                    # Keep directory in git
│
├── backups/                        # System backups (gitignored)
│
├── ollama-chat/                    # Ollama chat interface
│   ├── index.html                  # Web UI
│   ├── server.py                   # Backend server
│   └── start-chat.sh               # Startup script
│
├── docs-site/                      # Documentation website
│   └── package.json                # Documentation build config
│
├── .specify/                       # Specify AI configuration
│
├── .claude/                        # Claude Code configuration
│   ├── commands/                   # Custom Claude commands
│   └── settings.local.json         # Local settings
│
├── .env                            # Environment variables (gitignored)
├── .gitignore                      # Git ignore rules
├── .quality-checklist              # Quality assurance checklist
├── pom.xml                         # Maven parent POM
├── README.md                       # Project overview
├── archive/                        # Archived code (gateway)
│   └── rag-gateway/                # Archived API Gateway (see ADR-001)
├── BACKLOG.md                      # Product backlog
├── QUALITY_STANDARDS.md            # Quality standards & guidelines
└── README.md                       # Main project documentation

```

---

## 🏗️ **Microservices Architecture**

### **1. rag-shared** (Shared Components Library)
```
rag-shared/
├── src/
│   ├── main/java/com/byo/rag/shared/
│   │   ├── config/              # Shared configurations
│   │   ├── dto/                 # Data Transfer Objects
│   │   ├── entity/              # JPA entities
│   │   ├── exception/           # Custom exceptions
│   │   ├── repository/          # Repository interfaces
│   │   ├── security/            # Security utilities
│   │   └── util/                # Utility classes
│   └── test/java/               # Unit tests (90 tests)
├── pom.xml
└── README.md
```
**Purpose**: Shared DTOs, entities, utilities, and security components
**Dependencies**: None (base library)
**Test Status**: ⚠️ 88/90 passing (98%)

---

### **2. rag-auth-service** (Authentication & Authorization)
```
rag-auth-service/
├── src/
│   ├── main/
│   │   ├── java/com/byo/rag/auth/
│   │   │   ├── config/          # Security & JWT configuration
│   │   │   ├── controller/      # REST endpoints
│   │   │   ├── dto/             # Auth-specific DTOs
│   │   │   ├── entity/          # User & role entities
│   │   │   ├── repository/      # User repositories
│   │   │   ├── security/        # JWT & authentication
│   │   │   └── service/         # Auth business logic
│   │   └── resources/
│   │       └── application.yml  # ⚠️ YAML error - needs fix
│   └── test/java/               # Unit & integration tests (71 tests)
├── logs/                        # Service logs
│   └── .gitkeep
├── Dockerfile
├── pom.xml
└── README.md
```
**Port**: 8081
**Database**: PostgreSQL (rag_auth)
**Key Features**: JWT tokens, user management, role-based access
**Test Status**: ❌ 46/71 passing (65%) - CRITICAL YAML CONFIG ERROR

---

### **3. rag-document-service** (Document Management)
```
rag-document-service/
├── src/
│   ├── main/
│   │   ├── java/com/byo/rag/document/
│   │   │   ├── config/          # S3, Kafka, security config
│   │   │   ├── controller/      # Document REST endpoints
│   │   │   ├── dto/             # Document DTOs
│   │   │   ├── entity/          # Document entities
│   │   │   ├── repository/      # Document repositories
│   │   │   ├── security/        # Document security
│   │   │   └── service/         # Document processing
│   │   └── resources/
│   │       ├── application.yml
│   │       └── application-test.properties
│   └── test/java/               # Unit & integration tests (115 tests)
├── test-storage/                # Test file storage
│   └── .gitkeep
├── logs/
│   └── .gitkeep
├── Dockerfile
├── pom.xml
└── README.md
```
**Port**: 8082
**Database**: PostgreSQL (rag_document)
**Storage**: S3/MinIO
**Key Features**: File upload, text extraction (Tika), chunking, metadata
**Test Status**: ✅ 115/115 passing (100%)

---

### **4. rag-embedding-service** (Vector Embeddings)
```
rag-embedding-service/
├── src/
│   ├── main/
│   │   ├── java/com/byo/rag/embedding/
│   │   │   ├── config/          # Vector DB & Kafka config
│   │   │   ├── controller/      # Embedding REST endpoints
│   │   │   ├── dto/             # Embedding DTOs
│   │   │   ├── entity/          # Vector entities
│   │   │   ├── repository/      # Vector repositories
│   │   │   └── service/         # Embedding generation
│   │   └── resources/
│   │       ├── application.yml
│   │       └── application-test.properties
│   └── test/java/               # Unit & integration tests (181 tests)
│       ├── advanced/            # Advanced test scenarios
│       ├── config/              # Test configurations
│       ├── entity/              # Entity tests
│       ├── repository/          # Repository tests
│       └── service/             # Service tests
├── logs/
│   └── .gitkeep
├── Dockerfile
├── pom.xml
└── README.md
```
**Port**: 8083
**Database**: PostgreSQL with pgvector
**Key Features**: Embedding generation, vector storage, similarity search
**Test Status**: ⚠️ 173/181 passing (96%) - Integration test failures

---

### **5. rag-core-service** (RAG Orchestration)
```
rag-core-service/
├── src/
│   ├── main/
│   │   ├── java/com/byo/rag/core/
│   │   │   ├── config/          # LLM & caching config
│   │   │   ├── controller/      # RAG REST endpoints
│   │   │   ├── dto/             # Query/response DTOs
│   │   │   ├── entity/          # Conversation entities
│   │   │   ├── repository/      # Conversation repositories
│   │   │   └── service/         # RAG orchestration
│   │   │       ├── cache/       # Query caching
│   │   │       ├── context/     # Context assembly
│   │   │       ├── llm/         # LLM integration
│   │   │       └── search/      # Vector search
│   │   └── resources/
│   │       ├── application.yml
│   │       └── application-test.yml
│   └── test/java/               # Unit tests (108 tests)
├── logs/
│   └── .gitkeep
├── Dockerfile
├── pom.xml
└── README.md
```
**Port**: 8084
**Database**: PostgreSQL (rag_core), Redis (cache)
**Key Features**: Query optimization, context assembly, LLM integration
**Test Status**: ✅ 108/108 passing (100%)

---

### **6. rag-admin-service** (System Administration)
```
rag-admin-service/
├── src/
│   ├── main/
│   │   ├── java/com/byo/rag/admin/
│   │   │   ├── config/          # Admin security config
│   │   │   ├── controller/      # Admin REST endpoints
│   │   │   ├── dto/             # Admin DTOs
│   │   │   ├── entity/          # Tenant & user entities
│   │   │   ├── repository/      # Admin repositories
│   │   │   ├── security/        # Admin JWT & auth
│   │   │   └── service/         # Admin business logic
│   │   └── resources/
│   │       └── application.yml
│   └── test/java/               # Unit & integration tests (77 tests)
├── logs/
│   └── .gitkeep
├── Dockerfile
├── pom.xml
└── README.md
```
**Port**: 8086
**Database**: PostgreSQL (rag_admin)
**Key Features**: Tenant management, user admin, system config, audit logging
**Test Status**: ✅ 77/77 passing (100%)

---

### **7. ~~rag-gateway~~ (API Gateway)** 📦 **ARCHIVED**

**Status**: ✅ **ARCHIVED** per [ADR-001: Bypass API Gateway](development/ADR-001-BYPASS-API-GATEWAY.md)

**Archive Reason**: Gateway bypassed in favor of direct service access due to:
- 83% test failure rate (125/151 tests failing)
- Persistent CSRF and ApplicationContext issues
- Limited value add (services work perfectly with direct access)

**Location**: Moved to `archive/rag-gateway/`

**Alternative**: Use direct service access:
- Auth Service: http://localhost:8081
- Document Service: http://localhost:8082
- Embedding Service: http://localhost:8083
- Core Service: http://localhost:8084
- Admin Service: http://localhost:8086

---

### **8. rag-integration-tests** (End-to-End Testing)
```
rag-integration-tests/
├── src/
│   └── test/java/com/byo/rag/integration/
│       ├── base/                # Base test classes
│       ├── config/              # TestContainers config
│       ├── data/                # Test data builders
│       ├── smoke/               # Smoke tests
│       ├── standalone/          # Standalone integration tests
│       └── utils/               # Test utilities
├── pom.xml
└── README.md
```
**Purpose**: Cross-service integration testing
**Test Status**: ⏭️ SKIPPED (test compilation disabled)

---

## 🗄️ **Database Schema**

Each service has its own PostgreSQL database:

- **rag_auth**: Users, roles, permissions
- **rag_document**: Documents, chunks, metadata
- **rag_embedding**: Vector embeddings (with pgvector extension)
- **rag_core**: Conversations, query history
- **rag_admin**: Tenants, system configuration, audit logs

---

## 📊 **Key Directories**

### **Documentation** (`docs/`)
- **api/**: OpenAPI specifications, API documentation
- **deployment/**: Deployment guides, infrastructure docs
- **development/**: Coding standards, contribution guidelines
- **project-management/**: Backlogs, sprint plans, completed stories
- **testing/**: Test plans, test results, quality reports

### **Scripts** (`scripts/`)
- **backlog/**: Backlog validation and management
- **db/**: Database initialization, migrations, admin user creation
- **deploy/**: Docker compose, service deployment
- **dev/**: Development server startup, hot reload
- **maintenance/**: Cleanup, backup, restore operations
- **monitoring/**: Health checks, metrics collection
- **quality/**: Code quality validation, test verification
- **setup/**: Initial system setup, environment configuration
- **tests/**: Test execution, test validation

### **Configuration** (`config/`)
- Docker-specific configurations
- Local development overrides
- Shared application properties

---

## 🔧 **Technology Stack**

### **Backend**
- **Framework**: Spring Boot 3.2.8
- **Language**: Java 21
- **Build Tool**: Maven 3.9+
- **API Gateway**: ~~Spring Cloud Gateway~~ (archived - see ADR-001)
- **Security**: JWT, Spring Security

### **Databases**
- **Primary**: PostgreSQL 15+ (with pgvector extension)
- **Cache**: Redis 7+
- **Vector Store**: PostgreSQL with pgvector

### **Message Queue**
- **Kafka**: Event streaming for document processing

### **AI/ML**
- **Embedding**: Langchain4j
- **LLM Integration**: OpenAI, Ollama, Custom models
- **Document Processing**: Apache Tika

### **Infrastructure**
- **Containerization**: Docker
- **Orchestration**: Docker Compose
- **Monitoring**: Prometheus + Grafana
- **Storage**: S3/MinIO

### **Testing**
- **Unit Testing**: JUnit 5, Mockito
- **Integration Testing**: Spring Boot Test, TestContainers
- **API Testing**: Postman collections
- **Test Coverage**: JaCoCo (currently disabled)

---

## 📈 **Project Metrics**

### **Codebase Statistics**
- **Total Services**: 8 microservices
- **Lines of Code**: ~50,000+ (estimated)
- **Test Coverage**: 78% pass rate (561/722 tests)
- **Documentation**: 100+ markdown files

### **Service Health**
| Service | Port | Tests | Status |
|---------|------|-------|--------|
| rag-auth-service | 8081 | 46/71 | ❌ Critical |
| rag-document-service | 8082 | 115/115 | ✅ Excellent |
| rag-embedding-service | 8083 | 173/181 | ⚠️ Good |
| rag-core-service | 8084 | 108/108 | ✅ Excellent |
| rag-admin-service | 8086 | 77/77 | ✅ Excellent |
| ~~rag-gateway~~ | ~~8080~~ | ~~26/151~~ | 📦 Archived (bypassed) |
| rag-shared | N/A | 88/90 | ⚠️ Good |
| rag-integration-tests | N/A | 0/0 | ⏭️ Skipped |

---

## 🚀 **Getting Started**

### **Prerequisites**
- Java 21+
- Maven 3.9+
- Docker & Docker Compose
- PostgreSQL 15+
- Redis 7+

### **Quick Start**
```bash
# 1. Clone the repository
git clone <repository-url>
cd RAG

# 2. Start infrastructure
docker-compose up -d postgres redis kafka

# 3. Initialize databases
./scripts/db/create-admin-user.sh

# 4. Build all services
mvn clean install

# 5. Start all services
./scripts/dev/start-all-services.sh

# 6. Access the system
# Direct service access (no gateway - see ADR-001)
# Auth: http://localhost:8081
# Document: http://localhost:8082/swagger-ui.html
```

### **Development Workflow**
```bash
# Run tests
mvn test

# Run specific service tests
cd rag-document-service && mvn test

# Start single service in dev mode
./scripts/dev/run-service.sh rag-document-service

# Validate code quality
./scripts/quality/validate-system.sh
```

---

## 📝 **File Naming Conventions**

### **Configuration Files**
- `application.yml` - Main application configuration
- `application-{profile}.yml` - Profile-specific configuration
- `application-test.properties` - Test configuration
- `docker-compose.yml` - Docker orchestration

### **Documentation Files**
- `README.md` - Service/module overview
- `BACKLOG.md` - Product backlog
- `SPRINT_PLAN.md` - Sprint planning
- `*_SPECIFICATION.md` - Technical specifications
- `*_STANDARDS.md` - Standards and guidelines

### **Script Files**
- `*.sh` - Shell scripts (executable)
- `*.py` - Python scripts
- `*.sql` - SQL scripts

---

## 🔒 **Security Considerations**

### **Secrets Management**
- **Never commit**: `.env`, credentials, keys
- **Use**: Environment variables, secret managers
- **Gitignored**: `.env`, `*.key`, `credentials.*`

### **Access Control**
- JWT-based authentication
- Role-based authorization (RBAC)
- Tenant isolation (multi-tenancy)

---

## 📦 **Deployment Structure**

```
Production Deployment (Direct Service Access - ADR-001):
├── Load Balancer (AWS ALB / NGINX)
│   ├── Auth Service (rag-auth:8081)
│   ├── Document Service (rag-document:8082)
│   ├── Embedding Service (rag-embedding:8083)
│   ├── Core Service (rag-core:8084)
│   └── Admin Service (rag-admin:8086)
│
├── Infrastructure:
│   ├── PostgreSQL Cluster (Primary + Replicas)
│   ├── Redis Cluster (Cache)
│   ├── Kafka Cluster (Event Streaming)
│   ├── S3/MinIO (Document Storage)
│   └── Monitoring (Prometheus + Grafana)
```

> **Note**: Gateway bypassed per ADR-001 - load balancer routes directly to services

---

## 🔄 **CI/CD Pipeline** (Planned)

```
Code Push → Build → Test → Quality Gates → Deploy
```

**Quality Gates**:
- All tests must pass (100%)
- Code coverage > 80%
- No critical security vulnerabilities
- Documentation updated

---

## 📚 **Additional Resources**

- **API Documentation**: `docs/api/`
- **Deployment Guide**: `docs/deployment/`
- **Development Guide**: `docs/development/`
- **Test Results**: `docs/testing/TEST_RESULTS_SUMMARY.md`
- **Sprint Planning**: `SPRINT_PLAN.md`
- **Quality Standards**: `QUALITY_STANDARDS.md`

---

**Last Review**: 2025-09-30
**Maintained By**: Development Team
**Status**: Active Development

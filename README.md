# Enterprise RAG System

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.8-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.0--M1-blue.svg)](https://spring.io/projects/spring-ai)
[![Maven](https://img.shields.io/badge/Maven-3.8+-red.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

An enterprise-grade Enterprise RAG (Retrieval Augmented Generation) system built with Spring Boot 3.x, demonstrating advanced backend engineering and modern AI integration.

## 📋 Tech Stack & Dependencies

### Core Framework
| Component | Version | Purpose |
|-----------|---------|---------|
| **Java** | 21 (LTS) | Primary programming language |
| **Spring Boot** | 3.2.8 | Application framework |
| **Spring AI** | 1.0.0-M1 | AI/ML integration |
| **Spring Cloud** | 2023.0.2 | Microservices framework |
| **Maven** | 3.8+ | Build and dependency management |

### Database & Storage
| Component | Version | Purpose |
|-----------|---------|---------|
| **PostgreSQL** | 42.7.3 | Primary database with pgvector |
| **Redis Stack** | 5.0.2 | Vector storage and caching |
| **Apache Kafka** | 3.7.0 | Event streaming and messaging |

### AI/ML Libraries
| Component | Version | Purpose |
|-----------|---------|---------|
| **LangChain4j** | 0.33.0 | LLM integration framework |
| **Apache Tika** | 2.9.2 | Document processing and text extraction |
| **OpenAI API** | Latest | GPT models and embeddings |
| **Ollama** | Latest | Local LLM inference |

### Testing & Quality
| Component | Version | Purpose |
|-----------|---------|---------|
| **JUnit** | 5.10.2 | Unit testing framework |
| **Testcontainers** | 1.19.8 | Integration testing |
| **Mockito** | 5.14.2 | Mocking framework |
| **WireMock** | 3.8.0 | API mocking |

### Monitoring & Documentation
| Component | Version | Purpose |
|-----------|---------|---------|
| **Micrometer** | 1.12.7 | Application metrics |
| **SpringDoc OpenAPI** | 2.5.0 | API documentation |
| **Logstash Logback** | 7.4 | Structured logging |

## 🏗️ Architecture Overview

This system implements a sophisticated microservices architecture with complete multi-tenant isolation:

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   API Gateway   │────│  Authentication  │────│  Document Proc  │
│   (Port 8080)   │    │   Service (8081) │    │  Service (8083) │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                        │                        │
         └────────────────────────┼────────────────────────┘
                                  │
         ┌────────────────────────┼────────────────────────┐
         │                        │                        │
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   RAG Core      │────│   Embedding      │────│   Admin         │
│  Service (8082) │    │  Service (8084)  │    │ Service (8085)  │
└─────────────────┘    └──────────────────┘    └─────────────────┘

Infrastructure Services:
├── PostgreSQL (5432) - Primary database with pgvector
├── Redis Stack (6379) - Vector storage and caching  
├── Apache Kafka (9092) - Event streaming
├── Ollama (11434) - Local LLM inference
├── Prometheus (9090) - Metrics collection
└── Grafana (3000) - Monitoring dashboards
```

## 🚀 Key Features

### Multi-Tenant Architecture
- **Complete data isolation** between tenants
- **Tenant-scoped authentication** with JWT tokens
- **Per-tenant resource limits** and configurations
- **Horizontal scalability** to 1000+ concurrent users

### Document Processing Pipeline
- **5+ file formats** supported (PDF, DOCX, TXT, MD, HTML)
- **Intelligent text extraction** using Apache Tika
- **Configurable chunking strategies** (semantic, fixed-size, sliding window)
- **Async processing** with Kafka event streaming

### Vector Operations & Search
- **Multiple embedding models** (OpenAI, local models)
- **Redis vector storage** with tenant isolation
- **Hybrid search** combining semantic and keyword search
- **Sub-200ms query response** times (excluding LLM generation)

### RAG Query Engine
- **Context assembly** and optimization
- **Multiple LLM providers** (OpenAI + Ollama)
- **Response streaming** with Server-Sent Events
- **Intelligent caching** and query optimization

## 🛠️ Technology Stack

| Component | Technology | Version |
|-----------|------------|---------|
| **Backend Framework** | Spring Boot | 3.2+ |
| **Language** | Java | 21 (LTS) |
| **AI/ML Integration** | Spring AI, LangChain4j | Latest |
| **Vector Database** | Redis Stack + RediSearch | Latest |
| **Primary Database** | PostgreSQL + pgvector | 15+ |
| **Message Queue** | Apache Kafka | 7.4+ |
| **Caching** | Redis | Latest |
| **Monitoring** | Prometheus + Grafana | Latest |
| **Local LLM** | Ollama | Latest |

## 📋 Prerequisites

- **Java 21+** (OpenJDK recommended)
- **Maven 3.8+**
- **Docker & Docker Compose**
- **VSCode** with Java Extension Pack (recommended)

## 🚀 Quick Start

### 1. Clone and Setup
```bash
git clone <your-repo>
cd enterprise-rag
```

### 2. Start Infrastructure Services
```bash
# Start all infrastructure services
docker-compose up -d

# Verify services are running
docker-compose ps

# Check service health
docker-compose logs postgres redis kafka
```

### 3. Initialize Ollama (Optional - for local LLM)
```bash
# Pull a model for local inference
docker exec -it rag-ollama ollama pull llama2:7b
docker exec -it rag-ollama ollama pull nomic-embed-text
```

### 4. Build and Run Services

#### Option A: Run All Services
```bash
# Build all modules
mvn clean install

# Run services in separate terminals
cd rag-auth-service && mvn spring-boot:run
cd rag-document-service && mvn spring-boot:run  
cd rag-embedding-service && mvn spring-boot:run
cd rag-core-service && mvn spring-boot:run
cd rag-gateway && mvn spring-boot:run
cd rag-admin-service && mvn spring-boot:run
```

#### Option B: Run Individual Services
```bash
# Start with authentication service
cd rag-auth-service
mvn spring-boot:run

# In another terminal, start document service
cd rag-document-service  
mvn spring-boot:run
```

### 5. Verify Installation

Visit these URLs to confirm services are running:

- **API Gateway**: http://localhost:8080/actuator/health
- **Auth Service**: http://localhost:8081/swagger-ui.html
- **Document Service**: http://localhost:8083/swagger-ui.html
- **Monitoring Dashboard**: http://localhost:3000 (admin/admin)
- **Kafka UI**: http://localhost:8080
- **Redis Insight**: http://localhost:8001

## 🧪 Testing the System

### 1. Register a Tenant
```bash
curl -X POST http://localhost:8081/api/v1/tenants/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Company",
    "slug": "test-company",
    "description": "Test tenant for development"
  }'
```

### 2. Create Admin User
```bash
curl -X POST http://localhost:8081/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Admin",
    "lastName": "User", 
    "email": "admin@testcompany.com",
    "password": "SecurePass123!",
    "role": "ADMIN",
    "tenantId": "<tenant-id-from-step-1>"
  }'
```

### 3. Login and Get JWT Token
```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@testcompany.com",
    "password": "SecurePass123!"
  }'
```

### 4. Upload a Document
```bash
curl -X POST http://localhost:8083/api/v1/documents/upload \
  -H "Authorization: Bearer <jwt-token>" \
  -F "file=@sample-document.pdf"
```

### 5. Query the RAG System
```bash
curl -X POST http://localhost:8082/api/v1/rag/query \
  -H "Authorization: Bearer <jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What is the main topic of the uploaded document?",
    "maxResults": 5
  }'
```

## 📊 Service Ports

| Service | Port | Description |
|---------|------|-------------|
| **rag-gateway** | 8080 | API Gateway (main entry point) |
| **rag-auth-service** | 8081 | Authentication & tenant management |
| **rag-core-service** | 8082 | RAG query engine |
| **rag-document-service** | 8083 | Document processing |
| **rag-embedding-service** | 8084 | Vector operations |
| **rag-admin-service** | 8085 | Admin operations & analytics |

## 🏭 Enterprise Deployment

### Environment Variables
```bash
# Database
export DB_USERNAME=rag_user
export DB_PASSWORD=secure_password
export REDIS_PASSWORD=secure_redis_password

# JWT Security  
export JWT_SECRET=your-256-bit-secret-key

# AI/ML Services
export OPENAI_API_KEY=your-openai-key
export OLLAMA_HOST=http://ollama:11434

# Kafka
export KAFKA_BOOTSTRAP_SERVERS=kafka:29092
```

### Docker Enterprise Build
```bash
# Build enterprise images
docker build -t rag-auth-service:latest rag-auth-service/
docker build -t rag-document-service:latest rag-document-service/
# ... repeat for other services

# Deploy with enterprise docker-compose
docker-compose -f docker-compose.enterprise.yml up -d
```

### Kubernetes Deployment
```bash
# Apply Kubernetes manifests
kubectl apply -f k8s/namespace.yml
kubectl apply -f k8s/configmaps/
kubectl apply -f k8s/deployments/
kubectl apply -f k8s/services/
kubectl apply -f k8s/ingress/
```

## 📈 Performance Metrics

### Target Performance
- **Query Response Time**: <200ms (excluding LLM generation)
- **Concurrent Users**: 1000+
- **Document Processing**: 100+ docs/hour per instance
- **Retrieval Accuracy**: >80% relevant chunks in top-5
- **System Uptime**: 99.9%

### Monitoring
- **Prometheus metrics** at `/actuator/prometheus`
- **Grafana dashboards** at http://localhost:3000
- **Application logs** with structured JSON format
- **Health checks** at `/actuator/health`

## 🧪 Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify -P integration-tests
```

### Load Testing
```bash
# Use k6 or JMeter for load testing
k6 run tests/load/rag-system-load-test.js
```

## 🔒 Security Features

- **JWT-based authentication** with tenant-scoped tokens
- **Role-based access control** (ADMIN, USER, READER)
- **Multi-tenant data isolation** at database level
- **API rate limiting** per tenant
- **Secure file storage** with tenant separation
- **Input validation** and sanitization

## 📚 API Documentation

- **OpenAPI/Swagger UI** available at each service's `/swagger-ui.html`
- **Authentication**: http://localhost:8081/swagger-ui.html
- **Documents**: http://localhost:8083/swagger-ui.html
- **RAG Queries**: http://localhost:8082/swagger-ui.html

## 🛠️ Development

### Code Style
- Follow **Spring Boot 3.x best practices**
- Use **Java 21 features** (records, pattern matching)
- **Comprehensive error handling** with custom exceptions
- **Bean Validation** for input validation
- **Structured logging** with correlation IDs

### Testing Strategy
- **Unit tests** with JUnit 5 and Mockito
- **Integration tests** with Testcontainers
- **Contract testing** with Spring Cloud Contract
- **Performance testing** with k6

### Contributing
1. Fork the repository
2. Create feature branch: `git checkout -b feature/amazing-feature`
3. Commit changes: `git commit -m 'Add amazing feature'`
4. Push to branch: `git push origin feature/amazing-feature`
5. Open Pull Request

## 🎯 Portfolio Highlights

This project demonstrates:

**Backend Engineering Excellence**:
- Complex microservices architecture with proper separation of concerns
- Multi-tenant system design with complete data isolation
- Event-driven processing with Kafka at scale
- Enterprise-grade monitoring and observability

**AI/ML Engineering Skills**:
- RAG implementation with real-world complexity
- Vector database operations and optimization  
- Multiple LLM provider integration with intelligent fallback
- Embedding generation and similarity search at scale

**Modern Development Practices**:
- Spring Boot 3.x with latest Java 21 features
- Comprehensive testing with Testcontainers
- Docker containerization and Kubernetes deployment
- CI/CD with proper security scanning

## 🚨 Troubleshooting

### Common Issues

**Services won't start**:
```bash
# Check if ports are available
netstat -tulpn | grep :8081

# Check Docker services
docker-compose ps
docker-compose logs <service-name>
```

**Database connection issues**:
```bash
# Test PostgreSQL connection
docker exec -it rag-postgres psql -U rag_user -d rag_enterprise

# Check database logs
docker-compose logs postgres
```

**Kafka connectivity**:
```bash
# List Kafka topics
docker exec -it rag-kafka kafka-topics --bootstrap-server localhost:9092 --list

# Check consumer groups
docker exec -it rag-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --list
```

### Performance Tuning
- Increase JVM heap size: `-Xmx2g -Xms1g`
- Tune database connection pools
- Configure Redis memory policies
- Optimize Kafka producer/consumer settings

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

**Built with ❤️ using Spring Boot 3.x and modern Java practices**

For questions or support, please open an issue or contact the development team.
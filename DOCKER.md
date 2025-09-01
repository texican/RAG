# 🐳 Docker Setup - Enterprise RAG System

Complete Docker Compose configuration for the Enterprise RAG System with all 6 microservices, infrastructure components, and monitoring stack.

## 🚀 Quick Start

### Prerequisites
- **Docker Desktop** or **Colima** (recommended for macOS)
- **8GB+ RAM** (12GB+ recommended)  
- **20GB+ free disk space**

### For Colima Users (macOS)
```bash
# Start Colima with sufficient resources
colima start --memory 8 --cpu 4 --disk 60

# Verify Docker is running
docker info
```

### Start the System
```bash
# Make scripts executable (first time only)
chmod +x docker-start.sh docker-health.sh

# Start all services
./docker-start.sh

# Check system health
./docker-health.sh
```

## 📋 System Architecture

### 🏗️ Microservices (6 Services)
| Service | Port | Description |
|---------|------|-------------|
| **rag-gateway** | 8080 | API Gateway - Routes all external traffic |
| **rag-auth** | 8081 | Authentication - JWT auth, user/tenant management |
| **rag-document** | 8082 | Document Processing - File upload, text extraction, chunking |
| **rag-embedding** | 8083 | Embedding Service - Vector generation and similarity search |
| **rag-core** | 8084 | RAG Core - Query processing, LLM integration |
| **rag-admin** | 8085 | Admin Service - System administration, analytics |

### 🔧 Infrastructure Services
| Service | Port | Description |
|---------|------|-------------|
| **postgres** | 5432 | PostgreSQL with pgvector extension |
| **redis** | 6379 | Redis Stack for vector storage and caching |
| **kafka** | 9092 | Apache Kafka for event-driven messaging |
| **zookeeper** | 2181 | Kafka coordination service |
| **ollama** | 11434 | Local LLM inference engine |

### 📊 Monitoring & Management
| Service | Port | Description | Credentials |
|---------|------|-------------|-------------|
| **grafana** | 3000 | Monitoring dashboards | admin/admin |
| **prometheus** | 9090 | Metrics collection | - |
| **kafka-ui** | 8080 | Kafka management interface | - |
| **redis-insight** | 8001 | Redis management (dev only) | - |
| **pgadmin** | 5050 | PostgreSQL admin (dev only) | admin@enterprise-rag.com/admin |

## 🔧 Service Dependencies

```mermaid
graph TD
    A[rag-gateway] --> B[rag-auth]
    A --> C[rag-document] 
    A --> D[rag-embedding]
    A --> E[rag-core]
    A --> F[rag-admin]
    
    B --> G[postgres]
    B --> H[redis]
    
    C --> G
    C --> H
    C --> I[kafka]
    
    D --> H
    D --> I
    D --> J[ollama]
    
    E --> D
    E --> H
    E --> J
    
    F --> G
    
    I --> K[zookeeper]
```

## 🛠️ Configuration Files

- **`docker-compose.yml`** - Main service definitions
- **`docker-compose.override.yml`** - Development overrides (auto-loaded)
- **`.env`** - Environment variables
- **`docker/`** - Configuration files for infrastructure services
  - `postgres/init.sql` - Database initialization
  - `prometheus/prometheus.yml` - Metrics configuration
  - `grafana/` - Dashboard and datasource configurations

## 📝 Management Commands

### Basic Operations
```bash
# Start all services
docker-compose up -d

# Stop all services  
docker-compose down

# View service status
docker-compose ps

# View logs (all services)
docker-compose logs -f

# View logs (specific service)
docker-compose logs -f rag-gateway

# Restart a service
docker-compose restart rag-auth

# Rebuild and restart a service
docker-compose up -d --build rag-core
```

### Health Checks
```bash
# Run comprehensive health check
./docker-health.sh

# Check individual service health
curl http://localhost:8080/actuator/health  # Gateway
curl http://localhost:8081/actuator/health  # Auth
curl http://localhost:8082/actuator/health  # Document
curl http://localhost:8083/actuator/health  # Embedding
curl http://localhost:8084/actuator/health  # Core
curl http://localhost:8085/actuator/health  # Admin
```

### Database Operations
```bash
# Connect to PostgreSQL
docker-compose exec postgres psql -U rag_user -d rag_enterprise

# Connect to Redis CLI
docker-compose exec redis redis-cli -a redis_password

# View Kafka topics
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list
```

## 🔍 Troubleshooting

### Common Issues

#### 🐌 Slow Startup
- **Symptoms**: Services take >5 minutes to start
- **Solutions**: 
  - Increase Docker/Colima memory to 8GB+
  - Increase CPU allocation to 4+ cores
  - Ensure sufficient disk space (20GB+)

#### 🚫 Port Conflicts
- **Symptoms**: "Port already in use" errors
- **Solutions**:
  - Check for conflicting services: `lsof -i :8080`
  - Modify ports in `docker-compose.yml` if needed

#### 💾 Out of Memory
- **Symptoms**: Services randomly stopping, OOMKilled
- **Solutions**:
  - Increase Docker memory allocation
  - Reduce JVM heap sizes in Dockerfiles
  - Monitor with `docker stats`

#### 🔌 Service Connectivity
- **Symptoms**: Services can't reach each other
- **Solutions**:
  - Verify all services use `rag-network`
  - Check service names match Docker container names
  - Use `docker-compose exec SERVICE_NAME ping OTHER_SERVICE`

### Logs and Debugging
```bash
# View container resource usage
docker stats

# Check service startup order
docker-compose logs --timestamps | sort

# Debug networking issues
docker network ls
docker network inspect rag_rag-network

# View environment variables
docker-compose exec rag-gateway env
```

## 🔐 Security Considerations

### Development Environment
- Default passwords are used (change for production)
- All services run on localhost
- Debug logging enabled

### Production Recommendations
- Use external secret management
- Enable SSL/TLS for all connections
- Implement proper backup strategies
- Use production-grade monitoring
- Apply security updates regularly

## 📈 Performance Optimization

### Resource Allocation
```yaml
# Recommended Colima settings
colima start --memory 12 --cpu 6 --disk 100

# JVM optimization for containers
JAVA_OPTS: "-Xms512m -Xmx2048m -XX:+UseG1GC -XX:+UseContainerSupport"
```

### Monitoring
- **Grafana**: System metrics and application dashboards
- **Prometheus**: Metrics collection and alerting
- **Application Health**: Built-in Spring Boot Actuator endpoints

## 🚀 Next Steps

1. **Start Services**: Run `./docker-start.sh`
2. **Verify Health**: Run `./docker-health.sh` 
3. **Test API**: Access gateway at `http://localhost:8080`
4. **Monitor**: Open Grafana at `http://localhost:3000`
5. **Develop**: See `DEVELOPMENT.md` for development workflows

## 📞 Support

- Check service logs: `docker-compose logs -f [service-name]`
- Run health check: `./docker-health.sh`
- View this documentation: `cat DOCKER.md`
- Report issues: Create GitHub issue with logs and system info
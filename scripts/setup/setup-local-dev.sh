#!/bin/bash

# Enterprise RAG System - Local Development Setup Script
# This script automates the complete setup of the RAG system for local development
#
# Usage: ./scripts/setup/setup-local-dev.sh [--skip-docker] [--skip-build] [--verbose]
#
# Options:
#   --skip-docker    Skip Docker infrastructure setup
#   --skip-build     Skip Maven build process
#   --verbose        Enable verbose output

set -euo pipefail

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
LOG_DIR="${PROJECT_ROOT}/logs"
SETUP_LOG="${LOG_DIR}/setup.log"

# Default options
SKIP_DOCKER=false
SKIP_BUILD=false
VERBOSE=false

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging function
log() {
    local level=$1
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    # Create log directory if it doesn't exist
    mkdir -p "${LOG_DIR}"
    
    case $level in
        INFO)
            echo -e "${GREEN}[INFO]${NC} $message"
            echo "[$timestamp] [INFO] $message" >> "$SETUP_LOG"
            ;;
        WARN)
            echo -e "${YELLOW}[WARN]${NC} $message"
            echo "[$timestamp] [WARN] $message" >> "$SETUP_LOG"
            ;;
        ERROR)
            echo -e "${RED}[ERROR]${NC} $message"
            echo "[$timestamp] [ERROR] $message" >> "$SETUP_LOG"
            ;;
        DEBUG)
            if [[ "$VERBOSE" == "true" ]]; then
                echo -e "${BLUE}[DEBUG]${NC} $message"
            fi
            echo "[$timestamp] [DEBUG] $message" >> "$SETUP_LOG"
            ;;
    esac
}

# Error handler
error_exit() {
    log ERROR "$1"
    exit 1
}

# Parse command line arguments
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --skip-docker)
                SKIP_DOCKER=true
                shift
                ;;
            --skip-build)
                SKIP_BUILD=true
                shift
                ;;
            --verbose)
                VERBOSE=true
                shift
                ;;
            -h|--help)
                echo "Usage: $0 [--skip-docker] [--skip-build] [--verbose]"
                echo ""
                echo "Options:"
                echo "  --skip-docker    Skip Docker infrastructure setup"
                echo "  --skip-build     Skip Maven build process"
                echo "  --verbose        Enable verbose output"
                echo "  -h, --help       Show this help message"
                exit 0
                ;;
            *)
                error_exit "Unknown option: $1"
                ;;
        esac
    done
}

# Check prerequisites
check_prerequisites() {
    log INFO "Checking prerequisites..."
    
    # Check Java
    if ! command -v java &> /dev/null; then
        error_exit "Java is not installed. Please install Java 21 or later."
    fi
    
    local java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [[ "$java_version" -lt 21 ]]; then
        error_exit "Java 21 or later is required. Current version: $java_version"
    fi
    log DEBUG "Java version check passed: $java_version"
    
    # Check Maven
    if ! command -v mvn &> /dev/null; then
        error_exit "Maven is not installed. Please install Maven 3.8 or later."
    fi
    log DEBUG "Maven check passed"
    
    # Check Docker
    if [[ "$SKIP_DOCKER" == "false" ]]; then
        if ! command -v docker &> /dev/null; then
            error_exit "Docker is not installed. Please install Docker and Docker Compose."
        fi
        
        if ! command -v docker-compose &> /dev/null; then
            error_exit "Docker Compose is not installed."
        fi
        
        # Check if Docker daemon is running
        if ! docker info &> /dev/null; then
            error_exit "Docker daemon is not running. Please start Docker."
        fi
        log DEBUG "Docker checks passed"
    fi
    
    log INFO "✅ All prerequisites satisfied"
}

# Setup Docker infrastructure
setup_docker_infrastructure() {
    if [[ "$SKIP_DOCKER" == "true" ]]; then
        log INFO "Skipping Docker infrastructure setup"
        return 0
    fi
    
    log INFO "Setting up Docker infrastructure..."
    cd "$PROJECT_ROOT"
    
    # Check if containers are already running
    local running_containers=$(docker-compose ps -q)
    if [[ -n "$running_containers" ]]; then
        log WARN "Some containers are already running. Stopping them first..."
        docker-compose down || log WARN "Failed to stop existing containers"
    fi
    
    # Start infrastructure services
    log INFO "Starting infrastructure services (this may take a few minutes)..."
    if [[ "$VERBOSE" == "true" ]]; then
        docker-compose up -d
    else
        docker-compose up -d &> /dev/null
    fi
    
    # Wait for services to be healthy
    log INFO "Waiting for services to be healthy..."
    local max_attempts=30
    local attempt=0
    
    while [[ $attempt -lt $max_attempts ]]; do
        local healthy_count=0
        
        # Check PostgreSQL
        if docker-compose exec -T postgres pg_isready -U rag_user -d rag_enterprise &> /dev/null; then
            ((healthy_count++))
        fi
        
        # Check Redis
        if docker-compose exec -T redis redis-cli ping &> /dev/null; then
            ((healthy_count++))
        fi
        
        # Check Kafka (simplified check)
        if docker-compose ps kafka | grep -q "Up"; then
            ((healthy_count++))
        fi
        
        if [[ $healthy_count -eq 3 ]]; then
            log INFO "✅ All infrastructure services are healthy"
            break
        fi
        
        ((attempt++))
        log DEBUG "Health check attempt $attempt/$max_attempts (healthy: $healthy_count/3)"
        sleep 5
    done
    
    if [[ $attempt -eq $max_attempts ]]; then
        error_exit "Infrastructure services failed to become healthy within 5 minutes"
    fi
}

# Initialize Ollama models
initialize_ollama() {
    log INFO "Initializing Ollama models..."
    
    # Check if Ollama container is running
    if ! docker-compose ps ollama | grep -q "Up"; then
        log WARN "Ollama container is not running. Skipping model initialization."
        return 0
    fi
    
    # Pull essential models
    log INFO "Pulling Ollama models (this may take several minutes)..."
    
    # Pull embedding model
    docker-compose exec -T ollama ollama pull nomic-embed-text || log WARN "Failed to pull embedding model"
    
    # Pull small LLM model for testing
    docker-compose exec -T ollama ollama pull llama2:7b || log WARN "Failed to pull LLM model"
    
    # List available models
    log INFO "Available Ollama models:"
    docker-compose exec -T ollama ollama list || log WARN "Failed to list models"
}

# Build all services
build_services() {
    if [[ "$SKIP_BUILD" == "true" ]]; then
        log INFO "Skipping Maven build"
        return 0
    fi
    
    log INFO "Building all services..."
    cd "$PROJECT_ROOT"
    
    # Clean and build
    if [[ "$VERBOSE" == "true" ]]; then
        mvn clean install -DskipTests
    else
        mvn clean install -DskipTests &> /dev/null
    fi
    
    if [[ $? -eq 0 ]]; then
        log INFO "✅ All services built successfully"
    else
        error_exit "Maven build failed"
    fi
}

# Create environment files
create_environment_files() {
    log INFO "Creating environment configuration files..."
    
    # Create .env file for Docker Compose
    cat > "${PROJECT_ROOT}/.env" << 'EOF'
# Database Configuration
POSTGRES_DB=rag_enterprise
POSTGRES_USER=rag_user
POSTGRES_PASSWORD=rag_password

# Redis Configuration
REDIS_PASSWORD=redis_password

# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# JWT Configuration
JWT_SECRET=admin-super-secret-key-that-should-be-at-least-256-bits-long-for-production-use

# AI/ML Configuration (optional)
# OPENAI_API_KEY=your-openai-key-here
OLLAMA_HOST=http://localhost:11434

# CORS Configuration
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8080
EOF
    
    # Create local development profile
    mkdir -p "${PROJECT_ROOT}/config"
    cat > "${PROJECT_ROOT}/config/application-local.yml" << 'EOF'
# Local development configuration
logging:
  level:
    com.enterprise.rag: DEBUG
    org.springframework.security: INFO
    org.hibernate.SQL: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true

management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: always
EOF
    
    log INFO "✅ Environment files created"
}

# Create service startup scripts
create_service_scripts() {
    log INFO "Creating service management scripts..."
    
    # Create start-all script
    cat > "${PROJECT_ROOT}/scripts/services/start-all-services.sh" << 'EOF'
#!/bin/bash

# Start all RAG services in the correct order
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

echo "Starting all RAG services..."

# Array of services in startup order
services=(
    "rag-auth-service:8081"
    "rag-admin-service:8085"
    "rag-embedding-service:8084"
    "rag-document-service:8083"
    "rag-core-service:8082"
    "rag-gateway:8080"
)

for service_info in "${services[@]}"; do
    service_name=${service_info%:*}
    port=${service_info#*:}
    
    echo "Starting $service_name on port $port..."
    
    # Check if port is already in use
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1; then
        echo "⚠️  Port $port is already in use, skipping $service_name"
        continue
    fi
    
    cd "${PROJECT_ROOT}/${service_name}"
    
    # Start service in background
    nohup mvn spring-boot:run > "${PROJECT_ROOT}/logs/${service_name}.log" 2>&1 &
    echo $! > "${PROJECT_ROOT}/logs/${service_name}.pid"
    
    echo "✅ $service_name started (PID: $(cat ${PROJECT_ROOT}/logs/${service_name}.pid))"
    
    # Wait a moment before starting next service
    sleep 3
done

echo ""
echo "🎉 All services started! Check logs in ${PROJECT_ROOT}/logs/"
echo ""
echo "Service URLs:"
echo "- API Gateway: http://localhost:8080/actuator/health"
echo "- Auth Service: http://localhost:8081/swagger-ui.html"
echo "- Admin Service: http://localhost:8085/admin/api/swagger-ui.html"
echo "- Document Service: http://localhost:8083/swagger-ui.html"
echo "- Embedding Service: http://localhost:8084/swagger-ui.html"
echo "- RAG Core: http://localhost:8082/swagger-ui.html"
EOF

    # Create stop-all script
    cat > "${PROJECT_ROOT}/scripts/services/stop-all-services.sh" << 'EOF'
#!/bin/bash

# Stop all RAG services
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

echo "Stopping all RAG services..."

# Find and kill all service processes
for pidfile in "${PROJECT_ROOT}"/logs/*.pid; do
    if [[ -f "$pidfile" ]]; then
        service_name=$(basename "$pidfile" .pid)
        pid=$(cat "$pidfile")
        
        if kill -0 "$pid" 2>/dev/null; then
            echo "Stopping $service_name (PID: $pid)..."
            kill "$pid"
            rm "$pidfile"
            echo "✅ $service_name stopped"
        else
            echo "⚠️  $service_name was not running"
            rm "$pidfile"
        fi
    fi
done

echo "🛑 All services stopped"
EOF

    # Make scripts executable
    chmod +x "${PROJECT_ROOT}/scripts/services/start-all-services.sh"
    chmod +x "${PROJECT_ROOT}/scripts/services/stop-all-services.sh"
    
    log INFO "✅ Service management scripts created"
}

# Create health check script
create_health_check_script() {
    log INFO "Creating health check script..."
    
    cat > "${PROJECT_ROOT}/scripts/utils/health-check.sh" << 'EOF'
#!/bin/bash

# Health check script for all RAG services
set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "🏥 RAG System Health Check"
echo "=========================="

# Infrastructure services
echo ""
echo "📋 Infrastructure Services:"

# PostgreSQL
if docker-compose exec -T postgres pg_isready -U rag_user -d rag_enterprise &> /dev/null; then
    echo -e "✅ PostgreSQL: ${GREEN}Healthy${NC}"
else
    echo -e "❌ PostgreSQL: ${RED}Unhealthy${NC}"
fi

# Redis
if docker-compose exec -T redis redis-cli ping &> /dev/null; then
    echo -e "✅ Redis: ${GREEN}Healthy${NC}"
else
    echo -e "❌ Redis: ${RED}Unhealthy${NC}"
fi

# Kafka (simplified check)
if docker-compose ps kafka | grep -q "Up"; then
    echo -e "✅ Kafka: ${GREEN}Running${NC}"
else
    echo -e "❌ Kafka: ${RED}Not Running${NC}"
fi

# Application services
echo ""
echo "🚀 Application Services:"

services=(
    "API Gateway:http://localhost:8080/actuator/health"
    "Auth Service:http://localhost:8081/actuator/health"
    "Admin Service:http://localhost:8085/admin/api/actuator/health"
    "Document Service:http://localhost:8083/actuator/health"
    "Embedding Service:http://localhost:8084/actuator/health"
    "RAG Core:http://localhost:8082/actuator/health"
)

for service_info in "${services[@]}"; do
    name=${service_info%:*}
    url=${service_info#*:}
    
    if curl -s "$url" | grep -q '"status":"UP"' 2>/dev/null; then
        echo -e "✅ $name: ${GREEN}Healthy${NC}"
    else
        echo -e "❌ $name: ${RED}Unhealthy${NC}"
    fi
done

echo ""
echo "🎯 Quick Test URLs:"
echo "- Grafana Dashboard: http://localhost:3000 (admin/admin)"
echo "- Kafka UI: http://localhost:8080"
echo "- Redis Insight: http://localhost:8001"
EOF

    chmod +x "${PROJECT_ROOT}/scripts/utils/health-check.sh"
    
    log INFO "✅ Health check script created"
}

# Create test script
create_test_script() {
    log INFO "Creating system test script..."
    
    cat > "${PROJECT_ROOT}/scripts/tests/test-system.sh" << 'EOF'
#!/bin/bash

# System integration test script
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "🧪 RAG System Integration Tests"
echo "================================"

# Test 1: Admin Service Authentication
echo ""
echo "Test 1: Admin Service Authentication"
response=$(curl -s -w "%{http_code}" -X POST http://localhost:8085/admin/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{
        "username": "admin@enterprise.com",
        "password": "admin123"
    }')

http_code="${response: -3}"
if [[ "$http_code" == "200" ]]; then
    echo -e "✅ Admin authentication: ${GREEN}PASSED${NC}"
else
    echo -e "❌ Admin authentication: ${RED}FAILED${NC} (HTTP: $http_code)"
fi

# Test 2: Service Health Checks
echo ""
echo "Test 2: Service Health Checks"
healthy_count=0
total_services=6

services=(
    "Gateway:http://localhost:8080/actuator/health"
    "Auth:http://localhost:8081/actuator/health"
    "Admin:http://localhost:8085/admin/api/actuator/health"
    "Document:http://localhost:8083/actuator/health"
    "Embedding:http://localhost:8084/actuator/health"
    "Core:http://localhost:8082/actuator/health"
)

for service_info in "${services[@]}"; do
    name=${service_info%:*}
    url=${service_info#*:}
    
    if curl -s "$url" | grep -q '"status":"UP"' 2>/dev/null; then
        echo -e "✅ $name service: ${GREEN}HEALTHY${NC}"
        ((healthy_count++))
    else
        echo -e "❌ $name service: ${RED}UNHEALTHY${NC}"
    fi
done

echo ""
echo "📊 Test Summary:"
echo "- Services healthy: $healthy_count/$total_services"

if [[ $healthy_count -eq $total_services ]]; then
    echo -e "🎉 System Status: ${GREEN}ALL SYSTEMS OPERATIONAL${NC}"
    exit 0
else
    echo -e "⚠️  System Status: ${YELLOW}PARTIALLY OPERATIONAL${NC}"
    exit 1
fi
EOF

    chmod +x "${PROJECT_ROOT}/scripts/tests/test-system.sh"
    
    log INFO "✅ System test script created"
}

# Main setup function
main() {
    echo "🚀 Enterprise RAG System - Local Development Setup"
    echo "=================================================="
    
    parse_args "$@"
    
    log INFO "Setup started with options: skip-docker=$SKIP_DOCKER, skip-build=$SKIP_BUILD, verbose=$VERBOSE"
    
    check_prerequisites
    create_environment_files
    setup_docker_infrastructure
    initialize_ollama
    build_services
    create_service_scripts
    create_health_check_script
    create_test_script
    
    log INFO "✅ Setup completed successfully!"
    echo ""
    echo "🎯 Next steps:"
    echo "1. Start all services: ./scripts/services/start-all-services.sh"
    echo "2. Check system health: ./scripts/utils/health-check.sh"
    echo "3. Run integration tests: ./scripts/tests/test-system.sh"
    echo ""
    echo "📚 Documentation:"
    echo "- Full deployment guide: ./DEPLOYMENT.md"
    echo "- API documentation available at service URLs"
    echo ""
    echo "📋 Logs are available in: ${LOG_DIR}/"
}

# Run main function with all arguments
main "$@"
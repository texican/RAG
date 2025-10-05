#!/bin/bash

################################################################################
# RAG System Pre-flight Check
#
# Comprehensive system verification before running tests
################################################################################

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

ERRORS=0
WARNINGS=0
CHECKS=0

print_header() {
    echo -e "\n${CYAN}═══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  $1${NC}"
    echo -e "${CYAN}═══════════════════════════════════════════════════════════${NC}\n"
}

print_check() {
    echo -n "Checking $1... "
    ((CHECKS++))
}

print_ok() {
    echo -e "${GREEN}✓${NC}"
}

print_fail() {
    echo -e "${RED}✗${NC} $1"
    ((ERRORS++))
}

print_warn() {
    echo -e "${YELLOW}⚠${NC} $1"
    ((WARNINGS++))
}

print_header "RAG System Pre-flight Check"

# Check Docker
print_check "Docker daemon"
if docker info > /dev/null 2>&1; then
    print_ok
else
    print_fail "Docker is not running"
fi

# Check Docker Compose
print_check "Docker Compose"
if docker-compose --version > /dev/null 2>&1; then
    print_ok
else
    print_fail "Docker Compose is not installed"
fi

# Check Java
print_check "Java (JDK 17+)"
if java -version 2>&1 | grep -q "version \"17\|version \"18\|version \"19\|version \"20\|version \"21"; then
    print_ok
else
    print_fail "Java 17+ is required"
fi

# Check Maven
print_check "Maven"
if [ -f "./mvnw" ]; then
    print_ok
else
    print_fail "Maven wrapper (mvnw) not found"
fi

print_header "Container Status"

containers=(
    "rag-postgres:PostgreSQL Database"
    "rag-redis:Redis Cache"
    "rag-kafka:Kafka Message Bus"
    "rag-zookeeper:Zookeeper"
    "rag-ollama:Ollama LLM"
    "rag-auth:Auth Service"
    "rag-document:Document Service"
    "rag-embedding:Embedding Service"
    "rag-core:Core Service"
    "rag-admin:Admin Service"
)

for container_info in "${containers[@]}"; do
    IFS=':' read -r container_name service_name <<< "$container_info"
    print_check "$service_name"

    if docker ps --format '{{.Names}}' | grep -q "^${container_name}$"; then
        status=$(docker inspect -f '{{.State.Status}}' "$container_name" 2>/dev/null)
        if [ "$status" = "running" ]; then
            health=$(docker inspect -f '{{.State.Health.Status}}' "$container_name" 2>/dev/null || echo "none")
            if [ "$health" = "healthy" ] || [ "$health" = "none" ]; then
                print_ok
            else
                print_warn "Container running but health=$health"
            fi
        else
            print_fail "Container exists but status=$status"
        fi
    else
        print_fail "Container not found"
    fi
done

print_header "Service Health Endpoints"

services=(
    "8081:Auth Service:http://localhost:8081/actuator/health"
    "8082:Document Service:http://localhost:8082/actuator/health"
    "8083:Embedding Service:http://localhost:8083/actuator/health"
    "8084:Core Service:http://localhost:8084/actuator/health"
    "8085:Admin Service:http://localhost:8085/actuator/health"
)

for service_info in "${services[@]}"; do
    IFS=':' read -r port service_name url <<< "$service_info"
    print_check "$service_name (port $port)"

    if response=$(curl -sf "$url" 2>&1); then
        if echo "$response" | grep -q '"status":"UP"'; then
            print_ok
        else
            print_warn "Responding but not healthy"
        fi
    else
        print_fail "Not responding at $url"
    fi
done

print_header "Infrastructure Services"

# PostgreSQL
print_check "PostgreSQL database connection"
if docker exec rag-postgres pg_isready -U rag_user > /dev/null 2>&1; then
    print_ok
else
    print_fail "Cannot connect to PostgreSQL"
fi

print_check "Database 'rag_enterprise' exists"
if docker exec rag-postgres psql -U rag_user -lqt | cut -d \| -f 1 | grep -qw rag_enterprise; then
    print_ok
else
    print_fail "Database not found"
fi

# Redis
print_check "Redis connection"
if docker exec rag-redis redis-cli -a redis_password ping 2>/dev/null | grep -q PONG; then
    print_ok
else
    print_fail "Cannot connect to Redis"
fi

# Kafka
print_check "Kafka broker API"
if docker exec rag-kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1; then
    print_ok
else
    print_fail "Kafka broker not responding"
fi

# Ollama
print_check "Ollama API"
if curl -sf http://localhost:11434/api/tags > /dev/null 2>&1; then
    print_ok
else
    print_fail "Ollama not responding"
fi

print_check "Ollama models (llama3.2:1b)"
if curl -sf http://localhost:11434/api/tags | grep -q "llama3.2:1b"; then
    print_ok
else
    print_warn "Model llama3.2:1b not found - tests may fail"
fi

print_check "Ollama models (nomic-embed-text)"
if curl -sf http://localhost:11434/api/tags | grep -q "nomic-embed-text"; then
    print_ok
else
    print_warn "Model nomic-embed-text not found - tests may fail"
fi

print_header "Test Resources"

print_check "Test documents directory"
if [ -d "rag-integration-tests/src/test/resources/test-documents" ]; then
    print_ok
else
    print_fail "Test documents directory not found"
fi

print_check "Test document: company-policy.md"
if [ -f "rag-integration-tests/src/test/resources/test-documents/company-policy.md" ]; then
    print_ok
else
    print_fail "File not found"
fi

print_check "Test document: product-specification.md"
if [ -f "rag-integration-tests/src/test/resources/test-documents/product-specification.md" ]; then
    print_ok
else
    print_fail "File not found"
fi

print_check "Test document: api-documentation.md"
if [ -f "rag-integration-tests/src/test/resources/test-documents/api-documentation.md" ]; then
    print_ok
else
    print_fail "File not found"
fi

print_check "E2E test class"
if [ -f "rag-integration-tests/src/test/java/com/byo/rag/integration/endtoend/ComprehensiveRagEndToEndIT.java" ]; then
    print_ok
else
    print_fail "Test class not found"
fi

print_header "Summary"

echo "Total checks: $CHECKS"
echo -e "${GREEN}Passed: $((CHECKS - ERRORS - WARNINGS))${NC}"
if [ $WARNINGS -gt 0 ]; then
    echo -e "${YELLOW}Warnings: $WARNINGS${NC}"
fi
if [ $ERRORS -gt 0 ]; then
    echo -e "${RED}Errors: $ERRORS${NC}"
fi

echo ""

if [ $ERRORS -eq 0 ]; then
    echo -e "${GREEN}✓ System is ready for testing!${NC}"
    exit 0
else
    echo -e "${RED}✗ System is not ready. Please fix the errors above.${NC}"
    echo ""
    echo "Common fixes:"
    echo "  - Start services: docker-compose up -d"
    echo "  - Check logs: docker-compose logs [service-name]"
    echo "  - Rebuild services: docker-compose up -d --build"
    echo "  - Install Ollama models: docker exec rag-ollama ollama pull llama3.2:1b"
    exit 1
fi

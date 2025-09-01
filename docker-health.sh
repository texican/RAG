#!/bin/bash

# Enterprise RAG System - Health Check Script
# Monitors all services and provides detailed status

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🏥 Enterprise RAG System - Health Check${NC}"
echo "=============================================="

# Function to check HTTP endpoint health
check_http_health() {
    local service_name=$1
    local url=$2
    local timeout=${3:-10}
    
    if curl -f -s --max-time $timeout "$url" > /dev/null 2>&1; then
        echo -e "✅ ${service_name}: ${GREEN}Healthy${NC}"
        return 0
    else
        echo -e "❌ ${service_name}: ${RED}Unhealthy${NC}"
        return 1
    fi
}

# Function to check port connectivity
check_port() {
    local service_name=$1
    local host=$2
    local port=$3
    
    if nc -z "$host" "$port" 2>/dev/null; then
        echo -e "✅ ${service_name}: ${GREEN}Port $port accessible${NC}"
        return 0
    else
        echo -e "❌ ${service_name}: ${RED}Port $port not accessible${NC}"
        return 1
    fi
}

# Check Docker containers
echo -e "\n${YELLOW}📦 Container Status${NC}"
docker-compose ps

echo -e "\n${YELLOW}🔍 Service Health Checks${NC}"

# Infrastructure Services
echo -e "\n${BLUE}Infrastructure Services:${NC}"
check_port "PostgreSQL" "localhost" "5432"
check_port "Redis" "localhost" "6379" 
check_port "Kafka" "localhost" "9092"
check_port "Ollama" "localhost" "11434"

# Microservices Health Endpoints
echo -e "\n${BLUE}Microservices Health:${NC}"
check_http_health "Gateway" "http://localhost:8080/actuator/health"
check_http_health "Auth Service" "http://localhost:8081/actuator/health"
check_http_health "Document Service" "http://localhost:8082/actuator/health"
check_http_health "Embedding Service" "http://localhost:8083/actuator/health"
check_http_health "Core Service" "http://localhost:8084/actuator/health"
check_http_health "Admin Service" "http://localhost:8085/actuator/health"

# Monitoring Services
echo -e "\n${BLUE}Monitoring Services:${NC}"
check_http_health "Prometheus" "http://localhost:9090/-/healthy" 5
check_http_health "Grafana" "http://localhost:3000/api/health" 5
check_http_health "Kafka UI" "http://localhost:8080/actuator/health" 5

# Service-specific health details
echo -e "\n${YELLOW}🔬 Detailed Health Information${NC}"

# Check database connectivity
echo -e "\n${BLUE}Database Connectivity:${NC}"
if docker-compose exec -T postgres pg_isready -U rag_user -d rag_enterprise > /dev/null 2>&1; then
    echo -e "✅ PostgreSQL: ${GREEN}Database accessible${NC}"
else
    echo -e "❌ PostgreSQL: ${RED}Database not accessible${NC}"
fi

# Check Redis connectivity
echo -e "\n${BLUE}Redis Connectivity:${NC}"
if docker-compose exec -T redis redis-cli -a redis_password ping 2>/dev/null | grep -q PONG; then
    echo -e "✅ Redis: ${GREEN}Cache accessible${NC}"
else
    echo -e "❌ Redis: ${RED}Cache not accessible${NC}"
fi

# Check Kafka topics
echo -e "\n${BLUE}Kafka Status:${NC}"
KAFKA_TOPICS=$(docker-compose exec -T kafka kafka-topics --bootstrap-server localhost:9092 --list 2>/dev/null | wc -l)
echo -e "📊 Kafka Topics: $KAFKA_TOPICS"

# Memory and resource usage
echo -e "\n${YELLOW}💾 Resource Usage${NC}"
echo "Docker containers resource usage:"
docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}" | head -10

# Service logs (last few lines)
echo -e "\n${YELLOW}📜 Recent Service Logs${NC}"
echo "Gateway Service (last 3 lines):"
docker-compose logs --tail=3 rag-gateway 2>/dev/null || echo "No logs available"

echo -e "\n${GREEN}🏁 Health Check Complete${NC}"
echo "For detailed logs: docker-compose logs -f [service-name]"
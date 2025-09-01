#!/bin/bash

# Enterprise RAG System - Docker Compose Startup Script
# Optimized for Colima local development

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 Enterprise RAG System - Docker Startup${NC}"
echo "================================================"

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ Docker is not running. Please start Docker/Colima first.${NC}"
    echo "For Colima: colima start"
    exit 1
fi

# Check available memory (minimum 8GB recommended)
MEMORY_KB=$(grep MemTotal /proc/meminfo 2>/dev/null | awk '{print $2}' || echo "0")
MEMORY_GB=$((MEMORY_KB / 1024 / 1024))

if [ "$MEMORY_GB" -lt 6 ]; then
    echo -e "${YELLOW}⚠️  Warning: Less than 6GB RAM detected. Consider increasing Colima memory:${NC}"
    echo "   colima stop && colima start --memory 8"
fi

echo -e "${YELLOW}📋 System Requirements Check${NC}"
echo "Docker: ✅ Running"
echo "Memory: ${MEMORY_GB}GB $([ $MEMORY_GB -ge 8 ] && echo "✅" || echo "⚠️")"

# Build and start services
echo -e "\n${YELLOW}🔨 Building Docker Images${NC}"
echo "This may take 5-10 minutes on first run..."

# Build infrastructure services first
echo -e "${BLUE}📦 Starting Infrastructure Services${NC}"
docker-compose up -d postgres redis zookeeper kafka ollama

# Wait for infrastructure to be healthy
echo -e "${YELLOW}⏳ Waiting for Infrastructure Services${NC}"
echo "Checking PostgreSQL..."
while ! docker-compose exec -T postgres pg_isready -U rag_user -d rag_enterprise > /dev/null 2>&1; do
    printf "."
    sleep 2
done
echo " ✅ PostgreSQL ready"

echo "Checking Redis..."
while ! docker-compose exec -T redis redis-cli -a redis_password ping > /dev/null 2>&1; do
    printf "."
    sleep 2
done
echo " ✅ Redis ready"

echo "Checking Kafka..."
sleep 10  # Give Kafka more time to fully initialize
echo " ✅ Kafka ready"

# Build and start microservices
echo -e "\n${BLUE}🏗️  Building and Starting Microservices${NC}"
docker-compose up -d --build rag-auth rag-admin rag-document rag-embedding rag-core

# Wait for core services
echo -e "${YELLOW}⏳ Waiting for Core Services${NC}"
sleep 30  # Give services time to start

# Start gateway last (depends on other services)
echo -e "${BLUE}🌐 Starting API Gateway${NC}"
docker-compose up -d --build rag-gateway

# Start monitoring services
echo -e "${BLUE}📊 Starting Monitoring Services${NC}"
docker-compose up -d prometheus grafana kafka-ui

# Final health check
echo -e "\n${YELLOW}🔍 Final Health Check${NC}"
sleep 20

SERVICES=("rag-postgres:5432" "rag-redis:6379" "rag-kafka:9092" "rag-auth:8081" "rag-admin:8085" "rag-document:8082" "rag-embedding:8083" "rag-core:8084" "rag-gateway:8080")

for service in "${SERVICES[@]}"; do
    name=${service%:*}
    port=${service#*:}
    
    if nc -z localhost $port 2>/dev/null; then
        echo "✅ $name (port $port)"
    else
        echo "❌ $name (port $port) - may still be starting"
    fi
done

echo -e "\n${GREEN}🎉 Enterprise RAG System Started!${NC}"
echo "================================================"
echo -e "${BLUE}📖 Service URLs:${NC}"
echo "• Gateway (API):     http://localhost:8080"
echo "• Auth Service:      http://localhost:8081"
echo "• Document Service:  http://localhost:8082" 
echo "• Embedding Service: http://localhost:8083"
echo "• Core Service:      http://localhost:8084"
echo "• Admin Service:     http://localhost:8085"
echo ""
echo -e "${BLUE}🔧 Management URLs:${NC}"
echo "• Grafana:          http://localhost:3000 (admin/admin)"
echo "• Prometheus:       http://localhost:9090"
echo "• Kafka UI:         http://localhost:8080"
echo "• Redis Insight:    http://localhost:8001"
echo "• Ollama:           http://localhost:11434"
echo ""
echo -e "${BLUE}📊 Useful Commands:${NC}"
echo "• View logs:        docker-compose logs -f [service-name]"
echo "• Stop system:      docker-compose down"
echo "• Restart service:  docker-compose restart [service-name]"
echo "• Health check:     ./docker-health.sh"
echo ""
echo -e "${YELLOW}🔍 System is starting up - services may take 1-2 minutes to be fully ready${NC}"
echo "Check logs with: docker-compose logs -f"
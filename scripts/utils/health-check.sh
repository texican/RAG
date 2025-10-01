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
if docker exec postgres pg_isready -U rag_user -d rag_enterprise &> /dev/null 2>&1 || \
   docker exec enterprise-rag-postgres pg_isready -U rag_user -d rag_enterprise &> /dev/null 2>&1 || \
   nc -z localhost 5432 &> /dev/null 2>&1; then
    echo -e "✅ PostgreSQL: ${GREEN}Healthy${NC}"
else
    echo -e "❌ PostgreSQL: ${RED}Unhealthy${NC}"
fi

# Redis
if docker exec redis redis-cli ping &> /dev/null 2>&1 || \
   docker exec enterprise-rag-redis redis-cli ping &> /dev/null 2>&1 || \
   redis-cli -h localhost ping &> /dev/null 2>&1; then
    echo -e "✅ Redis: ${GREEN}Healthy${NC}"
else
    echo -e "❌ Redis: ${RED}Unhealthy${NC}"
fi

# Kafka
if docker ps --format '{{.Names}}' | grep -q "kafka" && \
   docker ps --format '{{.Names}}:{{.Status}}' | grep "kafka" | grep -q "Up"; then
    echo -e "✅ Kafka: ${GREEN}Running${NC}"
else
    echo -e "❌ Kafka: ${RED}Not Running${NC}"
fi

# Application services
echo ""
echo "🚀 Application Services:"

services=(
    "Auth Service:http://localhost:8081/actuator/health"
    "Document Service:http://localhost:8082/actuator/health"
    "Embedding Service:http://localhost:8083/actuator/health"
    "Core Service:http://localhost:8084/actuator/health"
    "Admin Service:http://localhost:8085/admin/api/actuator/health"
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
echo "- Redis Insight: http://localhost:8001"

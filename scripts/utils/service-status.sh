#!/bin/bash

# Service Status Check - Show which services are running and on which ports
set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "🔍 RAG Service Status Check"
echo "==========================="

# Define services and their ports (gateway archived per ADR-001)
services=(
    "rag-auth-service:8081"
    "rag-document-service:8082"
    "rag-embedding-service:8083"
    "rag-core-service:8084"
    "rag-admin-service:8086"
    # "rag-gateway:8080"  # Archived per ADR-001
)

echo ""
echo "📋 Application Services:"

for service_info in "${services[@]}"; do
    service_name=${service_info%:*}
    port=${service_info#*:}
    
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1; then
        pid=$(lsof -Pi :$port -sTCP:LISTEN -t | head -1)
        process_name=$(ps -p $pid -o comm= 2>/dev/null || echo "unknown")
        echo -e "✅ $service_name: ${GREEN}Running${NC} (Port $port, PID: $pid)"
        
        # Check if it's actually our service by testing health endpoint
        case $port in
            8086) health_url="http://localhost:$port/admin/api/actuator/health" ;;
            *) health_url="http://localhost:$port/actuator/health" ;;
        esac
        
        if curl -s -f "$health_url" >/dev/null 2>&1; then
            echo -e "   🏥 Health check: ${GREEN}PASSED${NC}"
        else
            echo -e "   🏥 Health check: ${YELLOW}PENDING${NC}"
        fi
    else
        echo -e "❌ $service_name: ${RED}Not Running${NC} (Port $port available)"
    fi
    echo ""
done

echo "🐳 Infrastructure Services:"
echo ""

# Check Docker services
if docker-compose ps postgres | grep -q "Up"; then
    echo -e "✅ PostgreSQL: ${GREEN}Running${NC}"
else
    echo -e "❌ PostgreSQL: ${RED}Stopped${NC}"
fi

if docker-compose ps redis | grep -q "Up"; then
    echo -e "✅ Redis Stack: ${GREEN}Running${NC}"
else
    echo -e "❌ Redis Stack: ${RED}Stopped${NC}"
fi

if docker-compose ps kafka | grep -q "Up"; then
    echo -e "✅ Kafka: ${GREEN}Running${NC}"
else
    echo -e "❌ Kafka: ${RED}Stopped${NC}"
fi

if docker-compose ps grafana | grep -q "Up"; then
    echo -e "✅ Grafana: ${GREEN}Running${NC}"
else
    echo -e "❌ Grafana: ${RED}Stopped${NC}"
fi

echo ""
echo "🌐 Access URLs (Direct Service Access - ADR-001):"
echo "- Grafana Dashboard: http://localhost:3000 (admin/admin)"
echo "- Redis Insight: http://localhost:8001"
echo "- Auth Service: http://localhost:8081/swagger-ui.html"
echo "- Document Service: http://localhost:8082/swagger-ui.html"
echo "- Admin Service: http://localhost:8086/admin/api/swagger-ui.html"
echo ""
echo "Note: Gateway archived - using direct service access per ADR-001"
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
        "username": "admin@enterprise-rag.com",
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

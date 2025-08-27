#!/bin/bash

# Wait for Services - Wait for all services to be healthy before proceeding
set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

TIMEOUT=${1:-300}  # Default 5 minute timeout
INTERVAL=5

echo "⏳ Waiting for services to be healthy (timeout: ${TIMEOUT}s)..."

# Define services and their health endpoints
services=(
    "Auth Service:http://localhost:8081/actuator/health"
    "Admin Service:http://localhost:8085/admin/api/actuator/health" 
    "Document Service:http://localhost:8083/actuator/health"
    "Embedding Service:http://localhost:8084/actuator/health"
    "Core Service:http://localhost:8082/actuator/health"
    "Gateway:http://localhost:8080/actuator/health"
)

wait_for_service() {
    local name=$1
    local url=$2
    local elapsed=0
    
    echo -n "Waiting for $name... "
    
    while [[ $elapsed -lt $TIMEOUT ]]; do
        if curl -s -f "$url" | grep -q '"status":"UP"' 2>/dev/null; then
            echo -e "${GREEN}Ready${NC} (${elapsed}s)"
            return 0
        fi
        
        sleep $INTERVAL
        elapsed=$((elapsed + INTERVAL))
        echo -n "."
    done
    
    echo -e " ${RED}Timeout${NC}"
    return 1
}

# Wait for each service
healthy_count=0
total_services=${#services[@]}

for service_info in "${services[@]}"; do
    name=${service_info%:*}
    url=${service_info#*:}
    
    if wait_for_service "$name" "$url"; then
        ((healthy_count++))
    fi
done

echo ""
if [[ $healthy_count -eq $total_services ]]; then
    echo -e "🎉 All services are healthy! (${healthy_count}/${total_services})"
    exit 0
else
    echo -e "⚠️  Some services failed to start: ${healthy_count}/${total_services} healthy"
    echo ""
    echo "🔍 Use './scripts/utils/service-status.sh' for detailed status"
    echo "📋 Check service logs in ./logs/ directory"
    exit 1
fi
#!/bin/bash

# Start all RAG services in the correct order
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

echo "Starting all RAG services..."

# Array of services in startup order (DOCKER-001 corrected ports)
services=(
    "rag-auth-service:8081"
    "rag-admin-service:8085"
    "rag-document-service:8082"
    "rag-embedding-service:8083"
    "rag-core-service:8084"
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

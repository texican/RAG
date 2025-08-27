#!/bin/bash

# Quick Start Script - Get RAG system running with one command
# Usage: ./scripts/utils/quick-start.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🚀 RAG System Quick Start${NC}"
echo "=========================="

# Step 1: Run complete setup
echo -e "${YELLOW}Step 1: Setting up environment...${NC}"
"${PROJECT_ROOT}/scripts/setup/setup-local-dev.sh"

# Step 2: Start all services
echo -e "${YELLOW}Step 2: Starting all services...${NC}"
"${PROJECT_ROOT}/scripts/services/start-all-services.sh"

# Step 3: Wait for services to be ready
echo -e "${YELLOW}Step 3: Waiting for services to be ready...${NC}"
sleep 15

# Step 4: Run health check
echo -e "${YELLOW}Step 4: Verifying system health...${NC}"
"${PROJECT_ROOT}/scripts/utils/health-check.sh"

# Step 5: Run tests
echo -e "${YELLOW}Step 5: Running integration tests...${NC}"
"${PROJECT_ROOT}/scripts/tests/test-system.sh"

echo ""
echo -e "${GREEN}🎉 RAG System is ready for development!${NC}"
echo ""
echo "🎯 Useful URLs:"
echo "- API Gateway: http://localhost:8080"
echo "- Admin Service: http://localhost:8085/admin/api/swagger-ui.html"
echo "- Grafana Dashboard: http://localhost:3000 (admin/admin)"
echo "- Kafka UI: http://localhost:8080"
echo ""
echo "📋 Management Commands:"
echo "- Stop services: ./scripts/services/stop-all-services.sh"
echo "- Health check: ./scripts/utils/health-check.sh"
echo "- Run tests: ./scripts/tests/test-system.sh"
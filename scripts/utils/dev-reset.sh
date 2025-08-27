#!/bin/bash

# Development Reset Script - Clean and reset the entire development environment
# Usage: ./scripts/utils/dev-reset.sh [--force]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

FORCE=false
if [[ "${1:-}" == "--force" ]]; then
    FORCE=true
fi

echo -e "${YELLOW}🔄 RAG System Development Reset${NC}"
echo "================================"

if [[ "$FORCE" == "false" ]]; then
    echo -e "${RED}⚠️  This will:${NC}"
    echo "- Stop all running services"
    echo "- Remove all Docker containers and volumes"
    echo "- Clean all Maven build artifacts"
    echo "- Remove logs and temporary files"
    echo ""
    read -p "Are you sure you want to continue? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Reset cancelled."
        exit 0
    fi
fi

cd "$PROJECT_ROOT"

# Step 1: Stop all services
echo -e "${YELLOW}Step 1: Stopping all services...${NC}"
if [[ -f "scripts/services/stop-all-services.sh" ]]; then
    ./scripts/services/stop-all-services.sh || true
else
    echo "Stop script not found, manually killing processes..."
    pkill -f "spring-boot:run" || true
fi

# Step 2: Stop and remove Docker infrastructure
echo -e "${YELLOW}Step 2: Cleaning Docker infrastructure...${NC}"
docker-compose down --volumes --remove-orphans || true
docker system prune -f || true

# Step 3: Clean Maven artifacts
echo -e "${YELLOW}Step 3: Cleaning Maven artifacts...${NC}"
mvn clean || true

# Step 4: Remove logs and temporary files
echo -e "${YELLOW}Step 4: Cleaning logs and temporary files...${NC}"
rm -rf logs/*.log* || true
rm -rf logs/*.pid || true
rm -rf */logs/* || true
rm -rf */target/* || true
rm -rf .env || true

# Step 5: Remove application-specific temporary directories
echo -e "${YELLOW}Step 5: Cleaning application temporary files...${NC}"
rm -rf rag-document-service/storage/* || true
rm -rf rag-document-service/test-storage/* || true

echo -e "${GREEN}✅ Development environment reset complete!${NC}"
echo ""
echo "🎯 To get back up and running:"
echo "./scripts/utils/quick-start.sh"
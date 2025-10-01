#!/bin/bash

# Rebuild and restart a specific RAG service with proper image management
# Usage: ./scripts/dev/rebuild-service.sh <service-name> [--no-cache]

set -e

BLUE='\033[0;34m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

SERVICE=$1
NO_CACHE=$2

if [ -z "$SERVICE" ]; then
    echo -e "${RED}❌ Error: Service name required${NC}"
    echo ""
    echo "Usage: $0 <service-name> [--no-cache]"
    echo ""
    echo "Available services:"
    echo "  - rag-auth"
    echo "  - rag-document"
    echo "  - rag-embedding"
    echo "  - rag-core"
    echo "  - rag-admin"
    echo ""
    echo "Example: $0 rag-auth --no-cache"
    exit 1
fi

# Map service names to Maven modules
case $SERVICE in
    rag-auth)
        MODULE="rag-auth-service"
        ;;
    rag-document)
        MODULE="rag-document-service"
        ;;
    rag-embedding)
        MODULE="rag-embedding-service"
        ;;
    rag-core)
        MODULE="rag-core-service"
        ;;
    rag-admin)
        MODULE="rag-admin-service"
        ;;
    *)
        echo -e "${RED}❌ Error: Unknown service '$SERVICE'${NC}"
        exit 1
        ;;
esac

echo -e "${BLUE}🔨 Rebuilding $SERVICE${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Step 1: Build JAR with Maven
echo -e "${YELLOW}📦 Building JAR with Maven...${NC}"
mvn clean package -DskipTests -pl $MODULE -am
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Maven build failed${NC}"
    exit 1
fi
echo -e "${GREEN}✅ JAR built successfully${NC}"
echo ""

# Step 2: Build Docker image
echo -e "${YELLOW}🐳 Building Docker image...${NC}"
BUILD_CMD="docker-compose build"
if [ "$NO_CACHE" == "--no-cache" ]; then
    BUILD_CMD="$BUILD_CMD --no-cache"
    echo "   Using --no-cache flag"
fi
BUILD_CMD="$BUILD_CMD $SERVICE"

$BUILD_CMD
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Docker build failed${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Docker image built successfully${NC}"
echo ""

# Step 3: Stop and remove old container
echo -e "${YELLOW}🛑 Stopping old container...${NC}"
docker stop $SERVICE 2>/dev/null || true
docker rm $SERVICE 2>/dev/null || true
echo -e "${GREEN}✅ Old container removed${NC}"
echo ""

# Step 4: Start new container
echo -e "${YELLOW}🚀 Starting new container...${NC}"
docker-compose up -d $SERVICE
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Failed to start container${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Container started${NC}"
echo ""

# Step 5: Wait for health check
echo -e "${YELLOW}⏳ Waiting for service to be healthy (30s max)...${NC}"
for i in {1..30}; do
    HEALTH=$(docker inspect --format='{{.State.Health.Status}}' $SERVICE 2>/dev/null || echo "unknown")
    if [ "$HEALTH" == "healthy" ]; then
        echo -e "${GREEN}✅ Service is healthy!${NC}"
        echo ""
        echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
        echo -e "${GREEN}✅ $SERVICE rebuilt and running${NC}"
        echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
        exit 0
    fi
    if [ $((i % 5)) -eq 0 ]; then
        echo "   Still waiting... ($i/30s)"
    fi
    sleep 1
done

echo -e "${YELLOW}⚠️  Health check not passing yet (container may still be starting)${NC}"
echo ""
echo "Check status with: docker ps | grep $SERVICE"
echo "Check logs with: docker logs $SERVICE"

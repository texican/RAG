#!/bin/bash

# Launch Swagger UI endpoints with authentication
# This script opens Swagger UIs in your default browser and displays credentials

set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "🚀 Launching RAG System Swagger UIs"
echo "===================================="
echo ""

# Check if services are running
echo "Checking services..."
healthy_count=0
for port in 8081 8082 8083 8084 8085; do
    if curl -s "http://localhost:$port/actuator/health" >/dev/null 2>&1 || \
       curl -s "http://localhost:$port/admin/api/actuator/health" >/dev/null 2>&1; then
        ((healthy_count++))
    fi
done

if [[ $healthy_count -eq 0 ]]; then
    echo "❌ No services appear to be running!"
    echo "Start services first with: docker-compose up -d"
    exit 1
fi

echo -e "${GREEN}✅ Found $healthy_count running services${NC}"
echo ""

# Function to get password from service logs
get_password() {
    local service_name=$1
    docker logs "$service_name" 2>&1 | grep "Using generated security password" | tail -1 | sed 's/.*Using generated security password: //' || echo ""
}

# Collect credentials
echo "📋 Collecting credentials..."
password_auth=$(get_password "rag-auth")
password_embedding=$(get_password "rag-embedding")
password_core=$(get_password "rag-core")
password_admin=$(get_password "rag-admin")

# Display credentials
echo ""
echo "🔑 Current Credentials (copy these!):"
echo "======================================"
echo ""
echo -e "${BLUE}📋 Document Service (Public - No Auth Required)${NC}"
echo "   URL: http://localhost:8082/swagger-ui.html"
echo ""

if [[ -n "$password_auth" ]]; then
    echo -e "${BLUE}📋 Auth Service (Port 8081)${NC}"
    echo "   Username: user"
    echo "   Password: $password_auth"
    echo ""
fi

if [[ -n "$password_embedding" ]]; then
    echo -e "${BLUE}📋 Embedding Service (Port 8083)${NC}"
    echo "   Username: user"
    echo "   Password: $password_embedding"
    echo ""
fi

if [[ -n "$password_core" ]]; then
    echo -e "${BLUE}📋 Core Service (Port 8084)${NC}"
    echo "   Username: user"
    echo "   Password: $password_core"
    echo ""
fi

echo -e "${BLUE}📋 Admin Service (Port 8085)${NC}"
echo "   ⚠️  Admin service requires JWT authentication"
echo "   Attempting to get JWT token..."

# Try to get JWT token for admin service
admin_token=$(curl -s -X POST http://localhost:8081/api/v1/auth/login \
    -H 'Content-Type: application/json' \
    -d '{"email":"admin@enterprise-rag.com","password":"admin123"}' 2>/dev/null | \
    grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [[ -n "$admin_token" ]]; then
    echo -e "   ${GREEN}✅ JWT Token obtained!${NC}"
    echo "   Token: $admin_token"
    echo ""
    echo "   To use in Swagger UI:"
    echo "   1. Open Admin Swagger UI"
    echo "   2. Click 'Authorize' button"
    echo "   3. Enter: Bearer $admin_token"
    echo ""
else
    echo -e "   ${YELLOW}⚠️  Could not auto-obtain JWT token${NC}"
    echo "   Manual steps:"
    echo "   1. Login via: curl -X POST http://localhost:8081/api/v1/auth/login \\"
    echo "        -H 'Content-Type: application/json' \\"
    echo "        -d '{\"email\":\"admin@enterprise-rag.com\",\"password\":\"admin123\"}'"
    echo "   2. Copy the 'token' from response"
    echo "   3. In Admin Swagger UI, click 'Authorize'"
    echo "   4. Enter: Bearer <your-token>"
    echo ""
fi

echo -e "${YELLOW}💡 TIP: Keep this terminal window open to reference passwords${NC}"
echo ""

# Ask user if they want to open browsers
read -p "Open Swagger UIs in browser? (y/n) " -n 1 -r
echo ""

if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo ""
    echo "🌐 Opening Swagger UIs..."

    # Open Document Service (public)
    echo "Opening Document Service (public access)..."
    open http://localhost:8082/swagger-ui.html 2>/dev/null || \
    xdg-open http://localhost:8082/swagger-ui.html 2>/dev/null || \
    echo "Could not auto-open browser. Visit: http://localhost:8082/swagger-ui.html"
    sleep 1

    # Open Embedding Service
    if curl -s http://localhost:8083/actuator/health >/dev/null 2>&1; then
        echo "Opening Embedding Service..."
        open http://localhost:8083/swagger-ui.html 2>/dev/null || \
        xdg-open http://localhost:8083/swagger-ui.html 2>/dev/null || true
        sleep 1
    fi

    # Open Core Service
    if curl -s http://localhost:8084/actuator/health >/dev/null 2>&1; then
        echo "Opening Core Service..."
        open http://localhost:8084/swagger-ui.html 2>/dev/null || \
        xdg-open http://localhost:8084/swagger-ui.html 2>/dev/null || true
        sleep 1
    fi

    # Open Admin Service
    if curl -s http://localhost:8085/admin/api/actuator/health >/dev/null 2>&1; then
        echo "Opening Admin Service..."
        open http://localhost:8085/admin/api/swagger-ui.html 2>/dev/null || \
        xdg-open http://localhost:8085/admin/api/swagger-ui.html 2>/dev/null || true
    fi

    echo ""
    echo -e "${GREEN}✅ Browser windows opened!${NC}"
    echo ""
    echo "When prompted for login:"
    echo "  • Username: user"
    echo "  • Password: (see credentials above for each service)"
fi

echo ""
echo "📖 Quick Reference:"
echo "   • Document Service: Public access, no login needed"
echo "   • Other services: Use 'user' + password shown above"
echo "   • Passwords change on service restart"
echo ""
echo "🔄 To refresh passwords after restart:"
echo "   ./scripts/utils/launch-swagger.sh"
echo ""

#!/bin/bash

# Admin Service Login Helper
# This script handles JWT authentication for the Admin service Swagger UI

set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo "🔐 Admin Service Login Helper"
echo "=============================="
echo ""

# Check if Auth service is running
if ! curl -s http://localhost:8081/actuator/health >/dev/null 2>&1; then
    echo -e "${RED}❌ Auth service not running on port 8081${NC}"
    echo "Start services first with: docker-compose up -d"
    exit 1
fi

# Check if Admin service is running
if ! curl -s http://localhost:8085/admin/api/actuator/health >/dev/null 2>&1; then
    echo -e "${RED}❌ Admin service not running on port 8085${NC}"
    echo "Start services first with: docker-compose up -d"
    exit 1
fi

echo -e "${GREEN}✅ Services are running${NC}"
echo ""

# Try to create admin user first (in case it doesn't exist)
echo "Checking for admin user..."
create_response=$(curl -s -X POST http://localhost:8081/api/v1/auth/register \
    -H 'Content-Type: application/json' \
    -d '{
        "email": "admin@enterprise-rag.com",
        "password": "admin12345",
        "firstName": "System",
        "lastName": "Administrator",
        "tenantName": "Default Tenant"
    }' 2>/dev/null || echo "")

if echo "$create_response" | grep -q "email"; then
    echo -e "${GREEN}✅ Admin user created or already exists${NC}"
elif echo "$create_response" | grep -q "error"; then
    echo -e "${YELLOW}⚠️  User may already exist (this is fine)${NC}"
fi

echo ""
echo "Attempting login..."

# Try to login and get JWT token
login_response=$(curl -s -X POST http://localhost:8081/api/v1/auth/login \
    -H 'Content-Type: application/json' \
    -d '{
        "email": "admin@enterprise-rag.com",
        "password": "admin12345"
    }' 2>/dev/null)

# Extract token from response (try different JSON formats)
token=$(echo "$login_response" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
if [[ -z "$token" ]]; then
    token=$(echo "$login_response" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
fi
if [[ -z "$token" ]]; then
    token=$(echo "$login_response" | jq -r '.token // .accessToken // empty' 2>/dev/null)
fi

if [[ -n "$token" && "$token" != "null" ]]; then
    echo -e "${GREEN}✅ Login successful!${NC}"
    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${YELLOW}📋 JWT Token (copy this):${NC}"
    echo ""
    echo "$token"
    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""

    # Try to copy to clipboard
    if command -v pbcopy &> /dev/null; then
        echo -n "$token" | pbcopy
        echo -e "${GREEN}✅ Token copied to clipboard!${NC}"
        echo ""
    elif command -v xclip &> /dev/null; then
        echo -n "$token" | xclip -selection clipboard
        echo -e "${GREEN}✅ Token copied to clipboard!${NC}"
        echo ""
    elif command -v xsel &> /dev/null; then
        echo -n "$token" | xsel --clipboard
        echo -e "${GREEN}✅ Token copied to clipboard!${NC}"
        echo ""
    fi

    echo "🌐 How to use in Admin Swagger UI:"
    echo ""
    echo "1. Open: http://localhost:8085/admin/api/swagger-ui.html"
    echo "2. Click the 'Authorize' button (🔒 icon)"
    echo "3. In the 'Value' field, enter:"
    echo ""
    echo -e "   ${YELLOW}Bearer $token${NC}"
    echo ""
    echo "4. Click 'Authorize' then 'Close'"
    echo "5. Now you can test API endpoints!"
    echo ""

    # Ask if user wants to open Swagger UI
    read -p "Open Admin Swagger UI now? (y/n) " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "Opening Admin Swagger UI..."
        open http://localhost:8085/admin/api/swagger-ui.html 2>/dev/null || \
        xdg-open http://localhost:8085/admin/api/swagger-ui.html 2>/dev/null || \
        echo "Could not auto-open browser. Visit: http://localhost:8085/admin/api/swagger-ui.html"
    fi

else
    echo -e "${RED}❌ Login failed${NC}"
    echo ""
    echo "Response from server:"
    echo "$login_response" | jq . 2>/dev/null || echo "$login_response"
    echo ""
    echo "Troubleshooting:"
    echo "1. Make sure admin user exists:"
    echo "   ./scripts/db/create-admin-user.sh"
    echo ""
    echo "2. Check Auth service logs:"
    echo "   docker logs rag-auth"
    echo ""
    echo "3. Verify database connection:"
    echo "   docker exec postgres psql -U rag_user -d rag_enterprise -c 'SELECT email FROM users;'"
    exit 1
fi

echo ""
echo "💡 Tips:"
echo "   • Token expires after 24 hours"
echo "   • Run this script again to get a new token"
echo "   • Store token securely if using in scripts/automation"
echo ""

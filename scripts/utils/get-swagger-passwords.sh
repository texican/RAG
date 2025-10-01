#!/bin/bash

# Get Current Swagger UI Passwords
# This script retrieves the current auto-generated passwords for all RAG services
# Run this after service restarts to get updated credentials

echo "🔑 RAG System - Current Swagger UI Passwords"
echo "=============================================="
echo ""
echo "⚠️  Note: These passwords change every time services restart!"
echo ""

# Function to get password from service logs
get_service_password() {
    local service_name=$1
    local display_name=$2
    local port=$3
    local path=$4
    
    echo "📋 $display_name (Port $port):"
    echo "   URL: http://localhost:$port$path"
    
    # Check if container is running
    if docker ps --format "table {{.Names}}" | grep -q "^$service_name$"; then
        # Get password from logs
        password=$(docker logs $service_name 2>&1 | grep "Using generated security password" | tail -1 | sed 's/.*Using generated security password: //')
        
        if [ -n "$password" ]; then
            echo "   Username: user"
            echo "   Password: $password"
            echo "   Status: ✅ Available"
        else
            echo "   Status: ❌ No generated password found"
        fi
    else
        echo "   Status: ❌ Container not running"
    fi
    echo ""
}

# Check public services first
echo "🌐 PUBLIC ACCESS (No Authentication Required):"
echo "📋 Document Service (Port 8082):"
echo "   URL: http://localhost:8082/swagger-ui.html"
echo "   Status: ✅ Public Access"
echo ""

echo "🔐 AUTHENTICATED ACCESS (Generated Passwords):"

# Get passwords for all authenticated services (Gateway bypassed per ADR-001)
get_service_password "rag-embedding" "Embedding Service" "8083" "/swagger-ui.html"
get_service_password "rag-core" "Core Service" "8084" "/swagger-ui.html"
get_service_password "rag-admin" "Admin Service" "8085" "/admin/api/swagger-ui.html"

# Check Auth Service (special case)
echo "📋 Auth Service (Port 8081):"
echo "   URL: http://localhost:8081/swagger-ui.html"
if docker ps --format "table {{.Names}}" | grep -q "^rag-auth$"; then
    auth_password=$(docker logs rag-auth 2>&1 | grep "Using generated security password" | tail -1 | sed 's/.*Using generated security password: //')
    if [ -n "$auth_password" ]; then
        echo "   Username: user"
        echo "   Password: $auth_password"
        echo "   Status: ✅ Available"
    else
        echo "   Status: ⚠️  No generated password (may use different auth)"
    fi
else
    echo "   Status: ❌ Container not running"
fi
echo ""

echo "💡 USAGE TIPS:"
echo "   • Copy passwords from above and paste into Swagger UI login forms"
echo "   • Run this script again after restarting any services"
echo "   • Start with Document Service (public access) for immediate testing"
echo ""

echo "🔄 TO UPDATE AFTER RESTART:"
echo "   ./scripts/utils/get-swagger-passwords.sh"
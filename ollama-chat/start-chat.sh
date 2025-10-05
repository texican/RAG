#!/bin/bash

# Ollama Chat Frontend Startup Script
# Supports both Direct Ollama mode and RAG-Enhanced mode

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVER_PORT=${CHAT_PORT:-8888}
RAG_MODE=${RAG_MODE:-disabled}
RAG_CORE_URL=${RAG_CORE_URL:-http://localhost:8084}
RAG_AUTH_URL=${RAG_AUTH_URL:-http://localhost:8081}

echo "🦙 Starting Ollama Chat Frontend..."
echo "===================================="
echo "Mode: ${RAG_MODE}"
echo "Port: ${SERVER_PORT}"
echo ""

# Function to check Ollama connection with multiple URLs
check_ollama_connection() {
    local urls=("http://localhost:11434" "http://rag-ollama:11434")
    local found_ollama=false
    
    echo "🔍 Checking Ollama connection..."
    
    for url in "${urls[@]}"; do
        echo "   Trying $url..."
        if curl -s "$url/api/tags" > /dev/null 2>&1; then
            echo "✅ Ollama found at $url"
            
            # Check if models are available
            model_count=$(curl -s "$url/api/tags" | jq -r '.models | length' 2>/dev/null || echo "0")
            if [ "$model_count" -gt 0 ]; then
                echo "📊 Found $model_count available model(s)"
            else
                echo "⚠️  No models found - you'll need to pull some models"
                echo ""
                echo "   Quick recommendations:"
                echo "   • Small & Fast (1.3GB):  docker-compose exec ollama ollama pull llama3.2:1b"
                echo "   • Balanced (4.7GB):      docker-compose exec ollama ollama pull llama3.2:3b"
                echo "   • High Quality (4.7GB):  docker-compose exec ollama ollama pull llama2:7b-chat"
                echo "   • Code Focused (3.8GB):  docker-compose exec ollama ollama pull codellama:7b"
            fi
            
            export OLLAMA_URL="$url"
            found_ollama=true
            break
        fi
    done
    
    if [ "$found_ollama" = false ]; then
        echo "❌ Cannot connect to Ollama at any known location"
        echo ""
        echo "🔧 Troubleshooting steps:"
        echo "   1. Check if BYO RAG Docker environment is running:"
        echo "      docker-compose ps ollama"
        echo ""
        echo "   2. Check Ollama container logs:"
        echo "      docker-compose logs ollama"
        echo ""
        echo "   3. Try starting the RAG system:"
        echo "      ./scripts/utils/quick-start.sh"
        echo ""
        echo "   4. Manual health check:"
        echo "      ./scripts/utils/health-check.sh"
        echo ""
        return 1
    fi
    
    return 0
}

# Check RAG services if RAG mode is enabled
check_rag_services() {
    echo "🎯 Checking RAG SpecKit services..."

    local all_ok=true

    # Check Auth Service
    if curl -s "${RAG_AUTH_URL}/actuator/health" > /dev/null 2>&1; then
        echo "   ✅ Auth Service (8081)"
    else
        echo "   ❌ Auth Service (8081) - Not available"
        all_ok=false
    fi

    # Check Core Service
    if curl -s "${RAG_CORE_URL}/actuator/health" > /dev/null 2>&1; then
        echo "   ✅ RAG Core Service (8084)"
    else
        echo "   ❌ RAG Core Service (8084) - Not available"
        all_ok=false
    fi

    # Check Document Service
    if curl -s "http://localhost:8082/actuator/health" > /dev/null 2>&1; then
        echo "   ✅ Document Service (8082)"
    else
        echo "   ⚠️  Document Service (8082) - Optional"
    fi

    # Check Embedding Service
    if curl -s "http://localhost:8083/actuator/health" > /dev/null 2>&1; then
        echo "   ✅ Embedding Service (8083)"
    else
        echo "   ⚠️  Embedding Service (8083) - Optional"
    fi

    if [ "$all_ok" = false ]; then
        echo ""
        echo "❌ Required RAG services are not available"
        echo ""
        echo "🔧 To start RAG services:"
        echo "   cd .. && docker-compose up -d"
        echo ""
        echo "Or disable RAG mode:"
        echo "   unset RAG_MODE"
        echo "   ./start-chat.sh"
        return 1
    fi

    echo ""
    echo "✅ All required RAG services are available"
    return 0
}

# Check Ollama connection before proceeding
if ! check_ollama_connection; then
    exit 1
fi

# Check RAG services if enabled
if [ "$RAG_MODE" = "enabled" ]; then
    echo ""
    if ! check_rag_services; then
        exit 1
    fi
fi

# Check if server is already running
if lsof -Pi :$SERVER_PORT -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo "⚠️  Chat server already running on port $SERVER_PORT"
    echo "   You can access it at: http://localhost:$SERVER_PORT"
    echo ""
    echo "🛑 To stop the existing server:"
    echo "   pkill -f 'python.*server.py' || lsof -ti :$SERVER_PORT | xargs kill"
    exit 0
fi

# Start the server
echo ""
echo "🚀 Starting chat server on port $SERVER_PORT..."
cd "$SCRIPT_DIR"

# Set environment variables for the Python server
export CHAT_PORT="$SERVER_PORT"

# Start server in background
python3 server.py > chat-server.log 2>&1 &
SERVER_PID=$!

# Wait a moment for server to start
sleep 3

# Check if server started successfully
if kill -0 $SERVER_PID 2>/dev/null; then
    echo "✅ Chat server started successfully!"
    echo ""
    echo "🌐 Access your Ollama Chat:"
    echo "   👉 Direct Chat:      http://localhost:$SERVER_PORT/index.html"
    if [ "$RAG_MODE" = "enabled" ]; then
        echo "   👉 RAG Integration:  http://localhost:$SERVER_PORT/rag-integration-example.html"
    else
        echo "   👉 RAG Demo:         http://localhost:$SERVER_PORT/rag-integration-example.html"
    fi
    echo ""

    if [ "$RAG_MODE" = "enabled" ]; then
        echo "🎯 RAG Mode Features:"
        echo "   ✅ Document-aware responses"
        echo "   ✅ Source citations"
        echo "   ✅ Multi-tenant support"
        echo "   ✅ Authentication integration"
        echo ""
    else
        echo "📋 Direct Ollama Features:"
        echo "   ✅ Automatic Docker/localhost Ollama detection"
        echo "   ✅ Real-time model discovery and loading"
        echo "   ✅ Enhanced error handling and retry logic"
        echo "   ✅ Connection health monitoring"
        echo "   ✅ Mobile-friendly responsive design"
        echo ""
        echo "💡 To enable RAG mode:"
        echo "   export RAG_MODE=enabled"
        echo "   export RAG_CORE_URL=http://localhost:8084"
        echo "   export RAG_AUTH_URL=http://localhost:8081"
        echo "   ./start-chat.sh"
        echo ""
    fi

    echo "🛑 To stop the server later:"
    echo "   kill $SERVER_PID"
    echo "   or use: pkill -f 'python.*server.py'"
    echo ""
    echo "📝 Server logs: $SCRIPT_DIR/chat-server.log"
    echo "📊 Environment:"
    echo "   OLLAMA_URL:    ${OLLAMA_URL:-auto-detect}"
    echo "   RAG_MODE:      ${RAG_MODE}"
    if [ "$RAG_MODE" = "enabled" ]; then
        echo "   RAG_CORE_URL:  ${RAG_CORE_URL}"
        echo "   RAG_AUTH_URL:  ${RAG_AUTH_URL}"
    fi
    
    # Try to open browser (optional)
    if command -v open >/dev/null 2>&1; then
        echo ""
        echo "🔗 Opening browser..."
        open "http://localhost:$SERVER_PORT" 2>/dev/null || true
    fi
    
    echo ""
    echo "🎉 Ollama Chat is ready! Happy chatting!"
    
else
    echo "❌ Failed to start chat server"
    echo ""
    echo "🔍 Debug information:"
    echo "   Server PID: $SERVER_PID (not running)"
    echo "   Log file: $SCRIPT_DIR/chat-server.log"
    echo ""
    echo "📋 Recent log entries:"
    if [ -f "$SCRIPT_DIR/chat-server.log" ]; then
        tail -n 10 "$SCRIPT_DIR/chat-server.log" | sed 's/^/   /'
    else
        echo "   No log file found"
    fi
    
    exit 1
fi
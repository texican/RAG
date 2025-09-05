#!/bin/bash

# Ollama Chat Frontend Startup Script

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVER_PORT=${CHAT_PORT:-8888}

echo "🦙 Starting Ollama Chat Frontend..."
echo "=================================="

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
                echo "   Try: docker-compose exec ollama ollama pull llama2:7b-chat"
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

# Check Ollama connection before proceeding
if ! check_ollama_connection; then
    exit 1
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
    echo "🌐 Access your Ollama Chat at:"
    echo "   👉 http://localhost:$SERVER_PORT"
    echo ""
    echo "📋 Enhanced Features:"
    echo "   ✅ Automatic Docker/localhost Ollama detection"
    echo "   ✅ Real-time model discovery and loading"
    echo "   ✅ Enhanced error handling and retry logic"
    echo "   ✅ Connection health monitoring"
    echo "   ✅ Helpful troubleshooting messages"
    echo "   ✅ Mobile-friendly responsive design"
    echo ""
    echo "🛑 To stop the server later:"
    echo "   kill $SERVER_PID"
    echo "   or use: pkill -f 'python.*server.py'"
    echo ""
    echo "📝 Server logs: $SCRIPT_DIR/chat-server.log"
    echo "📊 Environment: OLLAMA_URL=${OLLAMA_URL:-auto-detect}"
    
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
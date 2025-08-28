#!/bin/bash

# Ollama Chat Frontend Startup Script

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVER_PORT=8888

echo "🦙 Starting Ollama Chat Frontend..."
echo "=================================="

# Check if Ollama is running
echo "🔍 Checking Ollama connection..."
if curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
    echo "✅ Ollama is running and accessible"
else
    echo "❌ Ollama is not accessible at http://localhost:11434"
    echo "   Make sure your RAG system is running:"
    echo "   ./scripts/utils/health-check.sh"
    exit 1
fi

# Check if server is already running
if lsof -Pi :$SERVER_PORT -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo "⚠️  Chat server already running on port $SERVER_PORT"
    echo "   You can access it at: http://localhost:$SERVER_PORT"
    exit 0
fi

# Start the server
echo "🚀 Starting chat server on port $SERVER_PORT..."
cd "$SCRIPT_DIR"

# Start server in background
python3 server.py > chat-server.log 2>&1 &
SERVER_PID=$!

# Wait a moment for server to start
sleep 2

# Check if server started successfully
if kill -0 $SERVER_PID 2>/dev/null; then
    echo "✅ Chat server started successfully!"
    echo ""
    echo "🌐 Access your Ollama Chat at:"
    echo "   👉 http://localhost:$SERVER_PORT"
    echo ""
    echo "📋 Features:"
    echo "   • Modern chat interface"
    echo "   • Model selection dropdown"
    echo "   • Real-time responses"
    echo "   • Mobile-friendly design"
    echo ""
    echo "🛑 To stop the server later:"
    echo "   kill $SERVER_PID"
    echo "   or use: pkill -f server.py"
    echo ""
    echo "📝 Server logs: $SCRIPT_DIR/chat-server.log"
    
    # Try to open browser (optional)
    if command -v open >/dev/null 2>&1; then
        echo "🔗 Opening browser..."
        open "http://localhost:$SERVER_PORT" 2>/dev/null || true
    fi
else
    echo "❌ Failed to start chat server"
    echo "Check chat-server.log for details"
    exit 1
fi
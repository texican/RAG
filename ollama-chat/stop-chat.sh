#!/bin/bash

# Ollama Chat Frontend Stop Script
# Safely stops the chat server

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVER_PORT=${CHAT_PORT:-8888}

echo "🛑 Stopping Ollama Chat Frontend..."
echo "===================================="
echo ""

# Function to check if server is running
check_server_running() {
    if lsof -Pi :$SERVER_PORT -sTCP:LISTEN -t >/dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

# Function to get PID of server
get_server_pid() {
    lsof -ti :$SERVER_PORT 2>/dev/null || echo ""
}

# Check if server is running
if ! check_server_running; then
    echo "ℹ️  No chat server found running on port $SERVER_PORT"
    echo ""
    echo "🔍 Checking for any Python server processes..."

    # Check for any python server.py processes
    if pgrep -f "python.*server.py" > /dev/null 2>&1; then
        echo ""
        echo "⚠️  Found Python server processes running:"
        ps aux | grep "python.*server.py" | grep -v grep | sed 's/^/   /'
        echo ""
        read -p "Do you want to stop these? (y/N): " -n 1 -r
        echo

        if [[ $REPLY =~ ^[Yy]$ ]]; then
            pkill -f "python.*server.py"
            echo "✅ Stopped all Python server processes"
        else
            echo "❌ No action taken"
        fi
    else
        echo "   No Python server processes found"
    fi

    exit 0
fi

# Get server PID
SERVER_PID=$(get_server_pid)

if [ -z "$SERVER_PID" ]; then
    echo "❌ Could not determine server PID"
    exit 1
fi

echo "📊 Server Information:"
echo "   Port: $SERVER_PORT"
echo "   PID:  $SERVER_PID"
echo ""

# Show server process details
echo "🔍 Process Details:"
ps -p $SERVER_PID -o pid,ppid,user,start,time,command | sed 's/^/   /'
echo ""

# Ask for confirmation (optional - comment out for auto-stop)
read -p "Stop this server? (Y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Nn]$ ]]; then
    echo "❌ Cancelled - server still running"
    exit 0
fi

# Stop the server gracefully
echo "🔄 Stopping server (PID: $SERVER_PID)..."

if kill $SERVER_PID 2>/dev/null; then
    # Wait for graceful shutdown
    sleep 1

    # Check if still running
    if kill -0 $SERVER_PID 2>/dev/null; then
        echo "⚠️  Server still running, forcing shutdown..."
        kill -9 $SERVER_PID 2>/dev/null || true
        sleep 1
    fi

    # Verify server stopped
    if check_server_running; then
        echo "❌ Failed to stop server on port $SERVER_PORT"
        echo ""
        echo "🔧 Manual cleanup:"
        echo "   lsof -ti :$SERVER_PORT | xargs kill -9"
        exit 1
    else
        echo "✅ Chat server stopped successfully!"
        echo ""

        # Clean up log file (optional)
        if [ -f "$SCRIPT_DIR/chat-server.log" ]; then
            LOG_SIZE=$(du -h "$SCRIPT_DIR/chat-server.log" | cut -f1)
            echo "📝 Server log: $SCRIPT_DIR/chat-server.log ($LOG_SIZE)"
            echo ""
            read -p "Delete log file? (y/N): " -n 1 -r
            echo

            if [[ $REPLY =~ ^[Yy]$ ]]; then
                rm -f "$SCRIPT_DIR/chat-server.log"
                echo "   ✅ Log file deleted"
            else
                echo "   ℹ️  Log file preserved"
            fi
        fi

        echo ""
        echo "💡 To start the server again:"
        echo "   ./start-chat.sh"
        echo ""
        echo "🎉 Goodbye!"
    fi
else
    echo "❌ Failed to send stop signal to server"
    echo ""
    echo "🔧 Try manual cleanup:"
    echo "   pkill -f 'python.*server.py'"
    echo "   or"
    echo "   lsof -ti :$SERVER_PORT | xargs kill -9"
    exit 1
fi

#!/bin/bash

# Stop all RAG services
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

echo "Stopping all RAG services..."

# Find and kill all service processes
for pidfile in "${PROJECT_ROOT}"/logs/*.pid; do
    if [[ -f "$pidfile" ]]; then
        service_name=$(basename "$pidfile" .pid)
        pid=$(cat "$pidfile")
        
        if kill -0 "$pid" 2>/dev/null; then
            echo "Stopping $service_name (PID: $pid)..."
            kill "$pid"
            rm "$pidfile"
            echo "✅ $service_name stopped"
        else
            echo "⚠️  $service_name was not running"
            rm "$pidfile"
        fi
    fi
done

echo "🛑 All services stopped"

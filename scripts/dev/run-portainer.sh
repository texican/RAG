#!/bin/bash

# Script to run Portainer locally for Docker container management
# Portainer provides a web UI for managing Docker containers, images, networks, and volumes

set -e

PORTAINER_CONTAINER="portainer"
PORTAINER_PORT="9000"
PORTAINER_VOLUME="portainer_data"

echo "🐳 Starting Portainer locally..."

# Check if Portainer container already exists
if docker ps -a --format '{{.Names}}' | grep -q "^${PORTAINER_CONTAINER}$"; then
    echo "📦 Portainer container already exists"

    # Check if it's running
    if docker ps --format '{{.Names}}' | grep -q "^${PORTAINER_CONTAINER}$"; then
        echo "✅ Portainer is already running"
        echo "🌐 Access Portainer at: http://localhost:${PORTAINER_PORT}"
        exit 0
    else
        echo "▶️  Starting existing Portainer container..."
        docker start ${PORTAINER_CONTAINER}
    fi
else
    echo "📥 Creating new Portainer container..."

    # Create Portainer volume if it doesn't exist
    if ! docker volume ls | grep -q "${PORTAINER_VOLUME}"; then
        docker volume create ${PORTAINER_VOLUME}
        echo "✅ Created volume: ${PORTAINER_VOLUME}"
    fi

    # Run Portainer container
    docker run -d \
        --name ${PORTAINER_CONTAINER} \
        -p ${PORTAINER_PORT}:9000 \
        -v /var/run/docker.sock:/var/run/docker.sock \
        -v ${PORTAINER_VOLUME}:/data \
        --restart unless-stopped \
        portainer/portainer-ce:latest

    echo "✅ Portainer container created and started"
fi

echo ""
echo "🎉 Portainer is now running!"
echo "🌐 Access the web interface at: http://localhost:${PORTAINER_PORT}"
echo ""
echo "📝 First time setup:"
echo "   1. Open http://localhost:${PORTAINER_PORT} in your browser"
echo "   2. Create an admin account (username and password)"
echo "   3. Select 'Docker' as the environment to manage"
echo ""
echo "🛑 To stop Portainer: docker stop ${PORTAINER_CONTAINER}"
echo "🔄 To restart Portainer: docker start ${PORTAINER_CONTAINER}"
echo "🗑️  To remove Portainer: docker rm -f ${PORTAINER_CONTAINER}"

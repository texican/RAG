#!/bin/bash

# Docker wrapper to guide developers to use proper tools
# To enable, add to ~/.bashrc or ~/.zshrc:
#   alias docker='~/path/to/RAG/scripts/utils/docker-wrapper.sh'

YELLOW='\033[1;33m'
NC='\033[0m'

# Get the actual docker command
DOCKER_CMD=$(which -a docker | grep -v "$(readlink -f "$0")" | head -n 1)

# Detect if we're in the RAG project
if [[ "$PWD" =~ "RAG_SpecKit/RAG" ]]; then
    # Check for problematic commands
    case "$1" in
        build)
            echo -e "${YELLOW}⚠️  Direct 'docker build' detected in RAG project${NC}"
            echo ""
            echo "Consider using the rebuild script instead:"
            echo "  make rebuild SERVICE=rag-auth"
            echo ""
            read -p "Continue with docker build anyway? [y/N] " -n 1 -r
            echo
            if [[ ! $REPLY =~ ^[Yy]$ ]]; then
                exit 1
            fi
            ;;
        restart)
            if [[ "$2" =~ ^rag- ]]; then
                echo -e "${YELLOW}⚠️  'docker restart' doesn't reload the image!${NC}"
                echo ""
                echo "If you made code changes, use:"
                echo "  make rebuild SERVICE=$2"
                echo ""
                echo "If you just want to restart (no code changes):"
                echo "  docker stop $2 && docker start $2"
                echo ""
                read -p "Continue with restart anyway? [y/N] " -n 1 -r
                echo
                if [[ ! $REPLY =~ ^[Yy]$ ]]; then
                    exit 1
                fi
            fi
            ;;
    esac
fi

# Execute the actual docker command
exec "$DOCKER_CMD" "$@"

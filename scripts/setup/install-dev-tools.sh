#!/bin/bash

# Install development tools and setup for RAG project
# Run once after cloning the repository

set -e

BLUE='\033[0;34m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}🔧 RAG Development Tools Installer${NC}"
echo "===================================="
echo ""

# 1. Check prerequisites
echo -e "${YELLOW}Checking prerequisites...${NC}"

MISSING=""

if ! command -v docker &> /dev/null; then
    MISSING="${MISSING}\n  - Docker"
fi

if ! command -v docker-compose &> /dev/null; then
    MISSING="${MISSING}\n  - Docker Compose"
fi

if ! command -v mvn &> /dev/null; then
    MISSING="${MISSING}\n  - Maven"
fi

if ! command -v java &> /dev/null; then
    MISSING="${MISSING}\n  - Java (JDK 21)"
fi

if [ ! -z "$MISSING" ]; then
    echo -e "${YELLOW}⚠️  Missing prerequisites:${NC}"
    echo -e "$MISSING"
    echo ""
    echo "Please install these tools before continuing."
    exit 1
fi

echo -e "${GREEN}✅ All prerequisites found${NC}"
echo ""

# 2. Install git hooks
echo -e "${YELLOW}Installing git hooks...${NC}"

if [ -d .git ]; then
    # Install pre-push hook
    if [ ! -f .git/hooks/pre-push ]; then
        ln -sf ../../.githooks/pre-push .git/hooks/pre-push
        chmod +x .git/hooks/pre-push
        echo -e "${GREEN}✅ Installed pre-push hook${NC}"
    else
        echo -e "${YELLOW}⚠️  pre-push hook already exists, skipping${NC}"
    fi
else
    echo -e "${YELLOW}⚠️  Not a git repository, skipping git hooks${NC}"
fi
echo ""

# 3. Offer to install docker wrapper
echo -e "${YELLOW}Docker wrapper setup (optional)${NC}"
echo "The Docker wrapper warns you when using problematic commands."
echo ""
read -p "Install Docker wrapper alias? [y/N] " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    WRAPPER_PATH="$(pwd)/scripts/utils/docker-wrapper.sh"

    # Detect shell
    if [ ! -z "$ZSH_VERSION" ]; then
        SHELL_RC="$HOME/.zshrc"
    elif [ ! -z "$BASH_VERSION" ]; then
        SHELL_RC="$HOME/.bashrc"
    else
        echo -e "${YELLOW}⚠️  Unknown shell, please add manually${NC}"
        SHELL_RC=""
    fi

    if [ ! -z "$SHELL_RC" ]; then
        # Check if already added
        if grep -q "docker-wrapper.sh" "$SHELL_RC"; then
            echo -e "${YELLOW}⚠️  Docker wrapper already in $SHELL_RC${NC}"
        else
            echo "" >> "$SHELL_RC"
            echo "# RAG Project Docker Wrapper" >> "$SHELL_RC"
            echo "alias docker='$WRAPPER_PATH'" >> "$SHELL_RC"
            echo -e "${GREEN}✅ Added Docker wrapper to $SHELL_RC${NC}"
            echo -e "${YELLOW}⚠️  Run 'source $SHELL_RC' or restart your terminal${NC}"
        fi
    fi
else
    echo -e "${YELLOW}Skipped Docker wrapper${NC}"
fi
echo ""

# 4. Verify Makefile
echo -e "${YELLOW}Verifying Makefile...${NC}"
if [ ! -f Makefile ]; then
    echo -e "${RED}❌ Makefile not found!${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Makefile found${NC}"
echo ""

# 5. Verify rebuild script
echo -e "${YELLOW}Verifying rebuild script...${NC}"
if [ ! -x scripts/dev/rebuild-service.sh ]; then
    if [ -f scripts/dev/rebuild-service.sh ]; then
        chmod +x scripts/dev/rebuild-service.sh
        echo -e "${GREEN}✅ Made rebuild script executable${NC}"
    else
        echo -e "${RED}❌ rebuild-service.sh not found!${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}✅ Rebuild script is executable${NC}"
fi
echo ""

# 6. Test make commands
echo -e "${YELLOW}Testing make commands...${NC}"
if make help > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Make commands working${NC}"
else
    echo -e "${RED}❌ Make commands failed${NC}"
    exit 1
fi
echo ""

# 7. Create .env file if it doesn't exist
echo -e "${YELLOW}Checking .env file...${NC}"
if [ ! -f config/docker/.env ]; then
    echo -e "${YELLOW}Creating default .env file...${NC}"
    cat > config/docker/.env << 'EOF'
# RAG System Environment Configuration

# Spring Profile
SPRING_PROFILES_ACTIVE=dev

# Database Configuration
DB_HOST=rag-postgres
POSTGRES_DB=rag_enterprise
POSTGRES_USER=rag_user
POSTGRES_PASSWORD=rag_password

# Redis Configuration
REDIS_HOST=rag-redis
REDIS_PASSWORD=redis_password

# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=kafka:29092

# JWT Configuration
JWT_SECRET=your-secret-key-change-in-production-min-256-bits

# Ollama Configuration
OLLAMA_URL=http://rag-ollama:11434

# Embedding Service
EMBEDDING_SERVICE_URL=http://rag-embedding:8083
EOF
    echo -e "${GREEN}✅ Created config/docker/.env${NC}"
    echo -e "${YELLOW}⚠️  Remember to update JWT_SECRET for production!${NC}"
else
    echo -e "${GREEN}✅ .env file already exists${NC}"
fi
echo ""

# 8. Summary
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}✅ Development tools installed!${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${YELLOW}📖 Next steps:${NC}"
echo ""
echo "1. Read the documentation:"
echo "   - CONTRIBUTING.md - Development workflow"
echo "   - docs/DOCKER_DEVELOPMENT.md - Docker guide"
echo "   - CLAUDE.md - Current project state"
echo ""
echo "2. Build and start services:"
echo "   make build-all"
echo "   make start"
echo ""
echo "3. Check status:"
echo "   make status"
echo ""
echo "4. See all commands:"
echo "   make help"
echo ""
echo -e "${GREEN}Happy coding! 🚀${NC}"

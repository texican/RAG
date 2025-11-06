#!/bin/bash
set -euo pipefail

##############################################################################
# Create Local Development .env from Secret Manager
#
# This script retrieves secrets from Google Secret Manager and creates a
# local .env file for development/testing purposes.
#
# Prerequisites:
# - GCP-SECRETS-002 complete (secrets migrated to Secret Manager)
# - gcloud authenticated with sufficient permissions
# - roles/secretmanager.secretAccessor on required secrets
#
# Usage:
#   ./scripts/gcp/06-create-local-env.sh
#
##############################################################################

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PROJECT_ID=$(gcloud config get-value project)
ENV_FILE="$PROJECT_ROOT/.env"

echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║        Create Local .env from Secret Manager                  ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${BLUE}Project:${NC} $PROJECT_ID"
echo ""

##############################################################################
# Step 1: Check Prerequisites
##############################################################################

echo -e "${YELLOW}Step 1: Checking prerequisites...${NC}"

# Check if Secret Manager API is enabled
if ! gcloud services list --enabled --filter="name:secretmanager.googleapis.com" --format="value(name)" | grep -q secretmanager; then
  echo -e "${RED}✗ Secret Manager API is not enabled${NC}"
  exit 1
fi
echo -e "${GREEN}✓ Secret Manager API enabled${NC}"

# Check if .env already exists
if [ -f "$ENV_FILE" ]; then
  echo -e "${YELLOW}⚠️  .env file already exists${NC}"
  read -p "Overwrite? (y/N): " overwrite
  if [[ ! "$overwrite" =~ ^[Yy]$ ]]; then
    echo "Aborted."
    exit 0
  fi
fi

echo ""

##############################################################################
# Step 2: Retrieve Secrets
##############################################################################

echo -e "${YELLOW}Step 2: Retrieving secrets from Secret Manager...${NC}"

# Function to retrieve secret
get_secret() {
  local secret_name=$1
  local value

  if ! value=$(gcloud secrets versions access latest --secret="$secret_name" 2>/dev/null); then
    echo -e "${RED}✗ Failed to retrieve: $secret_name${NC}"
    echo "  Make sure the secret exists and you have permission to access it."
    exit 1
  fi

  echo "$value"
}

# Retrieve all secrets
POSTGRES_PASSWORD=$(get_secret "postgres-password")
echo -e "${GREEN}✓ Retrieved postgres-password${NC}"

REDIS_PASSWORD=$(get_secret "redis-password")
echo -e "${GREEN}✓ Retrieved redis-password${NC}"

JWT_SECRET=$(get_secret "jwt-secret")
echo -e "${GREEN}✓ Retrieved jwt-secret${NC}"

OPENAI_API_KEY=$(get_secret "openai-api-key")
echo -e "${GREEN}✓ Retrieved openai-api-key${NC}"

echo ""

##############################################################################
# Step 3: Create .env File
##############################################################################

echo -e "${YELLOW}Step 3: Creating .env file...${NC}"

cat > "$ENV_FILE" << EOF
# RAG System Environment Configuration
# Generated from Google Secret Manager on $(date)
# DO NOT COMMIT THIS FILE

# Spring Profile
SPRING_PROFILES_ACTIVE=docker

# Database Configuration
DB_HOST=postgres
POSTGRES_DB=rag_enterprise
POSTGRES_USER=rag_user
POSTGRES_PASSWORD=$POSTGRES_PASSWORD

# Redis Configuration
REDIS_HOST=redis
REDIS_PASSWORD=$REDIS_PASSWORD

# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=kafka:29092

# JWT Configuration (256-bit base64 encoded secret)
JWT_SECRET=$JWT_SECRET

# External Services
OLLAMA_URL=http://ollama:11434
EMBEDDING_SERVICE_URL=http://rag-embedding:8083

# OpenAI API Configuration
OPENAI_API_KEY=$OPENAI_API_KEY
EOF

# Set restrictive permissions
chmod 600 "$ENV_FILE"

echo -e "${GREEN}✓ Created .env file with secrets from Secret Manager${NC}"
echo -e "${GREEN}✓ Set file permissions to 600 (owner read/write only)${NC}"
echo ""

##############################################################################
# Step 4: Verify .gitignore
##############################################################################

echo -e "${YELLOW}Step 4: Verifying .gitignore...${NC}"

GITIGNORE="$PROJECT_ROOT/.gitignore"

if grep -q "^\.env$" "$GITIGNORE" 2>/dev/null; then
  echo -e "${GREEN}✓ .env is in .gitignore${NC}"
else
  echo -e "${RED}✗ .env is NOT in .gitignore${NC}"
  echo "  Add it manually or run: echo '.env' >> .gitignore"
fi

echo ""

##############################################################################
# Step 5: Summary
##############################################################################

echo -e "${GREEN}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║                   ✓ .env File Created                          ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${BLUE}File Location:${NC}"
echo "  $ENV_FILE"
echo ""
echo -e "${BLUE}Secrets Included:${NC}"
echo "  • PostgreSQL password (rotated)"
echo "  • Redis password (rotated)"
echo "  • JWT secret (256-bit, rotated)"
echo "  • OpenAI API key (rotated)"
echo ""
echo -e "${RED}⚠️  SECURITY REMINDERS:${NC}"
echo "  • This file contains production secrets"
echo "  • File permissions set to 600 (owner only)"
echo "  • DO NOT commit this file to git"
echo "  • DO NOT share this file"
echo "  • Delete after testing if not needed"
echo ""
echo -e "${YELLOW}To start services with new secrets:${NC}"
echo "  docker-compose down"
echo "  docker-compose up -d"
echo ""
echo -e "${YELLOW}To regenerate this file later:${NC}"
echo "  ./scripts/gcp/06-create-local-env.sh"
echo ""
echo -e "${GREEN}Local .env file created successfully!${NC}"
echo ""

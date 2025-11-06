#!/bin/bash
set -euo pipefail

##############################################################################
# GCP-SECRETS-002: Migrate Secrets to Google Secret Manager
#
# This script migrates all secrets from .env to Google Secret Manager and
# rotates all credentials for security.
#
# Prerequisites:
# - GCP-INFRA-001 complete (gcloud authenticated, project set)
# - Secret Manager API enabled
# - Service accounts created (02-setup-service-accounts.sh)
#
# SECURITY WARNING:
# - OpenAI API key in .env is COMPROMISED (committed to version control)
# - Must rotate IMMEDIATELY before creating in Secret Manager
#
# Usage:
#   ./scripts/gcp/04-migrate-secrets.sh [--openai-key YOUR_NEW_KEY]
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
GKE_NODE_SA="gke-node-sa@${PROJECT_ID}.iam.gserviceaccount.com"

# Parse arguments
OPENAI_API_KEY=""
while [[ $# -gt 0 ]]; do
  case $1 in
    --openai-key)
      OPENAI_API_KEY="$2"
      shift 2
      ;;
    *)
      echo -e "${RED}Unknown option: $1${NC}"
      exit 1
      ;;
  esac
done

echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║      GCP-SECRETS-002: Secret Manager Migration                ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${BLUE}Project:${NC} $PROJECT_ID"
echo ""

##############################################################################
# Step 1: Verify Prerequisites
##############################################################################

echo -e "${YELLOW}Step 1: Verifying prerequisites...${NC}"

# Check if Secret Manager API is enabled
if ! gcloud services list --enabled --filter="name:secretmanager.googleapis.com" --format="value(name)" | grep -q secretmanager; then
  echo -e "${RED}✗ Secret Manager API is not enabled${NC}"
  echo "Run: gcloud services enable secretmanager.googleapis.com"
  exit 1
fi
echo -e "${GREEN}✓ Secret Manager API enabled${NC}"

# Check if GKE node service account exists
if ! gcloud iam service-accounts list --filter="email:$GKE_NODE_SA" --format="value(email)" | grep -q "$GKE_NODE_SA"; then
  echo -e "${RED}✗ GKE node service account not found: $GKE_NODE_SA${NC}"
  echo "Run: ./scripts/gcp/02-setup-service-accounts.sh"
  exit 1
fi
echo -e "${GREEN}✓ GKE node service account exists${NC}"

echo ""

##############################################################################
# Step 2: Security Warning about Exposed OpenAI Key
##############################################################################

echo -e "${RED}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${RED}║                    ⚠️  SECURITY WARNING ⚠️                     ║${NC}"
echo -e "${RED}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${RED}The OpenAI API key in .env has been COMPROMISED (committed to git).${NC}"
echo -e "${RED}You MUST rotate it before continuing:${NC}"
echo ""
echo -e "${YELLOW}1. Go to: https://platform.openai.com/api-keys${NC}"
echo -e "${YELLOW}2. Create a new API key${NC}"
echo -e "${YELLOW}3. Delete the old key (starts with: sk-proj-A806F0...)${NC}"
echo -e "${YELLOW}4. Pass the new key to this script with --openai-key${NC}"
echo ""

if [ -z "$OPENAI_API_KEY" ]; then
  echo -e "${RED}✗ No OpenAI API key provided${NC}"
  echo ""
  echo "Usage: $0 --openai-key YOUR_NEW_KEY"
  echo ""
  exit 1
fi

# Validate key format
if [[ ! "$OPENAI_API_KEY" =~ ^sk-[a-zA-Z0-9\-_]{20,}$ ]]; then
  echo -e "${RED}✗ Invalid OpenAI API key format${NC}"
  echo "Expected format: sk-..."
  exit 1
fi

echo -e "${GREEN}✓ New OpenAI API key provided${NC}"
echo ""

##############################################################################
# Step 3: Generate New Secrets
##############################################################################

echo -e "${YELLOW}Step 2: Generating new secrets...${NC}"

# Generate JWT secret (256-bit = 32 bytes)
JWT_SECRET=$(openssl rand -base64 32)
echo -e "${GREEN}✓ Generated new JWT secret (256-bit)${NC}"

# Generate database password (192-bit = 24 bytes)
DB_PASSWORD=$(openssl rand -base64 24)
echo -e "${GREEN}✓ Generated new PostgreSQL password${NC}"

# Generate Redis password (192-bit = 24 bytes)
REDIS_PASSWORD=$(openssl rand -base64 24)
echo -e "${GREEN}✓ Generated new Redis password${NC}"

echo ""

##############################################################################
# Step 4: Create Secrets in Secret Manager
##############################################################################

echo -e "${YELLOW}Step 3: Creating secrets in Google Secret Manager...${NC}"

# Function to create or update secret
create_secret() {
  local secret_name=$1
  local secret_value=$2
  local description=$3

  if gcloud secrets describe "$secret_name" &>/dev/null; then
    echo -e "${YELLOW}  Updating existing secret: $secret_name${NC}"
    echo -n "$secret_value" | gcloud secrets versions add "$secret_name" --data-file=-
  else
    echo -e "${BLUE}  Creating new secret: $secret_name${NC}"
    echo -n "$secret_value" | gcloud secrets create "$secret_name" \
      --data-file=- \
      --replication-policy="automatic" \
      --labels="app=rag-enterprise,managed-by=script"
  fi
  echo -e "${GREEN}  ✓ $description${NC}"
}

# Create all secrets
create_secret "postgres-password" "$DB_PASSWORD" "PostgreSQL password"
create_secret "redis-password" "$REDIS_PASSWORD" "Redis password"
create_secret "jwt-secret" "$JWT_SECRET" "JWT secret (256-bit)"
create_secret "openai-api-key" "$OPENAI_API_KEY" "OpenAI API key (rotated)"

echo ""

##############################################################################
# Step 5: Grant Secret Access to Service Accounts
##############################################################################

echo -e "${YELLOW}Step 4: Configuring IAM permissions...${NC}"

SECRETS=("postgres-password" "redis-password" "jwt-secret" "openai-api-key")

for secret in "${SECRETS[@]}"; do
  echo -e "${BLUE}  Granting access to $secret...${NC}"

  gcloud secrets add-iam-policy-binding "$secret" \
    --member="serviceAccount:$GKE_NODE_SA" \
    --role="roles/secretmanager.secretAccessor" \
    --quiet

  echo -e "${GREEN}  ✓ $GKE_NODE_SA can access $secret${NC}"
done

echo ""

##############################################################################
# Step 6: Verify Secret Creation
##############################################################################

echo -e "${YELLOW}Step 5: Verifying secrets...${NC}"

echo ""
echo -e "${BLUE}Secrets in Google Secret Manager:${NC}"
gcloud secrets list --filter="labels.app=rag-enterprise" --format="table(name,createTime,labels.managed-by)"

echo ""

##############################################################################
# Step 7: Create .env.template
##############################################################################

echo -e "${YELLOW}Step 6: Creating .env.template...${NC}"

cat > "$PROJECT_ROOT/.env.template" << 'EOF'
# RAG System Environment Configuration Template
# DO NOT put real secrets in this file - use Google Secret Manager

# Spring Profile
SPRING_PROFILES_ACTIVE=docker

# Database Configuration
DB_HOST=postgres
POSTGRES_DB=rag_enterprise
POSTGRES_USER=rag_user
POSTGRES_PASSWORD=<USE_SECRET_MANAGER:postgres-password>

# Redis Configuration
REDIS_HOST=redis
REDIS_PASSWORD=<USE_SECRET_MANAGER:redis-password>

# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=kafka:29092

# JWT Configuration
JWT_SECRET=<USE_SECRET_MANAGER:jwt-secret>

# External Services
OLLAMA_URL=http://ollama:11434
EMBEDDING_SERVICE_URL=http://rag-embedding:8083

# OpenAI API Configuration
OPENAI_API_KEY=<USE_SECRET_MANAGER:openai-api-key>

# INSTRUCTIONS:
# For local development, retrieve secrets from Secret Manager:
#   gcloud secrets versions access latest --secret=postgres-password
#   gcloud secrets versions access latest --secret=redis-password
#   gcloud secrets versions access latest --secret=jwt-secret
#   gcloud secrets versions access latest --secret=openai-api-key
#
# For production (Kubernetes), secrets are mounted via Secret Manager CSI driver.
# See docs/deployment/GCP_DEPLOYMENT_GUIDE.md for details.
EOF

echo -e "${GREEN}✓ Created .env.template${NC}"
echo ""

##############################################################################
# Step 8: Update .gitignore
##############################################################################

echo -e "${YELLOW}Step 7: Updating .gitignore...${NC}"

GITIGNORE="$PROJECT_ROOT/.gitignore"

# Check if .env patterns already exist
if grep -q "^\.env$" "$GITIGNORE" 2>/dev/null; then
  echo -e "${GREEN}✓ .gitignore already contains .env pattern${NC}"
else
  cat >> "$GITIGNORE" << 'EOF'

# Environment and secrets
.env
.env.*
!.env.template
*.key
*.pem
credentials.json
service-account-key.json
*-sa-key.json
EOF
  echo -e "${GREEN}✓ Updated .gitignore${NC}"
fi

echo ""

##############################################################################
# Step 9: Summary and Next Steps
##############################################################################

echo -e "${GREEN}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║                   ✓ Migration Complete                         ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${BLUE}Secrets created in Google Secret Manager:${NC}"
echo "  • postgres-password (192-bit, rotated)"
echo "  • redis-password (192-bit, rotated)"
echo "  • jwt-secret (256-bit, rotated)"
echo "  • openai-api-key (rotated)"
echo ""
echo -e "${BLUE}IAM Permissions:${NC}"
echo "  • $GKE_NODE_SA has secretAccessor role"
echo ""
echo -e "${BLUE}Files Created:${NC}"
echo "  • .env.template (safe to commit)"
echo ""
echo -e "${RED}⚠️  CRITICAL NEXT STEPS:${NC}"
echo ""
echo -e "${YELLOW}1. Remove .env from Git History:${NC}"
echo "   This is REQUIRED to remove the compromised OpenAI key from history."
echo "   Run: ./scripts/gcp/05-remove-secrets-from-git.sh"
echo ""
echo -e "${YELLOW}2. Update Local Development Environment:${NC}"
echo "   For local testing, create a new .env file with secrets from Secret Manager:"
echo "   Run: ./scripts/gcp/06-create-local-env.sh"
echo ""
echo -e "${YELLOW}3. Update Kubernetes Manifests (GCP-K8S-008):${NC}"
echo "   Configure Secret Manager CSI driver in GKE cluster."
echo "   See: docs/deployment/GCP_DEPLOYMENT_GUIDE.md"
echo ""
echo -e "${YELLOW}4. Verify Old OpenAI Key Deleted:${NC}"
echo "   Confirm the old key is deleted from OpenAI dashboard:"
echo "   https://platform.openai.com/api-keys"
echo ""
echo -e "${BLUE}To retrieve a secret:${NC}"
echo "  gcloud secrets versions access latest --secret=postgres-password"
echo ""
echo -e "${GREEN}GCP-SECRETS-002 migration script complete!${NC}"
echo ""

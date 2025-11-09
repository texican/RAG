#!/bin/bash

#################################################################################
# Cloud Memorystore Redis Setup Script
# 
# This script provisions a Google Cloud Memorystore Redis instance for the
# RAG system. Memorystore provides a fully managed, highly available Redis
# service with automatic failover and scaling.
#
# Prerequisites:
#   - GCP project created and configured (GCP-INFRA-001)
#   - gcloud CLI authenticated
#   - Billing enabled on the project
#
# Usage:
#   bash scripts/gcp/10-setup-memorystore.sh
#
# What this script does:
#   1. Creates Cloud Memorystore Redis instance (Standard tier for HA)
#   2. Configures Redis version, memory size, and region
#   3. Enables AUTH for security
#   4. Stores connection details in Secret Manager
#   5. Configures automated maintenance window
#   6. Outputs connection information
#
#################################################################################

set -e  # Exit on error

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default configuration
PROJECT_ID=${GCP_PROJECT_ID:-"byo-rag-dev"}
REGION=${GCP_REGION:-"us-central1"}
INSTANCE_NAME="rag-redis"
TIER="standard"  # Standard tier with high availability (replicated across zones)
REDIS_VERSION="redis_7_0"  # Redis 7.0 for latest features
MEMORY_SIZE_GB=5  # 5GB memory for development
NETWORK="rag-vpc"

# Check if running in correct directory
if [ ! -f "pom.xml" ]; then
    echo -e "${RED}Error: Must run from RAG project root directory${NC}"
    exit 1
fi

echo -e "${BLUE}================================${NC}"
echo -e "${BLUE}Cloud Memorystore Redis Setup${NC}"
echo -e "${BLUE}================================${NC}"
echo ""
echo "Configuration:"
echo "  Project ID: $PROJECT_ID"
echo "  Region: $REGION"
echo "  Instance Name: $INSTANCE_NAME"
echo "  Tier: $TIER"
echo "  Redis Version: $REDIS_VERSION"
echo "  Memory Size: ${MEMORY_SIZE_GB}GB"
echo ""

# Confirm with user
read -p "Continue with these settings? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Setup cancelled."
    exit 0
fi

#################################################################################
# Step 1: Check Prerequisites
#################################################################################

echo -e "${YELLOW}Step 1: Checking prerequisites...${NC}"

# Check if gcloud is installed
if ! command -v gcloud &> /dev/null; then
    echo -e "${RED}Error: gcloud CLI not found. Please install it first.${NC}"
    exit 1
fi

# Check if authenticated
if ! gcloud auth list --filter=status:ACTIVE --format="value(account)" &> /dev/null; then
    echo -e "${RED}Error: Not authenticated with gcloud. Run 'gcloud auth login'${NC}"
    exit 1
fi

# Set project
gcloud config set project "$PROJECT_ID" --quiet

# Check if Redis API is enabled
echo "Checking if Redis API is enabled..."
if ! gcloud services list --enabled --filter="config.name:redis.googleapis.com" --format="value(config.name)" | grep -q "redis.googleapis.com"; then
    echo "Enabling Redis API..."
    gcloud services enable redis.googleapis.com --project="$PROJECT_ID"
    echo "Waiting for API to be fully enabled (30 seconds)..."
    sleep 30
fi

# Check if VPC network exists
echo "Checking VPC network..."
if ! gcloud compute networks describe "$NETWORK" --project="$PROJECT_ID" &> /dev/null; then
    echo -e "${YELLOW}Warning: VPC network '$NETWORK' not found. Memorystore requires VPC peering.${NC}"
    echo "Using default network instead..."
    NETWORK="default"
fi

echo -e "${GREEN}✓ Prerequisites check complete${NC}"
echo ""

#################################################################################
# Step 2: Check if Instance Already Exists
#################################################################################

echo -e "${YELLOW}Step 2: Checking if instance already exists...${NC}"

if gcloud redis instances describe "$INSTANCE_NAME" --region="$REGION" --project="$PROJECT_ID" &> /dev/null; then
    echo -e "${YELLOW}Instance '$INSTANCE_NAME' already exists!${NC}"
    
    # Get existing instance details
    HOST=$(gcloud redis instances describe "$INSTANCE_NAME" \
        --region="$REGION" \
        --project="$PROJECT_ID" \
        --format="value(host)")
    
    PORT=$(gcloud redis instances describe "$INSTANCE_NAME" \
        --region="$REGION" \
        --project="$PROJECT_ID" \
        --format="value(port)")
    
    AUTH_STRING=$(gcloud redis instances describe "$INSTANCE_NAME" \
        --region="$REGION" \
        --project="$PROJECT_ID" \
        --format="value(authString)")
    
    echo ""
    echo "Existing Instance Details:"
    echo "  Host: $HOST"
    echo "  Port: $PORT"
    echo "  Auth String: ${AUTH_STRING:0:20}... (truncated)"
    echo ""
    
    read -p "Do you want to continue and update secrets? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Setup cancelled."
        exit 0
    fi
    
    # Skip to secret storage
    SKIP_CREATION=true
else
    echo "Instance does not exist. Will create new instance."
    SKIP_CREATION=false
fi

echo ""

#################################################################################
# Step 3: Create Memorystore Instance
#################################################################################

if [ "$SKIP_CREATION" = false ]; then
    echo -e "${YELLOW}Step 3: Creating Cloud Memorystore Redis instance...${NC}"
    echo "This may take 5-10 minutes..."
    echo ""
    
    # Create the Redis instance
    gcloud redis instances create "$INSTANCE_NAME" \
        --project="$PROJECT_ID" \
        --region="$REGION" \
        --tier="$TIER" \
        --size="$MEMORY_SIZE_GB" \
        --redis-version="$REDIS_VERSION" \
        --network="projects/$PROJECT_ID/global/networks/$NETWORK" \
        --enable-auth \
        --maintenance-window-day="SUNDAY" \
        --maintenance-window-hour=4 \
        --display-name="RAG System Redis Cache" \
        --labels="environment=production,service=rag,component=cache" \
        --quiet
    
    echo -e "${GREEN}✓ Redis instance created successfully${NC}"
    echo ""
    
    # Wait a moment for instance to be fully ready
    echo "Waiting for instance to be fully ready..."
    sleep 10
else
    echo -e "${YELLOW}Step 3: Skipping instance creation (already exists)${NC}"
    echo ""
fi

#################################################################################
# Step 4: Retrieve Instance Details
#################################################################################

echo -e "${YELLOW}Step 4: Retrieving instance details...${NC}"

# Get instance details
HOST=$(gcloud redis instances describe "$INSTANCE_NAME" \
    --region="$REGION" \
    --project="$PROJECT_ID" \
    --format="value(host)")

PORT=$(gcloud redis instances describe "$INSTANCE_NAME" \
    --region="$REGION" \
    --project="$PROJECT_ID" \
    --format="value(port)")

AUTH_STRING=$(gcloud redis instances describe "$INSTANCE_NAME" \
    --region="$REGION" \
    --project="$PROJECT_ID" \
    --format="value(authString)")

CURRENT_LOCATION_ID=$(gcloud redis instances describe "$INSTANCE_NAME" \
    --region="$REGION" \
    --project="$PROJECT_ID" \
    --format="value(currentLocationId)")

MEMORY_SIZE=$(gcloud redis instances describe "$INSTANCE_NAME" \
    --region="$REGION" \
    --project="$PROJECT_ID" \
    --format="value(memorySizeGb)")

REDIS_VERSION_ACTUAL=$(gcloud redis instances describe "$INSTANCE_NAME" \
    --region="$REGION" \
    --project="$PROJECT_ID" \
    --format="value(redisVersion)")

TIER_ACTUAL=$(gcloud redis instances describe "$INSTANCE_NAME" \
    --region="$REGION" \
    --project="$PROJECT_ID" \
    --format="value(tier)")

echo -e "${GREEN}✓ Instance details retrieved${NC}"
echo ""

#################################################################################
# Step 5: Store Connection Details in Secret Manager
#################################################################################

echo -e "${YELLOW}Step 5: Storing connection details in Secret Manager...${NC}"

# Create JSON with connection details
CONNECTION_INFO=$(cat <<EOF
{
  "host": "$HOST",
  "port": $PORT,
  "region": "$REGION",
  "instance_name": "$INSTANCE_NAME",
  "zone": "$CURRENT_LOCATION_ID",
  "memory_gb": $MEMORY_SIZE,
  "redis_version": "$REDIS_VERSION_ACTUAL",
  "tier": "$TIER_ACTUAL"
}
EOF
)

# Store Redis password (AUTH string)
echo "Storing Redis password..."
if gcloud secrets describe memorystore-redis-password --project="$PROJECT_ID" &> /dev/null; then
    echo "Secret 'memorystore-redis-password' already exists, creating new version..."
    echo -n "$AUTH_STRING" | gcloud secrets versions add memorystore-redis-password \
        --project="$PROJECT_ID" \
        --data-file=-
else
    echo "Creating secret 'memorystore-redis-password'..."
    echo -n "$AUTH_STRING" | gcloud secrets create memorystore-redis-password \
        --project="$PROJECT_ID" \
        --replication-policy="automatic" \
        --data-file=- \
        --labels="environment=production,service=rag"
fi

# Store connection info
echo "Storing Redis connection info..."
if gcloud secrets describe memorystore-connection-info --project="$PROJECT_ID" &> /dev/null; then
    echo "Secret 'memorystore-connection-info' already exists, creating new version..."
    echo -n "$CONNECTION_INFO" | gcloud secrets versions add memorystore-connection-info \
        --project="$PROJECT_ID" \
        --data-file=-
else
    echo "Creating secret 'memorystore-connection-info'..."
    echo -n "$CONNECTION_INFO" | gcloud secrets create memorystore-connection-info \
        --project="$PROJECT_ID" \
        --replication-policy="automatic" \
        --data-file=- \
        --labels="environment=production,service=rag"
fi

echo -e "${GREEN}✓ Secrets stored successfully${NC}"
echo ""

#################################################################################
# Step 6: Grant IAM Permissions
#################################################################################

echo -e "${YELLOW}Step 6: Configuring IAM permissions...${NC}"

# Grant GKE service account access to secrets
GKE_SA="gke-node-sa@${PROJECT_ID}.iam.gserviceaccount.com"

if gcloud iam service-accounts describe "$GKE_SA" --project="$PROJECT_ID" &> /dev/null; then
    echo "Granting GKE service account access to Redis secrets..."
    
    gcloud secrets add-iam-policy-binding memorystore-redis-password \
        --project="$PROJECT_ID" \
        --member="serviceAccount:$GKE_SA" \
        --role="roles/secretmanager.secretAccessor" \
        --quiet
    
    gcloud secrets add-iam-policy-binding memorystore-connection-info \
        --project="$PROJECT_ID" \
        --member="serviceAccount:$GKE_SA" \
        --role="roles/secretmanager.secretAccessor" \
        --quiet
    
    echo -e "${GREEN}✓ IAM permissions configured${NC}"
else
    echo -e "${YELLOW}Warning: GKE service account not found. Skipping IAM binding.${NC}"
    echo "You'll need to grant permissions manually when creating the GKE cluster."
fi

echo ""

#################################################################################
# Step 7: Display Summary
#################################################################################

echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}Setup Complete!${NC}"
echo -e "${GREEN}================================${NC}"
echo ""
echo "Redis Instance Details:"
echo "  Instance Name: $INSTANCE_NAME"
echo "  Host: $HOST"
echo "  Port: $PORT"
echo "  Region: $REGION"
echo "  Zone: $CURRENT_LOCATION_ID"
echo "  Memory: ${MEMORY_SIZE}GB"
echo "  Redis Version: $REDIS_VERSION_ACTUAL"
echo "  Tier: $TIER_ACTUAL (High Availability)"
echo ""
echo "Connection String:"
echo "  redis://$HOST:$PORT"
echo ""
echo "Authentication:"
echo "  Enabled: Yes"
echo "  Password stored in Secret Manager: memorystore-redis-password"
echo ""
echo "Secret Manager Secrets:"
echo "  • memorystore-redis-password - Redis AUTH string"
echo "  • memorystore-connection-info - Connection details (JSON)"
echo ""
echo "Retrieve Secrets:"
echo "  # Get Redis password"
echo "  gcloud secrets versions access latest \\"
echo "      --secret=memorystore-redis-password \\"
echo "      --project=$PROJECT_ID"
echo ""
echo "  # Get connection info"
echo "  gcloud secrets versions access latest \\"
echo "      --secret=memorystore-connection-info \\"
echo "      --project=$PROJECT_ID"
echo ""
echo "Spring Boot Configuration:"
echo "  spring.data.redis.host=$HOST"
echo "  spring.data.redis.port=$PORT"
echo "  spring.data.redis.password=\${REDIS_PASSWORD:from-secret-manager}"
echo ""
echo "Cost Estimate:"
echo "  ~\$230-250/month (Standard tier, ${MEMORY_SIZE}GB, high availability)"
echo ""
echo "Management Console:"
echo "  https://console.cloud.google.com/memorystore/redis/instances?project=$PROJECT_ID"
echo ""
echo "Monitoring:"
echo "  https://console.cloud.google.com/monitoring/dashboards?project=$PROJECT_ID"
echo ""
echo -e "${GREEN}Next Steps:${NC}"
echo "  1. Update service configurations to use Memorystore host/port"
echo "  2. Configure Kubernetes secrets with Redis password (GCP-K8S-008)"
echo "  3. Test Redis connectivity from GKE pods"
echo "  4. Set up monitoring alerts for Redis metrics"
echo "  5. Configure Redis connection pooling in applications"
echo ""

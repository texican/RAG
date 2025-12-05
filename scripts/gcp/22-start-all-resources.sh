#!/bin/bash
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
PROJECT_ID=${PROJECT_ID:-"byo-rag-dev"}
REGION=${REGION:-"us-central1"}
CLUSTER_NAME="rag-gke-dev"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --project)
      PROJECT_ID="$2"
      shift 2
      ;;
    --region)
      REGION="$2"
      shift 2
      ;;
    *)
      echo -e "${RED}Unknown option: $1${NC}"
      exit 1
      ;;
  esac
done

echo -e "${BLUE}════════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}   GCP Resources - START${NC}"
echo -e "${BLUE}════════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${YELLOW}Project:${NC} $PROJECT_ID"
echo -e "${YELLOW}Region:${NC} $REGION"
echo ""

# Set the project
gcloud config set project "$PROJECT_ID"

echo ""
echo -e "${BLUE}[1/2] Checking Resource Status...${NC}"

# Cloud SQL
SQL_STATUS=$(gcloud sql instances describe rag-postgres \
  --format="value(state)" 2>/dev/null || echo "NOT_FOUND")
echo -e "  Cloud SQL: ${YELLOW}$SQL_STATUS${NC}"

# Redis
REDIS_STATUS=$(gcloud redis instances describe rag-redis \
  --region="$REGION" \
  --format="value(state)" 2>/dev/null || echo "NOT_FOUND")
echo -e "  Memorystore Redis: ${YELLOW}$REDIS_STATUS${NC}"

# GKE
CLUSTER_STATUS=$(gcloud container clusters describe "$CLUSTER_NAME" \
  --region="$REGION" \
  --format="value(status)" 2>/dev/null || echo "NOT_FOUND")
echo -e "  GKE Cluster: ${YELLOW}$CLUSTER_STATUS${NC}"

if [[ "$CLUSTER_STATUS" == "NOT_FOUND" ]]; then
  echo ""
  echo -e "${RED}Error: GKE cluster not found. Please create it first:${NC}"
  echo "  ./scripts/gcp/12-setup-gke-cluster.sh --env dev"
  exit 1
fi

echo ""
echo -e "${BLUE}[2/2] Starting GKE Cluster...${NC}"

# Get current node pool sizes
SYSTEM_NODES=$(gcloud container node-pools describe system-pool \
  --cluster="$CLUSTER_NAME" \
  --region="$REGION" \
  --format="value(initialNodeCount)" 2>/dev/null || echo "0")

WORKLOAD_NODES=$(gcloud container node-pools describe workload-pool \
  --cluster="$CLUSTER_NAME" \
  --region="$REGION" \
  --format="value(initialNodeCount)" 2>/dev/null || echo "0")

if [[ "$SYSTEM_NODES" -eq 0 ]]; then
  echo "  Starting system-pool (1 node)..."
  gcloud container clusters resize "$CLUSTER_NAME" \
    --node-pool=system-pool \
    --num-nodes=1 \
    --region="$REGION" \
    --quiet
  echo -e "${GREEN}✓ System pool started${NC}"
else
  echo -e "${YELLOW}✓ System pool already running ($SYSTEM_NODES nodes)${NC}"
fi

if [[ "$WORKLOAD_NODES" -eq 0 ]]; then
  echo "  Starting workload-pool (2 nodes)..."
  gcloud container clusters resize "$CLUSTER_NAME" \
    --node-pool=workload-pool \
    --num-nodes=2 \
    --region="$REGION" \
    --quiet
  echo -e "${GREEN}✓ Workload pool started${NC}"
else
  echo -e "${YELLOW}✓ Workload pool already running ($WORKLOAD_NODES nodes)${NC}"
fi

echo ""
echo -e "${BLUE}Waiting for nodes to be ready (30 seconds)...${NC}"
sleep 30

echo ""
echo -e "${GREEN}════════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}   GKE Cluster Started Successfully${NC}"
echo -e "${GREEN}════════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${BLUE}Next Steps:${NC}"
echo "  1. Check cluster status:"
echo "     kubectl get nodes"
echo ""
echo "  2. Deploy services (if not already deployed):"
echo "     make gcp-deploy ENV=dev"
echo ""
echo "  3. Check pod status:"
echo "     kubectl get pods -n rag-system"
echo ""
echo -e "${YELLOW}Estimated Monthly Cost:${NC} ~\$470/month (all resources running)"

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
echo -e "${BLUE}   GCP Resources - STOP (Cost Optimization)${NC}"
echo -e "${BLUE}════════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${YELLOW}Project:${NC} $PROJECT_ID"
echo -e "${YELLOW}Region:${NC} $REGION"
echo ""
echo -e "${YELLOW}This will STOP (not delete) the following resources:${NC}"
echo "  • GKE Cluster (saves ~\$150-300/month)"
echo ""
echo -e "${YELLOW}These resources will REMAIN RUNNING (minimal cost):${NC}"
echo "  • Cloud SQL - cannot be stopped, only deleted (~\$77/month)"
echo "  • Cloud Memorystore Redis - cannot be stopped (~\$230/month)"
echo "  • Persistent Disks - minimal cost when not attached (~\$2/month)"
echo "  • Cloud Storage - pay for storage only (~\$5/month)"
echo "  • Artifact Registry - pay for storage only (~\$1/month)"
echo ""
echo -e "${RED}WARNING: Cloud SQL and Redis will continue to incur charges!${NC}"
echo -e "${YELLOW}To avoid ALL charges, use 21-delete-all-resources.sh instead${NC}"
echo ""
read -p "Continue with stopping GKE cluster? (yes/no): " confirm
if [[ "$confirm" != "yes" ]]; then
  echo -e "${YELLOW}Aborted.${NC}"
  exit 0
fi

# Set the project
gcloud config set project "$PROJECT_ID"

echo ""
echo -e "${BLUE}[1/3] Checking GKE Cluster Status...${NC}"
CLUSTER_STATUS=$(gcloud container clusters describe "$CLUSTER_NAME" \
  --region="$REGION" \
  --format="value(status)" 2>/dev/null || echo "NOT_FOUND")

if [[ "$CLUSTER_STATUS" == "NOT_FOUND" ]]; then
  echo -e "${YELLOW}✓ GKE cluster '$CLUSTER_NAME' not found (already deleted)${NC}"
elif [[ "$CLUSTER_STATUS" == "STOPPING" ]]; then
  echo -e "${YELLOW}✓ GKE cluster is already stopping${NC}"
elif [[ "$CLUSTER_STATUS" == "RUNNING" ]]; then
  echo -e "${YELLOW}Stopping GKE cluster (this takes ~5 minutes)...${NC}"
  
  # Scale down node pools to 0
  echo "  Scaling down workload-pool to 0 nodes..."
  gcloud container clusters resize "$CLUSTER_NAME" \
    --node-pool=workload-pool \
    --num-nodes=0 \
    --region="$REGION" \
    --quiet
  
  echo "  Scaling down system-pool to 0 nodes..."
  gcloud container clusters resize "$CLUSTER_NAME" \
    --node-pool=system-pool \
    --num-nodes=0 \
    --region="$REGION" \
    --quiet
  
  echo -e "${GREEN}✓ GKE cluster node pools scaled to 0${NC}"
else
  echo -e "${YELLOW}✓ GKE cluster status: $CLUSTER_STATUS${NC}"
fi

echo ""
echo -e "${BLUE}[2/3] Current Resource Status...${NC}"

# Cloud SQL
SQL_STATUS=$(gcloud sql instances describe rag-postgres \
  --format="value(state)" 2>/dev/null || echo "NOT_FOUND")
echo -e "  Cloud SQL: ${YELLOW}$SQL_STATUS${NC} (cannot be stopped - ~\$77/month)"

# Redis
REDIS_STATUS=$(gcloud redis instances describe rag-redis \
  --region="$REGION" \
  --format="value(state)" 2>/dev/null || echo "NOT_FOUND")
echo -e "  Memorystore Redis: ${YELLOW}$REDIS_STATUS${NC} (cannot be stopped - ~\$230/month)"

# GKE
echo -e "  GKE Cluster: ${GREEN}STOPPED (0 nodes)${NC}"

echo ""
echo -e "${BLUE}[3/3] Estimated Monthly Cost After Stopping...${NC}"
echo "  • GKE Cluster: ~\$0/month (0 nodes)"
echo "  • Cloud SQL: ~\$77/month (STILL RUNNING)"
echo "  • Redis: ~\$230/month (STILL RUNNING)"
echo "  • Storage/Registry: ~\$6/month"
echo "  ${YELLOW}Total: ~\$313/month${NC} (down from ~\$470/month)"
echo ""
echo -e "${GREEN}Savings: ~\$157/month (33% reduction)${NC}"
echo ""
echo -e "${YELLOW}To completely avoid charges, delete Cloud SQL and Redis:${NC}"
echo "  ./scripts/gcp/21-delete-all-resources.sh"
echo ""
echo -e "${GREEN}════════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}   GKE Cluster Stopped Successfully${NC}"
echo -e "${GREEN}════════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${BLUE}To restart the cluster:${NC}"
echo "  gcloud container clusters resize $CLUSTER_NAME \\"
echo "    --node-pool=system-pool --num-nodes=1 --region=$REGION"
echo "  gcloud container clusters resize $CLUSTER_NAME \\"
echo "    --node-pool=workload-pool --num-nodes=2 --region=$REGION"

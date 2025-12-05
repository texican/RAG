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

echo -e "${RED}════════════════════════════════════════════════════════════════${NC}"
echo -e "${RED}   GCP Resources - COMPLETE DELETION (⚠️  DANGER ZONE)${NC}"
echo -e "${RED}════════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${YELLOW}Project:${NC} $PROJECT_ID"
echo -e "${YELLOW}Region:${NC} $REGION"
echo ""
echo -e "${RED}⚠️  WARNING: This will PERMANENTLY DELETE ALL resources:${NC}"
echo ""
echo "  1. GKE Cluster (rag-gke-dev) + all deployments"
echo "  2. Cloud SQL PostgreSQL (rag-postgres) + ALL DATA"
echo "  3. Cloud Memorystore Redis (rag-redis) + ALL DATA"
echo "  4. Persistent Disks + ALL DATA"
echo "  5. Cloud Storage Buckets (optional)"
echo "  6. Container Images (optional)"
echo "  7. Static IP Addresses"
echo ""
echo -e "${RED}THIS CANNOT BE UNDONE!${NC}"
echo -e "${YELLOW}All application data, databases, and configurations will be lost.${NC}"
echo ""
echo -e "${BLUE}Monthly cost after deletion: \$0${NC}"
echo ""
read -p "Type 'DELETE EVERYTHING' to confirm: " confirm
if [[ "$confirm" != "DELETE EVERYTHING" ]]; then
  echo -e "${YELLOW}Aborted. No resources were deleted.${NC}"
  exit 0
fi

echo ""
read -p "Are you ABSOLUTELY SURE? Type 'YES I AM SURE': " confirm2
if [[ "$confirm2" != "YES I AM SURE" ]]; then
  echo -e "${YELLOW}Aborted. No resources were deleted.${NC}"
  exit 0
fi

# Set the project
gcloud config set project "$PROJECT_ID"

echo ""
echo -e "${BLUE}════════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}   Beginning Resource Deletion...${NC}"
echo -e "${BLUE}════════════════════════════════════════════════════════════════${NC}"

# 1. Delete GKE Cluster
echo ""
echo -e "${BLUE}[1/7] Deleting GKE Cluster...${NC}"
if gcloud container clusters describe "$CLUSTER_NAME" --region="$REGION" &>/dev/null; then
  echo "  Deleting cluster '$CLUSTER_NAME' (this takes ~5-10 minutes)..."
  gcloud container clusters delete "$CLUSTER_NAME" \
    --region="$REGION" \
    --quiet
  echo -e "${GREEN}✓ GKE cluster deleted${NC}"
else
  echo -e "${YELLOW}✓ GKE cluster already deleted${NC}"
fi

# 2. Delete Cloud SQL
echo ""
echo -e "${BLUE}[2/7] Deleting Cloud SQL...${NC}"
if gcloud sql instances describe rag-postgres &>/dev/null; then
  echo "  Deleting Cloud SQL instance 'rag-postgres' (this takes ~5 minutes)..."
  gcloud sql instances delete rag-postgres --quiet
  echo -e "${GREEN}✓ Cloud SQL deleted${NC}"
else
  echo -e "${YELLOW}✓ Cloud SQL already deleted${NC}"
fi

# 3. Delete Cloud Memorystore Redis
echo ""
echo -e "${BLUE}[3/7] Deleting Cloud Memorystore Redis...${NC}"
if gcloud redis instances describe rag-redis --region="$REGION" &>/dev/null; then
  echo "  Deleting Redis instance 'rag-redis' (this takes ~5 minutes)..."
  gcloud redis instances delete rag-redis --region="$REGION" --quiet
  echo -e "${GREEN}✓ Redis instance deleted${NC}"
else
  echo -e "${YELLOW}✓ Redis instance already deleted${NC}"
fi

# 4. Delete Persistent Disks
echo ""
echo -e "${BLUE}[4/7] Deleting Persistent Disks...${NC}"
DISKS=$(gcloud compute disks list --filter="name~^gke-$CLUSTER_NAME" --format="value(name,zone)" 2>/dev/null || echo "")
if [[ -n "$DISKS" ]]; then
  while IFS=$'\t' read -r disk_name zone; do
    echo "  Deleting disk '$disk_name' in zone '$zone'..."
    gcloud compute disks delete "$disk_name" --zone="$zone" --quiet || true
  done <<< "$DISKS"
  echo -e "${GREEN}✓ Persistent disks deleted${NC}"
else
  echo -e "${YELLOW}✓ No persistent disks found${NC}"
fi

# 5. Delete Cloud Storage Buckets (optional)
echo ""
echo -e "${BLUE}[5/7] Cloud Storage Buckets...${NC}"
echo -e "${YELLOW}Delete Cloud Storage buckets? (contains backups, documents, snapshots)${NC}"
read -p "Delete buckets? (yes/no): " delete_buckets
if [[ "$delete_buckets" == "yes" ]]; then
  BUCKETS=$(gsutil ls -p "$PROJECT_ID" 2>/dev/null | grep "rag" || echo "")
  if [[ -n "$BUCKETS" ]]; then
    while read -r bucket; do
      echo "  Deleting bucket '$bucket'..."
      # Fix for macOS Python multiprocessing issue (https://bugs.python.org/issue33725)
      gsutil -m -o "GSUtil:parallel_process_count=1" rm -r "$bucket" || true
    done <<< "$BUCKETS"
    echo -e "${GREEN}✓ Cloud Storage buckets deleted${NC}"
  else
    echo -e "${YELLOW}✓ No buckets found${NC}"
  fi
else
  echo -e "${YELLOW}✓ Skipping bucket deletion${NC}"
fi

# 6. Delete Artifact Registry Images (optional)
echo ""
echo -e "${BLUE}[6/7] Artifact Registry Images...${NC}"
echo -e "${YELLOW}Delete container images? (can be rebuilt from source)${NC}"
read -p "Delete images? (yes/no): " delete_images
if [[ "$delete_images" == "yes" ]]; then
  if gcloud artifacts repositories describe rag-system --location="$REGION" &>/dev/null; then
    echo "  Deleting repository 'rag-system'..."
    gcloud artifacts repositories delete rag-system --location="$REGION" --quiet
    echo -e "${GREEN}✓ Artifact Registry repository deleted${NC}"
  else
    echo -e "${YELLOW}✓ Repository already deleted${NC}"
  fi
else
  echo -e "${YELLOW}✓ Skipping image deletion${NC}"
fi

# 7. Delete Static IP Addresses
echo ""
echo -e "${BLUE}[7/7] Deleting Static IP Addresses...${NC}"
IP_ADDRESSES=$(gcloud compute addresses list --filter="name~rag-ingress" --format="value(name,region)" 2>/dev/null || echo "")
if [[ -n "$IP_ADDRESSES" ]]; then
  while IFS=$'\t' read -r ip_name ip_region; do
    echo "  Deleting IP address '$ip_name'..."
    if [[ -n "$ip_region" ]]; then
      gcloud compute addresses delete "$ip_name" --region="$ip_region" --quiet || true
    else
      gcloud compute addresses delete "$ip_name" --global --quiet || true
    fi
  done <<< "$IP_ADDRESSES"
  echo -e "${GREEN}✓ Static IP addresses deleted${NC}"
else
  echo -e "${YELLOW}✓ No static IP addresses found${NC}"
fi

# Summary
echo ""
echo -e "${GREEN}════════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}   All Resources Deleted Successfully${NC}"
echo -e "${GREEN}════════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${BLUE}Deleted Resources:${NC}"
echo "  ✓ GKE Cluster"
echo "  ✓ Cloud SQL PostgreSQL"
echo "  ✓ Cloud Memorystore Redis"
echo "  ✓ Persistent Disks"
if [[ "$delete_buckets" == "yes" ]]; then
  echo "  ✓ Cloud Storage Buckets"
fi
if [[ "$delete_images" == "yes" ]]; then
  echo "  ✓ Container Images"
fi
echo "  ✓ Static IP Addresses"
echo ""
echo -e "${GREEN}Monthly Cost: \$0${NC}"
echo ""
echo -e "${BLUE}Remaining Resources (minimal/no cost):${NC}"
echo "  • VPC Network (free)"
echo "  • Service Accounts (free)"
echo "  • Secret Manager secrets (~\$0.06/month)"
if [[ "$delete_buckets" != "yes" ]]; then
  echo "  • Cloud Storage buckets (~\$5/month)"
fi
if [[ "$delete_images" != "yes" ]]; then
  echo "  • Container images (~\$1/month)"
fi
echo ""
echo -e "${YELLOW}To recreate the infrastructure:${NC}"
echo "  make gcp-setup ENV=dev"
echo "  make gcp-deploy-all ENV=dev"

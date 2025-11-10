#!/bin/bash

################################################################################
# GKE Cluster Setup Script
# 
# This script creates a Google Kubernetes Engine (GKE) cluster for the BYO RAG
# system with production-ready configuration including:
# - Regional cluster with multiple node pools
# - Workload Identity for pod-level IAM
# - Cluster autoscaling
# - VPC-native networking
# - GKE monitoring and logging
# - Security hardening (shielded nodes, network policies)
#
# Prerequisites:
# - gcloud CLI installed and authenticated
# - Project ID set in gcloud config or via PROJECT_ID environment variable
# - Compute Engine API enabled
# - Container API enabled
# - Sufficient quota for GKE clusters and node pools
#
# Usage:
#   ./12-setup-gke-cluster.sh [--dev|--prod]
#
# Options:
#   --dev   Create development cluster (zonal, smaller nodes)
#   --prod  Create production cluster (regional, HA configuration)
#
################################################################################

set -euo pipefail

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Configuration
PROJECT_ID="${PROJECT_ID:-$(gcloud config get-value project 2>/dev/null)}"
REGION="${REGION:-us-central1}"
ENVIRONMENT="${1:---dev}"

# Cluster configuration based on environment
if [[ "$ENVIRONMENT" == "--prod" ]]; then
    CLUSTER_NAME="rag-gke-prod"
    CLUSTER_TYPE="regional"
    ZONES="us-central1-a,us-central1-b,us-central1-c"
    SYSTEM_POOL_MIN=1
    SYSTEM_POOL_MAX=3
    WORKLOAD_POOL_MIN=3
    WORKLOAD_POOL_MAX=10
    SYSTEM_MACHINE_TYPE="e2-standard-2"
    WORKLOAD_MACHINE_TYPE="n1-standard-4"
    ENABLE_BACKUP=true
else
    CLUSTER_NAME="rag-gke-dev"
    CLUSTER_TYPE="zonal"
    ZONES="us-central1-a"
    SYSTEM_POOL_MIN=1
    SYSTEM_POOL_MAX=2
    WORKLOAD_POOL_MIN=2
    WORKLOAD_POOL_MAX=5
    SYSTEM_MACHINE_TYPE="e2-medium"
    WORKLOAD_MACHINE_TYPE="e2-standard-4"
    ENABLE_BACKUP=false
fi

# Network configuration
NETWORK="default"
SUBNET="default"
CLUSTER_SECONDARY_RANGE_NAME="gke-pods"
SERVICES_SECONDARY_RANGE_NAME="gke-services"

# Service accounts (from 02-setup-service-accounts.sh)
GKE_NODE_SA="gke-node-sa@${PROJECT_ID}.iam.gserviceaccount.com"

log_info "=========================================="
log_info "GKE Cluster Setup - BYO RAG System"
log_info "=========================================="
log_info "Project ID: ${PROJECT_ID}"
log_info "Region: ${REGION}"
log_info "Cluster Name: ${CLUSTER_NAME}"
log_info "Cluster Type: ${CLUSTER_TYPE}"
log_info "Environment: ${ENVIRONMENT}"
log_info "=========================================="

# Validate project ID
if [[ -z "$PROJECT_ID" ]]; then
    log_error "PROJECT_ID is not set. Please set it using: export PROJECT_ID=your-project-id"
    exit 1
fi

# Set the project
gcloud config set project "$PROJECT_ID"

################################################################################
# Step 1: Enable Required APIs
################################################################################

log_info "Step 1: Enabling required GCP APIs..."

REQUIRED_APIS=(
    "container.googleapis.com"           # GKE
    "compute.googleapis.com"             # Compute Engine
    "logging.googleapis.com"             # Cloud Logging
    "monitoring.googleapis.com"          # Cloud Monitoring
    "cloudresourcemanager.googleapis.com" # Resource Manager
)

for api in "${REQUIRED_APIS[@]}"; do
    log_info "Enabling $api..."
    gcloud services enable "$api" --project="$PROJECT_ID"
done

log_success "Required APIs enabled"

################################################################################
# Step 2: Check if Cluster Already Exists
################################################################################

log_info "Step 2: Checking if cluster already exists..."

if gcloud container clusters describe "$CLUSTER_NAME" --region="$REGION" --project="$PROJECT_ID" &>/dev/null; then
    log_warning "Cluster '$CLUSTER_NAME' already exists in region '$REGION'"
    
    read -p "Do you want to delete and recreate the cluster? (yes/no): " -r
    if [[ $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
        log_info "Deleting existing cluster..."
        gcloud container clusters delete "$CLUSTER_NAME" \
            --region="$REGION" \
            --project="$PROJECT_ID" \
            --quiet
        log_success "Existing cluster deleted"
    else
        log_info "Skipping cluster creation. Exiting."
        exit 0
    fi
fi

################################################################################
# Step 3: Create GKE Secondary Ranges (if needed)
################################################################################

log_info "Step 3: Configuring VPC secondary ranges for GKE..."

# Check if secondary ranges already exist
EXISTING_RANGES=$(gcloud compute networks subnets describe "$SUBNET" \
    --region="$REGION" \
    --format="value(secondaryIpRanges[].rangeName)" 2>/dev/null || echo "")

if [[ ! "$EXISTING_RANGES" =~ "$CLUSTER_SECONDARY_RANGE_NAME" ]]; then
    log_info "Adding secondary range for pods: $CLUSTER_SECONDARY_RANGE_NAME"
    gcloud compute networks subnets update "$SUBNET" \
        --region="$REGION" \
        --add-secondary-ranges="${CLUSTER_SECONDARY_RANGE_NAME}=10.4.0.0/14" \
        --project="$PROJECT_ID"
else
    log_info "Secondary range for pods already exists"
fi

if [[ ! "$EXISTING_RANGES" =~ "$SERVICES_SECONDARY_RANGE_NAME" ]]; then
    log_info "Adding secondary range for services: $SERVICES_SECONDARY_RANGE_NAME"
    gcloud compute networks subnets update "$SUBNET" \
        --region="$REGION" \
        --add-secondary-ranges="${SERVICES_SECONDARY_RANGE_NAME}=10.8.0.0/20" \
        --project="$PROJECT_ID"
else
    log_info "Secondary range for services already exists"
fi

log_success "VPC secondary ranges configured"

################################################################################
# Step 4: Create GKE Cluster
################################################################################

log_info "Step 4: Creating GKE cluster '$CLUSTER_NAME'..."

GKE_CREATE_CMD=(
    gcloud container clusters create "$CLUSTER_NAME"
    --region="$REGION"
    --project="$PROJECT_ID"
    --cluster-version="latest"
    --release-channel="regular"
    
    # Node pool configuration (default pool will be deleted later)
    --num-nodes=1
    --machine-type="e2-medium"
    --disk-type="pd-standard"
    --disk-size=50
    
    # Network configuration
    --network="$NETWORK"
    --subnetwork="$SUBNET"
    --enable-ip-alias
    --cluster-secondary-range-name="$CLUSTER_SECONDARY_RANGE_NAME"
    --services-secondary-range-name="$SERVICES_SECONDARY_RANGE_NAME"
    
    # Security configuration
    --enable-shielded-nodes
    --shielded-secure-boot
    --shielded-integrity-monitoring
    --enable-network-policy
    --enable-private-nodes
    --enable-private-endpoint
    --master-ipv4-cidr="172.16.0.0/28"
    
    # Workload Identity
    --workload-pool="${PROJECT_ID}.svc.id.goog"
    
    # Service account
    --service-account="$GKE_NODE_SA"
    
    # Autoscaling and maintenance
    --enable-autoupgrade
    --enable-autorepair
    --maintenance-window-start="2024-01-01T00:00:00Z"
    --maintenance-window-end="2024-01-01T04:00:00Z"
    --maintenance-window-recurrence="FREQ=WEEKLY;BYDAY=SU"
    
    # Addons
    --addons=HorizontalPodAutoscaling,HttpLoadBalancing,GcePersistentDiskCsiDriver
    --logging=SYSTEM,WORKLOAD
    --monitoring=SYSTEM
)

# Add zonal configuration for dev
if [[ "$CLUSTER_TYPE" == "zonal" ]]; then
    GKE_CREATE_CMD+=(--zone="${ZONES}")
fi

# Execute cluster creation
log_info "Executing cluster creation (this may take 5-10 minutes)..."
"${GKE_CREATE_CMD[@]}"

log_success "GKE cluster created successfully"

################################################################################
# Step 5: Get Cluster Credentials
################################################################################

log_info "Step 5: Getting cluster credentials..."

gcloud container clusters get-credentials "$CLUSTER_NAME" \
    --region="$REGION" \
    --project="$PROJECT_ID"

log_success "Cluster credentials configured for kubectl"

################################################################################
# Step 6: Delete Default Node Pool
################################################################################

log_info "Step 6: Deleting default node pool..."

# Wait a bit for cluster to stabilize
sleep 10

gcloud container node-pools delete default-pool \
    --cluster="$CLUSTER_NAME" \
    --region="$REGION" \
    --project="$PROJECT_ID" \
    --quiet || log_warning "Default pool already deleted or not found"

log_success "Default node pool deleted"

################################################################################
# Step 7: Create System Node Pool
################################################################################

log_info "Step 7: Creating system node pool for cluster components..."

gcloud container node-pools create "system-pool" \
    --cluster="$CLUSTER_NAME" \
    --region="$REGION" \
    --project="$PROJECT_ID" \
    --machine-type="$SYSTEM_MACHINE_TYPE" \
    --disk-type="pd-standard" \
    --disk-size=50 \
    --num-nodes="$SYSTEM_POOL_MIN" \
    --enable-autoscaling \
    --min-nodes="$SYSTEM_POOL_MIN" \
    --max-nodes="$SYSTEM_POOL_MAX" \
    --enable-autorepair \
    --enable-autoupgrade \
    --service-account="$GKE_NODE_SA" \
    --node-labels="workload-type=system" \
    --node-taints="workload-type=system:NoSchedule" \
    --metadata=disable-legacy-endpoints=true \
    --shielded-secure-boot \
    --shielded-integrity-monitoring

log_success "System node pool created"

################################################################################
# Step 8: Create Workload Node Pool
################################################################################

log_info "Step 8: Creating workload node pool for application services..."

gcloud container node-pools create "workload-pool" \
    --cluster="$CLUSTER_NAME" \
    --region="$REGION" \
    --project="$PROJECT_ID" \
    --machine-type="$WORKLOAD_MACHINE_TYPE" \
    --disk-type="pd-ssd" \
    --disk-size=100 \
    --num-nodes="$WORKLOAD_POOL_MIN" \
    --enable-autoscaling \
    --min-nodes="$WORKLOAD_POOL_MIN" \
    --max-nodes="$WORKLOAD_POOL_MAX" \
    --enable-autorepair \
    --enable-autoupgrade \
    --service-account="$GKE_NODE_SA" \
    --node-labels="workload-type=application" \
    --metadata=disable-legacy-endpoints=true \
    --shielded-secure-boot \
    --shielded-integrity-monitoring

log_success "Workload node pool created"

################################################################################
# Step 9: Configure Cluster Autoscaling
################################################################################

log_info "Step 9: Configuring cluster autoscaling..."

gcloud container clusters update "$CLUSTER_NAME" \
    --region="$REGION" \
    --project="$PROJECT_ID" \
    --enable-autoscaling \
    --min-nodes="$((SYSTEM_POOL_MIN + WORKLOAD_POOL_MIN))" \
    --max-nodes="$((SYSTEM_POOL_MAX + WORKLOAD_POOL_MAX))" \
    --autoscaling-profile=balanced

log_success "Cluster autoscaling configured"

################################################################################
# Step 10: Install Cluster Add-ons
################################################################################

log_info "Step 10: Installing cluster add-ons..."

# Create namespace for system components
kubectl create namespace ingress-nginx --dry-run=client -o yaml | kubectl apply -f -
kubectl create namespace cert-manager --dry-run=client -o yaml | kubectl apply -f -

# Install NGINX Ingress Controller
log_info "Installing NGINX Ingress Controller..."
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.1/deploy/static/provider/cloud/deploy.yaml

# Wait for ingress controller to be ready
log_info "Waiting for ingress controller deployment..."
kubectl wait --namespace ingress-nginx \
    --for=condition=ready pod \
    --selector=app.kubernetes.io/component=controller \
    --timeout=300s || log_warning "Ingress controller not ready yet"

# Install cert-manager
log_info "Installing cert-manager..."
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml

# Wait for cert-manager to be ready
log_info "Waiting for cert-manager deployment..."
kubectl wait --namespace cert-manager \
    --for=condition=ready pod \
    --selector=app.kubernetes.io/instance=cert-manager \
    --timeout=300s || log_warning "cert-manager not ready yet"

log_success "Cluster add-ons installed"

################################################################################
# Step 11: Configure Workload Identity Bindings
################################################################################

log_info "Step 11: Configuring Workload Identity bindings..."

# Service accounts that need Workload Identity
SERVICES=(
    "rag-auth:cloud-sql-sa"
    "rag-document:cloud-sql-sa"
    "rag-embedding:pubsub-sa"
    "rag-core:pubsub-sa"
    "rag-admin:cloud-sql-sa"
)

# Create Kubernetes namespaces
kubectl create namespace rag-system --dry-run=client -o yaml | kubectl apply -f -

# Create Kubernetes service accounts and bind to GCP service accounts
for service in "${SERVICES[@]}"; do
    K8S_SA="${service%%:*}"
    GCP_SA="${service##*:}@${PROJECT_ID}.iam.gserviceaccount.com"
    
    log_info "Creating K8s service account: $K8S_SA"
    kubectl create serviceaccount "$K8S_SA" -n rag-system --dry-run=client -o yaml | kubectl apply -f -
    
    log_info "Binding $K8S_SA to $GCP_SA"
    gcloud iam service-accounts add-iam-policy-binding "$GCP_SA" \
        --project="$PROJECT_ID" \
        --role="roles/iam.workloadIdentityUser" \
        --member="serviceAccount:${PROJECT_ID}.svc.id.goog[rag-system/${K8S_SA}]"
    
    kubectl annotate serviceaccount "$K8S_SA" -n rag-system \
        iam.gke.io/gcp-service-account="$GCP_SA" \
        --overwrite
done

log_success "Workload Identity bindings configured"

################################################################################
# Step 12: Create ConfigMaps and Secrets Placeholders
################################################################################

log_info "Step 12: Creating ConfigMaps for GCP resources..."

# Create ConfigMap with GCP resource information
cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: ConfigMap
metadata:
  name: gcp-config
  namespace: rag-system
data:
  PROJECT_ID: "${PROJECT_ID}"
  REGION: "${REGION}"
  CLOUD_SQL_INSTANCE: "${PROJECT_ID}:${REGION}:rag-postgres"
  REDIS_HOST: "10.170.252.12"
  REDIS_PORT: "6379"
  ARTIFACT_REGISTRY: "${REGION}-docker.pkg.dev/${PROJECT_ID}/rag-system"
EOF

log_success "ConfigMaps created"

################################################################################
# Step 13: Configure Network Policies
################################################################################

log_info "Step 13: Configuring network policies..."

# Default deny all ingress traffic
cat <<EOF | kubectl apply -f -
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny-ingress
  namespace: rag-system
spec:
  podSelector: {}
  policyTypes:
  - Ingress
EOF

# Allow ingress from ingress controller
cat <<EOF | kubectl apply -f -
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-ingress-controller
  namespace: rag-system
spec:
  podSelector:
    matchLabels:
      app: rag-gateway
  policyTypes:
  - Ingress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: ingress-nginx
    ports:
    - protocol: TCP
      port: 8080
EOF

# Allow inter-service communication
cat <<EOF | kubectl apply -f -
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-rag-services
  namespace: rag-system
spec:
  podSelector:
    matchLabels:
      app.kubernetes.io/part-of: rag-system
  policyTypes:
  - Ingress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          app.kubernetes.io/part-of: rag-system
    ports:
    - protocol: TCP
      port: 8080
    - protocol: TCP
      port: 8081
    - protocol: TCP
      port: 8082
    - protocol: TCP
      port: 8083
    - protocol: TCP
      port: 8084
    - protocol: TCP
      port: 8085
EOF

log_success "Network policies configured"

################################################################################
# Step 14: Verify Cluster Status
################################################################################

log_info "Step 14: Verifying cluster status..."

echo ""
log_info "Cluster Information:"
gcloud container clusters describe "$CLUSTER_NAME" \
    --region="$REGION" \
    --project="$PROJECT_ID" \
    --format="table(name,location,status,currentMasterVersion,currentNodeVersion)"

echo ""
log_info "Node Pools:"
gcloud container node-pools list \
    --cluster="$CLUSTER_NAME" \
    --region="$REGION" \
    --project="$PROJECT_ID" \
    --format="table(name,machineType,diskSizeGb,status,autoscaling.enabled,autoscaling.minNodeCount,autoscaling.maxNodeCount)"

echo ""
log_info "Kubernetes Nodes:"
kubectl get nodes -o wide

echo ""
log_info "Kubernetes Namespaces:"
kubectl get namespaces

echo ""
log_info "Service Accounts in rag-system:"
kubectl get serviceaccounts -n rag-system

################################################################################
# Summary
################################################################################

echo ""
log_success "=========================================="
log_success "GKE Cluster Setup Complete!"
log_success "=========================================="
echo ""
log_info "Cluster Details:"
log_info "  Name: ${CLUSTER_NAME}"
log_info "  Region: ${REGION}"
log_info "  Type: ${CLUSTER_TYPE}"
log_info "  Node Pools: system-pool, workload-pool"
log_info "  Workload Identity: Enabled"
log_info "  Network Policies: Enabled"
echo ""
log_info "Next Steps:"
log_info "  1. Review kubectl context: kubectl config current-context"
log_info "  2. Deploy Kubernetes manifests (GCP-K8S-008)"
log_info "  3. Configure ingress and DNS (GCP-INGRESS-010)"
log_info "  4. Deploy applications (GCP-DEPLOY-011)"
echo ""
log_info "Useful Commands:"
log_info "  View cluster: gcloud container clusters describe ${CLUSTER_NAME} --region=${REGION}"
log_info "  Get credentials: gcloud container clusters get-credentials ${CLUSTER_NAME} --region=${REGION}"
log_info "  View nodes: kubectl get nodes"
log_info "  View workloads: kubectl get all -n rag-system"
echo ""
log_success "=========================================="

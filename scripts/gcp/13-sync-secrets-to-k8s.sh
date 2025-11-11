#!/bin/bash

################################################################################
# Sync Secrets from GCP Secret Manager to Kubernetes
# 
# This script retrieves secrets from GCP Secret Manager and creates
# Kubernetes secrets in the rag-system namespace.
#
# Prerequisites:
# - gcloud CLI installed and authenticated
# - kubectl configured for target GKE cluster
# - Secrets already created in Secret Manager (via 03-setup-secrets.sh)
# - rag-system namespace exists in Kubernetes
#
# Usage:
#   ./13-sync-secrets-to-k8s.sh
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
NAMESPACE="rag-system"

log_info "=========================================="
log_info "Sync Secrets to Kubernetes"
log_info "=========================================="
log_info "Project ID: ${PROJECT_ID}"
log_info "Namespace: ${NAMESPACE}"
log_info "=========================================="

# Validate project ID
if [[ -z "$PROJECT_ID" ]]; then
    log_error "PROJECT_ID is not set. Please set it using: export PROJECT_ID=your-project-id"
    exit 1
fi

# Check if namespace exists
if ! kubectl get namespace "$NAMESPACE" &>/dev/null; then
    log_error "Namespace '$NAMESPACE' does not exist. Please create it first."
    exit 1
fi

################################################################################
# Function to create Kubernetes secret from Secret Manager
################################################################################

create_k8s_secret() {
    local SECRET_NAME=$1
    local K8S_SECRET_NAME=$2
    local KEY_NAME=$3
    
    log_info "Retrieving secret: $SECRET_NAME"
    
    # Get secret value from Secret Manager
    SECRET_VALUE=$(gcloud secrets versions access latest \
        --secret="$SECRET_NAME" \
        --project="$PROJECT_ID" 2>/dev/null)
    
    if [[ -z "$SECRET_VALUE" ]]; then
        log_error "Failed to retrieve secret: $SECRET_NAME"
        return 1
    fi
    
    # Check if secret already exists
    if kubectl get secret "$K8S_SECRET_NAME" -n "$NAMESPACE" &>/dev/null; then
        log_warning "Secret '$K8S_SECRET_NAME' already exists. Deleting..."
        kubectl delete secret "$K8S_SECRET_NAME" -n "$NAMESPACE"
    fi
    
    # Create Kubernetes secret
    kubectl create secret generic "$K8S_SECRET_NAME" \
        --from-literal="${KEY_NAME}=${SECRET_VALUE}" \
        -n "$NAMESPACE"
    
    log_success "Created Kubernetes secret: $K8S_SECRET_NAME"
}

################################################################################
# Step 1: Create Cloud SQL Credentials Secret
################################################################################

log_info "Step 1: Creating postgres credentials secret..."

# Get Cloud SQL password from cloudsql-app-password secret (username is rag_user)
DB_USERNAME="rag_user"
DB_PASSWORD=$(gcloud secrets versions access latest \
    --secret="cloudsql-app-password" \
    --project="$PROJECT_ID" 2>/dev/null)

if [[ -z "$DB_PASSWORD" ]]; then
    log_error "Failed to retrieve postgres password"
    exit 1
fi

# Delete existing secret if present
if kubectl get secret cloud-sql-credentials -n "$NAMESPACE" &>/dev/null; then
    log_warning "Secret 'cloud-sql-credentials' already exists. Deleting..."
    kubectl delete secret cloud-sql-credentials -n "$NAMESPACE"
fi

# Create postgres credentials secret (naming convention for K8s deployment compatibility)
kubectl create secret generic cloud-sql-credentials \
    --from-literal=username="$DB_USERNAME" \
    --from-literal=password="$DB_PASSWORD" \
    -n "$NAMESPACE"

log_success "Created cloud-sql-credentials secret"

################################################################################
# Step 2: Create Redis Credentials Secret
################################################################################

log_info "Step 2: Creating Redis credentials secret..."

create_k8s_secret "redis-password" "redis-credentials" "password"

################################################################################
# Step 3: Create GCP Secrets ConfigMap (JWT)
################################################################################

log_info "Step 3: Creating GCP secrets (JWT)..."

JWT_SECRET=$(gcloud secrets versions access latest \
    --secret="jwt-secret" \
    --project="$PROJECT_ID" 2>/dev/null)

if [[ -z "$JWT_SECRET" ]]; then
    log_error "Failed to retrieve JWT secret"
    exit 1
fi

# Delete existing secrets if present
if kubectl get secret jwt-secret -n "$NAMESPACE" &>/dev/null; then
    log_warning "Secret 'jwt-secret' already exists. Deleting..."
    kubectl delete secret jwt-secret -n "$NAMESPACE"
fi

# Create jwt-secret (naming convention for K8s deployment compatibility)
kubectl create secret generic jwt-secret \
    --from-literal=secret="$JWT_SECRET" \
    -n "$NAMESPACE"

log_success "Created jwt-secret"

################################################################################
# Step 4: Create GCP Config ConfigMap
################################################################################

log_info "Step 4: Creating GCP config ConfigMap..."

# Get Cloud SQL private IP
CLOUD_SQL_IP=$(gcloud sql instances describe rag-postgres \
    --project="$PROJECT_ID" \
    --format="value(ipAddresses[2].ipAddress)" 2>/dev/null)

# Get Redis connection info
REDIS_INFO=$(gcloud redis instances describe rag-redis \
    --region=us-central1 \
    --project="$PROJECT_ID" \
    --format="value(host,port)" 2>/dev/null)
REDIS_HOST=$(echo "$REDIS_INFO" | awk '{print $1}')
REDIS_PORT=$(echo "$REDIS_INFO" | awk '{print $2}')

if [[ -z "$REDIS_HOST" ]] || [[ -z "$REDIS_PORT" ]]; then
    log_error "Failed to retrieve Redis connection info"
    exit 1
fi

# Delete existing configmap if present
if kubectl get configmap gcp-config -n "$NAMESPACE" &>/dev/null; then
    log_warning "ConfigMap 'gcp-config' already exists. Deleting..."
    kubectl delete configmap gcp-config -n "$NAMESPACE"
fi

# Create gcp-config ConfigMap
kubectl create configmap gcp-config -n "$NAMESPACE" \
    --from-literal=SPRING_PROFILES_ACTIVE=gcp \
    --from-literal=DB_HOST=localhost \
    --from-literal=DB_PORT=5432 \
    --from-literal=DB_NAME=rag_auth \
    --from-literal=SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/rag_auth \
    --from-literal=REDIS_HOST="$REDIS_HOST" \
    --from-literal=REDIS_PORT="$REDIS_PORT" \
    --from-literal=GCP_PROJECT_ID="$PROJECT_ID" \
    --from-literal=GCP_REGION=us-central1

log_success "Created gcp-config ConfigMap"

################################################################################
# Step 5: Verify Secrets
################################################################################

log_info "Step 5: Verifying secrets..."

echo ""
log_info "Secrets in namespace '$NAMESPACE':"
kubectl get secrets -n "$NAMESPACE"

echo ""
log_success "=========================================="
log_success "Secrets Synced Successfully!"
log_success "=========================================="
echo ""
log_info "Next Steps:"
log_info "  1. Deploy services: kubectl apply -k k8s/overlays/dev"
log_info "  2. Verify pods: kubectl get pods -n rag-system"
log_info "  3. Check logs: kubectl logs -f -l app=rag-auth -n rag-system"
echo ""
log_success "=========================================="

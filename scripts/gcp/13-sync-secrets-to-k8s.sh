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

log_info "Step 1: Creating Cloud SQL credentials secret..."

# Get Cloud SQL username
DB_USERNAME=$(gcloud secrets versions access latest \
    --secret="cloud-sql-username" \
    --project="$PROJECT_ID" 2>/dev/null)

# Get Cloud SQL password
DB_PASSWORD=$(gcloud secrets versions access latest \
    --secret="cloud-sql-password" \
    --project="$PROJECT_ID" 2>/dev/null)

if [[ -z "$DB_USERNAME" ]] || [[ -z "$DB_PASSWORD" ]]; then
    log_error "Failed to retrieve Cloud SQL credentials"
    exit 1
fi

# Delete existing secret if present
if kubectl get secret cloud-sql-credentials -n "$NAMESPACE" &>/dev/null; then
    log_warning "Secret 'cloud-sql-credentials' already exists. Deleting..."
    kubectl delete secret cloud-sql-credentials -n "$NAMESPACE"
fi

# Create Cloud SQL credentials secret
kubectl create secret generic cloud-sql-credentials \
    --from-literal=username="$DB_USERNAME" \
    --from-literal=password="$DB_PASSWORD" \
    -n "$NAMESPACE"

log_success "Created Cloud SQL credentials secret"

################################################################################
# Step 2: Create Redis Credentials Secret
################################################################################

log_info "Step 2: Creating Redis credentials secret..."

create_k8s_secret "memorystore-redis-password" "redis-credentials" "password"

################################################################################
# Step 3: Create JWT Secret
################################################################################

log_info "Step 3: Creating JWT secret..."

create_k8s_secret "jwt-secret-key" "jwt-secret" "secret"

################################################################################
# Step 4: Verify Secrets
################################################################################

log_info "Step 4: Verifying secrets..."

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

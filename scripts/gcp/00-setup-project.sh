#!/bin/bash

################################################################################
# GCP-INFRA-001: GCP Project Setup and Foundation
#
# This script automates the creation and configuration of a GCP project
# for the BYO RAG System deployment.
#
# Prerequisites:
#   - gcloud CLI installed and authenticated
#   - Billing account access
#   - Organization/Folder permissions (or use personal account)
#
# Usage:
#   ./00-setup-project.sh
#
# Story: GCP-INFRA-001
# Story Points: 8
# Priority: P0 - Critical
################################################################################

set -euo pipefail

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
CONFIG_DIR="${SCRIPT_DIR}/config"
LOG_DIR="${PROJECT_ROOT}/logs/gcp-setup"

# Create directories
mkdir -p "${CONFIG_DIR}"
mkdir -p "${LOG_DIR}"

# Logging configuration
LOG_FILE="${LOG_DIR}/project-setup-$(date +%Y%m%d-%H%M%S).log"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Logging functions
log() {
    local level=$1
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')

    case $level in
        INFO)
            echo -e "${GREEN}[INFO]${NC} $message" | tee -a "$LOG_FILE"
            ;;
        WARN)
            echo -e "${YELLOW}[WARN]${NC} $message" | tee -a "$LOG_FILE"
            ;;
        ERROR)
            echo -e "${RED}[ERROR]${NC} $message" | tee -a "$LOG_FILE"
            ;;
        SUCCESS)
            echo -e "${GREEN}✓${NC} $message" | tee -a "$LOG_FILE"
            ;;
        STEP)
            echo -e "${CYAN}[STEP]${NC} $message" | tee -a "$LOG_FILE"
            ;;
    esac

    echo "[$timestamp] [$level] $message" >> "$LOG_FILE"
}

error_exit() {
    log ERROR "$1"
    exit 1
}

# Check prerequisites
check_prerequisites() {
    log STEP "Checking prerequisites..."

    # Check gcloud
    if ! command -v gcloud &> /dev/null; then
        error_exit "gcloud CLI is not installed. Please install from https://cloud.google.com/sdk"
    fi
    log SUCCESS "gcloud CLI found"

    # Check authentication
    if ! gcloud auth list --filter=status:ACTIVE --format="value(account)" &> /dev/null; then
        error_exit "Not authenticated with gcloud. Run: gcloud auth login"
    fi
    log SUCCESS "gcloud authentication verified"

    # Display current account
    CURRENT_ACCOUNT=$(gcloud auth list --filter=status:ACTIVE --format="value(account)")
    log INFO "Using account: $CURRENT_ACCOUNT"
}

# Interactive configuration
configure_project() {
    log STEP "Configuring GCP project parameters..."

    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "  BYO RAG System - GCP Project Configuration"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""

    # Project ID (must be globally unique)
    read -p "Enter GCP Project ID (e.g., byo-rag-system-dev): " PROJECT_ID

    # Validate project ID format
    if ! [[ "$PROJECT_ID" =~ ^[a-z][a-z0-9-]{4,28}[a-z0-9]$ ]]; then
        error_exit "Invalid project ID format. Must be 6-30 characters, lowercase letters, numbers, and hyphens."
    fi

    # Project Name
    read -p "Enter Project Name (e.g., BYO RAG System - Dev): " PROJECT_NAME
    PROJECT_NAME=${PROJECT_NAME:-"BYO RAG System"}

    # Region
    echo ""
    echo "Recommended regions for US:"
    echo "  1) us-central1 (Iowa)"
    echo "  2) us-east1 (South Carolina)"
    echo "  3) us-west1 (Oregon)"
    echo "  4) us-east4 (Northern Virginia)"
    read -p "Enter region [us-central1]: " REGION
    REGION=${REGION:-"us-central1"}

    # Zone
    read -p "Enter zone [${REGION}-a]: " ZONE
    ZONE=${ZONE:-"${REGION}-a"}

    # Billing account
    echo ""
    log INFO "Fetching billing accounts..."
    gcloud beta billing accounts list
    echo ""
    read -p "Enter Billing Account ID: " BILLING_ACCOUNT_ID

    # Budget
    read -p "Enter monthly budget limit in USD [1000]: " BUDGET_AMOUNT
    BUDGET_AMOUNT=${BUDGET_AMOUNT:-1000}

    # Environment
    echo ""
    echo "Select environment:"
    echo "  1) dev"
    echo "  2) staging"
    echo "  3) production"
    read -p "Enter environment [dev]: " ENV_INPUT

    case $ENV_INPUT in
        1|dev|"")
            ENVIRONMENT="dev"
            ;;
        2|staging)
            ENVIRONMENT="staging"
            ;;
        3|production)
            ENVIRONMENT="production"
            ;;
        *)
            error_exit "Invalid environment selection"
            ;;
    esac

    # Save configuration
    cat > "${CONFIG_DIR}/project-config.env" <<EOF
# GCP Project Configuration
# Generated: $(date)

PROJECT_ID="${PROJECT_ID}"
PROJECT_NAME="${PROJECT_NAME}"
REGION="${REGION}"
ZONE="${ZONE}"
BILLING_ACCOUNT_ID="${BILLING_ACCOUNT_ID}"
BUDGET_AMOUNT=${BUDGET_AMOUNT}
ENVIRONMENT="${ENVIRONMENT}"

# Network configuration
VPC_NAME="rag-vpc"
SUBNET_NAME="rag-gke-subnet"
SUBNET_RANGE="10.0.0.0/20"
PODS_RANGE="10.4.0.0/14"
SERVICES_RANGE="10.8.0.0/20"

# Service accounts
GKE_SA_NAME="gke-node-sa"
CLOUDSQL_SA_NAME="cloudsql-proxy-sa"
CLOUDBUILD_SA_NAME="cloud-build-sa"
EOF

    log SUCCESS "Configuration saved to: ${CONFIG_DIR}/project-config.env"

    # Display configuration
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "  Configuration Summary"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "  Project ID:        ${PROJECT_ID}"
    echo "  Project Name:      ${PROJECT_NAME}"
    echo "  Region:            ${REGION}"
    echo "  Zone:              ${ZONE}"
    echo "  Billing Account:   ${BILLING_ACCOUNT_ID}"
    echo "  Budget:            \$${BUDGET_AMOUNT}/month"
    echo "  Environment:       ${ENVIRONMENT}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""

    read -p "Proceed with this configuration? (yes/no): " CONFIRM
    if [[ "$CONFIRM" != "yes" ]]; then
        error_exit "Configuration cancelled by user"
    fi

    # Export variables
    export PROJECT_ID REGION ZONE BILLING_ACCOUNT_ID BUDGET_AMOUNT ENVIRONMENT
}

# Create GCP project
create_project() {
    log STEP "Creating GCP project: ${PROJECT_ID}..."

    # Check if project already exists
    if gcloud projects describe "${PROJECT_ID}" &>/dev/null; then
        log WARN "Project ${PROJECT_ID} already exists"
        read -p "Use existing project? (yes/no): " USE_EXISTING
        if [[ "$USE_EXISTING" != "yes" ]]; then
            error_exit "Please choose a different project ID"
        fi
        log INFO "Using existing project: ${PROJECT_ID}"
    else
        # Create project
        if gcloud projects create "${PROJECT_ID}" --name="${PROJECT_NAME}"; then
            log SUCCESS "Project created: ${PROJECT_ID}"
        else
            error_exit "Failed to create project"
        fi
    fi

    # Set as default project
    gcloud config set project "${PROJECT_ID}"
    log SUCCESS "Set ${PROJECT_ID} as default project"
}

# Enable billing
enable_billing() {
    log STEP "Enabling billing for project..."

    # Check if billing is already enabled
    CURRENT_BILLING=$(gcloud beta billing projects describe "${PROJECT_ID}" \
        --format="value(billingAccountName)" 2>/dev/null || echo "")

    if [[ -n "$CURRENT_BILLING" ]]; then
        log WARN "Billing already enabled: ${CURRENT_BILLING}"
    else
        # Link billing account
        if gcloud beta billing projects link "${PROJECT_ID}" \
            --billing-account="${BILLING_ACCOUNT_ID}"; then
            log SUCCESS "Billing enabled for project"
        else
            error_exit "Failed to enable billing"
        fi
    fi
}

# Enable required APIs
enable_apis() {
    log STEP "Enabling required GCP APIs..."

    # List of required APIs
    APIS=(
        "compute.googleapis.com"             # Compute Engine
        "container.googleapis.com"           # GKE
        "sqladmin.googleapis.com"            # Cloud SQL
        "redis.googleapis.com"               # Memorystore
        "artifactregistry.googleapis.com"    # Artifact Registry
        "secretmanager.googleapis.com"       # Secret Manager
        "cloudbuild.googleapis.com"          # Cloud Build
        "logging.googleapis.com"             # Cloud Logging
        "monitoring.googleapis.com"          # Cloud Monitoring
        "cloudtrace.googleapis.com"          # Cloud Trace
        "servicenetworking.googleapis.com"   # Private Service Access
        "dns.googleapis.com"                 # Cloud DNS
        "cloudresourcemanager.googleapis.com" # Resource Manager
        "iam.googleapis.com"                 # IAM
        "iamcredentials.googleapis.com"      # IAM Credentials
    )

    log INFO "Enabling ${#APIS[@]} APIs..."

    # Enable APIs (can do all at once)
    if gcloud services enable "${APIS[@]}" --project="${PROJECT_ID}"; then
        log SUCCESS "All APIs enabled successfully"
    else
        error_exit "Failed to enable APIs"
    fi

    # Wait for APIs to be fully enabled
    log INFO "Waiting for APIs to propagate (30 seconds)..."
    sleep 30

    # Verify APIs are enabled
    log INFO "Verifying API status..."
    for api in "${APIS[@]}"; do
        if gcloud services list --enabled --project="${PROJECT_ID}" | grep -q "$api"; then
            log SUCCESS "  ✓ $api"
        else
            log WARN "  ✗ $api (may need more time)"
        fi
    done
}

# Generate configuration summary
generate_summary() {
    log STEP "Generating setup summary..."

    SUMMARY_FILE="${LOG_DIR}/setup-summary-$(date +%Y%m%d-%H%M%S).txt"

    cat > "$SUMMARY_FILE" <<EOF
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  BYO RAG System - GCP Project Setup Summary
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Setup Date: $(date)
Script Version: GCP-INFRA-001

PROJECT INFORMATION
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Project ID:          ${PROJECT_ID}
Project Name:        ${PROJECT_NAME}
Project Number:      $(gcloud projects describe ${PROJECT_ID} --format="value(projectNumber)")
Region:              ${REGION}
Zone:                ${ZONE}
Environment:         ${ENVIRONMENT}

BILLING CONFIGURATION
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Billing Account:     ${BILLING_ACCOUNT_ID}
Budget Limit:        \$${BUDGET_AMOUNT}/month
Budget Alerts:       50%, 75%, 90%, 100%

ENABLED APIS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
$(gcloud services list --enabled --project="${PROJECT_ID}" | tail -n +2)

NEXT STEPS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1. Run ./01-setup-network.sh to configure VPC and networking
2. Run ./02-setup-service-accounts.sh to create IAM service accounts
3. Run ./03-setup-budget-alerts.sh to configure budget monitoring

CONFIGURATION FILES
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Project Config:      ${CONFIG_DIR}/project-config.env
Setup Log:           ${LOG_FILE}
Summary:             ${SUMMARY_FILE}

USEFUL COMMANDS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# Set default project
gcloud config set project ${PROJECT_ID}

# View project info
gcloud projects describe ${PROJECT_ID}

# View billing info
gcloud beta billing projects describe ${PROJECT_ID}

# List enabled APIs
gcloud services list --enabled

# View project in console
https://console.cloud.google.com/home/dashboard?project=${PROJECT_ID}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
EOF

    cat "$SUMMARY_FILE"
    log SUCCESS "Summary saved to: ${SUMMARY_FILE}"
}

# Main execution
main() {
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "  BYO RAG System - GCP Project Setup"
    echo "  Story: GCP-INFRA-001 (Step 1 of 4)"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""

    log INFO "Starting GCP project setup..."
    log INFO "Log file: ${LOG_FILE}"
    echo ""

    # Execute setup steps
    check_prerequisites
    configure_project
    create_project
    enable_billing
    enable_apis
    generate_summary

    echo ""
    log SUCCESS "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    log SUCCESS "  GCP Project Setup Complete!"
    log SUCCESS "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    log INFO "Next step: Run ./01-setup-network.sh"
    echo ""
}

# Run main function
main "$@"

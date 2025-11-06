#!/bin/bash

################################################################################
# GCP-INFRA-001: Service Accounts and IAM Configuration
#
# This script creates service accounts with least-privilege access for:
#   - GKE node pools
#   - Cloud SQL Proxy
#   - Cloud Build automation
#
# Prerequisites:
#   - 00-setup-project.sh completed
#   - 01-setup-network.sh completed
#   - Project config file exists
#
# Usage:
#   ./02-setup-service-accounts.sh
#
# Story: GCP-INFRA-001 (Step 3 of 4)
################################################################################

set -euo pipefail

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
CONFIG_DIR="${SCRIPT_DIR}/config"
LOG_DIR="${PROJECT_ROOT}/logs/gcp-setup"

# Ensure log directory exists
mkdir -p "${LOG_DIR}"

# Logging configuration
LOG_FILE="${LOG_DIR}/service-accounts-setup-$(date +%Y%m%d-%H%M%S).log"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

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

# Load configuration
load_config() {
    log STEP "Loading project configuration..."

    if [[ ! -f "${CONFIG_DIR}/project-config.env" ]]; then
        error_exit "Project configuration not found. Run ./00-setup-project.sh first."
    fi

    # shellcheck disable=SC1091
    source "${CONFIG_DIR}/project-config.env"

    log SUCCESS "Configuration loaded for project: ${PROJECT_ID}"

    # Set gcloud project
    gcloud config set project "${PROJECT_ID}"
}

# Create GKE node service account
create_gke_service_account() {
    log STEP "Creating GKE node service account..."

    SA_EMAIL="${GKE_SA_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"

    # Check if service account exists
    if gcloud iam service-accounts describe "${SA_EMAIL}" &>/dev/null; then
        log WARN "Service account ${GKE_SA_NAME} already exists"
    else
        # Create service account
        if gcloud iam service-accounts create "${GKE_SA_NAME}" \
            --display-name="GKE Node Service Account" \
            --description="Service account for GKE node pools with minimal permissions" \
            --project="${PROJECT_ID}"; then
            log SUCCESS "Service account created: ${GKE_SA_NAME}"
        else
            error_exit "Failed to create GKE service account"
        fi
    fi

    # Grant necessary roles for GKE nodes
    log INFO "Granting IAM roles to GKE service account..."

    ROLES=(
        "roles/logging.logWriter"           # Write logs
        "roles/monitoring.metricWriter"     # Write metrics
        "roles/monitoring.viewer"           # Read metrics
        "roles/artifactregistry.reader"     # Pull container images
    )

    for role in "${ROLES[@]}"; do
        if gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
            --member="serviceAccount:${SA_EMAIL}" \
            --role="${role}" \
            --condition=None \
            >/dev/null 2>&1; then
            log SUCCESS "  ✓ Granted ${role}"
        else
            log WARN "  ! Failed to grant ${role} (may already exist)"
        fi
    done
}

# Create Cloud SQL Proxy service account
create_cloudsql_service_account() {
    log STEP "Creating Cloud SQL Proxy service account..."

    SA_EMAIL="${CLOUDSQL_SA_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"

    # Check if service account exists
    if gcloud iam service-accounts describe "${SA_EMAIL}" &>/dev/null; then
        log WARN "Service account ${CLOUDSQL_SA_NAME} already exists"
    else
        # Create service account
        if gcloud iam service-accounts create "${CLOUDSQL_SA_NAME}" \
            --display-name="Cloud SQL Proxy Service Account" \
            --description="Service account for Cloud SQL Proxy sidecar containers" \
            --project="${PROJECT_ID}"; then
            log SUCCESS "Service account created: ${CLOUDSQL_SA_NAME}"
        else
            error_exit "Failed to create Cloud SQL Proxy service account"
        fi
    fi

    # Grant Cloud SQL Client role
    log INFO "Granting IAM roles to Cloud SQL Proxy service account..."

    if gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
        --member="serviceAccount:${SA_EMAIL}" \
        --role="roles/cloudsql.client" \
        --condition=None \
        >/dev/null 2>&1; then
        log SUCCESS "  ✓ Granted roles/cloudsql.client"
    else
        log WARN "  ! Failed to grant role (may already exist)"
    fi
}

# Create Cloud Build service account
create_cloudbuild_service_account() {
    log STEP "Creating Cloud Build service account..."

    SA_EMAIL="${CLOUDBUILD_SA_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"

    # Check if service account exists
    if gcloud iam service-accounts describe "${SA_EMAIL}" &>/dev/null; then
        log WARN "Service account ${CLOUDBUILD_SA_NAME} already exists"
    else
        # Create service account
        if gcloud iam service-accounts create "${CLOUDBUILD_SA_NAME}" \
            --display-name="Cloud Build Service Account" \
            --description="Service account for Cloud Build CI/CD pipelines" \
            --project="${PROJECT_ID}"; then
            log SUCCESS "Service account created: ${CLOUDBUILD_SA_NAME}"
        else
            error_exit "Failed to create Cloud Build service account"
        fi
    fi

    # Grant necessary roles for Cloud Build
    log INFO "Granting IAM roles to Cloud Build service account..."

    ROLES=(
        "roles/cloudbuild.builds.builder"    # Run builds
        "roles/artifactregistry.writer"      # Push images
        "roles/container.developer"          # Deploy to GKE
        "roles/iam.serviceAccountUser"       # Use service accounts
    )

    for role in "${ROLES[@]}"; do
        if gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
            --member="serviceAccount:${SA_EMAIL}" \
            --role="${role}" \
            --condition=None \
            >/dev/null 2>&1; then
            log SUCCESS "  ✓ Granted ${role}"
        else
            log WARN "  ! Failed to grant ${role} (may already exist)"
        fi
    done
}

# Configure Workload Identity bindings (for GKE)
configure_workload_identity() {
    log STEP "Preparing Workload Identity configuration..."

    # Save Workload Identity binding commands for later (after GKE cluster is created)
    WORKLOAD_IDENTITY_CONFIG="${CONFIG_DIR}/workload-identity-bindings.sh"

    cat > "${WORKLOAD_IDENTITY_CONFIG}" <<'WIEOF'
#!/bin/bash
# Workload Identity Bindings
# Run this after GKE cluster is created

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
source "${SCRIPT_DIR}/project-config.env"

echo "Configuring Workload Identity bindings..."

# Bind Cloud SQL Proxy service account to Kubernetes service account
gcloud iam service-accounts add-iam-policy-binding \
  ${CLOUDSQL_SA_NAME}@${PROJECT_ID}.iam.gserviceaccount.com \
  --role roles/iam.workloadIdentityUser \
  --member "serviceAccount:${PROJECT_ID}.svc.id.goog[rag-system/cloudsql-proxy]"

echo "✓ Workload Identity bindings configured"
WIEOF

    chmod +x "${WORKLOAD_IDENTITY_CONFIG}"
    log SUCCESS "Workload Identity config saved: ${WORKLOAD_IDENTITY_CONFIG}"
    log INFO "Run this script after GKE cluster creation"
}

# Export service account keys (for local development only)
export_service_account_keys() {
    log STEP "Exporting service account keys for local development..."

    KEYS_DIR="${CONFIG_DIR}/service-account-keys"
    mkdir -p "${KEYS_DIR}"

    # Add to .gitignore
    if [[ ! -f "${PROJECT_ROOT}/.gitignore" ]] || ! grep -q "service-account-keys" "${PROJECT_ROOT}/.gitignore"; then
        echo "" >> "${PROJECT_ROOT}/.gitignore"
        echo "# GCP Service Account Keys (DO NOT COMMIT)" >> "${PROJECT_ROOT}/.gitignore"
        echo "scripts/gcp/config/service-account-keys/" >> "${PROJECT_ROOT}/.gitignore"
    fi

    # Export Cloud SQL Proxy key
    SA_EMAIL="${CLOUDSQL_SA_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"
    KEY_FILE="${KEYS_DIR}/${CLOUDSQL_SA_NAME}-key.json"

    if [[ ! -f "${KEY_FILE}" ]]; then
        gcloud iam service-accounts keys create "${KEY_FILE}" \
            --iam-account="${SA_EMAIL}" \
            --project="${PROJECT_ID}"
        log SUCCESS "Exported key: ${CLOUDSQL_SA_NAME}-key.json"
        log WARN "Keep this key secure! Do not commit to version control."
    else
        log WARN "Key file already exists: ${KEY_FILE}"
    fi
}

# Verify service accounts
verify_service_accounts() {
    log STEP "Verifying service accounts..."

    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "  Service Accounts Verification"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""

    # List service accounts
    gcloud iam service-accounts list \
        --project="${PROJECT_ID}" \
        --format="table(email,displayName,disabled)"

    echo ""
    log SUCCESS "Service accounts verification complete"
}

# Generate service accounts summary
generate_summary() {
    log STEP "Generating service accounts summary..."

    SUMMARY_FILE="${LOG_DIR}/service-accounts-summary-$(date +%Y%m%d-%H%M%S).txt"

    cat > "$SUMMARY_FILE" <<EOF
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  BYO RAG System - Service Accounts Summary
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Setup Date: $(date)
Project ID: ${PROJECT_ID}

SERVICE ACCOUNTS CREATED
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

1. GKE Node Service Account
   Email:        ${GKE_SA_NAME}@${PROJECT_ID}.iam.gserviceaccount.com
   Purpose:      GKE node pools
   Roles:
     - roles/logging.logWriter
     - roles/monitoring.metricWriter
     - roles/monitoring.viewer
     - roles/artifactregistry.reader

2. Cloud SQL Proxy Service Account
   Email:        ${CLOUDSQL_SA_NAME}@${PROJECT_ID}.iam.gserviceaccount.com
   Purpose:      Cloud SQL Proxy sidecar
   Roles:
     - roles/cloudsql.client

3. Cloud Build Service Account
   Email:        ${CLOUDBUILD_SA_NAME}@${PROJECT_ID}.iam.gserviceaccount.com
   Purpose:      CI/CD automation
   Roles:
     - roles/cloudbuild.builds.builder
     - roles/artifactregistry.writer
     - roles/container.developer
     - roles/iam.serviceAccountUser

WORKLOAD IDENTITY
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Configuration script: ${CONFIG_DIR}/workload-identity-bindings.sh
Run after GKE cluster creation

SERVICE ACCOUNT KEYS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Location: ${CONFIG_DIR}/service-account-keys/
⚠️  WARNING: Keep keys secure! Never commit to version control.

NEXT STEPS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1. Run ./03-setup-budget-alerts.sh to configure budget monitoring
2. After GKE cluster creation, run workload-identity-bindings.sh

USEFUL COMMANDS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# List service accounts
gcloud iam service-accounts list --project=${PROJECT_ID}

# View service account details
gcloud iam service-accounts describe ${GKE_SA_NAME}@${PROJECT_ID}.iam.gserviceaccount.com

# View IAM policy for project
gcloud projects get-iam-policy ${PROJECT_ID}

# View service accounts in console
https://console.cloud.google.com/iam-admin/serviceaccounts?project=${PROJECT_ID}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
EOF

    cat "$SUMMARY_FILE"
    log SUCCESS "Summary saved to: ${SUMMARY_FILE}"
}

# Main execution
main() {
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "  BYO RAG System - Service Accounts Configuration"
    echo "  Story: GCP-INFRA-001 (Step 3 of 4)"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""

    log INFO "Starting service accounts configuration..."
    log INFO "Log file: ${LOG_FILE}"
    echo ""

    # Execute setup steps
    load_config
    create_gke_service_account
    create_cloudsql_service_account
    create_cloudbuild_service_account
    configure_workload_identity
    export_service_account_keys
    verify_service_accounts
    generate_summary

    echo ""
    log SUCCESS "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    log SUCCESS "  Service Accounts Configuration Complete!"
    log SUCCESS "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    log INFO "Next step: Run ./03-setup-budget-alerts.sh"
    echo ""
}

# Run main function
main "$@"

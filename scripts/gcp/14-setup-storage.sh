#!/usr/bin/env bash

################################################################################
# GCP-STORAGE-009: Persistent Storage Configuration
# 
# This script provisions Cloud Storage buckets for:
# - Document uploads and processed files
# - Volume snapshots and backups
# - Data exports and archives
#
# Usage:
#   ./14-setup-storage.sh --env dev|prod [--project PROJECT_ID] [--region REGION]
#
# Prerequisites:
#   - GCP project created (GCP-INFRA-001)
#   - gcloud CLI authenticated
#   - Storage Admin role (roles/storage.admin)
################################################################################

set -euo pipefail

# Color codes for output
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m' # No Color

# Default values
ENVIRONMENT=""
PROJECT_ID=""
REGION="us-central1"
DRY_RUN=false

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

################################################################################
# Logging Functions
################################################################################

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
    echo -e "${RED}[ERROR]${NC} $1" >&2
}

print_header() {
    echo -e "\n${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"
}

################################################################################
# Validation Functions
################################################################################

validate_prerequisites() {
    print_header "Validating Prerequisites"
    
    # Check gcloud CLI
    if ! command -v gcloud &> /dev/null; then
        log_error "gcloud CLI not found. Please install: https://cloud.google.com/sdk/docs/install"
        exit 1
    fi
    log_success "gcloud CLI found"
    
    # Check authentication
    if ! gcloud auth list --filter=status:ACTIVE --format="value(account)" &> /dev/null; then
        log_error "Not authenticated with gcloud. Run: gcloud auth login"
        exit 1
    fi
    log_success "gcloud authenticated"
    
    # Check gsutil
    if ! command -v gsutil &> /dev/null; then
        log_error "gsutil not found. Install with: gcloud components install gsutil"
        exit 1
    fi
    log_success "gsutil found"
}

validate_project() {
    print_header "Validating Project: $PROJECT_ID"
    
    if ! gcloud projects describe "$PROJECT_ID" &> /dev/null; then
        log_error "Project $PROJECT_ID not found or insufficient permissions"
        exit 1
    fi
    log_success "Project $PROJECT_ID exists and is accessible"
    
    # Set active project
    gcloud config set project "$PROJECT_ID" --quiet
}

################################################################################
# Bucket Creation Functions
################################################################################

create_bucket() {
    local bucket_name=$1
    local purpose=$2
    local lifecycle_age=${3:-0}
    
    log_info "Creating bucket: gs://$bucket_name"
    
    # Check if bucket exists
    if gsutil ls -b "gs://$bucket_name" &> /dev/null; then
        log_warning "Bucket gs://$bucket_name already exists"
        return 0
    fi
    
    # Create bucket with regional storage
    if ! gsutil mb -c STANDARD -l "$REGION" -b on "gs://$bucket_name"; then
        log_error "Failed to create bucket gs://$bucket_name"
        return 1
    fi
    
    # Enable versioning for backup buckets
    if [[ "$purpose" == "backups" || "$purpose" == "snapshots" ]]; then
        log_info "Enabling versioning for gs://$bucket_name"
        gsutil versioning set on "gs://$bucket_name"
    fi
    
    # Set lifecycle policy if specified
    if [[ $lifecycle_age -gt 0 ]]; then
        log_info "Setting lifecycle policy: delete after $lifecycle_age days"
        local lifecycle_file=$(mktemp)
        cat > "$lifecycle_file" <<EOF
{
  "lifecycle": {
    "rule": [
      {
        "action": {"type": "Delete"},
        "condition": {"age": $lifecycle_age}
      }
    ]
  }
}
EOF
        gsutil lifecycle set "$lifecycle_file" "gs://$bucket_name"
        rm "$lifecycle_file"
    fi
    
    # Set uniform bucket-level access
    log_info "Setting uniform bucket-level access"
    gsutil uniformbucketlevelaccess set on "gs://$bucket_name"
    
    # Add labels
    log_info "Adding labels"
    gsutil label ch -l "environment:$ENVIRONMENT" "gs://$bucket_name"
    gsutil label ch -l "purpose:$purpose" "gs://$bucket_name"
    gsutil label ch -l "managed-by:script" "gs://$bucket_name"
    
    log_success "Bucket gs://$bucket_name created successfully"
}

configure_bucket_iam() {
    local bucket_name=$1
    local purpose=$2
    
    log_info "Configuring IAM for gs://$bucket_name"
    
    # Grant GKE nodes access (for document uploads)
    if [[ "$purpose" == "documents" ]]; then
        local gke_sa="gke-node-sa@${PROJECT_ID}.iam.gserviceaccount.com"
        gsutil iam ch "serviceAccount:${gke_sa}:objectAdmin" "gs://$bucket_name"
        log_success "Granted objectAdmin to $gke_sa"
    fi
    
    # Grant backup service account access
    if [[ "$purpose" == "backups" || "$purpose" == "snapshots" ]]; then
        local backup_sa="backup-sa@${PROJECT_ID}.iam.gserviceaccount.com"
        
        # Create backup service account if it doesn't exist
        if ! gcloud iam service-accounts describe "$backup_sa" --project="$PROJECT_ID" &> /dev/null; then
            log_info "Creating backup service account"
            gcloud iam service-accounts create backup-sa \
                --display-name="Backup Service Account" \
                --description="Service account for automated backups and snapshots" \
                --project="$PROJECT_ID"
        fi
        
        gsutil iam ch "serviceAccount:${backup_sa}:objectAdmin" "gs://$bucket_name"
        log_success "Granted objectAdmin to $backup_sa"
    fi
}

################################################################################
# Main Storage Setup
################################################################################

setup_storage() {
    print_header "Setting Up Cloud Storage Buckets"
    
    local bucket_prefix="${PROJECT_ID}-rag"
    
    # 1. Documents bucket (primary storage)
    log_info "\n[1/4] Creating documents bucket"
    local documents_bucket="${bucket_prefix}-documents-${ENVIRONMENT}"
    create_bucket "$documents_bucket" "documents" 0
    configure_bucket_iam "$documents_bucket" "documents"
    
    # 2. Backups bucket (long-term storage)
    log_info "\n[2/4] Creating backups bucket"
    local backups_bucket="${bucket_prefix}-backups-${ENVIRONMENT}"
    create_bucket "$backups_bucket" "backups" 90  # Auto-delete after 90 days
    configure_bucket_iam "$backups_bucket" "backups"
    
    # 3. Snapshots bucket (volume snapshots)
    log_info "\n[3/4] Creating snapshots bucket"
    local snapshots_bucket="${bucket_prefix}-snapshots-${ENVIRONMENT}"
    create_bucket "$snapshots_bucket" "snapshots" 30  # Auto-delete after 30 days
    configure_bucket_iam "$snapshots_bucket" "snapshots"
    
    # 4. Exports bucket (data exports)
    log_info "\n[4/4] Creating exports bucket"
    local exports_bucket="${bucket_prefix}-exports-${ENVIRONMENT}"
    create_bucket "$exports_bucket" "exports" 30  # Auto-delete after 30 days
    configure_bucket_iam "$exports_bucket" "exports"
    
    print_summary "$documents_bucket" "$backups_bucket" "$snapshots_bucket" "$exports_bucket"
}

################################################################################
# Summary and Output
################################################################################

print_summary() {
    local documents_bucket=$1
    local backups_bucket=$2
    local snapshots_bucket=$3
    local exports_bucket=$4
    
    print_header "Storage Setup Complete"
    
    cat <<EOF
${GREEN}✓${NC} Cloud Storage buckets created successfully

${BLUE}Bucket Summary:${NC}
┌─────────────────────────────────────────────────────────────────────┐
│ ${YELLOW}Documents Bucket${NC}                                                  │
│   Name: gs://${documents_bucket}
│   Purpose: Primary document storage for uploaded files              │
│   Lifecycle: No automatic deletion                                  │
│   Access: gke-node-sa (objectAdmin)                                 │
├─────────────────────────────────────────────────────────────────────┤
│ ${YELLOW}Backups Bucket${NC}                                                    │
│   Name: gs://${backups_bucket}
│   Purpose: Long-term backups and archives                           │
│   Lifecycle: Delete after 90 days                                   │
│   Versioning: Enabled                                               │
│   Access: backup-sa (objectAdmin)                                   │
├─────────────────────────────────────────────────────────────────────┤
│ ${YELLOW}Snapshots Bucket${NC}                                                  │
│   Name: gs://${snapshots_bucket}
│   Purpose: Volume snapshot exports                                  │
│   Lifecycle: Delete after 30 days                                   │
│   Versioning: Enabled                                               │
│   Access: backup-sa (objectAdmin)                                   │
├─────────────────────────────────────────────────────────────────────┤
│ ${YELLOW}Exports Bucket${NC}                                                    │
│   Name: gs://${exports_bucket}
│   Purpose: Data exports and temporary files                         │
│   Lifecycle: Delete after 30 days                                   │
│   Access: gke-node-sa (objectAdmin)                                 │
└─────────────────────────────────────────────────────────────────────┘

${BLUE}Cost Estimate:${NC}
- Storage: ~\$0.02/GB/month (Standard storage)
- Operations: ~\$0.05/10k operations
- Estimated monthly cost: \$20-50 (for 100GB-500GB)

${BLUE}Next Steps:${NC}
1. Update document service to use Cloud Storage:
   - Add GOOGLE_CLOUD_STORAGE_BUCKET env var: ${documents_bucket}
   - Enable Cloud Storage API in application.yml
   
2. Configure automated backups:
   - Run: ./15-configure-snapshots.sh --env $ENVIRONMENT
   
3. Set up monitoring:
   - Create Cloud Monitoring alerts for bucket usage
   - Set up logging for bucket access

${BLUE}Verification Commands:${NC}
# List all buckets
gsutil ls -p $PROJECT_ID

# Check bucket details
gsutil ls -L -b gs://${documents_bucket}

# Test upload
echo "test" | gsutil cp - gs://${documents_bucket}/test.txt

# View IAM policies
gsutil iam get gs://${documents_bucket}

${YELLOW}Documentation:${NC}
- See docs/deployment/PERSISTENT_STORAGE_GUIDE.md for detailed configuration
- Backup procedures: docs/operations/BACKUP_RESTORE_PROCEDURES.md

EOF
}

################################################################################
# Usage and Argument Parsing
################################################################################

usage() {
    cat <<EOF
Usage: $0 --env <environment> [OPTIONS]

Provisions Cloud Storage buckets for RAG system persistent storage.

Required Arguments:
    --env ENV           Environment: dev or prod

Optional Arguments:
    --project ID        GCP project ID (default: current project)
    --region REGION     GCP region (default: us-central1)
    --dry-run           Print actions without executing
    -h, --help          Show this help message

Examples:
    # Development environment
    $0 --env dev

    # Production with specific project
    $0 --env prod --project byo-rag-prod --region us-east1

    # Dry run
    $0 --env dev --dry-run

EOF
}

parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --env)
                ENVIRONMENT="$2"
                shift 2
                ;;
            --project)
                PROJECT_ID="$2"
                shift 2
                ;;
            --region)
                REGION="$2"
                shift 2
                ;;
            --dry-run)
                DRY_RUN=true
                shift
                ;;
            -h|--help)
                usage
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                usage
                exit 1
                ;;
        esac
    done
    
    # Validate required arguments
    if [[ -z "$ENVIRONMENT" ]]; then
        log_error "Environment (--env) is required"
        usage
        exit 1
    fi
    
    if [[ "$ENVIRONMENT" != "dev" && "$ENVIRONMENT" != "prod" ]]; then
        log_error "Environment must be 'dev' or 'prod'"
        exit 1
    fi
    
    # Set project ID if not provided
    if [[ -z "$PROJECT_ID" ]]; then
        PROJECT_ID=$(gcloud config get-value project 2>/dev/null || echo "")
        if [[ -z "$PROJECT_ID" ]]; then
            log_error "No project ID specified and no default project configured"
            log_error "Set with: gcloud config set project PROJECT_ID"
            log_error "Or use: --project PROJECT_ID"
            exit 1
        fi
    fi
}

################################################################################
# Main Execution
################################################################################

main() {
    parse_arguments "$@"
    
    print_header "GCP Storage Setup - Environment: $ENVIRONMENT"
    
    log_info "Project: $PROJECT_ID"
    log_info "Region: $REGION"
    log_info "Environment: $ENVIRONMENT"
    
    if [[ "$DRY_RUN" == "true" ]]; then
        log_warning "DRY RUN MODE - No changes will be made"
        exit 0
    fi
    
    validate_prerequisites
    validate_project
    setup_storage
    
    log_success "\n✓ Storage setup complete!"
    log_info "Review the summary above for next steps"
}

# Run main function
main "$@"

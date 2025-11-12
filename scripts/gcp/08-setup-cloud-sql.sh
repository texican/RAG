#!/usr/bin/env bash

###############################################################################
# GCP-SQL-004: Cloud SQL PostgreSQL Setup
# 
# This script creates and configures a Cloud SQL PostgreSQL instance with
# pgvector extension for the RAG system.
#
# Prerequisites:
#   - GCP project configured (byo-rag-dev)
#   - gcloud CLI authenticated
#   - Sufficient IAM permissions for Cloud SQL
#
# Usage:
#   ./scripts/gcp/08-setup-cloud-sql.sh [--dry-run]
#
###############################################################################

set -euo pipefail

# Color codes for output
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly CYAN='\033[0;36m'
readonly NC='\033[0m' # No Color

# Configuration
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_ID="${GCP_PROJECT_ID:-byo-rag-dev}"
readonly REGION="${GCP_REGION:-us-central1}"
readonly INSTANCE_NAME="rag-postgres"
readonly DB_VERSION="POSTGRES_15"
readonly TIER="db-custom-2-7680"  # 2 vCPU, 7.5 GB RAM
readonly STORAGE_SIZE="20"  # GB
readonly STORAGE_TYPE="SSD"

# Database configuration
readonly ROOT_PASSWORD="$(openssl rand -base64 32)"
readonly APP_USER="rag_user"
readonly APP_PASSWORD="$(openssl rand -base64 32)"

# Databases to create
declare -a DATABASES=(
    "byo_rag_dev"
)

# Flags
DRY_RUN=false

###############################################################################
# Helper Functions
###############################################################################

log_info() {
    echo -e "${CYAN}ℹ${NC} $*"
}

log_success() {
    echo -e "${GREEN}✓${NC} $*"
}

log_error() {
    echo -e "${RED}✗${NC} $*" >&2
}

log_warning() {
    echo -e "${YELLOW}⚠${NC} $*"
}

print_header() {
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}$*${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
}

execute_command() {
    local description="$1"
    shift
    
    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "[DRY RUN] Would execute: $*"
        return 0
    fi
    
    log_info "$description"
    if "$@"; then
        log_success "$description complete"
        return 0
    else
        log_error "$description failed"
        return 1
    fi
}

###############################################################################
# Validation Functions
###############################################################################

check_prerequisites() {
    print_header "Step 1: Checking Prerequisites"
    
    # Check gcloud
    if ! command -v gcloud &> /dev/null; then
        log_error "gcloud CLI not found. Please install: https://cloud.google.com/sdk/docs/install"
        exit 1
    fi
    log_success "gcloud CLI installed"
    
    # Check authentication
    if ! gcloud auth list --filter=status:ACTIVE --format="value(account)" &> /dev/null; then
        log_error "Not authenticated with gcloud. Run: gcloud auth login"
        exit 1
    fi
    log_success "Authenticated with gcloud"
    
    # Check project
    local current_project
    current_project=$(gcloud config get-value project 2>/dev/null || echo "")
    if [[ "$current_project" != "$PROJECT_ID" ]]; then
        log_warning "Current project is '$current_project', switching to '$PROJECT_ID'"
        gcloud config set project "$PROJECT_ID"
    fi
    log_success "Using project: $PROJECT_ID"
    
    # Check if instance already exists
    if gcloud sql instances describe "$INSTANCE_NAME" --project="$PROJECT_ID" &>/dev/null; then
        log_warning "Cloud SQL instance '$INSTANCE_NAME' already exists"
        read -p "Do you want to continue and reconfigure it? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log_info "Aborted by user"
            exit 0
        fi
    fi
}

###############################################################################
# Cloud SQL Instance Creation
###############################################################################

create_cloud_sql_instance() {
    print_header "Step 2: Creating Cloud SQL PostgreSQL Instance"
    
    if gcloud sql instances describe "$INSTANCE_NAME" --project="$PROJECT_ID" &>/dev/null; then
        log_success "Instance '$INSTANCE_NAME' already exists, skipping creation"
        return 0
    fi
    
    log_info "Creating Cloud SQL instance: $INSTANCE_NAME"
    log_info "  Version: $DB_VERSION"
    log_info "  Tier: $TIER (2 vCPU, 7.5 GB RAM)"
    log_info "  Region: $REGION"
    log_info "  Storage: ${STORAGE_SIZE}GB $STORAGE_TYPE"
    
    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "[DRY RUN] Would create Cloud SQL instance"
        return 0
    fi
    
    gcloud sql instances create "$INSTANCE_NAME" \
        --project="$PROJECT_ID" \
        --database-version="$DB_VERSION" \
        --tier="$TIER" \
        --region="$REGION" \
        --storage-size="$STORAGE_SIZE" \
        --storage-type="$STORAGE_TYPE" \
        --storage-auto-increase \
        --backup \
        --backup-start-time="03:00" \
        --maintenance-window-day="SUN" \
        --maintenance-window-hour="04" \
        --database-flags=cloudsql.iam_authentication=on \
        --availability-type="ZONAL" \
        --root-password="$ROOT_PASSWORD" \
        --network=projects/"$PROJECT_ID"/global/networks/rag-vpc \
        --no-assign-ip
    
    log_success "Cloud SQL instance created successfully"
    
    # Wait for instance to be ready
    log_info "Waiting for instance to be ready..."
    gcloud sql operations wait \
        --project="$PROJECT_ID" \
        $(gcloud sql operations list --instance="$INSTANCE_NAME" --project="$PROJECT_ID" --limit=1 --format="value(name)")
    
    log_success "Instance is ready"
}

###############################################################################
# Database and User Setup
###############################################################################

create_databases() {
    print_header "Step 3: Creating Databases"
    
    for db in "${DATABASES[@]}"; do
        log_info "Creating database: $db"
        
        if [[ "$DRY_RUN" == "true" ]]; then
            log_info "[DRY RUN] Would create database: $db"
            continue
        fi
        
        # Check if database exists
        if gcloud sql databases describe "$db" \
            --instance="$INSTANCE_NAME" \
            --project="$PROJECT_ID" &>/dev/null; then
            log_success "Database '$db' already exists"
        else
            gcloud sql databases create "$db" \
                --instance="$INSTANCE_NAME" \
                --project="$PROJECT_ID" \
                --charset="UTF8" \
                --collation="en_US.UTF8"
            log_success "Database '$db' created"
        fi
    done
}

create_app_user() {
    print_header "Step 4: Creating Application User"
    
    log_info "Creating application user: $APP_USER"
    
    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "[DRY RUN] Would create user: $APP_USER"
        return 0
    fi
    
    # Check if user exists
    if gcloud sql users describe "$APP_USER" \
        --instance="$INSTANCE_NAME" \
        --project="$PROJECT_ID" &>/dev/null; then
        log_warning "User '$APP_USER' already exists, updating password"
        gcloud sql users set-password "$APP_USER" \
            --instance="$INSTANCE_NAME" \
            --project="$PROJECT_ID" \
            --password="$APP_PASSWORD"
    else
        gcloud sql users create "$APP_USER" \
            --instance="$INSTANCE_NAME" \
            --project="$PROJECT_ID" \
            --password="$APP_PASSWORD"
        log_success "User '$APP_USER' created"
    fi
}

###############################################################################
# pgvector Extension Setup
###############################################################################

enable_pgvector() {
    print_header "Step 5: Enabling pgvector Extension"
    
    log_info "Installing pgvector extension on all databases"
    
    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "[DRY RUN] Would enable pgvector extension"
        return 0
    fi
    
    # Get connection name
    local connection_name
    connection_name=$(gcloud sql instances describe "$INSTANCE_NAME" \
        --project="$PROJECT_ID" \
        --format="value(connectionName)")
    
    log_info "Connection name: $connection_name"
    log_warning "pgvector extension must be enabled manually via Cloud SQL proxy or psql"
    log_info "Run these commands after connecting:"
    echo ""
    for db in "${DATABASES[@]}"; do
        echo "  \\c $db"
        echo "  CREATE EXTENSION IF NOT EXISTS vector;"
        echo ""
    done
}

###############################################################################
# Secret Manager Integration
###############################################################################

store_credentials() {
    print_header "Step 6: Storing Credentials in Secret Manager"
    
    # Connection string
    local connection_name
    connection_name=$(gcloud sql instances describe "$INSTANCE_NAME" \
        --project="$PROJECT_ID" \
        --format="value(connectionName)" 2>/dev/null || echo "$PROJECT_ID:$REGION:$INSTANCE_NAME")
    
    local private_ip
    private_ip=$(gcloud sql instances describe "$INSTANCE_NAME" \
        --project="$PROJECT_ID" \
        --format="value(ipAddresses[0].ipAddress)" 2>/dev/null || echo "pending")
    
    # Store root password
    log_info "Storing Cloud SQL root password"
    if [[ "$DRY_RUN" == "false" ]]; then
        echo -n "$ROOT_PASSWORD" | gcloud secrets create cloudsql-root-password \
            --project="$PROJECT_ID" \
            --data-file=- \
            --replication-policy="automatic" 2>/dev/null || \
        echo -n "$ROOT_PASSWORD" | gcloud secrets versions add cloudsql-root-password \
            --project="$PROJECT_ID" \
            --data-file=-
        log_success "Root password stored"
    fi
    
    # Store app user password
    log_info "Storing application user password"
    if [[ "$DRY_RUN" == "false" ]]; then
        echo -n "$APP_PASSWORD" | gcloud secrets create cloudsql-app-password \
            --project="$PROJECT_ID" \
            --data-file=- \
            --replication-policy="automatic" 2>/dev/null || \
        echo -n "$APP_PASSWORD" | gcloud secrets versions add cloudsql-app-password \
            --project="$PROJECT_ID" \
            --data-file=-
        log_success "App password stored"
    fi
    
    # Store connection details
    log_info "Storing connection details"
    local connection_json
    connection_json=$(cat <<EOF
{
  "instance_connection_name": "$connection_name",
  "private_ip": "$private_ip",
  "database_version": "$DB_VERSION",
  "region": "$REGION",
  "username": "$APP_USER",
  "databases": [$(IFS=,; echo "\"${DATABASES[*]}\"" | sed 's/ /","/g')]
}
EOF
)
    
    if [[ "$DRY_RUN" == "false" ]]; then
        echo -n "$connection_json" | gcloud secrets create cloudsql-connection-info \
            --project="$PROJECT_ID" \
            --data-file=- \
            --replication-policy="automatic" 2>/dev/null || \
        echo -n "$connection_json" | gcloud secrets versions add cloudsql-connection-info \
            --project="$PROJECT_ID" \
            --data-file=-
        log_success "Connection info stored"
    fi
}

###############################################################################
# Summary and Next Steps
###############################################################################

print_summary() {
    print_header "Cloud SQL Setup Complete!"
    
    local connection_name
    connection_name=$(gcloud sql instances describe "$INSTANCE_NAME" \
        --project="$PROJECT_ID" \
        --format="value(connectionName)" 2>/dev/null || echo "$PROJECT_ID:$REGION:$INSTANCE_NAME")
    
    local private_ip
    private_ip=$(gcloud sql instances describe "$INSTANCE_NAME" \
        --project="$PROJECT_ID" \
        --format="value(ipAddresses[0].ipAddress)" 2>/dev/null || echo "pending")
    
    echo ""
    echo -e "${GREEN}Instance Details:${NC}"
    echo "  Name: $INSTANCE_NAME"
    echo "  Connection: $connection_name"
    echo "  Private IP: $private_ip"
    echo "  Version: $DB_VERSION"
    echo "  Tier: $TIER"
    echo "  Region: $REGION"
    echo ""
    echo -e "${GREEN}Databases Created:${NC}"
    for db in "${DATABASES[@]}"; do
        echo "  - $db"
    done
    echo ""
    echo -e "${GREEN}Application User:${NC}"
    echo "  Username: $APP_USER"
    echo "  Password: Stored in Secret Manager (cloudsql-app-password)"
    echo ""
    echo -e "${YELLOW}Next Steps:${NC}"
    echo "  1. Enable pgvector extension (manual step required):"
    echo "     gcloud sql connect $INSTANCE_NAME --user=postgres --project=$PROJECT_ID"
    echo ""
    for db in "${DATABASES[@]}"; do
        echo "     \\c $db"
        echo "     CREATE EXTENSION IF NOT EXISTS vector;"
    done
    echo ""
    echo "  2. Update service configurations to use Cloud SQL"
    echo "  3. Set up Cloud SQL Proxy for local development"
    echo "  4. Configure GKE workload identity for Cloud SQL access"
    echo "  5. Run database migrations"
    echo ""
    echo -e "${GREEN}Connection String Format:${NC}"
    echo "  jdbc:postgresql://$private_ip:5432/{database}"
    echo ""
    echo -e "${GREEN}Cloud SQL Proxy Connection:${NC}"
    echo "  cloud-sql-proxy --port 5432 $connection_name"
    echo ""
    echo -e "${GREEN}Console URL:${NC}"
    echo "  https://console.cloud.google.com/sql/instances/$INSTANCE_NAME/overview?project=$PROJECT_ID"
    echo ""
}

###############################################################################
# Main Execution
###############################################################################

main() {
    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --dry-run)
                DRY_RUN=true
                shift
                ;;
            *)
                log_error "Unknown option: $1"
                echo "Usage: $0 [--dry-run]"
                exit 1
                ;;
        esac
    done
    
    echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║     GCP-SQL-004: Cloud SQL PostgreSQL Setup                   ║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo "Project:     $PROJECT_ID"
    echo "Instance:    $INSTANCE_NAME"
    echo "Version:     $DB_VERSION"
    echo "Region:      $REGION"
    echo "Tier:        $TIER"
    echo ""
    
    if [[ "$DRY_RUN" == "true" ]]; then
        log_warning "DRY RUN MODE - No changes will be made"
        echo ""
    fi
    
    check_prerequisites
    create_cloud_sql_instance
    create_databases
    create_app_user
    enable_pgvector
    store_credentials
    print_summary
    
    log_success "GCP-SQL-004 Cloud SQL setup script complete!"
}

main "$@"

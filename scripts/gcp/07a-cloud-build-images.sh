#!/usr/bin/env bash

################################################################################
# RAG System - Cloud Build Image Builder
#
# This script uses Google Cloud Build to build container images.
# Advantages over local builds:
# - Correct architecture (x86) automatically
# - Faster parallel builds
# - No local resources consumed
# - Integrated security scanning
# - Build history and logs
#
# Prerequisites:
# - gcloud CLI authenticated
# - Cloud Build API enabled
# - Artifact Registry repository created
#
# Usage:
#   ./scripts/gcp/07a-cloud-build-images.sh --env dev
#   ./scripts/gcp/07a-cloud-build-images.sh --env prod --service rag-auth-service
#
################################################################################

set -euo pipefail

# Script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Defaults
ENVIRONMENT=""
PROJECT_ID=""
SERVICE="all"
REGION="us-central1"
REPOSITORY="rag-system"
VERSION="latest"
ASYNC=false

################################################################################
# Logging
################################################################################

log_info() {
    echo -e "${BLUE}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $*"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $*"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $*"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $*" >&2
}

################################################################################
# Validation
################################################################################

validate_prerequisites() {
    log_info "Validating prerequisites..."
    
    # Check gcloud
    if ! command -v gcloud &> /dev/null; then
        log_error "gcloud CLI not found"
        exit 1
    fi
    
    # Check authentication
    if ! gcloud auth list --filter=status:ACTIVE --format="value(account)" &> /dev/null; then
        log_error "Not authenticated with gcloud. Run: gcloud auth login"
        exit 1
    fi
    
    # Validate project
    if [[ -z "$PROJECT_ID" ]]; then
        log_error "PROJECT_ID not set"
        exit 1
    fi
    
    # Set active project
    gcloud config set project "$PROJECT_ID" --quiet
    
    # Check Cloud Build API
    if ! gcloud services list --enabled --filter="name:cloudbuild.googleapis.com" --format="value(name)" | grep -q cloudbuild; then
        log_warn "Cloud Build API not enabled. Enabling..."
        gcloud services enable cloudbuild.googleapis.com
    fi
    
    log_success "Prerequisites validated"
}

################################################################################
# Build
################################################################################

build_images() {
    log_info "========================================="
    log_info "Building RAG System Images with Cloud Build"
    log_info "========================================="
    log_info "Project: $PROJECT_ID"
    log_info "Repository: $REPOSITORY"
    log_info "Service: $SERVICE"
    log_info "Version: $VERSION"
    log_info "========================================="
    
    cd "$PROJECT_ROOT"
    
    local build_args=(
        "builds" "submit"
        "--config=cloudbuild.yaml"
        "--project=$PROJECT_ID"
        "--substitutions=_SERVICE=$SERVICE,_VERSION=$VERSION,_REGION=$REGION,_REPOSITORY=$REPOSITORY"
    )
    
    if [[ "$ASYNC" == "true" ]]; then
        build_args+=("--async")
        log_info "Starting asynchronous build..."
    else
        log_info "Starting build (this may take 10-15 minutes)..."
    fi
    
    if gcloud "${build_args[@]}"; then
        log_success "Cloud Build started successfully"
        
        if [[ "$ASYNC" == "false" ]]; then
            log_success "Images built and pushed to Artifact Registry"
            log_info ""
            log_info "Image tags created:"
            log_info "  - ${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPOSITORY}/<service>:latest"
            log_info "  - ${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPOSITORY}/<service>:<git-sha>"
            log_info "  - ${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPOSITORY}/<service>:<build-id>"
        else
            log_info "Build running in background. Check status:"
            log_info "  gcloud builds list --limit=5"
            log_info "  gcloud builds log --stream <BUILD_ID>"
        fi
    else
        log_error "Cloud Build failed"
        exit 1
    fi
}

show_build_logs() {
    log_info "Recent builds:"
    gcloud builds list --limit=5 --format="table(
        id,
        status,
        source.repoSource.branchName,
        createTime.date('%Y-%m-%d %H:%M:%S'),
        duration
    )"
    
    echo ""
    log_info "To view logs for a specific build:"
    log_info "  gcloud builds log <BUILD_ID>"
    log_info "  gcloud builds log --stream <BUILD_ID>  # Follow logs in real-time"
}

################################################################################
# Argument Parsing
################################################################################

usage() {
    cat << EOF
Usage: $0 --env <environment> [OPTIONS]

Build RAG system container images using Google Cloud Build.

Required Arguments:
  --env <environment>       Environment (dev, staging, prod)

Optional Arguments:
  --project <project-id>    GCP project ID (default: byo-rag-{env})
  --service <service-name>  Build only specific service (default: all)
  --version <tag>           Image version tag (default: latest)
  --async                   Start build and return immediately
  --logs                    Show recent build logs and exit
  -h, --help                Show this help message

Services:
  all (default)
  rag-auth-service
  rag-document-service
  rag-embedding-service
  rag-core-service
  rag-admin-service

Examples:
  # Build all services for dev
  $0 --env dev

  # Build specific service
  $0 --env dev --service rag-auth-service

  # Build and tag with specific version
  $0 --env prod --version v1.2.3

  # Start async build
  $0 --env dev --async

  # View recent builds
  $0 --env dev --logs

EOF
    exit 1
}

# Parse arguments
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
        --service)
            SERVICE="$2"
            shift 2
            ;;
        --version)
            VERSION="$2"
            shift 2
            ;;
        --async)
            ASYNC=true
            shift
            ;;
        --logs)
            SHOW_LOGS=true
            shift
            ;;
        -h|--help)
            usage
            ;;
        *)
            log_error "Unknown argument: $1"
            usage
            ;;
    esac
done

# Validate required arguments
if [[ -z "$ENVIRONMENT" ]]; then
    log_error "Environment not specified. Use --env flag."
    usage
fi

# Set defaults based on environment
if [[ -z "$PROJECT_ID" ]]; then
    PROJECT_ID="byo-rag-${ENVIRONMENT}"
fi

################################################################################
# Main
################################################################################

main() {
    log_info "RAG System - Cloud Build Image Builder"
    log_info "======================================="
    
    if [[ "${SHOW_LOGS:-false}" == "true" ]]; then
        show_build_logs
        exit 0
    fi
    
    validate_prerequisites
    build_images
    
    log_success "======================================="
    log_success "Cloud Build completed successfully"
    log_success "======================================="
}

main "$@"

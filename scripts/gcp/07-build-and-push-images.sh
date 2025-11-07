#!/bin/bash
set -euo pipefail

##############################################################################
# GCP-REGISTRY-003: Build and Push Docker Images to Artifact Registry
#
# This script builds all RAG service Docker images and pushes them to
# Google Artifact Registry with proper versioning and tagging.
#
# Prerequisites:
# - GCP-INFRA-001 complete (project setup)
# - GCP-REGISTRY-003 Artifact Registry created
# - Docker installed and running
# - gcloud authenticated
#
# Usage:
#   ./scripts/gcp/07-build-and-push-images.sh [OPTIONS]
#
# Options:
#   --service <name>    Build only specific service (optional)
#   --skip-build        Skip build, only tag and push existing images
#   --dry-run           Show what would be done without executing
#
##############################################################################

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
PROJECT_ID=$(gcloud config get-value project)
REGION="us-central1"
REPOSITORY="rag-system"
REGISTRY="${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPOSITORY}"

# Get version from pom.xml
VERSION=$(grep -m 1 '<version>' "$PROJECT_ROOT/pom.xml" | sed 's/.*<version>\(.*\)<\/version>.*/\1/' | tr -d '[:space:]')
VERSION_TAG="${VERSION%-SNAPSHOT}"  # Remove -SNAPSHOT suffix for cleaner tags

# Get git commit SHA (short)
GIT_SHA=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")

# Build timestamp
BUILD_TIMESTAMP=$(date +%Y%m%d-%H%M%S)

# Services to build
SERVICES=(
    "rag-auth-service"
    "rag-document-service"
    "rag-embedding-service"
    "rag-core-service"
    "rag-admin-service"
)

# Parse arguments
SPECIFIC_SERVICE=""
SKIP_BUILD=false
DRY_RUN=false

while [[ $# -gt 0 ]]; do
  case $1 in
    --service)
      SPECIFIC_SERVICE="$2"
      shift 2
      ;;
    --skip-build)
      SKIP_BUILD=true
      shift
      ;;
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    *)
      echo -e "${RED}Unknown option: $1${NC}"
      exit 1
      ;;
  esac
done

##############################################################################
# Helper Functions
##############################################################################

log_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

log_success() {
    echo -e "${GREEN}✓${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

log_error() {
    echo -e "${RED}✗${NC} $1"
}

log_step() {
    echo ""
    echo -e "${CYAN}═══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}$1${NC}"
    echo -e "${CYAN}═══════════════════════════════════════════════════════════${NC}"
}

execute_command() {
    local cmd="$1"
    if [ "$DRY_RUN" = true ]; then
        echo -e "${YELLOW}[DRY RUN]${NC} $cmd"
    else
        eval "$cmd"
    fi
}

##############################################################################
# Main Script
##############################################################################

echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║     GCP-REGISTRY-003: Build and Push Docker Images            ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${BLUE}Project:${NC}     $PROJECT_ID"
echo -e "${BLUE}Registry:${NC}    $REGISTRY"
echo -e "${BLUE}Version:${NC}     $VERSION_TAG"
echo -e "${BLUE}Git SHA:${NC}     $GIT_SHA"
echo -e "${BLUE}Timestamp:${NC}   $BUILD_TIMESTAMP"
if [ -n "$SPECIFIC_SERVICE" ]; then
    echo -e "${BLUE}Service:${NC}     $SPECIFIC_SERVICE (specific)"
else
    echo -e "${BLUE}Services:${NC}    All (${#SERVICES[@]})"
fi
if [ "$DRY_RUN" = true ]; then
    echo -e "${YELLOW}Mode:${NC}        DRY RUN (no changes will be made)"
fi
echo ""

##############################################################################
# Step 1: Verify Prerequisites
##############################################################################

log_step "Step 1: Verifying Prerequisites"

# Check Docker is running
if ! docker info >/dev/null 2>&1; then
    log_error "Docker is not running"
    exit 1
fi
log_success "Docker is running"

# Check gcloud authentication
if ! gcloud auth list --filter=status:ACTIVE --format="value(account)" | head -n 1 >/dev/null; then
    log_error "Not authenticated with gcloud"
    echo "Run: gcloud auth login"
    exit 1
fi
log_success "Authenticated with gcloud"

# Check if registry exists
if ! gcloud artifacts repositories describe "$REPOSITORY" --location="$REGION" >/dev/null 2>&1; then
    log_error "Artifact Registry repository not found: $REPOSITORY"
    echo "Run: gcloud artifacts repositories create $REPOSITORY --repository-format=docker --location=$REGION"
    exit 1
fi
log_success "Artifact Registry repository exists"

# Verify we're in the right directory
if [ ! -f "$PROJECT_ROOT/pom.xml" ]; then
    log_error "Not in project root directory"
    exit 1
fi
log_success "Project root directory verified"

# Check if specific service exists
if [ -n "$SPECIFIC_SERVICE" ]; then
    if [ ! -d "$PROJECT_ROOT/$SPECIFIC_SERVICE" ]; then
        log_error "Service not found: $SPECIFIC_SERVICE"
        exit 1
    fi
    # Check if service is in the list
    service_found=false
    for svc in "${SERVICES[@]}"; do
        if [ "$svc" = "$SPECIFIC_SERVICE" ]; then
            service_found=true
            break
        fi
    done
    if [ "$service_found" = false ]; then
        log_error "Service not in build list: $SPECIFIC_SERVICE"
        exit 1
    fi
fi

echo ""

##############################################################################
# Step 2: Configure Docker Authentication
##############################################################################

log_step "Step 2: Configuring Docker Authentication"

execute_command "gcloud auth configure-docker ${REGION}-docker.pkg.dev --quiet"
log_success "Docker authentication configured for Artifact Registry"

echo ""

##############################################################################
# Step 3: Build Docker Images
##############################################################################

if [ "$SKIP_BUILD" = false ]; then
    log_step "Step 3: Building Docker Images"
    
    # Determine which services to build
    if [ -n "$SPECIFIC_SERVICE" ]; then
        services_to_build=("$SPECIFIC_SERVICE")
    else
        services_to_build=("${SERVICES[@]}")
    fi
    
    for service in "${services_to_build[@]}"; do
        log_info "Building $service..."
        
        service_dir="$PROJECT_ROOT/$service"
        if [ ! -f "$service_dir/Dockerfile" ]; then
            log_warning "No Dockerfile found for $service, skipping"
            continue
        fi
        
        # Build the image from project root (Dockerfiles expect this context)
        local_tag="$service:$VERSION_TAG"
        
        execute_command "docker build -f $service_dir/Dockerfile -t $local_tag $PROJECT_ROOT"
        
        log_success "Built $service"
    done
else
    log_step "Step 3: Skipping Build (--skip-build specified)"
fi

echo ""

##############################################################################
# Step 4: Tag Images
##############################################################################

log_step "Step 4: Tagging Images for Artifact Registry"

# Determine which services to tag
if [ -n "$SPECIFIC_SERVICE" ]; then
    services_to_tag=("$SPECIFIC_SERVICE")
else
    services_to_tag=("${SERVICES[@]}")
fi

for service in "${services_to_tag[@]}"; do
    local_tag="$service:$VERSION_TAG"
    
    # Create multiple tags for different use cases
    tag_version="${REGISTRY}/${service}:${VERSION_TAG}"
    tag_git="${REGISTRY}/${service}:${GIT_SHA}"
    tag_latest="${REGISTRY}/${service}:latest"
    tag_build="${REGISTRY}/${service}:${VERSION_TAG}-${GIT_SHA}"
    
    log_info "Tagging $service with multiple tags..."
    
    execute_command "docker tag $local_tag $tag_version"
    log_success "  ${VERSION_TAG}"
    
    execute_command "docker tag $local_tag $tag_git"
    log_success "  ${GIT_SHA}"
    
    execute_command "docker tag $local_tag $tag_latest"
    log_success "  latest"
    
    execute_command "docker tag $local_tag $tag_build"
    log_success "  ${VERSION_TAG}-${GIT_SHA}"
done

echo ""

##############################################################################
# Step 5: Push Images to Artifact Registry
##############################################################################

log_step "Step 5: Pushing Images to Artifact Registry"

for service in "${services_to_tag[@]}"; do
    log_info "Pushing $service..."
    
    # Push all tags for this service
    tag_version="${REGISTRY}/${service}:${VERSION_TAG}"
    tag_git="${REGISTRY}/${service}:${GIT_SHA}"
    tag_latest="${REGISTRY}/${service}:latest"
    tag_build="${REGISTRY}/${service}:${VERSION_TAG}-${GIT_SHA}"
    
    for tag in "$tag_version" "$tag_git" "$tag_latest" "$tag_build"; do
        tag_name=$(echo "$tag" | awk -F: '{print $2}')
        execute_command "docker push $tag"
        log_success "  Pushed tag: $tag_name"
    done
done

echo ""

##############################################################################
# Step 6: Verify Images
##############################################################################

log_step "Step 6: Verifying Images in Registry"

for service in "${services_to_tag[@]}"; do
    log_info "Verifying $service..."
    
    if [ "$DRY_RUN" = false ]; then
        images=$(gcloud artifacts docker images list "${REGISTRY}/${service}" --format="value(IMAGE)" --limit=5 2>/dev/null || echo "")
        
        if [ -n "$images" ]; then
            log_success "$service images in registry:"
            echo "$images" | while read -r image; do
                echo "    - $image"
            done
        else
            log_warning "No images found for $service (may be pushed but not yet listed)"
        fi
    else
        log_info "[DRY RUN] Would verify images in registry"
    fi
done

echo ""

##############################################################################
# Summary
##############################################################################

log_step "Build and Push Complete!"

echo ""
echo -e "${GREEN}Images successfully pushed to Artifact Registry:${NC}"
echo ""

for service in "${services_to_tag[@]}"; do
    echo -e "${CYAN}$service:${NC}"
    echo "  ${REGISTRY}/${service}:${VERSION_TAG}"
    echo "  ${REGISTRY}/${service}:${GIT_SHA}"
    echo "  ${REGISTRY}/${service}:latest"
    echo "  ${REGISTRY}/${service}:${VERSION_TAG}-${GIT_SHA}"
    echo ""
done

echo -e "${YELLOW}Next Steps:${NC}"
echo "  1. View images in console:"
echo "     https://console.cloud.google.com/artifacts/docker/${PROJECT_ID}/${REGION}/${REPOSITORY}"
echo ""
echo "  2. Pull an image:"
echo "     docker pull ${REGISTRY}/rag-core-service:${VERSION_TAG}"
echo ""
echo "  3. Use in Kubernetes (GCP-K8S-008):"
echo "     image: ${REGISTRY}/rag-core-service:${VERSION_TAG}"
echo ""
echo "  4. Check vulnerability scan results:"
echo "     gcloud artifacts docker images list ${REGISTRY}/rag-core-service \\"
echo "       --show-occurrences \\"
echo "       --format='table(package,vulnerability.effectiveSeverity)'"
echo ""

if [ "$DRY_RUN" = true ]; then
    echo -e "${YELLOW}⚠ This was a DRY RUN - no changes were made${NC}"
    echo ""
fi

log_success "GCP-REGISTRY-003 build and push script complete!"

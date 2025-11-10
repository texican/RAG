#!/usr/bin/env bash

################################################################################
# Volume Snapshot Management Script
# 
# Provides operations for:
# - Creating on-demand snapshots
# - Listing all snapshots
# - Restoring from snapshots
# - Deleting old snapshots
# - Exporting snapshots to Cloud Storage
#
# Usage:
#   ./15-manage-snapshots.sh <command> [options]
#
# Prerequisites:
#   - kubectl configured with cluster access
#   - Appropriate RBAC permissions
#   - VolumeSnapshotClass configured
################################################################################

set -euo pipefail

# Color codes
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m'

# Default values
NAMESPACE="rag-system"
PVC_NAME="document-storage-pvc"
SNAPSHOT_CLASS="rag-snapshot-class"

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

################################################################################
# Snapshot Operations
################################################################################

create_snapshot() {
    local snapshot_name=${1:-""}
    local description=${2:-"Manual snapshot"}
    
    if [[ -z "$snapshot_name" ]]; then
        snapshot_name="document-storage-manual-$(date +%Y%m%d-%H%M%S)"
    fi
    
    log_info "Creating snapshot: $snapshot_name"
    
    # Check if PVC exists
    if ! kubectl get pvc "$PVC_NAME" -n "$NAMESPACE" &> /dev/null; then
        log_error "PVC $PVC_NAME not found in namespace $NAMESPACE"
        exit 1
    fi
    
    # Create VolumeSnapshot
    cat <<EOF | kubectl apply -f -
apiVersion: snapshot.storage.k8s.io/v1
kind: VolumeSnapshot
metadata:
  name: $snapshot_name
  namespace: $NAMESPACE
  labels:
    app: rag-document
    created-by: manual
  annotations:
    description: "$description"
spec:
  volumeSnapshotClassName: $SNAPSHOT_CLASS
  source:
    persistentVolumeClaimName: $PVC_NAME
EOF
    
    log_success "Snapshot $snapshot_name created"
    
    # Wait for snapshot to be ready
    log_info "Waiting for snapshot to be ready (this may take a few minutes)..."
    if kubectl wait --for=jsonpath='{.status.readyToUse}'=true \
        volumesnapshot/"$snapshot_name" \
        -n "$NAMESPACE" \
        --timeout=600s; then
        
        log_success "Snapshot $snapshot_name is ready"
        
        # Get snapshot details
        kubectl get volumesnapshot "$snapshot_name" -n "$NAMESPACE" -o wide
    else
        log_error "Snapshot failed to become ready within timeout"
        exit 1
    fi
}

list_snapshots() {
    log_info "Listing all VolumeSnapshots in namespace $NAMESPACE"
    echo ""
    
    if ! kubectl get volumesnapshots -n "$NAMESPACE" &> /dev/null; then
        log_warning "No snapshots found in namespace $NAMESPACE"
        return 0
    fi
    
    kubectl get volumesnapshots -n "$NAMESPACE" \
        -o custom-columns='NAME:.metadata.name,READY:.status.readyToUse,SOURCE:.spec.source.persistentVolumeClaimName,AGE:.metadata.creationTimestamp' \
        --sort-by='.metadata.creationTimestamp'
    
    echo ""
    log_info "Total snapshots: $(kubectl get volumesnapshots -n "$NAMESPACE" --no-headers 2>/dev/null | wc -l | tr -d ' ')"
}

get_snapshot_details() {
    local snapshot_name=$1
    
    log_info "Fetching details for snapshot: $snapshot_name"
    echo ""
    
    if ! kubectl get volumesnapshot "$snapshot_name" -n "$NAMESPACE" &> /dev/null; then
        log_error "Snapshot $snapshot_name not found in namespace $NAMESPACE"
        exit 1
    fi
    
    kubectl get volumesnapshot "$snapshot_name" -n "$NAMESPACE" -o yaml
}

restore_from_snapshot() {
    local snapshot_name=$1
    local new_pvc_name=${2:-"document-storage-restored-$(date +%Y%m%d-%H%M%S)"}
    
    log_info "Restoring from snapshot: $snapshot_name"
    log_info "New PVC name: $new_pvc_name"
    
    # Verify snapshot exists
    if ! kubectl get volumesnapshot "$snapshot_name" -n "$NAMESPACE" &> /dev/null; then
        log_error "Snapshot $snapshot_name not found"
        exit 1
    fi
    
    # Get storage size from original PVC
    local storage_size
    storage_size=$(kubectl get pvc "$PVC_NAME" -n "$NAMESPACE" -o jsonpath='{.spec.resources.requests.storage}' 2>/dev/null || echo "100Gi")
    
    log_info "Creating new PVC from snapshot with size: $storage_size"
    
    # Create PVC from snapshot
    cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: $new_pvc_name
  namespace: $NAMESPACE
  labels:
    app: rag-document
    restored-from: $snapshot_name
spec:
  accessModes:
  - ReadWriteOnce
  resources:
    requests:
      storage: $storage_size
  storageClassName: standard-rwo
  dataSource:
    name: $snapshot_name
    kind: VolumeSnapshot
    apiGroup: snapshot.storage.k8s.io
EOF
    
    log_success "PVC $new_pvc_name created from snapshot"
    
    # Wait for PVC to be bound
    log_info "Waiting for PVC to be bound..."
    if kubectl wait --for=jsonpath='{.status.phase}'=Bound \
        pvc/"$new_pvc_name" \
        -n "$NAMESPACE" \
        --timeout=300s; then
        
        log_success "PVC $new_pvc_name is bound and ready"
        log_warning "To use this PVC, update the document service deployment:"
        echo "    kubectl set volume deployment/rag-document \\"
        echo "      -n $NAMESPACE \\"
        echo "      --add --name=document-storage \\"
        echo "      --type=persistentVolumeClaim \\"
        echo "      --claim-name=$new_pvc_name \\"
        echo "      --mount-path=/app/storage"
    else
        log_error "PVC failed to bind within timeout"
        exit 1
    fi
}

delete_snapshot() {
    local snapshot_name=$1
    
    log_warning "Deleting snapshot: $snapshot_name"
    
    # Confirm deletion
    read -rp "Are you sure you want to delete snapshot $snapshot_name? (yes/no): " confirm
    if [[ "$confirm" != "yes" ]]; then
        log_info "Deletion cancelled"
        exit 0
    fi
    
    if kubectl delete volumesnapshot "$snapshot_name" -n "$NAMESPACE"; then
        log_success "Snapshot $snapshot_name deleted"
    else
        log_error "Failed to delete snapshot $snapshot_name"
        exit 1
    fi
}

delete_old_snapshots() {
    local days=${1:-7}
    
    log_info "Deleting snapshots older than $days days"
    
    # Calculate cutoff date
    local cutoff_date
    cutoff_date=$(date -u -d "$days days ago" +%Y%m%d 2>/dev/null || date -u -v-${days}d +%Y%m%d)
    
    log_info "Cutoff date: $cutoff_date"
    
    # Find and delete old snapshots
    local deleted_count=0
    
    while IFS= read -r snapshot_name; do
        # Extract date from snapshot name (format: document-storage-YYYYMMDD-HHMMSS)
        local snapshot_date
        snapshot_date=$(echo "$snapshot_name" | grep -oE '[0-9]{8}' | head -1)
        
        if [[ -n "$snapshot_date" && "$snapshot_date" -lt "$cutoff_date" ]]; then
            log_info "Deleting old snapshot: $snapshot_name (date: $snapshot_date)"
            if kubectl delete volumesnapshot "$snapshot_name" -n "$NAMESPACE" --ignore-not-found=true; then
                ((deleted_count++))
            fi
        fi
    done < <(kubectl get volumesnapshots -n "$NAMESPACE" -o jsonpath='{.items[*].metadata.name}' | tr ' ' '\n')
    
    log_success "Deleted $deleted_count old snapshots"
}

export_snapshot_to_gcs() {
    local snapshot_name=$1
    local bucket_name=$2
    
    log_info "Exporting snapshot $snapshot_name to gs://$bucket_name"
    log_warning "This operation requires the Compute Engine API to be enabled"
    
    # Get snapshot details
    local snapshot_content_name
    snapshot_content_name=$(kubectl get volumesnapshot "$snapshot_name" -n "$NAMESPACE" -o jsonpath='{.status.boundVolumeSnapshotContentName}')
    
    if [[ -z "$snapshot_content_name" ]]; then
        log_error "Could not find VolumeSnapshotContent for snapshot $snapshot_name"
        exit 1
    fi
    
    log_info "VolumeSnapshotContent: $snapshot_content_name"
    
    # Get the snapshot handle (GCP snapshot name)
    local gcp_snapshot_handle
    gcp_snapshot_handle=$(kubectl get volumesnapshotcontent "$snapshot_content_name" -o jsonpath='{.status.snapshotHandle}')
    
    if [[ -z "$gcp_snapshot_handle" ]]; then
        log_error "Could not find GCP snapshot handle"
        exit 1
    fi
    
    log_info "GCP Snapshot: $gcp_snapshot_handle"
    
    # Extract snapshot name from handle (format: projects/PROJECT/zones/ZONE/snapshots/NAME)
    local gcp_snapshot_name
    gcp_snapshot_name=$(basename "$gcp_snapshot_handle")
    
    log_info "Exporting GCP snapshot $gcp_snapshot_name to Cloud Storage..."
    
    # Export using gcloud
    if gcloud compute snapshots export "$gcp_snapshot_name" \
        --destination-uri="gs://${bucket_name}/${snapshot_name}.vmdk" \
        --export-format=vmdk; then
        
        log_success "Snapshot exported to gs://${bucket_name}/${snapshot_name}.vmdk"
    else
        log_error "Failed to export snapshot"
        exit 1
    fi
}

################################################################################
# Usage
################################################################################

usage() {
    cat <<EOF
Usage: $0 <command> [options]

Manage VolumeSnapshots for RAG persistent storage.

Commands:
    create [NAME] [DESCRIPTION]  Create a new snapshot
    list                         List all snapshots
    details NAME                 Show details for a snapshot
    restore NAME [NEW_PVC]       Restore from a snapshot to new PVC
    delete NAME                  Delete a specific snapshot
    cleanup DAYS                 Delete snapshots older than DAYS (default: 7)
    export NAME BUCKET           Export snapshot to Cloud Storage bucket

Options:
    -n, --namespace NS          Kubernetes namespace (default: rag-system)
    -p, --pvc PVC               PVC name to snapshot (default: document-storage-pvc)
    -h, --help                  Show this help message

Examples:
    # Create manual snapshot
    $0 create my-backup "Before upgrade"

    # List all snapshots
    $0 list

    # Restore from snapshot
    $0 restore document-storage-20240109-120000

    # Delete snapshots older than 30 days
    $0 cleanup 30

    # Export snapshot to Cloud Storage
    $0 export document-storage-20240109-120000 my-project-backups

EOF
}

################################################################################
# Main
################################################################################

main() {
    if [[ $# -eq 0 ]]; then
        usage
        exit 1
    fi
    
    local command=$1
    shift
    
    # Parse global options
    while [[ $# -gt 0 ]]; do
        case $1 in
            -n|--namespace)
                NAMESPACE="$2"
                shift 2
                ;;
            -p|--pvc)
                PVC_NAME="$2"
                shift 2
                ;;
            -h|--help)
                usage
                exit 0
                ;;
            *)
                break
                ;;
        esac
    done
    
    # Execute command
    case $command in
        create)
            create_snapshot "$@"
            ;;
        list)
            list_snapshots
            ;;
        details)
            if [[ $# -lt 1 ]]; then
                log_error "Missing snapshot name"
                usage
                exit 1
            fi
            get_snapshot_details "$1"
            ;;
        restore)
            if [[ $# -lt 1 ]]; then
                log_error "Missing snapshot name"
                usage
                exit 1
            fi
            restore_from_snapshot "$@"
            ;;
        delete)
            if [[ $# -lt 1 ]]; then
                log_error "Missing snapshot name"
                usage
                exit 1
            fi
            delete_snapshot "$1"
            ;;
        cleanup)
            delete_old_snapshots "${1:-7}"
            ;;
        export)
            if [[ $# -lt 2 ]]; then
                log_error "Missing snapshot name or bucket name"
                usage
                exit 1
            fi
            export_snapshot_to_gcs "$1" "$2"
            ;;
        *)
            log_error "Unknown command: $command"
            usage
            exit 1
            ;;
    esac
}

main "$@"

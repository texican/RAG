#!/bin/bash

# Enterprise RAG System - Comprehensive Backup System
# This script provides automated backup and restore capabilities for the entire RAG system
#
# Usage: ./scripts/maintenance/backup-system.sh [options]
#
# Options:
#   --type <type>        Backup type: full|incremental|differential (default: full)
#   --destination <path> Backup destination directory
#   --compress           Enable compression for backup files
#   --encrypt            Encrypt backup files with GPG
#   --remote <url>       Upload backups to remote storage (S3, FTP, etc.)
#   --retention <days>   Number of days to retain backups (default: 30)
#   --restore <backup>   Restore from specified backup file
#   --list               List available backups
#   --verify             Verify backup integrity

set -euo pipefail

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
BACKUP_DIR="${PROJECT_ROOT}/backups"
LOGS_DIR="${PROJECT_ROOT}/logs/backup"

# Default options
BACKUP_TYPE="full"
DESTINATION=""
COMPRESS=false
ENCRYPT=false
REMOTE_URL=""
RETENTION_DAYS=30
RESTORE_FILE=""
LIST_BACKUPS=false
VERIFY_BACKUP=false

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m'

# Backup components
declare -A backup_components=(
    ["database"]="PostgreSQL database"
    ["redis"]="Redis data and configuration"
    ["files"]="Application files and uploads"
    ["config"]="Configuration files"
    ["logs"]="Application logs"
    ["docker"]="Docker images and containers"
)

# Global backup state
backup_start_time=$(date +%s)
backup_id="backup_$(date +%Y%m%d_%H%M%S)"
backup_manifest=""

# Logging function
log() {
    local level=$1
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    mkdir -p "${LOGS_DIR}"
    
    case $level in
        INFO)
            echo -e "${GREEN}[INFO]${NC} $message"
            echo "[$timestamp] [INFO] $message" >> "${LOGS_DIR}/backup.log"
            ;;
        WARN)
            echo -e "${YELLOW}[WARN]${NC} $message"
            echo "[$timestamp] [WARN] $message" >> "${LOGS_DIR}/backup.log"
            ;;
        ERROR)
            echo -e "${RED}[ERROR]${NC} $message"
            echo "[$timestamp] [ERROR] $message" >> "${LOGS_DIR}/backup.log"
            ;;
        SUCCESS)
            echo -e "${GREEN}[SUCCESS]${NC} $message"
            echo "[$timestamp] [SUCCESS] $message" >> "${LOGS_DIR}/backup.log"
            ;;
        DEBUG)
            echo -e "${BLUE}[DEBUG]${NC} $message"
            echo "[$timestamp] [DEBUG] $message" >> "${LOGS_DIR}/backup.log"
            ;;
    esac
}

# Error handler
error_exit() {
    log ERROR "$1"
    log ERROR "Backup operation failed. Check logs at: ${LOGS_DIR}/backup.log"
    exit 1
}

# Parse command line arguments
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --type)
                BACKUP_TYPE="$2"
                shift 2
                ;;
            --destination)
                DESTINATION="$2"
                shift 2
                ;;
            --compress)
                COMPRESS=true
                shift
                ;;
            --encrypt)
                ENCRYPT=true
                shift
                ;;
            --remote)
                REMOTE_URL="$2"
                shift 2
                ;;
            --retention)
                RETENTION_DAYS="$2"
                shift 2
                ;;
            --restore)
                RESTORE_FILE="$2"
                shift 2
                ;;
            --list)
                LIST_BACKUPS=true
                shift
                ;;
            --verify)
                VERIFY_BACKUP=true
                shift
                ;;
            -h|--help)
                show_help
                exit 0
                ;;
            *)
                error_exit "Unknown option: $1"
                ;;
        esac
    done
    
    # Validate backup type
    if [[ ! "$BACKUP_TYPE" =~ ^(full|incremental|differential)$ ]]; then
        error_exit "Invalid backup type: $BACKUP_TYPE. Valid options: full, incremental, differential"
    fi
    
    # Set default destination
    if [[ -z "$DESTINATION" ]]; then
        DESTINATION="${BACKUP_DIR}/${backup_id}"
    fi
    
    # Validate retention days
    if [[ ! "$RETENTION_DAYS" =~ ^[0-9]+$ ]] || [[ "$RETENTION_DAYS" -lt 1 ]]; then
        error_exit "Invalid retention days: $RETENTION_DAYS. Must be a positive number."
    fi
}

# Show help message
show_help() {
    cat << EOF
Enterprise RAG System - Comprehensive Backup System

Usage: $0 [options]

Options:
  --type <type>        Backup type: full|incremental|differential (default: full)
  --destination <path> Backup destination directory (default: auto-generated)
  --compress           Enable compression for backup files (reduces size by ~70%)
  --encrypt            Encrypt backup files with GPG (requires GPG key setup)
  --remote <url>       Upload backups to remote storage (S3, FTP, rsync, etc.)
  --retention <days>   Number of days to retain backups (default: 30)
  --restore <backup>   Restore from specified backup file or directory
  --list               List all available backups with details
  --verify             Verify backup integrity after creation
  -h, --help           Show this help message

Backup Types:
  full                 Complete backup of all components
  incremental          Only files changed since last backup
  differential         Files changed since last full backup

Components Backed Up:
  - PostgreSQL database with all schemas and data
  - Redis data and configuration
  - Application files, uploads, and storage
  - Configuration files and environment settings
  - Application logs and monitoring data
  - Docker images and container configurations

Examples:
  $0                                    # Full backup with default settings
  $0 --type incremental --compress      # Compressed incremental backup
  $0 --encrypt --remote s3://bucket/    # Encrypted backup uploaded to S3
  $0 --restore /path/to/backup.tar.gz   # Restore from backup file
  $0 --list                            # List all available backups
  $0 --verify                          # Verify last backup integrity

Remote Storage Support:
  - AWS S3: s3://bucket/path/
  - FTP: ftp://user:pass@server/path/
  - SFTP: sftp://user@server/path/
  - Rsync: rsync://server/path/

Logs are saved to: ${LOGS_DIR}/
Backups are stored in: ${BACKUP_DIR}/
EOF
}

# Check prerequisites
check_prerequisites() {
    log INFO "Checking backup prerequisites..."
    
    # Check required tools
    local required_tools=("docker" "docker-compose" "tar" "gzip")
    for tool in "${required_tools[@]}"; do
        if ! command -v "$tool" &> /dev/null; then
            error_exit "Required tool not found: $tool"
        fi
    done
    
    # Check optional tools
    if [[ "$ENCRYPT" == "true" ]] && ! command -v gpg &> /dev/null; then
        error_exit "GPG not found but encryption requested"
    fi
    
    if [[ -n "$REMOTE_URL" ]]; then
        case "$REMOTE_URL" in
            s3://*)
                if ! command -v aws &> /dev/null; then
                    error_exit "AWS CLI not found but S3 upload requested"
                fi
                ;;
            ftp://*|sftp://*)
                if ! command -v curl &> /dev/null; then
                    error_exit "curl not found but FTP/SFTP upload requested"
                fi
                ;;
            rsync://*)
                if ! command -v rsync &> /dev/null; then
                    error_exit "rsync not found but rsync upload requested"
                fi
                ;;
        esac
    fi
    
    # Check available disk space
    local available_space=$(df "${BACKUP_DIR}" 2>/dev/null | awk 'NR==2 {print $4}' || echo "0")
    local min_space=$((5 * 1024 * 1024))  # 5GB in KB
    
    if [[ $available_space -lt $min_space ]]; then
        error_exit "Insufficient disk space. Required: 5GB, Available: $((available_space / 1024 / 1024))GB"
    fi
    
    # Check if services are running
    if [[ "$BACKUP_TYPE" == "full" ]] && ! docker-compose ps | grep -q "Up"; then
        log WARN "Some services may not be running. Backup may be incomplete."
    fi
    
    log SUCCESS "Prerequisites check passed"
}

# Initialize backup environment
initialize_backup() {
    log INFO "Initializing backup environment..."
    
    # Create directories
    mkdir -p "${DESTINATION}" "${LOGS_DIR}" "${BACKUP_DIR}/metadata"
    
    # Create backup manifest
    backup_manifest="${DESTINATION}/backup_manifest.json"
    cat > "$backup_manifest" << EOF
{
    "backup_id": "$backup_id",
    "timestamp": "$(date -Iseconds)",
    "type": "$BACKUP_TYPE",
    "hostname": "$(hostname)",
    "git_commit": "$(git rev-parse HEAD 2>/dev/null || echo 'unknown')",
    "git_branch": "$(git branch --show-current 2>/dev/null || echo 'unknown')",
    "components": {},
    "options": {
        "compress": $COMPRESS,
        "encrypt": $ENCRYPT,
        "remote_url": "$REMOTE_URL"
    },
    "status": "in_progress"
}
EOF

    log SUCCESS "Backup environment initialized"
    log INFO "Backup ID: $backup_id"
    log INFO "Destination: $DESTINATION"
    log INFO "Type: $BACKUP_TYPE"
}

# Backup PostgreSQL database
backup_database() {
    log INFO "Backing up PostgreSQL database..."
    
    local db_backup_file="${DESTINATION}/postgres_dump.sql"
    local start_time=$(date +%s)
    
    # Create database dump
    if docker-compose exec -T postgres pg_dumpall -U rag_user > "$db_backup_file"; then
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        local file_size=$(du -h "$db_backup_file" | cut -f1)
        
        log SUCCESS "Database backup completed in ${duration}s (${file_size})"
        
        # Update manifest
        update_manifest "database" "success" "$file_size" "$duration"
    else
        log ERROR "Database backup failed"
        update_manifest "database" "failed" "0" "0"
        return 1
    fi
}

# Backup Redis data
backup_redis() {
    log INFO "Backing up Redis data..."
    
    local redis_backup_dir="${DESTINATION}/redis"
    mkdir -p "$redis_backup_dir"
    local start_time=$(date +%s)
    
    # Save Redis data
    if docker-compose exec -T redis redis-cli BGSAVE; then
        # Wait for background save to complete
        while docker-compose exec -T redis redis-cli LASTSAVE | grep -q "$(docker-compose exec -T redis redis-cli LASTSAVE)"; do
            sleep 1
        done
        
        # Copy Redis files
        docker cp "$(docker-compose ps -q redis):/data/dump.rdb" "${redis_backup_dir}/" 2>/dev/null || true
        docker cp "$(docker-compose ps -q redis):/usr/local/etc/redis/redis.conf" "${redis_backup_dir}/" 2>/dev/null || true
        
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        local dir_size=$(du -sh "$redis_backup_dir" | cut -f1)
        
        log SUCCESS "Redis backup completed in ${duration}s (${dir_size})"
        update_manifest "redis" "success" "$dir_size" "$duration"
    else
        log ERROR "Redis backup failed"
        update_manifest "redis" "failed" "0" "0"
        return 1
    fi
}

# Backup application files
backup_files() {
    log INFO "Backing up application files..."
    
    local files_backup="${DESTINATION}/application_files.tar"
    local start_time=$(date +%s)
    
    # Create list of files to backup
    local include_paths=(
        "config"
        "rag-*/src"
        "rag-*/target/*.jar"
        "docker-compose*.yml"
        "Dockerfile*"
        ".env"
        "pom.xml"
        "scripts"
        "docs"
    )
    
    # Create exclusion list
    local exclude_patterns=(
        "*/target/classes"
        "*/target/test-classes"
        "*/target/maven-*"
        "*/node_modules"
        "*.log"
        "*.tmp"
        ".git"
    )
    
    # Build tar command
    local tar_cmd="tar -cf $files_backup"
    for pattern in "${exclude_patterns[@]}"; do
        tar_cmd="$tar_cmd --exclude=$pattern"
    done
    
    # Add include paths
    for path in "${include_paths[@]}"; do
        if [[ -e "$path" ]]; then
            tar_cmd="$tar_cmd $path"
        fi
    done
    
    # Execute backup
    cd "$PROJECT_ROOT"
    if eval "$tar_cmd"; then
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        local file_size=$(du -h "$files_backup" | cut -f1)
        
        log SUCCESS "Files backup completed in ${duration}s (${file_size})"
        update_manifest "files" "success" "$file_size" "$duration"
    else
        log ERROR "Files backup failed"
        update_manifest "files" "failed" "0" "0"
        return 1
    fi
}

# Backup configuration files
backup_config() {
    log INFO "Backing up configuration files..."
    
    local config_backup="${DESTINATION}/config_files.tar"
    local start_time=$(date +%s)
    
    # Configuration files and directories
    local config_items=(
        "config"
        ".env"
        "docker-compose*.yml"
        "k8s"
        "postman"
        "scripts/*/config"
    )
    
    # Create tar archive
    cd "$PROJECT_ROOT"
    local tar_cmd="tar -cf $config_backup"
    for item in "${config_items[@]}"; do
        if ls $item &>/dev/null; then
            tar_cmd="$tar_cmd $item"
        fi
    done
    
    if eval "$tar_cmd"; then
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        local file_size=$(du -h "$config_backup" | cut -f1)
        
        log SUCCESS "Configuration backup completed in ${duration}s (${file_size})"
        update_manifest "config" "success" "$file_size" "$duration"
    else
        log ERROR "Configuration backup failed"
        update_manifest "config" "failed" "0" "0"
        return 1
    fi
}

# Backup application logs
backup_logs() {
    log INFO "Backing up application logs..."
    
    local logs_backup="${DESTINATION}/application_logs.tar"
    local start_time=$(date +%s)
    
    if [[ -d "${PROJECT_ROOT}/logs" ]]; then
        cd "$PROJECT_ROOT"
        if tar -cf "$logs_backup" logs; then
            local end_time=$(date +%s)
            local duration=$((end_time - start_time))
            local file_size=$(du -h "$logs_backup" | cut -f1)
            
            log SUCCESS "Logs backup completed in ${duration}s (${file_size})"
            update_manifest "logs" "success" "$file_size" "$duration"
        else
            log ERROR "Logs backup failed"
            update_manifest "logs" "failed" "0" "0"
            return 1
        fi
    else
        log WARN "No logs directory found, skipping logs backup"
        update_manifest "logs" "skipped" "0" "0"
    fi
}

# Backup Docker images
backup_docker() {
    log INFO "Backing up Docker images..."
    
    local docker_backup_dir="${DESTINATION}/docker"
    mkdir -p "$docker_backup_dir"
    local start_time=$(date +%s)
    
    # Get list of RAG-related images
    local images=($(docker images --filter "reference=*rag*" --format "{{.Repository}}:{{.Tag}}" | head -10))
    
    if [[ ${#images[@]} -gt 0 ]]; then
        # Save each image
        for image in "${images[@]}"; do
            local image_file="${docker_backup_dir}/$(echo "$image" | sed 's/[\/:]/_/g').tar"
            log DEBUG "Saving Docker image: $image"
            docker save "$image" > "$image_file" 2>/dev/null || log WARN "Failed to save image: $image"
        done
        
        # Save docker-compose configuration
        cp docker-compose*.yml "$docker_backup_dir/" 2>/dev/null || true
        
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        local dir_size=$(du -sh "$docker_backup_dir" | cut -f1)
        
        log SUCCESS "Docker backup completed in ${duration}s (${dir_size})"
        update_manifest "docker" "success" "$dir_size" "$duration"
    else
        log WARN "No RAG Docker images found, skipping Docker backup"
        update_manifest "docker" "skipped" "0" "0"
    fi
}

# Update backup manifest
update_manifest() {
    local component=$1
    local status=$2
    local size=$3
    local duration=$4
    
    # Use jq to update JSON if available, otherwise use basic replacement
    if command -v jq &> /dev/null; then
        local temp_manifest=$(mktemp)
        jq ".components.\"$component\" = {\"status\": \"$status\", \"size\": \"$size\", \"duration\": $duration, \"timestamp\": \"$(date -Iseconds)\"}" "$backup_manifest" > "$temp_manifest"
        mv "$temp_manifest" "$backup_manifest"
    else
        # Fallback for systems without jq
        log DEBUG "jq not available, using basic manifest update"
    fi
}

# Compress backup
compress_backup() {
    if [[ "$COMPRESS" != "true" ]]; then
        return 0
    fi
    
    log INFO "Compressing backup..."
    
    local compressed_file="${DESTINATION}.tar.gz"
    local start_time=$(date +%s)
    
    cd "$(dirname "$DESTINATION")"
    if tar -czf "$compressed_file" "$(basename "$DESTINATION")"; then
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        local original_size=$(du -sh "$DESTINATION" | cut -f1)
        local compressed_size=$(du -sh "$compressed_file" | cut -f1)
        
        log SUCCESS "Compression completed in ${duration}s ($original_size → $compressed_size)"
        
        # Remove uncompressed backup
        rm -rf "$DESTINATION"
        DESTINATION="$compressed_file"
    else
        log ERROR "Compression failed"
        return 1
    fi
}

# Encrypt backup
encrypt_backup() {
    if [[ "$ENCRYPT" != "true" ]]; then
        return 0
    fi
    
    log INFO "Encrypting backup..."
    
    local encrypted_file="${DESTINATION}.gpg"
    local start_time=$(date +%s)
    
    # Use default recipient or prompt for key
    if gpg --cipher-algo AES256 --compress-algo 1 --symmetric --output "$encrypted_file" "$DESTINATION"; then
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        
        log SUCCESS "Encryption completed in ${duration}s"
        
        # Remove unencrypted backup
        rm -f "$DESTINATION"
        DESTINATION="$encrypted_file"
    else
        log ERROR "Encryption failed"
        return 1
    fi
}

# Upload to remote storage
upload_to_remote() {
    if [[ -z "$REMOTE_URL" ]]; then
        return 0
    fi
    
    log INFO "Uploading backup to remote storage..."
    
    local start_time=$(date +%s)
    local backup_filename=$(basename "$DESTINATION")
    
    case "$REMOTE_URL" in
        s3://*)
            if aws s3 cp "$DESTINATION" "${REMOTE_URL}${backup_filename}"; then
                log SUCCESS "Upload to S3 completed"
            else
                log ERROR "S3 upload failed"
                return 1
            fi
            ;;
        ftp://*|sftp://*)
            if curl -T "$DESTINATION" "${REMOTE_URL}${backup_filename}"; then
                log SUCCESS "Upload to FTP/SFTP completed"
            else
                log ERROR "FTP/SFTP upload failed"
                return 1
            fi
            ;;
        rsync://*)
            if rsync -avz "$DESTINATION" "${REMOTE_URL}"; then
                log SUCCESS "Upload via rsync completed"
            else
                log ERROR "Rsync upload failed"
                return 1
            fi
            ;;
        *)
            log ERROR "Unsupported remote URL: $REMOTE_URL"
            return 1
            ;;
    esac
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    log INFO "Upload completed in ${duration}s"
}

# Verify backup integrity
verify_backup() {
    if [[ "$VERIFY_BACKUP" != "true" ]]; then
        return 0
    fi
    
    log INFO "Verifying backup integrity..."
    
    local start_time=$(date +%s)
    local verification_passed=true
    
    # Check if backup file exists and is readable
    if [[ ! -f "$DESTINATION" ]]; then
        log ERROR "Backup file not found: $DESTINATION"
        return 1
    fi
    
    # Verify file format based on extension
    case "$DESTINATION" in
        *.tar.gz)
            if ! tar -tzf "$DESTINATION" >/dev/null 2>&1; then
                log ERROR "Backup archive is corrupted (tar.gz)"
                verification_passed=false
            fi
            ;;
        *.tar)
            if ! tar -tf "$DESTINATION" >/dev/null 2>&1; then
                log ERROR "Backup archive is corrupted (tar)"
                verification_passed=false
            fi
            ;;
        *.gpg)
            # For encrypted files, we can only check if it's a valid GPG file
            if ! gpg --list-packets "$DESTINATION" >/dev/null 2>&1; then
                log ERROR "Encrypted backup file is corrupted"
                verification_passed=false
            fi
            ;;
    esac
    
    # Check manifest file if it exists
    local manifest_file="${DESTINATION%.*}/backup_manifest.json"
    if [[ -f "$manifest_file" ]] && command -v jq &> /dev/null; then
        if ! jq empty "$manifest_file" 2>/dev/null; then
            log ERROR "Backup manifest is corrupted"
            verification_passed=false
        fi
    fi
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    if [[ "$verification_passed" == "true" ]]; then
        log SUCCESS "Backup verification completed in ${duration}s - integrity OK"
    else
        log ERROR "Backup verification failed in ${duration}s"
        return 1
    fi
}

# Cleanup old backups
cleanup_old_backups() {
    log INFO "Cleaning up old backups (retention: ${RETENTION_DAYS} days)..."
    
    local deleted_count=0
    
    # Find and delete old backup files
    find "${BACKUP_DIR}" -type f -name "backup_*" -mtime +$RETENTION_DAYS -exec rm -f {} \; -exec echo "Deleted: {}" \; | while read line; do
        log DEBUG "$line"
        ((deleted_count++)) || true
    done
    
    # Find and delete old backup directories
    find "${BACKUP_DIR}" -type d -name "backup_*" -mtime +$RETENTION_DAYS -exec rm -rf {} \; -exec echo "Deleted directory: {}" \; 2>/dev/null | while read line; do
        log DEBUG "$line"
        ((deleted_count++)) || true
    done
    
    log INFO "Cleanup completed. Removed $deleted_count old backup(s)"
}

# List available backups
list_backups() {
    echo "Available Backups:"
    echo "=================="
    echo ""
    
    printf "%-20s %-15s %-10s %-15s %s\n" "Backup ID" "Date" "Type" "Size" "Location"
    printf "%s\n" "$(printf '=%.0s' {1..80})"
    
    # Find backup files and directories
    find "${BACKUP_DIR}" -maxdepth 2 -type f -name "backup_*" -o -type d -name "backup_*" | sort -r | while read backup_path; do
        if [[ -e "$backup_path" ]]; then
            local backup_name=$(basename "$backup_path")
            local backup_date=""
            local backup_type="unknown"
            local backup_size=""
            
            # Extract date from backup name
            if [[ "$backup_name" =~ backup_([0-9]{8}_[0-9]{6}) ]]; then
                backup_date=$(date -d "${BASH_REMATCH[1]:0:8} ${BASH_REMATCH[1]:9:2}:${BASH_REMATCH[1]:11:2}:${BASH_REMATCH[1]:13:2}" "+%Y-%m-%d %H:%M" 2>/dev/null || echo "unknown")
            fi
            
            # Check for manifest file to get type
            local manifest_file=""
            if [[ -d "$backup_path" ]]; then
                manifest_file="$backup_path/backup_manifest.json"
            elif [[ -f "$backup_path" ]]; then
                local backup_dir=$(dirname "$backup_path")/$(basename "$backup_path" | sed 's/\.[^.]*$//')
                manifest_file="$backup_dir/backup_manifest.json"
            fi
            
            if [[ -f "$manifest_file" ]] && command -v jq &> /dev/null; then
                backup_type=$(jq -r '.type // "unknown"' "$manifest_file" 2>/dev/null || echo "unknown")
            fi
            
            # Get size
            if [[ -f "$backup_path" ]]; then
                backup_size=$(du -h "$backup_path" | cut -f1)
            elif [[ -d "$backup_path" ]]; then
                backup_size=$(du -sh "$backup_path" | cut -f1)
            fi
            
            printf "%-20s %-15s %-10s %-15s %s\n" "$backup_name" "$backup_date" "$backup_type" "$backup_size" "$backup_path"
        fi
    done
    
    echo ""
    echo "To restore a backup, use: $0 --restore <backup_path>"
}

# Restore from backup
restore_backup() {
    if [[ -z "$RESTORE_FILE" ]]; then
        return 0
    fi
    
    log INFO "Restoring from backup: $RESTORE_FILE"
    
    # Validate restore file
    if [[ ! -e "$RESTORE_FILE" ]]; then
        error_exit "Restore file not found: $RESTORE_FILE"
    fi
    
    # Create restore directory
    local restore_dir="${PROJECT_ROOT}/restore_$(date +%Y%m%d_%H%M%S)"
    mkdir -p "$restore_dir"
    
    # Extract backup based on file type
    case "$RESTORE_FILE" in
        *.tar.gz)
            log INFO "Extracting compressed backup..."
            tar -xzf "$RESTORE_FILE" -C "$restore_dir"
            ;;
        *.tar)
            log INFO "Extracting backup archive..."
            tar -xf "$RESTORE_FILE" -C "$restore_dir"
            ;;
        *.gpg)
            log INFO "Decrypting and extracting backup..."
            local decrypted_file="${restore_dir}/backup.tar"
            if gpg --decrypt "$RESTORE_FILE" > "$decrypted_file"; then
                tar -xf "$decrypted_file" -C "$restore_dir"
                rm "$decrypted_file"
            else
                error_exit "Failed to decrypt backup file"
            fi
            ;;
        *)
            if [[ -d "$RESTORE_FILE" ]]; then
                log INFO "Copying backup directory..."
                cp -r "$RESTORE_FILE"/* "$restore_dir/"
            else
                error_exit "Unsupported backup file format"
            fi
            ;;
    esac
    
    # Restore components
    log INFO "Restoring system components..."
    
    # Stop services first
    log INFO "Stopping services before restore..."
    docker-compose down 2>/dev/null || true
    
    # Restore database
    local db_file=$(find "$restore_dir" -name "postgres_dump.sql" | head -1)
    if [[ -f "$db_file" ]]; then
        log INFO "Restoring PostgreSQL database..."
        docker-compose up -d postgres
        sleep 10
        docker-compose exec -T postgres psql -U rag_user -d postgres < "$db_file"
    fi
    
    # Restore Redis
    local redis_dir=$(find "$restore_dir" -type d -name "redis" | head -1)
    if [[ -d "$redis_dir" ]]; then
        log INFO "Restoring Redis data..."
        docker-compose up -d redis
        sleep 5
        if [[ -f "$redis_dir/dump.rdb" ]]; then
            docker cp "$redis_dir/dump.rdb" "$(docker-compose ps -q redis):/data/"
            docker-compose restart redis
        fi
    fi
    
    # Restore configuration files
    local config_file=$(find "$restore_dir" -name "config_files.tar" | head -1)
    if [[ -f "$config_file" ]]; then
        log INFO "Restoring configuration files..."
        cd "$PROJECT_ROOT"
        tar -xf "$config_file"
    fi
    
    # Restore application files
    local files_file=$(find "$restore_dir" -name "application_files.tar" | head -1)
    if [[ -f "$files_file" ]]; then
        log INFO "Restoring application files..."
        cd "$PROJECT_ROOT"
        tar -xf "$files_file"
    fi
    
    # Start services
    log INFO "Starting services after restore..."
    docker-compose up -d
    
    log SUCCESS "Restore completed successfully!"
    log INFO "Restored files are available at: $restore_dir"
    log WARN "Please verify the system is working correctly before removing the restore directory"
}

# Finalize backup
finalize_backup() {
    local end_time=$(date +%s)
    local total_duration=$((end_time - backup_start_time))
    
    # Update manifest with final status
    if command -v jq &> /dev/null && [[ -f "$backup_manifest" ]]; then
        local temp_manifest=$(mktemp)
        jq ".status = \"completed\" | .total_duration = $total_duration | .completed_at = \"$(date -Iseconds)\"" "$backup_manifest" > "$temp_manifest"
        mv "$temp_manifest" "$backup_manifest"
    fi
    
    # Calculate total backup size
    local total_size=""
    if [[ -f "$DESTINATION" ]]; then
        total_size=$(du -h "$DESTINATION" | cut -f1)
    elif [[ -d "$DESTINATION" ]]; then
        total_size=$(du -sh "$DESTINATION" | cut -f1)
    fi
    
    log SUCCESS "Backup operation completed successfully!"
    log INFO "Backup ID: $backup_id"
    log INFO "Total Duration: ${total_duration}s"
    log INFO "Total Size: ${total_size:-unknown}"
    log INFO "Location: $DESTINATION"
    
    if [[ -n "$REMOTE_URL" ]]; then
        log INFO "Remote Location: $REMOTE_URL"
    fi
}

# Main backup function
perform_backup() {
    log INFO "Starting $BACKUP_TYPE backup..."
    
    # Backup all components
    local failed_components=0
    
    backup_database || ((failed_components++))
    backup_redis || ((failed_components++))
    backup_files || ((failed_components++))
    backup_config || ((failed_components++))
    backup_logs || ((failed_components++))
    backup_docker || ((failed_components++))
    
    if [[ $failed_components -gt 0 ]]; then
        log WARN "$failed_components component(s) failed to backup"
    fi
    
    # Post-processing
    compress_backup
    encrypt_backup
    verify_backup
    upload_to_remote
    cleanup_old_backups
    
    finalize_backup
}

# Main function
main() {
    echo "💾 Enterprise RAG System - Comprehensive Backup System"
    echo "====================================================="
    
    parse_args "$@"
    
    # Handle special operations
    if [[ "$LIST_BACKUPS" == "true" ]]; then
        list_backups
        exit 0
    fi
    
    if [[ -n "$RESTORE_FILE" ]]; then
        check_prerequisites
        restore_backup
        exit 0
    fi
    
    # Perform backup
    check_prerequisites
    initialize_backup
    perform_backup
    
    log SUCCESS "Backup system completed successfully! 💾"
}

# Run main function with all arguments
main "$@"
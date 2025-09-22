#!/bin/bash

# Enterprise RAG System - Docker Deployment Script
# This script handles complete Docker-based deployment of the RAG system
#
# Usage: ./scripts/deploy/docker-deploy.sh [options]
#
# Options:
#   --environment <env>  Deployment environment: dev|staging|production
#   --build              Force rebuild of all images
#   --no-cache           Build without using cache
#   --pull-latest        Pull latest base images
#   --services <list>    Deploy specific services only (comma-separated)
#   --scale <service:n>  Scale specific service to n replicas
#   --health-check       Wait for health checks before completing
#   --rollback           Rollback to previous version

set -euo pipefail

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
DEPLOY_LOG_DIR="${PROJECT_ROOT}/logs/deployment"
BACKUP_DIR="${PROJECT_ROOT}/backups/docker"

# Default options
ENVIRONMENT="dev"
FORCE_BUILD=false
NO_CACHE=false
PULL_LATEST=false
SPECIFIC_SERVICES=""
SCALE_CONFIG=""
HEALTH_CHECK=true
ROLLBACK=false

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m'

# Environment configurations
declare -A env_configs=(
    ["dev"]="compose_file=docker-compose.yml profile=dev replicas=1"
    ["staging"]="compose_file=docker-compose.staging.yml profile=staging replicas=2"
    ["production"]="compose_file=docker-compose.prod.yml profile=production replicas=3"
)

# Service order for deployment
deployment_order=(
    "postgres"
    "redis"
    "kafka"
    "rag-auth-service"
    "rag-admin-service"
    "rag-embedding-service"
    "rag-document-service"
    "rag-core-service"
    "rag-gateway"
)

# Logging function
log() {
    local level=$1
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    mkdir -p "${DEPLOY_LOG_DIR}"
    
    case $level in
        INFO)
            echo -e "${GREEN}[INFO]${NC} $message"
            echo "[$timestamp] [INFO] $message" >> "${DEPLOY_LOG_DIR}/deployment.log"
            ;;
        WARN)
            echo -e "${YELLOW}[WARN]${NC} $message"
            echo "[$timestamp] [WARN] $message" >> "${DEPLOY_LOG_DIR}/deployment.log"
            ;;
        ERROR)
            echo -e "${RED}[ERROR]${NC} $message"
            echo "[$timestamp] [ERROR] $message" >> "${DEPLOY_LOG_DIR}/deployment.log"
            ;;
        SUCCESS)
            echo -e "${GREEN}[SUCCESS]${NC} $message"
            echo "[$timestamp] [SUCCESS] $message" >> "${DEPLOY_LOG_DIR}/deployment.log"
            ;;
        DEBUG)
            echo -e "${BLUE}[DEBUG]${NC} $message"
            echo "[$timestamp] [DEBUG] $message" >> "${DEPLOY_LOG_DIR}/deployment.log"
            ;;
    esac
}

# Error handler
error_exit() {
    log ERROR "$1"
    log ERROR "Deployment failed. Check logs at: ${DEPLOY_LOG_DIR}/deployment.log"
    exit 1
}

# Parse command line arguments
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --environment)
                ENVIRONMENT="$2"
                shift 2
                ;;
            --build)
                FORCE_BUILD=true
                shift
                ;;
            --no-cache)
                NO_CACHE=true
                shift
                ;;
            --pull-latest)
                PULL_LATEST=true
                shift
                ;;
            --services)
                SPECIFIC_SERVICES="$2"
                shift 2
                ;;
            --scale)
                SCALE_CONFIG="$2"
                shift 2
                ;;
            --health-check)
                HEALTH_CHECK=true
                shift
                ;;
            --rollback)
                ROLLBACK=true
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
    
    # Validate environment
    if [[ -z "${env_configs[$ENVIRONMENT]:-}" ]]; then
        error_exit "Invalid environment: $ENVIRONMENT. Valid options: ${!env_configs[*]}"
    fi
    
    # Apply environment configuration
    eval "${env_configs[$ENVIRONMENT]}"
}

# Show help message
show_help() {
    cat << EOF
Enterprise RAG System - Docker Deployment Script

Usage: $0 [options]

Options:
  --environment <env>  Deployment environment: dev|staging|production (default: dev)
  --build              Force rebuild of all images
  --no-cache           Build without using cache
  --pull-latest        Pull latest base images before building
  --services <list>    Deploy specific services only (comma-separated)
  --scale <service:n>  Scale specific service to n replicas
  --health-check       Wait for health checks before completing (default: true)
  --rollback           Rollback to previous version
  -h, --help           Show this help message

Environments:
  dev                  - Development environment (single replicas, debug enabled)
  staging              - Staging environment (2 replicas, monitoring enabled)
  production           - Production environment (3 replicas, full monitoring)

Examples:
  $0                                           # Deploy dev environment
  $0 --environment production --build          # Production deployment with rebuild
  $0 --services rag-auth-service,rag-gateway  # Deploy specific services
  $0 --scale rag-embedding-service:5           # Scale embedding service to 5 replicas
  $0 --rollback                               # Rollback to previous version

Deployment artifacts are saved to: ${DEPLOY_LOG_DIR}/
Backups are saved to: ${BACKUP_DIR}/
EOF
}

# Check prerequisites
check_prerequisites() {
    log INFO "Checking deployment prerequisites..."
    
    # Check Docker
    if ! command -v docker &> /dev/null; then
        error_exit "Docker is not installed"
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        error_exit "Docker Compose is not installed"
    fi
    
    # Check if Docker daemon is running
    if ! docker info &> /dev/null; then
        error_exit "Docker daemon is not running"
    fi
    
    # Check if compose file exists
    local compose_file="${PROJECT_ROOT}/${compose_file}"
    if [[ ! -f "$compose_file" ]]; then
        log WARN "Compose file not found: $compose_file"
        log INFO "Using default docker-compose.yml"
        compose_file="${PROJECT_ROOT}/docker-compose.yml"
        
        if [[ ! -f "$compose_file" ]]; then
            error_exit "No compose file found"
        fi
    fi
    
    # Check available disk space (minimum 10GB)
    local available_space=$(df "${PROJECT_ROOT}" | awk 'NR==2 {print $4}')
    local min_space=$((10 * 1024 * 1024))  # 10GB in KB
    
    if [[ $available_space -lt $min_space ]]; then
        error_exit "Insufficient disk space. Required: 10GB, Available: $((available_space / 1024 / 1024))GB"
    fi
    
    log SUCCESS "Prerequisites check passed"
}

# Create deployment backup
create_backup() {
    if [[ "$ROLLBACK" == "true" ]]; then
        log INFO "Skipping backup for rollback operation"
        return 0
    fi
    
    log INFO "Creating deployment backup..."
    
    local backup_timestamp=$(date +%Y%m%d_%H%M%S)
    local backup_path="${BACKUP_DIR}/${backup_timestamp}"
    mkdir -p "$backup_path"
    
    cd "${PROJECT_ROOT}"
    
    # Backup current running state
    if docker-compose ps -q | grep -q .; then
        log INFO "Backing up current container state..."
        docker-compose ps > "${backup_path}/container_state.txt"
        
        # Export current images
        log INFO "Exporting current images..."
        local images=$(docker-compose config --services | xargs -I {} echo "$(basename ${PROJECT_ROOT})_{}")
        for image in $images; do
            if docker images | grep -q "$image"; then
                docker save "$image" > "${backup_path}/${image//\//_}.tar" 2>/dev/null || true
            fi
        done
    fi
    
    # Backup configuration files
    log INFO "Backing up configuration..."
    cp -r config "${backup_path}/" 2>/dev/null || true
    cp docker-compose*.yml "${backup_path}/" 2>/dev/null || true
    cp .env "${backup_path}/" 2>/dev/null || true
    
    # Create backup metadata
    cat > "${backup_path}/backup_info.json" << EOF
{
    "timestamp": "$(date -Iseconds)",
    "environment": "$ENVIRONMENT",
    "git_commit": "$(git rev-parse HEAD 2>/dev/null || echo 'unknown')",
    "git_branch": "$(git branch --show-current 2>/dev/null || echo 'unknown')",
    "backup_type": "pre_deployment"
}
EOF

    echo "$backup_timestamp" > "${BACKUP_DIR}/latest_backup"
    
    log SUCCESS "Backup created: $backup_path"
}

# Rollback to previous version
perform_rollback() {
    log INFO "Performing rollback..."
    
    local latest_backup_file="${BACKUP_DIR}/latest_backup"
    if [[ ! -f "$latest_backup_file" ]]; then
        error_exit "No backup found for rollback"
    fi
    
    local backup_timestamp=$(cat "$latest_backup_file")
    local backup_path="${BACKUP_DIR}/${backup_timestamp}"
    
    if [[ ! -d "$backup_path" ]]; then
        error_exit "Backup directory not found: $backup_path"
    fi
    
    log INFO "Rolling back to backup: $backup_timestamp"
    
    cd "${PROJECT_ROOT}"
    
    # Stop current services
    log INFO "Stopping current services..."
    docker-compose down || true
    
    # Restore configuration files
    log INFO "Restoring configuration..."
    cp "${backup_path}"/docker-compose*.yml . 2>/dev/null || true
    cp "${backup_path}/.env" . 2>/dev/null || true
    
    # Load backup images
    log INFO "Loading backup images..."
    for image_tar in "${backup_path}"/*.tar; do
        if [[ -f "$image_tar" ]]; then
            log DEBUG "Loading image: $(basename "$image_tar")"
            docker load < "$image_tar"
        fi
    done
    
    # Start services with backup configuration
    log INFO "Starting services from backup..."
    docker-compose up -d
    
    # Wait for health checks
    if [[ "$HEALTH_CHECK" == "true" ]]; then
        wait_for_health_checks
    fi
    
    log SUCCESS "Rollback completed successfully"
}

# Build Docker images
build_images() {
    log INFO "Building Docker images..."
    
    cd "${PROJECT_ROOT}"
    
    # Parse services to build
    local services_to_build=()
    if [[ -n "$SPECIFIC_SERVICES" ]]; then
        IFS=',' read -ra services_to_build <<< "$SPECIFIC_SERVICES"
    else
        services_to_build=($(docker-compose config --services | grep rag-))
    fi
    
    local build_args=""
    
    # Add build flags
    if [[ "$NO_CACHE" == "true" ]]; then
        build_args="$build_args --no-cache"
    fi
    
    if [[ "$PULL_LATEST" == "true" ]]; then
        build_args="$build_args --pull"
    fi
    
    # Force build if requested
    if [[ "$FORCE_BUILD" == "true" ]]; then
        build_args="$build_args --force-rm"
    fi
    
    # Build each service
    for service in "${services_to_build[@]}"; do
        log INFO "Building $service..."
        
        if docker-compose build $build_args "$service"; then
            log SUCCESS "Successfully built $service"
        else
            error_exit "Failed to build $service"
        fi
    done
    
    log SUCCESS "All images built successfully"
}

# Deploy services
deploy_services() {
    log INFO "Deploying services to $ENVIRONMENT environment..."
    
    cd "${PROJECT_ROOT}"
    
    # Set environment profile
    export SPRING_PROFILES_ACTIVE="$profile"
    export DEPLOYMENT_ENV="$ENVIRONMENT"
    
    # Parse services to deploy
    local services_to_deploy=()
    if [[ -n "$SPECIFIC_SERVICES" ]]; then
        IFS=',' read -ra services_to_deploy <<< "$SPECIFIC_SERVICES"
    else
        services_to_deploy=("${deployment_order[@]}")
    fi
    
    # Deploy infrastructure services first
    log INFO "Deploying infrastructure services..."
    for service in postgres redis kafka; do
        if [[ " ${services_to_deploy[*]} " =~ " ${service} " ]]; then
            log INFO "Starting $service..."
            docker-compose up -d "$service"
            
            # Wait for infrastructure service to be ready
            case $service in
                postgres)
                    wait_for_postgres
                    ;;
                redis)
                    wait_for_redis
                    ;;
                kafka)
                    wait_for_kafka
                    ;;
            esac
        fi
    done
    
    # Deploy application services
    log INFO "Deploying application services..."
    for service in "${deployment_order[@]}"; do
        if [[ "$service" =~ ^rag- ]] && [[ " ${services_to_deploy[*]} " =~ " ${service} " ]]; then
            log INFO "Deploying $service..."
            
            # Apply scaling if configured
            local scale_replicas="$replicas"
            if [[ -n "$SCALE_CONFIG" && "$SCALE_CONFIG" =~ ^${service}:([0-9]+)$ ]]; then
                scale_replicas="${BASH_REMATCH[1]}"
                log INFO "Scaling $service to $scale_replicas replicas"
            fi
            
            docker-compose up -d --scale "$service=$scale_replicas" "$service"
            
            # Brief pause between service deployments
            sleep 5
        fi
    done
    
    log SUCCESS "All services deployed successfully"
}

# Wait for infrastructure services
wait_for_postgres() {
    log INFO "Waiting for PostgreSQL to be ready..."
    local max_attempts=30
    local attempt=0
    
    while [[ $attempt -lt $max_attempts ]]; do
        if docker-compose exec -T postgres pg_isready -U rag_user &> /dev/null; then
            log SUCCESS "PostgreSQL is ready"
            return 0
        fi
        
        ((attempt++))
        log DEBUG "PostgreSQL not ready, attempt $attempt/$max_attempts"
        sleep 2
    done
    
    error_exit "PostgreSQL failed to become ready within 60 seconds"
}

wait_for_redis() {
    log INFO "Waiting for Redis to be ready..."
    local max_attempts=15
    local attempt=0
    
    while [[ $attempt -lt $max_attempts ]]; do
        if docker-compose exec -T redis redis-cli ping &> /dev/null; then
            log SUCCESS "Redis is ready"
            return 0
        fi
        
        ((attempt++))
        log DEBUG "Redis not ready, attempt $attempt/$max_attempts"
        sleep 2
    done
    
    error_exit "Redis failed to become ready within 30 seconds"
}

wait_for_kafka() {
    log INFO "Waiting for Kafka to be ready..."
    local max_attempts=20
    local attempt=0
    
    while [[ $attempt -lt $max_attempts ]]; do
        if docker-compose logs kafka | grep -q "started (kafka.server.KafkaServer)"; then
            log SUCCESS "Kafka is ready"
            return 0
        fi
        
        ((attempt++))
        log DEBUG "Kafka not ready, attempt $attempt/$max_attempts"
        sleep 3
    done
    
    log WARN "Kafka readiness check timed out, proceeding with deployment"
}

# Wait for application health checks
wait_for_health_checks() {
    if [[ "$HEALTH_CHECK" == "false" ]]; then
        log INFO "Skipping health checks"
        return 0
    fi
    
    log INFO "Waiting for application health checks..."
    
    local services_to_check=()
    if [[ -n "$SPECIFIC_SERVICES" ]]; then
        IFS=',' read -ra services_to_check <<< "$SPECIFIC_SERVICES"
    else
        services_to_check=($(docker-compose config --services | grep rag-))
    fi
    
    local max_attempts=60
    local attempt=0
    
    while [[ $attempt -lt $max_attempts ]]; do
        local healthy_services=0
        local total_services=${#services_to_check[@]}
        
        for service in "${services_to_check[@]}"; do
            # Get service port from docker-compose
            local port=$(docker-compose port "$service" 8080 2>/dev/null | cut -d: -f2)
            if [[ -z "$port" ]]; then
                # Try common service ports
                case $service in
                    rag-gateway) port=8080 ;;
                    rag-auth-service) port=8081 ;;
                    rag-document-service) port=8082 ;;
                    rag-embedding-service) port=8083 ;;
                    rag-core-service) port=8084 ;;
                    rag-admin-service) port=8085 ;;
                    *) continue ;;
                esac
            fi
            
            if curl -sf "http://localhost:$port/actuator/health" &> /dev/null; then
                ((healthy_services++))
            fi
        done
        
        if [[ $healthy_services -eq $total_services ]]; then
            log SUCCESS "All services are healthy ($healthy_services/$total_services)"
            return 0
        fi
        
        ((attempt++))
        log DEBUG "Health check progress: $healthy_services/$total_services healthy (attempt $attempt/$max_attempts)"
        sleep 5
    done
    
    log WARN "Health check timeout reached. Some services may not be fully ready."
    
    # Show service status
    log INFO "Current service status:"
    docker-compose ps
}

# Generate deployment report
generate_deployment_report() {
    log INFO "Generating deployment report..."
    
    local report_file="${DEPLOY_LOG_DIR}/deployment_report_$(date +%Y%m%d_%H%M%S).html"
    
    cat > "$report_file" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>RAG System Deployment Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background-color: #f0f0f0; padding: 20px; border-radius: 5px; }
        .section { margin: 20px 0; }
        .service { margin: 10px 0; padding: 10px; border: 1px solid #ddd; border-radius: 3px; }
        .success { background-color: #d4edda; }
        .warning { background-color: #fff3cd; }
        .error { background-color: #f8d7da; }
        table { width: 100%; border-collapse: collapse; margin: 15px 0; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; }
        pre { background-color: #f8f9fa; padding: 10px; border-radius: 3px; overflow-x: auto; }
    </style>
</head>
<body>
    <div class="header">
        <h1>RAG System Deployment Report</h1>
        <p>Environment: $ENVIRONMENT</p>
        <p>Deployed: $(date)</p>
        <p>Git Commit: $(git rev-parse HEAD 2>/dev/null || echo 'unknown')</p>
    </div>
    
    <div class="section">
        <h2>Deployment Summary</h2>
        <table>
            <tr><th>Service</th><th>Status</th><th>Replicas</th><th>Health Check</th></tr>
EOF

    # Add service status to report
    for service in $(docker-compose config --services); do
        local status="Unknown"
        local replicas="0"
        local health="Unknown"
        
        if docker-compose ps "$service" | grep -q "Up"; then
            status="Running"
            replicas=$(docker-compose ps "$service" | grep -c "Up" || echo "1")
            
            # Check health if it's an application service
            if [[ "$service" =~ ^rag- ]]; then
                local port=""
                case $service in
                    rag-gateway) port=8080 ;;
                    rag-auth-service) port=8081 ;;
                    rag-document-service) port=8082 ;;
                    rag-embedding-service) port=8083 ;;
                    rag-core-service) port=8084 ;;
                    rag-admin-service) port=8085 ;;
                esac
                
                if [[ -n "$port" ]] && curl -sf "http://localhost:$port/actuator/health" &> /dev/null; then
                    health="Healthy"
                else
                    health="Unhealthy"
                fi
            else
                health="N/A"
            fi
        else
            status="Stopped"
        fi
        
        echo "            <tr>" >> "$report_file"
        echo "                <td>$service</td>" >> "$report_file"
        echo "                <td>$status</td>" >> "$report_file"
        echo "                <td>$replicas</td>" >> "$report_file"
        echo "                <td>$health</td>" >> "$report_file"
        echo "            </tr>" >> "$report_file"
    done

    cat >> "$report_file" << EOF
        </table>
    </div>
    
    <div class="section">
        <h2>Container Status</h2>
        <pre>$(docker-compose ps)</pre>
    </div>
    
    <div class="section">
        <h2>System Resources</h2>
        <pre>$(docker stats --no-stream)</pre>
    </div>
    
    <div class="section">
        <h2>Service URLs</h2>
        <ul>
            <li><a href="http://localhost:8080/actuator/health">API Gateway Health</a></li>
            <li><a href="http://localhost:8081/swagger-ui.html">Auth Service API</a></li>
            <li><a href="http://localhost:8082/swagger-ui.html">Document Service API</a></li>
            <li><a href="http://localhost:8083/swagger-ui.html">Embedding Service API</a></li>
            <li><a href="http://localhost:8084/swagger-ui.html">Core Service API</a></li>
            <li><a href="http://localhost:8085/admin/api/swagger-ui.html">Admin Service API</a></li>
        </ul>
    </div>
</body>
</html>
EOF

    log SUCCESS "Deployment report generated: $report_file"
}

# Show deployment status
show_deployment_status() {
    echo ""
    echo "========================================"
    echo "        Deployment Status"
    echo "========================================"
    echo ""
    
    log INFO "Environment: $ENVIRONMENT"
    log INFO "Profile: $profile"
    log INFO "Replicas: $replicas"
    
    echo ""
    echo "Service Status:"
    docker-compose ps
    
    echo ""
    echo "Service URLs:"
    echo "- API Gateway: http://localhost:8080/actuator/health"
    echo "- Auth Service: http://localhost:8081/swagger-ui.html"
    echo "- Document Service: http://localhost:8082/swagger-ui.html"
    echo "- Embedding Service: http://localhost:8083/swagger-ui.html"
    echo "- Core Service: http://localhost:8084/swagger-ui.html"
    echo "- Admin Service: http://localhost:8085/admin/api/swagger-ui.html"
    
    echo ""
    echo "Management Commands:"
    echo "- View logs: docker-compose logs -f [service]"
    echo "- Scale service: docker-compose up -d --scale service=N"
    echo "- Stop all: docker-compose down"
    echo "- Update service: docker-compose up -d --force-recreate [service]"
}

# Main function
main() {
    echo "🚀 Enterprise RAG System - Docker Deployment"
    echo "============================================="
    
    parse_args "$@"
    
    log INFO "Starting deployment to $ENVIRONMENT environment"
    
    # Handle rollback
    if [[ "$ROLLBACK" == "true" ]]; then
        perform_rollback
        show_deployment_status
        return 0
    fi
    
    check_prerequisites
    create_backup
    
    # Build images if requested
    if [[ "$FORCE_BUILD" == "true" || "$NO_CACHE" == "true" || "$PULL_LATEST" == "true" ]]; then
        build_images
    fi
    
    deploy_services
    wait_for_health_checks
    generate_deployment_report
    show_deployment_status
    
    log SUCCESS "Deployment completed successfully! 🎉"
}

# Run main function with all arguments
main "$@"
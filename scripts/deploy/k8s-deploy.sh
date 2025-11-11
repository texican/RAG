#!/bin/bash

# Enterprise RAG System - Kubernetes Deployment Script
# This script handles deployment of the RAG system to Kubernetes clusters
#
# Usage: ./scripts/deploy/k8s-deploy.sh [options]
#
# Options:
#   --namespace <name>   Kubernetes namespace (default: rag-system)
#   --environment <env>  Deployment environment: dev|staging|production
#   --cluster <context>  Kubernetes cluster context
#   --replicas <count>   Number of replicas for each service
#   --image-tag <tag>    Docker image tag to deploy
#   --dry-run            Show what would be deployed without applying
#   --wait               Wait for rollout to complete
#   --rollback           Rollback to previous version

set -euo pipefail

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
K8S_MANIFESTS_DIR="${PROJECT_ROOT}/k8s"
DEPLOY_LOG_DIR="${PROJECT_ROOT}/logs/k8s-deployment"

# Default options
NAMESPACE="rag-system"
ENVIRONMENT="dev"
CLUSTER_CONTEXT=""
REPLICAS=1
IMAGE_TAG="latest"
DRY_RUN=false
WAIT=true
ROLLBACK=false
INIT=false

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m'

# Environment configurations (using case statement for bash 3.2 compatibility)
get_env_config() {
    case "$1" in
        dev)
            replicas=1
            resources_requests_cpu=100m
            resources_requests_memory=256Mi
            resources_limits_cpu=500m
            resources_limits_memory=512Mi
            ;;
        staging)
            replicas=2
            resources_requests_cpu=200m
            resources_requests_memory=512Mi
            resources_limits_cpu=1000m
            resources_limits_memory=1Gi
            ;;
        production)
            replicas=3
            resources_requests_cpu=500m
            resources_requests_memory=1Gi
            resources_limits_cpu=2000m
            resources_limits_memory=2Gi
            ;;
        *)
            return 1
            ;;
    esac
    return 0
}

# Kubernetes resource order
k8s_resources=(
    "namespace"
    "configmap"
    "secret"
    "pvc"
    "service"
    "deployment"
    "ingress"
    "hpa"
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
            echo "[$timestamp] [INFO] $message" >> "${DEPLOY_LOG_DIR}/k8s-deployment.log"
            ;;
        WARN)
            echo -e "${YELLOW}[WARN]${NC} $message"
            echo "[$timestamp] [WARN] $message" >> "${DEPLOY_LOG_DIR}/k8s-deployment.log"
            ;;
        ERROR)
            echo -e "${RED}[ERROR]${NC} $message"
            echo "[$timestamp] [ERROR] $message" >> "${DEPLOY_LOG_DIR}/k8s-deployment.log"
            ;;
        SUCCESS)
            echo -e "${GREEN}[SUCCESS]${NC} $message"
            echo "[$timestamp] [SUCCESS] $message" >> "${DEPLOY_LOG_DIR}/k8s-deployment.log"
            ;;
        DEBUG)
            echo -e "${BLUE}[DEBUG]${NC} $message"
            echo "[$timestamp] [DEBUG] $message" >> "${DEPLOY_LOG_DIR}/k8s-deployment.log"
            ;;
    esac
}

# Error handler
error_exit() {
    log ERROR "$1"
    log ERROR "Kubernetes deployment failed. Check logs at: ${DEPLOY_LOG_DIR}/k8s-deployment.log"
    exit 1
}

# Parse command line arguments
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --namespace)
                NAMESPACE="$2"
                shift 2
                ;;
            --environment)
                ENVIRONMENT="$2"
                shift 2
                ;;
            --cluster)
                CLUSTER_CONTEXT="$2"
                shift 2
                ;;
            --replicas)
                REPLICAS="$2"
                shift 2
                ;;
            --image-tag)
                IMAGE_TAG="$2"
                shift 2
                ;;
            --dry-run)
                DRY_RUN=true
                shift
                ;;
            --wait)
                WAIT=true
                shift
                ;;
            --rollback)
                ROLLBACK=true
                shift
                ;;
            --init)
                INIT=true
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
    if ! get_env_config "$ENVIRONMENT"; then
        error_exit "Invalid environment: $ENVIRONMENT. Valid options: dev, staging, production"
    fi
}

# Show help message
show_help() {
    cat << EOF
Enterprise RAG System - Kubernetes Deployment Script

Usage: $0 [options]

Options:
  --namespace <name>   Kubernetes namespace (default: rag-system)
  --environment <env>  Deployment environment: dev|staging|production (default: dev)
  --cluster <context>  Kubernetes cluster context to use
  --replicas <count>   Number of replicas for each service (overrides environment default)
  --image-tag <tag>    Docker image tag to deploy (default: latest)
  --dry-run            Show what would be deployed without applying changes
  --wait               Wait for rollout to complete (default: true)
  --rollback           Rollback to previous version
  --init               Initialize and create manifests from scratch
  -h, --help           Show this help message

Environments:
  dev                  - Development environment (1 replica, minimal resources)
  staging              - Staging environment (2 replicas, moderate resources)
  production           - Production environment (3 replicas, full resources)

Examples:
  $0                                          # Deploy to dev environment
  $0 --environment production --replicas 5   # Production deployment with 5 replicas
  $0 --namespace my-rag --image-tag v1.2.3   # Deploy specific version to custom namespace
  $0 --dry-run                               # Preview deployment without applying
  $0 --rollback                              # Rollback to previous version

Prerequisites:
  - kubectl installed and configured
  - Access to Kubernetes cluster
  - Docker images built and pushed to registry

Manifests directory: ${K8S_MANIFESTS_DIR}/
Logs directory: ${DEPLOY_LOG_DIR}/
EOF
}

# Check prerequisites
check_prerequisites() {
    log INFO "Checking Kubernetes deployment prerequisites..."
    
    # Check kubectl
    if ! command -v kubectl &> /dev/null; then
        error_exit "kubectl is not installed"
    fi
    
    # Set cluster context if specified
    if [[ -n "$CLUSTER_CONTEXT" ]]; then
        log INFO "Switching to cluster context: $CLUSTER_CONTEXT"
        if ! kubectl config use-context "$CLUSTER_CONTEXT"; then
            error_exit "Failed to switch to cluster context: $CLUSTER_CONTEXT"
        fi
    fi
    
    # Check cluster connectivity
    if ! kubectl cluster-info &> /dev/null; then
        error_exit "Cannot connect to Kubernetes cluster. Check your kubeconfig."
    fi
    
    # Check if namespace exists or can be created
    if ! kubectl get namespace "$NAMESPACE" &> /dev/null; then
        log INFO "Namespace '$NAMESPACE' does not exist. It will be created."
    fi
    
    # Check if Kubernetes manifests directory exists or can be created
    if [[ ! -d "$K8S_MANIFESTS_DIR" ]]; then
        log WARN "Kubernetes manifests directory not found: $K8S_MANIFESTS_DIR"
        if [[ "$INIT" == "true" ]]; then
            log INFO "Creating Kubernetes manifests..."
            create_k8s_manifests
        else
            error_exit "Kubernetes manifests directory not found. Use --init to create them."
        fi
    elif [[ "$INIT" == "true" ]]; then
        log INFO "Recreating Kubernetes manifests..."
        create_k8s_manifests
    fi
    
    log SUCCESS "Prerequisites check passed"
}

# Create Kubernetes manifests
create_k8s_manifests() {
    log INFO "Creating Kubernetes manifests..."
    
    mkdir -p "${K8S_MANIFESTS_DIR}"/{base,overlays/{dev,staging,production}}
    
    # Create namespace manifest
    cat > "${K8S_MANIFESTS_DIR}/base/namespace.yaml" << EOF
apiVersion: v1
kind: Namespace
metadata:
  name: rag-system
  labels:
    name: rag-system
    environment: "{{ environment }}"
EOF

    # Create ConfigMap for application configuration
    cat > "${K8S_MANIFESTS_DIR}/base/configmap.yaml" << EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: rag-config
  namespace: rag-system
data:
  application.yml: |
    server:
      port: 8080
    spring:
      profiles:
        active: "{{ environment }}"
      datasource:
        url: jdbc:postgresql://postgres:5432/rag_enterprise
        username: rag_user
        password: rag_password
      data:
        redis:
          host: redis
          port: 6379
      kafka:
        bootstrap-servers: kafka:9092
    logging:
      level:
        com.byo.rag: INFO
        org.springframework.security: WARN
EOF

    # Create Secret for sensitive data
    cat > "${K8S_MANIFESTS_DIR}/base/secret.yaml" << EOF
apiVersion: v1
kind: Secret
metadata:
  name: rag-secrets
  namespace: rag-system
type: Opaque
data:
  postgres-password: $(echo -n "rag_password" | base64)
  redis-password: $(echo -n "redis_password" | base64)
  jwt-secret: $(echo -n "admin-super-secret-key-that-should-be-at-least-256-bits-long-for-production-use" | base64)
  openai-api-key: $(echo -n "your-openai-api-key" | base64)
EOF

    # Create service manifests for each application service (gateway archived per ADR-001)
    local services=("rag-auth-service" "rag-admin-service" "rag-document-service" "rag-embedding-service" "rag-core-service")
    local ports=("8081" "8086" "8082" "8083" "8084")
    
    for i in "${!services[@]}"; do
        local service="${services[$i]}"
        local port="${ports[$i]}"
        
        # Create Service manifest
        cat > "${K8S_MANIFESTS_DIR}/base/${service}-service.yaml" << EOF
apiVersion: v1
kind: Service
metadata:
  name: ${service}
  namespace: rag-system
  labels:
    app: ${service}
spec:
  selector:
    app: ${service}
  ports:
    - port: 80
      targetPort: ${port}
      protocol: TCP
  type: ClusterIP
EOF

        # Create Deployment manifest
        cat > "${K8S_MANIFESTS_DIR}/base/${service}-deployment.yaml" << EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ${service}
  namespace: rag-system
  labels:
    app: ${service}
spec:
  replicas: {{ replicas }}
  selector:
    matchLabels:
      app: ${service}
  template:
    metadata:
      labels:
        app: ${service}
    spec:
      containers:
      - name: ${service}
        image: ${service}:{{ image_tag }}
        ports:
        - containerPort: ${port}
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "{{ environment }}"
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: rag-secrets
              key: postgres-password
        - name: REDIS_PASSWORD
          valueFrom:
            secretKeyRef:
              name: rag-secrets
              key: redis-password
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: rag-secrets
              key: jwt-secret
        - name: OPENAI_API_KEY
          valueFrom:
            secretKeyRef:
              name: rag-secrets
              key: openai-api-key
        volumeMounts:
        - name: config-volume
          mountPath: /app/config
        resources:
          requests:
            memory: "{{ resources_requests_memory }}"
            cpu: "{{ resources_requests_cpu }}"
          limits:
            memory: "{{ resources_limits_memory }}"
            cpu: "{{ resources_limits_cpu }}"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: ${port}
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: ${port}
          initialDelaySeconds: 30
          periodSeconds: 10
      volumes:
      - name: config-volume
        configMap:
          name: rag-config
EOF
    done
    
    # Create Ingress manifest
    cat > "${K8S_MANIFESTS_DIR}/base/ingress.yaml" << EOF
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: rag-ingress
  namespace: rag-system
  annotations:
    kubernetes.io/ingress.class: nginx
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  rules:
  - host: rag.{{ environment }}.local
    http:
      paths:
      # Gateway archived per ADR-001 - direct service routing
      - path: /auth
        pathType: Prefix
        backend:
          service:
            name: rag-auth-service
            port:
              number: 80
      - path: /admin
        pathType: Prefix
        backend:
          service:
            name: rag-admin-service
            port:
              number: 80
EOF

    # Create HPA manifests
    for service in "${services[@]}"; do
        cat > "${K8S_MANIFESTS_DIR}/base/${service}-hpa.yaml" << EOF
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: ${service}-hpa
  namespace: rag-system
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ${service}
  minReplicas: {{ replicas }}
  maxReplicas: $(( replicas * 3 ))
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
EOF
    done
    
    log SUCCESS "Kubernetes manifests created in: $K8S_MANIFESTS_DIR"
}

# Process manifest templates
process_manifests() {
    log INFO "Processing Kubernetes manifests for $ENVIRONMENT environment..."
    
    # If not using --init, use existing base manifests
    if [[ "$INIT" == "false" && -d "${K8S_MANIFESTS_DIR}/base" ]]; then
        log INFO "Using existing manifests from ${K8S_MANIFESTS_DIR}/base"
        
        local processed_dir="${K8S_MANIFESTS_DIR}/processed/${ENVIRONMENT}"
        mkdir -p "$processed_dir"
        
        # Copy base manifests to processed directory
        cp -r "${K8S_MANIFESTS_DIR}/base"/*.yaml "$processed_dir/" 2>/dev/null || true
        
        log SUCCESS "Using existing base manifests"
        return 0
    fi
    
    local processed_dir="${K8S_MANIFESTS_DIR}/processed/${ENVIRONMENT}"
    mkdir -p "$processed_dir"
    
    # Process each manifest file
    for manifest_file in "${K8S_MANIFESTS_DIR}/base"/*.yaml; do
        if [[ -f "$manifest_file" ]]; then
            local filename=$(basename "$manifest_file")
            local processed_file="${processed_dir}/${filename}"
            
            # Process template variables
            sed -e "s/{{ environment }}/${ENVIRONMENT}/g" \
                -e "s/{{ replicas }}/${replicas}/g" \
                -e "s/{{ image_tag }}/${IMAGE_TAG}/g" \
                -e "s/{{ resources_requests_cpu }}/${resources_requests_cpu}/g" \
                -e "s/{{ resources_requests_memory }}/${resources_requests_memory}/g" \
                -e "s/{{ resources_limits_cpu }}/${resources_limits_cpu}/g" \
                -e "s/{{ resources_limits_memory }}/${resources_limits_memory}/g" \
                -e "s/rag-system/${NAMESPACE}/g" \
                "$manifest_file" > "$processed_file"
            
            log DEBUG "Processed manifest: $filename"
        fi
    done
    
    log SUCCESS "Manifests processed for $ENVIRONMENT environment"
}

# Deploy to Kubernetes
deploy_to_k8s() {
    log INFO "Deploying RAG system to Kubernetes namespace: $NAMESPACE"
    
    local processed_dir="${K8S_MANIFESTS_DIR}/processed/${ENVIRONMENT}"
    
    # Apply manifests in order
    for resource_type in "${k8s_resources[@]}"; do
        manifest_files=""
        
        # Find manifest files for this resource type
        case $resource_type in
            namespace)
                manifest_files="${processed_dir}/namespace.yaml"
                ;;
            configmap)
                manifest_files="${processed_dir}/configmap.yaml"
                ;;
            secret)
                manifest_files="${processed_dir}/secret.yaml"
                ;;
            pvc)
                manifest_files=$(find "$processed_dir" -name "*pvc*.yaml" 2>/dev/null || true)
                ;;
            service)
                manifest_files=$(find "$processed_dir" -name "*service.yaml" 2>/dev/null || true)
                ;;
            deployment)
                manifest_files=$(find "$processed_dir" -name "*deployment.yaml" 2>/dev/null || true)
                ;;
            ingress)
                manifest_files="${processed_dir}/ingress.yaml"
                ;;
            hpa)
                manifest_files=$(find "$processed_dir" -name "*hpa.yaml" 2>/dev/null || true)
                ;;
        esac
        
        # Apply each manifest file
        for manifest_file in $manifest_files; do
            if [[ -f "$manifest_file" ]]; then
                log INFO "Applying $(basename "$manifest_file")..."
                
                if [[ "$DRY_RUN" == "true" ]]; then
                    kubectl apply -f "$manifest_file" --dry-run=client -o yaml
                else
                    kubectl apply -f "$manifest_file"
                fi
            fi
        done
    done
    
    if [[ "$DRY_RUN" == "true" ]]; then
        log INFO "Dry run completed. No changes were applied."
        return 0
    fi
    
    log SUCCESS "All Kubernetes resources deployed successfully"
}

# Wait for deployment rollout
wait_for_rollout() {
    if [[ "$WAIT" == "false" || "$DRY_RUN" == "true" ]]; then
        log INFO "Skipping rollout wait"
        return 0
    fi
    
    log INFO "Waiting for deployment rollout to complete..."
    
    local deployments=($(kubectl get deployments -n "$NAMESPACE" -o jsonpath='{.items[*].metadata.name}'))
    
    for deployment in "${deployments[@]}"; do
        log INFO "Waiting for deployment/$deployment rollout..."
        
        if kubectl rollout status deployment/"$deployment" -n "$NAMESPACE" --timeout=300s; then
            log SUCCESS "Deployment $deployment rolled out successfully"
        else
            log ERROR "Deployment $deployment rollout failed or timed out"
            return 1
        fi
    done
    
    log SUCCESS "All deployments rolled out successfully"
}

# Perform rollback
perform_rollback() {
    log INFO "Performing rollback..."
    
    local deployments=($(kubectl get deployments -n "$NAMESPACE" -o jsonpath='{.items[*].metadata.name}' 2>/dev/null || true))
    
    if [[ ${#deployments[@]} -eq 0 ]]; then
        error_exit "No deployments found in namespace $NAMESPACE"
    fi
    
    for deployment in "${deployments[@]}"; do
        log INFO "Rolling back deployment/$deployment..."
        
        if kubectl rollout undo deployment/"$deployment" -n "$NAMESPACE"; then
            log SUCCESS "Rollback initiated for $deployment"
        else
            log ERROR "Failed to rollback $deployment"
        fi
    done
    
    # Wait for rollback to complete
    if [[ "$WAIT" == "true" ]]; then
        wait_for_rollout
    fi
    
    log SUCCESS "Rollback completed"
}

# Show deployment status
show_deployment_status() {
    echo ""
    echo "========================================"
    echo "     Kubernetes Deployment Status"
    echo "========================================"
    echo ""
    
    log INFO "Namespace: $NAMESPACE"
    log INFO "Environment: $ENVIRONMENT"
    log INFO "Image Tag: $IMAGE_TAG"
    
    echo ""
    echo "Deployments:"
    kubectl get deployments -n "$NAMESPACE" -o wide 2>/dev/null || echo "No deployments found"
    
    echo ""
    echo "Services:"
    kubectl get services -n "$NAMESPACE" -o wide 2>/dev/null || echo "No services found"
    
    echo ""
    echo "Pods:"
    kubectl get pods -n "$NAMESPACE" -o wide 2>/dev/null || echo "No pods found"
    
    echo ""
    echo "Ingress:"
    kubectl get ingress -n "$NAMESPACE" -o wide 2>/dev/null || echo "No ingress found"
    
    echo ""
    echo "Management Commands:"
    echo "- View logs: kubectl logs -f deployment/<service> -n $NAMESPACE"
    echo "- Scale deployment: kubectl scale deployment <service> --replicas=N -n $NAMESPACE"
    echo "- Port forward: kubectl port-forward service/<service> 8080:80 -n $NAMESPACE"
    echo "- Delete namespace: kubectl delete namespace $NAMESPACE"
}

# Generate deployment report
generate_deployment_report() {
    log INFO "Generating Kubernetes deployment report..."
    
    local report_file="${DEPLOY_LOG_DIR}/k8s_deployment_report_$(date +%Y%m%d_%H%M%S).json"
    
    cat > "$report_file" << EOF
{
    "deployment_info": {
        "timestamp": "$(date -Iseconds)",
        "namespace": "$NAMESPACE",
        "environment": "$ENVIRONMENT",
        "image_tag": "$IMAGE_TAG",
        "replicas": $replicas,
        "cluster_context": "$(kubectl config current-context)"
    },
    "resources": {
        "deployments": $(kubectl get deployments -n "$NAMESPACE" -o json 2>/dev/null || echo '{"items":[]}'),
        "services": $(kubectl get services -n "$NAMESPACE" -o json 2>/dev/null || echo '{"items":[]}'),
        "pods": $(kubectl get pods -n "$NAMESPACE" -o json 2>/dev/null || echo '{"items":[]}')
    }
}
EOF

    log SUCCESS "Deployment report generated: $report_file"
}

# Main function
main() {
    echo "☸️  Enterprise RAG System - Kubernetes Deployment"
    echo "================================================="
    
    parse_args "$@"
    
    log INFO "Starting Kubernetes deployment to $ENVIRONMENT environment"
    
    # Handle rollback
    if [[ "$ROLLBACK" == "true" ]]; then
        perform_rollback
        show_deployment_status
        return 0
    fi
    
    check_prerequisites
    process_manifests
    deploy_to_k8s
    wait_for_rollout
    generate_deployment_report
    show_deployment_status
    
    log SUCCESS "Kubernetes deployment completed successfully! ☸️"
}

# Run main function with all arguments
main "$@"
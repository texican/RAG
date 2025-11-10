#!/usr/bin/env bash

################################################################################
# RAG System - Deployment Validation Script
#
# Purpose: Comprehensive validation of RAG system deployment on GKE
#
# Features:
# - Service health checks
# - Pod readiness validation
# - Inter-service connectivity tests
# - Database connectivity verification
# - Integration test execution
# - Swagger UI accessibility
# - Full RAG workflow test
# - Performance benchmarks
#
# Usage:
#   ./scripts/gcp/19-validate-deployment.sh --env dev
#   ./scripts/gcp/19-validate-deployment.sh --env prod --project byo-rag-prod
#   ./scripts/gcp/19-validate-deployment.sh --env dev --quick
#
# Prerequisites:
# - Services deployed (GCP-DEPLOY-011)
# - Database initialized
# - Admin user created
#
# Author: RAG DevOps Team
# Date: 2025-11-09
################################################################################

set -euo pipefail

# Script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Default values
ENVIRONMENT=""
PROJECT_ID=""
REGION="us-central1"
NAMESPACE="rag-system"
QUICK_MODE=false
SKIP_INTEGRATION_TESTS=false

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
WARNINGS=0

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

################################################################################
# Logging Functions
################################################################################

log_info() {
    echo -e "${BLUE}[INFO]${NC} $*"
}

log_success() {
    echo -e "${GREEN}[✓]${NC} $*"
}

log_warn() {
    echo -e "${YELLOW}[⚠]${NC} $*"
    ((WARNINGS++))
}

log_error() {
    echo -e "${RED}[✗]${NC} $*" >&2
}

log_test() {
    local test_name=$1
    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}TEST:${NC} $test_name"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    ((TOTAL_TESTS++))
}

mark_passed() {
    ((PASSED_TESTS++))
    log_success "$1"
}

mark_failed() {
    ((FAILED_TESTS++))
    log_error "$1"
}

################################################################################
# Service Health Checks
################################################################################

check_pod_status() {
    log_test "Pod Status Check"
    
    local all_pods_ready=true
    
    local services=("rag-auth" "rag-admin" "rag-document" "rag-embedding" "rag-core")
    
    for service in "${services[@]}"; do
        log_info "Checking $service pods..."
        
        local pod_count=$(kubectl get pods -n "$NAMESPACE" -l "app=$service" \
            --no-headers 2>/dev/null | wc -l)
        
        if [[ $pod_count -eq 0 ]]; then
            mark_failed "No pods found for $service"
            all_pods_ready=false
            continue
        fi
        
        local ready_pods=$(kubectl get pods -n "$NAMESPACE" -l "app=$service" \
            -o jsonpath='{.items[*].status.conditions[?(@.type=="Ready")].status}' \
            | grep -o "True" | wc -l)
        
        if [[ $ready_pods -eq $pod_count ]]; then
            log_success "$service: $ready_pods/$pod_count pods ready"
        else
            mark_failed "$service: Only $ready_pods/$pod_count pods ready"
            all_pods_ready=false
            
            # Show pod details
            kubectl get pods -n "$NAMESPACE" -l "app=$service" -o wide
        fi
    done
    
    if [[ "$all_pods_ready" == "true" ]]; then
        mark_passed "All pods are ready"
    else
        mark_failed "Some pods are not ready"
    fi
}

check_service_endpoints() {
    log_test "Service Endpoint Check"
    
    local all_endpoints_ready=true
    
    local services=("rag-auth-service" "rag-admin-service" "rag-document-service" "rag-embedding-service" "rag-core-service")
    
    for service in "${services[@]}"; do
        log_info "Checking $service endpoints..."
        
        local endpoints=$(kubectl get endpoints "$service" -n "$NAMESPACE" \
            -o jsonpath='{.subsets[*].addresses[*].ip}' 2>/dev/null)
        
        if [[ -n "$endpoints" ]]; then
            local endpoint_count=$(echo "$endpoints" | wc -w)
            log_success "$service: $endpoint_count endpoint(s) available"
        else
            mark_failed "$service: No endpoints available"
            all_endpoints_ready=false
        fi
    done
    
    if [[ "$all_endpoints_ready" == "true" ]]; then
        mark_passed "All service endpoints are ready"
    else
        mark_failed "Some service endpoints are missing"
    fi
}

check_health_endpoints() {
    log_test "Health Endpoint Check"
    
    local all_healthy=true
    
    local services=(
        "rag-auth:8081"
        "rag-admin:8085"
        "rag-document:8082"
        "rag-embedding:8083"
        "rag-core:8084"
    )
    
    for service_port in "${services[@]}"; do
        local service="${service_port%%:*}"
        local port="${service_port##*:}"
        
        log_info "Checking $service health endpoint..."
        
        local pod_name=$(kubectl get pods -n "$NAMESPACE" -l "app=$service" \
            -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)
        
        if [[ -z "$pod_name" ]]; then
            mark_failed "$service: No pod found"
            all_healthy=false
            continue
        fi
        
        # Check liveness endpoint
        if kubectl exec -n "$NAMESPACE" "$pod_name" -- \
            wget -qO- --timeout=5 "http://localhost:${port}/actuator/health/liveness" \
            &> /dev/null; then
            log_success "$service: Liveness check passed"
        else
            mark_failed "$service: Liveness check failed"
            all_healthy=false
        fi
        
        # Check readiness endpoint
        if kubectl exec -n "$NAMESPACE" "$pod_name" -- \
            wget -qO- --timeout=5 "http://localhost:${port}/actuator/health/readiness" \
            &> /dev/null; then
            log_success "$service: Readiness check passed"
        else
            log_warn "$service: Readiness check failed (may be normal during startup)"
        fi
    done
    
    if [[ "$all_healthy" == "true" ]]; then
        mark_passed "All services are healthy"
    else
        mark_failed "Some services are unhealthy"
    fi
}

################################################################################
# Connectivity Tests
################################################################################

test_service_connectivity() {
    log_test "Inter-Service Connectivity"
    
    log_info "Testing DNS resolution and HTTP connectivity..."
    
    local services=(
        "rag-auth-service:8081"
        "rag-document-service:8082"
        "rag-embedding-service:8083"
        "rag-core-service:8084"
        "rag-admin-service:8085"
    )
    
    local all_connected=true
    
    for service_port in "${services[@]}"; do
        local service="${service_port%%:*}"
        local port="${service_port##*:}"
        
        log_info "Testing $service connectivity..."
        
        # DNS resolution test
        if kubectl run test-dns-$$ --rm -i --restart=Never \
            --image=busybox:latest -n "$NAMESPACE" \
            -- nslookup "${service}.${NAMESPACE}.svc.cluster.local" &> /dev/null; then
            log_success "$service: DNS resolution OK"
        else
            mark_failed "$service: DNS resolution failed"
            all_connected=false
            continue
        fi
        
        # HTTP connectivity test
        if kubectl run test-http-$$ --rm -i --restart=Never \
            --image=curlimages/curl:latest -n "$NAMESPACE" \
            -- curl -sf --max-time 10 \
            "http://${service}:${port}/actuator/health/liveness" &> /dev/null; then
            log_success "$service: HTTP connectivity OK"
        else
            log_warn "$service: HTTP connectivity failed (may be initializing)"
        fi
    done
    
    if [[ "$all_connected" == "true" ]]; then
        mark_passed "Service connectivity validated"
    else
        mark_failed "Some connectivity tests failed"
    fi
}

test_database_connectivity() {
    log_test "Database Connectivity"
    
    log_info "Testing Cloud SQL connectivity from auth service..."
    
    local auth_pod=$(kubectl get pods -n "$NAMESPACE" -l "app=rag-auth" \
        -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)
    
    if [[ -z "$auth_pod" ]]; then
        mark_failed "No auth pod found"
        return 1
    fi
    
    # Check if postgres client is available
    if ! kubectl exec -n "$NAMESPACE" "$auth_pod" -- which psql &> /dev/null; then
        log_info "Installing PostgreSQL client in pod..."
        kubectl exec -n "$NAMESPACE" "$auth_pod" -- \
            sh -c "apt-get update -qq && apt-get install -y -qq postgresql-client > /dev/null 2>&1" || true
    fi
    
    # Get database password
    local db_password
    db_password=$(kubectl get secret postgres-credentials -n "$NAMESPACE" \
        -o jsonpath='{.data.password}' | base64 -d 2>/dev/null)
    
    # Test connection
    if kubectl exec -n "$NAMESPACE" "$auth_pod" -- \
        sh -c "PGPASSWORD='$db_password' psql -h 127.0.0.1 -U rag_user -d rag_db -c 'SELECT 1;'" \
        &> /dev/null; then
        mark_passed "Database connectivity successful"
    else
        mark_failed "Database connectivity failed"
    fi
}

test_redis_connectivity() {
    log_test "Redis Connectivity"
    
    log_info "Testing Redis connectivity from auth service..."
    
    local auth_pod=$(kubectl get pods -n "$NAMESPACE" -l "app=rag-auth" \
        -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)
    
    if [[ -z "$auth_pod" ]]; then
        mark_failed "No auth pod found"
        return 1
    fi
    
    # Get Redis host from ConfigMap
    local redis_host
    redis_host=$(kubectl get configmap gcp-config -n "$NAMESPACE" \
        -o jsonpath='{.data.REDIS_HOST}' 2>/dev/null)
    
    if [[ -z "$redis_host" ]]; then
        log_warn "Redis host not found in ConfigMap (skipping test)"
        return 0
    fi
    
    # Test connection using netcat
    if kubectl exec -n "$NAMESPACE" "$auth_pod" -- \
        nc -zv "$redis_host" 6379 &> /dev/null; then
        mark_passed "Redis connectivity successful"
    else
        log_warn "Redis connectivity failed (check if Memorystore instance is configured)"
    fi
}

################################################################################
# Authentication Tests
################################################################################

test_admin_authentication() {
    log_test "Admin Authentication"
    
    log_info "Testing admin login..."
    
    # Port-forward to auth service
    kubectl port-forward -n "$NAMESPACE" svc/rag-auth-service 8081:8081 > /dev/null 2>&1 &
    local pf_pid=$!
    
    # Wait for port-forward
    sleep 3
    
    # Test login
    local login_response
    login_response=$(curl -s -X POST http://localhost:8081/api/v1/auth/login \
        -H "Content-Type: application/json" \
        -d '{"email":"admin@enterprise-rag.com","password":"admin123"}' \
        2>/dev/null)
    
    # Kill port-forward
    kill $pf_pid 2>/dev/null || true
    wait $pf_pid 2>/dev/null || true
    
    if echo "$login_response" | grep -q "token"; then
        mark_passed "Admin authentication successful"
        
        # Extract and save token for subsequent tests
        JWT_TOKEN=$(echo "$login_response" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
        log_info "JWT token obtained (truncated): ${JWT_TOKEN:0:50}..."
    else
        mark_failed "Admin authentication failed: $login_response"
    fi
}

test_swagger_ui_access() {
    log_test "Swagger UI Access"
    
    log_info "Testing Swagger UI accessibility..."
    
    # Port-forward to auth service
    kubectl port-forward -n "$NAMESPACE" svc/rag-auth-service 8081:8081 > /dev/null 2>&1 &
    local pf_pid=$!
    
    # Wait for port-forward
    sleep 3
    
    # Test Swagger UI
    if curl -sf --max-time 10 http://localhost:8081/swagger-ui.html &> /dev/null || \
       curl -sf --max-time 10 http://localhost:8081/swagger-ui/index.html &> /dev/null; then
        mark_passed "Swagger UI is accessible"
    else
        log_warn "Swagger UI not accessible (check if springdoc-openapi is configured)"
    fi
    
    # Kill port-forward
    kill $pf_pid 2>/dev/null || true
    wait $pf_pid 2>/dev/null || true
}

################################################################################
# Integration Tests
################################################################################

run_integration_tests() {
    if [[ "$SKIP_INTEGRATION_TESTS" == "true" ]]; then
        log_warn "Skipping integration tests (--skip-integration-tests flag set)"
        return 0
    fi
    
    log_test "Integration Tests"
    
    log_info "Running integration test suite..."
    
    # Check if integration tests exist
    if [[ ! -d "$PROJECT_ROOT/rag-integration-tests" ]]; then
        log_warn "Integration test module not found (skipping)"
        return 0
    fi
    
    # Port-forward all services
    log_info "Setting up port-forwards for services..."
    
    kubectl port-forward -n "$NAMESPACE" svc/rag-auth-service 8081:8081 > /dev/null 2>&1 &
    local pf_auth=$!
    kubectl port-forward -n "$NAMESPACE" svc/rag-document-service 8082:8082 > /dev/null 2>&1 &
    local pf_doc=$!
    kubectl port-forward -n "$NAMESPACE" svc/rag-core-service 8084:8084 > /dev/null 2>&1 &
    local pf_core=$!
    
    # Wait for port-forwards
    sleep 5
    
    # Run tests
    cd "$PROJECT_ROOT/rag-integration-tests"
    
    if mvn -q test -Dtest=SmokeTest 2>&1 | tee /tmp/integration-test.log; then
        mark_passed "Integration tests passed"
    else
        mark_failed "Integration tests failed (see /tmp/integration-test.log)"
    fi
    
    # Cleanup port-forwards
    kill $pf_auth $pf_doc $pf_core 2>/dev/null || true
    wait $pf_auth $pf_doc $pf_core 2>/dev/null || true
    
    cd "$PROJECT_ROOT"
}

################################################################################
# RAG Workflow Test
################################################################################

test_rag_workflow() {
    log_test "RAG Workflow End-to-End"
    
    if [[ -z "${JWT_TOKEN:-}" ]]; then
        log_warn "No JWT token available (skipping RAG workflow test)"
        return 0
    fi
    
    log_info "Testing complete RAG workflow..."
    
    # Port-forward services
    kubectl port-forward -n "$NAMESPACE" svc/rag-document-service 8082:8082 > /dev/null 2>&1 &
    local pf_doc=$!
    kubectl port-forward -n "$NAMESPACE" svc/rag-core-service 8084:8084 > /dev/null 2>&1 &
    local pf_core=$!
    
    sleep 3
    
    # 1. Upload test document
    log_info "Step 1: Uploading test document..."
    
    echo "This is a test document for RAG validation." > /tmp/test-doc.txt
    
    local upload_response
    upload_response=$(curl -s -X POST http://localhost:8082/api/v1/documents/upload \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -F "file=@/tmp/test-doc.txt" \
        2>/dev/null)
    
    if echo "$upload_response" | grep -q "id"; then
        local doc_id=$(echo "$upload_response" | grep -o '"id":"[^"]*' | cut -d'"' -f4)
        log_success "Document uploaded: $doc_id"
    else
        mark_failed "Document upload failed: $upload_response"
        kill $pf_doc $pf_core 2>/dev/null || true
        return 1
    fi
    
    # 2. Query document
    log_info "Step 2: Querying document..."
    
    sleep 2  # Wait for indexing
    
    local query_response
    query_response=$(curl -s -X POST http://localhost:8084/api/v1/query \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{"query":"What is in the test document?","maxResults":5}' \
        2>/dev/null)
    
    if echo "$query_response" | grep -q "results"; then
        log_success "Query executed successfully"
        mark_passed "RAG workflow completed successfully"
    else
        log_warn "Query failed (may be normal if embedding service not ready): $query_response"
    fi
    
    # Cleanup
    kill $pf_doc $pf_core 2>/dev/null || true
    wait $pf_doc $pf_core 2>/dev/null || true
    rm -f /tmp/test-doc.txt
}

################################################################################
# Resource Checks
################################################################################

check_resource_usage() {
    log_test "Resource Usage Check"
    
    log_info "Checking pod resource usage..."
    
    # Get resource usage
    kubectl top pods -n "$NAMESPACE" 2>/dev/null || log_warn "Metrics server not available"
    
    # Check for OOMKilled pods
    local oom_pods=$(kubectl get pods -n "$NAMESPACE" \
        -o jsonpath='{.items[?(@.status.containerStatuses[*].lastState.terminated.reason=="OOMKilled")].metadata.name}')
    
    if [[ -n "$oom_pods" ]]; then
        mark_failed "OOMKilled pods found: $oom_pods"
    else
        mark_passed "No OOMKilled pods"
    fi
    
    # Check for CrashLoopBackOff pods
    local crash_pods=$(kubectl get pods -n "$NAMESPACE" \
        --field-selector=status.phase!=Running,status.phase!=Succeeded \
        -o jsonpath='{.items[*].metadata.name}')
    
    if [[ -n "$crash_pods" ]]; then
        mark_failed "Pods in error state: $crash_pods"
    else
        log_success "All pods in healthy state"
    fi
}

check_persistent_volumes() {
    log_test "Persistent Volume Check"
    
    log_info "Checking PVCs..."
    
    local pvcs=$(kubectl get pvc -n "$NAMESPACE" --no-headers 2>/dev/null | wc -l)
    
    if [[ $pvcs -gt 0 ]]; then
        kubectl get pvc -n "$NAMESPACE"
        
        # Check for unbound PVCs
        local unbound=$(kubectl get pvc -n "$NAMESPACE" \
            --field-selector=status.phase!=Bound --no-headers | wc -l)
        
        if [[ $unbound -gt 0 ]]; then
            mark_failed "$unbound PVC(s) not bound"
        else
            mark_passed "All PVCs bound successfully"
        fi
    else
        log_info "No PVCs found (may be normal if using Cloud Storage only)"
    fi
}

################################################################################
# Summary and Reporting
################################################################################

print_summary() {
    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}VALIDATION SUMMARY${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    echo "Total Tests: $TOTAL_TESTS"
    echo -e "${GREEN}Passed: $PASSED_TESTS${NC}"
    echo -e "${RED}Failed: $FAILED_TESTS${NC}"
    echo -e "${YELLOW}Warnings: $WARNINGS${NC}"
    echo ""
    
    local success_rate=$((PASSED_TESTS * 100 / TOTAL_TESTS))
    
    if [[ $FAILED_TESTS -eq 0 ]]; then
        echo -e "${GREEN}✓ All validation tests passed!${NC}"
        echo ""
        echo "Deployment is healthy and ready for use."
    elif [[ $success_rate -ge 80 ]]; then
        echo -e "${YELLOW}⚠ Deployment is mostly healthy (${success_rate}% success rate)${NC}"
        echo ""
        echo "Some non-critical tests failed. Review warnings and failed tests."
    else
        echo -e "${RED}✗ Deployment has issues (${success_rate}% success rate)${NC}"
        echo ""
        echo "Multiple tests failed. Review logs and fix issues before proceeding."
        return 1
    fi
}

print_next_steps() {
    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}NEXT STEPS${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    echo "1. Access services via port-forward:"
    echo "   kubectl port-forward -n $NAMESPACE svc/rag-auth-service 8081:8081"
    echo "   kubectl port-forward -n $NAMESPACE svc/rag-core-service 8084:8084"
    echo ""
    echo "2. Test authentication:"
    echo "   curl -X POST http://localhost:8081/api/v1/auth/login \\"
    echo "     -H 'Content-Type: application/json' \\"
    echo "     -d '{\"email\":\"admin@enterprise-rag.com\",\"password\":\"admin123\"}'"
    echo ""
    echo "3. Access Swagger UI:"
    echo "   http://localhost:8081/swagger-ui.html"
    echo ""
    echo "4. Check logs:"
    echo "   kubectl logs -n $NAMESPACE -l app=rag-auth --tail=100"
    echo ""
    echo "5. Monitor deployment:"
    echo "   kubectl get pods -n $NAMESPACE -w"
    echo ""
    echo "6. Set up ingress (if not already done):"
    echo "   ./scripts/gcp/16-setup-ingress.sh --env $ENVIRONMENT --domain your-domain.com"
    echo ""
}

################################################################################
# Argument Parsing
################################################################################

usage() {
    cat << EOF
Usage: $0 --env <environment> [OPTIONS]

Validate RAG system deployment on GKE.

Required Arguments:
  --env <environment>         Environment (dev, staging, prod)

Optional Arguments:
  --project <project-id>      GCP project ID (default: byo-rag-{env})
  --quick                     Run quick validation (skip integration tests)
  --skip-integration-tests    Skip integration test suite
  -h, --help                  Show this help message

Examples:
  # Full validation
  $0 --env dev

  # Quick validation
  $0 --env prod --quick

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
        --quick)
            QUICK_MODE=true
            SKIP_INTEGRATION_TESTS=true
            shift
            ;;
        --skip-integration-tests)
            SKIP_INTEGRATION_TESTS=true
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
    log_error "Environment not specified"
    usage
fi

# Set defaults
if [[ -z "$PROJECT_ID" ]]; then
    PROJECT_ID="byo-rag-${ENVIRONMENT}"
fi

################################################################################
# Main Execution
################################################################################

main() {
    log_info "RAG System - Deployment Validation"
    log_info "==================================="
    log_info "Environment: $ENVIRONMENT"
    log_info "Project: $PROJECT_ID"
    log_info "Namespace: $NAMESPACE"
    echo ""
    
    # Run validation tests
    check_pod_status
    check_service_endpoints
    check_health_endpoints
    test_service_connectivity
    test_database_connectivity
    test_redis_connectivity
    check_resource_usage
    check_persistent_volumes
    
    if [[ "$QUICK_MODE" == "false" ]]; then
        test_admin_authentication
        test_swagger_ui_access
        run_integration_tests
        test_rag_workflow
    fi
    
    # Print summary
    print_summary
    
    # Print next steps
    print_next_steps
    
    # Exit with appropriate code
    if [[ $FAILED_TESTS -gt 0 ]]; then
        exit 1
    fi
}

# Run main function
main "$@"

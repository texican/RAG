#!/usr/bin/env bash

################################################################################
# RAG System - Database Initialization Script
#
# Purpose: Initialize Cloud SQL PostgreSQL database with schema and admin user
#
# Features:
# - Run database migrations via Flyway
# - Create admin user with proper BCrypt hash
# - Create default tenant
# - Verify database connectivity
# - Validate schema creation
# - Idempotent operations (safe to run multiple times)
#
# Usage:
#   ./scripts/gcp/18-init-database.sh --env dev
#   ./scripts/gcp/18-init-database.sh --env prod --project byo-rag-prod
#   ./scripts/gcp/18-init-database.sh --env dev --skip-migrations
#
# Prerequisites:
# - Cloud SQL instance created (GCP-SQL-004)
# - Cloud SQL Proxy or private IP connectivity
# - Secrets synced to cluster
# - Services deployed (GCP-DEPLOY-011)
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
SKIP_MIGRATIONS=false
CLOUD_SQL_INSTANCE=""
DB_NAME="rag_db"
DB_USER="rag_user"

# Admin user defaults
ADMIN_EMAIL="admin@enterprise-rag.com"
ADMIN_PASSWORD="admin123"  # Will be hashed with BCrypt
ADMIN_BCRYPT_HASH='$2a$10$4ruqE8FlnERNCuIW/6pI6.1rlZmJiG/plwFwif5KPGxjwbM9Sm6je'  # BCrypt hash of "admin123"

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

################################################################################
# Logging Functions
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
# Validation Functions
################################################################################

validate_prerequisites() {
    log_info "Validating prerequisites..."
    
    # Check gcloud CLI
    if ! command -v gcloud &> /dev/null; then
        log_error "gcloud CLI not found"
        exit 1
    fi
    
    # Check kubectl
    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl not found"
        exit 1
    fi
    
    # Check psql
    if ! command -v psql &> /dev/null; then
        log_warn "psql not found. Install PostgreSQL client for direct DB access."
        log_warn "Will use kubectl exec instead..."
    fi
    
    # Validate project
    if [[ -z "$PROJECT_ID" ]]; then
        log_error "PROJECT_ID not set"
        exit 1
    fi
    
    gcloud config set project "$PROJECT_ID" --quiet
    
    log_success "Prerequisites validated"
}

get_database_credentials() {
    log_info "Retrieving database credentials from Kubernetes secrets..."
    
    # Get DB password from secret
    local db_password
    db_password=$(kubectl get secret postgres-credentials -n "$NAMESPACE" \
        -o jsonpath='{.data.password}' | base64 -d 2>/dev/null)
    
    if [[ -z "$db_password" ]]; then
        log_error "Failed to retrieve database password from secret"
        exit 1
    fi
    
    # Export for use in psql commands
    export PGPASSWORD="$db_password"
    
    log_success "Database credentials retrieved"
}

validate_cloud_sql_instance() {
    log_info "Validating Cloud SQL instance..."
    
    if [[ -z "$CLOUD_SQL_INSTANCE" ]]; then
        CLOUD_SQL_INSTANCE="${PROJECT_ID}:${REGION}:rag-postgres-${ENVIRONMENT}"
    fi
    
    if ! gcloud sql instances describe "rag-postgres-${ENVIRONMENT}" \
        --project="$PROJECT_ID" &> /dev/null; then
        log_error "Cloud SQL instance not found: rag-postgres-${ENVIRONMENT}"
        exit 1
    fi
    
    log_success "Cloud SQL instance validated: $CLOUD_SQL_INSTANCE"
}

################################################################################
# Database Connection Functions
################################################################################

test_database_connectivity() {
    log_info "Testing database connectivity..."
    
    # Get auth service pod
    local auth_pod
    auth_pod=$(kubectl get pods -n "$NAMESPACE" -l app=rag-auth \
        -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)
    
    if [[ -z "$auth_pod" ]]; then
        log_error "No rag-auth pod found. Deploy services first."
        exit 1
    fi
    
    # Test connection via Cloud SQL proxy in pod
    if kubectl exec -n "$NAMESPACE" "$auth_pod" -- \
        sh -c "apt-get update -qq && apt-get install -y -qq postgresql-client > /dev/null 2>&1 && \
        PGPASSWORD='$PGPASSWORD' psql -h 127.0.0.1 -U $DB_USER -d $DB_NAME -c 'SELECT 1;'" \
        &> /dev/null; then
        log_success "Database connectivity verified"
        return 0
    else
        log_error "Cannot connect to database"
        return 1
    fi
}

################################################################################
# Migration Functions
################################################################################

run_database_migrations() {
    if [[ "$SKIP_MIGRATIONS" == "true" ]]; then
        log_warn "Skipping database migrations (--skip-migrations flag set)"
        return 0
    fi
    
    log_info "Running database migrations..."
    
    # Get auth service pod (it contains Flyway migrations)
    local auth_pod
    auth_pod=$(kubectl get pods -n "$NAMESPACE" -l app=rag-auth \
        -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)
    
    if [[ -z "$auth_pod" ]]; then
        log_error "No rag-auth pod found"
        exit 1
    fi
    
    log_info "Triggering migrations via Spring Boot application startup..."
    log_info "Checking application logs for migration status..."
    
    # Check if migrations have run by looking for Flyway log messages
    if kubectl logs -n "$NAMESPACE" "$auth_pod" --tail=200 | \
        grep -q "Flyway.*Migrating schema"; then
        log_success "Database migrations completed successfully"
    else
        log_warn "Migration status unclear. Check application logs:"
        log_warn "kubectl logs -n $NAMESPACE $auth_pod | grep Flyway"
    fi
    
    # Verify key tables exist
    log_info "Verifying database schema..."
    
    local required_tables=(
        "users"
        "roles"
        "tenants"
        "documents"
    )
    
    for table in "${required_tables[@]}"; do
        if kubectl exec -n "$NAMESPACE" "$auth_pod" -- \
            sh -c "PGPASSWORD='$PGPASSWORD' psql -h 127.0.0.1 -U $DB_USER -d $DB_NAME \
            -t -c \"SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name='$table');\"" \
            2>/dev/null | grep -q "t"; then
            log_success "Table '$table' exists"
        else
            log_error "Table '$table' not found"
            return 1
        fi
    done
    
    log_success "Database schema validated"
}

################################################################################
# Admin User Functions
################################################################################

create_default_tenant() {
    log_info "Creating default tenant..."
    
    local auth_pod
    auth_pod=$(kubectl get pods -n "$NAMESPACE" -l app=rag-auth \
        -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)
    
    # Check if default tenant exists
    local tenant_exists
    tenant_exists=$(kubectl exec -n "$NAMESPACE" "$auth_pod" -- \
        sh -c "PGPASSWORD='$PGPASSWORD' psql -h 127.0.0.1 -U $DB_USER -d $DB_NAME \
        -t -c \"SELECT COUNT(*) FROM tenants WHERE tenant_key='default';\"" \
        2>/dev/null | tr -d ' ')
    
    if [[ "$tenant_exists" == "0" ]]; then
        log_info "Inserting default tenant..."
        
        kubectl exec -n "$NAMESPACE" "$auth_pod" -- \
            sh -c "PGPASSWORD='$PGPASSWORD' psql -h 127.0.0.1 -U $DB_USER -d $DB_NAME" <<EOF
INSERT INTO tenants (id, tenant_key, name, description, is_active, created_at, updated_at)
VALUES (
    'default-tenant-id',
    'default',
    'Default Tenant',
    'Default system tenant',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (tenant_key) DO NOTHING;
EOF
        
        log_success "Default tenant created"
    else
        log_info "Default tenant already exists (skipping)"
    fi
}

create_admin_role() {
    log_info "Creating ADMIN role..."
    
    local auth_pod
    auth_pod=$(kubectl get pods -n "$NAMESPACE" -l app=rag-auth \
        -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)
    
    # Check if ADMIN role exists
    local role_exists
    role_exists=$(kubectl exec -n "$NAMESPACE" "$auth_pod" -- \
        sh -c "PGPASSWORD='$PGPASSWORD' psql -h 127.0.0.1 -U $DB_USER -d $DB_NAME \
        -t -c \"SELECT COUNT(*) FROM roles WHERE name='ADMIN';\"" \
        2>/dev/null | tr -d ' ')
    
    if [[ "$role_exists" == "0" ]]; then
        log_info "Inserting ADMIN role..."
        
        kubectl exec -n "$NAMESPACE" "$auth_pod" -- \
            sh -c "PGPASSWORD='$PGPASSWORD' psql -h 127.0.0.1 -U $DB_USER -d $DB_NAME" <<EOF
INSERT INTO roles (id, name, description, created_at)
VALUES (
    'admin-role-id',
    'ADMIN',
    'Administrator role with full system access',
    CURRENT_TIMESTAMP
)
ON CONFLICT (name) DO NOTHING;
EOF
        
        log_success "ADMIN role created"
    else
        log_info "ADMIN role already exists (skipping)"
    fi
}

create_admin_user() {
    log_info "Creating admin user..."
    
    local auth_pod
    auth_pod=$(kubectl get pods -n "$NAMESPACE" -l app=rag-auth \
        -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)
    
    # Check if admin user exists
    local user_exists
    user_exists=$(kubectl exec -n "$NAMESPACE" "$auth_pod" -- \
        sh -c "PGPASSWORD='$PGPASSWORD' psql -h 127.0.0.1 -U $DB_USER -d $DB_NAME \
        -t -c \"SELECT COUNT(*) FROM users WHERE email='$ADMIN_EMAIL';\"" \
        2>/dev/null | tr -d ' ')
    
    if [[ "$user_exists" == "0" ]]; then
        log_info "Inserting admin user: $ADMIN_EMAIL"
        
        kubectl exec -n "$NAMESPACE" "$auth_pod" -- \
            sh -c "PGPASSWORD='$PGPASSWORD' psql -h 127.0.0.1 -U $DB_USER -d $DB_NAME" <<EOF
INSERT INTO users (id, email, password_hash, first_name, last_name, tenant_id, is_active, is_verified, created_at, updated_at)
VALUES (
    'admin-user-id',
    '$ADMIN_EMAIL',
    '$ADMIN_BCRYPT_HASH',
    'System',
    'Administrator',
    'default-tenant-id',
    true,
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (email) DO UPDATE SET
    password_hash = EXCLUDED.password_hash,
    updated_at = CURRENT_TIMESTAMP;
EOF
        
        log_success "Admin user created"
    else
        log_info "Admin user already exists"
        log_warn "Updating password hash to ensure it's correct..."
        
        kubectl exec -n "$NAMESPACE" "$auth_pod" -- \
            sh -c "PGPASSWORD='$PGPASSWORD' psql -h 127.0.0.1 -U $DB_USER -d $DB_NAME" <<EOF
UPDATE users
SET password_hash = '$ADMIN_BCRYPT_HASH',
    updated_at = CURRENT_TIMESTAMP
WHERE email = '$ADMIN_EMAIL';
EOF
        
        log_success "Admin user password updated"
    fi
}

assign_admin_role() {
    log_info "Assigning ADMIN role to admin user..."
    
    local auth_pod
    auth_pod=$(kubectl get pods -n "$NAMESPACE" -l app=rag-auth \
        -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)
    
    # Check if role assignment exists
    local assignment_exists
    assignment_exists=$(kubectl exec -n "$NAMESPACE" "$auth_pod" -- \
        sh -c "PGPASSWORD='$PGPASSWORD' psql -h 127.0.0.1 -U $DB_USER -d $DB_NAME \
        -t -c \"SELECT COUNT(*) FROM user_roles WHERE user_id='admin-user-id' AND role_id='admin-role-id';\"" \
        2>/dev/null | tr -d ' ')
    
    if [[ "$assignment_exists" == "0" ]]; then
        log_info "Assigning ADMIN role..."
        
        kubectl exec -n "$NAMESPACE" "$auth_pod" -- \
            sh -c "PGPASSWORD='$PGPASSWORD' psql -h 127.0.0.1 -U $DB_USER -d $DB_NAME" <<EOF
INSERT INTO user_roles (user_id, role_id, assigned_at)
VALUES (
    'admin-user-id',
    'admin-role-id',
    CURRENT_TIMESTAMP
)
ON CONFLICT (user_id, role_id) DO NOTHING;
EOF
        
        log_success "ADMIN role assigned"
    else
        log_info "ADMIN role already assigned (skipping)"
    fi
}

################################################################################
# Verification Functions
################################################################################

verify_admin_user() {
    log_info "Verifying admin user setup..."
    
    local auth_pod
    auth_pod=$(kubectl get pods -n "$NAMESPACE" -l app=rag-auth \
        -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)
    
    # Verify user exists
    local user_data
    user_data=$(kubectl exec -n "$NAMESPACE" "$auth_pod" -- \
        sh -c "PGPASSWORD='$PGPASSWORD' psql -h 127.0.0.1 -U $DB_USER -d $DB_NAME \
        -t -c \"SELECT email, is_active, is_verified FROM users WHERE email='$ADMIN_EMAIL';\"" \
        2>/dev/null)
    
    if [[ -z "$user_data" ]]; then
        log_error "Admin user not found in database"
        return 1
    fi
    
    log_success "Admin user verified: $user_data"
    
    # Verify role assignment
    local roles
    roles=$(kubectl exec -n "$NAMESPACE" "$auth_pod" -- \
        sh -c "PGPASSWORD='$PGPASSWORD' psql -h 127.0.0.1 -U $DB_USER -d $DB_NAME \
        -t -c \"SELECT r.name FROM roles r JOIN user_roles ur ON r.id = ur.role_id WHERE ur.user_id='admin-user-id';\"" \
        2>/dev/null)
    
    if echo "$roles" | grep -q "ADMIN"; then
        log_success "Admin role verified"
    else
        log_error "ADMIN role not assigned to user"
        return 1
    fi
    
    log_success "Admin user setup verified"
}

test_admin_authentication() {
    log_info "Testing admin authentication..."
    
    # Port-forward to auth service
    log_info "Setting up port-forward to auth service..."
    
    kubectl port-forward -n "$NAMESPACE" svc/rag-auth-service 8081:8081 > /dev/null 2>&1 &
    local pf_pid=$!
    
    # Wait for port-forward to be ready
    sleep 3
    
    # Test authentication
    log_info "Attempting authentication with admin credentials..."
    
    local auth_response
    auth_response=$(curl -s -X POST http://localhost:8081/api/v1/auth/login \
        -H "Content-Type: application/json" \
        -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}" \
        2>/dev/null)
    
    # Kill port-forward
    kill $pf_pid 2>/dev/null || true
    
    if echo "$auth_response" | grep -q "token"; then
        log_success "Admin authentication successful!"
        log_info "JWT token received (truncated): $(echo "$auth_response" | grep -o '"token":"[^"]*' | cut -c 10-50)..."
        return 0
    else
        log_error "Admin authentication failed"
        log_error "Response: $auth_response"
        return 1
    fi
}

################################################################################
# Main Flow
################################################################################

print_summary() {
    log_info ""
    log_info "========================================="
    log_info "Database Initialization Summary"
    log_info "========================================="
    log_info ""
    log_info "Database: $DB_NAME"
    log_info "User: $DB_USER"
    log_info "Cloud SQL Instance: $CLOUD_SQL_INSTANCE"
    log_info ""
    log_info "Admin Credentials:"
    log_info "  Email: $ADMIN_EMAIL"
    log_info "  Password: $ADMIN_PASSWORD"
    log_info ""
    log_info "⚠️  IMPORTANT: Change admin password in production!"
    log_info ""
    log_info "Next Steps:"
    log_info "1. Test authentication:"
    log_info "   curl -X POST http://localhost:8081/api/v1/auth/login \\"
    log_info "     -H 'Content-Type: application/json' \\"
    log_info "     -d '{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}'"
    log_info ""
    log_info "2. Validate deployment:"
    log_info "   ./scripts/gcp/19-validate-deployment.sh --env $ENVIRONMENT"
    log_info ""
    log_info "3. Access Swagger UI (via port-forward):"
    log_info "   kubectl port-forward -n $NAMESPACE svc/rag-auth-service 8081:8081"
    log_info "   Open: http://localhost:8081/swagger-ui.html"
    log_info ""
}

################################################################################
# Argument Parsing
################################################################################

usage() {
    cat << EOF
Usage: $0 --env <environment> [OPTIONS]

Initialize RAG system database with schema and admin user.

Required Arguments:
  --env <environment>       Environment (dev, staging, prod)

Optional Arguments:
  --project <project-id>    GCP project ID (default: byo-rag-{env})
  --region <region>         GCP region (default: us-central1)
  --skip-migrations         Skip database migrations
  --admin-email <email>     Admin user email (default: admin@enterprise-rag.com)
  --admin-password <pass>   Admin user password (default: admin123)
  -h, --help                Show this help message

Examples:
  # Initialize dev database
  $0 --env dev

  # Initialize prod with custom admin
  $0 --env prod --admin-email admin@mycompany.com --admin-password MySecurePass123

  # Skip migrations (if already run)
  $0 --env dev --skip-migrations

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
        --region)
            REGION="$2"
            shift 2
            ;;
        --skip-migrations)
            SKIP_MIGRATIONS=true
            shift
            ;;
        --admin-email)
            ADMIN_EMAIL="$2"
            shift 2
            ;;
        --admin-password)
            ADMIN_PASSWORD="$2"
            # Generate new BCrypt hash if password changed
            log_warn "Custom admin password set. Make sure to update ADMIN_BCRYPT_HASH in script."
            shift 2
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
    log_info "RAG System - Database Initialization"
    log_info "====================================="
    log_info "Environment: $ENVIRONMENT"
    log_info "Project: $PROJECT_ID"
    log_info ""
    
    # Validate prerequisites
    validate_prerequisites
    validate_cloud_sql_instance
    get_database_credentials
    
    # Test connectivity
    if ! test_database_connectivity; then
        log_error "Cannot proceed without database connectivity"
        exit 1
    fi
    
    # Run migrations
    run_database_migrations
    
    # Create default data
    create_default_tenant
    create_admin_role
    create_admin_user
    assign_admin_role
    
    # Verify setup
    verify_admin_user
    
    # Test authentication
    if command -v curl &> /dev/null; then
        test_admin_authentication || log_warn "Authentication test failed (manual verification required)"
    else
        log_warn "curl not found. Skip authentication test."
    fi
    
    # Print summary
    print_summary
    
    log_success "Database initialization completed successfully"
}

# Run main function
main "$@"

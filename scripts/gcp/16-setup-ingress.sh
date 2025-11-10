#!/usr/bin/env bash

################################################################################
# GCP-INGRESS-010: Ingress and Load Balancer Configuration
# 
# This script configures:
# - Static external IP address for ingress
# - Cloud DNS zone and records
# - Cloud Armor security policy
# - Ingress controller verification
#
# Usage:
#   ./16-setup-ingress.sh --env dev|prod --domain DOMAIN [OPTIONS]
#
# Prerequisites:
#   - GKE cluster running (GCP-GKE-007)
#   - NGINX ingress controller installed
#   - cert-manager installed
#   - kubectl configured
################################################################################

set -euo pipefail

# Color codes
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m'

# Default values
ENVIRONMENT=""
DOMAIN=""
PROJECT_ID=""
REGION="us-central1"
STATIC_IP_NAME="rag-ingress-ip"
DNS_ZONE_NAME="rag-zone"
SKIP_DNS=false
SKIP_ARMOR=false

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
    
    # Check gcloud
    if ! command -v gcloud &> /dev/null; then
        log_error "gcloud CLI not found"
        exit 1
    fi
    log_success "gcloud CLI found"
    
    # Check kubectl
    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl not found"
        exit 1
    fi
    log_success "kubectl found"
    
    # Check cluster access
    if ! kubectl cluster-info &> /dev/null; then
        log_error "Cannot access Kubernetes cluster. Configure kubectl first."
        exit 1
    fi
    log_success "kubectl configured"
    
    # Check NGINX ingress controller
    if ! kubectl get deployment -n ingress-nginx ingress-nginx-controller &> /dev/null; then
        log_error "NGINX ingress controller not found. Run GCP-GKE-007 setup first."
        exit 1
    fi
    log_success "NGINX ingress controller found"
    
    # Check cert-manager
    if ! kubectl get deployment -n cert-manager cert-manager &> /dev/null; then
        log_error "cert-manager not found. Run GCP-GKE-007 setup first."
        exit 1
    fi
    log_success "cert-manager found"
}

validate_domain() {
    print_header "Validating Domain: $DOMAIN"
    
    # Basic domain format check
    if [[ ! "$DOMAIN" =~ ^[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9]\.[a-zA-Z]{2,}$ ]]; then
        log_error "Invalid domain format: $DOMAIN"
        exit 1
    fi
    
    log_success "Domain format valid"
}

################################################################################
# Static IP Configuration
################################################################################

reserve_static_ip() {
    print_header "Reserving Static External IP"
    
    log_info "Static IP name: $STATIC_IP_NAME"
    
    # Check if IP already exists
    if gcloud compute addresses describe "$STATIC_IP_NAME" --global --project="$PROJECT_ID" &> /dev/null; then
        local existing_ip
        existing_ip=$(gcloud compute addresses describe "$STATIC_IP_NAME" --global --project="$PROJECT_ID" --format="value(address)")
        log_warning "Static IP $STATIC_IP_NAME already exists: $existing_ip"
        STATIC_IP="$existing_ip"
        return 0
    fi
    
    # Reserve new static IP
    log_info "Reserving new static IP address..."
    if ! gcloud compute addresses create "$STATIC_IP_NAME" \
        --global \
        --ip-version IPV4 \
        --project="$PROJECT_ID"; then
        log_error "Failed to reserve static IP"
        exit 1
    fi
    
    # Get the IP address
    STATIC_IP=$(gcloud compute addresses describe "$STATIC_IP_NAME" --global --project="$PROJECT_ID" --format="value(address)")
    
    log_success "Static IP reserved: $STATIC_IP"
}

################################################################################
# Cloud DNS Configuration
################################################################################

setup_cloud_dns() {
    if [[ "$SKIP_DNS" == "true" ]]; then
        log_warning "Skipping Cloud DNS setup (--skip-dns)"
        return 0
    fi
    
    print_header "Setting Up Cloud DNS"
    
    # Create DNS zone
    log_info "Creating DNS zone: $DNS_ZONE_NAME"
    
    if gcloud dns managed-zones describe "$DNS_ZONE_NAME" --project="$PROJECT_ID" &> /dev/null; then
        log_warning "DNS zone $DNS_ZONE_NAME already exists"
    else
        if ! gcloud dns managed-zones create "$DNS_ZONE_NAME" \
            --dns-name="$DOMAIN." \
            --description="RAG system DNS zone for $ENVIRONMENT" \
            --project="$PROJECT_ID"; then
            log_error "Failed to create DNS zone"
            exit 1
        fi
        log_success "DNS zone created"
    fi
    
    # Get name servers
    log_info "DNS zone name servers:"
    gcloud dns managed-zones describe "$DNS_ZONE_NAME" --project="$PROJECT_ID" --format="value(nameServers)"
    
    # Create DNS records
    log_info "Creating DNS A records..."
    
    # Start transaction
    if ! gcloud dns record-sets transaction start --zone="$DNS_ZONE_NAME" --project="$PROJECT_ID" 2>/dev/null; then
        log_warning "Transaction already in progress, aborting and starting new one"
        gcloud dns record-sets transaction abort --zone="$DNS_ZONE_NAME" --project="$PROJECT_ID" 2>/dev/null || true
        gcloud dns record-sets transaction start --zone="$DNS_ZONE_NAME" --project="$PROJECT_ID"
    fi
    
    # Add A records
    for subdomain in "" "api" "admin"; do
        local fqdn
        if [[ -z "$subdomain" ]]; then
            fqdn="$DOMAIN."
        else
            fqdn="$subdomain.$DOMAIN."
        fi
        
        # Check if record exists
        if gcloud dns record-sets list --zone="$DNS_ZONE_NAME" --project="$PROJECT_ID" --name="$fqdn" --type=A &> /dev/null; then
            log_warning "Record $fqdn already exists, skipping"
        else
            log_info "Adding A record: $fqdn -> $STATIC_IP"
            gcloud dns record-sets transaction add "$STATIC_IP" \
                --name="$fqdn" \
                --ttl=300 \
                --type=A \
                --zone="$DNS_ZONE_NAME" \
                --project="$PROJECT_ID" 2>/dev/null || true
        fi
    done
    
    # Execute transaction
    if gcloud dns record-sets transaction execute --zone="$DNS_ZONE_NAME" --project="$PROJECT_ID" 2>/dev/null; then
        log_success "DNS records created"
    else
        log_warning "Some DNS records may already exist"
        gcloud dns record-sets transaction abort --zone="$DNS_ZONE_NAME" --project="$PROJECT_ID" 2>/dev/null || true
    fi
}

################################################################################
# Cloud Armor Security Policy
################################################################################

setup_cloud_armor() {
    if [[ "$SKIP_ARMOR" == "true" ]]; then
        log_warning "Skipping Cloud Armor setup (--skip-armor)"
        return 0
    fi
    
    print_header "Setting Up Cloud Armor Security Policy"
    
    local policy_name="rag-security-policy"
    
    # Check if policy exists
    if gcloud compute security-policies describe "$policy_name" --project="$PROJECT_ID" &> /dev/null; then
        log_warning "Security policy $policy_name already exists"
        return 0
    fi
    
    # Create security policy
    log_info "Creating Cloud Armor security policy: $policy_name"
    if ! gcloud compute security-policies create "$policy_name" \
        --description="RAG system security policy for $ENVIRONMENT" \
        --project="$PROJECT_ID"; then
        log_error "Failed to create security policy"
        exit 1
    fi
    
    # Add rules
    
    # Rule 1: Block SQL injection attempts
    log_info "Adding rule: Block SQL injection"
    gcloud compute security-policies rules create 1000 \
        --security-policy="$policy_name" \
        --expression="evaluatePreconfiguredExpr('sqli-stable')" \
        --action=deny-403 \
        --description="Block SQL injection attempts" \
        --project="$PROJECT_ID"
    
    # Rule 2: Block XSS attempts
    log_info "Adding rule: Block XSS"
    gcloud compute security-policies rules create 1001 \
        --security-policy="$policy_name" \
        --expression="evaluatePreconfiguredExpr('xss-stable')" \
        --action=deny-403 \
        --description="Block XSS attempts" \
        --project="$PROJECT_ID"
    
    # Rule 3: Block RCE attempts
    log_info "Adding rule: Block RCE"
    gcloud compute security-policies rules create 1002 \
        --security-policy="$policy_name" \
        --expression="evaluatePreconfiguredExpr('rce-stable')" \
        --action=deny-403 \
        --description="Block remote code execution attempts" \
        --project="$PROJECT_ID"
    
    # Rule 4: Rate limiting (10000 requests per minute per IP)
    log_info "Adding rule: Rate limiting"
    gcloud compute security-policies rules create 2000 \
        --security-policy="$policy_name" \
        --expression="true" \
        --action=rate-based-ban \
        --rate-limit-threshold-count=10000 \
        --rate-limit-threshold-interval-sec=60 \
        --ban-duration-sec=600 \
        --conform-action=allow \
        --exceed-action=deny-429 \
        --description="Rate limit: 10000 req/min per IP" \
        --project="$PROJECT_ID"
    
    # Rule 5: Allow all (default rule with lowest priority)
    log_info "Adding rule: Allow all (default)"
    gcloud compute security-policies rules create 2147483647 \
        --security-policy="$policy_name" \
        --action=allow \
        --description="Default allow rule" \
        --project="$PROJECT_ID"
    
    log_success "Cloud Armor security policy created"
}

################################################################################
# Update Ingress Configuration
################################################################################

update_ingress_config() {
    print_header "Updating Ingress Configuration"
    
    log_info "Domain: $DOMAIN"
    log_info "Static IP: $STATIC_IP"
    
    local ingress_file="k8s/base/ingress.yaml"
    
    if [[ ! -f "$ingress_file" ]]; then
        log_error "Ingress file not found: $ingress_file"
        exit 1
    fi
    
    # Update domain placeholders
    log_info "Updating domain references in $ingress_file"
    sed -i.bak "s/rag\.example\.com/$DOMAIN/g" "$ingress_file"
    sed -i.bak "s/api\.rag\.example\.com/api.$DOMAIN/g" "$ingress_file"
    sed -i.bak "s/admin\.rag\.example\.com/admin.$DOMAIN/g" "$ingress_file"
    sed -i.bak "s/ops@example\.com/ops@$DOMAIN/g" "$ingress_file"
    
    # Uncomment static IP annotation
    log_info "Enabling static IP annotation"
    sed -i.bak "s/# kubernetes.io\/ingress.global-static-ip-name/kubernetes.io\/ingress.global-static-ip-name/g" "$ingress_file"
    
    rm -f "${ingress_file}.bak"
    
    log_success "Ingress configuration updated"
}

################################################################################
# Summary and Next Steps
################################################################################

print_summary() {
    print_header "Ingress Setup Complete"
    
    cat <<EOF
${GREEN}✓${NC} Ingress and load balancer configuration complete

${BLUE}Configuration Summary:${NC}
┌─────────────────────────────────────────────────────────────────────┐
│ ${YELLOW}Network Configuration${NC}                                             │
│   Static IP: $STATIC_IP
│   IP Name: $STATIC_IP_NAME
│   Region: Global                                                     │
├─────────────────────────────────────────────────────────────────────┤
│ ${YELLOW}DNS Configuration${NC}                                                 │
│   Domain: $DOMAIN
│   Zone: $DNS_ZONE_NAME
│   Records:                                                           │
│     - $DOMAIN -> $STATIC_IP
│     - api.$DOMAIN -> $STATIC_IP
│     - admin.$DOMAIN -> $STATIC_IP
│                                                                       │
│   ${YELLOW}⚠ Configure your domain registrar${NC}                                │
│   Point your domain's nameservers to Cloud DNS:                     │
EOF
    
    if [[ "$SKIP_DNS" == "false" ]]; then
        gcloud dns managed-zones describe "$DNS_ZONE_NAME" --project="$PROJECT_ID" --format="value(nameServers)" | while read -r ns; do
            echo "│     - $ns"
        done
    fi
    
    cat <<EOF
├─────────────────────────────────────────────────────────────────────┤
│ ${YELLOW}Security Configuration${NC}                                            │
│   Cloud Armor Policy: rag-security-policy                           │
│   WAF Rules:                                                         │
│     - SQL injection protection                                      │
│     - XSS protection                                                │
│     - RCE protection                                                │
│   Rate Limiting: 10,000 req/min per IP                              │
│   Ban Duration: 10 minutes                                          │
├─────────────────────────────────────────────────────────────────────┤
│ ${YELLOW}SSL/TLS Configuration${NC}                                             │
│   Certificate Manager: cert-manager + Let's Encrypt                 │
│   Issuer: letsencrypt-prod                                          │
│   Challenge Type: HTTP-01                                           │
│   Auto-renewal: Enabled                                             │
└─────────────────────────────────────────────────────────────────────┘

${BLUE}Cost Estimate:${NC}
- Static IP: \$0.01/hour (~\$7/month if unused, free when in use)
- Cloud DNS Zone: \$0.20/month + \$0.40/million queries
- Cloud Armor: \$5/policy/month + \$0.75/million requests
- Load Balancer: \$18/month + traffic costs
- ${YELLOW}Total: ~\$25-50/month${NC}

${BLUE}Next Steps:${NC}

1. ${YELLOW}Update domain nameservers${NC} (if using external registrar):
   Log into your domain registrar and update nameservers to Cloud DNS

2. ${YELLOW}Deploy ingress configuration${NC}:
   kubectl apply -f k8s/base/backendconfig.yaml
   kubectl apply -f k8s/base/ingress.yaml

3. ${YELLOW}Verify NGINX ingress controller${NC}:
   kubectl get pods -n ingress-nginx
   kubectl get svc -n ingress-nginx

4. ${YELLOW}Wait for certificate provisioning${NC} (5-10 minutes):
   kubectl get certificate -n rag-system
   kubectl describe certificate rag-tls-cert -n rag-system

5. ${YELLOW}Test DNS resolution${NC}:
   nslookup $DOMAIN
   nslookup api.$DOMAIN
   nslookup admin.$DOMAIN

6. ${YELLOW}Test HTTPS access${NC} (after certificate is ready):
   curl -I https://$DOMAIN
   curl -I https://api.$DOMAIN/api/v1/auth/health
   curl -I https://admin.$DOMAIN/admin/api/actuator/health

7. ${YELLOW}Monitor certificate status${NC}:
   kubectl logs -n cert-manager deployment/cert-manager -f

8. ${YELLOW}View Cloud Armor logs${NC}:
   gcloud logging read "resource.type=http_load_balancer"

${BLUE}Troubleshooting:${NC}

# Check ingress status
kubectl get ingress -n rag-system
kubectl describe ingress rag-ingress -n rag-system

# Check certificate challenges
kubectl get challenges -n rag-system

# Check ingress controller logs
kubectl logs -n ingress-nginx deployment/ingress-nginx-controller

# Verify backend services
kubectl get backendconfig -n rag-system

${YELLOW}Documentation:${NC}
- See docs/deployment/INGRESS_LOAD_BALANCER_GUIDE.md for detailed configuration

EOF
}

################################################################################
# Usage and Argument Parsing
################################################################################

usage() {
    cat <<EOF
Usage: $0 --env <environment> --domain <domain> [OPTIONS]

Configures GCP ingress and load balancer for RAG system.

Required Arguments:
    --env ENV           Environment: dev or prod
    --domain DOMAIN     Primary domain (e.g., rag-dev.example.com)

Optional Arguments:
    --project ID        GCP project ID (default: current project)
    --region REGION     GCP region (default: us-central1)
    --skip-dns          Skip Cloud DNS configuration
    --skip-armor        Skip Cloud Armor configuration
    -h, --help          Show this help message

Examples:
    # Development environment
    $0 --env dev --domain rag-dev.example.com

    # Production with specific project
    $0 --env prod --domain rag.example.com --project byo-rag-prod

    # Skip DNS (using external DNS provider)
    $0 --env dev --domain rag-dev.example.com --skip-dns

EOF
}

parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --env)
                ENVIRONMENT="$2"
                shift 2
                ;;
            --domain)
                DOMAIN="$2"
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
            --skip-dns)
                SKIP_DNS=true
                shift
                ;;
            --skip-armor)
                SKIP_ARMOR=true
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
    
    if [[ -z "$DOMAIN" ]]; then
        log_error "Domain (--domain) is required"
        usage
        exit 1
    fi
    
    # Set project ID if not provided
    if [[ -z "$PROJECT_ID" ]]; then
        PROJECT_ID=$(gcloud config get-value project 2>/dev/null || echo "")
        if [[ -z "$PROJECT_ID" ]]; then
            log_error "No project ID specified and no default project configured"
            exit 1
        fi
    fi
    
    # Append environment to resource names
    STATIC_IP_NAME="rag-ingress-ip-${ENVIRONMENT}"
    DNS_ZONE_NAME="rag-zone-${ENVIRONMENT}"
}

################################################################################
# Main Execution
################################################################################

main() {
    parse_arguments "$@"
    
    print_header "GCP Ingress Setup - Environment: $ENVIRONMENT"
    
    log_info "Project: $PROJECT_ID"
    log_info "Region: $REGION"
    log_info "Domain: $DOMAIN"
    
    validate_prerequisites
    validate_domain
    reserve_static_ip
    setup_cloud_dns
    setup_cloud_armor
    update_ingress_config
    print_summary
    
    log_success "\n✓ Ingress setup complete!"
    log_info "Review the summary above for next steps"
}

# Run main function
main "$@"

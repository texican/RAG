#!/bin/bash

################################################################################
# GCP-INFRA-001: VPC and Networking Configuration
#
# This script configures VPC network, subnets, Cloud Router, and Cloud NAT
# for the BYO RAG System GKE cluster.
#
# Prerequisites:
#   - 00-setup-project.sh completed
#   - Project config file exists
#
# Usage:
#   ./01-setup-network.sh
#
# Story: GCP-INFRA-001 (Step 2 of 4)
################################################################################

set -euo pipefail

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
CONFIG_DIR="${SCRIPT_DIR}/config"
LOG_DIR="${PROJECT_ROOT}/logs/gcp-setup"

# Ensure log directory exists
mkdir -p "${LOG_DIR}"

# Logging configuration
LOG_FILE="${LOG_DIR}/network-setup-$(date +%Y%m%d-%H%M%S).log"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# Logging functions
log() {
    local level=$1
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')

    case $level in
        INFO)
            echo -e "${GREEN}[INFO]${NC} $message" | tee -a "$LOG_FILE"
            ;;
        WARN)
            echo -e "${YELLOW}[WARN]${NC} $message" | tee -a "$LOG_FILE"
            ;;
        ERROR)
            echo -e "${RED}[ERROR]${NC} $message" | tee -a "$LOG_FILE"
            ;;
        SUCCESS)
            echo -e "${GREEN}✓${NC} $message" | tee -a "$LOG_FILE"
            ;;
        STEP)
            echo -e "${CYAN}[STEP]${NC} $message" | tee -a "$LOG_FILE"
            ;;
    esac

    echo "[$timestamp] [$level] $message" >> "$LOG_FILE"
}

error_exit() {
    log ERROR "$1"
    exit 1
}

# Load configuration
load_config() {
    log STEP "Loading project configuration..."

    if [[ ! -f "${CONFIG_DIR}/project-config.env" ]]; then
        error_exit "Project configuration not found. Run ./00-setup-project.sh first."
    fi

    # shellcheck disable=SC1091
    source "${CONFIG_DIR}/project-config.env"

    log SUCCESS "Configuration loaded for project: ${PROJECT_ID}"

    # Set gcloud project
    gcloud config set project "${PROJECT_ID}"
}

# Create VPC network
create_vpc() {
    log STEP "Creating VPC network: ${VPC_NAME}..."

    # Check if VPC already exists
    if gcloud compute networks describe "${VPC_NAME}" &>/dev/null; then
        log WARN "VPC ${VPC_NAME} already exists"
        return 0
    fi

    # Create custom VPC
    if gcloud compute networks create "${VPC_NAME}" \
        --subnet-mode=custom \
        --bgp-routing-mode=regional \
        --project="${PROJECT_ID}"; then
        log SUCCESS "VPC network created: ${VPC_NAME}"
    else
        error_exit "Failed to create VPC network"
    fi
}

# Create subnet for GKE
create_subnet() {
    log STEP "Creating GKE subnet: ${SUBNET_NAME}..."

    # Check if subnet already exists
    if gcloud compute networks subnets describe "${SUBNET_NAME}" \
        --region="${REGION}" &>/dev/null; then
        log WARN "Subnet ${SUBNET_NAME} already exists"
        return 0
    fi

    # Create subnet with secondary ranges for GKE
    if gcloud compute networks subnets create "${SUBNET_NAME}" \
        --network="${VPC_NAME}" \
        --region="${REGION}" \
        --range="${SUBNET_RANGE}" \
        --secondary-range pods="${PODS_RANGE}" \
        --secondary-range services="${SERVICES_RANGE}" \
        --enable-private-ip-google-access \
        --project="${PROJECT_ID}"; then
        log SUCCESS "Subnet created: ${SUBNET_NAME}"
        log INFO "  Primary range: ${SUBNET_RANGE}"
        log INFO "  Pods range: ${PODS_RANGE}"
        log INFO "  Services range: ${SERVICES_RANGE}"
    else
        error_exit "Failed to create subnet"
    fi
}

# Create firewall rules
create_firewall_rules() {
    log STEP "Creating firewall rules..."

    # Allow internal communication
    if ! gcloud compute firewall-rules describe rag-allow-internal &>/dev/null; then
        gcloud compute firewall-rules create rag-allow-internal \
            --network="${VPC_NAME}" \
            --allow=tcp,udp,icmp \
            --source-ranges="${SUBNET_RANGE},${PODS_RANGE},${SERVICES_RANGE}" \
            --project="${PROJECT_ID}"
        log SUCCESS "Firewall rule created: rag-allow-internal"
    else
        log WARN "Firewall rule rag-allow-internal already exists"
    fi

    # Allow SSH from IAP (Identity-Aware Proxy)
    if ! gcloud compute firewall-rules describe rag-allow-ssh-iap &>/dev/null; then
        gcloud compute firewall-rules create rag-allow-ssh-iap \
            --network="${VPC_NAME}" \
            --allow=tcp:22 \
            --source-ranges=35.235.240.0/20 \
            --project="${PROJECT_ID}"
        log SUCCESS "Firewall rule created: rag-allow-ssh-iap"
    else
        log WARN "Firewall rule rag-allow-ssh-iap already exists"
    fi

    # Allow health checks from Google load balancers
    if ! gcloud compute firewall-rules describe rag-allow-health-checks &>/dev/null; then
        gcloud compute firewall-rules create rag-allow-health-checks \
            --network="${VPC_NAME}" \
            --allow=tcp \
            --source-ranges=130.211.0.0/22,35.191.0.0/16 \
            --project="${PROJECT_ID}"
        log SUCCESS "Firewall rule created: rag-allow-health-checks"
    else
        log WARN "Firewall rule rag-allow-health-checks already exists"
    fi
}

# Create Cloud Router
create_router() {
    log STEP "Creating Cloud Router: rag-router..."

    # Check if router already exists
    if gcloud compute routers describe rag-router \
        --region="${REGION}" &>/dev/null; then
        log WARN "Cloud Router rag-router already exists"
        return 0
    fi

    # Create router
    if gcloud compute routers create rag-router \
        --network="${VPC_NAME}" \
        --region="${REGION}" \
        --project="${PROJECT_ID}"; then
        log SUCCESS "Cloud Router created: rag-router"
    else
        error_exit "Failed to create Cloud Router"
    fi
}

# Create Cloud NAT
create_nat() {
    log STEP "Creating Cloud NAT: rag-nat..."

    # Check if NAT already exists
    if gcloud compute routers nats describe rag-nat \
        --router=rag-router \
        --region="${REGION}" &>/dev/null; then
        log WARN "Cloud NAT rag-nat already exists"
        return 0
    fi

    # Create NAT gateway
    if gcloud compute routers nats create rag-nat \
        --router=rag-router \
        --region="${REGION}" \
        --auto-allocate-nat-external-ips \
        --nat-all-subnet-ip-ranges \
        --enable-logging \
        --project="${PROJECT_ID}"; then
        log SUCCESS "Cloud NAT created: rag-nat"
        log INFO "NAT provides internet access for private GKE nodes"
    else
        error_exit "Failed to create Cloud NAT"
    fi
}

# Configure Private Service Access (for Cloud SQL)
configure_private_service_access() {
    log STEP "Configuring Private Service Access for Cloud SQL..."

    # Allocate IP range for private services
    if ! gcloud compute addresses describe google-managed-services-${VPC_NAME} \
        --global &>/dev/null; then
        gcloud compute addresses create google-managed-services-${VPC_NAME} \
            --global \
            --purpose=VPC_PEERING \
            --prefix-length=16 \
            --network="${VPC_NAME}" \
            --project="${PROJECT_ID}"
        log SUCCESS "IP range allocated for private services"
    else
        log WARN "Private service IP range already exists"
    fi

    # Create private connection
    if gcloud services vpc-peerings list \
        --network="${VPC_NAME}" \
        --project="${PROJECT_ID}" 2>/dev/null | grep -q "servicenetworking"; then
        log WARN "Private service connection already exists"
    else
        gcloud services vpc-peerings connect \
            --service=servicenetworking.googleapis.com \
            --ranges=google-managed-services-${VPC_NAME} \
            --network="${VPC_NAME}" \
            --project="${PROJECT_ID}"
        log SUCCESS "Private service connection created"
    fi
}

# Verify network configuration
verify_network() {
    log STEP "Verifying network configuration..."

    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "  Network Configuration Verification"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""

    # VPC
    echo "VPC Network:"
    gcloud compute networks describe "${VPC_NAME}" \
        --format="table(name,autoCreateSubnetworks,routingConfig.routingMode)"

    echo ""
    echo "Subnets:"
    gcloud compute networks subnets list \
        --network="${VPC_NAME}" \
        --format="table(name,region,ipCidrRange,secondaryIpRanges[].rangeName,secondaryIpRanges[].ipCidrRange)"

    echo ""
    echo "Firewall Rules:"
    gcloud compute firewall-rules list \
        --filter="network:${VPC_NAME}" \
        --format="table(name,direction,priority,sourceRanges.list():label=SRC_RANGES,allowed[].map().firewall_rule().list():label=ALLOW)"

    echo ""
    echo "Cloud Router:"
    gcloud compute routers describe rag-router \
        --region="${REGION}" \
        --format="table(name,region,network)"

    echo ""
    echo "Cloud NAT:"
    gcloud compute routers nats describe rag-nat \
        --router=rag-router \
        --region="${REGION}" \
        --format="table(name,natIpAllocateOption,sourceSubnetworkIpRangesToNat)"

    echo ""
    log SUCCESS "Network verification complete"
}

# Generate network summary
generate_summary() {
    log STEP "Generating network setup summary..."

    SUMMARY_FILE="${LOG_DIR}/network-summary-$(date +%Y%m%d-%H%M%S).txt"

    cat > "$SUMMARY_FILE" <<EOF
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  BYO RAG System - Network Configuration Summary
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Setup Date: $(date)
Project ID: ${PROJECT_ID}

VPC CONFIGURATION
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
VPC Name:            ${VPC_NAME}
Routing Mode:        Regional
Subnet Mode:         Custom

SUBNET CONFIGURATION
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Subnet Name:         ${SUBNET_NAME}
Region:              ${REGION}
Primary Range:       ${SUBNET_RANGE}
Pods Range:          ${PODS_RANGE}
Services Range:      ${SERVICES_RANGE}

NETWORK SERVICES
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Cloud Router:        rag-router
Cloud NAT:           rag-nat (enabled)
Private Access:      Enabled
Private Services:    Configured (for Cloud SQL)

FIREWALL RULES
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✓ rag-allow-internal       (internal communication)
✓ rag-allow-ssh-iap        (SSH via IAP)
✓ rag-allow-health-checks  (load balancer health checks)

NEXT STEPS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1. Run ./02-setup-service-accounts.sh to create IAM service accounts
2. Run ./03-setup-budget-alerts.sh to configure budget monitoring

USEFUL COMMANDS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# View VPC details
gcloud compute networks describe ${VPC_NAME}

# View subnet details
gcloud compute networks subnets describe ${SUBNET_NAME} --region=${REGION}

# View firewall rules
gcloud compute firewall-rules list --filter="network:${VPC_NAME}"

# View Cloud Router
gcloud compute routers describe rag-router --region=${REGION}

# View Cloud NAT
gcloud compute routers nats describe rag-nat --router=rag-router --region=${REGION}

# View network in console
https://console.cloud.google.com/networking/networks/details/${VPC_NAME}?project=${PROJECT_ID}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
EOF

    cat "$SUMMARY_FILE"
    log SUCCESS "Summary saved to: ${SUMMARY_FILE}"
}

# Main execution
main() {
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "  BYO RAG System - Network Configuration"
    echo "  Story: GCP-INFRA-001 (Step 2 of 4)"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""

    log INFO "Starting network configuration..."
    log INFO "Log file: ${LOG_FILE}"
    echo ""

    # Execute setup steps
    load_config
    create_vpc
    create_subnet
    create_firewall_rules
    create_router
    create_nat
    configure_private_service_access
    verify_network
    generate_summary

    echo ""
    log SUCCESS "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    log SUCCESS "  Network Configuration Complete!"
    log SUCCESS "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    log INFO "Next step: Run ./02-setup-service-accounts.sh"
    echo ""
}

# Run main function
main "$@"

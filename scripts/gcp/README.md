# GCP Deployment Scripts

Automated scripts for deploying the BYO RAG System to Google Cloud Platform.

## Overview

This directory contains automation scripts for GCP infrastructure setup, following the implementation plan in [GCP_DEPLOYMENT_GUIDE.md](../../docs/deployment/GCP_DEPLOYMENT_GUIDE.md).

## Prerequisites

1. **Install gcloud CLI**
   ```bash
   curl https://sdk.cloud.google.com | bash
   exec -l $SHELL
   gcloud init
   ```

2. **Authenticate**
   ```bash
   gcloud auth login
   gcloud auth application-default login
   ```

3. **Required Permissions**
   - Project Creator role (or use existing project)
   - Billing Account Admin or User
   - Organization Admin (if creating under organization)

## Quick Start

### GCP-INFRA-001: Foundation Setup (Story Points: 8)

Run scripts in order:

```bash
# Step 1: Create GCP project and enable APIs (15-20 minutes)
./00-setup-project.sh

# Step 2: Configure VPC and networking (10-15 minutes)
./01-setup-network.sh

# Step 3: Create service accounts (5-10 minutes)
./02-setup-service-accounts.sh

# Step 4: Set up budget alerts (5-10 minutes)
./03-setup-budget-alerts.sh
```

**Total Time: ~45-55 minutes**

## Script Details

### 00-setup-project.sh

**Purpose:** Creates GCP project, enables required APIs, and configures billing.

**What it does:**
- Creates GCP project with unique ID
- Links billing account
- Enables 15+ required GCP APIs (GKE, Cloud SQL, etc.)
- Generates project configuration file
- Sets default project for gcloud

**Outputs:**
- `config/project-config.env` - Project configuration
- `logs/gcp-setup/project-setup-*.log` - Detailed logs
- `logs/gcp-setup/setup-summary-*.txt` - Setup summary

**Interactive Prompts:**
- Project ID (must be globally unique)
- Project Name
- Region (default: us-central1)
- Zone (default: us-central1-a)
- Billing Account ID
- Monthly budget (default: $1000)
- Environment (dev/staging/production)

**Example:**
```bash
$ ./00-setup-project.sh

Enter GCP Project ID: byo-rag-dev
Enter Project Name: BYO RAG System - Dev
Enter region [us-central1]:
Enter zone [us-central1-a]:
Enter Billing Account ID: 01234-ABCDEF-567890
Enter monthly budget limit in USD [1000]: 500
Select environment [dev]: 1
```

---

### 01-setup-network.sh

**Purpose:** Configures VPC network, subnets, Cloud Router, and Cloud NAT.

**What it does:**
- Creates custom VPC network
- Creates subnet with secondary ranges for GKE (pods, services)
- Configures firewall rules (internal, SSH, health checks)
- Sets up Cloud Router
- Configures Cloud NAT for private cluster egress
- Enables Private Service Access for Cloud SQL

**Network Configuration:**
- VPC Name: `rag-vpc`
- Subnet: `rag-gke-subnet`
- Primary Range: `10.0.0.0/20` (4,096 IPs)
- Pods Range: `10.4.0.0/14` (262,144 IPs)
- Services Range: `10.8.0.0/20` (4,096 IPs)

**Firewall Rules:**
- `rag-allow-internal` - Internal communication between pods
- `rag-allow-ssh-iap` - SSH access via Identity-Aware Proxy
- `rag-allow-health-checks` - Google load balancer health checks

**Example:**
```bash
$ ./01-setup-network.sh
# Reads configuration from config/project-config.env
# No user input required
```

---

### 02-setup-service-accounts.sh

**Purpose:** Creates IAM service accounts with least-privilege access.

**What it does:**
- Creates GKE node service account
- Creates Cloud SQL Proxy service account
- Creates Cloud Build service account
- Grants necessary IAM roles
- Prepares Workload Identity configuration
- Exports service account keys for local development

**Service Accounts Created:**

1. **GKE Node SA** (`gke-node-sa`)
   - Roles: logging.logWriter, monitoring.metricWriter, artifactregistry.reader
   - Purpose: GKE node pools

2. **Cloud SQL Proxy SA** (`cloudsql-proxy-sa`)
   - Roles: cloudsql.client
   - Purpose: Cloud SQL Proxy sidecar containers

3. **Cloud Build SA** (`cloud-build-sa`)
   - Roles: cloudbuild.builds.builder, artifactregistry.writer, container.developer
   - Purpose: CI/CD automation

**Security:**
- Service account keys exported to `config/service-account-keys/`
- Directory automatically added to `.gitignore`
- Keys only for local development (production uses Workload Identity)

**Example:**
```bash
$ ./02-setup-service-accounts.sh
# Creates all service accounts
# Exports keys to config/service-account-keys/
```

---

### 03-setup-budget-alerts.sh

**Purpose:** Configures budget monitoring and cost alerts.

**What it does:**
- Creates monthly budget with threshold alerts
- Configures notification channels (email)
- Provides cost monitoring setup instructions
- Generates cost estimates for environment
- Completes GCP-INFRA-001 story

**Budget Thresholds:**
- 50% - Early warning
- 75% - Review usage
- 90% - Take action
- 100% - Budget limit reached

**Cost Estimates:**

**Development:**
- GKE: ~$220/month
- Cloud SQL: ~$120/month
- Memorystore: ~$50/month
- Other: ~$40/month
- **Total: ~$430/month**

**Production:**
- GKE: ~$900/month
- Cloud SQL: ~$480/month
- Memorystore: ~$200/month
- Other: ~$310/month
- **Total: ~$1,890/month**

**Interactive Prompts:**
- Email address for budget alerts (optional)

**Example:**
```bash
$ ./03-setup-budget-alerts.sh
Enter email address for budget alerts: devops@example.com
```

## Configuration Files

After running scripts, the following files are created:

```
scripts/gcp/
├── config/
│   ├── project-config.env                    # Main configuration
│   ├── budget-config.json                    # Budget settings
│   ├── workload-identity-bindings.sh         # Run after GKE creation
│   └── service-account-keys/                 # SA keys (gitignored)
│       └── cloudsql-proxy-sa-key.json
└── logs/
    └── gcp-setup/
        ├── project-setup-*.log               # Detailed logs
        ├── network-setup-*.log
        ├── service-accounts-setup-*.log
        ├── budget-alerts-setup-*.log
        └── *-summary-*.txt                   # Human-readable summaries
```

## project-config.env Format

```bash
# GCP Project Configuration
PROJECT_ID="byo-rag-dev"
PROJECT_NAME="BYO RAG System - Dev"
REGION="us-central1"
ZONE="us-central1-a"
BILLING_ACCOUNT_ID="01234-ABCDEF-567890"
BUDGET_AMOUNT=1000
ENVIRONMENT="dev"

# Network configuration
VPC_NAME="rag-vpc"
SUBNET_NAME="rag-gke-subnet"
SUBNET_RANGE="10.0.0.0/20"
PODS_RANGE="10.4.0.0/14"
SERVICES_RANGE="10.8.0.0/20"

# Service accounts
GKE_SA_NAME="gke-node-sa"
CLOUDSQL_SA_NAME="cloudsql-proxy-sa"
CLOUDBUILD_SA_NAME="cloud-build-sa"
```

## Verification

### Check Project Setup
```bash
# View project details
gcloud projects describe $(cat config/project-config.env | grep PROJECT_ID | cut -d '=' -f2 | tr -d '"')

# List enabled APIs
gcloud services list --enabled

# View billing info
gcloud beta billing projects describe $PROJECT_ID
```

### Check Network
```bash
# List VPCs
gcloud compute networks list

# List subnets
gcloud compute networks subnets list --network=rag-vpc

# List firewall rules
gcloud compute firewall-rules list --filter="network:rag-vpc"

# Check Cloud NAT
gcloud compute routers describe rag-router --region=$REGION
```

### Check Service Accounts
```bash
# List service accounts
gcloud iam service-accounts list

# View roles for service account
gcloud projects get-iam-policy $PROJECT_ID \
  --flatten="bindings[].members" \
  --filter="bindings.members:serviceAccount:gke-node-sa@*"
```

### Check Budget
```bash
# List budgets
gcloud billing budgets list --billing-account=$BILLING_ACCOUNT_ID
```

## Troubleshooting

### API Not Enabled Error
```
ERROR: (gcloud...) PERMISSION_DENIED: Service X is not enabled
```

**Solution:**
```bash
gcloud services enable SERVICE_NAME --project=$PROJECT_ID
```

### Insufficient Permissions
```
ERROR: (gcloud...) PERMISSION_DENIED: You do not have permission
```

**Solution:**
- Verify you have Project Editor or Owner role
- Check billing account permissions
- Ensure you're authenticated: `gcloud auth login`

### VPC Already Exists
```
ERROR: VPC 'rag-vpc' already exists
```

**Solution:**
- Scripts detect existing resources and skip creation
- Review existing configuration before proceeding
- Or use different VPC name in config

### Budget Creation Failed
```
ERROR: Budget creation via CLI not supported
```

**Solution:**
- This is expected - gcloud CLI has limited budget support
- Follow instructions to create budget in GCP Console
- Budget configuration saved to `config/budget-config.json`

## Next Steps

After completing GCP-INFRA-001:

1. **GCP-SECRETS-002:** Migrate secrets to Secret Manager
   ```bash
   # Scripts coming next
   ./04-migrate-secrets.sh
   ```

2. **GCP-REGISTRY-003:** Set up Container Registry
   ```bash
   # Scripts coming next
   ./05-setup-registry.sh
   ./06-build-push-images.sh
   ```

3. **GCP-SQL-004:** Set up Cloud SQL
   ```bash
   # Scripts coming next
   ./07-setup-cloudsql.sh
   ```

## Useful Resources

- [GCP Deployment Guide](../../docs/deployment/GCP_DEPLOYMENT_GUIDE.md)
- [Project Backlog](../../docs/project-management/PROJECT_BACKLOG.md)
- [GCP Console](https://console.cloud.google.com)
- [GCP Pricing Calculator](https://cloud.google.com/products/calculator)

## Support

For issues or questions:
1. Check logs in `logs/gcp-setup/`
2. Review summaries in `logs/gcp-setup/*-summary-*.txt`
3. Consult [GCP_DEPLOYMENT_GUIDE.md](../../docs/deployment/GCP_DEPLOYMENT_GUIDE.md)
4. Create GitHub issue with logs attached

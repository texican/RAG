---
version: 1.0.0
last-updated: 2025-11-12
status: active
applies-to: 0.8.0-SNAPSHOT
category: deployment
---

# GCP Deployment Guide - BYO RAG System

**Last Updated:** 2025-11-06
**Status:** Planning Complete - Ready for Implementation
**Estimated Timeline:** 3-4 weeks
**Total Effort:** 89 story points

---

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Architecture](#architecture)
4. [Deployment Phases](#deployment-phases)
5. [Detailed Implementation Guide](#detailed-implementation-guide)
6. [Cost Estimates](#cost-estimates)
7. [Security Considerations](#security-considerations)
8. [Monitoring & Operations](#monitoring--operations)
9. [Troubleshooting](#troubleshooting)

---

## Overview

This guide covers the complete migration of the BYO RAG System from local Docker deployment to Google Cloud Platform (GCP) production environment.

### Current State
- **Environment:** Local Docker Compose
- **Services:** 5 microservices (Auth, Document, Embedding, Core, Admin)
- **Infrastructure:** PostgreSQL, Redis, Kafka, Ollama (all containerized)
- **Deployment:** Manual docker-compose commands

### Target State
- **Environment:** GCP with GKE (Google Kubernetes Engine)
- **Services:** Same 5 microservices deployed to Kubernetes
- **Infrastructure:** Managed GCP services (Cloud SQL, Memorystore, Pub/Sub or Confluent)
- **Deployment:** Automated CI/CD via Cloud Build

### Key Benefits
- **Scalability:** Auto-scaling based on load
- **Reliability:** High availability and automated failover
- **Operations:** Managed infrastructure reduces maintenance
- **Security:** Enterprise-grade security and compliance
- **Observability:** Native monitoring and logging

---

## Prerequisites

### Required Tools
- **gcloud CLI:** Google Cloud SDK
  ```bash
  # Install gcloud CLI
  curl https://sdk.cloud.google.com | bash
  exec -l $SHELL
  gcloud init
  ```

- **kubectl:** Kubernetes CLI
  ```bash
  # Install kubectl
  gcloud components install kubectl
  ```

- **Docker:** For local image building
  ```bash
  # Already installed for local development
  docker --version
  ```

### Required Access
- GCP project with billing enabled
- Project Owner or Editor role
- Ability to create service accounts
- Access to enable APIs

### Required Knowledge
- Basic Kubernetes concepts
- GCP service fundamentals
- Docker containerization
- Spring Boot applications

---

## Architecture

### GCP Services Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     External Users                          │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
         ┌───────────────────────┐
         │   Cloud Load Balancer │ ← Cloud Armor (WAF)
         │   (HTTPS/SSL)         │
         └───────────┬───────────┘
                     │
                     ▼
         ┌───────────────────────┐
         │   GKE Ingress         │
         └───────────┬───────────┘
                     │
     ┌───────────────┴───────────────┐
     │                               │
     ▼                               ▼
┌─────────────────────┐    ┌─────────────────────┐
│  GKE Cluster        │    │  GKE Cluster        │
│  (Microservices)    │    │  (Microservices)    │
│                     │    │                     │
│  ┌──────────────┐   │    │  ┌──────────────┐   │
│  │ rag-auth     │   │    │  │ rag-document │   │
│  │ Port: 8081   │   │    │  │ Port: 8082   │   │
│  └──────┬───────┘   │    │  └──────┬───────┘   │
│         │           │    │         │           │
│  ┌──────▼───────┐   │    │  ┌──────▼───────┐   │
│  │ rag-core     │   │    │  │ rag-embedding│   │
│  │ Port: 8084   │   │    │  │ Port: 8083   │   │
│  └──────────────┘   │    │  └──────────────┘   │
│                     │    │                     │
│  ┌──────────────┐   │    │                     │
│  │ rag-admin    │   │    │                     │
│  │ Port: 8085   │   │    │                     │
│  └──────┬───────┘   │    │                     │
└─────────┼───────────┘    └─────────────────────┘
          │                          │
          ▼                          ▼
┌─────────────────────┐    ┌─────────────────────┐
│  Cloud SQL          │    │  Memorystore        │
│  (PostgreSQL 15)    │    │  (Redis)            │
│  + pgvector         │    │  Vector Storage     │
└─────────────────────┘    └─────────────────────┘
          │
          ▼
┌─────────────────────┐    ┌─────────────────────┐
│  Cloud Pub/Sub      │    │  Persistent Disks   │
│  OR                 │    │  (Document Storage) │
│  Confluent Cloud    │    └─────────────────────┘
└─────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────┐
│           Supporting Services                    │
│  • Secret Manager (credentials)                 │
│  • Cloud Logging (logs)                         │
│  • Cloud Monitoring (metrics)                   │
│  • Cloud Trace (distributed tracing)            │
│  • Artifact Registry (Docker images)            │
│  • Cloud Build (CI/CD)                          │
└─────────────────────────────────────────────────┘
```

### Service Mapping

| Local Service | GCP Equivalent | Purpose |
|--------------|----------------|---------|
| PostgreSQL (Docker) | Cloud SQL for PostgreSQL | Relational database with pgvector |
| Redis Stack (Docker) | Cloud Memorystore | Caching and vector storage |
| Kafka (Docker) | Cloud Pub/Sub or Confluent Cloud | Event streaming |
| Ollama (Docker) | GKE Deployment or Vertex AI | LLM inference |
| Docker Compose | GKE (Kubernetes) | Container orchestration |
| Local volumes | Persistent Disks | Document storage |
| .env file | Secret Manager | Secrets management |
| N/A | Cloud Load Balancer | External access |
| N/A | Cloud Armor | DDoS and WAF protection |

---

## Deployment Phases

### Phase 1: Foundation (Week 1)
**Story Points:** 21
**Stories:** GCP-INFRA-001, GCP-SECRETS-002, GCP-REGISTRY-003

**Deliverables:**
- GCP project configured with APIs enabled
- Secrets migrated to Secret Manager
- Docker images in Artifact Registry
- Network infrastructure ready

**Key Activities:**
1. Create and configure GCP project
2. Enable required APIs
3. Set up VPC and networking
4. Rotate and migrate all secrets
5. Build and push Docker images

### Phase 2: Managed Infrastructure (Week 2)
**Story Points:** 34
**Stories:** GCP-SQL-004, GCP-REDIS-005, GCP-KAFKA-006

**Deliverables:**
- Cloud SQL PostgreSQL with pgvector
- Cloud Memorystore Redis instance
- Kafka/Pub-Sub messaging configured
- Database schema migrated

**Key Activities:**
1. Provision Cloud SQL instance
2. Migrate database schema
3. Set up Cloud Memorystore
4. Configure Kafka or Pub/Sub
5. Test connectivity and performance

### Phase 3: Kubernetes Setup (Week 2-3)
**Story Points:** 26
**Stories:** GCP-GKE-007, GCP-K8S-008, GCP-STORAGE-009

**Deliverables:**
- GKE cluster operational
- Kubernetes manifests updated for GCP
- Persistent storage configured
- Workload Identity enabled

**Key Activities:**
1. Create GKE cluster
2. Update K8s manifests for GCP
3. Configure Cloud SQL Proxy
4. Set up persistent volumes
5. Test deployments in dev cluster

### Phase 4: Deployment & Access (Week 3-4)
**Story Points:** 16
**Stories:** GCP-INGRESS-010, GCP-DEPLOY-011

**Deliverables:**
- Load balancer configured
- SSL/TLS certificates active
- All services deployed and healthy
- End-to-end testing complete

**Key Activities:**
1. Configure ingress and load balancer
2. Set up SSL certificates
3. Deploy all microservices
4. Run integration tests
5. Validate full RAG workflow

---

## Detailed Implementation Guide

### Step 1: GCP Project Setup (GCP-INFRA-001)

**Estimated Time:** 2-3 hours
**Story Points:** 8

#### Create GCP Project

```bash
# Set project ID (must be globally unique)
export PROJECT_ID="byo-rag-system"
export REGION="us-central1"
export ZONE="us-central1-a"

# Create project
gcloud projects create $PROJECT_ID --name="BYO RAG System"

# Set as default project
gcloud config set project $PROJECT_ID

# Link billing account (get billing account ID from console)
export BILLING_ACCOUNT_ID="YOUR-BILLING-ACCOUNT-ID"
gcloud beta billing projects link $PROJECT_ID \
  --billing-account=$BILLING_ACCOUNT_ID
```

#### Enable Required APIs

```bash
# Create script to enable all required APIs
cat > enable-apis.sh << 'EOF'
#!/bin/bash

APIs=(
  "container.googleapis.com"           # GKE
  "sqladmin.googleapis.com"            # Cloud SQL
  "redis.googleapis.com"               # Memorystore
  "artifactregistry.googleapis.com"    # Container Registry
  "secretmanager.googleapis.com"       # Secret Manager
  "cloudbuild.googleapis.com"          # Cloud Build
  "compute.googleapis.com"             # Compute Engine
  "logging.googleapis.com"             # Cloud Logging
  "monitoring.googleapis.com"          # Cloud Monitoring
  "cloudtrace.googleapis.com"          # Cloud Trace
  "servicenetworking.googleapis.com"   # Private Service Access
  "dns.googleapis.com"                 # Cloud DNS
)

echo "Enabling ${#APIs[@]} APIs..."

for api in "${APIs[@]}"; do
  echo "Enabling $api..."
  gcloud services enable $api
done

echo "All APIs enabled successfully!"
EOF

chmod +x enable-apis.sh
./enable-apis.sh
```

#### Configure VPC Network

```bash
# Create VPC network
gcloud compute networks create rag-vpc \
  --subnet-mode=custom \
  --bgp-routing-mode=regional

# Create subnet for GKE
gcloud compute networks subnets create rag-gke-subnet \
  --network=rag-vpc \
  --region=$REGION \
  --range=10.0.0.0/20 \
  --secondary-range pods=10.4.0.0/14 \
  --secondary-range services=10.8.0.0/20

# Create Cloud Router for NAT
gcloud compute routers create rag-router \
  --network=rag-vpc \
  --region=$REGION

# Create Cloud NAT for outbound internet access
gcloud compute routers nats create rag-nat \
  --router=rag-router \
  --region=$REGION \
  --auto-allocate-nat-external-ips \
  --nat-all-subnet-ip-ranges
```

#### Create Service Accounts

```bash
# GKE node service account
gcloud iam service-accounts create gke-node-sa \
  --display-name="GKE Node Service Account"

# Cloud SQL Proxy service account
gcloud iam service-accounts create cloudsql-proxy-sa \
  --display-name="Cloud SQL Proxy Service Account"

# Cloud Build service account
gcloud iam service-accounts create cloud-build-sa \
  --display-name="Cloud Build Service Account"

# Grant necessary roles
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:gke-node-sa@$PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/logging.logWriter"

gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:gke-node-sa@$PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/monitoring.metricWriter"

gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:cloudsql-proxy-sa@$PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/cloudsql.client"
```

#### Set Up Budget Alerts

```bash
# Set budget alert (requires billing account permissions)
gcloud billing budgets create \
  --billing-account=$BILLING_ACCOUNT_ID \
  --display-name="RAG System Monthly Budget" \
  --budget-amount=1000USD \
  --threshold-rule=percent=50 \
  --threshold-rule=percent=75 \
  --threshold-rule=percent=90 \
  --threshold-rule=percent=100
```

---

### Step 2: Secret Manager Migration (GCP-SECRETS-002)

**Estimated Time:** 2-3 hours
**Story Points:** 5
**CRITICAL:** Security vulnerability - exposed API keys

#### Rotate OpenAI API Key (IMMEDIATE)

```bash
# 1. Go to OpenAI dashboard and create new API key
# 2. Save new key securely
# 3. Delete old key from OpenAI dashboard
# 4. Update local development environment with new key (temporarily)
```

#### Create Secrets in Secret Manager

```bash
# Generate new JWT secret (256-bit)
JWT_SECRET=$(openssl rand -base64 32)

# Generate new database password
DB_PASSWORD=$(openssl rand -base64 24)

# Generate new Redis password
REDIS_PASSWORD=$(openssl rand -base64 24)

# Create secrets in Secret Manager
echo -n "$DB_PASSWORD" | gcloud secrets create postgres-password \
  --data-file=- \
  --replication-policy="automatic"

echo -n "$REDIS_PASSWORD" | gcloud secrets create redis-password \
  --data-file=- \
  --replication-policy="automatic"

echo -n "$JWT_SECRET" | gcloud secrets create jwt-secret \
  --data-file=- \
  --replication-policy="automatic"

# Add OpenAI API key (use the NEW rotated key)
echo -n "YOUR-NEW-OPENAI-API-KEY" | gcloud secrets create openai-api-key \
  --data-file=- \
  --replication-policy="automatic"

# Verify secrets created
gcloud secrets list
```

#### Grant Secret Access to Service Accounts

```bash
# Grant GKE nodes access to secrets
for secret in postgres-password redis-password jwt-secret openai-api-key; do
  gcloud secrets add-iam-policy-binding $secret \
    --member="serviceAccount:gke-node-sa@$PROJECT_ID.iam.gserviceaccount.com" \
    --role="roles/secretmanager.secretAccessor"
done
```

#### Remove Secrets from Git History

```bash
# IMPORTANT: Do this in a separate branch first!

# Install git-filter-repo
pip install git-filter-repo

# Back up repository first
cd /Users/stryfe/Projects/RAG_SpecKit/RAG
git branch backup-before-secret-removal

# Remove .env from git history
git filter-repo --path .env --invert-paths

# Force push to remote (DESTRUCTIVE - coordinate with team)
# git push origin --force --all
```

#### Update .gitignore

```bash
# Add to .gitignore if not already present
cat >> .gitignore << 'EOF'

# Environment and secrets
.env
.env.*
*.key
*.pem
credentials.json
service-account-key.json
EOF
```

---

### Step 3: Container Registry Setup (GCP-REGISTRY-003)

**Estimated Time:** 3-4 hours
**Story Points:** 8

#### Create Artifact Registry Repository

```bash
# Create Docker repository
gcloud artifacts repositories create rag-system \
  --repository-format=docker \
  --location=$REGION \
  --description="BYO RAG System container images"

# Configure Docker to use Artifact Registry
gcloud auth configure-docker ${REGION}-docker.pkg.dev
```

#### Build and Push Images

```bash
# Navigate to project root
cd /Users/stryfe/Projects/RAG_SpecKit/RAG

# Build all services
./mvnw clean package -DskipTests

# Build and tag Docker images
SERVICES=("rag-auth" "rag-document" "rag-embedding" "rag-core" "rag-admin")
VERSION="1.0.0"
GIT_SHA=$(git rev-parse --short HEAD)

for service in "${SERVICES[@]}"; do
  echo "Building $service..."

  # Build image
  docker build -t $service:latest -f ${service}-service/Dockerfile .

  # Tag for Artifact Registry
  docker tag $service:latest \
    ${REGION}-docker.pkg.dev/${PROJECT_ID}/rag-system/$service:$VERSION

  docker tag $service:latest \
    ${REGION}-docker.pkg.dev/${PROJECT_ID}/rag-system/$service:$GIT_SHA

  docker tag $service:latest \
    ${REGION}-docker.pkg.dev/${PROJECT_ID}/rag-system/$service:latest

  # Push to Artifact Registry
  docker push ${REGION}-docker.pkg.dev/${PROJECT_ID}/rag-system/$service:$VERSION
  docker push ${REGION}-docker.pkg.dev/${PROJECT_ID}/rag-system/$service:$GIT_SHA
  docker push ${REGION}-docker.pkg.dev/${PROJECT_ID}/rag-system/$service:latest

  echo "$service pushed successfully!"
done
```

#### Enable Vulnerability Scanning

```bash
# Vulnerability scanning is automatic on Artifact Registry
# View scan results
gcloud artifacts docker images list ${REGION}-docker.pkg.dev/${PROJECT_ID}/rag-system

# Get detailed scan for specific image
gcloud artifacts docker images describe \
  ${REGION}-docker.pkg.dev/${PROJECT_ID}/rag-system/rag-auth:latest \
  --show-all-metadata
```

---

### Step 4: Cloud SQL Setup (GCP-SQL-004)

**Estimated Time:** 4-6 hours
**Story Points:** 13

#### Create Cloud SQL Instance

```bash
# Create PostgreSQL instance with high availability
gcloud sql instances create rag-postgres \
  --database-version=POSTGRES_15 \
  --tier=db-custom-2-7680 \
  --region=$REGION \
  --network=rag-vpc \
  --no-assign-ip \
  --availability-type=regional \
  --backup-start-time=03:00 \
  --enable-bin-log \
  --database-flags=cloudsql.enable_pgvector=on

# Wait for instance to be ready (takes 5-10 minutes)
gcloud sql operations list --instance=rag-postgres --limit=1
```

#### Configure Database

```bash
# Set root password (use generated password from Secret Manager)
gcloud sql users set-password postgres \
  --instance=rag-postgres \
  --password="$DB_PASSWORD"

# Create application user
gcloud sql users create rag_user \
  --instance=rag-postgres \
  --password="$DB_PASSWORD"

# Create database
gcloud sql databases create rag_enterprise \
  --instance=rag-postgres
```

#### Install pgvector Extension

```bash
# Connect to database
gcloud sql connect rag-postgres --user=postgres

# In psql prompt:
# \c rag_enterprise
# CREATE EXTENSION IF NOT EXISTS vector;
# \q
```

#### Run Database Migrations

```bash
# Get Cloud SQL Proxy
wget https://dl.google.com/cloudsql/cloud_sql_proxy.linux.amd64 -O cloud_sql_proxy
chmod +x cloud_sql_proxy

# Start proxy
./cloud_sql_proxy -instances=${PROJECT_ID}:${REGION}:rag-postgres=tcp:5432 &

# Run Flyway or Liquibase migrations
# Or use Spring Boot schema initialization
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/rag_enterprise \
SPRING_DATASOURCE_USERNAME=rag_user \
SPRING_DATASOURCE_PASSWORD=$DB_PASSWORD \
./mvnw flyway:migrate

# Stop proxy
kill %1
```

---

### Step 5: Cloud Memorystore Setup (GCP-REDIS-005)

**Estimated Time:** 2-3 hours
**Story Points:** 8

#### Create Memorystore Instance

```bash
# Create Redis instance (Standard tier for HA)
gcloud redis instances create rag-redis \
  --size=5 \
  --region=$REGION \
  --network=rag-vpc \
  --redis-version=redis_7_0 \
  --tier=standard \
  --connect-mode=private-service-access

# Wait for instance creation (takes 5-10 minutes)
gcloud redis instances describe rag-redis --region=$REGION
```

#### Get Connection Information

```bash
# Get Redis host and port
export REDIS_HOST=$(gcloud redis instances describe rag-redis \
  --region=$REGION \
  --format='get(host)')

export REDIS_PORT=$(gcloud redis instances describe rag-redis \
  --region=$REGION \
  --format='get(port)')

echo "Redis connection: $REDIS_HOST:$REDIS_PORT"
```

---

### Step 6: Kafka/Pub-Sub Migration (GCP-KAFKA-006)

**Estimated Time:** 6-8 hours
**Story Points:** 13

#### Option A: Cloud Pub/Sub (Recommended)

```bash
# Create topics
TOPICS=("document-processing" "embedding-requests" "rag-queries")

for topic in "${TOPICS[@]}"; do
  gcloud pubsub topics create $topic

  # Create subscription
  gcloud pubsub subscriptions create ${topic}-sub \
    --topic=$topic \
    --ack-deadline=60
done
```

**Code Changes Required:**
- Replace Kafka client libraries with Pub/Sub client
- Update message producers and consumers
- Maintain message ordering where needed

#### Option B: Confluent Cloud (Drop-in replacement)

```bash
# Sign up for Confluent Cloud
# Create cluster in GCP us-central1
# Get bootstrap servers and API credentials
# Update Kubernetes secrets with new connection strings
```

---

### Step 7: GKE Cluster Setup (GCP-GKE-007)

**Estimated Time:** 4-6 hours
**Story Points:** 13

#### Create GKE Cluster

```bash
# Create regional cluster for high availability
gcloud container clusters create rag-cluster \
  --region=$REGION \
  --network=rag-vpc \
  --subnetwork=rag-gke-subnet \
  --cluster-secondary-range-name=pods \
  --services-secondary-range-name=services \
  --enable-ip-alias \
  --enable-autoscaling \
  --min-nodes=1 \
  --max-nodes=5 \
  --num-nodes=2 \
  --machine-type=n1-standard-4 \
  --enable-stackdriver-kubernetes \
  --enable-autorepair \
  --enable-autoupgrade \
  --workload-pool=${PROJECT_ID}.svc.id.goog \
  --service-account=gke-node-sa@${PROJECT_ID}.iam.gserviceaccount.com \
  --addons=HttpLoadBalancing,HorizontalPodAutoscaling

# Get cluster credentials
gcloud container clusters get-credentials rag-cluster --region=$REGION
```

#### Install Cluster Add-ons

```bash
# Install NGINX Ingress Controller
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.2/deploy/static/provider/cloud/deploy.yaml

# Install cert-manager for SSL
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml

# Install Secret Manager CSI driver
kubectl apply -f https://github.com/GoogleCloudPlatform/secrets-store-csi-driver-provider-gcp/releases/latest/download/secrets-store-csi-driver-provider-gcp.yaml
```

---

## Cost Estimates

### Development Environment
| Service | Configuration | Monthly Cost |
|---------|--------------|--------------|
| GKE Cluster | 3 x n1-standard-2 nodes | ~$220 |
| Cloud SQL | db-custom-1-3840 (1 vCPU, 3.75GB) | ~$120 |
| Cloud Memorystore | 5GB Standard tier | ~$50 |
| Cloud Pub/Sub | 10GB/month | ~$5 |
| Persistent Disks | 100GB SSD | ~$17 |
| Load Balancer | 1 forwarding rule | ~$18 |
| Cloud Build | 120 build-minutes/day | Free tier |
| Cloud Logging | 50GB/month | Free tier |
| **TOTAL** | | **~$430/month** |

### Production Environment
| Service | Configuration | Monthly Cost |
|---------|--------------|--------------|
| GKE Cluster | 6 x n1-standard-4 nodes (regional) | ~$900 |
| Cloud SQL | db-custom-4-15360 (4 vCPU, 15GB) HA | ~$480 |
| Cloud Memorystore | 20GB Standard tier | ~$200 |
| Cloud Pub/Sub | 100GB/month | ~$40 |
| Persistent Disks | 500GB SSD | ~$85 |
| Load Balancer | Multi-region | ~$35 |
| Cloud Build | 500 build-minutes/day | ~$50 |
| Cloud Logging | 200GB/month | ~$100 |
| **TOTAL** | | **~$1,890/month** |

**Cost Optimization Tips:**
- Use committed use discounts (30-50% savings)
- Enable cluster autoscaling
- Use preemptible VMs for non-critical workloads
- Set up budget alerts
- Review and right-size resources monthly

---

## Security Considerations

### Secret Management
- ✅ All secrets in Secret Manager
- ✅ No secrets in version control
- ✅ Workload Identity for pod-level access
- ✅ Automatic secret rotation configured
- ✅ Secret access audited

### Network Security
- ✅ Private GKE cluster (no public IPs on nodes)
- ✅ VPC with custom subnets
- ✅ Cloud NAT for outbound only
- ✅ Network policies for pod-to-pod communication
- ✅ Private Service Access for Cloud SQL

### Application Security
- ✅ Cloud Armor WAF rules
- ✅ DDoS protection via Cloud Armor
- ✅ SSL/TLS termination at load balancer
- ✅ Container vulnerability scanning
- ✅ Binary Authorization (optional)

### Access Control
- ✅ IAM roles with least privilege
- ✅ Workload Identity for GKE
- ✅ Service account per microservice
- ✅ Audit logging enabled
- ✅ MFA required for admin access

---

## Monitoring & Operations

### Key Metrics to Monitor
- Pod CPU and memory usage
- Request latency (p50, p95, p99)
- Error rates per service
- Database connection pool usage
- Redis memory usage
- Pub/Sub message lag
- SSL certificate expiration

### Alerting Policies
```bash
# Create uptime check
gcloud monitoring uptime create rag-system-uptime \
  --resource-type=uptime-url \
  --display-name="RAG System Health" \
  --checked-url=https://your-domain.com/health

# Create alerting policy for high error rate
# (Do this via GCP Console for easier configuration)
```

### Logging Best Practices
- Structured logging (JSON format)
- Include correlation IDs
- Log levels: ERROR, WARN, INFO, DEBUG
- Centralize in Cloud Logging
- Set up log-based metrics

---

## Troubleshooting

### Common Issues

#### Pods Not Starting
```bash
# Check pod status
kubectl get pods -n rag-system

# Describe pod for events
kubectl describe pod <pod-name> -n rag-system

# Check logs
kubectl logs <pod-name> -n rag-system

# Check image pull
kubectl get events -n rag-system --sort-by='.lastTimestamp'
```

#### Database Connection Issues
```bash
# Verify Cloud SQL Proxy is running
kubectl get pods -n rag-system | grep cloudsql-proxy

# Check Cloud SQL instance status
gcloud sql instances describe rag-postgres

# Test connection from pod
kubectl exec -it <pod-name> -n rag-system -- \
  psql -h localhost -U rag_user -d rag_enterprise
```

#### Load Balancer Not Working
```bash
# Check ingress status
kubectl get ingress -n rag-system

# Describe ingress for backend status
kubectl describe ingress rag-ingress -n rag-system

# Check backend health
gcloud compute backend-services list
gcloud compute backend-services get-health <backend-service-name>
```

---

## Next Steps

1. **Start with Phase 1:** Complete GCP-INFRA-001, GCP-SECRETS-002, GCP-REGISTRY-003
2. **Security First:** Rotate exposed API keys immediately
3. **Incremental Migration:** Test each component before proceeding
4. **Document Everything:** Keep runbooks updated
5. **Monitor Costs:** Review spending weekly during initial deployment

For detailed task breakdown, see [PROJECT_BACKLOG.md](../project-management/PROJECT_BACKLOG.md).

For questions or issues, refer to troubleshooting section or create a GitHub issue.

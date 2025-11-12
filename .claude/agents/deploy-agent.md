# Deploy Agent

---
**version**: 1.0.0  
**last-updated**: 2025-11-12  
**domain**: Deployment  
**depends-on**: main agent-instructions.md  
**can-call**: test-agent, git-agent  
---

## Purpose

The Deploy Agent is responsible for all deployment tasks including local development setup (Colima/Docker Compose), GCP/GKE production deployment, infrastructure provisioning, service health validation, and troubleshooting deployment issues.

## Responsibilities

- Local deployment using Colima and Docker Compose
- GCP infrastructure provisioning (7-phase deployment)
- GKE cluster management and service deployment
- Health validation and verification
- Deployment troubleshooting and rollback
- Infrastructure-as-code management (K8s manifests, Cloud Build)

## When to Use

**Invoke this agent when**:
- "Deploy to GCP"
- "Setup local environment"
- "Deploy services"
- "Fix deployment issue"
- "Validate deployment"
- "Setup infrastructure"
- "Deploy to Kubernetes"
- "Start local services"

**Don't invoke for**:
- Running tests (use test-agent)
- Code changes (use dev-agent)
- Story completion (use backlog-agent)

---

## Deployment Types

### Local Development (Colima)
- Docker Compose for all services
- Local PostgreSQL, Redis, Kafka, Ollama
- Hot reload with Spring Boot DevTools
- Port forwarding: 8081-8085
- Cost: $0 (free)

### GCP Production (GKE)
- Managed Kubernetes (GKE)
- Cloud SQL PostgreSQL
- Cloud Memorystore Redis
- Artifact Registry
- Secret Manager
- Cloud Load Balancer with SSL
- Cost: ~$430/month (dev), ~$1,890/month (prod)

---

# Part 1: Local Deployment (Colima)

## Prerequisites Check

```bash
# Verify Colima is running (macOS)
colima status

# Start Colima if needed (8GB RAM, 4 CPU, 60GB disk recommended)
colima start --memory 8 --cpu 4 --disk 60

# Verify Docker
docker info
docker-compose version

# Verify Java and Maven
java -version  # Should be Java 21+
mvn -version   # Should be Maven 3.8+
```

## Quick Start - Fully Automated

```bash
cd /Users/stryfe/Projects/RAG

# Run the automated setup script
./scripts/setup/setup-local-dev.sh

# This will:
# - Check prerequisites (Java 21+, Maven 3.8+, Docker, Colima)
# - Create .env file with default configuration
# - Start Docker infrastructure (PostgreSQL, Redis, Kafka, Zookeeper, Ollama)
# - Pull Ollama models (nomic-embed-text, llama2:7b)
# - Build all Maven modules
# - Create service management scripts
# - Create health check and test scripts
# - Set up initial admin user

# After setup completes, verify everything is healthy
./scripts/utils/health-check.sh
```

## Manual Step-by-Step Setup

```bash
cd /Users/stryfe/Projects/RAG

# Step 1: Create .env file (if not exists)
cat > .env << 'EOF'
# Spring Profile
SPRING_PROFILES_ACTIVE=docker

# Database Configuration
POSTGRES_DB=byo_rag_local
DB_HOST=postgres
DB_USERNAME=rag_user
DB_PASSWORD=rag_password

# Redis Configuration
REDIS_HOST=redis
REDIS_PASSWORD=redis_password

# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=kafka:29092

# JWT Configuration
JWT_SECRET=admin-super-secret-key-that-should-be-at-least-256-bits-long-for-production-use

# AI/ML Configuration
OLLAMA_URL=http://ollama:11434
EMBEDDING_SERVICE_URL=http://rag-embedding:8083

# CORS Configuration
CORS_ALLOWED_ORIGINS=http://localhost:3000
EOF

# Step 2: Build all JARs
make build-all
# OR: mvn clean package -DskipTests

# Step 3: Start all infrastructure services
make start
# OR: docker-compose up -d

# Step 4: Wait for services to be healthy (30-60 seconds)
# Watch logs to see startup progress
make logs
# OR: docker-compose logs -f

# Step 5: Check health
make status
# OR: docker-compose ps

# Step 6: Create admin user
make create-admin-user

# Step 7: Verify with health check
./scripts/utils/health-check.sh
```

## Service Startup Order

Docker Compose handles dependencies automatically, but the order is:

1. **Infrastructure** (no dependencies):
   - PostgreSQL (port 5432)
   - Zookeeper (port 2181)

2. **Messaging & Storage** (depends on infrastructure):
   - Kafka (port 9092) - depends on Zookeeper
   - Redis (port 6379)
   - Ollama (port 11434)

3. **Core Services** (depends on infrastructure):
   - rag-auth (port 8081) - depends on PostgreSQL, Redis
   - rag-document (port 8082) - depends on PostgreSQL, Kafka, Redis
   - rag-embedding (port 8083) - depends on Redis, Kafka, Ollama
   
4. **Dependent Services** (depends on core):
   - rag-core (port 8084) - depends on rag-embedding, Redis, Ollama
   - rag-admin (port 8085) - depends on PostgreSQL, Redis

5. **Monitoring** (optional):
   - Prometheus (port 9090)
   - Grafana (port 3000)
   - Kafka UI (port 8090)

## Environment Variables Explained

**Required for Local Development:**
```bash
SPRING_PROFILES_ACTIVE=docker       # Uses docker-compose service names
POSTGRES_DB=byo_rag_local           # Database name pattern: byo_rag_{env}
DB_HOST=postgres                    # Docker Compose service name
KAFKA_BOOTSTRAP_SERVERS=kafka:29092 # Internal Docker network address
REDIS_HOST=redis                    # Docker Compose service name
```

**Security (Development Defaults):**
```bash
DB_USERNAME=rag_user
DB_PASSWORD=rag_password
REDIS_PASSWORD=redis_password
JWT_SECRET=admin-super-secret-key-that-should-be-at-least-256-bits-long-for-production-use
```

**AI Configuration:**
```bash
OLLAMA_URL=http://ollama:11434      # Local LLM (optional, for embeddings)
EMBEDDING_SERVICE_URL=http://rag-embedding:8083
```

## Verifying Local Deployment

**1. Check Container Status:**
```bash
make status
# OR
docker-compose ps

# Should see all services "Up" and "healthy"
```

**2. Check Service Health Endpoints:**
```bash
# Quick check all services
curl http://localhost:8081/actuator/health  # Auth
curl http://localhost:8082/actuator/health  # Document
curl http://localhost:8083/actuator/health  # Embedding
curl http://localhost:8084/actuator/health  # Core
curl http://localhost:8085/admin/api/actuator/health  # Admin

# OR use health check script
./scripts/utils/health-check.sh
```

**3. Test Admin Login:**
```bash
# Login with default admin credentials
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@enterprise-rag.com","password":"admin123"}'

# Should return JWT token
```

**4. Access Interactive API Documentation:**
```bash
# Open Swagger UI for each service
open http://localhost:8081/swagger-ui.html  # Auth
open http://localhost:8082/swagger-ui.html  # Document
open http://localhost:8083/swagger-ui.html  # Embedding
open http://localhost:8084/swagger-ui.html  # Core
open http://localhost:8085/admin/api/swagger-ui.html  # Admin
```

## Local Deployment Troubleshooting

### Issue: Colima Not Running
```bash
# Symptoms
docker: Cannot connect to the Docker daemon

# Solution
colima status  # Check status
colima start --memory 8 --cpu 4 --disk 60  # Start with resources
```

### Issue: Port Already in Use
```bash
# Symptoms
Error: bind: address already in use

# Solution
# Find process using port
lsof -i :8081

# Kill process
kill -9 <PID>

# OR change port in docker-compose.yml
ports:
  - "8181:8081"  # Use 8181 externally, 8081 internally
```

### Issue: Services Not Healthy
```bash
# Symptoms
Containers running but health check failing

# Solution
# Check logs for specific service
make logs SERVICE=rag-auth

# Common causes:
# 1. Database not ready - wait 30-60 seconds
# 2. Out of memory - increase Colima memory
# 3. Missing dependencies - rebuild with make rebuild SERVICE=rag-xxx
```

### Issue: Database Connection Refused
```bash
# Symptoms
Could not open JDBC Connection

# Solution
# Verify PostgreSQL is running and healthy
docker-compose ps postgres
docker logs rag-postgres

# Check database exists
docker exec -it rag-postgres psql -U rag_user -l

# Verify database name matches .env
# Should be: byo_rag_local (or byo_rag_dev for GCP)
```

### Issue: Out of Memory (OOMKilled)
```bash
# Symptoms
Containers randomly stopping, exit code 137

# Solution
# Increase Colima memory allocation
colima stop
colima start --memory 12 --cpu 6 --disk 60

# Reduce JVM heap if needed (in Dockerfile)
ENV JAVA_OPTS="-Xms512m -Xmx1024m"
```

## Advanced Local Development

### Running Services Individually (Without Docker)
```bash
# Start infrastructure only
docker-compose up -d postgres redis kafka zookeeper ollama

# Run each service with Maven (in separate terminals)
cd rag-auth-service && mvn spring-boot:run        # Terminal 1
cd rag-document-service && mvn spring-boot:run    # Terminal 2
cd rag-embedding-service && mvn spring-boot:run   # Terminal 3
cd rag-core-service && mvn spring-boot:run        # Terminal 4
cd rag-admin-service && mvn spring-boot:run       # Terminal 5

# Use different .env for local Maven execution
SPRING_PROFILES_ACTIVE=local  # Uses localhost instead of docker service names
```

### Hot Reload During Development
```bash
# Spring Boot DevTools is included - code changes auto-reload
# Just edit Java files and save - service reloads automatically

# For Docker: rebuild specific service
make rebuild SERVICE=rag-auth

# Without cache (for stubborn issues)
make rebuild-nc SERVICE=rag-auth
```

### Database Access
```bash
# Connect to PostgreSQL
docker exec -it rag-postgres psql -U rag_user -d byo_rag_local

# Common SQL queries
\dt                           # List tables
SELECT * FROM tenants;        # View tenants
SELECT * FROM users;          # View users
SELECT * FROM documents;      # View documents

# Exit psql
\q
```

## Cleaning Up

```bash
# Stop all services (keeps data)
make stop
# OR
docker-compose down

# Stop and remove ALL data (⚠️ destructive!)
docker-compose down -v

# Remove all RAG Docker images
make clean-docker

# Clean Maven build
make clean

# Nuclear option - clean everything
make clean-all
```

---

# Part 2: GCP Deployment (7-Phase Process)

## Architecture Overview

Production deployment uses Google Kubernetes Engine (GKE) with fully managed GCP services:

**Infrastructure Stack:**
- **Compute:** GKE cluster (regional, auto-scaling 1-5 nodes)
- **Database:** Cloud SQL PostgreSQL 15 with pgvector extension
- **Cache:** Cloud Memorystore Redis 7.0 (Standard tier for HA)
- **Messaging:** Cloud Pub/Sub (Kafka optional but disabled to save $250-450/month)
- **Storage:** Persistent Disks for document storage
- **Registry:** Artifact Registry for container images
- **Secrets:** Secret Manager (Workload Identity for secure access)
- **Networking:** Private VPC, Cloud NAT, Cloud Load Balancer
- **Security:** Cloud Armor WAF, SSL/TLS via cert-manager

**Cost Estimates:**
- Dev environment: ~$430/month
- Production: ~$1,890/month
- Use committed use discounts for 30-50% savings

## GCP Prerequisites

**Required Tools:**
```bash
# Install gcloud CLI
curl https://sdk.cloud.google.com | bash
exec -l $SHELL
gcloud init

# Install kubectl via gcloud
gcloud components install kubectl

# Authenticate
gcloud auth login
gcloud auth application-default login
```

**Required Access:**
- GCP project with billing enabled
- Project Owner or Editor role
- Billing Account Admin (for budget setup)

**Verify Prerequisites:**
```bash
# Check gcloud
gcloud --version

# Check kubectl
kubectl version --client

# Check Docker
docker --version

# Check active account
gcloud auth list
```

## Phase 1: Infrastructure Setup (45-55 min | 8 points)

### Step 1: Project Setup (15-20 min)

```bash
cd /Users/stryfe/Projects/RAG
./scripts/gcp/00-setup-project.sh
```

**What it does:**
- Creates GCP project with unique ID
- Links billing account
- Enables 15+ required APIs
- Generates `config/project-config.env`
- Creates logs in `logs/gcp-setup/`

**Interactive prompts:**
- Project ID (globally unique, e.g., `byo-rag-dev`)
- Project Name
- Region (default: `us-central1`)
- Zone
- Billing Account ID
- Monthly budget
- Environment (dev/staging/production)

### Step 2: Network Setup (10-15 min)

```bash
./scripts/gcp/01-setup-network.sh
```

**What it does:**
- Creates VPC (`rag-vpc`)
- Creates GKE subnet with secondary ranges
- Configures firewall rules
- Sets up Cloud Router and NAT
- Enables Private Service Access

### Step 3: Service Accounts (5-10 min)

```bash
./scripts/gcp/02-setup-service-accounts.sh
```

**What it does:**
- Creates GKE node service account
- Creates Cloud SQL Proxy service account
- Creates Cloud Build service account
- Exports keys to `config/service-account-keys/`

### Step 4: Budget Alerts (5-10 min)

```bash
./scripts/gcp/03-setup-budget-alerts.sh
```

**What it does:**
- Creates monthly budget with threshold alerts (50%, 75%, 90%, 100%)
- Configures email notifications

## Phase 2: Secrets & Registry (1-2 hours | 13 points)

### Step 5: Migrate Secrets to Secret Manager

```bash
./scripts/gcp/04-migrate-secrets.sh
```

**CRITICAL SECURITY:** This script:
1. Generates new secure passwords
2. Migrates secrets to GCP Secret Manager
3. Removes `.env` from git history (creates backup branch first)
4. Updates `.gitignore`

**Secrets Created:**
- `postgres-password` - Generated 24-char password
- `redis-password` - Generated 24-char password
- `jwt-secret` - Generated 32-byte secret
- `openai-api-key` - Migrated from .env (rotated first!)

### Step 6: Setup Artifact Registry

```bash
# Create repository
gcloud artifacts repositories create rag-system \
  --repository-format=docker \
  --location=us-central1 \
  --description="BYO RAG System container images"

# Configure Docker authentication
gcloud auth configure-docker us-central1-docker.pkg.dev
```

## Phase 3: Build and Push Images

### Option A: Local Build (Faster for Development)

```bash
./scripts/gcp/07-build-and-push-images.sh
```

**What it does:**
1. Builds all services via Maven
2. Builds Docker images for 5 services
3. Tags images (version, git SHA, latest)
4. Pushes to Artifact Registry
5. Enables vulnerability scanning

**Build single service:**
```bash
./scripts/gcp/07-build-and-push-images.sh --service rag-auth-service
```

### Option B: Cloud Build (Recommended for Production)

```bash
./scripts/gcp/07a-cloud-build-images.sh --env dev
```

**Advantages:**
- Correct architecture (x86) automatically
- Faster parallel builds
- No local resources consumed
- Integrated security scanning
- Build history in Cloud Console

**Verify images:**
```bash
gcloud artifacts docker images list \
  us-central1-docker.pkg.dev/${PROJECT_ID}/rag-system
```

## Phase 4: Managed Infrastructure (2-4 hours | 21 points)

### Step 7: Setup Cloud SQL PostgreSQL

```bash
./scripts/gcp/08-setup-cloud-sql.sh
```

**What it does:**
1. Creates PostgreSQL 15 instance with HA
2. Enables pgvector extension
3. Creates database: `rag_enterprise`
4. Creates user: `rag_user`
5. Runs database migrations

**Instance specs (dev):**
- Tier: db-custom-1-3840 (1 vCPU, 3.75GB RAM)
- Cost: ~$120/month

**Verify Cloud SQL:**
```bash
gcloud sql instances describe rag-postgres-dev
gcloud sql databases list --instance=rag-postgres-dev
```

### Step 8: Setup Cloud Memorystore Redis

```bash
./scripts/gcp/10-setup-memorystore.sh
```

**What it does:**
1. Creates Redis 7.0 instance (Standard tier for HA)
2. Configures private IP
3. Enables automatic failover

**Instance specs (dev):**
- Size: 5GB
- Tier: Standard (HA)
- Cost: ~$50/month

**Verify Redis:**
```bash
gcloud redis instances describe rag-redis-dev --region=us-central1
```

## Phase 5: GKE Cluster Setup (30-60 min | 13 points)

### Step 9: Create GKE Cluster

```bash
./scripts/gcp/12-setup-gke-cluster.sh
```

**What it does:**
1. Creates regional GKE cluster (3 zones for HA)
2. Configures auto-scaling (1-5 nodes)
3. Enables Workload Identity
4. Installs cluster add-ons (NGINX Ingress, cert-manager, Secret Manager CSI)
5. Creates namespace: `rag-system`

**Cluster specs (dev):**
- Nodes: 2x n1-standard-4 (4 vCPU, 15GB RAM each)
- Auto-scaling: 1-5 nodes
- Cost: ~$220/month

**Get cluster credentials:**
```bash
gcloud container clusters get-credentials rag-cluster-dev \
  --region=us-central1 \
  --project=byo-rag-dev

# Verify access
kubectl cluster-info
kubectl get nodes
```

### Step 10: Sync Secrets to Kubernetes

```bash
./scripts/gcp/13-sync-secrets-to-k8s.sh --env dev
```

**What it does:**
1. Pulls secrets from Secret Manager
2. Creates Kubernetes secrets in `rag-system` namespace
3. Configures Workload Identity

**Verify secrets:**
```bash
kubectl get secrets -n rag-system
```

## Phase 6: Deploy Services (15-30 min | 8 points)

### Step 11: Deploy Microservices to GKE

```bash
./scripts/gcp/17-deploy-services.sh --env dev
```

**What it does:**
1. Validates prerequisites
2. Deploys services in dependency order
3. Waits for health checks
4. Validates inter-service connectivity

**Service architecture:**
- **rag-auth-service**: 1 replica (dev), 3 (prod) | 256Mi-512Mi RAM | Port 8081
- **rag-document-service**: 1 replica (dev), 3 (prod) | 1Gi-2Gi RAM | Port 8082
- **rag-embedding-service**: 1 replica (dev), 3 (prod) | 2Gi-4Gi RAM | Port 8083
- **rag-core-service**: 1 replica (dev), 3 (prod) | 1Gi-2Gi RAM | Port 8084
- **rag-admin-service**: 1 replica (dev), 3 (prod) | 512Mi-1Gi RAM | Port 8085

**Deploy single service:**
```bash
./scripts/gcp/17-deploy-services.sh --env dev --service rag-auth
```

**Monitor deployment:**
```bash
# Watch pods start
kubectl get pods -n rag-system -w

# Check deployment status
kubectl rollout status deployment -n rag-system

# Check logs
kubectl logs -n rag-system -l app=rag-auth --tail=100 -f
```

### Step 12: Initialize Database

```bash
./scripts/gcp/18-init-database.sh --env dev
```

**What it does:**
1. Tests Cloud SQL connectivity
2. Runs database migrations
3. Creates default tenant
4. Creates admin user: `admin@enterprise-rag.com` / `admin123`
5. Tests authentication

## Phase 7: Validation & Testing (10-20 min)

### Step 13: Comprehensive Deployment Validation

```bash
./scripts/gcp/19-validate-deployment.sh --env dev
```

**What it validates:**
1. Pod Status - All Running and Ready
2. Service Endpoints - All healthy
3. Health Checks - Passing
4. Inter-Service Connectivity - DNS and HTTP working
5. Database Connectivity - Cloud SQL accessible
6. Redis Connectivity - Memorystore accessible
7. Resource Usage - No OOMKilled pods
8. Admin Authentication - Login works
9. Swagger UI - API docs accessible
10. Integration Tests - Optional
11. RAG Workflow - End-to-end document upload → query

**Expected output:**
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
VALIDATION SUMMARY
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Total Tests: 12
Passed: 12
Failed: 0
Warnings: 0

✓ All validation tests passed!

Deployment is healthy and ready for use.
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

## Accessing Services

### Option A: Port Forwarding (Development)

```bash
# Auth service
kubectl port-forward -n rag-system svc/rag-auth-service 8081:8081 &

# Core service
kubectl port-forward -n rag-system svc/rag-core-service 8084:8084 &

# Test endpoints
curl http://localhost:8081/actuator/health
open http://localhost:8081/swagger-ui.html
```

### Option B: Ingress with SSL (Production)

```bash
# Setup ingress with domain
./scripts/gcp/16-setup-ingress.sh \
  --env dev \
  --domain rag-dev.example.com

# Access via HTTPS
curl https://rag-dev.example.com/api/v1/auth/health
```

## GCP Deployment Troubleshooting

### Issue: Pods Stuck in Pending

**Diagnosis:**
```bash
kubectl describe pod -n rag-system <pod-name>
```

**Common causes:**

1. **Insufficient resources** - Node pool full
   ```bash
   kubectl top nodes
   ```
   **Solution:** Scale up node pool

2. **Image pull failure** - Cannot pull from Artifact Registry
   ```bash
   kubectl get events -n rag-system | grep "Failed to pull image"
   ```
   **Solution:** Verify images exist, check IAM permissions

3. **PVC not bound** - Persistent volume claim unbound
   ```bash
   kubectl get pvc -n rag-system
   ```
   **Solution:** Check StorageClass configuration

### Issue: Pods CrashLoopBackOff

**Diagnosis:**
```bash
kubectl logs -n rag-system <pod-name> --previous
kubectl describe pod -n rag-system <pod-name>
```

**Common causes:**

1. **Database connection failure**
   ```bash
   kubectl logs -n rag-system <pod-name> | grep "Connection refused"
   ```
   **Solution:** Verify Cloud SQL Proxy running, check secrets

2. **Missing secrets**
   ```bash
   kubectl get secrets -n rag-system
   ```
   **Solution:** Run `./scripts/gcp/13-sync-secrets-to-k8s.sh`

3. **Application error**
   ```bash
   kubectl logs -n rag-system <pod-name> | grep "Exception"
   ```
   **Solution:** Review logs, check configuration

### Issue: Service Not Accessible

**Diagnosis:**
```bash
kubectl get svc -n rag-system
kubectl get endpoints -n rag-system
```

**Common causes:**

1. **No endpoints** - Service has no healthy pods
   **Solution:** Fix pod health issues first

2. **Wrong port** - Using incorrect service port
   **Solution:** Verify port mappings (8081-8085)

### Issue: Authentication Fails

**Diagnosis:**
```bash
# Check admin user in database
kubectl exec -n rag-system $AUTH_POD -- \
  sh -c "PGPASSWORD='$DB_PASSWORD' psql -h 127.0.0.1 -U rag_user -d rag_db \
  -c 'SELECT email, is_active FROM users;'"
```

**Solution:** Run `./scripts/gcp/18-init-database.sh` to recreate admin user

## Rollback Procedures

```bash
# Rollback specific service
kubectl rollout undo deployment rag-auth-service -n rag-system

# Rollback to specific revision
kubectl rollout history deployment rag-auth-service -n rag-system
kubectl rollout undo deployment rag-auth-service -n rag-system --to-revision=2

# Complete teardown (CAUTION)
kubectl delete all --all -n rag-system
```

## Quick Deploy Commands

Once infrastructure is set up:

```bash
# Build and push images
make gcp-build ENV=dev

# Deploy services
make gcp-deploy ENV=dev

# Initialize database
make gcp-init-db ENV=dev

# Validate deployment
make gcp-validate ENV=dev

# Or all in one:
make gcp-deploy-all ENV=dev
```

## Current GCP Infrastructure

**Project**: `byo-rag-dev`  
**Region**: `us-central1`  
**Resources:**
- **GKE Cluster**: `rag-cluster-dev`
- **Cloud SQL**: `rag-postgres-dev` (private IP: 10.200.0.3)
- **Memorystore Redis**: `rag-redis-dev` (10.170.252.12)
- **Artifact Registry**: `us-central1-docker.pkg.dev/byo-rag-dev/rag-system`
- **Namespace**: `rag-system`

---

## Production Considerations

**Resource Sizing (Production):**
```yaml
# Update k8s/overlays/prod/kustomization.yaml
resources:
  requests:
    cpu: 1000m
    memory: 2Gi
  limits:
    cpu: 2000m
    memory: 4Gi
replicas: 3  # For HA
```

**Auto-Scaling:**
- HPA configured in `k8s/base/hpa.yaml`
- Target: 70% CPU, 75-80% memory
- Min: 2 replicas, Max: 8 replicas

**Backup Strategy:**
- **Cloud SQL:** Daily backups (7-day retention), point-in-time recovery
- **Documents:** Cloud Storage versioning
- **Kubernetes:** Velero for cluster backups

**Security Hardening:**
- ✅ Workload Identity enabled
- ✅ Secret Manager for secrets
- ✅ Cloud Armor WAF
- ⚠️ Change default admin password
- ⚠️ Rotate JWT secrets regularly

**Cost Optimization:**
- Use committed use discounts (30-50% savings)
- Enable cluster autoscaler
- Set up budget alerts

---

## Cross-Agent Communication

### Before Deployment

**Call test-agent** to verify all tests passing:
- Run full test suite
- Verify 100% pass rate
- Document test results

### After Deployment

**Call test-agent** to validate deployment:
- Run integration tests
- Verify end-to-end workflows
- Check health endpoints

**Call git-agent** to tag deployment:
```
Tag: deploy-dev-2025-11-12
Message: Deploy to GCP dev environment
- All services healthy
- Tests passing (600/600)
```

---

## Key Files Reference

**GCP Scripts:**
```
scripts/gcp/
├── 00-setup-project.sh              # GCP project creation
├── 01-setup-network.sh              # VPC, subnets
├── 02-setup-service-accounts.sh     # IAM service accounts
├── 03-setup-budget-alerts.sh        # Cost monitoring
├── 04-migrate-secrets.sh            # Secret Manager migration
├── 07-build-and-push-images.sh      # Local Docker build
├── 07a-cloud-build-images.sh        # Cloud Build
├── 08-setup-cloud-sql.sh            # PostgreSQL setup
├── 10-setup-memorystore.sh          # Redis setup
├── 12-setup-gke-cluster.sh          # GKE cluster
├── 13-sync-secrets-to-k8s.sh        # Secrets to K8s
├── 16-setup-ingress.sh              # Load balancer, SSL
├── 17-deploy-services.sh            # Deploy to GKE
├── 18-init-database.sh              # DB migrations
├── 19-validate-deployment.sh        # Validation
```

**Kubernetes Manifests:**
```
k8s/
├── base/                            # Base configurations
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── hpa.yaml
├── overlays/
│   ├── dev/                         # Dev environment
│   └── prod/                        # Production
```

**Documentation:**
```
docs/deployment/
├── GCP_DEPLOYMENT_GUIDE.md
├── INITIAL_DEPLOYMENT_GUIDE.md
├── GKE_CLUSTER_SETUP.md
├── CLOUD_SQL_SETUP.md
└── GCP_DEPLOYMENT_TROUBLESHOOTING.md
```

---

## Critical Reminders

**🚨 NEVER**:
- Deploy without tests passing (call test-agent first)
- Skip validation phase
- Deploy to production without staging validation
- Ignore health check failures

**✅ ALWAYS**:
- Verify tests pass before deployment
- Run validation script after deployment
- Monitor logs during deployment
- Tag successful deployments (via git-agent)
- Document deployment issues

---

## Resources

**See Also**:
- Main instructions: `../agent-instructions.md`
- Test agent: `./test-agent.md` (for pre-deployment validation)
- Git agent: `./git-agent.md` (for deployment tagging)
- GCP Deployment Guide: `docs/deployment/GCP_DEPLOYMENT_GUIDE.md`

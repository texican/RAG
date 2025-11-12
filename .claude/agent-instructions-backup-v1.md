# Agent Instructions for RAG System - Main Index

**Version**: 2.0.0  
**Last Updated**: 2025-11-12  
**Architecture**: Modular Sub-Agent System

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Sub-Agent Architecture](#sub-agent-architecture)
3. [Routing Decision Tree](#routing-decision-tree)
4. [Critical Project Rules](#critical-project-rules)
5. [Cross-Agent Communication](#cross-agent-communication)
6. [Quick Reference](#quick-reference)

---

## Project Overview

This is the **BYO (Build Your Own) RAG System** - an enterprise-grade Retrieval Augmented Generation platform built with Java 21, Spring Boot 3.2, and Spring AI.

**Current Version**: 0.8.0-SNAPSHOT  
**Status**: Production-ready with GCP deployment infrastructure  
**Test Coverage**: 594/600 functional tests passing (99%)

### System Architecture
- **6 Microservices**: Auth, Document, Embedding, Core (RAG), Admin
- **Multi-tenant**: Complete data isolation via tenant_id filtering
- **Event-Driven**: Kafka optional (disabled by default)
- **Containerized**: Docker Compose (local), GKE (production)
- **AI-Powered**: Supports OpenAI and local Ollama models

### Technology Stack
```
Core:       Java 21, Spring Boot 3.2.11, Spring AI 1.0.0-M1
Data:       PostgreSQL 42.7.3, Redis Stack 5.0.2
AI/ML:      LangChain4j 0.33.0, Ollama
Cloud:      GCP (GKE, Cloud SQL, Memorystore, Artifact Registry)
Testing:    JUnit 5.10.2, Testcontainers 1.19.8
```

### Database Architecture
- **PostgreSQL**: Shared `byo_rag_{env}` database
- **Redis**: Single instance with key prefixes `byo_rag_{env}:{service}:*`
- **Kafka**: OPTIONAL (disabled by default, ~$250-450/month savings)

---

## Sub-Agent Architecture

This agent system uses **specialized sub-agents** for different domains. Each sub-agent is an expert in its area with focused responsibilities.

### Available Sub-Agents

| Agent | Domain | File | Purpose |
|-------|--------|------|---------|
| **Test Agent** | Testing | `.claude/agents/test-agent.md` | Test execution, validation, quality gates |
| **Backlog Agent** | Backlog Mgmt | `.claude/agents/backlog-agent.md` | Story estimation, completion, sprint planning |
| **Deploy Agent** | Deployment | `.claude/agents/deploy-agent.md` | Local/GCP deployment, infrastructure |
| **Dev Agent** | Development | `.claude/agents/dev-agent.md` | Feature implementation, debugging |
| **Git Agent** | Version Control | `.claude/agents/git-agent.md` | Commits, branches, backups, tagging |

### How Sub-Agents Work

1. **Main Agent** receives user request
2. **Routing** - Determines which sub-agent(s) to invoke
3. **Delegation** - Invokes specialized sub-agent(s)
4. **Coordination** - Sub-agents may call other sub-agents
5. **Response** - Results aggregated and returned

**Benefits**:
- 60-80% reduction in context loading per task
- Faster response times (less content to process)
- Better accuracy (focused domain knowledge)
- Easier maintenance (domain ownership)

---

## Routing Decision Tree

### When to Use Which Agent

**Use Test Agent** when:
- "Run tests"
- "Check test coverage"
- "Analyze test failure"
- "Validate tests before story completion"
- "Run integration tests"

**Use Backlog Agent** when:
- "Estimate this story"
- "Complete this story"
- "Move story to completed"
- "Plan next sprint"
- "Update backlog"

**Use Deploy Agent** when:
- "Deploy to GCP"
- "Setup local environment"
- "Deploy services"
- "Validate deployment"
- "Fix deployment issue"

**Use Dev Agent** when:
- "Implement this feature"
- "Add REST endpoint"
- "Fix this bug"
- "Debug this service"
- "Add configuration"

**Use Git Agent** when:
- "Commit these changes"
- "Create a backup"
- "Tag this deployment"
- "Create a branch"

### Agent Dependencies

```
Main Agent
  ├─ Test Agent (can call: none - leaf agent)
  ├─ Backlog Agent (can call: test-agent, git-agent)
  ├─ Deploy Agent (can call: test-agent, git-agent)
  ├─ Dev Agent (can call: test-agent, git-agent)
  └─ Git Agent (can call: none - leaf agent)
```

**Architecture Details**: See `.claude/agents/README.md`

---

## Critical Project Rules

### Development Workflow

**ALWAYS Use Makefile Commands for Docker**:
```bash
# ✅ CORRECT: Rebuild service after code changes
make rebuild SERVICE=rag-auth

# ✅ View logs
make logs SERVICE=rag-auth

# ❌ NEVER: docker restart rag-auth (doesn't reload code)
```

**Service Names (Strict)**:
| Service | Container | Port |
|---------|-----------|------|
| Auth | `rag-auth` | 8081 |
| Document | `rag-document` | 8082 |
| Embedding | `rag-embedding` | 8083 |
| Core | `rag-core` | 8084 |
| Admin | `rag-admin` | 8085 |

### Configuration Profiles
# docker: Docker Compose deployment (kafka:29092, postgres:5432)
# gcp: Google Cloud Platform (Cloud SQL, Memorystore)
# test: Test execution (H2 in-memory)
```

**Profile activation in Docker**:
```yaml
environment:
  - SPRING_PROFILES_ACTIVE=docker
```

#### 4. Database Migration Strategy
- **Current**: `ddl-auto: update` (auto-schema updates)
- **Production Goal**: Flyway migrations (see TECH-DEBT-005)
- **Never use**: `ddl-auto: create-drop` (data loss!)

## File Organization & Patterns

### Maven Multi-Module Structure
```
pom.xml                    # Parent POM with dependency management
├── rag-shared/            # Common DTOs, entities, utilities
├── rag-auth-service/      # JWT auth, user/tenant management
├── rag-document-service/  # File upload, text extraction, chunking
├── rag-embedding-service/ # Vector generation, similarity search
├── rag-core-service/      # RAG pipeline, LLM integration
├── rag-admin-service/     # Admin operations, analytics
└── rag-integration-tests/ # E2E test suite
```

### Service Structure Pattern (Standard Spring Boot)
```
src/main/java/com/byo/rag/{service}/
├── {Service}Application.java   # Main class with @SpringBootApplication
├── config/                      # @Configuration classes
├── controller/                  # @RestController REST endpoints
├── service/                     # @Service business logic
├── repository/                  # @Repository JPA repositories
├── entity/                      # JPA entities (if service uses DB)
├── dto/                         # Request/Response DTOs
├── client/                      # Feign clients for inter-service calls
├── listener/                    # Kafka listeners (if enabled)
└── exception/                   # Custom exceptions

src/main/resources/
├── application.yml              # Configuration with profiles
└── db/migration/                # Flyway migrations (future)

src/test/java/
├── {package}/service/           # Unit tests (*Test.java)
└── integration/                 # Integration tests (*IT.java)
```

### Testing Naming Conventions
```
Unit Tests:           {ClassName}Test.java          (Surefire)
Integration Tests:    {Feature}IT.java              (Failsafe)
E2E Tests:           {Scenario}E2ETest.java         (Failsafe)
Infrastructure:      {Type}ValidationTest.java      (Surefire)
```

## Common Tasks - How To

### Adding a New REST Endpoint

1. **Create/Update Controller**:
```java
@RestController
@RequestMapping("/api/v1/myresource")
@RequiredArgsConstructor
public class MyResourceController {
    private final MyService myService;
    
    @PostMapping
    public ResponseEntity<MyDTO> create(@RequestBody MyDTO request) {
        return ResponseEntity.ok(myService.create(request));
    }
}
```

2. **Implement Service**:
```java
@Service
@RequiredArgsConstructor
public class MyService {
    private final MyRepository repository;
    
    @Transactional
    public MyDTO create(MyDTO dto) {
        // Business logic
    }
}
```

3. **Add Tests**:
```java
@SpringBootTest
class MyServiceTest {
    @Mock private MyRepository repository;
    @InjectMocks private MyService service;
    
    @Test
    void shouldCreateResource() {
        // Given, When, Then
    }
}
```

4. **Rebuild Service**:
```bash
make rebuild SERVICE=rag-{service}
```

### Adding a Configuration Property

1. **Add to application.yml**:
```yaml
myapp:
  my-feature:
    enabled: true
    timeout: 30s
```

2. **Create ConfigurationProperties class**:
```java
@ConfigurationProperties(prefix = "myapp.my-feature")
@Validated
public class MyFeatureConfig {
    private boolean enabled = true;
    private Duration timeout = Duration.ofSeconds(30);
    // getters/setters
}
```

3. **Enable in Application class**:
```java
@SpringBootApplication
@EnableConfigurationProperties(MyFeatureConfig.class)
public class MyApplication { }
```

### Debugging a Service

1. **Check logs**:
```bash
make logs SERVICE=rag-auth
```

2. **Check health**:
```bash
curl http://localhost:8081/actuator/health
```

3. **Access container**:
```bash
docker exec -it rag-auth sh
```

4. **Check database**:
```bash
docker exec -it rag-postgres psql -U rag_user -d byo_rag_local
```

5. **Check Redis**:
```bash
docker exec -it enterprise-rag-redis redis-cli
```

### Making Schema Changes

**Current (Until TECH-DEBT-005 Complete)**:
1. Modify JPA entity
2. Rebuild service
3. Schema updates automatically (ddl-auto: update)

**Future (With Flyway)**:
1. Create migration: `V{version}__{description}.sql`
2. Test migration locally
3. Rebuild service
4. Flyway applies on startup

## Code Standards & Patterns

### Tenant Isolation (Critical!)
Every query MUST filter by tenant_id:

```java
// ✅ CORRECT: Tenant-scoped query
documentRepository.findByTenantIdAndId(tenantId, documentId);

// ❌ WRONG: No tenant filtering (data leak!)
documentRepository.findById(documentId);
```

### Error Handling Pattern
```java
@Service
public class MyService {
    public MyDTO doSomething(UUID tenantId, UUID id) {
        return myRepository.findByTenantIdAndId(tenantId, id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "MyResource", id, tenantId));
    }
}

// Custom exceptions in rag-shared
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, UUID id, UUID tenantId) {
        super(String.format("%s not found: id=%s, tenant=%s", 
            resource, id, tenantId));
    }
}
```

### JWT Token Handling
```java
// Extract tenant from JWT claims
String tenantId = SecurityContextHolder.getContext()
    .getAuthentication()
    .getPrincipal()
    .getTenantId();

// Or from X-Tenant-ID header (fallback)
@RequestHeader("X-Tenant-ID") String tenantId
```

### Kafka Message Publishing (When Enabled)
```java
@Service
@ConditionalOnBean(KafkaTemplate.class)  // Only register if Kafka available
public class MyKafkaService {
    @Autowired(required = false)  // Optional injection
    private KafkaTemplate<String, MyEvent> kafkaTemplate;
    
    public void publishEvent(MyEvent event) {
        if (kafkaTemplate != null) {
            kafkaTemplate.send("my-topic", event);
        }
        // Fallback: process synchronously
    }
}
```

## Important Files & Documentation

### Must-Read Documents
1. **README.md** - Quick start, architecture overview, deployment
2. **BACKLOG.md** - Active stories, sprint planning, technical debt
3. **CONTRIBUTING.md** - Development workflow, Docker rules
4. **docs/development/DOCKER_DEVELOPMENT.md** - Detailed Docker guide
5. **docs/architecture/KAFKA_OPTIONAL.md** - Kafka configuration details

### Configuration References
- **Service configs**: `{service}/src/main/resources/application.yml`
- **Docker Compose**: `docker-compose.yml`
- **Kubernetes**: `k8s/base/*-deployment.yaml`
- **Maven dependencies**: Parent `pom.xml` (dependency management)

### Critical Documentation for Context
- **docs/testing/STORY-002_E2E_TEST_FINDINGS.md** - E2E test status
- **docs/implementation/STORY-018_IMPLEMENTATION_SUMMARY.md** - Kafka implementation
- **docs/operations/DATABASE_PERSISTENCE_FIX.md** - Database configuration
- **docs/operations/DEPLOYMENT_TROUBLESHOOTING.md** - K8s deployment issues

## GCP Deployment

### Architecture Overview

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

---

### Prerequisites

Before deploying to GCP, ensure you have:

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

---

### Phase 1: Infrastructure Setup (GCP-INFRA-001)

**Estimated Time:** 45-55 minutes | **Story Points:** 8

This phase creates the foundational GCP infrastructure. Run scripts in order:

#### Step 1: Project Setup (15-20 min)

```bash
cd /Users/stryfe/Projects/RAG
./scripts/gcp/00-setup-project.sh
```

**What it does:**
- Creates GCP project with unique ID
- Links billing account
- Enables 15+ required APIs (GKE, Cloud SQL, Artifact Registry, Secret Manager, etc.)
- Generates `config/project-config.env` with project configuration
- Creates detailed logs in `logs/gcp-setup/`

**Interactive prompts:**
- Project ID (must be globally unique, e.g., `byo-rag-dev`)
- Project Name (e.g., `BYO RAG System - Dev`)
- Region (default: `us-central1`)
- Zone (default: `us-central1-a`)
- Billing Account ID
- Monthly budget (default: $1000)
- Environment (dev/staging/production)

**Outputs:**
- `config/project-config.env` - Main configuration file
- `logs/gcp-setup/project-setup-*.log` - Detailed logs
- `logs/gcp-setup/setup-summary-*.txt` - Human-readable summary

#### Step 2: Network Setup (10-15 min)

```bash
./scripts/gcp/01-setup-network.sh
```

**What it does:**
- Creates custom VPC network (`rag-vpc`)
- Creates GKE subnet with secondary ranges:
  - Primary: `10.0.0.0/20` (4,096 IPs for nodes)
  - Pods: `10.4.0.0/14` (262,144 IPs)
  - Services: `10.8.0.0/20` (4,096 IPs)
- Configures firewall rules (internal, SSH via IAP, health checks)
- Sets up Cloud Router and Cloud NAT for private cluster egress
- Enables Private Service Access for Cloud SQL

**No user input required** - reads from `config/project-config.env`

#### Step 3: Service Accounts (5-10 min)

```bash
./scripts/gcp/02-setup-service-accounts.sh
```

**What it does:**
- Creates GKE node service account (`gke-node-sa`)
  - Roles: logging.logWriter, monitoring.metricWriter, artifactregistry.reader
- Creates Cloud SQL Proxy service account (`cloudsql-proxy-sa`)
  - Roles: cloudsql.client
- Creates Cloud Build service account (`cloud-build-sa`)
  - Roles: cloudbuild.builds.builder, artifactregistry.writer
- Exports service account keys to `config/service-account-keys/` (gitignored)

**Security Note:** Service account keys only for local development. Production uses Workload Identity.

#### Step 4: Budget Alerts (5-10 min)

```bash
./scripts/gcp/03-setup-budget-alerts.sh
```

**What it does:**
- Creates monthly budget with threshold alerts (50%, 75%, 90%, 100%)
- Configures email notifications
- Provides cost estimates for environment

**Interactive prompt:**
- Email address for budget alerts (optional)

**Budget Thresholds:**
- 50% - Early warning
- 75% - Review usage
- 90% - Take action
- 100% - Budget limit reached

---

### Phase 2: Secrets & Registry (GCP-SECRETS-002, GCP-REGISTRY-003)

**Estimated Time:** 1-2 hours | **Story Points:** 13

#### Step 5: Migrate Secrets to Secret Manager

```bash
./scripts/gcp/04-migrate-secrets.sh
```

**CRITICAL SECURITY:** This script:
1. Generates new secure passwords (DB, Redis, JWT secret)
2. Migrates secrets to GCP Secret Manager
3. Removes `.env` from git history (DESTRUCTIVE - creates backup branch first)
4. Updates `.gitignore` to prevent future leaks

**Secrets Created:**
- `postgres-password` - Generated 24-char password
- `redis-password` - Generated 24-char password
- `jwt-secret` - Generated 32-byte secret
- `openai-api-key` - Migrated from .env (rotated first!)

**Security Best Practices:**
1. Rotate OpenAI API key BEFORE migration
2. Backup repository before removing secrets from git history
3. Coordinate with team before force-pushing to remote
4. Verify all secrets migrated before deleting .env

#### Step 6: Setup Artifact Registry

```bash
# Create repository (if not exists)
gcloud artifacts repositories create rag-system \
  --repository-format=docker \
  --location=us-central1 \
  --description="BYO RAG System container images"

# Configure Docker authentication
gcloud auth configure-docker us-central1-docker.pkg.dev
```

---

### Phase 3: Build and Push Images

**Two approaches:** Local build or Cloud Build (Cloud Build recommended for production)

#### Option A: Local Build (Faster for Development)

```bash
./scripts/gcp/07-build-and-push-images.sh
```

**What it does:**
1. Builds all services via Maven: `./mvnw clean package -DskipTests`
2. Builds Docker images for 5 services
3. Tags images with:
   - Version tag (from `pom.xml`)
   - Git SHA (short commit hash)
   - `latest` tag
4. Pushes to Artifact Registry: `us-central1-docker.pkg.dev/${PROJECT_ID}/rag-system/`
5. Enables automatic vulnerability scanning

**Build single service:**
```bash
./scripts/gcp/07-build-and-push-images.sh --service rag-auth-service
```

**Skip build, only tag/push:**
```bash
./scripts/gcp/07-build-and-push-images.sh --skip-build
```

**Image naming pattern:**
```
us-central1-docker.pkg.dev/byo-rag-dev/rag-system/rag-auth-service:1.0.0
us-central1-docker.pkg.dev/byo-rag-dev/rag-system/rag-auth-service:a1b2c3d
us-central1-docker.pkg.dev/byo-rag-dev/rag-system/rag-auth-service:latest
```

#### Option B: Cloud Build (Recommended for Production)

```bash
./scripts/gcp/07a-cloud-build-images.sh --env dev
```

**Advantages over local build:**
- Correct architecture (x86) automatically - no ARM/Mac issues
- Faster parallel builds (E2_HIGHCPU_8 machine)
- No local resources consumed
- Integrated security scanning
- Build history and logs in Cloud Console
- Automatic retry on failure

**Build single service:**
```bash
./scripts/gcp/07a-cloud-build-images.sh --env dev --service rag-auth-service
```

**Manual Cloud Build trigger:**
```bash
gcloud builds submit --config=cloudbuild.yaml
```

**cloudbuild.yaml workflow:**
1. Build `rag-shared` module first (dependency for all services)
2. Build 5 services in parallel (each via Maven)
3. Build 5 Docker images in parallel
4. Push all images to Artifact Registry
5. Automatic vulnerability scanning

**Verify images:**
```bash
gcloud artifacts docker images list \
  us-central1-docker.pkg.dev/${PROJECT_ID}/rag-system

# View vulnerability scan results
gcloud artifacts docker images describe \
  us-central1-docker.pkg.dev/${PROJECT_ID}/rag-system/rag-auth-service:latest \
  --show-all-metadata
```

---

### Phase 4: Managed Infrastructure (GCP-SQL-004, GCP-REDIS-005)

**Estimated Time:** 2-4 hours | **Story Points:** 21

#### Step 7: Setup Cloud SQL PostgreSQL

```bash
./scripts/gcp/08-setup-cloud-sql.sh
```

**What it does:**
1. Creates PostgreSQL 15 instance with:
   - Regional HA (automatic failover)
   - pgvector extension enabled
   - Private IP only (no public IP)
   - Automated daily backups (7-day retention)
   - Point-in-time recovery
2. Creates database: `rag_enterprise`
3. Creates user: `rag_user` with generated password
4. Installs pgvector extension
5. Runs database migrations (Flyway or Spring Boot)

**Instance specs (dev):**
- Tier: db-custom-1-3840 (1 vCPU, 3.75GB RAM)
- Cost: ~$120/month

**Instance specs (prod):**
- Tier: db-custom-4-15360 (4 vCPU, 15GB RAM) with HA
- Cost: ~$480/month

**Verify Cloud SQL:**
```bash
gcloud sql instances describe rag-postgres-dev
gcloud sql databases list --instance=rag-postgres-dev
gcloud sql users list --instance=rag-postgres-dev
```

#### Step 8: Setup Cloud Memorystore Redis

```bash
./scripts/gcp/10-setup-memorystore.sh
```

**What it does:**
1. Creates Redis 7.0 instance (Standard tier for HA)
2. Configures private IP connection
3. Enables automatic failover
4. Sets up monitoring alerts

**Instance specs (dev):**
- Size: 5GB
- Tier: Standard (HA)
- Cost: ~$50/month

**Instance specs (prod):**
- Size: 20GB
- Tier: Standard (HA)
- Cost: ~$200/month

**Verify Redis:**
```bash
gcloud redis instances describe rag-redis-dev --region=us-central1

# Get connection info
export REDIS_HOST=$(gcloud redis instances describe rag-redis-dev \
  --region=us-central1 --format='get(host)')
export REDIS_PORT=$(gcloud redis instances describe rag-redis-dev \
  --region=us-central1 --format='get(port)')

echo "Redis: $REDIS_HOST:$REDIS_PORT"
```

---

### Phase 5: GKE Cluster Setup (GCP-GKE-007)

**Estimated Time:** 30-60 minutes | **Story Points:** 13

#### Step 9: Create GKE Cluster

```bash
./scripts/gcp/12-setup-gke-cluster.sh
```

**What it does:**
1. Creates regional GKE cluster (3 zones for HA)
2. Configures auto-scaling (1-5 nodes)
3. Enables Workload Identity
4. Configures VPC-native networking with secondary ranges
5. Installs cluster add-ons:
   - NGINX Ingress Controller
   - cert-manager for SSL
   - Secret Manager CSI driver
6. Creates namespace: `rag-system`
7. Configures Workload Identity bindings

**Cluster specs (dev):**
- Nodes: 2x n1-standard-4 (4 vCPU, 15GB RAM each)
- Auto-scaling: 1-5 nodes
- Cost: ~$220/month

**Cluster specs (prod):**
- Nodes: 6x n1-standard-4 (regional for HA)
- Auto-scaling: 3-10 nodes
- Cost: ~$900/month

**Get cluster credentials:**
```bash
gcloud container clusters get-credentials rag-cluster-dev \
  --region=us-central1 \
  --project=byo-rag-dev

# Verify access
kubectl cluster-info
kubectl get nodes
kubectl get namespaces
```

#### Step 10: Sync Secrets to Kubernetes

```bash
./scripts/gcp/13-sync-secrets-to-k8s.sh --env dev
```

**What it does:**
1. Pulls secrets from Secret Manager
2. Creates Kubernetes secrets in `rag-system` namespace
3. Configures Workload Identity for secret access
4. Verifies secret availability

**Secrets created:**
- `gcp-secrets` - All application secrets
- `postgres-credentials` - DB connection
- `redis-credentials` - Redis connection

**Verify secrets:**
```bash
kubectl get secrets -n rag-system

# Decode secret (for debugging)
kubectl get secret gcp-secrets -n rag-system -o jsonpath='{.data.jwt-secret}' | base64 -d
```

---

### Phase 6: Deploy Services (GCP-DEPLOY-011)

**Estimated Time:** 15-30 minutes | **Story Points:** 8

#### Step 11: Deploy Microservices to GKE

```bash
./scripts/gcp/17-deploy-services.sh --env dev
```

**What it does:**
1. Validates prerequisites (gcloud, kubectl, images exist)
2. Deploys services in dependency order:
   - rag-auth-service (authentication) - Port 8081
   - rag-admin-service (administration) - Port 8085
   - rag-document-service (document management) - Port 8082
   - rag-embedding-service (embeddings) - Port 8083
   - rag-core-service (RAG queries) - Port 8084
3. Waits for each deployment to become healthy (timeout: 10 min)
4. Validates health endpoints (`/actuator/health`)
5. Tests inter-service connectivity
6. Displays deployment status summary

**Deploy single service:**
```bash
./scripts/gcp/17-deploy-services.sh --env dev --service rag-auth
```

**Skip health checks (faster):**
```bash
./scripts/gcp/17-deploy-services.sh --env dev --skip-health-check
```

**Each pod includes:**
- Application container (Spring Boot service)
- Cloud SQL Proxy sidecar (secure database connection)
- Resource limits (CPU/memory)
- Liveness probe (60s delay, 10s timeout)
- Readiness probe (30s delay, 5s timeout)

**Service architecture:**
```
rag-auth-service:
  - Replicas: 1 (dev), 3 (prod)
  - Resources: 256Mi-512Mi RAM, 250m-500m CPU
  - Port: 8081
  - Dependencies: PostgreSQL, Redis

rag-document-service:
  - Replicas: 1 (dev), 3 (prod)
  - Resources: 1Gi-2Gi RAM, 1000m-2000m CPU
  - Port: 8082
  - Dependencies: PostgreSQL, Redis, Cloud Storage

rag-embedding-service:
  - Replicas: 1 (dev), 3 (prod)
  - Resources: 2Gi-4Gi RAM, 1000m-2000m CPU
  - Port: 8083
  - Dependencies: PostgreSQL, Redis, OpenAI API

rag-core-service:
  - Replicas: 1 (dev), 3 (prod)
  - Resources: 1Gi-2Gi RAM, 1000m-2000m CPU
  - Port: 8084
  - Dependencies: PostgreSQL, Redis, all other services

rag-admin-service:
  - Replicas: 1 (dev), 3 (prod)
  - Resources: 512Mi-1Gi RAM, 500m-1000m CPU
  - Port: 8085
  - Dependencies: PostgreSQL, Redis
```

**Horizontal Pod Autoscaler (HPA):**
- Enabled by default (configured in `k8s/base/hpa.yaml`)
- Target CPU: 70%
- Target Memory: 75-80%
- Min replicas: 2
- Max replicas: 8
- Scale-up: 30s evaluation period
- Scale-down: 5m cooldown

**Monitor deployment:**
```bash
# Watch pods start
kubectl get pods -n rag-system -w

# Check deployment status
kubectl rollout status deployment -n rag-system

# View pod details
kubectl describe pod <pod-name> -n rag-system

# Check logs
kubectl logs -n rag-system -l app=rag-auth --tail=100 -f

# Check events
kubectl get events -n rag-system --sort-by='.lastTimestamp'
```

#### Step 12: Initialize Database

```bash
./scripts/gcp/18-init-database.sh --env dev
```

**What it does:**
1. Tests Cloud SQL connectivity via Cloud SQL Proxy
2. Runs Flyway database migrations (creates tables, indexes)
3. Creates default tenant
4. Creates ADMIN role
5. Creates admin user: `admin@enterprise-rag.com` with password `admin123`
6. Assigns ADMIN role to admin user
7. Verifies admin user setup
8. Tests authentication

**Expected output:**
```
[SUCCESS] Database connectivity verified
[SUCCESS] Database migrations completed successfully
[SUCCESS] Admin user created

Admin Credentials:
  Email: admin@enterprise-rag.com
  Password: admin123

IMPORTANT: Change default password after first login!
```

**Verify admin user:**
```bash
# Get auth pod
AUTH_POD=$(kubectl get pods -n rag-system -l app=rag-auth -o jsonpath='{.items[0].metadata.name}')

# Query database
kubectl exec -n rag-system $AUTH_POD -- \
  sh -c "PGPASSWORD='<password>' psql -h 127.0.0.1 -U rag_user -d rag_db \
  -c 'SELECT email, is_active FROM users;'"
```

---

### Phase 7: Validation & Testing (GCP-VALIDATE-012)

**Estimated Time:** 10-20 minutes

#### Step 13: Comprehensive Deployment Validation

```bash
./scripts/gcp/19-validate-deployment.sh --env dev
```

**What it validates:**

1. **Pod Status** - All pods Running and Ready (1/1)
2. **Service Endpoints** - ClusterIP services have endpoints
3. **Health Checks** - Liveness and readiness probes passing
4. **Inter-Service Connectivity** - DNS resolution and HTTP communication
5. **Database Connectivity** - Cloud SQL accessible via proxy
6. **Redis Connectivity** - Memorystore accessible
7. **Resource Usage** - No OOMKilled pods, CPU/memory within limits
8. **Persistent Volumes** - PVCs bound and writable
9. **Admin Authentication** - Login works with default credentials
10. **Swagger UI** - API documentation accessible
11. **Integration Tests** - Optional Maven integration tests
12. **RAG Workflow** - End-to-end: upload document → query → results

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

**Quick validation (skip integration tests):**
```bash
./scripts/gcp/19-validate-deployment.sh --env dev --quick
```

---

### Accessing Services

#### Option A: Port Forwarding (Development)

```bash
# Auth service
kubectl port-forward -n rag-system svc/rag-auth-service 8081:8081 &

# Core service
kubectl port-forward -n rag-system svc/rag-core-service 8084:8084 &

# Document service
kubectl port-forward -n rag-system svc/rag-document-service 8082:8082 &
```

**Test endpoints:**
```bash
# Health check
curl http://localhost:8081/actuator/health

# Swagger UI
open http://localhost:8081/swagger-ui.html

# Login
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@enterprise-rag.com","password":"admin123"}'

# Save JWT token
export JWT_TOKEN="<token-from-response>"
```

#### Option B: Ingress with SSL (Production)

```bash
# Setup ingress with domain
./scripts/gcp/16-setup-ingress.sh \
  --env dev \
  --domain rag-dev.example.com

# Check ingress status
kubectl get ingress -n rag-system

# Get load balancer IP
kubectl get svc -n ingress-nginx

# Access via HTTPS
curl https://rag-dev.example.com/api/v1/auth/health
```

**Ingress configuration:**
- SSL/TLS via cert-manager (Let's Encrypt)
- Cloud Armor WAF protection
- DDoS protection
- Rate limiting
- Path-based routing to services

---

### Quick Deploy Commands

Once infrastructure is set up, use these shortcuts:

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

### Current GCP Infrastructure

**Project**: `byo-rag-dev`
**Region**: `us-central1`
**Resources:**
- **GKE Cluster**: `rag-cluster-dev`
- **Cloud SQL**: `rag-postgres-dev` (private IP: 10.200.0.3)
- **Memorystore Redis**: `rag-redis-dev` (10.170.252.12)
- **Artifact Registry**: `us-central1-docker.pkg.dev/byo-rag-dev/rag-system`
- **Namespace**: `rag-system`

---

### Common GCP Deployment Issues

#### Issue: Pods Stuck in Pending

**Symptom:** Pods show `Pending` status for > 5 minutes

**Diagnosis:**
```bash
kubectl describe pod -n rag-system <pod-name>
```

**Common causes:**

1. **Insufficient resources** - Node pool full
   ```bash
   kubectl top nodes
   kubectl describe nodes
   ```
   **Solution:** Scale up node pool or add nodes

2. **Image pull failure** - Cannot pull from Artifact Registry
   ```bash
   kubectl get events -n rag-system | grep "Failed to pull image"
   ```
   **Solution:** Verify images exist, check IAM permissions on Artifact Registry

3. **PVC not bound** - Persistent volume claim unbound
   ```bash
   kubectl get pvc -n rag-system
   ```
   **Solution:** Check StorageClass configuration, verify disk quota

#### Issue: Pods CrashLoopBackOff

**Symptom:** Pods repeatedly crash and restart

**Diagnosis:**
```bash
kubectl logs -n rag-system <pod-name> --previous
kubectl describe pod -n rag-system <pod-name>
```

**Common causes:**

1. **Database connection failure** - Cannot connect to Cloud SQL
   ```bash
   kubectl logs -n rag-system <pod-name> | grep "Connection refused"
   ```
   **Solution:** Verify Cloud SQL Proxy sidecar running, check secrets, verify Cloud SQL instance active

2. **Missing secrets** - Environment variables not set
   ```bash
   kubectl get secrets -n rag-system
   ```
   **Solution:** Run `./scripts/gcp/13-sync-secrets-to-k8s.sh`

3. **Application error** - Java exception during startup
   ```bash
   kubectl logs -n rag-system <pod-name> | grep "Exception"
   ```
   **Solution:** Review full logs, check configuration, verify dependencies

#### Issue: Service Not Accessible

**Symptom:** Cannot curl service endpoint

**Diagnosis:**
```bash
kubectl get svc -n rag-system
kubectl get endpoints -n rag-system
```

**Common causes:**

1. **No endpoints** - Service has no healthy pods
   ```bash
   kubectl get pods -n rag-system -l app=<service-name>
   ```
   **Solution:** Fix pod health issues first

2. **Wrong port** - Using incorrect service port
   ```bash
   kubectl describe svc <service-name> -n rag-system
   ```
   **Solution:** Verify port mappings (8081-8085)

3. **Network policy** - Blocking traffic
   ```bash
   kubectl get networkpolicies -n rag-system
   ```
   **Solution:** Review network policies, check firewall rules

#### Issue: Authentication Fails

**Symptom:** Login returns "Invalid credentials"

**Diagnosis:**
```bash
# Check admin user in database
kubectl exec -n rag-system $AUTH_POD -- \
  sh -c "PGPASSWORD='$DB_PASSWORD' psql -h 127.0.0.1 -U rag_user -d rag_db \
  -c 'SELECT email, is_active FROM users;'"
```

**Solution:** Run `./scripts/gcp/18-init-database.sh` to recreate admin user

---

### Rollback Procedures

#### Rollback Deployment

```bash
# Rollback specific service
kubectl rollout undo deployment rag-auth-service -n rag-system

# Rollback to specific revision
kubectl rollout history deployment rag-auth-service -n rag-system
kubectl rollout undo deployment rag-auth-service -n rag-system --to-revision=2

# Check rollout status
kubectl rollout status deployment rag-auth-service -n rag-system
```

#### Complete Teardown

```bash
# Delete all resources in namespace
kubectl delete all --all -n rag-system

# Delete secrets
kubectl delete secrets --all -n rag-system

# Delete PVCs (CAUTION: data loss)
kubectl delete pvc --all -n rag-system

# Delete namespace (CAUTION: complete removal)
kubectl delete namespace rag-system
```

---

### Production Considerations

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
- HPA already configured in `k8s/base/hpa.yaml`
- Target: 70% CPU, 75-80% memory
- Min: 2 replicas, Max: 8 replicas
- Scale-up: 30s, Scale-down: 5m cooldown

**Monitoring:**
```bash
# View Cloud Monitoring dashboards
gcloud monitoring dashboards list

# Create custom alerts
# See docs/operations/MONITORING_SETUP.md
```

**Backup Strategy:**
- **Cloud SQL:** Automated daily backups (7-day retention), point-in-time recovery
- **Documents:** Cloud Storage versioning, lifecycle policies (90-day retention)
- **Kubernetes:** Velero for cluster backups

**Security Hardening:**
- ✅ Workload Identity enabled
- ✅ Secret Manager for secrets
- ✅ Cloud Armor WAF
- ✅ Network policies (default deny)
- ✅ Pod security policies (restricted)
- ⚠️ Change default admin password
- ⚠️ Rotate JWT secrets regularly
- ⚠️ Enable audit logging

**Cost Optimization:**
- Use committed use discounts (30-50% savings)
- Enable cluster autoscaler
- Use preemptible VMs for non-critical workloads
- Set up budget alerts (already done in Phase 1)
- Review and right-size resources monthly

---

### Key Files Reference

**GCP Scripts (Numbered Deployment Order):**
```
scripts/gcp/
├── 00-setup-project.sh              # GCP project creation
├── 01-setup-network.sh              # VPC, subnets, Cloud NAT
├── 02-setup-service-accounts.sh     # IAM service accounts
├── 03-setup-budget-alerts.sh        # Cost monitoring
├── 04-migrate-secrets.sh            # Secret Manager migration
├── 07-build-and-push-images.sh      # Local Docker build
├── 07a-cloud-build-images.sh        # Cloud Build (recommended)
├── 08-setup-cloud-sql.sh            # PostgreSQL setup
├── 10-setup-memorystore.sh          # Redis setup
├── 12-setup-gke-cluster.sh          # GKE cluster creation
├── 13-sync-secrets-to-k8s.sh        # Secrets to K8s
├── 16-setup-ingress.sh              # Load balancer, SSL
├── 17-deploy-services.sh            # Deploy to GKE
├── 18-init-database.sh              # Database migrations, admin user
├── 19-validate-deployment.sh        # Comprehensive validation
└── README.md                        # Script documentation
```

**Kubernetes Manifests:**
```
k8s/
├── base/                            # Base configurations
│   ├── deployment.yaml              # Service deployments
│   ├── service.yaml                 # ClusterIP services
│   ├── hpa.yaml                     # Auto-scaling
│   └── kustomization.yaml
├── overlays/
│   ├── dev/                         # Dev environment
│   │   └── kustomization.yaml       # 1 replica, lower resources
│   └── prod/                        # Production environment
│       └── kustomization.yaml       # 3 replicas, higher resources
└── README.md                        # K8s deployment guide
```

**Cloud Build:**
```
cloudbuild.yaml                      # Main Cloud Build config
cloudbuild-single.yaml               # Single service build
cloudbuild-full.yaml                 # Full build with tests
```

**Documentation:**
```
docs/deployment/
├── GCP_DEPLOYMENT_GUIDE.md          # Comprehensive GCP guide
├── INITIAL_DEPLOYMENT_GUIDE.md      # First-time deployment
├── GKE_CLUSTER_SETUP.md             # GKE cluster details
├── CLOUD_SQL_SETUP.md               # Database setup
└── GCP_DEPLOYMENT_TROUBLESHOOTING.md # Common issues
```

For complete deployment details, see `docs/deployment/GCP_DEPLOYMENT_GUIDE.md` and `docs/deployment/INITIAL_DEPLOYMENT_GUIDE.md`.

---

## Coding and Testing Best Practices

### Code Quality Standards

#### Mandatory Quality Gates

**Definition of "Done" - NON-NEGOTIABLE:**
```
✅ ALL unit tests passing (100% - no exceptions)
✅ ALL integration tests passing (100% - no exceptions)
✅ ALL end-to-end workflows functional and tested
✅ ZERO broken functionality or HTTP 500 errors
✅ Production deployment tested and validated
✅ Performance benchmarks met (if applicable)
✅ Security validation complete
✅ Documentation updated to reflect changes
```

**🔴 CRITICAL: Never claim work is "done" or "complete" unless ALL criteria above are met.**

#### Test-First Protocol

**BEFORE claiming ANY functionality works:**
1. **Write tests first** - never claim something works without tests
2. **Run COMPLETE test suite** - not just unit tests
3. **Test integration scenarios** - Spring contexts must load
4. **Validate core workflows** - document upload, authentication, etc.
5. **Manual verification** - actually use the functionality

**Test Execution Requirements:**
```bash
# MANDATORY test commands before any status updates:
mvn clean test                           # All modules
mvn test -f rag-auth-service/pom.xml    # Individual services  
mvn test -f rag-document-service/pom.xml
mvn test -f rag-embedding-service/pom.xml
mvn test -f rag-core-service/pom.xml
mvn test -f rag-admin-service/pom.xml

# Integration testing
docker-compose up -d                     # Full environment
./scripts/test-system.sh                 # End-to-end validation
```

---

### Service Development Standards

#### Defensive Programming Principles

**The "Defense in Depth" Approach:**
1. **Input Validation**: Validate all inputs at method boundaries
2. **Null Safety**: Assume any reference could be null until proven otherwise  
3. **Early Returns**: Exit early for invalid states rather than continuing
4. **Graceful Degradation**: Provide sensible defaults when possible
5. **Resource Safety**: Always clean up resources and handle resource failures
6. **Thread Safety**: Avoid shared mutable state or protect it properly
7. **Error Recovery**: Implement fallback mechanisms for critical operations

#### Input Validation Patterns

**Basic Input Validation:**
```java
public void processRequest(String input) {
    // Fail fast with clear validation
    if (input == null || input.trim().isEmpty()) {
        return; // or throw IllegalArgumentException for strict validation
    }
    
    try {
        // Business logic
    } catch (Exception e) {
        logger.error("Failed to process request: {}", input, e);
        throw new RagException("Failed to process request: " + input, e);
    }
}
```

**Collection Input Validation:**
```java
public void processDocuments(List<SourceDocument> documents) {
    // Handle null collections defensively
    if (documents == null || documents.isEmpty()) {
        logger.debug("No documents provided for processing");
        return; // Early return with graceful degradation
    }
    
    // Validate collection contents
    List<SourceDocument> validDocuments = documents.stream()
        .filter(Objects::nonNull) // Remove null elements
        .filter(doc -> doc.relevanceScore() >= 0.0) // Business rule validation
        .collect(Collectors.toList());
        
    if (validDocuments.isEmpty()) {
        logger.warn("No valid documents found after filtering");
        return;
    }
    
    // Continue with validated data...
}
```

**Configuration Parameter Validation:**
```java
private void validateConfig(ContextConfig config) {
    if (config == null) {
        throw new IllegalArgumentException("Configuration cannot be null");
    }
    if (config.maxTokens() <= 0) {
        throw new IllegalArgumentException("Max tokens must be positive: " + config.maxTokens());
    }
    if (config.relevanceThreshold() < 0.0 || config.relevanceThreshold() > 1.0) {
        throw new IllegalArgumentException("Relevance threshold must be between 0.0 and 1.0: " + 
                                         config.relevanceThreshold());
    }
}
```

#### Error Handling Standards

**Consistent Exception Strategy:**
- **Use RagException consistently**: All service methods should throw `RagException` for business logic errors
- **Preserve original exceptions**: Always include the original exception as the cause
- **Meaningful error messages**: Include context about what operation failed and why

**Good Error Handling:**
```java
public Conversation getConversation(String conversationId) {
    try {
        // Validate input
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Conversation ID cannot be null or empty");
        }
        
        // Business logic
        return conversation;
    } catch (Exception e) {
        logger.error("Failed to retrieve conversation: {}", conversationId, e);
        throw new RagException("Failed to retrieve conversation: " + conversationId, e);
    }
}
```

**Bad Error Handling:**
```java
public Conversation getConversation(String conversationId) {
    try {
        // logic
        return conversation;
    } catch (Exception e) {
        logger.error("Error retrieving conversation", e);
        return null; // ❌ Inconsistent return type - hides errors!
    }
}
```

#### Service Method Checklist

✅ **Defensive Programming:**
- [ ] All inputs validated at method entry (null checks, range validation)
- [ ] Collections checked for null and empty states
- [ ] Strings validated for null, empty, and whitespace-only
- [ ] Configuration parameters validated for business rules
- [ ] Thread-safe operations (no shared mutable state issues)

✅ **Method Design:**
- [ ] Throws RagException for business logic errors consistently
- [ ] Returns expected type consistently (no mixed null/false returns)
- [ ] Includes meaningful error messages with context
- [ ] Follows single responsibility principle
- [ ] Has clear preconditions and postconditions

✅ **Exception Handling:**  
- [ ] Catches specific exceptions when possible (avoid catching Exception)
- [ ] Preserves original exception as cause in wrapped exceptions
- [ ] Logs error with appropriate level and sufficient context
- [ ] Provides actionable error messages for debugging
- [ ] Implements graceful degradation where appropriate
- [ ] Uses fallback mechanisms for critical operations

---

### Testing Best Practices

#### Test Design Standards

**MANDATORY for all new tests:**
- [ ] **Single Approach**: Use EITHER reflection OR public API, never both
- [ ] **Public API First**: Test through public methods unless testing internal state
- [ ] **Clear Intent**: Test names must clearly describe expected behavior
- [ ] **Realistic Data**: Use production-representative test data
- [ ] **Proper Assertions**: Validate business logic, not implementation artifacts
- [ ] **Documentation**: Document what behavior is being validated and why
- [ ] **✅ ALL TESTS MUST PASS**: A test class is NOT complete until all tests pass without errors or failures

**Good Test Example:**
```java
@Test
@DisplayName("Should truncate context when single document exceeds token limit")
void assembleContext_SingleDocumentExceedsLimit_TruncatesContent() {
    // Clear test intent, tests public API
    int maxTokens = 100;
    ContextConfig config = new ContextConfig(maxTokens, 0.7, true, "\n---\n");
    
    String result = service.assembleContext(documents, request, config);
    
    int estimatedTokens = result.length() / 4;
    assertThat(estimatedTokens)
        .describedAs("Context should respect token limit of %d", maxTokens)
        .isLessThanOrEqualTo(maxTokens);
}
```

**Bad Test Example:**
```java
@Test
void assembleContext_MaxLengthLimit_TruncatesContext() {
    // BAD: Using both reflection AND config
    ReflectionTestUtils.setField(service, "maxTokens", 100);
    ContextConfig config = new ContextConfig(100, 0.7, true, "\n---\n");
    
    String result = service.assembleContext(documents, request, config);
    
    // BAD: Testing implementation detail (character count)
    assertTrue(result.length() <= 500);
}
```

#### Test Naming Conventions

**Unit Tests (Surefire plugin):**
- **Pattern**: `{ClassName}Test.java`
- **Purpose**: Test individual classes/methods in isolation with mocks
- **Location**: `src/test/java` in same package as class under test
- **Execution**: `mvn test`
- **Examples**:
  - `DocumentServiceTest.java` - tests `DocumentService.java`
  - `RagControllerTest.java` - tests `RagController.java`

**Integration Tests (Failsafe plugin):**
- **Pattern**: `{Feature}IT.java` or `{Component}IntegrationTest.java`
- **Purpose**: Test component interactions with real dependencies (DB, messaging)
- **Location**: `src/test/java` in integration-specific packages
- **Execution**: `mvn verify` or `mvn integration-test`
- **Examples**:
  - `DocumentUploadProcessingIT.java` - tests document upload flow
  - `EmbeddingRepositoryIntegrationTest.java` - tests repository with real DB

**End-to-End Tests:**
- **Pattern**: `{Scenario}E2ETest.java` or `{Feature}EndToEndIT.java`
- **Purpose**: Test complete user journeys across all services
- **Location**: `rag-integration-tests/src/test/java/com/byo/rag/integration/endtoend/`
- **Execution**: `mvn verify -Pintegration-tests`

**Method Naming Standards:**
- Unit tests: `methodName_condition_expectedBehavior`
- Integration tests: `feature_scenario_expectedOutcome`
- Example: `assembleContext_SingleDocumentExceedsLimit_TruncatesContent`

#### Test Categories and Organization

**Validation Tests**: `{Component}ValidationTest.java`
- `ApiEndpointValidationTest.java`
- `InfrastructureValidationTest.java`

**Security Tests**: `{Component}SecurityTest.java`
- `GatewayAuthenticationSecurityTest.java`
- `SecurityConfigurationTest.java`

**Performance Tests**: `{Feature}LoadTest.java` or `{Feature}PerformanceTest.java`
- `PerformanceLoadTest.java`
- `MemoryOptimizationTest.java`

#### Parameterized Testing for Boundary Conditions

```java
@ParameterizedTest
@ValueSource(ints = {1, 50, 100, 500, 1000, 4000})
@DisplayName("Should respect various token limits consistently")
void assembleContext_VariousTokenLimits_AlwaysRespectsLimit(int tokenLimit) {
    ContextConfig config = new ContextConfig(tokenLimit, 0.7, true, "\n---\n");
    String result = service.assembleContext(createLongDocuments(), request, config);
    
    int estimatedTokens = result.length() / 4;
    assertThat(estimatedTokens)
        .describedAs("Context should respect token limit of %d", tokenLimit)
        .isLessThanOrEqualTo(tokenLimit);
}
```

#### Testing Requirements Checklist

**For Every Test Class:**
- [ ] Tests happy path scenarios with valid inputs
- [ ] Tests error scenarios with exception verification
- [ ] Tests edge cases (null, empty, invalid inputs)
- [ ] Tests boundary conditions (min/max values, limits)
- [ ] Verifies proper error message format and content
- [ ] Tests thread safety if applicable
- [ ] Tests fallback and retry mechanisms
- [ ] **ALL tests pass without failures or errors**

---

### Code Review Requirements

**Every PR must validate:**
- [ ] **No Mixed Testing Approaches**: Reflection and public API not used together
- [ ] **Boundary Logic**: Any `if` conditions with item counts/positions reviewed
- [ ] **Configuration Overrides**: Any method accepting config parameters tested properly
- [ ] **Error Messages**: Assertions include descriptive failure messages
- [ ] **Thread Safety**: Service methods are thread-safe for concurrent use
- [ ] **✅ ALL TESTS PASS**: Test suites run successfully with 0 failures and 0 errors

**Code Review Checklist Template:**
```markdown
## Service Logic Review
- [ ] Edge cases handled (null, empty, boundary values)
- [ ] Logging matches actual behavior
- [ ] Configuration overrides work correctly
- [ ] Resource limits enforced consistently

## Test Quality Review
- [ ] Tests use single approach (API or reflection, not both)
- [ ] Assertions validate business logic, not implementation
- [ ] Test names clearly describe expected behavior
- [ ] Realistic test data used
- [ ] Descriptive error messages included
- [ ] All tests pass without failures or errors
```

---

### Transparent Reporting Protocol

**REQUIRED Status Update Format:**
```markdown
## Test Results Summary
✅ **Passing Tests**: X/Y total (Z%)
❌ **Failing Tests**: N failures
- Auth Service: X/Y passing  
- Document Service: X/Y passing
- Embedding Service: X/Y passing
- Core Service: X/Y passing
- Admin Service: X/Y passing

## 🔧 Broken Functionality
- [Specific list of non-working features]
- [HTTP errors and endpoints affected]
- [Failed workflows with details]

## 🚫 Production Blockers
- [What prevents production deployment]
- [Critical issues requiring resolution]
- [Missing functionality]

## ✅ Verified Working
- [Only list functionality that has been tested]
- [Include test evidence/commands]
```

**Communication Standards:**

**Always Lead With Problems:**
- **Start status updates with failures and blockers**
- **Quantify all test results explicitly**
- **Never minimize or dismiss test failures**
- **Call out broken workflows immediately**

**Forbidden Phrases:**
- ❌ "Mostly working"
- ❌ "Basic functionality works"  
- ❌ "Just configuration issues"
- ❌ "Production ready" (unless 100% validated)
- ❌ "Nearly complete"

**Required Phrases:**
- ✅ "X/Y tests passing"
- ✅ "Confirmed working via testing"
- ✅ "Blockers identified: [list]"
- ✅ "Requires resolution before proceeding"

---

### Story Completion Requirements

**Test Verification Tool:**
Use `scripts/tests/story-completion-test-check.sh <service-name>` to verify all tests pass before marking stories complete. This script is MANDATORY for any story affecting code.

**BEFORE marking any story as completed, verify:**

🔴 **MANDATORY TEST VERIFICATION (MUST BE FIRST):**
- [ ] **ALL TESTS PASSING**: Run `mvn test` for affected modules and verify 0 failures
- [ ] **COMPILATION SUCCESS**: Run `mvn compile` and verify no errors
- [ ] **TEST RESULTS DOCUMENTED**: Record actual test counts (X/Y tests passing)
- [ ] **NO FAILING TESTS EXCEPTION**: If ANY test fails, story CANNOT be marked complete

**Additional Verification:**
- [ ] All acceptance criteria have been met
- [ ] Definition of done is satisfied
- [ ] Documentation updates are included
- [ ] Integration tests pass (if applicable)
- [ ] Manual verification performed

---

### Critical Reminders

**🚨 NEVER:**
- Mix reflection and public API in the same test method
- Validate implementation details instead of business logic
- Submit test classes with failing tests
- Claim work is "done" with failing tests
- Make assumptions - test everything explicitly

**✅ ALWAYS:**
- Check boundary conditions in loops and conditionals (first item, last item, empty collections)
- Add descriptive assertions with expected vs actual values
- Test the public contract first, internal state validation second
- Document test intent with clear @DisplayName and method documentation
- Use realistic test data that mirrors production usage patterns
- Run full test suite before claiming completion
- Report test results accurately and completely

---

### Documentation Standards

**Required Service Method Documentation:**
```java
/**
 * Assembles coherent context from retrieved document chunks with token limiting.
 * 
 * @param documents list of retrieved documents with relevance scores
 * @param request the original RAG query request containing tenant and options
 * @param config configuration with token limits. MUST have maxTokens > 0
 * @return assembled context string within token limits
 * @throws IllegalArgumentException if config.maxTokens <= 0
 * @implNote Uses 4-character-per-token estimation for English text.
 *           First document will be truncated if it exceeds maxTokens limit.
 *           Subsequent documents are skipped if they would exceed limit.
 */
public String assembleContext(List<SourceDocument> documents, 
                            RagQueryRequest request, 
                            ContextConfig config) {
```

**Required Test Method Documentation:**
```java
/**
 * Validates that the service respects token limits by:
 * 1. Creating a document that would exceed 100 token limit (400+ chars)
 * 2. Configuring ContextConfig with maxTokens=100
 * 3. Verifying returned context stays within token budget using 4-char estimation
 * 
 * This test ensures the token limiting logic works correctly for large documents
 * and prevents LLM context overflow in production scenarios.
 */
@Test
@DisplayName("Should truncate context when single document exceeds token limit")
void assembleContext_SingleDocumentExceedsLimit_TruncatesContent() {
```

---

### Testing Resources

**Key Documentation:**
- `QUALITY_STANDARDS.md` - Enterprise quality gates and compliance
- `CONTRIBUTING.md` - Development workflow and practices
- `docs/development/TESTING_BEST_PRACTICES.md` - Comprehensive testing guide
- `docs/development/ERROR_HANDLING_GUIDELINES.md` - Error handling patterns
- `docs/development/METHODOLOGY.md` - Story management and processes
- `docs/testing/TEST_RESULTS_SUMMARY.md` - Current test status

**Test Execution:**
```bash
# All tests
mvn clean test

# Specific service
mvn test -f rag-auth-service/pom.xml

# Integration tests
mvn verify

# With coverage
mvn test jacoco:report
```

## Common Issues & Solutions

### Issue: Code changes not reflected
```bash
# Try rebuilding without cache
make rebuild-nc SERVICE=rag-auth

# Check JAR timestamp
ls -lh rag-auth-service/target/*.jar

# Verify container using correct image
docker ps --format "{{.Image}} {{.Names}}" | grep rag-auth
```

### Issue: Service won't start
```bash
# Check logs
make logs SERVICE=rag-auth

# Check health
curl http://localhost:8081/actuator/health

# Check dependencies (PostgreSQL, Redis, Kafka if enabled)
docker-compose ps
```

### Issue: Tenant not found errors
- Ensure tenant exists in shared database `byo_rag_local`
- Create admin user: `make create-admin-user`
- Verify tenant_id in JWT token matches database

### Issue: Tests failing
```bash
# Run specific service tests
make test SERVICE=rag-auth

# Run with specific profile
mvn test -Dspring.profiles.active=test

# Check for known issues in BACKLOG.md (TECH-DEBT-006, TECH-DEBT-007)
```

## Communication and Honesty Standards

### Core Principle: Always Lead With Problems

**The Golden Rule**: Start every status update with failures, blockers, and broken functionality. Never bury problems in optimistic summaries.

**Why This Matters**:
- Engineering decisions depend on accurate status
- Test failures indicate real issues, not "minor problems"
- "Nearly complete" creates false expectations
- Lies of omission are still lies

### Defining Honesty

#### What Constitutes Lying

**Explicit Lies**: Making false statements about status, test results, or functionality
- ❌ "All tests pass" when tests are failing
- ❌ "Service is running" when container is in CrashLoopBackOff
- ❌ "Feature is complete" when requirements aren't met

**Lies of Omission**: Withholding critical context that changes interpretation
- ❌ Reporting "94% test pass rate" without mentioning 40 tests failing
- ❌ Saying "Service deployed successfully" without mentioning health checks failing
- ❌ Claiming "Feature works" without mentioning it only works for one specific scenario
- ❌ Stating "Configuration updated" without mentioning it broke existing functionality
- ❌ Reporting "Database migration complete" without mentioning data validation errors

**Misleading Framing**: Technically true but creates wrong impression
- ❌ "Mostly working" (implies production-ready when it's not)
- ❌ "Just configuration issues" (minimizes real problems)
- ❌ "Basic functionality works" (ignores critical features broken)
- ❌ "Nearly complete" (hides significant remaining work)

#### What Honesty Requires

**Complete Context**: Every status update must include
- ✅ Exact test counts: "561/571 tests passing" not "most tests pass"
- ✅ Explicit failures: List what's broken, not what works
- ✅ Blockers identified: Name specific issues preventing progress
- ✅ Impact assessment: Who/what is affected by failures
- ✅ Dependencies: What depends on broken components

**Accurate Severity**: Use precise language
- ✅ "Critical blocker" for deployment-stopping issues
- ✅ "Production blocker" for issues preventing release
- ✅ "Known issue" for documented technical debt
- ✅ "Partial implementation" for incomplete features
- ✅ "Requires resolution" for mandatory fixes

**Verified Claims**: Never claim without proof
- ✅ "Confirmed working via testing" + show test results
- ✅ "Validated against checklist" + reference checklist
- ✅ "Health checks passing" + show actual health check output
- ✅ "Feature complete" + show all acceptance criteria met

### Transparent Reporting Protocol

#### Required Status Update Format

Every status update must follow this structure:

```markdown
## Status Update: [Component/Feature Name]

### Test Results
- **Total Tests**: X/Y passing (Z% pass rate)
- **Failures**: [specific count] tests failing
- **Test Details**: [link to test output or summary]

### Broken Functionality
- [Explicit list of what does NOT work]
- [Impact of each failure]
- [Services/features affected]

### Production Blockers
- [List of issues preventing production deployment]
- [Dependencies blocked by these issues]
- [Estimated effort to resolve]

### Working Functionality
- [List of verified working features]
- [How verification was performed]
- [Limitations/caveats]

### Next Steps
- [Specific actions to resolve blockers]
- [Priorities and dependencies]
- [Who needs to be involved]
```

#### Forbidden Phrases

**NEVER use these phrases** - they mislead and hide problems:

1. ❌ **"Mostly working"**
   - Why forbidden: Hides percentage of failures
   - Use instead: "X/Y tests passing, Z components working, [list] broken"

2. ❌ **"Basic functionality works"**
   - Why forbidden: Implies production-ready when critical features missing
   - Use instead: "Core features [list] working, advanced features [list] not implemented"

3. ❌ **"Just configuration issues"**
   - Why forbidden: Minimizes real problems that block deployment
   - Use instead: "Configuration blockers: [specific list with impact]"

4. ❌ **"Production ready"** (unless 100% validated)
   - Why forbidden: Implies no issues when thorough validation not done
   - Use instead: "Production ready: [checklist with all items confirmed]" OR "Not production ready: [blockers]"

5. ❌ **"Nearly complete"**
   - Why forbidden: Hides actual percentage of completion
   - Use instead: "X% complete (Y/Z items done), remaining: [specific list]"

#### Required Phrases

**ALWAYS use these phrases** - they provide accurate context:

1. ✅ **"X/Y tests passing"**
   - Provides exact pass rate
   - Shows absolute numbers, not percentages only
   - Example: "561/571 tests passing (98.2%)"

2. ✅ **"Confirmed working via testing"**
   - Proves functionality, not assumptions
   - Shows verification method
   - Example: "Document upload confirmed working via integration test #42"

3. ✅ **"Blockers identified: [list]"**
   - Makes blockers explicit and visible
   - Prevents surprises later
   - Example: "Blockers identified: Cloud SQL connection timeout, Redis auth failing"

4. ✅ **"Requires resolution before proceeding"**
   - Establishes hard dependencies
   - Prevents premature advancement
   - Example: "Database migration failure requires resolution before deploying to staging"

### Context Requirements

#### For Problems

When reporting problems, always provide:

**1. What's Broken**
- Specific component/feature/test name
- Exact error message or failure mode
- How to reproduce the issue

**2. Impact Assessment**
- Who is affected (developers, users, deployments)
- What functionality is blocked
- What depends on the broken component

**3. Investigation Status**
- What debugging has been done
- What causes have been ruled out
- What suspects remain

**4. Next Steps**
- Immediate actions needed
- Who needs to be involved
- Estimated effort/timeline

**Example - Good Problem Report**:
```markdown
### Problem: Embedding Service Container Failing

**What's Broken:**
- Container: rag-embedding exits with code 137 (OOM killed)
- Test: EmbeddingGenerationIntegrationTest.testLargeDocumentEmbedding failing
- Error: "java.lang.OutOfMemoryError: Java heap space"

**Impact:**
- Blocks document processing for documents >10MB
- Affects 3 integration tests (all document upload workflows)
- Prevents deployment to staging (production blocker)

**Investigation:**
- Heap dump shows 2GB allocated to embedding vectors
- Default JVM heap set to 512MB in Dockerfile
- Ollama model requires 1.5GB minimum per request
- No memory leak detected (verified with jmap)

**Next Steps:**
1. Increase heap to 3GB in Dockerfile (immediate fix)
2. Implement embedding batch processing for large docs (prevents future OOM)
3. Add memory monitoring alerts (prevents recurrence)
4. Update resource limits in k8s manifests
5. Re-run integration tests to verify fix

**Estimated Effort:** 2-3 hours to fix + test
```

#### For Solutions

When reporting solutions, always provide:

**1. What Was Fixed**
- Specific issue resolved
- Root cause identified
- Changes made to fix it

**2. Verification Performed**
- Tests run to confirm fix
- Manual validation done
- Edge cases checked

**3. Potential Side Effects**
- What else might be impacted
- What to monitor for regressions
- Related components to check

**4. Documentation Updated**
- Configuration changes
- Architecture changes
- Runbook updates

**Example - Good Solution Report**:
```markdown
### Solution: Embedding Service OOM Resolved

**What Was Fixed:**
- Increased JVM heap from 512MB to 3GB in Dockerfile
- Implemented batch processing for documents >5MB
- Added memory monitoring to /actuator/metrics

**Changes Made:**
- `rag-embedding-service/Dockerfile`: Updated JAVA_OPTS to `-Xmx3g -Xms1g`
- `EmbeddingService.java`: Added chunking for large documents (500 tokens per batch)
- `application.yml`: Added `management.metrics.memory.enabled=true`
- `k8s/base/embedding-deployment.yaml`: Updated resource limits to 4GB

**Verification Performed:**
- ✅ All 3 previously failing integration tests now pass
- ✅ Processed 50MB test document successfully
- ✅ Memory usage stable at 2.1GB under load
- ✅ No memory leaks detected after 1000 document batch
- ✅ Container runs 24 hours without restart
- ✅ Load test: 100 concurrent requests handled

**Test Results:** 571/571 tests passing (100%)

**Potential Side Effects:**
- Increased memory footprint (was 512MB, now 3GB)
- Slower processing for documents <1MB (batching overhead ~50ms)
- Monitor: Memory alerts if >3.5GB usage sustained

**Documentation Updated:**
- ✅ Updated `docs/deployment/RESOURCE_SIZING.md` with new requirements
- ✅ Added troubleshooting section to `docs/operations/EMBEDDING_SERVICE.md`
- ✅ Updated `docker-compose.yml` memory limits
- ✅ Updated `README.md` system requirements (6GB RAM → 8GB RAM)

**Production Ready:** Yes
- All tests passing
- Load tested successfully
- Documentation complete
- Runbook updated
```

### Communication Standards

#### Test Results

**ALWAYS quantify test results explicitly**:
- ✅ "594/600 tests passing (99%)"
- ✅ "6 tests failing in rag-core-service (known Redis issues)"
- ❌ "Tests mostly pass"
- ❌ "Most tests green"

**NEVER minimize test failures**:
- ✅ "12 integration tests failing - deployment blocked"
- ✅ "Critical: Authentication test failing - no users can login"
- ❌ "Just a few test failures"
- ❌ "Minor test issues"

#### Service Status

**ALWAYS check health endpoints before claiming services work**:
```bash
# Verify BEFORE claiming "service running"
curl http://localhost:8081/actuator/health

# Report actual status
✅ "Service health: UP, readiness: UP, liveness: UP"
❌ "Service is running" (without verification)
```

**ALWAYS report pod/container status accurately**:
- ✅ "Pod status: CrashLoopBackOff - database connection failing"
- ✅ "Container running but health checks failing (not ready)"
- ❌ "Container is up"
- ❌ "Deployment successful" (when pods not ready)

#### Deployment Status

**NEVER claim production-ready without validation**:

Must verify ALL 7 criteria:
1. ✅ All tests passing (100% pass rate)
2. ✅ All services healthy in target environment
3. ✅ Integration tests passing against deployed services
4. ✅ Security scan completed (no critical vulnerabilities)
5. ✅ Performance testing completed (meets SLAs)
6. ✅ Documentation updated (runbooks, architecture diagrams)
7. ✅ Rollback procedure tested

**Only then** claim: "Production Ready: All 7 criteria verified"

#### Work Completion

**ALWAYS lead with what's NOT done**:
- ✅ "Blockers: [list], Remaining work: [list], Completed: [list]"
- ❌ "Completed: [list], Just need to finish: [list]"

**ALWAYS quantify remaining work**:
- ✅ "60% complete (6/10 features), 4 features remaining: [list]"
- ❌ "Almost done, just a few more things"

### Status Reporting Frequency

**Daily Updates** (for active work):
- Current progress with exact percentages
- New blockers identified
- Test results from day's work
- Next day's plan

**Immediate Notification** (for critical issues):
- Any test failure introduced by new code
- Any service going unhealthy
- Any deployment failure
- Any production incident

**After Every Significant Change**:
- Complete test results (not just diff)
- Impact assessment
- Verification performed
- Related components checked

**End of Task Summary**:
- Full validation checklist
- All test results
- All documentation updated
- Blockers resolved
- Handoff notes

### Examples: Good vs Bad Communication

#### Example 1: Deployment Update

**❌ Bad**:
```
Deployed to GKE. Services are running. Should be ready soon.
```

**Why Bad**: 
- No test results
- "Should be ready" is vague
- No verification shown
- Hides potential issues

**✅ Good**:
```
## GKE Deployment Status

**Deployment Executed:** 2025-01-15 14:23 UTC

**Pod Status:**
- ✅ rag-auth: 2/2 pods Running, Ready
- ✅ rag-document: 2/2 pods Running, Ready
- ✅ rag-embedding: 2/2 pods Running, Ready
- ❌ rag-core: 0/2 pods Running (CrashLoopBackOff)
- ✅ rag-admin: 2/2 pods Running, Ready

**Blocker Identified:**
- rag-core failing to connect to Redis
- Error: "NOAUTH Authentication required"
- Impact: Query functionality completely broken
- Root cause: Redis password not in Secret Manager

**Test Results:** Cannot run validation - rag-core down

**Next Steps:**
1. Add Redis password to Secret Manager (15 min)
2. Update k8s secret manifest (5 min)
3. Restart rag-core pods (2 min)
4. Run validation script (10 min)
5. Expected resolution: 30-45 minutes

**Production Ready:** No - core service down
```

#### Example 2: Feature Implementation

**❌ Bad**:
```
Document upload feature is mostly working. Just some edge cases to handle.
```

**Why Bad**:
- "Mostly working" forbidden phrase
- "Just some edge cases" minimizes issues
- No test results
- No specifics on what works/doesn't

**✅ Good**:
```
## Document Upload Feature Status

**Test Results:** 18/25 tests passing (72%)

**Working Functionality:**
- ✅ PDF upload (tested with 10MB files)
- ✅ DOCX upload (tested with 5MB files)
- ✅ TXT upload (tested with 1MB files)
- ✅ Metadata extraction for all formats
- ✅ Tenant isolation verified
- ✅ Authentication/authorization working

**Broken Functionality (7 tests failing):**
- ❌ Files >50MB fail with timeout (3 tests)
- ❌ Special characters in filename cause 500 error (2 tests)
- ❌ Concurrent uploads corrupt data (1 test)
- ❌ XLSX format not supported yet (1 test)

**Production Blockers:**
- Concurrent upload bug affects multi-user scenarios
- Filename validation missing (security issue)
- Large file timeout impacts enterprise users

**Next Steps:**
1. Fix concurrent upload lock mechanism (4 hours)
2. Add filename sanitization (2 hours)
3. Implement streaming for large files (6 hours)
4. Add XLSX support (optional, can defer)

**Estimated to Production Ready:** 12-16 hours work + testing
```

#### Example 3: Bug Fix

**❌ Bad**:
```
Fixed the Redis connection issue. Everything should work now.
```

**Why Bad**:
- "Should work" shows no verification
- No explanation of fix
- No test results
- No verification of related functionality

**✅ Good**:
```
## Bug Fix: Redis Connection Timeout

**Root Cause Identified:**
- Redis client using default 2-second timeout
- Cloud Memorystore latency averages 3-4 seconds under load
- Connection pool exhaustion causing cascading failures

**Changes Made:**
- Updated `application.yml`: `spring.data.redis.timeout=10s`
- Increased connection pool: `min=10, max=50` (was `min=5, max=20`)
- Added connection retry logic with exponential backoff
- Added connection pool monitoring to `/actuator/metrics`

**Verification Performed:**
- ✅ All 47 Redis-dependent tests passing (was 12/47)
- ✅ Load test: 1000 concurrent requests, 0 timeouts
- ✅ Sustained load: 30-minute test, all requests <500ms
- ✅ Connection pool metrics stable (30-35 connections used)
- ✅ No connection leaks detected

**Test Results:** 594/600 tests passing (99%)
- 6 failures are existing known issues (TECH-DEBT-006, TECH-DEBT-007)
- This fix resolved 35 previously failing tests

**Related Components Verified:**
- ✅ rag-embedding-service: All caching working
- ✅ rag-core-service: Query response time improved 60%
- ✅ Integration tests: All Redis workflows passing

**Production Ready:** Yes for Redis connection fix
- All tests passing
- Load tested successfully
- Monitoring in place
- No regressions detected

**Documentation Updated:**
- ✅ `docs/operations/REDIS_CONFIGURATION.md` updated with new settings
- ✅ `docs/troubleshooting/REDIS_ISSUES.md` added timeout troubleshooting
- ✅ Runbook updated with connection pool monitoring alerts
```

### Summary: Communication Checklist

Before sending ANY status update, verify:

- [ ] Led with problems, failures, blockers (not successes)
- [ ] Quantified all test results exactly (X/Y passing)
- [ ] Listed what's broken explicitly (no minimizing)
- [ ] Identified production blockers clearly
- [ ] Showed verification for all claims
- [ ] Avoided all 5 forbidden phrases
- [ ] Used required phrases where applicable
- [ ] Provided complete context (not just highlights)
- [ ] Specified next steps with estimates
- [ ] Assessed impact on dependent components
- [ ] No lies of omission (all relevant context included)

## Backlog Management and Estimation

### Overview

The RAG project uses a comprehensive backlog management system with strict quality controls, clear definition of done, and consistent story point estimation methodology. Understanding and following these standards is **MANDATORY** for all development work.

### File Organization

**Critical Files**:
- **`BACKLOG.md`** - ONLY active/pending stories (never add completed stories here)
- **`docs/project-management/COMPLETED_STORIES.md`** - ALL completed stories with metadata
- **`docs/project-management/BACKLOG_MANAGEMENT_PROCESS.md`** - Comprehensive safety procedures
- **`docs/development/METHODOLOGY.md`** - Story point methodology and processes
- **`QUALITY_STANDARDS.md`** - Enterprise quality gates and compliance standards

**Key Principle**: Each file has ONE specific purpose. Never mix active and completed work.

### Story Point Estimation

#### Sizing Methodology (Pebbles, Rocks, Boulders)

The project uses **industry-standard story point anchoring**:

**Pebbles (1-3 points)**: Small, focused tasks
- **1 point**: 1-2 hours work
  - Bug fix with clear solution
  - Configuration change with testing
  - Simple documentation update
  - Example: Fix environment variable, update README section

- **2 points**: 4-6 hours work (half day)
  - Small feature addition
  - Test suite for single component
  - Documentation for new feature
  - Example: Add health check endpoint, write integration test

- **3 points**: 1-2 days work
  - Medium feature implementation
  - Refactoring single service
  - Comprehensive test coverage for feature
  - Example: Implement new API endpoint with tests, fix deployment issue

**Rocks (5-8 points)**: Medium anchor stories
- **5 points**: 3-4 days work
  - Significant feature implementation
  - Service-wide refactoring
  - Integration of new technology
  - Example: Implement Kafka optional architecture, add new service integration

- **8 points**: 5-7 days work (1 week)
  - Complex feature with multiple components
  - Architecture changes affecting multiple services
  - Comprehensive security implementation
  - Example: Implement rate limiting across all services, complete Docker deployment

**Boulders (13+ points)**: Large epics requiring breakdown
- **13 points**: 2 weeks work
  - Major architectural change
  - Multi-service feature implementation
  - Complete subsystem redesign
  - Example: Migrate to new database, implement complete monitoring stack
  - **NOTE**: Should be broken down into smaller stories when possible

- **21+ points**: Epic-level work (1 month+)
  - Fundamental system redesign
  - Multiple interdependent features
  - **ALWAYS requires breakdown** into smaller stories
  - Example: Complete GCP migration, new authentication system

#### Estimation Guidelines

**Story Point Reflects Complexity, NOT Time**:
- Technical complexity (architecture, algorithms, integrations)
- Uncertainty and unknowns (research, experimentation)
- Testing requirements (unit, integration, E2E)
- Documentation needs (API docs, runbooks, guides)
- Risk factors (breaking changes, dependencies)

**Avoid These Estimation Mistakes**:
- ❌ Estimating based solely on coding time
- ❌ Ignoring testing and documentation time
- ❌ Not accounting for unknown unknowns
- ❌ Forgetting integration and deployment complexity
- ❌ Underestimating review and revision cycles

**Good Estimation Practices**:
- ✅ Consider full development lifecycle (code + test + doc + deploy)
- ✅ Account for unknown complexity (add buffer)
- ✅ Include integration testing time
- ✅ Factor in review and revision cycles
- ✅ Compare to similar previously completed stories
- ✅ Break down boulders (13+ points) into smaller stories

### Definition of Done (Mandatory)

**A story is NEVER complete unless ALL 8 criteria are met**:

1. ✅ **ALL unit tests passing (100%)** - Zero exceptions allowed
2. ✅ **ALL integration tests passing (100%)** - Zero exceptions allowed
3. ✅ **ALL end-to-end workflows functional** - Tested and validated
4. ✅ **ZERO broken functionality** - No HTTP 500 errors, no crashes
5. ✅ **Production deployment tested** - Verified in target environment
6. ✅ **Performance benchmarks met** - If applicable to story
7. ✅ **Security validation complete** - No vulnerabilities introduced
8. ✅ **Documentation updated** - Code, API, deployment docs current

**Test-First Protocol (Mandatory)**:

Before claiming ANY functionality works:
1. Write tests FIRST (never claim something works without tests)
2. Run COMPLETE test suite (not just unit tests)
3. Test integration scenarios (Spring contexts must load)
4. Validate core workflows (document upload, auth, query, etc.)
5. Manual verification (actually use the functionality)

**Required Test Commands**:
```bash
# MANDATORY before marking story complete:
mvn clean test                           # All modules
mvn test -f rag-auth-service/pom.xml    # Individual services
mvn test -f rag-document-service/pom.xml
mvn test -f rag-embedding-service/pom.xml
mvn test -f rag-core-service/pom.xml
mvn test -f rag-admin-service/pom.xml

# Integration testing
docker-compose up -d                     # Full environment
make test-all                            # Complete test suite
```

**Critical Rule**: If ANY test fails, story CANNOT be marked complete. Period.

### Story Completion Process

#### Pre-Completion Checklist (MANDATORY)

**🔴 Test Verification (MUST BE FIRST)**:
- [ ] ALL tests passing - `mvn test` shows 0 failures
- [ ] Compilation successful - `mvn compile` shows no errors
- [ ] Test results documented - Record "X/Y tests passing"
- [ ] NO failing test exceptions - Even 1 failure blocks completion

**📋 Completion Evidence**:
- [ ] All acceptance criteria implemented
- [ ] All definition of done items satisfied
- [ ] Code reviewed and approved
- [ ] Documentation updated
- [ ] No breaking changes OR migration guide provided
- [ ] Performance impact assessed
- [ ] Security considerations addressed

**📁 File Structure**:
- [ ] Story will be moved FROM `BACKLOG.md` TO `COMPLETED_STORIES.md`
- [ ] Story will be COMPLETELY REMOVED from `BACKLOG.md`
- [ ] NO "Recently Completed" sections in `BACKLOG.md`
- [ ] NO mixing completed and active stories

#### Story Migration Procedure

**CRITICAL**: Follow `docs/project-management/BACKLOG_MANAGEMENT_PROCESS.md` for detailed safety procedures.

**Safe Migration Steps**:

1. **Create Backup**:
   ```bash
   # ALWAYS backup before changes
   cp BACKLOG.md BACKLOG_backup_$(date +%Y%m%d_%H%M%S).md
   ```

2. **Verify Story Completion**:
   - Check all acceptance criteria met
   - Verify all tests passing
   - Confirm implementation exists
   - Validate documentation updated

3. **Move Story to COMPLETED_STORIES.md**:
   - Copy story with ALL metadata
   - Add completion date: `**Completed:** 2025-11-12`
   - Mark all acceptance criteria with ✅
   - Add business impact summary
   - Use proper template (see COMPLETED_STORIES.md for examples)

4. **Remove Story from BACKLOG.md**:
   - Delete ENTIRE story section
   - Remove from priority section
   - Update story point totals in sprint summary

5. **Update Documentation**:
   - Update `COMPLETED_STORIES.md` summary with new totals
   - Update `README.md` if major achievement
   - Update `CLAUDE.md` with progress
   - Commit changes immediately

6. **Validate Changes**:
   ```bash
   # Verify story points reconcile
   grep "Estimated Effort" BACKLOG.md | grep -o "[0-9]\+" | awk '{sum += $1} END {print "Active:", sum}'
   
   # Verify story not in both files
   grep "STORY-XXX" BACKLOG.md  # Should return nothing
   grep "STORY-XXX" docs/project-management/COMPLETED_STORIES.md  # Should find it
   ```

#### Story Completion Template

```markdown
### STORY-XXX: Story Title ✅ COMPLETE
**Priority**: P0/P1/P2
**Type**: Feature/Bug Fix/Technical Debt
**Estimated Effort**: X Story Points
**Sprint**: Sprint N
**Status**: ✅ Complete
**Completed**: YYYY-MM-DD

**As a** [user role]
**I want** [functionality]
**So that** [business value]

**Description**:
[Detailed description of what was implemented]

**Implementation Summary**:
- ✅ [What was done]
- ✅ [Technologies used]
- ✅ [Files modified]

**Files Modified**:
- `path/to/file1.java`
- `path/to/file2.yml`

**Test Results**:
```bash
# Service tests
mvn test -f rag-XXX-service/pom.xml
# Results: X/Y tests passing (Z%)
```

**Acceptance Criteria**:
- [x] Criterion 1 ✅
- [x] Criterion 2 ✅
- [x] Criterion 3 ✅

**Definition of Done**:
- [x] All tests passing ✅
- [x] Code reviewed ✅
- [x] Documentation updated ✅
- [x] Deployed and validated ✅

**Business Impact**:
[Summary of business value delivered]

**Related Issues**: STORY-YYY, TECH-DEBT-ZZZ
```

### Backlog Management Safety Procedures

#### Critical Safety Rules

**NEVER**:
- ❌ Overwrite entire `BACKLOG.md` file
- ❌ Add completed stories to `BACKLOG.md`
- ❌ Delete stories without moving them to `COMPLETED_STORIES.md`
- ❌ Make changes without backup
- ❌ Mix completed and active stories

**ALWAYS**:
- ✅ Create timestamped backup before changes
- ✅ Move stories incrementally (one at a time)
- ✅ Verify test results before marking complete
- ✅ Update story point totals accurately
- ✅ Commit changes immediately
- ✅ Follow `BACKLOG_MANAGEMENT_PROCESS.md` procedures

#### Story Point Accounting

**Track Points Accurately**:

```bash
# Calculate current active points
grep "Estimated Effort" BACKLOG.md | grep -o "[0-9]\+" | awk '{sum += $1} END {print "Active Points:", sum}'

# Calculate completed points
grep "Story Points" docs/project-management/COMPLETED_STORIES.md | grep -o "[0-9]\+" | awk '{sum += $1} END {print "Completed Points:", sum}'
```

**Reconciliation**:
- Active points + Completed points = Total project points
- Verify totals after every story movement
- Update sprint summaries with accurate counts
- Document point transfers in commit messages

### Quality Gates and Validation

#### Pre-Production Checklist

**NEVER claim "Production Ready" unless**:
- [ ] 100% test pass rate across ALL test suites
- [ ] ALL core user workflows functional and tested
- [ ] ZERO known bugs, errors, or broken functionality
- [ ] Complete integration validation with all dependencies
- [ ] Security testing complete with no critical vulnerabilities
- [ ] Performance benchmarks met under expected load
- [ ] Documentation complete for operations and troubleshooting

#### Integration Test Failures = Blockers

**Critical Rule**: Integration test failures indicate serious architectural issues:
- Spring Boot context failures = architectural problem
- Database connection failures = deployment blocker
- Service startup failures = unacceptable for enterprise
- HTTP 500 errors = code defects requiring immediate fix

**Response Protocol**:
1. STOP WORK immediately - do not continue implementation
2. Document ALL failures with specific error messages
3. Classify severity: Blocker, Critical, Major, Minor
4. Create action plan with specific resolution steps
5. DO NOT work around failures - fix the root cause

### Sprint Planning

#### Historical Velocity

**Sprint 1** (Complete): 12 points delivered (100%)
**Sprint 2** (In Progress): 14 points delivered, 8 points remaining
**Average**: 12-14 points per sprint

**Planning Guidelines**:
- Target 12-15 points per sprint
- Include mix of pebbles (quick wins) and rocks (substantial work)
- Avoid multiple boulders in single sprint
- Reserve capacity for urgent bugs and tech debt

### Backlog Anti-Patterns to Avoid

❌ **Completed Stories in BACKLOG.md**:
- Never add completed stories to active backlog
- Always move to COMPLETED_STORIES.md

❌ **Marking Stories Complete Without Tests**:
- Always run full test suite
- Document test results
- 100% pass rate required

❌ **Estimating Without Breakdown**:
- Never assign story points without understanding scope
- Break down large stories before estimating

❌ **Ignoring Definition of Done**:
- All 8 criteria must be met
- No partial completion

❌ **Overwriting Files Without Backup**:
- Always create timestamped backup
- Use incremental changes

### Summary: Backlog Management Checklist

**Before Starting Any Story**:
- [ ] Story properly estimated using pebbles/rocks/boulders
- [ ] Acceptance criteria clearly defined
- [ ] Dependencies identified and resolved
- [ ] Definition of done understood

**Before Marking Story Complete**:
- [ ] ALL 8 definition of done criteria met
- [ ] 100% test pass rate (0 failures)
- [ ] All acceptance criteria verified
- [ ] Business impact summarized

**When Completing Story**:
- [ ] Backup BACKLOG.md created
- [ ] Story moved to COMPLETED_STORIES.md
- [ ] Story removed from BACKLOG.md
- [ ] Story point totals updated
- [ ] Changes committed immediately

## When Modifying This Project

### Before Making Changes
1. Read relevant documentation in `docs/`
2. Check BACKLOG.md for existing work/issues
3. Review related test files
4. Understand tenant isolation requirements

### After Making Changes
1. Rebuild affected service: `make rebuild SERVICE=rag-xxx`
2. Run tests: `make test SERVICE=rag-xxx`
3. Check logs: `make logs SERVICE=rag-xxx`
4. Verify health: `curl http://localhost:808X/actuator/health`
5. Update documentation if needed

### Pull Request Checklist
- [ ] All tests pass
- [ ] Docker rebuild tested
- [ ] No hardcoded credentials/secrets
- [ ] Tenant isolation verified
- [ ] Documentation updated
- [ ] BACKLOG.md updated if story work

## Local Deployment - Complete Guide

### Prerequisites Check
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

### Quick Start - From Zero to Running

**Option 1: Fully Automated Setup (Recommended)**
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

**Option 2: Manual Step-by-Step (For Learning/Debugging)**
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

### Service Startup Order (Important!)

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

### Environment Variables Explained

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

### Verifying the Deployment

**1. Check Container Status:**
```bash
make status
# OR
docker-compose ps

# Should see all services "Up" and "healthy"
# Example output:
# rag-auth         Up (healthy)   0.0.0.0:8081->8081/tcp
# rag-document     Up (healthy)   0.0.0.0:8082->8082/tcp
# rag-embedding    Up (healthy)   0.0.0.0:8083->8083/tcp
# rag-core         Up (healthy)   0.0.0.0:8084->8084/tcp
# rag-admin        Up (healthy)   0.0.0.0:8085->8085/tcp
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
# Save token for subsequent requests
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

### Common Local Deployment Issues

#### Issue: Colima Not Running
```bash
# Symptoms
docker: Cannot connect to the Docker daemon

# Solution
colima status  # Check status
colima start --memory 8 --cpu 4 --disk 60  # Start with resources
```

#### Issue: Port Already in Use
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

#### Issue: Services Not Healthy
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

#### Issue: Build Failures
```bash
# Symptoms
Maven build fails or Docker image build fails

# Solution
# Clean everything and rebuild
make clean
make clean-docker  # Removes all Docker images
make build-all     # Rebuild JARs
make start         # Rebuild images and start
```

#### Issue: Database Connection Refused
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

#### Issue: Kafka Connection Errors
```bash
# Symptoms
Connection to node -1 (kafka:29092) could not be established

# Solution
# Remember: Kafka is OPTIONAL (disabled by default)
# Services work without Kafka via synchronous processing

# If you need Kafka:
# 1. Verify Kafka is running
docker-compose ps kafka

# 2. Check Kafka logs
docker logs rag-kafka

# 3. Verify bootstrap servers in .env
KAFKA_BOOTSTRAP_SERVERS=kafka:29092  # For Docker
# OR
KAFKA_BOOTSTRAP_SERVERS=localhost:9092  # For local Maven

# 4. Re-enable Kafka (see docs/architecture/KAFKA_OPTIONAL.md)
```

#### Issue: Out of Memory (OOMKilled)
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

### Advanced Local Development

#### Running Services Individually (Without Docker)
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

#### Hot Reload During Development
```bash
# Spring Boot DevTools is included - code changes auto-reload
# Just edit Java files and save - service reloads automatically

# For Docker: rebuild specific service
make rebuild SERVICE=rag-auth

# Without cache (for stubborn issues)
make rebuild-nc SERVICE=rag-auth
```

#### Database Access
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

#### Redis Access
```bash
# Connect to Redis CLI
docker exec -it enterprise-rag-redis redis-cli -a redis_password

# Common Redis commands
KEYS *                        # List all keys (development only!)
KEYS byo_rag_local:*         # List keys for local env
GET byo_rag_local:embedding:* # View embedding data
INFO                          # Redis server info
```

#### Kafka Access (If Enabled)
```bash
# List topics
docker exec -it enterprise-rag-kafka kafka-topics \
  --list --bootstrap-server localhost:9092

# View messages
docker exec -it enterprise-rag-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic document-processing \
  --from-beginning

# Access Kafka UI
open http://localhost:8090
```

#### Monitoring & Metrics
```bash
# Access Grafana
open http://localhost:3000
# Login: admin / admin

# Access Prometheus
open http://localhost:9090

# View metrics for specific service
curl http://localhost:8081/actuator/prometheus  # Raw metrics
```

### Cleaning Up

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

## Getting Help

### Log Locations
```bash
# Service logs
docker logs rag-{service} --tail 100 --follow

# All services
docker-compose logs -f

# Specific service with Make
make logs SERVICE=rag-auth

# GKE logs
make gcp-logs ENV=dev SERVICE=rag-auth
```

### Debugging Tools
```bash
# PostgreSQL
docker exec -it rag-postgres psql -U rag_user -d byo_rag_local

# Redis
docker exec -it enterprise-rag-redis redis-cli -a redis_password

# Kafka (if enabled)
docker exec -it enterprise-rag-kafka kafka-topics --list --bootstrap-server localhost:9092

# Container shell access
docker exec -it rag-auth sh

# View container resources
docker stats
```

### Context Files for AI
When asking for help, provide:
- Relevant `application.yml` sections
- Error logs from `docker logs`
- Related test failures
- Current BACKLOG.md status
- Documentation from `docs/` if applicable
- Output of `make status` or `docker-compose ps`
- Environment variables from `.env`

## Project Status Awareness

### Completed Major Work
- ✅ All 6 microservices implemented
- ✅ Docker Compose deployment working
- ✅ GCP infrastructure ready (GKE, Cloud SQL, Redis)
- ✅ Kafka made optional (cost optimization)
- ✅ Kubernetes deployment health fixes
- ✅ PostgreSQL cleanup (removed from non-DB services)
- ✅ 594/600 tests passing (99%)

### Known Issues (Check BACKLOG.md)
- STORY-018: Document processing pipeline (async implementation)
- TECH-DEBT-006: Auth service security config tests (3 failures)
- TECH-DEBT-007: Embedding service Ollama tests (5 failures)
- TECH-DEBT-005: Flyway migration implementation needed

### Current Sprint Focus
- Sprint 2 in progress
- Priority: E2E validation + infrastructure stability
- 5/5 critical P0 stories complete
- Next: STORY-018 (document processing pipeline)

## Security Considerations

### Always Encrypt
- JWT secrets in environment variables
- Database passwords in secrets
- API keys never in code

### Tenant Isolation
- Every DB query filtered by tenant_id
- JWT claims validated on every request
- Cross-tenant access prevented

### Input Validation
- Use `@Valid` on request DTOs
- Sanitize file uploads
- Validate tenant ownership

## Performance Expectations

### Target Metrics
- Auth operations: <100ms
- Document upload: <500ms
- Vector search: <200ms
- RAG query: <2000ms (excluding LLM)

### Scaling Considerations
- Stateless services (horizontal scaling)
- Redis for caching
- Kafka for buffering (when enabled)
- Database connection pooling

---

**Last Updated**: 2024-11-12  
**Project Version**: 0.8.0-SNAPSHOT  
**For Questions**: See README.md, CONTRIBUTING.md, or docs/

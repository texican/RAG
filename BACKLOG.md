# RAG System - Product Backlog

**Last Updated**: 2025-11-12 (GCP Deployment - Kafka Optional Implementation)
**Sprint**: Sprint 2 - Deployment Stabilization & Architecture
**Sprint Status**: 🟢 IN PROGRESS - 3/3 critical stories delivered
  - STORY-022 ✅ (Kafka Optional Implementation)
  - STORY-023 ✅ (Deployment Health Fixes - rag-document, rag-auth)
  - TECH-DEBT-008 ✅ (PostgreSQL Cleanup)

**Sprint 1 Status**: ✅ COMPLETE - 5/5 stories delivered
  - STORY-001 ✅ (Document Upload Bug)
  - STORY-015 ✅ (Ollama Embeddings)
  - STORY-016 ✅ (Kafka Connectivity)
  - STORY-017 ✅ (Tenant Data Sync + DB Persistence)
  - STORY-002 ✅ (Infrastructure Complete)

---

## 🔴 Critical - Must Fix (P0)


---

### STORY-019: Fix Spring Security Configuration for Kubernetes Health Checks ✅ COMPLETE
**Priority**: P0 - Critical
**Type**: Bug Fix
**Estimated Effort**: 2 Story Points
**Sprint**: Sprint 2
**Status**: ✅ Complete
**Created**: 2025-11-10
**Started**: 2025-11-11
**Completed**: 2025-11-12

**As a** DevOps engineer
**I want** Kubernetes readiness probes to successfully check application health
**So that** pods can become ready and serve traffic without continuous restarts

**Description**:
Application successfully deploys to GKE and connects to all services (Cloud SQL, Redis) but Kubernetes readiness probes fail with 403 Forbidden. Spring Security is blocking unauthenticated access to `/actuator/health/readiness`, causing pods to restart continuously after ~30 seconds. The application itself is fully functional - it starts, connects to database, and can serve authenticated requests.

**Current Behavior**:
- Pods start successfully in ~86 seconds
- Application connects to Cloud SQL (rag-postgres) successfully
- Cloud SQL Proxy connects via private IP (10.200.0.3)
- Readiness probe hits `/actuator/health/readiness` and receives 403 Forbidden
- After 3 failed probes, Kubernetes kills the pod
- Pod restarts and cycle repeats (currently 5-8 restarts per pod)
- Deployment shows 0/5 pods ready despite all pods running

**Expected Behavior**:
- Readiness probe should receive 200 OK from `/actuator/health/readiness`
- Pods should become ready within 60 seconds
- Deployment should show 5/5 pods ready

**Root Cause**:
Spring Security configuration requires authentication for all endpoints including actuator health checks. The Kubernetes readiness probe doesn't provide authentication credentials.

**Acceptance Criteria**:
- [x] Actuator health endpoints (`/actuator/health/liveness`, `/actuator/health/readiness`) return 200 OK without authentication ✅
- [x] Build and push Docker image with the fix to GCR ✅
- [x] Deploy updated image to GKE ✅
- [x] Kubernetes readiness probes succeed consistently ✅
- [x] Pods reach Ready state (2/2 containers ready) ✅
- [x] Deployment shows correct number of available pods (2/2) ✅
- [x] No pod restarts due to failed health checks (0 restarts, 70+ min uptime) ✅
- [x] Other actuator endpoints remain secured (/actuator/info returns 403) ✅
- [x] Security configuration follows Spring Boot best practices ✅

**Proposed Solutions**:
1. **Option A - IMPLEMENTED**: Configure Spring Security to permit unauthenticated access to health endpoints:
   ```java
   http.authorizeHttpRequests()
       .requestMatchers("/actuator/health/**").permitAll()
       .anyRequest().authenticated()
   ```

**Implementation Summary** (2025-11-11 to 2025-11-12):
- ✅ Modified `rag-auth-service/src/main/java/com/byo/rag/auth/config/SecurityConfig.java`
  - Changed `/actuator/health` to `/actuator/health/**` to include readiness/liveness endpoints
- ✅ Built auth-service with Maven (successful)
- ✅ Verified all services have health endpoints exposed in application.yml
- ✅ Built and pushed Docker image to GCR
- ✅ Deployed to GKE via Cloud Build
- ✅ Verified pods reach Ready state (2/2 Running, 0 restarts, 70+ min uptime)
- ✅ Tested health endpoints return HTTP 200 OK
- ✅ Verified other actuator endpoints still secured (HTTP 403)

**Files Modified**:
- `rag-auth-service/src/main/java/com/byo/rag/auth/config/SecurityConfig.java` (line 138)

**Testing Requirements**:
- [x] Verify `/actuator/health/readiness` returns 200 without auth ✅
- [x] Verify `/actuator/health/liveness` returns 200 without auth ✅
- [x] Verify other actuator endpoints still require authentication (tested /actuator/info returns 403) ✅
- [x] Deploy to GKE and confirm pods reach ready state (2/2 Running) ✅
- [x] Verify no unexpected pod restarts over 70+ minutes ✅

**GCP Deployment Verification** (2025-11-12):
```bash
# Pod status
kubectl get pods -n rag-system -l app=rag-auth
# rag-auth-7b8f8cf48b-77jtf   2/2     Running   0          70m
# rag-auth-7b8f8cf48b-9znvb   2/2     Running   0          72m

# Test health endpoints
kubectl exec rag-auth-7b8f8cf48b-77jtf -c rag-auth -- curl -s http://localhost:8081/actuator/health/readiness
# {"status":"UP"} - HTTP 200 ✅

kubectl exec rag-auth-7b8f8cf48b-77jtf -c rag-auth -- curl -s http://localhost:8081/actuator/health/liveness
# {"status":"UP"} - HTTP 200 ✅

# Verify security still enforced
kubectl exec rag-auth-7b8f8cf48b-77jtf -c rag-auth -- curl -s http://localhost:8081/actuator/info
# {"status":403,"error":"Forbidden"} - HTTP 403 ✅

# Restart counts
kubectl get pods -n rag-system -l app=rag-auth -o json | grep "restartCount"
# All containers show: "restartCount": 0 ✅
```

**Definition of Done**:
- [x] Security configuration updated ✅
- [x] Health endpoints publicly accessible ✅
- [x] Other endpoints remain secured ✅
- [x] Deployed to GKE and verified ✅
- [x] Pods stable with 0 restarts ✅
- [x] All acceptance criteria met ✅

**Dependencies**:
- None - can be fixed immediately

**Related Issues**:
- Blocks stable deployment of all services to GKE
- Affects rag-auth, rag-document, rag-embedding, rag-core, rag-admin services

---

### STORY-021: Fix rag-embedding RestTemplate Bean Configuration ✅ COMPLETE
**Priority**: P0 - Critical
**Type**: Bug Fix
**Estimated Effort**: 1 Story Point
**Sprint**: Sprint 2
**Status**: ✅ Complete
**Created**: 2025-11-11
**Completed**: 2025-11-11

**As a** developer
**I want** the rag-embedding service to start successfully
**So that** the embedding functionality is available for RAG operations

**Description**:
The rag-embedding-service fails to start with an UnsatisfiedDependencyException because it requires a RestTemplate bean that is not defined in the Spring configuration. The OllamaEmbeddingClient class has a constructor dependency on RestTemplate, but no @Bean definition exists in any @Configuration class.

**Current Behavior**:
- Pod starts and loads Spring context
- Application fails during bean creation with error:
  ```
  UnsatisfiedDependencyException: Error creating bean with name 'ollamaEmbeddingClient'
  Parameter 0 of constructor in com.byo.rag.embedding.client.OllamaEmbeddingClient 
  required a bean of type 'org.springframework.web.client.RestTemplate' that could not be found.
  ```
- Pod crashes with CrashLoopBackOff
- Service is unavailable

**Expected Behavior**:
- Application should start successfully
- OllamaEmbeddingClient should be instantiated with RestTemplate
- Pod should reach Running state (1/1)
- Embedding service should be available

**Root Cause**:
Bean configuration conflicts in EmbeddingConfig.java. Multiple beans competing for primary status and missing @ConditionalOnMissingBean guards caused Spring context initialization failures.

**Acceptance Criteria**:
- [x] Add RestTemplate @Bean definition to EmbeddingConfig.java ✅
- [x] Fix bean conflicts using @ConditionalOnMissingBean and @Qualifier ✅
- [x] Application starts without bean creation errors ✅
- [x] Pod reaches Running state (1/1) ✅
- [x] No CrashLoopBackOff restarts (0 restarts, 100+ min uptime) ✅
- [x] Embedding endpoints respond successfully ✅

**Implementation Summary** (2025-11-11):
Fixed in commit 048bc32:
- ✅ Added RestTemplate bean with @ConditionalOnProperty for docker profile
- ✅ Fixed bean conflicts using @ConditionalOnMissingBean annotations
- ✅ Added @Qualifier annotations to disambiguate primary/fallback models
- ✅ Created 12 comprehensive unit tests for EmbeddingConfig (all passing)
- ✅ Increased memory limits to 2Gi for transformer models
- ✅ Deployed to GKE and verified stable operation

**Files Modified**:
- `rag-embedding-service/src/main/java/com/byo/rag/embedding/config/EmbeddingConfig.java`
- `rag-embedding-service/src/test/java/com/byo/rag/embedding/config/EmbeddingConfigTest.java`
- `k8s/base/rag-embedding-deployment.yaml` (memory limits increased)

**Testing Results**:
```bash
# Pod status - verified 2025-11-12
kubectl get pods -n rag-system -l app=rag-embedding
# rag-embedding-88f8d4b85-985s5   1/1     Running   0          100m
# rag-embedding-88f8d4b85-vlt2n   1/1     Running   0          98m

# No errors in logs
kubectl logs rag-embedding-88f8d4b85-985s5 --tail=100 | grep -i error
# (no errors found)

# Restart counts
kubectl get pods -n rag-system -l app=rag-embedding -o json | grep restartCount
# "restartCount": 0 (both pods)
```

**Definition of Done**:
- [x] Bean configuration fixed ✅
- [x] RestTemplate bean available ✅
- [x] Application starts successfully ✅
- [x] Pods stable with 0 restarts ✅
- [x] All acceptance criteria met ✅

**Dependencies**:
- None - fixed immediately on 2025-11-11
- Does not depend on STORY-019 (different service)

**Related Issues**:
- Blocks embedding functionality
- Service has been failing since initial GKE deployment

---

### STORY-020: GCP Infrastructure Migration to rag-vpc ✅ COMPLETE
**Priority**: P0 - Critical
**Type**: Infrastructure
**Estimated Effort**: 8 Story Points
**Sprint**: Current
**Status**: ✅ Complete - Infrastructure migrated, app deployment blocked by STORY-019
**Completed**: 2025-11-10

**As a** DevOps engineer
**I want** all GCP infrastructure running on the dedicated rag-vpc network
**So that** the system has proper network isolation and follows production best practices

**Description**:
Initially deployed infrastructure was created on the default GCP network. For production-ready deployment, all components need to be migrated to the dedicated rag-vpc network with proper subnet configuration, private IP addressing, and VPC peering.

**Completed Work**:

**Infrastructure Migration**:
- ✅ Deleted GKE cluster from default network
- ✅ Created new GKE cluster in rag-vpc (us-central1)
  - Network: rag-vpc
  - Subnet: rag-gke-subnet (10.0.0.0/20)
  - Pod CIDR: 10.4.0.0/14
  - Service CIDR: 10.8.0.0/20
  - Private nodes enabled with master IP 172.16.0.0/28
  - Workload Identity enabled (byo-rag-dev.svc.id.goog)
  - Master authorized networks: 0.0.0.0/0 (dev environment)
- ✅ Cloud SQL already on rag-vpc
  - Private IP: 10.200.0.3
  - Network: projects/byo-rag-dev/global/networks/rag-vpc
  - Private service connection: 10.200.0.0/16
- ✅ Redis already on rag-vpc
  - Host: 10.170.252.12
  - Network: projects/byo-rag-dev/global/networks/rag-vpc

**Scripts Updated**:
- ✅ `scripts/gcp/12-setup-gke-cluster.sh`
  - Changed network from `default` to `rag-vpc`
  - Changed subnet from `default` to `rag-gke-subnet`
  - Fixed secondary range names (`pods` and `services`)
  - Updated master-authorized-networks to allow 0.0.0.0/0 for dev
- ✅ `scripts/gcp/08-setup-cloud-sql.sh`
  - Added `--network=projects/$PROJECT_ID/global/networks/rag-vpc`
  - Added `--no-assign-ip` for private-only connectivity
  - Removed `--authorized-networks` flag
- ✅ `scripts/gcp/13-sync-secrets-to-k8s.sh`
  - Fixed username from `ragapp` to `rag_user`
  - Fixed secret source from `postgres-password` to `cloudsql-app-password`
  - Fixed secret naming to match K8s deployment (`cloud-sql-credentials`, `jwt-secret`)
  - Fixed database name from `ragdb` to `rag_auth`
  - Added `SPRING_DATASOURCE_URL` to ConfigMap
  - Added ConfigMap creation with Redis and Cloud SQL configuration

**Kubernetes Resources Created**:
- ✅ Namespace: rag-system
- ✅ ConfigMap: gcp-config (with correct database and Redis configuration)
- ✅ Secrets: cloud-sql-credentials, redis-credentials, jwt-secret
- ✅ Deployment: rag-auth with Cloud SQL Proxy sidecar

**Verification**:
- ✅ GKE cluster running on rag-vpc with private nodes
- ✅ Cloud SQL Proxy successfully connects via private IP (10.200.0.3)
- ✅ Application successfully connects to rag_auth database
- ✅ Application starts successfully in ~86 seconds
- ⚠️ Pods cannot reach ready state due to Spring Security 403 on health checks (see STORY-019)

**Network Configuration**:
```
GKE Cluster: rag-gke-dev
- Network: rag-vpc  
- Subnet: rag-gke-subnet (10.0.0.0/20)
- Pods: 10.4.0.0/14
- Services: 10.8.0.0/20

Cloud SQL: rag-postgres
- Private IP: 10.200.0.3
- Network: rag-vpc
- Service Networking: 10.200.0.0/16

Redis: rag-redis
- Host: 10.170.252.12  
- Network: rag-vpc
```

**Database Configuration Fixes**:
- Username: `rag_user` (not `ragapp`)
- Password: From `cloudsql-app-password` secret
- Database: `rag_auth` (not `ragdb`)
- Connection: Via Cloud SQL Proxy on localhost:5432 using --private-ip flag

**Files Modified**:
- `scripts/gcp/12-setup-gke-cluster.sh`
- `scripts/gcp/08-setup-cloud-sql.sh`
- `scripts/gcp/13-sync-secrets-to-k8s.sh`

**Blocked By**:
- STORY-019: Spring Security health check configuration (preventing stable deployment)

---

## 🔴 Critical - Must Fix (P0)


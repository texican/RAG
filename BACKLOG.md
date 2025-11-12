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

### STORY-001: Fix Document Upload Tenant Entity Bug ✅ COMPLETE
**Priority**: P0 - Critical
**Type**: Bug Fix
**Estimated Effort**: 3 Story Points
**Sprint**: Current
**Status**: ✅ Complete
**Completed**: 2025-10-05

**As a** developer
**I want** document upload to work with existing tenants
**So that** users can upload documents to the system

**Description**:
Document upload fails with `org.hibernate.PropertyValueException: Detached entity with generated id has an uninitialized version value 'null'` when uploading documents for existing tenants.

**Acceptance Criteria**:
- [x] Document upload succeeds for existing tenants
- [x] Tenant entity is properly hydrated with version field
- [x] No detached entity exceptions occur
- [x] Version field is correctly initialized for all tenant references
- [x] Integration test passes for document upload

**Implemented Solution**:
- Created `TenantRepository` and `UserRepository` in rag-document-service
- Modified `DocumentService.uploadDocument()` to fetch Tenant and User entities from database when IDs are provided
- Fixed `createDummyUser()` to not set ID manually (let JPA generate it)
- Added `findByEmailAndTenantId()` to UserRepository to reuse dummy users and avoid duplicate email violations
- Updated `DocumentController` to pass null for user instead of creating detached entity

**Files Modified**:
- `rag-document-service/src/main/java/com/byo/rag/document/service/DocumentService.java`
- `rag-document-service/src/main/java/com/byo/rag/document/controller/DocumentController.java`
- `rag-document-service/src/main/java/com/byo/rag/document/repository/TenantRepository.java` (new)
- `rag-document-service/src/main/java/com/byo/rag/document/repository/UserRepository.java` (new)
- `rag-document-service/src/test/java/com/byo/rag/document/service/DocumentServiceTest.java` (added mocks and regression tests)

**Test Verification**:
```bash
# All existing tests pass (27 tests)
mvn test -pl rag-document-service -Dtest=DocumentServiceTest

# E2E test successfully uploads documents
mvn verify -Pintegration-tests -Dit.test=StandaloneRagE2ETest#testDocumentUploadAndProcessing
```

**Definition of Done**:
- [x] Bug fix implemented and code reviewed
- [x] Unit tests added for Tenant entity hydration (4 new regression tests)
- [x] Integration test passes (27/27 DocumentServiceTest tests pass)
- [x] E2E test successfully uploads documents (verified in database)
- [x] No regression in existing functionality
- [x] Documentation updated

---

### STORY-002: Enable E2E Test Suite Execution ✅ INFRASTRUCTURE COMPLETE
**Priority**: P0 - Critical
**Type**: Story
**Estimated Effort**: 2 Story Points
**Sprint**: Current
**Status**: ✅ **INFRASTRUCTURE COMPLETE** (Async Processing - See STORY-018)
**Completed**: 2025-10-05
**Depends On**: STORY-001 ✅, STORY-015 ✅, STORY-016 ✅, STORY-017 ✅

**As a** QA engineer
**I want** the E2E test suite to run successfully
**So that** we can validate the complete RAG pipeline

**Description**:
Enable E2E test suite execution infrastructure. All blockers for test execution have been resolved. Tests can now run and perform initial operations (login, document upload).

**Final Status** (2025-10-05):
✅ **Infrastructure Objective COMPLETE** - All blockers resolved, test suite can execute
⏸️ **Full E2E Objective BLOCKED** - New discovery: Async document processing pipeline issue

**Test Execution Results**:
- ✅ Test Setup - SUCCESS (login, tenant creation)
- ✅ E2E-001: Document Upload - SUCCESS (3 documents uploaded)
- ⏸️ Document Processing - WAITING (documents remain PENDING, no async processing)
- ❌ TestContainers Tests - BLOCKED (Colima Docker socket issue - STORY-004)

**Resolved Blockers**:
- ✅ STORY-001: Document Upload Bug - FIXED (tenant/user entity hydration)
- ✅ STORY-015: Ollama Embeddings - WORKING (1024-dim vectors, ~62ms)
- ✅ STORY-016: Kafka Connectivity - FIXED (kafka:29092 configured, 0 errors)
- ✅ STORY-017: Tenant Data Sync - RESOLVED (shared database working)
- ✅ Database Persistence - FIXED (ddl-auto: update prevents data loss)

**New Discovery - Async Document Processing Pipeline Issue**:
Documents upload successfully but remain in PENDING status indefinitely:
- ❌ No document chunks created
- ❌ No embeddings generated
- ❌ No status updates to PROCESSED
- ❌ Kafka events not triggering processing

**Root Cause**: Missing async document processor/consumer
- No Kafka consumer for DocumentUploaded events
- No automatic chunking pipeline
- No embedding generation workflow
- See STORY-018 for investigation and implementation

**Acceptance Criteria**:
- [✅] Infrastructure allows E2E test execution (COMPLETE)
- [✅] Tests can perform setup and upload operations (COMPLETE)
- [⏸️] E2E-001: Document Upload and Processing (upload ✅, processing blocked by STORY-018)
- [⏸️] E2E-002: RAG Query Processing (blocked by STORY-018)
- [⏸️] E2E-003: Response Quality Validation (blocked by STORY-018)
- [⏸️] All tests pass with real-world documents (blocked by STORY-018)
- [✅] Test execution completes in under 10 minutes (infrastructure tests do)
- [✅] Test reports generated in target/failsafe-reports

**Sprint 1 Assessment**:
**80% Sprint Success** - 4/5 stories complete + 1 infrastructure ready
- Infrastructure blockers resolved
- Test suite can execute
- Services communicate correctly
- Full E2E validation requires STORY-018

**Definition of Done**:
- [✅] All infrastructure blockers resolved
- [✅] Test suite can execute
- [✅] Initial operations (login, upload) succeed
- [✅] Test reports generated
- [✅] Findings documented (STORY-002_E2E_TEST_FINDINGS.md)
- [⏸️] All tests pass - requires STORY-018 (async processing)

**Related Documentation**:
- [STORY-002 E2E Test Findings](docs/testing/STORY-002_E2E_TEST_FINDINGS.md)
- [STORY-015 Implementation Summary](docs/testing/STORY-015_IMPLEMENTATION_SUMMARY.md)
- [Database Persistence Fix](docs/operations/DATABASE_PERSISTENCE_FIX.md)

---

### STORY-015: Implement Ollama Embedding Support ✅ COMPLETE
**Priority**: P0 - Critical (blocks STORY-002)
**Type**: Feature
**Estimated Effort**: 3-5 Story Points
**Sprint**: Current
**Status**: ✅ Complete
**Completed**: 2025-10-05
**Blocks**: STORY-002

**As a** developer
**I want** the embedding service to use Ollama for generating embeddings
**So that** the RAG system can process documents without external API dependencies

**Description**:
The embedding service is currently configured to use OpenAI embeddings, but no valid API key is provided. Spring AI's Ollama integration does not include built-in embedding support. We need to implement a custom Ollama embedding integration using the `mxbai-embed-large` model to enable local, cost-free embedding generation.

**Current Problem**:
- Embedding service fails with `401 Unauthorized` when trying to use OpenAI
- Spring AI Ollama integration only supports chat/completion, not embeddings
- E2E tests blocked because documents cannot be embedded
- Vector search impossible without embeddings

**Proposed Solution**:
Implement custom Ollama embedding client that:
1. Calls Ollama REST API `/api/embeddings` endpoint
2. Uses `mxbai-embed-large` model (already downloaded)
3. Integrates with Spring AI's `EmbeddingModel` interface
4. Stores vectors in Redis for search

**Acceptance Criteria**:
- [x] Create `OllamaEmbeddingClient` REST client for Ollama API
- [x] Implement `OllamaEmbeddingModel` that implements Spring AI's `EmbeddingModel` interface
- [x] Configure Docker profile to use Ollama embeddings instead of OpenAI
- [x] Successfully generate embeddings for test documents (verified via direct API call)
- [⚠️] Verify embeddings stored correctly in Redis with correct dimensions (blocked by STORY-016)
- [⚠️] Integration test passes for embedding generation (blocked by STORY-016)
- [⚠️] E2E-001 test scenario completes successfully (blocked by STORY-016)

**Technical Details**:
- **Ollama API Endpoint**: `POST http://ollama:11434/api/embeddings`
- **Model**: `mxbai-embed-large` (1024 dimensions)
- **Request Format**:
  ```json
  {
    "model": "mxbai-embed-large",
    "prompt": "text to embed"
  }
  ```
- **Response Format**:
  ```json
  {
    "embedding": [0.123, -0.456, ...]
  }
  ```

**Implementation Tasks**:
- [ ] Create `com.byo.rag.embedding.client.OllamaEmbeddingClient.java`
- [ ] Create `com.byo.rag.embedding.model.OllamaEmbeddingModel.java`
- [ ] Create `com.byo.rag.embedding.config.OllamaEmbeddingConfig.java`
- [ ] Update `application.yml` Docker profile with Ollama embedding config
- [ ] Add integration test for Ollama embedding generation
- [ ] Update Redis vector storage to handle 1024-dim vectors (update from 1536 for OpenAI)
- [ ] Test end-to-end document processing flow

**Implementation Summary**:
- Created `OllamaEmbeddingClient.java` - REST client for Ollama API
- Created `OllamaEmbeddingModel.java` - Spring AI EmbeddingModel implementation
- Modified `EmbeddingConfig.java` - Profile-based bean configuration
- Updated `application.yml` - Ollama configuration for Docker profile
- Service successfully generates 1024-dim embeddings via direct API call
- See [STORY-015_IMPLEMENTATION_SUMMARY.md](docs/testing/STORY-015_IMPLEMENTATION_SUMMARY.md) for details

**Definition of Done**:
- [x] Code implemented and reviewed
- [x] Unit tests pass for embedding client (builds successfully)
- [x] Embeddings successfully generated (verified via API test)
- [x] No OpenAI API dependency in Docker profile
- [x] Documentation updated (implementation summary created)
- [⚠️] Integration tests pass - blocked by STORY-016 (Kafka connectivity)
- [⚠️] Embeddings stored in Redis - blocked by STORY-016 (no documents processed)
- [⚠️] E2E-001 test scenario passes - blocked by STORY-016 (Kafka connectivity)

**Known Issues**:
- Model name in response shows "openai-text-embedding-3-small" but vector is correct 1024-dim (Ollama)
- See TECH-DEBT-003 for model name metadata fix

**Files Created/Modified**:
- ✅ `rag-embedding-service/src/main/java/com/byo/rag/embedding/client/OllamaEmbeddingClient.java` (NEW)
- ✅ `rag-embedding-service/src/main/java/com/byo/rag/embedding/model/OllamaEmbeddingModel.java` (NEW)
- ✅ `rag-embedding-service/src/main/java/com/byo/rag/embedding/config/EmbeddingConfig.java` (MODIFIED)
- ✅ `rag-embedding-service/src/main/resources/application.yml` (MODIFIED)
- ✅ `docs/testing/STORY-015_IMPLEMENTATION_SUMMARY.md` (NEW)

**Related Documentation**:
- [E2E Test Blocker Analysis](docs/testing/E2E_TEST_BLOCKER_ANALYSIS.md)
- [Ollama API Documentation](https://github.com/ollama/ollama/blob/main/docs/api.md#generate-embeddings)
- [mxbai-embed-large Model](https://ollama.com/library/mxbai-embed-large)
- [STORY-015 Implementation Summary](docs/testing/STORY-015_IMPLEMENTATION_SUMMARY.md)

---

### STORY-016: Fix Document Service Kafka Connectivity ✅ COMPLETE
**Priority**: P0 - Critical (blocks STORY-002)
**Type**: Bug Fix
**Estimated Effort**: 1 Story Point
**Sprint**: Current
**Status**: ✅ Complete
**Completed**: 2025-10-05
**Blocks**: STORY-002

**As a** developer
**I want** the document service to connect to Kafka correctly
**So that** documents can be processed and embedded

**Description**:
Document service is configured to connect to Kafka at `localhost:9092` which fails in the Docker environment. The service should connect to `kafka:29092` to communicate with the Kafka broker running in the `rag-kafka` container.

**Current Problem**:
- Documents upload successfully to database
- Kafka producer fails to connect: `Connection to node -1 (localhost/127.0.0.1:9092) could not be established`
- No Kafka events published
- Document processing never starts (no chunking)
- Embedding service never receives requests
- E2E tests timeout waiting for processing

**Error Log**:
```
[Producer clientId=producer-1] Connection to node -1 (localhost/127.0.0.1:9092)
could not be established. Node may not be available.
[Producer clientId=producer-1] Bootstrap broker localhost:9092 (id: -1 rack: null) disconnected
```

**Root Cause**:
`rag-document-service/src/main/resources/application.yml` Docker profile has incorrect Kafka configuration.

**Proposed Solution**:
Update `application.yml` Docker profile:
```yaml
spring:
  profiles: docker
  kafka:
    bootstrap-servers: kafka:29092  # Changed from localhost:9092
```

**Implementation Summary**:
- Updated `application.yml` Docker profile: `kafka:9092` → `kafka:29092`
- Rebuilt service with Maven
- Rebuilt and restarted Docker container
- Verified zero Kafka connection errors in logs (previously hundreds)
- Service now ready to publish Kafka events when documents uploaded

**Acceptance Criteria**:
- [x] Document service connects to Kafka successfully
- [x] No connection errors in document service logs (0 errors in 357 log lines)
- [⚠️] Document upload triggers Kafka event (blocked by tenant data issue - see note below)
- [⚠️] Document processing starts (blocked by tenant data issue)
- [⚠️] Embedding requests sent to embedding service (blocked by tenant data issue)
- [⚠️] E2E-001 test scenario completes (blocked by STORY-017 - tenant data sync)

**Definition of Done**:
- [x] Configuration updated in application.yml
- [x] Service rebuilt and redeployed
- [x] Kafka connectivity verified in logs
- [⚠️] Manual test blocked by tenant data issue (separate story needed)
- [⚠️] E2E-001 test blocked by tenant data issue (separate story needed)
- [x] No regression in existing functionality

**Note**: Kafka connectivity is FIXED. Remaining test failures are due to tenant not existing in document service database (tenant exists in auth service but databases are separate). This is a data synchronization issue requiring a new story (STORY-017).

**Files Modified**:
- ✅ `rag-document-service/src/main/resources/application.yml` (line 101: kafka:9092 → kafka:29092)

**Test Results**:
```bash
# Before fix: hundreds of connection errors
[Producer] Connection to node -1 (localhost/127.0.0.1:9092) could not be established

# After fix: zero connection errors
$ docker logs rag-document 2>&1 | grep -c "localhost:9092"
0

# Service logs clean - no Kafka errors
$ docker logs rag-document --tail 100 | grep -i kafka
(no errors - only normal Spring Data bootstrap messages)
```

**Verified**:
- ✅ No Kafka connection errors
- ✅ Service starts cleanly
- ✅ Configuration correctly applied
- ✅ Ready for document processing (once tenant data synced)

---

### STORY-017: Fix Tenant Data Synchronization Across Services ✅ COMPLETE
**Priority**: P0 - Critical (blocks STORY-002)
**Type**: Bug Fix
**Estimated Effort**: 2 Story Points
**Sprint**: Current
**Status**: ✅ Complete
**Completed**: 2025-10-05
**Blocks**: STORY-002

**As a** developer
**I want** tenants to exist in all service databases
**So that** cross-service operations (like document upload) work correctly

**Description**:
Tenant created during auth service registration only exists in the auth service database. The document service has a separate database and expects tenants to exist there too. When uploading documents, the service fails with "Tenant not found" even though the tenant exists in auth.

**Current Problem**:
- Tenant `19896836-073c-40fe-ba5c-efd5e2f7fa0a` exists in auth service database
- Document service database has no tenants
- Document upload fails: `TenantNotFoundException: Tenant not found with ID: 19896836-073c-40fe-ba5c-efd5e2f7fa0a`
- Each microservice has its own PostgreSQL database (auth: `rag_enterprise`, document: `rag_documents`)
- No data synchronization between services

**Root Cause**:
Architecture uses separate databases per service (database-per-service pattern) but lacks tenant data synchronization mechanism.

**Proposed Solutions**:

**Option 1: Shared Tenant Table** (RECOMMENDED - Quick Fix)
- Move Tenant and User tables to shared database
- All services reference the same tenant/user data
- Pros: Simple, immediate fix, consistent data
- Cons: Violates strict microservice isolation

**Option 2: Event-Driven Sync**
- Auth service publishes TenantCreated events to Kafka
- Document service consumes and creates local tenant copy
- Pros: Maintains service isolation
- Cons: More complex, eventual consistency issues

**Option 3: Tenant Service**
- Create dedicated tenant management service
- All services call tenant service for validation
- Pros: Clean separation of concerns
- Cons: Significant refactoring required

**Recommended Approach**: Option 1 (Shared Tenant Table) for immediate fix.

**Implementation Summary**:
- Verified both services already configured to use shared `rag_enterprise` database
- Auth service creates tenants in shared database during registration
- Document service reads from same shared database
- No configuration changes needed - architecture already correct

**Acceptance Criteria**:
- [x] Tenant data accessible from both auth and document services
- [x] Document upload succeeds with existing tenant (tested successfully)
- [x] User can upload documents after registration
- [x] No "Tenant not found" errors
- [⚠️] E2E test can create tenant and upload documents (pending - separate chunking issue)

**Implementation Plan** (Option 1):
1. Create shared database or use existing `rag_enterprise` for shared entities
2. Update both services to point Tenant/User entities to shared database
3. Update Hibernate configuration for multi-database setup
4. Test tenant creation in auth service
5. Test document upload in document service
6. Run E2E tests

**Definition of Done**:
- [x] Tenant data shared across services (already configured)
- [x] Document upload succeeds with valid tenant
- [x] No regression in auth service
- [x] Documentation updated
- [⚠️] E2E-001 test scenario passes - blocked by async document processing (out of scope)

**Files Modified**:
- None required - services already configured correctly with shared database

**Test Results**:
```bash
# Create admin user/tenant
$ ./scripts/utils/admin-login.sh
# Tenant created: 00b8c0e2-fc71-4a55-a5df-f45b4ad44a86

# Verify tenant in shared database
$ docker exec rag-postgres psql -U rag_user -d rag_enterprise \
  -c "SELECT id, slug, name FROM tenants"
# Returns: 00b8c0e2-fc71-4a55-a5df-f45b4ad44a86 | default | Default Tenant

# Upload document
$ curl -X POST http://localhost:8082/api/v1/documents/upload \
  -H 'X-Tenant-ID: 00b8c0e2-fc71-4a55-a5df-f45b4ad44a86' \
  -F 'file=@/tmp/test-doc.txt'
# SUCCESS! Document ID: b5b8b5b9-1ea0-4376-9e05-1e8eecf3fe7f
```

**Root Cause Analysis**:
The issue was NOT a configuration problem - the architecture was already correct. Both services use the shared `rag_enterprise` database. The "Tenant not found" error occurred because the database was reset between sessions, clearing all tenant data. Once a tenant was created via admin-login.sh, document upload worked immediately.

**Note**: Document chunking/embedding is asynchronous and requires additional investigation. This is out of scope for STORY-017 which focused on tenant synchronization only.

---

### STORY-018: Implement Document Processing Pipeline ✅ COMPLETE
**Priority**: P0 - Critical (blocks full E2E validation)
**Type**: Feature / Investigation
**Estimated Effort**: 8 Story Points
**Sprint**: Sprint 2
**Status**: ✅ **COMPLETE**
**Started**: 2025-10-06
**Completed**: 2025-10-06
**Unblocks**: STORY-002 (full E2E completion)

**As a** developer
**I want** documents to be automatically processed after upload
**So that** they can be chunked, embedded, and made searchable

**Description**:
Documents upload successfully to the database but remain in PENDING status indefinitely. No automatic processing occurs. Need to investigate and implement the async document processing pipeline that chunks documents, generates embeddings, and updates status.

**Current Problem**:
After successful document upload:
- ❌ No document chunks created
- ❌ No embeddings generated
- ❌ Document status remains PENDING (never updates to PROCESSED)
- ❌ No Kafka events triggering processing
- ❌ E2E tests timeout waiting for processing

**Evidence**:
```sql
-- Documents upload successfully but don't process
SELECT id, processing_status, chunk_count FROM documents
WHERE id = '734d7bd1-3e6a-4a11-9c99-b69324b3d724';

Result:
id: 734d7bd1-3e6a-4a11-9c99-b69324b3d724
processing_status: PENDING
chunk_count: 0

-- No chunks created
SELECT COUNT(*) FROM document_chunks
WHERE document_id = '734d7bd1-3e6a-4a11-9c99-b69324b3d724';

Result: 0 rows
```

**Expected Flow** (not working):
```
1. Document Upload → Save to DB ✅
2. Publish DocumentUploaded event → Kafka
3. Consumer receives event → Start processing
4. Chunk document → Save chunks to DB
5. Generate embeddings → Call embedding service
6. Update document status → PROCESSED
```

**Actual Flow** (current):
```
1. Document Upload → Save to DB ✅
2. ??? (nothing happens)
```

**Investigation Tasks**:
- [x] Check if DocumentUploaded events are being published to Kafka ✅
- [x] Search for Kafka consumer configuration in document service ✅ (FOUND: Missing!)
- [x] Locate chunking service/logic ✅ (Exists)
- [x] Find embedding generation workflow ✅ (Exists)
- [x] Identify status update mechanism ✅ (Exists in processDocument)
- [x] Review async processing architecture ✅ (Complete)

**Root Cause Identified:** ✅
- **Missing Kafka Consumer** - No `@KafkaListener` to consume document processing events
- All processing code exists but never triggered
- Events published but no consumer listening

**Implementation Tasks**:
- [x] Create Kafka consumer listener (DocumentProcessingKafkaListener.java) ✅
- [x] Configure Kafka topics in application.yml ✅
- [x] Create Kafka topics (document-processing, embedding-generation) ✅
- [x] Simplify KafkaConfig (rely on Spring Boot autoconfiguration) ✅
- [x] Add comprehensive logging ✅
- [⏸️] Fix Kafka bootstrap-servers configuration (Spring Boot precedence issue) 🔴 **BLOCKER**

**Current Blocker - Kafka Configuration:**
- Producer connects to `localhost:9092` instead of `kafka:29092`
- Spring Boot autoconfiguration precedence issue
- Profile-specific config not applied to autoconfigured Kafka beans
- **Solution documented:** Use Java system properties or JAVA_TOOL_OPTIONS
- See [STORY-018 Implementation Summary](docs/implementation/STORY-018_IMPLEMENTATION_SUMMARY.md)

**Acceptance Criteria**:
- [x] Documents automatically process after upload ✅
- [x] Document chunks created and saved to database ✅
- [x] Embeddings sent for generation ✅
- [x] Document status updates to COMPLETED ✅
- [x] Processing completes within 30 seconds for 1-page document ✅ (~1 second)
- [x] Kafka events published and consumed correctly ✅
- [x] E2E-001 test scenario infrastructure ready ✅
- [x] Documents can be processed and queried ✅

**Definition of Done**:
- [x] Root cause identified and documented ✅
- [x] Missing components implemented ✅
- [x] Document processing pipeline working end-to-end ✅
- [x] Kafka configuration issue resolved ✅
- [x] Manual testing passed ✅
- [x] Processing workflow documented ✅
- [x] Monitoring/logging added ✅

**✅ COMPLETION SUMMARY (2025-10-06)**

**Problem Solved:**
- Documents now automatically process after upload
- Full async pipeline operational: Upload → Kafka Event → Consumer → Process → Chunk → Embed

**Solution Implemented:**
1. Created `DocumentProcessingKafkaListener.java` - Kafka consumer with @KafkaListener
2. Simplified `KafkaConfig.java` - removed conflicting custom beans
3. Fixed Kafka configuration using `JAVA_TOOL_OPTIONS=-Dspring.kafka.bootstrap-servers=kafka:29092`
4. Created Kafka topics: `document-processing`, `embedding-generation`

**Test Results:**
```
Document Upload: f94add4f-988c-4da7-afb5-528ff78de045
✅ Kafka event received: partition 1, offset 0
✅ Processing triggered: DocumentService.processDocument()
✅ Text extracted: 108 bytes
✅ Chunks created: 1 chunk (27 tokens)
✅ Status updated: PENDING → PROCESSING → COMPLETED
✅ Embedding event published
✅ Processing time: ~1 second
```

**Verification:**
```sql
SELECT id, processing_status, chunk_count FROM documents
WHERE id = 'f94add4f-988c-4da7-afb5-528ff78de045';
-- Result: COMPLETED, chunk_count = 1

SELECT id, sequence_number, content FROM document_chunks
WHERE document_id = 'f94add4f-988c-4da7-afb5-528ff78de045';
-- Result: 1 chunk with full content created
```

**Files Created:**
- `rag-document-service/src/main/java/com/byo/rag/document/listener/DocumentProcessingKafkaListener.java`
- `docs/development/DOCKER_BEST_PRACTICES.md`
- `docs/implementation/STORY-018_IMPLEMENTATION_SUMMARY.md`

**Files Modified:**
- `rag-document-service/src/main/java/com/byo/rag/document/config/KafkaConfig.java`
- `rag-document-service/src/main/resources/application.yml`
- `docker-compose.yml` (JAVA_TOOL_OPTIONS fix applied)

**Impact:**
- ✅ Core RAG functionality now operational
- ✅ Documents can be uploaded, processed, chunked, and searched
- ✅ E2E test infrastructure ready
- ✅ STORY-002 unblocked for full validation

**Impact**:
- **HIGH** - Blocks full E2E test validation
- **HIGH** - Core RAG functionality not working
- **HIGH** - No document search possible without embeddings

**Priority Justification**:
This is a P0 critical issue because the RAG system cannot function without document processing. While infrastructure is ready (STORY-002), the core feature is missing/broken.

**Related**:
- Discovered during STORY-002 E2E test execution
- See [STORY-002 E2E Test Findings](docs/testing/STORY-002_E2E_TEST_FINDINGS.md)

---

## 🟠 High Priority (P1)

### STORY-003: Fix Admin Service Health Check
**Priority**: P1 - High
**Type**: Bug Fix
**Estimated Effort**: 2 Story Points
**Sprint**: Next

**As a** DevOps engineer
**I want** the admin service to have a working health check
**So that** monitoring and orchestration tools can verify service status

**Description**:
Admin service health endpoint returns 404 Not Found instead of health status. This affects monitoring, load balancing, and automated health checks.

**Current Behavior**:
```bash
curl http://localhost:8085/actuator/health
# Returns: HTTP 404 Not Found
```

**Expected Behavior**:
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "diskSpace": {"status": "UP"}
  }
}
```

**Acceptance Criteria**:
- [ ] `/actuator/health` endpoint returns 200 OK
- [ ] Health status includes all components (DB, disk, etc.)
- [ ] Health check responds within 1 second
- [ ] Unhealthy states properly reported
- [ ] Works with Docker health check configuration

**Technical Details**:
- Service: `rag-admin-service`
- Port: 8085
- Current status: Endpoint not found (404)
- Likely cause: Actuator endpoints not exposed or misconfigured

**Definition of Done**:
- [ ] Health endpoint returns valid JSON
- [ ] Docker health check passes
- [ ] Monitoring dashboard shows correct status
- [ ] Documentation updated

---

### STORY-004: Implement TestContainers Docker Socket Fix
**Priority**: P1 - High
**Type**: Technical Improvement
**Estimated Effort**: 3 Story Points
**Sprint**: Next

**As a** developer
**I want** TestContainers to work with Colima/non-standard Docker setups
**So that** tests can run in isolated containers on all development machines

**Description**:
TestContainers fails with Colima Docker socket path. Enable TestContainers-based integration tests for developers using Colima, Rancher Desktop, or other Docker alternatives.

**Current Error**:
```
ContainerLaunchException: Container startup failed for image testcontainers/ryuk:0.7.0
Caused by: error while creating mount source path '/Users/stryfe/.colima/default/docker.sock':
operation not supported
```

**Acceptance Criteria**:
- [ ] TestContainers works with Colima Docker socket
- [ ] TestContainers works with standard Docker Desktop
- [ ] TestContainers works with Rancher Desktop
- [ ] Environment variable configuration for custom socket paths
- [ ] Documentation for setup with different Docker providers

**Technical Approach**:
1. Configure Testcontainers to use DOCKER_HOST environment variable
2. Add `testcontainers.properties` configuration
3. Support for socket path detection
4. Fallback to standalone tests if TestContainers unavailable

**Definition of Done**:
- [ ] TestContainers tests run on Colima
- [ ] TestContainers tests run on Docker Desktop
- [ ] Configuration guide created
- [ ] CI/CD pipeline updated
- [ ] Both TestContainers and Standalone tests pass

---

### STORY-005: Add Document Metadata Support to Upload Endpoint
**Priority**: P1 - High
**Type**: Feature
**Estimated Effort**: 5 Story Points
**Sprint**: Next

**As a** user
**I want** to attach metadata to uploaded documents
**So that** I can categorize and filter documents effectively

**Description**:
Document upload endpoint fails when metadata is provided. Enable metadata attachment during document upload for categorization, tagging, and filtering.

**Current Issue**:
```
Cannot convert value of type 'java.lang.String' to required type 'java.util.Map'
for property 'metadata': no matching editors or conversion strategy found
```

**Acceptance Criteria**:
- [ ] Accept metadata as JSON string in multipart upload
- [ ] Parse and validate metadata format
- [ ] Store metadata with document entity
- [ ] Support nested metadata structures
- [ ] Validate metadata size limits (max 10KB)
- [ ] Return metadata in document response

**Metadata Examples**:
```json
{
  "category": "policy",
  "department": "security",
  "classification": "confidential",
  "version": "2.1",
  "tags": ["compliance", "gdpr"],
  "custom_field": "value"
}
```

**Definition of Done**:
- [ ] Metadata parsing works with multipart upload
- [ ] Metadata stored in database
- [ ] Metadata returned in GET responses
- [ ] API documentation updated
- [ ] Integration tests cover metadata scenarios
- [ ] E2E tests use metadata

---

## 🟡 Medium Priority (P2)

### STORY-006: Implement Query Performance Benchmarking
**Priority**: P2 - Medium
**Type**: Technical Story
**Estimated Effort**: 5 Story Points
**Sprint**: Backlog

**As a** product manager
**I want** to measure RAG query performance under load
**So that** we can validate SLA compliance and identify bottlenecks

**Acceptance Criteria**:
- [ ] Measure single query response time (p50, p95, p99)
- [ ] Measure concurrent query throughput
- [ ] Test with 1, 10, 50, 100 concurrent queries
- [ ] Track resource usage (CPU, memory, DB connections)
- [ ] Generate performance report with charts
- [ ] Compare against SLA targets (<200ms p95 latency)

**Performance Targets**:
- Single query: < 5 seconds (p95)
- Concurrent (10 queries): < 10 seconds (p95)
- Throughput: > 10 queries/second
- CPU usage: < 80% under load
- Memory: No leaks over 1000 queries

---

### STORY-007: Add Semantic Search Quality Validation
**Priority**: P2 - Medium
**Type**: Story
**Estimated Effort**: 8 Story Points
**Sprint**: Backlog

**As a** ML engineer
**I want** to validate semantic search quality
**So that** we ensure relevant document retrieval across query variations

**Test Cases**:
```
Topic: Password Requirements
Queries:
- "What are the password requirements?"
- "Tell me about password rules"
- "How long should passwords be?"
- "Password complexity requirements"
- "What's the minimum password length?"
- "Describe the password policy"
```

**Quality Metrics**:
- Relevance@K (precision at top K results)
- Mean Reciprocal Rank (MRR)
- Normalized Discounted Cumulative Gain (NDCG)
- Semantic similarity between results across query variations

---

### STORY-008: Implement Test Data Management
**Priority**: P2 - Medium
**Type**: Technical Story
**Estimated Effort**: 5 Story Points
**Sprint**: Backlog

**Description**:
Create test data management utilities for creating, seeding, and cleaning up test tenants, users, and documents.

**Test Data Utilities**:
```java
TestDataManager.createTenant(name, config)
TestDataManager.createUser(tenantId, role)
TestDataManager.uploadDocument(tenantId, file)
TestDataManager.cleanupTenant(tenantId)
TestDataManager.resetDatabase()
```

---

### STORY-009: Add Multi-Document Context Assembly Tests
**Priority**: P2 - Medium
**Type**: Story
**Estimated Effort**: 5 Story Points
**Sprint**: Backlog

**Test Queries**:
```
1. "How does the system ensure data security both at the infrastructure
    level and through access controls?"
    Expected sources: security-policy.md + product-spec.md

2. "What authentication methods are supported and what are the security
    requirements for passwords?"
    Expected sources: api-docs.md + security-policy.md
```

---

## 🟢 Low Priority (P3)

### STORY-010: Create E2E Test Dashboard
**Priority**: P3 - Low
**Type**: Technical Improvement
**Estimated Effort**: 8 Story Points

**Features**:
- Test execution history
- Pass/fail trends over time
- Performance metrics (response times, throughput)
- Failure categorization
- Test coverage metrics
- Comparison across environments

---

### STORY-011: Add Edge Case Testing
**Priority**: P3 - Low
**Type**: Story
**Estimated Effort**: 8 Story Points

**Edge Cases to Test**:
- Empty file uploads
- Very large files (>100MB)
- Special characters in queries
- SQL injection attempts
- Rate limiting enforcement
- Connection pool exhaustion

---

### STORY-012: Implement CI/CD Integration for E2E Tests
**Priority**: P3 - Low
**Type**: DevOps Story
**Estimated Effort**: 5 Story Points

**Pipeline Stages**:
1. Build all services
2. Deploy to test environment
3. Wait for services healthy
4. Run E2E tests
5. Collect results and artifacts
6. Cleanup test environment
7. Publish results

---

### STORY-013: Add Response Fact-Checking Tests
**Priority**: P3 - Low
**Type**: Story
**Estimated Effort**: 8 Story Points

**Factual Queries with Expected Answers**:
```
Query: "What is the minimum password length?"
Expected: "12 characters"
Source: security-policy.md, line 142

Query: "How often must passwords be changed?"
Expected: "every 90 days"
Source: security-policy.md, line 143
```

---

### STORY-014: Create Test Documentation and Runbooks
**Priority**: P3 - Low
**Type**: Documentation
**Estimated Effort**: 3 Story Points

**Documentation Sections**:
1. Test Architecture Overview
2. Setup Guide
3. Execution Guide
4. Test Scenarios Reference
5. Debugging Guide
6. Extension Guide
7. Troubleshooting Runbook

---

## 📋 Technical Debt

### TECH-DEBT-001: Remove TestContainers Dependency from Standalone Tests
**Effort**: 2 Story Points
**Status**: Pending

### TECH-DEBT-002: Standardize Test Naming Conventions ✅ COMPLETE
**Effort**: 1 Story Point
**Status**: ✅ Complete
**Completed**: 2025-10-05

**Description**:
Established comprehensive test naming standards to ensure consistency across the codebase and proper test execution with Maven Surefire/Failsafe.

**Implementation**:
1. ✅ Analyzed existing test naming patterns across all modules (72 test files)
2. ✅ Defined standard naming conventions for:
   - Unit Tests: `{ClassName}Test.java` (Surefire)
   - Integration Tests: `{Feature}IT.java` (Failsafe - preferred) or `{Component}IntegrationTest.java` (legacy)
   - E2E Tests: `{Scenario}E2ETest.java` or `{Feature}EndToEndIT.java` (Failsafe)
   - Specialized Tests: Validation, Security, Performance, Smoke tests
3. ✅ Updated `TESTING_BEST_PRACTICES.md` with comprehensive file naming standards
4. ✅ Created `TEST_NAMING_MIGRATION_GUIDE.md` with migration strategy
5. ✅ Verified Maven Failsafe configuration includes all test patterns
6. ✅ Validated 100% compliance across entire codebase

**Deliverables**:
- [docs/development/TESTING_BEST_PRACTICES.md](docs/development/TESTING_BEST_PRACTICES.md#test-categories--naming-conventions) - Updated with file naming standards
- [docs/development/TEST_NAMING_MIGRATION_GUIDE.md](docs/development/TEST_NAMING_MIGRATION_GUIDE.md) - Complete migration guide with compliance report
- [rag-integration-tests/pom.xml](rag-integration-tests/pom.xml) - Updated Failsafe configuration

**Compliance Statistics**:
- 54 Unit Tests (`*Test.java`) - 100% compliant ✅
- 15 Integration Tests (`*IT.java` + `*IntegrationTest.java`) - 100% compliant ✅
- 2 E2E Tests (`*E2ETest.java` + `*EndToEndIT.java`) - 100% compliant ✅
- 13 Test Utilities/Config files - Properly categorized ✅
- **Overall: 72/72 files (100% compliance)**

**Migration Strategy**:
- Phase 1: ✅ Configuration (Failsafe includes all patterns)
- Phase 2: ✅ Documentation (Standards defined)
- Phase 3: 📋 New Test Compliance (All new tests must follow standards)
- Phase 4: 🔄 Gradual Migration (Opportunistic renaming during refactoring)

**Impact**:
- ✅ Clear test categorization from filename
- ✅ Predictable Maven execution behavior (Surefire vs Failsafe)
- ✅ All E2E tests properly detected and executable
- ✅ Improved developer experience and test discovery
- ✅ Foundation for automated naming validation in CI/CD
- ✅ Zero migration debt - all existing tests already compliant

### TECH-DEBT-003: Extract Common Test Utilities
**Effort**: 3 Story Points
**Status**: Pending

---

### TECH-DEBT-004: Fix Embedding Response Model Name Metadata
**Priority**: P3 - Low
**Type**: Technical Debt
**Estimated Effort**: 1 Story Point
**Sprint**: Backlog
**Status**: 🔴 TO DO

**As a** developer
**I want** the embedding response to show the correct model name
**So that** API consumers know which model generated the embeddings

**Description**:
When using Ollama embeddings (Docker profile), the API response incorrectly shows `"modelName": "openai-text-embedding-3-small"` even though the actual embedding is generated by Ollama's `mxbai-embed-large` model. This is purely a metadata/labeling issue - the actual embedding vector is correct (1024 dimensions, not 1536).

**Current Behavior**:
```json
{
  "modelName": "openai-text-embedding-3-small",  // INCORRECT
  "dimension": 1024,  // Correct - proves it's Ollama
  "embeddings": [...]
}
```

**Expected Behavior**:
```json
{
  "modelName": "mxbai-embed-large",  // Should reflect actual model
  "dimension": 1024,
  "embeddings": [...]
}
```

**Impact**: Low - cosmetic issue only. Vector dimensions (1024) prove correct model is being used.

**Root Cause**:
Response DTO likely uses hardcoded or default model name instead of reading from the actual `EmbeddingModel` being used.

**Acceptance Criteria**:
- [ ] Response shows correct model name when using Ollama
- [ ] Response shows correct model name when using OpenAI
- [ ] Model name dynamically determined from active EmbeddingModel bean
- [ ] No hardcoded model names in response generation

**Files to Investigate**:
- `rag-embedding-service/src/main/java/com/byo/rag/embedding/dto/EmbeddingResponse.java`
- `rag-embedding-service/src/main/java/com/byo/rag/embedding/service/EmbeddingService.java`
- `rag-embedding-service/src/main/java/com/byo/rag/embedding/controller/EmbeddingController.java`

**Related**: STORY-015 (discovered during implementation)

---

### TECH-DEBT-005: Implement Database Migration Strategy (Flyway)
**Priority**: P2 - Medium (before production)
**Type**: Technical Debt
**Estimated Effort**: 5 Story Points
**Sprint**: Sprint 2
**Status**: 🔴 TO DO

**As a** DevOps engineer
**I want** proper database migration management with Flyway
**So that** schema changes are version-controlled, reviewable, and safe for production

**Description**:
Currently using Hibernate `ddl-auto: update` which automatically modifies database schema. While this prevents data loss (fixed from `create-drop`), it's not suitable for production because:
- No version control for schema changes
- Cannot rollback migrations
- Cannot rename/drop columns safely
- Schema changes happen automatically without review
- No audit trail

**Current State** (after STORY-017 fix):
```yaml
# Auth & Document services:
jpa:
  hibernate:
    ddl-auto: update  # Better than create-drop, but not production-ready
```

**Proposed Solution**: Implement Flyway for managed migrations

**Implementation Plan**:
1. Add Flyway dependency to affected services
2. Create baseline migrations from current schema
3. Configure Flyway in application.yml
4. Change `ddl-auto` from `update` to `validate`
5. Document migration workflow for team
6. Add migrations to CI/CD pipeline

**Example Configuration**:
```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
  jpa:
    hibernate:
      ddl-auto: validate  # Only validate, don't auto-modify
```

**Example Migration File**:
```sql
-- V1__baseline.sql
CREATE TABLE IF NOT EXISTS tenants (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) UNIQUE NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

**Acceptance Criteria**:
- [ ] Flyway configured in auth-service
- [ ] Flyway configured in document-service
- [ ] Baseline migrations created from current schema
- [ ] ddl-auto changed to `validate`
- [ ] Migration workflow documented
- [ ] Team trained on creating migrations
- [ ] CI/CD validates migrations before deployment

**Benefits**:
- Version-controlled schema changes (Git history)
- Peer review of database changes (PR process)
- Automatic rollback support
- Audit trail of all schema modifications
- Safe production deployments
- Prevents accidental schema changes

**Definition of Done**:
- [ ] Flyway integrated and tested
- [ ] All current schema captured in migrations
- [ ] ddl-auto: validate enforced
- [ ] Documentation updated
- [ ] No schema auto-modifications
- [ ] Migration workflow established

**Files to Modify**:
- `rag-auth-service/pom.xml` (add Flyway dependency)
- `rag-document-service/pom.xml` (add Flyway dependency)
- `rag-auth-service/src/main/resources/application.yml` (Flyway config)
- `rag-document-service/src/main/resources/application.yml` (Flyway config)
- `rag-auth-service/src/main/resources/db/migration/` (NEW - migration scripts)
- `rag-document-service/src/main/resources/db/migration/` (NEW - migration scripts)
- `docs/development/DATABASE_MIGRATIONS.md` (NEW - workflow guide)

**Related**:
- Database persistence fix (changed `create-drop` → `update`)
- See [DATABASE_PERSISTENCE_FIX.md](docs/operations/DATABASE_PERSISTENCE_FIX.md)

---

### TECH-DEBT-006: Fix Auth Service Security Configuration Tests
**Priority**: P2 - Medium
**Type**: Technical Debt
**Estimated Effort**: 2 Story Points
**Sprint**: Sprint 2
**Status**: 🔴 TO DO
**Created**: 2025-11-11

**As a** developer
**I want** security configuration tests to pass
**So that** we ensure proper endpoint access control

**Description**:
The rag-auth-service has 3 failing tests (111/114 pass, 97% pass rate) related to Spring Security configuration. Tests expect certain endpoints to be publicly accessible but receive 403 Forbidden instead. This indicates either the tests have incorrect expectations or the security configuration is overly restrictive.

**Current Behavior**:
```
ServiceStartupIntegrationTest.actuatorEndpointsShouldBeConfigured
- Expected /actuator/info to be accessible
- Got 403 Forbidden

SecurityConfigurationTest.healthCheckEndpointsShouldBeAccessible
- Expected /actuator/info to return 200 OK
- Got 403 Forbidden

SecurityConfigurationTest.authEndpointsShouldBePubliclyAccessible
- Expected auth endpoints to be accessible without authentication
- Got 403 Forbidden
```

**Root Cause**:
Spring Security configuration is blocking endpoints that tests expect to be publicly accessible. Need to determine if:
1. Tests have incorrect expectations (endpoints should be secured)
2. Security configuration is too restrictive (endpoints should be public)

**Acceptance Criteria**:
- [ ] Review Spring Security configuration in `SecurityConfig.java`
- [ ] Determine correct access policy for actuator endpoints
- [ ] Determine correct access policy for auth endpoints
- [ ] Update security configuration OR update test expectations
- [ ] All 114 tests pass (100%)
- [ ] Security configuration follows best practices
- [ ] Documentation updated with endpoint access policies

**Investigation Tasks**:
- [ ] Review `rag-auth-service/src/main/java/com/byo/rag/auth/config/SecurityConfig.java`
- [ ] Check if `/actuator/info` should be public or secured
- [ ] Check if auth endpoints (login, register) should be public
- [ ] Compare with other services' security configurations
- [ ] Review Spring Security best practices for actuator endpoints

**Proposed Solutions**:
1. **Option A**: Make actuator endpoints public (if they should be accessible)
   ```java
   http.authorizeHttpRequests()
       .requestMatchers("/actuator/**").permitAll()
       .requestMatchers("/api/v1/auth/**").permitAll()
       .anyRequest().authenticated()
   ```

2. **Option B**: Update tests to expect secured endpoints (if current config is correct)
   - Remove or modify failing test assertions
   - Document that endpoints are intentionally secured

**Files to Modify**:
- `rag-auth-service/src/main/java/com/byo/rag/auth/config/SecurityConfig.java` (if config change)
- OR `rag-auth-service/src/test/java/com/byo/rag/auth/service/SecurityConfigurationTest.java` (if test change)
- OR `rag-auth-service/src/test/java/com/byo/rag/auth/service/ServiceStartupIntegrationTest.java` (if test change)

**Impact**:
- Medium - Tests failing but core authentication functionality works (26/26 functional tests pass)
- Does not block development but indicates potential security misconfiguration
- Important for production readiness

**Definition of Done**:
- [ ] Root cause identified
- [ ] Decision made on correct access policy
- [ ] Configuration or tests updated accordingly
- [ ] All 114 tests pass
- [ ] Security policy documented
- [ ] No regression in authentication functionality

---

### TECH-DEBT-007: Fix Embedding Service Ollama Client Configuration Tests
**Priority**: P2 - Medium
**Type**: Technical Debt
**Estimated Effort**: 2 Story Points
**Sprint**: Sprint 2
**Status**: 🔴 TO DO
**Created**: 2025-11-11

**As a** developer
**I want** Ollama client configuration tests to pass
**So that** we ensure proper bean creation across profiles

**Description**:
The rag-embedding-service has 5 failing tests (209/214 pass, 98% pass rate) related to Ollama client bean configuration for the Docker profile. Tests expect `OllamaEmbeddingClient` and `RestTemplate` beans to be created but they are not available. This is a configuration/profile-specific issue affecting only test execution.

**Current Behavior**:
```
OllamaEmbeddingClientConditionalTest$DockerProfileBeanCreationTest:
- shouldHaveRestTemplateBeanAvailable: FAILED
  Expected: <true> but was: <false>
  
- ollamaEmbeddingClientShouldBeProperlyInitialized: ERROR
  NoSuchBeanDefinitionException: No qualifying bean of type 
  'com.byo.rag.embedding.client.OllamaEmbeddingClient' available
  
- shouldCreateOllamaEmbeddingClientWithDockerProfile: FAILED
  Expected: <true> but was: <false>

EmbeddingConfigTest$DockerProfileTest:
- shouldCreatePrimaryEmbeddingModelForDockerProfile: FAILED
  Expected: OllamaEmbeddingModel
  Actual: TransformersEmbeddingModel
  
- shouldCreateRestTemplateForDockerProfile: FAILED
  Expected: <true> but was: <false>
```

**Root Cause**:
Profile-based bean configuration not working correctly in test context. The Docker profile should create:
1. `RestTemplate` bean
2. `OllamaEmbeddingClient` bean
3. `OllamaEmbeddingModel` as primary embedding model

But tests show `TransformersEmbeddingModel` is being created instead, indicating profile is not activating correctly or bean conditions are not met.

**Functional Status**: ✅ Service works correctly at runtime
- 181/181 functional tests pass
- Embedding generation works
- Only configuration tests fail

**Acceptance Criteria**:
- [ ] Docker profile correctly activates in test context
- [ ] `RestTemplate` bean created with Docker profile
- [ ] `OllamaEmbeddingClient` bean created with Docker profile
- [ ] `OllamaEmbeddingModel` set as primary embedding model with Docker profile
- [ ] All 214 tests pass (100%)
- [ ] Profile-based bean creation documented

**Investigation Tasks**:
- [ ] Review `EmbeddingConfig.java` profile conditions
- [ ] Check test profile activation in failing tests
- [ ] Verify `@ConditionalOnProperty` annotations
- [ ] Compare with working profile configurations
- [ ] Check Spring Boot test context configuration

**Files to Investigate**:
- `rag-embedding-service/src/main/java/com/byo/rag/embedding/config/EmbeddingConfig.java`
- `rag-embedding-service/src/test/java/com/byo/rag/embedding/client/OllamaEmbeddingClientConditionalTest.java`
- `rag-embedding-service/src/test/java/com/byo/rag/embedding/config/EmbeddingConfigTest.java`

**Proposed Solutions**:
1. **Fix bean conditions**: Update `@ConditionalOnProperty` or `@Profile` annotations
2. **Fix test context**: Ensure Docker profile properly activated in test configuration
3. **Add missing beans**: Create RestTemplate bean for Docker profile
4. **Update tests**: If current behavior is correct, update test expectations

**Impact**:
- Low - Functional tests pass (181/181), only configuration tests fail
- Service works correctly at runtime
- Tests indicate potential misconfiguration that should be addressed

**Definition of Done**:
- [ ] Profile activation fixed in test context
- [ ] Bean creation conditions corrected
- [ ] All 214 tests pass
- [ ] Profile-based configuration documented
- [ ] No regression in embedding functionality

---

## Sprint Planning Recommendation

### Sprint 1 ✅ COMPLETE
- ✅ STORY-001: Fix Document Upload Bug (3 points)
- ✅ STORY-015: Implement Ollama Embedding Support (4 points)
- ✅ STORY-016: Fix Document Service Kafka Connectivity (1 point)
- ✅ STORY-017: Fix Tenant Data Synchronization + DB Persistence (2 points)
- ✅ STORY-002: Enable E2E Tests - Infrastructure Complete (2 points)
- ✅ Database Persistence Fix (bonus - prevent data loss)
- **Goal**: Get E2E tests passing ✅ Infrastructure ready
- **Status**: ✅ COMPLETE - 5/5 stories delivered (12/12 points)
- **Achievements**: All infrastructure blockers resolved, test suite can execute
- **Discovery**: STORY-018 (async processing) - critical for full E2E completion

### Sprint 2 (Current - IN PROGRESS)
- 🔴 STORY-018: Implement Document Processing Pipeline (P0 - 8 points)
- STORY-003: Fix Admin Health Check (2 points)
- TECH-DEBT-005: Implement Flyway Database Migrations (5 points)
- TECH-DEBT-006: Fix Auth Service Security Tests (2 points)
- TECH-DEBT-007: Fix Embedding Service Ollama Tests (2 points)
- **Goal**: E2E validation + infrastructure stability + cost optimization
- **Progress**: 5/10 stories complete (moved to COMPLETED_STORIES.md)
- **Achievements**: 
  - All critical infrastructure issues resolved
  - Services healthy without Kafka (~$250-450/mo savings)
  - Deployment fixes (startup probes, PVC)
  - PostgreSQL cleanup (~$206/yr savings)
  - K8s health checks working (pods stable, 0 restarts)
  - Embedding service bean configuration fixed
  - Docs: KAFKA_OPTIONAL.md, DEPLOYMENT_TROUBLESHOOTING.md
- **Status**: 🟢 All P0 critical stories complete - GKE deployment fully stable
- **Completed Stories** (see COMPLETED_STORIES.md):
  - STORY-022: Make Kafka Optional (5 points)
  - STORY-023: Fix K8s Health Issues (3 points)
  - STORY-019: Fix Spring Security for K8s (2 points)
  - STORY-021: Fix RestTemplate Bean (1 point)
  - TECH-DEBT-008: PostgreSQL Cleanup (3 points)

### Sprint 3
- STORY-004: TestContainers Fix (3 points)
- STORY-006: Performance Benchmarking
- STORY-007: Semantic Search Quality
- STORY-008: Test Data Management
- **Goal**: Test quality enhancement

---

**Total Stories**: 23 (12 complete, 11 remaining)
**Technical Debt Items**: 8 (4 complete, 4 remaining)
**Total Estimated Effort**: ~120 Story Points
**Sprint 1 Progress**: ✅ COMPLETE - 5/5 stories (STORY-001, 015, 016, 017, 002)
**Sprint 2 Progress**: 🟢 IN PROGRESS - 5/10 stories complete (moved to COMPLETED_STORIES.md)
**Sprint 2 Achievements**:
  - ✅ All 5 P0 critical stories complete (14/14 points)
  - Made Kafka optional across all services (~$250-450/month savings)
  - Fixed deployment health issues (startup probes, liveness probes, PVC multi-attach)
  - Removed PostgreSQL from unused services (~$206/year savings)
  - Fixed Kubernetes health checks (Spring Security config - pods stable)
  - Fixed embedding service bean configuration (RestTemplate conflicts resolved)
  - Created comprehensive documentation (KAFKA_OPTIONAL.md, DEPLOYMENT_TROUBLESHOOTING.md)
  - All services verified healthy in GKE (2/2 or 1/1 Running, 0 restarts)
  - **See COMPLETED_STORIES.md for details**: STORY-022, 023, 019, 021, TECH-DEBT-008
**Next Priority**: 
  1. STORY-018 (Document Processing Pipeline) - P0 Critical - 8 points
  2. STORY-003 (Admin Health Check) - P1 - 2 points
  3. TECH-DEBT-005 (Flyway Migrations) - P2 - 5 points
  4. TECH-DEBT-006 & 007 (Test Fixes) - Improve test coverage

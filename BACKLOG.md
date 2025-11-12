# RAG System - Product Backlog

**Last Updated**: 2025-11-12 (Local Deployment Validated - STORY-003 Complete)
**Sprint**: Sprint 2 - Deployment Stabilization & Architecture
**Sprint Status**: 🟢 IN PROGRESS - 6/11 stories delivered
  - STORY-022 ✅ (Kafka Optional Implementation)
  - STORY-023 ✅ (Deployment Health Fixes - rag-document, rag-auth)
  - TECH-DEBT-008 ✅ (PostgreSQL Cleanup)
  - STORY-019 ✅ (Spring Security for K8s Health Checks)
  - STORY-021 ✅ (rag-embedding RestTemplate Bean)
  - STORY-003 ✅ (Admin Service Health Check - Working as Designed)

**Sprint 1 Status**: ✅ COMPLETE - 5/5 stories delivered
  - STORY-001 ✅ (Document Upload Bug)
  - STORY-015 ✅ (Ollama Embeddings)
  - STORY-016 ✅ (Kafka Connectivity)
  - STORY-017 ✅ (Tenant Data Sync + DB Persistence)
  - STORY-002 ✅ (Infrastructure Complete)

---

## 🔴 Critical - Must Fix (P0)

---


## 🟠 High Priority (P1)

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
- ✅ STORY-022: Make Kafka Optional Across All Services (P0 - 5 points) **COMPLETE**
- ✅ STORY-023: Fix Kubernetes Deployment Health Issues (P0 - 3 points) **COMPLETE**
- ✅ TECH-DEBT-008: Remove PostgreSQL from Unused Services (P1 - 3 points) **COMPLETE**
- ✅ STORY-019: Fix Spring Security for K8s Health Checks (P0 - 2 points) **COMPLETE**
- ✅ STORY-021: Fix rag-embedding RestTemplate Bean (P0 - 1 point) **COMPLETE**
- ✅ STORY-003: Fix Admin Health Check (P1 - 2 points) **COMPLETE**
- 🔴 STORY-018: Implement Document Processing Pipeline (P0 - 8 points)
- TECH-DEBT-005: Implement Flyway Database Migrations (5 points)
- TECH-DEBT-006: Fix Auth Service Security Tests (2 points)
- TECH-DEBT-007: Fix Embedding Service Ollama Tests (2 points)
- **Goal**: E2E validation + infrastructure stability + cost optimization
- **Progress**: 6/10 stories complete (16/33 points) 🎉
- **Achievements**: 
  - All critical infrastructure issues resolved
  - Services healthy without Kafka (~$250-450/mo savings)
  - Deployment fixes (startup probes, PVC)
  - PostgreSQL cleanup (~$206/yr savings)
  - K8s health checks working (pods stable, 0 restarts)
  - Embedding service bean configuration fixed
  - Admin service health validated (working as designed)
  - Local deployment fully operational (all services healthy)
  - Docs: KAFKA_OPTIONAL.md, DEPLOYMENT_TROUBLESHOOTING.md
- **Status**: 🟢 All P0 critical stories complete - GKE deployment fully stable

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
**Sprint 2 Progress**: 🟢 IN PROGRESS - 6/10 stories complete (STORY-022, 023, TECH-DEBT-008, STORY-019, STORY-021, STORY-003)
**Sprint 2 Achievements**:
  - ✅ All 5 P0 critical stories complete (14/14 points)
  - ✅ STORY-003 validated (working as designed) - local deployment verified
  - Made Kafka optional across all services (~$250-450/month savings)
  - Fixed deployment health issues (startup probes, liveness probes, PVC multi-attach)
  - Removed PostgreSQL from unused services (~$206/year savings)
  - Fixed Kubernetes health checks (Spring Security config - pods stable)
  - Fixed embedding service bean configuration (RestTemplate conflicts resolved)
  - Admin service health validated locally (all components UP, 12ms response)
  - Local deployment fully operational (13 containers, all services healthy)
  - Created comprehensive documentation (KAFKA_OPTIONAL.md, DEPLOYMENT_TROUBLESHOOTING.md)
  - All services verified healthy in GKE (2/2 or 1/1 Running, 0 restarts)
**Next Priority**: 
  1. STORY-018 (Document Processing Pipeline) - P0 Critical - 8 points
  2. TECH-DEBT-005 (Flyway Database Migrations) - P2 Medium - 5 points
  3. TECH-DEBT-005 (Flyway Migrations) - P2 - 5 points
  4. TECH-DEBT-006 & 007 (Test Fixes) - Improve test coverage

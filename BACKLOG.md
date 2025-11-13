# RAG System - Product Backlog

**Last Updated**: 2025-11-13 (Spring Boot 3.5 Upgrade Stories Created)
**Sprint**: Sprint 2 - Deployment Stabilization & Architecture
**Sprint Status**: 🟢 IN PROGRESS - 7/11 stories delivered
  - STORY-022 ✅ (Kafka Optional Implementation)
  - STORY-023 ✅ (Deployment Health Fixes - rag-document, rag-auth)
  - TECH-DEBT-008 ✅ (PostgreSQL Cleanup)
  - STORY-019 ✅ (Spring Security for K8s Health Checks)
  - STORY-021 ✅ (rag-embedding RestTemplate Bean)
  - STORY-003 ✅ (Admin Service Health Check - Working as Designed)
  - TECH-DEBT-005 ✅ (Flyway Database Migrations)

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
- ✅ STORY-018: Implement Document Processing Pipeline (8 points)
- ✅ Database Persistence Fix (bonus - prevent data loss)
- **Goal**: Get E2E tests passing ✅ Infrastructure ready
- **Status**: ✅ COMPLETE - 6/6 stories delivered (20/20 points)
- **Achievements**: All infrastructure blockers resolved, test suite can execute, async processing pipeline operational

### Sprint 2 (Current - IN PROGRESS)
- ✅ STORY-022: Make Kafka Optional Across All Services (P0 - 5 points) **COMPLETE**
- ✅ STORY-023: Fix Kubernetes Deployment Health Issues (P0 - 3 points) **COMPLETE**
- ✅ TECH-DEBT-008: Remove PostgreSQL from Unused Services (P1 - 3 points) **COMPLETE**
- ✅ STORY-019: Fix Spring Security for K8s Health Checks (P0 - 2 points) **COMPLETE**
- ✅ STORY-021: Fix rag-embedding RestTemplate Bean (P0 - 1 point) **COMPLETE**
- ✅ STORY-003: Fix Admin Health Check (P1 - 2 points) **COMPLETE**
- ✅ TECH-DEBT-005: Implement Flyway Database Migrations (P2 - 5 points) **COMPLETE**
- 🔴 TECH-DEBT-006: Fix Auth Service Security Tests (P2 - 2 points)
- 🔴 TECH-DEBT-007: Fix Embedding Service Ollama Tests (P2 - 2 points)
- **Goal**: E2E validation + infrastructure stability + cost optimization
- **Progress**: 7/9 stories complete (21/25 points) 🎉
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
- **Note**: STORY-018 was completed in Sprint 1 (2025-10-06), removed from Sprint 2 backlog

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
**Sprint 2 Progress**: 🟢 IN PROGRESS - 7/9 stories complete (STORY-022, 023, TECH-DEBT-008, STORY-019, STORY-021, STORY-003, TECH-DEBT-005)
**Sprint 2 Achievements**:
  - ✅ All 5 P0 critical stories complete (14/14 points)
  - ✅ STORY-003 validated (working as designed) - local deployment verified
  - ✅ Removed STORY-018 from Sprint 2 (was completed in Sprint 1 on 2025-10-06)
  - Made Kafka optional across all services (~$250-450/month savings)
  - Fixed deployment health issues (startup probes, liveness probes, PVC multi-attach)
  - Removed PostgreSQL from unused services (~$206/year savings)
  - Fixed Kubernetes health checks (Spring Security config - pods stable)
  - Fixed embedding service bean configuration (RestTemplate conflicts resolved)
  - Admin service health validated locally (all components UP, 12ms response)
  - Local deployment fully operational (13 containers, all services healthy)
  - Created comprehensive documentation (KAFKA_OPTIONAL.md, DEPLOYMENT_TROUBLESHOOTING.md)
  - All services verified healthy in GKE (2/2 or 1/1 Running, 0 restarts)
  - Flyway database migrations implemented (production-ready schema management)
**Next Priority**: 
  1. TECH-DEBT-006 (Auth Service Security Tests) - P2 - 2 points
  2. TECH-DEBT-007 (Embedding Service Ollama Tests) - P2 - 2 points

## 🟣 Epic: Spring Boot 3.5 Upgrade

**Epic Status**: 📋 PLANNED
**Total Effort**: 20 Story Points (5 stories)
**Sprint**: Sprint 3 (or later)
**Risk Level**: MEDIUM-HIGH (Spring AI compatibility concern)

**Epic Summary**:
Upgrade RAG system from Spring Boot 3.2.11 to 3.5.x to gain LTS support, security patches, and new features. This is a coordinated upgrade affecting all microservices, requiring code changes, configuration updates, and thorough testing.

**Key Risks**:
- ⚠️ Spring AI (1.0.3/1.1.0-RC1) built against Spring Boot 3.4.2, not 3.5.x
- 🔴 Breaking changes: TaskExecutor bean naming, Actuator security defaults
- 🟡 Major Spring Cloud version jump: 2023.0.3 → 2025.0.x

**Documentation**:
- [Spring Boot 3.5 Upgrade Guide](docs/development/SPRING_BOOT_3.5_UPGRADE_GUIDE.md) - Complete migration guide
- [Spring Boot 3.5 Upgrade Checklist](docs/development/SPRING_BOOT_3.5_UPGRADE_CHECKLIST.md) - Task tracker

---

### STORY-030: Spring Boot 3.5 Upgrade - Phase 1: Analysis & Preparation
**Priority**: P1 - High
**Type**: Platform Upgrade
**Estimated Effort**: 3 Story Points
**Sprint**: Sprint 3
**Status**: 📋 TODO

**As a** developer
**I want** to analyze breaking changes and prepare the codebase for Spring Boot 3.5 upgrade
**So that** we have a clear migration plan with identified risks and effort estimates

**Description**:
Perform comprehensive analysis of the codebase to identify all code, configuration, and dependencies affected by Spring Boot 3.5 breaking changes. Create feature branch, baseline metrics, and document findings.

**Acceptance Criteria**:
- [ ] Feature branch created: `upgrade/spring-boot-3.5`
- [ ] Backup tag created: `before-spring-boot-3.5-upgrade`
- [ ] Baseline test results documented (unit + integration pass rates)
- [ ] Baseline performance metrics captured (startup time, response times, memory)
- [ ] All breaking change searches completed (see checklist)
- [ ] Impact assessment documented (files affected, estimated effort)
- [ ] Risk assessment completed and documented
- [ ] Upgrade approach decided (gradual vs direct)

**Breaking Change Searches**:
```bash
# TaskExecutor references (HIGH IMPACT)
grep -r '@Qualifier.*taskExecutor' rag-*/src/main/java/

# TestRestTemplate usage (LOW IMPACT - tests only)
grep -r 'TestRestTemplate' rag-*/src/test/java/
grep -r 'ENABLE_REDIRECTS' rag-*/src/test/java/

# Boolean property validation (LOW IMPACT)
grep -r 'enabled:' rag-*/src/main/resources/ | grep -v 'true\|false'

# Deprecated properties (MEDIUM IMPACT)
grep -r 'spring.mvc.converters.preferred-json-mapper' rag-*/src/main/resources/
grep -r 'spring.codec' rag-*/src/main/resources/
```

**Deliverables**:
- [ ] Analysis report with all findings documented in upgrade guide
- [ ] List of affected files with line numbers
- [ ] Effort estimate for Phase 2 (POM updates) and Phase 3 (code changes)
- [ ] Decision: Gradual (3.2→3.4→3.5) or Direct (3.2→3.5) upgrade path

**Definition of Done**:
- [ ] All searches executed and results documented
- [ ] Impact assessment reviewed and approved
- [ ] Phase 2 story refined with specific file changes needed
- [ ] Upgrade checklist started (first section completed)

**References**:
- [Spring Boot 3.5 Upgrade Guide](docs/development/SPRING_BOOT_3.5_UPGRADE_GUIDE.md)
- [Spring Boot 3.5 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.5-Release-Notes)

---

### STORY-031: Spring Boot 3.5 Upgrade - Phase 2: POM Updates
**Priority**: P1 - High
**Type**: Platform Upgrade
**Estimated Effort**: 3 Story Points
**Sprint**: Sprint 3
**Status**: 📋 TODO
**Depends On**: STORY-030

**As a** developer
**I want** all Maven POM files updated to Spring Boot 3.5.x compatible versions
**So that** dependency management is correct before code changes

**Description**:
Update root `pom.xml` and all service POMs with Spring Boot 3.5.x, Spring Cloud 2025.0.x, and compatible dependency versions. Remove redundant version overrides now managed by Spring Boot BOM.

**POM Changes Required**:

**Root pom.xml Updates**:
```xml
<!-- MUST UPDATE -->
<spring-boot.version>3.5.10</spring-boot.version>  <!-- from 3.2.11 -->
<spring-cloud.version>2025.0.1</spring-cloud.version>  <!-- from 2023.0.3 -->
<spring-ai.version>1.1.0-RC1</spring-ai.version>  <!-- from 1.0.0-M1 -->

<!-- REMOVE (managed by Boot BOM) -->
<!-- <spring-security.version>6.2.8</spring-security.version> -->
<!-- <spring-framework.version>6.1.21</spring-framework.version> -->
<!-- <testcontainers.version>1.19.8</testcontainers.version> -->
<!-- <kafka.version>3.7.0</kafka.version> -->
```

**Acceptance Criteria**:
- [ ] `spring-boot.version` updated to 3.5.x (latest patch version)
- [ ] `spring-cloud.version` updated to 2025.0.x
- [ ] `spring-ai.version` updated to 1.1.0-RC1 or 1.0.3 (stable)
- [ ] Redundant version properties removed (security, framework, kafka, testcontainers)
- [ ] Spring Cloud BOM import verified in dependencyManagement
- [ ] `mvn dependency:tree` output reviewed for conflicts
- [ ] Dependency tree comparison shows expected version changes
- [ ] All POMs validate: `mvn validate`

**Validation Steps**:
1. **Dependency Tree Analysis**:
   ```bash
   mvn dependency:tree > dependency-tree-3.5.txt
   # Compare with baseline: dependency-tree-3.2.txt
   ```

2. **Verify Key Dependencies**:
   - Spring Boot: 3.5.x
   - Spring Framework: 6.2.x (managed by Boot BOM)
   - Spring Security: 6.5.x (managed by Boot BOM)
   - Spring Cloud: 2025.0.x
   - Kafka: 3.9.x (managed by Boot BOM)
   - Testcontainers: 1.21.x (managed by Boot BOM)

3. **Check for Conflicts**:
   ```bash
   mvn dependency:tree | grep CONFLICT
   ```

**Deliverables**:
- [ ] Updated root `pom.xml`
- [ ] Dependency tree comparison report
- [ ] List of version changes (managed dependencies)
- [ ] Conflict resolution documentation (if any)

**Definition of Done**:
- [ ] Maven validate passes
- [ ] No dependency conflicts
- [ ] Dependency tree reviewed and approved
- [ ] Changes committed to feature branch

**References**:
- [Spring Boot 3.5 Upgrade Guide - POM Updates](docs/development/SPRING_BOOT_3.5_UPGRADE_GUIDE.md#phase-2-update-parent-pom-15-minutes)
- [Spring Boot Dependency Versions](https://docs.spring.io/spring-boot/appendix/dependency-versions/index.html)

---

### STORY-032: Spring Boot 3.5 Upgrade - Phase 3: Configuration Updates
**Priority**: P1 - High
**Type**: Platform Upgrade
**Estimated Effort**: 2 Story Points
**Sprint**: Sprint 3
**Status**: 📋 TODO
**Depends On**: STORY-031

**As a** developer
**I want** all application.yml configuration files updated for Spring Boot 3.5 compatibility
**So that** services start correctly with new configuration requirements

**Description**:
Update all `application.yml` files across microservices to comply with Spring Boot 3.5 configuration changes, including actuator security, deprecated property replacements, and new defaults.

**Configuration Changes**:

**1. Actuator Heapdump Security (All Services)**
```yaml
# Only add to dev/docker profiles if heapdump needed
management:
  endpoints:
    web:
      exposure:
        include: heapdump  # Explicit exposure required
  endpoint:
    heapdump:
      access: unrestricted  # Or 'restricted' with role-based access
```

**2. Deprecated Property Migration (If Found)**
```yaml
# OLD (deprecated)
spring.mvc.converters.preferred-json-mapper: jackson
spring.codec.log-request-details: true
spring.codec.max-in-memory-size: 10MB

# NEW (Spring 3.5)
spring.http.converters.preferred-json-mapper: jackson
spring.http.codecs.log-request-details: true
spring.http.codecs.max-in-memory-size: 10MB
```

**3. Tomcat APR (Optional - Production Only)**
```yaml
server:
  tomcat:
    use-apr: when-available  # Performance optimization
```

**Files to Update**:
- [ ] `rag-auth-service/src/main/resources/application.yml`
- [ ] `rag-document-service/src/main/resources/application.yml`
- [ ] `rag-embedding-service/src/main/resources/application.yml`
- [ ] `rag-core-service/src/main/resources/application.yml`
- [ ] `rag-admin-service/src/main/resources/application.yml`

**Acceptance Criteria**:
- [ ] All deprecated properties replaced with new equivalents
- [ ] Actuator heapdump configuration added to dev profiles only
- [ ] Tomcat APR configuration documented (not enabled in dev)
- [ ] Profile names validated (docker, default, test - all valid)
- [ ] No boolean property value errors (all true/false)
- [ ] Redis configuration verified (using host/port - no changes)

**Validation Steps**:
```bash
# 1. Search for deprecated properties
grep -r 'spring.mvc.converters.preferred-json-mapper' rag-*/src/main/resources/
grep -r 'spring.codec' rag-*/src/main/resources/

# 2. Validate boolean values
grep -r 'enabled:' rag-*/src/main/resources/ | grep -v 'true\|false'

# 3. Verify profile names
grep -r 'spring.profiles' rag-*/src/main/resources/

# 4. Check Redis config (should use host/port, not url)
grep -r 'spring.data.redis' rag-*/src/main/resources/
```

**Definition of Done**:
- [ ] All application.yml files updated
- [ ] Validation searches show no deprecated properties
- [ ] Configuration documented in upgrade guide
- [ ] Changes committed to feature branch

**References**:
- [Spring Boot 3.5 Upgrade Guide - Configuration Updates](docs/development/SPRING_BOOT_3.5_UPGRADE_GUIDE.md#phase-3-configuration-updates-30-minutes)
- [Spring Boot 3.5 Release Notes - Breaking Changes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.5-Release-Notes)

---

### STORY-033: Spring Boot 3.5 Upgrade - Phase 4: Code Changes
**Priority**: P1 - High
**Type**: Platform Upgrade
**Estimated Effort**: 5 Story Points
**Sprint**: Sprint 3
**Status**: 📋 TODO
**Depends On**: STORY-032

**As a** developer
**I want** all Java code updated for Spring Boot 3.5 API changes
**So that** services compile and run without deprecation warnings or errors

**Description**:
Update all Java code affected by Spring Boot 3.5 breaking changes, including TaskExecutor bean qualifiers, TestRestTemplate redirect handling, and any other API changes.

**Code Changes Required**:

**1. TaskExecutor Bean Qualifier (HIGH PRIORITY)**
```java
// BEFORE (Spring Boot 3.2.x)
@Autowired
@Qualifier("taskExecutor")
private TaskExecutor taskExecutor;

// AFTER (Spring Boot 3.5.x)
@Autowired
@Qualifier("applicationTaskExecutor")
private TaskExecutor taskExecutor;

// OR (simpler - uses default)
@Autowired
private TaskExecutor applicationTaskExecutor;
```

**Search Command**:
```bash
grep -r '@Qualifier.*taskExecutor' rag-*/src/main/java/ --include="*.java" -n
```

**2. TestRestTemplate Redirect Handling (TEST CODE ONLY)**
```java
// BEFORE
TestRestTemplate template = new TestRestTemplate(HttpOption.ENABLE_REDIRECTS);

// AFTER
TestRestTemplate template = new TestRestTemplate()
    .withRedirects(HttpClientOption.ENABLE_REDIRECTS);
```

**Search Commands**:
```bash
grep -r 'TestRestTemplate' rag-*/src/test/java/ --include="*.java" -n
grep -r 'ENABLE_REDIRECTS' rag-*/src/test/java/ --include="*.java" -n
```

**Acceptance Criteria**:
- [ ] All `@Qualifier("taskExecutor")` replaced with `@Qualifier("applicationTaskExecutor")`
- [ ] All `HttpOption.ENABLE_REDIRECTS` replaced with `HttpClientOption.ENABLE_REDIRECTS`
- [ ] No deprecation warnings during compilation
- [ ] All services compile successfully: `mvn clean install -DskipTests`
- [ ] No Spring Boot 3.5 API compatibility errors

**Affected Files** (to be determined from search):
- [ ] File: _________________ (line: ___)
- [ ] File: _________________ (line: ___)
- [ ] File: _________________ (line: ___)

**Validation Steps**:
1. **Clean Build**:
   ```bash
   mvn clean install -DskipTests
   ```

2. **Check for Warnings**:
   ```bash
   mvn compile 2>&1 | grep -i "warning\|deprecated"
   ```

3. **Verify Imports**:
   ```bash
   # Ensure correct imports for HttpClientOption
   grep -r "import.*HttpClientOption" rag-*/src/test/java/
   ```

**Definition of Done**:
- [ ] All code changes completed
- [ ] Services compile without errors
- [ ] No deprecation warnings
- [ ] Code review completed
- [ ] Changes committed to feature branch

**References**:
- [Spring Boot 3.5 Upgrade Guide - Code Updates](docs/development/SPRING_BOOT_3.5_UPGRADE_GUIDE.md#phase-4-code-updates-1-2-hours)
- [Spring Boot 3.5 Release Notes - TaskExecutor](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.5-Release-Notes#taskexecutor-bean-names)

---

### STORY-034: Spring Boot 3.5 Upgrade - Phase 5: Testing & Validation
**Priority**: P0 - Critical
**Type**: Platform Upgrade
**Estimated Effort**: 7 Story Points
**Sprint**: Sprint 3
**Status**: 📋 TODO
**Depends On**: STORY-033

**As a** developer
**I want** comprehensive testing of the Spring Boot 3.5 upgrade
**So that** we can confidently deploy to production with no regressions

**Description**:
Execute full test suite (unit, integration, E2E), perform Docker-based testing, validate Spring AI compatibility, and measure performance against baseline metrics.

**Testing Phases**:

**Phase 1: Unit Tests (1 hour)**
```bash
mvn clean test
```
- [ ] All unit tests pass
- [ ] No new test failures
- [ ] Test execution time comparable to baseline

**Phase 2: Integration Tests (1 hour)**
```bash
mvn verify -pl rag-integration-tests
```
- [ ] All integration tests pass
- [ ] No new test failures
- [ ] Services integrate correctly

**Phase 3: Docker Build & Startup (1 hour)**
```bash
make clean-all
make build-all
make start
```
- [ ] All services build successfully
- [ ] All services start without errors
- [ ] All health checks pass: `./scripts/utils/service-status.sh`

**Phase 4: Spring AI Compatibility Testing (2 hours)**

**Critical**: Spring AI 1.1.0-RC1 built against Spring Boot 3.4.2, not 3.5.x

**Ollama Embeddings**:
```bash
curl -X POST http://localhost:8083/api/v1/embeddings/generate \
  -H 'Content-Type: application/json' \
  -H 'X-Tenant-ID: YOUR_TENANT_ID' \
  -d '{"texts":["test"]}'
```
- [ ] Embedding generation succeeds
- [ ] Returns 1024-dimensional vectors
- [ ] Response time acceptable (<100ms)
- [ ] No Spring AI errors in logs

**OpenAI Embeddings** (if API key configured):
```bash
# Same test with openai profile
```
- [ ] OpenAI embedding generation works
- [ ] Returns 1536-dimensional vectors
- [ ] No compatibility errors

**Check Logs**:
```bash
grep -i "spring.*ai" logs/*.log | grep -i "warn\|error\|deprecated"
```
- [ ] No Spring AI compatibility warnings
- [ ] No unexpected errors

**Phase 5: Functional Testing (2 hours)**

**Authentication Flow**:
- [ ] Admin login: `./scripts/utils/admin-login.sh`
- [ ] User registration (Swagger UI)
- [ ] JWT token validation

**Document Processing Pipeline**:
```bash
TENANT_ID="..." # From admin-login
curl -X POST http://localhost:8082/api/v1/documents/upload \
  -H "X-Tenant-ID: $TENANT_ID" \
  -F 'file=@test.txt'
```
- [ ] Document uploads successfully
- [ ] Document processing completes (chunks created)
- [ ] Embeddings generated
- [ ] Status updates to PROCESSED

**Query Execution**:
```bash
curl -X POST http://localhost:8084/api/v1/query \
  -H "X-Tenant-ID: $TENANT_ID" \
  -H 'Content-Type: application/json' \
  -d '{"query":"test","topK":5}'
```
- [ ] Query executes
- [ ] Results returned
- [ ] Response time acceptable

**Phase 6: Performance Validation (1 hour)**

**Baseline Comparison**:
- [ ] Startup time: _______ (baseline: _______)
- [ ] Document upload response time: _______ (baseline: _______)
- [ ] Query response time: _______ (baseline: _______)
- [ ] Memory usage: _______ (baseline: _______)
- [ ] CPU usage: _______ (baseline: _______)

**Resource Monitoring**:
```bash
docker stats
```
- [ ] Memory usage <80% of limits
- [ ] CPU usage <70% under load
- [ ] No resource leaks

**Acceptance Criteria**:
- [ ] All unit tests pass (58 tests)
- [ ] All integration tests pass (13 tests)
- [ ] All services start successfully
- [ ] All health checks pass
- [ ] Spring AI embeddings work (Ollama + OpenAI)
- [ ] Document processing pipeline works end-to-end
- [ ] Query execution works
- [ ] No performance regressions (within 10% of baseline)
- [ ] No memory leaks detected
- [ ] E2E tests pass (or known blockers documented)

**Critical Spring AI Test**:
- [ ] **BLOCKER**: If Spring AI fails, consider rollback or gradual upgrade path (3.2→3.4→3.5)

**Definition of Done**:
- [ ] All testing phases completed
- [ ] Test results documented
- [ ] Performance comparison documented
- [ ] Any issues documented with workarounds
- [ ] Upgrade guide updated with findings
- [ ] Sign-off obtained for merge

**Rollback Criteria**:
- Spring AI embedding generation fails
- >20% performance regression
- Critical functionality broken
- Memory leaks detected

**References**:
- [Spring Boot 3.5 Upgrade Guide - Testing](docs/development/SPRING_BOOT_3.5_UPGRADE_GUIDE.md#phase-5-build-and-test-2-4-hours)
- [Spring Boot 3.5 Upgrade Checklist - Testing Section](docs/development/SPRING_BOOT_3.5_UPGRADE_CHECKLIST.md#build-and-test)



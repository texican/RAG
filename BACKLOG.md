# RAG System - Product Backlog

**Last Updated**: 2025-10-03
**Sprint**: E2E Testing & Bug Fixes

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

### STORY-002: Enable E2E Test Suite Execution
**Priority**: P0 - Critical
**Type**: Story
**Estimated Effort**: 2 Story Points
**Sprint**: Current
**Depends On**: STORY-001

**As a** QA engineer
**I want** the E2E test suite to run successfully
**So that** we can validate the complete RAG pipeline

**Description**:
Once the document upload bug is fixed, enable full execution of the comprehensive E2E test suite with all three test scenarios.

**Acceptance Criteria**:
- [ ] All 3 E2E test scenarios execute successfully
  - [ ] E2E-001: Document Upload and Processing
  - [ ] E2E-002: RAG Query Processing
  - [ ] E2E-003: Response Quality Validation
- [ ] All tests pass with real-world documents
- [ ] Test execution completes in under 10 minutes
- [ ] Test reports generated in target/failsafe-reports

**Test Scenarios**:
1. **E2E-001**: Upload 3 documents, verify chunking and embedding
2. **E2E-002**: Execute 4 queries, validate responses and sources
3. **E2E-003**: Test factual accuracy with expected values

**Definition of Done**:
- [ ] All E2E tests passing
- [ ] Test execution time measured and documented
- [ ] Test reports reviewed
- [ ] Screenshots/logs of successful run captured
- [ ] README updated with execution instructions

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

### TECH-DEBT-002: Standardize Test Naming Conventions
**Effort**: 1 Story Point

### TECH-DEBT-003: Extract Common Test Utilities
**Effort**: 3 Story Points

---

## Sprint Planning Recommendation

### Sprint 1 (Current)
- STORY-001: Fix Document Upload Bug
- STORY-002: Enable E2E Tests
- **Goal**: Get E2E tests passing

### Sprint 2
- STORY-003: Fix Admin Health Check
- STORY-004: TestContainers Fix
- STORY-005: Document Metadata
- **Goal**: Infrastructure stability

### Sprint 3
- STORY-006: Performance Benchmarking
- STORY-007: Semantic Search Quality
- STORY-008: Test Data Management
- **Goal**: Test quality enhancement

---

**Total Stories**: 14 + 3 Technical Debt Items
**Total Estimated Effort**: 73 Story Points
**Estimated Duration**: 5 Sprints (15 points per sprint)

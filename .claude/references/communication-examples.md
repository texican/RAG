# Communication Examples and Templates

**Version**: 1.0.0  
**Last Updated**: 2025-11-12  
**Purpose**: Reference examples for status updates and communication standards

---

## Status Update Template

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

---

## Example 1: Deployment Status Update

### ❌ Bad Example

```
Deployed to GKE. Services are running. Should be ready soon.
```

**Why This is Bad**:
- No test results
- "Should be ready" is vague
- No verification shown
- Hides potential issues

### ✅ Good Example

```markdown
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

---

## Example 2: Feature Implementation Status

### ❌ Bad Example

```
Document upload feature is mostly working. Just some edge cases to handle.
```

**Why This is Bad**:
- "Mostly working" is a forbidden phrase
- "Just some edge cases" minimizes issues
- No test results provided
- No specifics on what works or doesn't work

### ✅ Good Example

```markdown
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

---

## Example 3: Bug Fix Report

### ❌ Bad Example

```
Fixed the Redis connection issue. Everything should work now.
```

**Why This is Bad**:
- "Should work" shows no verification performed
- No explanation of what was actually fixed
- No test results to prove fix works
- No verification of related functionality

### ✅ Good Example

```markdown
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

---

## Problem Reporting Template

When reporting problems, always provide:

### Template Structure

```markdown
### Problem: [Concise Problem Title]

**What's Broken:**
- Component: [specific service/feature]
- Test/Feature: [what's failing]
- Error: [exact error message]

**Impact:**
- [Who is affected]
- [What functionality is blocked]
- [What depends on this]

**Investigation:**
- [What debugging has been done]
- [What causes have been ruled out]
- [What suspects remain]

**Next Steps:**
1. [Immediate action needed]
2. [Follow-up actions]
3. [Monitoring/prevention]

**Estimated Effort:** [time to fix + test]
```

### Example: Good Problem Report

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

---

## Solution Reporting Template

When reporting solutions, always provide:

### Template Structure

```markdown
### Solution: [Concise Solution Title]

**What Was Fixed:**
- [Specific issue resolved]
- [Root cause identified]
- [Changes made]

**Changes Made:**
- [File 1]: [What changed]
- [File 2]: [What changed]

**Verification Performed:**
- ✅ [Test 1 result]
- ✅ [Test 2 result]
- ✅ [Manual validation]

**Test Results:** X/Y tests passing (Z%)

**Potential Side Effects:**
- [What else might be impacted]
- [What to monitor]

**Documentation Updated:**
- ✅ [Doc 1]
- ✅ [Doc 2]

**Production Ready:** [Yes/No + explanation]
```

---

## Communication Standards Quick Reference

### Test Results

**ALWAYS quantify explicitly**:
- ✅ "594/600 tests passing (99%)"
- ✅ "6 tests failing in rag-core-service (known Redis issues)"
- ❌ "Tests mostly pass"
- ❌ "Most tests green"

**NEVER minimize failures**:
- ✅ "12 integration tests failing - deployment blocked"
- ✅ "Critical: Authentication test failing - no users can login"
- ❌ "Just a few test failures"
- ❌ "Minor test issues"

### Service Status

**ALWAYS verify before claiming**:
```bash
# Verify FIRST
curl http://localhost:8081/actuator/health

# Then report accurately
✅ "Service health: UP, readiness: UP, liveness: UP"
❌ "Service is running" (without verification)
```

**ALWAYS report pod status accurately**:
- ✅ "Pod status: CrashLoopBackOff - database connection failing"
- ✅ "Container running but health checks failing (not ready)"
- ❌ "Container is up"
- ❌ "Deployment successful" (when pods not ready)

### Work Completion

**ALWAYS lead with what's NOT done**:
- ✅ "Blockers: [list], Remaining: [list], Completed: [list]"
- ❌ "Completed: [list], Just need to finish: [list]"

**ALWAYS quantify remaining work**:
- ✅ "60% complete (6/10 features), 4 remaining: [list]"
- ❌ "Almost done, just a few more things"

---

## Status Reporting Frequency Guide

### Daily Updates (for active work)
- Current progress with exact percentages
- New blockers identified
- Test results from day's work
- Next day's plan

### Immediate Notification (critical issues)
- Any test failure introduced by new code
- Any service going unhealthy
- Any deployment failure
- Any production incident

### After Every Significant Change
- Complete test results (not just diff)
- Impact assessment
- Verification performed
- Related components checked

### End of Task Summary
- Full validation checklist
- All test results
- All documentation updated
- Blockers resolved
- Handoff notes

---

## Communication Checklist

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

---

**See Also**:
- Main agent instructions: `../ agent-instructions.md`
- Communication standards: `../agent-instructions.md#communication-and-honesty-standards`
- Forbidden/required phrases: `../agent-instructions.md#forbidden-phrases`

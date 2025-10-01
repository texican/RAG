# Scripts Audit Report - Complete Analysis

**Audit Date**: 2025-09-30
**Total Scripts**: 22
**Total Lines**: ~8,200
**Context**: Post-ADR-001 Gateway Bypass

---

## 📊 Executive Summary

**Status Overview**:
- ✅ **Critical Scripts (6)**: Updated and functional
- ⚠️  **Gateway References (15)**: Require updates for ADR-001
- ✅ **No Gateway References (7)**: Already correct
- 📦 **Obsolete (0)**: All scripts still relevant

**Key Findings**:
1. **15 scripts contain gateway references** that need updating
2. **All scripts are still useful** - no candidates for removal
3. **2 already updated** (docker-deploy.sh, k8s-deploy.sh, start-all-services.sh)
4. **Inconsistent port numbers** (admin service: 8085 vs 8086)

---

## 🔍 Detailed Script Analysis

### **Category 1: Deployment Scripts** (2 scripts)
**Purpose**: Automate deployment to various environments

| Script | Lines | Status | Gateway Refs | Update Needed |
|--------|-------|--------|--------------|---------------|
| `deploy/docker-deploy.sh` | 742 | ✅ Updated | 0 (fixed) | ✅ Complete |
| `deploy/k8s-deploy.sh` | 704 | ✅ Updated | 0 (fixed) | ✅ Complete |

**Assessment**: ✅ **Both deployment scripts already updated**
- Gateway removed from service arrays
- Ports updated (admin: 8086)
- ADR-001 references added
- Fully functional for gateway-less deployment

---

### **Category 2: Service Management** (2 scripts)
**Purpose**: Start, stop, and manage services

| Script | Lines | Status | Gateway Refs | Update Needed |
|--------|-------|--------|--------------|---------------|
| `services/start-all-services.sh` | 56 | ✅ Updated | 0 (fixed) | ✅ Complete |
| `services/stop-all-services.sh` | 11 | ✅ Good | 0 | ✅ None |

**Assessment**: ✅ **Service scripts updated**
- `start-all-services.sh`: Already updated with correct ports
- `stop-all-services.sh`: Generic, no hardcoded services

---

### **Category 3: Utility Scripts** (9 scripts)
**Purpose**: Common operations, health checks, status monitoring

| Script | Lines | Status | Gateway Refs | Priority |
|--------|-------|--------|--------------|----------|
| `utils/health-check.sh` | 69 | ✅ Updated | 0 (fixed) | ✅ Done |
| `utils/service-status.sh` | 84 | ⚠️  Needs Update | Yes (8080, 8085) | 🔥 HIGH |
| `utils/wait-for-services.sh` | 70 | ⚠️  Needs Update | Yes (8080, 8085) | 🔥 HIGH |
| `utils/quick-start.sh` | 45 | ⚠️  Needs Update | Yes (8080) | 🔥 HIGH |
| `utils/get-swagger-passwords.sh` | 79 | ⚠️  Needs Update | Yes (gateway ref) | 🔴 MEDIUM |
| `utils/dev-reset.sh` | 74 | ✅ Good | No | ✅ None |
| `db/create-admin-user.sh` | 121 | ✅ Good | No | ✅ None |
| `validate-backlog.sh` | 108 | ✅ Good | No | ✅ None |

**Assessment**: ⚠️  **5 of 8 utility scripts need updates**

#### **Critical Issues**:
1. **service-status.sh**: Still checks gateway (8080), wrong admin port (8085)
2. **wait-for-services.sh**: Waits for gateway, wrong ports
3. **quick-start.sh**: Tries to start gateway
4. **get-swagger-passwords.sh**: Extracts gateway password

---

### **Category 4: Testing Scripts** (2 scripts)
**Purpose**: Run tests and validate completeness

| Script | Lines | Status | Gateway Refs | Priority |
|--------|-------|--------|--------------|----------|
| `tests/test-system.sh` | 71 | ⚠️  Needs Update | Yes (8080, 8085) | 🔥 HIGH |
| `tests/story-completion-test-check.sh` | 117 | ⚠️  Needs Update | Yes (gateway tests) | 🔴 MEDIUM |

**Assessment**: ⚠️  **Both test scripts need updates**
- test-system.sh tests gateway endpoints
- story-completion-test-check.sh validates gateway tests

---

### **Category 5: Development Scripts** (2 scripts)
**Purpose**: Development tools, benchmarks, integration tests

| Script | Lines | Status | Gateway Refs | Priority |
|--------|-------|--------|--------------|----------|
| `dev/performance-benchmark.sh` | 804 | ⚠️  Needs Update | Yes (extensive) | 🔴 MEDIUM |
| `dev/run-integration-tests.sh` | 570 | ⚠️  Needs Update | Yes (8080, 8085) | 🔴 MEDIUM |

**Assessment**: ⚠️  **Both dev scripts need updates**
- performance-benchmark.sh has extensive gateway benchmarks
- run-integration-tests.sh runs gateway integration tests

---

### **Category 6: Quality/Setup** (2 scripts)
**Purpose**: System setup and quality validation

| Script | Lines | Status | Gateway Refs | Priority |
|--------|-------|--------|--------------|----------|
| `quality/validate-system.sh` | 210 | ✅ Good | No hardcoded | ✅ None |
| `setup/setup-local-dev.sh` | 672 | ⚠️  Needs Update | Yes (8080 checks) | 🔥 HIGH |

**Assessment**: ⚠️  **Setup script needs gateway removal**
- validate-system.sh: Uses dynamic service discovery (good!)
- setup-local-dev.sh: Checks for gateway on 8080

---

### **Category 7: Monitoring/Maintenance** (4 scripts)
**Purpose**: System monitoring, alerting, backup, maintenance

| Script | Lines | Status | Gateway Refs | Priority |
|--------|-------|--------|--------------|----------|
| `monitoring/system-monitor.sh` | 698 | ⚠️  Needs Update | Yes (8080, 8085) | 🔴 MEDIUM |
| `monitoring/alerting-system.sh` | 843 | ⚠️  Needs Update | Yes (gateway alerts) | 🔴 MEDIUM |
| `maintenance/backup-system.sh` | 959 | ⚠️  Needs Update | Yes (gateway backup) | 🟡 LOW |
| `maintenance/system-maintenance.sh` | 1073 | ⚠️  Needs Update | Yes (gateway maint) | 🟡 LOW |

**Assessment**: ⚠️  **All 4 scripts need updates**
- These are large, comprehensive scripts
- Gateway deeply integrated into monitoring/backup logic
- Lower priority (not used in daily development)

---

## 🎯 Priority Update Matrix

### **🔥 CRITICAL (Must Update Immediately)**
These scripts are used daily and will fail with current gateway references:

1. ✅ **utils/health-check.sh** - UPDATED
2. **utils/service-status.sh** - Shows which services are running
3. **utils/wait-for-services.sh** - Used by other scripts
4. **utils/quick-start.sh** - Main entry point for users
5. **tests/test-system.sh** - Integration testing
6. **setup/setup-local-dev.sh** - New user onboarding

### **🔴 HIGH (Update Soon)**
Used regularly, but have workarounds:

7. **utils/get-swagger-passwords.sh** - Get service credentials
8. **tests/story-completion-test-check.sh** - Story validation
9. **dev/run-integration-tests.sh** - Integration testing
10. **dev/performance-benchmark.sh** - Performance testing
11. **monitoring/system-monitor.sh** - System health
12. **monitoring/alerting-system.sh** - Alerts

### **🟡 LOW (Update When Convenient)**
Rarely used, advanced features:

13. **maintenance/backup-system.sh** - Backup/restore
14. **maintenance/system-maintenance.sh** - Maintenance tasks

---

## 🔧 Required Changes Summary

### **Port Updates Needed**
```bash
# Change these ports across all scripts:
8080 → REMOVE (gateway archived)
8085 → 8086 (admin service actual port)

# Correct service ports:
8081 - Auth Service
8082 - Document Service
8083 - Embedding Service
8084 - Core Service
8086 - Admin Service (NOT 8085!)
```

### **Service Array Updates**
Remove gateway from all service arrays:
```bash
# OLD
services=("rag-auth" "rag-document" "rag-embedding" "rag-core" "rag-admin" "rag-gateway")

# NEW
services=("rag-auth" "rag-document" "rag-embedding" "rag-core" "rag-admin")
```

### **URL Pattern Updates**
```bash
# OLD (via gateway)
http://localhost:8080/api/auth/login

# NEW (direct)
http://localhost:8081/auth/login
```

---

## 📋 Detailed Update Recommendations

### **Tier 1: Quick Fixes** (5-10 minutes each)

#### 1. **utils/service-status.sh**
**Lines to change**: ~10-15
**Changes**:
- Remove gateway from services array
- Update ports (8085 → 8086)
- Add ADR-001 note

#### 2. **utils/wait-for-services.sh**
**Lines to change**: ~10
**Changes**:
- Remove gateway wait logic
- Update port checks
- Fix admin port

#### 3. **utils/quick-start.sh**
**Lines to change**: ~15
**Changes**:
- Remove gateway startup
- Update URLs shown to user
- Add direct access note

#### 4. **tests/test-system.sh**
**Lines to change**: ~20
**Changes**:
- Remove gateway test endpoints
- Update service URLs
- Fix port references

---

### **Tier 2: Moderate Updates** (15-30 minutes each)

#### 5. **utils/get-swagger-passwords.sh**
**Lines to change**: ~25
**Changes**:
- Remove gateway password extraction
- Update service list
- Fix admin port

#### 6. **setup/setup-local-dev.sh**
**Lines to change**: ~30-40
**Changes**:
- Remove gateway from setup flow
- Update health check URLs
- Update verification steps
- Fix all port references

#### 7. **tests/story-completion-test-check.sh**
**Lines to change**: ~25
**Changes**:
- Remove gateway test requirements
- Update test count expectations
- Fix service-specific test checks

---

### **Tier 3: Complex Updates** (1-2 hours each)

#### 8. **dev/performance-benchmark.sh**
**Lines to change**: ~100+
**Changes**:
- Remove all gateway benchmark tests
- Update direct service benchmarks
- Recalculate performance baselines
- Update reporting

#### 9. **dev/run-integration-tests.sh**
**Lines to change**: ~50
**Changes**:
- Remove gateway integration tests
- Update test expectations (151 fewer tests)
- Fix service discovery
- Update reporting

#### 10. **monitoring/system-monitor.sh**
**Lines to change**: ~60
**Changes**:
- Remove gateway from monitoring
- Update dashboard metrics
- Fix alert thresholds
- Update service health checks

#### 11. **monitoring/alerting-system.sh**
**Lines to change**: ~50
**Changes**:
- Remove gateway alerts
- Update service count (6 → 5)
- Fix notification templates

---

### **Tier 4: Low Priority** (defer for now)

#### 12. **maintenance/backup-system.sh**
**Changes**: Remove gateway from backup/restore logic
**Complexity**: High (100+ lines)
**Priority**: Low (rarely used)

#### 13. **maintenance/system-maintenance.sh**
**Changes**: Remove gateway from maintenance tasks
**Complexity**: High (100+ lines)
**Priority**: Low (rarely used)

---

## ✅ Scripts That DON'T Need Updates

These scripts are **already correct** and require no changes:

1. ✅ **services/stop-all-services.sh** - Generic process killing
2. ✅ **utils/dev-reset.sh** - Generic cleanup
3. ✅ **db/create-admin-user.sh** - Database only
4. ✅ **validate-backlog.sh** - File validation only
5. ✅ **quality/validate-system.sh** - Dynamic service discovery
6. ✅ **deploy/docker-deploy.sh** - Already updated
7. ✅ **deploy/k8s-deploy.sh** - Already updated
8. ✅ **services/start-all-services.sh** - Already updated
9. ✅ **utils/health-check.sh** - Already updated

---

## 🎯 Recommended Action Plan

### **Phase 1: Critical Scripts** (Complete First)
**Time**: ~2 hours
**Impact**: High - These are used daily

1. ✅ utils/health-check.sh (DONE)
2. utils/service-status.sh
3. utils/wait-for-services.sh
4. utils/quick-start.sh
5. tests/test-system.sh
6. setup/setup-local-dev.sh

### **Phase 2: Development Scripts** (Next Priority)
**Time**: ~4 hours
**Impact**: Medium - Used for testing/development

7. utils/get-swagger-passwords.sh
8. tests/story-completion-test-check.sh
9. dev/run-integration-tests.sh
10. dev/performance-benchmark.sh

### **Phase 3: Operations Scripts** (Lower Priority)
**Time**: ~3 hours
**Impact**: Low - Advanced features, rarely used

11. monitoring/system-monitor.sh
12. monitoring/alerting-system.sh
13. maintenance/backup-system.sh
14. maintenance/system-maintenance.sh

---

## 📊 Impact Analysis

### **Before Updates**
- **Total Scripts**: 22
- **Scripts with Gateway Refs**: 15 (68%)
- **Working Scripts**: 7 (32%)
- **Risk**: High (scripts will fail or give wrong information)

### **After Updates**
- **Total Scripts**: 22
- **Scripts with Gateway Refs**: 0 (0%)
- **Working Scripts**: 22 (100%)
- **Risk**: None (all scripts aligned with ADR-001)

---

## 🔍 Testing Recommendations

After updating each script, test it:

```bash
# Test utility scripts
./scripts/utils/health-check.sh
./scripts/utils/service-status.sh
./scripts/utils/wait-for-services.sh

# Test with services running
docker-compose up -d
./scripts/utils/quick-start.sh

# Test deployment
./scripts/deploy/docker-deploy.sh --environment dev

# Test integration
./scripts/tests/test-system.sh
```

---

## 📝 Summary

**Current Status**:
- ✅ 9 scripts already correct or updated
- ⚠️  13 scripts need gateway reference removal
- 📦 0 scripts obsolete (all still useful)

**Effort Required**:
- **Phase 1** (Critical): ~2 hours
- **Phase 2** (Development): ~4 hours
- **Phase 3** (Operations): ~3 hours
- **Total**: ~9 hours of work

**Priority**:
1. Complete Phase 1 scripts immediately (daily use)
2. Schedule Phase 2 for next sprint (testing impact)
3. Defer Phase 3 until needed (advanced features)

---

## 🔗 Related Documentation

- [ADR-001: Bypass API Gateway](../development/ADR-001-BYPASS-API-GATEWAY.md)
- [Gateway Archival Summary](GATEWAY_ARCHIVAL_SUMMARY.md)
- [Deployment Scripts Update](DEPLOYMENT_SCRIPTS_UPDATE_SUMMARY.md)

---

**Audit Completed**: 2025-09-30
**Next Review**: After Phase 1 updates complete
**Status**: ⚠️  **ACTION REQUIRED** - 13 scripts need updates

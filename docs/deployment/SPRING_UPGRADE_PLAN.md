# Spring Framework & Spring Boot Upgrade Plan

**Generated:** 2025-12-04
**Current Date Verified:** 2025-12-04 (December 4, 2025)

---

## Executive Summary

Your RAG project is currently using **Spring Boot 3.2.11** and **Spring Framework 6.1.21**, both of which are **end-of-life**. This document provides a comprehensive upgrade path to bring the project to actively supported versions.

### Critical Findings

🔴 **Spring Boot 3.2.x** - OSS support ended December 2024 (12 months ago)
🔴 **Spring Boot 3.4.x** - OSS support ended November 2025 (1 month ago)
🟡 **Spring Boot 3.5.x** - OSS support until May 2026 (5 months remaining)
🟡 **Spring Framework 6.1.21** - Still receiving patches but approaching EOL (Nov 2025)
✅ **Spring Boot 4.0.0** - Released November 2025, support until November 2026 (11 months)

### Recommended Target Versions

| Component | Current | Recommended | Status |
|-----------|---------|-------------|--------|
| Spring Boot | 3.2.11 | **4.0.0** | 🔴 Critical Upgrade Required |
| Spring Framework | 6.1.21 | **7.0.0** | 🔴 Major Version Jump |
| Spring Security | 6.2.8 | 7.0.x | 🔴 Upgrade Required |
| Spring Cloud | 2023.0.3 | 2025.0.x | 🔴 Upgrade Required |
| Java | 17+ | **21+** (23 recommended) | ⚠️ JDK Upgrade Required |

---

## Version Timeline & Support Status

### Spring Boot Support Timeline (as of December 2025)

| Version | Released | OSS EOL | Status |
|---------|----------|---------|--------|
| 3.2.x | Nov 2023 | Dec 2024 | 🔴 End of Life (12 months ago) |
| 3.3.x | May 2024 | Jun 2025 | 🔴 End of Life (6 months ago) |
| 3.4.x | Nov 2024 | Nov 2025 | 🔴 End of Life (1 month ago) |
| 3.5.x | May 2025 | May 2026 | 🟡 Active (5 months remaining) |
| 4.0.x | Nov 2025 | Nov 2026 | ✅ Active (11 months remaining) |

**Source:** [Spring Boot End of Life](https://endoflife.date/spring-boot)

### Spring Framework Support Timeline

| Version | Released | OSS EOL | Status |
|---------|----------|---------|--------|
| 6.1.x | Nov 2023 | Nov 2025 | 🔴 End of Life (1 month ago) |
| 6.2.x | Nov 2024 | Nov 2026 | ✅ Active |
| 7.0.x | Nov 2025 | Nov 2027 | ✅ Active (comes with Spring Boot 4.0) |

**Source:** [Spring Framework End of Life](https://endoflife.date/spring-framework)

---

## Why Skip Spring Boot 3.5 and Go to 4.0?

### The Math (as of December 2025)

**Spring Boot 3.5.x Option:**
- Support remaining: **5 months** (until May 2026)
- Effort: 3-4 days
- Result: Another upgrade needed in 5 months

**Spring Boot 4.0.0 Option:**
- Support remaining: **11 months** (until Nov 2026)
- Effort: 5-7 days (more breaking changes)
- Result: Stable until late 2026, skip intermediate upgrades

### Recommendation: Go Straight to Spring Boot 4.0

**Why:**
1. ✅ **Double the runway** - 11 months vs 5 months
2. ✅ **Avoid double upgrade** - Don't upgrade to 3.5 now and 4.0 in May
3. ✅ **Latest features** - Access to Spring Framework 7.0, Java 21+ features
4. ✅ **Better ROI** - Extra 2-3 days of work = 6 months more stability

**When 3.5 Makes Sense:**
- ❌ None - With only 5 months of support, it's not worth it

---

## Spring Boot 4.0 Breaking Changes

**Source:** [Spring Boot 4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)

### 1. Java Version Requirement

**Breaking Change:** Minimum Java 17, **Java 21+ recommended** (Java 23 optimal)

**Impact:** HIGH - Requires JDK upgrade
- Current project likely uses Java 17
- Need to upgrade to Java 21 for optimal performance
- GraalVM native-image requires v25+

**Action Required:**
```xml
<java.version>21</java.version>
```

---

### 2. Spring Framework 7.0

**Breaking Change:** Spring Boot 4.0 uses **Spring Framework 7.0** (not 6.x)

**Impact:** HIGH - Major framework version
- Jakarta EE 11 baseline (Servlet 6.1)
- Many API changes
- New reactive features

**Action Required:**
- Review Spring Framework 7.0 migration guide
- Test all framework integrations

---

### 3. Deprecated APIs Removed

**Breaking Change:** 88% of deprecated APIs from 2.x/3.x removed (36 classes)

**Impact:** HIGH - Code changes required

**Specific Removals:**
- `@MockBean` and `@SpyBean` (use Mockito's `@Mock` with `MockitoExtension`)
- `WebSecurityConfigurerAdapter` (already removed in 3.x)
- Legacy test utilities and slice annotations
- `MockitoTestExecutionListener`

**Action Required:**
```bash
# Search for removed APIs
grep -r "@MockBean\|@SpyBean" rag-*/src/test/
grep -r "WebSecurityConfigurerAdapter" rag-*/src/
```

---

### 4. Jackson 3.x Upgrade

**Breaking Change:** Jackson 2.x → **Jackson 3.x** (major version)

**Impact:** MEDIUM-HIGH - JSON serialization changes

**Changes:**
- Removed annotations
- Stricter type handling
- Module consolidation
- `ObjectMapper` behavior changes
- Configuration properties moved:
  - `spring.jackson.read.*` → `spring.jackson.json.read.*`
  - `spring.jackson.write.*` → `spring.jackson.json.write.*`

**Action Required:**
- Review all custom JSON serialization/deserialization
- Test document upload/download (uses Jackson)
- Test API request/response handling
- Update configuration properties

---

### 5. JUnit 5 → JUnit 6 (Jupiter 6)

**Breaking Change:** JUnit 5 → **JUnit 6** (Jupiter Platform 6)

**Impact:** MEDIUM - Test updates required

**Changes:**
- Updated annotations
- New lifecycle methods
- New extension APIs
- Removed older testing utilities

**Action Required:**
- Review all test classes
- Update test annotations
- Verify test execution

---

### 6. Configuration Property Binding Changes

**Breaking Change:** No more binding to **public fields** in `@ConfigurationProperties`

**Impact:** LOW-MEDIUM - Configuration classes affected

**Required Pattern:**
```java
// ❌ No longer works
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    public String name;  // Public field
}

// ✅ Required
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    private String name;  // Private field

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
```

**Action Required:**
```bash
# Find @ConfigurationProperties classes
grep -r "@ConfigurationProperties" rag-*/src/
```

---

### 7. Build Tool Changes

**Breaking Change:** Classic uber-jar loader removed

**Impact:** LOW - Maven/Gradle config

**Action Required:**
- Remove any `loader.implementation` configuration from POMs
- Spring Boot Maven plugin handles this automatically

---

### 8. Undertow Support Removed

**Breaking Change:** Undertow servlet container **removed** (not Servlet 6.1 compatible)

**Impact:** NONE for this project (uses Tomcat by default)

**Your Project:** Uses embedded Tomcat (default) - no action needed

---

### 9. Spring Batch Changes

**Breaking Change:** Default in-memory mode (no database)

**Impact:** NONE for this project (doesn't use Spring Batch)

---

### 10. Kotlin Version Requirement

**Breaking Change:** Kotlin 2.2+ required (if using Kotlin)

**Impact:** NONE for this project (Java only)

---

## Service-Specific Impact Analysis

### High-Impact Services

**1. rag-auth-service** 🔴
- **Testing:** `@MockBean`/`@SpyBean` removal
- **Security:** Spring Security 7.0 changes
- **Jackson:** JWT payload serialization
- **Testing Focus:**
  - Replace `@MockBean` with Mockito's `@Mock`
  - Verify JWT token generation/validation
  - Test authentication flows
  - Verify password encoding

**2. rag-document-service** 🔴
- **Jackson 3.x:** File upload/download JSON handling
- **Testing:** Mock beans in tests
- **Servlet 6.1:** Multipart file upload API
- **Testing Focus:**
  - Document upload with multipart requests
  - JSON serialization of document metadata
  - File chunking pipeline
  - Kafka event serialization

**3. rag-embedding-service** 🔴
- **Spring AI:** Compatibility with Spring Boot 4.0
- **Jackson 3.x:** Embedding response serialization
- **Testing:** Test mocks and fixtures
- **Testing Focus:**
  - Ollama client (HTTP client changes)
  - Embedding generation
  - Vector dimension handling
  - Async processing

**4. rag-core-service** 🔴
- **Jackson 3.x:** Query request/response handling
- **Redis:** Connection handling changes
- **Kafka:** Event serialization
- **Testing Focus:**
  - Service-to-service communication
  - Query execution pipeline
  - Result serialization
  - Cache operations

**5. rag-admin-service** 🟡
- **Actuator:** Configuration changes
- **Testing:** Mock beans
- **Testing Focus:**
  - Admin authentication
  - Actuator endpoints
  - Swagger UI integration

---

## Upgrade Implementation Plan

### Prerequisites Check

**Before Starting:**
- [ ] Verify Java 21 installed: `java -version`
- [ ] All tests passing on current version: `mvn test`
- [ ] Clean build: `mvn clean package`
- [ ] Git working directory clean: `git status`
- [ ] Backup created: `git tag pre-spring-4-upgrade-$(date +%Y%m%d)`

---

### Phase 1: Preparation (Day 1, 3-4 hours)

**1.1 Create Feature Branch**
```bash
git checkout -b upgrade/spring-boot-4.0
```

**1.2 Create Backup**
```bash
git tag pre-spring-4-upgrade-20251204
git push origin pre-spring-4-upgrade-20251204
```

**1.3 Review Migration Guides**
- [ ] [Spring Boot 4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)
- [ ] [Spring Boot 4.0 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Release-Notes)
- [ ] [Spring Framework 7.0 Documentation](https://docs.spring.io/spring-framework/reference/)

**1.4 Upgrade JDK** (if needed)
```bash
# Check current Java version
java -version

# Install Java 21 (if not present)
# macOS:
brew install openjdk@21

# Set JAVA_HOME
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
```

**1.5 Document Current State**
```bash
# Capture dependency tree before upgrade
mvn dependency:tree > dependency-tree-before-upgrade.txt

# Capture current versions
mvn versions:display-dependency-updates > version-updates-available.txt
```

---

### Phase 2: Version Updates (Day 1, 2-3 hours)

**2.1 Update Parent POM**

Edit `pom.xml`:
```xml
<properties>
    <!-- Java version -->
    <java.version>21</java.version>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>

    <!-- Spring versions -->
    <spring-boot.version>4.0.0</spring-boot.version>
    <spring-framework.version>7.0.0</spring-framework.version>
    <spring-security.version>7.0.0</spring-security.version>
    <spring-cloud.version>2025.0.0</spring-cloud.version>
    <spring-ai.version>1.0.0-M5</spring-ai.version>

    <!-- Security patches -->
    <tomcat.version>11.0.x</tomcat.version>  <!-- Update to Servlet 6.1 compatible -->
    <netty.version>4.1.124.Final</netty.version>

    <!-- Testing -->
    <junit.version>5.11.0</junit.version>  <!-- JUnit Jupiter 6 -->
    <mockito.version>5.14.2</mockito.version>
</properties>
```

**2.2 Check for Version Overrides**
```bash
# Search for version overrides in child POMs
grep -r "spring-boot.version\|spring-framework.version\|spring-security.version" */pom.xml
```

**2.3 Clean and Attempt Build**
```bash
mvn clean
mvn dependency:resolve
mvn compile -DskipTests
```

**Expected:** Compilation errors - proceed to Phase 3

---

### Phase 3: Code Updates (Day 2-3, 8-12 hours)

**3.1 Replace MockBean/SpyBean**

Search and replace in test classes:
```bash
# Find usages
grep -r "@MockBean\|@SpyBean" rag-*/src/test/

# Replace with Mockito
```

**Before:**
```java
@SpringBootTest
class MyServiceTest {
    @MockBean
    private MyRepository repository;
}
```

**After:**
```java
@SpringBootTest
@ExtendWith(MockitoExtension.class)
class MyServiceTest {
    @Mock
    private MyRepository repository;

    @InjectMocks
    private MyService service;
}
```

**3.2 Update Jackson Configuration**

Edit `application.yml` in all services:
```yaml
# Before
spring:
  jackson:
    read:
      ACCEPT_SINGLE_VALUE_AS_ARRAY: true
    write:
      WRITE_DATES_AS_TIMESTAMPS: false

# After
spring:
  jackson:
    json:
      read:
        ACCEPT_SINGLE_VALUE_AS_ARRAY: true
      write:
        WRITE_DATES_AS_TIMESTAMPS: false
```

**3.3 Fix @ConfigurationProperties**

Find and fix public field bindings:
```bash
grep -r "@ConfigurationProperties" rag-*/src/main/
```

Convert public fields to private with getters/setters.

**3.4 Update Security Configuration**

Review security configs for Spring Security 7.0 changes (if any).

**3.5 Clean Build Attempt**
```bash
mvn clean compile
```

Fix any remaining compilation errors.

---

### Phase 4: Testing (Day 3-4, 12-16 hours)

**4.1 Unit Tests**
```bash
mvn test
```

**Expected Issues:**
- MockBean/SpyBean failures
- Jackson serialization differences
- JUnit annotation changes

**Fix iteratively** until all unit tests pass.

**4.2 Integration Tests**
```bash
mvn verify -pl rag-integration-tests
```

**4.3 Service-Specific Testing**

**Auth Service:**
```bash
# Start services
make start

# Test registration
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"test-sb4@example.com","password":"password123","firstName":"Test","lastName":"User"}'

# Test login
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"test-sb4@example.com","password":"password123"}'
```

**Document Service:**
```bash
TENANT_ID=$(./scripts/utils/admin-login.sh | grep "Tenant ID" | awk '{print $3}')

echo "Test document for Spring Boot 4" > /tmp/test-sb4.txt
curl -X POST http://localhost:8082/api/v1/documents/upload \
  -H "X-Tenant-ID: $TENANT_ID" \
  -F "file=@/tmp/test-sb4.txt"
```

**Embedding Service:**
```bash
curl -X POST http://localhost:8083/api/v1/embeddings/generate \
  -H 'Content-Type: application/json' \
  -H "X-Tenant-ID: $TENANT_ID" \
  -d '{"texts":["Spring Boot 4 test"]}'
```

**Core Service:**
```bash
curl -X POST http://localhost:8084/api/v1/query \
  -H 'Content-Type: application/json' \
  -H "X-Tenant-ID: $TENANT_ID" \
  -d '{"query":"What is Spring Boot 4?"}'
```

**Admin Service:**
```bash
JWT=$(./scripts/utils/admin-login.sh | grep "JWT Token" | awk '{print $3}')

curl -X GET http://localhost:8085/admin/api/actuator/health \
  -H "Authorization: Bearer $JWT"
```

**4.4 E2E Tests**
```bash
cd rag-integration-tests
mvn verify -Dmaven.test.skip=false -DskipTests=false
```

**4.5 Performance Testing**

**Startup Times:**
```bash
# Record startup time for each service
docker-compose restart rag-auth
# Check logs for startup time

# Repeat for all services
```

**Memory Usage:**
```bash
docker stats --no-stream
```

**Query Latency:**
```bash
# Run multiple queries, measure response time
for i in {1..10}; do
  time curl -X POST http://localhost:8084/api/v1/query \
    -H "X-Tenant-ID: $TENANT_ID" \
    -d '{"query":"test query"}' \
    -o /dev/null -s
done
```

**4.6 Docker Testing**
```bash
# Rebuild all images
make clean-all
make build-all

# Start services
make start

# Health checks
./scripts/utils/health-check.sh
./scripts/utils/service-status.sh
```

---

### Phase 5: Validation & Rollout (Day 5, 4-6 hours)

**5.1 Smoke Tests**
- [ ] All services start without errors
- [ ] Database migrations run successfully
- [ ] Redis connections work
- [ ] Kafka connections work
- [ ] Swagger UI accessible on all services

**5.2 Regression Testing**
- [ ] User registration and login
- [ ] Document upload and processing
- [ ] Embedding generation (Ollama)
- [ ] Query execution
- [ ] Admin operations

**5.3 Performance Validation**
- [ ] Startup times within ±10% of baseline
- [ ] Memory usage within acceptable limits
- [ ] Query latency maintained or improved

**5.4 Documentation Updates**
- [ ] Update `README.md` with Spring Boot 4.0, Java 21
- [ ] Update `CLAUDE.md` with upgrade completion
- [ ] Update `PROJECT_BACKLOG.md` - mark TECH-DEBT-006 complete
- [ ] Document any configuration changes
- [ ] Note any behavioral changes

**5.5 Commit and Merge**
```bash
git add .
git commit -m "chore: upgrade to Spring Boot 4.0.0 and Spring Framework 7.0.0

- Upgrade Java 17 → 21
- Upgrade Spring Boot 3.2.11 → 4.0.0
- Upgrade Spring Framework 6.1.21 → 7.0.0
- Replace @MockBean/@SpyBean with Mockito @Mock
- Update Jackson configuration for Jackson 3.x
- Fix @ConfigurationProperties public field bindings
- All tests passing
- Performance validated

Closes TECH-DEBT-006"

git push origin upgrade/spring-boot-4.0
# Create pull request
```

---

## Testing Checklist

### Critical Path Tests

- [ ] **Authentication Flow**
  - [ ] User registration
  - [ ] User login
  - [ ] JWT token generation
  - [ ] JWT token validation
  - [ ] Tenant creation

- [ ] **Document Processing**
  - [ ] Document upload
  - [ ] Chunking pipeline
  - [ ] Kafka event publishing
  - [ ] Kafka event consumption
  - [ ] Status updates

- [ ] **Embedding Generation**
  - [ ] Ollama embeddings (Docker profile)
  - [ ] Vector dimension validation (1024-dim)
  - [ ] Batch embedding generation

- [ ] **Query Execution**
  - [ ] Query submission
  - [ ] Vector similarity search
  - [ ] Result ranking
  - [ ] Response formatting

- [ ] **Admin Operations**
  - [ ] Admin login
  - [ ] Actuator endpoints
  - [ ] Swagger UI access

### Infrastructure Tests

- [ ] **Database**
  - [ ] PostgreSQL connection
  - [ ] Transaction management
  - [ ] Connection pooling

- [ ] **Redis**
  - [ ] Connection establishment
  - [ ] Cache operations
  - [ ] Session management

- [ ] **Kafka**
  - [ ] Producer connectivity
  - [ ] Consumer connectivity
  - [ ] Message serialization (Jackson 3.x)
  - [ ] Error handling

### Performance Tests

- [ ] **Startup Time**
  - Auth service: _____ seconds (baseline: _____)
  - Document service: _____ seconds (baseline: _____)
  - Embedding service: _____ seconds (baseline: _____)
  - Core service: _____ seconds (baseline: _____)
  - Admin service: _____ seconds (baseline: _____)

- [ ] **Memory Usage**
  - Compare heap sizes before/after
  - Monitor GC activity
  - Check for memory leaks

- [ ] **Query Latency**
  - Baseline (3.2.11): _____ ms
  - After upgrade (4.0.0): _____ ms
  - Difference: _____ ms (target: ±10%)

---

## Rollback Plan

If critical issues are discovered:

### Option 1: Git Revert (Fastest)

```bash
# Revert to pre-upgrade tag
git checkout pre-spring-4-upgrade-20251204

# Downgrade JDK if needed
export JAVA_HOME=/path/to/java17

# Rebuild and redeploy
make clean-all
make build-all
make start
```

### Option 2: Docker Rollback

```bash
# Use previous images
docker-compose down
git checkout main
make start
```

### Option 3: Selective Rollback

If only one service has issues:

```bash
# Rebuild single service from main branch
git checkout main -- rag-{service}-service/
make rebuild SERVICE=rag-{service}
```

---

## Known Issues & Workarounds

### Issue 1: MockBean/SpyBean Removal

**Issue:** Tests using `@MockBean` or `@SpyBean` fail

**Workaround:**
```java
// Replace Spring Boot test mocking
@ExtendWith(MockitoExtension.class)
class MyTest {
    @Mock
    private MyDependency dependency;

    @InjectMocks
    private MyService service;
}
```

### Issue 2: Jackson 3.x Serialization Changes

**Issue:** JSON serialization behaves differently

**Workaround:**
- Review custom `ObjectMapper` configurations
- Test all REST endpoints
- Check date/time formatting
- Verify enum serialization

### Issue 3: Configuration Property Binding

**Issue:** `@ConfigurationProperties` with public fields fail

**Workaround:**
- Convert public fields to private
- Add getters and setters
- Rebuild

---

## Post-Upgrade Monitoring

Monitor these metrics for 7-14 days after upgrade:

### Application Metrics
- Startup time trends
- Memory usage patterns
- GC pause times
- Thread pool utilization

### Error Rates
- HTTP 5xx errors
- Exception logs
- Failed document processing
- Failed embedding generation

### Performance
- Query latency (p50, p95, p99)
- Document upload time
- Embedding generation time

### Infrastructure
- Database connection pool exhaustion
- Redis connection failures
- Kafka consumer lag

---

## Timeline Summary

### Spring Boot 4.0 Upgrade (Recommended)

| Phase | Duration | Tasks |
|-------|----------|-------|
| Preparation | 3-4 hours | Branch, backup, review guides, upgrade JDK |
| Version Updates | 2-3 hours | Update POMs, rebuild |
| Code Updates | 8-12 hours | MockBean, Jackson, ConfigProperties, Security |
| Testing | 12-16 hours | Unit, integration, E2E, performance, Docker |
| Validation | 4-6 hours | Smoke tests, regression, documentation |
| **Total** | **5-7 days** | Major version upgrade |

---

## Resources

### Official Documentation

- [Spring Boot 4.0.0 Release Announcement](https://spring.io/blog/2025/11/20/spring-boot-4-0-0-available-now/)
- [Spring Boot 4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)
- [Spring Boot 4.0 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Release-Notes)
- [Spring Framework 7.0 Documentation](https://docs.spring.io/spring-framework/reference/)
- [Spring Boot End of Life Dates](https://endoflife.date/spring-boot)
- [Spring Framework End of Life Dates](https://endoflife.date/spring-framework)

### Migration Tools

- [Moderne Spring Boot 4 Migration Guide](https://www.moderne.ai/blog/spring-boot-4x-migration-guide)
- [OpenRewrite Migration Recipes](https://docs.openrewrite.org/recipes/java/spring/boot4/upgradespringboot_4_0)

---

## Final Recommendation

**Proceed with Spring Boot 4.0.0 Upgrade**

**Rationale:**
1. ✅ Spring Boot 3.2.11 has been EOL for 12 months (critical security risk)
2. ✅ Spring Boot 3.5.x only has 5 months of support remaining
3. ✅ Spring Boot 4.0.0 provides 11 months of support (until Nov 2026)
4. ✅ Avoid double upgrade (don't upgrade to 3.5 now and 4.x in May)
5. ✅ Java 21 upgrade brings performance improvements
6. ✅ Spring Framework 7.0 includes latest features and fixes

**Critical Path:**
1. **Week 1:** Preparation + JDK upgrade + Version updates
2. **Week 2:** Code updates + Unit testing
3. **Week 3:** Integration testing + E2E testing + Performance validation
4. **Week 4:** Final validation + Documentation + Deployment

**Business Impact:**
- **Current Risk:** Running EOL software for 12 months (security vulnerabilities, compliance violations)
- **Upgrade Effort:** 5-7 days of focused work
- **Benefit:** 11 months of stable, supported platform + Java 21 performance improvements

---

**Document Version:** 2.0 (Corrected for December 2025)
**Last Updated:** 2025-12-04
**Next Review:** After upgrade completion

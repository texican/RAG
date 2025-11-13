# Spring Boot 3.5 Upgrade Guide

## Overview

This guide documents the changes needed to upgrade the RAG system from Spring Boot 3.2.11 to Spring Boot 3.5.x.

**Current Version:** Spring Boot 3.2.11
**Target Version:** Spring Boot 3.5.x (latest patch: check [Spring Boot Releases](https://spring.io/projects/spring-boot))

## Version Compatibility Matrix

| Component | Current Version | Target Version | Notes |
|-----------|----------------|----------------|-------|
| Spring Boot | 3.2.11 | 3.5.x | Major framework upgrade |
| Spring Cloud | 2023.0.3 | 2025.0.x | Required for Boot 3.5 |
| Spring AI | 1.0.0-M1 | 1.0.3 or 1.1.0-RC1 | Built against Boot 3.4.2, may work with 3.5 |
| Spring Security | 6.2.8 | 6.5.0+ | Included in Boot 3.5 BOM |
| Spring Framework | 6.1.21 | 6.2.x | Included in Boot 3.5 BOM |
| Java | 21 | 21+ | No change required |
| Kafka | 3.7.0 | 3.9.0+ | Included in Boot 3.5 |
| Testcontainers | 1.19.8 | 1.21.0+ | Included in Boot 3.5 |
| PostgreSQL Driver | 42.7.3 | Latest | Check for updates |

## Critical Breaking Changes

### 1. TaskExecutor Bean Name Change ⚠️ BREAKING

**Impact:** HIGH - Code that injects `@Qualifier("taskExecutor")` will fail.

**Change:**
- Spring Boot 3.5 only provides `applicationTaskExecutor` bean name
- The `taskExecutor` alias has been removed

**Action Required:**
```bash
# Search for affected code
grep -r "taskExecutor" --include="*.java" .
```

**Fix:**
```java
// Before (Spring Boot 3.2.x)
@Autowired
@Qualifier("taskExecutor")
private TaskExecutor taskExecutor;

// After (Spring Boot 3.5.x)
@Autowired
@Qualifier("applicationTaskExecutor")
private TaskExecutor taskExecutor;

// Or use @Async without qualifier (uses applicationTaskExecutor by default)
```

---

### 2. Actuator Heapdump Endpoint Security ⚠️ SECURITY

**Impact:** MEDIUM - Heapdump endpoint now defaults to `access=NONE`

**Change:**
- Previously exposed by default
- Now requires explicit configuration

**Action Required:**
Update `application.yml` in all services if heapdump is needed:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: heapdump  # Add explicitly
  endpoint:
    heapdump:
      access: unrestricted  # Or 'restricted' with role-based access
```

**Recommendation:** Only enable in development environments.

---

### 3. Boolean Property Values Validation ⚠️ BREAKING

**Impact:** LOW - Only affects misconfigured properties

**Change:**
- Boolean properties must be exactly `true` or `false`
- Previously, any non-false value was treated as enabled

**Action Required:**
```bash
# Find potential issues in YAML files
grep -r "enabled:" config/ rag-*/src/main/resources/ | grep -v "true\|false"
```

**Fix:**
```yaml
# Before (potentially accepted in 3.2.x)
feature.enabled: yes
feature.enabled: 1

# After (required in 3.5.x)
feature.enabled: true
```

---

### 4. Redis URL vs Database Configuration ⚠️ CONFIG

**Impact:** LOW - Only affects Redis URL-based configuration

**Change:**
- `spring.data.redis.database` is now ignored when `spring.data.redis.url` is set
- Aligns behavior with host/port/username/password properties

**Action Required:**
Review `application.yml` Redis configuration in all services:

```yaml
# Current configuration (using host/port - no change needed)
spring:
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}
      database: 1  # Still works when using host/port
      password: ${REDIS_PASSWORD}

# If using URL format, database MUST be in URL
spring:
  data:
    redis:
      url: redis://:${REDIS_PASSWORD}@${REDIS_HOST}:${REDIS_PORT}/1
      # database: 1  # This will be IGNORED
```

**Current Status:** ✅ Our configuration uses `host`/`port`, no changes needed.

---

### 5. Profile Naming Validation ⚠️ VALIDATION

**Impact:** LOW - Only affects custom profile names

**Change:**
- Profiles must contain only: letters, digits, dashes, underscores
- Cannot start or end with dashes/underscores
- As of 3.5.1: dots, plus signs, @ also allowed

**Action Required:**
```bash
# Check profile names in application.yml files
grep "spring.profiles" rag-*/src/main/resources/*.yml
```

**Current Profiles:**
- `docker` ✅ Valid
- `default` ✅ Valid
- `test` ✅ Valid

---

### 6. GraphQL Configuration Properties Renamed ⚠️ CONFIG

**Impact:** NONE - RAG system doesn't use GraphQL

**Change:**
- `spring.graphql.path` → `spring.graphql.http.path`
- `spring.graphql.sse.timeout` → `spring.graphql.http.sse.timeout`

**Action Required:** None (no GraphQL in current codebase)

---

### 7. Prometheus Pushgateway Dependency Change ⚠️ DEPENDENCY

**Impact:** LOW - Only if using Prometheus push metrics

**Change:**
- Old: `io.prometheus:simpleclient_pushgateway`
- New: `io.prometheus:prometheus-metrics-exporter-pushgateway`
- Property: `base-url` → `address` (format: `host:port`)

**Action Required:**
```bash
# Check if Prometheus push is used
grep -r "simpleclient_pushgateway" pom.xml */pom.xml
grep -r "pushgateway.base-url" rag-*/src/main/resources/*.yml
```

**Current Status:** ✅ Not using Prometheus push gateway.

---

### 8. Tomcat APR Default Changed ⚠️ PERFORMANCE

**Impact:** LOW - Performance optimization opt-in

**Change:**
- `server.tomcat.use-apr` now defaults to `never` (was `when-available`)
- APR (Apache Portable Runtime) provides native performance optimizations

**Action Required:**
To enable APR (if desired for production performance):

```yaml
server:
  tomcat:
    use-apr: when-available  # Or 'always'
```

**Recommendation:** Test with `when-available` in production after upgrade.

---

### 9. TestRestTemplate Redirect Behavior ⚠️ TEST

**Impact:** LOW - Only affects tests using `TestRestTemplate`

**Change:**
- Now uses same redirect settings as `RestTemplate`
- `HttpOption.ENABLE_REDIRECTS` deprecated
- Use `withRedirects()` instead

**Action Required:**
```bash
# Find affected tests
grep -r "TestRestTemplate" rag-*/src/test/
grep -r "ENABLE_REDIRECTS" rag-*/src/test/
```

**Fix:**
```java
// Before
TestRestTemplate template = new TestRestTemplate(HttpOption.ENABLE_REDIRECTS);

// After
TestRestTemplate template = new TestRestTemplate().withRedirects(HttpClientOption.ENABLE_REDIRECTS);
```

---

### 10. Couchbase Capella SSL ⚠️ CONFIG

**Impact:** NONE - RAG system uses PostgreSQL, not Couchbase

**Change:** Capella's embedded certificate no longer auto-detected

**Action Required:** None (not using Couchbase)

---

### 11. Apache Pulsar Client Upgrade ⚠️ DEPENDENCY

**Impact:** NONE - RAG system uses Kafka, not Pulsar

**Change:** Pulsar client upgraded from 3.3.x (EOL) to 4.0.x (LTS)

**Action Required:** None (not using Pulsar)

---

## Deprecated Features (Will be removed in 3.6/3.7)

### 1. HTTP Configuration Properties Consolidation

**Deprecated:**
```yaml
spring.mvc.converters.preferred-json-mapper
spring.codec.log-request-details
spring.codec.max-in-memory-size
```

**Replacement:**
```yaml
spring.http.converters.preferred-json-mapper
spring.http.codecs.log-request-details
spring.http.codecs.max-in-memory-size
```

**Action Required:**
```bash
grep -r "spring.mvc.converters.preferred-json-mapper\|spring.codec" rag-*/src/main/resources/*.yml
```

---

### 2. OAuth2 Condition Classes

Various OAuth2 condition classes deprecated in favor of `@ConditionalOn*` annotations.

**Action Required:**
```bash
grep -r "OAuth2.*Condition" rag-*/src/main/java/
```

**Current Status:** ✅ Auth service uses standard Spring Security, no custom OAuth2 conditions.

---

### 3. SignalFX Support

SignalFX metrics export deprecated (Micrometer deprecated it).

**Action Required:**
```bash
grep -r "signalfx" pom.xml */pom.xml rag-*/src/main/resources/*.yml
```

**Current Status:** ✅ Not using SignalFX.

---

## Dependency Version Updates

### Required Updates in `pom.xml`

```xml
<properties>
    <!-- Spring versions -->
    <spring-boot.version>3.5.10</spring-boot.version>  <!-- Check latest patch -->
    <spring-cloud.version>2025.0.1</spring-cloud.version>  <!-- Required for Boot 3.5 -->
    <spring-ai.version>1.1.0-RC1</spring-ai.version>  <!-- Or 1.0.3 -->

    <!-- Spring Security/Framework managed by Boot BOM - remove explicit versions -->
    <!-- <spring-security.version>6.2.8</spring-security.version> -->
    <!-- <spring-framework.version>6.1.21</spring-framework.version> -->

    <!-- Database versions -->
    <postgresql.version>42.7.4</postgresql.version>  <!-- Check for latest -->

    <!-- Testing versions (managed by Boot BOM) -->
    <!-- <testcontainers.version>1.19.8</testcontainers.version> -->

    <!-- Kafka (managed by Boot BOM) -->
    <!-- <kafka.version>3.7.0</kafka.version> -->
</properties>
```

### Key Dependency Changes

1. **Remove explicit version management** for dependencies managed by Spring Boot BOM:
   - Spring Framework 6.2.x (now in Boot BOM)
   - Spring Security 6.5.x (now in Boot BOM)
   - Kafka 3.9.x (now in Boot BOM)
   - Testcontainers 1.21.x (now in Boot BOM)

2. **Update Spring Cloud** to 2025.0.x (required for Boot 3.5 compatibility)

3. **Spring AI Consideration:**
   - Spring AI 1.0.3 and 1.1.0-RC1 are built against Spring Boot 3.4.2
   - May work with Spring Boot 3.5 but not officially tested
   - Consider staying on Spring AI 1.0.3 (stable) or testing 1.1.0-RC1
   - Monitor [Spring AI releases](https://spring.io/projects/spring-ai#learn) for official Spring Boot 3.5 support

---

## Upgrade Steps

### Phase 1: Preparation (30 minutes)

1. **Create feature branch:**
   ```bash
   git checkout -b upgrade/spring-boot-3.5
   ```

2. **Backup current state:**
   ```bash
   git tag before-spring-boot-3.5-upgrade
   ```

3. **Review breaking changes:**
   - Read this document thoroughly
   - Check [Spring Boot 3.5 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.5-Release-Notes)

4. **Run full test suite (baseline):**
   ```bash
   mvn clean test
   mvn verify -pl rag-integration-tests
   ```

---

### Phase 2: Update Parent POM (15 minutes)

1. **Update root `pom.xml`:**
   ```bash
   # Open pom.xml and update versions as documented above
   ```

2. **Remove redundant version overrides:**
   - Comment out `spring-security.version`
   - Comment out `spring-framework.version`
   - Comment out `testcontainers.version`
   - Comment out `kafka.version`

3. **Add Spring Cloud BOM:**
   ```xml
   <dependencyManagement>
       <dependencies>
           <!-- Spring Boot BOM -->
           <dependency>
               <groupId>org.springframework.boot</groupId>
               <artifactId>spring-boot-dependencies</artifactId>
               <version>${spring-boot.version}</version>
               <type>pom</type>
               <scope>import</scope>
           </dependency>

           <!-- Spring Cloud BOM (UPDATED) -->
           <dependency>
               <groupId>org.springframework.cloud</groupId>
               <artifactId>spring-cloud-dependencies</artifactId>
               <version>${spring-cloud.version}</version>
               <type>pom</type>
               <scope>import</scope>
           </dependency>

           <!-- Continue with existing dependencies... -->
       </dependencies>
   </dependencyManagement>
   ```

4. **Verify dependency resolution:**
   ```bash
   mvn dependency:tree > dependency-tree-3.5.txt
   # Review for conflicts or unexpected versions
   ```

---

### Phase 3: Configuration Updates (30 minutes)

1. **Update Actuator configuration** (if using heapdump):
   ```bash
   # Check all application.yml files
   find . -name "application.yml" -type f
   ```

2. **Verify Redis configuration:**
   ```bash
   grep -r "spring.data.redis" rag-*/src/main/resources/
   # Ensure using host/port or URL with database in URL
   ```

3. **Check for deprecated properties:**
   ```bash
   # Search for deprecated prefixes
   grep -r "spring.mvc.converters.preferred-json-mapper" rag-*/src/main/resources/
   grep -r "spring.codec" rag-*/src/main/resources/
   ```

4. **Validate profile names:**
   ```bash
   grep -r "spring.profiles" rag-*/src/main/resources/
   # Ensure all profile names follow new naming rules
   ```

---

### Phase 4: Code Updates (1-2 hours)

1. **Find TaskExecutor references:**
   ```bash
   grep -r "@Qualifier.*taskExecutor" rag-*/src/main/java/
   ```

2. **Update bean qualifiers:**
   ```java
   // Replace all instances of @Qualifier("taskExecutor")
   // with @Qualifier("applicationTaskExecutor")
   ```

3. **Find TestRestTemplate usage:**
   ```bash
   grep -r "TestRestTemplate" rag-*/src/test/java/
   grep -r "ENABLE_REDIRECTS" rag-*/src/test/java/
   ```

4. **Update test code:**
   ```java
   // Replace HttpOption.ENABLE_REDIRECTS
   // with .withRedirects(HttpClientOption.ENABLE_REDIRECTS)
   ```

---

### Phase 5: Build and Test (2-4 hours)

1. **Clean build:**
   ```bash
   mvn clean install -DskipTests
   ```

2. **Fix compilation errors:**
   - Address any API changes
   - Update imports if packages moved

3. **Run unit tests:**
   ```bash
   mvn test
   ```

4. **Run integration tests:**
   ```bash
   mvn verify -pl rag-integration-tests
   ```

5. **Review test failures:**
   - Check for behavior changes
   - Update test expectations if needed

---

### Phase 6: Docker and Local Testing (2-3 hours)

1. **Rebuild Docker images:**
   ```bash
   make clean-all
   make build-all
   ```

2. **Start services:**
   ```bash
   make start
   ```

3. **Monitor startup logs:**
   ```bash
   make logs SERVICE=rag-auth
   make logs SERVICE=rag-document
   make logs SERVICE=rag-embedding
   make logs SERVICE=rag-core
   make logs SERVICE=rag-admin
   ```

4. **Check for warnings/errors:**
   - Deprecation warnings
   - Configuration issues
   - Startup failures

5. **Manual testing:**
   ```bash
   # Test authentication
   ./scripts/utils/admin-login.sh

   # Test document upload
   curl -X POST http://localhost:8082/api/v1/documents/upload \
     -H 'X-Tenant-ID: YOUR_TENANT_ID' \
     -F 'file=@test.txt'

   # Test health endpoints
   ./scripts/utils/service-status.sh
   ```

6. **Run E2E tests:**
   ```bash
   cd rag-integration-tests
   mvn verify -Dmaven.test.skip=false -DskipTests=false
   ```

---

### Phase 7: Performance and Monitoring (1-2 hours)

1. **Enable Tomcat APR (optional):**
   ```yaml
   # application.yml
   server:
     tomcat:
       use-apr: when-available
   ```

2. **Monitor resource usage:**
   ```bash
   docker stats
   ```

3. **Check response times:**
   - Compare with baseline metrics
   - Look for performance regressions

4. **Review actuator endpoints:**
   ```bash
   curl http://localhost:8081/actuator/health
   curl http://localhost:8081/actuator/metrics
   ```

---

### Phase 8: Documentation and Cleanup (30 minutes)

1. **Update CLAUDE.md:**
   - Add upgrade session summary
   - Document any issues encountered
   - Update current version info

2. **Update README.md:**
   - Update Spring Boot version badge
   - Update prerequisites

3. **Update BACKLOG.md:**
   - Close upgrade task
   - Create follow-up tasks if needed

4. **Commit changes:**
   ```bash
   git add .
   git commit -m "chore: upgrade to Spring Boot 3.5.x

   - Update Spring Boot from 3.2.11 to 3.5.x
   - Update Spring Cloud to 2025.0.x
   - Update Spring AI to 1.1.0-RC1
   - Fix TaskExecutor bean qualifier references
   - Update configuration for breaking changes
   - All tests passing

   See docs/development/SPRING_BOOT_3.5_UPGRADE_GUIDE.md for details"
   ```

---

## Rollback Plan

If critical issues are found after upgrade:

1. **Immediate rollback:**
   ```bash
   git checkout main
   git reset --hard before-spring-boot-3.5-upgrade
   make clean-all
   make build-all
   make start
   ```

2. **Investigate issues:**
   - Review logs for root cause
   - Check compatibility matrix
   - Consider staying on Spring Boot 3.4.x instead

3. **Alternative: Spring Boot 3.4.x**
   - Spring Boot 3.4.x is LTS with 13 months free support
   - Spring AI 1.0.3 / 1.1.0-RC1 are built against Spring Boot 3.4.2
   - May be safer intermediate upgrade path

---

## Known Risks and Mitigations

### Risk 1: Spring AI Compatibility

**Risk:** Spring AI 1.0.3 / 1.1.0-RC1 built against Spring Boot 3.4.2, not 3.5.x

**Mitigation:**
1. Test embedding generation thoroughly after upgrade
2. Monitor Spring AI release notes for official 3.5 support
3. Consider waiting for Spring AI 1.1.0 GA with 3.5 support
4. Alternative: Upgrade to Spring Boot 3.4.x first, then 3.5 later

**Testing Checklist:**
- ✅ Ollama embedding generation works
- ✅ OpenAI embedding generation works (with API key)
- ✅ Embedding service health check passes
- ✅ Vector search functionality intact
- ✅ No Spring AI deprecation warnings

---

### Risk 2: TestContainers Compatibility

**Risk:** TestContainers upgrade from 1.19.8 to 1.21.x may have breaking changes

**Mitigation:**
1. Review [TestContainers 1.21 release notes](https://github.com/testcontainers/testcontainers-java/releases)
2. Test integration test suite thoroughly
3. Check for Docker compatibility (Colima, Docker Desktop)

**Known Issues:**
- STORY-004: Colima/TestContainers compatibility issue exists
- Issue unrelated to Spring Boot version
- Monitor TestContainers Colima support

---

### Risk 3: Performance Regression

**Risk:** Tomcat APR now defaults to `never` (was `when-available`)

**Mitigation:**
1. Benchmark response times before/after upgrade
2. Enable APR explicitly for production: `server.tomcat.use-apr: when-available`
3. Monitor metrics after deployment

---

### Risk 4: Spring Cloud Gateway (if re-enabled)

**Risk:** Major Spring Cloud Gateway refactoring in 2025.0.x

**Mitigation:**
- Gateway currently archived per ADR-001
- If re-enabling, follow [Spring Cloud Gateway 2025.0 migration guide](https://github.com/spring-cloud/spring-cloud-gateway/wiki/Spring-Cloud-Gateway-2025.0-Migration-Guide)
- Module names changed (e.g., `spring-cloud-gateway-server-webflux`)
- Property prefixes changed

---

## Post-Upgrade Validation Checklist

### Functional Testing

- [ ] All services start successfully
- [ ] Health checks pass for all services
- [ ] Authentication flow works (register, login, JWT validation)
- [ ] Document upload works
- [ ] Document processing pipeline works (chunking, embedding)
- [ ] Query execution returns results
- [ ] Admin API accessible
- [ ] Swagger UI accessible for all services

### Integration Testing

- [ ] Unit tests pass: `mvn test`
- [ ] Integration tests pass: `mvn verify -pl rag-integration-tests`
- [ ] E2E tests pass (or known blockers documented)
- [ ] No new test failures introduced by upgrade

### Configuration Testing

- [ ] Profile activation works (docker, test, default)
- [ ] Environment variables properly resolved
- [ ] Redis connectivity works
- [ ] PostgreSQL connectivity works
- [ ] Kafka connectivity works
- [ ] Ollama connectivity works

### Performance Testing

- [ ] Response times comparable to baseline
- [ ] Memory usage acceptable
- [ ] CPU usage acceptable
- [ ] No resource leaks detected

### Documentation

- [ ] CLAUDE.md updated with upgrade session
- [ ] README.md version badges updated
- [ ] BACKLOG.md updated (upgrade task closed)
- [ ] This guide updated with any new findings

---

## Support and Resources

### Official Documentation

- [Spring Boot 3.5 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.5-Release-Notes)
- [Spring Boot 3.5 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.5-Migration-Guide)
- [Spring Cloud 2025.0 Release Notes](https://github.com/spring-cloud/spring-cloud-release/wiki/Spring-Cloud-2025.0-Release-Notes)
- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Spring Boot Dependency Versions](https://docs.spring.io/spring-boot/appendix/dependency-versions/index.html)

### Community Support

- [Spring Boot GitHub Issues](https://github.com/spring-projects/spring-boot/issues)
- [Spring AI GitHub Issues](https://github.com/spring-projects/spring-ai/issues)
- [Stack Overflow - spring-boot](https://stackoverflow.com/questions/tagged/spring-boot)

### Internal Resources

- [CLAUDE.md](../../CLAUDE.md) - Project context and session history
- [BACKLOG.md](../../BACKLOG.md) - Project backlog and sprint planning
- [README.md](../../README.md) - Project overview and setup

---

## Conclusion

This upgrade is **moderate risk** with several breaking changes that require code and configuration updates. The main concerns are:

1. **Spring AI compatibility** - Built against Spring Boot 3.4.2, not officially tested with 3.5
2. **TaskExecutor bean name change** - Requires code changes
3. **Actuator security changes** - Requires configuration updates
4. **Spring Cloud 2025.0.x** - Major version jump

**Recommended Approach:**

**Option A: Gradual Upgrade (Lower Risk)**
1. Upgrade to Spring Boot 3.4.x first (matches Spring AI target)
2. Test thoroughly for 1-2 sprints
3. Then upgrade to Spring Boot 3.5.x

**Option B: Direct Upgrade (Higher Risk)**
1. Upgrade directly to Spring Boot 3.5.x
2. Thorough testing of Spring AI functionality
3. Be prepared to rollback if critical issues found

**Estimated Effort:** 8-16 hours (1-2 days) for complete upgrade and testing.

---

**Document Version:** 1.0
**Last Updated:** 2025-11-13
**Author:** Claude Code
**Reviewed By:** (Pending)

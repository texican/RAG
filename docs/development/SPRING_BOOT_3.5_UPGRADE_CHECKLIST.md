# Spring Boot 3.5 Upgrade Checklist

Quick reference checklist for upgrading from Spring Boot 3.2.11 to 3.5.x.

See [SPRING_BOOT_3.5_UPGRADE_GUIDE.md](SPRING_BOOT_3.5_UPGRADE_GUIDE.md) for detailed instructions.

---

## Pre-Upgrade Tasks

### Preparation
- [ ] Create feature branch: `upgrade/spring-boot-3.5`
- [ ] Create backup tag: `git tag before-spring-boot-3.5-upgrade`
- [ ] Run baseline tests: `mvn clean test && mvn verify -pl rag-integration-tests`
- [ ] Document baseline metrics (response times, startup time, memory usage)
- [ ] Review [Spring Boot 3.5 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.5-Release-Notes)

---

## Code Analysis Tasks

### Search for Breaking Changes
- [ ] Search for `@Qualifier("taskExecutor")`: `grep -r '@Qualifier.*taskExecutor' rag-*/src/main/java/`
- [ ] Search for `TestRestTemplate` usage: `grep -r 'TestRestTemplate' rag-*/src/test/java/`
- [ ] Search for `ENABLE_REDIRECTS`: `grep -r 'ENABLE_REDIRECTS' rag-*/src/test/java/`
- [ ] Search for boolean properties: `grep -r 'enabled:' rag-*/src/main/resources/ | grep -v 'true\|false'`
- [ ] Search for deprecated properties: `grep -r 'spring.mvc.converters.preferred-json-mapper\|spring.codec' rag-*/src/main/resources/`
- [ ] Verify profile names: `grep -r 'spring.profiles' rag-*/src/main/resources/`

### Review Results
- [ ] Document all findings
- [ ] Estimate effort for code changes
- [ ] Identify potential risks

---

## POM Updates

### Root pom.xml Changes
- [ ] Update `<spring-boot.version>` to `3.5.10` (or latest patch)
- [ ] Update `<spring-cloud.version>` to `2025.0.1` (or latest)
- [ ] Update `<spring-ai.version>` to `1.1.0-RC1` or `1.0.3`
- [ ] Remove `<spring-security.version>` (managed by Boot BOM)
- [ ] Remove `<spring-framework.version>` (managed by Boot BOM)
- [ ] Remove `<testcontainers.version>` (managed by Boot BOM)
- [ ] Remove `<kafka.version>` (managed by Boot BOM)
- [ ] Update `<postgresql.version>` to `42.7.4` or latest
- [ ] Verify Spring Cloud BOM is imported in `<dependencyManagement>`

### Dependency Resolution
- [ ] Run `mvn dependency:tree > dependency-tree-3.5.txt`
- [ ] Review for conflicts or unexpected versions
- [ ] Compare with previous dependency tree

---

## Configuration Updates

### application.yml Files (All Services)

#### Actuator Heapdump (if needed)
- [ ] Add `management.endpoints.web.exposure.include: heapdump`
- [ ] Add `management.endpoint.heapdump.access: unrestricted` (dev only)

#### Redis Configuration
- [ ] Verify using `host`/`port` (preferred) or `url` with database in URL
- [ ] Current config: ✅ Uses `host`/`port` - no changes needed

#### Profile Names
- [ ] Verify all profile names: `docker`, `default`, `test`
- [ ] Current profiles: ✅ All valid

#### Deprecated Properties (Replace if found)
- [ ] Replace `spring.mvc.converters.preferred-json-mapper` → `spring.http.converters.preferred-json-mapper`
- [ ] Replace `spring.codec.log-request-details` → `spring.http.codecs.log-request-details`
- [ ] Replace `spring.codec.max-in-memory-size` → `spring.http.codecs.max-in-memory-size`

#### Tomcat APR (Optional Performance Boost)
- [ ] Add `server.tomcat.use-apr: when-available` (production only)

---

## Code Changes

### TaskExecutor Bean Qualifier
Replace all instances:
```java
// Before
@Qualifier("taskExecutor")

// After
@Qualifier("applicationTaskExecutor")
```

**Affected Files:**
- [ ] File: __________________ (line: ____)
- [ ] File: __________________ (line: ____)
- [ ] File: __________________ (line: ____)

### TestRestTemplate Updates
Replace all instances:
```java
// Before
new TestRestTemplate(HttpOption.ENABLE_REDIRECTS)

// After
new TestRestTemplate().withRedirects(HttpClientOption.ENABLE_REDIRECTS)
```

**Affected Files:**
- [ ] File: __________________ (line: ____)
- [ ] File: __________________ (line: ____)

---

## Build and Test

### Initial Build
- [ ] Run `mvn clean install -DskipTests`
- [ ] Fix any compilation errors
- [ ] Document any API changes encountered

### Unit Tests
- [ ] Run `mvn test`
- [ ] Review test failures
- [ ] Fix legitimate failures (not test issues)
- [ ] Update test expectations if behavior changed intentionally
- [ ] Re-run until all tests pass

### Integration Tests
- [ ] Run `mvn verify -pl rag-integration-tests`
- [ ] Review integration test failures
- [ ] Fix legitimate failures
- [ ] Document any test changes needed
- [ ] Re-run until all tests pass

---

## Docker and Local Testing

### Docker Rebuild
- [ ] Run `make clean-all`
- [ ] Run `make build-all`
- [ ] Run `make start`

### Service Startup Verification
- [ ] Check `rag-auth` logs: `make logs SERVICE=rag-auth`
- [ ] Check `rag-document` logs: `make logs SERVICE=rag-document`
- [ ] Check `rag-embedding` logs: `make logs SERVICE=rag-embedding`
- [ ] Check `rag-core` logs: `make logs SERVICE=rag-core`
- [ ] Check `rag-admin` logs: `make logs SERVICE=rag-admin`
- [ ] Document any warnings or errors

### Service Health Checks
- [ ] Run `./scripts/utils/service-status.sh`
- [ ] Verify all services are "UP"
- [ ] Check actuator health: `curl http://localhost:8081/actuator/health`

### Manual Functional Testing

#### Authentication
- [ ] Test admin login: `./scripts/utils/admin-login.sh`
- [ ] Test user registration (via Swagger UI)
- [ ] Test JWT token validation

#### Document Processing
```bash
# Get tenant ID from admin-login.sh output
TENANT_ID="YOUR_TENANT_ID"

# Test document upload
curl -X POST http://localhost:8082/api/v1/documents/upload \
  -H "X-Tenant-ID: $TENANT_ID" \
  -F 'file=@test.txt'

# Note document ID from response
```
- [ ] Document uploads successfully
- [ ] Document processing completes (check logs)
- [ ] Chunks created (verify in database)
- [ ] Embeddings generated (verify in database)

#### Query Execution
```bash
# Test query
curl -X POST http://localhost:8084/api/v1/query \
  -H "X-Tenant-ID: $TENANT_ID" \
  -H "Content-Type: application/json" \
  -d '{"query": "test query", "topK": 5}'
```
- [ ] Query executes successfully
- [ ] Results returned
- [ ] Response time acceptable

#### Swagger UI Access
- [ ] Auth: http://localhost:8081/swagger-ui.html
- [ ] Document: http://localhost:8082/swagger-ui.html
- [ ] Embedding: http://localhost:8083/swagger-ui.html
- [ ] Core: http://localhost:8084/swagger-ui.html
- [ ] Admin: http://localhost:8085/admin/api/swagger-ui.html

### E2E Tests
- [ ] Run E2E test suite: `cd rag-integration-tests && mvn verify -Dmaven.test.skip=false`
- [ ] Document any failures (distinguish from pre-existing issues)
- [ ] Verify no new failures introduced by upgrade

---

## Spring AI Specific Testing

### Ollama Embeddings
- [ ] Service starts with Ollama configuration
- [ ] Embedding generation request succeeds
- [ ] Returns 1024-dimensional vectors
- [ ] Response time acceptable (~60-100ms)
- [ ] No Spring AI deprecation warnings in logs

### OpenAI Embeddings (if configured)
- [ ] Service starts with OpenAI configuration
- [ ] Embedding generation request succeeds (with API key)
- [ ] Returns 1536-dimensional vectors
- [ ] No Spring AI compatibility errors

### Spring AI Compatibility Check
```bash
# Check for Spring AI warnings
grep -i "spring.*ai" logs/*.log | grep -i "warn\|error\|deprecated"
```
- [ ] No Spring AI compatibility warnings
- [ ] No unexpected deprecations
- [ ] All AI features working as expected

---

## Performance Validation

### Baseline Comparison
- [ ] Document startup time (compare with baseline)
- [ ] Document response times (compare with baseline)
- [ ] Document memory usage (compare with baseline)
- [ ] Document CPU usage (compare with baseline)

### Resource Monitoring
```bash
# Monitor Docker resources
docker stats
```
- [ ] Memory usage acceptable (<80% of limits)
- [ ] CPU usage acceptable (<70% under load)
- [ ] No resource leaks detected

### Performance Testing
- [ ] Run load test (if available)
- [ ] Compare results with baseline
- [ ] Document any regressions

---

## Documentation Updates

### Project Documentation
- [ ] Update [CLAUDE.md](../../CLAUDE.md) with upgrade session summary
- [ ] Update [README.md](../../README.md) Spring Boot version badge
- [ ] Update [BACKLOG.md](../../BACKLOG.md) - close upgrade task
- [ ] Update version in all service [README.md](../../rag-*/README.md) files

### Findings Documentation
- [ ] Document any issues encountered
- [ ] Document workarounds applied
- [ ] Document performance changes observed
- [ ] Update upgrade guide with new findings

---

## Commit and PR

### Git Commit
```bash
git add .
git commit -m "chore: upgrade to Spring Boot 3.5.x

- Update Spring Boot from 3.2.11 to 3.5.x
- Update Spring Cloud to 2025.0.x
- Update Spring AI to [version]
- Fix TaskExecutor bean qualifier references
- Update configuration for breaking changes
- [List other changes]

All tests passing: [X/Y unit tests, A/B integration tests]

See docs/development/SPRING_BOOT_3.5_UPGRADE_GUIDE.md for details"
```

### PR Checklist
- [ ] All checklist items above completed
- [ ] All tests passing
- [ ] No new warnings in logs
- [ ] Performance acceptable
- [ ] Documentation updated
- [ ] PR description references upgrade guide
- [ ] PR includes before/after comparison (metrics, logs)

---

## Post-Merge Tasks

### Monitoring (First 24 Hours)
- [ ] Monitor service health in dev environment
- [ ] Monitor error rates
- [ ] Monitor response times
- [ ] Check for memory leaks
- [ ] Review logs for warnings/errors

### Gradual Rollout (if applicable)
- [ ] Deploy to staging environment
- [ ] Run full test suite in staging
- [ ] Monitor staging for 24-48 hours
- [ ] Deploy to production (gradual rollout recommended)
- [ ] Monitor production metrics closely

---

## Rollback Plan (If Needed)

### Emergency Rollback
```bash
git checkout main
git reset --hard before-spring-boot-3.5-upgrade
make clean-all
make build-all
make start
```

### Post-Rollback
- [ ] Document rollback reason
- [ ] Document issues encountered
- [ ] Create issues for blockers
- [ ] Plan remediation strategy
- [ ] Consider alternative: upgrade to Spring Boot 3.4.x first

---

## Sign-Off

### Developer
- [ ] All checklist items completed
- [ ] Tests passing locally
- [ ] Documentation updated
- [ ] Ready for review

**Developer:** __________________ **Date:** __________

### Reviewer
- [ ] Code changes reviewed
- [ ] Configuration changes reviewed
- [ ] Tests verified
- [ ] Documentation reviewed
- [ ] Approved for merge

**Reviewer:** __________________ **Date:** __________

---

## Notes and Issues

**Issues Encountered:**
```
(Document any issues, workarounds, or unexpected behavior here)
```

**Performance Changes:**
```
(Document any performance improvements or regressions here)
```

**Open Questions:**
```
(Document any unresolved questions or follow-up items here)
```

---

**Checklist Version:** 1.0
**Last Updated:** 2025-11-13
**Related Documents:**
- [SPRING_BOOT_3.5_UPGRADE_GUIDE.md](SPRING_BOOT_3.5_UPGRADE_GUIDE.md) - Detailed upgrade guide
- [CLAUDE.md](../../CLAUDE.md) - Project context
- [BACKLOG.md](../../BACKLOG.md) - Project backlog

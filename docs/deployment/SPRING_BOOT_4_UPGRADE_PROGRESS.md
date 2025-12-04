# Spring Boot 4.0 Upgrade Progress

**Date:** 2025-12-04
**Branch:** `upgrade/spring-boot-4.0`
**Status:** 🟡 IN PROGRESS - Auth Service (40% Complete)

---

## Executive Summary

This document tracks the Spring Boot 3.2.11 → 4.0.0 upgrade implementation for the RAG system. We're taking an **incremental service-by-service approach**, starting with the Auth Service to establish patterns, then documenting the process for remaining services.

### Why Spring Boot 4.0?

- 🔴 **CRITICAL**: Spring Boot 3.2.x reached EOL December 2024 (12 months ago)
- 🔴 **CRITICAL**: Spring Framework 6.1.x reached EOL November 2025 (1 month ago)
- ✅ **Spring Boot 4.0.0**: Released November 20, 2025 - Supported until November 2026
- ✅ **Spring Framework 7.0.0**: Supported until November 2027

**Why Skip 3.5?** Only 5 months of support remaining (EOL May 2026). Upgrading directly to 4.0 gives 11 months of stability and avoids a second upgrade cycle.

---

## Overall Progress

### ✅ Phase 1: Maven Dependencies (100% Complete)

**What Changed:**
```xml
<!-- Before -->
<spring-boot.version>3.2.11</spring-boot.version>
<spring-framework.version>6.1.21</spring-framework.version>
<spring-security.version>6.2.8</spring-security.version>
<spring-cloud.version>2023.0.3</spring-cloud.version>

<!-- After -->
<spring-boot.version>4.0.0</spring-boot.version>
<spring-framework.version>7.0.0</spring-framework.version>
<spring-security.version>7.0.0</spring-security.version>
<spring-cloud.version>2025.1.0</spring-cloud.version>  <!-- Oakwood -->
```

**Other Version Updates:**
- Tomcat: 10.1.35 → 11.0.2 (Servlet 6.1 compatible)
- JUnit: 5.10.2 → 5.11.0
- Spring AI: 1.0.0-M1 → 1.0.0-M5

**Repository Changes:**
- Added `spring-releases` repository for GA releases

**Result:** ✅ All dependencies resolve successfully

---

### ✅ Phase 2: Package Migrations (90% Complete)

Spring Boot 4.0 reorganized packages as part of modularization. Here are the changes discovered:

#### 1. `@EntityScan` Annotation

**Before:**
```java
import org.springframework.boot.autoconfigure.domain.EntityScan;
```

**After:**
```java
import org.springframework.boot.persistence.autoconfigure.EntityScan;
```

**Affected Files:**
- ✅ `rag-auth-service/src/main/java/com/byo/rag/auth/AuthServiceApplication.java`
- ✅ `rag-document-service/src/main/java/com/byo/rag/document/DocumentServiceApplication.java`
- ✅ `rag-admin-service/src/main/java/com/byo/rag/admin/AdminServiceApplication.java`
- ✅ `rag-integration-tests/src/test/java/com/byo/rag/integration/config/IntegrationTestConfig.java`

**Documentation:** [EntityScan API](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/persistence/autoconfigure/EntityScan.html)

---

#### 2. `spring-boot-starter-aop` → `spring-boot-starter-aspectj`

**Before:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

**After:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aspectj</artifactId>
</dependency>
```

**Affected Files:**
- ✅ `rag-embedding-service/pom.xml`

**Reason:** Renamed to clarify it provides AspectJ support specifically, not general AOP (which is built into Spring Framework).

---

#### 3. Kafka Autoconfiguration Package

**Before:**
```java
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
```

**After:**
```java
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
```

**Affected Files:**
- ✅ `rag-document-service/src/main/java/com/byo/rag/document/DocumentServiceApplication.java`
- ⏸️ `rag-embedding-service/src/main/java/com/byo/rag/embedding/EmbeddingServiceApplication.java` (pending)

**Documentation:** [KafkaAutoConfiguration API](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/kafka/autoconfigure/KafkaAutoConfiguration.html)

---

#### 4. Test Infrastructure Package Changes

**`AutoConfigureMockMvc`:**
```java
// Before
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

// After
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
```

**`TestRestTemplate` and `@AutoConfigureTestRestTemplate`:**
```java
// Before
import org.springframework.boot.test.web.client.TestRestTemplate;

// After
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
```

**Required POM Addition:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc-test</artifactId>
    <scope>test</scope>
</dependency>
```

**Note:** Spring Boot 4.0 introduces `RestTestClient` as a modern replacement for `TestRestTemplate`. Consider migrating to it for new tests.

**Affected Files:**
- ✅ `rag-auth-service/src/test/java/com/byo/rag/auth/SecurityConfigurationTest.java`
- ✅ `rag-auth-service/src/test/java/com/byo/rag/auth/ServiceStartupIntegrationTest.java`
- ✅ `rag-auth-service/pom.xml`

**Documentation:**
- [AutoConfigureMockMvc API](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/webmvc/test/autoconfigure/AutoConfigureMockMvc.html)
- [What's New for Testing in Spring Boot 4](https://rieckpil.de/whats-new-for-testing-in-spring-boot-4-0-and-spring-framework-7/)

---

#### 5. Remaining Package Moves (Pending)

These were identified but not yet fixed:

**Embedding Service:**
- `HibernateJpaAutoConfiguration`: `org.springframework.boot.autoconfigure.orm.jpa` → TBD
- `DataSourceAutoConfiguration`: `org.springframework.boot.autoconfigure.jdbc` → TBD

**Action Required:** Search Spring Boot 4.0 API docs for new package locations.

---

### 🟡 Phase 3: Java 25 + Mockito Compatibility (100% Complete)

**Issue:** Mockito's default byte-buddy mock maker fails on Java 25 due to stricter module encapsulation.

**Error:**
```
Mockito cannot mock this class: class com.byo.rag.auth.service.UserService.
Java : 25
Underlying exception: Could not modify all classes
```

**Solution:** Enable Mockito's inline mock maker

**File Created:**
`rag-auth-service/src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`
```
mock-maker-inline
```

**POM Updates:**
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.2.5</version>
    <configuration>
        <argLine>
            --add-opens java.base/java.lang=ALL-UNNAMED
            --add-opens java.base/java.util=ALL-UNNAMED
            --add-opens java.base/java.lang.reflect=ALL-UNNAMED
            --add-opens java.base/java.text=ALL-UNNAMED
            --add-opens java.desktop/java.awt.font=ALL-UNNAMED
        </argLine>
    </configuration>
</plugin>
```

**Result:** ✅ Mockito errors resolved

**Replication:** Copy this file to all service test resources directories.

---

### 🔴 Phase 4: Jackson 3.x Configuration (IN PROGRESS - BLOCKER)

**Current Error:**
```
Error creating bean with name 'spring.jackson-org.springframework.boot.jackson.autoconfigure.JacksonProperties':
Could not bind properties to 'JacksonProperties': prefix=spring.jackson
```

**Root Cause:** Jackson configuration properties structure changed in Spring Boot 4.0 (Jackson 3.x).

**Known Changes (from Spring Boot 4.0 docs):**
1. Configuration property paths moved (e.g., `spring.jackson.*` → new structure)
2. Some properties removed/renamed
3. `Jackson2ObjectMapperBuilder` deprecated in favor of new builder API

**Current Configuration to Review:**
```yaml
# rag-shared/src/main/resources/application.yml
spring:
  jackson:
    serialization:
      write-dates-as-timestamps: false
      indent-output: true
    deserialization:
      fail-on-unknown-properties: false
```

**Files to Check:**
- `rag-shared/src/main/java/com/byo/rag/shared/config/JsonConfig.java` (uses deprecated `Jackson2ObjectMapperBuilder`)
- All `application.yml` files with `spring.jackson` configuration

**Action Required:**
1. Review [Spring Boot 4.0 Jackson Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide#jackson)
2. Update `JsonConfig.java` to use new builder API
3. Update all `application.yml` Jackson properties
4. Test JSON serialization/deserialization

---

### ⏸️ Phase 5: Spring Security 7.0 (Pending)

**Expected Changes:**
- Security configuration DSL updates
- `@EnableWebSecurity` behavior changes
- JWT filter chain modifications
- CORS configuration updates

**Files to Review:**
- `rag-auth-service/src/main/java/com/byo/rag/auth/config/SecurityConfig.java`
- `rag-*/src/main/java/com/byo/rag/*/security/*Filter.java`

**Documentation:** [Spring Security 7.0 Migration Guide](https://docs.spring.io/spring-security/reference/7.0/migration/index.html)

---

### ⏸️ Phase 6: Spring AI API Changes (Pending - Embedding Service)

**Issue:** Spring AI `EmbeddingModel` interface changed return types.

**Before (Spring AI 1.0.0-M1):**
```java
public interface EmbeddingModel {
    List<Double> embed(Document document);
    List<List<Double>> embed(List<String> texts);
}
```

**After (Spring AI 1.0.0-M5):**
```java
public interface EmbeddingModel {
    float[] embed(Document document);
    List<float[]> embed(List<String> texts);
}
```

**Affected Files:**
- `rag-embedding-service/src/main/java/com/byo/rag/embedding/model/OllamaEmbeddingModel.java`
- `rag-embedding-service/src/main/java/com/byo/rag/embedding/service/EmbeddingService.java`

**Action Required:**
1. Update return types from `List<Double>` to `float[]`
2. Convert internal logic to use primitive arrays
3. Update tests

---

## Auth Service Status (40% Complete)

### ✅ Completed
1. ✅ Compilation successful (all package moves fixed)
2. ✅ Test compilation successful
3. ✅ Mockito configuration for Java 25
4. ✅ Test infrastructure dependencies added

### 🔴 Blocked
1. 🔴 Jackson 3.x configuration binding errors
2. ⏸️ 13 tests failing due to Jackson issue
3. ⏸️ Spring context not starting in tests

### Next Steps for Auth Service
1. Fix Jackson configuration properties
2. Update `JsonConfig.java` to use new builder
3. Run full test suite
4. Fix any Security 7.0 issues
5. Verify service starts and operates correctly

---

## Remaining Services

Once Auth Service is complete, apply the same patterns to:

1. **Document Service** (Medium complexity)
   - Similar to Auth (JPA, Security)
   - Additional: Kafka integration

2. **Admin Service** (Low complexity)
   - Simplest service
   - Similar to Auth

3. **Core Service** (Medium complexity)
   - Redis integration
   - Query processing

4. **Embedding Service** (HIGH complexity) 🔴
   - Spring AI API changes
   - Multiple autoconfiguration exclusions
   - Ollama integration updates

---

## Migration Checklist

Use this checklist for each service:

### Compilation Fixes
- [ ] Update `@EntityScan` import (if applicable)
- [ ] Update `KafkaAutoConfiguration` import (if applicable)
- [ ] Update test imports (`AutoConfigureMockMvc`, `TestRestTemplate`)
- [ ] Add `spring-boot-starter-webmvc-test` dependency
- [ ] Update `spring-boot-starter-aop` → `spring-boot-starter-aspectj`

### Mockito Configuration
- [ ] Create `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`
- [ ] Add content: `mock-maker-inline`

### Jackson Configuration
- [ ] Review all `application.yml` Jackson properties
- [ ] Update `@Configuration` classes using `Jackson2ObjectMapperBuilder`
- [ ] Test JSON serialization

### Spring Security 7.0
- [ ] Review security configurations
- [ ] Update filter chains
- [ ] Test authentication flows

### Testing
- [ ] Run unit tests: `mvn test -pl <service>`
- [ ] Fix test failures
- [ ] Verify service starts: `mvn spring-boot:run -pl <service>`
- [ ] Test API endpoints manually

---

## Key Resources

### Official Documentation
- [Spring Boot 4.0.0 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Release-Notes)
- [Spring Boot 4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)
- [Spring Framework 7.0 What's New](https://docs.spring.io/spring-framework/reference/7.0/whatsnew.html)
- [Spring Security 7.0 Migration](https://docs.spring.io/spring-security/reference/7.0/migration/index.html)
- [Spring Cloud 2025.1.0 (Oakwood) Release](https://spring.io/blog/2025/11/25/spring-cloud-2025-1-0-aka-oakwood-has-been-released/)

### API Documentation
- [Spring Boot 4.0 API](https://docs.spring.io/spring-boot/api/java/)
- [EntityScan](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/persistence/autoconfigure/EntityScan.html)
- [KafkaAutoConfiguration](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/kafka/autoconfigure/KafkaAutoConfiguration.html)
- [AutoConfigureMockMvc](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/webmvc/test/autoconfigure/AutoConfigureMockMvc.html)

### Articles
- [What's New for Testing in Spring Boot 4](https://rieckpil.de/whats-new-for-testing-in-spring-boot-4-0-and-spring-framework-7/)
- [The State of HTTP Clients in Spring](https://spring.io/blog/2025/09/30/the-state-of-http-clients-in-spring/)

---

## Timeline Estimate

**Original Estimate:** 5-7 days (major version jump)

**Current Progress:**
- ✅ Day 1 (4 hours): Maven dependencies + package migrations
- 🟡 Day 2 (in progress): Auth Service - Jackson configuration
- ⏸️ Remaining: 4-5 days

**Breakdown:**
- Auth Service completion: 0.5-1 day
- Document Service: 1-1.5 days
- Admin Service: 0.5 day
- Core Service: 1 day
- Embedding Service: 1.5-2 days (most complex)
- Integration testing: 0.5-1 day

**Total Remaining:** 4-6 days

---

## Rollback Plan

If issues are encountered:

1. **Branch Rollback:**
   ```bash
   git checkout main
   git branch -D upgrade/spring-boot-4.0
   ```

2. **Tag Restoration:**
   ```bash
   git checkout pre-spring-4-upgrade-20251204
   git checkout -b spring-boot-3.2-stable
   ```

3. **Dependencies Restored:** Previous versions in backup file `dependency-tree-before-upgrade.txt`

---

## Notes

- **Java Version:** Project uses Java 21 (meets Spring Boot 4.0 minimum requirement)
- **Test Environment:** Java 25 runtime (requires Mockito inline mock maker)
- **Branch:** All work on `upgrade/spring-boot-4.0` feature branch
- **Backup Tag:** `pre-spring-4-upgrade-20251204` created before any changes

---

**Last Updated:** 2025-12-04 12:50 PST
**Next Update:** After Jackson configuration resolution

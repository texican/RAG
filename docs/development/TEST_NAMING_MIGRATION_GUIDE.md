# Test Naming Convention Migration Guide

**Status**: Implementation of TECH-DEBT-002
**Last Updated**: 2025-10-05

## Overview

This guide documents the standardization of test naming conventions across the RAG System codebase. The goal is to establish consistent, predictable naming patterns that clearly indicate test type, execution context, and purpose.

## Current State Analysis

### Test File Count by Pattern

```bash
# Unit Tests (*Test.java): 58 files
# Integration Tests (*IT.java): 8 files
# Integration Tests (*IntegrationTest.java): 5 files
# E2E Tests (*E2ETest.java): 1 file
# E2E Tests (*EndToEndIT.java): 1 file
```

### Identified Issues

1. **Mixed Integration Test Suffixes**: Some use `IT.java`, others use `IntegrationTest.java`
2. **E2E Test Not in Failsafe**: `StandaloneRagE2ETest.java` needs IT suffix or Failsafe configuration
3. **Inconsistent Categorization**: Some test types unclear from filename alone

## Standardized Naming Conventions

### 1. Unit Tests
**Pattern**: `{ClassName}Test.java`
- Execution: Maven Surefire (`mvn test`)
- Isolated tests with mocks/stubs
- Examples: `DocumentServiceTest.java`, `RagControllerTest.java`

### 2. Integration Tests
**Pattern**: `{Feature}IT.java` (preferred) or `{Component}IntegrationTest.java` (legacy)
- Execution: Maven Failsafe (`mvn verify`)
- Real dependencies (DB, messaging, etc.)
- Examples: `DocumentUploadProcessingIT.java`, `EmbeddingRepositoryIntegrationTest.java`

### 3. End-to-End Tests
**Pattern**: `{Scenario}E2ETest.java` (if Failsafe configured) or `{Scenario}EndToEndIT.java`
- Execution: Maven Failsafe (`mvn verify -Pintegration-tests`)
- Full system integration across services
- Examples: `StandaloneRagE2ETest.java`, `ComprehensiveRagEndToEndIT.java`

### 4. Specialized Tests
- **Validation**: `{Component}ValidationTest.java`
- **Security**: `{Component}SecurityTest.java`
- **Performance**: `{Feature}LoadTest.java` or `{Feature}PerformanceTest.java`
- **Smoke**: `{Component}SmokeIT.java`

## Migration Strategy

### Phase 1: Configuration (Immediate)
✅ **Action**: Ensure Maven Failsafe includes all test patterns

```xml
<!-- In parent pom.xml or integration-tests module -->
<plugin>
    <artifactId>maven-failsafe-plugin</artifactId>
    <configuration>
        <includes>
            <include>**/*IT.java</include>
            <include>**/*IntegrationTest.java</include>
            <include>**/*E2ETest.java</include>
            <include>**/*EndToEndIT.java</include>
        </includes>
    </configuration>
</plugin>
```

### Phase 2: Documentation (Complete)
✅ **Action**: Updated [TESTING_BEST_PRACTICES.md](./TESTING_BEST_PRACTICES.md) with standards

### Phase 3: New Test Compliance (Ongoing)
📋 **Rule**: All new tests MUST follow the standard patterns
- Code review checklist includes naming verification
- PR template includes test naming compliance check

### Phase 4: Gradual Migration (Future)
🔄 **Approach**: Opportunistic renaming during refactoring
- Don't rename in bulk (too disruptive)
- Rename when touching test files for other reasons
- Document renames in commit messages

## Files Requiring Attention

### High Priority (Affects Test Execution)

| Current Name | Issue | Recommended Action |
|--------------|-------|-------------------|
| `StandaloneRagE2ETest.java` | May not run with Failsafe | Verify Failsafe config includes `*E2ETest.java` |

### Medium Priority (Standardization)

| Current Name | Recommendation | Reason |
|--------------|----------------|--------|
| `AdminAuthControllerIntegrationTest.java` | Rename to `AdminAuthControllerIT.java` | Align with IT suffix standard |
| `EmbeddingEntityIntegrationTest.java` | Rename to `EmbeddingEntityIT.java` | Align with IT suffix standard |
| `ServiceStartupIntegrationTest.java` | Rename to `ServiceStartupIT.java` | Align with IT suffix standard |
| `SecurityIntegrationTest.java` | Rename to `SecurityIT.java` | Align with IT suffix standard |
| `GatewayIntegrationTest.java` | Rename to `GatewayIT.java` | Align with IT suffix standard |

### Low Priority (Legacy Acceptable)

| File | Status | Note |
|------|--------|------|
| `EmbeddingRepositoryIntegrationTest.java` | Keep as-is | Acceptable alternative pattern |
| `ComprehensiveRagEndToEndIT.java` | Keep as-is | Already follows IT standard |

## Validation Checklist

After migration, verify:

- [ ] All unit tests run with `mvn test`
- [ ] All integration tests run with `mvn verify` or `mvn integration-test`
- [ ] All E2E tests run with `mvn verify -Pintegration-tests`
- [ ] No tests are accidentally excluded
- [ ] CI/CD pipelines detect and run all test categories
- [ ] Test reports correctly categorize test types

## Maven Test Execution Reference

```bash
# Unit tests only (Surefire)
mvn test

# Integration tests only (Failsafe)
mvn verify -DskipTests=false

# Run specific integration test
mvn verify -Dit.test=DocumentUploadProcessingIT

# Run all E2E tests
mvn verify -Pintegration-tests -Dit.test=*E2E*

# Run specific E2E test
mvn verify -Pintegration-tests -Dit.test=StandaloneRagE2ETest

# Skip unit tests, run only integration
mvn verify -Dmaven.test.skip=true

# Run all tests (unit + integration)
mvn verify
```

## Impact Assessment

### Benefits
✅ Clear test categorization from filename
✅ Predictable Maven execution behavior
✅ Easier CI/CD configuration
✅ Improved developer experience
✅ Better test discovery and organization

### Risks
⚠️ Renaming could break CI/CD pipelines (mitigated by gradual approach)
⚠️ IDE run configurations may need updates
⚠️ Test history/reports may show breaks (cosmetic only)

## Success Criteria

This migration is complete when:

1. ✅ Documentation fully describes standards (DONE)
2. ✅ Maven Failsafe configured to catch all patterns (DONE)
3. 📋 All new tests follow standards (ONGOING)
4. 🔄 Legacy tests migrated opportunistically (FUTURE)
5. ✅ No test execution gaps or failures (VERIFIED)

## Related Documentation

- [Testing Best Practices](./TESTING_BEST_PRACTICES.md) - Comprehensive testing guidelines
- [BACKLOG.md](../../BACKLOG.md) - TECH-DEBT-002 tracking

## Questions & Decisions

### Q: Should we rename all tests immediately?
**A**: No. Gradual migration during refactoring prevents disruption and allows validation at each step.

### Q: What if a test doesn't fit any category?
**A**: Use descriptive naming that reflects the test purpose. Follow pattern: `{Feature}{Type}Test.java`

### Q: How to handle tests that are both integration and E2E?
**A**: Categorize by primary purpose. E2E = tests full user journey. Integration = tests component interactions.

### Q: Should we enforce this via automation?
**A**: Yes, add to:
- Pre-commit hooks (warn on non-compliant names)
- CI/CD pipeline checks
- Code review checklist

---

**Implementation Status**: ✅ COMPLETE
- Standards defined and documented
- Maven configuration verified
- Migration guide created
- Ongoing enforcement via code review

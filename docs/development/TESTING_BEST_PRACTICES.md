# Testing Best Practices - BYO RAG System

## 🛡️ Quality Assurance & Bug Prevention Guidelines

### 🔍 Recent Bug Fix & Lessons Learned

**ContextAssemblyService Token Limiting Bug (Fixed 2025-09-05)**:
- **Issue**: Service logic allowed first document to bypass token limits with condition `&& documentsUsed > 0`
- **Test Issue**: Test used reflection + ContextConfig redundantly, validated character count instead of token limits
- **Fix**: Corrected service logic and improved test to use public API exclusively
- **Commit**: `ee8bfbbb` - "fix: Correct token limiting logic in ContextAssemblyService and improve test validation"

### 📋 Mandatory Prevention Measures for Future Development

## 1. Service Logic Review Checklist

Before implementing any service method with conditional logic:
- [ ] **Edge Case Analysis**: What happens with empty/null inputs?
- [ ] **Boundary Conditions**: Does logic handle first item, last item, and limits correctly?
- [ ] **Configuration Consistency**: Are default values and runtime overrides handled properly?
- [ ] **Logging Alignment**: Do debug logs match actual code behavior?
- [ ] **Token/Resource Limits**: Are limits enforced consistently across all items?

## 2. Test Design Standards

### MANDATORY for all new tests:
- [ ] **Single Approach**: Use EITHER reflection OR public API, never both
- [ ] **Public API First**: Test through public methods unless testing internal state
- [ ] **Clear Intent**: Test names must clearly describe expected behavior
- [ ] **Realistic Data**: Use production-representative test data
- [ ] **Proper Assertions**: Validate business logic, not implementation artifacts
- [ ] **Documentation**: Document what behavior is being validated and why

### Good vs Bad Test Examples:

#### ✅ GOOD - API Contract Testing
```java
@Test
@DisplayName("Should truncate context when single document exceeds token limit")
void assembleContext_SingleDocumentExceedsLimit_TruncatesContent() {
    // Clear test intent, tests public API
    int maxTokens = 100;
    ContextConfig config = new ContextConfig(maxTokens, 0.7, true, "\n---\n");
    
    String result = service.assembleContext(documents, request, config);
    
    int estimatedTokens = result.length() / 4;
    assertThat(estimatedTokens).isLessThanOrEqualTo(maxTokens);
}
```

#### ❌ BAD - Mixed Approaches
```java
@Test
void assembleContext_MaxLengthLimit_TruncatesContext() {
    // BAD: Using both reflection AND config
    ReflectionTestUtils.setField(service, "maxTokens", 100);
    ContextConfig config = new ContextConfig(100, 0.7, true, "\n---\n");
    
    String result = service.assembleContext(documents, request, config);
    
    // BAD: Testing implementation detail (character count)
    assertTrue(result.length() <= 500);
}
```

## 3. Code Review Requirements

### Every PR must validate:
- [ ] **No Mixed Testing Approaches**: Reflection and public API not used together
- [ ] **Boundary Logic**: Any `if` conditions with item counts/positions reviewed
- [ ] **Configuration Overrides**: Any method accepting config parameters tested properly
- [ ] **Error Messages**: Assertions include descriptive failure messages
- [ ] **Thread Safety**: Service methods are thread-safe for concurrent use

### Code Review Checklist Template:
```markdown
## Service Logic Review
- [ ] Edge cases handled (null, empty, boundary values)
- [ ] Logging matches actual behavior
- [ ] Configuration overrides work correctly
- [ ] Resource limits enforced consistently

## Test Quality Review
- [ ] Tests use single approach (API or reflection, not both)
- [ ] Assertions validate business logic, not implementation
- [ ] Test names clearly describe expected behavior
- [ ] Realistic test data used
- [ ] Descriptive error messages included
```

## 4. Automated Quality Gates

### Maven Plugin Configuration
Add to parent pom.xml:
```xml
<plugin>
    <groupId>com.github.spotbugs</groupId>
    <artifactId>spotbugs-maven-plugin</artifactId>
    <version>4.8.3.1</version>
    <configuration>
        <effort>Max</effort>
        <threshold>Low</threshold>
        <failOnError>true</failOnError>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### Pre-commit Hook
Create `.git/hooks/pre-commit`:
```bash
#!/bin/sh
echo "Running tests before commit..."
mvn test -q
if [ $? -ne 0 ]; then
    echo "❌ Tests failed. Fix tests before committing."
    exit 1
fi

echo "Running static analysis..."
mvn spotbugs:check -q
if [ $? -ne 0 ]; then
    echo "❌ Static analysis failed. Fix issues before committing."
    exit 1
fi

echo "✅ All checks passed. Proceeding with commit."
```

### Test Coverage Requirements
Add to pom.xml:
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <configuration>
        <rules>
            <rule>
                <element>CLASS</element>
                <limits>
                    <limit>
                        <counter>LINE</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.80</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</plugin>
```

## 5. Documentation Standards

### Required Service Method Documentation:
```java
/**
 * Assembles coherent context from retrieved document chunks with token limiting.
 * 
 * @param documents list of retrieved documents with relevance scores
 * @param request the original RAG query request containing tenant and options
 * @param config configuration with token limits. MUST have maxTokens > 0
 * @return assembled context string within token limits
 * @throws IllegalArgumentException if config.maxTokens <= 0
 * @implNote Uses 4-character-per-token estimation for English text.
 *           First document will be truncated if it exceeds maxTokens limit.
 *           Subsequent documents are skipped if they would exceed limit.
 */
public String assembleContext(List<SourceDocument> documents, 
                            RagQueryRequest request, 
                            ContextConfig config) {
```

### Required Test Method Documentation:
```java
/**
 * Validates that the service respects token limits by:
 * 1. Creating a document that would exceed 100 token limit (400+ chars)
 * 2. Configuring ContextConfig with maxTokens=100
 * 3. Verifying returned context stays within token budget using 4-char estimation
 * 
 * This test ensures the token limiting logic works correctly for large documents
 * and prevents LLM context overflow in production scenarios.
 */
@Test
@DisplayName("Should truncate context when single document exceeds token limit")
void assembleContext_SingleDocumentExceedsLimit_TruncatesContent() {
```

## 6. Advanced Testing Strategies

### Parameterized Testing for Boundary Conditions:
```java
@ParameterizedTest
@ValueSource(ints = {1, 50, 100, 500, 1000, 4000})
@DisplayName("Should respect various token limits consistently")
void assembleContext_VariousTokenLimits_AlwaysRespectsLimit(int tokenLimit) {
    ContextConfig config = new ContextConfig(tokenLimit, 0.7, true, "\n---\n");
    String result = service.assembleContext(createLongDocuments(), request, config);
    
    int estimatedTokens = result.length() / 4;
    assertThat(estimatedTokens)
        .describedAs("Context should respect token limit of %d", tokenLimit)
        .isLessThanOrEqualTo(tokenLimit);
}
```

### Property-Based Testing:
```java
@Test
void assembleContext_PropertyBased_TokenLimitsAlwaysRespected() {
    for (int i = 0; i < 100; i++) {
        int randomTokenLimit = ThreadLocalRandom.current().nextInt(10, 5000);
        List<SourceDocument> randomDocs = generateRandomDocuments();
        
        ContextConfig config = new ContextConfig(randomTokenLimit, 0.7, true, "\n---\n");
        String result = service.assembleContext(randomDocs, request, config);
        
        int estimatedTokens = result.length() / 4;
        assertThat(estimatedTokens).isLessThanOrEqualTo(randomTokenLimit);
    }
}
```

## 7. Service Monitoring & Production Validation

### Runtime Validation:
```java
@Service
public class ContextAssemblyService {
    private final MeterRegistry meterRegistry;
    
    public String assembleContext(List<SourceDocument> documents, 
                                RagQueryRequest request, 
                                ContextConfig config) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String result = doAssembleContext(documents, request, config);
        
        // Validate post-conditions in production
        int actualTokens = estimateTokenCount(result);
        if (actualTokens > config.maxTokens()) {
            meterRegistry.counter("context.token.limit.exceeded").increment();
            logger.error("CRITICAL: Token limit exceeded: {} > {} for tenant: {}", 
                        actualTokens, config.maxTokens(), request.tenantId());
        }
        
        sample.stop(Timer.builder("context.assembly.time")
                   .tag("tenant", request.tenantId().toString())
                   .register(meterRegistry));
        return result;
    }
}
```

### Health Check Integration:
```java
@Component
public class ContextAssemblyHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        // Test with known input to verify token limiting works
        try {
            ContextConfig testConfig = new ContextConfig(100, 0.7, true, "\n---\n");
            String result = contextAssemblyService.assembleContext(
                createTestDocuments(), createTestRequest(), testConfig);
            
            int tokens = result.length() / 4;
            if (tokens <= 100) {
                return Health.up()
                    .withDetail("token_limiting", "working")
                    .withDetail("test_tokens", tokens)
                    .build();
            } else {
                return Health.down()
                    .withDetail("token_limiting", "failed")
                    .withDetail("test_tokens", tokens)
                    .withDetail("limit", 100)
                    .build();
            }
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
```

## 8. Priority Implementation Plan

### High Priority (implement in next session):
1. **Add SpotBugs plugin** to parent pom.xml
2. **Create test design checklist** template for PR reviews
3. **Update service documentation** with proper @implNote specifications
4. **Add comprehensive Javadoc** to remaining service methods with boundary conditions

### Medium Priority:
5. **Implement pre-commit hooks** for test and static analysis validation
6. **Add service monitoring** for critical business rules validation
7. **Create parameterized tests** for boundary conditions across services
8. **Implement JaCoCo coverage requirements** with enforcement

### Lower Priority:
9. **Property-based testing** framework setup for complex business logic
10. **Integration test improvements** with realistic multi-service scenarios
11. **Advanced static analysis** rules customization for domain-specific issues
12. **Performance testing** for token limiting under high load

## 🚨 Critical Reminders for Future Sessions

- **NEVER mix reflection and public API** in the same test method
- **ALWAYS validate business logic** instead of implementation details
- **ALWAYS check boundary conditions** in loops and conditionals (first item, last item, empty collections)
- **ALWAYS add descriptive assertions** with expected vs actual values in error messages
- **ALWAYS test the public contract** first, internal state validation second
- **ALWAYS document test intent** with clear @DisplayName and method documentation
- **ALWAYS use realistic test data** that mirrors production usage patterns

## Test Categories & Naming Conventions

### Unit Tests
- **Pattern**: `methodName_condition_expectedBehavior`
- **Example**: `assembleContext_SingleDocumentExceedsLimit_TruncatesContent`

### Integration Tests  
- **Pattern**: `feature_scenario_expectedOutcome`
- **Example**: `ragPipeline_LargeDocument_RespectsTokenLimits`

### Contract Tests
- **Pattern**: `apiMethod_inputCondition_contractBehavior`
- **Example**: `assembleContext_NullDocuments_ReturnsEmptyString`
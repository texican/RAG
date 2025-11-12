---
version: 1.0.0
last-updated: 2025-11-12
status: archived
applies-to: 0.8.0-SNAPSHOT
category: fixes
---

# Embedding Service Conditional Bean Fix

## Issue Summary

The embedding service was experiencing `CrashLoopBackOff` in GCP due to a bean initialization error:

```
Error creating bean with name 'ollamaEmbeddingClient' defined in URL [...]: 
Unsatisfied dependency expressed through constructor parameter 0: 
No qualifying bean of type 'org.springframework.web.client.RestTemplate' available
```

## Root Cause

The `OllamaEmbeddingClient` was annotated with `@Component`, making it always created regardless of the Spring profile. However, it required a `RestTemplate` bean in its constructor, which was only created when the `docker` profile was active. When running with the `gcp` profile (production), the `RestTemplate` bean didn't exist, causing the initialization failure.

### Code Before Fix

```java
@Component
public class OllamaEmbeddingClient {
    private final RestTemplate restTemplate;
    
    public OllamaEmbeddingClient(
            RestTemplate restTemplate,  // ❌ Not available in gcp profile
            @Value("${spring.ai.ollama.base-url:...}") String ollamaBaseUrl,
            @Value("${embedding.models.ollama:...}") String embeddingModel) {
        // ...
    }
}
```

## Solution

Added `@ConditionalOnProperty` annotation to make `OllamaEmbeddingClient` only instantiate when the `docker` profile is active, matching the condition for `RestTemplate` bean creation.

### Code After Fix

```java
@Component
@ConditionalOnProperty(name = "spring.profiles.active", havingValue = "docker")
public class OllamaEmbeddingClient {
    // Now only created when docker profile is active
}
```

## Changes Made

### 1. Source Code Fix

**File:** `rag-embedding-service/src/main/java/com/byo/rag/embedding/client/OllamaEmbeddingClient.java`

- Added `@ConditionalOnProperty(name = "spring.profiles.active", havingValue = "docker")`
- Added import: `org.springframework.boot.autoconfigure.condition.ConditionalOnProperty`
- Updated class-level documentation

### 2. Test Coverage

Created comprehensive unit tests to prevent regression:

#### a. `OllamaEmbeddingClientConditionalTest.java`
Tests conditional bean creation across different profiles:

- **DockerProfileBeanCreationTest**: Validates OllamaEmbeddingClient IS created with docker profile
- **GcpProfileBeanNotCreatedTest**: Validates OllamaEmbeddingClient is NOT created with gcp profile ✅
- **LocalProfileBeanNotCreatedTest**: Validates OllamaEmbeddingClient is NOT created with local profile
- **TestProfileBeanNotCreatedTest**: Validates OllamaEmbeddingClient is NOT created with test profile

#### b. `ApplicationStartupTest.java`
Integration tests for full application startup:

- **GcpProfileStartupTest**: Application starts successfully with gcp profile
- **LocalProfileStartupTest**: Application starts successfully with local profile
- **TestProfileStartupTest**: Application starts successfully with test profile
- **RegressionTestOllamaRestTemplateBug**: Specific regression test for the exact error that occurred

#### c. Updated `EmbeddingConfigTest.java`
Enhanced existing tests to validate:

- Correct primary bean selection per profile
- No bean conflicts
- Registry functionality

## Test Results

All 181 tests passed, including the new tests:

```bash
mvn test
[INFO] Tests run: 181, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## Profile Behavior Matrix

| Profile | OllamaEmbeddingClient | RestTemplate | Primary Embedding Model |
|---------|----------------------|--------------|------------------------|
| docker  | ✅ Created           | ✅ Created   | OllamaEmbeddingModel |
| gcp     | ❌ Not Created       | ❌ Not Created | TransformersEmbeddingModel |
| local   | ❌ Not Created       | ❌ Not Created | TransformersEmbeddingModel |
| test    | ❌ Not Created       | ❌ Not Created | TransformersEmbeddingModel |

## Deployment

### Build
```bash
mvn clean package -DskipTests -pl rag-embedding-service -am
```

### Deploy to GCP
```bash
gcloud builds submit --config=cloudbuild-single.yaml \
  --substitutions=_SERVICE_NAME=rag-embedding \
  --project=byo-rag-dev
```

## Verification

After deployment, verify the fix:

```bash
# Check pod status
kubectl get pods -n rag-system | grep embedding

# Should show Running status, not CrashLoopBackOff
# rag-embedding-xxxxx   1/1     Running   0   30s

# Check logs for successful startup
kubectl logs -n rag-system deployment/rag-embedding --tail=50

# Should see:
# "Started EmbeddingServiceApplication"
# No "Unsatisfied dependency" errors
```

## Prevention

The new tests will catch this issue in CI/CD:

1. **Unit Tests**: Validate conditional bean creation per profile
2. **Integration Tests**: Ensure application starts successfully with each profile
3. **Regression Tests**: Specifically test for the RestTemplate dependency issue

Any future changes that break conditional bean creation will fail the test suite before reaching production.

## Related Files

- `rag-embedding-service/src/main/java/com/byo/rag/embedding/client/OllamaEmbeddingClient.java`
- `rag-embedding-service/src/main/java/com/byo/rag/embedding/config/EmbeddingConfig.java`
- `rag-embedding-service/src/test/java/com/byo/rag/embedding/client/OllamaEmbeddingClientConditionalTest.java`
- `rag-embedding-service/src/test/java/com/byo/rag/embedding/ApplicationStartupTest.java`
- `rag-embedding-service/src/test/java/com/byo/rag/embedding/config/EmbeddingConfigTest.java`

## Date
November 11, 2025

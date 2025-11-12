# Kafka Optional Configuration

**Status**: Active  
**Date**: 2025-11-12  
**Decision**: Kafka messaging has been made optional to simplify deployment and reduce infrastructure costs

## Overview

The RAG system services (Document, Embedding, Core) have been configured to run without requiring Apache Kafka infrastructure. Kafka auto-configuration has been disabled while preserving all Kafka-related code for future re-enablement.

## Motivation

### Problems Addressed
1. **Deployment Blocker**: Document service was crashing due to missing Kafka infrastructure
2. **Infrastructure Cost**: Running Kafka cluster in GCP costs ~$150-450/month (broker + Zookeeper)
3. **Operational Complexity**: Additional monitoring, patching, and scaling overhead
4. **Development Friction**: Developers needed Kafka running locally for development

### Trade-offs Accepted
- **No async document processing**: Document ingestion is now synchronous
- **No event-driven updates**: Embedding service won't auto-process via events
- **Tighter coupling**: Services use direct REST/Feign calls instead of message queues
- **Reduced scalability**: Synchronous calls limit throughput compared to message buffering

## Implementation Details

### Code Changes

#### 1. Application Class Exclusions

**rag-document-service/DocumentServiceApplication.java**:
```java
@SpringBootApplication(
    scanBasePackages = {
        "com.byo.rag.document",
        "com.byo.rag.shared.exception"
    },
    exclude = {
        org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration.class  // ← Added
    }
)
```

**rag-embedding-service/EmbeddingServiceApplication.java**:
```java
@SpringBootApplication(
    scanBasePackages = {
        "com.byo.rag.embedding",
        "com.byo.rag.shared.exception"
    },
    exclude = {
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration.class  // ← Added
    }
)
```

**rag-core-service/CoreServiceApplication.java**:
```java
@SpringBootApplication(
    scanBasePackages = {"com.byo.rag.core", "com.byo.rag.shared.exception"},
    exclude = {
        org.springframework.ai.autoconfigure.ollama.OllamaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration.class,
        org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration.class  // ← Added
    }
)
@EnableAsync
@EnableFeignClients  // ← Removed @EnableKafka annotation
```

#### 2. Conditional Bean Registration

**rag-document-service/service/DocumentProcessingKafkaService.java**:
```java
@Service
@Profile("!test")
@ConditionalOnBean(KafkaTemplate.class)  // ← Added - only instantiate if Kafka is available
public class DocumentProcessingKafkaService implements DocumentProcessingKafkaServiceInterface {
    // ... implementation
}
```

**rag-document-service/listener/DocumentProcessingKafkaListener.java**:
```java
@Component
@Profile("!test")
@ConditionalOnBean(KafkaTemplate.class)  // ← Added - only instantiate if Kafka is available
public class DocumentProcessingKafkaListener {
    // ... implementation
}
```

**rag-document-service/config/KafkaConfig.java**:
```java
@Configuration
@ConditionalOnProperty(
    name = "spring.kafka.enabled",
    havingValue = "true",
    matchIfMissing = false  // ← Added - disabled by default
)
public class KafkaConfig {
    // ... configuration
}
```

**rag-embedding-service/service/EmbeddingKafkaService.java**:
```java
@Service
@ConditionalOnBean(KafkaTemplate.class)  // ← Already present - only instantiate if Kafka is available
public class EmbeddingKafkaService {
    // ... implementation
}
```

**rag-embedding-service/service/DeadLetterQueueService.java**:
```java
@Service
@ConditionalOnBean(KafkaTemplate.class)  // ← Already present
public class DeadLetterQueueService {
    // ... implementation
}
```

**rag-embedding-service/service/NotificationService.java**:
```java
@Service
@ConditionalOnBean(KafkaTemplate.class)  // ← Already present
public class NotificationService {
    // ... implementation
}
```

#### 3. Optional Dependency Injection

**rag-document-service/service/DocumentService.java**:
```java
@Service
public class DocumentService {
    // ... other dependencies
    
    @Autowired(required = false)  // ← Added - makes Kafka service optional
    private DocumentProcessingKafkaServiceInterface kafkaService;
    
    public DocumentService(
            DocumentRepository documentRepository,
            TenantRepository tenantRepository,
            UserRepository userRepository,
            DocumentChunkService chunkService,
            FileStorageService fileStorageService,
            TextExtractionService textExtractionService) {
        // ... constructor no longer requires kafkaService parameter
    }
    
    // Use null check before calling Kafka service
    if (kafkaService != null) {
        kafkaService.sendDocumentForProcessing(document.getId());
    } else {
        // Fallback: trigger direct processing
        processDocument(document.getId());
    }
}
```

#### 4. Removed Annotations

- **rag-core-service**: Removed `@EnableKafka` annotation and import
- Updated JavaDoc comments to reflect Kafka is optional

### Comprehensive Component Audit

#### rag-document-service
| Component | Status | Conditional Logic |
|-----------|--------|-------------------|
| `DocumentProcessingKafkaService` | ✅ Protected | `@ConditionalOnBean(KafkaTemplate.class)` |
| `DocumentProcessingKafkaListener` | ✅ Protected | `@ConditionalOnBean(KafkaTemplate.class)` |
| `TestDocumentProcessingKafkaService` | ✅ Protected | `@Profile("test")` only |
| `KafkaConfig` | ✅ Protected | `@ConditionalOnProperty` |
| `DocumentService.kafkaService` | ✅ Protected | `@Autowired(required=false)` + null checks |

#### rag-embedding-service
| Component | Status | Conditional Logic |
|-----------|--------|-------------------|
| `EmbeddingKafkaService` | ✅ Protected | `@ConditionalOnBean(KafkaTemplate.class)` |
| `DeadLetterQueueService` | ✅ Protected | `@ConditionalOnBean(KafkaTemplate.class)` |
| `NotificationService` | ✅ Protected | `@ConditionalOnBean(KafkaTemplate.class)` |

#### rag-core-service
| Component | Status | Conditional Logic |
|-----------|--------|-------------------|
| No Kafka services | ✅ N/A | Exclusion at application level sufficient |

### What Remains Intact

The following Kafka-related components remain in the codebase but are dormant:

#### Configuration Classes
- `rag-document-service/config/KafkaConfig.java` - Not loaded due to `@ConditionalOnProperty`
- Spring Kafka configuration in `application.yml` files - Ignored when auto-config excluded

#### Listener Classes
- `rag-document-service/listener/DocumentProcessingKafkaListener.java` - Not registered (conditional)
- `rag-embedding-service/service/EmbeddingKafkaService.java` - Not registered (conditional)

#### Environment Variables (K8s)
- `KAFKA_BOOTSTRAP_SERVERS` in deployment YAMLs - Present but unused

#### Dependencies
- `spring-kafka` and Kafka client libraries remain in `pom.xml`
- Required for tests and future re-enablement

## Current Architecture

### Document Processing Flow (Without Kafka)

```
┌─────────────────┐
│  Client Upload  │
└────────┬────────┘
         │
         ▼
┌─────────────────────┐
│ Document Service    │
│  - Validate         │
│  - Store File       │
│  - Extract Text     │
│  - Create Chunks    │
└────────┬────────────┘
         │ (Direct REST/Feign Call)
         ▼
┌─────────────────────┐
│ Embedding Service   │
│  - Generate Vectors │
│  - Store in Redis   │
└─────────────────────┘
```

### Key Characteristics
- **Synchronous**: Each step waits for the previous to complete
- **Direct Coupling**: Services call each other via REST/Feign
- **No Buffering**: Request spikes hit services directly
- **Simpler**: Fewer moving parts, easier to debug

## Testing Impact

### Unit Tests
✅ **All tests pass** - No modifications required

**Why:**
- Document service tests use `@Mock` for Kafka services
- Mock interactions work regardless of auto-configuration
- Example: `DocumentServiceTest` verifies `kafkaService.sendDocumentForProcessing()` calls

### Dependency Validation Tests
✅ **All tests pass** - Kafka classes still on classpath

**Tests:**
- `DependencyValidationTest.shouldHaveKafka()` - Checks for `KafkaTemplate` class
- `InfrastructureValidationTest.kafkaConfigurationShouldBeValid()` - Checks for Kafka client classes

**Why they pass:**
- We excluded **auto-configuration**, not the **dependency**
- Kafka JARs remain on classpath for class loading
- Tests validate class existence, not runtime configuration

### Integration Tests
✅ **Already configured** to exclude Kafka

`rag-integration-tests/.../IntegrationTestConfig.java`:
```java
@EnableAutoConfiguration(exclude = {KafkaAutoConfiguration.class})
```

## Performance Characteristics

### Current (Without Kafka)
- **Throughput**: Limited by synchronous call chain
- **Latency**: Sum of all service call latencies
- **Failure Impact**: Single service failure breaks entire chain
- **Scalability**: Vertical scaling only (bigger pods)

### With Kafka (Future)
- **Throughput**: Message buffering enables higher peak load
- **Latency**: Fire-and-forget for async operations
- **Failure Impact**: Message queue provides resilience
- **Scalability**: Horizontal scaling with consumer groups

## Cost Analysis

### Infrastructure Savings (Monthly)
```
Kafka Cluster:
- 3 Broker nodes (t3.medium)     ~$150-250
- 3 Zookeeper nodes (t3.small)   ~$50-100
- Storage (500GB)                ~$50-100
─────────────────────────────────────────
Total Monthly Savings:           ~$250-450
```

### Additional Benefits
- No Kafka monitoring/alerting setup
- No Kafka version upgrades/patching
- Reduced operational knowledge requirement
- Simpler disaster recovery (no message replay)

## Re-enabling Kafka

When message-driven architecture becomes necessary:

### Step 1: Deploy Kafka Infrastructure

**Option A: GCP Pub/Sub (Recommended)**
```bash
# Create Pub/Sub topics
gcloud pubsub topics create document-processing
gcloud pubsub topics create embedding-generation

# Update application.yml to use Pub/Sub
spring:
  cloud:
    gcp:
      pubsub:
        project-id: ${GCP_PROJECT_ID}
```

**Option B: Confluent Cloud**
```bash
# Managed Kafka service - no infrastructure management
# Update KAFKA_BOOTSTRAP_SERVERS to Confluent endpoint
```

**Option C: Self-Hosted Kafka on GKE**
```bash
# Deploy using Strimzi operator
kubectl create namespace kafka
kubectl apply -f 'https://strimzi.io/install/latest?namespace=kafka'
kubectl apply -f k8s/kafka/kafka-cluster.yaml
```

### Step 2: Remove Auto-Configuration Exclusions and Re-enable Conditional Beans

**rag-document-service/DocumentServiceApplication.java**:
```java
@SpringBootApplication(
    scanBasePackages = {
        "com.byo.rag.document",
        "com.byo.rag.shared.exception"
    }
    // Remove exclude = {KafkaAutoConfiguration.class}
)
```

**rag-document-service/service/DocumentProcessingKafkaService.java**:
```java
@Service
@Profile("!test")
@ConditionalOnBean(KafkaTemplate.class)  // Keep this - still valid protection
public class DocumentProcessingKafkaService implements DocumentProcessingKafkaServiceInterface {
    // No changes needed - will auto-register when KafkaTemplate is available
}
```

**rag-document-service/listener/DocumentProcessingKafkaListener.java**:
```java
@Component
@Profile("!test")
@ConditionalOnBean(KafkaTemplate.class)  // Keep this - still valid protection
public class DocumentProcessingKafkaListener {
    // No changes needed - will auto-register when KafkaTemplate is available
}
```

**rag-document-service/config/KafkaConfig.java**:
```java
@Configuration
@ConditionalOnProperty(
    name = "spring.kafka.enabled",
    havingValue = "true",
    matchIfMissing = true  // ← Change to true to enable by default when Kafka is available
)
public class KafkaConfig {
    // No changes needed
}
```

**rag-document-service/service/DocumentService.java**:
```java
@Service
public class DocumentService {
    // Keep @Autowired(required = false) - allows graceful degradation if Kafka unavailable
    @Autowired(required = false)
    private DocumentProcessingKafkaServiceInterface kafkaService;
    
    // Keep null checks - defensive programming for runtime Kafka issues
    if (kafkaService != null) {
        kafkaService.sendDocumentForProcessing(document.getId());
    } else {
        processDocument(document.getId());
    }
}
```

**rag-embedding-service/EmbeddingServiceApplication.java**:
```java
@SpringBootApplication(
    scanBasePackages = {
        "com.byo.rag.embedding",
        "com.byo.rag.shared.exception"
    },
    exclude = {
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class
        // Remove KafkaAutoConfiguration.class
    }
)
```

**rag-embedding-service Kafka services**:
```java
// No changes needed - these already have @ConditionalOnBean(KafkaTemplate.class)
// They will auto-register when Kafka auto-configuration creates KafkaTemplate
@Service
@ConditionalOnBean(KafkaTemplate.class)
public class EmbeddingKafkaService { ... }

@Service
@ConditionalOnBean(KafkaTemplate.class)
public class DeadLetterQueueService { ... }

@Service
@ConditionalOnBean(KafkaTemplate.class)
public class NotificationService { ... }
```

**rag-core-service/CoreServiceApplication.java**:
```java
@SpringBootApplication(
    scanBasePackages = {"com.byo.rag.core", "com.byo.rag.shared.exception"},
    exclude = {
        org.springframework.ai.autoconfigure.ollama.OllamaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration.class
        // Remove KafkaAutoConfiguration.class
    }
)
@EnableAsync
@EnableKafka  // ← Add back this annotation
@EnableFeignClients
```

**Add import**:
```java
import org.springframework.kafka.annotation.EnableKafka;
```

### Step 3: Update Configuration

**Update K8s ConfigMap** (`k8s/base/gcp-config.yaml`):
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: gcp-config
  namespace: rag-system
data:
  KAFKA_BOOTSTRAP_SERVERS: "kafka-broker-0.kafka-broker:9092,kafka-broker-1.kafka-broker:9092"
  # OR for Pub/Sub:
  # SPRING_CLOUD_GCP_PUBSUB_PROJECT_ID: "byo-rag-dev"
```

**Update Deployments** - Environment variables are already present:
```yaml
- name: KAFKA_BOOTSTRAP_SERVERS
  value: "kafka:9092"  # Already configured
```

### Step 4: Verify Kafka Integration

**Check Kafka connection**:
```bash
# Verify pods can reach Kafka
kubectl run kafka-test --rm -i --tty --image=confluentinc/cp-kafka:latest \
  -- kafka-topics --list --bootstrap-server kafka-broker-0.kafka-broker:9092
```

**Monitor Kafka logs**:
```bash
# Check if listeners are registering
kubectl logs -n rag-system -l app=rag-document | grep "KafkaListenerEndpointRegistry"
kubectl logs -n rag-system -l app=rag-embedding | grep "KafkaListenerEndpointRegistry"
```

**Test message flow**:
```bash
# Upload a document and check if Kafka messages are produced
curl -X POST http://rag-document:8082/api/documents/upload \
  -F "file=@test.pdf" \
  -H "Authorization: Bearer $TOKEN"

# Check Kafka topic for messages
kubectl exec -n kafka kafka-broker-0 -- kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic document-processing \
  --from-beginning --max-messages 1
```

### Step 5: Update Documentation

Update the following comments in code:
- `rag-core-service/CoreServiceApplication.java` - Add back Kafka to features list
- `rag-document-service/config/KafkaConfig.java` - Update class-level comments
- `k8s/base/*-deployment.yaml` - Update Kafka environment variable comments

### Step 6: Rebuild and Deploy

```bash
# Rebuild all services
gcloud builds submit --config=cloudbuild.yaml

# Restart deployments to pick up new configuration
kubectl rollout restart deployment rag-document -n rag-system
kubectl rollout restart deployment rag-embedding -n rag-system
kubectl rollout restart deployment rag-core -n rag-system

# Verify all pods are healthy
kubectl get pods -n rag-system
```

## Migration Path: GCP Pub/Sub

For GCP-native deployments, consider using Pub/Sub instead of Kafka:

### Benefits
- Fully managed (no infrastructure)
- Automatic scaling
- 99.95% SLA
- Pay-per-use pricing
- Native GCP IAM integration

### Implementation
```xml
<!-- Add to pom.xml -->
<dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>spring-cloud-gcp-starter-pubsub</artifactId>
</dependency>
```

Update `application.yml`:
```yaml
spring:
  cloud:
    gcp:
      pubsub:
        project-id: ${GCP_PROJECT_ID}
        emulator-host: ${PUBSUB_EMULATOR_HOST:}  # For local testing

# Define topic mappings
pubsub:
  topics:
    document-processing: projects/byo-rag-dev/topics/document-processing
    embedding-generation: projects/byo-rag-dev/topics/embedding-generation
```

Replace `@KafkaListener` with `@PubsubListener`:
```java
@PubsubListener(subscription = "document-processing-subscription")
public void processDocument(DocumentEvent event) {
    // Process document
}
```

## Monitoring and Observability

### Current State (Without Kafka)
Monitor direct service calls:
- **Metrics**: Service-to-service latency via Feign metrics
- **Tracing**: Distributed traces show full synchronous chain
- **Logs**: Sequential log correlation via trace IDs

### Future State (With Kafka)
Add Kafka-specific monitoring:
- **Lag Monitoring**: Consumer group lag for each topic
- **Throughput**: Messages produced/consumed per second
- **Error Rate**: Dead letter queue message count
- **Partition Balance**: Even distribution across consumers

## Related Documentation

- [Architecture Decision Records](../architecture/README.md)
- [GCP Deployment Guide](../deployment/gcp-deployment.md)
- [Service Communication Patterns](../architecture/service-communication.md)
- [Kafka Configuration Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/messaging.html#messaging.kafka)
- [GCP Pub/Sub Documentation](https://cloud.google.com/pubsub/docs)

## Changelog

| Date | Change | Author |
|------|--------|--------|
| 2025-11-12 | Initial implementation - Made Kafka optional | System |
| 2025-11-12 | Removed @EnableKafka from rag-core | System |
| 2025-11-12 | Added conditional bean registration for all Kafka components | System |
| 2025-11-12 | Made DocumentService.kafkaService optional with fallback logic | System |
| 2025-11-12 | Added comprehensive component audit to documentation | System |

## Related Documentation

- [Deployment Troubleshooting](../operations/DEPLOYMENT_TROUBLESHOOTING.md) - Pod startup and storage issues
- [Architecture Decision Records](../architecture/README.md)

## Related Documentation

- [Deployment Troubleshooting](../operations/DEPLOYMENT_TROUBLESHOOTING.md) - Pod startup and storage issues
- [Architecture Decision Records](../architecture/README.md)
- [GCP Deployment Guide](../deployment/gcp-deployment.md)
- [Service Communication Patterns](../architecture/service-communication.md)
- [Kafka Configuration Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/messaging.html#messaging.kafka)
- [GCP Pub/Sub Documentation](https://cloud.google.com/pubsub/docs)

## Deployment Issues Encountered and Resolved

### Issue 1: Slow Startup Causing Liveness Probe Failures

**Problem**: rag-document pods were failing liveness probes and being killed by kubelet (exit code 137) because the application took 80-95 seconds to start, but liveness probe `initialDelaySeconds` was only 60s.

**Symptoms**:
- Pods stuck in CrashLoopBackOff or 1/2 Running
- Events showing "Liveness probe failed: connect: connection refused"
- Container terminations with exit code 137 (SIGKILL from kubelet)
- Logs showed clean startup but pod killed before completion

**Root Cause**: Spring Boot + Hibernate + JPA initialization takes ~90 seconds in GKE environment, exceeding the liveness probe initial delay.

**Solution Applied**:
1. Added `startupProbe` to handle long initialization (30 attempts × 10s = 5 min max startup)
2. Reduced `livenessProbe.initialDelaySeconds` to 10s (startupProbe handles initial startup)
3. Reduced `readinessProbe.initialDelaySeconds` to 10s

**Configuration Changes** (k8s/base/rag-document-deployment.yaml):
```yaml
startupProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8082
  failureThreshold: 30  # 30 * 10s = 5 minutes max startup time
  periodSeconds: 10
  timeoutSeconds: 5
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8082
  initialDelaySeconds: 10  # Reduced since startupProbe handles initial startup
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8082
  initialDelaySeconds: 10  # Reduced since startupProbe handles initial startup
  periodSeconds: 5
  timeoutSeconds: 3
  failureThreshold: 3
```

**Result**: Pods now start successfully and reach 2/2 Running within 2 minutes.

### Issue 2: PVC Multi-Attach Errors

**Problem**: Multiple rag-document replicas attempting to mount the same ReadWriteOnce (RWO) PersistentVolumeClaim across different nodes, causing Multi-Attach errors and pods stuck in ContainerCreating.

**Symptoms**:
- Events: "Multi-Attach error for volume ... Volume is already used by pod(s) ..."
- Some pods stuck in ContainerCreating indefinitely
- Scheduling churn as Kubernetes tries to place pods

**Root Cause**: GCE Persistent Disk (standard storage class) only supports ReadWriteOnce, meaning it can only be mounted by one pod at a time. Multiple replicas require ReadWriteMany (RWX) storage.

**Temporary Solution Applied**:
- Reduced `spec.replicas` from 2 to 1 in rag-document deployment
- Comment added: `# Temporarily reduced from 2 due to ReadWriteOnce PVC multi-attach issues`

**Long-term Solutions** (to be implemented):
1. **Option A: Use ReadWriteMany storage** (recommended for shared document storage)
   - Migrate to GCP Filestore (NFS-based RWX storage)
   - Update PVC storageClassName from `standard` to `filestore-csi` or similar
   - Cost: ~$200-300/month for Filestore Basic

2. **Option B: Use per-pod PVCs with StatefulSet**
   - Convert Deployment to StatefulSet with volumeClaimTemplates
   - Each pod gets its own PVC (document-storage-pvc-0, document-storage-pvc-1, etc.)
   - Requires application logic changes if documents need to be shared

3. **Option C: Use object storage (GCS)**
   - Replace local file storage with Google Cloud Storage buckets
   - No PVC needed, infinite scalability
   - Requires code changes in FileStorageService
   - Cost: ~$0.02/GB/month for Standard Storage

**Recommendation**: For production, implement Option C (GCS) as it provides best scalability and reliability. For development/testing, Option A (Filestore) is simpler.

**Status**: ✅ Temporary fix applied (replicas=1). Long-term fix tracked in backlog.

## Questions and Answers

**Q: Will this break existing functionality?**  
A: No. The Kafka listeners were not being invoked because Kafka infrastructure was not deployed. Services continue to work via direct REST calls.

**Q: Are we removing Kafka dependencies?**  
A: No. Dependencies remain for future re-enablement and test compatibility. We're only excluding auto-configuration.

**Q: What about message ordering?**  
A: Current synchronous flow maintains strict ordering. With Kafka, ordering would be per-partition.

**Q: How do we handle retries?**  
A: Current: Feign retry logic. Future with Kafka: Consumer retry policies + dead letter queues.

**Q: What's the rollback plan?**  
A: Simply revert the exclusion changes and redeploy. Kafka listener code is unchanged and ready to activate.

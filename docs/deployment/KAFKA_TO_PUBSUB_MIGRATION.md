---
version: 1.0.0
last-updated: 2025-11-12
status: active
applies-to: 0.8.0-SNAPSHOT
category: deployment
---

# Kafka to Cloud Pub/Sub Migration Guide

## Overview

This guide provides step-by-step instructions for migrating the RAG system from containerized Apache Kafka to Google Cloud Pub/Sub.

**Decision**: ✅ Cloud Pub/Sub (see [KAFKA_TO_PUBSUB_DECISION.md](KAFKA_TO_PUBSUB_DECISION.md))

**Effort**: 13 story points (~2-3 weeks)

**Benefits**:
- 95% cost reduction ($7/mo vs $175/mo)
- Zero operational overhead (serverless)
- Better GCP integration
- Automatic scaling

---

## Prerequisites

Before starting the migration:

1. ✅ Complete GCP-INFRA-001 (GCP project setup)
2. ✅ Complete GCP-SECRETS-002 (Secret Manager)
3. ✅ Complete GCP-REGISTRY-003 (Container Registry)
4. ✅ GKE cluster created (GCP-GKE-007) - if deploying
5. ✅ Run Pub/Sub setup script: `bash scripts/gcp/11-setup-pubsub.sh`

---

## Migration Phases

### Phase 1: Infrastructure Setup (2 story points)

**Status**: ✅ Can be done immediately

**Tasks**:
1. Enable Pub/Sub API
2. Create topics and subscriptions
3. Configure IAM permissions
4. Set up monitoring

**Script**:
```bash
cd /Users/stryfe/Projects/RAG_SpecKit/RAG
bash scripts/gcp/11-setup-pubsub.sh
```

**Verify**:
```bash
# List topics
gcloud pubsub topics list --project=byo-rag-dev

# List subscriptions
gcloud pubsub subscriptions list --project=byo-rag-dev

# Test message
gcloud pubsub topics publish document-processing \
    --project=byo-rag-dev \
    --message='{"documentId":"test-123"}'

# Pull message
gcloud pubsub subscriptions pull document-service-processor \
    --project=byo-rag-dev \
    --auto-ack \
    --limit=1
```

---

### Phase 2: Code Migration - Document Service (4 story points)

**Services**: rag-document-service

**Topics Used**:
- Producer: `document-processing`, `embedding-generation`
- Consumer: `document-processing`

#### Step 1: Update Dependencies

**File**: `rag-document-service/pom.xml`

```xml
<!-- Remove Kafka dependency -->
<!-- REMOVE THIS:
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
-->

<!-- Add Pub/Sub dependency -->
<dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>spring-cloud-gcp-starter-pubsub</artifactId>
    <version>4.9.0</version>
</dependency>
```

#### Step 2: Update Configuration

**File**: `rag-document-service/src/main/resources/application.yml`

```yaml
# REMOVE Kafka configuration:
# spring:
#   kafka:
#     bootstrap-servers: kafka:29092
#     producer: ...
#     consumer: ...

# ADD Pub/Sub configuration:
spring:
  cloud:
    gcp:
      pubsub:
        project-id: ${GCP_PROJECT_ID:byo-rag-dev}
        emulator-host: ${PUBSUB_EMULATOR_HOST:}  # For local testing
        
# Keep topic names (they match Pub/Sub topics)
pubsub:
  topics:
    document-processing: document-processing
    embedding-generation: embedding-generation
  subscriptions:
    document-processor: document-service-processor
```

#### Step 3: Create Pub/Sub Producer Service

**File**: `rag-document-service/src/main/java/com/byo/rag/document/service/DocumentProcessingPubSubService.java`

```java
package com.byo.rag.document.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class DocumentProcessingPubSubService {
    
    private final PubSubTemplate pubSubTemplate;
    private final ObjectMapper objectMapper;
    private final String documentProcessingTopic;
    private final String embeddingGenerationTopic;
    
    public DocumentProcessingPubSubService(
            PubSubTemplate pubSubTemplate,
            ObjectMapper objectMapper,
            @Value("${pubsub.topics.document-processing}") String documentProcessingTopic,
            @Value("${pubsub.topics.embedding-generation}") String embeddingGenerationTopic) {
        this.pubSubTemplate = pubSubTemplate;
        this.objectMapper = objectMapper;
        this.documentProcessingTopic = documentProcessingTopic;
        this.embeddingGenerationTopic = embeddingGenerationTopic;
    }
    
    public CompletableFuture<Void> sendDocumentForProcessing(UUID documentId) {
        try {
            String messageJson = objectMapper.writeValueAsString(
                Map.of("documentId", documentId.toString())
            );
            
            return pubSubTemplate.publish(documentProcessingTopic, messageJson)
                .thenAccept(messageId -> {
                    log.info("Published document processing message: documentId={}, messageId={}", 
                        documentId, messageId);
                })
                .exceptionally(ex -> {
                    log.error("Failed to publish document processing message: documentId={}", 
                        documentId, ex);
                    return null;
                });
        } catch (Exception e) {
            log.error("Error serializing document processing message", e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    public CompletableFuture<Void> sendChunkForEmbedding(UUID chunkId, String content) {
        try {
            String messageJson = objectMapper.writeValueAsString(
                Map.of("chunkId", chunkId.toString(), "content", content)
            );
            
            return pubSubTemplate.publish(embeddingGenerationTopic, messageJson)
                .thenAccept(messageId -> {
                    log.info("Published embedding generation message: chunkId={}, messageId={}", 
                        chunkId, messageId);
                })
                .exceptionally(ex -> {
                    log.error("Failed to publish embedding generation message: chunkId={}", 
                        chunkId, ex);
                    return null;
                });
        } catch (Exception e) {
            log.error("Error serializing embedding generation message", e);
            return CompletableFuture.failedFuture(e);
        }
    }
}
```

#### Step 4: Create Pub/Sub Consumer

**File**: `rag-document-service/src/main/java/com/byo/rag/document/listener/DocumentProcessingPubSubListener.java`

```java
package com.byo.rag.document.listener;

import com.byo.rag.document.service.DocumentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gcp.pubsub.core.subscriber.PubSubSubscriberTemplate;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class DocumentProcessingPubSubListener {
    
    private final DocumentService documentService;
    private final ObjectMapper objectMapper;
    
    public DocumentProcessingPubSubListener(
            DocumentService documentService,
            ObjectMapper objectMapper) {
        this.documentService = documentService;
        this.objectMapper = objectMapper;
    }
    
    @ServiceActivator(inputChannel = "document-processing-channel")
    public void handleDocumentProcessing(Message<?> message) {
        BasicAcknowledgeablePubsubMessage pubsubMessage = 
            message.getHeaders().get(GcpPubSubHeaders.ORIGINAL_MESSAGE, 
                BasicAcknowledgeablePubsubMessage.class);
        
        try {
            String payload = new String((byte[]) message.getPayload());
            log.info("Received document processing message: {}", payload);
            
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            UUID documentId = UUID.fromString((String) data.get("documentId"));
            
            // Process document
            documentService.processDocument(documentId);
            
            // Acknowledge message
            if (pubsubMessage != null) {
                pubsubMessage.ack();
                log.info("Successfully processed and acknowledged: documentId={}", documentId);
            }
            
        } catch (Exception e) {
            log.error("Error processing document message", e);
            
            // Nack message to retry or send to DLQ
            if (pubsubMessage != null) {
                pubsubMessage.nack();
            }
        }
    }
}
```

#### Step 5: Configure Pub/Sub Channel

**File**: `rag-document-service/src/main/java/com/byo/rag/document/config/PubSubConfig.java`

```java
package com.byo.rag.document.config;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;

@Configuration
public class PubSubConfig {
    
    @Value("${pubsub.subscriptions.document-processor}")
    private String documentProcessorSubscription;
    
    @Bean
    public MessageChannel documentProcessingChannel() {
        return new DirectChannel();
    }
    
    @Bean
    public PubSubInboundChannelAdapter documentProcessingAdapter(
            PubSubTemplate pubSubTemplate,
            MessageChannel documentProcessingChannel) {
        
        PubSubInboundChannelAdapter adapter = 
            new PubSubInboundChannelAdapter(pubSubTemplate, documentProcessorSubscription);
        adapter.setOutputChannel(documentProcessingChannel);
        adapter.setAckMode(AckMode.MANUAL);
        adapter.setPayloadType(byte[].class);
        
        return adapter;
    }
}
```

#### Step 6: Update Existing Services

Replace `DocumentProcessingKafkaService` usage with `DocumentProcessingPubSubService`:

**File**: `rag-document-service/src/main/java/com/byo/rag/document/service/DocumentService.java`

```java
// OLD:
// private final DocumentProcessingKafkaService kafkaService;

// NEW:
private final DocumentProcessingPubSubService pubSubService;

// Update method calls:
// kafkaService.sendDocumentForProcessing(documentId);
pubSubService.sendDocumentForProcessing(documentId);

// kafkaService.sendChunkForEmbedding(chunk.getId(), chunk.getContent());
pubSubService.sendChunkForEmbedding(chunk.getId(), chunk.getContent());
```

#### Step 7: Testing

```bash
# Build service
mvn clean package -DskipTests -pl rag-document-service

# Run with Pub/Sub emulator (local testing)
export PUBSUB_EMULATOR_HOST=localhost:8085
gcloud beta emulators pubsub start --project=byo-rag-dev

# In another terminal, run service
export GCP_PROJECT_ID=byo-rag-dev
export PUBSUB_EMULATOR_HOST=localhost:8085
mvn spring-boot:run -pl rag-document-service

# Test document upload
curl -X POST http://localhost:8082/api/v1/documents/upload \
  -H 'X-Tenant-ID: YOUR_TENANT_ID' \
  -F 'file=@test.txt'
```

---

### Phase 3: Code Migration - Embedding Service (3 story points)

**Services**: rag-embedding-service

**Topics Used**:
- Consumer: `embedding-generation`
- Producer: `dead-letter-queue` (for failed embeddings)

Follow similar pattern as Document Service:

1. Update `pom.xml` dependencies
2. Update `application.yml` configuration
3. Create `EmbeddingPubSubConsumer.java`
4. Create `DeadLetterQueuePubSubService.java`
5. Configure Pub/Sub channels
6. Update service integration points
7. Test with Pub/Sub emulator

---

### Phase 4: Code Migration - Core Service (3 story points)

**Services**: rag-core-service

**Topics Used**:
- Producer & Consumer: `rag-queries`, `rag-responses`, `feedback`

Follow similar pattern:

1. Update `pom.xml` dependencies
2. Update `application.yml` configuration
3. Create `RAGQueryPubSubService.java`
4. Create `FeedbackPubSubListener.java`
5. Configure Pub/Sub channels
6. Update service integration points
7. Test with Pub/Sub emulator

---

### Phase 5: Testing & Validation (1 story point)

#### Integration Testing

```bash
# Start Pub/Sub emulator
gcloud beta emulators pubsub start --project=byo-rag-dev

# Set environment
export GCP_PROJECT_ID=byo-rag-dev
export PUBSUB_EMULATOR_HOST=localhost:8085

# Run integration tests
mvn verify -Pintegration-tests
```

#### E2E Testing in GCP

```bash
# Deploy services to GKE
kubectl apply -f k8s/

# Run E2E tests
mvn verify -pl rag-integration-tests -Pe2e-tests
```

#### Performance Testing

```bash
# Load test with 100 concurrent document uploads
ab -n 1000 -c 100 -p test-doc.json \
  -T application/json \
  http://YOUR_SERVICE_URL/api/v1/documents/upload
```

#### Monitor Pub/Sub Metrics

```bash
# View message metrics
gcloud monitoring time-series list \
  --filter='resource.type="pubsub_subscription"' \
  --project=byo-rag-dev
```

---

## Rollback Procedure

If issues arise during migration:

### Option 1: Feature Flag Rollback

```yaml
# application.yml
messaging:
  provider: ${MESSAGING_PROVIDER:kafka}  # Switch to "pubsub" when ready

spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:kafka-profile}
```

### Option 2: Kubernetes Rollback

```bash
# Rollback to previous deployment
kubectl rollout undo deployment/rag-document-service

# Verify rollback
kubectl rollout status deployment/rag-document-service
```

### Option 3: Emergency Kafka Restart

```bash
# Restart Kafka containers
docker-compose up -d zookeeper kafka

# Restart services pointing to Kafka
docker-compose restart rag-document rag-embedding rag-core
```

---

## Monitoring and Alerting

### Key Metrics to Monitor

1. **Message Publish Latency**
   - Target: <100ms p95
   - Alert: >500ms p95

2. **Message Processing Latency**
   - Target: <5s p95
   - Alert: >30s p95

3. **Dead Letter Queue Size**
   - Target: 0 messages
   - Alert: >10 messages

4. **Subscription Backlog**
   - Target: <1000 messages
   - Alert: >10,000 messages

5. **Message Delivery Failures**
   - Target: <0.1% failure rate
   - Alert: >1% failure rate

### Cloud Monitoring Dashboards

```bash
# View Pub/Sub dashboard
open https://console.cloud.google.com/monitoring/dashboards?project=byo-rag-dev
```

---

## Cost Optimization

### Expected Costs

Based on estimated workload:

| Scenario | Messages/Month | Cost/Month |
|----------|----------------|------------|
| Low (Development) | 1M | $0.69 |
| Medium (Staging) | 10M | $6.90 |
| High (Production) | 100M | $69.00 |

### Optimization Tips

1. **Batch Messages**: Publish multiple messages at once
2. **Compression**: Use gzip compression for large payloads
3. **Acknowledgment**: Ack messages promptly to avoid redelivery
4. **Subscription Filters**: Filter messages at subscription level
5. **Message Retention**: Reduce retention from 7 days if not needed

---

## Troubleshooting

### Common Issues

#### Issue: Messages Not Being Delivered

**Symptoms**: Published messages not appearing in subscription

**Solutions**:
1. Check topic and subscription exist:
   ```bash
   gcloud pubsub topics list --project=byo-rag-dev
   gcloud pubsub subscriptions list --project=byo-rag-dev
   ```

2. Check IAM permissions:
   ```bash
   gcloud projects get-iam-policy byo-rag-dev \
     --flatten="bindings[].members" \
     --filter="bindings.role:roles/pubsub.publisher"
   ```

3. Check service account in GKE pod:
   ```bash
   kubectl exec -it POD_NAME -- env | grep GOOGLE
   ```

#### Issue: High Latency

**Symptoms**: Message processing takes >10 seconds

**Solutions**:
1. Check subscription backlog:
   ```bash
   gcloud pubsub subscriptions describe SUBSCRIPTION_NAME \
     --project=byo-rag-dev \
     --format="value(numUndeliveredMessages)"
   ```

2. Increase parallel processing (adjust `maxConcurrentMessages`):
   ```yaml
   spring:
     cloud:
       gcp:
         pubsub:
           subscriber:
             max-ack-extension-period: 0
             parallel-pull-count: 5
   ```

3. Optimize ack deadline:
   ```bash
   gcloud pubsub subscriptions update SUBSCRIPTION_NAME \
     --ack-deadline=120 \
     --project=byo-rag-dev
   ```

#### Issue: Messages Going to DLQ

**Symptoms**: Messages in dead-letter-queue topic

**Solutions**:
1. Pull and inspect DLQ messages:
   ```bash
   gcloud pubsub subscriptions pull dlq-monitor \
     --project=byo-rag-dev \
     --limit=10
   ```

2. Check application logs for errors
3. Fix bug and republish messages from DLQ

---

## Success Criteria

- ✅ All 5 topics created in Pub/Sub
- ✅ All subscriptions created with DLQ
- ✅ All services migrated and deployed
- ✅ E2E tests passing
- ✅ Message latency <200ms p95
- ✅ Zero message loss
- ✅ Cost under $10/month
- ✅ Kafka containers removed from docker-compose.yml
- ✅ Documentation updated

---

## References

- [Cloud Pub/Sub Documentation](https://cloud.google.com/pubsub/docs)
- [Spring Cloud GCP Pub/Sub](https://cloud.spring.io/spring-cloud-static/spring-cloud-gcp/current/reference/html/#spring-cloud-gcp-for-pub-sub)
- [Migrating from Kafka](https://cloud.google.com/pubsub/docs/migrating-from-kafka)
- [Pub/Sub Best Practices](https://cloud.google.com/pubsub/docs/publisher)
- [KAFKA_TO_PUBSUB_DECISION.md](KAFKA_TO_PUBSUB_DECISION.md)

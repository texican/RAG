---
version: 1.0.0
last-updated: 2025-11-12
status: active
applies-to: 0.8.0-SNAPSHOT
category: deployment
---

# GCP-KAFKA-006: Kafka to Cloud Pub/Sub Migration Decision

## Executive Summary

**Decision**: Migrate from containerized Apache Kafka to **Google Cloud Pub/Sub**

**Rationale**: Cloud Pub/Sub offers better GCP integration, lower operational complexity, and significant cost savings while meeting all functional requirements of the RAG system.

**Implementation Approach**: Phased migration with adapter pattern to minimize code changes

**Estimated Effort**: 13 story points (as planned)

---

## Current State Analysis

### Kafka Usage in RAG System

**Topics (5 total)**:
1. `document-processing` - Document service → Async document processor
2. `embedding-generation` - Document service → Embedding service  
3. `rag-queries` - External → Core service (query requests)
4. `rag-responses` - Core service → External (query responses)
5. `feedback` - External → Core service (user feedback)

**Services Using Kafka**:
- **Document Service**: Producer (document-processing, embedding-generation) + Consumer (document-processing)
- **Embedding Service**: Consumer (embedding-generation) + Dead Letter Queue
- **Core Service**: Producer + Consumer (rag-queries, rag-responses, feedback)

**Message Patterns**:
- Simple string/JSON payloads
- Standard publish-subscribe
- Single consumer group per service
- No Kafka Streams
- No complex transactions
- No exactly-once semantics required

**Current Infrastructure**:
- Confluent Kafka 7.4.0 (Docker container)
- Zookeeper 7.4.0 (Docker container)
- Single broker (replication factor: 1)
- Local development only

---

## Option A: Cloud Pub/Sub (RECOMMENDED ✅)

### Overview
Google Cloud's fully managed, serverless messaging service. Native GCP integration with automatic scaling, no infrastructure management.

### Pros ✅

**1. Operational Simplicity**
- **Zero infrastructure management**: No brokers, no Zookeeper, no upgrades
- **Serverless**: Auto-scales from zero to millions of messages/second
- **No capacity planning**: Pay only for actual usage
- **Managed by Google**: 99.95% SLA, automatic backups, multi-region replication

**2. GCP Integration**
- **Native GCP service**: First-class integration with GKE, IAM, Cloud Monitoring
- **Workload Identity**: Seamless authentication from GKE pods (no API keys)
- **Cloud Logging**: Automatic message logging and audit trails
- **Cloud Monitoring**: Built-in dashboards and alerts
- **VPC Service Controls**: Enhanced security boundaries

**3. Cost Efficiency**
- **Pay-per-message**: ~$0.06/GB ingress, $0.09/GB egress
- **No idle costs**: No minimum fees, no reserved capacity
- **Estimated cost**: $20-40/month for RAG workload (vs $150-300/month for Kafka)
- **Free tier**: 10 GB/month included

**4. Developer Experience**
- **Spring Cloud GCP**: Native Spring Boot integration with `@PubSubListener`
- **Simpler configuration**: No broker URLs, just project ID and topic names
- **Message retention**: 7 days default (vs 7 days Kafka with manual config)
- **Ordering keys**: Per-key message ordering (like Kafka partitions)
- **Dead letter queues**: Built-in DLQ support

**5. Reliability**
- **At-least-once delivery**: Guaranteed message delivery
- **Automatic retries**: Exponential backoff with jitter
- **Message acknowledgment**: Flexible ack deadlines (10s - 600s)
- **Multi-region replication**: Data replicated across zones automatically

### Cons ❌

**1. Code Changes Required**
- Replace Spring Kafka with Spring Cloud GCP Pub/Sub
- Change `@KafkaListener` → `@PubSubListener` (or use adapter pattern)
- Update producer code: `KafkaTemplate` → `PubSubTemplate`
- Update configuration: Remove Kafka config, add Pub/Sub config
- **Mitigation**: Use adapter pattern to minimize impact

**2. Learning Curve**
- Team needs to learn Pub/Sub concepts (subscriptions vs consumer groups)
- Different terminology (topics/subscriptions vs topics/consumer groups)
- **Mitigation**: Pub/Sub is simpler than Kafka, quick to learn

**3. Feature Differences**
- No consumer offset management (Pub/Sub uses ack-based model)
- No log compaction (not needed for our use case)
- No exactly-once semantics (we use at-least-once anyway)
- **Mitigation**: Our use case doesn't require these features

### Cost Analysis

**Monthly Cost Estimate** (RAG system workload):

| Component | Usage | Cost |
|-----------|-------|------|
| Message Ingestion | 1M messages/month (~1 GB) | $0.06 |
| Message Delivery | 1M messages/month (~1 GB) | $0.09 |
| Message Storage | Avg 2 days retention | $0.54 |
| Snapshot Storage | None | $0.00 |
| **Total** | | **~$0.69/month** |

**With higher load (10M messages/month)**:
- Ingestion: $0.60
- Delivery: $0.90
- Storage: $5.40
- **Total: ~$6.90/month**

**Free tier**: 10 GB/month included (covers ~10M small messages)

### Implementation Effort

**Estimated Effort**: 10-13 story points

**Code Changes** (5-8 points):
- Update dependencies (Spring Cloud GCP)
- Create Pub/Sub adapter interfaces
- Implement producers with PubSubTemplate
- Implement consumers with @PubSubListener
- Update configuration files
- Integration testing

**Infrastructure** (3-5 points):
- Create Pub/Sub topics
- Create subscriptions
- Configure IAM permissions
- Set up monitoring

---

## Option B: Confluent Cloud (NOT RECOMMENDED ❌)

### Overview
Fully managed Apache Kafka service by Confluent, running on GCP infrastructure.

### Pros ✅

**1. Kafka Compatibility**
- **Zero code changes**: Drop-in replacement for self-hosted Kafka
- **Same APIs**: Keep existing Spring Kafka code
- **Kafka features**: All Kafka features available (Streams, Connect, etc.)

**2. Kafka Ecosystem**
- Access to Confluent tools (Schema Registry, ksqlDB)
- Kafka Streams support (if needed in future)
- Large Kafka community and resources

### Cons ❌

**1. High Cost** 💰
- **Basic cluster**: $150/month minimum (0.5 CKU)
- **Standard cluster**: $700+/month (1 CKU)
- **Dedicated cluster**: $2,000+/month (minimum)
- **Data transfer**: Additional charges for ingress/egress
- **Estimated cost**: $150-300/month for minimal setup

**2. Operational Complexity**
- Still need to manage cluster sizing
- Capacity planning required (CKUs)
- Manual scaling decisions
- More complex monitoring setup

**3. Limited GCP Integration**
- Third-party service (less integration than Pub/Sub)
- Separate IAM/authentication management
- Additional network configuration
- Separate monitoring/logging setup

**4. Overkill for Use Case**
- RAG system doesn't use advanced Kafka features
- No Kafka Streams
- No complex transactions
- Simple pub-sub patterns only

### Cost Analysis

**Monthly Cost Estimate** (Confluent Cloud Basic):

| Component | Cost |
|-----------|------|
| Basic Cluster (0.5 CKU) | $150 |
| Storage (10 GB) | $5 |
| Network Egress | $10-20 |
| **Total** | **$165-175/month** |

**With Standard cluster**: $700-1,000/month

### Implementation Effort

**Estimated Effort**: 3-5 story points

**Code Changes** (1-2 points):
- Update connection strings
- Configure authentication (API keys)
- Update CI/CD

**Infrastructure** (2-3 points):
- Provision Confluent Cloud cluster
- Create topics
- Set up monitoring integration

---

## Decision Matrix

| Criteria | Cloud Pub/Sub | Confluent Cloud | Weight |
|----------|---------------|-----------------|--------|
| **Cost** | ⭐⭐⭐⭐⭐ $0.69-7/mo | ⭐☆☆☆☆ $150-300/mo | 25% |
| **GCP Integration** | ⭐⭐⭐⭐⭐ Native | ⭐⭐☆☆☆ Third-party | 20% |
| **Operational Complexity** | ⭐⭐⭐⭐⭐ Serverless | ⭐⭐⭐☆☆ Managed clusters | 20% |
| **Implementation Effort** | ⭐⭐⭐☆☆ 10-13 points | ⭐⭐⭐⭐⭐ 3-5 points | 15% |
| **Feature Set** | ⭐⭐⭐⭐☆ Meets needs | ⭐⭐⭐⭐⭐ Full Kafka | 10% |
| **Developer Experience** | ⭐⭐⭐⭐☆ Simple APIs | ⭐⭐⭐⭐⭐ Familiar Kafka | 10% |

**Weighted Score**:
- **Cloud Pub/Sub**: 4.5/5 (90%)
- **Confluent Cloud**: 2.9/5 (58%)

---

## Recommendation

### ✅ RECOMMENDED: Cloud Pub/Sub

**Primary Reasons**:
1. **95% cost savings** ($7/month vs $175/month)
2. **Zero operational overhead** (serverless vs managed clusters)
3. **Better GCP integration** (Workload Identity, IAM, monitoring)
4. **Sufficient features** (meets all RAG system requirements)
5. **Simpler architecture** (no brokers, no Zookeeper, no capacity planning)

**Trade-off Accepted**:
- Code changes required (10-13 story points)
- Team learning curve (~1 week)

**Mitigation Strategy**:
- Use adapter pattern to minimize code changes
- Comprehensive testing during migration
- Phased rollout (one service at a time)
- Maintain Kafka in dev for parallel testing

---

## Migration Strategy

### Phase 1: Infrastructure Setup (2 points)
1. Enable Pub/Sub API
2. Create Pub/Sub topics and subscriptions
3. Configure IAM permissions (Workload Identity)
4. Set up monitoring dashboards

### Phase 2: Code Migration - Document Service (4 points)
1. Add Spring Cloud GCP dependencies
2. Create Pub/Sub producer adapter
3. Create Pub/Sub consumer adapter
4. Update configuration
5. Integration testing

### Phase 3: Code Migration - Embedding Service (3 points)
1. Migrate consumer code
2. Migrate dead letter queue to Pub/Sub
3. Integration testing

### Phase 4: Code Migration - Core Service (3 points)
1. Migrate producer/consumer code
2. Integration testing

### Phase 5: Testing & Validation (1 point)
1. E2E testing with Pub/Sub
2. Performance testing
3. Failover testing

---

## Implementation Plan

### Scripts to Create
1. `scripts/gcp/11-setup-pubsub.sh` - Create topics and subscriptions
2. `scripts/gcp/12-migrate-to-pubsub.sh` - Migration helper script

### Code Changes
1. Update `pom.xml` dependencies:
   ```xml
   <dependency>
       <groupId>com.google.cloud</groupId>
       <artifactId>spring-cloud-gcp-starter-pubsub</artifactId>
   </dependency>
   ```

2. Update `application.yml`:
   ```yaml
   spring:
     cloud:
       gcp:
         pubsub:
           project-id: ${GCP_PROJECT_ID}
   ```

3. Create adapter interfaces:
   - `MessageProducerAdapter` (KafkaTemplate → PubSubTemplate)
   - `MessageConsumerAdapter` (@KafkaListener → @PubSubListener)

### Testing Strategy
1. Unit tests for adapters
2. Integration tests with Pub/Sub emulator
3. E2E tests in GCP environment
4. Performance benchmarks
5. Parallel Kafka/Pub/Sub testing

---

## Rollback Plan

If Pub/Sub migration fails or issues arise:

1. **Keep Kafka configuration** in separate profile (`kafka` profile)
2. **Maintain both implementations** during migration
3. **Feature flags** to switch between Kafka and Pub/Sub
4. **Quick rollback**: Change profile and redeploy
5. **Monitoring**: Track migration success metrics

---

## Success Criteria

- ✅ All 5 topics migrated to Pub/Sub
- ✅ All producers and consumers functional
- ✅ Message ordering maintained where required
- ✅ Dead letter queue working
- ✅ Monitoring and alerting operational
- ✅ E2E tests passing
- ✅ Performance meets SLAs (<200ms p95 latency)
- ✅ Cost under $10/month
- ✅ Zero message loss during migration

---

## Timeline

**Total Effort**: 13 story points (~2-3 weeks for 1 developer)

| Phase | Duration | Story Points |
|-------|----------|--------------|
| Infrastructure Setup | 2 days | 2 |
| Document Service Migration | 4 days | 4 |
| Embedding Service Migration | 3 days | 3 |
| Core Service Migration | 3 days | 3 |
| Testing & Validation | 1 day | 1 |
| **Total** | **13 days** | **13** |

---

## References

- [Cloud Pub/Sub Documentation](https://cloud.google.com/pubsub/docs)
- [Spring Cloud GCP Pub/Sub](https://cloud.spring.io/spring-cloud-static/spring-cloud-gcp/current/reference/html/#spring-cloud-gcp-for-pub-sub)
- [Pub/Sub Pricing](https://cloud.google.com/pubsub/pricing)
- [Confluent Cloud Pricing](https://www.confluent.io/confluent-cloud/pricing/)
- [Migrating from Kafka to Pub/Sub](https://cloud.google.com/pubsub/docs/migrating-from-kafka)

---
version: 1.0.0
last-updated: 2025-11-12
status: active
applies-to: 0.8.0-SNAPSHOT
category: deployment
---

# GCP-REDIS-005: Cloud Memorystore Redis Setup

## Overview

Cloud Memorystore Redis instance configured for the RAG system with high availability and automatic failover.

**Status**: ✅ COMPLETE (2025-11-09)

## Instance Details

- **Instance Name**: `rag-redis`
- **Host (Private IP)**: `10.170.252.12`
- **Port**: `6379`
- **Region**: `us-central1`
- **Zone**: `us-central1-a`
- **Tier**: `STANDARD_HA` (High Availability with replication)
- **Memory**: 5 GB
- **Redis Version**: 7.0

## Architecture

### High Availability

- **Standard Tier**: Provides automatic failover and replication across zones
- **Master-Replica Setup**: Data replicated to a standby replica for redundancy
- **Automatic Failover**: GCP automatically promotes replica to master if primary fails
- **Zero Data Loss**: Synchronous replication ensures no data loss during failover

### Network Configuration

- **Private IP Only**: Instance accessible only within VPC network
- **VPC Network**: Connected to `default` VPC (or `rag-vpc` if configured)
- **No Public IP**: Enhanced security by preventing internet access
- **VPC Peering**: GKE pods can access Memorystore directly via private network

## Authentication

### Redis AUTH

- **Enabled**: Yes (AUTH required for all connections)
- **Password Storage**: Stored in Secret Manager (`memorystore-redis-password`)
- **Rotation**: Manual rotation supported via Secret Manager

### Connection Security

```bash
# Connect with authentication
redis-cli -h 10.170.252.12 -p 6379 -a "$(gcloud secrets versions access latest \
    --secret=memorystore-redis-password \
    --project=byo-rag-dev)"

# Test connectivity
redis-cli -h 10.170.252.12 -p 6379 -a "$REDIS_PASSWORD" PING
# Expected: PONG
```

## Service Configuration

### Spring Boot Application Properties

Update `application.yml` for each service:

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:10.170.252.12}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:from-secret-manager}
      database: 0  # Adjust per service (auth:0, core:1, embedding:2)
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 15
          max-idle: 10
          min-idle: 3
          max-wait: -1ms
```

### Database Separation

Each service uses a separate Redis database for isolation:

| Service | Database | Purpose |
|---------|----------|---------|
| Auth Service | 0 | JWT token cache, session data |
| Core Service | 1 | Query cache, rate limiting |
| Embedding Service | 2 | Vector embeddings cache |

### Environment Variables (Kubernetes)

```yaml
env:
  - name: REDIS_HOST
    value: "10.170.252.12"
  - name: REDIS_PORT
    value: "6379"
  - name: REDIS_PASSWORD
    valueFrom:
      secretKeyRef:
        name: redis-credentials
        key: password
```

## Secret Manager Integration

### Stored Secrets

```bash
# Redis AUTH password
gcloud secrets versions access latest \
    --secret=memorystore-redis-password \
    --project=byo-rag-dev

# Connection information (JSON)
gcloud secrets versions access latest \
    --secret=memorystore-connection-info \
    --project=byo-rag-dev
```

### Connection Info JSON Structure

```json
{
  "host": "10.170.252.12",
  "port": 6379,
  "region": "us-central1",
  "instance_name": "rag-redis",
  "zone": "us-central1-a",
  "memory_gb": 5,
  "redis_version": "REDIS_7_0",
  "tier": "STANDARD_HA"
}
```

### IAM Permissions

GKE service account granted access:
- **Service Account**: `gke-node-sa@byo-rag-dev.iam.gserviceaccount.com`
- **Role**: `roles/secretmanager.secretAccessor`
- **Secrets**: `memorystore-redis-password`, `memorystore-connection-info`

## Use Cases by Service

### Auth Service
- **JWT Token Blacklist**: Store revoked tokens until expiration
- **Session Management**: Cache user session data
- **Rate Limiting**: Track API request counts per user/tenant
- **Tenant Cache**: Cache tenant metadata for fast lookups

### Core Service
- **Query Cache**: Cache RAG query results for repeated questions
- **Rate Limiting**: Track query rates per tenant
- **Feature Flags**: Store dynamic configuration
- **Circuit Breaker State**: Track service health status

### Embedding Service
- **Vector Cache**: Cache computed embeddings to avoid recomputation
- **Model Metadata**: Store embedding model versions and dimensions
- **Batch Processing State**: Track batch job progress

## Performance Characteristics

### Latency
- **Typical Latency**: <2ms for GET/SET operations (within same region)
- **Network Latency**: ~1-2ms from GKE pods in us-central1
- **High Availability Impact**: Minimal latency overhead (~0.5ms) for replication

### Throughput
- **Operations/Second**: ~80,000 ops/sec (5GB Standard tier)
- **Concurrent Connections**: Supports thousands of concurrent connections
- **Connection Pool Recommended**: Use Lettuce connection pooling

### Memory Management
- **Eviction Policy**: `noeviction` (default - no automatic eviction)
- **Memory Limit**: 5 GB total
- **Monitoring**: Set alerts at 70% and 85% memory utilization

## Maintenance

### Automated Maintenance

- **Maintenance Window**: Sunday, 04:00-05:00 UTC
- **Frequency**: Monthly security patches and updates
- **Downtime**: <30 seconds due to automatic failover
- **Notification**: No user action required

### Manual Maintenance

```bash
# View instance details
gcloud redis instances describe rag-redis \
    --region=us-central1 \
    --project=byo-rag-dev

# Check current memory usage
redis-cli -h 10.170.252.12 -p 6379 -a "$REDIS_PASSWORD" INFO memory

# Monitor performance metrics
gcloud monitoring time-series list \
    --filter='resource.type="redis.googleapis.com/Instance"' \
    --project=byo-rag-dev
```

## Monitoring and Alerts

### Key Metrics to Monitor

1. **Memory Usage**
   - Alert at 70% (warning)
   - Alert at 85% (critical)
   - Action: Scale up or implement eviction policy

2. **CPU Utilization**
   - Alert at 80%
   - Action: Consider scaling or optimizing queries

3. **Connections**
   - Monitor connection count
   - Alert on connection spikes
   - Action: Review connection pooling

4. **Operations/Second**
   - Track throughput trends
   - Alert on unusual spikes
   - Action: Investigate application behavior

5. **Replication Lag** (Standard tier)
   - Alert if lag >5 seconds
   - Action: Check network or instance health

### Setting Up Alerts

```bash
# Create memory usage alert (via gcloud or console)
gcloud alpha monitoring policies create \
    --notification-channels=CHANNEL_ID \
    --display-name="Redis Memory Usage High" \
    --condition-display-name="Memory > 70%" \
    --condition-threshold-value=0.70 \
    --condition-threshold-duration=300s \
    --condition-filter='resource.type="redis.googleapis.com/Instance" AND metric.type="redis.googleapis.com/stats/memory/usage_ratio"'
```

## Cost Management

### Current Cost Estimate

- **Standard Tier (5GB)**: ~$230-250/month
- **Network Egress**: Minimal (same region)
- **Backup/Snapshots**: Not applicable (managed by GCP)

### Cost Optimization

1. **Right-sizing**: Monitor actual memory usage
   - If consistently <50% used, consider scaling down
   - If consistently >70% used, consider scaling up

2. **Committed Use Discounts**: 
   - 1-year commitment: ~15% discount
   - 3-year commitment: ~30% discount

3. **Regional Selection**:
   - us-central1 is cost-effective for US workloads
   - Consider multi-region for global deployments

### Scaling

```bash
# Scale memory up to 10GB
gcloud redis instances update rag-redis \
    --size=10 \
    --region=us-central1 \
    --project=byo-rag-dev

# Scale memory down to 3GB
gcloud redis instances update rag-redis \
    --size=3 \
    --region=us-central1 \
    --project=byo-rag-dev
```

**Note**: Scaling causes brief downtime (~30 seconds with HA tier)

## GKE Integration

### Network Connectivity

1. **VPC Peering**: Memorystore automatically peers with VPC
2. **Private IP**: GKE pods access Redis via private IP (10.170.252.12)
3. **No Cloud SQL Proxy**: Direct connection (unlike Cloud SQL)

### Kubernetes ConfigMap

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: redis-config
  namespace: rag-system
data:
  REDIS_HOST: "10.170.252.12"
  REDIS_PORT: "6379"
```

### Kubernetes Secret

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: redis-credentials
  namespace: rag-system
type: Opaque
stringData:
  password: "$(gcloud secrets versions access latest --secret=memorystore-redis-password)"
```

### Pod Configuration Example

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: rag-auth-service
spec:
  containers:
  - name: auth-service
    image: us-central1-docker.pkg.dev/byo-rag-dev/rag-system/rag-auth-service:0.8.0
    env:
    - name: REDIS_HOST
      valueFrom:
        configMapKeyRef:
          name: redis-config
          key: REDIS_HOST
    - name: REDIS_PORT
      valueFrom:
        configMapKeyRef:
          name: redis-config
          key: REDIS_PORT
    - name: REDIS_PASSWORD
      valueFrom:
        secretKeyRef:
          name: redis-credentials
          key: password
```

## Troubleshooting

### Connection Issues

```bash
# 1. Verify instance is running
gcloud redis instances describe rag-redis \
    --region=us-central1 \
    --project=byo-rag-dev \
    --format="value(state)"
# Expected: READY

# 2. Test connectivity from GKE pod
kubectl run redis-test --rm -it --image=redis:7.0 -- sh
redis-cli -h 10.170.252.12 -p 6379 -a "$REDIS_PASSWORD" PING

# 3. Check VPC network configuration
gcloud compute networks describe default --project=byo-rag-dev

# 4. Verify firewall rules allow Redis port (6379)
gcloud compute firewall-rules list \
    --filter="name~redis" \
    --project=byo-rag-dev
```

### Authentication Issues

```bash
# Verify AUTH is enabled
redis-cli -h 10.170.252.12 -p 6379 PING
# Should return: (error) NOAUTH Authentication required

# Test with password
redis-cli -h 10.170.252.12 -p 6379 -a "$REDIS_PASSWORD" PING
# Should return: PONG

# Rotate password if compromised
# (Manual process via GCP Console or update Secret Manager)
```

### Performance Issues

```bash
# Check memory usage
redis-cli -h 10.170.252.12 -p 6379 -a "$REDIS_PASSWORD" INFO memory

# Check slow log for slow commands
redis-cli -h 10.170.252.12 -p 6379 -a "$REDIS_PASSWORD" SLOWLOG GET 10

# Monitor active connections
redis-cli -h 10.170.252.12 -p 6379 -a "$REDIS_PASSWORD" CLIENT LIST

# Check hit/miss ratio
redis-cli -h 10.170.252.12 -p 6379 -a "$REDIS_PASSWORD" INFO stats
```

### Common Errors

| Error | Cause | Solution |
|-------|-------|----------|
| `Connection refused` | Network/VPC issue | Verify VPC peering and firewall rules |
| `NOAUTH Authentication required` | Missing password | Provide AUTH password from Secret Manager |
| `OOM command not allowed` | Memory full | Scale up instance or enable eviction |
| `Connection timeout` | Network latency | Check network configuration, increase timeout |
| `Too many connections` | Connection leak | Implement connection pooling properly |

## Migration from Docker Redis

### Data Migration (if needed)

```bash
# 1. Export data from local Redis
docker exec rag-redis redis-cli --rdb /data/dump.rdb SAVE

# 2. Copy RDB file from container
docker cp rag-redis:/data/dump.rdb /tmp/redis-backup.rdb

# 3. Import to Cloud Memorystore (requires import feature)
# Note: Direct import not supported, consider application-level migration
```

### Configuration Migration

```diff
# docker-compose.yml
- REDIS_HOST=redis
+ REDIS_HOST=10.170.252.12

- REDIS_PASSWORD=redis_password
+ REDIS_PASSWORD=${REDIS_PASSWORD}  # From Secret Manager
```

### Testing Strategy

1. **Parallel Run**: Run both local and Memorystore simultaneously
2. **Gradual Migration**: Migrate one service at a time
3. **Monitoring**: Watch for errors and latency changes
4. **Rollback Plan**: Keep local Redis available for 48 hours

## Management Console

- **Instance Overview**: https://console.cloud.google.com/memorystore/redis/instances/rag-redis/details?project=byo-rag-dev
- **Monitoring Dashboard**: https://console.cloud.google.com/monitoring/dashboards?project=byo-rag-dev
- **Logs Viewer**: https://console.cloud.google.com/logs?project=byo-rag-dev

## Scripts

Setup script located in `scripts/gcp/`:

- **10-setup-memorystore.sh** - Creates instance, configures AUTH, stores secrets

## Next Steps

1. ✅ Cloud Memorystore instance created and configured
2. ✅ Redis AUTH enabled and password stored
3. ✅ IAM permissions configured for GKE access
4. ⏳ Update service configurations to use Memorystore
5. ⏳ Create Kubernetes ConfigMap and Secret (GCP-K8S-008)
6. ⏳ Test Redis connectivity from GKE pods
7. ⏳ Set up monitoring alerts for memory and performance
8. ⏳ Implement connection pooling in all services
9. ⏳ Load test to validate throughput requirements

## References

- [Cloud Memorystore for Redis Documentation](https://cloud.google.com/memorystore/docs/redis)
- [Redis 7.0 Release Notes](https://redis.io/docs/latest/operate/oss_and_stack/releases/redis-7-0/)
- [High Availability Configuration](https://cloud.google.com/memorystore/docs/redis/high-availability)
- [Best Practices](https://cloud.google.com/memorystore/docs/redis/best-practices)
- [Spring Data Redis Documentation](https://docs.spring.io/spring-data/redis/reference/)

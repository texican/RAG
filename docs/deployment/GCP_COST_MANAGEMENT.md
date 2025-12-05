---
version: 1.0.0
last-updated: 2025-11-28
status: active
applies-to: 0.8.0-SNAPSHOT
category: deployment
---

# GCP Cost Management Guide

**Last Updated:** 2025-11-28
**Status:** Active
**Purpose:** Minimize GCP costs when resources are not in use

## Quick Reference

| Action | Monthly Cost | Use Case |
|--------|-------------|----------|
| **Stop GKE Only** | ~$313/month | Short breaks (hours/days) |
| **Delete Everything** | ~$6/month | Long breaks (weeks/months) |
| **Running Everything** | ~$470/month | Active development/production |

## Cost Breakdown by Resource

### Always Running (Cannot Stop)
- **Cloud SQL PostgreSQL**: ~$77/month
- **Cloud Memorystore Redis**: ~$230/month
- **Persistent Disk Storage**: ~$2/month
- **Cloud Storage**: ~$5/month
- **Artifact Registry**: ~$1/month

### Can Stop/Scale Down
- **GKE Cluster**: ~$155/month
  - Control plane: Free (standard tier)
  - Nodes: ~$155/month (varies with node count)

## Three Cost Management Strategies

### Strategy 1: Stop GKE Cluster (33% Savings)

**When to Use:**
- Taking a break for a few hours or days
- Want to resume quickly
- Don't mind ~$313/month for databases

**Savings:** ~$157/month (33% reduction)

**Command:**
```bash
./scripts/gcp/20-stop-all-resources.sh
```

**What This Does:**
- ✅ Scales GKE node pools to 0 nodes
- ✅ Keeps databases running (Cloud SQL + Redis)
- ✅ Preserves all data
- ✅ Quick restart (30 seconds)

**Cost After Stopping:**
- GKE: $0/month
- Cloud SQL: ~$77/month (still running)
- Redis: ~$230/month (still running)
- Storage: ~$6/month
- **Total: ~$313/month**

**To Restart:**
```bash
./scripts/gcp/22-start-all-resources.sh
```

---

### Strategy 2: Delete Everything (99% Savings)

**When to Use:**
- Extended break (weeks or months)
- Don't need the infrastructure
- Want minimal costs (~$6/month)

**Savings:** ~$464/month (99% reduction)

**Command:**
```bash
./scripts/gcp/21-delete-all-resources.sh
```

**What This Does:**
- ⚠️  Deletes GKE cluster
- ⚠️  Deletes Cloud SQL (ALL DATA LOST)
- ⚠️  Deletes Redis (ALL DATA LOST)
- ⚠️  Deletes persistent disks
- ⚠️  Optionally deletes buckets and images

**Cost After Deletion:**
- VPC Network: Free
- Service Accounts: Free
- Secret Manager: ~$0.06/month
- Cloud Storage (if kept): ~$5/month
- Container Images (if kept): ~$1/month
- **Total: ~$6/month**

**To Recreate:**
```bash
# Full infrastructure setup (~2 hours)
make gcp-setup ENV=dev
make gcp-deploy-all ENV=dev
```

---

### Strategy 3: Optimize Running Costs

**When to Use:**
- Active development
- Need resources available 24/7
- Want to minimize ongoing costs

**Optimizations:**

#### 1. Use Smaller Machine Types (Development)
Current dev cluster uses `e2-standard-4` (4 vCPU, 16 GB RAM).

Downgrade to `e2-medium` for development:
```bash
# Reduce node pool machine type
gcloud container node-pools update workload-pool \
  --cluster=rag-gke-dev \
  --region=us-central1 \
  --machine-type=e2-medium
```

**Savings:** ~$50-70/month

#### 2. Reduce Minimum Node Count
```bash
# Scale down to 1 minimum node
gcloud container node-pools update workload-pool \
  --cluster=rag-gke-dev \
  --region=us-central1 \
  --min-nodes=1
```

**Savings:** ~$25-35/month

#### 3. Use Spot/Preemptible VMs (Production)
```bash
# Create preemptible node pool
gcloud container node-pools create preemptible-pool \
  --cluster=rag-gke-dev \
  --region=us-central1 \
  --machine-type=e2-standard-4 \
  --preemptible \
  --num-nodes=2
```

**Savings:** ~60-80% on compute costs

#### 4. Stop During Nights/Weekends
Use Cloud Scheduler to stop/start GKE automatically:

**Cron Schedule:**
- Stop: Every weekday at 6 PM PST (`0 18 * * 1-5`)
- Start: Every weekday at 8 AM PST (`0 8 * * 1-5`)

**Monthly Savings:** ~$70/month (assuming 60 hours/week stopped)

---

## Detailed Cost Analysis

### Development Environment (~$470/month)

| Resource | Monthly Cost | Notes |
|----------|-------------|-------|
| **GKE Cluster** | ~$155 | 3 nodes (2 workload, 1 system) |
| Cloud SQL PostgreSQL | ~$77 | db-custom-2-7680 (2 vCPU, 7.5 GB) |
| Cloud Memorystore Redis | ~$230 | Standard HA, 5 GB |
| Persistent Disk (100 GB SSD) | ~$17 | Regional SSD |
| Cloud Storage | ~$5 | Buckets (docs, backups, snapshots) |
| Artifact Registry | ~$1 | Container images |
| Load Balancer | ~$18 | Ingress + static IP |
| Network Egress | ~$5-20 | Varies with usage |
| **Total** | **~$508/month** | Full dev environment |

### After Stopping GKE (~$313/month)

| Resource | Monthly Cost | Notes |
|----------|-------------|-------|
| **GKE Cluster** | **$0** | 0 nodes |
| Cloud SQL PostgreSQL | ~$77 | Cannot stop |
| Cloud Memorystore Redis | ~$230 | Cannot stop |
| Persistent Disk (detached) | ~$2 | Minimal cost when not attached |
| Cloud Storage | ~$5 | Pay for storage only |
| Artifact Registry | ~$1 | Pay for storage only |
| **Total** | **~$315/month** | 38% reduction |

### After Deleting Everything (~$6/month)

| Resource | Monthly Cost | Notes |
|----------|-------------|-------|
| VPC Network | Free | No charge |
| Service Accounts | Free | No charge |
| Secret Manager | ~$0.06 | 4 secrets @ $0.06/secret/month |
| Cloud Storage (optional) | ~$5 | If you keep buckets |
| Container Images (optional) | ~$1 | If you keep images |
| **Total** | **~$6/month** | 99% reduction |

---

## Cost Monitoring Setup

### 1. Set Up Budget Alerts

```bash
gcloud billing budgets create \
  --billing-account=YOUR_BILLING_ACCOUNT_ID \
  --display-name="RAG Dev Environment Monthly Budget" \
  --budget-amount=500 \
  --threshold-rule=percent=50 \
  --threshold-rule=percent=90 \
  --threshold-rule=percent=100
```

### 2. Enable Cost Breakdown

View costs by service:
```bash
gcloud billing accounts describe YOUR_BILLING_ACCOUNT_ID
```

### 3. Use Cloud Console Cost Reports

https://console.cloud.google.com/billing/reports

**Recommended Reports:**
- By Service (see which services cost most)
- By Project (isolate RAG costs)
- By SKU (detailed billing breakdown)
- Forecast (predict next month's costs)

---

## Best Practices

### For Development

✅ **DO:**
- Stop GKE cluster when not actively developing
- Use `e2-medium` or `e2-small` for dev workloads
- Delete test data regularly to reduce storage costs
- Use Cloud Build free tier (120 build-minutes/day)

❌ **DON'T:**
- Leave resources running overnight/weekends
- Use production-sized instances for development
- Keep old container images indefinitely
- Forget to delete failed experiments

### For Production

✅ **DO:**
- Use committed use discounts (30-70% savings)
- Enable cluster autoscaling
- Use preemptible VMs for batch workloads
- Monitor costs weekly

❌ **DON'T:**
- Over-provision resources
- Use oversized machine types
- Ignore cost alerts
- Run dev and prod in same project (hard to track costs)

---

## Common Cost Pitfalls

### 1. Idle Load Balancers (~$18/month)
**Problem:** Ingress load balancer costs ~$18/month even with no traffic.

**Solution:**
```bash
# Delete ingress when not needed
kubectl delete ingress rag-ingress -n rag-system
```

### 2. Persistent Disk Snapshots
**Problem:** Snapshots accumulate ($0.026/GB/month).

**Solution:**
```bash
# Delete old snapshots (automated in script)
./scripts/gcp/15-manage-snapshots.sh cleanup 30
```

### 3. Cloud SQL High Availability
**Problem:** HA doubles Cloud SQL costs.

**Solution (dev only):**
```bash
# Switch to zonal (non-HA) for development
gcloud sql instances patch rag-postgres \
  --availability-type=ZONAL
```

**Savings:** ~$40/month

### 4. Unused Container Images
**Problem:** Artifact Registry charges for storage.

**Solution:**
```bash
# Delete old images
gcloud artifacts docker images delete \
  us-central1-docker.pkg.dev/byo-rag-dev/rag-system/rag-core-service:old-tag
```

---

## FAQ

### Q: Can I stop Cloud SQL to save costs?
**A:** No, Cloud SQL cannot be stopped. You can only delete it. For development, consider:
- Using Cloud SQL Proxy with a local PostgreSQL instance
- Deleting Cloud SQL and using docker-compose locally
- Accepting the ~$77/month cost

### Q: Can I stop Redis to save costs?
**A:** No, Memorystore Redis cannot be stopped. Alternatives:
- Use local Redis via docker-compose
- Delete Redis instance when not needed
- Use smaller Redis instance (1 GB minimum: ~$45/month)

### Q: How long does it take to start/stop resources?
- **Stop GKE:** ~2 minutes
- **Start GKE:** ~30 seconds
- **Delete Cloud SQL:** ~5 minutes
- **Create Cloud SQL:** ~10 minutes
- **Full Infrastructure Setup:** ~2 hours

### Q: Will I lose data when stopping GKE?
**A:** No. GKE node scale-down only removes compute. Your data in:
- Cloud SQL: Preserved ✅
- Redis: Preserved ✅
- Persistent Disks: Preserved ✅
- Cloud Storage: Preserved ✅

### Q: Will I lose data when deleting everything?
**A:** Yes. All data in Cloud SQL, Redis, and persistent disks is permanently deleted. Cloud Storage buckets can optionally be preserved.

### Q: What's the fastest way to reduce costs to $0?
```bash
# Nuclear option - deletes EVERYTHING
./scripts/gcp/21-delete-all-resources.sh

# Answer "yes" to delete buckets and images too
```

**Cost after deletion:** ~$0-6/month (only if you keep buckets/images)

---

## Scripts Reference

### Created Scripts

| Script | Purpose | Cost Impact |
|--------|---------|-------------|
| `20-stop-all-resources.sh` | Stop GKE cluster | -$157/month (33%) |
| `21-delete-all-resources.sh` | Delete everything | -$464/month (99%) |
| `22-start-all-resources.sh` | Restart GKE cluster | +$157/month |

### Usage Examples

**Daily Development Workflow:**
```bash
# Morning: Start cluster
./scripts/gcp/22-start-all-resources.sh

# Evening: Stop cluster
./scripts/gcp/20-stop-all-resources.sh
```

**Weekend Shutdown:**
```bash
# Friday evening
./scripts/gcp/20-stop-all-resources.sh

# Monday morning
./scripts/gcp/22-start-all-resources.sh
```

**Extended Break (2+ weeks):**
```bash
# Before break
./scripts/gcp/21-delete-all-resources.sh

# After break
make gcp-setup ENV=dev
make gcp-deploy-all ENV=dev
```

---

## Cost Optimization Checklist

### Before You Start
- [ ] Set up billing alerts at 50%, 90%, 100%
- [ ] Enable detailed billing export
- [ ] Review GCP Free Tier limits
- [ ] Consider committed use discounts for production

### Daily/Weekly
- [ ] Stop GKE cluster when not in use
- [ ] Delete test data and documents
- [ ] Review Cloud Console cost report
- [ ] Check for unexpected charges

### Monthly
- [ ] Delete old snapshots (>30 days)
- [ ] Clean up unused container images
- [ ] Review node pool sizes
- [ ] Optimize Cloud SQL tier if needed

### Quarterly
- [ ] Review committed use discounts
- [ ] Evaluate preemptible VM usage
- [ ] Consider reserved capacity
- [ ] Audit unused resources

---

## Emergency Cost Reduction

**If costs are too high and you need immediate action:**

```bash
# 1. Stop GKE immediately (saves ~$157/month)
./scripts/gcp/20-stop-all-resources.sh

# 2. Consider downgrading Cloud SQL tier
gcloud sql instances patch rag-postgres \
  --tier=db-f1-micro  # ~$7/month

# 3. Consider smaller Redis instance
gcloud redis instances update rag-redis \
  --size=1 \
  --region=us-central1  # ~$45/month

# 4. Delete load balancer if not needed
kubectl delete ingress rag-ingress -n rag-system  # ~$18/month
```

**Maximum Monthly Savings:** ~$300-350/month

---

## Support

For cost-related questions:
- GCP Billing Support: https://console.cloud.google.com/billing
- Cost Calculator: https://cloud.google.com/products/calculator
- Pricing Documentation: https://cloud.google.com/pricing

**Internal Resources:**
- [GKE Cluster Setup](GKE_CLUSTER_SETUP.md)
- [Cloud SQL Setup](CLOUD_SQL_SETUP.md)
- [Cloud Memorystore Setup](CLOUD_MEMORYSTORE_SETUP.md)

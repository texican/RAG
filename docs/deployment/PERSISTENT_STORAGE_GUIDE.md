---
version: 1.0.0
last-updated: 2025-11-12
status: active
applies-to: 0.8.0-SNAPSHOT
category: deployment
---

# Persistent Storage Guide for RAG System on GCP

## Overview

This guide covers persistent storage configuration for the RAG system deployed on Google Kubernetes Engine (GKE), including:

- **Persistent Volumes**: GKE persistent disks for document storage
- **Cloud Storage**: GCS buckets for long-term storage and backups
- **Volume Snapshots**: Automated backup and restore procedures
- **Storage Classes**: Different storage types for various use cases
- **Cost Optimization**: Storage tier selection and lifecycle policies

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Persistent Volumes (PVs)](#persistent-volumes)
3. [Cloud Storage Buckets](#cloud-storage-buckets)
4. [Volume Snapshots](#volume-snapshots)
5. [Backup and Restore Procedures](#backup-and-restore)
6. [Volume Expansion](#volume-expansion)
7. [Monitoring and Alerts](#monitoring)
8. [Cost Optimization](#cost-optimization)
9. [Troubleshooting](#troubleshooting)

---

## Architecture Overview

### Storage Tiers

```
┌─────────────────────────────────────────────────────────────────┐
│                         RAG Storage Architecture                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌────────────────────┐    ┌────────────────────┐               │
│  │  Document Service  │    │  Persistent Volume  │               │
│  │    (Pod)           │───▶│    (pd-balanced)    │               │
│  │                    │    │     100Gi SSD       │               │
│  └────────────────────┘    └──────────┬──────────┘               │
│                                       │                          │
│                            ┌──────────▼──────────┐               │
│                            │  VolumeSnapshot     │               │
│                            │  (Daily @ 2 AM)     │               │
│                            │  Retention: 7-30d   │               │
│                            └──────────┬──────────┘               │
│                                       │                          │
│  ┌────────────────────────────────────▼─────────────────────┐   │
│  │              Cloud Storage Buckets                        │   │
│  ├───────────────────────────────────────────────────────────┤   │
│  │  • Documents: Active file storage                         │   │
│  │  • Backups: Long-term archives (90d retention)            │   │
│  │  • Snapshots: Exported volume snapshots (30d)             │   │
│  │  • Exports: Data exports and temp files (30d)             │   │
│  └───────────────────────────────────────────────────────────┘   │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

### Storage Types

| Storage Type | Use Case | Performance | Cost | Availability |
|--------------|----------|-------------|------|--------------|
| **pd-balanced** | Active document storage | Balanced IOPS | Medium | Regional |
| **pd-ssd** | High-performance workloads | High IOPS | High | Zonal/Regional |
| **pd-standard** | Archives, cold storage | Low IOPS | Low | Zonal |
| **Cloud Storage** | Long-term backups | N/A | Very Low | Regional/Multi-regional |

---

## Persistent Volumes

### StorageClasses

Three StorageClasses are provided:

#### 1. Regional SSD (Production)

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: rag-regional-ssd
provisioner: pd.csi.storage.gke.io
parameters:
  type: pd-balanced
  replication-type: regional-pd
  fstype: ext4
volumeBindingMode: WaitForFirstConsumer
allowVolumeExpansion: true
reclaimPolicy: Retain
```

**Features:**
- Replicated across two zones in the same region
- Automatic failover for high availability
- Suitable for production workloads
- **Cost**: ~$0.10/GB/month

#### 2. Zonal SSD (Development)

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: rag-zonal-ssd
provisioner: pd.csi.storage.gke.io
parameters:
  type: pd-ssd
  replication-type: none
  fstype: ext4
volumeBindingMode: WaitForFirstConsumer
allowVolumeExpansion: true
reclaimPolicy: Delete
```

**Features:**
- Single zone storage
- High performance
- Lower cost for non-critical workloads
- **Cost**: ~$0.17/GB/month

#### 3. Standard HDD (Archives)

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: rag-standard
provisioner: pd.csi.storage.gke.io
parameters:
  type: pd-standard
  replication-type: none
  fstype: ext4
volumeBindingMode: WaitForFirstConsumer
allowVolumeExpansion: true
reclaimPolicy: Retain
```

**Features:**
- HDD-backed storage
- Lower cost
- Suitable for archival data
- **Cost**: ~$0.04/GB/month

### Applying StorageClasses

```bash
# Deploy StorageClasses
kubectl apply -f k8s/base/storageclass.yaml

# Verify
kubectl get storageclasses
```

---

## Cloud Storage Buckets

### Bucket Types

Four Cloud Storage buckets are provisioned per environment:

#### 1. Documents Bucket

**Purpose**: Primary storage for uploaded documents and processed files

**Configuration**:
- Location: Regional (us-central1)
- Storage Class: Standard
- Versioning: Disabled
- Lifecycle: No automatic deletion
- Access: gke-node-sa (objectAdmin)

**Usage**:
```bash
# Upload document
gsutil cp document.pdf gs://PROJECT_ID-rag-documents-ENV/

# List documents
gsutil ls gs://PROJECT_ID-rag-documents-ENV/
```

#### 2. Backups Bucket

**Purpose**: Long-term backups and archives

**Configuration**:
- Location: Regional
- Storage Class: Standard
- Versioning: Enabled
- Lifecycle: Delete after 90 days
- Access: backup-sa (objectAdmin)

#### 3. Snapshots Bucket

**Purpose**: Exported volume snapshots

**Configuration**:
- Location: Regional
- Storage Class: Standard
- Versioning: Enabled
- Lifecycle: Delete after 30 days
- Access: backup-sa (objectAdmin)

#### 4. Exports Bucket

**Purpose**: Data exports and temporary files

**Configuration**:
- Location: Regional
- Storage Class: Standard
- Versioning: Disabled
- Lifecycle: Delete after 30 days
- Access: gke-node-sa (objectAdmin)

### Provisioning Buckets

```bash
# Development environment
./scripts/gcp/14-setup-storage.sh --env dev

# Production environment
./scripts/gcp/14-setup-storage.sh --env prod --project byo-rag-prod
```

---

## Volume Snapshots

### Automated Snapshots

VolumeSnapshots are created automatically via CronJob:

**Schedule**: Daily at 2:00 AM UTC

**Configuration**:
```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: snapshot-document-storage
spec:
  schedule: "0 2 * * *"  # Daily at 2 AM
```

**Retention**:
- Development: 7 days
- Production: 30 days

### Deploying Snapshot Configuration

```bash
# Deploy VolumeSnapshotClass and CronJob
kubectl apply -f k8s/base/volumesnapshot.yaml

# Verify CronJob
kubectl get cronjobs -n rag-system

# Check recent snapshots
kubectl get volumesnapshots -n rag-system
```

### Manual Snapshot Operations

Use the snapshot management script:

```bash
# Create manual snapshot
./scripts/gcp/15-manage-snapshots.sh create my-backup "Before upgrade"

# List all snapshots
./scripts/gcp/15-manage-snapshots.sh list

# View snapshot details
./scripts/gcp/15-manage-snapshots.sh details document-storage-20250109-120000

# Delete old snapshots (older than 30 days)
./scripts/gcp/15-manage-snapshots.sh cleanup 30
```

---

## Backup and Restore

### Creating Backups

#### Option 1: Volume Snapshot

```bash
# Create snapshot
./scripts/gcp/15-manage-snapshots.sh create pre-upgrade-backup "Before v2.0 upgrade"

# Wait for snapshot to be ready
kubectl wait --for=jsonpath='{.status.readyToUse}'=true \
  volumesnapshot/pre-upgrade-backup \
  -n rag-system \
  --timeout=600s
```

#### Option 2: Export to Cloud Storage

```bash
# Export snapshot to GCS
./scripts/gcp/15-manage-snapshots.sh export \
  document-storage-20250109-120000 \
  byo-rag-dev-rag-snapshots-dev

# Verify export
gsutil ls gs://byo-rag-dev-rag-snapshots-dev/
```

### Restoring from Backup

#### Restore from VolumeSnapshot

```bash
# Restore to new PVC
./scripts/gcp/15-manage-snapshots.sh restore \
  document-storage-20250109-120000 \
  document-storage-restored

# Update deployment to use restored PVC
kubectl set volume deployment/rag-document \
  -n rag-system \
  --add --name=document-storage \
  --type=persistentVolumeClaim \
  --claim-name=document-storage-restored \
  --mount-path=/app/storage

# Restart pods to pick up new volume
kubectl rollout restart deployment/rag-document -n rag-system
```

#### Restore from Cloud Storage Export

```bash
# Import snapshot from GCS
# (Requires creating a new disk from the exported image)

# 1. Create disk from image
gcloud compute disks create document-storage-restored \
  --source-image=gs://BUCKET/snapshot.vmdk \
  --size=100GB \
  --type=pd-balanced \
  --zone=us-central1-a

# 2. Create PV and PVC manually pointing to disk
# See Kubernetes documentation for importing pre-existing disks
```

---

## Volume Expansion

### Online Expansion (Recommended)

Expand volumes without downtime:

```bash
# 1. Edit PVC to increase size
kubectl patch pvc document-storage-pvc -n rag-system \
  -p '{"spec":{"resources":{"requests":{"storage":"200Gi"}}}}'

# 2. Wait for resize to complete
kubectl get pvc document-storage-pvc -n rag-system -w

# 3. Verify new size
kubectl exec -it deployment/rag-document -n rag-system -- df -h /app/storage
```

**Notes**:
- StorageClass must have `allowVolumeExpansion: true`
- Can only increase size, not decrease
- Filesystem expansion happens automatically
- No pod restart required

### Offline Expansion

If online expansion is not supported:

```bash
# 1. Scale down deployment
kubectl scale deployment/rag-document -n rag-system --replicas=0

# 2. Edit PVC size
kubectl patch pvc document-storage-pvc -n rag-system \
  -p '{"spec":{"resources":{"requests":{"storage":"200Gi"}}}}'

# 3. Wait for resize
kubectl get pvc document-storage-pvc -n rag-system -w

# 4. Scale up deployment
kubectl scale deployment/rag-document -n rag-system --replicas=2
```

---

## Monitoring

### Storage Metrics

Monitor storage usage with these commands:

```bash
# PVC usage
kubectl get pvc -n rag-system

# Check disk usage inside pod
kubectl exec -it deployment/rag-document -n rag-system -- df -h /app/storage

# Snapshot status
kubectl get volumesnapshots -n rag-system

# CronJob history
kubectl get jobs -n rag-system -l app=snapshot-job
```

### Cloud Monitoring Alerts

Create alerts for:

1. **PVC Utilization > 80%**
2. **Snapshot failures**
3. **Cloud Storage bucket size > 500GB**
4. **Failed backup CronJobs**

```bash
# View Cloud Logging for snapshot jobs
gcloud logging read "resource.type=k8s_cluster AND resource.labels.cluster_name=rag-gke-dev AND labels.k8s-pod/app=snapshot-job" --limit 50
```

---

## Cost Optimization

### Storage Costs

#### Persistent Disks

| Disk Type | Size | Monthly Cost |
|-----------|------|--------------|
| pd-balanced (regional) | 100GB | ~$10 |
| pd-ssd (zonal) | 100GB | ~$17 |
| pd-standard (zonal) | 100GB | ~$4 |

#### Cloud Storage

| Storage Type | Operations | Monthly Cost |
|--------------|------------|--------------|
| Standard Storage (100GB) | ~$2 |
| Operations (10k/month) | ~$0.05 |

#### Snapshots

- Incremental only: ~$0.026/GB/month
- First snapshot: Full disk size
- Subsequent: Only changed blocks

### Cost Reduction Strategies

1. **Use appropriate StorageClass**:
   - Development: `rag-zonal-ssd` or `rag-standard`
   - Production: `rag-regional-ssd`

2. **Implement lifecycle policies**:
   - Automatically delete old snapshots (7-30 days)
   - Move cold data to Archive Storage Class

3. **Right-size volumes**:
   - Monitor actual usage
   - Expand only when needed
   - Consider using Cloud Storage for archives

4. **Optimize snapshot frequency**:
   - Daily for production
   - Weekly for development
   - On-demand for testing

---

## Troubleshooting

### PVC Not Binding

**Symptoms**: PVC stuck in `Pending` state

**Causes**:
- No available nodes in the zone
- Insufficient quota
- Invalid StorageClass

**Solutions**:
```bash
# Check PVC events
kubectl describe pvc document-storage-pvc -n rag-system

# Check StorageClass
kubectl get storageclass

# Check node capacity
kubectl describe nodes | grep -A5 "Allocated resources"
```

### Snapshot Failed

**Symptoms**: VolumeSnapshot not reaching `readyToUse: true`

**Causes**:
- Snapshot already in progress
- Insufficient permissions
- Volume not attached to node

**Solutions**:
```bash
# Check snapshot status
kubectl describe volumesnapshot SNAPSHOT_NAME -n rag-system

# Check VolumeSnapshotContent
kubectl get volumesnapshotcontent

# View events
kubectl get events -n rag-system --sort-by='.lastTimestamp'
```

### Volume Full

**Symptoms**: Pods crashing, write errors

**Solutions**:
```bash
# Check usage
kubectl exec -it deployment/rag-document -n rag-system -- df -h /app/storage

# Expand volume (see Volume Expansion section)
kubectl patch pvc document-storage-pvc -n rag-system \
  -p '{"spec":{"resources":{"requests":{"storage":"200Gi"}}}}'

# Or clean up old files
kubectl exec -it deployment/rag-document -n rag-system -- \
  find /app/storage -type f -mtime +30 -delete
```

### Slow Performance

**Symptoms**: High latency for file operations

**Causes**:
- Using pd-standard instead of pd-balanced/pd-ssd
- Disk IOPS limit reached
- Disk size too small (IOPS scales with size)

**Solutions**:
```bash
# Check disk type
kubectl get pvc document-storage-pvc -n rag-system -o yaml | grep storageClassName

# Increase size for more IOPS (pd-balanced: 0.6 IOPS/GB)
kubectl patch pvc document-storage-pvc -n rag-system \
  -p '{"spec":{"resources":{"requests":{"storage":"200Gi"}}}}'

# Or migrate to faster disk type
# (Requires creating new PVC with different StorageClass and copying data)
```

---

## Next Steps

1. **Deploy storage configuration**:
   ```bash
   ./scripts/gcp/14-setup-storage.sh --env dev
   kubectl apply -f k8s/base/storageclass.yaml
   kubectl apply -f k8s/base/volumesnapshot.yaml
   ```

2. **Verify snapshots are working**:
   ```bash
   kubectl get cronjobs -n rag-system
   kubectl logs -l app=snapshot-job -n rag-system
   ```

3. **Set up monitoring**:
   - Create Cloud Monitoring dashboards
   - Configure alerts for storage utilization
   - Set up log-based metrics for snapshot failures

4. **Document recovery procedures**:
   - Test restore process
   - Update runbooks with restore steps
   - Train team on backup/restore operations

5. **Proceed to GCP-INGRESS-010**:
   - Configure ingress controller
   - Set up SSL certificates
   - Deploy Cloud Armor WAF rules

---

## References

- [GKE Persistent Volumes](https://cloud.google.com/kubernetes-engine/docs/concepts/persistent-volumes)
- [Volume Snapshots](https://cloud.google.com/kubernetes-engine/docs/how-to/persistent-volumes/volume-snapshots)
- [Cloud Storage Documentation](https://cloud.google.com/storage/docs)
- [Persistent Disk Pricing](https://cloud.google.com/compute/disks-image-pricing)

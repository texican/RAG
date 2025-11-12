---
version: 1.0.0
last-updated: 2025-11-12
status: active
applies-to: 0.8.0-SNAPSHOT
category: operations
---

# Deployment Troubleshooting Guide

**Last Updated**: 2025-11-12  
**Status**: Active

This document tracks deployment issues encountered in GKE and their resolutions.

## Table of Contents
- [Startup Probe Issues](#startup-probe-issues)
- [Storage and PVC Issues](#storage-and-pvc-issues)
- [Resource Constraints](#resource-constraints)

---

## Startup Probe Issues

### Issue: Slow Startup Causing Liveness Probe Failures

**Affected Services**: rag-document, rag-auth

**Problem**: Spring Boot services with JPA/Hibernate initialization take 80-95 seconds to start, but liveness probe `initialDelaySeconds` was too short, causing kubelet to kill pods before they completed startup.

**Symptoms**:
- Pods stuck in CrashLoopBackOff or 1/2 Running
- Events showing "Liveness probe failed: connect: connection refused"
- Container terminations with exit code 137 (SIGKILL from kubelet)
- Logs showed clean startup but pod killed before completion

**Root Cause**: 
- Spring Boot + Hibernate + JPA initialization takes ~90 seconds in GKE environment
- Liveness probe `initialDelaySeconds: 60s` was too short
- Kubelet killed container after 3 failed probe attempts

**Solution Applied**:

Added `startupProbe` to handle long initialization without killing the pod:

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

**Benefits of startupProbe**:
- Kubelet doesn't run liveness checks until startup completes
- Pod has up to 5 minutes to complete initialization
- Once startup succeeds, fast liveness/readiness probes take over
- Prevents premature pod termination during slow JVM startup

**Files Modified**:
- `k8s/base/rag-document-deployment.yaml`
- `k8s/base/rag-auth-deployment.yaml` (initialDelaySeconds increased to 120s)

**Result**: Pods now start successfully and reach 2/2 Running within 2 minutes.

**Recommendation**: Add startupProbe to all Spring Boot services that take >30s to start.

---

## Storage and PVC Issues

### Issue: PVC Multi-Attach Errors

**Affected Services**: rag-document

**Problem**: Multiple rag-document replicas attempting to mount the same ReadWriteOnce (RWO) PersistentVolumeClaim across different nodes, causing Multi-Attach errors and pods stuck in ContainerCreating.

**Symptoms**:
- Events: `Multi-Attach error for volume ... Volume is already used by pod(s) ...`
- Pods stuck in ContainerCreating indefinitely
- Scheduling churn as Kubernetes tries to place pods on different nodes

**Root Cause**: 
- GCE Persistent Disk (standard storage class) only supports ReadWriteOnce
- RWO volumes can only be mounted by one pod at a time
- Multiple replicas across nodes cannot share the same PVC

**Temporary Solution Applied**:
```yaml
spec:
  replicas: 1  # Temporarily reduced from 2 due to ReadWriteOnce PVC multi-attach issues
```

**Long-term Solutions**:

#### Option A: Use ReadWriteMany Storage (Recommended for shared storage)

**Best for**: Shared document storage across multiple pods

**Implementation**:
```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: document-storage-pvc
  namespace: rag-system
spec:
  accessModes:
  - ReadWriteMany  # Changed from ReadWriteOnce
  resources:
    requests:
      storage: 10Gi
  storageClassName: filestore-csi  # GCP Filestore
```

**Setup Filestore**:
```bash
# Create Filestore instance
gcloud filestore instances create rag-filestore \
  --zone=us-central1-a \
  --tier=BASIC_HDD \
  --file-share=name=documents,capacity=1TB \
  --network=name=default

# Install Filestore CSI driver
kubectl apply -f https://raw.githubusercontent.com/kubernetes-sigs/gcp-filestore-csi-driver/master/deploy/kubernetes/overlays/stable/manifest.yaml
```

**Cost**: ~$200-300/month for Filestore Basic (1TB minimum)

#### Option B: Use Per-Pod PVCs with StatefulSet

**Best for**: When each pod needs its own storage

**Implementation**:
```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: rag-document
spec:
  serviceName: rag-document
  replicas: 2
  volumeClaimTemplates:
  - metadata:
      name: document-storage
    spec:
      accessModes: [ "ReadWriteOnce" ]
      resources:
        requests:
          storage: 10Gi
      storageClassName: standard
```

**Considerations**:
- Each pod gets: `document-storage-pvc-0`, `document-storage-pvc-1`, etc.
- Requires application logic changes if documents need to be shared
- Pod identity is stable (rag-document-0, rag-document-1)

#### Option C: Use Google Cloud Storage (GCS) (Recommended for production)

**Best for**: Scalable, reliable object storage

**Implementation**:

1. **Update FileStorageService**:
```java
@Service
public class FileStorageService {
    @Value("${gcs.bucket-name}")
    private String bucketName;
    
    private final Storage storage;
    
    public String storeFile(MultipartFile file, UUID tenantId, UUID documentId) {
        String blobName = String.format("%s/%s/%s", tenantId, documentId, file.getOriginalFilename());
        BlobId blobId = BlobId.of(bucketName, blobName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        storage.create(blobInfo, file.getBytes());
        return blobName;
    }
}
```

2. **Add GCS dependency** (pom.xml):
```xml
<dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>google-cloud-storage</artifactId>
</dependency>
```

3. **Update deployment** - Remove PVC mount:
```yaml
# No volumes section needed
# No volumeMounts needed
```

4. **Set IAM permissions**:
```bash
# Grant service account access to bucket
gcloud projects add-iam-policy-binding byo-rag-dev \
  --member="serviceAccount:rag-document@byo-rag-dev.iam.gserviceaccount.com" \
  --role="roles/storage.objectAdmin"
```

**Benefits**:
- No PVC needed
- Infinite scalability
- Built-in redundancy and versioning
- Pay only for what you use

**Cost**: ~$0.02/GB/month for Standard Storage

---

### Current Status

| Issue | Status | Solution Applied | Permanent Fix |
|-------|--------|------------------|---------------|
| Slow startup (rag-document) | ✅ Resolved | Added startupProbe | ✅ Complete |
| Slow startup (rag-auth) | ✅ Resolved | Increased initialDelaySeconds to 120s | ✅ Complete |
| PVC multi-attach | ⚠️ Temporary | Scaled to 1 replica | ❌ Pending (use GCS) |

---

## Resource Constraints

### Memory Limits

**Current Configuration** (rag-document):
```yaml
resources:
  requests:
    memory: "512Mi"
    cpu: "250m"
  limits:
    memory: "1Gi"
    cpu: "500m"
```

**Recommendation**: Monitor memory usage with `kubectl top pod` and adjust if needed. For JVM services, set `-Xmx` to ~75% of memory limit.

### CPU Limits

**Startup Performance**: JVM initialization is CPU-intensive. If startup is consistently >2 minutes, consider increasing CPU requests temporarily during startup.

---

## Monitoring Commands

### Check Pod Status
```bash
kubectl get pods -n rag-system -o wide
```

### Check Events
```bash
kubectl get events -n rag-system --sort-by='.metadata.creationTimestamp' | tail -50
```

### Check Probe Failures
```bash
kubectl describe pod <pod-name> -n rag-system | grep -A 10 Events
```

### Check Resource Usage
```bash
kubectl top pods -n rag-system
kubectl top nodes
```

### Check Logs for Startup Time
```bash
kubectl logs <pod-name> -n rag-system | grep "Started.*Application in"
```

---

## Related Documentation

- [GKE Deployment Guide](../deployment/gcp-deployment.md)
- [Kubernetes Best Practices](https://kubernetes.io/docs/concepts/configuration/overview/)
- [Probe Configuration](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/)
- [Storage Classes in GKE](https://cloud.google.com/kubernetes-engine/docs/concepts/persistent-volumes)

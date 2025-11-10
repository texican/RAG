# Kubernetes Manifests for GCP Deployment

This directory contains Kubernetes manifests for deploying the BYO RAG System to Google Kubernetes Engine (GKE).

## Directory Structure

```
k8s/
├── base/                           # Base Kubernetes manifests
│   ├── namespace.yaml             # rag-system namespace
│   ├── serviceaccounts.yaml       # Service accounts with Workload Identity
│   ├── configmap.yaml             # Environment configuration
│   ├── secrets-template.yaml      # Secret templates (not applied directly)
│   ├── rag-auth-deployment.yaml   # Auth service deployment + service
│   ├── rag-document-deployment.yaml # Document service + PVC
│   ├── rag-embedding-deployment.yaml # Embedding service
│   ├── rag-core-deployment.yaml   # Core service
│   ├── rag-admin-deployment.yaml  # Admin service
│   ├── hpa.yaml                   # Horizontal Pod Autoscalers
│   └── kustomization.yaml         # Kustomize base configuration
├── overlays/
│   ├── dev/                       # Development environment
│   │   └── kustomization.yaml    # Dev-specific patches
│   └── prod/                      # Production environment
│       └── kustomization.yaml    # Prod-specific patches
└── README.md                      # This file
```

## Prerequisites

1. **GKE Cluster** (GCP-GKE-007)
   - Cluster created and accessible via kubectl
   - Workload Identity enabled
   - Node pools configured

2. **GCP Resources** 
   - Cloud SQL PostgreSQL instance (GCP-SQL-004)
   - Cloud Memorystore Redis (GCP-REDIS-005)
   - Artifact Registry with service images (GCP-REGISTRY-003)
   - Secret Manager with credentials (GCP-SECRETS-002)

3. **Tools**
   - kubectl >= 1.28
   - kustomize >= 5.0 (built into kubectl)
   - gcloud CLI

## Deployment

### Step 1: Sync Secrets from Secret Manager

```bash
# Run the secrets sync script
cd scripts/gcp
./13-sync-secrets-to-k8s.sh
```

This creates Kubernetes secrets in the `rag-system` namespace:
- `cloud-sql-credentials`: Database username and password
- `redis-credentials`: Redis password
- `jwt-secret`: JWT signing key

### Step 2: Deploy to Development

```bash
# From repository root
kubectl apply -k k8s/overlays/dev

# Verify deployment
kubectl get pods -n rag-system
kubectl get services -n rag-system
kubectl get hpa -n rag-system
```

### Step 3: Deploy to Production

```bash
# Update prod kustomization with correct PROJECT_ID if needed
# Then apply
kubectl apply -k k8s/overlays/prod

# Verify deployment
kubectl get pods -n rag-system -o wide
kubectl get services -n rag-system
```

## Service Architecture

### Services

| Service | Port | Purpose | Dependencies |
|---------|------|---------|--------------|
| `rag-auth` | 8081 | Authentication & Authorization | Cloud SQL, Redis |
| `rag-document` | 8082 | Document Processing | Cloud SQL, Redis, PVC |
| `rag-embedding` | 8083 | Vector Embedding Generation | Redis |
| `rag-core` | 8084 | RAG Query Processing | Redis, Embedding Service |
| `rag-admin` | 8085 | Administration | Cloud SQL |

### Resource Allocation

**Development (1 replica each):**
- rag-auth: 256Mi-512Mi RAM, 250m-500m CPU
- rag-document: 1Gi-2Gi RAM, 1000m-2000m CPU
- rag-embedding: 2Gi-4Gi RAM, 1000m-2000m CPU
- rag-core: 1Gi-2Gi RAM, 1000m-2000m CPU
- rag-admin: 512Mi-1Gi RAM, 500m-1000m CPU

**Production (2-3 replicas):**
- Same resource limits as development
- Higher replica counts for HA

### Cloud SQL Proxy

Services that need Cloud SQL (auth, document, admin) include a Cloud SQL Proxy sidecar:
- Image: `gcr.io/cloud-sql-connectors/cloud-sql-proxy:2.8.0`
- Connects to: `byo-rag-dev:us-central1:rag-postgres`
- Proxy port: 5432 (localhost)
- Resources: 128Mi-256Mi RAM, 100m-200m CPU

### Workload Identity

Each service uses Workload Identity for GCP authentication:

| Kubernetes SA | GCP SA | Permissions |
|--------------|--------|-------------|
| `rag-auth` | `cloud-sql-sa` | Cloud SQL Client |
| `rag-document` | `cloud-sql-sa` | Cloud SQL Client, Storage |
| `rag-embedding` | `pubsub-sa` | Pub/Sub Publisher/Subscriber |
| `rag-core` | `pubsub-sa` | Pub/Sub Publisher/Subscriber |
| `rag-admin` | `cloud-sql-sa` | Cloud SQL Client |

## Horizontal Pod Autoscaling

HPAs are configured for all services (except admin):

- **Target CPU**: 70% utilization
- **Target Memory**: 75-80% utilization
- **Scale-up**: Aggressive (100% every 30s, or +2 pods)
- **Scale-down**: Conservative (50% every 60s after 5-minute stabilization)

**Replica Ranges:**
- rag-auth: 2-5 pods
- rag-document: 2-6 pods
- rag-embedding: 2-6 pods
- rag-core: 2-8 pods

Monitor HPA status:
```bash
kubectl get hpa -n rag-system -w
```

## Configuration

### ConfigMap

The `gcp-config` ConfigMap contains environment-specific configuration:

```yaml
PROJECT_ID: "byo-rag-dev"
REGION: "us-central1"
CLOUD_SQL_INSTANCE_CONNECTION_NAME: "byo-rag-dev:us-central1:rag-postgres"
REDIS_HOST: "10.170.252.12"
REDIS_PORT: "6379"
ARTIFACT_REGISTRY: "us-central1-docker.pkg.dev/byo-rag-dev/rag-system"
```

### Secrets

Secrets are created from Secret Manager (not stored in Git):

```bash
# Cloud SQL credentials
kubectl get secret cloud-sql-credentials -n rag-system

# Redis password
kubectl get secret redis-credentials -n rag-system

# JWT secret
kubectl get secret jwt-secret -n rag-system
```

## Persistent Storage

The document service uses a PersistentVolumeClaim:

```yaml
name: document-storage-pvc
size: 100Gi
storageClass: standard-rwo
accessModes: ReadWriteOnce
```

Check PVC status:
```bash
kubectl get pvc -n rag-system
kubectl describe pvc document-storage-pvc -n rag-system
```

## Health Checks

All services have liveness and readiness probes:

**Liveness Probe:**
- Path: `/actuator/health/liveness`
- Initial Delay: 60s
- Period: 10s
- Timeout: 5s
- Failure Threshold: 3

**Readiness Probe:**
- Path: `/actuator/health/readiness`
- Initial Delay: 30s
- Period: 5s
- Timeout: 3s
- Failure Threshold: 3

## Troubleshooting

### Check Pod Status

```bash
# List all pods
kubectl get pods -n rag-system

# Describe pod
kubectl describe pod <pod-name> -n rag-system

# View logs
kubectl logs -f <pod-name> -n rag-system

# View logs for specific container
kubectl logs -f <pod-name> -c rag-auth -n rag-system
kubectl logs -f <pod-name> -c cloud-sql-proxy -n rag-system
```

### Common Issues

#### 1. ImagePullBackOff

**Cause:** Cannot pull image from Artifact Registry

**Solution:**
```bash
# Verify image exists
gcloud artifacts docker images list \
    us-central1-docker.pkg.dev/byo-rag-dev/rag-system

# Check Workload Identity binding
kubectl describe sa rag-auth -n rag-system
```

#### 2. CrashLoopBackOff

**Cause:** Application failing to start

**Solution:**
```bash
# Check logs
kubectl logs <pod-name> -n rag-system

# Check environment variables
kubectl exec <pod-name> -n rag-system -- env

# Verify secrets exist
kubectl get secrets -n rag-system
```

#### 3. Cloud SQL Connection Errors

**Cause:** Cloud SQL Proxy cannot connect

**Solution:**
```bash
# Check Cloud SQL Proxy logs
kubectl logs <pod-name> -c cloud-sql-proxy -n rag-system

# Verify Workload Identity
gcloud iam service-accounts get-iam-policy cloud-sql-sa@byo-rag-dev.iam.gserviceaccount.com

# Test connection from pod
kubectl exec -it <pod-name> -n rag-system -- \
    psql -h 127.0.0.1 -U rag_user -d rag_enterprise
```

#### 4. Redis Connection Errors

**Cause:** Cannot connect to Cloud Memorystore

**Solution:**
```bash
# Verify Redis secret
kubectl get secret redis-credentials -n rag-system -o yaml

# Test Redis connection from pod
kubectl exec -it <pod-name> -n rag-system -- \
    redis-cli -h 10.170.252.12 -a <password> ping
```

### Service Discovery

Services communicate via internal DNS:

```bash
# Test service discovery
kubectl run -it --rm test-client \
    --image=busybox \
    -n rag-system \
    -- wget -O- http://rag-auth:8081/actuator/health
```

## Updating Manifests

### Update Image Tag

```bash
# Update image in deployment
kubectl set image deployment/rag-auth \
    rag-auth=us-central1-docker.pkg.dev/byo-rag-dev/rag-system/rag-auth:v1.0.1 \
    -n rag-system

# Or apply updated manifest
kubectl apply -f k8s/base/rag-auth-deployment.yaml
```

### Scale Deployment

```bash
# Manual scaling
kubectl scale deployment rag-core --replicas=5 -n rag-system

# Update HPA
kubectl edit hpa rag-core-hpa -n rag-system
```

### Update ConfigMap

```bash
# Edit ConfigMap
kubectl edit configmap gcp-config -n rag-system

# Restart deployments to pick up changes
kubectl rollout restart deployment/rag-auth -n rag-system
```

## Cleanup

```bash
# Delete all resources in namespace
kubectl delete namespace rag-system

# Or delete specific resources
kubectl delete -k k8s/overlays/dev
```

## Next Steps

1. **GCP-INGRESS-010**: Configure Ingress and Load Balancer
2. **GCP-DEPLOY-011**: Perform initial deployment
3. **GCP-KAFKA-006**: Migrate from Kafka to Cloud Pub/Sub
4. **Monitoring**: Set up custom dashboards and alerts

## References

- [GKE Cluster Setup Guide](../../docs/deployment/GKE_CLUSTER_SETUP.md)
- [Cloud SQL Proxy Documentation](https://cloud.google.com/sql/docs/postgres/connect-kubernetes-engine)
- [Workload Identity](https://cloud.google.com/kubernetes-engine/docs/how-to/workload-identity)
- [Kustomize Documentation](https://kustomize.io/)

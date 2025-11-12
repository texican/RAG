---
version: 1.0.0
last-updated: 2025-11-12
status: active
applies-to: 0.8.0-SNAPSHOT
category: deployment
---

# GKE Cluster Setup and Operations Guide

**Last Updated:** 2025-11-09  
**Status:** Production Ready  
**GCP Service:** Google Kubernetes Engine (GKE)

## Table of Contents

1. [Overview](#overview)
2. [Cluster Architecture](#cluster-architecture)
3. [Setup Instructions](#setup-instructions)
4. [Cluster Configuration](#cluster-configuration)
5. [Node Pools](#node-pools)
6. [Workload Identity](#workload-identity)
7. [Network Policies](#network-policies)
8. [Monitoring and Logging](#monitoring-and-logging)
9. [Scaling Operations](#scaling-operations)
10. [Maintenance and Upgrades](#maintenance-and-upgrades)
11. [Troubleshooting](#troubleshooting)
12. [Security Best Practices](#security-best-practices)

---

## Overview

This guide covers the setup and operation of the GKE cluster for the BYO RAG System. The cluster is configured with production-ready features including:

- **Regional High Availability** (production) or **Zonal** (development)
- **Multiple Node Pools** for workload isolation
- **Workload Identity** for secure pod-level IAM
- **VPC-Native Networking** with private nodes
- **Cluster Autoscaling** for dynamic resource management
- **Shielded GKE Nodes** for enhanced security
- **Network Policies** for pod-to-pod traffic control

### Cluster Resources

**Development Cluster:**
- **Name:** `rag-gke-dev`
- **Type:** Zonal (us-central1-a)
- **Nodes:** 2-5 (autoscaling)
- **Machine Types:** e2-medium (system), e2-standard-4 (workload)
- **Monthly Cost:** ~$150-300

**Production Cluster:**
- **Name:** `rag-gke-prod`
- **Type:** Regional (us-central1-a/b/c)
- **Nodes:** 4-13 (autoscaling across zones)
- **Machine Types:** e2-standard-2 (system), n1-standard-4 (workload)
- **Monthly Cost:** ~$800-1500

---

## Cluster Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         GKE Cluster                              │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                  Control Plane (Managed by GCP)          │   │
│  │  - Kubernetes API Server                                  │   │
│  │  - etcd                                                   │   │
│  │  - Controller Manager                                     │   │
│  │  - Scheduler                                              │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │               Node Pool: system-pool                      │   │
│  │  Purpose: Cluster components (ingress, monitoring)        │   │
│  │  Machine: e2-medium / e2-standard-2                       │   │
│  │  Min/Max: 1-2 (dev) / 1-3 (prod)                         │   │
│  │  Taints: workload-type=system:NoSchedule                 │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │             Node Pool: workload-pool                      │   │
│  │  Purpose: Application services                            │   │
│  │  Machine: e2-standard-4 / n1-standard-4                  │   │
│  │  Min/Max: 2-5 (dev) / 3-10 (prod)                        │   │
│  │  Disk: 100GB SSD                                          │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                  VPC Network                              │   │
│  │  - Private Nodes (no external IPs)                        │   │
│  │  - Pod CIDR: 10.4.0.0/14                                 │   │
│  │  - Service CIDR: 10.8.0.0/20                             │   │
│  │  - Master CIDR: 172.16.0.0/28                            │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### Service Deployment

```
Namespace: rag-system
├── rag-auth (Deployment)
│   ├── Replicas: 2-3
│   ├── Resources: 512Mi-1Gi RAM, 500m-1000m CPU
│   └── Dependencies: Cloud SQL, Redis
├── rag-document (Deployment)
│   ├── Replicas: 2-4
│   ├── Resources: 1Gi-2Gi RAM, 1000m-2000m CPU
│   └── Dependencies: Cloud SQL, Pub/Sub, Cloud Storage
├── rag-embedding (Deployment)
│   ├── Replicas: 2-4
│   ├── Resources: 2Gi-4Gi RAM, 1000m-2000m CPU
│   └── Dependencies: Redis, Pub/Sub
├── rag-core (Deployment)
│   ├── Replicas: 2-4
│   ├── Resources: 1Gi-2Gi RAM, 1000m-2000m CPU
│   └── Dependencies: Redis, Pub/Sub, Embedding Service
└── rag-admin (Deployment)
    ├── Replicas: 1-2
    ├── Resources: 512Mi-1Gi RAM, 500m-1000m CPU
    └── Dependencies: Cloud SQL
```

---

## Setup Instructions

### Prerequisites

1. **GCP Project Setup** (GCP-INFRA-001)
   - Project created and billing enabled
   - APIs enabled: Container, Compute, Logging, Monitoring

2. **Service Accounts** (GCP-SECRETS-002)
   - `gke-node-sa` with required IAM roles
   - Service-specific SAs for Workload Identity

3. **Networking**
   - Default VPC network
   - Firewall rules for GKE
   - Cloud NAT for outbound traffic (private nodes)

4. **Dependencies**
   - Cloud SQL instance (GCP-SQL-004)
   - Cloud Memorystore Redis (GCP-REDIS-005)
   - Artifact Registry (GCP-REGISTRY-003)

### Installation

#### Step 1: Run Setup Script

```bash
# Development cluster
cd scripts/gcp
./12-setup-gke-cluster.sh --dev

# Production cluster
./12-setup-gke-cluster.sh --prod
```

**Script Duration:** 10-15 minutes

#### Step 2: Verify Cluster

```bash
# Get cluster credentials
gcloud container clusters get-credentials rag-gke-dev \
    --region=us-central1 \
    --project=byo-rag-dev

# Check cluster status
kubectl cluster-info
kubectl get nodes
kubectl get namespaces
```

#### Step 3: Verify Add-ons

```bash
# Check ingress controller
kubectl get pods -n ingress-nginx

# Check cert-manager
kubectl get pods -n cert-manager

# Check network policies
kubectl get networkpolicies -n rag-system
```

---

## Cluster Configuration

### Control Plane

The GKE control plane is fully managed by Google Cloud Platform:

- **Kubernetes Version:** Latest stable (auto-upgraded via regular release channel)
- **Release Channel:** Regular (balanced stability and features)
- **Maintenance Window:** Sunday 00:00-04:00 UTC
- **High Availability:** Multi-zone control plane (production)

### Cluster Features

```bash
# View cluster configuration
gcloud container clusters describe rag-gke-dev \
    --region=us-central1 \
    --format=yaml
```

**Key Features:**
- ✅ **Workload Identity:** Pod-level IAM without service account keys
- ✅ **VPC-Native:** IP alias for pods and services
- ✅ **Private Cluster:** Nodes have no external IPs
- ✅ **Shielded Nodes:** Secure boot and integrity monitoring
- ✅ **Network Policy:** Pod-to-pod traffic control
- ✅ **Binary Authorization:** Image signature verification (optional)
- ✅ **Cloud Logging/Monitoring:** Integrated observability

### Cluster Autoscaling

```bash
# View autoscaling configuration
gcloud container clusters describe rag-gke-dev \
    --region=us-central1 \
    --format="value(autoscaling)"

# Update autoscaling profile
gcloud container clusters update rag-gke-dev \
    --region=us-central1 \
    --autoscaling-profile=optimize-utilization
```

**Autoscaling Profiles:**
- `balanced`: Default, balances cost and performance
- `optimize-utilization`: Aggressive scale-down, minimize cost

---

## Node Pools

### System Node Pool

**Purpose:** Cluster infrastructure components (ingress, monitoring, cert-manager)

```yaml
Name: system-pool
Machine Type: e2-medium (dev) / e2-standard-2 (prod)
Disk: 50GB SSD
Autoscaling:
  Min Nodes: 1
  Max Nodes: 2 (dev) / 3 (prod)
Taints:
  - workload-type=system:NoSchedule
Labels:
  - workload-type=system
```

**Components Running:**
- NGINX Ingress Controller
- cert-manager
- GKE system pods (kube-proxy, fluentd, metrics-server)

### Workload Node Pool

**Purpose:** Application services (RAG microservices)

```yaml
Name: workload-pool
Machine Type: e2-standard-4 (dev) / n1-standard-4 (prod)
Disk: 100GB SSD
Autoscaling:
  Min Nodes: 2 (dev) / 3 (prod)
  Max Nodes: 5 (dev) / 10 (prod)
Labels:
  - workload-type=application
```

**Services Running:**
- rag-auth
- rag-document
- rag-embedding
- rag-core
- rag-admin

### Node Pool Management

```bash
# List node pools
gcloud container node-pools list \
    --cluster=rag-gke-dev \
    --region=us-central1

# View node pool details
gcloud container node-pools describe workload-pool \
    --cluster=rag-gke-dev \
    --region=us-central1

# Update node pool size
gcloud container node-pools update workload-pool \
    --cluster=rag-gke-dev \
    --region=us-central1 \
    --enable-autoscaling \
    --min-nodes=3 \
    --max-nodes=12

# Manually resize node pool (overrides autoscaling temporarily)
gcloud container clusters resize rag-gke-dev \
    --region=us-central1 \
    --node-pool=workload-pool \
    --num-nodes=5
```

---

## Workload Identity

Workload Identity allows pods to authenticate as GCP service accounts without service account keys.

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Pod (rag-auth)                          │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ K8s ServiceAccount: rag-auth                           │  │
│  │ Annotation: iam.gke.io/gcp-service-account=           │  │
│  │             cloud-sql-sa@PROJECT.iam.gserviceaccount  │  │
│  └───────────────────────────────────────────────────────┘  │
│                            ↓                                 │
│  ┌───────────────────────────────────────────────────────┐  │
│  │        Workload Identity Binding                       │  │
│  │  roles/iam.workloadIdentityUser                       │  │
│  └───────────────────────────────────────────────────────┘  │
│                            ↓                                 │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  GCP ServiceAccount: cloud-sql-sa                     │  │
│  │  Permissions: cloudsql.client, secretmanager.viewer   │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Service Account Mappings

| Kubernetes SA | GCP SA | Purpose |
|--------------|--------|---------|
| `rag-auth` | `cloud-sql-sa` | Cloud SQL access |
| `rag-document` | `cloud-sql-sa` | Cloud SQL access |
| `rag-embedding` | `pubsub-sa` | Pub/Sub access |
| `rag-core` | `pubsub-sa` | Pub/Sub access |
| `rag-admin` | `cloud-sql-sa` | Cloud SQL access |

### Configuration

**Kubernetes ServiceAccount with Workload Identity:**

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: rag-auth
  namespace: rag-system
  annotations:
    iam.gke.io/gcp-service-account: cloud-sql-sa@byo-rag-dev.iam.gserviceaccount.com
```

**Pod using Workload Identity:**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: rag-auth
  namespace: rag-system
spec:
  template:
    spec:
      serviceAccountName: rag-auth
      containers:
      - name: rag-auth
        image: us-central1-docker.pkg.dev/byo-rag-dev/rag-system/rag-auth:latest
        env:
        - name: GOOGLE_APPLICATION_CREDENTIALS
          value: /var/run/secrets/workload-identity/gcp-service-account.json
```

### Verification

```bash
# Verify Workload Identity binding
gcloud iam service-accounts get-iam-policy cloud-sql-sa@byo-rag-dev.iam.gserviceaccount.com

# Test from pod
kubectl run -it --rm --restart=Never test-wi \
    --image=google/cloud-sdk:slim \
    --serviceaccount=rag-auth \
    -n rag-system \
    -- gcloud auth list
```

---

## Network Policies

Network policies control pod-to-pod communication within the cluster.

### Default Deny Policy

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny-ingress
  namespace: rag-system
spec:
  podSelector: {}
  policyTypes:
  - Ingress
```

### Allow Ingress Controller

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-ingress-controller
  namespace: rag-system
spec:
  podSelector:
    matchLabels:
      app: rag-gateway
  policyTypes:
  - Ingress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: ingress-nginx
    ports:
    - protocol: TCP
      port: 8080
```

### Allow Inter-Service Communication

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-rag-services
  namespace: rag-system
spec:
  podSelector:
    matchLabels:
      app.kubernetes.io/part-of: rag-system
  policyTypes:
  - Ingress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          app.kubernetes.io/part-of: rag-system
```

### Testing Network Policies

```bash
# Test connectivity between pods
kubectl run -it --rm test-client \
    --image=busybox \
    -n rag-system \
    --labels="app.kubernetes.io/part-of=rag-system" \
    -- wget -O- http://rag-auth:8081/actuator/health

# View network policies
kubectl get networkpolicies -n rag-system
kubectl describe networkpolicy allow-rag-services -n rag-system
```

---

## Monitoring and Logging

### Cloud Monitoring Integration

GKE automatically sends metrics to Cloud Monitoring:

```bash
# View cluster metrics in Cloud Console
gcloud monitoring dashboards list

# Query metrics using MQL
gcloud monitoring time-series list \
    --filter='resource.type="k8s_cluster" AND resource.labels.cluster_name="rag-gke-dev"'
```

**Key Metrics:**
- **Node CPU/Memory Utilization**
- **Pod CPU/Memory Usage**
- **Network Traffic (ingress/egress)**
- **Disk I/O**
- **Container Restart Count**

### Cloud Logging

```bash
# View logs in Cloud Console
gcloud logging read "resource.type=k8s_cluster AND resource.labels.cluster_name=rag-gke-dev" \
    --limit=50 \
    --format=json

# View pod logs
kubectl logs -f deployment/rag-auth -n rag-system

# View logs for all pods in deployment
kubectl logs -f -l app=rag-auth -n rag-system --all-containers=true
```

### Prometheus and Grafana (Optional)

```bash
# Install kube-prometheus-stack
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

helm install kube-prometheus-stack prometheus-community/kube-prometheus-stack \
    --namespace monitoring \
    --create-namespace \
    --set prometheus.prometheusSpec.serviceMonitorSelectorNilUsesHelmValues=false
```

---

## Scaling Operations

### Horizontal Pod Autoscaling (HPA)

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: rag-core-hpa
  namespace: rag-system
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: rag-core
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

```bash
# Create HPA
kubectl apply -f hpa.yaml

# View HPA status
kubectl get hpa -n rag-system
kubectl describe hpa rag-core-hpa -n rag-system

# Manual scaling (overrides HPA)
kubectl scale deployment rag-core --replicas=5 -n rag-system
```

### Cluster Autoscaling

```bash
# View autoscaling status
kubectl get nodes -o wide
gcloud container clusters describe rag-gke-dev \
    --region=us-central1 \
    --format="value(autoscaling)"

# Trigger scale-up (deploy resource-intensive workload)
kubectl apply -f resource-intensive-deployment.yaml

# Trigger scale-down (delete workload and wait 10-15 minutes)
kubectl delete deployment resource-intensive
```

### Vertical Pod Autoscaling (VPA)

```bash
# Install VPA (if not already installed)
git clone https://github.com/kubernetes/autoscaler.git
cd autoscaler/vertical-pod-autoscaler
./hack/vpa-up.sh

# Create VPA for deployment
cat <<EOF | kubectl apply -f -
apiVersion: autoscaling.k8s.io/v1
kind: VerticalPodAutoscaler
metadata:
  name: rag-auth-vpa
  namespace: rag-system
spec:
  targetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: rag-auth
  updatePolicy:
    updateMode: "Auto"
EOF

# View VPA recommendations
kubectl describe vpa rag-auth-vpa -n rag-system
```

---

## Maintenance and Upgrades

### Cluster Upgrades

GKE automatically upgrades clusters in the **regular** release channel:

```bash
# View available versions
gcloud container get-server-config \
    --region=us-central1 \
    --format="yaml(channels)"

# Manually upgrade cluster (if needed)
gcloud container clusters upgrade rag-gke-dev \
    --region=us-central1 \
    --cluster-version=1.28.3-gke.1203001

# Upgrade node pool
gcloud container clusters upgrade rag-gke-dev \
    --region=us-central1 \
    --node-pool=workload-pool
```

### Maintenance Window

- **Schedule:** Sunday 00:00-04:00 UTC
- **Frequency:** Weekly
- **Recurrence:** FREQ=WEEKLY;BYDAY=SU
- **Duration:** 4 hours

```bash
# Update maintenance window
gcloud container clusters update rag-gke-dev \
    --region=us-central1 \
    --maintenance-window-start="2024-01-01T02:00:00Z" \
    --maintenance-window-duration="4h" \
    --maintenance-window-recurrence="FREQ=WEEKLY;BYDAY=SU"
```

### Node Pool Upgrades

```bash
# Disable auto-upgrade temporarily
gcloud container node-pools update workload-pool \
    --cluster=rag-gke-dev \
    --region=us-central1 \
    --no-enable-autoupgrade

# Manually upgrade node pool
gcloud container node-pools upgrade workload-pool \
    --cluster=rag-gke-dev \
    --region=us-central1

# Re-enable auto-upgrade
gcloud container node-pools update workload-pool \
    --cluster=rag-gke-dev \
    --region=us-central1 \
    --enable-autoupgrade
```

---

## Troubleshooting

### Common Issues

#### Issue 1: Pods Not Scheduling

**Symptoms:**
- Pods stuck in `Pending` state
- Events show "Insufficient cpu" or "Insufficient memory"

**Solution:**
```bash
# Check pod status
kubectl get pods -n rag-system
kubectl describe pod <pod-name> -n rag-system

# Check node resources
kubectl top nodes
kubectl describe nodes

# Manually trigger scale-up
gcloud container clusters resize rag-gke-dev \
    --region=us-central1 \
    --node-pool=workload-pool \
    --num-nodes=5
```

#### Issue 2: Workload Identity Not Working

**Symptoms:**
- Pods can't access GCP resources
- Error: "could not generate access token"

**Solution:**
```bash
# Verify Workload Identity binding
gcloud iam service-accounts get-iam-policy cloud-sql-sa@byo-rag-dev.iam.gserviceaccount.com

# Verify service account annotation
kubectl get sa rag-auth -n rag-system -o yaml

# Re-create binding
gcloud iam service-accounts add-iam-policy-binding cloud-sql-sa@byo-rag-dev.iam.gserviceaccount.com \
    --role=roles/iam.workloadIdentityUser \
    --member="serviceAccount:byo-rag-dev.svc.id.goog[rag-system/rag-auth]"
```

#### Issue 3: Network Policy Blocking Traffic

**Symptoms:**
- Services can't communicate
- Connection timeouts

**Solution:**
```bash
# List network policies
kubectl get networkpolicies -n rag-system

# Temporarily disable network policy
kubectl delete networkpolicy default-deny-ingress -n rag-system

# Test connectivity
kubectl run -it --rm test-client --image=busybox -n rag-system -- wget -O- http://rag-auth:8081/actuator/health

# Re-apply network policy with correct selectors
kubectl apply -f network-policies.yaml
```

#### Issue 4: Cluster Autoscaling Not Working

**Symptoms:**
- Nodes not scaling up/down
- Pods pending but no new nodes

**Solution:**
```bash
# Check autoscaling configuration
gcloud container clusters describe rag-gke-dev \
    --region=us-central1 \
    --format="value(autoscaling)"

# Check node pool autoscaling
gcloud container node-pools describe workload-pool \
    --cluster=rag-gke-dev \
    --region=us-central1 \
    --format="value(autoscaling)"

# View autoscaler logs
kubectl logs -n kube-system -l app=cluster-autoscaler

# Manually trigger scale-up
kubectl scale deployment rag-core --replicas=10 -n rag-system
```

### Debugging Commands

```bash
# Check cluster health
kubectl cluster-info
kubectl get componentstatuses
kubectl get events -n rag-system --sort-by='.lastTimestamp'

# Check node health
kubectl get nodes
kubectl describe nodes
kubectl top nodes

# Check pod health
kubectl get pods -n rag-system -o wide
kubectl describe pod <pod-name> -n rag-system
kubectl logs <pod-name> -n rag-system
kubectl exec -it <pod-name> -n rag-system -- /bin/bash

# Check services and endpoints
kubectl get services -n rag-system
kubectl get endpoints -n rag-system
kubectl describe service rag-auth -n rag-system

# Check resource quotas
kubectl get resourcequotas -n rag-system
kubectl describe resourcequota -n rag-system
```

---

## Security Best Practices

### 1. Network Security

- ✅ **Private Cluster:** Nodes have no external IPs
- ✅ **Authorized Networks:** Restrict API access to trusted IPs
- ✅ **Network Policies:** Control pod-to-pod traffic
- ✅ **Private Google Access:** Nodes access GCP services via private IPs

```bash
# Enable authorized networks
gcloud container clusters update rag-gke-dev \
    --region=us-central1 \
    --enable-master-authorized-networks \
    --master-authorized-networks="203.0.113.0/24,198.51.100.0/24"
```

### 2. Pod Security

- ✅ **Workload Identity:** No service account keys in pods
- ✅ **Security Context:** Run as non-root user
- ✅ **Read-Only Root Filesystem:** Prevent container modifications
- ✅ **Resource Limits:** Prevent resource exhaustion

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: secure-pod
spec:
  securityContext:
    runAsNonRoot: true
    runAsUser: 1000
    fsGroup: 2000
  containers:
  - name: app
    image: app:latest
    securityContext:
      allowPrivilegeEscalation: false
      readOnlyRootFilesystem: true
      capabilities:
        drop:
        - ALL
    resources:
      limits:
        cpu: 1000m
        memory: 1Gi
      requests:
        cpu: 500m
        memory: 512Mi
```

### 3. Image Security

- ✅ **Vulnerability Scanning:** Artifact Registry auto-scans images
- ✅ **Binary Authorization:** Require signed images (optional)
- ✅ **Private Registry:** Use Artifact Registry, not Docker Hub

```bash
# Enable Binary Authorization
gcloud container clusters update rag-gke-dev \
    --region=us-central1 \
    --enable-binauthz
```

### 4. Secrets Management

- ✅ **Secret Manager CSI:** Mount secrets as volumes
- ✅ **No Hardcoded Secrets:** Use environment variables from secrets
- ✅ **RBAC:** Limit access to secrets

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: app-with-secrets
spec:
  serviceAccountName: rag-auth
  volumes:
  - name: db-password
    csi:
      driver: secrets-store.csi.k8s.io
      readOnly: true
      volumeAttributes:
        secretProviderClass: "db-secrets"
  containers:
  - name: app
    image: app:latest
    volumeMounts:
    - name: db-password
      mountPath: "/mnt/secrets"
      readOnly: true
```

### 5. RBAC

```bash
# Create role for developers (read-only)
kubectl create role pod-reader \
    --verb=get,list,watch \
    --resource=pods,deployments,services \
    -n rag-system

# Bind role to user
kubectl create rolebinding pod-reader-binding \
    --role=pod-reader \
    --user=developer@example.com \
    -n rag-system

# View RBAC permissions
kubectl auth can-i list pods -n rag-system --as=developer@example.com
```

---

## Cost Optimization

### 1. Node Pool Right-Sizing

```bash
# Use preemptible nodes for dev/staging
gcloud container node-pools create preemptible-pool \
    --cluster=rag-gke-dev \
    --region=us-central1 \
    --machine-type=e2-standard-4 \
    --preemptible \
    --num-nodes=2 \
    --enable-autoscaling \
    --min-nodes=0 \
    --max-nodes=5
```

### 2. Cluster Autoscaling

```bash
# Use optimize-utilization profile for aggressive scale-down
gcloud container clusters update rag-gke-dev \
    --region=us-central1 \
    --autoscaling-profile=optimize-utilization
```

### 3. Resource Quotas

```yaml
apiVersion: v1
kind: ResourceQuota
metadata:
  name: compute-quota
  namespace: rag-system
spec:
  hard:
    requests.cpu: "20"
    requests.memory: "40Gi"
    limits.cpu: "40"
    limits.memory: "80Gi"
```

---

## References

- [GKE Documentation](https://cloud.google.com/kubernetes-engine/docs)
- [Workload Identity](https://cloud.google.com/kubernetes-engine/docs/how-to/workload-identity)
- [GKE Security Best Practices](https://cloud.google.com/kubernetes-engine/docs/how-to/hardening-your-cluster)
- [Kubernetes Network Policies](https://kubernetes.io/docs/concepts/services-networking/network-policies/)
- [GKE Pricing Calculator](https://cloud.google.com/products/calculator)

---

**Document Version:** 1.0  
**Last Reviewed:** 2025-11-09  
**Next Review:** 2025-12-09

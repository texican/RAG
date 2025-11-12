---
version: 1.0.0
last-updated: 2025-11-12
status: active
applies-to: 0.8.0-SNAPSHOT
category: deployment
---

# Initial Deployment Guide - RAG System on GCP

## Overview

This guide provides comprehensive step-by-step instructions for deploying the RAG (Retrieval-Augmented Generation) system to Google Cloud Platform (GCP) using Google Kubernetes Engine (GKE).

**Deployment Time**: 2-3 hours for first-time deployment
**Target Environment**: Development or Production
**Prerequisites Time**: ~30 minutes

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Pre-Deployment Checklist](#pre-deployment-checklist)
3. [Deployment Steps](#deployment-steps)
4. [Post-Deployment Validation](#post-deployment-validation)
5. [Troubleshooting](#troubleshooting)
6. [Rollback Procedures](#rollback-procedures)
7. [Production Considerations](#production-considerations)

---

## Prerequisites

### Required Tools

| Tool | Version | Purpose | Installation |
|------|---------|---------|--------------|
| **gcloud CLI** | Latest | GCP management | [Install Guide](https://cloud.google.com/sdk/docs/install) |
| **kubectl** | 1.28+ | Kubernetes management | `gcloud components install kubectl` |
| **docker** | 20.10+ | Container image building | [Install Guide](https://docs.docker.com/get-docker/) |
| **psql** | 14+ | Database verification | `brew install postgresql` (macOS) |
| **curl** | Latest | API testing | Pre-installed on most systems |

### GCP Resources Required

Ensure the following resources are provisioned (see previous GCP setup tasks):

- ✅ GCP Project with billing enabled (GCP-INFRA-001)
- ✅ APIs enabled: GKE, Cloud SQL, Cloud Memorystore, Artifact Registry, Secret Manager
- ✅ VPC network configured (GCP-INFRA-001)
- ✅ Secret Manager secrets created (GCP-SECRETS-002)
- ✅ Artifact Registry for container images (GCP-REGISTRY-003)
- ✅ Cloud SQL PostgreSQL instance (GCP-SQL-004)
- ✅ Cloud Memorystore Redis instance (GCP-REDIS-005)
- ✅ GKE cluster created (GCP-GKE-007)
- ✅ Kubernetes manifests ready (GCP-K8S-008)
- ✅ Cloud Storage buckets configured (GCP-STORAGE-009)
- ✅ Ingress and load balancer configured (GCP-INGRESS-010)

### Access Requirements

```bash
# Authenticate with GCP
gcloud auth login

# Set default project
gcloud config set project byo-rag-dev

# Get GKE cluster credentials
gcloud container clusters get-credentials rag-cluster-dev \
  --region us-central1 \
  --project byo-rag-dev

# Verify kubectl access
kubectl cluster-info
kubectl get nodes
```

---

## Pre-Deployment Checklist

### 1. Container Images

Verify all container images are built and pushed:

```bash
# List images in Artifact Registry
gcloud artifacts docker images list \
  us-central1-docker.pkg.dev/byo-rag-dev/rag-system

# Expected images:
# - rag-auth-service:latest
# - rag-admin-service:latest
# - rag-document-service:latest
# - rag-embedding-service:latest
# - rag-core-service:latest
```

**If images are missing**, build and push:
```bash
./scripts/gcp/07-build-and-push-images.sh --env dev
```

### 2. Kubernetes Secrets

Verify secrets are synced to cluster:

```bash
# Check required secrets
kubectl get secrets -n rag-system

# Expected secrets:
# - gcp-secrets
# - postgres-credentials
# - redis-credentials
```

**If secrets are missing**, sync them:
```bash
./scripts/gcp/13-sync-secrets-to-k8s.sh --env dev
```

### 3. Infrastructure Validation

```bash
# Cloud SQL instance
gcloud sql instances describe rag-postgres-dev

# Redis instance
gcloud redis instances describe rag-redis-dev --region=us-central1

# GKE cluster
kubectl get nodes
kubectl get namespaces
```

### 4. Configuration Review

Review environment-specific configurations:

```bash
# Dev environment
cat k8s/overlays/dev/kustomization.yaml

# Production environment (if deploying to prod)
cat k8s/overlays/prod/kustomization.yaml
```

---

## Deployment Steps

### Step 1: Deploy Services to GKE

Deploy all 5 microservices in dependency order:

```bash
cd /path/to/RAG

# Development deployment
./scripts/gcp/17-deploy-services.sh --env dev

# Production deployment
./scripts/gcp/17-deploy-services.sh --env prod --project byo-rag-prod
```

**What this script does**:
1. Validates prerequisites (gcloud, kubectl, images)
2. Deploys services in order:
   - rag-auth-service (authentication)
   - rag-admin-service (administration)
   - rag-document-service (document management)
   - rag-embedding-service (embedding generation)
   - rag-core-service (RAG queries)
3. Waits for each service to become healthy
4. Validates health endpoints
5. Tests inter-service connectivity

**Expected output**:
```
[INFO] Deploying service: rag-auth
[INFO] Waiting for deployment rollout to complete...
[SUCCESS] Deployment rollout completed for rag-auth
[SUCCESS] rag-auth is healthy and responding to health checks
...
[SUCCESS] All services deployed successfully!
```

**Deployment time**: 10-15 minutes

### Step 2: Monitor Pod Startup

Watch pods as they start:

```bash
# Watch all pods
kubectl get pods -n rag-system -w

# Expected status:
NAME                                    READY   STATUS    RESTARTS   AGE
rag-auth-service-xxxxx-xxxxx            1/1     Running   0          2m
rag-admin-service-xxxxx-xxxxx           1/1     Running   0          2m
rag-document-service-xxxxx-xxxxx        1/1     Running   0          2m
rag-embedding-service-xxxxx-xxxxx       1/1     Running   0          2m
rag-core-service-xxxxx-xxxxx            1/1     Running   0          2m
```

**Common statuses**:
- `Pending`: Waiting for resources/image pull
- `ContainerCreating`: Pod is being created
- `Running`: Pod is healthy and running
- `CrashLoopBackOff`: Pod is failing (requires troubleshooting)

### Step 3: Initialize Database

Run database migrations and create admin user:

```bash
./scripts/gcp/18-init-database.sh --env dev
```

**What this script does**:
1. Tests Cloud SQL connectivity via Cloud SQL Proxy
2. Runs Flyway database migrations (tables, indexes)
3. Creates default tenant
4. Creates ADMIN role
5. Creates admin user with BCrypt password hash
6. Assigns ADMIN role to admin user
7. Verifies admin user setup
8. Tests authentication

**Expected output**:
```
[INFO] Testing database connectivity...
[SUCCESS] Database connectivity verified
[INFO] Running database migrations...
[SUCCESS] Database migrations completed successfully
[INFO] Creating admin user...
[SUCCESS] Admin user created
[SUCCESS] Admin authentication successful!

Admin Credentials:
  Email: admin@enterprise-rag.com
  Password: admin123
```

**Migration time**: 2-3 minutes

### Step 4: Validate Deployment

Run comprehensive validation tests:

```bash
./scripts/gcp/19-validate-deployment.sh --env dev
```

**What this script validates**:
1. Pod status (all pods running and ready)
2. Service endpoints (ClusterIP services have endpoints)
3. Health endpoints (liveness and readiness checks)
4. Inter-service connectivity (DNS and HTTP)
5. Database connectivity (Cloud SQL)
6. Redis connectivity (Memorystore)
7. Resource usage (CPU/memory, no OOMKilled pods)
8. Persistent volumes (PVCs bound)
9. Admin authentication (login works)
10. Swagger UI access
11. Integration tests (optional)
12. RAG workflow end-to-end

**Expected output**:
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
VALIDATION SUMMARY
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Total Tests: 12
Passed: 12
Failed: 0
Warnings: 0

✓ All validation tests passed!

Deployment is healthy and ready for use.
```

**Validation time**: 5-10 minutes

### Step 5: Configure Ingress (Optional)

If external HTTPS access is needed:

```bash
# Replace with your actual domain
./scripts/gcp/16-setup-ingress.sh \
  --env dev \
  --domain rag-dev.example.com

# Or skip DNS if using external DNS provider
./scripts/gcp/16-setup-ingress.sh \
  --env dev \
  --domain rag-dev.example.com \
  --skip-dns
```

**What this script does**:
1. Reserves static IP address
2. Creates Cloud DNS zone and A records
3. Creates Cloud Armor security policy
4. Updates ingress manifest with domain
5. Deploys ingress configuration

**Ingress setup time**: 5-10 minutes
**SSL certificate provisioning**: 5-10 minutes (via cert-manager)

### Step 6: Access Services

#### Option A: Port-Forward (Development)

```bash
# Auth service
kubectl port-forward -n rag-system svc/rag-auth-service 8081:8081

# Core service
kubectl port-forward -n rag-system svc/rag-core-service 8084:8084

# Document service
kubectl port-forward -n rag-system svc/rag-document-service 8082:8082
```

Test endpoints:
```bash
# Health check
curl http://localhost:8081/actuator/health

# Swagger UI
open http://localhost:8081/swagger-ui.html

# Login
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@enterprise-rag.com","password":"admin123"}'
```

#### Option B: Ingress (Production)

```bash
# Check ingress status
kubectl get ingress -n rag-system

# Get external IP
kubectl get svc -n ingress-nginx

# Access via domain
curl https://rag.example.com
curl https://api.rag.example.com/api/v1/auth/health
```

---

## Post-Deployment Validation

### 1. Service Health Checks

```bash
# All pods running
kubectl get pods -n rag-system

# Check logs for errors
kubectl logs -n rag-system -l app=rag-auth --tail=100
kubectl logs -n rag-system -l app=rag-core --tail=100

# Check events
kubectl get events -n rag-system --sort-by='.lastTimestamp'
```

### 2. Test Authentication

```bash
# Port-forward auth service
kubectl port-forward -n rag-system svc/rag-auth-service 8081:8081 &

# Login
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@enterprise-rag.com","password":"admin123"}'

# Expected: JSON response with JWT token
# {"token":"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...","expiresIn":3600}
```

### 3. Test Document Upload

```bash
# Save JWT token
export JWT_TOKEN="<token-from-login>"

# Port-forward document service
kubectl port-forward -n rag-system svc/rag-document-service 8082:8082 &

# Upload test document
echo "This is a test document." > test.txt

curl -X POST http://localhost:8082/api/v1/documents/upload \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -F "file=@test.txt"

# Expected: JSON response with document ID
# {"id":"doc-12345","filename":"test.txt","status":"uploaded"}
```

### 4. Test RAG Query

```bash
# Port-forward core service
kubectl port-forward -n rag-system svc/rag-core-service 8084:8084 &

# Query
curl -X POST http://localhost:8084/api/v1/query \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"query":"What is in the test document?","maxResults":5}'

# Expected: JSON response with query results
# {"results":[{"documentId":"doc-12345","relevanceScore":0.95,...}]}
```

### 5. Database Verification

```bash
# Get auth pod name
AUTH_POD=$(kubectl get pods -n rag-system -l app=rag-auth -o jsonpath='{.items[0].metadata.name}')

# Install psql in pod
kubectl exec -n rag-system $AUTH_POD -- \
  sh -c "apt-get update -qq && apt-get install -y -qq postgresql-client"

# Get database password
DB_PASSWORD=$(kubectl get secret postgres-credentials -n rag-system \
  -o jsonpath='{.data.password}' | base64 -d)

# Query database
kubectl exec -n rag-system $AUTH_POD -- \
  sh -c "PGPASSWORD='$DB_PASSWORD' psql -h 127.0.0.1 -U rag_user -d rag_db \
  -c 'SELECT email, is_active FROM users;'"

# Expected:
#            email            | is_active
# ---------------------------+-----------
#  admin@enterprise-rag.com  | t
```

### 6. Swagger UI Access

```bash
# Port-forward auth service
kubectl port-forward -n rag-system svc/rag-auth-service 8081:8081

# Open in browser
open http://localhost:8081/swagger-ui.html
```

Navigate through API documentation and test endpoints interactively.

---

## Troubleshooting

### Issue: Pods Stuck in Pending

**Symptom**: Pods show `Pending` status for > 5 minutes

**Diagnosis**:
```bash
kubectl describe pod -n rag-system <pod-name>
```

**Common causes**:
1. **Insufficient resources**: Node pool has no available CPU/memory
   ```bash
   kubectl top nodes
   kubectl describe nodes
   ```
   **Solution**: Scale up node pool or add more nodes

2. **Image pull failure**: Cannot pull container image
   ```bash
   kubectl get events -n rag-system | grep "Failed to pull image"
   ```
   **Solution**: Verify images exist in Artifact Registry, check IAM permissions

3. **PVC not bound**: Persistent volume claim unbound
   ```bash
   kubectl get pvc -n rag-system
   ```
   **Solution**: Check StorageClass configuration

### Issue: Pods CrashLoopBackOff

**Symptom**: Pods repeatedly crash and restart

**Diagnosis**:
```bash
kubectl logs -n rag-system <pod-name> --previous
kubectl describe pod -n rag-system <pod-name>
```

**Common causes**:
1. **Database connection failure**: Cannot connect to Cloud SQL
   ```bash
   kubectl logs -n rag-system <pod-name> | grep "Connection refused"
   ```
   **Solution**: Verify Cloud SQL Proxy sidecar, check secrets

2. **Missing secrets**: Required environment variables not set
   ```bash
   kubectl get secrets -n rag-system
   ```
   **Solution**: Run `./scripts/gcp/13-sync-secrets-to-k8s.sh`

3. **Application error**: Java exception during startup
   ```bash
   kubectl logs -n rag-system <pod-name> | grep "Exception"
   ```
   **Solution**: Review application logs, check configuration

### Issue: Service Not Accessible

**Symptom**: Cannot curl service endpoint

**Diagnosis**:
```bash
kubectl get svc -n rag-system
kubectl get endpoints -n rag-system
```

**Common causes**:
1. **No endpoints**: Service has no healthy pods
   ```bash
   kubectl get pods -n rag-system -l app=<service-name>
   ```
   **Solution**: Fix pod health issues first

2. **Wrong port**: Using incorrect service port
   ```bash
   kubectl describe svc <service-name> -n rag-system
   ```
   **Solution**: Verify port mappings (8081-8085)

3. **Network policy**: Blocking traffic between pods
   ```bash
   kubectl get networkpolicies -n rag-system
   ```
   **Solution**: Review network policies

### Issue: Authentication Fails

**Symptom**: Login returns "Invalid credentials"

**Diagnosis**:
```bash
# Check admin user in database
kubectl exec -n rag-system $AUTH_POD -- \
  sh -c "PGPASSWORD='$DB_PASSWORD' psql -h 127.0.0.1 -U rag_user -d rag_db \
  -c 'SELECT email, password_hash, is_active FROM users WHERE email='\''admin@enterprise-rag.com'\'';'"
```

**Common causes**:
1. **Wrong password hash**: BCrypt hash doesn't match
   **Solution**: Run `./scripts/gcp/18-init-database.sh` to update hash

2. **User not created**: Admin user missing from database
   **Solution**: Run database initialization script

3. **Token generation failure**: JWT secret not configured
   ```bash
   kubectl get secret gcp-secrets -n rag-system -o jsonpath='{.data.jwt-secret}' | base64 -d
   ```
   **Solution**: Verify JWT secret exists

### Issue: Slow Query Response

**Symptom**: RAG queries take > 10 seconds

**Diagnosis**:
```bash
# Check pod CPU/memory usage
kubectl top pods -n rag-system

# Check embedding service logs
kubectl logs -n rag-system -l app=rag-embedding --tail=100
```

**Common causes**:
1. **Embeddings not generated**: Documents not indexed
   **Solution**: Wait for embedding generation, check logs

2. **Insufficient resources**: Pods CPU-throttled
   **Solution**: Increase resource requests/limits

3. **Cold start**: First query after deployment
   **Solution**: Normal, subsequent queries faster

---

## Rollback Procedures

### Rollback Deployment

If deployment fails or causes issues:

```bash
# Rollback specific service
kubectl rollout undo deployment rag-auth-service -n rag-system

# Rollback all services (if applied via kustomize)
kubectl apply -k k8s/overlays/dev --prune

# Check rollout status
kubectl rollout status deployment -n rag-system
```

### Rollback Database Migrations

```bash
# Flyway doesn't support automatic rollback
# Manual steps required:

# 1. Identify migration to revert
kubectl exec -n rag-system $AUTH_POD -- \
  sh -c "PGPASSWORD='$DB_PASSWORD' psql -h 127.0.0.1 -U rag_user -d rag_db \
  -c 'SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;'"

# 2. Manual SQL to revert (if migration has down script)
kubectl exec -n rag-system $AUTH_POD -- \
  sh -c "PGPASSWORD='$DB_PASSWORD' psql -h 127.0.0.1 -U rag_user -d rag_db \
  -f /path/to/down-migration.sql"
```

### Complete Teardown

To completely remove deployment:

```bash
# Delete all resources in namespace
kubectl delete all --all -n rag-system

# Delete secrets
kubectl delete secrets --all -n rag-system

# Delete PVCs (caution: data loss)
kubectl delete pvc --all -n rag-system

# Delete namespace (caution: complete removal)
kubectl delete namespace rag-system
```

---

## Production Considerations

### 1. Resource Sizing

**Development** (default):
- CPU: 500m request, 1000m limit
- Memory: 1Gi request, 2Gi limit
- Replicas: 1 per service

**Production** (recommended):
- CPU: 1000m request, 2000m limit
- Memory: 2Gi request, 4Gi limit
- Replicas: 3 per service (for HA)

Update in `k8s/overlays/prod/kustomization.yaml`

### 2. Auto-Scaling

Enable Horizontal Pod Autoscaler:

```bash
# Already configured in k8s/base/hpa.yaml
# Scales based on CPU (target 70%) and memory (target 80%)

kubectl get hpa -n rag-system
```

### 3. Monitoring and Alerting

Configure Cloud Monitoring:

```bash
# View metrics
gcloud monitoring dashboards list
gcloud monitoring dashboards describe <dashboard-id>

# Create alerts (see docs/operations/MONITORING_SETUP.md)
```

### 4. Backup Strategy

**Database backups** (Cloud SQL automated):
- Daily automated backups (7-day retention)
- Point-in-time recovery enabled
- Cross-region replication for prod

**Document backups** (Cloud Storage):
- Versioning enabled on buckets
- Lifecycle policies (90-day retention)
- Cross-region replication for prod

**Kubernetes backups** (Velero recommended):
```bash
# Install Velero
kubectl apply -f k8s/velero/

# Create backup
velero backup create rag-system-backup --include-namespaces rag-system

# Restore
velero restore create --from-backup rag-system-backup
```

### 5. Security Hardening

- ✅ Enable Workload Identity (configured in GCP-GKE-007)
- ✅ Use Secret Manager for secrets (configured in GCP-SECRETS-002)
- ✅ Enable Cloud Armor WAF (configured in GCP-INGRESS-010)
- ✅ Network policies (default deny)
- ✅ Pod security policies (restricted)
- ⚠️ Change default admin password
- ⚠️ Rotate JWT secrets regularly
- ⚠️ Enable audit logging

### 6. CI/CD Integration

Deploy via automation:

```bash
# GitHub Actions workflow (example)
- name: Deploy to GKE
  run: |
    gcloud container clusters get-credentials rag-cluster-prod --region us-central1
    kubectl apply -k k8s/overlays/prod
    kubectl rollout status deployment -n rag-system

# Cloud Build trigger
gcloud builds submit --config=cloudbuild.yaml
```

### 7. Cost Optimization

Monitor costs:

```bash
# View GKE cluster costs
gcloud billing accounts list
gcloud billing budgets list --billing-account=<account-id>

# Optimize:
# - Use preemptible nodes for non-critical workloads
# - Enable cluster autoscaler
# - Use regional persistent disks (cheaper than zonal SSD)
# - Set resource quotas
```

---

## Next Steps

After successful deployment:

1. **Run Integration Tests**:
   ```bash
   cd rag-integration-tests
   mvn test
   ```

2. **Load Test** (optional):
   ```bash
   # Use k6, Locust, or JMeter
   k6 run load-test.js
   ```

3. **Security Scan**:
   ```bash
   # Scan images
   gcloud artifacts docker images scan <image>
   
   # Scan cluster
   gcloud container clusters describe rag-cluster-dev --enable-security-posture
   ```

4. **Documentation**:
   - Update runbooks with production-specific details
   - Document any custom configurations
   - Create incident response procedures

5. **Training**:
   - Train operations team on troubleshooting
   - Review monitoring dashboards
   - Practice rollback procedures

---

## Support and References

- **Internal Documentation**: [docs/](../README.md)
- **GCP Documentation**: https://cloud.google.com/docs
- **Kubernetes Documentation**: https://kubernetes.io/docs
- **Spring Boot Reference**: https://docs.spring.io/spring-boot/docs/current/reference/html/

For assistance, contact:
- **DevOps Team**: devops@example.com
- **On-Call**: Slack #rag-oncall
- **Incidents**: Create issue in GitHub repository

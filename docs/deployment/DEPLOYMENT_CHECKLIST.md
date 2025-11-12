---
version: 1.0.0
last-updated: 2025-11-12
status: active
applies-to: 0.8.0-SNAPSHOT
category: deployment
---

# GCP Deployment Checklist

## Pre-Deployment (Completed ✅)

- [x] **GCP-INFRA-001**: GCP Project Setup
  - [x] Project created with billing enabled
  - [x] Required APIs enabled
  - [x] IAM service accounts created
  - [x] VPC network configured
  - [x] Cloud NAT configured
  - [x] Budget alerts configured

- [x] **GCP-SECRETS-002**: Secret Manager Migration
  - [x] Secrets created in Secret Manager
  - [x] IAM permissions configured
  - [x] Secrets synced to GKE cluster

- [x] **GCP-REGISTRY-003**: Container Registry
  - [x] Artifact Registry repository created
  - [x] IAM permissions configured
  - [x] Docker authentication configured

- [x] **GCP-SQL-004**: Cloud SQL PostgreSQL
  - [x] Cloud SQL instance created
  - [x] Database and user created
  - [x] Private IP configured
  - [x] Backups enabled
  - [x] High availability enabled (prod)

- [x] **GCP-REDIS-005**: Cloud Memorystore Redis
  - [x] Redis instance created
  - [x] VPC peering configured
  - [x] Memory size configured

- [x] **GCP-KAFKA-006**: Kafka/Pub-Sub Planning
  - [x] Pub/Sub topics created
  - [x] Subscriptions configured
  - [x] IAM permissions set

- [x] **GCP-GKE-007**: GKE Cluster
  - [x] GKE cluster created
  - [x] Node pools configured
  - [x] Workload Identity enabled
  - [x] NGINX Ingress Controller installed
  - [x] cert-manager installed
  - [x] Service accounts configured

- [x] **GCP-K8S-008**: Kubernetes Manifests
  - [x] Namespace created
  - [x] ServiceAccounts created
  - [x] ConfigMaps created
  - [x] Deployment manifests for all 5 services
  - [x] Service manifests
  - [x] HPA configurations

- [x] **GCP-STORAGE-009**: Persistent Storage
  - [x] Cloud Storage buckets created
  - [x] StorageClasses configured
  - [x] Volume snapshot automation
  - [x] Backup procedures documented

- [x] **GCP-INGRESS-010**: Ingress & Load Balancer
  - [x] BackendConfig created
  - [x] Ingress manifest with SSL
  - [x] Cloud Armor security policy
  - [x] Static IP reserved
  - [x] DNS configured

---

## Deployment Day Checklist

### Phase 1: Build and Push Images (30-45 minutes)

- [ ] **Build all container images**
  ```bash
  make gcp-build ENV=dev
  # Or: ./scripts/gcp/07-build-and-push-images.sh --env dev
  ```

- [ ] **Verify images in Artifact Registry**
  ```bash
  gcloud artifacts docker images list \
    us-central1-docker.pkg.dev/byo-rag-dev/rag-system
  ```
  - [ ] rag-auth-service:latest
  - [ ] rag-admin-service:latest
  - [ ] rag-document-service:latest
  - [ ] rag-embedding-service:latest
  - [ ] rag-core-service:latest

**Checkpoint**: All 5 images present in Artifact Registry

---

### Phase 2: Deploy Services to GKE (15-20 minutes)

- [ ] **Verify cluster access**
  ```bash
  gcloud container clusters get-credentials rag-cluster-dev \
    --region us-central1 --project byo-rag-dev
  kubectl cluster-info
  ```

- [ ] **Verify secrets synced**
  ```bash
  kubectl get secrets -n rag-system
  ```
  - [ ] gcp-secrets
  - [ ] postgres-credentials
  - [ ] redis-credentials

- [ ] **Deploy all services**
  ```bash
  make gcp-deploy ENV=dev
  # Or: ./scripts/gcp/17-deploy-services.sh --env dev
  ```

- [ ] **Verify pod startup**
  ```bash
  kubectl get pods -n rag-system -w
  ```
  Wait for all pods to reach `Running` status (1/1 Ready)
  
  - [ ] rag-auth-service pod running
  - [ ] rag-admin-service pod running
  - [ ] rag-document-service pod running
  - [ ] rag-embedding-service pod running
  - [ ] rag-core-service pod running

- [ ] **Check pod logs for errors**
  ```bash
  kubectl logs -n rag-system -l app=rag-auth --tail=50
  kubectl logs -n rag-system -l app=rag-core --tail=50
  ```

**Checkpoint**: All 5 services running with healthy pods

---

### Phase 3: Initialize Database (5-10 minutes)

- [ ] **Run database initialization**
  ```bash
  make gcp-init-db ENV=dev
  # Or: ./scripts/gcp/18-init-database.sh --env dev
  ```

- [ ] **Verify migrations completed**
  - [ ] Flyway migration logs show success
  - [ ] No database connection errors

- [ ] **Verify admin user created**
  ```bash
  # Check database directly
  kubectl exec -n rag-system <auth-pod> -- \
    sh -c "PGPASSWORD='<password>' psql -h 127.0.0.1 -U rag_user -d rag_db \
    -c 'SELECT email, is_active FROM users;'"
  ```
  - [ ] admin@enterprise-rag.com exists
  - [ ] is_active = true

- [ ] **Test admin authentication**
  ```bash
  kubectl port-forward -n rag-system svc/rag-auth-service 8081:8081 &
  curl -X POST http://localhost:8081/api/v1/auth/login \
    -H 'Content-Type: application/json' \
    -d '{"email":"admin@enterprise-rag.com","password":"admin123"}'
  ```
  - [ ] Returns JWT token
  - [ ] No "Invalid credentials" error

**Checkpoint**: Database initialized, admin user can authenticate

---

### Phase 4: Validate Deployment (10-15 minutes)

- [ ] **Run comprehensive validation**
  ```bash
  make gcp-validate ENV=dev
  # Or: ./scripts/gcp/19-validate-deployment.sh --env dev
  ```

- [ ] **Pod Status Check**
  - [ ] All pods running and ready
  - [ ] No CrashLoopBackOff pods
  - [ ] No OOMKilled pods

- [ ] **Service Endpoint Check**
  - [ ] All services have endpoints
  - [ ] ClusterIP services accessible internally

- [ ] **Health Endpoint Check**
  - [ ] All liveness checks passing
  - [ ] All readiness checks passing

- [ ] **Connectivity Tests**
  - [ ] Inter-service DNS resolution works
  - [ ] HTTP connectivity between services works

- [ ] **Database Connectivity**
  - [ ] Cloud SQL connection via proxy working
  - [ ] Queries successful

- [ ] **Redis Connectivity** (if configured)
  - [ ] Memorystore connection working

- [ ] **Resource Usage**
  - [ ] No pods CPU-throttled
  - [ ] No memory pressure warnings

**Checkpoint**: All validation tests passing (>80% success rate acceptable)

---

### Phase 5: Functional Testing (15-20 minutes)

- [ ] **Test Authentication**
  ```bash
  kubectl port-forward -n rag-system svc/rag-auth-service 8081:8081
  ```
  - [ ] Login endpoint works
  - [ ] JWT token generated
  - [ ] Token contains correct claims

- [ ] **Test Swagger UI**
  - [ ] http://localhost:8081/swagger-ui.html accessible
  - [ ] API documentation loads
  - [ ] Can test endpoints interactively

- [ ] **Test Document Upload**
  ```bash
  kubectl port-forward -n rag-system svc/rag-document-service 8082:8082
  ```
  - [ ] Upload test document
  - [ ] Returns document ID
  - [ ] No errors in logs

- [ ] **Test RAG Query**
  ```bash
  kubectl port-forward -n rag-system svc/rag-core-service 8084:8084
  ```
  - [ ] Query endpoint responds
  - [ ] Returns results (may be empty initially)
  - [ ] No exceptions in logs

**Checkpoint**: Core functionality working end-to-end

---

### Phase 6: Ingress Setup (Optional, 15-20 minutes)

- [ ] **Reserve static IP and setup DNS**
  ```bash
  make gcp-setup-ingress ENV=dev DOMAIN=rag-dev.example.com
  # Or: ./scripts/gcp/16-setup-ingress.sh --env dev --domain rag-dev.example.com
  ```

- [ ] **Verify static IP created**
  ```bash
  gcloud compute addresses describe rag-ingress-ip-dev --global
  ```

- [ ] **Verify Cloud DNS configured**
  ```bash
  gcloud dns record-sets list --zone=rag-zone-dev
  ```
  - [ ] A record for rag.example.com
  - [ ] A record for api.rag.example.com
  - [ ] A record for admin.rag.example.com

- [ ] **Verify Cloud Armor policy**
  ```bash
  gcloud compute security-policies describe rag-security-policy
  ```
  - [ ] SQL injection rule
  - [ ] XSS rule
  - [ ] RCE rule
  - [ ] Rate limiting rule

- [ ] **Deploy ingress manifest**
  ```bash
  kubectl apply -f k8s/base/ingress.yaml
  ```

- [ ] **Wait for SSL certificate**
  ```bash
  kubectl get certificate -n rag-system -w
  ```
  Wait for certificate to show `True` in READY column (5-10 minutes)

- [ ] **Test HTTPS access**
  ```bash
  curl -I https://rag-dev.example.com
  curl https://api.rag-dev.example.com/api/v1/auth/health
  ```

**Checkpoint**: External HTTPS access working

---

## Post-Deployment Tasks

### Monitoring Setup

- [ ] **Create Cloud Monitoring dashboards**
  - [ ] Pod CPU/Memory usage
  - [ ] Request latency
  - [ ] Error rates
  - [ ] Database connections

- [ ] **Configure alerts**
  - [ ] High pod CPU (>80%)
  - [ ] High pod memory (>85%)
  - [ ] Pod restart count >5
  - [ ] SSL certificate expiring <30 days
  - [ ] High error rate (>5%)

- [ ] **Set up uptime checks**
  - [ ] Main domain health check
  - [ ] API health check
  - [ ] Admin health check

### Security Hardening

- [ ] **Change default passwords**
  ```bash
  # Update admin password via API
  curl -X POST https://api.rag-dev.example.com/api/v1/auth/change-password \
    -H "Authorization: Bearer $JWT_TOKEN" \
    -d '{"oldPassword":"admin123","newPassword":"<new-secure-password>"}'
  ```

- [ ] **Rotate JWT secrets**
  - [ ] Generate new JWT secret
  - [ ] Update Secret Manager
  - [ ] Restart auth service

- [ ] **Review IAM permissions**
  - [ ] Principle of least privilege
  - [ ] Remove unused service accounts
  - [ ] Enable audit logging

### Documentation

- [ ] **Update runbooks**
  - [ ] Document actual domain names
  - [ ] Document IP addresses
  - [ ] Update troubleshooting steps

- [ ] **Create incident response plan**
  - [ ] On-call rotation
  - [ ] Escalation procedures
  - [ ] Rollback procedures

- [ ] **Train operations team**
  - [ ] Walk through troubleshooting
  - [ ] Practice rollback
  - [ ] Review monitoring dashboards

### Backup Validation

- [ ] **Verify Cloud SQL backups**
  ```bash
  gcloud sql backups list --instance=rag-postgres-dev
  ```

- [ ] **Test database restore** (in dev environment)
  ```bash
  gcloud sql backups restore <backup-id> --backup-instance=rag-postgres-dev
  ```

- [ ] **Verify Cloud Storage backups**
  ```bash
  gsutil ls -r gs://rag-backups-dev/
  ```

- [ ] **Test volume snapshot restore**
  ```bash
  ./scripts/gcp/15-manage-snapshots.sh restore <snapshot-name> test-pvc
  ```

### Performance Testing

- [ ] **Run load tests**
  - [ ] Authentication endpoint (100 req/s)
  - [ ] Document upload (10 concurrent)
  - [ ] RAG query (50 req/s)

- [ ] **Verify auto-scaling**
  - [ ] HPA triggers at 70% CPU
  - [ ] Pods scale up under load
  - [ ] Pods scale down when idle

- [ ] **Measure response times**
  - [ ] Auth login <500ms
  - [ ] Document upload <2s (10MB file)
  - [ ] RAG query <3s

---

## Production Promotion (When Deploying to Prod)

- [ ] **Review prod configurations**
  ```bash
  cat k8s/overlays/prod/kustomization.yaml
  ```
  - [ ] Replicas: 3 per service
  - [ ] Resources: 1 CPU / 2Gi memory
  - [ ] Node pool: production tier
  - [ ] HPA: enabled

- [ ] **Update environment variables**
  - [ ] PROJECT_ID: byo-rag-prod
  - [ ] REGION: us-central1
  - [ ] ARTIFACT_REGISTRY: prod registry

- [ ] **Deploy to production**
  ```bash
  make gcp-deploy-all ENV=prod
  ```

- [ ] **Smoke test production**
  - [ ] All pods running
  - [ ] Admin authentication works
  - [ ] Upload test document
  - [ ] Execute test query

- [ ] **Enable production monitoring**
  - [ ] Stricter SLOs (99.9% uptime)
  - [ ] Page-worthy alerts only
  - [ ] On-call rotation active

---

## Success Criteria

✅ **Deployment is considered successful when**:

1. ✅ All 5 services deployed and running
2. ✅ All pods healthy (1/1 Ready)
3. ✅ Database initialized with admin user
4. ✅ Admin authentication working
5. ✅ All validation tests passing (>95%)
6. ✅ Functional testing complete (auth, upload, query)
7. ✅ Monitoring dashboards configured
8. ✅ SSL certificates provisioned (if using ingress)
9. ✅ No critical errors in logs
10. ✅ Operations team trained

---

## Rollback Procedures

If deployment fails or causes issues:

### Rollback Service Deployment

```bash
# Rollback specific service
kubectl rollout undo deployment rag-auth-service -n rag-system

# Check rollback status
kubectl rollout status deployment rag-auth-service -n rag-system
```

### Rollback Database (Manual)

```bash
# Identify migrations to revert
kubectl exec -n rag-system <auth-pod> -- \
  sh -c "PGPASSWORD='<password>' psql -h 127.0.0.1 -U rag_user -d rag_db \
  -c 'SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC;'"

# Manually revert if needed (no automatic rollback)
```

### Complete Teardown

```bash
# Delete all resources (CAUTION)
make gcp-cleanup ENV=dev

# Confirm deletion
kubectl get all -n rag-system
# Should return: No resources found
```

---

## Contact Information

**For assistance during deployment:**

- **DevOps Team**: devops@example.com
- **On-Call**: Slack #rag-oncall
- **Incidents**: Create GitHub issue with label `P0-Critical`

**Escalation:**
- Level 1: DevOps Engineer (5 min response)
- Level 2: Senior DevOps / Lead Engineer (15 min response)
- Level 3: Engineering Manager (30 min response)

---

## Appendix: Useful Commands

### Check Deployment Status
```bash
make gcp-status ENV=dev
kubectl get all -n rag-system
kubectl top pods -n rag-system
```

### View Logs
```bash
make gcp-logs ENV=dev SERVICE=rag-auth
kubectl logs -n rag-system -l app=rag-auth --tail=100 --follow
```

### Port-Forward Services
```bash
make gcp-port-forward ENV=dev SERVICE=rag-auth
kubectl port-forward -n rag-system svc/rag-auth-service 8081:8081
```

### Restart Service
```bash
make gcp-restart ENV=dev SERVICE=rag-auth
kubectl rollout restart deployment rag-auth-service -n rag-system
```

### Execute Commands in Pod
```bash
kubectl exec -it -n rag-system <pod-name> -- /bin/bash
```

### Check Ingress
```bash
kubectl get ingress -n rag-system
kubectl describe ingress rag-ingress -n rag-system
```

### Check SSL Certificate
```bash
kubectl get certificate -n rag-system
kubectl describe certificate rag-tls-cert -n rag-system
```

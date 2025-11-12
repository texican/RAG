---
version: 1.0.0
last-updated: 2025-11-12
status: active
applies-to: 0.8.0-SNAPSHOT
category: security
---

# Secret Rotation Procedures

**Document Version:** 1.0
**Last Updated:** 2025-11-06
**Owner:** Platform Security Team

## Overview

This document outlines procedures for rotating secrets in the RAG Enterprise system. All secrets are managed through Google Secret Manager and must be rotated regularly to maintain security posture.

## Secret Inventory

### Production Secrets

| Secret Name | Type | Rotation Frequency | Impact Scope |
|-------------|------|-------------------|--------------|
| `postgres-password` | Database Password | 90 days | All services accessing PostgreSQL |
| `redis-password` | Cache Password | 90 days | All services accessing Redis |
| `jwt-secret` | Signing Key | 180 days | All authentication tokens invalidated |
| `openai-api-key` | API Key | As needed | Embedding service only |

### Service Account Keys

| Service Account | Purpose | Rotation Frequency |
|-----------------|---------|-------------------|
| `gke-node-sa` | GKE node access to secrets | 90 days |
| `cloudsql-proxy-sa` | Cloud SQL proxy | 90 days |
| `artifact-registry-sa` | Container image publishing | 90 days |

---

## Rotation Procedures

### 1. PostgreSQL Password Rotation

**Impact:** All services will lose database connectivity during rotation.

**Downtime:** ~5 minutes (rolling restart)

**Prerequisites:**
- Maintenance window scheduled
- All services configured for graceful shutdown
- Database backups verified

**Procedure:**

```bash
# 1. Generate new password
NEW_PASSWORD=$(openssl rand -base64 24)

# 2. Update PostgreSQL user password
gcloud sql users set-password rag_user \
  --instance=rag-postgres \
  --password="$NEW_PASSWORD"

# 3. Update Secret Manager
echo -n "$NEW_PASSWORD" | gcloud secrets versions add postgres-password \
  --data-file=-

# 4. Restart services (Kubernetes rolling restart)
kubectl rollout restart deployment/rag-auth-service
kubectl rollout restart deployment/rag-document-service
kubectl rollout restart deployment/rag-core-service

# 5. Verify all services are healthy
kubectl get pods
kubectl logs -l app=rag-auth-service --tail=50

# 6. Test database connectivity
curl -X GET http://<service-endpoint>/actuator/health
```

**Verification:**
- All services show healthy status
- Database connections successful
- No authentication errors in logs

**Rollback:**
If issues occur, revert to previous secret version:
```bash
# Get previous version
PREVIOUS_VERSION=$(gcloud secrets versions list postgres-password --limit=2 --format="value(name)" | tail -1)

# Disable current version
gcloud secrets versions disable latest --secret=postgres-password

# Enable previous version
gcloud secrets versions enable "$PREVIOUS_VERSION" --secret=postgres-password

# Restart services
kubectl rollout restart deployment -l app=rag
```

---

### 2. Redis Password Rotation

**Impact:** Session cache cleared, temporary performance degradation.

**Downtime:** ~2 minutes

**Prerequisites:**
- Low traffic period
- Session persistence not critical

**Procedure:**

```bash
# 1. Generate new password
NEW_PASSWORD=$(openssl rand -base64 24)

# 2. Update Secret Manager
echo -n "$NEW_PASSWORD" | gcloud secrets versions add redis-password \
  --data-file=-

# 3. Update Redis instance (Cloud Memorystore)
gcloud redis instances update rag-redis \
  --region=us-central1 \
  --update-labels=password-rotated=$(date +%Y%m%d)

# Note: Cloud Memorystore requires recreation for password change
# Alternative: Use Redis AUTH command if self-managed

# 4. Restart services
kubectl rollout restart deployment -l uses-redis=true

# 5. Verify connectivity
redis-cli -h <redis-host> -a "$NEW_PASSWORD" PING
```

**Verification:**
- Redis responds to PING
- Services connect successfully
- Cache operations working

---

### 3. JWT Secret Rotation

**Impact:** ALL existing JWTs invalidated. Users must re-login.

**Downtime:** None (but all sessions terminated)

**Prerequisites:**
- Notification sent to all users 24h in advance
- Support team on standby
- Scheduled during low-usage period

**Procedure:**

```bash
# 1. Generate new JWT secret (256-bit)
NEW_SECRET=$(openssl rand -base64 32)

# 2. Update Secret Manager
echo -n "$NEW_SECRET" | gcloud secrets versions add jwt-secret \
  --data-file=-

# 3. Restart auth service FIRST
kubectl rollout restart deployment/rag-auth-service

# Wait for auth service to be ready
kubectl rollout status deployment/rag-auth-service

# 4. Restart all other services
kubectl rollout restart deployment/rag-core-service
kubectl rollout restart deployment/rag-document-service
kubectl rollout restart deployment/rag-admin-service

# 5. Monitor error rates
kubectl logs -l app=rag-auth-service --tail=100 | grep ERROR
```

**Verification:**
- New login requests succeed
- Old tokens return 401 Unauthorized
- Token validation working correctly

**User Communication:**
```
Subject: Scheduled JWT Secret Rotation - Action Required

Dear RAG Enterprise Users,

We will be rotating our JWT signing keys on [DATE] at [TIME].

What this means:
- You will be logged out of all sessions
- You will need to log in again after the rotation
- Active API tokens will be invalidated

Expected duration: 5 minutes

Thank you for your understanding.
```

---

### 4. OpenAI API Key Rotation

**Impact:** Embedding generation paused briefly.

**Downtime:** <1 minute

**When to Rotate:**
- Suspected key compromise
- Key leaked in code/logs
- 90-day routine rotation
- Billing concerns

**Procedure:**

```bash
# 1. Create new key in OpenAI dashboard
# Visit: https://platform.openai.com/api-keys
# Click "Create new secret key"
# Copy the new key

NEW_KEY="sk-proj-..."

# 2. Test new key
curl https://api.openai.com/v1/models \
  -H "Authorization: Bearer $NEW_KEY"

# 3. Update Secret Manager
echo -n "$NEW_KEY" | gcloud secrets versions add openai-api-key \
  --data-file=-

# 4. Restart embedding service
kubectl rollout restart deployment/rag-embedding-service

# 5. Verify embedding generation
curl -X POST http://<embedding-service>/api/v1/embeddings/generate \
  -H 'Content-Type: application/json' \
  -H 'X-Tenant-ID: <tenant-id>' \
  -d '{"texts":["test"]}'

# 6. Delete old key from OpenAI dashboard
# Visit: https://platform.openai.com/api-keys
# Find old key, click "Delete"
```

**Verification:**
- Embedding requests return 200 OK
- OpenAI API calls successful
- No 401 errors in logs
- Old key deleted from OpenAI dashboard

---

### 5. Service Account Key Rotation

**Impact:** Depends on service account usage.

**Frequency:** Every 90 days (automatic via Workload Identity recommended)

**Procedure:**

```bash
# 1. Create new service account key
SA_EMAIL="gke-node-sa@$PROJECT_ID.iam.gserviceaccount.com"

gcloud iam service-accounts keys create new-key.json \
  --iam-account="$SA_EMAIL"

# 2. Update Kubernetes secret
kubectl create secret generic gke-node-sa-key \
  --from-file=key.json=new-key.json \
  --dry-run=client -o yaml | kubectl apply -f -

# 3. Restart affected pods
kubectl rollout restart deployment -l uses-service-account=true

# 4. Verify services work with new key
kubectl get pods
kubectl logs -l uses-service-account=true --tail=50

# 5. List old keys
gcloud iam service-accounts keys list \
  --iam-account="$SA_EMAIL"

# 6. Delete old key
OLD_KEY_ID="<key-id-from-list>"
gcloud iam service-accounts keys delete "$OLD_KEY_ID" \
  --iam-account="$SA_EMAIL"

# 7. Clean up local key file
shred -u new-key.json
```

**Best Practice:**
Use Workload Identity instead of service account keys to avoid rotation entirely:
```bash
# Enable Workload Identity
gcloud container clusters update rag-cluster \
  --workload-pool=$PROJECT_ID.svc.id.goog

# Bind Kubernetes SA to GCP SA
kubectl create serviceaccount rag-ksa
gcloud iam service-accounts add-iam-policy-binding \
  $SA_EMAIL \
  --member="serviceAccount:$PROJECT_ID.svc.id.goog[default/rag-ksa]" \
  --role="roles/iam.workloadIdentityUser"
```

---

## Emergency Rotation (Compromise)

### Immediate Actions (Within 1 Hour)

1. **Disable compromised secret version:**
   ```bash
   gcloud secrets versions disable latest --secret=<secret-name>
   ```

2. **Create and deploy new secret:**
   ```bash
   # Generate new secret
   NEW_SECRET=$(openssl rand -base64 32)

   # Add to Secret Manager
   echo -n "$NEW_SECRET" | gcloud secrets versions add <secret-name> \
     --data-file=-

   # Force immediate restart (not rolling)
   kubectl delete pods -l app=<affected-service>
   ```

3. **Review access logs:**
   ```bash
   # Check who accessed the secret
   gcloud logging read "resource.type=secretmanager_secret AND resource.labels.secret_id=<secret-name>" \
     --limit=100 \
     --format=json
   ```

4. **Notify security team and stakeholders**

### Follow-up Actions (Within 24 Hours)

1. Root cause analysis
2. Review and update access policies
3. Check for unauthorized access/usage
4. Update incident response documentation
5. Schedule post-incident review

---

## Automation

### Automated Rotation Script

```bash
#!/bin/bash
# Location: scripts/gcp/rotate-secret.sh

set -euo pipefail

SECRET_NAME=$1
SECRET_TYPE=$2

case $SECRET_TYPE in
  password)
    NEW_VALUE=$(openssl rand -base64 24)
    ;;
  jwt)
    NEW_VALUE=$(openssl rand -base64 32)
    ;;
  *)
    echo "Unknown secret type: $SECRET_TYPE"
    exit 1
    ;;
esac

# Add new version
echo -n "$NEW_VALUE" | gcloud secrets versions add "$SECRET_NAME" \
  --data-file=-

# Tag with rotation metadata
gcloud secrets update "$SECRET_NAME" \
  --update-labels="last-rotated=$(date +%Y%m%d),rotated-by=automated"

echo "✓ Rotated $SECRET_NAME"
```

### Scheduled Rotation (Cloud Scheduler)

```bash
# Create Cloud Scheduler job for automatic rotation
gcloud scheduler jobs create http rotate-postgres-password \
  --schedule="0 0 1 */3 *" \
  --uri="https://us-central1-$PROJECT_ID.cloudfunctions.net/rotate-secret" \
  --message-body='{"secret":"postgres-password","type":"password"}' \
  --time-zone="America/Los_Angeles"
```

---

## Monitoring and Alerts

### Secret Age Monitoring

```yaml
# Cloud Monitoring Alert Policy
displayName: "Secret Rotation Overdue"
conditions:
  - conditionThreshold:
      filter: |
        resource.type="secretmanager_secret"
        metric.type="secretmanager.googleapis.com/secret/version/age"
      comparison: COMPARISON_GT
      thresholdValue: 7776000  # 90 days in seconds
      duration: 0s
mutationRecord:
  mutatedBy: "platform-team@example.com"
notificationChannels:
  - "projects/$PROJECT_ID/notificationChannels/security-team"
```

### Secret Access Monitoring

```bash
# Monitor unauthorized access attempts
gcloud logging read \
  "protoPayload.methodName=google.cloud.secretmanager.v1.SecretManagerService.AccessSecretVersion
   AND protoPayload.authenticationInfo.principalEmail!~'@$PROJECT_ID.iam.gserviceaccount.com$'" \
  --limit=50 \
  --format=json
```

---

## Compliance

### Rotation Requirements

| Compliance Standard | Requirement | Our Policy |
|---------------------|-------------|------------|
| SOC 2 | Rotate every 90 days | ✓ Implemented |
| PCI DSS | Rotate every 90 days | ✓ Implemented |
| HIPAA | Document rotation procedures | ✓ This document |
| ISO 27001 | Regular rotation schedule | ✓ Automated |

### Audit Trail

All secret rotations are logged in Cloud Audit Logs:
```bash
# View rotation history
gcloud logging read \
  "resource.type=secretmanager_secret
   AND protoPayload.methodName=google.cloud.secretmanager.v1.SecretManagerService.AddSecretVersion" \
  --limit=100 \
  --format="table(timestamp,protoPayload.authenticationInfo.principalEmail,resource.labels.secret_id)"
```

---

## Troubleshooting

### Issue: Service won't start after rotation

**Symptoms:**
- Pods in CrashLoopBackOff
- "authentication failed" errors

**Solutions:**
1. Check secret version is latest:
   ```bash
   gcloud secrets versions list <secret-name>
   ```

2. Verify IAM permissions:
   ```bash
   gcloud secrets get-iam-policy <secret-name>
   ```

3. Check pod environment:
   ```bash
   kubectl describe pod <pod-name>
   kubectl logs <pod-name>
   ```

4. Rollback to previous version:
   ```bash
   # Disable current
   gcloud secrets versions disable latest --secret=<secret-name>

   # Enable previous
   PREVIOUS=$(gcloud secrets versions list <secret-name> --limit=2 --format="value(name)" | tail -1)
   gcloud secrets versions enable "$PREVIOUS" --secret=<secret-name>
   ```

### Issue: Old secret still works

**Cause:** Services cached old secret or didn't restart.

**Solution:**
1. Force pod restart:
   ```bash
   kubectl delete pod -l app=<service>
   ```

2. Check if old secret version is still enabled:
   ```bash
   gcloud secrets versions list <secret-name>
   ```

---

## References

- [Google Secret Manager Documentation](https://cloud.google.com/secret-manager/docs)
- [Kubernetes Secrets](https://kubernetes.io/docs/concepts/configuration/secret/)
- [OWASP Secret Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html)
- [GCP Security Best Practices](https://cloud.google.com/security/best-practices)

---

## Changelog

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 1.0 | 2025-11-06 | Initial document creation | Platform Team |

---

## Contact

For questions or assistance with secret rotation:
- **Security Team:** security@example.com
- **On-Call:** Use PagerDuty escalation
- **Slack:** #platform-security

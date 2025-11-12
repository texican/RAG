---
version: 1.0.0
last-updated: 2025-11-12
status: active
applies-to: 0.8.0-SNAPSHOT
category: deployment
---

# Ingress and Load Balancer Guide for RAG System on GCP

## Overview

This guide covers external access configuration for the RAG system deployed on Google Kubernetes Engine (GKE), including:

- **NGINX Ingress Controller**: Layer 7 HTTP/HTTPS routing
- **Google Cloud Load Balancer**: Regional load balancing with health checks
- **SSL/TLS Certificates**: Automatic HTTPS via cert-manager and Let's Encrypt
- **Cloud Armor**: Web Application Firewall (WAF) and DDoS protection
- **Cloud DNS**: Domain name management and DNS records
- **Rate Limiting**: Request throttling and abuse prevention

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [NGINX Ingress Controller](#nginx-ingress-controller)
3. [SSL/TLS Configuration](#ssltls-configuration)
4. [Cloud Armor Security](#cloud-armor-security)
5. [Cloud DNS Setup](#cloud-dns-setup)
6. [Backend Configuration](#backend-configuration)
7. [Deployment Procedures](#deployment-procedures)
8. [Monitoring and Logging](#monitoring)
9. [Troubleshooting](#troubleshooting)
10. [Cost Optimization](#cost-optimization)

---

## Architecture Overview

### Traffic Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                    External Traffic Flow                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  Internet                                                             │
│     │                                                                 │
│     ▼                                                                 │
│  ┌────────────────────┐                                              │
│  │   Cloud DNS        │  DNS Resolution                              │
│  │   rag.example.com  │  → Static IP                                 │
│  └────────┬───────────┘                                              │
│           │                                                           │
│           ▼                                                           │
│  ┌────────────────────┐                                              │
│  │  Cloud Armor       │  WAF & DDoS Protection                       │
│  │  Security Policy   │  - SQL injection block                       │
│  │                    │  - XSS protection                            │
│  │                    │  - Rate limiting                             │
│  └────────┬───────────┘                                              │
│           │                                                           │
│           ▼                                                           │
│  ┌────────────────────┐                                              │
│  │  GCP Load Balancer │  Layer 7 (HTTP/HTTPS)                        │
│  │  Static IP:        │  - SSL termination                           │
│  │  35.x.x.x          │  - Health checks                             │
│  └────────┬───────────┘                                              │
│           │                                                           │
│           ▼                                                           │
│  ┌────────────────────┐                                              │
│  │ NGINX Ingress      │  Path-based Routing                          │
│  │ Controller         │  - /api/v1/auth → rag-auth                   │
│  │ (Pod in GKE)       │  - /api/v1/documents → rag-document          │
│  └────────┬───────────┘  - /api/v1/query → rag-core                 │
│           │                                                           │
│           ▼                                                           │
│  ┌─────────────────────────────────────┐                             │
│  │     Kubernetes Services             │                             │
│  ├─────────────────────────────────────┤                             │
│  │  rag-auth:8081   │  rag-core:8084   │                             │
│  │  rag-document:8082│  rag-admin:8085  │                             │
│  │  rag-embedding:8083│                 │                             │
│  └─────────────────────────────────────┘                             │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
```

### Domain Structure

| Domain | Purpose | Backend Service | Port |
|--------|---------|-----------------|------|
| `rag.example.com` | Main landing page | rag-core | 8084 |
| `api.rag.example.com/api/v1/auth` | Authentication | rag-auth | 8081 |
| `api.rag.example.com/api/v1/documents` | Document management | rag-document | 8082 |
| `api.rag.example.com/api/v1/embeddings` | Embedding generation | rag-embedding | 8083 |
| `api.rag.example.com/api/v1/query` | RAG queries | rag-core | 8084 |
| `admin.rag.example.com` | Admin interface | rag-admin | 8085 |

---

## NGINX Ingress Controller

### Overview

NGINX Ingress Controller was installed during GCP-GKE-007 cluster setup. It provides:
- Layer 7 (HTTP/HTTPS) routing
- TLS termination
- Path-based routing to services
- Request/response transformation
- Rate limiting and security headers

### Verification

```bash
# Check NGINX ingress controller status
kubectl get pods -n ingress-nginx
kubectl get svc -n ingress-nginx

# View controller logs
kubectl logs -n ingress-nginx deployment/ingress-nginx-controller -f

# Check ingress class
kubectl get ingressclass
```

### Configuration

The NGINX ingress controller is configured via annotations in `k8s/base/ingress.yaml`:

```yaml
annotations:
  # SSL/TLS
  nginx.ingress.kubernetes.io/ssl-redirect: "true"
  nginx.ingress.kubernetes.io/force-ssl-redirect: "true"
  nginx.ingress.kubernetes.io/ssl-protocols: "TLSv1.2 TLSv1.3"
  
  # Proxy settings
  nginx.ingress.kubernetes.io/proxy-body-size: "100m"  # Large file uploads
  nginx.ingress.kubernetes.io/proxy-connect-timeout: "60"
  nginx.ingress.kubernetes.io/proxy-read-timeout: "60"
  
  # Rate limiting
  nginx.ingress.kubernetes.io/rate-limit: "100"  # 100 req/min per IP
  nginx.ingress.kubernetes.io/limit-rps: "10"    # 10 req/sec per IP
  
  # Security headers
  nginx.ingress.kubernetes.io/configuration-snippet: |
    more_set_headers "X-Frame-Options: DENY";
    more_set_headers "X-Content-Type-Options: nosniff";
    more_set_headers "X-XSS-Protection: 1; mode=block";
```

---

## SSL/TLS Configuration

### cert-manager + Let's Encrypt

Automatic HTTPS certificates via cert-manager:

**ClusterIssuer (Production)**:
```yaml
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: ops@example.com
    privateKeySecretRef:
      name: letsencrypt-prod-key
    solvers:
    - http01:
        ingress:
          class: nginx
```

**Certificate Resource**:
```yaml
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: rag-tls-cert
spec:
  secretName: rag-tls-secret
  issuerRef:
    name: letsencrypt-prod
    kind: ClusterIssuer
  dnsNames:
  - rag.example.com
  - api.rag.example.com
  - admin.rag.example.com
```

### Certificate Lifecycle

1. **Initial Provisioning** (5-10 minutes):
   - cert-manager creates HTTP-01 challenge
   - Let's Encrypt verifies domain ownership
   - Certificate issued and stored in Kubernetes Secret

2. **Auto-Renewal** (every 60 days):
   - cert-manager monitors certificate expiration
   - Automatically renews 30 days before expiry
   - Zero downtime renewal

3. **Verification**:
```bash
# Check certificate status
kubectl get certificate -n rag-system
kubectl describe certificate rag-tls-cert -n rag-system

# View cert-manager logs
kubectl logs -n cert-manager deployment/cert-manager -f

# Check HTTP-01 challenges
kubectl get challenges -n rag-system
```

### Troubleshooting Certificate Issues

**Certificate Stuck in Pending**:
```bash
# Check challenge status
kubectl describe challenge -n rag-system

# Common issues:
# 1. DNS not resolving to correct IP
nslookup rag.example.com

# 2. Ingress not accessible (port 80 must be open)
curl http://rag.example.com/.well-known/acme-challenge/test

# 3. Rate limit hit (Let's Encrypt: 50 certs/week per domain)
# Solution: Use letsencrypt-staging for testing
```

---

## Cloud Armor Security

### Security Policy

Cloud Armor provides WAF and DDoS protection with preconfigured rules:

**Rule Priority System**:
```
1000  - Block SQL injection (sqli-stable)
1001  - Block XSS (xss-stable)
1002  - Block RCE (rce-stable)
2000  - Rate limiting (10,000 req/min per IP)
2147483647 - Allow all (default)
```

### Rule Configuration

Created via `scripts/gcp/16-setup-ingress.sh`:

```bash
# Create security policy
gcloud compute security-policies create rag-security-policy

# Add SQL injection protection
gcloud compute security-policies rules create 1000 \
  --security-policy=rag-security-policy \
  --expression="evaluatePreconfiguredExpr('sqli-stable')" \
  --action=deny-403

# Add rate limiting
gcloud compute security-policies rules create 2000 \
  --security-policy=rag-security-policy \
  --action=rate-based-ban \
  --rate-limit-threshold-count=10000 \
  --rate-limit-threshold-interval-sec=60 \
  --ban-duration-sec=600
```

### Custom Rules

Add custom rules for specific threats:

```bash
# Block specific IP ranges
gcloud compute security-policies rules create 1100 \
  --security-policy=rag-security-policy \
  --src-ip-ranges="1.2.3.0/24,5.6.7.8" \
  --action=deny-403 \
  --description="Block malicious IPs"

# Allow only specific countries (e.g., US, CA)
gcloud compute security-policies rules create 1200 \
  --security-policy=rag-security-policy \
  --expression="origin.region_code in ['US', 'CA']" \
  --action=allow \
  --description="Allow only US and Canada"
```

### Monitoring Security Events

```bash
# View Cloud Armor logs
gcloud logging read "resource.type=http_load_balancer AND jsonPayload.enforcedSecurityPolicy.name=rag-security-policy" --limit 50

# View blocked requests
gcloud logging read "resource.type=http_load_balancer AND jsonPayload.enforcedSecurityPolicy.outcome=DENY" --limit 50

# View rate-limited requests
gcloud logging read "resource.type=http_load_balancer AND jsonPayload.enforcedSecurityPolicy.outcome=RATE_LIMIT" --limit 50
```

---

## Cloud DNS Setup

### DNS Zone Creation

```bash
# Create DNS zone
gcloud dns managed-zones create rag-zone-dev \
  --dns-name="rag-dev.example.com." \
  --description="RAG system DNS zone for development"

# Get nameservers
gcloud dns managed-zones describe rag-zone-dev \
  --format="value(nameServers)"
```

### DNS Records

Three A records are created pointing to the static IP:

| Record | Type | Value | TTL |
|--------|------|-------|-----|
| `rag.example.com` | A | `35.x.x.x` | 300 |
| `api.rag.example.com` | A | `35.x.x.x` | 300 |
| `admin.rag.example.com` | A | `35.x.x.x` | 300 |

### External DNS Configuration

If using an external DNS provider (e.g., GoDaddy, Namecheap):

1. **Get static IP**:
   ```bash
   gcloud compute addresses describe rag-ingress-ip-dev --global \
     --format="value(address)"
   ```

2. **Create A records in your DNS provider**:
   - `rag.example.com` → `35.x.x.x`
   - `api.rag.example.com` → `35.x.x.x`
   - `admin.rag.example.com` → `35.x.x.x`

3. **Skip Cloud DNS setup**:
   ```bash
   ./scripts/gcp/16-setup-ingress.sh --env dev --domain rag.example.com --skip-dns
   ```

### DNS Propagation

```bash
# Test DNS resolution
nslookup rag.example.com
dig rag.example.com

# Check from multiple locations
curl -s "https://dns.google/resolve?name=rag.example.com&type=A" | jq .
```

---

## Backend Configuration

### BackendConfig Resource

Fine-grained control over GCP load balancer backend services:

```yaml
apiVersion: cloud.google.com/v1
kind: BackendConfig
metadata:
  name: rag-backend-config
spec:
  healthCheck:
    checkIntervalSec: 10
    timeoutSec: 5
    healthyThreshold: 2
    unhealthyThreshold: 3
    requestPath: /actuator/health/liveness
  
  connectionDraining:
    drainingTimeoutSec: 60
  
  sessionAffinity:
    affinityType: "CLIENT_IP"
    affinityCookieTtlSec: 3600
  
  securityPolicy:
    name: "rag-security-policy"
```

### Service Annotations

Link backend config to services:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: rag-auth
  annotations:
    cloud.google.com/backend-config: '{"default": "rag-backend-config"}'
```

### Health Check Configuration

**Spring Boot Health Endpoints**:
- Liveness: `/actuator/health/liveness` (used by K8s + LB)
- Readiness: `/actuator/health/readiness` (used by K8s only)

**Health Check Parameters**:
- Check Interval: 10 seconds
- Timeout: 5 seconds
- Healthy Threshold: 2 consecutive successes
- Unhealthy Threshold: 3 consecutive failures

---

## Deployment Procedures

### Initial Setup

```bash
# 1. Run ingress setup script
./scripts/gcp/16-setup-ingress.sh \
  --env dev \
  --domain rag-dev.example.com \
  --project byo-rag-dev

# 2. Update domain registrar nameservers (if using Cloud DNS)
# Point nameservers to Cloud DNS servers from script output

# 3. Deploy backend configuration
kubectl apply -f k8s/base/backendconfig.yaml

# 4. Deploy ingress
kubectl apply -f k8s/base/ingress.yaml

# 5. Verify ingress created
kubectl get ingress -n rag-system
kubectl describe ingress rag-ingress -n rag-system

# 6. Wait for certificate provisioning (5-10 minutes)
kubectl get certificate -n rag-system -w

# 7. Test HTTPS access
curl -I https://rag-dev.example.com
curl https://api.rag-dev.example.com/api/v1/auth/health
```

### Environment-Specific Deployment

**Development**:
```bash
kubectl apply -k k8s/overlays/dev
```

**Production**:
```bash
kubectl apply -k k8s/overlays/prod
```

### Updating Ingress Configuration

```bash
# Edit ingress manifest
vim k8s/base/ingress.yaml

# Apply changes
kubectl apply -f k8s/base/ingress.yaml

# Verify changes
kubectl get ingress rag-ingress -n rag-system -o yaml
```

---

## Monitoring

### Key Metrics

**Load Balancer Metrics** (Cloud Console):
- Request count
- Request latency (p50, p95, p99)
- Error rate (4xx, 5xx)
- Backend health status
- SSL handshake failures

**NGINX Metrics**:
```bash
# View NGINX metrics
kubectl port-forward -n ingress-nginx svc/ingress-nginx-controller-metrics 10254:10254
curl localhost:10254/metrics
```

**cert-manager Metrics**:
```bash
# Certificate expiration monitoring
kubectl get certificate -n rag-system -o json | \
  jq '.items[] | {name: .metadata.name, notAfter: .status.notAfter}'
```

### Cloud Monitoring Alerts

Create alerts for:

1. **SSL Certificate Expiring** (< 30 days):
```yaml
condition:
  displayName: "SSL Certificate Expiring Soon"
  metricThreshold:
    filter: |
      metric.type="certificate.googleapis.com/expiry_time"
      resource.type="k8s_pod"
    comparison: COMPARISON_LT
    thresholdValue: 2592000  # 30 days in seconds
```

2. **High Error Rate** (> 5%):
```yaml
condition:
  displayName: "High 5xx Error Rate"
  metricThreshold:
    filter: |
      metric.type="loadbalancing.googleapis.com/https/request_count"
      metric.label.response_code_class="500"
    comparison: COMPARISON_GT
    thresholdValue: 50  # 50 errors per minute
```

3. **Rate Limit Triggered**:
```yaml
condition:
  displayName: "Cloud Armor Rate Limit Triggered"
  metricThreshold:
    filter: |
      metric.type="loadbalancing.googleapis.com/https/request_count"
      metric.label.security_policy_result="RATE_LIMIT"
```

---

## Troubleshooting

### Ingress Not Accessible

**Symptoms**: Cannot access services via domain

**Diagnosis**:
```bash
# 1. Check ingress status
kubectl get ingress -n rag-system
kubectl describe ingress rag-ingress -n rag-system

# 2. Verify NGINX controller running
kubectl get pods -n ingress-nginx

# 3. Check NGINX logs
kubectl logs -n ingress-nginx deployment/ingress-nginx-controller --tail=100

# 4. Test DNS resolution
nslookup rag.example.com

# 5. Verify static IP assigned
gcloud compute addresses describe rag-ingress-ip-dev --global
```

**Solutions**:
- Ensure DNS records point to correct static IP
- Verify ingress controller has external IP: `kubectl get svc -n ingress-nginx`
- Check firewall rules allow traffic on ports 80/443

### Certificate Not Provisioning

**Symptoms**: Certificate stuck in "Pending" or "False" ready status

**Diagnosis**:
```bash
# 1. Check certificate status
kubectl describe certificate rag-tls-cert -n rag-system

# 2. Check challenges
kubectl get challenges -n rag-system
kubectl describe challenge -n rag-system

# 3. Check cert-manager logs
kubectl logs -n cert-manager deployment/cert-manager --tail=100
```

**Common Issues**:
1. **DNS not resolving**: Wait for DNS propagation (up to 48 hours)
2. **HTTP-01 challenge failing**: Ensure port 80 accessible
3. **Rate limit hit**: Use `letsencrypt-staging` for testing
4. **Wrong ClusterIssuer**: Verify issuer name matches in Certificate and Ingress

### Cloud Armor Blocking Legitimate Traffic

**Symptoms**: Valid requests returning 403

**Diagnosis**:
```bash
# View blocked requests
gcloud logging read \
  "resource.type=http_load_balancer AND jsonPayload.enforcedSecurityPolicy.outcome=DENY" \
  --limit 50 --format=json

# Check which rule blocked
# Look for: jsonPayload.enforcedSecurityPolicy.matchedFieldValue
```

**Solutions**:
- Whitelist specific IPs: Add allow rule with higher priority (lower number)
- Adjust WAF sensitivity: Use `*-stable` instead of `*-canary` rules
- Create exceptions: Add custom rules for false positives

### High Latency

**Symptoms**: Slow response times

**Diagnosis**:
```bash
# Check backend health
kubectl get backendconfig -n rag-system
gcloud compute backend-services list

# View load balancer metrics
# Cloud Console → Network Services → Load Balancing

# Check pod performance
kubectl top pods -n rag-system
```

**Solutions**:
- Increase backend timeout: Update `timeoutSec` in BackendConfig
- Scale replicas: Increase pod count via HPA or manual scaling
- Optimize health checks: Reduce `checkIntervalSec` if overloading backends
- Enable CDN: Add CDN configuration to BackendConfig

---

## Cost Optimization

### Pricing Breakdown

| Resource | Cost | Notes |
|----------|------|-------|
| **Static IP** | $0.01/hour (~$7/month) | Free when in use by forwarding rule |
| **Cloud DNS Zone** | $0.20/month | Per zone |
| **Cloud DNS Queries** | $0.40/million | First million free |
| **Cloud Armor** | $5/policy/month | Plus $0.75/million requests |
| **Load Balancer** | $18/month | 5 forwarding rules + data processing |
| **Let's Encrypt Certs** | Free | Unlimited |
| **NGINX Ingress** | Included | GKE cluster costs |

**Total**: ~$25-50/month (varies with traffic)

### Cost Reduction Strategies

1. **Use Single Domain with Paths** (instead of subdomains):
   ```
   rag.example.com/auth  instead of  api.rag.example.com/api/v1/auth
   ```
   Reduces DNS queries

2. **Optimize Health Checks**:
   - Increase check interval: 10s → 15s (reduce health check traffic)
   - Use lightweight endpoints

3. **Enable CDN** (for static content):
   ```yaml
   apiVersion: cloud.google.com/v1
   kind: BackendConfig
   spec:
     cdn:
       enabled: true
       cachePolicy:
         includeHost: true
         includeProtocol: true
   ```

4. **Use Cloud Armor Efficiently**:
   - Combine multiple projects under single policy
   - Use preconfigured rules (included in base price)

5. **Monitor Unused Resources**:
   ```bash
   # List static IPs not in use
   gcloud compute addresses list --filter="status:RESERVED" --global
   
   # Delete unused IPs
   gcloud compute addresses delete unused-ip-name --global
   ```

---

## Next Steps

1. **Execute ingress setup**:
   ```bash
   ./scripts/gcp/16-setup-ingress.sh --env dev --domain rag-dev.example.com
   ```

2. **Deploy ingress manifests**:
   ```bash
   kubectl apply -f k8s/base/backendconfig.yaml
   kubectl apply -f k8s/base/ingress.yaml
   ```

3. **Configure monitoring**:
   - Create Cloud Monitoring alerts
   - Set up uptime checks for all domains
   - Configure log-based metrics

4. **Test end-to-end**:
   - Verify HTTPS access
   - Test all service endpoints
   - Validate certificate auto-renewal
   - Trigger Cloud Armor rules

5. **Proceed to GCP-DEPLOY-011**:
   - Deploy all microservices
   - Run integration tests
   - Validate production readiness

---

## References

- [GKE Ingress Documentation](https://cloud.google.com/kubernetes-engine/docs/concepts/ingress)
- [NGINX Ingress Controller](https://kubernetes.github.io/ingress-nginx/)
- [cert-manager Documentation](https://cert-manager.io/docs/)
- [Cloud Armor Overview](https://cloud.google.com/armor/docs)
- [Cloud DNS Documentation](https://cloud.google.com/dns/docs)
- [Let's Encrypt Rate Limits](https://letsencrypt.org/docs/rate-limits/)

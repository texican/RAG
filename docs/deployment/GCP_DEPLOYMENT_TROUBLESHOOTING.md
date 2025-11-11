# GCP Deployment Troubleshooting Guide

**Date**: November 10, 2025  
**Environment**: byo-rag-dev (GKE + Cloud SQL)  
**Status**: Architecture Issue Resolved ✅ | Database Connectivity Issue ❌

## Executive Summary

This document chronicles the troubleshooting process for deploying RAG microservices to GKE with Cloud SQL PostgreSQL. The deployment encountered two major categories of issues:

1. **Architecture Incompatibility (RESOLVED)**: ARM containers built on Mac couldn't run on x86 GKE nodes
2. **Database Connectivity (UNRESOLVED)**: GKE pods cannot reach Cloud SQL instance via private or public IP

## Table of Contents

- [Issue 1: Architecture Mismatch](#issue-1-architecture-mismatch)
- [Issue 2: Database Connectivity](#issue-2-database-connectivity)
- [Current State](#current-state)
- [Next Steps](#next-steps)
- [Reference Commands](#reference-commands)

---

## Issue 1: Architecture Mismatch

### Problem Description

**Symptom**: Pods failing with `exec format error`

```
standard_init_linux.go:228: exec user process caused: exec format error
```

**Root Cause**: Container images were built on ARM-based Mac (M1/M2) producing `linux/arm64` binaries, but GKE cluster nodes are x86-based (`linux/amd64`).

### Investigation Steps

1. **Confirmed GKE node architecture**:
   ```bash
   kubectl get nodes -o wide
   # Showed x86 (amd64) architecture
   ```

2. **Checked container architecture**:
   ```bash
   docker manifest inspect us-central1-docker.pkg.dev/byo-rag-dev/rag-system/rag-auth-service:latest
   # Showed linux/arm64
   ```

3. **Verified Java version in running container**:
   ```bash
   kubectl logs -n rag-system rag-auth-xxx | grep "Java"
   # Showed errors before Java could start
   ```

### Solution: Google Cloud Build ✅

**Implementation**: Created `cloudbuild.yaml` to build images on GCP infrastructure (x86).

**Key Configuration**:

```yaml
steps:
  # Build shared module first
  - name: 'maven:3.9-eclipse-temurin-21'
    args:
      - 'mvn'
      - 'clean'
      - 'install'
      - '-f'
      - 'rag-shared/pom.xml'
      - '-DskipTests'
    
  # Build each service (example: rag-auth)
  - name: 'maven:3.9-eclipse-temurin-21'
    id: 'build-rag-auth'
    args:
      - 'mvn'
      - 'clean'
      - 'package'
      - '-f'
      - 'rag-auth-service/pom.xml'
      - '-Dmaven.test.skip=true'  # Skip test compilation AND execution
    waitFor: ['build-shared']
    
  # Build Docker image
  - name: 'gcr.io/cloud-builders/docker'
    id: 'docker-build-rag-auth'
    args:
      - 'build'
      - '-t'
      - 'us-central1-docker.pkg.dev/$PROJECT_ID/rag-system/rag-auth-service:latest'
      - '-f'
      - 'rag-auth-service/Dockerfile'
      - '.'  # Context is root directory
    waitFor: ['build-rag-auth']

options:
  machineType: 'E2_HIGHCPU_8'
  logging: CLOUD_LOGGING_ONLY
```

**Critical Fixes**:
- Used `maven:3.9-eclipse-temurin-21` instead of `gcr.io/cloud-builders/mvn` for Java 21 support
- Changed `-DskipTests` to `-Dmaven.test.skip=true` to skip test compilation (Lombok issues)
- Set Docker build context to `.` (root) instead of `./rag-{service}` for correct COPY paths
- Added `E2_HIGHCPU_8` machine type for faster builds

**Build Execution**:
```bash
cd /Users/stryfe/Projects/RAG_SpecKit/RAG
./scripts/gcp/07a-cloud-build-images.sh
```

**Results**:
- ✅ All 5 services built successfully
- ✅ Build time: 3 minutes 14 seconds
- ✅ Images tagged with `:latest`, `:<git-sha>`, and `:<build-id>`
- ✅ Containers start without "exec format error"
- ✅ Java 21.0.9 applications initialize correctly

### Alternative Solutions Considered

1. **Local Multi-Architecture Builds** ❌
   - Using `docker buildx build --platform linux/amd64`
   - **Problem**: Slow on ARM Macs (emulation), difficult CI/CD integration
   - **Status**: Rejected in favor of Cloud Build

2. **Cross-Compilation** ❌
   - Building for different architecture locally
   - **Problem**: Complex Java toolchain setup, not reliable
   - **Status**: Not attempted

---

## Issue 2: Database Connectivity

### Problem Description

**Symptom**: Applications cannot connect to Cloud SQL PostgreSQL database

```
org.postgresql.util.PSQLException: Connection to 10.200.0.3:5432 refused
java.net.SocketException: Connection reset
dial tcp 10.200.0.3:3307: i/o timeout (Cloud SQL Proxy)
dial tcp 34.121.222.56:3307: i/o timeout (Cloud SQL Proxy public IP)
```

**Root Cause**: VPC networking configuration preventing connectivity between GKE pods and Cloud SQL instance

### Environment Details

**GKE Cluster**: `rag-gke-dev`
- Region: `us-central1`
- Zones: `us-central1-a`, `us-central1-b`, `us-central1-c`
- Network: `rag-vpc`
- Subnet: `rag-gke-subnet` (10.0.0.0/20)
- Kubernetes Version: v1.33.5-gke.1201000

**Cloud SQL Instance**: `rag-postgres`
- Region: `us-central1`
- Private IP: `10.200.0.3` (from `10.200.0.0/16` range)
- Public IP: `34.121.222.56`
- Databases: `rag_auth`, `rag_admin`, `rag_document`, `postgres`

**VPC Configuration**:
- Private Service Connection: `10.200.0.0/16`
- VPC Peering: `servicenetworking-googleapis-com` (ACTIVE)
- Private Google Access: Enabled

### Investigation Timeline

#### Attempt 1: Cloud SQL Proxy with Service Account ❌

**Configuration**:
```yaml
- name: cloud-sql-proxy
  image: gcr.io/cloud-sql-connectors/cloud-sql-proxy:2.8.0
  args:
    - "--structured-logs"
    - "--port=5432"
    - "byo-rag-dev:us-central1:rag-postgres"
```

**Error**:
```
metadata: GCE metadata "instance/service-accounts/default/token" not defined
```

**Action Taken**: Created GCP service account and configured Workload Identity

```bash
# Create service account
gcloud iam service-accounts create cloud-sql-sa \
  --display-name="Cloud SQL Proxy Service Account" \
  --project=byo-rag-dev

# Grant Cloud SQL Client role
gcloud projects add-iam-policy-binding byo-rag-dev \
  --member="serviceAccount:cloud-sql-sa@byo-rag-dev.iam.gserviceaccount.com" \
  --role="roles/cloudsql.client"

# Bind Kubernetes SA to GCP SA
gcloud iam service-accounts add-iam-policy-binding \
  cloud-sql-sa@byo-rag-dev.iam.gserviceaccount.com \
  --role="roles/iam.workloadIdentityUser" \
  --member="serviceAccount:byo-rag-dev.svc.id.goog[rag-system/rag-auth]"

# Grant token creator role
gcloud iam service-accounts add-iam-policy-binding \
  cloud-sql-sa@byo-rag-dev.iam.gserviceaccount.com \
  --role="roles/iam.serviceAccountTokenCreator" \
  --member="serviceAccount:byo-rag-dev.svc.id.goog[rag-system/rag-auth]"
```

**Result**: Workload Identity configured correctly ✅, but new error appeared

#### Attempt 2: Cloud SQL Proxy with Public IP ❌

**Error**:
```
dial tcp 104.197.76.156:3307: i/o timeout
```

**Investigation**:
```bash
# Check Cloud SQL IP configuration
gcloud sql instances describe rag-postgres --format="value(ipAddresses)"
# Output: {'ipAddress': '104.197.76.156', 'type': 'PRIMARY'}

# Check NAT configuration
gcloud compute routers nats describe rag-nat --router=rag-router --region=us-central1
# Output: endpointTypes: ENDPOINT_TYPE_VM (missing GKE support)
```

**Action Taken**: Updated NAT to support general egress

```bash
# Delete and recreate NAT
gcloud compute routers nats delete rag-nat \
  --router=rag-router \
  --region=us-central1 \
  --quiet

gcloud compute routers nats create rag-nat \
  --router=rag-router \
  --region=us-central1 \
  --nat-all-subnet-ip-ranges \
  --auto-allocate-nat-external-ips \
  --enable-logging
```

**Result**: General internet access works (can curl google.com) ✅, but Cloud SQL still unreachable ❌

#### Attempt 3: Cloud SQL Proxy with Private IP ❌

**Configuration**:
```yaml
args:
  - "--structured-logs"
  - "--port=5432"
  - "--private-ip"  # Added this flag
  - "byo-rag-dev:us-central1:rag-postgres"
```

**Action Taken**: Enabled private IP on Cloud SQL instance

```bash
# Configure private IP
gcloud sql instances patch rag-postgres \
  --network=projects/byo-rag-dev/global/networks/rag-vpc \
  --no-assign-ip \
  --project=byo-rag-dev
```

**Error**:
```
dial tcp 10.200.0.3:3307: i/o timeout
```

**Investigation**:
```bash
# Check firewall rules
gcloud compute firewall-rules describe rag-allow-internal
# sourceRanges: 10.0.0.0/20, 10.4.0.0/14, 10.8.0.0/20
# Missing: 10.200.0.0/16 (private service connection range)

# Check VPC peering
gcloud compute networks peerings list --network=rag-vpc
# servicenetworking-googleapis-com: ACTIVE
# importCustomRoutes: False
# exportCustomRoutes: False
```

**Action Taken**: Fixed firewall and peering configuration

```bash
# Add private service connection range to firewall
gcloud compute firewall-rules update rag-allow-internal \
  --source-ranges=10.0.0.0/20,10.4.0.0/14,10.8.0.0/20,10.200.0.0/16

# Enable custom route import/export
gcloud compute networks peerings update servicenetworking-googleapis-com \
  --network=rag-vpc \
  --import-custom-routes \
  --export-custom-routes
```

**Result**: Still timing out ❌

#### Attempt 4: Direct Connection to Private IP ❌

**Configuration**: Removed Cloud SQL Proxy, connect directly

```yaml
env:
  - name: SPRING_DATASOURCE_URL
    value: "jdbc:postgresql://10.200.0.3:5432/rag_auth?sslmode=disable"
```

**Updated ConfigMap**:
```bash
kubectl patch configmap gcp-config -n rag-system --type='json' -p='[
  {"op": "replace", "path": "/data/DB_HOST", "value": "10.200.0.3"},
  {"op": "replace", "path": "/data/DB_NAME", "value": "rag_auth"}
]'
```

**Error**:
```
Connection to 10.200.0.3:5432 refused
```

**Connectivity Test**:
```bash
# Test from within pod
kubectl run test-connectivity --image=busybox --rm -it --restart=Never -n rag-system \
  -- /bin/sh -c "nc -zv -w 5 10.200.0.3 5432 2>&1"
# Result: Connection timed out
```

**Result**: Pods cannot reach private IP at all ❌

### Database Configuration Issues Fixed ✅

During troubleshooting, discovered and fixed:

1. **Database Name Mismatch**:
   - Application configured for: `rag_enterprise`
   - Actual database name: `rag_auth`
   - **Fix**: Updated ConfigMap to use correct database name

2. **SSL Configuration**:
   - Added `?sslmode=disable` to connection URL
   - Verified Cloud SQL doesn't require SSL: `requireSsl: False`

### Connectivity Tests Performed

1. **General Internet Access** ✅:
   ```bash
   kubectl run test-egress --image=curlimages/curl --rm -it --restart=Never -n rag-system \
     -- curl -I -m 5 https://www.google.com
   # Result: HTTP/2 200 (Success)
   ```

2. **Google API Access** ✅:
   ```bash
   kubectl run test-api --image=curlimages/curl --rm -it --restart=Never -n rag-system \
     -- curl -I -m 5 https://sqladmin.googleapis.com
   # Result: HTTP/2 404 (Connected, endpoint not found - expected)
   ```

3. **Cloud SQL Public IP** ❌:
   ```bash
   kubectl run test-cloudsql --image=busybox --rm -it --restart=Never -n rag-system \
     -- /bin/sh -c "nc -zv -w 10 34.121.222.56 3307 2>&1"
   # Result: Connection timed out
   ```

4. **Cloud SQL Private IP** ❌:
   ```bash
   kubectl run test-connectivity --image=busybox --rm -it --restart=Never -n rag-system \
     -- /bin/sh -c "nc -zv -w 5 10.200.0.3 5432 2>&1"
   # Result: Connection timed out
   ```

### Configuration Changes Made

#### VPC Networking

1. **Firewall Rules**:
   ```bash
   # Before
   sourceRanges: 10.0.0.0/20, 10.4.0.0/14, 10.8.0.0/20
   
   # After
   sourceRanges: 10.0.0.0/20, 10.4.0.0/14, 10.8.0.0/20, 10.200.0.0/16
   ```

2. **VPC Peering**:
   ```bash
   # Before
   importCustomRoutes: false
   exportCustomRoutes: false
   
   # After
   importCustomRoutes: true
   exportCustomRoutes: true
   ```

3. **Cloud NAT**:
   ```bash
   # Recreated with proper configuration for all subnet IP ranges
   --nat-all-subnet-ip-ranges
   --auto-allocate-nat-external-ips
   ```

#### Cloud SQL Instance

1. **IP Configuration**:
   ```bash
   # Current state (has both)
   Private IP: 10.200.0.3
   Public IP: 34.121.222.56
   ```

2. **Authorized Networks**:
   ```bash
   authorizedNetworks: 0.0.0.0/0
   ```

### Potential Root Causes (Unverified)

1. **VPC Peering Routes Not Propagating**:
   - Despite ACTIVE status, routes may not be functioning
   - Private service connection peering might be misconfigured at Google side

2. **GKE Cluster Network Policy**:
   - Possible network policies blocking egress
   - Pod security policies restricting connectivity

3. **Cloud SQL Instance Firewall**:
   - Instance-level firewall might be blocking connections
   - Even with authorized networks set to 0.0.0.0/0

4. **Subnet Route Priority**:
   - GKE pod subnet routes may conflict with peering routes
   - Need to verify route priority and propagation

---

## Current State

### What's Working ✅

1. **Container Architecture**:
   - Cloud Build successfully creates x86-compatible images
   - Images push to Artifact Registry
   - Containers start without "exec format error"
   - Java 21.0.9 applications initialize

2. **Kubernetes Resources**:
   - Deployments create successfully
   - Pods start and run
   - Service accounts configured with Workload Identity
   - ConfigMaps and Secrets accessible

3. **IAM & Permissions**:
   - GCP service account `cloud-sql-sa` exists
   - Has `roles/cloudsql.client` role
   - Workload Identity binding configured
   - Token creator permissions granted

4. **Networking (Partial)**:
   - Private Google Access enabled
   - Pods can reach Google APIs
   - General internet connectivity works (to google.com)
   - Firewall rules allow internal traffic

5. **Database**:
   - Cloud SQL instance running (RUNNABLE)
   - Database `rag_auth` exists
   - User credentials configured in secrets
   - SSL not required

### What's Not Working ❌

1. **Cloud SQL Connectivity**:
   - Pods cannot reach Cloud SQL private IP (10.200.0.3:5432)
   - Pods cannot reach Cloud SQL public IP (34.121.222.56:3307)
   - Cloud SQL Proxy times out connecting to instance
   - Direct PostgreSQL connections timeout

2. **VPC Peering**:
   - Routes show ACTIVE but traffic doesn't flow
   - Private service connection not functioning
   - Pods in 10.0.0.0/20 cannot reach 10.200.0.0/16

3. **Application Startup**:
   - Pods crash due to database connection failures
   - HikariCP pool initialization times out
   - Applications never reach "Started" state

### Pod Status

```bash
NAME                        READY   STATUS             RESTARTS
rag-auth-58d6c4fd79-vrxrd   0/1     CrashLoopBackOff   Multiple
```

**Crash Reason**: Cannot establish database connection

---

## Next Steps

### Immediate Actions Required

1. **Investigate VPC Peering Route Propagation**:
   ```bash
   # Check detailed route information
   gcloud compute routes list --filter="network:rag-vpc" --format=json
   
   # Check if routes are being exported from peered network
   gcloud compute networks peerings list-routes servicenetworking-googleapis-com \
     --network=rag-vpc \
     --region=us-central1 \
     --direction=INCOMING
   ```

2. **Verify GKE Network Policy**:
   ```bash
   # Check for network policies
   kubectl get networkpolicies -n rag-system
   
   # Check pod network configuration
   kubectl get pods -n rag-system -o wide
   ```

3. **Test Connectivity from GKE Node**:
   ```bash
   # SSH to GKE node
   gcloud compute ssh [NODE_NAME] --project=byo-rag-dev --zone=us-central1-a
   
   # Test from node (not pod)
   nc -zv 10.200.0.3 5432
   ```

4. **Review Cloud SQL Logs**:
   ```bash
   # Check for connection attempts
   gcloud sql operations list --instance=rag-postgres --limit=50
   
   # View Cloud SQL error logs
   gcloud logging read "resource.type=cloudsql_database \
     AND resource.labels.database_id=byo-rag-dev:rag-postgres" \
     --limit=50 --format=json
   ```

5. **Consider Alternative Approaches**:
   - Use Cloud SQL Auth Proxy as a separate deployment (not sidecar)
   - Deploy Cloud SQL Proxy on GKE nodes as DaemonSet
   - Use Serverless VPC Access connector
   - Temporarily use public IP with stricter authorized networks

### Diagnostic Commands

```bash
# Comprehensive network diagnostic
kubectl run netshoot --rm -it --image=nicolaka/netshoot -n rag-system -- /bin/bash

# Inside netshoot pod:
ping -c 3 10.200.0.3
traceroute 10.200.0.3
nslookup 10.200.0.3
telnet 10.200.0.3 5432
curl -v telnet://10.200.0.3:5432

# Check DNS resolution
nslookup rag-postgres.c.byo-rag-dev.internal
```

### Configuration to Review

1. **GKE Cluster Creation Parameters**:
   - Was `--enable-ip-alias` used?
   - Is `--enable-private-nodes` set?
   - Check `--enable-private-endpoint` setting
   - Verify `--master-ipv4-cidr` doesn't conflict

2. **Cloud SQL Private IP Allocation**:
   - Verify reserved IP range is correct size
   - Check for IP range conflicts
   - Confirm peering connection is bidirectional

3. **Service Networking Connection**:
   ```bash
   gcloud services vpc-peerings list \
     --network=rag-vpc \
     --project=byo-rag-dev
   ```

---

## Reference Commands

### Cloud Build

```bash
# Build all images
cd /Users/stryfe/Projects/RAG_SpecKit/RAG
./scripts/gcp/07a-cloud-build-images.sh

# Check build status
gcloud builds list --limit=5 --project=byo-rag-dev

# View build logs
gcloud builds log <BUILD_ID> --project=byo-rag-dev
```

### Kubernetes Operations

```bash
# Apply deployment
kubectl apply -f k8s/base/rag-auth-deployment.yaml

# Check pod status
kubectl get pods -n rag-system -l app=rag-auth

# View logs
kubectl logs -n rag-system <POD_NAME> -c rag-auth
kubectl logs -n rag-system <POD_NAME> -c cloud-sql-proxy

# Restart deployment
kubectl rollout restart deployment rag-auth -n rag-system

# Delete and recreate
kubectl delete deployment rag-auth -n rag-system
kubectl apply -f k8s/base/rag-auth-deployment.yaml

# Check events
kubectl get events -n rag-system --sort-by='.lastTimestamp'
```

### Cloud SQL Operations

```bash
# Describe instance
gcloud sql instances describe rag-postgres --project=byo-rag-dev

# List databases
gcloud sql databases list --instance=rag-postgres --project=byo-rag-dev

# Patch instance configuration
gcloud sql instances patch rag-postgres \
  --network=projects/byo-rag-dev/global/networks/rag-vpc \
  --project=byo-rag-dev

# Enable/disable public IP
gcloud sql instances patch rag-postgres --assign-ip --project=byo-rag-dev
gcloud sql instances patch rag-postgres --no-assign-ip --project=byo-rag-dev
```

### Networking

```bash
# List firewall rules
gcloud compute firewall-rules list --filter="network:rag-vpc" --project=byo-rag-dev

# Update firewall rule
gcloud compute firewall-rules update rag-allow-internal \
  --source-ranges=10.0.0.0/20,10.4.0.0/14,10.8.0.0/20,10.200.0.0/16 \
  --project=byo-rag-dev

# Check VPC peering
gcloud compute networks peerings list --network=rag-vpc --project=byo-rag-dev

# Check routes
gcloud compute routes list --filter="network:rag-vpc" --project=byo-rag-dev

# Check NAT configuration
gcloud compute routers nats describe rag-nat \
  --router=rag-router \
  --region=us-central1 \
  --project=byo-rag-dev
```

### ConfigMap Updates

```bash
# View ConfigMap
kubectl get configmap gcp-config -n rag-system -o yaml

# Update database configuration
kubectl patch configmap gcp-config -n rag-system --type='json' -p='[
  {"op": "replace", "path": "/data/DB_HOST", "value": "10.200.0.3"},
  {"op": "replace", "path": "/data/DB_NAME", "value": "rag_auth"},
  {"op": "replace", "path": "/data/SPRING_DATASOURCE_URL", 
   "value": "jdbc:postgresql://10.200.0.3:5432/rag_auth?sslmode=disable"}
]'
```

---

## Lessons Learned

### What Worked

1. **Cloud Build for Multi-Architecture**: Using Google Cloud Build eliminates local architecture issues and provides consistent, reproducible builds
2. **Workload Identity**: Proper IAM configuration prevents authentication errors with Cloud SQL Proxy
3. **Systematic Testing**: Network connectivity tests helped isolate the exact failure point
4. **Configuration Management**: Using ConfigMaps allows quick iteration on database connection parameters

### What Didn't Work

1. **Cloud SQL Proxy Sidecar**: Could not establish connection to Cloud SQL instance via public or private IP
2. **Direct Database Connection**: Even without proxy, pods cannot reach Cloud SQL
3. **NAT Gateway Updates**: Didn't resolve connectivity to Cloud SQL public IP
4. **Firewall Rule Updates**: Adding private service connection range didn't enable connectivity
5. **VPC Peering Route Import**: Enabling custom routes didn't fix traffic flow

### Key Insights

1. **VPC Peering is Complex**: Private service connections require careful configuration and troubleshooting
2. **Cloud SQL Access Patterns**: Multiple approaches exist (proxy, private IP, public IP), each with tradeoffs
3. **Network Isolation**: GKE network isolation is strong; requires explicit configuration for external services
4. **Testing is Critical**: Small connectivity tests reveal issues before full deployment

---

## Related Documentation

- [IMAGE_BUILD_PRACTICES.md](./IMAGE_BUILD_PRACTICES.md) - Container build best practices
- [Cloud SQL Private IP Documentation](https://cloud.google.com/sql/docs/postgres/configure-private-ip)
- [Workload Identity Documentation](https://cloud.google.com/kubernetes-engine/docs/how-to/workload-identity)
- [VPC Peering Documentation](https://cloud.google.com/vpc/docs/vpc-peering)

---

## Contact & Support

If you encounter similar issues or have solutions:
- Review GCP documentation on VPC peering and Cloud SQL connectivity
- Check GKE and Cloud SQL logs for additional error messages
- Consider opening a GCP support ticket for networking configuration review
- Test with simplified network topology to isolate issues

**Last Updated**: November 10, 2025

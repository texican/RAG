---
version: 1.0.0
last-updated: 2025-11-12
status: active
applies-to: 0.8.0-SNAPSHOT
category: deployment
---

# Container Image Building Best Practices for GCP

This document outlines best practices for building and deploying container images to Google Cloud Platform.

## Table of Contents
- [Quick Start](#quick-start)
- [Build Methods Comparison](#build-methods-comparison)
- [Recommended: Cloud Build](#recommended-cloud-build)
- [Alternative: Local Build with Correct Architecture](#alternative-local-build)
- [Image Tagging Strategy](#image-tagging-strategy)
- [Security Best Practices](#security-best-practices)
- [Troubleshooting](#troubleshooting)

## Quick Start

### Using Cloud Build (Recommended)
```bash
# Build all services
make gcp-cloud-build ENV=dev

# Build specific service
./scripts/gcp/07a-cloud-build-images.sh --env dev --service rag-auth-service

# Build and deploy in one command
gcloud builds submit --config=cloudbuild.yaml
```

### Using Local Docker (Not Recommended for ARM Macs)
```bash
# Only use if you're on x86 Linux or need local builds
make gcp-build ENV=dev
```

## Build Methods Comparison

### Method 1: Cloud Build ⭐ **RECOMMENDED**

**Advantages:**
- ✅ Correct architecture (x86) automatically
- ✅ Parallel builds (faster)
- ✅ No local resource consumption
- ✅ Integrated vulnerability scanning
- ✅ Build history and audit logs
- ✅ Direct push to Artifact Registry
- ✅ Works from any machine (ARM/x86)
- ✅ Consistent builds across team

**Disadvantages:**
- ❌ Requires Cloud Build API enabled
- ❌ Small cost per build minute (free tier available)
- ❌ Requires internet connection

**Cost:** ~$0.003/build-minute after free tier (first 120 minutes/day free)

**Usage:**
```bash
# Build all services
make gcp-cloud-build ENV=dev

# Build specific service
./scripts/gcp/07a-cloud-build-images.sh \
  --env dev \
  --service rag-auth-service

# Async build (return immediately)
./scripts/gcp/07a-cloud-build-images.sh \
  --env dev \
  --async

# Check build status
gcloud builds list --limit=5
gcloud builds log --stream <BUILD_ID>
```

### Method 2: Local Docker with Buildx

**Use When:**
- Testing Dockerfile changes locally
- No internet connection
- Development iteration

**Requirements:**
- Docker with buildx support
- Correct platform specification

**Usage:**
```bash
# Build for x86 (GKE architecture)
docker buildx build \
  --platform linux/amd64 \
  -t us-central1-docker.pkg.dev/byo-rag-dev/rag-system/rag-auth-service:latest \
  -f rag-auth-service/Dockerfile \
  ./rag-auth-service \
  --push

# Build multi-platform (ARM + x86)
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t us-central1-docker.pkg.dev/byo-rag-dev/rag-system/rag-auth-service:latest \
  -f rag-auth-service/Dockerfile \
  ./rag-auth-service \
  --push
```

### Method 3: GitHub Actions with Cloud Build

**Best For:**
- CI/CD pipelines
- Automated deployments
- Pull request builds

**Example `.github/workflows/build.yml`:**
```yaml
name: Build and Deploy

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
      
      - id: 'auth'
        uses: 'google-github-actions/auth@v2'
        with:
          credentials_json: '${{ secrets.GCP_SA_KEY }}'
      
      - name: 'Set up Cloud SDK'
        uses: 'google-github-actions/setup-gcloud@v2'
      
      - name: 'Build with Cloud Build'
        run: |
          gcloud builds submit \
            --config=cloudbuild.yaml \
            --project=byo-rag-dev
```

## Recommended: Cloud Build

### Initial Setup

1. **Enable Cloud Build API:**
```bash
gcloud services enable cloudbuild.googleapis.com
```

2. **Grant Permissions:**
```bash
# Get the Cloud Build service account
PROJECT_ID=$(gcloud config get-value project)
PROJECT_NUMBER=$(gcloud projects describe $PROJECT_ID --format='value(projectNumber)')
CLOUD_BUILD_SA="${PROJECT_NUMBER}@cloudbuild.gserviceaccount.com"

# Grant Artifact Registry Writer role
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${CLOUD_BUILD_SA}" \
  --role="roles/artifactregistry.writer"
```

### Build Configuration

The `cloudbuild.yaml` file defines your build:

```yaml
steps:
  # Build shared library
  - name: 'gcr.io/cloud-builders/mvn'
    args: ['clean', 'install', '-pl', 'rag-shared', '-DskipTests']
  
  # Build service
  - name: 'gcr.io/cloud-builders/mvn'
    args: ['package', '-pl', 'rag-auth-service', '-DskipTests']
  
  # Build Docker image
  - name: 'gcr.io/cloud-builders/docker'
    args:
      - 'build'
      - '-t'
      - 'us-central1-docker.pkg.dev/$PROJECT_ID/rag-system/rag-auth-service:latest'
      - '-f'
      - 'rag-auth-service/Dockerfile'
      - './rag-auth-service'

images:
  - 'us-central1-docker.pkg.dev/$PROJECT_ID/rag-system/rag-auth-service:latest'
```

### Monitoring Builds

```bash
# List recent builds
gcloud builds list --limit=10

# View build details
gcloud builds describe <BUILD_ID>

# Stream build logs
gcloud builds log --stream <BUILD_ID>

# View in console
open "https://console.cloud.google.com/cloud-build/builds"
```

## Alternative: Local Build

### Update the Build Script

If you must build locally (not recommended for ARM Macs), update the build script:

```bash
# Edit scripts/gcp/07-build-and-push-images.sh
# Add platform specification to docker build commands:

docker build \
  --platform linux/amd64 \
  -t ${REGISTRY}/${SERVICE}:${VERSION} \
  -f ${SERVICE}/Dockerfile \
  ./${SERVICE}
```

### Using Docker Buildx

```bash
# Create buildx builder
docker buildx create --name multiarch --use

# Build for x86
docker buildx build \
  --platform linux/amd64 \
  -t us-central1-docker.pkg.dev/byo-rag-dev/rag-system/rag-auth-service:latest \
  -f rag-auth-service/Dockerfile \
  ./rag-auth-service \
  --push
```

## Image Tagging Strategy

### Recommended Tags

Each image should have multiple tags for different use cases:

1. **`latest`** - Always points to the most recent build
   ```
   us-central1-docker.pkg.dev/PROJECT/rag-system/rag-auth-service:latest
   ```

2. **Git SHA** - Specific commit identifier
   ```
   us-central1-docker.pkg.dev/PROJECT/rag-system/rag-auth-service:a1b2c3d
   ```

3. **Semantic Version** - Release version
   ```
   us-central1-docker.pkg.dev/PROJECT/rag-system/rag-auth-service:1.2.3
   ```

4. **Build ID** - Cloud Build ID for tracking
   ```
   us-central1-docker.pkg.dev/PROJECT/rag-system/rag-auth-service:build-12345
   ```

5. **Environment-specific** - For different environments
   ```
   us-central1-docker.pkg.dev/PROJECT/rag-system/rag-auth-service:dev
   us-central1-docker.pkg.dev/PROJECT/rag-system/rag-auth-service:prod
   ```

### Kubernetes Deployment Strategy

**For Development:**
- Use `latest` tag
- Set `imagePullPolicy: Always`
- Enables quick iterations

**For Production:**
- Use specific version tags (semantic version or git SHA)
- Set `imagePullPolicy: IfNotPresent`
- Ensures reproducible deployments

## Security Best Practices

### 1. Vulnerability Scanning

Cloud Build automatically scans images:

```bash
# View vulnerabilities
gcloud artifacts docker images describe \
  us-central1-docker.pkg.dev/PROJECT/rag-system/rag-auth-service:latest \
  --show-all-metadata

# List vulnerabilities
gcloud artifacts docker images list-vulnerabilities \
  us-central1-docker.pkg.dev/PROJECT/rag-system/rag-auth-service:latest
```

### 2. Base Image Best Practices

```dockerfile
# Use specific version tags, not latest
FROM eclipse-temurin:17-jre-alpine

# Update packages
RUN apk update && apk upgrade

# Use non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
```

### 3. Image Signing

Sign images for production:

```bash
# Using Binary Authorization
gcloud beta container binauthz attestations sign-and-create \
  --artifact-url="us-central1-docker.pkg.dev/PROJECT/rag-system/rag-auth-service@sha256:..." \
  --attestor=prod-attestor \
  --project=PROJECT_ID
```

### 4. Private Registry Access

```bash
# Configure docker authentication
gcloud auth configure-docker us-central1-docker.pkg.dev

# Create service account for pulling images
gcloud iam service-accounts create artifact-registry-reader

# Grant read access
gcloud artifacts repositories add-iam-policy-binding rag-system \
  --location=us-central1 \
  --member="serviceAccount:artifact-registry-reader@PROJECT.iam.gserviceaccount.com" \
  --role="roles/artifactregistry.reader"
```

## Troubleshooting

### Issue: "exec format error"

**Cause:** Image built for wrong architecture (ARM vs x86)

**Solution:**
```bash
# Option 1: Use Cloud Build (automatically correct)
make gcp-cloud-build ENV=dev

# Option 2: Specify platform in local build
docker buildx build --platform linux/amd64 ...
```

### Issue: "Failed to pull image"

**Cause:** GKE can't access Artifact Registry

**Solution:**
```bash
# Check GKE service account has permission
kubectl get serviceaccount -n rag-system

# Grant Artifact Registry Reader role
gcloud projects add-iam-policy-binding PROJECT_ID \
  --member="serviceAccount:GKE_SA@PROJECT.iam.gserviceaccount.com" \
  --role="roles/artifactregistry.reader"
```

### Issue: Cloud Build fails with "permission denied"

**Solution:**
```bash
# Grant Cloud Build service account required permissions
PROJECT_NUMBER=$(gcloud projects describe PROJECT_ID --format='value(projectNumber)')
CLOUD_BUILD_SA="${PROJECT_NUMBER}@cloudbuild.gserviceaccount.com"

gcloud projects add-iam-policy-binding PROJECT_ID \
  --member="serviceAccount:${CLOUD_BUILD_SA}" \
  --role="roles/artifactregistry.writer"
```

### Issue: Build timeout

**Solution:**
```yaml
# In cloudbuild.yaml, increase timeout
options:
  machineType: 'E2_HIGHCPU_8'  # Use faster machine
  timeout: '3600s'              # Increase to 1 hour
```

## Performance Optimization

### 1. Layer Caching

```dockerfile
# Order layers from least to most frequently changing
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src/ ./src/
RUN mvn package
```

### 2. Multi-stage Builds

```dockerfile
# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 3. Parallel Builds

Cloud Build automatically builds services in parallel when possible.

## Continuous Integration

### Automated Builds on Push

```bash
# Create build trigger
gcloud builds triggers create github \
  --repo-name=RAG \
  --repo-owner=texican \
  --branch-pattern="^main$" \
  --build-config=cloudbuild.yaml \
  --description="Build RAG services on main branch"
```

### Build on Pull Request

```bash
# Create PR trigger
gcloud builds triggers create github \
  --repo-name=RAG \
  --repo-owner=texican \
  --pull-request-pattern="^.*$" \
  --build-config=cloudbuild.yaml \
  --description="Build RAG services on PRs" \
  --comment-control=COMMENTS_ENABLED
```

## Cost Optimization

### Free Tier
- First 120 build-minutes per day: FREE
- After that: $0.003 per build-minute

### Tips to Reduce Costs:
1. Use build caching
2. Build only changed services
3. Use lower-tier machines for dev builds
4. Enable async builds for non-critical builds

### Example Cost:
- Full build (all 5 services): ~15 minutes
- Cost: FREE (within daily quota)
- Monthly cost (2 builds/day): FREE

## Summary

**For GCP Deployments:**
1. ✅ **Use Cloud Build** - It's designed for this use case
2. ✅ Tag images properly for traceability
3. ✅ Enable vulnerability scanning
4. ✅ Use specific versions in production
5. ✅ Set up automated builds from Git

**Avoid:**
- ❌ Building on ARM Macs without platform specification
- ❌ Using `latest` tag in production
- ❌ Skipping vulnerability scans
- ❌ Manual image building for production

## Next Steps

1. Build images with Cloud Build:
   ```bash
   make gcp-cloud-build ENV=dev
   ```

2. Deploy to GKE:
   ```bash
   make gcp-deploy ENV=dev
   ```

3. Set up automated builds:
   ```bash
   gcloud builds triggers create github --repo-name=RAG --repo-owner=texican --branch-pattern="^main$" --build-config=cloudbuild.yaml
   ```

4. Monitor deployments:
   ```bash
   kubectl get pods -n rag-system -w
   ```

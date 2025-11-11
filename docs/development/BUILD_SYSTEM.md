# Cloud Build System Documentation

## Overview

The RAG system uses Google Cloud Build for building and deploying Docker images. We maintain three different build configurations optimized for different use cases.

## Build Configurations

### 1. cloudbuild.yaml - Fast Development Build

**Purpose**: Quick development iterations when you need to rebuild all services.

**Characteristics**:
- Builds all 5 services in parallel
- Uses Maven 3.9 with shared volume caching (`/root/.m2`)
- Single tag: `latest` only
- Build time: ~3 minutes
- Docker context: Root directory (`.`)

**Usage**:
```bash
# Build all services
gcloud builds submit --config=cloudbuild.yaml --project=byo-rag-dev

# Use --async to not wait for completion
gcloud builds submit --config=cloudbuild.yaml --project=byo-rag-dev --async
```

**When to use**:
- Development work affecting multiple services
- After updating `rag-shared` library
- Quick verification that all services still build

---

### 2. cloudbuild-single.yaml - Single Service Build

**Purpose**: Fastest builds when working on a single service.

**Characteristics**:
- Builds only the specified service
- Uses Maven 3.9 with shared volume caching
- Single tag: `latest` only
- Build time: ~1 minute
- Respects `_SERVICE_NAME` substitution

**Usage**:
```bash
# Build only embedding service
gcloud builds submit \
  --config=cloudbuild-single.yaml \
  --substitutions=_SERVICE_NAME=rag-embedding \
  --project=byo-rag-dev

# Build only auth service
gcloud builds submit \
  --config=cloudbuild-single.yaml \
  --substitutions=_SERVICE_NAME=rag-auth \
  --project=byo-rag-dev

# Other services: rag-document, rag-core, rag-admin
```

**When to use**:
- Iterative development on a single service
- Testing fixes for specific services
- Rapid prototyping
- CI/CD for service-specific branches

---

### 3. cloudbuild-full.yaml - Production Build

**Purpose**: Production releases with full versioning and artifact storage.

**Characteristics**:
- Builds all 5 services (ignores substitutions)
- Multiple tags per image: `latest`, `$SHORT_SHA`, `$BUILD_ID`
- Saves artifacts to GCS bucket
- Uses `gcr.io/cloud-builders/mvn` (standard builder)
- Build time: ~5-7 minutes
- Docker context: Service subdirectories (e.g., `./rag-auth-service`)

**Usage**:
```bash
# Production build with versioning
gcloud builds submit --config=cloudbuild-full.yaml --project=byo-rag-dev

# Automatic builds from Git
gcloud builds triggers create github \
  --repo-name=RAG \
  --repo-owner=texican \
  --branch-pattern="^main$" \
  --build-config=cloudbuild-full.yaml
```

**When to use**:
- Production releases
- Release candidates
- Git-triggered CI/CD builds
- When you need versioned images for rollback

---

## Build Configuration Comparison

| Feature | cloudbuild.yaml | cloudbuild-single.yaml | cloudbuild-full.yaml |
|---------|----------------|----------------------|---------------------|
| **Services Built** | All 5 (always) | 1 (specified) | All 5 (always) |
| **Build Time** | ~3 min | ~1 min | ~7 min |
| **Maven Cache** | ✅ Shared volume | ✅ Shared volume | ❌ No cache |
| **Tags per Image** | 1 (latest) | 1 (latest) | 3 (latest, SHA, BUILD_ID) |
| **Artifacts Saved** | ❌ No | ❌ No | ✅ Yes (GCS) |
| **Respects Substitutions** | ❌ No | ✅ Yes | ❌ No |
| **Best For** | Dev (all services) | Dev (one service) | Production |

## Service Names

When using `cloudbuild-single.yaml`, use these service names:
- `rag-auth` - Authentication service
- `rag-document` - Document processing service
- `rag-embedding` - Embedding generation service
- `rag-core` - Core RAG orchestration service
- `rag-admin` - Administration service

## Architecture Notes

### Why Maven Cache Matters

The shared Maven volume (`/root/.m2`) dramatically speeds up builds:
- **First build**: Downloads all dependencies (~2-3 min)
- **Subsequent builds**: Reuses cached dependencies (~30 sec)

Only `cloudbuild.yaml` and `cloudbuild-single.yaml` use this optimization.

### Docker Context Differences

**Root context (`.`)** - Used by `cloudbuild.yaml` and `cloudbuild-single.yaml`:
```yaml
- name: 'gcr.io/cloud-builders/docker'
  args:
    - 'build'
    - '-f'
    - 'rag-embedding-service/Dockerfile'
    - '.'  # Root context
```
- ✅ All files accessible to Docker
- ✅ Works with multi-module Maven projects
- ✅ Dockerfile can reference parent directories

**Service subdirectory** - Used by `cloudbuild-full.yaml`:
```yaml
- name: 'gcr.io/cloud-builders/docker'
  args:
    - 'build'
    - '-f'
    - 'rag-embedding-service/Dockerfile'
    - './rag-embedding-service'  # Service context
```
- ⚠️ Only service files accessible
- ⚠️ May require copying JAR files into context

## Deployment After Build

After a successful build, deploy the new image:

```bash
# Restart deployment to pull latest image
kubectl rollout restart deployment/rag-embedding -n rag-system

# Watch deployment progress
kubectl rollout status deployment/rag-embedding -n rag-system

# Check pod status
kubectl get pods -n rag-system -l app=rag-embedding

# View logs of new pod
kubectl logs -n rag-system -l app=rag-embedding --tail=50
```

## Troubleshooting

### Build Fails with "Unable to read file [cloudbuild.yaml]"

**Problem**: Working directory issue

**Solution**: Always run from project root:
```bash
cd /Users/stryfe/Projects/RAG_SpecKit/RAG
gcloud builds submit --config=cloudbuild.yaml --project=byo-rag-dev
```

### Build Times Out

**Problem**: Default timeout too short

**Solution**: All configs have 1-hour timeout, but you can override:
```bash
gcloud builds submit \
  --config=cloudbuild-single.yaml \
  --timeout=2h \
  --project=byo-rag-dev
```

### Maven Dependencies Not Found

**Problem**: `rag-shared` not built first

**Solution**: All configs build `rag-shared` first automatically. If still failing:
```bash
# Manually build and install rag-shared
mvn clean install -pl rag-shared -am
```

### Wrong Architecture (ARM64 vs x86_64)

**Problem**: Local Docker build creates ARM64 images (Mac M1/M2)

**Solution**: Always use Cloud Build - it automatically builds x86_64 for GKE

### Old Image Still Running

**Problem**: Kubernetes caching old image

**Solution**: Force pull new image:
```bash
# Delete old pods
kubectl delete pods -n rag-system -l app=rag-embedding

# Or rollout restart
kubectl rollout restart deployment/rag-embedding -n rag-system
```

## Build Performance Tips

1. **Use `cloudbuild-single.yaml` for single-service work**
   - 3x faster than building all services
   - Maven cache speeds up subsequent builds

2. **Use `--async` for background builds**
   ```bash
   gcloud builds submit --config=cloudbuild-single.yaml \
     --substitutions=_SERVICE_NAME=rag-embedding \
     --project=byo-rag-dev \
     --async
   
   # Check status later
   gcloud builds list --limit=1 --project=byo-rag-dev
   ```

3. **Parallel development**
   - Each service can be built independently
   - Multiple team members can trigger builds simultaneously

4. **Local testing before Cloud Build**
   ```bash
   # Test Maven build locally first
   mvn clean package -pl rag-embedding-service -am -DskipTests
   
   # Then trigger Cloud Build
   gcloud builds submit --config=cloudbuild-single.yaml \
     --substitutions=_SERVICE_NAME=rag-embedding
   ```

## CI/CD Integration

### GitHub Triggers (Recommended for Production)

```bash
# Create trigger for main branch
gcloud builds triggers create github \
  --repo-name=RAG \
  --repo-owner=texican \
  --branch-pattern="^main$" \
  --build-config=cloudbuild-full.yaml \
  --project=byo-rag-dev

# Create trigger for develop branch (fast builds)
gcloud builds triggers create github \
  --repo-name=RAG \
  --repo-owner=texican \
  --branch-pattern="^develop$" \
  --build-config=cloudbuild.yaml \
  --project=byo-rag-dev
```

### Manual Triggers (Development)

```bash
# Quick iteration cycle
while true; do
  # Make code changes
  vim rag-embedding-service/src/main/java/...
  
  # Build
  gcloud builds submit \
    --config=cloudbuild-single.yaml \
    --substitutions=_SERVICE_NAME=rag-embedding \
    --project=byo-rag-dev
  
  # Deploy
  kubectl rollout restart deployment/rag-embedding -n rag-system
  
  # Test
  kubectl logs -n rag-system -l app=rag-embedding --tail=50
done
```

## Cost Optimization

Cloud Build costs based on build-minutes:
- **Free tier**: 120 build-minutes/day
- **Standard**: $0.003/build-minute

**Estimated costs**:
- `cloudbuild-single.yaml`: ~1 min = $0.003
- `cloudbuild.yaml`: ~3 min = $0.009
- `cloudbuild-full.yaml`: ~7 min = $0.021

**Tips**:
- Use single-service builds during development
- Build all services only when necessary
- Use `--async` to avoid blocking terminal

## Related Documentation

- [Deployment Guide](./DEPLOYMENT.md)
- [Kubernetes Configuration](../deployment/kubernetes-setup.md)
- [Docker Best Practices](./DOCKER_BEST_PRACTICES.md)
- [Maven Multi-Module Setup](./MAVEN_STRUCTURE.md)

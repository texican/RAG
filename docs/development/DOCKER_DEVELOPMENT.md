---
version: 1.0.0
last-updated: 2025-11-12
status: active
applies-to: 0.8.0-SNAPSHOT
category: development
---

# Docker Development Guide

This guide helps avoid common Docker pitfalls during development.

## Common Issues and Solutions

### Issue 1: Image Name Inconsistencies

**Problem:** Building images with `docker build -t name:tag` but docker-compose uses different image names.

**Solution:** Use the explicit `image:` field in docker-compose.yml (already configured):

```yaml
rag-auth:
  image: rag-auth:latest  # Explicit image name
  build:
    context: .
    dockerfile: rag-auth-service/Dockerfile
```

### Issue 2: Containers Not Using New Images

**Problem:** `docker restart` doesn't reload images, and `docker-compose up -d` may not recreate containers.

**Solution:** Use the rebuild script:

```bash
# Rebuild a specific service
./scripts/dev/rebuild-service.sh rag-auth

# Rebuild with no cache (for stubborn issues)
./scripts/dev/rebuild-service.sh rag-auth --no-cache
```

The script does the proper sequence:
1. Builds JAR with Maven
2. Builds Docker image with correct name
3. Stops and removes old container
4. Creates new container from new image
5. Waits for health check

### Issue 3: Docker Image Cache Issues

**Problem:** Code changes not reflected even after rebuild.

**Causes:**
- Old JAR files in target/ directory
- Docker layer caching
- Wrong image being used

**Solutions:**

```bash
# Option 1: Use rebuild script with --no-cache
./scripts/dev/rebuild-service.sh rag-auth --no-cache

# Option 2: Manual deep clean
mvn clean -pl rag-auth-service
docker rmi rag-auth:latest
docker-compose build --no-cache rag-auth
docker-compose up -d --force-recreate rag-auth
```

### Issue 4: Multiple Image Versions

**Problem:** Multiple versions of same image with different tags/names.

**Prevention:**
- Always use the rebuild script
- Don't manually tag images unless necessary
- Use `docker images | grep <service>` to check for duplicates

**Cleanup:**

```bash
# List all images for a service
docker images | grep rag-auth

# Remove old/unused images
docker rmi <image-id>

# Remove all unused images
docker image prune -a
```

## Best Practices

### During Development

1. **Use the rebuild script** instead of manual docker commands
   ```bash
   ./scripts/dev/rebuild-service.sh rag-auth
   ```

2. **Check container is using correct image:**
   ```bash
   docker ps --format "{{.Image}} {{.Names}}"
   ```

3. **View recent container logs:**
   ```bash
   docker logs rag-auth --tail 50 --follow
   ```

4. **Check health status:**
   ```bash
   docker ps | grep rag-auth
   # Look for "(healthy)" in status column
   ```

### When Things Go Wrong

1. **Verify JAR was rebuilt:**
   ```bash
   ls -lh rag-auth-service/target/*.jar
   # Check timestamp matches your build time
   ```

2. **Check which image container is using:**
   ```bash
   docker inspect rag-auth --format='{{.Image}}'
   docker images rag-auth:latest --format='{{.ID}}'
   # These should match!
   ```

3. **Nuclear option - complete rebuild:**
   ```bash
   # Stop everything
   docker-compose down

   # Clean Maven build
   mvn clean

   # Remove all project images
   docker images | grep "rag-" | awk '{print $3}' | xargs docker rmi -f

   # Rebuild from scratch
   mvn clean package -DskipTests
   docker-compose build --no-cache
   docker-compose up -d
   ```

## Quick Reference Commands

```bash
# Build single service
./scripts/dev/rebuild-service.sh <service-name>

# Build with no cache
./scripts/dev/rebuild-service.sh <service-name> --no-cache

# View logs
docker logs <service-name> --tail 100 --follow

# Check status
docker ps | grep <service-name>

# Restart service (doesn't reload image!)
docker restart <service-name>

# Check container image vs built image
docker inspect <service-name> --format='{{.Image}}' | cut -d: -f2 | cut -c1-12
docker images <image-name>:latest --format='{{.ID}}'

# Access container shell
docker exec -it <service-name> sh

# View container health
docker inspect <service-name> --format='{{.State.Health.Status}}'
```

## Service Names

- `rag-auth` - Authentication Service (port 8081)
- `rag-document` - Document Service (port 8082)
- `rag-embedding` - Embedding Service (port 8083)
- `rag-core` - Core Service (port 8084)
- `rag-admin` - Admin Service (port 8085)

## Workflow Example

```bash
# 1. Make code changes to Auth service
vim rag-auth-service/src/main/java/...

# 2. Rebuild and restart
./scripts/dev/rebuild-service.sh rag-auth

# 3. Watch logs to verify changes
docker logs rag-auth --tail 50 --follow

# 4. Test the service
curl http://localhost:8081/api/v1/auth/register -X POST -H 'Content-Type: application/json' \
  -d '{"email":"test@example.com","password":"password123","firstName":"Test","lastName":"User"}'
```

## Troubleshooting Checklist

- [ ] Did you build the JAR first with Maven?
- [ ] Is the JAR timestamp recent in `target/` directory?
- [ ] Did you stop and remove the old container?
- [ ] Is the container using the correct image ID?
- [ ] Did you check container logs for errors?
- [ ] Is the health check passing?
- [ ] Are you running commands from the correct directory (RAG/)?

## Why Not Just `docker-compose up --build`?

While `docker-compose up --build` works, it has limitations:

1. Doesn't rebuild JARs with Maven automatically
2. May use cached layers even with code changes
3. Doesn't force container recreation
4. Less visibility into what's happening

The rebuild script handles all these cases properly.

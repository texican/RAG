# Contributing to RAG System

Thank you for contributing! Please follow these guidelines to ensure smooth development.

## 🚫 Deprecated Practices

The following practices are **deprecated** and will fail CI checks:

### ❌ DON'T: Manual Docker Commands

```bash
# ❌ Don't do this
cd rag-auth-service
mvn clean package
docker build -t some-name .
docker stop rag-auth
docker rm rag-auth
docker run ...
```

**Why:** This workflow is error-prone, creates inconsistently named images, and wastes time.

### ❌ DON'T: Use docker restart for Code Changes

```bash
# ❌ Don't do this after making code changes
docker restart rag-auth
```

**Why:** `docker restart` doesn't reload the image. Your code changes won't be reflected.

### ❌ DON'T: Build images with custom names

```bash
# ❌ Don't do this
docker build -t my-custom-name .
docker build -t docker-rag-auth .
docker build -t rag_rag-auth .
```

**Why:** docker-compose expects specific image names. Custom names cause confusion.

## ✅ Correct Practices

### ✅ DO: Use the Rebuild Script or Makefile

```bash
# ✅ Do this
make rebuild SERVICE=rag-auth

# Or directly
./scripts/dev/rebuild-service.sh rag-auth

# With no cache if needed
make rebuild-nc SERVICE=rag-auth
```

**Why:** This handles the entire workflow correctly: builds JAR, builds image, stops old container, starts new one.

### ✅ DO: Use Make Commands for Development

```bash
# View logs
make logs SERVICE=rag-auth

# Check status
make status

# Start/stop all services
make start
make stop

# Run tests
make test SERVICE=rag-auth

# See all commands
make help
```

### ✅ DO: Follow the Documented Workflow

See [docs/development/DOCKER_DEVELOPMENT.md](docs/development/DOCKER_DEVELOPMENT.md) for comprehensive guidance.

## Development Workflow

### 1. Make Code Changes

```bash
# Edit files as needed
vim rag-auth-service/src/main/java/...
```

### 2. Rebuild the Service

```bash
make rebuild SERVICE=rag-auth
```

This command will:
- ✅ Build the JAR with Maven
- ✅ Build Docker image with correct name
- ✅ Stop and remove old container
- ✅ Start new container
- ✅ Wait for health check

### 3. Verify Changes

```bash
# Watch the logs
make logs SERVICE=rag-auth

# Test the endpoint
curl http://localhost:8081/api/v1/auth/...
```

### 4. Run Tests

```bash
# Run tests for your service
make test SERVICE=rag-auth

# Or all tests
make test
```

### 5. Commit Changes

```bash
git add .
git commit -m "feat: your change description"
git push
```

## CI/CD Validation

Pull requests will be validated for:
- ✅ All services build successfully
- ✅ Docker images use correct names (rag-auth:latest, etc.)
- ✅ No incorrectly named images exist
- ✅ rebuild-service.sh is executable
- ✅ Required documentation exists
- ✅ Makefile has all required targets

## Pre-commit Hooks (Optional but Recommended)

Install the git hooks:

```bash
# Link the pre-push hook
ln -sf ../../.githooks/pre-push .git/hooks/pre-push
chmod +x .git/hooks/pre-push
```

This will warn you if you have Docker images with incorrect names.

## Docker Wrapper (Optional)

For extra safety, you can install the Docker wrapper:

```bash
# Add to ~/.bashrc or ~/.zshrc
alias docker='~/path/to/RAG/scripts/utils/docker-wrapper.sh'
```

This will prompt you when you try to use problematic Docker commands in the RAG project.

## Service Names Reference

Always use these exact names:

| Service | Image Name | Container Name | Port |
|---------|------------|----------------|------|
| Authentication | `rag-auth:latest` | `rag-auth` | 8081 |
| Document | `rag-document:latest` | `rag-document` | 8082 |
| Embedding | `rag-embedding:latest` | `rag-embedding` | 8083 |
| Core | `rag-core:latest` | `rag-core` | 8084 |
| Admin | `rag-admin:latest` | `rag-admin` | 8085 |

## Troubleshooting

### "My code changes aren't showing up"

1. Did you rebuild? `make rebuild SERVICE=rag-auth`
2. Try with no cache: `make rebuild-nc SERVICE=rag-auth`
3. Check the JAR timestamp: `ls -lh rag-auth-service/target/*.jar`
4. Verify container is using correct image:
   ```bash
   docker ps --format "{{.Image}} {{.Names}}" | grep rag-auth
   # Should show: rag-auth:latest   rag-auth
   ```

### "Container won't start"

1. Check logs: `make logs SERVICE=rag-auth`
2. Check health: `docker ps | grep rag-auth`
3. See [docs/development/DOCKER_DEVELOPMENT.md](docs/development/DOCKER_DEVELOPMENT.md)

### "I need to do something the rebuild script doesn't support"

That's fine! You can still use Docker commands directly. Just:
1. Be careful with image naming
2. Document what you're doing in your PR
3. Update the rebuild script if this is a common case

## Questions?

- 📖 Read [docs/development/DOCKER_DEVELOPMENT.md](docs/development/DOCKER_DEVELOPMENT.md)
- 📖 Check [CLAUDE.md](CLAUDE.md) for current project state
- 💬 Ask in the team chat
- 🐛 Open an issue

## Summary

**Golden Rule:** Use `make rebuild SERVICE=name` for all development.

Everything else follows from this. If you follow this rule, you'll avoid 95% of Docker issues.

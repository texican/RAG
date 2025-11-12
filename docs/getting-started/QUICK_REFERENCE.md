---
version: 1.0.0
last-updated: 2025-11-12
status: active
applies-to: 0.8.0-SNAPSHOT
category: getting-started
---

# RAG Development Quick Reference

> 📋 Keep this handy! Bookmark or print it.

## 🚀 Most Common Commands

```bash
# Rebuild a service after code changes
make rebuild SERVICE=rag-auth

# View logs in real-time
make logs SERVICE=rag-auth

# Check all services status
make status

# Get JWT token for Admin service
./scripts/utils/admin-login.sh

# Launch all Swagger UIs with passwords
./scripts/utils/launch-swagger.sh
```

## 📦 Service Names & Ports

| Service | Name | Port | URL |
|---------|------|------|-----|
| Auth | `rag-auth` | 8081 | http://localhost:8081 |
| Document | `rag-document` | 8082 | http://localhost:8082 |
| Embedding | `rag-embedding` | 8083 | http://localhost:8083 |
| Core | `rag-core` | 8084 | http://localhost:8084 |
| Admin | `rag-admin` | 8085 | http://localhost:8085 |

## 🔧 Make Commands Cheat Sheet

```bash
# Service Management
make rebuild SERVICE=name        # Rebuild service (JAR + Docker + restart)
make rebuild-nc SERVICE=name     # Rebuild with --no-cache
make restart SERVICE=name        # Just restart (no rebuild - rarely needed)
make logs SERVICE=name           # Follow logs

# System Management
make start                       # Start all services
make stop                        # Stop all services
make restart-all                # Restart everything
make status                      # Show service status

# Building
make build-all                   # Build all JARs with Maven
make test                        # Run all tests
make test SERVICE=name           # Run tests for one service

# Cleanup
make clean                       # Clean Maven builds
make clean-docker                # Remove all Docker images (⚠️ careful!)
make clean-all                   # Clean everything

# Help
make help                        # Show all commands
```

## 🔑 Authentication Quick Reference

### Basic Auth Services (Embedding, Core)
```bash
# Get password from Docker logs
docker logs rag-embedding 2>&1 | grep "generated security password"

# Or use the helper script
./scripts/utils/launch-swagger.sh
# Shows all passwords
```

### JWT Auth Services (Auth, Admin)
```bash
# For Auth service - registration
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{
    "email":"test@example.com",
    "password":"password123",
    "firstName":"Test",
    "lastName":"User"
  }'

# For Admin service - get JWT token
./scripts/utils/admin-login.sh
# Outputs token and instructions
```

## 🌐 Swagger UI URLs

| Service | URL | Auth |
|---------|-----|------|
| Auth | http://localhost:8081/swagger-ui.html | Spring Boot Basic Auth |
| Document | http://localhost:8082/swagger-ui.html | None (public) |
| Embedding | http://localhost:8083/swagger-ui.html | Spring Boot Basic Auth |
| Core | http://localhost:8084/swagger-ui.html | Spring Boot Basic Auth |
| Admin | http://localhost:8085/admin/api/swagger-ui.html | JWT (use admin-login.sh) |

## 🐛 Common Issues & Fixes

### "My code changes aren't showing up"
```bash
# Rebuild with no cache
make rebuild-nc SERVICE=rag-auth
```

### "Service won't start"
```bash
# Check logs
make logs SERVICE=rag-auth

# Check health
docker ps | grep rag-auth
```

### "Port already in use"
```bash
# Find what's using the port
lsof -i :8081

# Stop the service
make stop
```

### "Everything is broken"
```bash
# Nuclear option - complete rebuild
make clean-all
make build-all
make start
```

## 📝 Git Workflow

```bash
# 1. Make changes
vim rag-auth-service/src/main/java/...

# 2. Rebuild
make rebuild SERVICE=rag-auth

# 3. Test
curl http://localhost:8081/...

# 4. Commit
git add .
git commit -m "feat: your change"
git push
```

## 🔍 Debugging Commands

```bash
# View recent logs
docker logs rag-auth --tail 50

# Follow logs
docker logs rag-auth --follow

# Check container status
docker ps | grep rag-auth

# Check container image
docker inspect rag-auth --format='{{.Image}}'

# Check health
docker inspect rag-auth --format='{{.State.Health.Status}}'

# Access container shell
docker exec -it rag-auth sh

# Check Java process in container
docker exec rag-auth ps aux
```

## 🌡️ Health Checks

```bash
# Run health check script
./scripts/utils/health-check.sh

# Check individual service
curl http://localhost:8081/actuator/health

# Check all services
for port in 8081 8082 8083 8084 8085; do
  echo "Port $port:"
  curl -s http://localhost:$port/actuator/health | jq .status
done
```

## 📊 Monitoring

```bash
# View all containers
docker ps

# View resource usage
docker stats

# View logs from all services
docker-compose logs -f
```

## 🚫 What NOT to Do

```bash
# ❌ DON'T use docker restart for code changes
docker restart rag-auth

# ✅ DO use make rebuild
make rebuild SERVICE=rag-auth

# ❌ DON'T build images manually
docker build -t some-name .

# ✅ DO use make rebuild
make rebuild SERVICE=rag-auth

# ❌ DON'T run mvn package without rebuilding container
mvn package -pl rag-auth-service

# ✅ DO use make rebuild (it runs mvn + docker)
make rebuild SERVICE=rag-auth
```

## 🔗 Documentation Links

- [CONTRIBUTING.md](../CONTRIBUTING.md) - Development workflow
- [docs/DOCKER_DEVELOPMENT.md](DOCKER_DEVELOPMENT.md) - Docker guide
- [CLAUDE.md](../CLAUDE.md) - Current project state
- [README.md](../README.md) - Project overview

## 💡 Pro Tips

1. **Alias for common commands:**
   ```bash
   alias mr='make rebuild SERVICE='
   alias ml='make logs SERVICE='
   # Then: mr rag-auth
   ```

2. **Watch logs in split terminal:**
   ```bash
   # In one pane
   make logs SERVICE=rag-auth

   # In another pane
   curl http://localhost:8081/...
   ```

3. **Quick status check:**
   ```bash
   make status | grep -i healthy
   ```

4. **Test before committing:**
   ```bash
   make test SERVICE=rag-auth && git commit
   ```

5. **Bulk operations:**
   ```bash
   # Rebuild multiple services
   for svc in rag-auth rag-admin; do
     make rebuild SERVICE=$svc
   done
   ```

## 🆘 Getting Help

1. Check this reference
2. Read [DOCKER_DEVELOPMENT.md](DOCKER_DEVELOPMENT.md)
3. Run `make help`
4. Check logs: `make logs SERVICE=name`
5. Ask in team chat
6. Open GitHub issue

---

**Print this page and keep it at your desk!**

Last Updated: 2025-10-01

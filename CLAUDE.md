# Claude Context - RAG Project Current State

Last Updated: 2025-10-01

## Tool Choice: Why Make?

**Decision:** Use GNU Make as the task runner instead of npm scripts, Gradle, Task, or Just.

**Rationale:**
- ✅ Pre-installed on all Unix systems (zero setup)
- ✅ Perfect for shell/Docker/Maven workflows
- ✅ Tab completion works out of the box
- ✅ Everyone knows `make` - minimal learning curve
- ✅ Excellent IDE support (VS Code, IntelliJ, etc.)
- ✅ Self-documenting with `make help`
- ✅ Fast - no runtime overhead
- ✅ Standard for system-level projects

**Score:** Make: 53/53 points vs npm: 31, Task: 26, Just: 23, Gradle: 27

See [docs/development/MAKE_VS_ALTERNATIVES.md](docs/development/MAKE_VS_ALTERNATIVES.md) for detailed comparison.

## Recent Session Summary

### Completed Work

#### 1. Fixed Auth Service Registration & Login (✅ COMPLETE)
**Problem:** Auth service registration endpoint was failing with "id must not be null" error, then transaction rollback errors.

**Root Causes:**
- `UserService.createUser()` called `tenantService.findBySlug()` which threw exception when tenant didn't exist
- Exception in nested `@Transactional` method marked entire transaction for rollback
- Spring Security's `/error` endpoint wasn't in `permitAll()` list, causing 403 responses

**Solution:**
- Added `findBySlugOptional()` method to `TenantService` that returns `Optional<Tenant>` without throwing exceptions
- Updated `UserService` to use Optional pattern instead of try-catch
- Added `/error` endpoint to Spring Security `permitAll()` list in `SecurityConfig`

**Files Modified:**
- `rag-auth-service/src/main/java/com/byo/rag/auth/service/UserService.java` - Line 136
- `rag-auth-service/src/main/java/com/byo/rag/auth/service/TenantService.java` - Line 365 (new method)
- `rag-auth-service/src/main/java/com/byo/rag/auth/config/SecurityConfig.java` - Line 152

**Verification:**
```bash
# Registration now works:
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"test@example.com","password":"password123","firstName":"Test","lastName":"User"}'

# Returns user with tenant info
```

#### 2. Fixed Admin Service Swagger UI Access (✅ COMPLETE)
**Problem:** Admin Swagger UI returned 401 Unauthorized even though Swagger endpoints were in `permitAll()`.

**Root Cause:**
- Admin service has context path `/admin/api`
- Requests come in as `/admin/api/swagger-ui.html`
- But Spring Security matches against servlet path (after context path): `/swagger-ui.html`
- The security config was correct, but container was using old Docker image

**Solution:**
- Added explicit patterns for both with and without context path (defensive)
- Fixed Docker image naming in `docker-compose.yml` (see #3 below)
- Rebuilt and restarted admin service properly

**Files Modified:**
- `rag-admin-service/src/main/java/com/byo/rag/admin/config/AdminSecurityConfig.java` - Line 50

**Verification:**
```bash
# Swagger UI now accessible:
curl -I http://localhost:8085/admin/api/swagger-ui.html
# Returns: HTTP/1.1 302 (redirect to UI)
```

#### 3. Docker Development Workflow Improvements (✅ COMPLETE)
**Problem:** Multiple Docker issues causing development friction:
- Image naming inconsistencies (rag_rag-auth vs docker-rag-auth vs rag-auth)
- Containers not picking up new images after rebuild
- `docker restart` doesn't reload images
- `docker-compose up -d` wouldn't recreate containers
- Manual workflow required 8+ commands and was error-prone

**Solution - Part 1: Explicit Image Names**
Updated `config/docker/docker-compose.yml` to specify explicit image names for all services:
```yaml
rag-auth:
  image: rag-auth:latest  # Now explicit
  build:
    context: .
    dockerfile: rag-auth-service/Dockerfile
```

**Solution - Part 2: Rebuild Script**
Created `scripts/dev/rebuild-service.sh` that does proper rebuild workflow:
1. Builds JAR with Maven
2. Builds Docker image with correct name
3. Stops and removes old container
4. Creates new container from new image
5. Waits for health check

**Solution - Part 3: Makefile**
Created `Makefile` with convenient commands:
```bash
make rebuild SERVICE=rag-auth
make logs SERVICE=rag-auth
make status
```

**Solution - Part 4: Documentation**
- Created `docs/development/DOCKER_DEVELOPMENT.md` - Comprehensive developer guide
- Created `docs/deployment/DOCKER_IMPROVEMENTS_SUMMARY.md` - Summary of improvements
- Updated `README.md` with new workflow

**Files Created:**
- `RAG/scripts/dev/rebuild-service.sh` (executable)
- `RAG/Makefile`
- `RAG/docs/development/DOCKER_DEVELOPMENT.md`
- `RAG/docs/deployment/DOCKER_IMPROVEMENTS_SUMMARY.md`

**Files Modified:**
- `RAG/config/docker/docker-compose.yml` - Added explicit image names for all services
- `RAG/README.md` - Updated setup instructions

#### 4. Utility Scripts Verification (✅ WORKING)
**Scripts Tested and Working:**
- `scripts/utils/admin-login.sh` - Registers/finds admin user, logs in, returns JWT token
- `scripts/utils/launch-swagger.sh` - Extracts Spring Boot passwords, displays credentials for all services
- `scripts/utils/health-check.sh` - Checks PostgreSQL/Redis/Kafka health
- `scripts/utils/service-status.sh` - Shows all RAG service statuses

## Current System State

### All Services Status
- ✅ **rag-auth** (8081) - Healthy, registration and login working
- ✅ **rag-document** (8082) - Healthy
- ✅ **rag-embedding** (8083) - Healthy
- ✅ **rag-core** (8084) - Healthy
- ✅ **rag-admin** (8085) - Healthy, Swagger UI accessible
- ✅ **PostgreSQL** - Healthy
- ✅ **Redis** - Healthy
- ✅ **Kafka** - Healthy

### Swagger UI Access

| Service | URL | Auth Required |
|---------|-----|---------------|
| Auth | http://localhost:8081/swagger-ui.html | No (Spring Boot Basic Auth) |
| Document | http://localhost:8082/swagger-ui.html | No (public) |
| Embedding | http://localhost:8083/swagger-ui.html | Spring Boot Basic Auth |
| Core | http://localhost:8084/swagger-ui.html | Spring Boot Basic Auth |
| Admin | http://localhost:8085/admin/api/swagger-ui.html | JWT (use admin-login.sh) |

### Quick Commands for Next Session

```bash
# Check all services status
make status

# Rebuild a service
make rebuild SERVICE=rag-auth

# Get admin JWT token
./scripts/utils/admin-login.sh

# Launch all Swagger UIs with credentials
./scripts/utils/launch-swagger.sh

# View logs
make logs SERVICE=rag-auth

# Test auth registration
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"test@example.com","password":"password123","firstName":"Test","lastName":"User"}'
```

## Known Issues

### None Currently
All blocking issues have been resolved.

## Architecture Decisions

### ADR-001: Bypass API Gateway
API Gateway has been archived. All services accessed directly:
- Auth: 8081
- Document: 8082
- Embedding: 8083
- Core: 8084
- Admin: 8085

### Service Ports
- **Auth Service:** 8081 ✅ (not 8080)
- **Document Service:** 8082 ✅
- **Embedding Service:** 8083 ✅
- **Core Service:** 8084 ✅
- **Admin Service:** 8085 ✅ (not 8086)

All scripts and documentation have been updated to use correct ports.

## Development Workflow

### For Code Changes

1. Make your code changes
2. Rebuild the service:
   ```bash
   make rebuild SERVICE=rag-auth
   ```
3. Verify with logs:
   ```bash
   make logs SERVICE=rag-auth
   ```

### For Stubborn Issues

1. Try rebuild with no cache:
   ```bash
   make rebuild-nc SERVICE=rag-auth
   ```

2. If still broken, nuclear option:
   ```bash
   make clean-all
   make build-all
   make start
   ```

### Debugging Docker Issues

See `docs/development/DOCKER_DEVELOPMENT.md` for comprehensive troubleshooting guide.

## Files to Commit

The following files have been modified and should be committed:

**Auth Service Fixes:**
- `rag-auth-service/src/main/java/com/byo/rag/auth/service/UserService.java`
- `rag-auth-service/src/main/java/com/byo/rag/auth/service/TenantService.java`
- `rag-auth-service/src/main/java/com/byo/rag/auth/config/SecurityConfig.java`

**Admin Service Fixes:**
- `rag-admin-service/src/main/java/com/byo/rag/admin/config/AdminSecurityConfig.java`

**Docker Improvements:**
- `config/docker/docker-compose.yml`
- `scripts/dev/rebuild-service.sh` (new)
- `Makefile` (new)
- `docs/development/DOCKER_DEVELOPMENT.md` (new)
- `docs/deployment/DOCKER_IMPROVEMENTS_SUMMARY.md` (new)
- `README.md`
- `CLAUDE.md` (this file - new)

## Next Steps / TODO

### Potential Future Work
- [ ] Run full test suite to ensure nothing broke
- [ ] Test document upload workflow end-to-end
- [ ] Test embedding generation workflow
- [ ] Test RAG query workflow
- [ ] Add integration tests for auth registration flow
- [ ] Consider hot-reload for faster development (Spring Boot DevTools)

### Documentation
- ✅ Docker development guide
- ✅ README updated with new commands
- ✅ Scripts are documented and working
- [ ] Consider adding troubleshooting section to README

## Important Notes for Future Sessions

1. **Always use the rebuild script or Makefile** - Don't manually build Docker images
2. **Docker image names are now consistent** - All services use pattern `rag-{service}:latest`
3. **The auth service creates default tenant automatically** - No manual tenant creation needed
4. **Admin Swagger UI works** - Use `admin-login.sh` to get JWT token
5. **All utility scripts are functional** - They've been tested and work correctly

## Context for AI Assistants

When continuing work on this project:
- Development happens in the `RAG/` subdirectory
- Use `make rebuild SERVICE=name` for rebuilding services
- Check `make help` for all available commands
- Consult `docs/development/DOCKER_DEVELOPMENT.md` for Docker workflow
- All services use JWT authentication except Document service (public)
- Default tenant is auto-created on first user registration
- Spring Boot Basic Auth passwords are in Docker logs (use `launch-swagger.sh`)

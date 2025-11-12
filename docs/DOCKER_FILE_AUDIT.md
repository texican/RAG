---
version: 1.0.0
last-updated: 2025-11-12
status: active
applies-to: 0.8.0-SNAPSHOT
category: operations
---

---
version: 1.0.0
last-updated: 2025-11-12
status: archived
applies-to: 0.8.0-SNAPSHOT
category: documentation
---

# Docker Files Location Audit

Last Updated: 2025-10-01

## Executive Summary

**Current State:** Docker files are well-organized and all issues resolved.

**Audit Complete (2025-10-01) - Major Reorganization:**
1. ✅ Old docker-compose files archived to `archive/docker-old-2025-09/`
2. ✅ Old docker health/start scripts archived to `archive/docker-old-2025-09/`
3. ✅ **Moved `docker-compose.yml` and `.env` to project root** (standard convention)
4. ✅ Dockerfiles correctly located in service directories
5. ✅ **Removed entire `/config/` directory** (unused, duplicated Spring Boot configs)
6. ✅ Created `docker/README.md` to explain infrastructure config directory
7. ✅ Updated all documentation and scripts to reference new locations

## Complete File Inventory

### 🐳 Operational Docker Files

#### Dockerfiles (Service-Specific) ✅ CORRECT LOCATION

```
rag-*-service/Dockerfile         # ✅ Co-located with service code
├── rag-admin-service/Dockerfile
├── rag-auth-service/Dockerfile
├── rag-core-service/Dockerfile
├── rag-document-service/Dockerfile
└── rag-embedding-service/Dockerfile
```

**Status:** ✅ Perfect - Dockerfiles belong with their services
**Reasoning:** Each service owns its build process

#### Docker Compose Files

```
RAG/                                # Project root
├── docker-compose.yml              # ✅ ACTIVE - Main orchestration (MOVED 2025-10-01)
└── .env                            # ✅ Environment variables (MOVED 2025-10-01)
```

**Status:** ✅ **REORGANIZED** - Moved to project root (standard Docker Compose convention)
**Previous Location:** `config/docker/` (required `-f` flag)
**New Location:** Project root (no `-f` flag needed)
**Old files archived to:** `archive/docker-old-2025-09/`

#### Docker Ignore

```
.dockerignore                       # ✅ CORRECT - Project root
```

**Status:** ✅ Correct location (root of build context)

#### Infrastructure Config Files ✅ CORRECT

```
docker/                             # Infrastructure configs (mounted into containers)
├── README.md                       # ✅ Explains purpose of this directory
├── postgres/
│   └── init.sql                    # ✅ PostgreSQL initialization script
├── prometheus/
│   └── prometheus.yml              # ✅ Prometheus scraping config
└── grafana/
    ├── datasources/
    │   └── prometheus.yml          # ✅ Grafana datasource config
    └── dashboards/
        └── dashboard.yml           # ✅ Dashboard provisioning
```

**Status:** ✅ Well-organized - Files mounted into infrastructure containers
**Purpose:** Contains service configs that get volume-mounted at runtime
**Note:** Each microservice has its own `application.yml` in `src/main/resources/` (Spring Boot standard)

### 🛠️ Docker Scripts

#### Utility Scripts ✅ CORRECT

```
scripts/
├── deploy/docker-deploy.sh         # ✅ Deployment scripts
├── dev/rebuild-service.sh          # ✅ Development rebuild workflow
└── utils/
    ├── docker-wrapper.sh           # ✅ Development utility
    └── health-check.sh             # ✅ Health check script
```

**Status:** ✅ Clean - Old scripts archived, current scripts properly organized

### 📚 Docker Documentation

#### Development Guides ✅ WELL ORGANIZED

```
docs/development/
├── DOCKER_DEVELOPMENT.md           # ✅ Main Docker dev guide
├── GIT_AND_DOCKER.md              # ✅ Git/Docker relationship
└── MAKE_VS_ALTERNATIVES.md        # ✅ Tool choice rationale
```

#### Deployment Guides ✅ WELL ORGANIZED

```
docs/deployment/
├── DOCKER.md                       # ✅ Docker setup
├── DOCKER_IMPROVEMENTS_SUMMARY.md  # ✅ Docker improvements
├── ENFORCEMENT_MECHANISMS.md       # ✅ Enforcement strategy
└── DEPLOYMENT.md                   # ✅ General deployment
```

#### Quick Reference ✅ CORRECT

```
docs/getting-started/
└── QUICK_REFERENCE.md              # ✅ Docker commands included
```

#### Architecture Docs ✅ CORRECT

```
docs/architecture/
└── ENFORCEMENT_DIAGRAM.md          # ✅ Docker workflow diagrams
```

### 🔧 CI/CD Files

```
.github/workflows/
└── docker-validation.yml           # ✅ CORRECT - CI validation
```

### 🗑️ Archived Files

```
archive/
├── rag-gateway/Dockerfile          # ✅ Gateway archived per ADR-001
└── docker-old-2025-09/             # ✅ Old Docker files from 2025-09
    ├── docker-compose.fixed.yml    # Superseded by docker-compose.yml
    ├── docker-health.sh            # Superseded by scripts/utils/health-check.sh
    └── docker-start.sh             # Superseded by Makefile/scripts
```

## Current Organization Assessment

### ✅ What's Good

1. **Dockerfiles Co-located** - Each service has its Dockerfile in the service directory
2. **Documentation Organized** - Docker docs split by purpose (dev/deployment/architecture)
3. **Scripts Categorized** - Deploy scripts in `scripts/deploy/`, utils in `scripts/utils/`
4. **CI/CD Separated** - Validation in `.github/workflows/`
5. **Archive Clean** - Old gateway Dockerfile properly archived

### ✅ All Issues Resolved

#### 1. Old Config Files - RESOLVED ✅

**Action Taken:** Archived old files to `archive/docker-old-2025-09/`

```
BEFORE:
config/docker/
├── docker-compose.yml
├── docker-compose.fixed.yml   # ⚠️  Old
├── docker-health.sh           # ⚠️  Old
└── docker-start.sh            # ⚠️  Old

AFTER:
config/docker/
├── docker-compose.yml         # ✅ Active
├── README.md                  # ✅ Documentation
└── .env                       # ✅ Environment config
```

#### 2. Docker Config Documentation - RESOLVED ✅

**Action Taken:** Created `config/docker/README.md`

Documents:
- What's in `config/docker/`
- Purpose of each file
- Common operations
- Why old files were archived

## Recommended File Structure

### Current vs Recommended

#### Services (No Change Needed) ✅

```
CURRENT (and RECOMMENDED):
rag-*-service/
├── src/
├── Dockerfile              # Build instructions
└── pom.xml
```

#### Config Directory ✅ REORGANIZED (2025-10-01)

```
BEFORE (old structure):
config/docker/
├── docker-compose.yml          # ❌ Required -f flag
├── .env                        # ❌ Not auto-loaded
├── README.md
└── application-local.yml

AFTER (standard structure):
RAG/                            # Project root
├── docker-compose.yml          # ✅ Auto-found, no -f flag needed
├── .env                        # ✅ Auto-loaded by Docker Compose
├── Makefile                    # ✅ Simplified (no -f flag)
└── docker/                     # ✅ Infrastructure configs (mounted into containers)
    ├── README.md               # ✅ Explains this directory
    ├── postgres/init.sql       # ✅ PostgreSQL initialization
    ├── prometheus/             # ✅ Metrics config
    └── grafana/                # ✅ Dashboards
```

**Why reorganized:** Follows Docker Compose best practices - compose files at project root.

#### Scripts ✅ WELL ORGANIZED

```
CURRENT (and OPTIMAL):
scripts/
├── deploy/
│   └── docker-deploy.sh        # Production deployment
├── dev/
│   └── rebuild-service.sh      # Development rebuild workflow
└── utils/
    ├── docker-wrapper.sh       # Safety wrapper
    └── health-check.sh         # Health check script
```

#### Documentation (Already Good) ✅

```
CURRENT (and RECOMMENDED):
docs/
├── getting-started/
│   └── QUICK_REFERENCE.md
├── development/
│   ├── DOCKER_DEVELOPMENT.md
│   ├── GIT_AND_DOCKER.md
│   └── MAKE_VS_ALTERNATIVES.md
├── deployment/
│   ├── DOCKER.md
│   ├── DOCKER_IMPROVEMENTS_SUMMARY.md
│   └── ENFORCEMENT_MECHANISMS.md
└── architecture/
    └── ENFORCEMENT_DIAGRAM.md
```

## Action Items

### ✅ All Items Complete

1. **✅ Investigated Old Files** - Archived to `archive/docker-old-2025-09/`
2. **✅ Created Config README** - `config/docker/README.md` created
3. **✅ Archived Old Files** - Old compose files and scripts archived
4. **✅ Updated Documentation** - All docs updated to reference current files
5. **✅ Scripts Organized** - All Docker scripts properly categorized

### Future Considerations

Consider creating environment-specific compose files when needed:
```bash
docker-compose.dev.yml      # Development overrides
docker-compose.prod.yml     # Production overrides
```

## Verification Checklist

- [x] All Dockerfiles are in service directories
- [x] Main docker-compose.yml is in config/docker/
- [x] Old/unused files archived to archive/docker-old-2025-09/
- [x] Scripts are in appropriate locations (deploy/, dev/, utils/)
- [x] Documentation is organized by purpose
- [x] .dockerignore is at project root
- [x] CI/CD files are in .github/workflows/
- [x] Archive contains old files with clear dating
- [x] config/docker/README.md created
- [x] All documentation references updated

## File Purpose Reference

| File | Purpose | Location | Status |
|------|---------|----------|--------|
| `rag-*/Dockerfile` | Service build instructions | Service directory | ✅ Active |
| `docker-compose.yml` | Service orchestration | config/docker/ | ✅ Active |
| `docker-compose.fixed.yml` | Old orchestration | archive/docker-old-2025-09/ | ✅ Archived |
| `docker-health.sh` | Old health check | archive/docker-old-2025-09/ | ✅ Archived |
| `docker-start.sh` | Old startup script | archive/docker-old-2025-09/ | ✅ Archived |
| `.dockerignore` | Build exclusions | Project root | ✅ Active |
| `docker-validation.yml` | CI checks | .github/workflows/ | ✅ Active |
| `docker-deploy.sh` | Deployment | scripts/deploy/ | ✅ Active |
| `docker-wrapper.sh` | Safety wrapper | scripts/utils/ | ✅ Active |
| `rebuild-service.sh` | Dev rebuild | scripts/dev/ | ✅ Active |
| `health-check.sh` | Health check | scripts/utils/ | ✅ Active |
| `config/docker/README.md` | Config directory docs | config/docker/ | ✅ Active |

## Related Documentation

- [Docker Development Guide](docs/development/DOCKER_DEVELOPMENT.md)
- [Git and Docker](docs/development/GIT_AND_DOCKER.md)
- [Docker Improvements](docs/deployment/DOCKER_IMPROVEMENTS_SUMMARY.md)
- [Project Structure](docs/architecture/PROJECT_STRUCTURE.md)

---

**Status:** ✅ Audit complete. All issues resolved. **REORGANIZED 2025-10-01** - Moved compose files to project root, removed unused `/config/` directory, following standard conventions.

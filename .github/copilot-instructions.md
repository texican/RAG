---
version: 1.0.0
last-updated: 2025-11-12
status: active
applies-to: 0.8.0-SNAPSHOT
category: ai-agent-guidance
---

# Copilot Instructions for RAG Codebase

## 🏗️ Big Picture Architecture
- **Microservices**: 6 core services (auth, document, embedding, core, admin, shared) in `rag-*-service/` directories. Each owns its domain and data.
- **Data Flow**: All inter-service communication is via REST APIs (see each service's `pom.xml` and `src/`). Kafka is planned for future async events.
- **Persistence**: Single PostgreSQL DB (per env, pattern: `byo_rag_{env}`) and Redis DB 0 (key prefixes for tenant/service isolation).
- **Security**: JWT-based auth, role-based access, tenant isolation at DB and cache level.
- **Deployment**: Docker Compose for local, GCP GKE for production. K8s manifests in `k8s/`.
- **Why**: Simplicity, cost, and maintainability. See `README.md` and `docs/deployment/` for rationale and diagrams.

## 🚦 Developer Workflows
- **Build all**: `make build-all` (preferred) or `mvn clean install` (all modules)
- **Start all**: `make start` (preferred) or `docker-compose up -d`
- **Rebuild service**: `make rebuild SERVICE=rag-auth` (rebuilds JAR, Docker image, restarts container)
- **Run tests**: `make test SERVICE=rag-auth` or `mvn test` in service dir
- **Logs**: `make logs SERVICE=rag-auth` or `docker-compose logs -f rag-auth`
- **Status**: `make status` (shows all services)
- **GCP deploy**: Use `make gcp-deploy-all ENV=dev` for full workflow, or scripts in `scripts/gcp/`
- **K8s**: Apply overlays in `k8s/overlays/` for dev/prod
- **Docs**: See `docs/README.md` for documentation hub and standards

## 🧩 Project-Specific Conventions
- **Service Naming**: All services and containers use `rag-<service>` naming.
- **Database Names**: Pattern is `byo_rag_{env}`; see `docker/postgres/init.sql` and K8s manifests.
- **Redis Keys**: Use `byo_rag_{env}:{service}:{tenant_id}:*` for isolation.
- **Makefile**: All common tasks are in the root `Makefile`—always prefer `make` over raw Docker/Maven commands.
- **Testing**: 99% pass rate required. See `docs/development/TESTING_BEST_PRACTICES.md` for test patterns and categories.
- **Backlog**: All work tracked in `BACKLOG.md` (active) and `docs/project-management/COMPLETED_STORIES.md` (done). Use backup scripts before editing.
- **Agent System**: Specialized sub-agents for test, deploy, backlog, docs, etc. See `.github/agents/` and `.claude/agents/README.md` for routing and usage.

## 🔗 Integration & External Dependencies
- **Ollama**: Local LLM integration, see `ollama-chat/README.md` for setup and troubleshooting.
- **OpenAI**: API key in secrets, configured in `application.yml`.
- **GCP**: Deployment scripts in `scripts/gcp/`, K8s manifests in `k8s/`, Artifact Registry for images.
- **Monitoring**: Prometheus and Grafana via Docker Compose (`docker/` configs).
- **Swagger UI**: Each service exposes `/swagger-ui.html` for API docs.

## 📝 Examples & Patterns
- **Add endpoint**: Use Spring Boot REST controller in `src/main/java/.../controller/`, register route, add to OpenAPI docs.
- **Tenant isolation**: Always filter by `tenant_id` in DB queries and Redis keys.
- **Service config**: Use `config/application-*.yml` for env-specific settings.
- **Health checks**: All services expose `/actuator/health` for liveness/readiness.
- **Testing**: Use JUnit 5, Testcontainers for integration, see `docs/development/TESTING_BEST_PRACTICES.md`.

## 📚 Key References
- **Root**: `README.md` (architecture, workflows, quick links)
- **Docs Hub**: `docs/README.md` (all guides)
- **K8s**: `k8s/README.md` (deployment, troubleshooting)
- **Docker**: `docker/README.md` (infra config)
- **Ollama Chat**: `ollama-chat/README.md` (frontend integration)
- **Agent System**: `.github/agents/`, `.claude/agents/README.md`
- **Testing**: `docs/development/TESTING_BEST_PRACTICES.md`
- **Backlog**: `BACKLOG.md`, `docs/project-management/PROJECT_BACKLOG.md`

---

**For agent routing, always use the specialized agent in `.github/agents/` if available.**

**For unclear or missing conventions, check `README.md` and `docs/` before proceeding.**

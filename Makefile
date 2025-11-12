.PHONY: help build rebuild rebuild-nc clean test start stop restart restart-all logs status create-admin-user \
	build-all clean-docker clean-all \
	dev-auth dev-admin dev-document dev-embedding dev-core \
	gcp-check-env gcp-build gcp-cloud-build gcp-deploy gcp-init-db gcp-validate gcp-validate-quick \
	gcp-deploy-all gcp-status gcp-logs gcp-port-forward gcp-restart gcp-setup-ingress gcp-cleanup \
	gcp-dev gcp-prod

# Color output
BLUE := \033[0;34m
GREEN := \033[0;32m
YELLOW := \033[1;33m
NC := \033[0m

# Default target
help:
	@echo "$(BLUE)RAG System - Docker Development Commands$(NC)"
	@echo "=========================================="
	@echo ""
	@echo "$(GREEN)Service Management:$(NC)"
	@echo "  make rebuild SERVICE=rag-auth    - Rebuild and restart a service"
	@echo "  make rebuild-nc SERVICE=rag-auth - Rebuild with --no-cache"
	@echo "  make logs SERVICE=rag-auth       - Follow logs for a service"
	@echo "  make restart SERVICE=rag-auth    - Restart a service (no rebuild)"
	@echo "  make status                      - Show all service status"
	@echo ""
	@echo "$(GREEN)Full System:$(NC)"
	@echo "  make start                       - Start all services"
	@echo "  make stop                        - Stop all services"
	@echo "  make restart-all                 - Restart all services"
	@echo "  make build-all                   - Build all JARs"
	@echo ""
	@echo "$(GREEN)Database:$(NC)"
	@echo "  make create-admin-user           - Create initial admin user"
	@echo ""
	@echo "$(GREEN)Cleanup:$(NC)"
	@echo "  make clean                       - Clean Maven builds"
	@echo "  make clean-docker                - Remove all Docker images/containers"
	@echo "  make clean-all                   - Clean everything"
	@echo ""
	@echo "$(GREEN)Testing:$(NC)"
	@echo "  make test                        - Run all tests"
	@echo "  make test SERVICE=rag-auth       - Run tests for one service"
	@echo ""
	@echo "$(GREEN)Available Services:$(NC)"
	@echo "  - rag-auth     (Authentication)"
	@echo "  - rag-document (Document Processing)"
	@echo "  - rag-embedding (Embedding Generation)"
	@echo "  - rag-core     (RAG Core)"
	@echo "  - rag-admin    (Administration)"
	@echo ""
	@echo "$(GREEN)GCP Deployment:$(NC)"
	@echo "  make gcp-build ENV=dev           - Build and push images to GCP"
	@echo "  make gcp-deploy ENV=dev          - Deploy services to GKE"
	@echo "  make gcp-init-db ENV=dev         - Initialize database"
	@echo "  make gcp-validate ENV=dev        - Validate deployment"
	@echo "  make gcp-deploy-all ENV=dev      - Complete deployment (all steps)"
	@echo "  make gcp-status ENV=dev          - Show GKE deployment status"
	@echo "  make gcp-logs ENV=dev SERVICE=rag-auth - View GKE logs"

# Rebuild a single service
rebuild:
	@if [ -z "$(SERVICE)" ]; then \
		echo "$(YELLOW)Error: SERVICE not specified$(NC)"; \
		echo "Usage: make rebuild SERVICE=rag-auth"; \
		exit 1; \
	fi
	@./scripts/dev/rebuild-service.sh $(SERVICE)

# Rebuild with no cache
rebuild-nc:
	@if [ -z "$(SERVICE)" ]; then \
		echo "$(YELLOW)Error: SERVICE not specified$(NC)"; \
		echo "Usage: make rebuild-nc SERVICE=rag-auth"; \
		exit 1; \
	fi
	@./scripts/dev/rebuild-service.sh $(SERVICE) --no-cache

# View logs
logs:
	@if [ -z "$(SERVICE)" ]; then \
		echo "$(YELLOW)Error: SERVICE not specified$(NC)"; \
		echo "Usage: make logs SERVICE=rag-auth"; \
		exit 1; \
	fi
	@docker logs $(SERVICE) --tail 100 --follow

# Restart (no rebuild)
restart:
	@if [ -z "$(SERVICE)" ]; then \
		echo "$(YELLOW)Error: SERVICE not specified$(NC)"; \
		echo "Usage: make restart SERVICE=rag-auth"; \
		exit 1; \
	fi
	@docker restart $(SERVICE)

# Show status
status:
	@echo "$(BLUE)RAG Services Status:$(NC)"
	@docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -E "(NAMES|rag-)"

# Start all services
start:
	@echo "$(BLUE)Starting all services...$(NC)"
	@docker-compose up -d

# Stop all services
stop:
	@echo "$(BLUE)Stopping all services...$(NC)"
	@docker-compose down

# Restart all services
restart-all: stop start

# Build all JARs
build-all:
	@echo "$(BLUE)Building all services...$(NC)"
	@mvn clean package -DskipTests

# Clean Maven builds
clean:
	@echo "$(BLUE)Cleaning Maven builds...$(NC)"
	@mvn clean

# Clean Docker
clean-docker:
	@echo "$(YELLOW)⚠️  This will remove all RAG Docker images and containers$(NC)"
	@read -p "Are you sure? [y/N] " -n 1 -r; \
	echo; \
	if [[ $$REPLY =~ ^[Yy]$$ ]]; then \
		docker-compose down; \
		docker images | grep "rag-" | awk '{print $$3}' | xargs docker rmi -f 2>/dev/null || true; \
		echo "$(GREEN)✅ Docker cleanup complete$(NC)"; \
	fi

# Clean everything
clean-all: clean clean-docker

# Run all tests
test:
	@if [ -z "$(SERVICE)" ]; then \
		echo "$(BLUE)Running all tests...$(NC)"; \
		mvn test; \
	else \
		echo "$(BLUE)Running tests for $(SERVICE)...$(NC)"; \
		mvn test -pl $(SERVICE)-service; \
	fi

# Development shortcuts
dev-auth:
	@make rebuild SERVICE=rag-auth

dev-admin:
	@make rebuild SERVICE=rag-admin

dev-document:
	@make rebuild SERVICE=rag-document

dev-embedding:
	@make rebuild SERVICE=rag-embedding

dev-core:
	@make rebuild SERVICE=rag-core

# Create admin user via Docker (doesn't require local psql)
create-admin-user:
	@echo "$(BLUE)🔐 Creating Initial Admin User$(NC)"
	@echo "================================="
	@echo ""
	@docker exec rag-postgres psql -U rag_user -d byo_rag_local -c " \
		INSERT INTO tenants (id, created_at, updated_at, version, name, slug, description, status, max_documents, max_storage_mb) \
		VALUES ( \
			gen_random_uuid(), \
			NOW(), \
			NOW(), \
			0, \
			'System Administration', \
			'admin', \
			'System administration tenant for managing the RAG platform', \
			'ACTIVE', \
			10000, \
			10000 \
		) ON CONFLICT (slug) DO UPDATE SET \
			updated_at = NOW(), \
			version = tenants.version + 1 \
		RETURNING id;" 2>&1 | grep -v "INSERT" || true
	@docker exec rag-postgres psql -U rag_user -d byo_rag_local -c " \
		INSERT INTO users (id, created_at, updated_at, version, email, first_name, last_name, password_hash, role, status, email_verified, tenant_id) \
		VALUES ( \
			gen_random_uuid(), \
			NOW(), \
			NOW(), \
			0, \
			'$${ADMIN_EMAIL:-admin@enterprise-rag.com}', \
			'$${ADMIN_FIRST_NAME:-System}', \
			'$${ADMIN_LAST_NAME:-Administrator}', \
			'\$$2a\$$10\$$4ruqE8FlnERNCuIW/6pI6.1rlZmJiG/plwFwif5KPGxjwbM9Sm6je', \
			'ADMIN', \
			'ACTIVE', \
			true, \
			(SELECT id FROM tenants WHERE slug = 'admin') \
		) ON CONFLICT (email) DO UPDATE SET \
			password_hash = '\$$2a\$$10\$$4ruqE8FlnERNCuIW/6pI6.1rlZmJiG/plwFwif5KPGxjwbM9Sm6je', \
			updated_at = NOW(), \
			version = users.version + 1, \
			status = 'ACTIVE' \
		RETURNING id;" 2>&1 | grep -v "INSERT" || true
	@echo ""
	@echo "$(GREEN)✅ Admin user created successfully!$(NC)"
	@echo ""
	@echo "📋 Admin Credentials:"
	@echo "   Email: $${ADMIN_EMAIL:-admin@enterprise-rag.com}"
	@echo "   Password: $${ADMIN_PASSWORD:-admin123}"
	@echo "   Tenant: System Administration (admin)"
	@echo ""
	@echo "🌐 Login URL: http://localhost:8085/admin/api/auth/login"
	@echo ""
	@echo "$(YELLOW)⚠️  Change the default password after first login!$(NC)"

# ==============================================================================
# GCP Deployment Targets
# ==============================================================================

# Validate ENV parameter
gcp-check-env:
	@if [ -z "$(ENV)" ]; then \
		echo "$(YELLOW)Error: ENV not specified$(NC)"; \
		echo "Usage: make gcp-deploy ENV=dev"; \
		echo "Valid values: dev, staging, prod"; \
		exit 1; \
	fi

# Build and push images to Artifact Registry
gcp-build: gcp-check-env
	@echo "$(BLUE)Building and pushing images to GCP Artifact Registry...$(NC)"
	@echo "$(YELLOW)Note: Using current gcloud project configuration$(NC)"
	@./scripts/gcp/07-build-and-push-images.sh

# Build images using Cloud Build (recommended for GCP)
gcp-cloud-build: gcp-check-env
	@echo "$(BLUE)Building images using Google Cloud Build...$(NC)"
	@./scripts/gcp/07a-cloud-build-images.sh --env $(ENV)

# Deploy services to GKE
gcp-deploy: gcp-check-env
	@echo "$(BLUE)Deploying services to GKE...$(NC)"
	@./scripts/gcp/17-deploy-services.sh --env $(ENV)

# Initialize database
gcp-init-db: gcp-check-env
	@echo "$(BLUE)Initializing database...$(NC)"
	@./scripts/gcp/18-init-database.sh --env $(ENV)

# Validate deployment
gcp-validate: gcp-check-env
	@echo "$(BLUE)Validating deployment...$(NC)"
	@./scripts/gcp/19-validate-deployment.sh --env $(ENV)

# Quick validation (skip integration tests)
gcp-validate-quick: gcp-check-env
	@echo "$(BLUE)Running quick validation...$(NC)"
	@./scripts/gcp/19-validate-deployment.sh --env $(ENV) --quick

# Complete deployment (all steps)
gcp-deploy-all: gcp-check-env gcp-build gcp-deploy gcp-init-db gcp-validate
	@echo ""
	@echo "$(GREEN)✅ Complete GCP deployment finished successfully!$(NC)"
	@echo ""
	@echo "Next steps:"
	@echo "  1. Configure ingress: make gcp-setup-ingress ENV=$(ENV) DOMAIN=rag-$(ENV).example.com"
	@echo "  2. Check deployment: make gcp-status ENV=$(ENV)"
	@echo "  3. View logs: make gcp-logs ENV=$(ENV) SERVICE=rag-auth"

# Show GKE deployment status
gcp-status: gcp-check-env
	@echo "$(BLUE)GKE Deployment Status (ENV=$(ENV)):$(NC)"
	@echo "======================================"
	@kubectl get all -n rag-system
	@echo ""
	@echo "$(BLUE)Pod Details:$(NC)"
	@kubectl get pods -n rag-system -o wide
	@echo ""
	@echo "$(BLUE)Ingress:$(NC)"
	@kubectl get ingress -n rag-system || echo "No ingress configured"

# View GKE logs
gcp-logs: gcp-check-env
	@if [ -z "$(SERVICE)" ]; then \
		echo "$(YELLOW)Error: SERVICE not specified$(NC)"; \
		echo "Usage: make gcp-logs ENV=dev SERVICE=rag-auth"; \
		exit 1; \
	fi
	@echo "$(BLUE)Viewing logs for $(SERVICE) on GKE...$(NC)"
	@kubectl logs -n rag-system -l app=$(SERVICE) --tail=100 --follow

# Port-forward to service
gcp-port-forward: gcp-check-env
	@if [ -z "$(SERVICE)" ]; then \
		echo "$(YELLOW)Error: SERVICE not specified$(NC)"; \
		echo "Usage: make gcp-port-forward ENV=dev SERVICE=rag-auth"; \
		exit 1; \
	fi
	@case "$(SERVICE)" in \
		rag-auth) PORT=8081;; \
		rag-document) PORT=8082;; \
		rag-embedding) PORT=8083;; \
		rag-core) PORT=8084;; \
		rag-admin) PORT=8085;; \
		*) echo "$(YELLOW)Unknown service$(NC)"; exit 1;; \
	esac; \
	echo "$(BLUE)Port-forwarding $(SERVICE):$$PORT...$(NC)"; \
	kubectl port-forward -n rag-system svc/$(SERVICE)-service $$PORT:$$PORT

# Restart deployment
gcp-restart: gcp-check-env
	@if [ -z "$(SERVICE)" ]; then \
		echo "$(YELLOW)Restarting all deployments...$(NC)"; \
		kubectl rollout restart deployment -n rag-system; \
	else \
		echo "$(BLUE)Restarting $(SERVICE) deployment...$(NC)"; \
		kubectl rollout restart deployment $(SERVICE)-service -n rag-system; \
	fi

# Setup ingress
gcp-setup-ingress: gcp-check-env
	@if [ -z "$(DOMAIN)" ]; then \
		echo "$(YELLOW)Error: DOMAIN not specified$(NC)"; \
		echo "Usage: make gcp-setup-ingress ENV=dev DOMAIN=rag-dev.example.com"; \
		exit 1; \
	fi
	@echo "$(BLUE)Setting up ingress for $(DOMAIN)...$(NC)"
	@./scripts/gcp/16-setup-ingress.sh --env $(ENV) --domain $(DOMAIN)

# Cleanup GKE deployment
gcp-cleanup: gcp-check-env
	@echo "$(YELLOW)⚠️  This will delete all resources in rag-system namespace$(NC)"
	@read -p "Are you sure? [y/N] " -n 1 -r; \
	echo; \
	if [[ $$REPLY =~ ^[Yy]$$ ]]; then \
		kubectl delete all --all -n rag-system; \
		echo "$(GREEN)✅ GKE deployment cleaned up$(NC)"; \
	fi

# GCP shortcuts for common workflows
gcp-dev:
	@$(MAKE) gcp-deploy-all ENV=dev

gcp-prod:
	@$(MAKE) gcp-deploy-all ENV=prod


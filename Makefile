.PHONY: help build rebuild clean test start stop restart logs status

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

# Docker Infrastructure Configuration Files

This directory contains configuration files that are mounted into Docker containers at runtime.

## 📁 Directory Purpose

The `/docker/` directory holds **service-specific configuration files** that get volume-mounted into infrastructure containers (PostgreSQL, Prometheus, Grafana, etc.).

**Why separate from `config/docker/`?**
- `config/docker/` - Spring Boot application configs
- `docker/` - Infrastructure service configs (mounted into containers)

## 📂 Directory Structure

```
docker/
├── grafana/
│   ├── datasources/
│   │   └── prometheus.yml          # Grafana datasource config (Prometheus)
│   └── dashboards/
│       └── dashboard.yml           # Dashboard provisioning config
├── postgres/
│   └── init.sql                    # PostgreSQL initialization script
└── prometheus/
    └── prometheus.yml              # Prometheus scraping configuration
```

## 🔧 How These Files Are Used

These files are mounted into containers via `docker-compose.yml`:

### PostgreSQL (`postgres/init.sql`)

**Volume mount:**
```yaml
volumes:
  - ./docker/postgres/init.sql:/docker-entrypoint-initdb.d/init.sql
```

**Purpose:**
- Creates separate databases for each microservice (rag_auth, rag_documents, etc.)
- Enables pgvector extension for vector operations
- Sets up database permissions

**When it runs:** Automatically executed when PostgreSQL container first starts

### Prometheus (`prometheus/prometheus.yml`)

**Volume mount:**
```yaml
volumes:
  - ./docker/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
```

**Purpose:**
- Configures service discovery
- Defines scraping targets (all RAG services)
- Sets scrape intervals and timeouts

### Grafana (`grafana/datasources/` & `grafana/dashboards/`)

**Volume mounts:**
```yaml
volumes:
  - ./docker/grafana/datasources:/etc/grafana/provisioning/datasources
  - ./docker/grafana/dashboards:/etc/grafana/provisioning/dashboards
```

**Purpose:**
- Auto-configures Prometheus as a datasource
- Provisions dashboards automatically
- No manual Grafana setup required

## 🚫 What NOT to Put Here

- ❌ Application code
- ❌ Secrets or passwords (use `.env` instead)
- ❌ Dockerfiles (those belong in service directories)
- ❌ Docker Compose files (those are at project root)

## ✅ What DOES Belong Here

- ✅ Database initialization scripts
- ✅ Monitoring configuration (Prometheus, Grafana)
- ✅ Service-specific config files to be mounted
- ✅ Any file that needs to be volume-mounted into a container

## 📝 Modifying These Files

### PostgreSQL Init Script

**File:** `postgres/init.sql`

**To add a new database:**
```sql
CREATE DATABASE rag_newservice;
GRANT ALL PRIVILEGES ON DATABASE rag_newservice TO rag_user;
```

**⚠️ Important:** Changes only apply to new containers. To apply to existing:
```bash
docker-compose down -v  # ⚠️ Deletes all data!
docker-compose up -d
```

### Prometheus Configuration

**File:** `prometheus/prometheus.yml`

**To add a new scraping target:**
```yaml
scrape_configs:
  - job_name: 'rag-newservice'
    static_configs:
      - targets: ['rag-newservice:8086']
```

**To apply changes:**
```bash
docker restart prometheus
# Or: docker-compose restart prometheus
```

### Grafana Configuration

**Files:** `grafana/datasources/*.yml`, `grafana/dashboards/*.yml`

**Changes apply:** On container restart
```bash
docker restart grafana
```

## 🔍 Verifying Mount Points

Check if files are properly mounted:

```bash
# PostgreSQL
docker exec postgres ls -la /docker-entrypoint-initdb.d/

# Prometheus
docker exec prometheus ls -la /etc/prometheus/

# Grafana
docker exec grafana ls -la /etc/grafana/provisioning/
```

## 📚 Related Documentation

- **[docker-compose.yml](../docker-compose.yml)** - See volume mount definitions
- **[Deployment Guide](../docs/deployment/DEPLOYMENT.md)** - How to deploy
- **[Docker Development Guide](../docs/development/DOCKER_DEVELOPMENT.md)** - Development workflow

---

**Last Updated:** 2025-10-01

**Note:** This directory contains files that are mounted into infrastructure containers, not application code.

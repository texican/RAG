---
version: 1.0.0
last-updated: 2025-11-12
status: active
applies-to: 0.8.0-SNAPSHOT
category: development
---

# Docker Best Practices - RAG System

This guide covers Docker best practices for building, configuring, and deploying the RAG system microservices.

## Table of Contents
- [Dockerfile Best Practices](#dockerfile-best-practices)
- [Spring Boot Configuration in Docker](#spring-boot-configuration-in-docker)
- [Security Best Practices](#security-best-practices)
- [Image Optimization](#image-optimization)
- [Environment Variables & Configuration](#environment-variables--configuration)
- [Troubleshooting Common Issues](#troubleshooting-common-issues)

---

## Dockerfile Best Practices

### 1. Multi-Stage Builds

**✅ RECOMMENDED** - Separate build and runtime stages to minimize final image size:

```dockerfile
# Build stage - full JDK + Maven
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Cache dependencies layer (changes infrequently)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build application (changes frequently)
COPY src ./src
RUN mvn clean package -DskipTests -B

# Runtime stage - minimal JRE only
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy only the built JAR
COPY --from=builder /build/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Benefits:**
- Final image contains only JRE + JAR (not Maven, not source code)
- Build tools and dependencies don't bloat production image
- ~500MB build image → ~200MB runtime image

### 2. Layer Caching Optimization

Order Dockerfile instructions from **least to most frequently changing**:

```dockerfile
# 1. Base image (never changes)
FROM eclipse-temurin:21-jre-alpine

# 2. System dependencies (rarely change)
RUN apk add --no-cache wget curl

# 3. Application dependencies (changes occasionally)
COPY pom.xml .
RUN mvn dependency:go-offline

# 4. Application code (changes frequently)
COPY src ./src
RUN mvn package
```

**Result:** Docker reuses cached layers for unchanged steps, speeding up builds.

### 3. Use Specific Base Image Tags

**✅ DO:**
```dockerfile
FROM eclipse-temurin:21-jre-alpine      # Specific version
FROM maven:3.9-eclipse-temurin-21       # Specific version
```

**❌ DON'T:**
```dockerfile
FROM openjdk:latest                     # Unpredictable
FROM eclipse-temurin:21                 # Missing OS variant
```

**Why:** `latest` can change between builds, breaking reproducibility.

### 4. Minimize Image Size

```dockerfile
# Use Alpine variants (smaller base OS)
FROM eclipse-temurin:21-jre-alpine

# Clean up in same RUN command
RUN apk add --no-cache wget file && \
    rm -rf /var/cache/apk/*

# Don't install unnecessary packages
RUN apk add --no-cache wget  # Only what you need
```

**Current Project Status:**
- ✅ All services use `eclipse-temurin:21-jre` (good choice)
- ⚠️ Not using Alpine variants (opportunity for 30-50% size reduction)
- ✅ Cleanup commands present in Dockerfiles

---

## Spring Boot Configuration in Docker

### Configuration Precedence (Highest to Lowest)

Understanding Spring Boot's configuration precedence is **critical** for Docker deployments:

1. **Command-line arguments** (`java -jar app.jar --spring.kafka.bootstrap-servers=...`)
2. **SPRING_APPLICATION_JSON** environment variable
3. **Java System Properties** (`-Dspring.kafka.bootstrap-servers=...`)
4. **OS Environment Variables** (`SPRING_KAFKA_BOOTSTRAP_SERVERS=...`)
5. **Profile-specific application.yml** (`application-docker.yml`)
6. **Default application.yml**

### The Kafka Configuration Issue (STORY-018 Example)

**Problem:** Kafka producer connects to `localhost:9092` instead of `kafka:29092` despite:
- Docker profile configuration: `spring.kafka.bootstrap-servers: kafka:29092`
- Environment variable: `KAFKA_BOOTSTRAP_SERVERS=kafka:29092`

**Root Cause:** Spring Boot's Kafka autoconfiguration is created during `@ConfigurationProperties` binding phase **before** Spring profiles are fully merged. The default configuration (`localhost:9092`) is used instead of the profile-specific value.

**Solution Options:**

#### Option 1: Use Spring Boot Environment Variable Format (RECOMMENDED)

```yaml
# docker-compose.yml
environment:
  - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092  # Direct property name
```

**Why it works:** Spring Boot directly maps `SPRING_KAFKA_BOOTSTRAP_SERVERS` to `spring.kafka.bootstrap-servers` property before any autoconfiguration.

#### Option 2: Java System Properties

```yaml
# docker-compose.yml
environment:
  - JAVA_TOOL_OPTIONS=-Dspring.kafka.bootstrap-servers=kafka:29092
```

#### Option 3: Command-Line Arguments

```dockerfile
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.kafka.bootstrap-servers=kafka:29092"]
```

**❌ DON'T rely solely on profile-specific configs for autoconfigured beans.**

### Best Practices for Spring Boot + Docker

1. **Use Spring Boot's Environment Variable Format**

```yaml
# docker-compose.yml
environment:
  # ✅ Spring Boot understands this directly
  - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/mydb
  - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092
  - SPRING_REDIS_HOST=redis

  # ❌ Custom env vars require placeholder resolution
  - DB_HOST=postgres  # Needs ${DB_HOST} in application.yml
```

2. **Set Critical Configuration via Environment Variables**

For properties that **must** work regardless of profiles (like Kafka, database URLs):

```yaml
# docker-compose.yml
services:
  rag-document:
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092  # Override here
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/rag_enterprise
```

3. **Use Profiles for Feature Toggles, Not Infrastructure Config**

**✅ Good use of profiles:**
```yaml
# application-docker.yml
logging:
  level:
    root: INFO  # Less verbose in Docker

features:
  async-processing: true
```

**❌ Bad use of profiles (infrastructure):**
```yaml
# application-docker.yml
spring:
  kafka:
    bootstrap-servers: kafka:29092  # Won't work for autoconfigured beans!
```

**Better approach:**
```yaml
# docker-compose.yml
environment:
  - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092  # Set directly
```

### Testing Configuration

Always verify your configuration is being picked up:

```bash
# Check environment variables in running container
docker exec rag-document env | grep SPRING

# Check actual Spring Boot properties at startup
docker logs rag-document | grep "bootstrap.servers"

# Expected output:
# bootstrap.servers = [kafka:29092]
```

---

## Security Best Practices

### 1. Don't Run as Root

**✅ CURRENT PROJECT STATUS:** Already implemented in all Dockerfiles!

```dockerfile
# Create non-root user
RUN groupadd -r rag && useradd -r -g rag -s /bin/false rag

# Create directories and set ownership
RUN mkdir -p /app/logs /app/storage && \
    chown -R rag:rag /app

# Copy application files
COPY rag-document-service/target/rag-document-service-*.jar app.jar
RUN chown rag:rag app.jar

# Switch to non-root user BEFORE ENTRYPOINT
USER rag

# Run application as non-root
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Why:** Running as root is a security risk. If the container is compromised, the attacker has root privileges.

### 2. Use Read-Only Filesystem (Advanced)

For stateless services, make the filesystem read-only:

```yaml
# docker-compose.yml
services:
  rag-auth:
    read_only: true
    tmpfs:
      - /tmp
      - /app/logs
```

### 3. Scan Images for Vulnerabilities

```bash
# Using Docker Scout (built into Docker Desktop)
docker scout cves rag-document:latest

# Using Trivy
trivy image rag-document:latest
```

### 4. Don't Store Secrets in Images

**❌ DON'T:**
```dockerfile
ENV JWT_SECRET=my-secret-key-here  # Exposed in image layers!
```

**✅ DO:**
```yaml
# docker-compose.yml
services:
  rag-auth:
    environment:
      - JWT_SECRET=${JWT_SECRET}  # From .env file
    # OR use Docker secrets (Swarm/Kubernetes)
    secrets:
      - jwt_secret
```

---

## Image Optimization

### 1. Use .dockerignore

Prevent unnecessary files from being copied into the image:

```gitignore
# .dockerignore
target/
*.log
.git
.idea
*.md
docs/
scripts/
test-data/
*.iml
node_modules/
```

**Impact:** Faster builds, smaller context size sent to Docker daemon.

### 2. Combine RUN Commands

**❌ BAD** (creates multiple layers):
```dockerfile
RUN apt-get update
RUN apt-get install -y wget
RUN apt-get install -y file
RUN rm -rf /var/lib/apt/lists/*
```

**✅ GOOD** (single layer):
```dockerfile
RUN apt-get update && \
    apt-get install -y wget file && \
    rm -rf /var/lib/apt/lists/*
```

### 3. Order Layers by Change Frequency

```dockerfile
# Rarely changes (cache friendly)
FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y wget

# Changes occasionally
COPY pom.xml .
RUN mvn dependency:go-offline

# Changes frequently (invalidates cache least often)
COPY src ./src
RUN mvn package
```

### 4. Use BuildKit for Better Caching

```bash
# Enable BuildKit
export DOCKER_BUILDKIT=1

# Build with BuildKit
docker build -t rag-document:latest .
```

**Benefits:**
- Parallel layer builds
- Better cache invalidation
- Faster builds

---

## Environment Variables & Configuration

### Spring Boot Environment Variable Naming

Spring Boot automatically maps environment variables to properties:

| Environment Variable | Spring Property |
|---------------------|-----------------|
| `SPRING_DATASOURCE_URL` | `spring.datasource.url` |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `spring.kafka.bootstrap-servers` |
| `SPRING_PROFILES_ACTIVE` | `spring.profiles.active` |

**Rule:** Uppercase with underscores → lowercase with dots

### Configuration Strategy

**For the RAG Project:**

1. **Infrastructure (Kafka, DB, Redis)** → Environment variables in `docker-compose.yml`
2. **Feature flags** → Spring profiles (`application-docker.yml`)
3. **Secrets** → `.env` file (not committed) or secrets management
4. **Development overrides** → `.env` file

### Example: docker-compose.yml

```yaml
services:
  rag-document:
    environment:
      # Profile selection
      - SPRING_PROFILES_ACTIVE=docker

      # Infrastructure (MUST work regardless of profile)
      - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/rag_enterprise
      - SPRING_DATA_REDIS_HOST=redis

      # Application-specific (can use custom names)
      - DB_USERNAME=${POSTGRES_USER}
      - DB_PASSWORD=${POSTGRES_PASSWORD}
      - FILE_STORAGE_PATH=/app/storage
```

---

## Troubleshooting Common Issues

### Issue 1: Configuration Not Being Picked Up

**Symptom:** Service uses wrong configuration despite environment variables.

**Debug Steps:**
```bash
# 1. Check environment variables in container
docker exec rag-document env | grep SPRING

# 2. Check Spring Boot startup logs
docker logs rag-document | grep -i "active profile"
docker logs rag-document | grep "bootstrap.servers"

# 3. Verify docker-compose environment is applied
docker inspect rag-document | grep -A 20 Env
```

**Solution:**
- Use `SPRING_*` format for Spring Boot properties
- Don't rely on profile-specific configs for autoconfigured beans
- Set critical config as environment variables

### Issue 2: Image Not Updating After Rebuild

**Symptom:** Code changes not reflected after `docker-compose build`.

**Solution:**
```bash
# Use the project's rebuild script
./scripts/dev/rebuild-service.sh rag-document

# Or manual rebuild with cache invalidation
mvn clean package -pl rag-document-service -DskipTests
docker-compose build --no-cache rag-document
docker-compose up -d --force-recreate rag-document
```

### Issue 3: Kafka/Redis/DB Connection Refused

**Symptom:** `Connection refused` errors to `localhost:9092`, `localhost:6379`, etc.

**Cause:** Service is trying to connect to `localhost` instead of Docker network hostnames.

**Solution:**
```yaml
# ✅ Use Docker service names
environment:
  - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092  # Not localhost:9092
  - SPRING_DATA_REDIS_HOST=redis                # Not localhost:6379
  - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/db  # Not localhost:5432
```

### Issue 4: Profile Not Activating

**Symptom:** Docker profile configuration not being used.

**Debug:**
```bash
docker logs rag-document 2>&1 | grep "active profile"
# Should show: The following profiles are active: docker
```

**Solution:**
```yaml
# docker-compose.yml
environment:
  - SPRING_PROFILES_ACTIVE=docker  # Must be set!
```

### Issue 5: Spring Boot Autoconfiguration Precedence

**Symptom:** Profile-specific config ignored for autoconfigured beans (Kafka, DataSource).

**Root Cause:** Autoconfiguration happens before profile configs are merged.

**Solution:** Use environment variables with `SPRING_*` format:
```yaml
environment:
  - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092  # Takes precedence
```

---

## Quick Reference

### Building Images

```bash
# Development: Use rebuild script
./scripts/dev/rebuild-service.sh rag-document

# Production: Multi-stage build with version tag
docker build -t rag-document:1.0.0 -f rag-document-service/Dockerfile .

# With no cache (troubleshooting)
docker build --no-cache -t rag-document:latest .
```

### Checking Configuration

```bash
# Environment variables
docker exec rag-document env | grep SPRING

# Spring Boot properties at startup
docker logs rag-document | grep "bootstrap.servers"
docker logs rag-document | grep "active profile"

# Container configuration
docker inspect rag-document | grep -A 20 Env
```

### Image Cleanup

```bash
# Remove specific image
docker rmi rag-document:latest

# Remove all unused images
docker image prune -a

# Remove all unused containers, networks, volumes
docker system prune -a --volumes
```

---

## Checklist: Production-Ready Dockerfile

- [ ] Multi-stage build (build + runtime stages)
- [ ] Specific base image tag (no `latest`)
- [ ] Minimal base image (Alpine or JRE-only)
- [ ] Non-root user (USER directive)
- [ ] Health check (HEALTHCHECK or docker-compose)
- [ ] .dockerignore file (exclude unnecessary files)
- [ ] Layers ordered by change frequency
- [ ] Cleanup commands in same RUN layer
- [ ] No secrets in image layers
- [ ] Vulnerability scanning passed

## Checklist: Production-Ready docker-compose.yml

- [ ] Specific image tags (not `latest`)
- [ ] Health checks configured
- [ ] Restart policies set (`unless-stopped`)
- [ ] Resource limits defined (memory, CPU)
- [ ] Dependencies declared (`depends_on` with conditions)
- [ ] Critical config via `SPRING_*` environment variables
- [ ] Secrets in `.env` file (not committed) or secrets management
- [ ] Networks explicitly defined
- [ ] Volumes for persistent data

---

## Additional Resources

- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [Spring Boot Docker Documentation](https://spring.io/guides/topicals/spring-boot-docker/)
- [Spring Boot Configuration Properties](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [Project's Docker Development Guide](DOCKER_DEVELOPMENT.md)
- [Project's Docker Deployment Guide](../deployment/DOCKER.md)

---

**Document Version:** 1.0
**Last Updated:** 2025-10-06
**Author:** RAG Development Team
**Related Issues:** STORY-018 (Kafka Configuration), Sprint 1 Documentation

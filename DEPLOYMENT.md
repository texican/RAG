# Enterprise RAG System - Deployment Guide

[![Version](https://img.shields.io/badge/Version-1.0.0--SNAPSHOT-blue.svg)](https://semver.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-brightgreen.svg)](https://www.docker.com/)
[![Status](https://img.shields.io/badge/Status-Development-yellow.svg)](https://github.com/your-org/enterprise-rag)

> **🚧 Development Status (2025-09-03)**: 4/6 microservices operational in Docker. Core service debugging in progress. Infrastructure stable.

A comprehensive deployment guide for the Enterprise RAG (Retrieval Augmented Generation) system across development, staging, and production environments.

## 📋 Table of Contents

- [Prerequisites](#prerequisites)
- [Development Deployment](#development-deployment)
- [Staging Deployment](#staging-deployment)
- [Production Deployment](#production-deployment)
- [Environment Configuration](#environment-configuration)
- [Monitoring & Observability](#monitoring--observability)
- [Security Configuration](#security-configuration)
- [Backup & Recovery](#backup--recovery)
- [Troubleshooting](#troubleshooting)
- [Performance Tuning](#performance-tuning)

## 🔧 Prerequisites

### System Requirements

| Component | Minimum | Recommended | Production |
|-----------|---------|-------------|------------|
| **CPU** | 4 cores | 8 cores | 16+ cores |
| **RAM** | 8 GB | 16 GB | 32+ GB |
| **Storage** | 50 GB | 100 GB | 500+ GB SSD |
| **Network** | 100 Mbps | 1 Gbps | 10+ Gbps |

### Software Dependencies

```bash
# Required Software
- Java 21+ (OpenJDK recommended)
- Maven 3.8+
- Docker 24.0+
- Docker Compose 2.0+
- Git 2.30+

# Optional (for advanced deployments)
- Kubernetes 1.25+
- Helm 3.10+
- Terraform 1.5+
```

## 🚀 Development Deployment

### Quick Start (Docker - Recommended)

> **🚧 Current Status**: Docker infrastructure stable (PostgreSQL + Redis). 4/6 microservices operational. Core service debugging needed.

```bash
# 1. Clone and setup
git clone <repository-url>
cd enterprise-rag

# 2. Build all services
mvn clean package -DskipTests

# 3. Start all services with Docker Compose
docker-compose -f docker-compose.fixed.yml up -d

# 4. Check service status
docker-compose -f docker-compose.fixed.yml ps

# 5. View service logs
docker-compose -f docker-compose.fixed.yml logs -f

# 6. Health check working services
curl http://localhost:8081/actuator/health  # Auth Service
curl http://localhost:8083/actuator/health  # Embedding Service
curl http://localhost:8085/admin/api/actuator/health  # Admin Service
```

**Alternative: Individual Maven Services**
```bash
# Start infrastructure first
docker-compose up -d postgres redis

# Then start each service in separate terminal
cd rag-auth-service && mvn spring-boot:run        # Port 8081
cd rag-document-service && mvn spring-boot:run    # Port 8082
cd rag-embedding-service && mvn spring-boot:run   # Port 8083
cd rag-core-service && mvn spring-boot:run        # Port 8084 (currently failing)
cd rag-admin-service && mvn spring-boot:run       # Port 8085
cd rag-gateway && mvn spring-boot:run             # Port 8080 (depends on core)
```

### Development Environment Variables

Create `.env.dev` file:

```bash
# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_USERNAME=rag_user
DB_PASSWORD=rag_password

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=""

# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# JWT Configuration
JWT_SECRET=YourVerySecureJWTSecretKeyThatIsAtLeast256BitsLongForHS256Algorithm
JWT_ACCESS_EXPIRATION=3600
JWT_REFRESH_EXPIRATION=604800

# AI/ML Configuration
OPENAI_API_KEY=your-openai-api-key
OLLAMA_HOST=http://localhost:11434

# File Storage
FILE_STORAGE_LOCATION=./storage

# Monitoring
ENABLE_METRICS=true
PROMETHEUS_ENABLED=true
```

### Development Scripts

Create `scripts/dev-start.sh`:

```bash
#!/bin/bash
set -e

echo "🚀 Starting Enterprise RAG Development Environment"

# Start infrastructure
echo "📦 Starting infrastructure services..."
docker-compose up -d

# Wait for services to be ready
echo "⏳ Waiting for services to be ready..."
./scripts/wait-for-services.sh

# Build application
echo "🔨 Building application..."
mvn clean install -DskipTests

# Start services in background
echo "🎯 Starting application services..."
cd rag-auth-service && mvn spring-boot:run > ../logs/auth.log 2>&1 &
sleep 10
cd ../rag-document-service && mvn spring-boot:run > ../logs/document.log 2>&1 &
sleep 10
cd ../rag-embedding-service && mvn spring-boot:run > ../logs/embedding.log 2>&1 &
sleep 10
cd ../rag-core-service && mvn spring-boot:run > ../logs/core.log 2>&1 &
sleep 10
cd ../rag-gateway && mvn spring-boot:run > ../logs/gateway.log 2>&1 &
sleep 10
cd ../rag-admin-service && mvn spring-boot:run > ../logs/admin.log 2>&1 &

echo "✅ All services started! Check logs/ directory for service logs"
echo "🌐 Access points:"
echo "   - API Gateway: http://localhost:8080"
echo "   - Auth Service: http://localhost:8081"
echo "   - Document Service: http://localhost:8083"
echo "   - Grafana: http://localhost:3000 (admin/admin)"
echo "   - Prometheus: http://localhost:9090"
```

## 🏗️ Staging Deployment

### Docker-based Staging

Create `docker-compose.staging.yml`:

```yaml
version: '3.8'

services:
  # Application Services
  rag-auth-service:
    build:
      context: ./rag-auth-service
      dockerfile: Dockerfile
    container_name: rag-auth-staging
    environment:
      SPRING_PROFILES_ACTIVE: staging
      DB_HOST: postgres
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      REDIS_HOST: redis
      KAFKA_BOOTSTRAP_SERVERS: kafka:29092
    ports:
      - "8081:8081"
    depends_on:
      - postgres
      - redis
      - kafka
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  rag-document-service:
    build:
      context: ./rag-document-service
      dockerfile: Dockerfile
    container_name: rag-document-staging
    environment:
      SPRING_PROFILES_ACTIVE: staging
      DB_HOST: postgres
      DB_PASSWORD: ${DB_PASSWORD}
      REDIS_HOST: redis
      KAFKA_BOOTSTRAP_SERVERS: kafka:29092
      FILE_STORAGE_LOCATION: /app/storage
    ports:
      - "8083:8083"
    volumes:
      - document_storage:/app/storage
    depends_on:
      - postgres
      - redis
      - kafka
    restart: unless-stopped

  rag-embedding-service:
    build:
      context: ./rag-embedding-service
      dockerfile: Dockerfile
    container_name: rag-embedding-staging
    environment:
      SPRING_PROFILES_ACTIVE: staging
      DB_HOST: postgres
      DB_PASSWORD: ${DB_PASSWORD}
      REDIS_HOST: redis
      KAFKA_BOOTSTRAP_SERVERS: kafka:29092
      OPENAI_API_KEY: ${OPENAI_API_KEY}
    ports:
      - "8084:8084"
    depends_on:
      - postgres
      - redis
      - kafka
    restart: unless-stopped

  rag-core-service:
    build:
      context: ./rag-core-service
      dockerfile: Dockerfile
    container_name: rag-core-staging
    environment:
      SPRING_PROFILES_ACTIVE: staging
      DB_HOST: postgres
      DB_PASSWORD: ${DB_PASSWORD}
      REDIS_HOST: redis
      KAFKA_BOOTSTRAP_SERVERS: kafka:29092
      OPENAI_API_KEY: ${OPENAI_API_KEY}
      OLLAMA_HOST: http://ollama:11434
    ports:
      - "8082:8082"
    depends_on:
      - postgres
      - redis
      - kafka
      - ollama
    restart: unless-stopped

  rag-gateway:
    build:
      context: ./rag-gateway
      dockerfile: Dockerfile
    container_name: rag-gateway-staging
    environment:
      SPRING_PROFILES_ACTIVE: staging
      AUTH_SERVICE_URL: http://rag-auth-service:8081
      DOCUMENT_SERVICE_URL: http://rag-document-service:8083
      EMBEDDING_SERVICE_URL: http://rag-embedding-service:8084
      CORE_SERVICE_URL: http://rag-core-service:8082
      ADMIN_SERVICE_URL: http://rag-admin-service:8085
    ports:
      - "8080:8080"
    depends_on:
      - rag-auth-service
      - rag-document-service
      - rag-embedding-service
      - rag-core-service
      - rag-admin-service
    restart: unless-stopped

  # Load Balancer (Nginx)
  nginx:
    image: nginx:alpine
    container_name: rag-nginx-staging
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/staging.conf:/etc/nginx/nginx.conf
      - ./ssl/staging:/etc/ssl
    depends_on:
      - rag-gateway
    restart: unless-stopped

volumes:
  document_storage:
  postgres_data_staging:
  redis_data_staging:
  prometheus_data_staging:
  grafana_data_staging:

networks:
  rag-staging:
    driver: bridge
```

### Staging Deployment Script

Create `scripts/deploy-staging.sh`:

```bash
#!/bin/bash
set -e

echo "🏗️ Deploying to Staging Environment"

# Load environment variables
source .env.staging

# Pre-deployment checks
echo "🔍 Running pre-deployment checks..."
./scripts/pre-deploy-checks.sh staging

# Build Docker images
echo "🔨 Building Docker images..."
docker-compose -f docker-compose.staging.yml build

# Database migrations
echo "💾 Running database migrations..."
./scripts/run-migrations.sh staging

# Deploy services
echo "🚀 Deploying services..."
docker-compose -f docker-compose.staging.yml down
docker-compose -f docker-compose.staging.yml up -d

# Health checks
echo "🏥 Running health checks..."
./scripts/health-check.sh staging

# Run integration tests
echo "🧪 Running integration tests..."
./scripts/run-integration-tests.sh staging

echo "✅ Staging deployment completed successfully!"
echo "🌐 Staging URL: https://staging.your-domain.com"
```

## 🏭 Production Deployment

### Kubernetes Deployment

#### Namespace Configuration

Create `k8s/namespace.yml`:

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: enterprise-rag-prod
  labels:
    name: enterprise-rag-prod
    environment: production
```

#### ConfigMaps

Create `k8s/configmaps/app-config.yml`:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: rag-app-config
  namespace: enterprise-rag-prod
data:
  # Database Configuration
  DB_HOST: "postgres-service"
  DB_PORT: "5432"
  DB_NAME: "rag_enterprise"
  DB_USERNAME: "rag_user"
  
  # Redis Configuration
  REDIS_HOST: "redis-service"
  REDIS_PORT: "6379"
  
  # Kafka Configuration
  KAFKA_BOOTSTRAP_SERVERS: "kafka-service:9092"
  
  # Application Configuration
  SPRING_PROFILES_ACTIVE: "production"
  FILE_STORAGE_LOCATION: "/app/storage"
  
  # Monitoring
  MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE: "health,info,prometheus"
  MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS: "when-authorized"
```

#### Secrets

Create `k8s/secrets/app-secrets.yml`:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: rag-app-secrets
  namespace: enterprise-rag-prod
type: Opaque
data:
  # Base64 encoded values
  DB_PASSWORD: <base64-encoded-password>
  JWT_SECRET: <base64-encoded-jwt-secret>
  REDIS_PASSWORD: <base64-encoded-redis-password>
  OPENAI_API_KEY: <base64-encoded-openai-key>
```

#### Persistent Volumes

Create `k8s/storage/persistent-volumes.yml`:

```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: rag-postgres-pv
spec:
  capacity:
    storage: 100Gi
  volumeMode: Filesystem
  accessModes:
    - ReadWriteOnce
  persistentVolumeReclaimPolicy: Retain
  storageClassName: ssd
  hostPath:
    path: /data/postgres

---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: rag-postgres-pvc
  namespace: enterprise-rag-prod
spec:
  accessModes:
    - ReadWriteOnce
  volumeMode: Filesystem
  resources:
    requests:
      storage: 100Gi
  storageClassName: ssd
```

#### Deployments

Create `k8s/deployments/auth-service.yml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: rag-auth-service
  namespace: enterprise-rag-prod
  labels:
    app: rag-auth-service
    tier: backend
spec:
  replicas: 3
  selector:
    matchLabels:
      app: rag-auth-service
  template:
    metadata:
      labels:
        app: rag-auth-service
        tier: backend
    spec:
      containers:
      - name: rag-auth-service
        image: enterprise-rag/auth-service:latest
        ports:
        - containerPort: 8081
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        envFrom:
        - configMapRef:
            name: rag-app-config
        - secretRef:
            name: rag-app-secrets
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 10
        volumeMounts:
        - name: logs
          mountPath: /app/logs
      volumes:
      - name: logs
        emptyDir: {}

---
apiVersion: v1
kind: Service
metadata:
  name: rag-auth-service
  namespace: enterprise-rag-prod
  labels:
    app: rag-auth-service
spec:
  selector:
    app: rag-auth-service
  ports:
  - port: 8081
    targetPort: 8081
    name: http
  type: ClusterIP
```

#### Horizontal Pod Autoscaler

Create `k8s/hpa/auth-service-hpa.yml`:

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: rag-auth-service-hpa
  namespace: enterprise-rag-prod
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: rag-auth-service
  minReplicas: 3
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 10
        periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 0
      policies:
      - type: Percent
        value: 100
        periodSeconds: 15
      - type: Pods
        value: 4
        periodSeconds: 15
      selectPolicy: Max
```

#### Ingress Configuration

Create `k8s/ingress/ingress.yml`:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: rag-ingress
  namespace: enterprise-rag-prod
  annotations:
    kubernetes.io/ingress.class: nginx
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/use-regex: "true"
    nginx.ingress.kubernetes.io/rewrite-target: /$2
    nginx.ingress.kubernetes.io/rate-limit: "100"
    nginx.ingress.kubernetes.io/rate-limit-window: "1m"
spec:
  tls:
  - hosts:
    - api.your-domain.com
    secretName: rag-tls-secret
  rules:
  - host: api.your-domain.com
    http:
      paths:
      - path: /auth(/|$)(.*)
        pathType: Prefix
        backend:
          service:
            name: rag-auth-service
            port:
              number: 8081
      - path: /documents(/|$)(.*)
        pathType: Prefix
        backend:
          service:
            name: rag-document-service
            port:
              number: 8083
      - path: /embeddings(/|$)(.*)
        pathType: Prefix
        backend:
          service:
            name: rag-embedding-service
            port:
              number: 8084
      - path: /rag(/|$)(.*)
        pathType: Prefix
        backend:
          service:
            name: rag-core-service
            port:
              number: 8082
      - path: /(.*)
        pathType: Prefix
        backend:
          service:
            name: rag-gateway
            port:
              number: 8080
```

### Production Deployment Script

Create `scripts/deploy-production.sh`:

```bash
#!/bin/bash
set -e

echo "🏭 Deploying to Production Environment"

# Safety checks
if [[ "$1" != "--confirm" ]]; then
  echo "❌ Production deployment requires --confirm flag"
  echo "Usage: $0 --confirm"
  exit 1
fi

# Load production environment
source .env.production

# Pre-deployment validation
echo "🔍 Running pre-deployment validation..."
./scripts/validate-production.sh

# Create namespace
echo "📁 Creating/updating namespace..."
kubectl apply -f k8s/namespace.yml

# Apply secrets (ensure they exist)
echo "🔐 Applying secrets..."
kubectl apply -f k8s/secrets/

# Apply configmaps
echo "⚙️ Applying configuration..."
kubectl apply -f k8s/configmaps/

# Apply storage
echo "💾 Setting up storage..."
kubectl apply -f k8s/storage/

# Deploy infrastructure services
echo "🏗️ Deploying infrastructure..."
kubectl apply -f k8s/infrastructure/

# Wait for infrastructure to be ready
echo "⏳ Waiting for infrastructure services..."
kubectl wait --for=condition=ready pod -l app=postgres -n enterprise-rag-prod --timeout=300s
kubectl wait --for=condition=ready pod -l app=redis -n enterprise-rag-prod --timeout=300s
kubectl wait --for=condition=ready pod -l app=kafka -n enterprise-rag-prod --timeout=300s

# Run database migrations
echo "📊 Running database migrations..."
kubectl run migration --image=enterprise-rag/migrations:latest \
  --env="DB_HOST=postgres-service" \
  --env="DB_PASSWORD=$DB_PASSWORD" \
  --restart=Never \
  -n enterprise-rag-prod
kubectl wait --for=condition=complete job/migration -n enterprise-rag-prod --timeout=600s

# Deploy application services
echo "🚀 Deploying application services..."
kubectl apply -f k8s/deployments/

# Apply HPA
echo "📈 Setting up autoscaling..."
kubectl apply -f k8s/hpa/

# Apply ingress
echo "🌐 Setting up ingress..."
kubectl apply -f k8s/ingress/

# Apply monitoring
echo "📊 Setting up monitoring..."
kubectl apply -f k8s/monitoring/

# Wait for all deployments to be ready
echo "⏳ Waiting for all services to be ready..."
kubectl wait --for=condition=available deployment --all -n enterprise-rag-prod --timeout=600s

# Run health checks
echo "🏥 Running comprehensive health checks..."
./scripts/health-check.sh production

# Run smoke tests
echo "🧪 Running smoke tests..."
./scripts/smoke-tests.sh production

echo "✅ Production deployment completed successfully!"
echo "🌐 Production URL: https://api.your-domain.com"
echo "📊 Monitoring: https://grafana.your-domain.com"
```

## 🔧 Environment Configuration

### Application Profiles

#### Development (`application-dev.yml`)

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  
  logging:
    level:
      com.enterprise.rag: DEBUG
      org.springframework.security: DEBUG
      org.hibernate.SQL: DEBUG

management:
  endpoints:
    web:
      exposure:
        include: "*"
```

#### Staging (`application-staging.yml`)

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  
  logging:
    level:
      com.enterprise.rag: INFO
      org.springframework.security: WARN

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
```

#### Production (`application-prod.yml`)

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  
  logging:
    level:
      com.enterprise.rag: WARN
      org.springframework.security: ERROR
    file:
      name: /app/logs/application.log
    pattern:
      file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [%X{traceId},%X{spanId}] %logger{36} - %msg%n"

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      show-details: never
```

## 📊 Monitoring & Observability

### Prometheus Configuration

Create `monitoring/prometheus/prometheus-prod.yml`:

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "alert_rules.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093

scrape_configs:
  - job_name: 'rag-auth-service'
    kubernetes_sd_configs:
      - role: endpoints
        namespaces:
          names:
            - enterprise-rag-prod
    relabel_configs:
      - source_labels: [__meta_kubernetes_service_name]
        action: keep
        regex: rag-auth-service
      - source_labels: [__meta_kubernetes_endpoint_port_name]
        action: keep
        regex: http

  - job_name: 'rag-document-service'
    kubernetes_sd_configs:
      - role: endpoints
        namespaces:
          names:
            - enterprise-rag-prod
    relabel_configs:
      - source_labels: [__meta_kubernetes_service_name]
        action: keep
        regex: rag-document-service

  - job_name: 'kubernetes-nodes'
    kubernetes_sd_configs:
      - role: node
    relabel_configs:
      - action: labelmap
        regex: __meta_kubernetes_node_label_(.+)
```

### Grafana Dashboards

Create `monitoring/grafana/dashboards/rag-overview.json`:

```json
{
  "dashboard": {
    "id": null,
    "title": "Enterprise RAG System Overview",
    "tags": ["rag", "microservices"],
    "timezone": "browser",
    "panels": [
      {
        "id": 1,
        "title": "Service Health",
        "type": "stat",
        "targets": [
          {
            "expr": "up{job=~\"rag-.*-service\"}",
            "legendFormat": "{{job}}"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 0}
      },
      {
        "id": 2,
        "title": "Request Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_requests_total{job=~\"rag-.*-service\"}[5m])",
            "legendFormat": "{{job}} - {{method}} {{uri}}"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 0}
      },
      {
        "id": 3,
        "title": "Response Time",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(http_request_duration_seconds_bucket{job=~\"rag-.*-service\"}[5m]))",
            "legendFormat": "95th percentile - {{job}}"
          }
        ],
        "gridPos": {"h": 8, "w": 24, "x": 0, "y": 8}
      }
    ],
    "time": {
      "from": "now-1h",
      "to": "now"
    },
    "refresh": "30s"
  }
}
```

### Alert Rules

Create `monitoring/prometheus/alert_rules.yml`:

```yaml
groups:
  - name: rag-system-alerts
    rules:
      - alert: ServiceDown
        expr: up{job=~"rag-.*-service"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Service {{ $labels.job }} is down"
          description: "Service {{ $labels.job }} has been down for more than 1 minute."

      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value }} errors per second for {{ $labels.job }}"

      - alert: HighLatency
        expr: histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m])) > 1
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "High latency detected"
          description: "95th percentile latency is {{ $value }}s for {{ $labels.job }}"

      - alert: DatabaseConnectionsHigh
        expr: postgresql_connections_active / postgresql_connections_max > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Database connections high"
          description: "Database connection usage is at {{ $value }}%"

      - alert: RedisMemoryHigh
        expr: redis_memory_used_bytes / redis_memory_max_bytes > 0.9
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Redis memory usage high"
          description: "Redis memory usage is at {{ $value }}%"
```

## 🔐 Security Configuration

### Network Security

#### Network Policies

Create `k8s/security/network-policy.yml`:

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: rag-network-policy
  namespace: enterprise-rag-prod
spec:
  podSelector: {}
  policyTypes:
  - Ingress
  - Egress
  
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: ingress-nginx
  - from:
    - podSelector:
        matchLabels:
          tier: backend
    ports:
    - protocol: TCP
      port: 8080
    - protocol: TCP
      port: 8081
    - protocol: TCP
      port: 8082
    - protocol: TCP
      port: 8083
    - protocol: TCP
      port: 8084
    - protocol: TCP
      port: 8085

  egress:
  - to:
    - podSelector:
        matchLabels:
          tier: database
    ports:
    - protocol: TCP
      port: 5432
  - to:
    - podSelector:
        matchLabels:
          tier: cache
    ports:
    - protocol: TCP
      port: 6379
  - to: []
    ports:
    - protocol: TCP
      port: 443  # HTTPS outbound
    - protocol: TCP
      port: 53   # DNS
    - protocol: UDP
      port: 53   # DNS
```

#### Pod Security Policy

Create `k8s/security/pod-security-policy.yml`:

```yaml
apiVersion: policy/v1beta1
kind: PodSecurityPolicy
metadata:
  name: rag-psp
spec:
  privileged: false
  allowPrivilegeEscalation: false
  requiredDropCapabilities:
    - ALL
  volumes:
    - 'configMap'
    - 'emptyDir'
    - 'projected'
    - 'secret'
    - 'persistentVolumeClaim'
  runAsUser:
    rule: 'MustRunAsNonRoot'
  seLinux:
    rule: 'RunAsAny'
  fsGroup:
    rule: 'RunAsAny'
```

### SSL/TLS Configuration

#### Certificate Management

```yaml
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: admin@your-domain.com
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
    - http01:
        ingress:
          class: nginx
```

## 💾 Backup & Recovery

### Database Backup Strategy

Create `scripts/backup-database.sh`:

```bash
#!/bin/bash
set -e

NAMESPACE="${1:-enterprise-rag-prod}"
BACKUP_DIR="/backups/$(date +%Y/%m/%d)"
RETENTION_DAYS=30

echo "📦 Starting database backup for namespace: $NAMESPACE"

# Create backup directory
mkdir -p "$BACKUP_DIR"

# PostgreSQL backup
echo "🗄️ Backing up PostgreSQL..."
kubectl exec -n "$NAMESPACE" deployment/postgres -- pg_dumpall -U rag_user > "$BACKUP_DIR/postgres-$(date +%H%M%S).sql"

# Redis backup
echo "🔄 Backing up Redis..."
kubectl exec -n "$NAMESPACE" deployment/redis -- redis-cli BGSAVE
kubectl cp "$NAMESPACE/redis-pod:/data/dump.rdb" "$BACKUP_DIR/redis-$(date +%H%M%S).rdb"

# Compress backups
echo "🗜️ Compressing backups..."
tar -czf "$BACKUP_DIR.tar.gz" -C "$(dirname "$BACKUP_DIR")" "$(basename "$BACKUP_DIR")"

# Upload to cloud storage (example: AWS S3)
echo "☁️ Uploading to cloud storage..."
aws s3 cp "$BACKUP_DIR.tar.gz" "s3://your-backup-bucket/enterprise-rag/"

# Cleanup old backups
echo "🧹 Cleaning up old backups..."
find /backups -name "*.tar.gz" -mtime +$RETENTION_DAYS -delete

echo "✅ Backup completed successfully!"
```

### Disaster Recovery Plan

Create `scripts/disaster-recovery.sh`:

```bash
#!/bin/bash
set -e

BACKUP_FILE="$1"
NAMESPACE="${2:-enterprise-rag-prod}"

if [[ -z "$BACKUP_FILE" ]]; then
  echo "❌ Usage: $0 <backup-file> [namespace]"
  exit 1
fi

echo "🚨 Starting disaster recovery for namespace: $NAMESPACE"
echo "📁 Using backup file: $BACKUP_FILE"

# Scale down all services
echo "⏸️ Scaling down all services..."
kubectl scale deployment --all --replicas=0 -n "$NAMESPACE"

# Wait for pods to terminate
echo "⏳ Waiting for pods to terminate..."
kubectl wait --for=delete pod --all -n "$NAMESPACE" --timeout=300s

# Restore database
echo "🗄️ Restoring PostgreSQL..."
kubectl exec -n "$NAMESPACE" deployment/postgres -- psql -U rag_user -f /tmp/restore.sql

# Restore Redis
echo "🔄 Restoring Redis..."
kubectl cp "$BACKUP_FILE/redis.rdb" "$NAMESPACE/redis-pod:/data/dump.rdb"
kubectl exec -n "$NAMESPACE" deployment/redis -- redis-cli DEBUG RESTART

# Scale up services
echo "▶️ Scaling up services..."
kubectl scale deployment rag-auth-service --replicas=3 -n "$NAMESPACE"
kubectl scale deployment rag-document-service --replicas=3 -n "$NAMESPACE"
kubectl scale deployment rag-embedding-service --replicas=2 -n "$NAMESPACE"
kubectl scale deployment rag-core-service --replicas=3 -n "$NAMESPACE"
kubectl scale deployment rag-gateway --replicas=2 -n "$NAMESPACE"
kubectl scale deployment rag-admin-service --replicas=1 -n "$NAMESPACE"

# Wait for services to be ready
echo "⏳ Waiting for services to be ready..."
kubectl wait --for=condition=available deployment --all -n "$NAMESPACE" --timeout=600s

# Run health checks
echo "🏥 Running health checks..."
./scripts/health-check.sh production

echo "✅ Disaster recovery completed successfully!"
```

## 🔧 Performance Tuning

### JVM Tuning

Create `k8s/deployments/optimized-auth-service.yml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: rag-auth-service
  namespace: enterprise-rag-prod
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: rag-auth-service
        image: enterprise-rag/auth-service:latest
        env:
        - name: JAVA_OPTS
          value: >-
            -Xms1g -Xmx2g
            -XX:+UseG1GC
            -XX:MaxGCPauseMillis=200
            -XX:+HeapDumpOnOutOfMemoryError
            -XX:HeapDumpPath=/app/logs/
            -Dspring.profiles.active=production
            -Dlogging.config=/app/config/logback-spring.xml
        resources:
          requests:
            memory: "1.5Gi"
            cpu: "500m"
          limits:
            memory: "2.5Gi"
            cpu: "1000m"
```

### Database Optimization

Create `infrastructure/postgres/postgresql.conf`:

```conf
# Connection Settings
max_connections = 200
shared_buffers = 2GB
effective_cache_size = 6GB
maintenance_work_mem = 512MB
checkpoint_completion_target = 0.9
wal_buffers = 16MB
default_statistics_target = 100
random_page_cost = 1.1
effective_io_concurrency = 200

# Logging
log_min_duration_statement = 1000
log_checkpoints = on
log_connections = on
log_disconnections = on
log_lock_waits = on

# Replication (if using)
wal_level = replica
max_wal_senders = 3
max_replication_slots = 3
```

### Redis Optimization

Create `infrastructure/redis/redis.conf`:

```conf
# Memory Management
maxmemory 4gb
maxmemory-policy allkeys-lru
maxmemory-samples 5

# Persistence
save 900 1
save 300 10
save 60 10000
rdbcompression yes
rdbchecksum yes

# Networking
tcp-keepalive 300
timeout 0

# Performance
hash-max-ziplist-entries 512
hash-max-ziplist-value 64
list-max-ziplist-size -2
set-max-intset-entries 512
zset-max-ziplist-entries 128
zset-max-ziplist-value 64
```

## 🔍 Troubleshooting

### Common Issues and Solutions

#### 1. Service Not Starting

```bash
# Check pod status
kubectl get pods -n enterprise-rag-prod

# Check pod logs
kubectl logs -f deployment/rag-auth-service -n enterprise-rag-prod

# Check events
kubectl get events -n enterprise-rag-prod --sort-by=.metadata.creationTimestamp

# Debug pod
kubectl exec -it deployment/rag-auth-service -n enterprise-rag-prod -- /bin/bash
```

#### 2. Database Connection Issues

```bash
# Test database connectivity
kubectl run db-test --image=postgres:15 --rm -it --restart=Never \
  --env="PGPASSWORD=password" \
  -- psql -h postgres-service -U rag_user -d rag_enterprise

# Check database logs
kubectl logs deployment/postgres -n enterprise-rag-prod

# Check connection pool metrics
curl http://localhost:8081/actuator/metrics/hikaricp.connections.active
```

#### 3. Performance Issues

```bash
# Check resource usage
kubectl top pods -n enterprise-rag-prod
kubectl top nodes

# Check HPA status
kubectl get hpa -n enterprise-rag-prod

# Analyze slow queries
kubectl exec -it deployment/postgres -n enterprise-rag-prod -- \
  psql -U rag_user -d rag_enterprise -c "
  SELECT query, calls, total_time, mean_time 
  FROM pg_stat_statements 
  ORDER BY total_time DESC 
  LIMIT 10;"
```

#### 4. Memory Issues

```bash
# Check JVM memory usage
curl http://localhost:8081/actuator/metrics/jvm.memory.used
curl http://localhost:8081/actuator/metrics/jvm.memory.max

# Generate heap dump
kubectl exec deployment/rag-auth-service -n enterprise-rag-prod -- \
  jcmd 1 GC.run_finalization
kubectl exec deployment/rag-auth-service -n enterprise-rag-prod -- \
  jcmd 1 VM.classloader_stats
```

### Debugging Commands

Create `scripts/debug-toolkit.sh`:

```bash
#!/bin/bash

NAMESPACE="${1:-enterprise-rag-prod}"
SERVICE="${2:-rag-auth-service}"

echo "🔍 Debug Toolkit for $SERVICE in $NAMESPACE"

echo "📊 Resource Usage:"
kubectl top pods -l app=$SERVICE -n $NAMESPACE

echo "📋 Pod Status:"
kubectl get pods -l app=$SERVICE -n $NAMESPACE -o wide

echo "🔗 Service Endpoints:"
kubectl get endpoints $SERVICE -n $NAMESPACE

echo "📝 Recent Events:"
kubectl get events -n $NAMESPACE --field-selector involvedObject.name=$SERVICE --sort-by=.metadata.creationTimestamp

echo "🏥 Health Check:"
kubectl exec deployment/$SERVICE -n $NAMESPACE -- \
  curl -f http://localhost:8081/actuator/health || echo "Health check failed"

echo "📊 Metrics Sample:"
kubectl exec deployment/$SERVICE -n $NAMESPACE -- \
  curl -s http://localhost:8081/actuator/metrics | head -20

echo "📁 Recent Logs:"
kubectl logs deployment/$SERVICE -n $NAMESPACE --tail=50
```

## 📈 Scaling Guidelines

### Horizontal Scaling

| Service | Min Replicas | Max Replicas | CPU Threshold | Memory Threshold |
|---------|--------------|--------------|---------------|------------------|
| Auth Service | 3 | 20 | 70% | 80% |
| Document Service | 2 | 15 | 75% | 85% |
| Embedding Service | 2 | 10 | 80% | 90% |
| Core Service | 3 | 25 | 70% | 80% |
| Gateway | 2 | 10 | 60% | 70% |
| Admin Service | 1 | 5 | 80% | 85% |

### Vertical Scaling

```yaml
# Production Resource Allocations
resources:
  auth-service:
    requests: { memory: "1.5Gi", cpu: "500m" }
    limits: { memory: "2.5Gi", cpu: "1000m" }
  
  document-service:
    requests: { memory: "2Gi", cpu: "750m" }
    limits: { memory: "4Gi", cpu: "1500m" }
  
  embedding-service:
    requests: { memory: "3Gi", cpu: "1000m" }
    limits: { memory: "6Gi", cpu: "2000m" }
  
  core-service:
    requests: { memory: "2Gi", cpu: "750m" }
    limits: { memory: "4Gi", cpu: "1500m" }
```

## 🎯 Deployment Checklist

### Pre-Deployment

- [ ] Environment variables configured
- [ ] Secrets created and validated
- [ ] Database migrations tested
- [ ] SSL certificates valid
- [ ] Resource quotas set
- [ ] Monitoring configured
- [ ] Backup strategy implemented

### Deployment

- [ ] Infrastructure services deployed
- [ ] Application services deployed
- [ ] Health checks passing
- [ ] Ingress configured
- [ ] DNS records updated
- [ ] SSL/TLS working
- [ ] Monitoring active

### Post-Deployment

- [ ] Smoke tests passed
- [ ] Performance tests executed
- [ ] Security scan completed
- [ ] Documentation updated
- [ ] Team notified
- [ ] Rollback plan confirmed

---

## 📞 Support & Maintenance

For ongoing support and maintenance:

1. **Monitor dashboards** regularly
2. **Review logs** for errors and warnings
3. **Update dependencies** quarterly
4. **Backup verification** monthly
5. **Security updates** as available
6. **Performance reviews** bi-annually

**Emergency Contact**: DevOps Team - `devops@your-company.com`

**Documentation**: Keep this guide updated with environment changes and lessons learned.

---

*Last Updated: August 2025*
*Version: 1.0.0*
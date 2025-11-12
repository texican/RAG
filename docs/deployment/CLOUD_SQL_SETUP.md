---
version: 1.0.0
last-updated: 2025-11-12
status: active
applies-to: 0.8.0-SNAPSHOT
category: deployment
---

# GCP-SQL-004: Cloud SQL PostgreSQL Setup

## Overview

Cloud SQL PostgreSQL instance configured for the RAG system with pgvector extension support.

**Status**: ✅ COMPLETE (2025-11-07)

## Instance Details

- **Instance Name**: `rag-postgres`
- **Database Version**: PostgreSQL 15
- **Connection Name**: `byo-rag-dev:us-central1:rag-postgres`
- **Public IP**: `104.197.76.156`
- **Region**: `us-central1-a`
- **Tier**: `db-custom-2-7680` (2 vCPU, 7.5 GB RAM)
- **Storage**: 20 GB SSD (auto-increase enabled)
- **Availability**: Zonal

## Databases

Three databases created for microservice isolation:

1. **rag_auth** - Authentication service (users, tenants, JWT tokens)
2. **rag_document** - Document service (documents, chunks, metadata)
3. **rag_admin** - Admin service (system config, analytics, audit logs)

All databases have:
- Character set: `UTF8`
- Collation: `en_US.UTF8`
- pgvector extension: v0.8.0 (enabled)

## Users and Authentication

### Root User (postgres)
- **Username**: `postgres`
- **Password**: Stored in Secret Manager (`cloudsql-root-password`)
- **Purpose**: Database administration only

### Application User
- **Username**: `rag_user`
- **Password**: Stored in Secret Manager (`cloudsql-app-password`)
- **Purpose**: Application database access
- **Permissions**: Full access to all three databases

### IAM Authentication
- Enabled via `cloudsql.iam_authentication=on` flag
- Ready for GKE Workload Identity integration

## Connection Details

### JDBC Connection Strings

```properties
# Auth Service
spring.datasource.url=jdbc:postgresql://104.197.76.156:5432/rag_auth
spring.datasource.username=${DB_USERNAME:rag_user}
spring.datasource.password=${DB_PASSWORD:from-secret-manager}

# Document Service
spring.datasource.url=jdbc:postgresql://104.197.76.156:5432/rag_document
spring.datasource.username=${DB_USERNAME:rag_user}
spring.datasource.password=${DB_PASSWORD:from-secret-manager}

# Admin Service
spring.datasource.url=jdbc:postgresql://104.197.76.156:5432/rag_admin
spring.datasource.username=${DB_USERNAME:rag_user}
spring.datasource.password=${DB_PASSWORD:from-secret-manager}
```

### Cloud SQL Proxy (Local Development)

```bash
# Install Cloud SQL Proxy
brew install cloud-sql-proxy

# Connect to instance
cloud-sql-proxy --port 5432 byo-rag-dev:us-central1:rag-postgres

# Then use localhost in your application:
jdbc:postgresql://localhost:5432/{database}
```

### Direct psql Connection

```bash
# Get password from Secret Manager
APP_PASSWORD=$(gcloud secrets versions access latest \
    --secret="cloudsql-app-password" \
    --project="byo-rag-dev")

# Connect to a database
PGPASSWORD="$APP_PASSWORD" psql \
    -h 104.197.76.156 \
    -U rag_user \
    -d rag_auth
```

## Backup and Maintenance

### Automated Backups
- **Enabled**: Yes
- **Start Time**: 03:00 UTC daily
- **Retention**: Default (7 days)
- **Location**: us-central1

### Maintenance Windows
- **Day**: Sunday
- **Hour**: 04:00 UTC
- **Duration**: ~1 hour

## Security

### Network Security
- **Public IP**: Enabled (authorized networks: 0.0.0.0/0)
- **SSL**: Enforced for all connections
- **Private IP**: Not configured (requires VPC peering)

⚠️ **Note**: For production, configure private IP and remove public access.

### Secret Manager Integration

All sensitive credentials stored in Secret Manager:

```bash
# Retrieve root password
gcloud secrets versions access latest \
    --secret="cloudsql-root-password" \
    --project="byo-rag-dev"

# Retrieve app password  
gcloud secrets versions access latest \
    --secret="cloudsql-app-password" \
    --project="byo-rag-dev"

# Retrieve connection info (JSON)
gcloud secrets versions access latest \
    --secret="cloudsql-connection-info" \
    --project="byo-rag-dev"
```

## pgvector Extension

### Installed Version
- **Version**: 0.8.0
- **Enabled on**: All databases (rag_auth, rag_document, rag_admin)

### Verification

```sql
-- Connect to any database
\dx vector

-- Expected output:
                          List of installed extensions
  Name  | Version | Schema |                     Description                      
--------+---------+--------+------------------------------------------------------
 vector | 0.8.0   | public | vector data type and ivfflat and hnsw access methods
```

### Vector Operations

```sql
-- Create table with vector column
CREATE TABLE embeddings (
    id SERIAL PRIMARY KEY,
    content TEXT,
    embedding vector(1536)  -- OpenAI embedding dimension
);

-- Create HNSW index for fast similarity search
CREATE INDEX ON embeddings USING hnsw (embedding vector_cosine_ops);

-- Similarity search
SELECT content 
FROM embeddings 
ORDER BY embedding <=> '[0.1, 0.2, ...]'::vector 
LIMIT 10;
```

## GKE Integration (Future)

For Kubernetes deployment, Cloud SQL will be accessed via:

1. **Cloud SQL Proxy Sidecar** - Recommended approach
2. **Private IP** - Requires VPC peering setup
3. **Workload Identity** - For IAM-based authentication

### Workload Identity Setup (Pending GCP-K8S-008)

```bash
# Create service account for Cloud SQL access
gcloud iam service-accounts create cloud-sql-client \
    --project=byo-rag-dev

# Grant Cloud SQL Client role
gcloud projects add-iam-policy-binding byo-rag-dev \
    --member="serviceAccount:cloud-sql-client@byo-rag-dev.iam.gserviceaccount.com" \
    --role="roles/cloudsql.client"

# Bind to Kubernetes service account (in GKE setup)
```

## Cost Estimate

- **Instance**: ~$70/month (db-custom-2-7680, zonal)
- **Storage**: ~$0.34/GB/month (SSD)
- **Backups**: ~$0.08/GB/month
- **Network Egress**: Variable

**Estimated Monthly**: ~$77-85 (excluding network)

## Management Console

- **Instance Overview**: https://console.cloud.google.com/sql/instances/rag-postgres/overview?project=byo-rag-dev
- **Monitoring**: https://console.cloud.google.com/sql/instances/rag-postgres/monitoring?project=byo-rag-dev
- **Backups**: https://console.cloud.google.com/sql/instances/rag-postgres/backups?project=byo-rag-dev

## Migration from Local PostgreSQL

When migrating from docker-compose PostgreSQL:

1. **Export local data** (if needed):
   ```bash
   docker exec rag-postgres pg_dumpall -U rag_user > /tmp/local_backup.sql
   ```

2. **Import to Cloud SQL**:
   ```bash
   psql -h 104.197.76.156 -U rag_user -d rag_auth < /tmp/local_backup.sql
   ```

3. **Update service configurations** to use Cloud SQL IP

4. **Test connectivity** before switching production traffic

## Troubleshooting

### Connection Issues

```bash
# Verify instance is running
gcloud sql instances describe rag-postgres \
    --project=byo-rag-dev \
    --format="value(state)"

# Check authorized networks
gcloud sql instances describe rag-postgres \
    --project=byo-rag-dev \
    --format="value(settings.ipConfiguration.authorizedNetworks)"

# Test connectivity
telnet 104.197.76.156 5432
```

### Extension Issues

```bash
# Connect and verify pgvector
psql -h 104.197.76.156 -U postgres -d rag_auth -c "\dx vector"

# If not installed, enable it
psql -h 104.197.76.156 -U postgres -d rag_auth \
    -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

## Scripts

Setup scripts located in `scripts/gcp/`:

- **08-setup-cloud-sql.sh** - Creates instance, databases, users, stores secrets
- **09-enable-pgvector.sh** - Enables pgvector extension on all databases

## Next Steps

1. ✅ Cloud SQL instance created and configured
2. ✅ Databases and users set up
3. ✅ pgvector extension enabled
4. ⏳ Update service configurations to use Cloud SQL
5. ⏳ Set up Cloud SQL Proxy for local development
6. ⏳ Configure GKE workload identity (GCP-K8S-008)
7. ⏳ Implement private IP with VPC peering (production hardening)

## References

- [Cloud SQL for PostgreSQL Documentation](https://cloud.google.com/sql/docs/postgres)
- [pgvector Extension](https://github.com/pgvector/pgvector)
- [Cloud SQL Proxy](https://cloud.google.com/sql/docs/postgres/sql-proxy)
- [Workload Identity](https://cloud.google.com/kubernetes-engine/docs/how-to/workload-identity)

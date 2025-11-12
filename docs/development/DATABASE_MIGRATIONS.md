---
version: 1.0.0
last-updated: 2025-11-12
status: active
applies-to: 0.8.0-SNAPSHOT
category: development
---

# Database Migrations with Flyway

## Overview

The RAG system uses **Flyway** for version-controlled database schema migrations. This ensures that schema changes are:
- **Version-controlled** in Git
- **Reviewable** through pull requests
- **Repeatable** across environments
- **Auditable** with complete history
- **Safe** for production deployments

## Why Flyway?

### Problems with Hibernate `ddl-auto: update`
- ❌ No version control for schema changes
- ❌ Cannot rollback migrations  
- ❌ Cannot rename/drop columns safely
- ❌ Schema changes happen automatically without review
- ❌ No audit trail
- ❌ **NOT production-ready**

### Benefits of Flyway
- ✅ Version-controlled schema changes (Git history)
- ✅ Peer review of database changes (PR process)
- ✅ Automatic rollback support
- ✅ Audit trail of all schema modifications
- ✅ Safe production deployments
- ✅ Prevents accidental schema changes

## Configuration

### Services Using Flyway
- **rag-auth-service**: Manages `tenants` and `users` tables
- **rag-document-service**: Manages all tables (`tenants`, `users`, `documents`, `document_chunks`)

### Application Configuration
```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    baseline-version: 0
    locations: classpath:db/migration
    validate-on-migrate: true
  
  jpa:
    hibernate:
      ddl-auto: validate  # Only validate, don't auto-modify
```

### Baseline Migrations Created
1. **V1__create_tenants_table.sql** - Multi-tenant organizations
2. **V2__create_users_table.sql** - User accounts with roles
3. **V3__create_documents_table.sql** - Document metadata (document-service only)
4. **V4__create_document_chunks_table.sql** - Text chunks for RAG (document-service only)

## Migration File Structure

### Naming Convention
```
V{version}__{description}.sql
```

**Examples:**
- `V1__create_tenants_table.sql`
- `V2__create_users_table.sql`
- `V5__add_tenant_billing_fields.sql`
- `V6__add_user_preferences_column.sql`

### File Locations
```
rag-auth-service/src/main/resources/db/migration/
rag-document-service/src/main/resources/db/migration/
```

### Migration File Template
```sql
-- Flyway Migration VX: [Description]
-- Description: [Detailed explanation of changes]
-- Author: [Your Name]
-- Date: YYYY-MM-DD

-- Main changes
CREATE TABLE IF NOT EXISTS my_table (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    -- other columns...
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_my_table_field ON my_table(field);

-- Add comments
COMMENT ON TABLE my_table IS 'Description of the table';
COMMENT ON COLUMN my_table.id IS 'Unique identifier';
```

## Migration Workflow

### Creating a New Migration

1. **Identify the schema change needed**
   - Adding a column
   - Creating a new table
   - Modifying constraints
   - Adding indexes

2. **Determine the next version number**
   ```bash
   # List existing migrations
   ls -1 rag-auth-service/src/main/resources/db/migration/
   # V1__create_tenants_table.sql
   # V2__create_users_table.sql
   # Next version: V3
   ```

3. **Create the migration file**
   ```bash
   # Example: Adding a billing field to tenants
   touch rag-auth-service/src/main/resources/db/migration/V5__add_tenant_billing_fields.sql
   ```

4. **Write the migration SQL**
   ```sql
   -- Flyway Migration V5: Add Billing Fields to Tenants
   -- Description: Add billing_email and subscription_tier columns
   -- Author: Your Name
   -- Date: 2025-11-12

   ALTER TABLE tenants 
   ADD COLUMN IF NOT EXISTS billing_email VARCHAR(255),
   ADD COLUMN IF NOT EXISTS subscription_tier VARCHAR(20) DEFAULT 'FREE';

   CREATE INDEX IF NOT EXISTS idx_tenant_subscription ON tenants(subscription_tier);

   COMMENT ON COLUMN tenants.billing_email IS 'Email address for billing notifications';
   COMMENT ON COLUMN tenants.subscription_tier IS 'Subscription level: FREE, PRO, ENTERPRISE';
   ```

5. **Test the migration locally**
   ```bash
   # Restart the service to apply migration
   docker-compose down
   docker-compose up -d postgres
   docker-compose up auth-service
   ```

6. **Verify migration applied**
   ```bash
   # Check Flyway schema history
   docker exec -it postgres psql -U rag_user -d byo_rag_local
   
   SELECT * FROM flyway_schema_history ORDER BY installed_rank;
   ```

7. **Commit to Git**
   ```bash
   git add rag-auth-service/src/main/resources/db/migration/V5__add_tenant_billing_fields.sql
   git commit -m "Add billing fields to tenants table"
   ```

### Applying Migrations

#### Development (Local)
Migrations run automatically when the service starts:
```bash
docker-compose up -d
```

#### Production
Migrations run automatically on deployment, but:
1. **Always test migrations in staging first**
2. **Review migration SQL in pull request**
3. **Ensure migrations are backward-compatible**
4. **Have rollback plan ready**

## Best Practices

### DO ✅
- **Use versioned migration files** (V1, V2, V3...)
- **Test migrations locally before committing**
- **Make migrations idempotent** (use `IF NOT EXISTS`, `IF EXISTS`)
- **Add descriptive comments** to tables and columns
- **Create indexes** for frequently queried columns
- **Use foreign key constraints** for referential integrity
- **Commit migrations to Git** with descriptive messages
- **Review migrations in pull requests**
- **Keep migrations small and focused**

### DON'T ❌
- **Never modify existing migration files** after they've been applied
- **Don't use `DROP COLUMN` without a data migration plan**
- **Don't rename columns without a transition period**
- **Don't skip version numbers**
- **Don't commit untested migrations**
- **Don't use `ddl-auto: update` in production**
- **Don't ignore migration failures**

## Common Migration Scenarios

### Adding a Column
```sql
-- V5__add_user_phone_column.sql
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS phone VARCHAR(20);

COMMENT ON COLUMN users.phone IS 'User phone number (optional)';
```

### Adding an Index
```sql
-- V6__add_document_created_at_index.sql
CREATE INDEX IF NOT EXISTS idx_document_created_at ON documents(created_at DESC);
```

### Adding a Foreign Key
```sql
-- V7__add_document_category_fk.sql
ALTER TABLE documents
ADD COLUMN IF NOT EXISTS category_id UUID,
ADD CONSTRAINT fk_document_category 
  FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_document_category ON documents(category_id);
```

### Creating a New Table
```sql
-- V8__create_categories_table.sql
CREATE TABLE IF NOT EXISTS categories (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    tenant_id UUID NOT NULL,
    CONSTRAINT fk_category_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_category_name ON categories(tenant_id, name);
```

### Renaming a Column (Safe Method)
```sql
-- Step 1 - V9__add_new_column.sql
ALTER TABLE users ADD COLUMN IF NOT EXISTS full_name VARCHAR(200);

-- Step 2 - Deploy code that writes to both old and new columns

-- Step 3 - V10__migrate_data.sql
UPDATE users SET full_name = first_name || ' ' || last_name WHERE full_name IS NULL;

-- Step 4 - Deploy code that only uses new column

-- Step 5 - V11__drop_old_columns.sql (weeks later)
ALTER TABLE users DROP COLUMN IF EXISTS first_name;
ALTER TABLE users DROP COLUMN IF EXISTS last_name;
```

## Troubleshooting

### Migration Failed
```bash
# Check Flyway history
docker exec -it postgres psql -U rag_user -d byo_rag_local
SELECT * FROM flyway_schema_history WHERE success = false;
```

**Resolution:**
1. Fix the migration file
2. Delete the failed migration record:
   ```sql
   DELETE FROM flyway_schema_history WHERE version = 'X';
   ```
3. Restart the service to retry

### Schema Mismatch Error
```
Caused by: org.hibernate.tool.schema.spi.SchemaManagementException: 
Schema-validation: missing table [some_table]
```

**Cause:** Entity exists but migration doesn't create the table

**Resolution:** Create a migration for the missing table

### Checksum Mismatch
```
Migration checksum mismatch for migration version X
```

**Cause:** Migration file was modified after being applied

**Resolution:**
- **Development**: Delete the migration record and reapply
- **Production**: NEVER modify applied migrations

## Flyway Commands

### View Migration Status
```bash
docker exec -it postgres psql -U rag_user -d byo_rag_local

-- View all migrations
SELECT installed_rank, version, description, type, script, checksum, installed_on, success 
FROM flyway_schema_history 
ORDER BY installed_rank;
```

### Baseline Existing Database
If you have an existing database and want to start using Flyway:
```sql
-- In application.yml
spring:
  flyway:
    baseline-on-migrate: true
    baseline-version: 0
```

This marks the current schema as version 0, and new migrations will be V1, V2, etc.

## Testing Migrations

### Local Testing
```bash
# 1. Stop services
docker-compose down

# 2. Reset database (CAREFUL - this deletes all data)
docker volume rm rag_postgres_data

# 3. Start fresh
docker-compose up -d postgres
docker-compose up -d auth-service document-service

# 4. Verify migrations
docker-compose logs auth-service | grep Flyway
docker-compose logs document-service | grep Flyway
```

### Verify Schema
```bash
docker exec -it postgres psql -U rag_user -d byo_rag_local

\dt  -- List all tables
\d tenants  -- Describe tenants table
\d users    -- Describe users table
```

## CI/CD Integration

Flyway runs automatically on service startup, so:
1. **Build** service with new migration files
2. **Deploy** to environment
3. **Migration runs automatically** on first startup
4. **Service validates** schema matches entities

## Related Documentation

- [Database Persistence Fix](../operations/DATABASE_PERSISTENCE_FIX.md)
- [Spring Boot + Flyway Documentation](https://flywaydb.org/documentation/usage/plugins/springboot)
- [Flyway Versioned Migrations](https://flywaydb.org/documentation/concepts/migrations#versioned-migrations)

## Summary

Flyway provides production-ready database migration management with:
- ✅ Version control and audit trail
- ✅ Safe, reviewable schema changes
- ✅ Automatic migration execution
- ✅ Schema validation against entities
- ✅ No more `ddl-auto: update` surprises

All schema changes **must** now go through Flyway migrations. This ensures safety, traceability, and team collaboration on database changes.

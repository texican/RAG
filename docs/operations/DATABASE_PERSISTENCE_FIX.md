# Database Persistence Fix

**Date**: 2025-10-05
**Issue**: Data loss on service restart
**Root Cause**: Hibernate `ddl-auto: create-drop` configuration
**Status**: ✅ FIXED

---

## Problem

Tenant and user data was being lost every time services restarted because Hibernate was configured with `ddl-auto: create-drop`, which drops and recreates all tables on each application startup.

**Impact:**
- Tenant data created in one session disappeared after service restart
- Users had to re-register after every deployment
- E2E tests failed because tenants didn't persist
- Development workflow disrupted

**Example Error:**
```
TenantNotFoundException: Tenant not found with ID: 00b8c0e2-fc71-4a55-a5df-f45b4ad44a86
```

---

## Root Cause

Configuration in `application.yml` for both services:

```yaml
# BEFORE (causes data loss):
jpa:
  hibernate:
    ddl-auto: create-drop  # ❌ Drops tables on shutdown!
```

**Hibernate ddl-auto options:**
- `create-drop`: Creates schema on startup, **drops on shutdown** (development only)
- `create`: Creates schema on startup, keeps on shutdown (still loses data on restart)
- `update`: Updates schema to match entities, **preserves data** ✅
- `validate`: Only validates schema, no changes
- `none`: No schema management

---

## Solution

Changed Hibernate configuration to use `update` instead of `create-drop`:

```yaml
# AFTER (preserves data):
jpa:
  hibernate:
    ddl-auto: update  # ✅ Preserves data across restarts!
```

### Files Modified:

1. **rag-auth-service/src/main/resources/application.yml** (line 16)
   ```yaml
   ddl-auto: update  # Changed from create-drop
   ```

2. **rag-document-service/src/main/resources/application.yml** (line 16)
   ```yaml
   ddl-auto: update  # Changed from create-drop
   ```

---

## Testing

### Before Fix:
```bash
# Create tenant
$ ./scripts/utils/admin-login.sh
# Tenant: 00b8c0e2-fc71-4a55-a5df-f45b4ad44a86

# Restart service
$ docker-compose restart rag-auth

# Try to use tenant - FAILS
$ curl -H 'X-Tenant-ID: 00b8c0e2-fc71-4a55-a5df-f45b4ad44a86' ...
# Error: Tenant not found
```

### After Fix:
```bash
# Create tenant
$ ./scripts/utils/admin-login.sh
# Tenant: 00b8c0e2-fc71-4a55-a5df-f45b4ad44a86

# Restart service
$ docker-compose restart rag-auth

# Try to use tenant - SUCCESS!
$ curl -H 'X-Tenant-ID: 00b8c0e2-fc71-4a55-a5df-f45b4ad44a86' ...
# Works correctly - tenant persists!
```

---

## Production Considerations

### Current Approach: `update`

**Pros:**
- ✅ Preserves data across restarts
- ✅ Automatically updates schema for new columns
- ✅ Good for development and early production

**Cons:**
- ⚠️ Cannot remove columns (leaves orphaned columns)
- ⚠️ Cannot rename columns safely
- ⚠️ No rollback mechanism
- ⚠️ Schema changes happen automatically without review

### Recommended Production Approach: Migration Tools

For production, consider using proper database migration tools:

**Option 1: Flyway** (Recommended)
```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
  jpa:
    hibernate:
      ddl-auto: validate  # Only validate, don't modify
```

**Option 2: Liquibase**
```yaml
spring:
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.xml
  jpa:
    hibernate:
      ddl-auto: validate
```

**Benefits:**
- Version-controlled migrations
- Rollback support
- Audit trail of schema changes
- Team collaboration on schema
- Safe production deployments

---

## Current Status

### Development Environment: ✅ FIXED
- Auth service: `ddl-auto: update`
- Document service: `ddl-auto: update`
- Data persists across restarts
- Development workflow restored

### Production Readiness: ⚠️ NEEDS IMPROVEMENT
- Current `update` strategy works but not ideal for production
- See TECH-DEBT-005 for migration to Flyway/Liquibase
- Should be addressed before production deployment

---

## Services Status

| Service | ddl-auto Setting | Status |
|---------|------------------|--------|
| rag-auth-service | `update` | ✅ Fixed |
| rag-document-service | `update` | ✅ Fixed |
| rag-admin-service | `update` | ✅ Already correct |
| rag-core-service | `validate` | ✅ Already correct |
| rag-embedding-service | N/A (no JPA) | N/A |

---

## Related Issues

- **STORY-017**: Fix Tenant Data Synchronization (revealed this issue)
- **TECH-DEBT-005**: Implement Proper Database Migration Strategy (Flyway)

---

## Lessons Learned

1. **`create-drop` is for tests only** - Never use in persistent environments
2. **Schema management matters** - Small config changes have big impacts
3. **Test persistence** - Always verify data survives restarts
4. **Plan for production** - `update` is interim; need migration tools for prod

---

## Next Steps

1. ✅ **Immediate**: Changed to `update` (DONE)
2. ⏳ **Short-term**: Monitor for schema update issues
3. 📋 **Medium-term**: Implement Flyway migrations (TECH-DEBT-005)
4. 🎯 **Long-term**: Establish DB change review process

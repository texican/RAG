#!/bin/bash

# Create Initial Admin User - Bootstrap script for creating the first admin user
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-rag_enterprise}"
DB_USER="${DB_USER:-rag_user}"
DB_PASSWORD="${DB_PASSWORD:-rag_password}"

ADMIN_EMAIL="${ADMIN_EMAIL:-admin@enterprise-rag.com}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-admin123}"
ADMIN_FIRST_NAME="${ADMIN_FIRST_NAME:-System}"
ADMIN_LAST_NAME="${ADMIN_LAST_NAME:-Administrator}"

echo -e "${BLUE}🔐 Creating Initial Admin User${NC}"
echo "================================="

# Check if PostgreSQL is accessible
if ! pg_isready -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" >/dev/null 2>&1; then
    echo -e "${RED}❌ PostgreSQL is not ready on ${DB_HOST}:${DB_PORT}${NC}"
    echo "Please ensure the database is running and accessible"
    exit 1
fi

echo -e "${GREEN}✅ Database connection verified${NC}"

# Generate password hash (Spring Security BCrypt with strength 10)
# Use a simple Java program to generate BCrypt hash
cat > /tmp/bcrypt_hash.java << 'EOF'
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class bcrypt_hash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        String password = args.length > 0 ? args[0] : "admin123";
        String hash = encoder.encode(password);
        System.out.println(hash);
    }
}
EOF

# Try to use Java to generate hash, fallback to pre-generated hash if not available  
if command -v javac >/dev/null 2>&1 && [[ -f "/Users/stryfe/Projects/RAG/rag-admin-service/target/classes" ]]; then
    BCRYPT_PASSWORD=$(cd /tmp && javac -cp "/Users/stryfe/Projects/RAG/rag-admin-service/target/classes:/Users/stryfe/.m2/repository/org/springframework/security/spring-security-crypto/6.1.11/spring-security-crypto-6.1.11.jar" bcrypt_hash.java && java -cp ".:/Users/stryfe/Projects/RAG/rag-admin-service/target/classes:/Users/stryfe/.m2/repository/org/springframework/security/spring-security-crypto/6.1.11/spring-security-crypto-6.1.11.jar" bcrypt_hash "$ADMIN_PASSWORD")
else
    # Pre-generated BCrypt hash for "admin123" with strength 10
    BCRYPT_PASSWORD='$2a$10$8.1VrLQH1dpLgUaLtZ2VHeyJmY1gWW0Ih8z8wJLM8N1r8xO1m.NJq'
    echo -e "${YELLOW}⚠️  Using pre-generated hash for default password 'admin123'${NC}"
fi

# Create tenant first (admin tenant)
TENANT_ID=$(uuidgen)
TENANT_SQL="
INSERT INTO tenants (id, created_at, updated_at, version, name, slug, description, status, max_documents, max_storage_mb) 
VALUES (
    '$TENANT_ID',
    NOW(),
    NOW(),
    0,
    'System Administration',
    'admin',
    'System administration tenant for managing the RAG platform',
    'ACTIVE',
    10000,
    10000
) ON CONFLICT (slug) DO UPDATE SET 
    updated_at = NOW(),
    version = version + 1
RETURNING id;
"

# Create admin user
USER_ID=$(uuidgen)
USER_SQL="
INSERT INTO users (id, created_at, updated_at, version, email, first_name, last_name, password_hash, role, status, email_verified, tenant_id)
VALUES (
    '$USER_ID',
    NOW(),
    NOW(),
    0,
    '$ADMIN_EMAIL',
    '$ADMIN_FIRST_NAME',
    '$ADMIN_LAST_NAME',
    '$BCRYPT_PASSWORD',
    'ADMIN',
    'ACTIVE',
    true,
    '$TENANT_ID'
) ON CONFLICT (email) DO UPDATE SET
    password_hash = '$BCRYPT_PASSWORD',
    updated_at = NOW(),
    version = version + 1,
    status = 'ACTIVE'
RETURNING id;
"

echo -e "${YELLOW}Creating admin tenant...${NC}"
PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "$TENANT_SQL" >/dev/null

echo -e "${YELLOW}Creating admin user...${NC}"
PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "$USER_SQL" >/dev/null

echo ""
echo -e "${GREEN}✅ Admin user created successfully!${NC}"
echo ""
echo "📋 Admin Credentials:"
echo "   Email: $ADMIN_EMAIL"
echo "   Password: $ADMIN_PASSWORD"
echo "   Tenant: System Administration (admin)"
echo ""
echo "🌐 Login URL: http://localhost:8085/admin/api/auth/login"
echo ""
echo -e "${YELLOW}⚠️  Change the default password after first login!${NC}"
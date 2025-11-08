#!/usr/bin/env bash

###############################################################################
# Enable pgvector Extension on Cloud SQL
# 
# This script connects to Cloud SQL and enables the pgvector extension
# on all RAG databases using the root password from Secret Manager.
###############################################################################

set -euo pipefail

readonly PROJECT_ID="${GCP_PROJECT_ID:-byo-rag-dev}"
readonly INSTANCE_NAME="rag-postgres"
readonly DATABASES=("rag_auth" "rag_document" "rag_admin")

echo "Enabling pgvector extension on Cloud SQL databases..."
echo ""
echo "This will:"
echo "  1. Retrieve root password from Secret Manager"
echo "  2. Connect to Cloud SQL instance: $INSTANCE_NAME"
echo "  3. Enable vector extension on: ${DATABASES[*]}"
echo ""

# Get root password from Secret Manager
echo "Retrieving root password from Secret Manager..."
ROOT_PASSWORD=$(gcloud secrets versions access latest \
    --secret="cloudsql-root-password" \
    --project="$PROJECT_ID")

if [[ -z "$ROOT_PASSWORD" ]]; then
    echo "ERROR: Failed to retrieve root password from Secret Manager"
    exit 1
fi

echo "✓ Password retrieved"
echo ""

# Get Cloud SQL instance IP
INSTANCE_IP=$(gcloud sql instances describe "$INSTANCE_NAME" \
    --project="$PROJECT_ID" \
    --format="value(ipAddresses[0].ipAddress)")

echo "Instance IP: $INSTANCE_IP"
echo ""

# Enable pgvector on each database
for db in "${DATABASES[@]}"; do
    echo "Enabling pgvector on database: $db"
    
    PGPASSWORD="$ROOT_PASSWORD" psql \
        -h "$INSTANCE_IP" \
        -U postgres \
        -d "$db" \
        -c "CREATE EXTENSION IF NOT EXISTS vector;" \
        -c "\dx vector"
    
    echo "✓ pgvector enabled on $db"
    echo ""
done

echo "✓ pgvector extension enabled on all databases successfully!"

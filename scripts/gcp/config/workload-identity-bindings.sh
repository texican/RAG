#!/bin/bash
# Workload Identity Bindings
# Run this after GKE cluster is created

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
source "${SCRIPT_DIR}/project-config.env"

echo "Configuring Workload Identity bindings..."

# Bind Cloud SQL Proxy service account to Kubernetes service account
gcloud iam service-accounts add-iam-policy-binding \
  ${CLOUDSQL_SA_NAME}@${PROJECT_ID}.iam.gserviceaccount.com \
  --role roles/iam.workloadIdentityUser \
  --member "serviceAccount:${PROJECT_ID}.svc.id.goog[rag-system/cloudsql-proxy]"

echo "✓ Workload Identity bindings configured"

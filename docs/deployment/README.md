---
version: 1.0.0
last-updated: 2025-11-12
status: active
applies-to: 0.8.0-SNAPSHOT
category: deployment
---

## GCP Artifact Registry: RAG Service Images

All service images are published to Artifact Registry with vulnerability scanning enabled.

**Registry:**
`us-central1-docker.pkg.dev/byo-rag-dev/rag-system`

**Images:**
- rag-core-service
- rag-auth-service
- rag-admin-service
- rag-document-service
- rag-embedding-service

**Tags:**
- `0.8.0` (version)
- `latest`
- `<git-sha>`
- `0.8.0-<git-sha>`

**Pull Example:**
```sh
docker pull us-central1-docker.pkg.dev/byo-rag-dev/rag-system/rag-core-service:0.8.0
```

**Kubernetes Example:**
```yaml
image: us-central1-docker.pkg.dev/byo-rag-dev/rag-system/rag-core-service:0.8.0
```

**Console:**
https://console.cloud.google.com/artifacts/docker/byo-rag-dev/us-central1/rag-system
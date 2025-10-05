# Frontend Integration Guide - RAG SpecKit

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Service Endpoints](#service-endpoints)
3. [Authentication & Authorization](#authentication--authorization)
4. [API Integration Examples](#api-integration-examples)
5. [Data Models](#data-models)
6. [Error Handling](#error-handling)
7. [Best Practices](#best-practices)

---

## Architecture Overview

The RAG SpecKit consists of 5 microservices accessible from your frontend:

| Service | Port | Base URL (Docker) | Purpose |
|---------|------|-------------------|---------|
| **Auth Service** | 8081 | `http://localhost:8081` | User authentication, JWT token management, tenant/user management |
| **Document Service** | 8082 | `http://localhost:8082` | Document upload, processing, chunking, and metadata management |
| **Embedding Service** | 8083 | `http://localhost:8083` | Vector embeddings generation and similarity search |
| **Core Service** | 8084 | `http://localhost:8084` | RAG query processing, LLM integration, streaming responses |
| **Admin Service** | 8085 | `http://localhost:8085/admin/api` | System administration, tenant management, analytics |

### Infrastructure Services (Supporting)
- **PostgreSQL**: Port 5432 (Database)
- **Redis**: Port 6379 (Caching & vectors)
- **Kafka**: Port 9092 (Message broker)
- **Ollama**: Port 11434 (Local LLM)
- **Prometheus**: Port 9090 (Metrics)
- **Grafana**: Port 3000 (Dashboards)

---

## Service Endpoints

### 1. Auth Service (Port 8081)

#### Authentication Endpoints

**Login**
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePassword123"
}

Response 200:
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "user": {
    "id": "uuid",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "roles": ["USER"],
    "tenantId": "uuid"
  }
}
```

**Register New User**
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "newuser@example.com",
  "password": "SecurePassword123",
  "firstName": "Jane",
  "lastName": "Smith",
  "tenantSlug": "acme-corp"
}

Response 201:
{
  "id": "uuid",
  "email": "newuser@example.com",
  "firstName": "Jane",
  "lastName": "Smith",
  "emailVerified": false,
  "tenantId": "uuid"
}
```

**Refresh Token**
```http
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}

Response 200:
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "user": { ... }
}
```

**Validate Token**
```http
POST /api/v1/auth/validate
Content-Type: application/json

{
  "token": "eyJhbGciOiJIUzI1NiIs..."
}

Response 200:
{
  "valid": true
}
```

#### User Management Endpoints

**Get Current User**
```http
GET /api/v1/users/me
Authorization: Bearer <accessToken>

Response 200:
{
  "id": "uuid",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "roles": ["USER"],
  "tenantId": "uuid",
  "createdAt": "2025-01-01T00:00:00Z"
}
```

**Update Current User**
```http
PUT /api/v1/users/me
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "firstName": "John",
  "lastName": "Doe Updated"
}

Response 200: UserResponse
```

**List Users (Paginated)**
```http
GET /api/v1/users?page=0&size=20&sort=email,asc
Authorization: Bearer <accessToken>

Response 200:
{
  "content": [
    {
      "id": "uuid",
      "email": "user@example.com",
      "firstName": "John",
      "lastName": "Doe"
    }
  ],
  "totalElements": 50,
  "totalPages": 3,
  "number": 0,
  "size": 20
}
```

#### Tenant Management Endpoints

**Register New Tenant (Public)**
```http
POST /api/v1/tenants/register
Content-Type: application/json

{
  "name": "ACME Corporation",
  "slug": "acme-corp",
  "description": "ACME Corp tenant"
}

Response 201:
{
  "id": "uuid",
  "name": "ACME Corporation",
  "slug": "acme-corp",
  "active": true,
  "createdAt": "2025-01-01T00:00:00Z"
}
```

**Get Tenant by Slug (Public)**
```http
GET /api/v1/tenants/slug/acme-corp

Response 200: TenantResponse
```

---

### 2. Document Service (Port 8082)

**Upload Document**
```http
POST /api/v1/documents/upload
Authorization: Bearer <accessToken>
X-Tenant-ID: <tenantId>
Content-Type: multipart/form-data

FormData:
- file: <binary file>
- metadata: { "category": "technical", "author": "John Doe" } (optional)

Response 201:
{
  "id": "uuid",
  "fileName": "document.pdf",
  "fileSize": 1024000,
  "mimeType": "application/pdf",
  "status": "PROCESSING",
  "tenantId": "uuid",
  "uploadedAt": "2025-01-01T00:00:00Z",
  "metadata": {
    "category": "technical",
    "author": "John Doe"
  }
}
```

**List Documents**
```http
GET /api/v1/documents?page=0&size=20&sort=uploadedAt,desc
Authorization: Bearer <accessToken>
X-Tenant-ID: <tenantId>

Response 200:
{
  "content": [
    {
      "id": "uuid",
      "fileName": "document.pdf",
      "status": "COMPLETED",
      "chunkCount": 150
    }
  ],
  "totalElements": 100,
  "totalPages": 5,
  "number": 0,
  "size": 20
}
```

**Get Document Details**
```http
GET /api/v1/documents/{documentId}
Authorization: Bearer <accessToken>
X-Tenant-ID: <tenantId>

Response 200: DocumentResponse (full details)
```

**Update Document Metadata**
```http
PUT /api/v1/documents/{documentId}
Authorization: Bearer <accessToken>
X-Tenant-ID: <tenantId>
Content-Type: application/json

{
  "metadata": {
    "category": "updated-category",
    "tags": ["important", "reviewed"]
  }
}

Response 200: DocumentResponse
```

**Delete Document**
```http
DELETE /api/v1/documents/{documentId}
Authorization: Bearer <accessToken>
X-Tenant-ID: <tenantId>

Response 204: No Content
```

**Get Document Statistics**
```http
GET /api/v1/documents/stats
Authorization: Bearer <accessToken>
X-Tenant-ID: <tenantId>

Response 200:
{
  "totalDocuments": 250,
  "storageUsageBytes": 524288000,
  "processingDocuments": 5,
  "completedDocuments": 240,
  "failedDocuments": 5
}
```

---

### 3. Embedding Service (Port 8083)

**Generate Embeddings**
```http
POST /api/v1/embeddings/generate
Authorization: Bearer <accessToken>
X-Tenant-ID: <tenantId>
Content-Type: application/json

{
  "text": "What is machine learning?",
  "documentId": "uuid",
  "chunkId": "uuid",
  "modelName": "openai-text-embedding-3-small"
}

Response 200:
{
  "embedding": [0.023, -0.145, 0.678, ...],  // 1536 dimensions
  "dimension": 1536,
  "modelName": "openai-text-embedding-3-small",
  "documentId": "uuid",
  "chunkId": "uuid"
}
```

**Semantic Search**
```http
POST /api/v1/embeddings/search
Authorization: Bearer <accessToken>
X-Tenant-ID: <tenantId>
Content-Type: application/json

{
  "query": "Explain neural networks",
  "topK": 10,
  "threshold": 0.7,
  "modelName": "openai-text-embedding-3-small"
}

Response 200:
{
  "results": [
    {
      "documentId": "uuid",
      "chunkId": "uuid",
      "text": "Neural networks are...",
      "score": 0.95,
      "metadata": { "fileName": "ml-basics.pdf" }
    }
  ],
  "totalResults": 10,
  "queryTime": 120
}
```

**Batch Embedding Generation**
```http
POST /api/v1/embeddings/batch
Authorization: Bearer <accessToken>
X-Tenant-ID: <tenantId>
Content-Type: application/json

[
  {
    "text": "First chunk of text",
    "documentId": "uuid1",
    "chunkId": "chunk1"
  },
  {
    "text": "Second chunk of text",
    "documentId": "uuid2",
    "chunkId": "chunk2"
  }
]

Response 200: [EmbeddingResponse, ...]
```

**Hybrid Search (Semantic + Keyword)**
```http
POST /api/v1/embeddings/search/hybrid?keywords=machine,learning
Authorization: Bearer <accessToken>
X-Tenant-ID: <tenantId>
Content-Type: application/json

{
  "query": "Explain deep learning",
  "topK": 10,
  "threshold": 0.7
}

Response 200: SearchResponse (hybrid results)
```

**Find Similar Documents**
```http
GET /api/v1/embeddings/similar/{documentId}?topK=5
Authorization: Bearer <accessToken>
X-Tenant-ID: <tenantId>

Response 200: SearchResponse (similar documents)
```

**Get Available Models**
```http
GET /api/v1/embeddings/models

Response 200:
{
  "models": [
    {
      "name": "openai-text-embedding-3-small",
      "dimensions": 1536,
      "default": true
    },
    {
      "name": "sentence-transformers-all-minilm-l6-v2",
      "dimensions": 384,
      "default": false
    }
  ]
}
```

---

### 4. Core Service (Port 8084)

**Query RAG (Synchronous)**
```http
POST /api/v1/rag/query
Authorization: Bearer <accessToken>
X-Tenant-ID: <tenantId>
Content-Type: application/json

{
  "query": "What are the key features of transformer models?",
  "conversationId": "uuid",
  "maxTokens": 2000,
  "temperature": 0.7,
  "topK": 10,
  "includeContext": true,
  "provider": "openai"
}

Response 200:
{
  "response": "Transformer models have several key features...",
  "conversationId": "uuid",
  "sources": [
    {
      "documentId": "uuid",
      "fileName": "transformers.pdf",
      "relevanceScore": 0.95,
      "excerpt": "Transformers use self-attention..."
    }
  ],
  "metadata": {
    "tokensUsed": 1500,
    "provider": "openai",
    "model": "gpt-4o-mini",
    "processingTime": 2500
  }
}
```

**Query RAG (Streaming)**
```http
POST /api/v1/rag/query/stream
Authorization: Bearer <accessToken>
X-Tenant-ID: <tenantId>
Content-Type: application/json
Accept: text/event-stream

{
  "query": "Explain gradient descent",
  "conversationId": "uuid",
  "temperature": 0.1
}

Response 200 (Server-Sent Events):
data: Gradient descent is an optimization algorithm
data: that finds the minimum of a function by
data: iteratively moving in the direction of steepest descent

Event stream format: text/event-stream
```

**Query RAG (Async)**
```http
POST /api/v1/rag/query/async
Authorization: Bearer <accessToken>
X-Tenant-ID: <tenantId>
Content-Type: application/json

{
  "query": "What is backpropagation?",
  "conversationId": "uuid"
}

Response 202: Accepted
{
  "taskId": "uuid",
  "status": "PROCESSING"
}

// Poll for results or use WebSocket
```

**Analyze Query**
```http
POST /api/v1/rag/query/analyze
Authorization: Bearer <accessToken>
X-Tenant-ID: <tenantId>
Content-Type: application/json

{
  "query": "what r neural nets"
}

Response 200:
{
  "originalQuery": "what r neural nets",
  "optimizedQuery": "What are neural networks?",
  "suggestions": [
    "How do neural networks work?",
    "Explain neural network architecture"
  ],
  "complexity": "SIMPLE",
  "estimatedTokens": 150
}
```

**Get Conversation History**
```http
GET /api/v1/rag/conversations/{conversationId}
Authorization: Bearer <accessToken>
X-Tenant-ID: <tenantId>

Response 200:
{
  "id": "uuid",
  "messages": [
    {
      "role": "user",
      "content": "What is machine learning?",
      "timestamp": "2025-01-01T10:00:00Z"
    },
    {
      "role": "assistant",
      "content": "Machine learning is...",
      "timestamp": "2025-01-01T10:00:05Z"
    }
  ],
  "createdAt": "2025-01-01T10:00:00Z",
  "lastMessageAt": "2025-01-01T10:15:00Z"
}
```

**Get Provider Status**
```http
GET /api/v1/rag/providers/status
Authorization: Bearer <accessToken>
X-Tenant-ID: <tenantId>

Response 200:
{
  "openai": {
    "status": "HEALTHY",
    "model": "gpt-4o-mini",
    "latency": 1200
  },
  "ollama": {
    "status": "HEALTHY",
    "model": "llama3.1:8b",
    "latency": 3500
  },
  "anthropic": {
    "status": "OFFLINE",
    "error": "API key not configured"
  }
}
```

**Get RAG Statistics**
```http
GET /api/v1/rag/stats
Authorization: Bearer <accessToken>
X-Tenant-ID: <tenantId>

Response 200:
{
  "totalQueries": 1500,
  "avgResponseTime": 2300,
  "cacheHitRate": 0.45,
  "activeConversations": 25,
  "providerUsage": {
    "openai": 1200,
    "ollama": 300
  }
}
```

---

### 5. Admin Service (Port 8085)

**Admin Login**
```http
POST /admin/api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "AdminPassword123"
}

Response 200:
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "username": "admin",
  "roles": ["ADMIN"],
  "expiresIn": 86400
}
```

**Create Tenant (Admin)**
```http
POST /admin/api/tenants
Authorization: Bearer <adminToken>
Content-Type: application/json

{
  "name": "Enterprise Corp",
  "slug": "enterprise-corp",
  "description": "Enterprise customer",
  "maxUsers": 100,
  "maxStorage": 10737418240
}

Response 201:
{
  "id": "uuid",
  "name": "Enterprise Corp",
  "slug": "enterprise-corp",
  "active": true,
  "maxUsers": 100,
  "maxStorage": 10737418240,
  "createdAt": "2025-01-01T00:00:00Z"
}
```

**List Tenants (Admin)**
```http
GET /admin/api/tenants?page=0&size=20&sort=createdAt,desc
Authorization: Bearer <adminToken>

Response 200:
{
  "tenants": [
    {
      "id": "uuid",
      "name": "ACME Corp",
      "slug": "acme-corp",
      "active": true,
      "userCount": 50,
      "documentCount": 250
    }
  ],
  "totalElements": 100,
  "totalPages": 5,
  "currentPage": 0
}
```

**Suspend Tenant**
```http
POST /admin/api/tenants/{tenantId}/suspend
Authorization: Bearer <adminToken>
Content-Type: application/json

{
  "reason": "Payment overdue",
  "notifyUsers": true
}

Response 200: TenantResponse (with active: false)
```

**Reactivate Tenant**
```http
POST /admin/api/tenants/{tenantId}/reactivate
Authorization: Bearer <adminToken>

Response 200: TenantResponse (with active: true)
```

---

## Authentication & Authorization

### Authentication Flow

1. **User Registration & Login**
```javascript
// 1. Register new user
const registerResponse = await fetch('http://localhost:8081/api/v1/auth/register', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    email: 'user@example.com',
    password: 'SecurePassword123',
    firstName: 'John',
    lastName: 'Doe',
    tenantSlug: 'acme-corp'
  })
});

// 2. Login to get tokens
const loginResponse = await fetch('http://localhost:8081/api/v1/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    email: 'user@example.com',
    password: 'SecurePassword123'
  })
});

const { accessToken, refreshToken, user } = await loginResponse.json();

// Store tokens securely (use httpOnly cookies in production)
localStorage.setItem('accessToken', accessToken);
localStorage.setItem('refreshToken', refreshToken);
localStorage.setItem('user', JSON.stringify(user));
```

2. **Making Authenticated Requests**
```javascript
const accessToken = localStorage.getItem('accessToken');
const tenantId = JSON.parse(localStorage.getItem('user')).tenantId;

const response = await fetch('http://localhost:8082/api/v1/documents', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${accessToken}`,
    'X-Tenant-ID': tenantId,
    'Content-Type': 'application/json'
  }
});
```

3. **Token Refresh**
```javascript
async function refreshAccessToken() {
  const refreshToken = localStorage.getItem('refreshToken');

  const response = await fetch('http://localhost:8081/api/v1/auth/refresh', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken })
  });

  if (response.ok) {
    const { accessToken, refreshToken: newRefreshToken } = await response.json();
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', newRefreshToken);
    return accessToken;
  } else {
    // Redirect to login
    window.location.href = '/login';
  }
}
```

### Required Headers

**Standard API Request:**
```http
Authorization: Bearer <accessToken>
X-Tenant-ID: <tenantId>
Content-Type: application/json
```

**File Upload Request:**
```http
Authorization: Bearer <accessToken>
X-Tenant-ID: <tenantId>
Content-Type: multipart/form-data
```

**Streaming Request:**
```http
Authorization: Bearer <accessToken>
X-Tenant-ID: <tenantId>
Content-Type: application/json
Accept: text/event-stream
```

---

## API Integration Examples

### React/TypeScript Example

```typescript
// src/services/api.ts
import axios, { AxiosInstance } from 'axios';

const BASE_URLS = {
  auth: 'http://localhost:8081/api/v1',
  document: 'http://localhost:8082/api/v1',
  embedding: 'http://localhost:8083/api/v1',
  core: 'http://localhost:8084/api/v1',
  admin: 'http://localhost:8085/admin/api'
};

class ApiClient {
  private authClient: AxiosInstance;
  private documentClient: AxiosInstance;
  private embeddingClient: AxiosInstance;
  private coreClient: AxiosInstance;
  private adminClient: AxiosInstance;

  constructor() {
    this.authClient = this.createClient(BASE_URLS.auth);
    this.documentClient = this.createClient(BASE_URLS.document);
    this.embeddingClient = this.createClient(BASE_URLS.embedding);
    this.coreClient = this.createClient(BASE_URLS.core);
    this.adminClient = this.createClient(BASE_URLS.admin);

    this.setupInterceptors();
  }

  private createClient(baseURL: string): AxiosInstance {
    return axios.create({
      baseURL,
      headers: {
        'Content-Type': 'application/json'
      }
    });
  }

  private setupInterceptors() {
    const clients = [
      this.documentClient,
      this.embeddingClient,
      this.coreClient,
      this.adminClient
    ];

    clients.forEach(client => {
      // Request interceptor - add auth headers
      client.interceptors.request.use(
        (config) => {
          const token = localStorage.getItem('accessToken');
          const user = JSON.parse(localStorage.getItem('user') || '{}');

          if (token) {
            config.headers.Authorization = `Bearer ${token}`;
          }

          if (user.tenantId) {
            config.headers['X-Tenant-ID'] = user.tenantId;
          }

          return config;
        },
        (error) => Promise.reject(error)
      );

      // Response interceptor - handle token refresh
      client.interceptors.response.use(
        (response) => response,
        async (error) => {
          const originalRequest = error.config;

          if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;

            try {
              const refreshToken = localStorage.getItem('refreshToken');
              const response = await this.authClient.post('/auth/refresh', {
                refreshToken
              });

              const { accessToken, refreshToken: newRefreshToken } = response.data;
              localStorage.setItem('accessToken', accessToken);
              localStorage.setItem('refreshToken', newRefreshToken);

              originalRequest.headers.Authorization = `Bearer ${accessToken}`;
              return axios(originalRequest);
            } catch (refreshError) {
              localStorage.clear();
              window.location.href = '/login';
              return Promise.reject(refreshError);
            }
          }

          return Promise.reject(error);
        }
      );
    });
  }

  // Auth Service Methods
  async login(email: string, password: string) {
    const response = await this.authClient.post('/auth/login', { email, password });
    const { accessToken, refreshToken, user } = response.data;

    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    localStorage.setItem('user', JSON.stringify(user));

    return response.data;
  }

  async register(data: RegisterRequest) {
    return this.authClient.post('/auth/register', data);
  }

  async getCurrentUser() {
    return this.authClient.get('/users/me');
  }

  // Document Service Methods
  async uploadDocument(file: File, metadata?: object) {
    const formData = new FormData();
    formData.append('file', file);
    if (metadata) {
      formData.append('metadata', JSON.stringify(metadata));
    }

    return this.documentClient.post('/documents/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
  }

  async listDocuments(page = 0, size = 20) {
    return this.documentClient.get('/documents', {
      params: { page, size, sort: 'uploadedAt,desc' }
    });
  }

  async deleteDocument(documentId: string) {
    return this.documentClient.delete(`/documents/${documentId}`);
  }

  // Embedding Service Methods
  async searchSemantic(query: string, topK = 10, threshold = 0.7) {
    return this.embeddingClient.post('/embeddings/search', {
      query,
      topK,
      threshold
    });
  }

  // Core Service Methods
  async queryRAG(query: string, conversationId?: string) {
    return this.coreClient.post('/rag/query', {
      query,
      conversationId,
      maxTokens: 2000,
      temperature: 0.7,
      includeContext: true
    });
  }

  async queryRAGStream(query: string, onChunk: (chunk: string) => void) {
    const response = await fetch(`${BASE_URLS.core}/rag/query/stream`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('accessToken')}`,
        'X-Tenant-ID': JSON.parse(localStorage.getItem('user') || '{}').tenantId,
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream'
      },
      body: JSON.stringify({ query })
    });

    const reader = response.body?.getReader();
    const decoder = new TextDecoder();

    while (true) {
      const { done, value } = await reader!.read();
      if (done) break;

      const chunk = decoder.decode(value);
      const lines = chunk.split('\n');

      for (const line of lines) {
        if (line.startsWith('data: ')) {
          onChunk(line.substring(6));
        }
      }
    }
  }

  async getConversationHistory(conversationId: string) {
    return this.coreClient.get(`/rag/conversations/${conversationId}`);
  }
}

export const api = new ApiClient();

// Type definitions
interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  tenantSlug: string;
}
```

### React Component Example

```typescript
// src/components/DocumentUpload.tsx
import React, { useState } from 'react';
import { api } from '../services/api';

const DocumentUpload: React.FC = () => {
  const [file, setFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setFile(e.target.files[0]);
    }
  };

  const handleUpload = async () => {
    if (!file) return;

    setUploading(true);
    setError(null);

    try {
      const response = await api.uploadDocument(file, {
        category: 'technical',
        uploadedBy: 'user'
      });

      console.log('Document uploaded:', response.data);
      alert('Document uploaded successfully!');
      setFile(null);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Upload failed');
    } finally {
      setUploading(false);
    }
  };

  return (
    <div>
      <h2>Upload Document</h2>
      <input type="file" onChange={handleFileChange} accept=".pdf,.docx,.txt,.md" />
      <button onClick={handleUpload} disabled={!file || uploading}>
        {uploading ? 'Uploading...' : 'Upload'}
      </button>
      {error && <p style={{ color: 'red' }}>{error}</p>}
    </div>
  );
};

export default DocumentUpload;
```

```typescript
// src/components/RAGChat.tsx
import React, { useState } from 'react';
import { api } from '../services/api';

const RAGChat: React.FC = () => {
  const [query, setQuery] = useState('');
  const [response, setResponse] = useState('');
  const [loading, setLoading] = useState(false);
  const [conversationId, setConversationId] = useState<string | undefined>();

  const handleStreamingQuery = async () => {
    if (!query.trim()) return;

    setLoading(true);
    setResponse('');

    try {
      await api.queryRAGStream(query, (chunk) => {
        setResponse(prev => prev + chunk);
      });
    } catch (err) {
      console.error('Query failed:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleQuery = async () => {
    if (!query.trim()) return;

    setLoading(true);
    setResponse('');

    try {
      const result = await api.queryRAG(query, conversationId);
      setResponse(result.data.response);
      setConversationId(result.data.conversationId);
    } catch (err) {
      console.error('Query failed:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h2>RAG Query</h2>
      <textarea
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        placeholder="Ask a question..."
        rows={4}
        cols={50}
      />
      <br />
      <button onClick={handleQuery} disabled={loading}>
        {loading ? 'Processing...' : 'Query'}
      </button>
      <button onClick={handleStreamingQuery} disabled={loading}>
        Stream Response
      </button>

      {response && (
        <div style={{ marginTop: '20px', padding: '10px', border: '1px solid #ccc' }}>
          <h3>Response:</h3>
          <p>{response}</p>
        </div>
      )}
    </div>
  );
};

export default RAGChat;
```

---

## Data Models

### Common Types

```typescript
// User models
interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
  tenantId: string;
  emailVerified: boolean;
  createdAt: string;
  updatedAt: string;
}

interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  user: User;
}

// Tenant models
interface Tenant {
  id: string;
  name: string;
  slug: string;
  description?: string;
  active: boolean;
  maxUsers?: number;
  maxStorage?: number;
  createdAt: string;
  updatedAt: string;
}

// Document models
interface Document {
  id: string;
  fileName: string;
  fileSize: number;
  mimeType: string;
  status: 'UPLOADING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  tenantId: string;
  uploadedAt: string;
  processedAt?: string;
  chunkCount?: number;
  metadata?: Record<string, any>;
  errorMessage?: string;
}

// Embedding models
interface EmbeddingRequest {
  text: string;
  documentId: string;
  chunkId: string;
  modelName?: string;
}

interface EmbeddingResponse {
  embedding: number[];
  dimension: number;
  modelName: string;
  documentId: string;
  chunkId: string;
}

interface SearchRequest {
  query: string;
  topK?: number;
  threshold?: number;
  modelName?: string;
  filters?: Record<string, any>;
}

interface SearchResult {
  documentId: string;
  chunkId: string;
  text: string;
  score: number;
  metadata?: Record<string, any>;
}

interface SearchResponse {
  results: SearchResult[];
  totalResults: number;
  queryTime: number;
}

// RAG models
interface RAGQueryRequest {
  query: string;
  conversationId?: string;
  maxTokens?: number;
  temperature?: number;
  topK?: number;
  includeContext?: boolean;
  provider?: 'openai' | 'anthropic' | 'ollama';
}

interface RAGSource {
  documentId: string;
  fileName: string;
  relevanceScore: number;
  excerpt: string;
  chunkId?: string;
}

interface RAGQueryResponse {
  response: string;
  conversationId: string;
  sources: RAGSource[];
  metadata: {
    tokensUsed: number;
    provider: string;
    model: string;
    processingTime: number;
  };
}

interface Conversation {
  id: string;
  messages: ConversationMessage[];
  createdAt: string;
  lastMessageAt: string;
}

interface ConversationMessage {
  role: 'user' | 'assistant';
  content: string;
  timestamp: string;
}
```

---

## Error Handling

### Standard Error Response Format

```json
{
  "timestamp": "2025-01-01T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid email format",
  "path": "/api/v1/auth/register"
}
```

### HTTP Status Codes

| Code | Meaning | Common Causes |
|------|---------|---------------|
| 200 | OK | Successful GET, PUT requests |
| 201 | Created | Successful POST creating resource |
| 204 | No Content | Successful DELETE |
| 400 | Bad Request | Invalid request body, missing required fields |
| 401 | Unauthorized | Missing/invalid token, expired token |
| 403 | Forbidden | Insufficient permissions, wrong tenant |
| 404 | Not Found | Resource doesn't exist |
| 409 | Conflict | Duplicate resource (e.g., email already exists) |
| 413 | Payload Too Large | File upload exceeds limit (50MB) |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Server-side error |
| 503 | Service Unavailable | Service down or overloaded |

### Error Handling Example

```typescript
async function handleApiCall<T>(apiCall: () => Promise<T>): Promise<T | null> {
  try {
    return await apiCall();
  } catch (error: any) {
    if (error.response) {
      // Server responded with error
      const { status, data } = error.response;

      switch (status) {
        case 400:
          console.error('Validation error:', data.message);
          alert(`Invalid input: ${data.message}`);
          break;
        case 401:
          console.error('Authentication failed');
          // Redirect to login
          window.location.href = '/login';
          break;
        case 403:
          console.error('Access denied:', data.message);
          alert('You do not have permission to perform this action');
          break;
        case 404:
          console.error('Resource not found');
          alert('The requested resource was not found');
          break;
        case 413:
          console.error('File too large');
          alert('File size exceeds 50MB limit');
          break;
        case 500:
          console.error('Server error:', data.message);
          alert('An internal server error occurred. Please try again later.');
          break;
        default:
          console.error('API error:', data);
          alert(`Error: ${data.message || 'Unknown error'}`);
      }
    } else if (error.request) {
      // Request made but no response
      console.error('Network error:', error.message);
      alert('Network error. Please check your connection.');
    } else {
      // Other errors
      console.error('Error:', error.message);
      alert('An unexpected error occurred');
    }

    return null;
  }
}

// Usage
const result = await handleApiCall(() => api.uploadDocument(file));
if (result) {
  console.log('Upload successful:', result.data);
}
```

---

## Best Practices

### 1. Token Management

**Store tokens securely:**
```typescript
// ❌ Bad - XSS vulnerable
localStorage.setItem('accessToken', token);

// ✅ Better - Use httpOnly cookies (backend required)
// Set cookie via Set-Cookie header from backend

// ✅ Acceptable for development
const secureStorage = {
  setToken: (token: string) => {
    sessionStorage.setItem('accessToken', token); // Cleared on tab close
  },
  getToken: () => sessionStorage.getItem('accessToken'),
  clearToken: () => sessionStorage.clear()
};
```

**Implement automatic token refresh:**
```typescript
let isRefreshing = false;
let refreshSubscribers: ((token: string) => void)[] = [];

function subscribeTokenRefresh(callback: (token: string) => void) {
  refreshSubscribers.push(callback);
}

function onTokenRefreshed(token: string) {
  refreshSubscribers.forEach(callback => callback(token));
  refreshSubscribers = [];
}

axios.interceptors.response.use(
  response => response,
  async error => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // Queue request until token refreshed
        return new Promise(resolve => {
          subscribeTokenRefresh((token: string) => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            resolve(axios(originalRequest));
          });
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const newToken = await refreshAccessToken();
        isRefreshing = false;
        onTokenRefreshed(newToken);
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return axios(originalRequest);
      } catch (refreshError) {
        isRefreshing = false;
        // Redirect to login
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);
```

### 2. File Upload Best Practices

```typescript
async function uploadDocumentWithProgress(
  file: File,
  onProgress: (progress: number) => void
) {
  const formData = new FormData();
  formData.append('file', file);

  // Validate file size (client-side)
  const MAX_SIZE = 50 * 1024 * 1024; // 50MB
  if (file.size > MAX_SIZE) {
    throw new Error('File exceeds 50MB limit');
  }

  // Validate file type
  const ALLOWED_TYPES = ['application/pdf', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'text/plain', 'text/markdown'];
  if (!ALLOWED_TYPES.includes(file.type)) {
    throw new Error('Invalid file type');
  }

  return axios.post('/documents/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: (progressEvent) => {
      const percentCompleted = Math.round(
        (progressEvent.loaded * 100) / (progressEvent.total || file.size)
      );
      onProgress(percentCompleted);
    }
  });
}
```

### 3. Streaming Response Handling

```typescript
class StreamingClient {
  async streamQuery(
    query: string,
    onChunk: (chunk: string) => void,
    onComplete: () => void,
    onError: (error: Error) => void
  ) {
    try {
      const response = await fetch('http://localhost:8084/api/v1/rag/query/stream', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('accessToken')}`,
          'X-Tenant-ID': JSON.parse(localStorage.getItem('user') || '{}').tenantId,
          'Content-Type': 'application/json',
          'Accept': 'text/event-stream'
        },
        body: JSON.stringify({ query })
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const reader = response.body?.getReader();
      const decoder = new TextDecoder();

      if (!reader) {
        throw new Error('Response body is not readable');
      }

      while (true) {
        const { done, value } = await reader.read();

        if (done) {
          onComplete();
          break;
        }

        const chunk = decoder.decode(value, { stream: true });
        const lines = chunk.split('\n');

        for (const line of lines) {
          if (line.startsWith('data: ')) {
            const data = line.substring(6).trim();
            if (data) {
              onChunk(data);
            }
          }
        }
      }
    } catch (error) {
      onError(error as Error);
    }
  }
}
```

### 4. Pagination Helper

```typescript
interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

class PaginationHelper<T> {
  private currentPage = 0;
  private pageSize = 20;

  async fetchPage(
    fetchFn: (page: number, size: number) => Promise<PaginatedResponse<T>>
  ): Promise<PaginatedResponse<T>> {
    return fetchFn(this.currentPage, this.pageSize);
  }

  nextPage() {
    this.currentPage++;
  }

  previousPage() {
    if (this.currentPage > 0) {
      this.currentPage--;
    }
  }

  reset() {
    this.currentPage = 0;
  }
}

// Usage
const pagination = new PaginationHelper<Document>();
const documents = await pagination.fetchPage((page, size) =>
  api.listDocuments(page, size)
);
```

### 5. Caching Strategy

```typescript
class CachedApiClient {
  private cache = new Map<string, { data: any; timestamp: number }>();
  private cacheDuration = 5 * 60 * 1000; // 5 minutes

  async getCached<T>(
    key: string,
    fetchFn: () => Promise<T>
  ): Promise<T> {
    const cached = this.cache.get(key);
    const now = Date.now();

    if (cached && (now - cached.timestamp) < this.cacheDuration) {
      console.log('Cache hit:', key);
      return cached.data as T;
    }

    console.log('Cache miss:', key);
    const data = await fetchFn();
    this.cache.set(key, { data, timestamp: now });
    return data;
  }

  invalidate(key: string) {
    this.cache.delete(key);
  }

  clear() {
    this.cache.clear();
  }
}

// Usage
const cachedApi = new CachedApiClient();
const documents = await cachedApi.getCached(
  'documents-page-0',
  () => api.listDocuments(0, 20)
);
```

### 6. Environment Configuration

```typescript
// src/config/environment.ts
const environments = {
  development: {
    auth: 'http://localhost:8081/api/v1',
    document: 'http://localhost:8082/api/v1',
    embedding: 'http://localhost:8083/api/v1',
    core: 'http://localhost:8084/api/v1',
    admin: 'http://localhost:8085/admin/api'
  },
  production: {
    auth: 'https://api.yourapp.com/auth/v1',
    document: 'https://api.yourapp.com/document/v1',
    embedding: 'https://api.yourapp.com/embedding/v1',
    core: 'https://api.yourapp.com/core/v1',
    admin: 'https://api.yourapp.com/admin/api'
  }
};

export const config = environments[process.env.NODE_ENV || 'development'];
```

### 7. Request Timeout & Retry

```typescript
import axios, { AxiosRequestConfig } from 'axios';

const createClientWithRetry = (baseURL: string) => {
  const client = axios.create({
    baseURL,
    timeout: 30000 // 30 seconds
  });

  client.interceptors.response.use(
    response => response,
    async error => {
      const config = error.config as AxiosRequestConfig & { _retryCount?: number };

      if (!config || !config._retryCount) {
        config._retryCount = 0;
      }

      const shouldRetry = (
        config._retryCount < 3 &&
        error.code === 'ECONNABORTED' || // Timeout
        error.response?.status >= 500     // Server error
      );

      if (shouldRetry) {
        config._retryCount++;
        const delay = Math.pow(2, config._retryCount) * 1000; // Exponential backoff
        await new Promise(resolve => setTimeout(resolve, delay));
        return client(config);
      }

      return Promise.reject(error);
    }
  );

  return client;
};
```

---

## Testing API Integration

### Example Test with Jest

```typescript
// __tests__/api.test.ts
import { api } from '../services/api';
import axios from 'axios';

jest.mock('axios');
const mockedAxios = axios as jest.Mocked<typeof axios>;

describe('API Client', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('should login and store tokens', async () => {
    const mockResponse = {
      data: {
        accessToken: 'mock-access-token',
        refreshToken: 'mock-refresh-token',
        user: { id: '123', email: 'test@example.com' }
      }
    };

    mockedAxios.post.mockResolvedValue(mockResponse);

    const result = await api.login('test@example.com', 'password');

    expect(localStorage.getItem('accessToken')).toBe('mock-access-token');
    expect(result.user.email).toBe('test@example.com');
  });

  it('should upload document with correct headers', async () => {
    localStorage.setItem('accessToken', 'test-token');
    localStorage.setItem('user', JSON.stringify({ tenantId: 'tenant-123' }));

    const file = new File(['content'], 'test.pdf', { type: 'application/pdf' });
    const mockResponse = { data: { id: 'doc-123' } };

    mockedAxios.post.mockResolvedValue(mockResponse);

    await api.uploadDocument(file);

    expect(mockedAxios.post).toHaveBeenCalledWith(
      '/documents/upload',
      expect.any(FormData),
      expect.objectContaining({
        headers: expect.objectContaining({
          'Authorization': 'Bearer test-token',
          'X-Tenant-ID': 'tenant-123'
        })
      })
    );
  });
});
```

---

## Troubleshooting

### Common Issues

**1. CORS Errors**
```
Access to fetch at 'http://localhost:8081' from origin 'http://localhost:3000'
has been blocked by CORS policy
```

**Solution:** Add CORS configuration to backend services or use a proxy in development:

```javascript
// package.json (React/Next.js)
{
  "proxy": "http://localhost:8081"
}

// Or use vite.config.ts (Vite)
export default {
  server: {
    proxy: {
      '/api': 'http://localhost:8081'
    }
  }
}
```

**2. Token Expiration**
```
401 Unauthorized - Token has expired
```

**Solution:** Implement automatic token refresh (see [Token Management](#1-token-management))

**3. Missing Tenant Header**
```
400 Bad Request - X-Tenant-ID header is required
```

**Solution:** Ensure all authenticated requests include tenant ID:

```typescript
headers: {
  'X-Tenant-ID': user.tenantId
}
```

**4. File Upload Size Limit**
```
413 Payload Too Large
```

**Solution:** Check file size before upload (max 50MB) or increase backend limit

**5. Streaming Not Working**
```
Response is not a stream
```

**Solution:** Use `fetch` API instead of `axios` for streaming:

```typescript
// ✅ Correct
const response = await fetch(url, {
  headers: { 'Accept': 'text/event-stream' }
});

// ❌ Incorrect (axios doesn't support SSE well)
const response = await axios.get(url);
```

---

## OpenAPI/Swagger Documentation

All services expose Swagger UI for interactive API exploration:

- **Auth Service**: http://localhost:8081/swagger-ui.html
- **Document Service**: http://localhost:8082/swagger-ui.html
- **Embedding Service**: http://localhost:8083/swagger-ui.html
- **Core Service**: http://localhost:8084/swagger-ui.html
- **Admin Service**: http://localhost:8085/admin/api/swagger-ui.html

OpenAPI JSON specs available at `/v3/api-docs` for each service.

---

## Quick Start Checklist

- [ ] Start all services: `docker-compose up -d`
- [ ] Verify health: `curl http://localhost:8081/actuator/health`
- [ ] Register tenant: `POST /api/v1/tenants/register`
- [ ] Register user: `POST /api/v1/auth/register`
- [ ] Login: `POST /api/v1/auth/login`
- [ ] Store access token and tenant ID
- [ ] Upload document: `POST /api/v1/documents/upload`
- [ ] Query RAG: `POST /api/v1/rag/query`

---

**For questions or issues, refer to:**
- [CONTRIBUTING.md](./CONTRIBUTING.md) for development guidelines
- [README.md](./README.md) for system architecture
- Swagger UI endpoints for detailed API specs

# CloudSync API Documentation

## API Overview

The CloudSync API provides programmatic access to the CloudSync Enterprise Platform. This RESTful API allows developers to integrate file storage, sharing, and collaboration features into their applications.

**Base URL**: `https://api.cloudsync.com/v1`
**API Version**: 1.5.0
**Protocol**: HTTPS only
**Authentication**: OAuth 2.0, API Keys

## Authentication

### OAuth 2.0 Flow

CloudSync supports the Authorization Code flow for user-delegated access.

**1. Authorization Request**
```http
GET https://auth.cloudsync.com/oauth/authorize
  ?client_id=YOUR_CLIENT_ID
  &redirect_uri=YOUR_REDIRECT_URI
  &response_type=code
  &scope=files.read files.write
  &state=RANDOM_STRING
```

**2. Token Exchange**
```http
POST https://auth.cloudsync.com/oauth/token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code
&code=AUTHORIZATION_CODE
&client_id=YOUR_CLIENT_ID
&client_secret=YOUR_CLIENT_SECRET
&redirect_uri=YOUR_REDIRECT_URI
```

**Response**:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIs...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "refresh_token": "def50200a1b2c3d4e5f6..."
}
```

### API Key Authentication

For server-to-server integrations, use API keys.

```http
GET /files
Authorization: Bearer YOUR_API_KEY
```

### Token Refresh

```http
POST https://auth.cloudsync.com/oauth/token
Content-Type: application/x-www-form-urlencoded

grant_type=refresh_token
&refresh_token=YOUR_REFRESH_TOKEN
&client_id=YOUR_CLIENT_ID
&client_secret=YOUR_CLIENT_SECRET
```

## Rate Limiting

API requests are rate limited to ensure fair usage.

**Limits**:
- Standard: 1,000 requests/hour
- Premium: 5,000 requests/hour
- Enterprise: 20,000 requests/hour

**Headers**:
```http
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 847
X-RateLimit-Reset: 1617235200
```

**Rate Limit Exceeded Response**:
```json
{
  "error": "rate_limit_exceeded",
  "message": "Too many requests. Retry after 3600 seconds.",
  "retry_after": 3600
}
```

## Files API

### Upload File

Upload a file to CloudSync.

**Endpoint**: `POST /files/upload`

**Request**:
```http
POST /files/upload
Authorization: Bearer YOUR_ACCESS_TOKEN
Content-Type: multipart/form-data

file: [binary file data]
parent_folder_id: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
name: "quarterly-report.pdf"
```

**Response** (201 Created):
```json
{
  "id": "f9e8d7c6-b5a4-3210-9876-543210fedcba",
  "name": "quarterly-report.pdf",
  "size": 2457600,
  "mime_type": "application/pdf",
  "parent_folder_id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "created_at": "2024-03-20T10:30:00Z",
  "modified_at": "2024-03-20T10:30:00Z",
  "owner": {
    "id": "user-123",
    "email": "john.doe@example.com",
    "name": "John Doe"
  },
  "checksum": "sha256:a1b2c3d4e5f6..."
}
```

### Download File

Download a file from CloudSync.

**Endpoint**: `GET /files/{file_id}/download`

**Request**:
```http
GET /files/f9e8d7c6-b5a4-3210-9876-543210fedcba/download
Authorization: Bearer YOUR_ACCESS_TOKEN
```

**Response** (200 OK):
```http
Content-Type: application/pdf
Content-Disposition: attachment; filename="quarterly-report.pdf"
Content-Length: 2457600

[binary file data]
```

### Get File Metadata

Retrieve metadata for a specific file.

**Endpoint**: `GET /files/{file_id}`

**Request**:
```http
GET /files/f9e8d7c6-b5a4-3210-9876-543210fedcba
Authorization: Bearer YOUR_ACCESS_TOKEN
```

**Response** (200 OK):
```json
{
  "id": "f9e8d7c6-b5a4-3210-9876-543210fedcba",
  "name": "quarterly-report.pdf",
  "size": 2457600,
  "mime_type": "application/pdf",
  "parent_folder_id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "path": "/Finance/2024/quarterly-report.pdf",
  "created_at": "2024-03-20T10:30:00Z",
  "modified_at": "2024-03-20T15:45:00Z",
  "version": 3,
  "owner": {
    "id": "user-123",
    "email": "john.doe@example.com",
    "name": "John Doe"
  },
  "checksum": "sha256:a1b2c3d4e5f6...",
  "permissions": {
    "can_read": true,
    "can_write": true,
    "can_delete": true,
    "can_share": true
  }
}
```

### List Files

List files in a folder.

**Endpoint**: `GET /files`

**Query Parameters**:
- `parent_folder_id` (optional): Folder ID to list files from
- `page` (optional, default: 1): Page number
- `page_size` (optional, default: 50): Items per page
- `sort` (optional, default: "name"): Sort field (name, size, created_at, modified_at)
- `order` (optional, default: "asc"): Sort order (asc, desc)

**Request**:
```http
GET /files?parent_folder_id=a1b2c3d4-e5f6-7890-abcd-ef1234567890&page=1&page_size=20
Authorization: Bearer YOUR_ACCESS_TOKEN
```

**Response** (200 OK):
```json
{
  "files": [
    {
      "id": "f9e8d7c6-b5a4-3210-9876-543210fedcba",
      "name": "quarterly-report.pdf",
      "size": 2457600,
      "mime_type": "application/pdf",
      "created_at": "2024-03-20T10:30:00Z",
      "modified_at": "2024-03-20T15:45:00Z"
    },
    {
      "id": "a2b3c4d5-e6f7-8901-2345-6789abcdef01",
      "name": "budget-2024.xlsx",
      "size": 1048576,
      "mime_type": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      "created_at": "2024-03-19T14:20:00Z",
      "modified_at": "2024-03-19T14:20:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "page_size": 20,
    "total_pages": 5,
    "total_items": 87
  }
}
```

### Update File

Update file metadata or content.

**Endpoint**: `PATCH /files/{file_id}`

**Request**:
```http
PATCH /files/f9e8d7c6-b5a4-3210-9876-543210fedcba
Authorization: Bearer YOUR_ACCESS_TOKEN
Content-Type: application/json

{
  "name": "Q1-2024-report.pdf",
  "parent_folder_id": "new-folder-id"
}
```

**Response** (200 OK):
```json
{
  "id": "f9e8d7c6-b5a4-3210-9876-543210fedcba",
  "name": "Q1-2024-report.pdf",
  "size": 2457600,
  "modified_at": "2024-03-20T16:00:00Z"
}
```

### Delete File

Move file to trash or permanently delete.

**Endpoint**: `DELETE /files/{file_id}`

**Query Parameters**:
- `permanent` (optional, default: false): Permanently delete instead of moving to trash

**Request**:
```http
DELETE /files/f9e8d7c6-b5a4-3210-9876-543210fedcba
Authorization: Bearer YOUR_ACCESS_TOKEN
```

**Response** (204 No Content)

## Sharing API

### Create Share Link

Create a public sharing link for a file.

**Endpoint**: `POST /shares`

**Request**:
```http
POST /shares
Authorization: Bearer YOUR_ACCESS_TOKEN
Content-Type: application/json

{
  "file_id": "f9e8d7c6-b5a4-3210-9876-543210fedcba",
  "access": "view",
  "password": "SecureP@ssw0rd",
  "expires_at": "2024-04-20T23:59:59Z",
  "allow_download": true
}
```

**Response** (201 Created):
```json
{
  "id": "share-abc123def456",
  "file_id": "f9e8d7c6-b5a4-3210-9876-543210fedcba",
  "url": "https://cloudsync.com/s/abc123def456",
  "access": "view",
  "password_protected": true,
  "expires_at": "2024-04-20T23:59:59Z",
  "created_at": "2024-03-20T10:30:00Z",
  "allow_download": true,
  "view_count": 0
}
```

### Share with User

Share a file with specific users.

**Endpoint**: `POST /shares/user`

**Request**:
```http
POST /shares/user
Authorization: Bearer YOUR_ACCESS_TOKEN
Content-Type: application/json

{
  "file_id": "f9e8d7c6-b5a4-3210-9876-543210fedcba",
  "user_emails": ["jane.smith@example.com", "bob.jones@example.com"],
  "permission": "edit",
  "notify": true,
  "message": "Please review this quarterly report"
}
```

**Response** (201 Created):
```json
{
  "shares": [
    {
      "id": "share-user-001",
      "file_id": "f9e8d7c6-b5a4-3210-9876-543210fedcba",
      "user": {
        "email": "jane.smith@example.com",
        "name": "Jane Smith"
      },
      "permission": "edit",
      "created_at": "2024-03-20T10:35:00Z"
    },
    {
      "id": "share-user-002",
      "file_id": "f9e8d7c6-b5a4-3210-9876-543210fedcba",
      "user": {
        "email": "bob.jones@example.com",
        "name": "Bob Jones"
      },
      "permission": "edit",
      "created_at": "2024-03-20T10:35:00Z"
    }
  ]
}
```

## Search API

### Search Files

Search for files using keywords and filters.

**Endpoint**: `GET /search`

**Query Parameters**:
- `query` (required): Search keywords
- `type` (optional): File type filter (document, image, video, etc.)
- `owner` (optional): Owner email or ID
- `modified_after` (optional): ISO 8601 date
- `modified_before` (optional): ISO 8601 date
- `min_size` (optional): Minimum file size in bytes
- `max_size` (optional): Maximum file size in bytes
- `page` (optional, default: 1): Page number
- `page_size` (optional, default: 50): Items per page

**Request**:
```http
GET /search?query=quarterly+report&type=document&modified_after=2024-01-01T00:00:00Z
Authorization: Bearer YOUR_ACCESS_TOKEN
```

**Response** (200 OK):
```json
{
  "results": [
    {
      "id": "f9e8d7c6-b5a4-3210-9876-543210fedcba",
      "name": "Q1-2024-report.pdf",
      "snippet": "...quarterly report shows revenue growth of 15% compared to...",
      "score": 0.92,
      "path": "/Finance/2024/Q1-2024-report.pdf",
      "size": 2457600,
      "modified_at": "2024-03-20T16:00:00Z",
      "owner": {
        "name": "John Doe",
        "email": "john.doe@example.com"
      }
    }
  ],
  "pagination": {
    "page": 1,
    "page_size": 50,
    "total_results": 1
  },
  "query_time_ms": 87
}
```

## Webhooks API

### Create Webhook

Subscribe to events for real-time notifications.

**Endpoint**: `POST /webhooks`

**Request**:
```http
POST /webhooks
Authorization: Bearer YOUR_ACCESS_TOKEN
Content-Type: application/json

{
  "url": "https://yourapp.com/webhooks/cloudsync",
  "events": ["file.created", "file.updated", "file.deleted", "share.created"],
  "secret": "your-webhook-secret"
}
```

**Response** (201 Created):
```json
{
  "id": "webhook-abc123",
  "url": "https://yourapp.com/webhooks/cloudsync",
  "events": ["file.created", "file.updated", "file.deleted", "share.created"],
  "active": true,
  "created_at": "2024-03-20T10:40:00Z"
}
```

### Webhook Payload Example

When an event occurs, CloudSync sends a POST request to your webhook URL.

**Headers**:
```http
POST /webhooks/cloudsync HTTP/1.1
Content-Type: application/json
X-CloudSync-Signature: sha256=abc123def456...
X-CloudSync-Event: file.created
```

**Payload**:
```json
{
  "event": "file.created",
  "timestamp": "2024-03-20T10:45:00Z",
  "data": {
    "file": {
      "id": "new-file-123",
      "name": "new-document.pdf",
      "size": 524288,
      "owner": {
        "id": "user-456",
        "email": "user@example.com"
      }
    }
  }
}
```

## Error Handling

### Error Response Format

All API errors follow a consistent format:

```json
{
  "error": {
    "code": "file_not_found",
    "message": "The requested file could not be found",
    "details": {
      "file_id": "f9e8d7c6-b5a4-3210-9876-543210fedcba"
    },
    "request_id": "req-abc123def456"
  }
}
```

### Common Error Codes

| HTTP Status | Error Code | Description |
|------------|-----------|-------------|
| 400 | invalid_request | Malformed request |
| 401 | unauthorized | Missing or invalid authentication |
| 403 | forbidden | Insufficient permissions |
| 404 | not_found | Resource not found |
| 409 | conflict | Resource conflict (e.g., duplicate name) |
| 429 | rate_limit_exceeded | Too many requests |
| 500 | internal_error | Server error |
| 503 | service_unavailable | Temporary service outage |

## SDK Examples

### Python SDK

```python
from cloudsync import CloudSyncClient

# Initialize client
client = CloudSyncClient(api_key="YOUR_API_KEY")

# Upload file
file = client.files.upload(
    file_path="/path/to/quarterly-report.pdf",
    parent_folder_id="folder-123",
    name="quarterly-report.pdf"
)

# Search files
results = client.search(query="quarterly report", type="document")

# Create share link
share = client.shares.create(
    file_id=file.id,
    access="view",
    expires_in_days=30
)
print(f"Share URL: {share.url}")
```

### JavaScript SDK

```javascript
const CloudSync = require('cloudsync-sdk');

// Initialize client
const client = new CloudSync({ apiKey: 'YOUR_API_KEY' });

// Upload file
const file = await client.files.upload({
  filePath: '/path/to/quarterly-report.pdf',
  parentFolderId: 'folder-123',
  name: 'quarterly-report.pdf'
});

// Search files
const results = await client.search({
  query: 'quarterly report',
  type: 'document'
});

// Create share link
const share = await client.shares.create({
  fileId: file.id,
  access: 'view',
  expiresInDays: 30
});
console.log(`Share URL: ${share.url}`);
```

## Best Practices

### Pagination
Always use pagination for list endpoints to avoid timeouts and large response sizes.

### Error Handling
Implement exponential backoff for rate limit errors (429) and server errors (5xx).

### Webhooks
Validate webhook signatures using HMAC-SHA256 with your webhook secret.

```python
import hmac
import hashlib

def verify_webhook(payload, signature, secret):
    expected = hmac.new(
        secret.encode(),
        payload.encode(),
        hashlib.sha256
    ).hexdigest()
    return hmac.compare_digest(f"sha256={expected}", signature)
```

### Caching
Cache file metadata to reduce API calls. Use ETags for cache validation.

### Chunked Uploads
For files >100MB, use chunked upload for better reliability:

```http
POST /files/upload/chunked
Authorization: Bearer YOUR_ACCESS_TOKEN
Content-Type: application/json

{
  "name": "large-video.mp4",
  "size": 524288000,
  "chunk_size": 10485760
}
```

## Support

- **API Status**: https://status.cloudsync.com
- **Support Email**: api-support@cloudsync.com
- **Developer Forum**: https://community.cloudsync.com
- **GitHub**: https://github.com/cloudsync

---

**Last Updated**: March 20, 2024
**API Version**: 1.5.0

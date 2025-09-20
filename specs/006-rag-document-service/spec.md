# RAG Document Service Specification

## Overview

The RAG Document Service is a comprehensive microservice responsible for document ingestion, processing, and preparation for the Enterprise RAG system. It handles multi-format document uploads, text extraction, intelligent chunking, and integration with the vector embedding pipeline.

## Current Status

**✅ Production Ready & Fully Operational (2025-09-20)**

- All tests passing (12/12)
- Docker deployment functional at port 8082
- PostgreSQL integration complete
- Apache Tika text extraction operational
- Multi-tenant document management working
- Asynchronous processing pipeline implemented

## Service Architecture

### Core Responsibilities

1. **Document Ingestion**: Multi-format file upload with validation and security
2. **Text Extraction**: Apache Tika-based content extraction with metadata preservation
3. **Document Management**: Full CRUD operations with tenant isolation
4. **Intelligent Chunking**: Context-aware text segmentation for RAG optimization
5. **Storage Coordination**: File system and database synchronization
6. **Analytics**: Document statistics and storage usage tracking

### Technical Stack

- **Framework**: Spring Boot 3.2.8 with Spring Security
- **Database**: PostgreSQL with JPA/Hibernate
- **Text Processing**: Apache Tika for content extraction
- **File Storage**: Coordinated file system storage
- **Event Processing**: Kafka integration for async processing
- **API Documentation**: OpenAPI 3 with Swagger

## API Endpoints

### Document Management Operations

#### POST /api/v1/documents/upload
**Purpose**: Upload document for processing and integration into RAG system

**Request**:
```http
POST /api/v1/documents/upload
Content-Type: multipart/form-data
X-Tenant-ID: 550e8400-e29b-41d4-a716-446655440000

file: [document file]
metadata: {"category": "policy", "department": "hr"}
```

**Response**:
```json
{
  "id": "uuid",
  "filename": "generated-uuid.pdf",
  "originalFilename": "employee-handbook.pdf",
  "fileSize": 2048576,
  "contentType": "application/pdf",
  "documentType": "PDF",
  "processingStatus": "PENDING",
  "processingMessage": null,
  "chunkCount": 0,
  "embeddingModel": null,
  "metadata": {
    "category": "policy",
    "department": "hr"
  },
  "createdAt": "2025-09-20T10:30:00Z",
  "updatedAt": "2025-09-20T10:30:00Z",
  "uploadedBy": {
    "id": "uuid",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "role": "USER",
    "status": "ACTIVE"
  }
}
```

#### GET /api/v1/documents
**Purpose**: Retrieve paginated list of documents for tenant

**Request**:
```http
GET /api/v1/documents?page=0&size=20&sort=createdAt,desc
X-Tenant-ID: 550e8400-e29b-41d4-a716-446655440000
```

**Response**:
```json
{
  "content": [
    {
      "id": "uuid",
      "filename": "document.pdf",
      "documentType": "PDF",
      "processingStatus": "COMPLETED",
      "fileSize": 2048576,
      "chunkCount": 25,
      "createdAt": "2025-09-20T10:30:00Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "sorted": true,
      "orders": [{"property": "createdAt", "direction": "DESC"}]
    }
  },
  "totalElements": 150,
  "totalPages": 8,
  "first": true,
  "last": false
}
```

#### GET /api/v1/documents/{documentId}
**Purpose**: Retrieve detailed document information

**Response**: Same as upload response with current processing status

#### PUT /api/v1/documents/{documentId}
**Purpose**: Update document metadata and filename

**Request**:
```json
{
  "filename": "updated-filename.pdf",
  "metadata": {
    "category": "updated-policy",
    "version": "2.0"
  }
}
```

#### DELETE /api/v1/documents/{documentId}
**Purpose**: Delete document and all associated data

**Response**: HTTP 204 No Content

#### GET /api/v1/documents/stats
**Purpose**: Retrieve document statistics for tenant

**Response**:
```json
{
  "totalDocuments": 150,
  "storageUsageBytes": 104857600
}
```

## Document Processing Pipeline

### Phase 1: Upload and Validation

1. **File Validation**
   - File type validation (PDF, DOCX, TXT, MD, HTML)
   - File size limits (configurable per tenant)
   - MIME type verification
   - Security scanning

2. **Tenant Validation**
   - Document count limits
   - Storage quota limits
   - Tenant status verification

3. **Initial Storage**
   - Secure file storage with unique naming
   - Database record creation
   - Processing status: PENDING

### Phase 2: Asynchronous Processing

1. **Text Extraction**
   - Apache Tika content extraction
   - Encoding detection and normalization
   - Metadata extraction (author, creation date, etc.)
   - Error handling for corrupted files

2. **Content Analysis**
   - Language detection
   - Document structure analysis
   - Content quality assessment

3. **Intelligent Chunking**
   - Context-aware text segmentation
   - Configurable chunk sizes per tenant
   - Semantic boundary preservation
   - Overlap handling for context continuity

4. **Final Storage**
   - Extracted text storage
   - Chunk persistence
   - Processing status: COMPLETED
   - Metadata enrichment

### Phase 3: Embedding Integration

1. **Kafka Event Publishing**
   - Document chunks sent to embedding service
   - Processing status tracking
   - Error handling and retry logic

## Data Model

### Document Entity

```java
@Entity
@Table(name = "documents")
public class Document extends BaseEntity {
    @Column(nullable = false)
    private String filename;
    
    @Column(nullable = false)
    private String originalFilename;
    
    @Column
    private Long fileSize;
    
    @Column
    private String contentType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType documentType; // PDF, DOCX, DOC, TXT, MD, HTML, RTF, ODT
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessingStatus processingStatus; // PENDING, PROCESSING, COMPLETED, FAILED
    
    @Column(columnDefinition = "TEXT")
    private String processingMessage;
    
    @Column
    private String filePath;
    
    @Column(columnDefinition = "TEXT")
    private String extractedText;
    
    @Column(columnDefinition = "TEXT")
    private String metadata;
    
    @Column
    private Integer chunkCount;
    
    @Column
    private String embeddingModel;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    private User uploadedBy;
}
```

### DocumentChunk Entity

```java
@Entity
@Table(name = "document_chunks")
public class DocumentChunk extends BaseEntity {
    @Column(nullable = false)
    private Integer sequenceNumber;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column
    private Integer startIndex;
    
    @Column
    private Integer endIndex;
    
    @Column
    private Integer tokenCount;
    
    @Column
    private String embeddingVectorId;
    
    @Column(columnDefinition = "TEXT")
    private String metadata;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;
}
```

## Supported Document Types

### PDF Documents
- **Extensions**: .pdf
- **Features**: Text extraction, metadata preservation, image handling
- **Library**: Apache Tika with PDFBox
- **Limitations**: Scanned PDFs require OCR (future enhancement)

### Microsoft Word Documents
- **Extensions**: .doc, .docx
- **Features**: Full content extraction, style preservation, table handling
- **Library**: Apache Tika with Apache POI
- **Support**: Office 2003-2019+ formats

### Plain Text Files
- **Extensions**: .txt
- **Features**: Encoding detection, character normalization
- **Encodings**: UTF-8, UTF-16, ASCII, Latin-1
- **Processing**: Direct content extraction

### Markdown Files
- **Extensions**: .md, .markdown
- **Features**: Structure preservation, link extraction
- **Processing**: Content extraction with formatting hints
- **Metadata**: Header structure analysis

### HTML Documents
- **Extensions**: .html, .htm
- **Features**: Tag stripping, content extraction, link preservation
- **Processing**: Clean text extraction
- **Metadata**: Title, meta tags extraction

## Multi-Tenant Architecture

### Tenant Isolation

1. **Data Separation**
   - Complete document isolation by tenant ID
   - Tenant-scoped queries and operations
   - Cross-tenant access prevention

2. **Resource Limits**
   - Per-tenant document count limits
   - Storage quota enforcement
   - Configurable processing limits

3. **Configuration**
   - Tenant-specific chunking parameters
   - Processing pipeline customization
   - Storage location organization

### Security Features

1. **Access Control**
   - X-Tenant-ID header validation
   - JWT token integration (via gateway)
   - Operation-level tenant verification

2. **File Security**
   - MIME type validation
   - File size limits
   - Content security scanning
   - Secure file storage paths

3. **Audit Logging**
   - All operations logged with tenant context
   - Processing status tracking
   - Error logging and alerting

## Performance Characteristics

### Throughput

- **Document Upload**: ~100 docs/min (varies by size)
- **Text Extraction**: ~50 pages/sec (PDF processing)
- **Chunking**: ~1000 chunks/sec
- **Database Operations**: ~500 ops/sec

### Latency

- **Upload Response**: <500ms (synchronous portion)
- **Processing Time**: 1-30 seconds (depends on document size)
- **Retrieval Operations**: <100ms
- **Statistics Queries**: <50ms

### Scalability

- **Horizontal Scaling**: Stateless service design
- **Async Processing**: Queue-based processing pipeline
- **Database Optimization**: Indexed queries and connection pooling
- **File Storage**: Distributed storage support

## Integration Points

### Internal Service Dependencies

1. **rag-shared**: Common entities, DTOs, and utilities
2. **PostgreSQL**: Primary data storage
3. **File System**: Document storage (configurable location)
4. **Kafka**: Async processing events (optional)

### External Service Integration

1. **rag-auth-service (8081)**: User authentication and tenant validation
2. **rag-embedding-service (8083)**: Vector generation for processed chunks
3. **rag-core-service (8084)**: Document content for RAG queries
4. **rag-gateway (8080)**: API routing and security

### Event-Driven Architecture

```json
{
  "eventType": "document.uploaded",
  "documentId": "uuid",
  "tenantId": "uuid",
  "filename": "document.pdf",
  "timestamp": "2025-09-20T10:30:00Z"
}
```

```json
{
  "eventType": "chunks.created",
  "documentId": "uuid",
  "chunkIds": ["uuid1", "uuid2", "uuid3"],
  "tenantId": "uuid",
  "timestamp": "2025-09-20T10:30:00Z"
}
```

## Configuration

### Application Properties

```yaml
server:
  port: 8082

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/rag_documents
    username: ${DB_USERNAME:rag_user}
    password: ${DB_PASSWORD:rag_password}
    
  jpa:
    hibernate:
      ddl-auto: validate
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB

document:
  storage:
    path: ${STORAGE_PATH:/app/storage}
  processing:
    max-file-size: 52428800 # 50MB
    supported-types: pdf,docx,doc,txt,md,html,rtf,odt
  chunking:
    default-size: 512
    default-overlap: 64
    strategy: semantic

kafka:
  bootstrap-servers: ${KAFKA_SERVERS:localhost:9092}
  topics:
    document-processing: document-processing
    chunk-embedding: chunk-embedding

logging:
  level:
    com.byo.rag.document: INFO
    org.apache.tika: WARN
```

### Docker Configuration

```dockerfile
FROM openjdk:21-jre-slim

# Install necessary packages for Tika
RUN apt-get update && apt-get install -y \
    tesseract-ocr \
    tesseract-ocr-eng \
    && rm -rf /var/lib/apt/lists/*

COPY target/rag-document-service-*.jar app.jar

# Create storage directory
RUN mkdir -p /app/storage
VOLUME /app/storage

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## Monitoring and Observability

### Health Checks

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
      
  health:
    custom:
      - storage-health
      - tika-health
      - kafka-health
```

### Metrics

1. **Document Processing Metrics**
   - Upload rate and success/failure ratios
   - Processing time distributions
   - Chunk generation statistics
   - Storage usage by tenant

2. **System Metrics**
   - Memory usage (important for Tika operations)
   - File system space utilization
   - Database connection pool status
   - Kafka producer/consumer metrics

3. **Business Metrics**
   - Documents per tenant
   - Storage utilization by tenant
   - Processing error rates
   - Popular document types

### Logging

1. **Application Logs**
   - Document upload and processing events
   - Error conditions and stack traces
   - Performance timing information
   - Tenant operation audit trails

2. **Tika Logs**
   - Text extraction operations
   - Metadata extraction results
   - Processing warnings and errors

3. **Security Logs**
   - File upload validation results
   - Tenant access control events
   - Security scanning results

## Testing Strategy

### Unit Tests

1. **Service Layer Tests**: Document processing logic
2. **Controller Tests**: API endpoint validation
3. **Repository Tests**: Database operations
4. **Integration Tests**: Tika extraction and chunking

### Integration Tests

1. **End-to-End Processing**: Complete document pipeline
2. **Multi-Tenant Isolation**: Tenant separation validation
3. **File Storage**: Storage and retrieval operations
4. **API Validation**: Complete REST API testing

### Performance Tests

1. **Load Testing**: High-volume document uploads
2. **Stress Testing**: Memory usage under heavy processing
3. **Concurrent Processing**: Multi-threaded operations
4. **Storage Performance**: File I/O optimization

### Test Coverage

- **Current**: 12/12 tests passing (100% success rate)
- **Coverage Areas**: API endpoints, validation, error handling
- **Test Types**: Unit tests, integration tests, API validation

## Error Handling

### Validation Errors

```json
{
  "error": "VALIDATION_ERROR",
  "message": "File type not supported",
  "details": {
    "field": "file",
    "rejectedValue": "application/excel",
    "supportedTypes": ["application/pdf", "application/msword"]
  }
}
```

### Processing Errors

```json
{
  "error": "PROCESSING_ERROR",
  "message": "Text extraction failed",
  "details": {
    "documentId": "uuid",
    "stage": "TEXT_EXTRACTION",
    "cause": "Corrupted PDF structure"
  }
}
```

### Tenant Limit Errors

```json
{
  "error": "TENANT_LIMIT_EXCEEDED",
  "message": "Document count limit reached",
  "details": {
    "currentCount": 1000,
    "maxAllowed": 1000,
    "limitType": "DOCUMENT_COUNT"
  }
}
```

## Security Considerations

### File Upload Security

1. **MIME Type Validation**: Strict file type checking
2. **File Size Limits**: Configurable upload size restrictions
3. **Content Scanning**: Malware and virus scanning (future)
4. **Secure Storage**: Isolated file storage per tenant

### Access Control

1. **Tenant Isolation**: Complete data separation
2. **Authentication**: JWT token validation via gateway
3. **Authorization**: Operation-level permission checking
4. **Audit Logging**: Comprehensive operation tracking

### Data Protection

1. **Encryption at Rest**: File system encryption (infrastructure)
2. **Encryption in Transit**: HTTPS/TLS communication
3. **Data Retention**: Configurable retention policies
4. **Compliance**: GDPR/CCPA compliance features

## Future Enhancements

### Advanced Processing

1. **OCR Support**: Optical Character Recognition for scanned documents
2. **Image Processing**: Extract and analyze embedded images
3. **Advanced Chunking**: ML-based semantic chunking
4. **Content Analysis**: Automatic categorization and tagging

### Performance Optimizations

1. **Parallel Processing**: Multi-threaded document processing
2. **Caching**: Processed content caching
3. **CDN Integration**: Distributed file storage
4. **Database Optimization**: Advanced indexing and partitioning

### Integration Features

1. **External Storage**: S3/Azure Blob storage integration
2. **Webhook Support**: Real-time processing notifications
3. **Batch Processing**: Bulk document upload and processing
4. **API Versioning**: Backward-compatible API evolution

### Monitoring Enhancements

1. **Advanced Analytics**: Document processing insights
2. **Real-time Dashboards**: Processing status monitoring
3. **Alerting**: Proactive error and performance alerts
4. **Compliance Reporting**: Audit and compliance dashboards

## Deployment Considerations

### Production Requirements

1. **Storage**: High-performance file storage with backup
2. **Memory**: Sufficient memory for Tika operations (2-4GB)
3. **CPU**: Multi-core processing for document handling
4. **Network**: High bandwidth for file uploads

### High Availability

1. **Load Balancing**: Multiple service instances
2. **Data Replication**: Database and file storage replication
3. **Failover**: Automatic failover mechanisms
4. **Backup**: Regular data and file backups

### Security Hardening

1. **Container Security**: Secure container configuration
2. **Network Security**: VPC and firewall configuration
3. **Access Control**: IAM and service accounts
4. **Monitoring**: Security event monitoring and alerting
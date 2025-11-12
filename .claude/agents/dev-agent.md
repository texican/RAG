---
version: 1.0.0
last-updated: 2025-11-12
status: active
applies-to: 0.8.0-SNAPSHOT
category: agent-system
---

# Dev Agent

---
**version**: 1.0.0  
**last-updated**: 2025-11-12  
**domain**: Development  
**depends-on**: main agent-instructions.md  
**can-call**: test-agent, git-agent  
---

## Purpose

The Dev Agent is responsible for development tasks including implementing features, adding REST endpoints, debugging services, code standards enforcement, and development workflows.

## Responsibilities

- Implement new features and bug fixes
- Add REST endpoints and services
- Enforce code standards and patterns
- Debug services and troubleshoot issues
- Configuration management
- Database schema changes

## When to Use

**Invoke this agent when**:
- "Add a REST endpoint"
- "Implement this feature"
- "Fix this bug"
- "Debug this service"
- "Add configuration property"
- "Make schema change"
- "Write service logic"

**Don't invoke for**:
- Running tests (use test-agent)
- Deployment (use deploy-agent)
- Story completion (use backlog-agent)
- Version control (use git-agent)

---

## Common Development Tasks

### Adding a New REST Endpoint

**1. Create/Update Controller**:
```java
@RestController
@RequestMapping("/api/v1/myresource")
@RequiredArgsConstructor
public class MyResourceController {
    private final MyService myService;
    
    @PostMapping
    public ResponseEntity<MyDTO> create(@RequestBody MyDTO request) {
        return ResponseEntity.ok(myService.create(request));
    }
}
```

**2. Implement Service**:
```java
@Service
@RequiredArgsConstructor
public class MyService {
    private final MyRepository repository;
    
    @Transactional
    public MyDTO create(MyDTO dto) {
        // Business logic with tenant isolation
        return repository.save(dto);
    }
}
```

**3. Add Tests**:
```java
@SpringBootTest
class MyServiceTest {
    @Mock private MyRepository repository;
    @InjectMocks private MyService service;
    
    @Test
    void shouldCreateResource() {
        // Given, When, Then
    }
}
```

**4. Rebuild Service**:
```bash
make rebuild SERVICE=rag-{service}
```

### Adding a Configuration Property

**1. Add to application.yml**:
```yaml
myapp:
  my-feature:
    enabled: true
    timeout: 30s
```

**2. Create ConfigurationProperties class**:
```java
@ConfigurationProperties(prefix = "myapp.my-feature")
@Validated
public class MyFeatureConfig {
    private boolean enabled = true;
    private Duration timeout = Duration.ofSeconds(30);
    // getters/setters
}
```

**3. Enable in Application class**:
```java
@SpringBootApplication
@EnableConfigurationProperties(MyFeatureConfig.class)
public class MyApplication { }
```

### Debugging a Service

**1. Check logs**:
```bash
make logs SERVICE=rag-auth
```

**2. Check health**:
```bash
curl http://localhost:8081/actuator/health
```

**3. Access container**:
```bash
docker exec -it rag-auth sh
```

**4. Check database**:
```bash
docker exec -it rag-postgres psql -U rag_user -d byo_rag_local
```

**5. Check Redis**:
```bash
docker exec -it enterprise-rag-redis redis-cli
```

### Making Schema Changes

**Current (Until TECH-DEBT-005 Complete)**:
1. Modify JPA entity
2. Rebuild service
3. Schema updates automatically (ddl-auto: update)

**Future (With Flyway)**:
1. Create migration: `V{version}__{description}.sql`
2. Test migration locally
3. Rebuild service
4. Flyway applies on startup

---

## Code Standards & Patterns

### Tenant Isolation (Critical!)

**Every query MUST filter by tenant_id**:

```java
// ✅ CORRECT: Tenant-scoped query
documentRepository.findByTenantIdAndId(tenantId, documentId);

// ❌ WRONG: No tenant filtering (data leak!)
documentRepository.findById(documentId);
```

**Tenant extraction from JWT**:
```java
// Extract tenant from JWT claims
String tenantId = SecurityContextHolder.getContext()
    .getAuthentication()
    .getPrincipal()
    .getTenantId();

// Or from X-Tenant-ID header (fallback)
@RequestHeader("X-Tenant-ID") String tenantId
```

### Error Handling Pattern

**Standard exception handling**:
```java
@Service
public class MyService {
    public MyDTO doSomething(UUID tenantId, UUID id) {
        return myRepository.findByTenantIdAndId(tenantId, id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "MyResource", id, tenantId));
    }
}

// Custom exceptions in rag-shared
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, UUID id, UUID tenantId) {
        super(String.format("%s not found: id=%s, tenant=%s", 
            resource, id, tenantId));
    }
}
```

### Kafka Message Publishing (When Enabled)

**Optional Kafka integration**:
```java
@Service
@ConditionalOnBean(KafkaTemplate.class)  // Only register if Kafka available
public class MyKafkaService {
    @Autowired(required = false)  // Optional injection
    private KafkaTemplate<String, MyEvent> kafkaTemplate;
    
    public void publishEvent(MyEvent event) {
        if (kafkaTemplate != null) {
            kafkaTemplate.send("my-topic", event);
        }
        // Fallback: process synchronously
    }
}
```

---

## Development Workflow

### Standard Feature Implementation

**1. Understand Requirements**:
- Read story acceptance criteria
- Review related documentation
- Identify affected services

**2. Plan Implementation**:
- Identify files to modify
- Plan testing approach
- Consider tenant isolation

**3. Implement Changes**:
- Write code following standards
- Ensure tenant isolation
- Add comprehensive logging

**4. Write Tests**:
- Call `test-agent` to write tests
- Cover happy path, error cases, edge cases
- Test tenant isolation

**5. Verify Locally**:
- Rebuild service
- Run tests (call `test-agent`)
- Manual verification

**6. Commit Changes**:
- Call `git-agent` to commit with descriptive message
- Include "fixes" or "implements" keywords

### Bug Fix Workflow

**1. Reproduce Issue**:
- Understand symptoms
- Identify affected service
- Review logs

**2. Diagnose Root Cause**:
- Check recent changes
- Review related code
- Test hypotheses

**3. Implement Fix**:
- Make minimal targeted changes
- Add regression test
- Verify fix locally

**4. Verify**:
- Call `test-agent` to run all tests
- Manual verification
- Check for side effects

**5. Document**:
- Update documentation if needed
- Add comments explaining fix
- Call `git-agent` to commit

---

## Service-Specific Guidelines

### rag-auth-service (Port 8081)
- **Purpose**: Authentication, JWT tokens, user management
- **Dependencies**: PostgreSQL, Redis
- **Key Patterns**:
  - JWT token generation and validation
  - Password hashing with BCrypt
  - Session management in Redis
  - Multi-tenant user isolation

### rag-document-service (Port 8082)
- **Purpose**: Document upload, storage, chunking
- **Dependencies**: PostgreSQL, Kafka (optional), Redis, Cloud Storage
- **Key Patterns**:
  - File upload handling (multipart)
  - Document chunking for embedding
  - Metadata extraction
  - Async processing via Kafka (if enabled)

### rag-embedding-service (Port 8083)
- **Purpose**: Generate embeddings, vector storage
- **Dependencies**: Redis, Kafka (optional), Ollama/OpenAI
- **Key Patterns**:
  - Batch embedding generation
  - Vector storage in pgvector
  - Embedding model selection
  - Caching in Redis

### rag-core-service (Port 8084)
- **Purpose**: RAG queries, context assembly
- **Dependencies**: PostgreSQL, Redis, Ollama/OpenAI, rag-embedding
- **Key Patterns**:
  - Semantic search using embeddings
  - Context assembly from chunks
  - LLM integration (Ollama/OpenAI)
  - Response generation

### rag-admin-service (Port 8085)
- **Purpose**: Admin operations, tenant management
- **Dependencies**: PostgreSQL, Redis
- **Key Patterns**:
  - Tenant provisioning
  - User role management (ADMIN, USER)
  - System monitoring
  - Admin-only endpoints

---

## Code Quality Checklist

**Before calling test-agent**:
- [ ] Code follows project conventions
- [ ] Tenant isolation enforced
- [ ] Error handling implemented
- [ ] Logging added for debugging
- [ ] No hardcoded credentials
- [ ] Configuration externalized
- [ ] Javadoc comments added
- [ ] No security vulnerabilities

**After implementation**:
- [ ] Call `test-agent` to write tests
- [ ] Call `test-agent` to run tests
- [ ] Manual verification performed
- [ ] Documentation updated
- [ ] Call `git-agent` to commit

---

## Debugging Techniques

### Check Application Logs
```bash
# View logs for specific service
make logs SERVICE=rag-auth

# Follow logs in real-time
docker-compose logs -f rag-auth

# Filter logs by level
docker-compose logs rag-auth | grep ERROR
```

### Check Database State
```bash
# Connect to PostgreSQL
docker exec -it rag-postgres psql -U rag_user -d byo_rag_local

# Common queries
\dt                                    # List tables
SELECT * FROM tenants;                 # View tenants
SELECT * FROM users WHERE email='...'; # Find user
SELECT COUNT(*) FROM documents;        # Document count
```

### Check Redis Cache
```bash
# Connect to Redis
docker exec -it enterprise-rag-redis redis-cli -a redis_password

# Common commands
KEYS byo_rag_local:*                  # List keys
GET byo_rag_local:session:*           # View session
TTL byo_rag_local:session:*           # Check expiry
```

### Check Service Health
```bash
# Health endpoints
curl http://localhost:8081/actuator/health  # Auth
curl http://localhost:8082/actuator/health  # Document
curl http://localhost:8083/actuator/health  # Embedding
curl http://localhost:8084/actuator/health  # Core
curl http://localhost:8085/admin/api/actuator/health  # Admin

# Metrics
curl http://localhost:8081/actuator/prometheus
```

### Network Debugging
```bash
# Check service connectivity
docker exec -it rag-auth sh
wget -O- http://rag-document:8082/actuator/health

# Check DNS resolution
nslookup rag-document
```

---

## Cross-Agent Communication

### After Code Changes

**1. Call test-agent**:
- Write tests for new code
- Run test suite
- Verify 100% pass rate

**2. Call git-agent**:
- Commit changes with descriptive message
- Include story/ticket reference

### When Feature Complete

**1. Call test-agent**:
- Verify all tests passing
- Run integration tests
- Manual verification

**2. Call backlog-agent**:
- Request story completion
- backlog-agent will verify tests via test-agent

---

## Common Patterns

### Repository Method Naming
```java
// ✅ CORRECT: Include tenantId in method name
Optional<Document> findByTenantIdAndId(UUID tenantId, UUID id);
List<Document> findAllByTenantId(UUID tenantId);
Page<Document> findAllByTenantIdAndStatus(UUID tenantId, Status status, Pageable pageable);

// ❌ WRONG: No tenant filtering
Optional<Document> findById(UUID id);
List<Document> findAll();
```

### Service Layer Pattern
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {
    private final DocumentRepository repository;
    
    @Transactional(readOnly = true)
    public DocumentDTO getDocument(UUID tenantId, UUID documentId) {
        log.debug("Fetching document: id={}, tenant={}", documentId, tenantId);
        
        return repository.findByTenantIdAndId(tenantId, documentId)
            .map(DocumentMapper::toDTO)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Document", documentId, tenantId));
    }
    
    @Transactional
    public DocumentDTO createDocument(UUID tenantId, DocumentDTO dto) {
        log.info("Creating document: name={}, tenant={}", dto.getName(), tenantId);
        
        Document document = DocumentMapper.toEntity(dto);
        document.setTenantId(tenantId);
        document.setCreatedAt(Instant.now());
        
        Document saved = repository.save(document);
        log.info("Document created: id={}", saved.getId());
        
        return DocumentMapper.toDTO(saved);
    }
}
```

### Controller Layer Pattern
```java
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Validated
public class DocumentController {
    private final DocumentService documentService;
    
    @GetMapping("/{documentId}")
    public ResponseEntity<DocumentDTO> getDocument(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @PathVariable UUID documentId) {
        
        DocumentDTO document = documentService.getDocument(tenantId, documentId);
        return ResponseEntity.ok(document);
    }
    
    @PostMapping
    public ResponseEntity<DocumentDTO> createDocument(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @Valid @RequestBody DocumentDTO request) {
        
        DocumentDTO document = documentService.createDocument(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(document);
    }
}
```

---

## Critical Reminders

**🚨 NEVER**:
- Skip tenant isolation checks
- Hardcode credentials or secrets
- Commit without running tests
- Deploy without tests passing
- Make breaking changes without migration plan

**✅ ALWAYS**:
- Filter by tenant_id in all queries
- Validate input parameters
- Handle errors gracefully
- Log important operations
- Write tests for new code (via test-agent)
- Commit working code (via git-agent)

---

## Resources

**Documentation**:
- Code standards: `QUALITY_STANDARDS.md`
- Development guide: `docs/development/DEVELOPMENT_GUIDE.md`
- Testing guide: `docs/development/TESTING_BEST_PRACTICES.md`
- Error handling: `docs/development/ERROR_HANDLING_GUIDELINES.md`

**See Also**:
- Main instructions: `../agent-instructions.md`
- Test agent: `./test-agent.md` (for testing)
- Git agent: `./git-agent.md` (for commits)
- Backlog agent: `./backlog-agent.md` (for story completion)

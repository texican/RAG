package com.byo.rag.integration.endtoend;

import com.byo.rag.integration.base.BaseIntegrationTest;
import com.byo.rag.integration.data.TestDataBuilder;
import com.byo.rag.integration.data.TestDataCleanup;
import com.byo.rag.integration.utils.AuthenticationTestUtils;
import com.byo.rag.integration.utils.IntegrationTestUtils;
import com.byo.rag.shared.dto.DocumentDto;
import com.byo.rag.shared.dto.TenantDto;
import com.byo.rag.shared.entity.Document;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Comprehensive End-to-End Integration Test for RAG System.
 *
 * This test validates the complete RAG pipeline using real-world documents:
 * 1. Tenant creation and authentication
 * 2. Document upload with real technical documentation
 * 3. Document processing (chunking, embedding generation)
 * 4. Vector storage and indexing
 * 5. Query processing with context retrieval
 * 6. LLM integration and response generation
 * 7. Source citation and relevance scoring
 *
 * Test Scenario:
 * - Upload comprehensive enterprise documents (policies, specifications, API docs)
 * - Ask realistic questions that require document context
 * - Validate that responses are accurate and cite relevant sources
 * - Measure end-to-end performance and quality metrics
 */
@DisplayName("Comprehensive RAG End-to-End Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ComprehensiveRagEndToEndIT extends BaseIntegrationTest {

    @Autowired
    private TestDataCleanup testDataCleanup;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private AuthenticationTestUtils.TestTenantSetup testTenant;
    private String baseUrl;

    // Document IDs for uploaded test documents
    private UUID policyDocumentId;
    private UUID productSpecDocumentId;
    private UUID apiDocumentId;

    // URLs for different services
    private String authServiceUrl;
    private String documentServiceUrl;
    private String coreServiceUrl;

    @BeforeEach
    void setUp() {
        baseUrl = getBaseUrl();
        authServiceUrl = "http://localhost:8081/api/v1";
        documentServiceUrl = "http://localhost:8082/api/v1";
        coreServiceUrl = "http://localhost:8084/api/v1";

        // Create test tenant with realistic configuration
        TenantDto.CreateTenantRequest tenantRequest = new TenantDto.CreateTenantRequest(
            "Enterprise RAG Test Corp",
            TestDataBuilder.createUniqueTenantSlug(),
            "Comprehensive end-to-end testing tenant for RAG system validation",
            new TenantDto.TenantConfig(
                1000,  // maxDocuments
                10240L,  // maxStorageMb (10GB)
                "nomic-embed-text",  // embeddingModel
                "llama3.2:1b",  // llmModel
                new TenantDto.ChunkingConfig(
                    512,  // chunkSize - realistic for technical docs
                    64,   // chunkOverlap
                    TenantDto.ChunkingStrategy.SEMANTIC
                )
            )
        );

        testTenant = AuthenticationTestUtils.createTestTenantWithAdmin(
            new RestTemplate(),
            authServiceUrl,
            tenantRequest,
            TestDataBuilder.createAdminUserRequest(UUID.randomUUID())
        );

        System.out.println("=== Test Setup Complete ===");
        System.out.println("Tenant ID: " + testTenant.getTenantId());
        System.out.println("Admin Token: " + testTenant.adminToken().substring(0, 20) + "...");
    }

    @AfterEach
    void cleanup() {
        if (testTenant != null) {
            testDataCleanup.cleanupTenantData(testTenant.getTenantId());
        }
    }

    /**
     * E2E-TEST-001: Complete RAG Pipeline - Document Upload to Query Response
     *
     * This test validates the entire RAG system workflow:
     * - Upload multiple real-world documents
     * - Wait for complete processing (chunking + embedding)
     * - Execute queries requiring cross-document knowledge
     * - Verify response quality and source citations
     */
    @Test
    @Order(1)
    @DisplayName("E2E-001: Complete RAG pipeline from upload to query with real-world data")
    void testCompleteRagPipelineWithRealWorldDocuments() throws Exception {
        System.out.println("\n=== Starting E2E-001: Complete RAG Pipeline Test ===");

        // PHASE 1: Upload Real-World Documents
        System.out.println("\n--- PHASE 1: Document Upload ---");

        policyDocumentId = uploadDocumentFromResource(
            "test-documents/company-policy.md",
            "company-information-security-policy.md",
            "text/markdown",
            Map.of(
                "category", "policy",
                "department", "security",
                "classification", "confidential",
                "version", "2.1"
            )
        );
        System.out.println("✓ Uploaded security policy document: " + policyDocumentId);

        productSpecDocumentId = uploadDocumentFromResource(
            "test-documents/product-specification.md",
            "cloudsync-technical-specification.md",
            "text/markdown",
            Map.of(
                "category", "technical-spec",
                "product", "CloudSync Enterprise",
                "version", "3.5.0"
            )
        );
        System.out.println("✓ Uploaded product specification: " + productSpecDocumentId);

        apiDocumentId = uploadDocumentFromResource(
            "test-documents/api-documentation.md",
            "cloudsync-api-documentation.md",
            "text/markdown",
            Map.of(
                "category", "api-docs",
                "api_version", "1.5.0"
            )
        );
        System.out.println("✓ Uploaded API documentation: " + apiDocumentId);

        // PHASE 2: Wait for Document Processing
        System.out.println("\n--- PHASE 2: Document Processing ---");

        waitForDocumentProcessing(policyDocumentId, "Security Policy");
        waitForDocumentProcessing(productSpecDocumentId, "Product Specification");
        waitForDocumentProcessing(apiDocumentId, "API Documentation");

        // PHASE 3: Verify Document Processing Results
        System.out.println("\n--- PHASE 3: Processing Verification ---");

        verifyDocumentProcessingQuality(policyDocumentId, "Security Policy", 20, 100);
        verifyDocumentProcessingQuality(productSpecDocumentId, "Product Spec", 30, 150);
        verifyDocumentProcessingQuality(apiDocumentId, "API Docs", 25, 120);

        // PHASE 4: Execute RAG Queries with Real-World Scenarios
        System.out.println("\n--- PHASE 4: RAG Query Execution ---");

        // Query 1: Security Policy - Password Requirements
        testRagQuery(
            "What are the password requirements according to the security policy?",
            new String[]{"12 characters", "complexity", "uppercase", "lowercase", "90 days", "MFA"},
            new String[]{"company-information-security-policy.md"},
            0.7
        );

        // Query 2: Security Policy - Data Classification
        testRagQuery(
            "What are the different data classification levels and how should confidential data be handled?",
            new String[]{"Public", "Internal", "Confidential", "Restricted", "encrypted", "access limited"},
            new String[]{"company-information-security-policy.md"},
            0.7
        );

        // Query 3: Product Specification - Architecture
        testRagQuery(
            "What technologies are used in the CloudSync frontend applications?",
            new String[]{"React", "TypeScript", "Electron", "Material-UI", "Redux"},
            new String[]{"cloudsync-technical-specification.md"},
            0.7
        );

        // Query 4: Product Specification - Performance
        testRagQuery(
            "What are the performance SLA targets for the CloudSync platform?",
            new String[]{"99.95%", "uptime", "200ms", "latency", "500ms", "search"},
            new String[]{"cloudsync-technical-specification.md"},
            0.6
        );

        // Query 5: API Documentation - Authentication
        testRagQuery(
            "How do I authenticate with the CloudSync API using OAuth 2.0?",
            new String[]{"OAuth", "authorization", "access_token", "client_id", "redirect_uri"},
            new String[]{"cloudsync-api-documentation.md"},
            0.7
        );

        // Query 6: API Documentation - Rate Limiting
        testRagQuery(
            "What are the rate limits for the CloudSync API?",
            new String[]{"1,000", "5,000", "20,000", "requests", "hour", "Standard", "Premium", "Enterprise"},
            new String[]{"cloudsync-api-documentation.md"},
            0.6
        );

        // Query 7: Cross-Document Query - Security + Technical
        testRagQuery(
            "How does CloudSync ensure data security both at the infrastructure level and through access controls?",
            new String[]{"encryption", "AES-256", "TLS", "authentication", "MFA", "access control"},
            new String[]{"company-information-security-policy.md", "cloudsync-technical-specification.md"},
            0.5  // Lower threshold for cross-document queries
        );

        // PHASE 5: Performance and Quality Metrics
        System.out.println("\n--- PHASE 5: System Metrics ---");

        verifySystemMetrics();

        System.out.println("\n=== E2E-001 Test Completed Successfully ===");
    }

    /**
     * E2E-TEST-002: Semantic Search Quality Validation
     *
     * Tests the quality of semantic search by asking questions in different ways
     * and validating that the system returns relevant context regardless of phrasing.
     */
    @Test
    @Order(2)
    @DisplayName("E2E-002: Semantic search quality across query variations")
    void testSemanticSearchQuality() throws Exception {
        System.out.println("\n=== Starting E2E-002: Semantic Search Quality Test ===");

        // Upload documents first
        policyDocumentId = uploadDocumentFromResource(
            "test-documents/company-policy.md",
            "security-policy.md",
            "text/markdown",
            Map.of("category", "policy")
        );

        waitForDocumentProcessing(policyDocumentId, "Security Policy");

        // Test semantic understanding with query variations
        List<String> queryVariations = Arrays.asList(
            "What is the password policy?",
            "Tell me about password requirements",
            "How long should passwords be?",
            "What are the rules for creating passwords?",
            "Password complexity requirements"
        );

        System.out.println("\n--- Testing Query Variations for Semantic Understanding ---");

        for (String query : queryVariations) {
            System.out.println("\nQuery: " + query);
            RagQueryResponse response = executeRagQuery(query);

            assertThat(response).isNotNull();
            assertThat(response.answer).isNotEmpty();
            assertThat(response.sources).isNotEmpty();

            // All variations should reference the security policy document
            boolean hasSecurityPolicy = response.sources.stream()
                .anyMatch(s -> s.fileName.contains("security-policy"));
            assertThat(hasSecurityPolicy)
                .describedAs("Query '%s' should retrieve security policy document", query)
                .isTrue();

            // Verify minimum relevance score
            double maxScore = response.sources.stream()
                .mapToDouble(s -> s.relevanceScore)
                .max()
                .orElse(0.0);
            assertThat(maxScore)
                .describedAs("Query '%s' should have high relevance score", query)
                .isGreaterThan(0.5);

            System.out.println("  ✓ Max relevance: " + String.format("%.2f", maxScore));
        }

        System.out.println("\n=== E2E-002 Test Completed Successfully ===");
    }

    /**
     * E2E-TEST-003: Multi-Document Context Assembly
     *
     * Validates that the RAG system can correctly assemble context from multiple
     * documents to answer complex questions requiring cross-document knowledge.
     */
    @Test
    @Order(3)
    @DisplayName("E2E-003: Multi-document context assembly for complex queries")
    void testMultiDocumentContextAssembly() throws Exception {
        System.out.println("\n=== Starting E2E-003: Multi-Document Context Test ===");

        // Upload all documents
        policyDocumentId = uploadDocumentFromResource(
            "test-documents/company-policy.md",
            "policy.md",
            "text/markdown",
            Map.of("type", "policy")
        );

        productSpecDocumentId = uploadDocumentFromResource(
            "test-documents/product-specification.md",
            "spec.md",
            "text/markdown",
            Map.of("type", "spec")
        );

        apiDocumentId = uploadDocumentFromResource(
            "test-documents/api-documentation.md",
            "api.md",
            "text/markdown",
            Map.of("type", "api")
        );

        waitForDocumentProcessing(policyDocumentId, "Policy");
        waitForDocumentProcessing(productSpecDocumentId, "Spec");
        waitForDocumentProcessing(apiDocumentId, "API");

        System.out.println("\n--- Testing Cross-Document Queries ---");

        // Complex query requiring multiple documents
        String complexQuery = "How does the system handle authentication and what are the security requirements?";
        RagQueryResponse response = executeRagQuery(complexQuery);

        assertThat(response.sources)
            .describedAs("Complex query should retrieve sources from multiple documents")
            .hasSizeGreaterThanOrEqualTo(2);

        // Verify sources from different documents
        Set<String> uniqueDocuments = response.sources.stream()
            .map(s -> s.fileName)
            .collect(Collectors.toSet());

        System.out.println("  Sources from " + uniqueDocuments.size() + " unique documents:");
        uniqueDocuments.forEach(doc -> System.out.println("    - " + doc));

        assertThat(uniqueDocuments.size())
            .describedAs("Should retrieve context from multiple documents")
            .isGreaterThanOrEqualTo(2);

        // Verify answer quality
        assertThat(response.answer)
            .describedAs("Answer should be comprehensive")
            .hasSizeGreaterThan(100);

        System.out.println("\n  Generated answer length: " + response.answer.length() + " characters");
        System.out.println("  Total sources: " + response.sources.size());

        System.out.println("\n=== E2E-003 Test Completed Successfully ===");
    }

    /**
     * E2E-TEST-004: Response Quality and Citation Accuracy
     *
     * Validates that:
     * - Responses are factually grounded in source documents
     * - Citations are accurate and relevant
     * - Relevance scores correlate with answer quality
     */
    @Test
    @Order(4)
    @DisplayName("E2E-004: Response quality and citation accuracy validation")
    void testResponseQualityAndCitationAccuracy() throws Exception {
        System.out.println("\n=== Starting E2E-004: Response Quality Test ===");

        // Upload single document for controlled testing
        policyDocumentId = uploadDocumentFromResource(
            "test-documents/company-policy.md",
            "policy.md",
            "text/markdown",
            Map.of("type", "policy")
        );

        waitForDocumentProcessing(policyDocumentId, "Policy");

        // Test specific factual queries
        String[] factualQueries = {
            "What is the minimum password length?",
            "How often should passwords be changed?",
            "What encryption standard is required for data at rest?",
            "What is the incident response time for critical incidents?"
        };

        String[] expectedFacts = {
            "12",
            "90",
            "AES-256",
            "15 minutes"
        };

        System.out.println("\n--- Testing Factual Accuracy ---");

        for (int i = 0; i < factualQueries.length; i++) {
            String query = factualQueries[i];
            String expectedFact = expectedFacts[i];

            System.out.println("\nQuery: " + query);
            System.out.println("Expected fact: " + expectedFact);

            RagQueryResponse response = executeRagQuery(query);

            // Verify answer contains expected fact
            assertThat(response.answer.toLowerCase())
                .describedAs("Answer should contain expected fact '%s'", expectedFact)
                .contains(expectedFact.toLowerCase());

            // Verify sources are relevant
            assertThat(response.sources)
                .describedAs("Should have at least one source")
                .isNotEmpty();

            double topScore = response.sources.get(0).relevanceScore;
            assertThat(topScore)
                .describedAs("Top source should have high relevance")
                .isGreaterThan(0.6);

            System.out.println("  ✓ Fact verified in answer");
            System.out.println("  ✓ Top relevance: " + String.format("%.3f", topScore));
        }

        System.out.println("\n=== E2E-004 Test Completed Successfully ===");
    }

    /**
     * E2E-TEST-005: System Performance Under Load
     *
     * Tests system performance with multiple concurrent queries
     * and validates response time SLAs.
     */
    @Test
    @Order(5)
    @DisplayName("E2E-005: System performance validation under query load")
    void testSystemPerformanceUnderLoad() throws Exception {
        System.out.println("\n=== Starting E2E-005: Performance Test ===");

        // Upload documents
        policyDocumentId = uploadDocumentFromResource(
            "test-documents/company-policy.md",
            "policy.md",
            "text/markdown",
            Map.of("type", "policy")
        );

        waitForDocumentProcessing(policyDocumentId, "Policy");

        List<String> testQueries = Arrays.asList(
            "What is the password policy?",
            "What are the data classification levels?",
            "What is the incident response process?",
            "What are the network security requirements?",
            "How should backups be performed?"
        );

        List<Long> responseTimes = new ArrayList<>();

        System.out.println("\n--- Executing Performance Queries ---");

        for (String query : testQueries) {
            long startTime = System.currentTimeMillis();

            RagQueryResponse response = executeRagQuery(query);

            long endTime = System.currentTimeMillis();
            long responseTime = endTime - startTime;

            responseTimes.add(responseTime);

            assertThat(response.answer).isNotEmpty();

            System.out.println(String.format("  Query: '%s...' - %dms",
                query.substring(0, Math.min(40, query.length())), responseTime));
        }

        // Calculate statistics
        double avgResponseTime = responseTimes.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);

        long maxResponseTime = responseTimes.stream()
            .mapToLong(Long::longValue)
            .max()
            .orElse(0L);

        System.out.println("\n--- Performance Statistics ---");
        System.out.println(String.format("  Average response time: %.0fms", avgResponseTime));
        System.out.println(String.format("  Max response time: %dms", maxResponseTime));
        System.out.println(String.format("  Total queries: %d", testQueries.size()));

        // Validate performance SLAs (reasonable for local testing with Ollama)
        assertThat(avgResponseTime)
            .describedAs("Average response time should be under 30 seconds")
            .isLessThan(30000);

        System.out.println("\n=== E2E-005 Test Completed Successfully ===");
    }

    // ==================== Helper Methods ====================

    private UUID uploadDocumentFromResource(String resourcePath, String fileName,
                                           String contentType, Map<String, Object> metadata) throws IOException {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        byte[] content = Files.readAllBytes(resource.getFile().toPath());

        return uploadDocument(fileName, content, contentType, metadata);
    }

    private UUID uploadDocument(String filename, byte[] content, String contentType,
                               Map<String, Object> metadata) {
        String uploadUrl = documentServiceUrl + "/documents/upload";

        HttpHeaders headers = IntegrationTestUtils.createAuthHeaders(testTenant.adminToken());
        headers.set("X-Tenant-ID", testTenant.getTenantId().toString());
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        });

        if (metadata != null && !metadata.isEmpty()) {
            try {
                String metadataJson = objectMapper.writeValueAsString(metadata);
                body.add("metadata", metadataJson);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize metadata", e);
            }
        }

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<DocumentDto.DocumentResponse> response = restTemplate.exchange(
            uploadUrl,
            HttpMethod.POST,
            requestEntity,
            DocumentDto.DocumentResponse.class
        );

        IntegrationTestUtils.assertSuccessfulResponse(response);
        return response.getBody().id();
    }

    private void waitForDocumentProcessing(UUID documentId, String documentName) {
        System.out.println("  Waiting for processing: " + documentName);

        await(documentName + " processing")
            .atMost(Duration.ofMinutes(5))
            .pollInterval(Duration.ofSeconds(2))
            .until(() -> {
                try {
                    DocumentDto.DocumentResponse doc = getDocument(documentId);
                    Document.ProcessingStatus status = doc.processingStatus();

                    if (status == Document.ProcessingStatus.COMPLETED) {
                        System.out.println("    ✓ Processing completed");
                        return true;
                    } else if (status == Document.ProcessingStatus.FAILED) {
                        System.out.println("    ✗ Processing failed!");
                        throw new RuntimeException("Document processing failed: " + documentId);
                    } else {
                        System.out.print(".");
                        return false;
                    }
                } catch (Exception e) {
                    System.out.println("    Error checking status: " + e.getMessage());
                    return false;
                }
            });
    }

    private void verifyDocumentProcessingQuality(UUID documentId, String documentName,
                                                int minChunks, int maxChunks) {
        DocumentDto.DocumentResponse doc = getDocument(documentId);

        assertThat(doc.processingStatus())
            .describedAs("%s should be completed", documentName)
            .isEqualTo(Document.ProcessingStatus.COMPLETED);

        assertThat(doc.chunkCount())
            .describedAs("%s should have chunks within expected range", documentName)
            .isBetween(minChunks, maxChunks);

        System.out.println(String.format("  ✓ %s: %d chunks generated", documentName, doc.chunkCount()));

        // Verify chunks in database
        Integer dbChunkCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM document_chunks WHERE document_id = ?",
            Integer.class,
            documentId
        );

        assertThat(dbChunkCount)
            .describedAs("%s chunk count should match between API and database", documentName)
            .isEqualTo(doc.chunkCount());
    }

    private DocumentDto.DocumentResponse getDocument(UUID documentId) {
        String getUrl = documentServiceUrl + "/documents/" + documentId;

        HttpHeaders headers = IntegrationTestUtils.createAuthHeaders(testTenant.adminToken());
        headers.set("X-Tenant-ID", testTenant.getTenantId().toString());

        ResponseEntity<DocumentDto.DocumentResponse> response = restTemplate.exchange(
            getUrl,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            DocumentDto.DocumentResponse.class
        );

        IntegrationTestUtils.assertSuccessfulResponse(response);
        return response.getBody();
    }

    private void testRagQuery(String query, String[] expectedKeywords,
                            String[] expectedSourceFiles, double minRelevance) {
        System.out.println("\n  Query: " + query);

        RagQueryResponse response = executeRagQuery(query);

        // Verify response exists and is not empty
        assertThat(response)
            .describedAs("RAG query should return a response")
            .isNotNull();
        assertThat(response.answer)
            .describedAs("RAG answer should not be empty")
            .isNotEmpty();

        // Verify answer contains expected keywords
        String answerLower = response.answer.toLowerCase();
        int keywordsFound = 0;
        for (String keyword : expectedKeywords) {
            if (answerLower.contains(keyword.toLowerCase())) {
                keywordsFound++;
            }
        }

        int minKeywords = (int) Math.ceil(expectedKeywords.length * 0.5); // At least 50% of keywords
        assertThat(keywordsFound)
            .describedAs("Answer should contain at least %d of %d expected keywords",
                minKeywords, expectedKeywords.length)
            .isGreaterThanOrEqualTo(minKeywords);

        System.out.println("    ✓ Found " + keywordsFound + "/" + expectedKeywords.length + " keywords");

        // Verify sources
        assertThat(response.sources)
            .describedAs("Query should return source citations")
            .isNotEmpty();

        // Verify at least one expected source file is referenced
        boolean hasExpectedSource = false;
        for (String expectedFile : expectedSourceFiles) {
            boolean found = response.sources.stream()
                .anyMatch(s -> s.fileName.contains(expectedFile));
            if (found) {
                hasExpectedSource = true;
                break;
            }
        }

        assertThat(hasExpectedSource)
            .describedAs("Should cite at least one expected source document")
            .isTrue();

        // Verify relevance scores
        double topRelevance = response.sources.get(0).relevanceScore;
        assertThat(topRelevance)
            .describedAs("Top source should have relevance score >= %.2f", minRelevance)
            .isGreaterThanOrEqualTo(minRelevance);

        System.out.println("    ✓ Sources: " + response.sources.size() +
            " (top relevance: " + String.format("%.3f", topRelevance) + ")");
        System.out.println("    ✓ Answer length: " + response.answer.length() + " characters");
    }

    private RagQueryResponse executeRagQuery(String query) {
        String queryUrl = coreServiceUrl + "/rag/query";

        HttpHeaders headers = IntegrationTestUtils.createAuthHeaders(testTenant.adminToken());
        headers.set("X-Tenant-ID", testTenant.getTenantId().toString());
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("tenantId", testTenant.getTenantId().toString());
        requestBody.put("query", query);
        requestBody.put("includeContext", true);
        requestBody.put("topK", 5);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            queryUrl,
            HttpMethod.POST,
            requestEntity,
            String.class
        );

        IntegrationTestUtils.assertSuccessfulResponse(response);

        try {
            JsonNode root = objectMapper.readTree(response.getBody());

            RagQueryResponse ragResponse = new RagQueryResponse();
            ragResponse.answer = root.path("response").asText();
            ragResponse.sources = new ArrayList<>();

            JsonNode sourcesNode = root.path("sources");
            if (sourcesNode.isArray()) {
                for (JsonNode sourceNode : sourcesNode) {
                    SourceCitation source = new SourceCitation();
                    source.fileName = sourceNode.path("fileName").asText();
                    source.relevanceScore = sourceNode.path("relevanceScore").asDouble();
                    source.excerpt = sourceNode.path("excerpt").asText();
                    ragResponse.sources.add(source);
                }
            }

            return ragResponse;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse RAG query response", e);
        }
    }

    private void verifySystemMetrics() {
        // Query database for system metrics
        Integer totalDocuments = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM documents WHERE tenant_id = ?",
            Integer.class,
            testTenant.getTenantId()
        );

        Integer totalChunks = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM document_chunks dc " +
            "JOIN documents d ON dc.document_id = d.id " +
            "WHERE d.tenant_id = ?",
            Integer.class,
            testTenant.getTenantId()
        );

        System.out.println("  Total documents processed: " + totalDocuments);
        System.out.println("  Total chunks generated: " + totalChunks);
        System.out.println("  Average chunks per document: " +
            (totalDocuments > 0 ? totalChunks / totalDocuments : 0));

        assertThat(totalDocuments)
            .describedAs("Should have processed all uploaded documents")
            .isEqualTo(3);

        assertThat(totalChunks)
            .describedAs("Should have generated chunks for all documents")
            .isGreaterThan(50);
    }

    // ==================== Inner Classes ====================

    private static class RagQueryResponse {
        String answer;
        List<SourceCitation> sources;
    }

    private static class SourceCitation {
        String fileName;
        double relevanceScore;
        String excerpt;
    }
}

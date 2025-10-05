package com.byo.rag.integration.endtoend;

import com.byo.rag.shared.dto.DocumentDto;
import com.byo.rag.shared.dto.TenantDto;
import com.byo.rag.shared.dto.UserDto;
import com.byo.rag.shared.entity.Document;
import com.byo.rag.shared.entity.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Standalone End-to-End Test for RAG System (No TestContainers).
 *
 * This test connects to existing running services and validates the complete RAG pipeline.
 * Prerequisites: All services must be running (docker-compose up)
 */
@DisplayName("Standalone RAG E2E Tests - Real Services")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StandaloneRagE2ETest {

    private static final String AUTH_URL = "http://localhost:8081/api/v1";
    private static final String DOCUMENT_URL = "http://localhost:8082/api/v1";
    private static final String CORE_URL = "http://localhost:8084/api/v1";

    private static final RestTemplate restTemplate = new RestTemplate();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static UUID tenantId;
    private static String adminToken;
    private static List<UUID> uploadedDocumentIds = new ArrayList<>();

    @BeforeAll
    static void setupTenant() throws Exception {
        System.out.println("\n=== Setting Up Test Environment ===");

        try {
            // Login as existing admin (uses existing tenant)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            UserDto.LoginRequest loginRequest = new UserDto.LoginRequest("admin@enterprise-rag.com", "admin12345");
            HttpEntity<UserDto.LoginRequest> loginEntity = new HttpEntity<>(loginRequest, headers);

            ResponseEntity<Map> loginResponse = restTemplate.exchange(
                AUTH_URL + "/auth/login",
                HttpMethod.POST,
                loginEntity,
                Map.class
            );

            adminToken = loginResponse.getBody().get("accessToken").toString();

            // Extract tenant ID from JWT token
            String[] tokenParts = adminToken.split("\\.");
            if (tokenParts.length > 1) {
                String payload = new String(Base64.getDecoder().decode(tokenParts[1]));
                JsonNode claims = objectMapper.readTree(payload);
                tenantId = UUID.fromString(claims.get("tenantId").asText());
            }

            System.out.println("✓ Logged in as admin user");
            System.out.println("✓ Using tenant: " + tenantId);
            System.out.println("=== Setup Complete ===\n");

        } catch (Exception e) {
            System.err.println("Setup failed: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @AfterAll
    static void cleanup() {
        System.out.println("\n=== Cleanup Complete ===");
        // Note: In production, we'd delete the tenant and all associated data
    }

    @Test
    @Order(1)
    @DisplayName("E2E-001: Upload and process real-world documents")
    void testDocumentUploadAndProcessing() throws Exception {
        System.out.println("\n=== E2E-001: Document Upload and Processing ===");

        // Upload security policy
        UUID policyId = uploadDocument(
            "test-documents/company-policy.md",
            "company-security-policy.md",
            "text/markdown",
            null  // No metadata for now
        );
        uploadedDocumentIds.add(policyId);
        System.out.println("✓ Uploaded security policy: " + policyId);

        // Upload product spec
        UUID specId = uploadDocument(
            "test-documents/product-specification.md",
            "cloudsync-specification.md",
            "text/markdown",
            null
        );
        uploadedDocumentIds.add(specId);
        System.out.println("✓ Uploaded product specification: " + specId);

        // Upload API docs
        UUID apiId = uploadDocument(
            "test-documents/api-documentation.md",
            "cloudsync-api-docs.md",
            "text/markdown",
            null
        );
        uploadedDocumentIds.add(apiId);
        System.out.println("✓ Uploaded API documentation: " + apiId);

        // Wait for processing
        System.out.println("\nWaiting for document processing...");
        waitForProcessing(policyId, "Security Policy");
        waitForProcessing(specId, "Product Spec");
        waitForProcessing(apiId, "API Docs");

        // Verify all processed successfully
        for (UUID docId : uploadedDocumentIds) {
            DocumentDto.DocumentResponse doc = getDocument(docId);
            assertThat(doc.processingStatus())
                .describedAs("Document %s should be processed", docId)
                .isEqualTo(Document.ProcessingStatus.COMPLETED);
            assertThat(doc.chunkCount())
                .describedAs("Document %s should have chunks", docId)
                .isGreaterThan(0);
            System.out.println("  ✓ Document " + docId + ": " + doc.chunkCount() + " chunks");
        }

        System.out.println("=== E2E-001 Complete ===\n");
    }

    @Test
    @Order(2)
    @DisplayName("E2E-002: Query RAG system with real-world questions")
    void testRagQueries() {
        System.out.println("\n=== E2E-002: RAG Query Processing ===");

        // Query 1: Security Policy
        System.out.println("\nQuery 1: Password requirements");
        RagResponse response1 = executeRagQuery(
            "What are the password requirements according to the security policy?"
        );
        assertThat(response1.answer).isNotEmpty();
        assertThat(response1.answer.toLowerCase()).containsAnyOf("12", "character", "password");
        assertThat(response1.sources).isNotEmpty();
        System.out.println("  ✓ Answer length: " + response1.answer.length() + " chars");
        System.out.println("  ✓ Sources: " + response1.sources.size());

        // Query 2: Technical Spec
        System.out.println("\nQuery 2: CloudSync architecture");
        RagResponse response2 = executeRagQuery(
            "What technologies are used in the CloudSync frontend?"
        );
        assertThat(response2.answer).isNotEmpty();
        assertThat(response2.sources).isNotEmpty();
        System.out.println("  ✓ Answer length: " + response2.answer.length() + " chars");
        System.out.println("  ✓ Sources: " + response2.sources.size());

        // Query 3: API Documentation
        System.out.println("\nQuery 3: API authentication");
        RagResponse response3 = executeRagQuery(
            "How do I authenticate with the CloudSync API?"
        );
        assertThat(response3.answer).isNotEmpty();
        assertThat(response3.sources).isNotEmpty();
        System.out.println("  ✓ Answer length: " + response3.answer.length() + " chars");
        System.out.println("  ✓ Sources: " + response3.sources.size());

        // Query 4: Cross-document
        System.out.println("\nQuery 4: Cross-document query");
        RagResponse response4 = executeRagQuery(
            "What encryption standards are used for data security?"
        );
        assertThat(response4.answer).isNotEmpty();
        assertThat(response4.answer.toLowerCase()).containsAnyOf("aes", "256", "tls", "encryption");
        assertThat(response4.sources).isNotEmpty();
        System.out.println("  ✓ Answer length: " + response4.answer.length() + " chars");
        System.out.println("  ✓ Sources: " + response4.sources.size());

        System.out.println("\n=== E2E-002 Complete ===\n");
    }

    @Test
    @Order(3)
    @DisplayName("E2E-003: Validate response quality and citations")
    void testResponseQuality() {
        System.out.println("\n=== E2E-003: Response Quality Validation ===");

        String[] factualQueries = {
            "What is the minimum password length?",
            "What encryption is required for data at rest?",
            "What is the CloudSync API rate limit for standard tier?"
        };

        String[] expectedFacts = {
            "12",
            "aes-256",
            "1,000"
        };

        for (int i = 0; i < factualQueries.length; i++) {
            System.out.println("\nQuery: " + factualQueries[i]);
            RagResponse response = executeRagQuery(factualQueries[i]);

            assertThat(response.answer)
                .describedAs("Answer should not be empty")
                .isNotEmpty();

            assertThat(response.answer.toLowerCase())
                .describedAs("Answer should contain fact: %s", expectedFacts[i])
                .contains(expectedFacts[i].toLowerCase());

            assertThat(response.sources)
                .describedAs("Should have source citations")
                .isNotEmpty();

            double topRelevance = response.sources.get(0).relevanceScore;
            assertThat(topRelevance)
                .describedAs("Top source should have good relevance")
                .isGreaterThan(0.5);

            System.out.println("  ✓ Fact verified: " + expectedFacts[i]);
            System.out.println("  ✓ Top relevance: " + String.format("%.3f", topRelevance));
        }

        System.out.println("\n=== E2E-003 Complete ===\n");
    }

    // Helper Methods

    private static UUID uploadDocument(String resourcePath, String filename,
                                       String contentType, Map<String, Object> metadata) throws Exception {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        byte[] content = Files.readAllBytes(new File(resource.getURI()).toPath());

        HttpHeaders headers = createAuthHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        });

        if (metadata != null) {
            body.add("metadata", objectMapper.writeValueAsString(metadata));
        }

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<DocumentDto.DocumentResponse> response = restTemplate.exchange(
            DOCUMENT_URL + "/documents/upload",
            HttpMethod.POST,
            request,
            DocumentDto.DocumentResponse.class
        );

        return response.getBody().id();
    }

    private static void waitForProcessing(UUID documentId, String name) {
        await(name + " processing")
            .atMost(Duration.ofMinutes(5))
            .pollInterval(Duration.ofSeconds(2))
            .until(() -> {
                try {
                    DocumentDto.DocumentResponse doc = getDocument(documentId);
                    Document.ProcessingStatus status = doc.processingStatus();

                    if (status == Document.ProcessingStatus.COMPLETED) {
                        System.out.print("✓");
                        return true;
                    } else if (status == Document.ProcessingStatus.FAILED) {
                        System.out.println("\n  ✗ Processing failed!");
                        return false;
                    } else {
                        System.out.print(".");
                        return false;
                    }
                } catch (Exception e) {
                    return false;
                }
            });
        System.out.println();
    }

    private static DocumentDto.DocumentResponse getDocument(UUID documentId) {
        HttpHeaders headers = createAuthHeaders();

        ResponseEntity<DocumentDto.DocumentResponse> response = restTemplate.exchange(
            DOCUMENT_URL + "/documents/" + documentId,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            DocumentDto.DocumentResponse.class
        );

        return response.getBody();
    }

    private static RagResponse executeRagQuery(String query) {
        HttpHeaders headers = createAuthHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("tenantId", tenantId.toString());
        requestBody.put("query", query);
        requestBody.put("includeContext", true);
        requestBody.put("topK", 5);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            CORE_URL + "/rag/query",
            HttpMethod.POST,
            request,
            String.class
        );

        try {
            JsonNode root = objectMapper.readTree(response.getBody());

            RagResponse ragResponse = new RagResponse();
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
            throw new RuntimeException("Failed to parse RAG response", e);
        }
    }

    private static HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.set("X-Tenant-ID", tenantId.toString());
        return headers;
    }

    // Inner Classes

    private static class RagResponse {
        String answer;
        List<SourceCitation> sources = new ArrayList<>();
    }

    private static class SourceCitation {
        String fileName;
        double relevanceScore;
        String excerpt;
    }
}

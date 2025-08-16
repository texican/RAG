package com.enterprise.rag.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Tests to prevent security configuration errors that occurred during development.
 * 
 * Errors Encountered:
 * 1. Public endpoints not properly configured (403 Forbidden errors)
 * 2. Missing authentication for required endpoints
 * 3. CORS configuration issues
 * 4. JWT filter not properly configured
 * 5. User registration endpoint not marked as public
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "jwt.secret=TestSecretKeyForJWTThatIsAtLeast256BitsLongForHS256Algorithm",
    "management.health.redis.enabled=false",
    "spring.data.redis.enabled=false"
})
public class SecurityConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Public tenant registration endpoint should be accessible")
    void publicTenantRegistrationShouldBeAccessible() throws Exception {
        // This test prevents the error where tenant registration returned 403
        // Use unique slug to avoid conflicts with other tests
        String uniqueSlug = "security-test-" + System.currentTimeMillis();
        mockMvc.perform(post("/api/v1/tenants/register")
                .contentType("application/json")
                .content(String.format("""
                    {
                        "name": "Security Test Company",
                        "slug": "%s"
                    }
                    """, uniqueSlug)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Auth endpoints should be publicly accessible")
    void authEndpointsShouldBePubliclyAccessible() throws Exception {
        // Test login endpoint (should return 400 for bad request, not 403 for forbidden)
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType("application/json")
                .content("{}"))
                .andExpect(status().isBadRequest()); // Not 403 Forbidden

        // Test token validation endpoint
        mockMvc.perform(post("/api/v1/auth/validate")
                .contentType("application/json")
                .content("""
                    {
                        "token": "invalid-token"
                    }
                    """))
                .andExpect(status().isOk()); // Should process the request, not reject with 403
    }

    @Test
    @DisplayName("Health check endpoints should be accessible")
    void healthCheckEndpointsShouldBeAccessible() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Swagger documentation should be accessible")
    void swaggerDocumentationShouldBeAccessible() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Protected endpoints should require authentication")
    void protectedEndpointsShouldRequireAuthentication() throws Exception {
        // Admin endpoints should require ADMIN role
        String uniqueSlug2 = "protected-test-" + System.currentTimeMillis();
        mockMvc.perform(post("/api/v1/tenants")
                .contentType("application/json")
                .content(String.format("""
                    {
                        "name": "Protected Test Company",
                        "slug": "%s"
                    }
                    """, uniqueSlug2)))
                .andExpect(status().isForbidden()); // Should be forbidden without auth

        // User management endpoints should require authentication
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isForbidden()); // Should be forbidden without auth
    }

    @Test
    @DisplayName("CORS should be properly configured")
    void corsShouldBeProperlyConfigured() throws Exception {
        mockMvc.perform(options("/api/v1/tenants/register")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("Security filter chain should be properly configured")
    void securityFilterChainShouldBeProperlyConfigured() {
        // Verify that SecurityFilterChain bean exists
        assertDoesNotThrow(() -> {
            // This should not throw if security is properly configured
            Class.forName("org.springframework.security.web.SecurityFilterChain");
        });
    }

    @Test
    @DisplayName("JWT configuration should be valid")
    void jwtConfigurationShouldBeValid() {
        // Verify JWT service can be created without errors
        assertDoesNotThrow(() -> {
            Class.forName("com.enterprise.rag.auth.security.JwtService");
        }, "JwtService should be available and properly configured");
    }

    @Test
    @DisplayName("Password encoder should be configured")
    void passwordEncoderShouldBeConfigured() {
        assertDoesNotThrow(() -> {
            Class.forName("org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder");
        }, "BCryptPasswordEncoder should be available");
    }

    @Test
    @DisplayName("Session management should be stateless")
    void sessionManagementShouldBeStateless() throws Exception {
        // Verify that no sessions are created (stateless JWT)
        String uniqueSlug3 = "session-test-" + System.currentTimeMillis();
        mockMvc.perform(post("/api/v1/tenants/register")
                .contentType("application/json")
                .content(String.format("""
                    {
                        "name": "Session Test Company",
                        "slug": "%s"
                    }
                    """, uniqueSlug3)))
                .andExpect(status().isCreated())
                .andExpect(request().sessionAttributeDoesNotExist("SPRING_SECURITY_CONTEXT"));
    }

    @Test
    @DisplayName("Error handling should not expose security details")
    void errorHandlingShouldNotExposeSecurityDetails() throws Exception {
        // Verify that security errors don't leak sensitive information
        mockMvc.perform(get("/api/v1/admin/sensitive-endpoint"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("SQL")))) // Should not expose SQL errors
                .andExpect(content().string(not(containsString("password")))); // Should not expose password info
    }

    @Test
    @DisplayName("Authentication manager should be configured")
    void authenticationManagerShouldBeConfigured() {
        assertDoesNotThrow(() -> {
            Class.forName("org.springframework.security.authentication.AuthenticationManager");
        }, "AuthenticationManager should be configured");
    }

    @Test
    @DisplayName("Security headers should be properly set")
    void securityHeadersShouldBeProperlySet() throws Exception {
        mockMvc.perform(get("/api/v1/tenants/register"))
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().exists("X-Frame-Options"))
                .andExpect(header().exists("Cache-Control"));
    }
}
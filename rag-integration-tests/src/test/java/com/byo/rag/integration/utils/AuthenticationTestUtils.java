package com.byo.rag.integration.utils;

import com.byo.rag.shared.dto.TenantDto;
import com.byo.rag.shared.dto.UserDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Specialized utility class for authentication operations in integration tests.
 * 
 * This class provides high-level methods for creating tenants, users, and
 * performing authentication flows needed for integration test setup.
 */
public final class AuthenticationTestUtils {
    
    private AuthenticationTestUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Create a tenant and return the response.
     */
    public static TenantDto.TenantResponse createTenant(
            RestTemplate restTemplate, 
            String baseUrl, 
            TenantDto.CreateTenantRequest request) {
        
        String url = baseUrl + "/api/auth/tenants/register";
        ResponseEntity<TenantDto.TenantResponse> response = IntegrationTestUtils.performPost(
            restTemplate, url, request, TenantDto.TenantResponse.class
        );
        
        IntegrationTestUtils.assertSuccessfulResponse(response);
        IntegrationTestUtils.assertResponseBodyNotEmpty(response);
        
        TenantDto.TenantResponse tenant = response.getBody();
        assertThat(tenant).isNotNull();
        assertThat(tenant.id()).isNotNull();
        assertThat(tenant.name()).isEqualTo(request.name());
        assertThat(tenant.slug()).isEqualTo(request.slug());
        
        return tenant;
    }

    /**
     * Create a user within a tenant and return the response.
     */
    public static UserDto.UserResponse createUser(
            RestTemplate restTemplate, 
            String baseUrl, 
            UserDto.CreateUserRequest request,
            String adminToken) {
        
        String url = baseUrl + "/api/auth/users";
        ResponseEntity<UserDto.UserResponse> response = IntegrationTestUtils.performAuthenticatedPost(
            restTemplate, url, request, adminToken, UserDto.UserResponse.class
        );
        
        IntegrationTestUtils.assertSuccessfulResponse(response);
        IntegrationTestUtils.assertResponseBodyNotEmpty(response);
        
        UserDto.UserResponse user = response.getBody();
        assertThat(user).isNotNull();
        assertThat(user.id()).isNotNull();
        assertThat(user.email()).isEqualTo(request.email());
        assertThat(user.firstName()).isEqualTo(request.firstName());
        assertThat(user.lastName()).isEqualTo(request.lastName());
        assertThat(user.role()).isEqualTo(request.role());
        
        return user;
    }

    /**
     * Perform login and return the authentication response.
     */
    public static UserDto.LoginResponse login(
            RestTemplate restTemplate, 
            String baseUrl, 
            UserDto.LoginRequest loginRequest) {
        
        String url = baseUrl + "/api/auth/login";
        ResponseEntity<UserDto.LoginResponse> response = IntegrationTestUtils.performPost(
            restTemplate, url, loginRequest, UserDto.LoginResponse.class
        );
        
        IntegrationTestUtils.assertSuccessfulResponse(response);
        IntegrationTestUtils.assertResponseBodyNotEmpty(response);
        
        UserDto.LoginResponse loginResponse = response.getBody();
        assertThat(loginResponse).isNotNull();
        assertThat(loginResponse.accessToken()).isNotNull().isNotEmpty();
        assertThat(loginResponse.refreshToken()).isNotNull().isNotEmpty();
        assertThat(loginResponse.expiresIn()).isPositive();
        assertThat(loginResponse.user()).isNotNull();
        
        return loginResponse;
    }

    /**
     * Create a complete test tenant with admin user and return authentication details.
     */
    public static TestTenantSetup createTestTenantWithAdmin(
            RestTemplate restTemplate, 
            String baseUrl, 
            TenantDto.CreateTenantRequest tenantRequest,
            UserDto.CreateUserRequest adminRequest) {
        
        // Step 1: Create tenant
        TenantDto.TenantResponse tenant = createTenant(restTemplate, baseUrl, tenantRequest);
        
        // Step 2: Default admin user should be created automatically
        // Try to login with default admin credentials first
        UserDto.LoginRequest defaultAdminLogin = new UserDto.LoginRequest(
            "admin@enterprise-rag.com", 
            "admin123"
        );
        
        try {
            UserDto.LoginResponse adminAuth = login(restTemplate, baseUrl, defaultAdminLogin);
            
            return new TestTenantSetup(
                tenant,
                adminAuth.user(),
                adminAuth.accessToken(),
                adminAuth.refreshToken()
            );
        } catch (Exception e) {
            // If default admin doesn't work, we might need to create one
            // This depends on the authentication service implementation
            throw new RuntimeException("Failed to authenticate with default admin credentials", e);
        }
    }

    /**
     * Create a standard user within a tenant (requires admin token).
     */
    public static UserDto.UserResponse createStandardUser(
            RestTemplate restTemplate, 
            String baseUrl, 
            UUID tenantId,
            String email,
            String firstName,
            String lastName,
            String adminToken) {
        
        UserDto.CreateUserRequest userRequest = new UserDto.CreateUserRequest(
            firstName,
            lastName,
            email,
            "StandardPassword123!",
            com.byo.rag.shared.entity.User.UserRole.USER,
            tenantId
        );
        
        return createUser(restTemplate, baseUrl, userRequest, adminToken);
    }

    /**
     * Validate that a JWT token is properly formatted.
     */
    public static void validateJwtToken(String token) {
        assertThat(token).isNotNull().isNotEmpty();
        
        // JWT tokens should have 3 parts separated by dots
        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3);
        
        // Each part should be base64 encoded and not empty
        for (String part : parts) {
            assertThat(part).isNotEmpty();
        }
    }

    /**
     * Check if an authentication token is still valid by making a test API call.
     */
    public static boolean isTokenValid(RestTemplate restTemplate, String baseUrl, String token) {
        try {
            String url = baseUrl + "/api/auth/profile";
            ResponseEntity<UserDto.UserResponse> response = IntegrationTestUtils.performAuthenticatedGet(
                restTemplate, url, token, UserDto.UserResponse.class
            );
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Data class to hold complete tenant setup information for testing.
     */
    public static record TestTenantSetup(
        TenantDto.TenantResponse tenant,
        UserDto.UserResponse adminUser,
        String adminToken,
        String adminRefreshToken
    ) {
        public UUID getTenantId() {
            return tenant.id();
        }
        
        public UUID getAdminUserId() {
            return adminUser.id();
        }
        
        public String getTenantSlug() {
            return tenant.slug();
        }
    }

    /**
     * Refresh an authentication token.
     */
    public static UserDto.LoginResponse refreshToken(
            RestTemplate restTemplate, 
            String baseUrl, 
            String refreshToken) {
        
        String url = baseUrl + "/api/auth/refresh";
        // Assuming the refresh endpoint expects the refresh token in the body
        // This might need to be adjusted based on actual API implementation
        
        ResponseEntity<UserDto.LoginResponse> response = IntegrationTestUtils.performPost(
            restTemplate, url, new RefreshTokenRequest(refreshToken), UserDto.LoginResponse.class
        );
        
        IntegrationTestUtils.assertSuccessfulResponse(response);
        return response.getBody();
    }

    /**
     * Helper record for refresh token requests.
     */
    private static record RefreshTokenRequest(String refreshToken) {}
}
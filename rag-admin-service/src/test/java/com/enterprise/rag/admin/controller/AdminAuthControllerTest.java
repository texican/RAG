package com.enterprise.rag.admin.controller;

import com.enterprise.rag.admin.dto.AdminLoginRequest;
import com.enterprise.rag.admin.dto.AdminLoginResponse;
import com.enterprise.rag.admin.dto.AdminRefreshRequest;
import com.enterprise.rag.admin.dto.AdminUserValidationResponse;
import com.enterprise.rag.admin.dto.LogoutResponse;
import com.enterprise.rag.admin.service.AdminJwtService;
import com.enterprise.rag.admin.repository.UserRepository;
import com.enterprise.rag.shared.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAuthController Tests")
class AdminAuthControllerTest {

    @Mock
    private AdminJwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AdminAuthController controller;

    private final String validUsername = "admin@enterprise.com";
    private final String validPassword = "AdminPassword123!";
    private final String validToken = "valid.jwt.token";
    private final List<String> adminRoles = List.of("SUPER_ADMIN");

    @BeforeEach
    void setUp() {
        reset(jwtService, passwordEncoder, userRepository, authentication);
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void shouldLoginSuccessfullyWithValidCredentials() {
        // Given
        AdminLoginRequest request = new AdminLoginRequest(validUsername, validPassword);
        User validUser = createValidAdminUser();
        when(userRepository.findByEmail(validUsername)).thenReturn(Optional.of(validUser));
        when(passwordEncoder.matches(validPassword, encodedPassword())).thenReturn(true);
        when(jwtService.generateToken(validUsername, List.of("ADMIN"))).thenReturn(validToken);

        // When
        ResponseEntity<?> response = controller.login(request);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        AdminLoginResponse loginResponse = (AdminLoginResponse) response.getBody();
        assertNotNull(loginResponse);
        assertEquals(validToken, loginResponse.token());
        assertEquals(validUsername, loginResponse.username());
        assertEquals(List.of("ADMIN"), loginResponse.roles());
        assertEquals(86400000L, loginResponse.expiresIn());

        verify(userRepository).findByEmail(validUsername);
        verify(passwordEncoder).matches(validPassword, encodedPassword());
        verify(jwtService).generateToken(validUsername, List.of("ADMIN"));
    }

    @Test
    @DisplayName("Should return 401 with invalid credentials")
    void shouldReturn401WithInvalidCredentials() {
        // Given
        AdminLoginRequest request = new AdminLoginRequest(validUsername, "WrongPassword");
        User validUser = createValidAdminUser();
        when(userRepository.findByEmail(validUsername)).thenReturn(Optional.of(validUser));
        when(passwordEncoder.matches("WrongPassword", encodedPassword())).thenReturn(false);

        // When
        ResponseEntity<?> response = controller.login(request);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, String> errorResponse = (Map<String, String>) response.getBody();
        assertNotNull(errorResponse);
        assertEquals("Invalid credentials", errorResponse.get("error"));
        assertNotNull(errorResponse.get("message"));

        verify(userRepository).findByEmail(validUsername);
        verify(passwordEncoder).matches("WrongPassword", encodedPassword());
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Should return 401 with non-existent username")
    void shouldReturn401WithNonExistentUsername() {
        // Given
        String nonExistentUser = "nonexistent@enterprise.com";
        AdminLoginRequest request = new AdminLoginRequest(nonExistentUser, validPassword);
        when(userRepository.findByEmail(nonExistentUser)).thenReturn(Optional.empty());

        // When
        ResponseEntity<?> response = controller.login(request);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, String> errorResponse = (Map<String, String>) response.getBody();
        assertNotNull(errorResponse);
        assertEquals("Invalid credentials", errorResponse.get("error"));

        verify(userRepository).findByEmail(nonExistentUser);
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    @DisplayName("Should refresh token successfully with valid token")
    void shouldRefreshTokenSuccessfullyWithValidToken() {
        // Given
        String newToken = "new.jwt.token";
        List<String> roles = List.of("ADMIN");
        AdminRefreshRequest request = new AdminRefreshRequest(validToken);
        when(jwtService.isTokenValid(validToken)).thenReturn(true);
        when(jwtService.extractUsername(validToken)).thenReturn(validUsername);
        when(jwtService.extractRoles(validToken)).thenReturn(roles);
        when(jwtService.generateToken(validUsername, roles)).thenReturn(newToken);

        // When
        ResponseEntity<?> response = controller.refresh(request);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        AdminLoginResponse refreshResponse = (AdminLoginResponse) response.getBody();
        assertNotNull(refreshResponse);
        assertEquals(newToken, refreshResponse.token());
        assertEquals(validUsername, refreshResponse.username());
        assertEquals(roles, refreshResponse.roles());

        verify(jwtService).isTokenValid(validToken);
        verify(jwtService).extractUsername(validToken);
        verify(jwtService).extractRoles(validToken);
        verify(jwtService).generateToken(validUsername, roles);
    }

    @Test
    @DisplayName("Should return 401 with invalid refresh token")
    void shouldReturn401WithInvalidRefreshToken() {
        // Given
        String invalidToken = "invalid.jwt.token";
        AdminRefreshRequest request = new AdminRefreshRequest(invalidToken);
        when(jwtService.isTokenValid(invalidToken)).thenReturn(false);

        // When
        ResponseEntity<?> response = controller.refresh(request);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, String> errorResponse = (Map<String, String>) response.getBody();
        assertNotNull(errorResponse);
        assertEquals("Invalid token", errorResponse.get("error"));

        verify(jwtService).isTokenValid(invalidToken);
        verify(jwtService, never()).extractUsername(anyString());
        verify(jwtService, never()).generateToken(anyString(), anyList());
    }

    @Test
    @DisplayName("Should logout successfully")
    void shouldLogoutSuccessfully() {
        // When
        ResponseEntity<LogoutResponse> response = controller.logout();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        LogoutResponse logoutResponse = response.getBody();
        assertNotNull(logoutResponse);
        assertEquals("Logged out successfully", logoutResponse.message());
    }

    @Test
    @DisplayName("Should validate admin user exists")
    void shouldValidateAdminUserExists() {
        // Given
        User validUser = createValidAdminUser();
        when(userRepository.findByEmail(validUsername)).thenReturn(Optional.of(validUser));
        
        // When
        ResponseEntity<AdminUserValidationResponse> response = controller.validateUser(validUsername);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        AdminUserValidationResponse validationResponse = response.getBody();
        assertNotNull(validationResponse);
        assertTrue(validationResponse.exists());
        assertEquals(validUsername, validationResponse.username());
        
        verify(userRepository).findByEmail(validUsername);
    }

    @Test
    @DisplayName("Should return false for non-existent admin user")
    void shouldReturnFalseForNonExistentAdminUser() {
        // Given
        String nonExistentUser = "nonexistent@enterprise.com";
        when(userRepository.findByEmail(nonExistentUser)).thenReturn(Optional.empty());

        // When
        ResponseEntity<AdminUserValidationResponse> response = controller.validateUser(nonExistentUser);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        AdminUserValidationResponse validationResponse = response.getBody();
        assertNotNull(validationResponse);
        assertFalse(validationResponse.exists());
        assertEquals(nonExistentUser, validationResponse.username());
        
        verify(userRepository).findByEmail(nonExistentUser);
    }

    @Test
    @DisplayName("Should return current admin user info")
    @SuppressWarnings("unchecked")
    void shouldReturnCurrentAdminUserInfo() {
        // Given
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(validUsername);
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        doReturn(authorities).when(authentication).getAuthorities();

        // When
        ResponseEntity<?> response = controller.getCurrentUser(authentication);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> userInfo = (Map<String, Object>) response.getBody();
        assertNotNull(userInfo);
        assertEquals(validUsername, userInfo.get("username"));
        
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) userInfo.get("roles");
        assertNotNull(roles);
        assertEquals(1, roles.size());
        assertEquals("SUPER_ADMIN", roles.get(0));
    }

    @Test
    @DisplayName("Should return 401 for unauthenticated user info request")
    void shouldReturn401ForUnauthenticatedUserInfoRequest() {
        // Given
        when(authentication.isAuthenticated()).thenReturn(false);

        // When
        ResponseEntity<?> response = controller.getCurrentUser(authentication);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, String> errorResponse = (Map<String, String>) response.getBody();
        assertNotNull(errorResponse);
        assertEquals("Unauthorized", errorResponse.get("error"));
    }

    @Test
    @DisplayName("Should return 401 for null authentication")
    void shouldReturn401ForNullAuthentication() {
        // When
        ResponseEntity<?> response = controller.getCurrentUser(null);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, String> errorResponse = (Map<String, String>) response.getBody();
        assertNotNull(errorResponse);
        assertEquals("Unauthorized", errorResponse.get("error"));
    }

    // Helper method to simulate encoded password
    private String encodedPassword() {
        return "$2a$10$KBdADFHGKGYwIjnfh56vq.i2AcnMUAdYgkEfnoqJxUr1vBD8AWODm";
    }
    
    // Helper method to create a valid admin user
    private User createValidAdminUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(validUsername);
        user.setPasswordHash(encodedPassword());
        user.setRole(User.UserRole.ADMIN);
        user.setStatus(User.UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }
}
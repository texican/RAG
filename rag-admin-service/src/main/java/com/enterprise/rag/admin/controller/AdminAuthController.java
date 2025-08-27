package com.enterprise.rag.admin.controller;

import com.enterprise.rag.admin.dto.AdminLoginRequest;
import com.enterprise.rag.admin.dto.AdminLoginResponse;
import com.enterprise.rag.admin.dto.AdminRefreshRequest;
import com.enterprise.rag.admin.dto.AdminUserValidationResponse;
import com.enterprise.rag.admin.dto.LogoutResponse;
import com.enterprise.rag.admin.service.AdminJwtService;
import com.enterprise.rag.shared.entity.User;
import com.enterprise.rag.admin.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@Tag(name = "Admin Authentication", description = "Admin authentication and authorization endpoints")
@Validated
public class AdminAuthController {

    private static final Logger logger = LoggerFactory.getLogger(AdminAuthController.class);

    private final AdminJwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public AdminAuthController(AdminJwtService jwtService, PasswordEncoder passwordEncoder, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    @Operation(summary = "Admin login", description = "Authenticate admin user and return JWT token")
    public ResponseEntity<?> login(@Valid @RequestBody AdminLoginRequest request) {
        try {
            logger.debug("Admin login attempt for username: {}", request.username());
            
            // Find user in database
            Optional<User> userOpt = userRepository.findByEmail(request.username());
            if (userOpt.isEmpty()) {
                logger.warn("User not found for username: {}", request.username());
                return ResponseEntity.status(401)
                        .body(Map.of(
                                "error", "Invalid credentials",
                                "message", "Username or password is incorrect"
                        ));
            }
            
            User user = userOpt.get();
            
            // Validate password
            boolean passwordMatches = passwordEncoder.matches(request.password(), user.getPasswordHash());
            
            if (!passwordMatches) {
                logger.warn("Invalid password for username: {}", request.username());
                return ResponseEntity.status(401)
                        .body(Map.of(
                                "error", "Invalid credentials",
                                "message", "Username or password is incorrect"
                        ));
            }
            
            // Check if user is active and has admin role
            if (!user.getStatus().name().equals("ACTIVE")) {
                logger.warn("Inactive user login attempt: {}", request.username());
                return ResponseEntity.status(401)
                        .body(Map.of(
                                "error", "Account disabled",
                                "message", "Your account is not active"
                        ));
            }
            
            if (!user.getRole().name().equals("ADMIN")) {
                logger.warn("Non-admin user login attempt: {}", request.username());
                return ResponseEntity.status(403)
                        .body(Map.of(
                                "error", "Access denied",
                                "message", "Admin access required"
                        ));
            }

            // Generate JWT token
            List<String> roles = List.of("ADMIN");
            String token = jwtService.generateToken(request.username(), roles);
            
            logger.info("Admin user successfully authenticated: {}", request.username());
            
            AdminLoginResponse response = new AdminLoginResponse(
                    token,
                    request.username(),
                    roles,
                    86400000L // 24 hours in milliseconds
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error during admin login", e);
            return ResponseEntity.status(500)
                    .body(Map.of(
                            "error", "Internal server error",
                            "message", "An error occurred during authentication"
                    ));
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh JWT token", description = "Refresh an existing JWT token")
    public ResponseEntity<?> refresh(@Valid @RequestBody AdminRefreshRequest request) {
        try {
            logger.debug("Token refresh attempt");
            
            if (!jwtService.isTokenValid(request.token())) {
                logger.warn("Invalid token provided for refresh");
                return ResponseEntity.status(401)
                        .body(Map.of(
                                "error", "Invalid token",
                                "message", "Token is invalid or expired"
                        ));
            }

            String username = jwtService.extractUsername(request.token());
            List<String> roles = jwtService.extractRoles(request.token());
            
            String newToken = jwtService.generateToken(username, roles);
            
            logger.info("Token refreshed successfully for user: {}", username);
            
            AdminLoginResponse response = new AdminLoginResponse(
                    newToken,
                    username,
                    roles,
                    86400000L
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error during token refresh", e);
            return ResponseEntity.status(500)
                    .body(Map.of(
                            "error", "Internal server error",
                            "message", "An error occurred during token refresh"
                    ));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Admin logout", description = "Logout admin user")
    public ResponseEntity<LogoutResponse> logout() {
        logger.info("Admin user logged out");
        return ResponseEntity.ok(new LogoutResponse("Logged out successfully"));
    }

    @GetMapping("/validate")
    @Operation(summary = "Validate admin user", description = "Check if admin user exists")
    public ResponseEntity<AdminUserValidationResponse> validateUser(@RequestParam String username) {
        Optional<User> userOpt = userRepository.findByEmail(username);
        boolean exists = userOpt.isPresent() && 
                         userOpt.get().getRole().name().equals("ADMIN") &&
                         userOpt.get().getStatus().name().equals("ACTIVE");
        logger.debug("Admin user validation for {}: {}", username, exists);
        
        return ResponseEntity.ok(new AdminUserValidationResponse(exists, username));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current admin user", description = "Get current authenticated admin user information")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(Map.of(
                            "error", "Unauthorized",
                            "message", "Authentication required"
                    ));
        }

        String username = authentication.getName();
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role)
                .toList();

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("username", username);
        userInfo.put("roles", roles);
        
        logger.debug("Current admin user info requested: {}", username);
        
        return ResponseEntity.ok(userInfo);
    }
}
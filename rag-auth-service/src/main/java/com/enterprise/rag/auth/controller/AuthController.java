package com.enterprise.rag.auth.controller;

import com.enterprise.rag.auth.service.AuthService;
import com.enterprise.rag.auth.service.UserService;
import com.enterprise.rag.shared.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication and token management")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT tokens")
    public ResponseEntity<UserDto.LoginResponse> login(@Valid @RequestBody UserDto.LoginRequest request) {
        UserDto.LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Generate new access token using refresh token")
    public ResponseEntity<UserDto.LoginResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        UserDto.LoginResponse response = authService.refreshToken(request.refreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate token", description = "Validate JWT token")
    public ResponseEntity<TokenValidationResponse> validateToken(@RequestBody ValidateTokenRequest request) {
        boolean isValid = authService.validateToken(request.token());
        return ResponseEntity.ok(new TokenValidationResponse(isValid));
    }

    @PostMapping("/register")
    @Operation(summary = "User registration", description = "Register a new user (public endpoint)")
    public ResponseEntity<UserDto.UserResponse> register(@Valid @RequestBody UserDto.CreateUserRequest request) {
        UserDto.UserResponse response = userService.createUser(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email", description = "Verify user email with token")
    public ResponseEntity<UserDto.UserResponse> verifyEmail(@RequestBody EmailVerificationRequest request) {
        UserDto.UserResponse response = userService.verifyEmail(request.token());
        return ResponseEntity.ok(response);
    }

    public record RefreshTokenRequest(String refreshToken) {}
    public record ValidateTokenRequest(String token) {}
    public record TokenValidationResponse(boolean valid) {}
    public record EmailVerificationRequest(String token) {}
}
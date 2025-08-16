package com.enterprise.rag.auth.controller;

import com.enterprise.rag.auth.security.JwtAuthenticationFilter;
import com.enterprise.rag.auth.service.UserService;
import com.enterprise.rag.shared.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "User CRUD operations")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create user", description = "Create a new user (admin only)")
    public ResponseEntity<UserDto.UserResponse> createUser(@Valid @RequestBody UserDto.CreateUserRequest request) {
        UserDto.UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Get current authenticated user information")
    public ResponseEntity<UserDto.UserResponse> getCurrentUser(
            @AuthenticationPrincipal JwtAuthenticationFilter.RagUserPrincipal principal) {
        UserDto.UserResponse response = userService.getUser(principal.userId(), principal.tenantId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user", description = "Update current authenticated user information")
    public ResponseEntity<UserDto.UserResponse> updateCurrentUser(
            @Valid @RequestBody UserDto.UpdateUserRequest request,
            @AuthenticationPrincipal JwtAuthenticationFilter.RagUserPrincipal principal) {
        UserDto.UserResponse response = userService.updateUser(principal.userId(), request, principal.tenantId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @userService.getUser(#userId, authentication.principal.tenantId).tenant.id == authentication.principal.tenantId")
    @Operation(summary = "Get user by ID", description = "Retrieve user information")
    public ResponseEntity<UserDto.UserResponse> getUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal JwtAuthenticationFilter.RagUserPrincipal principal) {
        UserDto.UserResponse response = userService.getUser(userId, principal.tenantId());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    @Operation(summary = "List users", description = "List users in tenant")
    public ResponseEntity<Page<UserDto.UserSummary>> getUsers(
            Pageable pageable,
            @AuthenticationPrincipal JwtAuthenticationFilter.RagUserPrincipal principal) {
        Page<UserDto.UserSummary> response = userService.getUsersByTenant(principal.tenantId(), pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user", description = "Update user information (admin only)")
    public ResponseEntity<UserDto.UserResponse> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UserDto.UpdateUserRequest request,
            @AuthenticationPrincipal JwtAuthenticationFilter.RagUserPrincipal principal) {
        UserDto.UserResponse response = userService.updateUser(userId, request, principal.tenantId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user", description = "Delete user (admin only)")
    public ResponseEntity<Void> deleteUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal JwtAuthenticationFilter.RagUserPrincipal principal) {
        userService.deleteUser(userId, principal.tenantId());
        return ResponseEntity.noContent().build();
    }
}
package com.enterprise.rag.shared.dto;

import com.enterprise.rag.shared.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public sealed interface UserDto permits 
    UserDto.CreateUserRequest,
    UserDto.UpdateUserRequest,
    UserDto.UserResponse,
    UserDto.UserSummary,
    UserDto.LoginRequest,
    UserDto.LoginResponse {

    record CreateUserRequest(
        @NotBlank @Size(min = 2, max = 100) String firstName,
        @NotBlank @Size(min = 2, max = 100) String lastName,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password,
        User.UserRole role,
        UUID tenantId
    ) implements UserDto {}

    record UpdateUserRequest(
        @Size(min = 2, max = 100) String firstName,
        @Size(min = 2, max = 100) String lastName,
        @Email String email,
        User.UserRole role,
        User.UserStatus status
    ) implements UserDto {}

    record UserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        User.UserRole role,
        User.UserStatus status,
        Boolean emailVerified,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        TenantDto.TenantSummary tenant
    ) implements UserDto {}

    record UserSummary(
        UUID id,
        String firstName,
        String lastName,
        String email,
        User.UserRole role,
        User.UserStatus status
    ) implements UserDto {}

    record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
    ) implements UserDto {}

    record LoginResponse(
        String accessToken,
        String refreshToken,
        Long expiresIn,
        UserResponse user
    ) implements UserDto {}
}
package com.enterprise.rag.admin.dto;

import java.util.List;

public record AdminLoginResponse(
        String token,
        String username,
        List<String> roles,
        Long expiresIn
) {
}
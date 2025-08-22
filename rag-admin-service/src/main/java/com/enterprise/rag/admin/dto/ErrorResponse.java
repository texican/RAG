package com.enterprise.rag.admin.dto;

import java.time.LocalDateTime;

public record ErrorResponse(
        String error,
        String message,
        LocalDateTime timestamp,
        String path
) {
    public ErrorResponse(String error, String message, String path) {
        this(error, message, LocalDateTime.now(), path);
    }
}
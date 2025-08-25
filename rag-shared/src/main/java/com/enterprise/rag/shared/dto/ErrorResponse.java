package com.enterprise.rag.shared.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard error response DTO for API operations.
 */
public record ErrorResponse(
    String message,
    String errorCode,
    LocalDateTime timestamp,
    String path,
    Map<String, Object> details
) {
    
    public ErrorResponse(String message, String errorCode) {
        this(message, errorCode, LocalDateTime.now(), null, null);
    }
    
    public ErrorResponse(String message, String errorCode, String path) {
        this(message, errorCode, LocalDateTime.now(), path, null);
    }
    
    public static ErrorResponse of(String message, String errorCode) {
        return new ErrorResponse(message, errorCode);
    }
    
    public static ErrorResponse of(String message, String errorCode, String path) {
        return new ErrorResponse(message, errorCode, path);
    }
    
    public ErrorResponse withDetails(Map<String, Object> details) {
        return new ErrorResponse(message, errorCode, timestamp, path, details);
    }
}
package com.byo.rag.admin.dto;

import java.time.LocalDateTime;

/**
 * Standardized error response data transfer object for consistent API error handling.
 * 
 * <p>This record provides a uniform structure for all error responses in the admin service,
 * ensuring consistent error reporting across all endpoints. It includes contextual information
 * for debugging and user-friendly error messages for client applications.</p>
 * 
 * <p>Error response features:</p>
 * <ul>
 *   <li>Standardized error categorization and classification</li>
 *   <li>Human-readable messages for user interface display</li>
 *   <li>Temporal context with automatic timestamp generation</li>
 *   <li>Request path tracking for debugging and logging</li>
 * </ul>
 * 
 * <p>Common error scenarios:</p>
 * <ul>
 *   <li>Validation failures with detailed field-level messages</li>
 *   <li>Authentication and authorization errors</li>
 *   <li>Resource not found conditions</li>
 *   <li>Business logic violations and constraint errors</li>
 *   <li>System-level errors and service unavailability</li>
 * </ul>
 * 
 * @param error error type or category identifier
 * @param message descriptive error message for client consumption
 * @param timestamp when the error occurred (automatically generated if not specified)
 * @param path the request path where the error originated
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.0
 * @see com.byo.rag.admin.exception.GlobalExceptionHandler
 */
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
package com.byo.rag.shared.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standardized error response DTO for consistent error handling across the Enterprise RAG System.
 * <p>
 * This record provides a uniform error response structure used by all microservices
 * to ensure consistent error reporting, debugging capabilities, and client integration.
 * It supports hierarchical error codes, detailed error information, and request tracing.
 * </p>
 * 
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Consistent Structure</strong> - Uniform error format across all REST endpoints</li>
 *   <li><strong>Hierarchical Error Codes</strong> - Structured error classification (e.g., TENANT_001, AUTH_403)</li>
 *   <li><strong>Request Tracing</strong> - Path and timestamp information for debugging</li>
 *   <li><strong>Extensible Details</strong> - Additional context through details map</li>
 *   <li><strong>Convenience Methods</strong> - Factory methods and fluent API for easy construction</li>
 * </ul>
 * 
 * <h3>Error Code Conventions:</h3>
 * <ul>
 *   <li><strong>AUTH_xxx</strong> - Authentication and authorization errors</li>
 *   <li><strong>TENANT_xxx</strong> - Multi-tenant related errors</li>
 *   <li><strong>DOC_xxx</strong> - Document processing and storage errors</li>
 *   <li><strong>EMBED_xxx</strong> - Embedding and vector operation errors</li>
 *   <li><strong>RAG_xxx</strong> - RAG query and response generation errors</li>
 *   <li><strong>VALID_xxx</strong> - Validation and constraint errors</li>
 * </ul>
 * 
 * <h3>Usage Examples:</h3>
 * <pre>{@code
 * // Simple error
 * return ErrorResponse.of("User not found", "AUTH_404");
 * 
 * // Error with request path
 * return ErrorResponse.of("Invalid tenant", "TENANT_001", "/api/v1/tenants/invalid-slug");
 * 
 * // Error with additional details
 * return ErrorResponse.of("Validation failed", "VALID_001")
 *     .withDetails(Map.of(
 *         "field", "email",
 *         "constraint", "must be valid email format",
 *         "value", "invalid-email"
 *     ));
 * 
 * // Full error response
 * return new ErrorResponse(
 *     "Document processing failed",
 *     "DOC_500",
 *     LocalDateTime.now(),
 *     "/api/v1/documents/upload",
 *     Map.of("fileSize", "50MB", "maxSize", "25MB", "format", "PDF")
 * );
 * }</pre>
 * 
 * @param message human-readable error description
 * @param errorCode structured error code for programmatic handling
 * @param timestamp when the error occurred (auto-generated if not provided)
 * @param path API endpoint path where error occurred (if available)
 * @param details additional error context and debugging information
 * 
 * @author Enterprise RAG Development Team
 * @since 1.0.0
 * @see org.springframework.web.bind.annotation.ExceptionHandler
 */
public record ErrorResponse(
    String message,
    String errorCode,
    LocalDateTime timestamp,
    String path,
    Map<String, Object> details
) {
    
    /**
     * Convenience constructor for basic error response.
     * Sets timestamp to current time, path and details to null.
     * 
     * @param message human-readable error description
     * @param errorCode structured error code
     */
    public ErrorResponse(String message, String errorCode) {
        this(message, errorCode, LocalDateTime.now(), null, null);
    }
    
    /**
     * Convenience constructor for error response with request path.
     * Sets timestamp to current time, details to null.
     * 
     * @param message human-readable error description
     * @param errorCode structured error code
     * @param path API endpoint path where error occurred
     */
    public ErrorResponse(String message, String errorCode, String path) {
        this(message, errorCode, LocalDateTime.now(), path, null);
    }
    
    /**
     * Factory method for creating basic error response.
     * 
     * @param message human-readable error description
     * @param errorCode structured error code
     * @return new ErrorResponse instance
     */
    public static ErrorResponse of(String message, String errorCode) {
        return new ErrorResponse(message, errorCode);
    }
    
    /**
     * Factory method for creating error response with request path.
     * 
     * @param message human-readable error description
     * @param errorCode structured error code
     * @param path API endpoint path where error occurred
     * @return new ErrorResponse instance
     */
    public static ErrorResponse of(String message, String errorCode, String path) {
        return new ErrorResponse(message, errorCode, path);
    }
    
    /**
     * Creates a new ErrorResponse with additional details.
     * This is a fluent API method for adding contextual information.
     * 
     * @param details additional error context and debugging information
     * @return new ErrorResponse instance with details added
     */
    public ErrorResponse withDetails(Map<String, Object> details) {
        return new ErrorResponse(message, errorCode, timestamp, path, details);
    }
}
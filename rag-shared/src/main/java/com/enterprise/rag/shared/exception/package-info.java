/**
 * Standardized exception hierarchy for error handling.
 * 
 * <p>This package contains a comprehensive exception hierarchy that provides
 * consistent error handling across all microservices in the Enterprise RAG
 * System. All exceptions include rich context information, proper error codes,
 * and internationalization support for user-facing error messages.</p>
 * 
 * <h2>Exception Hierarchy</h2>
 * <p>The exception hierarchy follows Spring Boot patterns:</p>
 * <ul>
 *   <li><strong>Base Exceptions</strong> - Root exception classes with common functionality</li>
 *   <li><strong>Business Exceptions</strong> - Domain-specific exceptions for business logic errors</li>
 *   <li><strong>Security Exceptions</strong> - Authentication and authorization related errors</li>
 *   <li><strong>Validation Exceptions</strong> - Input validation and constraint violation errors</li>
 *   <li><strong>Integration Exceptions</strong> - External service and dependency errors</li>
 *   <li><strong>System Exceptions</strong> - Infrastructure and configuration errors</li>
 * </ul>
 * 
 * <h2>Error Context and Metadata</h2>
 * <p>All exceptions include comprehensive context information:</p>
 * <ul>
 *   <li><strong>Error Codes</strong> - Unique error codes for programmatic error handling</li>
 *   <li><strong>Tenant Context</strong> - Tenant ID and context information for debugging</li>
 *   <li><strong>User Context</strong> - User ID and role information (when applicable)</li>
 *   <li><strong>Request Context</strong> - HTTP request details and correlation IDs</li>
 *   <li><strong>Timestamp Information</strong> - Precise error occurrence timestamps</li>
 *   <li><strong>Stack Trace Context</strong> - Enhanced stack traces with business context</li>
 * </ul>
 * 
 * <h2>Multi-Tenant Error Handling</h2>
 * <p>Exception handling supports multi-tenant architecture:</p>
 * <ul>
 *   <li><strong>Tenant Isolation</strong> - No cross-tenant error information exposure</li>
 *   <li><strong>Context Validation</strong> - Automatic tenant context validation in exceptions</li>
 *   <li><strong>Audit Integration</strong> - Tenant-aware error logging and audit trails</li>
 *   <li><strong>Resource Limits</strong> - Per-tenant error rate limiting and throttling</li>
 * </ul>
 * 
 * <h2>Internationalization Support</h2>
 * <p>Error messages support multiple languages:</p>
 * <ul>
 *   <li><strong>Message Codes</strong> - Structured message codes for translation lookup</li>
 *   <li><strong>Parameter Substitution</strong> - Dynamic parameter substitution in messages</li>
 *   <li><strong>Locale Detection</strong> - Automatic locale detection from request headers</li>
 *   <li><strong>Fallback Strategy</strong> - Graceful fallback to default language</li>
 * </ul>
 * 
 * <h2>Security Considerations</h2>
 * <p>Exception handling includes security best practices:</p>
 * <ul>
 *   <li><strong>Information Disclosure</strong> - Careful control of error information exposure</li>
 *   <li><strong>Sanitization</strong> - Automatic sanitization of sensitive data in error messages</li>
 *   <li><strong>Audit Logging</strong> - Security-relevant errors logged for monitoring</li>
 *   <li><strong>Rate Limiting</strong> - Protection against error-based attacks</li>
 * </ul>
 * 
 * <h2>HTTP Status Code Mapping</h2>
 * <p>Exceptions map to appropriate HTTP status codes:</p>
 * <ul>
 *   <li><strong>400 Bad Request</strong> - Validation exceptions and malformed requests</li>
 *   <li><strong>401 Unauthorized</strong> - Authentication failures and invalid tokens</li>
 *   <li><strong>403 Forbidden</strong> - Authorization failures and access denied</li>
 *   <li><strong>404 Not Found</strong> - Resource not found or tenant isolation violations</li>
 *   <li><strong>409 Conflict</strong> - Business rule violations and constraint conflicts</li>
 *   <li><strong>422 Unprocessable Entity</strong> - Business logic validation failures</li>
 *   <li><strong>429 Too Many Requests</strong> - Rate limiting and quota exceeded</li>
 *   <li><strong>500 Internal Server Error</strong> - System errors and unexpected failures</li>
 *   <li><strong>502 Bad Gateway</strong> - External service failures and integration errors</li>
 *   <li><strong>503 Service Unavailable</strong> - Circuit breaker and service degradation</li>
 * </ul>
 * 
 * <h2>Error Response Format</h2>
 * <p>Standardized error response structure:</p>
 * <pre>{@code
 * {
 *   "error": {
 *     "code": "DOCUMENT_NOT_FOUND",
 *     "message": "Document with ID 12345 not found",
 *     "details": "The requested document does not exist or you don't have access to it",
 *     "timestamp": "2024-01-15T10:30:00.000Z",
 *     "path": "/api/v1/documents/12345",
 *     "correlationId": "abc123-def456-ghi789",
 *     "validationErrors": [
 *       {
 *         "field": "documentId",
 *         "message": "Document ID must be a valid UUID",
 *         "rejectedValue": "invalid-id"
 *       }
 *     ]
 *   }
 * }
 * }</pre>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Service
 * public class DocumentService {
 *     
 *     public Document findDocument(String tenantId, String documentId) {
 *         try {
 *             return documentRepository.findByTenantIdAndId(tenantId, documentId)
 *                 .orElseThrow(() -> new DocumentNotFoundException(
 *                     "Document not found",
 *                     "DOCUMENT_NOT_FOUND",
 *                     Map.of("documentId", documentId, "tenantId", tenantId)
 *                 ));
 *         } catch (DataAccessException e) {
 *             throw new DatabaseException("Failed to retrieve document", e);
 *         }
 *     }
 * }
 * 
 * @RestControllerAdvice
 * public class GlobalExceptionHandler {
 *     
 *     @ExceptionHandler(DocumentNotFoundException.class)
 *     public ResponseEntity<ErrorResponse> handleDocumentNotFound(
 *             DocumentNotFoundException e, HttpServletRequest request) {
 *         ErrorResponse response = ErrorResponse.builder()
 *             .code(e.getErrorCode())
 *             .message(e.getMessage())
 *             .path(request.getRequestURI())
 *             .timestamp(Instant.now())
 *             .correlationId(RequestContextUtils.getCorrelationId())
 *             .build();
 *         
 *         return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
 *     }
 * }
 * }</pre>
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 * @see org.springframework.web.bind.annotation.RestControllerAdvice Global exception handling
 * @see org.springframework.http.ResponseEntity HTTP response handling
 * @see com.enterprise.rag.shared.dto Error response DTOs
 */
package com.enterprise.rag.shared.exception;
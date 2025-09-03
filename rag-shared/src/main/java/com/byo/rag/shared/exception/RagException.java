package com.byo.rag.shared.exception;

/**
 * Base runtime exception for all Enterprise RAG system operational errors.
 * 
 * <p>This exception serves as the foundation for all custom exceptions within the Enterprise RAG
 * system, providing consistent error handling patterns, structured error codes, and comprehensive
 * debugging information across all microservices.</p>
 * 
 * <p><strong>Key Features:</strong></p>
 * <ul>
 *   <li><strong>Error Codes:</strong> Structured error identification for client applications</li>
 *   <li><strong>Chained Exceptions:</strong> Full exception chaining support for root cause analysis</li>
 *   <li><strong>Consistent Structure:</strong> Uniform error handling across all RAG services</li>
 *   <li><strong>Runtime Exception:</strong> Unchecked exception for cleaner service interfaces</li>
 * </ul>
 * 
 * <p><strong>Error Code Conventions:</strong></p>
 * <ul>
 *   <li><strong>RAG_ERROR:</strong> General RAG system errors</li>
 *   <li><strong>DOCUMENT_ERROR:</strong> Document processing and management errors</li>
 *   <li><strong>EMBEDDING_ERROR:</strong> Vector embedding and similarity search errors</li>
 *   <li><strong>TENANT_ERROR:</strong> Multi-tenant isolation and management errors</li>
 *   <li><strong>AUTHENTICATION_ERROR:</strong> Security and authentication errors</li>
 * </ul>
 * 
 * <p><strong>Usage Examples:</strong></p>
 * <pre>{@code
 * // Simple error
 * throw new RagException("Document processing failed");
 * 
 * // Error with specific code
 * throw new RagException("Invalid embedding model", "EMBEDDING_MODEL_ERROR");
 * 
 * // Error with cause chaining
 * try {
 *     processDocument(document);
 * } catch (IOException e) {
 *     throw new RagException("Failed to read document", "DOCUMENT_READ_ERROR", e);
 * }
 * }</pre>
 * 
 * <p><strong>Integration with Error Handling:</strong></p>
 * <p>This exception integrates with Spring Boot's global exception handling mechanisms
 * and provides structured error responses for REST APIs, including error codes that
 * client applications can programmatically handle.</p>
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 * @see RuntimeException
 */
public class RagException extends RuntimeException {
    
    /**
     * The structured error code for programmatic error handling.
     * 
     * <p>This error code enables client applications to programmatically
     * handle different types of errors without parsing error messages.</p>
     */
    private final String errorCode;
    
    /**
     * Creates a new RagException with the specified message and default error code.
     * 
     * <p>This constructor is suitable for general errors where a specific error
     * code is not required. The default error code "RAG_ERROR" will be used.</p>
     * 
     * @param message the detailed error message describing the problem
     */
    public RagException(String message) {
        this(message, "RAG_ERROR");
    }
    
    /**
     * Creates a new RagException with the specified message, default error code, and underlying cause.
     * 
     * <p>This constructor enables exception chaining to preserve the original cause of the error
     * while providing a RAG-specific error context. The default error code "RAG_ERROR" will be used.</p>
     * 
     * @param message the detailed error message describing the problem
     * @param cause the underlying exception that caused this error
     */
    public RagException(String message, Throwable cause) {
        this(message, "RAG_ERROR", cause);
    }
    
    /**
     * Creates a new RagException with the specified message and error code.
     * 
     * <p>This constructor allows specification of a custom error code for more
     * precise error categorization and programmatic error handling.</p>
     * 
     * @param message the detailed error message describing the problem
     * @param errorCode the structured error code for programmatic handling
     */
    public RagException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    /**
     * Creates a new RagException with the specified message, error code, and underlying cause.
     * 
     * <p>This is the most comprehensive constructor, supporting both custom error codes
     * and exception chaining for complete error context preservation.</p>
     * 
     * @param message the detailed error message describing the problem
     * @param errorCode the structured error code for programmatic handling
     * @param cause the underlying exception that caused this error
     */
    public RagException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    /**
     * Returns the structured error code for programmatic error handling.
     * 
     * <p>The error code enables client applications and monitoring systems
     * to programmatically identify and handle different types of errors
     * without relying on error message parsing.</p>
     * 
     * @return the error code associated with this exception
     */
    public String getErrorCode() {
        return errorCode;
    }
}
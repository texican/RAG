package com.byo.rag.shared.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for RagException functionality including error codes,
 * message handling, and exception chaining behavior.
 */
class RagExceptionTest {

    @Test
    @DisplayName("Should create RagException with message and default error code")
    void shouldCreateRagExceptionWithMessageAndDefaultErrorCode() {
        String message = "Test error occurred";
        
        RagException exception = new RagException(message);
        
        assertEquals(message, exception.getMessage());
        assertEquals("RAG_ERROR", exception.getErrorCode());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Should create RagException with message and custom error code")
    void shouldCreateRagExceptionWithMessageAndCustomErrorCode() {
        String message = "Custom error occurred";
        String errorCode = "CUSTOM_ERROR";
        
        RagException exception = new RagException(message, errorCode);
        
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Should create RagException with message, error code, and cause")
    void shouldCreateRagExceptionWithMessageErrorCodeAndCause() {
        String message = "Wrapped error occurred";
        String errorCode = "WRAPPED_ERROR";
        Throwable cause = new IllegalArgumentException("Original cause");
        
        RagException exception = new RagException(message, errorCode, cause);
        
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("Should create RagException with message, cause, and default error code")
    void shouldCreateRagExceptionWithMessageCauseAndDefaultErrorCode() {
        String message = "Error with cause";
        Throwable cause = new RuntimeException("Root cause");
        
        RagException exception = new RagException(message, cause);
        
        assertEquals(message, exception.getMessage());
        assertEquals("RAG_ERROR", exception.getErrorCode());
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("Should be a RuntimeException")
    void shouldBeRuntimeException() {
        RagException exception = new RagException("Test");
        
        assertTrue(exception instanceof RuntimeException, "RagException should be a RuntimeException");
        assertTrue(exception instanceof Exception, "RagException should be an Exception");
        // RuntimeException is unchecked, so this is correct behavior
    }

    @Test
    @DisplayName("Should handle null error code gracefully")
    void shouldHandleNullErrorCodeGracefully() {
        String message = "Test message";
        String errorCode = null;
        
        RagException exception = new RagException(message, errorCode);
        
        assertEquals(message, exception.getMessage());
        assertNull(exception.getErrorCode());
    }

    @Test
    @DisplayName("Should handle empty error code")
    void shouldHandleEmptyErrorCode() {
        String message = "Test message";
        String errorCode = "";
        
        RagException exception = new RagException(message, errorCode);
        
        assertEquals(message, exception.getMessage());
        assertEquals("", exception.getErrorCode());
    }

    @Test
    @DisplayName("Should support standard exception patterns")
    void shouldSupportStandardExceptionPatterns() {
        // Test exception chaining
        Exception originalException = new IllegalStateException("Original problem");
        RagException wrappedException = new RagException("Wrapped problem", "WRAP_ERROR", originalException);
        
        // Should be able to unwrap cause
        assertEquals(originalException, wrappedException.getCause());
        assertEquals("Original problem", wrappedException.getCause().getMessage());
        
        // Should support stack trace
        assertNotNull(wrappedException.getStackTrace());
        assertTrue(wrappedException.getStackTrace().length > 0);
    }

    @Test
    @DisplayName("Should handle common error code patterns")
    void shouldHandleCommonErrorCodePatterns() {
        // Test documented error code patterns
        RagException docError = new RagException("Document not found", "DOCUMENT_ERROR");
        RagException embeddingError = new RagException("Embedding failed", "EMBEDDING_ERROR");
        RagException tenantError = new RagException("Tenant access denied", "TENANT_ERROR");
        RagException authError = new RagException("Authentication failed", "AUTHENTICATION_ERROR");
        
        assertEquals("DOCUMENT_ERROR", docError.getErrorCode());
        assertEquals("EMBEDDING_ERROR", embeddingError.getErrorCode());
        assertEquals("TENANT_ERROR", tenantError.getErrorCode());
        assertEquals("AUTHENTICATION_ERROR", authError.getErrorCode());
    }

    @Test
    @DisplayName("Should have meaningful toString representation")
    void shouldHaveMeaningfulToStringRepresentation() {
        RagException exception = new RagException("Test error", "TEST_ERROR");
        
        String toString = exception.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("RagException"), "toString should contain class name");
        assertTrue(toString.contains("Test error"), "toString should contain message");
    }

    @Test
    @DisplayName("Should support error code-based exception handling")
    void shouldSupportErrorCodeBasedExceptionHandling() {
        // Simulate exception handling based on error codes
        RagException documentError = new RagException("Document not found", "DOCUMENT_NOT_FOUND");
        RagException permissionError = new RagException("Access denied", "PERMISSION_DENIED");
        RagException validationError = new RagException("Invalid input", "VALIDATION_ERROR");
        
        // Should be able to handle different error types programmatically
        String documentAction = handleError(documentError);
        String permissionAction = handleError(permissionError);
        String validationAction = handleError(validationError);
        
        assertEquals("retry_document", documentAction);
        assertEquals("check_permissions", permissionAction);
        assertEquals("validate_input", validationAction);
    }

    /**
     * Helper method to simulate error code-based exception handling
     */
    private String handleError(RagException exception) {
        return switch (exception.getErrorCode()) {
            case "DOCUMENT_NOT_FOUND" -> "retry_document";
            case "PERMISSION_DENIED" -> "check_permissions";
            case "VALIDATION_ERROR" -> "validate_input";
            default -> "generic_error";
        };
    }

    @Test
    @DisplayName("Should preserve exception details through re-throwing")
    void shouldPreserveExceptionDetailsThroughRethrowing() {
        String originalMessage = "Original error";
        String originalErrorCode = "ORIGINAL_ERROR";
        Exception originalCause = new RuntimeException("Root cause");
        
        RagException original = new RagException(originalMessage, originalErrorCode, originalCause);
        
        // Simulate re-throwing in a catch block
        try {
            throw original;
        } catch (RagException caught) {
            // Verify all details are preserved
            assertEquals(originalMessage, caught.getMessage());
            assertEquals(originalErrorCode, caught.getErrorCode());
            assertEquals(originalCause, caught.getCause());
            
            // Stack trace should be preserved
            assertNotNull(caught.getStackTrace());
            assertTrue(caught.getStackTrace().length > 0);
        }
    }
}
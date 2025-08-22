package com.enterprise.rag.document.controller;

import com.enterprise.rag.shared.exception.DocumentNotFoundException;
import com.enterprise.rag.shared.exception.DocumentProcessingException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class DocumentExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(DocumentExceptionHandler.class);

    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDocumentNotFound(DocumentNotFoundException ex) {
        logger.warn("Document not found: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            "DOCUMENT_NOT_FOUND",
            ex.getMessage(),
            HttpStatus.NOT_FOUND.value(),
            LocalDateTime.now(),
            null
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(DocumentProcessingException.class)
    public ResponseEntity<ErrorResponse> handleDocumentProcessing(DocumentProcessingException ex) {
        logger.error("Document processing error: {}", ex.getMessage(), ex);
        
        ErrorResponse error = new ErrorResponse(
            "DOCUMENT_PROCESSING_ERROR",
            ex.getMessage(),
            HttpStatus.BAD_REQUEST.value(),
            LocalDateTime.now(),
            null
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        logger.warn("Validation error: {}", ex.getMessage());
        
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });
        
        ErrorResponse error = new ErrorResponse(
            "VALIDATION_ERROR",
            "Request validation failed",
            HttpStatus.BAD_REQUEST.value(),
            LocalDateTime.now(),
            validationErrors
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        logger.warn("Constraint violation: {}", ex.getMessage());
        
        Map<String, String> validationErrors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            validationErrors.put(fieldName, errorMessage);
        });
        
        ErrorResponse error = new ErrorResponse(
            "CONSTRAINT_VIOLATION",
            "Request constraint violation",
            HttpStatus.BAD_REQUEST.value(),
            LocalDateTime.now(),
            validationErrors
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
        logger.warn("Missing required header: {}", ex.getHeaderName());
        
        Map<String, String> details = new HashMap<>();
        details.put("missingHeader", ex.getHeaderName());
        
        ErrorResponse error = new ErrorResponse(
            "MISSING_REQUIRED_HEADER",
            "Required header '" + ex.getHeaderName() + "' is missing",
            HttpStatus.BAD_REQUEST.value(),
            LocalDateTime.now(),
            details
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        logger.warn("Type mismatch for parameter: {} with value: {}", ex.getName(), ex.getValue());
        
        Map<String, String> details = new HashMap<>();
        details.put("parameter", ex.getName());
        details.put("value", String.valueOf(ex.getValue()));
        details.put("expectedType", ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        
        ErrorResponse error = new ErrorResponse(
            "INVALID_PARAMETER_TYPE",
            "Invalid parameter type for '" + ex.getName() + "'",
            HttpStatus.BAD_REQUEST.value(),
            LocalDateTime.now(),
            details
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestPart(MissingServletRequestPartException ex) {
        logger.warn("Missing required request part: {}", ex.getRequestPartName());
        
        Map<String, String> details = new HashMap<>();
        details.put("missingPart", ex.getRequestPartName());
        
        ErrorResponse error = new ErrorResponse(
            "MISSING_REQUIRED_PART",
            "Required request part '" + ex.getRequestPartName() + "' is missing",
            HttpStatus.BAD_REQUEST.value(),
            LocalDateTime.now(),
            details
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> handleMultipartException(MultipartException ex) {
        logger.warn("Multipart request error: {}", ex.getMessage());
        
        String message = "Invalid multipart request";
        if (ex.getMessage().contains("not a multipart request")) {
            message = "Request must be a multipart/form-data request for file upload";
        }
        
        ErrorResponse error = new ErrorResponse(
            "INVALID_MULTIPART_REQUEST",
            message,
            HttpStatus.BAD_REQUEST.value(),
            LocalDateTime.now(),
            null
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        logger.warn("File upload size exceeded: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            "FILE_SIZE_EXCEEDED",
            "File size exceeds maximum allowed size",
            HttpStatus.PAYLOAD_TOO_LARGE.value(),
            LocalDateTime.now(),
            null
        );
        
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        logger.warn("Unsupported media type: {}, supported types: {}", ex.getContentType(), ex.getSupportedMediaTypes());
        
        Map<String, String> details = new HashMap<>();
        details.put("provided", ex.getContentType() != null ? ex.getContentType().toString() : "none");
        details.put("supported", ex.getSupportedMediaTypes().toString());
        
        ErrorResponse error = new ErrorResponse(
            "UNSUPPORTED_MEDIA_TYPE",
            "Content type '" + (ex.getContentType() != null ? ex.getContentType() : "none") + "' is not supported",
            HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
            LocalDateTime.now(),
            details
        );
        
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(error);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        logger.warn("Method not supported: {}, supported methods: {}", ex.getMethod(), ex.getSupportedHttpMethods());
        
        Map<String, String> details = new HashMap<>();
        details.put("method", ex.getMethod());
        details.put("supported", ex.getSupportedHttpMethods() != null ? ex.getSupportedHttpMethods().toString() : "unknown");
        
        ErrorResponse error = new ErrorResponse(
            "METHOD_NOT_ALLOWED",
            "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint",
            HttpStatus.METHOD_NOT_ALLOWED.value(),
            LocalDateTime.now(),
            details
        );
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        logger.warn("Illegal argument: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            "INVALID_ARGUMENT",
            ex.getMessage(),
            HttpStatus.BAD_REQUEST.value(),
            LocalDateTime.now(),
            null
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        logger.error("Unexpected error: {}", ex.getMessage(), ex);
        
        // Generate a correlation ID for tracking
        String correlationId = UUID.randomUUID().toString();
        
        Map<String, String> details = new HashMap<>();
        details.put("correlationId", correlationId);
        
        ErrorResponse error = new ErrorResponse(
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred. Please contact support with correlation ID: " + correlationId,
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            LocalDateTime.now(),
            details
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    // Error response record for consistent error format
    public record ErrorResponse(
        String code,
        String message,
        int status,
        LocalDateTime timestamp,
        Map<String, String> details
    ) {}
}
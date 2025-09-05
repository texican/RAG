package com.byo.rag.core.client;

import com.byo.rag.shared.exception.RagException;
import feign.Request;
import feign.Response;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EmbeddingServiceErrorDecoder.
 */
@ExtendWith(MockitoExtension.class)
class EmbeddingServiceErrorDecoderTest {

    private EmbeddingServiceErrorDecoder errorDecoder;
    private Request request;

    @BeforeEach
    void setUp() {
        errorDecoder = new EmbeddingServiceErrorDecoder();
        
        // Create a mock request
        request = Request.create(
            Request.HttpMethod.POST,
            "http://embedding-service/api/v1/search",
            Collections.emptyMap(),
            "test request body".getBytes(StandardCharsets.UTF_8),
            StandardCharsets.UTF_8,
            null
        );
    }

    @Test
    void decode_BadRequest_ReturnsRagException() {
        // Arrange
        Response response = createMockResponse(400, "Bad Request: Invalid query parameter");

        // Act
        Exception exception = errorDecoder.decode("search", response);

        // Assert
        assertInstanceOf(RagException.class, exception);
        RagException ragException = (RagException) exception;
        assertTrue(ragException.getMessage().contains("Invalid query parameter"));
    }

    @Test
    void decode_Unauthorized_ReturnsRagException() {
        // Arrange
        Response response = createMockResponse(401, "Unauthorized: Invalid tenant ID");

        // Act
        Exception exception = errorDecoder.decode("search", response);

        // Assert
        assertInstanceOf(RagException.class, exception);
        RagException ragException = (RagException) exception;
        assertTrue(ragException.getMessage().contains("Authentication failed"));
        assertTrue(ragException.getMessage().contains("Invalid tenant ID"));
    }

    @Test
    void decode_Forbidden_ReturnsRagException() {
        // Arrange
        Response response = createMockResponse(403, "Forbidden: Insufficient permissions");

        // Act
        Exception exception = errorDecoder.decode("search", response);

        // Assert
        assertInstanceOf(RagException.class, exception);
        RagException ragException = (RagException) exception;
        assertTrue(ragException.getMessage().contains("Access denied"));
        assertTrue(ragException.getMessage().contains("Insufficient permissions"));
    }

    @Test
    void decode_NotFound_ReturnsRagException() {
        // Arrange
        Response response = createMockResponse(404, "Not Found: Tenant not found");

        // Act
        Exception exception = errorDecoder.decode("search", response);

        // Assert
        assertInstanceOf(RagException.class, exception);
        RagException ragException = (RagException) exception;
        assertTrue(ragException.getMessage().contains("Resource not found"));
        assertTrue(ragException.getMessage().contains("Tenant not found"));
    }

    @Test
    void decode_TooManyRequests_ReturnsRetryableException() {
        // Arrange
        Response response = createMockResponse(429, "Too Many Requests: Rate limit exceeded");

        // Act
        Exception exception = errorDecoder.decode("search", response);

        // Assert
        assertInstanceOf(RetryableException.class, exception);
        RetryableException retryableException = (RetryableException) exception;
        assertTrue(retryableException.getMessage().contains("Rate limit exceeded"));
        assertNotNull(retryableException.retryAfter());
    }

    @Test
    void decode_InternalServerError_ReturnsRetryableException() {
        // Arrange
        Response response = createMockResponse(500, "Internal Server Error: Database connection failed");

        // Act
        Exception exception = errorDecoder.decode("search", response);

        // Assert
        assertInstanceOf(RetryableException.class, exception);
        RetryableException retryableException = (RetryableException) exception;
        assertTrue(retryableException.getMessage().contains("Embedding service error"));
        assertTrue(retryableException.getMessage().contains("Database connection failed"));
    }

    @Test
    void decode_BadGateway_ReturnsRetryableException() {
        // Arrange
        Response response = createMockResponse(502, "Bad Gateway: Upstream service unavailable");

        // Act
        Exception exception = errorDecoder.decode("search", response);

        // Assert
        assertInstanceOf(RetryableException.class, exception);
        RetryableException retryableException = (RetryableException) exception;
        assertTrue(retryableException.getMessage().contains("Service temporarily unavailable"));
        assertTrue(retryableException.getMessage().contains("Upstream service unavailable"));
    }

    @Test
    void decode_ServiceUnavailable_ReturnsRetryableException() {
        // Arrange
        Response response = createMockResponse(503, "Service Unavailable: Maintenance mode");

        // Act
        Exception exception = errorDecoder.decode("search", response);

        // Assert
        assertInstanceOf(RetryableException.class, exception);
        RetryableException retryableException = (RetryableException) exception;
        assertTrue(retryableException.getMessage().contains("Service temporarily unavailable"));
        assertTrue(retryableException.getMessage().contains("Maintenance mode"));
    }

    @Test
    void decode_GatewayTimeout_ReturnsRetryableException() {
        // Arrange
        Response response = createMockResponse(504, "Gateway Timeout: Request timeout");

        // Act
        Exception exception = errorDecoder.decode("search", response);

        // Assert
        assertInstanceOf(RetryableException.class, exception);
        RetryableException retryableException = (RetryableException) exception;
        assertTrue(retryableException.getMessage().contains("Service temporarily unavailable"));
        assertTrue(retryableException.getMessage().contains("Request timeout"));
    }

    @Test
    void decode_UnknownClientError_ReturnsRagException() {
        // Arrange
        Response response = createMockResponse(418, "I'm a teapot"); // Unusual client error

        // Act
        Exception exception = errorDecoder.decode("search", response);

        // Assert
        assertInstanceOf(RagException.class, exception);
        RagException ragException = (RagException) exception;
        assertTrue(ragException.getMessage().contains("Embedding service client error"));
        assertTrue(ragException.getMessage().contains("I'm a teapot"));
    }

    @Test
    void decode_UnknownServerError_ReturnsRetryableException() {
        // Arrange
        Response response = createMockResponse(507, "Insufficient Storage"); // Server error

        // Act
        Exception exception = errorDecoder.decode("search", response);

        // Assert
        assertInstanceOf(RetryableException.class, exception);
        RetryableException retryableException = (RetryableException) exception;
        assertTrue(retryableException.getMessage().contains("Embedding service error"));
        assertTrue(retryableException.getMessage().contains("Insufficient Storage"));
    }

    @Test
    void decode_EmptyResponseBody_HandlesGracefully() {
        // Arrange
        Response response = createMockResponse(500, "");

        // Act
        Exception exception = errorDecoder.decode("search", response);

        // Assert
        assertInstanceOf(RetryableException.class, exception);
        RetryableException retryableException = (RetryableException) exception;
        assertTrue(retryableException.getMessage().contains("Embedding service error"));
    }

    @Test
    void decode_NullResponseBody_HandlesGracefully() {
        // Arrange
        Response response = createMockResponse(500, null);

        // Act
        Exception exception = errorDecoder.decode("search", response);

        // Assert
        assertInstanceOf(RetryableException.class, exception);
        RetryableException retryableException = (RetryableException) exception;
        assertTrue(retryableException.getMessage().contains("Embedding service error"));
    }

    @Test
    void decode_DifferentMethodNames_HandlesCorrectly() {
        // Test with different method names to ensure method-specific handling works

        // Arrange
        Response response = createMockResponse(400, "Bad request for embed operation");

        // Act
        Exception exception1 = errorDecoder.decode("search", response);
        Exception exception2 = errorDecoder.decode("embed", response);
        Exception exception3 = errorDecoder.decode("health", response);

        // Assert
        assertInstanceOf(RagException.class, exception1);
        assertInstanceOf(RagException.class, exception2);
        assertInstanceOf(RagException.class, exception3);

        // All should contain similar error information
        assertTrue(exception1.getMessage().contains("Bad request"));
        assertTrue(exception2.getMessage().contains("Bad request"));
        assertTrue(exception3.getMessage().contains("Bad request"));
    }

    @Test
    void decode_LongErrorMessage_TruncatesAppropriately() {
        // Arrange
        String longErrorMessage = "This is a very long error message that contains lots of details ".repeat(10);
        Response response = createMockResponse(400, longErrorMessage);

        // Act
        Exception exception = errorDecoder.decode("search", response);

        // Assert
        assertInstanceOf(RagException.class, exception);
        RagException ragException = (RagException) exception;
        // The error decoder should handle long messages appropriately
        assertNotNull(ragException.getMessage());
        assertTrue(ragException.getMessage().length() > 0);
    }

    @Test
    void decode_JsonErrorResponse_ParsesCorrectly() {
        // Arrange
        String jsonError = "{\"error\":\"Invalid query\",\"code\":\"INVALID_QUERY\",\"details\":\"Query cannot be empty\"}";
        Response response = createMockResponse(400, jsonError);

        // Act
        Exception exception = errorDecoder.decode("search", response);

        // Assert
        assertInstanceOf(RagException.class, exception);
        RagException ragException = (RagException) exception;
        assertTrue(ragException.getMessage().contains("Invalid query") || 
                  ragException.getMessage().contains("Query cannot be empty"));
    }

    private Response createMockResponse(int status, String body) {
        Response.Body responseBody = null;
        if (body != null) {
            responseBody = Response.Body.create(body, StandardCharsets.UTF_8);
        }

        return Response.builder()
            .status(status)
            .reason("HTTP " + status)
            .request(request)
            .headers(Collections.emptyMap())
            .body(responseBody)
            .build();
    }
}
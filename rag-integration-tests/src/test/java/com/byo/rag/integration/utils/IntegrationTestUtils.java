package com.byo.rag.integration.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.awaitility.Awaitility;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Utility class providing common helper methods for integration tests.
 * 
 * This class contains static methods for common testing operations like
 * HTTP requests, JSON processing, waiting for conditions, and assertions.
 */
public final class IntegrationTestUtils {
    
    private IntegrationTestUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Create HTTP headers with JSON content type.
     */
    public static HttpHeaders createJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * Create HTTP headers with authentication token.
     */
    public static HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = createJsonHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    /**
     * Create HTTP entity with JSON body and appropriate headers.
     */
    public static <T> HttpEntity<T> createJsonEntity(T body) {
        return new HttpEntity<>(body, createJsonHeaders());
    }

    /**
     * Create authenticated HTTP entity with JSON body.
     */
    public static <T> HttpEntity<T> createAuthEntity(T body, String token) {
        return new HttpEntity<>(body, createAuthHeaders(token));
    }

    /**
     * Perform a GET request and return the response.
     */
    public static <T> ResponseEntity<T> performGet(RestTemplate restTemplate, String url, Class<T> responseType) {
        return restTemplate.getForEntity(url, responseType);
    }

    /**
     * Perform an authenticated GET request.
     */
    public static <T> ResponseEntity<T> performAuthenticatedGet(
            RestTemplate restTemplate, 
            String url, 
            String token, 
            Class<T> responseType) {
        return restTemplate.exchange(
            url, 
            HttpMethod.GET, 
            new HttpEntity<>(createAuthHeaders(token)), 
            responseType
        );
    }

    /**
     * Perform a POST request with JSON body.
     */
    public static <T, R> ResponseEntity<R> performPost(
            RestTemplate restTemplate, 
            String url, 
            T requestBody, 
            Class<R> responseType) {
        return restTemplate.postForEntity(url, createJsonEntity(requestBody), responseType);
    }

    /**
     * Perform an authenticated POST request with JSON body.
     */
    public static <T, R> ResponseEntity<R> performAuthenticatedPost(
            RestTemplate restTemplate, 
            String url, 
            T requestBody, 
            String token, 
            Class<R> responseType) {
        return restTemplate.exchange(
            url, 
            HttpMethod.POST, 
            createAuthEntity(requestBody, token), 
            responseType
        );
    }

    /**
     * Perform a PUT request with JSON body.
     */
    public static <T, R> ResponseEntity<R> performPut(
            RestTemplate restTemplate, 
            String url, 
            T requestBody, 
            String token, 
            Class<R> responseType) {
        return restTemplate.exchange(
            url, 
            HttpMethod.PUT, 
            createAuthEntity(requestBody, token), 
            responseType
        );
    }

    /**
     * Perform an authenticated DELETE request.
     */
    public static <T> ResponseEntity<T> performAuthenticatedDelete(
            RestTemplate restTemplate, 
            String url, 
            String token, 
            Class<T> responseType) {
        return restTemplate.exchange(
            url, 
            HttpMethod.DELETE, 
            new HttpEntity<>(createAuthHeaders(token)), 
            responseType
        );
    }

    /**
     * Convert object to JSON string for logging and debugging.
     */
    public static String toJson(Object object, ObjectMapper objectMapper) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            return "Error serializing object: " + e.getMessage();
        }
    }

    /**
     * Convert JSON string to object.
     */
    public static <T> T fromJson(String json, Class<T> clazz, ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Error deserializing JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Wait for a condition to be true with default timeout.
     */
    public static void waitForCondition(Callable<Boolean> condition) {
        waitForCondition(condition, Duration.ofSeconds(30));
    }

    /**
     * Wait for a condition to be true with custom timeout.
     */
    public static void waitForCondition(Callable<Boolean> condition, Duration timeout) {
        Awaitility.await()
                .atMost(timeout)
                .pollInterval(Duration.ofMillis(500))
                .until(condition);
    }

    /**
     * Wait for a condition to be true and return a value.
     */
    public static <T> T waitForValue(Callable<T> valueSupplier) {
        return waitForValue(valueSupplier, Duration.ofSeconds(30));
    }

    /**
     * Wait for a condition to be true and return a value with custom timeout.
     */
    public static <T> T waitForValue(Callable<T> valueSupplier, Duration timeout) {
        return Awaitility.await()
                .atMost(timeout)
                .pollInterval(Duration.ofMillis(500))
                .until(valueSupplier, value -> value != null);
    }

    /**
     * Wait for HTTP endpoint to be available.
     */
    public static void waitForEndpoint(RestTemplate restTemplate, String url) {
        waitForCondition(() -> {
            try {
                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                return response.getStatusCode().is2xxSuccessful();
            } catch (Exception e) {
                return false;
            }
        }, Duration.ofSeconds(60));
    }

    /**
     * Assert that HTTP response is successful (2xx status).
     */
    public static void assertSuccessfulResponse(ResponseEntity<?> response) {
        assertThat(response.getStatusCode().is2xxSuccessful())
            .as("Expected successful HTTP response but got: %s", response.getStatusCode())
            .isTrue();
    }

    /**
     * Assert that HTTP response has specific status code.
     */
    public static void assertResponseStatus(ResponseEntity<?> response, int expectedStatus) {
        assertThat(response.getStatusCode().value())
            .as("Expected HTTP status %d but got: %d", expectedStatus, response.getStatusCode().value())
            .isEqualTo(expectedStatus);
    }

    /**
     * Assert that response body is not null and not empty.
     */
    public static void assertResponseBodyNotEmpty(ResponseEntity<?> response) {
        assertThat(response.getBody())
            .as("Expected non-empty response body")
            .isNotNull();
    }

    /**
     * Sleep for specified duration (use sparingly, prefer waitForCondition).
     */
    public static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Sleep interrupted", e);
        }
    }

    /**
     * Execute a block of code with retry logic.
     */
    public static <T> T withRetry(Supplier<T> operation, int maxAttempts, Duration delay) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return operation.get();
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxAttempts) {
                    sleep(delay);
                }
            }
        }
        
        throw new RuntimeException(
            String.format("Operation failed after %d attempts", maxAttempts), 
            lastException
        );
    }

    /**
     * Execute a block of code with default retry logic (3 attempts, 1 second delay).
     */
    public static <T> T withRetry(Supplier<T> operation) {
        return withRetry(operation, 3, Duration.ofSeconds(1));
    }

    /**
     * Generate a test correlation ID for tracking test execution.
     */
    public static String generateTestCorrelationId() {
        return "test-" + System.currentTimeMillis() + "-" + 
               Integer.toHexString((int) (Math.random() * 0x10000));
    }

    /**
     * Validate that a string is a valid UUID format.
     */
    public static boolean isValidUUID(String uuid) {
        try {
            java.util.UUID.fromString(uuid);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extract JWT token from authorization header.
     */
    public static String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }

    /**
     * Create a test timeout duration based on system properties or default.
     */
    public static Duration getTestTimeout() {
        String timeoutProperty = System.getProperty("test.timeout.default", "30");
        try {
            return Duration.ofSeconds(Long.parseLong(timeoutProperty));
        } catch (NumberFormatException e) {
            return Duration.ofSeconds(30);
        }
    }
}
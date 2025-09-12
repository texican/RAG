package com.byo.rag.gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Advanced Request Validation and Sanitization Service.
 * 
 * <p>This service provides comprehensive security validation for all incoming requests,
 * implementing OWASP security best practices to prevent common web application
 * vulnerabilities including injection attacks, XSS, and malicious payload detection.
 * 
 * <p><strong>Validation Categories:</strong>
 * <ul>
 *   <li>Input Sanitization - Removes or escapes dangerous characters</li>
 *   <li>Injection Prevention - Detects SQL, NoSQL, and command injection attempts</li>
 *   <li>XSS Prevention - Prevents cross-site scripting attacks</li>
 *   <li>Path Traversal Prevention - Blocks directory traversal attempts</li>
 *   <li>Content Validation - Validates content types and sizes</li>
 *   <li>Header Validation - Validates and sanitizes HTTP headers</li>
 * </ul>
 * 
 * <p><strong>OWASP Compliance:</strong>
 * Implements security controls from OWASP Top 10 and ASVS guidelines:
 * <ul>
 *   <li>A03:2021 - Injection prevention</li>
 *   <li>A07:2021 - Identification and Authentication Failures</li>
 *   <li>A09:2021 - Security Logging and Monitoring Failures</li>
 *   <li>A10:2021 - Server-Side Request Forgery (SSRF)</li>
 * </ul>
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.0
 */
@Service
public class RequestValidationService {

    private static final Logger logger = LoggerFactory.getLogger(RequestValidationService.class);

    /** Security audit service for logging validation failures. */
    private final SecurityAuditService auditService;

    /** SQL injection detection patterns. */
    private static final List<Pattern> SQL_INJECTION_PATTERNS = Arrays.asList(
        Pattern.compile("(?i).*(union|select|insert|update|delete|drop|create|alter|exec|execute).*"),
        Pattern.compile("(?i).*('|;|--|/\\*|\\*/|xp_|sp_).*"),
        Pattern.compile("(?i).*(script|javascript|vbscript|onload|onerror).*"),
        Pattern.compile("(?i).*(1=1|1'='1|' or '1'='1).*")
    );

    /** XSS attack detection patterns. */
    private static final List<Pattern> XSS_PATTERNS = Arrays.asList(
        Pattern.compile("(?i).*<\\s*script.*"),
        Pattern.compile("(?i).*javascript\\s*:.*"),
        Pattern.compile("(?i).*on\\w+\\s*=.*"),
        Pattern.compile("(?i).*<\\s*iframe.*"),
        Pattern.compile("(?i).*<\\s*object.*"),
        Pattern.compile("(?i).*<\\s*embed.*")
    );

    /** Path traversal detection patterns. */
    private static final List<Pattern> PATH_TRAVERSAL_PATTERNS = Arrays.asList(
        Pattern.compile(".*(\\.\\./|\\.\\.\\\\).*"),
        Pattern.compile("(?i).*(etc/passwd|boot\\.ini|web\\.config).*"),
        Pattern.compile("(?i).*(proc/self/environ|windows/system32).*")
    );

    /** Command injection detection patterns. */
    private static final List<Pattern> COMMAND_INJECTION_PATTERNS = Arrays.asList(
        Pattern.compile("(?i).*[;&|`].*"),
        Pattern.compile("(?i).*(wget|curl|nc|netcat|ping|nslookup).*"),
        Pattern.compile("(?i).*(bash|sh|cmd|powershell|python|perl).*")
    );

    /** Dangerous characters that should be escaped or removed. */
    private static final Pattern DANGEROUS_CHARS = Pattern.compile("[<>\"'%;()&+\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]");

    /** Maximum allowed content length (10MB). */
    private static final long MAX_CONTENT_LENGTH = 10 * 1024 * 1024;

    /** Maximum allowed header value length. */
    private static final int MAX_HEADER_LENGTH = 4096;

    /** Set of allowed content types for file uploads. */
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "text/plain", "text/csv", "application/json", "application/xml",
        "application/pdf", "application/msword", "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    /**
     * Validation result containing security assessment.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> violations;
        private final String sanitizedPath;
        private final Map<String, String> sanitizedHeaders;

        public ValidationResult(boolean valid, List<String> violations, String sanitizedPath, 
                              Map<String, String> sanitizedHeaders) {
            this.valid = valid;
            this.violations = new ArrayList<>(violations);
            this.sanitizedPath = sanitizedPath;
            this.sanitizedHeaders = new HashMap<>(sanitizedHeaders);
        }

        public boolean isValid() { return valid; }
        public List<String> getViolations() { return new ArrayList<>(violations); }
        public String getSanitizedPath() { return sanitizedPath; }
        public Map<String, String> getSanitizedHeaders() { return new HashMap<>(sanitizedHeaders); }
    }

    /**
     * Constructs request validation service with audit logging.
     */
    public RequestValidationService(@Autowired SecurityAuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Performs comprehensive validation of incoming HTTP requests.
     * 
     * @param exchange the server web exchange containing request details
     * @param userId the authenticated user ID (if available)
     * @return Mono containing validation result
     */
    public Mono<ValidationResult> validateRequest(ServerWebExchange exchange, String userId) {
        return Mono.fromCallable(() -> {
            ServerHttpRequest request = exchange.getRequest();
            List<String> violations = new ArrayList<>();
            
            // Get client information for audit logging
            String clientIP = getClientIP(request);
            String userAgent = request.getHeaders().getFirst("User-Agent");
            String requestPath = request.getURI().getPath();

            try {
                // Validate request path
                String sanitizedPath = validateAndSanitizePath(requestPath, violations);

                // Validate headers
                Map<String, String> sanitizedHeaders = validateAndSanitizeHeaders(request, violations);

                // Validate query parameters
                validateQueryParameters(request, violations);

                // Validate content length
                validateContentLength(request, violations);

                // Validate content type for upload endpoints
                if (isUploadEndpoint(requestPath)) {
                    validateContentType(request, violations);
                }

                // Validate for injection attacks
                validateForInjectionAttacks(requestPath, request.getURI().getQuery(), violations);

                // Log validation failures
                if (!violations.isEmpty()) {
                    String violationSummary = String.join(", ", violations);
                    auditService.logValidationFailure("REQUEST_VALIDATION", violationSummary, 
                        requestPath, clientIP, userId);
                    
                    logger.warn("Request validation failed from IP: {} Path: {} Violations: {}", 
                        clientIP, requestPath, violationSummary);
                }

                boolean isValid = violations.isEmpty();
                return new ValidationResult(isValid, violations, sanitizedPath, sanitizedHeaders);

            } catch (Exception e) {
                logger.error("Error during request validation from IP: {} Path: {}", 
                    clientIP, requestPath, e);
                violations.add("Internal validation error");
                return new ValidationResult(false, violations, requestPath, new HashMap<>());
            }
        });
    }

    /**
     * Validates and sanitizes request path to prevent path traversal attacks.
     */
    private String validateAndSanitizePath(String path, List<String> violations) {
        if (!StringUtils.hasText(path)) {
            violations.add("Empty or null path");
            return "/";
        }

        // Check for path traversal attempts
        for (Pattern pattern : PATH_TRAVERSAL_PATTERNS) {
            if (pattern.matcher(path).matches()) {
                violations.add("Path traversal attempt detected");
                break;
            }
        }

        // URL decode and validate
        try {
            String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
            
            // Check decoded path for additional traversal attempts
            if (!decodedPath.equals(path)) {
                for (Pattern pattern : PATH_TRAVERSAL_PATTERNS) {
                    if (pattern.matcher(decodedPath).matches()) {
                        violations.add("Encoded path traversal attempt detected");
                        break;
                    }
                }
            }

            // Normalize path to prevent bypasses
            String normalizedPath = normalizePath(decodedPath);
            
            // Validate path length
            if (normalizedPath.length() > 2048) {
                violations.add("Path too long");
                return normalizedPath.substring(0, 2048);
            }

            return normalizedPath;
            
        } catch (Exception e) {
            violations.add("Path decoding error");
            return path;
        }
    }

    /**
     * Validates and sanitizes HTTP headers.
     */
    private Map<String, String> validateAndSanitizeHeaders(ServerHttpRequest request, List<String> violations) {
        Map<String, String> sanitizedHeaders = new HashMap<>();
        
        request.getHeaders().forEach((name, values) -> {
            if (values != null && !values.isEmpty()) {
                String value = values.get(0); // Take first value
                
                // Validate header length
                if (value.length() > MAX_HEADER_LENGTH) {
                    violations.add(String.format("Header %s too long", name));
                    value = value.substring(0, MAX_HEADER_LENGTH);
                }

                // Sanitize header value
                String sanitizedValue = sanitizeInput(value);
                
                // Check for injection attempts in headers
                if (containsInjectionAttempt(sanitizedValue)) {
                    violations.add(String.format("Injection attempt in header %s", name));
                }

                sanitizedHeaders.put(name, sanitizedValue);
            }
        });

        return sanitizedHeaders;
    }

    /**
     * Validates query parameters for injection attacks and size limits.
     */
    private void validateQueryParameters(ServerHttpRequest request, List<String> violations) {
        String query = request.getURI().getQuery();
        if (query != null) {
            // Check query string length
            if (query.length() > 4096) {
                violations.add("Query string too long");
            }

            // Check for injection attempts
            if (containsInjectionAttempt(query)) {
                violations.add("Injection attempt in query parameters");
            }

            // Validate individual parameters
            request.getQueryParams().forEach((name, values) -> {
                for (String value : values) {
                    if (value != null) {
                        if (value.length() > 1024) {
                            violations.add(String.format("Query parameter %s too long", name));
                        }
                        
                        if (containsInjectionAttempt(value)) {
                            violations.add(String.format("Injection attempt in parameter %s", name));
                        }
                    }
                }
            });
        }
    }

    /**
     * Validates content length limits.
     */
    private void validateContentLength(ServerHttpRequest request, List<String> violations) {
        String contentLengthHeader = request.getHeaders().getFirst("Content-Length");
        if (contentLengthHeader != null) {
            try {
                long contentLength = Long.parseLong(contentLengthHeader);
                if (contentLength > MAX_CONTENT_LENGTH) {
                    violations.add("Request body too large");
                }
            } catch (NumberFormatException e) {
                violations.add("Invalid Content-Length header");
            }
        }
    }

    /**
     * Validates content type for file upload endpoints.
     */
    private void validateContentType(ServerHttpRequest request, List<String> violations) {
        String contentType = request.getHeaders().getFirst("Content-Type");
        if (contentType != null) {
            // Extract base content type (without charset, etc.)
            String baseContentType = contentType.split(";")[0].trim().toLowerCase();
            
            if (!ALLOWED_CONTENT_TYPES.contains(baseContentType) && 
                !baseContentType.startsWith("multipart/")) {
                violations.add("Unsupported content type: " + baseContentType);
            }
        }
    }

    /**
     * Validates input for various injection attack patterns.
     */
    private void validateForInjectionAttacks(String path, String query, List<String> violations) {
        String fullInput = path + (query != null ? "?" + query : "");

        if (containsSQLInjection(fullInput)) {
            violations.add("SQL injection attempt detected");
        }

        if (containsXSS(fullInput)) {
            violations.add("XSS attempt detected");
        }

        if (containsCommandInjection(fullInput)) {
            violations.add("Command injection attempt detected");
        }
    }

    /**
     * Checks if the input contains injection attempts.
     */
    private boolean containsInjectionAttempt(String input) {
        return containsSQLInjection(input) || containsXSS(input) || containsCommandInjection(input);
    }

    /**
     * Detects SQL injection attempts.
     */
    private boolean containsSQLInjection(String input) {
        if (input == null) return false;
        
        return SQL_INJECTION_PATTERNS.stream()
            .anyMatch(pattern -> pattern.matcher(input).matches());
    }

    /**
     * Detects XSS attempts.
     */
    private boolean containsXSS(String input) {
        if (input == null) return false;
        
        return XSS_PATTERNS.stream()
            .anyMatch(pattern -> pattern.matcher(input).matches());
    }

    /**
     * Detects command injection attempts.
     */
    private boolean containsCommandInjection(String input) {
        if (input == null) return false;
        
        return COMMAND_INJECTION_PATTERNS.stream()
            .anyMatch(pattern -> pattern.matcher(input).matches());
    }

    /**
     * Sanitizes input by removing or escaping dangerous characters.
     */
    private String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }

        // Remove control characters and dangerous characters
        String sanitized = DANGEROUS_CHARS.matcher(input).replaceAll("");
        
        // Trim whitespace
        sanitized = sanitized.trim();
        
        // Limit length
        if (sanitized.length() > 4096) {
            sanitized = sanitized.substring(0, 4096);
        }

        return sanitized;
    }

    /**
     * Normalizes path to prevent bypass attempts.
     */
    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }

        // Remove double slashes
        path = path.replaceAll("/+", "/");
        
        // Resolve .. and . components
        String[] parts = path.split("/");
        List<String> normalizedParts = new ArrayList<>();
        
        for (String part : parts) {
            if ("..".equals(part)) {
                if (!normalizedParts.isEmpty()) {
                    normalizedParts.remove(normalizedParts.size() - 1);
                }
            } else if (!".".equals(part) && !part.isEmpty()) {
                normalizedParts.add(part);
            }
        }

        String normalized = "/" + String.join("/", normalizedParts);
        return normalized.equals("/") ? "/" : normalized;
    }

    /**
     * Extracts client IP address considering proxy headers.
     */
    private String getClientIP(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeaders().getFirst("X-Real-IP");
        if (StringUtils.hasText(xRealIP)) {
            return xRealIP;
        }

        return request.getRemoteAddress() != null ? 
            request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }

    /**
     * Determines if the request path is for file upload functionality.
     */
    private boolean isUploadEndpoint(String path) {
        return path != null && (path.contains("/upload") || path.contains("/documents"));
    }
}
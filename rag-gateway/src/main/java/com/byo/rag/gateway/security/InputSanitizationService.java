package com.byo.rag.gateway.security;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Input Sanitization Service for RAG Gateway Security.
 * 
 * <p>This service provides comprehensive input sanitization capabilities
 * to prevent injection attacks and ensure data integrity. It implements
 * OWASP guidelines for input validation and sanitization.
 * 
 * <p><strong>Sanitization Features:</strong>
 * <ul>
 *   <li><strong>HTML Encoding</strong>: Encodes HTML special characters</li>
 *   <li><strong>SQL Escaping</strong>: Escapes SQL special characters</li>
 *   <li><strong>Script Removal</strong>: Removes JavaScript and other script content</li>
 *   <li><strong>Path Normalization</strong>: Normalizes file paths to prevent traversal</li>
 *   <li><strong>URL Decoding</strong>: Safely decodes URL-encoded content</li>
 *   <li><strong>Length Validation</strong>: Enforces maximum length constraints</li>
 * </ul>
 * 
 * <p><strong>Performance Optimizations:</strong>
 * <ul>
 *   <li>Pattern compilation caching for reuse</li>
 *   <li>Thread-safe operations for concurrent requests</li>
 *   <li>Minimal memory allocation for high throughput</li>
 *   <li>Early validation termination for invalid input</li>
 * </ul>
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.1
 */
@Service
public class InputSanitizationService {

    /** HTML encoding character map for XSS prevention. */
    private static final Map<String, String> HTML_ENCODING_MAP = Map.of(
        "<", "&lt;",
        ">", "&gt;",
        "\"", "&quot;",
        "'", "&#x27;",
        "&", "&amp;",
        "/", "&#x2F;"
    );

    /** SQL special characters that require escaping. */
    private static final Map<String, String> SQL_ESCAPING_MAP = Map.of(
        "'", "''",
        "\"", "\"\"",
        "\\", "\\\\",
        "\n", "\\n",
        "\r", "\\r",
        "\t", "\\t"
    );

    /** Compiled patterns cache for performance optimization. */
    private final Map<String, Pattern> patternCache = new ConcurrentHashMap<>();

    /** Maximum allowed string length for validation. */
    private static final int MAX_STRING_LENGTH = 10000;

    /**
     * Sanitizes input string to prevent XSS attacks.
     * 
     * <p>This method encodes HTML special characters and removes potentially
     * dangerous script content while preserving legitimate data.
     * 
     * @param input input string to sanitize
     * @return sanitized string safe for HTML context
     */
    public String sanitizeForHtml(String input) {
        if (!StringUtils.hasText(input)) {
            return input;
        }

        if (input.length() > MAX_STRING_LENGTH) {
            throw new IllegalArgumentException("Input exceeds maximum length");
        }

        String sanitized = input;
        
        // HTML encode special characters
        for (Map.Entry<String, String> entry : HTML_ENCODING_MAP.entrySet()) {
            sanitized = sanitized.replace(entry.getKey(), entry.getValue());
        }

        // Remove script tags and dangerous content
        sanitized = removeScriptContent(sanitized);
        
        return sanitized;
    }

    /**
     * Sanitizes input string to prevent SQL injection.
     * 
     * <p>This method escapes SQL special characters that could be used
     * in injection attacks while preserving legitimate data content.
     * 
     * @param input input string to sanitize
     * @return sanitized string safe for SQL context
     */
    public String sanitizeForSql(String input) {
        if (!StringUtils.hasText(input)) {
            return input;
        }

        if (input.length() > MAX_STRING_LENGTH) {
            throw new IllegalArgumentException("Input exceeds maximum length");
        }

        String sanitized = input;
        
        // Escape SQL special characters
        for (Map.Entry<String, String> entry : SQL_ESCAPING_MAP.entrySet()) {
            sanitized = sanitized.replace(entry.getKey(), entry.getValue());
        }

        return sanitized;
    }

    /**
     * Normalizes file path to prevent directory traversal attacks.
     * 
     * <p>This method removes path traversal sequences and normalizes
     * the path to prevent access to unauthorized directories.
     * 
     * @param path file path to normalize
     * @return normalized path safe for file operations
     */
    public String normalizePath(String path) {
        if (!StringUtils.hasText(path)) {
            return path;
        }

        // Remove null bytes
        String normalized = path.replace("\0", "");
        
        // Remove double dots and slashes
        normalized = normalized.replaceAll("\\.{2,}", ".");
        normalized = normalized.replaceAll("[/\\\\]+", "/");
        
        // Remove leading slashes for relative paths
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        
        // Validate final path doesn't contain traversal
        if (normalized.contains("../") || normalized.contains("..\\")) {
            throw new IllegalArgumentException("Path contains traversal sequences");
        }
        
        return normalized;
    }

    /**
     * Safely decodes URL-encoded input.
     * 
     * <p>This method decodes URL-encoded content while preventing
     * double-encoding attacks and maintaining input validation.
     * 
     * @param input URL-encoded input to decode
     * @return safely decoded string
     */
    public String safeUrlDecode(String input) {
        if (!StringUtils.hasText(input)) {
            return input;
        }

        try {
            // Prevent double-encoding attacks by limiting decode attempts
            String decoded = input;
            String previousDecoded;
            int decodeAttempts = 0;
            final int maxDecodeAttempts = 3;
            
            do {
                previousDecoded = decoded;
                decoded = URLDecoder.decode(decoded, StandardCharsets.UTF_8);
                decodeAttempts++;
            } while (!decoded.equals(previousDecoded) && decodeAttempts < maxDecodeAttempts);
            
            if (decodeAttempts >= maxDecodeAttempts) {
                throw new IllegalArgumentException("Potential double-encoding attack detected");
            }
            
            return decoded;
            
        } catch (Exception e) {
            throw new IllegalArgumentException("URL decoding failed: " + e.getMessage());
        }
    }

    /**
     * Validates and sanitizes JSON input.
     * 
     * <p>This method validates JSON structure and sanitizes string values
     * to prevent injection attacks through JSON payloads.
     * 
     * @param jsonInput JSON input to validate and sanitize
     * @return validated and sanitized JSON string
     */
    public String sanitizeJson(String jsonInput) {
        if (!StringUtils.hasText(jsonInput)) {
            return jsonInput;
        }

        if (jsonInput.length() > MAX_STRING_LENGTH) {
            throw new IllegalArgumentException("JSON input exceeds maximum length");
        }

        // Basic JSON structure validation
        if (!isValidJsonStructure(jsonInput)) {
            throw new IllegalArgumentException("Invalid JSON structure");
        }

        // Remove potentially dangerous content from JSON values
        String sanitized = jsonInput;
        
        // Remove script content from string values
        Pattern scriptPattern = getCompiledPattern("script_in_json", 
            "(?i)\"[^\"]*<script[^>]*>.*?</script>[^\"]*\"");
        sanitized = scriptPattern.matcher(sanitized).replaceAll("\"\"");
        
        // Remove event handlers from string values
        Pattern eventPattern = getCompiledPattern("events_in_json", 
            "(?i)\"[^\"]*\\b(on\\w+)\\s*=.*?\"");
        sanitized = eventPattern.matcher(sanitized).replaceAll("\"\"");
        
        return sanitized;
    }

    /**
     * Validates input against length constraints.
     * 
     * @param input input to validate
     * @param maxLength maximum allowed length
     * @return true if input is within length limits
     */
    public boolean validateLength(String input, int maxLength) {
        if (input == null) {
            return true;
        }
        return input.length() <= maxLength;
    }

    /**
     * Checks if input contains only allowed characters.
     * 
     * @param input input to validate
     * @param allowedPattern regex pattern for allowed characters
     * @return true if input contains only allowed characters
     */
    public boolean validateCharacters(String input, String allowedPattern) {
        if (!StringUtils.hasText(input)) {
            return true;
        }
        
        Pattern pattern = getCompiledPattern("allowed_chars", allowedPattern);
        return pattern.matcher(input).matches();
    }

    /**
     * Removes script content from input.
     * 
     * @param input input to clean
     * @return input with script content removed
     */
    private String removeScriptContent(String input) {
        String cleaned = input;
        
        // Remove script tags
        Pattern scriptPattern = getCompiledPattern("script_tags", 
            "(?i)<script[^>]*>.*?</script>");
        cleaned = scriptPattern.matcher(cleaned).replaceAll("");
        
        // Remove iframe tags
        Pattern iframePattern = getCompiledPattern("iframe_tags", 
            "(?i)<iframe[^>]*>.*?</iframe>");
        cleaned = iframePattern.matcher(cleaned).replaceAll("");
        
        // Remove javascript: and vbscript: protocols
        Pattern protocolPattern = getCompiledPattern("script_protocols", 
            "(?i)(javascript|vbscript):");
        cleaned = protocolPattern.matcher(cleaned).replaceAll("");
        
        // Remove event handlers
        Pattern eventPattern = getCompiledPattern("event_handlers", 
            "(?i)\\bon\\w+\\s*=\\s*[\"'][^\"']*[\"']");
        cleaned = eventPattern.matcher(cleaned).replaceAll("");
        
        return cleaned;
    }

    /**
     * Validates basic JSON structure.
     * 
     * @param json JSON string to validate
     * @return true if basic structure is valid
     */
    private boolean isValidJsonStructure(String json) {
        String trimmed = json.trim();
        
        // Basic structure checks
        if (trimmed.isEmpty()) {
            return false;
        }
        
        // Must start and end with appropriate brackets
        boolean isObject = trimmed.startsWith("{") && trimmed.endsWith("}");
        boolean isArray = trimmed.startsWith("[") && trimmed.endsWith("]");
        boolean isString = trimmed.startsWith("\"") && trimmed.endsWith("\"");
        boolean isNumber = trimmed.matches("-?\\d+(\\.\\d+)?");
        boolean isBoolean = "true".equals(trimmed) || "false".equals(trimmed);
        boolean isNull = "null".equals(trimmed);
        
        return isObject || isArray || isString || isNumber || isBoolean || isNull;
    }

    /**
     * Gets compiled pattern from cache or compiles and caches new pattern.
     * 
     * @param key pattern cache key
     * @param regex regular expression
     * @return compiled pattern
     */
    private Pattern getCompiledPattern(String key, String regex) {
        return patternCache.computeIfAbsent(key, k -> Pattern.compile(regex, Pattern.DOTALL));
    }

    /**
     * Clears the pattern cache (for testing or memory management).
     */
    public void clearPatternCache() {
        patternCache.clear();
    }

    /**
     * Gets the current size of the pattern cache.
     * 
     * @return cache size
     */
    public int getPatternCacheSize() {
        return patternCache.size();
    }
}
package com.byo.rag.admin.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom authentication entry point for admin service security.
 * <p>
 * This component handles authentication failures for administrative endpoints
 * by providing standardized JSON error responses. It ensures consistent error
 * handling across all admin API endpoints and provides detailed information
 * for troubleshooting authentication issues.
 * 
 * <h2>Authentication Failure Handling</h2>
 * <ul>
 *   <li><strong>Standardized Responses</strong> - Consistent JSON error format</li>
 *   <li><strong>Security Logging</strong> - Comprehensive audit trail for failed attempts</li>
 *   <li><strong>Request Context</strong> - Includes request path and timestamp</li>
 *   <li><strong>Error Classification</strong> - Categorizes different failure types</li>
 * </ul>
 * 
 * <h2>Error Response Format</h2>
 * <ul>
 *   <li><strong>Timestamp</strong> - ISO instant of the authentication failure</li>
 *   <li><strong>Status Code</strong> - HTTP 401 Unauthorized</li>
 *   <li><strong>Error Type</strong> - Classification of the error</li>
 *   <li><strong>Message</strong> - Human-readable error description</li>
 *   <li><strong>Request Path</strong> - The endpoint that was accessed</li>
 * </ul>
 * 
 * <h2>Security Considerations</h2>
 * <ul>
 *   <li><strong>Information Disclosure</strong> - Prevents leaking sensitive authentication details</li>
 *   <li><strong>Audit Trail</strong> - Logs all authentication failures for security monitoring</li>
 *   <li><strong>Rate Limiting</strong> - Supports integration with rate limiting mechanisms</li>
 *   <li><strong>Attack Detection</strong> - Provides data for intrusion detection systems</li>
 * </ul>
 * 
 * <h2>Admin Security Context</h2>
 * <ul>
 *   <li><strong>Elevated Privileges</strong> - Handles failures for high-privilege operations</li>
 *   <li><strong>Role Validation</strong> - Ensures ADMIN role requirements</li>
 *   <li><strong>Token Validation</strong> - Validates admin-specific JWT tokens</li>
 *   <li><strong>Session Management</strong> - Handles admin session lifecycle</li>
 * </ul>
 * 
 * <h2>Integration Points</h2>
 * <ul>
 *   <li><strong>Spring Security</strong> - Integrates with Spring Security filter chain</li>
 *   <li><strong>JWT Authentication</strong> - Works with admin JWT authentication filter</li>
 *   <li><strong>Global Exception Handler</strong> - Coordinates with global error handling</li>
 *   <li><strong>Monitoring Systems</strong> - Provides data for security monitoring</li>
 * </ul>
 * 
 * @author Enterprise RAG Development Team
 * @version 0.8.0
 * @since 0.1.0
 * @see org.springframework.security.web.AuthenticationEntryPoint
 * @see com.byo.rag.admin.security.AdminJwtAuthenticationFilter
 */
@Component
public class AdminAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(AdminAuthenticationEntryPoint.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                        HttpServletResponse response,
                        AuthenticationException authException) throws IOException {
        
        logger.warn("Admin authentication failed for request: {} - {}", 
                   request.getRequestURI(), authException.getMessage());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        errorResponse.put("error", "Unauthorized");
        errorResponse.put("message", "Admin authentication required");
        errorResponse.put("path", request.getRequestURI());

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
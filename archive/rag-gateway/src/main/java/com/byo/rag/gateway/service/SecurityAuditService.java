package com.byo.rag.gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Security Audit Service for comprehensive authentication and security event logging.
 * 
 * <p>This service provides centralized audit logging for all security-related events
 * in the RAG system, ensuring comprehensive security monitoring and compliance
 * with security standards and regulations.
 * 
 * <p><strong>Audit Event Types:</strong>
 * <ul>
 *   <li>Authentication attempts (successful and failed)</li>
 *   <li>Authorization decisions</li>
 *   <li>Rate limiting violations</li>
 *   <li>Security validation failures</li>
 *   <li>Token refresh operations</li>
 *   <li>Suspicious activity detection</li>
 * </ul>
 * 
 * <p><strong>Compliance Features:</strong>
 * <ul>
 *   <li>Structured logging format for SIEM integration</li>
 *   <li>Sensitive data protection and masking</li>
 *   <li>Event correlation through trace IDs</li>
 *   <li>Tamper-evident logging with event integrity</li>
 * </ul>
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.0
 */
@Service
public class SecurityAuditService {

    /** Logger for security audit events - configured for dedicated audit log. */
    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("SECURITY_AUDIT");
    
    /** Standard logger for service operations. */
    private static final Logger logger = LoggerFactory.getLogger(SecurityAuditService.class);

    /** Cache for tracking suspicious activity patterns. */
    private final Map<String, SuspiciousActivityTracker> suspiciousActivityCache = new ConcurrentHashMap<>();

    /**
     * Logs successful authentication events.
     * 
     * @param userId the authenticated user ID
     * @param tenantId the user's tenant ID
     * @param clientIp the client IP address
     * @param userAgent the client user agent
     * @param authMethod the authentication method used
     */
    public void logAuthenticationSuccess(String userId, String tenantId, String clientIp, 
                                       String userAgent, String authMethod) {
        try (var mdcCloseable = MDC.putCloseable("event.type", "AUTH_SUCCESS")) {
            MDC.put("user.id", maskSensitiveData(userId));
            MDC.put("tenant.id", maskSensitiveData(tenantId));
            MDC.put("client.ip", clientIp);
            MDC.put("auth.method", authMethod);
            MDC.put("timestamp", Instant.now().toString());
            MDC.put("severity", "INFO");
            
            AUDIT_LOGGER.info("Authentication successful - User: {} Tenant: {} IP: {} Method: {} UserAgent: {}", 
                maskSensitiveData(userId), maskSensitiveData(tenantId), clientIp, authMethod, 
                maskUserAgent(userAgent));
        }
    }

    /**
     * Logs failed authentication attempts with security analysis.
     * 
     * @param attemptedUser the attempted username/email
     * @param clientIp the client IP address
     * @param userAgent the client user agent
     * @param failureReason the reason for authentication failure
     * @param requestPath the request path attempted
     */
    public void logAuthenticationFailure(String attemptedUser, String clientIp, String userAgent, 
                                       String failureReason, String requestPath) {
        try (var mdcCloseable = MDC.putCloseable("event.type", "AUTH_FAILURE")) {
            MDC.put("attempted.user", maskSensitiveData(attemptedUser));
            MDC.put("client.ip", clientIp);
            MDC.put("failure.reason", failureReason);
            MDC.put("request.path", requestPath);
            MDC.put("timestamp", Instant.now().toString());
            MDC.put("severity", "WARN");
            
            // Track suspicious activity
            trackSuspiciousActivity(clientIp, "AUTH_FAILURE");
            
            AUDIT_LOGGER.warn("Authentication failed - User: {} IP: {} Reason: {} Path: {} UserAgent: {}", 
                maskSensitiveData(attemptedUser), clientIp, failureReason, requestPath, 
                maskUserAgent(userAgent));
        }
    }

    /**
     * Logs rate limiting violations and potential abuse attempts.
     * 
     * @param clientIp the client IP address triggering rate limit
     * @param userId the user ID if available
     * @param rateLimitType the type of rate limit exceeded
     * @param requestsInPeriod number of requests in the time period
     * @param limitExceeded the limit that was exceeded
     * @param requestPath the request path attempted
     */
    public void logRateLimitViolation(String clientIp, String userId, String rateLimitType, 
                                    int requestsInPeriod, int limitExceeded, String requestPath) {
        try (var mdcCloseable = MDC.putCloseable("event.type", "RATE_LIMIT_VIOLATION")) {
            MDC.put("client.ip", clientIp);
            MDC.put("user.id", maskSensitiveData(userId));
            MDC.put("rate.limit.type", rateLimitType);
            MDC.put("requests.count", String.valueOf(requestsInPeriod));
            MDC.put("limit.exceeded", String.valueOf(limitExceeded));
            MDC.put("request.path", requestPath);
            MDC.put("timestamp", Instant.now().toString());
            MDC.put("severity", "ERROR");
            
            // Track as highly suspicious activity
            trackSuspiciousActivity(clientIp, "RATE_LIMIT_VIOLATION");
            
            AUDIT_LOGGER.error("Rate limit exceeded - IP: {} User: {} Type: {} Requests: {}/{} Path: {}", 
                clientIp, maskSensitiveData(userId), rateLimitType, requestsInPeriod, 
                limitExceeded, requestPath);
        }
    }

    /**
     * Logs authorization failures and access control violations.
     * 
     * @param userId the user ID attempting access
     * @param tenantId the user's tenant ID
     * @param requiredRole the role required for the resource
     * @param userRole the user's actual role
     * @param resourcePath the protected resource path
     * @param clientIp the client IP address
     */
    public void logAuthorizationFailure(String userId, String tenantId, String requiredRole, 
                                      String userRole, String resourcePath, String clientIp) {
        try (var mdcCloseable = MDC.putCloseable("event.type", "AUTHZ_FAILURE")) {
            MDC.put("user.id", maskSensitiveData(userId));
            MDC.put("tenant.id", maskSensitiveData(tenantId));
            MDC.put("required.role", requiredRole);
            MDC.put("user.role", userRole);
            MDC.put("resource.path", resourcePath);
            MDC.put("client.ip", clientIp);
            MDC.put("timestamp", Instant.now().toString());
            MDC.put("severity", "WARN");
            
            AUDIT_LOGGER.warn("Authorization failed - User: {} Tenant: {} Required: {} Actual: {} Resource: {} IP: {}", 
                maskSensitiveData(userId), maskSensitiveData(tenantId), requiredRole, userRole, 
                resourcePath, clientIp);
        }
    }

    /**
     * Logs token refresh operations for session management tracking.
     * 
     * @param userId the user ID refreshing the token
     * @param tenantId the user's tenant ID
     * @param clientIp the client IP address
     * @param oldTokenId the ID of the token being refreshed
     * @param newTokenId the ID of the new token issued
     */
    public void logTokenRefresh(String userId, String tenantId, String clientIp, 
                              String oldTokenId, String newTokenId) {
        try (var mdcCloseable = MDC.putCloseable("event.type", "TOKEN_REFRESH")) {
            MDC.put("user.id", maskSensitiveData(userId));
            MDC.put("tenant.id", maskSensitiveData(tenantId));
            MDC.put("client.ip", clientIp);
            MDC.put("old.token.id", maskSensitiveData(oldTokenId));
            MDC.put("new.token.id", maskSensitiveData(newTokenId));
            MDC.put("timestamp", Instant.now().toString());
            MDC.put("severity", "INFO");
            
            AUDIT_LOGGER.info("Token refreshed - User: {} Tenant: {} IP: {} OldToken: {} NewToken: {}", 
                maskSensitiveData(userId), maskSensitiveData(tenantId), clientIp, 
                maskSensitiveData(oldTokenId), maskSensitiveData(newTokenId));
        }
    }

    /**
     * Logs security validation failures for request processing.
     * 
     * @param validationType the type of validation that failed
     * @param validationDetails details about the validation failure
     * @param requestPath the request path
     * @param clientIp the client IP address
     * @param userId the user ID if available
     */
    public void logValidationFailure(String validationType, String validationDetails, 
                                   String requestPath, String clientIp, String userId) {
        try (var mdcCloseable = MDC.putCloseable("event.type", "VALIDATION_FAILURE")) {
            MDC.put("validation.type", validationType);
            MDC.put("validation.details", validationDetails);
            MDC.put("request.path", requestPath);
            MDC.put("client.ip", clientIp);
            MDC.put("user.id", maskSensitiveData(userId));
            MDC.put("timestamp", Instant.now().toString());
            MDC.put("severity", "WARN");
            
            trackSuspiciousActivity(clientIp, "VALIDATION_FAILURE");
            
            AUDIT_LOGGER.warn("Validation failed - Type: {} Details: {} Path: {} IP: {} User: {}", 
                validationType, validationDetails, requestPath, clientIp, maskSensitiveData(userId));
        }
    }

    /**
     * Logs suspicious activity detection and security alerts.
     * 
     * @param clientIp the client IP showing suspicious behavior
     * @param activityType the type of suspicious activity
     * @param severity the severity level of the activity
     * @param details additional details about the suspicious activity
     */
    public void logSuspiciousActivity(String clientIp, String activityType, String severity, String details) {
        try (var mdcCloseable = MDC.putCloseable("event.type", "SUSPICIOUS_ACTIVITY")) {
            MDC.put("client.ip", clientIp);
            MDC.put("activity.type", activityType);
            MDC.put("details", details);
            MDC.put("timestamp", Instant.now().toString());
            MDC.put("severity", severity);
            
            AUDIT_LOGGER.warn("Suspicious activity detected - IP: {} Type: {} Severity: {} Details: {}", 
                clientIp, activityType, severity, details);
        }
    }

    /**
     * Tracks suspicious activity patterns for automatic threat detection.
     * 
     * @param clientIp the client IP to track
     * @param eventType the type of suspicious event
     */
    private void trackSuspiciousActivity(String clientIp, String eventType) {
        try {
            SuspiciousActivityTracker tracker = suspiciousActivityCache.computeIfAbsent(
                clientIp, k -> new SuspiciousActivityTracker());
            
            tracker.recordEvent(eventType);
            
            // Check if this IP should be flagged for blocking
            if (tracker.isSuspicious()) {
                logSuspiciousActivity(clientIp, "PATTERN_DETECTED", "HIGH", 
                    String.format("Multiple security violations: %d events in last hour", 
                        tracker.getEventCount()));
            }
        } catch (Exception e) {
            logger.error("Error tracking suspicious activity for IP: {}", clientIp, e);
        }
    }

    /**
     * Masks sensitive data for audit logging while preserving useful information.
     * 
     * @param data the sensitive data to mask
     * @return masked version of the data
     */
    private String maskSensitiveData(String data) {
        if (data == null || data.length() <= 4) {
            return "****";
        }
        return data.substring(0, 2) + "****" + data.substring(data.length() - 2);
    }

    /**
     * Masks user agent strings to remove potentially identifying information.
     * 
     * @param userAgent the user agent string to mask
     * @return masked user agent with essential information preserved
     */
    private String maskUserAgent(String userAgent) {
        if (userAgent == null) {
            return "unknown";
        }
        // Keep browser type but remove version details
        return userAgent.replaceAll("[0-9]+\\.[0-9]+\\.[0-9]+", "X.X.X")
                        .replaceAll("Version/[0-9.]+", "Version/X.X");
    }

    /**
     * Logs general security events for system monitoring.
     * 
     * @param eventSource the source of the security event
     * @param eventType the type of security event
     * @param subject the subject of the event (IP, user, etc.)
     * @param details additional details about the event
     */
    public void logSecurityEvent(String eventSource, String eventType, String subject, String details) {
        try (var mdcCloseable = MDC.putCloseable("event.type", "SECURITY_EVENT")) {
            MDC.put("event.source", eventSource);
            MDC.put("event.type", eventType);
            MDC.put("subject", maskSensitiveData(subject));
            MDC.put("details", details);
            MDC.put("timestamp", Instant.now().toString());
            MDC.put("severity", "INFO");
            
            AUDIT_LOGGER.info("Security event - Source: {} Type: {} Subject: {} Details: {}", 
                eventSource, eventType, maskSensitiveData(subject), details);
        }
    }

    /**
     * Logs authentication events with comprehensive details.
     * 
     * @param userId the user ID involved
     * @param tenantId the tenant ID
     * @param eventType the authentication event type
     * @param clientIp the client IP address
     * @param userAgent the user agent
     * @param details additional event details
     */
    public void logAuthenticationEvent(String userId, String tenantId, String eventType, 
                                     String clientIp, String userAgent, String details) {
        try (var mdcCloseable = MDC.putCloseable("event.type", "AUTH_EVENT")) {
            MDC.put("user.id", maskSensitiveData(userId));
            MDC.put("tenant.id", maskSensitiveData(tenantId));
            MDC.put("event.type", eventType);
            MDC.put("client.ip", clientIp);
            MDC.put("details", details);
            MDC.put("timestamp", Instant.now().toString());
            MDC.put("severity", "INFO");
            
            AUDIT_LOGGER.info("Authentication event - User: {} Tenant: {} Type: {} IP: {} Details: {} UserAgent: {}", 
                maskSensitiveData(userId), maskSensitiveData(tenantId), eventType, clientIp, details, 
                maskUserAgent(userAgent));
        }
    }

    /**
     * Logs security incidents for immediate attention.
     * 
     * @param userId the user ID involved (may be null)
     * @param incidentType the type of security incident
     * @param severity the severity level
     * @param clientIp the client IP address
     * @param details incident details
     */
    public void logSecurityIncident(String userId, String incidentType, String severity, 
                                  String clientIp, String details) {
        try (var mdcCloseable = MDC.putCloseable("event.type", "SECURITY_INCIDENT")) {
            MDC.put("user.id", maskSensitiveData(userId));
            MDC.put("incident.type", incidentType);
            MDC.put("client.ip", clientIp);
            MDC.put("details", details);
            MDC.put("timestamp", Instant.now().toString());
            MDC.put("severity", severity);
            
            if ("HIGH".equals(severity) || "CRITICAL".equals(severity)) {
                AUDIT_LOGGER.error("Security incident - User: {} Type: {} IP: {} Severity: {} Details: {}", 
                    maskSensitiveData(userId), incidentType, clientIp, severity, details);
            } else {
                AUDIT_LOGGER.warn("Security incident - User: {} Type: {} IP: {} Severity: {} Details: {}", 
                    maskSensitiveData(userId), incidentType, clientIp, severity, details);
            }
        }
    }

    /**
     * Detects suspicious activity patterns and triggers alerts.
     * 
     * @param clientIp the client IP to analyze
     * @param activityContext the context of the activity
     * @return true if suspicious activity detected, false otherwise
     */
    public boolean detectSuspiciousActivity(String clientIp, String activityContext) {
        try {
            SuspiciousActivityTracker tracker = suspiciousActivityCache.get(clientIp);
            if (tracker != null && tracker.isSuspicious()) {
                logSuspiciousActivity(clientIp, "PATTERN_DETECTED", "HIGH", 
                    String.format("Suspicious activity pattern detected in context: %s", activityContext));
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("Error detecting suspicious activity for IP: {}", clientIp, e);
            return false;
        }
    }

    /**
     * Inner class to track suspicious activity patterns per IP address.
     */
    private static class SuspiciousActivityTracker {
        private static final int SUSPICIOUS_THRESHOLD = 10;
        private static final long TIME_WINDOW_MS = 3600000; // 1 hour
        
        private final Map<String, Integer> eventCounts = new ConcurrentHashMap<>();
        private volatile long lastResetTime = System.currentTimeMillis();
        private volatile int totalEvents = 0;

        public void recordEvent(String eventType) {
            long now = System.currentTimeMillis();
            
            // Reset counters if time window has passed
            if (now - lastResetTime > TIME_WINDOW_MS) {
                eventCounts.clear();
                totalEvents = 0;
                lastResetTime = now;
            }
            
            eventCounts.merge(eventType, 1, Integer::sum);
            totalEvents++;
        }

        public boolean isSuspicious() {
            return totalEvents >= SUSPICIOUS_THRESHOLD;
        }

        public int getEventCount() {
            return totalEvents;
        }
    }
}
package com.byo.rag.gateway.security;

import com.byo.rag.gateway.service.AdvancedRateLimitingService;
import com.byo.rag.gateway.service.SecurityAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hierarchical Rate Limiting Service for RAG Gateway.
 * 
 * <p>This service implements multi-level rate limiting with hierarchical controls
 * that cascade from global to tenant to user to endpoint levels. It provides
 * sophisticated rate limiting strategies with adaptive thresholds and system
 * load awareness for enterprise-grade API gateway operations.
 * 
 * <p><strong>Hierarchical Rate Limiting Levels:</strong>
 * <ul>
 *   <li><strong>Level 1 - Global</strong>: System-wide rate limits for overall protection</li>
 *   <li><strong>Level 2 - Tenant</strong>: Per-tenant rate limits for multi-tenant isolation</li>
 *   <li><strong>Level 3 - User</strong>: Per-user rate limits for individual user protection</li>
 *   <li><strong>Level 4 - Endpoint</strong>: Per-endpoint rate limits for resource protection</li>
 *   <li><strong>Level 5 - IP</strong>: Per-IP rate limits for abuse prevention</li>
 * </ul>
 * 
 * <p><strong>Advanced Features:</strong>
 * <ul>
 *   <li><strong>Adaptive Thresholds</strong>: Dynamic rate limits based on system load</li>
 *   <li><strong>Burst Capacity</strong>: Token bucket algorithm for handling traffic spikes</li>
 *   <li><strong>Whitelisting</strong>: Bypass controls for trusted sources</li>
 *   <li><strong>Geographic Controls</strong>: Location-based rate limiting</li>
 *   <li><strong>Time-based Controls</strong>: Different limits for peak vs off-peak hours</li>
 * </ul>
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.1
 */
@Service
public class HierarchicalRateLimitingService {

    /** Advanced rate limiting service for core functionality. */
    private final AdvancedRateLimitingService advancedRateLimitingService;

    /** Security audit service for logging rate limit events. */
    private final SecurityAuditService securityAuditService;

    /** Global rate limit multiplier. */
    @Value("${security.rate-limiting.global.multiplier:1000}")
    private int globalMultiplier;

    /** Tenant rate limit multiplier. */
    @Value("${security.rate-limiting.tenant.multiplier:100}")
    private int tenantMultiplier;

    /** User rate limit multiplier. */
    @Value("${security.rate-limiting.user.multiplier:10}")
    private int userMultiplier;

    /** Enable adaptive rate limiting. */
    @Value("${security.rate-limiting.adaptive.enabled:true}")
    private boolean adaptiveEnabled;

    /** System load threshold for adaptive limiting. */
    @Value("${security.rate-limiting.adaptive.load-threshold:0.8}")
    private double loadThreshold;

    /** Whitelist of IPs that bypass rate limiting. */
    private final Map<String, String> whitelist = new ConcurrentHashMap<>();

    /** Current system load factor (0.0 to 1.0). */
    private volatile double currentLoadFactor = 0.0;

    /**
     * Hierarchical rate limit result with detailed decision information.
     */
    public static class HierarchicalRateLimitResult {
        private final boolean allowed;
        private final String blockedLevel;
        private final int globalRequests;
        private final int tenantRequests;
        private final int userRequests;
        private final int endpointRequests;
        private final int ipRequests;
        private final Duration retryAfter;
        private final String reason;
        private final boolean adaptivelyLimited;

        public HierarchicalRateLimitResult(boolean allowed, String blockedLevel, 
                                         int globalRequests, int tenantRequests, int userRequests,
                                         int endpointRequests, int ipRequests, Duration retryAfter, 
                                         String reason, boolean adaptivelyLimited) {
            this.allowed = allowed;
            this.blockedLevel = blockedLevel;
            this.globalRequests = globalRequests;
            this.tenantRequests = tenantRequests;
            this.userRequests = userRequests;
            this.endpointRequests = endpointRequests;
            this.ipRequests = ipRequests;
            this.retryAfter = retryAfter;
            this.reason = reason;
            this.adaptivelyLimited = adaptivelyLimited;
        }

        // Getters
        public boolean isAllowed() { return allowed; }
        public String getBlockedLevel() { return blockedLevel; }
        public int getGlobalRequests() { return globalRequests; }
        public int getTenantRequests() { return tenantRequests; }
        public int getUserRequests() { return userRequests; }
        public int getEndpointRequests() { return endpointRequests; }
        public int getIpRequests() { return ipRequests; }
        public Duration getRetryAfter() { return retryAfter; }
        public String getReason() { return reason; }
        public boolean isAdaptivelyLimited() { return adaptivelyLimited; }
    }

    /**
     * Rate limiting context containing request information.
     */
    public static class RateLimitContext {
        private final String clientIp;
        private final String userId;
        private final String tenantId;
        private final String endpoint;
        private final String requestPath;
        private final AdvancedRateLimitingService.RateLimitType rateLimitType;

        public RateLimitContext(String clientIp, String userId, String tenantId, 
                              String endpoint, String requestPath, 
                              AdvancedRateLimitingService.RateLimitType rateLimitType) {
            this.clientIp = clientIp;
            this.userId = userId;
            this.tenantId = tenantId;
            this.endpoint = endpoint;
            this.requestPath = requestPath;
            this.rateLimitType = rateLimitType;
        }

        // Getters
        public String getClientIp() { return clientIp; }
        public String getUserId() { return userId; }
        public String getTenantId() { return tenantId; }
        public String getEndpoint() { return endpoint; }
        public String getRequestPath() { return requestPath; }
        public AdvancedRateLimitingService.RateLimitType getRateLimitType() { return rateLimitType; }
    }

    /**
     * Constructs hierarchical rate limiting service.
     * 
     * @param advancedRateLimitingService advanced rate limiting service
     * @param securityAuditService security audit service
     */
    @Autowired
    public HierarchicalRateLimitingService(
            AdvancedRateLimitingService advancedRateLimitingService,
            SecurityAuditService securityAuditService) {
        this.advancedRateLimitingService = advancedRateLimitingService;
        this.securityAuditService = securityAuditService;
        
        // Initialize default whitelist (in production, load from configuration)
        initializeWhitelist();
    }

    /**
     * Performs hierarchical rate limit check across all levels.
     * 
     * <p>This method checks rate limits in hierarchical order, starting from
     * global limits and cascading down to specific limits. If any level
     * blocks the request, the check stops and returns the blocking result.
     * 
     * @param context rate limiting context
     * @return hierarchical rate limit result
     */
    public Mono<HierarchicalRateLimitResult> checkHierarchicalRateLimit(RateLimitContext context) {
        // Check if IP is whitelisted
        if (isWhitelisted(context.getClientIp())) {
            return Mono.just(createAllowedResult("WHITELISTED", "IP is whitelisted"));
        }

        // Apply adaptive limiting if enabled
        AdvancedRateLimitingService.RateLimitType effectiveType = getEffectiveRateLimitType(context.getRateLimitType());

        // Level 1: Global rate limiting
        return checkGlobalRateLimit(effectiveType)
            .flatMap(globalResult -> {
                if (!globalResult.isAllowed()) {
                    return Mono.just(createBlockedResult("GLOBAL", globalResult, 
                        "Global rate limit exceeded"));
                }

                // Level 2: Tenant rate limiting (if tenant context available)
                if (context.getTenantId() != null) {
                    return checkTenantRateLimit(context.getTenantId(), effectiveType, context)
                        .flatMap(tenantResult -> {
                            if (!tenantResult.isAllowed()) {
                                return Mono.just(createBlockedResult("TENANT", tenantResult, 
                                    "Tenant rate limit exceeded"));
                            }

                            // Continue to user level
                            return checkUserAndBelowLevels(context, effectiveType, globalResult, tenantResult);
                        });
                } else {
                    // Skip tenant level, go to user level
                    return checkUserAndBelowLevels(context, effectiveType, globalResult, null);
                }
            });
    }

    /**
     * Checks user, endpoint, and IP rate limits.
     * 
     * @param context rate limiting context
     * @param effectiveType effective rate limit type
     * @param globalResult global rate limit result
     * @param tenantResult tenant rate limit result (may be null)
     * @return hierarchical rate limit result
     */
    private Mono<HierarchicalRateLimitResult> checkUserAndBelowLevels(
            RateLimitContext context, 
            AdvancedRateLimitingService.RateLimitType effectiveType,
            AdvancedRateLimitingService.RateLimitResult globalResult,
            AdvancedRateLimitingService.RateLimitResult tenantResult) {

        // Level 3: User rate limiting (if user context available)
        if (context.getUserId() != null) {
            return advancedRateLimitingService.checkUserRateLimit(
                context.getUserId(), context.getTenantId(), context.getClientIp(), 
                effectiveType, context.getRequestPath())
                .flatMap(userResult -> {
                    if (!userResult.isAllowed()) {
                        return Mono.just(createBlockedResult("USER", userResult, 
                            "User rate limit exceeded"));
                    }

                    // Level 4: Endpoint rate limiting
                    return checkEndpointAndIpLevels(context, effectiveType, globalResult, 
                        tenantResult, userResult);
                });
        } else {
            // Skip user level, go to endpoint level
            return checkEndpointAndIpLevels(context, effectiveType, globalResult, 
                tenantResult, null);
        }
    }

    /**
     * Checks endpoint and IP rate limits.
     * 
     * @param context rate limiting context
     * @param effectiveType effective rate limit type
     * @param globalResult global rate limit result
     * @param tenantResult tenant rate limit result (may be null)
     * @param userResult user rate limit result (may be null)
     * @return hierarchical rate limit result
     */
    private Mono<HierarchicalRateLimitResult> checkEndpointAndIpLevels(
            RateLimitContext context,
            AdvancedRateLimitingService.RateLimitType effectiveType,
            AdvancedRateLimitingService.RateLimitResult globalResult,
            AdvancedRateLimitingService.RateLimitResult tenantResult,
            AdvancedRateLimitingService.RateLimitResult userResult) {

        // Level 4: Endpoint rate limiting
        return advancedRateLimitingService.checkEndpointRateLimit(
            context.getEndpoint(), context.getClientIp(), context.getUserId(), effectiveType)
            .flatMap(endpointResult -> {
                if (!endpointResult.isAllowed()) {
                    return Mono.just(createBlockedResult("ENDPOINT", endpointResult, 
                        "Endpoint rate limit exceeded"));
                }

                // Level 5: IP rate limiting
                return advancedRateLimitingService.checkIPRateLimit(
                    context.getClientIp(), effectiveType, context.getRequestPath())
                    .map(ipResult -> {
                        if (!ipResult.isAllowed()) {
                            return createBlockedResult("IP", ipResult, 
                                "IP rate limit exceeded");
                        }

                        // All levels passed
                        return createSuccessResult(globalResult, tenantResult, userResult, 
                            endpointResult, ipResult, effectiveType != context.getRateLimitType());
                    });
            });
    }

    /**
     * Checks global rate limit.
     * 
     * @param rateLimitType rate limit type
     * @return rate limit result
     */
    private Mono<AdvancedRateLimitingService.RateLimitResult> checkGlobalRateLimit(
            AdvancedRateLimitingService.RateLimitType rateLimitType) {
        return advancedRateLimitingService.checkGlobalRateLimit(rateLimitType, globalMultiplier);
    }

    /**
     * Checks tenant-specific rate limit.
     * 
     * @param tenantId tenant identifier
     * @param rateLimitType rate limit type
     * @param context rate limiting context
     * @return rate limit result
     */
    private Mono<AdvancedRateLimitingService.RateLimitResult> checkTenantRateLimit(
            String tenantId, 
            AdvancedRateLimitingService.RateLimitType rateLimitType,
            RateLimitContext context) {
        
        // Use a tenant-specific key with the advanced rate limiting service
        return advancedRateLimitingService.checkUserRateLimit(
            "tenant:" + tenantId, tenantId, context.getClientIp(), 
            rateLimitType, context.getRequestPath());
    }

    /**
     * Gets effective rate limit type based on adaptive settings.
     * 
     * @param originalType original rate limit type
     * @return effective rate limit type
     */
    private AdvancedRateLimitingService.RateLimitType getEffectiveRateLimitType(
            AdvancedRateLimitingService.RateLimitType originalType) {
        
        if (!adaptiveEnabled || currentLoadFactor < loadThreshold) {
            return originalType;
        }

        // Under high load, apply more restrictive limits
        switch (originalType) {
            case API_GENERAL:
                return AdvancedRateLimitingService.RateLimitType.API_SEARCH; // More restrictive
            case API_SEARCH:
                return AdvancedRateLimitingService.RateLimitType.API_UPLOAD; // Even more restrictive
            case API_UPLOAD:
                return AdvancedRateLimitingService.RateLimitType.TOKEN_REFRESH; // Most restrictive
            default:
                return originalType; // Keep original for authentication and admin operations
        }
    }

    /**
     * Checks if IP address is whitelisted.
     * 
     * @param clientIp client IP address
     * @return true if whitelisted
     */
    private boolean isWhitelisted(String clientIp) {
        return whitelist.containsKey(clientIp);
    }

    /**
     * Adds IP address to whitelist.
     * 
     * @param clientIp client IP address
     * @param reason reason for whitelisting
     */
    public void addToWhitelist(String clientIp, String reason) {
        whitelist.put(clientIp, reason);
        securityAuditService.logSecurityEvent("SYSTEM", "IP_WHITELISTED", clientIp, 
            "IP added to whitelist: " + reason);
    }

    /**
     * Removes IP address from whitelist.
     * 
     * @param clientIp client IP address
     */
    public void removeFromWhitelist(String clientIp) {
        String reason = whitelist.remove(clientIp);
        if (reason != null) {
            securityAuditService.logSecurityEvent("SYSTEM", "IP_WHITELIST_REMOVED", clientIp, 
                "IP removed from whitelist");
        }
    }

    /**
     * Updates current system load factor for adaptive rate limiting.
     * 
     * @param loadFactor load factor (0.0 to 1.0)
     */
    public void updateSystemLoad(double loadFactor) {
        this.currentLoadFactor = Math.max(0.0, Math.min(1.0, loadFactor));
    }

    /**
     * Creates successful hierarchical rate limit result.
     */
    private HierarchicalRateLimitResult createSuccessResult(
            AdvancedRateLimitingService.RateLimitResult globalResult,
            AdvancedRateLimitingService.RateLimitResult tenantResult,
            AdvancedRateLimitingService.RateLimitResult userResult,
            AdvancedRateLimitingService.RateLimitResult endpointResult,
            AdvancedRateLimitingService.RateLimitResult ipResult,
            boolean adaptivelyLimited) {
        
        return new HierarchicalRateLimitResult(
            true, null,
            globalResult.getCurrentRequests(),
            tenantResult != null ? tenantResult.getCurrentRequests() : 0,
            userResult != null ? userResult.getCurrentRequests() : 0,
            endpointResult.getCurrentRequests(),
            ipResult.getCurrentRequests(),
            Duration.ZERO,
            "Request allowed at all levels",
            adaptivelyLimited
        );
    }

    /**
     * Creates blocked hierarchical rate limit result.
     */
    private HierarchicalRateLimitResult createBlockedResult(
            String blockedLevel, 
            AdvancedRateLimitingService.RateLimitResult blockingResult, 
            String reason) {
        
        return new HierarchicalRateLimitResult(
            false, blockedLevel,
            blockingResult.getCurrentRequests(), 0, 0, 0, 0,
            blockingResult.getRetryAfter(),
            reason,
            false
        );
    }

    /**
     * Creates allowed result for whitelisted requests.
     */
    private HierarchicalRateLimitResult createAllowedResult(String level, String reason) {
        return new HierarchicalRateLimitResult(
            true, null, 0, 0, 0, 0, 0,
            Duration.ZERO, reason, false
        );
    }

    /**
     * Initializes default whitelist.
     */
    private void initializeWhitelist() {
        // Add default whitelisted IPs (localhost, health check services, etc.)
        whitelist.put("127.0.0.1", "Localhost");
        whitelist.put("::1", "IPv6 Localhost");
        // Add more default entries as needed
    }
}
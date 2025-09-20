package com.byo.rag.gateway.config;

import com.byo.rag.gateway.service.*;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;

/**
 * Test configuration for Security components.
 * 
 * <p>This configuration provides mock implementations of Redis-dependent
 * security services to enable testing without external Redis dependency.
 */
@TestConfiguration
public class TestSecurityConfig {

    /**
     * Provides a mock ReactiveRedisTemplate for testing.
     */
    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public ReactiveRedisTemplate<String, Object> mockReactiveRedisTemplate() {
        ReactiveRedisTemplate<String, Object> mockTemplate = Mockito.mock(ReactiveRedisTemplate.class);
        // Configure basic mock behavior
        Mockito.when(mockTemplate.hasKey(Mockito.anyString())).thenReturn(Mono.just(false));
        return mockTemplate;
    }

    /**
     * Provides a mock ReactiveStringRedisTemplate for testing.
     */
    @Bean
    @Primary
    public ReactiveStringRedisTemplate mockReactiveStringRedisTemplate() {
        ReactiveStringRedisTemplate mockTemplate = Mockito.mock(ReactiveStringRedisTemplate.class);
        // Configure basic mock behavior
        Mockito.when(mockTemplate.hasKey(Mockito.anyString())).thenReturn(Mono.just(false));
        return mockTemplate;
    }

    // JwtValidationService will use the test secret from application properties

    /**
     * Provides mock AdvancedRateLimitingService for testing.
     */
    @Bean
    @Primary
    public AdvancedRateLimitingService mockAdvancedRateLimitingService() {
        AdvancedRateLimitingService mockService = Mockito.mock(AdvancedRateLimitingService.class);
        
        // Create allowed rate limit result
        AdvancedRateLimitingService.RateLimitResult allowedResult = 
            new AdvancedRateLimitingService.RateLimitResult(true, 1, 100, Duration.ZERO, "Test allowed");
        
        // Configure to allow all requests by default in tests
        Mockito.when(mockService.checkIPRateLimit(Mockito.anyString(), Mockito.any(), Mockito.anyString()))
               .thenReturn(Mono.just(allowedResult));
        Mockito.when(mockService.checkUserRateLimit(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.anyString()))
               .thenReturn(Mono.just(allowedResult));
        Mockito.when(mockService.checkEndpointRateLimit(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
               .thenReturn(Mono.just(allowedResult));
        return mockService;
    }

    /**
     * Provides mock RequestValidationService for testing.
     */
    @Bean
    @Primary
    public RequestValidationService mockRequestValidationService() {
        RequestValidationService mockService = Mockito.mock(RequestValidationService.class);
        
        // Create valid validation result
        RequestValidationService.ValidationResult validResult = 
            new RequestValidationService.ValidationResult(true, Collections.emptyList(), "/test", new HashMap<>());
        
        // Configure to allow all requests by default in tests
        Mockito.when(mockService.validateRequest(Mockito.any(), Mockito.anyString()))
               .thenReturn(Mono.just(validResult));
        return mockService;
    }

    /**
     * Provides mock SecurityAuditService for testing.
     */
    @Bean
    @Primary
    public SecurityAuditService mockSecurityAuditService() {
        SecurityAuditService mockService = Mockito.mock(SecurityAuditService.class);
        // Configure to succeed silently for all audit operations
        Mockito.doNothing().when(mockService).logAuthenticationSuccess(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.doNothing().when(mockService).logAuthenticationFailure(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.doNothing().when(mockService).logRateLimitViolation(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyString());
        
        // Mock new methods added during implementation
        Mockito.doNothing().when(mockService).logSecurityEvent(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.doNothing().when(mockService).logAuthenticationEvent(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.doNothing().when(mockService).logSecurityIncident(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.when(mockService.detectSuspiciousActivity(Mockito.anyString(), Mockito.anyString())).thenReturn(false);
        
        return mockService;
    }

    /**
     * Provides mock SessionManagementService for testing.
     */
    @Bean
    @Primary
    public SessionManagementService mockSessionManagementService() {
        SessionManagementService mockService = Mockito.mock(SessionManagementService.class);
        
        // Mock the methods we added to SessionManagementService
        Mockito.when(mockService.isTokenBlacklisted(Mockito.anyString())).thenReturn(false);
        Mockito.when(mockService.validateSession(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        Mockito.when(mockService.isRefreshTokenUsed(Mockito.anyString())).thenReturn(false);
        Mockito.when(mockService.getRefreshCount(Mockito.anyString(), Mockito.any(Duration.class))).thenReturn(0);
        Mockito.when(mockService.updateSessionActivity(Mockito.anyString())).thenReturn(Mono.empty());
        Mockito.when(mockService.invalidateAllUserSessions(Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.empty());
        Mockito.doNothing().when(mockService).markRefreshTokenUsed(Mockito.anyString());
        
        return mockService;
    }

    /**
     * Provides mock HierarchicalRateLimitingService for testing.
     */
    @Bean
    @Primary
    public com.byo.rag.gateway.security.HierarchicalRateLimitingService mockHierarchicalRateLimitingService() {
        return Mockito.mock(com.byo.rag.gateway.security.HierarchicalRateLimitingService.class);
    }

    /**
     * Provides mock InputSanitizationService for testing.
     */
    @Bean
    @Primary
    public com.byo.rag.gateway.security.InputSanitizationService mockInputSanitizationService() {
        com.byo.rag.gateway.security.InputSanitizationService mockService = 
            Mockito.mock(com.byo.rag.gateway.security.InputSanitizationService.class);
        
        // Mock to return sanitized versions of input strings
        Mockito.when(mockService.sanitizeForHtml(Mockito.anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(mockService.sanitizeForSql(Mockito.anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(mockService.normalizePath(Mockito.anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(mockService.safeUrlDecode(Mockito.anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(mockService.sanitizeJson(Mockito.anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(mockService.validateLength(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);
        Mockito.when(mockService.validateCharacters(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        
        return mockService;
    }
}
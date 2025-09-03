/**
 * Configuration classes for the authentication service.
 * 
 * <p>This package contains Spring configuration classes that set up
 * the authentication service infrastructure, including security configuration,
 * JWT settings, database configuration, and integration with external systems.
 * All configurations support multi-tenant architecture and enterprise security requirements.</p>
 * 
 * <h2>Configuration Categories</h2>
 * <p>Configurations are organized by functional area:</p>
 * <ul>
 *   <li><strong>Security Configuration</strong> - Spring Security setup with JWT and multi-tenant support</li>
 *   <li><strong>JWT Configuration</strong> - Token generation, validation, and management settings</li>
 *   <li><strong>Database Configuration</strong> - JPA configuration for authentication data</li>
 *   <li><strong>Cache Configuration</strong> - Redis configuration for session and token caching</li>
 *   <li><strong>Integration Configuration</strong> - External service integration settings</li>
 * </ul>
 * 
 * <h2>Security Configuration</h2>
 * <p>Comprehensive Spring Security setup:</p>
 * <ul>
 *   <li><strong>Authentication Providers</strong> - Custom providers for multi-tenant authentication</li>
 *   <li><strong>Security Filters</strong> - JWT, CORS, and tenant context filters</li>
 *   <li><strong>Access Control</strong> - Method-level security and resource-based authorization</li>
 *   <li><strong>Session Management</strong> - Stateless session configuration with JWT</li>
 *   <li><strong>CORS Configuration</strong> - Cross-origin resource sharing policies</li>
 * </ul>
 * 
 * <h2>JWT Configuration</h2>
 * <p>Advanced JWT token management configuration:</p>
 * <ul>
 *   <li><strong>Token Generation</strong> - Secure token generation with configurable algorithms</li>
 *   <li><strong>Token Validation</strong> - Multi-layer validation with signature verification</li>
 *   <li><strong>Key Management</strong> - Secure key rotation and management</li>
 *   <li><strong>Claims Configuration</strong> - Custom claims for user and tenant context</li>
 *   <li><strong>Token Lifecycle</strong> - Access and refresh token expiry settings</li>
 * </ul>
 * 
 * <h2>Multi-Tenant Configuration</h2>
 * <p>Complete multi-tenant infrastructure setup:</p>
 * <ul>
 *   <li><strong>Tenant Context</strong> - Tenant context propagation and validation</li>
 *   <li><strong>Data Source Configuration</strong> - Tenant-aware data source routing</li>
 *   <li><strong>Security Isolation</strong> - Complete security isolation between tenants</li>
 *   <li><strong>Configuration Properties</strong> - Tenant-specific configuration support</li>
 * </ul>
 * 
 * <h2>Database Configuration</h2>
 * <p>Optimized database configuration for authentication:</p>
 * <ul>
 *   <li><strong>Connection Pooling</strong> - HikariCP configuration for authentication workloads</li>
 *   <li><strong>JPA Configuration</strong> - Entity scanning and performance tuning</li>
 *   <li><strong>Transaction Management</strong> - Multi-datasource transaction coordination</li>
 *   <li><strong>Audit Configuration</strong> - Automatic audit field population</li>
 * </ul>
 * 
 * <h2>Caching Configuration</h2>
 * <p>Redis-based caching for authentication performance:</p>
 * <ul>
 *   <li><strong>Session Caching</strong> - User session storage and retrieval</li>
 *   <li><strong>Token Caching</strong> - JWT token blacklist and validation cache</li>
 *   <li><strong>User Caching</strong> - Frequently accessed user data caching</li>
 *   <li><strong>Rate Limiting</strong> - Redis-based rate limiting for security endpoints</li>
 * </ul>
 * 
 * <h2>Integration Configuration</h2>
 * <p>External system integration setup:</p>
 * <ul>
 *   <li><strong>Email Configuration</strong> - SMTP configuration for authentication emails</li>
 *   <li><strong>SMS Configuration</strong> - SMS provider integration for MFA</li>
 *   <li><strong>SSO Configuration</strong> - SAML and OAuth2 provider integration</li>
 *   <li><strong>LDAP Configuration</strong> - Enterprise directory integration</li>
 * </ul>
 * 
 * <h2>Monitoring Configuration</h2>
 * <p>Comprehensive monitoring and observability:</p>
 * <ul>
 *   <li><strong>Metrics Configuration</strong> - Authentication metrics with Micrometer</li>
 *   <li><strong>Health Checks</strong> - Custom health indicators for dependencies</li>
 *   <li><strong>Audit Configuration</strong> - Security event logging and compliance</li>
 *   <li><strong>Tracing Configuration</strong> - Distributed tracing for authentication flows</li>
 * </ul>
 * 
 * <h2>Performance Tuning</h2>
 * <p>Configuration optimizations for high-performance authentication:</p>
 * <ul>
 *   <li><strong>Thread Pool Configuration</strong> - Async processing optimization</li>
 *   <li><strong>Connection Tuning</strong> - Database and Redis connection optimization</li>
 *   <li><strong>Memory Configuration</strong> - JVM memory tuning for authentication workloads</li>
 *   <li><strong>Serialization</strong> - Efficient serialization for cached objects</li>
 * </ul>
 * 
 * <h2>Environment Configuration</h2>
 * <p>Support for multiple deployment environments:</p>
 * <ul>
 *   <li><strong>Profile Configuration</strong> - Environment-specific authentication settings</li>
 *   <li><strong>External Configuration</strong> - Configuration server integration</li>
 *   <li><strong>Secret Management</strong> - Secure handling of authentication secrets</li>
 *   <li><strong>Feature Flags</strong> - Runtime feature toggling for authentication features</li>
 * </ul>
 * 
 * <h2>Configuration Properties Example</h2>
 * <pre>{@code
 * # application.yml
 * rag:
 *   auth:
 *     jwt:
 *       secret: ${JWT_SECRET:your-secret-key}
 *       access-token-expiry: PT15M
 *       refresh-token-expiry: P7D
 *       issuer: enterprise-rag-auth
 *       audience: enterprise-rag-api
 *     security:
 *       bcrypt-rounds: 12
 *       max-login-attempts: 5
 *       lockout-duration: PT30M
 *       password-policy:
 *         min-length: 12
 *         require-uppercase: true
 *         require-lowercase: true
 *         require-digits: true
 *         require-special-chars: true
 *     rate-limiting:
 *       enabled: true
 *       login-attempts: 5
 *       time-window: PT1M
 *     mfa:
 *       enabled: true
 *       totp-issuer: Enterprise RAG
 * }</pre>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Configuration
 * @EnableConfigurationProperties({AuthProperties.class, JwtProperties.class})
 * @ConditionalOnProperty(name = "rag.auth.enabled", havingValue = "true", matchIfMissing = true)
 * public class AuthServiceConfiguration {
 *     
 *     private final AuthProperties authProperties;
 *     private final JwtProperties jwtProperties;
 *     
 *     @Bean
 *     @ConditionalOnMissingBean
 *     public JwtEncoder jwtEncoder() {
 *         return new NimbusJwtEncoder(jwkSource());
 *     }
 *     
 *     @Bean
 *     @ConditionalOnMissingBean  
 *     public JwtDecoder jwtDecoder() {
 *         return NimbusJwtDecoder.withSecretKey(getSecretKey())
 *             .jwtProcessorCustomizer(this::customizeJwtProcessor)
 *             .build();
 *     }
 *     
 *     @Bean
 *     public PasswordEncoder passwordEncoder() {
 *         return new BCryptPasswordEncoder(authProperties.getSecurity().getBcryptRounds());
 *     }
 *     
 *     @Bean
 *     @ConditionalOnProperty(name = "rag.auth.rate-limiting.enabled", havingValue = "true")
 *     public RateLimitingService rateLimitingService(RedisTemplate<String, Object> redisTemplate) {
 *         return new RedisRateLimitingService(redisTemplate, authProperties.getRateLimiting());
 *     }
 * }
 * }</pre>
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 * @see org.springframework.context.annotation Spring configuration annotations
 * @see org.springframework.boot.context.properties Configuration properties
 * @see org.springframework.security.config Security configuration
 * @see com.byo.rag.shared.config Shared configuration classes
 */
package com.byo.rag.auth.config;
package com.byo.rag.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Enhanced Security Configuration for the RAG Gateway.
 * 
 * <p>This configuration implements comprehensive security measures including
 * CORS policies, security headers, and integration with advanced security
 * filters for enterprise-grade protection.
 * 
 * <p><strong>Security Features:</strong>
 * <ul>
 *   <li>Strict CORS policy with environment-specific origins</li>
 *   <li>Comprehensive security headers (HSTS, CSP, etc.)</li>
 *   <li>Content Security Policy for XSS prevention</li>
 *   <li>Frame protection against clickjacking</li>
 *   <li>Content type sniffing prevention</li>
 *   <li>Referrer policy for privacy protection</li>
 * </ul>
 * 
 * <p><strong>OWASP Compliance:</strong>
 * Implements security controls from OWASP ASVS and Top 10:
 * <ul>
 *   <li>V14.4 - HTTP Security Headers</li>
 *   <li>V14.5 - HTTP Request Header Validation</li>
 *   <li>A05:2021 - Security Misconfiguration</li>
 *   <li>A07:2021 - Cross-Site Scripting (XSS)</li>
 * </ul>
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.0
 */
@Configuration
@EnableWebFluxSecurity
public class EnhancedSecurityConfig {

    /** Allowed origins for CORS from configuration. */
    @Value("${security.cors.allowed-origins:http://localhost:3000,http://localhost:8080}")
    private String[] allowedOrigins;

    /** Maximum age for CORS preflight requests. */
    @Value("${security.cors.max-age:3600}")
    private long corsMaxAge;

    /** Content Security Policy directive. */
    @Value("${security.csp.policy:default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'}")
    private String contentSecurityPolicy;

    /** Whether to enforce HTTPS in production. */
    @Value("${security.enforce-https:true}")
    private boolean enforceHttps;

    /**
     * Configures the main security filter chain with enhanced protections.
     * 
     * @param http the ServerHttpSecurity configuration
     * @return configured SecurityWebFilterChain
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            // Disable default security for gateway (we handle auth in filters)
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                .pathMatchers("/api/auth/**").permitAll()
                .pathMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyExchange().authenticated()
            )
            
            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Disable CSRF for stateless JWT authentication
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            
            // Configure security headers
            .headers(headers -> headers
                // Content Security Policy  
                .contentSecurityPolicy(csp -> csp.policyDirectives(contentSecurityPolicy))
                
                // Frame options to prevent clickjacking (using deprecated API as alternative not available)
                .frameOptions(Customizer.withDefaults())
                
                // Prevent content type sniffing
                .contentTypeOptions(Customizer.withDefaults())
                
                // Referrer policy
                .referrerPolicy(ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
            )
            
            // Disable form login and HTTP basic (we use JWT)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            
            .build();
    }

    /**
     * Configures CORS settings with strict security policies.
     * 
     * <p>CORS configuration is environment-aware, allowing different
     * origins for development vs production deployments.
     * 
     * @return CorsConfigurationSource with security-focused settings
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Set allowed origins (environment-specific)
        if (allowedOrigins != null && allowedOrigins.length > 0) {
            configuration.setAllowedOriginPatterns(Arrays.asList(allowedOrigins));
        } else {
            // Default to strict policy if not configured
            configuration.setAllowedOriginPatterns(Arrays.asList("http://localhost:3000"));
        }
        
        // Configure allowed methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));
        
        // Configure allowed headers
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Accept",
            "X-Requested-With",
            "Cache-Control",
            "X-Client-Version",
            "X-API-Version"
        ));
        
        // Configure exposed headers (headers that browser can access)
        configuration.setExposedHeaders(Arrays.asList(
            "X-Total-Count",
            "X-Page-Count",
            "X-Per-Page",
            "X-Rate-Limit-Remaining",
            "X-Rate-Limit-Reset",
            "X-Request-ID"
        ));
        
        // Allow credentials for authenticated requests
        configuration.setAllowCredentials(true);
        
        // Set preflight cache duration
        configuration.setMaxAge(corsMaxAge);
        
        // Validate configuration
        validateCorsConfiguration(configuration);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }

    /**
     * Creates a production-ready CORS configuration with enhanced security.
     * 
     * <p>This configuration is more restrictive and suitable for production
     * environments where security is paramount.
     * 
     * @return production CORS configuration
     */
    @Bean
    public CorsConfiguration productionCorsConfiguration() {
        CorsConfiguration config = new CorsConfiguration();
        
        // Strict origin control - only allow specific production domains
        config.setAllowedOriginPatterns(Arrays.asList(
            "https://*.enterprise-rag.com",
            "https://*.your-domain.com"
        ));
        
        // Limited HTTP methods
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        
        // Minimal required headers
        config.setAllowedHeaders(Arrays.asList(
            "Authorization", "Content-Type", "Accept"
        ));
        
        // No wildcard exposure of headers
        config.setExposedHeaders(Arrays.asList("X-Request-ID"));
        
        // Credentials allowed but with strict origin control
        config.setAllowCredentials(true);
        
        // Shorter cache time for production
        config.setMaxAge(1800L); // 30 minutes
        
        return config;
    }

    /**
     * Validates CORS configuration for security compliance.
     * 
     * @param config the CORS configuration to validate
     * @throws IllegalArgumentException if configuration is insecure
     */
    private void validateCorsConfiguration(CorsConfiguration config) {
        // Check for wildcard origins with credentials (security risk)
        if (config.getAllowCredentials() != null && config.getAllowCredentials()) {
            List<String> origins = config.getAllowedOriginPatterns();
            if (origins != null && origins.contains("*")) {
                throw new IllegalArgumentException(
                    "CORS configuration security violation: Cannot use wildcard origin with credentials enabled");
            }
        }
        
        // Validate that HTTPS is enforced in production
        if (enforceHttps && config.getAllowedOriginPatterns() != null) {
            for (String origin : config.getAllowedOriginPatterns()) {
                if (origin.startsWith("http://") && !origin.contains("localhost")) {
                    throw new IllegalArgumentException(
                        "CORS configuration security violation: HTTP origins not allowed in production: " + origin);
                }
            }
        }
        
        // Check for overly permissive headers
        List<String> allowedHeaders = config.getAllowedHeaders();
        if (allowedHeaders != null && allowedHeaders.contains("*")) {
            throw new IllegalArgumentException(
                "CORS configuration security violation: Wildcard headers not recommended for production");
        }
    }

    /**
     * Creates additional security headers for enhanced protection.
     * 
     * @return map of additional security headers
     */
    public static java.util.Map<String, String> getAdditionalSecurityHeaders() {
        return java.util.Map.of(
            // Prevent MIME type sniffing
            "X-Content-Type-Options", "nosniff",
            
            // XSS protection
            "X-XSS-Protection", "1; mode=block",
            
            // Referrer policy
            "Referrer-Policy", "strict-origin-when-cross-origin",
            
            // Feature policy to restrict dangerous features
            "Permissions-Policy", "camera=(), microphone=(), geolocation=(), interest-cohort=()",
            
            // Prevent embedding in frames
            "X-Frame-Options", "DENY",
            
            // Cache control for sensitive endpoints
            "Cache-Control", "no-cache, no-store, must-revalidate",
            "Pragma", "no-cache",
            "Expires", "0"
        );
    }
}
package com.byo.rag.gateway.config;

import com.byo.rag.gateway.security.SecurityHeadersFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Security Headers Configuration for RAG Gateway.
 * 
 * <p>This configuration integrates security headers management into the
 * gateway routing pipeline, ensuring all responses include appropriate
 * security headers for OWASP compliance and enhanced protection.
 * 
 * <p><strong>Configuration Features:</strong>
 * <ul>
 *   <li><strong>Global Headers</strong>: Applied to all routes automatically</li>
 *   <li><strong>Environment-Specific</strong>: Different policies for dev/test/prod</li>
 *   <li><strong>Performance Optimized</strong>: Minimal overhead for header processing</li>
 *   <li><strong>Compliance Ready</strong>: OWASP and security scanner compatible</li>
 * </ul>
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.1
 */
@Configuration
@Profile("!test")
public class SecurityHeadersConfig {

    /** Security headers filter for header management. */
    private final SecurityHeadersFilter securityHeadersFilter;

    /**
     * Constructs security headers configuration.
     * 
     * @param securityHeadersFilter security headers filter
     */
    @Autowired
    public SecurityHeadersConfig(SecurityHeadersFilter securityHeadersFilter) {
        this.securityHeadersFilter = securityHeadersFilter;
    }

    /**
     * Creates security headers route locator.
     * 
     * <p>This route locator applies security headers to all routes by
     * integrating the security headers filter into the global filter chain.
     * 
     * @param builder route locator builder
     * @return route locator with security headers
     */
    @Bean("securityHeadersRouteLocator")
    public RouteLocator securityHeadersRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // Global security headers route that matches all requests
            .route("security-headers-global", r -> r
                .path("/**")
                .filters(f -> f
                    .filter(securityHeadersFilter)
                )
                .uri("no://op") // No-op URI since this is a filter-only route
            )
            .build();
    }
}
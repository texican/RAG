package com.enterprise.rag.auth.config;

import com.enterprise.rag.auth.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security configuration for the Enterprise RAG Authentication Service.
 * 
 * <p>This configuration establishes comprehensive security policies for the authentication
 * service, implementing JWT-based stateless authentication with role-based access control.
 * The configuration supports multi-tenant architecture with fine-grained endpoint security
 * and CORS policies for web client integration.
 * 
 * <p><strong>Security Architecture:</strong>
 * <ul>
 *   <li><strong>Stateless Authentication:</strong> JWT tokens replace traditional sessions</li>
 *   <li><strong>Role-Based Access Control:</strong> Different endpoints require specific user roles</li>
 *   <li><strong>Multi-Tenant Support:</strong> Tenant isolation enforced at the security layer</li>
 *   <li><strong>CORS Configuration:</strong> Cross-origin requests from web clients supported</li>
 * </ul>
 * 
 * <p><strong>Authentication Flow:</strong>
 * <ol>
 *   <li>Client sends JWT token in Authorization header</li>
 *   <li>JWT authentication filter validates token and sets security context</li>
 *   <li>Spring Security evaluates endpoint permissions based on user role</li>
 *   <li>Request proceeds to controller if authorized</li>
 * </ol>
 * 
 * <p><strong>Endpoint Security Matrix:</strong>
 * <ul>
 *   <li><strong>Public:</strong> Authentication endpoints, tenant registration, health checks</li>
 *   <li><strong>User Role:</strong> Profile management, basic user operations</li>
 *   <li><strong>Admin Role:</strong> Tenant management, user administration, system controls</li>
 *   <li><strong>Authenticated:</strong> All other endpoints require valid authentication</li>
 * </ul>
 * 
 * <p><strong>Security Features:</strong>
 * <ul>
 *   <li>BCrypt password encoding with strength factor 12</li>
 *   <li>CSRF protection disabled (stateless JWT architecture)</li>
 *   <li>Session management disabled (stateless design)</li>
 *   <li>Method-level security annotations enabled (@PreAuthorize, @Secured)</li>
 * </ul>
 * 
 * <p><strong>CORS Policy:</strong>
 * Configured to allow cross-origin requests from web clients with:
 * <ul>
 *   <li>All origins permitted (using origin patterns)</li>
 *   <li>Standard HTTP methods (GET, POST, PUT, DELETE, OPTIONS)</li>
 *   <li>Credential support for authenticated requests</li>
 *   <li>All headers allowed for maximum client compatibility</li>
 * </ul>
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.0
 * @see JwtAuthenticationFilter
 * @see SecurityFilterChain
 * @see EnableMethodSecurity
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    /** JWT authentication filter for processing Bearer tokens. */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Constructs security configuration with JWT authentication support.
     * 
     * @param jwtAuthenticationFilter the filter for processing JWT authentication
     */
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * Configures the main security filter chain with JWT authentication and authorization rules.
     * 
     * <p>This method establishes the complete security configuration for the authentication service,
     * defining access rules for all endpoints and integrating JWT-based authentication. The configuration
     * follows a stateless architecture suitable for microservices deployment.
     * 
     * <p><strong>Security Configuration:</strong>
     * <ul>
     *   <li><strong>CSRF:</strong> Disabled for stateless JWT architecture</li>
     *   <li><strong>CORS:</strong> Configured for cross-origin web client support</li>
     *   <li><strong>Sessions:</strong> Disabled in favor of stateless JWT tokens</li>
     *   <li><strong>JWT Filter:</strong> Added before standard username/password filter</li>
     * </ul>
     * 
     * <p><strong>Endpoint Authorization Matrix:</strong>
     * <ul>
     *   <li><strong>Public Access:</strong> Authentication endpoints, tenant registration, health checks, API docs</li>
     *   <li><strong>Admin Only:</strong> Administrative endpoints, tenant lifecycle management, system operations</li>
     *   <li><strong>User Profile:</strong> Self-service profile endpoints accessible by authenticated users</li>
     *   <li><strong>Multi-Role:</strong> User management endpoints for both admin and regular users</li>
     *   <li><strong>Authenticated:</strong> All other endpoints require valid authentication</li>
     * </ul>
     * 
     * <p><strong>Multi-Tenant Security:</strong>
     * While endpoint-level security is configured here, tenant isolation is enforced through:
     * <ul>
     *   <li>JWT claims containing tenant ID</li>
     *   <li>Request attributes set by authentication filter</li>
     *   <li>Service-layer tenant context validation</li>
     * </ul>
     * 
     * @param http the HttpSecurity configuration builder
     * @return configured SecurityFilterChain ready for Spring Security integration
     * @throws Exception if security configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            // Disable CSRF for stateless JWT architecture
            .csrf(AbstractHttpConfigurer::disable)
            
            // Enable CORS with custom configuration for web clients
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Disable session management for stateless JWT design
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Configure endpoint authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public endpoints - no authentication required
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/tenants/register").permitAll()
                
                // Operational endpoints - health checks and documentation
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                
                // Administrative endpoints - admin role required
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/tenants/**").hasRole("ADMIN")
                
                // Tenant lifecycle management - admin-only operations
                .requestMatchers(HttpMethod.POST, "/api/v1/tenants").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/tenants/**").hasRole("ADMIN")
                
                // User self-service - profile management
                .requestMatchers(HttpMethod.GET, "/api/v1/users/me").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/v1/users/me").authenticated()
                
                // User management - accessible to both admins and users (with tenant isolation)
                .requestMatchers("/api/v1/users/**").hasAnyRole("ADMIN", "USER")
                
                // Default policy - all other endpoints require authentication
                .anyRequest().authenticated()
            )
            
            // Add JWT authentication filter before standard username/password authentication
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    /**
     * Configures Cross-Origin Resource Sharing (CORS) policies for web client integration.
     * 
     * <p>This configuration allows web browsers to make cross-origin requests to the
     * authentication service from different domains, enabling integration with web-based
     * frontends and client applications.
     * 
     * <p><strong>CORS Policy:</strong>
     * <ul>
     *   <li><strong>Origins:</strong> All origins allowed using pattern matching</li>
     *   <li><strong>Methods:</strong> Standard HTTP methods (GET, POST, PUT, DELETE, OPTIONS)</li>
     *   <li><strong>Headers:</strong> All headers permitted for maximum client flexibility</li>
     *   <li><strong>Credentials:</strong> Cookie and authorization header support enabled</li>
     * </ul>
     * 
     * <p><strong>Security Considerations:</strong>
     * While this configuration is permissive for development and integration ease,
     * production deployments should consider:
     * <ul>
     *   <li>Restricting allowed origins to known client domains</li>
     *   <li>Limiting allowed headers to required ones only</li>
     *   <li>Monitoring cross-origin request patterns</li>
     * </ul>
     * 
     * <p><strong>Integration Support:</strong>
     * This configuration supports various client types:
     * <ul>
     *   <li>Web browsers with JavaScript applications</li>
     *   <li>Mobile applications using web technologies</li>
     *   <li>Development tools and API testing interfaces</li>
     * </ul>
     * 
     * @return configured CORS configuration source for Spring Security integration
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow all origins using pattern matching for maximum compatibility
        configuration.setAllowedOriginPatterns(List.of("*"));
        
        // Support standard HTTP methods used by REST APIs
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Allow all headers for client flexibility
        configuration.setAllowedHeaders(List.of("*"));
        
        // Enable credentials for authentication header support
        configuration.setAllowCredentials(true);
        
        // Apply configuration to all endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Provides the Spring Security authentication manager for programmatic authentication.
     * 
     * <p>The authentication manager is used by authentication controllers to validate
     * user credentials during login operations. While JWT authentication is stateless,
     * the initial login process still requires credential validation against the database.
     * 
     * @param config Spring Security's authentication configuration
     * @return the configured authentication manager
     * @throws Exception if authentication manager configuration fails
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Provides BCrypt password encoder for secure password hashing and verification.
     * 
     * <p>This encoder uses BCrypt hashing algorithm with a strength factor of 12,
     * providing strong security against brute-force attacks while maintaining
     * reasonable performance for authentication operations.
     * 
     * <p><strong>Security Features:</strong>
     * <ul>
     *   <li><strong>Salt Generation:</strong> Automatic per-password salt generation</li>
     *   <li><strong>Adaptive Hashing:</strong> Strength factor 12 balances security and performance</li>
     *   <li><strong>Time Resistance:</strong> Computational cost increases over time</li>
     *   <li><strong>Rainbow Table Resistance:</strong> Salted hashes prevent precomputed attacks</li>
     * </ul>
     * 
     * <p><strong>Usage:</strong>
     * Used for both password hashing during user registration and password verification
     * during authentication. The encoder automatically handles salt generation and
     * verification without additional configuration.
     * 
     * @return BCrypt password encoder with strength factor 12
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
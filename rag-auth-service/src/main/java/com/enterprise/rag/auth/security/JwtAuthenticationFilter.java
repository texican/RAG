package com.enterprise.rag.auth.security;

import com.enterprise.rag.auth.service.UserService;
import com.enterprise.rag.shared.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * JWT Authentication Filter for processing Bearer tokens in HTTP requests.
 * 
 * <p>This filter intercepts all HTTP requests to extract and validate JWT tokens
 * from the Authorization header, establishing the security context for authenticated users.
 * It integrates with Spring Security's filter chain to provide seamless JWT-based
 * authentication throughout the Enterprise RAG system.
 * 
 * <p><strong>Authentication Flow:</strong>
 * <ol>
 *   <li>Extract Bearer token from Authorization header</li>
 *   <li>Validate token signature, expiration, and format</li>
 *   <li>Load user details from database using token claims</li>
 *   <li>Verify user account status and permissions</li>
 *   <li>Create Spring Security authentication context</li>
 *   <li>Set tenant and user context for request processing</li>
 * </ol>
 * 
 * <p><strong>Multi-Tenant Security:</strong>
 * <ul>
 *   <li>Extracts tenant ID from JWT claims</li>
 *   <li>Sets tenant context as request attribute for downstream services</li>
 *   <li>Ensures tenant isolation throughout the request lifecycle</li>
 *   <li>Validates user access within their tenant boundary</li>
 * </ul>
 * 
 * <p><strong>Security Context Creation:</strong>
 * Creates {@link RagUserPrincipal} containing:
 * <ul>
 *   <li>User ID and email for identification</li>
 *   <li>Tenant ID for multi-tenant isolation</li>
 *   <li>User role for authorization decisions</li>
 *   <li>Spring Security authorities for role-based access control</li>
 * </ul>
 * 
 * <p><strong>Error Handling:</strong>
 * Invalid, expired, or malformed tokens are handled gracefully by:
 * <ul>
 *   <li>Logging authentication failures at WARN level</li>
 *   <li>Continuing request processing without setting authentication</li>
 *   <li>Allowing downstream security configurations to handle unauthenticated requests</li>
 * </ul>
 * 
 * <p><strong>Request Context:</strong>
 * Successfully authenticated requests receive:
 * <ul>
 *   <li><code>tenantId</code> request attribute for tenant isolation</li>
 *   <li><code>userId</code> request attribute for user context</li>
 *   <li>Spring Security context with user principal and authorities</li>
 * </ul>
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.0
 * @see JwtService
 * @see RagUserPrincipal
 * @see OncePerRequestFilter
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Logger for authentication processing and security events. */
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    /** HTTP header name for authorization tokens. */
    private static final String AUTHORIZATION_HEADER = "Authorization";
    
    /** Bearer token prefix in authorization header. */
    private static final String BEARER_PREFIX = "Bearer ";

    /** Service for JWT token validation and claims extraction. */
    private final JwtService jwtService;
    
    /** Service for loading user details from database. */
    private final UserService userService;

    /**
     * Constructs a new JWT authentication filter.
     * 
     * @param jwtService service for JWT token operations
     * @param userService service for user data access
     */
    public JwtAuthenticationFilter(JwtService jwtService, UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
    }

    /**
     * Processes each HTTP request to extract and validate JWT authentication tokens.
     * 
     * <p>This method implements the core JWT authentication logic, running once per request
     * to establish the security context based on the provided Bearer token. The filter
     * follows the standard Spring Security filter pattern with proper error handling.
     * 
     * <p><strong>Authentication Process:</strong>
     * <ol>
     *   <li><strong>Header Extraction:</strong> Looks for Authorization header with Bearer prefix</li>
     *   <li><strong>Token Validation:</strong> Validates JWT signature, expiration, and structure</li>
     *   <li><strong>User Loading:</strong> Retrieves user details from database using token username</li>
     *   <li><strong>Status Check:</strong> Verifies user account is active and accessible</li>
     *   <li><strong>Context Creation:</strong> Sets up Spring Security authentication context</li>
     *   <li><strong>Request Attribution:</strong> Adds tenant/user context to request attributes</li>
     * </ol>
     * 
     * <p><strong>Multi-Tenant Context:</strong>
     * Successfully authenticated requests receive:
     * <ul>
     *   <li><code>tenantId</code> attribute for downstream service isolation</li>
     *   <li><code>userId</code> attribute for user-specific operations</li>
     *   <li>Spring Security principal with full user context</li>
     * </ul>
     * 
     * <p><strong>Security Considerations:</strong>
     * <ul>
     *   <li>Only processes requests without existing authentication context</li>
     *   <li>Validates both token authenticity and user account status</li>
     *   <li>Handles authentication failures gracefully without blocking requests</li>
     *   <li>Logs security events for monitoring and auditing</li>
     * </ul>
     * 
     * <p><strong>Error Handling:</strong>
     * Authentication failures (invalid tokens, inactive users, etc.) are logged
     * but don't block request processing, allowing downstream security rules to
     * handle unauthorized access appropriately.
     * 
     * @param request the HTTP request being processed
     * @param response the HTTP response (not modified by this filter)
     * @param filterChain the Spring Security filter chain to continue processing
     * @throws ServletException if request processing fails
     * @throws IOException if I/O operations fail during request processing
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        
        // Skip processing if no Authorization header or wrong format
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Extract JWT token from Bearer header
            String token = authHeader.substring(BEARER_PREFIX.length());
            String username = jwtService.extractUsername(token);
            
            // Process authentication only if username exists and no current authentication
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                User user = userService.findByEmail(username);
                
                // Validate token and verify user account is active
                if (jwtService.isTokenValid(token, username) && user.isActive()) {
                    // Create Spring Security authorities based on user role
                    List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                    );
                    
                    // Create custom principal with multi-tenant context
                    RagUserPrincipal principal = new RagUserPrincipal(
                        user.getId(),
                        user.getEmail(),
                        user.getTenant().getId(),
                        user.getRole(),
                        authorities
                    );
                    
                    // Create and configure Spring Security authentication token
                    UsernamePasswordAuthenticationToken authToken = 
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // Set authentication context for this request
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    // Add tenant and user context to request attributes for downstream services
                    request.setAttribute("tenantId", user.getTenant().getId());
                    request.setAttribute("userId", user.getId());
                }
            }
        } catch (Exception e) {
            // Log authentication failures but continue request processing
            logger.warn("Cannot set user authentication: {}", e.getMessage());
        }

        // Continue with filter chain regardless of authentication success/failure
        filterChain.doFilter(request, response);
    }

    /**
     * Custom Spring Security principal containing user identity and multi-tenant context.
     * 
     * <p>This record serves as the authenticated user principal in the Spring Security context,
     * providing all necessary information for authorization decisions and tenant isolation
     * throughout the Enterprise RAG system.
     * 
     * <p><strong>Multi-Tenant Security:</strong>
     * The principal includes tenant ID to ensure proper isolation between organizations,
     * allowing downstream services to enforce tenant boundaries without additional database
     * lookups or token parsing.
     * 
     * <p><strong>Authorization Context:</strong>
     * Contains user role and Spring Security authorities for:
     * <ul>
     *   <li>Method-level security annotations (@PreAuthorize, @Secured)</li>
     *   <li>HTTP endpoint security configurations</li>
     *   <li>Custom authorization logic in service layers</li>
     *   <li>Role-based access control throughout the application</li>
     * </ul>
     * 
     * <p><strong>Usage in Controllers:</strong>
     * Controllers can access this principal via Spring Security:
     * <pre>{@code
     * @GetMapping("/profile")
     * public ResponseEntity<?> getProfile(Authentication auth) {
     *     RagUserPrincipal principal = (RagUserPrincipal) auth.getPrincipal();
     *     UUID tenantId = principal.tenantId();
     *     UUID userId = principal.userId();
     *     // ... use tenant and user context
     * }
     * }</pre>
     * 
     * <p><strong>Immutable Design:</strong>
     * As a record, this principal is immutable and thread-safe, suitable for
     * caching in Spring Security context and sharing across request processing.
     * 
     * @param userId the unique identifier for the authenticated user
     * @param email the user's email address (used as username in JWT subject)
     * @param tenantId the unique identifier for the user's organization/tenant
     * @param role the user's role enum for business logic authorization
     * @param authorities Spring Security authorities for framework integration
     * 
     * @author Enterprise RAG Team
     * @version 1.0
     * @since 1.0
     */
    public record RagUserPrincipal(
        UUID userId,
        String email,
        UUID tenantId,
        User.UserRole role,
        List<SimpleGrantedAuthority> authorities
    ) {}
}
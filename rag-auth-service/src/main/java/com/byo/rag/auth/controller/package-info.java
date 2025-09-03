/**
 * REST API controllers for authentication operations.
 * 
 * <p>This package contains REST controllers that provide the public API
 * for authentication operations in the Enterprise RAG System. Controllers
 * handle HTTP requests, validate input, and coordinate with the service
 * layer to execute authentication workflows.</p>
 * 
 * <h2>Controller Architecture</h2>
 * <p>Controllers follow Spring MVC patterns with enterprise extensions:</p>
 * <ul>
 *   <li><strong>Request Validation</strong> - Comprehensive input validation with JSR-303</li>
 *   <li><strong>Exception Handling</strong> - Consistent error responses with proper HTTP status codes</li>
 *   <li><strong>Security Integration</strong> - JWT token handling and security context management</li>
 *   <li><strong>Rate Limiting</strong> - Built-in rate limiting for security-sensitive endpoints</li>
 *   <li><strong>Audit Logging</strong> - Automatic logging of authentication operations</li>
 * </ul>
 * 
 * <h2>API Security</h2>
 * <p>All endpoints implement comprehensive security:</p>
 * <ul>
 *   <li><strong>HTTPS Only</strong> - All authentication endpoints require HTTPS</li>
 *   <li><strong>CSRF Protection</strong> - CSRF tokens for state-changing operations</li>
 *   <li><strong>Rate Limiting</strong> - Aggressive rate limiting on login endpoints</li>
 *   <li><strong>Input Sanitization</strong> - Automatic sanitization of all input parameters</li>
 *   <li><strong>Response Security</strong> - Secure headers and content type enforcement</li>
 * </ul>
 * 
 * <h2>Multi-Tenant API Design</h2>
 * <p>Controllers support multi-tenant authentication:</p>
 * <ul>
 *   <li><strong>Tenant Context</strong> - Automatic tenant context extraction from requests</li>
 *   <li><strong>Tenant Validation</strong> - Validation of tenant existence and status</li>
 *   <li><strong>Tenant Isolation</strong> - Complete isolation of tenant data in responses</li>
 *   <li><strong>Tenant-Aware Routing</strong> - Routing based on tenant configuration</li>
 * </ul>
 * 
 * <h2>Request/Response Patterns</h2>
 * <p>Standardized request and response handling:</p>
 * <ul>
 *   <li><strong>DTO Validation</strong> - Comprehensive validation of request DTOs</li>
 *   <li><strong>Response Formatting</strong> - Consistent response structure across endpoints</li>
 *   <li><strong>Error Handling</strong> - Standardized error responses with correlation IDs</li>
 *   <li><strong>Content Negotiation</strong> - Support for JSON and other content types</li>
 * </ul>
 * 
 * <h2>Authentication Endpoints</h2>
 * <p>Core authentication API endpoints:</p>
 * <ul>
 *   <li><strong>Login Controller</strong> - User authentication and token generation</li>
 *   <li><strong>Token Controller</strong> - JWT token refresh and validation</li>
 *   <li><strong>User Controller</strong> - User management and profile operations</li>
 *   <li><strong>Password Controller</strong> - Password reset and change operations</li>
 * </ul>
 * 
 * <h2>Performance Considerations</h2>
 * <p>Controllers are optimized for high-performance operation:</p>
 * <ul>
 *   <li><strong>Async Processing</strong> - Non-blocking request processing where appropriate</li>
 *   <li><strong>Caching Headers</strong> - Proper cache control headers for performance</li>
 *   <li><strong>Request Compression</strong> - Support for compressed request/response bodies</li>
 *   <li><strong>Connection Keep-Alive</strong> - Efficient HTTP connection management</li>
 * </ul>
 * 
 * <h2>OpenAPI Documentation</h2>
 * <p>All endpoints include comprehensive API documentation:</p>
 * <ul>
 *   <li><strong>Swagger Annotations</strong> - Complete OpenAPI 3.0 annotations</li>
 *   <li><strong>Example Requests</strong> - Sample requests and responses for all endpoints</li>
 *   <li><strong>Error Documentation</strong> - Documentation of all possible error responses</li>
 *   <li><strong>Security Documentation</strong> - Authentication requirements and token formats</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @RestController
 * @RequestMapping("/api/v1/auth")
 * @Validated
 * @Slf4j
 * public class AuthenticationController {
 *     
 *     private final AuthenticationService authService;
 *     private final JwtService jwtService;
 *     
 *     @PostMapping("/login")
 *     @RateLimited(requests = 5, window = "1m")
 *     public ResponseEntity<LoginResponse> login(
 *             @Valid @RequestBody LoginRequest request,
 *             @RequestHeader("X-Tenant-ID") String tenantId,
 *             HttpServletRequest httpRequest) {
 *         
 *         try {
 *             // Authenticate user
 *             User user = authService.authenticate(tenantId, request);
 *             
 *             // Generate tokens
 *             String accessToken = jwtService.generateAccessToken(user);
 *             String refreshToken = jwtService.generateRefreshToken(user);
 *             
 *             // Build response
 *             LoginResponse response = LoginResponse.builder()
 *                 .accessToken(accessToken)
 *                 .refreshToken(refreshToken)
 *                 .tokenType("Bearer")
 *                 .expiresIn(jwtService.getAccessTokenExpiry())
 *                 .user(UserMapper.toUserResponse(user))
 *                 .build();
 *             
 *             // Log successful authentication
 *             auditLogger.logAuthSuccess(user.getId(), tenantId, httpRequest.getRemoteAddr());
 *             
 *             return ResponseEntity.ok(response);
 *             
 *         } catch (AuthenticationException e) {
 *             auditLogger.logAuthFailure(request.getEmail(), tenantId, httpRequest.getRemoteAddr());
 *             throw e;
 *         }
 *     }
 * }
 * }</pre>
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 * @see org.springframework.web.bind.annotation Spring MVC annotations
 * @see org.springframework.validation.annotation Validation annotations
 * @see com.byo.rag.auth.service Authentication service layer
 * @see com.byo.rag.shared.dto Request and response DTOs
 */
package com.byo.rag.auth.controller;
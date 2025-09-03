/**
 * Authentication and authorization service for the Enterprise RAG System.
 * 
 * <p>This package contains the complete authentication and authorization service
 * that handles user authentication, JWT token management, and multi-tenant
 * access control for the Enterprise RAG System. The service provides secure,
 * scalable authentication patterns following OAuth 2.0 and JWT best practices.</p>
 * 
 * <h2>Service Architecture</h2>
 * <p>The auth service follows a layered microservice architecture:</p>
 * <ul>
 *   <li><strong>Controller Layer</strong> - REST API endpoints for authentication operations</li>
 *   <li><strong>Service Layer</strong> - Business logic for user management and token operations</li>
 *   <li><strong>Security Layer</strong> - JWT processing, password management, and access control</li>
 *   <li><strong>Repository Layer</strong> - Data access for users, tenants, and security policies</li>
 *   <li><strong>Configuration Layer</strong> - Security configuration and authentication policies</li>
 * </ul>
 * 
 * <h2>Multi-Tenant Authentication</h2>
 * <p>Complete multi-tenant authentication system:</p>
 * <ul>
 *   <li><strong>Tenant Isolation</strong> - Complete data isolation between tenants</li>
 *   <li><strong>Tenant-Aware Users</strong> - Users scoped to specific tenants with role management</li>
 *   <li><strong>Cross-Tenant Prevention</strong> - Automatic prevention of cross-tenant access</li>
 *   <li><strong>Tenant Metadata</strong> - Rich tenant information in JWT tokens</li>
 *   <li><strong>Tenant-Specific Policies</strong> - Configurable authentication policies per tenant</li>
 * </ul>
 * 
 * <h2>JWT Token Management</h2>
 * <p>Comprehensive JWT token handling:</p>
 * <ul>
 *   <li><strong>Access Tokens</strong> - Short-lived access tokens with rich claims</li>
 *   <li><strong>Refresh Tokens</strong> - Secure refresh token rotation with revocation</li>
 *   <li><strong>Token Validation</strong> - Comprehensive token validation with blacklisting</li>
 *   <li><strong>Claims Management</strong> - Rich token claims with user and tenant context</li>
 *   <li><strong>Token Introspection</strong> - Token validation endpoints for microservices</li>
 * </ul>
 * 
 * <h2>Security Features</h2>
 * <p>Enterprise-grade security implementation:</p>
 * <ul>
 *   <li><strong>Password Security</strong> - BCrypt hashing with configurable strength</li>
 *   <li><strong>Account Lockout</strong> - Automatic account lockout after failed attempts</li>
 *   <li><strong>Rate Limiting</strong> - Login rate limiting with Redis backend</li>
 *   <li><strong>Audit Logging</strong> - Comprehensive security event logging</li>
 *   <li><strong>Session Management</strong> - Secure session handling and invalidation</li>
 * </ul>
 * 
 * <h2>Role-Based Access Control</h2>
 * <p>Flexible RBAC implementation:</p>
 * <ul>
 *   <li><strong>Role Hierarchy</strong> - Hierarchical role structure with inheritance</li>
 *   <li><strong>Permission Management</strong> - Fine-grained permission assignment</li>
 *   <li><strong>Tenant-Scoped Roles</strong> - Roles scoped to specific tenants</li>
 *   <li><strong>Dynamic Authorization</strong> - Runtime authorization decision making</li>
 *   <li><strong>Resource-Based Access</strong> - Object-level access control</li>
 * </ul>
 * 
 * <h2>API Endpoints</h2>
 * <p>Complete authentication API:</p>
 * <ul>
 *   <li><strong>POST /auth/login</strong> - User authentication with tenant context</li>
 *   <li><strong>POST /auth/refresh</strong> - JWT token refresh and rotation</li>
 *   <li><strong>POST /auth/logout</strong> - Secure session termination</li>
 *   <li><strong>POST /auth/register</strong> - User registration (if enabled)</li>
 *   <li><strong>GET /auth/validate</strong> - Token validation for microservices</li>
 *   <li><strong>POST /auth/password-reset</strong> - Secure password reset workflow</li>
 * </ul>
 * 
 * <h2>Integration with Microservices</h2>
 * <p>Seamless integration across the RAG system:</p>
 * <ul>
 *   <li><strong>Gateway Integration</strong> - Authentication filters in API gateway</li>
 *   <li><strong>Service-to-Service</strong> - Internal service authentication</li>
 *   <li><strong>Token Propagation</strong> - Automatic token context propagation</li>
 *   <li><strong>Circuit Breaker</strong> - Resilience patterns for auth service calls</li>
 * </ul>
 * 
 * <h2>Performance and Scalability</h2>
 * <p>Optimized for high-performance authentication:</p>
 * <ul>
 *   <li><strong>Redis Caching</strong> - Token and user session caching</li>
 *   <li><strong>Connection Pooling</strong> - Optimized database connection management</li>
 *   <li><strong>Async Processing</strong> - Non-blocking authentication operations</li>
 *   <li><strong>Load Balancing</strong> - Horizontal scaling with stateless design</li>
 * </ul>
 * 
 * <h2>Monitoring and Observability</h2>
 * <p>Comprehensive security monitoring:</p>
 * <ul>
 *   <li><strong>Authentication Metrics</strong> - Login success/failure rates</li>
 *   <li><strong>Security Events</strong> - Failed login attempts and suspicious activity</li>
 *   <li><strong>Performance Metrics</strong> - Token generation and validation times</li>
 *   <li><strong>Health Checks</strong> - Service health and dependency monitoring</li>
 * </ul>
 * 
 * <h2>Configuration Example</h2>
 * <pre>{@code
 * # application.yml
 * rag:
 *   auth:
 *     jwt:
 *       access-token-expiry: PT15M
 *       refresh-token-expiry: P7D
 *       secret: ${JWT_SECRET:default-secret}
 *     security:
 *       max-login-attempts: 5
 *       lockout-duration: PT30M
 *       password-strength: STRONG
 *     rate-limiting:
 *       login-attempts: 5
 *       time-window: PT1M
 * }</pre>
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 * @see org.springframework.security Spring Security framework
 * @see io.jsonwebtoken JWT library for token management
 * @see com.byo.rag.shared.entity User and tenant entities
 * @see com.byo.rag.shared.security Shared security utilities
 */
package com.byo.rag.auth;
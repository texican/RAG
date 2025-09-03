/**
 * Core shared components for the Enterprise RAG System.
 * 
 * <p><strong>✅ Production Ready & Fully Integrated (2025-09-03):</strong> This package contains 
 * the foundational components shared across all microservices in the Enterprise RAG System, 
 * successfully deployed and operational in Docker. Provides common entities, DTOs, utilities, 
 * and configuration classes that ensure consistency and reduce code duplication across
 * the distributed architecture.</p>
 * 
 * <p><strong>🐳 Docker Integration Status:</strong> All shared components are working perfectly
 * across the microservices ecosystem with complete PostgreSQL and Redis integration.</p>
 * 
 * <h2>Architecture Overview</h2>
 * <p>The shared module follows a layered architecture pattern:</p>
 * <ul>
 *   <li><strong>Entity Layer</strong> - Core JPA entities with multi-tenant support</li>
 *   <li><strong>DTO Layer</strong> - Data transfer objects for inter-service communication</li>
 *   <li><strong>Repository Layer</strong> - Base repository interfaces and common queries</li>
 *   <li><strong>Utility Layer</strong> - Helper classes for validation, formatting, and common operations</li>
 *   <li><strong>Configuration Layer</strong> - Shared Spring configuration and property definitions</li>
 *   <li><strong>Exception Layer</strong> - Standardized exception hierarchy for error handling</li>
 * </ul>
 * 
 * <h2>Multi-Tenant Architecture</h2>
 * <p>All shared components are designed with multi-tenancy in mind:</p>
 * <ul>
 *   <li>Entities include tenant isolation at the database level</li>
 *   <li>DTOs carry tenant context for proper data segregation</li>
 *   <li>Repositories enforce tenant-aware queries automatically</li>
 *   <li>Utilities validate tenant access patterns</li>
 * </ul>
 * 
 * <h2>Integration Patterns</h2>
 * <p>The shared module provides standardized patterns for:</p>
 * <ul>
 *   <li><strong>Database Access</strong> - JPA repositories with automatic auditing</li>
 *   <li><strong>API Communication</strong> - RESTful DTOs with validation</li>
 *   <li><strong>Error Handling</strong> - Consistent exception types and error codes</li>
 *   <li><strong>Security</strong> - JWT token processing and tenant validation</li>
 *   <li><strong>Monitoring</strong> - Common metrics and health check patterns</li>
 * </ul>
 * 
 * <h2>Usage Guidelines</h2>
 * <p>When using shared components:</p>
 * <ul>
 *   <li>Always validate tenant context before data access</li>
 *   <li>Use provided DTOs for all inter-service communication</li>
 *   <li>Leverage common exception types for consistent error handling</li>
 *   <li>Apply shared validation utilities for input sanitization</li>
 *   <li>Follow audit logging patterns for compliance requirements</li>
 * </ul>
 * 
 * <h2>Performance Considerations</h2>
 * <p>The shared module is optimized for:</p>
 * <ul>
 *   <li><strong>Minimal Dependencies</strong> - Only essential Spring Boot and JPA dependencies</li>
 *   <li><strong>Efficient Serialization</strong> - DTOs designed for fast JSON processing</li>
 *   <li><strong>Database Performance</strong> - Entities with proper indexing strategies</li>
 *   <li><strong>Memory Management</strong> - Lightweight utility classes with static methods</li>
 * </ul>
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 * @see com.byo.rag.shared.entity Base entity classes
 * @see com.byo.rag.shared.dto Data transfer objects
 * @see com.byo.rag.shared.repository Repository interfaces
 * @see com.byo.rag.shared.util Utility classes
 * @see com.byo.rag.shared.config Configuration classes
 * @see com.byo.rag.shared.exception Exception hierarchy
 */
package com.byo.rag.shared;
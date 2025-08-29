/**
 * Data Transfer Objects (DTOs) for inter-service communication.
 * 
 * <p>This package contains all Data Transfer Objects used for communication
 * between microservices in the Enterprise RAG System. DTOs provide a clean
 * abstraction layer that decouples internal entity structures from external
 * API contracts, ensuring backward compatibility and API stability.</p>
 * 
 * <h2>DTO Design Principles</h2>
 * <p>All DTOs in this package follow these design principles:</p>
 * <ul>
 *   <li><strong>Immutability</strong> - DTOs are immutable value objects using records where possible</li>
 *   <li><strong>Validation</strong> - Comprehensive JSR-303 validation annotations</li>
 *   <li><strong>Serialization</strong> - Optimized for JSON serialization/deserialization</li>
 *   <li><strong>Documentation</strong> - Complete Javadoc for all fields and validation rules</li>
 *   <li><strong>Versioning</strong> - Designed to support API versioning and evolution</li>
 * </ul>
 * 
 * <h2>DTO Categories</h2>
 * <p>DTOs are organized into logical categories:</p>
 * <ul>
 *   <li><strong>Request DTOs</strong> - Input data for API endpoints (CreateRequest, UpdateRequest)</li>
 *   <li><strong>Response DTOs</strong> - Output data from API endpoints (Response, ListResponse)</li>
 *   <li><strong>Event DTOs</strong> - Kafka event payloads for inter-service messaging</li>
 *   <li><strong>Search DTOs</strong> - Specialized DTOs for search and filtering operations</li>
 *   <li><strong>Mapping DTOs</strong> - Internal DTOs for data transformation between layers</li>
 * </ul>
 * 
 * <h2>Validation Strategy</h2>
 * <p>All DTOs implement comprehensive validation:</p>
 * <ul>
 *   <li><strong>Field Validation</strong> - @NotNull, @NotBlank, @Size, @Pattern annotations</li>
 *   <li><strong>Custom Validation</strong> - Business-specific validators for complex rules</li>
 *   <li><strong>Cross-Field Validation</strong> - @Valid annotations for nested object validation</li>
 *   <li><strong>Conditional Validation</strong> - Context-aware validation based on operation type</li>
 * </ul>
 * 
 * <h2>Multi-Tenant Context</h2>
 * <p>DTOs support multi-tenant architecture through:</p>
 * <ul>
 *   <li><strong>Tenant Context</strong> - DTOs carry implicit tenant context from security layer</li>
 *   <li><strong>Data Isolation</strong> - No cross-tenant data exposure in response DTOs</li>
 *   <li><strong>Tenant Validation</strong> - Automatic tenant access validation during processing</li>
 *   <li><strong>Audit Information</strong> - Tenant-aware audit fields in response DTOs</li>
 * </ul>
 * 
 * <h2>Serialization Optimization</h2>
 * <p>DTOs are optimized for efficient serialization:</p>
 * <ul>
 *   <li><strong>JSON Annotations</strong> - @JsonProperty, @JsonIgnore for field control</li>
 *   <li><strong>Date Formatting</strong> - Consistent ISO-8601 date/time formatting</li>
 *   <li><strong>Null Handling</strong> - Strategic use of @JsonInclude for response size optimization</li>
 *   <li><strong>Type Safety</strong> - Strong typing for all fields with appropriate Java types</li>
 * </ul>
 * 
 * <h2>API Evolution Support</h2>
 * <p>DTOs are designed to support API evolution:</p>
 * <ul>
 *   <li><strong>Optional Fields</strong> - New fields are optional to maintain backward compatibility</li>
 *   <li><strong>Deprecation Strategy</strong> - @Deprecated annotation with migration guidance</li>
 *   <li><strong>Version Headers</strong> - DTOs support API versioning through HTTP headers</li>
 *   <li><strong>Schema Documentation</strong> - OpenAPI schema generation from annotations</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @RestController
 * public class DocumentController {
 *     
 *     @PostMapping("/documents")
 *     public ResponseEntity<DocumentResponse> createDocument(
 *             @Valid @RequestBody DocumentCreateRequest request,
 *             @RequestHeader("X-Tenant-ID") String tenantId) {
 *         
 *         Document document = documentService.createDocument(tenantId, request);
 *         DocumentResponse response = DocumentMapper.toResponse(document);
 *         
 *         return ResponseEntity.status(HttpStatus.CREATED).body(response);
 *     }
 * }
 * }</pre>
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 * @see javax.validation.constraints Validation annotations
 * @see com.fasterxml.jackson.annotation Jackson JSON annotations
 * @see com.enterprise.rag.shared.entity Entity classes that DTOs represent
 */
package com.enterprise.rag.shared.dto;
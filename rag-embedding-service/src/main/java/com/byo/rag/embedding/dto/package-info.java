/**
 * Data Transfer Objects (DTOs) for the Enterprise RAG Embedding Service.
 * <p>
 * This package contains all request and response objects used by the embedding service
 * for vector generation, similarity search, and embedding operations. These DTOs provide
 * type-safe data transfer between the embedding service and its clients.
 * 
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link com.byo.rag.embedding.dto.EmbeddingRequest} - Request for generating embeddings from text content</li>
 *   <li>{@link com.byo.rag.embedding.dto.EmbeddingResponse} - Response containing generated vector embeddings</li>
 *   <li>{@link com.byo.rag.embedding.dto.SearchRequest} - Request for vector similarity search operations</li>
 *   <li>{@link com.byo.rag.embedding.dto.SearchResponse} - Response containing search results and metadata</li>
 * </ul>
 * 
 * <h2>Design Patterns</h2>
 * <ul>
 *   <li><strong>Record-Based DTOs</strong> - Leverages Java records for immutable data structures</li>
 *   <li><strong>Bean Validation</strong> - Comprehensive input validation using Jakarta validation annotations</li>
 *   <li><strong>Multi-Tenant Support</strong> - All requests include tenant isolation identifiers</li>
 *   <li><strong>Batch Processing</strong> - Support for both single and batch embedding operations</li>
 * </ul>
 * 
 * <h2>Validation Standards</h2>
 * All DTOs implement comprehensive validation including:
 * <ul>
 *   <li>Tenant ID validation for multi-tenant isolation</li>
 *   <li>Text content size limits (8000 characters per text, 100 texts per batch)</li>
 *   <li>Model name validation and length constraints</li>
 *   <li>Document and chunk ID validation for traceability</li>
 * </ul>
 * 
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Single text embedding
 * var request = EmbeddingRequest.singleText(
 *     tenantId, 
 *     "Sample text content", 
 *     "openai-text-embedding-3-small",
 *     documentId,
 *     chunkId
 * );
 * 
 * // Batch text embedding
 * var batchRequest = EmbeddingRequest.batchTexts(
 *     tenantId,
 *     List.of("Text 1", "Text 2", "Text 3"),
 *     "sentence-transformers-all-minilm-l6-v2",
 *     documentId,
 *     List.of(chunkId1, chunkId2, chunkId3)
 * );
 * 
 * // Vector similarity search
 * var searchRequest = new SearchRequest(
 *     tenantId,
 *     queryEmbedding,
 *     10, // topK results
 *     0.7f, // similarity threshold
 *     null // no metadata filter
 * );
 * }</pre>
 * 
 * <h2>Integration Points</h2>
 * These DTOs are used by:
 * <ul>
 *   <li>REST API controllers for HTTP request/response handling</li>
 *   <li>Service layer for business logic processing</li>
 *   <li>Kafka message consumers for async processing</li>
 *   <li>Client libraries for inter-service communication</li>
 * </ul>
 * 
 * @author Enterprise RAG Development Team
 * @version 0.8.0
 * @since 0.1.0
 */
package com.byo.rag.embedding.dto;
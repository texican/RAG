package com.enterprise.rag.shared.dto;

import java.util.Map;
import java.util.UUID;

/**
 * Data Transfer Object representing a text chunk extracted from a document in the Enterprise RAG System.
 * <p>
 * Document chunks are the fundamental units of text processing in the RAG pipeline. Each chunk represents
 * a contiguous segment of text from a source document, optimized for embedding generation and semantic search.
 * Chunks maintain referential integrity back to their source document and preserve positional information.
 * </p>
 * 
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Text Segmentation</strong> - Represents a coherent segment of document text</li>
 *   <li><strong>Positional Tracking</strong> - Maintains start/end indices within source document</li>
 *   <li><strong>Sequence Ordering</strong> - Preserves natural order of chunks within document</li>
 *   <li><strong>Token Counting</strong> - Tracks token count for LLM context management</li>
 *   <li><strong>Metadata Support</strong> - Extensible metadata for enhanced search and retrieval</li>
 * </ul>
 * 
 * <h3>Usage in RAG Pipeline:</h3>
 * <ol>
 *   <li><strong>Text Extraction</strong> - Document content is split into manageable chunks</li>
 *   <li><strong>Embedding Generation</strong> - Each chunk is converted to vector embeddings</li>
 *   <li><strong>Vector Storage</strong> - Chunks stored in vector database with embeddings</li>
 *   <li><strong>Semantic Search</strong> - Query embeddings matched against chunk embeddings</li>
 *   <li><strong>Context Assembly</strong> - Relevant chunks assembled for LLM context</li>
 * </ol>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * var chunk = new DocumentChunkDto(
 *     chunkId,
 *     "This is the extracted text content from the document...",
 *     1,                    // First chunk in sequence
 *     0,                    // Starts at beginning of document
 *     150,                  // Ends at character 150
 *     45,                   // Contains 45 tokens
 *     "technical-report.pdf",
 *     Map.of("section", "introduction", "page", 1)
 * );
 * }</pre>
 * 
 * @param id unique identifier for this text chunk
 * @param content the actual text content of this chunk
 * @param sequenceNumber ordinal position of this chunk within the source document (0-based)
 * @param startIndex starting character position within the source document
 * @param endIndex ending character position within the source document
 * @param tokenCount number of tokens in this chunk (for LLM context management)
 * @param documentFilename filename of the source document this chunk was extracted from
 * @param metadata additional key-value metadata for enhanced search and categorization
 * 
 * @author Enterprise RAG Development Team
 * @since 1.0.0
 * @see com.enterprise.rag.shared.entity.DocumentChunk
 */
public record DocumentChunkDto(
    UUID id,
    String content,
    Integer sequenceNumber,
    Integer startIndex,
    Integer endIndex,
    Integer tokenCount,
    String documentFilename,
    Map<String, Object> metadata
) {}
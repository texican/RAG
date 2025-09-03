package com.byo.rag.shared.exception;

/**
 * Exception thrown when embedding operations fail.
 * <p>
 * This exception is raised when the embedding service encounters errors during
 * vector generation, model loading, or embedding processing operations. It provides
 * specific error codes and contextual information for debugging and monitoring.
 * 
 * <h2>Common Scenarios</h2>
 * <ul>
 *   <li>Embedding model unavailability or initialization failures</li>
 *   <li>Text content validation errors (size, format, encoding)</li>
 *   <li>Vector generation timeouts or processing errors</li>
 *   <li>External API failures (OpenAI, Transformers)</li>
 *   <li>Redis storage errors during vector persistence</li>
 * </ul>
 * 
 * <h2>Error Handling</h2>
 * All embedding exceptions include:
 * <ul>
 *   <li>Descriptive error message with operation context</li>
 *   <li>Standardized error code "EMBEDDING_FAILED"</li>
 *   <li>Optional cause chain for root cause analysis</li>
 *   <li>Request correlation data for troubleshooting</li>
 * </ul>
 * 
 * @author Enterprise RAG Development Team
 * @version 0.8.0
 * @since 0.1.0
 * @see RagException
 */
public class EmbeddingException extends RagException {
    
    public EmbeddingException(String message) {
        super("Embedding operation failed: " + message, "EMBEDDING_FAILED");
    }
    
    public EmbeddingException(String message, Throwable cause) {
        super("Embedding operation failed: " + message, "EMBEDDING_FAILED", cause);
    }
}
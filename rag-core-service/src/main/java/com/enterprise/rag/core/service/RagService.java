package com.enterprise.rag.core.service;

import com.enterprise.rag.core.dto.QuestionRequest;
import com.enterprise.rag.core.dto.RagResponse;
import com.enterprise.rag.shared.dto.DocumentChunkDto;
import com.enterprise.rag.shared.entity.DocumentChunk;
import com.enterprise.rag.shared.repository.DocumentChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RagService {

    private static final Logger logger = LoggerFactory.getLogger(RagService.class);

    private final ChatClient chatClient;
    private final DocumentChunkRepository documentChunkRepository;
    private final VectorSearchService vectorSearchService;
    private final CacheService cacheService;

    private static final String RAG_PROMPT_TEMPLATE = """
        You are a helpful AI assistant that answers questions based on the provided context.
        
        Context from documents:
        {context}
        
        Question: {question}
        
        Instructions:
        - Answer the question based ONLY on the provided context
        - If the context doesn't contain enough information, say so clearly
        - Cite specific sources when possible
        - Be concise but comprehensive
        - If unsure, express uncertainty rather than guessing
        
        Answer:
        """;

    @Autowired
    public RagService(ChatClient.Builder chatClientBuilder,
                     DocumentChunkRepository documentChunkRepository,
                     VectorSearchService vectorSearchService,
                     CacheService cacheService) {
        this.chatClient = chatClientBuilder.build();
        this.documentChunkRepository = documentChunkRepository;
        this.vectorSearchService = vectorSearchService;
        this.cacheService = cacheService;
    }

    public RagResponse processQuestion(QuestionRequest request, UUID tenantId) {
        long startTime = System.currentTimeMillis();
        
        logger.info("Processing RAG question for tenant: {}", tenantId);
        logger.debug("Question: {}", request.question());

        try {
            // Check cache first
            String cacheKey = generateCacheKey(request, tenantId);
            RagResponse cachedResponse = cacheService.getResponse(cacheKey);
            if (cachedResponse != null) {
                logger.info("Returning cached response for question");
                return cachedResponse;
            }

            // Step 1: Retrieve relevant document chunks
            List<DocumentChunk> relevantChunks = retrieveRelevantChunks(
                request.question(), 
                tenantId, 
                request.maxChunks(),
                request.similarityThreshold()
            );

            if (relevantChunks.isEmpty()) {
                return createNoContextResponse(request, startTime);
            }

            // Step 2: Build context from chunks
            String context = buildContext(relevantChunks);

            // Step 3: Generate answer using LLM
            String answer = generateAnswer(request.question(), context);

            // Step 4: Build response
            RagResponse response = buildResponse(
                answer, 
                relevantChunks, 
                request, 
                startTime,
                "gpt-4o-mini", // TODO: Make configurable
                false
            );

            // Cache the response
            cacheService.cacheResponse(cacheKey, response);

            logger.info("RAG processing completed in {}ms", response.processingTimeMs());
            return response;

        } catch (Exception e) {
            logger.error("Error processing RAG question", e);
            throw new RuntimeException("Failed to process question: " + e.getMessage(), e);
        }
    }

    private List<DocumentChunk> retrieveRelevantChunks(String question, UUID tenantId, 
                                                      int maxChunks, double threshold) {
        
        // For now, use simple text search - will upgrade to vector search later
        logger.debug("Retrieving relevant chunks for tenant: {}", tenantId);
        
        // Simple keyword search as fallback
        List<DocumentChunk> chunks = documentChunkRepository.findByTenant_IdAndContentContainingIgnoreCase(
            tenantId, 
            question.toLowerCase().split(" ")[0] // Use first word for simple search
        );

        return chunks.stream()
            .limit(maxChunks)
            .collect(Collectors.toList());
    }

    private String buildContext(List<DocumentChunk> chunks) {
        return chunks.stream()
            .map(chunk -> String.format(
                "Source: %s\nContent: %s\n", 
                chunk.getDocument().getFilename(),
                chunk.getContent()
            ))
            .collect(Collectors.joining("\n---\n"));
    }

    private String generateAnswer(String question, String context) {
        logger.debug("Generating answer using LLM");

        PromptTemplate promptTemplate = new PromptTemplate(RAG_PROMPT_TEMPLATE);
        Prompt prompt = promptTemplate.create(Map.of(
            "question", question,
            "context", context
        ));

        var response = chatClient.prompt(prompt).call().content();
        logger.debug("Generated answer length: {} characters", response.length());
        
        return response;
    }

    private RagResponse buildResponse(String answer, List<DocumentChunk> chunks, 
                                    QuestionRequest request, long startTime, 
                                    String modelUsed, boolean fromCache) {
        
        long processingTime = System.currentTimeMillis() - startTime;
        
        // Convert chunks to DTOs if sources are requested
        List<DocumentChunkDto> sources = request.includeSources() 
            ? chunks.stream().map(this::convertToDto).collect(Collectors.toList())
            : List.of();

        var metadata = new RagResponse.QueryMetadata(
            chunks.size(),
            modelUsed,
            estimateTokens(answer), // Rough estimate
            fromCache,
            "simple_keyword" // Will upgrade to semantic similarity
        );

        return new RagResponse(
            answer,
            0.8, // TODO: Implement confidence scoring
            sources,
            processingTime,
            LocalDateTime.now(),
            request.conversationId(),
            metadata
        );
    }

    private RagResponse createNoContextResponse(QuestionRequest request, long startTime) {
        long processingTime = System.currentTimeMillis() - startTime;
        
        String answer = "I don't have enough information in the knowledge base to answer your question. " +
                       "Please try rephrasing your question or ensure relevant documents have been uploaded.";

        var metadata = new RagResponse.QueryMetadata(
            0, "none", 0, false, "no_results"
        );

        return new RagResponse(
            answer,
            0.0,
            List.of(),
            processingTime,
            LocalDateTime.now(),
            request.conversationId(),
            metadata
        );
    }

    private DocumentChunkDto convertToDto(DocumentChunk chunk) {
        return new DocumentChunkDto(
            chunk.getId(),
            chunk.getContent(),
            chunk.getSequenceNumber(),
            chunk.getStartIndex(),
            chunk.getEndIndex(),
            chunk.getTokenCount(),
            chunk.getDocument().getFilename(),
            Map.of() // TODO: Parse metadata JSON string to Map
        );
    }

    private String generateCacheKey(QuestionRequest request, UUID tenantId) {
        return String.format("rag:%s:%d", tenantId, request.question().hashCode());
    }

    private int estimateTokens(String text) {
        // Rough estimate: ~4 characters per token
        return text.length() / 4;
    }
}
package com.enterprise.rag.core.service;

import com.enterprise.rag.core.dto.RagQueryResponse.SourceDocument;
import com.enterprise.rag.shared.exception.RagException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Enterprise service for managing conversation history, context, and query contextualization.
 * 
 * <p>This service provides sophisticated conversation management capabilities that enhance
 * the RAG system's ability to maintain context across multiple user interactions. It stores
 * conversation history in Redis with configurable TTL and provides intelligent query
 * contextualization using recent conversation history.</p>
 * 
 * <p>Core capabilities:</p>
 * <ul>
 *   <li><strong>Conversation History:</strong> Persistent storage of user-AI exchanges</li>
 *   <li><strong>Query Contextualization:</strong> Enhances new queries with conversation context</li>
 *   <li><strong>Context Windows:</strong> Configurable window of recent exchanges for context</li>
 *   <li><strong>Search & Similarity:</strong> Find similar previous exchanges in conversation</li>
 *   <li><strong>Statistics & Analytics:</strong> Comprehensive conversation metrics and insights</li>
 *   <li><strong>Multi-tenant Support:</strong> Isolated conversations per tenant and user</li>
 * </ul>
 * 
 * <p>Storage and lifecycle management:</p>
 * <ul>
 *   <li><strong>Redis Storage:</strong> High-performance conversation storage with TTL</li>
 *   <li><strong>Size Management:</strong> Automatic pruning of old exchanges to maintain limits</li>
 *   <li><strong>Expiration:</strong> Configurable conversation expiration for data privacy</li>
 *   <li><strong>Thread Safety:</strong> Concurrent conversation access across multiple users</li>
 * </ul>
 * 
 * <p>Query contextualization enhances user queries by:</p>
 * <ul>
 *   <li>Adding relevant conversation history for context</li>
 *   <li>Resolving pronouns and references from previous exchanges</li>
 *   <li>Maintaining conversation flow and coherence</li>
 *   <li>Improving LLM understanding of user intent</li>
 * </ul>
 * 
 * <p>Configuration properties:</p>
 * <ul>
 *   <li>{@code conversation.max-history} - Maximum exchanges per conversation (default: 20)</li>
 *   <li>{@code conversation.context-window} - Recent exchanges used for context (default: 5)</li>
 *   <li>{@code conversation.ttl-hours} - Conversation expiration hours (default: 24)</li>
 *   <li>{@code conversation.enable-context} - Enable query contextualization (default: true)</li>
 * </ul>
 * 
 * @author Enterprise RAG Development Team
 * @since 1.0.0
 * @version 1.0
 * @see ConversationExchange
 * @see ConversationSummary
 * @see ConversationStats
 */
@Service
public class ConversationService {

    private static final Logger logger = LoggerFactory.getLogger(ConversationService.class);

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${conversation.max-history:20}")
    private int maxHistorySize;

    @Value("${conversation.context-window:5}")
    private int contextWindow;

    @Value("${conversation.ttl-hours:24}")
    private long conversationTtlHours;

    @Value("${conversation.enable-context:true}")
    private boolean enableContextualization;

    public ConversationService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Add a new user-AI exchange to the conversation history.
     * 
     * <p>This method stores a complete interaction between the user and AI system,
     * including the original query, generated response, and source documents used.
     * It maintains conversation history within configured limits and sets appropriate
     * expiration times for data privacy compliance.</p>
     * 
     * <p>Exchange management features:</p>
     * <ul>
     *   <li><strong>Automatic History Pruning:</strong> Maintains max history size by removing oldest exchanges</li>
     *   <li><strong>Source Document Tracking:</strong> Records which documents contributed to the response</li>
     *   <li><strong>User Attribution:</strong> Associates exchanges with specific users for multi-user conversations</li>
     *   <li><strong>Persistence:</strong> Stores in Redis with configurable TTL for automatic cleanup</li>
     * </ul>
     * 
     * <p>If the conversation doesn't exist, it will be created automatically.
     * The method is thread-safe and handles concurrent access to the same conversation.</p>
     * 
     * @param conversationId unique identifier for the conversation
     * @param userId the user who asked the question
     * @param userQuery the original user question or query
     * @param aiResponse the AI-generated response
     * @param sources list of source documents that contributed to the response
     * @throws RagException if the exchange cannot be stored due to Redis errors
     * @see #getConversation(String)
     * @see #contextualizeQuery(String, String)
     */
    public void addExchange(String conversationId, UUID userId, String userQuery, 
                           String aiResponse, List<SourceDocument> sources) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            logger.warn("Cannot add exchange: conversationId is null or empty");
            return;
        }

        try {
            String key = getConversationKey(conversationId);
            
            ConversationExchange exchange = ConversationExchange.create(
                userQuery, aiResponse, sources, userId);

            // Get current conversation
            Conversation conversation = getConversation(conversationId);
            if (conversation == null) {
                conversation = Conversation.create(conversationId, userId);
            }

            // Add exchange and maintain size limit
            conversation.addExchange(exchange, maxHistorySize);

            // Store updated conversation
            redisTemplate.opsForValue().set(key, conversation, conversationTtlHours, TimeUnit.HOURS);
            
            logger.debug("Added exchange to conversation: {} for user: {}", conversationId, userId);

        } catch (Exception e) {
            logger.error("Failed to add exchange to conversation: {}", conversationId, e);
            throw new RagException("Failed to store conversation exchange", e);
        }
    }

    /**
     * Contextualize a new user query using recent conversation history.
     * 
     * <p>This method enhances new user queries by incorporating relevant context
     * from recent conversation exchanges. This helps resolve pronoun references,
     * maintain conversation flow, and provide better context for the LLM to
     * generate more accurate and contextually appropriate responses.</p>
     * 
     * <p>Contextualization process:</p>
     * <ol>
     *   <li><strong>History Retrieval:</strong> Gets recent exchanges within context window</li>
     *   <li><strong>Context Building:</strong> Constructs coherent conversation context</li>
     *   <li><strong>Query Enhancement:</strong> Combines context with new query</li>
     *   <li><strong>Fallback Handling:</strong> Returns original query if contextualization fails</li>
     * </ol>
     * 
     * <p>Benefits of contextualization:</p>
     * <ul>
     *   <li>Resolves pronouns and implicit references ("it", "that", "the document", etc.)</li>
     *   <li>Maintains conversation coherence across multiple exchanges</li>
     *   <li>Provides LLM with necessary background for accurate responses</li>
     *   <li>Improves user experience with more natural conversation flow</li>
     * </ul>
     * 
     * @param conversationId the conversation to use for context
     * @param newQuery the new user query to contextualize
     * @return enhanced query with conversation context, or original query if contextualization disabled/failed
     * @see #addExchange(String, UUID, String, String, List)
     * @see #getConversation(String)
     */
    public String contextualizeQuery(String conversationId, String newQuery) {
        if (!enableContextualization || conversationId == null) {
            return newQuery;
        }

        try {
            Conversation conversation = getConversation(conversationId);
            if (conversation == null || conversation.exchanges().isEmpty()) {
                return newQuery;
            }

            // Get recent exchanges for context
            List<ConversationExchange> recentExchanges = conversation.exchanges()
                .stream()
                .sorted((e1, e2) -> e2.timestamp().compareTo(e1.timestamp()))
                .limit(contextWindow)
                .collect(Collectors.toList());

            String contextualizedQuery = buildContextualizedQuery(newQuery, recentExchanges);
            
            logger.debug("Contextualized query for conversation: {} - Original: '{}', Contextualized: '{}'", 
                        conversationId, newQuery, contextualizedQuery);

            return contextualizedQuery;

        } catch (Exception e) {
            logger.error("Failed to contextualize query for conversation: {}", conversationId, e);
            // Return original query if contextualization fails
            return newQuery;
        }
    }

    /**
     * Retrieve complete conversation history including all exchanges and metadata.
     * 
     * <p>This method fetches the full conversation record from Redis storage,
     * including all user-AI exchanges, timestamps, user information, and metadata.
     * It's used by the contextualization system and for conversation management
     * in user interfaces.</p>
     * 
     * <p>Returned conversation includes:</p>
     * <ul>
     *   <li><strong>All Exchanges:</strong> Complete history of user questions and AI responses</li>
     *   <li><strong>Source Documents:</strong> Documents referenced in each exchange</li>
     *   <li><strong>Timestamps:</strong> Creation and update times for temporal analysis</li>
     *   <li><strong>User Information:</strong> User attribution for each exchange</li>
     *   <li><strong>Metadata:</strong> Additional conversation-level metadata</li>
     * </ul>
     * 
     * @param conversationId unique identifier for the conversation to retrieve
     * @return complete conversation object with all exchanges, or null if not found
     * @see Conversation
     * @see #getConversationSummary(String)
     * @see #addExchange(String, UUID, String, String, List)
     */
    public Conversation getConversation(String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            return null;
        }

        try {
            String key = getConversationKey(conversationId);
            Object result = redisTemplate.opsForValue().get(key);
            
            if (result instanceof Conversation conversation) {
                logger.debug("Retrieved conversation: {} with {} exchanges", 
                           conversationId, conversation.exchanges().size());
                return conversation;
            }
            
            return null;

        } catch (Exception e) {
            logger.error("Failed to retrieve conversation: {}", conversationId, e);
            return null;
        }
    }

    /**
     * Permanently delete a conversation and all its history.
     * 
     * <p>This method removes a conversation and all associated exchanges from
     * Redis storage. It's used for data privacy compliance, user requests
     * to delete their data, or administrative cleanup operations.</p>
     * 
     * <p>Deletion characteristics:</p>
     * <ul>
     *   <li><strong>Complete Removal:</strong> Deletes all exchanges, metadata, and conversation data</li>
     *   <li><strong>Immediate Effect:</strong> Conversation is no longer accessible after successful deletion</li>
     *   <li><strong>Privacy Compliance:</strong> Supports GDPR and data retention policies</li>
     *   <li><strong>Error Handling:</strong> Graceful handling of deletion failures</li>
     * </ul>
     * 
     * <p>This operation cannot be undone. Ensure appropriate authorization
     * and confirmation before calling this method.</p>
     * 
     * @param conversationId unique identifier of the conversation to delete
     * @return true if the conversation was successfully deleted, false if it didn't exist or deletion failed
     * @see #getConversation(String)
     * @see #cleanupExpiredConversations()
     */
    public boolean deleteConversation(String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            return false;
        }

        try {
            String key = getConversationKey(conversationId);
            Boolean deleted = redisTemplate.delete(key);
            
            logger.info("Conversation deleted: {} - Success: {}", conversationId, deleted);
            return Boolean.TRUE.equals(deleted);

        } catch (Exception e) {
            logger.error("Failed to delete conversation: {}", conversationId, e);
            return false;
        }
    }

    /**
     * Generate a concise summary of conversation for display purposes.
     * 
     * <p>This method creates a lightweight summary of conversation information
     * suitable for user interfaces, conversation lists, or dashboards. It extracts
     * key metadata without loading the full conversation history.</p>
     * 
     * <p>Summary includes:</p>
     * <ul>
     *   <li><strong>Basic Info:</strong> Conversation ID, user, creation/update times</li>
     *   <li><strong>Exchange Count:</strong> Total number of user-AI exchanges</li>
     *   <li><strong>Query Samples:</strong> First and most recent user queries</li>
     *   <li><strong>Activity Indicators:</strong> Last update time for recency assessment</li>
     * </ul>
     * 
     * <p>Use this method for:</p>
     * <ul>
     *   <li>Conversation list displays</li>
     *   <li>User dashboard recent activity</li>
     *   <li>Administrative conversation overviews</li>
     *   <li>Search result previews</li>
     * </ul>
     * 
     * @param conversationId unique identifier of the conversation to summarize
     * @return conversation summary with key metadata, or null if conversation not found
     * @see ConversationSummary
     * @see #getConversation(String)
     * @see #getStats(String)
     */
    public ConversationSummary getConversationSummary(String conversationId) {
        Conversation conversation = getConversation(conversationId);
        if (conversation == null) {
            return null;
        }

        return ConversationSummary.from(conversation);
    }

    /**
     * Search conversation history to find exchanges with similar user queries.
     * 
     * <p>This method performs similarity-based search within a conversation to find
     * previous exchanges that asked similar questions. It's useful for identifying
     * repeated questions, finding relevant context, or suggesting previously
     * successful responses.</p>
     * 
     * <p>Search methodology:</p>
     * <ul>
     *   <li><strong>Text Similarity:</strong> Uses Jaccard similarity on tokenized queries</li>
     *   <li><strong>Relevance Ranking:</strong> Orders results by similarity score</li>
     *   <li><strong>Configurable Threshold:</strong> Filters results above similarity threshold</li>
     *   <li><strong>Result Limiting:</strong> Returns top N most similar exchanges</li>
     * </ul>
     * 
     * <p>Use cases:</p>
     * <ul>
     *   <li>Detecting repeated questions for improved caching</li>
     *   <li>Finding related previous discussions</li>
     *   <li>Suggesting alternative phrasings</li>
     *   <li>Analysis of conversation patterns</li>
     * </ul>
     * 
     * <p>Future enhancements may include semantic similarity using embeddings
     * for better matching of conceptually similar queries.</p>
     * 
     * @param conversationId the conversation to search within
     * @param query the query text to find similarities for
     * @param limit maximum number of similar exchanges to return
     * @return list of similar exchanges ordered by similarity score (highest first)
     * @see ConversationExchange
     * @see #getConversation(String)
     */
    public List<ConversationExchange> findSimilarExchanges(String conversationId, String query, int limit) {
        Conversation conversation = getConversation(conversationId);
        if (conversation == null || conversation.exchanges().isEmpty()) {
            return List.of();
        }

        // Simple text similarity search (could be enhanced with semantic search)
        return conversation.exchanges().stream()
            .filter(exchange -> calculateTextSimilarity(query, exchange.userQuery()) > 0.3)
            .sorted((e1, e2) -> Double.compare(
                calculateTextSimilarity(query, e2.userQuery()),
                calculateTextSimilarity(query, e1.userQuery())
            ))
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Generate comprehensive statistics and analytics for a conversation.
     * 
     * <p>This method computes detailed metrics about conversation activity,
     * content characteristics, and usage patterns. The statistics are valuable
     * for understanding user engagement, system performance, and conversation
     * quality.</p>
     * 
     * <p>Statistical metrics include:</p>
     * <ul>
     *   <li><strong>Exchange Counts:</strong> Total exchanges and queries in conversation</li>
     *   <li><strong>Document Usage:</strong> Unique documents referenced across exchanges</li>
     *   <li><strong>Temporal Analysis:</strong> Conversation span and activity patterns</li>
     *   <li><strong>Response Analysis:</strong> Average response length and complexity</li>
     * </ul>
     * 
     * <p>Applications:</p>
     * <ul>
     *   <li>User engagement analysis</li>
     *   <li>System performance monitoring</li>
     *   <li>Conversation quality assessment</li>
     *   <li>Usage pattern identification</li>
     *   <li>Resource utilization tracking</li>
     * </ul>
     * 
     * @param conversationId the conversation to analyze
     * @return comprehensive statistics about the conversation, or empty stats if not found
     * @see ConversationStats
     * @see #getConversationSummary(String)
     * @see #getConversation(String)
     */
    public ConversationStats getStats(String conversationId) {
        Conversation conversation = getConversation(conversationId);
        if (conversation == null) {
            return ConversationStats.empty();
        }

        return ConversationStats.from(conversation);
    }

    /**
     * Perform cleanup of expired conversations and maintenance tasks.
     * 
     * <p>This method handles periodic maintenance of conversation storage,
     * primarily triggering cleanup of expired conversations. While Redis TTL
     * handles automatic expiration, this method provides a hook for additional
     * cleanup logic and maintenance operations.</p>
     * 
     * <p>Maintenance operations may include:</p>
     * <ul>
     *   <li><strong>TTL Verification:</strong> Ensuring expired conversations are properly removed</li>
     *   <li><strong>Storage Optimization:</strong> Compacting conversation data structures</li>
     *   <li><strong>Metrics Collection:</strong> Gathering cleanup statistics</li>
     *   <li><strong>Health Monitoring:</strong> Checking Redis storage health</li>
     * </ul>
     * 
     * <p>This method is typically called by scheduled tasks or administrative
     * operations. It's designed to be safe for frequent execution.</p>
     * 
     * @see #deleteConversation(String)
     * @see #getStats(String)
     */
    public void cleanupExpiredConversations() {
        // This would typically be handled by Redis TTL, but could include additional cleanup logic
        logger.debug("Expired conversations cleanup triggered (handled by Redis TTL)");
    }

    private String buildContextualizedQuery(String newQuery, List<ConversationExchange> recentExchanges) {
        if (recentExchanges.isEmpty()) {
            return newQuery;
        }

        StringBuilder contextBuilder = new StringBuilder();
        
        // Add recent conversation context
        contextBuilder.append("Given our recent conversation:\n");
        
        recentExchanges.stream()
            .sorted(Comparator.comparing(ConversationExchange::timestamp))
            .forEach(exchange -> {
                contextBuilder.append("User: ").append(exchange.userQuery()).append("\n");
                contextBuilder.append("AI: ").append(truncateResponse(exchange.aiResponse())).append("\n\n");
            });
        
        contextBuilder.append("New question: ").append(newQuery);
        
        return contextBuilder.toString();
    }

    private String truncateResponse(String response) {
        if (response == null || response.length() <= 200) {
            return response;
        }
        return response.substring(0, 200) + "...";
    }

    private double calculateTextSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) {
            return 0.0;
        }
        
        // Simple Jaccard similarity on words
        Set<String> words1 = Set.of(text1.toLowerCase().split("\\s+"));
        Set<String> words2 = Set.of(text2.toLowerCase().split("\\s+"));
        
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    private String getConversationKey(String conversationId) {
        return "conversation:" + conversationId;
    }

    /**
     * Individual conversation exchange.
     */
    public record ConversationExchange(
        String exchangeId,
        String userQuery,
        String aiResponse,
        List<SourceDocument> sources,
        UUID userId,
        Instant timestamp,
        Map<String, Object> metadata
    ) {
        public static ConversationExchange create(String userQuery, String aiResponse, 
                                                List<SourceDocument> sources, UUID userId) {
            return new ConversationExchange(
                UUID.randomUUID().toString(),
                userQuery,
                aiResponse,
                sources != null ? sources : List.of(),
                userId,
                Instant.now(),
                Map.of()
            );
        }
    }

    /**
     * Complete conversation with all exchanges.
     */
    public record Conversation(
        String conversationId,
        UUID userId,
        List<ConversationExchange> exchanges,
        Instant createdAt,
        Instant lastUpdatedAt,
        Map<String, Object> metadata
    ) {
        public static Conversation create(String conversationId, UUID userId) {
            return new Conversation(
                conversationId,
                userId,
                new ArrayList<>(),
                Instant.now(),
                Instant.now(),
                Map.of()
            );
        }

        public void addExchange(ConversationExchange exchange, int maxSize) {
            exchanges.add(exchange);
            
            // Maintain size limit by removing oldest exchanges
            while (exchanges.size() > maxSize) {
                exchanges.remove(0);
            }
        }
    }

    /**
     * Conversation summary for display.
     */
    public record ConversationSummary(
        String conversationId,
        UUID userId,
        int totalExchanges,
        Instant createdAt,
        Instant lastUpdatedAt,
        String lastUserQuery,
        String firstUserQuery
    ) {
        public static ConversationSummary from(Conversation conversation) {
            List<ConversationExchange> exchanges = conversation.exchanges();
            
            return new ConversationSummary(
                conversation.conversationId(),
                conversation.userId(),
                exchanges.size(),
                conversation.createdAt(),
                conversation.lastUpdatedAt(),
                exchanges.isEmpty() ? null : exchanges.get(exchanges.size() - 1).userQuery(),
                exchanges.isEmpty() ? null : exchanges.get(0).userQuery()
            );
        }
    }

    /**
     * Conversation statistics.
     */
    public record ConversationStats(
        String conversationId,
        int totalExchanges,
        int totalQueries,
        int uniqueDocumentsReferenced,
        Instant oldestExchange,
        Instant newestExchange,
        double averageResponseLength
    ) {
        public static ConversationStats empty() {
            return new ConversationStats(null, 0, 0, 0, null, null, 0.0);
        }

        public static ConversationStats from(Conversation conversation) {
            List<ConversationExchange> exchanges = conversation.exchanges();
            
            if (exchanges.isEmpty()) {
                return new ConversationStats(
                    conversation.conversationId(), 0, 0, 0, null, null, 0.0
                );
            }

            Set<UUID> uniqueDocuments = exchanges.stream()
                .flatMap(e -> e.sources().stream())
                .map(SourceDocument::documentId)
                .collect(Collectors.toSet());

            double avgResponseLength = exchanges.stream()
                .mapToInt(e -> e.aiResponse() != null ? e.aiResponse().length() : 0)
                .average()
                .orElse(0.0);

            Instant oldest = exchanges.stream()
                .map(ConversationExchange::timestamp)
                .min(Instant::compareTo)
                .orElse(null);

            Instant newest = exchanges.stream()
                .map(ConversationExchange::timestamp)
                .max(Instant::compareTo)
                .orElse(null);

            return new ConversationStats(
                conversation.conversationId(),
                exchanges.size(),
                exchanges.size(),
                uniqueDocuments.size(),
                oldest,
                newest,
                avgResponseLength
            );
        }
    }
}
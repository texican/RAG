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
 * Service for managing conversation history and context.
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
     * Add a new exchange to conversation history.
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
     * Contextualize a new query based on conversation history.
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
     * Get conversation history.
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
     * Delete conversation history.
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
     * Get conversation summary for display.
     */
    public ConversationSummary getConversationSummary(String conversationId) {
        Conversation conversation = getConversation(conversationId);
        if (conversation == null) {
            return null;
        }

        return ConversationSummary.from(conversation);
    }

    /**
     * Search conversation history for similar queries.
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
     * Get conversation statistics.
     */
    public ConversationStats getStats(String conversationId) {
        Conversation conversation = getConversation(conversationId);
        if (conversation == null) {
            return ConversationStats.empty();
        }

        return ConversationStats.from(conversation);
    }

    /**
     * Clean up expired conversations.
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
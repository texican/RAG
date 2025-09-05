package com.byo.rag.core.service;

import com.byo.rag.shared.dto.DocumentChunkDto;
import com.byo.rag.core.dto.RagQueryResponse.SourceDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConversationService.
 */
@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private ConversationService conversationService;

    private UUID tenantId;
    private UUID userId;
    private String conversationId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        conversationId = "conv-123";
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // Set default properties
        ReflectionTestUtils.setField(conversationService, "maxConversationSize", 50);
        ReflectionTestUtils.setField(conversationService, "conversationTtlHours", 24);
        ReflectionTestUtils.setField(conversationService, "contextWindowSize", 10);
    }

    @Test
    void addExchange_NewConversation_CreatesConversation() {
        String query = "What is Spring AI?";
        String response = "Spring AI is a framework...";
        List<SourceDocument> sources = List.of();

        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        doNothing().when(valueOperations).set(anyString(), any(), anyLong(), any(TimeUnit.class));

        assertDoesNotThrow(() -> 
            conversationService.addExchange(conversationId, userId, query, response, sources));

        verify(valueOperations).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void addExchange_ExistingConversation_UpdatesConversation() {
        String query = "Tell me more";
        String response = "Here's more information...";
        List<SourceDocument> sources = List.of();

        // Mock existing conversation
        ConversationService.Conversation existingConv = ConversationService.Conversation.create(conversationId, userId);
        when(redisTemplate.hasKey(anyString())).thenReturn(true);
        when(valueOperations.get(anyString())).thenReturn(existingConv);
        doNothing().when(valueOperations).set(anyString(), any(), anyLong(), any(TimeUnit.class));

        assertDoesNotThrow(() -> 
            conversationService.addExchange(conversationId, userId, query, response, sources));

        verify(valueOperations).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void contextualizeQuery_ExistingConversation_ReturnsContext() {
        String newQuery = "What about performance?";
        
        // Create mock conversation with history
        ConversationService.Conversation mockConv = ConversationService.Conversation.create(conversationId, userId);
        when(valueOperations.get(anyString())).thenReturn(mockConv);

        String context = conversationService.contextualizeQuery(conversationId, newQuery);

        assertNotNull(context);
        assertTrue(context.contains("New question: " + newQuery));
    }

    @Test
    void contextualizeQuery_NoConversation_ReturnsQueryOnly() {
        String newQuery = "What is machine learning?";
        
        when(valueOperations.get(anyString())).thenReturn(null);

        String context = conversationService.contextualizeQuery(conversationId, newQuery);

        assertNotNull(context);
        assertEquals("New question: " + newQuery, context);
    }

    @Test
    void getConversationSummary_ExistingConversation_ReturnsSummary() {
        ConversationService.Conversation mockConv = ConversationService.Conversation.create(conversationId, userId);
        when(valueOperations.get(anyString())).thenReturn(mockConv);

        ConversationService.ConversationSummary summary = conversationService.getConversationSummary(conversationId);

        assertNotNull(summary);
        assertEquals(conversationId, summary.conversationId());
        assertEquals(userId, summary.userId());
        assertEquals(0, summary.totalExchanges());
    }

    @Test
    void getConversationSummary_NonExistentConversation_ReturnsNull() {
        when(valueOperations.get(anyString())).thenReturn(null);

        ConversationService.ConversationSummary summary = conversationService.getConversationSummary(conversationId);

        assertNull(summary);
    }

    @Test
    void deleteConversation_ExistingConversation_ReturnsTrue() {
        when(redisTemplate.delete(anyString())).thenReturn(true);

        boolean deleted = conversationService.deleteConversation(conversationId);

        assertTrue(deleted);
        verify(redisTemplate).delete(anyString());
    }

    @Test
    void deleteConversation_NonExistentConversation_ReturnsFalse() {
        when(redisTemplate.delete(anyString())).thenReturn(false);

        boolean deleted = conversationService.deleteConversation(conversationId);

        assertFalse(deleted);
        verify(redisTemplate).delete(anyString());
    }

    @Test
    void getStats_ExistingConversation_ReturnsStats() {
        ConversationService.Conversation mockConv = ConversationService.Conversation.create(conversationId, userId);
        when(valueOperations.get(anyString())).thenReturn(mockConv);

        ConversationService.ConversationStats stats = conversationService.getStats(conversationId);

        assertNotNull(stats);
        assertEquals(conversationId, stats.conversationId());
        assertEquals(0, stats.totalExchanges());
    }

    @Test
    void getStats_NonExistentConversation_ReturnsEmptyStats() {
        when(valueOperations.get(anyString())).thenReturn(null);

        ConversationService.ConversationStats stats = conversationService.getStats(conversationId);

        assertEquals(ConversationService.ConversationStats.empty(), stats);
    }

    @Test
    void findSimilarExchanges_ExistingConversation_ReturnsExchanges() {
        String query = "machine learning";
        int limit = 5;
        
        ConversationService.Conversation mockConv = ConversationService.Conversation.create(conversationId, userId);
        when(valueOperations.get(anyString())).thenReturn(mockConv);

        List<ConversationService.ConversationExchange> exchanges = 
            conversationService.findSimilarExchanges(conversationId, query, limit);

        assertNotNull(exchanges);
        assertTrue(exchanges.size() <= limit);
    }

    @Test
    void findSimilarExchanges_NoConversation_ReturnsEmptyList() {
        String query = "machine learning";
        int limit = 5;
        
        when(valueOperations.get(anyString())).thenReturn(null);

        List<ConversationService.ConversationExchange> exchanges = 
            conversationService.findSimilarExchanges(conversationId, query, limit);

        assertNotNull(exchanges);
        assertTrue(exchanges.isEmpty());
    }

    @Test
    void cleanupExpiredConversations_ExecutesSuccessfully() {
        assertDoesNotThrow(() -> conversationService.cleanupExpiredConversations());
    }
}
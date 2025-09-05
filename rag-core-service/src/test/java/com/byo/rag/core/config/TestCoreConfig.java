package com.byo.rag.core.config;

import com.byo.rag.core.client.EmbeddingServiceClient;
import com.byo.rag.core.service.*;
import com.byo.rag.core.dto.RagQueryRequest;
import com.byo.rag.shared.dto.DocumentChunkDto;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Test configuration for RAG Core Service tests.
 * Provides mock implementations of external dependencies.
 */
@TestConfiguration
@EnableWebSecurity
public class TestCoreConfig {

    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz.anyRequest().permitAll())
            .csrf(csrf -> csrf.disable())
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(formLogin -> formLogin.disable());
        return http.build();
    }

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public RedisTemplate<String, Object> mockRedisTemplate() {
        RedisTemplate<String, Object> mockTemplate = Mockito.mock(RedisTemplate.class);
        ValueOperations<String, Object> mockValueOps = Mockito.mock(ValueOperations.class);
        
        // Mock basic Redis operations for ConversationService
        Mockito.when(mockTemplate.hasKey(Mockito.anyString())).thenReturn(false);
        Mockito.when(mockTemplate.opsForValue()).thenReturn(mockValueOps);
        Mockito.when(mockTemplate.expire(Mockito.anyString(), Mockito.anyLong(), Mockito.any(TimeUnit.class))).thenReturn(true);
        
        return mockTemplate;
    }

    @Bean
    public EmbeddingServiceClient mockEmbeddingServiceClient() {
        EmbeddingServiceClient mockClient = Mockito.mock(EmbeddingServiceClient.class);
        
        // Sample document chunks created but not used in mock response
        @SuppressWarnings("unused")
        List<DocumentChunkDto> mockChunks = List.of(
            new DocumentChunkDto(
                UUID.randomUUID(),
                "Sample document content for testing RAG functionality",
                1,
                0,
                100,
                25,
                "document.pdf",
                Map.of("score", 0.85)
            )
        );
        
        Mockito.when(mockClient.search(Mockito.any(), Mockito.any()))
            .thenReturn(new EmbeddingServiceClient.SearchResponse(
                UUID.randomUUID(),
                "test query",
                "test-model",
                List.of(),
                1,
                0.85,
                100L,
                "2024-01-01T10:00:00Z"
            ));
        
        return mockClient;
    }

    // ChatClient is not needed in test config - LLMIntegrationService is mocked separately

    @Bean
    @Primary
    public CacheService mockCacheService() {
        CacheService mockService = Mockito.mock(CacheService.class);
        
        // Mock cache miss for most tests
        Mockito.when(mockService.getResponse(Mockito.anyString()))
            .thenReturn(null);
        
        Mockito.doNothing().when(mockService)
            .cacheResponse(Mockito.anyString(), Mockito.any());
        
        return mockService;
    }

    @Bean
    @Primary
    public ConversationService mockConversationService() {
        ConversationService mockService = Mockito.mock(ConversationService.class);
        
        // Mock conversation retrieval
        Mockito.when(mockService.getConversation(Mockito.anyString()))
            .thenReturn(null);  // Return null for new conversation
        
        // Mock conversation summary - use simple values for parameters
        ConversationService.ConversationSummary mockSummary = Mockito.mock(ConversationService.ConversationSummary.class);
        Mockito.when(mockService.getConversationSummary(Mockito.anyString()))
            .thenReturn(mockSummary);
        
        return mockService;
    }

    @Bean
    @Primary
    public QueryOptimizationService mockQueryOptimizationService() {
        QueryOptimizationService mockService = Mockito.mock(QueryOptimizationService.class);
        
        // Mock optimization - return optimized request
        Mockito.when(mockService.optimizeQuery(Mockito.any()))
            .thenAnswer(invocation -> {
                RagQueryRequest req = invocation.getArgument(0);
                return RagQueryRequest.simple(req.tenantId(), "optimized " + req.query());
            });
        
        return mockService;
    }

    @Bean
    @Primary
    public ContextAssemblyService mockContextAssemblyService() {
        ContextAssemblyService mockService = Mockito.mock(ContextAssemblyService.class);
        
        // Mock context assembly - need to check actual method signature
        // Using any() for all parameters to avoid signature issues
        Mockito.when(mockService.assembleContext(Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn("Assembled context from retrieved documents for testing");
        
        return mockService;
    }

    @Bean
    @Primary 
    public VectorSearchService mockVectorSearchService() {
        VectorSearchService mockService = Mockito.mock(VectorSearchService.class);
        
        // Sample document chunks created for reference but not returned by mock
        @SuppressWarnings("unused")
        List<DocumentChunkDto> mockResults = List.of(
            new DocumentChunkDto(
                UUID.randomUUID(),
                "Mock document content for vector search testing",
                1,
                0,
                150,
                30,
                "test-document.pdf",
                Map.of("score", 0.9)
            )
        );
        
        // Mock findSimilarChunks method with correct signature
        Mockito.when(mockService.findSimilarChunks(Mockito.anyString(), Mockito.any(UUID.class), Mockito.anyInt(), Mockito.anyDouble()))
            .thenReturn(List.of("mock-chunk-1", "mock-chunk-2"));
        
        return mockService;
    }

    @Bean
    @Primary
    public LLMIntegrationService mockLLMIntegrationService() {
        LLMIntegrationService mockService = Mockito.mock(LLMIntegrationService.class);
        
        // Mock LLM response generation with correct signature
        Mockito.when(mockService.generateResponse(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
            .thenReturn("Mock LLM response for testing RAG functionality");
        
        // Mock streaming response
        Mockito.when(mockService.generateResponseStreaming(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
            .thenReturn(Flux.just("Mock", " streaming", " response"));
        
        return mockService;
    }
}
package com.byo.rag.embedding.config;

import com.byo.rag.embedding.config.EmbeddingConfig.EmbeddingModelProperties;
import com.byo.rag.embedding.config.RedisConfig.VectorStorageProperties;
import com.byo.rag.embedding.service.VectorStorageService;
import com.byo.rag.embedding.service.VectorStorageService.VectorStats;
import com.byo.rag.embedding.dto.SearchRequest;
import com.byo.rag.embedding.dto.SearchResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.mockito.Mockito;

import java.util.UUID;
import java.util.List;

/**
 * Test configuration for embedding service tests.
 */
@TestConfiguration
@EnableWebSecurity
public class TestEmbeddingConfig {
    
    @Bean
    @Primary
    public EmbeddingModelProperties testEmbeddingModelProperties() {
        return new EmbeddingModelProperties(
            "test-model",
            "test-fallback",
            60,
            new EmbeddingModelProperties.OpenAIConfig("test-api-key", "text-embedding-3-small", 1536),
            new EmbeddingModelProperties.TransformersConfig("models/sentence-transformers/all-MiniLM-L6-v2")
        );
    }
    
    @Bean
    @Primary
    public VectorStorageProperties testVectorStorageProperties() {
        return new VectorStorageProperties(
            "localhost",
            6370,  // Use test Redis port
            0,     // Use database 0 for tests
            2000,
            "test:vectors",
            100,
            384,   // Test dimension
            "COSINE",
            200,
            10,
            16,
            8,
            2
        );
    }
    
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
    public VectorStorageService testVectorStorageService() {
        VectorStorageService mockService = Mockito.mock(VectorStorageService.class);
        
        // Mock all methods to do nothing or return empty results
        Mockito.doNothing().when(mockService).storeEmbeddings(
            Mockito.any(UUID.class), Mockito.anyString(), Mockito.anyList());
        Mockito.doNothing().when(mockService).deleteVectors(
            Mockito.any(UUID.class), Mockito.anyString());
        
        // Return empty search results  
        SearchResponse emptyResponse = new SearchResponse(
            UUID.randomUUID(), "test query", "test-model", List.of(), 0, 0.0, 0L, java.time.Instant.now());
        Mockito.when(mockService.searchSimilar(Mockito.any(SearchRequest.class), Mockito.anyList()))
            .thenReturn(emptyResponse);
        
        // Mock getStats to return test statistics
        VectorStats testStats = new VectorStats(0L, 0L, 0.0, System.currentTimeMillis());
        Mockito.when(mockService.getStats()).thenReturn(testStats);
        
        return mockService;
    }
}
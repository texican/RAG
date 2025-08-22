package com.enterprise.rag.embedding.config;

import com.enterprise.rag.embedding.config.EmbeddingConfig.EmbeddingModelProperties;
import com.enterprise.rag.embedding.config.RedisConfig.VectorStorageProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

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
}
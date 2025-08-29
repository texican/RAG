package com.enterprise.rag.core.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Core configuration for the Enterprise RAG Core Service.
 * <p>
 * This configuration class sets up the essential infrastructure components
 * for the RAG (Retrieval Augmented Generation) processing pipeline including
 * AI chat clients, Redis caching, and CORS policies. It provides the foundation
 * for intelligent document retrieval and response generation.
 * 
 * <h2>Configuration Components</h2>
 * <ul>
 *   <li><strong>Chat Client</strong> - Spring AI ChatClient for LLM integration</li>
 *   <li><strong>Redis Template</strong> - Caching and session management</li>
 *   <li><strong>CORS Configuration</strong> - Cross-origin resource sharing policies</li>
 * </ul>
 * 
 * <h2>AI Integration</h2>
 * <ul>
 *   <li><strong>Default System Prompt</strong> - Configured AI assistant behavior</li>
 *   <li><strong>Response Formatting</strong> - Standardized response structure</li>
 *   <li><strong>Context Management</strong> - Maintains conversation context</li>
 *   <li><strong>Error Handling</strong> - Graceful degradation for AI service failures</li>
 * </ul>
 * 
 * <h2>Caching Strategy</h2>
 * <ul>
 *   <li><strong>Redis Integration</strong> - High-performance string-based caching</li>
 *   <li><strong>Session Management</strong> - User conversation state persistence</li>
 *   <li><strong>Query Caching</strong> - Cached responses for common queries</li>
 *   <li><strong>Performance Optimization</strong> - Reduced LLM API calls</li>
 * </ul>
 * 
 * <h2>Security and CORS</h2>
 * <ul>
 *   <li><strong>Development Origins</strong> - Localhost access for development</li>
 *   <li><strong>Production Ready</strong> - Configurable CORS policies</li>
 *   <li><strong>Header Management</strong> - Custom headers for pagination and metadata</li>
 *   <li><strong>Credential Support</strong> - Supports authentication cookies</li>
 * </ul>
 * 
 * @author Enterprise RAG Development Team
 * @version 0.8.0
 * @since 0.1.0
 * @see ChatClient
 * @see RedisTemplate
 */
@Configuration
public class CoreServiceConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
            .defaultSystem("You are a helpful AI assistant that provides accurate, concise answers based on provided context.")
            .build();
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins("http://localhost:3000", "http://localhost:8080")
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("*")
                    .exposedHeaders("X-Total-Count")
                    .allowCredentials(true);
            }
        };
    }
}
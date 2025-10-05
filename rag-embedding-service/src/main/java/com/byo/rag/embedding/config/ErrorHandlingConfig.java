package com.byo.rag.embedding.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.Collections;

/**
 * Configuration for error handling, retry mechanisms, and circuit breakers.
 */
@Configuration
@EnableRetry
public class ErrorHandlingConfig {

    /**
     * Retry template for embedding operations with exponential backoff.
     */
    @Bean
    public RetryTemplate embeddingRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        // Retry policy: retry up to 3 times on specific exceptions
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
            3, 
            Collections.singletonMap(Exception.class, true)
        );
        retryTemplate.setRetryPolicy(retryPolicy);
        
        // Exponential backoff: start with 1s, max 10s, multiply by 2
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000L);
        backOffPolicy.setMaxInterval(10000L);
        backOffPolicy.setMultiplier(2.0);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        return retryTemplate;
    }

    /**
     * Circuit breaker configuration for embedding service calls.
     * NOTE: Configuration moved to application.yml to avoid conflicts with YAML-based config.
     * See resilience4j.circuitbreaker.instances.embeddingService in application.yml
     */
    // REMOVED: Caused "waitIntervalFunction was configured multiple times" error
    // The circuit breaker configuration is now fully managed via application.yml

    /**
     * Circuit breaker configuration for Kafka operations.
     * NOTE: Configuration moved to application.yml to avoid conflicts with YAML-based config.
     * See resilience4j.circuitbreaker.instances.kafka in application.yml
     */
    // REMOVED: Caused "waitIntervalFunction was configured multiple times" error
    // The circuit breaker configuration is now fully managed via application.yml
}
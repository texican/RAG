package com.byo.rag.embedding.config;

import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.time.Duration;
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
     */
    @Bean
    public CircuitBreakerConfigCustomizer embeddingServiceCircuitBreakerCustomizer() {
        return CircuitBreakerConfigCustomizer
            .of("embeddingService", builder -> builder
                .failureRateThreshold(50) // Open circuit if 50% of calls fail
                .waitDurationInOpenState(Duration.ofSeconds(30)) // Wait 30s before transitioning to half-open
                .slidingWindowSize(20) // Consider last 20 calls for failure rate
                .minimumNumberOfCalls(5) // Need at least 5 calls before calculating failure rate
                .permittedNumberOfCallsInHalfOpenState(3) // Allow 3 test calls in half-open state
                .slowCallRateThreshold(50) // Consider slow calls in failure rate
                .slowCallDurationThreshold(Duration.ofSeconds(10)) // Calls slower than 10s are considered slow
                .build());
    }

    /**
     * Circuit breaker configuration for Kafka operations.
     */
    @Bean
    public CircuitBreakerConfigCustomizer kafkaCircuitBreakerCustomizer() {
        return CircuitBreakerConfigCustomizer
            .of("kafka", builder -> builder
                .failureRateThreshold(60) // More tolerant for Kafka operations
                .waitDurationInOpenState(Duration.ofSeconds(15)) // Shorter wait for Kafka
                .slidingWindowSize(10)
                .minimumNumberOfCalls(3)
                .permittedNumberOfCallsInHalfOpenState(2)
                .build());
    }
}
package com.byo.rag.core.client;

import feign.Logger;
import feign.Request;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configuration for embedding service Feign client.
 */
@Configuration
public class EmbeddingServiceClientConfig {
    
    @Value("${services.embedding.timeout:30s}")
    private String timeout;
    
    @Value("${services.embedding.retry-attempts:3}")
    private int retryAttempts;
    
    /**
     * Configure request timeout.
     */
    @Bean
    public Request.Options requestOptions() {
        long timeoutMs = parseTimeout(timeout);
        return new Request.Options(
            timeoutMs,  // connectTimeout
            TimeUnit.MILLISECONDS,
            timeoutMs,  // readTimeout  
            TimeUnit.MILLISECONDS,
            true        // followRedirects
        );
    }
    
    /**
     * Configure retry policy.
     */
    @Bean
    public Retryer retryer() {
        return new Retryer.Default(
            1000,              // period (initial interval)
            3000,              // maxPeriod (max interval)  
            retryAttempts      // maxAttempts
        );
    }
    
    /**
     * Configure logging level.
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }
    
    /**
     * Configure custom error decoder.
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return new EmbeddingServiceErrorDecoder();
    }
    
    private long parseTimeout(String timeoutStr) {
        if (timeoutStr.endsWith("s")) {
            return Long.parseLong(timeoutStr.substring(0, timeoutStr.length() - 1)) * 1000;
        } else if (timeoutStr.endsWith("ms")) {
            return Long.parseLong(timeoutStr.substring(0, timeoutStr.length() - 2));
        } else {
            return Long.parseLong(timeoutStr);
        }
    }
}
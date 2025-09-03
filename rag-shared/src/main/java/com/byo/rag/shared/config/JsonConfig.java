package com.byo.rag.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * JSON serialization configuration for the Enterprise RAG System.
 * <p>
 * This configuration class provides standardized JSON serialization settings
 * across all microservices in the RAG system. It ensures consistent date/time
 * formatting, proper Java 8+ time support, and enterprise-grade JSON handling.
 * </p>
 * 
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Java Time Support</strong> - Proper serialization of LocalDateTime, Instant, etc.</li>
 *   <li><strong>ISO 8601 Date Format</strong> - Standardized timestamp formatting instead of numeric timestamps</li>
 *   <li><strong>Consistent API Responses</strong> - Uniform JSON structure across all REST endpoints</li>
 * </ul>
 * 
 * <h3>Date/Time Handling:</h3>
 * <p>
 * All date/time fields are serialized in ISO 8601 format (e.g., "2023-12-25T10:30:00Z")
 * instead of numeric timestamps, improving API readability and client integration.
 * </p>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Entity with temporal fields
 * public class Document {
 *     private LocalDateTime createdDate; // Serialized as "2023-12-25T10:30:00"
 *     private Instant lastModified;      // Serialized as "2023-12-25T10:30:00Z"
 * }
 * 
 * // JSON output
 * {
 *   "createdDate": "2023-12-25T10:30:00",
 *   "lastModified": "2023-12-25T10:30:00Z"
 * }
 * }</pre>
 * 
 * @author Enterprise RAG Development Team
 * @since 1.0.0
 * @see com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
 * @see com.fasterxml.jackson.databind.ObjectMapper
 */
@Configuration
public class JsonConfig {

    /**
     * Creates the primary ObjectMapper bean with enterprise-standard JSON configuration.
     * <p>
     * This ObjectMapper is configured with:
     * <ul>
     *   <li>JavaTimeModule for Java 8+ date/time types</li>
     *   <li>Disabled timestamp serialization for human-readable dates</li>
     *   <li>Consistent formatting across all REST endpoints</li>
     * </ul>
     * </p>
     * 
     * @return configured ObjectMapper instance for JSON serialization/deserialization
     * @see JavaTimeModule
     * @see SerializationFeature#WRITE_DATES_AS_TIMESTAMPS
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return Jackson2ObjectMapperBuilder.json()
            .modules(new JavaTimeModule())
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
    }
}
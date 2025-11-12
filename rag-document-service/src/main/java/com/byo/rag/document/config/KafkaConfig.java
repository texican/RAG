package com.byo.rag.document.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Kafka configuration for document processing pipeline.
 *
 * <p>Enables Kafka listeners and relies on Spring Boot autoconfiguration
 * for producer and consumer setup based on application.yml properties.</p>
 *
 * <p>This configuration is only active when Kafka is enabled via the
 * spring.kafka.enabled property (defaults to false).</p>
 *
 * <p>Configuration properties:</p>
 * <ul>
 *   <li>spring.kafka.enabled - Enable/disable Kafka (default: false)</li>
 *   <li>spring.kafka.bootstrap-servers - Kafka broker addresses</li>
 *   <li>kafka.topics.document-processing - Document processing topic name</li>
 *   <li>kafka.topics.embedding-generation - Embedding generation topic name</li>
 *   <li>kafka.consumer.group-id - Consumer group identifier</li>
 * </ul>
 */
@Configuration
@EnableKafka
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class KafkaConfig {
    // Rely on Spring Boot autoconfiguration
    // Configuration is driven by application.yml properties
}

package com.enterprise.rag.document.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Apache Kafka configuration for the Enterprise RAG Document Service.
 * <p>
 * This configuration sets up Kafka producers for event-driven document processing
 * pipeline. It enables asynchronous communication between the document service
 * and downstream services (embedding service, core service) for real-time
 * document processing and indexing.
 * 
 * <h2>Event-Driven Architecture</h2>
 * <ul>
 *   <li><strong>Document Processing Events</strong> - Publishes document lifecycle events</li>
 *   <li><strong>Chunking Notifications</strong> - Notifies when document chunks are ready</li>
 *   <li><strong>Processing Status</strong> - Publishes processing status updates</li>
 *   <li><strong>Error Notifications</strong> - Publishes processing error events</li>
 * </ul>
 * 
 * <h2>Kafka Producer Configuration</h2>
 * <ul>
 *   <li><strong>String Serialization</strong> - JSON string serialization for message payloads</li>
 *   <li><strong>Acknowledgment Level</strong> - Level 1 acknowledgments for performance/reliability balance</li>
 *   <li><strong>Retry Configuration</strong> - 3 retry attempts for transient failures</li>
 *   <li><strong>Batch Configuration</strong> - Optimized batching for high throughput</li>
 *   <li><strong>Memory Management</strong> - Configured buffer memory for efficient processing</li>
 * </ul>
 * 
 * <h2>Performance Optimization</h2>
 * <ul>
 *   <li><strong>Batch Size</strong> - 16KB batches for optimal network utilization</li>
 *   <li><strong>Linger Time</strong> - 1ms linger for low-latency message delivery</li>
 *   <li><strong>Buffer Memory</strong> - 32MB buffer for high-throughput scenarios</li>
 *   <li><strong>Connection Pooling</strong> - Efficient connection management</li>
 * </ul>
 * 
 * <h2>Conditional Configuration</h2>
 * <ul>
 *   <li><strong>Environment Switching</strong> - Kafka can be disabled for testing</li>
 *   <li><strong>Development Mode</strong> - Simplified configuration for local development</li>
 *   <li><strong>Production Ready</strong> - Scalable configuration for production workloads</li>
 * </ul>
 * 
 * <h2>Message Topics</h2>
 * <ul>
 *   <li><strong>document.processed</strong> - Document processing completion events</li>
 *   <li><strong>document.chunked</strong> - Document chunking completion events</li>
 *   <li><strong>document.error</strong> - Document processing error events</li>
 *   <li><strong>document.deleted</strong> - Document deletion events</li>
 * </ul>
 * 
 * @author Enterprise RAG Development Team
 * @version 0.8.0
 * @since 0.1.0
 * @see org.springframework.kafka.core.KafkaTemplate
 * @see org.apache.kafka.clients.producer.ProducerConfig
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.producer.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    @ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

}
package com.byo.rag.integration.config;

import com.byo.rag.integration.data.TestDataCleanup;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.time.Duration;

/**
 * Main configuration class for integration tests.
 * 
 * This configuration sets up the Spring Boot application context for integration testing,
 * including database configuration, REST clients, and test-specific components.
 */
@SpringBootApplication
@EnableAutoConfiguration(exclude = {KafkaAutoConfiguration.class}) // Disable Kafka for now
@EntityScan(basePackages = "com.byo.rag.shared.entity")
@EnableJpaRepositories(basePackages = "com.byo.rag.shared.repository")
@ComponentScan(basePackages = {
    "com.byo.rag.shared",
    "com.byo.rag.integration"
})
public class IntegrationTestConfig {

    /**
     * Configure RestTemplate for integration testing with appropriate timeouts.
     */
    @Bean
    @Primary
    public RestTemplate restTemplate() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setConnectionRequestTimeout(Duration.ofSeconds(10));
        
        RestTemplate restTemplate = new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .requestFactory(() -> factory)
                .build();
                
        // Configure JSON message converter with Java time support
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper);
        
        restTemplate.getMessageConverters().removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
        restTemplate.getMessageConverters().add(converter);
        
        return restTemplate;
    }

    /**
     * Configure ObjectMapper for JSON processing in tests.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    /**
     * Test data cleanup component for maintaining test isolation.
     */
    @Bean
    public TestDataCleanup testDataCleanup() {
        return new TestDataCleanup();
    }
}
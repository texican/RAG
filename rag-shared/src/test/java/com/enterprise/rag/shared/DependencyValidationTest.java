package com.enterprise.rag.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to prevent missing dependency errors that occurred during development.
 * These tests validate that all required dependencies are available at compile time.
 */
public class DependencyValidationTest {

    @Test
    @DisplayName("Jackson datatype JSR310 should be available")
    void shouldHaveJacksonDatatypeJsr310() {
        assertDoesNotThrow(() -> {
            Class.forName("com.fasterxml.jackson.datatype.jsr310.JavaTimeModule");
        }, "Jackson JSR310 module should be available - add jackson-datatype-jsr310 dependency");
    }

    @Test
    @DisplayName("Spring Boot Web should be available")
    void shouldHaveSpringBootWeb() {
        assertDoesNotThrow(() -> {
            Class.forName("org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration");
        }, "Spring Boot Web should be available - add spring-boot-starter-web dependency");
    }

    @Test
    @DisplayName("Spring Security should be available")
    void shouldHaveSpringSecurity() {
        assertDoesNotThrow(() -> {
            Class.forName("org.springframework.security.config.annotation.web.configuration.EnableWebSecurity");
        }, "Spring Security should be available - add spring-boot-starter-security dependency");
    }

    @Test
    @DisplayName("Jackson core should be available for JSON processing")
    void shouldHaveJacksonCore() {
        assertDoesNotThrow(() -> {
            Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
        }, "Jackson databind should be available for JSON processing");
    }

    @Test
    @DisplayName("Validation API should be available")
    void shouldHaveValidationApi() {
        assertDoesNotThrow(() -> {
            Class.forName("jakarta.validation.constraints.NotNull");
        }, "Validation API should be available - add spring-boot-starter-validation dependency");
    }

    @Test
    @DisplayName("Apache Tika should be available for document processing")
    void shouldHaveApacheTika() {
        assertDoesNotThrow(() -> {
            Class.forName("org.apache.tika.Tika");
            Class.forName("org.apache.tika.parser.AutoDetectParser");
        }, "Apache Tika should be available - add tika-core and tika-parsers-standard-package dependencies");
    }

    @Test
    @DisplayName("Kafka should be available for message processing")
    void shouldHaveKafka() {
        assertDoesNotThrow(() -> {
            Class.forName("org.springframework.kafka.core.KafkaTemplate");
            Class.forName("org.apache.kafka.clients.producer.ProducerRecord");
        }, "Kafka should be available - add spring-kafka dependency");
    }

    @Test
    @DisplayName("PostgreSQL driver should be available")
    void shouldHavePostgreSQLDriver() {
        assertDoesNotThrow(() -> {
            Class.forName("org.postgresql.Driver");
        }, "PostgreSQL driver should be available - add postgresql dependency");
    }

    @Test
    @DisplayName("Redis should be available")
    void shouldHaveRedis() {
        assertDoesNotThrow(() -> {
            Class.forName("org.springframework.data.redis.core.RedisTemplate");
        }, "Redis should be available - add spring-boot-starter-data-redis dependency");
    }

    @Test
    @DisplayName("JPA should be available")
    void shouldHaveJPA() {
        assertDoesNotThrow(() -> {
            Class.forName("org.springframework.data.jpa.repository.JpaRepository");
            Class.forName("jakarta.persistence.Entity");
        }, "JPA should be available - add spring-boot-starter-data-jpa dependency");
    }
}
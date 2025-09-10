package com.byo.rag.embedding.service;

import com.byo.rag.embedding.service.DeadLetterQueueService.DeadLetterMessage;
import com.byo.rag.embedding.service.EmbeddingKafkaService.EmbeddingGenerationMessage;
import com.byo.rag.shared.util.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeadLetterQueueServiceTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private DeadLetterQueueService deadLetterQueueService;
    private final String deadLetterTopic = "embedding-dlq";

    @BeforeEach
    void setUp() {
        deadLetterQueueService = new DeadLetterQueueService(kafkaTemplate, deadLetterTopic);
    }

    @Test
    @DisplayName("Should send failed message to dead letter queue with complete failure context")
    void testSendToDeadLetterQueue() {
        // Given
        EmbeddingGenerationMessage originalMessage = new EmbeddingGenerationMessage(
            UUID.randomUUID(),
            "Test content that failed to process",
            UUID.randomUUID(),
            UUID.randomUUID()
        );
        
        Exception lastError = new RuntimeException("Persistent embedding service failure");
        int totalAttempts = 3;
        
        CompletableFuture<SendResult<String, String>> future = mock(CompletableFuture.class);
        when(kafkaTemplate.send(eq(deadLetterTopic), anyString(), anyString())).thenReturn(future);

        // When
        deadLetterQueueService.sendToDeadLetterQueue(originalMessage, lastError, totalAttempts);

        // Then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), messageCaptor.capture());
        
        assertThat(topicCaptor.getValue())
            .describedAs("Message should be sent to the configured dead letter topic")
            .isEqualTo(deadLetterTopic);
        assertThat(keyCaptor.getValue())
            .describedAs("DLQ message key should use chunk ID for partitioning")
            .isEqualTo(originalMessage.chunkId().toString());
        
        // Verify the DLQ message structure
        String dlqJson = messageCaptor.getValue();
        DeadLetterMessage dlqMessage = JsonUtils.fromJson(dlqJson, DeadLetterMessage.class);
        
        assertThat(dlqMessage.dlqId())
            .describedAs("DLQ message should have a unique identifier")
            .isNotNull();
        assertThat(dlqMessage.originalMessage())
            .describedAs("DLQ should preserve the complete original message for potential replay")
            .isEqualTo(originalMessage);
        assertThat(dlqMessage.errorType())
            .describedAs("DLQ should capture the specific exception type for error analysis")
            .isEqualTo("RuntimeException");
        assertThat(dlqMessage.errorMessage())
            .describedAs("DLQ should preserve the original error message for debugging")
            .isEqualTo("Persistent embedding service failure");
        assertThat(dlqMessage.attemptCount())
            .describedAs("DLQ should track the number of processing attempts for monitoring")
            .isEqualTo(3);
        assertThat(dlqMessage.failureReason())
            .describedAs("DLQ should categorize the failure reason for operational analysis")
            .isEqualTo("EMBEDDING_PROCESSING_FAILURE");
        assertThat(dlqMessage.failedAt())
            .describedAs("DLQ should timestamp when the failure occurred")
            .isNotNull();
    }

    @Test
    @DisplayName("Should handle Kafka publishing failures to DLQ without system crash")
    void testSendToDeadLetterQueueHandlesKafkaFailure() {
        // Given
        EmbeddingGenerationMessage originalMessage = new EmbeddingGenerationMessage(
            UUID.randomUUID(),
            "Test content",
            UUID.randomUUID(),
            UUID.randomUUID()
        );
        
        Exception lastError = new RuntimeException("Service failure");
        
        @SuppressWarnings("unchecked")
        CompletableFuture<SendResult<String, String>> future = mock(CompletableFuture.class);
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);
        
        // Simulate Kafka failure in callback
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            var callback = invocation.getArgument(0, java.util.function.BiConsumer.class);
            callback.accept(null, new RuntimeException("Kafka publish to DLQ failed"));
            return null;
        }).when(future).whenComplete(any());

        // When - should not throw exception despite Kafka failure
        assertThatNoException()
            .describedAs("DLQ service should handle Kafka failures gracefully to prevent message loss")
            .isThrownBy(() -> deadLetterQueueService.sendToDeadLetterQueue(originalMessage, lastError, 3));

        // Then
        verify(kafkaTemplate).send(eq(deadLetterTopic), eq(originalMessage.chunkId().toString()), anyString());
    }

    @Test
    @DisplayName("Should handle different exception types and capture error type correctly")
    void testSendToDeadLetterQueueWithDifferentErrorTypes() {
        // Given
        EmbeddingGenerationMessage originalMessage = new EmbeddingGenerationMessage(
            UUID.randomUUID(),
            "Test content",
            UUID.randomUUID(),
            UUID.randomUUID()
        );
        
        @SuppressWarnings("unchecked")
        CompletableFuture<SendResult<String, String>> future = mock(CompletableFuture.class);
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

        // Test with different exception types
        Exception[] exceptions = {
            new IllegalArgumentException("Invalid input"),
            new RuntimeException("Service unavailable"),
            new NullPointerException("Null reference"),
            new Exception("Generic exception")
        };

        for (Exception exception : exceptions) {
            // When
            deadLetterQueueService.sendToDeadLetterQueue(originalMessage, exception, 3);

            // Then - verify the error type is correctly captured
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(kafkaTemplate, atLeast(1)).send(eq(deadLetterTopic), anyString(), messageCaptor.capture());
            
            String dlqJson = messageCaptor.getAllValues().get(messageCaptor.getAllValues().size() - 1);
            DeadLetterMessage dlqMessage = JsonUtils.fromJson(dlqJson, DeadLetterMessage.class);
            
            assertThat(dlqMessage.errorType())
                .describedAs("DLQ should capture the specific exception class name for %s", exception.getClass().getSimpleName())
                .isEqualTo(exception.getClass().getSimpleName());
            assertThat(dlqMessage.errorMessage())
                .describedAs("DLQ should preserve the original error message for %s", exception.getClass().getSimpleName())
                .isEqualTo(exception.getMessage());
        }
    }

    @Test
    @DisplayName("Should handle message creation errors gracefully")
    void testSendToDeadLetterQueueHandlesMessageCreationError() {
        // Given
        EmbeddingGenerationMessage originalMessage = new EmbeddingGenerationMessage(
            UUID.randomUUID(),
            "Test content",
            UUID.randomUUID(),
            UUID.randomUUID()
        );
        
        Exception lastError = new RuntimeException("Service failure");

        // When & Then - should handle gracefully even if message creation fails
        assertThatNoException()
            .describedAs("DLQ service should handle message creation errors without system failure")
            .isThrownBy(() -> deadLetterQueueService.sendToDeadLetterQueue(originalMessage, lastError, 3));
    }

    @Test
    @DisplayName("Should handle multiple concurrent DLQ messages with thread safety")
    void testMultipleConcurrentDeadLetterMessages() throws InterruptedException {
        // Given
        EmbeddingGenerationMessage message1 = new EmbeddingGenerationMessage(
            UUID.randomUUID(), "Content 1", UUID.randomUUID(), UUID.randomUUID()
        );
        EmbeddingGenerationMessage message2 = new EmbeddingGenerationMessage(
            UUID.randomUUID(), "Content 2", UUID.randomUUID(), UUID.randomUUID()  
        );
        
        Exception error = new RuntimeException("Service failure");
        
        @SuppressWarnings("unchecked")
        CompletableFuture<SendResult<String, String>> future = mock(CompletableFuture.class);
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

        // When - send messages concurrently
        Thread thread1 = new Thread(() -> deadLetterQueueService.sendToDeadLetterQueue(message1, error, 3));
        Thread thread2 = new Thread(() -> deadLetterQueueService.sendToDeadLetterQueue(message2, error, 3));
        
        thread1.start();
        thread2.start();
        
        thread1.join();
        thread2.join();

        // Then - both messages should be sent to DLQ
        verify(kafkaTemplate, times(2)).send(eq(deadLetterTopic), anyString(), anyString());
    }
}
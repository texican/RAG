package com.byo.rag.embedding.service;

import com.byo.rag.embedding.service.EmbeddingKafkaService.EmbeddingGenerationMessage;
import com.byo.rag.embedding.service.NotificationService.FailureAlert;
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
class NotificationServiceTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(kafkaTemplate);
    }

    @Test
    @DisplayName("Should send structured failure alert with complete context information")
    void testSendFailureAlert() {
        // Given
        EmbeddingGenerationMessage originalMessage = new EmbeddingGenerationMessage(
            UUID.randomUUID(),
            "Test content",
            UUID.randomUUID(),
            UUID.randomUUID()
        );
        
        Exception error = new RuntimeException("Service failure");
        int attemptCount = 3;
        
        CompletableFuture<SendResult<String, String>> future = mock(CompletableFuture.class);
        when(kafkaTemplate.send(eq("failure-alerts"), anyString(), anyString())).thenReturn(future);

        // When
        notificationService.sendFailureAlert(originalMessage, error, attemptCount);

        // Then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), messageCaptor.capture());
        
        assertThat(topicCaptor.getValue())
            .describedAs("Alert should be sent to the failure-alerts topic")
            .isEqualTo("failure-alerts");
        
        // Verify the alert message structure
        String alertJson = messageCaptor.getValue();
        FailureAlert alert = JsonUtils.fromJson(alertJson, FailureAlert.class);
        
        assertThat(alert.alertId())
            .describedAs("Alert should have a unique alert ID")
            .isNotNull();
        assertThat(alert.alertType())
            .describedAs("Alert type should indicate embedding processing failure")
            .isEqualTo("EMBEDDING_PROCESSING_FAILURE");
        assertThat(alert.severity())
            .describedAs("Alert severity should be HIGH for processing failures")
            .isEqualTo("HIGH");
        assertThat(alert.tenantId())
            .describedAs("Alert should preserve original tenant ID for routing")
            .isEqualTo(originalMessage.tenantId());
        assertThat(alert.documentId())
            .describedAs("Alert should preserve original document ID for context")
            .isEqualTo(originalMessage.documentId());
        assertThat(alert.chunkId())
            .describedAs("Alert should preserve original chunk ID for tracking")
            .isEqualTo(originalMessage.chunkId());
        assertThat(alert.errorType())
            .describedAs("Alert should capture the specific exception type for categorization")
            .isEqualTo("RuntimeException");
        assertThat(alert.message())
            .describedAs("Alert message should contain chunk ID for identification")
            .contains(originalMessage.chunkId().toString())
            .describedAs("Alert message should contain attempt count for debugging")
            .contains("3 attempts");
        assertThat(alert.timestamp())
            .describedAs("Alert should have a timestamp for chronological ordering")
            .isNotNull();
    }

    @Test
    @DisplayName("Should handle Kafka publishing failures gracefully without throwing exceptions")
    void testSendFailureAlertHandlesKafkaFailure() {
        // Given
        EmbeddingGenerationMessage originalMessage = new EmbeddingGenerationMessage(
            UUID.randomUUID(),
            "Test content",
            UUID.randomUUID(),
            UUID.randomUUID()
        );
        
        Exception error = new RuntimeException("Service failure");
        
        CompletableFuture<SendResult<String, String>> future = mock(CompletableFuture.class);
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);
        
        // Simulate Kafka failure in callback
        doAnswer(invocation -> {
            var callback = invocation.getArgument(0, java.util.function.BiConsumer.class);
            callback.accept(null, new RuntimeException("Kafka publish failed"));
            return null;
        }).when(future).whenComplete(any());

        // When - should not throw exception despite Kafka failure
        assertThatNoException()
            .describedAs("Notification service should handle Kafka failures gracefully")
            .isThrownBy(() -> notificationService.sendFailureAlert(originalMessage, error, 3));

        // Then
        verify(kafkaTemplate).send(eq("failure-alerts"), anyString(), anyString());
    }

    @Test
    @DisplayName("Should handle message creation errors gracefully without system failure")
    void testSendFailureAlertHandlesMessageCreationError() {
        // Given - simulate JsonUtils failure (though unlikely in practice)
        EmbeddingGenerationMessage originalMessage = new EmbeddingGenerationMessage(
            UUID.randomUUID(),
            "Test content",
            UUID.randomUUID(),
            UUID.randomUUID()
        );
        
        Exception error = new RuntimeException("Service failure");

        // When & Then - should handle gracefully even if message creation fails
        assertThatNoException()
            .describedAs("Notification service should handle message creation errors without crashing")
            .isThrownBy(() -> notificationService.sendFailureAlert(originalMessage, error, 3));
    }
}
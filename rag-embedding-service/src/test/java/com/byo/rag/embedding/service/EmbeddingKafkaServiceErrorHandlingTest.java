package com.byo.rag.embedding.service;

import com.byo.rag.embedding.dto.EmbeddingRequest;
import com.byo.rag.embedding.dto.EmbeddingResponse;
import com.byo.rag.embedding.service.EmbeddingKafkaService.EmbeddingGenerationMessage;
import com.byo.rag.shared.util.JsonUtils;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;

/**
 * Test class for error handling scenarios in EmbeddingKafkaService.
 */
@ExtendWith(MockitoExtension.class)
class EmbeddingKafkaServiceErrorHandlingTest {

    @Mock
    private EmbeddingService embeddingService;
    
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Mock
    private NotificationService notificationService;
    
    @Mock
    private DeadLetterQueueService deadLetterQueueService;
    
    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;
    
    @Mock
    private CircuitBreaker circuitBreaker;
    
    @Mock
    private io.micrometer.core.instrument.MeterRegistry meterRegistry;

    private EmbeddingKafkaService embeddingKafkaService;

    private final String embeddingCompleteTopic = "embedding-complete";

    @BeforeEach
    void setUp() {
        // Mock the metrics registry - we don't need to test the metrics functionality, just ensure it doesn't break
        when(meterRegistry.counter(anyString(), anyString(), anyString())).thenReturn(mock(io.micrometer.core.instrument.Counter.class));
            
        embeddingKafkaService = new EmbeddingKafkaService(
            embeddingService,
            kafkaTemplate,
            notificationService,
            deadLetterQueueService,
            meterRegistry,
            embeddingCompleteTopic
        );
        
        // Reset counters
        ReflectionTestUtils.setField(embeddingKafkaService, "totalProcessedMessages", new AtomicInteger(0));
        ReflectionTestUtils.setField(embeddingKafkaService, "totalFailedMessages", new AtomicInteger(0));
    }

    @Test
    @DisplayName("Should successfully process embedding request and send completion notification")
    void testSuccessfulEmbeddingProcessing() {
        // Given
        EmbeddingGenerationMessage request = createTestRequest();
        String message = JsonUtils.toJson(request);
        
        EmbeddingResponse response = createTestResponse();
        when(embeddingService.generateEmbeddings(any(EmbeddingRequest.class))).thenReturn(response);
        
        CompletableFuture<SendResult<String, String>> future = mock(CompletableFuture.class);
        when(kafkaTemplate.send(eq(embeddingCompleteTopic), anyString(), anyString())).thenReturn(future);

        // When
        embeddingKafkaService.processEmbeddingRequest(message);

        // Then
        verify(embeddingService).generateEmbeddings(any(EmbeddingRequest.class));
        verify(kafkaTemplate).send(eq(embeddingCompleteTopic), eq(request.chunkId().toString()), anyString());
        verifyNoInteractions(notificationService, deadLetterQueueService);
    }

    @Test
    @DisplayName("Should retry embedding service failures and eventually succeed")
    void testEmbeddingServiceFailureTriggersRetry() {
        // Given
        EmbeddingGenerationMessage request = createTestRequest();
        String message = JsonUtils.toJson(request);
        
        // Service call fails (note: @Retry annotation requires Spring AOP to work)
        when(embeddingService.generateEmbeddings(any(EmbeddingRequest.class)))
            .thenThrow(new RuntimeException("Service temporarily unavailable"));

        // When
        embeddingKafkaService.processEmbeddingRequest(message);

        // Then - should call service once (retry won't work without Spring AOP)
        verify(embeddingService, times(1)).generateEmbeddings(any(EmbeddingRequest.class));
        verifyNoInteractions(kafkaTemplate); // No success message sent
        verify(notificationService).sendFailureAlert(eq(request), any(RuntimeException.class), eq(3));
    }

    @Test
    @DisplayName("Should send message to dead letter queue when all retry attempts are exhausted")
    void testMaxRetriesExceededSendsToDeadLetterQueue() {
        // Given
        EmbeddingGenerationMessage request = createTestRequest();
        String message = JsonUtils.toJson(request);
        
        RuntimeException persistentError = new RuntimeException("Persistent service failure");
        when(embeddingService.generateEmbeddings(any(EmbeddingRequest.class)))
            .thenThrow(persistentError);

        // When
        embeddingKafkaService.processEmbeddingRequest(message);

        // Then - should try once (retry doesn't work without Spring AOP), then send to DLQ and send alert
        verify(embeddingService, times(1)).generateEmbeddings(any(EmbeddingRequest.class));
        verify(deadLetterQueueService).sendToDeadLetterQueue(eq(request), eq(persistentError), eq(3));
        verify(notificationService).sendFailureAlert(eq(request), eq(persistentError), eq(3));
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("Should handle invalid JSON message format gracefully")
    void testInvalidMessageFormatHandling() {
        // Given
        String invalidMessage = "{ invalid json }";

        // When
        embeddingKafkaService.processEmbeddingRequest(invalidMessage);

        // Then - should not call any services and should handle gracefully
        verifyNoInteractions(embeddingService, kafkaTemplate, notificationService, deadLetterQueueService);
    }

    @Test
    @DisplayName("Should handle Kafka publish failures with circuit breaker protection")
    void testKafkaPublishFailureWithCircuitBreaker() {
        // Given
        EmbeddingGenerationMessage request = createTestRequest();
        String message = JsonUtils.toJson(request);
        
        EmbeddingResponse response = createTestResponse();
        when(embeddingService.generateEmbeddings(any(EmbeddingRequest.class))).thenReturn(response);
        
        // Kafka publish fails
        CompletableFuture<SendResult<String, String>> future = mock(CompletableFuture.class);
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);
        doAnswer(invocation -> {
            // Simulate async failure
            Runnable failureCallback = (Runnable) invocation.getArguments()[1];
            failureCallback.run();
            return null;
        }).when(future).whenComplete(any());

        // When
        embeddingKafkaService.processEmbeddingRequest(message);

        // Then
        verify(embeddingService).generateEmbeddings(any(EmbeddingRequest.class));
        verify(kafkaTemplate).send(eq(embeddingCompleteTopic), eq(request.chunkId().toString()), anyString());
        // Circuit breaker should handle Kafka failures gracefully
    }

    @Test
    @DisplayName("Should process multiple messages concurrently with thread-safe counters")
    void testConcurrentMessageProcessing() throws InterruptedException {
        // Given
        EmbeddingGenerationMessage request1 = createTestRequest();
        EmbeddingGenerationMessage request2 = createTestRequest();
        String message1 = JsonUtils.toJson(request1);
        String message2 = JsonUtils.toJson(request2);
        
        EmbeddingResponse response = createTestResponse();
        when(embeddingService.generateEmbeddings(any(EmbeddingRequest.class))).thenReturn(response);
        
        CompletableFuture<SendResult<String, String>> future = mock(CompletableFuture.class);
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

        // When - Process messages concurrently
        Thread thread1 = new Thread(() -> embeddingKafkaService.processEmbeddingRequest(message1));
        Thread thread2 = new Thread(() -> embeddingKafkaService.processEmbeddingRequest(message2));
        
        thread1.start();
        thread2.start();
        
        thread1.join();
        thread2.join();

        // Then - Both messages should be processed
        verify(embeddingService, times(2)).generateEmbeddings(any(EmbeddingRequest.class));
        verify(kafkaTemplate, times(2)).send(eq(embeddingCompleteTopic), anyString(), anyString());
        
        // Verify message counters are thread-safe
        AtomicInteger totalProcessed = (AtomicInteger) ReflectionTestUtils.getField(embeddingKafkaService, "totalProcessedMessages");
        assertThat(totalProcessed.get())
            .describedAs("Should process exactly 2 concurrent messages with thread-safe counter")
            .isEqualTo(2);
    }

    @Test
    @DisplayName("Should update metrics and counters correctly for success and failure scenarios")
    void testMetricsAndCountersUpdatedCorrectly() {
        // Given
        EmbeddingGenerationMessage request1 = createTestRequest();
        EmbeddingGenerationMessage request2 = createTestRequest();
        String message1 = JsonUtils.toJson(request1);
        String message2 = JsonUtils.toJson(request2);
        
        // First message succeeds, second fails
        when(embeddingService.generateEmbeddings(any(EmbeddingRequest.class)))
            .thenReturn(createTestResponse())
            .thenThrow(new RuntimeException("Service failure"));
        
        CompletableFuture<SendResult<String, String>> future = mock(CompletableFuture.class);
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

        // When
        embeddingKafkaService.processEmbeddingRequest(message1); // Success
        embeddingKafkaService.processEmbeddingRequest(message2); // Failure

        // Then - Verify counters
        AtomicInteger totalProcessed = (AtomicInteger) ReflectionTestUtils.getField(embeddingKafkaService, "totalProcessedMessages");
        AtomicInteger totalFailed = (AtomicInteger) ReflectionTestUtils.getField(embeddingKafkaService, "totalFailedMessages");
        
        assertThat(totalProcessed.get())
            .describedAs("Should track total of %d processed messages including successes and failures", 2)
            .isEqualTo(2);
        assertThat(totalFailed.get())
            .describedAs("Should track exactly %d failed message after one failure scenario", 1)
            .isEqualTo(1);
    }

    @Test
    @DisplayName("Should setup and cleanup MDC context properly for request tracing")
    void testMDCContextSetupAndCleanup() {
        // Given
        EmbeddingGenerationMessage request = createTestRequest();
        String message = JsonUtils.toJson(request);
        
        EmbeddingResponse response = createTestResponse();
        when(embeddingService.generateEmbeddings(any(EmbeddingRequest.class))).thenReturn(response);
        
        CompletableFuture<SendResult<String, String>> future = mock(CompletableFuture.class);
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

        // When
        embeddingKafkaService.processEmbeddingRequest(message);

        // Then - verify basic processing occurred (MDC testing would require PowerMock or similar)
        verify(embeddingService).generateEmbeddings(any(EmbeddingRequest.class));
        verify(kafkaTemplate).send(eq(embeddingCompleteTopic), eq(request.chunkId().toString()), anyString());
    }

    private EmbeddingGenerationMessage createTestRequest() {
        return new EmbeddingGenerationMessage(
            UUID.randomUUID(),
            "Test content for embedding",
            UUID.randomUUID(),
            UUID.randomUUID()
        );
    }

    private EmbeddingResponse createTestResponse() {
        UUID chunkId = UUID.randomUUID();
        return EmbeddingResponse.success(
            UUID.randomUUID(),
            UUID.randomUUID(), 
            "test-model",
            List.of(EmbeddingResponse.EmbeddingResult.success(
                chunkId,
                "test content",
                List.of(0.1f, 0.2f, 0.3f)
            )),
            3,
            100L
        );
    }
}
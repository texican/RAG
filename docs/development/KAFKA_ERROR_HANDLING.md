# Kafka Error Handling Implementation

## Overview

The RAG Embedding Service now includes comprehensive error handling for Kafka message processing, addressing ERROR-001 requirements for system reliability. This implementation provides robust error recovery, monitoring, and alerting capabilities.

## Features Implemented

### 1. Retry Mechanism with Exponential Backoff
- **Configuration**: Resilience4j-based retry with exponential backoff
- **Max Attempts**: 3 retries per message
- **Backoff Strategy**: 1s initial delay, 2x multiplier, up to 10s max delay
- **Retryable Exceptions**: `RuntimeException`, `TransientAiException`
- **Non-Retryable**: `IllegalArgumentException`

### 2. Circuit Breaker Pattern
- **Embedding Service Circuit Breaker**:
  - Failure rate threshold: 50%
  - Minimum calls: 5 
  - Wait duration: 30s in open state
  - Half-open test calls: 3
  - Sliding window: 20 calls
  
- **Kafka Circuit Breaker**:
  - Failure rate threshold: 60%
  - Minimum calls: 3
  - Wait duration: 15s in open state
  - Half-open test calls: 2
  - Sliding window: 10 calls

### 3. Dead Letter Queue (DLQ)
- **Topic**: `embedding-dlq`
- **Purpose**: Handles messages that consistently fail after all retry attempts
- **Message Structure**: Includes original message, error details, attempt count, and failure timestamp

### 4. Failure Notification System
- **Topic**: `failure-alerts`
- **Triggers**: Sent when messages are moved to DLQ
- **Alert Structure**: Contains tenant info, error type, severity, and detailed context

### 5. Comprehensive Logging
- **MDC Context**: Each message gets trace ID, tenant ID, chunk ID for tracking
- **Log Levels**: 
  - INFO: Successful processing, retry attempts
  - WARN: DLQ messages, circuit breaker activations
  - ERROR: Processing failures with full context
- **Structured Logging**: JSON format with correlation IDs

### 6. Metrics and Monitoring
- **Success Counter**: `embedding.kafka.processing.success` - tracks successfully processed messages
- **Error Counter**: `embedding.kafka.processing.error` - tracks failed messages with error type tags
- **Circuit Breaker Health**: Exposed via Actuator endpoints  
- **Performance Tracking**: Processing time measurements
- **Error Classification**: Metrics tagged by error type for detailed analysis

## Configuration

### Application Properties
```yaml
# Resilience4j Configuration
resilience4j:
  circuitbreaker:
    instances:
      embeddingService:
        failure-rate-threshold: 50
        minimum-number-of-calls: 5
        wait-duration-in-open-state: 30s
        permitted-number-of-calls-in-half-open-state: 3
        sliding-window-size: 20
      kafka:
        failure-rate-threshold: 60
        minimum-number-of-calls: 3
        wait-duration-in-open-state: 15s
        sliding-window-size: 10
  retry:
    instances:
      embeddingGeneration:
        max-attempts: 3
        wait-duration: 1s
        exponential-backoff-multiplier: 2

# Kafka Topics
kafka:
  topics:
    embedding-generation: embedding-generation
    embedding-complete: embedding-complete
    dead-letter-queue: embedding-dlq
    failure-alerts: failure-alerts
```

## Error Handling Flow

### Success Path
1. Message received from `embedding-generation` topic
2. Parse and validate message
3. Generate embeddings via EmbeddingService
4. Send completion notification to `embedding-complete` topic
5. Log success with processing time

### Failure Path
1. Message processing fails
2. Retry mechanism activated with exponential backoff
3. If all retries exhausted:
   - Send message to DLQ (`embedding-dlq`)
   - Send failure alert (`failure-alerts`)
   - Log comprehensive error context
4. Circuit breaker may activate for repeated failures

### Circuit Breaker Activation
1. Service failure rate exceeds threshold
2. Circuit opens, requests fail fast
3. After wait duration, circuit moves to half-open
4. Test calls determine circuit state transition

## Monitoring and Alerting

### Health Endpoints
- `/actuator/health` - Overall service health including circuit breakers
- `/actuator/metrics` - Detailed metrics including retry and circuit breaker stats
- `/actuator/prometheus` - Prometheus-compatible metrics

### Key Metrics
- `resilience4j.circuitbreaker.calls` - Circuit breaker call statistics
- `resilience4j.retry.calls` - Retry attempt statistics
- `embedding.kafka.processing.success` - Successfully processed embedding messages
- `embedding.kafka.processing.error` - Failed message processing attempts (tagged by error type)
- Custom counters for thread-safe message tracking

### Alert Topics
Monitor these Kafka topics for operational issues:
- `failure-alerts` - Critical failures requiring attention
- `embedding-dlq` - Messages that couldn't be processed

## Service Classes

### EmbeddingKafkaService
- **Purpose**: Main Kafka listener with error handling
- **Annotations**: `@Retry`, `@CircuitBreaker`
- **Features**: MDC logging, fallback methods, metrics tracking

### NotificationService
- **Purpose**: Send failure alerts to administrators
- **Topic**: `failure-alerts`
- **Alert Structure**: Tenant info, error details, severity levels

### DeadLetterQueueService
- **Purpose**: Handle messages that consistently fail
- **Topic**: `embedding-dlq`
- **Message Structure**: Original message + failure metadata

### ErrorHandlingConfig
- **Purpose**: Spring configuration for retry and circuit breaker
- **Components**: RetryTemplate, CircuitBreakerCustomizers
- **Policies**: Exponential backoff, failure thresholds

## Testing

### Unit Tests
- `EmbeddingKafkaServiceErrorHandlingTest`: Core error handling scenarios
- `NotificationServiceTest`: Alert notification functionality  
- `DeadLetterQueueServiceTest`: DLQ message handling

### Test Coverage
- Retry mechanism validation
- Circuit breaker state transitions
- DLQ message structure verification
- Failure notification content validation
- Concurrent processing scenarios
- MDC context setup and cleanup

## Operational Considerations

### Performance Impact
- Minimal overhead for successful operations
- Retry delays only affect failed messages
- Circuit breaker provides fail-fast behavior
- Comprehensive logging may increase log volume

### Scaling
- Thread-safe counters support concurrent processing
- Circuit breaker state shared across service instances
- DLQ topics should be partitioned appropriately
- Alert topics may need rate limiting

### Troubleshooting

#### Common Issues
1. **High Retry Rate**: Check embedding service health, increase circuit breaker thresholds
2. **DLQ Buildup**: Monitor `embedding-dlq` topic, investigate recurring failure patterns
3. **Circuit Breaker Open**: Check service dependencies, review failure logs
4. **Missing Alerts**: Verify `failure-alerts` topic consumers are running

#### Log Analysis
- Search by trace ID for complete request flow
- Filter by tenant ID for tenant-specific issues  
- Monitor ERROR level logs for immediate attention
- Review circuit breaker state changes in logs

## Future Enhancements

### Potential Improvements
1. **Adaptive Retry**: Dynamic retry intervals based on error types
2. **Batch DLQ Processing**: Replay failed messages in batches
3. **Alert Routing**: Route alerts based on tenant or error type
4. **Metrics Dashboard**: Custom dashboards for error handling metrics
5. **Auto-Recovery**: Automatic retry of DLQ messages during low-load periods

### Integration Points
- External monitoring systems (Prometheus, Grafana)
- Alert management platforms (PagerDuty, Slack)
- Log aggregation systems (ELK Stack, Splunk)
- Message replay tools for DLQ processing

## Dependencies

### Required Dependencies
- `spring-retry`: Retry mechanism implementation
- `resilience4j-spring-boot3`: Circuit breaker and retry annotations
- `resilience4j-circuitbreaker`: Circuit breaker core functionality
- `spring-kafka`: Kafka integration and messaging
- `micrometer-registry-prometheus`: Metrics collection and export

### Version Information
- Spring Boot: 3.2.8
- Resilience4j: 2.0.2
- Spring Kafka: Managed by Spring Boot
- Micrometer: 1.12.7
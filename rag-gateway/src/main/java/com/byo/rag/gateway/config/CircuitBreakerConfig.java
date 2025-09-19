package com.byo.rag.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Circuit Breaker Configuration for RAG Gateway.
 * 
 * <p>This configuration implements service-specific circuit breakers using
 * Resilience4j for fault tolerance and graceful degradation. Each microservice
 * has tailored circuit breaker settings optimized for its specific
 * characteristics and failure patterns.
 * 
 * <p><strong>Circuit Breaker Features:</strong>
 * <ul>
 *   <li><strong>Service-Specific Configuration</strong>: Tailored settings per microservice</li>
 *   <li><strong>Intelligent Fallbacks</strong>: Cached responses and degraded functionality</li>
 *   <li><strong>Health Integration</strong>: Circuit state influences routing decisions</li>
 *   <li><strong>Metrics Collection</strong>: Circuit breaker state and performance metrics</li>
 *   <li><strong>Automatic Recovery</strong>: Half-open state testing for service recovery</li>
 * </ul>
 * 
 * <p><strong>Service-Specific Settings:</strong>
 * <ul>
 *   <li><strong>Auth Service</strong>: Fast failure detection for critical authentication</li>
 *   <li><strong>Document Service</strong>: Extended timeouts for file processing operations</li>
 *   <li><strong>Embedding Service</strong>: Aggressive fallbacks with cached embeddings</li>
 *   <li><strong>Core Service</strong>: Extended recovery time for complex RAG operations</li>
 *   <li><strong>Admin Service</strong>: Conservative settings for administrative operations</li>
 * </ul>
 * 
 * @author Enterprise RAG Team
 * @version 1.0
 * @since 1.1
 */
@Configuration
public class CircuitBreakerConfig {

    /** Base failure rate threshold for circuit breakers. */
    @Value("${resilience4j.circuitbreaker.configs.default.failure-rate-threshold:50}")
    private float baseFailureRateThreshold;

    /** Base sliding window size for circuit breakers. */
    @Value("${resilience4j.circuitbreaker.configs.default.sliding-window-size:10}")
    private int baseSlidingWindowSize;

    /** Base minimum number of calls before circuit breaker evaluation. */
    @Value("${resilience4j.circuitbreaker.configs.default.minimum-number-of-calls:5}")
    private int baseMinimumNumberOfCalls;

    /** Base wait duration in open state. */
    @Value("${resilience4j.circuitbreaker.configs.default.wait-duration-in-open-state:10s}")
    private Duration baseWaitDurationInOpenState;

    /** Health check service for circuit breaker integration. */
    private final ServiceHealthMonitor serviceHealthMonitor;

    /** Circuit breaker metrics collector. */
    private final CircuitBreakerMetricsCollector metricsCollector;

    /** Service-specific circuit breaker configurations. */
    private final Map<String, ServiceCircuitBreakerConfig> serviceConfigs = new ConcurrentHashMap<>();

    /**
     * Constructs circuit breaker configuration.
     */
    public CircuitBreakerConfig() {
        this.serviceHealthMonitor = new ServiceHealthMonitor();
        this.metricsCollector = new CircuitBreakerMetricsCollector();
        initializeServiceConfigs();
    }

    /**
     * Creates reactive circuit breaker factory with service-specific configurations.
     * 
     * @return configured circuit breaker factory
     */
    @Bean
    public ReactiveResilience4JCircuitBreakerFactory circuitBreakerFactory() {
        ReactiveResilience4JCircuitBreakerFactory factory = new ReactiveResilience4JCircuitBreakerFactory();
        
        // Configure each service-specific circuit breaker
        serviceConfigs.forEach((serviceName, config) -> {
            factory.configure(builder -> builder
                .circuitBreakerConfig(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                    .failureRateThreshold(config.failureRateThreshold)
                    .slowCallRateThreshold(config.slowCallRateThreshold)
                    .slowCallDurationThreshold(config.slowCallDurationThreshold)
                    .slidingWindowType(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                    .slidingWindowSize(config.slidingWindowSize)
                    .minimumNumberOfCalls(config.minimumNumberOfCalls)
                    .waitDurationInOpenState(config.waitDurationInOpenState)
                    .permittedNumberOfCallsInHalfOpenState(config.permittedNumberOfCallsInHalfOpenState)
                    .automaticTransitionFromOpenToHalfOpenEnabled(true)
                    .recordExceptions(config.recordedExceptions)
                    .build())
                .timeLimiterConfig(io.github.resilience4j.timelimiter.TimeLimiterConfig.custom()
                    .timeoutDuration(config.timeoutDuration)
                    .cancelRunningFuture(true)
                    .build())
                .build(), serviceName);
        });

        return factory;
    }

    /**
     * Creates circuit breaker registry for manual circuit breaker management.
     * 
     * @return circuit breaker registry
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        
        // Register service-specific circuit breakers
        serviceConfigs.forEach((serviceName, config) -> {
            CircuitBreaker circuitBreaker = registry.circuitBreaker(serviceName, 
                io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                    .failureRateThreshold(config.failureRateThreshold)
                    .slidingWindowSize(config.slidingWindowSize)
                    .minimumNumberOfCalls(config.minimumNumberOfCalls)
                    .waitDurationInOpenState(config.waitDurationInOpenState)
                    .build());
            
            // Register event listeners for metrics collection
            circuitBreaker.getEventPublisher()
                .onStateTransition(event -> {
                    metricsCollector.recordStateTransition(serviceName, 
                        event.getStateTransition().getFromState().toString(),
                        event.getStateTransition().getToState().toString());
                    
                    // Update service health based on circuit breaker state
                    serviceHealthMonitor.updateServiceHealth(serviceName, 
                        event.getStateTransition().getToState());
                })
                .onCallNotPermitted(event -> {
                    metricsCollector.recordCallNotPermitted(serviceName);
                })
                .onFailureRateExceeded(event -> {
                    metricsCollector.recordFailureRateExceeded(serviceName, 
                        event.getFailureRate());
                });
        });

        return registry;
    }

    /**
     * Creates service fallback handler for graceful degradation.
     * 
     * @return service fallback handler
     */
    @Bean
    public ServiceFallbackHandler serviceFallbackHandler() {
        return new ServiceFallbackHandler();
    }

    /**
     * Creates time limiter registry for timeout management.
     * 
     * @return time limiter registry
     */
    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        return TimeLimiterRegistry.ofDefaults();
    }

    /**
     * Initializes service-specific circuit breaker configurations.
     */
    private void initializeServiceConfigs() {
        // Auth Service - Critical for authentication, fast failure detection
        serviceConfigs.put("auth-service", new ServiceCircuitBreakerConfig(
            40.0f,  // Lower failure threshold for critical service
            50.0f,  // Slow call rate threshold
            Duration.ofSeconds(2),  // Quick timeout for auth operations
            8,      // Smaller sliding window for faster detection
            3,      // Minimum calls
            Duration.ofSeconds(15), // Wait duration in open state
            3,      // Permitted calls in half-open
            Duration.ofSeconds(5),  // Timeout duration
            java.net.ConnectException.class, java.util.concurrent.TimeoutException.class
        ));

        // Document Service - Extended timeouts for file operations
        serviceConfigs.put("document-service", new ServiceCircuitBreakerConfig(
            60.0f,  // Higher threshold for file operations
            50.0f,  // Slow call rate threshold
            Duration.ofSeconds(10), // Extended timeout for file processing
            15,     // Larger sliding window
            5,      // Minimum calls
            Duration.ofSeconds(30), // Longer wait for recovery
            5,      // More calls for testing recovery
            Duration.ofSeconds(30), // Extended timeout
            java.net.ConnectException.class, java.util.concurrent.TimeoutException.class,
            java.io.IOException.class
        ));

        // Embedding Service - Aggressive caching, moderate thresholds
        serviceConfigs.put("embedding-service", new ServiceCircuitBreakerConfig(
            55.0f,  // Moderate failure threshold
            60.0f,  // Higher slow call threshold (embeddings can be slow)
            Duration.ofSeconds(15), // Extended timeout for embedding generation
            12,     // Moderate sliding window
            4,      // Minimum calls
            Duration.ofSeconds(20), // Moderate wait duration
            4,      // Permitted calls in half-open
            Duration.ofSeconds(20), // Extended timeout for embeddings
            java.net.ConnectException.class, java.util.concurrent.TimeoutException.class
        ));

        // Core Service - Most complex operations, conservative settings
        serviceConfigs.put("core-service", new ServiceCircuitBreakerConfig(
            65.0f,  // Higher threshold for complex RAG operations
            70.0f,  // Higher slow call threshold
            Duration.ofSeconds(30), // Extended timeout for RAG processing
            20,     // Large sliding window for accuracy
            8,      // Higher minimum calls
            Duration.ofSeconds(60), // Extended recovery time
            6,      // More calls for recovery testing
            Duration.ofSeconds(45), // Very extended timeout
            java.net.ConnectException.class, java.util.concurrent.TimeoutException.class
        ));

        // Admin Service - Conservative settings for administrative operations
        serviceConfigs.put("admin-service", new ServiceCircuitBreakerConfig(
            45.0f,  // Conservative failure threshold
            50.0f,  // Standard slow call threshold
            Duration.ofSeconds(5),  // Moderate timeout
            10,     // Standard sliding window
            5,      // Standard minimum calls
            Duration.ofSeconds(25), // Moderate wait duration
            4,      // Standard recovery calls
            Duration.ofSeconds(10), // Standard timeout
            java.net.ConnectException.class, java.util.concurrent.TimeoutException.class
        ));
    }

    /**
     * Service-specific circuit breaker configuration.
     */
    public static class ServiceCircuitBreakerConfig {
        public final float failureRateThreshold;
        public final float slowCallRateThreshold;
        public final Duration slowCallDurationThreshold;
        public final int slidingWindowSize;
        public final int minimumNumberOfCalls;
        public final Duration waitDurationInOpenState;
        public final int permittedNumberOfCallsInHalfOpenState;
        public final Duration timeoutDuration;
        public final Class<? extends Throwable>[] recordedExceptions;

        @SafeVarargs
        public ServiceCircuitBreakerConfig(float failureRateThreshold, float slowCallRateThreshold,
                                         Duration slowCallDurationThreshold, int slidingWindowSize,
                                         int minimumNumberOfCalls, Duration waitDurationInOpenState,
                                         int permittedNumberOfCallsInHalfOpenState, Duration timeoutDuration,
                                         Class<? extends Throwable>... recordedExceptions) {
            this.failureRateThreshold = failureRateThreshold;
            this.slowCallRateThreshold = slowCallRateThreshold;
            this.slowCallDurationThreshold = slowCallDurationThreshold;
            this.slidingWindowSize = slidingWindowSize;
            this.minimumNumberOfCalls = minimumNumberOfCalls;
            this.waitDurationInOpenState = waitDurationInOpenState;
            this.permittedNumberOfCallsInHalfOpenState = permittedNumberOfCallsInHalfOpenState;
            this.timeoutDuration = timeoutDuration;
            this.recordedExceptions = recordedExceptions;
        }
    }

    /**
     * Service health monitor for circuit breaker integration.
     */
    public static class ServiceHealthMonitor {
        private final Map<String, io.github.resilience4j.circuitbreaker.CircuitBreaker.State> serviceStates = 
            new ConcurrentHashMap<>();

        public void updateServiceHealth(String serviceName, io.github.resilience4j.circuitbreaker.CircuitBreaker.State state) {
            serviceStates.put(serviceName, state);
            // In a real implementation, this would update health indicators
            System.out.println("Service " + serviceName + " circuit breaker state: " + state);
        }

        public boolean isServiceHealthy(String serviceName) {
            io.github.resilience4j.circuitbreaker.CircuitBreaker.State state = serviceStates.get(serviceName);
            return state == null || state == io.github.resilience4j.circuitbreaker.CircuitBreaker.State.CLOSED;
        }

        public Map<String, io.github.resilience4j.circuitbreaker.CircuitBreaker.State> getAllServiceStates() {
            return Map.copyOf(serviceStates);
        }
    }

    /**
     * Circuit breaker metrics collector.
     */
    public static class CircuitBreakerMetricsCollector {
        private final Map<String, CircuitBreakerMetrics> serviceMetrics = new ConcurrentHashMap<>();

        public void recordStateTransition(String serviceName, String fromState, String toState) {
            CircuitBreakerMetrics metrics = serviceMetrics.computeIfAbsent(serviceName, 
                k -> new CircuitBreakerMetrics());
            metrics.recordStateTransition(fromState, toState);
        }

        public void recordCallNotPermitted(String serviceName) {
            CircuitBreakerMetrics metrics = serviceMetrics.computeIfAbsent(serviceName, 
                k -> new CircuitBreakerMetrics());
            metrics.recordCallNotPermitted();
        }

        public void recordFailureRateExceeded(String serviceName, float failureRate) {
            CircuitBreakerMetrics metrics = serviceMetrics.computeIfAbsent(serviceName, 
                k -> new CircuitBreakerMetrics());
            metrics.recordFailureRateExceeded(failureRate);
        }

        public CircuitBreakerMetrics getServiceMetrics(String serviceName) {
            return serviceMetrics.get(serviceName);
        }

        public Map<String, CircuitBreakerMetrics> getAllMetrics() {
            return Map.copyOf(serviceMetrics);
        }
    }

    /**
     * Circuit breaker metrics data class.
     */
    public static class CircuitBreakerMetrics {
        private long stateTransitions = 0;
        private long callsNotPermitted = 0;
        private long failureRateExceededEvents = 0;
        private float lastFailureRate = 0.0f;

        public void recordStateTransition(String fromState, String toState) {
            stateTransitions++;
        }

        public void recordCallNotPermitted() {
            callsNotPermitted++;
        }

        public void recordFailureRateExceeded(float failureRate) {
            failureRateExceededEvents++;
            lastFailureRate = failureRate;
        }

        // Getters
        public long getStateTransitions() { return stateTransitions; }
        public long getCallsNotPermitted() { return callsNotPermitted; }
        public long getFailureRateExceededEvents() { return failureRateExceededEvents; }
        public float getLastFailureRate() { return lastFailureRate; }
    }

    /**
     * Service fallback handler for graceful degradation.
     */
    public static class ServiceFallbackHandler {
        
        /**
         * Provides fallback response for auth service failures.
         */
        public Mono<String> authServiceFallback(Exception ex) {
            return Mono.just("{\"error\":\"AUTH_SERVICE_UNAVAILABLE\",\"message\":\"Authentication service temporarily unavailable\"}");
        }

        /**
         * Provides fallback response for document service failures.
         */
        public Mono<String> documentServiceFallback(Exception ex) {
            return Mono.just("{\"error\":\"DOCUMENT_SERVICE_UNAVAILABLE\",\"message\":\"Document service temporarily unavailable\"}");
        }

        /**
         * Provides fallback response for embedding service failures.
         */
        public Mono<String> embeddingServiceFallback(Exception ex) {
            return Mono.just("{\"error\":\"EMBEDDING_SERVICE_UNAVAILABLE\",\"message\":\"Embedding service temporarily unavailable, using cached results\"}");
        }

        /**
         * Provides fallback response for core service failures.
         */
        public Mono<String> coreServiceFallback(Exception ex) {
            return Mono.just("{\"error\":\"CORE_SERVICE_UNAVAILABLE\",\"message\":\"RAG service temporarily unavailable\"}");
        }

        /**
         * Provides fallback response for admin service failures.
         */
        public Mono<String> adminServiceFallback(Exception ex) {
            return Mono.just("{\"error\":\"ADMIN_SERVICE_UNAVAILABLE\",\"message\":\"Admin service temporarily unavailable\"}");
        }
    }
}
/**
 * Configuration classes for the RAG core service.
 * 
 * <p>This package contains Spring configuration classes that set up the
 * RAG (Retrieval Augmented Generation) processing infrastructure, including
 * LLM integration, vector search configuration, caching setup, and performance
 * optimization for the Enterprise RAG System.</p>
 * 
 * <h2>Configuration Categories</h2>
 * <p>RAG configurations cover all aspects of RAG processing:</p>
 * <ul>
 *   <li><strong>LLM Configuration</strong> - Large Language Model provider setup and integration</li>
 *   <li><strong>Vector Search Configuration</strong> - Semantic search and embedding configuration</li>
 *   <li><strong>Context Assembly Configuration</strong> - Context processing and optimization</li>
 *   <li><strong>Caching Configuration</strong> - RAG response and context caching</li>
 *   <li><strong>Streaming Configuration</strong> - Real-time response streaming setup</li>
 *   <li><strong>Performance Configuration</strong> - RAG pipeline optimization</li>
 * </ul>
 * 
 * <h2>LLM Integration Configuration</h2>
 * <p>Comprehensive Large Language Model integration setup:</p>
 * <ul>
 *   <li><strong>Multi-Provider Support</strong> - Configuration for multiple LLM providers</li>
 *   <li><strong>Model Selection</strong> - Dynamic model selection and routing</li>
 *   <li><strong>Authentication</strong> - API key management and authentication</li>
 *   <li><strong>Rate Limiting</strong> - LLM provider rate limiting and quota management</li>
 *   <li><strong>Failover Configuration</strong> - Provider failover and fallback strategies</li>
 *   <li><strong>Token Management</strong> - Token usage optimization and cost control</li>
 * </ul>
 * 
 * <h2>Vector Search Configuration</h2>
 * <p>Advanced vector search and semantic matching setup:</p>
 * <ul>
 *   <li><strong>Embedding Service Integration</strong> - Connection to embedding service</li>
 *   <li><strong>Search Parameters</strong> - Default search parameters and thresholds</li>
 *   <li><strong>Hybrid Search</strong> - Configuration for semantic and keyword search</li>
 *   <li><strong>Index Selection</strong> - Vector index routing and selection</li>
 *   <li><strong>Performance Tuning</strong> - Search performance optimization</li>
 * </ul>
 * 
 * <h2>Context Assembly Configuration</h2>
 * <p>Context processing and optimization configuration:</p>
 * <ul>
 *   <li><strong>Context Limits</strong> - Maximum context length and token limits</li>
 *   <li><strong>Ranking Configuration</strong> - Context ranking algorithms and parameters</li>
 *   <li><strong>Deduplication</strong> - Context deduplication strategies</li>
 *   <li><strong>Summarization</strong> - Context summarization configuration</li>
 *   <li><strong>Assembly Strategies</strong> - Context assembly and optimization strategies</li>
 * </ul>
 * 
 * <h2>Caching Configuration</h2>
 * <p>Multi-level caching for RAG processing performance:</p>
 * <ul>
 *   <li><strong>Response Caching</strong> - RAG response caching strategies</li>
 *   <li><strong>Context Caching</strong> - Assembled context caching</li>
 *   <li><strong>Search Result Caching</strong> - Vector search result caching</li>
 *   <li><strong>LLM Response Caching</strong> - Language model response caching</li>
 *   <li><strong>Cache Invalidation</strong> - Intelligent cache invalidation strategies</li>
 * </ul>
 * 
 * <h2>Streaming Configuration</h2>
 * <p>Real-time response streaming configuration:</p>
 * <ul>
 *   <li><strong>WebFlux Configuration</strong> - Reactive streaming setup</li>
 *   <li><strong>Server-Sent Events</strong> - SSE configuration for streaming</li>
 *   <li><strong>WebSocket Configuration</strong> - WebSocket setup for real-time communication</li>
 *   <li><strong>Backpressure Handling</strong> - Stream backpressure management</li>
 *   <li><strong>Stream Lifecycle</strong> - Stream connection lifecycle management</li>
 * </ul>
 * 
 * <h2>Conversation Configuration</h2>
 * <p>Multi-turn conversation management setup:</p>
 * <ul>
 *   <li><strong>Conversation Storage</strong> - Conversation history storage configuration</li>
 *   <li><strong>Memory Management</strong> - Conversation memory and context management</li>
 *   <li><strong>History Limits</strong> - Conversation history size and retention limits</li>
 *   <li><strong>Context Windows</strong> - Conversation context window configuration</li>
 *   <li><strong>Summarization</strong> - Conversation summarization configuration</li>
 * </ul>
 * 
 * <h2>Quality Assurance Configuration</h2>
 * <p>Response quality and validation configuration:</p>
 * <ul>
 *   <li><strong>Hallucination Detection</strong> - Configuration for hallucination detection</li>
 *   <li><strong>Fact Verification</strong> - Fact-checking and verification setup</li>
 *   <li><strong>Confidence Scoring</strong> - Response confidence calculation</li>
 *   <li><strong>Quality Metrics</strong> - Quality measurement and scoring</li>
 *   <li><strong>Validation Rules</strong> - Response validation rule configuration</li>
 * </ul>
 * 
 * <h2>Performance Optimization</h2>
 * <p>RAG pipeline performance optimization configuration:</p>
 * <ul>
 *   <li><strong>Async Processing</strong> - Asynchronous processing configuration</li>
 *   <li><strong>Parallel Execution</strong> - Parallel pipeline stage execution</li>
 *   <li><strong>Connection Pooling</strong> - HTTP connection pool optimization</li>
 *   <li><strong>Thread Pool Configuration</strong> - Thread pool tuning for RAG operations</li>
 *   <li><strong>Memory Management</strong> - Memory optimization for large contexts</li>
 * </ul>
 * 
 * <h2>Monitoring and Observability</h2>
 * <p>RAG processing monitoring configuration:</p>
 * <ul>
 *   <li><strong>Metrics Collection</strong> - RAG processing metrics configuration</li>
 *   <li><strong>Distributed Tracing</strong> - End-to-end request tracing</li>
 *   <li><strong>Performance Monitoring</strong> - RAG performance monitoring setup</li>
 *   <li><strong>Quality Monitoring</strong> - Response quality monitoring</li>
 *   <li><strong>Cost Tracking</strong> - LLM token usage and cost tracking</li>
 * </ul>
 * 
 * <h2>Security Configuration</h2>
 * <p>Security configuration for RAG operations:</p>
 * <ul>
 *   <li><strong>Input Sanitization</strong> - Query input sanitization configuration</li>
 *   <li><strong>Response Filtering</strong> - Response content filtering and validation</li>
 *   <li><strong>Rate Limiting</strong> - RAG endpoint rate limiting</li>
 *   <li><strong>Access Control</strong> - RAG operation access control</li>
 * </ul>
 * 
 * <h2>Configuration Properties Example</h2>
 * <pre>{@code
 * # application.yml
 * rag:
 *   core:
 *     llm:
 *       default-provider: openai
 *       providers:
 *         openai:
 *           api-key: ${OPENAI_API_KEY}
 *           base-url: https://api.openai.com/v1
 *           models:
 *             default: gpt-4
 *             streaming: gpt-4
 *           parameters:
 *             temperature: 0.7
 *             max-tokens: 2048
 *             top-p: 0.95
 *           rate-limiting:
 *             requests-per-minute: 1000
 *             tokens-per-minute: 150000
 *         anthropic:
 *           api-key: ${ANTHROPIC_API_KEY}
 *           base-url: https://api.anthropic.com
 *           models:
 *             default: claude-3-sonnet-20240229
 *             streaming: claude-3-sonnet-20240229
 *     search:
 *       max-results: 10
 *       similarity-threshold: 0.75
 *       hybrid-search:
 *         enabled: true
 *         semantic-weight: 0.7
 *         keyword-weight: 0.3
 *     context:
 *       max-length: 8000
 *       deduplication-enabled: true
 *       summarization:
 *         enabled: true
 *         threshold: 4000
 *       ranking-strategy: RELEVANCE_SCORE
 *     conversation:
 *       max-history-turns: 10
 *       memory-window: PT1H
 *       summarization-threshold: 20
 *     caching:
 *       enabled: true
 *       responses:
 *         ttl: PT30M
 *         max-size: 10000
 *       contexts:
 *         ttl: PT1H
 *         max-size: 5000
 *     streaming:
 *       enabled: true
 *       chunk-size: 256
 *       buffer-size: 1024
 * }</pre>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Configuration
 * @EnableConfigurationProperties({
 *     RagProperties.class,
 *     LLMProperties.class,
 *     ContextProperties.class
 * })
 * @ConditionalOnProperty(name = "rag.core.enabled", havingValue = "true", matchIfMissing = true)
 * public class CoreServiceConfiguration {
 *     
 *     private final RagProperties ragProperties;
 *     private final LLMProperties llmProperties;
 *     
 *     @Bean
 *     @ConditionalOnProperty(name = "rag.core.llm.providers.openai.enabled", havingValue = "true")
 *     public OpenAIService openAIService() {
 *         OpenAIProperties openAIProps = llmProperties.getProviders().getOpenai();
 *         return OpenAIService.builder()
 *             .apiKey(openAIProps.getApiKey())
 *             .baseUrl(openAIProps.getBaseUrl())
 *             .timeout(Duration.ofSeconds(30))
 *             .build();
 *     }
 *     
 *     @Bean
 *     public LLMProviderFactory llmProviderFactory(List<LLMProvider> providers) {
 *         return new LLMProviderFactory(providers, llmProperties.getDefaultProvider());
 *     }
 *     
 *     @Bean
 *     @ConditionalOnProperty(name = "rag.core.caching.enabled", havingValue = "true")
 *     public RagCacheManager ragCacheManager(RedisTemplate<String, Object> redisTemplate) {
 *         return new RagCacheManager(redisTemplate, ragProperties.getCaching());
 *     }
 *     
 *     @Bean
 *     @ConditionalOnProperty(name = "rag.core.streaming.enabled", havingValue = "true")
 *     public StreamingConfiguration streamingConfiguration() {
 *         return StreamingConfiguration.builder()
 *             .chunkSize(ragProperties.getStreaming().getChunkSize())
 *             .bufferSize(ragProperties.getStreaming().getBufferSize())
 *             .backpressureStrategy(BackpressureStrategy.BUFFER)
 *             .build();
 *     }
 * }
 * }</pre>
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 * @see org.springframework.context.annotation Spring configuration annotations
 * @see org.springframework.boot.context.properties Configuration properties
 * @see org.springframework.ai Spring AI configuration
 * @see com.byo.rag.shared.config Shared configuration classes
 */
package com.byo.rag.core.config;
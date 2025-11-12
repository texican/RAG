---
version: 1.0.0
last-updated: 2025-11-12
status: active
applies-to: 0.8.0-SNAPSHOT
category: development
---

# Error Handling Guidelines

## Overview

This document outlines the standardized error handling approach for the RAG Core Service, based on improvements implemented across QueryOptimizationService, ConversationService, and LLMIntegrationService.

## Core Principles

### 1. Consistent Exception Strategy
- **Use RagException consistently**: All service methods should throw `RagException` for business logic errors instead of returning null, false, or mixed return types
- **Preserve original exceptions**: Always include the original exception as the cause when wrapping exceptions
- **Meaningful error messages**: Include context about what operation failed and why

### 2. Defensive Programming First
- **Input validation is mandatory**: Always validate inputs before processing
- **Null safety**: Check for null values early and handle gracefully
- **Fail fast principle**: Validate preconditions at method entry
- **Graceful degradation**: Provide fallback behavior when possible

### 3. Error Message Format
```java
// Good: Descriptive with context
throw new RagException("Failed to retrieve conversation: " + conversationId, e);

// Bad: Generic without context  
throw new RagException("Operation failed", e);
```

### 4. Logging Strategy
```java
try {
    // Business logic
} catch (Exception e) {
    logger.error("Failed to [operation] for [entity]: [identifier]", identifier, e);
    throw new RagException("Failed to [operation]: " + identifier, e);
}
```

## Implementation Patterns

### Service Method Error Handling

#### Before (Inconsistent)
```java
public Conversation getConversation(String conversationId) {
    try {
        // logic
        return conversation;
    } catch (Exception e) {
        logger.error("Error retrieving conversation", e);
        return null; // ❌ Inconsistent return type
    }
}

public boolean deleteConversation(String conversationId) {
    try {
        // logic  
        return redisTemplate.delete(key);
    } catch (Exception e) {
        logger.error("Error deleting conversation", e);
        return false; // ❌ Hides the actual error
    }
}
```

#### After (Consistent)
```java
public Conversation getConversation(String conversationId) {
    try {
        // logic
        return conversation;
    } catch (Exception e) {
        logger.error("Failed to retrieve conversation: {}", conversationId, e);
        throw new RagException("Failed to retrieve conversation: " + conversationId, e);
    }
}

public boolean deleteConversation(String conversationId) {
    try {
        // logic
        return redisTemplate.delete(key);
    } catch (Exception e) {
        logger.error("Failed to delete conversation: {}", conversationId, e);
        throw new RagException("Failed to delete conversation: " + conversationId, e);
    }
}
```

## Defensive Programming Best Practices

Based on our implementation experience across QueryOptimizationService, ConversationService, and LLMIntegrationService, here are the essential defensive programming patterns:

### 1. Input Validation Patterns

#### Basic Input Validation
```java
public void processRequest(String input) {
    // Fail fast with clear validation
    if (input == null || input.trim().isEmpty()) {
        return; // or throw IllegalArgumentException for strict validation
    }
    
    try {
        // Business logic
    } catch (Exception e) {
        logger.error("Failed to process request: {}", input, e);
        throw new RagException("Failed to process request: " + input, e);
    }
}
```

#### Collection Input Validation
```java
public void processDocuments(List<SourceDocument> documents) {
    // Handle null collections defensively
    if (documents == null || documents.isEmpty()) {
        logger.debug("No documents provided for processing");
        return; // Early return with graceful degradation
    }
    
    // Validate collection contents
    List<SourceDocument> validDocuments = documents.stream()
        .filter(Objects::nonNull) // Remove null elements
        .filter(doc -> doc.relevanceScore() >= 0.0) // Business rule validation
        .collect(Collectors.toList());
        
    if (validDocuments.isEmpty()) {
        logger.warn("No valid documents found after filtering");
        return;
    }
    
    // Continue with validated data...
}
```

#### Configuration Parameter Validation
```java
public void configureService(ContextConfig config) {
    // Validate configuration parameters early
    validateConfig(config);
    
    try {
        // Use validated configuration
    } catch (Exception e) {
        logger.error("Service configuration failed", e);
        throw new RagException("Invalid service configuration", e);
    }
}

private void validateConfig(ContextConfig config) {
    if (config == null) {
        throw new IllegalArgumentException("Configuration cannot be null");
    }
    if (config.maxTokens() <= 0) {
        throw new IllegalArgumentException("Max tokens must be positive: " + config.maxTokens());
    }
    if (config.relevanceThreshold() < 0.0 || config.relevanceThreshold() > 1.0) {
        throw new IllegalArgumentException("Relevance threshold must be between 0.0 and 1.0: " + 
                                         config.relevanceThreshold());
    }
}
```

### 2. Resource Safety Patterns

#### Safe Resource Access
```java
public String retrieveData(String key) {
    if (key == null || key.trim().isEmpty()) {
        logger.debug("Empty key provided for data retrieval");
        return null; // or appropriate default
    }
    
    try {
        Object data = redisTemplate.opsForValue().get(formatKey(key));
        return data != null ? data.toString() : null;
    } catch (Exception e) {
        logger.error("Failed to retrieve data for key: {}", key, e);
        throw new RagException("Data retrieval failed for key: " + key, e);
    }
}

private String formatKey(String key) {
    // Defensive key formatting to prevent injection
    return "data:" + key.replaceAll("[^a-zA-Z0-9_-]", "_");
}
```

#### Thread-Safe Operations
```java
// Based on ContextAssemblyService thread safety fix
public String assembleContextSafely(List<SourceDocument> documents, 
                                   RagQueryRequest request, ContextConfig config) {
    // Validate inputs first
    if (documents == null || request == null || config == null) {
        throw new IllegalArgumentException("Required parameters cannot be null");
    }
    
    try {
        // Use configuration parameters directly instead of modifying instance variables
        return assembleContextWithConfig(documents, request, config);
    } catch (Exception e) {
        logger.error("Failed to assemble context with custom config for tenant: {}", 
                    request.tenantId(), e);
        throw new RagException("Context assembly failed with custom configuration", e);
    }
}
```

### 3. Data Processing Safety

#### Safe String Operations (QueryOptimizationService Pattern)
```java
public List<String> extractKeyTermsSafely(String query) {
    if (query == null || query.trim().isEmpty()) {
        return Collections.emptyList(); // Safe empty result
    }
    
    try {
        return Arrays.stream(query.toLowerCase().split("\\s+"))
            .filter(word -> word != null && word.length() > 2) // Null safety
            .filter(word -> !STOP_WORDS.contains(word))
            .filter(word -> word.matches("[a-zA-Z]+")) // Safe regex
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    } catch (Exception e) {
        logger.error("Failed to extract key terms from query: {}", query, e);
        return Collections.emptyList(); // Graceful degradation
    }
}
```

#### Safe Numeric Operations
```java
public int estimateTokenCountSafely(String text) {
    try {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        int length = text.length();
        if (length == 0) return 0;
        
        // Safe division with minimum bound
        return Math.max(1, length / 4);
    } catch (Exception e) {
        logger.warn("Failed to estimate token count, using fallback", e);
        return text != null ? Math.max(1, text.length() / 10) : 0; // Conservative fallback
    }
}
```

### 4. Collection Safety Patterns

#### Safe Stream Operations
```java
public List<SourceDocument> filterDocumentsSafely(List<SourceDocument> documents, 
                                                 double threshold) {
    if (documents == null) {
        return Collections.emptyList();
    }
    
    try {
        return documents.stream()
            .filter(Objects::nonNull) // Null safety
            .filter(doc -> {
                try {
                    return doc.relevanceScore() >= threshold;
                } catch (Exception e) {
                    logger.debug("Error checking relevance score, excluding document", e);
                    return false; // Exclude problematic documents
                }
            })
            .collect(Collectors.toList());
    } catch (Exception e) {
        logger.error("Failed to filter documents", e);
        return Collections.emptyList(); // Graceful degradation
    }
}
```

### 5. Error Recovery Patterns

#### Retry with Fallback (LLMIntegrationService Pattern)
```java
public String generateResponseSafely(String query, String context, RagQueryRequest request) {
    // Input validation first
    if (query == null || query.trim().isEmpty()) {
        throw new IllegalArgumentException("Query cannot be null or empty");
    }
    
    try {
        return generateResponse(query, context, request, defaultProvider);
    } catch (Exception e) {
        logger.error("Primary provider failed for tenant: {}", request.tenantId(), e);
        
        // Fallback with different provider
        if (fallbackProvider != null && !defaultProvider.equals(fallbackProvider)) {
            try {
                logger.info("Attempting fallback provider: {} for tenant: {}", 
                           fallbackProvider, request.tenantId());
                return generateResponse(query, context, request, fallbackProvider);
            } catch (Exception fallbackError) {
                logger.error("Fallback provider also failed for tenant: {}", 
                           request.tenantId(), fallbackError);
                throw new RagException("Both primary and fallback providers failed", fallbackError);
            }
        }
        
        throw new RagException("LLM response generation failed: " + e.getMessage(), e);
    }
}
```

### 6. Initialization Safety

#### Safe Static Initialization (QueryOptimizationService Pattern)  
```java
// Avoid Set.of() limitations with large collections
private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
    // Large list of stopwords - avoids Set.of() element limit
    "a", "an", "and", "are", "as", "at", "be", "by", "for", "from", "in", "is", "it",
    // ... many more words
    "well", "while"
));

// Safe map initialization for limited collections
private static final Map<String, String> ACRONYM_EXPANSIONS = Map.of(
    "AI", "artificial intelligence",
    "ML", "machine learning",
    "API", "application programming interface",
    // Limited to Map.of() constraints
);
```

### 7. Business Logic Safety

#### Provider Availability Checking
```java
public boolean isProviderAvailableSafely(String provider) {
    if (provider == null || provider.trim().isEmpty()) {
        logger.debug("Invalid provider identifier provided");
        return false;
    }
    
    try {
        // Minimal test to avoid expensive operations
        String testPrompt = "Test";
        
        ChatResponse response = chatClient.prompt()
            .user(testPrompt)
            .call()
            .chatResponse();
            
        boolean available = response != null && 
                           response.getResult() != null && 
                           response.getResult().getOutput() != null;
                           
        if (available) {
            logger.debug("Provider {} is available", provider);
        }
        
        return available;
        
    } catch (Exception e) {
        logger.warn("Provider {} is not available: {}", provider, e.getMessage());
        return false; // Safe default
    }
}
```

### Resource Access Errors

#### Database/Cache Operations
```java
public Object getData(String key) {
    try {
        Object data = redisTemplate.opsForValue().get(key);
        return data;
    } catch (Exception e) {
        logger.error("Failed to retrieve data for key: {}", key, e);
        throw new RagException("Failed to retrieve data: " + key, e);
    }
}
```

#### External Service Integration
```java
public String callExternalService(String request) {
    try {
        return externalClient.call(request);
    } catch (Exception e) {
        logger.error("External service call failed: {}", request, e);
        
        // Try fallback if available
        if (fallbackService != null) {
            try {
                return fallbackService.call(request);
            } catch (Exception fallbackError) {
                logger.error("Fallback service also failed: {}", request, fallbackError);
                throw new RagException("Both primary and fallback services failed", fallbackError);
            }
        }
        
        throw new RagException("External service call failed: " + e.getMessage(), e);
    }
}
```

## Exception Hierarchy

### RagException Usage
```java
// Business logic errors
throw new RagException("Business operation failed", cause);

// Configuration errors  
throw new RagException("Invalid configuration: " + configValue, cause);

// Integration errors
throw new RagException("External service integration failed", cause);

// Data validation errors
throw new RagException("Invalid data format: " + data, cause);
```

### When NOT to use RagException
- Parameter validation → Use `IllegalArgumentException`
- Programming errors → Use `IllegalStateException` 
- Framework/infrastructure issues → Let original exceptions bubble up with proper logging

## Testing Error Handling

### Test Exception Scenarios
```java
@Test
@DisplayName("Should handle Redis errors during conversation retrieval")
void getConversation_RedisError_ThrowsRagException() {
    // Arrange
    when(valueOperations.get(anyString()))
        .thenThrow(new RuntimeException("Redis connection failed"));
    
    // Act & Assert
    RagException exception = assertThrows(RagException.class, () ->
        conversationService.getConversation(testConversationId)
    );
    
    // Verify error message and cause
    assertNotNull(exception.getMessage());
    assertTrue(exception.getMessage().contains("Failed to retrieve conversation"));
    assertNotNull(exception.getCause());
    assertTrue(exception.getCause().getMessage().contains("Redis connection failed"));
}
```

## Monitoring Integration

### Structured Logging
```java
// Include relevant context in logs
logger.error("Operation failed for tenant: {} with provider: {}", 
           tenantId, provider, exception);

// Use consistent log levels
logger.error() // For exceptions that will be thrown
logger.warn()  // For recoverable issues  
logger.debug() // For detailed troubleshooting
```

### Metrics Integration
```java
try {
    // Business logic
    successCounter.increment();
} catch (Exception e) {
    errorCounter.increment(Tags.of("error_type", e.getClass().getSimpleName()));
    throw new RagException("Operation failed", e);
}
```

## Defensive Programming Principles Summary

### The "Defense in Depth" Approach
1. **Input Validation**: Validate all inputs at method boundaries
2. **Null Safety**: Assume any reference could be null until proven otherwise  
3. **Early Returns**: Exit early for invalid states rather than continuing
4. **Graceful Degradation**: Provide sensible defaults when possible
5. **Resource Safety**: Always clean up resources and handle resource failures
6. **Thread Safety**: Avoid shared mutable state or protect it properly
7. **Error Recovery**: Implement fallback mechanisms for critical operations

### Common Defensive Patterns from Our Implementation

#### Pattern: Null-Safe Collections
```java
// Always check for null collections and provide empty defaults
List<String> items = getItems(); // might return null
List<String> safeItems = items != null ? items : Collections.emptyList();
```

#### Pattern: Safe Stream Processing  
```java
// Filter null elements and handle exceptions within streams
return documents.stream()
    .filter(Objects::nonNull)
    .filter(doc -> {
        try {
            return isValid(doc);
        } catch (Exception e) {
            logger.debug("Invalid document excluded", e);
            return false; // Exclude problematic items
        }
    })
    .collect(Collectors.toList());
```

#### Pattern: Configuration Validation
```java
// Validate configuration early and fail fast
private void validateConfig(Config config) {
    Objects.requireNonNull(config, "Configuration cannot be null");
    if (config.timeout() <= 0) {
        throw new IllegalArgumentException("Timeout must be positive: " + config.timeout());
    }
}
```

#### Pattern: Safe String Operations
```java
// Handle potential null strings and edge cases
public String processText(String input) {
    if (input == null || input.trim().isEmpty()) {
        return ""; // Safe default
    }
    // Continue with validated input...
}
```

## Comprehensive Service Method Checklist

✅ **Defensive Programming**
- [ ] All inputs validated at method entry (null checks, range validation)
- [ ] Collections checked for null and empty states
- [ ] Strings validated for null, empty, and whitespace-only
- [ ] Configuration parameters validated for business rules
- [ ] Thread-safe operations (no shared mutable state issues)
- [ ] Resource cleanup handled properly (try-with-resources where applicable)

✅ **Method Design**
- [ ] Throws RagException for business logic errors consistently
- [ ] Returns expected type consistently (no mixed null/false returns)
- [ ] Includes meaningful error messages with context
- [ ] Follows single responsibility principle
- [ ] Has clear preconditions and postconditions

✅ **Exception Handling**  
- [ ] Catches specific exceptions when possible (avoid catching Exception)
- [ ] Preserves original exception as cause in wrapped exceptions
- [ ] Logs error with appropriate level and sufficient context
- [ ] Provides actionable error messages for debugging
- [ ] Implements graceful degradation where appropriate
- [ ] Uses fallback mechanisms for critical operations

✅ **Business Logic Safety**
- [ ] Handles edge cases gracefully (empty results, boundary conditions)
- [ ] Validates business rules early in processing
- [ ] Provides safe defaults when operation cannot complete
- [ ] Implements retry logic for transient failures
- [ ] Fails fast for invalid states or configurations

✅ **Testing Requirements**
- [ ] Tests happy path scenarios with valid inputs
- [ ] Tests error scenarios with exception verification
- [ ] Tests edge cases (null, empty, invalid inputs)
- [ ] Tests boundary conditions (min/max values, limits)
- [ ] Verifies proper error message format and content
- [ ] Tests thread safety if applicable
- [ ] Tests fallback and retry mechanisms

## Migration Strategy

### For Existing Services
1. **Audit current error handling**: Identify methods returning null/false for errors
2. **Update return types**: Change to throw RagException consistently  
3. **Update tests**: Modify test expectations to check for exceptions
4. **Validate integration**: Ensure calling code handles new exception behavior

### Implementation Order
1. Core service methods first (highest business impact)
2. Integration points second (external dependencies)
3. Utility methods last (lowest impact)

This approach ensures consistent, predictable error handling that improves debugging, monitoring, and system reliability.
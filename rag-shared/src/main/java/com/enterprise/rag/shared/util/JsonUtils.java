package com.enterprise.rag.shared.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.Map;

/**
 * Enterprise-grade JSON utility class providing comprehensive serialization and deserialization operations.
 * 
 * <p>This utility class offers a centralized, thread-safe approach to JSON operations across the
 * Enterprise RAG system. It provides a preconfigured Jackson ObjectMapper optimized for enterprise
 * use cases, including proper date/time handling, type safety, and performance optimization.</p>
 * 
 * <p><strong>Key Features:</strong></p>
 * <ul>
 *   <li><strong>Thread-Safe Operations:</strong> Static ObjectMapper instance with thread-safe configuration</li>
 *   <li><strong>Enterprise Configuration:</strong> Optimized Jackson settings for production environments</li>
 *   <li><strong>Type Safety:</strong> Generic type support with TypeReference for complex types</li>
 *   <li><strong>Date/Time Support:</strong> Java 8+ date/time types with ISO format serialization</li>
 *   <li><strong>Error Handling:</strong> Comprehensive exception wrapping with meaningful messages</li>
 *   <li><strong>Validation Support:</strong> JSON structure validation and parsing verification</li>
 * </ul>
 * 
 * <p><strong>ObjectMapper Configuration:</strong></p>
 * <ul>
 *   <li>JavaTimeModule registered for Java 8+ date/time support</li>
 *   <li>Date timestamps disabled (uses ISO format strings)</li>
 *   <li>Optimized for both performance and readability</li>
 *   <li>Consistent configuration across all microservices</li>
 * </ul>
 * 
 * <p><strong>Usage Examples:</strong></p>
 * <pre>{@code
 * // Object to JSON
 * User user = new User("john@example.com", "John Doe");
 * String json = JsonUtils.toJson(user);
 * 
 * // JSON to Object
 * User parsedUser = JsonUtils.fromJson(json, User.class);
 * 
 * // JSON to Generic Type
 * List<User> users = JsonUtils.fromJson(json, new TypeReference<List<User>>() {});
 * 
 * // Object to Map
 * Map<String, Object> map = JsonUtils.toMap(user);
 * 
 * // Validation
 * if (JsonUtils.isValidJson(input)) {
 *     JsonNode node = JsonUtils.toJsonNode(input);
 * }
 * }</pre>
 * 
 * <p><strong>Error Handling:</strong></p>
 * <p>All methods wrap Jackson exceptions in RuntimeException with descriptive error messages.
 * This approach simplifies error handling while maintaining clear error context for debugging
 * and monitoring in production environments.</p>
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 * @see ObjectMapper
 * @see TypeReference
 * @see JsonNode
 */
public class JsonUtils {

    /**
     * Preconfigured Jackson ObjectMapper instance optimized for enterprise use.
     * 
     * <p>This ObjectMapper is configured with:</p>
     * <ul>
     *   <li>JavaTimeModule for Java 8+ date/time support</li>
     *   <li>Disabled timestamp serialization (uses ISO format strings)</li>
     *   <li>Thread-safe configuration suitable for concurrent use</li>
     * </ul>
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * Serializes any Java object to its JSON string representation.
     * 
     * <p>This method handles all serializable Java objects including POJOs, collections,
     * maps, and objects with proper Jackson annotations. Date/time objects are serialized
     * using ISO format strings for maximum compatibility.</p>
     * 
     * @param object the Java object to serialize to JSON
     * @return JSON string representation of the object
     * @throws RuntimeException if serialization fails due to Jackson processing errors
     * @see ObjectMapper#writeValueAsString(Object)
     */
    public static String toJson(Object object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }

    /**
     * Deserializes JSON string to a Java object of the specified type.
     * 
     * <p>This method provides type-safe JSON deserialization with comprehensive
     * error handling. Supports all Jackson-compatible types including POJOs
     * with proper constructors or Jackson annotations.</p>
     * 
     * @param <T> the target type for deserialization
     * @param json the JSON string to deserialize
     * @param clazz the Class object representing the target type
     * @return deserialized Java object of type T
     * @throws RuntimeException if deserialization fails due to invalid JSON or type mismatch
     * @see ObjectMapper#readValue(String, Class)
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize JSON to object", e);
        }
    }

    /**
     * Deserializes JSON string to a complex generic type using TypeReference.
     * 
     * <p>This method enables deserialization of complex generic types such as
     * {@code List<User>}, {@code Map<String, List<Object>>}, etc. that cannot be
     * represented with simple Class objects due to Java's type erasure.</p>
     * 
     * <p><strong>Usage Example:</strong></p>
     * <pre>{@code
     * List<User> users = JsonUtils.fromJson(json, new TypeReference<List<User>>() {});
     * Map<String, List<Document>> docs = JsonUtils.fromJson(json, 
     *     new TypeReference<Map<String, List<Document>>>() {});
     * }</pre>
     * 
     * @param <T> the target generic type for deserialization
     * @param json the JSON string to deserialize
     * @param typeReference the TypeReference specifying the complex generic type
     * @return deserialized Java object of the specified generic type
     * @throws RuntimeException if deserialization fails due to invalid JSON or type mismatch
     * @see ObjectMapper#readValue(String, TypeReference)
     * @see TypeReference
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize JSON to object", e);
        }
    }

    /**
     * Converts any Java object to a Map representation for dynamic processing.
     * 
     * <p>This method is useful for converting objects to key-value pairs for
     * dynamic processing, templating, or when working with unstructured data.
     * Object properties become map keys, and their values become map values.</p>
     * 
     * @param object the Java object to convert to a Map
     * @return Map representation of the object with String keys and Object values
     * @see ObjectMapper#convertValue(Object, TypeReference)
     */
    public static Map<String, Object> toMap(Object object) {
        return OBJECT_MAPPER.convertValue(object, new TypeReference<Map<String, Object>>() {});
    }

    /**
     * Converts a Map to a Java object of the specified type.
     * 
     * <p>This method is the inverse of {@link #toMap(Object)} and enables
     * construction of typed objects from key-value map data, useful for
     * configuration processing and dynamic object creation.</p>
     * 
     * @param <T> the target type for conversion
     * @param map the Map containing object data
     * @param clazz the Class object representing the target type
     * @return typed Java object constructed from the map data
     * @see ObjectMapper#convertValue(Object, Class)
     */
    public static <T> T fromMap(Map<String, Object> map, Class<T> clazz) {
        return OBJECT_MAPPER.convertValue(map, clazz);
    }

    /**
     * Parses JSON string into a JsonNode for tree-based processing.
     * 
     * <p>This method enables tree-based JSON manipulation and querying
     * using Jackson's JsonNode API. Useful for extracting specific values
     * from JSON without deserializing to specific object types.</p>
     * 
     * @param json the JSON string to parse
     * @return JsonNode representing the parsed JSON structure
     * @throws RuntimeException if JSON parsing fails due to invalid format
     * @see ObjectMapper#readTree(String)
     * @see JsonNode
     */
    public static JsonNode toJsonNode(String json) {
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }

    /**
     * Validates whether a string contains valid JSON structure.
     * 
     * <p>This method provides a safe way to validate JSON without throwing
     * exceptions, useful for input validation and error prevention in
     * data processing pipelines.</p>
     * 
     * @param json the string to validate as JSON
     * @return {@code true} if the string is valid JSON, {@code false} otherwise
     */
    public static boolean isValidJson(String json) {
        try {
            OBJECT_MAPPER.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    /**
     * Provides access to the configured ObjectMapper instance for advanced use cases.
     * 
     * <p>This method exposes the underlying ObjectMapper for scenarios requiring
     * direct access to Jackson functionality not covered by the utility methods.
     * Use with caution as direct ObjectMapper modifications can affect system-wide behavior.</p>
     * 
     * <p><strong>Warning:</strong> Do not modify the returned ObjectMapper configuration
     * as it is shared across the entire application.</p>
     * 
     * @return the configured ObjectMapper instance
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }
}
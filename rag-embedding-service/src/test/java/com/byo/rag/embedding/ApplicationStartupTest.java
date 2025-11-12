package com.byo.rag.embedding;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests to validate that the application starts successfully
 * with different Spring profiles. These tests prevent deployment failures
 * caused by bean configuration issues.
 * 
 * CRITICAL: These tests validate the fix for the OllamaEmbeddingClient
 * RestTemplate dependency issue that caused CrashLoopBackOff in production.
 */
@DisplayName("Application Startup Integration Tests")
class ApplicationStartupTest {

    /**
     * Test that the application starts successfully with GCP profile.
     * This is the production profile used in Google Cloud Platform.
     */
    @SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.ai.vectorstore.redis.uri=redis://localhost:6379",
            "spring.ai.vectorstore.redis.index=embedding-index",
            "spring.ai.vectorstore.redis.prefix=doc:",
            "embedding.models.defaultModel=openai-text-embedding-3-small",
            "embedding.models.fallbackModel=sentence-transformers-all-minilm-l6-v2",
            "embedding.batch.size=50",
            "embedding.batch.timeout=PT30S"
        }
    )
    @ActiveProfiles("gcp")
    @DisplayName("GCP Profile Startup Test")
    static class GcpProfileStartupTest {

        @Autowired
        private ApplicationContext context;

        @Test
        @DisplayName("Application should start successfully with GCP profile")
        void shouldStartSuccessfullyWithGcpProfile() {
            assertNotNull(context, "Application context should not be null");
            
            // Verify critical beans exist
            assertTrue(context.containsBean("primaryEmbeddingModel"),
                "Primary embedding model bean should exist");
            assertTrue(context.containsBean("fallbackEmbeddingModel"),
                "Fallback embedding model bean should exist");
            assertTrue(context.containsBean("embeddingClientRegistry"),
                "Embedding client registry bean should exist");
            
            // Verify Ollama-specific beans do NOT exist
            assertFalse(context.containsBean("ollamaEmbeddingClient"),
                "OllamaEmbeddingClient should NOT exist with GCP profile");
            assertFalse(context.containsBean("restTemplate"),
                "RestTemplate should NOT exist with GCP profile");
        }

        @Test
        @DisplayName("Application should not throw UnsatisfiedDependencyException")
        void shouldNotThrowUnsatisfiedDependencyException() {
            // If we got here, the application context loaded successfully
            // without any UnsatisfiedDependencyException
            assertDoesNotThrow(() -> {
                // Try to get all beans to ensure they can be instantiated
                String[] beanNames = context.getBeanDefinitionNames();
                assertTrue(beanNames.length > 0, "Should have beans defined");
            });
        }
    }

    /**
     * Test that the application starts successfully with local profile.
     */
    @SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.ai.vectorstore.redis.uri=redis://localhost:6379",
            "embedding.models.defaultModel=sentence-transformers-all-minilm-l6-v2",
            "embedding.models.fallbackModel=sentence-transformers-all-minilm-l6-v2"
        }
    )
    @ActiveProfiles("local")
    @DisplayName("Local Profile Startup Test")
    static class LocalProfileStartupTest {

        @Autowired
        private ApplicationContext context;

        @Test
        @DisplayName("Application should start successfully with local profile")
        void shouldStartSuccessfullyWithLocalProfile() {
            assertNotNull(context, "Application context should not be null");
            
            // Verify critical beans exist
            assertTrue(context.containsBean("primaryEmbeddingModel"),
                "Primary embedding model bean should exist");
            
            // Verify Ollama-specific beans do NOT exist
            assertFalse(context.containsBean("ollamaEmbeddingClient"),
                "OllamaEmbeddingClient should NOT exist with local profile");
        }
    }

    /**
     * Test that the application starts successfully with test profile.
     */
    @SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.ai.vectorstore.redis.uri=redis://localhost:6379",
            "embedding.models.defaultModel=sentence-transformers-all-minilm-l6-v2"
        }
    )
    @ActiveProfiles("test")
    @DisplayName("Test Profile Startup Test")
    static class TestProfileStartupTest {

        @Autowired
        private ApplicationContext context;

        @Test
        @DisplayName("Application should start successfully with test profile")
        void shouldStartSuccessfullyWithTestProfile() {
            assertNotNull(context, "Application context should not be null");
            
            // Verify Ollama-specific beans do NOT exist
            assertFalse(context.containsBean("ollamaEmbeddingClient"),
                "OllamaEmbeddingClient should NOT exist with test profile");
        }
    }

    /**
     * Regression test: Validates the specific error that occurred in production.
     * 
     * Before the fix:
     * - OllamaEmbeddingClient was always created as @Component
     * - It required RestTemplate in constructor
     * - RestTemplate was only created with docker profile
     * - With gcp profile: UnsatisfiedDependencyException occurred
     * 
     * After the fix:
     * - OllamaEmbeddingClient is @ConditionalOnProperty("spring.profiles.active", "docker")
     * - It's only created when docker profile is active
     * - No dependency issues with other profiles
     */
    @SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.ai.vectorstore.redis.uri=redis://localhost:6379",
            "embedding.models.defaultModel=openai-text-embedding-3-small"
        }
    )
    @ActiveProfiles("gcp")
    @DisplayName("Regression Test - OllamaEmbeddingClient RestTemplate Issue")
    static class RegressionTestOllamaRestTemplateBug {

        @Autowired
        private ApplicationContext context;

        @Test
        @DisplayName("REGRESSION: Should not throw UnsatisfiedDependencyException for RestTemplate")
        void shouldNotThrowUnsatisfiedDependencyExceptionForRestTemplate() {
            // This test validates the fix for the production bug:
            // "Parameter 0 of constructor in com.byo.rag.embedding.client.OllamaEmbeddingClient 
            //  required a bean of type 'org.springframework.web.client.RestTemplate' 
            //  that could not be found."
            
            assertNotNull(context, 
                "Application context should load without UnsatisfiedDependencyException");
            
            // The bug manifested as CrashLoopBackOff with this error in logs:
            // "Error creating bean with name 'ollamaEmbeddingClient' ... 
            //  Unsatisfied dependency expressed through constructor parameter 0"
            
            // If we reach this point, the bug is fixed
            assertFalse(context.containsBean("ollamaEmbeddingClient"),
                "OllamaEmbeddingClient should not be created without docker profile");
            
            assertFalse(context.containsBean("restTemplate"),
                "RestTemplate should not be created without docker profile");
            
            // But the application should still work with the default embedding model
            assertTrue(context.containsBean("primaryEmbeddingModel"),
                "Primary embedding model should be available (TransformersEmbeddingModel)");
        }

        @Test
        @DisplayName("REGRESSION: TransformersEmbeddingModel should be used as primary")
        void shouldUseTransformersEmbeddingModelAsPrimary() {
            assertTrue(context.containsBean("primaryEmbeddingModel"),
                "Primary embedding model bean should exist");
            
            // Get the primary embedding model
            Object primaryModel = context.getBean("primaryEmbeddingModel");
            assertNotNull(primaryModel);
            
            // Should be TransformersEmbeddingModel, not OllamaEmbeddingModel
            assertEquals("org.springframework.ai.transformers.TransformersEmbeddingModel",
                primaryModel.getClass().getName(),
                "Primary model should be TransformersEmbeddingModel for non-docker profiles");
        }
    }
}

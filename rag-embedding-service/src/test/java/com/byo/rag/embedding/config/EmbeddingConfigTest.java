package com.byo.rag.embedding.config;

import com.byo.rag.embedding.client.OllamaEmbeddingClient;
import com.byo.rag.embedding.model.OllamaEmbeddingModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformers.TransformersEmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EmbeddingConfig to verify bean creation and conditional logic.
 * Tests validate the fix for bean conflicts and ensure correct primary bean selection.
 */
class EmbeddingConfigTest {

    /**
     * Test for GCP profile - should create defaultEmbeddingModel as primaryEmbeddingModel.
     */
    @SpringBootTest(classes = {EmbeddingConfig.class})
    @ActiveProfiles("gcp")
    @TestPropertySource(properties = {
        "embedding.models.defaultModel=openai-text-embedding-3-small",
        "embedding.models.fallbackModel=sentence-transformers-all-minilm-l6-v2",
        "embedding.models.cacheTtl=3600",
        "embedding.models.openai.apiKey=test-key",
        "embedding.models.openai.model=text-embedding-3-small",
        "embedding.models.openai.dimensions=1536",
        "embedding.models.transformers.modelPath=models/sentence-transformers/all-MiniLM-L6-v2"
    })
    @DisplayName("GCP Profile Tests")
    static class GcpProfileTest {

        @Autowired
        private ApplicationContext context;

        @Test
        @DisplayName("Should create primaryEmbeddingModel bean for GCP profile")
        void shouldCreatePrimaryEmbeddingModelForGcpProfile() {
            // Verify the bean exists
            assertTrue(context.containsBean("primaryEmbeddingModel"));
            
            // Get the bean
            EmbeddingModel primaryModel = context.getBean("primaryEmbeddingModel", EmbeddingModel.class);
            assertNotNull(primaryModel);
            
            // Should be TransformersEmbeddingModel (not Ollama)
            assertThat(primaryModel).isInstanceOf(TransformersEmbeddingModel.class);
        }

        @Test
        @DisplayName("Should create fallbackEmbeddingModel bean")
        void shouldCreateFallbackEmbeddingModel() {
            assertTrue(context.containsBean("fallbackEmbeddingModel"));
            
            EmbeddingModel fallbackModel = context.getBean("fallbackEmbeddingModel", EmbeddingModel.class);
            assertNotNull(fallbackModel);
            assertThat(fallbackModel).isInstanceOf(TransformersEmbeddingModel.class);
        }

        @Test
        @DisplayName("Should create embeddingClientRegistry bean with correct dependencies")
        void shouldCreateEmbeddingClientRegistry() {
            assertTrue(context.containsBean("embeddingClientRegistry"));
            
            EmbeddingConfig.EmbeddingModelRegistry registry = 
                context.getBean("embeddingClientRegistry", EmbeddingConfig.EmbeddingModelRegistry.class);
            
            assertNotNull(registry);
            assertNotNull(registry.clients());
            assertFalse(registry.clients().isEmpty());
            
            // Verify default model name is set
            assertEquals("openai-text-embedding-3-small", registry.defaultModelName());
        }

        @Test
        @DisplayName("Should NOT create Ollama bean for GCP profile")
        void shouldNotCreateOllamaBeanForGcpProfile() {
            // Ollama bean should not exist when profile is not "docker"
            assertFalse(context.containsBean("ollamaEmbeddingModel"));
        }

        @Test
        @DisplayName("Should have exactly one primaryEmbeddingModel bean")
        void shouldHaveExactlyOnePrimaryEmbeddingModelBean() {
            String[] beanNames = context.getBeanNamesForType(EmbeddingModel.class);
            
            // Count beans named "primaryEmbeddingModel"
            long primaryBeanCount = java.util.Arrays.stream(beanNames)
                .filter(name -> name.equals("primaryEmbeddingModel"))
                .count();
            
            assertEquals(1, primaryBeanCount, 
                "Should have exactly one primaryEmbeddingModel bean, found: " + primaryBeanCount);
        }

        @Test
        @DisplayName("Registry should provide client by model name")
        void registryShouldProvideClientByModelName() {
            EmbeddingConfig.EmbeddingModelRegistry registry = 
                context.getBean("embeddingClientRegistry", EmbeddingConfig.EmbeddingModelRegistry.class);
            
            // Test getting client by name
            EmbeddingModel model = registry.getClient("openai-text-embedding-3-small");
            assertNotNull(model);
            
            // Test fallback model
            EmbeddingModel fallback = registry.getClient("sentence-transformers-all-minilm-l6-v2");
            assertNotNull(fallback);
        }

        @Test
        @DisplayName("Registry should return default client for unknown model")
        void registryShouldReturnDefaultClientForUnknownModel() {
            EmbeddingConfig.EmbeddingModelRegistry registry = 
                context.getBean("embeddingClientRegistry", EmbeddingConfig.EmbeddingModelRegistry.class);
            
            // Test with unknown model name
            EmbeddingModel model = registry.getClient("unknown-model");
            assertNotNull(model, "Should return default model for unknown model name");
        }

        @Test
        @DisplayName("Registry should check if model is available")
        void registryShouldCheckIfModelIsAvailable() {
            EmbeddingConfig.EmbeddingModelRegistry registry = 
                context.getBean("embeddingClientRegistry", EmbeddingConfig.EmbeddingModelRegistry.class);
            
            assertTrue(registry.hasModel("openai-text-embedding-3-small"));
            assertTrue(registry.hasModel("sentence-transformers-all-minilm-l6-v2"));
            assertFalse(registry.hasModel("non-existent-model"));
        }

        @Test
        @DisplayName("Registry should list all available models")
        void registryShouldListAllAvailableModels() {
            EmbeddingConfig.EmbeddingModelRegistry registry = 
                context.getBean("embeddingClientRegistry", EmbeddingConfig.EmbeddingModelRegistry.class);
            
            var availableModels = registry.getAvailableModels();
            assertNotNull(availableModels);
            assertFalse(availableModels.isEmpty());
            assertTrue(availableModels.contains("openai-text-embedding-3-small"));
            assertTrue(availableModels.contains("sentence-transformers-all-minilm-l6-v2"));
        }
    }

    /**
     * Test for Docker profile - should create ollamaEmbeddingModel as primaryEmbeddingModel.
     * Note: This test requires OllamaEmbeddingClient bean to be available.
     */
    @SpringBootTest(classes = {EmbeddingConfig.class, OllamaEmbeddingClient.class})
    @ActiveProfiles("docker")
    @TestPropertySource(properties = {
        "embedding.models.defaultModel=openai-text-embedding-3-small",
        "embedding.models.fallbackModel=sentence-transformers-all-minilm-l6-v2",
        "embedding.models.cacheTtl=3600",
        "embedding.models.openai.apiKey=test-key",
        "ollama.base-url=http://localhost:11434"
    })
    @DisplayName("Docker Profile Tests")
    static class DockerProfileTest {

        @Autowired
        private ApplicationContext context;

        @Test
        @DisplayName("Should create primaryEmbeddingModel bean for Docker profile")
        void shouldCreatePrimaryEmbeddingModelForDockerProfile() {
            assertTrue(context.containsBean("primaryEmbeddingModel"));
            
            EmbeddingModel primaryModel = context.getBean("primaryEmbeddingModel", EmbeddingModel.class);
            assertNotNull(primaryModel);
            
            // Should be OllamaEmbeddingModel (not Transformers)
            assertThat(primaryModel).isInstanceOf(OllamaEmbeddingModel.class);
        }

        @Test
        @DisplayName("Should create RestTemplate bean for Docker profile")
        void shouldCreateRestTemplateForDockerProfile() {
            assertTrue(context.containsBean("restTemplate"));
        }

        @Test
        @DisplayName("Should NOT create defaultEmbeddingModel bean for Docker profile")
        void shouldNotCreateDefaultEmbeddingModelForDockerProfile() {
            // When Ollama bean is created, defaultEmbeddingModel should not be created
            // due to @ConditionalOnMissingBean
            String[] beanNames = context.getBeanNamesForType(EmbeddingModel.class);
            
            // Count beans named "defaultEmbeddingModel"
            long defaultBeanCount = java.util.Arrays.stream(beanNames)
                .filter(name -> name.equals("defaultEmbeddingModel"))
                .count();
            
            assertEquals(0, defaultBeanCount, 
                "defaultEmbeddingModel should not be created when Ollama bean exists");
        }
    }

    /**
     * Test for Local profile - should create defaultEmbeddingModel as primaryEmbeddingModel.
     */
    @SpringBootTest(classes = {EmbeddingConfig.class})
    @ActiveProfiles("local")
    @TestPropertySource(properties = {
        "embedding.models.defaultModel=sentence-transformers-all-minilm-l6-v2",
        "embedding.models.fallbackModel=sentence-transformers-all-minilm-l6-v2",
        "embedding.models.cacheTtl=3600"
    })
    @DisplayName("Local Profile Tests")
    static class LocalProfileTest {

        @Autowired
        private ApplicationContext context;

        @Test
        @DisplayName("Should create primaryEmbeddingModel bean for Local profile")
        void shouldCreatePrimaryEmbeddingModelForLocalProfile() {
            assertTrue(context.containsBean("primaryEmbeddingModel"));
            
            EmbeddingModel primaryModel = context.getBean("primaryEmbeddingModel", EmbeddingModel.class);
            assertNotNull(primaryModel);
            assertThat(primaryModel).isInstanceOf(TransformersEmbeddingModel.class);
        }

        @Test
        @DisplayName("Should NOT create Ollama bean for Local profile")
        void shouldNotCreateOllamaBeanForLocalProfile() {
            assertFalse(context.containsBean("ollamaEmbeddingModel"));
        }
    }

    /**
     * Unit test for EmbeddingModelRegistry record methods (no Spring context needed).
     */
    @DisplayName("EmbeddingModelRegistry Unit Tests")
    static class EmbeddingModelRegistryUnitTest {

        @Test
        @DisplayName("Registry should handle null model name by returning default")
        void shouldHandleNullModelName() {
            EmbeddingModel defaultModel = new TransformersEmbeddingModel();
            EmbeddingModel fallbackModel = new TransformersEmbeddingModel();
            
            java.util.Map<String, EmbeddingModel> clients = new java.util.HashMap<>();
            clients.put("default", defaultModel);
            clients.put("fallback", fallbackModel);
            
            EmbeddingConfig.EmbeddingModelRegistry registry = 
                new EmbeddingConfig.EmbeddingModelRegistry(clients, "default");
            
            EmbeddingModel result = registry.getClient(null);
            assertNotNull(result);
            assertEquals(defaultModel, result);
        }

        @Test
        @DisplayName("Registry should handle empty model name by returning default")
        void shouldHandleEmptyModelName() {
            EmbeddingModel defaultModel = new TransformersEmbeddingModel();
            
            java.util.Map<String, EmbeddingModel> clients = new java.util.HashMap<>();
            clients.put("default", defaultModel);
            
            EmbeddingConfig.EmbeddingModelRegistry registry = 
                new EmbeddingConfig.EmbeddingModelRegistry(clients, "default");
            
            EmbeddingModel result = registry.getClient("");
            assertNotNull(result);
            assertEquals(defaultModel, result);
        }

        @Test
        @DisplayName("Registry should return correct client for valid model name")
        void shouldReturnCorrectClientForValidModelName() {
            EmbeddingModel model1 = new TransformersEmbeddingModel();
            EmbeddingModel model2 = new TransformersEmbeddingModel();
            
            java.util.Map<String, EmbeddingModel> clients = new java.util.HashMap<>();
            clients.put("model1", model1);
            clients.put("model2", model2);
            
            EmbeddingConfig.EmbeddingModelRegistry registry = 
                new EmbeddingConfig.EmbeddingModelRegistry(clients, "model1");
            
            assertEquals(model1, registry.getClient("model1"));
            assertEquals(model2, registry.getClient("model2"));
        }
    }
}

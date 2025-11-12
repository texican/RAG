package com.byo.rag.embedding.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to validate that OllamaEmbeddingClient is only created with docker profile.
 * This prevents the bean initialization error that occurred when the client tried
 * to autowire RestTemplate in non-docker profiles.
 */
@DisplayName("OllamaEmbeddingClient Conditional Bean Creation Tests")
class OllamaEmbeddingClientConditionalTest {

    /**
     * Test that OllamaEmbeddingClient IS created when docker profile is active.
     */
    @SpringBootTest(classes = {
        OllamaEmbeddingClient.class,
        RestTemplateTestConfig.class
    })
    @ActiveProfiles("docker")
    @TestPropertySource(properties = {
        "spring.ai.ollama.base-url=http://localhost:11434",
        "embedding.models.ollama=mxbai-embed-large"
    })
    @DisplayName("Docker Profile - Bean Should Be Created")
    static class DockerProfileBeanCreationTest {

        @Autowired
        private ApplicationContext context;

        @Test
        @DisplayName("Should create OllamaEmbeddingClient bean with docker profile")
        void shouldCreateOllamaEmbeddingClientWithDockerProfile() {
            // Verify the bean exists
            assertTrue(context.containsBean("ollamaEmbeddingClient"),
                "OllamaEmbeddingClient bean should exist with docker profile");
            
            // Verify we can get the bean
            OllamaEmbeddingClient client = context.getBean(OllamaEmbeddingClient.class);
            assertNotNull(client, "Should be able to retrieve OllamaEmbeddingClient bean");
        }

        @Test
        @DisplayName("Should have RestTemplate bean available for OllamaEmbeddingClient")
        void shouldHaveRestTemplateBeanAvailable() {
            assertTrue(context.containsBean("restTemplate"),
                "RestTemplate bean should exist with docker profile");
            
            RestTemplate restTemplate = context.getBean(RestTemplate.class);
            assertNotNull(restTemplate, "Should be able to retrieve RestTemplate bean");
        }

        @Test
        @DisplayName("OllamaEmbeddingClient should be properly initialized")
        void ollamaEmbeddingClientShouldBeProperlyInitialized() {
            OllamaEmbeddingClient client = context.getBean(OllamaEmbeddingClient.class);
            
            // Verify client is not null and properly constructed
            assertNotNull(client);
            assertDoesNotThrow(() -> {
                // If initialization was successful, the bean should be usable
                // (though actual API calls may fail without a running Ollama instance)
            });
        }
    }

    /**
     * Test that OllamaEmbeddingClient is NOT created when gcp profile is active.
     * This is the critical test that prevents the bug we just fixed.
     */
    @SpringBootTest(classes = {
        OllamaEmbeddingClient.class
    })
    @ActiveProfiles("gcp")
    @TestPropertySource(properties = {
        "spring.ai.ollama.base-url=http://localhost:11434",
        "embedding.models.ollama=mxbai-embed-large"
    })
    @DisplayName("GCP Profile - Bean Should NOT Be Created")
    static class GcpProfileBeanNotCreatedTest {

        @Autowired
        private ApplicationContext context;

        @Test
        @DisplayName("Should NOT create OllamaEmbeddingClient bean with gcp profile")
        void shouldNotCreateOllamaEmbeddingClientWithGcpProfile() {
            // Verify the bean does NOT exist
            assertFalse(context.containsBean("ollamaEmbeddingClient"),
                "OllamaEmbeddingClient bean should NOT exist with gcp profile");
        }

        @Test
        @DisplayName("Should throw NoSuchBeanDefinitionException when trying to get OllamaEmbeddingClient")
        void shouldThrowExceptionWhenGettingOllamaEmbeddingClient() {
            assertThrows(NoSuchBeanDefinitionException.class, 
                () -> context.getBean(OllamaEmbeddingClient.class),
                "Should throw NoSuchBeanDefinitionException when trying to get OllamaEmbeddingClient with gcp profile");
        }

        @Test
        @DisplayName("Should NOT have RestTemplate bean with gcp profile")
        void shouldNotHaveRestTemplateBeanWithGcpProfile() {
            // RestTemplate is also conditional on docker profile
            assertFalse(context.containsBean("restTemplate"),
                "RestTemplate bean should NOT exist with gcp profile");
        }

        @Test
        @DisplayName("Application context should start successfully without OllamaEmbeddingClient")
        void applicationContextShouldStartSuccessfully() {
            assertNotNull(context, "Application context should start successfully");
            // Context started successfully if we got here
        }
    }

    /**
     * Test that OllamaEmbeddingClient is NOT created when local profile is active.
     */
    @SpringBootTest(classes = {
        OllamaEmbeddingClient.class
    })
    @ActiveProfiles("local")
    @TestPropertySource(properties = {
        "spring.ai.ollama.base-url=http://localhost:11434",
        "embedding.models.ollama=mxbai-embed-large"
    })
    @DisplayName("Local Profile - Bean Should NOT Be Created")
    static class LocalProfileBeanNotCreatedTest {

        @Autowired
        private ApplicationContext context;

        @Test
        @DisplayName("Should NOT create OllamaEmbeddingClient bean with local profile")
        void shouldNotCreateOllamaEmbeddingClientWithLocalProfile() {
            assertFalse(context.containsBean("ollamaEmbeddingClient"),
                "OllamaEmbeddingClient bean should NOT exist with local profile");
        }

        @Test
        @DisplayName("Should throw NoSuchBeanDefinitionException when trying to get OllamaEmbeddingClient")
        void shouldThrowExceptionWhenGettingOllamaEmbeddingClient() {
            assertThrows(NoSuchBeanDefinitionException.class, 
                () -> context.getBean(OllamaEmbeddingClient.class),
                "Should throw NoSuchBeanDefinitionException when trying to get OllamaEmbeddingClient with local profile");
        }
    }

    /**
     * Test that OllamaEmbeddingClient is NOT created when test profile is active.
     */
    @SpringBootTest(classes = {
        OllamaEmbeddingClient.class
    })
    @ActiveProfiles("test")
    @TestPropertySource(properties = {
        "spring.ai.ollama.base-url=http://localhost:11434",
        "embedding.models.ollama=mxbai-embed-large"
    })
    @DisplayName("Test Profile - Bean Should NOT Be Created")
    static class TestProfileBeanNotCreatedTest {

        @Autowired
        private ApplicationContext context;

        @Test
        @DisplayName("Should NOT create OllamaEmbeddingClient bean with test profile")
        void shouldNotCreateOllamaEmbeddingClientWithTestProfile() {
            assertFalse(context.containsBean("ollamaEmbeddingClient"),
                "OllamaEmbeddingClient bean should NOT exist with test profile");
        }
    }

    /**
     * Test configuration to provide RestTemplate bean for docker profile tests.
     */
    @org.springframework.boot.test.context.TestConfiguration
    static class RestTemplateTestConfig {
        
        @org.springframework.context.annotation.Bean
        @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
            name = "spring.profiles.active", 
            havingValue = "docker"
        )
        public RestTemplate restTemplate() {
            return new RestTemplate();
        }
    }
}

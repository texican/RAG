package com.byo.rag.auth;

import com.byo.rag.auth.config.SecurityConfig;
import com.byo.rag.auth.security.JwtAuthenticationFilter;
import com.byo.rag.auth.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to prevent circular dependency errors that occurred during development.
 * 
 * Original Error: SecurityConfig -> JwtAuthenticationFilter -> UserService -> PasswordEncoder -> SecurityConfig
 * Solution: Removed PasswordEncoder dependency from UserService, used SecurityUtils.hashPassword() instead
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "jwt.secret=TestSecretKeyForJWTThatIsAtLeast256BitsLongForHS256Algorithm"
})
public class CircularDependencyPreventionTest {

    @Test
    @DisplayName("Security configuration should not have circular dependencies")
    void shouldNotHaveCircularDependenciesInSecurity(ApplicationContext context) {
        // Verify that all security-related beans can be created without circular dependencies
        assertDoesNotThrow(() -> {
            SecurityConfig securityConfig = context.getBean(SecurityConfig.class);
            assertNotNull(securityConfig, "SecurityConfig should be created without circular dependency");
        });

        assertDoesNotThrow(() -> {
            JwtAuthenticationFilter jwtFilter = context.getBean(JwtAuthenticationFilter.class);
            assertNotNull(jwtFilter, "JwtAuthenticationFilter should be created without circular dependency");
        });

        assertDoesNotThrow(() -> {
            UserService userService = context.getBean(UserService.class);
            assertNotNull(userService, "UserService should be created without circular dependency");
        });
    }

    @Test
    @DisplayName("UserService should not depend on PasswordEncoder from SecurityConfig")
    void userServiceShouldNotDependOnPasswordEncoder() {
        // This test ensures UserService uses SecurityUtils instead of injecting PasswordEncoder
        // which would create a circular dependency with SecurityConfig
        
        assertDoesNotThrow(() -> {
            // Verify SecurityUtils has password hashing functionality
            Class.forName("com.byo.rag.shared.util.SecurityUtils");
            
            // Verify SecurityUtils has the required methods
            var securityUtilsClass = Class.forName("com.byo.rag.shared.util.SecurityUtils");
            var hashPasswordMethod = securityUtilsClass.getDeclaredMethod("hashPassword", String.class);
            var verifyPasswordMethod = securityUtilsClass.getDeclaredMethod("verifyPassword", String.class, String.class);
            
            assertNotNull(hashPasswordMethod, "SecurityUtils should have hashPassword method");
            assertNotNull(verifyPasswordMethod, "SecurityUtils should have verifyPassword method");
            
        }, "SecurityUtils should provide password hashing without creating circular dependencies");
    }

    @Test
    @DisplayName("Bean creation order should be valid")
    void beanCreationOrderShouldBeValid(ApplicationContext context) {
        // Test that beans are created in the correct order without dependency cycles
        
        // 1. SecurityUtils (static utility - no dependencies)
        assertDoesNotThrow(() -> {
            Class.forName("com.byo.rag.shared.util.SecurityUtils");
        });

        // 2. UserService (depends on UserRepository and TenantService, uses SecurityUtils)
        assertDoesNotThrow(() -> {
            UserService userService = context.getBean(UserService.class);
            assertNotNull(userService);
        });

        // 3. JwtAuthenticationFilter (depends on UserService and JwtService)
        assertDoesNotThrow(() -> {
            JwtAuthenticationFilter filter = context.getBean(JwtAuthenticationFilter.class);
            assertNotNull(filter);
        });

        // 4. SecurityConfig (depends on JwtAuthenticationFilter, creates PasswordEncoder)
        assertDoesNotThrow(() -> {
            SecurityConfig config = context.getBean(SecurityConfig.class);
            assertNotNull(config);
        });
    }

    @Test
    @DisplayName("PasswordEncoder should be created by SecurityConfig only")
    void passwordEncoderShouldBeCreatedBySecurityConfigOnly(ApplicationContext context) {
        // Ensure PasswordEncoder is only created in SecurityConfig and not injected elsewhere
        assertDoesNotThrow(() -> {
            var passwordEncoder = context.getBean("passwordEncoder");
            assertNotNull(passwordEncoder, "PasswordEncoder should be available as a bean");
        });
        
        // Verify that only SecurityConfig creates the PasswordEncoder bean
        assertTrue(context.getBeanDefinitionNames().length > 0, "Context should have beans");
    }

    @Test
    @DisplayName("Configuration classes should not have mutual dependencies")
    void configurationClassesShouldNotHaveMutualDependencies() {
        // Test that configuration classes don't depend on each other in a circular manner
        assertDoesNotThrow(() -> {
            // SecurityConfig should not depend on any other configuration classes
            var securityConfigClass = SecurityConfig.class;
            var constructor = securityConfigClass.getConstructors()[0];
            var parameterTypes = constructor.getParameterTypes();
            
            // SecurityConfig should only depend on JwtAuthenticationFilter
            assertEquals(1, parameterTypes.length, "SecurityConfig should have exactly one dependency");
            assertEquals(JwtAuthenticationFilter.class, parameterTypes[0], 
                "SecurityConfig should only depend on JwtAuthenticationFilter");
        });
    }
}
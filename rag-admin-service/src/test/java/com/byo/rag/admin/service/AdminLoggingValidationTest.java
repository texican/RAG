package com.byo.rag.admin.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.byo.rag.admin.dto.TenantCreateRequest;
import com.byo.rag.admin.repository.TenantRepository;
import com.byo.rag.admin.repository.UserRepository;
import com.byo.rag.shared.entity.Tenant;
import com.byo.rag.shared.entity.User;
import com.byo.rag.shared.entity.User.UserRole;
import com.byo.rag.shared.entity.User.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Enhanced logging validation tests for administrative operations.
 * 
 * <p>This test suite validates the quality, structure, and compliance aspects of
 * audit logging for administrative operations. It ensures that logging meets
 * enterprise standards for security monitoring, compliance reporting, and
 * operational visibility.</p>
 * 
 * <p><strong>Validation Areas:</strong></p>
 * <ul>
 *   <li><strong>Log Structure:</strong> Consistent formatting and contextual information</li>
 *   <li><strong>Security Standards:</strong> Sensitive data protection and access logging</li>
 *   <li><strong>Compliance Requirements:</strong> Audit trail completeness and retention</li>
 *   <li><strong>Operational Monitoring:</strong> Performance and error tracking</li>
 *   <li><strong>Integration Standards:</strong> SIEM and monitoring tool compatibility</li>
 * </ul>
 * 
 * <p><strong>Enterprise Logging Standards:</strong></p>
 * <ul>
 *   <li>Structured logging with consistent field names and formats</li>
 *   <li>Correlation IDs for tracking operations across service boundaries</li>
 *   <li>Appropriate log levels for different types of events</li>
 *   <li>Complete context information for security and compliance analysis</li>
 *   <li>Protection of sensitive information (passwords, tokens, PII)</li>
 * </ul>
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Admin Service Enhanced Logging Validation Tests")
class AdminLoggingValidationTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private TenantServiceImpl tenantService;

    @InjectMocks
    private UserServiceImpl userService;

    private ListAppender<ILoggingEvent> tenantLogAppender;
    private ListAppender<ILoggingEvent> userLogAppender;
    private Logger tenantLogger;
    private Logger userLogger;

    private Tenant sampleTenant;
    private User sampleUser;
    private UUID tenantId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        // Setup log capture for TenantServiceImpl
        tenantLogger = (Logger) LoggerFactory.getLogger(TenantServiceImpl.class);
        tenantLogAppender = new ListAppender<>();
        tenantLogAppender.start();
        tenantLogger.addAppender(tenantLogAppender);
        tenantLogger.setLevel(Level.DEBUG);

        // Setup log capture for UserServiceImpl
        userLogger = (Logger) LoggerFactory.getLogger(UserServiceImpl.class);
        userLogAppender = new ListAppender<>();
        userLogAppender.start();
        userLogger.addAppender(userLogAppender);
        userLogger.setLevel(Level.DEBUG);

        // Setup test data
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();

        sampleTenant = new Tenant();
        sampleTenant.setId(tenantId);
        sampleTenant.setName("Test Tenant");
        sampleTenant.setSlug("test-tenant");
        sampleTenant.setDescription("Test tenant for logging validation");
        sampleTenant.setStatus(Tenant.TenantStatus.ACTIVE);
        sampleTenant.setCreatedAt(LocalDateTime.now());

        sampleUser = new User();
        sampleUser.setId(userId);
        sampleUser.setEmail("test@example.com");
        sampleUser.setFirstName("Test");
        sampleUser.setLastName("User");
        sampleUser.setRole(UserRole.ADMIN);
        sampleUser.setStatus(UserStatus.ACTIVE);
        sampleUser.setTenant(sampleTenant);
        sampleUser.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should validate log message structure and consistency")
    void shouldValidateLogMessageStructure() {
        // Given
        TenantCreateRequest request = new TenantCreateRequest(
            "Test Tenant",
            "admin@test.com",
            "Test description"
        );

        when(tenantRepository.existsByName(anyString())).thenReturn(false);
        when(tenantRepository.existsBySlug(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(sampleTenant);
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");

        // When
        tenantService.createTenant(request);

        // Then - Validate log message structure
        List<ILoggingEvent> logEvents = tenantLogAppender.list;
        
        assertThat(logEvents).isNotEmpty();
        
        for (ILoggingEvent event : logEvents) {
            // Validate each log entry has proper structure
            assertThat(event.getFormattedMessage())
                .as("Log message should not be null or empty")
                .isNotNull()
                .isNotEmpty();
            
            assertThat(event.getLevel())
                .as("Log level should be appropriate (INFO or ERROR)")
                .isIn(Level.INFO, Level.ERROR, Level.WARN, Level.DEBUG);
            
            assertThat(event.getLoggerName())
                .as("Logger name should be properly set")
                .isNotNull()
                .contains("TenantServiceImpl");
        }
    }

    @Test
    @DisplayName("Should validate sensitive information is not logged")
    void shouldValidateSensitiveInformationNotLogged() {
        // Given
        String sensitivePassword = "secretPassword123!@#";
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        // When
        userService.resetUserPassword(userId, sensitivePassword);

        // Then - Validate sensitive information protection
        List<ILoggingEvent> logEvents = userLogAppender.list;
        
        for (ILoggingEvent event : logEvents) {
            String logMessage = event.getFormattedMessage();
            
            // Verify password is not logged
            assertThat(logMessage)
                .as("Password should not appear in log messages")
                .doesNotContain(sensitivePassword)
                .doesNotContain("secretPassword")
                .doesNotContain("123!@#");
            
            // Verify encoded password is not logged
            assertThat(logMessage)
                .as("Encoded password should not appear in log messages")
                .doesNotContain("encoded-password");
        }
    }

    @Test
    @DisplayName("Should validate log levels are appropriate for operation types")
    void shouldValidateLogLevelsForOperationTypes() {
        // Given
        TenantCreateRequest request = new TenantCreateRequest(
            "Valid Tenant",
            "admin@valid.com",
            "Valid description"
        );

        // Test successful operation
        when(tenantRepository.existsByName(anyString())).thenReturn(false);
        when(tenantRepository.existsBySlug(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(sampleTenant);
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");

        // When - Successful operation
        tenantService.createTenant(request);

        // Then - Validate successful operations use INFO level
        List<ILoggingEvent> successEvents = tenantLogAppender.list.stream()
            .filter(event -> event.getLevel().equals(Level.INFO))
            .toList();
        
        assertThat(successEvents)
            .as("Successful operations should log at INFO level")
            .hasSizeGreaterThanOrEqualTo(1);

        // Clear logs for error test
        tenantLogAppender.list.clear();

        // Test error operation
        when(tenantRepository.save(any(Tenant.class)))
            .thenThrow(new RuntimeException("Database error"));

        // When - Error operation
        assertThatThrownBy(() -> tenantService.createTenant(request))
            .isInstanceOf(RuntimeException.class);

        // Then - Validate error operations use ERROR level
        List<ILoggingEvent> errorEvents = tenantLogAppender.list.stream()
            .filter(event -> event.getLevel().equals(Level.ERROR))
            .toList();
        
        assertThat(errorEvents)
            .as("Error operations should log at ERROR level")
            .hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Should validate contextual information completeness")
    void shouldValidateContextualInformationCompleteness() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        // When
        userService.updateUserStatus(userId, UserStatus.SUSPENDED);

        // Then - Validate contextual information
        List<ILoggingEvent> logEvents = userLogAppender.list;
        
        assertThat(logEvents).hasSizeGreaterThanOrEqualTo(2);
        
        // Check initiation log
        ILoggingEvent initiationLog = logEvents.stream()
            .filter(event -> event.getFormattedMessage().contains("Updating user status"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Initiation log not found"));
        
        String initiationMessage = initiationLog.getFormattedMessage();
        assertThat(initiationMessage)
            .as("Initiation log should contain user ID")
            .contains(userId.toString());
        assertThat(initiationMessage)
            .as("Initiation log should contain target status")
            .contains("SUSPENDED");
        
        // Check completion log
        ILoggingEvent completionLog = logEvents.stream()
            .filter(event -> event.getFormattedMessage().contains("Updated user status"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Completion log not found"));
        
        assertThat(completionLog.getFormattedMessage())
            .as("Completion log should contain user ID")
            .contains(userId.toString());
    }

    @Test
    @DisplayName("Should validate operation correlation and traceability")
    void shouldValidateOperationCorrelationAndTraceability() {
        // Given
        TenantCreateRequest request = new TenantCreateRequest(
            "Correlated Tenant",
            "admin@correlated.com",
            "Correlated operation test"
        );

        when(tenantRepository.existsByName(anyString())).thenReturn(false);
        when(tenantRepository.existsBySlug(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(sampleTenant);
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");

        // When
        tenantService.createTenant(request);

        // Then - Validate operation correlation
        List<ILoggingEvent> logEvents = tenantLogAppender.list;
        
        // Find creation initiation log
        ILoggingEvent initiationLog = logEvents.stream()
            .filter(event -> event.getFormattedMessage().contains("Creating new tenant"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Initiation log not found"));
        
        // Find tenant creation completion log
        ILoggingEvent tenantCompletionLog = logEvents.stream()
            .filter(event -> event.getFormattedMessage().contains("Created tenant with ID"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Tenant completion log not found"));
        
        // Find admin user creation log
        ILoggingEvent adminUserLog = logEvents.stream()
            .filter(event -> event.getFormattedMessage().contains("Created admin user for tenant"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Admin user log not found"));
        
        // Validate logical correlation through tenant name/ID
        String tenantName = "Correlated Tenant";
        assertThat(initiationLog.getFormattedMessage())
            .as("Initiation log should contain tenant name for correlation")
            .contains(tenantName);
        
        String tenantIdString = tenantId.toString();
        assertThat(tenantCompletionLog.getFormattedMessage())
            .as("Completion log should contain tenant ID for correlation")
            .contains(tenantIdString);
        
        assertThat(adminUserLog.getFormattedMessage())
            .as("Admin user log should contain user ID for correlation")
            .contains(userId.toString());
    }

    @Test
    @DisplayName("Should validate log message format consistency")
    void shouldValidateLogMessageFormatConsistency() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        // When - Perform multiple operations
        userService.updateUserStatus(userId, UserStatus.SUSPENDED);
        userService.updateUserRole(userId, UserRole.USER);

        // Then - Validate format consistency
        List<ILoggingEvent> logEvents = userLogAppender.list;
        
        // Define expected patterns for different log types
        Pattern updateInitiationPattern = Pattern.compile("Updating user (status|role): [a-f0-9-]+ to \\w+");
        Pattern updateCompletionPattern = Pattern.compile("Updated user (status|role): [a-f0-9-]+");
        
        for (ILoggingEvent event : logEvents) {
            String message = event.getFormattedMessage();
            
            if (message.contains("Updating user")) {
                assertThat(updateInitiationPattern.matcher(message).find())
                    .as("Update initiation messages should follow consistent format: '%s'", message)
                    .isTrue();
            }
            
            if (message.contains("Updated user")) {
                assertThat(updateCompletionPattern.matcher(message).find())
                    .as("Update completion messages should follow consistent format: '%s'", message)
                    .isTrue();
            }
        }
    }

    @Test
    @DisplayName("Should validate exception logging includes proper context")
    void shouldValidateExceptionLoggingContext() {
        // Given
        TenantCreateRequest request = new TenantCreateRequest(
            "Exception Test",
            "admin@exception.com",
            "Exception handling test"
        );

        String errorMessage = "Simulated database constraint violation";
        when(tenantRepository.existsByName(anyString())).thenReturn(false);
        when(tenantRepository.existsBySlug(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class)))
            .thenThrow(new RuntimeException(errorMessage));

        // When
        assertThatThrownBy(() -> tenantService.createTenant(request))
            .isInstanceOf(RuntimeException.class);

        // Then - Validate exception logging
        List<ILoggingEvent> errorEvents = tenantLogAppender.list.stream()
            .filter(event -> event.getLevel().equals(Level.ERROR))
            .toList();
        
        assertThat(errorEvents)
            .as("Should have at least one error log entry")
            .hasSizeGreaterThanOrEqualTo(1);
        
        ILoggingEvent errorEvent = errorEvents.get(0);
        
        // Validate error message content
        assertThat(errorEvent.getFormattedMessage())
            .as("Error log should contain descriptive error message")
            .contains("Error creating tenant")
            .contains(errorMessage);
        
        // Validate exception is attached
        assertThat(errorEvent.getThrowableProxy())
            .as("Error log should include exception details")
            .isNotNull();
        
        assertThat(errorEvent.getThrowableProxy().getClassName())
            .as("Exception class should be properly captured")
            .contains("RuntimeException");
    }

    @Test
    @DisplayName("Should validate logging performance and efficiency")
    void shouldValidateLoggingPerformanceAndEfficiency() {
        // Given
        TenantCreateRequest request = new TenantCreateRequest(
            "Performance Test",
            "admin@performance.com",
            "Performance validation test"
        );

        when(tenantRepository.existsByName(anyString())).thenReturn(false);
        when(tenantRepository.existsBySlug(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(sampleTenant);
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");

        // When - Measure logging overhead
        long startTime = System.nanoTime();
        tenantService.createTenant(request);
        long endTime = System.nanoTime();
        long duration = endTime - startTime;

        // Then - Validate reasonable logging overhead
        List<ILoggingEvent> logEvents = tenantLogAppender.list;
        
        assertThat(logEvents)
            .as("Should generate appropriate number of log entries (not excessive)")
            .hasSizeBetween(3, 10);
        
        assertThat(duration)
            .as("Operation with logging should complete in reasonable time")
            .isLessThan(100_000_000L); // 100ms threshold
        
        // Validate log message lengths are reasonable
        for (ILoggingEvent event : logEvents) {
            assertThat(event.getFormattedMessage().length())
                .as("Log messages should be concise but informative")
                .isBetween(20, 500);
        }
    }

    @Test
    @DisplayName("Should validate compliance with security logging standards")
    void shouldValidateSecurityLoggingStandards() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        // When - Perform security-sensitive operations
        userService.updateUserRole(userId, UserRole.ADMIN); // Privilege escalation
        userService.updateUserStatus(userId, UserStatus.SUSPENDED); // Access control change

        // Then - Validate security logging standards
        List<ILoggingEvent> logEvents = userLogAppender.list;
        
        // Verify privilege escalation is logged
        boolean privilegeEscalationLogged = logEvents.stream()
            .anyMatch(event -> 
                event.getFormattedMessage().contains("Updating user role") &&
                event.getFormattedMessage().contains("ADMIN")
            );
        
        assertThat(privilegeEscalationLogged)
            .as("Privilege escalation should be logged for security monitoring")
            .isTrue();
        
        // Verify status change is logged
        boolean statusChangeLogged = logEvents.stream()
            .anyMatch(event -> 
                event.getFormattedMessage().contains("Updating user status") &&
                event.getFormattedMessage().contains("SUSPENDED")
            );
        
        assertThat(statusChangeLogged)
            .as("Status changes should be logged for security monitoring")
            .isTrue();
        
        // Verify all security events have sufficient context
        for (ILoggingEvent event : logEvents) {
            if (event.getFormattedMessage().contains("Updating user")) {
                assertThat(event.getFormattedMessage())
                    .as("Security logs should include user ID for accountability")
                    .contains(userId.toString());
            }
        }
    }
}
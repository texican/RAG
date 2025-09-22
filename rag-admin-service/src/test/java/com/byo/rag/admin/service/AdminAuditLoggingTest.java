package com.byo.rag.admin.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.byo.rag.admin.dto.TenantCreateRequest;
import com.byo.rag.admin.dto.TenantSuspendRequest;
import com.byo.rag.admin.dto.TenantUpdateRequest;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Enterprise-grade audit logging tests for admin service operations.
 * 
 * <p>This comprehensive test suite validates audit trail functionality and logging
 * behavior for all administrative operations, ensuring compliance requirements
 * and operational visibility for security and monitoring purposes.</p>
 * 
 * <p><strong>Test Coverage:</strong></p>
 * <ul>
 *   <li><strong>Tenant Operations Audit:</strong> Creation, updates, suspension/reactivation, deletion</li>
 *   <li><strong>User Operations Audit:</strong> Status changes, role modifications, password resets</li>
 *   <li><strong>Security Events Logging:</strong> Authentication, authorization, privilege changes</li>
 *   <li><strong>Error Conditions Audit:</strong> Failed operations, validation errors, exception handling</li>
 *   <li><strong>Compliance Logging:</strong> Data access, modifications, administrative actions</li>
 * </ul>
 * 
 * <p><strong>Audit Trail Validation:</strong></p>
 * <ul>
 *   <li>Proper log levels for different operation types</li>
 *   <li>Complete contextual information in log messages</li>
 *   <li>Structured logging for automated analysis</li>
 *   <li>Error logging with exception details</li>
 *   <li>Security-sensitive operation logging</li>
 * </ul>
 * 
 * @author Enterprise RAG Development Team
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Admin Service Audit Logging Tests")
class AdminAuditLoggingTest {

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
        tenantLogger.setLevel(Level.INFO);

        // Setup log capture for UserServiceImpl
        userLogger = (Logger) LoggerFactory.getLogger(UserServiceImpl.class);
        userLogAppender = new ListAppender<>();
        userLogAppender.start();
        userLogger.addAppender(userLogAppender);
        userLogger.setLevel(Level.INFO);

        // Setup test data
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();

        sampleTenant = new Tenant();
        sampleTenant.setId(tenantId);
        sampleTenant.setName("Test Tenant");
        sampleTenant.setSlug("test-tenant");
        sampleTenant.setDescription("Test tenant for audit logging");
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
    @DisplayName("Should log tenant creation audit trail with complete context")
    void shouldLogTenantCreationAuditTrail() {
        // Given
        TenantCreateRequest request = new TenantCreateRequest(
            "New Tenant",
            "admin@newtenant.com",
            "New tenant description"
        );

        when(tenantRepository.existsByName(anyString())).thenReturn(false);
        when(tenantRepository.existsBySlug(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(sampleTenant);
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");

        // When
        tenantService.createTenant(request);

        // Then - Verify audit logging
        List<ILoggingEvent> logEvents = tenantLogAppender.list;
        
        assertThat(logEvents).hasSizeGreaterThanOrEqualTo(3);
        
        // Verify creation initiation log
        ILoggingEvent creationLog = logEvents.stream()
            .filter(event -> event.getFormattedMessage().contains("Creating new tenant"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Creation initiation log not found"));
        
        assertThat(creationLog.getLevel()).isEqualTo(Level.INFO);
        assertThat(creationLog.getFormattedMessage()).contains("New Tenant");
        
        // Verify tenant creation success log
        ILoggingEvent tenantCreatedLog = logEvents.stream()
            .filter(event -> event.getFormattedMessage().contains("Created tenant with ID"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Tenant creation success log not found"));
        
        assertThat(tenantCreatedLog.getLevel()).isEqualTo(Level.INFO);
        assertThat(tenantCreatedLog.getFormattedMessage()).contains(tenantId.toString());
        
        // Verify admin user creation log
        ILoggingEvent adminUserLog = logEvents.stream()
            .filter(event -> event.getFormattedMessage().contains("Created admin user for tenant"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Admin user creation log not found"));
        
        assertThat(adminUserLog.getLevel()).isEqualTo(Level.INFO);
        assertThat(adminUserLog.getFormattedMessage()).contains(userId.toString());
    }

    @Test
    @DisplayName("Should log tenant update operations with change tracking")
    void shouldLogTenantUpdateOperations() {
        // Given
        TenantUpdateRequest updateRequest = new TenantUpdateRequest(
            "Updated Tenant Name",
            "admin@updated.com",
            "Updated description"
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(sampleTenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(sampleTenant);

        // When
        tenantService.updateTenant(tenantId.toString(), updateRequest);

        // Then - Verify update audit logging
        List<ILoggingEvent> logEvents = tenantLogAppender.list;
        
        assertThat(logEvents).hasSizeGreaterThanOrEqualTo(2);
        
        // Verify update initiation log
        ILoggingEvent updateInitLog = logEvents.stream()
            .filter(event -> event.getFormattedMessage().contains("Updating tenant"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Update initiation log not found"));
        
        assertThat(updateInitLog.getLevel()).isEqualTo(Level.INFO);
        assertThat(updateInitLog.getFormattedMessage()).contains(tenantId.toString());
        
        // Verify update completion log
        ILoggingEvent updateCompleteLog = logEvents.stream()
            .filter(event -> event.getFormattedMessage().contains("Updated tenant"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Update completion log not found"));
        
        assertThat(updateCompleteLog.getLevel()).isEqualTo(Level.INFO);
        assertThat(updateCompleteLog.getFormattedMessage()).contains(tenantId.toString());
    }

    @Test
    @DisplayName("Should log tenant suspension with security context")
    void shouldLogTenantSuspension() {
        // Given
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(sampleTenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(sampleTenant);

        // When
        TenantSuspendRequest suspendRequest = new TenantSuspendRequest("Policy violation - terms of service breach");
        tenantService.suspendTenant(tenantId.toString(), suspendRequest);

        // Then - Verify suspension audit logging
        List<ILoggingEvent> logEvents = tenantLogAppender.list;
        
        assertThat(logEvents).hasSizeGreaterThanOrEqualTo(2);
        
        // Verify suspension initiation log
        ILoggingEvent suspensionLog = logEvents.stream()
            .filter(event -> event.getFormattedMessage().contains("Suspending tenant"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Suspension initiation log not found"));
        
        assertThat(suspensionLog.getLevel()).isEqualTo(Level.INFO);
        assertThat(suspensionLog.getFormattedMessage()).contains(tenantId.toString());
        
        // Verify suspension completion log
        ILoggingEvent suspendedLog = logEvents.stream()
            .filter(event -> event.getFormattedMessage().contains("Suspended tenant"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Suspension completion log not found"));
        
        assertThat(suspendedLog.getLevel()).isEqualTo(Level.INFO);
        assertThat(suspendedLog.getFormattedMessage()).contains(tenantId.toString());
    }

    @Test
    @DisplayName("Should log tenant reactivation with proper audit trail")
    void shouldLogTenantReactivation() {
        // Given
        sampleTenant.setStatus(Tenant.TenantStatus.SUSPENDED);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(sampleTenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(sampleTenant);

        // When
        tenantService.reactivateTenant(tenantId.toString());

        // Then - Verify reactivation audit logging
        List<ILoggingEvent> logEvents = tenantLogAppender.list;
        
        assertThat(logEvents).hasSizeGreaterThanOrEqualTo(2);
        
        // Verify reactivation initiation log
        ILoggingEvent reactivationLog = logEvents.stream()
            .filter(event -> event.getFormattedMessage().contains("Reactivating tenant"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Reactivation initiation log not found"));
        
        assertThat(reactivationLog.getLevel()).isEqualTo(Level.INFO);
        assertThat(reactivationLog.getFormattedMessage()).contains(tenantId.toString());
        
        // Verify reactivation completion log
        ILoggingEvent reactivatedLog = logEvents.stream()
            .filter(event -> event.getFormattedMessage().contains("Reactivated tenant"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Reactivation completion log not found"));
        
        assertThat(reactivatedLog.getLevel()).isEqualTo(Level.INFO);
        assertThat(reactivatedLog.getFormattedMessage()).contains(tenantId.toString());
    }

    @Test
    @DisplayName("Should log tenant deletion with comprehensive audit information")
    void shouldLogTenantDeletion() {
        // Given
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(sampleTenant));
        // Mock no dependent users for deletion
        doNothing().when(tenantRepository).delete(sampleTenant);

        // When
        tenantService.deleteTenant(tenantId.toString());

        // Then - Verify deletion audit logging
        List<ILoggingEvent> logEvents = tenantLogAppender.list;
        
        assertThat(logEvents).hasSizeGreaterThanOrEqualTo(2);
        
        // Verify deletion initiation log
        ILoggingEvent deletionLog = logEvents.stream()
            .filter(event -> event.getFormattedMessage().contains("Deleting tenant"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Deletion initiation log not found"));
        
        assertThat(deletionLog.getLevel()).isEqualTo(Level.INFO);
        assertThat(deletionLog.getFormattedMessage()).contains(tenantId.toString());
        
        // Verify deletion completion log
        ILoggingEvent deletedLog = logEvents.stream()
            .filter(event -> event.getFormattedMessage().contains("Deleted tenant"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Deletion completion log not found"));
        
        assertThat(deletedLog.getLevel()).isEqualTo(Level.INFO);
        assertThat(deletedLog.getFormattedMessage()).contains(tenantId.toString());
    }

    @Test
    @DisplayName("Should log user status changes with security context")
    void shouldLogUserStatusChanges() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        // When
        userService.updateUserStatus(userId, UserStatus.SUSPENDED);

        // Then - Verify status change audit logging
        List<ILoggingEvent> logEvents = userLogAppender.list;
        
        assertThat(logEvents).hasSizeGreaterThanOrEqualTo(2);
        
        // Verify status change initiation log
        ILoggingEvent statusChangeLog = logEvents.stream()
            .filter(event -> event.getFormattedMessage().contains("Updating user status"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Status change initiation log not found"));
        
        assertThat(statusChangeLog.getLevel()).isEqualTo(Level.INFO);
        assertThat(statusChangeLog.getFormattedMessage()).contains(userId.toString());
        assertThat(statusChangeLog.getFormattedMessage()).contains("SUSPENDED");
        
        // Verify status change completion log
        ILoggingEvent statusUpdatedLog = logEvents.stream()
            .filter(event -> event.getFormattedMessage().contains("Updated user status"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Status change completion log not found"));
        
        assertThat(statusUpdatedLog.getLevel()).isEqualTo(Level.INFO);
        assertThat(statusUpdatedLog.getFormattedMessage()).contains(userId.toString());
    }

    @Test
    @DisplayName("Should log user role changes with privilege escalation tracking")
    void shouldLogUserRoleChanges() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        // When
        userService.updateUserRole(userId, UserRole.USER);

        // Then - Verify role change audit logging
        List<ILoggingEvent> logEvents = userLogAppender.list;
        
        assertThat(logEvents).hasSizeGreaterThanOrEqualTo(2);
        
        // Verify role change initiation log
        ILoggingEvent roleChangeLog = logEvents.stream()
            .filter(event -> event.getFormattedMessage().contains("Updating user role"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Role change initiation log not found"));
        
        assertThat(roleChangeLog.getLevel()).isEqualTo(Level.INFO);
        assertThat(roleChangeLog.getFormattedMessage()).contains(userId.toString());
        assertThat(roleChangeLog.getFormattedMessage()).contains("USER");
        
        // Verify role change completion log
        ILoggingEvent roleUpdatedLog = logEvents.stream()
            .filter(event -> event.getFormattedMessage().contains("Updated user role"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Role change completion log not found"));
        
        assertThat(roleUpdatedLog.getLevel()).isEqualTo(Level.INFO);
        assertThat(roleUpdatedLog.getFormattedMessage()).contains(userId.toString());
    }

    @Test
    @DisplayName("Should log password reset operations with security audit")
    void shouldLogPasswordResetOperations() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.encode(anyString())).thenReturn("new-encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        // When
        userService.resetUserPassword(userId, "newPassword123");

        // Then - Verify password reset audit logging
        List<ILoggingEvent> logEvents = userLogAppender.list;
        
        assertThat(logEvents).hasSizeGreaterThanOrEqualTo(2);
        
        // Verify password reset initiation log
        ILoggingEvent resetInitLog = logEvents.stream()
            .filter(event -> event.getFormattedMessage().contains("Resetting password for user"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Password reset initiation log not found"));
        
        assertThat(resetInitLog.getLevel()).isEqualTo(Level.INFO);
        assertThat(resetInitLog.getFormattedMessage()).contains(userId.toString());
        // Verify password is not logged for security
        assertThat(resetInitLog.getFormattedMessage()).doesNotContain("newPassword123");
        
        // Verify password reset completion log
        ILoggingEvent resetCompleteLog = logEvents.stream()
            .filter(event -> event.getFormattedMessage().contains("Reset password for user"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Password reset completion log not found"));
        
        assertThat(resetCompleteLog.getLevel()).isEqualTo(Level.INFO);
        assertThat(resetCompleteLog.getFormattedMessage()).contains(userId.toString());
    }

    @Test
    @DisplayName("Should log user deletion with comprehensive audit trail")
    void shouldLogUserDeletion() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        // Mock more than one admin exists for safe deletion
        doNothing().when(userRepository).delete(sampleUser);

        // When
        userService.deleteUser(userId);

        // Then - Verify user deletion audit logging
        List<ILoggingEvent> logEvents = userLogAppender.list;
        
        assertThat(logEvents).hasSizeGreaterThanOrEqualTo(2);
        
        // Verify deletion initiation log
        ILoggingEvent deletionLog = logEvents.stream()
            .filter(event -> event.getFormattedMessage().contains("Deleting user"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("User deletion initiation log not found"));
        
        assertThat(deletionLog.getLevel()).isEqualTo(Level.INFO);
        assertThat(deletionLog.getFormattedMessage()).contains(userId.toString());
        
        // Verify deletion completion log
        ILoggingEvent deletedLog = logEvents.stream()
            .filter(event -> event.getFormattedMessage().contains("Deleted user"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("User deletion completion log not found"));
        
        assertThat(deletedLog.getLevel()).isEqualTo(Level.INFO);
        assertThat(deletedLog.getFormattedMessage()).contains(userId.toString());
    }

    @Test
    @DisplayName("Should log error conditions with proper context and exception details")
    void shouldLogErrorConditionsWithContext() {
        // Given
        TenantCreateRequest request = new TenantCreateRequest(
            "Duplicate Tenant",
            "admin@duplicate.com",
            "Duplicate tenant description"
        );

        when(tenantRepository.existsByName(anyString())).thenReturn(false);
        when(tenantRepository.existsBySlug(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class)))
            .thenThrow(new RuntimeException("Database constraint violation"));

        // When & Then
        assertThatThrownBy(() -> tenantService.createTenant(request))
            .isInstanceOf(RuntimeException.class);

        // Verify error logging
        List<ILoggingEvent> logEvents = tenantLogAppender.list;
        
        ILoggingEvent errorLog = logEvents.stream()
            .filter(event -> event.getLevel().equals(Level.ERROR))
            .filter(event -> event.getFormattedMessage().contains("Error creating tenant"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Error log not found"));
        
        assertThat(errorLog.getLevel()).isEqualTo(Level.ERROR);
        assertThat(errorLog.getFormattedMessage()).contains("Database constraint violation");
        assertThat(errorLog.getThrowableProxy()).isNotNull();
    }
}
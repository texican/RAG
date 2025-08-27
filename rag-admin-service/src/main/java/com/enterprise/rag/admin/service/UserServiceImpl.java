package com.enterprise.rag.admin.service;

import com.enterprise.rag.admin.repository.UserRepository;
import com.enterprise.rag.admin.repository.TenantRepository;
import com.enterprise.rag.shared.entity.User;
import com.enterprise.rag.shared.entity.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of UserService for comprehensive user management in the admin service.
 */
@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                          TenantRepository tenantRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getUsersByTenantId(UUID tenantId) {
        return userRepository.findByTenantId(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> getUsersByTenantId(UUID tenantId, Pageable pageable) {
        return userRepository.findByTenantId(tenantId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> searchUsers(String searchTerm, Pageable pageable) {
        return userRepository.findByNameOrEmailContainingIgnoreCase(searchTerm, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getUsersByStatus(User.UserStatus status) {
        return userRepository.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getUsersByRole(User.UserRole role) {
        return userRepository.findByRole(role);
    }

    @Override
    public User updateUserStatus(UUID userId, User.UserStatus status) {
        logger.info("Updating user status: {} to {}", userId, status);
        
        User user = getUserById(userId);
        user.setStatus(status);
        
        user = userRepository.save(user);
        logger.info("Updated user status: {}", user.getId());
        
        return user;
    }

    @Override
    public User updateUserRole(UUID userId, User.UserRole role) {
        logger.info("Updating user role: {} to {}", userId, role);
        
        User user = getUserById(userId);
        user.setRole(role);
        
        user = userRepository.save(user);
        logger.info("Updated user role: {}", user.getId());
        
        return user;
    }

    @Override
    public void resetUserPassword(UUID userId, String newPassword) {
        logger.info("Resetting password for user: {}", userId);
        
        User user = getUserById(userId);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setStatus(User.UserStatus.PENDING_VERIFICATION); // Require verification after password reset
        
        userRepository.save(user);
        logger.info("Reset password for user: {}", user.getId());
    }

    @Override
    public void deleteUser(UUID userId) {
        logger.info("Deleting user: {}", userId);
        
        User user = getUserById(userId);
        
        // Check if this is the last admin for the tenant
        List<User> tenantAdmins = userRepository.findTenantAdministrators(user.getTenant().getId());
        if (tenantAdmins.size() == 1 && tenantAdmins.get(0).getId().equals(userId)) {
            throw new IllegalStateException("Cannot delete the last administrator for tenant: " + user.getTenant().getName());
        }
        
        userRepository.delete(user);
        logger.info("Deleted user: {}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserStatistics getUserStatistics() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus(User.UserStatus.ACTIVE);
        long suspendedUsers = userRepository.countByStatus(User.UserStatus.SUSPENDED);
        long pendingVerificationUsers = userRepository.countByStatus(User.UserStatus.PENDING_VERIFICATION);
        long adminUsers = userRepository.countByRole(User.UserRole.ADMIN);
        long regularUsers = userRepository.countByRole(User.UserRole.USER);
        long readerUsers = userRepository.countByRole(User.UserRole.READER);

        return new UserStatistics(
                totalUsers,
                activeUsers,
                suspendedUsers,
                pendingVerificationUsers,
                adminUsers,
                regularUsers,
                readerUsers
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TenantUserStatistics getTenantUserStatistics(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found with ID: " + tenantId));

        long totalUsers = userRepository.countByTenantId(tenantId);
        long activeUsers = userRepository.countByTenantIdAndStatus(tenantId, User.UserStatus.ACTIVE);
        long adminUsers = userRepository.countByTenantIdAndRole(tenantId, User.UserRole.ADMIN);

        return new TenantUserStatistics(
                tenantId,
                tenant.getName(),
                totalUsers,
                activeUsers,
                adminUsers
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findInactiveUsers(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        return userRepository.findInactiveUsers(cutoffDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findUsersAwaitingVerification() {
        return userRepository.findByEmailVerifiedFalse();
    }
}
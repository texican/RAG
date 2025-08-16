package com.enterprise.rag.auth.service;

import com.enterprise.rag.auth.repository.UserRepository;
import com.enterprise.rag.shared.dto.TenantDto;
import com.enterprise.rag.shared.dto.UserDto;
import com.enterprise.rag.shared.entity.Tenant;
import com.enterprise.rag.shared.entity.User;
import com.enterprise.rag.shared.exception.TenantAccessDeniedException;
import com.enterprise.rag.shared.exception.UserNotFoundException;
import com.enterprise.rag.shared.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final TenantService tenantService;

    public UserService(UserRepository userRepository, TenantService tenantService) {
        this.userRepository = userRepository;
        this.tenantService = tenantService;
    }

    public UserDto.UserResponse createUser(UserDto.CreateUserRequest request) {
        logger.info("Creating user with email: {}", request.email());

        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("User with email '" + request.email() + "' already exists");
        }

        Tenant tenant = tenantService.findById(request.tenantId());

        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setPasswordHash(SecurityUtils.hashPassword(request.password()));
        user.setRole(request.role() != null ? request.role() : User.UserRole.USER);
        user.setTenant(tenant);
        user.setEmailVerificationToken(SecurityUtils.generateVerificationToken());

        user = userRepository.save(user);
        logger.info("Created user with ID: {}", user.getId());

        return mapToResponse(user);
    }

    @Transactional(readOnly = true)
    public UserDto.UserResponse getUser(UUID userId, UUID requestingUserTenantId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        validateTenantAccess(user.getTenant().getId(), requestingUserTenantId);
        return mapToResponse(user);
    }

    @Transactional(readOnly = true)
    public Page<UserDto.UserSummary> getUsersByTenant(UUID tenantId, Pageable pageable) {
        return userRepository.findByTenantId(tenantId, pageable)
            .map(this::mapToSummary);
    }

    public UserDto.UserResponse updateUser(UUID userId, UserDto.UpdateUserRequest request, UUID requestingUserTenantId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        validateTenantAccess(user.getTenant().getId(), requestingUserTenantId);

        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.email() != null && !request.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new IllegalArgumentException("Email already in use");
            }
            user.setEmail(request.email());
            user.setEmailVerified(false);
            user.setEmailVerificationToken(SecurityUtils.generateVerificationToken());
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.status() != null) {
            user.setStatus(request.status());
        }

        user = userRepository.save(user);
        logger.info("Updated user with ID: {}", user.getId());

        return mapToResponse(user);
    }

    public void deleteUser(UUID userId, UUID requestingUserTenantId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        validateTenantAccess(user.getTenant().getId(), requestingUserTenantId);

        userRepository.deleteById(userId);
        logger.info("Deleted user with ID: {}", userId);
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException(email));
    }

    @Transactional(readOnly = true)
    public User findActiveByEmail(String email) {
        return userRepository.findActiveByEmail(email)
            .orElseThrow(() -> new UserNotFoundException(email));
    }

    public void updateLastLogin(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }

    public UserDto.UserResponse verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
            .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));

        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setStatus(User.UserStatus.ACTIVE);

        user = userRepository.save(user);
        logger.info("Verified email for user: {}", user.getEmail());

        return mapToResponse(user);
    }

    private void validateTenantAccess(UUID resourceTenantId, UUID requestingUserTenantId) {
        if (!resourceTenantId.equals(requestingUserTenantId)) {
            throw new TenantAccessDeniedException(requestingUserTenantId, resourceTenantId);
        }
    }

    private UserDto.UserResponse mapToResponse(User user) {
        TenantDto.TenantSummary tenantSummary = new TenantDto.TenantSummary(
            user.getTenant().getId(),
            user.getTenant().getName(),
            user.getTenant().getSlug(),
            user.getTenant().getStatus(),
            user.getTenant().getCreatedAt()
        );

        return new UserDto.UserResponse(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getEmail(),
            user.getRole(),
            user.getStatus(),
            user.getEmailVerified(),
            user.getLastLoginAt(),
            user.getCreatedAt(),
            tenantSummary
        );
    }

    private UserDto.UserSummary mapToSummary(User user) {
        return new UserDto.UserSummary(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getEmail(),
            user.getRole(),
            user.getStatus()
        );
    }
}
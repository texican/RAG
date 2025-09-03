package com.byo.rag.admin.config;

import com.byo.rag.admin.repository.UserRepository;
import com.byo.rag.shared.entity.Tenant;
import com.byo.rag.shared.entity.User;
import com.byo.rag.admin.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;

@TestConfiguration
@Profile("test")
public class TestDataConfig {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TenantRepository tenantRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    public void setupTestData() {
        // Create a test tenant
        Tenant testTenant = new Tenant();
        testTenant.setName("Test Enterprise");
        testTenant.setSlug("test-enterprise");
        testTenant.setDescription("Test tenant for integration tests");
        testTenant.setStatus(Tenant.TenantStatus.ACTIVE);
        testTenant.setCreatedAt(LocalDateTime.now());
        testTenant.setUpdatedAt(LocalDateTime.now());
        testTenant = tenantRepository.save(testTenant);

        // Create admin user with expected credentials
        User adminUser = new User();
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setEmail("admin@enterprise.com");
        adminUser.setPasswordHash(passwordEncoder.encode("AdminPassword123!"));
        adminUser.setRole(User.UserRole.ADMIN);
        adminUser.setStatus(User.UserStatus.ACTIVE);
        adminUser.setEmailVerified(true);
        adminUser.setTenant(testTenant);
        adminUser.setCreatedAt(LocalDateTime.now());
        adminUser.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(adminUser);
    }
}
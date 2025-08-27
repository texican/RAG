package com.enterprise.rag.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EntityScan(basePackages = {"com.enterprise.rag.shared.entity"})
@EnableJpaRepositories(basePackages = {"com.enterprise.rag.admin.repository"})
@EnableScheduling
public class AdminServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(AdminServiceApplication.class, args);
    }
}
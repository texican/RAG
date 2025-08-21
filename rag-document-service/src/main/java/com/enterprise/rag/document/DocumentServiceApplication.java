package com.enterprise.rag.document;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = {
    "com.enterprise.rag.document",
    "com.enterprise.rag.shared.exception"
})
@EntityScan("com.enterprise.rag.shared.entity")
@EnableJpaRepositories(basePackages = "com.enterprise.rag.document.repository")
@EnableJpaAuditing
@EnableTransactionManagement
@EnableAsync
public class DocumentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentServiceApplication.class, args);
    }
}
package com.enterprise.rag.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication(scanBasePackages = {
    "com.enterprise.rag.core",
    "com.enterprise.rag.shared"
})
@EntityScan(basePackages = "com.enterprise.rag.shared.entity")
public class CoreServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoreServiceApplication.class, args);
    }
}
package com.enterprise.rag.document.config;

import com.enterprise.rag.shared.config.JsonConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(JsonConfig.class)
public class DocumentJpaConfig {
}
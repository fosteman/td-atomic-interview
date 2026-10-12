package com.atomicfx.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Solace configuration for HTTP-based publishing.
 * Solace exposes a REST API on port 8080.
 * We publish events via HTTP POST for simplicity (no SDK version conflicts).
 *
 * In production, this would point to managed Solace Cloud.
 * The REST API is simple, reliable, and language-agnostic.
 */
@Configuration
public class SolaceConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public static class SolaceProperties {
        public static final String BASE_URL = "http://localhost:9000";
        public static final String USERNAME = "admin";
        public static final String PASSWORD = "admin";
    }
}

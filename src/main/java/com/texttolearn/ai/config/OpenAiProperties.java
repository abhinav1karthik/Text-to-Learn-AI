package com.texttolearn.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openai")
public record OpenAiProperties(
        String apiKey,
        String baseUrl,
        String model
) {

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}

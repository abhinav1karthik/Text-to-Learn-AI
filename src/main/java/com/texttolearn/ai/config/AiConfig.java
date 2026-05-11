package com.texttolearn.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({GeminiProperties.class, OpenAiProperties.class})
public class AiConfig {
}

package com.texttolearn.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth0")
public record Auth0Properties(String issuerUri, String audience) {

    public boolean isConfigured() {
        return hasText(issuerUri) && hasText(audience);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

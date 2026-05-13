package com.texttolearn.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth0")
public record Auth0Properties(String issuerUri, String audience, String jwkSetUri) {

    public boolean isConfigured() {
        return hasText(issuerUri) && hasText(audience);
    }

    public String resolvedJwkSetUri() {
        if (hasText(jwkSetUri)) {
            return jwkSetUri;
        }

        String normalizedIssuer = issuerUri.endsWith("/")
                ? issuerUri.substring(0, issuerUri.length() - 1)
                : issuerUri;
        return normalizedIssuer + "/.well-known/jwks.json";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

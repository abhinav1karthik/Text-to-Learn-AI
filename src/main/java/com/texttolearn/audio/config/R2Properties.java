package com.texttolearn.audio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "r2")
public record R2Properties(
        String accountId,
        String accessKeyId,
        String secretAccessKey,
        String bucketName,
        String endpoint
) {

    public boolean isConfigured() {
        return !isBlank(accessKeyId)
                && !isBlank(secretAccessKey)
                && !isBlank(bucketName)
                && (!isBlank(endpoint) || !isBlank(accountId));
    }

    public String effectiveEndpoint() {
        if (!isBlank(endpoint)) {
            return endpoint.trim();
        }

        if (isBlank(accountId)) {
            return "";
        }

        return "https://" + accountId.trim() + ".r2.cloudflarestorage.com";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

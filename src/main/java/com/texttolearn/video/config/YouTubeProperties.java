package com.texttolearn.video.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "youtube")
public record YouTubeProperties(
        String apiKey,
        String baseUrl,
        int maxResults,
        long cacheTtlMinutes
) {

    private static final int DEFAULT_MAX_RESULTS = 3;
    private static final long DEFAULT_CACHE_TTL_MINUTES = 1_440;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public int sanitizedMaxResults() {
        return sanitizedMaxResults(maxResults);
    }

    public int sanitizedMaxResults(int requestedMaxResults) {
        if (requestedMaxResults <= 0) {
            return DEFAULT_MAX_RESULTS;
        }

        return Math.min(requestedMaxResults, 3);
    }

    public long sanitizedCacheTtlMinutes() {
        if (cacheTtlMinutes <= 0) {
            return DEFAULT_CACHE_TTL_MINUTES;
        }

        return cacheTtlMinutes;
    }
}

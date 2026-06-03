package com.texttolearn.generation.service;

import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class GenerationJobRetryPolicy {

    private static final Duration[] BACKOFFS = {
            Duration.ofSeconds(15),
            Duration.ofSeconds(60),
            Duration.ofSeconds(180)
    };

    boolean shouldRetry(GenerationJobFailureDecision failure, int attemptCount, int maxAttempts) {
        return failure.retryable() && attemptCount < maxAttempts;
    }

    Duration backoffForAttempt(int attemptCount) {
        int index = Math.max(0, Math.min(attemptCount - 1, BACKOFFS.length - 1));
        return BACKOFFS[index];
    }
}

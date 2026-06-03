package com.texttolearn.generation.service;

import com.texttolearn.ai.error.AiGenerationException;
import com.texttolearn.common.error.ResourceNotFoundException;
import com.texttolearn.generation.model.GenerationJobErrorType;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Locale;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class GenerationJobFailureClassifier {

    GenerationJobFailureDecision classify(RuntimeException exception) {
        if (exception instanceof ResourceNotFoundException) {
            return permanent(GenerationJobErrorType.NOT_FOUND, exception);
        }

        if (exception instanceof AccessDeniedException) {
            return permanent(GenerationJobErrorType.AUTHORIZATION_ERROR, exception);
        }

        if (exception instanceof DataAccessException) {
            return retryable(GenerationJobErrorType.DATABASE_ERROR, exception);
        }

        RestClientResponseException responseException = findCause(exception, RestClientResponseException.class);
        if (responseException != null) {
            return classifyHttpResponse(responseException);
        }

        if (hasCause(exception, ResourceAccessException.class)
                || hasCause(exception, SocketTimeoutException.class)
                || hasCause(exception, ConnectException.class)) {
            return retryable(GenerationJobErrorType.AI_TIMEOUT, exception);
        }

        if (exception instanceof AiGenerationException) {
            return classifyAiException(exception);
        }

        return permanent(GenerationJobErrorType.UNKNOWN, exception);
    }

    private GenerationJobFailureDecision classifyHttpResponse(RestClientResponseException exception) {
        int status = exception.getStatusCode().value();
        if (status == 429) {
            return retryable(GenerationJobErrorType.AI_RATE_LIMIT, exception);
        }

        if (status == 408 || status == 502 || status == 503 || status == 504) {
            return retryable(GenerationJobErrorType.AI_TIMEOUT, exception);
        }

        if (status >= 500) {
            return retryable(GenerationJobErrorType.AI_TIMEOUT, exception);
        }

        if (status == 401 || status == 403) {
            return permanent(GenerationJobErrorType.AUTHORIZATION_ERROR, exception);
        }

        if (status == 400 || status == 422) {
            return permanent(GenerationJobErrorType.VALIDATION_ERROR, exception);
        }

        return permanent(GenerationJobErrorType.UNKNOWN, exception);
    }

    private GenerationJobFailureDecision classifyAiException(RuntimeException exception) {
        String message = safeMessage(exception).toLowerCase(Locale.ROOT);

        if (containsAny(message, "429", "rate limit", "quota", "resource_exhausted")) {
            return retryable(GenerationJobErrorType.AI_RATE_LIMIT, exception);
        }

        if (containsAny(message, "timeout", "timed out", "connection reset", "failed to generate")) {
            return retryable(GenerationJobErrorType.AI_TIMEOUT, exception);
        }

        if (containsAny(message, "api key", "unauthorized", "forbidden")) {
            return permanent(GenerationJobErrorType.AUTHORIZATION_ERROR, exception);
        }

        if (containsAny(message, "invalid", "unreadable", "empty", "missing", "without")) {
            return retryable(GenerationJobErrorType.AI_BAD_RESPONSE, exception);
        }

        return permanent(GenerationJobErrorType.UNKNOWN, exception);
    }

    private GenerationJobFailureDecision retryable(GenerationJobErrorType errorType, RuntimeException exception) {
        return new GenerationJobFailureDecision(errorType, true, safeMessage(exception));
    }

    private GenerationJobFailureDecision permanent(GenerationJobErrorType errorType, RuntimeException exception) {
        return new GenerationJobFailureDecision(errorType, false, safeMessage(exception));
    }

    private String safeMessage(RuntimeException exception) {
        if (exception.getMessage() == null || exception.getMessage().isBlank()) {
            return "Generation job failed.";
        }

        return exception.getMessage();
    }

    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> causeType) {
        return findCause(throwable, causeType) != null;
    }

    private <T extends Throwable> T findCause(Throwable throwable, Class<T> causeType) {
        Throwable current = throwable;
        while (current != null) {
            if (causeType.isInstance(current)) {
                return causeType.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }
}

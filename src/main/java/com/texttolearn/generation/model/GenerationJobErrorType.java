package com.texttolearn.generation.model;

public enum GenerationJobErrorType {
    AI_TIMEOUT,
    AI_RATE_LIMIT,
    AI_BAD_RESPONSE,
    VALIDATION_ERROR,
    AUTHORIZATION_ERROR,
    NOT_FOUND,
    DATABASE_ERROR,
    UNKNOWN
}

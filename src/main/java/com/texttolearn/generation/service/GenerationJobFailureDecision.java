package com.texttolearn.generation.service;

import com.texttolearn.generation.model.GenerationJobErrorType;

record GenerationJobFailureDecision(
        GenerationJobErrorType errorType,
        boolean retryable,
        String message
) {
}

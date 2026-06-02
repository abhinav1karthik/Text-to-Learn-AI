package com.texttolearn.generation.dto;

import com.texttolearn.generation.model.GenerationJobErrorType;
import com.texttolearn.generation.model.GenerationJobPriority;
import com.texttolearn.generation.model.GenerationJobStatus;
import com.texttolearn.generation.model.GenerationJobType;
import java.time.OffsetDateTime;
import java.util.UUID;

public record GenerationJobResponse(
        UUID id,
        GenerationJobType type,
        GenerationJobStatus status,
        GenerationJobPriority priority,
        String prompt,
        UUID courseId,
        UUID lessonId,
        String errorMessage,
        GenerationJobErrorType lastErrorType,
        int attemptCount,
        int maxAttempts,
        OffsetDateTime nextRunAt,
        OffsetDateTime lockedAt,
        String lockedBy,
        OffsetDateTime lastPublishedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt
) {
}

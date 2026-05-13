package com.texttolearn.generation.dto;

import com.texttolearn.generation.model.GenerationJobStatus;
import com.texttolearn.generation.model.GenerationJobType;
import java.time.OffsetDateTime;
import java.util.UUID;

public record GenerationJobResponse(
        UUID id,
        GenerationJobType type,
        GenerationJobStatus status,
        String prompt,
        UUID courseId,
        String errorMessage,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt
) {
}

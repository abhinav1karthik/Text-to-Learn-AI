package com.texttolearn.course.dto;

import com.texttolearn.course.model.CourseStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CourseResponse(
        UUID id,
        String prompt,
        String title,
        String description,
        List<String> tags,
        CourseStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<ModuleResponse> modules
) {
}

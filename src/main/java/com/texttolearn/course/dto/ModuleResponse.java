package com.texttolearn.course.dto;

import java.util.List;
import java.util.UUID;

public record ModuleResponse(
        UUID id,
        String title,
        String summary,
        int position,
        List<LessonSummaryResponse> lessons
) {
}

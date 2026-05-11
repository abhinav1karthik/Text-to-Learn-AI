package com.texttolearn.course.dto;

import com.texttolearn.course.model.LessonStatus;
import java.util.UUID;

public record LessonSummaryResponse(
        UUID id,
        String title,
        int position,
        LessonStatus status
) {
}

package com.texttolearn.course.dto;

import com.texttolearn.course.model.LessonStatus;
import com.texttolearn.generation.model.GenerationJobStatus;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record LessonResponse(
        UUID id,
        String title,
        int position,
        LessonStatus status,
        List<String> objectives,
        List<Map<String, Object>> content,
        UUID moduleId,
        String moduleTitle,
        UUID courseId,
        String courseTitle,
        UUID generationJobId,
        GenerationJobStatus generationJobStatus,
        String generationErrorMessage
) {
}

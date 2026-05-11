package com.texttolearn.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCourseRequest(
        @NotBlank(message = "Topic is required")
        @Size(max = 180, message = "Topic must be 180 characters or fewer")
        String topic
) {
}

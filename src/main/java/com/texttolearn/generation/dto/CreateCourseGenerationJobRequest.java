package com.texttolearn.generation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCourseGenerationJobRequest(
        @NotBlank(message = "Topic is required")
        @Size(max = 180, message = "Topic must be 180 characters or fewer")
        String topic
) {
}

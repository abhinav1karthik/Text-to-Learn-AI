package com.texttolearn.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeneratedLessonContent(
        String title,
        List<String> objectives,
        List<Map<String, Object>> content
) {
}

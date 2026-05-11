package com.texttolearn.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeneratedCourseOutline(
        String title,
        String description,
        List<String> tags,
        List<GeneratedModuleOutline> modules
) {
}

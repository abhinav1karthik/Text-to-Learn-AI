package com.texttolearn.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeneratedModuleOutline(
        String title,
        String summary,
        List<String> lessons
) {
}

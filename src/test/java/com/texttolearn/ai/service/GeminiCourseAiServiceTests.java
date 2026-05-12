package com.texttolearn.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.texttolearn.ai.config.GeminiProperties;
import org.junit.jupiter.api.Test;

class GeminiCourseAiServiceTests {

    private final GeminiCourseAiService service = new GeminiCourseAiService(
            new CoursePromptBuilder(),
            new ObjectMapper(),
            new GeminiProperties(
                    "test-key",
                    "https://generativelanguage.googleapis.com/v1beta",
                    "gemini-test",
                    "gemini-tts-test",
                    "Kore",
                    6_000
            )
    );

    @Test
    void extractsJsonObjectFromWrappedResponseText() {
        String extractedJson = service.extractJsonObject("""
                Here is the JSON:
                ```json
                {
                  "title": "Lesson",
                  "objectives": ["Understand"],
                  "content": []
                }
                ```
                """);

        assertThat(extractedJson).startsWith("{");
        assertThat(extractedJson).endsWith("}");
        assertThat(extractedJson).contains("\"title\": \"Lesson\"");
    }

    @Test
    void parsesLessonContentAfterExtractingWrappedJson() {
        var lesson = service.parseLessonContent("""
                ```json
                {
                  "title": "Range Queries",
                  "objectives": ["Understand range queries"],
                  "content": [
                    { "type": "heading", "text": "Range Queries" }
                  ]
                }
                ```
                """);

        assertThat(lesson.title()).isEqualTo("Range Queries");
        assertThat(lesson.objectives()).containsExactly("Understand range queries");
        assertThat(lesson.content()).hasSize(1);
    }

    @Test
    void createsFallbackLessonWhenGenerationCannotProduceValidJson() {
        var fallback = service.fallbackLessonContent(
                "Segment Trees",
                "Range Query Basics",
                "Range Sum Query",
                new RuntimeException("invalid json")
        );

        assertThat(fallback.title()).isEqualTo("Range Sum Query");
        assertThat(fallback.objectives()).hasSize(3);
        assertThat(fallback.content()).extracting(block -> block.get("type"))
                .contains("heading", "paragraph", "video", "mcq");
    }
}

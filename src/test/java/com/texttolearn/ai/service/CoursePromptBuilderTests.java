package com.texttolearn.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CoursePromptBuilderTests {

    private final CoursePromptBuilder coursePromptBuilder = new CoursePromptBuilder();

    @Test
    void generateCoursePromptDefinesStrictCourseOutlineJsonContract() {
        String prompt = coursePromptBuilder.generateCoursePrompt("Segment Trees and Its Applications");

        assertThat(prompt).contains("Segment Trees and Its Applications");
        assertThat(prompt).contains("Return raw JSON only");
        assertThat(prompt).contains("\"title\"");
        assertThat(prompt).contains("\"description\"");
        assertThat(prompt).contains("\"tags\"");
        assertThat(prompt).contains("\"modules\"");
        assertThat(prompt).contains("\"summary\"");
        assertThat(prompt).contains("\"lessons\"");
        assertThat(prompt).contains("Create 4 to 7 modules");
        assertThat(prompt).contains("Do not generate lesson content yet");
    }

    @Test
    void generateLessonPromptDefinesStrictLessonContentJsonContract() {
        String prompt = coursePromptBuilder.generateLessonPrompt(
                "Segment Trees and Applications",
                "Range Query Foundations",
                "Building a Segment Tree"
        );

        assertThat(prompt).contains("Segment Trees and Applications");
        assertThat(prompt).contains("Range Query Foundations");
        assertThat(prompt).contains("Building a Segment Tree");
        assertThat(prompt).contains("Return raw JSON only");
        assertThat(prompt).contains("\"objectives\"");
        assertThat(prompt).contains("\"content\"");
        assertThat(prompt).contains("\"type\": \"heading\"");
        assertThat(prompt).contains("\"type\": \"paragraph\"");
        assertThat(prompt).contains("\"type\": \"code\"");
        assertThat(prompt).contains("\"type\": \"video\"");
        assertThat(prompt).contains("\"query\"");
        assertThat(prompt).contains("\"maxResults\": 1");
        assertThat(prompt).contains("Do not place all video blocks at the end");
        assertThat(prompt).contains("Do not place video blocks consecutively");
        assertThat(prompt).contains("Place code blocks immediately after the explanation they support");
        assertThat(prompt).contains("Do not include direct video links or URLs");
        assertThat(prompt).contains("Add 4 or 5 MCQ blocks at the very end");
        assertThat(prompt).contains("\"explanation\"");
        assertThat(prompt).contains("zero-based index");
    }

    @Test
    void rejectsBlankPromptInputs() {
        assertThatThrownBy(() -> coursePromptBuilder.generateCoursePrompt(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("topic must not be blank");

        assertThatThrownBy(() -> coursePromptBuilder.generateLessonPrompt("Course", "", "Lesson"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("moduleTitle must not be blank");
    }
}

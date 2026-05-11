package com.texttolearn.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.texttolearn.ai.config.GeminiProperties;
import com.texttolearn.ai.dto.GeneratedCourseOutline;
import com.texttolearn.ai.dto.GeneratedLessonContent;
import com.texttolearn.ai.error.AiGenerationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
@ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "gemini", matchIfMissing = true)
public class GeminiCourseAiService implements CourseAiService {

    private static final int COURSE_MAX_OUTPUT_TOKENS = 5000;
    private static final int LESSON_MAX_OUTPUT_TOKENS = 12000;
    private static final int LESSON_GENERATION_ATTEMPTS = 3;

    private final CoursePromptBuilder coursePromptBuilder;
    private final ObjectMapper objectMapper;
    private final GeminiProperties geminiProperties;
    private final RestClient restClient;

    public GeminiCourseAiService(
            CoursePromptBuilder coursePromptBuilder,
            ObjectMapper objectMapper,
            GeminiProperties geminiProperties
    ) {
        this.coursePromptBuilder = coursePromptBuilder;
        this.objectMapper = objectMapper;
        this.geminiProperties = geminiProperties;
        this.restClient = RestClient.builder()
                .baseUrl(geminiProperties.baseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public GeneratedCourseOutline generateCourseOutline(String topic) {
        if (!geminiProperties.isConfigured()) {
            throw new AiGenerationException("Gemini API key is not configured. Set GEMINI_API_KEY.");
        }

        String prompt = coursePromptBuilder.generateCoursePrompt(topic);
        String generatedJson = callGenerateContentApi(
                prompt,
                "course outline",
                courseOutlineSchema(),
                COURSE_MAX_OUTPUT_TOKENS
        );
        return parseCourseOutline(generatedJson);
    }

    @Override
    public GeneratedLessonContent generateLessonContent(String courseTitle, String moduleTitle, String lessonTitle) {
        if (!geminiProperties.isConfigured()) {
            throw new AiGenerationException("Gemini API key is not configured. Set GEMINI_API_KEY.");
        }

        String prompt = coursePromptBuilder.generateLessonPrompt(courseTitle, moduleTitle, lessonTitle);
        AiGenerationException lastException = null;

        for (int attempt = 1; attempt <= LESSON_GENERATION_ATTEMPTS; attempt++) {
            try {
                String generatedJson = callGenerateContentApi(
                        retryPrompt(prompt, attempt),
                        "lesson content",
                        lessonContentSchema(),
                        LESSON_MAX_OUTPUT_TOKENS
                );
                return parseLessonContent(generatedJson);
            } catch (AiGenerationException exception) {
                if (!isInvalidLessonJson(exception)) {
                    throw exception;
                }
                lastException = exception;
            }
        }

        return fallbackLessonContent(courseTitle, moduleTitle, lessonTitle, lastException);
    }

    private String retryPrompt(String prompt, int attempt) {
        if (attempt == 1) {
            return prompt;
        }

        return prompt + """

                Previous attempt returned invalid JSON.
                Regenerate the lesson as one complete, valid JSON object.
                Keep the lesson concise enough to finish the JSON.
                Escape all newlines inside code strings as \\n.
                Do not include Markdown, comments, or text outside the JSON object.
                """;
    }

    private String callGenerateContentApi(
            String prompt,
            String responseDescription,
            Map<String, Object> responseSchema,
            int maxOutputTokens
    ) {
        Map<String, Object> textPart = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(textPart));

        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("temperature", 0.2);
        generationConfig.put("maxOutputTokens", maxOutputTokens);
        generationConfig.put("responseMimeType", MediaType.APPLICATION_JSON_VALUE);
        generationConfig.put("responseSchema", responseSchema);

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("contents", List.of(content));
        requestBody.put("generationConfig", generationConfig);

        try {
            String responseBody = restClient.post()
                    .uri("/models/{model}:generateContent", geminiProperties.model())
                    .header("x-goog-api-key", geminiProperties.apiKey())
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            String outputText = extractOutputText(parseResponseBody(responseBody));
            if (outputText == null || outputText.isBlank()) {
                throw new AiGenerationException("Gemini returned empty " + responseDescription + ".");
            }

            return stripMarkdownCodeFence(outputText);
        } catch (RestClientResponseException exception) {
            throw new AiGenerationException(
                    "Gemini request failed with status " + exception.getStatusCode().value()
                            + ": " + safeGeminiErrorBody(exception),
                    exception
            );
        } catch (RestClientException exception) {
            throw new AiGenerationException("Failed to generate course outline from Gemini.", exception);
        }
    }

    private Map<String, Object> courseOutlineSchema() {
        Map<String, Object> moduleSchema = objectSchema(
                Map.of(
                        "title", stringSchema(),
                        "summary", stringSchema(),
                        "lessons", arraySchema(stringSchema())
                ),
                List.of("title", "summary", "lessons"),
                List.of("title", "summary", "lessons")
        );

        return objectSchema(
                Map.of(
                        "title", stringSchema(),
                        "description", stringSchema(),
                        "tags", arraySchema(stringSchema()),
                        "modules", arraySchema(moduleSchema)
                ),
                List.of("title", "description", "tags", "modules"),
                List.of("title", "description", "tags", "modules")
        );
    }

    private Map<String, Object> lessonContentSchema() {
        Map<String, Object> contentBlockSchema = objectSchema(
                Map.of(
                        "type", enumStringSchema(List.of("heading", "paragraph", "code", "video", "mcq")),
                        "text", stringSchema("Required for heading, paragraph, and code blocks. For code blocks, this must contain the complete source code."),
                        "language", stringSchema("Programming language for code blocks, such as java."),
                        "title", stringSchema("Title for video blocks."),
                        "query", stringSchema("YouTube search query for video blocks. Do not return a URL."),
                        "maxResults", integerSchema(),
                        "question", stringSchema("Question text for MCQ blocks."),
                        "options", arraySchema(stringSchema()),
                        "answer", integerSchema(),
                        "explanation", stringSchema("Explanation for the correct MCQ answer.")
                ),
                List.of("type"),
                List.of(
                        "type", "text", "language", "title", "query", "maxResults",
                        "question", "options", "answer", "explanation"
                )
        );

        return objectSchema(
                Map.of(
                        "title", stringSchema(),
                        "objectives", arraySchema(stringSchema()),
                        "content", arraySchema(contentBlockSchema)
                ),
                List.of("title", "objectives", "content"),
                List.of("title", "objectives", "content")
        );
    }

    private Map<String, Object> objectSchema(
            Map<String, Object> properties,
            List<String> required,
            List<String> propertyOrdering
    ) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "OBJECT");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("propertyOrdering", propertyOrdering);
        return schema;
    }

    private Map<String, Object> arraySchema(Map<String, Object> items) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "ARRAY");
        schema.put("items", items);
        return schema;
    }

    private Map<String, Object> stringSchema() {
        return Map.of("type", "STRING");
    }

    private Map<String, Object> stringSchema(String description) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "STRING");
        schema.put("description", description);
        return schema;
    }

    private Map<String, Object> enumStringSchema(List<String> values) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "STRING");
        schema.put("enum", values);
        return schema;
    }

    private Map<String, Object> integerSchema() {
        return Map.of("type", "INTEGER");
    }

    private String safeGeminiErrorBody(RestClientResponseException exception) {
        String responseBody = exception.getResponseBodyAsString();
        if (responseBody == null || responseBody.isBlank()) {
            return exception.getStatusText();
        }

        return responseBody;
    }

    private GeneratedCourseOutline parseCourseOutline(String generatedJson) {
        try {
            return objectMapper.readValue(extractJsonObject(generatedJson), GeneratedCourseOutline.class);
        } catch (JsonProcessingException exception) {
            throw new AiGenerationException("Gemini returned invalid course outline JSON.", exception);
        }
    }

    GeneratedLessonContent parseLessonContent(String generatedJson) {
        try {
            return objectMapper.readValue(extractJsonObject(generatedJson), GeneratedLessonContent.class);
        } catch (JsonProcessingException exception) {
            throw new AiGenerationException("Gemini returned invalid lesson content JSON.", exception);
        }
    }

    private boolean isInvalidLessonJson(AiGenerationException exception) {
        return exception.getMessage() != null
                && exception.getMessage().contains("invalid lesson content JSON");
    }

    private JsonNode parseResponseBody(String responseBody) {
        try {
            return objectMapper.readTree(responseBody);
        } catch (JsonProcessingException exception) {
            throw new AiGenerationException("Gemini returned an unreadable response.", exception);
        }
    }

    private String extractOutputText(JsonNode response) {
        if (response == null) {
            return null;
        }

        JsonNode candidates = response.get("candidates");
        if (candidates == null || !candidates.isArray() || candidates.isEmpty()) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        for (JsonNode candidate : candidates) {
            JsonNode parts = candidate.path("content").path("parts");
            if (!parts.isArray()) {
                continue;
            }

            for (JsonNode part : parts) {
                JsonNode text = part.get("text");
                if (text != null && text.isTextual()) {
                    builder.append(text.asText());
                }
            }
        }

        return builder.toString();
    }

    private String stripMarkdownCodeFence(String value) {
        String trimmed = value.trim();

        if (trimmed.startsWith("```json") && trimmed.endsWith("```")) {
            return trimmed.substring(7, trimmed.length() - 3).trim();
        }

        if (trimmed.startsWith("```") && trimmed.endsWith("```")) {
            return trimmed.substring(3, trimmed.length() - 3).trim();
        }

        return trimmed;
    }

    String extractJsonObject(String value) {
        String trimmed = stripMarkdownCodeFence(value);
        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');

        if (firstBrace == -1 || lastBrace == -1 || lastBrace <= firstBrace) {
            return trimmed;
        }

        return trimmed.substring(firstBrace, lastBrace + 1);
    }

    GeneratedLessonContent fallbackLessonContent(
            String courseTitle,
            String moduleTitle,
            String lessonTitle,
            Throwable cause
    ) {
        return new GeneratedLessonContent(
                lessonTitle,
                List.of(
                        "Understand the main idea of " + lessonTitle,
                        "Connect this lesson to " + moduleTitle,
                        "Identify what to review or practice next"
                ),
                List.of(
                        Map.of("type", "heading", "text", lessonTitle),
                        Map.of(
                                "type", "paragraph",
                                "text",
                                "This lesson belongs to " + courseTitle + ". A full detailed version could not "
                                        + "be prepared right now, so this page provides a stable starter explanation "
                                        + "instead of leaving the lesson unavailable."
                        ),
                        Map.of(
                                "type", "paragraph",
                                "text",
                                "Review the lesson title, relate it to the current module, and use the course "
                                        + "outline to continue learning while this lesson is regenerated later."
                        ),
                        Map.of(
                                "type", "video",
                                "title", lessonTitle + " overview",
                                "query", courseTitle + " " + moduleTitle + " " + lessonTitle + " tutorial",
                                "maxResults", 1
                        ),
                        Map.of(
                                "type", "mcq",
                                "question", "What is the best next step for this lesson?",
                                "options", List.of(
                                        "Skip the whole course",
                                        "Review the topic and continue through the module",
                                        "Ignore the module context",
                                        "Delete the course outline"
                                ),
                                "answer", 1,
                                "explanation", "The useful path is to keep the lesson connected to its module and continue learning."
                        )
                )
        );
    }
}

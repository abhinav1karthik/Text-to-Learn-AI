package com.texttolearn.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.texttolearn.ai.config.OpenAiProperties;
import com.texttolearn.ai.dto.GeneratedCourseOutline;
import com.texttolearn.ai.dto.GeneratedLessonContent;
import com.texttolearn.ai.error.AiGenerationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
@ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "openai")
public class OpenAiCourseAiService implements CourseAiService {

    private final CoursePromptBuilder coursePromptBuilder;
    private final ObjectMapper objectMapper;
    private final OpenAiProperties openAiProperties;
    private final RestClient restClient;

    public OpenAiCourseAiService(
            CoursePromptBuilder coursePromptBuilder,
            ObjectMapper objectMapper,
            OpenAiProperties openAiProperties
    ) {
        this.coursePromptBuilder = coursePromptBuilder;
        this.objectMapper = objectMapper;
        this.openAiProperties = openAiProperties;
        this.restClient = RestClient.builder()
                .baseUrl(openAiProperties.baseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public GeneratedCourseOutline generateCourseOutline(String topic) {
        if (!openAiProperties.isConfigured()) {
            throw new AiGenerationException("OpenAI API key is not configured. Set OPENAI_API_KEY.");
        }

        String prompt = coursePromptBuilder.generateCoursePrompt(topic);
        String generatedJson = callResponsesApi(prompt, "course outline");
        return parseCourseOutline(generatedJson);
    }

    @Override
    public GeneratedLessonContent generateLessonContent(String courseTitle, String moduleTitle, String lessonTitle) {
        if (!openAiProperties.isConfigured()) {
            throw new AiGenerationException("OpenAI API key is not configured. Set OPENAI_API_KEY.");
        }

        String prompt = coursePromptBuilder.generateLessonPrompt(courseTitle, moduleTitle, lessonTitle);
        String generatedJson = callResponsesApi(prompt, "lesson content");
        return parseLessonContent(generatedJson);
    }

    private String callResponsesApi(String prompt, String responseDescription) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", openAiProperties.model());
        requestBody.put("input", prompt);
        requestBody.put("max_output_tokens", 5000);

        try {
            String responseBody = restClient.post()
                    .uri("/responses")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiProperties.apiKey())
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            String outputText = extractOutputText(parseResponseBody(responseBody));
            if (outputText == null || outputText.isBlank()) {
                throw new AiGenerationException("OpenAI returned empty " + responseDescription + ".");
            }

            return stripMarkdownCodeFence(outputText);
        } catch (RestClientResponseException exception) {
            throw new AiGenerationException(
                    "OpenAI request failed with status " + exception.getStatusCode().value()
                            + ": " + safeOpenAiErrorBody(exception),
                    exception
            );
        } catch (RestClientException exception) {
            throw new AiGenerationException("Failed to generate course outline from OpenAI.", exception);
        }
    }

    private String safeOpenAiErrorBody(RestClientResponseException exception) {
        String responseBody = exception.getResponseBodyAsString();
        if (responseBody == null || responseBody.isBlank()) {
            return exception.getStatusText();
        }

        return responseBody;
    }

    private GeneratedCourseOutline parseCourseOutline(String generatedJson) {
        try {
            return objectMapper.readValue(generatedJson, GeneratedCourseOutline.class);
        } catch (JsonProcessingException exception) {
            throw new AiGenerationException("OpenAI returned invalid course outline JSON.", exception);
        }
    }

    private GeneratedLessonContent parseLessonContent(String generatedJson) {
        try {
            return objectMapper.readValue(generatedJson, GeneratedLessonContent.class);
        } catch (JsonProcessingException exception) {
            throw new AiGenerationException("OpenAI returned invalid lesson content JSON.", exception);
        }
    }

    private JsonNode parseResponseBody(String responseBody) {
        try {
            return objectMapper.readTree(responseBody);
        } catch (JsonProcessingException exception) {
            throw new AiGenerationException("OpenAI returned an unreadable response.", exception);
        }
    }

    private String extractOutputText(JsonNode response) {
        if (response == null) {
            return null;
        }

        JsonNode outputText = response.get("output_text");
        if (outputText != null && outputText.isTextual()) {
            return outputText.asText();
        }

        JsonNode output = response.get("output");
        if (output == null || !output.isArray()) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        for (JsonNode outputItem : output) {
            JsonNode content = outputItem.get("content");
            if (content == null || !content.isArray()) {
                continue;
            }

            for (JsonNode contentItem : content) {
                JsonNode text = contentItem.get("text");
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
}

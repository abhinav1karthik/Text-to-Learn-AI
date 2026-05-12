package com.texttolearn.audio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.texttolearn.ai.config.GeminiProperties;
import com.texttolearn.audio.storage.AudioObjectStorageService;
import com.texttolearn.course.dto.LessonResponse;
import com.texttolearn.course.model.LessonStatus;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class LessonAudioServiceTests {

    private final LessonAudioService service = new LessonAudioService(
            new ObjectMapper(),
            new GeminiProperties(
                    "test-key",
                    "https://generativelanguage.googleapis.com/v1beta",
                    "gemini-test",
                    "gemini-tts-test",
                    "Kore",
                    6_000
            ),
            null,
            null,
            new DisabledAudioObjectStorageService(),
            RestClient.builder()
    );

    @Test
    void buildsLessonSourceTextFromStructuredLessonBlocks() {
        LessonResponse lesson = lessonResponse(List.of(
                Map.of("type", "heading", "text", "Range Sum Query"),
                Map.of("type", "paragraph", "text", "Segment trees answer range queries quickly."),
                Map.of("type", "code", "language", "java", "text", "class SegmentTree {}"),
                Map.of("type", "video", "query", "segment tree range sum tutorial"),
                Map.of("type", "mcq", "question", "Why use a segment tree?")
        ));

        String sourceText = service.buildLessonSourceText(lesson);

        assertThat(sourceText).contains("Course: Segment Trees");
        assertThat(sourceText).contains("Module: Range Queries");
        assertThat(sourceText).contains("Lesson: Range Sum Query");
        assertThat(sourceText).contains("Code example in java is included");
        assertThat(sourceText).contains("Related video topic: segment tree range sum tutorial");
        assertThat(sourceText).contains("Practice question: Why use a segment tree?");
    }

    @Test
    void wrapsPcmAudioDataInWaveContainer() {
        byte[] wav = LessonAudioService.toWav(new byte[] {1, 2, 3, 4}, 24_000, 1, 16);

        assertThat(new String(wav, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("RIFF");
        assertThat(new String(wav, 8, 4, StandardCharsets.US_ASCII)).isEqualTo("WAVE");
        assertThat(new String(wav, 36, 4, StandardCharsets.US_ASCII)).isEqualTo("data");
        assertThat(wav).hasSize(48);
    }

    @Test
    void extractsInlineAudioDataFromGeminiResponse() {
        String encodedAudio = Base64.getEncoder().encodeToString(new byte[] {10, 20});
        String responseBody = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "inlineData": {
                              "mimeType": "audio/L16;codec=pcm;rate=24000",
                              "data": "%s"
                            }
                          }
                        ]
                      }
                    }
                  ]
                }
                """.formatted(encodedAudio);

        LessonAudioService.InlineAudioData audioData = service.extractInlineAudioData(responseBody);

        assertThat(audioData.audio()).containsExactly(10, 20);
        assertThat(audioData.mimeType()).isEqualTo("audio/L16;codec=pcm;rate=24000");
    }

    @Test
    void rejectsInvalidInlineAudioDataWithAudioGenerationError() {
        String responseBody = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "inlineData": {
                              "mimeType": "audio/L16;codec=pcm;rate=24000",
                              "data": "@@@"
                            }
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        assertThatThrownBy(() -> service.extractInlineAudioData(responseBody))
                .isInstanceOf(com.texttolearn.audio.error.AudioGenerationException.class)
                .hasMessage("Gemini returned empty audio data.");
    }

    @Test
    void fallsBackToConfiguredVoiceWhenVoiceNameIsUnsafe() {
        assertThat(service.sanitizeVoiceName("../bad")).isEqualTo("Kore");
        assertThat(service.sanitizeVoiceName("Puck")).isEqualTo("Puck");
    }

    @Test
    void shortensTranscriptAfterFirstTtsAttempt() {
        LessonResponse lesson = lessonResponse(List.of());
        String longTranscript = "This is a sentence. ".repeat(200);

        assertThat(service.transcriptForAttempt(longTranscript, lesson, 1)).isEqualTo(longTranscript);
        assertThat(service.transcriptForAttempt(longTranscript, lesson, 2))
                .hasSizeLessThan(longTranscript.length())
                .contains("Quick recap");
        assertThat(service.transcriptForAttempt(longTranscript, lesson, 2))
                .hasSizeGreaterThan(2_000);
        assertThat(service.transcriptForAttempt(longTranscript, lesson, 3))
                .hasSizeLessThan(longTranscript.length())
                .contains("Quick recap");
    }

    @Test
    void resolvesSupportedLanguagesAndFallsBackToHinglish() {
        assertThat(service.languageOption("telugu").code()).isEqualTo("telugu");
        assertThat(service.languageOption("spanish").spokenName()).isEqualTo("Spanish");
        assertThat(service.languageOption("unknown-language").code()).isEqualTo("hinglish");
    }

    @Test
    void parsesPlainTranscriptTextFromGeminiResponse() {
        String responseBody = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "Namaste! Aaj hum rhythm ko simple Hinglish mein samjhenge."
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        assertThat(service.parseTranscript(responseBody))
                .isEqualTo("Namaste! Aaj hum rhythm ko simple Hinglish mein samjhenge.");
    }

    @Test
    void stillParsesJsonTranscriptIfGeminiReturnsJson() {
        String responseBody = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"transcript\\":\\"Aaj ka lesson simple tarike se samjhte hain.\\"}"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        assertThat(service.parseTranscript(responseBody))
                .isEqualTo("Aaj ka lesson simple tarike se samjhte hain.");
    }


    private LessonResponse lessonResponse(List<Map<String, Object>> content) {
        return new LessonResponse(
                UUID.randomUUID(),
                "Range Sum Query",
                1,
                LessonStatus.GENERATED,
                List.of("Understand range sums"),
                content,
                UUID.randomUUID(),
                "Range Queries",
                UUID.randomUUID(),
                "Segment Trees"
        );
    }

    private static class DisabledAudioObjectStorageService implements AudioObjectStorageService {

        @Override
        public boolean isConfigured() {
            return false;
        }

        @Override
        public byte[] get(String storageKey) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void put(String storageKey, byte[] audio, String contentType) {
            throw new UnsupportedOperationException();
        }
    }
}

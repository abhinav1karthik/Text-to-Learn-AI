package com.texttolearn.audio.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.texttolearn.ai.config.GeminiProperties;
import com.texttolearn.audio.dto.LessonAudioResponse;
import com.texttolearn.audio.error.AudioGenerationException;
import com.texttolearn.audio.model.LessonAudio;
import com.texttolearn.audio.repository.LessonAudioRepository;
import com.texttolearn.audio.storage.AudioObjectStorageService;
import com.texttolearn.course.dto.LessonResponse;
import com.texttolearn.course.model.Lesson;
import com.texttolearn.course.repository.LessonRepository;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class LessonAudioService {

    private static final String AUDIO_CONTENT_TYPE = "audio/wav";
    private static final String DEFAULT_AUDIO_LANGUAGE = "hinglish";
    private static final String STORAGE_PROVIDER = "r2";
    private static final int DEFAULT_PCM_SAMPLE_RATE = 24_000;
    private static final int DEFAULT_PCM_CHANNELS = 1;
    private static final int DEFAULT_PCM_BITS_PER_SAMPLE = 16;
    private static final int TRANSCRIPT_MAX_OUTPUT_TOKENS = 4_000;
    private static final int MIN_TRANSCRIPT_CHARACTERS = 1_000;
    private static final int TTS_GENERATION_ATTEMPTS = 3;
    private static final int TTS_SHORT_TRANSCRIPT_CHARACTERS = 2_600;
    private static final Pattern MIME_RATE_PATTERN = Pattern.compile("rate=(\\d+)");
    private static final Pattern SAFE_VOICE_NAME_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9_-]{1,40}");
    private static final Map<String, AudioLanguage> SUPPORTED_LANGUAGES = supportedLanguages();

    private final GeminiProperties geminiProperties;
    private final LessonAudioRepository lessonAudioRepository;
    private final LessonRepository lessonRepository;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final AudioObjectStorageService audioObjectStorageService;
    private final Map<String, LessonAudioResponse> audioCache = new ConcurrentHashMap<>();

    @Autowired
    public LessonAudioService(
            ObjectMapper objectMapper,
            GeminiProperties geminiProperties,
            LessonAudioRepository lessonAudioRepository,
            LessonRepository lessonRepository,
            AudioObjectStorageService audioObjectStorageService
    ) {
        this(
                objectMapper,
                geminiProperties,
                lessonAudioRepository,
                lessonRepository,
                audioObjectStorageService,
                RestClient.builder()
        );
    }

    LessonAudioService(
            ObjectMapper objectMapper,
            GeminiProperties geminiProperties,
            LessonAudioRepository lessonAudioRepository,
            LessonRepository lessonRepository,
            AudioObjectStorageService audioObjectStorageService,
            RestClient.Builder restClientBuilder
    ) {
        this.geminiProperties = geminiProperties;
        this.lessonAudioRepository = lessonAudioRepository;
        this.lessonRepository = lessonRepository;
        this.objectMapper = objectMapper;
        this.audioObjectStorageService = audioObjectStorageService;
        this.restClient = restClientBuilder
                .baseUrl(geminiProperties.baseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public LessonAudioResponse generateHinglishAudio(LessonResponse lesson, String voiceName) {
        return generateAudio(lesson, voiceName, DEFAULT_AUDIO_LANGUAGE);
    }

    public LessonAudioResponse generateAudio(LessonResponse lesson, String voiceName, String language) {
        if (!geminiProperties.isConfigured()) {
            throw new AudioGenerationException("Gemini API key is not configured. Set GEMINI_API_KEY.");
        }

        String sanitizedVoiceName = sanitizeVoiceName(voiceName);
        AudioLanguage audioLanguage = languageOption(language);
        String lessonSourceText = buildLessonSourceText(lesson);
        String cacheKey = cacheKey(lesson.id(), sanitizedVoiceName, audioLanguage.code(), lessonSourceText);

        if (audioObjectStorageService.isConfigured()) {
            LessonAudioResponse savedAudio = findSavedAudio(lesson, sanitizedVoiceName, audioLanguage.code());
            if (savedAudio != null) {
                return savedAudio;
            }
        }

        LessonAudioResponse cachedAudio = audioCache.get(cacheKey);
        if (cachedAudio != null) {
            return cachedAudio;
        }

        String transcript = generateTranscript(lesson, lessonSourceText, audioLanguage);
        InlineAudioData inlineAudioData = generateSpeechWithRetries(transcript, lesson, sanitizedVoiceName, audioLanguage);
        byte[] audio = wavAudio(inlineAudioData);

        LessonAudioResponse response = new LessonAudioResponse(
                audio,
                safeFileName(lesson.title()) + "-" + audioLanguage.code() + ".wav",
                AUDIO_CONTENT_TYPE
        );
        audioCache.put(cacheKey, response);
        if (audioObjectStorageService.isConfigured()) {
            saveGeneratedAudio(lesson, sanitizedVoiceName, audioLanguage.code(), response);
        }
        return response;
    }

    private LessonAudioResponse findSavedAudio(LessonResponse lesson, String voiceName, String language) {
        return lessonAudioRepository.findByLessonIdAndLanguageAndVoiceName(
                        lesson.id(),
                        language,
                        voiceName
                )
                .map(savedAudio -> new LessonAudioResponse(
                        audioObjectStorageService.get(savedAudio.getStorageKey()),
                        savedAudio.getFileName(),
                        savedAudio.getContentType()
                ))
                .orElse(null);
    }

    private void saveGeneratedAudio(
            LessonResponse lessonResponse,
            String voiceName,
            String language,
            LessonAudioResponse response
    ) {
        String storageKey = storageKey(lessonResponse, voiceName, language);
        audioObjectStorageService.put(storageKey, response.audio(), response.contentType());

        Lesson lesson = lessonRepository.getReferenceById(lessonResponse.id());
        LessonAudio lessonAudio = lessonAudioRepository.findByLessonIdAndLanguageAndVoiceName(
                        lessonResponse.id(),
                        language,
                        voiceName
                )
                .orElseGet(() -> new LessonAudio(
                        lesson,
                        language,
                        voiceName,
                        STORAGE_PROVIDER,
                        storageKey,
                        response.contentType(),
                        response.fileName(),
                        response.audio().length
                ));

        lessonAudio.replaceStoredObject(
                storageKey,
                response.contentType(),
                response.fileName(),
                response.audio().length
        );
        lessonAudioRepository.save(lessonAudio);
    }

    String buildLessonSourceText(LessonResponse lesson) {
        StringBuilder builder = new StringBuilder();
        appendLine(builder, "Course: " + lesson.courseTitle());
        appendLine(builder, "Module: " + lesson.moduleTitle());
        appendLine(builder, "Lesson: " + lesson.title());

        if (lesson.objectives() != null && !lesson.objectives().isEmpty()) {
            appendLine(builder, "Objectives:");
            lesson.objectives().forEach(objective -> appendLine(builder, "- " + objective));
        }

        appendLine(builder, "Lesson content:");
        if (lesson.content() != null) {
            lesson.content().forEach(block -> appendContentBlock(builder, block));
        }

        String sourceText = builder.toString().trim();
        int maxCharacters = geminiProperties.sanitizedAudioMaxInputCharacters();
        if (sourceText.length() <= maxCharacters) {
            return sourceText;
        }

        return sourceText.substring(0, maxCharacters).trim()
                + "\n\n[Lesson text was shortened for audio generation.]";
    }

    String sanitizeVoiceName(String voiceName) {
        String requestedVoiceName = voiceName == null || voiceName.isBlank()
                ? geminiProperties.effectiveTtsVoiceName()
                : voiceName.trim();

        if (!SAFE_VOICE_NAME_PATTERN.matcher(requestedVoiceName).matches()) {
            return geminiProperties.effectiveTtsVoiceName();
        }

        return requestedVoiceName;
    }

    InlineAudioData extractInlineAudioData(String responseBody) {
        JsonNode root = readResponseBody(responseBody, "Gemini returned unreadable TTS response.");
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            throw new AudioGenerationException("Gemini returned no audio candidates.");
        }

        for (JsonNode candidate : candidates) {
            JsonNode parts = candidate.path("content").path("parts");
            if (!parts.isArray()) {
                continue;
            }

            for (JsonNode part : parts) {
                JsonNode inlineData = part.path("inlineData");
                if (inlineData.isMissingNode()) {
                    inlineData = part.path("inline_data");
                }

                String encodedAudio = textAt(inlineData, "data");
                if (encodedAudio != null && !encodedAudio.isBlank()) {
                    try {
                        byte[] decodedAudio = Base64.getMimeDecoder().decode(encodedAudio);
                        if (decodedAudio.length == 0) {
                            throw new AudioGenerationException("Gemini returned empty audio data.");
                        }
                        return new InlineAudioData(
                                decodedAudio,
                                textAt(inlineData, "mimeType")
                        );
                    } catch (AudioGenerationException exception) {
                        throw exception;
                    } catch (IllegalArgumentException exception) {
                        throw new AudioGenerationException("Gemini returned invalid audio data.", exception);
                    }
                }
            }
        }

        throw new AudioGenerationException("Gemini returned an audio response without audio data.");
    }

    static byte[] toWav(byte[] pcmData, int sampleRate, int channels, int bitsPerSample) {
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;
        int dataSize = pcmData.length;

        ByteBuffer buffer = ByteBuffer
                .allocate(44 + dataSize)
                .order(ByteOrder.LITTLE_ENDIAN);
        buffer.put("RIFF".getBytes(StandardCharsets.US_ASCII));
        buffer.putInt(36 + dataSize);
        buffer.put("WAVE".getBytes(StandardCharsets.US_ASCII));
        buffer.put("fmt ".getBytes(StandardCharsets.US_ASCII));
        buffer.putInt(16);
        buffer.putShort((short) 1);
        buffer.putShort((short) channels);
        buffer.putInt(sampleRate);
        buffer.putInt(byteRate);
        buffer.putShort((short) blockAlign);
        buffer.putShort((short) bitsPerSample);
        buffer.put("data".getBytes(StandardCharsets.US_ASCII));
        buffer.putInt(dataSize);
        buffer.put(pcmData);
        return buffer.array();
    }

    private void appendContentBlock(StringBuilder builder, Map<String, Object> block) {
        if (block == null) {
            return;
        }

        String type = stringValue(block.get("type")).toLowerCase(Locale.ROOT);
        switch (type) {
            case "heading" -> appendLine(builder, "Heading: " + stringValue(block.get("text")));
            case "paragraph" -> appendLine(builder, stringValue(block.get("text")));
            case "code" -> appendLine(
                    builder,
                    "Code example in " + stringValue(block.get("language"))
                            + " is included. Summarize what it does, but do not read it character by character: "
                            + abbreviated(stringValue(block.get("text")), 1_000)
            );
            case "video" -> appendLine(builder, "Related video topic: " + stringValue(block.get("query")));
            case "mcq" -> appendLine(builder, "Practice question: " + stringValue(block.get("question")));
            default -> appendLine(builder, stringValue(block.get("text")));
        }
    }

    private String generateTranscript(LessonResponse lesson, String lessonSourceText, AudioLanguage audioLanguage) {
        String prompt = """
                Create a complete audio teaching script for a student.

                Rules:
                - Language requirement: Explain in %s.
                - Language style: %s
                - Keep technical words such as API, segment tree, recursion, database, query, and code in English.
                - Explain like a calm teacher.
                - Cover the full lesson, not only the introduction.
                - Follow this structure: quick intro, all key concepts, examples or intuition, common mistakes, quick recap.
                - Include at least 8 to 12 substantial sentences.
                - Do not read code character by character. Summarize what code does.
                - Target around 2 to 3 minutes of spoken audio.
                - Do not mention that this was translated or generated.
                - Return only the script text.
                - Do not return JSON.
                - Do not use Markdown formatting.

                Lesson title:
                %s

                English lesson content:
                %s
                """.formatted(audioLanguage.spokenName(), audioLanguage.styleNote(), lesson.title(), lessonSourceText);

        String transcript = parseTranscript(callGenerateContent(
                geminiProperties.model(),
                prompt,
                transcriptGenerationConfig()
        ));

        if (transcript.length() < MIN_TRANSCRIPT_CHARACTERS) {
            transcript = expandShortTranscript(lesson, lessonSourceText, transcript, audioLanguage);
        }

        return transcript;
    }

    private InlineAudioData generateSpeech(String transcript, String voiceName, AudioLanguage audioLanguage) {
        String prompt = """
                Read this as a friendly teacher explaining a lesson.
                Language and style: %s. %s
                Use clear pacing and a warm, helpful tone.

                %s
                """.formatted(audioLanguage.spokenName(), audioLanguage.styleNote(), transcript);

        String responseBody = callGenerateContent(
                geminiProperties.effectiveTtsModel(),
                prompt,
                ttsGenerationConfig(voiceName)
        );

        return extractInlineAudioData(responseBody);
    }

    private String callGenerateContent(String model, String prompt, Map<String, Object> generationConfig) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));
        requestBody.put("generationConfig", generationConfig);

        try {
            String responseBody = restClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .header("x-goog-api-key", geminiProperties.apiKey())
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                throw new AudioGenerationException("Gemini returned an empty audio-generation response.");
            }

            return responseBody;
        } catch (RestClientResponseException exception) {
            throw new AudioGenerationException(
                    "Gemini audio request failed with status " + exception.getStatusCode().value()
                            + ": " + safeGeminiErrorBody(exception),
                    exception
            );
        } catch (RestClientException exception) {
            throw new AudioGenerationException("Failed to generate lesson audio.", exception);
        }
    }

    private Map<String, Object> transcriptGenerationConfig() {
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("temperature", 0.2);
        generationConfig.put("maxOutputTokens", TRANSCRIPT_MAX_OUTPUT_TOKENS);
        return generationConfig;
    }

    private Map<String, Object> ttsGenerationConfig(String voiceName) {
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("responseModalities", List.of("AUDIO"));
        generationConfig.put("speechConfig", Map.of(
                "voiceConfig", Map.of(
                        "prebuiltVoiceConfig", Map.of("voiceName", voiceName)
                )
        ));
        return generationConfig;
    }

    String parseTranscript(String responseBody) {
        JsonNode root = readResponseBody(responseBody, "Gemini returned unreadable transcript response.");
        String outputText = extractOutputText(root);
        if (outputText == null || outputText.isBlank()) {
            throw new AudioGenerationException("Gemini returned an empty audio transcript.");
        }

        String transcript = stripMarkdownCodeFence(outputText).trim();
        String maybeJsonTranscript = transcriptFromJson(transcript);
        if (maybeJsonTranscript != null && !maybeJsonTranscript.isBlank()) {
            transcript = maybeJsonTranscript.trim();
        }

        transcript = stripWrappingQuotes(transcript);
        if (transcript.isBlank()) {
            throw new AudioGenerationException("Gemini audio transcript is missing transcript text.");
        }

        return transcript;
    }

    private String expandShortTranscript(
            LessonResponse lesson,
            String lessonSourceText,
            String shortTranscript,
            AudioLanguage audioLanguage
    ) {
        String prompt = """
                The current audio script is too short and only introduces the topic.
                Expand it into a fuller lesson explanation.

                Rules:
                - Language requirement: Explain in %s.
                - Language style: %s
                - Keep technical terms in English.
                - Cover the full lesson content.
                - Include practical examples or intuition.
                - Include common mistakes or things to remember.
                - End with a quick recap.
                - Target 2 to 3 minutes of spoken audio.
                - Return only script text. No JSON. No Markdown.

                Lesson title:
                %s

                Too-short script:
                %s

                English lesson content:
                %s
                """.formatted(audioLanguage.spokenName(), audioLanguage.styleNote(), lesson.title(), shortTranscript, lessonSourceText);

        String expandedTranscript = parseTranscript(callGenerateContent(
                geminiProperties.model(),
                prompt,
                transcriptGenerationConfig()
        ));

        if (expandedTranscript.length() > shortTranscript.length()) {
            return expandedTranscript;
        }

        return deterministicExpandedTranscript(lesson, lessonSourceText, shortTranscript, audioLanguage);
    }

    private String deterministicExpandedTranscript(
            LessonResponse lesson,
            String lessonSourceText,
            String shortTranscript,
            AudioLanguage audioLanguage
    ) {
        if (!DEFAULT_AUDIO_LANGUAGE.equals(audioLanguage.code())) {
            return shortTranscript;
        }

        String lessonSummary = abbreviated(lessonSourceText.replaceAll("\\s+", " "), 1_800);
        return """
                %s

                Ab is lesson ko thoda detail mein samajhte hain. Is topic ka main idea yeh hai ki aap concept ko
                sirf memorize na karo, balki uska use-case aur intuition bhi samjho. Pehle objectives par focus karo:
                kya define karna hai, kya identify karna hai, aur is idea ko practical situation mein kaise apply
                karna hai. Lesson content ka short summary yeh hai: %s

                Jab aap is topic ko practice kar rahe ho, ek simple example lo aur usme har step ko slow motion mein
                follow karo. Pehle basic definition bolo, phir dekho ki example mein woh definition kaise apply hoti
                hai. Common mistake yeh hoti hai ki learner directly final answer ya final technique par jump karta
                hai, lekin beech ka reasoning skip kar deta hai. Aapko reasoning ko bhi equally importance deni hai.

                Quick recap: pehle concept samjho, phir example connect karo, phir khud se ek small exercise try karo.
                Agar kahin confusion aaye, course outline mein previous lesson par wapas jao aur foundation revise karo.
                """.formatted(shortTranscript, lessonSummary).trim();
    }

    private InlineAudioData generateSpeechWithRetries(
            String transcript,
            LessonResponse lesson,
            String voiceName,
            AudioLanguage audioLanguage
    ) {
        AudioGenerationException lastException = null;

        for (int attempt = 1; attempt <= TTS_GENERATION_ATTEMPTS; attempt++) {
            try {
                return generateSpeech(transcriptForAttempt(transcript, lesson, attempt, audioLanguage), voiceName, audioLanguage);
            } catch (AudioGenerationException exception) {
                lastException = exception;
            }
        }

        throw new AudioGenerationException(
                "Gemini could not prepare lesson audio after retrying with a shorter explanation.",
                lastException
        );
    }

    String transcriptForAttempt(String transcript, LessonResponse lesson, int attempt) {
        return transcriptForAttempt(transcript, lesson, attempt, languageOption(DEFAULT_AUDIO_LANGUAGE));
    }

    String transcriptForAttempt(String transcript, LessonResponse lesson, int attempt, AudioLanguage audioLanguage) {
        if (attempt == 1) {
            return transcript;
        }

        if (attempt == 2) {
            return shortenTranscript(transcript, TTS_SHORT_TRANSCRIPT_CHARACTERS, audioLanguage);
        }

        return shortenTranscript(transcript, 1_600, audioLanguage);
    }

    private String shortenTranscript(String transcript, int maxCharacters) {
        return shortenTranscript(transcript, maxCharacters, languageOption(DEFAULT_AUDIO_LANGUAGE));
    }

    private String shortenTranscript(String transcript, int maxCharacters, AudioLanguage audioLanguage) {
        if (transcript.length() <= maxCharacters) {
            return transcript;
        }

        int sentenceEnd = transcript.lastIndexOf('.', maxCharacters);
        int cutIndex = sentenceEnd > 400 ? sentenceEnd + 1 : maxCharacters;
        String shortened = transcript.substring(0, cutIndex).trim();

        if (DEFAULT_AUDIO_LANGUAGE.equals(audioLanguage.code())) {
            return shortened + "\n\nQuick recap: Ab lesson ke main ideas ko revise karo aur next example par move karo.";
        }

        return shortened;
    }

    private JsonNode readResponseBody(String responseBody, String errorMessage) {
        try {
            return objectMapper.readTree(responseBody);
        } catch (JsonProcessingException exception) {
            throw new AudioGenerationException(errorMessage, exception);
        }
    }

    private String extractOutputText(JsonNode response) {
        StringBuilder builder = new StringBuilder();
        JsonNode candidates = response.path("candidates");
        if (!candidates.isArray()) {
            return null;
        }

        for (JsonNode candidate : candidates) {
            JsonNode parts = candidate.path("content").path("parts");
            if (!parts.isArray()) {
                continue;
            }

            for (JsonNode part : parts) {
                String text = textAt(part, "text");
                if (text != null) {
                    builder.append(text);
                }
            }
        }

        return builder.toString();
    }

    private String extractJsonObject(String value) {
        String trimmed = stripMarkdownCodeFence(value);
        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');

        if (firstBrace == -1 || lastBrace == -1 || lastBrace <= firstBrace) {
            return trimmed;
        }

        return trimmed.substring(firstBrace, lastBrace + 1);
    }

    private String transcriptFromJson(String value) {
        String extractedJson = extractJsonObject(value);
        if (!extractedJson.startsWith("{")) {
            return null;
        }

        try {
            JsonNode transcriptRoot = objectMapper.readTree(extractedJson);
            return textAt(transcriptRoot, "transcript");
        } catch (JsonProcessingException exception) {
            return null;
        }
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

    private String stripWrappingQuotes(String value) {
        String trimmed = value.trim();
        if (trimmed.length() < 2) {
            return trimmed;
        }

        if ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }

        return trimmed;
    }

    private byte[] wavAudio(InlineAudioData inlineAudioData) {
        if (inlineAudioData.mimeType() != null
                && inlineAudioData.mimeType().toLowerCase(Locale.ROOT).contains("wav")) {
            return inlineAudioData.audio();
        }

        return toWav(
                inlineAudioData.audio(),
                sampleRate(inlineAudioData.mimeType()),
                DEFAULT_PCM_CHANNELS,
                DEFAULT_PCM_BITS_PER_SAMPLE
        );
    }

    private int sampleRate(String mimeType) {
        if (mimeType == null) {
            return DEFAULT_PCM_SAMPLE_RATE;
        }

        Matcher matcher = MIME_RATE_PATTERN.matcher(mimeType);
        if (!matcher.find()) {
            return DEFAULT_PCM_SAMPLE_RATE;
        }

        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException exception) {
            return DEFAULT_PCM_SAMPLE_RATE;
        }
    }

    private String cacheKey(UUID lessonId, String voiceName, String language, String lessonSourceText) {
        return lessonId + "::" + language + "::" + voiceName + "::" + Integer.toHexString(Objects.hash(lessonSourceText));
    }

    private String storageKey(LessonResponse lesson, String voiceName, String language) {
        return "lesson-audio/%s/%s/%s.wav".formatted(
                lesson.id(),
                language,
                safeFileName(voiceName)
        );
    }

    AudioLanguage languageOption(String language) {
        String languageCode = language == null || language.isBlank()
                ? DEFAULT_AUDIO_LANGUAGE
                : safeFileName(language);
        return SUPPORTED_LANGUAGES.getOrDefault(languageCode, SUPPORTED_LANGUAGES.get(DEFAULT_AUDIO_LANGUAGE));
    }

    private static Map<String, AudioLanguage> supportedLanguages() {
        Map<String, AudioLanguage> languages = new LinkedHashMap<>();
        addLanguage(languages, "hinglish", "Hinglish", "Roman-script Hinglish",
                "Use Roman script Hinglish, not Devanagari. Mix simple Hindi and English naturally.");
        addLanguage(languages, "english", "English", "English",
                "Use clear beginner-friendly English.");
        addLanguage(languages, "hindi", "Hindi", "Hindi",
                "Use clear Hindi. Keep unavoidable technical terms in English when they are easier to understand.");
        addLanguage(languages, "telugu", "Telugu", "Telugu",
                "Use clear Telugu. Keep unavoidable technical terms in English when they are easier to understand.");
        addLanguage(languages, "tamil", "Tamil", "Tamil",
                "Use clear Tamil. Keep unavoidable technical terms in English when they are easier to understand.");
        addLanguage(languages, "kannada", "Kannada", "Kannada",
                "Use clear Kannada. Keep unavoidable technical terms in English when they are easier to understand.");
        addLanguage(languages, "malayalam", "Malayalam", "Malayalam",
                "Use clear Malayalam. Keep unavoidable technical terms in English when they are easier to understand.");
        addLanguage(languages, "bengali", "Bengali", "Bengali",
                "Use clear Bengali. Keep unavoidable technical terms in English when they are easier to understand.");
        addLanguage(languages, "marathi", "Marathi", "Marathi",
                "Use clear Marathi. Keep unavoidable technical terms in English when they are easier to understand.");
        addLanguage(languages, "gujarati", "Gujarati", "Gujarati",
                "Use clear Gujarati. Keep unavoidable technical terms in English when they are easier to understand.");
        addLanguage(languages, "punjabi", "Punjabi", "Punjabi",
                "Use clear Punjabi. Keep unavoidable technical terms in English when they are easier to understand.");
        addLanguage(languages, "spanish", "Spanish", "Spanish",
                "Use clear beginner-friendly Spanish.");
        addLanguage(languages, "french", "French", "French",
                "Use clear beginner-friendly French.");
        addLanguage(languages, "german", "German", "German",
                "Use clear beginner-friendly German.");
        addLanguage(languages, "japanese", "Japanese", "Japanese",
                "Use clear beginner-friendly Japanese.");
        addLanguage(languages, "korean", "Korean", "Korean",
                "Use clear beginner-friendly Korean.");
        addLanguage(languages, "arabic", "Arabic", "Arabic",
                "Use clear beginner-friendly Arabic.");
        addLanguage(languages, "portuguese", "Portuguese", "Portuguese",
                "Use clear beginner-friendly Portuguese.");
        addLanguage(languages, "italian", "Italian", "Italian",
                "Use clear beginner-friendly Italian.");
        addLanguage(languages, "chinese", "Chinese", "Mandarin Chinese",
                "Use clear beginner-friendly Mandarin Chinese.");
        return Map.copyOf(languages);
    }

    private static void addLanguage(
            Map<String, AudioLanguage> languages,
            String code,
            String label,
            String spokenName,
            String styleNote
    ) {
        languages.put(code, new AudioLanguage(code, label, spokenName, styleNote));
    }

    private String safeFileName(String value) {
        String normalized = value == null ? "lesson" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        if (normalized.isBlank()) {
            return "lesson";
        }

        return normalized;
    }

    private String abbreviated(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength).trim() + "...";
    }

    private void appendLine(StringBuilder builder, String line) {
        if (line == null || line.isBlank()) {
            return;
        }

        builder.append(line.trim()).append('\n');
    }

    private String stringValue(Object value) {
        if (value == null) {
            return "";
        }

        return String.valueOf(value).trim();
    }

    private String textAt(JsonNode node, String fieldName) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }

        return value.asText();
    }

    private String safeGeminiErrorBody(RestClientResponseException exception) {
        String responseBody = exception.getResponseBodyAsString();
        if (responseBody == null || responseBody.isBlank()) {
            return exception.getStatusText();
        }

        return responseBody;
    }

    record InlineAudioData(byte[] audio, String mimeType) {
    }

    record AudioLanguage(
            String code,
            String label,
            String spokenName,
            String styleNote
    ) {
    }
}

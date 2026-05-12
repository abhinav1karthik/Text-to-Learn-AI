package com.texttolearn.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
        String apiKey,
        String baseUrl,
        String model,
        String ttsModel,
        String ttsVoiceName,
        int audioMaxInputCharacters
) {

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String effectiveTtsModel() {
        if (ttsModel == null || ttsModel.isBlank()) {
            return "gemini-2.5-flash-preview-tts";
        }

        return ttsModel.trim();
    }

    public String effectiveTtsVoiceName() {
        if (ttsVoiceName == null || ttsVoiceName.isBlank()) {
            return "Kore";
        }

        return ttsVoiceName.trim();
    }

    public int sanitizedAudioMaxInputCharacters() {
        if (audioMaxInputCharacters <= 0) {
            return 6_000;
        }

        return Math.min(audioMaxInputCharacters, 12_000);
    }
}

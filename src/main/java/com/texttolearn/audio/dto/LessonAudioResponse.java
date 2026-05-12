package com.texttolearn.audio.dto;

public record LessonAudioResponse(
        byte[] audio,
        String fileName,
        String contentType
) {
}

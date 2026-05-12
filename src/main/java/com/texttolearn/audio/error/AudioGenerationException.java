package com.texttolearn.audio.error;

public class AudioGenerationException extends RuntimeException {

    public AudioGenerationException(String message) {
        super(message);
    }

    public AudioGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}

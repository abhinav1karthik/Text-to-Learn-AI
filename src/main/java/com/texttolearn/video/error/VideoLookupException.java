package com.texttolearn.video.error;

public class VideoLookupException extends RuntimeException {

    public VideoLookupException(String message) {
        super(message);
    }

    public VideoLookupException(String message, Throwable cause) {
        super(message, cause);
    }
}

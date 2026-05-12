package com.texttolearn.audio.storage;

public interface AudioObjectStorageService {

    boolean isConfigured();

    byte[] get(String storageKey);

    void put(String storageKey, byte[] audio, String contentType);
}

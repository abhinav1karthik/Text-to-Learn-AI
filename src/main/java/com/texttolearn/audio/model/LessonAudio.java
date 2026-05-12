package com.texttolearn.audio.model;

import com.texttolearn.course.model.Lesson;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "lesson_audio",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_lesson_audio_lesson_language_voice",
                columnNames = {"lesson_id", "language", "voice_name"}
        )
)
public class LessonAudio {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(nullable = false, length = 32)
    private String language;

    @Column(name = "voice_name", nullable = false, length = 64)
    private String voiceName;

    @Column(name = "storage_provider", nullable = false, length = 32)
    private String storageProvider;

    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected LessonAudio() {
    }

    public LessonAudio(
            Lesson lesson,
            String language,
            String voiceName,
            String storageProvider,
            String storageKey,
            String contentType,
            String fileName,
            long fileSizeBytes
    ) {
        this.id = UUID.randomUUID();
        this.lesson = lesson;
        this.language = language;
        this.voiceName = voiceName;
        this.storageProvider = storageProvider;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.fileName = fileName;
        this.fileSizeBytes = fileSizeBytes;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void replaceStoredObject(String storageKey, String contentType, String fileName, long fileSizeBytes) {
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.fileName = fileName;
        this.fileSizeBytes = fileSizeBytes;
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public Lesson getLesson() {
        return lesson;
    }

    public String getLanguage() {
        return language;
    }

    public String getVoiceName() {
        return voiceName;
    }

    public String getStorageProvider() {
        return storageProvider;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getContentType() {
        return contentType;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}

package com.texttolearn.generation.model;

import com.texttolearn.user.model.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "generation_jobs")
public class GenerationJob {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private GenerationJobType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private GenerationJobStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GenerationJobPriority priority;

    @Column(nullable = false, columnDefinition = "text")
    private String prompt;

    @Column(name = "course_id")
    private UUID courseId;

    @Column(name = "lesson_id")
    private UUID lessonId;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "next_run_at", nullable = false)
    private OffsetDateTime nextRunAt;

    @Column(name = "locked_at")
    private OffsetDateTime lockedAt;

    @Column(name = "locked_by", length = 128)
    private String lockedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_error_type", length = 64)
    private GenerationJobErrorType lastErrorType;

    @Column(name = "last_published_at")
    private OffsetDateTime lastPublishedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    protected GenerationJob() {
    }

    public GenerationJob(AppUser user, GenerationJobType type, String prompt) {
        this(user, type, GenerationJobPriority.NORMAL, prompt, null);
    }

    public GenerationJob(
            AppUser user,
            GenerationJobType type,
            GenerationJobPriority priority,
            String prompt,
            UUID lessonId
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        this.id = UUID.randomUUID();
        this.user = user;
        this.type = type;
        this.status = GenerationJobStatus.QUEUED;
        this.priority = priority;
        this.prompt = prompt;
        this.lessonId = lessonId;
        this.attemptCount = 0;
        this.maxAttempts = 3;
        this.nextRunAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void markRunning() {
        OffsetDateTime now = OffsetDateTime.now();
        this.status = GenerationJobStatus.RUNNING;
        this.attemptCount++;
        this.startedAt = now;
        this.lockedAt = now;
        this.updatedAt = now;
        this.errorMessage = null;
        this.lastErrorType = null;
    }

    public void markRunning(String lockedBy) {
        markRunning();
        this.lockedBy = lockedBy;
    }

    public void markSucceeded(UUID courseId) {
        OffsetDateTime now = OffsetDateTime.now();
        this.status = GenerationJobStatus.SUCCEEDED;
        this.courseId = courseId;
        this.errorMessage = null;
        this.lastErrorType = null;
        this.lockedAt = null;
        this.lockedBy = null;
        this.completedAt = now;
        this.updatedAt = now;
    }

    public void markFailed(String errorMessage) {
        markFailed(errorMessage, GenerationJobErrorType.UNKNOWN);
    }

    public void markFailed(String errorMessage, GenerationJobErrorType errorType) {
        OffsetDateTime now = OffsetDateTime.now();
        this.status = GenerationJobStatus.FAILED;
        this.errorMessage = errorMessage;
        this.lastErrorType = errorType;
        this.lockedAt = null;
        this.lockedBy = null;
        this.completedAt = now;
        this.updatedAt = now;
    }

    public void markPublished() {
        this.lastPublishedAt = OffsetDateTime.now();
        this.updatedAt = this.lastPublishedAt;
    }

    public UUID getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public GenerationJobType getType() {
        return type;
    }

    public GenerationJobStatus getStatus() {
        return status;
    }

    public GenerationJobPriority getPriority() {
        return priority;
    }

    public String getPrompt() {
        return prompt;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public UUID getLessonId() {
        return lessonId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public OffsetDateTime getNextRunAt() {
        return nextRunAt;
    }

    public OffsetDateTime getLockedAt() {
        return lockedAt;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public GenerationJobErrorType getLastErrorType() {
        return lastErrorType;
    }

    public OffsetDateTime getLastPublishedAt() {
        return lastPublishedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }
}

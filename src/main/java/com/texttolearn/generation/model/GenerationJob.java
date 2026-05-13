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

    @Column(nullable = false, columnDefinition = "text")
    private String prompt;

    @Column(name = "course_id")
    private UUID courseId;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

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
        OffsetDateTime now = OffsetDateTime.now();
        this.id = UUID.randomUUID();
        this.user = user;
        this.type = type;
        this.status = GenerationJobStatus.QUEUED;
        this.prompt = prompt;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void markRunning() {
        OffsetDateTime now = OffsetDateTime.now();
        this.status = GenerationJobStatus.RUNNING;
        this.startedAt = now;
        this.updatedAt = now;
        this.errorMessage = null;
    }

    public void markSucceeded(UUID courseId) {
        OffsetDateTime now = OffsetDateTime.now();
        this.status = GenerationJobStatus.SUCCEEDED;
        this.courseId = courseId;
        this.errorMessage = null;
        this.completedAt = now;
        this.updatedAt = now;
    }

    public void markFailed(String errorMessage) {
        OffsetDateTime now = OffsetDateTime.now();
        this.status = GenerationJobStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = now;
        this.updatedAt = now;
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

    public String getPrompt() {
        return prompt;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public String getErrorMessage() {
        return errorMessage;
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

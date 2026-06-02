package com.texttolearn.course.model;

import com.texttolearn.user.model.AppUser;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, columnDefinition = "text")
    private String prompt;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "generation_job_id", unique = true)
    private UUID generationJobId;

    @Column(columnDefinition = "text")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String tags;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CourseStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position asc")
    private List<CourseModule> modules = new ArrayList<>();

    protected Course() {
    }

    public Course(AppUser user, String prompt, String title, String description) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.prompt = prompt;
        this.title = title;
        this.description = description;
        this.tags = "[]";
        this.status = CourseStatus.OUTLINE_READY;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void addModule(CourseModule module) {
        modules.add(module);
        module.assignToCourse(this);
    }

    public void replaceTagsJson(String tags) {
        this.tags = tags;
        this.updatedAt = OffsetDateTime.now();
    }

    public void assignGenerationJob(UUID generationJobId) {
        this.generationJobId = generationJobId;
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getTitle() {
        return title;
    }

    public UUID getGenerationJobId() {
        return generationJobId;
    }

    public String getDescription() {
        return description;
    }

    public String getTags() {
        return tags;
    }

    public CourseStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<CourseModule> getModules() {
        return List.copyOf(modules);
    }
}

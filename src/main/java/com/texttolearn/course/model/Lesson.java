package com.texttolearn.course.model;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "lessons")
public class Lesson {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private CourseModule module;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private int position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private LessonStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "objectives_json", columnDefinition = "jsonb")
    private String objectivesJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_json", columnDefinition = "jsonb")
    private String contentJson;

    @Column(name = "is_enriched", nullable = false)
    private boolean enriched;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Lesson() {
    }

    public Lesson(String title, int position) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.position = position;
        this.status = LessonStatus.PLANNED;
        this.contentJson = "[]";
        this.enriched = false;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    void assignToModule(CourseModule module) {
        this.module = module;
    }

    public void replaceGeneratedContent(String objectivesJson, String contentJson) {
        this.objectivesJson = objectivesJson;
        this.contentJson = contentJson;
        this.status = LessonStatus.GENERATED;
        this.enriched = true;
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public CourseModule getModule() {
        return module;
    }

    public String getTitle() {
        return title;
    }

    public int getPosition() {
        return position;
    }

    public LessonStatus getStatus() {
        return status;
    }

    public String getObjectivesJson() {
        return objectivesJson;
    }

    public String getContentJson() {
        return contentJson;
    }

    public boolean isEnriched() {
        return enriched;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}

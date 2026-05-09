package com.texttolearn.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    private UUID id;

    @Column(name = "auth0_subject", nullable = false, unique = true)
    private String auth0Subject;

    @Column(length = 320)
    private String email;

    @Column(length = 160)
    private String name;

    @Column(name = "picture_url", columnDefinition = "text")
    private String pictureUrl;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected AppUser() {
    }

    public AppUser(String auth0Subject, String email, String name, String pictureUrl) {
        this.id = UUID.randomUUID();
        this.auth0Subject = auth0Subject;
        this.email = email;
        this.name = name;
        this.pictureUrl = pictureUrl;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getAuth0Subject() {
        return auth0Subject;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}

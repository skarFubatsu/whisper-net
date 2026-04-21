package com.whispernetwork.api.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Placeholder-backed user account.
 */
@Entity
@Table(name = "user_accounts")
public class UserAccountEntity {
    @Id
    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    @Column(name = "display_name", length = 120, nullable = false)
    private String displayName;

    @Column(name = "email", length = 200, nullable = false)
    private String email;

    @Column(name = "role", length = 40, nullable = false)
    private String role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserAccountEntity() {}

    public UserAccountEntity(String userId, String displayName, String email, String role) {
        this.userId = userId;
        this.displayName = displayName;
        this.email = email;
        this.role = role;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public String getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

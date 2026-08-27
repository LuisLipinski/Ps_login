package com.mypetadmin.ps_login.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "login_credentials")
public class LoginCredential {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CredentialStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "password_updated_at")
    private Instant passwordUpdatedAt;

    protected LoginCredential() {
    }

    public LoginCredential(UUID id, UUID userId, CredentialStatus status, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.status = status;
        this.createdAt = createdAt;
    }

    public void activate(String passwordHash, Instant now) {
        this.passwordHash = passwordHash;
        this.status = CredentialStatus.ACTIVE;
        this.activatedAt = now;
        this.passwordUpdatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getPasswordHash() { return passwordHash; }
    public CredentialStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getActivatedAt() { return activatedAt; }
    public Instant getPasswordUpdatedAt() { return passwordUpdatedAt; }
}

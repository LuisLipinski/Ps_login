package com.mypetadmin.ps_login.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "activation_tokens")
public class ActivationToken {

    @Id
    private UUID id;

    @Column(name = "credential_id", nullable = false)
    private UUID credentialId;

    @Column(name = "request_id", unique = true)
    private UUID requestId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ActivationToken() {
    }

    public ActivationToken(UUID id, UUID credentialId, String tokenHash, Instant expiresAt, Instant createdAt) {
        this(id, credentialId, null, tokenHash, expiresAt, createdAt);
    }

    public ActivationToken(UUID id,
                           UUID credentialId,
                           UUID requestId,
                           String tokenHash,
                           Instant expiresAt,
                           Instant createdAt) {
        this.id = id;
        this.credentialId = credentialId;
        this.requestId = requestId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public boolean isUsableAt(Instant now) {
        return usedAt == null && revokedAt == null && expiresAt.isAfter(now);
    }

    public void markUsed(Instant now) {
        this.usedAt = now;
    }

    public void revoke(Instant now) {
        if (usedAt == null && revokedAt == null) {
            this.revokedAt = now;
        }
    }

    public UUID getId() { return id; }
    public UUID getCredentialId() { return credentialId; }
    public UUID getRequestId() { return requestId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getUsedAt() { return usedAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public Instant getCreatedAt() { return createdAt; }
}

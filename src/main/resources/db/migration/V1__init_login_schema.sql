CREATE TABLE login_credentials (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activated_at TIMESTAMPTZ,
    password_updated_at TIMESTAMPTZ,
    CONSTRAINT ck_login_credentials_status CHECK (status IN ('PENDING_ACTIVATION', 'ACTIVE'))
);

CREATE TABLE activation_tokens (
    id UUID PRIMARY KEY,
    credential_id UUID NOT NULL REFERENCES login_credentials(id) ON DELETE CASCADE,
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_activation_tokens_credential_id ON activation_tokens(credential_id);
CREATE INDEX idx_activation_tokens_expires_at ON activation_tokens(expires_at);

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY,
    credential_id UUID NOT NULL REFERENCES login_credentials(id) ON DELETE CASCADE,
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_password_reset_tokens_credential_id ON password_reset_tokens(credential_id);
CREATE INDEX idx_password_reset_tokens_expires_at ON password_reset_tokens(expires_at);

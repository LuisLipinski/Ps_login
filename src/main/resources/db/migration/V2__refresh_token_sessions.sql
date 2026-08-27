CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    credential_id UUID NOT NULL REFERENCES login_credentials(id) ON DELETE CASCADE,
    family_id UUID NOT NULL,
    token_hash CHAR(64) NOT NULL UNIQUE,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ
);

CREATE INDEX idx_refresh_tokens_credential_id ON refresh_tokens(credential_id);
CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens(family_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

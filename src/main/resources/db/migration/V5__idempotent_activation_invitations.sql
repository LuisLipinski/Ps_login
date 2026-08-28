ALTER TABLE activation_tokens
    ADD COLUMN request_id UUID;

CREATE UNIQUE INDEX uk_activation_tokens_request_id
    ON activation_tokens (request_id)
    WHERE request_id IS NOT NULL;

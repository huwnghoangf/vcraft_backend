ALTER TABLE users ADD COLUMN security_version INTEGER NOT NULL DEFAULT 0;
CREATE TABLE password_reset_tokens (
    token_hash VARCHAR(64) PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    CHECK (expires_at > created_at)
);
CREATE INDEX idx_password_reset_user ON password_reset_tokens(user_id, created_at DESC);

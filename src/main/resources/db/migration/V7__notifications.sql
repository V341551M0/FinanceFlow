CREATE TABLE notifications (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type         VARCHAR(40) NOT NULL,
    title        VARCHAR(120) NOT NULL,
    message      VARCHAR(500) NOT NULL,
    external_key VARCHAR(160) NOT NULL,
    read         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_notifications_user_external UNIQUE (user_id, external_key)
);
CREATE INDEX idx_notifications_user_read ON notifications(user_id, read, created_at DESC);
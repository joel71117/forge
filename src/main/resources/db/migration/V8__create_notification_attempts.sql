CREATE TABLE notification_attempts (
    id UUID PRIMARY KEY,
    notification_id UUID NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    provider VARCHAR(100) NOT NULL,
    attempt_number INTEGER NOT NULL CHECK (attempt_number > 0),
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ,
    status VARCHAR(30) NOT NULL,
    provider_reference VARCHAR(255),
    error_code VARCHAR(100),
    error_message VARCHAR(1000)
);

CREATE INDEX idx_notification_attempts_notification_id ON notification_attempts (notification_id);
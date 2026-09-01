CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(200) NOT NULL,
    schema_version INTEGER NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL,
    last_error TEXT,
    CONSTRAINT ck_outbox_status CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED'))
);

CREATE INDEX idx_outbox_pending
    ON outbox_events (status, next_attempt_at, created_at);

CREATE TABLE processed_events (
    event_id UUID NOT NULL,
    consumer_name VARCHAR(200) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL,
    result TEXT,
    PRIMARY KEY (event_id, consumer_name)
);

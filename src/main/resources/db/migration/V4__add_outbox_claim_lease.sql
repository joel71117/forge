ALTER TABLE outbox_events
    ADD COLUMN claimed_at TIMESTAMPTZ;

CREATE INDEX idx_outbox_claim_lease
    ON outbox_events (status, claimed_at);

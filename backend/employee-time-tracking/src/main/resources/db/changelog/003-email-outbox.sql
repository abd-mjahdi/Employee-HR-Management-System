CREATE TABLE email_outbox (
    id              BIGSERIAL PRIMARY KEY,
    company_id      BIGINT REFERENCES companies(id),
    aggregate_type  VARCHAR(64) NOT NULL,
    aggregate_id    BIGINT,
    event_type      VARCHAR(64) NOT NULL,
    recipient       VARCHAR(255) NOT NULL,
    payload         JSONB NOT NULL,
    status          TEXT NOT NULL CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    attempt_count   INT NOT NULL DEFAULT 0,
    max_attempts    INT NOT NULL DEFAULT 8,
    next_attempt_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_error      TEXT,
    sent_at         TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX email_outbox_due_idx
    ON email_outbox (next_attempt_at, id)
    WHERE status = 'PENDING';

CREATE INDEX email_outbox_company_id_idx ON email_outbox (company_id);

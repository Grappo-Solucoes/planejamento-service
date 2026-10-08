-- Outbox de eventos de integração e inbox idempotente do serviço.
-- As tabelas vivem explicitamente no schema agendamento (ownership do serviço).

CREATE TABLE IF NOT EXISTS agendamento.outbox_events (
    id VARCHAR(36) PRIMARY KEY,
    event_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    version VARCHAR(16) NOT NULL,
    occurred_on TIMESTAMP NOT NULL,
    correlation_id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    published_at TIMESTAMP,
    last_error TEXT
);

CREATE INDEX IF NOT EXISTS idx_outbox_events_pendentes
    ON agendamento.outbox_events (status, next_attempt_at, created_at);

CREATE TABLE IF NOT EXISTS agendamento.processed_events (
    event_id VARCHAR(36) PRIMARY KEY,
    event_type VARCHAR(120) NOT NULL,
    processed_at TIMESTAMP NOT NULL
);

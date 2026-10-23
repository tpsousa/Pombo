CREATE TABLE events (
                        id              UUID         PRIMARY KEY,
                        user_id         UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
                        type            VARCHAR(64)  NOT NULL,
                        payload         JSONB        NOT NULL,
                        idempotency_key VARCHAR(128),
                        created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Idempotência: mesma chave não pode repetir para o mesmo usuário
CREATE UNIQUE INDEX uq_events_user_idempotency
    ON events (user_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE TABLE deliveries (
                            id              UUID        PRIMARY KEY,
                            event_id        UUID        NOT NULL REFERENCES events (id) ON DELETE CASCADE,
                            endpoint_id     UUID        NOT NULL REFERENCES webhook_endpoints (id) ON DELETE CASCADE,
                            status          VARCHAR(20) NOT NULL,
                            attempt_count   INTEGER     NOT NULL DEFAULT 0,
                            next_attempt_at TIMESTAMPTZ,
                            last_attempt_at TIMESTAMPTZ,
                            created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                            updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                            version         BIGINT      NOT NULL DEFAULT 0
);
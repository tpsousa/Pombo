CREATE TABLE outbox_events (
                               id               UUID         PRIMARY KEY,
                               aggregate_id     UUID         NOT NULL,
                               topic            VARCHAR(120) NOT NULL,
                               message_key      VARCHAR(120),
                               payload          JSONB        NOT NULL,
                               created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
                               published_at     TIMESTAMPTZ,
                               publish_attempts INTEGER      NOT NULL DEFAULT 0
);
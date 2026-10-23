CREATE TABLE webhook_endpoints (
                                   id               UUID          PRIMARY KEY,
                                   user_id          UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
                                   url              VARCHAR(2048) NOT NULL,
                                   description      VARCHAR(255),
                                   event_types      TEXT[]        NOT NULL,
                                   secret_encrypted BYTEA         NOT NULL,
                                   active           BOOLEAN       NOT NULL DEFAULT TRUE,
                                   created_at       TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE TABLE delivery_attempts (
                                   id               UUID          PRIMARY KEY,
                                   delivery_id      UUID          NOT NULL REFERENCES deliveries (id) ON DELETE CASCADE,
                                   attempt_number   INTEGER       NOT NULL,
                                   started_at       TIMESTAMPTZ   NOT NULL,
                                   duration_ms      INTEGER       NOT NULL,
                                   http_status      INTEGER,
                                   error_code       VARCHAR(40),
                                   response_snippet VARCHAR(1024),
                                   CONSTRAINT uq_attempt_per_delivery UNIQUE (delivery_id, attempt_number)
);
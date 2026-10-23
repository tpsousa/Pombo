CREATE INDEX idx_endpoints_user          ON webhook_endpoints (user_id);
CREATE INDEX idx_events_user_created     ON events (user_id, created_at DESC);
CREATE INDEX idx_deliveries_event        ON deliveries (event_id);
CREATE INDEX idx_deliveries_endpoint_st  ON deliveries (endpoint_id, status);
CREATE INDEX idx_deliveries_status_last  ON deliveries (status, last_attempt_at);

-- Publicador do outbox: só varre o que ainda não foi publicado
CREATE INDEX idx_outbox_pending
    ON outbox_events (created_at)
    WHERE published_at IS NULL;
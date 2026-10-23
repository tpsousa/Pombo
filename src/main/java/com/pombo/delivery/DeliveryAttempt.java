package com.pombo.delivery;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "delivery_attempts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"delivery_id", "attempt_number"}))
public class DeliveryAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_id", nullable = false, updatable = false)
    private Delivery delivery;

    @Column(name = "attempt_number", nullable = false, updatable = false)
    private int attemptNumber;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "duration_ms", nullable = false, updatable = false)
    private int durationMs;

    /** Nulo quando não houve resposta HTTP (timeout, DNS, conexão). */
    @Column(name = "http_status", updatable = false)
    private Integer httpStatus;

    /** Ex.: TIMEOUT, CONNECTION_REFUSED, DNS_FAILURE, BLOCKED_TARGET. */
    @Column(name = "error_code", length = 40, updatable = false)
    private String errorCode;

    /** Primeiros ~1 KB da resposta do destinatário. */
    @Column(name = "response_snippet", length = 1024, updatable = false)
    private String responseSnippet;

    protected DeliveryAttempt() { }

    DeliveryAttempt(Delivery delivery, int attemptNumber, Instant startedAt, int durationMs,
                    Integer httpStatus, String errorCode, String responseSnippet) {
        this.delivery = delivery;
        this.attemptNumber = attemptNumber;
        this.startedAt = startedAt;
        this.durationMs = durationMs;
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.responseSnippet = responseSnippet == null ? null
                : responseSnippet.substring(0, Math.min(responseSnippet.length(), 1024));
    }

    public UUID getId() { return id; }
    public int getAttemptNumber() { return attemptNumber; }
    public Instant getStartedAt() { return startedAt; }
    public int getDurationMs() { return durationMs; }
    public Integer getHttpStatus() { return httpStatus; }
    public String getErrorCode() { return errorCode; }
    public String getResponseSnippet() { return responseSnippet; }
}
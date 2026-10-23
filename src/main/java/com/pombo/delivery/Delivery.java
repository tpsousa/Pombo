package com.pombo.delivery;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "deliveries")
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "endpoint_id", nullable = false, updatable = false)
    private UUID endpointId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)

    private DeliveryStatus status = DeliveryStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Concorrência otimista. */
    @Version
    private Long version;

    @OneToMany(mappedBy = "delivery", cascade = CascadeType.ALL)
    @OrderBy("attemptNumber ASC")
    private List<DeliveryAttempt> attempts = new ArrayList<>();

    protected Delivery() { }

    public Delivery(UUID eventId, UUID endpointId) {
        this.eventId = eventId;
        this.endpointId = endpointId;
    }

    /** Registra uma tentativa e mantém o contador consistente com o histórico. */
    public DeliveryAttempt addAttempt(Instant startedAt, int durationMs,
                                      Integer httpStatus, String errorCode, String responseSnippet) {
        int number = ++attemptCount;
        DeliveryAttempt attempt = new DeliveryAttempt(this, number, startedAt,
                durationMs, httpStatus, errorCode, responseSnippet);
        attempts.add(attempt);
        lastAttemptAt = startedAt;
        return attempt;
    }

    /** Reenvio manual: só a partir de FAILED ou DEAD. O serviço traduz a exceção em HTTP 409. */
    public void requeue() {
        if (!status.isRedeliverable()) {
            throw new IllegalStateException("Entrega em estado " + status + " não pode ser reenviada");
        }
        status = DeliveryStatus.PENDING;
        nextAttemptAt = null;
    }

    public void markDelivered()  { status = DeliveryStatus.DELIVERED; nextAttemptAt = null; }
    public void markFailed()     { status = DeliveryStatus.FAILED;    nextAttemptAt = null; }
    public void markDead()       { status = DeliveryStatus.DEAD;      nextAttemptAt = null; }
    public void markRetrying(Instant nextAttemptAt) {
        status = DeliveryStatus.RETRYING;
        this.nextAttemptAt = nextAttemptAt;
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public UUID getEndpointId() { return endpointId; }
    public DeliveryStatus getStatus() { return status; }
    public int getAttemptCount() { return attemptCount; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public Instant getLastAttemptAt() { return lastAttemptAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
    public List<DeliveryAttempt> getAttempts() { return attempts; }
}
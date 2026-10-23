package com.pombo.endpoint;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhook_endpoints")
public class WebhookEndpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Referência por id (e não @ManyToOne) para manter o módulo desacoplado de "user". */
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(length = 255)
    private String description;

    /** Tipos de evento assinados. "*" significa todos. */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "event_types", nullable = false, columnDefinition = "text[]")
    private String[] eventTypes;

    /** Segredo de assinatura cifrado (AES-GCM): nonce + ciphertext + tag. */
    @Column(name = "secret_encrypted", nullable = false, columnDefinition = "bytea")
    private byte[] secretEncrypted;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected WebhookEndpoint() { }

    public WebhookEndpoint(UUID userId, String url, String description,
                           String[] eventTypes, byte[] secretEncrypted) {
        this.userId = userId;
        this.url = url;
        this.description = description;
        this.eventTypes = eventTypes;
        this.secretEncrypted = secretEncrypted;
    }

    public boolean subscribesTo(String eventType) {
        for (String t : eventTypes) {
            if (t.equals("*") || t.equals(eventType)) return true;
        }
        return false;
    }

    public void rotateSecret(byte[] newSecretEncrypted) { this.secretEncrypted = newSecretEncrypted; }
    public void deactivate() { this.active = false; }

    public void update(String url, String description, String[] eventTypes, Boolean active) {
        if (url != null) this.url = url;
        if (description != null) this.description = description;
        if (eventTypes != null) this.eventTypes = eventTypes;
        if (active != null) this.active = active;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getUrl() { return url; }
    public String getDescription() { return description; }
    public String[] getEventTypes() { return eventTypes; }
    public byte[] getSecretEncrypted() { return secretEncrypted; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
}
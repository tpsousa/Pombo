package com.pombo;

import com.pombo.delivery.Delivery;
import com.pombo.delivery.DeliveryStatus;
import com.pombo.endpoint.WebhookEndpoint;
import com.pombo.event.Event;
import com.pombo.outbox.OutboxEvent;
import com.pombo.user.Role;
import com.pombo.user.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Valida as entidades contra um PostgreSQL real e descartável.
 * O Flyway aplica V1..V6 no container e o Hibernate valida o esquema ao subir o contexto.
 * Cada teste roda em transação e é revertido ao final.
 */
@SpringBootTest
@Testcontainers
@Transactional
class EntityPersistenceTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @PersistenceContext
    EntityManager em;

    // ---------- helpers ----------

    private User newUser() {
        User user = new User("User-" + UUID.randomUUID() + "@Exemplo.com", "hash-bcrypt", Role.USER);
        em.persist(user);
        return user;
    }

    private WebhookEndpoint newEndpoint(User user) {
        WebhookEndpoint endpoint = new WebhookEndpoint(user.getId(), "https://cliente.exemplo.com/hook",
                "Destino de teste", new String[]{"pedido.pago", "pedido.cancelado"}, new byte[]{1, 2, 3});
        em.persist(endpoint);
        return endpoint;
    }

    private Event newEvent(User user, String idempotencyKey) {
        Event event = new Event(user.getId(), "pedido.pago", "{\"pedidoId\": 1042, \"valor\": \"199.90\"}", idempotencyKey);
        em.persist(event);
        return event;
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    // ---------- users ----------

    @Test
    void salvaELeUsuario() {
        User user = newUser();
        flushAndClear();

        User found = em.find(User.class, user.getId());

        assertThat(found).isNotNull();
        assertThat(found.getEmail()).isEqualTo(found.getEmail().toLowerCase());
        assertThat(found.getRole()).isEqualTo(Role.USER);
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    void emailDuplicadoViolaUnique() {
        String email = "duplicado@exemplo.com";
        em.persist(new User(email, "hash", Role.USER));
        em.flush();

        em.persist(new User(email, "hash", Role.USER));

        assertThrows(PersistenceException.class, () -> em.flush());
    }

    // ---------- webhook_endpoints (text[] e bytea) ----------

    @Test
    void salvaELeDestinoComArrayEBytea() {
        WebhookEndpoint endpoint = newEndpoint(newUser());
        flushAndClear();

        WebhookEndpoint found = em.find(WebhookEndpoint.class, endpoint.getId());

        assertThat(found.getEventTypes()).containsExactly("pedido.pago", "pedido.cancelado");
        assertThat(found.getSecretEncrypted()).containsExactly(1, 2, 3);
        assertThat(found.isActive()).isTrue();
        assertThat(found.subscribesTo("pedido.pago")).isTrue();
        assertThat(found.subscribesTo("outro.tipo")).isFalse();
    }

    // ---------- events (jsonb e idempotência) ----------

    @Test
    void salvaELeEventoComPayloadJsonb() {
        Event event = newEvent(newUser(), null);
        flushAndClear();

        Event found = em.find(Event.class, event.getId());

        // Se o Hibernate tratasse a String como "texto JSON", começaria com aspas.
        assertThat(found.getPayload()).startsWith("{");
        assertThat(found.getPayload()).contains("\"pedidoId\"").contains("1042");
    }

    @Test
    void idempotencyKeyDuplicadaParaMesmoUsuarioViolaUnique() {
        User user = newUser();
        newEvent(user, "pedido-1042");
        em.flush();

        newEvent(user, "pedido-1042");

        assertThrows(PersistenceException.class, () -> em.flush());
    }

    @Test
    void eventosSemIdempotencyKeyPodemRepetir() {
        User user = newUser();
        newEvent(user, null);
        newEvent(user, null);

        em.flush(); // não deve lançar: o índice único é parcial
    }

    @Test
    void mesmaIdempotencyKeyEmUsuariosDiferentesEPermitida() {
        newEvent(newUser(), "chave-compartilhada");
        newEvent(newUser(), "chave-compartilhada");

        em.flush();
    }

    // ---------- deliveries e attempts ----------

    private Delivery newDeliveryWithTwoAttempts() {
        User user = newUser();
        WebhookEndpoint endpoint = newEndpoint(user);
        Event event = newEvent(user, null);

        Delivery delivery = new Delivery(event.getId(), endpoint.getId());
        delivery.addAttempt(Instant.now(), 412, 503, null, "Service Unavailable");
        delivery.addAttempt(Instant.now(), 5001, null, "TIMEOUT", null);
        delivery.markRetrying(Instant.now().plusSeconds(18));
        em.persist(delivery);
        return delivery;
    }

    @Test
    void salvaEntregaComTentativasEmOrdem() {
        Delivery delivery = newDeliveryWithTwoAttempts();
        flushAndClear();

        Delivery found = em.find(Delivery.class, delivery.getId());

        assertThat(found.getStatus()).isEqualTo(DeliveryStatus.RETRYING);
        assertThat(found.getAttemptCount()).isEqualTo(2);
        assertThat(found.getVersion()).isNotNull();
        assertThat(found.getNextAttemptAt()).isNotNull();
        assertThat(found.getAttempts()).hasSize(2);
        assertThat(found.getAttempts().get(0).getAttemptNumber()).isEqualTo(1);
        assertThat(found.getAttempts().get(0).getHttpStatus()).isEqualTo(503);
        assertThat(found.getAttempts().get(1).getErrorCode()).isEqualTo("TIMEOUT");
        assertThat(found.getAttempts().get(1).getHttpStatus()).isNull();
    }

    @Test
    void numeroDeTentativaRepetidoViolaUnique() {
        Delivery delivery = newDeliveryWithTwoAttempts();
        em.flush();

        var insertDuplicado = em.createNativeQuery(
                        "insert into delivery_attempts (id, delivery_id, attempt_number, started_at, duration_ms) "
                                + "values (gen_random_uuid(), :d, 1, now(), 10)")
                .setParameter("d", delivery.getId());

        assertThrows(PersistenceException.class, insertDuplicado::executeUpdate);
    }

    @Test
    void reenvioSoEPermitidoEmFailedOuDead() {
        Delivery delivery = new Delivery(UUID.randomUUID(), UUID.randomUUID());

        delivery.markDelivered();
        assertThrows(IllegalStateException.class, delivery::requeue);

        delivery.markDead();
        delivery.requeue();
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.PENDING);
    }

    // ---------- outbox ----------

    @Test
    void outboxPendenteSaiDaFilaAoSerPublicado() {
        OutboxEvent outbox = new OutboxEvent(UUID.randomUUID(), "pombo.deliveries", "endpoint-1",
                "{\"deliveryId\": \"abc\", \"schema\": 1}");
        em.persist(outbox);
        em.flush();

        String jpql = "select count(o) from OutboxEvent o where o.publishedAt is null and o.id = :id";
        Long pendentes = em.createQuery(jpql, Long.class).setParameter("id", outbox.getId()).getSingleResult();
        assertThat(pendentes).isEqualTo(1);

        outbox.markPublished();
        em.flush();

        pendentes = em.createQuery(jpql, Long.class).setParameter("id", outbox.getId()).getSingleResult();
        assertThat(pendentes).isZero();
    }
}
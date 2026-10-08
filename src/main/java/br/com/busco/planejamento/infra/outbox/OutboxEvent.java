package br.com.busco.planejamento.infra.outbox;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Registro de outbox: gravado na mesma transação da mudança de estado e
 * publicado de forma assíncrona pelo {@link OutboxPublisher} (entrega ao menos
 * uma vez; {@code eventId}/{@code correlationId} não mudam em republicação).
 */
@Table(name = "outbox_events")
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Column(name = "version", nullable = false, length = 16)
    private String version;

    /** Instante UTC do evento (serializado no envelope com sufixo Z). */
    @Column(name = "occurred_on", nullable = false)
    private LocalDateTime occurredOn;

    @Column(name = "correlation_id", nullable = false, length = 36)
    private String correlationId;

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Column(name = "payload", nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;
}

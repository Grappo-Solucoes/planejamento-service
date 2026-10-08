package br.com.busco.planejamento.infra.inbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

/**
 * Registro de evento já processado (inbox idempotente, CONTRACT §6): cada
 * {@code eventId} é processado no máximo uma vez; reentrega apenas confirma
 * (ack) sem efeito.
 */
@Entity
@Table(name = "processed_events")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED, force = true)
public class ProcessedEvent {

    @Id
    @Column(name = "event_id", nullable = false)
    private final String eventId;

    @Column(name = "event_type", nullable = false, length = 120)
    private final String eventType;

    @Column(name = "processed_at", nullable = false)
    private final LocalDateTime processedAt;
}

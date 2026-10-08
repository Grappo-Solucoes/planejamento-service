package br.com.busco.planejamento.infra.inbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

/**
 * Registro de evento já processado (inbox idempotente, CONTRACT §6): cada
 * {@code eventId} é processado no máximo uma vez; reentrega apenas confirma
 * (ack) sem efeito.
 *
 * <p>Implementa {@link Persistable} com {@code isNew() == true} para que o
 * {@code save()} do Spring Data faça INSERT ({@code persist}) e não
 * {@code merge}: com id atribuído, o merge faria SELECT + UPDATE silencioso
 * quando a linha já existisse, permitindo que uma entrega duplicada que
 * registrasse o marcador após o commit do vencedor também commitasse o
 * comando (dupla aplicação). Com INSERT real, a colisão de {@code event_id}
 * propaga como violação de integridade e reverte a transação do comando.</p>
 */
@Entity
@Table(name = "processed_events")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED, force = true)
public class ProcessedEvent implements Persistable<String> {

    @Id
    @Column(name = "event_id", nullable = false, length = 36)
    private final String eventId;

    @Column(name = "event_type", nullable = false, length = 120)
    private final String eventType;

    @Column(name = "processed_at", nullable = false)
    private final LocalDateTime processedAt;

    @Override
    public String getId() {
        return eventId;
    }

    @Override
    public boolean isNew() {
        return true;
    }
}

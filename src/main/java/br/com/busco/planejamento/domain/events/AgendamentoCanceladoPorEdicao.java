package br.com.busco.planejamento.domain.events;

import br.com.busco.planejamento.domain.AgendamentoOperacional;
import br.com.busco.planejamento.sk.ddd.DomainEvent;
import br.com.busco.planejamento.sk.ids.AgendamentoOperacionalId;
import br.com.busco.planejamento.sk.ids.PlanejamentoLoteId;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Cancelamento de agendamento disparado pela edição do lote (motivo
 * "Cancelado por edição" no contrato v1).
 */
@Data
@Builder
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class AgendamentoCanceladoPorEdicao implements DomainEvent {
    @NotNull(message = "O parâmetro 'id' é obrigatório!")
    private AgendamentoOperacionalId id;
    private PlanejamentoLoteId loteId;
    private LocalDateTime data;
    private Instant occurredOn;

    public static AgendamentoCanceladoPorEdicao of(AgendamentoOperacional agendamento) {
        return new AgendamentoCanceladoPorEdicao(
                agendamento.getId(),
                agendamento.getOrigem() != null ? agendamento.getOrigem().getPlanejamentoId() : null,
                agendamento.getData(),
                Instant.now());
    }
}

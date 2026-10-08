package br.com.busco.planejamento.domain.events;

import br.com.busco.planejamento.domain.AgendamentoOperacional;
import br.com.busco.planejamento.sk.ddd.DomainEvent;
import br.com.busco.planejamento.sk.ids.AgendamentoOperacionalId;
import br.com.busco.planejamento.sk.ids.PlanejamentoLoteId;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class AgendamentoCanceladoPorPlanejamento implements DomainEvent {
    @NotNull(message = "O parâmetro 'id' é obrigatório!")
    private AgendamentoOperacionalId id;
    private PlanejamentoLoteId loteId;
    private LocalDateTime data;
    private Instant occurredOn;

    public static AgendamentoCanceladoPorPlanejamento of(AgendamentoOperacional agendamento) {
        return new AgendamentoCanceladoPorPlanejamento(
                agendamento.getId(),
                agendamento.getOrigem() != null ? agendamento.getOrigem().getPlanejamentoId() : null,
                agendamento.getData(),
                Instant.now());
    }
}

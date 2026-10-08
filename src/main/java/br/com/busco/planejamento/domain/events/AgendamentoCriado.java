package br.com.busco.planejamento.domain.events;

import br.com.busco.planejamento.domain.AgendamentoOperacional;
import br.com.busco.planejamento.sk.ddd.DomainEvent;
import br.com.busco.planejamento.sk.ids.*;
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
public class AgendamentoCriado implements DomainEvent {
    @NotNull(message = "O parâmetro 'id' é obrigatório!")
    private AgendamentoOperacionalId id;
    private PlanejamentoLoteId loteId;
    private LocalDateTime data;
    private RotaId rota;
    private VeiculoId veiculo;
    private MotoristaId motorista;
    private Instant occurredOn;

    public static AgendamentoCriado of(AgendamentoOperacional agendamento) {
        PlanejamentoLoteId loteId = agendamento.getOrigem() != null
                ? agendamento.getOrigem().getPlanejamentoId()
                : null;
        return new AgendamentoCriado(
                agendamento.getId(),
                loteId,
                agendamento.getData(),
                agendamento.getRota(),
                agendamento.getVeiculo(),
                agendamento.getMotorista(),
                Instant.now());
    }
}

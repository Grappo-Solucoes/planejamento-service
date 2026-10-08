package br.com.busco.planejamento.domain.events;

import br.com.busco.planejamento.domain.PlanejamentoLote;
import br.com.busco.planejamento.sk.ddd.DomainEvent;
import br.com.busco.planejamento.sk.ids.MotoristaId;
import br.com.busco.planejamento.sk.ids.PlanejamentoLoteId;
import br.com.busco.planejamento.sk.ids.RotaId;
import br.com.busco.planejamento.sk.ids.VeiculoId;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class PlanejamentoLoteCriado implements DomainEvent {
    @NotNull(message = "O parâmetro 'id' é obrigatório!")
    private PlanejamentoLoteId id;
    private RotaId rota;
    private VeiculoId veiculo;
    private MotoristaId motorista;
    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;
    private Set<DayOfWeek> diasDaSemana;
    private Instant occurredOn;

    public static PlanejamentoLoteCriado of(PlanejamentoLote planejamentoLote) {
        return new PlanejamentoLoteCriado(
                planejamentoLote.getId(),
                planejamentoLote.getRota(),
                planejamentoLote.getVeiculo(),
                planejamentoLote.getMotorista(),
                planejamentoLote.getPeriodo().getInicio(),
                planejamentoLote.getPeriodo().getFim(),
                planejamentoLote.getRecorrencia().getDiasDaSemana(),
                Instant.now());
    }
}

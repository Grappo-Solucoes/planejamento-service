package br.com.busco.planejamento.app.cmd;

import br.com.busco.planejamento.sk.ids.MotoristaId;
import br.com.busco.planejamento.sk.ids.PlanejamentoLoteId;
import br.com.busco.planejamento.sk.ids.RotaId;
import br.com.busco.planejamento.sk.ids.VeiculoId;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Comando de edição dos campos de agenda de um lote
 * (evento {@code planejamento.lote.editar.solicitado}).
 */
@Data
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class EditarPlanejamentoLote {
    private PlanejamentoLoteId id;
    private RotaId rota;
    private VeiculoId veiculo;
    private MotoristaId motorista;
    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;
    private Set<DayOfWeek> diasDaSemana;
}

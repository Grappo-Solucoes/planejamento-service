package br.com.busco.planejamento.app.cmd;

import br.com.busco.planejamento.sk.ids.MotoristaId;
import br.com.busco.planejamento.sk.ids.RotaId;
import br.com.busco.planejamento.sk.ids.VeiculoId;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CriarPlanejamentoLote {
    private RotaId rota;
    private VeiculoId veiculo;
    private MotoristaId motorista;
    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;
    private Set<DayOfWeek> diasDaSemana;
}
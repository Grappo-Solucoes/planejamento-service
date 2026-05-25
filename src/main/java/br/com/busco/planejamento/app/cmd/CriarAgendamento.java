package br.com.busco.planejamento.app.cmd;

import br.com.busco.planejamento.sk.ids.MotoristaId;
import br.com.busco.planejamento.sk.ids.RotaId;
import br.com.busco.planejamento.sk.ids.VeiculoId;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CriarAgendamento {
    private RotaId rota;
    private VeiculoId veiculoPadrao;
    private MotoristaId motoristaPadrao;
    private LocalDateTime data;
    private Set<AlocarPassageiro> passageiros;
}
package br.com.busco.planejamento.app.cmd;

import br.com.busco.planejamento.domain.TipoPassageiro;
import br.com.busco.planejamento.sk.ids.AgendamentoOperacionalId;
import br.com.busco.planejamento.sk.ids.PassageiroId;
import br.com.busco.planejamento.sk.ids.PontoId;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AlocarPassageiro {
    @NotNull(message = "Agendamento ID é obrigatório")
    private AgendamentoOperacionalId agendamentoId;

    @NotNull(message = "Passageiro ID é obrigatório")
    private PassageiroId passageiroId;

    @NotNull(message = "Tipo do passageiro é obrigatório")
    private TipoPassageiro tipo;

    @NotNull(message = "Ponto de embarque é obrigatório")
    private PontoId pontoEmbarque;

    @NotNull(message = "Ponto de desembarque é obrigatório")
    private PontoId pontoDesembarque;

    private String observacao;

}

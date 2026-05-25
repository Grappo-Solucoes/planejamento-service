package br.com.busco.planejamento.app.cmd;

import br.com.busco.planejamento.sk.ids.AgendamentoOperacionalId;
import lombok.*;

@Data
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AlterarVeiculo {
    private AgendamentoOperacionalId id;
}
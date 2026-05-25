package br.com.busco.planejamento.app.cmd;

import br.com.busco.planejamento.sk.ids.PlanejamentoLoteId;
import lombok.*;

@Data
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ReativarPlanejamentoLote {
    private PlanejamentoLoteId id;
}
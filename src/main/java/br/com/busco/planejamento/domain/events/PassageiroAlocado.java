package br.com.busco.planejamento.domain.events;

import br.com.busco.planejamento.domain.AgendamentoOperacional;
import br.com.busco.planejamento.domain.AlocacaoPassageiro;
import br.com.busco.planejamento.domain.PlanejamentoLote;
import br.com.busco.planejamento.sk.ddd.DomainEvent;
import br.com.busco.planejamento.sk.ids.AgendamentoOperacionalId;
import br.com.busco.planejamento.sk.ids.PlanejamentoLoteId;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Data
@Builder
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class PassageiroAlocado implements DomainEvent {
    @NotNull(message = "O parâmetro 'id' é obrigatório!")
    private AgendamentoOperacionalId id;
    private AlocacaoPassageiro alocacao;
    private Instant occurredOn;

    public static PassageiroAlocado of(AgendamentoOperacional agendamentoOperacional, AlocacaoPassageiro alocacao) {
        return new PassageiroAlocado(agendamentoOperacional.getId(), alocacao, Instant.now());
    }
}


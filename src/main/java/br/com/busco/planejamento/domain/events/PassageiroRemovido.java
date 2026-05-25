package br.com.busco.planejamento.domain.events;

import br.com.busco.planejamento.domain.AgendamentoOperacional;
import br.com.busco.planejamento.domain.AlocacaoPassageiro;
import br.com.busco.planejamento.sk.ddd.DomainEvent;
import br.com.busco.planejamento.sk.ids.AgendamentoOperacionalId;
import br.com.busco.planejamento.sk.ids.PassageiroId;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Data
@Builder
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class PassageiroRemovido implements DomainEvent {
    @NotNull(message = "O parâmetro 'id' é obrigatório!")
    private AgendamentoOperacionalId id;
    private PassageiroId passageiroId;
    private String motivo;
    private Instant occurredOn;

    public static PassageiroRemovido of(AgendamentoOperacional agendamentoOperacional, PassageiroId passageiroId, String motivo) {
        return new PassageiroRemovido(agendamentoOperacional.getId(), passageiroId, motivo, Instant.now());
    }
}


package br.com.busco.planejamento.domain.events;

import br.com.busco.planejamento.domain.AgendamentoOperacional;
import br.com.busco.planejamento.sk.ddd.DomainEvent;
import br.com.busco.planejamento.sk.ids.AgendamentoOperacionalId;
import br.com.busco.planejamento.sk.ids.PassageiroId;
import br.com.busco.planejamento.sk.ids.VeiculoId;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Data
@Builder
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class VeiculoSubstituido implements DomainEvent {
    @NotNull(message = "O parâmetro 'id' é obrigatório!")
    private AgendamentoOperacionalId id;
    private VeiculoId veiculoAntigo;
    private VeiculoId veiculoNovo;
    private Instant occurredOn;

    public static VeiculoSubstituido of(AgendamentoOperacional agendamentoOperacional, VeiculoId veiculoAntigo, VeiculoId veiculoNovo) {
        return new VeiculoSubstituido(agendamentoOperacional.getId(), veiculoAntigo, veiculoNovo, Instant.now());
    }
}


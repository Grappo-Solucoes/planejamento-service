package br.com.busco.planejamento.domain.events;

import br.com.busco.planejamento.sk.ddd.DomainEvent;
import br.com.busco.planejamento.sk.ids.PlanejamentoId;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlanejamentoCancelado implements DomainEvent {

    @NotNull(message = "O parâmetro 'id' é obrigatório!")
    private PlanejamentoId id;
    private Instant occurredOn;

    public static PlanejamentoCancelado of(PlanejamentoId id) {
        return new PlanejamentoCancelado(id, Instant.now());
    }
}

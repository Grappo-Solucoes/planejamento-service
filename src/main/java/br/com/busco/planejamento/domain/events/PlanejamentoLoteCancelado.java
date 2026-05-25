package br.com.busco.planejamento.domain.events;

import br.com.busco.planejamento.domain.PlanejamentoLote;
import br.com.busco.planejamento.sk.ddd.DomainEvent;
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
public class PlanejamentoLoteCancelado implements DomainEvent {
    @NotNull(message = "O parâmetro 'id' é obrigatório!")
    private PlanejamentoLoteId id;
    private Instant occurredOn;

    public static PlanejamentoLoteCancelado of(PlanejamentoLote planejamentoLote) {
        return new PlanejamentoLoteCancelado(planejamentoLote.getId(), Instant.now());
    }
}


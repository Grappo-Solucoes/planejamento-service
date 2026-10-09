package br.com.busco.planejamento.domain.events;

import br.com.busco.planejamento.sk.ddd.DomainEvent;
import br.com.busco.planejamento.sk.ids.PlanejamentoId;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Publicado quando o planejamento é editado, para que o contexto de viagem
 * ressincronize as viagens ainda planejadas (substitui a escrita direta
 * {@code sincronizarViagensPlanejadas()} do monólito).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlanejamentoEditado implements DomainEvent {

    @NotNull(message = "O parâmetro 'id' é obrigatório!")
    private PlanejamentoId id;
    private Instant occurredOn;

    public static PlanejamentoEditado of(PlanejamentoId id) {
        return new PlanejamentoEditado(id, Instant.now());
    }
}

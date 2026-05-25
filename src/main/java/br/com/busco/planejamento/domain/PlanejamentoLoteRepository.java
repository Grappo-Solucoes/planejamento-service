package br.com.busco.planejamento.domain;

import br.com.busco.planejamento.sk.ids.PlanejamentoLoteId;

import java.util.Optional;

public interface PlanejamentoLoteRepository {
    PlanejamentoLote save(PlanejamentoLote agendamento);

    Optional<PlanejamentoLote> findById(PlanejamentoLoteId id);
}

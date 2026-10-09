package br.com.busco.planejamento.domain;

import br.com.busco.planejamento.sk.ids.PlanejamentoId;

import java.util.Optional;

public interface PlanejamentoRepository {
    Planejamento save(Planejamento planejamento);

    Optional<Planejamento> findById(PlanejamentoId id);
}

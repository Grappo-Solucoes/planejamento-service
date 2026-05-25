package br.com.busco.planejamento.infra.repo;

import br.com.busco.planejamento.domain.PlanejamentoLote;
import br.com.busco.planejamento.domain.PlanejamentoLoteRepository;
import br.com.busco.planejamento.sk.ids.PlanejamentoLoteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanejamentoLoteRepositoryImpl extends JpaRepository<PlanejamentoLote, PlanejamentoLoteId>, PlanejamentoLoteRepository {
}

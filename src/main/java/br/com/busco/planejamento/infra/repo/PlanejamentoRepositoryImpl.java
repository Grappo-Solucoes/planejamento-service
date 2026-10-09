package br.com.busco.planejamento.infra.repo;

import br.com.busco.planejamento.domain.Planejamento;
import br.com.busco.planejamento.domain.PlanejamentoRepository;
import br.com.busco.planejamento.sk.ids.PlanejamentoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanejamentoRepositoryImpl extends JpaRepository<Planejamento, PlanejamentoId>, PlanejamentoRepository {
}

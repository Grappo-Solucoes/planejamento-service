package br.com.busco.planejamento.infra.repo;

import br.com.busco.planejamento.domain.AgendamentoOperacional;
import br.com.busco.planejamento.domain.AgendamentoRepository;
import br.com.busco.planejamento.sk.ids.AgendamentoOperacionalId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgendamentoRepositoryImpl extends JpaRepository<AgendamentoOperacional, AgendamentoOperacionalId>, AgendamentoRepository {
}

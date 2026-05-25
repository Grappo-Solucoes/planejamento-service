package br.com.busco.planejamento.domain;

import br.com.busco.planejamento.app.AgendamentoService;
import br.com.busco.planejamento.sk.ids.AgendamentoOperacionalId;
import br.com.busco.planejamento.sk.ids.PlanejamentoLoteId;
import br.com.busco.planejamento.sk.ids.RotaId;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AgendamentoRepository {
    AgendamentoOperacional save(AgendamentoOperacional agendamento);
    Optional<AgendamentoOperacional> findById(AgendamentoOperacionalId id);

    @Query("SELECT a.id FROM AgendamentoOperacional a WHERE a.status = br.com.busco.planejamento.domain.StatusAgendamento.EM_ANALISE AND a.data <= :data")
    List<AgendamentoOperacionalId> buscarIdEmAnaliseAte(@Param("data") LocalDateTime data);

    @Query("SELECT a.id FROM AgendamentoOperacional a WHERE a.status = br.com.busco.planejamento.domain.StatusAgendamento.CONFIRMADO AND a.data <= :data")
    List<AgendamentoOperacionalId> buscarIdConfirmadosAte(@Param("data") LocalDateTime data);

    @Query("SELECT a.id FROM AgendamentoOperacional a WHERE a.origem.planejamentoId = :planejamento AND a.data > :data")
    List<AgendamentoOperacionalId> buscarIdFuturosPorPlanejamento( @Param("planejamento") PlanejamentoLoteId planejamento,
                                                                   @Param("data") LocalDateTime data);

//    boolean existeSobreposicao(RotaId rota, LocalDateTime inicio, LocalDateTime fim, AgendamentoOperacionalId ignorar);

}
